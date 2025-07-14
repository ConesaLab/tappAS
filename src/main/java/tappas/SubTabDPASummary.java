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
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDPASummary extends SubTabBase {
    AnchorPane paneContents;
    ScrollPane spCondition, spGenes;
    BarChart barGenes;
    StackedBarChart barCondition;
    
    ContextMenu cmDiversity;
    DataDPA dpaData;
    DlgDPAnalysis.Params dpaParams;
    
    public SubTabDPASummary(Project project) {
        super(project);
    }
    
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            dpaData = new DataDPA(project);
            result = dpaData.hasDPAData();
            if(result)
                dpaParams = DlgDPAnalysis.Params.load(dpaData.getDPAParamsFilepath(), project);
            else
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    
    //
    // Internal Functions
    //
    private void showSubTab() {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        spCondition = (ScrollPane) tabNode.getParent().lookup("#spCondition");
        barCondition = (StackedBarChart) spCondition.getContent().lookup("#barCondition");
        spGenes = (ScrollPane) tabNode.getParent().lookup("#spGenes");
        barGenes = (BarChart) spGenes.getContent().lookup("#barGenes");
        showConditionDPAChart();
        showGenesDPAChart();
        setupExportMenu();
        setFocusNode(barCondition, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item;
        if(!project.data.isSingleTimeSeriesExpType()) {
            item = new MenuItem("Export DPA Condition chart image...");
            item.setOnAction((event) -> { 
                SnapshotParameters sP = new SnapshotParameters();
                double x = barCondition.getWidth();
                double ratio = 3840/x;
                barCondition.setScaleX(ratio);
                barCondition.setScaleY(ratio);
                WritableImage img = barCondition.snapshot(sP, null);
                double newX = barCondition.getWidth();
                ratio = x/newX;
                barCondition.setScaleX(ratio);
                barCondition.setScaleY(ratio);
                
                //WritableImage img = barCondition.snapshot(new SnapshotParameters(), null);
                app.export.exportImage("DPA Condition Chart", "tappAS_DPA_Condition.png", (Image)img);
            });
            cm.getItems().add(item);
        }
        item = new MenuItem("Export DPA Condition Genes chart image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = barGenes.getWidth();
            double ratio = 3840/x;
            barGenes.setScaleX(ratio);
            barGenes.setScaleY(ratio);
            WritableImage img = barGenes.snapshot(sP, null);
            double newX = barGenes.getWidth();
            ratio = x/newX;
            barGenes.setScaleX(ratio);
            barGenes.setScaleY(ratio);
            
            //WritableImage img = barGenes.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("DPA Condition Genes Chart", "tappAS_DPA_Condition_Genes.png", (Image)img);
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
            app.export.exportImage("DPA Summary Panel", "tappAS_DPA_Summary_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void showConditionDPAChart() {
        // need to get the favored worked out for time series - add later!!!
        if(project.data.isTimeCourseExpType()) {
            spCondition.setVisible(false);
            return;
        }
        
        spCondition.widthProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            barCondition.setPrefWidth((double)newValue - 20);
            barCondition.setMaxWidth((double)newValue - 20);
        });
        // this must be done at the feature level not the feature Id level
        // for example, we could have hundreds or even thousands of miRNAs
        HashMap<String, Counts> hmCondition = new HashMap<>();
        String[] names = project.data.getGroupNames();
        int grpcnt = project.data.getGroupsCount();
        ArrayList<DataDPA.DPAResultsData> lst = dpaData.getDPAResultsData(dpaParams.sigValue);
        ArrayList<String> lstCondition = new ArrayList<>();
        for(DataDPA.DPAResultsData rd : lst) {
            String key = rd.gene;
            if(hmCondition.containsKey(key)) {
                Counts cnts = hmCondition.get(key);
                if(rd.ds) {
                    if(rd.favored.equals(names[0]))
                        cnts.dsc1++;
                    else if(rd.favored.equals(names[1]))
                        cnts.dsc2++;
                }
                cnts.total++;
            }
        }
        Collections.sort(lstCondition, String.CASE_INSENSITIVE_ORDER);
        XYChart.Series<Number, String> series1 = new XYChart.Series<>();
        series1.setName(names[0] + " Favored");
        XYChart.Series<Number, String> series2 = new XYChart.Series<>();
        series2.setName(names[1] + " Favored");
        String[] fields;
        for(String key : lstCondition) {
            fields = key.split("\t");
            Counts cnts = hmCondition.get(fields[1].trim());
            double p1 = Double.parseDouble(String.format("%.02f", ((double)cnts.dsc1/(double)cnts.total) * 100));
            double p2 = Double.parseDouble(String.format("%.02f", ((double)cnts.dsc2/(double)cnts.total) * 100));
            series1.getData().add(new XYChart.Data(p1, cnts.lengthTrans));
            series2.getData().add(new XYChart.Data(p2, cnts.lengthTrans));
        }
        barCondition.setPrefWidth((double)spCondition.getWidth() - 20);
        barCondition.setMaxWidth((double)spCondition.getWidth() - 20);
        barCondition.getData().addAll(series1, series2);
        double h = 120 + Math.max(100, (series1.getData().size() + 2) * 20);
        barCondition.setPrefHeight(h);
        barCondition.setMaxHeight(h);
        Tooltip tt;
        for(int i = 0; i < series1.getData().size(); i++) {
            XYChart.Data item = series1.getData().get(i);
            tt = new Tooltip(item.getYValue() + " (" + (names[0] + " Favored)") + ": " + item.getXValue().toString() + "%");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
        for(int i = 0; i < series2.getData().size(); i++) {
            XYChart.Data item = series2.getData().get(i);
            tt = new Tooltip(item.getYValue() + " (" + (names[1] + " Favored)") + ": " + item.getXValue().toString() + "%");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
        app.ctls.setupChartExport(barCondition, "DPA Condition", "tappAS_DPA_Condition.png", null);
    }
    private void showGenesDPAChart() {
        spGenes.widthProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            barGenes.setPrefWidth((double)newValue - 20);
            barGenes.setMaxWidth((double)newValue - 20);
        });
        // this must be done at the feature level not the feature Id level
        // for example, we could have hundreds or even thousands of miRNAs
        HashMap<String, GeneCounts> hmCondition = new HashMap<>();
        ArrayList<DataDPA.DPAResultsData> lst = dpaData.getDPAResultsData(dpaParams.sigValue);
        ArrayList<String> lstCondition = new ArrayList<>();
        for(DataDPA.DPAResultsData rd : lst) {
            String key = rd.gene;
            if(hmCondition.containsKey(key)) {
                GeneCounts cnts = hmCondition.get(key);
                cnts.updateGene(rd.gene, rd.ds);
            }else{
                GeneCounts cnts = new GeneCounts(rd.gene, rd.ds);
                hmCondition.put(key, cnts);
                lstCondition.add(key);
            }
        }
        Collections.sort(lstCondition, String.CASE_INSENSITIVE_ORDER);
        XYChart.Series<Number, String> series1 = new XYChart.Series<>();
        for(String key : lstCondition) {
            GeneCounts cnts = hmCondition.get(key);
            double p1 = Double.parseDouble(String.format("%.02f", ((double)cnts.hmDPAGenes.size()/(double)cnts.hmTotalGenes.size()) * 100));
            series1.getData().add(new XYChart.Data<>(p1, cnts.lengthTrans));
        }
        barGenes.setPrefWidth((double)spGenes.getWidth() - 20);
        barGenes.setMaxWidth((double)spGenes.getWidth() - 20);
        barGenes.getData().addAll(series1);
        double h = 100 + Math.max(100, series1.getData().size() * 20);
        barGenes.setPrefHeight(h);
        barGenes.setMaxHeight(h);
        Tooltip tt;
        for(int i = 0; i < series1.getData().size(); i++) {
            XYChart.Data item = series1.getData().get(i);
            tt = new Tooltip(item.getYValue() + ": " + item.getXValue().toString() + "%");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
        app.ctls.setupChartExport(barGenes, "DPA Condition Genes", "tappAS_DPA_Condition_Genes.png", null);
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        showSubTab();
    }
    
    //
    //Data Classes
    //
    public static class VarSummaryData {
        public final SimpleStringProperty region;
        public final SimpleIntegerProperty genes;
        public final SimpleDoubleProperty pct;

        public VarSummaryData(String region, int genes, double pct) {
            this.region = new SimpleStringProperty(region);
            this.genes = new SimpleIntegerProperty(genes);
            this.pct = new SimpleDoubleProperty(pct);
        }
        public String getRegion() { return region.get(); }
        public int getGenes() { return genes.get(); }
        public double getPct() { return pct.get(); }
    }
    public static class Counts {
        public String lengthTrans;
        public int dsc1, dsc2;
        public int total;

        public Counts(String length, int dsc1, int dsc2, int total) {
            this.lengthTrans = length;
            this.dsc1 = dsc1;
            this.dsc2 = dsc2;
            this.total = total;
        }
    }
    public static class GeneCounts {
        public String lengthTrans;
        public HashMap<String, Object> hmDPAGenes;
        public HashMap<String, Object> hmTotalGenes;

        public GeneCounts(String gene, boolean ds) {
            //this.lengthTrans = feature;
            this.hmDPAGenes = new HashMap<>();
            this.hmTotalGenes = new HashMap<>();
            if(ds)
                hmDPAGenes.put(gene, null);
            hmTotalGenes.put(gene, null);
        }
        public void updateGene(String gene, boolean ds) {
            // ok to re-add instead of checking if already there
            if(ds)
                hmDPAGenes.put(gene, null);
            hmTotalGenes.put(gene, null);
        }
    }
}
