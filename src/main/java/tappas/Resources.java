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
public class Resources extends AppObject {
    public static final String webResourcePath = "/tappas/resources/web/";

    public Resources(App app) {
        super(app);
    }
    public String replaceHTMLAllResources(String html) {
        String newHtml = replaceHTMLImgSrc(html);
        newHtml = replaceHTMLLinkHref(newHtml);
        newHtml = replaceHTMLScriptSrc(newHtml);
        return newHtml;
    }
    public String replaceHTMLImgSrc(String html) {
        String newHtml = html;
        int spos = newHtml.indexOf("<img src=\"");
        while(spos != -1) {
            int epos = newHtml.indexOf("\"", spos+10);
            if(epos != -1) {
                String imgName = newHtml.substring(spos+10, epos).trim();
                String resName = getClass().getResource(webResourcePath + imgName).toString();
                String updHtml = newHtml.substring(0, spos);
                updHtml += "<img src=\"" + resName + newHtml.substring(epos);
                newHtml = updHtml;
            }
            spos = newHtml.indexOf("<img src=\"", spos+1);
        }
        return newHtml;
    }
    public String replaceHTMLLinkHref(String html) {
        String newHtml = html;
        int spos = newHtml.indexOf("<link href=\"");
        while(spos != -1) {
            int epos = newHtml.indexOf("\"", spos+12);
            if(epos != -1) {
                String hrefName = newHtml.substring(spos+12, epos).trim();
                String resName = getClass().getResource(webResourcePath + hrefName).toString();
                String updHtml = newHtml.substring(0, spos);
                updHtml += "<link href=\"" + resName + newHtml.substring(epos);
                newHtml = updHtml;
            }
            spos = newHtml.indexOf("<link href=\"", spos+1);
        }
        return newHtml;
    }
    public String replaceHTMLScriptSrc(String html) {
        String newHtml = html;
        int spos = newHtml.indexOf("<script src=\"");
        while(spos != -1) {
            int epos = newHtml.indexOf("\"", spos+13);
            if(epos != -1) {
                String srcName = newHtml.substring(spos+13, epos).trim();
                System.out.println("ss: " + srcName);
                String resName = getClass().getResource(webResourcePath + srcName).toString();
                System.out.println("src: " + srcName + " - to - " + resName);
                String updHtml = newHtml.substring(0, spos);
                updHtml += "<script src=\"" + resName + newHtml.substring(epos);
                newHtml = updHtml;
            }
            spos = newHtml.indexOf("<script src=\"", spos+1);
        }
        return newHtml;
    }    
}
