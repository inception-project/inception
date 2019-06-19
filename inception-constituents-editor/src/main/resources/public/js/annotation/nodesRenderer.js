/* 
 * Thesis: Collaborative Web-based Tool for the Annotation of Syntactic Parse Trees
 * @author : Asha Joshi
 * Technische Universität Darmstadt 
 * This script contains methods for visualization and handling of POS nodes of tree
 * It also contains the generic implementation of Nodes ( applicable for both POS and Constituent Nodes) 
 */

//Global variables
var posgroup;			//the group containing POS nodes 
var tmpnode ;			//node selected on click/drag/drop events
var start = null; 		//For Drag/Drop events 
var outline = null; 	//For Drag/Drop events
var dragoffset = null; 	//For Drag/Drop events
var startnode = null;	//For Drag/Drop events
var nodesmargin = 80;	//Vertical margin between nodes of 1 level differnce
var marginContainerAndTree_vert = 50;	//Margin between the tree container and tree elements

/**
 * Get of index of the given offset from array of offsets
 */ 
var idx = function(array, val){
    var index = 0;
    $.each(array, function( i, value ) {
        if(value[0] === val[0] && value[1] === val[1] ){
            index = i; return false;
        }else{
            index = -1;
        }
    });
    return index;
};
   
/**
 * Get the width of text
 * @param text	 		the text for which width is to be calculated
 * @param fontsize	 	the font size of text
 * @returns				the width of text in px	
 */ 
function getTextWidth(text, fontsize) {
 //create a canvas to measure width of text
  var a = document.createElement('canvas');
  var b = a.getContext('2d');
  b.font = fontsize + 'px ' + 'serif';
  return b.measureText(text).width;
} 

/**
 * Object SyntacticTree method
 * Create all POS nodes ( Layer 0 )  
 */
SyntacticTree.prototype.createPOSLayer = function (){
	//create group 'pos' which will contain all the pos nodes
	//add the group 'pos' to main group
    posgroup = this.sketchpad.group(this.maingroup,{'class': 'pos' });
    var self = this;
    sketchpad = this.sketchpad;
    //iterate over all nodes and get the nodes with offsets ( POS nodes )
    $.each(this.nodesdata, function( i, value ) {
        if(( value.offset[0] === 0 && value.offset[1] !== 0) || ( value.offset[0] !== 0 && value.offset[1] !== 0)){
        var index = idx(self.offsetdata, value.offset);
            if(index != -1){
            	//add class 'hasnode' to the text span corresponding to the offset 
                self.textspans[index]['hasnode'] = true;
                var textnode = $('#s_' +value.offset[0] +'_'+ value.offset[1])[0];
                $(textnode, sketchpad.root()).addClass('hasnode');                
                //create POS node at level 0
                tmpnode = new Node(value.text, value.offset, value.id, true);
                var midpt = self.textspans[index].x + self.textspans[index].width/2;
                tmpnode.createNodeElt(self.sketchpad, midpt, self.textspans[index].y,self.textspans[index].width, posgroup, 0);
                //tmpnode.createNodeElt(self.sketchpad, self.textspans[index].x, self.textspans[index].y,self.textspans[index].width, posgroup, 0);
                //add the pos node to list of nodes of tree
                self.allnodes[self.allnodes.length] = tmpnode;
            }
        }
       
     });
};

/**
 * Node object method
 * Create node element ( POS or Constituent )  
 * @param sketch
 * @param x
 * @param y
 * @param width
 * @param group
 * @param groupclass
 */
Node.prototype.createNodeElt = function(sketch, x, y, width, group, groupclass){
	
   //add the level of node as class 'level_0/1/2..'
   var subgroup = sketch.group(group, { 'class': 'level_'+groupclass+''});
   
   //set the address received from backend as ID of node
   subgroup.setAttribute('id',this.id);   
   
   this.level = groupclass;
   
   var node_name = this.text;
   
   //get the font size applied on node
   var fontsize = parseInt($(group).css('font-size'));
   
   //measure the width of text and adjust the node rectangle width accordingly
   var measurewidth = getTextWidth(node_name, fontsize);
   
   //measure approx. height of text 
   var measureheight = fontsize * 1.5;
   
   //add padding of 10px to rectange
   var rect_width = measurewidth +  10;
   var rect_height = measureheight + 10;
   
   //Add 80px fixed vertical margin between child and parent nodes
   var rect_y =  y - nodesmargin - measureheight;

   //create the node above the child node 
   //this.x =  x + width/4 - 5;
   this.x = x - rect_width/2;
   this.y = rect_y;
   this.width = rect_width;
   this.height = rect_height;
   
   sketch.rect(subgroup, x - rect_width/2, rect_y, rect_width, rect_height, 8,8,{fill: 'none', stroke: 'black', strokeWidth: '0,50'});
   
   var textelt = sketch.text(subgroup, x - rect_width/2 + 5, y - nodesmargin,node_name, {fill: 'black'});
   textelt.setAttribute('class', node_name);
   
   this.nodecreated = true;
   
   //bind the node with click/drag/drop/hover mouse events to handlers
   $(subgroup).on( "mousedown",{node: this}, startDrag).on( "mousemove", dragging).on( "mouseup mouseenter", {node: this}, endDrag).on( "mouseover", animateNode).on("mouseleave", nodeMouseLeave);
   
   //dynamically adjust height with tree nodes height (If the container height is not enough increase it)
   if(rect_y < (nodesmargin + 5)){
	   var increasedheight = (nodesmargin * 2) + 5;
	   var currentheight  = $('#svgsketch').height();
	   //need to update height of container
		$('#svgsketch').height(parseInt(currentheight) + increasedheight);
		//need to update height of container
		if(parseInt(currentheight) + increasedheight > parseInt($('#content').height())  && parseInt($('#content').height()) < treeContainerMaxHeight ){
			parseInt(currentheight) + increasedheight + marginContainerAndTree_vert > treeContainerMaxHeight ? $('#content').height(treeContainerMaxHeight) : $('#content').height(parseInt(currentheight) + increasedheight + marginContainerAndTree_vert);
	    }
		//return false to calling method to request re-rendering view
		return false;
   }else { 
	   //no need to update height of container 
	   return true; 
   }
   
};

/**
 * Object Node function
 * Reposition node if its level changed in runtime
 * @param new_x
 * @param new_y
 * @param new_level
 * @returns {Boolean} 
 */
Node.prototype.repositionParentElt = function(new_x, new_y, new_level){
	//get the current node elements 
	var rect = $('#'+this.id)[0].childNodes[0];
    var text = $('#'+this.id)[0].childNodes[1];
    // get the font size applied on node
    var fontsize = parseInt($(text).css('font-size'));
    //get the approx. height of node
    var measureheight = fontsize * 1.5;
    var rect_y;
    //set the new y position with 80px fixed margin from child node
    rect_y =  new_y - nodesmargin - measureheight;
    this.x = new_x - 5;
	this.y = rect_y;
	this.level = new_level;
    rect.setAttribute('x', new_x - 5 );
    text.setAttribute('x', new_x);
    rect.setAttribute('y', rect_y);
    text.setAttribute('y', rect_y + measureheight);

    if(rect_y < (nodesmargin + 5)){
	    var currentheight  = $('#svgsketch').height();
	    //increase height of tree container
	    var increasedheight = (nodesmargin * 2) + 5;
		$('#svgsketch').height(parseInt(currentheight) + increasedheight);
		//need to update height of container
		if(parseInt(currentheight) + increasedheight > parseInt($('#content').height())  && parseInt($('#content').height()) < treeContainerMaxHeight ){
			parseInt(currentheight) + increasedheight + marginContainerAndTree_vert > treeContainerMaxHeight ? $('#content').height(treeContainerMaxHeight) : $('#content').height(parseInt(currentheight) + increasedheight + marginContainerAndTree_vert);
	    }
		//return false to calling method to request re-rendering view
		return false;
    }else{
 	   //no need to update height of container     	
    	return true; 
    }
};

/**
 * Object Node function
 * update the x position of node in case of overlappings
 * @param new_x
 */
Node.prototype.updateXPos = function(new_x){
	//get the elements of node
	var rect = $('#'+this.id)[0].childNodes[0];
    var text = $('#'+this.id)[0].childNodes[1];
    //set the new x position
    this.x = new_x - 5;
    rect.setAttribute('x', new_x - 5 );
    text.setAttribute('x', new_x);
};

/**
 * Object SyntacticTree function
 * Applicable to Rendering mode : Iterative 
 * In case of iterative view rendering, adding POS node iteratively
 * @param nodedata 			the details of new POS to be added 
 */
SyntacticTree.prototype.nodeIterativePOSAdd = function ( nodedata ){
	iterativeupdate = true; 
    var self = this;
    sketchpad = this.sketchpad;
    var index = idx(self.offsetdata, nodedata.offset);
            if(index != -1){
            	//add 'hasparent' class to the child text span
                self.textspans[index]['hasnode'] = true;
                var textnode = $('#s_' +nodedata.offset[0] +'_'+ nodedata.offset[1])[0];
                textnode.setAttribute('class', 'hasnode');
                //create the POS node
                tmpnode = new Node(nodedata.text, nodedata.offset, nodedata.id, true);
                var midpt = self.textspans[index].x + self.textspans[index].width/2;
                tmpnode.createNodeElt(self.sketchpad, midpt, self.textspans[index].y,self.textspans[index].width, posgroup, 0);
                //tmpnode.createNodeElt(self.sketchpad, self.textspans[index].x, self.textspans[index].y,self.textspans[index].width, posgroup, 0);
                //Add the new pos node to the list of the tree nodes
                self.allnodes[self.allnodes.length] = tmpnode;
    }
    iterativeupdate = false; 
};

/**
 * Event handler on node click event
 * @param event
 */
function nodeclick(event){
    selectednode = event.data.node;
    //restrict the add parent option, only nodes without parent allowed
    if(selectednode.hasParent){
		$('.deletenodeaction').hide();
	}else{
		$('.deletenodeaction').show();
	}
    $('#node_id').val(selectednode.id);
    //open node actions modal
    $('#nodeActions').modal('show');    
}

/**
 * Event handler binded to node mousedown event
 * @param event
 */
function startDrag(event) { 
		//get the node data on which event performed
		if(event.data){
			startnode = event.data.node;
			//node does not have a parent, start dragging
			if(!startnode.hasParent){
				dragoffset = (sketchpad._embedded ? {left: 0, top: 0} : { left: $('#svgsketch').offset().left , top : $('#svgsketch').offset().top});
				if (!sketchpad._embedded) { 
					dragoffset.left -= document.documentElement.scrollLeft || document.body.scrollLeft;
					dragoffset.top -= document.documentElement.scrollTop || document.body.scrollTop;
				}
				
				start = {X: event.clientX - dragoffset.left, Y: event.clientY - dragoffset.top };
				
			}else if(startnode.hasParent){
				//node has parent, show node actions modal
				$('.deletenodeaction').hide();
				$('#node_id').val(startnode.id);
				$('#nodeActions').modal('show');  
			}
		}else{
			start = null;
			return;
		}
		event.preventDefault();	
	} 
	
/**
 * Event handler bind to mouse move event
 * updates the line showing the dragging
 * @param event
 */
function dragging(event) {   
	
		if (!start) { 
			return; 
		} 
		if (!outline) { 
			outline = sketchpad.line(0, 0, 0, 0, 
				{fill: 'none', stroke: '#6666ff', strokeWidth: 2, strokeDashArray: '2,2'}); 
			$(outline).mouseup(endDrag); 
		} 
		
		sketchpad.change(outline, 
			{x1: start.X, 
			y1: start.Y, 
			x2: dragoffset.left < 0 ? event.clientX - dragoffset.left : Math.min(event.clientX - dragoffset.left, event.clientX),
			y2: dragoffset.top < 0? event.clientY  - dragoffset.top : Math.min(event.clientY - dragoffset.top, event.clientY)}); 
		
		event.preventDefault(); 
} 


/**
 * Event handler bind to mouseup and mouseenter event
 * @param event
 */	 
function endDrag(event, test) {
		
		if (!start) { 
			return; 
		} 
		if(event.data){
			$(outline).remove(); 
			outline = null; 
			//send add edge request if valid drag start and drag end nodes
			createEdge(start.X, start.Y, 
				event.clientX - dragoffset.left, event.clientY - dragoffset.top, event.data.node); 
			start = null;
		}else{
			$().toastmessage('showErrorToast', "Please try again, drag unsuccessful.");			
			$(outline).remove(); 
			outline = null; 
			start = null;
			return ;	
		}
		event.preventDefault(); 
} 

/**
 * function creates an edge between existing parent and child selected
 * @param x1:	x pos drag start
 * @param y1:	y pos drag start
 * @param x2:	x pos drag end
 * @param y2: 	y pos drag end
 * @param node: parent node
 */
function createEdge(x1, y1, x2, y2, node) { 
		selectednode = node;
		var left = Math.min(x1, x2); 
		var top = Math.min(y1, y2); 
		var right = Math.max(x1, x2); 
		var bottom = Math.max(y1, y2); 
		var settings = {fill: 'white', stroke: 'black', strokeWidth: '1'}; 
		//If drag did not end at same point
		if(!(x1 === x2 && y1 === y2)){
			var id = startnode.id;	//child address
			var parentid = selectednode.id;	//parent address
			if(selectednode.isPOS){			// STOP: If selected parent is POS node
				$().toastmessage('showWarningToast', "POS node cannot be parent");
				return;
			}else if(!startnode.hasParent && id && parentid && parentid !== id){	
				//Allow: If selected parent child nodes have valid id 
				// If drag did not stop at same node i.e. child address = parent address
				// If Child node does not have any parent ( only one parent allowed in dependency tree )
				var allowedge = true;
				//Check if parent node selected address is not among children of node
				$("[id^=e_"+id+"_]").each(function(){
					var edgeid = $(this).attr('id').split("_");
					if(edgeid[2] == parentid){
						allowedge = false;
						$().toastmessage('showWarningToast', "Child node cannot be selected");
					}
				});
				
				if(allowedge){
					$().toastmessage('showSuccessToast', "Add Edge requested");
					//Send addEdge request to server
					json = {"action":"addEdge",
							"mode":$('#renderingmode').val(),
							"parentid":parentid,
							"childid":id, 
							"file": localStorage.getItem("file"), 
							"sendUnixTime" : new Date().getTime(),
							"version": renderview.version,
							"latency": renderview.latency,
							"user": localStorage.getItem("user")
							};
					sendMessage(JSON.stringify(json));
				}
				start = null;
				return;
			}else{
				$().toastmessage('showNoticeToast', "drag unsuccessful");
				start = null;
				return;
			}	
		}else{
			//If drag ends at same  point, consider click event
			start = null;
			if(startnode.hasParent){
				$('.deletenodeaction').hide();
			}else{
				$('.deletenodeaction').show();
			}
			$('#node_id').val(selectednode.id);
			$('#nodeActions').modal('show');  
		}
	}; 

/**
 * Event handler on mouseover event on node
 * Animates the nodes on mouse hover
 * @param event
 */	
function animateNode(event){
	//sent position message for user position update
	if($(event.currentTarget).attr("id")){
		if(localStorage.getItem("user") != "" && localStorage.getItem("user") != undefined ){
			var json = {action:"pos", user: localStorage.getItem("user"), element: $(event.currentTarget).attr("id"), selectedfile : localStorage.getItem("file"), show: true };
			sendMessage(JSON.stringify(json));
		}
	}
	
	//Show hover effect for 500ms
	var nodegroup = event.currentTarget;
	$(nodegroup, sketchpad.root()).addClass('hovereffect');
		setTimeout(function() {
		$(nodegroup, sketchpad.root()).removeClass('hovereffect');
	}, 500)
}

/**
 * Event handler on mouseleave event on node
 * sent position message for user position update
 * @param event
 */ 
function nodeMouseLeave(event){
	if($(event.currentTarget).attr("id")){
		var json = {action:"pos", user: localStorage.getItem("user"), element: $(event.currentTarget).attr("id"), selectedfile : localStorage.getItem("file"), show: false};
		sendMessage(JSON.stringify(json));
	}
}