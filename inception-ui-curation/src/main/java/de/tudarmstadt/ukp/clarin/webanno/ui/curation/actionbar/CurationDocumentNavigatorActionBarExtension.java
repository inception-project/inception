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

import static java.lang.Integer.MAX_VALUE;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.docnav.DefaultDocumentNavigatorActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.open.OpenDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.DecoratedObject;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.CurationPage;

@Order(0)
@Component
public class CurationDocumentNavigatorActionBarExtension
    extends DefaultDocumentNavigatorActionBarExtension
{
    private @SpringBean CurationDocumentService curationDocumentService;

    @Override
    public String getRole()
    {
        return DefaultDocumentNavigatorActionBarExtension.class.getName();
    }

    @Override
    public int getPriority()
    {
        return MAX_VALUE;
    }

    @Override
    public boolean accepts(AnnotationPageBase aPage)
    {
        return aPage instanceof CurationPage;
    }

    @Override
    protected OpenDocumentDialog createOpenDocumentsDialog(String aId, AnnotationPageBase aPage)
    {
        return new OpenDocumentDialog(aId, aPage.getModel(), aPage.getAllowedProjects(),
                this::listDocuments);
    }

    private List<DecoratedObject<SourceDocument>> listDocuments(Project aProject, User aUser)
    {
        final List<DecoratedObject<SourceDocument>> allSourceDocuments = new ArrayList<>();
        List<SourceDocument> sdocs = curationDocumentService.listCuratableSourceDocuments(aProject);

        for (SourceDocument sourceDocument : sdocs) {
            DecoratedObject<SourceDocument> dsd = DecoratedObject.of(sourceDocument);
            dsd.setLabel("%s (%s)", sourceDocument.getName(), sourceDocument.getState());
            dsd.setColor(sourceDocument.getState().getColor());
            allSourceDocuments.add(dsd);
        }
        return allSourceDocuments;
    }

}
