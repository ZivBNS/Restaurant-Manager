package utils;

import javafx.scene.Parent;
import javafx.scene.Scene;

/**
 * Helper for creating Scenes with the application's theme applied.
 */
public class SceneUtils {

    /**
     * Create a new Scene with the theme stylesheets attached.
     * @param root root node
     * @return Scene with theme applied
     */
    public static Scene createStyledScene(Parent root) {
        // Remove any stylesheets that the FXML may have specified on the root
        try {
            if (root != null && !root.getStylesheets().isEmpty()) {
                root.getStylesheets().clear();
            }
        } catch (Exception ignored) {}

        Scene scene = new Scene(root);
        applyTheme(scene);
        return scene;
    }

    /**
     * Apply the application theme stylesheets to a Scene.
     * Loads base application.css first, then application_modern.css so modern rules override.
     */
    public static void applyTheme(Scene scene) {
        if (scene == null) return;
        try {
            java.net.URL baseCss = SceneUtils.class.getResource("/Theme/application.css");
            if (baseCss != null) {
                scene.getStylesheets().add(baseCss.toExternalForm());
            }
        } catch (Exception ignored) {
        }
        try {
            java.net.URL modernCss = SceneUtils.class.getResource("/Theme/application_modern.css");
            if (modernCss != null) {
                scene.getStylesheets().add(modernCss.toExternalForm());
            }
        } catch (Exception ignored) {
        }
    }
}
