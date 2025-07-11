//
// Author: H. del Risco
//

var EBook = function(pageIds) {
	var pageIdx = 0;
	var pages= pageIds.length;
	var pageMax = pages - 1;

	this.pageSelect = function(contentId) {
		pageIdx = getPageIdx(contentId);
		updPageButtons();
		/*var endpos = contentId.indexOf("_");
		if(endpos != -1)
			gotoWebsiteUrl(contentId.replace("_", ".html#"));
		else
			gotoWebsiteUrl(contentId + ".html#Top");*/
	}
	this.pageSectionSelect = function(contentId) {
		var pos = contentId.indexOf("_");
		if(pos != -1) {
			pageIdx = getPageIdx(contentId);
			updPageButtons();
			gotoWebsiteUrl(getURL(contentId.replace("_", ".html#")));
		}
	}
	this.pageNext = function() {
		if(pageIdx < pageMax) {
			++pageIdx;
			updPageButtons();
			gotoWebsiteUrl(getURL(pageIds[pageIdx] + ".html#Top"));
		}
	}
	this.pagePrev = function() {
		if(pageIdx > 0) {
		    --pageIdx;
		    updPageButtons();
			gotoWebsiteUrl(getURL(pageIds[pageIdx] + ".html#Top"));
		}
	}

	//
	// Internal class functions
	//
	function getPageIdx(contentId) {
		var pageId = contentId;
		var endpos = contentId.indexOf("_");
		if(endpos != -1)
			pageId = contentId.substring(0, endpos);
		var idx = 0;
		for(; idx < pageMax; idx++) {
			if(pageId == pageIds[idx])
				break;
		}
		if(idx < 0)
		    idx = 0;
		else if(idx > pageMax)
		    idx = pageMax;
		return idx;
	}
	function updPageButtons() {
		document.getElementById('pagePrev').className = (pageIdx <= 0)? "contentButtonOff" : "contentControl contentButton";
		document.getElementById('pageNext').className = (pageIdx >= pageMax)? "contentButtonOff" : "contentControl contentButton";
	}
}

