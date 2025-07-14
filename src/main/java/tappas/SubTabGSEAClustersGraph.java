/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.beans.value.ObservableValue;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static javafx.concurrent.Worker.State;

/**
 *
 * @author Pedro Salguero - psalguero@cipf.es
 */
public class SubTabGSEAClustersGraph extends SubTabBase {
    AnchorPane paneWeb;
    WebView webGraph;
    WebEngine engine;

    DataGSEA GSEAData;
    HashMap<String, String> GSEAParams = new HashMap<>();
    DlgGSEAnalysis.Params params;
    boolean run = false;
    String analysisId = "";
    String contextNode = "";
    boolean updGraph = false;
    boolean htmlLoaded = false;

    public SubTabGSEAClustersGraph(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            GSEAData = new DataGSEA(project);
            // check if id was not passed, allow passing as part of subTabId
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectDataViz.Panels.CLUSTERSGSEA.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectDataViz.Panels.CLUSTERSGSEA.name().length()).trim());
            }
            if(args.containsKey("id")) {
                analysisId = (String) args.get("id");
                GSEAParams = GSEAData.getGSEAParams(analysisId);
                params = new DlgGSEAnalysis.Params(GSEAParams);
                String shortName = getShortName(params.name);
                subTabInfo.title = "GSEA Clusters Graph: '" + shortName + "'";
                subTabInfo.tooltip = "GSEA enriched GSEAFeatures clusters graph for '" + params.name + "'";
                setTabLabel(subTabInfo.title, subTabInfo.tooltip);
            }
            else
                result = false;
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    @Override
    protected void onSelect(boolean selected) {
        super.onSelect(selected);
        if(selected) {
            if(updGraph) {
                double w = paneWeb.getWidth();
                double h = paneWeb.getHeight();
                System.out.println("onSelect svgResize(" + w + ", " + h + ")");
                if(w > 10 && h > 10 && htmlLoaded)
                    engine.executeScript("svgResize(" + w + ", " + h + ")");
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
        paneWeb = (AnchorPane) tabNode.lookup("#paneWeb");
        webGraph = (WebView) tabNode.getParent().lookup("#webView");

        engine = webGraph.getEngine();
        webGraph.setContextMenuEnabled(false);
        ContextMenu webCM = new ContextMenu();
        MenuItem item = new MenuItem("Export image...");
        item.setOnAction((event) -> { exportImage(); });
        webCM.getItems().add(item);
        webGraph.setOnMouseClicked((event) -> {
            if(event.getButton() == MouseButton.SECONDARY) {
                //contextNode = (String) engine.executeScript("getContextMenuNode()");
                //System.out.println("contextnode: " + contextNode);
                //if(!contextNode.isEmpty())
                if(htmlLoaded)
                    webCM.show(tabNode, event.getScreenX(), event.getScreenY());
            }
            else
                webCM.hide();
        });

        String html = "";
        if(args.containsKey("html")) {
            logger.logInfo("Got arg HTML content.");
            html = (String) args.get("html");
        }
        else {
            String filepath = GSEAData.getGSEAResultsClusterFilepath("graph", analysisId);
            if(Files.exists(Paths.get(filepath))) {
                try {
                    byte[] htmlBytes = Files.readAllBytes(Paths.get(filepath));
                    html = new String(htmlBytes, StandardCharsets.UTF_8);
                    logger.logInfo("Got file HTML content (" + html.length() + "): " + filepath);
                }
                catch (Exception e) {
                    app.logError("Unable to load graph HTML file: " + e.getMessage());
                }
            }
            else
                logger.logInfo("HTML file does not exists: " + filepath);
        }
        if(!html.isEmpty()) {
            ArrayList<Integer> lstClusters = null;
            if(args.containsKey("lstClusters"))
                lstClusters = (ArrayList<Integer>) args.get("lstClusters");
            else {
                String filepath = GSEAData.getGSEAResultsClusterFilepath("clusters", analysisId);
                if(Files.exists(Paths.get(filepath))) {
                    try {
                        lstClusters = new ArrayList<>();
                        List<String> lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
                        for(String line : lines) {
                            String[] fields = line.split("\t");
                            if(fields.length > 1)
                                lstClusters.add(Integer.parseInt(fields[0].trim()));
                        }
                    }
                    catch (Exception e) {
                        app.logError("Unable to load clusters data file: " + e.getMessage());
                    }
                }
            }
            if(lstClusters != null) {
                int width = (int) app.tabs.getBottomTPWidth();
                int height = (int) app.tabs.getBottomTPHeight();
                String htmlPositions = getJSInitialPositions(lstClusters, width, height);
                html = html.replace("<<d3positions>>", htmlPositions);
                html = html.replace("<<width>>", width + "");
                html = html.replace("<<height>>", height + "");
                engine.getLoadWorker().stateProperty().addListener((ObservableValue <? extends State> ov, State oldValue, State newValue) -> {
                    if (newValue == State.SUCCEEDED) {
                        System.out.println("WebEngine state changed - HTML loaded.");
                        htmlLoaded = true;
                    }
                });                    
                engine.loadContent(html);
                //Utils.saveTextToFile(html, "<dbgdir>/html.txt");

                // the problem with resizing is that the html/js script has the width and height set in the document
                paneWeb.widthProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                    if(subTab.isSelected()) {
                        double w = newValue.doubleValue();
                        double h = paneWeb.getHeight();
                        if(w > 10 && h > 10 && htmlLoaded)
                            engine.executeScript("svgResize(" + w + ", " +h + ")");
                    }
                    else
                        updGraph = true;
                });
                paneWeb.heightProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
                    if(subTab.isSelected()) {
                        double w = paneWeb.getWidth();
                        double h = newValue.doubleValue();
                        if(w > 10 && h > 10 && htmlLoaded)
                            engine.executeScript("svgResize(" + w + ", " + h + ")");
                    }
                    else
                        updGraph = true;
                });
            }
            else
                logger.logInfo("Empty cluster list.");
        }
        else
            logger.logInfo("No HTML content available.");
        onSelectFocusNode = webGraph;
        webGraph.requestFocus();
    }
    private void exportImage() {
        SnapshotParameters sP = new SnapshotParameters();
        double x = webGraph.getWidth();
        double ratio = 3840/x;
        webGraph.setScaleX(ratio);
        webGraph.setScaleY(ratio);
        WritableImage img = webGraph.snapshot(sP, null);
        double newX = webGraph.getWidth();
        ratio = x/newX;
        webGraph.setScaleX(ratio);
        webGraph.setScaleY(ratio);
        
        //WritableImage img = webGraph.snapshot(new SnapshotParameters(), null);
        app.export.exportImage("GSEA Cluster", "tappAS_GSEA_"+params.name+"_cluster.png", (Image)img);
    }
    private String getJSInitialPositions(ArrayList<Integer> lstClusters, int width, int height) {
        System.out.println("webwidth: " + webGraph.getWidth() + ", " + webGraph.getHeight());
        double ratio = (int) width / height;
        int delta = 50;
        int jsdelta = 15;
        int cnt = lstClusters.size();
        String str = "var delta = " + jsdelta + ";\nvar nClusters = " + cnt + ";\n";
        ArrayList<Integer> lst = new ArrayList<>();
        int nodes = 0;
        for(int size : lstClusters) {
            lst.add(size);
            nodes += size;
        }
        Collections.sort(lst);
        double n = Math.round(Math.sqrt(nodes) + 1);
        System.out.println("clusters: " + cnt + ", nodes: " + nodes + ", largest: " + ((cnt > 0)? lst.get(cnt -1) : "N/A") + ", n: " + n);

        // we have 'n' clusters sorted in asc order (by number of nodes) - now how do we lay them out?
        // size of clusters relative to each other and the number of them is relevant
        String strX = "var posx = [0";
        String strY = "var posy = [0";
        int posX, posY;
        if(cnt == 1) {
            n = Math.sqrt(lst.get(0));
            posX = (int) (width / 2 - (n * delta / 2));
            posY = (int) (height / 2 - (n * delta / 2));
            strX += "," + posX;
            strY += "," + posY;
        }
        else if(cnt == 2) {
            n = Math.sqrt(lst.get(1));
            posX = (int) (width / 2 - delta - (n * delta / 2));
            posY = (int) (height / 2 - (n * delta / 2));
            strX += "," + posX;
            strY += "," + posY;
            n = Math.sqrt(lst.get(0));
            posX = (int) (posX - (4 * delta) - (n * delta));
            posY = (int) (height / 2 - (n * delta / 2));
            strX += "," + posX;
            strY += "," + posY;
        }
        else if(cnt == 3) {
            n = Math.sqrt(lst.get(2));
            posX = (int) (width / 2 - (n * delta / 2));
            posY = (int) (height / 2 - (n * delta / 2));
            strX += "," + posX;
            strY += "," + posY;
            n = Math.sqrt(lst.get(1));
            posX = (int) (posX - (4 * delta) - (n * delta));
            posY = (int) (height / 2 - (n * delta / 2));
            strX += "," + posX;
            strY += "," + posY;
            n = Math.sqrt(lst.get(0));
            posX = (int) (posX + (4 * delta) + (n * delta));
            posY = (int) (height / 2 - (n * delta / 2));
            strX += "," + posX;
            strY += "," + posY;
        }
        else if(cnt > 0) {
            int xdivs = (int) Math.sqrt(cnt) + 1;
            if(ratio > 2.8)
                xdivs += (int) xdivs / 2;
            else if(ratio > 1.8)
                xdivs += (int) xdivs / 3;
            int ydivs = (int) Math.sqrt(cnt - xdivs * xdivs) + 1;
            ydivs = (xdivs * ydivs > cnt)? (ydivs - 1) : ydivs;
            int dx = (int) width / xdivs;
            int dy = (int) height / ydivs;
            int xoff = (int) dx / 3;
            int yoff = (int) dy / 3;
            int nx = 0;
            int ny = 0;
            for(int i = 0; i < cnt; i++) {
                posX = (int) nx * dx + xoff;
                strX += "," + posX;
                posY = (int) ny * dy + yoff;
                strY += "," + posY;
                if(++nx >= xdivs) {
                    if(xoff <= 0)
                        xoff = (int) dx / 3;
                    else
                        xoff = - (int) dx / 3;
                    nx = 1;
                    if(++ny >= ydivs) {
                        if(yoff <= 0)
                            yoff = (int) dy / 3;
                        else
                            yoff = - (int) dy / 3;
                        ny = 1;
                    }
                }
            }
        }
        strX += "];\n";
        strY += "];\n";
        return str + strX + strY;
    }

    @Override
    protected void runDataLoadThread() {
        showSubTab();
    }
}
