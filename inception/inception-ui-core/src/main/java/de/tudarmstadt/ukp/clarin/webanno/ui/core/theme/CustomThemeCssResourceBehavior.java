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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.theme;

import static de.tudarmstadt.ukp.inception.support.SettingsUtil.getApplicationHome;

import java.io.File;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.resource.FileSystemResourceReference;

public class CustomThemeCssResourceBehavior
    extends Behavior
{
    private static final long serialVersionUID = 5519463574787275765L;

    @Override
    public void renderHead(Component aComponent, IHeaderResponse aResponse)
    {
        File customCss = new File(getApplicationHome(), "theme.css");
        aResponse.render(CssHeaderItem
                .forReference(new FileSystemResourceReference("theme.css", customCss.toPath())));
    }
}
