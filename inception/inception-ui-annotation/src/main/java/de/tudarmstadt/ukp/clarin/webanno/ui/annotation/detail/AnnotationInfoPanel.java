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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.lang.invoke.MethodHandles;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.behavior.CssClassNameAppender;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public class AnnotationInfoPanel
    extends Panel
{
    private static final long serialVersionUID = -2911353962253404751L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final WebMarkupContainer noAnnotationWarning;
    private final WebMarkupContainer annotationInfo;

    private final AnnotationDetailEditorPanel actionHandler;

    public AnnotationInfoPanel(String aId, IModel<AnnotatorState> aModel,
            AnnotationDetailEditorPanel aOwner)
    {
        super(aId, aModel);

        actionHandler = aOwner;

        setOutputMarkupPlaceholderTag(true);

        // If there are no features, we want this panel to fill its entire parent so the no-data
        // info is shown prominently
        add(new CssClassNameAppender(LoadableDetachableModel.of(() -> {
            return !isAnnotationSelected() ? "flex-content flex-v-container" : "";
        })));

        noAnnotationWarning = new WebMarkupContainer("noAnnotationWarning");
        noAnnotationWarning.setOutputMarkupPlaceholderTag(true);
        noAnnotationWarning.add(visibleWhen(() -> !isAnnotationSelected()));
        add(noAnnotationWarning);

        annotationInfo = new WebMarkupContainer("annotationInfo");
        annotationInfo.setOutputMarkupPlaceholderTag(true);
        annotationInfo.add(visibleWhen(this::isAnnotationSelected));
        annotationInfo.add(createSelectedAnnotationTypeLabel());
        annotationInfo.add(new AnnotationTextPanel("annotationText", actionHandler, aModel));
        annotationInfo.add(createSelectedAnnotationLayerLabel());
        add(annotationInfo);
    }

    private boolean isAnnotationSelected()
    {
        return getModelObject().getSelection().getAnnotation().isSet();
    }

    public AnnotationPageBase getEditorPage()
    {
        return (AnnotationPageBase) getPage();
    }

    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    private Label createSelectedAnnotationLayerLabel()
    {
        Label label = new Label("selectedAnnotationLayer",
                CompoundPropertyModel.of(getDefaultModel()).bind("selectedAnnotationLayer.uiName"));
        label.setOutputMarkupPlaceholderTag(true);
        return label;
    }

    private Label createSelectedAnnotationTypeLabel()
    {
        Label label = new Label("selectedAnnotationType", LoadableDetachableModel.of(() -> {
            try {
                var editorPanel = findParent(AnnotationDetailEditorPanel.class);
                return String.valueOf(selectFsByAddr(editorPanel.getEditorCas(),
                        getModelObject().getSelection().getAnnotation().getId())).trim();
            }
            catch (Exception e) {
                LOG.warn("Unable to render selected annotation type", e);
                return ExceptionUtils.getRootCauseMessage(e);
            }
        }));
        label.setOutputMarkupPlaceholderTag(true);
        // We show the extended info on the selected annotation only when run in development mode
        label.add(visibleWhen(() -> getModelObject().getSelection().getAnnotation().isSet()
                && DEVELOPMENT.equals(getApplication().getConfigurationType())));
        return label;
    }
}
