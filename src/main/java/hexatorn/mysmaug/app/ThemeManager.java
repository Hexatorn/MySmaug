package hexatorn.mysmaug.app;

import atlantafx.base.theme.Dracula;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.scene.Scene;

/**
 * Zarządza motywem aplikacji. Trzy odrębne motywy: Jasny (PrimerLight), Ciemny
 * (Dracula ≈ IntelliJ Darcula) i Fioletowy (PrimerLight + fioletowy akcent/sidebar).
 * Przy starcie motyw wybierany wg preferencji OS (Jasny/Ciemny); Fioletowy tylko ręcznie.
 */
public final class ThemeManager {

    public enum Theme { JASNY, CIEMNY, FIOLETOWY }

    private final Scene scene;
    private Theme theme;

    public ThemeManager(Scene scene) {
        this.scene = scene;
        // Start wg OS: ciemny motyw systemu → Ciemny, inaczej Jasny. Fioletowy nigdy automatycznie.
        boolean osDark = Platform.getPreferences().getColorScheme() == ColorScheme.DARK;
        apply(osDark ? Theme.CIEMNY : Theme.JASNY);
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        apply(theme);
    }

    /** Nakłada bazowy motyw AtlantaFX (user-agent stylesheet) i klasę motywu na root. */
    private void apply(Theme theme) {
        this.theme = theme;
        Application.setUserAgentStylesheet(switch (theme) {
            case JASNY     -> new PrimerLight().getUserAgentStylesheet();
            case CIEMNY    -> new Dracula().getUserAgentStylesheet();
            case FIOLETOWY -> new PrimerLight().getUserAgentStylesheet();
        });
        var classes = scene.getRoot().getStyleClass();
        classes.removeAll("theme-jasny", "theme-ciemny", "theme-fioletowy");
        classes.add(switch (theme) {
            case JASNY     -> "theme-jasny";
            case CIEMNY    -> "theme-ciemny";
            case FIOLETOWY -> "theme-fioletowy";
        });
    }
}