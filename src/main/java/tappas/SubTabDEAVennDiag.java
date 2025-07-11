/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDEAVennDiag extends SubTabBase {
    AnchorPane paneContents;
    AnchorPane paneImgView;
    GridPane grdTable;
    Pane paneMenuTbl;
    ImageView imgView;
    TableView tblMembers;
    RadioButton rbUnion, rbInt, rbDiff;
    CheckBox chk1, chk2, chk3, chk4, chk5;

    String panel;
    boolean timeSeries = false;
    boolean timeMultiple = false;
    boolean hasData = false;
    boolean hasVennDiag = false;
    String vdFilepath, vdLogFilepath;
    DataApp.DataType deaType;
    String deaTypeID, deaTypeName;
    DataDEA deaData;
    ArrayList<DlgVennParams.Params.DataSetDef> lstDefs;
    java.nio.file.Path runScriptPath;

    ArrayList<String> setNames = new ArrayList<>();
    ArrayList<HashSet> lstSets = new ArrayList<>();
    
    public SubTabDEAVennDiag(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            deaData = new DataDEA(project);
            lstDefs = new ArrayList<>();
            timeSeries = project.data.isTimeCourseExpType();
            timeMultiple = project.data.getExperimentType().equals(DataApp.ExperimentType.Time_Course_Multiple);
            deaType = (DataApp.DataType) args.get("dataType");
            deaTypeID = deaType.name();
            panel = TabGeneDataViz.Panels.TRANS.name();
            vdFilepath = deaData.getDEAVennDiagFilepath(deaType);
            String[] resultNames = project.data.getResultNames();
            for(String grpname : resultNames) {
                String filepath = deaData.getDEAClusterMembersFilepath(deaType, grpname);
                lstDefs.add(new DlgVennParams.Params.DataSetDef(grpname, false, filepath));
            }
            hasData = deaData.hasDEAData(deaType);
            switch(deaType) {
                case PROTEIN:
                    deaTypeName = "Proteins";
                    panel = TabGeneDataViz.Panels.PROTEIN.name();
                    break;
                case GENE:
                    deaTypeName = "Genes";
                    break;
                case TRANS:
                    deaTypeName = "Transcripts";
                    break;
                default:
                    result = false;
                    break;
            }
        }
        if(result) {
            if(hasData) {
                hasVennDiag = Files.exists(Paths.get(vdFilepath));
                vdLogFilepath = vdFilepath.replace(DataApp.PNG_EXT, DataApp.LOG_EXT);
            }
            else
                app.ctls.alertInformation("DEA " + deaTypeName + " Venn Diagram", "No DEA data available.");
        }
        if(!result)
            subTabInfo.subTabBase = null;
        
        return result;
    }

    //
    // Internal Functions
    //
    private void showSubTab() {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        tblMembers = (TableView) tabNode.lookup("#tblMembers");
        ObservableList<TableColumn> cols = tblMembers.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Member"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Contained"));
        SubTabBase.addRowNumbersCol(tblMembers);
        rbUnion = (RadioButton) tabNode.lookup("#rbUnion");
        rbInt = (RadioButton) tabNode.lookup("#rbInt");
        rbDiff = (RadioButton) tabNode.lookup("#rbDiff");
        rbUnion.setOnAction(e -> { ObservableList<ToolVennDiag.VennData> ol = getVennData(ToolVennDiag.VennDataType.UNION); tblMembers.setItems(ol);});
        rbInt.setOnAction(e -> { ObservableList<ToolVennDiag.VennData> ol = getVennData(ToolVennDiag.VennDataType.INTERSECT); tblMembers.setItems(ol);});
        rbDiff.setOnAction(e -> { ObservableList<ToolVennDiag.VennData> ol = getVennData(ToolVennDiag.VennDataType.DIFFERENCE); tblMembers.setItems(ol);});

        chk1 = (CheckBox) tabNode.lookup("#chk1");
        chk2 = (CheckBox) tabNode.lookup("#chk2");
        chk3 = (CheckBox) tabNode.lookup("#chk3");
        chk4 = (CheckBox) tabNode.lookup("#chk4");
        chk5 = (CheckBox) tabNode.lookup("#chk5");
        int chk = 1;
        for(DlgVennParams.Params.DataSetDef dsd : lstDefs) {
            ((CheckBox) tabNode.lookup("#chk" + chk)).setText(dsd.name);
            ((CheckBox) tabNode.lookup("#chk" + chk)).setVisible(true);
            ((CheckBox) tabNode.lookup("#chk" + chk)).setOnAction((event) -> { updateTable(); });
            chk++;
        }
        for(; chk <= 5; chk++)
            ((CheckBox) tabNode.lookup("#chk" + chk)).setVisible(false);
        if(lstDefs.size() <= 2) {
            for(chk = 1; chk <= 2; chk++)
                ((CheckBox) tabNode.lookup("#chk" + chk)).setDisable(true);
        }
        grdTable = (GridPane) tabNode.lookup("#grdTable");
        paneMenuTbl = (Pane) tabNode.lookup("#paneMenuTbl");
        if(lstDefs.size() < 5) {
            double off;
            if(lstDefs.size() < 3)
                off = 50.0;
            else
                off = 25.0;
            ObservableList<RowConstraints> lrc = grdTable.getRowConstraints();
            double h = lrc.get(1).getMaxHeight() - off;
            paneMenuTbl.setPrefHeight(h);
            paneMenuTbl.setMaxHeight(h);
            lrc.remove(1); lrc.add(1, new RowConstraints(h));
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            lrc.remove(0); lrc.add(0, rc);
        }
/*        
         // determine time lines for time series
        ArrayList<Integer> lstTimeLines = new ArrayList<>();
        if(timeSeries) {
            //int[] scnt = project.data.getGroupSamples();
            int[] scnt = project.data.getGroupTimes();
            int pt = 0;
            int n = scnt.length;
            for(int i = 0; i < (n - 1); i++) {
                lstTimeLines.add(pt + scnt[i]);
                pt += scnt[i];
            }
        }*/
        
        Image img = new Image("file:" + vdFilepath);
        pi.setVisible(false);
        imgView.fitHeightProperty().bind(paneImgView.heightProperty());
        imgView.fitWidthProperty().bind(paneImgView.widthProperty());
        imgView.setPreserveRatio(true);
        imgView.setImage(img);
        imgView.setVisible(true);
        app.ctls.setupImageExport(imgView, "DEA Significant " + deaTypeName + " Venn Diagram", "tappAS_" + deaTypeName + "_VennDiag.png", null);

        // populate table - always reset to union
        tblMembers.setItems(null);
        rbUnion.setSelected(true);
        ObservableList<ToolVennDiag.VennData> ol = getVennData(ToolVennDiag.VennDataType.UNION);
        tblMembers.setItems(ol);

        // set table popup context menu and include export option
        ContextMenu cm = new ContextMenu();
        addGeneDVItem(cm, tblMembers, panel);
        app.export.setupSimpleTableExport(tblMembers, cm, true, "tappAS_DEA_Significant_" + deaTypeName + ".tsv");
        tblMembers.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblMembers);
        
        // setup export menu
        setupExportMenu();
        setFocusNode(tblMembers, true);
    }
    private boolean isSelected(int setnum) {
        boolean sel = false;
        switch(setnum) {
            case 1:
                sel = chk1.isSelected();
                break;
            case 2:
                sel = chk2.isSelected();
                break;
            case 3:
                sel = chk3.isSelected();
                break;
            case 4:
                sel = chk4.isSelected();
                break;
            case 5:
                sel = chk5.isSelected();
                break;
        }
        return sel;
    }
    @Override
    // differs from subTabBase in ToolVennDiag.VennData "rdata = (ToolVennDiag.VennData) tbl.getItems()" - maybe change to use same
    protected void addGeneDVItem(ContextMenu cm, TableView tbl, String panel) {
        MenuItem item = new MenuItem("Show gene data visualization");
        cm.getItems().add(item);
        item.setOnAction((event) -> {
            // get highlighted row's data and show gene data visualization
            ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
            if(lstIdxs.size() != 1)
                app.ctls.alertInformation("Display Gene Data Visualization", "You have multiple gene rows highlighted.\nHighlight a single row with the gene you want to visualize.");
            else {
                ToolVennDiag.VennData rdata = (ToolVennDiag.VennData) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
                String gene = rdata.getMember().trim();
                String genes[] = gene.split(",");
                if(genes.length > 1) {
                    List<String> lst = Arrays.asList(genes);
                    Collections.sort(lst);
                    ChoiceDialog dlg = new ChoiceDialog(lst.get(0), lst);
                    dlg.setTitle("Gene Data Visualization");
                    dlg.setHeaderText("Select gene to show data visualization for.");
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
        // check if we can provide online references
        if(WebRefs.hasOnlineReferences(project)) {
            item = new MenuItem("Show gene online references in web browser");
            cm.getItems().add(item);
            item.setOnAction((event) -> {
                // get highlighted row's data and show gene data visualization
                ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
                if(lstIdxs.size() != 1)
                    app.ctls.alertInformation("Display Gene Online Reference", "You have multiple gene rows highlighted.\nHighlight a single row with the gene to show online references for.");
                else {
                    ToolVennDiag.VennData rdata = (ToolVennDiag.VennData) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
                    String gene = rdata.getMember().trim();
                    String genes[] = gene.split(",");
                    if(genes.length > 1) {
                        List<String> lst = Arrays.asList(genes);
                        Collections.sort(lst);
                        ChoiceDialog dlg = new ChoiceDialog(lst.get(0), lst);
                        dlg.setTitle("Gene Online Reference");
                        dlg.setHeaderText("Select gene to show online references for");
                        Optional<String> result = dlg.showAndWait();
                        if(result.isPresent()) {
                            gene = result.get();
                            WebRefs.showGeneOnlineReferences(gene, project);
                        }
                    }
                    else
                        WebRefs.showGeneOnlineReferences(gene, project);
                }
            });
        }
    }
    private ObservableList<ToolVennDiag.VennData> getVennData(ToolVennDiag.VennDataType type) {
        ObservableList<ToolVennDiag.VennData> data = FXCollections.observableArrayList();
        // make sure two sets are selected
        int cnt = 0;
        for(int i = 0; i < lstSets.size(); i++) {
            if(isSelected(i+1))
                cnt++;
        }
        if(cnt >= 2) {
            HashMap<String, ArrayList<String>> hm = new HashMap<>();
            int idx = 0;
            for(; idx < lstSets.size(); idx++) {
                if(isSelected(idx + 1))
                    break;
            }
            if(idx < lstSets.size()) {
                Set<String> set = new HashSet<>(lstSets.get(idx));
                for(int i = idx + 1; i < lstSets.size(); i++) {
                    if(isSelected(i+1)) {
                        switch(type) {
                            case INTERSECT:
                                set.retainAll(lstSets.get(i));
                                break;
                            default:
                                set.addAll(lstSets.get(i));
                                break;
                        }
                    }
                }

                // get 'contained in' information
                for(String member : set) {
                    String contained = "";
                    for(int i = 0; i < lstSets.size(); i++) {
                        if(isSelected(i+1)) {
                            if(lstSets.get(i).contains(member))
                                contained += (contained.isEmpty()? "" : "; ") + setNames.get(i);
                        }
                    }
                    data.add(new ToolVennDiag.VennData(member, contained));
                }

                // check if we are doing single set - difference
                if(type.equals(ToolVennDiag.VennDataType.DIFFERENCE)) {
                    ObservableList<ToolVennDiag.VennData> newData = FXCollections.observableArrayList();
                    for(ToolVennDiag.VennData vd : data) {
                        // only move members belonging to a single set
                        if(!vd.contained.get().contains(";"))
                            newData.add(vd);
                    }
                    data = newData;
                }
                FXCollections.sort(data);
            }
        }
        return data;
    }
    private void updateTable() {
        // make sure two sets are selected
        int cnt = 0;
        for(int i = 0; i < lstSets.size(); i++) {
            if(isSelected(i+1))
                cnt++;
        }
        tblMembers.setItems(null);
        if(cnt >= 2) {
            // populate table
            ToolVennDiag.VennDataType vdt = ToolVennDiag.VennDataType.UNION;
            if(rbInt.isSelected())
                vdt = ToolVennDiag.VennDataType.INTERSECT;
            else if(rbDiff.isSelected())
                vdt = ToolVennDiag.VennDataType.DIFFERENCE;
            ObservableList<ToolVennDiag.VennData> ol = getVennData(vdt);
            tblMembers.setItems(ol);
        }
    }
    
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export DEA significant " + deaTypeName + " table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblMembers, true, "tappAS_DEA_Significant" + deaTypeName + ".tsv"); });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Export Venn Diagram");
        item.setOnAction((event) -> { 
            app.export.exportImage("DEA Significant " + deaTypeName + " Venn Diagram", "tappAS_" + deaTypeName + "_VennDiag.png", imgView.getImage());
        });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Export subtab contents image...");
        item.setOnAction((event) -> { 
            WritableImage img = paneContents.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Data Summary Panel", "tappAS_DEA_Significant" + deaTypeName + "_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        paneImgView = (AnchorPane) tabNode.lookup("#paneImgView");
        imgView = (ImageView) tabNode.lookup("#imgView");
        pi = (ProgressIndicator) tabNode.lookup("#piImage");
        if(hasVennDiag) {
            getMembers();
            showSubTab();
        }
        else {
            // get script path and run service/task
            runScriptPath = app.data.getTmpScriptFileFromResource("VennDiagram.R");
            service = new DataLoadService();
            service.initialize();
            service.start();
        }
    }
    private class DataLoadService extends TaskHandler.ServiceExt {
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
                app.logWarning("DEA VennDiag failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("DEA VennDiag - task aborted.");
            app.ctls.alertWarning("Differential Expression Analysis Venn Diagram", "DEA VennDiag failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(Files.exists(Paths.get(vdFilepath))) {
                showSubTab();
            }
            else
                app.ctls.alertWarning("Venn Diagram", "Unable to generate Venn diagram.\nMake sure the VennDiagram R package is installed.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Utils.removeFile(Paths.get(vdFilepath));
                    String logfilepath = vdLogFilepath;

                    // get data sets data
                    String strNames = "";
                    String strCounts = "";
                    lstSets.clear();
                    setNames.clear();
                    for(DlgVennParams.Params.DataSetDef dsd : lstDefs) {
                        HashSet hset = new HashSet();
                        int mbrcnt = 0;
                        String[] mbrs = (dsd.members)? dsd.value.split("\n") : getMembersFromFile(dsd.value);
                        if(mbrs != null && mbrs.length > 0) {
                            for(String mbr : mbrs) {
                                String tmbr = mbr.trim();
                                if(!tmbr.isEmpty()) {
                                    hset.add(tmbr);
                                    mbrcnt++;
                                }
                            }
                            setNames.add(dsd.name.substring(0, Math.min(20, dsd.name.length())));
                            lstSets.add(hset);

                            if(!strCounts.isEmpty())
                                strCounts += ",";
                            strCounts += mbrcnt;
                            if(!strNames.isEmpty())
                                strNames += ";";
                            strNames += dsd.name.substring(0, Math.min(20, dsd.name.length()));
                        }
                        else
                            app.ctls.alertWarning("Venn Diagram", "Unable to get data set members for:\n\n" + dsd.name + "\n");
                    }
                    
                    // make sure we got something to work with
                    int cnt = lstSets.size();                        
                    if(cnt < 2)
                        return null;

                    // n12, n13...
                    for(int i = 0; i < cnt - 1; i++) {
                        for(int j = i + 1; j < cnt; j++) {
                            Set<String> intersection = new HashSet<>(lstSets.get(j));
                            intersection.retainAll(lstSets.get(i));
                            strCounts += "," + intersection.size();
                        }                    
                    }
                    if(cnt >= 3) {
                        // n123, n124...
                        for(int i = 0; i < cnt - 2; i++) {
                            for(int j = i + 1; j < cnt - 1; j++) {
                                for(int k = j + 1; k < cnt; k++) {
                                    Set<String> intersection = new HashSet<>(lstSets.get(k));
                                    intersection.retainAll(lstSets.get(j));
                                    intersection.retainAll(lstSets.get(i));
                                    strCounts += "," + intersection.size();
                                }
                            }                    
                        }
                        if(cnt >= 4) {
                            // n1234, n1235...
                            for(int i = 0; i < cnt - 3; i++) {
                                for(int j = i + 1; j < cnt - 2; j++) {
                                    for(int k = j + 1; k < cnt - 1; k++) {
                                        for(int m = k + 1; m < cnt; m++) {
                                            Set<String> intersection = new HashSet<>(lstSets.get(m));
                                            intersection.retainAll(lstSets.get(k));
                                            intersection.retainAll(lstSets.get(j));
                                            intersection.retainAll(lstSets.get(i));
                                            strCounts += "," + intersection.size();
                                        }
                                    }
                                }                    
                            }
                        }
                        if(cnt == 5) {
                            Set<String> intersection = new HashSet<>(lstSets.get(0));
                            intersection.retainAll(lstSets.get(1));
                            intersection.retainAll(lstSets.get(2));
                            intersection.retainAll(lstSets.get(3));
                            intersection.retainAll(lstSets.get(4));
                            strCounts += "," + intersection.size();
                        }
                    }
                        
                    // setup script arguments
                    List<String> lst = new ArrayList<>();
                    lst.add(app.data.getRscriptFilepath());
                    lst.add(runScriptPath.toString());
                    lst.add(vdFilepath);
                    lst.add("" + cnt);
                    lst.add("\"" + strNames + "\"");
                    lst.add(strCounts);
                    runScript(taskInfo, lst, "VennDiagram " + deaTypeName, logfilepath);

                    // remove script file from temp folder
                    Utils.removeFile(runScriptPath);
                    runScriptPath = null;
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("Venn Diagram " + deaTypeName, task);
            return task;
        }
    }
    private void getMembers() {
        lstSets.clear();
        setNames.clear();
        for(DlgVennParams.Params.DataSetDef dsd : lstDefs) {
            HashSet hset = new HashSet();
            String[] mbrs = (dsd.members)? dsd.value.split("\n") : getMembersFromFile(dsd.value);
            if(mbrs != null && mbrs.length > 0) {
                for(String mbr : mbrs) {
                    String tmbr = mbr.trim();
                    if(!tmbr.isEmpty())
                        hset.add(tmbr);
                }
                setNames.add(dsd.name.substring(0, Math.min(20, dsd.name.length())));
                lstSets.add(hset);
            }
            else
                app.ctls.alertWarning("Venn Diagram", "Unable to get data set members for:\n\n" + dsd.name + "\n");
        }
    }
    private String[] getMembersFromFile(String filepath) {
        ArrayList<String> lstItems = Utils.loadSingleItemListFromFile(filepath, true);
        String[] mbrs = lstItems.toArray(new String[0]);
        return mbrs;
    }
}
