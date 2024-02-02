package qupath.ext.liverquant.gui;

import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DetectFatGlobulesCommand implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(DetectFatGlobulesCommand.class);
    private final Stage owner;
    private DetectFatGlobules detectFatGlobules;

    public DetectFatGlobulesCommand(Stage owner) {
        this.owner = owner;
    }

    @Override
    public void run() {
        if (detectFatGlobules == null) {
            try {
                detectFatGlobules = new DetectFatGlobules(owner);
            } catch (IOException e) {
                logger.error("Error while creating the detect fat globules window", e);
            }
        } else {
            UiUtilities.showWindow(detectFatGlobules);
        }
    }
}
