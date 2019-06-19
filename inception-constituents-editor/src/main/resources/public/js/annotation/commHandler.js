/*
 * Thesis: Collaborative Web-based Tool for Annotation of Syntactic Parse Trees
 * @author : Asha Joshi
 * Technische Universität Darmstadt 
 * 
 * This file is mainly responsible for communication handling and invoking the respective methods based on the data received  
 *
 */

$("#reconnectAlert").hide();

/**
 * Send the message in parameter using WebSocket connection
 */
var sendMessage = function(message){
	//If the connection not establish do not send message
	if(Wicket.WebSocket.INSTANCE.ws == null){
		$("#reconnectAlert").show();
	}else{
		console.log("send message", message);
		Wicket.WebSocket.send(message);
	}
}

//Start: Variable required for load test execution
var loadtest = false;
var loadtestExecute = 0;
//End: Variable required for load test execution

/**
 * Synchronization request
 */
var requestCompleteData = function(){
	iterativeupdate = false;
	//If annotation file already selected request data to server 
	if (localStorage.getItem("file") != null
			&& $.trim(localStorage.getItem("file")) !== "") {
		if( localStorage.getItem("user") != "" && localStorage.getItem("user") != undefined ){
			//set container dimensions to minimum value
			$('#content').width(400);
			$('#content').height(400);
			$('#svgsketch').height(400);
			$('#svgsketch').width(400);
			
			//request complete annotation data
			var json = {
				"action" : "init",
				"file" : localStorage.getItem("file")
			};
			sendMessage(JSON.stringify(json));
			
		} else{
			//If annotation file not selected then ask user to select file first
			$().toastmessage('showErrorToast',
					"Please enter a user name first");
		}
	} else {
		//If annotation file not selected then ask user to select file first
		$().toastmessage('showErrorToast',
				"Please select a file for annotation first");
	}
}

/*
 * WebSocket event handlers
 * Event: open - on connection open
 */
Wicket.Event.subscribe("/websocket/open", function(jqEvent, message) {
	$().toastmessage('showSuccessToast',
			"WebSocket connection successfully opened");
});

// Event: error - on error in connection
Wicket.Event.subscribe("/websocket/error", function(jqEvent, message) {
	$("#reconnectAlert").show();
});

// Event: close - on connection close 
Wicket.Event.subscribe("/websocket/close", function(jqEvent, message) {
	$().toastmessage('showErrorToast', "The connection is closed");
	$("#reconnectAlert").show();
});


//Global variables
var requeststack = []; // for testing - stack of all the request send by server
var activeTester = false; // For testing
var allowNextTest = true; // For testing

// Event: message - on receiving a message
Wicket.Event.subscribe("/websocket/message",
				function(jqEvent, message) {
					// Parse the received message
					var obj = $.parseJSON(message);
					
					if (obj.selectedfile === localStorage.getItem("file")
							&& obj.action !== undefined) {
						//Show mouse pointer location of other users
						if (obj.action == "pos"
								&& obj.user.replace(/\s+/, "") !== localStorage
										.getItem("user").replace(/\s+/, "")) {
							//Get the pointer position on the tree 
							var bbox;
							if ($("#" + obj.element + " > text").length == 1) {
								bbox = $("#" + obj.element + " > text")[0]
										.getBBox();
							} else {
								bbox = $("#" + obj.element)[0].getBBox();
							}
							
							//Show the user name on the selected position
							renderview.showColleague(bbox.y - 10, bbox.x,
									obj.user.replace(/\s+/, ""), obj.show);
						}
					}
					
					// Start : for testing
					// If test setup received
					if (obj.testFile != undefined) {
						localStorage.setItem("file", obj.testFile);
						$('#renderingmode').val(obj.testMode);
						localStorage.setItem("renderingmode", obj.testMode);
						loadtest = obj.testCase.indexOf("loadTest") != -1? true: false;
						loadtestExecute = loadtest? 3 : 1;
						requeststack = [];
						//Get analysis data and initiate test suite execution
						var json = {
							"action" : "init",
							"file" : localStorage.getItem("file"),
							"testingmode" : true,
							"testcase" : obj.testCase,
							"activeTester" : activeTester
						};
						sendMessage(JSON.stringify(json));
					}

					// If the message is for testing
					if (obj.testData !== undefined) {
						//add the received requests to a stack
						requeststack.push(obj);
						// add a delay to get all the test actions from server
						if (allowNextTest) {
							allowNextTest = false;
							setTimeout(function timer() {
								allowNextTest = true;
								// Execute the first action in the stack
								// runTest();
							}, 2000);
						}
					}
					// End : for testing

					// If the message is with list of annotation files from filesystem
					if (obj.files !== undefined) {
						updatefilesList(obj.files);
					}

					// If conflict notifications are received
					if (obj.conflict !== undefined) {
						var conflict_notification = obj.conflict;
						$.each(conflict_notification, function(i, value) {
							if ($('#' + i).text() != "") {
								// If data is empty ask user to provide
								// annotation file
								$().toastmessage(
										'showNoticeToast',
										'Conflicting action "' + value
												+ '" on node "'
												+ $('#' + i).text()
												+ '" was rejected');
							}

						});

					}

					// If there is a notification
					if (obj.notification !== undefined) {
						$().toastmessage('showNoticeToast', obj.notification);
					}

					// If the message is with annotation data
					if (obj.mode !== undefined
							&& obj.selectedfile === localStorage
									.getItem("file")) {
						var sendunixtime = obj.sentTimestamp;
						var processingtime = obj.processingtime;
						var currentdunixtime = new Date().getTime();
						// Assuming time taken for sending request = time taken
						// for receiving request
						// divide total time by 2
						var connectiontime = (currentdunixtime - sendunixtime - processingtime) / 2;
						if (!isNaN(connectiontime)) {
							renderview.latency = connectiontime;
							//Commented : notifications of latency information
							/*var connectionstrength = 'Last message latency was "'	+ connectiontime + '" milliseconds';
							if (connectiontime < 1) {
								connectionstrength = "Last message latency time was less than 1 millisecond";
							}
							$().toastmessage('showNoticeToast',
									connectionstrength);*/
						}

						// Full rendering mode
						if (obj.mode === 0) {
							//Initiate complete rendering of sxntactic tree on baseline protocol response
							renderview.renderCompleteView(obj.text, obj.offset,
									obj.nodes, obj.edges, obj.version);
							
							//Highlight the last updated element
							if (obj.lastUpdate !== undefined
									&& obj.lastUpdate !== "") {
								renderview.showLastupdate(obj.lastUpdate, obj.user);
							}
						}
						// Incremental mode
						else if (obj.mode === 1 && obj.actions !== undefined) {
							
							//Check if version recieved is the expected version
							if (obj.version == (renderview.version + 1)) {
								renderview.version = obj.version;
								// Iterate through all the actions to be
								// iteratively added to the syntactic tree
								renderview.updateIncrementally(obj.actions);
								
								//Highlight the last updated element
								if (obj.lastUpdate !== undefined
										&& obj.lastUpdate !== "") {
									renderview.showLastupdate(obj.lastUpdate, obj.user);
								}
							} else {
								//If version mismatch, request synchronization
								$().toastmessage('showErrorToast',
												"You were out of sync. Performing synchronization");
								requestCompleteData();
							}

						} else {
							// If data is empty ask user to provide annotation file
							$().toastmessage('showErrorToast',
									"Please select an annotation file");
						}

						// Start : for testing
						// at end of server response execute the next action in the stack
						runTest();
						// End : for testing
					}

					obj = null;
				});
