/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.control.*;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

/**
 *
 * @author Pedro Salguero - psalguero@cipf.es
 */
public class DlgFDAnalysisCombinedResults extends DlgBase {
    ChoiceBox cbFeatures;

    public DlgFDAnalysisCombinedResults(Project project, Window window) {
        super(project, window);
    }
    public Params showAndWait(Params dfltParams) {
        if(createDialog("FDAParamsCombinedResults.fxml", "Functional Diversity Analysis Combined Results", true, "Help_PD_FAIDCOMBINED.html")) {
            if(dfltParams == null)
                dfltParams = new Params(project);

            // get control objects
            cbFeatures = (ChoiceBox) scene.lookup("#cbFeatures");

            // set features list
            for(String feat : Params.FeatureList) {
                cbFeatures.getItems().add(feat);
            }

            cbFeatures.getSelectionModel().select(Params.getIdxByName(Params.FeatureList[0], Params.FeatureList));

            int idx = 0;
            for(int i = 0; i < cbFeatures.getItems().size(); i++) {
                if(((String)cbFeatures.getItems().get(i)).startsWith(getCompareNameStart(Params.FeatureList[idx]))){
                    cbFeatures.getSelectionModel().select(i);
                    break;
                }
            }

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

            // process dialog
            Optional<HashMap> result = dialog.showAndWait();
            if(result.isPresent())
                return(new Params(result.get()));
        }
        return null;
    }


    private HashMap<String, String> validate(Dialog dialog) {
        HashMap<String, String> results = new HashMap<>();
        System.out.println("Validate dialog");

        // get the feature list
        int idx = cbFeatures.getSelectionModel().getSelectedIndex();
        String feature = Params.FeatureList[idx];
        results.put(Params.FEATURELIST_PARAM, feature);
        String ana1 = Params.hmFeatures.get(feature).get(Params.GENEPOS);
        results.put(Params.ANALYSISGENOMIC_PARAM, ana1);
        String ana2 = Params.hmFeatures.get(feature).get(Params.PRESENCE);
        results.put(Params.ANALYSISPRESENCE_PARAM, ana2);
        return results;
    }

    private static String getCompareNameStart(String name) {
        int idx = name.indexOf("@");
        if(idx != -1)
            return name.substring(0, idx);
        return name;
    }
    
    //
    // Data Classes
    //
    public static class Params extends DlgParams {
        public static DataFDA fdaData;
        public static HashMap<String, HashMap<String, String>> hmFeatures;
        public static String[] FeatureList;

        public static final String GENEPOS = "GENPOS";
        public static final String PRESENCE = "PRESENCE";

        public static final String FEATURELIST_PARAM = "feature";
        public static final String ANALYSISGENOMIC_PARAM = "analysis_genomic";
        public static final String ANALYSISPRESENCE_PARAM = "analysis_presence";

        String feature;
        String analysis_genomic;
        String analysis_presence;

        public String getFeature(int i){
            return FeatureList[i];
        }

        public String[] hmToArray(HashMap<String, HashMap<String, String>> hmFeature){
            String[] features = new String[hmFeature.size()];
            int cont = 0;
            for(String aux : hmFeature.keySet()){
                features[cont] = aux;
                cont++;
            }
            return features;
        }

        public Params(Project project) {
            fdaData = new DataFDA(project);
            hmFeatures = fdaData.getFDAIdDataFeatures();
            ArrayList<String> delete = new ArrayList<String>();
            for(String feat : hmFeatures.keySet()) {
                if (hmFeatures.get(feat).size() < 2)
                    delete.add(feat);
            }

            for(String d : delete)
                hmFeatures.remove(d);

            FeatureList = hmToArray(hmFeatures);

            this.feature = "";
            this.analysis_genomic = "";
            this.analysis_presence = "";
        }
        
        public Params(HashMap<String, String> hmParams){
            hmFeatures = fdaData.getFDAIdDataFeatures();
            ArrayList<String> delete = new ArrayList<String>();
            for(String feat : hmFeatures.keySet()) {
                if (hmFeatures.get(feat).size() < 2)
                    delete.add(feat);
            }

            for(String d : delete)
                hmFeatures.remove(d);

            FeatureList = hmToArray(hmFeatures);

            this.feature = hmParams.containsKey(FEATURELIST_PARAM)? hmParams.get(FEATURELIST_PARAM) : "";
            this.analysis_genomic = hmParams.containsKey(ANALYSISGENOMIC_PARAM)? hmParams.get(ANALYSISGENOMIC_PARAM) : "";
            this.analysis_presence = hmParams.containsKey(ANALYSISPRESENCE_PARAM)? hmParams.get(ANALYSISPRESENCE_PARAM) : "";
        }
        
        @Override
        public HashMap<String, String> getParams() {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(ANALYSISGENOMIC_PARAM, analysis_genomic);
            hm.put(ANALYSISPRESENCE_PARAM, analysis_presence);
            return hm;
        }

        //
        // Static functions
        //
        public static Params load(String filepath) {
            HashMap<String, String> params = new HashMap<>();
            Utils.loadParams(params, filepath);
            return (new Params(params));
        }
        
        private static int getIdxByName(String name, String[] lst) {
            int idx = 0;
            for(String entry : lst) {
                String cmpName = getCompareNameStart(entry);
                if(name.startsWith(cmpName)) {
                    // "All @" matches "All multiple..." so handle it at end
                    if(idx != 0)
                        break;
                }
                idx++;
            }
            if(idx >= lst.length)
                idx = 0;
            return idx;
        }
    }   
}