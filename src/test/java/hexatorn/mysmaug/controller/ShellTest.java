package hexatorn.mysmaug.controller;

import hexatorn.mysmaug.app.MySmaugApplication;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy shella — pokrywają jedyną realną logikę, jaką dziś ma: leniwe ładowanie widoków
 * i przełączanie sekcji, oraz (Faza 5) pasek statusu na dole shella. Celują w ryzyko
 * „cicha regresja GUI: ekran przestaje się ładować po zmianie, wykryte tygodnie później".
 *
 * <p>Scena budowana przez {@link MySmaugApplication#createShellScene()}, czyli tę samą metodę,
 * której używa produkcja. Test omijający ten punkt pobiegłby inną ścieżką — bez wstrzykniętego
 * ThemeManagera — i przegapiłby klasę błędów, które trafiają do użytkownika.
 *
 * <p>Wymaga realnego ekranu; robot przejmuje kursor. Stąd {@code @Tag("ui")}.
 */
@Tag("ui")
@ExtendWith(ApplicationExtension.class)
class ShellTest {

    private static final String KLASA_AKTYWNEGO = "nav-button-active";

    private static final String ROOT = "#root";
    private static final String WPROWADZANIE = "#btnWprowadzanie";
    private static final String PODSUMOWANIA = "#btnPodsumowania";
    private static final String USTAWIENIA = "#btnUstawienia";
    private static final String STATUS = "#lblStatus";
    private static final String KLASA_BLEDU = "status-message-error";

    @BeforeAll
    static void wyciszZrzutyTestFx() {
        // TestFX domyślnie drukuje na stdout każdy wyjątek z wątku JavaFX, w trzech opakowaniach
        // i w pełnej długości, zanim test zdąży zareagować. Odkąd klik() sam go wyjmuje i zgłasza
        // jako porażkę, ten zrzut jest duplikatem — a przy tym spycha właściwy komunikat na środek
        // wyjścia builda, gdzie nikt go nie znajdzie.
        WaitForAsyncUtils.printException = false;
    }

    @Start
    void start(Stage stage) throws IOException {
        MySmaugApplication.configureStage(stage, MySmaugApplication.createShellScene());
        stage.show();
        // Robot klika w to, co jest na wierzchu ekranu — okno zasłonięte przez inne oddaje mu
        // kliknięcia i test pada, mimo że kod jest poprawny.
        stage.toFront();
    }

    @Test
    void shellWstajeZDomyslnaSekcjaWCentrum(FxRobot robot) {
        assertThat(root(robot).getCenter())
                .as("Shell wstał, ale środek jest pusty — initialize() nie pokazało domyślnej sekcji")
                .isNotNull();
        assertThat(przycisk(robot, WPROWADZANIE).getStyleClass())
                .as("Domyślna sekcja powinna być oznaczona jako aktywna")
                .contains(KLASA_AKTYWNEGO);
    }

    @Test
    void przelaczanieSekcjiPodmieniaWidokIPrzenosiKlaseAktywnego(FxRobot robot) {
        BorderPane root = root(robot);
        Node widokWprowadzania = root.getCenter();

        klik(robot, PODSUMOWANIA);
        Node widokPodsumowan = root.getCenter();
        assertThat(widokPodsumowan)
                .as("Klik w Podsumowania nie podmienił widoku w centrum")
                .isNotSameAs(widokWprowadzania);
        assertThat(przycisk(robot, PODSUMOWANIA).getStyleClass())
                .as("Kliknięty przycisk nie dostał klasy aktywnego")
                .contains(KLASA_AKTYWNEGO);
        assertThat(przycisk(robot, WPROWADZANIE).getStyleClass())
                .as("Poprzedni przycisk nie stracił klasy aktywnego")
                .doesNotContain(KLASA_AKTYWNEGO);

        klik(robot, USTAWIENIA);
        assertThat(root.getCenter())
                .as("Klik w Ustawienia nie podmienił widoku w centrum")
                .isNotSameAs(widokPodsumowan);
        assertThat(przycisk(robot, USTAWIENIA).getStyleClass())
                .as("Klasa aktywnego nie przeszła na Ustawienia")
                .contains(KLASA_AKTYWNEGO);
        assertThat(przycisk(robot, PODSUMOWANIA).getStyleClass())
                .as("Podsumowania powinny stracić klasę aktywnego")
                .doesNotContain(KLASA_AKTYWNEGO);

        // Powrót na odwiedzoną sekcję: porównanie tożsamości, nie równości — ten sam obiekt
        // dowodzi, że widok wyszedł z viewCache, a nie został załadowany po raz drugi.
        klik(robot, WPROWADZANIE);
        assertThat(root.getCenter())
                .as("Powrót na odwiedzoną sekcję załadował widok od nowa zamiast wziąć go z cache")
                .isSameAs(widokWprowadzania);
    }

    @Test
    void bladWypchnietyNaBelkeDostajeOdrebnaKlaseCss(FxRobot robot) {
        String komunikat = "awaria-testowa-" + System.nanoTime();

        robot.interact(() -> controller(robot).pokazBlad(komunikat));
        WaitForAsyncUtils.waitForFxEvents();

        Label status = etykietaStatusu(robot);
        assertThat(status.getText())
                .as("Komunikat błędu wypchnięty przez kontroler nie pojawił się na pasku statusu")
                .isEqualTo(komunikat);
        assertThat(status.getStyleClass())
                .as("Komunikat błędu nie dostał odrębnej klasy CSS odróżniającej go od zwykłego statusu")
                .contains(KLASA_BLEDU);
    }

    @Test
    void zwyklyKomunikatNaBelceNieDostajeKlasyBledu(FxRobot robot) {
        String komunikat = "status-testowy-" + System.nanoTime();

        robot.interact(() -> controller(robot).pokazStatus(komunikat));
        WaitForAsyncUtils.waitForFxEvents();

        Label status = etykietaStatusu(robot);
        assertThat(status.getText())
                .as("Zwykły komunikat wypchnięty przez kontroler nie pojawił się na pasku statusu")
                .isEqualTo(komunikat);
        assertThat(status.getStyleClass())
                .as("Zwykły komunikat nie powinien nieść klasy CSS zarezerwowanej dla błędów")
                .doesNotContain(KLASA_BLEDU);
    }

    @Test
    void komunikatInformacyjnyNiePrzykrywaAktywnegoBledu(FxRobot robot) {
        String blad = "awaria-priorytet-" + System.nanoTime();
        String status = "status-po-bledzie-" + System.nanoTime();

        robot.interact(() -> controller(robot).pokazBlad(blad));
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() -> controller(robot).pokazStatus(status));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(etykietaStatusu(robot).getText())
                .as("Komunikat informacyjny nadpisał aktywny błąd na pasku statusu")
                .isEqualTo(blad);
    }

    @Test
    void kolejnyBladPodczasAktywnegoDostajeLicznikWczesniejszych(FxRobot robot) {
        String pierwszy = "awaria-pierwsza-" + System.nanoTime();
        String drugi = "awaria-druga-" + System.nanoTime();

        robot.interact(() -> controller(robot).pokazBlad(pierwszy));
        WaitForAsyncUtils.waitForFxEvents();
        robot.interact(() -> controller(robot).pokazBlad(drugi));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(etykietaStatusu(robot).getText())
                .as("Drugi błąd podczas aktywnego pierwszego nie pokazał najnowszego komunikatu z licznikiem stłumionych")
                .isEqualTo(drugi + " (+1 wcześniejszych)");
    }

    private static BorderPane root(FxRobot robot) {
        return robot.lookup(ROOT).queryAs(BorderPane.class);
    }

    private static MainController controller(FxRobot robot) {
        return (MainController) root(robot).getScene().getUserData();
    }

    private static Label etykietaStatusu(FxRobot robot) {
        return robot.lookup(STATUS).queryAs(Label.class);
    }

    private static Button przycisk(FxRobot robot, String fxId) {
        return robot.lookup(fxId).queryAs(Button.class);
    }

    /** Klik plus bariera na kolejkę zdarzeń JavaFX — bez niej asercja może wyprzedzić handler. */
    private static void klik(FxRobot robot, String fxId) {
        robot.clickOn(fxId);
        WaitForAsyncUtils.waitForFxEvents();
        // Wyjątek rzucony w handlerze leci na wątku JavaFX i nigdy nie dociera do wątku testu —
        // TestFX odkłada go na własny stos. Bez tego wyciągnięcia test padłby dopiero na dalszej
        // asercji, mówiąc „widok się nie podmienił" i przemilczając powód.
        try {
            WaitForAsyncUtils.checkException();
        } catch (Throwable wyjatekZWatkuFx) {
            throw new AssertionError(
                    "Klik w " + fxId + " nie doszedł do skutku: " + przyczynaZrodlowa(wyjatekZWatkuFx),
                    wyjatekZWatkuFx);
        }
    }

    /**
     * Schodzi po łańcuchu przyczyn do korzenia. Warstwy pośrednie (InvocationTargetException,
     * opakowania FXMLLoadera) nic nie mówią — komunikat, który wskazuje winowajcę, siedzi na dnie.
     */
    private static Throwable przyczynaZrodlowa(Throwable wyjatek) {
        Throwable korzen = wyjatek;
        while (korzen.getCause() != null && korzen.getCause() != korzen) {
            korzen = korzen.getCause();
        }
        return korzen;
    }
}