/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package de.tudarmstadt.ukp.clarin.webanno.support.wicket;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;

public class WicketUtil
{
    public static void refreshPage(AjaxRequestTarget aTarget, Page aPage)
    {
        aPage.forEach(child -> {
            if (child.getOutputMarkupId()) {
                aTarget.add(child);
            }
        });
    }
    
    /**
     * Add call that wraps given script in try-catch block to script text
     */
    public static String wrapInTryCatch(String aJsCall) {
        return " tryCatch(() => {" + aJsCall + "}); ";
    }
}
