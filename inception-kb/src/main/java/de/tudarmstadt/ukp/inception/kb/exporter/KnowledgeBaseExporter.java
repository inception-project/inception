/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static java.util.Arrays.asList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.export.exporters.LayerExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.SchemaProfile;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;

@Component
public class KnowledgeBaseExporter implements ProjectExporter
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
    private final AnnotationSchemaService schemaService;

    @Autowired
    public KnowledgeBaseExporter(KnowledgeBaseService aKbService,
        AnnotationSchemaService aSchemaService)
    {
        kbService = aKbService;
        schemaService = aSchemaService;
    }

    @Override
    public List<Class<? extends ProjectExporter>> getImportDependencies() {
        return asList(LayerExporter.class);
    }

    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aFile)
        throws Exception
    {
        Project project = aRequest.getProject();
        List<ExportedKnowledgeBase> exportedKnowledgeBases = new ArrayList<>();
        for (KnowledgeBase kb: kbService.getKnowledgeBases(project)) {
            ExportedKnowledgeBase exportedKB = new ExportedKnowledgeBase();
            exportedKB.setId(kb.getRepositoryId());
            exportedKB.setName(kb.getName());
            exportedKB.setType(kb.getType().toString());
            exportedKB.setClassIri(kb.getClassIri().stringValue());
            exportedKB.setSubclassIri(kb.getSubclassIri().stringValue());
            exportedKB.setTypeIri(kb.getTypeIri().stringValue());
            exportedKB.setDescriptionIri(kb.getDescriptionIri().stringValue());
            exportedKB.setLabelIri(kb.getLabelIri().stringValue());
            exportedKB.setPropertyTypeIri(kb.getPropertyTypeIri().stringValue());
            exportedKB.setPropertyLabelIri(kb.getPropertyLabelIri().stringValue());
            exportedKB.setPropertyDescriptionIri(kb.getPropertyDescriptionIri().stringValue());
            exportedKB.setFullTextSearchIri(
                    kb.getFullTextSearchIri() != null ? kb.getFullTextSearchIri().stringValue()
                            : null);
            exportedKB.setReadOnly(kb.isReadOnly());
            exportedKB.setEnabled(kb.isEnabled());
            exportedKB.setReification(kb.getReification().toString());
            exportedKB.setSupportConceptLinking(kb.isSupportConceptLinking());
            exportedKB.setBasePrefix(kb.getBasePrefix());
            exportedKB.setRootConcepts(
                kb.getExplicitlyDefinedRootConcepts()
                    .stream()
                    .map(conceptIRI -> conceptIRI.stringValue())
                    .collect(Collectors.toList()));
            exportedKB.setDefaultLanguage(kb.getDefaultLanguage());
            exportedKB.setMaxResults(kb.getMaxResults());
            exportedKnowledgeBases.add(exportedKB);

            if (kb.getType() == RepositoryType.REMOTE) {
                // set url for remote KB
                RepositoryImplConfig cfg = kbService.getKnowledgeBaseConfig(kb);
                String url = ((SPARQLRepositoryConfig) cfg).getQueryEndpointUrl();
                exportedKB.setRemoteURL(url);
            }
            else {
                // export local kb files
                exportKnowledgeBaseFiles(aFile, kb);
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
    private void exportKnowledgeBaseFiles(File aFile, KnowledgeBase kb)
        throws IOException
    {
        File sourceKnowledgeBaseDir = new File(aFile + KB_FOLDER);
        FileUtils.forceMkdir(sourceKnowledgeBaseDir);

        // create file with name "<knowledgebaseName>.<fileExtension>" in folder
        // KB_FOLDER
        File kbData = new File(aFile + getSourceFileName(kb));
        kbData.createNewFile();
        try (OutputStream os = new FileOutputStream(kbData)) {
            kbService.exportData(kb, knowledgeBaseFileExportFormat, os);
        }
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
        ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        ExportedKnowledgeBase[] knowledgeBases = aExProject
            .getArrayProperty(KEY, ExportedKnowledgeBase.class);
        ValueFactory vf = SimpleValueFactory.getInstance();
        for (ExportedKnowledgeBase exportedKB : knowledgeBases) {
            KnowledgeBase kb = new KnowledgeBase();
            kb.setName(exportedKB.getName());
            kb.setType(RepositoryType.valueOf(exportedKB.getType()));
            // set default value for IRIs if no value is present in
            // order to support import of older projects
            kb.setClassIri(exportedKB.getClassIri() != null ?
                vf.createIRI(exportedKB.getClassIri()) :
                DEFAULTPROFILE.getClassIri());
            kb.setSubclassIri(exportedKB.getSubclassIri() != null ?
                vf.createIRI(exportedKB.getSubclassIri()) :
                DEFAULTPROFILE.getSubclassIri());
            kb.setTypeIri(exportedKB.getTypeIri() != null ?
                vf.createIRI(exportedKB.getTypeIri()) :
                DEFAULTPROFILE.getTypeIri());
            kb.setDescriptionIri(exportedKB.getDescriptionIri() != null ?
                vf.createIRI(exportedKB.getDescriptionIri()) :
                DEFAULTPROFILE.getDescriptionIri());
            kb.setLabelIri(exportedKB.getLabelIri() != null ?
                vf.createIRI(exportedKB.getLabelIri()) :
                DEFAULTPROFILE.getLabelIri());
            kb.setPropertyTypeIri(exportedKB.getPropertyTypeIri() != null ?
                vf.createIRI(exportedKB.getPropertyTypeIri()) :
                DEFAULTPROFILE.getPropertyTypeIri());
            kb.setPropertyLabelIri(exportedKB.getPropertyLabelIri() != null ?
                vf.createIRI(exportedKB.getPropertyLabelIri()) :
                DEFAULTPROFILE.getPropertyLabelIri());
            kb.setPropertyDescriptionIri(exportedKB.getPropertyDescriptionIri() != null ?
                vf.createIRI(exportedKB.getPropertyDescriptionIri()) :
                DEFAULTPROFILE.getPropertyDescriptionIri());
            // The imported project may date from a time where we did not yet have the FTS IRI.
            // In that case we use concept linking support as an indicator that we dealt with a
            // remote Virtuoso.
            if (exportedKB.isSupportConceptLinking() && exportedKB.getFullTextSearchIri() == null) {
                kb.setFullTextSearchIri(IriConstants.FTS_VIRTUOSO);
            }
            kb.setFullTextSearchIri(exportedKB.getFullTextSearchIri() != null
                ? vf.createIRI(exportedKB.getFullTextSearchIri()) : null);

            kb.setReadOnly(exportedKB.isReadOnly());
            kb.setEnabled(exportedKB.isEnabled());
            kb.setReification(Reification.valueOf(exportedKB.getReification()));
            kb.setBasePrefix(exportedKB.getBasePrefix());

            if (exportedKB.getRootConcepts() != null) {
                kb.setExplicitlyDefinedRootConcepts(
                    exportedKB.getRootConcepts().stream()
                        .map(conceptId -> vf.createIRI(conceptId)).collect(Collectors.toList()));
            }
            else {
                kb.setExplicitlyDefinedRootConcepts(new ArrayList<>());
            }
            kb.setDefaultLanguage(exportedKB.getDefaultLanguage());
            kb.setMaxResults(exportedKB.getMaxResults());
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

            // Early versions of INCEpTION did not set the ID.
            if (exportedKB.getId() == null) {
                LOG.warn(
                    "Cannot update concept feature traits: KB identifier is not set in the exported project.");
                continue;
            }

            for (AnnotationFeature feature : schemaService.listAnnotationFeature(aProject)) {
                if (feature.getType().startsWith("kb:")) {
                    try {
                        ConceptFeatureTraits traits = JSONUtil
                            .fromJsonString(ConceptFeatureTraits.class, feature.getTraits());

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
        String sourceFileName = getSourceFileName(kb);
        // remove leading "/"
        ZipEntry entry = aZip.getEntry(sourceFileName.substring(1));

        try (InputStream is = aZip.getInputStream(entry)) {
            kbService.importData(kb, sourceFileName, is);
        }
    }

    private String getSourceFileName(KnowledgeBase kb)
    {
        return KNOWLEDGEBASEFILES + kb.getName() + "."
            + knowledgeBaseFileExportFormat.getDefaultFileExtension();
    }
}
