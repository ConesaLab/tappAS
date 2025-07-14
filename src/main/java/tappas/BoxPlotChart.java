/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BoxPlot chart implementation
 * <p>
 * Implementation of a box plot chart using
 * existing Java BarChart as a base
 * 
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */

public class BoxPlotChart extends AppObject {
    // Annotation symbols color list
    static private final List<String> COLORS_LIST = Arrays.asList(
        "255,69,0",     //"ORANGERED",
        "255,165,0",    //"ORANGE",
        "255,255,0",    //"YELLOW",
        "0,128,0",      //"GREEN",
        "0,191,255",    //"DEEPSKYBLUE",
        "138,43,226",   //"BLUEVIOLET",
        "255,0,255",    //"MAGENTA",
        "0,128,128",    //"TEAL",
        "128,0,128",    //"PURPLE",
        "218,165,32",   //"GOLDENROD",
        "0,255,0"       //"LIME",
    );

    /**
     * Instantiates a BoxPlotChart object - constructor
     */
    public BoxPlotChart() {
        super(null, null);
    }
    
    public void initialize(BarChart barChart, BoxPlotChartData[] boxPlotValues) {
        XYChart.Series series1 = new XYChart.Series();
        for(BoxPlotChartData data : boxPlotValues)
            series1.getData().add(new XYChart.Data(data.name, data.max));
        
        Tooltip tt;
        for(int i = 0; i < series1.getData().size(); i++) {
            XYChart.Data item = (XYChart.Data) series1.getData().get(i);
            BoxPlotChartData data = boxPlotValues[i];
            item.setNode(getBoxPlotNode(data));
            if(data.showOutliers) {
                String tooltip = data.name + "\n\nMax: " + data.max + "\nQ3: " + data.q3 + "\nMedian: " + data.median + "\nQ1: " + data.q1 + "\nMin: " + data.min;
                tooltip += "\n\nUpperFence: " + data.bpd.upperFence;
                tooltip += "\nUpperWhisker: " + data.bpd.upperWhisker + "\nUpperOutliers: " + (int)data.bpd.upperOutliersCnt;
                tooltip += "\nLowerOutliers: " + (int)data.bpd.lowerOutliersCnt + "\nLowerFence: " + data.bpd.lowerFence;
                tooltip += "\nLowerWhisker: " + data.bpd.lowerWhisker;
                tt = new Tooltip(tooltip);
            }
            else
                tt = new Tooltip(data.name + "\n\nMax: " + data.max + "\nQ3: " + data.q3 + "\nMedian: " + data.median + "\nQ1: " + data.q1 + "\nMin: " + data.min);
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
        barChart.setLegendVisible(false);
        barChart.getData().add(series1);
        
    }
    private Pane getBoxPlotNode(BoxPlotChartData parts) {
        Pane p1 = new Pane();
        p1.setStyle("-fx-background-color: transparent;");
        p1.setUserData(parts);
        p1.widthProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            BoxPlotChartData parts1 = (BoxPlotChartData) p1.getUserData();
            double ppb = (double)p1.getHeight() / parts1.max;
            parts1.rect.setWidth((double)newValue-1);
            parts1.tophLine.setWidth((double)newValue/4);
            parts1.bottomhLine.setWidth((double)newValue/4);
            parts1.medianLine.setWidth((double)newValue-3);
            parts1.topvLine.setWidth(1);
            parts1.bottomvLine.setWidth(1);
            double offset = 4;
            double midx = p1.getWidth()/2 + offset - parts1.tophLine.getWidth() / 2;
            if (parts1.showOutliers) {
                parts1.tophLine.relocate(midx, (parts1.max - parts1.upperWhisker) * ppb);
                parts1.bottomhLine.relocate(midx, (parts1.max - parts1.lowerWhisker) * ppb - 1);
                parts1.topvLine.relocate(p1.getWidth()/2 + offset, (parts1.max - parts1.upperWhisker) * ppb);
                parts1.bottomvLine.relocate(p1.getWidth()/2 + offset, (parts1.max - parts1.q1) * ppb + 1);
                parts1.outliers.forEach((outlier) -> {
                    outlier.symbol.relocate(p1.getWidth()/2 + offset - 2.5, (parts1.max - outlier.value) * ppb + 1);
                });
            } else {
                parts1.tophLine.relocate(midx, 0);
                parts1.bottomhLine.relocate(midx, (parts1.max - parts1.min) * ppb - 1);
                parts1.topvLine.relocate(p1.getWidth()/2 + offset, 0);
                parts1.bottomvLine.relocate(p1.getWidth()/2 + offset, (parts1.max - parts1.q1) * ppb - 2.5);
            }
        });
        p1.heightProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            BoxPlotChartData parts1 = (BoxPlotChartData) p1.getUserData();
            double ppb = (double)newValue / parts1.max;
            parts1.rect.setHeight((parts1.q3 - parts1.q1) * ppb + 1);
            parts1.rect.relocate(0.5, Math.floor((parts1.max - parts1.q3) * ppb) + 0.5);
            parts1.medianLine.setStroke(Color.RED);
            parts1.medianLine.setHeight(0.5);
            parts1.medianLine.relocate(1.5, Math.floor((parts1.max - parts1.median) * ppb) + 0.5);
            double offset = 4;
            double midx = p1.getWidth()/2 + offset - parts1.tophLine.getWidth() / 2;
            parts1.tophLine.setHeight(1);
            parts1.bottomhLine.setHeight(1);
            if (parts1.showOutliers) {
                parts1.tophLine.relocate(midx, (parts1.max - parts1.upperWhisker) * ppb);
                parts1.bottomhLine.relocate(midx, (parts1.max - parts1.lowerWhisker) * ppb - 1);
                parts1.topvLine.setHeight((parts1.upperWhisker - parts1.q3) * ppb);
                parts1.topvLine.relocate(p1.getWidth()/2 + offset, (parts1.max - parts1.upperWhisker) * ppb);
                parts1.bottomvLine.setHeight((parts1.q1 - parts1.lowerWhisker) * ppb - 1);
                parts1.bottomvLine.relocate(p1.getWidth()/2 + offset, (parts1.max - parts1.q1) * ppb + 1);
                parts1.outliers.forEach((outlier) -> {
                    outlier.symbol.relocate(p1.getWidth()/2 + offset - 2.5, (parts1.max - outlier.value) * ppb - 2.5);
                });
            } else {
                parts1.tophLine.relocate(midx, 0);
                parts1.bottomhLine.relocate(midx, (parts1.max - parts1.min) * ppb - 1);
                parts1.topvLine.setHeight((parts1.max - parts1.q3) * ppb);
                parts1.topvLine.relocate(p1.getWidth()/2 + offset, 0);
                parts1.bottomvLine.setHeight((parts1.q1 - parts1.min) * ppb - 1);
                parts1.bottomvLine.relocate(p1.getWidth()/2 + offset, (parts1.max - parts1.q1) * ppb + 1);
            }
        });
        p1.getChildren().addAll(parts.rect, parts.tophLine, parts.bottomhLine, parts.topvLine, parts.bottomvLine, parts.medianLine);
        if(parts.showOutliers) {
            parts.outliers.forEach((outlier) -> {
                p1.getChildren().add(outlier.symbol);
            });
        }
        return p1;
    }
    
    //
    // Data Classes
    //
    public static class Outlier {
        double value;
        Polygon symbol;
        public Outlier(double y) {
            this.value = y;
            this.symbol = new Polygon();
            this.symbol.getPoints().addAll(new Double[] {2.5, 5.0, 5.0, 2.5, 2.5, 0.0, 0.0, 2.5});
            this.symbol.setStyle("-fx-fill: yellow; -fx-stroke: black; -fx-stroke-width: 0.5;");
        }
    }
    public static class BoxPlotChartData {
        String name;
        Stats.BoxPlotData bpd;
        boolean showOutliers;
        double min, max, lowerWhisker, upperWhisker, q1, q3, median;
        Rectangle rect, tophLine, bottomhLine, topvLine, bottomvLine, medianLine;
        ArrayList<Outlier> outliers = new ArrayList<>();
        public BoxPlotChartData(String name, Stats.BoxPlotData bpd, int colorIdx, boolean showOutliers) {
            this.name = name;
            this.showOutliers = showOutliers;
            
            // save stats
            this.bpd = bpd;
            this.min = ((double)Math.round(bpd.min*100)/100.0);
            this.max = ((double)Math.round(bpd.max*100)/100.0);
            this.lowerWhisker = this.min;
            this.upperWhisker = this.max;
            if(showOutliers && bpd.lowerOutliersCnt > 0)
                this.lowerWhisker = ((double)Math.round(bpd.lowerFence*100)/100.0);
            if(showOutliers && bpd.upperOutliersCnt > 0)
                this.upperWhisker = ((double)Math.round(bpd.upperFence*100)/100.0);
            this.q1 = ((double)Math.round(bpd.q1*100)/100.0);
            this.q3 = ((double)Math.round(bpd.q3*100)/100.0);
            this.median = ((double)Math.round(bpd.median*100)/100.0);
            
            // create internal parts
            rect = new Rectangle();
            if(colorIdx >= COLORS_LIST.size())
                colorIdx = 0;
            rect.setStyle("-fx-fill: rgba(" + COLORS_LIST.get(colorIdx) + ",0.35); -fx-stroke: rgba(0,0,0,1); -fx-stroke-width: 0.5;");
            tophLine = new Rectangle();
            bottomhLine = new Rectangle();
            topvLine = new Rectangle();
            bottomvLine = new Rectangle();
            medianLine = new Rectangle();
            
            for(int i = 0; i < bpd.vals.length; i++) {
                if(bpd.vals[i] > bpd.upperWhisker)
                    outliers.add(new Outlier(((double)Math.round(bpd.vals[i]*100)/100.0)));
                else if(bpd.vals[i] < bpd.lowerWhisker)
                    outliers.add(new Outlier(((double)Math.round(bpd.vals[i]*100)/100.0)));
            }
        }
    }    
}
