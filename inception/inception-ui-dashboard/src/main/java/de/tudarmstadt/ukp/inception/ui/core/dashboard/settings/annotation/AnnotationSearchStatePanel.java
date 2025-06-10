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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.settings.annotation;

import static de.tudarmstadt.ukp.inception.search.model.AnnotationSearchState.KEY_SEARCH_STATE;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.search.SearchService;
import de.tudarmstadt.ukp.inception.search.model.AnnotationSearchState;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaForm;

public class AnnotationSearchStatePanel
    extends Panel
{
    private static final long serialVersionUID = 4663693446465391162L;

    private static final String CID_REINDEX_PROJECT = "reindexProject";

    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;
    private @SpringBean PreferencesService preferencesService;
    private @SpringBean(required = false) SearchService searchService;
    private @SpringBean UserDao userService;

    public AnnotationSearchStatePanel(String aId, IModel<Project> aProjectModel)
    {
        super(aId, aProjectModel);

        if (searchService == null) {
            setVisibilityAllowed(false);
            return;
        }

        setOutputMarkupPlaceholderTag(true);

        var state = preferencesService.loadDefaultTraitsForProject(KEY_SEARCH_STATE,
                getModel().getObject());

        queue(new LambdaForm<>("form", CompoundPropertyModel.of(state)) //
                .onSubmit(this::actionSave));

        queue(new CheckBox("caseSensitiveDocumentText").setOutputMarkupId(true));
        queue(new CheckBox("caseSensitiveFeatureValues").setOutputMarkupId(true));

        queue(new LambdaAjaxLink(CID_REINDEX_PROJECT, this::actionRebuildIndex));
    }

    @SuppressWarnings("unchecked")
    public IModel<Project> getModel()
    {
        return (IModel<Project>) getDefaultModel();
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<AnnotationSearchState> aForm)
    {
        preferencesService.saveDefaultTraitsForProject(KEY_SEARCH_STATE, getModel().getObject(),
                aForm.getModelObject());
    }

    private void actionRebuildIndex(AjaxRequestTarget aTarget)
    {
        searchService.enqueueReindexTask(getModel().getObject(), userService.getCurrentUser(),
                "Project settings");
        info("Starting index rebuild... this may take a while. You can work as usual but search "
                + "results will only become available once the process is complete.");
        aTarget.addChildren(getPage(), IFeedback.class);
    }
}
