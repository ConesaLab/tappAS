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
public class DrillDownDFISummary extends DlgBase {
    static int id = 0;
    static String exportFilename = "";

    Label lblHeader, lblDescription;
    Button btnHelpTbl, btnExportTbl;
    TableView tblMembers;
    Pane paneMenu;
    ProgressIndicator pi;
    
    int toolId;
    String dataType;
    String hdrName;
    String source, feature, featureId;
    String panel;
    String analysisId;
    TaskHandler.ServiceExt service = null;
    ObservableList<DFISummaryDrillDownData> data = null;
    DataDFI dfiData;
    //DlgDFIAnalysis.Params dfiParams = new DlgDFIAnalysis.Params();
    DlgDFIAnalysis.Params dfiParams;
    
    public DrillDownDFISummary(Project project, Window window) {
        super(project, window);
        toolId = ++id;
    }
    public HashMap<String, String> showAndWait(String analysisId, String dataType, String source, String feature, String featureId) {
        if(createToolDialog("DrillDownDFISummary.fxml", "DFI Summary Features Drill Down Data",  null)) {
            this.analysisId = analysisId;
            this.dataType = dataType;
            this.source = source;
            this.feature = feature;
            this.featureId = featureId;
            exportFilename = "tappAS_DFI_ResultsSummary_" + featureId + ".tsv";
            dfiData = new DataDFI(project);
            dfiParams = DlgDFIAnalysis.Params.load(dfiData.getDFIParamsFilepath(analysisId), project);
            panel = TabGeneDataViz.Panels.TRANS.name();
            switch(dataType) {
                case "FDS":
                    hdrName = "DFI genes containing " + featureId;
                    break;
            }

            // get control objects
            lblHeader = (Label) scene.lookup("#lblHeader");
            lblDescription = (Label) scene.lookup("#lblDescription");
            pi = (ProgressIndicator) scene.lookup("#piImage");
            tblMembers = (TableView) scene.lookup("#tblMembers");
            paneMenu = (Pane) scene.lookup("#paneMenu");

            // setup dialog
            dialog.setTitle("DFI Summary Drill Down Data for: " + featureId);
            lblHeader.setText(hdrName);
            lblDescription.setText("");
            tblMembers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            ObservableList<TableColumn> cols = tblMembers.getColumns();
            cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Gene"));
            cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Position"));
            cols.get(2).setCellValueFactory(new PropertyValueFactory<>("Favored"));
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
            btnHelpTbl.setOnAction((event) -> { DlgHelp dlg = new DlgHelp(); dlg.show(title, "Help_DrillDown_DFISummary.html", Tappas.getWindow()); });
            
            // process dialog - modeless
            dialog.setOnShown(e -> { runThread();});
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
                DFISummaryDrillDownData dd = (DFISummaryDrillDownData) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
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
                app.logWarning("DrillDownDFI Summary - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("DrillDownDFI Summary - task aborted.");
            app.ctls.alertWarning("Drill Down Differential Feature Inclusion Analysis Summary", "DrillDownDFI Summary - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                tblMembers.setItems(data);
                if(!data.isEmpty())
                    tblMembers.getSelectionModel().select(0);
            }
            else
                app.ctls.alertWarning("DFI Summary Drill Down Data", "Unable to load drill down data.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        if(dataType.equals("FDS")) {
                            data = FXCollections.observableArrayList();
                            ArrayList<DataDFI.DFIResultsData> lst = dfiData.getDFIResultsData(dfiParams.sigValue, analysisId);
                            for(DataDFI.DFIResultsData rd : lst) {
                                if(rd.ds && rd.source.equals(source) && rd.feature.equals(feature) && rd.featureId.equals(featureId))
                                    data.add(new DFISummaryDrillDownData(rd.gene, rd.position, rd.favored, rd.geneDescription));
                            }
                            FXCollections.sort(data);
                        }
                    }
                    catch (Exception e) {
                        app.logError("Unable to load " + hdrName + ": " + e.getMessage());
                    }
                    
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("DFI Summary Drill Down Data " + toolId, task);
            return task;
        }
    }
    
    //
    // Data Classes
    //
    static public class DFISummaryDrillDownData implements Comparable<DFISummaryDrillDownData> {
        public SimpleStringProperty gene;
        public SimpleStringProperty favored;
        public SimpleStringProperty position;
        public SimpleStringProperty geneDescription;
        public DFISummaryDrillDownData(String gene, String position, String favored, String geneDescription) {
            this.gene = new SimpleStringProperty(gene);
            this.position = new SimpleStringProperty(position);
            this.favored = new SimpleStringProperty(favored);
            this.geneDescription = new SimpleStringProperty(geneDescription);
        }
        public String getGene() { return gene.get(); }
        public String getPosition() { return position.get(); }
        public String getFavored() { return favored.get(); }
        public String getGeneDescription() { return geneDescription.get(); }
        
        @Override
        public int compareTo(DFISummaryDrillDownData td) {
            return (gene.get().compareToIgnoreCase(td.gene.get()));
        }
    }
}
