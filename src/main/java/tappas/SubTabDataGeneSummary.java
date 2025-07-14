/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import tappas.DataApp.DataType;
import tappas.TabProjectDataViz.GeneSummaryData1;

import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDataGeneSummary extends SubTabBase {
    AnchorPane paneContents;
    PieChart pieSummaryGeneIso, pieSummaryGeneProt;
    TableView tblGeneSummaryIso, tblGeneSummaryProt;
    
    ContextMenu cmGeneSummaryIso, cmGeneSummaryProt;
    ExpLevelDensityPlot eldplot;
    
    public SubTabDataGeneSummary(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            eldplot = new ExpLevelDensityPlot(project);
        }
        return result;
    }
    
    //
    // Internal Functions
    //
    private void showSubTab() {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        tblGeneSummaryIso = (TableView) tabNode.lookup("#tblGeneSummaryIso");

        // populate transcript coding table
        ObservableList<TableColumn> cols = tblGeneSummaryIso.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Field"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Count1"));
        cols.get(2).setCellValueFactory(new PropertyValueFactory<>("Pct1"));
        HashMap<String, Integer> hmCnt = project.data.getExpressedGeneIsoformsCount(project.data.getResultsTrans());
        int totalcnt = hmCnt.size();
        int scnt = 0;
        int mcnt = 0;
        for(int cnt : hmCnt.values()) {
            if(cnt > 1)
                mcnt++;
            else
                scnt++;
        }
        double spct = 0.0;
        double mpct = 0.0;
        if(totalcnt > 0) {
            spct = scnt * 100 / (double)totalcnt;
            mpct = mcnt * 100 / (double)totalcnt;
        }
        spct = Double.parseDouble(String.format("%.2f", ((double)Math.round(spct*100)/100.0)));
        mpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(mpct*100)/100.0)));
        ObservableList<GeneSummaryData1> sumdata = FXCollections.observableArrayList();
        sumdata.add(new GeneSummaryData1("with Single Transcript", scnt, spct));
        sumdata.add(new GeneSummaryData1("with Multiple Transcripts", mcnt, mpct));
        sumdata.add(new GeneSummaryData1("Total", totalcnt, 100.00));
        tblGeneSummaryIso.setRowFactory(tableView -> {
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
        tblGeneSummaryIso.setOnMouseClicked((event) -> {
            if(cmGeneSummaryIso != null)
                cmGeneSummaryIso.hide();
            cmGeneSummaryIso = null;
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                Node node = event.getPickResult().getIntersectedNode();
                if(node != null) {
                    System.out.println("node: " + node);
                    if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        node = node.getParent();
                        System.out.println("parent: " + node);
                    }
                    if(node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        cmGeneSummaryIso = new ContextMenu();
                        app.export.setupSimpleTableExport(tblGeneSummaryIso, cmGeneSummaryIso, false, "tappAS_GeneIsoSummary.tsv");
                        cmGeneSummaryIso.setAutoHide(true);
                        cmGeneSummaryIso.show(tblGeneSummaryIso, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });
        app.export.addCopyToClipboardHandler(tblGeneSummaryIso);
        tblGeneSummaryIso.setItems(sumdata);
        if(tblGeneSummaryIso.getItems().size() > 0)
            tblGeneSummaryIso.getSelectionModel().select(0);

        tblGeneSummaryProt = (TableView) tabNode.lookup("#tblGeneSummaryProt");
        cols = tblGeneSummaryProt.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Field"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Count1"));
        cols.get(2).setCellValueFactory(new PropertyValueFactory<>("Pct1"));
        hmCnt = project.data.getExpressedGeneAssignedProteinsCount(project.data.getResultsTrans());
        totalcnt = hmCnt.size();
        int zcnt = 0;
        scnt = 0;
        mcnt = 0;
        for(int cnt : hmCnt.values()) {
            if(cnt == 0)
                zcnt++;
            else if(cnt == 1)
                scnt++;
            else
                mcnt++;
        }
        double zpct = 0.0;
        spct = 0.0;
        mpct = 0.0;
        if(totalcnt > 0) {
            zpct = zcnt * 100 / (double)totalcnt;
            spct = scnt * 100 / (double)totalcnt;
            mpct = mcnt * 100 / (double)totalcnt;
        }
        zpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(zpct*100)/100.0)));
        spct = Double.parseDouble(String.format("%.2f", ((double)Math.round(spct*100)/100.0)));
        mpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(mpct*100)/100.0)));
        sumdata = FXCollections.observableArrayList();
        sumdata.add(new GeneSummaryData1("Non-coding", zcnt, zpct));
        sumdata.add(new GeneSummaryData1("Coding: One Protein", scnt, spct));
        sumdata.add(new GeneSummaryData1("Coding: Multiple Proteins", mcnt, mpct));
        sumdata.add(new GeneSummaryData1("Total", totalcnt, 100.00));
        app.export.addCopyToClipboardHandler(tblGeneSummaryProt);
        tblGeneSummaryProt.setRowFactory(tableView -> {
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
        tblGeneSummaryProt.setOnMouseClicked((event) -> {
            if(cmGeneSummaryProt != null)
                cmGeneSummaryProt.hide();
            cmGeneSummaryProt = null;
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                Node node = event.getPickResult().getIntersectedNode();
                if(node != null) {
                    System.out.println("node: " + node);
                    if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        node = node.getParent();
                        System.out.println("parent: " + node);
                    }
                    if(node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        cmGeneSummaryProt = new ContextMenu();
                        app.export.setupSimpleTableExport(tblGeneSummaryProt, cmGeneSummaryProt, false, "tappAS_GeneProteinSummary.tsv");
                        cmGeneSummaryProt.setAutoHide(true);
                        cmGeneSummaryProt.show(tblGeneSummaryProt, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });
        tblGeneSummaryProt.setItems(sumdata);
        if(tblGeneSummaryProt.getItems().size() > 0)
            tblGeneSummaryProt.getSelectionModel().select(0);
        showGeneIsoChart();
        showGeneProtChart();
        eldplot.showExpLevelsDensityPlot(DataType.GENE, subTabInfo, tabNode);

        // manage focus on both tables
        tblGeneSummaryIso.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue)
                onSelectFocusNode = tblGeneSummaryIso;
        });
        tblGeneSummaryProt.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue)
                onSelectFocusNode = tblGeneSummaryProt;
        });
        setupExportMenu();
        setFocusNode(tblGeneSummaryIso, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export coding summary table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblGeneSummaryProt, true, "tappAS_Gene_Summary_Coding.tsv"); });
        cm.getItems().add(item);
        item = new MenuItem("Export transcripts summary table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblGeneSummaryIso, true, "tappAS_Gene_Summary_Transcripts.tsv"); });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Export coding summary chart image...");
        item.setOnAction((event) -> { 
            WritableImage img = pieSummaryGeneProt.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Gene Coding Summary Chart", "tappAS_Gene_Summary_Coding.png", (Image)img);
        });
        cm.getItems().add(item);
        item = new MenuItem("Export transcripts summary chart image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = pieSummaryGeneIso.getWidth();
            double ratio = 3840/x;
            pieSummaryGeneIso.setScaleX(ratio);
            pieSummaryGeneIso.setScaleY(ratio);
            WritableImage img = pieSummaryGeneIso.snapshot(sP, null);
            double newX = pieSummaryGeneIso.getWidth();
            ratio = x/newX;
            pieSummaryGeneIso.setScaleX(ratio);
            pieSummaryGeneIso.setScaleY(ratio);
            
            //WritableImage img = pieSummaryGeneIso.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Gene Transcripts Summary Chart", "tappAS_Gene_Summary_Transcripts.png", (Image)img);
        });
        cm.getItems().add(item);
        item = new MenuItem("Export density plot image...");
        item.setOnAction((event) -> {
            app.export.exportImage("Gene Expression Levels Density Plot", "tappAS_Gene_Density_Plot.png", eldplot.imgChart.getImage());
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
            app.export.exportImage("Gene Summary Panel", "tappAS_Gene_Summary_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void showGeneIsoChart() {
        HashMap<String, Integer> hmCnt = project.data.getExpressedGeneIsoformsCount(project.data.getResultsTrans());
        int totalcnt = hmCnt.size();
        int scnt = 0;
        int mcnt = 0;
        for(int cnt : hmCnt.values()) {
            if(cnt > 1)
                mcnt++;
            else
                scnt++;
        }
        double spct = 0.0;
        double mpct = 0.0;
        if(totalcnt > 0) {
            spct = scnt * 100 / (double)totalcnt;
            mpct = mcnt * 100 / (double)totalcnt;
        }
        spct = Double.parseDouble(String.format("%.2f", ((double)Math.round(spct*100)/100.0)));
        mpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(mpct*100)/100.0)));

        ObservableList<PieChart.Data> pieChartData =  FXCollections.observableArrayList();
        pieChartData.add(new PieChart.Data("Single Transcript", spct));
        pieChartData.add(new PieChart.Data("Multiple Transcripts", mpct));
        pieSummaryGeneIso = (PieChart) tabNode.lookup("#pieSummaryGeneIso");
        pieSummaryGeneIso.setData(pieChartData);
        app.ctls.setupChartExport(pieSummaryGeneIso, "Gene Transcripts Chart", "tappAS_GeneTranscriptsChart.png", null);
        Tooltip tt;
        for (PieChart.Data pcdata : pieChartData) {
            tt = new Tooltip("  " + pcdata.getName() + "\n  Percent: " + String.valueOf(pcdata.getPieValue()) + "%  \n  Count: " + (pcdata.getName().equals("Single Transcript")? scnt : mcnt) + " out of " + totalcnt + " genes");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(pcdata.getNode(), tt);
        }
    }
    private void showGeneProtChart() {
        HashMap<String, Integer> hmCnt = project.data.getExpressedGeneAssignedProteinsCount(project.data.getResultsTrans());
        int totalcnt = hmCnt.size();
        int zcnt = 0;
        int scnt = 0;
        int mcnt = 0;
        for(int cnt : hmCnt.values()) {
            if(cnt == 0)
                zcnt++;
            else if(cnt == 1)
                scnt++;
            else
                mcnt++;
        }
        double zpct = 0.0;
        double spct = 0.0;
        double mpct = 0.0;
        if(totalcnt > 0) {
            zpct = zcnt * 100 / (double)totalcnt;
            spct = scnt * 100 / (double)totalcnt;
            mpct = mcnt * 100 / (double)totalcnt;
        }
        zpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(zpct*100)/100.0)));
        spct = Double.parseDouble(String.format("%.2f", ((double)Math.round(spct*100)/100.0)));
        mpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(mpct*100)/100.0)));

        ObservableList<PieChart.Data> pieChartData =  FXCollections.observableArrayList();
        pieChartData.add(new PieChart.Data("No Proteins", zpct));
        pieChartData.add(new PieChart.Data("Single Protein", spct));
        pieChartData.add(new PieChart.Data("Multiple Proteins", mpct));
        pieSummaryGeneProt = (PieChart) tabNode.lookup("#pieSummaryGeneProt");
        pieSummaryGeneProt.setData(pieChartData);
        app.ctls.setupChartExport(pieSummaryGeneProt, "Gene Proteins Chart", "tappAS_GeneProteinsChart.png", null);
        Tooltip tt;
        for (PieChart.Data pcdata : pieChartData) {
            tt = new Tooltip("  " + pcdata.getName() + "\n  Percent: " + String.valueOf(pcdata.getPieValue()) + "%  \n  Count: " + (pcdata.getName().equals("No Proteins")? zcnt : (pcdata.getName().equals("Single Protein")? scnt : mcnt)) + " out of " + totalcnt + " genes");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(pcdata.getNode(), tt);
        }
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        showSubTab();
    }
}
