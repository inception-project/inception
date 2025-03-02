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
package de.tudarmstadt.ukp.inception.bootstrap;

import static org.apache.wicket.event.Broadcast.BUBBLE;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.wicketstuff.event.annotation.AbstractAjaxAwareEvent;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.IconType;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkboxx.CheckBoxX;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkboxx.CheckBoxXConfig;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;

public class IconToggleBox
    extends GenericPanel<Boolean>
{
    private static final long serialVersionUID = 4721646397508723919L;

    private IconType checked = FontAwesome5IconType.check_s;
    private IconType unchecked = NoIcon.NO_ICON;
    private IModel<String> checkedTitle = Model.of();
    private IModel<String> uncheckedTitle = Model.of();
    private IModel<String> postLabelText = Model.of();
    private CheckBoxX checkBox;
    private Label postLabel;

    public IconToggleBox(String aId)
    {
        this(aId, null);
    }

    public IconToggleBox(String aId, IModel<Boolean> aModel)
    {
        super(aId, aModel);

        setOutputMarkupId(true);

        queue(checkBox = new CheckBoxX("checkbox")
        {
            private static final long serialVersionUID = -5843739263020826942L;

            @Override
            protected void onChange(Boolean aValue, AjaxRequestTarget aTarget)
            {
                aTarget.add(IconToggleBox.this);
                IconToggleBox.this.send(IconToggleBox.this, BUBBLE,
                        new IconToggleBoxChangedEvent(aTarget, IconToggleBox.this));
            }
        });
        queue(postLabel = new Label("postLabel"));
    }

    @Override
    public MarkupContainer setDefaultModel(IModel<?> aModel)
    {
        return super.setDefaultModel(aModel);
    }

    @Override
    public IconToggleBox setModel(IModel<Boolean> aModel)
    {
        this.setDefaultModel(aModel);
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public IModel<Boolean> getModel()
    {
        return (IModel<Boolean>) getDefaultModel();
    }

    public IconToggleBox setUncheckedIcon(IconType aUnchecked)
    {
        unchecked = aUnchecked;
        return this;
    }

    public IconToggleBox setCheckedIcon(IconType aChecked)
    {
        checked = aChecked;
        return this;
    }

    public IconToggleBox setCheckedTitle(IModel<String> aCheckedTitle)
    {
        checkedTitle = aCheckedTitle;
        return this;
    }

    public IconToggleBox setUncheckedTitle(IModel<String> aUncheckedTitle)
    {
        uncheckedTitle = aUncheckedTitle;
        return this;
    }

    public IconToggleBox setPostLabelText(IModel<String> aPostLabelText)
    {
        postLabelText = aPostLabelText;
        return this;
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        checkBox.setModel(getModel());
        postLabel.setDefaultModel(postLabelText);

        add(AttributeAppender.replace("title", () -> {
            if (checkBox.getModelObject()) {
                return checkedTitle != null ? checkedTitle.getObject() : null;
            }
            else {
                return uncheckedTitle != null ? uncheckedTitle.getObject() : null;
            }
        }));
    }

    @Override
    public IconToggleBox add(Behavior... aBehaviors)
    {
        for (Behavior b : aBehaviors) {
            if (b instanceof FormComponentUpdatingBehavior
                    || b instanceof AjaxFormComponentUpdatingBehavior) {
                checkBox.add(b);
            }
            else {
                super.add(b);
            }
        }
        return this;
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();
        CheckBoxXConfig config = checkBox.getConfig();
        config.withIconChecked(renderIcon(checked));
        config.withIconUnchecked(renderIcon(unchecked));
        config.withEnclosedLabel(true);
        config.withThreeState(false);
    }

    private String renderIcon(IconType aIcon)
    {
        var body = aIcon.getTagBody();
        if (body != null) {
            return body;
        }
        else {
            return "<i class=\"" + aIcon.cssClassName() + "\"></i>";
        }
    }

    public static class IconToggleBoxChangedEvent
        extends AbstractAjaxAwareEvent
    {
        private final IconToggleBox source;

        public IconToggleBoxChangedEvent(AjaxRequestTarget aTarget, IconToggleBox aSource)
        {
            super(aTarget);

            source = aSource;
        }

        public IconToggleBox getSource()
        {
            return source;
        }
    }
}
