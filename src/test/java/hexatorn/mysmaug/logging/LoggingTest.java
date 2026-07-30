package hexatorn.mysmaug.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dowód, że logowanie działa end-to-end: wpis oddany do SLF4J faktycznie ląduje w pliku
 * i przenosi polskie diakrytyki. Sama obecność biblioteki w buildzie tego nie dowodzi —
 * bez implementacji pod API SLF4J wpina logger NOP i wszystko znika bez śladu i bez błędu.
 *
 * <p>Pisze do {@code target/test-logs/}, wskazanego przez {@code logback-test.xml}, żeby testy
 * nie walczyły o plik z katalogiem {@code log/} w repo. Plik jest dopisywany między przebiegami,
 * więc asercje celują w znacznik unikalny dla tego uruchomienia, a nie w samą obecność treści.
 *
 * <p>Bez uruchamiania toolkitu JavaFX, bez {@code @Tag("ui")}.
 */
class LoggingTest {

    /** Ścieżka z {@code logback-test.xml} — rozwiązywana względem katalogu roboczego przebiegu testów. */
    private static final Path PLIK_LOGU = Path.of("target", "test-logs", "mysmaug-test.log");

    /** Komplet polskich znaków w obu wielkościach — {@code ż} musi przeżyć jako {@code ż}, nie jako {@code z}. */
    private static final String DIAKRYTYKI = "ą ć ę ł ń ó ś ź ż Ą Ć Ę Ł Ń Ó Ś Ź Ż";

    @Test
    void wpisTrafiaDoPlikuZachowujacPolskieZnaki() throws IOException {
        String znacznik = "znacznik-przebiegu-" + System.nanoTime();
        Logger log = LoggerFactory.getLogger(LoggingTest.class);

        log.info("{} {}", znacznik, DIAKRYTYKI);

        assertThat(PLIK_LOGU)
                .as("Plik logu nie powstał — appender plikowy nie zadziałał albo pisze gdzie indziej niż %s",
                        PLIK_LOGU)
                .isRegularFile();

        String tresc = Files.readString(PLIK_LOGU, StandardCharsets.UTF_8);

        assertThat(tresc)
                .as("Plik logu istnieje, ale nie ma w nim wpisu z tego przebiegu — komunikat nie dojechał"
                        + " do appendera (poziom loggera? filtr? logger NOP?)")
                .contains(znacznik);
        assertThat(tresc)
                .as("Wpis jest, ale polskie diakrytyki się rozsypały — kodowanie zapisu albo odczytu"
                        + " nie jest UTF-8")
                .contains(DIAKRYTYKI);
    }
}
