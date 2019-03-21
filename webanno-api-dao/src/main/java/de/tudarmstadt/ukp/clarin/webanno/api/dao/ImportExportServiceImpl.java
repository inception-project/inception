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

import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.DOCUMENT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.PROJECT_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.ProjectService.SOURCE_FOLDER;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createSentence;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createToken;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.exists;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectSentences;
import static java.util.Collections.unmodifiableList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.apache.uima.fit.util.CasUtil.getType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.ImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.ZipUtils;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

@Component(ImportExportService.SERVICE_NAME)
public class ImportExportServiceImpl
    implements ImportExportService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RepositoryProperties repositoryProperties;
    private final CasStorageService casStorageService;
    private final AnnotationSchemaService annotationService;

    private final List<FormatSupport> formatsProxy;
    private Map<String, FormatSupport> formats;
    
    public ImportExportServiceImpl(
            @Autowired RepositoryProperties aRepositoryProperties, 
            @Lazy @Autowired(required = false) List<FormatSupport> aFormats,
            @Autowired CasStorageService aCasStorageService,
            @Autowired AnnotationSchemaService aAnnotationService)
    {
        repositoryProperties = aRepositoryProperties;
        casStorageService = aCasStorageService;
        annotationService = aAnnotationService;
        formatsProxy = aFormats;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshedEvent()
    {
        init();
    }
    
    /* package private */ void init()
    {
        Map<String, FormatSupport> formatMap = new LinkedHashMap<>();
        
        if (formatsProxy != null) {
            List<FormatSupport> forms = new ArrayList<>(formatsProxy);
            AnnotationAwareOrderComparator.sort(forms);
            forms.forEach(format -> {
                formatMap.put(format.getId(), format);
                log.info("Found format: {} ({}, {})",
                        ClassUtils.getAbbreviatedName(format.getClass(), 20), format.getId(),
                        readWriteMsg(format));
            });
        }
        
        // Parse "formats.properties" information into format supports
//        if (readWriteFileFormats != null) {
//            for (String key : readWriteFileFormats.stringPropertyNames()) {
//                if (key.endsWith(".label")) {
//                    String formatId = key.substring(0, key.lastIndexOf(".label"));
//                    String formatName = readWriteFileFormats.getProperty(key);
//                    String readerClass = readWriteFileFormats.getProperty(formatId + ".reader");
//                    String writerClass = readWriteFileFormats.getProperty(formatId + ".writer");
//                    
//                    if (formatMap.containsKey(formatId)) {
//                        log.info("Found format (format.properties): {} - format already defined by "
//                                + "a built-in format support, ignoring entry from "
//                                + "formats.properties file", formatId);
//                    }
//                    else {
//                        FormatSupport format = new FormatSupportDescription(formatId, formatName,
//                                readerClass, writerClass);
//                        formatMap.put(format.getId(), format);
//                        log.info("Found format (format.properties): {} ({})", formatId,
//                                readWriteMsg(format));
//                    }
//                }
//            }
//        }
        
        formats = Collections.unmodifiableMap(formatMap);
    }
    
    private String readWriteMsg(FormatSupport aFormat)
    {
        if (aFormat.isReadable() && !aFormat.isWritable()) {
            return "read only";
        }
        else if (!aFormat.isReadable() && aFormat.isWritable()) {
            return "write only";
        }
        else if (aFormat.isReadable() && aFormat.isWritable()) {
            return "read/write";
        }
        else {
            throw new IllegalStateException(
                    "Format [" + aFormat.getId() + "] must be at least readable or writable.");
        }
    }
    
    @Override
    public List<FormatSupport> getFormats()
    {
        return unmodifiableList(new ArrayList<>(formats.values()));
    }
    
    /**
     * A new directory is created using UUID so that every exported file will reside in its own
     * directory. This is useful as the written file can have multiple extensions based on the
     * Writer class used.
     */
    @Override
    @Transactional
    public File exportAnnotationDocument(SourceDocument aDocument, String aUser,
            FormatSupport aFormat, String aFileName, Mode aMode)
        throws UIMAException, IOException, ClassNotFoundException
    {
        return exportAnnotationDocument(aDocument, aUser, aFormat, aFileName, aMode, true);
    }

    @Override
    @Transactional
    public File exportAnnotationDocument(SourceDocument aDocument, String aUser,
            FormatSupport aFormat, String aFileName, Mode aMode, boolean aStripExtension)
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
        CasPersistenceUtils.readSerializedCas(cas, serializedCasFile);

        File exportFile = exportCasToFile(cas, aDocument, aFileName, aFormat, aStripExtension);

        Project project = aDocument.getProject();
        
        try (MDC.MDCCloseable closable = MDC.putCloseable(Logging.KEY_PROJECT_ID,
                String.valueOf(project.getId()))) {
            log.info("Exported annotations [{}]({}) for user [{}] from project [{}]({}) "
                    + "using format [{}]", aDocument.getName(), aDocument.getId(), aUser, 
                    project.getName(), project.getId(), aFormat.getId());
        }

        return exportFile;
    }
    
    @Override
    public CAS importCasFromFile(File aFile, Project aProject, String aFormatId)
        throws UIMAException, IOException
    {
        // Prepare a CAS with the project type system
        TypeSystemDescription allTypes = annotationService.getFullProjectTypeSystem(aProject);
        CAS cas = JCasFactory.createJCas(allTypes).getCas();

        // Convert the source document to CAS
        FormatSupport format = getReadableFormatById(aFormatId).orElseThrow(() -> 
                new IOException("No reader available for format [" + aFormatId + "]"));
        
        CollectionReaderDescription readerDescription = format.getReaderDescription();
        ConfigurationParameterFactory.addConfigurationParameters(readerDescription, 
                ResourceCollectionReaderBase.PARAM_SOURCE_LOCATION, 
                    aFile.getParentFile().getAbsolutePath(), 
                ResourceCollectionReaderBase.PARAM_PATTERNS,
                    new String[] { "[+]" + aFile.getName() });
        CollectionReader reader = createReader(readerDescription);
        
        if (!reader.hasNext()) {
            throw new FileNotFoundException(
                    "Source file [" + aFile.getName() + "] not found in [" + aFile.getPath() + "]");
        }
        reader.getNext(cas);
        CAS jCas = cas;

        // Create sentence / token annotations if they are missing
        boolean hasTokens = exists(jCas, getType(jCas, Token.class));
        boolean hasSentences = exists(jCas, getType(jCas, Sentence.class));

//        if (!hasTokens || !hasSentences) {
//            AnalysisEngine pipeline = createEngine(createEngineDescription(
//                    BreakIteratorSegmenter.class, 
//                    BreakIteratorSegmenter.PARAM_WRITE_TOKEN, !hasTokens,
//                    BreakIteratorSegmenter.PARAM_WRITE_SENTENCE, !hasSentences));
//            pipeline.process(jCas);
//        }
        
        if (!hasSentences) {
            splitSentences(jCas);
        }

        if (!hasTokens) {
            tokenize(jCas);
        }
        
        if (!exists(jCas, getType(jCas, Token.class))
                || !exists(jCas, getType(jCas, Sentence.class))) {
            throw new IOException("The document appears to be empty. Unable to detect any "
                    + "tokens or sentences. Empty documents cannot be imported.");
        }
        
        return jCas;
    }
    
    public static void splitSentences(CAS aJCas)
    {
        BreakIterator bi = BreakIterator.getSentenceInstance(Locale.US);
        bi.setText(aJCas.getDocumentText());
        int last = bi.first();
        int cur = bi.next();
        while (cur != BreakIterator.DONE) {
            int[] span = new int[] { last, cur };
            trim(aJCas.getDocumentText(), span);
            if (!isEmpty(span[0], span[1])) {
                aJCas.addFsToIndexes(createSentence(aJCas, span[0], span[1]));
            }
            last = cur;
            cur = bi.next();
        }
    }
    
    public static void tokenize(CAS aJCas)
    {
        BreakIterator bi = BreakIterator.getWordInstance(Locale.US);
        for (AnnotationFS s : selectSentences(aJCas)) {
            bi.setText(s.getCoveredText());
            int last = bi.first();
            int cur = bi.next();
            while (cur != BreakIterator.DONE) {
                int[] span = new int[] { last, cur };
                trim(s.getCoveredText(), span);
                if (!isEmpty(span[0], span[1])) {
                    aJCas.addFsToIndexes(
                            createToken(aJCas, span[0] + s.getBegin(), span[1] + s.getBegin()));
                }
                last = cur;
                cur = bi.next();
            }
        }
    }
    
    /**
     * Remove trailing or leading whitespace from the annotation.
     * 
     * @param aText
     *            the text.
     * @param aSpan
     *            the offsets.
     */
    public static void trim(String aText, int[] aSpan)
    {
        String data = aText;

        int begin = aSpan[0];
        int end = aSpan[1] - 1;

        // Remove whitespace at end
        while ((end > 0) && trimChar(data.charAt(end))) {
            end--;
        }
        end++;

        // Remove whitespace at start
        while ((begin < end) && trimChar(data.charAt(begin))) {
            begin++;
        }

        aSpan[0] = begin;
        aSpan[1] = end;
    }

    public static boolean isEmpty(int aBegin, int aEnd)
    {
        return aBegin >= aEnd;
    }

    public static boolean trimChar(final char aChar)
    {
        switch (aChar) {
        case '\n':     return true; // Line break
        case '\r':     return true; // Carriage return
        case '\t':     return true; // Tab
        case '\u200E': return true; // LEFT-TO-RIGHT MARK
        case '\u200F': return true; // RIGHT-TO-LEFT MARK
        case '\u2028': return true; // LINE SEPARATOR
        case '\u2029': return true; // PARAGRAPH SEPARATOR
        default:
            return  Character.isWhitespace(aChar);
        }
    }    
    
    @Override
    public File exportCasToFile(CAS aCas, SourceDocument aDocument, String aFileName,
            FormatSupport aFormat, boolean aStripExtension)
        throws IOException, UIMAException
    {
        // Update type system the CAS, compact it (remove all non-reachable feature strucutres)
        // and remove all internal feature structures in the process
        CAS cas = annotationService.prepareCasForExport(aCas, aDocument);
        
        // Update the source file name in case it is changed for some reason. This is necessary
        // for the writers to create the files under the correct names.
        Project project = aDocument.getProject();
        File currentDocumentUri = new File(repositoryProperties.getPath().getAbsolutePath() + "/"
                + PROJECT_FOLDER + "/" + project.getId() + "/" + DOCUMENT_FOLDER + "/"
                + aDocument.getId() + "/" + SOURCE_FOLDER);
        DocumentMetaData documentMetadata = DocumentMetaData.get(cas.getJCas());
        documentMetadata.setDocumentBaseUri(currentDocumentUri.toURI().toURL().toExternalForm());
        documentMetadata.setDocumentUri(new File(currentDocumentUri, aFileName).toURI().toURL()
                .toExternalForm());
        documentMetadata.setCollectionId(currentDocumentUri.toURI().toURL().toExternalForm());
        documentMetadata.setDocumentId(aFileName);

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
            
            AnalysisEngineDescription writer = aFormat.getWriterDescription(aDocument.getProject(),
                    cas);
            ConfigurationParameterFactory.addConfigurationParameters(writer,
                    JCasFileWriter_ImplBase.PARAM_USE_DOCUMENT_ID, true,
                    JCasFileWriter_ImplBase.PARAM_ESCAPE_DOCUMENT_ID, false,
                    JCasFileWriter_ImplBase.PARAM_TARGET_LOCATION, exportTempDir,
                    JCasFileWriter_ImplBase.PARAM_STRIP_EXTENSION, aStripExtension);

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
                exportFile = new File(exportTempDir.getParent(),
                        exportTempDir.listFiles()[0].getName());
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
