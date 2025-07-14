/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */

public class GO extends AppObject {
    static public enum GOCat {
        BP, MF, CC
    }
    static public String GOSLIM_GENERIC = "goslim_generic";
    static String BP = "GO:0008150";
    static String MF = "GO:0003674";
    static String CC = "GO:0005575";
    
    // category flags
    static public int FLG_BP = 0x0001;
    static public int FLG_MF = 0x0002;
    static public int FLG_CC = 0x0004;
    
    // ancestor flags
    static int FLG_GOSLIM = 0x0001;
    static int FLG_PARTOF = 0x0002;
    static int FLG_MASK = 0x00FF;
    static int FLG_ROOT = 0x0100;

    HashMap<String, String> goSubsets = new HashMap<>();
    HashMap<String, NamespaceInfo> goNamespaces = new HashMap<>();
    int idxBP = -1;
    int idxMF = -1;
    int idxCC = -1;
    HashMap<String, GOTerm> goTerms = new HashMap<>();
    ArrayList<HashMap<String, GOTerm>> goCategoryTerms = new ArrayList<>();

    public GO() {
        super(null, null);
    }

    //
    // Public Interface
    //
    public String getCategoryNamespace(int catidx) {
        String catns = "";
        for(String namespace : goNamespaces.keySet()) {
            if(goNamespaces.get(namespace).idx == catidx) {
                catns = namespace;
                break;
            }
        }
        return catns;
    }
    /*
    public HashMap<String, NamespaceInfo> getNamespaces() {
        if(goTerms.isEmpty())
            loadGOTerms(Paths.get(app.data.getAppGOTermsFilepath()));
        return goNamespaces;
    }*/
    public GOTerm getGOTermInfo(String term) {
        GOTerm gt = null;
        if(goTerms.isEmpty())
            loadGOTerms(Paths.get(app.data.getAppGOTermsFilepath()));
        if(goTerms.containsKey(term))
            gt = goTerms.get(term);
        return gt;
    }
    // Even though the function is allowing multiple categories, the application will support displaying only one at a time
    // this minimizes clutter and avoids top/bottom-edgeArrow issue with dagre-d3
    public static enum Show { Id, Name, Both }
    public GraphResult buildGraph(GOCat cat, int maxLevel, ArrayList<String> lstGOTerms, double width, double height, Show show) {
        String goSlim = GOSLIM_GENERIC;
        GraphResult result = new GraphResult(0, "", false, false, false);
        String htmlNodes = "";
        String htmlEdges = "";
        if(goTerms.isEmpty())
            loadGOTerms(Paths.get(app.data.getAppGOTermsFilepath()));
        HashMap<String, HashMap<String, TermInfo>> hmGraph = new HashMap<>();
        HashMap<String, Integer> hmNodes = new HashMap<>();
        ArrayList<HashMap<String, Object>> lstGlobalLevelTerms = new ArrayList<>();
        // there is an issue with the fact that multiple terms share the same ancestors so the distance is different for each
        ArrayList<Integer> lstDistances = new ArrayList<>();
        for(int i = 0; i < goNamespaces.size(); i++) //lstDistances(i)!!!
            lstDistances.add(0);
        int idx;
        ArrayList<Integer> lstNewDistances = new ArrayList<>();
        ArrayList<Integer> lstMaxDistances = new ArrayList<>();
        for(Integer dist : lstDistances)
            lstMaxDistances.add(dist);
        for(String term : lstGOTerms) {
            ArrayList<ArrayList<HashMap<String, Object>>> lstCatLevelTerms = new ArrayList<>();
            for(int i = 0; i < goNamespaces.size(); i++)
                lstCatLevelTerms.add(new ArrayList(0));
            lstNewDistances.clear();
            for(Integer dist : lstDistances)
                lstNewDistances.add(dist);
            getTermGraph(term, hmGraph, cat, goSlim, lstNewDistances, lstCatLevelTerms, false);
            idx = 0;
            for(Integer dist : lstNewDistances) {
                if(dist > lstMaxDistances.get(idx))
                    lstMaxDistances.set(idx, dist);
                idx++;
            }
            int catidx = 0;
            for(ArrayList<HashMap<String, Object>> lstLevelTerms : lstCatLevelTerms) {
                Collections.reverse(lstLevelTerms);
                int level = 0;
                for(HashMap<String, Object> hmLevel : lstLevelTerms) {
                    // must be incremental adding so only add one
                    if(lstGlobalLevelTerms.size() < (level +1))
                        lstGlobalLevelTerms.add(new HashMap<>());
                    HashMap<String, Object> hmOutLevel = lstGlobalLevelTerms.get(level);
                    //System.out.println("Level " + level + " for " + getCategoryNamespace(catidx) + ": " + hmLevel.toString());
                    for(String levelTerm : hmLevel.keySet())
                        hmOutLevel.put(levelTerm, null);
                    level++;
                }
                catidx++;
            }
        }
        // if the user does not request bp then bp is false!!!
        // to get a true answer need to traverse all terms for all categories - that's expensive!
        //System.out.println("Final max distances: ");
        for(String namespace : goNamespaces.keySet()) {
            int md = lstMaxDistances.get(goNamespaces.get(namespace).idx);
            if(md > 0) {
                if(goNamespaces.get(namespace).term.equals(BP))
                    result.bp = true;
                else if(goNamespaces.get(namespace).term.equals(MF))
                    result.mf = true;
                else if(goNamespaces.get(namespace).term.equals(CC))
                    result.cc = true;
            }
            //System.out.println("Max distance for " + namespace + ": " + md);
        }
        //System.out.println("Global level terms: ");
        int level = 0;
        //for(HashMap<String, Object> hmLevel : lstGlobalLevelTerms) {
        //    System.out.println("Level " + level + ": " + hmLevel.toString());
        //    level++;
        //}
        ArrayList<HashMap<String, Object>> lstFinalLevelTerms = new ArrayList<>();
        for(int i = 0; i < lstGlobalLevelTerms.size(); i++)
            lstFinalLevelTerms.add(new HashMap<>());
        level = 0;
        // need to either update lstGlobalLevelTerms or xfer to new list - some issues with both approaches
        for(HashMap<String, Object> hmLevel : lstGlobalLevelTerms) {
            HashMap<String, Object> hmFinal = lstFinalLevelTerms.get(level);
            for(String term : hmLevel.keySet()) {
                hmFinal.put(term, null);
                // the terms are moved to the next level in lstFinalLevelTerms but we are checkng lstFinalLevelTerms!
                moveOtherTermChildrenUp(term, lstGlobalLevelTerms, lstFinalLevelTerms, level+1); 
            }
            level++;
        }
        int len = lstFinalLevelTerms.size();
        boolean rmvflg = true;
        do {
            if(len > 1) {
                HashMap<String, Object> hmLast = lstFinalLevelTerms.get(len-1);
                HashMap<String, Object> hmPrev = lstFinalLevelTerms.get(len-2);
                for(String term : hmLast.keySet()) {
                    if(!hmPrev.containsKey(term)) {
                        rmvflg = false;
                        break;
                    }
                }
                if(rmvflg)
                    lstFinalLevelTerms.remove(len-1);
                len = lstFinalLevelTerms.size();
            }
            else
                rmvflg = false;
        } while(rmvflg);
        result.maxLevel = lstFinalLevelTerms.size() - 1;
        
        int node = 0;
        for(String term : hmGraph.keySet()) {
            boolean skip = true;
            int skipLevel = 0;
            for(HashMap<String, Object> hmLevel : lstFinalLevelTerms) {
                if(hmLevel.containsKey(term)) {
                    skip = false;
                    break;
                }
                if(++skipLevel > maxLevel)
                    break;
            }
            if(skip)
                continue;
            
            String name = getGOTermInfo(term).name;
            String label = "";
            if(name.length() > 20) {
                ArrayList<String> lines = new ArrayList<>();
                String fields[] = name.split(" ");
                for(String word : fields) {
                    if((label.length() + word.length()) < 22)
                        label += label.isEmpty()? word : " " + word;
                    else {
                        if(word.length() > 25) {
                            String subFields[] = word.split(",");
                            for(String subWord : subFields) {
                                if((label.length() + subWord.length()) < 22)
                                    label += label.isEmpty()? subWord : "," + subWord;
                                else {
                                    if(!label.isEmpty())
                                        lines.add(label);
                                    label = subWord;
                                }
                            }
                        }
                        else {
                            if(!label.isEmpty())
                                lines.add(label);
                            label = word;
                        }
                    }
                }
                if(!lines.isEmpty()) {
                    String lastLine = label;
                    label = "";
                    // pad it with a couple of spaces, the text will overflow the rectangle some times while zooming (d3js)
                    for(String line : lines)
                        label += label.isEmpty()? line + "  " : "\\n" + line + "  ";
                    label += "\\n" + lastLine;
                }
            }
            else
                label = name;

            String nodeClass = "";
            String slim = "";
            GOTerm gt = getGOTermInfo(term);
            if(gt.isRoot())
                nodeClass += nodeClass.isEmpty()? "type-RootNode" : " " + "type-RootNode";
            if(gt.lstSubset.contains(goSlim)) {
                slim = ", slim: \"" + goSlim + "\"";
                nodeClass += nodeClass.isEmpty()? "type-SlimNode" : " " + "type-SlimNode";
            }
            if(lstGOTerms.contains(term))
                nodeClass += nodeClass.isEmpty()? "type-EndNode" : " " + "type-EndNode";
            if(!nodeClass.isEmpty())
                nodeClass = ", class: \"" + nodeClass + "\"";
            String tooltip = term + "\\n" + gt.namespace + (slim.isEmpty()? "" : " - " + goSlim);
            tooltip += "\\n" + wrapLine(gt.def.replace("\"", ""), 60);
            switch(show) {
                case Id:
                    label = term + " ";
                    break;
                case Name:
                    label += " ";
                    break;
                case Both:
                    label = term + "\\n" + label + " ";
                    break;
            }
            htmlNodes += "{id: " + node + ", args: " + "{label: \"" + label + "\"" + nodeClass + slim + ", tooltip: \"" + tooltip + "\"}},\n";
            hmNodes.put(term, node++);
        }
        for(String term : hmGraph.keySet()) {
            if(hmNodes.containsKey(term)) {
                int nodeidx = hmNodes.get(term);
                HashMap<String, TermInfo> hmLinks = hmGraph.get(term);
                GOTerm gt;
                for(String subTerm : hmLinks.keySet()) {
                    if(hmNodes.get(subTerm) != null) {
                        gt = getGOTermInfo(term); //hmLinks.get(subTerm);
                        if(gt.hmPartOf.containsKey(subTerm))
                            htmlEdges += "{source: " + nodeidx + ", target: " + hmNodes.get(subTerm) + ", args: {class: 'edgePathPartOf', lineInterpolate: 'basis'}},\n";
                        else
                            htmlEdges += "{source: " + nodeidx + ", target: " + hmNodes.get(subTerm) + ", args: {lineInterpolate: 'basis'}},\n";
                    }
                }
            }
        }
        String htmlGraph = app.data.getFileContentFromResource("/tappas/scripts/GODAG.html");
        // edge arrows are lost when using the base tag (internal library reference) - so don't use for now
        String htmlRankSep = "60";
        if(node > 200)
            htmlRankSep = "100";
        else if(node > 50)
            htmlRankSep = "80";
        htmlGraph = htmlGraph.replace("<<ranksep>>", htmlRankSep);
        htmlGraph = htmlGraph.replace("<<d3nodes>>", htmlNodes);
        htmlGraph = htmlGraph.replace("<<d3edges>>", htmlEdges);
        htmlGraph = htmlGraph.replace("<<width>>", width + "");
        htmlGraph = htmlGraph.replace("<<height>>", height + "");
        result.html = htmlGraph;
        return result;
    }
    public HashMap<String, TermInfo> getTermAncestors(String term) {
        HashMap<String, TermInfo> hmTermAncestors = new HashMap<>();
        if(goTerms.isEmpty())
            loadGOTerms(Paths.get(app.data.getAppGOTermsFilepath()));
        if(goTerms.containsKey(term))
            getTermAncestors(term, hmTermAncestors, "", 0, true, true);
        return hmTermAncestors;
    }
    public HashMap<String, HashMap<String, Object>> getSlims(ArrayList<String> terms, String slim) {
        if(slim == null || slim.isEmpty())
            slim = GOSLIM_GENERIC;
        HashMap<String, HashMap<String, Object>> hmSlims = new HashMap<>();
        if(goTerms.isEmpty())
            loadGOTerms(Paths.get(app.data.getAppGOTermsFilepath()));
        for(String term : terms) {
            if(goTerms.containsKey(term))
                getTermSlims(term, term, hmSlims, slim);
            else {
                System.out.println("Warning: GO term '" + term + "' not found.");
                if(!hmSlims.containsKey("TERM_NOT_FOUND"))
                    hmSlims.put("TERM_NOT_FOUND", new HashMap<>());
                hmSlims.get("TERM_NOT_FOUND").put(term, null);
            }
        }
        return hmSlims;
    }
    public HashMap<String, HashMap<String, Object>> getPartsOf(ArrayList<String> terms) {
        HashMap<String, HashMap<String, Object>> hmPartsOf = new HashMap<>();
        if(goTerms.isEmpty())
            loadGOTerms(Paths.get(app.data.getAppGOTermsFilepath()));
        for(String term : terms) {
            if(goTerms.containsKey(term))
                getTermPartsOf(term, term, hmPartsOf);
            else {
                System.out.println("Warning: GO term '" + term + "' not found.");
                if(!hmPartsOf.containsKey("TERM_NOT_FOUND"))
                    hmPartsOf.put("TERM_NOT_FOUND", new HashMap<>());
                hmPartsOf.get("TERM_NOT_FOUND").put(term, null);
            }
        }
        return hmPartsOf;
    }
    public HashMap<String, Object> getChildren(HashMap<String, Object> hmAncestors, int catidx, boolean allowAncestorChild) {
        HashMap<String, Object> hmChildren = new HashMap<>();
        HashMap<String, GOTerm> hmTerms = goCategoryTerms.get(catidx);
        hmAncestors.keySet().stream().forEach((ancestor) -> {
            hmTerms.keySet().stream().forEach((term) -> {
                if(allowAncestorChild || !hmAncestors.containsKey(term)) {
                    GOTerm gt = hmTerms.get(term);
                    if (gt.hmIsA.containsKey(ancestor) || gt.hmPartOf.containsKey(ancestor)) {
                        hmChildren.put(term, null);
                    }
                }
            });
        });
        return hmChildren;
    }
    
    //
    // Internal Functions
    //
    
    private void  moveOtherTermChildrenUp(String term, ArrayList<HashMap<String, Object>> lstGlobalLevelTerms, 
            ArrayList<HashMap<String, Object>> lstFinalLevelTerms, int level) {
        for(int chkLevel = level; chkLevel < lstGlobalLevelTerms.size(); chkLevel++) {
            if(lstGlobalLevelTerms.get(chkLevel).containsKey(term)) {
                int nxtLevel = chkLevel + 1;
                if(nxtLevel < lstGlobalLevelTerms.size()) {
                    HashMap<String, Object> hmChildren = lstGlobalLevelTerms.get(nxtLevel);
                    for(String child : hmChildren.keySet()) {
                        GOTerm gt = getGOTermInfo(child);
                        if(gt.hmIsA.containsKey(term) || gt.hmPartOf.containsKey(term)) {
                            //System.out.println("Moved child " + child + " for term " + term + " to level " + level);
                            lstFinalLevelTerms.get(level).put(child, null);
                            lstGlobalLevelTerms.get(level).put(child, null); // need to update this for calling func to process
                        }
                    }
                }
            }
        }
    }
    // builds a GOterm -> list of ancestors with distance from term and GOslim|PartOf flags
    private void getTermAncestors(String term, HashMap<String, TermInfo> hmTermAncestors, String slim, int distance, 
            boolean isPartOf, boolean originalTerm) {
        if(goTerms.isEmpty())
            loadGOTerms(Paths.get(app.data.getAppGOTermsFilepath()));
        if(goTerms.containsKey(term)) {
            GOTerm gt = goTerms.get(term);
            int flags = isPartOf? FLG_PARTOF : 0;
            if(slim != null && !slim.isEmpty()) {
                if(gt.lstSubset.contains(slim))
                    flags |= FLG_GOSLIM;
            }
            if(gt.lstIsA.isEmpty() && !gt.obsolete)
                flags |= FLG_ROOT;
            if(!originalTerm)
                hmTermAncestors.put(term, new TermInfo(gt.name, distance, flags, gt.catidx));
            for(String isa : gt.lstIsA)
                getTermAncestors(isa, hmTermAncestors, slim, distance+1, false, false);
            for(String pof : gt.lstPartOf)
                getTermAncestors(pof, hmTermAncestors, slim, distance+1, true, false);
        }
    }

    // builds a GOterm -> list of ancestors with distance from term and GOslim|PartOf flags
    // term -> immediate ancestors
    private boolean isCatIncluded(GOCat cat, int catidx) {
        boolean result = false;
        if(cat != null) {
            if(cat.equals(GOCat.BP) && catidx == idxBP)
                result = true;
            else if(cat.equals(GOCat.MF) && catidx == idxMF)
                result = true;
            else if(cat.equals(GOCat.CC) && catidx == idxCC)
                result = true;
        }
        return result;
    }
    // pass ArrayList<HashMap<String, Object>> - 
    // add HM to list at each level for each category
    // once all terms done (at calling func), reverse all the lists
    // consolidate all the diff cat terms into a new list for each level of the reversed lists
    private void getTermGraph(String term, HashMap<String, HashMap<String, TermInfo>> hmGraph, GOCat cat, String slim, 
            ArrayList<Integer> lstDistances, ArrayList<ArrayList<HashMap<String, Object>>> lstCatLevelTerms, boolean isPartOf) {
        if(goTerms.containsKey(term)) {
            // get term info and check if category included
            GOTerm gt = goTerms.get(term);
            if(!isCatIncluded(cat, gt.catidx))
                return;
            int distance = lstDistances.get(gt.catidx);
            ArrayList<HashMap<String, Object>> lstLevelTerms = lstCatLevelTerms.get(gt.catidx);
            // must be incremental adding so only add one
            if(lstLevelTerms.size() < (distance +1))
                lstLevelTerms.add(new HashMap<>());
            HashMap<String, Object> hmLevel = lstLevelTerms.get(distance);
            hmLevel.put(term, null);
                
            // set flags
            int flags = isPartOf? FLG_PARTOF : 0;
            if(slim != null && !slim.isEmpty()) {
                if(gt.lstSubset.contains(slim))
                    flags |= FLG_GOSLIM;
            }
            if(gt.lstIsA.isEmpty() && !gt.obsolete)
                flags |= FLG_ROOT;
            
            // update distance to max value, could have been set at a lower level - maybe remove later!!!
            int maxdist = distance;
            HashMap<String, TermInfo> hmTerm;
            if(!hmGraph.containsKey(term)) {
                hmTerm = new HashMap<>();
                hmGraph.put(term, hmTerm);
            }
            else {
                hmTerm = hmGraph.get(term);
                for(TermInfo ti : hmTerm.values()) {
                    if(ti.distance > maxdist)
                        maxdist = ti.distance;
                    break;
                }
            }
            // Note: term info is for the 'term' and it's added to every child
            TermInfo ti = new TermInfo(maxdist, flags, gt.catidx);

            // save all direct ancestors
            for(String isa : gt.lstIsA)
                hmTerm.put(isa, ti);
            for(String pof : gt.lstPartOf)
                hmTerm.put(pof, ti);

            // traverse ancestors
            int idx = 0;
            ArrayList<Integer> lstNewDistances = new ArrayList<>();
            ArrayList<Integer> lstMaxDistances = new ArrayList<>();
            for(Integer dist : lstDistances)
                lstMaxDistances.add(dist);
            for(String isa : gt.lstIsA) {
                lstNewDistances.clear();
                for(Integer dist : lstDistances)
                    lstNewDistances.add(dist);
                lstNewDistances.set(gt.catidx, lstNewDistances.get(gt.catidx) + 1);
                getTermGraph(isa, hmGraph, cat, slim, lstNewDistances, lstCatLevelTerms, false);
                idx = 0;
                for(Integer dist : lstNewDistances) {
                    if(dist > lstMaxDistances.get(idx))
                        lstMaxDistances.set(idx, dist);
                    idx++;
                }
            }
            for(String pof : gt.lstPartOf) {
                lstNewDistances.clear();
                for(Integer dist : lstDistances)
                    lstNewDistances.add(dist);
                lstNewDistances.set(gt.catidx, lstNewDistances.get(gt.catidx) + 1);
                getTermGraph(pof, hmGraph, cat, slim, lstNewDistances, lstCatLevelTerms, true);
                idx = 0;
                for(Integer dist : lstNewDistances) {
                    if(dist > lstMaxDistances.get(idx))
                        lstMaxDistances.set(idx, dist);
                    idx++;
                }
            }
            idx = 0;
            for(Integer dist : lstMaxDistances) {
                if(dist > lstDistances.get(idx))
                    lstDistances.set(idx, dist);
                idx++;
            }
        }
    }

    // builds a GOslims -> terms having that GOslim
    private void getTermSlims(String term, String ancestor, HashMap<String, HashMap<String, Object>> hmSlims, String slim) {
        if(goTerms.containsKey(ancestor)) {
            GOTerm gt = goTerms.get(ancestor);
            if(gt.lstSubset.contains(slim))
            {
                if(!hmSlims.containsKey(ancestor))
                    hmSlims.put(ancestor, new HashMap<>());
                hmSlims.get(ancestor).put(term, null);
            }
            for(String isa : gt.lstIsA)
                getTermSlims(term, isa, hmSlims, slim);
        }
    }
    // builds a Part -> terms being a part of it
    private void getTermPartsOf(String term, String ancestor, HashMap<String, HashMap<String, Object>> hmPartsOf) {
        if(goTerms.containsKey(ancestor)) {
            GOTerm gt = goTerms.get(ancestor);
            if(!gt.lstPartOf.isEmpty())
            {
                for(String part : gt.lstPartOf) {
                    if(!hmPartsOf.containsKey(ancestor))
                        hmPartsOf.put(ancestor, new HashMap<>());
                    // unlike with isA, we do not traverse the 'part of' term
                    // http://geneontology.org/page/ontology-relations#isa
                    hmPartsOf.get(ancestor).put(part, null);
                }
            }
            for(String isa : gt.lstIsA)
                getTermPartsOf(term, isa, hmPartsOf);
        }
    }
    
    private void loadGOTerms(Path filepath) {
        try {
            
            idxBP = -1;
            idxMF = -1;
            idxCC = -1;
            goSubsets.clear();
            goNamespaces.clear();
            goTerms.clear();
            goCategoryTerms.clear();
            if(Files.exists(filepath)) {
                List<String> lines = Files.readAllLines(filepath, StandardCharsets.UTF_8);
                int idx = 0;
                int cnt = lines.size();
                boolean terms = false;
                for(String line : lines) {
                    if(line.trim().equals(GOTerm.GOTERMDEF)) {
                        terms = true;
                        GOTerm gt = GOTerm.getGOTerm(lines, cnt, idx);
                        if(gt != null)
                            goTerms.put(gt.id, gt);
                    }
                    if(!terms) {
                        GOSubset gs = GOSubset.getGOSubset(line);
                        if(gs != null)
                            goSubsets.put(gs.id, gs.name);
                    }
                    idx++;
                }

                // we have the GO terms loaded, now build specificity levels for each root
                // start out by getting all the root terms
                //ArrayList<String> lstRoot = new ArrayList<>();
                for(String term : goTerms.keySet()) {
                    GOTerm gt = goTerms.get(term);
                    
                    //Get ancestors by each term
                    //ArrayList<String> all = new ArrayList<>();
                    //all.addAll(gt.lstIsA);
                    //all.addAll(gt.lstPartOf);
                    //Key GO:XXXX - List: A lot of GO:YYYYY
                    //ances.put(gt.id,all);
                    
                    if(!gt.namespace.isEmpty()) {
                        if(!goNamespaces.containsKey(gt.namespace)) {
                            goNamespaces.put(gt.namespace, new NamespaceInfo(goNamespaces.size()));
                            goCategoryTerms.add(new HashMap<>());
                        }
                        int catidx = goNamespaces.get(gt.namespace).idx;
                        gt.catidx = catidx;
                        goCategoryTerms.get(catidx).put(term, gt);
                        if(gt.lstIsA.isEmpty() && !gt.obsolete) {
                            //lstRoot.add(term);
                            HashMap<String, Object> hm = new HashMap<>();
                            hm.put(term, null);
                            goNamespaces.get(gt.namespace).term = term;
                        }
                    }
                }
                //SAVE HASHMAP ANCES INVALID CHARACTERS!!! SAVE IN PROJECT!!!!
                String ascen_path = app.data.getAppGOTermsAncesFilepath();
                
                //ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(ascen_path));
                // we have to calculate ancestors for each go.obo update - detect when you run for first time the new version of the app
                if(!Files.exists(Paths.get(ascen_path)) || !app.checkAppLastVersion()) {
                    PrintWriter writer = new PrintWriter(ascen_path, "UTF-8");
                    for(String term : goTerms.keySet()) {
                        GOTerm gt = goTerms.get(term);
                        //Get ancestors by each term
                        ArrayList<String> all = new ArrayList<>();
                        all.addAll(gt.lstIsA);
                        all.addAll(gt.lstPartOf);
                        writer.println(term + "\t" + all.toString());
                        
                    }
                    writer.close();
                }
                System.out.println("Finish");
                //System.out.println("GO Namespaces/Categories: ");
                //for(String namespace : goNamespaces.keySet())
                //    System.out.println("    " + namespace + "(" + goNamespaces.get(namespace).idx + ") " + goNamespaces.get(namespace).term);
                    
                //int catidx = 0;
                //for(String cat : goNamespaces.keySet())
                //    System.out.println("Total GO terms for " + cat + ": " + goCategoryTerms.get(catidx++).size());
                //System.out.println("Total GO terms: " + goTerms.size());
            }
        }
        catch (Exception e) {
            app.logError("Unable to load GO terms definition file '" + filepath.toString() + "': " + e.getMessage());
            goSubsets.clear();
            goNamespaces.clear();
            goTerms.clear();
            goCategoryTerms.clear();
        }
        for(String namespace : goNamespaces.keySet()) {
            NamespaceInfo ni = goNamespaces.get(namespace);
            if(ni.term.equals(BP))
                idxBP = ni.idx;
            else if(ni.term.equals(MF))
                idxMF = ni.idx;
            else if(ni.term.equals(CC))
                idxCC = ni.idx;
        }
    }

    //
    // Static Functions
    //
    static public String getShortName(String name, int maxlen) {
        String label = "";
        if(name.length() > maxlen) {
            String subName = abbrevNameWords(name);
            ArrayList<String> lines = new ArrayList<>();
            String fields[] = subName.split(" ");
            for(String word : fields) {
                if((label.length() + word.length()) < maxlen)
                    label += label.isEmpty()? word : " " + word;
                else {
                    // this assumes maxlen is in the 20s, change if needed
                    // it is done to minimize ending up with dup names being displayed
                    if(label.isEmpty() || label.length() < 20)
                        label += label.isEmpty()? word : " " + word;
                    label += "...";
                    break;
                }
            }
        }
        else
            label = name;
        return label;
    }
    static public String abbrevNameWords(String name) {
        String subName = name.replaceAll("Negative regulation", "-reg");
        subName = subName.replaceAll("negative regulation", "-reg");
        subName = subName.replaceAll("Positive regulation", "+reg");
        subName = subName.replaceAll("positive regulation", "+reg");
        subName = subName.replaceAll("Negative", "Neg.");
        subName = subName.replaceAll("negative", "neg.");
        subName = subName.replaceAll("Positive", "Pos.");
        subName = subName.replaceAll("positive", "pos.");
        subName = subName.replaceAll("Regulation", "Reg.");
        subName = subName.replaceAll("regulation", "reg.");
        return subName;
    }
    static public String wrapLine(String line, int maxlen) {
        String newLine = "";
        if(line.length() > maxlen) {
            String nline = "";
            String fields[] = line.split(" ");
            for(String word : fields) {
                if((nline.length() + word.length()) < maxlen)
                    nline += nline.isEmpty()? word : " " + word;
                else {
                    if(!nline.isEmpty()) {
                        newLine += newLine.isEmpty()? nline : "\\n" + nline;
                        nline = word;
                    }
                    else
                        newLine += newLine.isEmpty()? word : "\\n" + word;
                }
            }
            if(!nline.isEmpty())
                newLine += newLine.isEmpty()? nline : "\\n" + nline;
        }
        else
            newLine = line;
        
        // check for bracketed references at the end and remove them
        if(!newLine.isEmpty()) {
            if(newLine.charAt(newLine.length() - 1) == ']') {
                int pos = newLine.lastIndexOf("[");
                if(pos != -1)
                    newLine = newLine.substring(0, pos);
            }
        }
        return newLine;
    }

    //
    // Data Classes
    //
    public static class GOSubset {
        private final static String SUBSETDEF = "subsetdef:";
        
        public String id;
        public String name;
        GOSubset(String id, String name) {
            this.id = id;
            this.name = name;
        }
        public static GOSubset getGOSubset(String def) {
            GOSubset gs = null;
            if(isGOSubsetDef(def)) {
                String[] fields = def.split(" ");
                int pos = def.indexOf(" \"");
                gs = new GOSubset(fields[1].trim(), def.substring(pos).replaceAll("[\"]", "").trim());
            }
            return gs;
        }
        public static boolean isGOSubsetDef(String def) {
            boolean result = false;
            String[] fields = def.split(" ");
            int pos = def.indexOf(" \"");
            if(fields.length >= 3 && fields[0].trim().equals(SUBSETDEF) && pos != -1)
                result = true;
            return result;
        }
    }
    public static class GOTerm {
        public final static String GOTERMDEF = "[Term]";

        public String id;
        public String name;
        public String namespace;
        public String def;
        public boolean obsolete;
        public int catidx;
        public HashMap<String, Object> hmIsA;
        public HashMap<String, Object> hmPartOf;
        public ArrayList<String> lstIsA;
        public ArrayList<String> lstPartOf;
        public ArrayList<String> lstSubset;
        GOTerm(String id) {
            this.id = id;
            this.obsolete = false;
            this.catidx = -1;
            this.hmIsA = new HashMap<>();
            this.hmPartOf = new HashMap<>();
            this.lstIsA = new ArrayList<>();
            this.lstPartOf = new ArrayList<>();
            this.lstSubset = new ArrayList<>();
        }
        public static GOTerm getGOTerm(List<String> lines, int cnt, int idx) {
            GOTerm gt = null;
            if(lines.get(idx).trim().equals(GOTERMDEF)) {
                for(++idx; idx < cnt; idx++) {
                    String line = lines.get(idx).trim();
                    if(line.isEmpty() || line.equals(GOTERMDEF))
                        break;
                    KeyValuePair kvp = KeyValuePair.getKeyValuePair(line);
                    if(kvp != null) {
                        if(gt == null) {
                            if(kvp.key.equals("id"))
                                gt = new GOTerm(kvp.value);
                            else {
                                System.out.print("ERROR: NO ID for Go [Term]");
                                break;
                            }
                        }
                        else {
                            // other possible keys to capture: alt_id, consider, intersection_of, relationship
                            switch(kvp.key) {
                                case "name":
                                    gt.name = kvp.value;
                                    break;
                                case "namespace":
                                    gt.namespace = kvp.value;
                                    break;
                                case "def":
                                    gt.def = kvp.value;
                                    break;
                                case "is_obsolete":
                                    gt.obsolete = Boolean.valueOf(kvp.value.toLowerCase());
                                    break;
                                case "is_a":
                                    int pos = kvp.value.indexOf(" ");
                                    if(pos != -1) {
                                        gt.lstIsA.add(kvp.value.substring(0, pos));
                                        gt.hmIsA.put(kvp.value.substring(0, pos), null);
                                    }
                                    else {
                                        gt.lstIsA.add(kvp.value);
                                        gt.hmIsA.put(kvp.value, null);
                                    }
                                    break;
                                case "subset":
                                    gt.lstSubset.add(kvp.value);
                                    break;
                                case "relationship":
                                    // get 'part of' relationships
                                    if(PartOf.isPartOf(kvp.value)) {
                                        gt.lstPartOf.add(PartOf.getPartOf(kvp.value));
                                        gt.hmPartOf.put(PartOf.getPartOf(kvp.value), null);
                                    }
                                    break;
                            }
                        }                        
                    }
                }
            }
            return gt;
        }
        public boolean isRoot() {
            return(lstIsA.isEmpty() && !obsolete);
        }
    }
    public static class KeyValuePair  {
        String key, value;
        public KeyValuePair(String key, String value) {
            this.key = key;
            this.value = value;
        }
        private static KeyValuePair getKeyValuePair(String line) {
            KeyValuePair kvp = null;
            int pos = line.indexOf(": ");
            if(pos != -1)
                kvp = new KeyValuePair(line.substring(0, pos).trim(), line.substring(pos+2).trim());
            return kvp;
        }
    }
    public static class PartOf  {
        static String PART_OF = "part_of ";
        static String PART_OF_SEPARATOR = " ! ";
        String id;
        public static String getPartOf(String relationship) {
            String po = "";
            if(isPartOf(relationship)) {
                String[] fields = relationship.substring(PART_OF.length()).split(PART_OF_SEPARATOR);
                po = fields[0].trim();
            }
            return po;
        }
        private static boolean isPartOf(String relationship) {
            boolean result = false;
            if(relationship.startsWith(PART_OF)) {
                String[] fields = relationship.substring(PART_OF.length()).split(PART_OF_SEPARATOR);
                if(fields.length == 2)
                    result = true;
            }
            return result;
        }
    }
    public static class TermInfo  {
        int distance, flags, catidx;
        String name;
        public TermInfo(int distance, int flags, int catidx) {
            this.name = "";
            this.distance = distance;
            this.flags = flags;
            this.catidx = catidx;
        }
        public TermInfo(String name, int distance, int flags, int catidx) {
            this.name = name;
            this.distance = distance;
            this.flags = flags;
            this.catidx = catidx;
        }
        public boolean isRoot() {
            return ((flags & FLG_ROOT) != 0);
        }
        public boolean isGOslim() {
            return ((flags & FLG_GOSLIM) != 0);
        }
        public boolean isPartOf() {
            return ((flags & FLG_PARTOF) != 0);
        }
    }
    public static class TermGroupInfo  {
        int maxDistance;
        ArrayList<String> levels;
        public TermGroupInfo(int maxDistance) {
            this.maxDistance = maxDistance;
            this.levels = new ArrayList<>();
        }
    }
    public static class NamespaceInfo {
        int idx;
        String term;
        public NamespaceInfo(int idx) {
            this.idx = idx;
            this.term = "";
        }
    }
    public static class GraphResult {
        boolean bp, mf, cc;
        int maxLevel;
        String html;
        public GraphResult(int maxLevels, String html, boolean bp, boolean mf, boolean cc) {
            this.maxLevel = maxLevels;
            this.html = html;
            this.bp = bp;
            this.mf = mf;
            this.cc = cc;
        }
    }
}
