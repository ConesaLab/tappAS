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

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Pedro Salguero - psalguero@cipf.es
 */
public class DrillDownFDAData extends DlgBase {
    static int id = 0;

    Label lblHeader, lblDescription;
    Button btnHelpTbl, btnExportTbl;
    TableView tblMembers;
    Pane paneMenu;
    ProgressIndicator pi;

    ArrayList<String> gene, varying;
    int toolId;
    String idColName;
    String ID;
    String panel;
    
    static String exportFilename = "";
    
    TaskHandler.ServiceExt service = null;
    ObservableList<FDADrillDownData> data = null;
    
    public DrillDownFDAData(Project project, Window window) {
        super(project, window);
        toolId = ++id;
    }
    public HashMap<String, String> showAndWait(String ID, HashMap<String,ArrayList<String>> hsGenes) {
        if(createToolDialog("DrillDownFDAData.fxml", "FDA Features Drill Down Data",  null)) {
            this.ID = ID;
            this.gene = new ArrayList<>();
            this.varying = new ArrayList<>();            
            
            for(String var : hsGenes.keySet()){
                if(hsGenes.get(var).size()>0){
                    for(String g : hsGenes.get(var)){
                        this.gene.add(g);
                        this.varying.add(var.equals("V")? "YES" : "NO"); 
                    }
                }
            }
            
            panel = TabGeneDataViz.Panels.TRANS.name();
            idColName = "ID";
            exportFilename = "tappAS_FDA_"+gene+"_Isoforms_" + ID + ".tsv";
            // get control objects
            lblHeader = (Label) scene.lookup("#lblHeader");
            lblDescription = (Label) scene.lookup("#lblDescription");
            pi = (ProgressIndicator) scene.lookup("#piImage");
            tblMembers = (TableView) scene.lookup("#tblMembers");
            paneMenu = (Pane) scene.lookup("#paneMenu");

            // setup dialog
            dialog.setTitle("FDA Drill Down Data for " + ID);
            lblHeader.setText("ID " + ID + " contains next Genes");
            lblDescription.setText("");
            tblMembers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            
            //Create 2 columns - gene and varying
            ObservableList<TableColumn> cols = tblMembers.getColumns();
//            TableColumn<FDADrillDownData,String> geneCol = new TableColumn<FDADrillDownData,String>("Genes");
//            geneCol.setCellValueFactory(new PropertyValueFactory("gene"));
//            TableColumn<FDADrillDownData,String> varyingCol = new TableColumn<FDADrillDownData,String>("Varying");
//            varyingCol.setCellValueFactory(new PropertyValueFactory("varying"));
//            tblMembers.getColumns().setAll(geneCol, varyingCol);
//            SubTabBase.addRowNumbersCol(tblMembers);        
            
            cols.get(0).setCellValueFactory(new PropertyValueFactory<>("gene"));
            cols.get(1).setCellValueFactory(new PropertyValueFactory<>("varying"));

            ContextMenu cm = new ContextMenu();
            addGeneDVItemFDA(cm, tblMembers, panel);
            //addGeneDVItem(cm, tblMembers, panel);
            app.export.setupTableExport(this, cm, true);
            tblMembers.setContextMenu(cm);
            app.export.addCopyToClipboardHandler(tblMembers);
            
            // setup menu buttons
            double yoffset = 3.0;
            btnExportTbl = app.ctls.addImgButton(paneMenu, "export.png", "Export table data...", yoffset, 32, true);
            btnExportTbl.setOnAction((event) -> { onButtonExport(); });
            yoffset += 36;
            btnHelpTbl = app.ctls.addImgButton(paneMenu, "help.png", "Help", yoffset, 32, true);
            btnHelpTbl.setOnAction((event) -> { DlgHelp dlg = new DlgHelp(); dlg.show(title, "Help_DrillDownData_FDA.html", Tappas.getWindow()); });
            
            // process dialog - modeless
            dialog.setOnShown(e -> { runThread();});
            dialog.showAndWait();
        }
        return null;
    }
    @Override
    protected void onButtonExport() {
        DlgExportData.Config cfg = new DlgExportData.Config(true, idColName + "s list (IDs only)", false, "");
        DlgExportData.Params results = app.ctlr.getExportDataParams(cfg, null, null);
        if(results != null) {
            HashMap<String, String> hmColNames = new HashMap<>();
            Export.ExportSelection selection = Export.ExportSelection.All;
            if(results.dataSelection.equals(DlgExportData.Params.DataSelection.SELECTEDROWS))
                selection = Export.ExportSelection.Highlighted;
            if(results.dataType.equals(DlgExportData.Params.DataType.TABLEROWS.name()))
                app.export.exportTableDataToFile(tblMembers, selection, exportFilename);
            else if(results.dataType.equals(DlgExportData.Params.DataType.LIST.name())) {
                hmColNames.put(idColName, "");
                app.export.exportTableDataListToFile(tblMembers, selection, hmColNames, exportFilename);
            }
        }
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
        public void initialize() {
            data = null;
        }
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
                app.logWarning("DrillDownFDA - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("DrillDownFDA - task aborted.");
            app.ctls.alertWarning("Drill Down", "DrillDownFDA - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                tblMembers.setItems(data);
                if(!data.isEmpty())
                    tblMembers.getSelectionModel().select(0);
            }
            else
                app.ctls.alertWarning("FDA Drill Down Data", "Unable to load drill down data.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        ObservableList<FDADrillDownData> lst = FXCollections.observableArrayList();
                        for(int i = 0; i < gene.size(); i++){
                            lst.add(new FDADrillDownData(gene.get(i), varying.get(i)));
                        }
                        FXCollections.sort(lst);
                        data = lst;
                    }
                    catch (Exception e) {
                        app.logError("Unable to load gene': " + ID + " " + e.getMessage());
                    }
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("FDA Drill Down Data " + toolId, task);
            return task;
        }
    }
    
    //
    // Data Classes
    //
    static public class FDADrillDownData implements Comparable<FDADrillDownData> {
        //public SimpleStringProperty gene;
        public SimpleStringProperty gene;
        public SimpleStringProperty varying;
        
        public FDADrillDownData(String gene, String varying) {
            this.gene = new SimpleStringProperty(gene);
            this.varying = new SimpleStringProperty(varying);
        }
        //public String getGene() { return gene.get(); }
        public String getGene() {
            return gene.get();
        }
        public String getVarying() {
            return varying.get();
        }
        @Override
        public int compareTo(FDADrillDownData td) {
            return 1;
        }
    }
}