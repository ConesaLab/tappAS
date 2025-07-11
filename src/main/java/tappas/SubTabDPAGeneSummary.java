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
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDPAGeneSummary extends SubTabBase {
    AnchorPane paneContents;
    PieChart pieSummary, pieDistal;
    BarChart barFoldChange;
    TableView tblProteins, tblClusters; //tblData,
    GridPane grdMain;
    
    ScrollPane scrollPane;
    
    ContextMenu cmData, cmProteins, cmClusters;
    String analysisId;
    DataDPA dpaData;
    DlgDPAnalysis.Params dpaParams;
    
    ProgressIndicator pi;
    PlotsDPA heatmap;
    
    public SubTabDPAGeneSummary(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            dpaData = new DataDPA(project);
            dpaParams =  dpaData.getDPAParams();
            result = dpaData.hasDPAData();
            if(result) {
                if(args.containsKey("id")) {
                    heatmap = new PlotsDPA(project);
                    analysisId = (String) args.get("id");
                    if(project.data.isMultipleTimeSeriesExpType()) {
                        subTabInfo.title = "DPA Genes Summary: " + analysisId;
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
        if(hm.containsKey("updateDPAResults")) {
            // should not take long, OK to not do in background - Arreglar lista!!!
            dpaParams = dpaData.getDPAParams();
            ArrayList<DataDPA.DPAResultsData> dpaResults = dpaData.getDPAResultsData(dpaParams.sigValue);
            displayData(dpaResults, project.data.isTimeCourseExpType());
        }
        return obj;
    }

    //
    // Internal Functions
    //
    private void showSubTab(ArrayList<DataDPA.DPAResultsData> dpaResults) {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        scrollPane = (ScrollPane) tabNode.lookup("#scrollPane");
        
        pieSummary = (PieChart) scrollPane.getContent().lookup("#pieDA_GeneDPAResults");
        pieDistal = (PieChart) scrollPane.getContent().lookup("#pieDPA_DistalResults");
        barFoldChange = (BarChart) scrollPane.getContent().lookup("#barDE_SummaryGeneFold");
        //tblData = (TableView) tabNode.lookup("#tblDE_GeneIsosSummary");
        tblProteins = (TableView) scrollPane.getContent().lookup("#tblDE_GeneProtSummary");
        tblClusters = (TableView) scrollPane.getContent().lookup("#tblClusters");
        pi = (ProgressIndicator) scrollPane.getContent().lookup("#piDPAHeatmapPlot");
        grdMain = (GridPane) scrollPane.getContent().lookup("#grdMain");

        //HEATMAP solo para CASE-CONTROL
        if(project.data.isCaseControlExpType()) {
            heatmap.showDPAHeatmapPlot(subTabInfo, tabNode, true);
        }else{
            pi.setVisible(false);
        }
        // GENE ISOFORMS TABLE
        // setup and populate table
        /*tblData.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
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
                        app.export.setupSimpleTableExport(tblData, cmData, false, "tappas_DPA_Gene_Summary_Transcripts.tsv");
                        cmData.setAutoHide(true);
                        cmData.show(tblData, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });
        */
        // GENE PROTEINS TABLE
        // setup and populate table
        tblProteins.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ObservableList<TableColumn> cols = tblProteins.getColumns();
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
                        app.export.setupSimpleTableExport(tblProteins, cmProteins, false, "tappAS_DPA_Gene_Summary_Proteins.tsv");
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
                            app.export.setupSimpleTableExport(tblClusters, cmClusters, false, "tappAS_DPA_Gene_Clusters.tsv");
                            cmClusters.setAutoHide(true);
                            cmClusters.show(tblClusters, event.getScreenX(), event.getScreenY());
                        }
                    }
                }
            });
            tblClusters.setVisible(true);
            barFoldChange.setVisible(false);
            displayData(dpaResults, true);
        }
        else {
            barFoldChange.setVisible(true);
            displayData(dpaResults, false);
        }
        
        setupExportMenu();
        setFocusNode(tblProteins, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export proteins summary table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblProteins, true, "tappAS_DPA_Gene_Summary_Proteins.tsv"); });
        cm.getItems().add(item);
        if(project.data.isTimeCourseExpType()) {
            item = new MenuItem("Export cluster table data...");
            item.setOnAction((event) -> { app.export.exportTableData(tblClusters, true, "tappAS_DPA_Gene_Summary_Clusters.tsv"); });
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
            app.export.exportImage("DPA Summary Chart", "tappAS_DPA_Gene_Summary.png", (Image)img);
        });
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Export distal pie chart image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = pieDistal.getWidth();
            double ratio = 3840/x;
            pieDistal.setScaleX(ratio);
            pieDistal.setScaleY(ratio);
            WritableImage img = pieDistal.snapshot(sP, null);
            double newX = pieDistal.getWidth();
            ratio = x/newX;
            pieDistal.setScaleX(ratio);
            pieDistal.setScaleY(ratio);
            
            //WritableImage img = pieDistal.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Distal DPA Summary Chart", "tappAS_DPA_Distal_Summary.png", (Image)img);
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
                app.export.exportImage("L2FC Distribution Chart", "tappAS_DPA_Gene_Summary_L2FC_Distribution.png", (Image)img);
            });
            cm.getItems().add(item);
        }
        
        item = new MenuItem("Heatmap plot image...");
        item.setOnAction((event) -> {
            app.export.exportImage("DPA Heatmap Plot", "tappAS_DPA_Heatmap_Plot.png", heatmap.imgChart.getImage());
        });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        
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
            app.export.exportImage("DPA Summary Panel", "tappAS_DPA_Gene_Summary_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void displayData(ArrayList<DataDPA.DPAResultsData> dpaResults, boolean tc) {
        setTableData(dpaResults);
        showGeneDPAResultsChart(dpaResults);
        if(project.data.isCaseControlExpType())
            showDistalDPAResultsChart(dpaResults);
        if(tc)
            setClustersData();
        else
            showGeneFoldChangeChart(dpaResults);
    }
    private void setClustersData() {
        //HashMap<String, Integer> hm = dpaData.getDPACluster();
        HashMap<String, HashMap<String, Integer>> hm = dpaData.getDPACluster();
        ArrayList<ClusterCount> lst = new ArrayList<>();
        HashMap<Integer, Integer> hmClusters = new HashMap<>();        
        for(HashMap<String, Integer> group : hm.values()) {
            for(int cnum : group.values()){
                if(hmClusters.containsKey(cnum))
                    hmClusters.put(cnum, hmClusters.get(cnum) + 1);
                else
                    hmClusters.put(cnum, 1);
            }
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
    private void setTableData(ArrayList<DataDPA.DPAResultsData> dpaResults) {
        /* GENE TABLE
        HashMap<String, HashMap<String, Object>> hmGeneTrans = project.data.getResultsGeneTrans();
        int totalcnt, scnt, mcnt, ntotalcnt, nscnt, nmcnt, nfcnt;
        totalcnt = scnt = mcnt = ntotalcnt = nscnt = nmcnt = nfcnt = 0;
        for(DataDPA.DPAResultsData aux : dpaResults) {
            if(aux.ds) {
                if(hmGeneTrans.containsKey(aux.gene)) {
                    if(hmGeneTrans.get(aux.gene).size() > 1)
                        mcnt++;
                    else
                        scnt++;
                    totalcnt++;
                }
                else
                    nfcnt++;
            }
            else {
                if(hmGeneTrans.containsKey(aux.gene)) {
                    if(hmGeneTrans.get(aux.gene).size() > 1)
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
        // DPA has not single transcripts
        sumdata.add(new SummaryCountData("with Single Transcript", scnt, nscnt, (scnt + nscnt)));
        sumdata.add(new SummaryCountData("with Multiple Transcripts", mcnt, nmcnt, (mcnt + nmcnt)));
        sumdata.add(new SummaryCountData("Total", totalcnt, ntotalcnt, (totalcnt + ntotalcnt)));
        tblData.setItems(sumdata);
        if(!sumdata.isEmpty())
            tblData.getSelectionModel().select(0);
        */
        int totalcnt, scnt, mcnt, ntotalcnt, nscnt, nmcnt, nfcnt;
        totalcnt = scnt = mcnt = ntotalcnt = nscnt = nmcnt = nfcnt = 0;
        ObservableList<SummaryCountData> sumdata = FXCollections.observableArrayList();

        // protein table
        HashMap<String, Integer> hmCnt = project.data.getExpressedGeneAssignedProteinsCount(project.data.getResultsTrans());
        totalcnt = scnt = mcnt = 0;
        int zcnt = 0;
        ntotalcnt = nscnt = nmcnt = 0;
        int nzcnt = 0;
        for(DataDPA.DPAResultsData aux : dpaResults) {
            if(aux.ds) {
                if(hmCnt.containsKey(aux.gene)) {
                    int cnt = hmCnt.get(aux.gene);
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
                if(hmCnt.containsKey(aux.gene)) {
                    int cnt = hmCnt.get(aux.gene);
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
    private void showGeneDPAResultsChart(ArrayList<DataDPA.DPAResultsData> dpaResults) {
        int totalcnt = 0;
        int ntotalcnt = 0;
        for(DataDPA.DPAResultsData aux : dpaResults) {
            if(aux.ds)
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
        pieChartData.add(new PieChart.Data("DPA", depct));
        pieChartData.add(new PieChart.Data("Not DPA", ndepct));
        pieSummary.setData(pieChartData);
        pieSummary.setStartAngle(90);
        
        app.ctls.setupChartExport(pieSummary, "Genes DPA Summary Chart", "tappAS_DPA_Gene_Summary.png", null);
        Tooltip tt;
        for (PieChart.Data pcdata : pieChartData) {
            tt = new Tooltip("  " + pcdata.getName() + "\n  Percent: " + String.valueOf(pcdata.getPieValue()) + "%  \n  Count: " + (pcdata.getName().contains("Not")? ntotalcnt : totalcnt) + " out of " + fullcnt + " genes");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(pcdata.getNode(), tt);
        }
    }
    
    private void showDistalDPAResultsChart(ArrayList<DataDPA.DPAResultsData> dpaResults) {
        int grp1_sw = 0;
        int grp1_nsw = 0;
        int grp2_sw = 0;
        int grp2_nsw = 0;
        
        for(DataDPA.DPAResultsData aux : dpaResults) {
            if(aux.LongerCondition.equals(project.data.getGroupNames()[0])){
                if(aux.podiumChange)
                    grp1_sw++;
                else
                    grp1_nsw++;
            }else{
                if(aux.podiumChange)
                    grp2_sw++;
                else
                    grp2_nsw++;
            }
        }
        double grppct1_sw, grppct1_nsw, grppct2_sw, grppct2_nsw, grppctTotal1, grppctTotal2;
        grppct1_sw = grppct1_nsw = grppct2_sw = grppct2_nsw = grppctTotal1 = grppctTotal2 = 0.0;
        int fullcnt_1 = grp1_sw + grp1_nsw;
        int fullcnt_2 = grp2_sw + grp2_nsw;
        int fullcnt_Total = fullcnt_1 + fullcnt_2;
        
        if(fullcnt_1 > 0 && fullcnt_2 > 0) {
            grppct1_sw = grp1_sw * 100 / (double)fullcnt_Total;
            grppct1_nsw = grp1_nsw * 100 / (double)fullcnt_Total;
            grppct2_sw = grp2_sw * 100 / (double)fullcnt_Total;
            grppct2_nsw = grp2_nsw * 100 / (double)fullcnt_Total;
            grppctTotal1 = fullcnt_1 * 100 / (double)fullcnt_Total;
            grppctTotal2 = fullcnt_2 * 100 / (double)fullcnt_Total;
        }
        
        grppct1_sw = Double.parseDouble(String.format("%.2f", ((double)Math.round(grppct1_sw*100)/100.0)));
        grppct1_nsw = Double.parseDouble(String.format("%.2f", ((double)Math.round(grppct1_nsw*100)/100.0)));
        grppct2_sw = Double.parseDouble(String.format("%.2f", ((double)Math.round(grppct2_sw*100)/100.0)));
        grppct2_nsw = Double.parseDouble(String.format("%.2f", ((double)Math.round(grppct2_nsw*100)/100.0)));
        grppctTotal1 = Double.parseDouble(String.format("%.2f", ((double)Math.round(grppctTotal1*100)/100.0)));
        grppctTotal2 = Double.parseDouble(String.format("%.2f", ((double)Math.round(grppctTotal2*100)/100.0)));

        ObservableList<PieChart.Data> pieChartData =  FXCollections.observableArrayList();
        pieChartData.add(new PieChart.Data("Distal Cond. "+ project.data.getGroupNames()[0] + " Switch", grppct1_sw));
        pieChartData.add(new PieChart.Data("Distal Cond. "+ project.data.getGroupNames()[0] + " Not Switch", grppct1_nsw));
        pieChartData.add(new PieChart.Data("Distal Cond. "+ project.data.getGroupNames()[1] + " Switch", grppct2_sw));
        pieChartData.add(new PieChart.Data("Distal Cond. "+ project.data.getGroupNames()[1] + " Not Switch", grppct2_nsw));
        
        pieDistal.setData(pieChartData);
        pieDistal.setStartAngle(90);
        
        app.ctls.setupChartExport(pieDistal, "Distal DPA Summary Chart", "tappAS_DPA_Distal_Summary.png", null);
        
        Tooltip tt;
        
        String[] colors = new String[]{"#f2635c","#ff948e","#7cafd6","#aad6f7"};
        
        int cont = 0;
        for (PieChart.Data pcdata : pieChartData) {
            pcdata.getNode().setStyle("-fx-pie-color: " + colors[cont]);
            tt = new Tooltip("  " + pcdata.getName() + "\n  Percent: " + String.valueOf(pcdata.getPieValue()) + "%  \n  Count: " + (pcdata.getName().contains("Not")? (pcdata.getName().contains(project.data.getGroupNames()[0])? grp1_nsw : grp2_nsw) : (pcdata.getName().contains(project.data.getGroupNames()[0])? grp1_sw : grp2_sw)) + " out of " + fullcnt_Total + " genes");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(pcdata.getNode(), tt);
            cont++;
        }
    }
    
    private void showGeneFoldChangeChart(ArrayList<DataDPA.DPAResultsData> dpaResults) {
        ((TabProjectDataViz)tabBase).showFoldChangeChart(dpaResults, barFoldChange, "Gene");
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        ArrayList<DataDPA.DPAResultsData> data = dpaData.getDPAResultsData(dpaParams.sigValue);
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
