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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.support.lambda.AjaxCallback;

/**
 * Dialog providing access to the per-user annotation preferences.
 */
public class AnnotationPreferencesDialog
    extends BootstrapModalDialog
{
    private static final long serialVersionUID = -6911254813496835955L;

    private IModel<AnnotatorState> state;

    private AjaxCallback onChangeAction;

    public AnnotationPreferencesDialog(String id, final IModel<AnnotatorState> aModel)
    {
        super(id);

        state = aModel;

        trapFocus();
    }

    public void show(AjaxRequestTarget aTarget)
    {
        var content = new AnnotationPreferencesDialogContent(CONTENT_ID, state, onChangeAction);

        open(content, aTarget);
    }

    public AjaxCallback getOnChangeAction()
    {
        return onChangeAction;
    }

    public void setOnChangeAction(AjaxCallback aOnChangeAction)
    {
        onChangeAction = aOnChangeAction;
    }
}
