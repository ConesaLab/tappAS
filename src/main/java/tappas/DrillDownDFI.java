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
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DrillDownDFI extends DlgBase {
    static int id = 0;
    static String exportFilename = "";

    LineChartTimeSeries chartMain;
    Button btnHelpTbl, btnExportChart;
    GridPane grdCharts;
    Pane paneMenu;

    int toolId;
    boolean timeSeries = false;
    boolean timeMultiple = false;
    DataDFI.DFISelectionResults data = null;
    
    public DrillDownDFI(Project project, Window window) {
        super(project, window);
        toolId = ++id;
        timeSeries = project.data.isTimeCourseExpType();
        timeMultiple = project.data.getExperimentType().equals(DataApp.ExperimentType.Time_Course_Multiple);
    }
    public HashMap<String, String> showAndWait(DataDFI.DFISelectionResults data) {
        if(createToolDialog("DrillDownDFI.fxml", "DFI Gene FeatureId Data Visualization",  null)) {
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

            // create chart
            chartMain = new LineChartTimeSeries(new CategoryAxis(), new NumberAxis(), project.data.getGroupsTimeNames().length);
            chartMain.setMultipleGroupsTS(timeMultiple);
            chartMain.setAnimated(false);
            chartMain.addTimeLines(lstTimeLines);
            grdCharts.add(chartMain, 0, 0);
            
            // setup dialog
            setProjectName();
            dialog.setTitle("DFI Gene FeatureId Data Visualization");
            chartMain.setTitle("DFI Gene '" + data.getGene() + "' Feature Id '" + data.getFeatureId() + "' - " + data.getDS() + "\n");
            exportFilename = "tappAS_DFI_"+ data.getGene() +"_FeatureId.png";
            
            // determine number of 
            ArrayList<Integer> lstCounts = new ArrayList<>();
            int[] grps = project.data.getGroupTimes();
            for(int i = 0; i < grps.length; i++) {
                int timecnt = grps[i];
                for(int j = 0; j < timecnt; j++) {
                    lstCounts.add(project.data.getGroupTimeSamples(i, j));
                }
            }
            int totalCols = lstCounts.size();
            Integer[] scnt = new Integer[totalCols];
            scnt = lstCounts.toArray(scnt);

            String[] names;
            if(project.data.isTimeCourseExpType()) {
                int idx = 0;
                names = new String[totalCols];
                for(int i = 0; i < grps.length; i++) {
                    String[] timeNames = project.data.getGroupTimeNames(i);
                    for(int j = 0; j < timeNames.length; j++) {
                        names[idx++] = timeNames[j];
                    }
                }
            }
            else {
                names = project.data.getGroupNames();
            }
            XYChart.Series<String, Number> series1 = new XYChart.Series();
            series1.setName("With Feature Id");
            for(int i = 0; i < totalCols; i++) {
                series1.getData().add(new XYChart.Data(names[i], data.getWithMeanExp(i)));
            }
            XYChart.Series<String, Number> series2 = new XYChart.Series();
            series2.setName("Without Feature Id");
            for(int i = 0; i < totalCols; i++) {
                series2.getData().add(new XYChart.Data(names[i], data.getWithoutMeanExp(i)));
            }
            chartMain.getYAxis().setLabel("Expression Level");
            if(project.data.isSingleTimeSeriesExpType())
                chartMain.getXAxis().setLabel("Favored times (" + data.getFavored() + ")");
            else
                chartMain.getXAxis().setLabel("Conditions (" + data.getFavored() + " favored)");
            chartMain.getData().addAll(series1, series2);
            Tooltip tt;
            for(int i = 0; i < series1.getData().size(); i++) {
                XYChart.Data item = series1.getData().get(i);
                String tooltip = names[i] + " withFeatureID: " + String.format("%.2f", item.getYValue());
                tt = new Tooltip(tooltip);
                tt.setStyle("-fx-font: 13 system;");
                Tooltip.install(item.getNode(), tt);
            }
            for(int i = 0; i < series2.getData().size(); i++) {
                XYChart.Data item = series2.getData().get(i);
                String tooltip = names[i] + " withoutFeatureID: " + String.format("%.2f", item.getYValue());
                tt = new Tooltip(tooltip);
                tt.setStyle("-fx-font: 13 system;");
                Tooltip.install(item.getNode(), tt);
            }
            app.ctls.setupChartExport(chartMain, "DFI Gene FeatureId Data Visualization", exportFilename, null);

            // setup menu buttons
            double yoffset = 3.0;
            btnExportChart = app.ctls.addImgButton(paneMenu, "export.png", "Export chart image...", yoffset, 32, true);
            btnExportChart.setOnAction((event) -> { onButtonExportChart(); });
            yoffset += 36;
            btnHelpTbl = app.ctls.addImgButton(paneMenu, "help.png", "Help", yoffset, 32, true);
            btnHelpTbl.setOnAction((event) -> { DlgHelp dlg = new DlgHelp(); dlg.show(title, "Help_DrillDown_DFI.html", Tappas.getWindow()); });

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
        app.export.exportImage("DFI Gene FeatureId Data Visualization", exportFilename, img);
    }
}
