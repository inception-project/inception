/*
 * Thesis: Collaborative Web-based Tool for Annotation of Syntactic Parse Trees
 * @author : Asha Joshi
 * Technische Universität Darmstadt 
 * 
 * This file is mainly responsible for the evaluation of the application enabling the automatic testing feature  
 *
 */


//start auto testing
$('#automatictesting').click(function(e) {
	// get the selected test case
	var selectedTestCase = $("input:radio[name=testcase]:checked").val();
	if ($.trim(selectedTestCase) !== "") {
		activeTester = true;
		json = {
			"action" : "autotest",
			"type" : selectedTestCase
		};
		// request server to start perfoming random test
		sendMessage(JSON.stringify(json));
		$("#testCases_modal").modal('hide');
	} else {
		// If no test case selected.
		$().toastmessage('showErrorToast', "Please select a Test Case");
	}
});

/**
 * Pop one test action from the stack and send respective request to server
 */
var runTest = function() {

	loadtestExecute ++;
	setTimeout(function timer() {
		// Get one test action at a time from stack
		if (requeststack.length > 0) {
			// Send request to server depending on the test action
			if(loadtest){
				if(loadtestExecute > 3){
					var reqData = requeststack.shift();
					loadtestExecute = 1;
					performTest(reqData);
				}
			}else{
				var reqData = requeststack.shift();
				performTest(reqData);
			}
			
		} else {
			// when all tests are performed
			activeTester = false; // reset active user.
		}
	}, 1000);
}

/**
 * Request server depending on the received test action
 */
var performTest = function(obj) {
	var action = obj.action;
	var testdata = obj.testData;
	var nodes = testdata.split("_");
	var parent = "";
	var child = "";
	if (nodes.length == 2) {
		var parent = nodes[0];
		var child = nodes[1];
	}
	
	//Execute test editing requests
	switch (action) {
	case "testAddNode":
		var childID = $("text[class=" + child + "]").attr('id');
		if (childID == undefined) {
			childID = $("text[class=" + child + "]").closest('g').attr('id');
		}
		if ($.trim(childID) !== "") {
			$().toastmessage('showSuccessToast', "Add node requested");
			json = {
				"action" : "addNode",
				"mode" : $('#renderingmode').val(),
				"childnode" : childID,
				"label" : parent,
				"file" : localStorage.getItem("file"),
				"sendUnixTime" : new Date().getTime(),
				"version" : renderview.version,
				"latency" : renderview.latency,
				"user": localStorage.getItem("user")
			};
			sendMessage(JSON.stringify(json));
		}
		break;
	case "testAddEdge":
		var parentID = $("text[class=" + parent + "]").closest('g').attr('id');
		var childID = $("text[class=" + child + "]").closest('g').attr('id');
		if ($.trim(parentID) !== "" && $.trim(childID) != "") {
			$().toastmessage('showSuccessToast', "Add Edge requested");
			// Send addEdge request to server
			json = {
				"action" : "addEdge",
				"mode" : $('#renderingmode').val(),
				"parentid" : parentID,
				"childid" : childID,
				"file" : localStorage.getItem("file"),
				"sendUnixTime" : new Date().getTime(),
				"version" : renderview.version,
				"latency" : renderview.latency,
				"user": localStorage.getItem("user")
			};
			sendMessage(JSON.stringify(json));
		}
		break;
	case "testDeleteNode":
		var nodeID = $("text[class=" + testdata + "]").closest('g').attr('id');
		if ($.trim(nodeID) !== "" && $.trim(nodeID) != "") {
			$().toastmessage('showSuccessToast', "Delete node requested");
			// delete node request
			json = {
				"action" : "deleteNode",
				"mode" : $('#renderingmode').val(),
				"nodeid" : nodeID,
				"file" : localStorage.getItem("file"),
				"sendUnixTime" : new Date().getTime(),
				"version" : renderview.version,
				"latency" : renderview.latency,
				"user": localStorage.getItem("user")
			};
			sendMessage(JSON.stringify(json));
		}
		break;
	case "testDeleteEdge":
		var parentID = $("text[class=" + parent + "]").closest('g').attr('id');
		var childID = $("text[class=" + child + "]").closest('g').attr('id');
		if ($.trim(parentID) !== "" && $.trim(childID) != "") {
			$().toastmessage('showSuccessToast', "Add Edge requested");
			// Send addEdge request to server
			json = {
				"action" : "deleteEdge",
				"mode" : $('#renderingmode').val(),
				"edgeid" : 'e_' + parentID + '_' + childID,
				"file" : localStorage.getItem("file"),
				"sendUnixTime" : new Date().getTime(),
				"version" : renderview.version,
				"latency" : renderview.latency,
				"user": localStorage.getItem("user")
			};
			sendMessage(JSON.stringify(json));
		}
		break;
	case "testEditNode":
		var nodeID = $("text[class=" + child + "]").closest('g').attr('id');
		if ($.trim(nodeID) != "") {
			$().toastmessage('showSuccessToast', "Edit node requested");
			json = {
				"action" : "editNode",
				"mode" : $('#renderingmode').val(),
				"nodeid" : nodeID,
				"newlabel" : parent,
				"file" : localStorage.getItem("file"),
				"sendUnixTime" : new Date().getTime(),
				"version" : renderview.version,
				"latency" : renderview.latency,
				"user": localStorage.getItem("user")
			};
			sendMessage(JSON.stringify(json));
		}
		break;
	case "testEditEdge":
		var parentID = $("text[class=" + parent + "]").closest('g').attr('id');
		var childID = $("text[class=" + child + "]").closest('g').attr('id');
		if ($.trim(parentID) !== "" && $.trim(childID) != "") {
			$().toastmessage('showSuccessToast', "Edit Edge requested");
			json = {
				"action" : "editEdge",
				"mode" : $('#renderingmode').val(),
				"nodeid" : 'e_' + parentID + '_' + childID,
				"edgelabel" : "test",
				"file" : localStorage.getItem("file"),
				"sendUnixTime" : new Date().getTime(),
				"version" : renderview.version,
				"latency" : renderview.latency,
				"user": localStorage.getItem("user")
			};
			sendMessage(JSON.stringify(json));
		}
		break;
	case "testaddNodeBW":
		var parentID = $("text[class=" + parent + "]").closest('g').attr('id');
		var childID = $("text[class=" + child + "]").closest('g').attr('id');
		if ($.trim(parentID) !== "" && $.trim(childID) != "") {
			$().toastmessage('showSuccessToast',
					"Add Node in between requested");
			json = {
				"action" : "addNodeBW",
				"mode" : $('#renderingmode').val(),
				"nodelabel" : "Test",
				"edgeid" : 'e_' + parentID + '_' + childID,
				"file" : localStorage.getItem("file"),
				"sendUnixTime" : new Date().getTime(),
				"version" : renderview.version,
				"latency" : renderview.latency,
				"user": localStorage.getItem("user")
			};
			sendMessage(JSON.stringify(json));
		}
		break;
	case "testResult":
		//Check test results
		if (testdata == "randomTestBaseline"
				|| testdata == "randomTestIncremental") {
			if ($("text[class=DET]").closest('g').length == 1
					&& $("text[class=VERB]").closest('g').length == 1
					&& $("text[class=Test]").closest('g').length == 1
					&& $("text[class=PP]").closest('g').length == 0
					&& $("text[class=S").closest('g').length == 0) {
				$().toastmessage('showSuccessToast', 'Test success');
			} else {
				$().toastmessage('showNoticeToast', 'Test failed');
			}
		} else if (testdata == "concurrentAccessBaseline") {
			if ($("text[class=P44]").closest('g').length == 1
					&& $("text[class=P5]").closest('g').length == 1
					&& ($("text[class=NOUN]").closest('g').length == 1 || $("text[class=PRON]").closest('g').length == 1) 
					&& $("text[class=SS]").closest('g').length == 1
					&& $("text[class=P44").closest('g').length == 1
					&& $("text[class=P3").closest('g').length == 1) {
				
				$().toastmessage('showSuccessToast', 'Test success');
			} else {
				$().toastmessage('showNoticeToast', 'Test failed');
			}
		} else if (testdata == "loadTestBaseline") {
			if ($("text[class=S1]").closest('g').length == 1
					&& $("text[class=NP]").closest('g').length == 1
					&& $("text[class=S2]").closest('g').length == 1
					&& $("text[class=WHADVP]").closest('g').length == 1
					&& $("text[class=SBAR]").closest('g').length == 1
					&& $("text[class=NP2]").closest('g').length == 1
					&& $("text[class=NP3]").closest('g').length == 1
					&& $("text[class=S]").closest('g').length == 1) {
				$().toastmessage('showSuccessToast', 'Test success');
			} else {
				$().toastmessage('showNoticeToast', 'Test failed');
			}
		}
	}
}
// End : for testing
