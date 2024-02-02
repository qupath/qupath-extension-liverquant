package qupath.ext.liverquant.gui;

import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class UiUtilities {

    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.liverquant.strings");

    private UiUtilities() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Loads the FXML file located at the URL and set its controller.
     *
     * @param controller  the controller of the FXML file to load
     * @param url  the path of the FXML file to load
     * @throws IOException if an error occurs while loading the FXML file
     */
    public static void loadFXML(Object controller, URL url) throws IOException {
        FXMLLoader loader = new FXMLLoader(url, resources);
        loader.setRoot(controller);
        loader.setController(controller);
        loader.load();
    }

    /**
     * @return the resources containing the localized strings
     */
    public static ResourceBundle getResources() {
        return resources;
    }

    /**
     * Show a window. The focus is also set to it.
     *
     * @param window  the window to show
     */
    public static void showWindow(Stage window) {
        window.show();
        window.requestFocus();

        // This is necessary to avoid a bug on Linux
        // that resets the window size
        window.setWidth(window.getWidth() + 1);
        window.setHeight(window.getHeight() + 1);
    }
}
