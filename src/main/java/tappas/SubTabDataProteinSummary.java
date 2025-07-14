/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import tappas.DataApp.DataType;
import tappas.TabProjectDataViz.GeneSummaryData1;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDataProteinSummary extends SubTabBase {
    AnchorPane paneContents;
    TableView tblProtUnique;
    PieChart pieSummaryProtUnique;
    BarChart barProtLength;

    ContextMenu cmProtUnique;
    BoxPlotChart boxPlot;
    ExpLevelDensityPlot eldplot;

    public SubTabDataProteinSummary(Project project) {
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
        tblProtUnique = (TableView) tabNode.lookup("#tblProtUnique");
        pieSummaryProtUnique = (PieChart) tabNode.lookup("#pieSummaryProtUnique");
        barProtLength = (BarChart) tabNode.lookup("#barProtLength");

        // populate transcript coding table
        ObservableList<TableColumn> cols = tblProtUnique.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Field"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Count1"));
        cols.get(2).setCellValueFactory(new PropertyValueFactory<>("Pct1"));
        HashMap<String, Integer> hmProts = project.data.getProteinsTransCounts(project.data.getResultsTrans());
        int totalcnt = hmProts.size();
        int mcnt = 0;
        for(int cnt : hmProts.values())
        {
            if(cnt > 1)
                mcnt++;
        }
        int scnt = totalcnt - mcnt;
        double mpct = 0.0;
        double spct = 0.0;
        if(totalcnt > 0) {
            spct = scnt * 100 / (double)totalcnt;
            mpct = mcnt * 100 / (double)totalcnt;
        }
        spct = Double.parseDouble(String.format("%.2f", ((double)Math.round(spct*100)/100.0)));
        mpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(mpct*100)/100.0)));
        
        ObservableList<GeneSummaryData1> sumdata = FXCollections.observableArrayList();
        sumdata.add(new GeneSummaryData1("Single Transcript", scnt, spct));
        sumdata.add(new GeneSummaryData1("Multiple Transcripts", mcnt, mpct));
        sumdata.add(new GeneSummaryData1("Total", totalcnt, 100.00));
        tblProtUnique.setRowFactory(tableView -> {
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
        app.export.addCopyToClipboardHandler(tblProtUnique);
        tblProtUnique.setItems(sumdata);
        if(tblProtUnique.getItems().size() > 0)
            tblProtUnique.getSelectionModel().select(0);
        tblProtUnique.setOnMouseClicked((event) -> {
            if(cmProtUnique != null)
                cmProtUnique.hide();
            cmProtUnique = null;
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                Node node = event.getPickResult().getIntersectedNode();
                if(node != null) {
                    System.out.println("node: " + node);
                    if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        node = node.getParent();
                        System.out.println("parent: " + node);
                    }
                    if(node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        cmProtUnique = new ContextMenu();
                        app.export.setupSimpleTableExport(tblProtUnique, cmProtUnique, false, "tappAS_Proteins_Summary.tsv");
                        cmProtUnique.setAutoHide(true);
                        cmProtUnique.show(tblProtUnique, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });
        showProtSummaryChart(scnt, spct, mcnt, mpct, totalcnt);
        showProtLengthsChart();
        eldplot.showExpLevelsDensityPlot(DataType.PROTEIN, subTabInfo, tabNode);
        setupExportMenu();
        setFocusNode(tblProtUnique, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export transcripts summary table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblProtUnique, true, "tappAS_Proteins_Summary.tsv"); });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Export transcripts summary pie chart image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = pieSummaryProtUnique.getWidth();
            double ratio = 3840/x;
            pieSummaryProtUnique.setScaleX(ratio);
            pieSummaryProtUnique.setScaleY(ratio);
            WritableImage img = pieSummaryProtUnique.snapshot(sP, null);
            double newX = pieSummaryProtUnique.getWidth();
            ratio = x/newX;
            pieSummaryProtUnique.setScaleX(ratio);
            pieSummaryProtUnique.setScaleY(ratio);
            
            //WritableImage img = pieSummaryProtUnique.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Proteins Summary Chart", "tappAS_Proteins_SummaryChart.png", (Image)img);
        });
        cm.getItems().add(item);
        item = new MenuItem("Export lengths box plot image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = barProtLength.getWidth();
            double ratio = 3840/x;
            barProtLength.setScaleX(ratio);
            barProtLength.setScaleY(ratio);
            WritableImage img = barProtLength.snapshot(sP, null);
            double newX = barProtLength.getWidth();
            ratio = x/newX;
            barProtLength.setScaleX(ratio);
            barProtLength.setScaleY(ratio);
            
            //WritableImage img = barProtLength.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Proteins Lengths Chart", "tappAS_Proteins_Lengths.png", (Image)img);
        });
        cm.getItems().add(item);
        item = new MenuItem("Export density plot image...");
        item.setOnAction((event) -> {
            app.export.exportImage("Proteins Expression Levels Density Plot", "tappAS_Proteins_Density_Plot.png", eldplot.imgChart.getImage());
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
            app.export.exportImage("Proteins Summary Panel", "tappAS_Proteints_Summary_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void showProtSummaryChart(int scnt, double spct, int mcnt, double mpct, int totalcnt) {
        ObservableList<PieChart.Data> pieChartData =  FXCollections.observableArrayList();
        pieChartData.add(new PieChart.Data("Single Transcript", spct));
        pieChartData.add(new PieChart.Data("Multiple Transcripts", mpct));
        pieSummaryProtUnique.setData(pieChartData);
        app.ctls.setupChartExport(pieSummaryProtUnique, "Proteins Summary Chart", "tappAS_Proteins_SummaryChart.png", null);
        Tooltip tt;
        for (PieChart.Data pcdata : pieChartData) {
            tt = new Tooltip("  " + pcdata.getName() + "\n  Percent: " + String.valueOf(pcdata.getPieValue()) + "%  \n  Count: " + (pcdata.getName().equals("Single Transcript")? scnt : mcnt) + " out of " + totalcnt);
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(pcdata.getNode(), tt);
        }
    }
    private void showProtLengthsChart() {
        barProtLength.setTitle("Protein Lengths");
        barProtLength.getYAxis().setLabel("Length (AAs)");
        String[] typename = {"Proteins"};

        ArrayList<Integer>[] values = new ArrayList[typename.length];
        values[0] = project.data.getExpressedProteinsLengthArray(null);
        boxPlot = new BoxPlotChart();
        BoxPlotChart.BoxPlotChartData[] plots = new BoxPlotChart.BoxPlotChartData[typename.length];
        int colorIdx = 0;
        for(int i = 0; i < typename.length; i++) {
            // get length values into array
            double[] vals = new double[values[i].size()];
            int idx = 0;
            for(int val : values[i])
                vals[idx++] = val;
            Stats.BoxPlotData bpd = new Stats.BoxPlotData(vals);
            plots[i] = new BoxPlotChart.BoxPlotChartData(typename[i], bpd, colorIdx++, true);
        }
        boxPlot.initialize(barProtLength, plots);
        app.ctls.setupChartExport(barProtLength, "Proteins Length Distribution Chart", "tappAS_ProtLengthChart.png", null);
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        showSubTab();
    }
}
