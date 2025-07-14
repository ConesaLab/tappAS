/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Window;
import tappas.DataAnnotation.AFStatsTblData;
import tappas.DbProject.AFStatsData;

import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabAnnotationSummary extends SubTabBase {
    AnchorPane paneContents;
    TableView tblDBSummary;
    PieChart pieSummary;
    
    String selSource = "";
    String selFeature = "";
    
    public SubTabAnnotationSummary(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        return result;
    }
    @Override
    public void search(Object obj, String txt, boolean hide) {
        if(obj != null) {
            if(obj.equals(tblDBSummary)) {
                TableView<AFStatsTblData> tblView = (TableView<AFStatsTblData>) obj;
                TabBase.SearchInfo info = (TabBase.SearchInfo) tblView.getUserData();
                info.txt = txt;
                info.hide = hide;
                ObservableList<AFStatsTblData> items = (ObservableList<AFStatsTblData>) info.data;
                if(items != null) {
                    ObservableList<AFStatsTblData> matchItems;
                    if(txt.isEmpty()) {
                        // restore original dataset
                        matchItems = items;
                    }
                    else {
                        matchItems = FXCollections.observableArrayList();
                        for(AFStatsTblData data : items) {
                            if(data.getSource().toLowerCase().contains(txt) || data.getFeature().toLowerCase().contains(txt))
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
    private void showAnnotationDrillDown(String source, String feature, HashMap<String, HashMap<String, Object>> hmGeneTransFilter) {
        app.tabs.openTabAnnotDetails(project, source, feature, hmGeneTransFilter);
        // clear selection to avoid chart using the value onmouseclicked event
        selSource = "";
        selFeature = "";
    }
    private void showSubTab(HashMap<String, HashMap<String, AFStatsData>> hmAFStats) {
        // get control objects
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        tblDBSummary = (TableView) tabNode.lookup("#tblDBSummary");

        // setup summary table
        ObservableList<TableColumn> cols = tblDBSummary.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Source"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Feature"));
        cols.get(2).setCellValueFactory(new PropertyValueFactory<>("IdCount"));
        cols.get(3).setCellValueFactory(new PropertyValueFactory<>("Count"));
        ObservableList<AFStatsTblData> lstSD = FXCollections.observableArrayList();
        for(String source : hmAFStats.keySet()) {
            HashMap<String, AFStatsData> hmFeatures = hmAFStats.get(source);
            for(String feature : hmFeatures.keySet()) {
                AFStatsData sd = hmFeatures.get(feature);
                lstSD.add(new AFStatsTblData(source, feature, sd.count, sd.idCount, sd.transCount));
            }
        }
        FXCollections.sort(lstSD);
        addRowNumbersCol(tblDBSummary);
        tblDBSummary.setItems(lstSD);
        if(tblDBSummary.getItems().size() > 0)
            tblDBSummary.getSelectionModel().select(0);
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Show source details");
        item.setOnAction((itemEvent) -> { 
            AFStatsTblData sel = (AFStatsTblData) tblDBSummary.getSelectionModel().getSelectedItem();
            String source = "";
            String feature = "";
            if(sel != null) {
                source = sel.getSource();
                feature = sel.getFeature();
            }
            showAnnotationDrillDown(source, feature, project.data.getResultsGeneTrans());
        });
        cm.getItems().add(item);
        app.export.setupSimpleTableExport(tblDBSummary, cm, true, "tapas_AnnotationFeatures.tsv");
        app.export.addCopyToClipboardHandler(tblDBSummary);
        tblDBSummary.setContextMenu(cm);
        tblDBSummary.setUserData(new TabBase.SearchInfo(tblDBSummary.getItems(), "", false));
        setupTableSearch(tblDBSummary);
        subTabInfo.lstSearchTables.add(tblDBSummary);

        // setup summary pie chart
        // only show source if at least one of its feature is not reserved
        ObservableList<PieChart.Data> pieChartData =  FXCollections.observableArrayList();
        HashMap<String, HashMap<String, AFStatsData>> hmSources = new HashMap<>();
        for(String source : hmAFStats.keySet()) {
            boolean add = false;
            HashMap<String, AFStatsData> hmFeatures = hmAFStats.get(source);
            for(String feature : hmFeatures.keySet()) {
                if(!project.data.isRsvdFeature(source, feature)) {
                    add = true;
                    break;
                }
            }
            if(add)
                hmSources.put(source, hmFeatures);
        }
        // calculate total pie count, exclude reserved cats
        double piecnt = 0;
        for(String source : hmSources.keySet()) {
            HashMap<String, AFStatsData> hmFeatures = hmAFStats.get(source);
            for(String feature : hmFeatures.keySet()) {
                if(!project.data.isRsvdFeature(source, feature))
                    piecnt += hmFeatures.get(feature).count;
            }
        }
        double cnt, pct;
        for(String source : hmSources.keySet()) {
            cnt = 0.0;
            HashMap<String, AFStatsData> hmFeatures = hmAFStats.get(source);
            for(String feature : hmFeatures.keySet()) {
                if(!project.data.isRsvdFeature(source, feature))
                    cnt += hmFeatures.get(feature).count;
            }
            pct = cnt * 100 / piecnt;
            pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
            pieChartData.add(new PieChart.Data(source, pct));
        }
        pieSummary = (PieChart) tabNode.lookup("#pieSummary");
        pieSummary.setData(pieChartData);
        MenuItem[] items = new MenuItem[1];
        items[0] = new MenuItem("Show source details...");
        items[0].setOnAction((itemEvent) -> { showAnnotationDrillDown(selSource, selFeature, project.data.getResultsGeneTrans()); });
        app.ctls.setupChartExport(pieSummary, "Annotation Sources Chart", "tappAS_AnnotationSourcesChart.png", items);
        ContextMenu cmpc = (ContextMenu) pieSummary.getUserData();
        if(cmpc != null) {
            cmpc.getItems().get(0).setDisable(true);
            cmpc.getItems().get(0).setText("Right click on pie area to select annotation source...");
        }
        pieSummary.setOnMouseClicked((event) -> {
            if(event.getButton() == MouseButton.SECONDARY) {
                ContextMenu cmpsd = (ContextMenu) pieSummary.getUserData();
                if (cmpsd != null) {
                    if(!selSource.isEmpty()) {
                        cmpsd.getItems().get(0).setDisable(false);
                        cmpsd.getItems().get(0).setText("Show " + selSource + " details...");
                    }
                    else {
                        cmpsd.getItems().get(0).setDisable(true);
                        cmpsd.getItems().get(0).setText("Right click on pie area to select annotation source...");
                    }
                    Window wnd = pieSummary.getScene().getWindow();
                    cmpsd.show(wnd, event.getScreenX(), event.getScreenY());
                }
            }
        });
        Tooltip tt;
        for (PieChart.Data pcdata : pieChartData) {
            // set pie chart area tooltip
            tt = new Tooltip("  " + pcdata.getName() + " " + String.valueOf(pcdata.getPieValue()) + "%  ");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(pcdata.getNode(), tt);
            
            // handle click event to set the source it was clicked on
            pcdata.getNode().setOnMouseClicked((MouseEvent e) -> {
                // pcdata object is available inside each individual event handler in the loop - find Java doc on this
                selSource = pcdata.getName();
                selFeature = "~";
            });
        }

        setupExportMenu();
        setFocusNode(tblDBSummary, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export summary table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblDBSummary, true, "tappAS_Annotations_Summary.tsv"); });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Export summary chart image...");
        item.setOnAction((event) -> {
            SnapshotParameters sP = new SnapshotParameters();
            double x = pieSummary.getWidth();
            double ratio = 3840/x;
            pieSummary.setScaleX(ratio);
            pieSummary.setScaleY(ratio);
            WritableImage img = pieSummary.snapshot(sP, null);
            double newX = pieSummary.getWidth();
            ratio = x/newX;
            pieSummary.setScaleX(ratio);
            pieSummary.setScaleY(ratio);
            
            //WritableImage img = pieSummary.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Annotation Summary Chart", "tappAS_Annotations_Summary.png", (Image)img);
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
            app.export.exportImage("Annotations Summary Panel", "tappAS_Annotations_Summary_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        HashMap<String, HashMap<String, AFStatsData>> hmData = project.data.getAFStats();
        showSubTab(hmData);
    }
}
