/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import tappas.DataApp.DataType;
import tappas.DataDEA.DASelectionResults;
import tappas.DlgSelectRows.ColumnDefinition;
import tappas.DlgSelectRows.Params.CompType;

import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDACombinedResults extends SubTabBase {
    TableView tblData;

    private final List<ColumnDefinition> lstColDefs = Arrays.asList(
        //                   id,            colGroup, colTitle,           propName, prefWidth, alignment, selection, Params.CompType
        new ColumnDefinition("GENE",        "Genes", "Gene",             "Gene", 125, "TOP-CENTER", "Gene name", CompType.TEXT),
        new ColumnDefinition("GENEDESC",    "Genes", "Gene Description", "GeneDescription", 200, "", "Gene description", CompType.TEXT),
        new ColumnDefinition("TRANSDIURESULT","Genes", "DIU Transcripts Result", "TransDSGene", 140, "TOP-CENTER", "DIU Result for Transcripts", CompType.LISTIDVALS),
        new ColumnDefinition("PROTDIURESULT", "Genes", "DIU Proteins Result",    "ProtDSGene", 120, "TOP-CENTER", "DIU Result for Proteins", CompType.LISTIDVALS),
        new ColumnDefinition("DEARESULT",   "Genes", "DEA Result",       "DEGene", 100, "TOP-CENTER", "DEA Result", CompType.LIST),
        new ColumnDefinition("CHROMO",      "Genes", "Chr",              "Chromo", 75, "TOP-CENTER", "Chromosome", CompType.LIST),
        new ColumnDefinition("STRAND",      "Genes", "Strand",           "Strand", 75, "TOP-CENTER", "Strand", CompType.STRAND),
        new ColumnDefinition("ISOCNT",      "Genes", "Isoforms",         "Isoforms", 75, "TOP-RIGHT", "Number of gene isoforms", CompType.NUMINT),
        new ColumnDefinition("CODING",      "Genes", "Coding",           "Coding", 75, "TOP-CENTER", "Protein coding gene", CompType.BOOL),
        new ColumnDefinition("DEPROTEIN",   "Proteins", "DE",            "DEProteins", 100, "TOP-CENTER", "Differentially Expressed proteins", CompType.TEXT),
        new ColumnDefinition("DEPTOTAL",    "Proteins", "Total",         "TotalProteinCnt", 100, "TOP-RIGHT", "Differentially Expressed proteins Count", CompType.NUMINT),
        new ColumnDefinition("PROTSWITCHING",    "Proteins", "Switching",         "ProteinSwitching", 100, "TOP-CENTER", "Major Switching Protein", CompType.TEXT),
        new ColumnDefinition("DETRANS",     "Transcripts", "DE",         "DEIsoforms", 100, "TOP-CENTER", "Differentially Expressed transcripts", CompType.TEXT),
        new ColumnDefinition("DETTOTAL",    "Transcripts", "Total",      "TotalIsoformCnt", 100, "TOP-RIGHT", "Differentially Expressed transcripts count", CompType.NUMINT),
        new ColumnDefinition("ISOSWITCHING",    "Transcripts", "Switching",         "TranscriptSwitching", 100, "TOP-CENTER", "Major Switching Isoform", CompType.TEXT)
        // DFI was moved out to its own menu and it seems out of place here
        //new ColumnDefinition("DSFEATURES",  "Features",  "DS",           "DSFeatures", 125, "", "Differentially Spliced Features", CompType.TEXT)
    );
    ObservableList<MenuItem> lstTableMenus = null;
    
    public SubTabDACombinedResults(Project project) {
        super(project);
    }

    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnSelect = true;
        subTabInfo.btnExport = true;
        // until maSigPro is finalized, only show for case-control
        if(project.data.isCaseControlExpType())
            subTabInfo.btnDataViz = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        return result;
    }
    @Override
    protected void onButtonExport() {
        ArrayList<DataApp.EnumData> lstOtherSelections = new ArrayList<>();
        if(project.data.analysis.hasDIUDataTrans()) {
            lstOtherSelections.add(new DataApp.EnumData("TRANSDS", "Differentially spliced genes list (Transcripts)"));
            lstOtherSelections.add(new DataApp.EnumData("TRANSNOTDS", "NOT differentially spliced genes list (Transcripts - multiple isoforms)"));
        }
        if(project.data.analysis.hasDIUDataProtein()) {
            lstOtherSelections.add(new DataApp.EnumData("PROTDS", "Differentially spliced genes list (Proteins)"));
            lstOtherSelections.add(new DataApp.EnumData("PROTNOTDS", "NOT differentially spliced genes list (Proteins - multiple isoforms)"));
        }
        if(project.data.analysis.hasDEAData(DataApp.DataType.GENE)) {
            lstOtherSelections.add(new DataApp.EnumData("GDE", "Differentially expressed genes list"));
            lstOtherSelections.add(new DataApp.EnumData("GNOTDE", "NOT differentially expressed genes list"));
        }
        if(project.data.analysis.hasDEAData(DataApp.DataType.PROTEIN)) {
            lstOtherSelections.add(new DataApp.EnumData("PDE", "Differentially expressed proteins list"));
        }
        if(project.data.analysis.hasDEAData(DataApp.DataType.TRANS)) {
            lstOtherSelections.add(new DataApp.EnumData("TDE", "Differentially expressed transcripts list"));
        }
        DlgExportData.Config cfg = new DlgExportData.Config(true, "All genes list (IDs only)", false, "");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, lstOtherSelections);
        if(results != null) {
            String filename = "tappAS_DifferentialAnalyses_Results.tsv";
            HashMap<String, String> hmColNames = new HashMap<>();
            boolean allRows = true;
            if(results.dataSelection.equals(DlgExportData.Params.DataSelection.SELECTEDROWS))
                allRows = false;
            if(results.dataType.equals(DlgExportData.Params.DataType.TABLEROWS.name())) {
                app.export.exportTableDataToFile(tblData, allRows, filename);
            }
            else if(results.dataType.equals(DlgExportData.Params.DataType.LIST.name())) {
                hmColNames.put("Genes", "Name");
                app.export.exportTableDataListToFile(tblData, allRows, hmColNames, filename);
            }
            else {
                // if users selects one of the special lists then all rows must be included
                // otherwise they can just use selected only
                filename = "";
                HashMap<String, Object> hmItems = new HashMap<>();
                switch(results.dataType) {
                    case "TRANSDS":
                        filename = "tappAS_TransDS.tsv";
                        if(allRows) {
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                if(data.getTransDSGene().trim().equals("DS"))
                                    hmItems.put(data.getGene(), null);
                            }
                        }
                        else {
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                if(data.isSelected()) {
                                    if(data.getTransDSGene().trim().equals("DS"))
                                        hmItems.put(data.getGene(), null);
                                }
                            }
                        }
                        break;
                    case "TRANSNOTDS":
                        filename = "tappAS_TransNotDS.tsv";
                        if(allRows) {
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                if(data.getTransDSGene().trim().equals("Not DS"))
                                    hmItems.put(data.getGene(), null);
                            }
                        }
                        else {
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                if(data.isSelected()) {
                                    if(data.getTransDSGene().trim().equals("Not DS"))
                                        hmItems.put(data.getGene(), null);
                                }
                            }
                        }
                        break;
                    case "PROTDS":
                        filename = "tappAS_ProteinDS.tsv";
                        if(allRows) {
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                if(data.getProtDSGene().trim().equals("DS"))
                                    hmItems.put(data.getGene(), null);
                            }
                        }
                        else {
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                if(data.isSelected()) {
                                    if(data.getProtDSGene().trim().equals("DS"))
                                        hmItems.put(data.getGene(), null);
                                }
                            }
                        }
                        break;
                    case "PROTNOTDS":
                        filename = "tappAS_ProteinNotDS.tsv";
                        if(allRows) {
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                if(data.getProtDSGene().trim().equals("Not DS"))
                                    hmItems.put(data.getGene(), null);
                            }
                        }
                        else {
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                if(data.isSelected()) {
                                    if(data.getProtDSGene().trim().equals("Not DS"))
                                        hmItems.put(data.getGene(), null);
                                }
                            }
                        }
                        break;
                    case "GDE":
                        filename = "tappAS_DEgenes.tsv";
                        if(allRows) {
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                if(data.getDEGene().trim().equals("DE"))
                                    hmItems.put(data.getGene(), null);
                            }
                        }
                        else {
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                if(data.isSelected()) {
                                    if(data.getDEGene().trim().equals("DE"))
                                        hmItems.put(data.getGene(), null);
                                }
                            }
                        }
                        break;
                    case "GNOTDE":
                        filename = "tappAS_NotDEgenes.tsv";
                        if(allRows) {
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                if(data.getDEGene().trim().equals("Not DE"))
                                    hmItems.put(data.getGene(), null);
                            }
                        }
                        else {
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                if(data.isSelected()) {
                                    if(data.getDEGene().trim().equals("Not DE"))
                                        hmItems.put(data.getGene(), null);
                                }
                            }
                        }
                        break;
                    case "PDE":
                        filename = "tappAS_DEproteins.tsv";
                        if(allRows) {
                            String values;
                            String[] fields;
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                values = data.getDEProteins().trim();
                                if(!values.isEmpty()) {
                                    fields = values.split(",");
                                    for(String field : fields)
                                        hmItems.put(field.trim(), null);
                                }
                            }
                        }
                        else {
                            String values;
                            String[] fields;
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                if(data.isSelected()) {
                                    values = data.getDEProteins().trim();
                                    if(!values.isEmpty()) {
                                        fields = values.split(",");
                                        for(String field : fields)
                                            hmItems.put(field.trim(), null);
                                    }
                                }
                            }
                        }
                        break;
                    case "TDE":
                        filename = "tappAS_DEtranscripts.tsv";
                        if(allRows) {
                            String values;
                            String[] fields;
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                values = data.getDEIsoforms().trim();
                                if(!values.isEmpty()) {
                                    fields = values.split(",");
                                    for(String field : fields)
                                        hmItems.put(field.trim(), null);
                                }
                            }
                        }
                        else {
                            String values;
                            String[] fields;
                            for(DASelectionResults data : (ObservableList<DASelectionResults>)tblData.getItems()) {
                                if(data.isSelected()) {
                                    values = data.getDEIsoforms().trim();
                                    if(!values.isEmpty()) {
                                        fields = values.split(",");
                                        for(String field : fields)
                                            hmItems.put(field.trim(), null);
                                    }
                                }
                            }
                        }
                        break;
                }
                
                ArrayList<String> lstItems = new ArrayList<>();
                for(String item : hmItems.keySet())
                    lstItems.add(item);
                Collections.sort(lstItems);
                app.export.exportListToFile(lstItems, filename);
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
                System.out.println("DA combined results table search called: '" + txt + ", hide: " + hide);
                TableView<DASelectionResults> tblView = (TableView<DASelectionResults>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<DASelectionResults> items = (ObservableList<DASelectionResults>) info.data;
                if(items != null) {
                    ObservableList<DASelectionResults> matchItems;
                    if(txt.isEmpty()) {
                        if(hide) {
                            matchItems = FXCollections.observableArrayList();
                            for(DASelectionResults data : items) {
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
                        for(DASelectionResults data : items) {
                            if(data.getGene().toLowerCase().contains(txt) || data.getGeneDescription().toLowerCase().contains(txt) ||
                                    data.getDEIsoforms().toLowerCase().contains(txt) ||
                                    data.getDEProteins().toLowerCase().contains(txt)) { // || data.getDSFeatures().toLowerCase().contains(txt)) {
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
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        if(hm.containsKey("updateDEACombinedResults")) {
            ObservableList<DASelectionResults> data = project.data.analysis.getDASelectionResults();
            updateDisplayData(data);
        }
        return obj;
    }
    
    //
    // Internal Functions
    //
    private void showSubTab(ObservableList<DASelectionResults> data) {
        String[] resultNames = project.data.getResultNames();

        // get control objects
        tblData = (TableView) tabNode.lookup("#tblDA_Results");

        // create data visualization context menu - until maSigPro is finalized, only show for case-control
        ContextMenu cm = new ContextMenu();
        if(project.data.isCaseControlExpType()) {
            String group = resultNames[0];
            MenuItem item = new MenuItem("View DIU Transcript Summary");
            item.setDisable(!project.data.analysis.hasDIUDataTrans());
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, TabProjectDataViz.Panels.SUMMARYDIUTRANS); });
            cm.getItems().add(item);
            item = new MenuItem("View DIU Protein Summary");
            item.setDisable(!project.data.analysis.hasDIUDataProtein());
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTab(project, TabProjectDataViz.Panels.SUMMARYDIUPROT); });
            cm.getItems().add(item);
            item = new MenuItem("View DEA Transcripts Summary");
            item.setDisable(!project.data.analysis.hasDEAData(DataApp.DataType.TRANS));
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, TabProjectDataViz.Panels.SUMMARYDEATRANS, group); });
            cm.getItems().add(item);
            item = new MenuItem("View DEA Proteins Summary");
            item.setDisable(!project.data.analysis.hasDEAData(DataApp.DataType.PROTEIN));
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, TabProjectDataViz.Panels.SUMMARYDEAPROT, group); });
            cm.getItems().add(item);
            item = new MenuItem("View DEA Genes Summary");
            item.setDisable(!project.data.analysis.hasDEAData(DataApp.DataType.GENE));
            item.setOnAction((event) -> { app.ctlr.openProjectDataVizSubTabId(project, TabProjectDataViz.Panels.SUMMARYDEAGENE, group); });
            cm.getItems().add(item);
            subTabInfo.cmDataViz = cm;
        }
        
        // populate table columns list
        ArrayList<ColumnDefinition> lstTblCols = new ArrayList<>();
        for(ColumnDefinition cd : lstColDefs) {
            // hide some columns initially
            cd.visible = !(cd.id.equals("CHROMO") || cd.id.equals("STRAND") || cd.id.equals("ISOCNT") || cd.id.equals("CODING"));
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

        // set table popup context menu and include export option
        cm = new ContextMenu();
        addGeneDVItem(cm, tblData, TabGeneDataViz.Panels.TRANS.name());
        app.export.setupTableExport(subTabInfo.subTabBase, cm, true);
        tblData.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblData);

        // handle special columns, once data is available
        for(ColumnDefinition cd : lstTblCols) {
            // handle special cases - only need to do this once
            switch (cd.id) {
                case "CHROMO":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DASelectionResults dr : data)
                            hm.put(dr.getChromo(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String chr : hm.keySet())
                            lstSelections.add(chr);
                        Collections.sort(lstSelections, new Chromosome.ChromoComparator());
                        cd.lstSelections = (List)lstSelections;
                        break;
                    }
                case "TRANSDIURESULT":
                    cd.lstIdValuesSelections = DlgSelectRows.Params.lstIdValuesDIU;
                    break;
                case "PROTDIURESULT":
                    cd.lstIdValuesSelections = DlgSelectRows.Params.lstIdValuesDIU;
                    break;
                case "DEARESULT":
                    {
                        HashMap<String, Object> hm = new HashMap<>();
                        for(DASelectionResults dr : data)
                            hm.put(dr.getDEGene(), null);
                        ArrayList<String> lstSelections = new ArrayList<>();
                        for(String val : hm.keySet())
                            lstSelections.add(val);
                        Collections.sort(lstSelections);
                        cd.lstSelections = (List)lstSelections;
                        break;
                    }
            }
        }

        // setup table search functionality
        updateDisplayData(data);
        setupTableSearch(tblData);

        // setup table menu to include selection column and custom annotation columns - make default focus node
        setupTableMenu(tblData, DataType.GENE, true, true, lstTblCols, data, DASelectionResults.class.getName());
        setFocusNode(tblData, true);
    }
    private void updateDisplayData(ObservableList<DASelectionResults> data) {
        // set table data and select first row
        FXCollections.sort(data);
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
        ObservableList<DASelectionResults> data = project.data.analysis.getDASelectionResults();
        showSubTab(data);
    }
}
