/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import tappas.DbProject.AFStatsData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabAnnotationDistribution extends SubTabBase {
    final static int MAX_COUNT = 100;
    final static String ALL_FEATURES = "<All Features>";

    AnchorPane paneContents;
    ChoiceBox cbFeatures;
    BarChart barDist;

    String selSource = "";
    String selFeature = "";
    HashMap<String, AFStatsData> hmFeatures = new HashMap<>();
    
    public SubTabAnnotationDistribution(Project project) {
        super(project);
    }
    
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            selSource = (String)args.get("Source");
            selFeature = (String)args.get("Feature");
        }
        return result;
    }
    @Override
    public void refreshSubTab(HashMap<String, Object> hmArgs) {
        selSource = (String)hmArgs.get("Source");
        selFeature = (String)hmArgs.get("Feature");
        // data is loaded in memory, OK to not do in background
        hmFeatures = project.data.getSourceFeaturesStats(selSource);
        populateDistribution();
    }
    
    //
    // Internal Functions
    //
    private void showSubTab(HashMap<String, AFStatsData> hmData) {
        hmFeatures = hmData;

        // get control objects
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        cbFeatures = (ChoiceBox) tabNode.getParent().lookup("#cbFeatures");
        barDist = (BarChart) tabNode.getParent().lookup("#barDistribution");

        // setup and populate data
        cbFeatures.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            if(newValue != null && (int)newValue != -1)
                showDistributionChart(selSource, (String)cbFeatures.getItems().get((int)newValue));
        });
        populateDistribution();
        setupExportMenu();
        setFocusNode(barDist, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export distribution chart...");
        item.setOnAction((event) -> {
            SnapshotParameters sP = new SnapshotParameters();
            double x = barDist.getWidth();
            double ratio = 3840/x;
            barDist.setScaleX(ratio);
            barDist.setScaleY(ratio);
            WritableImage img = barDist.snapshot(sP, null);
            double newX = barDist.getWidth();
            ratio = x/newX;
            barDist.setScaleX(ratio);
            barDist.setScaleY(ratio);
            
            //WritableImage img = barDist.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Annotation Source Features Distribution Chart", "tappAS_Features_Distribution.png", (Image)img);
        });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Export subtab contents image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = paneContents.getWidth();
            double ratio = 3840/x;
            paneContents.setScaleX(ratio);
            paneContents.setScaleY(ratio);
            WritableImage img = paneContents.snapshot(sP, null);
            double newX = paneContents.getWidth();
            ratio = x/newX;
            paneContents.setScaleX(ratio);
            paneContents.setScaleY(ratio);
            
            //WritableImage img = paneContents.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Annotation Source Features Distribution Panel", "tappAS_Annotation_Source_Features_Distribution_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void populateDistribution() {
        ArrayList<String> lstFeatures = new ArrayList<>();
        for(String feature : hmFeatures.keySet())
            lstFeatures.add(feature);
        Collections.sort(lstFeatures, String.CASE_INSENSITIVE_ORDER);
        if(cbFeatures.getItems().size() > 0) {
            cbFeatures.getSelectionModel().clearSelection();
            cbFeatures.getItems().clear();
        }
        cbFeatures.getItems().add(ALL_FEATURES);
        cbFeatures.getItems().addAll(lstFeatures);
        if(cbFeatures.getItems().size() > 0)
            cbFeatures.getSelectionModel().select(0);
    }
    private void showDistributionChart(String source, String feature) {
        if(feature.equals(ALL_FEATURES))
            feature = "";
        XYChart.Series<String, Number> series = getDistribution(source, feature, MAX_COUNT);
        barDist.setTitle("'" + source + " : " + (feature.isEmpty()? "(All Features)" : feature) + "' Distribution per Transcript");
        barDist.getData().clear();
        barDist.getData().add(series);
        app.ctls.setupChartExport(barDist, "Annotation Features Distribution Chart", "tappAS_Feature_Distribution.png", null);

        Tooltip tt;
        for(int i = 0; i < series.getData().size(); i++) {
            XYChart.Data item = series.getData().get(i);
            tt = new Tooltip(item.getXValue().toString() + " Annotation(s) per Transcript:  " + item.getYValue().toString() + " transcripts");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
    }
    private XYChart.Series<String, Number> getDistribution(String source, String feature, int maxCnt) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        int[] dist = project.data.getFeatureDistributionPerTrans(source, feature, maxCnt, ((TabAnnotation)tabBase).getTransFilter());
        int min = dist[0] > 0? 0 : 1;
        int max = 0;
        for(int i = 0; i <= maxCnt; i++) {
            if(dist[i] > 0)
                max = i;
        }
        String val;
        for(int i = min; i <= max; i++) {
            val = "" + i;
            if(i == maxCnt)
                val += "+";
            series.getData().add(new XYChart.Data<>(val, dist[i]));
        }
        return series;
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        if(project.data.getDataAnnotation().isTransAFIdPosLoaded()) {
            HashMap<String, AFStatsData> data = project.data.getSourceFeaturesStats(selSource);
            showSubTab(data);
        }
        else {
            service = new DataLoadService();
            service.start();
        }
    }
    private class DataLoadService extends TaskHandler.ServiceExt {
        HashMap<String, AFStatsData> data = null;
        
        @Override protected void onRunning() { showProgress(); }
        @Override protected void onStopped() { hideProgress(); }
        @Override
        protected void onFailed() {
            java.lang.Throwable e = getException();
            if(e != null)
                app.logWarning("AnnotationDistribution failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("AnnotationDistribution - task aborted.");
            app.ctls.alertWarning("Annotation Distribution", "AnnotationDistribution failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                // set page data ready flag and show actual subtab contents
                pageDataReady = true;
                showSubTab(data);
            }
            else
                app.ctls.alertWarning("Project Data", "Unable to retrieve annotation features for '" + selSource + "'");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    // get data to display
                    data = project.data.getSourceFeaturesStats(selSource);
                    
                    // Need to load TransAFIdPos for distribution - could take a few seconds
                    // if we address the pre-loading of this somewhere else, get rid of using a bkgnd thread here
                    project.data.getDataAnnotation().loadTransAFIdPos();
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("Get annotation features for source '" + selSource + "'", task);
            return task;
        }
    }
}
