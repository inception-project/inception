/* 
 * Thesis: Collaborative Web-based Tool for the Annotation of Syntactic Parse Trees
 * @author : Asha Joshi
 * Technische Universität Darmstadt 
 * This script contains all the methods for visualization and handling of constituent nodes and edges of Tree
 */

//var edgetmp; //-- to remove

//Global variables
var constituentGroup;		//the group consisting of all constituent nodes
var groupedges;				//the group consisting of all edges
var iterativeupdate = false;
/**
 * Object Edge method
 * creates edge with the give start point(x1,y1) to give end point(x2,y2)
 * @param sketchpad
 * @param x1
 * @param y1
 * @param x2
 * @param y2
 * @param setting
 */
Edge.prototype.createEdge = function (sketchpad, x1,y1,x2,y2,setting){	
	//Add the new edge group to the main 'edge' group
	//Edge id 'e_parentAddress_childAddress
	var subgroup = sketchpad.group(groupedges, { 'id': ''+this.id+''});
	
	var node_ids = this.id.split("_");
	
	//Check if edge has a valid ID
	if(node_ids.length == 3){
		//Bind mouse events to handler
		$(subgroup).on( "mouseover", animateEdge);
		$(subgroup).on( "mouseleave", edgeMouseleave);
		$(subgroup).on( "click", updateEdge);
		var path1 = sketchpad.createPath(); 
		var path2 = sketchpad.createPath();	
		var edgeheight = y2 - y1;
		
		//calculate edge label text height width
		var fontsize = parseInt($(groupedges).css('font-size'));
		var textwidth = getTextWidth(this.text, fontsize);
		var textheight =  fontsize * 1;
		
		//create edge components
		sketchpad.text(subgroup, x2 - textwidth/2 , y2 - edgeheight/2 - 2 , this.text, {fill: 'black'});
		sketchpad.path(subgroup, path1.move(x2, y2).line(x2, y2 - edgeheight/2).close(),{fill: 'none', stroke: '#6666ff', strokeWidth: '1px', markerStart: 'url(#markerArrow)'});
		sketchpad.path(subgroup, path2.move(x2, y2 - edgeheight/2 - textheight).line(x1, y1).close(),{fill: 'none', stroke: '#6666ff', strokeWidth: '1px'});
		this.text_x = x2;
		
		//set the rendered property to true
		this.rendered = true;
	}
};

/**
 * This function calculates and returns the index of an element with a specific ID in an array 
 */
var idx_id = function(array, val){
    var index = -1;
    //Iterate through all elements of array
    $.each(array, function( i, value ) {
    	//compare the ID with the ID of element in array
        if(value.id == val){
        	//If found return from loop
            index = i; return false;
        }else{
            index = -1;
        }
    });
    //return the index
    return index;
};


/**
 * This function returns all the child nodes of a node and the respective edges
 */
var idx_childedges = function(array, val){
	//array to store child nodes
    var childNodeid = [];
    //array to store child edges
    var childedge = [];
    //Iterate over all edges
    $.each(array, function( i, value ) {
        if(value.to == val){
            childNodeid[childNodeid.length] = value.from;
            childedge[childedge.length] = value;
        }
    });
    var child = new Edgechildlist( childedge, childNodeid );
    return child;
};

/**
 * This function returns the  parent edge of a node
 */
var idx_parentege = function(array, val){
    //var parentNodeid = [];
    var parentedge = -1;
    $.each(array, function( i, value ) {
        if(value.from == val){
            parentedge = i;
            return false;
        }
    });
    return parentedge;
};

/**
 * This node returns the node with highest level among all the children
 */
var getparentYpos = function ( allnodes, childarray ){
	//the index of child node in array of all nodes
	var nodeindex = null;
	//the level of child node
	var level = null;
	//Iterate over all children nodes
	 $.each(childarray, function( i, value ) {
	        index = idx_id( allnodes, value );
	        if(allnodes[index].nodecreated){
	        	if(level == null){
		        	level = allnodes[index].level;
		        	nodeindex = index;
	        	}
	        	//get the node with highest level
		        if(level < allnodes[index].level){
		        	nodeindex = index;
		        	level = allnodes[index].level;
		        }
	        }
	  });
	 return nodeindex;
}

/**
 * calculates the leftmost child node and right most node
 * array1 : all nodes
 * array2: child nodes
 * returns the horizontal mid point from all the child nodes 
 */
var getparentXpos = function(array1, array2){
    var index = -1;
    var least_x  = -1;
    var most_x  = -1;    
    $.each(array2, function( i, value ) {
        index = idx_id(array1,value);
        if(index >= 0){
            if(i === 0){
                least_x = array1[index].x;
                most_x = array1[index].x;
            }else{
            	//calculate the least X point and most X point  
                if((least_x > array1[index].x))
                    least_x = array1[index].x;
                if((most_x < array1[index].x))
                    most_x = array1[index].x;
            }
        }
    });    
    var middistance  = (most_x - least_x)/2;
    //return the horizontal mid point of all nodes
    return least_x + middistance;
};

/**
 * Object SyntacticTree function
 * On node creation or reposition, update its parent edge and all child nodes(rendered and not yet rendered)
 * @param node the node updated/created
 */
SyntacticTree.prototype.fullUpdateChildParentEdge = function ( node ){
	//get all the childedges of node
	var childedges = idx_childedges(this.edges, this.allnodes[node].id);
	var self = this;
	//iterate over all child edges
	$.each(childedges.edgelist, function( indexedge, edgevalue ) {
		var tempindex = idx_id(self.edges, edgevalue.id);	
		var tempchildindex = idx_id(self.allnodes, edgevalue.from);
		//delete old edge and create new edge with node new position 
		if($('#'+edgevalue.id).length == 1){
	       $('#'+edgevalue.id)[0].remove();
	       self.edges[tempindex].createEdge(self.sketchpad, self.allnodes[node].x + self.allnodes[node].width/2 , self.allnodes[node].y + self.allnodes[node].height , self.allnodes[tempchildindex].x  + self.allnodes[tempchildindex].width/2, self.allnodes[tempchildindex].y, {fill: 'white', stroke: 'black', strokeWidth: '1'});
	    }
		//also create edges not yet rendered
		else{
	    	self.allnodes[tempchildindex].hasParent = true;
            $( $('#' + self.allnodes[tempchildindex].id)[0], self.sketchpad.root).addClass('hasParent');
	        self.edges[tempindex].createEdge(self.sketchpad,self.allnodes[node].x + self.allnodes[node].width/2, self.allnodes[node].y + self.allnodes[node].height,self.allnodes[tempchildindex].x + self.allnodes[tempchildindex].width/2 , self.allnodes[tempchildindex].y , {fill: 'white', stroke: 'black', strokeWidth: '1'});
	    }
	});
	
	//remove the parent edge to be rendered again later
	var parentedge = idx_parentege(this.edges, this.allnodes[node].id);
	if(parentedge != -1){
		if(this.edges[parentedge].rendered){
			this.edges[parentedge].rendered = false;
			$('#' +this.edges[parentedge].id)[0].remove();
		}
			                	
	}
};

/**
 * Object SyntacticTree function
 * On node creation or reposition, update parent edge and all already rendered child nodes
 * @param node the node updated/created
 */
SyntacticTree.prototype.updateChildParentEdge = function ( node ){
	//get all the childedges of node	
	var childedges = idx_childedges(this.edges, this.allnodes[node].id);
	var self = this;
	//iterate over all child edges	
	$.each(childedges.edgelist, function( indexedge, edgevalue ) {
		var tempindex = idx_id(self.edges, edgevalue.id);	
		var tempchildindex = idx_id(self.allnodes, edgevalue.from);
		//delete old edge and create new edge with node new position		
		if($('#'+edgevalue.id).length == 1){
	       $('#'+edgevalue.id)[0].remove();
	       self.edges[tempindex].createEdge(self.sketchpad, self.allnodes[node].x + self.allnodes[node].width/2 , self.allnodes[node].y + self.allnodes[node].height , self.allnodes[tempchildindex].x  + self.allnodes[tempchildindex].width/2, self.allnodes[tempchildindex].y, {fill: 'white', stroke: 'black', strokeWidth: '1'});
	    }
	});

	//remove the parent edge to be rendered again later
	var parentedge = idx_parentege(this.edges, this.allnodes[node].id);
	if(parentedge != -1){
		if(this.edges[parentedge].rendered){
			this.edges[parentedge].rendered = false;
			$('#' +this.edges[parentedge].id)[0].remove();
		}
	}
};


/**
 * This function check for overlapping of two nodes
 * node 1 dimensions 
 * x1: X pos
 * y1: Y pos
 * w1: width
 * h1: height
 * node 2 dimensions
 * x2: X pos
 * y2: Y pos
 * w2: width
 * h2: height
 * returns boolean value overlapping  
 */
var collision = function (x1, y1, w1, h1, x2, y2, w2, h2) {
    var b1 = y1 + h1;
    var r1 = x1 + w1;
    var b2 = y2 + h2;
    var r2 = x2 + w2;

    if (b1 < y2 || y1 > b2 || r1 < x2 || x1 > r2) return false;
    return true;
};

/**
 * Object SyntacticTree function
 * This checks if the newly create node is overlapping with any other node  
 * @param newElement
 */
SyntacticTree.prototype.checkOverlapping = function ( newElement ){
	//array to the index of all the nodes which overlap with the new node 
	var overlappingElt = [];
	var newElementIndex = idx_id(this.allnodes, newElement);
	var self = this;
	//Iterate over all nodes and check for overlapping
	$.each(self.allnodes, function( i, value ) {
		if(value.nodecreated && value.id != newElement){
			if(collision(self.allnodes[newElementIndex].x, self.allnodes[newElementIndex].y, 
					self.allnodes[newElementIndex].width, self.allnodes[newElementIndex].height, 
					value.x, value.y, value.width, value.height)){
				overlappingElt[overlappingElt.length] = i;	
			}
		}
		
	});
	
	//add new node to the array of nodes which need repositioning
	if(overlappingElt.length != 0){
		overlappingElt[overlappingElt.length] = newElementIndex;
	}
	
	//get the left most node 
	var startpoint = 0;
	while(overlappingElt.length > 0 ){
		var least_x = 0;
		var totaloverlappingwidth = 0;
		var leftmostelt = null;
		var indexupdated = null;
		$.each(overlappingElt , function ( i , value){
			if(self.allnodes[value].nodecreated){
				if(least_x == 0){
					least_x = self.allnodes[value].x;
					leftmostelt = value;
					indexupdated = i;
				}
				if(least_x > self.allnodes[value].x){
					least_x = self.allnodes[value].x;
					leftmostelt = value;
					indexupdated = i;
				}
				totaloverlappingwidth += self.allnodes[value].width;
			}
		});
		
		//set startpoint the leftmost node minus half the (width of all overlapping nodes)
		//done only for the fist repositioned node
		if(leftmostelt != null && startpoint == 0){
			var totaloverlapping = overlappingElt.length;
			startpoint =  this.allnodes[leftmostelt].x - Math.floor(totaloverlappingwidth/2);
		}
		
		//update element x pos
		this.allnodes[leftmostelt].updateXPos(startpoint);
		var isoverlapping = false;
		var overlappingwith = null;
		//check if the node updated position still overlapps with any other node
		$.each(self.allnodes, function( i, value ) {
			if(value.nodecreated && i != leftmostelt){
				if(collision(self.allnodes[leftmostelt].x, self.allnodes[leftmostelt].y, 
						self.allnodes[leftmostelt].width, self.allnodes[leftmostelt].height, 
						value.x, value.y, value.width, value.height)){
					isoverlapping = true;
					overlappingwith = value;
					return false;
				}
			}
		});
		//if overlapping after repositioning shift the new node to 10px right of overlapped node
		if(isoverlapping){
			startpoint = startpoint + overlappingwith.width + 10;
			this.allnodes[leftmostelt].updateXPos(startpoint);
		}
		
		//update the parent edge and child edges w.r.t the new node position
		this.updateChildParentEdge( leftmostelt )
		startpoint = startpoint + this.allnodes[leftmostelt].width + 10;
		//remove the updated node from array
		overlappingElt.splice(indexupdated,1);
	}
}

/**
 * Object SyntacticTree function
 * Layer 1 renders all the nodes which are immediate parents of POS nodes 
 */
SyntacticTree.prototype.createPOSParents = function (){
		var self = this;		
		var rerender = false;
		$.each(self.edgesdata, function( i, value ) {
			//add the new edge to the array
			var edgetmp = new Edge(value.id, value.label, value.from, value.to);
			self.edges[self.edges.length] = edgetmp;
			var childindex = idx_id(self.allnodes, value.from);

			var parentindex = idx_id(self.allnodes, value.to);
			if(self.allnodes[childindex].isPOS){
					//get all the child edges of parent node
					var childedges = idx_childedges(self.edges, value.to);
					var edgeindex = idx_id(self.edges, value.id);
					//create parent node
					if(!self.createPOSParent(childedges, childindex, parentindex, edgeindex)){
						//if node created successfully but the container dimensions changed stop further processing
						rerender = true;
						return false;
					}
			}
		});
		
		if(rerender){
			rerender = false;
			if (localStorage.getItem("renderingmode") == "1" && iterativeupdate){
				requestCompleteData();
			}else{
				//render the whole view again
				renderview.reRenderView();
			}
		}
};

/**
 * Object SyntacticTree function 
 * Create the parent node of child POS node
 * @param childedges
 * @param childindex
 * @param parentindex
 * @param edgeindex
 * @returns {Boolean}
 */
SyntacticTree.prototype.createPOSParent = function (childedges, childindex, parentindex, edgeindex){
	//If parent node doesn't exist
    if($('#' + this.allnodes[parentindex].id).length <= 0){
		this.allnodes[childindex].hasParent = true;
    	$( $('#' + this.allnodes[childindex].id)[0] , this.sketchpad.root).addClass('hasParent');
    	//create parent node
    	var midpt = this.allnodes[childindex].x + this.allnodes[childindex].width/2;
		if(this.allnodes[parentindex].createNodeElt(this.sketchpad, midpt, this.allnodes[childindex].y, this.allnodes[childindex].width, constituentGroup, 1)){
    		this.checkOverlapping(this.allnodes[parentindex].id);
    		this.edges[edgeindex].createEdge(this.sketchpad,this.allnodes[parentindex].x + this.allnodes[parentindex].width/2, this.allnodes[parentindex].y + this.allnodes[parentindex].height,this.allnodes[childindex].x + this.allnodes[childindex].width/2 ,this.allnodes[childindex].y , {fill: 'white', stroke: 'black', strokeWidth: '1'});
    		//If node create successfully
    		return true;
		}else{
			//If node doesn't fit the container and container updates height
			return false;
		}

    }
    //If parent node already exist
    else{
    	//In this case parent node is already created
    	this.allnodes[childindex].hasParent = true;
    	$( $('#' + this.allnodes[childindex].id)[0], this.sketchpad.root).addClass('hasParent');
    	//Get the new x position 
    	var parentXPos = getparentXpos(this.allnodes , childedges.childnodelist);
    	//Get the new y position of node with highest level
    	var childnodeselected =  getparentYpos(this.allnodes , childedges.childnodelist);
    	var parentlevel = this.allnodes[childnodeselected].level + 1;
    	//reposition node slightly higher than other nodes
    	$('#' + this.allnodes[parentindex].id).removeAttr('class');
    	$('#' + this.allnodes[parentindex].id).attr('class', ('level_' + parentlevel));
    	var parent_childedges = idx_childedges(this.edges, this.allnodes[parentindex].id);
    	if(this.allnodes[parentindex].repositionParentElt(parentXPos, this.allnodes[childnodeselected].y, parentlevel)){
    		this.fullUpdateChildParentEdge(parentindex);
    		//check if parent node is overlapping any other node
            this.checkOverlapping(this.allnodes[parentindex].id);
            return true;
    	}else{
    		return false;
    	}
    }
}

/**
 * This function checks if all edges are rendered
 * @returns {Boolean}
 */
SyntacticTree.prototype.alledgesRendered = function(){
	var edgesrendered = true;
	$.each(this.edges, function(i, value){
		if(!value.rendered){
			edgesrendered = false;
			return false;
		}
	});
	
	return edgesrendered;
	
}

/**
 * Object SyntacticTree function
 * @param currentlevel 			The level form which the rendering will start
 */
SyntacticTree.prototype.createOtherConstituents = function ( currentlevel ){	
		var self = this;
		var test = 5;
		var breakfromhere = false;
		//iterate until all the edges are rendered
		while(!self.alledgesRendered()){
		//while(test > 1){
			test =  test - 1;
			//iterate over all edges
			$.each(self.edges, function( i, value ) {
				var childindex = idx_id(self.allnodes, value.from);
				var parentindex = idx_id(self.allnodes, value.to);
				//Check if the edge is not yet rendered 
				if(!value.rendered && childindex >= 0 && parentindex >= 0){
					//Render edges which child node equal to current level
					if(self.allnodes[childindex].level == currentlevel){
						var edgeindex = idx_id(self.edges , value.id);
						
						//get the child nodes of for both Edge parent and child node 
						var parent_childedges = idx_childedges(self.edges, value.to);
						var child_childedges = idx_childedges(self.edges, value.from);
						if($('#' + self.allnodes[parentindex].id).length == 1 ){
							
							//Parent node already exist
							self.allnodes[childindex].hasParent = true;
				        	$( $('#' + self.allnodes[childindex].id)[0], self.sketchpad.root).addClass('hasParent');
				        	
							var parentXPos = getparentXpos(self.allnodes , parent_childedges.childnodelist);
				        	//Get the new y position of node with highest level
				        	var childnodeselected =  getparentYpos(self.allnodes , parent_childedges.childnodelist);
				        	var parentlevel = self.allnodes[childnodeselected].level + 1;
				        	//reposition node slightly higher than other nodes
				        	$('#' + self.allnodes[parentindex].id).removeAttr('class');
				        	$('#' + self.allnodes[parentindex].id).attr('class', ('level_' + parentlevel));
				        	if(self.allnodes[parentindex].repositionParentElt(parentXPos, self.allnodes[childnodeselected].y, parentlevel)){
				        		self.fullUpdateChildParentEdge(parentindex);
				        		//Check overlapping
					            self.checkOverlapping(self.allnodes[parentindex].id);
				        	}else{
				        		//If the repositioned node doesn't fit the container, stop further execution
				        		breakfromhere = true;
				        		return false;
				        	}
			                	
						}else{
							//Parent node doesn't exist
							
							//Child node not yet rendered. Mark this edge as not rendered
							if(parent_childedges.childnodelist.length == 0 ){
								self.edges[edgeindex].rendered = false;
							}
							//Node has only one child
							else if( parent_childedges.childnodelist.length == 1 ){
								//Child node already rendered
								self.allnodes[childindex].hasParent = true;
					        	$( $('#' + self.allnodes[childindex].id)[0] , self.sketchpad.root).addClass('hasParent');	
					        	//Create parent node
					        	var midpt = self.allnodes[childindex].x + self.allnodes[childindex].width/2;
					        	if(self.allnodes[parentindex].createNodeElt(self.sketchpad, midpt, self.allnodes[childindex].y, self.allnodes[childindex].width, constituentGroup, currentlevel + 1)){
					        		self.edges[edgeindex].createEdge(self.sketchpad,self.allnodes[parentindex].x + self.allnodes[parentindex].width/2, self.allnodes[parentindex].y + self.allnodes[parentindex].height,self.allnodes[childindex].x + self.allnodes[childindex].width/2 ,self.allnodes[childindex].y , {fill: 'white', stroke: 'black', strokeWidth: '1'});
										self.fullUpdateChildParentEdge(parentindex);
										//Check overlapping
						                self.checkOverlapping(self.allnodes[parentindex].id);
					        	}else{
					        		//If the repositioned node doesn't fit the container, stop further execution					        		
							        breakfromhere = true;
							        return false;
							    }
							}
							//Node has more than one child							
							else if( parent_childedges.childnodelist.length > 1 ){
								//In this case parent node is already created
					        	self.allnodes[childindex].hasParent = true;
					        	$( $('#' + self.allnodes[childindex].id)[0], self.sketchpad.root).addClass('hasParent');
					        	//get the parent X position (mid point of all child nodes)
					        	var parentXPos = getparentXpos(self.allnodes , parent_childedges.childnodelist);
					        	//get the parent y position (Y position of child with highest level) 
					        	var childnodeselected =  getparentYpos(self.allnodes , parent_childedges.childnodelist);
					        	//Create parent node
						        if(self.allnodes[parentindex].createNodeElt(self.sketchpad, parentXPos, self.allnodes[childnodeselected].y, self.allnodes[childindex].width, constituentGroup, currentlevel + 1)){
						        		self.edges[edgeindex].createEdge(self.sketchpad,self.allnodes[parentindex].x + self.allnodes[parentindex].width/2, self.allnodes[parentindex].y + self.allnodes[parentindex].height,self.allnodes[childindex].x + self.allnodes[childindex].width/2 ,self.allnodes[childindex].y , {fill: 'white', stroke: 'black', strokeWidth: '1'});
										self.fullUpdateChildParentEdge(parentindex);
						                self.checkOverlapping(self.allnodes[parentindex].id);
								}else{
					        		//If the repositioned node doesn't fit the container, stop further execution	
					        		breakfromhere = true;
					        		return false;
					        	}
							}
						}
					}
				}
			});
			
			//If container height changed
			if(breakfromhere){
				breakfromhere = false;
				if (localStorage.getItem("renderingmode") == "1" && iterativeupdate){
					requestCompleteData();
				}else{
					//re-render whole tree again
					renderview.reRenderView();
				}
				//break the while loop
				break;
			}
			
			//increment the current level
			currentlevel ++;
		}
		
};

/**
 * Object SyntacticTree function
 * render the all the constituent nodes and edges
 */
SyntacticTree.prototype.createConstituentLayer = function (){
	//create group for all constituent node
	//set the parent to 'main group'
    constituentGroup = this.sketchpad.group(this.maingroup,{'class': 'constituent' });
    //create group for all edge
	//set the parent to 'main group'
    groupedges = this.sketchpad.group(this.maingroup,{'class': 'edges' });

    var self = this;
    //Iterate through all the node received from backend and add the constituents nodes to array
    $.each(this.nodesdata, function( i, value ) {
    	//Ignore POS nodes
    	//the pos nodes are already rendered and are in the array
        if(value.offset[0] === 0 && value.offset[1] === 0){
                tmpnode = new Node(value.text, [], value.id, false);
                self.allnodes[self.allnodes.length] =  tmpnode;
        }
    });
    
    //First create the immediate parent nodes of POS nodes ( Level 1 )
    this.createPOSParents();
    
    //Create all the other nodes 
    //start from level 1 ( currentlevel = 1 )
    this.createOtherConstituents( 1 );
};

/**
 * Object SyntacticTree function
 * Applicable to Rendering mode : Iterative 
 * add the new node to list of all nodes
 * @param nodedata
 */
SyntacticTree.prototype.nodeIterativeAdd = function ( nodedata ){
    tmpnode = new Node(nodedata.text, [], nodedata.id, false);
    this.allnodes[this.allnodes.length] =  tmpnode;
};

/**
 * Object SyntacticTree function
 * Applicable to Rendering mode : Iterative 
 * Render the new edge and respective parent node
 * @param edgedata
 */
SyntacticTree.prototype.edgeIterativeAdd = function ( edgedata ){
	iterativeupdate = true;
	//create an new edge
	var edgetmp = new Edge(edgedata.id, edgedata.label, edgedata.from, edgedata.to);
	//add the new edge to array of all edges
	this.edges[this.edges.length] = edgetmp;
	//get the index of child node of edge
	var childindex = idx_id(this.allnodes, edgedata.from);
	if(childindex != -1){
		//if child node is POS
		if(this.allnodes[childindex].isPOS){
			var edgeindex = idx_id(this.edges , edgedata.id);
			//get all the other children if exist of the parent node
			var childedges = idx_childedges(this.edges, edgedata.to);
			var parentindex = idx_id(this.allnodes, edgedata.to);
			//create POS parent
			if(!this.createPOSParent(childedges, childindex, parentindex, edgeindex)){
				//If the parent node doesn't fit in container 
				if (localStorage.getItem("renderingmode") == "1" && iterativeupdate){
					requestCompleteData();
				}else{
					//updater container height and re-render view
					renderview.reRenderView();
				}
			}
			//Create other Constituent node
			else{
				this.createOtherConstituents(1);
			}
		}else{
			//check for all non rendered nodes starting from with child node level and render them
			this.createOtherConstituents( this.allnodes[childindex].level );
		}
	}
	iterativeupdate = false;
}

/**
 * Object SyntacticTree function
 * Applicable to Rendering mode : Iterative 
 * Updates the node text
 * @param nodedata
 */
SyntacticTree.prototype.nodeIterativeUpdate = function ( nodedata ){
	iterativeupdate = true;
	var index = idx_id(this.allnodes, nodedata.id);
	if(index != -1){
		this.allnodes[index].text = nodedata.text;
		//get the font size applied on node
		var fontsize = parseInt($(constituentGroup).css('font-size'));
		var measurewidth = getTextWidth(nodedata.text, fontsize);
		if(measurewidth + 10 > this.allnodes[index].width){
			this.allnodes[index].width = measurewidth + 10;
			var rect = $('#'+this.allnodes[index].id)[0].childNodes[0];
			rect.setAttribute("width", measurewidth + 10);
		}
		var text = $('#'+this.allnodes[index].id)[0].childNodes[1];
		text.setAttribute('class', nodedata.text);
		text.textContent = nodedata.text;
	}
	iterativeupdate= false;
}; 

/**
 * Object SyntacticTree function
 * Applicable to Rendering mode : Iterative 
 * Updates the edge label for all the edges of the parent node
 * @param edgedata
 */
SyntacticTree.prototype.edgeIterativeUpdate = function ( edgedata ){
	iterativeupdate = true;
		var edgeindex = idx_id(this.edges,edgedata.id);
		this.edges[edgeindex].text = edgedata.text;
		var fontsize = parseInt($(groupedges).css('font-size'));
		var textwidth = getTextWidth(edgedata.text, fontsize);
		var text = $('#'+edgedata.id)[0].childNodes[0];
		text.setAttribute('x', this.edges[edgeindex].text_x - textwidth/2);
		text.textContent = edgedata.text;
		iterativeupdate = false;
}; 

/**
 * Object SyntacticTree function
 * Applicable to Rendering mode : Iterative 
 * Delete the edge
 * @param edgeID
 */
SyntacticTree.prototype.edgeIterativeDelete = function ( edgeID ){
	iterativeupdate = true;
	var edgeindex = idx_id(this.edges, edgeID);
	
	this.edges.splice(edgeindex,1);
	var self = this;
	var animation = {svgStrokeWidth: 2, svgFillOpacity: 0.9};
	$($("#"+edgeID), self.sketchpad.root()).addClass('TBdeleted');
	$('#'+ edgeID)[0].remove();
	/*$("#"+edgeID).animate(animation, 200, function() {
		$("#"+edgeID).removeAttr("stroke-width");
		$("#"+edgeID).removeAttr("fill-opacity");
		$($("#"+edgeID), self.sketchpad.root()).removeClass('TBdeleted');
		$('#'+ edgeID)[0].remove();
    });*/
		
	var node_ids = edgeID.split("_");
	
	
	var childnodeindex = idx_id(this.allnodes, node_ids[2]);
	this.allnodes[childnodeindex].hasParent = false;
	$($('#'+ this.allnodes[childnodeindex].id)[0], this.sketchpad.root()).removeClass('hasParent');
	
	var childEdges = idx_childedges(this.edges, node_ids[1]);
	
	if(childEdges.childnodelist.length > 0 ){
		var childnodeselected =  getparentYpos(this.allnodes , childEdges.childnodelist);
		var edgeindex = idx_id(this.edges, 'e_' + node_ids[1] + '_' + this.allnodes[childnodeselected].id);
		this.edges[edgeindex].rendered = false;
		this.createOtherConstituents(this.allnodes[childnodeselected].level);
	}
	iterativeupdate = false;
};

/**
 * Object SyntacticTree function
 * Applicable to Rendering mode : Iterative
 * Delete the node 
 * @param nodeID
 */
SyntacticTree.prototype.nodeIterativeDelete = function ( nodeID ){
	iterativeupdate = true;
	var animation = {svgStrokeWidth: 2, svgFillOpacity: 0.9};
	var index = idx_id(this.allnodes, nodeID);
	var self = this;
	this.allnodes.splice(index,1);
	$('#'+ nodeID)[0].remove();
	/*$("#"+nodeID).animate(animation, 3000, function() {
		$("#"+nodeID).removeAttr("stroke-width");
		$("#"+nodeID).removeAttr("fill-opacity");
		$($("#"+nodeID), self.sketchpad.root()).removeClass('TBdeleted');
		$('#'+ nodeID)[0].remove();
    });*/
	iterativeupdate = false;
};

/**
 * Event handler bind with mouseover event on edge
 * @param event
 */
function animateEdge(event){
	if($(event.currentTarget).attr("id")){
		if(localStorage.getItem("user") != "" && localStorage.getItem("user") != undefined ){
			var json = {action:"pos", user: localStorage.getItem("user"), element: $(event.currentTarget).attr("id"), selectedfile : localStorage.getItem("file"), show: true};
			sendMessage(JSON.stringify(json));
		}
		
	}
	var edgeGroup = event.currentTarget;
		$(edgeGroup, sketchpad.root()).addClass('hovereffect');
		setTimeout(function() {
		$(edgeGroup, sketchpad.root()).removeClass('hovereffect');
	}, 500)
}

/**
 * Event handler bind with mouseleave event on edge
 * @param event
 */
function edgeMouseleave(event){
	if($(event.currentTarget).attr("id")){
		var json = {action:"pos", user: localStorage.getItem("user"), element: $(event.currentTarget).attr("id"), selectedfile : localStorage.getItem("file"), show: false};
		sendMessage(JSON.stringify(json));
	}
}

/**
 * Event handler bind with click event on edge
 * @param event
 */
function updateEdge(event){
	var selectededge = event.currentTarget;
	var edge_id = $(selectededge).attr('id');
	$('#edge_id').val(edge_id);
	
	edge_id = edge_id.split("_");
	var childnodes_count = $("[id^=e_"+edge_id[1]+"_]").length;
	//Allow delete edge option only when more than one child edges exist
	//For node with one child the edge will be automatically deleted once parent node is deleted 
	if(childnodes_count > 1){
		$('#edge_action2_label').show();
		$('#edge_action2').show();
	}else{
		$('#edge_action2_label').hide();
		$('#edge_action2').hide();
	}
    $('#edgeActions').modal('show');
}