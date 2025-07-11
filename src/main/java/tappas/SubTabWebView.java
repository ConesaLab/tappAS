/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.input.MouseButton;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.nio.file.Paths;
import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabWebView extends SubTabBase {
    // class data
    WebView web;
    WebEngine engine;
    
    public SubTabWebView(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        boolean result = super.initialize(tabBase, subTabInfo, args);
        return result;
    }
    @Override
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        if(hm.containsKey("url")) {
            processURL((String) hm.get("url"));
        }
        return obj;
    }
    
    //
    // Internal Functions
    //
    private void showSubTab() {
        web = (WebView) tabNode.getParent().lookup("#webView");        
        engine = web.getEngine();
        
        if(args.containsKey("url")) {
            String url = (String) args.get("url");
            processURL(url);
        }
        setFocusNode(web, true);
    }
    private void processURL(String url) {
        if(url.startsWith("page")) {
            String filepath = Paths.get(app.data.getAppDataFolder(), "web", url).toString();
            logger.logDebug("WebFP: " + filepath);
            String weburl;
            if(Utils.isWindowsOS()) {
                // need an additional slash when using drive letters
                weburl = "/" + filepath.replaceAll("\\\\", "/");
                logger.logInfo("fp: " + url);
            }
            else
                weburl = filepath;
            logger.logDebug("WebURL: " + weburl);
            engine.load("file://" + weburl);
        }
        else {
            String content = app.data.getFileContentFromResource("/tappas/resources/web/" + url);
            content = app.resources.replaceHTMLAllResources(content);
            engine.loadContent(content);
            web.setOnMouseClicked((event) -> {
                if(event.getButton() == MouseButton.PRIMARY) {
                    String cmd = (String) web.getEngine().executeScript("getCmdRequest()");
                    System.out.println("cmd: " + cmd);
                    if(!cmd.trim().isEmpty()) {
                        app.ctlr.showOverviewSection(cmd);
                    }
                }
            });
        }
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        showSubTab();
    }
}
