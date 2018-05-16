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
package de.tudarmstadt.ukp.inception.ui.kb;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

/**
 * Behavior which allows a component to be visible only if the given {@link KnowledgeBase} is
 * <strong>not</strong> write protected.
 */
public class WriteProtectionBehavior extends Behavior {

    private static final long serialVersionUID = -1872816229304015030L;

    private IModel<KnowledgeBase> kbModel;

    public WriteProtectionBehavior(IModel<KnowledgeBase> aKbModel) {
        kbModel = aKbModel;
    }

    @Override
    public void onConfigure(Component component) {
        component.setVisibilityAllowed(!kbModel.getObject().isReadOnly());
        super.onConfigure(component);
    }
}
