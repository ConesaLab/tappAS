/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabDFISummary extends SubTabBase {
    AnchorPane paneContents;
    ScrollPane spAll;
    GridPane grMain;
    ProgressIndicator pi1, pi2, pi3, pi4;
    TableView tblFeatures, tblGenes;

    String analysisId = "";
    String dfiName = "";
    boolean hasData;
    ContextMenu cmtblFeature;
    ContextMenu cmDiversity;
    DataDFI dfiData;
    DlgDFIAnalysis.Params dfiParams;
    PlotsDFI barplot;
    
    public SubTabDFISummary(Project project) {
        super(project);
    }
    
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            dfiData = new DataDFI(project);
            // check if id was not passed, allow passing as part of subTabId
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectDataViz.Panels.SUMMARYDFI.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectDataViz.Panels.SUMMARYDFI.name().length()).trim());
            }
            if(args.containsKey("id")) {
                analysisId = (String) args.get("id");

                // DFI parameter file is required
                result = Files.exists(Paths.get(dfiData.getDFIParamsFilepath(analysisId)));
                if(result) {
                    //params = dfiData.getDFIParams();
                    dfiParams = DlgDFIAnalysis.Params.load(dfiData.getDFIParamsFilepath(analysisId), project);
                    dfiName = dfiParams.name;
                    String shortName = getShortName(dfiParams.name);
                    subTabInfo.title = "DFI Summary: " + shortName;
                    subTabInfo.tooltip = "Differential Feature Inclusion Analysis Summary '" + dfiParams.name.trim() + "'";
                    setTabLabel(subTabInfo.title, subTabInfo.tooltip);
                    
                    hasData = dfiData.hasDFIData(analysisId);
                }
            else
                app.ctls.alertInformation("DFI Gene Association", "Missing DFI parameter file");

            }else{
                logger.logDebug("DFI id not found");
                result = false;
            }
            
            //if(project.data.isCaseControlExpType()){
                barplot = new PlotsDFI(project, analysisId);
            //}
        }
        return result;
    }
    
    //
    // Internal Functions
    //
    private void showSubTab() {
        paneContents = (AnchorPane) spAll.getContent().lookup("#paneContents");
        grMain = (GridPane) spAll.getContent().lookup("#grdMain");
        pi1 = (ProgressIndicator) spAll.getContent().lookup("#piDFIBarPlot1");
        pi2 = (ProgressIndicator) spAll.getContent().lookup("#piDFIBarPlot2");
        pi3 = (ProgressIndicator) spAll.getContent().lookup("#piDFIBarPlot3");
        pi4 = (ProgressIndicator) spAll.getContent().lookup("#piDFIBarPlot4");
        tblFeatures = (TableView) spAll.getContent().lookup("#tblTestFeatures");
        tblGenes = (TableView) spAll.getContent().lookup("#tblTestGenes");
        
        //if(project.data.isCaseControlExpType()){
            barplot.showDFIBarPlots(subTabInfo, spAll, analysisId, true);
           
            String filepathFeatures = project.data.getDFITestFeaturesFilepath(analysisId);
            String filepathGenes = project.data.getDFITestGenesFilepath(analysisId);
            
            setupTable(tblFeatures, filepathFeatures);
            setupTable(tblGenes, filepathGenes);
            setupExportMenu();

        /*}else{
            grMain.getColumnConstraints().remove(0);
            grMain.getColumnConstraints().remove(1);
            // to dont show the progress indicator
            pi1.setOpacity(0);
            pi2.setOpacity(0);
            pi3.setOpacity(0);
            pi4.setOpacity(0);
        }*/
    }
    
    private void setupTable(TableView tbl, String filepath){
        // setup data summary table
        ObservableList<TableColumn> cols = tbl.getColumns();
        for(TableColumn column : cols){
            switch(column.getId()){
                case "feature":
                        column.setCellValueFactory(new PropertyValueFactory("Feature"));
                        column.setMinWidth(50);
                        column.setPrefWidth(75);
                        column.setMaxWidth(100);
                        column.setSortType(TableColumn.SortType.DESCENDING);
                        break;
                case "pvalue":
                        column.setCellValueFactory(new PropertyValueFactory("PValue"));
                        column.setMinWidth(50);
                        column.setPrefWidth(75);
                        column.setMaxWidth(125);
                        break;
                case "adjpvalue":
                        column.setCellValueFactory(new PropertyValueFactory("AdjPValue"));
                        column.setMinWidth(60);
                        column.setPrefWidth(75);
                        column.setMaxWidth(125);
                        break;
                case "all_dfi":
                        column.setCellValueFactory(new PropertyValueFactory("ALL_DFI"));
                        column.setMinWidth(50);
                        column.setPrefWidth(75);
                        column.setMaxWidth(100);
                        break;
                case "all_ndfi":
                        column.setCellValueFactory(new PropertyValueFactory("ALL_NDFI"));
                        column.setMinWidth(50);
                        column.setPrefWidth(75);
                        column.setMaxWidth(100);
                        break;
                case "feat_dfi":
                        column.setCellValueFactory(new PropertyValueFactory("FEAT_DFI"));
                        column.setMinWidth(50);
                        column.setPrefWidth(75);
                        column.setMaxWidth(100);
                        break;
                case "feat_ndfi":
                        column.setCellValueFactory(new PropertyValueFactory("FEAT_NDFI"));
                        column.setMinWidth(60);
                        column.setPrefWidth(75);
                        column.setMaxWidth(100);
                        break;            
            }
        }
        
        HashMap<String,String[]> hmTestFeatures = Utils.loadTSVListFromFile(filepath);
        ObservableList<DataTable> res = FXCollections.observableArrayList();
        
        for(String aux : hmTestFeatures.keySet()){
            if(aux.equals("Feature")){continue;} //dont take header
            DataTable d = new DataTable(hmTestFeatures.get(aux));
            res.add(d);
        }
        
        tbl.setItems(res);
        
        if(tbl.getItems().size() > 0)
            tbl.getSelectionModel().select(0);
        
        app.export.addCopyToClipboardHandler(tbl);
        tbl.setOnMouseClicked((event) -> {
            if(cmtblFeature != null)
                cmtblFeature.hide();
            cmtblFeature = null;
            if(event.getButton().equals(MouseButton.SECONDARY)) {
                Node node = event.getPickResult().getIntersectedNode();
                if(node != null) {
                    System.out.println("node: " + node);
                    if(!node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        node = node.getParent();
                        System.out.println("parent: " + node);
                    }
                    if(node.getClass().toString().toLowerCase().contains("javafx.scene.control.tablecolumn")) {
                        cmtblFeature = new ContextMenu();
                        app.export.setupSimpleTableExport(tbl, cmtblFeature, false, "tappAS_DFI_" + tbl.getId() + "_" + dfiName + ".tsv");
                        cmtblFeature.setAutoHide(true);
                        cmtblFeature.show(tbl, event.getScreenX(), event.getScreenY());
                    }
                }
            }
        });
    }
    
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item;
        if(project.data.isCaseControlExpType()){
            item = new MenuItem("Bar Plot Features...");
            item.setOnAction((event) -> {
                app.export.exportImage("Bar Plot Features", "tappAS_DFI_BarPlot_Features_" + dfiName + ".png", barplot.imgChart1.getImage());
            });
            cm.getItems().add(item);
            cm.getItems().add(new SeparatorMenuItem());
            
            item = new MenuItem("Bar Plot Genes...");
            item.setOnAction((event) -> {
                app.export.exportImage("Bar Plot Genes", "tappAS_DFI_BarPlot_Genes_" + dfiName + ".png", barplot.imgChart2.getImage());
            });
            cm.getItems().add(item);
            cm.getItems().add(new SeparatorMenuItem());
            
            item = new MenuItem("Bar Plot Favored...");
            item.setOnAction((event) -> {
                app.export.exportImage("Bar Plot Favored", "tappAS_DFI_BarPlot_Favored_" + dfiName + ".png", barplot.imgChart3.getImage());
            });
            cm.getItems().add(item);
            cm.getItems().add(new SeparatorMenuItem());
            
            item = new MenuItem("Box Plot...");
            item.setOnAction((event) -> {
                app.export.exportImage("Bar Plot Genes", "tappAS_DFI_BoxPlot_" + dfiName + ".png", barplot.imgChart4.getImage());
            });
            cm.getItems().add(item);
            cm.getItems().add(new SeparatorMenuItem());
        }
        subTabInfo.cmExport = cm;
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        spAll = (ScrollPane) tabNode.lookup("#spAll");
        showSubTab();
    }
    
    //
    //Data Classes
    //
    public static class DataTable {
        public final SimpleStringProperty Feature;
        public final SimpleIntegerProperty ALL_NDFI;
        public final SimpleIntegerProperty ALL_DFI;
        public final SimpleIntegerProperty FEAT_NDFI;
        public final SimpleIntegerProperty FEAT_DFI;
        public final SimpleDoubleProperty PValue;
        public final SimpleDoubleProperty AdjPValue;

        public DataTable(String[] data) {
            this.Feature = new SimpleStringProperty(data[0]);
            this.PValue = new SimpleDoubleProperty(Double.valueOf(data[1]));
            this.ALL_NDFI = new SimpleIntegerProperty(Integer.valueOf(data[2]));
            this.FEAT_NDFI = new SimpleIntegerProperty(Integer.valueOf(data[3]));
            this.ALL_DFI = new SimpleIntegerProperty(Integer.valueOf(data[4]));
            this.FEAT_DFI = new SimpleIntegerProperty(Integer.valueOf(data[5]));
            this.AdjPValue = new SimpleDoubleProperty(Double.valueOf(data[6]));
        }
        
        public String getFeature(){return Feature.get();}
        public int getALL_NDFI(){return ALL_NDFI.get();}
        public int getALL_DFI(){return ALL_DFI.get();}
        public int getFEAT_NDFI(){return FEAT_NDFI.get();}
        public int getFEAT_DFI(){return FEAT_DFI.get();}
        public double getPValue(){return PValue.get();}
        public double getAdjPValue(){return AdjPValue.get();}
    }
    
    public static class VarSummaryData {
        public final SimpleStringProperty region;
        public final SimpleIntegerProperty genes;
        public final SimpleDoubleProperty pct;

        public VarSummaryData(String region, int genes, double pct) {
            this.region = new SimpleStringProperty(region);
            this.genes = new SimpleIntegerProperty(genes);
            this.pct = new SimpleDoubleProperty(pct);
        }
        public String getRegion() { return region.get(); }
        public int getGenes() { return genes.get(); }
        public double getPct() { return pct.get(); }
    }
    
    public static class Counts {
        public String feature;
        public int dsc1, dsc2;
        public int total;

        public Counts(String feature, int dsc1, int dsc2, int total) {
            this.feature = feature;
            this.dsc1 = dsc1;
            this.dsc2 = dsc2;
            this.total = total;
        }
    }
    public static class GeneCounts {
        public String feature;
        public HashMap<String, Object> hmDSGenes;
        public HashMap<String, Object> hmTotalGenes;

        public GeneCounts(String feature, String gene, boolean ds) {
            this.feature = feature;
            this.hmDSGenes = new HashMap<>();
            this.hmTotalGenes = new HashMap<>();
            if(ds)
                hmDSGenes.put(gene, null);
            hmTotalGenes.put(gene, null);
        }
        public void updateGene(String gene, boolean ds) {
            // ok to re-add instead of checking if already there
            if(ds)
                hmDSGenes.put(gene, null);
            hmTotalGenes.put(gene, null);
        }
    }
}
