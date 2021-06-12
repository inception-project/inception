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
package de.tudarmstadt.ukp.inception.ui.core.about;

import java.time.Year;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.StringResourceModel;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.support.about.ApplicationInformation;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

@MountPath("/about")
public class AboutPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -3156459881790077891L;

    public AboutPage()
    {
        setStatelessHint(true);
        setVersioned(false);

        add(new Label("dependencies", ApplicationInformation.loadDependencies()));
        add(new Label("copyright",
                new StringResourceModel("copyright")
                        .setParameters(Integer.toString(Year.now().getValue())))
                                .setEscapeModelStrings(false));
        add(new BookmarkablePageLink<>("home", getApplication().getHomePage()));
    }
}
