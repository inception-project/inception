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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import static de.tudarmstadt.ukp.inception.kb.RepositoryType.LOCAL;

import java.util.Map;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;
import de.tudarmstadt.ukp.inception.ui.kb.project.local.LocalRepositorySettingsPanel;
import de.tudarmstadt.ukp.inception.ui.kb.project.remote.RemoteRepositorySettingsPanel;

public class AccessSpecificSettingsPanel
    extends Panel
{
    private static final long serialVersionUID = -7834443872889805698L;

    private final LocalRepositorySettingsPanel local;
    private final RemoteRepositorySettingsPanel remote;

    public AccessSpecificSettingsPanel(String id,
            CompoundPropertyModel<KnowledgeBaseWrapper> aModel,
            Map<String, KnowledgeBaseProfile> aKnowledgeBaseProfiles)
    {
        super(id, aModel);
        setOutputMarkupId(true);

        aModel.getObject().clearFiles();

        var isHandlingLocalRepository = aModel.getObject().getKb().getType() == LOCAL;

        // container for form components related to local KBs
        local = new LocalRepositorySettingsPanel("localSpecificSettings", aModel,
                aKnowledgeBaseProfiles);
        local.setVisibilityAllowed(isHandlingLocalRepository);
        add(local);

        // container for form components related to remote KBs
        remote = new RemoteRepositorySettingsPanel("remoteSpecificSettings", aModel,
                aKnowledgeBaseProfiles);
        remote.setVisibilityAllowed(!isHandlingLocalRepository);
        add(remote);
    }

    @SuppressWarnings("unchecked")
    public IModel<KnowledgeBaseWrapper> getModel()
    {
        return (IModel<KnowledgeBaseWrapper>) getDefaultModel();
    }

    public void applyState()
    {
        KnowledgeBase kb = getModel().getObject().getKb();
        switch (kb.getType()) {
        case LOCAL:
            // We need to handle this manually here because the onSubmit method of the upload
            // form is only called *after* the upload component has already been detached and
            // as a consequence all uploaded files have been cleared
            local.handleUploadedFiles();
            break;
        case REMOTE:
            // MB: as of 2018-02, all remote knowledge bases are read-only, hence the
            // PermissionsStep is currently not shown. Therefore, set read-only property here
            // manually.
            kb.setReadOnly(true);
            remote.applyState();
            break;
        default:
            throw new IllegalStateException("Unsupported repository type [" + kb.getType() + "]");
        }
    }
}
