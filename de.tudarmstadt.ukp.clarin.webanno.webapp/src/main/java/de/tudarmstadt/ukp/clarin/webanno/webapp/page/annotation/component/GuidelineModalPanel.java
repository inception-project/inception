/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.component;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.GuidelineModalWindowPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.correction.CorrectionPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.CurationPage;

/**
 * A panel used by {@link AnnotationPage} {@link CurationPage} and {@link CorrectionPage} consisting
 * of a link to open  annotation guideline
 *
 * @author Seid Muhie Yimam
 *
 */
public class GuidelineModalPanel
    extends Panel
{
    private static final long serialVersionUID = 671214149298791793L;

    public GuidelineModalPanel(String id, final IModel<BratAnnotatorModel> aModel)
    {
        super(id, aModel);
        final ModalWindow guidelineModal;
        add(guidelineModal = new ModalWindow("guidelineModal"));

        guidelineModal.setInitialWidth(550);
        guidelineModal.setInitialHeight(450);
        guidelineModal.setResizable(true);
        guidelineModal.setWidthUnit("px");
        guidelineModal.setHeightUnit("px");
        guidelineModal.setTitle("Open Annotation Guideline, in separate window");

        guidelineModal.setPageCreator(new ModalWindow.PageCreator()
        {
            private static final long serialVersionUID = -2827824968207807739L;

            @Override
            public Page createPage()
            {
                return new GuidelineModalWindowPage(guidelineModal, aModel.getObject().getProject());
            }

        });
        add(new AjaxLink<Void>("showGuidelineModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                guidelineModal.show(target);

            }
        });

    }
}