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

import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;
import static org.apache.wicket.settings.ExceptionSettings.SHOW_INTERNAL_ERROR_PAGE;

import org.apache.wicket.Page;
import org.apache.wicket.markup.html.IPackageResourceGuard;
import org.apache.wicket.markup.html.SecurePackageResourceGuard;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.WicketApplicationBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.app.config.InceptionResourcesBehavior;
import de.tudarmstadt.ukp.inception.ui.core.ErrorListener;
import de.tudarmstadt.ukp.inception.ui.core.ErrorTestPage;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist.ProjectsOverviewPage;
import de.tudarmstadt.ukp.inception.ui.core.menubar.MenuBar;

@org.springframework.stereotype.Component("wicketApplication")
public class WicketApplication
    extends WicketApplicationBase
{
    @Override
    protected void initOnce()
    {
        super.initOnce();

        initAccessToVueComponents();

        setMetaData(ApplicationPageBase.MENUBAR_CLASS, MenuBar.class);

        initErrorPage();
    }

    /**
     * @see org.apache.wicket.Application#getHomePage()
     */
    @Override
    public Class<? extends Page> getHomePage()
    {
        return ProjectsOverviewPage.class;
    }

    @Override
    protected String getLogoLocation()
    {
        return "/de/tudarmstadt/ukp/inception/app/logo/ukp-logo.png";
    }

    private void initErrorPage()
    {
        // Instead of configuring the different types of errors to refer to our error page, we
        // use @WicketInternalErrorPage and friends on our ErrorPage
        getExceptionSettings().setUnexpectedExceptionDisplay(SHOW_INTERNAL_ERROR_PAGE);
        getRequestCycleListeners().add(new ErrorListener());

        // When running in development mode, we mount the exception test page
        if (DEVELOPMENT.equals(getConfigurationType())) {
            mountPage("/whoops/test", ErrorTestPage.class);
        }
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

    private void initAccessToVueComponents()
    {
        IPackageResourceGuard resourceGuard = getResourceSettings().getPackageResourceGuard();
        if (resourceGuard instanceof SecurePackageResourceGuard) {
            SecurePackageResourceGuard securePackageResourceGuard = (SecurePackageResourceGuard) resourceGuard;
            securePackageResourceGuard.addPattern("+*.vue");
        }
    }

    @Override
    public String getMimeType(String aFileName)
    {
        if (aFileName.endsWith(".vue")) {
            return "text/javascript";
        }

        return super.getMimeType(aFileName);
    }
}
