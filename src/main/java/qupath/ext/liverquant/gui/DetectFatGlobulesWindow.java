package qupath.ext.liverquant.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import qupath.ext.liverquant.core.FatGlobuleDetector;
import qupath.ext.liverquant.core.FatGlobulesDetectorParameters;
import qupath.lib.gui.QuPathGUI;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * A window to input parameters and run a {@link FatGlobuleDetector}.
 */
public class DetectFatGlobulesWindow extends Stage {

    private static final ResourceBundle resources = UiUtilities.getResources();
    @FXML
    private ChoiceBox<FatGlobulesDetectorParameters.ObjectToCreate> objectsToCreate;
    @FXML
    private TextField pixelSize;
    @FXML
    private TextField lowerHue;
    @FXML
    private TextField lowerSaturation;
    @FXML
    private TextField lowerValue;
    @FXML
    private TextField upperHue;
    @FXML
    private TextField upperSaturation;
    @FXML
    private TextField upperValue;
    @FXML
    private TextField minIsolatedGlobuleElongation;
    @FXML
    private TextField minOverlappingGlobuleElongation;
    @FXML
    private TextField minIsolatedGlobuleSolidity;
    @FXML
    private TextField minOverlappingGlobuleSolidity;
    @FXML
    private TextField minDiameter;
    @FXML
    private TextField maxDiameter;
    @FXML
    private TextField tileWidth;
    @FXML
    private TextField tileHeight;
    @FXML
    private TextField padding;
    @FXML
    private TextField boundaryThreshold;
    @FXML
    private Button run;

    /**
     * Create the FatGlobuleWindow.
     *
     * @param owner  the stage that should own this window
     * @throws IOException if an error occurs while creating the window
     */
    public DetectFatGlobulesWindow(Stage owner) throws IOException {
        initUI(owner);

        //TODO: add tooltips
        //TODO: check input (e.g. 0 < hue < 180)
        //TODO: strings fr
        //TODO: Groovy much faster
    }

    @FXML
    private void run(ActionEvent ignoredEvent) {
        QuPathGUI quPathGUI = QuPathGUI.getInstance();

        ImageData<BufferedImage> imageData = quPathGUI.getImageData();
        if (imageData == null) {
            Dialogs.showErrorMessage(
                    resources.getString("DetectFatGlobulesWindow.liverquant"),
                    resources.getString("DetectFatGlobulesWindow.imageMustBeOpened")
            );
            return;
        }

        List<PathObject> selectedAnnotations = quPathGUI.getViewer().getAllSelectedObjects().stream().filter(PathObject::isAnnotation).toList();
        if (selectedAnnotations.isEmpty()) {
            Dialogs.showErrorMessage(
                    resources.getString("DetectFatGlobulesWindow.liverquant"),
                    resources.getString("DetectFatGlobulesWindow.selectAtLeastOneAnnotation")
            );
            return;
        }

        run.setDisable(true);
        FatGlobuleDetector.run(new FatGlobulesDetectorParameters.Builder(imageData, selectedAnnotations)
                .setProgressDisplay(FatGlobulesDetectorParameters.ProgressDisplay.WINDOW)
                .setObjectToCreate(objectsToCreate.getSelectionModel().getSelectedItem())
                .setPixelSize(Float.parseFloat(pixelSize.getText()))
                .setLowerBound(new FatGlobulesDetectorParameters.HsvArray(
                        Integer.parseInt(lowerHue.getText()),
                        Integer.parseInt(lowerSaturation.getText()),
                        Integer.parseInt(lowerValue.getText())
                ))
                .setUpperBound(new FatGlobulesDetectorParameters.HsvArray(
                        Integer.parseInt(upperHue.getText()),
                        Integer.parseInt(upperSaturation.getText()),
                        Integer.parseInt(upperValue.getText())
                ))
                .setMinIsolatedGlobuleElongation(Float.parseFloat(minIsolatedGlobuleElongation.getText()))
                .setMinOverlappingGlobuleElongation(Float.parseFloat(minOverlappingGlobuleElongation.getText()))
                .setMinIsolatedGlobuleSolidity(Float.parseFloat(minIsolatedGlobuleSolidity.getText()))
                .setMinOverlappingGlobuleSolidity(Float.parseFloat(minOverlappingGlobuleSolidity.getText()))
                .setMinDiameter(Float.parseFloat(minDiameter.getText()))
                .setMaxDiameter(Float.parseFloat(maxDiameter.getText()))
                .setTileWidth(Integer.parseInt(tileWidth.getText()))
                .setTileHeight(Integer.parseInt(tileHeight.getText()))
                .setPadding(Integer.parseInt(padding.getText()))
                .setBoundaryThreshold(Float.parseFloat(boundaryThreshold.getText()))
                .setOnFinished(() -> Platform.runLater(() -> {
                    run.setDisable(false);
                    quPathGUI.getViewer().getHierarchy().resolveHierarchy();
                }))
                .build()
        );
    }

    private void initUI(Stage owner) throws IOException {
        UiUtilities.loadFXML(this, DetectFatGlobulesWindow.class.getResource("detect_fat_globules.fxml"));

        FatGlobulesDetectorParameters defaultParameters = new FatGlobulesDetectorParameters.Builder(null, null).build();

        Pattern unsignerIntegerPattern = Pattern.compile("\\d*");
        UnaryOperator<TextFormatter.Change> unsignedIntegerFilter = change ->
                unsignerIntegerPattern.matcher(change.getControlNewText()).matches() ? change : null;

        Pattern unsignerFloatPattern = Pattern.compile("\\d*\\.?\\d*");
        UnaryOperator<TextFormatter.Change> unsignedFloatFilter = change ->
                unsignerFloatPattern.matcher(change.getControlNewText()).matches() ? change : null;

        objectsToCreate.setItems(FXCollections.observableList(List.of(FatGlobulesDetectorParameters.ObjectToCreate.values())));
        objectsToCreate.setConverter(new StringConverter<>() {
            @Override
            public String toString(FatGlobulesDetectorParameters.ObjectToCreate object) {
                return switch (object) {
                    case ANNOTATION -> resources.getString("DetectFatGlobulesWindow.annotation");
                    case DETECTION -> resources.getString("DetectFatGlobulesWindow.detection");
                };
            }

            @Override
            public FatGlobulesDetectorParameters.ObjectToCreate fromString(String string) {
                return null;
            }
        });
        objectsToCreate.getSelectionModel().select(defaultParameters.getObjectToCreate());
        pixelSize.setText(String.valueOf(defaultParameters.getPixelSize()));
        pixelSize.setTextFormatter(new TextFormatter<>(unsignedFloatFilter));

        lowerHue.setText(String.valueOf(defaultParameters.getLowerBound().hue()));
        lowerSaturation.setText(String.valueOf(defaultParameters.getLowerBound().saturation()));
        lowerValue.setText(String.valueOf(defaultParameters.getLowerBound().value()));
        upperHue.setText(String.valueOf(defaultParameters.getUpperBound().hue()));
        upperSaturation.setText(String.valueOf(defaultParameters.getUpperBound().saturation()));
        upperValue.setText(String.valueOf(defaultParameters.getUpperBound().value()));
        lowerHue.setTextFormatter(new TextFormatter<>(unsignedIntegerFilter));
        lowerSaturation.setTextFormatter(new TextFormatter<>(unsignedIntegerFilter));
        lowerValue.setTextFormatter(new TextFormatter<>(unsignedIntegerFilter));
        upperHue.setTextFormatter(new TextFormatter<>(unsignedIntegerFilter));
        upperSaturation.setTextFormatter(new TextFormatter<>(unsignedIntegerFilter));
        upperValue.setTextFormatter(new TextFormatter<>(unsignedIntegerFilter));

        minIsolatedGlobuleElongation.setText(String.valueOf(defaultParameters.getMinIsolatedGlobuleElongation()));
        minOverlappingGlobuleElongation.setText(String.valueOf(defaultParameters.getMinOverlappingGlobuleElongation()));
        minIsolatedGlobuleSolidity.setText(String.valueOf(defaultParameters.getMinIsolatedGlobuleSolidity()));
        minOverlappingGlobuleSolidity.setText(String.valueOf(defaultParameters.getMinIsolatedGlobuleSolidity()));
        minDiameter.setText(String.valueOf(defaultParameters.getMinDiameter()));
        maxDiameter.setText(String.valueOf(defaultParameters.getMaxDiameter()));
        minIsolatedGlobuleElongation.setTextFormatter(new TextFormatter<>(unsignedFloatFilter));
        minOverlappingGlobuleElongation.setTextFormatter(new TextFormatter<>(unsignedFloatFilter));
        minIsolatedGlobuleSolidity.setTextFormatter(new TextFormatter<>(unsignedFloatFilter));
        minOverlappingGlobuleSolidity.setTextFormatter(new TextFormatter<>(unsignedFloatFilter));
        minDiameter.setTextFormatter(new TextFormatter<>(unsignedFloatFilter));
        maxDiameter.setTextFormatter(new TextFormatter<>(unsignedFloatFilter));

        tileWidth.setText(String.valueOf(defaultParameters.getTileWidth()));
        tileHeight.setText(String.valueOf(defaultParameters.getTileHeight()));
        padding.setText(String.valueOf(defaultParameters.getPadding()));
        boundaryThreshold.setText(String.valueOf(defaultParameters.getBoundaryThreshold()));
        tileWidth.setTextFormatter(new TextFormatter<>(unsignedIntegerFilter));
        tileHeight.setTextFormatter(new TextFormatter<>(unsignedIntegerFilter));
        padding.setTextFormatter(new TextFormatter<>(unsignedIntegerFilter));
        boundaryThreshold.setTextFormatter(new TextFormatter<>(unsignedFloatFilter));

        if (owner != null) {
            initOwner(owner);
        }

        show();
    }
}
