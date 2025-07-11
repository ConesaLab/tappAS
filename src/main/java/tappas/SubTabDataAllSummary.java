/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import com.sun.javafx.charts.Legend;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import tappas.TabProjectDataViz.SummaryDataCount;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.*;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDataAllSummary extends SubTabBase {
    AnchorPane paneContents;
    ScrollPane spAll;
    TableView tblSummary;
    ContextMenu cmSummary;
    Pane panePCAChart;
    ScatterChart chartPCA;
    BarChart barDistribution, barAnnotation;
    PieChart pieSummaryTransStructCats;

    boolean hasData = false;
    String pcaFilepath;
    Path runScriptPath = null;

    public SubTabDataAllSummary(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            pcaFilepath = project.data.getMatrixPCAFilepath();
            hasData = Files.exists(Paths.get(pcaFilepath));
        }
        return result;
    }

    //
    // Internal Functions
    //
    private void showSubTab() {
        // get control objects
        paneContents = (AnchorPane) spAll.getContent().lookup("#paneContents");
        tblSummary = (TableView) spAll.getContent().lookup("#tblSummary");
                
        // setup data summary table
        ObservableList<TableColumn> cols = tblSummary.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Field"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Count"));
        HashMap<String, Object> hmTrans = project.data.getResultsTrans();
        HashMap<String, HashMap<String, Object>> hmGeneTrans = project.data.getResultsGeneTrans();
        int protcnt = project.data.getTransProteinCount(hmTrans);
        int transcnt = hmTrans.size();
        int genecnt = hmGeneTrans.size();
        ObservableList<SummaryDataCount> sumdata = FXCollections.observableArrayList();
        sumdata.add(new SummaryDataCount("Transcripts", transcnt));
        sumdata.add(new SummaryDataCount("Proteins", protcnt));
        sumdata.add(new SummaryDataCount("Genes", genecnt));
        tblSummary.setRowFactory(tableView -> {
            TableRow<SummaryDataCount> row = new TableRow<>();
            row.selectedProperty().addListener((obs, oldData, newData) -> {
                SummaryDataCount sd = row.getItem();
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
        tblSummary.setItems(sumdata);
        if(tblSummary.getItems().size() > 0)
            tblSummary.getSelectionModel().select(0);
        app.export.addCopyToClipboardHandler(tblSummary);
        tblSummary.setOnMouseClicked((event) -> {
            if(cmSummary != null)
                cmSummary.hide();
            cmSummary = null;
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                Node node = event.getPickResult().getIntersectedNode();
                if(node != null) {
                    System.out.println("node: " + node);
                    if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        node = node.getParent();
                        System.out.println("parent: " + node);
                    }
                    if(node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        cmSummary = new ContextMenu();
                        app.export.setupSimpleTableExport(tblSummary, cmSummary, false, "tappAS_DataSummary.tsv");
                        cmSummary.setAutoHide(true);
                        cmSummary.show(tblSummary, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });
        
        // display charts
        barDistribution = (BarChart) spAll.getContent().lookup("#barChartDistribution");
        showDistributionPerGene(barDistribution);
        barAnnotation = (BarChart) spAll.getContent().lookup("#barAnnotation");
        showAnnotationFeaturesBarChart(barAnnotation);
        
        pieSummaryTransStructCats = (PieChart) spAll.getContent().lookup("#pieSummaryTransStructCats");
        showTransStructCatsChart(pieSummaryTransStructCats);
        
        setupExportMenu();
        setFocusNode(tblSummary, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export data summary table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblSummary, true, "tappAS_Data_Summary.tsv"); });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Export transcripts and proteins distribution chart image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = barDistribution.getWidth();
            double ratio = 3840/x;
            barDistribution.setScaleX(ratio);
            barDistribution.setScaleY(ratio);
            WritableImage img = barDistribution.snapshot(sP, null);
            double newX = barDistribution.getWidth();
            ratio = x/newX;
            barDistribution.setScaleX(ratio);
            barDistribution.setScaleY(ratio);
            
            //WritableImage img = barDistribution.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Transcripts and Proteins Distribution Chart", "tappAS_Data_Summary_Transcripts_Proteins_Distribution.png", (Image)img);
        });
        cm.getItems().add(item);
        item = new MenuItem("Export transcripts structural categories chart image...");
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
            app.export.exportImage("Transcripts Structural Categories Chart", "tappAS_Data_Summary_Transcripts_Structural_Categories.png", (Image)img);
        });
        cm.getItems().add(item);
        item = new MenuItem("Export annotation sources chart image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = barAnnotation.getWidth();
            double ratio = 3840/x;
            barAnnotation.setScaleX(ratio);
            barAnnotation.setScaleY(ratio);
            WritableImage img = barAnnotation.snapshot(sP, null);
            double newX = barAnnotation.getWidth();
            ratio = x/newX;
            barAnnotation.setScaleX(ratio);
            barAnnotation.setScaleY(ratio);
            
            //WritableImage img = barAnnotation.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Annotation Sources Chart", "tappAS_Data_Summary_Annotation_Sources.png", (Image)img);
        });
        cm.getItems().add(item);
        item = new MenuItem("Export expression levels PCA plot image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = chartPCA.getWidth();
            double ratio = 3840/x;
            chartPCA.setScaleX(ratio);
            chartPCA.setScaleY(ratio);
            WritableImage img = chartPCA.snapshot(sP, null);
            double newX = chartPCA.getWidth();
            ratio = x/newX;
            chartPCA.setScaleX(ratio);
            chartPCA.setScaleY(ratio);
            
            //WritableImage img = chartPCA.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Expression Levels PCA Plot", "tappAS_Data_Summary_ExpLevels_PCA.png", (Image)img);
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
            app.export.exportImage("Data Summary Panel", "tappAS_Data_Summary_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    // Note: We always display one symbol for each experimental group for all experiment types
    private void showPCAPlot(DataProject.ExpLevelPCAData data) {
        DataInputMatrix.ExpMatrixParams params = project.data.getInputMatrixParams();

        // all this work is just to try to use a couple of standard ranges if possible
        double xmax_neg = 0.0;
        double xmax_pos = 0.0;
        double ymax_neg = 0.0;
        double ymax_pos = 0.0;
        int grpnum = 0;
        String[] groupNames = project.data.getGroupNames();
        for(XYChart.Series<Number, Number> series : data.lstSeries) {
            series.setName(groupNames[grpnum++]);
            for(XYChart.Data xy : series.getData())
            {
                double x = (double) xy.getXValue();
                double y = (double) xy.getYValue();
                if(x < 0) {
                    if(x < xmax_neg)
                        xmax_neg = x;
                }
                else {
                    if(x > xmax_pos)
                        xmax_pos = x;
                }
                if(y < 0) {
                    if(y < ymax_neg)
                        ymax_neg = y;
                }
                else {
                    if(y > ymax_pos)
                        ymax_pos = y;
                }
            }
        }
        // check for standard range configurations
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        boolean autoranging = false;
        if(Math.abs(xmax_neg) < 50 && xmax_pos < 50 && Math.abs(ymax_neg) < 50 && ymax_pos < 50) {
            xAxis = new NumberAxis(-50, 50, 25);
            yAxis = new NumberAxis(-50, 50, 25);
        }
        else {
            if(Math.abs(xmax_neg) < 100 && xmax_pos < 100 && Math.abs(ymax_neg) < 100 && ymax_pos < 100) {
                xAxis = new NumberAxis(-100, 100, 50);
                yAxis = new NumberAxis(-100, 100, 50);
            }
            else
                autoranging = true;
        }
        chartPCA = new ScatterChart(xAxis, yAxis);
        chartPCA.setTitle("Expression Levels PCA Plot");
        chartPCA.setStyle("-fx-background-color: white;");
        chartPCA.setLegendVisible(true);
        chartPCA.getXAxis().setAutoRanging(autoranging);
        chartPCA.setAnimated(false);
        app.ctls.setupChartExport(chartPCA, "Transcripts Expression Levels Density Plot", "tappAS_TransExpLevelsDensityPlot.png", null);

        chartPCA.getXAxis().setLabel("PC 1 (" + (int)(data.pc1 * 100) + "%)");
        chartPCA.getYAxis().setLabel("PC 2 (" + (int)(data.pc2 * 100) + "%)");
        chartPCA.getData().addAll(data.lstSeries);
        chartPCA.prefHeightProperty().bind(panePCAChart.heightProperty());
        chartPCA.prefWidthProperty().bind(panePCAChart.widthProperty());
        panePCAChart.getChildren().add(chartPCA);

        boolean timeSeries = project.data.isTimeCourseExpType();
        ArrayList<Color> lstColors = new ArrayList<>();
        String[] timeNames = project.data.getTimePointNames();
        HashMap<String, Integer> hmTimeIdx = new HashMap<>();
        int idx = 0;
        for(String time : timeNames)
            hmTimeIdx.put(time, idx++);
        for(DlgInputData.Params.ExpMatrixGroup emg : params.lstGroups) {
            for(DlgInputData.Params.ExpMatrixTime emt : emg.lstTimes) {
                int idxcolor = hmTimeIdx.get(emt.name) % Controls.chartOpacityColors.length;
                for(String sname : emt.lstSampleNames)
                    lstColors.add(Controls.chartOpacityColors[idxcolor]);
            }
        }
        grpnum = 0;
        int idxcolor = 0;
        for(XYChart.Series xys : data.lstSeries) {
            int samnum = 0;
            ObservableList<XYChart.Data> lstData = xys.getData();
            for(XYChart.Data xyd : lstData) {
                if(timeSeries) {
                    StackPane stackPane =  (StackPane) xyd.getNode();
                    stackPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.0);");
                    stackPane.setPrefWidth(10);
                    stackPane.setPrefHeight(10);
                    ObservableList<Node> lst = stackPane.getChildren();
                    lst.clear();
                    Node symbol = getSymbol(grpnum, lstColors.get(idxcolor++), false);
                    lst.add(symbol);
                }else{
                    StackPane stackPane =  (StackPane) xyd.getNode();
                    stackPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.0);");
                    stackPane.setPrefWidth(10);
                    stackPane.setPrefHeight(10);
                    ObservableList<Node> lst = stackPane.getChildren();
                    lst.clear();
                    Node symbol = getSymbolCaseControl(grpnum, false);
                    lst.add(symbol);
                }
                String name = groupNames[grpnum];
                if(!params.experimentType.equals(DataApp.ExperimentType.Two_Group_Comparison))
                    name += " Time " + params.getGroupSampleTime(grpnum, samnum);
                Tooltip tt = new Tooltip(name + ": Sample " + params.getGroupSampleName(grpnum, samnum) + " (" + xyd.getXValue() + ", " + xyd.getYValue() + ")");
                tt.setStyle("-fx-font: 13 system;");
                Tooltip.install(xyd.getNode(), tt);
                samnum++;
            }
            grpnum++;
        }
        if(timeSeries) {
            // update legend symbols to match our symbols
            Set<Node> items = chartPCA.lookupAll("Label.chart-legend-item"); //Credit https://gist.github.com/jewelsea/1422628
            if(items != null && !items.isEmpty()) {
                Node nodeItem = null;
                for (Node item : items) {
                    nodeItem = item;
                    break;
                }
                if(nodeItem != null) {
                    ObservableList<com.sun.javafx.charts.Legend.LegendItem> lstli = ((com.sun.javafx.charts.Legend)nodeItem.getParent()).getItems();
                    lstli.clear();
                    grpnum = 0;
                    lstli.add(new com.sun.javafx.charts.Legend.LegendItem("Groups:"));
                    for(String name : groupNames) {
                        Node symbol = getSymbol(grpnum, null, true);
                        lstli.add(new com.sun.javafx.charts.Legend.LegendItem(groupNames[grpnum], symbol));
                        grpnum++;
                    }
                    lstli.add(new com.sun.javafx.charts.Legend.LegendItem("Times:"));
                    for(String time : timeNames) {
                        Rectangle rect = new Rectangle(10, 10);
                        idxcolor = hmTimeIdx.get(time) % Controls.chartOpacityColors.length;
                        rect.setFill(Controls.chartOpacityColors[idxcolor]);
                        rect.setStroke(Color.TRANSPARENT);
                        lstli.add(new com.sun.javafx.charts.Legend.LegendItem("" + time, rect));

                    }
                }
            }
        //update case-controls legend color PCA
        }else{
            // update legend symbols to match our symbols
            Set<Node> items = chartPCA.lookupAll("Label.chart-legend-item"); //Credit https://gist.github.com/jewelsea/1422628
            if(items != null && !items.isEmpty()) {
                Node nodeItem = null;
                for (Node item : items) {
                    nodeItem = item;
                    break;
                }
                if(nodeItem != null) {
                    ObservableList<com.sun.javafx.charts.Legend.LegendItem> lstli = ((com.sun.javafx.charts.Legend)nodeItem.getParent()).getItems();
                    lstli.clear();
                    grpnum = 0;
                    lstli.add(new com.sun.javafx.charts.Legend.LegendItem("Groups:"));
                    for(String name : groupNames) {
                        Node symbol = getSymbolCaseControl(grpnum, true);
                        lstli.add(new com.sun.javafx.charts.Legend.LegendItem(groupNames[grpnum], symbol));
                        grpnum++;
                    }
                }
            }
        }
    }
    private Node getSymbol(int idxsym, Color color, boolean legend) {
        Node node = null;
        Color fill, stroke;
        fill = legend? Color.TRANSPARENT : color;
        stroke = Color.BLACK;
        switch(idxsym) {
            case 0:
                Circle circle = new Circle(5);
                circle.setFill(fill);
                circle.setStroke(stroke);
                node = circle;
                break;
            case 1:
                Polygon poly = new Polygon(new double[]{ 0.0, 10.0, 10.0, 10.0, 5.0, 0.0 });
                poly.setFill(fill);
                poly.setStroke(stroke);
                node = poly;
                break;
            case 2:
                Polygon diamond = new Polygon(new double[]{ 5.0, 12.0, 1.0, 6.0, 5.0, 0.0, 9.0, 6.0});
                diamond.setFill(fill);
                diamond.setStroke(stroke);
                node = diamond;
                break;
            case 3:
                Ellipse elly = new Ellipse(6, 4);
                elly.setFill(fill);
                elly.setStroke(stroke);
                node = elly;
                break;
        }
        return node;
    }
    
    private Node getSymbolCaseControl(int idxsym, boolean legend) {
        Node node = null;
        Color fill, stroke;
        stroke = Color.BLACK;
        switch(idxsym) {
            case 0:
                Circle circle = new Circle(5);
                circle.setFill(Controls.chartOpacityColors[0]);
                circle.setStroke(stroke);
                node = circle;
                break;
            case 1:
                Polygon poly = new Polygon(new double[]{ 0.0, 10.0, 10.0, 10.0, 5.0, 0.0 });
                poly.setFill(Controls.chartOpacityColors[2]);
                poly.setStroke(stroke);
                node = poly;
                break;
            case 2:
                Polygon diamond = new Polygon(new double[]{ 5.0, 12.0, 1.0, 6.0, 5.0, 0.0, 9.0, 6.0});
                diamond.setFill(Controls.chartOpacityColors[1]);
                diamond.setStroke(stroke);
                node = diamond;
                break;
            case 3:
                Ellipse elly = new Ellipse(6, 4);
                elly.setFill(Controls.chartOpacityColors[3]);
                elly.setStroke(stroke);
                node = elly;
                break;
        }
        return node;
    }
    private void showAnnotationFeaturesBarChart(BarChart barChart) {
        HashMap<String, DataAnnotation.AnnotSourceStats> hmCounts = project.data.getAnnotSourceStats(project.data.getResultsTrans());

        XYChart.Series<String, Number> series[] = new XYChart.Series[2];
        DataAnnotation.AnnotSourceStats counts;
        series[0] = new XYChart.Series<>();
        series[0].setName("Transcripts");
        series[1] = new XYChart.Series<>();
        series[1].setName("Genes");
        ArrayList<String> lstSources = new ArrayList<>();
        for(String source : hmCounts.keySet()) {
            lstSources.add(source);
            counts = hmCounts.get(source);
            if(source.length() > 10)
                source = source.substring(0, 10) + "...";
            series[0].getData().add(new XYChart.Data(source, counts.transCounts));
            series[1].getData().add(new XYChart.Data(source, counts.geneCounts));
        }
        barChart.getXAxis().setStyle("-fx-tick-label-font-size:0.75em;");
        barChart.getXAxis().setTickLabelRotation(45);
        for(XYChart.Series<String, Number> pcs : series)
            barChart.getData().add(pcs);
        app.ctls.setupChartExport(barChart, "Annotation Databases", "tappAS_AnnotationSources.png", null);
        Tooltip tt;
        for(int c = 0; c < series.length; c++) {
            for(int i = 0; i < series[c].getData().size(); i++) {
                XYChart.Data item = series[c].getData().get(i);
                String tooltip = ((c==0)? "Transcripts" : "Genes") + " with " + lstSources.get(i) + " annotations\nIsoforms: " + NumberFormat.getInstance().format(item.getYValue());
                tt = new Tooltip(tooltip);
                tt.setStyle("-fx-font: 13 system;");
                Tooltip.install(item.getNode(), tt);
            }
        }
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
        
        //sort pie chart 
        Comparator<PieChart.Data> comparator = Comparator.comparingDouble(PieChart.Data::getPieValue);
        comparator = comparator.reversed();
        FXCollections.sort(pieChartData, comparator);
        
        //show only top 10 elements if there are more than 10
        String text = "";
        if(pieChartData.size()>10){
            ObservableList<PieChart.Data> aux = FXCollections.observableArrayList();
            int cont = 0;
            double totalSum = 0;
            for(PieChart.Data i : pieChartData){
                if(cont<9)
                    aux.add(new PieChart.Data(i.getName(),i.getPieValue()));
                else{
                    text += i.getName() + " - " + String.valueOf(i.getPieValue()) + "% - Isoforms: " + NumberFormat.getInstance().format(hmAlignCats.get(i.getName())) + " out of " + NumberFormat.getInstance().format(totalcnt) + "\n";
                    totalSum += i.getPieValue();
                }
                cont++;
            }
            aux.add(new PieChart.Data("others", totalSum));
            pieChart.setData(aux);
            pieChartData = aux;
        }else{
            pieChart.setData(pieChartData);
        }
        
        pieChart.setStartAngle(90);
        app.ctls.setupChartExport(pieChart, name + "s Structural Categories Chart", "tappAS_" + name + "_StructuralCategories.png", null);
        Tooltip tt;
        for (PieChart.Data pcdata : pieChartData) {
            if(pcdata.getName().equals("others")){
                tt = new Tooltip(text);
            }else{
            tt = new Tooltip("  " + pcdata.getName() + "\n  Percent: " + String.valueOf(pcdata.getPieValue()) + "%  \n  Isoforms: " + NumberFormat.getInstance().format(hmAlignCats.get(pcdata.getName())) + " out of " + NumberFormat.getInstance().format(totalcnt));
            }
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(pcdata.getNode(), tt);
        }
    }
    
    private void showDistributionPerGene(BarChart barChart) {
        barChart.setTitle("Transcripts and Proteins per Gene");
        barChart.getYAxis().setLabel("Percentage");
        XYChart.Series<String, Number> seriesT = new XYChart.Series<>();
        seriesT.setName("Transcripts");
        DataProject.DistributionData ddexp = project.data.getExpressedTransDistribution(project.data.getResultsTrans());
        double val, pct;
        if(ddexp != null) {
            ddexp.series.getData().add(0, new XYChart.Data<>("0", 0));
            ddexp.series.getData().get(0).setExtraValue(0);
            ddexp.series.setName("Transcripts");
            for(int i = 1; i < ddexp.series.getData().size(); i++) {
                val = ddexp.series.getData().get(i).getYValue().doubleValue();
                pct = val * 100 / ddexp.genes;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                ddexp.series.getData().get(i).setYValue(pct);
                ddexp.series.getData().get(i).setExtraValue((int)val);
            }
            seriesT = ddexp.series;
        }
        barChart.getData().add(seriesT);

        Tooltip tt;
        if(ddexp != null) {
            for(int i = 0; i < ddexp.series.getData().size(); i++) {
                XYChart.Data item = ddexp.series.getData().get(i);
                tt = new Tooltip(i + " Transcript(s) per gene:  " + NumberFormat.getInstance().format(item.getExtraValue()) + " genes, " + NumberFormat.getInstance().format(item.getYValue()) + "% ");
                tt.setStyle("-fx-font: 13 system;");
                Tooltip.install(item.getNode(), tt);
            }
        }
        XYChart.Series<String, Number> seriesP = new XYChart.Series<>();
        ddexp = project.data.getGeneProteinDistribution(project.data.getResultsTrans());
        if(ddexp != null) {
            ddexp.series.setName("CDS");
            for(int i = 0; i < ddexp.series.getData().size(); i++) {
                val = ddexp.series.getData().get(i).getYValue().doubleValue();
                pct = val * 100 / ddexp.genes;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                ddexp.series.getData().get(i).setYValue(pct);
                ddexp.series.getData().get(i).setExtraValue((int)val);
            }
            seriesP = ddexp.series;
        }
        barChart.setCategoryGap(5);
        barChart.getXAxis().setLabel("Number of Transcripts and Proteins per Gene");
        barChart.getData().addAll(seriesP);
        app.ctls.setupChartExport(barChart, "Transcripts and Proteins Distribution Chart", "tappAS_GeneTransProtDistributionChart.png", null);
        if(ddexp != null) {
            for(int i = 0; i < ddexp.series.getData().size(); i++) {
                XYChart.Data item = ddexp.series.getData().get(i);
                tt = new Tooltip(i + " Protein(s) per gene:  " + NumberFormat.getInstance().format(item.getExtraValue()) + " genes, " + NumberFormat.getInstance().format(item.getYValue()) + "% ");
                tt.setStyle("-fx-font: 13 system;");
                Tooltip.install(item.getNode(), tt);
            }
        }
    }
    
    //
    // Data Load
    //
    
    // NOTE: this is only loading the PCA plot's data so the rest of the form is displayed
    //       immediately and only the plot is displayed after the data is obtained (if not already done)
    @Override
    protected void runDataLoadThread() {
        spAll = (ScrollPane) tabNode.lookup("#spAll");;
        pi = (ProgressIndicator) spAll.getContent().lookup("#piDensityPlot");
        panePCAChart = (Pane) spAll.getContent().lookup("#panePCAChart");
        chartPCA = (ScatterChart) spAll.getContent().lookup("#chartPCA");
        
        pi.layoutXProperty().bind(Bindings.subtract(Bindings.divide(panePCAChart.widthProperty(), 2.0), 25.0));
        pi.layoutYProperty().bind(Bindings.subtract(Bindings.divide(panePCAChart.heightProperty(), 2.0), 25.0));
        showSubTab();

        if(hasData) {
            DataProject.ExpLevelPCAData data = project.data.getExpLevelsPCAResults();
            showPCAPlot(data);
        }
        else {
            runScriptPath = app.data.getTmpScriptFileFromResource("PCA.R");
            service = new DataLoadService();
            service.start();
        }
    }
    private class DataLoadService extends TaskHandler.ServiceExt {
        DataProject.ExpLevelPCAData data = null;
        
        @Override
        protected void onRunning() {
            pi.setProgress(-1);
            pi.setVisible(true);
        }
        @Override
        protected void onStopped() {
            pi.setVisible(false);
            pi.setProgress(0);
        }
        @Override
        protected void onFailed() {
            java.lang.Throwable e = getException();
            if(e != null)
                app.logWarning("DataAll Summary failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("DataAll Summary - task aborted.");
            app.ctls.alertWarning("DataAll Summary", "DataAll Summary failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(data != null) {
                showPCAPlot(data);
            }
            else
                app.ctls.alertWarning("Expression Levels PCA Plot", "Unable to generate expression levels PCA plot.");
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Utils.removeFile(Paths.get(project.data.getMatrixPCAFilepath()));
                    String logfilepath = project.data.getSubTabLogFilepath(subTabInfo.subTabId);
                    outputFirstLogLine("Expression Levels PCA thread running...", logfilepath);

                    // setup script arguments
                    List<String> lst = new ArrayList<>();
                    lst.add(app.data.getRscriptFilepath());
                    lst.add(runScriptPath.toString());
                    lst.add(project.data.getResultMatrixFilepath());
                    lst.add(project.data.getInputFactorsFilepath());
                    lst.add(project.data.getMatrixPCAFilepath());
                    appendLogLine("Starting R script...", logfilepath);
                    app.logDebug("Running expression levels PCA. Arguments: " + lst);
                    runScript(taskInfo, lst, "Expression Levels PCA", logfilepath);
                    DataProject.ExpLevelPCAData results = project.data.getExpLevelsPCAResults();
                    if(!results.lstSeries.isEmpty())
                        data = results;
                    
                    // remove script file from temp folder
                    Utils.removeFile(runScriptPath);
                    runScriptPath = null;
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("Expression Levels PCA Plot", task);
            return task;
        }
    }
    public class CustomPieChart extends PieChart {
        public CustomPieChart(ObservableList<PieChart.Data> data){
            super(data);
            setLabelLineLength(20);
            createScrollableLegend();
            setLegendSide(Side.RIGHT);
            setClockwise(true);
        }

        private void createScrollableLegend(){
            Legend legend = (Legend) getLegend();
            if(legend != null){
                legend.setPrefWidth(100);
                ScrollPane scrollPane = new ScrollPane(legend);
                scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
                scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
                scrollPane.maxHeightProperty().bind(heightProperty());
                setLegend(scrollPane);
            }
        }
    }
}
