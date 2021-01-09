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
package de.tudarmstadt.ukp.inception.app;

import org.apache.wicket.Page;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.WicketApplicationBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.app.config.InceptionResourcesBehavior;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage;
import de.tudarmstadt.ukp.inception.ui.core.menubar.MenuBar;

@org.springframework.stereotype.Component("wicketApplication")
public class WicketApplication
    extends WicketApplicationBase
{
    @Override
    protected void initOnce()
    {
        super.initOnce();

        setMetaData(ApplicationPageBase.MENUBAR_CLASS, MenuBar.class);
    }

    /**
     * @see org.apache.wicket.Application#getHomePage()
     */
    @Override
    public Class<? extends Page> getHomePage()
    {
        return ProjectDashboardPage.class;
    }

    @Override
    protected String getLogoLocation()
    {
        return "/de/tudarmstadt/ukp/inception/app/logo/ukp-logo.png";
    }

    @Override
    protected void initWebFrameworks()
    {
        super.initWebFrameworks();

        initInceptionResources();
    }

    protected void initInceptionResources()
    {
        getComponentInstantiationListeners().add(component -> {
            if (component instanceof Page) {
                component.add(InceptionResourcesBehavior.get());
            }
        });
    }

    @Override
    protected void initDefaultPageMounts()
    {
        super.initDefaultPageMounts();

        // We don't want the project dashboard to be linked as "welcome.html" but rather only under
        // its default URL as defined in the ProjectDashboard class
        unmount("/welcome.html");
    }

}
