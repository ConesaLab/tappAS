/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;


import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Chart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;

import java.util.Optional;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class Controls extends AppObject {
    // JavaFX 8 Chart Color Palette
    // may want to change to match theme if it changes in later releases
    static public final String CHART_COLOR_1 = "#f2635c";
    static public final String CHART_COLOR_2 = "#7cafd6";
    static public final String CHART_COLOR_3 = "#a2ca72";
    static public final String CHART_COLOR_4 = "#f7c967";
    static public final String CHART_COLOR_5 = "#d3d03b";
    static public final String CHART_COLOR_6 = "#a16cc1";
    static public final String CHART_COLOR_7 = "#a796ff";
    static public final String CHART_COLOR_8 = "#ff96eb";
    
    static public final String[] chartHexColors = { 
        CHART_COLOR_1,CHART_COLOR_2, CHART_COLOR_3, CHART_COLOR_4,
        CHART_COLOR_5,CHART_COLOR_6, CHART_COLOR_7, CHART_COLOR_8
    };
    static public Color[] chartColors = { 
        Color.web(CHART_COLOR_1), Color.web(CHART_COLOR_2), Color.web(CHART_COLOR_3), Color.web(CHART_COLOR_4),
        Color.web(CHART_COLOR_5), Color.web(CHART_COLOR_6), Color.web(CHART_COLOR_7), Color.web(CHART_COLOR_8)
    };
    // WARNING: wraps around based on size 8
    static public String getChartHexColor(int idx) {
        return chartHexColors[(idx & 0x7)];
    }
    // possible option for PCA chart - right now is set to opaque
    static public final double ccOpacity = 1.0;
    static public Color[] chartOpacityColors = { 
        Color.web(CHART_COLOR_1, ccOpacity), Color.web(CHART_COLOR_2, ccOpacity), Color.web(CHART_COLOR_3, ccOpacity), Color.web(CHART_COLOR_4, ccOpacity),
        Color.web(CHART_COLOR_5, ccOpacity), Color.web(CHART_COLOR_6, ccOpacity), Color.web(CHART_COLOR_7, ccOpacity), Color.web(CHART_COLOR_8, ccOpacity)
    };
    
    public Controls(App app) {
        super(app);
    }
    
    //
    // Chart and Image Functions
    //
    
    public static WritableImage pixelScaleAwareCanvasSnapshot(Chart canvas, double pixelScale) {
        WritableImage writableImage = new WritableImage((int)Math.rint(pixelScale*canvas.getWidth()), (int)Math.rint(pixelScale*canvas.getHeight()));
        SnapshotParameters spa = new SnapshotParameters();
        spa.setTransform(Transform.scale(pixelScale, pixelScale));
        return canvas.snapshot(spa, writableImage);     
    }
    
    public void setupChartExport(Chart chart, String name, String filename, MenuItem[] items) {
        ContextMenu cm = new ContextMenu();
        if(items != null) {
            for(MenuItem item : items)
                cm.getItems().add(item);
            cm.getItems().add(new SeparatorMenuItem());
        }
        cm.setAutoHide(true);
        chart.setUserData(cm);
        MenuItem item = new MenuItem("Export chart image...");
        cm.getItems().add(item);
        item.setOnAction((event) -> {
            // the menu will not hide from the DrillDownDFI
            cm.hide();
            SnapshotParameters sP = new SnapshotParameters();
            double x = chart.getWidth();
            double ratio = 3840/x;
            chart.setScaleX(ratio);
            chart.setScaleY(ratio);
            WritableImage img = chart.snapshot(sP, null);
            //WritableImage img = pixelScaleAwareCanvasSnapshot(chart, 2);
            double newX = chart.getWidth();
            ratio = x/newX;
            chart.setScaleX(ratio);
            chart.setScaleY(ratio);
            app.export.exportImage(name, filename, img);
        });
        
        item = new MenuItem("Copy Image to Clipboard...");
        item.setOnAction((event) -> { Utils.copyToClipboardImage(chart); });
        cm.getItems().add(item);

        chart.setOnMouseClicked((event) -> {
            if(MouseButton.SECONDARY.equals(event.getButton())) {
                cm.show(Tappas.getStage(), event.getScreenX(), event.getScreenY());
            }  
        });
    }
    
    public void setupArchorPaneExport(AnchorPane chart, String name, String filename, MenuItem[] items) {
        ContextMenu cm = new ContextMenu();
        if(items != null) {
            for(MenuItem item : items)
                cm.getItems().add(item);
            cm.getItems().add(new SeparatorMenuItem());
        }
        cm.setAutoHide(true);
        chart.setUserData(cm);
        MenuItem item = new MenuItem("Export image...");
        cm.getItems().add(item);
        item.setOnAction((event) -> {
            // the menu will not hide from the DrillDownDFI
            cm.hide();
            SnapshotParameters sP = new SnapshotParameters();
            double x = chart.getWidth();
            double ratio = 3840/x;
            chart.setScaleX(ratio);
            chart.setScaleY(ratio);
            WritableImage img = chart.snapshot(sP, null);
            double newX = chart.getWidth();
            ratio = x/newX;
            chart.setScaleX(ratio);
            chart.setScaleY(ratio);
            app.export.exportImage(name, filename, img);
        });
        
        item = new MenuItem("Copy Image to Clipboard...");
        item.setOnAction((event) -> { Utils.copyToClipboardImage(chart); });
        cm.getItems().add(item);
        
        chart.setOnMouseClicked((event) -> {
            if(MouseButton.SECONDARY.equals(event.getButton())) {
                cm.show(Tappas.getStage(), event.getScreenX(), event.getScreenY());
            }  
        });
    }
    public void setupImageExport(ImageView imgView, String name, String filename, MenuItem[] items) {
        ContextMenu cm = new ContextMenu();
        if(items != null) {
            for(MenuItem item : items)
                cm.getItems().add(item);
            cm.getItems().add(new SeparatorMenuItem());
        }
        cm.setAutoHide(true);
        imgView.setUserData(cm);
        MenuItem item = new MenuItem("Export image...");
        cm.getItems().add(item);
        item.setOnAction((event) -> {
            // the menu will not hide from the VennDiagram tool
            cm.hide();
            app.export.exportImage(name, filename, imgView.getImage());
        });
        
        item = new MenuItem("Copy Image to Clipboard...");
        item.setOnAction((event) -> { Utils.copyToClipboardImage(imgView); });
        cm.getItems().add(item);
        
        imgView.setOnMouseClicked((event) -> {
            if(MouseButton.SECONDARY.equals(event.getButton())) {
              cm.show(Tappas.getStage(), event.getScreenX(), event.getScreenY());
            }  
        });
    }
    public Button addImgButton(Pane pane, String imgName, String tooltip, double yoffset, double size, boolean dlg) {
        Image img = new Image(getClass().getResourceAsStream("/tappas/images/" + imgName));
        Button btn = new Button();
        btn.setGraphicTextGap(0.0);
        /*
        btn.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-border-style: none; -fx-border-color: transparent;"); /-* +
                    "rgba(0,0,0,0.08), " +
                    "linear-gradient(#9a9a9a, #909090), " +
                    "linear-gradient(#f0f8ff 0%, #f3f3f8 50%, #ececf6 51%, #f2f4f8 100%); " +
                    "-fx-background-insets: 0 0 -1 0,0,1; " +
                    "-fx-background-radius: 2,2,1; " +
                    "-fx-padding: 0; " +
                    "-fx-text-fill: #242d35; " +
                    "-fx-font-size: 13px");*/
        btn.setPrefSize(size, size);
        ImageView iv = new ImageView(img);
        iv.setFitHeight(SubTabBase.BTN_IMGSIZE);
        iv.setFitWidth(SubTabBase.BTN_IMGSIZE);
        btn.setGraphic(iv);
        btn.getStyleClass().add("buttonHelpHMenu");
        btn.setLayoutX(dlg? 3.5 : 2.25);
        btn.setLayoutY(yoffset);
        btn.setTooltip(new Tooltip(tooltip));
        pane.getChildren().add(btn);
        return btn;
    }
    /* no longer used
    public void changeImgButton(Button btn, String imgName, String tooltip) {
        Image img = new Image(getClass().getResourceAsStream("/tappas/images/" + imgName));
        ImageView iv = new ImageView(img);
        iv.setFitHeight(SubTabBase.BTN_IMGSIZE);
        iv.setFitWidth(SubTabBase.BTN_IMGSIZE);
        btn.setGraphic(iv);
        btn.setTooltip(new Tooltip(tooltip));
    }*/
    
    //
    // Alert Functions
    //
    public void alertInformation(String title, String msg) { alert(Alert.AlertType.INFORMATION, title, msg); }
    public void alertWarning(String title, String msg) { alert(Alert.AlertType.WARNING, title, msg); }
    public void alertError(String title, String msg) { alert(Alert.AlertType.ERROR, title, msg); }
    public boolean alertConfirmation(String title, String msg, ButtonType btnDefault) { return alertConfirmation(title, msg, btnDefault, false); }
    public boolean alertConfirmationYN(String title, String msg, ButtonType btnDefault) { return alertConfirmation(title, msg, btnDefault, true); }
    public ButtonType alertConfirmationYNC(String title, String msg, ButtonType btnDefault) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, title, ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        if(btnDefault != null)
            setDefaultButton(alert, btnDefault);
        Optional<ButtonType> result = alert.showAndWait();
        return result.get();
    }
    
    //
    // Internal Functions
    //
    private void alert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }
    private boolean alertConfirmation(String title, String msg, ButtonType btnDefault, boolean YesNo) {
        Alert alert;
        if(YesNo)
            alert = new Alert(Alert.AlertType.CONFIRMATION, title, ButtonType.YES, ButtonType.NO);
        else
            alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        if(btnDefault != null)
            setDefaultButton(alert, btnDefault);
        Optional<ButtonType> result = alert.showAndWait();
        return(result.get() == (YesNo? ButtonType.YES : ButtonType.OK));
    }
    private void setDefaultButton(Alert alert, ButtonType btnDefault) {
        Button btn;
        ObservableList<ButtonType> lst = alert.getButtonTypes();
        for(ButtonType type : lst) {
            btn = (Button) alert.getDialogPane().lookupButton(type);
            btn.setDefaultButton(false);
        }
        btn = (Button) alert.getDialogPane().lookupButton(btnDefault);
        if(btn != null)
            btn.setDefaultButton(true);
    }
    public void updateProgress(ProgressBar pbProgress, double value) {
        Platform.runLater(() -> {
            pbProgress.setProgress(value);
        });
    }
}
