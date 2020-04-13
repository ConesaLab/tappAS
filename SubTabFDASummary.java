/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import com.sun.javafx.charts.Legend;
import com.sun.javafx.charts.Legend.LegendItem;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.control.*;
import javafx.scene.effect.Glow;
import javafx.scene.effect.Reflection;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.util.*;

import static tappas.DataAnnotation.AnnotationType;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class SubTabFDASummary extends SubTabBase {
    AnchorPane paneContents;
    ScrollPane spDiversity;
    StackedBarChart barGeneDiversity, barIsoDiversity;
    TextArea taGene, taIsoforms;
    
    ContextMenu cmDiversity;
    String tableColumnId = "";
    DataFDA fdaData;
    //HashMap<String, String> fdaParams = new HashMap<>();
    DlgFDAnalysis.Params fdaParams;
    String analysisId = "";
    boolean usingGenPos = false;

    int barItemsCount;
    int barItemsCountPW;
    
    public SubTabFDASummary(Project project) {
        super(project);
    }
    @Override
    public boolean initialize(TabBase tabBase, SubTabInfo subTabInfo, HashMap<String, Object> args) {
        subTabInfo.btnExport = true;
        boolean result = super.initialize(tabBase, subTabInfo, args);
        if(result) {
            // must have FDA results
            fdaData = new DataFDA(project);
            if(!args.containsKey("id")) {
                if(subTabInfo.subTabId.length() > TabProjectDataViz.Panels.SUMMARYFDA.name().length())
                    args.put("id", subTabInfo.subTabId.substring(TabProjectDataViz.Panels.SUMMARYFDA.name().length()).trim());
            }
            
             if(args.containsKey("id")) {
                analysisId = (String) args.get("id");
                fdaParams = DlgFDAnalysis.Params.load(fdaData.getFDAParamsFilepath(analysisId));
                usingGenPos = fdaParams.using.equals(fdaParams.using.GENPOS);
                String shortName = getShortName(fdaParams.name);
                subTabInfo.title = "FDA: " + shortName;
                subTabInfo.tooltip = "Functional Diversity Analysis summary for '" + fdaParams.name.trim() + "'";
                setTabLabel(subTabInfo.title, subTabInfo.tooltip);
            }
            else
                result = false;
            
            result = fdaData.hasFDAData(analysisId);
            if(!result)
                subTabInfo.subTabBase = null;
        }
        return result;
    }
    
    //
    // Internal Functions
    //
    private void showSubTab(DataAnnotation.DiversityResults fdaSummaryResults) {
        paneContents = (AnchorPane) tabNode.lookup("#paneContents");
        spDiversity = (ScrollPane) tabNode.getParent().lookup("#spDiversity");
        barGeneDiversity = (StackedBarChart) spDiversity.getContent().lookup("#barDiversity");
        barIsoDiversity = (StackedBarChart) spDiversity.getContent().lookup("#barDiversityPW");
        
        String usingTitle = usingGenPos? "(Genomic Position)" : "(Presence)";
        String usingSave = usingGenPos? "genomicPosition" : "presence";
        barGeneDiversity.setTitle("Gene Level Functional Diversity " + usingTitle);
        barIsoDiversity.setTitle("Pairwise Isoform Level Functional Diversity " + usingTitle);
        
        HashMap<String, String> hmp = fdaData.getFDAParams(analysisId);
        int geneCount = project.data.getMultipleIsoformsGeneCount();

        String style = "-fx-background-color: white; -fx-font-size: 16px;";
        String styleLabels = "-fx-font-size: 16; -fx-tick-label-font-size: 14px;";
        String styleLegend = "-fx-font-size: 16px;";

        barGeneDiversity.setStyle(style);
        barGeneDiversity.lookup(".axis").setStyle(styleLabels);
        barGeneDiversity.lookup(".axis-label").setStyle(styleLabels);
        barGeneDiversity.lookup(".chart-legend").setStyle(styleLegend);
        barGeneDiversity.getYAxis().setStyle(styleLabels);

        barIsoDiversity.setStyle(style);
        barIsoDiversity.lookup(".axis").setStyle(styleLabels);
        barIsoDiversity.lookup(".axis-label").setStyle(styleLabels);
        barIsoDiversity.lookup(".chart-legend").setStyle(styleLegend);
        barIsoDiversity.getYAxis().setStyle(styleLabels);

        ArrayList<DataAnnotation.DiversityData> lstStats = fdaSummaryResults.lstData;
        ArrayList<DataAnnotation.DiversityData> lstPWStats = fdaSummaryResults.lstPWData;
        Collections.sort(lstStats);
        Collections.sort(lstPWStats);
        HashMap<String, Integer> hmTotal = new HashMap<>();
        HashMap<String, Double> hmTrans = new HashMap<>();
        HashMap<String, Double> hmProtein = new HashMap<>();
        HashMap<String, Double> hmGenomic = new HashMap<>();
        HashMap<String, String> hmNameSource = new HashMap<>();
        XYChart.Series<Number, String> series1 = new XYChart.Series<>();
        XYChart.Series<Number, String> series2 = new XYChart.Series<>();
        XYChart.Series<Number, String> seriesPW1 = new XYChart.Series<>();
        XYChart.Series<Number, String> seriesPW2 = new XYChart.Series<>();
        
        XYChart.Series<Number, String> series1aux = new XYChart.Series<>();
        XYChart.Series<Number, String> series2aux = new XYChart.Series<>();
        XYChart.Series<Number, String> seriesPW1aux = new XYChart.Series<>();
        XYChart.Series<Number, String> seriesPW2aux = new XYChart.Series<>();
        
        series1.setName("Varying");
        series2.setName("Not-Varying");
        seriesPW1.setName("Varying");
        seriesPW2.setName("Not-Varying");
        ArrayList<String> lstSeries1 = new ArrayList<>();
        ArrayList<String> lstSeries2 = new ArrayList<>();
        ArrayList<String> lstSeriesPW1 = new ArrayList<>();
        ArrayList<String> lstSeriesPW2 = new ArrayList<>();
        
        barItemsCount = 0;
        barItemsCountPW = 0;
        int total;
        double pct;
        ArrayList<String> db = new ArrayList<>();
        ArrayList<String> dbPW = new ArrayList<>();
        // set to display transcript annotations
        
        // FEATURE, FEATURE(T), [diff, same]
        HashMap<String, HashMap<String, Integer[]>> hmValues = new HashMap<>();
        //FEATURE, DESCRIPTION
        HashMap<String, String> hmStrings = new HashMap<>();
        HashMap<String, String> hmStringsND = new HashMap<>();
        
        for(DataAnnotation.DiversityData dd : lstStats) {
            if(dd.type.equals(AnnotationType.TRANS)) {
                String fields[] = dd.name.split(":");
                String name = fields[1];
                hmNameSource.put(name, dd.name);
                if(hmValues.containsKey(name)){
                    //values
                    Integer[] val = new Integer[2];
                    HashMap<String, Integer[]> hmAux = hmValues.get(name);
                    for(String name_aux : hmAux.keySet()){
                        val = hmAux.get(name_aux);
                        val[0] += dd.diff;
                        val[1] += dd.same;
                        hmAux.put(name_aux, val);
                    }
                    hmValues.put(name, hmAux);
                    //description
                    total = val[0] + val[1];
                    pct = (double)val[0] / total * 100;//((double)val[0] + (double)val[1]) * 100;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));

                    hmStrings.put(name, "Varying Transcript Feature: " + name + "\n" + pct + "% (" + val[0] + ") of " + total + " all feature genes");
                    pct = (double)val[1] / total * 100;// ((double)val[0] + (double)val[1]) * 100;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsND.put(name, "Not-Varying Transcript Feature: " + name + "\n" + pct + "% (" + val[1] + ") of " + total + " all feature genes");
                }else{
                    //values
                    HashMap<String, Integer[]> hmAux = new HashMap<>();
                    Integer[] val = new Integer[2];
                    val[0] = dd.diff;
                    val[1] = dd.same;
                    hmAux.put(name + "(T)", val);
                    hmValues.put(name, hmAux);
                    //description
                    total = val[0] + val[1];
                    pct = (double)val[0] / total * 100;// ((double)val[0] + (double)val[1]) * 100;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));

                    hmStrings.put(name, "Varying Transcript Feature: " + name + "\n" + pct + "% (" + val[0] + ") of " + total + " all feature genes");
                    pct = (double)val[1] / total * 100;// ((double)val[0] + (double)val[1]) * 100;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsND.put(name, "Not-Varying Transcript Feature: " + name + "\n" + pct + "% (" + val[1] + ") of " + total + " all feature genes");
                    db.add(dd.name);
                }
                double genepct = ((double)dd.diff + (double)dd.same) / geneCount * 100;
                genepct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                hmTrans.put(dd.name, genepct);
                if(hmTotal.containsKey(name)){
                    int res = hmTotal.get(name);
                    res += dd.diff+dd.same;
                    hmTotal.put(name, res);
                }else{
                    hmTotal.put(name, dd.diff+dd.same);
                }
            }
        }
        
        //updating values in series1 and descriptions
        for(String name : hmValues.keySet()){
            HashMap<String, Integer[]> hmAux = hmValues.get(name);
            barItemsCount++;
            for(String name_aux : hmAux.keySet()){
                Integer[] val = hmAux.get(name_aux);
                pct = (double)val[0] / geneCount * 100;// ((double)val[0] + (double)val[1]) * 100;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                series1.getData().add(new XYChart.Data(pct, name_aux));
                pct = (double)val[1] / geneCount * 100;// ((double)val[0] + (double)val[1]) * 100;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                series2.getData().add(new XYChart.Data(pct, name_aux));
            }
        }
        
        for(String name : hmStrings.keySet()){
            lstSeries1.add(hmStrings.get(name));
            lstSeries2.add(hmStringsND.get(name));
        }
        
        // FEATURE, FEATURE(T), [diff, same]
        hmValues = new HashMap<>();
        //FEATURE, DESCRIPTION
        hmStrings = new HashMap<>();
        hmStringsND = new HashMap<>();
        
        // set to display protein annotations
        for(DataAnnotation.DiversityData dd : lstStats) {
            if(dd.type.equals(AnnotationType.PROTEIN)) {
                String fields[] = dd.name.split(":");
                String name = fields[1];
                hmNameSource.put(name, dd.name);
                if(hmValues.containsKey(name)){
                    //values
                    Integer[] val = new Integer[2];
                    HashMap<String, Integer[]> hmAux = hmValues.get(name);
                    for(String name_aux : hmAux.keySet()){
                        val = hmAux.get(name_aux);
                        val[0] += dd.diff;
                        val[1] += dd.same;
                        hmAux.put(name_aux, val);
                    }
                    hmValues.put(name, hmAux);
                    //description
                    total = val[0] + val[1];
                    pct = (double)val[0] / total * 100;// ((double)val[0] + (double)val[1]) * 100;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));

                    hmStrings.put(name, "Varying Protein Feature: " + name + "\n" + pct + "% (" + val[0] + ") of " + total + " all feature genes");
                    pct = (double)val[1] / total * 100;// ((double)val[0] + (double)val[1]) * 100;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsND.put(name, "Not-Varying Protein Feature: " + name + "\n" + pct + "% (" + val[1] + ") of " + total + " all feature genes");
                }else{
                    //values
                    HashMap<String, Integer[]> hmAux = new HashMap<>();
                    Integer[] val = new Integer[2];
                    val[0] = dd.diff;
                    val[1] = dd.same;
                    hmAux.put(name + "(P)", val);
                    hmValues.put(name, hmAux);
                    //description
                    total = val[0] + val[1];
                    pct = (double)val[0] / total * 100;// ((double)val[0] + (double)val[1]) * 100;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));

                    hmStrings.put(name, "Varying Protein Feature: " + name + "\n" + pct + "% (" + val[0] + ") of " + total + " all feature genes");
                    pct = (double)val[1] / total * 100;// ((double)val[0] + (double)val[1]) * 100;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsND.put(name, "Not-Varying Protein Feature: " + name + "\n" + pct + "% (" + val[1] + ") of " + total + " all feature genes");
                    db.add(dd.name);
                }
                double genepct = ((double)dd.diff + (double)dd.same) / geneCount * 100;
                genepct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                hmProtein.put(dd.name, genepct);
                if(hmTotal.containsKey(name)){
                    int res = hmTotal.get(name);
                    res += dd.diff+dd.same;
                    hmTotal.put(name, res);
                }else{
                    hmTotal.put(name, dd.diff+dd.same);
                }
            }
        }
        
        //updating values in series1 and descriptions
        for(String name : hmValues.keySet()){
            HashMap<String, Integer[]> hmAux = hmValues.get(name);
            barItemsCount++;
            for(String name_aux : hmAux.keySet()){
                Integer[] val = hmAux.get(name_aux);
                pct = (double)val[0] / geneCount * 100;// ((double)val[0] + (double)val[1]) * 100;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                series1.getData().add(new XYChart.Data(pct, name_aux));
                pct = (double)val[1] / geneCount * 100;// ((double)val[0] + (double)val[1]) * 100;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                series2.getData().add(new XYChart.Data(pct, name_aux));
            }
        }
        
        for(String name : hmStrings.keySet()){
            lstSeries1.add(hmStrings.get(name));
            lstSeries2.add(hmStringsND.get(name));
        }
        
/*        // FEATURE, FEATURE(T), [diff, same]
        hmValues = new HashMap<>();
        //FEATURE, DESCRIPTION
        hmStrings = new HashMap<>();
        hmStringsND = new HashMap<>();
        
        // set to display genomic annotations
        for(DataAnnotation.DiversityData dd : lstStats) {
            if(dd.type.equals(AnnotationType.GENOMIC)) {
                String fields[] = dd.name.split(":");
                String name = fields[1];
                hmNameSource.put(name, dd.name);
                if(hmValues.containsKey(name)){
                    //values
                    Integer[] val = new Integer[2];
                    HashMap<String, Integer[]> hmAux = hmValues.get(name);
                    for(String name_aux : hmAux.keySet()){
                        val = hmAux.get(name_aux);
                        val[0] += dd.diff;
                        val[1] += dd.same;
                        hmAux.put(name_aux, val);
                    }
                    hmValues.put(dd.name, hmAux);
                    //description
                    total = val[0] + val[1];
                    pct = (double)val[0] / total * 100;// ((double)val[0] + (double)val[1]) * 100;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));

                    hmStrings.put(name, "Varying Genomic Feature: " + name + "\n" + pct + "% (" + val[0] + ") of " + total + " all feature genes");
                    pct = (double)val[1] / total * 100;// ((double)val[0] + (double)val[1]) * 100;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsND.put(name, "Not-Varying Genomic Feature: " + name + "\n" + pct + "% (" + val[1] + ") of " + total + " all feature genes");
                }else{
                    //values
                    HashMap<String, Integer[]> hmAux = new HashMap<>();
                    Integer[] val = new Integer[2];
                    val[0] = dd.diff;
                    val[1] = dd.same;
                    hmAux.put(name + "(G)", val);
                    hmValues.put(name, hmAux);
                    //description
                    total = val[0] + val[1];
                    pct = (double)val[0] / total * 100;// ((double)val[0] + (double)val[1]) * 100;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));

                    hmStrings.put(name, "Varying Genomic Feature: " + name + "\n" + pct + "% (" + val[0] + ") of " + total + " all feature genes");
                    pct = (double)val[1] / total * 100;// ((double)val[0] + (double)val[1]) * 100;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsND.put(name, "Not-Varying Genomic Feature: " + name + "\n" + pct + "% (" + val[1] + ") of " + total + " all feature genes");
                    db.add(dd.name);
                }
                double genepct = ((double)dd.diff + (double)dd.same) / geneCount * 100;
                genepct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                hmGenomic.put(dd.name, genepct);
                if(hmTotal.containsKey(name)){
                    int res = hmTotal.get(name);
                    res += dd.diff+dd.same;
                    hmTotal.put(name, res);
                }else{
                    hmTotal.put(name, dd.diff+dd.same);
                }
            }
        }

        //updating values in series1 and descriptions
        for(String name : hmValues.keySet()){
            HashMap<String, Integer[]> hmAux = hmValues.get(name);
            barItemsCount++;
            for(String name_aux : hmAux.keySet()){
                Integer[] val = hmAux.get(name_aux);
                pct = (double)val[0] / geneCount * 100;// ((double)val[0] + (double)val[1]) * 100;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                series1.getData().add(new XYChart.Data(pct, name_aux));
                pct = (double)val[1] / geneCount * 100;// ((double)val[0] + (double)val[1]) * 100;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                series2.getData().add(new XYChart.Data(pct, name_aux));
            }
        }
        
        for(String name : hmStrings.keySet()){
            lstSeries1.add(hmStrings.get(name));
            lstSeries2.add(hmStringsND.get(name));
        }*/
        
        // pairwise isoforms data
        HashMap<String, HashMap<String, Integer[]>> hmValuesPW = new HashMap<>();
        HashMap<String, String> hmStringsPW = new HashMap<>();
        HashMap<String, String> hmStringsPWND = new HashMap<>();
        // set to display transcript annotations
        for(DataAnnotation.DiversityData dd : lstPWStats) {
            if(dd.type.equals(AnnotationType.TRANS)) {
                String fields[] = dd.name.split(":");
                String name = fields[1];
                
                if(hmValuesPW.containsKey(name)){
                    //values
                    Integer[] val = new Integer[3];
                    HashMap<String, Integer[]> hmAux = hmValuesPW.get(name);
                    for(String name_aux : hmAux.keySet()){
                        val = hmAux.get(name_aux);
                        val[0] += dd.diff;
                        val[1] += dd.same;
                        hmAux.put(name_aux, val);
                    }
                    hmValuesPW.put(name, hmAux);
                    //description
                    pct = (double)val[0] / ((double)val[0] + (double)val[1]) * 100 * hmTrans.get(hmNameSource.get(name)) / 100.0;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsPW.put(name, "Varying Transcript Feature: " + name + "\n" + pct + "% of pairwise isoforms with feature");
                    pct = (double)val[1] / ((double)val[0] + (double)val[1]) * 100 * hmTrans.get(hmNameSource.get(name)) / 100.0;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsPWND.put(name, "Not-Varying Transcript Feature: " + name + "\n" + pct + "% of pairwise isoforms with feature");
                }else{
                    //values
                    HashMap<String, Integer[]> hmAux = new HashMap<>();
                    Integer[] val = new Integer[2];
                    val[0] = dd.diff;
                    val[1] = dd.same;
                    hmAux.put(name + "(T)", val);
                    hmValuesPW.put(name, hmAux);
                    //description
                    pct = (double)val[0] / ((double)val[0] + (double)val[1]) * 100 * hmTrans.get(hmNameSource.get(name)) / 100.0;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    total = val[0] + val[1];
                    if(!hmStringsPW.containsKey(name)){
                        barItemsCountPW++;
                        dbPW.add(dd.name);
                    }
                    hmStringsPW.put(name, "Varying Transcript Feature: " + name + "\n" + pct + "% of pairwise isoforms with feature");
                    pct = (double)val[1] / ((double)val[0] + (double)val[1]) * 100 * hmTrans.get(hmNameSource.get(name)) / 100.0;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsPWND.put(name, "Not-Varying Transcript Feature: " + name + "\n" + pct + "% of pairwise isoforms with feature");
                }
                //dbPW.add(dd.name);
            }
        }
        
        //updating trans PW
        for(String name : hmValuesPW.keySet()){
            HashMap<String, Integer[]> hmAux = hmValuesPW.get(name);
            for(String name_aux : hmAux.keySet()){
                Integer[] val = hmAux.get(name_aux);
                pct = (double)val[0] / ((double)val[0] + (double)val[1]) * 100  * hmTrans.get(hmNameSource.get(name)) / 100.0;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                seriesPW1.getData().add(new XYChart.Data(pct, name_aux));
                pct = (double)val[1] / ((double)val[0] + (double)val[1]) * 100  * hmTrans.get(hmNameSource.get(name)) / 100.0;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                seriesPW2.getData().add(new XYChart.Data(pct, name_aux));
            }
        }
        
        for(String name : hmStringsPW.keySet()){
            lstSeriesPW1.add(hmStringsPW.get(name));
            lstSeriesPW2.add(hmStringsPWND.get(name));
        }
        
        hmValuesPW = new HashMap<>();
        hmStringsPW = new HashMap<>();
        hmStringsPWND = new HashMap<>();
        // set to display protein annotations
        for(DataAnnotation.DiversityData dd : lstPWStats) {
            if(dd.type.equals(AnnotationType.PROTEIN)) {
                String fields[] = dd.name.split(":");
                String name = fields[1];
                if(hmValuesPW.containsKey(name)){
                    //values
                    Integer[] val = new Integer[2];
                    HashMap<String, Integer[]> hmAux = hmValuesPW.get(name);
                    for(String name_aux : hmAux.keySet()){
                        val = hmAux.get(name_aux);
                        val[0] += dd.diff;
                        val[1] += dd.same;
                        hmAux.put(name_aux, val);
                    }
                    hmValuesPW.put(name, hmAux);
                    //description
                    pct = (double)val[0] / ((double)val[0] + (double)val[1]) * 100 * hmProtein.get(hmNameSource.get(name)) / 100.0;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsPW.put(name, "Varying Protein Feature: " + name + "\n" + pct + "% of pairwise isoforms with feature");
                    pct = (double)val[1] / ((double)val[0] + (double)val[1]) * 100 * hmProtein.get(hmNameSource.get(name)) / 100.0;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsPWND.put(name, "Not-Varying Protein Feature: " + name + "\n" + pct + "% of pairwise isoforms with feature");
                }else{
                    //values
                    HashMap<String, Integer[]> hmAux = new HashMap<>();
                    Integer[] val = new Integer[2];
                    val[0] = dd.diff;
                    val[1] = dd.same;
                    hmAux.put(name + "(P)", val);
                    hmValuesPW.put(name, hmAux);
                    //description
                    pct = (double)val[0] / ((double)val[0] + (double)val[1]) * 100 * hmProtein.get(hmNameSource.get(name)) / 100.0;;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    total = val[0] + val[1];
                    if(!hmStringsPW.containsKey(name)){
                        barItemsCountPW++;
                        dbPW.add(dd.name);
                    }
                    hmStringsPW.put(name, "Varying Protein Feature: " + name + "\n" + pct + "% of pairwise isoforms with feature");
                    pct = (double)val[1] / ((double)val[0] + (double)val[1]) * 100 * hmProtein.get(hmNameSource.get(name)) / 100.0;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsPWND.put(name, "Not-Varying Protein Feature: " + name + "\n" + pct + "% of pairwise isoforms with feature");
                }
                //dbPW.add(dd.name);
            }
        }
        
        //updating protein PW
        for(String name : hmValuesPW.keySet()){
            HashMap<String, Integer[]> hmAux = hmValuesPW.get(name);
            for(String name_aux : hmAux.keySet()){
                Integer[] val = hmAux.get(name_aux);
                pct = (double)val[0] / ((double)val[0] + (double)val[1]) * 100  * hmProtein.get(hmNameSource.get(name)) / 100.0;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                seriesPW1.getData().add(new XYChart.Data(pct, name_aux));
                pct = (double)val[1] / ((double)val[0] + (double)val[1]) * 100  * hmProtein.get(hmNameSource.get(name)) / 100.0;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                seriesPW2.getData().add(new XYChart.Data(pct, name_aux));
            }
        }
        
        for(String name : hmStringsPW.keySet()){
            lstSeriesPW1.add(hmStringsPW.get(name));
            lstSeriesPW2.add(hmStringsPWND.get(name));
        }
        
/*        hmValuesPW = new HashMap<>();
        hmStringsPW = new HashMap<>();
        hmStringsPWND = new HashMap<>();
        // set to display genomic annotations
        for(DataAnnotation.DiversityData dd : lstPWStats) {
            if(dd.type.equals(AnnotationType.GENOMIC)) {
                String fields[] = dd.name.split(":");
                String name = fields[1];
                if(hmValuesPW.containsKey(name)){
                    //values
                    Integer[] val = new Integer[2];
                    HashMap<String, Integer[]> hmAux = hmValuesPW.get(name);
                    for(String name_aux : hmAux.keySet()){
                        val = hmAux.get(name_aux);
                        val[0] += dd.diff;
                        val[1] += dd.same;
                        hmAux.put(name_aux, val);
                    }
                    hmValuesPW.put(name, hmAux);
                    //description
                    pct = (double)val[0] / ((double)val[0] + (double)val[1]) * 100  * hmGenomic.get(hmNameSource.get(name)) / 100.0;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsPW.put(name, "Varying Genomic Feature: " + name + "\n" + pct + "% of pairwise isoforms with feature");
                    pct = (double)val[1] / ((double)val[0] + (double)val[1]) * 100  * hmGenomic.get(hmNameSource.get(name)) / 100.0;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsPWND.put(name, "Not-Varying Genomic Feature: " + name + "\n" + pct + "% of pairwise isoforms with feature");
                }else{
                    //values
                    HashMap<String, Integer[]> hmAux = new HashMap<>();
                    Integer[] val = new Integer[2];
                    val[0] = dd.diff;
                    val[1] = dd.same;
                    hmAux.put(name + "(G)", val);
                    hmValuesPW.put(name, hmAux);
                    //description
                    pct = (double)val[0] / ((double)val[0] + (double)val[1]) * 100  * hmGenomic.get(hmNameSource.get(name)) / 100.0;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    total = val[0] + val[1];
                    if(!hmStringsPW.containsKey(name)){
                        barItemsCountPW++;
                        dbPW.add(dd.name);
                    }
                    hmStringsPW.put(name, "Varying Genomic Feature: " + name + "\n" + pct + "% of pairwise isoforms with feature");
                    pct = (double)val[1] / ((double)val[0] + (double)val[1]) * 100  * hmGenomic.get(hmNameSource.get(name)) / 100.0;
                    pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                    hmStringsPWND.put(name, "Not-Varying Genomic Feature: " + name + "\n" + pct + "% of pairwise isoforms with feature");
                }
                //dbPW.add(dd.name);
            }
        }
        
        //updating genomic PW
        for(String name : hmValuesPW.keySet()){
            HashMap<String, Integer[]> hmAux = hmValuesPW.get(name);
            for(String name_aux : hmAux.keySet()){
                Integer[] val = hmAux.get(name_aux);
                pct = (double)val[0] / ((double)val[0] + (double)val[1]) * 100  * hmGenomic.get(hmNameSource.get(name)) / 100.0;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                seriesPW1.getData().add(new XYChart.Data(pct, name_aux));
                pct = (double)val[1] / ((double)val[0] + (double)val[1]) * 100  * hmGenomic.get(hmNameSource.get(name)) / 100.0;
                pct = Double.parseDouble(String.format("%.2f", ((double)Math.round(pct*100)/100.0)));
                seriesPW2.getData().add(new XYChart.Data(pct, name_aux));
            }
        }
        
        for(String name : hmStringsPW.keySet()){
            lstSeriesPW1.add(hmStringsPW.get(name));
            lstSeriesPW2.add(hmStringsPWND.get(name));
        }*/
        
        //print value
//        for(int i = 0; i< series1.getData().size(); i++){
//            final Data<Number, String> data = series1.getData().get(i);
//            data.nodeProperty().addListener(new ChangeListener<Node>() {
//              @Override public void changed(ObservableValue<? extends Node> ov, Node oldNode, final Node node) {
//                if (node != null) {
//                  displayLabelForData(data, hmTotal);
//                } 
//              }
//            });
//            series1aux.getData().add(data);
//        }
        
        for(int i = 0; i< series2.getData().size(); i++){
            final Data<Number, String> data = series2.getData().get(i);
            final Data<Number, String> dataVar = series1.getData().get(i);
            data.nodeProperty().addListener(new ChangeListener<Node>() {
              @Override public void changed(ObservableValue<? extends Node> ov, Node oldNode, final Node node) {
                if (node != null) {
                  displayLabelForData(data, dataVar, hmTotal);
                } 
              }
            });
            series2aux.getData().add(data);
        }
        
//        for(int i = 0; i< seriesPW1.getData().size(); i++){
//            final Data<Number, String> data = seriesPW1.getData().get(i);
//            data.nodeProperty().addListener(new ChangeListener<Node>() {
//              @Override public void changed(ObservableValue<? extends Node> ov, Node oldNode, final Node node) {
//                if (node != null) {
//                  displayLabelForData(data, hmTotal);
//                } 
//              }
//            });
//            seriesPW1aux.getData().add(data);
//        }
//        
//        for(int i = 0; i< seriesPW2.getData().size(); i++){
//            final Data<Number, String> data = seriesPW2.getData().get(i);
//            data.nodeProperty().addListener(new ChangeListener<Node>() {
//              @Override public void changed(ObservableValue<? extends Node> ov, Node oldNode, final Node node) {
//                if (node != null) {
//                  displayLabelForData(data, hmTotal);
//                } 
//              }
//            });
//            seriesPW2aux.getData().add(data);
//        }
        
        showGeneDiversityChart(series1, series2aux, lstSeries1, lstSeries2, barItemsCount, geneCount, db);
        //showGeneDiversityChart(series1, lstSeries1, barItemsCount, geneCount, db);
        showIsoformsDiversityChart(seriesPW1, seriesPW2, lstSeriesPW1, lstSeriesPW2, barItemsCountPW, geneCount, dbPW);
        //showIsoformsDiversityChart(seriesPW1, lstSeriesPW1, barItemsCountPW, geneCount, dbPW);
        
        setupExportMenu();
        setFocusNode(barGeneDiversity, true);
    }
    private void setupExportMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem item = new MenuItem("Export gene level diversity chart image...");
        String usingTitle = usingGenPos? "(Genomic Position)" : "(Presence)";
        String usingSave = usingGenPos? "genomicPosition" : "presence";
        item.setOnAction((event) -> {
            SnapshotParameters sP = new SnapshotParameters();
            double x = barGeneDiversity.getWidth();
            double ratio = 3840/x;
            barGeneDiversity.setScaleX(ratio);
            barGeneDiversity.setScaleY(ratio);
            WritableImage img = barGeneDiversity.snapshot(sP, null);
            //WritableImage img = Controls.pixelScaleAwareCanvasSnapshot(barGeneDiversity, 2);
            double newX = barGeneDiversity.getWidth();
            ratio = x/newX;
            barGeneDiversity.setScaleX(ratio);
            barGeneDiversity.setScaleY(ratio);
            
            //WritableImage img = barGeneDiversity.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Gene Level Annotation Features Diversity " + usingTitle, "tappAS_Genes_Diversity_" + usingSave + ".png", (Image)img);
        });
        cm.getItems().add(item);
        item = new MenuItem("Export pairwise isoforms level diversity chart image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = barIsoDiversity.getWidth();
            double ratio = 3840/x;
            barIsoDiversity.setScaleX(ratio);
            barIsoDiversity.setScaleY(ratio);
            WritableImage img = barIsoDiversity.snapshot(sP, null);
            double newX = barIsoDiversity.getWidth();
            ratio = x/newX;
            barIsoDiversity.setScaleX(ratio);
            barIsoDiversity.setScaleY(ratio);
            
            //WritableImage img = barIsoDiversity.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Pairwise Isoforms Level Annotation Features Diversity " + usingTitle, "tappAS_PairwiseIsoforms_Diversity_" + usingSave + ".png", (Image)img);
        });
        cm.getItems().add(item);
        cm.getItems().add(new SeparatorMenuItem());
        item = new MenuItem("Export subtab contents image...");
        item.setOnAction((event) -> { 
            SnapshotParameters sP = new SnapshotParameters();
            double x = paneContents.getWidth();
            double ratio = 3840/x;
            paneContents.setScaleX(ratio);
            paneContents.setScaleY(ratio);
            WritableImage img = paneContents.snapshot(sP, null);
            double newX = paneContents.getWidth();
            ratio = x/newX;
            paneContents.setScaleX(ratio);
            paneContents.setScaleY(ratio);
            
            //WritableImage img = paneContents.snapshot(new SnapshotParameters(), null);
            app.export.exportImage("Functional Annotation Features Diversity " + usingTitle, "tappAS_AnnotationFeatures_Diversity_Panel.png", (Image)img);
        });
        cm.getItems().add(item);
        subTabInfo.cmExport = cm;
    } 
    
    //Thank to jewelsea https://stackoverflow.com/questions/15237192/how-to-display-bar-value-on-top-of-bar-javafx 
    private void displayLabelForData(XYChart.Data<Number, String> data, XYChart.Data<Number, String> dataVar, HashMap<String, Integer> hmTotal) {
        final Node node = data.getNode();
        int l1 = data.getYValue().length();
        String name = data.getYValue().substring(0,l1-3);
        double value = data.getXValue().doubleValue();
        double valueVar = dataVar.getXValue().doubleValue();
        double totalValue = value + valueVar;
        Integer total = hmTotal.get(name);
        
        final Text dataText = new Text(total + "");
        if(totalValue>=95.0){
            dataText.setFill(Color.WHITE);
        }
        
        node.parentProperty().addListener(new ChangeListener<Parent>() {
          @Override public void changed(ObservableValue<? extends Parent> ov, Parent oldParent, Parent parent) {
            Group parentGroup = (Group) parent;
            parentGroup.getChildren().add(dataText);
          }
        });

        node.boundsInParentProperty().addListener(new ChangeListener<Bounds>() {
          @Override public void changed(ObservableValue<? extends Bounds> ov, Bounds oldBounds, Bounds bounds) {
            if(project.data.isCaseControlExpType()){
                dataText.setLayoutY(
                    Math.round(
                        bounds.getMaxY() + bounds.getHeight() / 2 - barItemsCount/2
                    )
                );
            }else{ // we have to create a new height because the barsize is different!
                dataText.setLayoutY(
                    Math.round(
                        bounds.getMaxY() + bounds.getHeight() / 2 - barItemsCount
                    )
                );
            }
            if(totalValue>=95.0){
                dataText.setLayoutX(
                    Math.round(
                        bounds.getMaxX() - dataText.prefWidth(-1) - 10
                    )
                );
            }else{
                dataText.setLayoutX(
                    Math.round(
                        bounds.getMaxX() + 5
                    )
                );
            }
            
          }
        });
    }
    
    private void showGeneDiversityChart(XYChart.Series<Number, String> series1, XYChart.Series<Number, String> series2, 
            ArrayList<String> lstSeries1, ArrayList<String> lstSeries2, int barItemsCount, int totalGenesCount, ArrayList<String> db) {
        
        spDiversity.widthProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            barGeneDiversity.setPrefWidth((double)newValue - 20);
            barGeneDiversity.setMaxWidth((double)newValue - 20);
        });
        barGeneDiversity.setPrefWidth((double)spDiversity.getWidth() - 20);
        barGeneDiversity.setMaxWidth((double)spDiversity.getWidth() - 20);
        barGeneDiversity.getYAxis().setLabel("Features");
        barGeneDiversity.getXAxis().setLabel("Gene % with feature out of " + totalGenesCount + " Multiple Isoform Genes");
        barGeneDiversity.setCategoryGap(5);
        
        //Create new series with the correct order
        ChartPlusNames aux1 = shortSerieByFeature(series1, db);
        ChartPlusNames aux2 = shortSerieByFeature(series2, db);
        
        //db sorted
        db = aux1.db;
        
        XYChart.Series<Number, String> struc1 = aux1.funct;
        XYChart.Series<Number, String> struc2 = aux2.funct;
        barGeneDiversity.getData().addAll(struc1, struc2);
        changeColorStackBar(barGeneDiversity, struc1, struc2, db);
        addFeatureLegend(barGeneDiversity);
        
        double h = 120 + Math.max(100, (barItemsCount + 2) * 20);
        barGeneDiversity.setPrefHeight(h);
        barGeneDiversity.setMaxHeight(h);
        Tooltip tt;
        for(int i = 0; i < struc1.getData().size(); i++) {
            int pos = 0;
            int l1 = struc1.getData().get(i).getYValue().length();
            String n1 = (String) struc1.getData().get(i).getYValue().subSequence(0, l1-3);
            for(int j = 0; j < series1.getData().size(); j++) {
                int l2 = series1.getData().get(j).getYValue().length();
                String n2 = (String) series1.getData().get(j).getYValue().subSequence(0, l2-3);
                if(n1.equals(n2)){
                    pos = j;
                    break;
                }
            }
            XYChart.Data item = struc1.getData().get(i);
            tt = new Tooltip(lstSeries1.get(pos));
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
            //item.setYValue(n1.split(":")[1] + (String) struc1.getData().get(i).getYValue().subSequence(l1-3, l1));
        }
        for(int i = 0; i < struc2.getData().size(); i++) {
            int pos = 0;
            int l1 = struc2.getData().get(i).getYValue().length();
            String n1 = (String) struc2.getData().get(i).getYValue().subSequence(0, l1-3);
            for(int j = 0; j < series2.getData().size(); j++) {
                int l2 = series2.getData().get(j).getYValue().length();
                String n2 = (String) series2.getData().get(j).getYValue().subSequence(0, l2-3);
                if(n1.equals(n2)){
                    pos = j;
                    break;
                }
            }
            XYChart.Data item = struc2.getData().get(i);
            tt = new Tooltip(lstSeries2.get(pos));
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
            //item.setYValue(n1.split(":")[1] + (String) struc2.getData().get(i).getYValue().subSequence(l1-3, l1));
        }
        String usingTitle = usingGenPos? "(Genomic Position)" : "(Presence)";
        String usingSave = usingGenPos? "genomicPosition" : "presence";
        app.ctls.setupChartExport(barGeneDiversity, "Genes Diversity " + usingTitle, "tappAS_Genes_Diversity_" + usingSave + ".png", null);
    }
    
    private void showGeneDiversityChart(XYChart.Series<Number, String> series1, ArrayList<String> lstSeries1, int barItemsCount, int totalGenesCount, ArrayList<String> db) {
        
        spDiversity.widthProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            barGeneDiversity.setPrefWidth((double)newValue - 20);
            barGeneDiversity.setMaxWidth((double)newValue - 20);
        });
        barGeneDiversity.setPrefWidth((double)spDiversity.getWidth() - 20);
        barGeneDiversity.setMaxWidth((double)spDiversity.getWidth() - 20);
        barGeneDiversity.getYAxis().setLabel("Features");
        barGeneDiversity.getXAxis().setLabel("Gene % with feature out of " + totalGenesCount + " Multiple Isoform Genes");
        
        //Create new series with the correct order
        ChartPlusNames aux1 = shortSerieByFeature(series1, db);
        
        //db sorted
        db = aux1.db;
        
        XYChart.Series<Number, String> struc1 = aux1.funct;
        barGeneDiversity.getData().addAll(struc1);
        changeColorStackBar(barGeneDiversity, struc1, db);
        colorFeatureLegend(barGeneDiversity);
        
        double h = 120 + Math.max(100, (barItemsCount + 2) * 20);
        barGeneDiversity.setPrefHeight(h);
        barGeneDiversity.setMaxHeight(h);
        Tooltip tt;
        for(int i = 0; i < struc1.getData().size(); i++) {
            int pos = 0;
            int l1 = struc1.getData().get(i).getYValue().length();
            String n1 = (String) struc1.getData().get(i).getYValue().subSequence(0, l1-3);
            for(int j = 0; j < series1.getData().size(); j++) {
                int l2 = series1.getData().get(j).getYValue().length();
                String n2 = (String) series1.getData().get(j).getYValue().subSequence(0, l2-3);
                if(n1.equals(n2)){
                    pos = j;
                    break;
                }
            }
            XYChart.Data item = struc1.getData().get(i);
            tt = new Tooltip(lstSeries1.get(pos));
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
            //item.setYValue(n1.split(":")[1] + (String) struc1.getData().get(i).getYValue().subSequence(l1-3, l1));
        }
        String usingTitle = usingGenPos? "(Genomic Position)" : "(Presence)";
        String usingSave = usingGenPos? "genomicPosition" : "presence";
        app.ctls.setupChartExport(barGeneDiversity, "Genes Diversity " + usingTitle, "tappAS_Genes_Diversity_"+usingSave+".png", null);
    }
    
    private void showIsoformsDiversityChart(XYChart.Series<Number, String> series1, XYChart.Series<Number, String> series2, 
            ArrayList<String> lstSeries1, ArrayList<String> lstSeries2, int barItemsCount, int totalGenesCount, ArrayList<String> db) {

        spDiversity.widthProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            barIsoDiversity.setPrefWidth((double)newValue - 20);
            barIsoDiversity.setMaxWidth((double)newValue - 20);
        });
        barIsoDiversity.setPrefWidth((double)spDiversity.getWidth() - 20);
        barIsoDiversity.setMaxWidth((double)spDiversity.getWidth() - 20);
        barIsoDiversity.getYAxis().setLabel("Features");
        barIsoDiversity.getXAxis().setLabel("Isoform pairs % that correspond to feature-annotated multi-isoform genes");
        barIsoDiversity.setCategoryGap(5);

        //Create new series with the correct order
        ChartPlusNames aux1 = shortSerieByFeature(series1, db);
        ChartPlusNames aux2 = shortSerieByFeature(series2, db);
        
        //db sorted
        db = aux1.db;
        
        XYChart.Series<Number, String> struc1 = aux1.funct;
        XYChart.Series<Number, String> struc2 = aux2.funct;
        barIsoDiversity.getData().addAll(struc1, struc2);
        changeColorStackBar(barIsoDiversity, struc1, struc2, db);
        addFeatureLegend(barIsoDiversity);

        double h = 100 + Math.max(100, (barItemsCount + 2) * 20);
        barIsoDiversity.setPrefHeight(h);
        barIsoDiversity.setMaxHeight(h);
        
        Tooltip tt;
        
        for(int i = 0; i < struc1.getData().size(); i++) {
            int pos = 0;
            int l1 = struc1.getData().get(i).getYValue().length();
            String n1 = (String) struc1.getData().get(i).getYValue().subSequence(0, l1-3);
            for(int j = 0; j < series1.getData().size(); j++) {
                int l2 = series1.getData().get(j).getYValue().length();
                String n2 = (String) series1.getData().get(j).getYValue().subSequence(0, l2-3);
                if(n1.equals(n2)){
                    pos = j;
                    break;
                }
            }
            XYChart.Data item = struc1.getData().get(i);
            tt = new Tooltip(lstSeries1.get(pos));
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
        for(int i = 0; i < struc2.getData().size(); i++) {
            int pos = 0;
            int l1 = struc2.getData().get(i).getYValue().length();
            String n1 = (String) struc2.getData().get(i).getYValue().subSequence(0, l1-3);
            for(int j = 0; j < series2.getData().size(); j++) {
                int l2 = series2.getData().get(j).getYValue().length();
                String n2 = (String) series2.getData().get(j).getYValue().subSequence(0, l2-3);
                if(n1.equals(n2)){
                    pos = j;
                    break;
                }
            }
            XYChart.Data item = struc2.getData().get(i);
            tt = new Tooltip(lstSeries2.get(pos));
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
        
        String usingTitle = usingGenPos? "(Genomic Position)" : "(Presence)";
        String usingSave = usingGenPos? "genomicPosition" : "presence";
        app.ctls.setupChartExport(barIsoDiversity, "Pairwise Isoforms Level Diversity " + usingTitle, "tappAS_Pairwise_Isoforms_Diversity_"+usingSave+".png", null);
    }
    
    private void showIsoformsDiversityChart(XYChart.Series<Number, String> series1, ArrayList<String> lstSeries1, int barItemsCount, int totalGenesCount, ArrayList<String> db) {

        spDiversity.widthProperty().addListener((ObservableValue<? extends Number> ov, Number oldValue, Number newValue) -> {
            barIsoDiversity.setPrefWidth((double)newValue - 20);
            barIsoDiversity.setMaxWidth((double)newValue - 20);
        });
        barIsoDiversity.setPrefWidth((double)spDiversity.getWidth() - 20);
        barIsoDiversity.setMaxWidth((double)spDiversity.getWidth() - 20);
        barIsoDiversity.getYAxis().setLabel("Features");
        barIsoDiversity.getXAxis().setLabel("Isoform pairs % that correspond to feature-annotated multi-isoform genes");
        
        //Create new series with the correct order
        ChartPlusNames aux1 = shortSerieByFeature(series1, db);
        
        //db sorted
        db = aux1.db;
        
        XYChart.Series<Number, String> struc1 = aux1.funct;
        barIsoDiversity.getData().addAll(struc1);
        changeColorStackBar(barIsoDiversity, struc1, db);
        colorFeatureLegend(barIsoDiversity);

        double h = 100 + Math.max(100, (barItemsCount + 2) * 20);
        barIsoDiversity.setPrefHeight(h);
        barIsoDiversity.setMaxHeight(h);
        
        Tooltip tt;
        
        for(int i = 0; i < struc1.getData().size(); i++) {
            int pos = 0;
            int l1 = struc1.getData().get(i).getYValue().length();
            String n1 = (String) struc1.getData().get(i).getYValue().subSequence(0, l1-3);
            for(int j = 0; j < series1.getData().size(); j++) {
                int l2 = series1.getData().get(j).getYValue().length();
                String n2 = (String) series1.getData().get(j).getYValue().subSequence(0, l2-3);
                if(n1.equals(n2)){
                    pos = j;
                    break;
                }
            }
            XYChart.Data item = struc1.getData().get(i);
            tt = new Tooltip(lstSeries1.get(pos));
            tt.setStyle("-fx-font: 13 system;");
            Tooltip.install(item.getNode(), tt);
        }
        
        String usingTitle = usingGenPos? "(Genomic Position)" : "(Presence)";
        String usingSave = usingGenPos? "genomicPosition" : "presence";
        app.ctls.setupChartExport(barIsoDiversity, "Pairwise Isoforms Level Diversity " + usingTitle, "tappAS_Pairwise_Isoforms_Diversity_"+usingSave+".png", null);
    }
    
    private void changeColorStackBar(StackedBarChart bar, XYChart.Series<Number, String> struc1, XYChart.Series<Number, String> struc2, ArrayList<String> db){
        HashMap<String, HashMap<String, String>> hmType = project.data.getAFStatsType();
        
        for(int i = 0; i< struc1.getData().size(); i++){
            int l = struc1.getData().get(i).getYValue().length();
            String aux = (String) struc1.getData().get(i).getYValue().subSequence(0, l-3);
            String s = db.get(i);//(String) struc1.getData().get(i).getYValue().subSequence(0, l-3);
            
            String source = s.split(":")[0];
            String feature = s.split(":")[1];
            
            String group = hmType.get(source).get(feature);
            AnnotationType type = AnnotationType.TRANS;
            if(AnnotationType.TRANS.name().startsWith(group)) //TRANSCRIPT
                type = AnnotationType.TRANS;
            else if(AnnotationType.PROTEIN.name().startsWith(group)) //PROTEIN
                type = AnnotationType.PROTEIN;
            else if(AnnotationType.GENOMIC.name().startsWith(group)) //GENOMIC
                type = AnnotationType.GENOMIC;
            
            Set<Node> n = bar.lookupAll(".data" + i);
            Node n1 = null;
            Node n2 = null;
            
            for (Iterator<Node> it = n.iterator(); it.hasNext(); ) {
                //always a couple of data
                n1 = it.next();
                n2 = it.next();
            }
            //Node n2 = barIsoDiversity.lookupAll(".data" + i);
            
            if(type.name().equals(AnnotationType.GENOMIC.name())){
                n1.setStyle("-fx-bar-fill: #cea856");
                n2.setStyle("-fx-bar-fill: #f7c967");
            }
            //TRANSCRIPT
            if(type.name().equals(AnnotationType.TRANS.name())){
                n1.setStyle("-fx-bar-fill: #bc352f");
                n2.setStyle("-fx-bar-fill: #f2635c");
            }
            //PROTEIN
            if(type.name().equals(AnnotationType.PROTEIN.name())){
                n1.setStyle("-fx-bar-fill: #648dad");
                n2.setStyle("-fx-bar-fill: #7cafd6");
            }
        }
    }
    
    private void changeColorStackBar(StackedBarChart bar, XYChart.Series<Number, String> struc1, ArrayList<String> db){
        HashMap<String, HashMap<String, String>> hmType = project.data.getAFStatsType();
        
        for(int i = 0; i< struc1.getData().size(); i++){
            int l = struc1.getData().get(i).getYValue().length();
            String aux = (String) struc1.getData().get(i).getYValue().subSequence(0, l-3);
            String s = db.get(i);//(String) struc1.getData().get(i).getYValue().subSequence(0, l-3);
            
            String source = s.split(":")[0];
            String feature = s.split(":")[1];
            
            String group = hmType.get(source).get(feature);
            AnnotationType type = AnnotationType.TRANS;
            if(AnnotationType.TRANS.name().startsWith(group)) //TRANSCRIPT
                type = AnnotationType.TRANS;
            else if(AnnotationType.PROTEIN.name().startsWith(group)) //PROTEIN
                type = AnnotationType.PROTEIN;
            else if(AnnotationType.GENOMIC.name().startsWith(group)) //GENOMIC
                type = AnnotationType.GENOMIC;
            
            Set<Node> n = bar.lookupAll(".data" + i);
            Node n1 = null;
            
            for (Iterator<Node> it = n.iterator(); it.hasNext(); ) {
                //always a couple of data
                n1 = it.next();
            }
            //Node n2 = barIsoDiversity.lookupAll(".data" + i);
            
            if(type.name().equals(AnnotationType.GENOMIC.name())){
                n1.setStyle("-fx-bar-fill: #cea856");
            }
            //TRANSCRIPT
            if(type.name().equals(AnnotationType.TRANS.name())){
                n1.setStyle("-fx-bar-fill: #bc352f");
            }
            //PROTEIN
            if(type.name().equals(AnnotationType.PROTEIN.name())){
                n1.setStyle("-fx-bar-fill: #648dad");
            }
        }
    }
    
    private void addFeatureLegend(StackedBarChart bar){
        //Change color - genomic = yellow / transcript = red / protein = blue / functional = green + 2 extra colors to dvierse and not diverse
        //Color[] colors = new Color[]{Controls.chartColors[0], Controls.chartColors[1], Controls.chartColors[3], Color.DIMGRAY, Color.LIGHTGRAY};
        Color[] colors = new Color[]{Controls.chartColors[0], Controls.chartColors[1], Color.DIMGRAY, Color.LIGHTGRAY};

        //There are 2 series, we need(at least) 4 more//
        for(Node n : bar.getChildrenUnmodifiable()) {
            if (n instanceof Legend) {
                Legend l = (Legend) n;
                for(int i = 0; i<app.data.getFeatureTypeDiversityLength(); i++){
                    LegendItem e = new LegendItem(app.data.getFeatureTypeDiversity(i));
                    l.getItems().add(i,e);
                }
            }
        }
        
        Set<Node> items = bar.lookupAll("Label.chart-legend-item");
        int aux = 0;
        boolean flag = true; //first label as diverse, next as not diverse
        for (Node item : items) {
          item.autosize();
          Label label = (Label) item;
          final Rectangle rectangle = new Rectangle(10, 10, colors[aux]);
          
          label.setAlignment(Pos.CENTER);
          label.setCenterShape(true);            
          
          String name = app.data.getFeatureTypeDiversity(aux);
          if(!name.equals("")){
            label.setText(name);
            label.setGraphic(rectangle);
          }else{
            Glow niceEffect = new Glow();
            niceEffect.setInput(new Reflection());
            rectangle.setEffect(niceEffect);
            label.setGraphic(rectangle);
            //Text
            if(flag){
                label.setText("Varying");
                flag = false;
            }else{
                label.setText("Not-Varying");
            }
          }
          aux++;
        }
    }
    
    private void colorFeatureLegend(StackedBarChart bar){
        //Change color - genomic = yellow / transcript = red / protein = blue / functional = green + 2 extra colors to dvierse and not diverse
        //Color[] colors = new Color[]{Controls.chartColors[0], Controls.chartColors[1], Controls.chartColors[3]};
        Color[] colors = new Color[]{Controls.chartColors[0], Controls.chartColors[1]};

        //There are 2 series, we need(at least) 4 more//
        for(Node n : bar.getChildrenUnmodifiable()) {
            if (n instanceof Legend) {
                Legend l = (Legend) n;
                for(int i = 0; i<app.data.getFeatureTypeDiversityLength()-1; i++){
                    LegendItem e = new LegendItem(app.data.getFeatureTypeDiversity(i));
                    l.getItems().add(i,e);
                }
            }
        }
        
        Set<Node> items = bar.lookupAll("Label.chart-legend-item");
        int aux = 0;
        for (Node item : items) {
            item.autosize();
            Label label = (Label) item;
            final Rectangle rectangle = new Rectangle(10, 10, colors[aux]);

            label.setAlignment(Pos.CENTER);
            label.setCenterShape(true);            

            String name = app.data.getFeatureTypeDiversity(aux);
            if(!name.equals("")){
              label.setText(name);
              label.setGraphic(rectangle);
              aux++;
            }
            
        }
    }
    
    private ChartPlusNames shortSerieByFeature(XYChart.Series<Number, String> series1, ArrayList<String> db){
        
        ArrayList<String> db_sortedG = new ArrayList<>();
        ArrayList<String> db_sortedP = new ArrayList<>();
        ArrayList<String> db_sortedT = new ArrayList<>();
        ArrayList<String> db_res = new ArrayList<>();
        
        HashMap<String, HashMap<String, String>> hmType = project.data.getAFStatsType();
        //Organizing all series
        XYChart.Series<Number, String> geno1 = new XYChart.Series<>();
        XYChart.Series<Number, String> trans1 = new XYChart.Series<>();
        XYChart.Series<Number, String> prote1 = new XYChart.Series<>();
        XYChart.Series<Number, String> funct1 = new XYChart.Series<>();
        
        for(int i = 0; i < series1.getData().size(); i++) {
            int l = series1.getData().get(i).getYValue().length();
            String aux = (String) series1.getData().get(i).getYValue().subSequence(0, l-3);
            String s = db.get(i);//(String) series1.getData().get(i).getYValue().subSequence(0, l-3);
            String source = s.split(":")[0];
            String feature = s.split(":")[1];
            
            String group = hmType.get(source).get(feature);
            
            if(AnnotationType.TRANS.name().startsWith(group)){ //TRANSCRIPT
                trans1.getData().add(series1.getData().get(i));
                db_sortedT.add(s);
            }else if(AnnotationType.PROTEIN.name().startsWith(group)){ //PROTEIN
                prote1.getData().add(series1.getData().get(i));
                db_sortedP.add(s);
            }else if(AnnotationType.GENOMIC.name().startsWith(group)){ //GENOMIC
                geno1.getData().add(series1.getData().get(i));
                db_sortedG.add(s);
            }
        }
        
        db_res.addAll(db_sortedG);
        db_res.addAll(db_sortedP);
        db_res.addAll(db_sortedT);
        
        funct1.getData().addAll(geno1.getData());
        funct1.getData().addAll(prote1.getData());
        funct1.getData().addAll(trans1.getData());
        
        ChartPlusNames res = new ChartPlusNames(funct1, db_res);
        
        return res;
    }
    
    //
    // Data Load
    //
    @Override
    protected void runDataLoadThread() {
        DataAnnotation.DiversityResults fdaSummaryResults = fdaData.getFDAResultsSummaryData(analysisId);
        showSubTab(fdaSummaryResults);
    }
    
    //
    //Data Classes
    //
    
    public static class ChartPlusNames {
        public final XYChart.Series<Number, String> funct;
        public final ArrayList<String> db;
        
        public ChartPlusNames(){
            funct = null;
            db = null;
        }
        
        public ChartPlusNames(XYChart.Series<Number, String> funct, ArrayList<String> db){
            this.funct = funct;
            this.db = db;
        }
    }
    
    public static class VarSummaryData {
        public final SimpleStringProperty region;
        public final SimpleIntegerProperty genes;
        public final SimpleDoubleProperty pct;

        public VarSummaryData(String region, int genes, double pct) {
            this.region = new SimpleStringProperty(region);
            this.genes = new SimpleIntegerProperty(genes);
            this.pct = new SimpleDoubleProperty(pct);
        }
        public String getRegion() { return region.get(); }
        public int getGenes() { return genes.get(); }
        public double getPct() { return pct.get(); }
    }
}
