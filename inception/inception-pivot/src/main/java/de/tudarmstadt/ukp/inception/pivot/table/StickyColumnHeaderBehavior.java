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
package de.tudarmstadt.ukp.inception.pivot.table;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;

public class StickyColumnHeaderBehavior
    extends Behavior
{
    private static final long serialVersionUID = 1809352595271274686L;

    private final String targetSelector;

    public StickyColumnHeaderBehavior(String aTargetSelector)
    {
        targetSelector = aTargetSelector;
    }

    @Override
    public void bind(Component component)
    {
        component.setOutputMarkupId(true);
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response)
    {
        super.renderHead(component, response);

        var script = """
                     (function() {
                     var table = document.querySelector('#%s');
                     if (!table) return;

                     var targets = document.querySelectorAll('%s');
                     if (!targets.length) return;

                     function updateStickyLeft() {
                         var thead = table.querySelector('thead tr:first-child th:first-child');
                         if (!thead) return;
                         var width = thead.getBoundingClientRect().width;
                         targets.forEach(function(el) {
                             el.style.position = 'sticky';
                             el.style.left = width + 'px';
                         });
                     }

                     // Initial calculation
                     updateStickyLeft();

                     // Observe changes in thead size
                     if (window.ResizeObserver) {
                         var observer = new ResizeObserver(updateStickyLeft);
                         var thead = table.querySelector('thead');
                         if (thead) observer.observe(thead);
                     }

                     // Also recalc on window resize
                     window.addEventListener('resize', updateStickyLeft);
                     })();
                     """;
        var js = String.format(script, component.getMarkupId(), targetSelector);

        response.render(OnDomReadyHeaderItem.forScript(js));
    }
}
