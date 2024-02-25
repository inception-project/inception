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
package de.tudarmstadt.ukp.inception.processing;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.processing.recommender.BulkRecommenderPanel;
import de.tudarmstadt.ukp.inception.ui.scheduling.TaskMonitorPanel;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/process")
public class BulkProcessingPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = -8640092578172550838L;

    private @SpringBean UserDao userRepository;

    public BulkProcessingPage(PageParameters aParameters)
    {
        super(aParameters);

        var user = userRepository.getCurrentUser();

        requireProjectRole(user, MANAGER);

        queue(new BulkRecommenderPanel("processingPanel", getProjectModel()));

        queue(new TaskMonitorPanel("runningProcesses", getProject()) //
                .setPopupMode(false) //
                .setShowFinishedTasks(true));
    }
}
