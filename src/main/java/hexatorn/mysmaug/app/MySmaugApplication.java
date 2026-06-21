package hexatorn.mysmaug.app;

import hexatorn.mysmaug.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;

public class MySmaugApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Zdejmuje systemową belkę tytułu i ramkę okna — własny pasek tytułu rysujemy w main-view.fxml.
        stage.initStyle(StageStyle.UNDECORATED);
        FXMLLoader fxmlLoader = new FXMLLoader(MySmaugApplication.class.getResource("/hexatorn/mysmaug/controller/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
        // Motyw (AtlantaFX light/dark) wstrzykiwany do kontrolera — ThemeManager nakłada
        // user-agent stylesheet i pilnuje auto light/dark wg motywu OS.
        MainController controller = fxmlLoader.getController();
        controller.setThemeManager(new ThemeManager(scene));
        stage.setTitle("MySmaug");
        // Ikona aplikacji opcjonalna — wczytujemy tylko, gdy plik istnieje (dodawany później).
        URL iconUrl = MySmaugApplication.class.getResource("/hexatorn/mysmaug/app-icon.png");
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }
        stage.setScene(scene);
        WindowResizeHelper.install(stage, scene);
        stage.show();
    }
}
