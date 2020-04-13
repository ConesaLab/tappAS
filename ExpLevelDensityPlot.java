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

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class ExpLevelDensityPlot extends AppObject {
    ProgressIndicator pi;
    Pane paneImgChart;
    ImageView imgChart;
    
    boolean plotShown = false;
    Path runScriptPath = null;
    double ratio = 0.75;
    DataApp.DataType dataType;
    SubTabBase.SubTabInfo subTabInfo = null;
    TaskHandler.ServiceExt service = null;
    
    public ExpLevelDensityPlot(Project project) {
        super(project, null);
    }
    
    public void showExpLevelsDensityPlot(DataApp.DataType type, SubTabBase.SubTabInfo subTabInfo, Node tabNode) {
        this.dataType = type;
        this.subTabInfo = subTabInfo;
        paneImgChart = (Pane) tabNode.lookup("#paneImgChart");
        imgChart = (ImageView) tabNode.lookup("#imgChart");
        pi = (ProgressIndicator) tabNode.lookup("#piDensityPlot");

        pi.layoutXProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart.widthProperty(), 2.0), 25.0));
        pi.layoutYProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart.heightProperty(), 2.0), 25.0));
        imgChart.fitHeightProperty().bind(paneImgChart.heightProperty());
        imgChart.fitWidthProperty().bind(paneImgChart.widthProperty());
        imgChart.setPreserveRatio(true);
        app.ctls.setupImageExport(imgChart, "Transcripts Expression Levels Density Plot", "tapas_" + "TransExpLevelsDPlot.png", null);

        String filepath = project.data.getMatrixMeanLogExpLevelsPlotFilepath(dataType.name());
        if(!plotShown) {
            if(Files.exists(Paths.get(filepath))) {
                plotShown = true;
                showDensityPlotImage(filepath);
            }
            else {
                boolean runDLT = true;
                if(!Files.exists(Paths.get(project.data.getMatrixMeanLogExpLevelsFilepath(dataType.name())))) {
                    if(!genExpLevelsPlotData(dataType))
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
    private void showDensityPlotImage(String filepath) {
        Image img = new Image("file:" + filepath);
        pi.setVisible(false);
        imgChart.setImage(img);
        imgChart.setVisible(true);
    }
    private boolean genExpLevelsPlotData(DataApp.DataType type) {
        boolean result = false;
        double[][] expLevels = project.data.getMeanExpressionLevels(type, project.data.getResultsTrans());
        String filepath = project.data.getMatrixMeanLogExpLevelsFilepath(type.name());
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
            String header = "";
            String[] names = project.data.getExpTypeGroupNames();
            for(String cname : names)
                header += (header.isEmpty()? "" : "\t") + cname;
            writer.write(header + "\n");
            int rows = expLevels[0].length;
            for(int row = 0; row < rows; row++) {
                String rowvals = "";
                for(int cond = 0; cond < expLevels.length; cond++)
                    rowvals += (rowvals.isEmpty()? "" : "\t") + Math.log10(expLevels[cond][row]);
                writer.write(rowvals + "\n");
            }
            result = true;
        } catch(Exception e) {
            logger.logError("Unable to write expression matrix mean log file: " + e.getMessage());
        } finally {
           try {
                if(writer != null)
                    writer.close();
           } catch (Exception e) { 
               System.out.println("Code exception closing Writer within an exception.");
           }
        }
        
        // make sure to remove bad file if needed
        if(!result)
            Utils.removeFile(Paths.get(filepath));
        return result;
    }

    //
    // Data Load
    //
    private void runDataLoadThread() {
        // get script path and run service/task
        runScriptPath = app.data.getTmpScriptFileFromResource("densityPlot.R");
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
                app.logWarning("ExpLevelDensityPlot failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("ExpLevelDensityPlot - task aborted.");
            app.ctls.alertWarning("Experiment Level Density Plot", "ExpLevelDensityPlot failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            String filepath = project.data.getMatrixMeanLogExpLevelsPlotFilepath(dataType.name());
            if(Files.exists(Paths.get(filepath))) {
                if(!plotShown) {
                    plotShown = true;
                    showDensityPlotImage(filepath);
                }
            }
            else
                app.ctls.alertWarning("Transcripts Expression Levels Density Plot", "Unable to generate expression levels density plot.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String logfilepath = project.data.getSubTabLogFilepath(subTabInfo.subTabId);
                    subTabInfo.subTabBase.outputFirstLogLine("Expression Levels Density Plot thread running...", logfilepath);

                    // setup script arguments
                    List<String> lst = new ArrayList<>();
                    lst.add(app.data.getRscriptFilepath());
                    lst.add(runScriptPath.toString());
                    String type = app.data.getDataTypeSingular(dataType.name());
                    lst.add("-d" + type.substring(0,1).toUpperCase() + type.substring(1).toLowerCase());
                    lst.add("-i" + project.data.getMatrixMeanLogExpLevelsFilepath(dataType.name()));
                    lst.add("-o" + project.data.getMatrixMeanLogExpLevelsPlotFilepath(dataType.name()));
                    subTabInfo.subTabBase.appendLogLine("Starting R script...", logfilepath);
                    logger.logDebug("Running expression levels density plot script:\n    " + lst);
                    subTabInfo.subTabBase.runScript(taskInfo, lst, "Expression Levels Density Plot", logfilepath);

                    // remove script file from temp folder
                    Utils.removeFile(runScriptPath);
                    runScriptPath = null;
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("Expression Levels Density Plot", task);
            return task;
        }
    }
}
