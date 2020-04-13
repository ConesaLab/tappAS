/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import tappas.DataApp.EnumData;
import tappas.DbProject.AFStatsData;
import tappas.DrillDownFDAData.FDADrillDownData;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgBase extends AppObject {
    Dialog<HashMap> dialog;
    Scene scene;
    Label lblMsg = null;
    ButtonType btnOK, btnHelp, btnCancel;

    Window window;
    String title;
    
    public DlgBase(Project project, Window window) {
        super(project, null);
        this.window = window;
    }
    public void showDlgMsg(String msg) {
        if(lblMsg != null)
            lblMsg.setText(msg);
        if(!msg.isEmpty())
            app.ctls.alertInformation(title, msg);
    }
    public boolean createFloatingWindow(Node node, String title) {
        return createDialogInternal("", node, title, true, true, false, "");
    }
    public boolean createToolDialog(String fxml, String title, String helpHtml) {
        return createDialogInternal(fxml, null, title, true, true, false, helpHtml);
    }
    public boolean createDialog(String fxml, String title, boolean btnOKFlg, boolean btnCancelFlg, String helpHtml) {
        return createDialogInternal(fxml, null, title, false, btnOKFlg, btnCancelFlg, helpHtml);
    }
    public boolean createDialog(String fxml, String title, boolean btnCancelFlg, String helpHtml) {
        return createDialogInternal(fxml, null, title, false, true, btnCancelFlg, helpHtml);
    }

    //
    // Protected Functions
    //

    protected void onButtonExport() {
        // override
    }
    protected void onButtonExportChart() {
        // override
    }
    protected boolean isValidNumInt(String strnum) {
        boolean valid = false;
        try {
            int num = Integer.parseUnsignedInt(strnum.trim());
            valid = true;
        } catch(Exception e) {
            System.out.println("Not a valid integer value.");
        }
        return valid;
    }
    protected boolean isValidNumDbl(String strnum) {
        boolean valid = false;
        try {
            double num = Double.parseDouble(strnum.trim());
            valid = true;
        } catch(Exception e) {
            System.out.println("Not a valid double value.");
        }
        return valid;
    }
    protected void populateAnnotationFeatures(TreeView tv, HashMap<String, HashMap<String, Object>> hmSels) {
        tv.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
        TreeItem<String> rootItem = new TreeItem<> ("Annotation File");
        rootItem.setExpanded(true);
        AFStatsData sd;
        HashMap<String, Object> hm;
        HashMap<String, HashMap<String, AFStatsData>> hmStats = project.data.getAFStats();
        
        for (String source : hmStats.keySet()) {
                hm = null;
                CheckBoxTreeItem<String> item = new CheckBoxTreeItem<> (source);
                item.selectedProperty().addListener((obsValue, oldValue, newValue) -> annotationFeaturesOnSelectionChanged());
                if(hmSels.containsKey(source)) {
                    hm = hmSels.get(source);
                    if(hm.size() > 0 && hm.size() != hmStats.get(source).size())
                        item.setIndeterminate(true);
                    else
                        item.setSelected(true);
                }
                boolean addflg = false;
                HashMap<String, AFStatsData> hmFeatures = hmStats.get(source);
                for (String feature : hmFeatures.keySet()) {
                    if(!project.data.isRsvdFeature(source, feature)) {
                        sd = hmFeatures.get(feature);
                        if(sd.idCount > 0) {
                            addflg = true;
                            CheckBoxTreeItem<String> subitem = new CheckBoxTreeItem<> (feature);
                            subitem.selectedProperty().addListener((observableValue, oldnum, newnum) -> annotationFeaturesOnSelectionChanged());
                            if(hm != null && (hm.containsKey(feature) || hm.isEmpty()))
                                subitem.setSelected(true);
                            item.getChildren().add(subitem);
                        }
                    }
                }
                if(addflg)
                    rootItem.getChildren().add(item);
        }
        tv.setShowRoot(false);
        tv.setRoot(rootItem);
    }
    
    protected void populateAnnotationFeaturesPositional(TreeView tv, HashMap<String, HashMap<String, Object>> hmSels) {
        tv.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
        TreeItem<String> rootItem = new TreeItem<> ("Annotation File");
        rootItem.setExpanded(true);
        AFStatsData sd;
        HashMap<String, Object> hm;
        HashMap<String, HashMap<String, AFStatsData>> hmStats = project.data.getAFStats();
        HashMap<String, ArrayList<String>> hmPositional = project.data.getAFStatsPositional();
        
        for (String source : hmPositional.keySet()) {
            if(!hmStats.containsKey(source))
                continue;
            hm = null;
            CheckBoxTreeItem<String> item = new CheckBoxTreeItem<> (source);
            item.selectedProperty().addListener((obsValue, oldValue, newValue) -> annotationFeaturesOnSelectionChanged());
            if(hmSels.containsKey(source)) {
                hm = hmSels.get(source);
                if(hm.size() > 0 && hm.size() != hmStats.get(source).size())
                    item.setIndeterminate(true);
                else
                    item.setSelected(true);
            }
            boolean addflg = false;
            HashMap<String, AFStatsData> hmFeatures = hmStats.get(source);
            for (String feature : hmFeatures.keySet()) {
                //Just get Features that appear in hmPositional
                if(hmPositional.get(source).contains(feature) && !project.data.isRsvdFeature(source, feature)) {
                    sd = hmFeatures.get(feature);
                    if(sd.idCount > 0) {
                        addflg = true;
                        CheckBoxTreeItem<String> subitem = new CheckBoxTreeItem<> (feature);
                        subitem.selectedProperty().addListener((observableValue, oldnum, newnum) -> annotationFeaturesOnSelectionChanged());
                        if(hm != null && (hm.containsKey(feature) || hm.isEmpty()))
                            subitem.setSelected(true);
                        item.getChildren().add(subitem);
                    }
                }
            }
            if(addflg)
                rootItem.getChildren().add(item);
        }
        tv.setShowRoot(false);
        tv.setRoot(rootItem);
    }
    
    protected void populateAnnotationFeaturesPresence(TreeView tv, HashMap<String, HashMap<String, Object>> hmSels) {
        tv.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
        TreeItem<String> rootItem = new TreeItem<> ("Annotation File");
        rootItem.setExpanded(true);
        AFStatsData sd;
        HashMap<String, Object> hm;
        HashMap<String, HashMap<String, AFStatsData>> hmStats = project.data.getAFStats();
        HashMap<String, ArrayList<String>> hmPresence = project.data.getAFStatsPresence();
        
        for (String source : hmPresence.keySet()) {
            if(!hmStats.containsKey(source))
                continue;
            hm = null;
            CheckBoxTreeItem<String> item = new CheckBoxTreeItem<> (source);
            item.selectedProperty().addListener((obsValue, oldValue, newValue) -> annotationFeaturesOnSelectionChanged());
            if(hmSels.containsKey(source)) {
                hm = hmSels.get(source);
                if(hm.size() > 0 && hm.size() != hmStats.get(source).size())
                    item.setIndeterminate(true);
                else
                    item.setSelected(true);
            }
            boolean addflg = false;
            HashMap<String, AFStatsData> hmFeatures = hmStats.get(source);
            for (String feature : hmFeatures.keySet()) {
                //Just get Features that appear in hmPresence
                if(hmPresence.get(source).contains(feature) && !project.data.isRsvdFeature(source, feature)) {
                    sd = hmFeatures.get(feature);
                    if(sd.idCount > 0) {
                        addflg = true;
                        CheckBoxTreeItem<String> subitem = new CheckBoxTreeItem<> (feature);
                        subitem.selectedProperty().addListener((observableValue, oldnum, newnum) -> annotationFeaturesOnSelectionChanged());
                        if(hm != null && (hm.containsKey(feature) || hm.isEmpty()))
                            subitem.setSelected(true);
                        item.getChildren().add(subitem);
                    }
                }
            }
            if(addflg)
                rootItem.getChildren().add(item);
        }
        tv.setShowRoot(false);
        tv.setRoot(rootItem);
    }
    
    protected void annotationFeaturesOnSelectionChanged() {
        // override
    }
    protected void setProjectName() {
        if(scene != null) {
            AnchorPane paneProject = (AnchorPane) scene.lookup("#paneProject");
            paneProject.getStyleClass().add("project" + project.getProjectNumber());
            Label lblProjectName = (Label) scene.lookup("#lblProjectName");
            lblProjectName.setText("Project \"" + project.getProjectName()+ "\"");
        }
    }
    
//    protected void setProjectNameScrollPane() {
//        if(scene != null) {
//            ScrollPane scPane = (ScrollPane) scene.lookup("#scPane");
//            AnchorPane acPane = (AnchorPane) scPane.getContent().lookup("#acPane");
//            
//            AnchorPane paneProject = (AnchorPane) acPane.lookup("#paneProject");
//            paneProject.getStyleClass().add("project" + project.getProjectNumber());
//            Label lblProjectName = (Label) acPane.lookup("#lblProjectName");
//            lblProjectName.setText("Project \"" + project.getProjectName()+ "\"");
//        }
//    }
    // run script and write output to log file
    protected void runScript(TaskHandler.TaskInfo taskInfo, List<String> lst, String name, String logFilepath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(lst);
            System.out.println(lst);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            taskInfo.process = process;

            // monitor process output
            Writer writer = null;
            try {
                String line, dspline;
                LocalDate date = LocalDate.now();
                LocalTime time = LocalTime.now();
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(logFilepath, true), "utf-8"));
                dspline = name + " script is running...\n";
                writer.write(dspline);
                BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while ((line = input.readLine()) != null) {
                    time = LocalTime.now();
                    dspline = time.toString() + " " + line + "\n";
                    writer.write(dspline);
                }
                try { input.close(); } catch(Exception e) { }
                int ev = process.waitFor();
                dspline = time.toString() + " " + name + " script ended. Exit value: " + ev + "\n";
                writer.write(dspline);
            } catch(Exception e) {
                app.logError("Unable to capture " + name + " process output: " + e.getMessage());
            } finally {
               try {writer.close();} catch (Exception e) {;}
            }
        } catch(Exception e) {
            app.logError("Unable to run " + name + " script: " + e.getMessage());
        }
    }
    // override if needed
    protected void onClose() {
        dialog.close();        
    }
    
    protected void addGeneDVItemFDA(ContextMenu cm, TableView tbl, String panel) {
        MenuItem item = new MenuItem("Show gene data visualization");
        cm.getItems().add(item);
        item.setOnAction((event) -> {
            // get highlighted row's data and show gene data visualization
            ObservableList<Integer> lstIdxs = tbl.getSelectionModel().getSelectedIndices();
            if(lstIdxs.size() != 1)
                app.ctls.alertInformation("Display Gene Data Visualization", "You have multiple gene rows highlighted.\nHighlight a single row with the gene you want to visualize.");
            else {
                FDADrillDownData a = (FDADrillDownData) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
                String gene = a.getGene().trim();
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
                        showGeneDataVisualizationFDA(gene, panel);
                    }
                }
                else
                    showGeneDataVisualizationFDA(gene, panel);
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
                    DataApp.SelectionDataResults rdata = (DataApp.SelectionDataResults) tbl.getItems().get((tbl.getSelectionModel().getSelectedIndex()));
                    String gene = rdata.getGene().trim();
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
    
    public void showGeneDataVisualizationFDA(String gene, String panel) {
        HashMap<String, Object> hmArgs = new HashMap<>();
        hmArgs.put("panels", panel);
        app.tabs.openTabGeneDataViz(project, project.getDef().id, gene, project.data.getResultsGeneTrans().get(gene), "Project '" + project.getDef().name + "'", hmArgs);
    }
    
    //
    // Internal Functions
    //
    
    private boolean createDialogInternal(String fxml, Node node, String title, boolean tool, boolean btnOKFlg, boolean btnCancelFlg, String helpHtml) {
        boolean result = false;
        this.title = title;
        dialog = new Dialog();
        dialog.setTitle(title);
        if(tool) {
            // using unified style in Windows 10 results in the dialog content being blank (not shown)
            // change if issue is resolved or workaround found
            dialog.initStyle(Utils.isWindowsOS()? StageStyle.DECORATED : StageStyle.UNIFIED);
            dialog.initModality(Modality.NONE);
            dialog.setResizable(true);
            dialog.setOnCloseRequest(e -> { onClose(); });
        }
        else {
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setResizable(false);
            dialog.initStyle(StageStyle.DECORATED);
        }
        dialog.initOwner(window);
        try {
            if(node == null)
                node = (Node) FXMLLoader.load(app.ctlr.fxdc.getClass().getResource("/tappas/dialogs/" + fxml));
            else {
                String url = getClass().getResource("/tappas/Style.css").toExternalForm();
                dialog.getDialogPane().getStylesheets().add(url);
                dialog.getDialogPane().getStyleClass().add("float-dialog-pane");
            }
            dialog.getDialogPane().setContent(node);
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
            if(btnOKFlg) {
                btnOK = new ButtonType((tool? "Close" : "OK"), ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().add(btnOK); 
            }
            if(btnCancelFlg) {
                btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                dialog.getDialogPane().getButtonTypes().add(btnCancel); 
            }
            if(helpHtml != null && !helpHtml.isEmpty()) {
                btnHelp = new ButtonType("Help", ButtonBar.ButtonData.HELP);
                dialog.getDialogPane().getButtonTypes().add(btnHelp);
                System.out.println("bt: " + dialog.getDialogPane().getButtonTypes());
                final Button btHelp = (Button) dialog.getDialogPane().lookupButton(btnHelp);
                System.out.println("hlp: " + btHelp);
                if(btHelp != null) {
                    btHelp.addEventFilter(ActionEvent.ACTION, event -> {
                        DlgHelp dlg = new DlgHelp();
                        dlg.show(title, helpHtml, Tappas.getWindow());
                        event.consume();
                    });
                }
            }
            scene = dialog.getDialogPane().getContent().getScene();
            lblMsg = (Label) scene.lookup("#lblMsg");
            result = true;
        } catch(Exception e) {
            logger.logError("Unable to create dialog: " + e.getMessage());
        }
        return result;
    }

    
    //
    // Data Classes
    //
    public static class DlgParams {
        final static double MIN_PVAL_THRESHOLD = 0.0;
        final static double MAX_PVAL_THRESHOLD = 1.0;
        final static double MAX_PVAL_FC = 10000.0;
        final static double MIN_R2CUTOFF = 0.4;
        final static double MAX_R2CUTOFF = 0.8;
        public static enum YesNo {
            NO, YES
        }
        protected static final List<EnumData> lstYesNo = Arrays.asList(
            new EnumData(YesNo.NO.name(), "No"),
            new EnumData(YesNo.YES.name(), "Yes")
        );

        public DlgParams() {
        }
        public DlgParams(HashMap<String, String> hmParams) {
        }
        public HashMap<String, String> getParams() {
            // override
            HashMap<String, String> hm = new HashMap<>();
            return hm;
        }
        public boolean save(String filepath) {
            return Utils.saveParams(getParams(), filepath);
        }

        protected static int getYNIndex(String ynID) {
            int idx = 0;
            for(EnumData yn : lstYesNo) {
                if(yn.id.equals(ynID))
                    break;
                idx++;
            }
            if(idx >= lstYesNo.size())
                idx = 0;
            return idx;
        }
    }
}
