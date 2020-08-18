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

var enjoyhint_instance = new EnjoyHint({});
var contextPath;
const tabObserver = new MutationObserver(runActiveTabsRoutines);
	

function runRoutines() {
			var enjoyhint_script_steps;
			var currentPage = window.location.pathname;
			var projectId = getUrlParameter('p');
			
			var ps = getCookie(cName); 
			
			//don't do all the stuff if we're not in the tutorial
			if (ps == ""){ 
				return;
			}
			
			// if the tutorial ended, remove cookies
			if (ps == "ended"){
				deleteCookies();
				tabObserver.disconnect();
			}
			
			// observe tabcontainer on settings page for changes in its tabs
			if (currentPage.includes("projectsetting.html") && 
					document.querySelectorAll("[name^='tabContainer']").length > 0){
				tabObserver.observe(document.querySelectorAll("[name^='tabContainer']")[0], 
						{childList : true, subtree : true});
			}
			
			if (currentPage.includes("projects.html") && ps == "tutorialStarted")
			{
				enjoyhint_instance = new EnjoyHint({
					onEnd : function() {
						setCookie(cName, 'tutorialStarted');
					},
					onSkip : function() {
						setCookie(cName, 'ended');
					}
				});

				enjoyhint_script_steps = createFirstPageRoutine();
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			} 
			
			else if (currentPage.includes("projectsetting.html")  && projectId == 'NEW' && ps == "tutorialStarted") 
			{
				enjoyhint_instance = new EnjoyHint({
					onSkip : function() {
						setCookie(cName, 'ended');
					}
				});

				setCookie(cName, 'projectSaved');
				enjoyhint_script_steps = createNewProjectRoutine();
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			} 
			
			else if (currentPage.includes("projectsetting.html") && projectId == 'NEW' && ps == "projectSaved") 
			{
				//save the project name in the cookie
				var projectName = $('[name="p::name"]').val()
				setCookie(projectCName, projectName);
				
				//re-initialize the instance
				enjoyhint_instance = new EnjoyHint({
					onEnd : function() {
						setCookie(cName, 'projectCreated');
					},
					onSkip : function() {
						setCookie(cName, 'ended');
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
							setCookie(cName, "projectView");
						},
						onSkip : function() {
							setCookie(cName, 'ended');
						}
					});
					// disable the buttons that should not be clicked during the tutorial
					if (document.querySelectorAll("[name^='projectImport']").length > 0){
						document.querySelectorAll("[name^='projectImport']")[0].disabled = true;
					}
					document.querySelectorAll("[id^='roleFilterLink']")
						.forEach(element => element.disabled = true);
					// run routine
					enjoyhint_script_steps = createFirstPageRoutinePart2();
					enjoyhint_instance.set(enjoyhint_script_steps);
					enjoyhint_instance.runScript();
			}
			
			
			else if (!currentPage.includes("projectsetting.html") && ps == "projectView") 
			{
				enjoyhint_instance = new EnjoyHint({
					onEnd : function() {
						setCookie(cName, "projectsettingView");
					},
					onSkip : function() {
						setCookie(cName, 'ended');
					}
				});
				
					enjoyhint_script_steps = createDashboardRoutine();
					enjoyhint_instance.set(enjoyhint_script_steps);
					enjoyhint_instance.runScript();
			} 
			
			// this is kind of an ugly work around for instances with context path root, 
			// setting cookie contextpath to /project on dashboard, see #1668
			else if (currentPage.includes("projectsetting.html") && 
					(ps == "projectsettingView" || ps == "projectView")) {
				
				enjoyhint_instance = new EnjoyHint({     
					
					onEnd : function() {
						setCookie(cName, "projectsettingsOpenedDoc");
					},
					onSkip : function() {
						setCookie(cName, 'ended');
					}

				});

				enjoyhint_script_steps = createOpenDocumentsRoutine(enjoyhint_instance);
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			}
			
			else if (currentPage.includes("projectsetting.html") && ps == "projectsettingsDocUploaded") {
				
				enjoyhint_instance = new EnjoyHint({     
					
					onEnd : function() {
						setCookie(cName, "projectsettingsOpenedRecommenders");
					},
					onSkip : function() {
						setCookie(cName, 'ended');
					}

				});
				enjoyhint_script_steps = createOpenRecommendersRoutine(enjoyhint_instance);
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			}
			
			else if (currentPage.includes("projectsetting.html") && ps == "projectsettingsCreatedRecommender") {
				
				enjoyhint_instance = new EnjoyHint({     
					
					onEnd : function() {
						setCookie(cName, "projectsettingsConfigured");
					},
					onSkip : function() {
						setCookie(cName, 'ended');
					}

				});

				enjoyhint_script_steps = createRecommenderSettingsRoutine(enjoyhint_instance);
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			}
			
			else if (ps == "projectsettingsConfigured")
			{
				enjoyhint_instance = new EnjoyHint({
					onEnd : function() {
						setCookie(cName, "farewell");
						location.reload(); //necessary to enable links again
					},
					onSkip : function() {
						setCookie(cName, 'ended');
					}
				});
				
				// disable buttons
				let dashboardLinks = document.getElementsByClassName("hvr-fade");
				for (i=0; i<dashboardLinks.length; i++){
					dashboardLinks[i].style.pointerEvents = 'none';
				}
				// start routine
				enjoyhint_script_steps = createDashboardRoutine2();
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			}
			
			else if (ps == "farewell")
			{
				enjoyhint_instance = new EnjoyHint({
					onEnd : function() {
						setCookie(cName, "ended");
						
					},
					onSkip : function() {
						setCookie(cName, 'ended');
					}
				});
				
				enjoyhint_script_steps = createLastRoutine();
				enjoyhint_instance.set(enjoyhint_script_steps);
				enjoyhint_instance.runScript();
			}
};

function startTutorial() {
	var enjoyhint_instance = new EnjoyHint({
		onEnd : function() {
		},
		onSkip : function() {
			setCookie(cName, 'ended');
		}
	});

	enjoyhint_script_steps = createFirstPageRoutine();
	enjoyhint_instance.set(enjoyhint_script_steps);
	
	setCookie(cName, 'tutorialStarted');

	enjoyhint_instance.runScript();
}

function setContextPath(aContextPath){
	contextPath = aContextPath;
}

// run routines on active tabs
function runActiveTabsRoutines(){
	
	//don't do all the stuff if we're not in the tutorial
	if (getCookie(cName) == ""){ 
		return;
	}
	
	if ($('.tab1.active').length > 0){
		if (getCookie(cName) == 'projectsettingsOpenedDoc'){
			enjoyhint_instance = new EnjoyHint({     
					
				onEnd : function() {
					setCookie(cName, "projectsettingsDocUploaded");
					location.reload();
				},
				onSkip : function() {
					setCookie(cName, 'ended');
				}
			});

			enjoyhint_script_steps = createAddDocumentRoutine(enjoyhint_instance);
			enjoyhint_instance.set(enjoyhint_script_steps);
			enjoyhint_instance.runScript()
		}
	}
	
	if ($('.tab5.active').length > 0){
		if (getCookie(cName) == 'projectsettingsOpenedRecommenders'){
			enjoyhint_instance = new EnjoyHint({     
				
				onEnd : function() {
					setCookie(cName, "projectsettingsCreatedRecommender");
					location.reload();
				},
				onSkip : function() {
					setCookie(cName, 'ended');
				}

			});

			enjoyhint_script_steps = createAddRecommenderRoutine(enjoyhint_instance);
			enjoyhint_instance.set(enjoyhint_script_steps);
			enjoyhint_instance.runScript();
		}
	}
}

function deleteCookies(){
	var expires = "expires=" + (new Date(0)).toUTCString();
	setCookieWithExpires(cName, "", expires);
	setCookieWithExpires(projectCName, "", expires);
}

function setCookie(cname, cvalue) {
	var d = new Date();
	d.setTime(d.getTime() + (1 * 24 * 60 * 60 * 1000));
	var expires = "expires=" + d.toUTCString();
	setCookieWithExpires(cname, cvalue, expires);
}

function setCookieWithExpires(cname, cvalue, expires) {
	document.cookie = cname + "=" + cvalue + ";" + expires + ";path="+contextPath;
}

// document.cookie should get the cookies from the current document location, should
// find the right cookie for the right path?
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
		'showSkip': false
	} ];
	return a;
}

function createFirstPageRoutine() {
	var a = [
			{
				'next .navbar-brand' : "Welcome to INCEpTION! Let me guide you through its features. At any time during the tutorial you can click 'x' to quit the tour.",
				'nextButton' : {
					className : "myNext",
					text : "Sure"
				},
				'showSkip': false
			},
			{
				'click .btn btn-primary' : "Click here to create a new project.", 'showSkip': false
			} 
			];
	return a;
}

function createFirstPageRoutinePart2() {
	var projectName = getCookie("projectName");
	var selector = "a:contains('"+projectName+"'):first";
	
	var a = [
			{
				'next [name=projectImport]' : "Instead of creating a new project, existing projects can also be imported.", 'showSkip': false
			},
			{
				'next .input-group:last' : "You can filter the projects based on your role in them.",
				'showSkip': false
			},
			{
				onBeforeStart:function(){ 
					$('.scrolling').scrollTo(selector, 80);

				},
				"event": "click", 
				"selector": selector,
				"description": "Click on the project to get started.",
				'showSkip': false
			} 
			];
	return a;
}

function createDashboardRoutine() {
	var a = [
			{
				'click li:nth-last-child(1)' : "Before getting started, let's configure the project. Please click 'Settings'.",
				'showSkip': false
			} ];
	return a;
}

function createOpenDocumentsRoutine(enjoyHint) {
	var a = [
		    {
		    	'click .tab1' : 'Click here to add a document to the project.',
		    	'showSkip': false
		    }
			];

	return a;
}

function createAddDocumentRoutine(enjoyHint) {
	var a = [
			{
				'next [class=flex-h-container]' : "Upload a document (e.g. a .txt file) using the button next to 'Select Files', " +
						"choose a format (e.g. 'Plain Text') and click 'Import'. Then click 'Next'.",
						'showSkip': false
			}
			];

	return a;
}

function createOpenRecommendersRoutine(enjoyHint) {
	var a = [
			{
				'click .tab5' : 'Now, lets add a recommender. Click here!',
				'showSkip': false
		    }
			];

	return a;
}

function createAddRecommenderRoutine(enjoyHint) {
	var a = [
			{
				'click .btn-primary' : "Click here to create a new recommender that will give you suggestions while annotating.",
				'showSkip': false
			}
			];
	
	return a;
}

function createRecommenderSettingsRoutine(enjoyHint) {
	var a = [
			{
				'next .form-group:nth(3)' : "Select a Layer (the enclosing annotation e.g. 'Named entity').",
				'showSkip': false
			},
			{
				'next .form-group:nth(4)' : "Select a Feature (the annotation's attribute which should be predicted e.g. 'value').",
				'showSkip': false
			},
			{
				'next .form-group:nth(5)' : "Select a Tool which is used to produce the suggestions e.g. 'Stringmatcher'.",
				'showSkip': false
			},
			{
				'next [name=save]' : "Click 'Save'.",
				'showSkip': false
			},
			{
				'click [href=\'.\']:last' : "Now, let's go back to the Dashboard.",
				'showSkip': false
			} 
			];

	return a;
}

function createProjectSavedRoutine() {
	var a = [ {
		'click .nav-item:first-of-type' : 'Click here to go back to the projects overview page.',
		'showSkip': false
	} ];

	return a;
}

function createLastRoutine() {
	var a = [
		{
			'event .flex-sidebar' : 'Now, feel free to explore INCEpTION on your own.',
			
			'skipButton' : {
				className : "mySkip",
				text : "Thanks!"
			}
		} 
		];

	return a;
}

function createDashboardRoutine2() {
	var a = [
		{
			'next li:first-of-type' : "Finally, some information on the Dashboard's buttons: Here, you can annotate your documents.",
			'showSkip': false
		},
		{
			'next li:nth-of-type(2)' : 'Completely annotated documents can be curated to form the final result documents.',
			'showSkip': false
		},
		{
			'next li:nth-of-type(3)' : 'This will show you the agreement between annotators across documents.',
			'showSkip': false
		},
		{
			'next li:nth-of-type(4)' : 'Here, you can see the annotators\' progress and assign documents.',
			'showSkip': false
		},
		{
			'next li:nth-of-type(5)' : 'This allows you to evaluate your recommenders.',
			'showSkip': false
		} 
		];

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
