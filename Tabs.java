/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import com.sun.javafx.scene.control.behavior.TabPaneBehavior;
import com.sun.javafx.scene.control.skin.TabPaneSkin;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class Tabs extends AppObject {
    // normal tabs
    final static String TAB_AI = "tabAppInfo";
    final static String TAB_PROJECTDATA = "tabProjectData";
    final static String TAB_PROJECTDV = "tabProjectDV";
    // dynamic tabs
    final static String TAB_GENEDV = "tabGeneDV";
    final static String TAB_ANNOTATION = "tabAnnotation";
    // limit the number of gene DV tabs - users have a tendency to just keep opening them
    final int MAX_GENEDV_TABS = 12;
    
    public TabPane tpMain;
    private TabPane tpBottom;
    private Tab tabSelectedPreviously;
    private Tab tabSelected;
    
    TabAppInfo appInfo = null;
    HashMap<String, TabData> tabs = new HashMap<>();
    public ObjectProperty<Tab> draggingTab;
    public static final String TAPPAS_TAB_DD = "tappas_tab_draganddrop";
    private SearchInfo searchInfo = null;
    private ZoomInfo zoomInfo = null;
    
    // Public interface
    public Tabs(App app) {
        super(app);
    }
    public boolean initialize(TabPane main, TabPane bottom) {
        boolean result;
        
        tpMain = main;
        tpBottom = bottom;
        draggingTab = new SimpleObjectProperty<>();
        setupForDragDrop(tpMain);
        setupForDragDrop(tpBottom);
        setupTabPanes();
        result = true;
        return result;
    }
    public static boolean isProjectTab(String tabId) {
        // only non-project tab is AppInfo - change accordingly
        return !tabId.equals(TAB_AI);
    }
    
    //
    // Zoom Functions
    //
    
    public void zoomSet(ZoomInfo zoomInfo) {
        this.zoomInfo = zoomInfo;
    }
    public void zoomIn() {
        if(zoomInfo != null && zoomInfo.obj != null) {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("zoomIn", zoomInfo.obj);
            zoomInfo.tab.processRequest(hm);
        }
    }
    public void zoomOut() {
        if(zoomInfo != null && zoomInfo.obj != null) {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("zoomOut", zoomInfo.obj);
            zoomInfo.tab.processRequest(hm);
            
        }
    }
    public void zoomFit() {
        if(zoomInfo != null && zoomInfo.obj != null) {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("zoomFit", zoomInfo.obj);
            zoomInfo.tab.processRequest(hm);
        }
    }
    
    //
    // Search Functions
    //
    
    public boolean isSearchTableFocused() {
        boolean result = false;
        if(searchInfo != null) {
            // we need to make sure the focus is not in the selection check box or another part of the table
            boolean fnd = false;
            Node node = Tappas.getScene().getFocusOwner();
            if(node != null) {
                Parent parent = node.getParent();
                while(parent != null) {
                    if(parent.equals(searchInfo.obj)) {
                        result = true;
                        break;
                    }
                    parent = parent.getParent();
                }
            }
        }
        return result;
    }
    public void searchSet(SearchInfo searchInfo) {
        this.searchInfo = null;
        app.ctlr.fxdc.searchSet(searchInfo == null, searchInfo == null? "" : searchInfo.txt, searchInfo == null? false : searchInfo.hide);
        this.searchInfo = searchInfo;
    }
    public void search(String txt, boolean hide) {
        if(searchInfo != null && searchInfo.tab != null) {
            searchInfo.txt = txt;
            searchInfo.hide = hide;
            System.out.println("tabs search called: " + txt + ", hide: " + hide);
            searchInfo.tab.search(searchInfo.obj, txt, hide);
        }
        else
            System.out.println("Tabs has no object to execute the search!");
    }
    
    //
    // Tab Pane Functions
    //
    
    private void setupTabPanes() {
        // only set the context menu for the active tab to avoid user getting menu by clicking on non-active tabs
        if(tpMain.getSelectionModel().getSelectedItem() != null)
            tpMain.getSelectionModel().getSelectedItem().setContextMenu((ContextMenu)tpMain.getSelectionModel().getSelectedItem().getUserData());
        tpMain.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Tab> ov, Tab oldValue, Tab newValue) -> {
            onTabChanged(oldValue, newValue);
        });
        tpMain.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue)
            {
                if(tpMain.getSelectionModel().getSelectedItem() != null) {
                    TabBase tb = getTabObject(tpMain.getSelectionModel().getSelectedItem().getId());
                    onTabChanged(null, tpMain.getSelectionModel().getSelectedItem());
                }
            }
        });
        if(tpBottom.getSelectionModel().getSelectedItem() != null)
            tpBottom.getSelectionModel().getSelectedItem().setContextMenu((ContextMenu)tpBottom.getSelectionModel().getSelectedItem().getUserData());
        tpBottom.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Tab> ov, Tab oldValue, Tab newValue) -> {
            onTabChanged(oldValue, newValue);
        });
        tpBottom.focusedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) -> {
            if(newValue)
            {
                onTabChanged(null, tpBottom.getSelectionModel().getSelectedItem());
            }
        });
    }
    public double getBottomTPWidth() {
        double w = 0.0;
        if(tpBottom != null)
            w = tpBottom.getWidth();
         return w;
    }
    public double getBottomTPHeight() {
        double h = 0.0;
        if(tpBottom != null)
            h = tpBottom.getHeight();
         return h;
    }
    public TabPane getTabPane(String tabId) {
        TabPane tp = null;
        for(TabData td : tabs.values()) {
            if(td.id.equals(tabId)) {
                tp = td.pane;
                break;
            }
        }
        return tp;
    }
    public void changeToNotFocused(TabPane tpSelected) {
        String id;
        if(tpSelected.getId().equals(tpMain.getId())) {
            id = getSelectedBottomTabId();
            if(!id.isEmpty()) {
                System.out.println("Changing color for selected tab in tpBottom: " + id);
                TabBase tb = getTabBase(id);
                if(tb != null)
                    tb.changeToNotFocused();
            }
        }
        else if(tpSelected.getId().equals(tpBottom.getId())) {
            id = getSelectedMainTabId();
            if(!id.isEmpty()) {
                System.out.println("Changing color for selected tab in tpMain: " + id);
                TabBase tb = getTabBase(id);
                if(tb != null)
                    tb.changeToNotFocused();
            }
        }
    }
    public String getSelectedMainTabId() {
        String id = "";
        Tab tab = tpMain.getSelectionModel().getSelectedItem();
        for(TabData td : tabs.values()) {
            if(td.obj.getTab() == tab) {
                id = td.id;
                break;
            }
        }
        return id;
    }
    public String getSelectedBottomTabId() {
        String id = "";
        Tab tab = tpBottom.getSelectionModel().getSelectedItem();
        for(TabData td : tabs.values()) {
            if(td.obj.getTab() == tab) {
                id = td.id;
                break;
            }
        }
        return id;
    }
    public FloatWindow floatSubTab(Node node, String tabId, SubTabBase.SubTabInfo subTabInfo) {
        FloatWindow dlgFloat = null;
        for(TabData td : tabs.values()) {
            if(td.id.equals(tabId)) {
                dlgFloat = new FloatWindow(project, Tappas.getWindow());
                dlgFloat.createFloatingWindow(node, subTabInfo.title);
                dlgFloat.show(subTabInfo);
                break;
            }
        }
        return dlgFloat;
    }
    
    //
    // Project Related Functions
    //
    
    public ArrayList<Project.ProjectDef> getActiveProjects() {
        ArrayList<Project.ProjectDef> plst = new ArrayList<>();
        for(String key : tabs.keySet()) {
            if(key.startsWith(TAB_PROJECTDATA)) {
                TabData td = tabs.get(key);
                plst.add(td.obj.project.getDef());
            }
        }
        return plst;
    }
    public ArrayList<TabBase> getActiveProjectsTabBase() {
        ArrayList<TabBase> plst = new ArrayList<>();
        for(String key : tabs.keySet()) {
            // for a project to be opened, TAB_PROJECTDATA* must be opened
            if(key.startsWith(TAB_PROJECTDATA)) {
                TabData td = tabs.get(key);
                plst.add(td.obj);
            }
        }
        return plst;
    }
    public void closeProjectTabs(Project.ProjectDef def) {
        for(Iterator<HashMap.Entry<String, TabData>> iterator = tabs.entrySet().iterator(); iterator.hasNext();) {
            HashMap.Entry<String, TabData> hm = iterator.next();
            String key = hm.getKey();
            if(key.contains(def.id)) {
                System.out.println("closing project tab " + key);
                TabData td = tabs.get(key);
                if(td.obj.onCloseRequestOK()) {
                    Tab tab = td.obj.getTab();
                    tab.setClosable(true);
                    td.obj.closeAllSubTabs();

                    // this does not seem to trigger the tab's set on close event
                    TabPaneBehavior behavior = ((TabPaneSkin) tab.getTabPane().getSkin()).getBehavior();
                    behavior.closeTab(tab);

                    // just in case something went wrong at the OnClosed tab event
                    try { tab.getTabPane().getTabs().remove(tab); } catch(Exception e) {}
                    iterator.remove();
                }
                else
                    break;
            }
        }
    }    
    
    //
    // Close Tab Functions
    //
    
    public void closeAllGeneDataVizTabs(Project project, String id) {
        long tstart = System.nanoTime();
        ArrayList<String> lstIds = new ArrayList<>();
        String tabId = TAB_GENEDV + "_" + id + "_";
        for(String key : tabs.keySet()) {
            if(key.startsWith(tabId))
                lstIds.add(key);
        }
        for(String gdvid : lstIds)
            closeTab(gdvid);
        long tend = System.nanoTime();
        long duration = (tend - tstart)/1000000;
        System.out.println("closeAllGeneDataVizTabs Total time: " + duration + " ms");
    }
    public void closeTab(String id) {
        System.out.println("closeTab('" + id + "'):");
        for(String key : tabs.keySet()) {
            if(key.equals(id)) {
                TabData td = tabs.get(key);
                if(id.startsWith(TAB_PROJECTDATA)) {
                    System.out.println("...closing project");
                    td.obj.app.ctlr.closeProject(td.obj.project.getDef());
                }
                else {
                    System.out.println("...closing tab");
                    if(td.obj.onCloseRequestOK()) {
                        Tab tab = td.obj.getTab();
                        tab.setClosable(true);
                        td.obj.closeAllSubTabs();

                        // this does not seem to trigger the tab's set on close event
                        TabPaneBehavior behavior = ((TabPaneSkin) tab.getTabPane().getSkin()).getBehavior();
                        behavior.closeTab(tab);

                        // just in case something went wrong at the OnClosed tab event
                        try { tab.getTabPane().getTabs().remove(tab); } catch(Exception e) {}
                        tabs.remove(key);
                    }
                }
                break;
            }
        }
    }
    // will not close a tab if running a task and user cancels closing
    // partial closing of tabs will affect caller function - return flag(s)?!!!
    public void closeTabs(String id) {
        System.out.println("closeTabs('" + id + "'):");
        for(Iterator<HashMap.Entry<String, TabData>> iterator = tabs.entrySet().iterator(); iterator.hasNext();) {
            HashMap.Entry<String, TabData> hm = iterator.next();
            String key = hm.getKey();
            if(key.startsWith(id)) {
                System.out.println("...closing tab " + key);
                TabData td = tabs.get(key);
                if(td.obj.onCloseRequestOK()) {
                    Tab tab = td.obj.getTab();
                    tab.setClosable(true);
                    td.obj.closeAllSubTabs();

                    // this does not seem to trigger the tab's set on close event
                    TabPaneBehavior behavior = ((TabPaneSkin) tab.getTabPane().getSkin()).getBehavior();
                    behavior.closeTab(tab);

                    // just in case something went wrong at the OnClosed tab event
                    try { tab.getTabPane().getTabs().remove(tab); } catch(Exception e) {}
                    iterator.remove();
                }
            }
        }
    }
    // will not close a tab if running a task and user cancels closing
    // partial closing of tabs will affect caller function!
    public void closeAllTabs() {
        System.out.println("closeAllTabs():");
        for(Iterator<HashMap.Entry<String, TabData>> iterator = tabs.entrySet().iterator(); iterator.hasNext();) {
            HashMap.Entry<String, TabData> hm = iterator.next();
            String key = hm.getKey();
            System.out.println("...closing tab " + key);
            TabData td = tabs.get(key);
            if(td.obj.onCloseRequestOK()) {
                Tab tab = td.obj.getTab();
                tab.setClosable(true);
                td.obj.closeAllSubTabs();

                // this does not seem to trigger the tab's set on close event
                TabPaneBehavior behavior = ((TabPaneSkin) tab.getTabPane().getSkin()).getBehavior();
                behavior.closeTab(tab);

                // just in case something went wrong at the OnClosed tab event
                try { tab.getTabPane().getTabs().remove(tab); } catch(Exception e) {}
                iterator.remove();
            }
            else
                break;
        }
    }
    public void onCloseRequest(Event event) {
        Tab tab = (Tab) event.getSource();
        System.out.println("OnCloseRequest: " + tab.getId() + " : " + tab.getTabPane().getId());
    }
    
    //
    // Open Tab Functions
    //
    
    public boolean isTabOpened(String id) {
        boolean result = false;
        for(TabData td : tabs.values()) {
            if(td.id.equals(id)) {
                result = true;
                break;
            }
        }
        return result;
    }
    public void openTab(String id, Project project, HashMap<String, Object> args) {
        switch(id) {
            case TAB_AI:
                openTabAI(project, args);
                break;
            default:
                if(id.startsWith(TAB_PROJECTDATA))
                    openTabProject(project, id, args);
                else if(id.startsWith(TAB_PROJECTDV))
                    openTabProjectDV(project, id, args);
                else if(id.startsWith(TAB_GENEDV))
                    openTabGeneDataViz(project, id, args);
                break;
        }
    }
    private void openTabAI(Project project, HashMap<String, Object> args) {
        if(!selectTab(TAB_AI)) {
            TabAppInfo tab = new TabAppInfo(project);
            if(tab.initialize(TAB_AI, tpBottom, (args == null? new HashMap<>() : args))) {
                TabData tabData = new TabData(tab, tpBottom, TAB_AI, tab.getHeaderLabel());
                addTabData(TAB_AI, tabData);
                appInfo = tab;
            }
            else
                try { if(tab.tab != null) tpBottom.getTabs().remove(tab.tab); } catch(Exception e) {}
        }
    }
    private void openTabProjectDV(Project project, String tabId, HashMap<String, Object> args) {
        if(!selectTab(tabId)) {
            TabProjectDataViz tab = new TabProjectDataViz(project);
            if(tab.initialize(tabId, tpBottom, (args == null? new HashMap<>() : args))) {
                TabData tabData = new TabData(tab, tpBottom, tabId, tab.getHeaderLabel());
                addTabData(tabId, tabData);
            }
            else
                try { if(tab.tab != null) tpBottom.getTabs().remove(tab.tab); } catch(Exception e) {}
        }
        else {
            // pass request to open specific panel
            TabBase tabBase = getTabBase(tabId);
            if(tabBase != null)
                tabBase.processRequest(args == null? new HashMap<>() : args);
        }
    }
    private void openTabProject(Project project, String tabId, HashMap<String, Object> args) {
        if(!selectTab(tabId)) {
            TabProjectData tab = new TabProjectData(project);
            if(tab.initialize(tabId, tpMain, (args == null? new HashMap<>() : args))) {
                TabData tabData = new TabData(tab, tpMain, tabId, tab.getHeaderLabel());
                addTabData(tabId, tabData);
            }
            else
                try { if(tab.tab != null) tpMain.getTabs().remove(tab.tab); } catch(Exception e) {}
        }
        else {
            // pass request to open specific panel
            TabBase tabBase = getTabBase(tabId);
            if(tabBase != null)
                tabBase.processRequest(args == null? new HashMap<>() : args);
        }
    }
    public void openTabAnnotDetails(Project project, String source, String feature,
                                    HashMap<String, HashMap<String, Object>> hmGeneTransFilter) {
        HashMap<String, Object> args = new HashMap<>();
        args.put("Source", source);
        args.put("Feature", feature);
        args.put("Filter", hmGeneTransFilter);
        if(!selectTab(TAB_ANNOTATION)) {
            TabAnnotation tab = new TabAnnotation(project);
            if(tab.initialize(TAB_ANNOTATION, tpBottom, args)) {
                TabData tabData = new TabData(tab, tpBottom, TAB_ANNOTATION, tab.getHeaderLabel());
                addTabData(TAB_ANNOTATION, tabData);
            }
            else
                try { if(tab.tab != null) tpBottom.getTabs().remove(tab.tab); } catch(Exception e) {}
        }
        else {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("setFeature", args);
            TabBase tb = getTabObject(TAB_ANNOTATION);
            if(tb != null)
                tb.processRequest(hm);
        }
    }
    public void openTabGeneDataViz(Project project, String tabId, HashMap<String, Object> hmArgs) {
        if(!selectTab(tabId)) {
            int cnt = getTabDVCount();
            if(cnt < MAX_GENEDV_TABS) {
                TabGeneDataViz tab = new TabGeneDataViz(project);
                if(tab.initialize(tabId, tpBottom, hmArgs)) {
                    TabData tabData = new TabData(tab, tpBottom, tabId, tab.getHeaderLabel());
                    addTabData(tabId, tabData);
                }
                else
                    try { if(tab.tab != null) tpBottom.getTabs().remove(tab.tab); } catch(Exception e) {}
            }
            else
                app.ctls.alertWarning("Gene Data Visualization Tabs Limit Exceeded", "Please close some Gene Data Visualization tabs.");
        }
        else {
            // pass request to open specific panel
            TabBase tabBase = getTabBase(tabId);
            if(tabBase != null)
                tabBase.processRequest(hmArgs == null? new HashMap<>() : hmArgs);
        }
    }
    // set hmTrans to null for all annotated isoforms of given gene
    // srcCaption describes where the isoforms come from, if filtered, e.g. "Tanscript Group 'my group' isoforms"
    public void openTabGeneDataViz(Project project, String id, String gene, HashMap<String, Object> hmTrans, String caption, HashMap<String, Object> hmArgs) {
        String tabId = TAB_GENEDV + "_" + id + "_" + gene;
        if(!selectTab(tabId)) {
            int cnt = getTabDVCount();
            if(cnt < MAX_GENEDV_TABS) {
                HashMap<String, Object> args = new HashMap<>();
                args.put("gene", gene);
                if(hmTrans != null && !hmTrans.isEmpty())
                    args.put("trans", hmTrans);
                if(caption != null && !caption.isEmpty())
                    args.put("caption", caption);
                if(hmArgs != null) {
                    for(String key : hmArgs.keySet())
                        args.put(key, hmArgs.get(key));
                }
                TabGeneDataViz tab = new TabGeneDataViz(project);
                if(tab.initialize(tabId, tpBottom, args)) {
                    TabData tabData = new TabData(tab, tpBottom, tabId, tab.getHeaderLabel());
                    addTabData(tabId, tabData);
                }
                else
                    try { if(tab.tab != null) tpBottom.getTabs().remove(tab.tab); } catch(Exception e) {}
            }
            else
                app.ctls.alertWarning("Data Visualization Tabs Limit Exceeded", "Please close some Data Visualization tabs.");
        }
        else {
            // pass request to open specific panel
            TabBase tabBase = getTabBase(tabId);
            if(tabBase != null)
                tabBase.processRequest(hmArgs == null? new HashMap<>() : hmArgs);
        }
    }
    
    //
    // Tab Handling Functions
    //
    
    public TabBase getTabBase(String tabId) {
        TabBase tabBase = null;
        for(TabData td : tabs.values()) {
            if(td.id.equals(tabId)) {
                tabBase = td.obj;
                break;
            }
        }
        return tabBase;
    }
    public String getSelectedTabId() {
        String id = "";
        Tab tab = (tabSelected != null)? tabSelected : tabSelectedPreviously;
        if(tab != null) {
            for(TabData td : tabs.values()) {
                if(td.obj.getTab() == tab) {
                    id = td.id;
                    break;
                }
            }
        }
        return id;
    }
    public boolean selectTab(String id) {
        boolean selected = false;
        if(tabs.containsKey(id)) {
            selected = true;
            TabData td = tabs.get(id);
            if(td.dlg == null) {
                TabPane tp = td.pane;
                for(Tab tab: tp.getTabs()) {
                    if(tab.getId().equals(id)) {
                        tp.requestFocus();
                        tp.getSelectionModel().select(tab);
                        // if tab was loaded from pipeline with loadingTabs set
                        // then content was not displayed even if it is the selected tab
                        // we must call onSelect here - it will not do anything if already shown
                        ((TabBase)td.obj).onSelect(true);
                        System.out.println("UTLM from Tabs::selectTab()");
                        app.ctlr.updateTopLevelMenus(id, false);
                        break;
                    }
                }
            }
            else {
                System.out.println("Got a dialog for tab " + id);
            }
        }
        return selected;
    }

    //
    // Internal Tab Handling Functions
    //
    
    private TabBase getTabObject(String id) {
        TabBase tab = null;
        for(String key : tabs.keySet()) {
            if(key.equals(id)) {
                tab = tabs.get(key).obj;
                break;
            }
        }
        return tab;
    }
    private int getTabDVCount() {
        int cnt = 0;
        for(String key : tabs.keySet()) {
            if(key.startsWith(TAB_GENEDV))
                cnt++;
        }
        return cnt;
    }
    private void addTabData(String tabId, TabData tabData) {
        tabs.put(tabId, tabData);
        app.ctlr.updateTopLevelMenus(tabId, false);
    }
    private TabBase getAppTab(String id) {
        TabBase tab = null;
        if(tabs.containsKey(id))
            tab = tabs.get(id).obj;
        return tab;
    }
    private int getDropTabIndex(TabPane tp, Tab dropTab, Point2D ptScreen) {
        int idx = -1;
        int wAdjust = 30;
        int tabIdx = 0;
        double minX, width;
        Bounds bl, bb;
        for(Tab tab : tp.getTabs()) {
            if(tab != dropTab) {
                for(TabData td : tabs.values()) {
                    if(td.obj.tab == tab && td.lblHeader != null) {
                        bl = td.lblHeader.localToScreen(td.lblHeader.getBoundsInLocal());
                        if(bl != null) {
                            minX = bl.getMinX() - wAdjust;
                            width = bl.getWidth() + wAdjust;
                            bb = new BoundingBox(minX, bl.getMinY() - 10, bl.getMinZ(), width, bl.getHeight() + 20, bl.getDepth());
                            if(bb.contains(ptScreen)) {
                                idx = tabIdx;
                                break;
                            }
                        }
                        break;
                    }
                }
                tabIdx++;
            }
        }
        return idx;
    }
    private void setupForDragDrop(TabPane tp) {
        tp.setOnDragOver((DragEvent event) -> {
            final Dragboard dragboard = event.getDragboard();
            if (dragboard.hasString()
                    && TAPPAS_TAB_DD.equals(dragboard.getString())
                    && draggingTab.get() != null)
            {
                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            }
        });
        tp.setOnDragDropped((DragEvent event) -> {
            final Dragboard dragboard = event.getDragboard();
            if(dragboard.hasString() && TAPPAS_TAB_DD.equals(dragboard.getString())
                    && draggingTab.get() != null && draggingTab.get().getClass().toString().toLowerCase().contains("javafx.scene.control.tab")) {
                Point2D ptScreen = new Point2D(event.getScreenX(), event.getScreenY());
                final Tab tab = draggingTab.get();
                int idx = getDropTabIndex(tp, tab, ptScreen);
                if(tab.getTabPane() != tp || idx != tab.getTabPane().getTabs().indexOf(tab)) {
                    if(idx == -1 && tab.getTabPane() == tp)
                        System.out.println("Ignore ambigous tab drop request within same tabPane");
                    else {
                        tab.getTabPane().getTabs().remove(tab);
                        if(idx < 0)
                            tp.getTabs().add(tab);
                        else
                            tp.getTabs().add(idx, tab);
                        
                        // tab was dropped, update tab accordingly
                        for(TabData td : tabs.values()) {
                            if(td.obj.tab == tab) {
                                td.pane = tp;
                                td.lblHeader = null;
                                TabBase tb = getTabObject(tab.getId());
                                tb.tabDropped(td);
                                break;
                            }
                        }
                        tp.getSelectionModel().select(tab);
                    }
                }
                event.setDropCompleted(true);
                draggingTab.set(null);
                event.consume();
            }
        });
    }
    // this if for the tabs which contain the actual sub tabs users work with
    // this only works within the tab pane itself so users switching tabs 
    // in different panes is not captured here
    // may want to remove completely and allow the global control level focus handle it all
    private void onTabChanged(Tab oldTab, Tab newTab) {
        tabSelectedPreviously = oldTab;
        tabSelected = newTab;
        if(oldTab != null) {
            oldTab.setContextMenu(null);
            System.out.println("Tab Selection changed from " + oldTab.getId());
        }
        if(newTab != null) {
            System.out.println("Tab Selection changed to " + newTab.getId());
            newTab.setContextMenu((ContextMenu)newTab.getUserData());
        }
        app.ctlr.focusChanged(newTab == null? null : (Node) newTab.getTabPane());
    }
    
    //
    // Log Functions
    //
    
    public void clearLogDisplay() {
        if(appInfo != null) {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("setText", "");
            appInfo.processRequest(hm);
        }
    }
    public void updateLogDisplay(String msg) {
        if(appInfo != null) {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("appendText", msg);
            appInfo.processRequest(hm);
        }
    }
    
    // Data Classes
    public static class TabData {
        public TabBase obj;
        public TabPane pane;
        public String id;
        public Label lblHeader;
        public Dialog dlg;
 
        public TabData(TabBase obj, TabPane pane, String id, Label lblHeader) {
            this.obj = obj;
            this.pane = pane;
            this.id = id;
            this.dlg = null;
            this.lblHeader = lblHeader;
        }
    }
    public static class SubTabStateInfo {
        String id, state;
        public SubTabStateInfo(String id, String state) {
            this.id = id;
            this.state = state;
        }
    }
    public static class TabStateInfo {
        public enum TabPane { Top, Bottom };
        
        TabPane pane;
        String id, state;
        ArrayList<SubTabStateInfo> lstSubTabs;
        public TabStateInfo(TabPane pane, String id, String state, ArrayList<SubTabStateInfo> lstSubTabs) {
            this.pane = pane;
            this.id = id;
            this.state = state;
            this.lstSubTabs = (lstSubTabs == null)? new ArrayList<>() : lstSubTabs;
        }
    }
    public static class SearchInfo {
        public String txt;
        public boolean hide;
        public TabBase tab;
        public Object obj;
        public SearchInfo(TabBase tab, Object obj, String txt, boolean hide) {
            this.tab = tab;
            this.obj = obj;
            this.txt = txt;
            this.hide = hide;
        }
    }
    public static class ZoomInfo {
        public TabBase tab;
        public Object obj;
        public ZoomInfo(TabBase tab, Object obj) {
            this.tab = tab;
            this.obj = obj;
        }
    }
    public static class ProjectState {
        public static final String ID_PARAM = "projectId";
        public static final String TOPTAB_PARAM = "tT";
        public static final String BOTTOMTAB_PARAM = "bT";

        String id, name;
        ArrayList<TabStateInfo> lstTabs;
        
        public ProjectState(String id, String name) {
            this.id = id;
            this.name = name;
            this.lstTabs = new ArrayList<>();
        }
        public boolean saveToFile(String filepath) {
            HashMap<String, String> hmParams = new HashMap<>();
            hmParams.put(ID_PARAM, id);
            int tn = 1;
            for(TabStateInfo tsi : lstTabs) {
                String tabPane = tsi.pane.name();
                hmParams.put(tabPane + tn, tsi.id);
                if(!tsi.state.isEmpty())
                    hmParams.put(tabPane + tn + "_S", tsi.state);
                int stn = 1;
                for(SubTabStateInfo stsi : tsi.lstSubTabs) {
                    hmParams.put(tabPane + tn + "_" + stn, stsi.id);
                    if(!stsi.state.isEmpty())
                        hmParams.put(tabPane + tn + "_" + stn + "_S", stsi.state);
                    stn++;
                }
                tn++;
            }
            return Utils.saveParams(hmParams, filepath);
        }
        public boolean loadFromFile(String filepath) {
            boolean result = false;
            HashMap<String, String> hmParams = new HashMap<>();
            Utils.loadParams(hmParams, filepath);
            String settings;
            lstTabs = new ArrayList<>();
            if(!hmParams.isEmpty()) {
                if(hmParams.containsKey(ID_PARAM))
                    id = hmParams.get(ID_PARAM).trim();
                for(int tn = 1; ; tn++) {
                    // check both the top and bottom pane
                    int cnt = 0;
                    for(TabStateInfo.TabPane tp : TabStateInfo.TabPane.values()) {
                        // check if we have the next tab definition
                        String tabPane = tp.name();
                        if(hmParams.containsKey(tabPane + tn)) {
                            cnt++;
                            settings = "";
                            if(hmParams.containsKey(tabPane + tn + "_S"))
                                settings = hmParams.get(tabPane + tn + "_S");
                            ArrayList<SubTabStateInfo> lstSubTabs = new ArrayList<>();
                            TabStateInfo tsi = new TabStateInfo(tp, hmParams.get(tabPane + tn), settings, lstSubTabs);
                            lstTabs.add(tsi);

                            // now get all  the sub tabs for this tab
                            String state;
                            for(int stn = 1; ; stn++) {
                                if(hmParams.containsKey(tabPane + tn + "_" + stn)) {
                                    if(hmParams.containsKey(tabPane + tn + "_" + stn + "_S"))
                                        state = hmParams.get(tabPane + tn + "_" + stn + "_S");
                                    else
                                        state = "";
                                    lstSubTabs.add(new SubTabStateInfo(hmParams.get(tabPane + tn + "_" + stn), state));
                                }
                                else
                                    break;
                            }
                        }
                    }
                    
                    if(cnt == 0)
                        break;
                }
                
                result = true;
            }
            return result;
        }
    }
    public class FloatWindow extends DlgBase {
        Node node;
        SubTabBase.SubTabInfo subTabInfo = null;
        public FloatWindow(Project project, Window window) {
            super(project, window);
        }
        public void show(SubTabBase.SubTabInfo subTabInfo) {
            this.subTabInfo = subTabInfo;
            dialog.show();
        }
        @Override
        public boolean createFloatingWindow(Node node, String title) {
            this.node = node;
            return super.createFloatingWindow(node, title);
        }
        @Override
        protected void onClose() {
            // release subTabInfo if not docking
            if(!subTabInfo.dock)
                subTabInfo.release();
            dialog.close();        
        }
    }
}
