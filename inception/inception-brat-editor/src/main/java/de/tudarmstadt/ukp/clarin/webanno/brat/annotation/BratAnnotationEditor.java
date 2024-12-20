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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.brat.annotation.RenderType.FULL;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.wicket.ServletContextUtils.referenceToUrl;
import static java.lang.String.format;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.Serializable;

import org.apache.uima.cas.CAS;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.Request;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorProperties;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.command.LoadCollectionCommand;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.BratSerializer;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratCssReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.schema.BratSchemaGenerator;
import de.tudarmstadt.ukp.inception.diam.editor.DiamAjaxBehavior;
import de.tudarmstadt.ukp.inception.diam.editor.actions.EditorAjaxRequestHandlerBase;
import de.tudarmstadt.ukp.inception.diam.model.ajax.AjaxResponse;
import de.tudarmstadt.ukp.inception.diam.model.ajax.DefaultAjaxResponse;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.externaleditor.ExternalAnnotationEditorBase;
import de.tudarmstadt.ukp.inception.externaleditor.command.EditorCommand;
import de.tudarmstadt.ukp.inception.externaleditor.command.LoadAnnotationsCommand;
import de.tudarmstadt.ukp.inception.externaleditor.command.QueuedEditorCommandsMetaDataKey;
import de.tudarmstadt.ukp.inception.externaleditor.model.AnnotationEditorProperties;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import jakarta.servlet.ServletContext;

/**
 * Brat annotator component.
 */
public class BratAnnotationEditor
    extends ExternalAnnotationEditorBase
{
    private static final String BRAT_EVENT_RENDER_DATA_PATCH = "renderDataPatch";
    private static final String BRAT_EVENT_RENDER_DATA = "renderData";

    private static final long serialVersionUID = -1537506294440056609L;

    private @SpringBean BratMetrics metrics;
    private @SpringBean BratAnnotationEditorProperties bratProperties;
    private @SpringBean BratSerializer bratSerializer;
    private @SpringBean BratSchemaGenerator bratSchemaGenerator;
    private @SpringBean ServletContext servletContext;

    private GetCollectionInformationHandler collectionInformationHandler;

    private DifferentialRenderingSupport diffRenderSupport;

    public BratAnnotationEditor(String id, IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider,
            String aEditorFactoryId)
    {
        super(id, aModel, aActionHandler, aCasProvider, aEditorFactoryId);

        add(visibleWhen(getModel().map(AnnotatorState::getProject).isPresent()));

        diffRenderSupport = new DifferentialRenderingSupport(metrics);
    }

    @Override
    protected WebMarkupContainer makeView()
    {
        return new WebMarkupContainer(CID_VIS);
    }

    @Override
    protected DiamAjaxBehavior createDiamBehavior()
    {
        var vis = getViewComponent();
        collectionInformationHandler = new GetCollectionInformationHandler(vis,
                bratSchemaGenerator);

        var diam = super.createDiamBehavior();
        diam.addPriorityHandler(new LoadConfHandler(vis, bratProperties));
        diam.addPriorityHandler(collectionInformationHandler);
        diam.addPriorityHandler(new GetDocumentHandler(vis));
        return diam;
    }

    @Override
    protected AnnotationEditorProperties getProperties()
    {
        var props = super.getProperties();
        // The factory is the JS call. Cf. the "globalName" in build.js and the factory method
        // defined in main.ts
        props.setEditorFactory("Brat.factory()");
        props.setStylesheetSources(asList(referenceToUrl(servletContext, BratCssReference.get())));
        props.setScriptSources(asList(referenceToUrl(servletContext, BratResourceReference.get())));
        return props;
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        var cmdQueue = QueuedEditorCommandsMetaDataKey.get();
        var collInfo = collectionInformationHandler.getCollectionInformation(getModelObject());
        cmdQueue.add(new LoadCollectionCommand(collInfo));
        cmdQueue.add(new LoadAnnotationsCommand());

        super.renderHead(aResponse);
    }

    @Override
    protected EditorCommand renderCommand()
    {
        return new PushAnnotationsCommand();
    }

    private GetDocumentResponse render(CAS aCas)
    {
        var aState = getModelObject();
        return render(aCas, aState.getWindowBeginOffset(), aState.getWindowEndOffset(),
                bratSerializer);
    }

    @Order(100)
    private class PushAnnotationsCommand
        implements EditorCommand
    {
        private static final long serialVersionUID = -4921356927811675492L;

        @Override
        public String command(String aEditorVariable)
        {
            try {
                var bratDocModel = render(getCasProvider().get());
                return diffRenderSupport.differentialRendering(bratDocModel).map(rr -> {
                    var js = new StringBuilder();

                    js.append("{");

                    if (bratProperties.isClientSideProfiling()) {
                        js.append("Util.profileEnable(true);");
                        js.append("Util.profileClear();");
                    }

                    js.append(format("e.post('%s', [%s]);", //
                            rr.getRenderType() == FULL //
                                    ? BRAT_EVENT_RENDER_DATA //
                                    : BRAT_EVENT_RENDER_DATA_PATCH, //
                            rr.getJsonStr()));

                    if (bratProperties.isClientSideProfiling()) {
                        js.append("Util.profileReport();");
                    }

                    js.append("}");

                    return js.toString();
                }).orElse("{}");
            }
            catch (IOException e) {
                handleError("Unable to load data", e);
            }
            catch (Exception e) {
                handleError("Unable to render document", e);
            }

            return "{}";
        }
    }

    private class GetDocumentHandler
        extends EditorAjaxRequestHandlerBase
        implements Serializable
    {

        private static final long serialVersionUID = 1601968431851817445L;

        private final Component vis;

        public GetDocumentHandler(Component aVis)
        {
            vis = aVis;
        }

        @Override
        public String getCommand()
        {
            return GetDocumentResponse.COMMAND;
        }

        @Override
        public AjaxResponse handle(DiamAjaxBehavior aBehavior, AjaxRequestTarget aTarget,
                Request aRequest)
        {
            try {
                var cas = getCasProvider().get();
                var response = render(cas);
                var result = diffRenderSupport.fullRendering(response).getJsonStr();
                BratRequestUtils.attachResponse(aTarget, vis, result);
                return new DefaultAjaxResponse(getAction(aRequest));
            }
            catch (Exception e) {
                return handleError("Unable to load annotations", e);
            }
        }
    }
}
