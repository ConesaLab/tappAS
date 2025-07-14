/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */

public class AnnotationFiles extends AppObject {
    // limit the number of characters in list returned
    // to avoid getting back something unexpected
    // limit!!! maybe to load cancer project
    static long MAX_LIST_SIZE = 50000;
    
    public AnnotationFiles(App app) {
        super(app);
    }
    
    // WARNING: should run from a background thread due to possible delays
    //          list is built based on the assumption HTML is using href="filename"
    // path contains the folder path where all the annotation files are located
    public ArrayList<AnnotationFileInfo> getServerFilesList(String path) {
        ArrayList<AnnotationFileInfo> lst = new ArrayList<>();
        
        // establish server connection to folder
        long cntTotal;
        URLConnection conn = null;
        try {
            URL url = new URL(path);
            conn = url.openConnection();
            cntTotal = conn.getContentLengthLong();
            System.out.println("Server files list size: " + cntTotal);
            String list = "";
            if(cntTotal > 0 && cntTotal < MAX_LIST_SIZE) {
                // download data
                byte[] buf = new byte[1024 * 1024];
                InputStream is = conn.getInputStream();
                int rdcnt = is.read(buf);
                System.out.println("Started data download loop...");
                while(rdcnt != -1) {
                    list += new String(buf, 0, rdcnt, StandardCharsets.UTF_8);
                    rdcnt = is.read(buf);
                }
                try { is.close(); } catch (Exception e) {}
                System.out.println("Done with data download loop...");
                
                // we have the html page contents for the list - need to parse it now
                int idx = 0;
                do {
                    idx = list.indexOf("href=\"", idx);
                    if(idx != -1) {
                        int eidx = list.indexOf("\"", idx + 6);
                        if(eidx != -1) {
                            // it is possible for non-valid hrefs to be in the list
                            String file = list.substring(idx + 6, eidx);
                            AnnotationFileInfo afi = AnnotationFileInfo.parseFile(file, false);
                            if(afi != null)
                                lst.add(afi);
                        }
                        idx += 6;
                    }
                } while(idx != -1);
            }
            else
                logger.logWarning("Invalid server annotation file list size: " + cntTotal);
        }
        catch(Exception e) {
            if(conn == null) {
                logger.logWarning("Unable to establish tappAS server connection: " + e.getMessage());
            }
            else {
                logger.logWarning("Unable to download annotation files list: " + e.getMessage());
            }
        }
        return lst;
    }
    
    public ArrayList<String> getServerDownloadsFilesList(String path) {
        ArrayList<String> lst = new ArrayList<>();
        
        // establish server connection to folder
        long cntTotal;
        URLConnection conn = null;
        try {
            URL url = new URL(path);
            conn = url.openConnection();
            cntTotal = conn.getContentLengthLong();
            System.out.println("Server files list size: " + cntTotal);
            String list = "";
            if(cntTotal > 0 && cntTotal < MAX_LIST_SIZE) {
                // download data
                byte[] buf = new byte[1024 * 1024];
                InputStream is = conn.getInputStream();
                int rdcnt = is.read(buf);
                System.out.println("Started data download loop...");
                while(rdcnt != -1) {
                    list += new String(buf, 0, rdcnt, StandardCharsets.UTF_8);
                    rdcnt = is.read(buf);
                }
                try { is.close(); } catch (Exception e) {}
                System.out.println("Done with data download loop...");
                
                // we have the html page contents for the list - need to parse it now
                int idx = 0;
                do {
                    idx = list.indexOf("href=\"", idx);
                    if(idx != -1) {
                        int eidx = list.indexOf("\"", idx + 6);
                        if(eidx != -1) {
                            // it is possible for non-valid hrefs to be in the list
                            String file = list.substring(idx + 6, eidx);
                            if(file != null)
                                lst.add(file);
                        }
                        idx += 6;
                    }
                } while(idx != -1);
            }
            else
                logger.logWarning("Invalid server downloads file list size: " + cntTotal);
        }
        catch(Exception e) {
            if(conn == null) {
                logger.logWarning("Unable to establish tappAS server connection: " + e.getMessage());
            }
            else {
                logger.logWarning("Unable to download tappAS files list: " + e.getMessage());
            }
        }
        return lst;
    }

    public ArrayList<AnnotationFileInfo> getLocalFilesList() {
        ArrayList<AnnotationFileInfo> lst = new ArrayList<>();
        
        // get list of folders in annotation reference folder
        ArrayList<String> lstFolders = Utils.getFolderSubFolders(Paths.get(app.data.getAppReferenceFileBaseFolder()));
        String[] fields;
        String genus, species;
        for(String folder : lstFolders) {
            fields = folder.split("_");
            if(fields.length == 2) {
                genus = fields[0].trim();
                species = fields[1].trim();
                ArrayList<String> lstRefs = Utils.getFolderSubFolders(Paths.get(app.data.getAppReferenceFileBaseFolder(), folder));
                for(String ref : lstRefs) {
                    // check if it is a valid reference type
                    if(ref.equals(DataApp.RefType.Demo.name()) || ref.equals(DataApp.RefType.Ensembl.name()) || ref.equals(DataApp.RefType.RefSeq.name())) {
                        ArrayList<String> lstRels = Utils.getFolderSubFolders(Paths.get(app.data.getAppReferenceFileBaseFolder(), folder, ref));
                        for(String rel : lstRels) {
                            // make sure it was downloaded and decompressed successfully
                            // we don't set the local file extension since it's not needed and requires additional work
                            if(Files.exists(Paths.get(app.data.getAppReferenceFileBaseFolder(), folder, ref, rel, DataApp.REFDOWNLOAD_OK)))
                                lst.add(new AnnotationFileInfo(genus, species, ref, rel, "", true));
                        }
                    }
                }
            }
        }
        return lst;
    }

    // get full list of annotation files available to user - for use in creating project and data input
    // the server list is only retrieved once at the start of the application to avoid delays
    public ArrayList<AnnotationFileInfo> getAnnotationFilesList(ArrayList<AnnotationFileInfo> lstServer) {
        // get local annotation files and merge with local one
        ArrayList<AnnotationFileInfo> lstFiles = getLocalFilesList();
        for(AnnotationFileInfo afi : lstServer) {
            boolean fnd = false;
            for(AnnotationFileInfo afil : lstFiles) {
                if(afi.compare(afil)) {
                    fnd = true;
                    break;
                }
            }
            if(!fnd)
                lstFiles.add(afi);
        }
        return lstFiles;
    }
    
    //
    // Data Classes
    //
    static public class AnnotationFileInfo {
        String genus, species, reference, release, ext;
        boolean local;
        public AnnotationFileInfo(String genus, String species, String reference, String release, String ext, boolean local) {
            this.genus = genus;
            this.species = species;
            this.reference = reference;
            this.release = release;
            this.ext = ext;
            this.local = local;
        }
        public boolean compare(AnnotationFileInfo afi) {
            boolean result = false;
            // extension and local flag values are ignored
            // having the same files with different extensions will cause problems
            if(afi.genus.equals(genus) && afi.species.equals(species) && 
                    afi.reference.equals(reference) && afi.release.equals(release) && 
                    afi.release.equals(release)) {
                result = true;
            }
            return result;
        }
        @Override
        public String toString() {
            // local flag is not displayed
            return(genus + "_" + species + "_" + reference + "_" + release + ext);
        }
        public static AnnotationFileInfo parseFile(String file, boolean local) {
            AnnotationFileInfo afi = null;
            if(file.endsWith(".tar.gz") || file.endsWith(".zip")) {
                String[] fields = file.split("_");
                if(fields.length == 4) {
                    // file can have .gff3.tar.gz, etc.
                    int idx = fields[3].indexOf(".");
                    if(idx != -1) {
                        String ext = fields[3].substring(idx);
                        String release = fields[3].substring(0, idx).trim();
                        afi = new AnnotationFileInfo(fields[0], fields[1], fields[2], release, ext, local);
                    }
                }
            }
            return afi;
        }
    }
}
