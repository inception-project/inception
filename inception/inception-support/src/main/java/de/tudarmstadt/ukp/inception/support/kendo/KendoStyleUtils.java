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
package de.tudarmstadt.ukp.inception.support.kendo;

import static java.lang.String.join;

import org.wicketstuff.jquery.core.JQueryBehavior;

public class KendoStyleUtils
{
    /**
     * Use one-third of the browser width but not less than 300 pixels. This is better than using
     * the Kendo auto-sizing feature because that sometimes doesn't get the width right.
     * 
     * @param aBehavior
     *            behavior associated with the component having the dropdown.
     */
    public static void autoDropdownWidth(JQueryBehavior aBehavior)
    {
        aBehavior.setOption("open", join(" ", //
                "function(e) {", //
                "  var listContainer = e.sender.list.closest('.k-popup');", //
                "  listContainer.width(Math.max(window.innerWidth*0.3+kendo.support.scrollbar(),300))", //
                "}"));
    }

    /**
     * Use one-third of the browser height but not less than 200 pixels. This is better than using
     * the Kendo auto-sizing feature because that sometimes doesn't get the height right.
     * 
     * @param aBehavior
     *            behavior associated with the component having the dropdown.
     */
    public static void autoDropdownHeight(JQueryBehavior aBehavior)
    {
        //
        aBehavior.setOption("height", join(" ", //
                "Math.max(window.innerHeight*0.5,200)"));
    }

    /**
     * Prevent scrolling action from closing the dropdown while the focus is on the input field. The
     * solution we use here is a NASTY hack, but I didn't find any other way to cancel out only the
     * closing triggered by scrolling the browser window without having other adverse side effects
     * such as mouse clicks or enter no longer selecting and closing the dropdown. See:
     * https://github.com/inception-project/inception/issues/1517
     *
     * @param aBehavior
     *            behavior associated with the component having the dropdown.
     */
    public static void keepDropdownVisibleWhenScrolling(JQueryBehavior aBehavior)
    {
        // aBehavior.setOption("close", join(" ", //
        // "function(e) {", //
        // " if (document.activeElement == e.sender.element[0]) {", //
        // " e.preventDefault();", //
        // " }", //
        // "}"));

        aBehavior.setOption("close", join(" ", //
                "function(e) {", //
                "  if (new Error().stack.toString().includes('_resize')) {", //
                "    e.preventDefault();", //
                "  }", //
                "}"));
    }

    /**
     * Reset the values in the dropdown listbox to avoid that when opening the dropdown the next
     * time ALL items with the same label as the selected item appear as selected
     *
     * @param aBehavior
     *            behavior associated with the component having the dropdown.
     */
    public static void resetDropdownSelectionOnOpen(JQueryBehavior aBehavior)
    {
        aBehavior.setOption("filtering", join(" ", //
                "function(e) {", //
                "  e.sender.listView.value([]);", //
                "}"));
    }
}
