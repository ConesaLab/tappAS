/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import tappas.DataApp.DataType;
import tappas.TabProjectDataViz.SummaryDataCount;
import tappas.TabProjectDataViz.SummaryDataPct;

import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabExpMatrixSummary extends SubTabBase {
    AnchorPane paneContents;
    PieChart pieTranscripts;
    TableView tblTranscripts, tblConditions;
    
    ContextMenu cmTranscripts, cmParameters, cmConditions;
    
    public SubTabExpMatrixSummary(Project project) {
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
    private void showSubTab(ObservableList<SummaryDataCount> conditionsData, ObservableList<SummaryDataPct> tblData, ObservableList<PieChart.Data> pieChartData) {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        pieTranscripts = (PieChart) tabNode.lookup("#pieTranscripts");
        tblConditions = (TableView) tabNode.lookup("#tblConditions");
        tblTranscripts = (TableView) tabNode.lookup("#tblTranscripts");

        // populate conditions table
        ObservableList<TableColumn> cols = tblConditions.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Field"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Count"));
        setTotalRowSummaryDataCount(tblConditions);
        tblConditions.setItems(conditionsData);
        if(!conditionsData.isEmpty())
            tblConditions.getSelectionModel().select(0);
        tblConditions.setOnMouseClicked((event) -> { cmConditions = processTableRightClick(event, cmConditions, tblConditions, "tappAS_ExpMatrixConditionsSummary.tsv"); });

        // populate filtered transcripts table
        cols = tblTranscripts.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("Field"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("Count"));
        cols.get(2).setCellValueFactory(new PropertyValueFactory<>("Pct"));
        setTotalRowSummaryDataPct(tblTranscripts);
        tblTranscripts.setItems(tblData);
        if(!tblData.isEmpty())
            tblTranscripts.getSelectionModel().select(0);
        tblTranscripts.setOnMouseClicked((event) -> { cmTranscripts = processTableRightClick(event, cmTranscripts, tblTranscripts, "tappAS_ExpMatrixFilteringSummary.tsv"); });
  
        showTranscriptsChart(pieChartData);
        
        // manage focus on all tables
        tblConditions.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue)
                onSelectFocusNode = tblConditions;
        });
        tblTranscripts.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue)
                onSelectFocusNode = tblTranscripts;
        });
        
        setupExportMenu();
        setFocusNode(tblConditions, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export conditions table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblConditions, true, "tappAS_ExpMatrix_Conditions.tsv"); });
        cm.getItems().add(item);
        item = new MenuItem("Export filtering table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tblTranscripts, true, "tappAS_ExpMatrix_Filtering.tsv"); });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Export filtering chart image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = pieTranscripts.getWidth();
            double ratio = 3840/x;
            pieTranscripts.setScaleX(ratio);
            pieTranscripts.setScaleY(ratio);
            WritableImage img = pieTranscripts.snapshot(sP, null);
            double newX = pieTranscripts.getWidth();
            ratio = x/newX;
            pieTranscripts.setScaleX(ratio);
            pieTranscripts.setScaleY(ratio);
            
            //WritableImage img = pieTranscripts.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Expression Matrix Filtering Chart", "tappAS_ExpMatrix_Filtering.png", (Image)img);
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
            app.export.exportImage("Expression Matrix Summary Panel", "tappAS_ExpMatrix_Summary_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void showTranscriptsChart(ObservableList<PieChart.Data> pieChartData) {
        pieTranscripts.setData(pieChartData);
        app.ctls.setupChartExport(pieTranscripts, "Expression Matrix Filtering", "tappAS_ExpMatrix_Filtering.png", null);
        Tooltip tt;
        for (PieChart.Data pcdata : pieChartData) {
            // add count?
            tt = new Tooltip("  " + pcdata.getName() + "\n  Percent: " + String.valueOf(pcdata.getPieValue()) + "%");
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(pcdata.getNode(), tt);
        }
    }

    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        service = new DataLoadService();
        service.initialize();
        service.start();
    }
    private class DataLoadService extends TaskHandler.ServiceExt {
        ObservableList<SummaryDataCount> conditionsData = null;
        ObservableList<SummaryDataPct> filterData = null;
        ObservableList<PieChart.Data> pieChartData = null;
        
        // do all service FX thread initialization
        @Override
        public void initialize() {
            filterData = null;
        }
        @Override
        protected void onRunning() {
            showProgress();
        }
        @Override
        protected void onStopped() {
            hideProgress();
        }
        @Override
        protected void onFailed() {
            java.lang.Throwable e = getException();
            if(e != null)
                app.logWarning("ExpMatrix Summary failed - task aborted. Exception: " + e.getMessage());
            else
                app.logWarning("ExpMatrix Summary - task aborted.");
            app.ctls.alertWarning("Experimental Matrix Summary", "ExpMatrix failed - task aborted");
        }
        @Override
        protected void onSucceeded() {
            if(conditionsData != null && filterData != null && pieChartData != null) {
                // set page data ready flag and show subtab
                pageDataReady = true;
                showSubTab(conditionsData, filterData, pieChartData);
            }
            else {
                app.ctls.alertWarning("Expression Matrix", "Unable to retrieve expression matrix data");
            }
        }
        @Override
        protected Task<Void> createTask() {
            Task task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    ObservableList<Analysis.DataSelectionResults> data =  project.data.getOriginalExpMatrixSelectionResults();
                    if(data != null) {
                        FXCollections.sort(data);
                        conditionsData = FXCollections.observableArrayList();
                        filterData = FXCollections.observableArrayList();
                        pieChartData =  FXCollections.observableArrayList();
                        
                        int total = data.size();
                        if(total > 0) {
                            DataInputMatrix.ExpMatrixData emdata = project.data.getRawExpressionData(DataType.TRANS, new HashMap<>());
                            int nsamples = 0;
                            for(DataInputMatrix.ExpMatrixCondition emc : emdata.conditions) {
                                nsamples += emc.nsamples;
                                conditionsData.add(new SummaryDataCount(emc.name, emc.nsamples));
                            }
                            conditionsData.add(new SummaryDataCount("Total", nsamples));
                            HashMap<String, Object> hmProject = project.data.getResultsTrans();
                            int level = 0;
                            int annot = 0;
                            int filter = 0;
                            HashMap<String, Object> hmInput = new HashMap<>();
                            for(DataInputMatrix.ExpMatrixArray emd : emdata.data)
                                hmInput.put(emd.getTranscript(), null);
                            HashMap<String, Object> hmNA = Utils.loadListFromFile(project.data.getInputMatrixNATransFilepath());
                            HashMap<String, Object> hmFiltered = Utils.loadListFromFile(project.data.getInputMatrixFilteredTransFilepath());
                            for(Analysis.DataSelectionResults dsr : data) {
                                if(hmNA.containsKey(dsr.getId()))
                                    annot++;
                                else if(hmFiltered.containsKey(dsr.getId()))
                                    filter++;
                                else if(!hmInput.containsKey(dsr.getId()))
                                    level++;
                            }
                            int notFiltered = total - annot - level - filter;
                            double pct;
                            if(annot > 0) {
                                pct = annot * 100 / (double)total;
                                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                                filterData.add(new SummaryDataPct("Filtered - No Annotation", annot, pct));
                                pieChartData.add(new PieChart.Data("Filtered - No Annotation", pct));
                            }
                            if(level > 0) {
                                pct = level * 100 / (double)total;
                                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                                filterData.add(new SummaryDataPct("Filtered - LowCount/COV", level, pct));
                                pieChartData.add(new PieChart.Data("Filtered - LowCount/COV", pct));
                            }
                            if(filter > 0) {
                                pct = filter * 100 / (double)total;
                                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                                filterData.add(new SummaryDataPct("Filtered - Transcripts List", filter, pct));
                                pieChartData.add(new PieChart.Data("Filtered - Transcripts List", pct));
                            }
                            pct = notFiltered * 100 / (double)total;
                            pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                            filterData.add(new SummaryDataPct("Not filtered", notFiltered, pct));
                            pieChartData.add(new PieChart.Data("Not filtered", pct));
                            filterData.add(new SummaryDataPct("Total", total, 100.0));
                        }
                    }
                    return null;
                }
            };
            taskInfo = new TaskHandler.TaskInfo("Get project expression matrix data", task);
            return task;
        }
    }
}
