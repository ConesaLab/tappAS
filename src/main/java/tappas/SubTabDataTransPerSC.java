/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
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
public class SubTabDataTransPerSC extends SubTabBase {
    AnchorPane paneContents;
    BarChart barChart;
    
    public SubTabDataTransPerSC(Project project) {
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
    private void showSubTab() {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        barChart = (BarChart) tabNode.lookup("#barChart");
        
        showDistributionPerSC();
        setupExportMenu();
        setFocusNode(barChart, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export chart image...");
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
            app.export.exportImage("Transcripts per Structural Category", "tappAS_Trans_PerStructCategory.png", (Image)img);
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
            app.export.exportImage("Transcripts per Structural Category", "tappAS_Trans_PerStructCategory_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void showDistributionPerSC() {
        barChart.setTitle("Transcripts per Structural Category");
        barChart.getXAxis().setLabel("Structural Category");
        barChart.getYAxis().setLabel("Transcripts");
        barChart.setBarGap(4.0);
        barChart.setCategoryGap(10.0);
        HashMap<String, Integer> hmAlignCats = project.data.getAlignmentCategoriesTransCount(project.data.getResultsTrans());
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        ArrayList<String> lstOrder = project.data.getAlignmentCategoriesDisplayOrder();
        for(String cat : lstOrder) {
            if(hmAlignCats.containsKey(cat))
                series.getData().add(new XYChart.Data(cat, hmAlignCats.get(cat)));
        }
        barChart.setLegendVisible(false);
        barChart.getXAxis().tickLabelFontProperty().set(Font.font(10));
        barChart.getData().addAll(series);
        app.ctls.setupChartExport(barChart, "Transcripts per Structural Category Chart", "tappAS__Trans_PerStructCategory_Chart.png", null);
        
        Tooltip tt;
        for(int i = 0; i < series.getData().size(); i++) {
            XYChart.Data item = series.getData().get(i);
            tt = new Tooltip("Transcripts in category:  " + item.getYValue().toString());
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        showSubTab();
    }
}
