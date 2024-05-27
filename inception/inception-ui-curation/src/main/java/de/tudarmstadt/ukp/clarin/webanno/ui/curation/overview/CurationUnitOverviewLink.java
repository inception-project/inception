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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.overview;

import static de.tudarmstadt.ukp.inception.support.wicket.WicketUtil.wrapInTryCatch;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;

import de.tudarmstadt.ukp.clarin.webanno.ui.curation.event.CurationUnitClickedEvent;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public class CurationUnitOverviewLink
    extends AjaxLink<CurationUnit>
{
    private static final long serialVersionUID = 4558300090461815010L;

    private static final String ATTR_CLASS = "class";

    private static final String CSS_CLASS_OUT_RANGE = "out-range";
    private static final String CSS_CLASS_IN_RANGE = "in-range";
    private static final String CSS_CLASS_CURRENT = "current";

    private IModel<AnnotatorState> annotatorState;

    public CurationUnitOverviewLink(String aId, IModel<CurationUnit> aModel,
            IModel<AnnotatorState> aAnnotatorState)
    {
        super(aId, aModel);
        setBody(Model.of(aModel.getObject().getUnitIndex().toString()));
        annotatorState = aAnnotatorState;
    }

    @Override
    protected void onComponentTag(ComponentTag aTag)
    {
        super.onComponentTag(aTag);

        final CurationUnit unitState = getModelObject();
        final AnnotatorState state = annotatorState.getObject();

        aTag.append(ATTR_CLASS, unitState.getState().getCssClass(), " ");

        // Is in focus?
        if (unitState.getUnitIndex() == state.getFocusUnitIndex()) {
            aTag.append(ATTR_CLASS, CSS_CLASS_CURRENT, " ");
        }

        // In range or not?
        if (unitState.getUnitIndex() >= state.getFirstVisibleUnitIndex()
                && unitState.getUnitIndex() <= state.getLastVisibleUnitIndex()) {
            aTag.append(ATTR_CLASS, CSS_CLASS_IN_RANGE, " ");
        }
        else {
            aTag.append(ATTR_CLASS, CSS_CLASS_OUT_RANGE, " ");
        }
    }

    @Override
    protected void onAfterRender()
    {
        super.onAfterRender();

        // The sentence list is refreshed using AJAX. Unfortunately, the renderHead() method
        // of the AjaxEventBehavior created by AjaxLink does not seem to be called by Wicket
        // during an AJAX rendering, causing the sentence links to loose their functionality.
        // Here, we ensure that the callback scripts are attached to the sentence links even
        // during AJAX updates.
        if (isEnabledInHierarchy()) {
            RequestCycle.get().find(AjaxRequestTarget.class).ifPresent(_target -> {
                for (AjaxEventBehavior b : getBehaviors(AjaxEventBehavior.class)) {
                    _target.appendJavaScript(wrapInTryCatch(b.getCallbackScript()));
                }
            });
        }
    }

    @Override
    public void onClick(AjaxRequestTarget aTarget)
    {
        send(this, BUBBLE, new CurationUnitClickedEvent(aTarget, getModelObject()));
    }
}
