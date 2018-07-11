/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.ui.kb;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.app.session.SessionMetaData;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.less.KnowledgeBasePageLRR;

/**
 * Knowledge Base Page. Current UI/UX issues:
 * <ul>
 * <li>Save/Delete buttons right next to each other in many spots</li>
 * <li>URI should be a label, not an text edit field</li>
 * <li>Instance editor should not offer "Instance of", should always create instance of currently
 * selected concept instead</li>
 * </ul>
 */
@MountPath(value = "/kb.html", alt = { "/kb/${" + PAGE_PARAM_PROJECT_ID + "}" })
public class KnowledgeBasePage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -745797540252091140L;

    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeBasePage.class);
    
    private @SpringBean UserDao userRepository;
    private @SpringBean ProjectService projectService;
    private @SpringBean KnowledgeBaseService kbService;
        
    public KnowledgeBasePage() {
        super();
        LOG.debug("Setting up page without parameters");
        Project project = Session.get().getMetaData(SessionMetaData.CURRENT_PROJECT);
        if (project == null) {
            abort();
        }
        commonInit(project);
    }

    public KnowledgeBasePage(PageParameters aPageParameters) {
        super(aPageParameters);

        User user = userRepository.getCurrentUser();

        // Project has been specified when the page was opened
        Project project = null;
        StringValue projectParam = aPageParameters.get(PAGE_PARAM_PROJECT_ID);
        if (!projectParam.isEmpty()) {
            long projectId = projectParam.toLong();
            try {
                project = projectService.getProject(projectId);
                
                // Check access to project
                if (!SecurityUtil.isAnnotator(project, projectService, user)) {
                    error("You have no permission to access project [" + project.getId() + "]");
                    abort();
                }
            }
            catch (NoResultException e) {
                error("Project [" + projectId + "] does not exist");
                abort();
            }
        }
        commonInit(project);
    }
    
    private void abort() {
        throw new RestartResponseException(getApplication().getHomePage());
    }
    
    protected void commonInit(Project aProject) {
        IModel<Project> projectModel = new LambdaModel<>(() -> aProject);

        List<KnowledgeBase> knowledgeBases = new ArrayList<KnowledgeBase>();
        try {
            knowledgeBases = kbService.getEnabledKnowledgeBases(projectModel.getObject());
        }
        catch (Exception e) {
            error("Unable to fetch knowledgebases: " + e.getLocalizedMessage());
            LOG.error("Unable to fetch knowledgebases.",e);
        }
        
        if (knowledgeBases.isEmpty()) {
            abort();
        }       
        
        // add the main content panel
        IModel<KnowledgeBase> kbModel = Model.of(knowledgeBases.get(0));
        KnowledgeBasePanel panel = new KnowledgeBasePanel("content", projectModel, kbModel);
        add(panel);
        
    }
    
    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssReferenceHeaderItem.forReference(KnowledgeBasePageLRR.get()));
    }
}
