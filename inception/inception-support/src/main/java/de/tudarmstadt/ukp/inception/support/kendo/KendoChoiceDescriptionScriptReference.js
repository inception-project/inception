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
function applyTooltip(aElement) {
  try {
    if (!$(aElement).data('ui-tooltip')) {
      $(aElement).tooltip({
        position: { my: 'center bottom', at: 'center top', of: '.page-footer' },
        show: false,
        hide: false,
        classes: {
          "ui-tooltip": "rounded shadow-lg"
        },
        create: function( event, ui ) {
          // The elements to which the tooltips are bound may be dynamically discarded and reloaded
          // and may not receive the events then which usually cause the tooltip to disappear. To
          // avoid orphan tooltips hanging around, clear all JQuery tooltips here before rendering
          // new ones.
          $("[role='tooltip']").each(function() { $(this).remove(); });
        },
        content: function() { 
          var html =  
            '<div class="tooltip-title">' + ($(this).text() ? $(this).text() : 'no title') +
            '</div> <div class="tooltip-content tooltip-pre">' + ($(this).attr('title') ? $(this).attr('title') : 'no description' )+'</div>';
          return html;
        }
      });
      setTimeout(function() { $(aElement).tooltip().mouseover(); }, 0);
    }
  }
  catch (e) {
    consoler.error(e);
  }
}
