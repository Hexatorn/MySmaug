package hexatorn.mysmaug.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dowód, że nagłówek diagnostyczny (Faza 2) niesie każde zamówione pole i że start sesji
 * jest w logu widoczny jako osobny wpis, nie tylko domyślnie z obecności nagłówka. Bez
 * uruchamiania toolkitu JavaFX — {@code Diagnostics} czyta wyłącznie właściwości systemowe
 * i deskryptor modułu javafx.graphics, bez {@code Platform.startup}/{@code Application.launch}.
 *
 * <p>Pisze do {@code target/test-logs/}, wskazanego przez {@code logback-test.xml} — ten sam
 * plik, którego dowiódł już {@code LoggingTest}.
 */
class DiagnosticsTest {

    private static final Path PLIK_LOGU = Path.of("target", "test-logs", "mysmaug-test.log");

    /** Logger tego testu, w roli klasy wołającej — Diagnostics nie ma już własnego loggera. */
    private static final Logger TEST_LOG = LoggerFactory.getLogger(DiagnosticsTest.class);

    @Test
    void naglowekZawieraWszystkiePolaZamowione() throws IOException {
        Diagnostics.zalogujNaglowekStartowy(TEST_LOG);

        String tresc = Files.readString(PLIK_LOGU, StandardCharsets.UTF_8);

        assertThat(tresc)
                .as("Brak wersji aplikacji w nagłówku")
                .contains(Diagnostics.WERSJA_APLIKACJI);
        assertThat(tresc)
                .as("Brak wersji Javy w nagłówku")
                .contains(System.getProperty("java.version"));
        assertThat(tresc)
                .as("Brak wersji JavaFX w nagłówku")
                .contains(wersjaJavaFx());
        assertThat(tresc)
                .as("Brak nazwy systemu operacyjnego w nagłówku")
                .contains(System.getProperty("os.name"));
        assertThat(tresc)
                .as("Brak wersji systemu operacyjnego w nagłówku")
                .contains(System.getProperty("os.version"));
        assertThat(tresc)
                .as("Brak nazwy komputera w nagłówku")
                .contains(nazwaKomputera());
        assertThat(tresc)
                .as("Brak katalogu roboczego w nagłówku")
                .contains(System.getProperty("user.dir"));
        assertThat(tresc)
                .as("Brak bezwzględnej ścieżki pliku logu w nagłówku — bez niej nagłówek nie jest samosprawdzalny")
                .contains(Path.of("log", "mysmaug.log").toAbsolutePath().toString());
        assertThat(tresc)
                .as("Brak wpisu o starcie sesji — bez niego nie da się odróżnić granicy sesji od zwykłego"
                        + " wpisu diagnostycznego; to tak samo zamówione pole nagłówka jak reszta")
                .contains("START SESJI MYSMAUG");
    }

    @Test
    void koniecSesjiMaOsobnyZnacznik() throws IOException {
        Diagnostics.zalogujZakonczenieSesji(TEST_LOG);

        String tresc = Files.readString(PLIK_LOGU, StandardCharsets.UTF_8);

        assertThat(tresc)
                .as("Brak wpisu o zakończeniu sesji — bez niego brak w logu wpisu 'koniec' jest sam"
                        + " w sobie sygnałem awarii, ale tylko jeśli wpis w ogóle bywa zapisywany")
                .contains("KONIEC SESJI MYSMAUG");
    }

    @Test
    void naglowekUzywaLoggeraWolajacego() throws IOException {
        Logger loggerKlasyInicjalizujacej = LoggerFactory.getLogger("hexatorn.mysmaug.app.KlasaInicjalizujacaTest");

        Diagnostics.zalogujNaglowekStartowy(loggerKlasyInicjalizujacej);

        String tresc = Files.readString(PLIK_LOGU, StandardCharsets.UTF_8);

        assertThat(tresc)
                .as("Wpis ma nieść nazwę loggera wołającego (klasy inicjalizującej aplikację), nie loggera"
                        + " Diagnostics — inaczej nazwa loggera w pliku nie wskazuje, kto naprawdę woła")
                .contains("KlasaInicjalizujacaTest");
    }

    /** Ta sama reguła, którą stosuje Diagnostics — dowodzimy obserwowalnego efektu, nie duplikujemy jej w oderwaniu. */
    private static String wersjaJavaFx() {
        return javafx.application.Application.class.getModule().getDescriptor()
                .rawVersion().orElse("nieznana");
    }

    private static String nazwaKomputera() {
        String zEnv = System.getenv("COMPUTERNAME");
        if (zEnv != null && !zEnv.isBlank()) {
            return zEnv;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "nieznany";
        }
    }
}