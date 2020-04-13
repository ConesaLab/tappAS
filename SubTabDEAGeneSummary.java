/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDEAGeneSummary extends SubTabBase {
    AnchorPane paneContents;
    PieChart pieSummary;
    BarChart barFoldChange;
    TableView tblData, tblProteins, tblClusters;
    
    ContextMenu cmData, cmProteins, cmClusters;
    String analysisId;
    DataDEA deaData;
    DlgDEAnalysis.Params deaParams;
    
    public SubTabDEAGeneSummary(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            deaData = new DataDEA(project);
            deaParams =  deaData.getDEAParams(DataType.GENE);
            result = deaData.hasDEAData(DataApp.DataType.GENE);
            if(result) {
                if(args.containsKey("id")) {
                    analysisId = (String) args.get("id");
                    if(project.data.isMultipleTimeSeriesExpType()) {
                        subTabInfo.title = "DEA Genes Summary: " + analysisId;
                        subTabInfo.tooltip = "Genes Differential Expression Analysis summary for '" + analysisId + "'";
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
            deaParams = deaData.getDEAParams(DataType.GENE);
            DataDEA.DEAResults deaResults = deaData.getDEAResults(DataType.GENE, analysisId, deaParams.sigValue, deaParams.FCValue);
            displayData(deaResults, project.data.isTimeCourseExpType());
        }
        return obj;
    }

    //
    // Internal Functions
    //
    private void showSubTab(DataDEA.DEAResults deaResults) {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        pieSummary = (PieChart) tabNode.getParent().lookup("#pieDA_GeneDEAResults");
        barFoldChange = (BarChart) tabNode.getParent().lookup("#barDE_SummaryGeneFold");
        tblData = (TableView) tabNode.lookup("#tblDE_GeneIsosSummary");
        tblProteins = (TableView) tabNode.lookup("#tblDE_GeneProtSummary");
        tblClusters = (TableView) tabNode.lookup("#tblClusters");
        
        // GENE ISOFORMS TABLE
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
                        app.export.setupSimpleTableExport(tblData, cmData, false, "tappAS_DEA_Gene_Summary_Transcripts.tsv");
                        cmData.setAutoHide(true);
                        cmData.show(tblData, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });

        // GENE PROTEINS TABLE
        // setup and populate table
        tblProteins.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        cols = tblProteins.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Field"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Count1"));
        cols.get(2).setCellValueFactory(new PropertyValueFactory<>("Count2"));
        cols.get(3).setCellValueFactory(new PropertyValueFactory<>("Count3"));
        app.export.addCopyToClipboardHandler(tblProteins);
        tblProteins.setRowFactory(tableView -> {
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
        tblProteins.setOnMouseClicked((event) -> {
            if(cmProteins != null)
                cmProteins.hide();
            cmProteins = null;
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                Node node = event.getPickResult().getIntersectedNode();
                if(node != null) {
                    System.out.println("node: " + node);
                    if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        node = node.getParent();
                        System.out.println("parent: " + node);
                    }
                    if(node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        cmProteins = new ContextMenu();
                        app.export.setupSimpleTableExport(tblProteins, cmProteins, false, "tappAS_DEA_Gene_Summary_Proteins.tsv");
                        cmProteins.setAutoHide(true);
                        cmProteins.show(tblProteins, event.getScreenX(), event.getScreenY());
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
                        if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                            node = node.getParent();
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
        MenuItem item = new MenuItem("Export transcripts summary table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblData, true, "tappAS_DEA_Gene_Summary_Transcripts.tsv"); });
        cm.getItems().add(item);
        item = new MenuItem("Export proteins summary table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblProteins, true, "tappAS_DEA_Gene_Summary_Proteins.tsv"); });
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
            app.export.exportImage("DEA Summary Chart", "tappAS_DEA_Gene_Summary.png", (Image)img);
        });
        cm.getItems().add(item);
        if(project.data.isCaseControlExpType()) {
            item = new MenuItem("Export L2FC distribution chart image...");
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
                app.export.exportImage("L2FC Distribution Chart", "tappAS_DEA_Gene_Summary_L2FC_Distribution.png", (Image)img);
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
            app.export.exportImage("DEA Summary Panel", "tappAS_DEA_Gene_Summary_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void displayData(DataDEA.DEAResults deaResults, boolean tc) {
        setTableData(deaResults);
        showGeneDEAResultsChart(deaResults);
        if(tc)
            setClustersData();
        else
            showGeneFoldChangeChart(deaResults);
    }
    private void setClustersData() {
        HashMap<String, Integer> hm = deaData.getDEAClusters(project.data.getExperimentType(), DataType.GENE, analysisId);
        ArrayList<ClusterCount> lst = new ArrayList<>();
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
        HashMap<String, HashMap<String, Object>> hmGeneTrans = project.data.getResultsGeneTrans();
        int totalcnt, scnt, mcnt, ntotalcnt, nscnt, nmcnt, nfcnt;
        totalcnt = scnt = mcnt = ntotalcnt = nscnt = nmcnt = nfcnt = 0;
        for(DataDEA.DEAResultsData rd : deaResults.lstResults) {
            if(rd.de) {
                if(hmGeneTrans.containsKey(rd.id)) {
                    if(hmGeneTrans.get(rd.id).size() > 1)
                        mcnt++;
                    else
                        scnt++;
                    totalcnt++;
                }
                else
                    nfcnt++;
            }
            else {
                if(hmGeneTrans.containsKey(rd.id)) {
                    if(hmGeneTrans.get(rd.id).size() > 1)
                        nmcnt++;
                    else
                        nscnt++;
                    ntotalcnt++;
                }
                else
                    nfcnt++;
            }
        }
        if(nfcnt > 0)
            app.logWarning("Unable to find gene information for " + nfcnt + " gene(s).");

        ObservableList<SummaryCountData> sumdata = FXCollections.observableArrayList();
        sumdata.add(new SummaryCountData("with Single Transcript", scnt, nscnt, (scnt + nscnt)));
        sumdata.add(new SummaryCountData("with Multiple Transcripts", mcnt, nmcnt, (mcnt + nmcnt)));
        sumdata.add(new SummaryCountData("Total", totalcnt, ntotalcnt, (totalcnt + ntotalcnt)));
        tblData.setItems(sumdata);
        if(!sumdata.isEmpty())
            tblData.getSelectionModel().select(0);

        // protein table
        HashMap<String, Integer> hmCnt = project.data.getExpressedGeneAssignedProteinsCount(project.data.getResultsTrans());
        totalcnt = scnt = mcnt = 0;
        int zcnt = 0;
        ntotalcnt = nscnt = nmcnt = 0;
        int nzcnt = 0;
        for(DataDEA.DEAResultsData rd : deaResults.lstResults) {
            if(rd.de) {
                if(hmCnt.containsKey(rd.id)) {
                    int cnt = hmCnt.get(rd.id);
                    if(cnt == 0)
                        zcnt++;
                    else {
                        if(cnt == 1)
                            scnt++;
                        else
                            mcnt++;
                    }
                    totalcnt++;
                }
                else
                    nfcnt++;
            }
            else {
                if(hmCnt.containsKey(rd.id)) {
                    int cnt = hmCnt.get(rd.id);
                    if(cnt == 0)
                        nzcnt++;
                    else {
                        if(cnt == 1)
                            nscnt++;
                        else
                            nmcnt++;
                    }
                    ntotalcnt++;
                }
                else
                    nfcnt++;
            }
        }
        if(nfcnt > 0)
            app.logWarning("Unable to find gene information for " + nfcnt + " genes.");

        sumdata = FXCollections.observableArrayList();
        sumdata.add(new SummaryCountData("Non-coding", zcnt, nzcnt, (zcnt + nzcnt)));
        sumdata.add(new SummaryCountData("Coding: One Protein", scnt, nscnt, (scnt + nscnt)));
        sumdata.add(new SummaryCountData("Coding: Multiple Proteins", mcnt, nmcnt, (mcnt + nmcnt)));
        sumdata.add(new SummaryCountData("Total", totalcnt, ntotalcnt, (totalcnt + ntotalcnt)));
        tblProteins.setItems(sumdata);
        if(!sumdata.isEmpty())
            tblProteins.getSelectionModel().select(0);
    }
    private void showGeneDEAResultsChart(DataDEA.DEAResults deaResults) {
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
        app.ctls.setupChartExport(pieSummary, "Genes DEA Summary Chart", "tappAS_DEA_Gene_Summary.png", null);
        Tooltip tt;
        for (PieChart.Data pcdata : pieChartData) {
            tt = new Tooltip("  " + pcdata.getName() + "\n  Percent: " + String.valueOf(pcdata.getPieValue()) + "%  \n  Count: " + (pcdata.getName().contains("Not")? ntotalcnt : totalcnt) + " out of " + fullcnt + " genes");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(pcdata.getNode(), tt);
        }
    }
    private void showGeneFoldChangeChart(DataDEA.DEAResults deaResults) {
        ((TabProjectDataViz)tabBase).showFoldChangeChart(deaResults, barFoldChange, "Gene");
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        DataDEA.DEAResults data = deaData.getDEAResults(DataType.GENE, analysisId, deaParams.sigValue, deaParams.FCValue);
        showSubTab(data);
    }

    //
    // Data Classes
    //
    public static class SummaryCountData {
        public final SimpleStringProperty field;
        public final SimpleIntegerProperty count1;
        public final SimpleIntegerProperty count2;
        public final SimpleIntegerProperty count3;
 
        public SummaryCountData(String field, int count1, int count2, int count3) {
            this.field = new SimpleStringProperty(field);
            this.count1 = new SimpleIntegerProperty(count1);
            this.count2 = new SimpleIntegerProperty(count2);
            this.count3 = new SimpleIntegerProperty(count3);
        }
        public String getField() { return field.get(); }
        public int getCount1() { return count1.get(); }
        public int getCount2() { return count2.get(); }
        public int getCount3() { return count3.get(); }
    }
    public static class ClusterCount {
        int cluster, count;
        public ClusterCount(int cluster, int count) {
            this.cluster = cluster;
            this.count = count;
        }
    }
}
