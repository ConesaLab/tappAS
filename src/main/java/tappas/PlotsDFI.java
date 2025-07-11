/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
/**
 *
 * @author Pedro Salguero - psalguero@cipf.es
 */
public class PlotsDFI extends AppObject {
    ProgressIndicator pi1, pi2, pi3, pi4;
    Pane paneImgChart1, paneImgChart2, paneImgChart3, paneImgChart4;
    ImageView imgChart1, imgChart2, imgChart3, imgChart4;
    
    String analysisId = "";
    boolean plotShown = false;
    Path runScriptPath = null;
    double ratio = 0.75;
    DataApp.DataType dataType;
    SubTabBase.SubTabInfo subTabInfo = null;
    TaskHandler.ServiceExt service = null;
    
    String export_name1 = "DFIBarPlot_Features";
    String export_name2 = "DFIBarPlot_Genes";
    String export_name3 = "DFIBarPlot_Bar";
    String export_name4 = "DFIBarPlot_Box";
    String png_extension = ".png";

    public PlotsDFI(Project project, String analysisId) {
        super(project, null, analysisId);
    }
    
    public void showDFIBarPlots(SubTabBase.SubTabInfo subTabInfo, ScrollPane spAll, String analysisId, boolean update){
        this.subTabInfo = subTabInfo;
        this.analysisId = analysisId;
        
        paneImgChart1 = (Pane) spAll.getContent().lookup("#paneImgChart1");
        imgChart1 = (ImageView) spAll.getContent().lookup("#imgChart1");
        pi1 = (ProgressIndicator) spAll.getContent().lookup("#piDFIBarPlot1");
        
        paneImgChart2 = (Pane) spAll.getContent().lookup("#paneImgChart2");
        imgChart2 = (ImageView) spAll.getContent().lookup("#imgChart2");
        pi2 = (ProgressIndicator) spAll.getContent().lookup("#piDFIBarPlot2");
        
        paneImgChart3 = (Pane) spAll.getContent().lookup("#paneImgChart3");
        imgChart3 = (ImageView) spAll.getContent().lookup("#imgChart3");
        pi3 = (ProgressIndicator) spAll.getContent().lookup("#piDFIBarPlot3");
        
        paneImgChart4 = (Pane) spAll.getContent().lookup("#paneImgChart4");
        imgChart4 = (ImageView) spAll.getContent().lookup("#imgChart4");
        pi4 = (ProgressIndicator) spAll.getContent().lookup("#piDFIBarPlot4");

        pi1.layoutXProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart1.widthProperty(), 2.0), 25.0));
        pi1.layoutYProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart1.heightProperty(), 2.0), 25.0));
        imgChart1.fitHeightProperty().bind(paneImgChart1.heightProperty());
        imgChart1.fitWidthProperty().bind(paneImgChart1.widthProperty());
        imgChart1.setPreserveRatio(true);
        //imgChart1.translateXProperty().bind(paneImgChart1.widthProperty().subtract(imgChart1.getLayoutX()).divide(2));
        app.ctls.setupImageExport(imgChart1, "DFI Bar Plot Features", "tappAS_" + export_name1 + analysisId + png_extension, null);
        
        pi2.layoutXProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart2.widthProperty(), 2.0), 25.0));
        pi2.layoutYProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart2.heightProperty(), 2.0), 25.0));
        imgChart2.fitHeightProperty().bind(paneImgChart2.heightProperty());
        imgChart2.fitWidthProperty().bind(paneImgChart2.widthProperty());
        imgChart2.setPreserveRatio(true);
        app.ctls.setupImageExport(imgChart2, "DFI Bar Plot Genes", "tappAS_" + export_name2 + analysisId + png_extension, null);
        
        pi3.layoutXProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart3.widthProperty(), 2.0), 25.0));
        pi3.layoutYProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart3.heightProperty(), 2.0), 25.0));
        imgChart3.fitHeightProperty().bind(paneImgChart3.heightProperty());
        imgChart3.fitWidthProperty().bind(paneImgChart3.widthProperty());
        imgChart3.setPreserveRatio(true);
        app.ctls.setupImageExport(imgChart3, "DFI Bar Plot Genes", "tappAS_" + export_name3 + analysisId + png_extension, null);
        
        pi4.layoutXProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart4.widthProperty(), 2.0), 25.0));
        pi4.layoutYProperty().bind(Bindings.subtract(Bindings.divide(paneImgChart4.heightProperty(), 2.0), 25.0));
        imgChart4.fitHeightProperty().bind(paneImgChart4.heightProperty());
        imgChart4.fitWidthProperty().bind(paneImgChart4.widthProperty());
        imgChart4.setPreserveRatio(true);
        app.ctls.setupImageExport(imgChart4, "DFI Bar Plot Genes", "tappAS_" + export_name4 + analysisId + png_extension, null);

        //Change loading and load DFI plots results
        String filepath1 = project.data.getDFIPlot1Filepath(analysisId);
        String filepath2 = project.data.getDFIPlot2Filepath(analysisId);
        String filepath3 = project.data.getDFIPlot3Filepath(analysisId);
        String filepath4 = project.data.getDFIPlot4Filepath(analysisId);
        if(!plotShown) {
            if(Files.exists(Paths.get(filepath1)) && Files.exists(Paths.get(filepath2)) && 
               Files.exists(Paths.get(filepath3)) && Files.exists(Paths.get(filepath4)) && !update) {
                plotShown = true;
                showDFIPlotsImage(filepath1, filepath2, filepath3, filepath4);
            }
            else {
                boolean runDLT = true;
                if(!Files.exists(Paths.get(project.data.getMatrixDFIFilepath(analysisId)))) {
                    runDLT = false;
                }
                // start task to create chart in R and return image
                if(runDLT)
                    runDataLoadThread();
            }
        }
    }
    
    /*public void runDFISummaryAnalysis(String analysisId){
        boolean runDLT = true;
        if(!Files.exists(Paths.get(project.data.getMatrixDFIFilepath(analysisId)))) {
            runDLT = false;
        }
        // start task to create chart in R and return image
        if(runDLT)
            runDataLoadFirstTimeThread();
    }*/
    
    //
    // Internal Functions
    //
    private void showDFIPlotsImage(String filepath1, String filepath2, String filepath3, String filepath4) {
        Image img = new Image("file:" + filepath1);
        pi1.setVisible(false);
        imgChart1.setImage(img);
        imgChart1.setVisible(true);
        
        img = new Image("file:" + filepath2);
        pi2.setVisible(false);
        imgChart2.setImage(img);
        imgChart2.setVisible(true);
        
        img = new Image("file:" + filepath3);
        pi3.setVisible(false);
        imgChart3.setImage(img);
        imgChart3.setVisible(true);

        if(project.data.isTimeCourseExpType()) {
            img = new Image("file:" + filepath4);
            pi4.setVisible(false);
            imgChart4.setImage(img);
            imgChart4.setVisible(false);
        }else{
            img = new Image("file:" + filepath4);
            pi4.setVisible(false);
            imgChart4.setImage(img);
            imgChart4.setVisible(true);
        }
    }
    
    //
    // Data Load
    //
    private void runDataLoadThread() {
        // get script path and run service/task
        //CountDownLatch latch = new CountDownLatch(1);
        runScriptPath = app.data.getTmpScriptFileFromResource("DFI_BarPlot.R");
        service = new DataLoadService();
        service.start();
    }
    private class DataLoadService extends TaskHandler.ServiceExt {
        @Override
        protected void onRunning() {
            pi1.setProgress(-1);
            pi2.setProgress(-1);
            pi3.setProgress(-1);
            pi4.setProgress(-1);
            pi1.setVisible(true);
            pi2.setVisible(true);
            pi3.setVisible(true);
            pi4.setVisible(true);
        }
        @Override
        protected void onStopped() {
            pi1.setVisible(false);
            pi2.setVisible(false);
            pi3.setVisible(false);
            pi4.setVisible(false);
            pi1.setProgress(0);
            pi2.setProgress(0);
            pi3.setProgress(0);
            pi4.setProgress(0);
        }
        @Override
        protected void onFailed() {
            java.lang.Throwable e = getException();
            if(e != null)
                app.logWarning("DFI Bar Plot failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("DFI Bar Plot - task aborted.");
            app.ctls.alertWarning("DFI Bar Plot", "ScatterPlot failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            String filepath1 = project.data.getDFIPlot1Filepath(analysisId);
            String filepath2 = project.data.getDFIPlot2Filepath(analysisId);
            String filepath3 = project.data.getDFIPlot3Filepath(analysisId);
            String filepath4 = project.data.getDFIPlot4Filepath(analysisId);
            if(Files.exists(Paths.get(filepath1)) && Files.exists(Paths.get(filepath2)) &&
               Files.exists(Paths.get(filepath3)) && Files.exists(Paths.get(filepath4))) {
                if(!plotShown) {
                    plotShown = true;
                    showDFIPlotsImage(filepath1, filepath2, filepath3, filepath4);
                }
            }
            else
                app.ctls.alertWarning("DFI Bar Plot", "Unable to generate DFI plots.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String logfilepath = project.data.getSubTabLogFilepath(subTabInfo.subTabId);
                    subTabInfo.subTabBase.outputFirstLogLine("DFI Bar Plot Running...", logfilepath);
                    //Creating lists
                    String trans = "";
                    String proteins = "";
                    
                    HashMap<String, HashMap<String, String>> hmType = project.data.getAFStatsType();
                    
                    for(String db : hmType.keySet()){
                        HashMap<String, String> hmFeat = hmType.get(db);
                        for(String feat : hmFeat.keySet()){
                            if(hmFeat.get(feat).equals("T")){
                                if(trans.equals(""))
                                    trans = feat;
                                else
                                    trans = trans + "," + feat;
                            }else if(hmFeat.get(feat).equals("P")){
                                if(proteins.equals(""))
                                    proteins = feat;
                                else
                                    proteins = proteins + "," + feat;
                            }
                        }
                    }
                    
                    // setup script arguments
                    List<String> lst = new ArrayList<>();
                    lst.add(app.data.getRscriptFilepath());
                    lst.add(runScriptPath.toString());
                    lst.add("-i" + project.data.getMatrixDFIFilepath(analysisId));
                    lst.add("-a" + analysisId);
                    lst.add("-c" + project.data.getDFITotalFeaturesFilepath(analysisId));
                    lst.add("-lt" + trans);
                    lst.add("-lp" + proteins);
                    lst.add("-o1" + project.data.getDFIPlot1Filepath(analysisId));
                    lst.add("-o2" + project.data.getDFIPlot2Filepath(analysisId));
                    lst.add("-o3" + project.data.getDFIPlot3Filepath(analysisId));
                    lst.add("-o4" + project.data.getDFIPlot4Filepath(analysisId));
                    lst.add("-t1" + project.data.getDFITestFeaturesFilepath(analysisId));
                    lst.add("-t2" + project.data.getDFITestGenesFilepath(analysisId));

                    /*//DONT WORK IN WINDOWS
                    List<String> newlst = new ArrayList<>();
                    if(Utils.isWindowsOS()) {
                        for (String i : lst) {
                            if (i.startsWith("-"))
                                newlst.add(i.replace("\\", "/"));
                            else
                                newlst.add(i);
                        }
                        lst = newlst;
                    }

                    System.out.println(lst);*/

                    subTabInfo.subTabBase.appendLogLine("Starting R script...", logfilepath);
                    logger.logDebug("Running bar plot script:\n    " + lst);
                    subTabInfo.subTabBase.runScript(taskInfo, lst, "DFI Bar Plot for " + analysisId, logfilepath);

                    // remove script file from temp folder
                    Utils.removeFile(runScriptPath);
                    runScriptPath = null;
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("DFI Bar Plot for " + analysisId, task);
            return task;
        }
    }
    
    
    /*private void runDataLoadFirstTimeThread() {
        // get script path and run service/task
        //CountDownLatch latch = new CountDownLatch(1);
        runScriptPath = app.data.getTmpScriptFileFromResource("DFI_BarPlot.R");
        service = new DataLoadFirstTime();
        service.start();
    }
    private class DataLoadFirstTime extends TaskHandler.ServiceExt {
        @Override
        protected void onRunning() {
            logger.logDebug("Running...");
        }
        
        @Override
        protected void onStopped() {
            logger.logDebug("Stopped...");
        }
        
        @Override
        protected void onFailed() {
            java.lang.Throwable e = getException();
            if(e != null)
                app.logWarning("DFI Bar Plot " + analysisId + " failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("DFI Bar Plot " + analysisId + " - task aborted.");
            app.ctls.alertWarning("DFI Bar Plot " + analysisId, "ScatterPlot failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            String filepath1 = project.data.getDFIPlot1Filepath(analysisId);
            String filepath2 = project.data.getDFIPlot2Filepath(analysisId);
            String filepath3 = project.data.getDFIPlot3Filepath(analysisId);
            String filepath4 = project.data.getDFIPlot4Filepath(analysisId);
            if(Files.exists(Paths.get(filepath1)) && Files.exists(Paths.get(filepath2)) &&
               Files.exists(Paths.get(filepath3)) && Files.exists(Paths.get(filepath4))) {
                logger.logDebug("Complete...");
            }
            else
                app.ctls.alertWarning("DFI Bar Plot " + analysisId, "Unable to generate expression levels density plot.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String logfilepath = project.data.getDFILogFilepath(analysisId);
                    //Creating lists
                    String trans = "";
                    String proteins = "";

                    HashMap<String, HashMap<String, String>> hmType = project.data.getAFStatsType();

                    for(String db : hmType.keySet()){
                        HashMap<String, String> hmFeat = hmType.get(db);
                        for(String feat : hmFeat.keySet()){
                            if(hmFeat.get(feat).equals("T")){
                                if(trans.equals(""))
                                    trans = feat;
                                else
                                    trans = trans + "\t" + feat;
                            }else if(hmFeat.get(feat).equals("P")){
                                if(proteins.equals(""))
                                    proteins = feat;
                                else
                                    proteins = proteins + "\t" + feat;
                            }
                        }
                    }

                    // setup script arguments
                    List<String> lst = new ArrayList<>();
                    lst.add(app.data.getRscriptFilepath());
                    lst.add(runScriptPath.toString());
                    lst.add("-i" + project.data.getMatrixDFIFilepath(analysisId));
                    lst.add("-a" + analysisId);
                    lst.add("-c" + project.data.getDFITotalFeaturesFilepath(analysisId));
                    lst.add("-lt" + trans);
                    lst.add("-lp" + proteins);
                    lst.add("-o1" + project.data.getDFIPlot1Filepath(analysisId));
                    lst.add("-o2" + project.data.getDFIPlot2Filepath(analysisId));
                    lst.add("-o3" + project.data.getDFIPlot3Filepath(analysisId));
                    lst.add("-o4" + project.data.getDFIPlot4Filepath(analysisId));
                    lst.add("-t1" + project.data.getDFITestFeaturesFilepath(analysisId));
                    lst.add("-t2" + project.data.getDFITestGenesFilepath(analysisId));
                    logger.logDebug("Running bar plot script:\n    " + lst);
                    System.out.println(lst);
                    // remove script file from temp folder
                    Utils.removeFile(runScriptPath);
                    runScriptPath = null;
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("DFI Bar Plot " + analysisId, task);
            return task;
        }
    }*/
    
}
