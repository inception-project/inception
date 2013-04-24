/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.project;

import org.apache.wicket.IConverterLocator;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import de.tudarmstadt.ukp.clarin.webanno.webapp.page.ApplicationPageBase;
/**
 * Implementing the settings for the project BasePage
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
public abstract class SettingsPageBase
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 102508640275351772L;

    protected SettingsPageBase()
    {
    }

    protected SettingsPageBase(final PageParameters parameters)
    {
        super(parameters);
    }

    protected IConverterLocator newConverterLocator()
    {
        return null;
    }
}
