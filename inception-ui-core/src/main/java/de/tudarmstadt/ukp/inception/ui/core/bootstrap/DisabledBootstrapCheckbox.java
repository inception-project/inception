/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.ui.core.bootstrap;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;

import de.agilecoders.wicket.core.markup.html.bootstrap.form.BootstrapCheckbox;

/**
 * Extension of {@link BootstrapCheckbox} which can be disabled in the frontend.<br>
 * The input tag of the checkbox will receive the attribute {@code disabled="disabled"}.
 */
public class DisabledBootstrapCheckbox extends BootstrapCheckbox {

    private static final long serialVersionUID = -202721702026002675L;

    public DisabledBootstrapCheckbox(String id, IModel<Boolean> model, IModel<?> labelModel) {
        // MB: Unfortunately, we cannot write a "disableable" checkbox: newCheckBox(...) is called
        // in super(), and at that point in time, attributes of this class (like "boolean disabled")
        // are not defined yet...
        super(id, model, labelModel);
    }

    @Override
    protected CheckBox newCheckBox(String id, IModel<Boolean> model) {
        CheckBox checkBox = super.newCheckBox(id, model);
        checkBox.add(new AttributeAppender("disabled", "disabled"));
        return checkBox;
    }

}
