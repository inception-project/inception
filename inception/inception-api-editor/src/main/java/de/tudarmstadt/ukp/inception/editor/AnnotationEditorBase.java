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
package de.tudarmstadt.ukp.inception.editor;

import static de.tudarmstadt.ukp.clarin.webanno.model.Mode.CURATION;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CHAIN_TYPE;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.uima.cas.CAS;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.pipeline.RenderingPipeline;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequestedEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.serialization.VDocumentSerializer;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxComponentRespondListener;

public abstract class AnnotationEditorBase
    extends Panel
{
    private static final long serialVersionUID = 8637373389151630602L;
    private static final Logger LOG = getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean AnnotationSchemaProperties properties;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean UserDao userService;
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
     * 
     * @param aTarget
     *            the AJAX target
     */
    public void requestRender(AjaxRequestTarget aTarget)
    {
        if (aTarget == null) {
            LOG.warn("Cannot request render when not in the scope of an AJAX request");
            return;
        }

        try {
            aTarget.registerRespondListener(new AjaxComponentRespondListener(this, _target -> {
                // Is a document loaded?
                if (getModelObject().getDocument() == null) {
                    return;
                }

                Collection<? extends Component> componentsBeingRerendered = _target.getComponents();
                Component c = AnnotationEditorBase.this;
                while (c != null) {
                    if (componentsBeingRerendered.contains(c)) {
                        // If the editor or any of its parents are re-rendered anyway, we do not
                        // need to schedule a JS-based rendering.
                        return;
                    }
                    c = c.getParent();
                }

                // Check if this editor has already been rendered in the current request cycle and
                // if this is the case, skip rendering.
                Set<String> renderedEditors = AnnotationEditorRenderedMetaDataKey.get();

                if (renderedEditors.contains(getMarkupId())) {
                    LOG.trace("[{}] render (AJAX) - was already rendered in this cycle - skipping",
                            getMarkupId());
                    return;
                }

                renderedEditors.add(getMarkupId());

                render(_target);
            }));

            if (getModelObject().getDocument() != null) {
                // Fire render event into UI
                extensionRegistry.fireRenderRequested(aTarget, getModelObject());
                aTarget.getPage().send(aTarget.getPage(), Broadcast.BREADTH,
                        new RenderRequestedEvent(aTarget));
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

    protected <T> T render(CAS aCas, int aWindowBeginOffset, int aWindowEndOffset,
            VDocumentSerializer<T> aTerminalStep)
    {
        var request = RenderRequest.builder() //
                .withState(getModelObject()) //
                .withWindow(aWindowBeginOffset, aWindowEndOffset) //
                .withCas(aCas) //
                .withSessionOwner(userService.getCurrentUser()) //
                .withVisibleLayers(getLayersToRender(getModelObject())) //
                .withLongArcs(true) //
                .build();

        var vdoc = renderingPipeline.render(request);
        return aTerminalStep.render(vdoc, request);
    }

    protected List<AnnotationLayer> getLayersToRender(AnnotatorState state)
    {
        List<AnnotationLayer> layersToRender = new ArrayList<>();
        for (AnnotationLayer layer : state.getAnnotationLayers()) {
            if (!layer.isEnabled()) {
                continue;
            }

            if (!properties.isTokenLayerEditable()
                    && Token.class.getName().equals(layer.getName())) {
                continue;
            }

            if (!properties.isSentenceLayerEditable()
                    && Sentence.class.getName().equals(layer.getName())) {
                continue;
            }

            if (layer.getType().equals(CHAIN_TYPE) && CURATION == state.getMode()) {
                continue;
            }

            layersToRender.add(layer);
        }
        return layersToRender;
    }

    protected void handleError(String aMessage, Exception e)
    {
        RequestCycle requestCycle = RequestCycle.get();
        try {
            requestCycle.find(AjaxRequestTarget.class)
                    .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));
        }
        catch (Exception ex) {
            LOG.error("Cannot refresh feedback panel", ex);
        }

        if (e instanceof AnnotationException) {
            // These are common exceptions happening as part of the user interaction. We do
            // not really need to log their stack trace to the log.
            error(aMessage + ": " + e.getMessage());
            // If debug is enabled, we'll also write the error to the log just in case.
            if (LOG.isDebugEnabled()) {
                LOG.error("{}: {}", aMessage, e.getMessage(), e);
            }
            return;
        }

        LOG.error("{}: {}", aMessage, e.getMessage(), e);
        error(aMessage + ": " + e.getMessage());
    }

    public abstract Optional<ContextMenuLookup> getContextMenuLookup();
}
