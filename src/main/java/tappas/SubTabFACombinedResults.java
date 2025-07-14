/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.util.Callback;
import tappas.DataFDA.DataSelectionCombinedResults;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static tappas.DataFDA.DataSelectionResults;
import static tappas.DlgSelectRows.ColumnDefinition;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabFACombinedResults extends SubTabBase {
    TableView tblData;

    String analysisId1 = "";
    String analysisId2 = "";
    boolean hasData = false;
    DataFDA fdaData;
    HashMap<String, String> params = new HashMap<>();
    DlgFDAnalysisCombinedResults.Params fdaParamsCombinedResults;

    private final List<ColumnDefinition> lstColDefsID = Arrays.asList(
        //                   id,            colGroup, colTitle,     propName, prefWidth, alignment, selection, Params.CompType
        new DlgSelectRows.ColumnDefinition("ID",        "", "Feature ID",             "id", 250, "", "Feature ID", DlgSelectRows.Params.CompType.TEXT),
        new DlgSelectRows.ColumnDefinition("ID_DESCRIPTION",    "", "ID Description", "description", 200, "", "Gene description", DlgSelectRows.Params.CompType.TEXT),
        
        new DlgSelectRows.ColumnDefinition("PVALUE_GENOMIC",    "Genomic", "P-Value", "pValueGenomic", 90, "center", "P-Value", DlgSelectRows.Params.CompType.NUMDBL),
        new DlgSelectRows.ColumnDefinition("ADJPVALUE_GENOMIC",    "Genomic", "AdjP-Value", "adjPValueGenomic", 90, "center", "AdjP-Value", DlgSelectRows.Params.CompType.NUMDBL),
        new DlgSelectRows.ColumnDefinition("VARYING_GENOMIC",    "Genomic", "Varying", "varyingGenomic", 90, "center", "Varying", DlgSelectRows.Params.CompType.NUMDBL),
        new DlgSelectRows.ColumnDefinition("NOTVARYING_GENOMIC",    "Genomic", "Not Varying", "notVaryingGenomic", 110, "center", "Not Varying", DlgSelectRows.Params.CompType.NUMDBL),

        new DlgSelectRows.ColumnDefinition("PVALUE_PRESENCE",    "Presence", "P-Value", "pValuePresence", 90, "center", "P-Value", DlgSelectRows.Params.CompType.NUMDBL),
        new DlgSelectRows.ColumnDefinition("ADJPVALUE_PRESENCE",    "Presence", "AdjP-Value", "adjPValuePresence", 90, "center", "AdjP-Value", DlgSelectRows.Params.CompType.NUMDBL),
        new DlgSelectRows.ColumnDefinition("VARYING_PRESENCE",    "Presence", "Varying", "varyingPresence", 90, "center", "Varying", DlgSelectRows.Params.CompType.NUMDBL),
        new DlgSelectRows.ColumnDefinition("NOTVARYING_PRESENCE",    "Presence", "Not Varying", "notVaryingPresence", 110, "center", "Not Varying", DlgSelectRows.Params.CompType.NUMDBL)
    );

    ObservableList<MenuItem> lstTableMenus = null;

    public SubTabFACombinedResults(Project project) {
        super(project);
    }
    @Override
    
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = false;
        subTabInfo.btnSelect = true;
        subTabInfo.btnExport = true;
        subTabInfo.btnDataViz = true;
        subTabInfo.btnRun = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result){
            fdaData = new DataFDA(project);
            fdaParamsCombinedResults = DlgFDAnalysisCombinedResults.Params.load(fdaData.getFDAParamsCombinedResultsFilepath());
            hasData = fdaData.hasFDAData(fdaParamsCombinedResults.analysis_genomic) && fdaData.hasFDAData(fdaParamsCombinedResults.analysis_presence);
            if(hasData)
                return true;
            else
                app.ctls.alertInformation("FDA Combined Results", "Don't detect the two FDA ID Results");
        }else
            subTabInfo.subTabBase = null;
        return result;
    }

    @Override
    protected void onButtonRun() {
        app.ctlr.runFDACombinedResults("");
    }
    @Override
    protected void onButtonExport() {
        DlgExportData.Config cfg = new DlgExportData.Config(true, "Genes list (IDs only)", false, "");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            String filename = "tappAS_FDA_CombinedResults_"+fdaParamsCombinedResults.feature.trim()+ ".tsv";
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
                System.out.println("FDA Combined Results tblData search called: '" + txt + ", hide: " + hide);
                TableView<DataSelectionCombinedResults> tblView = (TableView<DataSelectionCombinedResults>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<DataSelectionCombinedResults> items = (ObservableList<DataSelectionCombinedResults>) info.data;
                if(items != null) {
                    ObservableList<DataSelectionCombinedResults> matchItems;
                    if(txt.isEmpty()) {
                        if(hide) {
                            matchItems = FXCollections.observableArrayList();
                            for(DataSelectionCombinedResults data : items) {
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
                        for(DataSelectionCombinedResults data : items) {
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
    public void viewLog() {
        String logdata = app.getLogFromFile(fdaData.getFDALogFilepath(analysisId), App.MAX_LOG_LINES);
        viewLog(logdata);
    }

    //
    // Internal Functions
    //
    private void showSubTab(ArrayList<DataSelectionCombinedResults> data_analyses) {
        tblData = (TableView) tabNode.lookup("#tblFA_Results");

        // set options menu
        ContextMenu cm = new ContextMenu();

//        MenuItem item_drill = new MenuItem("Drill down data");
//        cm.getItems().add(item_drill);
//        item_drill.setOnAction((event) -> {
//            try {
//                drillDownData(tblData);
//            } catch (IOException ex) {
//                Logger.getLogger(SubTabFDAResults.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        });
        
        cm = new ContextMenu();
        MenuItem item = new MenuItem("View Results Summary");
        item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, TabProjectDataViz.Panels.SUMMARYFDACOMBINEDRESULTS); });
        cm.getItems().add(item);
        subTabInfo.cmDataViz = cm;

        cm = new ContextMenu();
        app.export.setupTableExport(subTabInfo.subTabBase, cm, true);
        tblData.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblData);
        
        ArrayList<ColumnDefinition> lstTblCols = new ArrayList<>();
        for(ColumnDefinition cd : lstColDefsID) {
            lstTblCols.add(cd);
        }

        // configure table columns
        tblData.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        addTableColumns(tblData, lstTblCols, false, false);
        CheckBox chk = addRowNumsSelCols(tblData);
        chk.selectedProperty().addListener((ov, oldValue, newValue) -> { 
            changeAllRows(newValue? RowSelection.ActionType.SELECT : RowSelection.ActionType.DESELECT);
            tblData.requestFocus();
        });
        
        //ID
        // populate table columns
        ObservableList<DataSelectionCombinedResults> data = FXCollections.observableArrayList();
    
        // get table data
        if(!data_analyses.isEmpty()) {
            for(DataSelectionCombinedResults aux : data_analyses)
                data.add(aux);
        }

        // setup table search functionality
        updateDisplayData(data);
        setupTableSearch(tblData);
        
        // setup table menu to include selection column and custom annotation columns - make default focus node
        setupTableMenu(tblData, DataApp.DataType.GENE, true, true, lstTblCols, data, DataSelectionCombinedResults.class.getName());
        setFocusNode(tblData, true);
    }

    private void updateDisplayData(ObservableList<DataSelectionCombinedResults> data) {
        // set table data and select first row
        tblData.setItems(data);
        if(data.size() > 0)
            tblData.getSelectionModel().select(0);
        tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        ArrayList<DataSelectionCombinedResults> data = new ArrayList<DataSelectionCombinedResults>();
        //always have data
        if(hasData) {
            //export if ID
            HashMap<String, String[]> data_genomic = Utils.loadTSVListFromFile(fdaData.getFDAResultsFilepath(fdaParamsCombinedResults.analysis_genomic), true);
            HashMap<String, String[]> data_presence = Utils.loadTSVListFromFile(fdaData.getFDAResultsFilepath(fdaParamsCombinedResults.analysis_presence), true);
            String[] genomic = null;
            String[] presence = null;
            //check same size in both hashmaps
            if(data_genomic.size() == data_presence.size()){
                for(String featureID : data_genomic.keySet()){
                    genomic = data_genomic.get(featureID);
                    if(data_presence.containsKey(featureID))
                        presence = data_presence.get(featureID);
                    else
                        app.ctls.alertWarning("Funtional Diversity Analysis Combined Results", "FDA Combined Results failed - Presence analysis doesn't have the same key - task aborted");
                    //FeatureID\tDescription\tPValue\tAdjPValue\tVarying\tNotVarying\n
                    data.add(new DataSelectionCombinedResults(project, true, genomic[0], genomic[1], genomic[2], genomic[3], genomic[4], genomic[5], presence[2], presence[3], presence[4], presence[5]));
                }
            }else{
                //take the intersection
                for(String featureID : data_genomic.keySet()){
                    genomic = data_genomic.get(featureID);
                    if(data_presence.containsKey(featureID))
                        presence = data_presence.get(featureID);
                    else
                        continue;
                    //FeatureID\tDescription\tPValue\tAdjPValue\tVarying\tNotVarying\n
                    data.add(new DataSelectionCombinedResults(project, true, genomic[0], genomic[1], genomic[2], genomic[3], genomic[4], genomic[5], presence[2], presence[3], presence[4], presence[5]));
                }
            }
            
            //DataFDA.DiversityResults data1 = fdaData.getFDAResults(DlgFDAnalysis.Params.Method.ID.name(), fdaParamsCombinedResults.analysis1);
            //DataFDA.DiversityResults data2 = fdaData.getFDAResults(DlgFDAnalysis.Params.Method.ID.name(), fdaParamsCombinedResults.analysis2);
            showSubTab(data);
        }
        else {
            app.ctls.alertWarning("Funtional Diversity Analysis Combined Results", "FDA Combined Results failed - task aborted");
        }
    }

    //
    // Static Functions
    //

    // Just add all possible sub tabs - no need to bother with experiment type, etc.
    // List is used to close all subtabs that start with given panel names
    // Used to make sure we close all associated subtabs when running the analysis - data will be deleted
    public static String getAssociatedDVSubTabs(String id) {
        String subTabs = TabProjectDataViz.Panels.SUMMARYFDACOMBINEDRESULTS.toString() + id + ";";
        return subTabs;
    }
    
//    protected void addAnalysesColumns(TableView tbl, ArrayList<ColumnDefinition> lstTblCols){
//        String[] titles = { "Genomic Position", "Presence" };
//        String[] groups = { "groupA", "groupB"};
//        String[] split = { "GENOMIC", "PRESENCE" };
//        int sourceIdx = 0;
//        ArrayList<String> lstSources = new ArrayList<>();
//        ObservableList<TableColumn> cols = tbl.getColumns();
//        boolean flag = false;
//        int tidx = 0;
//        int tidxmax = groups.length;
//        for(String using : split) {
//            lstSources.clear();
//            for(ColumnDefinition cd : lstColDefsID) {
//                if(!cd.id.contains("_"))
//                    continue;
//                if(cd.id.split("_")[1].equals(using))
//                    lstSources.add(cd.colTitle);
//            }
//            int cnt = lstSources.size();
//            int subcolWidth = cnt > 1? 80 : 150;
//            TableColumn col = new TableColumn<>(titles[tidx]);
//            col.setPrefWidth(cnt * subcolWidth);
//            col.setEditable(false);
//            col.getStyleClass().add(groups[tidx++]);
//            
//            for(String key : lstSources){
//                String source = key; //COLTITLE
//                TableColumn subcol = new TableColumn<>(source);
//                subcol.setMinWidth(90);
//                subcol.setPrefWidth(subcolWidth);
//                subcol.setEditable(false);
//                subcol.setCellValueFactory(getDoubleGenomicDataCallback(sourceIdx));
//                subcol.setStyle("-fx-alignment:CENTER;");
//                col.getColumns().add(subcol);
//                ColumnDefinition cd = new ColumnDefinition("SOURCE" + sourceIdx, col.getText(), subcol.getText(), RowSelection.ARG_PREFIX + "Source\t" + sourceIdx,
//                                                            100, " CENTER", col.getText() + ": " + subcol.getText(), DlgSelectRows.Params.CompType.NUMINT);
//                cd.lstIdValuesSelections = DlgSelectRows.Params.lstIdValuesVarying;
//                cd.visible = false;
//                lstTblCols.add(cd);
//                sourceIdx++;
//            }
//            cols.add(col);
//                flag = true;
//        }
//        if(tidx >= tidxmax)
//            tidx = 0;
//        else if(!flag)
//            tidx++;
//    }

    private Callback getDoubleGenomicDataCallback(int idx) {
        return (new Callback<TableColumn.CellDataFeatures<DataSelectionCombinedResults, String>, ObservableValue<Double>>() {
            @Override
            public ObservableValue<Double> call(TableColumn.CellDataFeatures<DataSelectionCombinedResults, String> p) {
                return p.getValue().genomic_data[idx].asObject();
            }
        });
    }
    
    private Callback getDoublePresenceDataCallback(int idx) {
        return (new Callback<TableColumn.CellDataFeatures<DataSelectionCombinedResults, String>, ObservableValue<Double>>() {
            @Override
            public ObservableValue<Double> call(TableColumn.CellDataFeatures<DataSelectionCombinedResults, String> p) {
                return p.getValue().presence_data[idx].asObject();
            }
        });
    }
    
        private Callback getIntegerGenomicDataCallback(int idx) {
        return (new Callback<TableColumn.CellDataFeatures<DataSelectionCombinedResults, String>, ObservableValue<Integer>>() {
            @Override
            public ObservableValue<Integer> call(TableColumn.CellDataFeatures<DataSelectionCombinedResults, String> p) {
                return p.getValue().genomic_varying[idx].asObject();
            }
        });
    }
    
    private Callback getIntegerPresenceDataCallback(int idx) {
        return (new Callback<TableColumn.CellDataFeatures<DataSelectionCombinedResults, String>, ObservableValue<Integer>>() {
            @Override
            public ObservableValue<Integer> call(TableColumn.CellDataFeatures<DataSelectionCombinedResults, String> p) {
                return p.getValue().presence_varying[idx].asObject();
            }
        });
    }
    
    //DrillDown Data ISO
    private void drillDownData(TableView tbl) throws IOException {
        ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
        if(lstIdxs.size() != 1) {
            app.ctls.alertInformation("FDA Drill Down Data", "You have multiple rows highlighted.\nHighlight a single row with the feature you are interested in.");
        }
        else {
            DataSelectionResults rdata = (DataSelectionResults) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
            String id = rdata.getGene().trim();
            HashMap<String,ArrayList<String>> genes = getFDAGenes(id);
            app.ctlr.drillDownFDAData(id, genes);
        }
    }

    public HashMap<String, ArrayList<String>> getFDAGenes(String id) throws IOException {
        HashMap<String, ArrayList<String>> hmData = new HashMap<>();
        ArrayList<String> varying = new ArrayList<String>();
        ArrayList<String> notVarying = new ArrayList<String>();
        boolean find = false;
        boolean last = false;
        try {
            if(Files.exists(Paths.get(fdaData.getFDAResultsGeneDetailsFilepath(analysisId)))) {
                List<String> lines = Files.readAllLines(Paths.get(fdaData.getFDAResultsGeneDetailsFilepath(analysisId)));
                // get cluster info
                String[] fields;
                int length = 3; //gene and cluster
                int lnum = 1; // 1st line not count
                for(String line : lines) {
                    if(last==false && find ==true)
                        break;
                    if(!line.isEmpty() && lnum>1) {
                        fields = line.split("\t");
                        if(fields.length == length) {
                            if(fields[0].equals(id)){
                                find = true;
                                last = true;
                                if(fields[2].equals("V"))
                                    varying.add(fields[1]);
                                if(fields[2].equals("S"))
                                    notVarying.add(fields[1]);
                            }else{
                                last = false;
                            }
                        }
                        else {
                            logger.logError("Invalid number of columns in line " + lnum + ", " + fields.length + ", in DPA cluster file.");
                            break;
                        }
                    }
                    lnum++;
                }
                hmData.put("S", notVarying);
                hmData.put("V", varying);
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load DPA info file: " + e.getMessage());
        }
        return hmData;
    }

    //
    //Data Classes
    //
    public static class AnnotationTypeSource {
        String type;
        String source;
        public AnnotationTypeSource(String type, String source) {
            this.type = type;
            this.source = source;
        }
    }
}
