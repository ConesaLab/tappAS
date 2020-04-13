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
 * @author Pedro Salguero - psalguero@cipf.es
 */
public class DrillDownDPASummary extends DlgBase {
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
    TaskHandler.ServiceExt service = null;
    ObservableList<DPASummaryDrillDownData> data = null;
    DataDPA dpaData;
    //DlgDPAnalysis.Params dpaParams = new DlgDPAnalysis.Params();
    DlgDPAnalysis.Params dpaParams;
    
    public DrillDownDPASummary(Project project, Window window) {
        super(project, window);
        toolId = ++id;
    }
    public HashMap<String, String> showAndWait(String dataType, String source, String feature, String featureId) {
        if(createToolDialog("DrillDownDPASummary.fxml", "DPA Summary Features Drill Down Data",  null)) {
            this.dataType = dataType;
            this.source = source;
            this.feature = feature;
            this.featureId = featureId;
            dpaData = new DataDPA(project);
            dpaParams = DlgDPAnalysis.Params.load(dpaData.getDPAParamsFilepath(), project);
            panel = TabGeneDataViz.Panels.TRANS.name();
            switch(dataType) {
                case "FDS":
                    hdrName = "Genes containing DS " + featureId;
                    break;
            }

            // get control objects
            lblHeader = (Label) scene.lookup("#lblHeader");
            lblDescription = (Label) scene.lookup("#lblDescription");
            pi = (ProgressIndicator) scene.lookup("#piImage");
            tblMembers = (TableView) scene.lookup("#tblMembers");
            paneMenu = (Pane) scene.lookup("#paneMenu");

            // setup dialog
            exportFilename = "tappAS_DPA_Summary_"+featureId+".tsv";

            dialog.setTitle("DPA Summary Drill Down Data for DS " + featureId);
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
            btnHelpTbl.setOnAction((event) -> { DlgHelp dlg = new DlgHelp(); dlg.show(title, "Help_DrillDown_DPASummary.html", Tappas.getWindow()); });
            
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
                DPASummaryDrillDownData dd = (DPASummaryDrillDownData) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
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
                app.logWarning("DrillDownDPA Summary - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("DrillDownDPA Summary - task aborted.");
            app.ctls.alertWarning("Drill Down Differential PolyAdenylation Analysis Summary", "DrillDownDPA Summary - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                tblMembers.setItems(data);
                if(!data.isEmpty())
                    tblMembers.getSelectionModel().select(0);
            }
            else
                app.ctls.alertWarning("DPA Summary Drill Down Data", "Unable to load drill down data.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        if(dataType.equals("FDS")) {
                            data = FXCollections.observableArrayList();
                            ArrayList<DataDPA.DPAResultsData> lst = dpaData.getDPAResultsData(dpaParams.sigValue);
                            for(DataDPA.DPAResultsData rd : lst) {
                                data.add(new DPASummaryDrillDownData(rd.gene, rd.favored, rd.geneDescription));
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
            taskInfo = new TaskHandler.TaskInfo("DPA Summary Drill Down Data " + toolId, task);
            return task;
        }
    }
    
    //
    // Data Classes
    //
    static public class DPASummaryDrillDownData implements Comparable<DPASummaryDrillDownData> {
        public SimpleStringProperty gene;
        public SimpleStringProperty shortIso;
        public SimpleStringProperty longIso;
        public DPASummaryDrillDownData(String gene, String shortIso, String longIso) {
            this.gene = new SimpleStringProperty(gene);
            this.shortIso = new SimpleStringProperty(shortIso);
            this.longIso = new SimpleStringProperty(longIso);
        }
        public String getGene() { return gene.get(); }
        public String getShortIso() { return shortIso.get(); }
        public String getLongIso() { return longIso.get(); }
        
        @Override
        public int compareTo(DPASummaryDrillDownData td) {
            return (gene.get().compareToIgnoreCase(td.gene.get()));
        }
    }
}
