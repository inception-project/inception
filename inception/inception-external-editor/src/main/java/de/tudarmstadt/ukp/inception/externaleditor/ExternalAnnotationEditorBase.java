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
package de.tudarmstadt.ukp.inception.externaleditor;

import static de.tudarmstadt.ukp.clarin.webanno.security.WicketSecurityUtils.getCsrfTokenFromSession;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.wicket.WicketUtil.wrapInTryCatch;
import static de.tudarmstadt.ukp.inception.websocket.config.WebsocketConfig.WS_ENDPOINT;
import static java.lang.String.format;
import static java.lang.invoke.MethodHandles.lookup;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;
import static org.apache.wicket.markup.head.OnDomReadyHeaderItem.forScript;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Optional;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.DiamJavaScriptReference;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorBase;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorFactory;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.inception.editor.ContextMenuLookup;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.editor.view.DocumentViewExtensionPoint;
import de.tudarmstadt.ukp.inception.externaleditor.command.CommandQueue;
import de.tudarmstadt.ukp.inception.externaleditor.command.EditorCommand;
import de.tudarmstadt.ukp.inception.externaleditor.command.LoadAnnotationsCommand;
import de.tudarmstadt.ukp.inception.externaleditor.command.QueuedEditorCommandsMetaDataKey;
import de.tudarmstadt.ukp.inception.externaleditor.command.ScrollToCommand;
import de.tudarmstadt.ukp.inception.externaleditor.model.AnnotationEditorProperties;
import de.tudarmstadt.ukp.inception.externaleditor.resources.ExternalEditorJavascriptResourceReference;
import de.tudarmstadt.ukp.inception.preferences.ClientSideUserPreferencesProvider;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.selection.ScrollToEvent;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.wicket.ContextMenu;
import jakarta.servlet.ServletContext;

public abstract class ExternalAnnotationEditorBase
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = -196999336741495239L;

    private static final Logger LOG = getLogger(lookup().lookupClass());

    protected static final String CID_VIS = "vis";

    private @SpringBean PreferencesService preferencesService;
    private @SpringBean DocumentViewExtensionPoint documentViewExtensionPoint;
    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean ServletContext context;
    private @SpringBean UserDao userService;

    private final String editorFactoryId;

    private DiamAjaxBehavior diamBehavior;
    private Component vis;
    private ContextMenu contextMenu;

    public ExternalAnnotationEditorBase(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            String aEditorFactoryId)
    {
        super(aId, aModel, aActionHandler, aCasProvider);

        editorFactoryId = aEditorFactoryId;

        setOutputMarkupPlaceholderTag(true);
        add(visibleWhen(getModel().map(AnnotatorState::getProject).isPresent()));
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        vis = makeView();
        vis.setOutputMarkupPlaceholderTag(true);
        queue(vis);

        contextMenu = new ContextMenu("contextMenu");
        queue(contextMenu);

        diamBehavior = createDiamBehavior();
        add(diamBehavior);

        LOG.trace("[{}][{}] {}", getMarkupId(), vis.getMarkupId(), getClass().getSimpleName());
    }

    protected AnnotationEditorFactory getFactory()
    {
        return annotationEditorRegistry.getEditorFactory(editorFactoryId);
    }

    protected DiamAjaxBehavior createDiamBehavior()
    {
        return new DiamAjaxBehavior(contextMenu);
    }

    protected Component getViewComponent()
    {
        return vis;
    }

    public DiamAjaxBehavior getDiamBehavior()
    {
        return diamBehavior;
    }

    @Override
    public Optional<ContextMenuLookup> getContextMenuLookup()
    {
        return Optional.ofNullable(diamBehavior);
    }

    protected abstract Component makeView();

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(DiamJavaScriptReference.get()));

        aResponse.render(forReference(ExternalEditorJavascriptResourceReference.get()));

        if (getModelObject().getDocument() != null && getProperties() != null) {
            aResponse.render(forScript(wrapInTryCatch(initScript())));
        }
    }

    @Override
    protected void onRemove()
    {
        getRequestCycle().find(IPartialPageRequestHandler.class)
                .ifPresent(target -> target.prependJavaScript(wrapInTryCatch(destroyScript())));

        super.onRemove();
    }

    @Override
    public void onEvent(IEvent<?> aEvent)
    {
        // We cannot use a @OnEvent annotation for this because it will not handle
        // events for non-visible components - and the editor may not be visible in the
        // hierarchy a this time
        if (aEvent.getPayload() instanceof ScrollToEvent event) {
            var command = new ScrollToCommand(event.getOffset(), event.getPosition());
            command.setPingRange(event.getPingRange());
            QueuedEditorCommandsMetaDataKey.get().add(command);

            // Do not call our requestRender because we do not want to unnecessarily add the
            // LoadAnnotationsCommand
            if (event.getRequestHandler() != null) {
                super.requestRender(event.getRequestHandler());
            }
        }
    }

    @Override
    public void requestRender(AjaxRequestTarget aTarget)
    {
        QueuedEditorCommandsMetaDataKey.get().add(renderCommand());

        super.requestRender(aTarget);
    }

    protected EditorCommand renderCommand()
    {
        return new LoadAnnotationsCommand();
    }

    protected AnnotationEditorProperties getProperties()
    {
        var props = new AnnotationEditorProperties();
        props.setEditorFactoryId(getFactory().getBeanName());
        props.setDiamAjaxCallbackUrl(getDiamBehavior().getCallbackUrl().toString());
        props.setDiamWsUrl(constructWsEndpointUrl());
        props.setCsrfToken(getCsrfTokenFromSession());

        if (getFactory() instanceof ClientSideUserPreferencesProvider factory) {
            factory.getUserPreferencesKey()
                    .ifPresent(key -> props.setUserPreferencesKey(key.getClientSideKey()));
        }

        return props;
    }

    private String constructWsEndpointUrl()
    {
        Url endPointUrl = Url.parse(format("%s%s", context.getContextPath(), WS_ENDPOINT));
        endPointUrl.setProtocol("ws");
        return RequestCycle.get().getUrlRenderer().renderFullUrl(endPointUrl);
    }

    private String getPropertiesAsJson()
    {
        var props = getProperties();
        try {
            return JSONUtil.toInterpretableJsonString(props);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private CharSequence destroyScript()
    {
        return "ExternalEditor.destroy(document.getElementById('" + vis.getMarkupId() + "'));";
    }

    private String initScript()
    {
        var commandQueue = QueuedEditorCommandsMetaDataKey.get();
        // // If we initialize the editor, it should auto-load the annotations - we do nothing
        // commandQueue.removeIf(cmd -> cmd instanceof LoadAnnotationsCommand);
        return assembleScript(commandQueue);
    }

    private String renderScript()
    {
        return assembleScript(QueuedEditorCommandsMetaDataKey.get());
    }

    private String assembleScript(CommandQueue aCommandQueue)
    {
        var script = getOrInitializeEditorScript();
        for (var cmd : aCommandQueue) {
            script += format(".then(e => { console.log('EDITOR COMMAND: %s'); %s; return e; })",
                    cmd.getClass().getSimpleName(), cmd.command("e"));
        }
        return script;
    }

    private String getOrInitializeEditorScript()
    {
        return format(
                "ExternalEditor.getOrInitialize(document.getElementById('%s'), Diam.factory(), %s)",
                vis.getMarkupId(), getPropertiesAsJson());
    }

    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(renderScript());
    }
}
