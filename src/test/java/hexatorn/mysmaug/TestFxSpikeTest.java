package hexatorn.mysmaug;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.testfx.assertions.api.Assertions.assertThat;

/**
 * SPIKE — klasa tymczasowa, znika w Fazie 3. Jej jedynym zadaniem jest rozstrzygnąć
 * niewiadomą tej zmiany: czy TestFX 4.0.18 (ostatnie wydanie, luty 2024, oficjalne
 * wsparcie do JavaFX 21) uruchomi się na JavaFX 25, Javie 23 i module-path.
 *
 * <p>Dowodzone na najprostszym możliwym przypadku: własny {@code Stage}, jeden
 * {@code Button}, zero FXML i zero kontaktu z {@code MySmaugApplication}. Dzięki temu
 * czerwień oznacza problem z biblioteką albo z flagami modułowymi, a nie z shellem
 * aplikacji. Sprawdzany jest pełen łańcuch: toolkit startuje, okno się pokazuje, robot
 * klika, asercja czyta scene-graph.
 *
 * <p>Klasa leży świadomie w pakiecie {@code hexatorn.mysmaug}, który obecny wpis
 * {@code add-opens} w {@code module-info-patch.maven} już pokrywa. Nowy pakiet
 * wprowadziłby drugą niewiadomą naraz i nie dałoby się orzec, co zawiodło.
 *
 * <p>Wymaga <b>realnej sesji graficznej</b> — otwiera widoczne okno i przejmuje kursor.
 * Monocle nie ma buildu dla Javy 23/JFX 25, więc headless odpada do czasu JavaFX 26.
 * Stąd {@code @Tag("ui")} i możliwość pominięcia przez {@code -DexcludedGroups=ui}.
 */
@Tag("ui")
@ExtendWith(ApplicationExtension.class)
class TestFxSpikeTest {

    private static final String ID_PRZYCISKU = "#przyciskDowodowy";
    private static final String PRZED_KLIKNIECIEM = "Kliknij mnie";
    private static final String PO_KLIKNIECIU = "Kliknięto";

    /** Buduje scenę na Stage'u dostarczonym przez TestFX i pokazuje okno. */
    @Start
    void start(Stage stage) {
        Button przycisk = new Button(PRZED_KLIKNIECIEM);
        przycisk.setId("przyciskDowodowy");
        przycisk.setOnAction(zdarzenie -> przycisk.setText(PO_KLIKNIECIU));
        stage.setScene(new Scene(new StackPane(przycisk), 300, 200));
        stage.show();
    }

    @Test
    void robotKlikaWPrzyciskAScenaReaguje(FxRobot robot) {
        robot.clickOn(ID_PRZYCISKU);
        // Bariera na kolejkę zdarzeń JavaFX — bez niej odczyt z wątku testu może wyprzedzić handler.
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(robot.lookup(ID_PRZYCISKU).queryAs(Button.class))
                .hasText(PO_KLIKNIECIU);
    }
}