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
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabGSEASummary extends SubTabBase {
    AnchorPane paneContents;
    TableView tblItems, tblFeatures;
    ScrollPane spFeatures;
    BarChart barFeatures;
    
    ContextMenu cmSummary;
    DataGSEA gseaData;
    DlgGSEAnalysis.Params gseaParams;
    String analysisId = "";
    
    public SubTabGSEASummary(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            gseaData = new DataGSEA(project);
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectDataViz.Panels.SUMMARYGSEA.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectDataViz.Panels.SUMMARYGSEA.name().length()).trim());
            }
            if(args.containsKey("id")) {
                analysisId = (String) args.get("id");
                gseaParams = DlgGSEAnalysis.Params.load(gseaData.getGSEAParamsFilepath(analysisId));
                String shortName = getShortName(gseaParams.name);
                subTabInfo.title = "GSEA: " + shortName;
                subTabInfo.tooltip = "Gene Set Enrichment Analysis summary for '" + gseaParams.name.trim() + "'";
                setTabLabel(subTabInfo.title, subTabInfo.tooltip);
            }
            else
                result = false;
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }

    //
    // Internal Functions
    //

    //We changed input FEA by GSEA - dont know why was FEA
    private void showSubTab(DataGSEA.EnrichedFeaturesData data, ObservableList<DataGSEA.GSEASelectionResults> dataResults) {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
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
                        app.export.setupSimpleTableExport(tblItems, cmSummary, false, "tappAS_GSEA_"+gseaParams.name.trim()+"_Summary.tsv");
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
                        app.export.setupSimpleTableExport(tblFeatures, cmSummary, false, "tappAS_GSEA_"+gseaParams.name.trim()+"_Features_Summary.tsv");
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
        item.setOnAction((event) -> { app.export.exportTableData(tblItems, true, "tappAS_GSEA_"+gseaParams.name.trim()+"_Summary.tsv"); });
        cm.getItems().add(item);
        item = new MenuItem("Export features table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblFeatures, true, "tappAS_GSEA_"+gseaParams.name.trim()+"_Features_Summary.tsv"); });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
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
            app.export.exportImage("GSEA Enriched Features", "tappAS_GSEA_"+gseaParams.name.trim()+"_Features_Summary.png", (Image)img);
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
            app.export.exportImage("GSEA Summary Panel", "tappAS_GSEA_"+gseaParams.name.trim()+"_Summary_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    }

    //We changed FEA by GSEA - dont know why FEA input
    private void showSummary(DataGSEA.EnrichedFeaturesData data, ObservableList<DataGSEA.GSEASelectionResults> dataResults) {
//        String itemsName = Utils.properCase(app.data.getDataTypePlural(gseaParams.dataType.name()));
//        ObservableList<GeneSummaryData1> sumdata = null;
//        // items table
//        double testpct1 = 0.0;
//        double testpct2 = 0.0;
//        if(gseaParams.useMulti.equals("TRUE")){
//            if(data.items_rank1 > 0) {
//                testpct1 = gseaData.items_rank1 * 100 / project.data.getResultsGenes().size();
//                testpct1 = Double.parseDouble(String.format("%.2f", ((double)Math.round(testpct1*100)/100.0)));
//            }
//            if(data.items_rank2 > 0) {
//                testpct2 = data.items_rank2 * 100 / project.data.getResultsGenes().size();
//                testpct2 = Double.parseDouble(String.format("%.2f", ((double)Math.round(testpct2*100)/100.0)));
//            }
//            sumdata = FXCollections.observableArrayList();
//            sumdata.add(new GeneSummaryData1("Ranked List " + gseaParams.rankedList1.name() + " - " + itemsName, data.items_rank1, testpct1));
//            sumdata.add(new GeneSummaryData1("Ranked List " + gseaParams.rankedList2.name() + " - " + itemsName, data.items_rank2, testpct2));
//            sumdata.add(new GeneSummaryData1("Total", project.data.getResultsGenes().size(), 100.00));
//            tblItems.setItems(sumdata);
//        }else{
//            if(gseaData.items_rank1 > 0) {
//                testpct1 =  data.items_rank1 * 100 / project.data.getResultsGenes().size();
//                testpct1 = Double.parseDouble(String.format("%.2f", ((double)Math.round(testpct1*100)/100.0)));
//            }
//            sumdata = FXCollections.observableArrayList();
//            sumdata.add(new GeneSummaryData1("Ranked List " + gseaParams.rankedList1.name() + " - " + itemsName, data.items_rank1, testpct1));
//            sumdata.add(new GeneSummaryData1("Total", project.data.getResultsGenes().size(), 100.00));
//            tblItems.setItems(sumdata);
//        }
//
//        if(!sumdata.isEmpty())
//            tblItems.getSelectionModel().select(0);
//        tblItems.setRowFactory(tableView -> {
//            TableRow<GeneSummaryData1> row = new TableRow<>();
//            row.selectedProperty().addListener((obs, oldData, newData) -> {
//                GeneSummaryData1 sd = row.getItem();
//                if(sd != null && sd.getField().toLowerCase().equals("total")) {
//                    if(newData)
//                        row.setStyle("");
//                    else
//                        row.setStyle("-fx-background-color: honeydew;");
//                }
//            });
//            row.itemProperty().addListener((obs, oldData, newData) -> {
//                if(newData != null && newData.getField().toLowerCase().equals("total"))
//                    row.setStyle("-fx-background-color: honeydew;");
//            });
//            return row;
//        });
//        app.export.addCopyToClipboardHandler(tblItems);

        tblItems.setVisible(false);

        // features table
        ObservableList<GSEAFeaturesSummaryData> lstFeaturesSummary = FXCollections.observableArrayList();
        HashMap<String, Integer> hmCat = new HashMap<>();
        for(DataGSEA.GSEAStatsData stats : data.lstStats) {
            if(!hmCat.containsKey(stats.getCategory())) {
                hmCat.put(stats.getCategory(), 1);
            }
            else
                hmCat.put(stats.getCategory(), (hmCat.get(stats.getCategory()) + 1));
        }
        int cntTotal = 0;
        int cntEnriched = 0;
        // add pct?
        HashMap<String, Integer> hmNotEnriched = new HashMap<>();
        for(String cat : hmCat.keySet()) {
            int total = 0;
            for(DataGSEA.GSEASelectionResults fear : dataResults) {
                if(fear.getFeature().equals(cat))
                    total++;
            }
            cntTotal += total;
            cntEnriched += hmCat.get(cat);
            lstFeaturesSummary.add(new GSEAFeaturesSummaryData(cat, hmCat.get(cat), total));
        }
        for(DataGSEA.GSEASelectionResults fear : dataResults) {
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
            lstFeaturesSummary.add(new GSEAFeaturesSummaryData(cat, 0, hmNotEnriched.get(cat)));
            cntTotal += hmNotEnriched.get(cat);
        }
        Collections.sort(lstFeaturesSummary);
        lstFeaturesSummary.add(new GSEAFeaturesSummaryData("Total", cntEnriched, cntTotal));

        tblFeatures.setRowFactory(tableView -> {
            TableRow<GSEAFeaturesSummaryData> row = new TableRow<>();
            row.selectedProperty().addListener((obs, oldData, newData) -> {
                GSEAFeaturesSummaryData sd = row.getItem();
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
    private void showFeaturesChart(ObservableList<GSEAFeaturesSummaryData> lstFeaturesSummary) {
        // this must be done at the feature level not the feature Id level
        // for example, we could have hundreds or even thousands of miRNAs
        HashMap<String, SubTabDFISummary.GeneCounts> hmFeatures = new HashMap<>();
        XYChart.Series<Number, String> series1 = new XYChart.Series<>();
        String[] fields;
        int idx = 0;
        for(GSEAFeaturesSummaryData sd : lstFeaturesSummary) {
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
        app.ctls.setupChartExport(barFeatures, "GSEA Enriched Features", "tappAS_GSEA_"+gseaParams.name.trim()+"_Features_Summary.png", null);
    }

    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        // get ranked list
        // we cahnged the method to return DataGSEA.EnrichmedFeaturesData data - before was DataFEA.EnrichmedFeturesData
        HashMap<String, Object> hmTest = new HashMap<>();
        // CHANGE TO BE ABLE TO GET 2 RANKED LIST !!!
        ArrayList<String> lstItems = Utils.loadSingleItemListFromFile(gseaData.getGSEARankedListFilepath(gseaParams.dataType.name().toLowerCase(), analysisId, 1), false);
        for(String item : lstItems)
            hmTest.put(item, null);
        // CHANGE TO BE ABLE TO GET 2 RANKED LIST !!!
        DataGSEA.EnrichedFeaturesData data = gseaData.getGSEAEnrichedFeaturesData(gseaParams.dataType.name(), analysisId, gseaParams.sigValue);
        ObservableList<DataGSEA.GSEASelectionResults> dataResults = gseaData.getGSEASelectionResults(gseaParams.dataType.name(), analysisId, gseaParams.sigValue);
        FXCollections.sort(dataResults);
        showSubTab(data, dataResults);
    }

    //
    // Data Classes
    //
    public static class GSEAItemsSummaryData {
        public final SimpleStringProperty item;
        public final SimpleIntegerProperty test;
        public final SimpleDoubleProperty testpct;
        public final SimpleIntegerProperty nontest;
        public final SimpleDoubleProperty nontestpct;
        public final SimpleIntegerProperty total;
 
        public GSEAItemsSummaryData(String item, int test, double testpct, int nontest, double nontestpct, int total) {
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
    public static class GSEAFeaturesSummaryData implements Comparable<GSEAFeaturesSummaryData> {
        public final SimpleStringProperty cat;
        public final SimpleIntegerProperty enriched;
        public final SimpleIntegerProperty total;
 
        public GSEAFeaturesSummaryData(String cat, int enriched, int total) {
            this.cat = new SimpleStringProperty(cat);
            this.enriched = new SimpleIntegerProperty(enriched);
            this.total = new SimpleIntegerProperty(total);
        }
        public String getCategory() { return cat.get(); }
        public Integer getEnrichedCount() { return enriched.get(); }
        public Integer getTotalCount() { return total.get(); }
        
        @Override
        public int compareTo(GSEAFeaturesSummaryData td) {
            return (cat.get().compareToIgnoreCase(td.cat.get()));
        }
    }
}
