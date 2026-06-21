package hexatorn.mysmaug.controller;

import atlantafx.base.controls.Popover;
import hexatorn.mysmaug.app.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Kontroler shella: trzyma BorderPane, leniwie ładuje i cache'uje widok każdej
 * sekcji, podmienia środek (center) na wybrany widok. Bez logiki domenowej —
 * to czysta nawigacja (F-05, Faza 1).
 */
public class MainController {

    /** Sekcje nawigacji; każda zna swój plik FXML (widok ładowany leniwie). */
    private enum Section {
        WPROWADZANIE("entry-view.fxml"),
        PODSUMOWANIA("summary-view.fxml"),
        USTAWIENIA("settings-view.fxml");

        private final String fxml;

        Section(String fxml) {
            this.fxml = fxml;
        }
    }

    @FXML
    private BorderPane root;
    @FXML
    private Button btnWprowadzanie;
    @FXML
    private Button btnPodsumowania;
    @FXML
    private Button btnUstawienia;
    @FXML
    private FontIcon btnMaksymalizuj;
    @FXML
    private Button btnMotyw;

    /** Manager motywu (Jasny/Ciemny/Fioletowy) — wstrzykiwany z MySmaugApplication po utworzeniu Scene. */
    private ThemeManager themeManager;

    /** Dymek z wyborem motywu (AtlantaFX Popover) — budowany leniwie przy pierwszym otwarciu. */
    private Popover motywPopover;

    /** Klasa CSS aktywnego buttona (Faza 2 — wyróżnienie aktywnej sekcji). */
    private static final String ACTIVE_CLASS = "nav-button-active";

    /** Cache widoków — każdy FXML ładowany najwyżej raz, potem reużywany. */
    private final Map<Section, Node> viewCache = new EnumMap<>(Section.class);

    /** Aktualnie pokazana sekcja — przygotowane pod toggle stanu aktywnego w Fazie 2. */
    private Section active;

    /** Offset kursora względem lewego-górnego rogu okna przy przeciąganiu paska tytułu. */
    private double dragOffsetX;
    private double dragOffsetY;

    @FXML
    private void initialize() {
        show(Section.WPROWADZANIE);
    }

    /** Wstrzyknięcie managera motywu z punktu wejścia (po utworzeniu Scene). */
    public void setThemeManager(ThemeManager themeManager) {
        this.themeManager = themeManager;
        // Start wg OS — etykieta selektora pokazuje motyw wybrany przez ThemeManager.
        btnMotyw.setText("Motyw: " + displayName(themeManager.getTheme()));
    }

    @FXML
    private void onActionWprowadzanie() {
        show(Section.WPROWADZANIE);
    }

    @FXML
    private void onActionPodsumowania() {
        show(Section.PODSUMOWANIA);
    }

    @FXML
    private void onActionUstawienia() {
        show(Section.USTAWIENIA);
    }

    @FXML
    private void onActionZamknij() {
        Platform.exit();
    }

    @FXML
    private void onActionMotyw() {
        if (motywPopover == null) {
            motywPopover = buildMotywPopover();
        }
        motywPopover.show(btnMotyw);
    }

    /** Buduje dymek z trzema opcjami motywu (sam tekst, bez ikon). */
    private Popover buildMotywPopover() {
        VBox box = new VBox(
                themeOption(ThemeManager.Theme.JASNY),
                themeOption(ThemeManager.Theme.CIEMNY),
                themeOption(ThemeManager.Theme.FIOLETOWY));
        box.getStyleClass().add("theme-popover");
        Popover popover = new Popover(box);
        popover.setArrowLocation(Popover.ArrowLocation.TOP_CENTER);
        popover.setDetachable(false);
        popover.setHeaderAlwaysVisible(false);
        return popover;
    }

    private Button themeOption(ThemeManager.Theme theme) {
        Button option = new Button(displayName(theme));
        option.getStyleClass().add("theme-option");
        option.setMaxWidth(Double.MAX_VALUE);
        option.setOnAction(e -> selectMotyw(theme));
        return option;
    }

    private void selectMotyw(ThemeManager.Theme theme) {
        themeManager.setTheme(theme);
        btnMotyw.setText("Motyw: " + displayName(theme));
        motywPopover.hide();
    }

    /** Nazwa motywu w UI — Ciemny prezentowany po palecie („Dracula"). */
    private String displayName(ThemeManager.Theme theme) {
        return switch (theme) {
            case JASNY     -> "Jasny";
            case CIEMNY    -> "Dracula";
            case FIOLETOWY -> "Fioletowy";
        };
    }

    @FXML
    private void onActionMinimalizuj() {
        stage().setIconified(true);
    }

    @FXML
    private void onActionMaksymalizuj() {
        Stage stage = stage();
        boolean maximize = !stage.isMaximized();
        stage.setMaximized(maximize);
        btnMaksymalizuj.setIconLiteral(maximize ? "mdi2w-window-restore" : "mdi2w-window-maximize");
    }

    @FXML
    private void onTitleBarPressed(MouseEvent event) {
        Stage stage = stage();
        dragOffsetX = event.getScreenX() - stage.getX();
        dragOffsetY = event.getScreenY() - stage.getY();
    }

    @FXML
    private void onTitleBarDragged(MouseEvent event) {
        Stage stage = stage();
        if (stage.isMaximized()) /* == true*/ {
            return;
        }
        stage.setX(event.getScreenX() - dragOffsetX);
        stage.setY(event.getScreenY() - dragOffsetY);
    }

    /** Stage shella — pobierany przez Scene korzenia (okno undecorated). */
    private Stage stage() {
        return (Stage) root.getScene().getWindow();
    }

    /** Pokazuje wskazaną sekcję — z cache lub ładując ją leniwie przy pierwszym wejściu. */
    private void show(Section section) {
        Node view = viewCache.computeIfAbsent(section, this::loadView);
        /*Ustawienie centralnego elementu BorderPane*/
        root.setCenter(view);
        markActive(section);
    }

    /** Przełącza klasę CSS aktywnego buttona: poprzedni traci, nowy dostaje. */
    private void markActive(Section section) {
        if (active != null) {
            buttonFor(active).getStyleClass().remove(ACTIVE_CLASS);
        }
        active = section;
        buttonFor(section).getStyleClass().add(ACTIVE_CLASS);
    }

    /** Mapuje sekcję na jej button w sidebarze. */
    private Button buttonFor(Section section) {
        return switch (section) {
            case WPROWADZANIE -> btnWprowadzanie;
            case PODSUMOWANIA -> btnPodsumowania;
            case USTAWIENIA   -> btnUstawienia;
        };
    }

    private Node loadView(Section section) {
        URL url = Objects.requireNonNull(
                MainController.class.getResource(section.fxml),
                "Brak zasobu FXML: " + section.fxml);
        try {
            return new FXMLLoader(url).load();
        } catch (IOException e) {
            throw new UncheckedIOException("Nie udało się załadować widoku: " + section.fxml, e);
        }
    }
}
