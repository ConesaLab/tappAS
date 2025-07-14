/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import tappas.TabProjectDataViz.GeneSummaryData1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabFEASummary extends SubTabBase {
    AnchorPane paneContents;
    TableView tblItems, tblFeatures;
    PieChart pieSummary;
    ScrollPane spFeatures;
    BarChart barFeatures;
    
    ContextMenu cmSummary;
    DataFEA feaData;
    DlgFEAnalysis.Params feaParams;
    String analysisId = "";
    
    public SubTabFEASummary(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            feaData = new DataFEA(project);
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectDataViz.Panels.SUMMARYFEA.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectDataViz.Panels.SUMMARYFEA.name().length()).trim());
            }
            if(args.containsKey("id")) {
                analysisId = (String) args.get("id");
                feaParams = DlgFEAnalysis.Params.load(feaData.getFEAParamsFilepath(analysisId));
                String shortName = getShortName(feaParams.name);
                subTabInfo.title = "FEA: " + shortName;
                subTabInfo.tooltip = "Functional Enrichment Analysis summary for '" + feaParams.name.trim() + "'";
                setTabLabel(subTabInfo.title, subTabInfo.tooltip);
            }
            else
                result = false;
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        if(hm.containsKey("updateFEAResults")) {
            // get test list
            HashMap<String, Object> hmTest = new HashMap<>();
            ArrayList<String> lstItems = Utils.loadSingleItemListFromFile(feaData.getFEATestListFilepath(feaParams.dataType.name(), analysisId), false);
            if(!lstItems.isEmpty()) {
                for(String item : lstItems)
                    hmTest.put(item, null);
            }
            HashMap<String, Object> hmBkgnd = new HashMap<>();
            lstItems = Utils.loadSingleItemListFromFile(feaData.getFEABkgndListFilepath(feaParams.dataType.name(), analysisId), false);
            if(!lstItems.isEmpty()) {
                for(String item : lstItems)
                    hmBkgnd.put(item, null);
            }
            DataFEA.EnrichedFeaturesData data = feaData.getFEAEnrichedFeaturesData(feaParams.dataType.name(), analysisId, feaParams.sigValue, hmTest, hmBkgnd);
            ObservableList<DataFEA.FEASelectionResults> dataResults = feaData.getFEASelectionResults(feaParams.dataType.name(), analysisId, feaParams.sigValue);
            FXCollections.sort(dataResults);
            showSummary(data, dataResults);
        }
        return obj;
    }

    //
    // Internal Functions
    //
    private void showSubTab(DataFEA.EnrichedFeaturesData data, ObservableList<DataFEA.FEASelectionResults> dataResults) {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        pieSummary = (PieChart) tabNode.getParent().lookup("#pieSummary");
        spFeatures = (ScrollPane) tabNode.getParent().lookup("#spFeatures");
        barFeatures = (BarChart) spFeatures.getContent().lookup("#barFeatures");
        tblItems = (TableView) tabNode.getParent().lookup("#tblItems");
        tblFeatures = (TableView) tabNode.getParent().lookup("#tblFeatures");

        // items table
        ObservableList<TableColumn> cols = tblItems.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Field"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Count1"));
        cols.get(2).setCellValueFactory(new PropertyValueFactory<>("Pct1"));
        app.export.addCopyToClipboardHandler(tblItems);
        tblItems.setOnMouseClicked((event) -> {
            if(cmSummary != null)
                cmSummary.hide();
            cmSummary = null;
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                Node node = event.getPickResult().getIntersectedNode();
                if(node != null) {
                    System.out.println("node: " + node);
                    if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        node = node.getParent();
                        System.out.println("parent: " + node);
                    }
                    if(node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        cmSummary = new ContextMenu();
                        app.export.setupSimpleTableExport(tblItems, cmSummary, false, "tappAS_FEA_"+feaParams.name.trim()+"_Summary.tsv");
                        cmSummary.setAutoHide(true);
                        cmSummary.show(tblItems, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });

        cols = tblFeatures.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Category"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("EnrichedCount"));
        cols.get(2).setCellValueFactory(new PropertyValueFactory<>("TotalCount"));
        app.export.addCopyToClipboardHandler(tblFeatures);
        tblFeatures.setOnMouseClicked((event) -> {
            if(cmSummary != null)
                cmSummary.hide();
            cmSummary = null;
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                Node node = event.getPickResult().getIntersectedNode();
                if(node != null) {
                    System.out.println("node: " + node);
                    if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        node = node.getParent();
                        System.out.println("parent: " + node);
                    }
                    if(node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        cmSummary = new ContextMenu();
                        app.export.setupSimpleTableExport(tblFeatures, cmSummary, false, "tappAS_FEA_"+feaParams.name.trim()+"_Features_Summary.tsv");
                        cmSummary.setAutoHide(true);
                        cmSummary.show(tblFeatures, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });

        showSummary(data, dataResults);

        // manage focus on both tables
        tblFeatures.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue)
                onSelectFocusNode = tblFeatures;
        });
        tblItems.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue)
                onSelectFocusNode = tblItems;
        });
        
        setupExportMenu();
        setFocusNode(tblFeatures, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export summary table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblItems, true, "tappAS_FEA_"+feaParams.name.trim()+"_Summary.tsv"); });
        cm.getItems().add(item);
        item = new MenuItem("Export features table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblFeatures, true, "tappAS_FEA_"+feaParams.name.trim()+"_Features_Summary.tsv"); });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Export summary pie chart image...");
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
            app.export.exportImage("Transcripts Summary Chart", "tappAS_FEA_"+feaParams.name.trim()+"_Summary.png", (Image)img);
        });
        cm.getItems().add(item);
        item = new MenuItem("Export features bar chart image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = barFeatures.getWidth();
            double ratio = 3840/x;
            barFeatures.setScaleX(ratio);
            barFeatures.setScaleY(ratio);
            WritableImage img = barFeatures.snapshot(sP, null);
            double newX = barFeatures.getWidth();
            ratio = x/newX;
            barFeatures.setScaleX(ratio);
            barFeatures.setScaleY(ratio);
            
            //WritableImage img = barFeatures.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("FEA Enriched Features", "tappAS_FEA_"+feaParams.name.trim()+"_Features_Summary.png", (Image)img);
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
            app.export.exportImage("FEA Summary Panel", "tappAS_FEA_"+feaParams.name.trim()+"_Summary_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void showSummary(DataFEA.EnrichedFeaturesData data, ObservableList<DataFEA.FEASelectionResults> dataResults) {
        String itemsName = Utils.properCase(app.data.getDataTypePlural(feaParams.dataType.name()));
        
        // items table
        double testpct = 0.0;
        double nontestpct = 0.0;
        if(data.cntBkgndMembers > 0) {
            testpct = data.cntTestMembers * 100 / (double)data.cntBkgndMembers;
            testpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(testpct*100)/100.0)));
            nontestpct = Double.parseDouble(String.format("%.2f", ((double)Math.round((100.0 - testpct)*100)/100.0)));
        }
        ObservableList<GeneSummaryData1> sumdata = FXCollections.observableArrayList();
        sumdata.add(new GeneSummaryData1("Test " + itemsName, data.cntTestMembers, testpct));
        sumdata.add(new GeneSummaryData1("Non-Test " + itemsName, (data.cntBkgndMembers - data.cntTestMembers), nontestpct));
        sumdata.add(new GeneSummaryData1("Total", data.cntBkgndMembers, 100.00));
        tblItems.setItems(sumdata);
        if(!sumdata.isEmpty())
            tblItems.getSelectionModel().select(0);
        tblItems.setRowFactory(tableView -> {
            TableRow<GeneSummaryData1> row = new TableRow<>();
            row.selectedProperty().addListener((obs, oldData, newData) -> {
                GeneSummaryData1 sd = row.getItem();
                if(sd != null && sd.getField().toLowerCase().equals("total")) {
                    if(newData)
                        row.setStyle("");
                    else
                        row.setStyle("-fx-background-color: honeydew;");
                }
            });
            row.itemProperty().addListener((obs, oldData, newData) -> {
                if(newData != null && newData.getField().toLowerCase().equals("total"))
                    row.setStyle("-fx-background-color: honeydew;");
            });
            return row;
        });
        app.export.addCopyToClipboardHandler(tblItems);

        ObservableList<PieChart.Data> pieChartData =  FXCollections.observableArrayList();
        pieChartData.add(new PieChart.Data("Test " + itemsName, testpct));
        pieChartData.add(new PieChart.Data("Non-Test " + itemsName, nontestpct));
        pieSummary.setData(pieChartData);
        pieSummary.setTitle(itemsName + " Summary");
        app.ctls.setupChartExport(pieSummary, itemsName + " List Items", "tappAS_FEA_"+feaParams.name.trim()+"_TransCodingChart.png", null);
        Tooltip tt;
        for (PieChart.Data pcdata : pieChartData) {
            tt = new Tooltip("  " + pcdata.getName() + "\n  Percent: " + String.valueOf(pcdata.getPieValue()) + "%  \n  Count: " + (pcdata.getName().equals("Non-Test")? data.cntTestMembers : (data.cntBkgndMembers - data.cntTestMembers)) + " out of " + data.cntBkgndMembers);
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(pcdata.getNode(), tt);
        }

        // features table
        ObservableList<FEAFeaturesSummaryData> lstFeaturesSummary = FXCollections.observableArrayList();
        HashMap<String, Integer> hmCat = new HashMap<>();
        for(DataFEA.FEAStatsData stats : data.lstStats) {
            if(!hmCat.containsKey(stats.getCategory()))
                hmCat.put(stats.getCategory(), 1);
            else
                hmCat.put(stats.getCategory(), (hmCat.get(stats.getCategory()) + 1));
        }
        int cntTotal = 0;
        int cntEnriched = 0;
        // add pct?
        HashMap<String, Integer> hmNotEnriched = new HashMap<>();
        for(String cat : hmCat.keySet()) {
            int total = 0;
            for(DataFEA.FEASelectionResults fear : dataResults) {
                if(fear.getFeature().equals(cat))
                    total++;
            }
            cntTotal += total;
            cntEnriched += hmCat.get(cat);
            lstFeaturesSummary.add(new FEAFeaturesSummaryData(cat, hmCat.get(cat), total));
        }
        for(DataFEA.FEASelectionResults fear : dataResults) {
            String rcat = fear.getFeature();
            if(!hmCat.containsKey(rcat))
            {
                if(!hmNotEnriched.containsKey(rcat))
                    hmNotEnriched.put(rcat, 1);
                else
                    hmNotEnriched.put(rcat, hmNotEnriched.get(rcat) + 1);
            }
        }
        for(String cat : hmNotEnriched.keySet()) {
            lstFeaturesSummary.add(new FEAFeaturesSummaryData(cat, 0, hmNotEnriched.get(cat)));
            cntTotal += hmNotEnriched.get(cat);
        }
        Collections.sort(lstFeaturesSummary);
        lstFeaturesSummary.add(new FEAFeaturesSummaryData("Total", cntEnriched, cntTotal));

        tblFeatures.setRowFactory(tableView -> {
            TableRow<FEAFeaturesSummaryData> row = new TableRow<>();
            row.selectedProperty().addListener((obs, oldData, newData) -> {
                FEAFeaturesSummaryData sd = row.getItem();
                if(sd.getCategory().toLowerCase().equals("total")) {
                    if(newData)
                        row.setStyle("");
                    else
                        row.setStyle("-fx-background-color: honeydew;");
                }
            });
            row.itemProperty().addListener((obs, oldData, newData) -> {
                if(newData != null && newData.getCategory().toLowerCase().equals("total")) {
                    row.setStyle("-fx-background-color: honeydew;");
                }
            });
            return row;
        });
        tblFeatures.setItems(lstFeaturesSummary);
        showFeaturesChart(lstFeaturesSummary);
    }
    private void showFeaturesChart(ObservableList<FEAFeaturesSummaryData> lstFeaturesSummary) {
        // this must be done at the feature level not the feature Id level
        // for example, we could have hundreds or even thousands of miRNAs
        HashMap<String, SubTabDFISummary.GeneCounts> hmFeatures = new HashMap<>();
        XYChart.Series<Number, String> series1 = new XYChart.Series<>();
        String[] fields;
        int idx = 0;
        for(FEAFeaturesSummaryData sd : lstFeaturesSummary) {
            // last entry has totals
            if(idx++ < (lstFeaturesSummary.size() - 1)) {
                double p1 = Double.parseDouble(String.format("%.02f", ((double)sd.getEnrichedCount()/(double)sd.getTotalCount()) * 100));
                series1.getData().add(new XYChart.Data<>(p1, sd.getCategory()));
            }
        }
        barFeatures.getData().addAll(series1);
        Tooltip tt;
        for(int i = 0; i < series1.getData().size(); i++) {
            XYChart.Data item = series1.getData().get(i);
            tt = new Tooltip(item.getYValue() + ": " + item.getXValue().toString() + "%");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
        spFeatures.widthProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            barFeatures.setPrefWidth((double)newValue - 20);
            barFeatures.setMaxWidth((double)newValue - 20);
        });
        barFeatures.setPrefWidth((double)spFeatures.getWidth() - 20);
        barFeatures.setMaxWidth((double)spFeatures.getWidth() - 20);
        double h = 100 + Math.max(100, series1.getData().size() * 20);
        barFeatures.setPrefHeight(h);
        barFeatures.setMaxHeight(h);
        app.ctls.setupChartExport(barFeatures, "FEA Enriched Features", "tappAS_FEA_"+feaParams.name.trim()+"_Features_Summary.png", null);
    }

    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        // get test list
        HashMap<String, Object> hmTest = new HashMap<>();
        ArrayList<String> lstItems = Utils.loadSingleItemListFromFile(feaData.getFEATestListFilepath(feaParams.dataType.name(), analysisId), false);
        if(!lstItems.isEmpty()) {
            for(String item : lstItems)
                hmTest.put(item, null);
        }
        HashMap<String, Object> hmBkgnd = new HashMap<>();
        lstItems = Utils.loadSingleItemListFromFile(feaData.getFEABkgndListFilepath(feaParams.dataType.name(), analysisId), false);
        if(!lstItems.isEmpty()) {
            for(String item : lstItems)
                hmBkgnd.put(item, null);
        }
        DataFEA.EnrichedFeaturesData data = feaData.getFEAEnrichedFeaturesData(feaParams.dataType.name(), analysisId, feaParams.sigValue, hmTest, hmBkgnd);
        ObservableList<DataFEA.FEASelectionResults> dataResults = feaData.getFEASelectionResults(feaParams.dataType.name(), analysisId, feaParams.sigValue);
        FXCollections.sort(dataResults);
        showSubTab(data, dataResults);
    }

    //
    // Data Classes
    //
    public static class FEAItemsSummaryData {
        public final SimpleStringProperty item;
        public final SimpleIntegerProperty test;
        public final SimpleDoubleProperty testpct;
        public final SimpleIntegerProperty nontest;
        public final SimpleDoubleProperty nontestpct;
        public final SimpleIntegerProperty total;
 
        public FEAItemsSummaryData(String item, int test, double testpct, int nontest, double nontestpct, int total) {
            this.item = new SimpleStringProperty(item);
            this.test = new SimpleIntegerProperty(test);
            this.testpct = new SimpleDoubleProperty(testpct);
            this.nontest = new SimpleIntegerProperty(nontest);
            this.nontestpct = new SimpleDoubleProperty(nontestpct);
            this.total = new SimpleIntegerProperty(total);
        }
        public String getItem() { return item.get(); }
        public Integer getTest() { return test.get(); }
        public Double getTestPct() { return testpct.get(); }
        public Integer getNonTest() { return nontest.get(); }
        public Double getNonTestPct() { return nontestpct.get(); }
        public Integer getTotal() { return total.get(); }
    }
    public static class FEAFeaturesSummaryData implements Comparable<FEAFeaturesSummaryData> {
        public final SimpleStringProperty cat;
        public final SimpleIntegerProperty enriched;
        public final SimpleIntegerProperty total;
 
        public FEAFeaturesSummaryData(String cat, int enriched, int total) {
            this.cat = new SimpleStringProperty(cat);
            this.enriched = new SimpleIntegerProperty(enriched);
            this.total = new SimpleIntegerProperty(total);
        }
        public String getCategory() { return cat.get(); }
        public Integer getEnrichedCount() { return enriched.get(); }
        public Integer getTotalCount() { return total.get(); }
        
        @Override
        public int compareTo(FEAFeaturesSummaryData td) {
            return (cat.get().compareToIgnoreCase(td.cat.get()));
        }
    }
}
