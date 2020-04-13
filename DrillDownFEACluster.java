/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.Window;
import tappas.SubTabFEAClusters.NodesTableData;

import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DrillDownFEACluster extends DlgBase {
    static int id = 0;
    static String exportFilename = "";

    Label lblHeader, lblDescription;
    Button btnHelpTbl, btnExportTbl;
    TableView tblMembers;
    Pane paneMenu;
    ProgressIndicator pi;

    int toolId;
    DlgFEAnalysis.Params params;
    String analysisId = "";
    String cluster, itemName;
    int clusterId;
    String hdrName;
    String panel;
    HashMap<String, Object> hmTest = new HashMap<>();
    HashMap<String, Object> hmBkgnd = new HashMap<>();
    DataFEA.EnrichedFeaturesData featuresData = null;
    ObservableList<NodesTableData> nodesData;
    TaskHandler.ServiceExt service = null;
    DataFEA feaData;
    ObservableList<FEAClusterDrillDownData> data = null;
    
    public DrillDownFEACluster(Project project, Window window) {
        super(project, window);
        toolId = ++id;
    }
    public HashMap<String, String> showAndWait(DlgFEAnalysis.Params params, String analysisId, String cluster, int clusterId, ObservableList<NodesTableData> nodesData) {
        if(createToolDialog("DrillDownFEACluster.fxml", "FEA Cluster Drill Down Data",  null)) {
            this.params = params;
            this.analysisId = analysisId;
            this.cluster = cluster;
            this.clusterId = clusterId;
            this.nodesData = nodesData;
            feaData = new DataFEA(project);
            panel = TabGeneDataViz.Panels.TRANS.name();
            switch(params.dataType) {
                case TRANS:
                    itemName = "Transcript";
                    break;
                case PROTEIN:
                    itemName = "Protein";
                    break;
                default:
                    itemName = "Gene";
                    break;
            }

            // get control objects
            lblHeader = (Label) scene.lookup("#lblHeader");
            lblDescription = (Label) scene.lookup("#lblDescription");
            pi = (ProgressIndicator) scene.lookup("#piImage");
            tblMembers = (TableView) scene.lookup("#tblMembers");
            paneMenu = (Pane) scene.lookup("#paneMenu");

            // setup dialog
            exportFilename = "tappAS_FEA_Cluster_"+params.name+".tsv";

            dialog.setTitle("FEA Cluster Drill Down Data for " + cluster);
            lblHeader.setText(itemName + "s in cluster '" + cluster + "'");
            lblDescription.setText("Cluster: " + cluster); // not meaningful, already shown - something else?
            tblMembers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            ObservableList<TableColumn> cols = tblMembers.getColumns();
            cols.get(0).setText(itemName);
            cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Gene"));
            cols.get(1).setCellValueFactory(new PropertyValueFactory<>("InTest"));
            cols.get(2).setCellValueFactory(new PropertyValueFactory<>("Nodes"));
            cols.get(3).setCellValueFactory(new PropertyValueFactory<>("GeneDescription"));
            SubTabBase.addRowNumbersCol(tblMembers);
            ContextMenu cm = new ContextMenu();
            addGeneDVItem(cm, tblMembers, panel);
            app.export.setupTableExport(this, cm, true);
            tblMembers.setContextMenu(cm);
            app.export.addCopyToClipboardHandler(tblMembers);
            
            // setup menu buttons
            double yoffset = 3.0;
            btnExportTbl = app.ctls.addImgButton(paneMenu, "export.png", "Export table data...", yoffset, 32, true);
            btnExportTbl.setOnAction((event) -> { onButtonExport(); });
            yoffset += 36;
            btnHelpTbl = app.ctls.addImgButton(paneMenu, "help.png", "Help", yoffset, 32, true);
            btnHelpTbl.setOnAction((event) -> { DlgHelp dlg = new DlgHelp(); dlg.show(title, "Help_DrillDown_FEACluster.html", Tappas.getWindow()); });
            
            // process dialog - modeless
            dialog.setOnShown(e -> { runThread();});
            dialog.showAndWait();
        }
        return null;
    }
    @Override
    protected void onButtonExport() {
        DlgExportData.Config cfg = new DlgExportData.Config(true, itemName + "s list (IDs only)", false, "");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            HashMap<String, String> hmColNames = new HashMap<>();
            Export.ExportSelection selection = Export.ExportSelection.All;
            if(results.dataSelection.equals(DlgExportData.Params.DataSelection.SELECTEDROWS))
                selection = Export.ExportSelection.Highlighted;
            if(results.dataType.equals(DlgExportData.Params.DataType.TABLEROWS.name()))
                app.export.exportTableDataToFile(tblMembers, selection, exportFilename);
            else if(results.dataType.equals(DlgExportData.Params.DataType.LIST.name())) {
                hmColNames.put(itemName, "");
                app.export.exportTableDataListToFile(tblMembers, selection, hmColNames, exportFilename);
            }
        }
    }    

    //
    // Internal Functions
    //
    private void addGeneDVItem(ContextMenu cm, TableView tbl, String panel) {
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
                FEAClusterDrillDownData dd = (FEAClusterDrillDownData) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
                String member = dd.getGene();
                String gene = project.data.getGenes(params.dataType, member);
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
    // Data Load
    //
    private void runThread() {
        // get script path and run service/task
        service = new DataService();
        service.initialize();
        service.start();
    }
    private class DataService extends TaskHandler.ServiceExt {
        @Override
        protected void onRunning() {
            pi.setVisible(true);
        }
        @Override
        protected void onStopped() {
            pi.setVisible(false);
        }
        @Override
        protected void onFailed() {
            java.lang.Throwable e = getException();
            if(e != null)
                app.logWarning("DrillDownFEACluster failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("DrillDownFEACluster - task aborted.");
            app.ctls.alertWarning("Drill Down Funtional Enrichment Analysis Cluster", "DrillDownFEACluster - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                tblMembers.setItems(data);
                if(!data.isEmpty())
                    tblMembers.getSelectionModel().select(0);
            }
            else
                app.ctls.alertWarning("FEA Cluster Drill Down Data", "Unable to load drill down data.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        // get test list
                        ArrayList<String> lstItems = Utils.loadSingleItemListFromFile(feaData.getFEATestListFilepath(params.dataType.name(), analysisId), false);
                        if(!lstItems.isEmpty()) {
                            for(String item : lstItems)
                                hmTest.put(item, null);
                        }
                        // get background list
                        lstItems = Utils.loadSingleItemListFromFile(feaData.getFEABkgndListFilepath(params.dataType.name(), analysisId), false);
                        if(!lstItems.isEmpty()) {
                            for(String item : lstItems)
                                hmBkgnd.put(item, null);
                        }
                        // get FEA results
                        featuresData = feaData.getFEAEnrichedFeaturesData(params.dataType.name(), analysisId, params.sigValue, hmTest, hmBkgnd);
                        HashMap<String, FEAClusterDrillDownData> hmMembers = new HashMap<>();
                        for(NodesTableData nd : nodesData) {
                            if(nd.getClusterID().equals(clusterId)) {
                                String node = nd.getID();
                                int idx = 0;
                                for(DataFEA.FEAStatsData st : featuresData.lstStats) {
                                    if(st.feature.get().equals(node)) {
                                        String member;
                                        if(data == null)
                                            data = FXCollections.observableArrayList();
                                        BitSet bs = featuresData.lstTermMembers.get(idx);
                                        for(int mbr = 0; mbr < bs.size(); mbr++) {
                                            if(bs.get(mbr)) {
                                                member = featuresData.lstMembers.get(mbr);
                                                if(!hmMembers.containsKey(member)) {
                                                    String geneDesc = project.data.getGeneDescriptions(params.dataType, member);
                                                    FEAClusterDrillDownData cd = new FEAClusterDrillDownData(member, hmTest.containsKey(member), node, geneDesc);
                                                    hmMembers.put(member, cd);
                                                    data.add(cd);
                                                }
                                                else {
                                                    FEAClusterDrillDownData cd = hmMembers.get(member);
                                                    cd.nodes.set(cd.nodes.get() + ", " + node);
                                                }
                                            }
                                        }
                                    }
                                    idx++;
                                }
                            }
                        }
                        if(data != null)
                            FXCollections.sort(data);
                        else
                            app.logError("Unable to find cluster, " + cluster + ", nodes in FEA results.");
                    }
                    catch (Exception e) {
                        app.logError("Unable to load " + hdrName + ": " + e.getMessage());
                    }
                    
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("FEA Cluster Node Drill Down Data " + toolId, task);
            return task;
        }
    }
    
    //
    // Data Classes
    //
    static public class FEAClusterDrillDownData implements Comparable<FEAClusterDrillDownData> {
        public SimpleStringProperty gene;
        public SimpleStringProperty inTest;
        public SimpleStringProperty nodes;
        public SimpleStringProperty geneDescription;
        public FEAClusterDrillDownData(String gene, boolean inTest, String nodes, String geneDescription) {
            this.gene = new SimpleStringProperty(gene);
            this.inTest = new SimpleStringProperty(inTest? "YES" : "NO");
            this.nodes = new SimpleStringProperty(nodes);
            this.geneDescription = new SimpleStringProperty(geneDescription);
        }
        public String getGene() { return gene.get(); }
        public String getInTest() { return inTest.get(); }
        public String getNodes() { return nodes.get(); }
        public String getGeneDescription() { return geneDescription.get(); }
        
        @Override
        public int compareTo(FEAClusterDrillDownData td) {
            return (gene.get().compareToIgnoreCase(td.gene.get()));
        }
    }
}
