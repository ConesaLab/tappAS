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
import tappas.DataDFI.DFISelectionResultsSummary;
import tappas.DlgSelectRows.ColumnDefinition;
import tappas.DlgSelectRows.Params.CompType;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDFIResultsSummary extends SubTabBase {
    TableView tblData;

    String analysisId = "";
    boolean hasData = false;
    String panel;
    DataDFI dfiData;
    //DlgDFIAnalysis.Params dfiParams = new DlgDFIAnalysis.Params();
    DlgDFIAnalysis.Params dfiParams;
    
    private final List<ColumnDefinition> lstColDefs = Arrays.asList(
        //                   id,            colGroup, colTitle,     propName, prefWidth, alignment, selection, Params.CompType
        new ColumnDefinition("SOURCE",      "", "Source",           "Source", 100, "", "Annotation source", CompType.LIST),
        new ColumnDefinition("FEATURE",     "", "Feature",          "Feature", 100, "", "Annotation feature", CompType.LIST),
        new ColumnDefinition("FEATUREID",   "", "Feature ID",       "FeatureId", 100, "", "Annotation feature ID", CompType.TEXT),
        new ColumnDefinition("FEATUREDESC",    "", "Feature Description", "FeatureDescription", 200, "", "Feature description", CompType.TEXT),
        new ColumnDefinition("DSFEATUREIDS",  "", "DFI Feature IDs", "DSFeatureIDs", 120, "TOP-RIGHT", "DFI feature IDs count", CompType.NUMINT),
        new ColumnDefinition("C1FAVCNT",    "", "C1 Favored",       "C1Favored", 100, "TOP-RIGHT", "C1 favored count", CompType.NUMINT),
        new ColumnDefinition("C2FAVCNT",    "", "C2 Favored",       "C2Favored", 100, "TOP-RIGHT", "C2 favored count", CompType.NUMINT),
        new ColumnDefinition("TESTEDFEATUREIDS", "", "Tested Feature IDs",     "TestedFeatureIDs", 120, "TOP-RIGHT", "Tested feature IDs count", CompType.NUMINT)
    );
    ObservableList<MenuItem> lstTableMenus = null;
    
    public SubTabDFIResultsSummary(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnSelect = true;
        subTabInfo.btnExport = true;
        subTabInfo.btnDataViz = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            // DFI data is required
            dfiData = new DataDFI(project);
            // check if id was not passed, allow passing as part of subTabId
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectData.Panels.FIRESULTSSUMMARY.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectData.Panels.FIRESULTSSUMMARY.name().length()).trim());
            }
            if(args.containsKey("id")) {
                analysisId = (String) args.get("id");

                // DFI parameter file is required
                result = Files.exists(Paths.get(dfiData.getDFIParamsFilepath(analysisId)));
                if(result) {
                    //params = dfiData.getDFIParams();
                    dfiParams = DlgDFIAnalysis.Params.load(dfiData.getDFIParamsFilepath(analysisId), project);

                    String shortName = getShortName(dfiParams.name);
                    subTabInfo.title = "DFI Results Summary: " + shortName;
                    subTabInfo.tooltip = "Differential Feature Inclusion Results Summary for '" + dfiParams.name.trim() + "'";
                    setTabLabel(subTabInfo.title, subTabInfo.tooltip);
                    
                    hasData = dfiData.hasDFISummaryData(analysisId);
                }
            else
                app.ctls.alertInformation("DFI Results Summary", "Missing DFI parameter file");

            }else{
                logger.logDebug("DFI id not found");
                result = false;
            }
            
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    protected void onButtonExport() {
        DlgExportData.Config cfg = new DlgExportData.Config(true, "Feature Id list (IDs only)", false, "");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            String filename = "tappAS_DFI_FeaturesSummary_" + dfiParams.name.trim() + "_.tsv";
            HashMap<String, String> hmColNames = new HashMap<>();
            boolean allRows = true;
            if(results.dataSelection.equals(DlgExportData.Params.DataSelection.SELECTEDROWS))
                allRows = false;
            if(results.dataType.equals(DlgExportData.Params.DataType.TABLEROWS.name())) {
                app.export.exportTableDataToFile(tblData, allRows, filename);
            }
            else if(results.dataType.equals(DlgExportData.Params.DataType.LIST.name())) {
                hmColNames.put("Feature Id", "");
                app.export.exportTableDataListToFile(tblData, allRows, hmColNames, filename);
            }
        }
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
                TableView<DFISelectionResultsSummary> tblView = (TableView<DFISelectionResultsSummary>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<DFISelectionResultsSummary> items = (ObservableList<DFISelectionResultsSummary>) info.data;
                if(items != null) {
                    ObservableList<DFISelectionResultsSummary> matchItems;
                    if(txt.isEmpty()) {
                        if(hide) {
                            matchItems = FXCollections.observableArrayList();
                            for(DFISelectionResultsSummary data : items) {
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
                        for(DFISelectionResultsSummary data : items) {
                            if(data.getGene().toLowerCase().contains(txt) || data.getGeneDescription().toLowerCase().contains(txt) ||  data.getFeatureDescription().toLowerCase().contains(txt) ||
                                    data.getSource().toLowerCase().contains(txt) || data.getFeature().toLowerCase().contains(txt) || data.getFeatureId().toLowerCase().contains(txt)) {
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
    private void showSubTab(ObservableList<DFISelectionResultsSummary> data) {
        tblData = (TableView) tabNode.lookup("#tblDA_Stats");

        // create data visualization context menu
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("View Results Summary");
        item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, TabProjectDataViz.Panels.SUMMARYDFI, analysisId); });
        cm.getItems().add(item);
        subTabInfo.cmDataViz = cm;

        // set table popup context menu and include gene data viz and export options
        /* DS - features that were found to be DS (a gene can have multiple features of the same kind, e.g. uORFs)
           Tested - number of features that were actually tested (must have multiple isoforms and varying feature)
        */


        cm = new ContextMenu();
        item = new MenuItem("Drill down data for DFI features");
        item.setOnAction((event) -> { drillDownData("FDS", tblData); });
        cm.getItems().add(item);
        app.export.setupTableExport(subTabInfo.subTabBase, cm, true);
        tblData.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblData);

        // populate table columns list
        String[] names = project.data.getGroupNames();
        ArrayList<ColumnDefinition> lstTblCols = new ArrayList<>();
        for(ColumnDefinition cd : lstColDefs) {
            switch (cd.id) {
                case "C1FAVCNT":
                    cd.colTitle = names[0] + " Favored";
                    cd.selection = names[0] + " favored count";
                    break;
                case "C2FAVCNT":
                    // waiting for favored decision using time course!!!
                    if(project.data.isTimeCourseExpType()) {
                        cd.colTitle = "N/A" + " Favored";
                        cd.selection = "N/A" + " favored count";
                    }
                    else {
                        cd.colTitle = names[1] + " Favored";
                        cd.selection = names[1] + " favored count";
                    }
                    break;
            }

            // hide some columns initially
            cd.visible = !cd.id.equals("SOURCE");
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

        // set table data and select first row
        tblData.setItems(data);
        if(data.size() > 0)
            tblData.getSelectionModel().select(0);

        // handle special columns, once data is available
        for(ColumnDefinition cd : lstTblCols) {
            switch (cd.id) {
                case "SOURCE":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DataDFI.DFISelectionResultsSummary dr : data)
                            hm.put(dr.getSource(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String source : hm.keySet())
                            lstSelections.add(source);
                        Collections.sort(lstSelections);
                        cd.lstSelections = (List)lstSelections;
                        break;
                    }
                // Note: there could be hundreds or even thousands of feature IDs so not doing a list
                case "FEATURE":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DataDFI.DFISelectionResultsSummary dr : data)
                            hm.put(dr.getFeature(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String feature : hm.keySet())
                            lstSelections.add(feature);
                        Collections.sort(lstSelections);
                        cd.lstSelections = (List)lstSelections;
                        break;
                    }
            }
        }

        // setup table search functionality
        tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
        setupTableSearch(tblData);

        // setup table menu to include selection column and custom annotation columns - make default focus node
        setupTableMenu(tblData, DataType.GENE, true, false, lstTblCols, data, DFISelectionResultsSummary.class.getName());
        setFocusNode(tblData, true);
    }
    private void drillDownData(String type, TableView tbl) {
        // get highlighted row's data and show gene data visualization
        ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
        if(lstIdxs.size() != 1) {
            String msg = "You have multiple rows highlighted.\nHighlight a single row with the gene/feature id you want to visualize.";
            app.ctls.alertInformation("Show Gene Feature Id Data Visualization", msg);
        }
        else {
            DFISelectionResultsSummary rdata = (DFISelectionResultsSummary) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
            app.ctlr.drillDownDFISummaryFeatureId(analysisId, type, rdata.getSource(), rdata.getFeature(), rdata.getFeatureId());
        }
    }
        
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        if(hasData) {
            ObservableList<DFISelectionResultsSummary> data = dfiData.getDFISelectionResultsSummary(dfiParams.sigValue, analysisId);
            showSubTab(data);
        }
        else {
            service = new DataService();
            service.start();
        }
    }
    private class DataService extends TaskHandler.ServiceExt {
        ObservableList<DFISelectionResultsSummary> data = null;
        
        @Override
        protected void onRunning() {
            showProgress();
        }
        @Override
        protected void onStopped() {
            hideProgress();
        }
        @Override
        protected void onFailed() {
            java.lang.Throwable e = getException();
            if(e != null)
                app.logWarning("DFI Analysis Summary failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("DFI Analysis Summary - task aborted.");
            app.ctls.alertWarning("Differential Feature Inclusion Analysis Summary", "DFI Analysis Summary failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                pageDataReady = true;
                showSubTab(data);
            }
            else {
                app.ctls.alertWarning("DFI Summary", "Unable to load DFI data.");
                populated = true;
            }
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    ObservableList<DFISelectionResultsSummary> results = dfiData.genDFISelectionResultsSummary(dfiParams.sigValue, analysisId);
                    if(!results.isEmpty())
                        data = results;
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("DFI Summary", task);
            return task;
        }
    }
}
