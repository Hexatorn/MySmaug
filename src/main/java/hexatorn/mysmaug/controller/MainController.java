package hexatorn.mysmaug.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

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

    /** Cache widoków — każdy FXML ładowany najwyżej raz, potem reużywany. */
    private final Map<Section, Node> viewCache = new EnumMap<>(Section.class);

    /** Aktualnie pokazana sekcja — przygotowane pod toggle stanu aktywnego w Fazie 2. */
    private Section active;

    @FXML
    private void initialize() {
        show(Section.WPROWADZANIE);
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

    /** Pokazuje wskazaną sekcję — z cache lub ładując ją leniwie przy pierwszym wejściu. */
    private void show(Section section) {
        Node view = viewCache.computeIfAbsent(section, this::loadView);
        /*Ustawienie centralnego elementu BorderPane*/
        root.setCenter(view);
        active = section;
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
