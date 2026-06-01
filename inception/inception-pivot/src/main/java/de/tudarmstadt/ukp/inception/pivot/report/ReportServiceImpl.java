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
package de.tudarmstadt.ukp.inception.pivot.report;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.pivot.api.aggregator.AggregatorSupportRegistry;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorBindingResolutionContext;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.ExtractorSupportRegistry;
import de.tudarmstadt.ukp.inception.pivot.api.report.AggregatorDef;
import de.tudarmstadt.ukp.inception.pivot.api.report.ExtractorDef;
import de.tudarmstadt.ukp.inception.pivot.api.report.FilterDef;
import de.tudarmstadt.ukp.inception.pivot.api.model.PivotReport;
import de.tudarmstadt.ukp.inception.pivot.api.report.ReportDef;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import jakarta.persistence.EntityManager;
import tools.jackson.databind.ObjectMapper;

public class ReportServiceImpl
    implements ReportService
{
    private final EntityManager entityManager;
    private final AnnotationSchemaService schemaService;
    private final ExtractorSupportRegistry extractorRegistry;
    private final AggregatorSupportRegistry aggregatorRegistry;
    private final ProjectService projectService;
    private final DocumentService documentService;
    private final UserDao userService;
    private final ObjectMapper jsonMapper;

    public ReportServiceImpl(EntityManager aEntityManager, AnnotationSchemaService aSchemaService,
            ExtractorSupportRegistry aExtractorRegistry,
            AggregatorSupportRegistry aAggregatorRegistry, ProjectService aProjectService,
            DocumentService aDocumentService, UserDao aUserService)
    {
        entityManager = aEntityManager;
        schemaService = aSchemaService;
        extractorRegistry = aExtractorRegistry;
        aggregatorRegistry = aAggregatorRegistry;
        projectService = aProjectService;
        documentService = aDocumentService;
        userService = aUserService;
        jsonMapper = JSONUtil.getObjectMapper();
    }

    @Override
    @Transactional
    public void createOrUpdateReport(PivotReport aReport)
    {
        if (aReport.getId() == null) {
            entityManager.persist(aReport);
        }
        else {
            entityManager.merge(aReport);
        }
    }

    @Override
    @Transactional
    public void deleteReport(PivotReport aReport)
    {
        var managed = entityManager.contains(aReport) ? aReport : entityManager.merge(aReport);
        entityManager.remove(managed);
    }

    @Override
    @Transactional
    public Optional<PivotReport> getReport(long aId)
    {
        return Optional.ofNullable(entityManager.find(PivotReport.class, aId));
    }

    @Override
    @Transactional
    public List<PivotReport> listReports(Project aProject)
    {
        return entityManager
                .createQuery("FROM PivotReport WHERE project = :project ORDER BY name ASC",
                        PivotReport.class) //
                .setParameter("project", aProject) //
                .getResultList();
    }

    @Override
    public ReportDef readDef(PivotReport aReport)
    {
        var json = aReport.getDefinition();
        if (json == null || json.isBlank()) {
            return null;
        }

        ReportDef def;
        try {
            def = jsonMapper.readValue(json, ReportDef.class);
        }
        catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Failed to deserialize definition of report [" + aReport.getName() + "]", e);
        }

        if (def.getSchemaVersion() > ReportDef.CURRENT_SCHEMA_VERSION) {
            throw new IllegalStateException("Definition of report [" + aReport.getName()
                    + "] uses schema version [" + def.getSchemaVersion()
                    + "] which is newer than the supported version ["
                    + ReportDef.CURRENT_SCHEMA_VERSION + "]");
        }

        return def;
    }

    @Override
    public void writeDef(PivotReport aReport, ReportDef aDef)
    {
        try {
            aReport.setDefinition(jsonMapper.writeValueAsString(aDef));
        }
        catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Failed to serialize definition of report [" + aReport.getName() + "]", e);
        }
    }

    @Override
    public ReportDef toDef(ReportDecl aDecl)
    {
        var def = new ReportDef();

        if (aDecl.getAggregator() != null) {
            def.setAggregator(new AggregatorDef(aDecl.getAggregator().id()));
        }

        def.setRowExtractors(toExtractorDefs(aDecl.getRowExtractors()));
        def.setColExtractors(toExtractorDefs(aDecl.getColExtractors()));
        def.setCellExtractors(toExtractorDefs(aDecl.getCellExtractors()));

        var filter = new FilterDef();
        filter.setAnnotators(aDecl.getAnnotators().stream() //
                .map(ProjectUserPermissions::getUsername) //
                .toList());
        filter.setDocuments(aDecl.getDocuments().stream() //
                .map(SourceDocument::getName) //
                .toList());
        filter.setStates(new ArrayList<>(aDecl.getStates()));
        def.setFilter(filter);

        return def;
    }

    @Override
    public ResolvedReport resolve(ReportDef aDef, Project aProject)
    {
        var decl = new ReportDecl();
        var problems = new ArrayList<LogMessage>();

        resolveAggregator(aDef, decl, problems);

        var layersByName = schemaService.listAnnotationLayer(aProject).stream() //
                .collect(toMap(AnnotationLayer::getName, identity()));

        // Snapshot all features once (grouped by layer name, then feature name) instead of issuing
        // one listAnnotationFeature query per feature-bound extractor reference.
        var featuresByLayer = schemaService.listAnnotationFeature(aProject).stream() //
                .collect(groupingBy(f -> f.getLayer().getName(),
                        toMap(AnnotationFeature::getName, identity())));

        decl.setRowExtractors(resolveExtractors(aDef.getRowExtractors(), layersByName,
                featuresByLayer, problems));
        decl.setColExtractors(resolveExtractors(aDef.getColExtractors(), layersByName,
                featuresByLayer, problems));
        decl.setCellExtractors(resolveExtractors(aDef.getCellExtractors(), layersByName,
                featuresByLayer, problems));

        var filter = aDef.getFilter();
        if (filter != null) {
            resolveAnnotators(filter, aProject, decl, problems);
            resolveDocuments(filter, aProject, decl, problems);
            decl.setStates(new ArrayList<>(filter.getStates()));
        }

        return new ResolvedReport(decl, problems);
    }

    private List<ExtractorDef> toExtractorDefs(List<ExtractorDecl> aDecls)
    {
        return aDecls.stream().map(this::toExtractorDef).toList();
    }

    private ExtractorDef toExtractorDef(ExtractorDecl aDecl)
    {
        return aDecl.binding().toDef(aDecl.id());
    }

    private void resolveAggregator(ReportDef aDef, ReportDecl aDecl, List<LogMessage> aProblems)
    {
        var aggregator = aDef.getAggregator();
        if (aggregator == null || aggregator.getId() == null) {
            return;
        }

        var maybeExt = aggregatorRegistry.getExtension(aggregator.getId());
        if (maybeExt.isEmpty()) {
            aProblems.add(LogMessage.warn(this, "Aggregator '%s' is no longer available.",
                    aggregator.getId()));
            return;
        }

        var ext = maybeExt.get();
        aDecl.setAggregator(new AggregatorDecl(ext.getId(), ext.getName(), ext.supportsCells()));
    }

    private List<ExtractorDecl> resolveExtractors(List<ExtractorDef> aRefs,
            Map<String, AnnotationLayer> aLayersByName,
            Map<String, Map<String, AnnotationFeature>> aFeaturesByLayer,
            List<LogMessage> aProblems)
    {
        var bindings = new ArrayList<ExtractorDecl>();
        var context = new BindingResolutionContext(aLayersByName, aFeaturesByLayer, aProblems);

        for (var ref : aRefs) {
            // The extractor is identified by its support id; look the support up directly rather
            // than inferring its kind from the presence of a layer/feature.
            var support = extractorRegistry.getExtension(ref.getId()).orElse(null);
            if (support == null) {
                aProblems.add(LogMessage.warn(this, "Extractor '%s' is no longer available.",
                        ref.getId()));
                continue;
            }

            // The support rebuilds its own binding; a null result means a referenced layer/feature
            // is gone and the context has already recorded the problem.
            var binding = support.bindingFromDef(ref, context);
            if (binding == null) {
                continue;
            }

            if (!support.accepts(binding)) {
                aProblems.add(LogMessage.warn(this,
                        "Extractor '%s' is no longer applicable to its binding.", ref.getId()));
                continue;
            }

            bindings.add(new ExtractorDecl(support.getId(), support.renderLabel(binding), binding));
        }

        return bindings;
    }

    private final class BindingResolutionContext
        implements ExtractorBindingResolutionContext
    {
        private final Map<String, AnnotationLayer> layersByName;
        private final Map<String, Map<String, AnnotationFeature>> featuresByLayer;
        private final List<LogMessage> problems;

        BindingResolutionContext(Map<String, AnnotationLayer> aLayersByName,
                Map<String, Map<String, AnnotationFeature>> aFeaturesByLayer,
                List<LogMessage> aProblems)
        {
            layersByName = aLayersByName;
            featuresByLayer = aFeaturesByLayer;
            problems = aProblems;
        }

        @Override
        public AnnotationLayer resolveLayer(String aLayerName)
        {
            var layer = layersByName.get(aLayerName);
            if (layer == null) {
                problems.add(LogMessage.warn(ReportServiceImpl.this,
                        "Layer '%s' is no longer available.", aLayerName));
            }
            return layer;
        }

        @Override
        public AnnotationFeature resolveFeature(String aLayerName, String aFeatureName)
        {
            var layer = resolveLayer(aLayerName);
            if (layer == null) {
                return null;
            }

            var feature = featuresByLayer.getOrDefault(aLayerName, Map.of()).get(aFeatureName);
            if (feature == null) {
                problems.add(LogMessage.warn(ReportServiceImpl.this,
                        "Feature '%s.%s' is no longer available.", aLayerName, aFeatureName));
            }
            return feature;
        }
    }

    private void resolveAnnotators(FilterDef aFilter, Project aProject, ReportDecl aDecl,
            List<LogMessage> aProblems)
    {
        if (aFilter.getAnnotators().isEmpty()) {
            return;
        }

        var byUsername = listDataOwners(aProject).stream() //
                .collect(toMap(ProjectUserPermissions::getUsername, identity(), (a, $) -> a));

        for (var username : aFilter.getAnnotators()) {
            var perm = byUsername.get(username);
            if (perm != null) {
                aDecl.getAnnotators().add(perm);
            }
            else {
                aProblems.add(
                        LogMessage.warn(this, "Annotator '%s' is no longer available.", username));
            }
        }
    }

    private void resolveDocuments(FilterDef aFilter, Project aProject, ReportDecl aDecl,
            List<LogMessage> aProblems)
    {
        if (aFilter.getDocuments().isEmpty()) {
            return;
        }

        var byName = documentService.listSourceDocuments(aProject).stream() //
                .collect(toMap(SourceDocument::getName, identity(), (a, $) -> a));

        for (var name : aFilter.getDocuments()) {
            var doc = byName.get(name);
            if (doc != null) {
                aDecl.getDocuments().add(doc);
            }
            else {
                aProblems.add(LogMessage.warn(this, "Document '%s' is no longer available.", name));
            }
        }
    }

    @Override
    public List<ProjectUserPermissions> listDataOwners(Project aProject)
    {
        var dataOwners = new ArrayList<ProjectUserPermissions>();

        projectService.listProjectUserPermissions(aProject).stream() //
                .filter(p -> p.getRoles().contains(ANNOTATOR)) //
                .sorted(comparing(p -> p.getUser().map(User::getUiName).orElse(p.getUsername()))) //
                .forEach(dataOwners::add);

        var curationUser = userService.getCurationUser();
        dataOwners.add(new ProjectUserPermissions(aProject, curationUser.getUsername(),
                curationUser, emptySet()));

        var initialCasUser = userService.getInitialCasUser();
        dataOwners.add(new ProjectUserPermissions(aProject, initialCasUser.getUsername(),
                initialCasUser, emptySet()));

        return dataOwners;
    }
}
