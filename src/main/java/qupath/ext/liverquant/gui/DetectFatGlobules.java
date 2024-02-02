package qupath.ext.liverquant.gui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class DetectFatGlobules extends Stage {

    @FXML
    private ChoiceBox<String> objectsCreated;

    public DetectFatGlobules(Stage owner) throws IOException {
        initUI(owner);
    }

    private void initUI(Stage owner) throws IOException {
        UiUtilities.loadFXML(this, DetectFatGlobules.class.getResource("detect_fat_globules.fxml"));

        objectsCreated.setItems(FXCollections.observableList(List.of("Annotations", "Detections")));
        objectsCreated.getSelectionModel().selectFirst();

        if (owner != null) {
            initOwner(owner);
        }

        show();
    }
}
