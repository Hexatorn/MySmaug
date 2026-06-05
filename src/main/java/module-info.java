module hexatorn.mysmaug {
    requires javafx.controls;
    requires javafx.fxml;


    opens hexatorn.mysmaug to javafx.fxml;
    exports hexatorn.mysmaug;
}