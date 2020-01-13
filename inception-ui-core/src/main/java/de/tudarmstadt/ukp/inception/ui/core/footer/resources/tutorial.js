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

var cName = "tutorialSession";
var projectCName = "projectName";
var contextPath = "";

var enjoyhint_instance = new EnjoyHint({});
	
$(document).ready(function() {
			var enjoyhint_script_steps;
			var currentPage = window.location.pathname;
			var projectId = getUrlParameter('p');
			
			var ps = getCookie(cName); 
			
			if (currentPage.includes("projects.html") && ps == "tutorialStarted")
			{
				enjoyhint_instance = new EnjoyHint({
					onEnd : function() {
						setCookie(cName, 'tutorialStarted', contextPath);
					},
					onSkip : function() {
						setCookie(cName, 'ended', contextPath);
					}
				});

				enjoyhint_script_steps = createFirstPageRoutine();
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			} 
			
			else if (ps == "ended"){
				deleteCookie(cName);
				deleteCookie(projectCName);
			}
			
			else if (currentPage.includes("projectsetting.html")  && projectId == 'NEW' && ps == "tutorialStarted") 
			{
				enjoyhint_instance = new EnjoyHint({
					onSkip : function() {
						setCookie(cName, 'ended', contextPath);
					}
				});

				setCookie(cName, 'projectSaved', contextPath);
				enjoyhint_script_steps = createNewProjectRoutine();
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			} 
			
			else if (currentPage.includes("projectsetting.html") && projectId == 'NEW' && ps == "projectSaved") 
			{
				//save the project name in the cookie
				var projectName = $('[name="p::name"]').val()
				setCookie(projectCName, projectName, contextPath);
				
				//re-initialize the instance
				enjoyhint_instance = new EnjoyHint({
					onEnd : function() {
						setCookie(cName, 'projectCreated',contextPath);
					},
					onSkip : function() {
						setCookie(cName, 'ended', contextPath);
					}
				});

				enjoyhint_script_steps = createProjectSavedRoutine();
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			} 
			
			else if (currentPage.includes("projects.html") && ps == "projectCreated") 
			{
					enjoyhint_instance = new EnjoyHint({
						onEnd : function() {
							setCookie(cName, "projectView", contextPath);
						},
						onSkip : function() {
							setCookie(cName, 'ended', contextPath);
						}
					});
					
					enjoyhint_script_steps = createFirstPageRoutinePart2();
					enjoyhint_instance.set(enjoyhint_script_steps);
					enjoyhint_instance.runScript();
			}
			
			
			else if (currentPage.includes("project.html") && ps == "projectView") 
			{
				enjoyhint_instance = new EnjoyHint({
					onEnd : function() {
						setCookie(cName, "projectsettingView", contextPath);
					},
					onSkip : function() {
						setCookie(cName, 'ended', contextPath);
					}
				});
				
					enjoyhint_script_steps = createDashboardRoutine();
					enjoyhint_instance.set(enjoyhint_script_steps);
					enjoyhint_instance.runScript();
			} 
			
			else if (currentPage.includes("projectsetting.html") && ps == "projectsettingView") {
				
				enjoyhint_instance = new EnjoyHint({     
					
					onEnd : function() {
						setCookie(cName, "projectsettingsOpenedDoc", contextPath);
						location.reload();
					},
					onSkip : function() {
						setCookie(cName, 'ended', contextPath);
					}

				});

				enjoyhint_script_steps = createOpenDocumentsRoutine(enjoyhint_instance);
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			}
			
			else if (currentPage.includes("projectsetting.html") && ps == "projectsettingsOpenedDoc") {
				
					enjoyhint_instance = new EnjoyHint({     
						
						onEnd : function() {
							setCookie(cName, "projectsettingsDocUploaded", contextPath);
							location.reload();
						},
						onSkip : function() {
							setCookie(cName, 'ended', contextPath);
						}

					});

					enjoyhint_script_steps = createAddDocumentRoutine(enjoyhint_instance);
					enjoyhint_instance.set(enjoyhint_script_steps);
					enjoyhint_instance.runScript();
			}
			
			else if (currentPage.includes("projectsetting.html") && ps == "projectsettingsDocUploaded") {
				
				enjoyhint_instance = new EnjoyHint({     
					
					onEnd : function() {
						setCookie(cName, "projectsettingsOpenedRecommenders", contextPath);
						location.reload();
					},
					onSkip : function() {
						setCookie(cName, 'ended', contextPath);
					}

				});

				enjoyhint_script_steps = createOpenRecommendersRoutine(enjoyhint_instance);
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			}
			
			else if (currentPage.includes("projectsetting.html") && ps == "projectsettingsOpenedRecommenders") {
				
				enjoyhint_instance = new EnjoyHint({     
					
					onEnd : function() {
						setCookie(cName, "projectsettingsCreatedRecommender", contextPath);
						location.reload();
					},
					onSkip : function() {
						setCookie(cName, 'ended', contextPath);
					}

				});

				enjoyhint_script_steps = createAddRecommenderRoutine(enjoyhint_instance);
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			}
			
			else if (currentPage.includes("projectsetting.html") && ps == "projectsettingsCreatedRecommender") {
				
				enjoyhint_instance = new EnjoyHint({     
					
					onEnd : function() {
						setCookie(cName, "projectsettingsConfigured", contextPath);
					},
					onSkip : function() {
						setCookie(cName, 'ended', contextPath);
					}

				});

				enjoyhint_script_steps = createRecommenderSettingsRoutine(enjoyhint_instance);
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			}
			
			else if (currentPage.includes("project.html") && ps == "projectsettingsConfigured")
			{
				enjoyhint_instance = new EnjoyHint({
					onEnd : function() {
						setCookie(cName, "ended", contextPath);
					},
					onSkip : function() {
						setCookie(cName, 'ended', contextPath);
					}
				});
				enjoyhint_script_steps = createLastRoutine();
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			}
		});

function startTutorial(aContextPath) {
	contextPath = aContextPath;
	var enjoyhint_instance = new EnjoyHint({
		onEnd : function() {
		},
		onSkip : function() {
			setCookie(cName, 'ended', aContextPath);
		}
	});

	enjoyhint_script_steps = createFirstPageRoutine();
	enjoyhint_instance.set(enjoyhint_script_steps);
	
	setCookie(cName, 'tutorialStarted', aContextPath);

	enjoyhint_instance.runScript();
}

function deleteCookie(cname){
	document.cookie = cname + "=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
}

function setCookie(cname, cvalue, contextPath) {
	var d = new Date();
	d.setTime(d.getTime() + (1 * 24 * 60 * 60 * 1000));
	var expires = "expires=" + d.toUTCString();
	document.cookie = cname + "=" + cvalue + ";" + expires + ";path="+contextPath;
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
		'event [name=\'p::name\']' : 'Write a name for your project and press Enter.',
	} ];
	return a;
}

function createFirstPageRoutine() {
	var a = [
			{
				'next .navbar-brand' : 'Welcome to INCEpTION! Let me guide you through its features. At any time during the tutorial you can click skip to exit the tour.',
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
				'click .btn btn-primary' : "Click here to create a new project.",
			} 
			];
	return a;
}

function createFirstPageRoutinePart2() {
	var projectName = getCookie("projectName");
	var selector = "a:contains('"+projectName+"'):first";
	
	var a = [

			{
				'next .file-input-new' : "The projects can also be imported.",
			},
			{
				'next .input-group:last' : "You can filter the projects based on your role in them.",
			},
			{
				onBeforeStart:function(){ 
					$('.scrolling').scrollTo(selector, 80);

				},
				"event": "click", 
				"selector": selector,
				"description": "Click on the project to get started.",
			} 
			];
	return a;
}

function createDashboardRoutine() {
	var a = [
			{
				'next li:first-of-type' : 'Here, you can annotate your documents.',
			},
			{
				'next li:nth-of-type(2)' : 'Completely annotated documents can be curated to form the final result documents.',
			},
			{
				'next li:nth-of-type(3)' : 'This will show you the agreement between annotators across documents.',
			},
			{
				'next li:nth-of-type(4)' : 'Here, you can see the annotators\' progress and assign documents.',
			},
			{
				'next li:nth-of-type(5)' : 'This allows you to evaluate your recommenders.',
			},
			{
				'click li:nth-last-child(1)' : "Before getting started, let's configure the project. Please click 'Settings'."
			} ];
	return a;
}

function createOpenDocumentsRoutine(enjoyHint) {
	var a = [
		    {
		    	"event": "click", 
				"selector": $("span:contains('Documents')"),
				"description": "Click here to add a document to the project.",
		    }
			];

	return a;
}

function createAddDocumentRoutine(enjoyHint) {
	var a = [
			{
				'next [class=flex-h-container]' : "Upload a file and import. Then click 'Next'.",
			}
			];

	return a;
}

function createOpenRecommendersRoutine(enjoyHint) {
	var a = [
			{
		    	"event": "click", 
				"selector": $("span:contains('Recommenders')"),
				"description": "Now, lets add a recommender. Click here!",
		    }
			];

	return a;
}

function createAddRecommenderRoutine(enjoyHint) {
	var a = [
			{
				'click [value=Create]' : "Click here to create a new recommender that will give you suggestions while annotating.",
			}
			];
	
	return a;
}

function createRecommenderSettingsRoutine(enjoyHint) {
	var a = [
			{
				'next .form-group:nth(2)' : "Select a Layer (the enclosing annotation e.g. 'Named entity').",
			},
			{
				'next .form-group:nth(3)' : "Select a Feature (the annotation's attribute that should be predicted e.g. 'value').",
			},
			{
				'next .form-group:nth(4)' : "Select a Tool that is used to produce the suggestions e.g. 'Stringmatcher'.",
			},
			{
				"next [value=Save]" : "Click 'Save'.",
			},
			{
				'click [href=\'./project.html\']:last' : "Now, let's go to the Dashboard.",
			} 
			];

	return a;
}

function goBackToDashboardRoutine()
{
	var enjoyhint_instance = new EnjoyHint({     
		onEnd : function() {
			setCookie(cName, "projectsettingConfigured", contextPath);
		},
		onSkip : function() {
			setCookie(cName, true, contextPath);
		}
	});
 
	enjoyhint_instance.set(
	[
		{
			'click [href=\'./project.html\']:last' : 'Now, lets go to the Dashboard.',
		}
	]		
	);
	enjoyhint_instance.runScript();}

function createProjectSavedRoutine() {
	var a = [ {
		'click .navbar-link' : 'Click here to go back to the projects page.'
	} ];

	return a;
}

function createLastRoutine() {
	var a = [
			{
				'click .flex-sidebar' : 'You can start exploring the website further.',
				
				'skipButton' : {
					className : "mySkip",
					text : "Thanks!"
				}
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
