/*
 * Thesis: Collaborative Web-based Tool for Annotation of Syntactic Parse Trees
 * @author : Asha Joshi
 * Technische Universität Darmstadt 
 * 
 * This file is mainly responsible for initiating the rendering process and the handling the various possible inputs on the web page  
 *
 */


//Show the sentences dynamically corresponding to the selected files
var updatefilesList = function (filelist) {
	$('#file_select').empty();
	var filedata = $.parseJSON(filelist);
	$().toastmessage('showSuccessToast', "Please select a document or create new");
	$('#file_select').find('option').remove().end().append($('<option value=""></option>'));
	$.each(filedata, function(filename, sentence_data) {
		var filemodifiedname = filename.toLowerCase().replace(/\./g, '');
		$('#file_select').append($("<option></option>").attr("value", filemodifiedname).text(filename));
		var html = '<div class="sentence_options" id="'+filemodifiedname+'">';
		$.each(sentence_data, function(sentence_addr, sentence_text) {
			var optionval = filename +"#"+sentence_addr;
			html += '<div class="radio"> <label><input type="radio" name="selecteddocument" value="'+optionval+'">'+sentence_text+'</label></div>';					
		});
		html +="</div>";
		$("#file_modal .modal-body").append(html);
		$("#"+filemodifiedname).hide();
	});
	
	//by default select the previously selected sentence, if any
	if(localStorage.getItem("file") != undefined){
		var currentfile = localStorage.getItem("file");
		var filedetails = currentfile.split("#");
		if(filedetails.length == 2){
			var filemodifiedname = filedetails[0].toLowerCase().replace(/\./g, '');
			$('#file_select').val(filemodifiedname);
			$('#file_select').data('val', filemodifiedname);
			$("#"+filemodifiedname).show();
			$('input[name=selecteddocument][value="'+currentfile+'"]').prop("checked",true);
		}
	}
}
		
var Annotation = function() {
	//called from the AnnotationPage.html on document ready
	var init = function() {
		
		//Set the default scale of svg to 1
		localStorage.setItem("scale", 1.0);
		
		//Initial height of container div (minimum)
		$('#svgsketch').height(400);
		
		//Show/hide history side bar
		$("#menu-toggle").click(function(e) {
	        e.preventDefault();
	        $("#wrapper").toggleClass("toggled");
	    });
		
		//get the user name from local storage and set it on the web page
		if (localStorage.getItem("user") !== undefined || localStorage.getItem("user") !== "" ) {
			$('#username').val(localStorage.getItem("user"));
		}
		
		//On change event of user name input, update local storage
		$('#username').change( function (){
			localStorage.setItem("user",$(this).val());
		});
		
		//minimize window button click handler
		$('#halfwindow').click(function() {
			if (localStorage.getItem("file") != null
					&& $.trim(localStorage.getItem("file")) !== "") {
				//If tree height more than max allowed value, set max height
				if(parseInt($('#svgsketch').height()) > treeContainerMaxHeight ){
					$('#content').height(treeContainerMaxHeight);
				}else{
				//set the height of tree
					$('#content').height(parseInt($('#svgsketch').height()) + 50);
				}
				
				//If tree width more than max allowed value, set max width
				if(parseInt($('#svgsketch').width()) > treeContainerMaxWidth ){
					$('#content').width(treeContainerMaxWidth);
				}else{
					//set the width of tree
					$('#content').width(parseInt($('#svgsketch').width()) + 50);
				}
				
				//scroll to bottom of the div
				$('#content').scrollTop($('#content')[0].scrollHeight);
			} else {
				//If annotation file not selected then ask user to select file first
				$().toastmessage('showErrorToast',
						"Please select a file for annotation first");
			}
		});
		
		//maximize window button click handler
		$('#fullwindow').click(function() {
			if (localStorage.getItem("file") != null
					&& $.trim(localStorage.getItem("file")) !== "") {
				//set the height width of container with the height width of svg plus 50px
				$('#content').height(parseInt($('#svgsketch').height()) + 50);
				$('#content').width(parseInt($('#svgsketch').width()) + 50);
				$('#content').scrollTop($('#content')[0].scrollHeight);
			} else {
				//If annotation file not selected then ask user to select file first
				$().toastmessage('showErrorToast',
					"Please select a file for annotation first");
			}
		});
		
		//default scale button click handler
		$('#default').click(function (){
			if (localStorage.getItem("file") != null
					&& $.trim(localStorage.getItem("file")) !== "") {
				//set the svg scale to 1 and also update local storage
				localStorage.setItem("scale", 1.0);
				$('g.maincontainer').attr("transform", "scale(1.0)");
			} else {
				//If annotation file not selected then ask user to select file first
				$().toastmessage('showErrorToast',
						"Please select a file for annotation first");
			}
		});
		
		
		//zoom out button click handler
		$('#zoomout').click(function(){
			if (localStorage.getItem("file") != null
					&& $.trim(localStorage.getItem("file")) !== "") {
				//get the current scale from local storage and reduce its value
				var currentscale = localStorage.getItem("scale");
				currentscale = parseFloat(currentscale) - 0.2;
				localStorage.setItem("scale", currentscale);
				$('g.maincontainer').attr("transform", "scale("+currentscale+")");
			} else {
				//If annotation file not selected then ask user to select file first
				$().toastmessage('showErrorToast',
						"Please select a file for annotation first");
			}
		});
		
		// populate the annotation types
		var nodeTypes = {
			"ADJ" : "ADJ - Adjective",
			"ADP" : "ADP - Adposition",
			"ADV" : "ADV - Adverb",
			"ART" : "ART - Determiners and articles",
			"CONJ" : "CONJ - Conjunction",
			"DT" : "DT - Determiner",
			"DET" : "DET - Determiner",
			"IN" : "IN - Preposition or subordinating conjunction",
			"JJ" : "JJ- adjective",
			"NN" : "NN - Noun",
			"NOUN" : "NOUN - Noun",
			"NUM" : "NUM -Numeral",
			"NP" : "NP - Noun phrase",
			"PP" : "PP - Prepositions and postpositions",
			"PROPN" : "PROPN - Proper noun",
			"PRON" : "PRON - Pronoun",
			"PRP" : "PRP - Pronoun",
			"PRP$" : "PRP$ - possessive personal pronoun",
			"PUNCT" : "PUNCT - Punctuation",
			"RB" : "RB - Adverb",
			"SYM" : "SYM - Symbol",
			"S" : "S - Sentence",
			"VERB" : "VERB - Verb",
			"VB" : "VB - Verb, base form",
			"VBD" : "VBD - Verb, past tense",
			"VBZ" : "VBZ - Verb, 3rd. singular present",
			"VP" :  "VP - VERB",
			"." : ". - Period",
			"X" : "X - Other"
		};
		
		//Iterate through nodeTypes json and set options 
		for ( var key in nodeTypes) {
			//For POS node adding dialogue  
			$('#pos_text').append(
					'<option value="' + key + '">' + nodeTypes[key]
							+ '</option>');
			//For node editing dialogue
			$('#node_new_type').append(
					'<option value="' + key + '">' + nodeTypes[key]
							+ '</option>');
			
			//For edge editing dialogue
			$('#nodeInBW_type').append(
					'<option value="' + key + '">' + nodeTypes[key]
							+ '</option>');
		}

		// Get the existing annotation files when modal opens
		$('#file_modal').on('shown.bs.modal', function() {
			var json = '{"action":"getAnnotationFiles"}';
			sendMessage(json);
		});

		// reset the select with annotation option when ADD POS modal open
		$('#addPOSmodal').on('shown.bs.modal', function() {
			$('#pos_text', this).chosen('destroy').chosen();
		});

		// reset the select with annotation option when ADD POS modal open
		$('#nodeActions').on(
				'shown.bs.modal',
				function() {
					$('#node_new_type', this).chosen('destroy').chosen();
					var selecttag = $(
							'input[type=radio][name=node_action]:checked')
							.val();
					if (selecttag === '1' || selecttag === '3') {
						$('#node_new_type_chosen').show();
					} else {
						$('#node_new_type_chosen').hide();
					}
				});
		
		// reset actions when Edge actions modal open
		$('#edgeActions').on(
				'shown.bs.modal',
				function() {
					$('#nodeInBW_type', this).chosen('destroy').chosen();
					var selecttag = $(
							'input[type=radio][name=edge_action]:checked')
							.val();
					if (selecttag === '1') {
						$('#edge_text').show();
						$('#nodeInBW_type_chosen').hide();
					} else if(selecttag === '3'){
						$('#edge_text').hide();
						$('#nodeInBW_type_chosen').show();
					} else {
						$('#edge_text').hide();
						$('#nodeInBW_type_chosen').hide();
					}
				});

		// show/hide node types options as per selections
		$('input[type=radio][name=node_action]').change(function() {
			if (this.value !== '2') {
				$('#node_new_type_chosen').show();
			} else {
				$('#node_new_type_chosen').hide();
			}
		});
		
		//Handle edge actions
		$('input[type=radio][name=edge_action]').change(function() {
			if (this.value === '1') {
				$('#edge_text').show();
				$('#nodeInBW_type_chosen').hide();
			} else if(this.value === '3'){
				$('#edge_text').hide();
				$('#nodeInBW_type_chosen').show();
			} else {
				$('#edge_text').hide();
				$('#nodeInBW_type_chosen').hide();
			}
		});
		
		//0- Full more
		//1- Iterative mode
		//set the mode selected as per the value in local storage (by default "0") 
		if (localStorage.getItem("renderingmode") !== undefined) {
			if (localStorage.getItem("renderingmode") == "0") {
				$('#renderingmode').val("0");
			} else if (localStorage.getItem("renderingmode") == "1") {
				$('#renderingmode').val("1");
			}
		} else {
			//Rendering mode initially set to Baseline- 0( Full mode )
			localStorage.setItem("renderingmode", "0");
			$('#renderingmode').val("0");
		}
		
		//On changing rendering mode update local storage
		$('#renderingmode').change(function() {
			localStorage.setItem("renderingmode", $(this).val());
		});

		//Update the list of annotation files when user request list of files from server 
		$('#file_modal').on('shown.bs.modal',function() {
			$(".sentence_options").remove();
		});
		
		//update the local storage when different annotation file is selected
		$('#file_select').change(function(e) {
			var oldval = $(this).data('val');
			$("#"+oldval).hide();
			$(this).data('val', $(this).val());
			$("#"+this.value).show();
		});
		
		//select file
		$("#submitfile").click( function(){
			if($("input[type='radio'][name='selecteddocument']").is(":checked")){
				localStorage.setItem("file", $("input[type='radio'][name='selecteddocument']:checked").val());
				$().toastmessage('showSuccessToast', "Document select successfully.");
				$('#file_modal').modal('hide');  
			}else{
				$().toastmessage('showErrorToast',
				"Please select one sentence");
			}
			
		});
		
		//New corpus create button handler
		$('#newCorpusForm').submit( function (e){
			e.preventDefault();
			
			//send request for creating corpus with entered text for annotation 
			var json = {
					"action" : "createNew",
					"file" : $("#newfilename").val(),
					"text" : $("#corpustext").val()
				};
			$('#createCorpus').modal('hide'); 
			sendMessage(JSON.stringify(json));
		});
		
		//Start button clicked: request annotation data for the selected file to server
		$('#startannotation').click(function() {
					requestCompleteData();
		});
		
		/**
		 * Object RenderView function
		 * render complete syntactic tree
		 * Parameter: data received from server
		 */
		RenderView.prototype.renderCompleteView = function(datatext,
				dataoffset, datanodes, dataedges, versionNR) {
			start = null; //dragging
			$('#svgsketch').svg('destroy');
			$('#svgid').remove();
			var self = this;
			//create svg on the container div
			$('#svgsketch').svg(
					{	
						//on successful creation of svg element
						onLoad : function(svg) {
							sketchpad = svg;
							// set id of svg
							svg._svg.setAttribute('id', 'svgid');
							//set initial height of svg
							$('#svgid').height(
									parseInt($('#svgsketch').height()));
							
							//create the maingroup 'maincontainer' which will contain all the elements of svg syntactic tree
							self.maingroup = svg.group({
								'class' : 'maincontainer'
							});
							self.sketchpad = svg;
							self.maingroup.setAttribute("transform", "scale("+localStorage.getItem("scale")+")");
							
							//create a rect on svg covering whole svg height and width of enabling drag event
							var surface = svg.rect(self.maingroup, 0, 0,
									'100%', '100%', {
										id : 'surface',
										fill : 'white'
									});
							$('#surface').height(
									parseInt($('#svgsketch').height()));
							$(surface).mousemove(dragging);
							
							//create the arrow pointer for edges
							var definition = svg.defs(self.maingroup);
							var marker = svg.marker(definition, 'markerArrow',
									2, 6, 13, 13, "auto");
							var path = svg.createPath();
							svg.path(marker, path.move(2, 6).line(
									[ [ 10, 0 ], [ 10, 12 ], [ 2, 6 ] ])
									.close(), {
								fill : '#000000',
								stroke : 'black'
							});
							
							//If the annotation data is valid
							if (datatext !== "") {
								self.text = datatext;
								self.version = versionNR;
								self.offset = eval(dataoffset);
								self.nodes = eval(datanodes);
								self.edges = eval(dataedges);
								//create SyntacticTree object 
								self.treeobj = new SyntacticTree(sketchpad,
										self.text, self.offset, self.nodes,
										self.edges, self.maingroup);
								//create SyntacticTree text span
								self.treeobj.createSpanLayer();
								//create SyntacticTree POS nodes								
								self.treeobj.createPOSLayer();
								//create SyntacticTree constituent nodes and edges								
								self.treeobj.createConstituentLayer();
							}
							if(localStorage.getItem("scale") == 1){
								$('#content').scrollTop(
										$('#content')[0].scrollHeight);
							}
						}
					});

		};
		
		/**
		 * Object RenderView function
		 * re-render complete syntactic tree
		 * Parameter: previously saved analysis data
		 */
		RenderView.prototype.reRenderView = function() {
			start = null; //dragging
			$('#svgsketch').svg('destroy');
			$('#svgid').remove();
			var self = this;
			$('#svgsketch').svg(
					{
						onLoad : function(svg) {
							sketchpad = svg;
							// set id of svg
							svg._svg.setAttribute('id', 'svgid');
							$('#svgid').height(
									parseInt($('#svgsketch').height()));
							
							//create the maingroup 'maincontainer' which will contain all the elements of svg syntactic tree
							self.maingroup = svg.group({
								'class' : 'maincontainer'
							});
							self.maingroup.setAttribute("transform", "scale("+localStorage.getItem("scale")+")");
							self.sketchpad = svg;
							//create a rect on svg covering whole svg height and width of enabling drag event
							var surface = svg.rect(self.maingroup, 0, 0,
									'100%', '100%', {
										id : 'surface',
										fill : 'white'
									});
							$('#surface').height(
									parseInt($('#svgsketch').height()));
							$(surface).mousemove(dragging);
							
							//create the arrow pointer for edges
							var definition = svg.defs(self.maingroup);
							var marker = svg.marker(definition, 'markerArrow',
									2, 6, 13, 13, "auto");
							var path = svg.createPath();
							svg.path(marker, path.move(2, 6).line(
									[ [ 10, 0 ], [ 10, 12 ], [ 2, 6 ] ])
									.close(), {
								fill : '#000000',
								stroke : 'black'
							});
							//create SyntacticTree object 							
							self.treeobj = new SyntacticTree(sketchpad,
									self.text, self.offset, self.nodes,
									self.edges, self.maingroup);
							//create SyntacticTree text span 
							self.treeobj.createSpanLayer();
							//create SyntacticTree POS nodes
							self.treeobj.createPOSLayer();
							//create SyntacticTree constituent nodes and edges
							self.treeobj.createConstituentLayer();
							if(localStorage.getItem("scale") == 1){
								$('#content').scrollTop(
										$('#content')[0].scrollHeight);
							}
							
						}
					});

		};
		
		//Node action modal on chnage of radio button value
		$('#node_select').change(function() {
			if ($(this).val() === '0' || $(this).val() === '2') {
				$('#node_text_label').css('display', 'none');
				$('#node_text').css('display', 'none');
				$('#parentnode_text_label').css('display', 'none');
				$('#parentnode_text').css('display', 'none');
			} else if ($(this).val() === '1') {
				$('#node_text_label').css('display', '');
				$('#node_text').css('display', '');
				$('#parentnode_text_label').css('display', 'none');
				$('#parentnode_text').css('display', 'none');
			} else if ($(this).val() === '3') {
				$('#node_text_label').css('display', 'none');
				$('#node_text').css('display', 'none');
				$('#parentnode_text_label').css('display', '');
				$('#parentnode_text').css('display', '');
			}
		});
		
		//Add POS modal 'save' button click
		$('#addPOS').click(function() {
			var childnode = $('#sentence_id').val();
			//request add node 
			//child node: text span ID
			var json = {
				"action" : "addNode",
				"mode" : $('#renderingmode').val(),
				"childnode" : childnode,
				"label" : $('#pos_text').val(),
				"file" : localStorage.getItem("file"),
				"sendUnixTime" : new Date().getTime(),
				"version": renderview.version,
				"latency": renderview.latency,
				"user": localStorage.getItem("user")
			};
			$().toastmessage('showSuccessToast', "Add Node requested");
			sendMessage(JSON.stringify(json));
		});
		
		//Node action Modal 'save' button click
		$('#submitnodeaction').click(
				function() {
					var id = $('#node_id').val();
					var selecttag = $(
							'input[type=radio][name=node_action]:checked')
							.val();
					var json = "";
					if (typeof selecttag !== undefined) {
						//If Edit node selected request node new type
						if (selecttag === '1') {
							$().toastmessage('showSuccessToast',
									"Edit node requested");
							json = {
								"action" : "editNode",
								"mode" : $('#renderingmode').val(),
								"nodeid" : id,
								"newlabel" : $('#node_new_type').val(),
								"file" : localStorage.getItem("file"),
								"sendUnixTime" : new Date().getTime(),
								"version": renderview.version,
								"latency": renderview.latency,
								"user": localStorage.getItem("user")
							};
							sendMessage(JSON.stringify(json));
						} else if (selecttag === '3') {
							//If Add node selected request adding constituent node
							$().toastmessage('showSuccessToast',
									"Add node requested");
							json = {
								"action" : "addNode",
								"mode" : $('#renderingmode').val(),
								"childnode" : id,
								"label" : $('#node_new_type').val(),
								"file" : localStorage.getItem("file"),
								"sendUnixTime" : new Date().getTime(),
								"version": renderview.version,
								"latency": renderview.latency,
								"user": localStorage.getItem("user")
							};
							sendMessage(JSON.stringify(json));
						} else if (selecttag === '2') {
							//If Delete node selected request 
							$().toastmessage('showSuccessToast',
									"Delete node requested");
							// delete node request
							json = {
								"action" : "deleteNode",
								"mode" : $('#renderingmode').val(),
								"nodeid" : id,
								"file" : localStorage.getItem("file"),
								"sendUnixTime" : new Date().getTime(),
								"version": renderview.version,
								"latency": renderview.latency,
								"user": localStorage.getItem("user")
							};
							sendMessage(JSON.stringify(json));
						}
					} else {
						//If no option was selected
						$().toastmessage('showErrorToast',
						"Please select required option in radiobutton");
					}
				});
		
		//On edge action modal 'save' is clicked
		$('#submitEdgeaction').click(
				function() {
					var id = $('#edge_id').val();
					var json = "";
					var selecttag = $(
							'input[type=radio][name=edge_action]:checked')
							.val();
					if (typeof selecttag !== undefined) {
						//If Edit edge selected
						if (selecttag === '1') {
							/*if($('#edge_text').val() == ""){
								$().toastmessage('showErrorToast',
								"Edge label cannot be empty");
							}else{*/
								$().toastmessage('showSuccessToast',
								"Edit Edge requested");
								json = {
									"action" : "editEdge",
									"mode" : $('#renderingmode').val(),
									"nodeid" : id,
									"edgelabel" : $('#edge_text').val(),
									"file" : localStorage.getItem("file"),
									"sendUnixTime" : new Date().getTime(),
									"version": renderview.version,
									"latency": renderview.latency,
									"user": localStorage.getItem("user")
								};
								sendMessage(JSON.stringify(json));
							//}
						} else if (selecttag === '2') {
							//If Delete Edge selected
							$().toastmessage('showSuccessToast',
									"Delete Edge requested");
							json = {
								"action" : "deleteEdge",
								"mode" : $('#renderingmode').val(),
								"edgeid" : id,
								"file" : localStorage.getItem("file"),
								"sendUnixTime" : new Date().getTime(),
								"version": renderview.version,
								"latency": renderview.latency,
								"user": localStorage.getItem("user")
							};
							sendMessage(JSON.stringify(json));
						} else if (selecttag === '3') {
							//If Delete Edge selected
							$().toastmessage('showSuccessToast',
									"Add Node in between requested");
							json = {
								"action" : "addNodeBW",
								"mode" : $('#renderingmode').val(),
								"nodelabel" : $('#nodeInBW_type').val(),
								"edgeid" : id,
								"file" : localStorage.getItem("file"),
								"sendUnixTime" : new Date().getTime(),
								"version": renderview.version,
								"latency": renderview.latency,
								"user": localStorage.getItem("user")
							};
							sendMessage(JSON.stringify(json));
						}
					} else {
						//If no option was selected
						$().toastmessage('showWarningToast',
						"Please select required option in radiobutton");
					}
				});
		
		/**
		 * Object RenderView function
		 * Highlight the last update on the tree
		 * @param lastUpdate
		 */
		RenderView.prototype.showLastupdate =  function(lastUpdate, user){
			if($("#"+lastUpdate).length == 1){
				var updateMessage = localStorage.getItem("user") == user? "You" : user;
				updateMessage += " edited tree element : " + $("#"+lastUpdate).text();
				$("ul.sidebar-nav").append('<li><a href="#">' + updateMessage + '</a></li>');
				$($("#"+lastUpdate), self.sketchpad.root()).addClass('highlight');
				var animation = {svgFill: "cornflowerblue"};
				$("#"+lastUpdate).find("rect").attr("stroke-width", "5");
				$("#"+lastUpdate).find("path").attr("stroke-width", "1.5");
				$("#"+lastUpdate).find("text").attr("font-weight", "600");
				$("#"+lastUpdate).animate(animation, 10000, 'easeInBounce', function() {
					$("#"+lastUpdate).find("rect").attr("stroke-width", "0.5");
					$("#"+lastUpdate).removeAttr("fill-opacity");
					$("#"+lastUpdate).find("text").removeAttr("font-weight");
					$("#"+lastUpdate).find("path").attr("stroke-width", "1");
					$($("#"+lastUpdate), self.sketchpad.root()).removeClass('highlight');
		        });
			}
		};
		
		
		
		/**
		 * Object RenderView function
		 * Add the node iteratively to the tree
		 * @param value
		 */
		RenderView.prototype.showColleague = function(top, left, user, display) {
			if(display){
				if($('.user_' + user).length !== 1){
					var colleague = this.sketchpad.text(this.maingroup, left, top, user, {fill: 'maroon', fontSize: '12'});
					colleague.setAttribute('class', 'user_' + user);
				}
			}else{
				$('.user_' + user).remove();
			}
		}
		
		
		/**
		 * Object RenderView function
		 * perform actions iteratively on the tree
		 * @param value
		 */
		RenderView.prototype.updateIncrementally = function(actions) {
			start = null;	//dragging
			$.each(actions, function(action, value) {
				switch (action) {
				case "3_addNode":
					renderview.createNodeIterative(value);
					break;
				case "4_addEdge":
					renderview.createEdgeIterative(value);
					break;
				case "2_deleteNode":
					renderview.deleteNodeIterative(value);
					break;
				case "1_deleteEdge":
					renderview.deleteEdgeIterative(value);
					break;
				case "updateNode":
					renderview.updateNodeIterative(value);
					break;
				case "updateEdge":
					renderview.updateEdgeIterative(value);
					break;
				}
			});
		}
		
		
		
		/**
		 * Object RenderView function
		 * Add the node iteratively to the tree
		 * @param value
		 */
		RenderView.prototype.createNodeIterative = function(value) {
			var nodedata = $.parseJSON(value);
			//If the node is POS node
			if ((nodedata.offset[0] === 0 && nodedata.offset[1] !== 0)
					|| (nodedata.offset[0] !== 0 && nodedata.offset[1] !== 0)) {
				this.treeobj.nodeIterativePOSAdd(nodedata);
			} else {
				//If node is constituent node
				this.treeobj.nodeIterativeAdd(nodedata);
			}
		}
		
		/**
		 * Object RenderView function
		 * Delete the node iteratively from the tree
		 * @param value
		 */
		RenderView.prototype.deleteNodeIterative = function(value) {
			var self = this;
			//If more than one node to be deleted
			if ($.isArray(value) && value.length > 0) {
				//Iterate over all the nodes and delete sequentially 
				$.each(value,
								function(i, node) {
									var index = idx_id(self.treeobj.allnodes,
											node);
									var textoffset = self.treeobj.allnodes[index].offset;
									//If the node is POS node
									if ((textoffset[0] === 0 && textoffset[1] !== 0)
											|| (textoffset[0] !== 0 && textoffset[1] !== 0)) {
										if ($('#s_' + textoffset[0] + '_'+ textoffset[1]).length) {
											//remove class hasnode from child text span
											$($('#s_' + textoffset[0] + '_'	+ textoffset[1]), self.treeobj.sketchpad.root()).removeClass('hasnode');
											index = idx_id(self.treeobj.textspans,'s_' + textoffset[0] + '_'+ textoffset[1]);											
											if (index != -1) {
												//update the text span hasnode property in textspans array of tree
												self.treeobj.textspans[index].hasnode = false;
											}
										}
									}
									//Delete the node
									self.treeobj.nodeIterativeDelete(node);
								});
			} else if(!$.isArray(value)){
				//If only one node deleted
				var index = idx_id(self.treeobj.allnodes, value);
				var textoffset = self.treeobj.allnodes[index].offset;
				//Check if node is POS
				if ((textoffset[0] === 0 && textoffset[1] !== 0)
						|| (textoffset[0] !== 0 && textoffset[1] !== 0)) {
					if ($('#s_' + textoffset[0] + '_' + textoffset[1]).length) {
						//remove class hasnode from child text span
						$($('#s_' + textoffset[0] + '_' + textoffset[1]),self.treeobj.sketchpad.root()).removeClass('hasnode');
						index = idx_id(self.treeobj.textspans, 's_'+ textoffset[0] + '_' + textoffset[1]);
						if (index != -1) {
							//update the text span hasnode property in textspans array of tree
							self.treeobj.textspans[index].hasnode = false;
						}
					}
				}
				//Delete the node
				self.treeobj.nodeIterativeDelete(value);
			}

		}
		
		/**
		 * Object RenderView function
		 * add edge iteratively to the tree
		 * @param value
		 */
		RenderView.prototype.createEdgeIterative = function(value) {
			var self = this;
			//If more than one edge added
			if ($.isArray(value) && value.length > 0) {
				$.each(value, function(i, edge) {
					//add all the edges to the tree
					self.treeobj.edgeIterativeAdd(edge);
				});
			} else if(!$.isArray(value)){
				//If one edge added
				//add the edge to the tree
				self.treeobj.edgeIterativeAdd($.parseJSON(value));
			}
		}
		
		/**
		 * Object RenderView function
		 * Delete edge iteratively from the tree
		 * @param value
		 */
		RenderView.prototype.deleteEdgeIterative = function(value) {
			var self = this;
			//If more than one edge deleted
			//Iterate over the array and delete all edges
			if ($.isArray(value) && value.length > 0) {
				$.each(value, function(i, edge) {
					self.treeobj.edgeIterativeDelete(edge);
				});
			} else if(!$.isArray(value)){
				//If only one edge deleted
				self.treeobj.edgeIterativeDelete(value);
			}
		}
		
		/**
		 * Object RenderView function
		 * Update node text iteratively
		 * @param value
		 */
		RenderView.prototype.updateNodeIterative = function(value) {
			var nodedata = $.parseJSON(value);
			this.treeobj.nodeIterativeUpdate(nodedata);
		}
		
		/**
		 * Object RenderView function
		 * Update all edges text of the parent node
		 * @param value
		 */
		RenderView.prototype.updateEdgeIterative = function(value) {
			var edgedata = $.parseJSON(value);
			this.treeobj.edgeIterativeUpdate(edgedata);
		}
		
	};

	return {
		// main function to initiate the module
		init : function() {
			init();
		}
	};
}();