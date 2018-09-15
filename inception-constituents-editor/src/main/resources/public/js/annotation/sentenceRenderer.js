/* 
 * Thesis: Collaborative Web-based Tool for the Annotation of Syntactic Parse Trees
 * @author : Asha Joshi
 * Technische Universität Darmstadt 
 * This script contains methods for visualization and handling of text spans or sentence layer of the tree
 */


//Global variables
var sentenceGroup;				//the parent group of all text spans/tokens  
var current_x = 0;				//value of x position to start.		
var selectedSpan = new Span();	//The span selected on click/drag/drop events
var spanmargin = 100;			//space between text spans
var marginContainerAndTree_horiz = 50;


/**
 * Object SyntacticTree function
 * Create sentence layer
 */
SyntacticTree.prototype.createSpanLayer = function (){
	//set the x position to 100. Start from 100px from left
    current_x = spanmargin;
    
    // create a group 'sentence' for all the text spans 
    //add the group 'sentence' to main group
    sentenceGroup = this.sketchpad.group(this.maingroup,{'class': 'sentence'});
    
    //iterate over offset array and create spans for each token
    //All spans are created next to each other and positioned later
    for(var i = 0; i < this.offsetdata.length; i++) {
    	var text_start = this.offsetdata[i][0];
    	var text_end =  this.offsetdata[i][1];
    	var spanoffset = this.offsetdata[i];
    	//get the text with given offset
    	var txt = this.textdata.substring(text_start, text_end);
    	//id of text span with s_offsetstart_offsetend
    	var id = "s_" +  text_start + "_" + text_end;
    	//create span with the ID
    	var spantemp =  new Span(id, spanoffset, txt, this.maxheight());
    	spantemp.createSpanElt();
    	//add the span to List of all text spans
    	this.textspans[this.textspans.length] = spantemp;
    }
    //update the container width according to the space occupied by text spans
    //dynamically updated
    $('#svgid').css('width', current_x);
    $('#surface').css('width', current_x);
    $('#svgsketch').width(current_x);
    if(current_x > $('#content').width()  && $('#content').width() < treeContainerMaxWidth ){
    	current_x + marginContainerAndTree_horiz > treeContainerMaxWidth ? $('#content').width(treeContainerMaxWidth) : $('#content').width(current_x + marginContainerAndTree_horiz);
    }
};

/**
 * Object Span function
 * Function to create span element
 */
Span.prototype.createSpanElt = function (){
	//draw at current x position
    this.x = current_x;
    //draw text
    var textnode = sketchpad.text(sentenceGroup,this.x, this.y, this.text,{fill: "black" });
    //set id
    textnode.setAttribute("id", this.id);
    textnode.setAttribute('class', this.text);
    this.spanelt = textnode;
    
    //get the width and height of text
    var bbox = textnode.getBBox();
    this.width = bbox.width;
    this.height =  bbox.height;
    
    //update current x pos 
    //add newly created span width
    current_x += bbox.width;
    //add standard margin between text spans
    current_x += spanmargin;
    
    //adding data and bind Click event with event handler addNode()  
    $(textnode).on( "click", {span: this}, addNode );
    
    //bind mouseover event with event handler animateText() 
	$(textnode).on( "mouseover", animateText);
	$(textnode).on( "mouseleave", textMouseLeave);
};

    
/**
 *	Event handler on click
 *	@param event 
 */
function addNode(event){
    selectedSpan = event.data.span;
    //Allow only when element doesn't have POS node
    if(!selectedSpan.hasnode){
        $('#sentence_id').val(selectedSpan.id);
        //Open POS modal
        $('#addPOSmodal').modal('show');    
    }
}

/**
 * Event handler on mouse hover
 * @param event
 */
function animateText(event){
	//Send position message with updated user position
	if($(event.currentTarget).attr("id")){
		if(localStorage.getItem("user") != "" && localStorage.getItem("user") != undefined ){
			var json = {action:"pos", user: localStorage.getItem("user"), element: $(event.currentTarget).attr("id"), selectedfile : localStorage.getItem("file"), show: true};
			sendMessage(JSON.stringify(json));
		}
	}
	
	var textnode = $('text#'+event.target.id)[0];
	if(!$(textnode, sketchpad.root()).hasClass('hasnode')){
		//add animation on hover
		$(textnode, sketchpad.root()).addClass('hovereffect');
		setTimeout(function() {
			$(textnode, sketchpad.root()).removeClass('hovereffect');
	    }, 500)
	}
}

//Send position message with updated user position
function textMouseLeave(event){
	if($(event.currentTarget).attr("id")){
		var json = {action:"pos", user: localStorage.getItem("user"), element: $(event.currentTarget).attr("id"), selectedfile : localStorage.getItem("file"), show: false};
		sendMessage(JSON.stringify(json));
	}
}