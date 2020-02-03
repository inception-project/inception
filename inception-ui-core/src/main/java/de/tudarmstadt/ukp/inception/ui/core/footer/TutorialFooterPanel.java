/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.ui.core.footer;

import javax.servlet.ServletContext;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.WebAnnoJavascriptReference;
import de.tudarmstadt.ukp.inception.ui.core.footer.resources.EnjoyHintJsReference;
import de.tudarmstadt.ukp.inception.ui.core.footer.resources.TutorialJavascriptReference;

public class TutorialFooterPanel
    extends Panel
{
    private static final long serialVersionUID = -1520440226035000228L;
    private @SpringBean ServletContext context;

    public TutorialFooterPanel(String aId)
    {
        super(aId); 
    }
    
    @Override
    public void renderHead(IHeaderResponse aResponse) {
        aResponse.render(JavaScriptHeaderItem.forReference(WebAnnoJavascriptReference.get()));
        
        //TODO move it back to web jars after latest release
        aResponse.render(JavaScriptHeaderItem.forReference(EnjoyHintJsReference.get()));
//        aResponse.render(JavaScriptHeaderItem
//                .forReference(new WebjarsJavaScriptResourceReference("enjoyhint/current/enjoyhint.js")));

        // Loading resources for the tour guide feature for the new users
        aResponse.render(JavaScriptHeaderItem.forReference(
                new WebjarsJavaScriptResourceReference("enjoyhint/current/jquery.enjoyhint.js")));
        aResponse.render(CssHeaderItem.forReference(
                new WebjarsCssResourceReference("enjoyhint/current/jquery.enjoyhint.css")));
        aResponse.render(JavaScriptHeaderItem.forReference(new WebjarsJavaScriptResourceReference(
                "jquery.scrollTo/current/jquery.scrollTo.js")));
        aResponse.render(JavaScriptHeaderItem.forReference(
                new WebjarsJavaScriptResourceReference("kinetic/current/kinetic.min.js")));

        aResponse.render(JavaScriptHeaderItem.forReference(TutorialJavascriptReference.get()));
        // add top-margin to next button to fix label and buttons overlapping
        aResponse.render(CssHeaderItem.forCSS(
                ".enjoyhint_next_btn{\n" +
                "  margin-top: 8px;\n" +
                "}",
                "enjoyhint"));
        // check if the tutorial will need to be run
        aResponse.render(OnLoadHeaderItem
                .forScript("setContextPath('" + context.getContextPath() + "');\n" +
                           "runRoutines();"));
    }
}
