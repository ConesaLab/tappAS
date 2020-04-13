/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DrillDownDFIAssociation extends DlgBase {
    static int id = 0;
    static String exportFilename = "";

    Label lblHeader, lblDescription;
    Button btnHelpTbl, btnExportTbl;
    TableView tblMembers;
    Pane paneMenu;
    ProgressIndicator pi;

    int toolId;
    String source1, feature1, featureId1;
    String source2, feature2, featureId2;
    String panel;
    TaskHandler.ServiceExt service = null;
    ArrayList<DataDFI.DFIResultsData> lstResultsData = null;
    //DlgDFIAnalysis.Params dfiParams = new DlgDFInalysis.Params();
    
    public DrillDownDFIAssociation(Project project, Window window) {
        super(project, window);
        toolId = ++id;
    }
    public HashMap<String, String> showAndWait(ArrayList<DataDFI.DFIResultsData> lstResultsData,
                                                DataDFI.DFISelectionResultsAssociation selection) {
        if(createToolDialog("DrillDownDFIAssociation.fxml", "DFI Association Drill Down Data",  null)) {
            this.lstResultsData = lstResultsData;
            this.source1 = selection.getSource1();
            this.feature1 = selection.getFeature1();
            this.featureId1 = selection.getFeatureId1();
            this.source2 = selection.getSource2();
            this.feature2 = selection.getFeature2();
            this.featureId2 = selection.getFeatureId2();
            panel = TabGeneDataViz.Panels.TRANS.name();

            // get control objects
            lblHeader = (Label) scene.lookup("#lblHeader");
            lblDescription = (Label) scene.lookup("#lblDescription");
            pi = (ProgressIndicator) scene.lookup("#piImage");
            tblMembers = (TableView) scene.lookup("#tblMembers");
            paneMenu = (Pane) scene.lookup("#paneMenu");

            // setup dialog
            exportFilename = "tappAS_coDFI_GeneAssociation.tsv";
            dialog.setTitle("DFI Drill Down Data for Association Genes");
            lblHeader.setText("Genes containing both DFI " + featureId1 + " and " + featureId2);
            lblDescription.setText("");
            pi.setVisible(false);
            tblMembers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            ObservableList<TableColumn> cols = tblMembers.getColumns();
            cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Gene"));
            cols.get(1).setText("Favored (" + featureId1 + ")");
            cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Favored1"));
            cols.get(2).setText("Favored (" + featureId2 + ")");
            cols.get(2).setCellValueFactory(new PropertyValueFactory<>("Favored2"));
            cols.get(3).setCellValueFactory(new PropertyValueFactory<>("GeneDescription"));
            SubTabBase.addRowNumbersCol(tblMembers);
            ContextMenu cm = new ContextMenu();
            addGeneDVItem(cm, tblMembers, panel);
            app.export.setupTableExport(this, cm, true);
            tblMembers.setContextMenu(cm);
            app.export.addCopyToClipboardHandler(tblMembers);
            
            // populate table with data
            ObservableList<DFIAssociationDrillDownData> items = FXCollections.observableArrayList();
            HashMap<String, ArrayList<DataDFI.DFIResultsData>> hmGeneResults = new HashMap<>();
            for(DataDFI.DFIResultsData rd : lstResultsData) {
                if(rd.featureId.equals(featureId1) && rd.ds && rd.feature.equals(feature1) && rd.source.equals(source1) ||
                        rd.featureId.equals(featureId2) && rd.ds && rd.feature.equals(feature2) && rd.source.equals(source2)) {
                    if(hmGeneResults.containsKey(rd.gene)) {
                        ArrayList<DataDFI.DFIResultsData> lst = hmGeneResults.get(rd.gene);

                        // check if featureId previously added (by position can have multiple ones)
                        boolean found = false;
                        for(DataDFI.DFIResultsData lrd : lst) {
                            if(rd.featureId.equals(lrd.featureId) && rd.source.equals(lrd.source) && rd.feature.equals(lrd.feature)) {
                                found = true;
                                break;
                            }
                        }

                        // only add if different featureid
                        if(!found)
                            lst.add(rd);
                    }
                    else {
                        ArrayList<DataDFI.DFIResultsData> lst = new ArrayList<>();
                        lst.add(rd);
                        hmGeneResults.put(rd.gene, lst);
                    }
                }
            }
            for(String gene : hmGeneResults.keySet()) {
                ArrayList<DataDFI.DFIResultsData> lst = hmGeneResults.get(gene);
                if(lst.size() == 2) {
                    DataDFI.DFIResultsData rd1 = lst.get(0);
                    DataDFI.DFIResultsData rd2;
                    if(rd1.featureId.equals(featureId1) && rd1.feature.equals(feature1) && rd1.source.equals(source1))
                        rd2 = lst.get(1);
                    else {
                        rd2 = rd1;
                        rd1 = lst.get(1);
                    }
                    items.add(new DFIAssociationDrillDownData(gene, rd1.favored, rd2.favored, rd2.geneDescription));
                }
            }
            tblMembers.setItems(items);
            if(!items.isEmpty())
                tblMembers.getSelectionModel().select(0);

            // setup menu buttons
            double yoffset = 3.0;
            btnExportTbl = app.ctls.addImgButton(paneMenu, "export.png", "Export table data to file...", yoffset, 32, true);
            btnExportTbl.setOnAction((event) -> { onButtonExport(); });
            yoffset += 36;
            btnHelpTbl = app.ctls.addImgButton(paneMenu, "help.png", "Help", yoffset, 32, true);
            btnHelpTbl.setOnAction((event) -> { DlgHelp dlg = new DlgHelp(); dlg.show(title, "Help_DrillDown_DFIAssociation.html", Tappas.getWindow()); });

            // process dialog - modeless
            dialog.showAndWait();
        }
        return null;
    }
    @Override
    protected void onButtonExport() {
        DlgExportData.Config cfg = new DlgExportData.Config(true, "Genes list (IDs only)", false, "");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            HashMap<String, String> hmColNames = new HashMap<>();
            Export.ExportSelection selection = Export.ExportSelection.All;
            if(results.dataSelection.equals(DlgExportData.Params.DataSelection.SELECTEDROWS))
                selection = Export.ExportSelection.Highlighted;
            if(results.dataType.equals(DlgExportData.Params.DataType.TABLEROWS.name()))
                app.export.exportTableDataToFile(tblMembers, selection, exportFilename);
            else if(results.dataType.equals(DlgExportData.Params.DataType.LIST.name())) {
                hmColNames.put("Gene", "");
                app.export.exportTableDataListToFile(tblMembers, selection, hmColNames, exportFilename);
            }
        }
    }

    //
    // Internal Functions
    //
    protected void addGeneDVItem(ContextMenu cm, TableView tbl, String panel) {
        MenuItem item = new MenuItem("Show gene data visualization");
        cm.getItems().add(item);
        item.setOnAction((event) -> {
            // get highlighted row's data and show gene data visualization
            ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
            if(lstIdxs.size() != 1) {
                String msg = "You have multiple gene rows highlighted.\nHighlight a single row with the gene you want to visualize.";
                app.ctls.alertInformation("Display Gene Data Visualization", msg);
            }
            else {
                DFIAssociationDrillDownData dd = (DFIAssociationDrillDownData) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
                String gene = dd.getGene();
                String genes[] = gene.split(",");
                if(genes.length > 1) {
                    List<String> lst = Arrays.asList(genes);
                    Collections.sort(lst);
                    ChoiceDialog dlg = new ChoiceDialog(lst.get(0), lst);
                    dlg.setTitle("Gene Data Visualization");
                    dlg.setHeaderText("Select gene to visualize");
                    Optional<String> result = dlg.showAndWait();
                    if(result.isPresent()) {
                        gene = result.get();
                        showGeneDataVisualization(gene, panel);
                    }
                }
                else
                    showGeneDataVisualization(gene, panel);
            }
        });
    }
    private void showGeneDataVisualization(String gene, String panel) {
        HashMap<String, Object> args = new HashMap<>();
        args.put("panels", panel);
        app.tabs.openTabGeneDataViz(project, project.getDef().id, gene, project.data.getResultsGeneTrans().get(gene), "Project '" + project.getDef().name + "'", args);
    }
    
    //
    // Data Classes
    //
    static public class DFIAssociationDrillDownData implements Comparable<DFIAssociationDrillDownData> {
        public SimpleStringProperty gene;
        public SimpleStringProperty favored1;
        public SimpleStringProperty favored2;
        public SimpleStringProperty geneDescription;
        public DFIAssociationDrillDownData(String gene, String favored1, String favored2, String geneDescription) {
            this.gene = new SimpleStringProperty(gene);
            this.favored1 = new SimpleStringProperty(favored1);
            this.favored2 = new SimpleStringProperty(favored2);
            this.geneDescription = new SimpleStringProperty(geneDescription);
        }
        public String getGene() { return gene.get(); }
        public String getFavored1() { return favored1.get(); }
        public String getFavored2() { return favored2.get(); }
        public String getGeneDescription() { return geneDescription.get(); }
        
        @Override
        public int compareTo(DFIAssociationDrillDownData td) {
            return (gene.get().compareToIgnoreCase(td.gene.get()));
        }
    }
}
