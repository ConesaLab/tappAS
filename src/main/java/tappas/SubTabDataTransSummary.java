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
public class SubTabDataTransSummary extends SubTabBase {
    AnchorPane paneContents;
    TableView tblTransCoding;
    PieChart pieSummaryTransCoding;
    PieChart pieSummaryTransStructCats;
    BarChart barTransLength;
    
    ContextMenu cmTransCoding;
    ExpLevelDensityPlot eldplot;
    BoxPlotChart boxPlot;

    public SubTabDataTransSummary(Project project) {
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
        tblTransCoding = (TableView) tabNode.lookup("#tblTransCoding");
        barTransLength = (BarChart) tabNode.lookup("#barTransLength");
        pieSummaryTransCoding = (PieChart) tabNode.lookup("#pieSummaryTransCoding");
        pieSummaryTransStructCats = (PieChart) tabNode.lookup("#pieSummaryTransStructCats");

        // populate transcript coding table
        ObservableList<TableColumn> cols = tblTransCoding.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Field"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Count1"));
        cols.get(2).setCellValueFactory(new PropertyValueFactory<>("Pct1"));

        HashMap<String, Boolean> hmCoding = project.data.getTransCoding(project.data.getResultsTrans());
        int totalcnt = hmCoding.size();
        int scnt = 0;
        int mcnt = 0;
        for(boolean coding : hmCoding.values()) {
            if(coding)
                scnt++;
            else
                mcnt++;
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
        sumdata.add(new GeneSummaryData1("Coding", scnt, spct));
        sumdata.add(new GeneSummaryData1("Non-Coding", mcnt, mpct));
        sumdata.add(new GeneSummaryData1("Total", totalcnt, 100.00));
        tblTransCoding.setRowFactory(tableView -> {
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
        app.export.addCopyToClipboardHandler(tblTransCoding);
        tblTransCoding.setItems(sumdata);
        if(tblTransCoding.getItems().size() > 0)
            tblTransCoding.getSelectionModel().select(0);
        tblTransCoding.setOnMouseClicked((event) -> {
            if(cmTransCoding != null)
                cmTransCoding.hide();
            cmTransCoding = null;
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                Node node = event.getPickResult().getIntersectedNode();
                if(node != null) {
                    System.out.println("node: " + node);
                    if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        node = node.getParent();
                        System.out.println("parent: " + node);
                    }
                    if(node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        cmTransCoding = new ContextMenu();
                        app.export.setupSimpleTableExport(tblTransCoding, cmTransCoding, false, "tappAS_SummaryTrans.tsv");
                        cmTransCoding.setAutoHide(true);
                        cmTransCoding.show(tblTransCoding, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });
        // display charts
        showTransCodingChart(hmCoding);
        showTransLengthsChart();
        eldplot.showExpLevelsDensityPlot(DataType.TRANS, subTabInfo, tabNode);
        showTransStructCatsChart(pieSummaryTransStructCats);
        setupExportMenu();
        setFocusNode(tblTransCoding, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export coding summary table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblTransCoding, true, "tappAS_Transcripts_Coding.tsv"); });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Export coding summary pie chart image...");
        item.setOnAction((event) -> { 
            WritableImage img = pieSummaryTransCoding.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Transcripts Summary Chart", "tappAS_Transcripts_Coding.png", (Image)img);
        });
        cm.getItems().add(item);
        item = new MenuItem("Export lengths box plot image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = barTransLength.getWidth();
            double ratio = 3840/x;
            barTransLength.setScaleX(ratio);
            barTransLength.setScaleY(ratio);
            WritableImage img = barTransLength.snapshot(sP, null);
            double newX = barTransLength.getWidth();
            ratio = x/newX;
            barTransLength.setScaleX(ratio);
            barTransLength.setScaleY(ratio);
            
            //WritableImage img = barTransLength.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Transcripts Lengths Chart", "tappAS_Transcripts_Lengths.png", (Image)img);
        });
        cm.getItems().add(item);
        item = new MenuItem("Export density plot image...");
        item.setOnAction((event) -> {
            app.export.exportImage("Expression Levels Density Plot", "tappAS_Transcripts_Density_Plot.png", eldplot.imgChart.getImage());
        });
        cm.getItems().add(item);
        item = new MenuItem("Export structural categories chart image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = pieSummaryTransStructCats.getWidth();
            double ratio = 3840/x;
            pieSummaryTransStructCats.setScaleX(ratio);
            pieSummaryTransStructCats.setScaleY(ratio);
            WritableImage img = pieSummaryTransStructCats.snapshot(sP, null);
            double newX = pieSummaryTransStructCats.getWidth();
            ratio = x/newX;
            pieSummaryTransStructCats.setScaleX(ratio);
            pieSummaryTransStructCats.setScaleY(ratio);
            
            //WritableImage img = pieSummaryTransStructCats.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Structural Categories", "tappAS_Transcripts_Structural_Categories.png", (Image)img);
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
            app.export.exportImage("Transcripts Summary Panel", "tappAS_Transcripts_Summary_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void showTransLengthsChart() {
        barTransLength.setTitle("Transcript Lengths\n");
        barTransLength.getYAxis().setLabel("Length (NTs)");
        String[] typename = {"Transcripts"};
        ArrayList<Integer>[] values = new ArrayList[typename.length];
        values[0] = project.data.getTransLengthArray(project.data.getResultsGeneTrans());
        boxPlot = new BoxPlotChart();
        BoxPlotChart.BoxPlotChartData[] plots = new BoxPlotChart.BoxPlotChartData[typename.length];
        int colorIdx = 0;
        for(int i = 0; i < typename.length; i++) {
            // get length values into array
            double[] vals = new double[values[i].size()];
            int idx = 0;
            for(int val : values[i])
                vals[idx++] = val;
            plots[i] = new BoxPlotChart.BoxPlotChartData(typename[i], new Stats.BoxPlotData(vals), colorIdx++, true);
        }
        boxPlot.initialize(barTransLength, plots);
        app.ctls.setupChartExport(barTransLength, "Transcripts Length Distribution Chart", "tappAS_TransLengthChart.png", null);
    }
    private void showTransStructCatsChart(PieChart pieChart) {
        String name = "Transcript";
        HashMap<String, Integer> hmAlignCats = project.data.getAlignmentCategoriesTransCount(project.data.getResultsTrans());
        ArrayList<String> lstOrder = project.data.getAlignmentCategoriesDisplayOrder();
        ObservableList<PieChart.Data> pieChartData =  FXCollections.observableArrayList();
        int totalcnt = 0;
        for(String cat : lstOrder) {
            if(hmAlignCats.containsKey(cat))
                totalcnt += hmAlignCats.get(cat);
        }
        if(totalcnt > 0) {
            for(String cat : lstOrder) {
                if(hmAlignCats.containsKey(cat)) {
                    double pct = hmAlignCats.get(cat) * 100 / (double)totalcnt;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    pieChartData.add(new PieChart.Data(cat, pct));
                }
            }
        }
        pieChart.setData(pieChartData);
        app.ctls.setupChartExport(pieChart, name + "s Structural Categories Chart", "tappAS_" + name + "_StructCatsChart.png", null);
        Tooltip tt;
        for (PieChart.Data pcdata : pieChartData) {
            tt = new Tooltip("  " + pcdata.getName() + "\n  Percent: " + String.valueOf(pcdata.getPieValue()) + "%  \n  Count: " + hmAlignCats.get(pcdata.getName()) + " out of " + totalcnt);
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(pcdata.getNode(), tt);
        }
    }
    private void showTransCodingChart(HashMap<String, Boolean> hmCoding) {
        int totalcnt = hmCoding.size();
        int ccnt = 0;
        int nccnt = 0;
        for(boolean coding : hmCoding.values()) {
            if(coding)
                ccnt++;
            else
                nccnt++;
        }
        double cpct = 0.0;
        double ncpct = 0.0;
        if(totalcnt > 0) {
            cpct = ccnt * 100 / (double)totalcnt;
            ncpct = nccnt * 100 / (double)totalcnt;
        }
        cpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(cpct*100)/100.0)));
        ncpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(ncpct*100)/100.0)));

        ObservableList<PieChart.Data> pieChartData =  FXCollections.observableArrayList();
        pieChartData.add(new PieChart.Data("Coding", cpct));
        pieChartData.add(new PieChart.Data("Non-Coding", ncpct));
        pieSummaryTransCoding.setData(pieChartData);
        app.ctls.setupChartExport(pieSummaryTransCoding, "Isoforms Coding Chart", "tappAS_TransCodingChart.png", null);
        Tooltip tt;
        for (PieChart.Data pcdata : pieChartData) {
            tt = new Tooltip("  " + pcdata.getName() + "\n  Percent: " + String.valueOf(pcdata.getPieValue()) + "%  \n  Count: " + (pcdata.getName().equals("Coding")? ccnt : nccnt) + " out of " + totalcnt);
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
