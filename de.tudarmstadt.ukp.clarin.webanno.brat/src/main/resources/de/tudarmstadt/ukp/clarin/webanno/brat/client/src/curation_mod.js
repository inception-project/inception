var CurationMod = (function($, window, undefined) {
	var CurationMod = function(dispatcher, svg) {
		var data;
		
		// send click to server
		var onMouseDown = function(evt) {
			var target = $(evt.target);
			// if clicked on a span, send ajax call to server
			if (type = target.attr('data-arc-role')) {
		          var originSpanId = target.attr('data-arc-origin');
		          var targetSpanId = target.attr('data-arc-target');
		          //var originSpan = data.spans[originSpanId];
		          //var targetSpan = data.spans[targetSpanId];
					dispatcher.post('ajax', [ {
						action: 'selectArcForMerge',
						originSpanId: originSpanId,
						targetSpanId: targetSpanId,
						type: type
					}, 'serverResult']);
			}
			if (id = target.attr('data-span-id')) {
				var editedSpan = data.spans[id];
				dispatcher.post('ajax', [ {
					action: 'selectSpanForMerge',
					id: id,
					spanType: editedSpan.type,
				}, 'serverResult']);
			}
			// TODO check for arcs
		};
		
		// callback function which is called after ajax response has arrived
		var serverResult = function(response) {
			// dummy, probably not called at all
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
        on('serverResult', serverResult);
	};

	return CurationMod;
})(jQuery, window);
