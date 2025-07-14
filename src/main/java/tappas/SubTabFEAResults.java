/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Side;
import javafx.scene.control.*;
import tappas.DataFEA.FEASelectionResults;
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
public class SubTabFEAResults extends SubTabBase {
    TableView tblData;

    boolean hasData = false;
    Path runScriptPath = null;
    String analysisId = "";
    DataFEA feaData;
    DlgFEAnalysis.Params feaParams = new DlgFEAnalysis.Params();
    private final List<ColumnDefinition> lstColDefs = Arrays.asList(
        //                   id,            colGroup, colTitle,     propName, prefWidth, alignment, selection, Params.CompType
        new ColumnDefinition("SOURCE",     "", "Source",          "Source", 120, "TOP-CENTER", "Source", CompType.LIST),
        new ColumnDefinition("FEATURE",     "", "Feature",          "Feature", 100, "TOP-CENTER", "Feature", CompType.LIST),
        new ColumnDefinition("FEATUREID",     "", "Feature ID",     "FeatureId", 100, "TOP-CENTER", "Feature ID", CompType.TEXT),
        new ColumnDefinition("DESCRIPTION", "", "Description",      "Description", 200, "", "Description", CompType.TEXT),
        new ColumnDefinition("RESULT",      "", "Significant",      "Significant", 100, "TOP-CENTER", "Significant", CompType.LIST),
        new ColumnDefinition("OPVALUE",      "", "Over P-Value",     "OverPValue", 110, "TOP-RIGHT", "Over P-Value", CompType.NUMDBL),
        new ColumnDefinition("UPVALUE",      "", "Under P-Value",    "UnderPValue", 120, "TOP-RIGHT", "Under P-Value", CompType.NUMDBL),
        new ColumnDefinition("APVALUE",      "", "Adjusted P-Value", "AdjustedPValue", 140, "TOP-RIGHT", "Adjusted P-Value", CompType.NUMDBL),
        new ColumnDefinition("TESTCNT",     "", "Test with ID",     "NumInCat", 100, "TOP-RIGHT", "Test Count with Feature ID", CompType.NUMINT),
        new ColumnDefinition("BKGNDCNT",    "", "Total with ID",    "NumTotalInCat", 100, "TOP-RIGHT", "Total Count with Feature ID", CompType.NUMINT)
    );
    ContextMenu cmCluster = null;
    ObservableList<MenuItem> lstTableMenus = null;
    
    public SubTabFEAResults(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = true;
        subTabInfo.btnSelect = true;
        subTabInfo.btnExport = true;
        subTabInfo.btnDataViz = true;
        subTabInfo.btnClusters = true;
        subTabInfo.btnSignificance = true;
        subTabInfo.btnRun = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            feaData = new DataFEA(project);
            // check if id was not passed, allow passing as part of subTabId
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectData.Panels.STATSFEA.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectData.Panels.STATSFEA.name().length()).trim());
            }
            if(args.containsKey("id")) {
                analysisId = (String) args.get("id");

                // FEA parameter file is required
                result = Files.exists(Paths.get(feaData.getFEAParamsFilepath(analysisId)));
                if(result) {
                    feaParams = DlgFEAnalysis.Params.load(feaData.getFEAParamsFilepath(analysisId));
                    String shortName = getShortName(feaParams.name);
                    subTabInfo.title = "FEA: " + shortName;
                    subTabInfo.tooltip = "Functional Enrichment Analysis statistical results for '" + feaParams.name.trim() + "'";
                    setTabLabel(subTabInfo.title, subTabInfo.tooltip);
                    hasData = feaData.hasFEAData(feaParams.dataType.name(), analysisId);
                }
                else
                    app.ctls.alertInformation("FEA Results", "Missing FEA parameter file");
            }
            else
                result = false;

            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    protected void onButtonSignificance() {
        String sigLevel = getSignificanceLevel(feaParams.sigValue, "FEA Significance Level");
        if(!sigLevel.isEmpty()) {
            Utils.removeFile(Paths.get(feaData.getFEAClusterDoneFilepath(analysisId)));
            feaParams.sigValue = Double.parseDouble(sigLevel);
            feaParams.save(feaData.getFEAParamsFilepath(analysisId));
            // set table data and select first row
            ObservableList<FEASelectionResults> data = feaData.getFEASelectionResults(feaParams.dataType.name(), analysisId, feaParams.sigValue);
            setTableData(data);
            // clear any pre-existing search values - worked but left table yellow
            tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
            searchSet(tblData, "", false);
            setTableSearchActive(tblData, false);

            // update summary display if opened
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("updateEACombinedResults", "FEA");
            hm.put("featureId", analysisId);
            tabBase.processRequest(hm);
        }
    }    
    @Override
    protected void onButtonRun() {
        app.ctlr.runFEAnalysis(analysisId);
    }    
    @Override
    protected void onButtonExport() {
        String itemsName = app.data.getDataTypePlural(feaParams.dataType.name());
        String properItemsName = itemsName.substring(0, 1).toUpperCase() + itemsName.substring(1);
        DlgExportData.Config cfg = new DlgExportData.Config(true, "Feature IDs list (IDs only)", true, "Feature IDs ranked list (IDs and AdjP-Values)");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            String prefix = "tappAS_FEA_"+feaParams.name.trim()+"_" + properItemsName + "_FeatureIDs";
            String filename = prefix + ".tsv";
            HashMap<String, String> hmColNames = new HashMap<>();
            boolean allRows = true;
            if(results.dataSelection.equals(DlgExportData.Params.DataSelection.SELECTEDROWS))
                allRows = false;
            if(results.dataType.equals(DlgExportData.Params.DataType.TABLEROWS.name())) {
                app.export.exportTableDataToFile(tblData, allRows, filename);
            }
            else if(results.dataType.equals(DlgExportData.Params.DataType.LIST.name())) {
                hmColNames.put("Feature ID", "");
                filename = prefix + "_List.tsv";
                app.export.exportTableDataListToFile(tblData, allRows, hmColNames, filename);
            }
            else if(results.dataType.equals(DlgExportData.Params.DataType.RANKEDLIST.name())) {
                hmColNames.put("Feature ID", "");
                hmColNames.put("Adjusted P-Value", "");
                filename = prefix + "_RankedList.tsv";
                app.export.exportTableDataListToFile(tblData, allRows, hmColNames, filename);
            }
        }
    }    
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
                System.out.println("FEA tblData search called: '" + txt + ", hide: " + hide);
                TableView<FEASelectionResults> tblView = (TableView<FEASelectionResults>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<FEASelectionResults> items = (ObservableList<FEASelectionResults>) info.data;
                if(items != null) {
                    ObservableList<FEASelectionResults> matchItems;
                    if(txt.isEmpty()) {
                        if(hide) {
                            matchItems = FXCollections.observableArrayList();
                            for(FEASelectionResults data : items) {
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
                        for(FEASelectionResults data : items) {
                            if(data.getFeatureId().toLowerCase().contains(txt) || data.getDescription().toLowerCase().contains(txt)) {
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
    @Override
    protected void onButtonClusters() {
        if(cmCluster == null) {
            cmCluster = new ContextMenu();
            MenuItem item = new MenuItem("View Clusters");
            if(feaParams.hmFeatures.size()>1)
                app.ctls.alertInformation("FEA Cluster Information", "You have to run the analysis just for one feature to be able to run a cluster.");
            else{
            item.setOnAction((event) -> { app.ctlr.viewFEAClusters(analysisId); });
            cmCluster.getItems().add(item);
            cmCluster.getItems().add(new SeparatorMenuItem());
            item = new MenuItem("Run Cluster Analysis...");
            item.setOnAction((event) -> { app.ctlr.runFEAClusterAnalysis(analysisId, feaData.getFEAClusterParams(analysisId)); });
            cmCluster.getItems().add(item);
            cmCluster.show(btnClusters, Side.BOTTOM, 0, 0);
            }
        }
        // WARNING: hard coded index
        cmCluster.getItems().get(0).setDisable(!feaData.hasFEAClusterData(analysisId));
        cmCluster.show(btnClusters, Side.BOTTOM, 0, 0);
    }
    
    //
    // Internal Functions
    //
    private void showSubTab(ObservableList<FEASelectionResults> data) {
        tblData = (TableView) tabNode.lookup("#tblEA_StatsFEA");

        // set options menu
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("View Analysis Log");
        item.setOnAction((event) -> { viewLog(); });
        cm.getItems().add(item);
        subTabInfo.cmOptions = cm;

        // create data visualization context menu
        cm = new ContextMenu();
        item = new MenuItem("View Results Summary");
        item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, TabProjectDataViz.Panels.SUMMARYFEA, analysisId); });
        cm.getItems().add(item);
        item = new MenuItem("View Enriched Features Chart");
        item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, TabProjectDataViz.Panels.ENRICHEDFEA, analysisId); });
        cm.getItems().add(item);
        subTabInfo.cmDataViz = cm;

        // populate table columns list
        ArrayList<ColumnDefinition> lstTblCols = new ArrayList<>();
        for(ColumnDefinition cd : lstColDefs) {
            // hide some columns initially, if any
            lstTblCols.add(cd);
        }

        // configure table columns
        tblData.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        addTableColumns(tblData, lstTblCols, false, false);
        CheckBox chk = addRowNumsSelCols(tblData);
        chk.selectedProperty().addListener((obs, oldData, newData) -> { 
            changeAllRows(newData? RowSelection.ActionType.SELECT : RowSelection.ActionType.DESELECT);
            tblData.requestFocus();
        });

        // set table popup context menu and include export option
        cm = new ContextMenu();
        item = new MenuItem("Drill down data");
        item.setOnAction((event) -> { drillDownData(tblData); });
        cm.getItems().add(item);
        app.export.setupTableExport(subTabInfo.subTabBase, cm, true);
        tblData.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblData);
        setTableData(data);

        // handle special columns, once data is available
        for(ColumnDefinition cd : lstTblCols) {
            // handle special cases - only need to do this once
            switch (cd.id) {
//                case "SOURCE":
//                    {
//                        HashMap<String, Object> hm = new HashMap<>();
//                        for(FEASelectionResults dr : data)
//                            hm.put(dr.getSource(), null);
//                        ArrayList<String> lstSelections = new ArrayList<>();
//                        for(String cat : hm.keySet())
//                            lstSelections.add(cat);
//                        Collections.sort(lstSelections);
//                        cd.lstSelections = (List) lstSelections;
//                        break;
//                    }
                case "FEATURE":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(FEASelectionResults dr : data)
                            hm.put(dr.getFeature(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String cat : hm.keySet())
                            lstSelections.add(cat);
                        Collections.sort(lstSelections);
                        cd.lstSelections = (List) lstSelections;
                        break;
                    }
                case "RESULT":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(FEASelectionResults dr : data)
                            hm.put(dr.getSignificant(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String cat : hm.keySet())
                            lstSelections.add(cat);
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
        setupTableMenu(tblData, feaParams.dataType, true, false, lstTblCols, data, FEASelectionResults.class.getName());
        setFocusNode(tblData, true);
    }
    private void viewLog() {
        String logdata = app.getLogFromFile(feaData.getFEALogFilepath(analysisId), App.MAX_LOG_LINES);
        viewLog(logdata);
    }
    private void drillDownData(TableView tbl) {
        ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
        if(lstIdxs.size() != 1) {
            app.ctls.alertInformation("FEA Drill Down Data", "You have multiple rows highlighted.\nHighlight a single row with the feature you are interested in.");
        }
        else {
            FEASelectionResults rdata = (FEASelectionResults) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
            String featureId = rdata.getFeatureId().trim();
            String description = rdata.getDescription().trim();
            String fileFeatures = Paths.get(feaData.getFEAFeaturesFilepath(analysisId, feaParams.dataType.name())).toString();
            ArrayList<String> lstTestItems = Utils.loadSingleItemListFromFile(feaData.getFEATestListFilepath(feaParams.dataType.name(), analysisId), false);
            app.ctlr.drillDownFEAFeatures(feaParams.dataType, featureId, description, fileFeatures, lstTestItems);
        }
    }
    private void setTableData(ObservableList<FEASelectionResults> data) {
        FXCollections.sort(data);
        tblData.setItems(data);
        if(data.size() > 0)
            tblData.getSelectionModel().select(0);
    }  
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        if(hasData) {
            ObservableList<FEASelectionResults> data = feaData.getFEASelectionResults(feaParams.dataType.name(), analysisId, feaParams.sigValue);
            showSubTab(data);
        }
        else {
            runScriptPath = app.data.getTmpScriptFileFromResource("FEA.R");
            service = new DataLoadService();
            service.initialize();
            service.start();
        }
    }
    
    private class DataLoadService extends TaskHandler.ServiceExt {
        ObservableList<FEASelectionResults> data = null;

        // do all service FX thread initialization
        @Override
        public void initialize() {
            // show run analysis screen
            showRunAnalysisScreen(true, "Functional Enrichment Analysis in Progress...");
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
            String logfilepath = feaData.getFEALogFilepath(analysisId);
            java.lang.Throwable e = getException();
            if(e != null)
                appendLogLine("FEAnalysis failed - task aborted. Exception: " + e.getMessage(), logfilepath);
            else
                appendLogLine("FEAnalysis failed - task aborted.", logfilepath);
            app.ctls.alertWarning("Funtional Enrichment Analysis", "FEA failed - task aborted");
            Utils.removeFolderFilesContaining(Paths.get(feaData.getFEAFolder()), "." + analysisId + ".");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                app.logInfo("FEA completed successfully.");
                HashMap<String, Object> hm = new HashMap<>();
                hm.put("updateEACombinedResults", "FEA");
                hm.put("featureId", analysisId);
                tabBase.processRequest(hm);
                
                // analysis succeeded, hide run analysis screen
                // set page data ready flag and show subtab
                showRunAnalysisScreen(false, "");
                pageDataReady = true;
                showSubTab(data);
            }
            else {
                app.logInfo("FEA " + (taskAborted? "aborted by request." : "failed."));
                app.ctls.alertWarning("Functional Enrichment Analysis", "FEA " + (taskAborted? "aborted by request." : "failed."));
                setRunAnalysisScreenTitle("Functional Enrichment Analysis " + (taskAborted? "Aborted by Request" : "Failed"));
                Utils.removeFolderFilesContaining(Paths.get(feaData.getFEAFolder()), "." + analysisId + ".");
            }
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String logfilepath = feaData.getFEALogFilepath(analysisId);
                    outputFirstLogLine("Functional Enrichment Analysis thread running...", logfilepath);
                    appendLogLine("Generating required input files...", logfilepath);
                    if(feaData.genFEAInputFiles(analysisId, feaParams, subTabInfo.subTabBase, logfilepath)) { 
                        // setup script arguments and run it
                        appendLogLine("Starting R script...", logfilepath);
                        List<String> lst = new ArrayList<>();
                        lst.add(app.data.getRscriptFilepath());
                        lst.add(runScriptPath.toString());
                        String method = feaParams.method.name().substring(0, 1) + feaParams.method.name().substring(1).toLowerCase();
                        Integer scnt = 2000;
                        String useWOCat = "0";
                        if(feaParams.useWOCat.equals(DlgFEAnalysis.Params.YesNo.YES))
                            useWOCat = "1";
                        lst.add("-m" + method);
                        lst.add("-s" + scnt.toString());
                        lst.add("-u" + useWOCat);
                        lst.add("-d" + feaParams.dataType.name().toLowerCase());
                        lst.add("-f" + analysisId);
                        lst.add("-i" + feaData.getFEAFolder());
                        lst.add("-o" + feaData.getFEAFolder());
                        lst.add("-g" + app.data.getAppGOTermsAncesFilepath());
                        app.logInfo("Running Functional Enrichment Analysis script:\n    " + lst);
                        runScript(taskInfo, lst, "Functional Enrichment Analysis", logfilepath);
                        ObservableList<FEASelectionResults> results = feaData.getFEASelectionResults(feaParams.dataType.name(), analysisId, feaParams.sigValue);
                        if(!results.isEmpty())
                            data = results;

                        // remove script file from temp folder
                        Utils.removeFile(runScriptPath);
                        runScriptPath = null;
                    }
                    else {
                        appendLogLine("Unable to create required files for Functional Enrichment Analysis.", logfilepath);
                        app.logError("Unable to create required files for Functional Enrichment Analysis");
                    }
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("Functional Enrichment Analysis", task);
            return task;
        }
    }

    //
    // Static Functions
    //
    
    // Just add all possible sub tabs - no need to bother with experiment type, etc.
    // List is used to close all subtabs that start with given panel names
    // Used to make sure we close all associated subtabs when running the analysis - data will be deleted
    public static String getAssociatedDVSubTabs(String id) {
        String subTabs = TabProjectDataViz.Panels.SUMMARYFEA.toString() + id + ";";
        subTabs += TabProjectDataViz.Panels.ENRICHEDFEA.toString() + id + ";";
        subTabs += TabProjectDataViz.Panels.CLUSTERSFEA.toString() + id + ";";
        subTabs += TabProjectDataViz.Panels.CLUSTERSGOFEA.toString() + id;
        return subTabs;
    }
}
