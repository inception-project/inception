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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.casdoctor;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractChoice.LabelPosition;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.diag.ChecksRegistry;
import de.tudarmstadt.ukp.clarin.webanno.diag.RepairsRegistry;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentStorageService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class ProjectCasDoctorPanel
    extends ProjectSettingsPanelBase
{
    private final static Logger LOG = LoggerFactory.getLogger(ProjectCasDoctorPanel.class);

    private static final long serialVersionUID = 2116717853865353733L;

    private @SpringBean DocumentService documentService;
    private @SpringBean DocumentStorageService documentStorageService;
    private @SpringBean CasStorageService casStorageService;
    private @SpringBean DocumentImportExportService importExportService;
    private @SpringBean RepairsRegistry repairsRegistry;
    private @SpringBean ChecksRegistry checksRegistry;
    private @SpringBean SchedulingService schedulingService;
    private @SpringBean UserDao userService;

    // Data properties
    private FormModel formModel = new FormModel();

    public ProjectCasDoctorPanel(String id, IModel<Project> aProjectModel)
    {
        super(id, aProjectModel);

        setOutputMarkupId(true);

        var form = new Form<FormModel>("casDoctorForm", PropertyModel.of(this, "formModel"));
        add(form);

        var repairs = new CheckBoxMultipleChoice<String>("repairs");
        repairs.setModel(PropertyModel.of(this, "formModel.repairs"));
        repairs.setChoices(repairsRegistry.getExtensions().stream() //
                .map(r -> r.getId()).collect(toList()));
        repairs.setPrefix("<div class=\"checkbox\">");
        repairs.setSuffix("</div>");
        repairs.setLabelPosition(LabelPosition.WRAP_AFTER);
        form.add(repairs);

        form.add(new LambdaAjaxButton<FormModel>("check", this::actionCheck));
        form.add(new LambdaAjaxButton<FormModel>("repair", this::actionRepair));
        add(createMessageSetsView());
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        formModel = new FormModel();
    }

    private ListView<LogMessageSet> createMessageSetsView()
    {
        return new ListView<LogMessageSet>("messageSets",
                PropertyModel.of(this, "formModel.messageSets"))
        {
            private static final long serialVersionUID = 8957632000765128508L;

            @Override
            protected void populateItem(ListItem<LogMessageSet> aItem)
            {
                IModel<LogMessageSet> set = aItem.getModel();
                aItem.add(new Label("name", PropertyModel.of(set, "name")));
                aItem.add(createMessagesView(set));
            }
        };
    }

    private ListView<LogMessage> createMessagesView(IModel<LogMessageSet> aModel)
    {
        return new ListView<LogMessage>("messages", PropertyModel.of(aModel, "messages"))
        {
            private static final long serialVersionUID = 8957632000765128508L;

            @Override
            protected void populateItem(ListItem<LogMessage> aItem)
            {
                IModel<LogMessage> msg = aItem.getModel();
                aItem.add(new Label("level", PropertyModel.of(msg, "level")));
                aItem.add(new Label("source", PropertyModel.of(msg, "source")));
                aItem.add(new Label("message", PropertyModel.of(msg, "message")));
            }
        };
    }

    private void actionRepair(AjaxRequestTarget aTarget, Form<?> aForm)
        throws IOException, UIMAException, ClassNotFoundException
    {
        var repairTask = RepairTask.builder() //
                .withSessionOwner(userService.getCurrentUser()) //
                .withProject(getModelObject()) //
                .withRepairs(formModel.repairs) //
                .withTrigger("User request") //
                .build();

        schedulingService.executeSync(repairTask);

        formModel.messageSets = repairTask.getMessageSets();

        aTarget.add(this);
    }

    private void actionCheck(AjaxRequestTarget aTarget, Form<?> aForm)
        throws IOException, UIMAException, ClassNotFoundException
    {
        var checks = checksRegistry.getExtensions().stream().map(c -> c.getId()).toList();
        var checkTask = CheckTask.builder() //
                .withSessionOwner(userService.getCurrentUser()) //
                .withProject(getModelObject()) //
                .withTrigger("User request") //
                .withChecks(checks) //
                .build();

        schedulingService.executeSync(checkTask);

        formModel.messageSets = checkTask.getMessageSets();

        var objectCount = checkTask.getObjectCount();
        if (objectCount > 0) {
            info("Applied " + checks.size() + " checks to " + objectCount
                    + " annotation objects - see report for details");
        }
        else {
            warn("Project does not contain any annotation objects that can be checked");
        }

        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(this);
    }

    private class FormModel
        implements Serializable
    {
        private static final long serialVersionUID = 5421427363671176637L;

        @SuppressWarnings("unused")
        private List<LogMessageSet> messageSets = new ArrayList<>();
        private List<String> repairs;

        {
            // Fetch only the safe/non-destructive repairs
            repairs = repairsRegistry.getExtensions().stream() //
                    .filter(r -> {
                        Safe s = r.getClass().getAnnotation(Safe.class);
                        return s != null && s.value();
                    }) //
                    .map(r -> r.getId()) //
                    .collect(toList());
        }
    }
}
