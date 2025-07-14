/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
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

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class GOGraph extends AppObject {    
    AnchorPane paneGO;
    Pane paneApply, paneControls;
    ToggleButton tbBP, tbMF, tbCC;
    Slider sliderLevels;
    WebView webGraph;
    WebEngine engine;
    ChoiceBox cbTerms, cbLevels;
    
    SubTabBase subTabBase;
    String parentID;
    ArrayList<String> lstDataSelections = new ArrayList<>();
    ArrayList<String> lstGOTerms = new ArrayList<>();
    GO.GOCat catSel = GO.GOCat.BP;
    GO.Show show = GO.Show.Name;
    boolean bpData = false;
    boolean mfData = false;
    boolean ccData = false;
    boolean updatingFlag = true;
    String selectedData = "";
    double tpWidth = 0.0;
    double tpHeight = 0.0;
    GO go = new GO();
    
    public GOGraph() {
        super(null, null);
    }
    public void initialize(SubTabBase subTabBase, String parentID) {
        this.subTabBase = subTabBase;
        this.parentID = parentID;
    }
    public void setupControls(AnchorPane paneGO, double tpWidth, double tpHeight, ArrayList<String> lstDataSelections) {
        this.paneGO = paneGO;
        this.tpWidth = tpWidth;
        this.tpHeight = tpHeight;
        this.lstDataSelections = lstDataSelections;
        try {
            Node tabNode = (Node) FXMLLoader.load(app.ctlr.fxdc.getClass().getResource("/tappas/tabs/GOGraph.fxml"));
            paneGO.getChildren().clear();
            paneGO.getChildren().add(tabNode);
            AnchorPane.setTopAnchor(tabNode, 0.0);
            AnchorPane.setBottomAnchor(tabNode, 0.0);
            AnchorPane.setLeftAnchor(tabNode, 0.0);
            AnchorPane.setRightAnchor(tabNode, 0.0);

            // get control objects
            paneControls = (Pane) tabNode.lookup("#paneGO_Controls");
            cbTerms = (ChoiceBox) tabNode.lookup("#cbGO_Terms");
            tbBP = (ToggleButton) tabNode.lookup("#tbGO_BP");
            tbMF = (ToggleButton) tabNode.lookup("#tbGO_MF");
            tbCC = (ToggleButton) tabNode.lookup("#tbGO_CC");
            cbLevels = (ChoiceBox) tabNode.lookup("#cbGO_Levels");
            webGraph = (WebView) tabNode.lookup("#webGO_DAG");

            // setup listeners and event handlers
            cbTerms.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                selectedData = (String) cbTerms.getItems().get((int)newValue);
                updateData();
                setGODataFlags();
                if(!updatingFlag)
                    updatePage();
            });
            tbBP.setOnAction((itemEvent) -> { if(tbBP.isSelected()) catSel = GO.GOCat.BP; if(!updatingFlag) updatePage();});// disableBtnApply(getCategory() == null);});
            tbMF.setOnAction((itemEvent) -> { if(tbMF.isSelected()) catSel = GO.GOCat.MF; if(!updatingFlag) updatePage();});//   disableBtnApply(getCategory() == null);});
            tbCC.setOnAction((itemEvent) -> { if(tbCC.isSelected()) catSel = GO.GOCat.CC; if(!updatingFlag) updatePage();});//   disableBtnApply(getCategory() == null);});
            cbLevels.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                if(!updatingFlag)
                    updatePage();
            });
            
            // setup web objects
            webGraph.setContextMenuEnabled(false);
            engine = webGraph.getEngine();
        } catch(Exception e) {
            app.logError("Unable to create GO graph display: " + e.getMessage());
        }
    }
    public void updateDataSelections(ArrayList<String> lstDataSelections) {
        this.lstDataSelections = lstDataSelections;
    }
    public void updateShowGraph(GO.Show show) {
        this.show = show;
        showGraph(false);
    }
    public void showGraph(boolean populated) {
        if(!populated) {
            // set first time only
            updatingFlag = true;
            if(cbTerms.getItems().size() == 0) {
                for(String sel : lstDataSelections)
                    cbTerms.getItems().add(sel);
                cbTerms.getSelectionModel().select(0);
                updateData();
                setGODataFlags();
                if(getCategory() == null) {
                    app.ctls.alertInformation("No GO Terms", "There are no GO Terms for specified data");
                    paneControls.setDisable(true);
                    engine.loadContent("<htm><body></body></html>");
                    return;
                }
            }
            else
                updateData();
            updateGraph(true);
            updatingFlag = false;
        }
    }
    public void updateGraph(boolean alert) {
        // build graph html file
        String filepath = Paths.get(app.data.getAppDataFolder(), "GODAG_" + parentID + ".html").toString();
        int selLevel = 0;
        if(cbLevels.getItems().size() > 0)
            selLevel = cbLevels.getSelectionModel().getSelectedIndex();
        int maxLevel = (selLevel == 0)? 2 : ((selLevel == 1)? 999 : selLevel - 1);
        double width = paneGO.getWidth();
        double height = paneGO.getHeight();
        width = (width == 0.0)? tpWidth : width;
        height = (height == 0.0)? tpHeight : height;
        GO.GraphResult result = go.buildGraph(getCategory(), maxLevel, lstGOTerms, width, height, show);

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
            //System.out.println("Max DAG level: " + result.maxLevel);
            Writer writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filepath, false), "utf-8"));
                writer.write(result.html);
            } catch(Exception e) {
                app.logError("Unable to write GO DAG html file: " + e.getMessage());
            } finally {
               try { if(writer != null) writer.close();} catch (Exception e) { System.out.println(e.getClass().getName() + " Close writer code exception: " + e.getMessage()); }
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
            app.ctls.alertInformation("No GO Terms", "There are no GO Terms for specified data");
            engine.loadContent("<htm><body></body></html>");
        }
    }
    
    //
    // Internal Functions
    //
    private void setGODataFlags() {
        double width = paneGO.getWidth();
        double height = paneGO.getHeight();
        width = (width == 0.0)? tpWidth : width;
        height = (height == 0.0)? tpHeight : height;
        
        // determine what GO categories are available (BP, MF, CC) and setup UI accordingly
        GO.GraphResult result = go.buildGraph(GO.GOCat.BP, 1, lstGOTerms, width, height, show);
        bpData = result.bp;
        result = go.buildGraph(GO.GOCat.MF, 1, lstGOTerms, width, height, show);
        mfData = result.mf;
        result = go.buildGraph(GO.GOCat.CC, 1, lstGOTerms, width, height, show);
        ccData = result.cc;
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
    private void updatePage() {
        updateData();
        showGraph(false);
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
    private void updateData() {
        lstGOTerms.clear();
        HashMap<String, Object> hm = new HashMap<>();
        if(cbTerms.getSelectionModel().getSelectedItem() != null) {
            hm.put("getData", selectedData);
            lstGOTerms = (ArrayList<String>) subTabBase.processRequest(hm);
        }
    }    
}
