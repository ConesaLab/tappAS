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

import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DrillDownGSEAClusterNode extends DlgBase {
    static int id = 0;
    static String exportFilename = "";

    Label lblHeader, lblDescription;
    Button btnHelpTbl, btnExportTbl;
    TableView tblMembers;
    Pane paneMenu;
    ProgressIndicator pi;

    int toolId;
    DlgGSEAnalysis.Params params;
    String analysisId = "";
    String cluster, itemName;
    String hdrName;
    String node;
    String panel;
    HashMap<String, Object> hmTest = new HashMap<>();
    HashMap<String, Object> hmBkgnd = new HashMap<>();
    DataGSEA.EnrichedFeaturesData featuresData = null;
    TaskHandler.ServiceExt service = null;
    DataGSEA gseaData;
    ObservableList<GSEAClusterNodeDrillDownData> data = null;
    
    public DrillDownGSEAClusterNode(Project project, Window window) {
        super(project, window);
        toolId = ++id;
    }
    public HashMap<String, String> showAndWait(DlgGSEAnalysis.Params params, String analysisId, String cluster, String node) {
        if(createToolDialog("DrillDownGSEAClusterNode.fxml", "GSEA Cluster Node Drill Down Data",  null)) {
            this.params = params;
            this.analysisId = analysisId;
            this.cluster = cluster;
            this.node = node;
            gseaData = new DataGSEA(project);
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
            pi = (ProgressIndicator) scene.lookup("#piImage");
            tblMembers = (TableView) scene.lookup("#tblMembers");
            lblDescription = (Label) scene.lookup("#lblDescription");
            paneMenu = (Pane) scene.lookup("#paneMenu");
            
            // setup dialog
            exportFilename = "tappAS_GSEA_ClusterNode_"+params.name+".tsv";
            dialog.setTitle("GSEA Cluster Drill Down Data for Node " + node);
            lblHeader.setText(itemName + "s in node '" + node + "'");
            lblDescription.setText("Cluster: " + cluster);
            tblMembers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            ObservableList<TableColumn> cols = tblMembers.getColumns();
            cols.get(0).setText(itemName);
            cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Gene"));
            cols.get(1).setCellValueFactory(new PropertyValueFactory<>("InTest"));
            cols.get(2).setCellValueFactory(new PropertyValueFactory<>("GeneDescription"));
            SubTabBase.addRowNumbersCol(tblMembers);
            ContextMenu cm = new ContextMenu();
            addGeneDVItem(cm, tblMembers, panel);
            app.export.setupTableExport(this, cm, true);
            tblMembers.setContextMenu(cm);
            app.export.addCopyToClipboardHandler(tblMembers);
            
            double yoffset = 3.0;
            btnExportTbl = app.ctls.addImgButton(paneMenu, "export.png", "Export table data...", yoffset, 32, true);
            btnExportTbl.setOnAction((event) -> { onButtonExport(); });
            yoffset += 36;
            btnHelpTbl = app.ctls.addImgButton(paneMenu, "help.png", "Help", yoffset, 32, true);
            btnHelpTbl.setOnAction((event) -> { DlgHelp dlg = new DlgHelp(); dlg.show(title, "Help_DrillDown_GSEAClusterNode.html", Tappas.getWindow()); });
            
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
                GSEAClusterNodeDrillDownData dd = (GSEAClusterNodeDrillDownData) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
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
    // Thread Processing Functions
    //
    private void runThread() {
        // get script path and run service/task
        service = new DataService();
        service.initialize();
        service.start();
    }
    
    //
    // Script service/task
    //
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
            Throwable e = getException();
            if(e != null)
                app.logWarning("DrillDownGSEAClusterNode failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("DrillDownGSEAClusterNode - task aborted.");
            app.ctls.alertWarning("Drill Down Funtional Enrichment Analysis Cluster Node", "DrillDownGSEAClusterNode - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                tblMembers.setItems(data);
                if(!data.isEmpty())
                    tblMembers.getSelectionModel().select(0);
            }
            else
                app.ctls.alertWarning("GSEA Cluster Node Drill Down Data", "Unable to load drill down data.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        // get GSEA results
                        featuresData = gseaData.getGSEAEnrichedFeaturesData(params.dataType.name(), analysisId, params.sigValue);
                        int idx = 0;
                        for(DataGSEA.GSEAStatsData st : featuresData.lstStats) {
                            if(st.feature.get().equals(node)) {
                                String member;
                                data = FXCollections.observableArrayList();
                                BitSet bs = featuresData.lstTermMembers.get(idx);
                                for(int mbr = 0; mbr < bs.size(); mbr++) {
                                    if(bs.get(mbr)) {
                                        //list members son cuantos genes poseen esa característica?? !!!
                                        member = featuresData.lstMembers.get(mbr);
                                        String geneDesc = project.data.getGeneDescriptions(params.dataType, member);
                                        data.add(new GSEAClusterNodeDrillDownData(member, hmTest.containsKey(member), geneDesc));
                                    }
                                }
                            }
                            idx++;
                        }
                        if(data != null)
                            FXCollections.sort(data);
                        else
                            app.logError("Unable to find node, " + node + ", in GSEA results.");
                    }
                    catch (Exception e) {
                        app.logError("Unable to load " + hdrName + ": " + e.getMessage());
                    }
                    
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("GSEA Cluster Node Drill Down Data " + toolId, task);
            return task;
        }
    }
    
    //
    // Data Classes
    //
    static public class GSEAClusterNodeDrillDownData implements Comparable<GSEAClusterNodeDrillDownData> {
        public SimpleStringProperty gene;
        public SimpleStringProperty inTest;
        public SimpleStringProperty geneDescription;
        public GSEAClusterNodeDrillDownData(String gene, boolean inTest, String geneDescription) {
            this.gene = new SimpleStringProperty(gene);
            this.inTest = new SimpleStringProperty(inTest? "YES" : "NO");
            this.geneDescription = new SimpleStringProperty(geneDescription);
        }
        public String getGene() { return gene.get(); }
        public String getInTest() { return inTest.get(); }
        public String getGeneDescription() { return geneDescription.get(); }
        
        @Override
        public int compareTo(GSEAClusterNodeDrillDownData td) {
            return (gene.get().compareToIgnoreCase(td.gene.get()));
        }
    }
}
