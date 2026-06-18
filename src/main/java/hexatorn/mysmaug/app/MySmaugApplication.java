package hexatorn.mysmaug.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MySmaugApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MySmaugApplication.class.getResource("/hexatorn/mysmaug/controller/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
        stage.setTitle("MySmaug");
        stage.setScene(scene);
        stage.show();
    }
}
