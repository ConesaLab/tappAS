/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import tappas.DataAnnotation.AnnotFeature;
import javafx.scene.text.Text;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import static tappas.DataAnnotation.POLYA_FEATURE;
import static tappas.DataAnnotation.STRUCTURAL_SOURCE;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class ViewProtein extends ViewAnnotation {
    public ViewProtein(Project project) {
        super(project);
    }

    // Initialization
    @Override
    public boolean initialize(SubTabBase subTabBase, Canvas canvasViewer, ScrollPane canvasScrollPane, String filterCaption) {
        return super.initialize(subTabBase, canvasViewer, canvasScrollPane, filterCaption);
    }
    public void refreshViewer() {
    }
    // Get GO terms for given transcript or all transcripts if rsid is empty
    protected ArrayList<String> getGOTerms(String rsid, ArrayList<String[]> lines) {
        ArrayList<String> lstTerms = new ArrayList<>();
        HashMap<String, Object> hmTerms = new HashMap<>();
        try {
            boolean process = false;
            HashMap<String, String> attrs;
            AnnotationFileDefs defs = project.data.getAnnotationFileDefs();
            for(String[] fields : lines) {
                if(rsid.isEmpty() || fields[0].equals(rsid)) {
                    String source = fields[1];
                    String type = fields[2];
                    if(project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.PROTEIN))
                        process = true;
                    else if(project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.TRANSCRIPT) || project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.GENOMIC))
                        process = false;
                    if(process) {
                        if(source.equals(defs.srcGO)) {
                            attrs = DataAnnotation.getAttributes(fields[8]);
                            if(attrs.containsKey(project.data.getAnnotationFileDefs().attrId))
                                hmTerms.put(attrs.get(project.data.getAnnotationFileDefs().attrId), null);
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.logError("Unable to process annotation data for protein " + rsid + ": " + ex.getMessage());
            hmTerms.clear();
        }
        for(String term : hmTerms.keySet())
            lstTerms.add(term);
        return lstTerms;
    }
    @Override
    public void handleMouseClickAction(MouseEvent event) {
        if(event.getClickCount() == 2) {
            HashMap<String, Object> hm = new HashMap<>();
            if(app.ctlr.fxdc.isShiftKeyDown())
                hm.put("zoomOut", TabGeneDataViz.Views.PROTEIN);
            else
                hm.put("zoomIn", TabGeneDataViz.Views.PROTEIN);
            subTabBase.processRequest(hm);
        }
        if(tipEntry != null && event.getButton() == MouseButton.PRIMARY) {
            String url = "";
            AnnotFeature dbcatid = project.data.getRsvdAnnotFeature(tipEntry.source, tipEntry.feature);
            String species = project.data.getGenus() + "_" + project.data.getSpecies();
            DataApp.RefType rt = project.data.getRefType();
            if(species.equals("Homo_sapiens") || species.equals("Mus_musculus")) {
                switch(dbcatid) {
                    case GENE:
                        if(rt.equals(DataApp.RefType.Ensembl))
                            url = "http://www.ensembl.org/" + species + "/Gene/Summary?g=" + tipEntry.id;
                        else { //if(rt.equals(DataApp.RefType.Demo)) { // chg to MMDemo to allow other demos?!
                            String id = app.data.getNCBIGeneId(species, tipEntry.id);
                            if(!id.isEmpty())
                                url = "https://www.ncbi.nlm.nih.gov/gene/" + id;
                        }
                        break;
                    case PROTEIN:
                        // currently the id contains the id of the transcript
                        // this may change in the near future!!!
                        if(rt.equals(DataApp.RefType.Ensembl) && tipEntry.id.startsWith("ENSP"))
                            url = "http://www.ensembl.org/" + species + "/Transcript/ProteinSummary?t=" + tipEntry.id;
                        else {
                            // our demo uses the PacBio transcript as the Id
                            String id = tipEntry.id;
                            if(rt.equals(DataApp.RefType.Demo))
                                id = tipEntry.featureId;
                            
                            if(id.isEmpty())
                                app.ctls.alertInformation("Protein Online Reference", "There are no online references for this protein.");
                            else {
                                // we don't have the NCBI internal id to use ".../protein/id" so use nuccore with the XP_ id
                                if(id.startsWith("XP_"))
                                    url = "https://www.ncbi.nlm.nih.gov/nuccore/" + id;
                                if(rt.equals(DataApp.RefType.Demo) && id.toLowerCase().startsWith("novel"))
                                    app.ctls.alertInformation("Protein Online Reference", "There are no online references for this protein.");
                                else {
                                    // give UniProt a try, uses A-Z as first character and number as second
                                    // for full details see http://www.uniprot.org/help/accession_numbers
                                    if(id.length() > 2 && id.substring(0, 1).matches("[A-Z]") && id.substring(1, 2).matches("[0-9]"))
                                        url = "http://www.uniprot.org/uniprot/" + id;
                                    else
                                        app.ctls.alertInformation("Protein Online Reference", "Unable to determine what online references to use for this protein.");
                                }
                            }
                        }
                        break;
                }
            }
            if(!url.isEmpty())
                Tappas.getHost().showDocument(url);

        }
    }
    @Override
    protected void draw(GraphicsContext gc, DrawArgs args) {
        ArrayList<AnnotationEntry> cdsList = new ArrayList<>();
        ArrayList<SeqAlign.Position> tgexonsList = new ArrayList<>();
        ArrayList<ViewAnnotation.SpliceJuncData> genoSpliceJuncList = new ArrayList<>();
        ArrayList<SeqAlign.Position> spliceJuncList = new ArrayList<>();
        //ArrayList<SeqAlign.Position> tgcdsList = new ArrayList<>();
        AnnotationEntry seqEntry = null;
        
        //check NMD trasncripts
        HashMap<String, Object> hmNMD = project.data.getTransWithAnnot("NMD");
        
        //System.out.println("draw at " + args.prefs.scale + ", " + args.redrawCount + ", align: " + args.prefs.alignment);
        gc.setFill(Color.valueOf("white"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        double opacity = limitExceeded? 0.10 : 1.0;
        gc.setFill(Color.web(limitExceeded? "darkorange" : ((args.prefs.hmNotFeatureIDs.isEmpty() && !args.prefs.showVaryingOnly)? "white" : "white"), opacity));
        if(annotationList.isEmpty() || geneEntry == null) {
            double cwo = canvas.getWidth();
            double cyo = canvas.getHeight();
            canvas.setWidth(canvas.getWidth() * scale);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.setGlobalAlpha(0.75);
            gc.setFill(Color.LIGHTGRAY);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("No Protein Information Available", cwo/2, cyo/scale/2);
            gc.setGlobalAlpha(1.0);
            return;
        }
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // note that the first time this function is called, redrawCount = 1,
        // the canvas width has been set to ((canvasPane.getWidth() - 2) / scale)
        // this allows the proper expansion of the width when scaled
        // otherwise you always end up having (width * scale) and horizontal scroll bar
        double ch = canvas.getHeight();
        double chs = ch / scale;
        double cw = canvas.getWidth();
        double cws = cw / scale;
        if(args.redrawCount > 1) {
            ch = chs;
            cw = cws;
        }
            
        // initially allow for one level of symbols on top and one on bottom
        // if there is no room, add levels on bottom
        // this keeps the symbols from getting stacked up on top of each other
        ArrayList<String> symbolsList = new ArrayList<>();
        ArrayList<ArrayList<UsedPoints>> seqList = new ArrayList<>();
        seqList.add(new ArrayList<>());   // top, 1st level
        seqList.add(new ArrayList<>());   // bottom, 1st level

        // display title
        tipsList.clear();
        double ytitle = 20;
        double ysubtitle = 40;
        gc.setFont(titleFont);
        gc.setFill(titleColor);
        gc.setTextAlign(TextAlignment.CENTER);
        // set gene display title - name plus description if available
        String title = geneEntry.id;
        if(!geneEntry.attrs.isEmpty()) {
            HashMap<String, String> attrs = DataAnnotation.getAttributes(geneEntry.attrs);
            if(attrs.containsKey(project.data.getAnnotationFileDefs().attrDesc)) {
                String desc = attrs.get(project.data.getAnnotationFileDefs().attrDesc);
                if(!desc.isEmpty())
                    title += " - " + desc.substring(0,1).toUpperCase() + desc.substring(1);
                if(title.length() > maxTitleLength)
                    title = title.substring(0, maxTitleLength) + "...";
            }
        }
        gc.fillText(title, (cw/2), ytitle);
        Text text = new Text(title);
        text.setFont(gc.getFont());
        double width = text.getLayoutBounds().getWidth();
        tipsList.add(new ToolTipPoints(geneEntry, (cw/2 - width/2), ytitle - 10, width, 16,
                            String.format("%s\n", geneEntry.tooltip), true));

        // display subtitle
        gc.setFont(subFont);
        gc.setFill(subColor);
        gc.fillText("Proteins View - " + (args.prefs.alignment? "Aligned" : "Unaligned"), (cw/2), ysubtitle);
        //gc.fillText("Proteins View - " + (args.prefs.alignment? "Aligned" : "Unaligned") + (filterCaption.isEmpty()? "" : (" for " + filterCaption)), (cw/2), ysubtitle);

        double yoffset = ysubtitle + 20;
        if(args.prefs.ruler)
            yoffset += 30;
        double xoffset = 12;
        double rightPadding = 60;
        double leftPadding = 60;
        double htop = 20;
        double hbottom = 17;
        double seqHeight = 40;
        double yRegion = yoffset - 20;
        double ymain = yoffset + htop;
        double wRegion = (cw - 2 * xoffset - rightPadding - leftPadding);
        double bases = geneEntry.endpos - geneEntry.strpos + 1;
        if(bases <= 0)
            bases = 1;
        int proteinLen = 0;
        SeqAlign.Position cdspos = new SeqAlign.Position(0,0);
        SeqAlign.Position utr5pos = new SeqAlign.Position(0,0);
        SeqAlign.Position utr3pos = new SeqAlign.Position(0,0);
        double dspsizeX = 0;
        double dspsizeY = 0;
        double ppb = wRegion / bases;
        zoomPPB = ppb;
        //System.out.println("Bases: " + bases + ", ppb: " + ppb);
        double rw = AnnotationSymbols.CDSblockHeight;
        double rws = adjustValue(rw);
        String fmtpos, str1;
        double x, y;
        boolean dspExons = false;
        boolean negStrand = false;
        double wExpLevel = leftPadding - 10;
        double expLevelPpb = (maxExpLevel == 0)? 0 : wExpLevel / maxExpLevel;

        //System.out.println("wRgn: " + wRegion + ", bases: " + bases + ", ppb: " + ppb);
        if(args.prefs.ruler)
            drawRuler(gc, xoffset + leftPadding, yoffset - 20, wRegion, args.hRegion, ppb, geneEntry.strpos, geneEntry.endpos, 0 , true);

        // process all gene annotations
        AnnotationFileDefs defs = project.data.getAnnotationFileDefs();
        int proteinCount = 0;
        boolean endloop = false;
        boolean skipEntry = false;
        HashMap<String, ProteinEntry> hmProts = new HashMap<>();
        int lstSize = annotationList.size();
        int idxEntry = 0;
        for(AnnotationEntry entry : annotationList) {
            idxEntry++;
            
            //don't draw NMD transcripts
            if(hmNMD.containsKey(entry.id))
                continue;
            
            //dont draw polyASite in proteins view or Structural Information
            if(entry.feature.equals(POLYA_FEATURE) || entry.source.equals(STRUCTURAL_SOURCE))
                continue;
            
            // get entry source/feature and check if we are skipping this protein
            AnnotFeature dbcatid = project.data.getRsvdAnnotFeature(entry.source, entry.feature);
            if(seqEntry == null && dbcatid != DataAnnotation.AnnotFeature.PROTEIN) {
                logger.logError("Gene protein annotations must have 'protein' as first entry. Entry: " + entry.feature);
                return;
            }
            if(skipEntry && !dbcatid.equals(AnnotFeature.PROTEIN))
                continue;
            
            gc.setStroke(defaultColor);
            gc.setFill(defaultColor);
            gc.setFont(defaultFont);
            switch(dbcatid) { //entry.type) { //.toLowerCase()) {
                case PROTEIN:
                    skipEntry = false;
                    HashMap<String, String> attrs = DataAnnotation.getAttributes(entry.attrs);
                    String pid = "";
                    String alias = "";
                    if(attrs.containsKey(project.data.getAnnotationFileDefs().attrId))
                        pid = attrs.get(project.data.getAnnotationFileDefs().attrId);
                    if(attrs.containsKey("Alias"))
                        alias = attrs.get("Alias");
                    if(pid.isEmpty()) {
                        if(args.redrawCount == 1) {
                            String msg = "\nShowing transcript ID in its place.";
                            if(!alias.isEmpty())
                                msg = "\nShowing alias in its place.";
                            Utils.showAlertLater(Alert.AlertType.WARNING, "Gene Protein Missing Required ID", msg);
                        }
                        if(!alias.isEmpty())
                            pid = alias;
                        else
                            pid = entry.id;
                    }
                    if(proteinCount >= TabGeneDataViz.MAX_GENE_TRANS && !limitExceeded) {
                        if(args.redrawCount == 1)
                            Utils.showAlertLater(Alert.AlertType.WARNING, "Gene Proteins Limit Exceeded", "The selected gene contains too many proteins,\nshowing only the first " + TabGeneDataViz.MAX_GENE_TRANS + ".");
                        limitExceeded = true;
                        endloop = true;
                        break;
                    }
                    
                    // check if we already have this protein and set to skip if so
                    // multiple transcripts can have the same protein but we only show it once
                    // however, we show all the transcripts that code the protein
                    if(hmProts.containsKey(pid)) {
                        // add this transcript to its list
                        //System.out.println("Exisiting protein " + pid + ", adding trans " + entry.id);
                        hmProts.get(pid).addTrans(entry.id);
                        skipEntry = true;
                    }
                    else {
                        proteinLen = entry.endpos - entry.strpos + 1;
                        if(seqEntry == null) {
                            gc.setFont(smallFont);
                            x = xoffset + leftPadding - 5;
                            y = yoffset - 10;
                            gc.setFill(Color.PLUM);
                            gc.setTextAlign(TextAlignment.RIGHT);
                            gc.fillText("N-terminal", x, y);
                            x = cw - xoffset - rightPadding + 5;
                            gc.setTextAlign(TextAlignment.LEFT);
                            gc.fillText("C-terminal", x, y);
                            gc.setFont(defaultFont);
                        }
                        else {
                            tgexonsList.clear();
                            //tgcdsList.clear();
                            dspExons = false;
                            cdspos.start = 0; cdspos.end = 0;
                            utr5pos.start = 0; utr5pos.end = 0;
                            utr3pos.start = 0; utr3pos.end = 0;

                            // clear all used points from last sequence
                            int cnt = seqList.size();
                            for(int i = seqList.size() - 1; i >= 0; i--)
                            {
                                if(i < 2)
                                    seqList.get(i).clear();
                                else
                                    seqList.remove(i);
                            }
                            // adjust starting 'y' position, taking into account additional levels (if any)
                            ymain += adjustY(seqHeight + (cnt - 2) * levelOffset);
                        }
                        seqEntry = entry;
                        negStrand = entry.strand.equals("-");

                        // we need to populate cds (domain) list now or the top symbol placements will not
                        // be able to take it into account and the domain will be placed on top of them
                        cdsList.clear();
                        int idx = idxEntry;
                        while(idx < lstSize) {
                            AnnotationEntry e = annotationList.get(idx);
                            AnnotFeature afid = project.data.getRsvdAnnotFeature(e.source, e.feature);
                            if(!afid.equals(AnnotFeature.PROTEIN)) {
                                if(defs.isDomain(e.source, e.feature)) {
                                    if(isDisplayableFeatureId(e, args.prefs))
                                        cdsList.add(e);
                                }
                            }
                            else
                                break;
                            idx++;
                        }
                        
                        String provean = "";
                        if(args.prefs.provean) {
                            // even though it is for a protein, the provean score is specified at the transcript level
                            String score = getTransProveanScore(entry.id);
                            if(score.isEmpty())
                                score = "N/A";
                            provean = " Provean Score: " + score;
                        }
                        hmProts.put(pid, new ProteinEntry(pid, alias, entry.id, provean, xoffset, ymain - 4));
                    }
                    
                    // draw protein title section (must update even if same protein)
                    gc.setTextAlign(TextAlignment.LEFT);
                    ProteinEntry pe = hmProts.get(pid);
                    String dspid = pe.id;
                    if(!pe.alias.isEmpty() && !pid.equals(pe.alias))
                        dspid = pid + " (" + pe.alias + ")";
                    String dspextra = "  [" + pe.trans + "]" + pe.provean;
                    String fulltxt = dspid + dspextra;
                    

                    Text fulltext = new Text(fulltxt);
                    fulltext.setFont(gc.getFont());
                    double wfulltxt = fulltext.getLayoutBounds().getWidth();

                    gc.setFill(Color.WHITE);
                    gc.fillRect(pe.x, pe.y - 9, wfulltxt, 12);
                    gc.setFill(Color.MAROON);
                    gc.fillText(dspid, pe.x, pe.y);
                    gc.setFill(Color.GRAY);
                    Text txttext = new Text(dspid);
                    txttext.setFont(gc.getFont());
                    double wtxt = txttext.getLayoutBounds().getWidth();

                    gc.fillText(dspextra, pe.x + wtxt, pe.y);
                    if(skipEntry)
                        continue;

                    // display expression levels if not time series and available
                    if(project.data.isCaseControlExpType() && geneIsoformsExpression.containsKey(pid)) {
                        Font f = gc.getFont();
                        DataProject.TransExpLevels el = geneIsoformsExpression.get(pid);
                        System.out.println("maxEL: " + maxExpLevel + ", wEL: " + wExpLevel + ", ppb: " + expLevelPpb + "x1: " + el.X1_mean + ", x2: " + el.X2_mean);
                        double xc = xoffset;
                        //double xc = xoffset + leftPadding + wRegion + 5;
                        double fy = adjustY(hbottom + ymain - (AnnotationSymbols.RNAblockHeight/2) - 2);
                        gc.setFill(Color.ORANGE);
                        double wexp1 = adjustX(el.X1_mean * expLevelPpb);
                        gc.fillRect(xc + 0.5, fy - 2, wexp1, 4);
                        double wexp2 = adjustX(el.X2_mean * expLevelPpb);
                        gc.setFill(Color.ORANGERED);
                        gc.fillRect(xc + 0.5, fy + 3, wexp2, 4);
                        gc.setFill(Color.BLACK);
                        gc.fillRect(xc, fy - 4, 0.5, 12);
                        gc.fillRect(xc, fy + 8.5, wExpLevel, 0.5);
                        gc.setTextAlign(TextAlignment.RIGHT);
                        gc.setFont(new Font(8)); // some font sizes will create a gap between digits, e.g. 7
                        gc.setFill(Color.GRAY);
                        gc.fillText(NumberFormat.getInstance().format(maxExpLevel), xc + wExpLevel, fy + 17);
                        String dea = "";
                        if(project.data.analysis.hasDEAData(DataApp.DataType.PROTEIN)) {
                            String der = "(not included in analysis)";
                            if(geneIsoformsDEResults.containsKey(pid)) {
                                der = geneIsoformsDEResults.get(pid)? "DE" : "Not DE";
                                gc.setTextAlign(TextAlignment.LEFT);
                                gc.setFont(new Font(8));
                                gc.setFill(Color.CHOCOLATE);
                                gc.fillText(der, xc + 1, fy-5);
                            }
                            dea = "DEA Result: " + der + "\n";
                        }
                        double el1 = Double.parseDouble(String.format("%.2f", ((double)Math.round(el.X1_mean*100)/100.0)));
                        double el2 = Double.parseDouble(String.format("%.2f", ((double)Math.round(el.X2_mean*100)/100.0)));
                        String[] names = project.data.getExpTypeGroupNames();
                        String ttip = String.format("Protein Expression Level (normalized mean)\n" + dea +
                                     names[0] + " ExpLevel: %s   (top line)\n" +
                                     names[1] + " ExpLevel: %s   (bottom line)", 
                                     NumberFormat.getInstance().format(el1), NumberFormat.getInstance().format(el2));
                        addToolTipToList(ttip, xc, fy - 10, wExpLevel, 15, false);
                        gc.setTextAlign(TextAlignment.LEFT);
                        gc.setFont(f);
                    }


                    Text dsptext = new Text(dspid);
                    dsptext.setFont(gc.getFont());
                    width = dsptext.getLayoutBounds().getWidth();

                    addToolTipToList(entry, xoffset, ymain - 12, width, 10, true);
                    ymain += hbottom;
                    ymain = adjustY(ymain);

                    x = xoffset + leftPadding + (entry.strpos - geneEntry.strpos) * ppb;
                    gc.setFill(defaultColor);
                    gc.setStroke(defaultColor);
                    x = adjustX(x);
                    int dspstr = entry.strpos;
                    int dspend = entry.endpos;
                    if(args.prefs.alignment) {
                        dspstr = geneEntry.strpos;
                        dspend = geneEntry.endpos;
                    }
                    double dw = dspend - dspstr + 1;
                    if(dw > 1) {
                        if(!args.prefs.alignment) {
                            y = adjustY(ymain - (AnnotationSymbols.RNAblockHeight/2)); 
                            symbols.drawRNAblock(gc, x, y, (dw * ppb));
                            gc.setFill(Color.DARKGREY);
                            x = xoffset + leftPadding + 5 + dw * ppb;
                            str1 = NumberFormat.getInstance().format(proteinLen);
                            gc.fillText(str1 + "aa", x, ymain + 4);
                        }
                        else {
                            dspsizeX = xoffset + leftPadding + 5 + dw * ppb;
                            dspsizeY = ymain + 4;
                        }
                    }
                    break;

                case SPLICEJUNCTION:
                    if (args.prefs.alignment && args.prefs.spliceJuncs) {
                        int start = negStrand ? (entry.strpos - 1) : (entry.strpos - 1);
                        int end = negStrand ? (entry.endpos + 1) : (entry.endpos + 1);
                        genoSpliceJuncList.add(new ViewAnnotation.SpliceJuncData(negStrand, new SeqAlign.Position(start, end), entry.attrs));
                    } 
                    break;
                case CDS:
                case GENE:
                case EXON:
                    break;
                default:
                    switch(entry.feature) {
                        case DataAnnotation.INTCAT_SPLICEJUNCTION:
                            if (args.prefs.alignment && args.prefs.spliceJuncs) {
                                spliceJuncList.add(new SeqAlign.Position(entry.strpos, entry.endpos));
                            }
                            break;

                        case DataAnnotation.INTCAT_NODATA:
                            gc.setFill(Color.DARKGREY);
                            x = xoffset + leftPadding;
                            gc.fillText("No alignment data available...", x, ymain + 4);
                            break;
                        case DataAnnotation.INTCAT_CDS:
                            // treating CDS like exons in transcripts
                            if(args.prefs.alignment) {
                                dspExons = true;
                                tgexonsList.add(new SeqAlign.Position(entry.strpos, entry.endpos));
                            }
                            break;
                        default:
                            // System.out.println("Default entry type: " + entry.type);
                            if(args.prefs.alignment) {
                                if(dspExons) {
                                    dspExons = false;
                                    gc.setFill(Color.DARKGREY);
                                    str1 = NumberFormat.getInstance().format(proteinLen);
                                    gc.fillText(str1 + "aa", dspsizeX, dspsizeY);
                                    displayExons(gc, xoffset, leftPadding, ymain, ppb, negStrand,
                                                tgexonsList, seqEntry);
                                }

                                // check if we just wanted to make sure exon data was flushed
                                if(entry.feature.equals(DataAnnotation.INTCAT_FLUSHDATA))
                                    break;

                                entry.dspstrpos = Math.floor((seqAlign.getAlignedPos(entry.strpos * 3, tgexonsList)+2)/3);
                                entry.dspendpos = Math.floor((seqAlign.getAlignedPos(entry.endpos * 3, tgexonsList)+2)/3);
                                fmtpos = String.format("%s%s", NumberFormat.getInstance().format(entry.dspstrpos),
                                                (entry.dspstrpos == entry.dspendpos)? "" : String.format(" - %s", NumberFormat.getInstance().format(entry.dspendpos)));
                                entry.tooltipAligned = String.format("\nAligned: %s", fmtpos);
                            }
                            else {
                                entry.dspstrpos = entry.strpos;
                                entry.dspendpos = entry.endpos;
                            }
                            entry.dsppos = entry.dspstrpos + (entry.dspendpos - entry.dspstrpos + 1)/2;
                            
                            // in protein display, the domains are treated like CDS since they must be handled the same way
                            if(defs.isDomain(entry.source, entry.feature)) {
                                addFeatureID(entry);
                                if(isDisplayableFeatureId(entry, args.prefs))
                                {
                                    // cds (domain) has already been added to cdsList, just display it accordingly
                                    if(args.prefs.alignment) {
                                        x = xoffset + leftPadding;
                                        y = adjustY(ymain - (rw/2));
                                        dspDomain(gc, x, y, rws, ppb, entry, tgexonsList);
                                    }
                                    else {
                                        rw = 15;
                                        rws = adjustValue(rw);
                                        x = adjustX(xoffset + leftPadding + (entry.strpos - geneEntry.strpos) * ppb);
                                        y = adjustY(ymain - (rw/2));
                                        gc.setGlobalAlpha(0.75);
                                        gc.setFill(entry.symbol.colors.fill);
                                        gc.setStroke(entry.symbol.colors.stroke);
                                        gc.fillRect(x, y, (entry.endpos - entry.strpos + 1) * ppb, rws);
                                        gc.strokeRect(x, y, (entry.endpos - entry.strpos + 1) * ppb, rws);
                                        gc.setGlobalAlpha(1.0);
                                        gc.setTextAlign(TextAlignment.LEFT);
                                        gc.setFill(Color.valueOf("#606060"));
                                        fmtpos = String.format("%s%s", NumberFormat.getInstance().format(entry.strpos),
                                                        (entry.strpos == entry.endpos)? "" : String.format(" - %s", NumberFormat.getInstance().format(entry.endpos)));
                                        tipsList.add(new ToolTipPoints(entry, x, y, (entry.endpos - entry.strpos + 1) * ppb, rws,
                                                            String.format("%s\nPosition: %s", entry.tooltip, fmtpos), false));
                                    }
                                }
                            }
                            else {
                                // we are getting provean and gene ontology annotations coming in here - they have no symbol associated with them
                                if(!project.data.isInternalFeature(entry.source, entry.feature) && entry.symbol != null) {
                                    if(seqEntry != null) {
                                        ArrayList<AnnotationEntry> algCDSList = cdsList;
                                        if(args.prefs.alignment) {
                                            algCDSList = new ArrayList<>();
                                            for(ViewAnnotation.AnnotationEntry e : cdsList) {
                                                // need to convert to aligned position for symbol display (determines vert distance from center)
                                                int sp = (int)seqAlign.getAlignedPos(e.strpos * 3, tgexonsList)/3;
                                                int ep = (int)seqAlign.getAlignedPos(e.endpos * 3, tgexonsList)/3;
                                                ViewAnnotation.AnnotationEntry ae = new ViewAnnotation.AnnotationEntry(e.id, e.source, e.feature, e.featureId, e.caption, e.strand, sp, ep, e.attrs, e.tooltip, e.urlValue);
                                                algCDSList.add(ae);
                                            }
                                        }
                                        addFeatureID(entry);
                                        if(isDisplayableFeatureId(entry, args.prefs))
                                            drawTrackSymbol(entry, geneEntry, seqEntry, tgexonsList, algCDSList, seqList, gc, xoffset + leftPadding, ymain, ppb, negStrand, false, args.prefs.alignment, SeqAlign.WithinRegion.YES);
                                    }
                                    if(symbolsList.indexOf(entry.feature) == -1) {
                                        symbolsList.add(entry.feature);
                                    }
                                }
                            }
                            break;
                    }
                    break;
            }
            if(endloop)
                break;
        }
            
        // draw legend
        int cnt = seqList.size();
        y = ymain + seqHeight + (cnt - 2) * levelOffset;
        args.hRegion = y - yRegion;
        Rectangle2D.Double rc = new Rectangle2D.Double(xoffset, y, (cw - 2 * xoffset), 0);
        if(args.redrawCount == 1)
            args.singleColumn = symbols.isSingleColumnLegend(gc, legendFont, rc);
        Point2D.Double pmax = symbols.drawLegend(gc, legendColor, legendFont, rc, TextAlignment.CENTER, true, args.singleColumn, false, false, false);
        y = pmax.y;

        // adjust size accordingly if needed
        y += 30/scale;
        double crh = 0;
        boolean redraw = false;
        double reqHeight = (y + crh) * scale;
        System.out.println("Required canvas height (" + y + "): " + ((y + crh) * scale) + ", current canvas height: " + ch);
        if(args.redrawCount == 1) {
            if(ch < ((y + crh) * scale)) {
                System.out.println("Setting canvas height to: " + ((y + crh) * scale));
                canvas.setHeight(reqHeight);
                redraw = true;
            }

            double sbHeight = 0.0;
            double sbWidth = 0.0;
            Set<Node> nodes = canvasPane.lookupAll(".scroll-bar");
            for (final Node node : nodes) {
                if (node instanceof ScrollBar) {
                    ScrollBar sb = (ScrollBar) node;
                    if (sb.getOrientation() == Orientation.HORIZONTAL) // && sb.isVisible())
                            sbHeight = sb.getHeight();
                    else if (sb.getOrientation() == Orientation.VERTICAL) // && sb.isVisible())
                            sbWidth = sb.getWidth();
                }
            }
            double xadd = 0;
            if(redraw)
                xadd -= sbWidth;
            // note: the sb width is reported wrong initially, 20, if window is resized then it goes to 15
            System.out.println("Setting canvas width to: " + ((cw * scale + xadd) * widthFactor) + ", sbw: " + sbWidth);
            canvas.setWidth((cw * scale + xadd) * widthFactor);
            if(scale != 1) {
                if(pmax.x > cw)
                    System.out.println("Legend exceeded canvas width, " + pmax.x);

                // check if we have a vertical bar and adjust if so
                if(widthFactor > 1)
                    canvas.setHeight(canvas.getHeight() - sbHeight);
                redraw = true;
            }
            else if(widthFactor > 1) {
                canvas.setHeight(canvas.getHeight() - sbHeight);
                redraw = true;
            }
            else if(args.prefs.ruler)
                redraw = true;

            if(redraw) {
                args.redrawCount++;
                draw(gc, args);
            }
        }
        else {
            double setHeight = canvas.getHeight();
            System.out.println("Canvas height after recalc: " + setHeight + ", redrawcnt: " + args.redrawCount);
            if(args.redrawCount < 3 && Math.abs(setHeight - reqHeight) > 10) {
                canvas.setHeight(reqHeight);
                args.redrawCount++;
                draw(gc, args);
            }
        }
    }
    protected String getTransProveanScore(String trans) {
        String provean = "";
        for(ProveanData pd : proveanList) {
            if(pd.trans.equals(trans)) {
                provean = pd.score;
                break;
            }
        }
        return provean;
    }
    // the values start at 1 and the ruler starts at 0 - reconcile, see aligned arghdia protein !!!!
    // when it's not aligned, the left part starts properly at 0 (1)! see what's diff
    protected void displayExons(GraphicsContext gc, double xoffset, double leftPadding, double ymain, double ppb, boolean negStrand,
                              ArrayList<SeqAlign.Position> tgexonsList, AnnotationEntry seqEntry) {
        // determine starting and ending exon positions for transcript
        // and draw dashed line for full length
        Collections.sort(tgexonsList);
        int min = 0;
        int max = 0;
        for(SeqAlign.Position ptg : tgexonsList) {
            if(ptg.start < min || min == 0)
                min = ptg.start;
            if(ptg.end > max)
                max = ptg.end;
        }
        gc.setLineWidth(0.25);
        double x = xoffset + leftPadding + (Math.floor((seqAlign.getXlatedPos(min)+2)/3) -1) * ppb;
        x = adjustX(x);
        double ddw = Math.floor((seqAlign.getXlatedPos(max) - seqAlign.getXlatedPos(min) + 1)/3) - 1;
        symbols.drawDashedLine(gc, x, ymain, x + (ddw * ppb), defaultColor);
        gc.setLineWidth(1.0);

        // add aligned position to transcript tooltip
        double ttstr = seqAlign.getAlignedPos(seqEntry.strpos, tgexonsList);
        double ttend = seqAlign.getAlignedPos(seqEntry.endpos, tgexonsList);
        String fmtpos = String.format("%s%s", NumberFormat.getInstance().format(ttstr),
                        (ttstr == ttend)? "" : String.format(" - %s", NumberFormat.getInstance().format(ttend)));
        seqEntry.tooltipAligned = String.format("\nAligned: %s", fmtpos);
        //System.out.println(String.format("\nProt Aligned: %s", fmtpos));

        ArrayList<SeqAlign.SegmentDisplay> segList = seqAlign.getCdsSegments(tgexonsList, tgexonsList);
        for(SeqAlign.SegmentDisplay sd : segList)
            dspCDS(gc, xoffset + leftPadding, ymain, ppb, sd);
    }
    protected void dspDomain(GraphicsContext gc, double xi, double y, double height, double ppb, 
                                AnnotationEntry entry, ArrayList<SeqAlign.Position> tgexonsList) {
        ArrayList<SeqAlign.SegmentDisplay> segList = seqAlign.getCdsSegments(tgexonsList, tgexonsList);
        ArrayList<SeqAlign.Position> posList = new ArrayList<>();
        for(SeqAlign.SegmentDisplay sd : segList) {
            //System.out.println("sds: " + ((int)Math.floor((seqAlign.getXlatedPos(sd.start)+2)/3)) + ".." + ((int)Math.floor((seqAlign.getXlatedPos(sd.end)+2)/3)));
            posList.add(new SeqAlign.Position((int)Math.floor((seqAlign.getXlatedPos(sd.start)+2)/3), 
                                     (int)Math.floor((seqAlign.getXlatedPos(sd.end)+2)/3)));
        }
        int endseg;
        double x, width;
        int strseg = (int)Math.floor(seqAlign.getAlignedPos(entry.strpos * 3, tgexonsList)/3);
        int endpos = (int)Math.floor(seqAlign.getAlignedPos(entry.endpos * 3, tgexonsList)/3);
        AnnotationEntry segEntry;
        int totalWidth = 0;
        //System.out.println("sp: " + strseg + ", ep: " + endpos + ", entry: " + entry.strpos + ".." + entry.endpos);
        boolean done = false;
        int segcnt = 0;
        for(SeqAlign.Position pos : posList) {
            if(!done && strseg >= pos.start && strseg <= pos.end) {
                done = true;
                endseg = endpos;
                if(endpos > pos.end) {
                    //System.out.println("Split domain segment: " + endpos + " > " + pos.end);
                    endseg = pos.end;
                    done = false;
                }
                totalWidth += (endseg - strseg + 1);
                //System.out.println("Domain segment: " + strseg + ".." + endseg);
                width = endseg - strseg + 1;
                x = adjustX(xi + (strseg - geneEntry.strpos) * ppb);
                gc.setGlobalAlpha(0.75);
                gc.setFill(entry.symbol.colors.fill);
                gc.setStroke(entry.symbol.colors.stroke);
                gc.fillRect(x, y, width * ppb, height);
                gc.strokeRect(x, y, width * ppb, height);
                gc.setGlobalAlpha(1.0);
                gc.setTextAlign(TextAlignment.LEFT);
                gc.setFill(Color.valueOf("#606060"));
                
                // add tooltip, check if multiple segments, need to create new entry for tooltip
                if(segcnt > 0)
                    segEntry = new AnnotationEntry("", "", "", "", "", "", entry.strpos, entry.endpos, "", "", "");
                else
                    segEntry = entry;
                String fmtpos = String.format("%s%s", NumberFormat.getInstance().format(strseg),
                                (strseg == endseg)? "" : String.format(" - %s", NumberFormat.getInstance().format(endseg)));
                segEntry.tooltipAligned = String.format("\nAligned: %s", fmtpos);
                fmtpos = String.format("%s%s", NumberFormat.getInstance().format(entry.strpos),
                                (entry.strpos == entry.endpos)? "" : String.format(" - %s", NumberFormat.getInstance().format(entry.endpos)));
                tipsList.add(new ToolTipPoints(segEntry, x, y, width * ppb, height,
                                    String.format("%s\nPosition: %s", entry.tooltip, fmtpos), false));
                
                // set for next segment if spanning across gap
                if(!done)
                    strseg = (int)Math.floor(seqAlign.getAlignedPos((entry.strpos + totalWidth) * 3, tgexonsList)/3);
                segcnt++;
            }
        }
    }
    // tooltip displayed positions can be off by 1 due to (x+2)/3 - fix later, not a big deal
    protected void dspCDS(GraphicsContext gc, double x, double y, double ppb, SeqAlign.SegmentDisplay sd) {
        double height = AnnotationSymbols.RNAblockHeight;
        x = adjustX(x + Math.floor((sd.x+2)/3) * ppb);
        y = adjustY(y - (height/2));
        symbols.drawRNAblock(gc, x, y, Math.floor((sd.width+2)/3) * ppb);

        AnnotationEntry blk = new AnnotationEntry("", "", "", "", "", "", 0, 0, "", "", "");
        String fmtpos = String.format("%s%s", NumberFormat.getInstance().format(Math.floor((sd.xRelative+2)/3)),
                        String.format(" - %s", NumberFormat.getInstance().format(Math.floor((sd.xRelative+2)/3) + Math.floor((sd.width+2)/3) - 1)));
        tipsList.add(new ToolTipPoints(blk, x, y, ((sd.width+2)/3) * ppb, adjustValue(height),
                            String.format("%s\nPosition: %s", "CDS", fmtpos), false));
        double ttstr = Math.floor((sd.x+2)/3);
        double ttend = Math.floor((sd.x+2)/3) + Math.floor((sd.width+2)/3) - 1;
        fmtpos = String.format("%s%s", NumberFormat.getInstance().format(ttstr),
                        (ttstr == ttend)? "" : String.format(" - %s", NumberFormat.getInstance().format(ttend)));
        blk.tooltipAligned = String.format("\nAligned: %s", fmtpos);
    }
    @Override
    protected void getRSAnnotationData(String rsid, ArrayList<String[]> lines, ArrayList<AnnotationEntry> alst) {
        ArrayList<AnnotationEntry> al = new ArrayList<>();
        try {
            boolean process = false;
            boolean transflg = false;
            int strpos, endpos;
            HashMap<String, String> attrs;
            String gi = "";
            for(String[] fields : lines) {
                if(fields[0].equals(rsid)) {
                    String source = fields[1];
                    String type = fields[2];
                    if(project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.PROTEIN)) {
                        process = true;
                        transflg = false;
                    }
                    else if(project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.TRANSCRIPT)) {
                        process = false;
                        transflg = true;
                    }
                    else if(project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.GENOMIC)) {
                        process = false;
                        transflg = false;
                    }
                    if(process || 
                            project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.TRANSCRIPT) || 
                            project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.GENE) || 
                            project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.CDS) || 
                            project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.EXON)) {
                        if(project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.CDS) && !transflg)
                            continue;
                        // skip past non-positional annotations
                        if(fields[3].isEmpty() || fields[3].equals(".") || fields[4].isEmpty() || fields[4].equals("."))                          
                            continue;

                        String id = "";
                        String name = "";
                        String desc;
                        String caption = type;
                        attrs = DataAnnotation.getAttributes(fields[8]);
                        if(attrs.containsKey(project.data.getAnnotationFileDefs().attrId))
                            id = attrs.get(project.data.getAnnotationFileDefs().attrId);
                        if(attrs.containsKey(project.data.getAnnotationFileDefs().attrName))
                            name = attrs.get(project.data.getAnnotationFileDefs().attrName);
                        String tooltip = "Source: " + source + "\nFeature: " + type;
                        if(!id.isEmpty()) {
                            caption = id;
                            tooltip += "\nID: " + id;
                        }
                        if(!name.isEmpty() && !name.toLowerCase().equals(id.toLowerCase())
                                           && !name.toLowerCase().equals(type.toLowerCase())) {
                            if(id.isEmpty())
                                caption = name;
                            tooltip += "\nName: " + name;
                        }
                        if(attrs.containsKey(project.data.getAnnotationFileDefs().attrDesc)) {
                            desc = attrs.get(project.data.getAnnotationFileDefs().attrDesc);
                            if(!desc.isEmpty() && !desc.toLowerCase().equals(id.toLowerCase()) 
                                               && !desc.toLowerCase().equals(name.toLowerCase())
                                               && !desc.toLowerCase().equals(type.toLowerCase()))
                                tooltip += "\nDescription: " + desc;
                        }
                        if(project.data.getRsvdAnnotFeature(source, type).equals(DataAnnotation.AnnotFeature.PROTEIN)) {
                            tooltip = "Transcript " + fields[0];
                            tooltip += "\nStrand: " + (fields[6].trim().equals("-")? "Negative" : "Positive");
                            tooltip += "\nProtein " + id;
                        }
                        
                        String urlValue = "";
                        strpos = Integer.parseInt(fields[3]);
                        endpos = Integer.parseInt(fields[4]);
                        if(endpos < strpos) {
                            int tmp = endpos;
                            endpos = strpos;
                            strpos = tmp;
                        }
                        String idval = id.isEmpty()? name : id;
                        al.add(new AnnotationEntry(fields[0],fields[1],type,idval,caption,fields[6],strpos,endpos,fields[8], tooltip, urlValue));
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.logError("Unable to process annotation data for protein " + rsid + ": " + ex.getMessage());
            al.clear();
        }
        
        // must add in proper order for processing at drawing time
        // these values come from the annotation file, there are no internal cats
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(DataAnnotation.AnnotFeature.TRANSCRIPT)) {
                alst.add(a);
                break;
            }
        }
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(DataAnnotation.AnnotFeature.PROTEIN)) {
                alst.add(a);
                break;
            }
        }
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(DataAnnotation.AnnotFeature.GENE)) {
                alst.add(a);
                break;
            }
        }
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(DataAnnotation.AnnotFeature.CDS)) {
                alst.add(a);
                break;
            }
        }
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(DataAnnotation.AnnotFeature.EXON))
                alst.add(a);
        }
        for(AnnotationEntry a : al) {
            if(!project.data.getRsvdAnnotFeature(a.source, a.feature).equals(DataAnnotation.AnnotFeature.TRANSCRIPT) &&
                    !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(DataAnnotation.AnnotFeature.PROTEIN) &&
                    !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(DataAnnotation.AnnotFeature.GENE) &&
                    !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(DataAnnotation.AnnotFeature.CDS) && 
                    !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(DataAnnotation.AnnotFeature.EXON))
                alst.add(a);
        }
    }
    
    //
    // Data Classes
    //
    public class ProteinEntry {
        public String id, alias, trans, provean;
        public double x, y;
        public ProteinEntry(String id, String alias, String trans, String provean, double x, double y) {
            this.id = id;
            this.alias = alias;
            this.trans = trans;
            this.x = x;
            this.y = y;
            this.provean = provean;
        }
        public void addTrans(String trans) {
            this.trans += (this.trans.isEmpty()? "" : ", ") + trans;
        }
    }
}
