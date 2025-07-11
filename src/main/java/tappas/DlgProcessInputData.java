/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogEvent;
import javafx.scene.control.ProgressBar;
import javafx.stage.Window;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */

public class DlgProcessInputData extends DlgBase {
    ProgressBar pbProgress;
    Thread thread = null;
    DlgInputData.Params params;
    boolean checkClose = true;
    boolean threadResult = false;
    TaskHandler.ServiceExt service = null;
    
    public DlgProcessInputData(Project project, HashMap<String, String> hmParams, Window window) {
        super(project, window);
        this.params = new DlgInputData.Params(hmParams);
    }
    public HashMap<String, String> showAndWait() {
        if(createDialog("ProcessInputData.fxml", "Processing Input Data...", false, true, null)) {
            pbProgress = (ProgressBar) scene.lookup("#pbProgress");
            
            HashMap<String, String> hmResults = new HashMap<>();
            dialog.setResultConverter((ButtonType b) -> {
                hmResults.put("result", threadResult? "OK" : "Failed");
                return hmResults;
            });
            dialog.setOnCloseRequest((DialogEvent event) -> {
                // requires use of checkClose since dialog.close() is call from OnStopped so it's still running
                if(checkClose && service != null && service.isRunning()) {
                    if(app.ctls.alertConfirmationYN("Cancel Input Data Processing", "Do you want to abort processing input data?", ButtonType.NO)) {
                        ((IDLService) service).abortRequest = true;
                        logger.logError("Input data processing aborted by request.");
                        hmResults.put("result", "Failed");
                        service.cancel();
                    }
                    else
                        event.consume();
                }
            });
            
            dialog.setOnShown(e-> { runThread(); });
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return result.get();
            else
                hmResults.put("result", threadResult? "OK" : "Failed");
            return hmResults;
        }
        return null;
    }
    private void updateMsg(String msg) {
        Platform.runLater(() -> {
            lblMsg.setText(msg);
        });
    }
    private void updatePIDProgress(double value) {
        Platform.runLater(() -> {
            pbProgress.setProgress(value);
        });
    }

    private boolean runThread() {
        boolean result;
        
        // get script path and run service/task
        service = new IDLService();
        service.initialize();
        service.start();
        result = true;
        return result;
    }
    
    //
    // IDL service/task
    //
    private class IDLService extends TaskHandler.ServiceExt {
        boolean abortRequest = false;
        
        // do all service FX thread initialization
        @Override
        public void initialize() {
            System.out.println("initialized...");
        }
        @Override
        protected void onRunning() {
            System.out.println("running...");
        }
        @Override
        protected void onStopped() {
            // clear checkClose to avoid checking on close event
            System.out.println("stopped...");
            checkClose = false;
            dialog.close();
        }
        @Override
        protected void onFailed() {
            java.lang.Throwable e = getException();
            if(e != null)
                app.logWarning("InputData - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("InputData - task aborted.");
            app.ctls.alertWarning("Input Data", "InputData - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(project.data.hasInputData()) {
                threadResult = true;
                lblMsg.setText("Input Data Processing Completed Successfully");
            }
            else
                lblMsg.setText("Input Data Processing Failed");
        }
        
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    threadResult = false;
                    app.logInfo("Process Input Data script is running...");

                    double start = 0.0;
                    double featuresPart = 0.60;

                    // process annotation file data if needed - user provided and not previously processed
                    updateMsg("Processing annotation features...");
                    boolean aok = true;
                    if(!params.useAppRef)
                        aok = app.data.processAnnotationFeatures(project, params.getParams(), start, featuresPart, pbProgress);
                    updatePIDProgress(start + featuresPart);
                    start += featuresPart;
                    featuresPart = 0.40;

                    // process expression matrix file
                    if(aok) {
                        if(abortRequest)
                            return null;

                        // get file name
                        String emFile = params.emFilepath;
                        String edFile = params.edFilepath;
                        
                        // check if using app anotation reference file
                        boolean bok = false;
                        if(params.useAppRef) {
                            // special case if this is a demo - includes design and matrix files
                            if(params.refType.equals(DataApp.RefType.Demo)) {
                                updateMsg("Processing expression matrix...");

                                // get experimental design and expression matrix filepath
                                edFile = app.data.getDemoDesignFilepath(params.genus, params.species, params.refType.name(), params.refRelease);
                                emFile = app.data.getDemoExpMatrixFilepath(params.genus, params.species, params.refType.name(), params.refRelease);
                                // check them now and update the parameters to reflect actual file locations
                                HashMap<String, String> hmParams = params.getParams();
                                DlgInputData dlg = new DlgInputData(project, null);
                                String errmsg = dlg.checkExperimentData(params.experimentType, edFile, emFile, hmParams, logger);
                                if(errmsg.isEmpty()) {
                                    // use hmParams updated in checkExperimentData
                                    params = new DlgInputData.Params(hmParams);
                                    params.edFilepath = edFile;
                                    params.emFilepath = emFile;
                                    project.data.setParams(params);
                                }
                                else 
                                    app.logError("Expression Design/Matrix file error: " + errmsg);
                            }
                        }                    
                        if(!edFile.isEmpty() && !emFile.isEmpty()) {
                            if(abortRequest)
                                return null;

                            updateMsg("Processing expression matrix...");
                            project.data.initialize();
                            app.logInfo("Processing expression matrix file: " + emFile + "...");
                            if(project.data.genMasterExpressionFile()) {
                                app.logInfo("Expression matrix file processing completed.\n");
                                bok = true;
                            }
                            else
                                app.logError("Unable to process Expression Matrix file.");
                        }
                        else
                            app.logError("Missing experimental design or expression Matrix file path in Input Data parameters.");
                        updatePIDProgress(start + 0.025 * featuresPart);

                        if(aok && bok) {
                            if(abortRequest)
                                return null;
                            updateMsg("Generating expression factors file...");
                            if(project.data.genExpFactorsFile()) {
                                if(abortRequest)
                                    return null;
                                updatePIDProgress(start + 0.05 * featuresPart);
                                if(params.norm && params.filter){
                                    updateMsg("Filtering/normalizing expression matrix...");
                                    app.logInfo("Filtering/normalizing expression matrix...");
                                }else if(params.norm){
                                    updateMsg("Normalizing expression matrix...");
                                    app.logInfo("Normalizing expression matrix...");
                                }else if(params.filter){
                                    updateMsg("Filtering expression matrix...");
                                    app.logInfo("Filtering expression matrix...");
                                }else{
                                    updateMsg("Processing transcripts...");
                                    app.logInfo("Processing transcripts...");
                                }
                                // just one script to filter/normalize or do nothing 
                                Path runScriptPath;
                                boolean filter = false;
                                boolean norm = false;
                                runScriptPath = app.data.getTmpScriptFileFromResource("tappas_inpMatrix.R");
                                
                                if(params.norm)
                                    norm = true;                                
                                if(params.filter)
                                    filter = true;
                                
                                project.data.normalized = norm;
                                app.logInfo("Matrix script: " + runScriptPath.toString());

                                // setup script arguments
                                List<String> lst = new ArrayList<>();
                                lst.add(app.data.getRscriptFilepath());
                                app.logInfo("Rscript: " + app.data.getRscriptFilepath());
                                lst.add(runScriptPath.toString());
                                lst.add(project.data.getInputMatrixFilepath());
                                lst.add(project.data.getInputFactorsFilepath());
                                lst.add(project.data.getInputTransLenFilepath());
                                lst.add(project.data.getInputNormMatrixFilepath());
                                lst.add(filter? "" + params.filterValue : "0.0");
                                lst.add(filter? "" + params.filterCOV : "0.0");
                                lst.add(norm? "Y" : "N");
                                app.logInfo("Running ExpMatrix filter/normalization script:\n    " + lst);
                                if(runScript(taskInfo, lst, "Input matrix Filter/Normalization") == 0) {
                                    // remove script file from temp folder
                                    Utils.removeFile(runScriptPath);

                                    if(abortRequest)
                                        return null;
                                    updatePIDProgress(start + 0.2 * featuresPart);

                                    // check if transcript filtering specified
                                    HashMap<String, Object> hmFilterTrans = null;
                                    HashMap<String, Object> hmFiltered = new HashMap<>();
                                    HashMap<String, Object> hmMatrixTrans;
                                    HashMap<String, Object> hmListTrans;
                                    ArrayList<String> lstTrans;
                                    DlgInputData.Params.FilterList filterList = DlgInputData.Params.FilterList.NONE;
                                    switch(params.filterList) {
                                        case INCLUSION:
                                            app.logInfo("Filtering transcripts using inclusion list...");
                                            updateMsg("Filtering transcripts using inclusion list...");
                                            hmFilterTrans = new HashMap<>();
                                            hmListTrans = new HashMap();
                                            hmMatrixTrans = project.data.getInputMatrixFilterTrans();
                                            lstTrans = Utils.loadSingleItemListFromFile(params.lstFilepath, false);
                                            for(String trans : lstTrans)
                                                hmListTrans.put(trans,null);
                                            for(String trans : hmMatrixTrans.keySet()) {
                                                if(hmListTrans.containsKey(trans))
                                                    hmFilterTrans.put(trans, null);
                                                else
                                                    hmFiltered.put(trans, null);
                                            }
                                            app.logInfo("Filtered " + hmFilterTrans.size() + " transcripts");
                                            break;
                                        case EXCLUSION:
                                            app.logInfo("Filtering transcripts using exclusion list...");
                                            updateMsg("Filtering transcripts using exclusion list...");
                                            hmFilterTrans = new HashMap<>();
                                            hmListTrans = new HashMap();
                                            hmMatrixTrans = project.data.getInputMatrixFilterTrans();
                                            lstTrans = Utils.loadSingleItemListFromFile(params.lstFilepath, false);
                                            for(String trans : lstTrans)
                                                hmListTrans.put(trans,null);
                                            for(String trans : hmMatrixTrans.keySet()) {
                                                if(!hmListTrans.containsKey(trans))
                                                    hmFilterTrans.put(trans, null);
                                                else
                                                    hmFiltered.put(trans, null);
                                            }
                                            app.logInfo("Filtered " + hmFilterTrans.size() + " transcripts");
                                            break;
                                    }

                                    // check if we have any transcripts to work with
                                    if(hmFilterTrans == null || !hmFilterTrans.isEmpty()) {
                                        if(abortRequest)
                                            return null;
                                        // check if we filtered transcripts out
                                        if(!hmFiltered.isEmpty()) {
                                            System.out.println("Filtered " + hmFiltered.size() + " transcripts by " + filterList.name());
                                            // save list of filtered transcripts
                                            String filteredTrans = "";
                                            for(String trans : hmFiltered.keySet())
                                                filteredTrans += trans + "\n";
                                            Utils.saveTextToFile(filteredTrans, project.data.getInputMatrixFilteredTransFilepath());
                                        }

                                        // generate initial result matrix - appply trans filtering if defined
                                        updateMsg("Creating expression matrix file...");
                                        app.logInfo("Creating expression matrix file.\n");
                                        if(project.data.genResultMatrixFile(hmFilterTrans)) {
                                            if(abortRequest)
                                                return null;
                                            updatePIDProgress(start + 0.3 * featuresPart);
                                            start += 0.3 * featuresPart;
                                            featuresPart -= 0.3 * featuresPart;
                                            // Create structure file
                                            updateMsg("Creating structure information...");
                                            app.logInfo("Creating structure information...");
                                            DbAnnotation db = new DbAnnotation(project);
                                            db.initialize();
                                            if(db.openDB(project.data.getAnnotationDBFilepath())){
                                                // create structural file if it has not created yet
                                                if(!Files.exists(Paths.get(project.data.getStructuralInfoFilePath()))){
                                                    if(db.getStructureFile(project.data.getProjectDataFolder(), db.getTranscriptGenomicPosition())){
                                                        app.logInfo("File created succesfully");
                                                    }
                                                }
                                            }
                                            //UPDATE OLD ANNOT TO GET NEW WITH PAS || comment when all db have the info!!!
                                            if(!db.existStructural()){
                                                app.logInfo("Adding structural information to database...");
                                                for(int feature=1;feature<=project.data.STRUCTURAL_FEATURES.size();feature++){
                                                    updateMsg("Adding structural information (" + project.data.STRUCTURAL_FEATURES.get(feature-1) + ") to database...");
                                                    updatePIDProgress(start + (0.3 + feature*0.1) * featuresPart);
                                                    start += (0.3 + feature*0.1) * featuresPart;
                                                    featuresPart -= (0.3 + feature*0.1) * featuresPart;
                                                    db.insertNewAnnot(feature);
                                                }
                                            }
                                            // create project database
                                            updateMsg("Creating project database...");
                                            app.logInfo("Creating project database...");
                                            if(project.data.createProjectDB(start, 0.8 * featuresPart, pbProgress)) {
                                                if(abortRequest)
                                                    return null;
                                                updatePIDProgress(start + 0.8 * featuresPart);

                                                // write initial gene trans file
                                                HashMap<String, HashMap<String, Object>> hmGeneTrans = project.data.loadGeneTrans();
                                                app.logInfo("Writting Gene and Transcriptions into database...");
                                                if(project.data.writeResults(hmGeneTrans)) {
                                                    if(abortRequest)
                                                        return null;
                                                    updatePIDProgress(start + 0.9 * featuresPart);
                                                    if(project.data.writeInputDataCompletedFile())
                                                        app.logInfo("Input data processing completed successfully.");
                                                        updateMsg("Input data processing completed successfully.");
                                                    app.logInfo("Input data processing completed.");
                                                    updatePIDProgress(1.0);
                                                }
                                                else
                                                    app.logError("Unable to write results file.");
                                            }
                                            else
                                                app.logError("Unable to create project database.");
                                        }
                                        else
                                            app.logError("Unable to write project data results to file.");
                                    }
                                    else
                                        app.logError("No transcripts available after filtering.");
                                }
                                else {
                                    app.logError("Unable to filter/normalize expression matrix.");

                                    // remove script file from temp folder
                                    Utils.removeFile(runScriptPath);
                                }
                            }
                        }
                    }
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("Process Input Data", task);
            return task;
        }
    }
    protected int runScript(TaskHandler.TaskInfo taskInfo, List<String> lst, String name) {
        int exitCode = 1;
        
        // run script
        try {
            // need to add to a queue and check if app exit!!!
            ProcessBuilder pb = new ProcessBuilder(lst);
            System.out.println(lst);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            taskInfo.process = process;
            app.logInfo(name + " process started, process id: " + process.toString());

            // monitor process output
            // could change to have PB send it to a log but still need to update screen
            app.logInfo(name + " process is running...");
            String line;
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = input.readLine()) != null)
                app.logInfo(line);
            try { input.close(); } catch(IOException e) { System.out.println("Input close code exception: " + e.getMessage()); }
            exitCode = process.waitFor();
            app.logInfo(name + " script ended. Exit value: " + exitCode);
        } catch(Exception e) {
            app.logError("Unable to run " + name + " script: " + e.getMessage());
        }
        return exitCode;
    }    
}
