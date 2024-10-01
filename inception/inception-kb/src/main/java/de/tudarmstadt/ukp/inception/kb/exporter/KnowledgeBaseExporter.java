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
package de.tudarmstadt.ukp.inception.kb.exporter;

import static de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter.getEntry;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits_ImplBase;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.MultiValueConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.SchemaProfile;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBasePropertiesImpl;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.exporters.LayerExporter;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link KnowledgeBaseServiceAutoConfiguration#knowledgeBaseExporter}.
 * </p>
 */
public class KnowledgeBaseExporter
    implements ProjectExporter
{
    private static final String KEY = "knowledge_bases";
    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeBaseExporter.class);

    private static final String KB = "kb";
    private static final String KB_FOLDER = "/" + KB;
    private static final String KNOWLEDGEBASEFILES = KB_FOLDER + "/";

    // Use default profile IRIs for missing IRI values in order to import older projects
    private static final SchemaProfile DEFAULTPROFILE = SchemaProfile.OWLSCHEMA;

    private static final RDFFormat knowledgeBaseFileExportFormat = RDFFormat.TURTLE;

    private final KnowledgeBaseService kbService;
    private final KnowledgeBaseProperties kbProperties;
    private final AnnotationSchemaService schemaService;

    @Autowired
    public KnowledgeBaseExporter(KnowledgeBaseService aKbService,
            KnowledgeBaseProperties aKbProperties, AnnotationSchemaService aSchemaService)
    {
        kbService = aKbService;
        kbProperties = aKbProperties;
        schemaService = aSchemaService;
    }

    @Override
    public List<Class<? extends ProjectExporter>> getImportDependencies()
    {
        return asList(LayerExporter.class);
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aStage)
        throws InterruptedException, IOException
    {
        var project = aRequest.getProject();
        List<ExportedKnowledgeBase> exportedKnowledgeBases = new ArrayList<>();
        for (var kb : kbService.getKnowledgeBases(project)) {
            // check if the export has been cancelled
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            var exportedKB = new ExportedKnowledgeBase();
            exportedKB.setId(kb.getRepositoryId());
            exportedKB.setName(kb.getName());
            exportedKB.setType(kb.getType().toString());
            exportedKB.setClassIri(kb.getClassIri());
            exportedKB.setSubclassIri(kb.getSubclassIri());
            exportedKB.setTypeIri(kb.getTypeIri());
            exportedKB.setDescriptionIri(kb.getDescriptionIri());
            exportedKB.setLabelIri(kb.getLabelIri());
            exportedKB.setPropertyTypeIri(kb.getPropertyTypeIri());
            exportedKB.setPropertyLabelIri(kb.getPropertyLabelIri());
            exportedKB.setPropertyDescriptionIri(kb.getPropertyDescriptionIri());
            exportedKB.setDeprecationPropertyIri(kb.getDeprecationPropertyIri());
            exportedKB.setFullTextSearchIri(kb.getFullTextSearchIri());
            exportedKB.setReadOnly(kb.isReadOnly());
            exportedKB.setUseFuzzy(kb.isUseFuzzy());
            exportedKB.setEnabled(kb.isEnabled());
            exportedKB.setReification(kb.getReification().toString());
            exportedKB.setSupportConceptLinking(kb.isSupportConceptLinking());
            exportedKB.setBasePrefix(kb.getBasePrefix());
            exportedKB.setRootConcepts(new ArrayList<>(kb.getRootConcepts()));
            exportedKB.setAdditionalMatchingProperties(
                    new ArrayList<>(kb.getAdditionalMatchingProperties()));
            exportedKB.setAdditionalLanguages(new ArrayList<>(kb.getAdditionalLanguages()));
            exportedKB.setDefaultLanguage(kb.getDefaultLanguage());
            exportedKB.setDefaultDatasetIri(
                    kb.getDefaultDatasetIri() != null ? kb.getDefaultDatasetIri() : null);
            exportedKB.setMaxResults(kb.getMaxResults());
            exportedKB.setSubPropertyIri(kb.getSubPropertyIri());
            exportedKB.setTraits(kb.getTraits());
            exportedKnowledgeBases.add(exportedKB);

            if (kb.getType() == RepositoryType.REMOTE) {
                // set url for remote KB
                var cfg = kbService.getKnowledgeBaseConfig(kb);
                var url = ((SPARQLRepositoryConfig) cfg).getQueryEndpointUrl();
                exportedKB.setRemoteURL(url);
            }
            else {
                // export local kb files
                exportKnowledgeBaseFiles(aStage, kb);
            }

        }
        aExProject.setProperty(KEY, exportedKnowledgeBases);
        int n = exportedKnowledgeBases.size();
        LOG.info("Exported [{}] knowledge bases for project [{}]", n, project.getName());
    }

    /**
     * exports the source files of local a knowledge base in the format specified in
     * {@link #knowledgeBaseFileExportFormat}
     */
    private void exportKnowledgeBaseFiles(ZipOutputStream aStage, KnowledgeBase kb)
        throws IOException
    {
        ProjectExporter.writeEntry(aStage, getSourceFileName(kb), os -> {
            kbService.exportData(kb, knowledgeBaseFileExportFormat, os);
        });
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        var knowledgeBases = aExProject.getArrayProperty(KEY, ExportedKnowledgeBase.class);

        for (var exportedKB : knowledgeBases) {
            var kb = new KnowledgeBase();
            kb.setName(exportedKB.getName());
            kb.setType(RepositoryType.valueOf(exportedKB.getType()));
            // set default value for IRIs if no value is present in
            // order to support import of older projects
            kb.setClassIri(exportedKB.getClassIri() != null //
                    ? exportedKB.getClassIri() //
                    : DEFAULTPROFILE.getClassIri());
            kb.setSubclassIri(exportedKB.getSubclassIri() != null //
                    ? exportedKB.getSubclassIri() //
                    : DEFAULTPROFILE.getSubclassIri());
            kb.setTypeIri(exportedKB.getTypeIri() != null //
                    ? exportedKB.getTypeIri() //
                    : DEFAULTPROFILE.getTypeIri());
            kb.setDescriptionIri(exportedKB.getDescriptionIri() != null //
                    ? exportedKB.getDescriptionIri() //
                    : DEFAULTPROFILE.getDescriptionIri());
            kb.setLabelIri(exportedKB.getLabelIri() != null //
                    ? exportedKB.getLabelIri() //
                    : DEFAULTPROFILE.getLabelIri());
            kb.setPropertyTypeIri(exportedKB.getPropertyTypeIri() != null //
                    ? exportedKB.getPropertyTypeIri() //
                    : DEFAULTPROFILE.getPropertyTypeIri());
            kb.setPropertyLabelIri(exportedKB.getPropertyLabelIri() != null //
                    ? exportedKB.getPropertyLabelIri() //
                    : DEFAULTPROFILE.getPropertyLabelIri());
            kb.setPropertyDescriptionIri(exportedKB.getPropertyDescriptionIri() != null //
                    ? exportedKB.getPropertyDescriptionIri() //
                    : DEFAULTPROFILE.getPropertyDescriptionIri());
            kb.setSubPropertyIri(exportedKB.getSubPropertyIri() != null //
                    ? exportedKB.getSubPropertyIri() //
                    : DEFAULTPROFILE.getSubPropertyIri());
            kb.setDeprecationPropertyIri(exportedKB.getDeprecationPropertyIri() != null //
                    ? exportedKB.getDeprecationPropertyIri() //
                    : DEFAULTPROFILE.getDeprecationPropertyIri());

            // The imported project may date from a time where we did not yet have the FTS IRI.
            // In that case we use concept linking support as an indicator that we dealt with a
            // remote Virtuoso.
            if (exportedKB.isSupportConceptLinking() && exportedKB.getFullTextSearchIri() == null) {
                kb.setFullTextSearchIri(IriConstants.FTS_VIRTUOSO.stringValue());
            }
            kb.setFullTextSearchIri(exportedKB.getFullTextSearchIri() != null ? //
                    exportedKB.getFullTextSearchIri() : null);

            kb.setEnabled(exportedKB.isEnabled());
            kb.setUseFuzzy(exportedKB.isUseFuzzy());
            kb.setReification(Reification.valueOf(exportedKB.getReification()));
            kb.setBasePrefix(exportedKB.getBasePrefix());
            kb.setTraits(exportedKB.getTraits());

            if (exportedKB.getRootConcepts() != null) {
                kb.setRootConcepts(exportedKB.getRootConcepts());
            }
            else {
                kb.setRootConcepts(new ArrayList<>());
            }

            if (exportedKB.getAdditionalMatchingProperties() != null) {
                kb.setAdditionalMatchingProperties(exportedKB.getAdditionalMatchingProperties());
            }
            else {
                kb.setAdditionalMatchingProperties(new ArrayList<>());
            }

            if (exportedKB.getAdditionalLanguages() != null) {
                kb.setAdditionalLanguages(exportedKB.getAdditionalLanguages());
            }
            else {
                kb.setAdditionalLanguages(new ArrayList<>());
            }

            kb.setDefaultLanguage(exportedKB.getDefaultLanguage());
            kb.setDefaultDatasetIri(
                    exportedKB.getDefaultDatasetIri() != null ? exportedKB.getDefaultDatasetIri() //
                            : null);
            kb.setMaxResults(exportedKB.getMaxResults());
            // If not setting, initialize with default
            if (kb.getMaxResults() == 0) {
                kb.setMaxResults(kbProperties.getDefaultMaxResults());
            }
            // Cap at local min results
            if (kb.getMaxResults() < KnowledgeBasePropertiesImpl.HARD_MIN_RESULTS) {
                kb.setMaxResults(KnowledgeBasePropertiesImpl.HARD_MIN_RESULTS);
            }
            // Cap at local max results
            if (kb.getMaxResults() > kbProperties.getHardMaxResults()) {
                kb.setMaxResults(kbProperties.getHardMaxResults());
            }
            kb.setProject(aProject);

            // Get config and register knowledge base
            if (kb.getType() == RepositoryType.LOCAL) {
                RepositoryImplConfig cfg = kbService.getNativeConfig();
                kbService.registerKnowledgeBase(kb, cfg);
                kbService.defineBaseProperties(kb);
                importKnowledgeBaseFiles(aZip, kb);
            }
            else {
                RepositoryImplConfig cfg = kbService.getRemoteConfig(exportedKB.getRemoteURL());
                kbService.registerKnowledgeBase(kb, cfg);
            }

            // Set read-only flag only at the end to ensure that we do not run into a "read only"
            // error during import
            kb.setReadOnly(exportedKB.isReadOnly());

            // Early versions of INCEpTION did not set the ID.
            if (exportedKB.getId() == null) {
                LOG.warn(
                        "Cannot update concept feature traits: KB identifier is not set in the exported project.");
                continue;
            }

            for (AnnotationFeature feature : schemaService.listAnnotationFeature(aProject)) {
                if (feature.getType().startsWith("kb:")
                        || feature.getType().startsWith("kb-multi:")) {
                    try {
                        ConceptFeatureTraits_ImplBase traits;

                        if (feature.getType().startsWith("kb:")) {
                            traits = JSONUtil.fromJsonString(ConceptFeatureTraits.class,
                                    feature.getTraits());
                        }
                        else if (feature.getType().startsWith("kb-multi:")) {
                            traits = JSONUtil.fromJsonString(MultiValueConceptFeatureTraits.class,
                                    feature.getTraits());
                        }
                        else {
                            throw new IllegalStateException(
                                    "Prefix must be [kb:] or [kb-multi:] in [" + feature.getType()
                                            + "]");
                        }

                        if (traits != null && exportedKB.getId().equals(traits.getRepositoryId())) {
                            traits.setRepositoryId(kb.getRepositoryId());
                            feature.setTraits(JSONUtil.toJsonString(traits));
                            schemaService.createFeature(feature);
                        }
                    }
                    catch (IOException e) {
                        LOG.error("Unable to update traits", e);
                    }
                }
            }
        }

        int n = knowledgeBases.length;
        LOG.info("Imported [{}] knowledge bases for project [{}]", n, aProject.getName());
    }

    /**
     * import the source files of local a knowledge base form the zip file of a previously exported
     * project {@link #knowledgeBaseFileExportFormat}
     */
    private void importKnowledgeBaseFiles(ZipFile aZip, KnowledgeBase kb) throws IOException
    {
        var entry = getEntry(aZip, getSourceFileName(kb));
        try (var is = aZip.getInputStream(entry)) {
            kbService.importData(kb, entry.getName(), is);
        }
    }

    private String getSourceFileName(KnowledgeBase kb)
    {
        return KNOWLEDGEBASEFILES + kb.getName() + "."
                + knowledgeBaseFileExportFormat.getDefaultFileExtension();
    }
}
