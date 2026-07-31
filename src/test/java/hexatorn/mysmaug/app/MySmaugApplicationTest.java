package hexatorn.mysmaug.app;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dowód, że granice sesji z Fazy 2 są realnie wpięte — {@code init()}/{@code stop()} faktycznie
 * wołają {@code Diagnostics}, a nie tylko wyglądają na wpięte przy czytaniu kodu. Wołane wprost
 * na instancji, bez {@code Application.launch()}: {@code init()} z definicji biegnie PRZED startem
 * toolkitu JavaFX, {@code stop()} tylko woła {@code Diagnostics} — żadne nie potrzebuje TestFX.
 *
 * <p>Od Fazy 3 {@code stop()} korzysta z pola {@code log} przypisywanego w {@code init()} (patrz
 * komentarz przy tym polu) — dokładnie tak, jak realny cykl życia JavaFX zawsze woła {@code init()}
 * przed {@code stop()}, więc test odwzorowuje tę kolejność zamiast wołać {@code stop()} w pełnej
 * izolacji od {@code init()}.
 *
 * <p>Bez {@code @Tag("ui")}. Pisze do {@code target/test-logs/}, tak jak {@code DiagnosticsTest}.
 */
class MySmaugApplicationTest {

    private static final Path PLIK_LOGU = Path.of("target", "test-logs", "mysmaug-test.log");

    @Test
    void initLogujeStartSesji() throws IOException {
        new MySmaugApplication().init();

        String tresc = Files.readString(PLIK_LOGU, StandardCharsets.UTF_8);

        assertThat(tresc)
                .as("init() nie zalogował startu sesji — granica startu z Fazy 2 nie jest realnie"
                        + " wpięta, tylko wygląda na wpiętą przy czytaniu kodu")
                .contains("START SESJI MYSMAUG");
    }

    @Test
    void stopLogujeKoniecSesji() throws IOException {
        MySmaugApplication aplikacja = new MySmaugApplication();
        aplikacja.init();
        aplikacja.stop();

        String tresc = Files.readString(PLIK_LOGU, StandardCharsets.UTF_8);

        assertThat(tresc)
                .as("stop() nie zalogował końca sesji — granica końca z Fazy 2 nie jest realnie wpięta")
                .contains("KONIEC SESJI MYSMAUG");
    }
}