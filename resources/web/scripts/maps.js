//
// Credit https://stackoverflow.com/questions/13321067/dynamically-resizing-image-maps-and-images
// Modified by H. del Risco
//   Changed to use an array of maps and to always use original image width to compute the new coordinates
//

var ImageMap = function (maps, imgId, imgWidth) {
	var mapCoords = {};
	for(var idx = 0; idx < maps.length; idx++) {
		var mapId = maps[idx];
		if(mapId != "") {
			var map = document.getElementById(mapId),
				areas = map.getElementsByTagName('area'),
				len = areas.length,
				coords = [];
				mapCoords[mapId] = coords;
			for(var n = 0; n < len; n++) {
				coords[n] = areas[n].coords.split(',');
			}
		}
	}
	this.resize = function () {
		var img = document.getElementById(imgId);
		for(var mapId in mapCoords) {
			var coords = mapCoords[mapId],
				map = document.getElementById(mapId),
				areas = map.getElementsByTagName('area'),
				len = areas.length,
				factor = img.offsetWidth / imgWidth,
				newCoords = new Array(len);
			for(var n = 0; n < len; n++) {
				var clen = coords[n].length;
				newCoords[n] = new Array(clen);
				for (var m = 0; m < clen; m++)
				    newCoords[n][m] = factor * coords[n][m];
				areas[n].coords = newCoords[n].join(',');
			}
		}
	    return true;
	};
	window.onresize = this.resize;
}

