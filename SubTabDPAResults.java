/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.util.Callback;
import tappas.DataApp.DataType;
import tappas.DataDPA.DPASelectionResults;
import tappas.DlgSelectRows.ColumnDefinition;
import tappas.DlgSelectRows.Params.CompType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tappas.DataAnnotation.STRUCTURAL_SOURCE;

/**
 *
 * @author Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDPAResults extends SubTabBase {
    TableView tblData;

    public static final String DPA_INFO = "dpa_info.tsv";
    public static final String DPA_CAT = "dpa_cat.tsv";
    public String getDPAFolder() { return Paths.get(project.data.getProjectDataFolder(), DataProject.FOLDER_DPA).toString(); }
    public String getDPAInfoFilepath() { return Paths.get(getDPAFolder(), DPA_INFO).toString(); }
    public String getDPACatFilepath() { return Paths.get(getDPAFolder(), DPA_CAT).toString(); }
    
    boolean hasData = false;
    Path runScriptPath = null;
    String panel;
    DataDPA dpaData;
    DataType dpaType;
    String dpaTypeID;
    String dpaTypeName, dpaColTitle;
    TabProjectDataViz.Panels dpaPanel;
    TabProjectDataViz.Panels dpaClusterPanel;
    TabProjectDataViz.Panels dpaVennDiagPanel;
    DlgDPAnalysis.Params dpaParams;
    
    private final List<ColumnDefinition> lstColDefs = new LinkedList<>(Arrays.asList(
        //                   id,            colGroup, colTitle,             propName,           prefWidth, alignment, selection, Params.CompType
        new ColumnDefinition("GENE",            "",     "Gene",              "Gene",             125, "TOP-CENTER", "Gene name", CompType.TEXT),
        //new ColumnDefinition("DPA",             "",     "Differential PolyA","DifferentialPolyA",         100, "TOP-CENTER", "Differencial PolyA", CompType.LIST),
        new ColumnDefinition("RESULT",          "",     "DPA Result",        "DS", 100, "TOP-CENTER", "DPA Result", CompType.LIST),
        new ColumnDefinition("QVALUE",          "",     "Q-Value",           "QValue",           80, "TOP-RIGHT", "Q-Value", CompType.NUMDBL),
        new ColumnDefinition("PVALUE",          "",     "P-Value",           "PValue",           100, "TOP-RIGHT", "P-Value", CompType.NUMDBL),
        new ColumnDefinition("LONGER_CONDITION","",     "Distal Condition",  "LongerCondition",     130, "TOP-CENTER", "Longer condition", CompType.TEXT),
        new ColumnDefinition("CLUSTER",         "",     "Cluster",           "Cluster",           75, "TOP-CENTER", "Cluster expression profile", CompType.LISTIDVALS),
        
        //Expression levels Two-Group Comparison
        new ColumnDefinition("DISTAL_EXP_A", "",  "Distal 1 Expression",  "distalExpressionA",   140, "TOP-RIGHT", "distalExpressionA", CompType.NUMDBL),
        new ColumnDefinition("DISTAL_EXP_B",      "",  "Distal 2 Expression",  "distalExpressionB",   140, "TOP-RIGHT", "Distal 2 Expression", CompType.NUMDBL),
        new ColumnDefinition("PROXIMAL_EXP_A",    "",  "Proximal 1 Expression",  "proximalExpressionA",   140, "TOP-RIGHT", "Proximal 1 Expression", CompType.NUMDBL),
        new ColumnDefinition("PROXIMAL_EXP_B",    "",  "Proximal 2 Expression",  "proximalExpressionB",   140, "TOP-RIGHT", "Proximal 2 Expression", CompType.NUMDBL),
        
        //Mean expression levels
        new ColumnDefinition("DISTAL_EXPRESSION",    "",  "Distal Mean Expression",  "distalMeanExpression",   140, "TOP-RIGHT", "Distal Expression", CompType.NUMDBL),
        new ColumnDefinition("PROXIMAL_EXPRESSION",  "",  "Proximal Mean Expression","proximalMeanExpression", 160, "TOP-RIGHT", "Proximal Expression", CompType.NUMDBL),
        
        new ColumnDefinition("FAVORED",     "", "Favored Condition","Favored", 150, "TOP-CENTER", "Favored condition", CompType.LIST),
        new ColumnDefinition("PODIUMCHG",   "", "PolyA Switching",        "PodiumChange", 140, "TOP-CENTER", "Podium change", CompType.LIST),
        new ColumnDefinition("PODIUMTIMES",   "", "Switching Times",    "PodiumTimes", 130, "TOP-CENTER", "Podium times", CompType.TEXT),
        new ColumnDefinition("FAVOREDTIMES",   "", "Favored Times",    "FavoredTimes", 150, "TOP-CENTER", "Favored times", CompType.TEXT),
        new ColumnDefinition("CHGVALUE",    "", "ΔDPAU",         "TotalChange", 80, "TOP-CENTER", "Total change value", CompType.NUMDBL),
        
        new ColumnDefinition("GENEDESC",    "", "Gene Description", "GeneDescription", 200, "", "Gene description", CompType.TEXT),
        new ColumnDefinition("SOURCE",      "", "Source",           "Source", 100, "", "Annotation source", CompType.LIST),
        
        new ColumnDefinition("CHROMO",      "", "Chr",              "Chromo", 75, "TOP-CENTER", "Chromosome", CompType.LIST),
        new ColumnDefinition("STRAND",      "", "Strand",           "Strand", 75, "TOP-CENTER", "Strand", CompType.STRAND),
        new ColumnDefinition("ISOCNT",      "", "Isoforms",         "Isoforms", 75, "TOP-CENTER", "Number of gene isoforms", CompType.NUMINT),
        new ColumnDefinition("CODING",      "", "Coding",           "Coding", 75, "TOP-CENTER", "Protein coding gene", CompType.BOOL)
    ));
    ObservableList<MenuItem> lstTableMenus = null;
    
    public SubTabDPAResults(Project project) {
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
            dpaPanel = TabProjectDataViz.Panels.SUMMARYDPAGENE;
            dpaClusterPanel = TabProjectDataViz.Panels.CLUSTERSDPA;
            //dpaVennDiagPanel = TabProjectDataViz.Panels.VENNDIAGDPA;
            dpaData = new DataDPA(project);
            result = Files.exists(Paths.get(dpaData.getDPAParamsFilepath()));
            if(result) {
                panel = TabGeneDataViz.Panels.TRANS.name();
                dpaParams = DlgDPAnalysis.Params.load(dpaData.getDPAParamsFilepath(), project);
                hasData = dpaData.hasDPAData();
            }
            else
                app.ctls.alertInformation("DPA Gene Association", "Missing DPA parameter file");

            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    protected void onButtonSignificance() {
        String sigLevel = getSignificanceLevel(dpaParams.sigValue, "DPA Significance Level");
        if(!sigLevel.isEmpty()) {
            dpaParams.sigValue = Double.parseDouble(sigLevel);
            Utils.saveParams(dpaParams.getParams(), dpaData.getDPAParamsFilepath());
            ObservableList<DPASelectionResults> data = dpaData.getDPASelectionResults(dpaParams.sigValue, true);
            // remove summary file, results affected by significance level
            Utils.removeFile(Paths.get(dpaData.getDPAResultsSummaryFilepath()));
            updateDisplayData(data);
            
            // clear any pre-existing search values - worked but left table yellow
            tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
            searchSet(tblData, "", false);
            setTableSearchActive(tblData, false);

            // update summary display if opened
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("updateDPACombinedResults", "DPA");
            tabBase.processRequest(hm);
        }
    }    
    @Override
    protected void onButtonRun() {
        app.ctlr.runDPAnalysis();
    }    
    @Override
    protected void onButtonExport() {
        DlgExportData.Config cfg = new DlgExportData.Config(true, "Genes list (IDs only)", true, "Genes ranked list (IDs and values)");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            String filename = "tappAS_DPA_Genes.tsv";
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
                System.out.println("DPA tblData search called: '" + txt + ", hide: " + hide);
                TableView<DPASelectionResults> tblView = (TableView<DPASelectionResults>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<DPASelectionResults> items = (ObservableList<DPASelectionResults>) info.data;
                if(items != null) {
                    ObservableList<DPASelectionResults> matchItems;
                    if(txt.isEmpty()) {
                        if(hide) {
                            matchItems = FXCollections.observableArrayList();
                            for(DPASelectionResults data : items) {
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
                        for(DPASelectionResults data : items) {
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
        String logdata = app.getLogFromFile(dpaData.getDPALogFilepath(), App.MAX_LOG_LINES);
        viewLog(logdata);
    }
    
    //
    // Internal Functions
    //
    private void showSubTab(ObservableList<DPASelectionResults> data) {
        tblData = (TableView) tabNode.lookup("#tblDA_Stats");
        //String[] resultNames = project.data.getResultNames();
        String[] resultNames = project.data.getGroupNames();
        
        // set options menu
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("View Analysis Log");
        item.setOnAction((event) -> { viewLog(); });
        cm.getItems().add(item);
        subTabInfo.cmOptions = cm;

        // cluster options
        cm = new ContextMenu();
        if(project.data.isMultipleTimeSeriesExpType()) {
            /*Menu menu = new Menu("View Results Summary");
            for(String group : resultNames) {
                item = new MenuItem(group);
                item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, dpaPanel, group); });
                menu.getItems().add(item);
            }
            cm.getItems().add(menu);
            */
            //Same as case-control
            String group = resultNames[0];
            item = new MenuItem("View Results Summary");
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, dpaPanel, group); });
            cm.getItems().add(item);
            
        }
        else {
            // case-control will ignore the group - there is no time series data
            // single time series only has a single group
            String group = resultNames[0];
            item = new MenuItem("View Results Summary");
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, dpaPanel, group); });
            cm.getItems().add(item);
        }
        if(project.data.isTimeCourseExpType()) {
            if(project.data.isMultipleTimeSeriesExpType()) {
                Menu menu = new Menu("View Expression Profile Clusters");
                for(String group : resultNames) {
                    item = new MenuItem(group);
                    item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, dpaClusterPanel, group); });
                    menu.getItems().add(item);
                }
                cm.getItems().add(menu);
            }
            else {
                HashMap<String, Object> hmArgs = new HashMap<>();
                String group = resultNames[0];
                item = new MenuItem("View Expression Profile Clusters");
                item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, dpaClusterPanel, group); });
                cm.getItems().add(item);
            }
        }
/*        if(project.data.isMultipleTimeSeriesExpType()) {
            item = new MenuItem("View Significant Venn Diagram");
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, dpaVennDiagPanel); });
            cm.getItems().add(item);
        }*/
        subTabInfo.cmDataViz = cm;

        // set table popup context menu
        cm = new ContextMenu();
        item = new MenuItem("Show gene feature id data visualization");
        item.setOnAction((event) -> { showFeatureId(tblData); });
        cm.getItems().add(item);
        addGeneDVItem(cm, tblData, panel);
        
        // add drill down data
        MenuItem item_drill = new MenuItem("Drill down data");
        cm.getItems().add(item_drill);
        item_drill.setOnAction((event) -> { try {
            drillDownData(tblData);
            } catch (IOException ex) {
                Logger.getLogger(SubTabDPAResults.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        app.export.setupTableExport(subTabInfo.subTabBase, cm, true);
        tblData.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblData);
        
        // populate table columns list
        String[] names = project.data.getGroupNames();
        ArrayList<ColumnDefinition> lstTblCols = new ArrayList<>();
        for(ColumnDefinition cd : lstColDefs) {
            // enum DEXSeqValues { QValue, PodiumChange, TotalChange }
            // enum EdgeRValues { PValue, QValue, FavorC2, PodiumChange, TotalChange }
            // don't add columns that are not available for some analysis
        // hide position if n/a
            if((cd.id.equals("PVALUE") && (DlgDPAnalysis.Params.Method.DEXSEQ.equals(dpaParams.method) || DlgDPAnalysis.Params.Method.MASIGPRO.equals(dpaParams.method))) ||
               (cd.id.equals("CLUSTER") && !DlgDPAnalysis.Params.Method.MASIGPRO.equals(dpaParams.method)) ||
               (cd.id.equals("CHGVALUE") && DlgDPAnalysis.Params.Method.MASIGPRO.equals(dpaParams.method)) ||
               (cd.id.equals("PODIUMTIMES") && !project.data.isSingleTimeSeriesExpType()) ||
               (cd.id.equals("FAVOREDTIMES") && !project.data.isTimeCourseExpType()) ||
               (cd.id.equals("LONGER_CONDITION") && project.data.isMultipleTimeSeriesExpType()) ||
               //Expression levels
               (cd.id.equals("DISTAL_EXP_A") && project.data.isTimeCourseExpType()) ||
               (cd.id.equals("DISTAL_EXP_B") && project.data.isTimeCourseExpType()) ||
               (cd.id.equals("PROXIMAL_EXP_A") && project.data.isTimeCourseExpType()) ||
               (cd.id.equals("PROXIMAL_EXP_B") && project.data.isTimeCourseExpType()) ||
               //Mean expressions always
               //(cd.id.equals("DISTAL_EXPRESSION") && project.data.isTimeCourseExpType()) ||
               //(cd.id.equals("PROXIMAL_EXPRESSION") && project.data.isTimeCourseExpType()) ||
               //Don't show in Time course because we have FavoredTimes
               (cd.id.equals("FAVORED") && (names.length < 2 || project.data.isTimeCourseExpType() || project.data.isCaseControlExpType())))
                continue;
            
            //Changing column names
            if(cd.id.equals("FAVOREDTIMES") && project.data.isCaseControlExpType()) {
                cd.colTitle = "Favored Condition";
            }
            if(cd.id.equals("DISTAL_EXP_A") && project.data.isCaseControlExpType()) {
                cd.colTitle = "Distal " + project.data.getGroupNames()[0] + " Exp. Level";
                cd.prefWidth = cd.colTitle.length()*8;
            }
            if(cd.id.equals("DISTAL_EXP_B") && project.data.isCaseControlExpType()) {
                cd.colTitle = "Distal " + project.data.getGroupNames()[1] + " Exp. Level";
                cd.prefWidth = cd.colTitle.length()*8;
            }
            if(cd.id.equals("PROXIMAL_EXP_A") && project.data.isCaseControlExpType()) {
                cd.colTitle = "Proximal " + project.data.getGroupNames()[0] + " Exp. Level";
                cd.prefWidth = cd.colTitle.length()*8;
            }
            if(cd.id.equals("PROXIMAL_EXP_B") && project.data.isCaseControlExpType()) {
                cd.colTitle = "Proximal " + project.data.getGroupNames()[1] + " Exp. Level";
                cd.prefWidth = cd.colTitle.length()*8;
            }
            
            // hide some columns initially
            cd.visible = !(cd.id.equals("GENEDESC") || cd.id.equals("SOURCE") || cd.id.equals("CHROMO") || cd.id.equals("STRAND") || 
                            cd.id.equals("ISOCNT") || cd.id.equals("CODING") || cd.id.equals("DISTAL_EXPRESSION") || cd.id.equals("PROXIMAL_EXPRESSION"));
            
            // special case for multiple time series
            if(project.data.isMultipleTimeSeriesExpType()) {
                lstTblCols.add(cd);
                if(cd.id.equals("CLUSTER")) {
                    cd.colGroup = resultNames[0];
                    for(int i = 1; i < resultNames.length; i++) {
                        // create duplicate for other groups and add
                        ColumnDefinition cdm = new ColumnDefinition(cd.id + "Cmp" + i, cd.colGroup, cd.colTitle + " Cmp" + i, cd.propName +"Cmp" + i, 125, cd.alignment, cd.selection + " for Cmp" + i, cd.compType);
                        cdm.colGroup = resultNames[i];
                        lstTblCols.add(cdm);
                    }
                }
            }
            else
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
        //updateDisplayData(data);

        // handle special columns, once data is available
        for(ColumnDefinition cd : lstTblCols) {
            // handle special cases - only need to do this once
            switch (cd.id) {
                case "CHROMO":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DPASelectionResults dr : data)
                            hm.put(dr.getChromo(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String chr : hm.keySet())
                            lstSelections.add(chr);
                        //Collections.sort(lstSelections, new Chromosome.ChromoComparator());
                        cd.lstSelections = (List) lstSelections;
                        break;
                    }
                // Note: there could be hundreds or even thousands of feature IDs so not doing a list
                case "FAVORED":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DPASelectionResults dr : data)
                            hm.put(dr.getFavored(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String favored : hm.keySet())
                            lstSelections.add(favored);
                        //Collections.sort(lstSelections);
                        cd.lstSelections = (List) lstSelections;
                        break;
                    }
                case "PODIUMCHG":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DPASelectionResults dr : data)
                            hm.put(dr.getPodiumChange(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String val : hm.keySet())
                            lstSelections.add(val);
                        //Collections.sort(lstSelections);
                        cd.lstSelections = (List) lstSelections;
                            break;
                    }                
                case "SOURCE":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DPASelectionResults dr : data)
                            hm.put(STRUCTURAL_SOURCE, null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String db : hm.keySet())
                            lstSelections.add(db);
                        //Collections.sort(lstSelections);
                        cd.lstSelections = (List)lstSelections;
                        break;
                    }
                case "RESULT":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DPASelectionResults dr : data)
                            hm.put(dr.getDS(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String val : hm.keySet())
                            lstSelections.add(val);
                        //Collections.sort(lstSelections);
                        cd.lstSelections = (List) lstSelections;
                        break;
                    }
                //4 because is the maximum number of comparative groups
                case "CLUSTER":
                    {
                        if(!cd.colTitle.equals("Cluster")){
                            break;
                        }
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DataDPA.DPASelectionResults dr : data) {
                            hm.put(dr.getCluster(), null);
                        }
                        List<DataApp.EnumData> lstIdValues = new ArrayList<>();
                        boolean addBlank = false;
                        for(String val : hm.keySet()) {
                            if(val.isEmpty())
                                addBlank = true;
                            else
                                lstIdValues.add(new DataApp.EnumData(val, val.isEmpty()? "<blank> - not part of a cluster" : "Cluster " + val));
                        }
                        cd.lstIdValuesSelections = (List)lstIdValues;
                        // blank must be the last entry in the list or it will not work when combined with another selection
                        // this is due to the way checked boxes are handled in DlgSelectRows
                        if(addBlank)
                            lstIdValues.add(new DataApp.EnumData("", "<blank> - not part of a cluster"));
                    }
                case "CLUSTERCmp1":
                    {
                        if(!cd.colTitle.equals("Cluster Cmp1")){
                            break;
                        }
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DataDPA.DPASelectionResults dr : data) {
                            hm.put(dr.getClusterCmp1(), null);
                        }
                        List<DataApp.EnumData> lstIdValues = new ArrayList<>();
                        boolean addBlank = false;
                        for(String val : hm.keySet()) {
                            if(val.isEmpty())
                                addBlank = true;
                            else
                                lstIdValues.add(new DataApp.EnumData(val, val.isEmpty()? "<blank> - not part of a cluster" : "Cluster " + val));
                        }
                        cd.lstIdValuesSelections = (List)lstIdValues;
                        // blank must be the last entry in the list or it will not work when combined with another selection
                        // this is due to the way checked boxes are handled in DlgSelectRows
                        if(addBlank)
                            lstIdValues.add(new DataApp.EnumData("", "<blank> - not part of a cluster"));
                    }
                case "CLUSTERCmp2":
                    {
                        if(!cd.colTitle.equals("Cluster Cmp2")){
                            break;
                        }
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DataDPA.DPASelectionResults dr : data) {
                            hm.put(dr.getClusterCmp2(), null);
                        }
                        List<DataApp.EnumData> lstIdValues = new ArrayList<>();
                        boolean addBlank = false;
                        for(String val : hm.keySet()) {
                            if(val.isEmpty())
                                addBlank = true;
                            else
                                lstIdValues.add(new DataApp.EnumData(val, val.isEmpty()? "<blank> - not part of a cluster" : "Cluster " + val));
                        }
                        cd.lstIdValuesSelections = (List)lstIdValues;
                        // blank must be the last entry in the list or it will not work when combined with another selection
                        // this is due to the way checked boxes are handled in DlgSelectRows
                        if(addBlank)
                            lstIdValues.add(new DataApp.EnumData("", "<blank> - not part of a cluster"));
                    }
                case "CLUSTERCmp3":
                    {
                        if(!cd.colTitle.equals("Cluster Cmp3")){
                            break;
                        }
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DataDPA.DPASelectionResults dr : data) {
                            hm.put(dr.getClusterCmp3(), null);
                        }
                        List<DataApp.EnumData> lstIdValues = new ArrayList<>();
                        boolean addBlank = false;
                        for(String val : hm.keySet()) {
                            if(val.isEmpty())
                                addBlank = true;
                            else
                                lstIdValues.add(new DataApp.EnumData(val, val.isEmpty()? "<blank> - not part of a cluster" : "Cluster " + val));
                        }
                        cd.lstIdValuesSelections = (List)lstIdValues;
                        // blank must be the last entry in the list or it will not work when combined with another selection
                        // this is due to the way checked boxes are handled in DlgSelectRows
                        if(addBlank)
                            lstIdValues.add(new DataApp.EnumData("", "<blank> - not part of a cluster"));
                    } 
            }
        }

        //Extra columns for Time Series expression values
        if(project.data.isTimeCourseExpType()){
            //if(project.data.isSingleTimeSeriesExpType()){
                ArrayList<ColumnDefinition> lstTblColsExtra = new ArrayList<>();
                addTableColumnsDPA(tblData, lstTblColsExtra, data);
            //}
        }
        
        updateDisplayData(data);
        
        // setup table search functionality
        tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
        setupTableSearch(tblData);

        // setup table menu to include selection column and custom annotation columns - make default focus node
        setupTableMenu(tblData, DataType.GENE, true, true, lstTblCols, data, DPASelectionResults.class.getName());
        setFocusNode(tblData, true);
    }
    private void showFeatureId(TableView tbl) {
        // get highlighted row's data and show gene data visualization
        ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
        if(lstIdxs.size() != 1) {
            String msg = "You have multiple rows highlighted.\nHighlight a single row with the gene id you want to visualize.";
            app.ctls.alertInformation("Show Gene Data Drill Down", msg);
        }
        else {
            DPASelectionResults data = (DPASelectionResults) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
            app.ctlr.drillDownDPA(data);
        }
    }
    
    //DrillDown Data ISO
    private void drillDownData(TableView tbl) throws IOException {
        ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
        if(lstIdxs.size() != 1) {
            app.ctls.alertInformation("DPA Drill Down Data", "You have multiple rows highlighted.\nHighlight a single row with the feature you are interested in.");
        }
        else {
            DPASelectionResults rdata = (DPASelectionResults) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
            String gene = rdata.getGene().trim();
            HashMap<String,ArrayList<String>> isoforms = getDPAIsoforms(gene); 
            app.ctlr.drillDownDPAData(gene, isoforms);
        }
    }
    
    public HashMap<String, ArrayList<String>> getDPAIsoforms(String gene) throws IOException {
        HashMap<String, ArrayList<String>> hmData = new HashMap<>();
        ArrayList<String> shortIso = new ArrayList<String>();
        ArrayList<String> longIso = new ArrayList<String>();
        boolean find = false;
        boolean last = false;
        try {
            if(Files.exists(Paths.get(getDPACatFilepath()))) {
                    List<String> lines = Files.readAllLines(Paths.get(getDPACatFilepath()));
                    // get cluster info 
                    String[] fields;
                    int length = 4; //gene and cluster
                    int lnum = 1; // 1st line not count
                    for(String line : lines) {
                        if(last==false && find ==true)
                            break;
                        if(!line.isEmpty() && lnum>1) {
                            fields = line.split("\t");
                            if(fields.length == length) {
                                if(fields[0].equals(gene)){
                                    find = true;
                                    last = true;
                                    if(fields[3].equals("S"))
                                        shortIso.add(fields[1]);
                                    if(fields[3].equals("L"))
                                        longIso.add(fields[1]);
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
                hmData.put("L", longIso);
                hmData.put("S", shortIso);
            }
        }
        catch (Exception e) {
            logger.logError("Unable to load DPA info file: " + e.getMessage());
        }
        return hmData;
    }
    
    private void updateDisplayData(ObservableList<DPASelectionResults> data) {
        FXCollections.sort(data);
        tblData.setItems(data);
        if(data.size() > 0)
            tblData.getSelectionModel().select(0);
    }    
    
    //
    // Data Load
    //
    
    public static <S, T> Callback<TableColumn.CellDataFeatures<S, T>, ObservableValue<T>> createArrayValueFactory(Function<S, T[]> arrayExtractor, final int index) {
        if (index < 0) {
            return cd -> null;
        }
        return cd -> {
            T[] array = arrayExtractor.apply(cd.getValue());
            return array == null || array.length <= index ? null : new SimpleObjectProperty<>(array[index]);
        };
    }
    
    @Override
    protected void runDataLoadThread() {
        if(hasData) {
            ObservableList<DPASelectionResults> data = dpaData.getDPASelectionResults(dpaParams.sigValue, true);
            showSubTab(data);
        }
        else {
            String name = project.data.isTimeCourseExpType()? "DPA_TimeCourse.R" : "DPA.R";
            runScriptPath = app.data.getTmpScriptFileFromResource(name);
            service = new DataLoadService();
            service.initialize();
            service.start();
        }
    }
    private class DataLoadService extends TaskHandler.ServiceExt {
        ObservableList<DPASelectionResults> data = null;
        
        // do all service FX thread initialization
        @Override
        public void initialize() {
            showRunAnalysisScreen(true, "Differential PolyAdenylation Analysis in Progress...");
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
            String logfilepath = dpaData.getDPALogFilepath();
            java.lang.Throwable e = getException();
            if(e != null)
                appendLogLine("DPAnalysis failed - task aborted. Exception: " + e.getMessage(), logfilepath);
            else
                appendLogLine("DPAnalysis failed - task aborted.", logfilepath);
            app.ctls.alertWarning("Differential PolyAdenylation Analysis", "DPA failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                HashMap<String, Object> hm = new HashMap<>();
                hm.put("updateDEACombinedResults", "DPA");
                tabBase.processRequest(hm);
                showRunAnalysisScreen(false, "");
                pageDataReady = true;
                showSubTab(data);
            }
            else {
                String msg = "Differential PolyAdenylation Analysis " + (taskAborted? "Aborted by Request" : "Failed");
                app.ctls.alertWarning("Differential PolyAdenylation Analysis", msg);
                setRunAnalysisScreenTitle("Differential PolyAdenylation Analysis " + (taskAborted? "Aborted by Request" : "Failed"));
            }
        }
        //!!!DPA!!! Rscript para isoModel - funcion genDPA genera S and L
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String logfilepath = dpaData.getDPALogFilepath();
                    outputFirstLogLine("Differential PolyAdenylation Analysis thread running...", logfilepath);
                    appendLogLine("Generating required input files...", logfilepath);
                    if(dpaData.genDPAInputFiles(dpaParams)) { 
                        // setup script arguments and run it
                        appendLogLine("Starting R script...", logfilepath);
                        List<String> lst = new ArrayList<>();
                        lst.add(app.data.getRscriptFilepath());
                        lst.add(runScriptPath.toString());
                        //String d = "trans"; //dpaParams.dataType.name(); TODO !!!
                        //lst.add("-d" + d.toLowerCase());
                        String method = dpaParams.method.name();
                        lst.add("-m" + method);
                        lst.add("-i" + project.data.getProjectDataFolder());
                        lst.add("-o" + dpaData.getDPAFolder());
                        
                        String names = "";
                        for(int i = 0; i < project.data.getGroupsCount(); i++){
                            if(i==project.data.getGroupsCount()-1)
                                names = names + project.data.getGroupNames()[i];
                            else
                                names = names + project.data.getGroupNames()[i] + ";";
                        }
                        
                        lst.add("-n" + names);
                        String f = "0";
                        String t = "FOLD";
                        if(dpaParams.filter){
                            f = "" + dpaParams.filterFC;
                            t = "" + dpaParams.filteringType;
                        }
                        lst.add("-f" + f);
                        lst.add("-t" + t);
                        lst.add("-l" + dpaParams.lengthValue);
                        if(project.data.isTimeCourseExpType()) {
                            lst.add("-u" + dpaParams.degree);
                            lst.add("-k" + dpaParams.k);
                            lst.add("-c" + (dpaParams.mclust? "1" : "0"));
                            String cmp = "any";
                            String r = "0";
                            if(project.data.isMultipleTimeSeriesExpType()) {
                                r = dpaParams.strictType.name();
                                cmp = "group";
                            }
                            lst.add("-g" + cmp);
                            lst.add("-r" + r);
                        }
                        app.logInfo("Running Differential PolyAdenylation Analysis...");
                        app.logDebug("DPA arguments: " + lst);
                        runScript(taskInfo, lst, "Differential PolyAdenylation Analysis", logfilepath);
                        try {
                            data = dpaData.getDPASelectionResults(dpaParams.sigValue, true);
                        }
                        catch (Exception e) {
                            data = null;
                            appendLogLine("Unable to load DPA results.", logfilepath);
                            app.logError("Unable to load DPA results. Code exception: " + e.getMessage());
                        }

                        // remove script file from temp folder
                        Utils.removeFile(runScriptPath);
                        runScriptPath = null;
                        app.logInfo("Differential PolyAdenylation Analysis completed successfully.");
                    }
                    else {
                        appendLogLine("Unable to create required files for Differential PolyAdenylation Analysis.", logfilepath);
                        app.logError("Unable to create required files for Differential PolyAdenylation Analysis");
                    }
                    return null;
                    }
            };
            taskInfo = new TaskHandler.TaskInfo("Differential PolyAdenylation Analysis", task);
            return task;
        }
    }

    //
    // Static Functions
    //
    
    // Just add all possible sub tabs - no need to bother with experiment type, etc.
    // List is used to close all subtabs that start with given panel names
    // Used to make sure we close all associated subtabs when running the analysis - data will be deleted
    public static String getAssociatedDVSubTabs() {
        String subTabs = TabProjectDataViz.Panels.SUMMARYDPAGENE.toString();
        return subTabs;
    }
}
