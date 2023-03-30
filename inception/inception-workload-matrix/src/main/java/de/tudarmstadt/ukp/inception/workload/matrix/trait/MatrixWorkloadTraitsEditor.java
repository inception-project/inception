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
package de.tudarmstadt.ukp.inception.workload.matrix.trait;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtension;
import de.tudarmstadt.ukp.inception.workload.extension.WorkloadManagerExtensionPoint;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

public class MatrixWorkloadTraitsEditor
    extends Panel
{
    private static final long serialVersionUID = 9150032039791522664L;

    private static final String MID_FORM = "form";
    private static final String MID_REOPENABLE_BY_ANNOTATOR = "reopenableByAnnotator";
    private static final String MID_RANDOM_DOCUMENT_ACCESS_ALLOWED = "randomDocumentAccessAllowed";

    private @SpringBean WorkloadManagerExtensionPoint workloadManagerExtensionPoint;

    private IModel<WorkloadManager> workloadManager;
    private CompoundPropertyModel<MatrixWorkloadTraits> model;

    public MatrixWorkloadTraitsEditor(String aId, IModel<WorkloadManager> aWorkloadManager)
    {
        super(aId, aWorkloadManager);

        workloadManager = aWorkloadManager;

        model = CompoundPropertyModel
                .of(getWorkloadManagerExtension().readTraits(workloadManager.getObject()));

        var form = new Form<MatrixWorkloadTraits>(MID_FORM, model)
        {
            private static final long serialVersionUID = 2781481443442601171L;

            @Override
            protected void onInitialize()
            {
                super.onInitialize();

                CheckBox randomDocumentAccessAllowed = new CheckBox(
                        MID_RANDOM_DOCUMENT_ACCESS_ALLOWED);
                randomDocumentAccessAllowed.setOutputMarkupId(true);
                queue(randomDocumentAccessAllowed);

                queue(new CheckBox("documentResetAllowed").setOutputMarkupId(true));

                CheckBox reopenableByAnnotator = new CheckBox(MID_REOPENABLE_BY_ANNOTATOR);
                reopenableByAnnotator.setOutputMarkupId(true);
                queue(reopenableByAnnotator);
            }

            @Override
            protected void onSubmit()
            {
                super.onSubmit();
                getWorkloadManagerExtension().writeTraits(workloadManager.getObject(),
                        model.getObject());
            }
        };
        form.setOutputMarkupPlaceholderTag(true);
        add(form);
    }

    @SuppressWarnings("unchecked")
    private WorkloadManagerExtension<MatrixWorkloadTraits> getWorkloadManagerExtension()
    {
        return (WorkloadManagerExtension<MatrixWorkloadTraits>) workloadManagerExtensionPoint
                .getExtension(workloadManager.getObject().getType()).orElseThrow();
    }
}
