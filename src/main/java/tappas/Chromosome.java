/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Class to handle chromosome display for supported species.
 * <p>
 * Chromosomes are displayed in the Gene Data Visualization tabs.
 * Chromosome sizes may be updated in the future
 * but should never be genome build dependent
 * 
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class Chromosome extends AppObject {
    // Homo sapiens
    static private final List<String> CHR_HS_NAMES = Arrays.asList(
           "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
           "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "X", "Y", "M"
    );
    static private final List<Integer> CHR_HS_SIZES = Arrays.asList(
           249250622, 243199374, 198022431, 191154277, 180915261, 171115068, 159138664, 146364023, 141213432, 135534748,
           135006517, 133851896, 115169879, 107349541, 102531393, 90354754, 81195211, 78077249, 59128984, 63025521, 
           48129896, 51304567, 155270561, 59373567, 16570
    );
    static private HashMap<String, ArrayList<ChromoBand>> hmHSBands = new HashMap<>();
    
    // Mus musculus
    static private final List<String> CHR_MM_NAMES = Arrays.asList(
           "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
           "11", "12", "13", "14", "15", "16", "17", "18", "19", "X", "Y", "M"
    );
    static private final List<Integer> CHR_MM_SIZES = Arrays.asList(
           195471972, 182113225, 160039681, 156508117, 151834685, 149736547, 145441460, 129401214, 124595111, 130694994,
           122082544, 120129023, 120421640, 124902245, 104043686, 98207769, 94987272, 90702640, 61431567, 171031300, 91744699, 16300
    );
    static private HashMap<String, ArrayList<ChromoBand>> hmMMBands = new HashMap<>();

    ChromoInfo chromoInfo = null;
    HashMap<String, Integer> hmChromos = new HashMap<>();
    
    /**
     * Instantiates a Chromosome object - constructor
     * <p>
     * Chromosome objects are species specific
     * 
     * @param   species species, see DataApp.Species
     */
    public Chromosome(String genus, String species) {
        super(null, null);
        String name = genus + "_" + species;
        
        // setup chromosome data based on genus_species
        switch(name) {
            case "Mus_musculus":
                if(hmMMBands.isEmpty())
                    hmMMBands = loadBands(Paths.get(app.data.getAppDataFolder(), DataApp.APP_MM_CHROMOBANDS));
                chromoInfo = new ChromoInfo(CHR_MM_NAMES, CHR_MM_SIZES, hmMMBands);
                break;
            case "Homo_sapiens":
                if(hmHSBands.isEmpty())
                    hmHSBands = loadBands(Paths.get(app.data.getAppDataFolder(), DataApp.APP_HS_CHROMOBANDS));
                chromoInfo = new ChromoInfo(CHR_HS_NAMES, CHR_HS_SIZES, hmHSBands);
                break;
        }
        
        // setup individual chromosome sizes
        if(chromoInfo != null) {
            int idx = 0;
            for(String chrName : chromoInfo.lstNames) {
                hmChromos.put(chrName, chromoInfo.lstSizes.get(idx));
                idx++;
            }
        }
    }
    /**
     * Returns a list of chromosome names
     * <p>
     * Names list is hard coded inside class
     * 
     * @return  list of chromosome names
     */
    public List<String> getChromoNames() {
        if(chromoInfo != null)
            return chromoInfo.lstNames;
        else
            return new ArrayList<>();
    }
    /**
     * Returns size, in bases, for given chromosome
     * 
     * @param   chromo  chromosome, may be specified as 'chr1' or '1'
     * 
     * @return  size chromosome in bases
     */
    public int getChromoSize(String chromo) {
        int size = 0;
        String chr = getChromosome(chromo);
        if(hmChromos.containsKey(chr)) {
            if(chromoInfo.hmBands.containsKey(chr)) {
                for(ChromoBand cb : chromoInfo.hmBands.get(chr)) {
                    if(cb.end > size)
                        size = cb.end;
                }
            }
            else
                size = hmChromos.get(chr);
        }
        return size;
    }
    /**
     * Draw given chromosome in GC at given position
     * <p>
     * Chromosome may be specified as 'chr1' or '1'
     * 
     * @param   chromo  chromosome, may be specified as 'chr1' or '1'
     * @param   gc      GraphicsContext to draw in
     * @param   x       starting x position
     * @param   y       starting y position
     * @param   width   amount of horizontal space to use
     * @param   height  amount of vertical space to use
     * @param   start   starting x position for location bar
     * @param   end     starting y position for location bar
     */
    public void drawChromosome(String chromo, GraphicsContext gc, double x, double y, double width, double height, int start, int end) {
        int size = getChromoSize(chromo);
        if(size > 0) {
            String chr = getChromosome(chromo);
            
            // draw chromosome rectangle outline
            double ppb = width / size;
            gc.setFill(Color.TRANSPARENT);
            gc.setStroke(Color.BLACK);
            gc.fillRect(x, y, width, height);
            gc.setLineWidth(0.25);
            gc.strokeRect(x, y, width, height);
            
            if(chromoInfo.hmBands.containsKey(chr)) {
                for(ChromoBand cb : chromoInfo.hmBands.get(chr)) {
                    gc.setFill(Color.web(cb.color, 1.0));
                    gc.fillRect(x + cb.start * ppb, y, (cb.end - cb.start + 1) * ppb, height);
                }
            }
            
            // draw start/end position
            double xp = x - 5;
            double yp = y + 10;
            gc.setFill(Color.PLUM);
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText("0", xp, yp);
            xp = xp + width + 10;
            gc.setTextAlign(TextAlignment.LEFT);
            gc.fillText(ViewAnnotation.getRegionXLabel(size), xp, yp);

            // draw bar in chromosome area
            x += start * ppb;
            y -= 2;
            double h = height + 4;
            double w = Math.max(1, (end - start + 1) * ppb);
            gc.setFill(Color.RED);
            gc.setStroke(Color.RED);
            gc.fillRect(x, y, w, h);
        }
    }
    
    //
    // Static Functions
    //

    /**
     * Returns chromosome name without prefix
     * 
     * @param   chromo  chromosome, may be specified as 'chromosome1', 'chr1' or '1'
     * @return          chromosome name without any prefix, e.g. '1'
     */
    static public String getChromosome(String chromo) {
        String chr = chromo.toUpperCase();
        if(chr.startsWith("CHROMOSOME"))
            chr = chr.substring(10);
        else if(chr.startsWith("CHROMO"))
            chr = chr.substring(6);
        else if(chr.startsWith("CHR"))
            chr = chr.substring(3);
        return chr.trim();
    }
    
    //
    // Internal Functions
    //
    
    private HashMap<String, ArrayList<ChromoBand>> loadBands(Path filepath) {
        System.out.println("Reading chromosome bands data from " + filepath.toString() + ".");
        HashMap<String, ArrayList<ChromoBand>> hm = new HashMap<>();
        if(Files.exists(filepath)) {
            try {
                logger.logDebug("Reading chromosome bands data from " + filepath.toString() + ".");
                List<String> lines = Files.readAllLines(filepath, StandardCharsets.UTF_8);

                int lnum = 1;
                ChromoBand cb;
                ArrayList<ChromoBand> lst;
                for(String line : lines) {
                    if(!line.trim().isEmpty() && !line.startsWith("#")) {
                        cb = new ChromoBand(line, lnum);
                        if(!hm.containsKey(cb.chr)) {
                            lst = new ArrayList<>();
                            lst.add(cb);
                            hm.put(cb.chr, lst);
                        }
                        else {
                            lst = hm.get(cb.chr);
                            lst.add(cb);
                        }
                    }
                    lnum += 1;
                }
            }
            catch (Exception e) {
                hm = new HashMap<>();
                app.logError("Unable to load chromosome bands data file: " + e.getMessage());
            }
            
            for(ArrayList<ChromoBand> lst: hm.values())
                Collections.sort(lst);
        }
        return hm;
    }
    
    //
    // Data Classes
    //
    
    static private class ChromoInfo {
        List<String> lstNames;
        List<Integer> lstSizes;
        HashMap<String, ArrayList<ChromoBand>> hmBands;
        public ChromoInfo(List<String> lstNames, List<Integer> lstSizes, HashMap<String, ArrayList<ChromoBand>> hmBands) {
            this.lstNames = lstNames;
            this.lstSizes = lstSizes;
            this.hmBands = hmBands;
        }
    }
    static private class ChromoBand  implements Comparable<ChromoBand> {
        int start, end;
        String chr, name, gieStain, color;
        public ChromoBand(String chr, int start, int end, String name, String gieStain) {
            this.chr = getChromosome(chr);
            this.start = start;
            this.end = end;
            this.name = name;
            this.gieStain = gieStain.toLowerCase();
            this.color = getBandColor(gieStain);
        }
        public ChromoBand(String entry, int lineNum)  throws Exception  {
            String[] fields = entry.split("\t");
            if(fields.length == 5) {
                chr = getChromosome(fields[0].trim());
                start = Integer.parseInt(fields[1].trim());
                end = Integer.parseInt(fields[2].trim());
                name = fields[3].trim();
                gieStain = fields[4].trim().toLowerCase();
                color = getBandColor(gieStain);
            }
            else
                throw new Exception("Invalid chromosome band file input line (" + lineNum + ").");
        }
        private String getBandColor(String gieStain) {
            String bandColor;
            switch (gieStain) {
                case "acen":
                    bandColor = "0xA52A2A";
                    break;
                case "gpos100":
                    bandColor = "0x000000";
                    break;
                case "gpos75":
                    bandColor = "0x404040";
                    break;
                case "gpos66":
                    bandColor = "0x808080";
                    break;
                case "gpos50":
                    bandColor = "0xA0A0A0";
                    break;
                case "gpos33":
                    bandColor = "0xC0C0C0";
                    break;
                case "gpos25":
                    bandColor = "0xD4D4D4";
                    break;
                default:
                    bandColor = "0xFFFFFF";
                    break;
            }
            return bandColor;
        }
        
        @Override
        public int compareTo(ChromoBand cb) {
            return ((start > cb.start)? 1 : (start < cb.start)? -1 : 0);
        }
    }
    static public class ChromoComparator implements Comparator<String>{
        @Override
        public int compare(String s1, String s2) {
            String c1 = getChromosome(s1);
            String c2 = getChromosome(s2);
            for(Character c : c1.toCharArray()) {
                if(!Character.isDigit(c))
                    return c1.compareToIgnoreCase(c2);
            }
            for(Character c : c2.toCharArray()) {
                if(!Character.isDigit(c))
                    return c1.compareToIgnoreCase(c2);
            }
            Integer i1, i2;
            try {
                i1 = Integer.parseUnsignedInt(c1);
                i2 = Integer.parseUnsignedInt(c2);
            }
            catch(Exception e) {
                return c1.compareToIgnoreCase(c2);
            }
            return i1.compareTo(i2);
        }
    }
}
