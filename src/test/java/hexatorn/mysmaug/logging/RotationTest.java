package hexatorn.mysmaug.logging;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Dowód, że {@link LogRotation} odsuwa zbyt duży plik logu jako archiwum wyłącznie przy starcie
 * aplikacji — bez podmiany pliku w trakcie działania sesji (patrz aneks Fazy 3 w planie).
 *
 * <p>Pracuje na realnym progu 1 MB — tej samej wartości, której użyje produkcja. Fixture 1 MB jest
 * tworzony raz w {@code @BeforeAll} i kopiowany w miejsce testu, żeby nie generować megabajta danych
 * przy każdym z czterech testów. Katalog roboczy leży pod {@code target/test-logs/rotation/}, tak
 * jak inne testy logowania z Fazy 1 — bez katalogu tymczasowego systemu, żeby artefakty były
 * widoczne w projekcie i objęte tym samym {@code .gitignore} co reszta {@code target/}.
 */
class RotationTest {

    private static final double PROG_MB = 1.0;
    private static final long BAJTOW_NA_MB = 1_048_576L;
    private static final int LIMIT_ARCHIWALNYCH = 2;

    private static final Path KATALOG_BAZOWY = Path.of("target", "test-logs", "rotation");

    /** Musi zgadzać się ze wzorcem, który generuje {@code LogRotation} dla pliku {@code mysmaug.log}. */
    private static final Pattern WZORZEC_ARCHIWUM = Pattern.compile("mysmaug-\\d{8}_\\d{6}\\.log");

    private static Path fixture1MB;

    private Path katalogTestu;
    private Path plikLogu;

    @BeforeAll
    static void utworzFixture1MB() throws IOException {
        Files.createDirectories(KATALOG_BAZOWY);
        fixture1MB = KATALOG_BAZOWY.resolve("fixture-1mb.log");
        if (Files.notExists(fixture1MB) || Files.size(fixture1MB) < BAJTOW_NA_MB) {
            String linia = "x".repeat(999) + System.lineSeparator();
            try (BufferedWriter writer = Files.newBufferedWriter(fixture1MB, StandardCharsets.UTF_8)) {
                long zapisanoBajtow = 0;
                while (zapisanoBajtow < BAJTOW_NA_MB) {
                    writer.write(linia);
                    zapisanoBajtow += linia.getBytes(StandardCharsets.UTF_8).length;
                }
            }
        }
    }

    @BeforeEach
    void przygotujKatalogTestu(TestInfo testInfo) throws IOException {
        katalogTestu = KATALOG_BAZOWY.resolve(testInfo.getTestMethod().orElseThrow().getName());
        if (Files.exists(katalogTestu)) {
            usunRekurencyjnie(katalogTestu);
        }
        Files.createDirectories(katalogTestu);
        plikLogu = katalogTestu.resolve("mysmaug.log");
    }

    @Test
    void plikPonizejProguZostajeBezZmian() throws IOException {
        Files.writeString(plikLogu, "krotki wpis");
        // Rozmiar i liczba archiwów liczone PRZED wywołaniem rotacji — z dwóch powodów. Po
        // pierwsze, to one opisują stan faktycznie sprawdzany względem progu. Po drugie, gdyby
        // coś jednak przekształciło oryginał w archiwum, sam pomiar rozmiaru pod starą ścieżką
        // rzuciłby NoSuchFileException, maskując prawdziwą porażkę asercji poniżej.
        double rozmiarPrzedMB = rozmiarMB(plikLogu);
        int liczbaArchiwowPrzed = zliczArchiwa().size();

        LogRotation.obrocJesliZaDuzy(plikLogu, PROG_MB, LIMIT_ARCHIWALNYCH);

        // Brak rotacji = (2.1) liczba archiwów stała, (2.2) oryginał wciąż istnieje pod pierwotną
        // nazwą. Rotacja przekształciłaby oryginał w archiwum (mysmaug.log -> mysmaug-<data>.log)
        // — plik pod starą nazwą by zniknął, nowy mysmaug.log powstałby dopiero przy pierwszym
        // zapisie przez appender, nie w tej metodzie.
        assertThat(plikLogu)
                .as("Plik miał %.6f MB przy progu %.1f MB (poniżej) — nie powinien zostać"
                        + " przekształcony w archiwum, a zniknął spod pierwotnej nazwy",
                        rozmiarPrzedMB, PROG_MB)
                .isRegularFile();
        assertThat(zliczArchiwa())
                .as("Plik poniżej progu nie powinien wygenerować nowego archiwum — przed wywołaniem"
                        + " było ich %d, a liczba się zmieniła", liczbaArchiwowPrzed)
                .hasSize(liczbaArchiwowPrzed);
    }

    @Test
    void brakPlikuNieRzucaINicNieRobi() {
        assertThatCode(() -> LogRotation.obrocJesliZaDuzy(plikLogu, PROG_MB, LIMIT_ARCHIWALNYCH))
                .as("Brak pliku logu (pierwsze uruchomienie) nie powinien wywoływać wyjątku — start"
                        + " aplikacji nie może paść na tym")
                .doesNotThrowAnyException();
        assertThat(plikLogu)
                .as("Sonda nie powinna niczego utworzyć, gdy pliku logu jeszcze nie ma")
                .doesNotExist();
    }

    @Test
    void plikPowyzejProguZostajeOdsunietyJakoArchiwumZZachowaniemTresci() throws IOException {
        Files.copy(fixture1MB, plikLogu);
        String tresc = Files.readString(plikLogu, StandardCharsets.UTF_8);
        double rozmiarFixtureMB = rozmiarMB(plikLogu);

        LogRotation.obrocJesliZaDuzy(plikLogu, PROG_MB, LIMIT_ARCHIWALNYCH);

        assertThat(plikLogu)
                .as("Plik miał %.6f MB przy progu %.1f MB (powyżej) — powinien zniknąć z oryginalnej"
                        + " ścieżki po rotacji, a appender startowy ma tworzyć go od nowa",
                        rozmiarFixtureMB, PROG_MB)
                .doesNotExist();
        List<Path> archiwa = zliczArchiwa();
        assertThat(archiwa)
                .as("Rotacja jednego przekroczenia progu %.1f MB powinna zostawić dokładnie jedno"
                        + " archiwum, a zostawiła %d", PROG_MB, archiwa.size())
                .hasSize(1);
        Path archiwum = archiwa.getFirst();
        assertThat(Files.readString(archiwum, StandardCharsets.UTF_8))
                .as("Archiwum %s powinno zachować pełną treść oryginalnego pliku, a jej nie zachowało",
                        archiwum)
                .isEqualTo(tresc);
    }

    @Test
    void nadmiaroweArchiwaSaPrzycinaneZZachowaniemNajnowszych() throws IOException {
        Path najstarsze = utworzArchiwum("mysmaug-20260101_000000.log", Instant.parse("2026-01-01T00:00:00Z"));
        utworzArchiwum("mysmaug-20260201_000000.log", Instant.parse("2026-02-01T00:00:00Z"));
        utworzArchiwum("mysmaug-20260301_000000.log", Instant.parse("2026-03-01T00:00:00Z"));
        Files.copy(fixture1MB, plikLogu);

        LogRotation.obrocJesliZaDuzy(plikLogu, PROG_MB, LIMIT_ARCHIWALNYCH);

        List<Path> archiwa = zliczArchiwa();
        assertThat(archiwa)
                .as("Po rotacji przy trzech istniejących archiwach i limicie %d plików powinny"
                        + " zostać dokładnie %d, a zostało %d", LIMIT_ARCHIWALNYCH, LIMIT_ARCHIWALNYCH,
                        archiwa.size())
                .hasSize(LIMIT_ARCHIWALNYCH);
        assertThat(archiwa)
                .as("Najstarsze archiwum %s powinno zostać usunięte przy przycinaniu do limitu %d"
                        + " plików, a wciąż istnieje", najstarsze, LIMIT_ARCHIWALNYCH)
                .doesNotContain(najstarsze);
    }

    @Test
    void obcePlikiWKatalogNieSaUsuwanePrzyPrzycinaniu() throws IOException {
        utworzArchiwum("mysmaug-20260101_000000.log", Instant.parse("2026-01-01T00:00:00Z"));
        utworzArchiwum("mysmaug-20260201_000000.log", Instant.parse("2026-02-01T00:00:00Z"));
        // Plik, który NIE pasuje do wzorca nazwy archiwum tej rotacji — symuluje realny scenariusz
        // (np. użytkownik zostawił notatkę w log/), nie tylko debugowy artefakt. Przycinanie nie ma
        // prawa uznać go za swoje archiwum tylko dlatego, że nie jest aktywnym plikiem logu.
        Path obcyPlik = katalogTestu.resolve("notatka.txt");
        Files.writeString(obcyPlik, "nie mam nic wspolnego z rotacja logow");
        Files.copy(fixture1MB, plikLogu);

        LogRotation.obrocJesliZaDuzy(plikLogu, PROG_MB, LIMIT_ARCHIWALNYCH);

        assertThat(obcyPlik)
                .as("Plik %s nie pasuje do wzorca nazwy archiwum rotacji — przycinanie nie powinno"
                        + " go ruszać, a zniknął", obcyPlik)
                .isRegularFile();
        List<Path> archiwa = zliczArchiwa();
        assertThat(archiwa)
                .as("Liczba prawdziwych archiwów rotacji (dopasowanych do wzorca nazwy) powinna być"
                        + " przycięta do limitu %d, a jest %d", LIMIT_ARCHIWALNYCH, archiwa.size())
                .hasSize(LIMIT_ARCHIWALNYCH);
    }

    @Test
    void przycinanieOpierASieNaZnacznikuCzasuZNazwyNieNaCzasieModyfikacji() throws IOException {
        Path stareNazwa = utworzArchiwum("mysmaug-20260101_000000.log", Instant.parse("2026-01-01T00:00:00Z"));
        Path mlodeNazwa = utworzArchiwum("mysmaug-20260301_000000.log", Instant.parse("2026-03-01T00:00:00Z"));
        // Ktoś dotknął starego pliku PO utworzeniu obu (np. otworzył i dopisał znak) — jego mtime
        // jest teraz nowszy niż mtime pliku, który po nazwie jest młodszy. Kolejność przycinania ma
        // się opierać na znaczniku w nazwie, nie na tym metadanym — który jest mutowalny przez
        // kogokolwiek, bez zmiany tego, KIEDY faktycznie doszło do rotacji.
        Files.setLastModifiedTime(stareNazwa, FileTime.from(Instant.parse("2026-06-01T00:00:00Z")));
        Files.copy(fixture1MB, plikLogu);

        LogRotation.obrocJesliZaDuzy(plikLogu, PROG_MB, LIMIT_ARCHIWALNYCH);

        List<Path> archiwa = zliczArchiwa();
        assertThat(archiwa)
                .as("%s ma najstarszy znacznik w nazwie (styczeń) mimo najnowszego czasu modyfikacji"
                        + " (czerwiec) — przycinanie oparte na nazwie powinno je usunąć jako"
                        + " najstarsze niezależnie od mtime, a wciąż istnieje", stareNazwa)
                .doesNotContain(stareNazwa);
        assertThat(archiwa)
                .as("%s ma nowszy znacznik w nazwie (marzec) niż %s (styczeń) — powinno przetrwać"
                        + " przycinanie niezależnie od mtime, a zniknęło", mlodeNazwa, stareNazwa)
                .contains(mlodeNazwa);
    }

    private Path utworzArchiwum(String nazwa, Instant czasModyfikacji) throws IOException {
        Path archiwum = katalogTestu.resolve(nazwa);
        Files.writeString(archiwum, "tresc-" + nazwa);
        Files.setLastModifiedTime(archiwum, FileTime.from(czasModyfikacji));
        return archiwum;
    }

    private List<Path> zliczArchiwa() throws IOException {
        try (Stream<Path> pliki = Files.list(katalogTestu)) {
            return pliki.filter(Files::isRegularFile)
                    .filter(plik -> WZORZEC_ARCHIWUM.matcher(plik.getFileName().toString()).matches())
                    .sorted()
                    .toList();
        }
    }

    private static double rozmiarMB(Path plik) throws IOException {
        return Files.size(plik) / (double) BAJTOW_NA_MB;
    }

    private static void usunRekurencyjnie(Path katalog) throws IOException {
        try (Stream<Path> pliki = Files.walk(katalog)) {
            pliki.sorted(Comparator.reverseOrder()).forEach(plik -> {
                try {
                    Files.delete(plik);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}