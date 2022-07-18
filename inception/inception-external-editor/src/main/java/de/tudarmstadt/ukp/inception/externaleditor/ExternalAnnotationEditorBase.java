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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil.wrapInTryCatch;
import static java.lang.String.format;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;
import static org.apache.wicket.markup.head.OnDomReadyHeaderItem.forScript;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.servlet.ServletContext;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.core.request.handler.IPartialPageRequestHandler;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Request;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;

import com.googlecode.wicket.jquery.ui.widget.menu.IMenuItem;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.Selection;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.DocumentViewExtensionPoint;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.JumpToEvent;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ContextMenu;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.DiamJavaScriptReference;
import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandlerBase;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.externaleditor.command.CommandQueue;
import de.tudarmstadt.ukp.inception.externaleditor.command.JumpToCommand;
import de.tudarmstadt.ukp.inception.externaleditor.command.LoadAnnotationsCommand;
import de.tudarmstadt.ukp.inception.externaleditor.command.QueuedEditorCommandsMetaDataKey;
import de.tudarmstadt.ukp.inception.externaleditor.model.AnnotationEditorProperties;
import de.tudarmstadt.ukp.inception.externaleditor.resources.ExternalEditorJavascriptResourceReference;

public abstract class ExternalAnnotationEditorBase
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = -196999336741495239L;

    protected static final String MID_VIS = "vis";

    private @SpringBean DocumentViewExtensionPoint documentViewExtensionPoint;
    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationEditorRegistry annotationEditorRegistry;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean ServletContext context;

    private DiamAjaxBehavior diamBehavior;
    private Component vis;
    private ContextMenu contextMenu;

    public ExternalAnnotationEditorBase(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider)
    {
        super(aId, aModel, aActionHandler, aCasProvider);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        setOutputMarkupPlaceholderTag(true);

        diamBehavior = new DiamAjaxBehavior();
        diamBehavior.addPriorityHandler(new ShowContextMenuHandler());
        add(diamBehavior);

        contextMenu = new ContextMenu("contextMenu");
        queue(contextMenu);

        vis = makeView();
        vis.setOutputMarkupPlaceholderTag(true);
        queue(vis);
    }

    protected Component getViewComponent()
    {
        return vis;
    }

    public DiamAjaxBehavior getDiamBehavior()
    {
        return diamBehavior;
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

    @OnEvent
    public void onJumpTo(JumpToEvent aEvent)
    {
        QueuedEditorCommandsMetaDataKey.get()
                .add(new JumpToCommand(aEvent.getOffset(), aEvent.getPosition()));

        // Do not call our requestRender because we do not want to unnecessarily add the
        // LoadAnnotationsCommand
        super.requestRender(aEvent.getRequestHandler());
    }

    @Override
    public void requestRender(AjaxRequestTarget aTarget)
    {
        QueuedEditorCommandsMetaDataKey.get().add(new LoadAnnotationsCommand());

        super.requestRender(aTarget);
    }

    protected abstract AnnotationEditorProperties getProperties();

    private String getPropertiesAsJson()
    {
        AnnotationEditorProperties props = getProperties();
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
        // If we initialize the editor, it should auto-load the annotations - we do nothing
        commandQueue.removeIf(cmd -> cmd instanceof LoadAnnotationsCommand);
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

    private void actionArcRightClick(AjaxRequestTarget aTarget, VID paramId)
        throws IOException, AnnotationException
    {
        if (!getModelObject().getSelection().isSpan()) {
            return;
        }

        CAS cas;
        try {
            cas = getCasProvider().get();
        }
        catch (Exception e) {
            handleError("Unable to load data", e);
            return;
        }

        // Currently selected span
        AnnotationFS originFs = selectAnnotationByAddr(cas,
                getModelObject().getSelection().getAnnotation().getId());

        // Target span of the relation
        AnnotationFS targetFs = selectAnnotationByAddr(cas, paramId.getId());

        AnnotatorState state = getModelObject();
        Selection selection = state.getSelection();
        selection.selectArc(VID.NONE_ID, originFs, targetFs);

        // Create new annotation
        getActionHandler().actionCreateOrUpdate(aTarget, cas);
    }

    private class ShowContextMenuHandler
        extends EditorAjaxRequestHandlerBase
        implements Serializable
    {
        private static final long serialVersionUID = 2566256640285857435L;

        @Override
        public String getCommand()
        {
            return ACTION_CONTEXT_MENU;
        }

        @Override
        public boolean accepts(Request aRequest)
        {
            final VID paramId = getVid(aRequest);
            return super.accepts(aRequest) && paramId.isSet() && !paramId.isSynthetic()
                    && !paramId.isSlotSet();
        }

        @Override
        public AjaxResponse handle(AjaxRequestTarget aTarget, Request aRequest)
        {
            try {
                List<IMenuItem> items = contextMenu.getItemList();
                items.clear();

                if (getModelObject().getSelection().isSpan()) {
                    VID vid = getVid(aRequest);
                    items.add(new LambdaMenuItem("Link to ...",
                            _target -> actionArcRightClick(_target, vid)));
                }

                extensionRegistry.generateContextMenuItems(items);

                if (!items.isEmpty()) {
                    contextMenu.onOpen(aTarget);
                }
            }
            catch (Exception e) {
                handleError("Unable to load data", e);
            }

            return new DefaultAjaxResponse(getAction(aRequest));
        }
    }
}
