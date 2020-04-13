/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class ToolVennDiag extends DlgBase {
    static int id = 0;
    static public enum VennDataType { UNION, INTERSECT, DIFFERENCE  }
    
    ProgressIndicator pi;
    AnchorPane paneImgView;
    Pane paneMenuDV, paneMenuTbl;
    ImageView imgView;
    TableView tblMembers;
    Button btnHelpDV, btnExportDV, btnRunDV, btnHelpTbl, btnExportTbl;
    RadioButton rbUnion, rbInt, rbDiff;
    CheckBox chk1, chk2, chk3, chk4, chk5;

    Path runScriptPath;
    String imgPath = "";
    int toolId;
    double ratio = 1.0;
    TaskHandler.ServiceExt service = null;
    ArrayList<String> setNames = new ArrayList<>();
    ArrayList<HashSet> lstSets = new ArrayList<>();
    DlgVennParams.Params params = new DlgVennParams.Params();
        
    public ToolVennDiag(Project project, Window window) {
        super(project, window);
        toolId = ++id;
    }
    public HashMap<String, String> showAndWait() {
        if(createToolDialog("ToolVennDiag.fxml", "Venn Diagrams",  null)) {
            // get control objects
            paneMenuDV = (Pane) scene.lookup("#paneMenuDV");
            paneMenuTbl = (Pane) scene.lookup("#paneMenuTbl");            
            paneImgView = (AnchorPane) scene.lookup("#paneImgView");
            imgView = (ImageView) scene.lookup("#imgView");
            pi = (ProgressIndicator) scene.lookup("#piImage");
            tblMembers = (TableView) scene.lookup("#tblMembers");
            rbUnion = (RadioButton) scene.lookup("#rbUnion");
            rbInt = (RadioButton) scene.lookup("#rbInt");
            rbDiff = (RadioButton) scene.lookup("#rbDiff");
            chk1 = (CheckBox) scene.lookup("#chk1");
            chk2 = (CheckBox) scene.lookup("#chk2");
            chk3 = (CheckBox) scene.lookup("#chk3");
            chk4 = (CheckBox) scene.lookup("#chk4");
            chk5 = (CheckBox) scene.lookup("#chk5");
            
            // setup listeners and bindings
            paneImgView.widthProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                double h = (double)newValue * ratio;
                System.out.println("w chg: " + (double)newValue + ", h: " + h + ", paneH: " + paneImgView.getHeight());
                if(h <= (paneImgView.getHeight() - 5.0)) {
                    imgView.setFitHeight(h);
                    imgView.setFitWidth((double)newValue);
                }
                else {
                    h = paneImgView.getHeight() - 5.0;
                    imgView.setFitWidth(h / ratio);
                    imgView.setFitHeight(h);
                }
                double w = imgView.getFitWidth();
                h = imgView.getFitHeight();
                double ph = paneImgView.getHeight();
                double pw = paneImgView.getWidth();
                if(h < (ph - 5.0))
                    imgView.setLayoutY((ph - 5.0 - h) /2);
                else
                    imgView.setLayoutY(1.0);
                if(w < pw)
                    imgView.setLayoutX((pw - w) /2);
                else
                    imgView.setLayoutX(0);
                double ipw = pi.getWidth();
                pi.setLayoutX((pw - ipw) /2);
                double iph = pi.getHeight();
                pi.setLayoutY((ph - iph) /2);
                System.out.println("imgChart fw: " + w + ", fh:" + h + ", pw: " + pw + ", ph: " + ph);
            });
            paneImgView.heightProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                // use width to get max size
                double w = ((double)newValue - 5.0)/ ratio;
                System.out.println("h chg: " + (double)newValue + ", w: " + w + ", paneW: " + paneImgView.getWidth());
                if(w < paneImgView.getWidth()) {
                    imgView.setFitHeight(((double)newValue - 5.0));
                    imgView.setFitWidth(w);
                }
                else {
                    w = paneImgView.getWidth();
                    imgView.setFitWidth(w);
                    imgView.setFitHeight(w * ratio);
                }
                w = imgView.getFitWidth();
                double h = imgView.getFitHeight();
                double ph = paneImgView.getHeight();
                double pw = paneImgView.getWidth();
                if(h < (ph - 5.0))
                    imgView.setLayoutY((ph - 5.0 - h) /2);
                else
                    imgView.setLayoutY(1.0);
                if(w < pw)
                    imgView.setLayoutX((pw - w) /2);
                else
                    imgView.setLayoutX(0);
                double ipw = pi.getWidth();
                pi.setLayoutX((pw - ipw) /2);
                double iph = pi.getHeight();
                pi.setLayoutY((ph - iph) /2);
                System.out.println("imgChart fw: " + w + ", fh:" + h + ", pw: " + pw + ", ph: " + ph);
            });
            ObservableList<TableColumn> cols = tblMembers.getColumns();
            cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Member"));
            cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Contained"));
            SubTabBase.addRowNumbersCol(tblMembers);
            rbUnion.setOnAction(e -> { ObservableList<VennData> ol = getVennData(VennDataType.UNION); tblMembers.setItems(ol);});
            rbInt.setOnAction(e -> { ObservableList<VennData> ol = getVennData(VennDataType.INTERSECT); tblMembers.setItems(ol);});
            rbDiff.setOnAction(e -> { ObservableList<VennData> ol = getVennData(VennDataType.DIFFERENCE); tblMembers.setItems(ol);});

            double yoffset = 3.0;
            btnExportDV = addImgButton(paneMenuDV, "export.png", "Export Venn diagram image...", yoffset, 32);
            btnExportDV.setOnAction((event) -> { onButtonExportDV(); });
            yoffset += 36;
            btnRunDV = addImgButton(paneMenuDV, "run.png", "Redefine Venn diagram...", yoffset, 32);
            btnRunDV.setOnAction((event) -> { onButtonRun(); });
            yoffset += 36;
            btnHelpDV = addImgButton(paneMenuDV, "help.png", "Help", yoffset, 32);
            btnHelpDV.setOnAction((event) -> {
                DlgHelp dlg = new DlgHelp();
                dlg.show(title, "HelpToolVennDiag.html", Tappas.getWindow());
            });
            
            yoffset = 3.0;
            btnExportTbl = addImgButton(paneMenuTbl, "export.png", "Export table data...", yoffset, 32);
            btnExportTbl.setOnAction((event) -> { onButtonExportTable(); });
            yoffset += 36;
            btnHelpTbl = addImgButton(paneMenuTbl, "help.png", "Help", yoffset, 32);
            btnHelpTbl.setOnAction((event) -> {
                DlgHelp dlg = new DlgHelp();
                dlg.show(title, "HelpToolVennDiag.html", Tappas.getWindow());
            });
            chk1.setOnAction((event) -> { updateTable(); });
            chk2.setOnAction((event) -> { updateTable(); });
            chk3.setOnAction((event) -> { updateTable(); });
            chk4.setOnAction((event) -> { updateTable(); });
            chk5.setOnAction((event) -> { updateTable(); });

            // process dialog
            DlgVennParams dlg = new DlgVennParams(project, Tappas.getWindow());
            DlgVennParams.Params results = dlg.showAndWait(params);
            if(results != null) {
                params = results;
                dialog.setOnShown(e -> { runScriptThread();} );
                dialog.showAndWait();
            }
        }
        return null;
    }
    
    //
    // Event Handlers
    //
    private void onButtonRun() {
        DlgVennParams dlg = new DlgVennParams(project, Tappas.getWindow());
        DlgVennParams.Params results = dlg.showAndWait(params);
        if(results != null) {
            // disable all!!!
            runScriptThread();
        }
    }
    private void onButtonExportDV() {
        if(!imgPath.isEmpty()) {
            String filename = "tappAS_VennDiagram.png";
            app.export.exportImageFile("Venn Diagram", filename, imgPath);
        }
    }
    private void onButtonExportTable() {
        String tblType = "union";
        if(rbInt.isSelected())
            tblType = "intersection";
        else if(rbDiff.isSelected())
            tblType = "differences";
        String filename = "tappAS_VennDiagram_" + tblType + ".tsv";
        app.export.exportTableDataToFile(tblMembers, true, filename);
    }
    
    //
    // Internal Functions
    //
    private Button addImgButton(Pane pane, String imgName, String tooltip, double yoffset, double size) {
        Image img = new Image(getClass().getResourceAsStream("/tappas/images/" + imgName));
        Button btn = new Button();
        btn.setGraphicTextGap(0.0);
        btn.setStyle("-fx-padding:0px;");
        btn.setPrefSize(size, size);
        ImageView iv = new ImageView(img);
        iv.setFitHeight(20.0);
        iv.setFitWidth(20.0);
        btn.setGraphic(iv);
        btn.getStyleClass().add("buttonHelpHMenu");
        btn.setLayoutX(3.0);
        btn.setLayoutY(yoffset);
        btn.setTooltip(new Tooltip(tooltip));
        pane.getChildren().add(btn);
        return btn;
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
    private ObservableList<VennData> getVennData(VennDataType type) {
        ObservableList<VennData> data = FXCollections.observableArrayList();
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
                    data.add(new VennData(member, contained));
                }

                // check if we are doing single set - difference
                if(type.equals(VennDataType.DIFFERENCE)) {
                    ObservableList<VennData> newData = FXCollections.observableArrayList();
                    for(VennData vd : data) {
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
    private void showVennDiagram(String filepath) {
        Image img = new Image("file:" + filepath);
        imgPath = filepath;
        pi.setVisible(false);
        imgView.setImage(img);
        imgView.setPreserveRatio(true);
        imgView.setVisible(true);
        app.ctls.setupImageExport(imgView, "Venn Diagram", "tappAS_VennDiag.png", null);
        
        // populate table - always reset to union
        tblMembers.setItems(null);
        rbUnion.setSelected(true);
        ObservableList<VennData> ol = getVennData(VennDataType.UNION);
        tblMembers.setItems(ol);
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
            VennDataType vdt = VennDataType.UNION;
            if(rbInt.isSelected())
                vdt = VennDataType.INTERSECT;
            else if(rbDiff.isSelected())
                vdt = VennDataType.DIFFERENCE;
            ObservableList<VennData> ol = getVennData(vdt);
            tblMembers.setItems(ol);
        }
    }
    
    //
    // Script service/task
    //
    private void runScriptThread() {
        // default dialog size or min w/h - doesn't work here!
        dialog.getDialogPane().setMinWidth(500.0);
        dialog.getDialogPane().setMinHeight(500.0);
        dialog.getDialogPane().getScene().getWindow().sizeToScene();

        // get script path and run service/task
        runScriptPath = app.data.getTmpScriptFileFromResource("VennDiagram.R");
        service = new ScriptService();
        service.initialize();
        service.start();
    }
    private class ScriptService extends TaskHandler.ServiceExt {
        String resultsfilepath;
        
        @Override
        public void initialize() {
            pi.setProgress(-1);
            pi.setVisible(true);
        }
        @Override
        protected void onRunning() {
        }
        @Override
        protected void onStopped() {
            pi.setProgress(0);
            pi.setVisible(false);
        }
        @Override
        protected void onFailed() {
            java.lang.Throwable e = getException();
            if(e != null)
                app.logWarning("Venn diagram failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("Venn diagram failed - task aborted.");
            app.ctls.alertWarning("Venn diagram", "Venn diagram failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(Files.exists(Paths.get(resultsfilepath))) {
                int cnt = lstSets.size();
                chk3.setDisable(cnt < 3);
                chk4.setDisable(cnt < 4);
                chk5.setDisable(cnt < 5);
                chk3.setSelected(cnt >= 3);
                chk4.setSelected(cnt >= 4);
                chk5.setSelected(cnt >= 5);
                chk1.setText(setNames.get(0));
                chk2.setText(setNames.get(1));
                chk3.setText((cnt < 3)? "N/A" : setNames.get(2));
                chk4.setText((cnt < 4)? "N/A" : setNames.get(3));
                chk5.setText((cnt < 5)? "N/A" : setNames.get(4));
                showVennDiagram(resultsfilepath);
            }
            else
                app.ctls.alertWarning("Venn Diagram", "Unable to generate Venn diagram.\nMake sure the VennDiagram R package is installed.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    resultsfilepath = project.data.getProjectToolJpgFilepath("VennDiagram_" + toolId);
                    Utils.removeFile(Paths.get(resultsfilepath));
                    String logfilepath = project.data.getProjectToolLogFilepath("VennDiagram_" + toolId);

                    // get data sets data
                    String strNames = "";
                    String strCounts = "";
                    lstSets.clear();
                    setNames.clear();
                    for(DlgVennParams.Params.DataSetDef dsd : params.lstDefs) {
                        HashSet hset = new HashSet();
                        String[] mbrs;
                        int mbrcnt = 0;
                        if(dsd.members)
                            mbrs = dsd.value.split("\n");
                        else
                            mbrs = getMembersFromFile(dsd.value);
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
                    lst.add(resultsfilepath);
                    lst.add("" + cnt);
                    lst.add("\"" + strNames + "\"");
                    lst.add(strCounts);
                    runScript(taskInfo, lst, "VennDiagram " + toolId, logfilepath);

                    // remove script file from temp folder
                    Utils.removeFile(runScriptPath);
                    runScriptPath = null;
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("Venn Diagram " + toolId, task);
            return task;
        }
    }
    private String[] getMembersFromFile(String filepath) {
        ArrayList<String> lstItems = Utils.loadSingleItemListFromFile(filepath, false);
        String[] mbrs = lstItems.toArray(new String[0]);
        return mbrs;
    }
    
    //
    // Data Classes
    //
    static public class VennData implements Comparable<VennData> {
        public SimpleStringProperty member;
        public SimpleStringProperty contained;
        public VennData(String member, String contained) {
            this.member = new SimpleStringProperty(member);
            this.contained = new SimpleStringProperty(contained);
        }
        public String getMember() { return member.get(); }
        public String getContained() { return contained.get(); }
        
        @Override
        public int compareTo(VennData td) {
            return (member.get().compareTo(td.member.get()));
        }
    }
}
