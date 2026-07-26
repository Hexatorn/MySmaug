package hexatorn.mysmaug;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test zasobów — wyprowadza listę odwołań wprost z kodu i sprawdza, że każde z nich
 * rozwiązuje się na zbudowanym classpathie. Łapie dwie różne awarie naraz: rozjazd
 * między ścieżką w kodzie a plikiem na dysku oraz zasób, który wypadł z pakowania
 * modułowego. Obie ujawniłyby się inaczej dopiero przy starcie aplikacji.
 *
 * <p>Lista nie jest wpisana ręcznie, bo sztywna lista gnije: każdy widok dodany w kolejnym
 * slice'ie wymagałby pamiętania o dopisaniu wpisu, a pominięcie nie dawałoby żadnego sygnału.
 * Skaner odwrotnie — zasób dodany w istniejącej konwencji wpada do niego sam.
 *
 * <p><b>Jeden test na zasób.</b> {@link #kazdyZasobZKoduJestWidocznyNaClasspath()} to fabryka
 * ({@code @TestFactory}), nie pojedynczy test: buduje osobny przypadek dla każdego znaleziska,
 * nazwany jego ścieżką. Dzięki temu przy czerwieni winowajca stoi w nazwie testu, a nie dopiero
 * w komunikacie asercji, a liczba testów w raporcie odpowiada liczbie pokrytych zasobów.
 * Fabryka nie wymaga dodatkowej zależności — {@code DynamicTest} należy do junit-jupiter-api.
 *
 * <p><b>Dwa kierunki, które się wzajemnie kontrolują.</b>
 * {@link #kazdyZasobZKoduJestWidocznyNaClasspath()} idzie od kodu do pliku i łapie zasób
 * usunięty, przeniesiony albo wypadnięty z pakowania. {@link #kazdyZasobNaDyskuJestUzywanyPrzezKod()}
 * idzie odwrotnie i łapie plik osierocony — taki, którego nikt nie ładuje, a który mimo to jedzie
 * do artefaktu.
 *
 * <p><b>Wada testu wyprowadzanego i obrona przed nią.</b> Gdyby reguła ekstrakcji przestała
 * pasować do kodu (ścieżki przeniesione do pliku właściwości, sklejane z kilku napisów), skaner
 * znalazłby mniej odwołań albo zero, a fabryka kod&rarr;zasób wyprodukowałaby odpowiednio mniej
 * testów — same przechodzące. To samo zdarzenie, które wprowadza ryzyko, wyłączyłoby jego
 * wykrywanie. Dlatego kierunek odwrotny jest tu ważniejszy niż wykrywanie sierot: pliki dalej leżą
 * na dysku, więc zapali się na wszystkich naraz. Dodatkowo
 * {@link #skanerZnajdujeOdwolaniaDoZasobow()} trzyma podłogę na wypadek, gdyby zniknęły też same
 * katalogi źródeł: muszą istnieć, a wśród znalezisk musi być co najmniej jeden FXML i jeden arkusz
 * CSS. Dla {@code .png} podłogi nie ma — jedyny dziś to ikona aplikacji, ładowana warunkowo, więc
 * wymóg jej obecności kłóciłby się z kodem.
 *
 * <p><b>Świadome ograniczenie zakresu:</b> skaner obsługuje konwencje ładowania, które projekt
 * faktycznie stosuje. Ścieżki składanej w czasie działania nie wykryje i nie budujemy pod to
 * obsługi, dopóki taki kod nie powstanie. Klasę awarii, która się prześlizgnie, ma złapać
 * TestFX.
 *
 * <p>Bez uruchamiania toolkitu JavaFX, bez {@code @Tag("ui")}.
 */
class ResourcesTest {

    private static final Path KATALOG_JAVA = Path.of("src", "main", "java");
    private static final Path KATALOG_ZASOBOW = Path.of("src", "main", "resources");

    /**
     * Rozszerzenia, po których skaner <b>tekstu</b> rozpoznaje ścieżkę zasobu. Dotyczy wyłącznie
     * kierunku kod&rarr;zasób: przeszukując źródła, trzeba jakoś odróżnić literał ze ścieżką od
     * dowolnego innego napisu, a rozszerzenie jest tu jedyną zaczepką. Kierunek odwrotny żadnego
     * filtra nie ma — enumeruje katalog, więc niczego nie musi zgadywać.
     *
     * <p>Skutek tej asymetrii jest zamierzony: zasób o nieznanym rozszerzeniu zgłosi się jako
     * osierocony i tym samym upomni o dopisanie go tutaj. Lista nie jest definicją „co jest
     * zasobem", tylko stanem wiedzy skanera tekstu — a drugi kierunek pilnuje jej aktualności.
     *
     * <p>Zapisane jako literał, żeby wyrażenia niżej pozostały stałymi kompilacji — inaczej
     * inspekcja wyrażeń regularnych w IDE nie umie ich rozwinąć i zgłasza fałszywe ostrzeżenia.
     */
    private static final String ALTERNATYWA = "fxml|css|png";

    /**
     * Literał w kodzie Javy wskazujący zasób — cały napis, kończący się rozszerzeniem zasobu.
     * Zakaz białych znaków odsiewa komunikaty w rodzaju {@code "Brak zasobu FXML: /hexatorn/..."}
     * (MySmaugApplication:24), które kończą się tak samo, a ścieżkami nie są. Ścieżki zasobów
     * spacji nie zawierają, więc nic sensownego przez to nie tracimy.
     */
    private static final Pattern LITERAL_JAVA =
            Pattern.compile("\"([^\"\\s]+\\.(?:" + ALTERNATYWA + "))\"");

    /**
     * Odwołanie wewnątrz FXML-a — wartość atrybutu, opcjonalnie z prefiksem {@code @}
     * (np. {@code stylesheets}, {@code fx:include source}, {@code Image url}).
     */
    private static final Pattern ODWOLANIE_FXML =
            Pattern.compile("=\"(@?[^\"\\s]*\\.(?:" + ALTERNATYWA + "))\"");

    @Test
    void skanerZnajdujeOdwolaniaDoZasobow() throws IOException {
        assertThat(KATALOG_JAVA)
                .as("Katalog źródeł Javy — bez niego skaner nie ma czego czytać")
                .isDirectory();
        assertThat(KATALOG_ZASOBOW)
                .as("Katalog zasobów — bez niego skaner nie ma czego czytać")
                .isDirectory();

        Set<String> odwolania = skanujOdwolania();

        assertThat(odwolania)
                .as("Skaner nie znalazł ani jednego odwołania — reguła ekstrakcji przestała pasować do kodu")
                .isNotEmpty();
        assertThat(odwolania)
                .as("Brak odwołania do jakiegokolwiek FXML-a — skaner przestał widzieć widoki")
                .anyMatch(odwolanie -> odwolanie.endsWith(".fxml"));
        assertThat(odwolania)
                .as("Brak odwołania do arkusza stylów — skaner przestał widzieć CSS")
                .anyMatch(odwolanie -> odwolanie.endsWith(".css"));
    }

    /**
     * Po jednym teście na każde znalezione odwołanie, nazwanym jego ścieżką. Testy są niezależne,
     * więc pierwszy brakujący zasób nie ucina sprawdzenia pozostałych — w raporcie widać komplet
     * winowajców, każdego z osobna.
     */
    @TestFactory
    Stream<DynamicTest> kazdyZasobZKoduJestWidocznyNaClasspath() throws IOException {
        return skanujOdwolania().stream()
                .map(odwolanie -> DynamicTest.dynamicTest(odwolanie, () ->
                        // Ta sama operacja, której używa kod produkcyjny: getResource na klasie z modułu.
                        assertThat(ResourcesTest.class.getResource(odwolanie))
                                .as("Kod odwołuje się do tego zasobu, ale nie ma go na classpathie"
                                        + " po zbudowaniu")
                                .isNotNull()));
    }

    /**
     * Kierunek odwrotny — po jednym teście na każdy zasób leżący na dysku, sprawdzającym, że kod
     * w ogóle się do niego odwołuje. Wprost łapie pliki osierocone: takie, których nikt nie ładuje,
     * a które i tak jadą do artefaktu i do obrazu jlink.
     *
     * <p>Ważniejsza jednak jest jego druga rola: <b>to strażnik samego skanera</b>. Gdy reguła
     * ekstrakcji przestanie pasować do kodu, kierunek kod&rarr;zasób po cichu wyprodukuje mniej
     * testów, wszystkie zielone. Ten zapali się wtedy na wszystkich zasobach naraz, bo pliki dalej
     * leżą na dysku, a odwołań nagle brak. Cicha degradacja zamienia się w głośną czerwień.
     *
     * <p>Skutek uboczny, i to pożądany: nowa konwencja ładowania (np. {@code url(...)} w CSS)
     * objawi się jako osierocony zasób, czyli sama poprosi o rozszerzenie skanera.
     */
    @TestFactory
    Stream<DynamicTest> kazdyZasobNaDyskuJestUzywanyPrzezKod() throws IOException {
        Set<String> odwolania = skanujOdwolania();
        return zasobyNaDysku().stream()
                .map(zasob -> DynamicTest.dynamicTest(zasob, () ->
                        assertThat(odwolania)
                                .as("Zasób leży w src/main/resources, ale żadne odwołanie w kodzie"
                                        + " go nie wskazuje — plik osierocony albo konwencja"
                                        + " ładowania, której skaner nie zna")
                                .contains(zasob)));
    }

    /**
     * Wszystko, co leży w katalogu zasobów, wyrażone jako ścieżki na classpathie. Bez filtrowania
     * po rozszerzeniu: cokolwiek trafia do {@code src/main/resources}, trafia też do artefaktu
     * i do obrazu jlink, więc podlega pytaniu „kto to ładuje".
     */
    private static Set<String> zasobyNaDysku() throws IOException {
        Set<String> zasoby = new TreeSet<>();
        try (Stream<Path> zawartosc = Files.walk(KATALOG_ZASOBOW)) {
            List<Path> pliki = zawartosc
                    .filter(Files::isRegularFile)
                    .toList();
            for (Path plik : pliki) {
                zasoby.add(bazaDla(KATALOG_ZASOBOW, plik) + "/" + plik.getFileName());
            }
        }
        return zasoby;
    }

    /** Wszystkie odwołania do zasobów znalezione w kodzie Javy i w plikach FXML, bez duplikatów. */
    private static Set<String> skanujOdwolania() throws IOException {
        Set<String> odwolania = new TreeSet<>();
        zbierzZKatalogu(KATALOG_JAVA, ".java", LITERAL_JAVA, odwolania);
        zbierzZKatalogu(KATALOG_ZASOBOW, ".fxml", ODWOLANIE_FXML, odwolania);
        return odwolania;
    }

    /** Przechodzi katalog, dopasowuje wzorzec w każdym pliku o danym rozszerzeniu i rozwiązuje znaleziska. */
    private static void zbierzZKatalogu(Path korzen, String rozszerzenie, Pattern wzorzec,
                                        Set<String> wynik) throws IOException {
        try (Stream<Path> zawartosc = Files.walk(korzen)) {
            List<Path> pliki = zawartosc
                    .filter(Files::isRegularFile)
                    .filter(plik -> plik.getFileName().toString().endsWith(rozszerzenie))
                    .toList();
            for (Path plik : pliki) {
                Matcher dopasowanie = wzorzec.matcher(Files.readString(plik));
                while (dopasowanie.find()) {
                    wynik.add(rozwiazSciezke(bazaDla(korzen, plik), dopasowanie.group(1)));
                }
            }
        }
    }

    /**
     * Baza dla ścieżek względnych — katalog pliku wyrażony jako ścieżka na classpathie.
     * Dla {@code src/main/java/hexatorn/mysmaug/controller/MainController.java} zwraca
     * {@code /hexatorn/mysmaug/controller}. Iteracja po segmentach zamiast na napisach,
     * żeby nie zależeć od separatora katalogów systemu.
     */
    private static String bazaDla(Path korzen, Path plik) {
        Path katalog = korzen.relativize(plik).getParent();
        if (katalog == null) {
            return "";
        }
        StringBuilder baza = new StringBuilder();
        for (Path segment : katalog) {
            baza.append('/').append(segment);
        }
        return baza.toString();
    }

    /**
     * Sprowadza odwołanie do ścieżki bezwzględnej na classpathie: zdejmuje prefiks {@code @}
     * z FXML-a, dokleja bazę dla ścieżek względnych i usuwa segmenty {@code .} oraz {@code ..}.
     */
    private static String rozwiazSciezke(String baza, String odwolanie) {
        String bezPrefiksu = odwolanie.startsWith("@") ? odwolanie.substring(1) : odwolanie;
        String pelna = bezPrefiksu.startsWith("/") ? bezPrefiksu : baza + "/" + bezPrefiksu;

        Deque<String> segmenty = new ArrayDeque<>();
        for (String segment : pelna.split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                segmenty.pollLast();
            } else {
                segmenty.addLast(segment);
            }
        }
        return "/" + String.join("/", segmenty);
    }
}