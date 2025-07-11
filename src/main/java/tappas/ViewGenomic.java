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
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import tappas.DataAnnotation.AnnotFeature;

import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.List;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class ViewGenomic extends ViewAnnotation {
    public ViewGenomic(Project project) {
        super(project);
    }
    
    // Initialization
    @Override
    public boolean initialize(SubTabBase subTabBase, Canvas canvasViewer, ScrollPane canvasScrollPane, String filterCaption) {
        return super.initialize(subTabBase, canvasViewer, canvasScrollPane, filterCaption);
    }
    public void refreshViewer() {
    }
    @Override
    public void handleMouseClickAction(MouseEvent event) {
        if(event.getClickCount() == 2) {
            HashMap<String, Object> hm = new HashMap<>();
            if(app.ctlr.fxdc.isShiftKeyDown())
                hm.put("zoomOut", TabGeneDataViz.Views.GENOMIC);
            else
                hm.put("zoomIn", TabGeneDataViz.Views.GENOMIC);
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
                        else {
                            String id = app.data.getNCBIGeneId(species, tipEntry.id);
                            if(!id.isEmpty())
                                url = "https://www.ncbi.nlm.nih.gov/gene/" + id;
                            else
                                app.ctls.alertInformation("Gene Online Reference", "Unable to determine what online references to use for this gene.");
                        }
                        break;
                    case TRANSCRIPT:
                        if(rt.equals(DataApp.RefType.Ensembl))
                            url = "http://www.ensembl.org/" + species + "/Transcript/Summary?t=" + tipEntry.id;
                        else { //if(rt.equals(DataApp.RefType.Demo)) { // chg to MMDemo to allow other demos?!
                            // our demo uses the PacBio transcript as the Id
                            String id = tipEntry.id;
                            if(rt.equals(DataApp.RefType.Demo))
                                id = tipEntry.featureId;
                            
                            if(id.isEmpty())
                                app.ctls.alertInformation("Transcript Online Reference", "There are no online references for this transcript.");
                            else {
                                if(id.startsWith("ENSMUST"))
                                    url = "http://www.ensembl.org/" + species + "/Gene/Summary?g=" + id;
                                else if(id.startsWith("NM_") || id.startsWith("NR_") || id.startsWith("XM_") || id.startsWith("XR_"))
                                    url = "https://www.ncbi.nlm.nih.gov/nuccore/" + id;
                                else
                                    app.ctls.alertInformation("Transcript Online Reference", "Unable to determine what online references to use for this transcript.");
                            }
                        }
                        break;
                        
                    // Note: proteins are handled in the ViewProtein class
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
        ArrayList<SeqAlign.Position> tgcdsList = new ArrayList<>();
        AnnotationEntry seqEntry = null;
        
        //System.out.println("draw at " + args.prefs.scale + ", " + args.redrawCount + ", align: " + args.prefs.alignment);
        gc.setFill(Color.valueOf("white"));
        if(annotationList.isEmpty() || geneEntry == null) {
            double cwo = canvas.getWidth();
            double cyo = canvas.getHeight();
            canvas.setWidth(canvas.getWidth() * scale);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
            gc.setGlobalAlpha(0.75);
            gc.setFill(Color.LIGHTGRAY);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("No Genomic Information Available", cwo/2, cyo/scale/2);
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

        
        Text widthtext = new Text(title);
        widthtext.setFont(gc.getFont());
        double width =  widthtext.getLayoutBounds().getWidth();

        tipsList.add(new ToolTipPoints(geneEntry, (cw/2 - width/2), ytitle - 10, width, 16,
                            String.format("%s\n", geneEntry.tooltip), true));

        int genostrpos = Integer.MAX_VALUE;
        int genoendpos = 0;
        for(AnnotationEntry entry : annotationList) {
            AnnotFeature dbcatid = project.data.getRsvdAnnotFeature(entry.source, entry.feature);
            if(seqEntry == null && dbcatid != AnnotFeature.TRANSCRIPT) {
                logger.logError("Gene genomic annotations must have 'transcript' as first entry. Entry: " + entry.feature);
                return;
            }
            break;
        }
        
        // get exon range to draw ruler with
        int min, max;
        boolean incflg = true;
        String chromo = "";
        HashMap<String, ArrayList<SeqAlign.Position>> hmTransExons = new HashMap<>();
        HashMap<String, List> hmTransJunctions_sQTL = new HashMap<>();
        HashMap<String, SeqAlign.Position> hmTransCDS = new HashMap<>();
        HashMap<String, ArrayList<SeqAlign.Position>> hmTrans5UTR = new HashMap<>();
        HashMap<String, ArrayList<SeqAlign.Position>> hmTrans3UTR = new HashMap<>();
        for(AnnotationEntry entry : annotationList) {
            if (entry.source.equals("tappAS") && entry.feature.equals("transcript") && 
                !hmTransJunctions_sQTL.containsKey(entry.id)) {
                HashMap<String, String> transSJ = this.project.data.getTransAnnotFeatures(entry.id, "tappAS").get("splice_junction");
                String[] sQTL_junctions = ((String)transSJ.get("sQTL_canonical")).split(";");

                ArrayList<ArrayList<Float>> listOLists = new ArrayList<>();
                for (int i = 0; i < sQTL_junctions.length; i++) {
                    String pos = sQTL_junctions[i].split("G")[1];
                    ArrayList<Float> singleList = new ArrayList<>();

                    singleList.add(Float.valueOf(Float.parseFloat(pos.split("-")[0])));
                    singleList.add(Float.valueOf(Float.parseFloat(pos.split("-")[1])));

                    listOLists.add(singleList);
                } 

                hmTransJunctions_sQTL.put(entry.id, listOLists);
            } 

            if(project.data.getRsvdAnnotFeature(entry.source, entry.feature).equals(AnnotFeature.EXON)) {
                if(!hmTransExons.containsKey(entry.id))
                    hmTransExons.put(entry.id, new ArrayList<>());
                
                min = Math.min(entry.strpos, entry.endpos);
                max = Math.max(entry.strpos, entry.endpos);
                hmTransExons.get(entry.id).add(new SeqAlign.Position(entry.strpos, entry.endpos));
                if(genostrpos > min)
                    genostrpos = min;
                if(genoendpos < max)
                    genoendpos = max;
                if(entry.strand.equals("-"))
                    incflg = false;
                if(chromo.isEmpty()) {
                    HashMap<String, String> attrs = DataAnnotation.getAttributes(entry.attrs);
                    if(attrs.containsKey(project.data.getAnnotationFileDefs().attrChr))
                        chromo = Chromosome.getChromosome(attrs.get(project.data.getAnnotationFileDefs().attrChr));
                }
            }
            else if(project.data.getRsvdAnnotFeature(entry.source, entry.feature).equals(AnnotFeature.CDS)) {
                // these are offsets from start of transcript, not genomic positions
                min = Math.min(entry.strpos, entry.endpos);
                max = Math.max(entry.strpos, entry.endpos);
                hmTransCDS.put(entry.id, new SeqAlign.Position(min, max));
            }
        }
        if(genostrpos == Integer.MAX_VALUE || genoendpos == 0) {
            logger.logError("Unable to find genomic range for given entry: " + geneEntry.id);
            return;
        }

        // display subtitle
        gc.setFont(subFont);
        gc.setFill(subColor);
        gc.fillText("Genomic View - Chromosome " + chromo, (cw/2), ysubtitle);
        //gc.fillText("Genomic View - Chromosome " + chromo + (filterCaption.isEmpty()? "" : (" for " + filterCaption)), (cw/2), ysubtitle);

        
        // just for looks - will take out if doing upstream/downstream display later
        // could also change to a specific width % padding on each side for more consistent look
        double genowidth = Math.abs(genostrpos - genoendpos);
        if(genowidth > 100000) {
            genostrpos = (int)(((double)genostrpos / 10000.0)) * 10000;
            genoendpos = (int)((((double)genoendpos + 10001.0) / 10000.0)) * 10000;
        }
        else if(genowidth > 10000) {
            genostrpos = (int)(((double)genostrpos / 1000.0)) * 1000;
            genoendpos = (int)((((double)genoendpos + 1001.0) / 1000.0)) * 1000;
        }
        else {
            genostrpos = (int)(((double)genostrpos / 100.0)) * 100;
            genoendpos = (int)((((double)genoendpos + 101.0) / 100.0)) * 100;
        }

        // get translated exons for all transcripts
        int exonBase = incflg? genostrpos : genoendpos;
        for(String trans : hmTransExons.keySet()) {
            ArrayList<SeqAlign.Position> lstExons = hmTransExons.get(trans);
            Collections.sort(lstExons);
            int exonslen = 0;
            for(SeqAlign.Position pos : lstExons)
                exonslen += pos.end - pos.start + 1;
            SeqAlign.Position posTransCDS = hmTransCDS.get(trans);
            if(posTransCDS != null) {
                if(posTransCDS.start > 1)
                    hmTrans5UTR.put(trans, SeqAlign.getUTRExons(!incflg, new SeqAlign.Position(1, posTransCDS.start - 1), lstExons));
                if(posTransCDS.end < exonslen)
                    hmTrans3UTR.put(trans, SeqAlign.getUTRExons(!incflg, new SeqAlign.Position(posTransCDS.end + 1, exonslen), lstExons));
            }
            if(incflg) {
                for(SeqAlign.Position pos : lstExons) {
                    pos.start -= exonBase;
                    pos.end -= exonBase;
                }
            }
            else {
                Collections.reverse(lstExons);
                for(SeqAlign.Position pos : lstExons) {
                    int start = pos.start;
                    pos.start = exonBase - pos.end;
                    pos.end = exonBase - start;
                }
            }
        }
        for(String trans : hmTrans3UTR.keySet()) {
            ArrayList<SeqAlign.Position> lstExons = hmTrans3UTR.get(trans);
            Collections.sort(lstExons);
            if(incflg) {
                for(SeqAlign.Position pos : lstExons) {
                    pos.start -= exonBase;
                    pos.end -= exonBase;
                }
            }
            else {
                Collections.reverse(lstExons);
                for(SeqAlign.Position pos : lstExons) {
                    int start = pos.start;
                    pos.start = exonBase - pos.end;
                    pos.end = exonBase - start;
                }
            }
        }
        for(String trans : hmTrans5UTR.keySet()) {
            ArrayList<SeqAlign.Position> lstExons = hmTrans5UTR.get(trans);
            Collections.sort(lstExons);
            if(incflg) {
                for(SeqAlign.Position pos : lstExons) {
                    pos.start -= exonBase;
                    pos.end -= exonBase;
                }
            }
            else {
                Collections.reverse(lstExons);
                for(SeqAlign.Position pos : lstExons) {
                    int start = pos.start;
                    pos.start = exonBase - pos.end;
                    pos.end = exonBase - start;
                }
            }
        }
        double xoffset = 12;
        double rightPadding = 60;
        double leftPadding = 60;
        double wRegion = (cw - 2 * xoffset - rightPadding - leftPadding);
        double yoffset = ysubtitle + 7;
        double hChromo = 15;
        Chromosome chromosome = new Chromosome(project.data.getGenus(), project.data.getSpecies());
        gc.setFont(smallFont);
        chromosome.drawChromosome(chromo, gc, xoffset + rightPadding, yoffset, wRegion, hChromo, Math.min(genostrpos, genoendpos), Math.max(genostrpos, genoendpos));
        gc.setFont(defaultFont);
        yoffset += 35;
        if(args.prefs.ruler)
            yoffset += 30;
        double htop = 20;
        double hbottom = 17;
        double seqHeight = 30;
        double yRegion = yoffset - 20;
        double ymain = yoffset + htop;
        double bases = genoendpos - genostrpos + 1;
        if(bases <= 0)
            bases = 1;
        SeqAlign.Position cdspos = new SeqAlign.Position(0,0);
        SeqAlign.Position utr5pos = new SeqAlign.Position(0,0);
        SeqAlign.Position utr3pos = new SeqAlign.Position(0,0);
        double ppb = wRegion / bases;
        zoomPPB = ppb;
        System.out.println("Bases: " + bases + ", ppb: " + ppb);
        String str1;
        double x, y;
        boolean negStrand = false, nmd = false;
        double wExpLevel = leftPadding - 10;
        double expLevelPpb = (maxExpLevel == 0)? 0 : wExpLevel / maxExpLevel;

        System.out.println("wRgn: " + wRegion + ", bases: " + bases + ", ppb: " + ppb);

        double dspOffset = incflg? genostrpos : genoendpos;
        if(args.prefs.ruler)
            drawRuler(gc, xoffset + leftPadding, yoffset - 20, wRegion, args.hRegion, ppb, 1, genoendpos - genostrpos + 1, dspOffset, incflg);
        
        // process all gene annotations
        HashMap<String, ArrayList<AnnotationEntry>> hmAnnots = new HashMap<>();
        for(AnnotationEntry entry : annotationList) {
            AnnotFeature dbcatid = project.data.getRsvdAnnotFeature(entry.source, entry.feature);
            if(seqEntry == null && dbcatid != AnnotFeature.TRANSCRIPT) {
                logger.logError("Gene genomic annotations must have 'transcript' as first entry. Entry: " + entry.feature);
                return;
            }
            String cat = project.data.getTransAlignmentCategory(entry.id);
            if(cat.contains("nonsense_mediated"))
                nmd=true;
            gc.setStroke(defaultColor);
            gc.setFill(defaultColor);
            gc.setFont(defaultFont);
            switch(dbcatid) { 
                case TRANSCRIPT:
                    if(seqEntry == null) {
                        gc.setFont(smallFont);
                        x = xoffset + leftPadding - 5;
                        y = yoffset - 10;
                        gc.setFill(Color.PLUM);
                        gc.setTextAlign(TextAlignment.RIGHT);
                        gc.fillText("5' UTR", x, y);
                        x = cw - xoffset - rightPadding + 5;
                        gc.setTextAlign(TextAlignment.LEFT);
                        gc.fillText("3' UTR", x, y);
                        System.out.println("Starting transcript: " + entry.id);
                        gc.setFont(defaultFont);
                    }
                    else {
                        cdsList.clear();
                        tgexonsList.clear();
                        tgcdsList.clear();
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

                    // display transcript ID
                    gc.setFill(Color.MAROON);
                    gc.setTextAlign(TextAlignment.LEFT);
                    String rsid = "";
                    HashMap<String, String> attrs = DataAnnotation.getAttributes(entry.attrs);
                    if(attrs.containsKey(project.data.getAnnotationFileDefs().attrId)) {
                        String tid = attrs.get(project.data.getAnnotationFileDefs().attrId);
                        if(!tid.equals(entry.id))
                            rsid = " " + tid;
                    }
                    String dsptransid = entry.id;
                    double yTT = ymain - 12;
                    if(args.prefs.details) {
                        dsptransid += " (" + entry.strand + ")" + rsid;
                        gc.fillText(dsptransid, xoffset, ymain - 4);

                        Text wtxttext = new Text(dsptransid);
                        wtxttext.setFont(gc.getFont());
                        double wtxt =  widthtext.getLayoutBounds().getWidth();


                        if(!cat.isEmpty()) {
                            String dspid = "";
                            if(args.prefs.structSubClass) {
                                if(attrs.containsKey(project.data.getAnnotationFileDefs().attrSecClass))
                                    dspid = "  (" + attrs.get(project.data.getAnnotationFileDefs().attrSecClass).replaceAll("[,]", ", ") + ")";
                            }
                            gc.setFill(Color.DARKGRAY);
                            gc.fillText(cat + dspid, xoffset + wtxt + 10, ymain - 4);
                        }

                        // display expression levels if available
                        if(project.data.isCaseControlExpType() && geneIsoformsExpression.containsKey(seqEntry.id)) {
                            Font f = gc.getFont();
                            DataProject.TransExpLevels el = geneIsoformsExpression.get(seqEntry.id);
                            System.out.println("maxEL: " + maxExpLevel + ", wEL: " + wExpLevel + ", ppb: " + expLevelPpb + "x1: " + el.X1_mean + ", x2: " + el.X2_mean);
                            double xc = xoffset;
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
                            if(project.data.analysis.hasDEAData()) {
                                String der = "(not included in analysis)";
                                if(geneIsoformsDEResults.containsKey(seqEntry.id)) {
                                    der = geneIsoformsDEResults.get(seqEntry.id)? "DE" : "Not DE";
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
                            String ttip = String.format("Isoform Expression Level (normalized mean)\n" + dea +
                                         names[0] + " ExpLevel: %s   (top line)\n" +
                                         names[1] + " ExpLevel: %s   (bottom line)", 
                                         NumberFormat.getInstance().format(el1), NumberFormat.getInstance().format(el2));
                            addToolTipToList(ttip, xc, fy - 10, wExpLevel, 25, false);
                            gc.setTextAlign(TextAlignment.LEFT);
                            gc.setFont(f);
                        }
                    }
                    else {
                        Font f = gc.getFont();
                        gc.setFont(smallFont);
                        gc.fillText(dsptransid, 2, ymain - 12);
                        gc.setFont(f);
                        yTT = ymain - 20;
                    }

                    Text witext = new Text(dsptransid);
                    witext.setFont(gc.getFont());
                    width =  witext.getLayoutBounds().getWidth();

                    addToolTipToList(entry, xoffset, yTT, width, 10, true);
                    if(args.prefs.details) {
                        ymain += hbottom;
                        ymain = adjustY(ymain);
                    }
                    if(hmTransExons.containsKey(entry.id)) {
                        ArrayList<SeqAlign.Position> lstExons = hmTransExons.get(entry.id);
                        List<ArrayList<Float>> lstJunctions = hmTransJunctions_sQTL.get(entry.id);
                        SeqAlign.Position strpos = lstExons.get(0);
                        SeqAlign.Position endpos = lstExons.get(lstExons.size() - 1);
                        x = xoffset + leftPadding + strpos.start * ppb;
                        gc.setFill(defaultColor);
                        gc.setStroke(defaultColor);
                        x = adjustX(x);
                        int dspstr = strpos.start;
                        int dspend = endpos.end;
                        double dw = dspend - dspstr + 1;
                        if(dw > 1) {
                            y = adjustY(ymain - (AnnotationSymbols.IntronBlockHeight/2)); 
                            symbols.drawIntronBlock(gc, x, y, (dw * ppb));
                            gc.setFill(Color.DARKGREY);
                            x = xoffset + leftPadding + 5 + (strpos.start + dw) * ppb;
                            str1 = NumberFormat.getInstance().format(dw);
                            gc.fillText(str1 + "nt", x, ymain + 4);
                        }
                        int exon = 1;
                        for (SeqAlign.Position pos : lstExons) {
                            for (int a = 0; a < lstJunctions.size(); a++) {
                                float ini = ((Float)((ArrayList<Float>)lstJunctions.get(a)).get(0)).floatValue();
                                float end = ((Float)((ArrayList<Float>)lstJunctions.get(a)).get(1)).floatValue();
                                if ((pos.start == ini || pos.start == end) && (pos.end == ini || pos.end == end)) {
                                    double d = adjustY(ymain - 0.5D);
                                    this.symbols.drawIntronBlock(gc, x, d, dw * ppb);
                                    gc.setFill((Paint)Color.RED);
                                } 
                            } 
                        } 

                        exon = 1;

                        for(SeqAlign.Position pos : lstExons) {
                            gc.setFill(Color.RED);
                            dw = pos.end - pos.start + 1;
                            x = xoffset + leftPadding + pos.start * ppb;
                            y = adjustY(ymain - (AnnotationSymbols.CDSblockHeight/2)); 
                            symbols.drawCDSblock(gc, x, y, (dw * ppb), nmd);
                            // add tooltip
                            AnnotationEntry exonEntry;
                            exonEntry = new AnnotationEntry("", "", "", "", "", "", incflg? ((int)dspOffset + pos.start) : ((int)dspOffset - pos.start + 1),
                                                                                incflg? ((int)dspOffset + pos.end) : ((int)dspOffset - pos.end + 1), "", "", "");
                            String ttpos = String.format("%s%s", NumberFormat.getInstance().format(exonEntry.strpos),
                                            (exonEntry.strpos == exonEntry.endpos)? "" : String.format(" - %s", NumberFormat.getInstance().format(exonEntry.endpos)));
                            tipsList.add(new ToolTipPoints(exonEntry, x, y, dw * ppb, AnnotationSymbols.CDSblockHeight,
                                                String.format("Exon %s\nPosition: %s", NumberFormat.getInstance().format(exon++), ttpos), false));
                        }
                    }
                    if(hmTrans5UTR.containsKey(entry.id)) {
                        ArrayList<SeqAlign.Position> lstExons = hmTrans5UTR.get(entry.id);
                        for(SeqAlign.Position pos : lstExons) {
                            gc.setFill(Color.RED);
                            double dw = pos.end - pos.start + 1;
                            x = xoffset + leftPadding + pos.start * ppb;
                            y = adjustY(ymain - (AnnotationSymbols.CDSblockHeight/2)); 
                            symbols.drawUTRblock(gc, x, y, (dw * ppb));
                            // add tooltip
                            AnnotationEntry utrEntry;
                            utrEntry = new AnnotationEntry("", "", "", "", "", "", incflg? ((int)dspOffset + pos.start) : ((int)dspOffset - pos.start + 1),
                                                                                incflg? ((int)dspOffset + pos.end) : ((int)dspOffset - pos.end + 1), "", "", "");
                            String ttpos = String.format("%s%s", NumberFormat.getInstance().format(utrEntry.strpos),
                                            (utrEntry.strpos == utrEntry.endpos)? "" : String.format(" - %s", NumberFormat.getInstance().format(utrEntry.endpos)));
                            tipsList.add(new ToolTipPoints(utrEntry, x, y, dw * ppb, AnnotationSymbols.CDSblockHeight,
                                                String.format("5'UTR\nPosition: %s", ttpos), false));
                        }
                    }
                    if(hmTrans3UTR.containsKey(entry.id)) {
                        ArrayList<SeqAlign.Position> lstExons = hmTrans3UTR.get(entry.id);
                        for(SeqAlign.Position pos : lstExons) {
                            gc.setFill(Color.RED);
                            double dw = pos.end - pos.start + 1;
                            x = xoffset + leftPadding + pos.start * ppb;
                            y = adjustY(ymain - (AnnotationSymbols.CDSblockHeight/2)); 
                            symbols.drawUTRblock(gc, x, y, (dw * ppb));
                            // add tooltip
                            AnnotationEntry utrEntry;
                            utrEntry = new AnnotationEntry("", "", "", "", "", "", incflg? ((int)dspOffset + pos.start) : ((int)dspOffset - pos.start + 1),
                                                                                incflg? ((int)dspOffset + pos.end) : ((int)dspOffset - pos.end + 1), "", "", "");
                            String ttpos = String.format("%s%s", NumberFormat.getInstance().format(utrEntry.strpos),
                                            (utrEntry.strpos == utrEntry.endpos)? "" : String.format(" - %s", NumberFormat.getInstance().format(utrEntry.endpos)));
                            tipsList.add(new ToolTipPoints(utrEntry, x, y, dw * ppb, AnnotationSymbols.CDSblockHeight,
                                                String.format("3'UTR\nPosition: %s", ttpos), false));
                        }
                    }
                    break;
                case CDS:
                case GENOMIC:
                case GENE:
                case EXON:
                    break;
                default:
                    // we do not want to display tracks now - must display all transcripts first
                    if(!project.data.isInternalFeature(entry.source, entry.feature)) {
                        String key = entry.source + "\t" + entry.feature;
                        if(!hmAnnots.containsKey(key)) {
                            ArrayList<AnnotationEntry> lst = new ArrayList<>();
                            lst.add(entry);
                            hmAnnots.put(key, lst);
                        }
                        else {
                            boolean found = false;
                            for(AnnotationEntry ae : hmAnnots.get(key)) {
                                // chromosome and strand must match for it to be in the list, only need to check position
                                if(ae.strpos == entry.strpos && ae.endpos == entry.endpos) {
                                    found = true;
                                    break;
                                }
                            }
                            if(!found)
                                hmAnnots.get(key).add(entry);
                        }
                    }
                    break;
            }
        }
            
        // draw legend - we don't want legend here? just plain display for each?
        int cnt = seqList.size();
        y = ymain + seqHeight + (cnt - 2) * levelOffset;
        Rectangle2D.Double rc = new Rectangle2D.Double(xoffset, y, (cw - 2 * xoffset), 0);
        System.out.println("Calling with y: " + rc.y + ", cnt: " + cnt);
        if(args.redrawCount == 1)
            args.singleColumn = symbols.isSingleColumnLegend(gc, legendFont, rc);

        if(!hmAnnots.isEmpty())
            y = drawGenomicTracks(hmAnnots, gc, xoffset + leftPadding, y, ppb, genostrpos, genoendpos, negStrand);
        args.hRegion = y - yRegion;
        
        // adjust size accordingly if needed
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
    // the values start at 1 and the ruler starts at 0 - reconcile, see aligned arghdia protein !!!!
    // when it's not aligned, the left part starts properly at 0 (1)! see what's diff
    protected void displayExons(GraphicsContext gc, double xoffset, double leftPadding, double ymain, double ppb, 
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
        if(seqEntry.id.startsWith("PB.1442."))
            System.out.println(Math.floor((seqAlign.getXlatedPos(min)+2)/3));
        double x = xoffset + leftPadding + Math.floor((seqAlign.getXlatedPos(min)+2)/3-1) * ppb;
        x = adjustX(x);
        double ddw = Math.floor((seqAlign.getXlatedPos(max) - seqAlign.getXlatedPos(min) + 1)/3-1);
        symbols.drawDashedLine(gc, x, ymain, x + (ddw * ppb), defaultColor);
        gc.setLineWidth(1.0);

        // add aligned position to transcript tooltip
        double ttstr = seqAlign.getAlignedPos(seqEntry.strpos, tgexonsList);
        double ttend = seqAlign.getAlignedPos(seqEntry.endpos, tgexonsList);
        String fmtpos = String.format("%s%s", NumberFormat.getInstance().format(ttstr),
                        (ttstr == ttend)? "" : String.format(" - %s", NumberFormat.getInstance().format(ttend)));
        seqEntry.tooltipAligned = String.format("\nAligned: %s", fmtpos);
        System.out.println(String.format("\nProt Aligned: %s", fmtpos));

        ArrayList<SeqAlign.SegmentDisplay> segList = seqAlign.getCdsSegments(tgexonsList, tgexonsList);
        for(SeqAlign.SegmentDisplay sd : segList)
            dspCDS(gc, xoffset + leftPadding, ymain, ppb, sd);
    }
    // tooltip displayed positions can be off by 1 due to (x+2)/3 - fix later, not a big deal
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
        x = adjustX(x + Math.floor((sd.x+2)/3-1) * ppb);
        y = adjustY(y - (height/2));
        symbols.drawRNAblock(gc, x, y, Math.floor(sd.width/3-1) * ppb);

        AnnotationEntry blk = new AnnotationEntry("", "", "", "", "", "", 0, 0, "", "", "");
        String fmtpos = String.format("%s%s", NumberFormat.getInstance().format(Math.floor((sd.xRelative+2)/3)),
                        String.format(" - %s", NumberFormat.getInstance().format(Math.floor((sd.xRelative+2)/3+sd.width/3-1))));
        tipsList.add(new ToolTipPoints(blk, x, y, (sd.width/3-1) * ppb, adjustValue(height),
                            String.format("%s\nPosition: %s", "CDS", fmtpos), false));
        double ttstr = Math.floor((sd.x+2)/3);
        double ttend = Math.floor((sd.x+2)/3+sd.width/3-1);
        fmtpos = String.format("%s%s", NumberFormat.getInstance().format(ttstr),
                        (ttstr == ttend)? "" : String.format(" - %s", NumberFormat.getInstance().format(ttend)));
        blk.tooltipAligned = String.format("\nAligned: %s", fmtpos);
    }
    @Override
    protected void getRSAnnotationData(String rsid, ArrayList<String[]> lines, ArrayList<AnnotationEntry> alst) {
        ArrayList<AnnotationEntry> al = new ArrayList<>();
        try {
            boolean process = false;
            int strpos, endpos;
            HashMap<String, String> attrs;
            for(String[] fields : lines) {
                if(fields[0].equals(rsid)) {
                    String source = fields[1];
                    String type = fields[2];
                    if(project.data.getRsvdAnnotFeature(source, type).equals(AnnotFeature.GENOMIC)) {
                        process = true;
                    }
                    else if(project.data.getRsvdAnnotFeature(source, type).equals(AnnotFeature.TRANSCRIPT)) {
                        process = false;
                    }
                    else if(project.data.getRsvdAnnotFeature(source, type).equals(AnnotFeature.PROTEIN)) {
                        process = false;
                    }
                    if(process || 
                            project.data.getRsvdAnnotFeature(source, type).equals(AnnotFeature.TRANSCRIPT) || 
                            project.data.getRsvdAnnotFeature(source, type).equals(AnnotFeature.GENE) || 
                            project.data.getRsvdAnnotFeature(source, type).equals(AnnotFeature.CDS) || 
                            project.data.getRsvdAnnotFeature(source, type).equals(AnnotFeature.EXON)) {
                        if(project.data.getRsvdAnnotFeature(source, type).equals(AnnotFeature.SPLICEJUNCTION))
                            continue;
                        // skip past non-positional annotations
                        if(fields[3].isEmpty() || fields[3].equals(".") || fields[4].isEmpty() || fields[4].equals("."))                          
                            continue;

                        String desc;
                        String id = "";
                        String name = "";
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
                        if(project.data.getRsvdAnnotFeature(source, type).equals(AnnotFeature.TRANSCRIPT)) {
                            tooltip = "Transcript " + fields[0];
                            tooltip += "\nStrand: " + (fields[6].trim().equals("-")? "Negative" : "Positive");
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
        // maybe do in a cleaner way once the details are all worked out...
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.TRANSCRIPT)) {
                alst.add(a);
                break;
            }
        }
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.GENOMIC)) {
                alst.add(a);
                break;
            }
        }
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.GENE)) {
                alst.add(a);
                break;
            }
        }
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.CDS)) {
                alst.add(a);
                break;
            }
        }
        for(AnnotationEntry a : al) {
            if(project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.EXON))
                alst.add(a);
        }
        for(AnnotationEntry a : al) {
            if(!project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.TRANSCRIPT) &&
                    !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.GENOMIC) &&
                    !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.GENE) &&
                    !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.CDS) && 
                    !project.data.getRsvdAnnotFeature(a.source, a.feature).equals(AnnotFeature.EXON))
                alst.add(a);
        }
        //System.out.println("Genomic annotation: ");
        //for(AnnotationEntry a : alst)
        //    System.out.println(a.id + ", " + a.source + ", " + a.feature);
    }
}
