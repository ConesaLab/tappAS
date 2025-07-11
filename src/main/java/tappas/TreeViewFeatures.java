/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import tappas.DbProject.AFStatsData;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class TreeViewFeatures extends AppObject {
    DlgBase dlgBase;
    TreeView tv;
    boolean singleSelection = false;
    boolean ignoreCBTIevents = false;
    
    public TreeViewFeatures(Project project, DlgBase dlgBase) {
        super(project, null);
        this.dlgBase = dlgBase;
    }
    public void initialize(TreeView tv, HashMap<String, HashMap<String, Object>> hmFeatureSels, boolean singleSelection) {
        this.tv = tv;
        this.singleSelection = singleSelection;
        HashMap<String, HashMap<String, AFStatsData>> hmFeatures = project.data.getAFStats();

        // set for checkbox tree items
        tv.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
        TreeItem<String> rootItem = new TreeItem<> ("");
        AFStatsData sd;
        HashMap<String, Object> hm;
        for (String source : hmFeatures.keySet()) {
            hm = null;
            CheckBoxTreeItem<String> item = new CheckBoxTreeItem<> (source);
            item.selectedProperty().addListener((obsValue, oldValue, newValue) -> dlgBase.annotationFeaturesOnSelectionChanged());
            if(singleSelection)
                setupForSingleItem(item);
            if(hmFeatureSels.containsKey(source)) {
                hm = hmFeatureSels.get(source);
                if(hm.size() > 0) {
                    item.setIndeterminate(true);
                    item.setExpanded(true);
                }
                else
                    item.setSelected(true);
            }
            boolean addflg = false;
            HashMap<String, AFStatsData> hmSF = hmFeatures.get(source);
            for (String feature : hmSF.keySet()) {
                if(!project.data.isRsvdFeature(source, feature)) {
                    sd = hmSF.get(feature);
                    if(sd.idCount > 0) {
                        addflg = true;
                        CheckBoxTreeItem<String> subitem = new CheckBoxTreeItem<> (feature);
                        subitem.selectedProperty().addListener((obsValue, oldValue, newValue) -> dlgBase.annotationFeaturesOnSelectionChanged());
                        if(hm != null && (hm.containsKey(feature) || hm.isEmpty()))
                            subitem.setSelected(true);
                        item.getChildren().add(subitem);
                    }
                }
            }
            if(addflg)
                rootItem.getChildren().add(item);
        }
        rootItem.setExpanded(true);
        tv.setShowRoot(false);
        tv.setRoot(rootItem);
    }
    public void initializePosition(TreeView tv, HashMap<String, HashMap<String, Object>> hmFeatureSels, boolean singleSelection) {
        this.tv = tv;
        this.singleSelection = singleSelection;
        HashMap<String, ArrayList<String>> hmFeatures = project.data.getAFStatsPositional();

        // set for checkbox tree items
        tv.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
        TreeItem<String> rootItem = new TreeItem<> ("");
        AFStatsData sd;
        HashMap<String, Object> hm;
        for (String source : hmFeatures.keySet()) {
            hm = null;
            CheckBoxTreeItem<String> item = new CheckBoxTreeItem<> (source);
            item.selectedProperty().addListener((obsValue, oldValue, newValue) -> dlgBase.annotationFeaturesOnSelectionChanged());
            if(singleSelection)
                setupForSingleItem(item);
            if(hmFeatureSels.containsKey(source)) {
                hm = hmFeatureSels.get(source);
                if(hm.size() > 0) {
                    item.setIndeterminate(true);
                    item.setExpanded(true);
                }
                else
                    item.setSelected(true);
            }
            boolean addflg = false;
            ArrayList<String> hmSF = hmFeatures.get(source);
            for (String feature : hmSF) {
                if(!project.data.isRsvdFeature(source, feature)) {
                    addflg = true;
                    CheckBoxTreeItem<String> subitem = new CheckBoxTreeItem<> (feature);
                    subitem.selectedProperty().addListener((obsValue, oldValue, newValue) -> dlgBase.annotationFeaturesOnSelectionChanged());
                    if(hm != null && (hm.containsKey(feature) || hm.isEmpty()))
                        subitem.setSelected(true);
                    item.getChildren().add(subitem);

                }
            }
            if(addflg)
                rootItem.getChildren().add(item);
        }
        rootItem.setExpanded(true);
        tv.setShowRoot(false);
        tv.setRoot(rootItem);
    }
    public void initializePresence(TreeView tv, HashMap<String, HashMap<String, Object>> hmFeatureSels, boolean singleSelection) {
        this.tv = tv;
        this.singleSelection = singleSelection;
        HashMap<String, ArrayList<String>> hmFeatures = project.data.getAFStatsPresence();

        // set for checkbox tree items
        tv.setCellFactory(CheckBoxTreeCell.<String>forTreeView());
        TreeItem<String> rootItem = new TreeItem<> ("");
        AFStatsData sd;
        HashMap<String, Object> hm;
        for (String source : hmFeatures.keySet()) {
            hm = null;
            CheckBoxTreeItem<String> item = new CheckBoxTreeItem<> (source);
            item.selectedProperty().addListener((obsValue, oldValue, newValue) -> dlgBase.annotationFeaturesOnSelectionChanged());
            if(singleSelection)
                setupForSingleItem(item);
            if(hmFeatureSels.containsKey(source)) {
                hm = hmFeatureSels.get(source);
                if(hm.size() > 0) {
                    item.setIndeterminate(true);
                    item.setExpanded(true);
                }
                else
                    item.setSelected(true);
            }
            boolean addflg = false;
            ArrayList<String> hmSF = hmFeatures.get(source);
            for (String feature : hmSF) {
                if(!project.data.isRsvdFeature(source, feature)) {
                    addflg = true;
                    CheckBoxTreeItem<String> subitem = new CheckBoxTreeItem<> (feature);
                    subitem.selectedProperty().addListener((obsValue, oldValue, newValue) -> dlgBase.annotationFeaturesOnSelectionChanged());
                    if(hm != null && (hm.containsKey(feature) || hm.isEmpty()))
                        subitem.setSelected(true);
                    item.getChildren().add(subitem);

                }
            }
            if(addflg)
                rootItem.getChildren().add(item);
        }
        rootItem.setExpanded(true);
        tv.setShowRoot(false);
        tv.setRoot(rootItem);
    }
    public String validate(String param, HashMap<String, String> results) {
        String errmsg = "";
        String feature;
        ArrayList<String> lstFeatures = new ArrayList<>();
        TreeItem<String> rootItem = tv.getRoot();
        ObservableList<TreeItem<String>> lst = rootItem.getChildren();
        for(TreeItem ti : lst) {
            feature = "";
            CheckBoxTreeItem<String> item = (CheckBoxTreeItem<String>) ti;
            if(item.isSelected() && !item.isIndeterminate()) {
                feature = item.getValue();
                lstFeatures.add(feature);
            }
            else {
                ObservableList<TreeItem<String>> sublst = item.getChildren();
                for(TreeItem subti : sublst) {
                    CheckBoxTreeItem<String> subitem = (CheckBoxTreeItem<String>) subti;
                    if(subitem.isSelected()) {
                        if(feature.isEmpty())
                            feature = item.getValue();
                        feature += "\t" + subitem.getValue();
                    }
                }
                if(!feature.isEmpty())
                    lstFeatures.add(feature);
            }
        }
        if(lstFeatures.isEmpty()) {
            tv.requestFocus();
            errmsg = "You must select one or more annotation features.";
        }
        else {
            if(singleSelection)
                results.put(param, lstFeatures.get(0));
            else {
                int numFeature = 1;
                for(String selFeature : lstFeatures)
                    results.put(param + numFeature++, selFeature);
            }
        }
        return errmsg;
    }
    
    public final void setDisable(boolean value) {
        tv.disableProperty().set(value);
    }
    
    //
    // Internal Functions
    //
    
    // add CBTI listener to detect change
    protected void setupForSingleItem(CheckBoxTreeItem<String> item) {
        item.indeterminateProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(!ignoreCBTIevents && newValue) {
                ignoreCBTIevents = true;
                System.out.println("The indeterminate item is " + item.valueProperty().get());
                deselectOtherItems(item);
                ignoreCBTIevents = false;
            }
        });
        item.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(!ignoreCBTIevents && newValue) {
                ignoreCBTIevents = true;
                System.out.println("The selected item is " + item.valueProperty().get());
                deselectOtherItems(item);
                ignoreCBTIevents = false;
            }
        });
    }
    // a new selection took place: deselect all other CBTI in the tree view
    private void deselectOtherItems(CheckBoxTreeItem selItem) {
        ObservableList<TreeItem<String>> lstItems = tv.getRoot() == null? null : tv.getRoot().getChildren();
        if(lstItems != null) {
            for(TreeItem ti : lstItems) {
                CheckBoxTreeItem<String> cti = (CheckBoxTreeItem<String>) ti;
                if(!cti.equals(selItem) && (cti.isSelected() || cti.isIndeterminate())) {
                    ObservableList<TreeItem<String>> lstSubItems = cti.getChildren();
                    for(TreeItem sti : lstSubItems) {
                        CheckBoxTreeItem<String> csti = (CheckBoxTreeItem<String>) sti;
                        if(csti.isSelected())
                            csti.setSelected(false);
                    }
                }                            
            }
        }
    }
}
