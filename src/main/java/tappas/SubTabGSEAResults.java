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
import tappas.DataGSEA.GSEASelectionResults;
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
public class SubTabGSEAResults extends SubTabBase {
    TableView tblData;

    boolean hasData = false;
    Path runScriptPath = null;
    String analysisId = "";
    DataGSEA gseaData;
    int items_rank1 = 0;
    int items_rank2 = 0;
    DlgGSEAnalysis.Params gseaParams = new DlgGSEAnalysis.Params();
    private final List<ColumnDefinition> lstColDefs = Arrays.asList(
        //                   id,            colGroup, colTitle,     propName, prefWidth, alignment, selection, Params.CompType
        new ColumnDefinition("FEATURE",    "", "Feature",          "Feature", 100, "", "Feature", CompType.LIST),
        new ColumnDefinition("FEATUREID",     "", "Feature ID",       "FeatureId", 100, "", "Feature ID", CompType.TEXT),
        new ColumnDefinition("DESCRIPTION", "", "Description",      "Description", 200, "", "Description", CompType.TEXT),
        new ColumnDefinition("RESULT",      "", "Significant",      "Significant", 100, "TOP-CENTER", "Significant", CompType.LIST),
        new ColumnDefinition("OPVALUE",     "", "Over P-Value",      "OverPValue", 130, "TOP-RIGHT", "Over P-Value", CompType.NUMDBL),
        new ColumnDefinition("APVALUE",     "", "Adjusted P-Value",  "AdjustedPValue", 140, "TOP-RIGHT", "Adjusted P-Value", CompType.NUMDBL),
        // 2 new columns to multidimensional option - the others OverPValue and Adjusted PValue
        new ColumnDefinition("OPVALUE2",     "", "Over P-Value2",      "OverPValue2", 130, "TOP-RIGHT", "Over P-Value2", CompType.NUMDBL),
        new ColumnDefinition("APVALUE2",     "", "Adjusted P-Value2",  "AdjustedPValue2", 140, "TOP-RIGHT", "Adjusted P-Value2", CompType.NUMDBL)
    );
    ContextMenu cmCluster = null;
    ObservableList<MenuItem> lstTableMenus = null;
    
    public SubTabGSEAResults(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = true;
        subTabInfo.btnSelect = true;
        subTabInfo.btnExport = true;
        subTabInfo.btnDataViz = true;
        subTabInfo.btnClusters = false; //not implemented for unidimensional GSEA arreglar para que se muestre aquí el de multidimensional
        subTabInfo.btnSignificance = true;
        subTabInfo.btnRun = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            gseaData = new DataGSEA(project);

            // check if id was not passed, allow passing as part of subTabId
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectData.Panels.STATSGSEA.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectData.Panels.STATSGSEA.name().length()).trim());
            }
            if(args.containsKey("id")) {
                analysisId = (String) args.get("id");

                // GSEA parameter file is required
                result = Files.exists(Paths.get(gseaData.getGSEAParamsFilepath(analysisId)));
                if(result) {
                    gseaParams = DlgGSEAnalysis.Params.load(gseaData.getGSEAParamsFilepath(analysisId));

                    //check if multi
                    if(gseaParams.useMulti.equals("TRUE")){
                        subTabInfo.btnClusters = true;
                        boolean new_result = super.initialize(tabBase, subTabInfo, args);

                        if(new_result){
                            String shortName = getShortName(gseaParams.name);
                            subTabInfo.title = "GSEA: " + shortName;
                            subTabInfo.tooltip = "Gene Set Enrichment Analysis statistical results for '" + gseaParams.name.trim() + "'";
                            setTabLabel(subTabInfo.title, subTabInfo.tooltip);
                            hasData = gseaData.hasGSEAData(gseaParams.dataType.name(), analysisId);
                        }
                    }else{
                        String shortName = getShortName(gseaParams.name);
                        subTabInfo.title = "GSEA: " + shortName;
                        subTabInfo.tooltip = "Gene Set Enrichment Analysis statistical results for '" + gseaParams.name.trim() + "'";
                        setTabLabel(subTabInfo.title, subTabInfo.tooltip);
                        hasData = gseaData.hasGSEAData(gseaParams.dataType.name(), analysisId);
                    }
                }
                else
                    app.ctls.alertInformation("GSEA Results", "Missing GSEA parameter file");
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
        String sigLevel = getSignificanceLevel(gseaParams.sigValue, "GSEA Significance Level");
        if(!sigLevel.isEmpty()) {
            Utils.removeFile(Paths.get(gseaData.getGSEAClusterDoneFilepath(analysisId)));
            gseaParams.sigValue = Double.parseDouble(sigLevel);
            gseaParams.save(gseaData.getGSEAParamsFilepath(analysisId));
            ObservableList<GSEASelectionResults> data = gseaData.getGSEASelectionResults(gseaParams.dataType.name(), analysisId, gseaParams.sigValue);
            setTableData(data);
            // clear any pre-existing search values - worked but left table yellow
            tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
            searchSet(tblData, "", false);
            setTableSearchActive(tblData, false);

            // update summary display if opened
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("updateEACombinedResults", "GSEA");
            hm.put("featureId", analysisId);
            tabBase.processRequest(hm);
        }
    }    
    @Override
    protected void onButtonRun() {
        app.ctlr.runGSEAnalysis(analysisId);
    }    
    @Override
    protected void onButtonExport() {
        String itemsName = app.data.getDataTypePlural(gseaParams.dataType.name());
        String properItemsName = itemsName.substring(0, 1).toUpperCase() + itemsName.substring(1);
        DlgExportData.Config cfg = new DlgExportData.Config(true, "Feature IDs list (IDs only)", true, "Feature IDs ranked list (IDs and AdjP-Values)");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            String prefix = "tappAS_GSEA_"+gseaParams.name.trim()+"_" + properItemsName + "_FeatureIDs";
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

    protected void onButtonClusters() {
        // only for multidimensional
        if(cmCluster == null) {
            cmCluster = new ContextMenu();
            MenuItem item = new MenuItem("View Clusters");
            if(gseaParams.useMulti.equals("TRUE"))
                item = new MenuItem("Run Multidimensional Enrichment Clustering");
            item.setOnAction((event) -> {
                app.ctlr.runGSEAClusterAnalysis(analysisId, gseaData.getGSEAClusterParams(analysisId));
                //after run he have to see app.ctlr.viewGSEAClusters(analysisId);
            });
            cmCluster.getItems().add(item);
        }
        // WARNING: hard coded index
        //cmCluster.getItems().get(0).setDisable(!gseaData.hasGSEAClusterData(analysisId));
        cmCluster.show(btnClusters, Side.BOTTOM, 0, 0);
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
                System.out.println("GSEA tblData search called: '" + txt + ", hide: " + hide);
                TableView<GSEASelectionResults> tblView = (TableView<GSEASelectionResults>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<GSEASelectionResults> items = (ObservableList<GSEASelectionResults>) info.data;
                if(items != null) {
                    ObservableList<GSEASelectionResults> matchItems;
                    if(txt.isEmpty()) {
                        if(hide) {
                            matchItems = FXCollections.observableArrayList();
                            for(GSEASelectionResults data : items) {
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
                        for(GSEASelectionResults data : items) {
                            if(data.getFeatureId().toLowerCase().contains(txt) ||data.getFeature().toLowerCase().contains(txt) || data.getDescription().toLowerCase().contains(txt)) {
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
    private void showSubTab(ObservableList<GSEASelectionResults> data) {
        tblData = (TableView) tabNode.lookup("#tblEA_StatsGSEA");

        // set options menu
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("View Analysis Log");
        item.setOnAction((event) -> { viewLog(); });
        cm.getItems().add(item);
        subTabInfo.cmOptions = cm;

        // create data visualization context menu
        cm = new ContextMenu();
        item = new MenuItem("View Results Summary");
        item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, TabProjectDataViz.Panels.SUMMARYGSEA, analysisId); });
        cm.getItems().add(item);
        item = new MenuItem("View Enriched Features Chart");
        item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, TabProjectDataViz.Panels.ENRICHEDGSEA, analysisId); });
        cm.getItems().add(item);
        // only for multidimensional --- NOW IT IS IN CLUSTER BUTTON
        /*if(gseaParams.useMulti.equals("TRUE")) {
            item = new MenuItem("View Multidimensional Enrichment Summary");
            item.setOnAction((event) -> {
                app.ctlr.runGSEAClusterAnalysis(analysisId, gseaData.getGSEAClusterParams(analysisId));
                //after run he have to see app.ctlr.viewFEAClusters(analysisId);
            });
            cm.getItems().add(item);
        }*/
        subTabInfo.cmDataViz = cm;

        // populate table columns list
        ArrayList<ColumnDefinition> lstTblCols = new ArrayList<>();
        for(ColumnDefinition cd : lstColDefs) {
            // don't add columns that are not available for some analysis - just for multivariant mdgsa
            if(cd.id.equals("OPVALUE2") && (gseaParams.useMulti.equals("FALSE")) ||
              (cd.id.equals("APVALUE2") && (gseaParams.useMulti.equals("FALSE"))))
                continue;
            // hide some columns initially
            //

            // md mdgsa works always for two rankeds list - different in time series?
            if(gseaParams.useMulti.equals("TRUE")){
                if(project.data.isCaseControlExpType()) {
                    if (cd.id.equals("OPVALUE")) {
                        cd.colTitle = "Over P-Value - " + gseaParams.rankedList1.name();
                        cd.selection = "Over P-Value - " + gseaParams.rankedList1.name();
                        cd.prefWidth = cd.colTitle.length()*8;
                    }
                    if (cd.id.equals("APVALUE")) {
                        cd.colTitle = "Adjusted P-Value - " + gseaParams.rankedList1.name();
                        cd.selection = "Adjusted P-Value - " + gseaParams.rankedList1.name();
                        cd.prefWidth = cd.colTitle.length()*8;
                    }
                    if (cd.id.equals("OPVALUE2")) {
                        cd.colTitle = "Over P-Value - " + gseaParams.rankedList2.name();
                        cd.selection = "Over P-Value - " + gseaParams.rankedList2.name();
                        cd.prefWidth = cd.colTitle.length()*8;
                    }
                    if (cd.id.equals("APVALUE2")) {
                        cd.colTitle = "Adjusted P-Value - " + gseaParams.rankedList2.name();
                        cd.selection = "Adjusted P-Value - " + gseaParams.rankedList2.name();
                        cd.prefWidth = cd.colTitle.length()*8;
                    }
                }else{
                    //we have to think how to manage time points in mdgsa - md and how to get dynamic columns!!!
                }
            }
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
                case "FEATURE":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(GSEASelectionResults dr : data)
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
                        for(GSEASelectionResults dr : data)
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
        setupTableMenu(tblData, gseaParams.dataType, true, false, lstTblCols, data, GSEASelectionResults.class.getName());
        setFocusNode(tblData, true);
    }
    private void viewLog() {
        String logdata = app.getLogFromFile(gseaData.getGSEALogFilepath(analysisId), App.MAX_LOG_LINES);
        viewLog(logdata);
    }
    private void drillDownData(TableView tbl) {
        ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
        if(lstIdxs.size() != 1) {
            app.ctls.alertInformation("GSEA Drill Down Data", "You have multiple rows highlighted.\nHighlight a single row with the feature you are interested in.");
        }
        else {
            GSEASelectionResults rdata = (GSEASelectionResults) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
            String featureId = rdata.getFeatureId().trim();
            String description = rdata.getDescription().trim();
            String fileFeatures = Paths.get(gseaData.getGSEAFeaturesFilepath(analysisId, gseaParams.dataType.name(), 1)).toString();
            String fileFeatures2 = Paths.get(gseaData.getGSEAFeaturesFilepath(analysisId, gseaParams.dataType.name(), 2)).toString();
            // search in both list inside method drillDownGSEAFeatures
            ArrayList<String> lstTestItems = Utils.loadSingleItemListFromFile(gseaData.getGSEARankedListFilepath(gseaParams.dataType.name(), analysisId, 1), false);
            if(lstTestItems.isEmpty())
                lstTestItems = Utils.loadSingleItemListFromFile(gseaData.getGSEARankedListFilepath(gseaParams.dataType.name(), analysisId, 2), false);
            app.ctlr.drillDownGSEAFeatures(gseaParams.dataType, featureId, description, fileFeatures, fileFeatures2, lstTestItems);
        }
    }
    private void setTableData(ObservableList<GSEASelectionResults> data) {
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
            ObservableList<GSEASelectionResults> data = gseaData.getGSEASelectionResults(gseaParams.dataType.name(), analysisId, gseaParams.sigValue);
            Integer[] itemRankedList = gseaData.getGSEAItemsResults(gseaParams.dataType.name(), analysisId);
            showSubTab(data);
        }
        else {
            runScriptPath = app.data.getTmpScriptFileFromResource("GSEA.R");
            service = new DataLoadService();
            service.initialize();
            service.start();
        }
    }
    private class DataLoadService extends TaskHandler.ServiceExt {
        ObservableList<GSEASelectionResults> data = null;
        
        @Override
        public void initialize() {
            showRunAnalysisScreen(true, "Gene Set Enrichment Analysis in Progress...");
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
            String logfilepath = gseaData.getGSEALogFilepath(analysisId);
            java.lang.Throwable e = getException();
            if(e != null)
                appendLogLine("GSEAnalysis failed - task aborted. Exception: " + e.getMessage(), logfilepath);
            else
                appendLogLine("GSEAnalysis failed - task aborted.", logfilepath);
            app.ctls.alertWarning("Gene Set Enrichment Analysis", "GSEA failed - task aborted");
            Utils.removeFolderFilesContaining(Paths.get(gseaData.getGSEAFolder()), "." + analysisId + ".");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                HashMap<String, Object> hm = new HashMap<>();
                hm.put("updateEACombinedResults", "GSEA");
                hm.put("featureId", analysisId);
                tabBase.processRequest(hm);
                showRunAnalysisScreen(false, "");
                pageDataReady = true;
                showSubTab(data);
            }
            else {
                app.logInfo("GSEA " + (taskAborted? "aborted by request." : "failed."));
                app.ctls.alertWarning("Gene Set Enrichment Analysis", "GSEA " + (taskAborted? "aborted by request." : "failed."));
                setRunAnalysisScreenTitle("Gene Set Enrichment Analysis " + (taskAborted? "Aborted by Request" : "Failed"));
                Utils.removeFolderFilesContaining(Paths.get(gseaData.getGSEAFolder()), "." + analysisId + ".");
            }
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String logfilepath = gseaData.getGSEALogFilepath(analysisId);
                    outputFirstLogLine("Gene Set Enrichment Analysis thread running...", logfilepath);
                    appendLogLine("Generating required input files...", logfilepath);
                    if(gseaData.genGSEAInputFiles(analysisId, gseaParams, subTabInfo.subTabBase, logfilepath)) { 
                        // setup script arguments and run it
                        appendLogLine("Starting R script...", logfilepath);
                        List<String> lst = new ArrayList<>();
                        lst.add(app.data.getRscriptFilepath());
                        lst.add(runScriptPath.toString());
                        lst.add("-d" + gseaParams.dataType.name().toLowerCase());
                        String method = gseaParams.method.name();
                        lst.add("-m" + method);
                        lst.add("-a" + analysisId);
                        lst.add("-i" + gseaData.getGSEAFolder());
                        lst.add("-o" + gseaData.getGSEAFolder());
                        
                        //extra multivariant
                        lst.add("-v" + gseaParams.useMulti);
                        
                        lst.add("-g" + app.data.getAppGOTermsAncesFilepath());
                        app.logInfo("Running Gene Set Enrichment Analysis...\n    " + lst);
                        runScript(taskInfo, lst, "Gen Set Enrichment Analysis", logfilepath);
                        ObservableList<GSEASelectionResults> results = gseaData.getGSEASelectionResults(gseaParams.dataType.name(), analysisId, gseaParams.sigValue);
                        if(!results.isEmpty())
                            data = results;

                        // remove script file from temp folder
                        Utils.removeFile(runScriptPath);
                        runScriptPath = null;
                        app.logInfo("Gene Set Enrichment Analysis completed successfully.");
                    }
                    else {
                        appendLogLine("Unable to create required files for Gene Set Enrichment Analysis.", logfilepath);
                        app.logError("Unable to create required files for Gene Set Enrichment Analysis");
                    }
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("Gene Set Enrichment Analysis", task);
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
        String subTabs = TabProjectDataViz.Panels.SUMMARYGSEA.toString() + id;
        subTabs += TabProjectDataViz.Panels.ENRICHEDGSEA.toString() + id + ";";
        subTabs += TabProjectDataViz.Panels.CLUSTERSGSEA.toString() + id + ";";
        //subTabs += TabProjectDataViz.Panels.CLUSTERSGOGSEA.toString() + id;
        return subTabs;
    }
}
