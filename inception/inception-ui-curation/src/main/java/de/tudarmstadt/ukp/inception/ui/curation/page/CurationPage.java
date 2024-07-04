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
package de.tudarmstadt.ukp.inception.ui.curation.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase.PAGE_PARAM_DOCUMENT;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;

import java.util.List;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.CurationSidebarBehavior;
import de.tudarmstadt.ukp.inception.ui.curation.sidebar.CurationSidebarService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/curate/#{" + PAGE_PARAM_DOCUMENT + "}")
public class CurationPage
    extends AnnotationPageBase2
{
    private static final long serialVersionUID = 8665608337791132617L;

    private @SpringBean CurationSidebarService curationSidebarService;
    private @SpringBean UserDao userRepository;
    private @SpringBean WorkloadManagementService workloadManagementService;
    private @SpringBean CurationDocumentService curationDocumentService;

    public CurationPage(PageParameters aPageParameters)
    {
        super(aPageParameters);

        add(new CurationSidebarBehavior());

        var sessionOwner = userRepository.getCurrentUsername();
        var state = getModelObject();

        curationSidebarService.startSession(sessionOwner, state.getProject(), false);
    }

    @Override
    protected void handleParameters(StringValue aDocumentParameter, StringValue aFocusParameter,
            StringValue aUserParameter)
    {
        var sessionOwner = userRepository.getCurrentUser();
        requireProjectRole(sessionOwner, CURATOR);

        super.handleParameters(aDocumentParameter, aFocusParameter, aUserParameter);
    }

    @Override
    public List<SourceDocument> getListOfDocs()
    {
        var state = getModelObject();
        // Since the curatable documents depend on the document state, let's make sure the document
        // state is up-to-date
        workloadManagementService.getWorkloadManagerExtension(state.getProject())
                .freshenStatus(state.getProject());
        return curationDocumentService.listCuratableSourceDocuments(state.getProject());
    }
}
