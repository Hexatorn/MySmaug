package hexatorn.mysmaug.logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Sonda rotacji wołana wyłącznie przy starcie aplikacji, zanim padnie pierwszy
 * {@code LoggerFactory.getLogger(...)} w całej JVM (patrz aneks Fazy 3 w planie — Logback otwiera
 * plik logu w tym samym momencie, w którym pada to pierwsze wywołanie). Dzięki temu odsunięcie
 * zbyt dużego pliku nie koliduje z otwartym uchwytem zapisu — problem, który uczynił
 * {@code FixedWindowRollingPolicy} deprecated, tu nie występuje: nie ma podmiany w trakcie sesji.
 */
public final class LogRotation {

    private static final long BAJTOW_NA_MB = 1_048_576L;
    private static final DateTimeFormatter WZORZEC_ZNACZNIKA = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private LogRotation() {
    }

    /**
     * Gdy {@code plikLogu} przekracza {@code progMB}, przenosi go pod nazwę z dopisanym
     * znacznikiem czasu i przycina katalog do {@code limitArchiwalnych} najnowszych archiwów.
     * Porażka (np. brak uprawnień) jest połykana — ta sama zasada co {@code sondujKatalogLogow}
     * w {@code MySmaugApplication}: rotacja nie może przerwać startu aplikacji.
     */
    public static void obrocJesliZaDuzy(Path plikLogu, double progMB, int limitArchiwalnych) {
        try {
            if (!Files.isRegularFile(plikLogu)) {
                return;
            }
            long progBajtow = (long) (progMB * BAJTOW_NA_MB);
            if (Files.size(plikLogu) <= progBajtow) {
                return;
            }
            Path archiwum = plikLogu.resolveSibling(nazwaArchiwum(plikLogu));
            Files.move(plikLogu, archiwum);
            przytnijArchiwa(plikLogu, limitArchiwalnych);
        } catch (IOException e) {
            // Świadomie połknięte — patrz Javadoc metody.
        }
    }

    private static String nazwaArchiwum(Path plikLogu) {
        String rdzen = rdzenNazwy(plikLogu);
        String rozszerzenie = rozszerzenieNazwy(plikLogu);
        return rdzen + "-" + LocalDateTime.now().format(WZORZEC_ZNACZNIKA) + rozszerzenie;
    }

    /**
     * Rozpoznaje WŁASNE archiwa tej rotacji po nazwie — nie każdy plik w katalogu poza aktywnym.
     * Bez tego przycinanie potraktowałoby dowolny obcy plik w katalogu logów (np. zostawioną tam
     * notatkę) jako kandydata do usunięcia, tylko dlatego że nie jest aktywnym plikiem logu.
     */
    private static Pattern wzorzecNazwyArchiwum(Path plikLogu) {
        String rdzen = Pattern.quote(rdzenNazwy(plikLogu));
        String rozszerzenie = Pattern.quote(rozszerzenieNazwy(plikLogu));
        // Grupa przechwytująca na znacznik czasu — pozwala go potem wyciągnąć do sortowania
        // w przytnijArchiwa, zamiast polegać na czasie modyfikacji pliku (patrz Javadoc tamtej metody).
        return Pattern.compile(rdzen + "-(\\d{8}_\\d{6})" + rozszerzenie);
    }

    private static String rdzenNazwy(Path plikLogu) {
        String nazwaPliku = plikLogu.getFileName().toString();
        int kropka = nazwaPliku.lastIndexOf('.');
        return kropka >= 0 ? nazwaPliku.substring(0, kropka) : nazwaPliku;
    }

    private static String rozszerzenieNazwy(Path plikLogu) {
        String nazwaPliku = plikLogu.getFileName().toString();
        int kropka = nazwaPliku.lastIndexOf('.');
        return kropka >= 0 ? nazwaPliku.substring(kropka) : "";
    }

    /**
     * Usuwa najstarsze archiwa ponad limit — wyłącznie spośród plików pasujących do wzorca nazwy
     * własnych archiwów, nigdy spośród plików obcych danej rotacji. Kolejność „najnowsze" liczona
     * jest po znaczniku czasu wyciągniętym z NAZWY pliku, nie po czasie modyfikacji systemu plików
     * — ten drugi jest mutowalny przez kogokolwiek (np. otwarcie i dopisanie znaku do starego
     * archiwum zmienia jego mtime), a znacznik w nazwie jest ustalany raz, przy rotacji, i nie
     * zmienia się bez jawnej zmiany nazwy pliku.
     */
    private static void przytnijArchiwa(Path plikAktywny, int limitArchiwalnych) throws IOException {
        Path katalog = plikAktywny.getParent();
        Pattern wzorzecArchiwum = wzorzecNazwyArchiwum(plikAktywny);
        try (Stream<Path> pliki = Files.list(katalog)) {
            List<Path> archiwa = pliki
                    .filter(Files::isRegularFile)
                    .map(plik -> dopasujArchiwum(plik, wzorzecArchiwum))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Archiwum::znacznik).reversed())
                    .map(Archiwum::plik)
                    .toList();
            for (Path nadmiarowe : archiwa.subList(Math.min(limitArchiwalnych, archiwa.size()), archiwa.size())) {
                Files.deleteIfExists(nadmiarowe);
            }
        }
    }

    /**
     * Dopasowuje plik do wzorca własnych archiwów **raz** — jednocześnie filtruje (obcy plik ->
     * {@code null}) i wyciąga znacznik czasu z grupy 1, zamiast dopasowywać dwukrotnie (osobno w
     * filtrze, osobno przy sortowaniu).
     */
    private static Archiwum dopasujArchiwum(Path plik, Pattern wzorzecArchiwum) {
        Matcher dopasowanie = wzorzecArchiwum.matcher(plik.getFileName().toString());
        if (!dopasowanie.matches()) {
            return null;
        }
        return new Archiwum(plik, LocalDateTime.parse(dopasowanie.group(1), WZORZEC_ZNACZNIKA));
    }

    private record Archiwum(Path plik, LocalDateTime znacznik) {
    }
}