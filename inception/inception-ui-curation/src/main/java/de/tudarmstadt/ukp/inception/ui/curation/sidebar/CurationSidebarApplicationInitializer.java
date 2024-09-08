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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

import org.apache.wicket.Component;
import org.apache.wicket.protocol.http.WebApplication;
import org.slf4j.Logger;

import com.giffing.wicket.spring.boot.context.extensions.WicketApplicationInitConfiguration;

import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.inception.ui.curation.page.CurationPage;

/**
 * @deprecated Can be removed when the sidebar curation mode on the annotation page goes away. On
 *             the new {@link CurationPage}, this is not required anymore because the
 *             {@code MatrixWorkflowActionBarExtension} is used.
 * @forRemoval 35.0
 */
@Deprecated
public class CurationSidebarApplicationInitializer
    implements WicketApplicationInitConfiguration
{
    private static final Logger LOG = getLogger(lookup().lookupClass());

    @Override
    public void init(WebApplication aWebApplication)
    {
        aWebApplication.getComponentInitializationListeners()
                .add(this::addCurationSidebarBehaviorToAnnotationPage);
    }

    private void addCurationSidebarBehaviorToAnnotationPage(Component aComponent)
    {
        if (aComponent instanceof AnnotationPage annotationPage) {
            if (!annotationPage.getBehaviors(CurationSidebarBehavior.class).isEmpty()) {
                LOG.trace("CurationSidebarBehavior is already installed");
            }

            LOG.trace("Installing CurationSidebarBehavior");
            annotationPage.add(new CurationSidebarBehavior());
        }
    }
}
