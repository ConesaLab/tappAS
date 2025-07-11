/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabFEAClustersGO extends SubTabBase {
    AnchorPane paneOntology;
    
    GOGraph goTerms;
    DataFEA feaData;
    HashMap<String, String> feaParams = new HashMap<>();
    DlgFEAnalysis.Params params;
    String analysisId = "";
    boolean updGraph = false;
    ArrayList<ArrayList<String>> lstClusterTerms = new ArrayList<>();

    public SubTabFEAClustersGO(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = true;
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            feaData = new DataFEA(project);
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectDataViz.Panels.CLUSTERSGOFEA.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectDataViz.Panels.CLUSTERSGOFEA.name().length()).trim());
            }
            if(args.containsKey("id") && args.containsKey("lstClusterTerms")) {
                analysisId = (String) args.get("id");
                lstClusterTerms = (ArrayList<ArrayList<String>>) args.get("lstClusterTerms");
                feaParams = feaData.getFEAParams(analysisId);
                params = new DlgFEAnalysis.Params(feaParams);
                result = feaData.hasFEAData(params.dataType.name(), analysisId);
                if(result) {
                    String shortName = getShortName(params.name);
                    subTabInfo.title = "FEA Clusters GO: '" + shortName + "'";
                    subTabInfo.tooltip = "FEA enriched features clusters GO Terms for '" + params.name + "'";
                    setTabLabel(subTabInfo.title, subTabInfo.tooltip);
                }
                else
                    result = false;
            }
            else
                result = false;
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        if(hm.containsKey("getData")) {
            ArrayList<String> lst = new ArrayList<>();
            String sel = (String) hm.get("getData");
            int pos = sel.indexOf("Cluster ");
            if(pos != -1) {
                String cidx = sel.substring(pos + "Cluster ".length());
                pos = cidx.indexOf(" ");
                if(pos != -1)
                    cidx = cidx.substring(0, pos);
                try {
                    int idx = Integer.parseUnsignedInt(cidx) - 1;
                    if(idx >= 0 && lstClusterTerms.size() > idx) {
                        ArrayList<String> lstCT = lstClusterTerms.get(idx);
                        for(String term : lstCT)
                            lst.add(term);
                    }
                } catch(Exception e) { 
                    logger.logError("Internal program error: " + e.getMessage());
                }
            }
            obj = lst;
        }
        return obj;
    }
    @Override
    protected void onSelect(boolean selected) {
        super.onSelect(selected);
        if(selected) {
            if(updGraph) {
                goTerms.updateGraph(false);
                updGraph = false;
            }
        }
    }
    @Override
    protected void onButtonExport() {
        exportImage();
    }    

    //
    // Internal Functions
    //
    private void showSubTab() {
        paneOntology = (AnchorPane) tabNode.lookup("#paneMain");

        // set options menu
        ContextMenu cm = new ContextMenu();
        ToggleGroup tg = new ToggleGroup();
        RadioMenuItem ritem = new RadioMenuItem("Show GO term id");
        ritem.setToggleGroup(tg);
        ritem.setOnAction((event) -> { goTerms.updateShowGraph(GO.Show.Id); });
        cm.getItems().add(ritem);
        ritem = new RadioMenuItem("Show GO term name");
        ritem.setToggleGroup(tg);
        ritem.setSelected(true);
        ritem.setOnAction((event) -> { goTerms.updateShowGraph(GO.Show.Name); });
        cm.getItems().add(ritem);
        ritem = new RadioMenuItem("Show GO term id and name");
        ritem.setToggleGroup(tg);
        ritem.setOnAction((event) -> { goTerms.updateShowGraph(GO.Show.Both); });
        cm.getItems().add(ritem);
        subTabInfo.cmOptions = cm;

        goTerms = new GOGraph();
        goTerms.initialize(this, analysisId);
        ArrayList<String> lstDataSelections = new ArrayList<>();
        for(int idx = 1; idx <= lstClusterTerms.size(); idx++)
            lstDataSelections.add("Cluster " + idx);
        goTerms.updateDataSelections(lstDataSelections);
        goTerms.setupControls(paneOntology, app.tabs.getBottomTPWidth(), app.tabs.getBottomTPHeight(), lstDataSelections);
        goTerms.showGraph(false);
        paneOntology.widthProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            if(subTab.isSelected())
                goTerms.updateGraph(false);
            else
                updGraph = true;
        });
        paneOntology.heightProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            if(subTab.isSelected())
                goTerms.updateGraph(false);
            else
                updGraph = true;
        });
        setFocusNode(paneOntology, true);
    }
    private void exportImage() {
        WritableImage img = goTerms.webGraph.snapshot(new SnapshotParameters(), null);
        app.export.exportImage("FEA Cluster Gene Ontology Visualization", "tappAS_FEA_"+params.name+"_Cluster_Gene_Ontology_Visualization.png", (Image)img);
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        showSubTab();
    }
}
