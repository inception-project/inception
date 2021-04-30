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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.Validate;
import org.apache.uima.cas.CAS;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPageRequestHandler;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.PreRenderer;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VAnnotationMarker;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VMarker;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxComponentRespondListener;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public abstract class AnnotationEditorBase
    extends Panel
{
    private static final Logger LOG = LoggerFactory.getLogger(AnnotationEditorBase.class);

    private static final long serialVersionUID = 8637373389151630602L;

    private @SpringBean PreRenderer preRenderer;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;

    private final AnnotationActionHandler actionHandler;
    private final CasProvider casProvider;
    private boolean enableHighlight = true;

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
        VDocument vdoc = new VDocument();
        preRenderer.render(vdoc, aWindowBeginOffset, aWindowEndOffset, aCas, getLayersToRender());

        // Fire render event into backend
        extensionRegistry.fireRender(aCas, getModelObject(), vdoc, aWindowBeginOffset,
                aWindowEndOffset);

        // Fire render event into UI
        Page page = null;
        Optional<IPageRequestHandler> handler = RequestCycle.get().find(IPageRequestHandler.class);
        if (handler.isPresent()) {
            page = (Page) handler.get().getPage();
        }
        ;

        if (page == null) {
            page = getPage();
        }
        send(page, Broadcast.BREADTH,
                new RenderAnnotationsEvent(
                        RequestCycle.get().find(IPartialPageRequestHandler.class).get(), aCas,
                        getModelObject(), vdoc));

        if (isHighlightEnabled()) {
            AnnotatorState state = getModelObject();

            // Disabling for 3.3.0 by default per #406
            // FIXME: should be enabled by default and made optional per #606
            // if (state.getFocusUnitIndex() > 0) {
            // response.addMarker(new SentenceMarker(Marker.FOCUS, state.getFocusUnitIndex()));
            // }

            if (state.getSelection().getAnnotation().isSet()) {
                vdoc.add(
                        new VAnnotationMarker(VMarker.FOCUS, state.getSelection().getAnnotation()));
            }
        }

        return vdoc;
    }

    public List<AnnotationLayer> getLayersToRender()
    {
        AnnotatorState state = getModelObject();
        List<AnnotationLayer> layersToRender = new ArrayList<>();
        for (AnnotationLayer layer : state.getAnnotationLayers()) {
            boolean isSegmentationLayer = layer.getName().equals(Token.class.getName())
                    || layer.getName().equals(Sentence.class.getName());
            boolean isUnsupportedLayer = layer.getType().equals(CHAIN_TYPE)
                    && CURATION == state.getMode();

            if (layer.isEnabled() && !isSegmentationLayer && !isUnsupportedLayer) {
                layersToRender.add(layer);
            }
        }
        return layersToRender;
    }

    public void setHighlightEnabled(boolean aValue)
    {
        enableHighlight = aValue;
    }

    public boolean isHighlightEnabled()
    {
        return enableHighlight;
    }

}
