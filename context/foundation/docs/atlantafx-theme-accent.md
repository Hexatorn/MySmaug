# AtlantaFX — instalacja themu + akcent

> Pelny zrzut z context7. ID: `/mkpaz/atlantafx`. Data: 2026-06-15.
> Zapytanie: "install theme setUserAgentStylesheet, set global accent color, light dark theme switching".
> Destylacja i wnioski: `context/changes/view-navigation-shell/research.md` (pkt 6).

---

### Setting AtlantaFX Themes Programmatically

Source: https://github.com/mkpaz/atlantafx/blob/master/docs/docs/getting-started.md

This Java code demonstrates how to set an AtlantaFX theme when launching a JavaFX application. You can choose between light and dark themes like PrimerLight or PrimerDark.

```java
public class Launcher extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // find more themes in 'atlantafx.base.theme' package
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // the rest of the code ...
    }
```

--------------------------------

### Setting AtlantaFX Theme in JavaFX Application

Source: https://github.com/mkpaz/atlantafx/blob/master/README.md

This Java code demonstrates how to set an AtlantaFX theme for a JavaFX application. It shows how to instantiate a theme class (e.g., PrimerLight or PrimerDark) and apply its stylesheet using `Application.setUserAgentStylesheet()`.

```java
import javafx.application.Application;
import javafx.stage.Stage;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.PrimerDark;

public class Launcher extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // find more themes in 'atlantafx.base.theme' package
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // the rest of the code ...
    }
}
```

--------------------------------

### AtlantaFX Accent and Status Colors

Source: https://github.com/mkpaz/atlantafx/blob/master/docs/docs/reference/global-colors.md

Provides accent and status-related color variables in AtlantaFX, including neutral, accent, success, warning, and danger color sets. These are used to convey specific meanings or highlight content.

```css
/* Neutral Colors */
--color-neutral-emphasis-plus: /* ... */;
--color-neutral-emphasis: /* ... */;
--color-neutral-muted: /* ... */;
--color-neutral-subtle: /* ... */;

/* Accent Colors */
--color-accent-fg: /* ... */;
--color-accent-emphasis: /* ... */;
--color-accent-muted: /* ... */;
--color-accent-subtle: /* ... */;

/* Success Colors */
--color-success-fg: /* ... */;
--color-success-emphasis: /* ... */;
--color-success-muted: /* ... */;
--color-success-subtle: /* ... */;

/* Attention Colors */
--color-warning-fg: /* ... */;
--color-warning-emphasis: /* ... */;
--color-warning-muted: /* ... */;
--color-warning-subtle: /* ... */;

/* Danger Colors */
--color-danger-fg: /* ... */;
--color-danger-emphasis: /* ... */;
--color-danger-muted: /* ... */;
--color-danger-subtle: /* ... */;
```

--------------------------------

### Styles Theme Constants

Source: https://github.com/mkpaz/atlantafx/blob/master/docs/docs/apidocs/constant-values.html

Provides constants for various style properties used in the Atlantafx theme, including accent colors and background variations. These are fundamental for theming UI components.

```java
public static final String ACCENT = "accent";
public static final String BG_ACCENT_EMPHASIS = "bg-accent-emphasis";
public static final String BG_ACCENT_MUTED = "bg-accent-muted";
public static final String BG_ACCENT_SUBTLE = "bg-accent-subtle";
public static final String BG_DANGER_EMPHASIS = "bg-danger-emphasis";
```

--------------------------------

### Applying Custom Theme Pseudo-Class in Java

Source: https://github.com/mkpaz/atlantafx/blob/master/docs/docs/theming.md

Shows how to declare and apply a custom pseudo-class to a JavaFX node to change its theme, typically used for theme switching.

```java
// declare pseudo-class
private static PseudoClass CUSTOM_THEME = PseudoClass.getPseudoClass("custom-theme");
// then apply it to the root node
getScene().getRoot().pseudoClassStateChanged(CUSTOM_THEME, true);
```
