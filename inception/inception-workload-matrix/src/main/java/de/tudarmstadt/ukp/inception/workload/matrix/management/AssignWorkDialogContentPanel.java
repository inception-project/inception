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
package de.tudarmstadt.ukp.inception.workload.matrix.management;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;

import de.tudarmstadt.ukp.inception.support.lambda.AjaxFormCallback;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxSubmitLink;

public class AssignWorkDialogContentPanel
    extends Panel
{
    private static final long serialVersionUID = 4180353033097308419L;

    public AssignWorkDialogContentPanel(String aId,
            AjaxFormCallback<AssignWorkRequest> aConfirmCallback)
    {
        super(aId);

        queue(new Form<>("form", CompoundPropertyModel.of(new AssignWorkRequest())));

        queue(new NumberTextField<Integer>("annotatorsPerDocument").setMinimum(1).setMaximum(100)
                .setOutputMarkupId(true));

        queue(new CheckBox("override").setOutputMarkupId(true));

        queue(new LambdaAjaxSubmitLink<>("assign", aConfirmCallback));
        queue(new LambdaAjaxLink("close", this::actionClose));
        queue(new LambdaAjaxLink("cancel", this::actionClose));
    }

    private void actionClose(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }

    public static final class AssignWorkRequest
        implements Serializable
    {
        private static final long serialVersionUID = 1111050546393629498L;

        private int annotatorsPerDocument = 3;
        private boolean override = false;

        public int getAnnotatorsPerDocument()
        {
            return annotatorsPerDocument;
        }

        public void setAnnotatorsPerDocument(int aAnnotatorsPerDocument)
        {
            annotatorsPerDocument = aAnnotatorsPerDocument;
        }

        public boolean isOverride()
        {
            return override;
        }

        public void setOverride(boolean aOverride)
        {
            override = aOverride;
        }
    }
}
