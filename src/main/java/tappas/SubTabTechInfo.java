/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.control.TextArea;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabTechInfo extends SubTabBase {
    TextArea txtInfo;
    
    public SubTabTechInfo(Project project) {
        super(project);
    }
    @Override
    // subTabDef contains the definition for this subTab - this allows reusing the same subTab object in multiple tabs
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        boolean result = super.initialize(tabBase, subTabInfo, args);
        return result;
    }

    //
    // Internal Functions
    //
    private void showSubTab() {
        txtInfo = (TextArea) tabNode.lookup("#txtInfo");

        String techInfo = app.getAppInfo();
        techInfo += "\n" + getTechInfo();
        txtInfo.setText(techInfo);
        setFocusNode(txtInfo, true);
    }
    private String getTechInfo() {
        String info = "";
        Process p;
        BufferedReader in;
        String line;
        
        try {
            if(!Utils.isWindowsOS()) {
                p = Runtime.getRuntime().exec("which java");
                in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                info += "which java = ";
                while ((line = in.readLine()) != null)
                    info += line + "\n";
                in.close();
                info += "which Rscript = ";
                p = Runtime.getRuntime().exec("which Rscript");
                in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                boolean fnd = false;
                while ((line = in.readLine()) != null) {
                    fnd = true;
                    info += line + "\n";
                }
                in.close();
                if(!fnd){
                    info += "\n";
                }
            }
            info += "Java Environment (System.getenv()):\n";
            Map<String, String> hmEnv =  System.getenv();
            for(String key : hmEnv.keySet())
                info += "    " + key + ": " + hmEnv.get(key) + "\n";
            info += "java.io.tmpdir:\n";
            info += System.getProperty("java.io.tmpdir") + "\n";
            info += "Script Environment (printenv):\n";
            p = Runtime.getRuntime().exec("printenv");
            in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((line = in.readLine()) != null)
                info += "    " + line + "\n";
            info += "\n";
            in.close();
        } catch (IOException e) {
            logger.logError("Internal program error: " + e.getMessage());
        }
        return info;
    }   
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        showSubTab();
    }
}
