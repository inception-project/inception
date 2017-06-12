/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.DOCUMENT;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationCasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

@Component(ImportExportService.SERVICE_NAME)
public class ImportExportServiceImpl
    implements ImportExportService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Value(value = "${repository.path}")
    private File dir;
    
    @Resource(name = "casStorageService")
    private CasStorageService casStorageService;
    
    @Resource(name = "automationCasStorageService")
    private AutomationCasStorageService automationCasStorageService;
    
    
    @Resource(name = "annotationService")
    private AnnotationSchemaService annotationService;

    @Resource(name = "documentService")
    private DocumentService documentService;
    
    @Resource(name = "automationService")
    private AutomationService automationService;

    @Resource(name = "formats")
    private Properties readWriteFileFormats;

    public ImportExportServiceImpl()
    {
        // Nothing to do
    }

    /**
     * A new directory is created using UUID so that every exported file will reside in its own
     * directory. This is useful as the written file can have multiple extensions based on the
     * Writer class used.
     */
    @SuppressWarnings("rawtypes")
    @Override
    @Transactional
    public File exportAnnotationDocument(SourceDocument aDocument, String aUser, Class aWriter,
            String aFileName, Mode aMode)
        throws UIMAException, IOException, ClassNotFoundException
    {
        return exportAnnotationDocument(aDocument, aUser, aWriter, aFileName, aMode, true);
    }

    @SuppressWarnings("rawtypes")
    @Override
    @Transactional
    public File exportAnnotationDocument(SourceDocument aDocument, String aUser, Class aWriter,
            String aFileName, Mode aMode, boolean aStripExtension)
        throws UIMAException, IOException, ClassNotFoundException
    {
        File annotationFolder = casStorageService.getAnnotationFolder(aDocument);
        String serializedCasFileName;
        // for Correction, it will export the corrected document (of the logged in user)
        // (CORRECTION_USER.ser is the automated result displayed for the user to correct it, not
        // the final result) for automation, it will export either the corrected document
        // (Annotated) or the automated document
        if (aMode.equals(Mode.ANNOTATION) || aMode.equals(Mode.AUTOMATION)
                || aMode.equals(Mode.CORRECTION)) {
            serializedCasFileName = aUser + ".ser";
        }
        // The merge result will be exported
        else {
            serializedCasFileName = WebAnnoConst.CURATION_USER + ".ser";
        }

        // Read file
        File serializedCasFile = new File(annotationFolder, serializedCasFileName);
        if (!serializedCasFile.exists()) {
            throw new FileNotFoundException("CAS file [" + serializedCasFileName
                    + "] not found in [" + annotationFolder + "]");
        }

        CAS cas = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
        CasPersistenceUtils.readSerializedCas(cas.getJCas(), serializedCasFile);

        // Update type system the CAS
        annotationService.upgradeCas(cas, aDocument, aUser);
        
        File exportFile = exportCasToFile(cas, aDocument, aFileName, aWriter, aStripExtension);

        Project project = aDocument.getProject();
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(project.getId()))) {
            log.info("Exported annotation document [{}]({}) for user [{}] from project [{}]({})",
                    aDocument.getName(), aDocument.getId(), aUser, project.getName(),
                    project.getId());
        }

        return exportFile;
    }
    
    @Override
    @Transactional
    public void uploadTrainingDocument(File aFile, TrainingDocument aDocument)
        throws IOException
    {
        // Check if the file has a valid format / can be converted without error
        JCas cas = null;
        try {
            if (aDocument.getFormat().equals(WebAnnoConst.TAB_SEP)) {
                if (!isTabSepFileFormatCorrect(aFile)) {
                    throw new IOException(
                            "This TAB-SEP file is not in correct format. It should have two columns separated by TAB!");
                }
            }
            else {
                cas = importCasFromFile(aFile, aDocument.getProject(), aDocument.getFormat());
                automationCasStorageService.analyzeAndRepair(aDocument, cas.getCas());
            }
        }
        catch (IOException e) {
            automationService.removeTrainingDocument(aDocument);
            throw e;
        }
        catch (Exception e) {
        	automationService.removeTrainingDocument(aDocument);
            throw new IOException(e.getMessage(), e);
        }

        // Copy the original file into the repository
        File targetFile = automationService.getTrainingDocumentFile(aDocument);
        FileUtils.forceMkdir(targetFile.getParentFile());
        FileUtils.copyFile(aFile, targetFile);

        // Copy the initial conversion of the file into the repository
        if (cas != null) {
            CasPersistenceUtils.writeSerializedCas(cas,
            		automationService.getCasFile(aDocument));
        }

        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(aDocument.getProject().getId()))) {
            Project project = aDocument.getProject();
            log.info("Imported training document [{}]({}) to project [{}]({})", 
                    aDocument.getName(), aDocument.getId(), project.getName(), project.getId());
        }
    }
    
    @Override
    public List<String> getReadableFormatLabels()
    {
        List<String> readableFormats = new ArrayList<>();
        for (String key : readWriteFileFormats.stringPropertyNames()) {
            if (key.contains(".label") && !isBlank(readWriteFileFormats.getProperty(key))) {
                String readerLabel = key.substring(0, key.lastIndexOf(".label"));
                if (!isBlank(readWriteFileFormats.getProperty(readerLabel + ".reader"))) {
                    readableFormats.add(readWriteFileFormats.getProperty(key));
                }
            }
        }
        Collections.sort(readableFormats);
        return readableFormats;
    }

    @Override
    public String getReadableFormatId(String aLabel)
    {
        String readableFormat = "";
        for (String key : readWriteFileFormats.stringPropertyNames()) {
            if (key.contains(".label") && !isBlank(readWriteFileFormats.getProperty(key))) {
                if (readWriteFileFormats.getProperty(key).equals(aLabel)) {
                    readableFormat = key.substring(0, key.lastIndexOf(".label"));
                    break;
                }
            }
        }
        return readableFormat;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Map<String, Class<CollectionReader>> getReadableFormats()
        throws ClassNotFoundException
    {
        Map<String, Class<CollectionReader>> readableFormats = new HashMap<>();
        for (String key : readWriteFileFormats.stringPropertyNames()) {
            if (key.contains(".label") && !isBlank(readWriteFileFormats.getProperty(key))) {
                String readerLabel = key.substring(0, key.lastIndexOf(".label"));
                if (!isBlank(readWriteFileFormats.getProperty(readerLabel + ".reader"))) {
                    readableFormats.put(readerLabel, (Class) Class.forName(readWriteFileFormats
                            .getProperty(readerLabel + ".reader")));
                }
            }
        }
        return readableFormats;
    }

    @Override
    public List<String> getWritableFormatLabels()
    {
        List<String> writableFormats = new ArrayList<>();
        for (String key : readWriteFileFormats.stringPropertyNames()) {
            if (key.contains(".label") && !isBlank(readWriteFileFormats.getProperty(key))) {
                String writerLabel = key.substring(0, key.lastIndexOf(".label"));
                if (!isBlank(readWriteFileFormats.getProperty(writerLabel + ".writer"))) {
                    writableFormats.add(readWriteFileFormats.getProperty(key));
                }
            }
        }
        Collections.sort(writableFormats);
        return writableFormats;
    }

    @Override
    public String getWritableFormatId(String aLabel)
    {
        String writableFormat = "";
        for (String key : readWriteFileFormats.stringPropertyNames()) {
            if (key.contains(".label") && !isBlank(readWriteFileFormats.getProperty(key))) {
                if (readWriteFileFormats.getProperty(key).equals(aLabel)) {
                    writableFormat = key.substring(0, key.lastIndexOf(".label"));
                    break;
                }
            }
        }
        return writableFormat;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Map<String, Class<JCasAnnotator_ImplBase>> getWritableFormats()
        throws ClassNotFoundException
    {
        Map<String, Class<JCasAnnotator_ImplBase>> writableFormats = new HashMap<>();
        Set<String> keys = (Set) readWriteFileFormats.keySet();

        for (String keyvalue : keys) {
            if (keyvalue.contains(".label")) {
                String writerLabel = keyvalue.substring(0, keyvalue.lastIndexOf(".label"));
                if (readWriteFileFormats.getProperty(writerLabel + ".writer") != null) {
                    writableFormats.put(writerLabel, (Class) Class.forName(readWriteFileFormats
                            .getProperty(writerLabel + ".writer")));
                }
            }
        }
        return writableFormats;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public JCas importCasFromFile(File aFile, Project aProject, String aFormat)
        throws UIMAException, IOException, ClassNotFoundException
    {
        Class readerClass = getReadableFormats().get(aFormat);
        if (readerClass == null) {
            throw new IOException("No reader available for format [" + aFormat + "]");
        }
        
        // Prepare a CAS with the project type system
        TypeSystemDescription builtInTypes = TypeSystemDescriptionFactory
                .createTypeSystemDescription();
        List<TypeSystemDescription> projectTypes = annotationService.getProjectTypes(aProject);
        projectTypes.add(builtInTypes);
        TypeSystemDescription allTypes = CasCreationUtils.mergeTypeSystems(projectTypes);
        CAS cas = JCasFactory.createJCas(allTypes).getCas();

        // Convert the source document to CAS
        CollectionReader reader = CollectionReaderFactory.createReader(readerClass,
                ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION, aFile.getParentFile()
                        .getAbsolutePath(), ResourceCollectionReaderBase.PARAM_PATTERNS,
                new String[] { "[+]" + aFile.getName() });
        if (!reader.hasNext()) {
            throw new FileNotFoundException("Annotation file [" + aFile.getName()
                    + "] not found in [" + aFile.getPath() + "]");
        }
        reader.getNext(cas);
        JCas jCas = cas.getJCas();

        // Create sentence / token annotations if they are missing
        boolean hasTokens = JCasUtil.exists(jCas, Token.class);
        boolean hasSentences = JCasUtil.exists(jCas, Sentence.class);

        if (!hasTokens || !hasSentences) {
            AnalysisEngine pipeline = createEngine(createEngineDescription(
                    BreakIteratorSegmenter.class, BreakIteratorSegmenter.PARAM_WRITE_TOKEN,
                    !hasTokens, BreakIteratorSegmenter.PARAM_WRITE_SENTENCE, !hasSentences));
            pipeline.process(jCas);
        }

        return jCas;
    }
    
    /**
     * A new directory is created using UUID so that every exported file will reside in its own
     * directory. This is useful as the written file can have multiple extensions based on the
     * Writer class used.
     */
    @Override
    public File exportCasToFile(CAS cas, SourceDocument aDocument, String aFileName,
            @SuppressWarnings("rawtypes") Class aWriter, boolean aStripExtension)
        throws IOException, UIMAException
    {
        // Update the source file name in case it is changed for some reason. This is necessary
        // for the writers to create the files under the correct names.
        Project project = aDocument.getProject();
        File currentDocumentUri = new File(dir.getAbsolutePath() + PROJECT + project.getId()
                + DOCUMENT + aDocument.getId() + SOURCE);
        DocumentMetaData documentMetadata = DocumentMetaData.get(cas.getJCas());
        documentMetadata.setDocumentUri(new File(currentDocumentUri, aFileName).toURI().toURL()
                .toExternalForm());
        documentMetadata.setDocumentBaseUri(currentDocumentUri.toURI().toURL().toExternalForm());
        documentMetadata.setCollectionId(currentDocumentUri.toURI().toURL().toExternalForm());
        documentMetadata.setDocumentUri(new File(dir.getAbsolutePath() + PROJECT + project.getId()
                + DOCUMENT + aDocument.getId() + SOURCE + "/" + aFileName).toURI().toURL()
                .toExternalForm());

        // update with the correct tagset name
        List<AnnotationFeature> features = annotationService.listAnnotationFeature(project);
        for (AnnotationFeature feature : features) {

            TagSet tagSet = feature.getTagset();
            if (tagSet == null) {
                continue;
            }
            else if (!feature.getLayer().getType().equals(WebAnnoConst.CHAIN_TYPE)) {
                updateCasWithTagSet(cas, feature.getLayer().getName(), tagSet.getName());
            }
        }

        File exportTempDir = File.createTempFile("webanno", "export");
        try {
            exportTempDir.delete();
            exportTempDir.mkdirs();
            
            AnalysisEngineDescription writer;
            if (aWriter.getName()
                    .equals("de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv3Writer")) {
                List<AnnotationLayer> layers = annotationService.listAnnotationLayer(aDocument.getProject());
    
                List<String> slotFeatures = new ArrayList<>();
                List<String> slotTargets = new ArrayList<>();
                List<String> linkTypes = new ArrayList<>();
    
                Set<String> spanLayers = new HashSet<>();
                Set<String> slotLayers = new HashSet<>();
                for (AnnotationLayer layer : layers) {
                    
                    if (layer.getType().contentEquals(WebAnnoConst.SPAN_TYPE)) {
                        // TSV will not use this
                        if(!annotationExists(cas, layer.getName())){
                            continue;
                        }
                        boolean isslotLayer = false;
                        for (AnnotationFeature f : annotationService.listAnnotationFeature(layer)) {
                            if (MultiValueMode.ARRAY.equals(f.getMultiValueMode())
                                    && LinkMode.WITH_ROLE.equals(f.getLinkMode())) {
                                isslotLayer = true;
                                slotFeatures.add(layer.getName() + ":" + f.getName());
                                slotTargets.add(f.getType());
                                linkTypes.add(f.getLinkTypeName());
                            }
                        }
                        
                        if (isslotLayer) {
                            slotLayers.add(layer.getName());
                        } else {
                            spanLayers.add(layer.getName());
                        }
                    }
                }
                spanLayers.addAll(slotLayers);
                List<String> chainLayers = new ArrayList<>();
                for (AnnotationLayer layer : layers) {
                    if (layer.getType().contentEquals(WebAnnoConst.CHAIN_TYPE)) {
                        if(!chainAnnotationExists(cas, layer.getName()+"Chain")){
                            continue;
                        }
                        chainLayers.add(layer.getName());
                    }
                }
    
                List<String> relationLayers = new ArrayList<>();
                for (AnnotationLayer layer : layers) {
                    if (layer.getType().contentEquals(WebAnnoConst.RELATION_TYPE)) {
                        // TSV will not use this
                        if(!annotationExists(cas, layer.getName())){
                            continue;
                        }
                        relationLayers.add(layer.getName());
                    }
                }
    
                writer = createEngineDescription(aWriter, JCasFileWriter_ImplBase.PARAM_TARGET_LOCATION, exportTempDir,
                        JCasFileWriter_ImplBase.PARAM_STRIP_EXTENSION, aStripExtension, "spanLayers", spanLayers,
                        "slotFeatures", slotFeatures, "slotTargets", slotTargets, "linkTypes", linkTypes, "chainLayers",
                        chainLayers, "relationLayers", relationLayers);
            }
            else {
                writer = createEngineDescription(aWriter,
                        JCasFileWriter_ImplBase.PARAM_TARGET_LOCATION, exportTempDir,
                        JCasFileWriter_ImplBase.PARAM_STRIP_EXTENSION, aStripExtension);
            }
    
            runPipeline(cas, writer);
    
            // If the writer produced more than one file, we package it up as a ZIP file
            File exportFile;
            if (exportTempDir.listFiles().length > 1) {
                exportFile = new File(exportTempDir.getAbsolutePath() + ".zip");
                try {
                    ZipUtils.zipFolder(exportTempDir, exportFile);
                }
                catch (Exception e) {
                    try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                            String.valueOf(project.getId()))) {
                        log.info("Unable to create zip File");
                    }
                }
            }
            else {
                exportFile = new File(exportTempDir.getParent(), exportTempDir.listFiles()[0].getName());
                FileUtils.copyFile(exportTempDir.listFiles()[0], exportFile);
            }
            
            return exportFile;
        }
        finally {
            if (exportTempDir != null) {
                FileUtils.forceDelete(exportTempDir);
            }
        }
    }
    
    private boolean annotationExists(CAS aCas, String aType) {

        Type type = aCas.getTypeSystem().getType(aType);
        if (CasUtil.select(aCas, type).size() == 0) {
            return false;
        }
        return true;
    }
    
    private boolean chainAnnotationExists(CAS aCas, String aType) {

        Type type = aCas.getTypeSystem().getType(aType);
        if (CasUtil.selectFS(aCas, type).size() == 0) {
            return false;
        }
        return true;
    }
    
    /**
     * Check if a TAB-Sep training file is in correct format before importing
     */
    private boolean isTabSepFileFormatCorrect(File aFile)
    {
        try {
            LineIterator it = new LineIterator(new FileReader(aFile));
            while (it.hasNext()) {
                String line = it.next();
                if (line.trim().length() == 0) {
                    continue;
                }
                if (line.split("\t").length != 2) {
                    return false;
                }
            }
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * A Helper method to add {@link TagsetDescription} to {@link CAS}
     *
     * @param aCas
     *            the CAA.
     * @param aLayer
     *            the layer.
     * @param aTagSetName
     *            the tagset.
     */
    private static void updateCasWithTagSet(CAS aCas, String aLayer, String aTagSetName)
    {
        Type TagsetType = CasUtil.getType(aCas, TagsetDescription.class);
        Feature layerFeature = TagsetType.getFeatureByBaseName("layer");
        Feature nameFeature = TagsetType.getFeatureByBaseName("name");

        boolean tagSetModified = false;
        // modify existing tagset Name
        for (FeatureStructure fs : CasUtil.select(aCas, TagsetType)) {
            String layer = fs.getStringValue(layerFeature);
            String tagSetName = fs.getStringValue(nameFeature);
            if (layer.equals(aLayer)) {
                // only if the tagset name is changed
                if (!aTagSetName.equals(tagSetName)) {
                    fs.setStringValue(nameFeature, aTagSetName);
                    aCas.addFsToIndexes(fs);
                }
                tagSetModified = true;
                break;
            }
        }
        if (!tagSetModified) {
            FeatureStructure fs = aCas.createFS(TagsetType);
            fs.setStringValue(layerFeature, aLayer);
            fs.setStringValue(nameFeature, aTagSetName);
            aCas.addFsToIndexes(fs);
        }
    }
}
