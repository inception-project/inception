/*
 * Thesis: Collaborative Web-based Tool for Annotation of Syntactic Parse Trees
 * @author : Asha Joshi
 * Technische Universität Darmstadt 
 * This script contains all the objects used in the visualization component.
 */

//Global variables
var sketchpad = null; // maintain reference to the svg object
var treeContainerMaxWidth = 1200;
var treeContainerMaxHeight = 750;
Date.prototype.getUnixTime = function() { return this.getTime()/1000|0 };

/**
 * Object RenderView
 * Manages the rendering and re-rendering of the syntax tree 
 * text: the text received from backend 
 * nodes: the array of nodes received from backend 
 * edges: the array of edges received from backend 
 * treeobj: the SyntacticTree object 
 * maingroup: the maingroup which contains all the svg elements related to the syntactic tree
 */
var RenderView = function() {
	this.sketchpad = null;
	this.text = "";
	this.nodes = [];
	this.offset = [];
	this.edges = [];
	this.version;
	this.treeobj;
	this.maingroup = null;
	this.latency = "";
}

var renderview = new RenderView(); // the object of RenderView()

/**
 * Object SyntacticTree : Syntactic Tree
 * Maintains the details of all Constituents nodes and edges 
 * allnodes: 			all nodes POS and Constituent of Object Node type 
 * nodesdata: 			nodes received from server
 * edgesdata: 			edges received from server 
 * edges: 				array of edges of type Object Edge 
 * constituentnodes: 	array of all constituent nodes of type Object Node
 */
var SyntacticTree = function(sketchpad, text, offset, nodesdata, edgesdata, group) {
	this.sketchpad = sketchpad;
	this.svg = sketchpad._svg;
	this.maingroup = group;
	this.textdata = text;
	this.offsetdata = offset;
	this.nodesdata = nodesdata;
	this.edgesdata = edgesdata;
	this.textspans = [];
	this.allnodes = [];
	this.edges = [];
	this.maxheight = function() {
		var rect = this.svg.getBoundingClientRect();
		return rect.height;
	};
}

/**
 * Object Span : Text spans 
 * id : 		the id of span "s_offsetstart_offsetend" 
 * width: 		the width of span height: the height of span text 
 * hasnode : 	Boolean if token has POS node 
 * offset: 		Store offset 
 * x: 			X pos 
 * y: 			Y pos 
 * posupdate: 	if Span re-positioned
 */
var Span = function(id, offset, text, height) {
	this.id = id;
	this.width = 0;
	this.height = 0;
	this.text = text;
	this.hasnode = false;
	this.offset = offset;
	this.x = 0;
	this.y = height - 40;
	this.posupdate = false;
};

/**
 * Object Node for POS and Constituent Nodes 
 * id: 				id of the node 
 * text: 			node text
 * offset: 			applicable for POS nodes 
 * color: 			color of node type: x: X pos y: Y pos
 * width: 			node width 
 * height: 			node height 
 * isPos: 			Boolean node is POS or Constituent 
 * nodecreated: 	If node is rendered
 * hasParent: 		if node has parent level: at which layer node created
 * level:			The level at which node is positioned
 */
var Node = function(datatext, offset, name, isPOS) {
	this.id = name;
	this.text = datatext;
	this.offset = offset;
	this.x = 0;
	this.y = 0;
	this.width = 0;
	this.height = 0;
	this.isPOS = isPOS;
	this.nodecreated = false;
	this.hasParent = false;
	this.level = 0;
}

/**
 * Object Edge for maintaining and rendering edges received from server 
 * id: 				id of edge e_addresParent_addressChild 
 * text: 			Edge syntactic function 
 * from: 			Child node address 
 * to: 				Parent node address 
 * rendered:		If edge is rendered
 */
var Edge = function(id, label, from, to) {
	this.id = id;
	this.text = label;
	this.from = from;
	this.to = to;
	this.text_x = 0;
	this.rendered = false;

}

/**
 * Object Edgechildlist 
 * edgelist 			the child edges of parent node 
 * childnodelist		the child nodes IDs of parent node
 */
var Edgechildlist = function(list1, list2) {
	this.edgelist = list1;
	this.childnodelist = list2;
}
