/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.Optional;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgGDVFilterFeatures extends DlgBase {
    Label lblFeatures;
    TreeView tvFeatures;
    Hyperlink lnkClearAll, lnkSetAll;
    
    Params params;
    
    public DlgGDVFilterFeatures(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(Params dfltParams, String type, HashMap<String, HashMap<String, HashMap<String, Object>>> hmFeatureIDs) { //, int isoforms
        if(createDialog("GDVFilterFeatures.fxml", "Annotation Features Display Filter", true, "Help_Dlg_GDV_FilterFeatures.html")) {
            setProjectName();
            if(dfltParams == null)
                params = new Params();
            else
                params = dfltParams;
            
            // get control objects
            lblFeatures = (Label) scene.lookup("#lblFeatures");
            lnkClearAll = (Hyperlink) scene.lookup("#lnkClearAll");
            lnkSetAll = (Hyperlink) scene.lookup("#lnkSetAll");
            tvFeatures = (TreeView) scene.lookup("#tvFeatures");

            // setup button actions
            lnkSetAll.setOnAction((event) -> { setAllFeatures(); });
            lnkClearAll.setOnAction((event) -> { clearAllFeatures(); });

            // populate dialog
            lblFeatures.setText(type + " Annotation Features");
            populateAnnotationFeatureIDs(tvFeatures, hmFeatureIDs);
            selectAnnotationFeatureIDs(tvFeatures, params.hmNotFeatureIDs);
            
            // setup dialog event handlers
            dialog.setOnCloseRequest((DialogEvent event) -> {
                if(dialog.getResult() != null && dialog.getResult().containsKey("ERRMSG")) {
                    showDlgMsg((String)dialog.getResult().get("ERRMSG"));
                    dialog.setResult(null);
                    event.consume();
                }
            });
            dialog.setResultConverter((ButtonType b) -> {
                HashMap<String, Object> params1 = null;
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
    protected void populateAnnotationFeatureIDs(TreeView tv, HashMap<String, HashMap<String, HashMap<String, Object>>> hmFeatureIDs) {
        tv.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
        TreeItem<String> rootItem = new TreeItem<> ("Annotation Features");
        rootItem.setExpanded(true);
        HashMap<String, HashMap<String, Object>> hmFeatures;
        HashMap<String, Object> hmIDs;
        for (String source : hmFeatureIDs.keySet()) {
            hmFeatures = hmFeatureIDs.get(source);
            CheckBoxTreeItem<String> item = new CheckBoxTreeItem<> (source);
            item.setSelected(true);
            item.selectedProperty().addListener((obsValue, oldValue, newValue) -> annotationFeaturesOnSelectionChanged());
            for (String feature : hmFeatures.keySet()) {
                hmIDs = hmFeatures.get(feature);
                CheckBoxTreeItem<String> subitem = new CheckBoxTreeItem<> (feature);
                subitem.setSelected(true);
                subitem.selectedProperty().addListener((observableValue, oldnum, newnum) -> annotationFeaturesOnSelectionChanged());
                for (String id : hmIDs.keySet()) {
                    CheckBoxTreeItem<String> subsubitem = new CheckBoxTreeItem<> (id);
                    subsubitem.setSelected(true);
                    subsubitem.selectedProperty().addListener((observableValue, oldnum, newnum) -> annotationFeaturesOnSelectionChanged());
                    subitem.getChildren().add(subsubitem);
                }
                item.getChildren().add(subitem);
            }
            rootItem.getChildren().add(item);
        }
        tv.setShowRoot(false);
        tv.setRoot(rootItem);
    }
    protected void selectAnnotationFeatureIDs(TreeView tv, HashMap<String, HashMap<String, HashMap<String, Object>>> hmNotSels) {
        HashMap<String, HashMap<String, Object>> hmNotFeatures;
        HashMap<String, Object> hmNotIDs;
        TreeItem<String> rootItem = tvFeatures.getRoot();
        ObservableList<TreeItem<String>> lst = rootItem.getChildren();
        for(TreeItem ti : lst) {
            CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) ti;
            // check if source, or part of, is not selected
            if(hmNotSels.containsKey(item.getValue())) {
                // item is either not selected or indeterminate
                hmNotFeatures = hmNotSels.get(item.getValue());
                if(hmNotFeatures.isEmpty())
                    item.setSelected(false);
                else {
                    // item is indeterminate set children accordingly
                    item.setIndeterminate(true);
                    ObservableList<TreeItem<String>> sublst = item.getChildren();
                    for(TreeItem subti : sublst) {
                        CheckBoxTreeItem<String> subitem = (CheckBoxTreeItem<String>) subti;
                        // check if feature, or part of, is not selected
                        if(hmNotFeatures.containsKey(subitem.getValue())) {
                            // subitem is either not selected or indeterminate
                            hmNotIDs = hmNotFeatures.get(subitem.getValue());
                            if(hmNotIDs.isEmpty())
                                subitem.setSelected(false);
                            else {
                                // subitem is indeterminate set children accordingly
                                subitem.setIndeterminate(true);
                                ObservableList<TreeItem<String>> subsublst = subitem.getChildren();
                                for(TreeItem subsubti : subsublst) {
                                    CheckBoxTreeItem<String> subsubitem = (CheckBoxTreeItem<String>) subsubti;
                                    // check if feature id is not selected
                                    if(hmNotIDs.containsKey(subsubitem.getValue()))
                                        subsubitem.setSelected(false);
                                }
                            }                            
                        }
                    }
                }
            }
        }
    }

    //
    // Dialog Validation
    //
    private HashMap<String, Object> validate(Dialog dialog) {
        HashMap<String, HashMap<String, HashMap<String, Object>>> hmNotFeatureIDs = new HashMap<>();
        HashMap<String, HashMap<String, Object>> hmNotFeatures;
        HashMap<String, Object> hmNotIDs;
        System.out.println("Validate dialog");
        
        TreeItem<String> rootItem = tvFeatures.getRoot();
        ObservableList<TreeItem<String>> lst = rootItem.getChildren();
        for(TreeItem ti : lst) {
            hmNotFeatures = new HashMap<>();
            CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) ti;
            if(!item.isSelected() && !item.isIndeterminate())
                hmNotFeatureIDs.put(item.getValue(), hmNotFeatures);
            else {
                ObservableList<TreeItem<String>> sublst = item.getChildren();
                for(TreeItem subti : sublst) {
                    hmNotIDs = new HashMap<>();
                    CheckBoxTreeItem<String> subitem = (CheckBoxTreeItem<String>) subti;
                    if(!subitem.isSelected() && !subitem.isIndeterminate()) {
                        hmNotFeatures.put(subitem.getValue(), hmNotIDs);
                        if(!hmNotFeatureIDs.containsKey(item.getValue()))
                            hmNotFeatureIDs.put(item.getValue(), hmNotFeatures);
                    }
                    else {
                        ObservableList<TreeItem<String>> subsublst = subitem.getChildren();
                        for(TreeItem subsubti : subsublst) {
                            CheckBoxTreeItem<String> subsubitem = (CheckBoxTreeItem<String>) subsubti;
                            if(!subsubitem.isSelected() && !subsubitem.isIndeterminate()) {
                                hmNotIDs.put(subsubitem.getValue(), null);
                                if(!hmNotFeatureIDs.containsKey(item.getValue()))
                                    hmNotFeatureIDs.put(item.getValue(), hmNotFeatures);
                                if(!hmNotFeatures.containsKey(subitem.getValue()))
                                    hmNotFeatures.put(subitem.getValue(), hmNotIDs);
                            }
                        }                        
                    }
                }
            }
        }
        Params results = new Params(hmNotFeatureIDs, false);
        return results.getParamsObj();
    }

    //
    // Data Classes
    //
    public static class Params extends DlgParams {
        static String FEATURES_PARAM = "features";

        HashMap<String, HashMap<String, HashMap<String, Object>>> hmNotFeatureIDs;
        public Params() {
            this.hmNotFeatureIDs = new HashMap<>();
        }
        // dummy used to create a different signature - pretty lame
        public Params(HashMap<String, HashMap<String, HashMap<String, Object>>> hmFeatureIDs, boolean dummy) { //, boolean chkVaryinOnly) {
            this.hmNotFeatureIDs = hmFeatureIDs;
        }
        public Params(HashMap<String, Object> hmParams) {
            this.hmNotFeatureIDs = hmParams.containsKey(FEATURES_PARAM)? (HashMap<String, HashMap<String, HashMap<String, Object>>>) hmParams.get(FEATURES_PARAM) : new HashMap<>();
        }
        public HashMap<String, Object> getParamsObj() {
            HashMap<String, Object> hmParams = new HashMap<>();
            hmParams.put(FEATURES_PARAM, hmNotFeatureIDs);
            return hmParams;
        }
        // base class implements boolean save(String filepath)
    }
}
