/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabGOGraph extends SubTabBase {
    AnchorPane paneGO;
    Pane paneControls;
    ToggleButton tbBP, tbMF, tbCC;
    ChoiceBox cbTerms, cbLevels;
    WebView webGraph;
    WebEngine engine;
    
    ArrayList<String> lstDataSelections = new ArrayList<>();
    ArrayList<String> lstGOTerms = new ArrayList<>();
    GO.GOCat catSel = GO.GOCat.BP;
    GO.Show show = GO.Show.Name;
    boolean bpData = false;
    boolean mfData = false;
    boolean ccData = false;
    boolean updatingFlag = true;
    boolean updGraph = false;
    String gene;
    String selectedLevel = "";
    String selectedData = "";
    GO go = new GO();
    Random rand = new Random();
    
    public SubTabGOGraph(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = true;
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            gene = (String)args.get("gene");
        }
        return result;
    }
    @Override
    protected void onSelect(boolean selected) {
        super.onSelect(selected);
        if(selected) {
            if(updGraph) {
                showGraph();
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
        paneGO = (AnchorPane) tabNode.lookup("#paneGO");
        paneControls = (Pane) tabNode.lookup("#paneGO_Controls");
        cbTerms = (ChoiceBox) tabNode.lookup("#cbGO_Terms");

        // set options menu
        ContextMenu cm = new ContextMenu();
        ToggleGroup tg = new ToggleGroup();
        RadioMenuItem ritem = new RadioMenuItem("Show GO term id");
        ritem.setToggleGroup(tg);
        ritem.setOnAction((event) -> { showInfo(GO.Show.Id); });
        cm.getItems().add(ritem);
        ritem = new RadioMenuItem("Show GO term name");
        ritem.setToggleGroup(tg);
        ritem.setSelected(true);
        ritem.setOnAction((event) -> { showInfo(GO.Show.Name); });
        cm.getItems().add(ritem);
        ritem = new RadioMenuItem("Show GO term id and name");
        ritem.setToggleGroup(tg);
        ritem.setOnAction((event) -> { showInfo(GO.Show.Both); });
        cm.getItems().add(ritem);
        subTabInfo.cmOptions = cm;

        HashMap<String, Object> hm = project.data.getGeneTrans(gene);
        lstDataSelections = new ArrayList<>();
        lstDataSelections.add("All transcripts combined");
        for(String trans : hm.keySet())
            lstDataSelections.add("Transcript " + trans);

        cbTerms.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            if(newValue != null) {
                selectedData = (String) cbTerms.getItems().get((int)newValue);
                updateData();
                setGODataFlags();
                if(!updatingFlag)
                    updatePage();
            }
        });
        tbBP = (ToggleButton) tabNode.lookup("#tbGO_BP");
        tbBP.setOnAction((itemEvent) -> { if(tbBP.isSelected()) catSel = GO.GOCat.BP; if(!updatingFlag) updatePage();});
        tbMF = (ToggleButton) tabNode.lookup("#tbGO_MF");
        tbMF.setOnAction((itemEvent) -> { if(tbMF.isSelected()) catSel = GO.GOCat.MF; if(!updatingFlag) updatePage();});
        tbCC = (ToggleButton) tabNode.lookup("#tbGO_CC");
        tbCC.setOnAction((itemEvent) -> { if(tbCC.isSelected()) catSel = GO.GOCat.CC; if(!updatingFlag) updatePage();});
        cbLevels = (ChoiceBox) tabNode.lookup("#cbGO_Levels");
        cbLevels.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            if(!updatingFlag)
                updatePage();
        });
        webGraph = (WebView) tabNode.lookup("#webGO_DAG");
        webGraph.setContextMenuEnabled(false);
        ContextMenu webCM = new ContextMenu();
        MenuItem item = new MenuItem("Export image...");
        item.setOnAction((event) -> { exportImage(); });
        webCM.getItems().add(item);
        webGraph.setOnMouseClicked((event) -> {
            if(event.getButton() == MouseButton.SECONDARY) {
                webCM.show(tabNode, event.getScreenX(), event.getScreenY());
            }
            else
                webCM.hide();
        });

        engine = webGraph.getEngine();
        paneGO.widthProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            if(subTab.isSelected())
                showGraph();
            else
                updGraph = true;
        });
        paneGO.heightProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            if(subTab.isSelected())
                showGraph();
            else
                updGraph = true;
        });
        showGraph();
        setFocusNode(webGraph, true);
    }
    private void showInfo(GO.Show show) {
        this.show = show;
        showGraph();
    }
    private void showGraph() {
        updatingFlag = true;
        if(cbTerms.getItems().size() == 0) {
            for(String sel : lstDataSelections)
                cbTerms.getItems().add(sel);
            cbTerms.getSelectionModel().select(0);
            updateData();
            setGODataFlags();
            if(getCategory() == null) {
                // don't call from here, "showAndWait is not allowed during animation or layout processing"
                Platform.runLater(() -> {
                    app.ctls.alertInformation("No GO Terms", "There are no GO Terms for specified data");
                });
                paneControls.setDisable(true);
                engine.loadContent("<htm><body></body></html>");
                return;
            }
        }
        else
            updateData();

        // build graph html file
        String filepath = Paths.get(app.data.getAppDataFolder(), "GODAG_" + tabBase.tabId + ".html").toString();
        int selLevel = 0;
        if(cbLevels.getItems().size() > 0)
            selLevel = cbLevels.getSelectionModel().getSelectedIndex();
        int maxLevel = (selLevel == 0)? 2 : ((selLevel == 1)? 999 : selLevel - 1);
        double w = paneGO.getWidth();
        double h = paneGO.getHeight();
        w =  (w == 0)? 800 : w;
        h = (h == 0)? 300 : h;
        GO.GraphResult result = go.buildGraph(getCategory(), maxLevel, lstGOTerms, w, h, show);

        if(result.maxLevel > 0) {
            cbLevels.getItems().clear();
            cbLevels.getItems().add("Low level graph");
            cbLevels.getItems().add("Full graph");
            for(int i = 1; i <= result.maxLevel; i++)
                cbLevels.getItems().add("Up to level " + i);
            if((selLevel - 1) > result.maxLevel)
                cbLevels.getSelectionModel().select(1);
            else
                cbLevels.getSelectionModel().select(selLevel);
            System.out.println("Max DAG level: " + result.maxLevel);
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
                writer.write(result.html);
            } catch(Exception e) {
                app.logError("Unable to write GO DAG html file: " + e.getMessage());
            } finally {
               try { if(writer != null) writer.close(); } catch (Exception e) { System.out.println("Writer close exception within exception: " + e.getMessage());}
            }
            String url;
            if(Utils.isWindowsOS()) {
                // need an additional slash when using drive letters
                url = "/" + filepath.replaceAll("\\\\", "/");
                logger.logInfo("fp: " + url);
            }
            else
                url = filepath;
            engine.load("file://" + url);
        }
        else {
            // don't call from here, "showAndWait is not allowed during animation or layout processing"
            Platform.runLater(() -> {
                app.ctls.alertInformation("No GO Terms", "There are no GO Terms for specified data");
            });
            engine.loadContent("<htm><body></body></html>");
        }
        updatingFlag = false;
    }
    private void updateData() {
        lstGOTerms.clear();
        HashMap<String, Object> hm = new HashMap<>();
        if(cbTerms.getSelectionModel().getSelectedItem() != null) {
            hm.put("getData", selectedData);
            lstGOTerms = (ArrayList<String>) tabBase.processRequest(hm);
        }
    }
    private void updatePage() {
        updateData();
        showGraph();
    }
    private GO.GOCat getCategory() {
        GO.GOCat cat = null;
        if(tbBP.isSelected())
            cat = GO.GOCat.BP;
        else if(tbMF.isSelected())
            cat = GO.GOCat.MF;
        else if(tbCC.isSelected())
            cat = GO.GOCat.CC;
        return cat;
    }
    private void setGODataFlags() {
        double w = paneGO.getWidth();
        double h = paneGO.getHeight();
        w =  (w == 0)? 800 : w;
        h = (h == 0)? 300 : h;
        GO.GraphResult result = go.buildGraph(GO.GOCat.BP, 1, lstGOTerms, w, h, show);
        bpData = result.bp;
        result = go.buildGraph(GO.GOCat.MF, 1, lstGOTerms, w, h, show);
        mfData = result.mf;
        result = go.buildGraph(GO.GOCat.CC, 1, lstGOTerms, w, h, show);
        ccData = result.cc;
        System.out.println("setGODataFlags: " + bpData + ", " + mfData + ", " + ccData);
        tbBP.setDisable(!bpData);
        tbMF.setDisable(!mfData);
        tbCC.setDisable(!ccData);
        if(!tbBP.isSelected() && !tbMF.isSelected() && !tbCC.isSelected())
            tbBP.setSelected(true);
        if(tbBP.isSelected() && !bpData) {
            tbBP.setSelected(false);
            if(mfData)
                tbMF.setSelected(true);
            else if(ccData)
                tbCC.setSelected(true);
        }
        if(tbMF.isSelected() && !mfData) {
            tbMF.setSelected(false);
            if(bpData)
                tbBP.setSelected(true);
            else if(ccData)
                tbCC.setSelected(true);
        }
        if(tbCC.isSelected() && !ccData) {
            tbCC.setSelected(false);
            if(bpData)
                tbBP.setSelected(true);
            else if(mfData)
                tbMF.setSelected(true);
        }
    }
    private void exportImage() {
        WritableImage img = webGraph.snapshot(new SnapshotParameters(), null);
        app.export.exportImage("Gene Ontology Visualization", "tappAS_"+gene+"_Gene_Ontology_Visualization.png", (Image)img);
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        showSubTab();
    }
}
