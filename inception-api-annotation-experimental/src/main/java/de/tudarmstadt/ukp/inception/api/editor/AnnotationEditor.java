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
package de.tudarmstadt.ukp.inception.api.editor;

import com.googlecode.wicket.jquery.ui.settings.JQueryUILibrarySettings;
import de.agilecoders.wicket.webjars.request.resource.WebjarsCssResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorRenderedMetaDataKey;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.*;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ContextMenu;
import de.tudarmstadt.ukp.inception.api.editor.controller.AnnotationEditorController;
import de.tudarmstadt.ukp.inception.support.vue.VueBehavior;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

@EnableWebSocket
public class AnnotationEditor extends AnnotationEditorBase
{
    private static final String PARAM_ACTION = "action";
    private static final long serialVersionUID = 2983502506977571078L;

    private @SpringBean  AnnotationEditorController controller;

    private AbstractAjaxBehavior requestHandler;

    private final ContextMenu contextMenu;
    private final WebMarkupContainer vis;

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationEditor.class);

    public AnnotationEditor(String id, IModel<AnnotatorState> aModel,
                            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider) {
        super(id, aModel, aActionHandler, aCasProvider);

        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);
        add(vis);

        LOG.trace("[{}][{}] BratAnnotationEditor", getMarkupId(), vis.getMarkupId());

        contextMenu = new ContextMenu("contextMenu");
        add(contextMenu);

        AnnotatorState state = getModelObject();

        controller.initController(state.getUser(), state.getProject(), state.getDocument(), aCasProvider);

        add(new VueBehavior(
            new PackageResourceReference(getClass(), "Websocket.vue")));

        add(new VueBehavior(
            new PackageResourceReference(getClass(), "AnnotationAPI.vue")));
    }
    @OnMessage
    public void onMessage(String message, Session s) throws IOException, InterruptedException {
        System.out.println(message);
    }

    @OnOpen
    public void onOpen() {
        System.out.println("Client connected");
    }

    @OnClose
    public void onClose() {
        System.out.println("Closing");
    }


    @Override
    protected void onConfigure() {
        super.onConfigure();

        setVisible(getModelObject() != null && getModelObject().getProject() != null);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse) {
        super.renderHead(aResponse);

        // CSS
        aResponse.render(CssHeaderItem.forReference(BratCssVisReference.get()));
        aResponse.render(CssHeaderItem.forReference(BratCssUiReference.get()));
        aResponse.render(CssHeaderItem
            .forReference(new WebjarsCssResourceReference("animate.css/current/animate.css")));

        // Libraries
        aResponse.render(forReference(JQueryUILibrarySettings.get().getJavaScriptReference()));
        aResponse.render(forReference(JQuerySvgResourceReference.get()));
        aResponse.render(forReference(JQuerySvgDomResourceReference.get()));
        aResponse.render(forReference(JQueryJsonResourceReference.get()));
        aResponse.render(forReference(JQueryScrollbarWidthReference.get()));
        aResponse.render(forReference(JSONPatchResourceReference.get()));

        // BRAT helpers
        aResponse.render(forReference(BratConfigurationResourceReference.get()));
        aResponse.render(forReference(BratUtilResourceReference.get()));
        // aResponse.render(
        // JavaScriptHeaderItem.forReference(BratAnnotationLogResourceReference.get()));

        // BRAT modules
        aResponse.render(forReference(BratDispatcherResourceReference.get()));
        aResponse.render(forReference(BratAjaxResourceReference.get()));
        aResponse.render(forReference(BratVisualizerResourceReference.get()));
        aResponse.render(forReference(BratVisualizerUiResourceReference.get()));
        aResponse.render(forReference(BratAnnotatorUiResourceReference.get()));
        // aResponse.render(
        // JavaScriptHeaderItem.forReference(BratUrlMonitorResourceReference.get()));

        // When the page is re-loaded or when the component is added to the page, we need to
        // initialize the brat stuff.
        StringBuilder js = new StringBuilder();

        // If a document is already open, we also need to render the document. This happens either
        // when a page is freshly loaded or when e.g. the whole editor is added to the page or
        // when it is added to a partial page update (AJAX request).
        // If the editor is part of a full or partial page update, then it needs to be
        // reinitialized. So we need to use deferred rendering. The render() method checks the
        // partial page update to see if the editor is part of it and if so, it skips itself so
        // no redundant rendering is performed.
        aResponse.render(OnDomReadyHeaderItem.forScript(js));
    }


    @Override
    protected void render(AjaxRequestTarget aTarget) {
        // Check if this editor has already been rendered in the current request cycle and if this
        // is the case, skip rendering.
        RequestCycle requestCycle = RequestCycle.get();
        Set<String> renderedEditors = requestCycle
            .getMetaData(AnnotationEditorRenderedMetaDataKey.INSTANCE);
        if (renderedEditors == null) {
            renderedEditors = new HashSet<>();
            requestCycle.setMetaData(AnnotationEditorRenderedMetaDataKey.INSTANCE, renderedEditors);
        }

        if (renderedEditors.contains(getMarkupId())) {
            LOG.trace("[{}][{}] render (AJAX) - was already rendered in this cycle - skipping",
                getMarkupId(), vis.getMarkupId());
            return;
        }

        renderedEditors.add(getMarkupId());

        // Check if the editor or any of its parents has been added to a partial page update. If
        // this is the case, then deferred rendering in renderHead kicks in and we do not need to
        // render here.
        Set<Component> components = new HashSet<>(aTarget.getComponents());
        boolean deferredRenderingRequired = components.contains(this)
            || visitParents(MarkupContainer.class, (aParent, aVisit) -> {
            if (components.contains(aParent)) {
                aVisit.stop(aParent);
            }
        }) != null;
        if (deferredRenderingRequired) {
            LOG.trace("[{}][{}] render (AJAX) - deferred rendering will trigger - skipping",
                getMarkupId(), vis.getMarkupId());
            return;
        }

        LOG.trace("[{}][{}] render (AJAX)", getMarkupId(), vis.getMarkupId());
    }
}
