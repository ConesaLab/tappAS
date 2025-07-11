/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import tappas.DataApp.DataType;
import tappas.DbProject.AFStatsData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DataProject extends AppObject {
    //normalized
    
    public boolean normalized = false;
    
    public static final String FOLDER_PROJECT = "Project.";
    public static String getProjectFolderName(String id) { return(FOLDER_PROJECT + id + DataApp.APP_FOLDER_EXT); }
    public static String getProjectNamePrefix() { return FOLDER_PROJECT; }
    public static String getProjectNameExt() { return DataApp.APP_FOLDER_EXT; }
    public static String getProjectFolderFilename(String id) { return(FOLDER_PROJECT + id + DataApp.APP_FOLDER_EXT); }
    
    // Project folders
    public static final String FOLDER_ID = "InputData";
    public static final String FOLDER_DATA = "Data";
    // Data sub folders
    public static final String FOLDER_CONTENT = "Content";
    // Functional Diversity Analysis
    public static final String FOLDER_FDA = "FDA";
    // Differential Analysis
    public static final String FOLDER_DEA = "DEA";
    public static final String FOLDER_DIU = "DIU";
    // Differential Feature Inclusion  DIU
    public static final String FOLDER_DFI = "DFI";
    // Differential Alternative PolyA Analysis
    public static final String FOLDER_DPA = "DPA";
    // UTRLengthening
    public static final String FOLDER_UTRL = "UTRL";
    // Enrichment Analysis
    public static final String FOLDER_GSEA = "GSEA";
    public static final String FOLDER_FEA = "FEA";
    
    public static final String CHECK_PACKAGES = "check_packages";

    // InputData files
    public static final String EXP_DESIGN = "experimental_design.tsv";
    public static final String EXP_MATRIX = "expression_matrix.tsv";
    public static final String INPUT_MATRIX = "input_matrix.tsv";
    public static final String ORIGINAL_MATRIX = "original_matrix.tsv";
    public static final String INPUT_MATRIX_FILTERED_TRANS = "input_matrix_filtered_trans.tsv";
    public static final String INPUT_MATRIX_NATRANS = "input_matrix_NA_trans.tsv";
    public static final String INPUT_MATRIX_NATROW = "input_matrix_NA_row.tsv";
    public static final String INPUT_NORM_MATRIX = "input_normalized_matrix.tsv";
    public static final String RESULT_MATRIX = "result_matrix.tsv";
    public static final String EXP_FACTORS = "exp_factors.txt";
    public static final String TIME_FACTORS = "time_factors.txt";
    public static final String ID_COMPLETED = "done_inputdata.txt";
    public static final String TRANS_DBCATIDS = "trans_db_cat_ids.tsv";
    public static final String ANNOTATION_FEATURE_SIZES = "feature_sizes.tsv";
    public static final String ANNOTATION_BLOCK_FEATURES = "block_features.tsv";
    public static final String ANNOTATION_GENEDESC = "gene_descriptions.tsv";
    public static final String ANNOTATION_PROTEINDESC = "protein_descriptions.tsv";
    public static final String ALIGNMENT_CATEGORIES = "alignment_cats.tsv";
    public static final String ANNOTATION_FILE = "annotations.gff3";
    public static final String ANNOTATION_IDX = "annotations.idx";
    
    // Project level files used for DEA/DIU and DFI
    public static final String TRANS_MATRIX = "transcript_matrix.tsv";
    public static final String MATRIXMEANLOG_NAME = "_matrix_meanLog";
    public static final String TRANS_MATRIX_RAW = "transcript_matrix_raw.tsv";
    public static final String PROT_MATRIX = "protein_matrix.tsv";
    public static final String PROT_MATRIX_RAW = "protein_matrix_raw.tsv";
    public static final String GENE_MATRIX = "gene_matrix.tsv";
    public static final String GENE_MATRIX_RAW = "gene_matrix_raw.tsv";
    public static final String GENEPROT_MATRIX = "gene_protein_matrix.tsv";
    public static final String GENEPROT_MATRIX_RAW = "gene_protein_matrix_raw.tsv";
    public static final String TRANS_LENGTHS = "transcript_lengths.tsv";
    public static final String PROT_LENGTHS = "protein_lengths.tsv";
    public static final String GENE_LENGTHS = "gene_lengths.tsv";
    public static final String GENE_TRANS = "gene_transcripts.tsv";
    public static final String GENE_PROTEINS = "gene_proteins.tsv";
    //FDA
    public static final String FDA_COMBINED_PLOT_NAME = "_FDA_CombinedResults";
    //DIU
    public static final String SCATTER_PLOT_NAME = "_scatter_plot";
    public static final String MATRIX_DIU = "_diu_matrix";
    //DPA
    public static final String HEATMAP_NAME = "heatmap_plot";
    //UTR Lengthening
    public static final String UTRBOX_NAME = "utr_plot";

    public static final String DFI_PLOT1_NAME = "dfi_bar_plot1";
    public static final String DFI_PLOT2_NAME = "dfi_bar_plot2";
    public static final String DFI_PLOT3_NAME = "dfi_bar_plot3";
    public static final String DFI_PLOT4_NAME = "dfi_bar_plot4";
    public static final String MATRIX_DFI = "dfi_matrix";
    public static final String DFI_TOTAL_FEATURES = "dfi_total_features";
    public static final String DFI_TEST_FEATURES = "dfi_test_features";
    public static final String DFI_TEST_GENES = "dfi_test_genes";
    
    // Project level files used for DAPA
    public static final String STRUCTURAL_INFO = "structural_info.tsv";
    public static final List<String> STRUCTURAL_FEATURES = Arrays.asList("3'UTR Length", "5'UTR Length", "CDS Length", "PolyAdenylation Site");
    public static final String DPA_ShortLong = "dpa_info.tsv";
    public static final String UTRL_ShortLong = "utrl_info.tsv";
    public static final String UTRL_PVAL = "result_pval.tsv";
    // RankedLists for GSEA - want to keep file names different from FEA
    public static final String RANKEDLIST_LENGTHS = "RLlengths";
    public static final String RANKEDLIST_VALUES = "RLvalues";
    // Test and Background lists for FEA
    public static final String LIST_LENGTHS = "lengths";
    public static final String LIST_TEST = "test";
    public static final String LIST_BKGND = "bkgnd";
    // PCA
    public static final String MATRIX_PCA_DATA = "matrix_pca.tsv";
    
    //Chormosome Data 
    static private final List<String> CHR_SPECIES = Arrays.asList("Mus_musculus", "Homo_sapiens");
    public List<String> getChromoDataSpecies() {return CHR_SPECIES;}

    //analysis path
    public String getDFIFolder(){return Paths.get(getProjectFolder(), FOLDER_DFI).toString(); }

    // file paths and names
    public String getProjectFolderName() { return(getProjectFolderName(project.getProjectId())); }
    public String getProjectFolder() { return Paths.get(app.data.getAppProjectsFolder(), getProjectFolderName()).toString(); }
    public String getProjectToolLogFilepath(String tool) { return Paths.get(getProjectFolder(), tool + "_" + DataApp.LOG_NAME).toString(); }
    public String getProjectToolJpgFilepath(String tool) { return Paths.get(getProjectFolder(), tool + DataApp.JPG_EXT).toString(); }
    public String getInputDataFolder() { return Paths.get(getProjectFolder(), FOLDER_ID).toString(); }
    public String getProjectDataFolder() { return Paths.get(getProjectFolder(), FOLDER_DATA).toString(); }
    public String getResultsFilepath() { return Paths.get(getProjectDataFolder(), DataApp.RESULTS_GENE_TRANS).toString(); }
    public String getResultsGeneProtFilepath() { return Paths.get(getProjectDataFolder(), DataApp.RESULTS_GENE_PROT).toString(); }
    public String getContentFolder() { return Paths.get(getProjectDataFolder(), FOLDER_CONTENT).toString(); }
    public String getSubTabLogFilepath(String subTab) { return Paths.get(getContentFolder(), subTab.toLowerCase() + "_" + DataApp.LOG_NAME).toString(); }
    public String getMatrixMeanLogExpLevelsFilepath(String dataType) { return Paths.get(getContentFolder(), dataType.toLowerCase() + MATRIXMEANLOG_NAME + DataApp.TSV_EXT).toString(); }
    public String getMatrixMeanLogExpLevelsPlotFilepath(String dataType) { return Paths.get(getContentFolder(), dataType.toLowerCase() + MATRIXMEANLOG_NAME + DataApp.PNG_EXT).toString(); }
    public String getMatrixPCAFilepath() { return Paths.get(getContentFolder(), MATRIX_PCA_DATA).toString(); }
    // Check Packages
    public String getCheckPackagesFilepath() { return Paths.get(getContentFolder(), CHECK_PACKAGES + DataApp.TEXT_EXT).toString(); }
    //FDA
    public String getFDACombinedResultsPlotFilepath() { return Paths.get(getContentFolder(), "tappAS" + FDA_COMBINED_PLOT_NAME + DataApp.PNG_EXT).toString(); }
    
    // DIU graph
    public String getScatterPlotFilepath(String dataType) { return Paths.get(getContentFolder(), dataType.toLowerCase() + SCATTER_PLOT_NAME + DataApp.PNG_EXT).toString(); }
    public String getMatrixDIUFilepath(String dataType) { return Paths.get(getContentFolder(), dataType.toLowerCase() + MATRIX_DIU + DataApp.TSV_EXT).toString(); }
    // DFI graph
    public String getDFILogFilepath(String id) { return Paths.get(getDFIFolder(), DataApp.LOG_PREFIXID + id + DataApp.LOG_EXT).toString(); }
    public String getDFIPlot1Filepath(String id) { return Paths.get(getContentFolder(), DFI_PLOT1_NAME + "." + id + DataApp.PNG_EXT).toString(); }
    public String getDFIPlot2Filepath(String id) { return Paths.get(getContentFolder(), DFI_PLOT2_NAME + "." + id + DataApp.PNG_EXT).toString(); }
    public String getDFIPlot3Filepath(String id) { return Paths.get(getContentFolder(), DFI_PLOT3_NAME + "." + id + DataApp.PNG_EXT).toString(); }
    public String getDFIPlot4Filepath(String id) { return Paths.get(getContentFolder(), DFI_PLOT4_NAME + "." + id + DataApp.PNG_EXT).toString(); }
    public String getMatrixDFIFilepath(String id) { return Paths.get(getContentFolder(), MATRIX_DFI + "." + id + DataApp.TSV_EXT).toString(); }
    public String getDFITotalFeaturesFilepath(String id) { return Paths.get(getContentFolder(), DFI_TOTAL_FEATURES + "." + id + DataApp.TSV_EXT).toString(); }
    public String getDFITestFeaturesFilepath(String id) { return Paths.get(getContentFolder(), DFI_TEST_FEATURES + "." + id + DataApp.TSV_EXT).toString(); }
    public String getDFITestGenesFilepath(String id) { return Paths.get(getContentFolder(), DFI_TEST_GENES + "." + id + DataApp.TSV_EXT).toString(); }
    // DPA graph
    public String getDPAHeatmapFilepath() { return Paths.get(getContentFolder(), HEATMAP_NAME + DataApp.PNG_EXT).toString(); }

    // UTR graph
    public String getUTRPlotFilepath() { return Paths.get(getContentFolder(), UTRBOX_NAME + DataApp.PNG_EXT).toString(); }
    public HashMap<String, String> getUTRPVal() { return Utils.loadSingleStringTSVListFromFile(Paths.get(project.data.getProjectDataFolder(),"UTRL",UTRL_PVAL).toString(), true); }

    public String getProjectParamFilepath() { return Paths.get(getProjectFolder(), DataApp.PRM_PROJECT).toString(); }
    public String getProjectIDParamFilepath() { return Paths.get(getInputDataFolder(), DataApp.PRM_PROJECT_DATA).toString(); }

    // project annotation file names
    public String getAnnotationFilepath() { return annotationFilepath; }
    public String getAnnotationDBFilepath() { return getAnnotationFilepath().isEmpty()? "" : Paths.get(getAnnotationFilepath(), DataApp.ANNOTATION_DB).toString(); }
    public String getProjectDBFilepath() { return Paths.get(getProjectFolder(), DataApp.PROJECT_DB).toString(); }

    // file names
    public String getAnnotationBlockFeaturesFilepath() { return Paths.get(getProjectDataFolder(), ANNOTATION_BLOCK_FEATURES).toString(); }
    public String getResultMatrixFilepath() {  return( Paths.get(getProjectDataFolder(), RESULT_MATRIX).toString()); }
    public String getOriginalMatrixFilepath() { return Paths.get(getInputDataFolder(), ORIGINAL_MATRIX).toString(); }
    public String getDesignFilepath() { return Paths.get(getInputDataFolder(), EXP_DESIGN).toString(); }
    public String getInputMatrixFilepath() { return Paths.get(getInputDataFolder(), INPUT_MATRIX).toString(); }
    public String getInputNormMatrixFilepath() { return Paths.get(getInputDataFolder(), INPUT_NORM_MATRIX).toString(); }
    public String getInputFactorsFilepath() { return Paths.get(getInputDataFolder(), EXP_FACTORS).toString(); }
    public String getInputTimeFactorsFilepath() { return Paths.get(getInputDataFolder(), TIME_FACTORS).toString(); }
    public String getInputTransLenFilepath() { return Paths.get(getAnnotationFilepath(), TRANS_LENGTHS).toString(); }
    public String getInputMatrixNATransFilepath() { return Paths.get(getInputDataFolder(), INPUT_MATRIX_NATRANS).toString(); }
    public String getInputMatrixFilteredTransFilepath() { return Paths.get(getInputDataFolder(), INPUT_MATRIX_FILTERED_TRANS).toString(); }
    // not really open to external change - single global file for now - see note in AnnotationFileDefs
    public String getAnnotationDefFilepath() { return app.data.getAppAnnotationDefFilepath(); }

    // files for data analysis input
    public String getExpFactorsFilepath() { return Paths.get(project.data.getProjectDataFolder(), EXP_FACTORS).toString(); }
    public String getTimeFactorsFilepath() { return Paths.get(project.data.getProjectDataFolder(), TIME_FACTORS).toString(); }
    public String getGeneTransFilePath() { return Paths.get(project.data.getProjectDataFolder(), GENE_TRANS).toString(); }
    public String getGeneProteinsFilePath() { return Paths.get(project.data.getProjectDataFolder(), GENE_PROTEINS).toString(); }
    public String getStructuralInfoFilePath() { return Paths.get(project.data.getProjectDataFolder(), STRUCTURAL_INFO).toString(); }
    public String getGeneMatrixFilepath() { return Paths.get(project.data.getProjectDataFolder(), GENE_MATRIX).toString(); }
    public String getGeneMatrixRawFilepath() { return Paths.get(project.data.getProjectDataFolder(), GENE_MATRIX_RAW).toString(); }
    public String getTranscriptMatrixFilepath() { return Paths.get(project.data.getProjectDataFolder(), TRANS_MATRIX).toString(); }
    public String getTranscriptMatrixRawFilepath() { return Paths.get(project.data.getProjectDataFolder(), TRANS_MATRIX_RAW).toString(); }
    public String getProteinMatrixFilepath() { return Paths.get(project.data.getProjectDataFolder(), PROT_MATRIX).toString(); }
    public String getProteinMatrixRawFilepath() { return Paths.get(project.data.getProjectDataFolder(), PROT_MATRIX_RAW).toString(); }
    public String getGeneProteinMatrixFilepath() { return Paths.get(project.data.getProjectDataFolder(), GENEPROT_MATRIX).toString(); }
    public String getGeneProteinMatrixRawFilepath() { return Paths.get(project.data.getProjectDataFolder(), GENEPROT_MATRIX_RAW).toString(); }
    
    // application objects
    AnnotationFileDefs fileDefs;
    String annotationFilepath = "";
    private DataAnnotation dataAnnotation;
    private DbAnnotation dbAnnotation;
    private DbProject dbProject;
    DataInputMatrix dataInputMatrix;
    private DataExpMatrix dataExpMatrix;
    public Analysis analysis;
    private boolean dataLoading = false;
    private boolean dataLoaded = false;
    public boolean isDataLoaded() { return dataLoaded; }
    public boolean isDataLoading() { return dataLoading; }
    public DataAnnotation getDataAnnotation() { return dataAnnotation; }
    public Analysis getAnalysis() { return analysis; }
    public AnnotationFileDefs getAnnotationFileDefs() { return fileDefs; }
    
    // class data
    public static String rscriptPath = "Rscript";
    DlgInputData.Params params = new DlgInputData.Params(new HashMap<>());
    HashMap<String, HashMap<String, Object>> hmResultsGeneTrans = new HashMap<>();
    HashMap<String, Object> hmResultsTrans = new HashMap<>();
    
    public DataProject(Project project) {
        super(project, null);
// TODO: remove from here later?...  but try to figure out why it was placed in the constructor to begin with!!!
        this.analysis = new Analysis(project);
        
    }
    public void initialize() throws Exception {
        logger.logDebug("Initializing data for project '" + project.getProjectName() + "' (" + project.getProjectId() + ")...");

        // create folders if needed
        Utils.createFolder(getInputDataFolder());
        Utils.createFolder(getProjectDataFolder());
        Utils.createFolder(getContentFolder());
        
        // load input data parameters
        params = DlgInputData.Params.load(getProjectIDParamFilepath());
        normalized = params.norm;

        //if annotation file change in the folder the project can be loaded because tappAS has a list with all references and its sizes!!!
        annotationFilepath = app.data.getAnnotationFeaturesFolder(params);
        if(annotationFilepath.isEmpty()){
            app.logInfo("The custom annotation file has been modified. We are going to load the version you used to create the project.");
            annotationFilepath = app.data.getAnnotationFeaturesFolderWithoutSize(params);
            if(annotationFilepath == null){
                app.logInfo("We can not find your annotation. The projact can not be loaded.");
                return;
            }else{
                app.logInfo("Annotation loaded correctly. Check if the project contains the correct annotation or you want to create a new one.");
            }
        }
        hmResultsGeneTrans = readResults();
        hmResultsTrans.clear();
        for(HashMap<String, Object> hm : hmResultsGeneTrans.values()) {
            for(String trans : hm.keySet())
                hmResultsTrans.put(trans, null);
        }

        // initialize annotation objects
        loadAnnotationsFileDefs();
        dataAnnotation = new DataAnnotation(project);
        dataAnnotation.initialize();
        dbAnnotation = new DbAnnotation(project);
        dbAnnotation.initialize();
        dbProject = new DbProject(project);
        dbProject.initialize();
        
        // initialize input data expression matrix object
        dataInputMatrix = new DataInputMatrix(project);
        dataInputMatrix.initialize();

        // initialize project expression matrix object
        dataExpMatrix = new DataExpMatrix(project);
        dataExpMatrix.initialize();

        // initialize analysis object
        analysis.initialize();
        
        // load initial data if available - change this later to not happen here!!!
        // ideally to not happen at all until some data is needed
        initializeData();
        
        logger.logDebug("Project data initialization completed.");
    }
    // must have already created/downloaded annotation DB
    private boolean initializeData() {
        boolean result = false;
        // check if we already have the annotation data path
        // it is possible to run without having the data yet
        // if the code tried to access the data then it would fail
        if(!getAnnotationFilepath().isEmpty()) {
            logger.logDebug("AnnotFile: " + getAnnotationFilepath());
            logger.logDebug("AnnotDB: " + getAnnotationDBFilepath());
            // check if annotation DB already created, otherwise don't try to open it
            System.out.println("path: " + Paths.get(getAnnotationDBFilepath()).toString());
            if(Files.exists(Paths.get(getAnnotationDBFilepath()))) {
                if(dbAnnotation.openDB(getAnnotationDBFilepath())) {
                    logger.logDebug("ProjectDB: " + getProjectDBFilepath());
                    // check if project DB already created, otherwise don't try to open it
                    // return true since the project creation is taking place
                    if(Files.exists(Paths.get(getProjectDBFilepath()))) {
                        result = dbProject.openDB(getProjectDBFilepath());
                        if(result) {
                            // create structural file if it has not created yet
                            //if(!Files.exists(Paths.get(getStructuralInfoFilePath()))){
                            //    dbAnnotation.getStructureFile(project.data.getProjectDataFolder(), dbAnnotation.getTranscriptGenomicPosition());
                            //}
                            // start data load on background task, will set flags accordingly
                            runLoadData();
                        }
                    }
                    else
                        result = true;
                }
            }
        }
        return result;
    }
    
    //
    // Project Data Functions
    //
    
    public boolean hasResults() {
        boolean has = false;
        String filepath = getResultsFilepath();
        if(Files.exists(Paths.get(filepath)))
            has = true;
        return has;
    }    
    public HashMap<String, HashMap<String, Object>> getResultsGeneTrans() { return hmResultsGeneTrans; }
    public HashMap<String, Object> getResultsGenes() { 
        HashMap<String, Object> hmGenes = new HashMap<>();
        for(String gene : hmResultsGeneTrans.keySet())
            hmGenes.put(gene, null);
        return hmGenes; 
    }
    public HashMap<String, Object> getResultsTrans() { return hmResultsTrans; }
    public HashMap<String, Object> getResultsProteins() {
        HashMap<String, Object> hmResultsProteins = new HashMap<>();
        for(String trans : hmResultsTrans.keySet()) {
            String protein = getTransProtein(trans).trim();
            if(!protein.isEmpty())
                hmResultsProteins.put(protein, null);
        }
        return hmResultsProteins;
    }
    public HashMap<String, HashMap<String, Object>> readResults() {
        hmResultsGeneTrans.clear();
        hmResultsTrans.clear();
        String filepath = getResultsFilepath();
        try {
            if(Files.exists(Paths.get(filepath))) {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                for(String line : lines) {
                    String[] fields = line.split("\t");
                    if(fields.length == 2) {
                        if(!hmResultsGeneTrans.containsKey(fields[0].trim())) {
                            HashMap<String, Object> hm = new HashMap<>();
                            hm.put(fields[1].trim(), null);
                            hmResultsGeneTrans.put(fields[0].trim(), hm);
                        }
                        else
                            hmResultsGeneTrans.get(fields[0].trim()).put(fields[1].trim(), null);
                    }
                    else if(fields.length != 1){
                        logger.logError("Invalid line, " + line + ", in file '" + filepath + "'");
                        hmResultsGeneTrans.clear();
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load project results file '" + filepath + "': " + e.getMessage());
            hmResultsGeneTrans.clear();
        }
        for(HashMap<String, Object> hm : hmResultsGeneTrans.values()) {
            for(String trans : hm.keySet())
                hmResultsTrans.put(trans, null);
        }
        
        return hmResultsGeneTrans;
    }
    public boolean writeResults(HashMap<String, HashMap<String, Object>> hmGeneTrans) {
        boolean result = false;
        hmResultsGeneTrans.clear();
        hmResultsTrans.clear();
        Writer writer = null;
        String filepath = getResultsFilepath();
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath), "utf-8"));
            for(String gene : hmGeneTrans.keySet()) {
                HashMap<String, Object> hmTrans = hmGeneTrans.get(gene);
                for(String trans : hmTrans.keySet())
                    writer.write(gene + "\t" + trans + "\n");
            }
            hmResultsGeneTrans = hmGeneTrans;
            for(HashMap<String, Object> hm : hmResultsGeneTrans.values()) {
                for(String trans : hm.keySet())
                    hmResultsTrans.put(trans, null);
            }
            result = true;
        } catch (IOException e) {
            logger.logError("Unable to save project file '" + filepath +"': " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }
        return result;
    }
    public void removeResultFile() {
        Path filePath = Paths.get(getResultsFilepath());
        if(Files.exists(filePath))
            Utils.removeFile(filePath);
    }
    public void removeResultMatrixFile() {
        Path filePath = Paths.get(getResultMatrixFilepath());
        if(Files.exists(filePath))
            Utils.removeFile(filePath);
    }
    
    
    //
    // Project Settings Functions
    //
    
    public String getGenus() {
        return params.genus;
    }
    public String getSpecies() {
        return params.species;
    }
    public DataApp.RefType getRefType() {
        return params.refType;
    }
    public DataApp.ExperimentType getExperimentType() {
        return params.experimentType;
    }
    public boolean isMultipleTimeSeriesExpType() {
        return params.experimentType.equals(DataApp.ExperimentType.Time_Course_Multiple);
    }
    public boolean isSingleTimeSeriesExpType() {
        return params.experimentType.equals(DataApp.ExperimentType.Time_Course_Single);
    }
    public boolean isTimeCourseExpType() {
        return (isSingleTimeSeriesExpType() || isMultipleTimeSeriesExpType());
    }
    public boolean isCaseControlExpType() {
        return params.experimentType.equals(DataApp.ExperimentType.Two_Group_Comparison);
    }
    
    //
    // Data Load
    //
    
    private void runLoadData() {
        // run background code to load initial project data
        Thread thread = new LoadDataThread();
        thread.start();
    }

    private class LoadDataThread extends Thread {
        @Override
        public void run(){
            dataLoading = true;
            logger.logDebug("Load project data thread running");
            try {
                if(dbAnnotation.openDB(getAnnotationDBFilepath())) {
                    // check if project DB already created, otherwise don't try to open it
                    if(Files.exists(Paths.get(getProjectDBFilepath()))) {
                        boolean result = dbProject.openDB(getProjectDBFilepath());
                        if(result) {
                            // start data load on background task
                            result = dataAnnotation.loadBaseData();
                            dataLoading = false;
                            dataLoaded = result;
                            if(dataLoaded) {
                                Platform.runLater(() -> {
                                    app.ctlr.projectDataLoaded(project);
                                });                                    
                            }
                        }
                    }
                    //if(!Files.exists(Paths.get(getProjectDBFilepath()))) {
                    //    dbAnnotation.getStructureFile(project.getProjectFolder()+"/Data/", dbAnnotation.getTranscriptGenomicPosition());
                    //}
                }
            } catch(Exception e) {
                app.logger.logError("Unable to load project data: " + e.getMessage());
            }
            logger.logDebug("Load project data thread exiting.");
        }
    }
    
    
    
    // get all annotation transcripts from dbAnnotation
    // only use for generating the master expression matrix
    // since at that point the project DB has not been created
    public HashMap<String, Object> getAnnotationTrans(){
        return dbAnnotation.loadTrans();
    }
    
    public boolean createProjectDB(double offset, double featuresPart, ProgressBar pbProgress){
        boolean result = false;
        // create project DB
        dbProject = new DbProject(project);
        dbProject.initialize();
        
        // get the list of transcripts after all the filtering (expression and transcripts data) is done
        ArrayList<String> lstTrans = Utils.loadSingleItemListFromFile(getResultMatrixFilepath(), true);
        if(!lstTrans.isEmpty()) {
            if(dbProject.createDB(getProjectDBFilepath(), Paths.get(getAnnotationFilepath(), DataApp.ANNOTATION_DB).toString(), lstTrans, offset, featuresPart, pbProgress))
                result = true;
        }
        return result;
    }
    // wipes out all the project data - in the data folder
    // called when reloading input data - all data will be affected
    public void removeProjectData() {
        app.data.removeFolder(Paths.get(getProjectDataFolder()));
        Utils.createFolder(getProjectDataFolder());
        analysis.createFolders();
    }
    public void removeProjectDB() {
        Utils.removeFile(Paths.get(getProjectDBFilepath()));
    }
    public boolean hasResultMatrixFile() {
        return Files.exists(Paths.get(getResultMatrixFilepath()));
    }
    public void clearData() {
    }
    public void clearDataFolder(boolean rmvprm) {
        Utils.removeAllFolderFiles(Paths.get(getProjectDataFolder()), rmvprm);
    }
    public void clearInputDataFolder(boolean rmvprm) {
        Utils.removeAllFolderFiles(Paths.get(getInputDataFolder()), rmvprm);
    }
    // get annotation file definitions
    private void loadAnnotationsFileDefs() throws Exception {
        // load annotation file definition if not already done
        if(fileDefs == null) {
            // load file, will throw exception
            fileDefs = new AnnotationFileDefs(project, null);
            // see note in AnnotationFileDefs
            fileDefs.loadDefinitions(getAnnotationDefFilepath());
        }
    }    
    
    //
    // Project Parameters Data Functions
    //
    //public HashMap<String, String> getIDParams() { return IDParams; }
    public DlgInputData.Params getParams() { return params; }
    public void setParams(DlgInputData.Params params) { 
        this.params = params; 
        params.save(getProjectIDParamFilepath());
    }
    /*
    public void setIDParams(HashMap<String, String> hmp) {
        IDParams = hmp;
        if(IDParams != null)
            Utils.saveParams(IDParams, getProjectIDParamFilepath());
    }*/
    public static String getProjectIdHash(String name) {
        String hashCode = name.hashCode() + "";
        if(hashCode.charAt(0) == '-')
            hashCode = hashCode.substring(1);
        return(FOLDER_PROJECT + hashCode + DataApp.APP_FOLDER_EXT);
    }

    //
    // Project Data
    //

    //
    // Expression Matrix
    //
    //
    // result expression matrix interface
    //
    public double[][] getMeanExpressionLevels(DataType type, HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.getMeanExpressionLevels(type, hmFilterTrans);
    }
    public HashMap<String, double[]> getMeanExpressionLevelsHM(DataType type, HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.getMeanExpressionLevelsHM(type, hmFilterTrans);
    }
    public HashMap<String, double[][]> getMeanExpressionLevelsSD_HM(DataType type, HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.getMeanExpressionLevelsSD_HM(type, hmFilterTrans);
    }
    public DataExpMatrix.ExpressionLevelsDistribution getMeanLog10ExpressionLevelsDistribution(DataType type, HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.getMeanLog10ExpressionLevelsDistribution(type, hmFilterTrans);
    }
    public DataInputMatrix.ExpMatrixData getExpressionData(DataType type, HashMap<String, Object> hmFilterTrans) {
        return(dataExpMatrix.getExpressionData(false, type, hmFilterTrans));
    }
    public HashMap<String, HashMap<String, Object>> getExpressedGeneTransFilter(HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.getExpressed(hmFilterTrans);
    }
    public ArrayList<Integer> getExpressedTransLengthArray(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, HashMap<String, Object>> hmGeneTrans = dataExpMatrix.getExpressed(hmFilterTrans);
        return getTransLengthArray(hmGeneTrans);
    }
    public ArrayList<Integer> getExpressedProteinsLengthArray(HashMap<String, Object> hmFilterTrans) {
        HashMap<String, HashMap<String, Object>> hmGeneTrans = dataExpMatrix.getExpressed(hmFilterTrans);
        return getProteinsLengthArray(hmGeneTrans);
    }
    public HashMap<String, Object> getExpressedTrans(HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.getExpressedTrans(hmFilterTrans);
    }
    public HashMap<String, HashMap<String, Integer>> getExpressedGeneIsoformsLength(HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.getExpressedGeneIsoformsLength(hmFilterTrans);
    }
    public HashMap<String, Integer> getExpressedGeneIsoformsCount(HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.getExpressedGeneIsoformsCount(hmFilterTrans);
    }
    public HashMap<String, Integer> getExpressedGeneAssignedProteinsCount(HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.getExpressedGeneAssignedProteinsCount(hmFilterTrans);
    }
    public int getExpressedAssignedProteinsCount(HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.getExpressedAssignedProteinsCount(hmFilterTrans);
    }
    public HashMap<String, Boolean> getExpressedIsoformsCoding(HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.getExpressedIsoformsCoding(hmFilterTrans);
    }
    public DataProject.DistributionData getExpressedTransDistribution(HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.getExpressedTransDistribution(hmFilterTrans);
    }
    public DataProject.DistributionData getExpressedProtDistribution(HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.getExpressedProtDistribution(hmFilterTrans);
    }
    public boolean genResultMatrixFile(HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.genResultMatrixFile(hmFilterTrans);
    }
    // geneProtein is used to create a gene_protein id which is required for DIU
    public boolean genExpressionFile(DataType dataType, boolean geneProtein) {
        boolean result = false;
        switch(dataType) {
            case GENE:
                // this file includes all the genes in the project, there is no need to recreate it each time
                if(!Files.exists(Paths.get(project.data.getGeneMatrixFilepath())))
                    result = dataExpMatrix.genExpressionFile(dataType, false, getResultsTrans(), getGeneMatrixFilepath());
                else
                    result = true;
                break;
            case TRANS:
                // this file includes all the transcripts in the project, there is no need to recreate it each time
                if(!Files.exists(Paths.get(project.data.getTranscriptMatrixFilepath())))
                    result = dataExpMatrix.genExpressionFile(dataType, false, getResultsTrans(), getTranscriptMatrixFilepath());
                else
                    result = true;
                break;
            case PROTEIN:
                // set for either protein or gene_protein ids file
                String filepath = geneProtein? project.data.getGeneProteinMatrixFilepath() : project.data.getProteinMatrixFilepath();
                
                // this file includes all the proteins or gene-proteins in the project, there is no need to recreate it each time
                if(!Files.exists(Paths.get(filepath)))
                    result = dataExpMatrix.genExpressionFile(dataType, geneProtein, getResultsTrans(), filepath);
                else
                    result = true;
                break;
        }
        return result;
    }
    public ObservableList<Analysis.DataSelectionResults> getDataSelectionResults(DataType dataType) {
        ObservableList<Analysis.DataSelectionResults> data = FXCollections.observableArrayList();
        DataInputMatrix.ExpMatrixData emdata;
        HashMap<String, double[]> hmMEL = getMeanExpressionLevelsHM(dataType, hmResultsTrans);
        emdata = getExpressionData(dataType, hmResultsTrans);
        if(emdata != null) {
            for(DataInputMatrix.ExpMatrixArray rowdata : emdata.data) {
                Analysis.DataSelectionResults dsr = new Analysis.DataSelectionResults(project, false, dataType, rowdata);
                dsr.pos = getLocusPosition(dataType, dsr.getId());
                if(!hmMEL.isEmpty()) {
                    if(hmMEL.containsKey(dsr.getId())) {
                        double[] conds = hmMEL.get(dsr.getId());
                        dsr.conditions = new SimpleDoubleProperty[conds.length];
                        for(int i = 0; i < conds.length; i++)
                            dsr.conditions[i] = new SimpleDoubleProperty(Double.parseDouble(String.format("%.02f", ((double)Math.round(conds[i]*100)/100.0))));
                    }
                    else
                        logger.logWarning("Unable to find expression values for '" + dsr.id + "'");
                }
                data.add(dsr);
            }
        }
        return data;
    }
    public ObservableList<Analysis.DataSelectionResults> getOriginalExpMatrixSelectionResults() {
        ObservableList<Analysis.DataSelectionResults> data = FXCollections.observableArrayList();
        DataInputMatrix.ExpMatrixData emdata;
        emdata = getOriginalExpressionData();
        if(emdata != null) {
            for(DataInputMatrix.ExpMatrixArray rowdata : emdata.data) {
                Analysis.DataSelectionResults dsr = new Analysis.DataSelectionResults(project, false, DataType.TRANS, rowdata);
                data.add(dsr);
            }
        }
        return data;
    }
    // get gene isoforms expression levels
    public HashMap<String, TransExpLevels> getGeneIsosExpLevels(String gene, HashMap<String, Object> hmFilterTrans) {
        HashMap<String, double[]> hmExpLevels = dataExpMatrix.getMeanExpressionLevelsHM(DataType.TRANS, hmFilterTrans);
        HashMap<String, TransExpLevels> hmResults = new HashMap<>();
        HashMap<String, Object> hm = dataExpMatrix.getExpressedGeneTrans(gene, hmFilterTrans);
        for(String trans : hm.keySet()) {
            if(hmExpLevels.containsKey(trans)) {
                double[] vals = hmExpLevels.get(trans);
                hmResults.put(trans, new TransExpLevels(vals[0], vals[1]));
            }
        }
        return hmResults;
    }
    public HashMap<String, double[]> getGeneTimesIsosExpLevels(String gene, HashMap<String, Object> hmFilterTrans) {
        HashMap<String, double[]> hmExpLevels = dataExpMatrix.getMeanExpressionLevelsHM(DataType.TRANS, hmFilterTrans);
        HashMap<String, double[]> hmResults = new HashMap<>();
        HashMap<String, Object> hm = dataExpMatrix.getExpressedGeneTrans(gene, hmFilterTrans);
        for(String trans : hm.keySet()) {
            if(hmExpLevels.containsKey(trans)) {
                double[] vals = hmExpLevels.get(trans);
                hmResults.put(trans, vals);
            }
        }
        return hmResults;
    }
    // get gene proteins expression levels
    public HashMap<String, TransExpLevels> getGeneProtsExpLevels(String gene, HashMap<String, Object> hmFilterTrans) {
        HashMap<String, double[]> hmExpLevels = dataExpMatrix.getMeanExpressionLevelsHM(DataType.PROTEIN, hmFilterTrans);
        HashMap<String, TransExpLevels> hmResults = new HashMap<>();
        HashMap<String, Object> hm = dataExpMatrix.getExpressedGeneProteins(gene, hmFilterTrans);
        for(String protein : hm.keySet()) {
            if(hmExpLevels.containsKey(protein)) {
                double[] vals = hmExpLevels.get(protein);
                hmResults.put(protein, new TransExpLevels(vals[0], vals[1]));
            }
        }
        return hmResults;
    }
    
    //
    // Input source matrix functions - work on the project input source matrix
    //
    public HashMap<String, HashMap<String, Object>> getInputMatrixFilterGeneTrans() {
        return dataExpMatrix.getInputMatrixFilterGeneTrans();
    }
    public HashMap<String, Object> getInputMatrixFilterTrans() {
        return dataExpMatrix.getInputMatrixFilterTrans();
    }
    public HashMap<String, double[]> getInputMatrixMeanExpressionLevelsHM(DataType type, HashMap<String, Object> hmFilterTrans) {
        return dataExpMatrix.getInputMatrixMeanExpressionLevelsHM(type, hmFilterTrans);
    }
    
    //
    // Source/Features data
    //
    public HashMap<String, HashMap<String, Object>> getAnnotFeatures() {
        return dataAnnotation.getAnnotFeatures();
    }
    public HashMap<String, Object> getSourceFeatures(String source) {
        return dataAnnotation.getSourceFeatures(source);
    }
    public HashMap<String, Object> getSourceFeaturesPosition(String source) {
        return dataAnnotation.getSourceFeaturesPosition(source);
    }
    public HashMap<String, Object> getSourceFeaturesPresence(String source) {
        return dataAnnotation.getSourceFeaturesPresence(source);
    }
    public HashMap<String, HashMap<String, AFStatsData>> getAFStats() {
        return dataAnnotation.getAFStats();
    }
    public HashMap<String, ArrayList<String>> getAFStatsPositional() {
        return dataAnnotation.getAFStatsPositional();
    }
    public HashMap<String, ArrayList<String>> getAFStatsPresence() {
        return dataAnnotation.getAFStatsPresence();
    }
    public HashMap<String, HashMap<String, String>> getAFStatsType() {
        return dataAnnotation.getAFStatsType();
    }
    public HashMap<String, AFStatsData> getSourceFeaturesStats(String source) {
        return dataAnnotation.getSourceFeaturesStats(source);
    }
    public HashMap<String, AFStatsData> getSummaryAFStatsData(String sourceFilter, boolean includeReserved) {
        return dataAnnotation.getSummaryAFStatsData(sourceFilter, includeReserved);
    }
    public HashMap<String, HashMap<String, Object>> loadAllAFs(){
        return dbAnnotation.loadAnnotFeatures();
    }
    // should only be called from DataAnnotation
    public HashMap<String, DataAnnotation.AnnotSourceStats> loadAnnotSourceStats(){
        return dbProject.loadAnnotSourceStats();
    }
    public HashMap<String, HashMap<String, HashMap<String, String>>> loadFeatureDescriptions(){
        return dbProject.loadFeatureDescriptions();
    }
    public HashMap<String, HashMap<String, AFStatsData>> loadAFStats(){
        return dbProject.loadAFStats();
    }
    public HashMap<String, ArrayList<String>> loadAFStatsPositional(){
        //return dbAnnotation.loadAFStatsPositional();
        return dbProject.loadAFStatsPositional();
    }
    public HashMap<String, ArrayList<String>> loadAFStatsPresence(){
        //return dbAnnotation.loadAFStatsPresence();
        return dbProject.loadAFStatsPresence();
    }
    public HashMap<String, HashMap<String, String>> loadAFStatsType(){
        return dbAnnotation.loadAFStatsType();
    }
    public HashMap<String, String> loadTransAnnotFeaturesString(){
        return dbProject.loadTransAnnotFeaturesString();
    }
    public HashMap<String, HashMap<String, HashMap<String, Integer>>> loadTransAnnotFeatures(){
        return dbProject.loadTransAnnotFeatures();
    }
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> loadTransAFIdPos(){
        return dbProject.loadTransAFIdPos();
    }
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> loadTransAFIdGenPos(){
        return dbProject.loadTransAFIdGenPos();
    }    
    public int getIfTransHasAnnot(String trans, String source){
        return dbProject.getIfTransHasAnnot(trans, source);
    }
    public HashMap<String, Object> getTransWithAnnot(String source){
        return dbProject.getTransWithAnnot(source);
    }
    public int getTotalAnnot(){
        return dbProject.getTotalAnnot();
    }
    
    public int getAnnotCount(String annot){
        return dbProject.getAnnotCount(annot);
    }
    
    public int getAnnotCountByGene(String annot){
        return dbProject.getAnnotCountByGene(annot);
    }
    
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> loadTransAFIdPos(ArrayList<String> lstTrans){
        return dbProject.loadTransAFIdPos(lstTrans);
    }
    public HashMap<String, HashMap<String, Object>> loadGeneTrans(){
        return dbProject.loadGeneTrans();
    }
    public DbProject.TransDataResults loadTransData(){
        return dbProject.loadTransData();
    }
    public ArrayList<String> loadGeneAnnotations(String gene){
        return dbProject.loadGeneAnnotations(gene);
    }
    /*
    public HashMap<String, String> loadGeneDescriptions() {
        return dbProject.loadDescriptions(dataAnnotation.fileDefs.gene.source, dataAnnotation.fileDefs.gene.feature);
    }
    public HashMap<String, String> loadProteinDescriptions() {
        return dbProject.loadDescriptions(dataAnnotation.fileDefs.protein.source, dataAnnotation.fileDefs.protein.feature);
    }*/
    
    // Results Checking Functions
    public boolean hasInputData() {
        return Files.exists(Paths.get(getInputDataFolder(), ID_COMPLETED));
    }
    public boolean writeInputDataCompletedFile() {
        boolean result = false;
        String filepath = Paths.get(getInputDataFolder(), ID_COMPLETED).toString();
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
            writer.write("done");
        } catch(Exception e) {
            logger.logError("Unable to create input data completed file: " + e.getMessage());
        } finally {
           try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
        }
        return result; 
    }

    //
    // Input data Expression Matrix interface - unfiltered, unchanged, copy of the user provided matrix
    //
    // for getExpTypeGroupNames, the group names will depend on the experiment type
    // ex: TIME_COURSE_SINGLE will be composed of "GroupName T?"
    public String[] getExpTypeGroupNames() {
        return dataInputMatrix.getExpTypeGroupNames();
    }
    public int getTimePoints() {
        return dataInputMatrix.getTimePoints();
    }
    public String[] getTimePointNames() {
        return dataInputMatrix.getTimePointNames();
    }
    public String[] getResultNames() {
        return dataInputMatrix.getResultNames();
    }
    public int[] getConditionsSamples() {
        int cnt = 0;
        int[] scnt = null;
        ArrayList<DataInputMatrix.ExpMatrixCondition> lst = dataInputMatrix.getExpressionMatrixConditions();
        int conditions = 0;
        switch(project.data.getExperimentType()) {
            case Two_Group_Comparison:
                scnt = new int[lst.size()];
                for(DataInputMatrix.ExpMatrixCondition emc : lst)
                    scnt[conditions++] = emc.nsamples;
                break;
            case Time_Course_Multiple:
            case Time_Course_Single:
                // get number of group-time
                for(DataInputMatrix.ExpMatrixCondition emc : lst)
                    cnt += emc.group.lstTimes.size();
                scnt = new int[cnt];
                for(DataInputMatrix.ExpMatrixCondition emc : lst) {
                    DlgInputData.Params.ExpMatrixGroup emg = emc.group;
                    for(DlgInputData.Params.ExpMatrixTime emt : emg.lstTimes)
                        scnt[conditions++] = emt.lstSampleNames.size();
                }
                break;
        }
        return scnt;
    }
    public int[] getGroupSamples() {
        ArrayList<DataInputMatrix.ExpMatrixCondition> lst = dataInputMatrix.getExpressionMatrixConditions();
        int conditions = 0;
        int[] scnt = new int[lst.size()];
        for(DataInputMatrix.ExpMatrixCondition emc : lst)
            scnt[conditions++] = emc.nsamples;
        return scnt;
    }
    public int[] getGroupTimes() {
        ArrayList<DataInputMatrix.ExpMatrixCondition> lst = dataInputMatrix.getExpressionMatrixConditions();
        int grpnum = 0;
        //DlgInputData.Params params = new DlgInputData.Params(IDParams);
        int[] scnt = new int[params.lstGroups.size()];
        for(DlgInputData.Params.ExpMatrixGroup emc : params.lstGroups)
            scnt[grpnum++] = emc.lstTimes.size();
        return scnt;
    }
    public int getGroupTimeSamples(int grpnum, int timenum) {
        int scnt = 0;
        if(grpnum >= 0 && grpnum < params.lstGroups.size()) {
            DlgInputData.Params.ExpMatrixGroup emc = params.lstGroups.get(grpnum);
            if(timenum >= 0 && timenum < emc.lstTimes.size()) {
                DlgInputData.Params.ExpMatrixTime emt = emc.lstTimes.get(timenum);
                scnt = emt.lstSampleNames.size();
            }
        }
        return scnt;
    }
    public String[] getGroupNames() {
        return dataInputMatrix.getGroupNames();
    }
    public int getGroupsCount() {
        return dataInputMatrix.getGroupNames().length;
    }
    public String[] getGroupsTimeNames() {
        return dataInputMatrix.getGroupsTimeNames();
    }
    public String[] getGroupTimeNames(int grpnum) {
        return dataInputMatrix.getGroupTimeNames(grpnum);
    }
    public int getGroupTimesCount(int grpnum) {
        return dataInputMatrix.getGroupTimeNames(grpnum).length;
    }
    /*
    public String getConditionName(int num) {
        String name = "";
        if(num >= 1 && num <= 2) {
            name = "Condition " + num;
            if(IDParams != null) {
                if(IDParams.containsKey("condition" + num))
                    name = IDParams.get("condition" + num);
            }
        }
        return name;
    }*/
    public DataInputMatrix.ExpMatrixParams getInputMatrixParams() {
        return(dataInputMatrix.getInputMatrixParams());
    }
    public ArrayList<DataInputMatrix.ExpMatrixCondition> getExpressionMatrixConditions() {
        return(dataInputMatrix.getExpressionMatrixConditions());
    }
    public boolean genExpFactorsFile() {
        return dataInputMatrix.genExpFactorsFile();
    }
    // geneProtein is used to create a gene_protein id which is required for DIU
    public boolean genExpressionRawFile(DataType dataType, boolean geneProtein) {
        boolean result = false;
        switch(dataType) {
            case GENE:
                // this file includes all the genes in the project, there is no need to recreate it each time
                if(!Files.exists(Paths.get(project.data.getGeneMatrixRawFilepath())))
                    result = dataExpMatrix.genExpressionFile(dataType, false, getResultsTrans(), getGeneMatrixRawFilepath());
                else
                    result = true;
                break;
            case TRANS:
                // this file includes all the transcripts in the project, there is no need to recreate it each time
                if(!Files.exists(Paths.get(project.data.getTranscriptMatrixRawFilepath())))
                    result = dataInputMatrix.genExpressionRawFile(dataType, false, getResultsTrans(), getTranscriptMatrixRawFilepath());
                else
                    result = true;
                break;
            case PROTEIN:
                // set for either protein or gene_protein ids file
                String filepath = geneProtein? project.data.getGeneProteinMatrixRawFilepath() : project.data.getProteinMatrixRawFilepath();
                
                // this file includes all the proteins or gene-proteins in the project, there is no need to recreate it each time
                if(!Files.exists(Paths.get(filepath)))
                    result = dataInputMatrix.genExpressionRawFile(dataType, geneProtein, getResultsTrans(), filepath);
                else
                    result = true;
                break;
        }
        return result;
    }
    // should only be called from processing input data dialog
    public boolean genMasterExpressionFile() {
        return dataInputMatrix.genMasterExpressionFile(params.getParams());
    }
    public String getExpMatrixFileHeader() {
        return dataInputMatrix.getExpMatrixFileHeader();
    }
    public DataInputMatrix.ExpMatrixData getOriginalExpressionData() {
        return dataInputMatrix.getOriginalExpressionData();
    }
    /*
    public ArrayList<DataInputMatrix.ExpMatrixCondition> getOriginalExpressionConditions() {
        return dataInputMatrix.getOriginalExpressionConditions();
    }*/
    public DataInputMatrix.ExpMatrixData getRawExpressionData(DataType type, HashMap<String, Object> hmFilterTrans) {
        return dataInputMatrix.getRawExpressionData(type, hmFilterTrans);
    }
    public HashMap<String, HashMap<String, Object>> getInputMatrixGeneTrans() {
        return dataInputMatrix.getInputMatrixGeneTrans();
    }
    public boolean copyExpFactorsFile(String filepath) {
        return dataInputMatrix.copyExpFactorsFile(filepath);
    }
    public boolean copyTimeFactorsFile(String filepath) {
        return dataInputMatrix.copyTimeFactorsFile(filepath);
    }

    //
    // Annotation data interface
    //
    public boolean isInternalFeature(String db, String cat) {
        return dataAnnotation.isInternalFeature(db, cat);
    }
    public boolean isRsvdFeature(String db, String cat) {
        return dataAnnotation.isRsvdFeature(db, cat);
    }
    public boolean isRsvdNoDiversityFeature(String db, String cat) {
        return dataAnnotation.isRsvdNoDiversityFeature(db, cat);
    }
    public String getGeneChromo(String gene) {
        return dataAnnotation.getGeneChromo(gene);
    }
    public String getGeneStrand(String gene) {
        return dataAnnotation.getGeneStrand(gene);
    }
    public HashMap<String, Object> getGeneTrans(String gene) {
        return dataAnnotation.getGeneTrans(gene);
    }
    public HashMap<String, HashMap<String, HashMap<String, Object>>> getDiverseFeaturesUsingPresence(HashMap<String, HashMap<String, Object>> hmGeneIsos, 
                                                     HashMap<String, HashMap<String, Object>> hmFeatures) {
        return dataAnnotation.getDiverseFeaturesUsingPresence(hmGeneIsos, hmFeatures);
    }
    // Check it because there are another functions that call it (change by new??)!!!
    public HashMap<String, HashMap<String, HashMap<String, Object>>> getDiverseFeaturesUsingGenomicPositionExons(HashMap<String, HashMap<String, Object>> hmGeneIsos, 
                                                     HashMap<String, HashMap<String, Object>> hmFeatures, boolean returnGenoPos,
                                                     HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmTransAFIdPos) {
        return dataAnnotation.getDiverseFeaturesUsingGenomicPositionExons(hmGeneIsos, hmFeatures, returnGenoPos, hmTransAFIdPos);
    }
    
    public HashMap<String, HashMap<String, HashMap<String, Object>>> getDiverseFeaturesUsingNewGenomicPosition(HashMap<String, HashMap<String, Object>> hmGeneIsos, 
                                                     HashMap<String, HashMap<String, Object>> hmFeatures, boolean returnGenoPos,
                                                     HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> hmTransAFIdPos) {
        return dataAnnotation.getDiverseFeaturesUsingNewGenomicPosition(hmGeneIsos, hmFeatures, returnGenoPos, hmTransAFIdPos);
    }
    
    public DataAnnotation.AnnotFeatureDef getRsvdAnnotFeatureDef(DataAnnotation.AnnotFeature af) {
        return dataAnnotation.getRsvdAnnotFeatureDef(af);
    }
    public DataAnnotation.AnnotFeature getRsvdAnnotFeature(String source, String feature) {
        return dataAnnotation.getRsvdAnnotFeature(source, feature);
    }    
    public boolean isTransCoding(String trans) {
       return dataAnnotation.isTransCoding(trans); 
    }
    public boolean isGeneCoding(String gene) {
       return dataAnnotation.isGeneCoding(gene); 
    }
    public HashMap<String, HashMap<String, Object>> getTransAnnotFeatures(String trans) {
        return dataAnnotation.getTransAnnotFeatures(trans);
    }
    public HashMap<String, HashMap<String, String>> getTransAnnotFeatures(String trans, String source) {
        return dataAnnotation.getTransAnnotFeatures(trans, source);
    }
    public ArrayList<Integer> getTransLengthArray(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        return dataAnnotation.getTransLengthArray(hmFilterGeneTrans);
    }
    public ArrayList<Integer> getProteinsLengthArray(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        return dataAnnotation.getProteinsLengthArray(hmFilterGeneTrans);
    }
    public int getProteinLength(String protein, HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getProteinLength(protein, hmFilterTrans); 
    }
    public HashMap<String, HashMap<String, Integer>> getProteinTransLengths(HashMap<String, Object> hmProteins, HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getProteinTransLengths(hmProteins, hmFilterTrans); 
    }    
    public String getTransProtein(String trans) {
        return dataAnnotation.getTransProtein(trans); 
    }
    public int getTransProteinLength(String trans) {
        return dataAnnotation.getTransProteinLength(trans); 
    }
    public String getTransName(String trans) {
        return dataAnnotation.getTransName(trans); 
    }
    public int getTransLength(String trans) {
        return dataAnnotation.getTransLength(trans); 
    }
    public int getTransGenomicLength(String trans) {
        return dataAnnotation.getTransGenomicLength(trans); 
    }
    public String getTransGene(String trans) {
        return dataAnnotation.getTransGene(trans); 
    }
    public String getTransChromo(String trans) {
        return dataAnnotation.getTransChromo(trans);
    }
    public String getTransStrand(String trans) {
        return dataAnnotation.getTransStrand(trans);
    }
    public ArrayList<String> getProteinTrans(String protein, HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getProteinTrans(protein, hmFilterTrans); 
    }
    public HashMap<String, Integer> getProteinsTransCounts(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getProteinsTransCounts(hmFilterTrans); 
    }
    public String getProteinGene(String protein, HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getProteinGene(protein, hmFilterTrans); 
    }
    public ArrayList<String> getProteinGenes(String protein, HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getProteinGenes(protein, hmFilterTrans); 
    }
    public int getMultipleIsoformsGeneCount() {
        int cnt = 0;
        for(String gene : hmResultsGeneTrans.keySet()) {
            HashMap<String, Object> hm = hmResultsGeneTrans.get(gene);
            if(hm.size() > 1)
                cnt++;
        }
        return cnt;
    }
    public int getGeneTransCount(String gene) {
        return dataAnnotation.getGeneTransCount(gene); 
    }
    public HashMap<String, Integer> getGeneTransCount() {
        return dataAnnotation.getGeneTransCount(DataAnnotation.IsoType.ALL); 
    }
    public int getGeneProteinCount(String gene) {
        return dataAnnotation.getGeneProteinsCount(gene); 
    }
    public HashMap<String, Integer> getGeneProteinCounts(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getGeneProteinCounts(hmFilterTrans); 
    }
    public HashMap<String, HashMap<String, Object>> getGeneProteins(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getGeneProteins(hmFilterTrans); 
    }    
    public DistributionData getGeneProteinDistribution(HashMap<String, Object> hmFilterTrans) {
        DataProject.DistributionData dd = null;
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        //HashMap<String, Integer> hmGeneProtCnts = getExpressedGeneAssignedProteinsCount(hmFilterTrans);
        HashMap<String, Integer> hmGenesProteinCounts = getGeneProteinCounts(hmFilterTrans);
        if(hmGenesProteinCounts.size() > 0) {
            int gcnt = hmGenesProteinCounts.size();
            int tcnt = 0;
            HashMap<String, Integer> hm = new HashMap<>();
            for(Integer cnt : hmGenesProteinCounts.values())
                tcnt += cnt;
            int min = 1000000;
            int max = 0;
            for(Integer cnt : hmGenesProteinCounts.values()) {
                if(cnt > max)
                    max = cnt;
                if(cnt < min)
                    min = cnt;
            }
            if(min == 1000000)
                min = max;
            int[] dist = new int[max+1];
            for(int i = 0; i <= max; i++)
                dist[i] = 0;
            for(Integer cnt : hmGenesProteinCounts.values())
                dist[cnt]++;
            for(int i = 0; i <= max; i++)
                series.getData().add(new XYChart.Data<>("" + (i), dist[i]));
            double mean = (double)tcnt / (double)gcnt;
            mean = Double.parseDouble(String.format("%.2f", ((double)Math.round(mean*100)/100.0)));
            dd = new DataProject.DistributionData(series, gcnt, tcnt, min, max, mean);
        }
        return dd;
    }
    public int getTransProteinCount(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getTransProteinCount(hmFilterTrans); 
    }
    public String getTransAssignedProtein(String trans) {
        return dataAnnotation.getTransAssignedProtein(trans); 
    }
    public ArrayList<String> getAlignmentCategoriesDisplayOrder() {
        return dataAnnotation.getAlignmentCategoriesDisplayOrder();
    }
    public HashMap<String, Integer> getAlignmentCategoriesTransCount(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getAlignmentCategoriesTransCount(hmFilterTrans);
    }
    public HashMap<String, ArrayList<Integer>> getAlignmentCategoriesTransLengths(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getAlignmentCategoriesTransLengths(hmFilterTrans);
    }
    public HashMap<String, ArrayList<Integer>> getAlignmentCategoriesProtLengths(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getAlignmentCategoriesProtLengths(hmFilterTrans);
    }
    public HashMap<String, ArrayList<Integer>> getAlignmentCategoriesTransJunctions(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getAlignmentCategoriesTransJunctions(hmFilterTrans);
    }
    public HashMap<String, ArrayList<Integer>> getAlignmentCategoriesTransExons(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getAlignmentCategoriesTransExons(hmFilterTrans);
    }
    public String getTransAlignmentCategory(String trans) {
        return dataAnnotation.getTransAlignmentCategory(trans); 
    }
    public String getTransAlignmentAttributes(String trans) {
        return dataAnnotation.getTransAlignmentAttributes(trans); 
    }
    public HashMap<String, Object> getTransAlignmentAttributesHM(String trans) {
        return dataAnnotation.getTransAlignmentAttributesHM(trans); 
    }
    public ArrayList<String> getAlignmentAttributes() {
        return dataAnnotation.getAlignmentAttributes(); 
    }
    public HashMap<String, Object> getAlignmentAttributesHM() {
        return dataAnnotation.getAlignmentAttributesHM(); 
    }
    public ArrayList<Integer> getTransJunctions(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getTransJunctions(hmFilterTrans); 
    }
    public String getGenes(DataType dataType, String item) {
        String gene;
        ArrayList<String> lstGenes;
        switch(dataType) {
            case TRANS:
                gene = getTransGene(item);
                break;
            case PROTEIN:
                gene = "";
                lstGenes = getProteinGenes(item, null);
                for(String g : lstGenes)
                    gene += (gene.isEmpty()? "" : ",") + g;
                break;
            default:
                gene = item;
                break;
        }
        return gene;
    }
    public String getGeneDescriptions(DataType dataType, String item) {
        String geneDesc;
        ArrayList<String> lstGenes;
        switch(dataType) {
            case TRANS:
                geneDesc = getGeneDescription(getTransGene(item));
                break;
            case PROTEIN:
                geneDesc = "";
                lstGenes = getProteinGenes(item, null);
                for(String g : lstGenes)
                    geneDesc += (geneDesc.isEmpty()? "" : "; ") + getGeneDescription(g);
                break;
            default:
                geneDesc = getGeneDescription(item);
                break;
        }
        return geneDesc;
    }
    public String getGeneDescription(String gene) {
        return dataAnnotation.getGeneDescription(gene);
    }
    public String getProteinDescription(String protein) {
        return dataAnnotation.getProteinDescription(protein);
    }
    
    public String getFeatureDescription(String source, String feature, String ID) {
        return dataAnnotation.getFeatureDescription(source, feature, ID);
    }
    
    public HashMap<String, HashMap<String, Integer>> getGeneTransLength(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        return dataAnnotation.getGeneTransLength(hmFilterGeneTrans);
    }
    public boolean genTransFeatures(String filepath, HashMap<String, HashMap<String, Object>> hmFeatures, HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.genTransFeatures(filepath, hmFeatures, hmFilterTrans);
    }
    public boolean genProteinFeatures(String filepath, HashMap<String, HashMap<String, Object>> hmFeatures, HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.genProteinFeatures(filepath, hmFeatures, hmFilterTrans);
    }
    public boolean genGeneFeatures(String filepath, HashMap<String, HashMap<String, Object>> hmFeatures, HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.genGeneFeatures(filepath, hmFeatures, hmFilterTrans);
    }
    public boolean hasGene(String gene) {
        return dataAnnotation.hasGene(gene);
    }
    public boolean genCustomFeatures(String pathFeatures, String customFile, DataApp.DataType dataType, ArrayList<DataApp.RankedListEntry> lstRL) {
        boolean result = false;
        ArrayList<Utils.Set> lst = Utils.loadSetsFile(customFile);
        if(!lst.isEmpty()) {
            HashMap<String, Object> hmFilter = new HashMap<>();
            for(DataApp.RankedListEntry rle : lstRL)
                hmFilter.put(rle.id, null);
            Writer writer = null;
            try {
                int cnt = 0;
                Utils.createFilepathFolder(pathFeatures);
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathFeatures), "utf-8"));
                for(Utils.Set set : lst) {
                    for(String member : set.lstMembers) {
                        boolean hasMember = false;
                        switch(dataType) {
                            case GENE:
                                hasMember = dataAnnotation.hasGene(member);
                                break;
                            case PROTEIN:
                                hasMember = dataAnnotation.hasProtein(member);
                                break;
                            case TRANS:
                                hasMember = dataAnnotation.hasTranscript(member);
                                break;
                        }
                        if(hasMember && hmFilter.containsKey(member)) {
                            cnt++;
                            writer.write(member + "\t" + set.name + "\t" + "CustomSet" + "\t" + set.name + "\t\"" + set.description + "\"\n");
                        }
                    }
                }
                
                result = (cnt > 0);
            } catch (IOException e) {
                logger.logWarning("Generate custom features set file exception(" + pathFeatures + "): " + e.getMessage());
            } finally {
                try {if(writer != null) writer.close();} catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage()); }
            }
        }
        return result;
    }
    public boolean geneHasFeatureId(String gene, String source, String feature, String featureId, HashMap<String, Object> hmGeneTrans) {
        return dataAnnotation.geneHasFeatureId(gene, source, feature, featureId, hmGeneTrans);
    }
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> getGeneFeatures(HashMap<String, HashMap<String, Object>> hmFeatures, HashMap<String, 
                                                                                                      Object> hmFilterTrans, boolean incNameDesc, boolean expandGO) {
        return dataAnnotation.getGeneFeatures(hmFeatures, hmFilterTrans, incNameDesc, expandGO);
    }
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> getProteinFeatures(HashMap<String, HashMap<String, Object>> hmFeatures, HashMap<String, 
                                                                                                      Object> hmFilterTrans, boolean incNameDesc, boolean expandGO) {
        return dataAnnotation.getProteinFeatures(hmFeatures, hmFilterTrans, incNameDesc, expandGO);
    }
    public HashMap<String, HashMap<String, HashMap<String, HashMap<String, String>>>> getTransFeatures(HashMap<String, HashMap<String, Object>> hmFeatures, HashMap<String, 
                                                                                                      Object> hmFilterTrans, boolean incNameDesc, boolean expandGO) {
        return dataAnnotation.getTransFeatures(hmFeatures, hmFilterTrans, incNameDesc, expandGO);
    }
    public HashMap<String, DataAnnotation.TermTransInfo> loadTransFeatures(String filepath, HashMap<String, HashMap<String, Object>> hmFeatures, HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.loadTransFeatures(filepath, hmFeatures, hmFilterTrans);
    }
    public HashMap<String, DataAnnotation.FeatureItemsInfo> loadFeatureItemsInfo(String filepath) {
        return dataAnnotation.loadFeatureItemsInfo(filepath);
    }
    public HashMap<String, DataAnnotation.AnnotSourceStats> getAnnotSourceStats(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getAnnotSourceStats(hmFilterTrans);
    }
    public int[] getFeatureDistributionPerTrans(String source, String feature, int maxCnt, HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getFeatureDistributionPerTrans(source, feature, maxCnt, hmFilterTrans);
    }    
    public ArrayList<String[]> getData(String gene, HashMap<String, Object> trans) {
        return dataAnnotation.getAnnotationData(gene, trans);
    }
    public SeqAlign.Position getLocusPosition(DataType dataType, String id) {
        return dataAnnotation.getLocusPosition(dataType, id);
    }
    public DataAnnotation.TransData getTransData(String trans) {
        return dataAnnotation.getTransData(trans);
    }
    public ArrayList<DataAnnotation.TransData> getTransData(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getTransData(hmFilterTrans);
    }
    public HashMap<String, DataAnnotation.TransData> getTransDataHM(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getTransDataHM(hmFilterTrans);
    }
    public HashMap<String, Boolean> getTransCoding(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getTransCoding(hmFilterTrans);
    }    
    public DataAnnotation.TransTermsData getTransTermsData(String gene, ArrayList<String> transcripts, 
            HashMap<String, HashMap<String, Object>> hmTerms, boolean incReserved) {
        return dataAnnotation.getTransTermsData(gene, transcripts, hmTerms, incReserved);
    }    
    public DataAnnotation.TransTermsData getTransTermsDiversity(String gene, ArrayList<String> transcripts, 
            HashMap<String, HashMap<String, Object>> hmTerms, boolean coding) {
        return dataAnnotation.getTransTermsDiversity(gene, transcripts, hmTerms, coding);
    }
    public DataAnnotation.DiversityResultsInternal genAnnotationDiversityData(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans, 
            HashMap<String, HashMap<String, Object>> hmTerms, TaskHandler.TaskAbortFlag taf) {
        return dataAnnotation.genAnnotationDiversityData(hmFilterGeneTrans, hmTerms, taf);
    }
    public DataAnnotation.DiversityResultsInternal getDiversityDataFromHm(HashMap<String, HashMap<String, HashMap<String, Object>>> hmDiversity, HashMap<String, HashMap<String, Object>> hmTerms, TaskHandler.TaskAbortFlag taf) {
        return dataAnnotation.getDiversityDataFromHm(hmDiversity, hmTerms, taf);
    }
    public HashMap<String, HashMap<String, Boolean>> getDiversityDataFromHmID(HashMap<String, HashMap<String, HashMap<String, Object>>> hmDiversity, HashMap<String, HashMap<String, Object>> hmTerms, TaskHandler.TaskAbortFlag taf, boolean genpos) {
        return dataAnnotation.getDiversityDataFromHmID(hmDiversity, hmTerms, taf, genpos);
    }
    public ArrayList<DataAnnotation.AnnotationDataEntry> getTransAnnotationData(String gene, ArrayList<String> transcripts) {
        return dataAnnotation.getTransAnnotationData(gene, transcripts);
    }
    public DataAnnotation.SummaryData getSummaryData(HashMap<String, HashMap<String, Object>> hmTGFilter, boolean includeReserved) {
        return dataAnnotation.getSummaryData(hmTGFilter, includeReserved);
    }
    public HashMap<String, Integer> getAnnotatedGeneIsoformsCount(DataAnnotation.IsoType type) {
        return dataAnnotation.getGeneTransCount(type);
    }
    public HashMap<String, Integer> getAnnotatedGeneAssignedProteinsCount(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        return dataAnnotation.getGeneAssignedProteinsCount(hmFilterGeneTrans);
    }
    public HashMap<String, Integer> getAnnotatedAssignedProteinsLength(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        return dataAnnotation.getAssignedProteinsLength(hmFilterGeneTrans);
    }
    public HashMap<String, HashMap<String, Object>> getGeneAssignedProteins(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        return dataAnnotation.getGeneAssignedProteins(hmFilterGeneTrans);
    }
    public HashMap<String, HashMap<String, Object>> getAssignedProteinGenes() {
        return dataAnnotation.getAssignedProteinGenes();
    }
    public int getAnnotatedAssignedProteinsCount(HashMap<String, HashMap<String, Object>> hmFilterGeneTrans) {
        return dataAnnotation.getAssignedProteinsCount(hmFilterGeneTrans);
    }
    public HashMap<String, HashMap<String, Integer>> getAnnotatedGeneIsoformsLength() {
        return dataAnnotation.getGeneTransLength(null);
    }
    public HashMap<String, Boolean> getAnnotatedIsoformsCoding() {
        return dataAnnotation.getTransCoding(null);
    }
    public HashMap<String, DataAnnotation.ChromoDistributionData> getGeneTransChromoDistribution(HashMap<String, Object> hmFilterTrans) {
        return dataAnnotation.getGeneTransChromoDistribution(hmFilterTrans);
    }
    public boolean genGeneTransFile() {
        // this file includes all the gene-trans in the project, there is no need to recreate it each time
        if(!Files.exists(Paths.get(project.data.getGeneTransFilePath())))
            return dataAnnotation.genGeneTransFile(project.data.getGeneTransFilePath(), project.data.getResultsGeneTrans());
        else
            return true;
    }
    public boolean genGeneProteinsFile() {
        // this file includes all the gene-proteins in the project, there is no need to recreate it each time
        if(!Files.exists(Paths.get(project.data.getGeneProteinsFilePath())))
            return dataAnnotation.genGeneProteinsFile(project.data.getGeneProteinsFilePath(), project.data.getResultsGeneTrans());
        else
            return true;
    }
    public static void loadProjectDef(HashMap<String, String> hmParams, String folderpath) {
        String filepath = Paths.get(folderpath, DataApp.PRM_PROJECT).toString();
        Utils.loadParams(hmParams, filepath);
    }
    public static boolean saveProjectDef(HashMap<String, String> hmParams, String folderpath) {
        String filepath = Paths.get(folderpath, DataApp.PRM_PROJECT).toString();
        return Utils.saveParams(hmParams, filepath);
    }
    
    public static boolean saveDIUResultAnalysis(TableView tblData, String  analysisFolder){
        return Utils.saveDIUResultAnalysis(tblData, analysisFolder);
    }
    
    public static boolean saveDFIResultAnalysis(TableView tblData, String  analysisFolder){
        return Utils.saveDFIResultAnalysis(tblData, analysisFolder);
    }
    
    public boolean saveDFIAnnotCount(HashMap<String, HashMap<String, Object>> hmAnnot, String  analysisFolder){
        int total = 0;
        int byGene = 0;
        String aux = "";
        HashMap<String, HashMap<String, Object>> hmtotalAnnot = new HashMap<String, HashMap<String, Object>>();
        hmtotalAnnot = project.data.getAnnotFeatures();
        String res = "Feature\tTotal\tByGenes\n";
        for(String item : hmAnnot.keySet()){
            if(hmAnnot.get(item).keySet().isEmpty()){
                for(String feature : hmtotalAnnot.get(item).keySet()){
                    //clan is inside PAM but we only want DOMAIN
                    total = project.data.getAnnotCount(feature);
                    byGene = project.data.getAnnotCountByGene(feature);
                    res += feature + "\t" + Integer.toString(total) + "\t" + Integer.toString(byGene) + "\n";
                }
            }else{
                for(String feature : hmAnnot.get(item).keySet()){
                    total = project.data.getAnnotCount(feature);
                    byGene = project.data.getAnnotCountByGene(feature);
                    res += feature + "\t" + Integer.toString(total) + "\t" + Integer.toString(byGene) + "\n";
                }
            }
        }
        return Utils.saveTextToFile(res, analysisFolder);
    }
    
    public ExpLevelPCAData getExpLevelsPCAResults () {
        DataProject.ExpLevelPCAData data = new DataProject.ExpLevelPCAData();
        DataInputMatrix.ExpMatrixParams imParams = getInputMatrixParams();

        //int[] scnt = project.data.getConditionsSamples();
        //String[] groups = project.data.getExpTypeGroupNames();
        int[] scnt = project.data.getGroupSamples();
        String[] groups = project.data.getGroupNames();
        for(String group : groups) {
            XYChart.Series series = new XYChart.Series();
            series.setName(group);
            data.lstSeries.add(series);
        }
        int nsamples = imParams.getTotalSamplesCount();
        String filepath = getMatrixPCAFilepath();
        if(Files.exists(Paths.get(filepath))) {
            try {
                List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                for(int idx = 0; idx < lines.size(); idx++) {
                    String line = lines.get(idx);
                    if(line.startsWith("#VAR")) {
                        int pc = 0;
                        for(idx++; idx < lines.size() && pc < 2; idx++, pc++) {
                            line = lines.get(idx);
                            String[] fields = line.split("\t");
                            if(fields.length >= 2) {
                                if(pc == 0)
                                    data.pc1 = Double.parseDouble(String.format("%.2f", ((double)Math.round(Double.parseDouble(fields[0])*100)/100.0)));
                                else
                                    data.pc2 = Double.parseDouble(String.format("%.2f", ((double)Math.round(Double.parseDouble(fields[0])*100)/100.0)));
                            }
                            else {
                                logger.logWarning("Invalid line, " + line + ", in expression levels PCA results: " + line);
                                break;
                            }
                        }
                    }
                    else if(line.startsWith("#SCORES")) {
                        int c = 0;
                        int sample = 0;
                        int cscnt = scnt[c];
                        double x,y;
                        XYChart.Series series = data.lstSeries.get(c);
                        for(idx++; idx < lines.size() && sample < nsamples; idx++, sample++) {
                            line = lines.get(idx);
                            String[] fields = line.split("\t");
                            if(fields.length >= 2) {
                                if(sample >= cscnt) {
                                    series = data.lstSeries.get(++c);
                                    cscnt += scnt[c];
                                }
                                x = Double.parseDouble(String.format("%.2f", ((double)Math.round(Double.parseDouble(fields[0])*100)/100.0)));
                                y = Double.parseDouble(String.format("%.2f", ((double)Math.round(Double.parseDouble(fields[1])*100)/100.0)));
                                series.getData().add(new XYChart.Data(x, y));
                            }
                            else {
                                logger.logWarning("Invalid line, " + line + ", in expression levels PCA results: " + line);
                                break;
                            }
                        }
                    }
                }
            }
            catch(IOException ioe) {
                logger.logError("Expression levels PCA results file exception: " + ioe.getMessage());
            }
        }

        return data;
    }
    
    //
    // Data Classes
    //
    public static class DistributionData {
        public int genes;
        public int trans;
        public int min;
        public int max;
        public double mean;
        public XYChart.Series<String, Number> series;
        public DistributionData(XYChart.Series<String, Number> series, int genes, int trans, int min, int max, double mean) {
            this.series = series;
            this.genes = genes;
            this.trans = trans;
            this.min = min;
            this.max = max;
            this.mean = mean;
        }
    }
    public static class ExpLevelPCAData {
        public double pc1, pc2;
        public List<XYChart.Series<Number, Number>> lstSeries;
        public ExpLevelPCAData() {
            pc1 = 0.0;
            pc2 = 0.0;
            lstSeries = new ArrayList<>();
        }
    }
    public static class TransExpLevels {
        double X1_mean;
        double X2_mean;
        public TransExpLevels(double x1, double x2) {
            X1_mean = x1;
            X2_mean = x2;
        }
    }

}
