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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.open;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.model.IModel;
import org.danekja.java.util.function.serializable.SerializableBiFunction;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

public class OpenDocumentDialog
    extends BootstrapModalDialog
{
    private static final long serialVersionUID = 2767538203924633288L;

    private final SerializableBiFunction<Project, User, List<AnnotationDocument>> docListProvider;
    private final IModel<AnnotatorState> state;

    public OpenDocumentDialog(String aId, IModel<AnnotatorState> aModel,
            SerializableBiFunction<Project, User, List<AnnotationDocument>> aDocListProvider)
    {
        super(aId);
        setOutputMarkupId(true);
        trapFocus();

        docListProvider = aDocListProvider;
        state = aModel;
    }

    public void show(AjaxRequestTarget aTarget)
    {
        var content = new OpenDocumentDialogPanel(ModalDialog.CONTENT_ID, state, docListProvider);
        super.open(content, aTarget);
    }
}
