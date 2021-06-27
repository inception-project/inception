/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var cName = "tutorialSession";
var projectCName = "projectName";

var enjoyhint_instance = new EnjoyHint({});
var contextPath;
  

function runRoutines() {
      var enjoyhint_script_steps;
      var currentPage = window.location.pathname;
      var projectId = getPageParameter(currentPage, 'p');
      
      var ps = getCookie(cName); 
      
      //don't do all the stuff if we're not in the tutorial
      if (ps == ""){ 
        return;
      }
      
      // if the tutorial ended, remove cookies
      if (ps == "ended"){
        deleteCookies();
        return;
      }
      
      //if (currentPage == contextPath + "/" && ps == "tutorialStarted")
      //{
      //  enjoyhint_instance = new EnjoyHint({
      //    onEnd : function() {
      //      setCookie(cName, 'tutorialStarted');
      //    },
      //    onSkip : function() {
      //      setCookie(cName, 'ended');
      //    }
      //  });

      //  enjoyhint_script_steps = createFirstPageRoutine();
      //  enjoyhint_instance.set(enjoyhint_script_steps);
      //  enjoyhint_instance.runScript();
      //} 
      
      //else 
      if (currentPage.endsWith("/settings")  && projectId == 'NEW' && ps == "tutorialStarted") 
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
      
      else if (currentPage.endsWith("/settings") && projectId == 'NEW' && ps == "projectSaved") 
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
      
      else if (currentPage == contextPath + "/" && ps == "projectCreated") 
      {
          enjoyhint_instance = new EnjoyHint({
            onStart: function() {
              $("[name^='projectImport']").prop("disabled", true);
              $("[id^='roleFilterLink']").prop("disabled", true);
            },
            onEnd : function() {
              setCookie(cName, "projectView");
              $("[name^='projectImport']").prop("disabled", false);
              $("[id^='roleFilterLink']").prop("disabled", false);
            },
            onSkip : function() {
              setCookie(cName, 'ended');
              $("[name^='projectImport']").prop("disabled", false);
              $("[id^='roleFilterLink']").prop("disabled", false);
            }
          });
          
          enjoyhint_script_steps = createFirstPageRoutinePart2();
          enjoyhint_instance.set(enjoyhint_script_steps);
          enjoyhint_instance.runScript();
      }
      
      else if (!currentPage.endsWith("/settings") && ps == "projectView") 
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
      
      else if (currentPage.endsWith("/settings/details") && ps == "projectsettingView") {
        
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
      
      else if (currentPage.endsWith("/settings/documents") && ps == 'projectsettingsOpenedDoc'){
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
      
      else if (currentPage.endsWith("/settings/documents") && ps == "projectsettingsDocUploaded") {
        
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
      
      else if (currentPage.endsWith("/settings/recommenders") && ps == 'projectsettingsOpenedRecommenders'){
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
      
      else if (currentPage.endsWith("/settings/recommenders") && ps == "projectsettingsCreatedRecommender") {
        
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
          onStart: function() {
            $('.sidebar-item').css('pointer-events', 'none');
          },
          onEnd : function() {
            setCookie(cName, "farewell");
            location.reload(); // necessary to cause switch to farewell
            // $('.sidebar-item').css('pointer-events', '');
          },
          onSkip : function() {
            setCookie(cName, 'ended');
            $('.sidebar-item').css('pointer-events', '');
          }
        });
        
        // start routine
        enjoyhint_script_steps = createDashboardRoutine2();
        enjoyhint_instance.set(enjoyhint_script_steps);
        enjoyhint_instance.runScript();
      }
      
      else if (ps == "farewell")
      {
        enjoyhint_instance = new EnjoyHint({
          onStart: function() {
            $('.sidebar-item').css('pointer-events', 'none');
          },
          onEnd : function() {
            setCookie(cName, "ended");
            $('.sidebar-item').css('pointer-events', '');
          },
          onSkip : function() {
            setCookie(cName, 'ended');
            $('.sidebar-item').css('pointer-events', '');
          }
        });
        
        enjoyhint_script_steps = createLastRoutine();
        enjoyhint_instance.set(enjoyhint_script_steps);
        enjoyhint_instance.runScript();
      }
};

function startTutorial() {
  var enjoyhint_instance = new EnjoyHint({
    onStart: function() {
      $('.navbar-brand').css('pointer-events', 'none');
    },
    onEnd : function() {
      $('.navbar-brand').css('pointer-events', '');
    },
    onSkip : function() {
      $('.navbar-brand').css('pointer-events', '');
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
    var cookiePath = contextPath == '' ? '/' : contextPath;
  document.cookie = cname + "=" + cvalue + ";" + expires + ";path="+cookiePath;
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
  return [{
    margin: 0,
    'event [name=\'p::name\']' : 'Write a name for your project and press Enter.', 
    'showSkip': false
  }];
}

function createFirstPageRoutine() {
  return [{
    margin: 0,
    'next .navbar-brand' : "Welcome to INCEpTION! Let me guide you through its features. At any time during the tutorial you can click 'x' to quit the tour.",
    'nextButton' : {
      className : "myNext",
      text : "Let's start!"
    },
    'showSkip': false
  },
  {
    margin: 0,
    'click .btn btn-primary' : "Click here to create a new project.", 
    'showSkip': false
  }];
}

function createFirstPageRoutinePart2() {
  var projectName = getCookie("projectName");
  var selector = "a:contains('"+projectName+"'):first";
  return [{
    margin: 0,
    'next [name=projectImport]' : "Instead of creating a new project, existing projects can also be imported.", 
    'showSkip': false
  },
  {
    margin: 0,
    'next .input-group:last' : "You can filter the projects based on your role in them.",
    'showSkip': false
  },
  {
    margin: 0,
    onBeforeStart:function(){ 
      $('.scrolling').scrollTo(selector, 80);

    },
    "event": "click", 
    "selector": selector,
    "description": "Click on the project to get started.",
    'showSkip': false
  }];
}

function createDashboardRoutine() {
  var settingsSidebarItemSelector = getDashboardSidebarSelector('/settings/details');
  return [{
    margin: 0,
    "event": "click",
    "selector": settingsSidebarItemSelector,
    "description" : "Before getting started, let's configure the project. Please click 'Settings'.",
    "showSkip": false
  }];
}

function createOpenDocumentsRoutine(enjoyHint) {
  var documentSidebarItemSelector = getDashboardSidebarSelector('/documents');
  return [{
    margin: 0,
    "event": "click",
    "selector": documentSidebarItemSelector,
    "description" : "Click here to add a document to the project.",
    "showSkip": false
  }];
}

/**
 * If the sidebar is collapsed we select the icon to click on, else the whole link
 */
function getDashboardSidebarSelector(urlEnd) {
  if (document.querySelector('.dashboard-sidebar.collapsed') != null) {
    return 'a[href$="' + urlEnd + '"] > .icon';
  }
  else {
    return 'a[href$="' + urlEnd + '"]';
  }
}

function createAddDocumentRoutine(enjoyHint) {
  return [{
    margin: 0,
    'next [class=flex-h-container]' : "Upload a document (e.g. a .txt file) using the button next to 'Select Files', " +
    "choose a format (e.g. 'Plain Text') and click 'Import'.",
    'showSkip': false
  }];
}

function createOpenRecommendersRoutine(enjoyHint) {
  var recommendersSidebarItemSelector = getDashboardSidebarSelector('/recommenders');
  return [{
    margin: 0,
    "event": "click",
    "selector": recommendersSidebarItemSelector,
    "description" : 'Now, lets add a recommender. Click here!',
    "showSkip": false
  }];
}

function createAddRecommenderRoutine(enjoyHint) {
  return [{
    margin: 0,
    'click .btn-primary' : "Click here to create a new recommender that will give you suggestions while annotating.",
    'showSkip': false
  }];
}

function createRecommenderSettingsRoutine(enjoyHint) {
  return [{
    margin: 0,
    'next .form-group:nth(3)' : "Select a Layer (the enclosing annotation e.g. 'Named entity').",
    'showSkip': false
  }, {
    margin: 0,
    'next .form-group:nth(4)' : "Select a Feature (the annotation's attribute which should be predicted e.g. 'value').",
    'showSkip': false
  }, {
    margin: 0,
    'next .form-group:nth(5)' : "Select a Tool which is used to produce the suggestions e.g. 'Stringmatcher'.",
    'showSkip': false
  }, {
    margin: 0,
    'next [name=save]' : "Click 'Save'.",
    'showSkip': false
  }, {
    margin: 0,
    'click .nav-item:nth-of-type(2)' : "Now, let's go back to the Dashboard.",
    'showSkip': false
  }];
}

function createProjectSavedRoutine() {
  return [{
    margin: 0,
    'click .nav-item:first-of-type' : 'Click here to go back to the projects overview page.',
    'showSkip': false
  }];
}

function createLastRoutine() {
  return [{
    margin: 0,
    'event .dashboard-sidebar' : 'Now, feel free to explore INCEpTION on your own.',
    'skipButton' : { className : "mySkip", text : "Thanks!" }
  }];
}

function createDashboardRoutine2() {
  var annotateSidebarItemSelector = getDashboardSidebarSelector('/annotate');
  var curateSidebarItemSelector = getDashboardSidebarSelector('/curate');
  var monitoringSidebarItemSelector = getDashboardSidebarSelector('/monitoring');
  var simulationSidebarItemSelector = getDashboardSidebarSelector('/simulation');
  return [{
    margin: 0,
    "event": "next",
    "selector": annotateSidebarItemSelector,
    "description" : "Finally, some information on the Dashboard's buttons: Here, you can annotate your documents.",
    "showSkip": false,
    "showNext": true
  }, {
    margin: 0,
    "event": "next",
    "selector": curateSidebarItemSelector,
    "description" : "Completely annotated documents can be curated to form the final result documents.",
    "showSkip": false,
    "showNext": true
  }, {
    margin: 0,
    "event": "next",
    "selector": monitoringSidebarItemSelector,
    "description" : "Here, you can see the annotators' progress and assign documents.",
    "showSkip": false,
    "showNext": true
  }, {
    margin: 0,
    "event": "next",
    "selector": simulationSidebarItemSelector,
    "description" : "This allows you to evaluate your recommenders.",
    "showSkip": false,
    "showNext": true
  }];
}

function getPageParameter(sPath, sParam) {
  var sPathParts = sPath.split("/");

  for (i = 0; i < sPathParts.length - 1; i++) {
    if (sPathParts[i] === sParam) {
      return decodeURIComponent(sPathParts[i + 1]);
    }
  }
  
  return null;
};
