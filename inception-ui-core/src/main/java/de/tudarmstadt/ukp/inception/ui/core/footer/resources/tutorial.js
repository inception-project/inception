/*
#Copyright 2019
#Ubiquitous Knowledge Processing (UKP) Lab
#Technische Universität Darmstadt
#
#Licensed under the Apache License, Version 2.0 (the "License");
#you may not use this file except in compliance with the License.
#You may obtain a copy of the License at
# 
# http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#See the License for the specific language governing permissions and
#limitations under the License.
*/
$(document).ready(function() {
			var enjoyhint_instance = new EnjoyHint({
				onSkip : function() {
					// reset all the cookies
				}
			});

			var enjoyhint_script_steps;
			var currentPage = window.location.pathname;
			var projectId = getUrlParameter('p');
			
			var cName = document.location.pathname.match(/[^\/]+$/)[0];
			//createCookieName(currentPage);
			var ps = getCookie(cName); 

			if (currentPage.includes("projects.html")) {
				ps = getCookie(cName);
				if (ps == 'true') {
				} else if (ps == "") {
					enjoyhint_instance = new EnjoyHint({
						onEnd : function() {
							setCookie(cName, 'projectCreated');
						},
						onSkip : function() {
							setCookie(cName, true);
						}
					});

					enjoyhint_script_steps = createFirstPageRoutine();
					enjoyhint_instance.set(enjoyhint_script_steps);
					enjoyhint_instance.runScript();
				} else if (ps == "projectCreated") {
					enjoyhint_instance = new EnjoyHint({
						onEnd : function() {
							setCookie(cName, true);
						},
						onSkip : function() {
							setCookie(cName, true);
						}
					});
					enjoyhint_script_steps = createFirstPageRoutinePart2();
					enjoyhint_instance.set(enjoyhint_script_steps);
					enjoyhint_instance.runScript();

				}
			} else if (currentPage.includes("projectsetting.html") 
					&& projectId == 'NEW' && ps != "projectSaved") {
				enjoyhint_instance = new EnjoyHint({
					onEnd : function() {
						// reset all the cookies
						setCookie(cName, 'projectSaved');
					},
					onSkip : function() {
						setCookie(cName, true);
					}

				});

				ps = getCookie(cName);
				if (ps == 'true') {
				} else {
					ps = 'projectSaved';
					if (ps != "" && ps != null) {
						setCookie(cName, ps, 365);
					}

					enjoyhint_script_steps = createNewProjectRoutine();
					enjoyhint_instance.set(enjoyhint_script_steps);
					enjoyhint_instance.runScript();
				}
			} else if (currentPage.includes("projectsetting.html")
					&& projectId == 'NEW' && ps == "projectSaved") {

				//save the project name in the cookie
				var projectName = $('[name="p::name"]').val()
				setCookie("projectName", projectName, 365);
				
				//re-initialize the instance
				enjoyhint_instance = new EnjoyHint({});

				//reset the projectsetting cookie so the tutorial shows up on the settings page later
				ps = "";
				if (ps != "" && ps != null) {
					setCookie(cName, ps, 365);
				}
				enjoyhint_script_steps = createProjectSavedRoutine();
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();

			} else if (currentPage.includes("project.html")) {
				var ps = getCookie(cName);
				if (ps != "") {
				} else {
					ps = true;
					if (ps != "" && ps != null) {
						setCookie(cName, ps, 365);
					}

					enjoyhint_script_steps = createDashboardRoutine();
					enjoyhint_instance.set(enjoyhint_script_steps);
					enjoyhint_instance.runScript();
				}
			} else if (currentPage.includes("projectsetting.html")) {
				var ps = getCookie(cName);
				
				if (ps == 'true') {
					// alert("PS Visited? " + ps);
				} 
				//TODO do we need it
				else if (ps == "recommenderSaved") {
					ps = true;
					if (ps != "" && ps != null) {
						setCookie(cName, ps, 365);
					}
					enjoyhint_script_steps = createAnnotationRoutine();
					enjoyhint_instance.set(enjoyhint_script_steps);
					enjoyhint_instance.runScript();
				}
				else {
					enjoyhint_instance = new EnjoyHint({     
						
						onEnd : function() {
							ps = "true";
							if (ps != "" && ps != null) {
								setCookie(cName, ps, 365);
							}
						},
						onSkip : function() {
							setCookie(cName, true);
						}

					});

					enjoyhint_script_steps = createSettingsRoutine(enjoyhint_instance);
					enjoyhint_instance.set(enjoyhint_script_steps);
					enjoyhint_instance.runScript();
				}
			}
		});



function setCookie(cname, cvalue, exdays) {
	var d = new Date();
	d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
	var expires = "expires=" + d.toUTCString();
	document.cookie = cname + "=" + cvalue + ";" + expires + ";path=/";
}

function getCookie(cname) {
	var name = cname + "=";
	var ca = document.cookie.split(';');
	for (var i = 0; i < ca.length; i++) {
		var c = ca[i];
		while (c.charAt(0) == ' ') {
			c = c.substring(1);
		}
		if (c.indexOf(name) == 0) {
			return c.substring(name.length, c.length);
		}
	}
	return "";
}

function createNewProjectRoutine() {
	var a = [ {
		'key [name=\'p::name\']' : 'Write the name of the project and press Enter',
	}
//	, {
//		'click [type=submit]' : 'Click save'
//	}

	];
	return a;
}

function createFirstPageRoutine() {
	var a = [
			{
				'next .navbar-brand' : 'Welcome to INCEpTION! Let me guide you through its features. At any time during the tutorial you can skip and exit the tour',
				'nextButton' : {
					className : "myNext",
					text : "Sure"
				},
				'skipButton' : {
					className : "mySkip",
					text : "Skip"
				}
			},
			{
				'click .btn btn-primary' : "Click here to create a new project",
			} ];
	return a;
}

function createFirstPageRoutinePart2() {
	var projectName = getCookie("projectName");
	var selector = "a:contains('"+projectName+"'):first";
	
	var t = $('a').filter(function() {
		 return $(this).text() == projectName;
    });
	
	var a = [

			{
				'next .file-input-new' : "The projects can also be imported",
			},
			{
				'next .input-group:last' : "You can filter the projects based on the ownership type",
			},
			{
				"event": "click", 
				"selector": t[0], 
				"description": "Click on the project to get started.",
				

			} 
			];
	return a;
}

function createDashboardRoutine() {
	var a = [
		{
			'next li:first-of-type' : 'Here, you can annotate your documents',
		},
		{
			'next li:nth-of-type(2)' : 'Completely annotated documents can be curated to form the  final result documents.',
		},
		{
			'next li:nth-of-type(3)' : 'This will show you the agreement between annotators across  documents.',
		},
		{
			'next li:nth-of-type(4)' : 'Here, you can see the annotators\' progress and assign documents.',
		},
		{
			'next li:nth-of-type(5)' : 'This allows you to evaluate your recommenders.',
		},
			{
				'click li:nth-last-child(1)' : "Before getting started, lets configure the project. Please click Settings"
			} ];
	return a;
}

function createSettingsRoutine(enjoyHint) {
	var a = [
			{
				'click .tab2' : 'Click here to add a document to the project',
			},
			{
				'next [class=flex-h-container]' : "Upload a file and import. Then click Next.",
			}, {
				'click .tab5' : "Now, lets add a recommender. Click here!",
			}, {
				'click [value=Create]' : "Click to create a new recommender",
			},
			 {
				'next .form-group:nth(2)' : "Select a Layer", 
				onBeforeStart:function(){ 
				       $('[name=layer]').on('change', function() {
						  enjoyHint.trigger('next'); 
				    	});
					}
			},
			 {
				'next .form-group:nth(3)' : "Select a Feature", 
				onBeforeStart:function(){ 
				       $('[name=feature]').on('change', function() {
							  enjoyHint.trigger('next'); 
				    	});
					}
			},
			 {
				'next .form-group:nth(4)' : "Select a Tool", 
				onBeforeStart:function(){ 
				       $('[name=tool]').on('change', function() {
							enjoyHint.trigger('next'); 
				    	});
				},
			},

			{
				"event_type": "custom", 
				"event": "event-save-recommender", 
				"selector": "[method=post]", 
				"description": "Fill in the details and click save", 
				onBeforeStart:function(){ 
				     $("[name=\'save\']").on('click',
								function(e) { 
									enjoyHint.trigger('next'); 
								});
				     
				}, 
		     

			}, 
			{
				'click [href=\'./project.html\']:last' : 'Now, lets go to the Dashboard',
			}
			];

	return a;
}

function createSettingsRoutine2() {
	var a = [
			{
				"event_type": "custom", 
				"event": "event-save-recommender", 
				"selector": "[method=post]", 
				"description": "Fill in the details and click save", 
			}, 
			{
				'click [href=\'./project.html\']:last' : 'Now, lets go to the Dashboard',
			}
			];

	return a;
}
function createProjectSavedRoutine() {
	var a = [ {
		'click .navbar-link' : 'Click here to go back to the projects page'
	} ];

	return a;
}

function createAnnotationRoutine() {
	var a = [
			{
				'click [href=\'./project.html\']:last' : 'Now, lets go to go to the Dashboard',
			} ];

	return a;
}

function createCookieName(currentPage) {
	return currentPage.substr(1);
}

function getUrlParameter(sParam) {
	var sPageURL = window.location.search.substring(1), sURLVariables = sPageURL
			.split('&'), sParameterName, i;

	for (i = 0; i < sURLVariables.length; i++) {
		sParameterName = sURLVariables[i].split('=');

		if (sParameterName[0] === sParam) {
			return sParameterName[1] === undefined ? true
					: decodeURIComponent(sParameterName[1]);
		}
	}
};


