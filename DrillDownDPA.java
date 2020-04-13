/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.SnapshotParameters;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Pedro Salguero - psalguero@cipf.es
 */
public class DrillDownDPA extends DlgBase {
    static int id = 0;
    static String exportFilename = "";

    LineChartTimeSeries chartMain, chartCondition;
    Button btnHelpTbl, btnExportChart;
    GridPane grdCharts;
    Pane paneMenu;

    int toolId;
    boolean timeSeries = false;
    boolean timeMultiple = false;
    DataDPA.DPASelectionResults data = null;
    
    public DrillDownDPA(Project project, Window window) {
        super(project, window);
        toolId = ++id;
        timeSeries = project.data.isTimeCourseExpType();
        timeMultiple = project.data.getExperimentType().equals(DataApp.ExperimentType.Time_Course_Multiple);
    }
    public HashMap<String, String> showAndWait(DataDPA.DPASelectionResults data) {
        if(createToolDialog("DrillDownDPA.fxml", "DPA Gene FeatureId Data Visualization",  null)) {
            this.data = data;

            // get control objects
            grdCharts = (GridPane) scene.lookup("#grdCharts");
            paneMenu = (Pane) scene.lookup("#paneMenu");

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

            // create chart expression
            chartMain = new LineChartTimeSeries(new CategoryAxis(), new NumberAxis(), project.data.getGroupsTimeNames().length);
            chartMain.setMultipleGroupsTS(timeMultiple);
            chartMain.setAnimated(false);
            chartMain.addTimeLines(lstTimeLines);
            grdCharts.add(chartMain, 0, 0);
            
            // create chart Distal Condition
            chartCondition = new LineChartTimeSeries(new CategoryAxis(), new NumberAxis(), project.data.getGroupsTimeNames().length);
            chartCondition.setMultipleGroupsTS(timeMultiple);
            chartCondition.setAnimated(false);
            chartCondition.addTimeLines(lstTimeLines);
            grdCharts.add(chartCondition, 1, 0);
            
            // setup dialog
            setProjectName();
            String[] names = project.data.getExpTypeGroupNames();
            dialog.setTitle("DPA Gene FeatureId Data Visualization");
            chartMain.setTitle("Gene '" + data.getGene() + "' - Isoform Expressions" + "\n");
            chartCondition.setTitle("Gene '" + data.getGene() + "' - Distal PolyA Usage" + "\n");

            exportFilename = "tappAS_DPA_"+data.getGene()+"_FeatureId.png";
            // determine number of 
            ArrayList<String> lstVar = new ArrayList<>();
            lstVar.add("Distal");lstVar.add("Proximal");

            int[] grps = project.data.getGroupTimes();
            //grps X groups, grps[0] how many times in each group
            int ngrps = grps[0]*grps.length;
            double[] expValues = new double[ngrps];
            
            // DISTAL AND PROXIMAL
            int num_groups = grps.length;
            // if single serie add +1 to check distal and proximal
            if(num_groups == 1)
                num_groups += 1;
            for(int j = 0; j < num_groups; j++) {
                XYChart.Series<String, Number> series = new XYChart.Series();
                series.setName(lstVar.get(j));
                int idx = 0;
                
                for(String name : names){ //by each group name - add point
                    if(j==0)
                        expValues[idx]=data.getLongMeanExp(idx);
                    else
                        expValues[idx]=data.getShortMeanExp(idx);
                    series.getData().add(new XYChart.Data(name, expValues[idx++]));
                    
                }
                chartMain.getData().add(series);
                
                Tooltip tt;
                for(int i = 0; i < series.getData().size(); i++) {
                    XYChart.Data item = series.getData().get(i);
                    String tooltip;
                    if(j==0)
                        tooltip = names[i] + " Distal: " + String.format("%.2f", item.getYValue());
                    else
                        tooltip = names[i] + " Proximal: " + String.format("%.2f", item.getYValue());
                    tt = new Tooltip(tooltip);
                    tt.setStyle("-fx-font: 13 system;");
                    Tooltip.install(item.getNode(), tt);
                }
            }
            
            // Usage
            XYChart.Series<String, Number> series3 = new XYChart.Series();
            series3.setName("Distal PolyA Usage");
            for(int i = 0; i < ngrps; i++) {
                series3.getData().add(new XYChart.Data(names[i], data.getLongerConditionValues(i)));
            }
            
            chartMain.getYAxis().setLabel("Expression Level");
            /*if(project.data.isSingleTimeSeriesExpType())
                chartMain.getXAxis().setLabel("Favored times (" + data.getFavored() + ")");
            else
                chartMain.getXAxis().setLabel("Conditions (" + data.getFavored() + " favored)");
            */
            chartCondition.getYAxis().setLabel("Distal PolyA Usage");
            chartCondition.getData().addAll(series3);
            Tooltip tt;
            for(int i = 0; i < series3.getData().size(); i++) {
                XYChart.Data item = series3.getData().get(i);
                String tooltip = names[i] + " Distal PolyA Usage: " + String.format("%.2f", item.getYValue());
                tt = new Tooltip(tooltip);
                tt.setStyle("-fx-font: 13 system;");
                Tooltip.install(item.getNode(), tt);
            }
            
            app.ctls.setupChartExport(chartMain, "DPA Gene Data Visualization", exportFilename, null);

            // setup menu buttons
            double yoffset = 3.0;
            btnExportChart = app.ctls.addImgButton(paneMenu, "export.png", "Export chart image...", yoffset, 32, true);
            btnExportChart.setOnAction((event) -> { onButtonExportChart(); });
            yoffset += 36;
            btnHelpTbl = app.ctls.addImgButton(paneMenu, "help.png", "Help", yoffset, 32, true);
            btnHelpTbl.setOnAction((event) -> { DlgHelp dlg = new DlgHelp(); dlg.show(title, "Help_DrillDown_DPA.html", Tappas.getWindow()); });

            // process dialog - modeless
            dialog.showAndWait();
        }
        return null;
    }
    @Override
    protected void onButtonExportChart() {
        SnapshotParameters sP = new SnapshotParameters();
        double x = chartMain.getWidth();
        double ratio = 3840/x;
        chartMain.setScaleX(ratio);
        chartMain.setScaleY(ratio);
        WritableImage img = chartMain.snapshot(sP, null);
        double newX = chartMain.getWidth();
        ratio = x/newX;
        chartMain.setScaleX(ratio);
        chartMain.setScaleY(ratio);
        
        //WritableImage img = chartMain.snapshot(new SnapshotParameters(), null);
        app.export.exportImage("DPA Gene FeatureId Data Visualization", exportFilename, img);
    }
}
