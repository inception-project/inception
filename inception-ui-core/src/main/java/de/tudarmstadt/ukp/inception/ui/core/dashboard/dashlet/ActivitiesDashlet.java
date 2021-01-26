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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.dashlet;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.ui.core.LinkProvider.createDocumentPageLink;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.SetUtils.unmodifiableSet;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.ChainLinkCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.ChainLinkDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.ChainSpanCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.ChainSpanDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.RelationCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.RelationDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.CurationDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.CurationPage;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;

public class ActivitiesDashlet
    extends Dashlet_ImplBase
{
    private static final long serialVersionUID = -2010294259619748756L;

    private static final Logger log = LoggerFactory.getLogger(ActivitiesDashlet.class);

    private static final int MAX_NUM_ACTIVITIES = 10;

    private @SpringBean EventRepository eventRepository;
    private @SpringBean UserDao userRepository;
    private @SpringBean DocumentService documentService;
    private @SpringBean CurationDocumentService curationService;
    private @SpringBean ProjectService projectService;

    private final IModel<Project> projectModel;

    private final Set<String> annotationEvents = unmodifiableSet( //
            SpanCreatedEvent.class.getSimpleName(), //
            SpanDeletedEvent.class.getSimpleName(), //
            RelationCreatedEvent.class.getSimpleName(), //
            RelationDeletedEvent.class.getSimpleName(), //
            ChainLinkCreatedEvent.class.getSimpleName(), //
            ChainLinkDeletedEvent.class.getSimpleName(), //
            ChainSpanCreatedEvent.class.getSimpleName(), //
            ChainSpanDeletedEvent.class.getSimpleName(), //
            FeatureValueUpdatedEvent.class.getSimpleName(), //
            "DocumentMetadataCreatedEvent", //
            "DocumentMetadataDeletedEvent");

    public ActivitiesDashlet(String aId, IModel<Project> aCurrentProject)
    {
        super(aId);

        projectModel = aCurrentProject;

        if (aCurrentProject == null || aCurrentProject.getObject() == null) {
            return;
        }

        WebMarkupContainer activitiesList = new WebMarkupContainer("activities",
                new StringResourceModel("activitiesHeading", this));
        activitiesList.setOutputMarkupPlaceholderTag(true);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

        ListView<Pair<LoggedEvent, SourceDocument>> listView = new ListView<Pair<LoggedEvent, SourceDocument>>(
                "activity", LoadableDetachableModel.of(this::listActivities))
        {
            private static final long serialVersionUID = -8613360620764882858L;

            @Override
            protected void populateItem(ListItem<Pair<LoggedEvent, SourceDocument>> aItem)
            {
                LoggedEvent event = aItem.getModelObject().getKey();
                SourceDocument doc = aItem.getModelObject().getValue();

                aItem.add(createLastActivityLink("eventLink", event, doc));

                aItem.add(new Label("type",
                        CURATION_USER.equals(event.getAnnotator()) ? "Curation" : "Annotation"));

                aItem.add(new Label("user", event.getAnnotator()).add(visibleWhen(
                        () -> !CURATION_USER.equals(event.getAnnotator()) && !userRepository
                                .getCurrentUsername().equals(event.getAnnotator()))));

                aItem.add(new Label("timestamp", formatDateStr(event)).add(
                        AttributeModifier.replace("title", dateFormat.format(event.getCreated()))));
            }
        };

        add(visibleWhen(() -> !listView.getList().isEmpty()));
        setOutputMarkupPlaceholderTag(true);
        activitiesList.add(listView);
        add(activitiesList);
    }

    private String formatDateStr(LoggedEvent event)
    {
        long timeDifference = currentTimeMillis() - event.getCreated().toInstant().toEpochMilli();

        // Less than a minute?
        if (timeDifference < 60 * 1000) {
            return "seconds ago";
        }

        // Less than an hour?
        if (timeDifference < 60 * 60 * 1000) {
            return format("%d minutes ago", timeDifference / (60 * 1000));
        }

        // Less than a day?
        if (timeDifference < 24 * 60 * 60 * 1000) {
            return format("%d hours ago", timeDifference / (60 * 60 * 1000));
        }

        return format("%d days ago", timeDifference / (24 * 60 * 60 * 1000));
    }

    private ExternalLink createLastActivityLink(String aId, LoggedEvent aEvent,
            SourceDocument aDocument)
    {
        Project project = projectModel.getObject();
        Long docId = aDocument.getId();
        String documentName = aDocument.getName();

        ExternalLink link;
        if (CURATION_USER.equals(aEvent.getAnnotator())) {
            link = createDocumentPageLink(project, docId, aId, documentName, CurationPage.class);
        }
        else {
            link = createDocumentPageLink(project, docId, aId, documentName, AnnotationPage.class);
        }

        link.add(AttributeModifier.replace("title", documentName));

        return link;
    }

    private List<Pair<LoggedEvent, SourceDocument>> listActivities()
    {
        User user = userRepository.getCurrentUser();
        Project project = projectModel.getObject();

        Map<Long, SourceDocument> annotatableSourceDocuments = documentService
                .listAnnotatableDocuments(project, user).keySet().stream()
                .collect(toMap(SourceDocument::getId, identity()));

        Map<Long, SourceDocument> curatableSourceDocuments = curationService
                .listCuratableSourceDocuments(project).stream()
                .collect(toMap(SourceDocument::getId, identity()));

        boolean isCurator = projectService.isCurator(project, user);

        // get last annotation events
        // return filtered by user rights and document state
        List<LoggedEvent> recentEvents = eventRepository.listRecentActivity(project,
                user.getUsername(), annotationEvents, MAX_NUM_ACTIVITIES);
        return recentEvents.stream() //
                .filter(Objects::nonNull) //
                // Filter out documents which are not annotatable or curatable
                .filter(event -> {
                    if (CURATION_USER.equals(event.getAnnotator())) {
                        if (!isCurator) {
                            return false;
                        }

                        return curatableSourceDocuments.containsKey(event.getDocument());
                    }
                    else {
                        return annotatableSourceDocuments.containsKey(event.getDocument());
                    }
                })
                // Link events and documents before returning them
                .map(event -> {
                    if (CURATION_USER.equals(event.getAnnotator())) {
                        return Pair.of(event, curatableSourceDocuments.get(event.getDocument()));
                    }
                    else {
                        return Pair.of(event, annotatableSourceDocuments.get(event.getDocument()));
                    }
                })//
                .collect(toList());
    }
}
