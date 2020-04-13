/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.SnapshotParameters;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabFDASummaryCombinedResults extends SubTabBase {
    AnchorPane paneContents;
    ScrollPane spFDA;
    GridPane grMain;
    ProgressIndicator pi;
    Pane paneImgChart;
    
    DataFDA fdaData;
    PlotsFDACombinedResults barplot;
    DlgFDAnalysisCombinedResults.Params fdaCombinedParams;
    String analysis_genomic, analysis_presence;
    
    public SubTabFDASummaryCombinedResults(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
             barplot = new PlotsFDACombinedResults(project);
            // must have FDA results
            fdaData = new DataFDA(project);
            result = fdaData.hasTwoFDAIdData();
            if(result){
                fdaCombinedParams = DlgFDAnalysisCombinedResults.Params.load(fdaData.getFDAParamsCombinedResultsFilepath());
            }
                
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    
    //
    // Internal Functions
    //
    private void showSubTab(DataAnnotation.DiversityResults fdaSummaryResults) {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        grMain = (GridPane) tabNode.getParent().lookup("#grdMain");
        spFDA = (ScrollPane) tabNode.getParent().lookup("#spFDA");
        paneImgChart = (Pane) spFDA.getContent().lookup("#paneImgChart");
        pi = (ProgressIndicator) spFDA.getContent().lookup("#piFDAplot");
        
        barplot.showFDACombinedResultsPlots(subTabInfo, tabNode, true, fdaCombinedParams.analysis_genomic, fdaCombinedParams.analysis_presence);

        setupExportMenu();
    }
    
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export subtab contents image...");
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
            app.export.exportImage("Functional Annotation Features Diversity ", "tappAS_FDA_CombinedResults_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        DataAnnotation.DiversityResults fdaSummaryResults = fdaData.getFDAResultsSummaryData(analysisId);
        showSubTab(fdaSummaryResults);
    }
}
