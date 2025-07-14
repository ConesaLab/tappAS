/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;

import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabGDVProtein extends SubTabBase {
    Canvas canvas;
    ScrollPane spCanvas;
    TabGeneDataViz tabDataViz;

    private final ViewProtein dvView;

    // canvas settings - transcripts
    private final ViewAnnotation.Prefs dvPrefs = new ViewAnnotation.Prefs(true, 1.25, 1, true, true, false, false, false, false, ViewAnnotation.SortBy.ID);
    
    public SubTabGDVProtein(Project project) {
        super(project);
        dvView = new ViewProtein(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = true;
        subTabInfo.btnExport = true;
        subTabInfo.btnZoom = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            tabDataViz = (TabGeneDataViz)tabBase;
        }
        return result;
    }
    @Override
    protected void onSelect(boolean selected) {
        super.onSelect(selected);
        if(selected && canvas != null) {
            if(!dvView.hasBeenDisplayed())
                updateView();
            canvas.requestFocus(); 
        }
    }
    @Override
    public void onButtonZoomIn() {
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("zoomIn", null);
        processRequest(hm);
    }
    @Override
    public void onButtonZoomOut() {
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("zoomOut", null);
        processRequest(hm);
    }
    @Override
    public void onButtonZoomFit() {
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("zoomFit", null);
        processRequest(hm);
    }
    @Override
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        if(hm.containsKey("zoomIn")) {
            double ppb = dvView.zoomPPB;
            // too much of anything is not good - only allow to zoom in to the point where MAX_PPB pixel is approximately equal to 1 base
            // if you let it keep going you can start seeing some visual differences, based on just 1 base, that are meaningless
            double widthFactor = dvPrefs.widthFactor + TabGeneDataViz.DELTA_WIDTHFACTOR;
            if(ppb < TabGeneDataViz.MAX_PPB && widthFactor <= TabGeneDataViz.MAX_WIDTHFACTOR)
                tabDataViz.updateWidth(dvView, dvPrefs, dvPrefs.widthFactor + TabGeneDataViz.DELTA_WIDTHFACTOR);
            updateZoom();
        }
        else if(hm.containsKey("zoomOut")) {
            if((dvPrefs.widthFactor - TabGeneDataViz.DELTA_WIDTHFACTOR) < TabGeneDataViz.MIN_WIDTHFACTOR)
                tabDataViz.updateWidth(dvView, dvPrefs, TabGeneDataViz.MIN_WIDTHFACTOR);
            else
                tabDataViz.updateWidth(dvView, dvPrefs, dvPrefs.widthFactor - TabGeneDataViz.DELTA_WIDTHFACTOR);
            updateZoom();
        }
        else if(hm.containsKey("zoomFit")) {
            tabDataViz.updateWidth(dvView, dvPrefs, TabGeneDataViz.MIN_WIDTHFACTOR);
            updateZoom();
        }
        return obj;
    }
    @Override
    protected void onButtonExport() {
        exportImage();
    }    
    
    //
    // Internal Functions
    //
    private void showSubTab() {
        spCanvas = (ScrollPane) tabNode.lookup("#spCanvas");
        canvas = (Canvas) spCanvas.getContent().lookup("#canvas");
        
        subTabInfo.cmOptions = tabDataViz.setupOptionsMenu(dvView, dvPrefs);
        if(dvView.initialize(subTabInfo.subTabBase, canvas, spCanvas, tabDataViz.getCaption())) {
            ContextMenu canvasCM = new ContextMenu();
            MenuItem item = new MenuItem("Export image...");
            item.setOnAction((event) -> { exportImage(); });
            canvasCM.getItems().add(item);
            
            item = new MenuItem("Copy Image to Clipboard...");
            item.setOnAction((event) -> { Utils.copyToClipboardImage(canvas); });
            canvasCM.getItems().add(item);
            
            item = new MenuItem("Copy Gene name to Clipboard...");
            item.setOnAction((event) -> { Utils.copyToClipboardText(tabDataViz.getGene()); });
            canvasCM.getItems().add(item);
            
            canvas.setOnMouseMoved((event) -> { dvView.onMouseMoved(event); });
            canvas.setOnMouseExited((event) -> { dvView.onMouseExited(event); });
            canvas.setOnMouseClicked((event) -> { 
                if(event.getButton() == MouseButton.SECONDARY) {
                    canvasCM.show(tabNode, event.getScreenX(), event.getScreenY());
                }
                else {
                    canvasCM.hide();
                    dvView.handleMouseClickAction(event);
                }
            });
            spCanvas.widthProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                if(subTab.isSelected() && tabDataViz.isTabSelected())
                    updateView();
            });
            spCanvas.heightProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                if(oldValue != null && oldValue.doubleValue() < 20) {
                    if(subTab.isSelected() && tabDataViz.isTabSelected())
                        updateView();
                }
            });
            updateView();
        }
        setFocusNode(canvas, true);
    }
    private void updateZoom() {
        double ppb = dvView.zoomPPB;
        
        setBtnZoomInDisable(ppb >= TabGeneDataViz.MAX_PPB || ((dvPrefs.widthFactor + TabGeneDataViz.DELTA_WIDTHFACTOR) > TabGeneDataViz.MAX_WIDTHFACTOR));
        setBtnZoomOutDisable(dvPrefs.widthFactor <= TabGeneDataViz.MIN_WIDTHFACTOR);
        setBtnZoomFitDisable(dvPrefs.widthFactor <= TabGeneDataViz.MIN_WIDTHFACTOR);
    }
    private void updateView() {
        dvView.update(tabDataViz.getGene(), tabDataViz.getGeneAnnotations(), dvPrefs);
        updateZoom();
    }
    private void exportImage() {
        WritableImage img = canvas.snapshot(new SnapshotParameters(), null);
        app.export.exportImage("Gene Proteins Visualization", "tappAS_"+tabDataViz.gene+"_Gene_Proteins_Visualization.png", (Image)img);
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        showSubTab();
    }
}
