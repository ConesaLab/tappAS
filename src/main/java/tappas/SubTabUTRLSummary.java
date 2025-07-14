/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Pedro Salguero - psalguero@cipf.es
 */
public class SubTabUTRLSummary extends SubTabBase {
    AnchorPane paneContents;
    TableView tbl_UTR; //tblData,
    ImageView imgChart;
    
    ScrollPane scrollPane;
    ContextMenu cmTbl;
    Pane paneImgChart;
    

    DataUTRL utrlData;
    DlgUTRLAnalysis.Params utrlParams;
    
    ProgressIndicator pi;
    
    public SubTabUTRLSummary(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            utrlData = new DataUTRL(project);
            utrlParams =  utrlData.getUTRLParams();
            result = utrlData.hasUTRLData();
            //already have img
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        if(hm.containsKey("updateUTRLResults")) {
            // should not take long, OK to not do in background - Arreglar lista!!!
            utrlParams = utrlData.getUTRLParams();
            ArrayList<DataUTRL.UTRLResultsData> utrlResults = utrlData.getUTRLResultsData();
            displayData(utrlResults, project.data.isTimeCourseExpType());
        }
        return obj;
    }

    //
    // Internal Functions
    //
    private void showSubTab(ArrayList<DataUTRL.UTRLResultsData> utrlResults) {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        scrollPane = (ScrollPane) tabNode.lookup("#scrollPane");

        tbl_UTR = (TableView) scrollPane.getContent().lookup("#tbl_UTR");
        pi = (ProgressIndicator) scrollPane.getContent().lookup("#piUTRPlot");
        imgChart = (ImageView) scrollPane.getContent().lookup("#imgChart");
        paneImgChart = (Pane) scrollPane.getContent().lookup("#paneImgChart");

        imgChart.fitHeightProperty().bind(paneImgChart.heightProperty());
        imgChart.fitWidthProperty().bind(paneImgChart.widthProperty());
        imgChart.setPreserveRatio(true);

        String filepath = project.data.getUTRPlotFilepath();
        showBoxPlotImage(filepath);

        // PVAL table
        // setup and populate table - from file data - add from RScrit new file with all pvals
        tbl_UTR.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        ObservableList<TableColumn> cols = tbl_UTR.getColumns();
        cols.get(0).setCellValueFactory(new PropertyValueFactory<>("UTR"));
        cols.get(1).setCellValueFactory(new PropertyValueFactory<>("PVAL"));
        app.export.addCopyToClipboardHandler(tbl_UTR);
        tbl_UTR.setRowFactory(tableView -> {
            TableRow<SummaryCountData> row = new TableRow<>();
            row.selectedProperty().addListener((obs, oldData, newData) -> {
                SummaryCountData sd = row.getItem();
                if(newData)
                    row.setStyle("");
                else
                    row.setStyle("-fx-background-color: honeydew;");
            });
            row.itemProperty().addListener((obs, oldData, newData) -> {
                row.setStyle("-fx-background-color: honeydew;");
            });
            return row;
        });
        
        tbl_UTR.setOnMouseClicked((event) -> {
            if(cmTbl != null)
                cmTbl.hide();
            cmTbl = null;
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                Node node = event.getPickResult().getIntersectedNode();
                if(node != null) {
                    System.out.println("node: " + node);
                    if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        node = node.getParent();
                        System.out.println("parent: " + node);
                    }
                    if(node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        cmTbl = new ContextMenu();
                        app.export.setupSimpleTableExport(tbl_UTR, cmTbl, false, "tappAS_UTRL_Summary.tsv");
                        cmTbl.setAutoHide(true);
                        cmTbl.show(tbl_UTR, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });

        app.export.addCopyToClipboardHandler(tbl_UTR);

        displayData(utrlResults, project.data.isTimeCourseExpType());

        setupExportMenu();
        setFocusNode(tbl_UTR, true);
    }

    private void showBoxPlotImage(String filepath) {
        Image img = new Image("file:" + filepath);
        pi.setVisible(false);
        imgChart.setImage(img);
        imgChart.setVisible(true);
        app.ctls.setupImageExport(imgChart, "UTRL Violin Plot", "tappAS_wUTRDifference_ViolinPlot.png", null);

    }

    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export UTR summary table data...");
        item.setOnAction((event) -> { app.export.exportTableData(tbl_UTR, true, "tappAS_UTRL_Summary_table.tsv"); });
        cm.getItems().add(item);
        
        item = new MenuItem("UTRL Violin Plot...");
        item.setOnAction((event) -> {
            app.export.exportImage("UTRL Violin Plot", "tappAS_wUTRDifference_ViolinPlot.png", imgChart.getImage());
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
            app.export.exportImage("UTRL Summary Panel", "tappAS_UTRL_Summary_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    private void displayData(ArrayList<DataUTRL.UTRLResultsData> utrlResults, boolean tc) {
        setTableData();
    }

    private void setTableData() {
        ObservableList<SummaryCountData> sumdata = FXCollections.observableArrayList();

        // UTR table
        HashMap<String, String> hmUTRPVal = project.data.getUTRPVal();

        sumdata = FXCollections.observableArrayList();
        sumdata.add(new SummaryCountData("UTR3", Double.parseDouble(hmUTRPVal.get("UTR3"))));
        sumdata.add(new SummaryCountData("UTR5", Double.parseDouble(hmUTRPVal.get("UTR5"))));
        tbl_UTR.setItems(sumdata);

        if(!sumdata.isEmpty())
            tbl_UTR.getSelectionModel().select(0);
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        ArrayList<DataUTRL.UTRLResultsData> data = utrlData.getUTRLResultsData();
        showSubTab(data);
    }

    //
    // Data Classes
    //
    public static class SummaryCountData {
        public final SimpleStringProperty UTR;
        public final SimpleDoubleProperty PVAL;
 
        public SummaryCountData(String field, double pval) {
            this.UTR = new SimpleStringProperty(field);
            this.PVAL = new SimpleDoubleProperty(pval);
        }
        public String getUTR() { return UTR.get(); }
        public double getPVAL() { return PVAL.get(); }
    }
}
