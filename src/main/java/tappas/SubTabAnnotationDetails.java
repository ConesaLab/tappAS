/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import tappas.DbProject.AFIdStatsData;
import tappas.DbProject.AFStatsData;

import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabAnnotationDetails extends SubTabBase {
    AnchorPane paneContents;
    TableView tblFeatures, tblIDs;    

    String selSource = "";
    String selFeature = "";
    HashMap<String, AFStatsData> hmFeatures = null;

    public SubTabAnnotationDetails(Project project) {
        super(project);
    }

    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            selSource = (String)args.get("Source");
            selFeature = (String)args.get("Feature");
        }
        return result;
    }
    @Override
    public void refreshSubTab(HashMap<String, Object> hmArgs) {
        selSource = (String)hmArgs.get("Source");
        selFeature = (String)hmArgs.get("Feature");
        // data is loaded in memory, OK to not do in background
        hmFeatures = project.data.getSourceFeaturesStats(selSource);
        populateFeatures();
    }
    @Override
    public void search(Object obj, String txt, boolean hide) {
        if(obj != null) {
            if(obj.equals(tblFeatures)) {
                if(txt.isEmpty() && !hide)
                    setTableSearchActive(tblFeatures, false);
                else
                    setTableSearchActive(tblFeatures, true);
                TableView<FeatureData> tblView = (TableView<FeatureData>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<FeatureData> items = (ObservableList<FeatureData>) info.data;
                if(items != null) {
                    ObservableList<FeatureData> matchItems;
                    if(txt.isEmpty()) {
                        // restore original dataset
                        matchItems = items;
                    }
                    else {
                        matchItems = FXCollections.observableArrayList();
                        for(FeatureData data : items) {
                            if(data.getFeature().toLowerCase().contains(txt))
                                matchItems.add(data);
                        }
                    }
                    tblView.setItems(matchItems);
                }
            }
            else if(obj.equals(tblIDs)) {
                if(txt.isEmpty() && !hide)
                    setTableSearchActive(tblIDs, false);
                else
                    setTableSearchActive(tblIDs, true);
                TableView<FeatureIdData> tblView = (TableView<FeatureIdData>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                ObservableList<FeatureIdData> items = (ObservableList<FeatureIdData>) info.data;
                if(items != null) {
                    ObservableList<FeatureIdData> matchItems;
                    if(txt.isEmpty()) {
                        // restore original dataset
                        matchItems = items;
                    }
                    else {
                        matchItems = FXCollections.observableArrayList();
                        for(FeatureIdData data : items) {
                            if(data.getFeatureID().toLowerCase().contains(txt))
                                matchItems.add(data);
                        }
                    }
                    tblView.setItems(matchItems);
                }
            }
        }
    }

    //
    // Internal Functions
    //
    private void showSubTab(HashMap<String, AFStatsData> hmData) {
        hmFeatures = hmData;
        
        // get control objects
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        tblFeatures = (TableView) tabNode.getParent().lookup("#tblFeatures");
        tblIDs = (TableView) tabNode.getParent().lookup("#tblIDs");

        // setup features table
        tblFeatures.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tblIDs.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ObservableList<TableColumn> cols = tblFeatures.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Feature"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("IdCount"));
        cols.get(2).setCellValueFactory(new PropertyValueFactory<>("Entries"));
        cols.get(3).setCellValueFactory(new PropertyValueFactory<>("TransCount"));
        addRowNumbersCol(tblFeatures);
        tblFeatures.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                ObservableList<FeatureData> lstItems = tblFeatures.getSelectionModel().getSelectedItems();
                ObservableList<FeatureIdData> dataIDs = FXCollections.observableArrayList();
                for(FeatureData fd : lstItems) {
                    if(hmFeatures.containsKey(fd.getFeature())) {
                        HashMap<String, AFIdStatsData> hmIDs = hmFeatures.get(fd.getFeature()).hmIds;
                        for(String id : hmIDs.keySet()) {
                            AFIdStatsData cidd = hmIDs.get(id);
                            dataIDs.add(new FeatureIdData(fd.getFeature(), id, cidd.count, cidd.transCount));
                        }
                    }
                }
                FXCollections.sort(dataIDs);
                tblIDs.setItems(dataIDs);
                tblIDs.setUserData(new TabBase.SearchInfo(tblIDs.getItems(), "", false));
                setTableSearchActive(tblIDs, false);
            }
            else {
                ObservableList<FeatureIdData> dataIDs = FXCollections.observableArrayList();
                tblIDs.setItems(dataIDs);
                tblIDs.setUserData(new TabBase.SearchInfo(tblIDs.getItems(), "", false));
                setTableSearchActive(tblIDs, false);
            }
        });
        ContextMenu cm = new ContextMenu();
        app.export.setupSimpleTableExport(tblFeatures, cm, false, "tappAS_Annotation_Features.tsv");
        tblFeatures.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblFeatures);
        setupTableSearch(tblFeatures);
        subTabInfo.lstSearchTables.add(tblFeatures);

        // setup feature Ids table
        cols = tblIDs.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Feature"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("FeatureID"));
        cols.get(2).setCellValueFactory(new PropertyValueFactory<>("Entries"));
        cols.get(3).setCellValueFactory(new PropertyValueFactory<>("TransCount"));
        addRowNumbersCol(tblIDs);
        cm = new ContextMenu();
        app.export.setupSimpleTableExport(tblIDs, cm, false, "tappAS_Annotation_FeatureIDs.tsv");
        tblIDs.setContextMenu(cm);
        app.export.addCopyToClipboardHandler(tblIDs);
        setupTableSearch(tblIDs);
        subTabInfo.lstSearchTables.add(tblIDs);

        // populate tables with data
        populateFeatures();

        // manage focus on both tables
        tblFeatures.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if (newValue)
                onSelectFocusNode = tblFeatures;
        });
        tblIDs.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if (newValue)
                onSelectFocusNode = tblIDs;
        });
        setupExportMenu();
        setFocusNode(tblFeatures, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export features table...");
        item.setOnAction((event) -> {
            app.export.exportTableData(tblFeatures, true, "tappAS_Annotation_Features.tsv");
        });
        cm.getItems().add(item);
        item = new MenuItem("Export feature IDs table...");
        item.setOnAction((event) -> { 
            app.export.exportTableData(tblIDs, true, "tappAS_Annotation_FeatureIDs.tsv");
        });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Export subtab contents image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = paneContents.getWidth();
            double ratio = 3840/x;
            paneContents.setScaleX(ratio);
            paneContents.setScaleY(ratio);
            WritableImage img = paneContents.snapshot(sP, null);
            double newX = paneContents.getWidth();
            ratio = x/newX;
            paneContents.setScaleX(ratio);
            paneContents.setScaleY(ratio);
            
            //WritableImage img = paneContents.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Annotation Source Details Panel", "tappAS_Annotation_Source_Details_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void populateFeatures() {
        ObservableList<FeatureData> dataFeatures = FXCollections.observableArrayList();
        for(String feature : hmFeatures.keySet()) {
            AFStatsData cd = hmFeatures.get(feature);
            dataFeatures.add(new FeatureData(feature, cd.count, cd.idCount, cd.transCount));
        }
        FXCollections.sort(dataFeatures);
        tblIDs.setItems(null);
        tblFeatures.setItems(dataFeatures);
        tblFeatures.setUserData(new TabBase.SearchInfo(tblFeatures.getItems(), "", false));
        if(!dataFeatures.isEmpty()) {
            int idx = 0;
            int selIdx = 0;
            if(!selFeature.isEmpty()) {
                for(FeatureData cd : dataFeatures) {
                    if(cd.getFeature().equals(selFeature)) {
                        selIdx = idx;
                        break;
                    }
                    idx++;
                }
                selFeature = "";
            }
            tblFeatures.getSelectionModel().select(selIdx);
        }
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        HashMap<String, AFStatsData> hmData = project.data.getSourceFeaturesStats(selSource);
        showSubTab(hmData);
    }

    //
    // Data Classes
    //
    public static class ValueData implements Comparable<ValueData> {
        public final SimpleStringProperty value;
 
        public ValueData(String value) {
            this.value = new SimpleStringProperty(value);
        }
        public String getValue() { return value.get(); }
        @Override
        public int compareTo(ValueData vd) {
            return (value.get().compareTo(vd.value.get()));
        }
    }
    public static class FeatureData implements Comparable<FeatureData> {
        public final SimpleStringProperty feature;
        SimpleIntegerProperty entries;
        SimpleIntegerProperty idCount;
        SimpleIntegerProperty transCount;

        public FeatureData(String feature, int entries, int idCount, int transCount) {
            this.feature = new SimpleStringProperty(feature);
            this.entries = new SimpleIntegerProperty(entries);
            this.idCount = new SimpleIntegerProperty(idCount);
            this.transCount = new SimpleIntegerProperty(transCount);
        }
        public String getFeature() { return feature.get(); }
        public int getEntries() { return entries.get(); }
        public int getIdCount() { return idCount.get(); }
        public int getTransCount() { return transCount.get(); }
        @Override
        public int compareTo(FeatureData dbcd) {
            return (feature.get().compareToIgnoreCase(dbcd.feature.get()));
        }
    }
    public static class FeatureIdData implements Comparable<FeatureIdData> {
        SimpleStringProperty feature;
        SimpleStringProperty featureID;
        SimpleIntegerProperty entries;
        SimpleIntegerProperty transCount;

        public FeatureIdData(String feature, String featureID, int entries, int transCount) {
            this.feature = new SimpleStringProperty(feature);
            this.featureID = new SimpleStringProperty(featureID);
            this.entries = new SimpleIntegerProperty(entries);
            this.transCount = new SimpleIntegerProperty(transCount);
        }
        public String getFeature() { return feature.get(); }
        public String getFeatureID() { return featureID.get(); }
        public int getEntries() { return entries.get(); }
        public int getTransCount() { return transCount.get(); }
        @Override
        public int compareTo(FeatureIdData fidd) {
            if(feature.get().equals(fidd.feature.get()))
                return (featureID.get().compareToIgnoreCase(fidd.featureID.get()));
            else
                return (feature.get().compareTo(fidd.feature.get()));
        }
    }
}
