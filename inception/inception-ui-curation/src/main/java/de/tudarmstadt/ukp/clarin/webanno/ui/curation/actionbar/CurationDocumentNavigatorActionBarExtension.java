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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.actionbar;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarContext;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.LegacyCurationPage;
import de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationOpenDocumentDialog;
import de.tudarmstadt.ukp.inception.ui.curation.page.CuratableDocumentPage;
import de.tudarmstadt.ukp.inception.ui.curation.page.CurationPage;
import de.tudarmstadt.ukp.inception.ui.curation.page.SplitCurationPage;

@Order(ActionBarExtension.ORDER_DOCUMENT_NAVIGATOR)
public class CurationDocumentNavigatorActionBarExtension
    implements ActionBarExtension
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public String getRole()
    {
        return ROLE_NAVIGATOR;
    }

    @Override
    public int getPriority()
    {
        return PRIORITY_GLOBAL;
    }

    @Override
    public boolean accepts(ActionBarContext aContext)
    {
        return (aContext.page() instanceof LegacyCurationPage
                || aContext.page() instanceof CurationPage
                || aContext.page() instanceof SplitCurationPage);
    }

    @Override
    public Panel createActionBarItem(String aId, ActionBarContext aContext)
    {
        var documents = listCuratableDocuments(aContext.page());
        return new CurationDocumentNavigator(aId, aContext, documents);
    }

    @Override
    public void onInitialize(ActionBarContext aContext)
    {
        if (!(aContext.page() instanceof LegacyCurationPage)) {
            return;
        }

        // Open the dialog if no document has been selected.
        aContext.page().add(new CurationAutoOpenDialogBehavior());

        aContext.page().addToFooter(createOpenDocumentsDialog("item", aContext.page()));
    }

    /**
     * Must be static because this class is not serializable but the method is used in a callback
     * stored in a Wicket component.
     */
    private static void showCuratedDocument(AnnotationPageBase aPage, AjaxRequestTarget aTarget,
            SourceDocument aDocument)
    {
        try {
            aPage.getDocumentEditorManager().actionShowDocument(aTarget, aDocument,
                    AnnotationSet.CURATION_SET);
        }
        catch (Exception e) {
            LOG.error("Unable to open document [{}]", aDocument, e);
            aPage.error("Unable to open document [" + aDocument.getName() + "]: "
                    + getRootCauseMessage(e));
            aTarget.addChildren(aPage, IFeedback.class);
        }
    }

    private static IModel<List<SourceDocument>> listCuratableDocuments(AnnotationPageBase aPage)
    {
        if (aPage instanceof CuratableDocumentPage page) {
            return LoadableDetachableModel.of(page::listCuratableDocuments);
        }

        if (aPage instanceof CurationPage page) {
            return LoadableDetachableModel.of(page::listCuratableDocuments);
        }

        throw new IllegalStateException("Page class [" + aPage.getClass() + "] not supported");
    }

    private static CurationOpenDocumentDialog createOpenDocumentsDialog(String aId,
            AnnotationPageBase aPage)
    {
        var documents = listCuratableDocuments(aPage);

        return new CurationOpenDocumentDialog(aId, aPage.getProjectModel(), documents,
                (aTarget, aDocument) -> showCuratedDocument(aPage, aTarget, aDocument));
    }
}
