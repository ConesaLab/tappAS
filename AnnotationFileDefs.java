/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Class to load annotation file definitions.
 * <p>
 * These definitions determine how to extract various fields
 * from the GFF3 annotation file, i.e. what the field names are.
 * It also allows the code to know the name of various
 * required sources and features.
 * 
 * The loading approach in dataProject::loadAnnotationsFileDefs() could be changed 
 * to load a different annotation definition file for the project,
 * in cases where users want to use their own annotation file,
 * by providing an option in the dialog to specify the definition's file location.
 * Not worth doing anything until the subject of allowing users to create
 * their own annotation file has been fully addressed. 
 * We will probably end up allowing users to create their own annotation files dynamically 
 * using our application. In which case, it will be under our control.
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class AnnotationFileDefs extends AppObject {
    String srcGO, goP, goF, goC;
    SourceFeatureNames transcript, protein, genomic;
    SourceFeatureNames gene, cds, exon;
    SourceFeatureNames sj, pScore;
    ArrayList<SourceFeatureNames> domains = new ArrayList<>();
    String attrId, attrName, attrDesc;
    String attrChr, attrPScore, attrSJ;
    String attrPriClass, attrSecClass;
    String valSJ;

    /**
     * Instantiates an AnnotationFileDefs - constructor
     * <p>
     * Must call loadDefinitions before using definitions
     * 
     * @param   project project object if applicable, may be null
     * @param   logger  logger object if applicable, may be null - defaults to app.logger
     */
    public AnnotationFileDefs(Project project, Logger logger) {
        super(project, null);
    }
    
    /**
     * Loads annotation file definitions from given file path
     * <p>
     * Original file is provided with the application resources: annotations.def
     * 
     * @param filepath  definition file path
     * @throws java.lang.Exception
     */
    public void loadDefinitions(String filepath) throws Exception {
        int lnum = 1;
        String line;
        BufferedReader br = null;
        try {
            br = Files.newBufferedReader(Paths.get(filepath), StandardCharsets.UTF_8);
            for (; (line = br.readLine()) != null;) {
                line = line.trim();
                if(!line.isEmpty() && line.charAt(0) != '#') {
                    if(!setValue(line))
                        throw new Exception("Invalid line, " + lnum + ", found in annotation definitions file.");
                }
                lnum++;
            }
            try { br.close(); } catch(IOException e) {
                // just log a warning, file was read properly
                logger.logWarning("Closing annotation definitions file exception '" + e.getMessage() + "'");
            }
            if(!areAllValuesSet())
                throw new Exception("Incomplete annotation definitions file.");
        } catch ( IOException ioe) {
            try { if(br != null) br.close(); } catch(IOException e) { System.out.println("Closing annotation definitions file exception (within IOException) '" + e.getMessage() + "'"); }
            throw new Exception("Annotation definitions file I/O exception '" + ioe.getMessage() + "'");
        }
    }
    /**
     * Checks to see if given annotation source:feature refers to a protein domain
     * <p>
     * Protein domains are drawn differently in the Gene Data Visualization protein tab
     * 
     * @param source    annotation source
     * @param feature   annotation feature
     * @return          returns true if it is a protein domain
     */
    // the DOMAIN feature is special - it is drawn as a semi-transparent block like the CDS for transcripts
    public boolean isDomain(String source, String feature) {
        boolean result = false;
        for(SourceFeatureNames sfn : domains) {
            if(sfn.source.equals(source) && sfn.feature.equals(feature)) {
                result = true;
                break;
            }
        }
        return result;
    }
    
    //
    // Internal Functions
    //
    
    // set internal definition value from given definition file line
    private boolean setValue(String line) {
        boolean result = false;

        // pad line to allow having an empty field, e.g. "X="
        // SAF_DOMAINS can have multiple values: SAF_DOMAINS=src1,ftr1;src2,ftr2
        String id;
        String fields[];
        line += " ";
        fields = line.split("=");
        if(fields.length == 2) {
            id = fields[0].trim();
            String multivals[] = fields[1].trim().split(";");
            for (String multival : multivals) {
                fields = multival.trim().split(",");
                for(int i = 0; i < fields.length; i++) {
                    fields[i] = fields[i].trim();
                    if(fields[i].equals("N/A"))
                        fields[i] = "";
                }
                boolean processed = true;
                switch(id) {
                    //
                    // Source
                    //
                    case "SRC_GO":
                        srcGO = fields[0];
                        break;
                        //
                        // GO Features
                        //
                    case "GO_P":
                        goP = fields[0];
                        break;
                    case "GO_F":
                        goF = fields[0];
                        break;
                    case "GO_C":
                        goC = fields[0];
                        break;

                        //
                        // Source and Feature
                        //
                    case "SAF_TRANSCRIPT":
                        transcript = new SourceFeatureNames(fields[0], fields[1]);
                        break;
                    case "SAF_PROTEIN":
                        protein = new SourceFeatureNames(fields[0], fields[1]);
                        break;
                    case "SAF_GENOMIC":
                        genomic = new SourceFeatureNames(fields[0], fields[1]);
                        break;
                    case "SAF_GENE":
                        gene = new SourceFeatureNames(fields[0], fields[1]);
                        break;
                    case "SAF_CDS":
                        cds = new SourceFeatureNames(fields[0], fields[1]);
                        break;
                    case "SAF_EXON":
                        exon = new SourceFeatureNames(fields[0], fields[1]);
                        break;
                    case "SAF_SPLICEJUNCTION":
                        sj = new SourceFeatureNames(fields[0], fields[1]);
                        break;
                    case "SAF_DOMAIN":
                        domains.add(new SourceFeatureNames(fields[0], fields[1]));
                        break;
                    case "SAF_PROTEINSCORE":
                        pScore = new SourceFeatureNames(fields[0], fields[1]);
                        break;

                        //
                        // Attributes
                        //
                    case "ATTR_ID":
                        attrId = fields[0];
                        break;
                    case "ATTR_NAME":
                        attrName = fields[0];
                        break;
                    case "ATTR_DESCRIPTION":
                        attrDesc = fields[0];
                        break;
                    case "ATTR_CHROMOSOME":
                        attrChr = fields[0];
                        break;
                    case "ATTR_PROTEINSCORE":
                        attrPScore = fields[0];
                        break;
                    case "ATTR_SPLICEJUNCTION":
                        attrSJ = fields[0];
                        break;
                    case "ATTR_PRICLASS":
                        attrPriClass = fields[0];
                        break;
                    case "ATTR_SECCLASS":
                        attrSecClass = fields[0];
                        break;

                        //
                        // Special Values
                        //
                    case "NOVEL_SPLICEJUNCTION":
                        valSJ = fields[0];
                        break;

                    default:
                        processed = false;
                        break;
                }
                if(processed)
                    result = true;
            }
        }
        return result;
    }
    // check that all definition values were set
    // all definitions are required but can be set to empty string
    private boolean areAllValuesSet() {
        boolean result = false;
        if(srcGO != null && goP != null && goF != null && goC != null &&
           transcript != null && protein != null && genomic != null && 
           gene != null && cds != null && exon != null && 
           sj != null && !domains.isEmpty() && pScore != null && 
           attrId != null && attrName != null && attrDesc != null && 
           attrChr != null && attrPScore != null && attrSJ != null && 
           valSJ != null) {
                result = true;
        }
        return result;
    }
    
    //
    // Data Classes
    //
    
    public class SourceFeatureNames {
        String source, feature;
        public SourceFeatureNames(String source, String feature) {
            this.source = source;
            this.feature = feature;
        }
    }
}
