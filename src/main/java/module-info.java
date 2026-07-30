module hexatorn.mysmaug {
    requires javafx.controls;
    requires javafx.fxml;
    requires atlantafx.base;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.materialdesign2;

    requires org.slf4j;
    // Logbacka nie wskazuje ani jeden import — SLF4J znajduje implementację przez ServiceLoader.
    // Bez tej deklaracji graf modułów w ogóle jej nie wciągnie, a logowanie zamilknie BEZ błędu
    // kompilacji i bez wyjątku w czasie działania. Deklaracja celowo wskazuje implementację,
    // nie interfejs; alternatywa (--add-modules w konfiguracji uruchomieniowej) rozproszyłaby
    // tę wiedzę po pom.xml. Testy tego nie dowodzą — Maven dokłada czytelność zależności
    // testowych sam. Dowodzi tego dopiero uruchomienie aplikacji.
    requires ch.qos.logback.classic;


    exports hexatorn.mysmaug.app;
    opens hexatorn.mysmaug.controller to javafx.fxml;
    opens hexatorn.mysmaug.controller.view to javafx.fxml;
}