/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author Pedro Salguero - psalguero@cipf.es
 */
public class PlotsDPA extends AppObject {
    ScrollPane scrollPane;
    ProgressIndicator pi;
    Pane paneImgChart;
    ImageView imgChart;
    
    DataDPA dpaData;
    DlgDPAnalysis.Params dpaParams;
    
    boolean plotShown = false;
    Path runScriptPath = null;
    double ratio = 0.75;
    SubTabBase.SubTabInfo subTabInfo = null;
    TaskHandler.ServiceExt service = null;
    
    String export_name = "DPA_heatmap.png";

    public PlotsDPA(Project project) {
        super(project, null);
    }
    
    public void showDPAHeatmapPlot(SubTabBase.SubTabInfo subTabInfo, Node tabNode, boolean update) {
        this.subTabInfo = subTabInfo;
        
        scrollPane = (ScrollPane) tabNode.lookup("#scrollPane");
        
        paneImgChart = (Pane) scrollPane.getContent().lookup("#paneImgChart");
        imgChart = (ImageView) scrollPane.getContent().lookup("#imgChart");
        pi = (ProgressIndicator) scrollPane.getContent().lookup("#piDPAHeatmapPlot");

        pi.layoutXProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart.widthProperty(), 2.0), 25.0));
        pi.layoutYProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart.heightProperty(), 2.0), 25.0));
        imgChart.fitHeightProperty().bind(paneImgChart.heightProperty());
        imgChart.fitWidthProperty().bind(paneImgChart.widthProperty());
        imgChart.setPreserveRatio(true);
        app.ctls.setupImageExport(imgChart, "DPA Heatmap", "tappAS_" + export_name, null);
        
        //check DPA
        dpaData = new DataDPA(project);
        boolean result = dpaData.hasDPAData();
        if(result)
                dpaParams = DlgDPAnalysis.Params.load(dpaData.getDPAParamsFilepath(), project);
            else
                subTabInfo.subTabBase = null;
        
        //Change loading and load DPA table results
        String filepath = project.data.getDPAHeatmapFilepath();
        if(!plotShown) {
            if(Files.exists(Paths.get(filepath)) && !update) {
                plotShown = true;
                showHeatmapPlotImage(filepath);
            }
            else {
                boolean runDLT = true;
                if(!result) {
                    runDLT = false;
                }
                // start task to create chart in R and return image
                if(runDLT)
                    runDataLoadThread();
            }
        }
    }
    
    //
    // Internal Functions
    //
    private void showHeatmapPlotImage(String filepath) {
        Image img = new Image("file:" + filepath);
        pi.setVisible(false);
        imgChart.setImage(img);
        imgChart.setVisible(true);
    }
    
    //
    // Data Load
    //
    private void runDataLoadThread() {
        // get script path and run service/task
        runScriptPath = app.data.getTmpScriptFileFromResource("DPA_Heatmap.R");
        service = new DataLoadService();
        service.start();
    }
    private class DataLoadService extends TaskHandler.ServiceExt {
        @Override
        protected void onRunning() {
            pi.setProgress(-1);
            pi.setVisible(true);
        }
        @Override
        protected void onStopped() {
            pi.setVisible(false);
            pi.setProgress(0);
        }
        @Override
        protected void onFailed() {
            java.lang.Throwable e = getException();
            if(e != null)
                app.logWarning("HeatMapPlot failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("HeatMapPlot - task aborted.");
            app.ctls.alertWarning("DPA Heatmap Plot", "HeatMapPlot failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            String filepath = project.data.getDPAHeatmapFilepath();
            if(Files.exists(Paths.get(filepath))) {
                if(!plotShown) {
                    plotShown = true;
                    showHeatmapPlotImage(filepath);
                }
            }
            else
                app.ctls.alertWarning("DPA Heatmap Plot", "Unable to generate heatmap plot.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String logfilepath = project.data.getSubTabLogFilepath(subTabInfo.subTabId);
                    subTabInfo.subTabBase.outputFirstLogLine("DPA Heatmap Plot Running...", logfilepath);
                    subTabInfo.subTabBase.appendLogLine("Generating required input files...", logfilepath);
                    // setup script arguments
                    List<String> lst = new ArrayList<>();
                    lst.add(app.data.getRscriptFilepath());
                    lst.add(runScriptPath.toString());
                    lst.add("-s" + dpaParams.sigValue);
                    lst.add(("-i" + dpaData.getDPAFolder()).replace("\\","/"));
                    lst.add(("-o" + project.data.getDPAHeatmapFilepath()).replace("\\","/"));
                    subTabInfo.subTabBase.appendLogLine("Starting R script...", logfilepath);
                    logger.logDebug("Running heatmap plot script:\n    " + lst);
                    subTabInfo.subTabBase.runScript(taskInfo, lst, "DPA Heatmap Plot", logfilepath);

                    // remove script file from temp folder
                    Utils.removeFile(runScriptPath);
                    runScriptPath = null;
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("DPA Heatmap Plot", task);
            return task;
        }
    }
    
}
