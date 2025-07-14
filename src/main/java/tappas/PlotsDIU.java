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
public class PlotsDIU extends AppObject {
    ProgressIndicator pi;
    Pane paneImgChart;
    ImageView imgChart;
    
    boolean plotShown = false;
    Path runScriptPath = null;
    double ratio = 0.75;
    DataApp.DataType dataType;
    SubTabBase.SubTabInfo subTabInfo = null;
    TaskHandler.ServiceExt service = null;
    
    String export_name = "DIU_ScatterPlot.png";

    public PlotsDIU(Project project) {
        super(project, null);
    }
    
    public void showDIUScatterPlot(DataApp.DataType type, SubTabBase.SubTabInfo subTabInfo, Node tabNode, boolean update) {
        this.dataType = type;
        this.subTabInfo = subTabInfo;
        paneImgChart = (Pane) tabNode.lookup("#paneImgChart");
        imgChart = (ImageView) tabNode.lookup("#imgChart");
        pi = (ProgressIndicator) tabNode.lookup("#piDIUScatterPlot");

        pi.layoutXProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart.widthProperty(), 2.0), 25.0));
        pi.layoutYProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart.heightProperty(), 2.0), 25.0));
        imgChart.fitHeightProperty().bind(paneImgChart.heightProperty());
        imgChart.fitWidthProperty().bind(paneImgChart.widthProperty());
        imgChart.setPreserveRatio(true);
        app.ctls.setupImageExport(imgChart, "DIU Scatter Plot", "tappAS_" + export_name, null);

        //Change loading and load DIU table results
        String filepath = project.data.getScatterPlotFilepath(dataType.name());
        if(!plotShown) {
            if(Files.exists(Paths.get(filepath)) && !update) {
                plotShown = true;
                showScatterPlotImage(filepath);
            }
            else {
                boolean runDLT = true;
                if(!Files.exists(Paths.get(project.data.getMatrixDIUFilepath(dataType.name())))) {
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
    private void showScatterPlotImage(String filepath) {
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
        runScriptPath = app.data.getTmpScriptFileFromResource("DIU_ScatterPlot.R");
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
                app.logWarning("ScatterPlot failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("ScatterPlot - task aborted.");
            app.ctls.alertWarning("DIU Scatter Plot", "ScatterPlot failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            String filepath = project.data.getScatterPlotFilepath(dataType.name());
            if(Files.exists(Paths.get(filepath))) {
                if(!plotShown) {
                    plotShown = true;
                    showScatterPlotImage(filepath);
                }
            }
            else
                app.ctls.alertWarning("DIU Scatter Plot", "Unable to generate expression levels density plot.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String logfilepath = project.data.getSubTabLogFilepath(subTabInfo.subTabId);
                    subTabInfo.subTabBase.outputFirstLogLine("DIU Scatter Plot Running...", logfilepath);

                    // setup script arguments
                    List<String> lst = new ArrayList<>();
                    lst.add(app.data.getRscriptFilepath());
                    lst.add(runScriptPath.toString());
                    String type = app.data.getDataTypeSingular(dataType.name());
                    lst.add("-d" + type.toLowerCase());
                    lst.add("-i" + project.data.getMatrixDIUFilepath(dataType.name()));
                    lst.add("-o" + project.data.getScatterPlotFilepath(dataType.name()));
                    subTabInfo.subTabBase.appendLogLine("Starting R script...", logfilepath);
                    logger.logDebug("Running expression levels density plot script:\n    " + lst);
                    subTabInfo.subTabBase.runScript(taskInfo, lst, "DIU Scatter Plot", logfilepath);

                    // remove script file from temp folder
                    Utils.removeFile(runScriptPath);
                    runScriptPath = null;
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("DIU Scatter Plot", task);
            return task;
        }
    }
    
}
