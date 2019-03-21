/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CHAIN_TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public abstract class AnnotationEditorBase
    extends Panel
{
    private static final Logger LOG = LoggerFactory.getLogger(AnnotationEditorBase.class);
    
    private static final long serialVersionUID = 8637373389151630602L;

    private @SpringBean PreRenderer preRenderer;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;

    private AnnotationActionHandler actionHandler;
    private CasProvider casProvider;
    private boolean enableHighlight = true;
    
    public AnnotationEditorBase(final String aId, final IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider)
    {
        super(aId, aModel);
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
    
    public CasProvider getJCasProvider()
    {
        return casProvider;
    }
    
    /**
     * Schedules a rendering call via at the end of the given AJAX cycle. This method can be
     * called multiple times, even for the same annotation editor, but only resulting in a single
     * rendering call.
     */
    public final void requestRender(AjaxRequestTarget aTarget)
    {
        aTarget.registerRespondListener(new RenderListener());
    }
    
    /**
     * Render the contents of the annotation editor again in this present AJAX request. This
     * typically happens by sending JavaScript commands including the complete data structures as
     * JSON via {@link AjaxRequestTarget#appendJavaScript(CharSequence)}.
     */
    protected abstract void render(AjaxRequestTarget aTarget);

    protected VDocument render(CAS aCas, int windowBeginOffset, int windowEndOffset)
    {
        VDocument vdoc = new VDocument();
        preRenderer.render(vdoc, windowBeginOffset, windowEndOffset,
                aCas, getLayersToRender());

        // Fire render event into backend
        extensionRegistry.fireRender(aCas, getModelObject(), vdoc);

        // Fire render event into UI
        Page page = null;
        Optional<IPageRequestHandler> handler = RequestCycle.get().find(IPageRequestHandler.class);
        if (handler.isPresent()) {
            page = (Page) handler.get().getPage();
        };

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
                vdoc.add(new VAnnotationMarker(
                        VMarker.FOCUS, state.getSelection().getAnnotation()));
            }
        }

        return vdoc;
    }

    private List<AnnotationLayer> getLayersToRender()
    {
        AnnotatorState state = getModelObject();
        List<AnnotationLayer> layersToRender = new ArrayList<>();
        for (AnnotationLayer layer : state.getAnnotationLayers()) {
            boolean isSegmentationLayer = layer.getName().equals(Token.class.getName())
                    || layer.getName().equals(Sentence.class.getName());
            boolean isUnsupportedLayer = layer.getType().equals(CHAIN_TYPE)
                    && (state.getMode().equals(Mode.AUTOMATION)
                    || state.getMode().equals(Mode.CORRECTION)
                    || state.getMode().equals(Mode.CURATION));

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
    
    /**
     * This is a special AJAX target response listener which implements hashCode and equals.
     * It useds the markup ID of its host component to identify itself. This enables us to add
     * multiple instances of this listener to an AJAX response without *actually* adding
     * multiple instances since the AJAX response internally keeps track of the listeners
     * using a set.
     */
    private class RenderListener
        implements AjaxRequestTarget.ITargetRespondListener
    {
        private String markupId;

        public RenderListener()
        {
            markupId = AnnotationEditorBase.this.getMarkupId();
        }

        @Override
        public void onTargetRespond(AjaxRequestTarget aTarget)
        {
            AnnotatorState state = getModelObject();
            if (state.getDocument() != null) {
                render(aTarget);
            }
        }

        private AnnotationEditorBase getOuterType()
        {
            return AnnotationEditorBase.this;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((markupId == null) ? 0 : markupId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            RenderListener other = (RenderListener) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (markupId == null) {
                if (other.markupId != null) {
                    return false;
                }
            }
            else if (!markupId.equals(other.markupId)) {
                return false;
            }
            return true;
        }
    }
}
