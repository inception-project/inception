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

$(document).ready(
		function() {

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
			debugger;

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
					&& projectId == 'NEW' && ps != "redirect") {

				enjoyhint_instance = new EnjoyHint({
					onEnd : function() {
						// reset all the cookies
						setCookie(cName, 'redirect');
					},
					onSkip : function() {
						setCookie(cName, true);
					}

				});

				ps = getCookie(cName);
				if (ps == 'true') {
				} else {
					ps = 'redirect';
					if (ps != "" && ps != null) {
						setCookie(cName, ps, 365);
					}

					enjoyhint_script_steps = createNewProjectRoutine();
					enjoyhint_instance.set(enjoyhint_script_steps);
					enjoyhint_instance.runScript();
				}
			} else if (currentPage.includes("projectsetting.html")
					&& projectId == 'NEW' && ps == "redirect") {
				enjoyhint_instance = new EnjoyHint({});

				ps = true;
				if (ps != "" && ps != null) {
					setCookie(cName, ps, 365);
				}
				enjoyhint_script_steps = createRedirectRoutine();
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
				
				debugger;

				if (ps == 'true') {
					// alert("PS Visited? " + ps);
				} 
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
							ps = "recommenderSaved";
							if (ps != "" && ps != null) {
								setCookie(cName, ps, 365);
							}
						},
						onSkip : function() {
							setCookie(cName, true);
						}

					});

					enjoyhint_script_steps = createSettingsRoutine();
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
		'change [name=\'p::name\']' : 'Enter the name of the project and click next',
		'nextButton' : {
			className : "myNext",
			text : "Next"
		},
		'skipButton' : {
			className : "mySkip",
			text : "Nope!"
		}
	}, {
		'click [type=submit]' : 'Click save'
	}

	];
	return a;
}

function createFirstPageRoutine() {
	var a = [
			{
				'next .navbar-brand' : 'Welcome to INCEpTION! Let me guide you through its features.',
				'nextButton' : {
					className : "myNext",
					text : "Sure"
				},
				'skipButton' : {
					className : "mySkip",
					text : "Nope!"
				}
			},
			{
				'click .btn btn-primary' : "Click here to create a new project",
			} ];
	return a;
}

function createFirstPageRoutinePart2() {
	var a = [

			{
				'next .file-input-new' : "The projects can also be imported",
			},
			{
				'next .input-group:last' : "You can filter the projects based on the ownership type",
			},
			{
				'click .list-group-item' : "Click on the project to get started.",

			} ];
	return a;
}

function createDashboardRoutine() {
	var a = [
			{
				'next .nav-pills' : 'Lets explore the main features regarding the project',
				'nextButton' : {
					className : "myNext",
					text : "Okay!"
				},
				'skipButton' : {
					className : "mySkip",
					text : "Skip"
				}
			},
			{
				'click li:nth-last-child(1)' : "Before getting started, lets configure the project"
			} ];
	return a;
}

function createSettingsRoutine() {
	var a = [
			{
				'click .tab2' : 'Click here to add a the document to the project',
			},
			{
				'next [class=flex-h-container]' : "Upload a file and import. Then click Next.",
			}, {
				'click .tab5' : "Now, lets add a recommender. Click here!",
			}, {
				'click [value=Create]' : "Click to create a new recommender",
			}, {
				'next form:last' : "Fill in the details and click next",
			}, {
				'click [name=save]' : "Click to save",
			} ];

	return a;
}

function createRedirectRoutine() {
	debugger;
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
