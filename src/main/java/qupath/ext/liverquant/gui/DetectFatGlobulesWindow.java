package qupath.ext.liverquant.gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TitledPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import qupath.ext.liverquant.core.FatGlobuleDetector;
import qupath.ext.liverquant.core.FatGlobulesDetectorParameters;
import qupath.ext.liverquant.core.HsvArray;
import qupath.ext.liverquant.core.TissueDetectorParameters;
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
    private static final Pattern unsignerIntegerPattern = Pattern.compile("\\d*");
    @FXML
    private ChoiceBox<FatGlobulesDetectorParameters.ObjectToCreate> objectsToCreate;
    @FXML
    private TextField pixelSize;
    @FXML
    private ChoiceBox<FatGlobulesDetectorParameters.DetectionRegion> detectionRegion;
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
    private TitledPane tissueDetectionParameters;
    @FXML
    private TextField lowerHueTissue;
    @FXML
    private TextField lowerSaturationTissue;
    @FXML
    private TextField lowerValueTissue;
    @FXML
    private TextField upperHueTissue;
    @FXML
    private TextField upperSaturationTissue;
    @FXML
    private TextField upperValueTissue;
    @FXML
    private TextField downsample;
    @FXML
    private TextField minTissueArea;
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
        if (detectionRegion.getSelectionModel().getSelectedItem().equals(FatGlobulesDetectorParameters.DetectionRegion.SELECTED_ANNOTATIONS)
                && selectedAnnotations.isEmpty()
        ) {
            Dialogs.showErrorMessage(
                    resources.getString("DetectFatGlobulesWindow.liverquant"),
                    resources.getString("DetectFatGlobulesWindow.selectAtLeastOneAnnotation")
            );
            return;
        }

        run.setDisable(true);
        FatGlobuleDetector.run(new FatGlobulesDetectorParameters.Builder(imageData)
                .setTissueDetectorParameters(new TissueDetectorParameters.Builder(imageData.getServer())
                        .setLowerBound(new HsvArray(
                                lowerHueTissue.getText().isEmpty() ? 0 : Integer.parseInt(lowerHueTissue.getText()),
                                lowerSaturationTissue.getText().isEmpty() ? 0 : Integer.parseInt(lowerSaturationTissue.getText()),
                                lowerValueTissue.getText().isEmpty() ? 0 : Integer.parseInt(lowerValueTissue.getText())
                        ))
                        .setUpperBound(new HsvArray(
                                upperHueTissue.getText().isEmpty() ? 0 : Integer.parseInt(upperHueTissue.getText()),
                                upperSaturationTissue.getText().isEmpty() ? 0 : Integer.parseInt(upperSaturationTissue.getText()),
                                upperValueTissue.getText().isEmpty() ? 0 : Integer.parseInt(upperValueTissue.getText())
                        ))
                        .setDownsample(downsample.getText().isEmpty() ? 0 : Float.parseFloat(downsample.getText()))
                        .setMinTissueArea(minTissueArea.getText().isEmpty() ? 0 : Float.parseFloat(minTissueArea.getText()))
                        .build()
                )
                .setAnnotations(selectedAnnotations)
                .setDetectionRegion(detectionRegion.getSelectionModel().getSelectedItem())
                .setProgressDisplay(FatGlobulesDetectorParameters.ProgressDisplay.WINDOW)
                .setObjectToCreate(objectsToCreate.getSelectionModel().getSelectedItem())
                .setPixelSize(pixelSize.getText().isEmpty() ? 0 : Float.parseFloat(pixelSize.getText()))
                .setLowerBound(new HsvArray(
                        lowerHue.getText().isEmpty() ? 0 : Integer.parseInt(lowerHue.getText()),
                        lowerSaturation.getText().isEmpty() ? 0 : Integer.parseInt(lowerSaturation.getText()),
                        lowerValue.getText().isEmpty() ? 0 : Integer.parseInt(lowerValue.getText())
                ))
                .setUpperBound(new HsvArray(
                        upperHue.getText().isEmpty() ? 0 : Integer.parseInt(upperHue.getText()),
                        upperSaturation.getText().isEmpty() ? 0 : Integer.parseInt(upperSaturation.getText()),
                        upperValue.getText().isEmpty() ? 0 : Integer.parseInt(upperValue.getText())
                ))
                .setMinIsolatedGlobuleElongation(minIsolatedGlobuleElongation.getText().isEmpty() ? 0 : Float.parseFloat(minIsolatedGlobuleElongation.getText()))
                .setMinOverlappingGlobuleElongation(minOverlappingGlobuleElongation.getText().isEmpty() ? 0 : Float.parseFloat(minOverlappingGlobuleElongation.getText()))
                .setMinIsolatedGlobuleSolidity(minIsolatedGlobuleSolidity.getText().isEmpty() ? 0 : Float.parseFloat(minIsolatedGlobuleSolidity.getText()))
                .setMinOverlappingGlobuleSolidity(minOverlappingGlobuleSolidity.getText().isEmpty() ? 0 : Float.parseFloat(minOverlappingGlobuleSolidity.getText()))
                .setMinDiameter(minDiameter.getText().isEmpty() ? 0 : Float.parseFloat(minDiameter.getText()))
                .setMaxDiameter(maxDiameter.getText().isEmpty() ? 0 : Float.parseFloat(maxDiameter.getText()))
                .setTileWidth(tileWidth.getText().isEmpty() ? 0 : Integer.parseInt(tileWidth.getText()))
                .setTileHeight(tileHeight.getText().isEmpty() ? 0 : Integer.parseInt(tileHeight.getText()))
                .setPadding(padding.getText().isEmpty() ? 0 : Integer.parseInt(padding.getText()))
                .setBoundaryThreshold(boundaryThreshold.getText().isEmpty() ? 0 : Float.parseFloat(boundaryThreshold.getText()))
                .setOnFinished(() -> Platform.runLater(() -> {
                    run.setDisable(false);
                    quPathGUI.getViewer().getHierarchy().resolveHierarchy();
                }))
                .build()
        );
    }

    private void initUI(Stage owner) throws IOException {
        UiUtilities.loadFXML(this, DetectFatGlobulesWindow.class.getResource("detect_fat_globules.fxml"));

        FatGlobulesDetectorParameters defaultParameters = new FatGlobulesDetectorParameters.Builder(null).build();

        Pattern unsignerFloatPattern = Pattern.compile("\\d*\\.?\\d*");
        UnaryOperator<TextFormatter.Change> unsignedFloatFilter = change ->
                unsignerFloatPattern.matcher(change.getControlNewText()).matches() ? change : null;
        Pattern floatPattern = Pattern.compile("[+-]?\\d*\\.?\\d*");
        UnaryOperator<TextFormatter.Change> floatFilter = change ->
                floatPattern.matcher(change.getControlNewText()).matches() ? change : null;

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
        pixelSize.setTextFormatter(new TextFormatter<>(floatFilter));
        detectionRegion.setItems(FXCollections.observableList(List.of(FatGlobulesDetectorParameters.DetectionRegion.values())));
        detectionRegion.setConverter(new StringConverter<>() {
            @Override
            public String toString(FatGlobulesDetectorParameters.DetectionRegion object) {
                return switch (object) {
                    case DETECTED_TISSUE -> resources.getString("DetectFatGlobulesWindow.detectedTissue");
                    case SELECTED_ANNOTATIONS -> resources.getString("DetectFatGlobulesWindow.selectedAnnotations");
                };
            }

            @Override
            public FatGlobulesDetectorParameters.DetectionRegion fromString(String string) {
                return null;
            }
        });
        detectionRegion.getSelectionModel().select(defaultParameters.getDetectionRegion());

        lowerHue.setText(String.valueOf(defaultParameters.getLowerBound().hue()));
        lowerSaturation.setText(String.valueOf(defaultParameters.getLowerBound().saturation()));
        lowerValue.setText(String.valueOf(defaultParameters.getLowerBound().value()));
        upperHue.setText(String.valueOf(defaultParameters.getUpperBound().hue()));
        upperSaturation.setText(String.valueOf(defaultParameters.getUpperBound().saturation()));
        upperValue.setText(String.valueOf(defaultParameters.getUpperBound().value()));
        lowerHue.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(180)));
        lowerSaturation.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(255)));
        lowerValue.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(255)));
        upperHue.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(180)));
        upperSaturation.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(255)));
        upperValue.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(255)));

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
        tileWidth.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(Integer.MAX_VALUE)));
        tileHeight.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(Integer.MAX_VALUE)));
        padding.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(Integer.MAX_VALUE)));
        boundaryThreshold.setTextFormatter(new TextFormatter<>(unsignedFloatFilter));

        tissueDetectionParameters.visibleProperty().bind(
                detectionRegion.getSelectionModel().selectedItemProperty().isEqualTo(FatGlobulesDetectorParameters.DetectionRegion.DETECTED_TISSUE)
        );
        tissueDetectionParameters.managedProperty().bind(tissueDetectionParameters.visibleProperty());
        lowerHueTissue.setText(String.valueOf(defaultParameters.getTissueDetectorParameters().getLowerBound().hue()));
        lowerSaturationTissue.setText(String.valueOf(defaultParameters.getTissueDetectorParameters().getLowerBound().saturation()));
        lowerValueTissue.setText(String.valueOf(defaultParameters.getTissueDetectorParameters().getLowerBound().value()));
        upperHueTissue.setText(String.valueOf(defaultParameters.getTissueDetectorParameters().getUpperBound().hue()));
        upperSaturationTissue.setText(String.valueOf(defaultParameters.getTissueDetectorParameters().getUpperBound().saturation()));
        upperValueTissue.setText(String.valueOf(defaultParameters.getTissueDetectorParameters().getUpperBound().value()));
        lowerHueTissue.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(180)));
        lowerSaturationTissue.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(255)));
        lowerValueTissue.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(255)));
        upperHueTissue.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(180)));
        upperSaturationTissue.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(255)));
        upperValueTissue.setTextFormatter(new TextFormatter<>(getIntegerBetweenBoundsFilter(255)));
        downsample.setText(String.valueOf(defaultParameters.getTissueDetectorParameters().getDownsample()));
        minTissueArea.setText(String.valueOf(defaultParameters.getTissueDetectorParameters().getMinTissueArea()));
        downsample.setTextFormatter(new TextFormatter<>(unsignedFloatFilter));
        minTissueArea.setTextFormatter(new TextFormatter<>(unsignedFloatFilter));

        if (owner != null) {
            initOwner(owner);
        }

        show();
    }

    private static UnaryOperator<TextFormatter.Change> getIntegerBetweenBoundsFilter(int upperBound) {
        return change -> {
            if (unsignerIntegerPattern.matcher(change.getControlNewText()).matches()) {
                if (change.getControlNewText().isEmpty()) {
                    return change;
                } else {
                    try {
                        int value = Integer.parseInt(change.getControlNewText());
                        if (value < 0 || value > upperBound) {
                            return null;
                        } else {
                            return change;
                        }
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            } else {
                return null;
            }
        };
    }
}
