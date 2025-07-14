/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.application.Platform;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.HashMap;


/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabLog extends SubTabBase {
    TextArea txtLog = null;
    ArrayList<String> lstLogUpdates = new ArrayList<>();
    private final Object lock = new Object();
    
    public SubTabLog(Project project) {
        super(project);
    }
    @Override
    // subTabDef contains the definition for this subTab - this allows reusing the same subTab object in multiple tabs
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnOptions = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            txtLog = (TextArea) tabNode.lookup("#txtLog");
        }
        return result;
    }
    @Override
    public Object processRequest(HashMap<String, Object> hm) {
        Object obj = null;
        boolean upd = false;
        if(hm.containsKey("setText")) {
            String txt = (String) hm.get("setText");
            synchronized(lock) {
                if(lstLogUpdates.isEmpty())
                    upd = true;
                lstLogUpdates.add("");
                if(!txt.isEmpty())
                    lstLogUpdates.add(txt);
            }
        }
        else if(hm.containsKey("appendText")) {
            String txt = (String) hm.get("appendText");
            synchronized(lock) {
                if(lstLogUpdates.isEmpty())
                    upd = true;
                lstLogUpdates.add(txt);
                System.out.println("added log txt(" + lstLogUpdates.size() + "): " + txt);
            }
        }
        
        // check if we need to invoke actual UI display update or already in the works
        if(upd) {
            Platform.runLater(() -> {
                synchronized(lock) {
                    for(String newtxt : lstLogUpdates) {
                        if(newtxt.isEmpty())
                            txtLog.setText("");
                        else
                            txtLog.appendText(newtxt);
                    }
                    lstLogUpdates.clear();
                }
            });
        }
        
        return obj;
    }
    @Override
    protected void onSelect(boolean selected) {
        super.onSelect(selected);

        // always clear see log message on top tool bar if log is shown
        app.ctlr.showSeeAppLog(false);
    }

    //
    // Internal Functions
    //
    private void showSubTab() {
        // set options menu
        ContextMenu cm = new ContextMenu();
        if(subTabInfo.subTabBase.tabBase.tabId.equals(Tabs.TAB_AI)) {
            CheckMenuItem check = new CheckMenuItem("Log debug messages");
            check.setSelected(app.isShowDebugMsg());
            check.setOnAction((event) -> { app.setShowDebugMsg(!app.isShowDebugMsg()); });
            cm.getItems().add(check);
            cm.getItems().add(new SeparatorMenuItem());
        }
        MenuItem item = new MenuItem("Clear Log");
        item.setOnAction((event) -> { app.logClear(); });
        cm.getItems().add(item);
        subTabInfo.cmOptions = cm;

        // populate initial log display
        txtLog.setText(app.getAppLogContents());
        txtLog.appendText("");
        System.out.println("initial log display: " + txtLog.getText());
        setFocusNode(txtLog, true);
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        // it is possible for app to have been updating the log but the log itself was not displayed
        // clear any messages pending, they have already been written to file and
        // will be included in the log contents from app.getAppLogContents()
        synchronized(lock) {
            lstLogUpdates.clear();
        }
        showSubTab();
    }
}
