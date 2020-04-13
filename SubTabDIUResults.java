/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import tappas.DataApp.DataType;
import tappas.DlgSelectRows.ColumnDefinition;
import tappas.DlgSelectRows.Params.CompType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDIUResults extends SubTabBase {
    TableView tblData;

    boolean hasData = false;
    Path runScriptPath = null;
    String panel;
    TabProjectDataViz.Panels diuPanel;
    DataType dataType;
    String dataName;
    DataDIU diuData;
    DlgDIUAnalysis.Params diuParams;
    
    private final List<ColumnDefinition> lstColDefs = Arrays.asList(
        //                   id,            colGroup, colTitle,     propName, prefWidth, alignment, selection, Params.CompType
        new ColumnDefinition("GENE",        "", "Gene",             "Gene", 125, "TOP-CENTER", "Gene name", CompType.TEXT),
        new ColumnDefinition("GENEDESC",    "", "Gene Description", "GeneDescription", 200, "", "Gene description", CompType.TEXT),
        new ColumnDefinition("RESULT",      "", "DIU Result",       "DS", 100, "TOP-CENTER", "DIU Result", CompType.LIST),
        new ColumnDefinition("PVALUE",      "", "P-Value",          "PValue", 100, "TOP-RIGHT", "P-Value", CompType.NUMDBL),
        new ColumnDefinition("QVALUE",      "", "Q-Value",          "QValue", 100, "TOP-RIGHT", "Q-Value", CompType.NUMDBL),
        new ColumnDefinition("PODIUMCHG",   "", "Major Isoform Switching",        "PodiumChange", 190, "TOP-CENTER", "Major Isoform Switching", CompType.LIST),
        new ColumnDefinition("PODIUMTIMES",   "", "Switching Times",    "PodiumTimes", 130, "TOP-CENTER", "Podium times", CompType.TEXT),
        new ColumnDefinition("CHGVALUE",    "", "Total Usage Change",         "TotalChange", 150, "TOP-RIGHT", "Total change value", CompType.NUMDBL),
        new ColumnDefinition("CHROMO",      "", "Chr",              "Chromo", 75, "TOP-CENTER", "Chromosome", CompType.LIST),
        new ColumnDefinition("STRAND",      "", "Strand",           "Strand", 75, "TOP-CENTER", "Strand", CompType.STRAND),
        new ColumnDefinition("ISOCNT",      "", "Isoforms",         "Isoforms", 75, "TOP-RIGHT", "Number of gene isoforms", CompType.NUMINT),
        new ColumnDefinition("PROTCNT",      "", "Proteins",         "Proteins", 75, "TOP-RIGHT", "Number of gene proteins", CompType.NUMINT),
        new ColumnDefinition("CODING",      "", "Coding",           "Coding", 75, "TOP-CENTER", "Protein coding gene", CompType.BOOL)
    );
    
    ObservableList<MenuItem> lstTableMenus = null;
    
    public SubTabDIUResults(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = true;
        subTabInfo.btnSelect = true;
        subTabInfo.btnExport = true;
        subTabInfo.btnDataViz = true;
        subTabInfo.btnSignificance = true;
        subTabInfo.btnRun = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            diuData = new DataDIU(project);
            dataType = (DataType) args.get("dataType");
            hasData = diuData.hasDIUData(dataType);
            switch(dataType) {
                case PROTEIN:
                    dataName = "Proteins";
                    panel = TabGeneDataViz.Panels.PROTEIN.name();
                    diuPanel = TabProjectDataViz.Panels.SUMMARYDIUPROT;
                    break;
                case TRANS:
                    dataName = "Transcripts";
                    panel = TabGeneDataViz.Panels.TRANS.name();
                    diuPanel = TabProjectDataViz.Panels.SUMMARYDIUTRANS;
                    break;
                default:
                    result = false;
                    break;
            }
            
            if(result) {
                // parameter data file is required
                result = Files.exists(Paths.get(diuData.getDIUParamsFilepath(dataType)));
                if(result) {
                    diuParams = DlgDIUAnalysis.Params.load(diuData.getDIUParamsFilepath(dataType), project);
                }
                else
                    app.ctls.alertInformation("DIU (" + dataName + ") Results", "Missing DIU parameter file");
            }
            
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    protected void onButtonSignificance() {
        String sigLevel = getSignificanceLevel(diuParams.sigValue, "DIU Significance Level");
        if(!sigLevel.isEmpty()) {
            diuParams.sigValue = Double.parseDouble(sigLevel);
            Utils.saveParams(diuParams.getParams(), diuData.getDIUParamsFilepath(dataType));
            ObservableList<DataDIU.DIUSelectionResults> data = diuData.getDIUSelectionResults(dataType, diuParams.sigValue, true);
            FXCollections.sort(data);
            tblData.setItems(data);
            if(data.size() > 0)
                tblData.getSelectionModel().select(0);
            tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
            searchSet(tblData, "", false);
            setTableSearchActive(tblData, false);

            // update summary display if opened
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("updateDEACombinedResults", "DIU");
            tabBase.processRequest(hm);
            
            if(project.data.isCaseControlExpType()){
                String logfilepath = diuData.getDIULogFilepath(dataType);
                if(project.data.saveDIUResultAnalysis(tblData, project.data.getMatrixDIUFilepath(dataType.toString()))){
                    appendLogLine("Results save correctly.", logfilepath);
                }
            }
        }
    }    
    @Override
    protected void onButtonRun() {
        app.ctlr.runDIUnalysis(dataType);
    }    
    @Override
    protected void onButtonExport() {
        DlgExportData.Config cfg = new DlgExportData.Config(true, "Genes list (IDs only)", true, "Genes ranked list (IDs and values)");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            String filename = "tappAS_DIUGene_" + dataName + ".tsv";
            HashMap<String, String> hmColNames = new HashMap<>();
            boolean allRows = true;
            if(results.dataSelection.equals(DlgExportData.Params.DataSelection.SELECTEDROWS))
                allRows = false;
            if(results.dataType.equals(DlgExportData.Params.DataType.TABLEROWS.name())) {
                app.export.exportTableDataToFile(tblData, allRows, filename);
            }
            else if(results.dataType.equals(DlgExportData.Params.DataType.LIST.name())) {
                hmColNames.put("Gene", "");
                app.export.exportTableDataListToFile(tblData, allRows, hmColNames, filename);
            }
            else if(results.dataType.equals(DlgExportData.Params.DataType.RANKEDLIST.name())) {
                hmColNames.put("Gene", "");
                hmColNames.put("Q-Value", "");
                app.export.exportTableDataListToFile(tblData, allRows, hmColNames, filename);
            }
        }
    }    
    // look into possibly getting rid of this approach to display log data?!!!
    @Override
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        if(hm.containsKey("setTextLog")) {
            String txt = (String) hm.get("setTextLog");
            TextArea txtLog = (TextArea) nodeLog.lookup("#txtA_Log");
            if(txtLog != null)
                txtLog.setText(txt);
        }
        else if(hm.containsKey("appendTextLog")) {
            String txt = (String) hm.get("appendTextLog");
            TextArea txtLog = (TextArea) nodeLog.lookup("#txtA_Log");
            if(txtLog != null)
                txtLog.appendText(txt);
        }
        return obj;
    }
    @Override
    public void search(Object obj, String txt, boolean hide) {
        ignoreHideEvent = true;
        if(obj != null) {
            if(obj.equals(tblData)) {
                if(txt.isEmpty() && !hide)
                    setTableSearchActive(tblData, false);
                else
                    setTableSearchActive(tblData, true);
                System.out.println("DIU tblData search called: '" + txt + ", hide: " + hide);
                TableView<DataDIU.DIUSelectionResults> tblView = (TableView<DataDIU.DIUSelectionResults>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<DataDIU.DIUSelectionResults> items = (ObservableList<DataDIU.DIUSelectionResults>) info.data;
                if(items != null) {
                    ObservableList<DataDIU.DIUSelectionResults> matchItems;
                    if(txt.isEmpty()) {
                        if(hide) {
                            matchItems = FXCollections.observableArrayList();
                            for(DataDIU.DIUSelectionResults data : items) {
                                if(data.isSelected())
                                    matchItems.add(data);
                            }
                        }
                        else {
                            // restore original dataset
                            matchItems = items;
                        }
                    }
                    else {
                        matchItems = FXCollections.observableArrayList();
                        for(DataDIU.DIUSelectionResults data : items) {
                            if(data.getGene().toLowerCase().contains(txt) || data.getGeneDescription().toLowerCase().contains(txt)) {
                                if(!hide || data.isSelected())
                                    matchItems.add(data);
                            }
                        }
                    }
                    tblView.setItems(matchItems);
                }
            }
        }
        ignoreHideEvent = false;
    }

    //
    // Internal Functions
    //
    private void showSubTab(ObservableList<DataDIU.DIUSelectionResults> data) {
        // set options menu
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("View Analysis Log");
        item.setOnAction((event) -> { viewLog(); });
        cm.getItems().add(item);
        subTabInfo.cmOptions = cm;

        // set data visualization menu
        cm = new ContextMenu();
        item = new MenuItem("View Results Summary");
        item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, diuPanel); });
        cm.getItems().add(item);
        subTabInfo.cmDataViz = cm;

        // populate table columns list
        ArrayList<ColumnDefinition> lstTblCols = new ArrayList<>();
        for(ColumnDefinition cd : lstColDefs) {
            // don't add columns that are not available for some analysis
            if(cd.id.equals("PVALUE") && (DlgDIUAnalysis.Params.Method.DEXSEQ.equals(diuParams.method) || DlgDIUAnalysis.Params.Method.MASIGPRO.equals(diuParams.method)) ||
              (cd.id.equals("PODIUMTIMES") && !project.data.isSingleTimeSeriesExpType()) ||
              (cd.id.equals("CHGVALUE") && DlgDIUAnalysis.Params.Method.MASIGPRO.equals(diuParams.method)))
                continue;

            // hide some columns initially - can have for some data types
            cd.visible = !(cd.id.equals("CHROMO") || cd.id.equals("STRAND") || cd.id.equals("ISOCNT") || cd.id.equals("PROTCNT")|| cd.id.equals("CODING"));
            lstTblCols.add(cd);
        }

        // configure table columns
        tblData = (TableView) tabNode.lookup("#tblDA_Stats");
        tblData.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        addTableColumns(tblData, lstTblCols, true, false);
        CheckBox chk = addRowNumsSelCols(tblData);
        chk.selectedProperty().addListener((obs, oldData, newData) -> { 
            changeAllRows(newData? RowSelection.ActionType.SELECT : RowSelection.ActionType.DESELECT);
            tblData.requestFocus();
        });

        // set table popup context menu and include export option
        cm = new ContextMenu();
        addGeneDVItem(cm, tblData, panel);
        app.export.setupTableExport(subTabInfo.subTabBase, cm, true);
        tblData.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblData);
        tblData.setItems(data);
        if(data.size() > 0)
            tblData.getSelectionModel().select(0);

        // handle special columns, once data is available
        for(ColumnDefinition cd : lstTblCols) {
            // handle special cases - only need to do this once
            switch (cd.id) {
                case "CHROMO":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DataDIU.DIUSelectionResults dr : data)
                            hm.put(dr.getChromo(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String chr : hm.keySet())
                            lstSelections.add(chr);
                        Collections.sort(lstSelections, new Chromosome.ChromoComparator());
                        cd.lstSelections = (List) lstSelections;
                        break;
                    }
                case "RESULT":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DataDIU.DIUSelectionResults dr : data)
                            hm.put(dr.getDS(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String val : hm.keySet())
                            lstSelections.add(val);
                        Collections.sort(lstSelections);
                        cd.lstSelections = (List) lstSelections;
                        break;
                    }
                case "PODIUMCHG":
                {
                    HashMap<String, Object> hm = new HashMap<>();
                    for(DataDIU.DIUSelectionResults dr : data)
                        hm.put(dr.getPodiumChange(), null);
                    ArrayList<String> lstSelections = new ArrayList<>();
                    for(String val : hm.keySet())
                        lstSelections.add(val);
                    Collections.sort(lstSelections);
                    cd.lstSelections = (List) lstSelections;
                        break;
                    }
            }
        }

        // setup table search functionality
        tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
        setupTableSearch(tblData);

        // setup table menu to include selection column and custom annotation columns - make default focus node
        setupTableMenu(tblData, DataType.GENE, true, true, lstTblCols, data, DataDIU.DIUSelectionResults.class.getName());
        onSelectFocusNode = tblData;
        setFocusNode(tblData, true);
    }
    public void viewLog() {
        String logdata = app.getLogFromFile(diuData.getDIULogFilepath(dataType), App.MAX_LOG_LINES);
        viewLog(logdata);
    }
    
    //
    // Static Functions
    //
    
    // Just add all possible data type sub tabs - no need to bother with experiment type, etc.
    // List is used to close all subtabs that start with given panel names
    // Used to make sure we close all associated subtabs when running the analysis - data will be deleted
    public static String getAssociatedDVSubTabs(DataType dataType) {
        String subTabs = "";
        switch(dataType) {
            case PROTEIN:
                subTabs = TabProjectDataViz.Panels.SUMMARYDIUPROT.toString();
                break;
            case TRANS:
                subTabs = TabProjectDataViz.Panels.SUMMARYDIUTRANS.toString();
                break;
        }
        return subTabs;
    }

    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        if(hasData) {
            ObservableList<DataDIU.DIUSelectionResults> data = diuData.getDIUSelectionResults(dataType, diuParams.sigValue, true);
            FXCollections.sort(data);
            showSubTab(data);
        }
        else {
            String name = project.data.isTimeCourseExpType()? "DIU_TimeCourse.R" : "DIU.R";
            runScriptPath = app.data.getTmpScriptFileFromResource(name);
            service = new DataLoadService();
            service.initialize();
            service.start();
        }
    }
    private class DataLoadService extends TaskHandler.ServiceExt {
        ObservableList<DataDIU.DIUSelectionResults> data = null;
        
        // do all service FX thread initialization
        @Override
        public void initialize() {
            showRunAnalysisScreen(true, "Differential Isoform Usage Analysis in Progress...");
        }
        @Override
        protected void onRunning() {
            showProgress();
            setDisableAbortTaskButton(false);
        }
        @Override
        protected void onStopped() {
            hideProgress();
            setDisableAbortTaskButton(true);
        }
        @Override
        protected void onFailed() {
            String logfilepath = diuData.getDIULogFilepath(dataType);
            java.lang.Throwable e = getException();
            if(e != null)
                appendLogLine("DIU Analysis failed - task aborted. Exception: " + e.getMessage(), logfilepath);
            else
                appendLogLine("DIU Analysis failed - task aborted.", logfilepath);
            app.ctls.alertWarning("Differential Isoform Usage Analysis", "DIU failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                app.logInfo(dataName + " DIU completed successfully.");
                HashMap<String, Object> hm = new HashMap<>();
                hm.put("updateDEACombinedResults", "DIU");
                tabBase.processRequest(hm);
                showRunAnalysisScreen(false, "");
                pageDataReady = true;
                showSubTab(data);
                if(project.data.isCaseControlExpType()){
                    String logfilepath = diuData.getDIULogFilepath(dataType);
                    if(project.data.saveDIUResultAnalysis(tblData, project.data.getMatrixDIUFilepath(dataType.toString()))){
                        appendLogLine("Results save correctly.", logfilepath);
                    }
                }
            }
            else {
                app.logInfo(dataName + " DIU " + (taskAborted? "aborted by request." : "failed."));
                app.ctls.alertWarning(dataName + " DIU", "Differential Isoform Usage Analysis " + (taskAborted? "aborted by request." : "failed."));
                setRunAnalysisScreenTitle("Differential Isoform Usage Analysis " + (taskAborted? "Aborted by Request" : "Failed"));
            }
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String logfilepath = diuData.getDIULogFilepath(dataType);
                    outputFirstLogLine(dataName + " Differential Isoform Usage Analysis thread running...", logfilepath);
                    appendLogLine("Generating required input files...", logfilepath);
                    if(diuData.genDIUInputFiles(dataType)) { 
                        // setup script arguments and run it
                        appendLogLine("Starting R script...", logfilepath);
                        List<String> lst = new ArrayList<>();
                        lst.add(app.data.getRscriptFilepath());
                        lst.add(runScriptPath.toString());
                        String d = diuParams.dataType.name();
                        lst.add("-d" + d.toLowerCase());
                        String method = diuParams.method.name();
                        lst.add("-m" + method);
                        lst.add("-i" + project.data.getProjectDataFolder());
                        lst.add("-o" + diuData.getDIUFolder());
                        String f = "0";
                        String t = "FOLD";
                        if(diuParams.filter){
                            f = "" + diuParams.filterFC;
                            t = "" + diuParams.filteringType;
                        }
                        lst.add("-f" + f);
                        lst.add("-t" + t);
                        if(project.data.isTimeCourseExpType()) {
                            lst.add("-u" + diuParams.degree);
                            String cmp = "any";
                            String r = "0";
                            if(project.data.isMultipleTimeSeriesExpType()) {
                                r = diuParams.strictType.name();
                                cmp = "group";
                            }
                            lst.add("-k" + cmp);
                            lst.add("-r" + r);
                        }
                        app.logInfo("Running Differential Isoform Usage Analysis...");
                        app.logDebug(dataName + " DIU arguments: " + lst);
                        runScript(taskInfo, lst, dataName + " Differential Isoform Usage Analysis", logfilepath);
                    
                        // get results if available
                        ObservableList<DataDIU.DIUSelectionResults> results = diuData.getDIUSelectionResults(dataType, diuParams.sigValue, true);
                        if(!results.isEmpty()) {
                            data = results;
                            FXCollections.sort(data);
                        }

                        // remove script file from temp folder
                        Utils.removeFile(runScriptPath);
                        runScriptPath = null;
                    }
                    else {
                        appendLogLine("Unable to create required files for " + dataName + " Differential Isoform Usage Analysis.", logfilepath);
                        app.logError("Unable to create required files for " + dataName + " Differential Isoform Usage Analysis");
                    }
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo(dataName + " Differential Isoform Usage Analysis", task);
            return task;
        }
    }
}
