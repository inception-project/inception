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
package de.tudarmstadt.ukp.clarin.webanno.telemetry.ui;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetryDetail;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetryService;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetrySupport;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.model.TelemetrySettings;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

@AuthorizeInstantiation("ROLE_ADMIN")
@MountPath("/telemetry.html")
public class TelemetrySettingsPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = -3257036055275434572L;

    private @SpringBean TelemetryService telemetryService;

    private IModel<List<TelemetrySettings>> settingModel;

    public TelemetrySettingsPage()
    {
        Form<Void> form = new Form<>("form");

        settingModel = new ListModel<TelemetrySettings>(listSettings());

        ListView<TelemetrySettings> settings = new ListView<TelemetrySettings>("settings")
        {
            private static final long serialVersionUID = 7433492093706423431L;

            @Override
            protected void populateItem(ListItem<TelemetrySettings> aItem)
            {
                // We already filtered for settings where the support exists in listSettings, so
                // we can rely on the support being present here.
                TelemetrySupport<?> support = telemetryService
                        .getTelemetrySuppport(aItem.getModelObject().getSupport()).get();

                int version = support.getVersion();

                aItem.add(new Label("name", support.getName()));

                aItem.add(support.createTraitsEditor("traitsEditor", aItem.getModel()));

                aItem.add(new WebMarkupContainer("reviewRequiredMessage")
                        .add(visibleWhen(() -> aItem.getModelObject().getVersion() < version)));

                TelemetryDetailsPanel details = new TelemetryDetailsPanel("details",
                        new ListModel<TelemetryDetail>(support.getDetails()));
                details.setOutputMarkupPlaceholderTag(true);
                details.setVisible(false);
                aItem.add(details);

                aItem.add(new LambdaAjaxLink("toggleDetails", _target -> {
                    details.setVisible(!details.isVisible());
                    _target.add(details);
                }));
            }
        };
        settings.setModel(settingModel);
        form.add(settings);

        form.add(new LambdaAjaxButton<Void>("save", this::actionSave).triggerAfterSubmit());

        add(form);
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<Void> aForm)
    {
        // Update the versions of the saved settings
        for (TelemetrySettings settings : settingModel.getObject()) {
            // We already filtered for settings where the support exists in listSettings, so
            // we can rely on the support being present here.
            TelemetrySupport<?> support = telemetryService
                    .getTelemetrySuppport(settings.getSupport()).get();
            settings.setVersion(support.getVersion());
        }

        // The individual traits editors are responsible for committing their content to the traits
        // objects and for committing the traits to the TelemetrySettings object. So here, we only
        // update the TelemetrySettings objects in the DB.
        telemetryService.writeAllSettings(settingModel.getObject());

        // If saving was successful, redirect to the home page
        setResponsePage(getApplication().getHomePage());
    }

    private List<TelemetrySettings> listSettings()
    {
        List<TelemetrySettings> settings = new ArrayList<>();

        for (TelemetrySupport<?> support : telemetryService.getTelemetrySupports()) {
            settings.add(telemetryService.readOrCreateSettings(support));
        }

        return settings;
    }
}
