/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tappas;

/**
 *
 * @author Hector del Risco - hdelrisco@ufl.edu & Pedro Salguero - psalguero@cipf.es
 */
public class WebRefs {
    public static void showGeneOnlineReferences(String gene, Project project) {
        String url = "";
        String species = project.data.getGenus() + "_" + project.data.getSpecies();
        DataApp.RefType rt = project.data.getRefType();
        if(species.equals("Homo_sapiens") || species.equals("Mus_musculus")) {
            if(rt != null) {
                if(rt.equals(DataApp.RefType.Ensembl))
                    url = "http://www.ensembl.org/" + species + "/Gene/Summary?g=" + gene;
                else {
                    String id = project.app.data.getNCBIGeneId(species, gene);
                    if(!id.isEmpty())
                        url = "https://www.ncbi.nlm.nih.gov/gene/" + id;
                    else
                        project.app.ctls.alertInformation("Gene Online Reference", "Unable to determine what online references to use for this gene.");
                }
                if(!url.isEmpty())
                    Tappas.getHost().showDocument(url);
            }
            else
                project.app.ctls.alertInformation("Gene Online Reference", "Online references for user defined annotations not currently supported.");
        }
        else
            project.app.ctls.alertInformation("Gene Online Reference", "Online references for '" + species + "' not currently supported.");
    }
    public static boolean hasOnlineReferences(Project project) {
        boolean result = false;
        String species = project.data.getGenus() + "_" + project.data.getSpecies();
        if((species.equals("Homo_sapiens") || species.equals("Mus_musculus")) && project.data.getRefType() != null)
            result = true;
        return result;
    }
}
