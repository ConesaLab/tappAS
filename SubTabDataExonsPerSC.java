/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDataExonsPerSC extends SubTabBase {
    AnchorPane paneContents;
    BarChart barChart;
    
    public SubTabDataExonsPerSC(Project project) {
        super(project);
    }

    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        return result;
    }

    //
    // Internal Functions
    //
    private void showSubTab(HashMap<String, ArrayList<Integer>> hmAlignCats) {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        barChart = (BarChart) tabNode.lookup("#barChart");
        
        showDistributionPerSC(hmAlignCats);
        setupExportMenu();
        setFocusNode(barChart, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export distribution chart image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = barChart.getWidth();
            double ratio = 3840/x;
            barChart.setScaleX(ratio);
            barChart.setScaleY(ratio);
            WritableImage img = barChart.snapshot(sP, null);
            double newX = barChart.getWidth();
            ratio = x/newX;
            barChart.setScaleX(ratio);
            barChart.setScaleY(ratio);
            
            //WritableImage img = barChart.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Transcript Exons Distribution per Structural Category", "tappAS_TransExons_Distribution_PerStructCategory.png", (Image)img);
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
            app.export.exportImage("Transcript Exons Distribution per Structural Category", "tappAS_TransExons_Distribution_PerStructCategory_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void showDistributionPerSC(HashMap<String, ArrayList<Integer>> hmAlignCats) {
        barChart.setTitle("Transcript Exons Distribution per Structural Category");
        barChart.getXAxis().setLabel("Structural Category");
        barChart.getYAxis().setLabel("Exons");
        barChart.setBarGap(4.0);
        barChart.setCategoryGap(10.0);
        BoxPlotChart boxPlot = new BoxPlotChart();
        BoxPlotChart.BoxPlotChartData[] plots = new BoxPlotChart.BoxPlotChartData[hmAlignCats.size()];
        int colorIdx = 0;
        ArrayList<String> lstOrder = project.data.getAlignmentCategoriesDisplayOrder();
        for(String cat : lstOrder) {
            if(hmAlignCats.containsKey(cat)) {
                ArrayList<Integer> lst = hmAlignCats.get(cat);
                double[] vals = new double[lst.size()];
                int idx = 0;
                for(int len : lst)
                    vals[idx++] = len;
                plots[colorIdx] = new BoxPlotChart.BoxPlotChartData(cat, new Stats.BoxPlotData(vals), colorIdx, true);
                colorIdx++;
            }
        }
        boxPlot.initialize(barChart, plots);
        app.ctls.setupChartExport(barChart, "Transcripts Exons Structural Distribution Chart", "tappAS__TransExonsStructDistributionChart.png", null);
        barChart.getXAxis().tickLabelFontProperty().set(Font.font(10));
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        HashMap<String, ArrayList<Integer>> data = project.data.getAlignmentCategoriesTransExons(project.data.getExpressedTrans(project.data.getResultsTrans()));
        showSubTab(data);
    }
}
