module hexatorn.mysmaug {
    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;


    exports hexatorn.mysmaug.app;
    opens hexatorn.mysmaug.controller to javafx.fxml;
    opens hexatorn.mysmaug.controller.view to javafx.fxml;
}