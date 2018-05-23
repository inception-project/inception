/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.app.bootstrap;

import org.apache.wicket.extensions.wizard.AjaxWizardButtonBar;
import org.apache.wicket.extensions.wizard.Wizard;

public class BootstrapWizardButtonBar extends AjaxWizardButtonBar {

    private static final long serialVersionUID = 7136194479524182883L;

    public BootstrapWizardButtonBar(String id, Wizard wizard) {
        super(id, wizard);
    }
}
