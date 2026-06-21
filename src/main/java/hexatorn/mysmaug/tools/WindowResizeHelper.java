package hexatorn.mysmaug.tools;

import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * Dodaje przeciąganie krawędzi/rogów okna undecorated — odtwarza natywny resize,
 * którego StageStyle.UNDECORATED nie zapewnia. Instaluje filtry na Scene: kursor
 * w strefie BORDER przy krawędzi przełącza kursor i pozwala ciągnięciem zmieniać rozmiar.
 */
public final class WindowResizeHelper {

    private static final double BORDER = 6;   // szerokość strefy chwytu przy krawędzi (px)
    private static final double MIN_W = 600;
    private static final double MIN_H = 400;

    private final Stage stage;
    private Cursor edge = Cursor.DEFAULT;      // krawędź wskazana przez kursor (zapamiętana na czas ciągnięcia)

    private WindowResizeHelper(Stage stage) {
        this.stage = stage;
    }

    public static void install(Stage stage, Scene scene) {
        WindowResizeHelper helper = new WindowResizeHelper(stage);
        // Filtry (faza przechwytywania) — wyprzedzają handlery węzłów, np. drag paska tytułu.
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> helper.updateCursor(scene, e));
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, helper::resize);
    }

    private void updateCursor(Scene scene, MouseEvent event) {
        if (stage.isMaximized()) /* == true*/ {
            edge = Cursor.DEFAULT;
            scene.setCursor(Cursor.DEFAULT);
            return;
        }
        boolean left = event.getSceneX() < BORDER;
        boolean right = event.getSceneX() > stage.getWidth() - BORDER;
        boolean top = event.getSceneY() < BORDER;
        boolean bottom = event.getSceneY() > stage.getHeight() - BORDER;

        if (top && left) edge = Cursor.NW_RESIZE;
        else if (top && right) edge = Cursor.NE_RESIZE;
        else if (bottom && left) edge = Cursor.SW_RESIZE;
        else if (bottom && right) edge = Cursor.SE_RESIZE;
        else if (left) edge = Cursor.W_RESIZE;
        else if (right) edge = Cursor.E_RESIZE;
        else if (top) edge = Cursor.N_RESIZE;
        else if (bottom) edge = Cursor.S_RESIZE;
        else edge = Cursor.DEFAULT;

        scene.setCursor(edge);
    }

    private void resize(MouseEvent event) {
        if (edge == Cursor.DEFAULT) {
            return;
        }
        double mx = event.getScreenX();
        double my = event.getScreenY();
        double maxX = stage.getX() + stage.getWidth();
        double maxY = stage.getY() + stage.getHeight();

        if (edge == Cursor.E_RESIZE || edge == Cursor.NE_RESIZE || edge == Cursor.SE_RESIZE) {
            stage.setWidth(Math.max(MIN_W, mx - stage.getX()));
        }
        if (edge == Cursor.S_RESIZE || edge == Cursor.SE_RESIZE || edge == Cursor.SW_RESIZE) {
            stage.setHeight(Math.max(MIN_H, my - stage.getY()));
        }
        // Lewa/górna krawędź: zmiana rozmiaru przesuwa też origin okna (X/Y).
        if (edge == Cursor.W_RESIZE || edge == Cursor.NW_RESIZE || edge == Cursor.SW_RESIZE) {
            double newWidth = maxX - mx;
            if (newWidth >= MIN_W) {
                stage.setX(mx);
                stage.setWidth(newWidth);
            }
        }
        if (edge == Cursor.N_RESIZE || edge == Cursor.NW_RESIZE || edge == Cursor.NE_RESIZE) {
            double newHeight = maxY - my;
            if (newHeight >= MIN_H) {
                stage.setY(my);
                stage.setHeight(newHeight);
            }
        }
        event.consume();   // nie pozwól, by drag krawędzi uruchomił też przeciąganie paska tytułu
    }
}