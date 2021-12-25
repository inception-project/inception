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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation;

import java.lang.invoke.MethodHandles;

import org.apache.commons.lang3.Validate;
import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.AnnotationEditorProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.RenderingPipeline;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxComponentRespondListener;

public abstract class AnnotationEditorBase
    extends Panel
{
    private static final long serialVersionUID = 8637373389151630602L;
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean AnnotationEditorProperties properties;
    private @SpringBean PreRenderer preRenderer;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean ColoringService coloringService;
    private @SpringBean RenderingPipeline renderingPipeline;

    private final AnnotationActionHandler actionHandler;
    private final CasProvider casProvider;

    public AnnotationEditorBase(final String aId, final IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider)
    {
        super(aId, aModel);

        Validate.notNull(aActionHandler, "Annotation action handle must be provided");
        Validate.notNull(aCasProvider, "CAS provider must be provided");

        actionHandler = aActionHandler;
        casProvider = aCasProvider;

        // Allow AJAX updates.
        setOutputMarkupId(true);

        // The annotator is invisible when no document has been selected. Make sure that we can
        // make it visible via AJAX once the document has been selected.
        setOutputMarkupPlaceholderTag(true);
    }

    public void setModel(IModel<AnnotatorState> aModel)
    {
        setDefaultModel(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    public void setModelObject(AnnotatorState aModel)
    {
        setDefaultModelObject(aModel);
    }

    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    public AnnotationActionHandler getActionHandler()
    {
        return actionHandler;
    }

    public CasProvider getCasProvider()
    {
        return casProvider;
    }

    /**
     * Schedules a rendering call via at the end of the given AJAX cycle. This method can be called
     * multiple times, even for the same annotation editor, but only resulting in a single rendering
     * call.
     */
    public final void requestRender(AjaxRequestTarget aTarget)
    {
        try {
            aTarget.registerRespondListener(new AjaxComponentRespondListener(this, _target -> {
                // Is a document loaded?
                if (getModelObject().getDocument() == null) {
                    return;
                }

                render(_target);
            }));

            if (getModelObject().getDocument() != null) {
                extensionRegistry.fireRenderRequested(getModelObject());
            }
        }
        catch (IllegalStateException e) {
            LOG.warn("Cannot request editor rendering anymore - request is already frozen");
        }
    }

    /**
     * Render the contents of the annotation editor again in this present AJAX request. This
     * typically happens by sending JavaScript commands including the complete data structures as
     * JSON via {@link AjaxRequestTarget#appendJavaScript(CharSequence)}.
     */
    protected abstract void render(AjaxRequestTarget aTarget);

    protected VDocument render(CAS aCas, int aWindowBeginOffset, int aWindowEndOffset)
    {
        return renderingPipeline.render(getModelObject(), aCas, aWindowBeginOffset,
                aWindowEndOffset);
    }
}
