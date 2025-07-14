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
public class SubTabData extends SubTabBase {
    TableView tblData;

    Path runScriptPath = null;
    DataType dataType;
    String colTitle;
    String panel;
    private final List<ColumnDefinition> lstColDefs = Arrays.asList(
        //                   id,            colGroup, colTitle,     propName, prefWidth, alignment, selection, Params.CompType
        new ColumnDefinition("ID",          "", "Id",               "Id", 125, "TOP-CENTER", "Id", CompType.TEXT),
        new ColumnDefinition("NAME",        "", "Name/Description", "Name", 200, "", "Name/Description", CompType.TEXT),
        new ColumnDefinition("LENGTH",      "", "Length",           "Length", 75, "TOP-RIGHT", "Transcript length in nucleotides", CompType.NUMINT),
        new ColumnDefinition("TRANS",       "", "Transcript",       "Transcript", 125, "", "Transcript", CompType.TEXT),
        new ColumnDefinition("PROTEIN",     "", "Protein",          "Protein", 125, "TOP-CENTER", "Protein name", CompType.TEXT),
        new ColumnDefinition("GENE",        "", "Gene",             "Gene", 125, "TOP-CENTER", "Gene name", CompType.TEXT),
        new ColumnDefinition("GENEDESC",    "", "Gene Description", "GeneDescription", 200, "", "Gene description", CompType.TEXT),
        new ColumnDefinition("STCAT",       "", "Structural Category",   "Category", 125, "", "Structural category", CompType.LIST),
        //new ColumnDefinition("STATTR",      "", "Structural Attributes", "Attributes", 200, "", "Structural attributes", CompType.TEXT),
        new ColumnDefinition("CHROMO",      "", "Chr",              "Chromo", 75, "TOP-CENTER", "Chromosome", CompType.LIST),
        new ColumnDefinition("STRAND",      "", "Strand",           "Strand", 75, "TOP-CENTER", "Strand", CompType.STRAND),
        new ColumnDefinition("ISOCNT",      "", "Isoforms",         "Isoforms", 75, "TOP-RIGHT", "Number of gene isoforms", CompType.NUMINT),
        new ColumnDefinition("PROTCNT",      "", "Proteins",         "Proteins", 75, "TOP-RIGHT", "Number of gene proteins", CompType.NUMINT),
        new ColumnDefinition("CODING",      "", "Coding",           "Coding", 75, "TOP-CENTER", "Protein coding gene", CompType.BOOL)
    );
    
    ObservableList<MenuItem> lstTableMenus = null;
    
    public SubTabData(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnSelect = true;
        subTabInfo.btnExport = true;
        subTabInfo.btnDataViz = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            panel = TabGeneDataViz.Panels.TRANS.name();
            dataType = (DataType) args.get("dataType");
            switch(dataType) {
                case PROTEIN:
                    colTitle = "Protein";
                    panel = TabGeneDataViz.Panels.PROTEIN.name();
                    break;
                case GENE:
                    colTitle = "Gene";
                    break;
                case TRANS:
                    colTitle = "Transcript";
                    break;
                default:
                    result = false;
                    break;
            }
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    protected void onButtonExport() {
        String colName = colTitle;
        String lstName = "";
        String filename = "";
        switch(dataType) {
            case TRANS:
                lstName = "Transcripts list (IDs only)";
                filename = "tappAS_Transcripts.tsv";
                break;
            case PROTEIN:
                lstName = "Proteins list (IDs only)";
                filename = "tappAS_Proteins.tsv";
                break;
            case GENE:
                lstName = "Genes list (IDs only)";
                filename = "tappAS_Genes.tsv";
                break;
        }
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
                            if(data.getId().toLowerCase().contains(txt) || data.getName().toLowerCase().contains(txt) || 
                                    data.getTranscript().toLowerCase().contains(txt) ||
                                    data.getGene().toLowerCase().contains(txt) || data.getGeneDescription().toLowerCase().contains(txt) ||
                                    (dataType == DataApp.DataType.TRANS && data.getProtein().toLowerCase().contains(txt))) {
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
        // get data type
        boolean trans = false;
        boolean protein = false;
        boolean gene = false;
        switch(dataType) {
            case TRANS:
                trans = true;
                break;
            case PROTEIN:
                protein = true;
                break;
            case GENE:
                gene = true;
                break;
        }
        
        // create data visualization context menu
// chg to using tabtransgroupdataviz static func call to get subtabinfo list!!! - is this msg still valid?
        ContextMenu cm = new ContextMenu();
        MenuItem item = null;
        if(trans) {
            item = new MenuItem("View Transcripts Summary");
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, TabProjectDataViz.Panels.SUMMARYTRANS); });
        }
        else if(protein) {
            item = new MenuItem("View Proteins Summary");
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, TabProjectDataViz.Panels.SUMMARYPROT); });
        }
        else if(gene) {
            item = new MenuItem("View Genes Summary");
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, TabProjectDataViz.Panels.SUMMARYGENE); });
        }
        cm.getItems().add(item);
        item = new MenuItem("View General Summary");
        item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, TabProjectDataViz.Panels.SUMMARYALL); });
        cm.getItems().add(item);
        item = new MenuItem("View Annotation Summary");
        item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, TabProjectDataViz.Panels.SUMMARYANNOTATION); });
        cm.getItems().add(item);
        item = new MenuItem("View Distribution per Gene");
        item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, TabProjectDataViz.Panels.DPERGENE); });
        cm.getItems().add(item);
        item = new MenuItem("View Transcripts, Proteins, and Gene per Chromosome");
        item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, TabProjectDataViz.Panels.DPERCHROMO); });
        cm.getItems().add(item);
        if(trans) {
            item = new MenuItem("View Transcripts per Structural Category");
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, TabProjectDataViz.Panels.SDTRANS); });
            cm.getItems().add(item);
            item = new MenuItem("View Transcript Lengths Distribution per Structural Category");
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, TabProjectDataViz.Panels.SDTRANSLENGTHS); });
            cm.getItems().add(item);
            item = new MenuItem("View Transcript Exons Distribution  per Structural Category");
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, TabProjectDataViz.Panels.SDEXONS); });
            cm.getItems().add(item);
        }
        subTabInfo.cmDataViz = cm;

        // populate table columns list
        ArrayList<ColumnDefinition> lstTblCols = new ArrayList<>();
        for(ColumnDefinition cd : lstColDefs) {
            // enum NOISeqValues { X1mean, X2mean, probability, log2FC }
            // enum EdgeRValues { log2FC, logCPM, PValue, FDR }
            // don't add columns that are not available for some analysis or data types
            // we don't have a name/desc for trans right now so don't show - will need to change to check if provided!!!
            if(((cd.id.equals("ISOCNT") || cd.id.equals("NAME")) && dataType.equals(DataType.TRANS)) ||
                    ((cd.id.equals("CHROMO") || cd.id.equals("STRAND")) && dataType.equals(DataType.PROTEIN)) ||
                    (cd.id.equals("PROTEIN") && !dataType.equals(DataType.TRANS)) ||
                    (cd.id.equals("CODING") && dataType.equals(DataType.PROTEIN)) ||
                    (cd.id.equals("TRANS") && !dataType.equals(DataType.PROTEIN)) ||
                    ((cd.id.equals("STCAT") || cd.id.equals("STATTR")) && !dataType.equals(DataType.TRANS)) ||
                    ((cd.id.equals("PROTCNT"))) ||
                    ((cd.id.equals("GENE") || cd.id.equals("GENEDESC") || cd.id.equals("LENGTH")) && dataType.equals(DataType.GENE))
                 )
                continue;
            
            // hide some columns initially
            cd.visible = !(cd.id.equals("CHROMO") || cd.id.equals("STRAND") || cd.id.equals("ISOCNT") || cd.id.equals("PROTCNT")|| cd.id.equals("CODING") ||
                            cd.id.equals("STCAT") || cd.id.equals("STATTR") || cd.id.equals("LENGTH"));

            // dynamic column titles
            switch (cd.id) {
                case "ID":
                    cd.colTitle = colTitle;
                    cd.selection = colTitle;
                    break;
                case "TRANS":
                    if(dataType.equals(DataType.PROTEIN))
                        cd.colTitle = "Transcript(s)";
                    else
                        cd.colTitle = "Transcript";
                    break;
                case "GENE":
                    if(dataType.equals(DataType.PROTEIN))
                        cd.colTitle = "Gene(s)";
                    else
                        cd.colTitle = "Gene";
                    break;
                case "GENEDESC":
                    if(dataType.equals(DataType.PROTEIN))
                        cd.colTitle = "Gene Description(s)";
                    else
                        cd.colTitle = "Gene Description";
                    break;
            }
            lstTblCols.add(cd);
        }

        // configure table columns
        tblData = (TableView) tabNode.lookup("#tblData");
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

        // set table data and select first row
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
                        for(DataSelectionResults dr : data)
                            hm.put(dr.getChromo(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String chr : hm.keySet())
                            lstSelections.add(chr);
                        Collections.sort(lstSelections, new Chromosome.ChromoComparator());
                        cd.lstSelections = (List)lstSelections;
                        break;
                    }
                case "STCAT":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DataSelectionResults dr : data)
                            hm.put(dr.getCategory(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String cat : hm.keySet())
                            lstSelections.add(cat);
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
        setupTableMenu(tblData, dataType, true, true, lstTblCols, data, DataSelectionResults.class.getName());
        setFocusNode(tblData, true);
    }
    
    //
    // Background Data Load
    //
    @Override
    protected void runDataLoadThread() {
        ObservableList<DataSelectionResults> data =  project.data.getDataSelectionResults(dataType);
        FXCollections.sort(data);
        showSubTab(data);
    }
}
