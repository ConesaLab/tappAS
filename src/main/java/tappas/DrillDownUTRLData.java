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
public class DrillDownUTRLData extends DlgBase {
    static int id = 0;

    Label lblHeader, lblDescription;
    Button btnHelpTbl, btnExportTbl;
    TableView tblMembers;
    Pane paneMenu;
    ProgressIndicator pi;

    int toolId;
    String idColName;
    String gene;
    String panel;
    
    static String exportFilename = "";
    
    ArrayList<String> shortIso, longIso;
    TaskHandler.ServiceExt service = null;
    ObservableList<UTRLDrillDownData> data = null;
    
    public DrillDownUTRLData(Project project, Window window) {
        super(project, window);
        toolId = ++id;
    }
    public HashMap<String, String> showAndWait(String gene, HashMap<String,ArrayList<String>> iso) {
        if(createToolDialog("DrillDownUTRLData.fxml", "UTRL Features Drill Down Data",  null)) {
            this.gene = gene;
            this.shortIso = iso.get("S");
            this.longIso = iso.get("L");
            panel = TabGeneDataViz.Panels.TRANS.name();
            idColName = "Gene";
            exportFilename = "tappAS_UTRL_Isoforms_" + gene + ".tsv";
            // get control objects
            lblHeader = (Label) scene.lookup("#lblHeader");
            lblDescription = (Label) scene.lookup("#lblDescription");
            pi = (ProgressIndicator) scene.lookup("#piImage");
            tblMembers = (TableView) scene.lookup("#tblMembers");
            paneMenu = (Pane) scene.lookup("#paneMenu");

            // setup dialog
            dialog.setTitle("UTRL Drill Down Data for " + gene);
            lblHeader.setText("Gene " + gene + " contains next Isoforms");
            lblDescription.setText("");
            tblMembers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            ObservableList<TableColumn> cols = tblMembers.getColumns();
            //cols.get(0).setText(idColName);
            cols.get(0).setCellValueFactory(new PropertyValueFactory<>("shortIso"));
            cols.get(1).setCellValueFactory(new PropertyValueFactory<>("longIso"));

            SubTabBase.addRowNumbersCol(tblMembers);
            ContextMenu cm = new ContextMenu();
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
            btnHelpTbl.setOnAction((event) -> { DlgHelp dlg = new DlgHelp(); dlg.show(title, "Help_DrillDownData_UTRL.html", Tappas.getWindow()); });
            
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
            Throwable e = getException();
            if(e != null)
                app.logWarning("DrillDownUTRL - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("DrillDownUTRL - task aborted.");
            app.ctls.alertWarning("Drill Down", "DrillDownUTRL - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                tblMembers.setItems(data);
                if(!data.isEmpty())
                    tblMembers.getSelectionModel().select(1);
            }
            else
                app.ctls.alertWarning("UTRL Drill Down Data", "Unable to load drill down data.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    try {
                        ObservableList<UTRLDrillDownData> lst = FXCollections.observableArrayList();
                        int max = Math.max(shortIso.size(),longIso.size());
                        for(int i=0;i<max;i++){
                            if(i<=shortIso.size()-1 && i <= longIso.size()-1)
                                lst.add(new UTRLDrillDownData(shortIso.get(i), longIso.get(i)));
                            else if(i>longIso.size()-1 && i <= shortIso.size()-1)
                                lst.add(new UTRLDrillDownData(shortIso.get(i), ""));
                            else
                                lst.add(new UTRLDrillDownData("", longIso.get(i)));  
                        }
                        //FXCollections.sort(lst);
                        data = lst;
                    }
                    catch (Exception e) {
                        app.logError("Unable to load gene': " + gene + " " + e.getMessage());
                    }
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("UTRL Drill Down Data " + toolId, task);
            return task;
        }
    }
    
    //
    // Data Classes
    //
    static public class UTRLDrillDownData implements Comparable<UTRLDrillDownData> {
        //public SimpleStringProperty gene;
        public SimpleStringProperty shortIso;
        public SimpleStringProperty longIso;
        public UTRLDrillDownData(String shortIso, String longIso) {
            this.shortIso = new SimpleStringProperty(shortIso);
            this.longIso = new SimpleStringProperty(longIso);
        }
        //public String getGene() { return gene.get(); }
        public String getShortIso() {
            return shortIso.get();
        }
        public String getLongIso() {
            return longIso.get();
        }
        @Override
        public int compareTo(UTRLDrillDownData td) {
            return 1;
        }
        /*public int compareTo(UTRLDrillDownData td) {
            return (gene.get().compareToIgnoreCase(td.gene.get()));
        }*/
    }
}