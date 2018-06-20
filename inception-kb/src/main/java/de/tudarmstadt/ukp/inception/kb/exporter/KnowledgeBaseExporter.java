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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
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

import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;

@Component
public class KnowledgeBaseExporter implements ProjectExporter
{
    private static final String KEY = "knowledgeBases";
    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeBaseExporter.class);
    
    private static final String SOURCE = "knowledgeBase_source";
    private static final String SOURCE_FOLDER = "/" + SOURCE;
    private static final String KNOWLEDGEBASEFILES = SOURCE_FOLDER + "/";
    
    private static final RDFFormat knowledgeBaseFileExportFormat = RDFFormat.TURTLE;
    
    private final KnowledgeBaseService kbService;
    
    @Autowired
    public KnowledgeBaseExporter(KnowledgeBaseService knowledgeBaseService)
    {
        kbService = knowledgeBaseService;
    }
    
    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aFile)
        throws Exception
    {
        Project project = aRequest.getProject();
        List<ExportedKnowledgeBase> exportedKnowledgeBases = new ArrayList<>();
        for (KnowledgeBase kb: kbService.getKnowledgeBases(project)) {
            ExportedKnowledgeBase exportedKB = new ExportedKnowledgeBase();
            exportedKB.setName(kb.getName());
            exportedKB.setType(kb.getType().toString());
            exportedKB.setClassIri(kb.getClassIri().stringValue());
            exportedKB.setSubclassIri(kb.getSubclassIri().stringValue());
            exportedKB.setTypeIri(kb.getTypeIri().stringValue());
            exportedKB.setDescriptionIri(kb.getDescriptionIri().stringValue());
            exportedKB.setLabelIri(kb.getLabelIri().stringValue());
            exportedKB.setPropertyTypeIri(kb.getPropertyTypeIri().stringValue());
            exportedKB.setReadOnly(kb.isReadOnly());
            exportedKB.setEnabled(kb.isEnabled());
            exportedKB.setReification(kb.getReification().toString());
            exportedKB.setSupportConceptLinking(kb.isSupportConceptLinking());
            exportedKB.setBasePrefix(kb.getBasePrefix());
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
        throws FileNotFoundException, IOException
    {
        File sourceKnowledgeBaseDir = new File(aFile + SOURCE_FOLDER);
        FileUtils.forceMkdir(sourceKnowledgeBaseDir);

        // create file with name "<knowledgebaseName>.<fileExtension>" in folder
        // knowledgeBase_source
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
            kb.setClassIri(vf.createIRI(exportedKB.getClassIri()));
            kb.setSubclassIri(vf.createIRI(exportedKB.getSubclassIri()));
            kb.setTypeIri(vf.createIRI(exportedKB.getTypeIri()));
            kb.setDescriptionIri(vf.createIRI(exportedKB.getDescriptionIri()));
            kb.setLabelIri(vf.createIRI(exportedKB.getLabelIri()));
            kb.setPropertyTypeIri(vf.createIRI(exportedKB.getPropertyTypeIri()));
            kb.setReadOnly(exportedKB.isReadOnly());
            kb.setEnabled(exportedKB.isEnabled());
            kb.setReification(Reification.valueOf(exportedKB.getReification()));
            kb.setSupportConceptLinking(exportedKB.isSupportConceptLinking());
            kb.setBasePrefix(exportedKB.getBasePrefix());
            kb.setProject(aProject);
            
            // Get config and register knowledge base
            RepositoryImplConfig cfg;
            if (kb.getType() == RepositoryType.LOCAL) {
                cfg = kbService.getNativeConfig();
                kbService.registerKnowledgeBase(kb, cfg);
                importKnowledgeBaseFiles(aZip, kb);
            }
            else {
                cfg = kbService.getRemoteConfig(exportedKB.getRemoteURL());
                kbService.registerKnowledgeBase(kb, cfg);
            }
        }
        int n = knowledgeBases.length;
        LOG.info("Imported [{}] knowledge bases for project [{}]", n, aProject.getName());
    }
    
    /**
     * import the source files of local a knowledge base form the zip file of an previously exported
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
