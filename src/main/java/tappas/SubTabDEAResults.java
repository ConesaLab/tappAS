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
public class SubTabDEAResults extends SubTabBase {
    TableView tblData;
    boolean hasData = false;
    Path runScriptPath = null;
    DataDEA deaData;
    DataType deaType;
    String deaTypeID;
    String deaTypeName, deaColTitle;
    TabProjectDataViz.Panels deaPanel;
    TabProjectDataViz.Panels deaClusterPanel;
    TabProjectDataViz.Panels deaVennDiagPanel;
    String rankedColName, panel;
    DlgDEAnalysis.Params deaParams;
    private final List<ColumnDefinition> lstColDefs = Arrays.asList(
        //                   id,            colGroup, colTitle,     propName, prefWidth, alignment, selection, Params.CompType
        new ColumnDefinition("ID",          "", "Gene",             "Id", 125, "TOP-CENTER", "Gene name", CompType.TEXT),
        new ColumnDefinition("NAME",        "", "Name/Description", "Name", 160, "", "Name/Description", CompType.TEXT),
        new ColumnDefinition("LENGTH",      "", "Length",           "Length", 75, "TOP-RIGHT", "Transcript length in nucleotides", CompType.LIST),
        new ColumnDefinition("TRANS",       "", "Transcript",       "Transcript", 125, "", "Transcript", CompType.TEXT),
        new ColumnDefinition("GENE",        "", "Gene",             "Gene", 125, "", "Gene name", CompType.TEXT),
        new ColumnDefinition("GENEDESC",    "", "Gene Description", "GeneDescription", 200, "", "Gene description", CompType.TEXT),
        new ColumnDefinition("RESULT",      "", "DEA Result",       "DE", 100, "TOP-CENTER", "DEA Result", CompType.LIST),
        new ColumnDefinition("CLUSTER",     "", "Cluster",          "ClusterString", 75, "TOP-CENTER", "Cluster expression profile", CompType.LISTIDVALS),
        new ColumnDefinition("REGULATED",   "", "Regulated",        "Regulated", 95, "TOP-CENTER", "DE Regulation", CompType.LISTIDVALS),
        new ColumnDefinition("STCAT",       "", "Structural Category",   "Category", 125, "", "Structural category", CompType.LIST),
        //new ColumnDefinition("STATTR",      "", "Structural Attributes", "Attributes", 200, "", "Structural attributes", CompType.TEXT),
        // will change based on analysis method
        new ColumnDefinition("VALUE1",      "", "Value1",           "Value1", 100, "TOP-RIGHT", "", CompType.NUMDBL),
        new ColumnDefinition("VALUE2",      "", "Value2",           "Value2", 100, "TOP-RIGHT", "", CompType.NUMDBL),
        new ColumnDefinition("VALUE3",      "", "Value3",           "Value3", 100, "TOP-RIGHT", "", CompType.NUMDBL),
        new ColumnDefinition("VALUE4",      "", "Value4",           "Value4", 100, "TOP-RIGHT", "", CompType.NUMDBL),
        // end
        new ColumnDefinition("CHROMO",      "", "Chr",              "Chromo", 75, "TOP-CENTER", "Chromosome", CompType.LIST),
        new ColumnDefinition("STRAND",      "", "Strand",           "Strand", 75, "TOP-CENTER", "Strand", CompType.STRAND),
        new ColumnDefinition("ISOCNT",      "", "Isoforms",         "Isoforms", 75, "TOP-RIGHT", "Number of gene isoforms", CompType.NUMINT),
        new ColumnDefinition("PROTCNT",      "", "Proteins",         "Proteins", 75, "TOP-RIGHT", "Number of gene proteins", CompType.NUMINT),
        new ColumnDefinition("CODING",      "", "Coding",           "Coding", 75, "TOP-CENTER", "Protein coding gene", CompType.BOOL)
    );
    ObservableList<MenuItem> lstTableMenus = null;
    
    public SubTabDEAResults(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = true;
        subTabInfo.btnSelect = true;
        subTabInfo.btnExport = true;
        subTabInfo.btnDataViz = true;
        if(!project.data.isTimeCourseExpType())
            subTabInfo.btnSignificance = true;
        subTabInfo.btnRun = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            deaData = new DataDEA(project);
            deaType = (DataType) args.get("dataType");
            deaTypeID = deaType.name();
            hasData = deaData.hasDEAData(deaType);
            switch(deaType) {
                case PROTEIN:
                    deaTypeName = "Proteins";
                    deaColTitle = "Protein";
                    deaPanel = TabProjectDataViz.Panels.SUMMARYDEAPROT;
                    deaClusterPanel = TabProjectDataViz.Panels.CLUSTERSDEAPROT;
                    deaVennDiagPanel = TabProjectDataViz.Panels.VENNDIAGDEAPROT;
                    panel = TabGeneDataViz.Panels.PROTEIN.name();
                    break;
                case GENE:
                    deaTypeName = "Genes";
                    deaColTitle = "Gene";
                    deaPanel = TabProjectDataViz.Panels.SUMMARYDEAGENE;
                    deaClusterPanel = TabProjectDataViz.Panels.CLUSTERSDEAGENE;
                    deaVennDiagPanel = TabProjectDataViz.Panels.VENNDIAGDEAGENE;
                    panel = TabGeneDataViz.Panels.TRANS.name();
                    break;
                case TRANS:
                    deaTypeName = "Transcripts";
                    deaColTitle = "Transcript";
                    deaPanel = TabProjectDataViz.Panels.SUMMARYDEATRANS;
                    deaClusterPanel = TabProjectDataViz.Panels.CLUSTERSDEATRANS;
                    deaVennDiagPanel = TabProjectDataViz.Panels.VENNDIAGDEATRANS;
                    panel = TabGeneDataViz.Panels.TRANS.name();
                    break;
                default:
                    result = false;
                    break;
            }
            
            if(result) {
                // parameter data file is required
                result = Files.exists(Paths.get(deaData.getDEAParamsFilepath(deaType)));
                if(result) {
                    deaParams = DlgDEAnalysis.Params.load(deaData.getDEAParamsFilepath(deaType), project);
                    if(DlgDEAnalysis.Params.Method.NOISEQ.equals(deaParams.method))
                        rankedColName = "(1 - Probability)";
                    else if(DlgDEAnalysis.Params.Method.EDGER.equals(deaParams.method))
                        rankedColName = "FDR";
                    else if(DlgDEAnalysis.Params.Method.MASIGPRO.equals(deaParams.method))
                        rankedColName = "P-Value";
                }
                else
                    app.ctls.alertInformation("DEA " + deaTypeName + " Results", "Missing DEA parameter file");
            }
            
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    protected void onButtonSignificance() {
        if(deaParams.method.equals(deaParams.method.NOISEQ)){
            String[] val = getSignificanceLevelByFC(deaParams.sigValue, deaParams.FCValue, deaTypeName + " DEA Significance Level");
            if(!val[0].equals("") && !val[1].equals("")) {
                deaParams.sigValue = Double.parseDouble(val[0]);
                deaParams.FCValue = Double.parseDouble(val[1]);
                Utils.saveParams(deaParams.getParams(), deaData.getDEAParamsFilepath(deaType));
                ObservableList<DataDEA.DEASelectionResults> data = deaData.getDEASelectionResults(deaType, "", deaParams.sigValue, deaParams.FCValue);
                FXCollections.sort(data);
                tblData.setItems(data);
                if(data.size() > 0)
                    tblData.getSelectionModel().select(0);

                // clear any pre-existing search values - worked but left table yellow
                tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
                searchSet(tblData, "", false);
                setTableSearchActive(tblData, false);

                // update summary display if opened
                HashMap<String, Object> hm = new HashMap<>();
                hm.put("updateDEACombinedResults", "DEA" + deaTypeID);
                tabBase.processRequest(hm);
            }
        }else{
            String val = getSignificanceLevel(deaParams.sigValue, deaTypeName + " DEA Significance Level");
            if(!val.isEmpty()) {
                deaParams.sigValue = Double.parseDouble(val);
                Utils.saveParams(deaParams.getParams(), deaData.getDEAParamsFilepath(deaType));
                ObservableList<DataDEA.DEASelectionResults> data = deaData.getDEASelectionResults(deaType, "", deaParams.sigValue, deaParams.FCValue);
                FXCollections.sort(data);
                tblData.setItems(data);
                if(data.size() > 0)
                    tblData.getSelectionModel().select(0);

                // clear any pre-existing search values - worked but left table yellow
                tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
                searchSet(tblData, "", false);
                setTableSearchActive(tblData, false);

                // update summary display if opened
                HashMap<String, Object> hm = new HashMap<>();
                hm.put("updateDEACombinedResults", "DEA" + deaTypeID);
                tabBase.processRequest(hm);
            }
        }
    }    
    @Override
    protected void onButtonRun() {
        app.ctlr.runDEAnalysis(deaType);
    }    
    @Override
    protected void onButtonExport() {
        DlgExportData.Config cfg;
        //if(project.data.isCaseControlExpType())
            cfg = new DlgExportData.Config(true, deaTypeName + " list (IDs only)", true, deaTypeName + " ranked list (IDs and values)");
        //else
        //    cfg = new DlgExportData.Config(true, deaTypeName + " list (IDs only)", false, "");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            String filename = "tappAS_DEA_" + deaTypeName + ".tsv";
            HashMap<String, String> hmColNames = new HashMap<>();
            boolean allRows = true;
            if(results.dataSelection.equals(DlgExportData.Params.DataSelection.SELECTEDROWS))
                allRows = false;
            if(results.dataType.equals(DlgExportData.Params.DataType.TABLEROWS.name())) {
                app.export.exportTableDataToFile(tblData, allRows, filename);
            }
            else if(results.dataType.equals(DlgExportData.Params.DataType.LIST.name())) {
                hmColNames.put(deaColTitle, "");
                app.export.exportTableDataListToFile(tblData, allRows, hmColNames, filename);
            }
            else if(results.dataType.equals(DlgExportData.Params.DataType.RANKEDLIST.name())) {
                hmColNames.put(deaColTitle, "");
                hmColNames.put(rankedColName, "");
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
                System.out.println("DEA tblData search called: '" + txt + ", hide: " + hide);
                TableView<DataDEA.DEASelectionResults> tblView = (TableView<DataDEA.DEASelectionResults>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<DataDEA.DEASelectionResults> items = (ObservableList<DataDEA.DEASelectionResults>) info.data;
                if(items != null) {
                    ObservableList<DataDEA.DEASelectionResults> matchItems;
                    if(txt.isEmpty()) {
                        if(hide) {
                            matchItems = FXCollections.observableArrayList();
                            for(DataDEA.DEASelectionResults data : items) {
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
                        for(DataDEA.DEASelectionResults data : items) {
                            if(data.getId().toLowerCase().contains(txt) || data.getName().toLowerCase().contains(txt) || 
                                    data.getTranscript().toLowerCase().contains(txt) ||
                                    data.getGene().toLowerCase().contains(txt) || data.getGeneDescription().toLowerCase().contains(txt)) {
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
    private void showSubTab(ObservableList<DataDEA.DEASelectionResults> data) {
        String[] resultNames = project.data.getResultNames();

        // set options menu
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("View Analysis Log");
        item.setOnAction((event) -> { viewLog(); });
        cm.getItems().add(item);
        subTabInfo.cmOptions = cm;

        // set data visualization menu
        cm = new ContextMenu();
        if(project.data.isMultipleTimeSeriesExpType()) {
            Menu menu = new Menu("View Results Summary");
            for(String group : resultNames) {
                item = new MenuItem(group);
                item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, deaPanel, group); });
                menu.getItems().add(item);
            }
            cm.getItems().add(menu);
        }
        else {
            // case-control will ignore the group - there is no time series data
            // single time series only has a single group
            String group = resultNames[0];
            item = new MenuItem("View Results Summary");
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, deaPanel, group); });
            cm.getItems().add(item);
        }
        if(project.data.isTimeCourseExpType()) {
            if(project.data.isMultipleTimeSeriesExpType()) {
                Menu menu = new Menu("View Expression Profile Clusters");
                for(String group : resultNames) {
                    item = new MenuItem(group);
                    item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, deaClusterPanel, group); });
                    menu.getItems().add(item);
                }
                cm.getItems().add(menu);
            }
            else {
                HashMap<String, Object> hmArgs = new HashMap<>();
                String group = resultNames[0];
                item = new MenuItem("View Expression Profile Clusters");
                item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, deaClusterPanel, group); });
                cm.getItems().add(item);
            }
        }
/*        if(project.data.isMultipleTimeSeriesExpType()) {
            item = new MenuItem("View Significant " + deaTypeName + " Venn Diagram");
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, deaVennDiagPanel); });
            cm.getItems().add(item);
        }*/
        subTabInfo.cmDataViz = cm;

        // populate table columns list
        ArrayList<ColumnDefinition> lstTblCols = new ArrayList<>();
        for(ColumnDefinition cd : lstColDefs) {
            // enum NOISeqValues { X1mean, X2mean, probability, log2FC }
            // enum EdgeRValues { log2FC, logCPM, PValue, FDR }
            // don't add columns that are not available for some analysis or data types
            // we don't have a name/desc for trans right now so don't show - will need to change to check if provided!!!
            if((cd.id.equals("VALUE1") && DlgDEAnalysis.Params.Method.NOISEQ.equals(deaParams.method)) ||
                    (cd.id.equals("CLUSTER") && !DlgDEAnalysis.Params.Method.MASIGPRO.equals(deaParams.method)) ||
                    (cd.id.equals("REGULATED") && DlgDEAnalysis.Params.Method.MASIGPRO.equals(deaParams.method)) ||
                    (cd.id.equals("VALUE3") && DlgDEAnalysis.Params.Method.MASIGPRO.equals(deaParams.method)) ||
                    (cd.id.equals("VALUE4") && DlgDEAnalysis.Params.Method.MASIGPRO.equals(deaParams.method)) ||
                    ((cd.id.equals("ISOCNT") && (deaType.equals(DataType.TRANS) || deaType.equals(DataType.PROTEIN)))) ||
                    (cd.id.equals("CODING") && deaType.equals(DataType.PROTEIN)) ||
                    (cd.id.equals("TRANS") && !deaType.equals(DataType.PROTEIN)) ||
                    ((cd.id.equals("STCAT") || cd.id.equals("STATTR")) && !deaType.equals(DataType.TRANS)) ||
                    ((cd.id.equals("CHROMO") || cd.id.equals("STRAND")) && deaType.equals(DataType.PROTEIN)) ||
                    ((cd.id.equals("GENE") || cd.id.equals("GENEDESC") || cd.id.equals("LENGTH")) && deaType.equals(DataType.GENE))
                 )
                continue;

            // hide some columns initially - can have for some data types
            cd.visible = !(cd.id.equals("CHROMO") || cd.id.equals("STRAND") || cd.id.equals("ISOCNT") || cd.id.equals("PROTCNT")|| cd.id.equals("CODING") ||
                            cd.id.equals("STCAT") || cd.id.equals("STATTR") || cd.id.equals("LENGTH"));

            // dynamic column titles
            if(cd.id.equals("ID")) {
                cd.colTitle = deaColTitle;
                cd.selection = deaColTitle;
            }
            if(DlgDEAnalysis.Params.Method.NOISEQ.equals(deaParams.method)) {
                switch (cd.id) {
                    case "VALUE3":
                        cd.colTitle = "Probability";
                        cd.selection = "Probability";
                        break;
                    case "VALUE2":
                        cd.colTitle = "(1 - Probability)";
                        cd.selection = "(1 - Probability)";
                        break;
                    case "VALUE4":
                        if(deaParams.replicates.toString().equals("BIOLOGICAL")){
                            System.out.println(deaParams.replicates);
                            cd.colTitle = "Log2FC";
                        }else{
                            cd.colTitle = "Ranking";
                        }
                        cd.selection = "Log2 Fold Change";
                        cd.compType = DlgSelectRows.Params.CompType.NUMDBLPN;
                        break;
                }
            }
            else if(DlgDEAnalysis.Params.Method.EDGER.equals(deaParams.method)) {
                switch (cd.id) {
                    case "VALUE1":
                        cd.colTitle = "Log2FC";
                        cd.selection = "Log2 Fold Change";
                        cd.compType = DlgSelectRows.Params.CompType.NUMDBLPN;
                        break;
                    case "VALUE2":
                        cd.colTitle = "logCPM";
                        cd.selection = "LogCPM";
                        cd.compType = DlgSelectRows.Params.CompType.NUMDBLPN;
                        break;
                    case "VALUE3":
                        cd.colTitle = "P-Value";
                        cd.selection = "P-Value";
                        break;
                    case "VALUE4":
                        cd.colTitle = "FDR";
                        cd.selection = "FDR";
                        break;
                }
            }

            else if(DlgDEAnalysis.Params.Method.MASIGPRO.equals(deaParams.method)) {
                switch (cd.id) {
                    case "VALUE1":
                        cd.colTitle = "P-Value";
                        cd.selection = "P-Value";
                        cd.compType = DlgSelectRows.Params.CompType.NUMDBLPN;
                        break;
                    case "VALUE2":
                        cd.colTitle = "R.squared";
                        cd.selection = "R.squared";
                        cd.compType = DlgSelectRows.Params.CompType.NUMDBLPN;
                        break;
                }
            }

            switch (cd.id) {
                case "TRANS":
                    if(deaType.equals(DataType.PROTEIN))
                        cd.colTitle = "Transcript(s)";
                    else
                        cd.colTitle = "Transcript";
                    break;
                case "GENE":
                    if(deaType.equals(DataType.PROTEIN))
                        cd.colTitle = "Gene(s)";
                    else
                        cd.colTitle = "Gene";
                    break;
                case "GENEDESC":
                    if(deaType.equals(DataType.PROTEIN))
                        cd.colTitle = "Gene Description(s)";
                    else
                        cd.colTitle = "Gene Description";
                    break;
            }
            
            // special case for multiple time series
            if(project.data.isMultipleTimeSeriesExpType()) {
                lstTblCols.add(cd);
                if(cd.id.equals("RESULT") || cd.id.equals("CLUSTER")) {
                    cd.colGroup = resultNames[0];
                    for(int i = 1; i < resultNames.length; i++) {
                        // create duplicate for other groups and add
                        ColumnDefinition cdm = new ColumnDefinition(cd.id + "Cmp" + i, cd.colGroup, cd.colTitle + " Cmp" + i, cd.propName +"Cmp" + i, cd.prefWidth, cd.alignment, cd.selection + " for Cmp" + i, cd.compType);
                        cdm.colGroup = resultNames[i];
                        lstTblCols.add(cdm);
                    }
                }
            }
            else
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
            if(cd.id.equals("CHROMO")) {
                HashMap<String, Object> hm = new HashMap<>();
                for(DataDEA.DEASelectionResults dr : data)
                    hm.put(dr.getChromo(), null);
                ArrayList<String> lstSelections = new ArrayList<>();
                for(String chr : hm.keySet())
                    lstSelections.add(chr);
                Collections.sort(lstSelections, new Chromosome.ChromoComparator());
                cd.lstSelections = lstSelections;
            }
            else if(cd.id.equals("STCAT")) {
                HashMap<String, Object> hm = new HashMap<>();
                for(DataDEA.DEASelectionResults dr : data)
                    hm.put(dr.getCategory(), null);
                ArrayList<String> lstSelections = new ArrayList<>();
                for(String cat : hm.keySet())
                    lstSelections.add(cat);
                Collections.sort(lstSelections);
                cd.lstSelections = (List)lstSelections;
            }
            else if(cd.id.startsWith("RESULT")) {
                HashMap<String, Object> hm = new HashMap<>();
                for(DataDEA.DEASelectionResults dr : data) {
                    switch(cd.id) {
                        case "RESULT":
                            hm.put(dr.getDE(), null);
                            break;
                        case "RESULTCmp1":
                            hm.put(dr.getDECmp1(), null);
                            break;
                        case "RESULTCmp2":
                            hm.put(dr.getDECmp2(), null);
                            break;
                        case "RESULTCmp3":
                            hm.put(dr.getDECmp3(), null);
                            break;
                    }
                }
                ArrayList<String> lstSelections = new ArrayList<>();
                // no blank entries for DE
                for(String val : hm.keySet())
                    lstSelections.add(val);
                Collections.sort(lstSelections);
                cd.lstSelections = (List)lstSelections;
            }
            else if(cd.id.startsWith("CLUSTER")) {
                HashMap<String, Object> hm = new HashMap<>();
                for(DataDEA.DEASelectionResults dr : data) {
                    switch(cd.id) {
                        case "CLUSTER":
                            hm.put(dr.getClusterString(), null);
                            break;
                        case "CLUSTERCmp1":
                            hm.put(dr.getClusterStringCmp1(), null);
                            break;
                        case "CLUSTERCmp2":
                            hm.put(dr.getClusterStringCmp2(), null);
                            break;
                        case "CLUSTERCmp3":
                            hm.put(dr.getClusterStringCmp3(), null);
                            break;
                    }
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
            else if(cd.id.equals("REGULATED"))
                cd.lstIdValuesSelections = (List) DlgSelectRows.Params.lstIdValuesDERegulated;
            if(cd.id.equals("REGULATED") && deaParams.method.equals(deaParams.method.NOISEQ))
                cd.compType = CompType.TEXT;
        }

        // setup table search functionality
        tblData.setUserData(new TabBase.SearchInfo(tblData.getItems(), "", false));
        setupTableSearch(tblData);

        // setup table menu to include selection column and custom annotation columns - make default focus node
        setupTableMenu(tblData, deaType, true, true, lstTblCols, data, DataDEA.DEASelectionResults.class.getName());
        setFocusNode(tblData, true);
    }
    public void viewLog() {
        String logdata = app.getLogFromFile(deaData.getDEALogFilepath(deaType), App.MAX_LOG_LINES);
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
                subTabs = TabProjectDataViz.Panels.SUMMARYDEAPROT.toString() + ";";
                subTabs += TabProjectDataViz.Panels.CLUSTERSDEAPROT.toString() + ";";
                subTabs += TabProjectDataViz.Panels.VENNDIAGDEAPROT.toString();
                break;
            case GENE:
                subTabs = TabProjectDataViz.Panels.SUMMARYDEAGENE.toString() + ";";
                subTabs += TabProjectDataViz.Panels.CLUSTERSDEAGENE.toString() + ";";
                subTabs += TabProjectDataViz.Panels.VENNDIAGDEAGENE.toString();
                break;
            case TRANS:
                subTabs = TabProjectDataViz.Panels.SUMMARYDEATRANS.toString() + ";";
                subTabs += TabProjectDataViz.Panels.CLUSTERSDEATRANS.toString() + ";";
                subTabs += TabProjectDataViz.Panels.VENNDIAGDEATRANS.toString();
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
            ObservableList<DataDEA.DEASelectionResults> data = deaData.getDEASelectionResults(deaType, "", deaParams.sigValue, deaParams.FCValue);
            FXCollections.sort(data);
            showSubTab(data);
        }
        else {
            // get script path and run service/task
            runScriptPath = app.data.getTmpScriptFileFromResource(project.data.isCaseControlExpType()? "DEA.R" : "DEA_TimeCourse.R");
            service = new DataLoadService();
            service.initialize();
            service.start();
        }
    }
    private class DataLoadService extends TaskHandler.ServiceExt {
        ObservableList<DataDEA.DEASelectionResults> data = null;

        // do all service FX thread initialization
        @Override
        public void initialize() {
            showRunAnalysisScreen(true, "Differential Expresssion Analysis in Progress...");
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
            String logfilepath = deaData.getDEALogFilepath(deaType);
            java.lang.Throwable e = getException();
            if(e != null)
                appendLogLine("DEAnalysis failed - task aborted. Exception: " + e.getMessage(), logfilepath);
            else
                appendLogLine("DEAnalysis failed - task aborted.", logfilepath);
            app.ctls.alertWarning("Differential Expression Analysis", "DEA failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                app.logInfo(deaTypeName + " DEA completed successfully.");
                HashMap<String, Object> hm = new HashMap<>();
                hm.put("updateDEACombinedResults", "DEA");
                tabBase.processRequest(hm);
                showRunAnalysisScreen(false, "");
                pageDataReady = true;
                showSubTab(data);
            }
            else {
                app.logInfo(deaTypeName + " DEA " + (taskAborted? "aborted by request." : "failed."));
                app.ctls.alertWarning(deaTypeName + " DEA", "Differential Expresssion Analysis " + (taskAborted? "aborted by request." : "failed."));
                setRunAnalysisScreenTitle("Differential Expresssion Analysis " + (taskAborted? "Aborted by Request" : "Failed"));
            }
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    String logfilepath = deaData.getDEALogFilepath(deaType);
                    outputFirstLogLine(deaTypeName + " Differential Expression Analysis for " + deaTypeName + " thread running...", logfilepath);
                    appendLogLine("Generating required input files...", logfilepath);
                    if(deaData.genDEAInputFiles(deaType)) {
                        // setup script arguments (do not use -g (for genes) since Rscript will interpret it)
                        appendLogLine("Starting R script...", logfilepath);
                        List<String> lst = new ArrayList<>();
                        lst.add(app.data.getRscriptFilepath());
                        lst.add(runScriptPath.toString());
                        String dataType = deaParams.dataType.name();
                        lst.add("-d" + dataType.toLowerCase());
                        String method = deaParams.method.name();
                        lst.add("-m" + method);
                        String reps = deaParams.replicates.name();
                        if(project.data.isCaseControlExpType()) {
                            if(deaParams.replicates.equals(DlgDEAnalysis.Params.Replicates.NONE))
                                reps = "NO";
                            lst.add("-r" + reps.toLowerCase());
                        }
                        else {
                            lst.add("-r" + deaParams.R2Cutoff);
                            lst.add("-u" + deaParams.degree);
                            lst.add("-s" + deaParams.sigValue);
                            lst.add("-k" + deaParams.k);
                            lst.add("-c" + (deaParams.mclust? "1" : "0"));
                        }
                        lst.add("-i" + project.data.getProjectDataFolder());
                        lst.add("-o" + deaData.getDEAFolder());
                        app.logInfo("Running Differential Expression Analysis...");
                        app.logDebug(deaTypeName + "DEA arguments: " + lst);
                        runScript(taskInfo, lst, "Differential Expression Analysis", logfilepath);

                        // get results if available
                        if(deaData.hasDEAData(deaType)) {
                            ObservableList<DataDEA.DEASelectionResults> results = deaData.getDEASelectionResults(deaType, "", deaParams.sigValue, deaParams.FCValue);
                            if(!results.isEmpty()) {
                                data = results;
                                FXCollections.sort(data);
                            }
                        }
                        
                        // remove script file from temp folder
                        Utils.removeFile(runScriptPath);
                        runScriptPath = null;
                    }
                    else {
                        appendLogLine("Unable to create required files for " + deaTypeName + " DEA.", logfilepath);
                        app.logError("Unable to create required files for  " + deaTypeName + " DEA");
                    }
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo(deaTypeName + " Differential Expresssion Analysis", task);
            return task;
        }
    }
}
