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
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import tappas.DataDIU.DIUSelectionResults;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDIUSummary extends SubTabBase {
    AnchorPane paneContents;
    PieChart pieSummary;
    Label lbSubtitle;
    GridPane grMain;
    ProgressIndicator pi;
    
    ContextMenu cmData;
    DataApp.DataType dataType;
    String dataName;
    DataDIU diuData;
    DlgDIUAnalysis.Params diuParams;
    PlotsDIU scatterplot;
    
    public SubTabDIUSummary(Project project) {
        super(project);
    }

    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            
            if(project.data.isCaseControlExpType()){
                scatterplot = new PlotsDIU(project);
            }
            
            diuData = new DataDIU(project);
            dataType = (DataApp.DataType) args.get("dataType");
            switch(dataType) {
                case PROTEIN:
                    dataName = "Proteins";
                    result = diuData.hasDIUData(dataType);
                    break;
                case TRANS:
                    dataName = "Transcripts";
                    result = diuData.hasDIUData(dataType);
                    break;
                default:
                    result = false;
                    break;
            }
            
            // DIU data is required
            if(result) {
                // DIU parameter file is required
                result = Files.exists(Paths.get(diuData.getDIUParamsFilepath(dataType)));
                if(result) {
                    diuParams = DlgDIUAnalysis.Params.load(diuData.getDIUParamsFilepath(dataType), project);
                }                
                else
                    app.ctls.alertInformation("DIU Summary", "Missing DIU parameter file");
            }
            else
                app.ctls.alertInformation("DIU Summary", "Missing DIU data");

            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        if(hm.containsKey("updateDIUResults")) {
            // should not take long, OK to not do in background
            diuParams = DlgDIUAnalysis.Params.load(diuData.getDIUParamsFilepath(dataType), project);
            ObservableList<DataDIU.DIUSelectionResults> diuResults = diuData.getDIUSelectionResults(dataType, diuParams.sigValue, false);
            updateDisplayData(diuResults);
        }
        return obj;
    }

    //
    // Internal Functions
    //
    private void showSubTab(ObservableList<DIUSelectionResults> diuResults) {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        pieSummary = (PieChart) tabNode.getParent().lookup("#pieDA_GeneDIUResults");
        lbSubtitle = (Label) tabNode.getParent().lookup("#lbSubtitle");
        grMain = (GridPane) tabNode.getParent().lookup("#grdMain");
        pi = (ProgressIndicator) tabNode.getParent().lookup("#piDIUScatterPlot");
        
        if(project.data.isCaseControlExpType()){
            scatterplot.showDIUScatterPlot(dataType, subTabInfo, tabNode, true);
        }else{
            grMain.getColumnConstraints().remove(1);
            // to dont show the progress indicator
            pi.setOpacity(0);
        }
        updateDisplayData(diuResults);
        setupExportMenu();
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export summary pie chart image...");
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
            app.export.exportImage("DIU Summary Chart", "tappAS_DIU_Gene_" + dataName + "_Summary.png", (Image)img);
        });
        cm.getItems().add(item);
        
        if(project.data.isCaseControlExpType()){
            item = new MenuItem("Scatter plot image...");
            item.setOnAction((event) -> {
                app.export.exportImage("DIU Scatter Plot", "tappAS_DIU_Scatter_Plot.png", scatterplot.imgChart.getImage());
            });
            cm.getItems().add(item);
            cm.getItems().add(new SeparatorMenuItem());
            }
        
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
            app.export.exportImage("DIU Summary Panel", "tappAS_DEA_Gene_" + dataName + "_Summary_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void updateDisplayData(ObservableList<DIUSelectionResults> diuResults) {
        showGeneDIUResultsChart(diuResults);
    }
    
    private void showGeneDIUResultsChart(ObservableList<DIUSelectionResults> diuResults) {
        HashMap<String, Integer> hmCnt = project.data.getExpressedGeneAssignedProteinsCount(project.data.getResultsTrans());
        int totalcntPC, totalcntNPC, ntotalcntPC, ntotalcntNPC, nfcnt;
        totalcntPC = totalcntNPC = ntotalcntPC = ntotalcntNPC = nfcnt = 0;
        for(DataDIU.DIUSelectionResults diur : diuResults) {
            if(diur.getDS().equals("DIU")){
                if(diur.podiumChange.getValue().equals("YES") || !diur.podiumTimes.getName().isEmpty()){
                    if(hmCnt.containsKey(diur.getGene()))
                        totalcntPC++;
                    else
                        nfcnt++;
                }else{
                    if(hmCnt.containsKey(diur.getGene()))
                        totalcntNPC++;
                    else
                        nfcnt++;
                }
            }else{
                if(diur.podiumChange.getValue().equals("YES") || !diur.podiumTimes.getName().isEmpty()){
                    if(hmCnt.containsKey(diur.getGene()))
                        ntotalcntPC++;
                    else
                        nfcnt++;
                }else{
                    if(hmCnt.containsKey(diur.getGene()))
                        ntotalcntNPC++;
                    else
                        nfcnt++;
                }
            }
        }
        double dsPCpct, dsNPCpct, ndsPCpct, ndsNPCpct;
        dsPCpct = dsNPCpct = ndsPCpct = ndsNPCpct = 0.0;
        int fullcnt = totalcntPC + totalcntNPC + ntotalcntPC + ntotalcntNPC;
        if(fullcnt > 0) {
            dsPCpct = totalcntPC * 100 / (double)fullcnt;
            dsNPCpct = totalcntNPC * 100 / (double)fullcnt;
            ndsPCpct = ntotalcntPC * 100 / (double)fullcnt;
            ndsNPCpct = ntotalcntNPC * 100 / (double)fullcnt;
        }
        
        dsPCpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(dsPCpct*100)/100.0)));
        dsNPCpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(dsNPCpct*100)/100.0)));
        ndsPCpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(ndsPCpct*100)/100.0)));
        ndsNPCpct = Double.parseDouble(String.format("%.2f", ((double)Math.round(ndsNPCpct*100)/100.0)));

        ObservableList<PieChart.Data> pieChartData =  FXCollections.observableArrayList();

        pieSummary.setStartAngle(90);
        pieChartData.add(new PieChart.Data("DIU - Major Isoform Switching", dsPCpct));
        pieChartData.add(new PieChart.Data("DIU - Not Major Isoform Switching", dsNPCpct));
        pieChartData.add(new PieChart.Data("Not DIU - Major Isoform Switching", ndsPCpct));
        pieChartData.add(new PieChart.Data("Not DIU - Not Major Isoform Switching", ndsNPCpct));
        pieSummary.setData(pieChartData);

        
        String[] colors = new String[]{"#f2635c","#ff948e","#7cafd6","#aad6f7"};
        
        HashMap<String, HashMap<String, Object>> hmGeneTrans = project.data.getResultsGeneTrans();
        int genecnt = hmGeneTrans.size();
        pieSummary.setTitle("Gene " + dataName + " DIU Results");
        pieSummary.setStyle("-fx-font: 15 system;");
        
        app.ctls.setupChartExport(pieSummary, "Gene " + dataName + " DIU Results Chart", "tappAS_DIU_Gene_" + dataName + "_Summary.png", null);
        Tooltip tt;
        int cont = 0;
        for (PieChart.Data pcdata : pieChartData) {
            pcdata.getNode().setStyle("-fx-pie-color: " + colors[cont]);
            String txt;
            if(pcdata.getName().contains("Not DIU")){
                if(pcdata.getName().contains("Not Major")){
                    txt = "" + ntotalcntNPC;
                }else{
                    txt = "" + ntotalcntPC;
                }
            }else{
                if(pcdata.getName().contains("Not Major")){
                    txt = "" + totalcntNPC;
                }else{
                    txt = "" + totalcntPC;
                }
            }
            
            tt = new Tooltip("  " + pcdata.getName() + "\n  Percent: " + String.valueOf(pcdata.getPieValue()) + "%  \n  Count: " + txt + " out of " + fullcnt + " genes");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(pcdata.getNode(), tt);
            cont++;
        }
        if(nfcnt > 0)
            app.logWarning("Unable to find gene information for " + nfcnt + " genes.");
        
        lbSubtitle.setText(String.valueOf(fullcnt) + " genes of " + String.valueOf(genecnt) + " tested");
        //lbSubtitle.setStyle("-fx-font: 13 system;");
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        ObservableList<DIUSelectionResults> data = diuData.getDIUSelectionResults(dataType, diuParams.sigValue, false);
        showSubTab(data);
    }
}
