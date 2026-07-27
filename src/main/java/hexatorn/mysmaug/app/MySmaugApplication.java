package hexatorn.mysmaug.app;

import hexatorn.mysmaug.controller.MainController;
import hexatorn.mysmaug.tools.ThemeManager;
import hexatorn.mysmaug.tools.WindowResizeHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class MySmaugApplication extends Application {

    private static final String FXML_SHELL = "/hexatorn/mysmaug/controller/main-view.fxml";
    private static final String IKONA_APLIKACJI = "/hexatorn/mysmaug/app-icon.png";

    /**
     * Składa scenę shella: wczytuje main-view.fxml i wstrzykuje ThemeManager do kontrolera.
     * Wołane przez start() oraz przez testy widoku, żeby jedne i drugie szły tą samą ścieżką.
     */
    public static Scene createShellScene() throws IOException {
        URL fxmlUrl = Objects.requireNonNull(
                MySmaugApplication.class.getResource(FXML_SHELL),
                "Brak zasobu FXML: " + FXML_SHELL);
        FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
        // Motyw (AtlantaFX light/dark) wstrzykiwany do kontrolera — ThemeManager nakłada
        // user-agent stylesheet i pilnuje auto light/dark wg motywu OS.
        MainController controller = fxmlLoader.getController();
        controller.setThemeManager(new ThemeManager(scene));
        return scene;
    }

    /**
     * Konfiguruje okno: własne chrome, tytuł, ikona, obsługa zmiany rozmiaru. Bez show() —
     * o momencie pokazania decyduje wołający. Testy widoku przechodzą tędy tak samo jak
     * produkcja, żeby okno w teście powstawało dokładnie tak, jak u użytkownika.
     */
    public static void configureStage(Stage stage, Scene scene) {
        // Zdejmuje systemową belkę tytułu i ramkę okna — własny pasek tytułu rysujemy w main-view.fxml.
        // Strażnik idempotencji: JavaFX zabrania zmiany stylu okna po pierwszym pokazaniu, a TestFX
        // wywołuje przygotowanie sceny osobno dla każdego testu na tym samym Stage'u.
        if (stage.getStyle() != StageStyle.UNDECORATED) {
            stage.initStyle(StageStyle.UNDECORATED);
        }
        stage.setTitle("MySmaug");
        // Ikona aplikacji opcjonalna — wczytujemy tylko, gdy plik istnieje.
        URL iconUrl = MySmaugApplication.class.getResource(IKONA_APLIKACJI);
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }
        stage.setScene(scene);
        WindowResizeHelper.install(stage, scene);
    }

    @Override
    public void start(Stage stage) throws IOException {
        configureStage(stage, createShellScene());
        stage.show();
    }
}
