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
public class PlotsFDACombinedResults extends AppObject {
    ProgressIndicator pi;
    Pane paneImgChart;
    ImageView imgChart;
    ScrollPane sp;
    
    String analysis_genomic, analysis_presence;
    boolean plotShown = false;
    Path runScriptPath = null;
    double ratio = 0.75;
    SubTabBase.SubTabInfo subTabInfo = null;
    TaskHandler.ServiceExt service = null;
    
    String export_name = "FDA_CombinedResults_ID.png";

    public PlotsFDACombinedResults(Project project) {
        super(project, null);
    }
    
    public void showFDACombinedResultsPlots(SubTabBase.SubTabInfo subTabInfo, Node tabNode, boolean update, String analysis_genomic, String analysis_presence) {
        this.subTabInfo = subTabInfo;
        this.analysis_genomic = analysis_genomic;
        this.analysis_presence = analysis_presence;
        
        sp = (ScrollPane) tabNode.lookup("#spFDA");
        paneImgChart = (Pane) sp.getContent().lookup("#paneImgChart");
        imgChart = (ImageView) sp.getContent().lookup("#imgChart");
        pi = (ProgressIndicator) sp.getContent().lookup("#piFDAplot");

        pi.layoutXProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart.widthProperty(), 2.0), 25.0));
        pi.layoutYProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart.heightProperty(), 2.0), 25.0));
        imgChart.fitHeightProperty().bind(paneImgChart.heightProperty());
        imgChart.fitWidthProperty().bind(paneImgChart.widthProperty());
        imgChart.setPreserveRatio(true);
        app.ctls.setupImageExport(imgChart, "FDA Combined ResultsBarplot", "tappAS_" + export_name, null);

        //Change loading and load DIU table results
        runDataLoadThread();
    }
    
    //
    // Internal Functions
    //
    private void showBarPlotImage(String filepath) {
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
        runScriptPath = app.data.getTmpScriptFileFromResource("FDA_CombinedResults_Barplot.R");
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
                app.logWarning("FDA Combine Results Barplot failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("FDA Combine Results Barplot - task aborted.");
            app.ctls.alertWarning("FDA Combine Results Barplot", "Barplot failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            String filepath = project.data.getFDACombinedResultsPlotFilepath();
            if(Files.exists(Paths.get(filepath))) {
                if(!plotShown) {
                    plotShown = true;
                    showBarPlotImage(filepath);
                }
            }
            else
                app.ctls.alertWarning("FDA Combine Results Barplot", "Unable to generate barplot.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String logfilepath = project.data.getSubTabLogFilepath(subTabInfo.subTabId);
                    subTabInfo.subTabBase.outputFirstLogLine("FDA Combine Results Barplot Running...", logfilepath);

                    // setup script arguments
                    List<String> lst = new ArrayList<>();
                    lst.add(app.data.getRscriptFilepath());
                    lst.add(runScriptPath.toString());
                    lst.add("-i" + project.data.analysis.getFDAFolder());
                    lst.add("-g" + analysis_genomic);
                    lst.add("-p" + analysis_presence);
                    lst.add("-o" + project.data.getFDACombinedResultsPlotFilepath());
                    subTabInfo.subTabBase.appendLogLine("Starting R script...", logfilepath);
                    logger.logDebug("Running FDA CombineResults Barplot script:\n    " + lst);
                    subTabInfo.subTabBase.runScript(taskInfo, lst, "FDA Combined Results Plot", logfilepath);

                    // remove script file from temp folder
                    Utils.removeFile(runScriptPath);
                    runScriptPath = null;
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("FDA Combine Results Barplot", task);
            return task;
        }
    }
}
