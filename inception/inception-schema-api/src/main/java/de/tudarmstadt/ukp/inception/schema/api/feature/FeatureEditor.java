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
package de.tudarmstadt.ukp.inception.schema.api.feature;

import static org.apache.wicket.event.Broadcast.BUBBLE;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;

/**
 * Base class for feature editors. This component sends {@link FeatureEditorValueChangedEvent}
 * events when the value updates. It is typical for multiple editors to live in one contained which
 * then listens to this event and updates the CAS if necessary.
 */
public abstract class FeatureEditor
    extends GenericPanel<FeatureState>
{
    private static final long serialVersionUID = -7275181609671919722L;

    protected static final String MID_FEATURE = "feature";
    protected static final String MID_VALUE = "value";

    private MarkupContainer owner;

    /**
     * @param aId
     *            the component ID.
     * @param aOwner
     *            an enclosing component which may contain other feature editors. If actions are
     *            performed which may affect other feature editors, e.g because of constraints
     *            rules, then these need to be re-rendered. This is done by requesting a
     *            re-rendering of the enclosing component.
     * @param aModel
     *            provides access to the state of the feature being edited.
     */
    public FeatureEditor(String aId, MarkupContainer aOwner, IModel<FeatureState> aModel)
    {
        super(aId, aModel);
        owner = aOwner;

        add(createLabel());
    }

    public MarkupContainer getOwner()
    {
        return owner;
    }

    public Component getLabelComponent()
    {
        return get("feature");
    }

    private Component createLabel()
    {
        return new Label(MID_FEATURE, getModelObject().feature.getUiName());
    }

    public void addFeatureUpdateBehavior()
    {
        FormComponent<?> focusComponent = getFocusComponent();
        focusComponent.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = -8944946839865527412L;

            @Override
            protected void onUpdate(AjaxRequestTarget aTarget)
            {
                send(focusComponent, BUBBLE,
                        new FeatureEditorValueChangedEvent(FeatureEditor.this, aTarget));
            }
        });
    }

    abstract public FormComponent<?> getFocusComponent();
}
