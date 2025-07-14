/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import tappas.Analysis.DataSelectionResults;
import tappas.DataApp.DataType;
import tappas.DlgSelectRows.ColumnDefinition;
import tappas.DlgSelectRows.Params.CompType;

import java.nio.file.Path;
import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabExpMatrix extends SubTabBase {
    TableView tblData;

    Path runScriptPath = null;
    DataType dataType;
    private final List<ColumnDefinition> lstColDefs = Arrays.asList(
        //                   id,            colGroup, colTitle,     propName, prefWidth, alignment, selection, Params.CompType
        new ColumnDefinition("ID",          "", "Id",               "Id",       125, "", "Id", CompType.TEXT),
        new ColumnDefinition("FILTERED",    "", "Filtered",         "Filtered", 100, "", "Filtered", CompType.LIST)
    );
    ObservableList<MenuItem> lstTableMenus = null;
    
    public SubTabExpMatrix(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnSelect = true;
        subTabInfo.btnExport = true;
        subTabInfo.btnDataViz = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        return result;
    }
    @Override
    protected void onButtonExport() {
        String colName = "Transcript";
        String lstName = "Transcripts list (IDs only)";
        String filename = "tappAS_Transcripts.tsv";
        DlgExportData.Config cfg = new DlgExportData.Config(true, lstName, false, "");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            HashMap<String, String> hmColNames = new HashMap<>();
            boolean allRows = true;
            if(results.dataSelection.equals(DlgExportData.Params.DataSelection.SELECTEDROWS))
                allRows = false;
            if(results.dataType.equals(DlgExportData.Params.DataType.TABLEROWS.name())) {
                app.export.exportTableDataToFile(tblData, allRows, filename);
            }
            else if(results.dataType.equals(DlgExportData.Params.DataType.LIST.name())) {
                hmColNames.put(colName, "");
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
                System.out.println("Project data search called: '" + txt + ", hide: " + hide);
                TableView<DataSelectionResults> tblView = (TableView<DataSelectionResults>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<DataSelectionResults> items = (ObservableList<DataSelectionResults>) info.data;
                if(items != null) {
                    ObservableList<DataSelectionResults> matchItems;
                    if(txt.isEmpty()) {
                        if(hide) {
                            matchItems = FXCollections.observableArrayList();
                            for(DataSelectionResults data : items) {
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
                        for(DataSelectionResults data : items) {
                            if(data.getId().toLowerCase().contains(txt)) {
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
    private void showSubTab(ObservableList<DataSelectionResults> data) {
        // create data visualization context menu
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("View Summary");
        item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, TabProjectDataViz.Panels.SUMMARYEXPMATRIX); });
        cm.getItems().add(item);
        subTabInfo.cmDataViz = cm;

        // populate table columns list
        ArrayList<ColumnDefinition> lstTblCols = new ArrayList<>();
        for(ColumnDefinition cd : lstColDefs) {
            lstTblCols.add(cd);
        }

        // configure table columns
        tblData = (TableView) tabNode.lookup("#tblData");
        tblData.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        addTableColumns(tblData, lstTblCols, false, true);
        CheckBox chk = addRowNumsSelCols(tblData);
        chk.selectedProperty().addListener((obs, oldData, newData) -> { 
            changeAllRows(newData? RowSelection.ActionType.SELECT : RowSelection.ActionType.DESELECT);
            tblData.requestFocus();
        });

        // set table popup context menu and include export option
        cm = new ContextMenu();
        app.export.setupTableExport(subTabInfo.subTabBase, cm, false);
        tblData.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblData);

        // set table data and select first row
        tblData.setItems(data);
        if(data.size() > 0)
            tblData.getSelectionModel().select(0);

        // handle special columns, once data is available
        for(ColumnDefinition cd : lstTblCols) {
            // handle special cases - only need to do this once
            if(cd.id.equals("FILTERED")) {
                HashMap<String, Object> hm = new HashMap<>();
                for(DataSelectionResults dr : data)
                    hm.put(dr.getFiltered(), null);
                ArrayList<String> lstSelections = new ArrayList<>();
                for(String filter : hm.keySet())
                    lstSelections.add(filter);
                Collections.sort(lstSelections);
                cd.lstSelections = (List) lstSelections;
            }
        }

        // setup table search functionality
        tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
        setupTableSearch(tblData);

        // setup table menu to include selection column and custom annotation columns - make default focus node
        setupTableMenu(tblData, dataType, true, false, lstTblCols, data, DataSelectionResults.class.getName());
        setFocusNode(tblData, true);
    }
    public void viewLog() {
        String logdata = app.getLogFromFile(project.data.analysis.getDEALogFilepath(dataType), App.MAX_LOG_LINES);
        viewLog(logdata);
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        ObservableList<DataSelectionResults> data = project.data.getOriginalExpMatrixSelectionResults();
        FXCollections.sort(data);
        HashMap<String, Object> hmProject = project.data.getResultsTrans();
        HashMap<String, Object> hmNA = Utils.loadListFromFile(project.data.getInputMatrixNATransFilepath());
        HashMap<String, Object> hmFiltered = Utils.loadListFromFile(project.data.getInputMatrixFilteredTransFilepath());
        for(DataSelectionResults dsr : data) {
            if(hmNA.containsKey(dsr.getId()))
                dsr.setFiltered("YES - No Annotation");
            else if(hmFiltered.containsKey(dsr.getId()))
                dsr.setFiltered("YES - Transcripts List");
            else if(!hmProject.containsKey(dsr.getId()))
                dsr.setFiltered("YES - LowCount/COV");
        }
        showSubTab(data);
    }
}
