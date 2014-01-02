
	 window.onload = setupFunc;
	 
	 function setupFunc() {
	   document.getElementsByTagName('body')[0].onclick = clickFunc;
	   hideBusysign();
       Wicket.Ajax.registerPostCallHandler(hideBusysign);
       Wicket.Ajax.registerFailureHandler(hideBusysign);
	 }
	 
	 function hideBusysign() {
	   document.getElementById('bysy_indicator').style.display ='none';
	 }
	 
	 function showBusysign() {
	   document.getElementById('bysy_indicator').style.display ='inline';
	 }
	 
	 function clickFunc(eventData) {
	   var clickedElement = (window.event) ? event.srcElement : eventData.target;
	   if (clickedElement.tagName.toUpperCase() == 'A') {
	     showBusysign();
	   }
	 }