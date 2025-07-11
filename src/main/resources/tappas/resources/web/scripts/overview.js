//
// Author: H. del Risco
//
// Warning: this code is shared with the application so handle accordingly (header/footer/navigation )
//

var hideContextMenu = false;
var ebook, contentItemsWidth;
var pageIds = ["pageIntro", "pageAppXface", "pageAppTips", "pageProjects", "pageDataViz", "pageDrillDown", "pageExport", "pageQuery", "pageDEA", "pageDIU", "pageAFA", "pageEA"];
/*
var contentEntries = [["Introduction", ["pageIntro_Top", "pageIntro_Reqs"]], 
	["Projects", ["pageProjects_Top", "pageProjects_InputData", "pageProjects_Normalization", "pageProjects_DesignFileFormat", "pageProjects_ExpMatrixFileFormat", "pageProjects_AnnotationFileFormat"]],
	["Application Interface", ["pageAppXface_Top", "pageAppXface_Layout", "pageAppXface_Menus", 
			  "pageAppXface_Tabs", "pageAppXface_SubtabMenuBar", "pageAppXface_Tables", "pageAppXface_Visual", "pageAppXface_Tips"]], 
	["Application Tips", ["pageAppTips_Top"]],
	["Data Visualization", ["pageDataViz_Top", "pageDataViz_DVAccess", "pageDataViz_GeneDV"]],
	["Data Drill Down", ["pageDrillDown_Top"]],
	["Export Data and Images", ["pageExport_Top"]],
	["Ad Hoc Query", ["pageQuery_Top", "pageQuery_Search", "pageQuery_Query"]],
	["Differential Expression Analysis", ["pageDEA_Top"]],
	["Differential Isoform Usage Analysis", ["pageDIU_Top"]],
	["Functional Diversity Analysis", ["pageAFA_Top", "pageAFA_FDA", "pageAFA_DFI"]],
	["Enrichment Analysis", ["pageEA_Top", "pageEA_FEA", "pageEA_FEAClusters", "pageEA_GSEA"]]];*/
var picImgs = ["images/GeneDV1.png", "images/GeneDV2.png", "images/GeneDV3.png", "images/GeneDV4.png", 
			   "images/GeneDV5.png", "images/GeneDV6.png", "images/GeneDV7.png"];
var picMaps = ["", "", "", "", "", "", ""];
var imgWidth = 1310;
var slides;

function init(website, pageId, pageTitle) {
	if(website) {
		navSelect("navOverview");
		document.getElementById("headerSectionName").innerHTML = "Overview";
	}
	document.getElementById('contentTitle').innerHTML = pageTitle;
	document.getElementById(pageId + "_Top").className = "contentItemSelected";
	if(document.getElementById(pageId + "_subitems"))
		document.getElementById(pageId + "_subitems").style.display = "block";
	var elarray = document.getElementById(pageId + "_Top").childNodes;
	for(var i = 0; i < elarray.length; i++) {
		if(elarray[i].nodeName == "A") {
			elarray[i].className = "selected";
			break;
		}
	}
	ebook = new EBook(pageIds);
	contentItemsWidth = document.getElementById("contentItems").offsetWidth;
	slides = new Slides(picImgs, picMaps, "imgMain", imgWidth, "imgPrev", "imgNext");
	ebook.pageSelect(pageId);
// the old code used to first load the #Top and then change to #xxx and it worked properly
// now we load #xxx by A tag and in some cases the offset is a little off
// changing here does not work and even rigged up to go to #Top and then change failed
// the problem seems to be with the space of the top (section title), it is properly aligned in the application
/*
	if(window.location.href.indexOf("#") != -1 && window.location.href.indexOf("#Top") == -1) {
		var href = window.location.href;
		var pos = window.location.href.lastIndexOf("#");
		if(pos != -1) {
//alert(window.location.href.substring(0, pos + 1) + "Top");
			window.location.replace(window.location.pathname + "#Top");
alert(window.location.pathname + "#Top  -- " + window.location.pathname + "#" + href.substring(pos + 1));
			window.location.replace(window.location.pathname + "#" + href.substring(pos + 1));
		}
	}
	if(window.location.href.indexOf("Query") != -1) {
		window.location.replace(window.location.pathname + "#Search");
	}*/

	// if hideCM is given, always hide context menu (shown by default)
	var pos = window.location.href.search("hideCM=");
	if(pos != -1)
		toggleContents();
	onResize();
}
function toggleContents() {
	hideContextMenu = document.getElementById("contentControl").innerHTML.includes("Show")? false : true;
	document.getElementById("contentItems").style.display = hideContextMenu? "none" : "block";
	document.getElementById("contentControl").innerHTML = hideContextMenu? "Show Contents &lt;&lt;" : "Hide Contents &gt;&gt;";
	onResize();
}
function getURL(url) {
	if(hideContextMenu)
		return url + "?hideCM=1";
	else
		return url;
}
function onResize() {
	var minWidth = 725;
	var totalWidth = parseInt(document.getElementById("mainSection").offsetWidth);
	var totalHeight = parseInt(document.getElementById("mainSection").offsetHeight);
	var clWidth = document.getElementById("contentItems").style.display == "none"? 0 : contentItemsWidth;
	var contentWidth = totalWidth - clWidth - 20;
	if(contentWidth < minWidth) {
		// make sure to hide contents menu, adjust content size (if menu was not already hidden)
		document.getElementById("contentItems").style.display = "none";
		document.getElementById("tdContentControl").style.display = "none";
		contentWidth += clWidth;
	}
	else {
		// make sure to redisplay contents menu if it is set to be shown and there is enough room
		if((contentWidth + clWidth - contentItemsWidth) >= minWidth) {
			if(document.getElementById("contentControl").innerHTML.includes("Hide"))
				document.getElementById("contentItems").style.display = "block";
			document.getElementById("tdContentControl").style.display = "table-cell";
		}
	}
	document.getElementById("contentSection").style.width = contentWidth + "px";
}
// attempt to make the browser's back/forward buttons work for hahsmark links - seems to work properly
//function onHashChanged() {
//	window.location.assign(window.location.href);
//}


