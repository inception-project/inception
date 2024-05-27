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
$(document).ready(function() {
  // Based on this accepted answer of so
  // http://stackoverflow.com/questions/2335553/jquery-how-to-catch-enter-key-and-change-event-to-tab

  function moveToNextInput(current) {
    let inputs = $(current).parents("#annotationFeatureForm").eq(0).find(":input");
    let idx = inputs.index(current);
    if (idx == inputs.length - 1) {
      inputs[idx].blur();
    } else {
      let input = inputs[idx + 1];
      if (input.focus) input.focus(); //  handles submit buttons
      if (input.select) input.select();
    }
  }
  
  function manageEnter() {
    $("#annotationFeatureForm :input").keydown(function(e) {
      if (e.which == 13) {
        // On a textarea, we consider the enter as a submit only if CTRL or CMD is pressed
        if ($(this).is("textarea") && !(e.ctrlKey || e.metaKey)) {
          return true;
        }
        
        // Avoid Kendo Comboboxes submitting their value twice
        if ($(this).attr('role') == 'combobox') {
          return true;
        }
    
        e.preventDefault();
        moveToNextInput(this);
        return false;
      }
    });    
  }

  $(document).on("keypress", manageEnter);
  manageEnter();
});
