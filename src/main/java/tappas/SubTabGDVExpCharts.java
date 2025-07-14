/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import com.sun.javafx.charts.Legend;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabGDVExpCharts extends SubTabBase {
    GridPane grdCharts;
    AnchorPane paneContents;

    String gene;
    /*HashMap<String, double[]> hmGeneExp = new HashMap<>();
    HashMap<String, double[]> hmTransExp = new HashMap<>();
    HashMap<String, double[]> hmProtExp = new HashMap<>();*/
    HashMap<String, double[][]> hmGeneExp = new HashMap<>();
    HashMap<String, double[][]> hmTransExp = new HashMap<>();
    HashMap<String, double[][]> hmProtExp = new HashMap<>();
    LineChartTimeSeries chartGene, chartTranscripts, chartProteins;
    boolean timeSeries = false;
    boolean timeMultiple = false;
    
    public SubTabGDVExpCharts(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = true;
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            timeSeries = project.data.isTimeCourseExpType();
            timeMultiple = project.data.getExperimentType().equals(DataApp.ExperimentType.Time_Course_Multiple);
            gene = (String)args.get("gene");
        }
        return result;
    }
    
    //
    // Internal Functions
    //
    private void showSubTab() {      
        grdCharts = (GridPane) tabNode.lookup("#grdCharts");
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        // set options menu
        ContextMenu cm = new ContextMenu();
        subTabInfo.miFloat = new MenuItem("Float subtab in window");
        subTabInfo.miFloat.setOnAction((event) -> { onButtonFloat(); });
        cm.getItems().add(subTabInfo.miFloat);
        subTabInfo.cmOptions = cm;

        String style = "-fx-background-color: white; -fx-font-size: 18px;";
        String styleLabels = "-fx-font-size: 20px; -fx-tick-label-font-size: 18px;";
        String styleLegend = "-fx-font-size: 18px;";


        // determine time lines for time series
        ArrayList<Integer> lstTimeLines = new ArrayList<>();
        if(timeSeries) {
            int[] scnt = project.data.getGroupTimes();
            int pt = 0;
            int n = scnt.length;
            for(int i = 0; i < (n - 1); i++) {
                lstTimeLines.add(pt + scnt[i]);
                pt += scnt[i];
            }
        }
        
        // gene chart
        int a = project.data.getGroupsCount();
        int b = project.data.getGroupsTimeNames().length;
        chartGene = new LineChartTimeSeries(new CategoryAxis(), new NumberAxis(), project.data.getGroupsTimeNames().length);
        chartGene.setMultipleGroupsTS(timeMultiple);
        chartGene.setAnimated(false);
        chartGene.setTitle(gene + " - Gene Expression");
        chartGene.getXAxis().setLabel(timeSeries? "Experimental Group and Time" : "Experimental Group");
        chartGene.getYAxis().setLabel("Expression Level");
        chartGene.setStyle(style);
        chartGene.lookup(".axis").setStyle(styleLabels);
        chartGene.lookup(".axis-label").setStyle(styleLabels);
        chartGene.lookup(".chart-legend").setStyle(styleLegend);
        chartGene.getYAxis().setStyle(styleLabels);
        chartGene.addTimeLines(lstTimeLines);
        grdCharts.add(chartGene, 0, 0);
        String[] names = project.data.getExpTypeGroupNames();
        XYChart.Series<String, Number> series1 = new XYChart.Series();
        series1.setName(gene);
        //double[] expValues = hmGeneExp.get(gene);
        double[][] expValues = hmGeneExp.get(gene);
        
        //Error bars
        XYChart.Series<String, Number> series2;
        
        int idx = 0;
        for(String name : names){
            //max 20% y min 5%
            //double random = (int)Math.floor(Math.random()*(expValues[0][idx]*0.1-expValues[0][idx]*0.05)+expValues[0][idx]*0.05);
            series1.getData().add(new XYChart.Data(name, expValues[0][idx]));
            series2 = new XYChart.Series();
            series2.getData().add(new XYChart.Data(name, expValues[0][idx]+expValues[1][idx]));//+random));
            series2.getData().add(new XYChart.Data(name, expValues[0][idx]-expValues[1][idx++]));//-random));
            chartGene.getData().addAll(series2);
            for(int i = 0; i < series2.getData().size(); i++) {
                XYChart.Data item = series2.getData().get(i);
                item.getNode().lookup(".chart-line-symbol").setStyle("-fx-stroke: 0px; -fx-background-color: " + "transparent" + "," + Controls.CHART_COLOR_1 + ";-fx-background-radius: 0px;");
                series2.getNode().lookup(".chart-series-line").setStyle("-fx-stroke:" + Controls.CHART_COLOR_1 + ";-fx-stroke-width: 1px;");
            }
        }
        
        //we want the series the last to see the tooltip
        chartGene.getData().addAll(series1);
        
        //delete legend node series - thanks to: https://stackoverflow.com/users/8568673/samed-sivasl%c4%b1o%c4%9flu
        for(Node n : chartGene.getChildrenUnmodifiable()){
            if(n instanceof Legend){
                while(((Legend)n).getChildren().size()>1){
                    int aux = 0;
                    for(final Node legendItem : ((Legend)n).getChildren()){
                        if(aux!=names.length+1 && !legendItem.equals(null)){
                            ((Legend)n).getChildren().remove(legendItem);
                            break;
                        }
                        aux++;
                    }
                }
            }
        }
        
        for(Node n : chartGene.getChildrenUnmodifiable()){
            if(n instanceof Legend){
                //just 1 gene and always red
                Legend.LegendItem legendItem = ((Legend)n).getItems().get(names.length);
                legendItem.getSymbol().setStyle("-fx-background-color: " + Controls.getChartHexColor(0) + ",white;");
            }
        }
        
        Tooltip tt;
        for(int i = 0; i < series1.getData().size(); i++) {
            XYChart.Data item = series1.getData().get(i);
            String tooltip = gene + " " + names[i] + ": " + String.format("%.2f", expValues[0][i]) + " with a " + String.format("%.2f", expValues[1][i]) + " standard deviation";
            tt = new Tooltip(tooltip);
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
            item.getNode().lookup(".chart-line-symbol").setStyle("-fx-stroke-width: 0.5px; -fx-background-color: transparent, " + Controls.CHART_COLOR_1 + ";");
            series1.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: " + Controls.CHART_COLOR_1 + "; -fx-stroke-width: 2px;");
        }
        app.export.setupChartExport(chartGene, "Gene Expression Levels", "tappAS_"+gene+"_GeneExpLevels.png", null, tabBase);

        // transcripts chart
        chartTranscripts = new LineChartTimeSeries(new CategoryAxis(), new NumberAxis(), project.data.getGroupsTimeNames().length);
        chartTranscripts.setMultipleGroupsTS(timeMultiple);
        chartTranscripts.setAnimated(false);
        chartTranscripts.setTitle(gene + " - Transcript Expression");
        chartTranscripts.getXAxis().setLabel(timeSeries? "Experimental Group and Time" : "Experimental Group");
        chartTranscripts.getYAxis().setLabel("Expression Level");
        chartTranscripts.setStyle("-fx-background-color: white; -fx-border-color: lightgray; -fx-border-width: 0 1 0 1;" + style);
        chartTranscripts.lookup(".axis").setStyle(styleLabels);
        chartTranscripts.lookup(".axis-label").setStyle(styleLabels);
        chartTranscripts.lookup(".chart-legend").setStyle(styleLegend);
        chartTranscripts.getYAxis().setStyle(styleLabels);
        chartTranscripts.addTimeLines(lstTimeLines);
        grdCharts.add(chartTranscripts, 1, 0);
        
        ArrayList<String> lstTrans = new ArrayList<>();
        lstTrans.addAll(hmTransExp.keySet());
        Collections.sort(lstTrans, String.CASE_INSENSITIVE_ORDER);
        HashMap<String, HashMap<String, TransProtInfo>> hmProts = new HashMap<>();
        int transIdx = 1;
        int transProt = 0;
        
        for(String trans : lstTrans) {
            XYChart.Series<String, Number> series = new XYChart.Series();
            series.setName(trans);
            expValues = hmTransExp.get(trans);
            idx = 0;
            
            //Error bars
            series2 = new XYChart.Series();
            
            for(String name : names){
                //max 20% y min 5%
                //double random = (int)Math.floor(Math.random()*(expValues[0][idx]*0.1-expValues[0][idx]*0.05)+expValues[0][idx]*0.05);
                series.getData().add(new XYChart.Data(name, expValues[0][idx]));
                series2 = new XYChart.Series();
                series2.getData().add(new XYChart.Data(name, expValues[0][idx]+expValues[1][idx]));//+random));
                series2.getData().add(new XYChart.Data(name, expValues[0][idx]-expValues[1][idx++]));//-random));
                chartTranscripts.getData().addAll(series2);
                for(int i = 0; i < series2.getData().size(); i++) {
                    XYChart.Data item = series2.getData().get(i);
                    item.getNode().lookup(".chart-line-symbol").setStyle("-fx-stroke: 0px; -fx-background-color: " + "transparent" + "," + Controls.getChartHexColor(transIdx-1) + ";-fx-background-radius: 0px;");
                    series2.getNode().lookup(".chart-series-line").setStyle("-fx-stroke:" + Controls.getChartHexColor(transIdx-1) + ";-fx-stroke-width: 1px;");
                }
            }
            
            TransProtInfo tpi = null;
            String protein = project.data.getTransProtein(trans);
            if(!protein.isEmpty()) {
                HashMap<String, TransProtInfo> hmTrans;
                if(hmProts.containsKey(protein))
                    hmTrans = hmProts.get(protein);
                else {
                    hmTrans = new HashMap<>();
                    hmProts.put(protein, hmTrans);
                }
                tpi = new TransProtInfo(trans, 0, transProt++, 0);
                hmTrans.put(trans, tpi);
            }
            
            //we want the series the last to see the tooltip
            chartTranscripts.getData().add(series);
            
            for(Node n : chartTranscripts.getChildrenUnmodifiable()){
                if(n instanceof Legend){
                    while(((Legend)n).getChildren().size()>transIdx){
                        for(final Node legendItem : ((Legend)n).getChildren()){
                            int g = legendItem.toString().length();
                            //g=52 happens when a series is created without a name
                            if(g<=52){
                                ((Legend)n).getChildren().remove(legendItem);
                                break;
                            }
                        }
                    }
                }
            }
            
            if(transIdx==lstTrans.size() && lstTrans.size()!=0){
                for(Node n : chartTranscripts.getChildrenUnmodifiable()){
                    if(n instanceof Legend){
                        for(int k = 0; k<lstTrans.size(); k++){
                            Legend.LegendItem legendItem = ((Legend)n).getItems().get((k+1)*(names.length+1)-1);
                            legendItem.getSymbol().setStyle("-fx-background-color: " + Controls.getChartHexColor(k) + ",white;");
                        }
                    }
                }
            }
            
            for(int i = 0; i < series.getData().size(); i++) {
                XYChart.Data item = series.getData().get(i);
                String tooltip = trans + " " + names[i] + ": " + String.format("%.2f", expValues[0][i]) + " with a " + String.format("%.2f", expValues[1][i]) + " standard deviation";
                //diff color and line type
                if(!protein.isEmpty()) {
                    series.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: " + Controls.getChartHexColor(transIdx-1) + "; -fx-stroke-width: 2px;");
                    tooltip += "\nProtein: " + protein;
                    if(tpi != null) {
                        //tpi.expsum += expValues[i];
                        tpi.expsum += expValues[0][i];
                        ObservableList<String> lst = item.getNode().getStyleClass(); // .lookup(".chart-line-symbol").getStyle();
                        for(String sc : lst) {
                            if(sc.startsWith("default-color")) {
                                tpi.colorIdx = Integer.parseInt(sc.substring("default-color".length()));
                                break;
                            }
                        }
                    }
                }
                else {
                    // do non-coding transcripts using a dashed line for visual feedback
                    series.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: " + Controls.getChartHexColor(transIdx-1) + "; -fx-stroke-dash-array: 1 5; -fx-stroke-width: 2px;");
                }
                tt = new Tooltip(tooltip);
                tt.setStyle("-fx-font: 13 system;");
                Tooltip.install(item.getNode(), tt);
                
                item = series.getData().get(i);
                item.getNode().lookup(".chart-line-symbol").setStyle("-fx-stroke-width: 0.5px; -fx-background-color: transparent, " + Controls.getChartHexColor(transIdx-1) + ";");
                //item.getNode().lookup(".chart-line-symbol").setStyle("-fx-stroke: 0px; -fx-background-color: " + Controls.getChartHexColor(transIdx-1) + ",white;");
            }
            transIdx++;
        }
        app.export.setupChartExport(chartTranscripts, "Transcripts Expression Levels", "tappAS_"+gene+"_TransExpLevels.png", null, tabBase);

        // proteins chart
        // add the proteins in the same order as the transcripts and use the same color
        // when there are multiple transcripts coding for the same protein,
        // the color of the transcript with the highest expression sum is used
        chartProteins = new LineChartTimeSeries(new CategoryAxis(), new NumberAxis(), project.data.getGroupsTimeNames().length);
        chartProteins.setMultipleGroupsTS(timeMultiple);
        chartProteins.setAnimated(false);
        chartProteins.setTitle(gene + " - CDS Expression");
        chartProteins.getXAxis().setLabel(timeSeries? "Experimental Group and Time" : "Experimental Group");
        chartProteins.getYAxis().setLabel("Expression Level");
        chartProteins.setStyle(style);
        chartProteins.lookup(".axis").setStyle(styleLabels);
        chartProteins.lookup(".axis-label").setStyle(styleLabels);
        chartProteins.lookup(".chart-legend").setStyle(styleLegend);
        chartProteins.getYAxis().setStyle(styleLabels);
        chartProteins.addTimeLines(lstTimeLines);
        grdCharts.add(chartProteins, 2, 0);
        HashMap<String, Object> hmGeneTrans = new HashMap<>();
        for(String tr : hmTransExp.keySet())
            hmGeneTrans.put(tr, null);
        ArrayList<String> lstProteins = new ArrayList<>();
        lstProteins.addAll(hmProtExp.keySet());
        Collections.sort(lstProteins, String.CASE_INSENSITIVE_ORDER);
        int protIdx = 1;
        
        //we need to create a list with only one transcript per protein
        ArrayList<String> lstAux;
        ArrayList<Integer> colors = new ArrayList<Integer>();
        for(int i=0;i<lstProteins.size();i++){
            colors.add(0);
        }
        
        for(String protein : lstProteins) {
            XYChart.Series<String, Number> series = new XYChart.Series();
            series.setName(protein);
            expValues = hmProtExp.get(protein);
            idx = 0;
            lstAux = project.data.getProteinTrans(protein, hmGeneTrans);
            int color_trans = 0;
            
            //Error bars
            series2 = new XYChart.Series();
            
            for(String name : names){
                //max 20% y min 5%
                //double random = (int)Math.floor(Math.random()*(expValues[0][idx]*0.1-expValues[0][idx]*0.05)+expValues[0][idx]*0.05);
                series.getData().add(new XYChart.Data(name, expValues[0][idx]));series2 = new XYChart.Series();
                series2.getData().add(new XYChart.Data(name, expValues[0][idx]+expValues[1][idx]));//+random));
                series2.getData().add(new XYChart.Data(name, expValues[0][idx]-expValues[1][idx++]));//-random));
                chartProteins.getData().addAll(series2);
                //always a protein has its own transcript
                for(color_trans = 0; color_trans < lstTrans.size(); color_trans++){
                    //if this proteins belong to k transcript paint all
                    if(lstAux.contains(lstTrans.get(color_trans))){
                        for(int i = 0; i < series2.getData().size(); i++) {
                            XYChart.Data item = series2.getData().get(i);
                            item.getNode().lookup(".chart-line-symbol").setStyle("-fx-stroke: 0px; -fx-background-color: " + "transparent" + "," + Controls.getChartHexColor(color_trans) + ";-fx-background-radius: 0px;");
                            series2.getNode().lookup(".chart-series-line").setStyle("-fx-stroke:" + Controls.getChartHexColor(color_trans) + ";-fx-stroke-width: 1px;");
                        }
                    break;
                    }
                }
                colors.set(protIdx-1, color_trans);
            }
            
            double expsum = 0;
            HashMap<String, TransProtInfo> hm = hmProts.get(protein);
            for(TransProtInfo tpi : hm.values()) {
                if(tpi.expsum > expsum) {
                    expsum = tpi.expsum;
                    idx = tpi.colorIdx;
                }
            }
            
            //we want the series the last to see the tooltip
            chartProteins.getData().addAll(series);
            
            for(Node n : chartProteins.getChildrenUnmodifiable()){
                if(n instanceof Legend){
                    while(((Legend)n).getChildren().size()>protIdx){
                        for(final Node legendItem : ((Legend)n).getChildren()){
                            int g = legendItem.toString().length();
                            //g=52 happens when a series is created without a name
                            if(g<=52){
                                ((Legend)n).getChildren().remove(legendItem);
                                break;
                            }
                        }
                    }
                }
            }
            
            if(protIdx==lstProteins.size() && lstProteins.size()!=0){
                for(Node n : chartProteins.getChildrenUnmodifiable()){
                    if(n instanceof Legend){
                        for(int k = 0; k<lstProteins.size(); k++){
                            Legend.LegendItem legendItem = ((Legend)n).getItems().get((k+1)*(names.length+1)-1);
                            legendItem.getSymbol().setStyle("-fx-background-color: " + Controls.getChartHexColor(colors.get(k)) + ",white;");
                        }
                    }
                }
            }

            
            for(int i = 0; i < series.getData().size(); i++) {
                XYChart.Data item = series.getData().get(i);
                //item.getNode().lookup(".chart-line-symbol").setStyle("-fx-stroke-width: 2px; -fx-background-color: " + Controls.getChartHexColor(protIdx-1) + ", white;");
                item.getNode().lookup(".chart-line-symbol").setStyle("-fx-stroke-width: 0.5px; -fx-background-color: transparent, " + Controls.getChartHexColor(color_trans) + ";");
                series.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: " + Controls.getChartHexColor(color_trans) + "; -fx-stroke-width: 2px;");
                String tooltip = protein + " " + names[i] + ": " + String.format("%.2f", expValues[0][i])+ " with a " + String.format("%.2f", expValues[1][i]) + " standard deviation";
                ArrayList<String> lst = project.data.getProteinTrans(protein, hmGeneTrans);
                String transcripts = "";
                int cnt = 0;
                for(String tr : lst) {
                    if(++cnt > 4) {
                        cnt = 1;
                        transcripts += ",\n    " + tr;
                    }
                    else
                        transcripts += transcripts.isEmpty()? tr : (", " + tr);
                }
                if(!transcripts.isEmpty())
                    tooltip += "\nTranscript(s): " + transcripts;
                tt = new Tooltip(tooltip);
                tt.setStyle("-fx-font: 13 system;");
                Tooltip.install(item.getNode(), tt);
            }
            protIdx++;
        }
        app.export.setupChartExport(chartProteins, "Proteins Expression Levels", "tappAS_"+gene+"_ProteinsExpLevels.png", null, tabBase);

        // setup chart export menu
        setupExportMenu();
        setFocusNode(chartGene, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export Gene chart");
        item.setOnAction((event) -> { 
            
            SnapshotParameters sP = new SnapshotParameters();
            double x = chartGene.getWidth();
            double ratio = 3840/x;
            chartGene.setScaleX(ratio);
            chartGene.setScaleY(ratio);
            WritableImage img = chartGene.snapshot(sP, null);
            double newX = chartGene.getWidth();
            ratio = x/newX;
            chartGene.setScaleX(ratio);
            chartGene.setScaleY(ratio);
            
            app.export.exportImage("Gene Expression Levels", "tappAS_"+gene+"_GeneExpLevels.png", img);
        });
        cm.getItems().add(item);
        item = new MenuItem("Export Transcript chart");
        item.setOnAction((event) -> { 
            
            SnapshotParameters sP = new SnapshotParameters();
            double x = chartTranscripts.getWidth();
            double ratio = 3840/x;
            chartTranscripts.setScaleX(ratio);
            chartTranscripts.setScaleY(ratio);
            WritableImage img = chartTranscripts.snapshot(sP, null);
            double newX = chartTranscripts.getWidth();
            ratio = x/newX;
            chartTranscripts.setScaleX(ratio);
            chartTranscripts.setScaleY(ratio);
            
            app.export.exportImage("Transcripts Expression Levels", "tappAS_"+gene+"_TransExpLevels.png", img);
        });
        cm.getItems().add(item);
        item = new MenuItem("Export CDS chart");
        item.setOnAction((event) -> { 
            
            SnapshotParameters sP = new SnapshotParameters();
            double x = chartProteins.getWidth();
            double ratio = 3840/x;
            chartProteins.setScaleX(ratio);
            chartProteins.setScaleY(ratio);
            WritableImage img = chartProteins.snapshot(sP, null);
            double newX = chartProteins.getWidth();
            ratio = x/newX;
            chartProteins.setScaleX(ratio);
            chartProteins.setScaleY(ratio);
            
            app.export.exportImage("Proteins Expression Levels", "tappAS_"+gene+"_ProteinsExpLevels.png", img);
        });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Export Panel chart");
        item.setOnAction((event) -> {
            SnapshotParameters sP = new SnapshotParameters();
            double x = paneContents.getWidth();
            double ratio = 3840/x;
            paneContents.setScaleX(ratio);
            paneContents.setScaleY(ratio);

            WritableImage img = paneContents.snapshot(new SnapshotParameters(), null);
            double newX = paneContents.getWidth();
            ratio = x/newX;
            paneContents.setScaleX(ratio);
            paneContents.setScaleY(ratio);
            app.export.exportImage("Data Summary Panel", "tappAS_"+ gene +"_ExpressionCharts_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    }
    
    //
    // Data Load
    //
    /*@Override
    protected void runDataLoadThread() {
        boolean loaded = false;
        
        // load gene and transcript expression - must have some
        HashMap<String, Object> hmTrans = project.data.getGeneTrans(gene);
        hmGeneExp = project.data.getMeanExpressionLevelsHM(DataApp.DataType.GENE, hmTrans);
        if(!hmGeneExp.isEmpty()) {
            hmTransExp  = project.data.getMeanExpressionLevelsHM(DataApp.DataType.TRANS, hmTrans);
            if(!hmTransExp.isEmpty()) {
                // there may not be any proteins - OK to be empty
                hmProtExp  = project.data.getMeanExpressionLevelsHM(DataApp.DataType.PROTEIN, hmTrans);
                loaded = true;
            }
        }
        if(loaded)
            showSubTab();
        else
            app.ctls.alertWarning("Gene Data Visualization - Expression Charts", "Unable to load charts expression data");
    }*/

    @Override
    protected void runDataLoadThread() {
        boolean loaded = false;
        
        // load gene and transcript expression - must have some
        HashMap<String, Object> hmTrans = project.data.getGeneTrans(gene);
        hmGeneExp = project.data.getMeanExpressionLevelsSD_HM(DataApp.DataType.GENE, hmTrans);
        if(!hmGeneExp.isEmpty()) {
            hmTransExp  = project.data.getMeanExpressionLevelsSD_HM(DataApp.DataType.TRANS, hmTrans);
            if(!hmTransExp.isEmpty()) {
                // there may not be any proteins - OK to be empty
                hmProtExp  = project.data.getMeanExpressionLevelsSD_HM(DataApp.DataType.PROTEIN, hmTrans);
                loaded = true;
            }
        }
        if(loaded)
            showSubTab();
        else
            app.ctls.alertWarning("Gene Data Visualization - Expression Charts", "Unable to load charts expression data");
    }
    
    //
    // Data Classes
    //
    private static class TransProtInfo {
        String trans;
        int idx, colorIdx;
        double expsum;
        public TransProtInfo(String trans, int idx, int colorIdx, double expsum) {
            this.trans = trans;
            this.idx = idx;
            this.colorIdx = colorIdx;
            this.expsum = expsum;
        }
    }
}
