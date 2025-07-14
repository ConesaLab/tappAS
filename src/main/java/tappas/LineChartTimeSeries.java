/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.shape.Line;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import java.util.ArrayList;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class LineChartTimeSeries<X, Y> extends LineChart {
    int times;
    boolean multipleGroupsTS = false;
    boolean dpa = false;
    ArrayList<LineInfo> lstLines = new ArrayList();
    public LineChartTimeSeries(Axis<X> xAxis, Axis<Y> yAxis, int length_times) {
        super(xAxis, yAxis);
        times = length_times;
    }
    public void addTimeLines(ArrayList<Integer> lstPoints) {
        for(int pt : lstPoints) {
            Line line = new Line();
            lstLines.add(new LineInfo(pt, line));
            getPlotChildren().add(line);
        }
    }
    public void setMultipleGroupsTS(boolean flg) {
        multipleGroupsTS = flg;
    }
    @Override
    protected void layoutPlotChildren() {
        super.layoutPlotChildren();

        // only need to do for multiple groups time series
        // Credit for basic idea of erasing a line to break up the groups:
        // https://stackoverflow.com/questions/38591452/is-there-a-way-to-disconnect-2-dots-in-series-in-javafx-linechart
        // https://stackoverflow.com/questions/28952133/how-to-add-two-vertical-lines-with-javafx-linechart
        /*if(multipleGroupsTS) {
            ObservableList<XYChart.Series<String, Number>> lstSeries = getData();
            boolean addLines = true;
            int aux = 1;
            for(XYChart.Series<String, Number> series : lstSeries) {
                if(aux>1 && aux%(times+1)==0){
                    Path p = (Path) series.getNode();
                    int offset = 1;
                    for(LineInfo li : lstLines) {
                        XYChart.Data<String, Number> pt = series.getData().get(li.pt + 1);
                        p.getElements().add((li.pt + offset), new MoveTo(getXAxis().getDisplayPosition(pt.getXValue()), getYAxis().getDisplayPosition(pt.getYValue())));
                        if(addLines) {
                            XYChart.Data<String, Number> pt1 = series.getData().get(li.pt);
                            double pos = (getXAxis().getDisplayPosition(pt1.getXValue()) - getXAxis().getDisplayPosition(pt.getXValue())) / 2.0;
                            li.line.setStartX(getXAxis().getDisplayPosition(pt1.getXValue()) + pos);
                            li.line.setEndX(li.line.getStartX());
                            li.line.setStartY(0d);
                            li.line.setEndY(getBoundsInLocal().getHeight());
                            li.line.toFront();
                        }
                        offset++;
                    }
                    addLines = false;
                }
                aux++;
            }
        }*/
        
        if(multipleGroupsTS) {
            ObservableList<XYChart.Series<String, Number>> lstSeries = getData();
            boolean addLines = true;
            
            for(XYChart.Series<String, Number> series : lstSeries) {
                int aux = 1;
                for(int k=1; k<= series.getData().size()+1;k++){
                
                    if(aux>1 && aux%(times+1)==0){
                        Path p = (Path) series.getNode();
                        int offset = 1;
                        for(LineInfo li : lstLines) {
                            XYChart.Data<String, Number> pt = series.getData().get(li.pt + 1);
                            p.getElements().add((li.pt + offset), new MoveTo(getXAxis().getDisplayPosition(pt.getXValue()), getYAxis().getDisplayPosition(pt.getYValue())));
                            if(addLines) {
                                XYChart.Data<String, Number> pt1 = series.getData().get(li.pt);
                                double pos = (getXAxis().getDisplayPosition(pt1.getXValue()) - getXAxis().getDisplayPosition(pt.getXValue())) / 2.0;
                                li.line.setStartX(getXAxis().getDisplayPosition(pt1.getXValue()) + pos);
                                li.line.setEndX(li.line.getStartX());
                                li.line.setStartY(0d);
                                li.line.setEndY(getBoundsInLocal().getHeight());
                                li.line.toFront();
                            }
                            offset++;
                        }
                        addLines = false;
                    }
                    aux++;
                }
            }
        }
        
    }
    public class LineInfo {
        int pt;
        Line line;
        public LineInfo(int pt, Line line) {
            this.pt = pt;
            this.line = line;
        }
    }
    public Node getLegendTS(){
        return this.getLegend();
    }
    
    public ObjectProperty<Node> getLegendProperty(){
        return this.legendProperty();
    }
}

