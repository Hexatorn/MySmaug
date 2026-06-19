package hexatorn.mysmaug.app;

import atlantafx.base.theme.PrimerLight;
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
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        FXMLLoader fxmlLoader = new FXMLLoader(MySmaugApplication.class.getResource("/hexatorn/mysmaug/controller/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
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
