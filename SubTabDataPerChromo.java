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
import java.util.List;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDataPerChromo extends SubTabBase {
    AnchorPane paneContents;
    BarChart barChart;
    
    public SubTabDataPerChromo(Project project) {
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
    private void showSubTab(HashMap<String, DataAnnotation.ChromoDistributionData> data) {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        barChart = (BarChart) tabNode.lookup("#barChart");
        showDistributionPerChromo(data);
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
            app.export.exportImage("Transcripts, Proteins, and Genes per Chromosome Chart", "tappAS_PerChromosomeChart.png", (Image)img);
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
            app.export.exportImage("Transcripts, Proteins, and Genes per Chromosome Panel", "tappAS_PerChromosome_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void showDistributionPerChromo(HashMap<String, DataAnnotation.ChromoDistributionData> hmData) {
        barChart.setTitle("Transcripts, Proteins, and Genes per Chromosome");
        barChart.getYAxis().setLabel("Count");
        XYChart.Series<String, Number> seriesT = new XYChart.Series<>();
        seriesT.setName("Transcripts");
        XYChart.Series<String, Number> seriesP = new XYChart.Series<>();
        seriesP.setName("Proteins");
        XYChart.Series<String, Number> seriesG = new XYChart.Series<>();
        seriesG.setName("Genes");
        Chromosome chromosome = new Chromosome(project.data.getGenus(), project.data.getSpecies());
        List<String> lstNames = chromosome.getChromoNames();
        for(String chr : lstNames) {
            // allow "1" or "chr1" or "Chr1" for chromosome names
            if(!hmData.containsKey(chr)) {
                if(!hmData.containsKey("chr" + chr))
                    chr = "Chr" + chr;
                else
                    chr = "chr" + chr;
            }
            if(hmData.containsKey(chr)) {
                seriesT.getData().add(new XYChart.Data(chr, hmData.get(chr).trans));
                seriesP.getData().add(new XYChart.Data(chr, hmData.get(chr).prots));
                seriesG.getData().add(new XYChart.Data(chr, hmData.get(chr).genes));
            }
            else {
                seriesT.getData().add(new XYChart.Data(chr, 0));
                seriesP.getData().add(new XYChart.Data(chr, 0));
                seriesG.getData().add(new XYChart.Data(chr, 0));
            }
        }
        barChart.setCategoryGap(5);
        barChart.getXAxis().setLabel("Chromosome");
        barChart.getData().add(seriesT);
        barChart.getData().add(seriesP);
        barChart.getData().add(seriesG);
        app.ctls.setupChartExport(barChart, "Transcripts, Proteins, and Genes per Chromosome Chart", "tappAS_PerChromosomeChart.png", null);

        Tooltip tt;
        for(int i = 0; i < seriesT.getData().size(); i++) {
            XYChart.Data item = seriesT.getData().get(i);
            tt = new Tooltip(item.getYValue().toString() + " transcripts");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
        for(int i = 0; i < seriesP.getData().size(); i++) {
            XYChart.Data item = seriesP.getData().get(i);
            tt = new Tooltip(item.getYValue().toString() + " proteins");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
        for(int i = 0; i < seriesG.getData().size(); i++) {
            XYChart.Data item = seriesG.getData().get(i);
            tt = new Tooltip(item.getYValue().toString() + " genes");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        HashMap<String, DataAnnotation.ChromoDistributionData> data = project.data.getGeneTransChromoDistribution(project.data.getResultsTrans());
        showSubTab(data);
    }
}
