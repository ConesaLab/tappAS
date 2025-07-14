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
import tappas.SubTabDEAGeneSummary.SummaryCountData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDEATransSummary extends SubTabBase {
    AnchorPane paneContents;
    TableView tblData, tblClusters;
    PieChart pieSummary;
    BarChart barFoldChange;
    
    ContextMenu cmData, cmClusters;
    String analysisId;
    DataDEA deaData;
    DlgDEAnalysis.Params deaParams;
    
    public SubTabDEATransSummary(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            deaData = new DataDEA(project);
            deaParams =  deaData.getDEAParams(DataType.TRANS);
            result = deaData.hasDEAData(DataApp.DataType.TRANS);
            if(result) {
                if(args.containsKey("id")) {
                    analysisId = (String) args.get("id");
                    if(project.data.isMultipleTimeSeriesExpType()) {
                        subTabInfo.title = "DEA Transcripts Summary: " + analysisId;
                        subTabInfo.tooltip = "Transcripts Differential Expression Analysis summary for '" + analysisId + "'";
                        setTabLabel(subTabInfo.title, subTabInfo.tooltip);
                    }
                }
                else
                    result = false;
            }
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        if(hm.containsKey("updateDEAResults")) {
            // should not take long, OK to not do in background
            deaParams = deaData.getDEAParams(DataType.TRANS);
            DataDEA.DEAResults deaResults = deaData.getDEAResults(DataType.TRANS, analysisId, deaParams.sigValue, deaParams.FCValue);
            displayData(deaResults, project.data.isTimeCourseExpType());
        }
        return obj;
    }
    
    //
    // Internal Functions
    //
    private void showSubTab(DataDEA.DEAResults deaResults) {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        pieSummary = (PieChart) tabNode.getParent().lookup("#pieDA_transDEAResults");
        barFoldChange = (BarChart) tabNode.getParent().lookup("#barDE_SummaryTransFold");
        tblData = (TableView) tabNode.lookup("#tblDE_TransSummaryCoding");
        tblClusters = (TableView) tabNode.lookup("#tblClusters");

        // setup and populate table
        tblData.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ObservableList<TableColumn> cols = tblData.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Field"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Count1"));
        cols.get(2).setCellValueFactory(new PropertyValueFactory<>("Count2"));
        cols.get(3).setCellValueFactory(new PropertyValueFactory<>("Count3"));
        app.export.addCopyToClipboardHandler(tblData);
        tblData.setRowFactory(tableView -> {
            TableRow<SummaryCountData> row = new TableRow<>();
            row.selectedProperty().addListener((obs, oldData, newData) -> {
                SummaryCountData sd = row.getItem();
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
        tblData.setOnMouseClicked((event) -> {
            if(cmData != null)
                cmData.hide();
            cmData = null;
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                Node node = event.getPickResult().getIntersectedNode();
                if(node != null) {
                    System.out.println("node: " + node);
                    if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        node = node.getParent();
                        System.out.println("parent: " + node);
                    }
                    if(node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        cmData = new ContextMenu();
                        app.export.setupSimpleTableExport(tblData, cmData, false, "tappAS_DEA_Trans_Summary.tsv");
                        cmData.setAutoHide(true);
                        cmData.show(tblData, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });
        
        // table clusters for time course experiments
        if(project.data.isTimeCourseExpType()) {
            cols = tblClusters.getColumns();
            cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Field"));
            cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Count1"));
            app.export.addCopyToClipboardHandler(tblClusters);
            tblClusters.setOnMouseClicked((event) -> {
                if(cmClusters != null)
                    cmClusters.hide();
                cmClusters = null;
                if(event.getButton().equals(MouseButton.SECONDARY)) {
                    Node node = event.getPickResult().getIntersectedNode();
                    if(node != null) {
                        System.out.println("node: " + node);
                        if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                            node = node.getParent();
                            System.out.println("parent: " + node);
                        }
                        if(node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                            cmClusters = new ContextMenu();
                            app.export.setupSimpleTableExport(tblClusters, cmClusters, false, "tappAS_DEA_Gene_Clusters.tsv");
                            cmClusters.setAutoHide(true);
                            cmClusters.show(tblClusters, event.getScreenX(), event.getScreenY());
                        }
                    }
                }
            });
            tblClusters.setVisible(true);
            barFoldChange.setVisible(false);
            displayData(deaResults, true);
        }
        else {
            barFoldChange.setVisible(true);
            displayData(deaResults, false);
        }
        
        setupExportMenu();
        setFocusNode(tblData, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export summary table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblData, true, "tappAS_DEA_Trans_Summary.tsv"); });
        cm.getItems().add(item);
        if(project.data.isTimeCourseExpType()) {
            item = new MenuItem("Export cluster table data...");
            item.setOnAction((event) -> { app.export.exportTableData(tblClusters, true, "tappAS_DEA_Gene_Summary_Clusters.tsv"); });
            cm.getItems().add(item);
        }
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
            app.export.exportImage("DEA Summay Chart", "tappAS_DEA_Trans_Summary.png", (Image)img);
        });
        cm.getItems().add(item);
        if(project.data.isCaseControlExpType()) {
            item = new MenuItem("Export distribution chart image...");
            item.setOnAction((event) -> { 
                SnapshotParameters sP = new SnapshotParameters();
                double x = barFoldChange.getWidth();
                double ratio = 3840/x;
                barFoldChange.setScaleX(ratio);
                barFoldChange.setScaleY(ratio);
                WritableImage img = barFoldChange.snapshot(sP, null);
                double newX = barFoldChange.getWidth();
                ratio = x/newX;
                barFoldChange.setScaleX(ratio);
                barFoldChange.setScaleY(ratio);
                
                //WritableImage img = barFoldChange.snapshot(new SnapshotParameters(), null);
                app.export.exportImage("L2FC Distribution Chart", "tappAS_DEA_Trans_Summary_L2FC_Distribution.png", (Image)img);
            });
            cm.getItems().add(item);
        }
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
            app.export.exportImage("DEA Summary Panel", "tappAS_DEA_Trans_Summary_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void displayData(DataDEA.DEAResults deaResults, boolean tc) {
        setTableData(deaResults);
        showTransDEAResultsChart(deaResults);
        if(tc)
            setClustersData();
        else
            showTransFoldChangeChart(deaResults);
    }
    private void setClustersData() {
        HashMap<String, Integer> hm = deaData.getDEAClusters(project.data.getExperimentType(), DataType.TRANS, analysisId);
        ArrayList<SubTabDEAGeneSummary.ClusterCount> lst = new ArrayList<>();
        HashMap<Integer, Integer> hmClusters = new HashMap<>();        
        for(int cnum : hm.values()) {
            if(hmClusters.containsKey(cnum))
                hmClusters.put(cnum, hmClusters.get(cnum) + 1);
            else
                hmClusters.put(cnum, 1);
        }
        ArrayList<Integer> lstCN = new ArrayList<>();
        lstCN.addAll(hmClusters.keySet());
        Collections.sort(lstCN);
        ObservableList<SummaryCountData> data = FXCollections.observableArrayList();
        for(int cn : lstCN)
            data.add(new SummaryCountData("Cluster " + cn, hmClusters.get(cn), 0, 0));
        tblClusters.setItems(data);
        if(!data.isEmpty())
            tblClusters.getSelectionModel().select(0);
    }
    private void setTableData(DataDEA.DEAResults deaResults) {
        int totalcnt, scnt, mcnt, ntotalcnt, nscnt, nmcnt;
        totalcnt = scnt = mcnt = ntotalcnt = nscnt = nmcnt = 0;
        for(DataDEA.DEAResultsData rd : deaResults.lstResults) {
            if(rd.de) {
                if(project.data.isTransCoding(rd.id))
                    scnt++;
                else
                    mcnt++;
                totalcnt++;
            }
            else {
                if(project.data.isTransCoding(rd.id))
                    nscnt++;
                else
                    nmcnt++;
                ntotalcnt++;
            }
        }
        ObservableList<SummaryCountData> sumdata = FXCollections.observableArrayList();
        sumdata.add(new SummaryCountData("Coding", scnt, nscnt, (scnt + nscnt)));
        sumdata.add(new SummaryCountData("Non-Coding", mcnt, nmcnt, (mcnt + nmcnt)));
        sumdata.add(new SummaryCountData("Total", totalcnt, ntotalcnt, (totalcnt + ntotalcnt)));
        tblData.setItems(sumdata);
        if(!sumdata.isEmpty())
            tblData.getSelectionModel().select(0);
    }    
    private void showTransDEAResultsChart(DataDEA.DEAResults deaResults) {
        // show transcript table
        int totalcnt = 0;
        int ntotalcnt = 0;
        for(DataDEA.DEAResultsData rd : deaResults.lstResults) {
            if(rd.de)
                totalcnt++;
            else
                ntotalcnt++;
        }
        double depct, ndepct;
        depct = ndepct = 0.0;
        int fullcnt = totalcnt + ntotalcnt;
        if(fullcnt > 0) {
            depct = totalcnt * 100 / (double)fullcnt;
            ndepct = ntotalcnt * 100 / (double)fullcnt;
        }
        depct = Double.parseDouble(String.format("%.2f", ((double)Math.round(depct*100)/100.0)));
        ndepct = Double.parseDouble(String.format("%.2f", ((double)Math.round(ndepct*100)/100.0)));

        ObservableList<PieChart.Data> pieChartData =  FXCollections.observableArrayList();
        pieChartData.add(new PieChart.Data("DE", depct));
        pieChartData.add(new PieChart.Data("Not DE", ndepct));
        pieSummary.setData(pieChartData);
        app.ctls.setupChartExport(pieSummary, "DEA Transcripts Summary Chart", "tappAS_DEA_Trans_Summary.png", null);
        Tooltip tt;
        for (PieChart.Data pcdata : pieChartData) {
            tt = new Tooltip("  " + pcdata.getName() + "\n  Percent: " + String.valueOf(pcdata.getPieValue()) + "%  \n  Count: " + (pcdata.getName().contains("Not")? ntotalcnt : totalcnt) + " out of " + fullcnt + " transcripts");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(pcdata.getNode(), tt);
        }
    }
    private void showTransFoldChangeChart(DataDEA.DEAResults deaResults) {
        ((TabProjectDataViz)tabBase).showFoldChangeChart(deaResults, barFoldChange, "Transcript");
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        DataDEA.DEAResults data = deaData.getDEAResults(DataType.TRANS, analysisId, deaParams.sigValue, deaParams.FCValue);
        showSubTab(data);
    }
}
