/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.control.TabPane;

import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class TabAppInfo extends TabBase {
    enum Panels { OVERVIEW, LOG, TECHINFO };
    SubTabBase.SubTabInfo _subTabsInfo[] = {
        // subTabId, resId, group, title, tooltip, menuName, imgName, description
        new SubTabBase.SubTabInfo(Panels.OVERVIEW.name(), "WebView", "Documentation", "", "Overview", "Application Overview", "", "appInfoTab.png", "Application overview"),
        new SubTabBase.SubTabInfo(Panels.LOG.name(), "Log", "Log", "", "Log", "Application Log", "", "log.png", "Application log"),
        new SubTabBase.SubTabInfo(Panels.TECHINFO.name(), "TechInfo", "Application Information", "", "Technical Information", "Application Technical Information", "", "definition.png", "application technical information")
    };
    TabBase.TabDef _tabDef = new TabBase.TabDef("AI", "Application Information", "App Info", "Application Information", "Application Information: ", "appInfoTab.png", _subTabsInfo);
    public TabAppInfo(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(String tabId, TabPane tpParent, HashMap<String, Object> args) {
        super.initialize(tabId, tpParent, args);
        boolean result = initializeTab(_tabDef);
        if(result)
        {
            openSubTab(_subTabsInfo[Panels.OVERVIEW.ordinal()], true);
            openSubTab(_subTabsInfo[Panels.LOG.ordinal()], false);
        }
        return result;
    }
    @Override
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        if(hm.containsKey("setText")) {
            if(_subTabsInfo[Panels.LOG.ordinal()].subTabBase != null)
                obj = _subTabsInfo[Panels.LOG.ordinal()].subTabBase.processRequest(hm);
        }
        else if(hm.containsKey("appendText")) {
            if(_subTabsInfo[Panels.LOG.ordinal()].subTabBase != null)
                obj = _subTabsInfo[Panels.LOG.ordinal()].subTabBase.processRequest(hm);
        }
        else if(hm.containsKey("panels")) {
            HashMap<String, Object> hmReqArgs = (HashMap<String, Object>)hm.get("panels");
            String[] panels = ((String)hmReqArgs.get("panels")).split(";");
            for(String panelName : panels)
                openPanels(panelName);
            if(hmReqArgs.containsKey("url")) {
                if(tabDef.subTabsInfo[Panels.OVERVIEW.ordinal()].subTabBase != null)
                    obj = tabDef.subTabsInfo[Panels.OVERVIEW.ordinal()].subTabBase.processRequest(hmReqArgs);
            }
        }
        return obj;
    }
    @Override
    public SubTabBase createSubTabInstance(SubTabBase.SubTabInfo info) {
        SubTabBase sub = null;
        HashMap<String, Object> hmSubArgs = new HashMap<>();
        for(String key : args.keySet())
            hmSubArgs.put(key, args.get(key));
        try {
            Panels panel = Panels.valueOf(info.subTabId);
             switch(panel) {
                case OVERVIEW:
                    sub = new SubTabWebView(project);
                    hmSubArgs.put("url", "pageAppTips.html");
                    break;
                case LOG:
                    sub = new SubTabLog(project);
                    break;
                case TECHINFO:
                    sub = new SubTabTechInfo(project);
                    break;
            }
        }
        catch(Exception e) { 
            logger.logError("Internal program error: " + e.getMessage());
        }
        if(sub != null)
            sub.initialize(tbObj, info, hmSubArgs);
        return sub;
    }
}
