/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.control.*;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.Optional;

/** 
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class DlgClusterAnalysis extends DlgBase {
    TextField txtSeedNodes, txtClusterNodes, txtSimilarTerms;
    TextField txtThreshold, txtLinkage;
    
    public DlgClusterAnalysis(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(HashMap<String, String> dfltValues) {
        if(createDialog("ClusterParams.fxml", "Cluster Analysis Parameters", true, "Help_Dlg_ClusterAnalysis.html")) {
            // setup default values
            HashMap<String, String> vals = new HashMap<>();
            vals.put(Params.SIMILAR_PARAM, "4");
            vals.put(Params.KAPPA_PARAM, "0.35");
            vals.put(Params.SEEDNODES_PARAM, "4");
            vals.put(Params.CLUSTERNODES_PARAM, "4");
            vals.put(Params.LINKAGE_PARAM, "0.5");
            if(dfltValues != null) {
                if(dfltValues.containsKey(Params.SIMILAR_PARAM))
                    vals.put(Params.SIMILAR_PARAM, dfltValues.get(Params.SIMILAR_PARAM));
                if(dfltValues.containsKey(Params.KAPPA_PARAM))
                    vals.put(Params.KAPPA_PARAM, dfltValues.get(Params.KAPPA_PARAM));
                if(dfltValues.containsKey(Params.SEEDNODES_PARAM))
                    vals.put(Params.SEEDNODES_PARAM, dfltValues.get(Params.SEEDNODES_PARAM));
                if(dfltValues.containsKey(Params.CLUSTERNODES_PARAM))
                    vals.put(Params.CLUSTERNODES_PARAM, dfltValues.get(Params.CLUSTERNODES_PARAM));
                if(dfltValues.containsKey(Params.LINKAGE_PARAM))
                    vals.put(Params.LINKAGE_PARAM, dfltValues.get(Params.LINKAGE_PARAM));
            }

            // get control objects
            txtSimilarTerms = (TextField) scene.lookup("#txtSimilarTerms");
            txtThreshold = (TextField) scene.lookup("#txtKappaThreshold");
            txtSeedNodes = (TextField) scene.lookup("#txtSeedNodes");
            txtClusterNodes = (TextField) scene.lookup("#txtClusterNodes");
            txtLinkage = (TextField) scene.lookup("#txtLinkageThreshold");

            // populate dialog
            txtSimilarTerms.setText(vals.get(Params.SIMILAR_PARAM));
            txtThreshold.setText(vals.get(Params.KAPPA_PARAM));
            txtSeedNodes.setText(vals.get(Params.SEEDNODES_PARAM));
            txtClusterNodes.setText(vals.get(Params.CLUSTERNODES_PARAM));
            txtLinkage.setText(vals.get(Params.LINKAGE_PARAM));
            
            // setup dialog event handlers
            dialog.setOnCloseRequest((DialogEvent event) -> {
                if(dialog.getResult() != null && dialog.getResult().containsKey("ERRMSG")) {
                    showDlgMsg((String)dialog.getResult().get("ERRMSG"));
                    dialog.setResult(null);
                    event.consume();
                }
            });
            dialog.setResultConverter((ButtonType b) -> {
                HashMap<String, String> params = null;
                System.out.println(b.getButtonData().toString());
                if (b.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                    params = validate(dialog);
                return params;
            });

            // setup dialog event handlers
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return new Params(result.get());
        }
        return null;
    }
    
    //
    // Dialog Validation
    //
    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        String errmsg = "";
        System.out.println("Validate dialog");
        
        // there is no way to screw up the choice boxes - must select a value
        // so all we need to do is check for valid nodes and threshold value
        String txt = txtSimilarTerms.getText().trim();
        if(txt.length() > 0) {
            try {
                Double val = Double.parseDouble(txt);
                if(val >= Params.MIN_SIMILAR_TERMS && val <= Params.MAX_SIMILAR_TERMS) {
                    results.put(Params.SIMILAR_PARAM, txt);
                }
                else
                    errmsg = "Invalid similar terms value entered (" + Params.MIN_SIMILAR_TERMS + " to " + Params.MAX_SIMILAR_TERMS + " allowed).";
            } catch(Exception e) {
                errmsg = "Invalid similar terms value number entered.";
            }
        }
        else
            errmsg = "You must enter a similar terms value.";
        if(!errmsg.isEmpty()) {
            txtSimilarTerms.requestFocus();
            results.put("ERRMSG", errmsg);
        }
        else {
            txt = txtThreshold.getText().trim();
            if(txt.length() > 0) {
                try {
                    Double val = Double.parseDouble(txt);
                    if(val >= Params.MIN_KAPPA_THRESHOLD && val <= Params.MAX_KAPPA_THRESHOLD) {
                        results.put(Params.KAPPA_PARAM, txt);
                    }
                    else
                        errmsg = "Invalid kappa threshold value entered (" + Params.MIN_KAPPA_THRESHOLD + " to " + Params.MAX_KAPPA_THRESHOLD + " allowed).";
                } catch(Exception e) {
                    errmsg = "Invalid kappa threshold value number entered.";
                }
            }
            else
                errmsg = "You must enter a kappa threshold value.";
            
        }
        if(!errmsg.isEmpty()) {
            txtThreshold.requestFocus();
            results.put("ERRMSG", errmsg);
        }
        else {
            txt = txtSeedNodes.getText().trim();
            if(txt.length() > 0) {
                try {
                    Double val = Double.parseDouble(txt);
                    if(val >= Params.MIN_CLUSTER_NODES && val <= Params.MAX_CLUSTER_NODES) {
                        results.put(Params.SEEDNODES_PARAM, txt);
                    }
                    else
                        errmsg = "Invalid nodes value entered (" + Params.MIN_CLUSTER_NODES + " to " + Params.MAX_CLUSTER_NODES + " allowed).";
                } catch(Exception e) {
                    errmsg = "Invalid nodes value number entered.";
                }
            }
            else
                errmsg = "You must enter a minimum nodes value.";
            
        }
        if(!errmsg.isEmpty()) {
            txtSeedNodes.requestFocus();
            results.put("ERRMSG", errmsg);
        }
        else {
            txt = txtClusterNodes.getText().trim();
            if(txt.length() > 0) {
                try {
                    Double val = Double.parseDouble(txt);
                    if(val >= Params.MIN_CLUSTER_NODES && val <= Params.MAX_CLUSTER_NODES) {
                        results.put(Params.CLUSTERNODES_PARAM, txt);
                    }
                    else
                        errmsg = "Invalid cluster nodes value entered (" + Params.MIN_CLUSTER_NODES + " to " + Params.MAX_CLUSTER_NODES + " allowed).";
                } catch(Exception e) {
                    errmsg = "Invalid cluster nodes value number entered.";
                }
            }
            else
                errmsg = "You must enter a minimum cluster nodes value.";
            
        }
        if(!errmsg.isEmpty()) {
            txtClusterNodes.requestFocus();
            results.put("ERRMSG", errmsg);
        }
        return results;
    }
    
    //
    // Data Classes
    //
    public static class Params extends DlgParams {
        public static final String SIMILAR_PARAM = "similar";
        public static final String KAPPA_PARAM = "kappa";
        public static final String SEEDNODES_PARAM = "seednodes";
        public static final String CLUSTERNODES_PARAM = "clusternodes";
        public static final String LINKAGE_PARAM = "linkage";
        
        final static int MIN_SIMILAR_TERMS = 0;
        final static int MAX_SIMILAR_TERMS = 99;
        final static double MIN_KAPPA_THRESHOLD = 0.0;
        final static double MAX_KAPPA_THRESHOLD = 1.0;
        final static int MIN_CLUSTER_NODES = 2;
        final static int MAX_CLUSTER_NODES = 99;
        final static double MIN_LINKAGE_THRESHOLD = 0.0;
        final static double MAX_LINKAGE_THRESHOLD = 1.0;

        private final int dfltSimilarTerms = 4;
        private final double dfltKappa = 0.35;
        private final int dfltSeeds = 4;
        private final int dfltClusterNodes = 4;
        private final double dfltLinkage = 0.5;
        
        public int similarTerms;
        public double kappa, linkage;
        public int seeds;
        public int clusterNodes;
        public Params() {
            this.similarTerms = dfltSimilarTerms;
            this.kappa = dfltKappa;
            this.seeds = dfltSeeds;
            this.clusterNodes = dfltClusterNodes;
            this.linkage = dfltLinkage;
        }
        public Params(HashMap<String, String> hmParams) {
            this.similarTerms = hmParams.containsKey(SIMILAR_PARAM)? Integer.parseInt(hmParams.get(SIMILAR_PARAM)) : dfltSimilarTerms;
            this.kappa = hmParams.containsKey(KAPPA_PARAM)? Double.parseDouble(hmParams.get(KAPPA_PARAM)) : dfltKappa;
            this.seeds = hmParams.containsKey(SEEDNODES_PARAM)? Integer.parseInt(hmParams.get(SEEDNODES_PARAM)) : dfltSeeds;
            this.clusterNodes = hmParams.containsKey(CLUSTERNODES_PARAM)? Integer.parseInt(hmParams.get(CLUSTERNODES_PARAM)) : dfltClusterNodes;
            this.linkage = hmParams.containsKey(LINKAGE_PARAM)? Double.parseDouble(hmParams.get(LINKAGE_PARAM)) : dfltLinkage;
        }
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(SIMILAR_PARAM, "" + similarTerms);
            hm.put(KAPPA_PARAM, "" + kappa);
            hm.put(SEEDNODES_PARAM, "" + seeds);
            hm.put(CLUSTERNODES_PARAM, "" + clusterNodes);
            hm.put(LINKAGE_PARAM, "" + linkage);
            return hm;
        }
        // base class implements boolean save(String filepath)

        //
        // Static functions
        //
        public static Params load(String filepath) {
            HashMap<String, String> params = new HashMap<>();
            Utils.loadParams(params, filepath);
            return (new Params(params));
        }
    }
}
