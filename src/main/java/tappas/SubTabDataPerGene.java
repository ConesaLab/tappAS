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

import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDataPerGene extends SubTabBase {
    AnchorPane paneContents;
    BarChart barChart;
    
    public SubTabDataPerGene(Project project) {
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
    private void showSubTab(DataProject.DistributionData dataTrans, DataProject.DistributionData dataProtein) {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        barChart = (BarChart) tabNode.lookup("#barChart");
        showDistributionPerGene(dataTrans, dataProtein);
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
            app.export.exportImage("Distribution per Gene Chart", "tappAS_Distribution_PerGene.png", (Image)img);
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
            app.export.exportImage("Distribution per Gene Panel", "tappAS_Distribution_PerGene_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void showDistributionPerGene(DataProject.DistributionData dataTrans, DataProject.DistributionData dataProtein) {
        barChart.setTitle("Transcripts and Proteins Distribution per Gene");
        barChart.getYAxis().setLabel("Percentage of Genes");
        XYChart.Series<String, Number> seriesT = new XYChart.Series<>();
        seriesT.setName("Transcripts");
        double val, pct;
        if(dataTrans != null) {
            dataTrans.series.getData().add(0, new XYChart.Data<>("0", 0));
            dataTrans.series.getData().get(0).setExtraValue(0);
            dataTrans.series.setName("Transcripts");
            for(int i = 1; i < dataTrans.series.getData().size(); i++) {
                val = dataTrans.series.getData().get(i).getYValue().doubleValue();
                pct = val * 100 / dataTrans.genes;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                dataTrans.series.getData().get(i).setYValue(pct);
                dataTrans.series.getData().get(i).setExtraValue((int)val);
            }
            seriesT = dataTrans.series;
        }
        barChart.getData().add(seriesT);

        Tooltip tt;
        if(dataTrans != null) {
            for(int i = 0; i < dataTrans.series.getData().size(); i++) {
                XYChart.Data item = dataTrans.series.getData().get(i);
                tt = new Tooltip(i + " Transcript(s) per gene:  " + item.getExtraValue().toString() + " genes, " + item.getYValue().toString() + "% ");
                tt.setStyle("-fx-font: 13 system;");
                Tooltip.install(item.getNode(), tt);
            }
        }
        XYChart.Series<String, Number> seriesP = new XYChart.Series<>();
        if(dataProtein != null) {
            dataProtein.series.setName("Proteins");
            for(int i = 0; i < dataProtein.series.getData().size(); i++) {
                val = dataProtein.series.getData().get(i).getYValue().doubleValue();
                pct = val * 100 / dataProtein.genes;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                dataProtein.series.getData().get(i).setYValue(pct);
                dataProtein.series.getData().get(i).setExtraValue((int)val);
            }
            seriesP = dataProtein.series;
        }
        barChart.setCategoryGap(5);
        barChart.getXAxis().setLabel("Number of Transcripts and Proteins per Gene");
        barChart.getData().addAll(seriesP);
        app.ctls.setupChartExport(barChart, "Distribution per Gene Chart", "tappAS_Distribution_PerGeneChart.png", null);

        if(dataProtein != null) {
            for(int i = 0; i < dataProtein.series.getData().size(); i++) {
                XYChart.Data item = dataProtein.series.getData().get(i);
                tt = new Tooltip(i + " Protein(s) per gene:  " + item.getExtraValue().toString() + " genes, " + item.getYValue().toString() + "% ");
                tt.setStyle("-fx-font: 13 system;");
                Tooltip.install(item.getNode(), tt);
            }
        }
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        DataProject.DistributionData dataTrans = project.data.getExpressedTransDistribution(project.data.getResultsTrans());
        DataProject.DistributionData dataProtein = project.data.getGeneProteinDistribution(project.data.getResultsTrans());
        showSubTab(dataTrans, dataProtein);
    }
}
