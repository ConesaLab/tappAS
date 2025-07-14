/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgGSEAFilterFeatures extends DlgBase {
    TextField txtN, txtFile;
    RadioButton rbTopN, rbManual, rbList;
    Label lblTV, lblN;
    TreeView tvFeatures;
    Hyperlink lnkClearAll, lnkSetAll;
    Button btnFile;

    Params params;

    public DlgGSEAFilterFeatures(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(Params dfltParams, HashMap<String, HashMap<String, Object>> hmFeatureIDs) {
        if(createDialog("GSEAFilterFeatures.fxml", "GSEA Enriched Features Display Filter", true, "Help_Dlg_GSEA_FilterFeatures.html")) {
            setProjectName();
            params = dfltParams;
            
            // get control objects
            rbTopN = (RadioButton) scene.lookup("#rbTopN");
            rbManual = (RadioButton) scene.lookup("#rbManual");
            txtN = (TextField) scene.lookup("#txtN");
            lblN = (Label) scene.lookup("#lblN");
            lblTV = (Label) scene.lookup("#lblTV");
            lnkClearAll = (Hyperlink) scene.lookup("#lnkClearAll");
            lnkSetAll = (Hyperlink) scene.lookup("#lnkSetAll");
            tvFeatures = (TreeView) scene.lookup("#tvFeatures");

            //list
            txtFile = (TextField) scene.lookup("#txtFile");
            rbList = (RadioButton) scene.lookup("#rbList");
            btnFile = (Button) scene.lookup("#btnFile");
            
            // setup listeners and bindings
            txtFile.setDisable(true);
            btnFile.setDisable(true);
            lblTV.setDisable(true);
            tvFeatures.setDisable(true);
            lnkClearAll.setDisable(true);
            lnkSetAll.setDisable(true);
            
            rbList.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                if(newValue){
                    txtN.setDisable(true);
                    lblN.setDisable(true);
                    lblTV.setDisable(true);
                    tvFeatures.setDisable(true);
                    lnkClearAll.setDisable(true);
                    lnkSetAll.setDisable(true);
                    
                    txtFile.setDisable(false);
                    btnFile.setDisable(false);
                }
            });
            
            rbManual.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                if(newValue){
                    txtN.setDisable(true);
                    lblN.setDisable(true);
                    txtFile.setDisable(true);
                    btnFile.setDisable(true);
                    
                    lblTV.setDisable(false);
                    tvFeatures.setDisable(false);
                    lnkClearAll.setDisable(false);
                    lnkSetAll.setDisable(false);
                }
            });
            
            rbTopN.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
                if(newValue){
                    txtFile.setDisable(true);
                    btnFile.setDisable(true);
                    lblTV.setDisable(true);
                    tvFeatures.setDisable(true);
                    lnkClearAll.setDisable(true);
                    lnkSetAll.setDisable(true);
                    
                    txtN.setDisable(false);
                    lblN.setDisable(false);
                }
            });
            
            // setup button actions
            lnkSetAll.setOnAction((event) -> { setAllFeatures(); });
            lnkClearAll.setOnAction((event) -> { clearAllFeatures(); });
            
            // populate dialog
            lblTV.setText("Select '" + params.source + "' features to display:");
            if(params.type.equals(Params.Type.TOPN)) {
                rbTopN.setSelected(true);
                txtN.setText("" + params.n);
                // always clear if manual mode is not selected
                params.hmFeatureIDs.clear();
            }
            else
                rbManual.setSelected(true);
            populateAnnotationFeatureIDs(tvFeatures, hmFeatureIDs, params.hmFeatureIDs);
            
            // setup dialog event handlers
            btnFile.setOnAction((event) -> { getFile(txtFile); });
            
            dialog.setOnCloseRequest((DialogEvent event) -> {
                if(dialog.getResult() != null && dialog.getResult().containsKey("ERRMSG")) {
                    showDlgMsg((String)dialog.getResult().get("ERRMSG"));
                    dialog.setResult(null);
                    event.consume();
                }
            });
            dialog.setResultConverter((ButtonType b) -> {
                HashMap<String, String> params1 = null;
                System.out.println(b.getButtonData().toString());
                if (b.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                    params1 = validate(dialog);
                }
                return params1;
            });

            // process dialog
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return(new Params(result.get()));
        }
        return null;
    }
    
    //
    // Internal Functions
    //
    private void getFile(TextField txt) {
        File f = getFileName();
        if(f != null)
            txt.setText(f.getPath());
    }
    
    private File getFileName() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(("Select Feature list File"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Feature list files", "*.txt", "*.tsv"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        String expFolder = app.userPrefs.getImportFeaturesFolder();
        if(!expFolder.isEmpty()) {
            File f = new File(expFolder);
            if(f.exists() && f.isDirectory())
                fileChooser.setInitialDirectory(f);
            else
                app.userPrefs.setImportFeaturesFolder("");
        }
        File selectedFile = fileChooser.showOpenDialog(dialog.getOwner());
        if(selectedFile!=null)
            app.userPrefs.setImportFeaturesFolder(selectedFile.getParent());
        return selectedFile;
    }
    
    private void setAllFeatures() {
        TreeItem<String> rootItem = tvFeatures.getRoot();
        ObservableList<TreeItem<String>> lst = rootItem.getChildren();
        for(TreeItem ti : lst) {
            CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) ti;
            ObservableList<TreeItem<String>> sublst = item.getChildren();
            for(TreeItem subti : sublst) {
                CheckBoxTreeItem<String> subitem = (CheckBoxTreeItem<String>) subti;
                subitem.setSelected(true);
            }
            item.setSelected(true);
        }
    }
    private void clearAllFeatures() {
        TreeItem<String> rootItem = tvFeatures.getRoot();
        ObservableList<TreeItem<String>> lst = rootItem.getChildren();
        for(TreeItem ti : lst) {
            CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) ti;
            ObservableList<TreeItem<String>> sublst = item.getChildren();
            for(TreeItem subti : sublst) {
                CheckBoxTreeItem<String> subitem = (CheckBoxTreeItem<String>) subti;
                subitem.setSelected(false);
            }
            item.setSelected(false);
        }
    }
    protected void populateAnnotationFeatureIDs(TreeView tv, HashMap<String, HashMap<String, Object>> hmFeatureIDs,
                                                HashMap<String, HashMap<String, Object>> hmSelFeatureIDs) {
        tv.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
        TreeItem<String> rootItem = new TreeItem<> ("Annotation Features");
        rootItem.setExpanded(true);
        HashMap<String, Object> hmIDs;
        ArrayList<String> lstFeatures = new ArrayList<>();
        lstFeatures.addAll(hmFeatureIDs.keySet());
        Collections.sort(lstFeatures, String.CASE_INSENSITIVE_ORDER);
        for (String feature : lstFeatures) {
            hmIDs = hmFeatureIDs.get(feature);
            ArrayList<FeatureDataObj> lstIDs = new ArrayList<>();
            for(String id : hmIDs.keySet())
                lstIDs.add(new FeatureDataObj(id, (double)hmIDs.get(id)));
            Collections.sort(lstIDs);
            CheckBoxTreeItem<String> subitem = new CheckBoxTreeItem<> (feature);
            HashMap<String, Object> hmSels = null;
            if(hmSelFeatureIDs.containsKey(feature)) {
                hmSels = hmSelFeatureIDs.get(feature);
                if(hmSels.isEmpty())
                    subitem.setSelected(true);
                else
                    subitem.setIndeterminate(true);
            }
            subitem.selectedProperty().addListener((observableValue, oldnum, newnum) -> annotationFeaturesOnSelectionChanged());
            for(FeatureDataObj fd : lstIDs) {
                CheckBoxTreeItem<String> subsubitem = new CheckBoxTreeItem<> (fd.id);
                if(hmSels != null && (hmSels.isEmpty() || hmSels.containsKey(fd.id)))
                    subsubitem.setSelected(true);
                subsubitem.selectedProperty().addListener((observableValue, oldnum, newnum) -> annotationFeaturesOnSelectionChanged());
                subitem.getChildren().add(subsubitem);
            }
            rootItem.getChildren().add(subitem);
            rootItem.setExpanded(true);
        }
        tv.setShowRoot(false);
        tv.setRoot(rootItem);
    }

    //
    // Dialog Validation
    //
    private HashMap<String, String> validate(Dialog dialog) {
        String errmsg = "";
        HashMap<String, String> hmResults = new HashMap<>();
        hmResults.put(Params.TYPE_PARAM, rbTopN.isSelected()? Params.Type.TOPN.name() : rbManual.isSelected()? Params.Type.MANUAL.name() : Params.Type.LIST.name());
        if(rbTopN.isSelected()) {
            String txt = txtN.getText().trim();
            if(txt.length() > 0) {
                try {
                    int val = Integer.parseInt(txt);
                    if(val >= Params.MIN_TOPN && val <= Params.MAX_TOPN) {
                        hmResults.put(Params.N_PARAM, txt);
                    }
                    else
                        errmsg = "Invalid top 'n' value entered (" + Params.MIN_TOPN + " to " + Params.MAX_TOPN + " allowed).";
                } catch(Exception e) {
                    errmsg = "Invalid top 'n' value number entered.";
                }
            }
            else
                errmsg = "You must enter a top 'n' value.";
            if(!errmsg.isEmpty()) {
                txtN.requestFocus();
                hmResults.put("ERRMSG", errmsg);
            }
        }
        else if(rbManual.isSelected()){
            errmsg = getFeatures(Params.FEATURE_PARAM, hmResults);
            if(!errmsg.isEmpty()) {
                tvFeatures.requestFocus();
                hmResults.put("ERRMSG", errmsg);
            }
        }else{ //list selected
            String filepath = txtFile.getText().trim();
            errmsg = checkTestListFile(filepath);
            if(errmsg.isEmpty()){
                hmResults.put(Params.FILE_PARAM, filepath);
            }else{
                txtFile.requestFocus();
                hmResults.put("ERRMSG", errmsg);
            }
        }
        return hmResults;
    }
    
    private String checkTestListFile(String filepath) {
        String errmsg = "";
        if(!Files.exists(Paths.get(filepath)))
            errmsg = "Specified test list file not found.";
        return errmsg;
    }
    
    // validate annotation features selections, at least one feature required
    // if singleSelection, then user could have only selected one
    public String getFeatures(String param, HashMap<String, String> results) {
        String errmsg = "";
        String feature;
        int selcnt = 0;
        ArrayList<String> lstFeatures = new ArrayList<>();
        TreeItem<String> rootItem = tvFeatures.getRoot();
        ObservableList<TreeItem<String>> lst = rootItem.getChildren();
        for(TreeItem ti : lst) {
            feature = "";
            CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) ti;
            if(item.isSelected()) {
                feature = item.getValue();
                lstFeatures.add(feature);
                selcnt += item.getChildren().size();
            }
            else if(item.isIndeterminate()) {
                ObservableList<TreeItem<String>> sublst = item.getChildren();
                for(TreeItem subti : sublst) {
                    CheckBoxTreeItem<String> subitem = (CheckBoxTreeItem<String>) subti;
                    if(subitem.isSelected()) {
                        if(feature.isEmpty())
                            feature = item.getValue();
                        feature += "\t" + subitem.getValue();
                        selcnt++;
                    }
                }
                if(!feature.isEmpty())
                    lstFeatures.add(feature);
            }
        }
        if(lstFeatures.isEmpty())
            errmsg = "You must select one or more enriched features.";
        else {
            if(selcnt > Params.MAX_SELECTIONS)
                errmsg = "You have selected too many features, " + selcnt + ". Maximum allowed is " + Params.MAX_SELECTIONS + ".";
            else {
                int numFeature = 1;
                for(String selFeature : lstFeatures)
                    results.put(param + numFeature++, selFeature);
            }
        }
        return errmsg;
    }

    //
    // Data Classes
    //
    public static class Params extends DlgParams {
        public static enum Type {
            TOPN, MANUAL, LIST
        }
        static int MIN_TOPN = 1;
        static int MAX_TOPN = 50;
        static int MAX_SELECTIONS = 100;
        static String TYPE_PARAM = "type";
        static String SOURCE_PARAM = "source";
        static String N_PARAM = "n";
        static String FEATURE_PARAM = "feature";
        static String FILE_PARAM = "file";
        static int dfltN = 5;
        static Type dfltType = Type.TOPN;
        static String dfltFile = "";

        String source;
        Type type;
        int n;
        String file;
        HashMap<String, HashMap<String, Object>> hmFeatureIDs;
        public Params(String source) {
            this.source = source;
            this.type = dfltType;
            this.n = dfltN;
            this.file = dfltFile;
            this.hmFeatureIDs = new HashMap<>();
        }
        public Params(String source, Type type, int n, String file,HashMap<String, HashMap<String, Object>> hmFeatureIDs) {
            this.source = source;
            this.type = type;
            this.n = n;
            this.file = file;
            this.hmFeatureIDs = hmFeatureIDs;
        }
        public Params(HashMap<String, String> hmParams) {
            this.source = hmParams.containsKey(SOURCE_PARAM)? hmParams.get(SOURCE_PARAM) : "";
            this.type = hmParams.containsKey(TYPE_PARAM)? Type.valueOf(hmParams.get(TYPE_PARAM)) : dfltType;
            this.n = hmParams.containsKey(N_PARAM)? Integer.parseInt(hmParams.get(N_PARAM)) : dfltN;
            this.file = hmParams.containsKey(FILE_PARAM)? hmParams.get(FILE_PARAM) : dfltFile;
            this.hmFeatureIDs = new HashMap<>();
            for(int t = 1; ; t++) {
                if(hmParams.containsKey(FEATURE_PARAM + t)) {
                    String[] fields = hmParams.get(FEATURE_PARAM + t).trim().split("\t");
                    if(fields.length > 0) {
                        HashMap<String, Object> hm = new HashMap<>();
                        hmFeatureIDs.put(fields[0].trim(), hm);
                        for(int i = 1; i < fields.length; i++)
                            hm.put(fields[i].trim(), null);
                    }
                }
                else
                    break;
            }
        }
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hmParams = new HashMap<>();
            hmParams.put(SOURCE_PARAM, source);
            hmParams.put(TYPE_PARAM, type.name());
            hmParams.put(N_PARAM, "" + n);
            hmParams.put(FILE_PARAM, "" + n);
            int numFeature = 1;
            for(String feature : hmFeatureIDs.keySet()) {
                String features = feature;
                HashMap<String, Object> hmIDs = hmFeatureIDs.get(feature);
                for(String id : hmIDs.keySet())
                    features += "\t" + id;
                hmParams.put(FEATURE_PARAM + numFeature++, features);
            }
            return hmParams;
        }
        // base class implements boolean save(String filepath)
    }
    protected static class FeatureDataObj implements Comparable<FeatureDataObj> {
        public String id;
        public double adjPValue;
        
        public FeatureDataObj(String id, double adjPValue) {
            this.id = id;
            this.adjPValue = adjPValue;
        }
        @Override
        public int compareTo(FeatureDataObj entry) {
            if(adjPValue != entry.adjPValue)
                return ((adjPValue > entry.adjPValue)? 1 : -1);
            else
                return (id.compareToIgnoreCase(entry.id));
        }
    }
}
