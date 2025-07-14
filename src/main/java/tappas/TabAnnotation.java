/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import tappas.DbProject.AFStatsData;

import java.util.HashMap;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class TabAnnotation extends TabBase {
    enum Panels { DETAILS, DISTRIBUTION };
    SubTabBase.SubTabInfo _subTabsInfo[] = {
        // subTabId, resId, group, title, tooltip, menuName, imgName, description
        new SubTabBase.SubTabInfo(Panels.DETAILS.name(), "AnnoDetails", "", "", "Details", "Annotation Source Details", "", "details.png", "annotation features and IDs"),
        new SubTabBase.SubTabInfo(Panels.DISTRIBUTION.name(), "AnnoDistribution", "", "", "Features Distribution", "Annotation Features Distribution", "", "distribution.png", "annotation feature distribution")
    };
    TabBase.TabDef _tabDef = new TabBase.TabDef("ASF", "Annotation Source Details", "Annotation Source Details", "Annotation Source Details", "Annotation Source Details: ", "annotationTab.png", _subTabsInfo);
    
    String selSource = "";
    String selFeature = "";
    Boolean includeReserved = false;
    HashMap<String, HashMap<String, Object>> hmGeneTransFilter;    
    HashMap<String, AFStatsData> hmFeatures = null;
    
    public TabAnnotation(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(String tabId, TabPane tpParent, HashMap<String, Object> args) {
        super.initialize(tabId, tpParent, args);
        if(args.containsKey("Source"))
            selSource = (String) args.get("Source");
        if(args.containsKey("Feature"))
            selFeature = (String) args.get("Feature");
        if(args.containsKey("Filter"))
            hmGeneTransFilter = (HashMap<String, HashMap<String, Object>>) args.get("Filter");
        _tabDef.title = "\"" + selSource + "\"";
        _tabDef.tooltip = "Annotation source '" + selSource + "' Details for Project '" + project.getProjectName() + "'";
        boolean result = initializeTab(_tabDef);
        if(result)
            openSubTab(_subTabsInfo[Panels.DETAILS.ordinal()], false);
        return result;
    }
    public HashMap<String, HashMap<String, Object>> getGeneTransFilter() {
        return hmGeneTransFilter;
    }    
    public HashMap<String, Object> getTransFilter() {
        HashMap<String, Object> hmTransFilter = new HashMap<>();
        for(String gene : hmGeneTransFilter.keySet()) {
            HashMap<String, Object> hm = hmGeneTransFilter.get(gene);
            for(String trans : hm.keySet())
                hmTransFilter.put(trans, null);
        }
        return hmTransFilter;
    }    
    
    @Override
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        if(hm.containsKey("setFeature")) {
            HashMap<String, Object> hmReqArgs = (HashMap<String, Object>) hm.get("setFeature");
            if(hmReqArgs.containsKey("Source")) {
                selSource = (String)hmReqArgs.get("Source");
                selFeature = "";
                if(hmReqArgs.containsKey("Feature"))
                    selFeature = (String)hmReqArgs.get("Feature");
            }
            if(hmReqArgs.containsKey("Filter"))
                hmGeneTransFilter = (HashMap<String, HashMap<String, Object>>) hmReqArgs.get("Filter");
            _tabDef.title = "\"" + selSource + "\"";
            _tabDef.tooltip = "Annotation source '" + selSource + "' features for project '" + project.getProjectName() + "'";
            setTabLabel(_tabDef.title, _tabDef.tooltip);
            HashMap<String, Object> hmArgs = new HashMap<>();
            hmArgs.put("Source", selSource);
            hmArgs.put("Feature", selFeature);
            hmArgs.put("Filter", hmGeneTransFilter);
            hmFeatures = null;
            if(_subTabsInfo[Panels.DETAILS.ordinal()].subTabBase != null)
                _subTabsInfo[Panels.DETAILS.ordinal()].subTabBase.refreshSubTab(hmArgs);
            if(_subTabsInfo[Panels.DISTRIBUTION.ordinal()].subTabBase != null)
                _subTabsInfo[Panels.DISTRIBUTION.ordinal()].subTabBase.refreshSubTab(hmArgs);

            // always open/select details tab
            openSubTab(_subTabsInfo[Panels.DETAILS.ordinal()], false);
        }
        return obj;
    }
    @Override
    public SubTabBase createSubTabInstance(SubTabBase.SubTabInfo info) {
        SubTabBase sub = null;
        HashMap<String, Object> subArgs = new HashMap<>();
        for(String key : args.keySet())
            subArgs.put(key, args.get(key));
        subArgs.put("Source", selSource);
        subArgs.put("Feature", selFeature);
        subArgs.put("Filter", hmGeneTransFilter);
        try {
            Panels panel = Panels.valueOf(info.subTabId);
             switch(panel) {
                case DETAILS:
                    sub = new SubTabAnnotationDetails(project);
                    break;
                case DISTRIBUTION:
                    sub = new SubTabAnnotationDistribution(project);
                    break;
            }
        }
        catch(Exception e) { 
            logger.logError("Internal program error: " + e.getMessage());
        }
        if(sub != null)
            sub.initialize(tbObj, info, subArgs);
        return sub;
    }
    @Override
    public void search(Object obj, String txt, boolean hide) {
        if(obj != null) {
            for(SubTabBase.SubTabInfo info : tabDef.subTabsInfo) {
                for(TableView tb : info.lstSearchTables) {
                    if(tb.equals(obj)) {
                        info.subTabBase.search(obj, txt, hide);
                        break;
                    }
                }
            }
        }
    }
}
