package hexatorn.mysmaug.app;

import hexatorn.mysmaug.controller.MainController;
import hexatorn.mysmaug.logging.Diagnostics;
import hexatorn.mysmaug.logging.ExceptionHandler;
import hexatorn.mysmaug.logging.LogRotation;
import hexatorn.mysmaug.tools.ThemeManager;
import hexatorn.mysmaug.tools.WindowResizeHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class MySmaugApplication extends Application {

    private static final String FXML_SHELL = "/hexatorn/mysmaug/controller/main-view.fxml";
    private static final String IKONA_APLIKACJI = "/hexatorn/mysmaug/app-icon.png";
    private static final Path KATALOG_LOGOW = Path.of("log");
    private static final Path PLIK_LOGU = KATALOG_LOGOW.resolve("mysmaug.log");
    private static final double PROG_ROTACJI_MB = 1.0;
    private static final int LIMIT_ARCHIWALNYCH = 2;

    // Nie static-final z inline-inicjalizacją: LoggerFactory.getLogger(...) wiąże Logbacka i otwiera
    // plik logu w tym samym momencie (Faza 1). Pole musi zostać przypisane w init() PO sondzie
    // rotacji — inaczej rotacja próbowałaby przenieść plik, który appender ma już otwarty do zapisu.
    private Logger log;

    // Jedna instancja dla obu punktów wpięcia (domyślny handler JVM i handler wątku FX) — Faza 6
    // doda tłumienie ze stanem per typ wyjątku, które musi być współdzielone między oboma wpięciami.
    private final Thread.UncaughtExceptionHandler handlerWyjatkow = new ExceptionHandler();

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

    /**
     * Granica startu sesji — wołane przed toolkitem JavaFX, więc sceny jeszcze nie ma.
     * Pokrywa oba punkty wejścia (Launcher i javafx:run), bo oba wołają launch(MySmaugApplication).
     */
    @Override
    public void init() {
        sondujKatalogLogow();
        // Czyste I/O, bez wywołania SLF4J — musi paść przed poniższym getLogger(...), inaczej
        // Logback zdąży otworzyć plik logu pierwszy (patrz komentarz przy polu log).
        LogRotation.obrocJesliZaDuzy(PLIK_LOGU, PROG_ROTACJI_MB, LIMIT_ARCHIWALNYCH);
        log = LoggerFactory.getLogger(MySmaugApplication.class);
        Diagnostics.zalogujNaglowekStartowy(log);
        // Pokrywa wątki bez własnego handlera, na obu punktach wejścia (Launcher i javafx:run) —
        // patrz też jawne wpięcie na wątku FX Application Thread w start().
        Thread.setDefaultUncaughtExceptionHandler(handlerWyjatkow);
    }

    @Override
    public void start(Stage stage) throws IOException {
        // JavaFX obsługuje wyjątki z kolejki zdarzeń własną ścieżką i domyślny handler z init()
        // nie zawsze go łapie — jawne wpięcie na samym wątku FX Application Thread, na którym
        // faktycznie teraz jesteśmy (init() biegnie wcześniej, na osobnym wątku JavaFX-Launcher,
        // zanim ten wątek w ogóle powstanie).
        Thread.currentThread().setUncaughtExceptionHandler(handlerWyjatkow);
        configureStage(stage, createShellScene());
        stage.show();
    }

    /**
     * Granica końca sesji — JavaFX woła stop() przy każdym wyjściu przez Platform.exit(),
     * więc pokrywa obie dzisiejsze ścieżki zamknięcia (pasek tytułu i sidebar).
     */
    @Override
    public void stop() {
        Diagnostics.zalogujZakonczenieSesji(log);
    }

    /**
     * Zapewnia istnienie katalogu logów przed startem logowania. Porażka nie może przerwać
     * startu aplikacji — Logback i tak milczy funkcjonalnie przy nieudanym zapisie (nie rzuca),
     * więc na tym etapie nie ma jeszcze komu zgłosić błędu. Zapamiętanie wyniku i pokazanie
     * go użytkownikowi należy do Fazy 7 (StanLogowania) — tu tylko sonda.
     */
    private static void sondujKatalogLogow() {
        try {
            Files.createDirectories(KATALOG_LOGOW);
        } catch (IOException e) {
            // Świadomie połknięte — patrz komentarz metody.
        }
    }
}
