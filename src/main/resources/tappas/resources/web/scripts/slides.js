//
// Author: H. del Risco
//

var Slides = function(picImgs, picMaps, imgId, imgWidth, imgPrvId, imgNxtId) {
	var picIdx = 0;
	var picMax = picImgs.length - 1;

	this.picImage = function(idx) {
		if(idx < 0)
		    idx = 0;
		else if(idx > picMax)
		    idx = picMax;
		picIdx = idx;
		updImage();
	}
	this.picNext = function(imgBtn) {
		if(picIdx < picMax) {
			++picIdx;
			updImage();
		}
	    this.btnOnMouseOver(imgBtn.id);
	}
	this.picPrev = function(imgBtn) {
		if(picIdx > 0) {
		    --picIdx;
		    updImage();
		}
	    this.btnOnMouseOver(imgBtn.id);
	}
	this.btnOnMouseOver = function(imgBtnId) {
		btnOnMouseOver(imgBtnId);
	}
	this.btnOnMouseOut = function(imgBtnId) {
		btnOnMouseOut(imgBtnId);
	}

	//
	// Internal class functions
	//
	function updImage() {
		img = document.getElementById(imgId);
		var src = picImgs[picIdx];
		img.src = src;
		if(picMaps != null) {
			var map = picMaps[picIdx];
			if(map != "")
				map = "#" + map;
			img.setAttribute("usemap", map);
		}
		updButtons();
	}
	function updButtons() {
		btnPrev = document.getElementById(imgPrvId);
		btnNext = document.getElementById(imgNxtId);
		if(picIdx <= 0) {
		    btnOnMouseOut(imgPrvId);
		    btnPrev.className = "slideBtnOff";
		}
		else
		    btnPrev.className = "slideBtnOn";
		if(picIdx >= picMax) {
		    btnOnMouseOut(imgNxtId);
		    btnNext.className = "slideBtnOff";
		}
		else
		    btnNext.className = "slideBtnOn";
		for(var idx = 0; idx <= picMax; idx++) {
		    var color = "lightblue";
		    if(idx == picIdx)
		        color = "steelblue";
		    document.getElementById('slide' + (idx + 1)).style.fill = color;
		}
	}
	function btnOnMouseOver(imgBtnId) {
		if((imgBtnId == imgPrvId && picIdx > 0) || (imgBtnId == imgNxtId && picIdx < picMax))
			document.getElementById(imgBtnId).style.background = "lightgray";
	}
	function btnOnMouseOut(imgBtnId) {
		document.getElementById(imgBtnId).style.background = "white";
	}
}

