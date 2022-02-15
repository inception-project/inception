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
package de.tudarmstadt.ukp.inception.ui.kb;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.ui.kb.KnowledgeBasePage.PAGE_PARAM_KB_NAME;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.head.CssReferenceHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.sass.KnowledgeBasePageCssReference;

/**
 * Knowledge Base Page. Current UI/UX issues:
 * <ul>
 * <li>Save/Delete buttons right next to each other in many spots</li>
 * <li>URI should be a label, not an text edit field</li>
 * <li>Instance editor should not offer "Instance of", should always create instance of currently
 * selected concept instead</li>
 * </ul>
 */
@MountPath(value = NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/kb/${" + PAGE_PARAM_KB_NAME
        + "}", alt = { NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/kb" })
public class KnowledgeBasePage
    extends ProjectPageBase
{
    private static final long serialVersionUID = -745797540252091140L;
    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeBasePage.class);
    public static final String PAGE_PARAM_KB_NAME = "kb";

    private @SpringBean UserDao userRepository;
    private @SpringBean KnowledgeBaseService kbService;

    public KnowledgeBasePage(PageParameters aPageParameters)
    {
        super(aPageParameters);

        Project project = getProject();

        requireProjectRole(userRepository.getCurrentUser(), ANNOTATOR);

        // If a KB id was specified in the URL, we try to get it
        KnowledgeBase kb = null;
        String kbName = aPageParameters.get("kb").toOptionalString();
        if (project != null && kbName != null) {
            kb = kbService.getKnowledgeBaseByName(project, kbName).orElse(null);
        }

        List<KnowledgeBase> knowledgeBases = new ArrayList<>();
        try {
            knowledgeBases = kbService.getEnabledKnowledgeBases(project);
        }
        catch (Exception e) {
            getSession().error("Unable to fetch knowledgebases: " + e.getLocalizedMessage());
            LOG.error("Unable to fetch knowledgebases.", e);
            setResponsePage(getApplication().getHomePage());
        }

        // add the main content panel
        IModel<KnowledgeBase> kbModel = Model.of(kb != null ? kb : knowledgeBases.get(0));
        KnowledgeBasePanel panel = new KnowledgeBasePanel("content", Model.of(project), kbModel);
        add(panel);
    }

    @Override
    public void renderHead(IHeaderResponse response)
    {
        super.renderHead(response);
        response.render(CssReferenceHeaderItem.forReference(KnowledgeBasePageCssReference.get()));
    }
}
