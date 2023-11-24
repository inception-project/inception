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

import static de.tudarmstadt.ukp.inception.bootstrap.BootstrapFileInputField.initConfig;

import java.util.List;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptReferenceHeaderItem;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.IModel;

import de.agilecoders.wicket.core.Bootstrap;
import de.agilecoders.wicket.core.settings.IBootstrapSettings;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.FileInputConfig;

/**
 * @deprecated Use
 *             {@link de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.BootstrapFileInput}
 *             instead.
 */
@Deprecated
public class BootstrapFileInput
    extends de.agilecoders.wicket.extensions.markup.html.bootstrap.form.fileinput.BootstrapFileInput
{
    private static final long serialVersionUID = 7152684712265476472L;

    public BootstrapFileInput(String aId, IModel<List<FileUpload>> aModel, FileInputConfig aConfig)
    {
        super(aId, aModel, aConfig);
        initConfig(getConfig());
    }

    public BootstrapFileInput(String aId, IModel<List<FileUpload>> aModel)
    {
        super(aId, aModel);
        initConfig(getConfig());
    }

    public BootstrapFileInput(String aId)
    {
        super(aId);
        initConfig(getConfig());
    }

    @Override
    public void renderHead(final IHeaderResponse response)
    {
        // Workaround https://github.com/l0rdn1kk0n/wicket-bootstrap/issues/957
        IBootstrapSettings settings = Bootstrap.getSettings(getApplication());
        response.render(
                JavaScriptReferenceHeaderItem.forReference(settings.getJsResourceReference()));
        super.renderHead(response);
    }
}
