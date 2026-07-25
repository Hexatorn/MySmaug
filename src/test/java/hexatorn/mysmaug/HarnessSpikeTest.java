package hexatorn.mysmaug;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SPIKE — tymczasowa klasa do weryfikacji, czy harness testowy w ogóle wstaje.
 * Nie testuje niczego z domeny (domeny jeszcze nie ma). Do usunięcia lub
 * zastąpienia przy implementacji planu F-01.
 */
class HarnessSpikeTest {

    @Test
    void harnessUruchamiaSie() {
        assertEquals(2, 1 + 1);
    }
}