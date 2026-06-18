module hexatorn.mysmaug {
    requires javafx.controls;
    requires javafx.fxml;


    exports hexatorn.mysmaug.app;
    opens hexatorn.mysmaug.controller to javafx.fxml;
}