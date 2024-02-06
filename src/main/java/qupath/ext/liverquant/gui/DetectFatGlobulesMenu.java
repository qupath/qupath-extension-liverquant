package qupath.ext.liverquant.gui;

import javafx.scene.control.MenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.gui.QuPathGUI;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * A menu item that starts a {@link DetectFatGlobulesWindow}.
 */
public class DetectFatGlobulesMenu extends MenuItem {

    private static final Logger logger = LoggerFactory.getLogger(DetectFatGlobulesMenu.class);
    private static final ResourceBundle resources = UiUtilities.getResources();
    private DetectFatGlobulesWindow detectFatGlobulesWindow;

    /**
     * <p>
     *     Create the menu. A {@link DetectFatGlobulesWindow} will be shown
     *     when this menu item is clicked.
     * </p>
     * <p>
     *     This menu item is disabled if no image is currently open.
     * </p>
     *
     * @param qupath  the QuPath GUI instance
     */
    public DetectFatGlobulesMenu(QuPathGUI qupath) {
        setText(resources.getString("DetectFatGlobulesMenu.title"));

        setOnAction(event -> {
            if (detectFatGlobulesWindow == null) {
                try {
                    detectFatGlobulesWindow = new DetectFatGlobulesWindow(qupath.getStage());
                } catch (IOException e) {
                    logger.error("Error while creating the detect fat globules window", e);
                }
            } else {
                UiUtilities.showWindow(detectFatGlobulesWindow);
            }
        });

        disableProperty().bind(qupath.imageDataProperty().isNull());
    }
}
