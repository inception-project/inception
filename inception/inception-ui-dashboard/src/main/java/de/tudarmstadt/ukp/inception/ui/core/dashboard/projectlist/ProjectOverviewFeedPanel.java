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
package de.tudarmstadt.ukp.inception.ui.core.dashboard.projectlist;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;
import static java.time.Duration.ofMillis;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.AbstractDateTextFieldConfig.TodayButton;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.DateTextField;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.DateTextFieldConfig;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaClassAttributeModifier;
import de.tudarmstadt.ukp.inception.support.wicket.SymbolLabel;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage;

public class ProjectOverviewFeedPanel
    extends Panel
{
    private static final long serialVersionUID = -5938719501747228790L;

    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());

    private @SpringBean UserDao userService;
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean EventRepository eventRepository;

    private WebMarkupContainer projectListContainer;

    public ProjectOverviewFeedPanel(String aId)
    {
        super(aId);

        var now = Instant.now().truncatedTo(DAYS);
        var dateFrom = Model.of(Date.from(now.minus(7, DAYS)));
        var dateTo = Model.of(Date.from(now));
        var model = LoadableDetachableModel.of(() -> getProjectSummaries(dateFrom, dateTo));

        projectListContainer = new WebMarkupContainer("projectListContainer");
        projectListContainer.setOutputMarkupPlaceholderTag(true);
        projectListContainer.add(visibleWhenNot(model.map(List::isEmpty)));
        queue(projectListContainer);

        var config = new DateTextFieldConfig() //
                .autoClose(true) //
                .highlightToday(true) //
                .showTodayButton(TodayButton.LINKED);
        add(new DateTextField("dateFrom", dateFrom, config)
                .add(new LambdaAjaxFormComponentUpdatingBehavior("changeDate",
                        _target -> _target.add(projectListContainer)).withDebounce(ofMillis(200))));
        add(new DateTextField("dateTo", dateTo, config)
                .add(new LambdaAjaxFormComponentUpdatingBehavior("changeDate",
                        _target -> _target.add(projectListContainer)).withDebounce(ofMillis(200))));

        queue(createProjectList("project", model));
    }

    private ListView<ProjectSummary> createProjectList(String aId,
            IModel<List<ProjectSummary>> aModel)
    {
        return new ListView<>(aId, aModel)
        {
            @Override
            protected void populateItem(ListItem<ProjectSummary> aItem)
            {
                var project = aItem.getModel().map(ProjectSummary::getProject);
                var pageParameters = new PageParameters();
                ProjectPageBase.setProjectPageParameter(pageParameters, project.getObject());
                aItem.queue(new BookmarkablePageLink<Void>("link", ProjectDashboardPage.class,
                        pageParameters));
                var name = project.map(Project::getName);
                aItem.queue(new Label("name", name).add(AttributeModifier.replace("title", name)));
                aItem.queue(new SymbolLabel("state", project.map(Project::getState)));

                aItem.queue(new Label("toDate", aItem.getModel().map(ps -> {
                    var d = ps.getToDate();
                    return d == null ? "" : DATE_ONLY.format(d.toInstant());
                })));

                aItem.queue(new Label("fromDate", aItem.getModel().map(ps -> {
                    var d = ps.getFromDate();
                    return d == null ? "" : DATE_ONLY.format(d.toInstant());
                })));

                var sourceDocuments = aItem.getModel().map(ProjectSummary::getSourceDocuments);
                aItem.queue(new WebMarkupContainer("sourceDocumentContainer")
                        .add(visibleWhenNot(sourceDocuments.map(List::isEmpty))));
                aItem.queue(createSourceDocumentList("sourceDocument", sourceDocuments));

                var annotationSets = aItem.getModel().map(ProjectSummary::getAnnotationSets);
                aItem.queue(new WebMarkupContainer("annotationSetContainer")
                        .add(visibleWhenNot(annotationSets.map(List::isEmpty))));
                aItem.queue(createAnnotationSetList("annotationSet", annotationSets,
                        aItem.getModel().map(ProjectSummary::getIdxAnnDocs)));
            }
        };
    }

    private ListView<SourceDocument> createSourceDocumentList(String aId,
            IModel<List<SourceDocument>> aModel)
    {
        return new ListView<>(aId, aModel)
        {
            @Override
            protected void populateItem(ListItem<SourceDocument> aItem)
            {
                var name = aItem.getModel().map(SourceDocument::getName);
                aItem.add(new Label("name", name).add(AttributeModifier.replace("title", name)));
                aItem.add(new SymbolLabel("state", aItem.getModel().map(SourceDocument::getState)));
            }
        };
    }

    private ListView<AnnotationSet> createAnnotationSetList(String aId,
            IModel<List<AnnotationSet>> aAnnSets,
            IModel<Map<AnnotationSet, List<AnnotationDocument>>> aIdxAnnDocs)
    {
        return new ListView<>(aId, aAnnSets)
        {
            @Override
            protected void populateItem(ListItem<AnnotationSet> aItem)
            {
                var model = aItem.getModel();
                var annDocs = model.flatMap(as -> aIdxAnnDocs.map(m -> m.get(as)));

                var counts = new HashMap<AnnotationDocumentState, AtomicInteger>();
                annDocs.map(docs -> {
                    for (var d : docs) {
                        counts.computeIfAbsent(d.getState(), $ -> new AtomicInteger())
                                .incrementAndGet();
                    }
                    return null;
                }).getObject(); // getObject() needed to execute map operation

                var zero = new AtomicInteger(0);

                var name = model.map(AnnotationSet::displayName);
                aItem.queue(new Label("name", name).add(AttributeModifier.replace("title", name)));

                var docsAnnotationInProgress = counts.getOrDefault(IN_PROGRESS, zero).get();
                aItem.queue(new WebMarkupContainer("badgeAnnotationInProgress") //
                        .add(new LambdaClassAttributeModifier(classes -> {
                            if (docsAnnotationInProgress == 0) {
                                classes.add("text-opacity-25");
                            }
                            return classes;
                        })));
                aItem.queue(new SymbolLabel("iconAnnotationInProgress", IN_PROGRESS));
                aItem.queue(new Label("docsAnnotationInProgress", docsAnnotationInProgress));

                var docsAnnotationFinished = counts.getOrDefault(FINISHED, zero).get();
                aItem.queue(new WebMarkupContainer("badgeAnnotationFinished") //
                        .add(new LambdaClassAttributeModifier(classes -> {
                            if (docsAnnotationFinished == 0) {
                                classes.add("text-opacity-25");
                            }
                            return classes;
                        })));
                aItem.queue(new SymbolLabel("iconAnnotationFinished", FINISHED));
                aItem.queue(new Label("docsAnnotationFinished", docsAnnotationFinished));
            }
        };
    }

    private List<ProjectSummary> getProjectSummaries(IModel<Date> aDateFrom, IModel<Date> aDateTo)
    {
        if (!aDateFrom.isPresent().getObject() || !aDateTo.isPresent().getObject()) {
            return Collections.emptyList();
        }

        var sessionOwner = userService.getCurrentUsername();
        var roles = Set.of(MANAGER, CURATOR);

        // Clamp to local start/end of day
        var zone = ZoneId.systemDefault();
        var fromLocal = aDateFrom.getObject().toInstant().atZone(zone).toLocalDate();
        var toLocal = aDateTo.getObject().toInstant().atZone(zone).toLocalDate();
        var fromDate = Date.from(fromLocal.atStartOfDay(zone).toInstant());
        var toDate = Date.from(toLocal.plusDays(1).atStartOfDay(zone).toInstant().minus(1, MILLIS));

        var projectSummaries = new HashMap<Project, ProjectSummary.Builder>();
        var userCache = new HashMap<String, Optional<AnnotationSet>>();

        for (var p : projectService.listProjectsWithState(sessionOwner, roles,
                Set.of(ProjectState.ANNOTATION_IN_PROGRESS, ProjectState.ANNOTATION_FINISHED,
                        ProjectState.CURATION_IN_PROGRESS, ProjectState.CURATION_FINISHED),
                fromDate, toDate)) {
            projectSummaries.computeIfAbsent(p, ProjectSummary::builder);
        }

        for (var sd : documentService.listSourceDocumentsWithState(sessionOwner, roles,
                Set.of(ANNOTATION_IN_PROGRESS, ANNOTATION_FINISHED, CURATION_IN_PROGRESS,
                        CURATION_FINISHED),
                fromDate, toDate)) {
            projectSummaries.computeIfAbsent(sd.getProject(), ProjectSummary::builder)
                    .addSourceDocument(sd);
        }

        for (var ad : documentService.listAnnotationDocumentsWithState(sessionOwner, roles,
                Set.of(AnnotationDocumentState.IN_PROGRESS, AnnotationDocumentState.FINISHED),
                fromDate, toDate)) {

            var dataOwner = userCache.computeIfAbsent(ad.getAnnotationSet().id(), username -> {
                var u = userService.get(username);
                return u != null ? Optional.of(AnnotationSet.forUser(u)) : Optional.empty();
            });

            dataOwner.ifPresent(owner -> projectSummaries
                    .computeIfAbsent(ad.getDocument().getProject(), ProjectSummary::builder)
                    .addAnnotationDocument(owner, ad));
        }

        return projectSummaries.values().stream() //
                .map(ProjectSummary.Builder::build) //
                .sorted(comparing(ps -> ps.getToDate(), reverseOrder())) //
                .toList();
    }

    public static class ProjectSummary
        implements Serializable
    {
        private static final long serialVersionUID = 8014606638583476339L;

        private final Date fromDate;
        private final Date toDate;
        private final Project project;
        private final List<SourceDocument> sourceDocuments;
        private final List<AnnotationDocument> annotationDocuments;
        private final List<AnnotationSet> annotationSets;
        private final Map<AnnotationSet, List<AnnotationDocument>> idxAnnDocs;

        private ProjectSummary(Builder builder)
        {
            project = builder.project;
            sourceDocuments = builder.sourceDocuments;
            annotationDocuments = builder.annotationDocuments;
            annotationSets = builder.annotationSets.stream() //
                    .sorted(comparing(AnnotationSet::displayName)) //
                    .toList();
            idxAnnDocs = builder.annotationDocuments.stream()
                    .collect(groupingBy(AnnotationDocument::getAnnotationSet));

            var latestSource = sourceDocuments.stream() //
                    .map(SourceDocument::getStateUpdated) //
                    .filter(java.util.Objects::nonNull) //
                    .max(Date::compareTo) //
                    .orElse(null);

            var latestAnnotation = annotationDocuments.stream() //
                    .map(AnnotationDocument::getStateUpdated) //
                    .filter(java.util.Objects::nonNull) //
                    .max(Date::compareTo) //
                    .orElse(null);

            fromDate = Stream.of(project.getStateUpdated(), latestSource, latestAnnotation) //
                    .filter(java.util.Objects::nonNull) //
                    .min(Date::compareTo) //
                    .orElse(project.getStateUpdated());

            toDate = Stream.of(project.getStateUpdated(), latestSource, latestAnnotation) //
                    .filter(java.util.Objects::nonNull) //
                    .max(Date::compareTo) //
                    .orElse(project.getStateUpdated());
        }

        public Date getFromDate()
        {
            return fromDate;
        }

        public Date getToDate()
        {
            return toDate;
        }

        public Project getProject()
        {
            return project;
        }

        public List<SourceDocument> getSourceDocuments()
        {
            return sourceDocuments;
        }

        public List<AnnotationSet> getAnnotationSets()
        {
            return annotationSets;
        }

        public List<AnnotationDocument> getAnnotationDocuments()
        {
            return annotationDocuments;
        }

        public Map<AnnotationSet, List<AnnotationDocument>> getIdxAnnDocs()
        {
            return idxAnnDocs;
        }

        public static Builder builder(Project aProject)
        {
            return new Builder(aProject);
        }

        public static final class Builder
        {
            private final Project project;
            private final List<SourceDocument> sourceDocuments = new ArrayList<>();
            private final List<AnnotationDocument> annotationDocuments = new ArrayList<>();
            private final Set<AnnotationSet> annotationSets = new HashSet<>();

            private Builder(Project aProject)
            {
                project = aProject;
            }

            public Builder addSourceDocument(SourceDocument aSourceDocument)
            {
                sourceDocuments.add(aSourceDocument);
                return this;
            }

            public Builder addAnnotationDocument(AnnotationSet aDataOwner,
                    AnnotationDocument aAnnotationDocument)
            {
                annotationSets.add(aDataOwner);
                annotationDocuments.add(aAnnotationDocument);
                return this;
            }

            public ProjectSummary build()
            {
                return new ProjectSummary(this);
            }
        }
    }
}
