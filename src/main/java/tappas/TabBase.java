/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import java.util.HashMap;
import java.util.Set;

// TODO: since I don't use the tab text (use a node in the tab header that allows for drag/drop) the menu (when too many shown) shows blanks
//       need to use text and still allow drag/drop somehow or figure out a way for menu to work - override menu drop down functionality

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class TabBase extends AppObject {
    final int BTN_IMGSIZE = 22;

    Tab tab;
    Tabs tabs;
    TabDef tabDef;
    TabBase tbObj;
    Label lblHeader;
    Pane paneTabHeader, panePI;
    public TabPane tpParent, tpSubTabs;
    Node waitNode;
    GridPane grdTabMain, waitGrid;
    HashMap<String, String> params = new HashMap<>();

    SubTabPlus subTabPlus;
    SubTabBase.SubTabInfo subTabPlusInfo = new SubTabBase.SubTabInfo("Base_Plus", "Plus", "", "", "+", "Click to open additional tabs...", "", "", "");

    String tabId;
    Thread thread;
    Process process;
    boolean run = false;
    boolean threadResult = false;
    String threadMsgError = "";
    HashMap<String, Object> args;
    final Image imgTab = new Image(getClass().getResourceAsStream("/tappas/images/tab.png"));

    // all tabs can use tabs and app - depending on the tab: project and/or transGroup may be null
    public TabBase(Project project) {
        super(project, null);
        this.tabs = app.tabs;
    }
    public boolean initialize(String tabId, TabPane tpParent, HashMap<String, Object> args) {
        this.tbObj = this;
        this.tabId = tabId;
        this.tpParent = tpParent;
        this.args = args;
        return false;
    }
    public String getTabName() { return tabDef.name; }
    public String getTabTitle() { return tabDef.title; }
    public void updateTabTitle() {
        // lblHeader
        tab.setText(tabDef.title + "  ");
    }
    public String getShortName(String name) { return name.length() > 25? name.substring(0, 25) + "...": name; }
    public SubTabBase.SubTabInfo[] getSubTabsInfo() { return tabDef.subTabsInfo; }
    
    public boolean initializeTab(TabDef tabDef) {
        boolean result = false;
        this.tabDef = tabDef;
        try {
            // create tab control with tab pane
            tab = createTab();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tappas/tabs/Template.fxml"));
            Node tabNode = loader.load();
            tab.setContent(tabNode);
            grdTabMain = (GridPane) tabNode.lookup("#grdTabMain");
            tab.setId(tabId);
            tab.setOnSelectionChanged((Event t) -> {
                // keep in mind this event won't trigger if the sub tab is already selected
                // in cases when coming from another tab pane or external window
                boolean selected = tab.isSelected();
                if(selected) {
                    System.out.println("UTLM from TabBase::setOnSelectionChanged");
                    setTabStyle();
                    app.ctlr.updateTopLevelMenus(tabId, false);
                }
                onSelect(selected);
            });
            
            if(tpParent.getTabs().add(tab)) {
                tpParent.getSelectionModel().select(tab);

                // find the proper tab label and setup for drag/drop
                // don't abort if it fails, not a critical function
                setTabLblHeader(true);

                // setup subtabs tabPane
                tpSubTabs = (TabPane) tabNode.lookup("#tpSubTabs");
                if(project != null && Tabs.isProjectTab(tabId)) {
                    int num = project.getProjectNumber();
                    tpSubTabs.getStyleClass().add("subTabHolder" + num);
                }
                setTabStyle();
                app.ctlr.updateTopLevelMenus(tabId, false);
                result = true;
            }
            else
                logger.logError("Unable to add tab '" + tabDef.title + "'\n");
        } catch(Exception e) {
            logger.logError("Unable to create " + tabDef.title + " tab: " + e.getMessage() + "\n");
        }
        return result;
    }
    // get tab lblHeader control in the tabPane and setOnDragDetected if requested
    private boolean setTabLblHeader(boolean setOnDragDetected) {
        boolean result = false;
        lblHeader = null;

        // search all the tab labels in the tabPane
        Set<Node> items = tpParent.lookupAll(".tab-label");
        if(items.size() > 0) {
            Label lbl = null;
            for(Node node : items) {
                lbl = ((Label)node);
                Node parent = node.getParent();
                while((parent != null && lblHeader == null)) {
                    // check if this is the tabPaneSkin - it's the only one with the tabId
                    // WARNING: it is possible that Java might change the way this works - change approach if needed
                    //System.out.println("Parent class: " + parent.getClass().getName() + ", " + parent.getClass().getSimpleName());
                    if(parent.getClass().getName().equals("com.sun.javafx.scene.control.skin.TabPaneSkin$TabHeaderSkin")) {
                        // check that id matches this tab's id
                        if(parent.getId() != null && parent.getId().equals(tabId)) {
                            // found it, set to return label and setOnDragDetected if requested 
                            lblHeader = lbl;
                            result = true;
                            if(setOnDragDetected) {
                                lblHeader.setOnDragDetected((MouseEvent event) -> {
                                    Dragboard dragboard = lblHeader.startDragAndDrop(TransferMode.MOVE);
                                    ClipboardContent clipboardContent = new ClipboardContent();
                                    clipboardContent.putString(Tabs.TAPPAS_TAB_DD);
                                    dragboard.setContent(clipboardContent);
                                    dragboard.setDragView(imgTab);
                                    tabs.draggingTab.set(tab);
                                    event.consume();
                                });
                            }

                            // all done
                            break;
                        }
                    }
                    parent = parent.getParent();
                }
                
                // check if found
                if(lblHeader != null)
                    break;
            }
        }
        return result;
    }
    // tab was dropped (drag/drop)
    public void tabDropped(Tabs.TabData td) {
        // update the parent tabPane, it could have changed
        tpParent = td.pane;
        System.out.println("tabDropped in tabPane: " + tpParent.getId());
        
        // find the proper tab label and setup for drag/drop
        setTabLblHeader(true);
        td.lblHeader = lblHeader;
    }
    private void setTabStyle() {
        // this is where we can add changes to the tab based on project number
        int num = -1;
        if(project != null && Tabs.isProjectTab(tabId))
            num = project.getProjectNumber();
        TabPane tp = tab.getTabPane();
        if(tp != null) {
            ObservableList<String> lst = tp.getStyleClass();
            if(lst != null) {
                ObservableList<String> lstAdd = FXCollections.observableArrayList();
                for(String name : lst) {
                    if(!name.startsWith("tabHolder"))
                        lstAdd.add(name);
                }
                lstAdd.add("tabHolder");
                if(num != -1)
                    lstAdd.add("tabHolder" + num);
                tp.getStyleClass().clear();
                tp.getStyleClass().addAll(lstAdd);
            }
        }
    }
    public SubTabBase.SubTabInfo getSelectedSubTab() {
        SubTabBase.SubTabInfo sti = null;
        if(tpSubTabs != null) {
            Tab subTab = tpSubTabs.getSelectionModel().getSelectedItem();
            if(subTab.getId().equals(subTabPlusInfo.subTabId)) {
                if(subTabPlusInfo.subTabBase.subTab.isSelected())
                    sti = subTabPlusInfo;
            }
            else {
                for(SubTabBase.SubTabInfo info : tabDef.subTabsInfo) {
                    System.out.println("subtabinfo: " + info.subTabId + ", " + info.subTabBase);
                    if(info.subTabBase != null && info.subTabBase.subTab.isSelected()) {
                        sti = info;
                        break;
                    }
                }
            }
        }
        return sti;
    }
    public SubTabBase.SubTabInfo getSubTab(String subTabId) {
        SubTabBase.SubTabInfo sti = null;
        for(SubTabBase.SubTabInfo info : tabDef.subTabsInfo) {
            if(info.subTabId.equals(subTabId)) {
                sti = info;
                break;
            }
        }
        return sti;
    }
    public void changeToNotFocused() {
        SubTabBase.SubTabInfo sti = getSelectedSubTab();
        if(sti != null && sti.subTabBase != null)
            sti.subTabBase.setTabFocusStyle(false);
        else
            System.out.println("------------ no ptr");
    }
    // Note: if override, make sure to call base
    protected void onSelect(boolean selected) {
        System.out.println("onSelect tab " + tabDef.title + " selected: " + selected);
        if(selected) {
            String called = "NO FUNCTION";
            if(subTabPlusInfo != null && subTabPlusInfo.subTabBase != null && subTabPlusInfo.subTabBase.subTab.isSelected()) {
                called = "subTabPlusInfo.subTabBase.onSelect(true)";
                subTabPlusInfo.subTabBase.onSelect(true);
            }
            else {
                for(SubTabBase.SubTabInfo info : tabDef.subTabsInfo) {
                    if(info.subTabBase != null && info.subTabBase.subTab.isSelected()) {
                        info.subTabBase.onSelect(true);
                        called = "info.subTabBase.onSelect(true)";
                        break;
                    }
                }
            }
            tabs.changeToNotFocused(tpParent);
            System.out.println("Selected tab function called: " + called);
        }
        else
            tab.setStyle("");
    }
    protected boolean openRequestedPanels() {
        boolean opened = false;
        if(args.containsKey("panels"))
            opened = openPanels((String) args.get("panels"));
        return opened;
    }
    protected boolean openPanels(String panelNames) {
        boolean opened = false;
        String[] panels = panelNames.split(";");
        for(String panelName : panels) {
            setupSpecialPanel(panelName);
            for(SubTabBase.SubTabInfo info : tabDef.subTabsInfo) {
                if(info.subTabId.equals(panelName)) {
                    // openSubTab will call createSubTabInstance which uses args
                    openSubTab(info, !opened);
                    if(info.subTabBase != null)
                        tab.getTabPane().getSelectionModel().select(tab);
                    opened = true;
                    break;
                }
            }
        }
        return opened;
    }
    protected void setupSpecialPanel(String panelName) {
        // override if needed
    }
    public String getState() {
        // override
        return "";
    }
    protected void closePanels(String panelNames) {
        String[] panels = panelNames.split(";");
        for(String panelName : panels) {
            for(SubTabBase.SubTabInfo info : tabDef.subTabsInfo) {
                if(info.subTabId.equals(panelName))
                    closeSubTab(info);
            }
        }
    }
    protected void closePanelsStartWith(String panelNames) {
        String[] panels = panelNames.split(";");
        for(String panelName : panels) {
            for(SubTabBase.SubTabInfo info : tabDef.subTabsInfo) {
                if(info.subTabId.startsWith(panelName))
                    closeSubTab(info);
            }
        }
    }
    protected SubTabBase.SubTabInfo getPanelInfo(String panel) {
        SubTabBase.SubTabInfo panelInfo = null;
        for(SubTabBase.SubTabInfo info : tabDef.subTabsInfo) {
            if(info.subTabId.equals(panel)) {
                panelInfo = info;
                break;
            }
        }
        return panelInfo;
    }
    // display a dialog with help for the subtab currently being displayed
    public void openHelpDialog(SubTabBase.SubTabInfo subTabInfo) {
        DlgHelp dlg = new DlgHelp();
        // this is not a clean way to do this but it works
        String helpSubTabId = subTabInfo.subTabId;

        //Results
        if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.STATSFDA.name()))
            helpSubTabId = TabProjectData.Panels.STATSFDA.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.STATSDEAGENE.name()))
            helpSubTabId = TabProjectData.Panels.STATSDEAGENE.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.STATSDEAPROT.name()))
            helpSubTabId = TabProjectData.Panels.STATSDEAPROT.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.STATSDEATRANS.name()))
            helpSubTabId = TabProjectData.Panels.STATSDEATRANS.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.STATSDIUPROT.name()))
            helpSubTabId = TabProjectData.Panels.STATSDIUPROT.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.STATSDIUTRANS.name()))
            helpSubTabId = TabProjectData.Panels.STATSDIUTRANS.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.STATSDFI.name()))
            helpSubTabId = TabProjectData.Panels.STATSDFI.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.FIASSOCIATION.name()))
            helpSubTabId = TabProjectData.Panels.FIASSOCIATION.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.FIRESULTSSUMMARY.name()))
            helpSubTabId = TabProjectData.Panels.FIRESULTSSUMMARY.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.STATSDPA.name()))
            helpSubTabId = TabProjectData.Panels.STATSDPA.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.STATSUTRL.name()))
            helpSubTabId = TabProjectData.Panels.STATSUTRL.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.STATSFEA.name()))
            helpSubTabId = TabProjectData.Panels.STATSFEA.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.STATSGSEA.name()))
            helpSubTabId = TabProjectData.Panels.STATSGSEA.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.CLUSTERSFEA.name()))
            helpSubTabId = TabProjectData.Panels.CLUSTERSFEA.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectData.Panels.CLUSTERSGSEA.name()))
            helpSubTabId = TabProjectData.Panels.CLUSTERSGSEA.name();

        //Visualization
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SUMMARYFDA.name()))
            helpSubTabId = TabProjectDataViz.Panels.SUMMARYFDA.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SUMMARYFDACOMBINEDRESULTS.name()))
            helpSubTabId = TabProjectDataViz.Panels.SUMMARYFDACOMBINEDRESULTS.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SUMMARYDEAGENE.name()))
            helpSubTabId = TabProjectDataViz.Panels.SUMMARYDEAGENE.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SUMMARYDEAPROT.name()))
            helpSubTabId = TabProjectDataViz.Panels.SUMMARYDEAPROT.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SUMMARYDEATRANS.name()))
            helpSubTabId = TabProjectDataViz.Panels.SUMMARYDEATRANS.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SUMMARYDIUPROT.name()))
            helpSubTabId = TabProjectDataViz.Panels.SUMMARYDIUPROT.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SUMMARYDIUTRANS.name()))
            helpSubTabId = TabProjectDataViz.Panels.SUMMARYDIUTRANS.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SUMMARYDFI.name()))
            helpSubTabId = TabProjectDataViz.Panels.SUMMARYDFI.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SUMMARYDPAGENE.name()))
            helpSubTabId = TabProjectDataViz.Panels.SUMMARYDPAGENE.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SUMMARYUTRL.name()))
            helpSubTabId = TabProjectDataViz.Panels.SUMMARYUTRL.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SUMMARYFEA.name()))
            helpSubTabId = TabProjectDataViz.Panels.SUMMARYFEA.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.ENRICHEDFEA.name()))
            helpSubTabId = TabProjectDataViz.Panels.ENRICHEDFEA.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SUMMARYGSEA.name()))
            helpSubTabId = TabProjectDataViz.Panels.SUMMARYGSEA.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.ENRICHEDGSEA.name()))
            helpSubTabId = TabProjectDataViz.Panels.ENRICHEDGSEA.name();

        // Clusters
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.CLUSTERSDEAGENE.name()))
            helpSubTabId = TabProjectDataViz.Panels.CLUSTERSDEAGENE.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.CLUSTERSDEAPROT.name()))
            helpSubTabId = TabProjectDataViz.Panels.CLUSTERSDEAPROT.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.CLUSTERSDEATRANS.name()))
            helpSubTabId = TabProjectDataViz.Panels.CLUSTERSDEATRANS.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.CLUSTERSDPA.name()))
            helpSubTabId = TabProjectDataViz.Panels.CLUSTERSDPA.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.CLUSTERSUTRL.name()))
            helpSubTabId = TabProjectDataViz.Panels.CLUSTERSUTRL.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.CLUSTERSFEA.name()))
            helpSubTabId = TabProjectDataViz.Panels.CLUSTERSFEA.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.CLUSTERSGOFEA.name()))
            helpSubTabId = TabProjectDataViz.Panels.CLUSTERSGOFEA.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.CLUSTERSGSEA.name()))
            helpSubTabId = TabProjectDataViz.Panels.CLUSTERSGSEA.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.CLUSTERSGOGSEA.name()))
            helpSubTabId = TabProjectDataViz.Panels.CLUSTERSGOGSEA.name();

        //Venn Diagrams
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.VENNDIAGDEAGENE.name()))
            helpSubTabId = TabProjectDataViz.Panels.VENNDIAGDEAGENE.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.VENNDIAGDEAPROT.name()))
            helpSubTabId = TabProjectDataViz.Panels.VENNDIAGDEAPROT.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.VENNDIAGDEATRANS.name()))
            helpSubTabId = TabProjectDataViz.Panels.VENNDIAGDEATRANS.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.VENNDIAGDPA.name()))
            helpSubTabId = TabProjectDataViz.Panels.VENNDIAGDPA.name();

        //Others
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SDEXONS.name()))
            helpSubTabId = TabProjectDataViz.Panels.SDEXONS.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SDTRANS.name()))
            helpSubTabId = TabProjectDataViz.Panels.SDTRANS.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.SDTRANSLENGTHS.name()))
            helpSubTabId = TabProjectDataViz.Panels.SDTRANSLENGTHS.name();

        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.DPERGENE.name()))
            helpSubTabId = TabProjectDataViz.Panels.DPERGENE.name();
        else if(subTabInfo.subTabId.startsWith(TabProjectDataViz.Panels.DPERCHROMO.name()))
            helpSubTabId = TabProjectDataViz.Panels.DPERCHROMO.name();

        // get rid of the specific title information - help is general
        String title = subTabInfo.title;
        int pos = subTabInfo.title.indexOf(":");
        if(pos != -1)
            title = subTabInfo.title.substring(0, pos);
        dlg.show(tabDef.dlgHelpTitle + title + " Help", "Help_" + tabDef.resId + "_" + helpSubTabId + ".html", Tappas.getWindow());
    }
    public void setTabLabel(String txt, String tooltip) {
        //lblHeader
        tab.setText(txt + "  ");
        tab.setTooltip(new Tooltip(tooltip));
    }
    public void search(Object obj, String txt, boolean hide) {
        // override
        System.out.println("No search function available to do search...");
    }
    // if obj is null, txt and hide are ignored
    protected void searchSet(Object obj, String txt, boolean hide) {
        if(obj == null)
            tabs.searchSet(null);
        else
            tabs.searchSet(new Tabs.SearchInfo(tbObj, obj, txt, hide));
    }
    public Tab getTab() { return tab; }
    public Label getHeaderLabel() { return lblHeader; }
    public void closeSubTabPlus() { if(subTabPlus != null) tpSubTabs.getTabs().remove(subTabPlus.subTab); }

    protected Tab createTab() {
        Tab newtab = new Tab();
        paneTabHeader = new Pane();
        int offx = 2;
        if(!tabDef.imgName.isEmpty()) {
            Image img = new Image(getClass().getResourceAsStream("/tappas/images/" + tabDef.imgName));
            ImageView iv = new ImageView(img);
            iv.setFitHeight(BTN_IMGSIZE);
            iv.setFitWidth(BTN_IMGSIZE);
            paneTabHeader.getChildren().add(iv);
            iv.setLayoutX(offx);
            iv.setLayoutY(0);
            offx += BTN_IMGSIZE + 5;
        }
        
        // use the tab text so tabPane over flow button menu shows the tab names properly
        /*lblHeader = new Label(tabDef.title + "  ");
        paneTabHeader.getChildren().add(lblHeader);
        lblHeader.setLayoutX(offx);
        lblHeader.setLayoutY(4);
        lblHeader.setTooltip(new Tooltip(tabDef.tooltip));*/
        newtab.setText(tabDef.title + "  ");
        newtab.setTooltip(new Tooltip(tabDef.tooltip));

        // setup the pane for drag/drop, the text label will be setup once added to the tab pane
        paneTabHeader.setOnDragDetected((MouseEvent event) -> {
            Dragboard dragboard = paneTabHeader.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(Tabs.TAPPAS_TAB_DD);
            dragboard.setContent(clipboardContent);
            dragboard.setDragView(imgTab);
            tabs.draggingTab.set(newtab);
            event.consume();
        });
        newtab.setGraphic(paneTabHeader);
        newtab.setClosable(true);

        // create menu and add close tab listener
        createTabMenu(newtab);
        newtab.setOnCloseRequest((Event event) -> {
            Tab t = (Tab) event.getSource();
            System.out.println("OnCloseRequest: " + t.getId() + " in " + t.getTabPane().getId());
            event.consume();
        });
        return newtab;
    }
    
    // override if needed but should call this base
//this is not being used!!!
    public void onClosing() {
        System.out.println("TabBase onClosing");
        for(SubTabBase.SubTabInfo info : tabDef.subTabsInfo) {
            if(info.subTabBase != null)
                info.subTabBase.onClosing();
        }
    }
    public void addSubTab(Tab subTab) { 
        if(tpSubTabs == null || tpSubTabs.getTabs() == null || subTab == null)
            System.out.println("addSubTab - tpSubTabs or subTab is null");
        else {
            try {
                tpSubTabs.getTabs().add(subTab);
            }
            catch(Exception e) {
                logger.logError("Unable to add sub tab to pane: " + e.getMessage());
            }
        }
    };
    public void selectSubTab(String subTabId) { 
        SubTabBase.SubTabInfo sti =  getSubTab(subTabId);
        if(sti != null && sti.subTabBase != null && sti.subTabBase.subTab != null)
            selectSubTab(sti.subTabBase.subTab); 
    };
    public void selectSubTab(Tab subTab) { 
        if(tpSubTabs.getSelectionModel().getSelectedItem() == subTab)
            onSelect(true);
        else
            tpSubTabs.getSelectionModel().select(subTab);
    };
    public void closeSubTab(SubTabBase.SubTabInfo info) {
        // close existing tab and clear all working variables in subTabInfo
        if(info.subTabBase != null) {
            if(info.subTabBase.subTab != null)
                tpSubTabs.getTabs().remove(info.subTabBase.subTab);
            if(info.dlgFloat != null && info.dlgFloat.dialog != null)
                info.dlgFloat.dialog.close();
            System.out.println("TabBase:closeSubTab('" + info.subTabBase.tabBase.getTabName() + "')");
        }
        info.release();
    }
    public void floatSubTab(SubTabBase.SubTabInfo subTabInfo) {
        // close existing tab and clear all working variables in subTabInfo
        if(subTabInfo.subTabBase != null && subTabInfo.subTabBase.subTab != null) {
            // get the subtab's content and change for docking (shared control)
            Node node = subTabInfo.subTabBase.subTab.getContent();
            subTabInfo.dock = false;
            subTabInfo.subTabBase.setFloatToDock();
            
            // remove subtab from tab pane and show contents in floating window
            tpSubTabs.getTabs().remove(subTabInfo.subTabBase.subTab);
            subTabInfo.dlgFloat = tabs.floatSubTab(node, tabId, subTabInfo);
            System.out.println("TabBase:floatSubTab(" + getTabTitle() + " : " + subTabInfo.title + "), dlg: " + subTabInfo.dlgFloat);
            if(subTabInfo.dlgFloat == null) {
                // make sure to clear subtab object, user will need to re-open it
                subTabInfo.release();
                app.ctls.alertInformation("Float subtab", "Unable to float subtab in window");
            }
        }
    }
    // no checking for ok to close at this point
    public void closeAllSubTabs() {
        System.out.println("TabBase:closeAllSubTabs()");

        // let subtabs know of closing
        for(SubTabBase.SubTabInfo info : tabDef.subTabsInfo) {
            if(info.subTabBase != null)
                info.subTabBase.onClosing();
        }
        
        // close all sub tabs and clear information
        for(SubTabBase.SubTabInfo info : tabDef.subTabsInfo) {
            if(info.subTabBase != null) {
                if(info.subTabBase.subTab != null)
                    tpSubTabs.getTabs().remove(info.subTabBase.subTab);
                if(info.dlgFloat != null && info.dlgFloat.dialog != null)
                    info.dlgFloat.dialog.close();
            }
            info.release();
        }
    }
    public void openSubTab(String subTabId) {
        for(SubTabBase.SubTabInfo info : tabDef.subTabsInfo) {
            if(info.subTabId.equals(subTabId)) {
                if(info.subTabBase != null && info.subTabBase.subTabInfo.dlgFloat != null)
                    app.ctls.alertInformation("Open Subtab", "The requested subtab is\nalready opened in a floating window");
                else
                    openSubTab(info, true);
                break;
            }
        }
    }
    public void openSubTab(SubTabBase.SubTabInfo info, boolean select) {
        if(info.subTabBase != null)
            tpSubTabs.getSelectionModel().select(info.subTabBase.subTab);
        else {
            if(subTabPlus != null)
                tpSubTabs.getTabs().remove(subTabPlus.subTab);
            SubTabBase sub = createSubTabInstance(info);
            if(sub != null) {
                if(sub.subTab == null)
                    System.out.println("NULL***************************");
                addSubTab(sub.subTab);
                if(select)
                    selectSubTab(sub.subTab);
            }
            if(subTabPlus == null) {
                subTabPlus = new SubTabPlus(project);
                subTabPlus.initialize(tbObj, subTabPlusInfo, new HashMap<>());
                subTabPlus.subTab.setClosable(false);
                tpSubTabs.getTabs().add(subTabPlus.subTab);
            }
            else {
                // always move to the end
                tpSubTabs.getTabs().add(tpSubTabs.getTabs().size(), subTabPlus.subTab);
            }
        }
    }
    public SubTabBase createSubTabInstance(SubTabBase.SubTabInfo info) {
        // override
        return null;
    }
    public void createTabMenu(Tab tab) {
        ContextMenu cm = new ContextMenu();
        
        // setup so that a request to close the project tab will be treated as a close project request
        String menutxt = "Close tab and all sub tabs";
        if(tabId.startsWith(Tabs.TAB_PROJECTDATA))
            menutxt = "Close project and all associated tabs and sub tabs.";
        MenuItem item = new MenuItem(menutxt);
        cm.getItems().add(item);
        item.setOnAction((event) -> { tabs.closeTab(tabId); /*onTabCloseRequest();*/ });
        if(tabId.startsWith(Tabs.TAB_GENEDV)) {
            cm.getItems().add(new SeparatorMenuItem());
            item = new MenuItem("Close all Gene Data Visualization tabs for this project");
            cm.getItems().add(item);
            item.setOnAction((event) -> { tabs.closeAllGeneDataVizTabs(project, project.getProjectId()); });
        }
        cm.setAutoHide(true);
        tab.setUserData(cm);
    }
    // override if need additional logic
    protected boolean onCloseRequestOK() {
        boolean ok = true;
        for(SubTabBase.SubTabInfo info : tabDef.subTabsInfo) {
            if(info.subTabBase != null) {
                if(!info.subTabBase.onCloseRequestOK()) {
                    ok = false;
                    break;
                }
            }
        }
        System.out.println("tabBase:onCloseRequestOK() for " + tabId + " returning " + ok);
        return ok;
    }
    // must Override and implement functionality
    public Object processRequest(HashMap<String, Object> hm) {
        return null;
    }
    // must Override and implement functionality
    public void onContextMenuRequested(Object obj, ContextMenu cm) {
    }
    
    //
    // Data Classes
    //
    
    public static class SearchInfo {
        public Object data;
        public String txt;
        public boolean hide;
        public SearchInfo(Object data, String txt, boolean hide) {
            this.data = data;
            this.txt = txt;
            this.hide = hide;
        }        
    }
    static public class TabDef {
        String resId, name, title, tooltip, dlgHelpTitle, imgName;
        SubTabBase.SubTabInfo[] subTabsInfo;
        public TabDef(String resId, String name, String title, String tooltip, String dlgHelpTitle, String imgName, SubTabBase.SubTabInfo[] subTabsInfo) {
            this.resId = resId;
            this.name = name;
            this.title = title;
            this.tooltip = tooltip;
            this.dlgHelpTitle = dlgHelpTitle;
            this.imgName = imgName;
            this.subTabsInfo = subTabsInfo;
        }
    }
}
