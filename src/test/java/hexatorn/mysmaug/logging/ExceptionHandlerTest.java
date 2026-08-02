package hexatorn.mysmaug.logging;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dowód, że handler nieobsłużonych wyjątków (Faza 4) zapisuje komplet informacji
 * potrzebnych do diagnozy — bez uruchamiania toolkitu JavaFX i bez faktycznego
 * rzucania wyjątku z wątku (handler wołany wprost, tak jak zrobiłby to
 * {@code Thread.UncaughtExceptionHandler}).
 *
 * <p>Pisze do {@code target/test-logs/}, wskazanego przez {@code logback-test.xml}.
 * Bez {@code @Tag("ui")}.
 */
class ExceptionHandlerTest {

    private static final Path PLIK_LOGU = Path.of("target", "test-logs", "mysmaug-test.log");

    @Test
    void handlerZapisujeNazweWatkuKomunikatIKlaseWyjatku() throws IOException {
        String komunikat = "awaria-testowa-" + System.nanoTime();
        RuntimeException wyjatek = new IllegalStateException(komunikat);
        Thread watek = new Thread(() -> { }, "watek-testowy-" + System.nanoTime());

        new ExceptionHandler().uncaughtException(watek, wyjatek);

        String tresc = Files.readString(PLIK_LOGU, StandardCharsets.UTF_8);
        assertThat(tresc)
                .as("Brak nazwy wątku w wpisie o nieobsłużonym wyjątku")
                .contains(watek.getName());
        assertThat(tresc)
                .as("Brak komunikatu wyjątku w logu")
                .contains(komunikat);
        assertThat(tresc)
                .as("Brak nazwy klasy wyjątku w logu")
                .contains(IllegalStateException.class.getName());
    }

    @Test
    void handlerZapisujeStacktraceIPrzyczyneZrodlowa() throws IOException {
        String komunikatPrzyczyny = "przyczyna-zrodlowa-" + System.nanoTime();
        Exception przyczyna = new IOException(komunikatPrzyczyny);
        RuntimeException wyjatek = new RuntimeException("opakowanie-zewnetrzne", przyczyna);
        Thread watek = new Thread(() -> { }, "watek-przyczyna-" + System.nanoTime());

        new ExceptionHandler().uncaughtException(watek, wyjatek);

        String tresc = Files.readString(PLIK_LOGU, StandardCharsets.UTF_8);
        assertThat(tresc)
                .as("Brak linii stacktrace'u — wpis nie niesie pełnego śladu wywołań. Uwaga: build modularny"
                        + " (JPMS) dopisuje przed nazwą klasy prefiks modułu (\"hexatorn.mysmaug@.../\"),"
                        + " więc sprawdzamy samą obecność linii \"at\" i nazwy klasy, nie ich bezpośrednie"
                        + " sąsiedztwo")
                .contains("\tat ")
                .contains(ExceptionHandlerTest.class.getName());
        assertThat(tresc)
                .as("Brak przyczyny źródłowej w logu — zapisano wyłącznie zewnętrzne opakowanie,"
                        + " nie IOException spod spodu")
                .contains(IOException.class.getName())
                .contains(komunikatPrzyczyny);
    }
}
