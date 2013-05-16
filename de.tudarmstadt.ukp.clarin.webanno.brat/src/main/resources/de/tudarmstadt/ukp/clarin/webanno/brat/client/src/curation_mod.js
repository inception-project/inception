var CurationMod = (function($, window, undefined) {
	var CurationMod = function(dispatcher, svg) {
		var data;
		
		// send click to server
		var onMouseDown = function(evt) {
			var target = $(evt.target);
			// if clicked on a span, send ajax call to server
			if (id = target.attr('data-span-id')) {
				var span = data.spans[id];
				dispatcher.post('ajax', [ {
					action: 'selectSpanForMerge',
					id: id}, 'selectSpanForMerge']);
			}
			// TODO check for arcs
		};
		
		// callback function which is called after ajax response has arrived
		var selectSpanForMerge = function(response) {
	        if (response.exception) {
	            // TODO: better response to failure
	            dispatcher.post('messages', [[['Lookup error', 'warning', -1]]]);
	            return false;
	        }        
	        alert(response);
		};
		
		// remember data at initialization
		var rememberData = function(_data) {
			if (_data && !_data.exception) {
				data = _data;
			}
		};
		
		// register events
		dispatcher.on('mousedown', onMouseDown).
		on('dataReady', rememberData).
        on('selectSpanForMerge', selectSpanForMerge);
	};

	return CurationMod;
})(jQuery, window);
