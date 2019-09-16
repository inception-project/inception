/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar;

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static wicket.contrib.input.events.EventType.click;
import static wicket.contrib.input.events.key.KeyType.Page_down;
import static wicket.contrib.input.events.key.KeyType.Page_up;
import static wicket.contrib.input.events.key.KeyType.Shift;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.danekja.java.util.function.serializable.SerializableBiFunction;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.export.ExportDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.open.OpenDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;

public class DocumentNavigator
    extends Panel
{
    private static final long serialVersionUID = 7061696472939390003L;

    private @SpringBean ProjectService projectService;
    
    private AnnotationPageBase page;
    private IModel<List<DecoratedObject<Project>>> projectListModel;
    private SerializableBiFunction<Project, User, List<DecoratedObject<SourceDocument>>> 
            docListProvider;
    
    private final OpenDocumentDialog openDocumentsModal;
    private final ExportDocumentDialog exportDialog;

    public DocumentNavigator(String aId, AnnotationPageBase aPage,
            IModel<List<DecoratedObject<Project>>> aProjectListModel)
    {
        this(aId, aPage, aProjectListModel, null);
    }

    public DocumentNavigator(String aId, AnnotationPageBase aPage,
            IModel<List<DecoratedObject<Project>>> aProjectListModel,
            SerializableBiFunction<Project, User, List<DecoratedObject<SourceDocument>>> 
                    aDocListProvider)
    {
        super(aId);

        page = aPage;
        projectListModel = aProjectListModel;
        docListProvider = aDocListProvider;

        add(new LambdaAjaxLink("showPreviousDocument", t -> actionShowPreviousDocument(t))
                .add(new InputBehavior(new KeyType[] { Shift, Page_up }, click)));

        add(new LambdaAjaxLink("showNextDocument", t -> actionShowNextDocument(t))
                .add(new InputBehavior(new KeyType[] { Shift, Page_down }, click)));

        add(new LambdaAjaxLink("showOpenDocumentDialog", this::actionShowOpenDocumentDialog));

        // Open the dialog if no document has been selected
        page.add(new AutoOpenDialogBehavior());

        // We put the dialog into the page footer since this is presently the only place where we
        // can dynamically add stuff to the page.
        openDocumentsModal = createOpenDocumentsDialog("item");
        page.addToFooter(openDocumentsModal);
        
        add(exportDialog = new ExportDocumentDialog("exportDialog", page.getModel()));
        add(new LambdaAjaxLink("showExportDialog", exportDialog::show).add(visibleWhen(() -> {
            AnnotatorState state = page.getModelObject();
            return state.getProject() != null
                    && (projectService.isManager(state.getProject(), state.getUser())
                            || !state.getProject().isDisableExport());
        })));
    }
    
    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        page.addToFooter(openDocumentsModal);
    }
        
    @Override
    protected void onRemove()
    {
        page.removeFromFooter(openDocumentsModal);
        
        super.onRemove();
    }
    
    protected OpenDocumentDialog createOpenDocumentsDialog(String aId)
    {
        return new OpenDocumentDialog(aId, page.getModel(), projectListModel,
                docListProvider)
        {
            private static final long serialVersionUID = 5474030848589262638L;

            @Override
            public void onDocumentSelected(AjaxRequestTarget aTarget)
            {
                DocumentNavigator.this.onDocumentSelected(aTarget);
            }
        };
    }

    public void onDocumentSelected(AjaxRequestTarget aTarget)
    {
        page.actionLoadDocument(aTarget);
    }

    /**
     * Show the previous document, if exist
     */
    public void actionShowPreviousDocument(AjaxRequestTarget aTarget)
    {
        page.getModelObject().moveToPreviousDocument(page.getListOfDocs());
        page.actionLoadDocument(aTarget);
    }

    /**
     * Show the next document if exist
     */
    public void actionShowNextDocument(AjaxRequestTarget aTarget)
    {
        page.getModelObject().moveToNextDocument(page.getListOfDocs());
        page.actionLoadDocument(aTarget);
    }

    public void actionShowOpenDocumentDialog(AjaxRequestTarget aTarget)
    {
        page.getModelObject().getSelection().clear();
        openDocumentsModal.show(aTarget);
    }

    /**
     * Opens the "Open document" dialog if the page is loaded and no document has been selected yet.
     */
    private class AutoOpenDialogBehavior
        extends AbstractDefaultAjaxBehavior
    {
        private static final long serialVersionUID = 5700114110001447912L;

        /**
         * for the first time, open the <b>open document dialog</b>
         */
        @Override
        public void renderHead(Component aComponent, IHeaderResponse aResponse)
        {
            super.renderHead(aComponent, aResponse);

            aResponse.render(OnLoadHeaderItem.forScript(getCallbackScript()));
        }

        @Override
        protected void respond(AjaxRequestTarget aTarget)
        {
            // If the page has loaded and there is no document open yet, show the open-document
            // dialog. Also check that the dialog is actually on the page (in the footer) before
            // trying to open it.
            if (
                    page.getModelObject().getDocument() == null && 
                    page.getFooterItems().getObject().contains(openDocumentsModal)
            ) {
                actionShowOpenDocumentDialog(aTarget);
            }
        }
    }
}
