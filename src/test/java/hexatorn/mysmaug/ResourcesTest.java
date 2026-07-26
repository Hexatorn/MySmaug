package hexatorn.mysmaug;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
 * <p><b>Wada testu wyprowadzanego i obrona przed nią.</b> Gdyby reguła ekstrakcji przestała
 * pasować do kodu (ścieżki przeniesione do pliku właściwości, sklejane z kilku napisów),
 * skaner znalazłby zero odwołań, a asercja na pustym zbiorze braków przeszłaby — zielony bez
 * żadnego pokrycia. To samo zdarzenie, które wprowadza ryzyko, wyłączyłoby jego wykrywanie.
 * Dlatego {@link #skanerZnajdujeOdwolaniaDoZasobow()} pilnuje samego skanera: katalogi źródeł
 * muszą istnieć, a wśród znalezisk musi być co najmniej jeden FXML i jeden arkusz CSS.
 * Dla {@code .png} podłogi nie ma — jedyny dziś to ikona aplikacji, ładowana warunkowo.
 *
 * <p><b>Świadome ograniczenie zakresu:</b> skaner obsługuje konwencje ładowania, które
 * projekt faktycznie stosuje. Ścieżki składanej w czasie działania nie wykryje i nie budujemy
 * pod to obsługi, dopóki taki kod nie powstanie. Klasę awarii, która się prześlizgnie, ma
 * złapać TestFX. Przy dodawaniu zasobu poza istniejącą konwencją — patrz
 * {@code src/main/resources/CLAUDE.md}.
 *
 * <p>Bez uruchamiania toolkitu JavaFX, bez {@code @Tag("ui")}.
 */
class ResourcesTest {

    private static final Path KATALOG_JAVA = Path.of("src", "main", "java");
    private static final Path KATALOG_ZASOBOW = Path.of("src", "main", "resources");

    /**
     * Literał w kodzie Javy wskazujący zasób — cały napis, kończący się rozszerzeniem zasobu.
     * Zakaz białych znaków odsiewa komunikaty w rodzaju {@code "Brak zasobu FXML: /hexatorn/..."}
     * (MySmaugApplication:24), które kończą się tak samo, a ścieżkami nie są. Ścieżki zasobów
     * spacji nie zawierają, więc nic sensownego przez to nie tracimy.
     */
    private static final Pattern LITERAL_JAVA =
            Pattern.compile("\"([^\"\\s]+\\.(?:fxml|css|png))\"");

    /**
     * Odwołanie wewnątrz FXML-a — wartość atrybutu, opcjonalnie z prefiksem {@code @}
     * (np. {@code stylesheets}, {@code fx:include source}, {@code Image url}).
     */
    private static final Pattern ODWOLANIE_FXML =
            Pattern.compile("=\"(@?[^\"\\s]*\\.(?:fxml|css|png))\"");

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

    @Test
    void wszystkieZasobyZKoduSaWidoczneNaClasspath() throws IOException {
        Set<String> odwolania = skanujOdwolania();

        // Zbieramy WSZYSTKIE braki, zamiast przerywać na pierwszym — komunikat ma wymienić
        // każdy zgubiony zasób, nie tylko ten, który wypadł najwcześniej.
        List<String> brakujace = new ArrayList<>();
        for (String odwolanie : odwolania) {
            // Ta sama operacja, której używa kod produkcyjny: getResource na klasie z modułu.
            if (ResourcesTest.class.getResource(odwolanie) == null) {
                brakujace.add(odwolanie);
            }
        }

        assertThat(brakujace)
                .as("Zasoby, do których odwołuje się kod, ale których nie ma na classpathie po"
                        + " zbudowaniu. Skaner znalazł %d odwołań: %s", odwolania.size(), odwolania)
                .isEmpty();
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