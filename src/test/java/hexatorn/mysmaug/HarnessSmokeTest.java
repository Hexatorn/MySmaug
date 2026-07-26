package hexatorn.mysmaug;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke runnera — stały element harnessu (F-01), nie tymczasowy spike.
 * Nie testuje domeny. Jego jedyną rolą jest dowód, że runner żyje: testy są
 * znajdowane, wykonywane, a porażka łamie build. Gdy ten test zamilknie albo
 * zniknie z raportu, zepsuł się sam harness, a nie kod aplikacji.
 * Bez {@code @Tag("ui")} — zero kontaktu z JavaFX.
 */
class HarnessSmokeTest {

    @Test
    void harnessUruchamiaSie() {
        assertThat(1 + 1).isEqualTo(2);
    }
}