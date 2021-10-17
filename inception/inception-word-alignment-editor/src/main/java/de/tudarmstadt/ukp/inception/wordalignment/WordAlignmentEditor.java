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
package de.tudarmstadt.ukp.inception.wordalignment;

import javax.servlet.ServletContext;

import de.tudarmstadt.ukp.inception.wordalignment.resources.WebsocketAPIReference;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.inception.wordalignment.resources.VisualizationReference;
import de.tudarmstadt.ukp.inception.wordalignment.resources.WordAlignmentEditorReference;

public class WordAlignmentEditor
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = -3812646280364767142L;

    private @SpringBean ServletContext servletContext;
    private AbstractAjaxBehavior requestHandler;

    public WordAlignmentEditor(String aId, IModel<AnnotatorState> aModel,
            final AnnotationActionHandler aActionHandler, final CasProvider aCasProvider)
    {
        super(aId, aModel, aActionHandler, aCasProvider);



        requestHandler = new AbstractDefaultAjaxBehavior()
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void respond(AjaxRequestTarget aTarget)
            {
                if (getModelObject().getDocument() == null) {
                    return;
                }


                long timerStart = System.currentTimeMillis();

                final IRequestParameters request = getRequest().getPostParameters();

                System.out.println(request);

                try {
                    /*
                    // Whenever an action should be performed, do ONLY perform this action and
                    // nothing else, and only if the item actually is an action item
                    if (NormDataResponse.is(action)) {
                        AnnotatorState state = getModelObject();
                        result = lazyDetailsLookupService.actionLookupNormData(request, paramId,
                            getCasProvider(), state.getDocument(), state.getUser());
                    }
                    else if (DoActionResponse.is(action)) {
                        if (paramId.isSynthetic()) {
                            extensionRegistry.fireAction(getActionHandler(), getModelObject(),
                                aTarget, cas, paramId, action);
                        }
                        else {
                            actionDoAction(aTarget, request, cas, paramId);
                        }
                    }
                    else {
                        if (paramId.isSynthetic()) {
                            extensionRegistry.fireAction(getActionHandler(), getModelObject(),
                                aTarget, cas, paramId, action);
                        }
                        else {
                            // Doing anything but selecting or creating a span annotation when a
                            // slot is armed will unarm it
                            if (getModelObject().isSlotArmed()
                                && !SpanAnnotationResponse.is(action)) {
                                getModelObject().clearArmedSlot();
                            }

                            if (ACTION_CONTEXT_MENU.equals(action.toString())
                                && !paramId.isSlotSet()) {
                                actionOpenContextMenu(aTarget, request, cas, paramId);
                            }
                            else if (SpanAnnotationResponse.is(action)) {
                                result = actionSpan(aTarget, request, cas, paramId);
                            }
                            else if (ArcAnnotationResponse.is(action)) {
                                result = actionArc(aTarget, request, cas, paramId);
                            }
                            else if (LoadConfResponse.is(action)) {
                                result = new LoadConfResponse(bratProperties);
                            }
                            else if (GetCollectionInformationResponse.is(action)) {
                                result = actionGetCollectionInformation();
                            }
                            else if (GetDocumentResponse.is(action)) {
                                result = actionGetDocument(cas);
                            }
                        }
                    }

                     */
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                /*
                // Serialize updated document to JSON
                if (result == null) {
                    LOG.trace("AJAX-RPC: Action [{}] produced no result!", action);
                }
                else {
                    try {
                        BratRequestUtils.attachResponse(aTarget, vis, result);
                    }
                    catch (IOException e) {
                        handleError("Unable to produce JSON response", e);
                    }
                }

                long duration = System.currentTimeMillis() - timerStart;
                LOG.trace("AJAX-RPC DONE: [{}] completed in {}ms", action, duration);

                serverTiming("Brat-AJAX", "Brat-AJAX (" + action + ")", duration);

                 */
            }
        };

        add(requestHandler);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);
        aResponse.render(JavaScriptHeaderItem.forReference(WebsocketAPIReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(WordAlignmentEditorReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(VisualizationReference.get()));
    }


    @Override
    protected void render(AjaxRequestTarget aTarget)
    {
        // Nothing to do
    }
}
