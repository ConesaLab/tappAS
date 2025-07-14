/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabFEAEnriched extends SubTabBase {
    static double MIN_DIAMETER = 3.5;
    static double MEDIUM_DIAMETER = 6.33;
    static double UPPER_MEDIUM_DIAMETER = 10.66;
    static double MAX_DIAMETER = 17;
    
    static double[] logAdjPValue = {1.0, 4.0, 8.0};
    
    AnchorPane paneContents;
    ScrollPane spScatChart;
    ScatterChart scatChart;
    
    DataFEA feaData;
    DlgFEAnalysis.Params feaParams;
    String analysisId = "";
    ArrayList<ArrayList<DataFEA.FEAResultsData>> lstChartData = null;
    DlgFEAFilterFeatures.Params filterParams = null;
    HashMap<String, HashMap<String, Object>> hmFeatureIDs = new HashMap<>();
    
    public SubTabFEAEnriched(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = false;
        subTabInfo.btnExport = true;
        subTabInfo.btnDataViz = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            feaData = new DataFEA(project);
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectDataViz.Panels.ENRICHEDFEA.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectDataViz.Panels.ENRICHEDFEA.name().length()).trim());
            }
            if(args.containsKey("id")) {
                analysisId = (String) args.get("id");
                feaParams = DlgFEAnalysis.Params.load(feaData.getFEAParamsFilepath(analysisId));
                String shortName = getShortName(feaParams.name);
                subTabInfo.title = "FEA Enriched: " + shortName;
                subTabInfo.tooltip = "FEA enriched features for '" + feaParams.name.trim() + "'";
                setTabLabel(subTabInfo.title, subTabInfo.tooltip);
                filterParams = new DlgFEAFilterFeatures.Params(feaParams.getFeaturesName());       
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
    private void showSubTab() {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        spScatChart = (ScrollPane) tabNode.lookup("#spScatChart");
        scatChart = (ScatterChart) spScatChart.getContent().lookup("#scatChart");
        ContextMenu cm = new ContextMenu();
        MenuItem mi = new MenuItem("Enriched Features Display Filter...");
        mi.setOnAction((event) -> { selectFeaturesDisplayed(); });
        cm.getItems().add(mi);
        subTabInfo.cmDataViz = cm;
        
        spScatChart.widthProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            scatChart.setPrefWidth((double)newValue - 20);
            scatChart.setMaxWidth((double)newValue - 20);
        });
        scatChart.getXAxis().setLabel("Number of Test Genes with Feature ID");
        scatChart.getYAxis().setLabel("Feature IDs");
        scatChart.setLegendSide(Side.RIGHT);
        scatChart.setLegendVisible(true);
        showData();
        app.ctls.setupChartExport(scatChart, "FEA Enriched Features", "tappAS_FEA_"+feaParams.name.trim()+"_Enriched_Features_Chart.png", null);
        scatChart.getXAxis().tickLabelFontProperty().set(Font.font(10));

        setupExportMenu();
        setFocusNode(scatChart, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export enriched features chart image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = scatChart.getWidth();
            double ratio = 3840/x;
            scatChart.setScaleX(ratio);
            scatChart.setScaleY(ratio);
            WritableImage img = scatChart.snapshot(sP, null);
            double newX = scatChart.getWidth();
            ratio = x/newX;
            scatChart.setScaleX(ratio);
            scatChart.setScaleY(ratio);
            
            //WritableImage img = scatChart.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("FEA Enriched Features", "tappAS_FEA_"+feaParams.name.trim()+"_Enriched_Features.png", (Image)img);
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
            app.export.exportImage("FEA Enriched Features", "tappAS_FEA_"+feaParams.name.trim()+"_Enriched_Features_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void showData() {
        String title;
        boolean topn = true;
        boolean manual = false;
        ArrayList<String> lstFeatures = null;
        if(filterParams.type.equals(DlgFEAFilterFeatures.Params.Type.TOPN))
            title = "Top " + filterParams.n + " Feature IDs for each Feature";
        else if(filterParams.type.equals(DlgFEAFilterFeatures.Params.Type.MANUAL)) {
            topn = false;
            manual = true;
            title = "Manually Selected Features";
        }else{
            topn = false;
            title = "By List Selected Features";
            lstFeatures = Utils.loadSingleItemListFromFile(filterParams.file, true);
            // we have to create a new list to get all the features and not only significants 
            lstChartData = feaData.getFEAChartData(feaParams.dataType.name(), analysisId, feaParams.sigValue);
            if(lstFeatures.size() == 0)
                app.ctls.alertInformation("FEA Enriched Features", "We found 0 features in the Feature List.");
        }
        scatChart.setTitle("FEA Enriched Features - " + title);
        
        int totalFeatures = 0;
        boolean maxedout = false;
        int minx = Integer.MAX_VALUE;
        int maxx = 0;
        hmFeatureIDs.clear();
        ArrayList<XYChart.Series<Number, String>> lstSeries = new ArrayList<>();
        ArrayList<ArrayList<DataFEA.FEAResultsData>> lstDisplayData = new ArrayList<>();
        for(ArrayList<DataFEA.FEAResultsData> lst : lstChartData) {
            if(maxedout)
                break;
            
            XYChart.Series<Number, String> series = new XYChart.Series<>();
            DataFEA.FEAResultsData ffrd = lst.get(0);
            series.setName(ffrd.category);
            HashMap<String, Object> hmIDs = new HashMap<>();
            hmFeatureIDs.put(ffrd.category, hmIDs);

            int cnt = 0;
            boolean addSeries = false;
            ArrayList<DataFEA.FEAResultsData> lstDD = null;
            for(DataFEA.FEAResultsData frd : lst) {
                boolean add = false;
                if(topn) {
                    if(cnt++ < filterParams.n)
                        add = true;
                }
                else if(manual){
                    if(filterParams.hmFeatureIDs.containsKey(frd.category)) {
                        HashMap<String, Object> hm = filterParams.hmFeatureIDs.get(frd.category);
                        if(hm.isEmpty() || hm.containsKey(frd.feature))
                            add = true;
                    }
                }else{ //by list
                    if(lstFeatures.contains(frd.feature))
                        add = true;
                }
                if(add) {
                    totalFeatures++;
                    addSeries = true;
                    if(frd.numDEInCat < minx)
                        minx = frd.numDEInCat;
                    if(frd.numDEInCat > maxx)
                        maxx = frd.numDEInCat;
                    if(frd.source.equals("miRWalk") || frd.source.equals("REACTOME"))
                        series.getData().add(new XYChart.Data(frd.numDEInCat, frd.feature));
                    else
                        series.getData().add(new XYChart.Data(frd.numDEInCat, frd.description));
                    if(lstDD == null) {
                        lstDD = new ArrayList<>();
                        lstDisplayData.add(lstDD);
                    }
                    lstDD.add(frd);
                }
                hmIDs.put(frd.feature, frd.adjPValue);
                
                // limit total number of features displayed
                if(totalFeatures >= DlgFEAFilterFeatures.Params.MAX_SELECTIONS) {
                    maxedout = true;
                    break;
                }
            }
            if(addSeries)
                lstSeries.add(series);
        }
        if(lstSeries.isEmpty())
            app.ctls.alertInformation("FEA Enriched Features", "There are no enriched features.");
        else if(maxedout)
            app.ctls.alertInformation("FEA Enriched Features", "The number of selected features exceeded the limit." + 
                    "\nOnly the first " + DlgFEAFilterFeatures.Params.MAX_SELECTIONS + " are being displayed.");
        scatChart.setPrefWidth((double)spScatChart.getWidth() - 20);
        scatChart.setMaxWidth((double)spScatChart.getWidth() - 20);
        scatChart.getData().clear();
        scatChart.getData().addAll(lstSeries);
        ValueAxis va = (ValueAxis)scatChart.getXAxis();
        if(minx == Integer.MAX_VALUE)
            minx = 20;
        else if(minx < 20)
            minx = 20;
        minx = minx/10 * 10 - 20;
        maxx = maxx/10 * 10 + 20;
        va.setAutoRanging(false);
        va.setLowerBound(minx);
        va.setUpperBound(maxx);
        va.setTickLabelFormatter(new NumberAxis.DefaultFormatter((NumberAxis)scatChart.getXAxis()) {
            @Override public String toString(Number object) {
                return String.format("" + object.intValue());
            }
        });
        
        //we want a height can see all the legend in vertical position
        double h = 300 + Math.max(100, (totalFeatures + 2) * 24);
        scatChart.setPrefHeight(h);
        scatChart.setMaxHeight(h);
        
        // draw symbols the way we want them to show up - will recycle colors if too many needed
        // we currently only have the 8 colors from java charts which should be sufficient
        // update Controls class to include more colors if needed
        Tooltip tt;
        int idxData = 0;
        int colorIdx = 0;
        for(XYChart.Series<Number, String> s : lstSeries) {
            ArrayList<DataFEA.FEAResultsData> lstSeriesData = lstDisplayData.get(idxData++);
            for(int i = 0; i < s.getData().size(); i++) {
                DataFEA.FEAResultsData seriesData = lstSeriesData.get(i);
                XYChart.Data item = s.getData().get(i);
                StackPane stackPane =  (StackPane) item.getNode();
                stackPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.0);");
                ObservableList<Node> lst = stackPane.getChildren();
                lst.clear();
                Circle circle = new Circle();
                
                double d = seriesData.adjPValue;
                double r = MIN_DIAMETER;
                if(Math.abs(Math.log10(d)) < logAdjPValue[0]){
                    r = MIN_DIAMETER;
                }else if(logAdjPValue[0] <= Math.abs(Math.log10(d)) && Math.abs(Math.log10(d)) <= logAdjPValue[1]){
                    r = MEDIUM_DIAMETER;
                }else if(logAdjPValue[1] <= Math.abs(Math.log10(d)) && Math.abs(Math.log10(d)) <= logAdjPValue[2]){
                    r = UPPER_MEDIUM_DIAMETER;
                }else if(logAdjPValue[2] <= Math.abs(Math.log10(d))){
                    r = MAX_DIAMETER;
                }
                
//                double d = Math.abs(seriesData.adjPValue != 0? Math.log10(seriesData.adjPValue) * 2 : MAX_DIAMETER);
//                d = Math.max(Math.min(d, MAX_DIAMETER), MIN_DIAMETER);
//                double r = d / 2.0;
                
                circle.setRadius(r);
                circle.setFill(Controls.chartColors[colorIdx]);
                circle.setStroke(Color.BLACK);
                stackPane.setPrefWidth(d);
                stackPane.setPrefHeight(d);
                lst.add(circle);
                tt = new Tooltip(item.getYValue() + ": " + seriesData.description +
                        "\nAdj. PValue: " + seriesData.adjPValue +
                        "\nTest Items: " + seriesData.numDEInCat +
                        "\nTotal Items: " + seriesData.numInCat);
                tt.setStyle("-fx-font: 13 system;");
                Tooltip.install(item.getNode(), tt);
            }
            // change colors for next feature
            if(++colorIdx >= Controls.chartColors.length)
                colorIdx = 0;
        }
        
        // update legend symbols to match our symbols
        Set<Node> items = scatChart.lookupAll("Label.chart-legend-item"); //Credit https://gist.github.com/jewelsea/1422628
        if(items != null && !items.isEmpty()) {
            int idx = 0;
            Node nodeItem = null;
            for (Node item : items) {
              Label label = (Label) item;
              final Circle lc = new Circle(5);
              lc.setFill(Controls.chartColors[idx]);
              label.setGraphic(lc);
              if(nodeItem == null)
                  nodeItem = item;
              idx++;
            }
        
            // incude additional information in the legend
            if(nodeItem != null) {
                Region rcc = (Region) scatChart.lookup(".chart-content");
                if(rcc != null)
                    rcc.setStyle("-fx-padding:6px 20px 6px 6px; -fx-font-size: 9;");
                //System.out.println(nodeItem.getParent().getClass().toString());
                ObservableList<com.sun.javafx.charts.Legend.LegendItem> lstli = ((com.sun.javafx.charts.Legend)nodeItem.getParent()).getItems();
                lstli.add(0, new com.sun.javafx.charts.Legend.LegendItem(" Features: "));
                lstli.add(new com.sun.javafx.charts.Legend.LegendItem(" -log10(Adjusted P-Value): "));
                Circle c = new Circle(MIN_DIAMETER);
                c.setFill(Color.TRANSPARENT);
                c.setStroke(Color.BLACK);
                StackPane sp = new StackPane(c);
                sp.setPrefHeight(MAX_DIAMETER*2);
                sp.setPrefWidth(MAX_DIAMETER*2);
                lstli.add(new com.sun.javafx.charts.Legend.LegendItem("less than " + logAdjPValue[0], sp));
                c = new Circle(MEDIUM_DIAMETER);
                c.setFill(Color.TRANSPARENT);
                c.setStroke(Color.BLACK);
                sp = new StackPane(c);
                sp.setPrefHeight(MAX_DIAMETER*2);
                sp.setPrefWidth(MAX_DIAMETER*2);
                lstli.add(new com.sun.javafx.charts.Legend.LegendItem(logAdjPValue[0] + " to " + logAdjPValue[1], sp));
                c = new Circle(UPPER_MEDIUM_DIAMETER);
                c.setFill(Color.TRANSPARENT);
                c.setStroke(Color.BLACK);
                sp = new StackPane(c);
                sp.setPrefHeight(MAX_DIAMETER*2);
                sp.setPrefWidth(MAX_DIAMETER*2);
                lstli.add(new com.sun.javafx.charts.Legend.LegendItem(logAdjPValue[1] + " to " + logAdjPValue[2], sp));
                c = new Circle(MAX_DIAMETER);
                c.setFill(Color.TRANSPARENT);
                c.setStroke(Color.BLACK);
                sp = new StackPane(c);
                sp.setPrefHeight(MAX_DIAMETER*2);
                sp.setPrefWidth(MAX_DIAMETER*2);
                lstli.add(new com.sun.javafx.charts.Legend.LegendItem("more than " + logAdjPValue[2], sp));
            }
        }
    }
    private void selectFeaturesDisplayed() {
        Window wnd = Tappas.getWindow();
        DlgFEAFilterFeatures dlg = new DlgFEAFilterFeatures(project, wnd);
        DlgFEAFilterFeatures.Params results = dlg.showAndWait(filterParams, hmFeatureIDs);
        if(results != null) {
            filterParams = results;
            showData();
        }
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        loadData();
        showSubTab();
    }
    // function also gets called for refresh
    private void loadData() {
        lstChartData = feaData.getFEAEnrichedChartData(feaParams.dataType.name(), analysisId, feaParams.sigValue);
    }
}
