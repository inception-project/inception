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
package de.tudarmstadt.ukp.inception.export;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_ACCESS;
import static de.tudarmstadt.ukp.inception.project.api.ProjectService.withProjectLogger;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.exists;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getRealCas;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.select;
import static org.apache.uima.fit.util.FSUtil.setFeature;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctorImpl;
import de.tudarmstadt.ukp.clarin.webanno.diag.ChecksRegistry;
import de.tudarmstadt.ukp.clarin.webanno.diag.RepairsRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceProperties;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceProperties.CasDoctorOnImportPolicy;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.BaseLoggers;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.support.uima.SegmentationUtils;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DocumentImportExportServiceAutoConfiguration#documentImportExportService}.
 * </p>
 */
public class DocumentImportExportServiceImpl
    implements DocumentImportExportService
{
    static final String FEATURE_BASE_NAME_UI_NAME = "uiName";
    static final String FEATURE_BASE_NAME_NAME = "name";
    static final String FEATURE_BASE_NAME_LAYER = "layer";
    static final String TYPE_NAME_FEATURE_DEFINITION = "de.tudarmstadt.ukp.clarin.webanno.api.type.FeatureDefinition";
    static final String TYPE_NAME_LAYER_DEFINITION = "de.tudarmstadt.ukp.clarin.webanno.api.type.LayerDefinition";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CasStorageService casStorageService;
    private final AnnotationSchemaService annotationService;
    private final DocumentImportExportServiceProperties properties;
    private final ChecksRegistry checksRegistry;
    private final RepairsRegistry repairsRegistry;

    private final FormatSupport fallbackFormat;

    private final List<FormatSupport> formatsProxy;
    private Map<String, FormatSupport> formats;

    private final TypeSystemDescription schemaTypeSystem;

    public DocumentImportExportServiceImpl(
            @Lazy @Autowired(required = false) List<FormatSupport> aFormats,
            CasStorageService aCasStorageService, AnnotationSchemaService aAnnotationService,
            DocumentImportExportServiceProperties aServiceProperties,
            ChecksRegistry aChecksRegistry, RepairsRegistry aRepairsRegistry,
            FormatSupport aFallbackFormat)
    {
        casStorageService = aCasStorageService;
        annotationService = aAnnotationService;
        formatsProxy = aFormats;
        properties = aServiceProperties;
        checksRegistry = aChecksRegistry;
        repairsRegistry = aRepairsRegistry;
        fallbackFormat = aFallbackFormat;

        schemaTypeSystem = createTypeSystemDescription(
                "de/tudarmstadt/ukp/clarin/webanno/api/type/schema-types");
    }

    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshedEvent()
    {
        init();
    }

    /* package private */ void init()
    {
        var formatMap = new LinkedHashMap<String, FormatSupport>();

        if (formatsProxy != null) {
            var forms = new ArrayList<>(formatsProxy);
            AnnotationAwareOrderComparator.sort(forms);
            forms.forEach(format -> {
                formatMap.put(format.getId(), format);
                LOG.debug("Found format: {} ({}, {})",
                        ClassUtils.getAbbreviatedName(format.getClass(), 20), format.getId(),
                        readWriteMsg(format));
            });
        }

        BaseLoggers.BOOT_LOG.info("Found [{}] format supports", formatMap.size());

        formats = unmodifiableMap(formatMap);
    }

    private String readWriteMsg(FormatSupport aFormat)
    {
        if (aFormat.isReadable() && !aFormat.isWritable()) {
            return "read only";
        }

        if (!aFormat.isReadable() && aFormat.isWritable()) {
            return "write only";
        }

        if (aFormat.isReadable() && aFormat.isWritable()) {
            return "read/write";
        }

        throw new IllegalStateException(
                "Format [" + aFormat.getId() + "] must be at least readable or writable.");
    }

    @Override
    public List<FormatSupport> getFormats()
    {
        return unmodifiableList(new ArrayList<>(formats.values()));
    }

    @Override
    public FormatSupport getFallbackFormat()
    {
        return fallbackFormat;
    }

    @Override
    @Transactional
    public File exportAnnotationDocument(SourceDocument aDocument, String aDataOwner,
            FormatSupport aFormat, Mode aMode)
        throws UIMAException, IOException
    {
        return exportAnnotationDocument(aDocument, aDataOwner, aFormat, aDocument.getName(), aMode,
                true, null);
    }

    @Override
    @Transactional
    public File exportAnnotationDocument(SourceDocument aDocument, String aDataOwner,
            FormatSupport aFormat, String aFileName, Mode aMode)
        throws UIMAException, IOException
    {
        return exportAnnotationDocument(aDocument, aDataOwner, aFormat, aFileName, aMode, true,
                null);
    }

    @Override
    @Transactional
    public File exportAnnotationDocument(SourceDocument aDocument, String aDataOwner,
            FormatSupport aFormat, Mode aMode, boolean aStripExtension,
            Map<Pair<Project, String>, Object> aBulkOperationContext)
        throws IOException, UIMAException
    {
        return exportAnnotationDocument(aDocument, aDataOwner, aFormat, aDocument.getName(), aMode,
                aStripExtension, aBulkOperationContext);
    }

    @Override
    @Transactional
    public File exportAnnotationDocument(SourceDocument aDocument, String aDataOwner,
            FormatSupport aFormat, String aFileName, Mode aMode, boolean aStripExtension,
            Map<Pair<Project, String>, Object> aBulkOperationContext)
        throws IOException, UIMAException
    {
        try (var logCtx = withProjectLogger(aDocument.getProject())) {
            var bulkOperationContext = aBulkOperationContext;
            if (bulkOperationContext == null) {
                bulkOperationContext = new HashMap<>();
            }

            AnnotationSet set;
            switch (aMode) {
            case ANNOTATION:
                set = AnnotationSet.forUser(aDataOwner);
                break;
            case CURATION:
                // The merge result will be exported
                set = AnnotationSet.CURATION_SET;
                break;
            default:
                throw new IllegalArgumentException("Unknown mode [" + aMode + "]");
            }

            // Read file
            File exportFile;
            try (var session = CasStorageSession.openNested()) {
                // We do not want to add the CAS to the exclusive access pool here to avoid
                // potentially running out of memory when exporting a large project
                var cas = casStorageService.readCas(aDocument, set, UNMANAGED_ACCESS);
                exportFile = exportCasToFile(cas, aDocument, set.id(), aFormat, aStripExtension,
                        bulkOperationContext);
            }

            LOG.info("Exported annotations for [{}]@{} in {} using format [{}]", aDataOwner,
                    aDocument, aDocument.getProject(), aFormat.getId());

            return exportFile;
        }
    }

    @Override
    public CAS importCasFromFile(File aFile, SourceDocument aDocument)
        throws UIMAException, IOException
    {
        return importCasFromFile(aFile, aDocument, null);
    }

    @Override
    public CAS importCasFromFile(File aFile, SourceDocument aDocument,
            TypeSystemDescription aFullProjectTypeSystem)
        throws UIMAException, IOException
    {
        return importCasFromFile(aFile, aDocument, aDocument.getFormat(), aFullProjectTypeSystem);
    }

    @Override
    public CAS importCasFromFile(File aFile, SourceDocument aDocument, String aFormat,
            TypeSystemDescription aFullProjectTypeSystem)
        throws UIMAException, IOException
    {
        var cas = importCasFromFileNoChecks(aFile, aDocument, aFormat, aFullProjectTypeSystem);

        // Convert the source document to CAS
        var format = getReadableFormatById(aFormat).orElseThrow(
                () -> new IOException("No reader available for format [" + aFormat + "]"));

        runCasDoctorOnImport(aDocument, format, cas);

        return cas;
    }

    @Override
    public CAS importCasFromFileNoChecks(File aFile, SourceDocument aDocument)
        throws UIMAException, IOException
    {
        return importCasFromFileNoChecks(aFile, aDocument, aDocument.getFormat(), null);
    }

    @Override
    public CAS importCasFromFileNoChecks(File aFile, SourceDocument aDocument,
            TypeSystemDescription aFullProjectTypeSystem)
        throws UIMAException, IOException
    {
        return importCasFromFileNoChecks(aFile, aDocument, aDocument.getFormat(),
                aFullProjectTypeSystem);
    }

    private CAS importCasFromFileNoChecks(File aFile, SourceDocument aDocument, String aFormat,
            TypeSystemDescription aFullProjectTypeSystem)
        throws UIMAException, IOException
    {
        var tsd = aFullProjectTypeSystem;

        if (tsd == null) {
            tsd = annotationService.getFullProjectTypeSystem(aDocument.getProject());
        }

        // Convert the source document to CAS
        var format = getReadableFormatById(aFormat).orElseThrow(
                () -> new IOException("No reader available for format [" + aFormat + "]"));

        // Prepare a CAS with the project type system
        var cas = WebAnnoCasUtil.createCas(tsd);
        format.read(aDocument.getProject(), WebAnnoCasUtil.getRealCas(cas), aFile);

        // Create sentence / token annotations if they are missing - sentences first because
        // tokens are then generated inside the sentences
        splitSenencesIfNecssary(cas, format);
        checkSentenceQuota(cas, format);

        splitTokens(cas, format);
        checkTokenQuota(cas, format);

        LOG.info("Imported CAS with [{}] tokens and [{}] sentences from file [{}] (size: {} bytes)",
                cas.getAnnotationIndex(getType(cas, Token.class)).size(),
                cas.getAnnotationIndex(getType(cas, Sentence.class)).size(), aFile, aFile.length());

        return cas;
    }

    private void runCasDoctorOnImport(SourceDocument aDocument, FormatSupport aFormat, CAS aCas)
    {
        if (properties.getRunCasDoctorOnImport() == CasDoctorOnImportPolicy.OFF) {
            return;
        }

        if (properties.getRunCasDoctorOnImport() == CasDoctorOnImportPolicy.AUTO
                && !aFormat.isProneToInconsistencies()) {
            return;
        }

        var messages = new ArrayList<LogMessage>();
        var casDoctor = new CasDoctorImpl(checksRegistry, repairsRegistry);
        casDoctor.setActiveChecks(
                checksRegistry.getExtensions().stream().map(c -> c.getId()).toArray(String[]::new));
        casDoctor.analyze(aDocument, INITIAL_CAS_PSEUDO_USER, aCas, messages, true);
    }

    private void splitTokens(CAS cas, FormatSupport aFormat) throws IOException
    {
        var tokenType = getType(cas, Token.class);

        if (!exists(cas, tokenType)) {
            SegmentationUtils.tokenize(cas);
        }
    }

    private void checkTokenQuota(CAS cas, FormatSupport aFormat) throws IOException
    {
        var tokenType = getType(cas, Token.class);

        if (properties.getMaxTokens() > 0) {
            var tokenCount = cas.getAnnotationIndex(tokenType).size();
            if (tokenCount > properties.getMaxTokens()) {
                throw new IOException("Number of tokens [" + tokenCount + "] exceeds limit ["
                        + properties.getMaxTokens()
                        + "]. Maybe file does not conform to the format [" + aFormat.getName()
                        + "]? Otherwise, increase the global token limit in the settings file.");
            }
        }

        if (!exists(cas, tokenType)) {
            throw new IOException("The document appears to be empty. Unable to detect any "
                    + "tokens. Empty documents cannot be imported.");
        }
    }

    private void splitSenencesIfNecssary(CAS cas, FormatSupport aFormat) throws IOException
    {
        var sentenceType = getType(cas, Sentence.class);

        if (!exists(cas, sentenceType)) {
            SegmentationUtils.splitSentences(cas);
        }
    }

    private void checkSentenceQuota(CAS cas, FormatSupport aFormat) throws IOException
    {
        var sentenceType = getType(cas, Sentence.class);

        if (properties.getMaxSentences() > 0) {
            var sentenceCount = cas.getAnnotationIndex(sentenceType).size();
            if (sentenceCount > properties.getMaxSentences()) {
                throw new IOException("Number of sentences [" + sentenceCount + "] exceeds limit ["
                        + properties.getMaxSentences()
                        + "]. Maybe file does not conform to the format [" + aFormat.getName()
                        + "]? Otherwise, increase the global sentence limit in the settings file.");
            }
        }

        if (!exists(cas, sentenceType)) {
            throw new IOException("The document appears to be empty. Unable to detect any "
                    + "sentences. Empty documents cannot be imported.");
        }
    }

    @Override
    public File exportCasToFile(CAS aCas, SourceDocument aDocument, String aDataOwner,
            FormatSupport aFormat)
        throws IOException, UIMAException
    {
        return exportCasToFile(aCas, aDocument, aDataOwner, aFormat, true, null);
    }

    @Override
    public File exportCasToFile(CAS aCas, SourceDocument aDocument, String aDataOwner,
            FormatSupport aFormat, boolean aStripExtension,
            Map<Pair<Project, String>, Object> aBulkOperationContext)
        throws IOException, UIMAException
    {
        var project = aDocument.getProject();
        try (var logCtx = withProjectLogger(project)) {
            var bulkOperationContext = aBulkOperationContext;
            if (bulkOperationContext == null) {
                bulkOperationContext = new HashMap<>();
            }

            // Either fetch the type system from the bulk-context or fetch it from the DB and store
            // it in the bulk-context to avoid further lookups in the same bulk operation
            var exportTypeSystemKey = Pair.of(project, "exportTypeSystem");
            var exportTypeSystem = (TypeSystemDescription) bulkOperationContext
                    .get(exportTypeSystemKey);
            if (exportTypeSystem == null) {
                exportTypeSystem = getTypeSystemForExport(project);
                bulkOperationContext.put(exportTypeSystemKey, exportTypeSystem);
            }

            try (var session = CasStorageSession.openNested()) {
                var exportCas = WebAnnoCasUtil.createCas();
                session.add(AnnotationSet.EXPORT_SET, EXCLUSIVE_WRITE_ACCESS, exportCas);

                // Update type system the CAS, compact it (remove all non-reachable feature
                // structures) and remove all internal feature structures in the process
                prepareCasForExport(aCas, exportCas, aDocument, exportTypeSystem);

                // Update the source file name in case it is changed for some reason. This is
                // necessary for the writers to create the files under the correct names.
                addOrUpdateDocumentMetadata(exportCas, aDocument, aDataOwner);

                addLayerAndFeatureDefinitionAnnotations(exportCas, project, bulkOperationContext);

                addTagsetDefinitionAnnotations(exportCas, project, bulkOperationContext);

                var exportTempDir = Files.createTempDirectory("inception-export").toFile();
                try {
                    return aFormat.write(aDocument, getRealCas(exportCas), exportTempDir,
                            aStripExtension);
                }
                finally {
                    if (exportTempDir != null) {
                        forceDelete(exportTempDir);
                    }
                }
            }
        }
    }

    @Override
    public TypeSystemDescription getExportSpecificTypes()
    {
        return (TypeSystemDescription) schemaTypeSystem.clone();
    }

    @Override
    public TypeSystemDescription getTypeSystemForExport(Project aProject)
        throws ResourceInitializationException
    {
        return mergeTypeSystems(asList( //
                schemaTypeSystem, //
                annotationService.getFullProjectTypeSystem(aProject, false)));
    }

    /**
     * Performs a CAS upgrade and removes all internal feature structures from the CAS. The
     * resulting CAS should be <b>only</b> used for export and never be persisted within the
     * repository.
     * 
     * @param aSourceCas
     *            the source CAS
     * @param aTargetCas
     *            the target CAS
     * @param aSourceDocument
     *            the document the source CAS belongs to
     * @param aFullProjectTypeSystem
     *            the project's full type system ({@link #getTypeSystemForExport}). Used to speed up
     *            bulk exports. If null, the type system is fetched from the project.
     * @throws ResourceInitializationException
     *             if there was a problem obtaining the type system
     * @throws UIMAException
     *             if there was an UIMA-level problem
     * @throws IOException
     *             if there was an I/O-level problem
     */
    public void prepareCasForExport(CAS aSourceCas, CAS aTargetCas, SourceDocument aSourceDocument,
            TypeSystemDescription aFullProjectTypeSystem)
        throws ResourceInitializationException, UIMAException, IOException
    {
        var tsd = aFullProjectTypeSystem;
        if (tsd == null) {
            tsd = getTypeSystemForExport(aSourceDocument.getProject());
        }

        annotationService.upgradeCas(aSourceCas, aTargetCas, tsd);
    }

    private List<AnnotationFeature> listSupportedFeatures(Project aProject,
            Map<Pair<Project, String>, Object> aBulkOperationContext)
    {
        var exportFeaturesKey = Pair.of(aProject, "exportFeatures");
        @SuppressWarnings("unchecked")
        var features = (List<AnnotationFeature>) aBulkOperationContext.get(exportFeaturesKey);
        if (features == null) {
            features = annotationService.listSupportedFeatures(aProject).stream() //
                    .filter(AnnotationFeature::isEnabled) //
                    .toList();
            aBulkOperationContext.put(exportFeaturesKey, features);
        }

        return features;
    }

    static void addOrUpdateDocumentMetadata(CAS aCas, SourceDocument aDocument, String aDataOwner)
        throws MalformedURLException, CASException
    {
        var slug = aDocument.getProject().getSlug();
        var documentMetadata = DocumentMetaData.get(aCas.getJCas());
        documentMetadata.setDocumentTitle(aDocument.getName());
        documentMetadata.setCollectionId(slug);
        documentMetadata.setDocumentId(aDataOwner);
        documentMetadata.setDocumentBaseUri(slug);
        documentMetadata.setDocumentUri(slug + "/" + aDocument.getName());
    }

    private void addLayerAndFeatureDefinitionAnnotations(CAS aCas, Project aProject,
            Map<Pair<Project, String>, Object> aBulkOperationContext)
    {
        var allFeatures = listSupportedFeatures(aProject, aBulkOperationContext);

        var layerDefType = aCas.getTypeSystem().getType(TYPE_NAME_LAYER_DEFINITION);
        var featureDefType = aCas.getTypeSystem().getType(TYPE_NAME_FEATURE_DEFINITION);

        var featuresGroupedByLayer = allFeatures.stream() //
                .collect(groupingBy(AnnotationFeature::getLayer));

        var layers = featuresGroupedByLayer.keySet().stream() //
                .sorted(comparing(AnnotationLayer::getName)) //
                .toList();

        for (var layer : layers) {
            final var layerDefFs = aCas.createFS(layerDefType);
            setFeature(layerDefFs, FEATURE_BASE_NAME_NAME, layer.getName());
            setFeature(layerDefFs, FEATURE_BASE_NAME_UI_NAME, layer.getUiName());
            aCas.addFsToIndexes(layerDefFs);

            var features = featuresGroupedByLayer.get(layer).stream() //
                    .sorted(comparing(AnnotationFeature::getName)) //
                    .toList();

            for (var feature : features) {
                final var featureDefFs = aCas.createFS(featureDefType);
                setFeature(featureDefFs, FEATURE_BASE_NAME_LAYER, layerDefFs);
                setFeature(featureDefFs, FEATURE_BASE_NAME_NAME, feature.getName());
                setFeature(featureDefFs, FEATURE_BASE_NAME_UI_NAME, feature.getUiName());
                aCas.addFsToIndexes(featureDefFs);
            }
        }
    }

    private void addTagsetDefinitionAnnotations(CAS aCas, Project aProject,
            Map<Pair<Project, String>, Object> aBulkOperationContext)
    {
        var features = listSupportedFeatures(aProject, aBulkOperationContext);

        for (var feature : features) {
            var tagSet = feature.getTagset();
            if (tagSet == null || ChainLayerSupport.TYPE.equals(feature.getLayer().getType())) {
                continue;
            }

            var aLayer = feature.getLayer().getName();
            var aTagSetName = tagSet.getName();
            var tagsetType = getType(aCas, TagsetDescription.class);
            var layerFeature = tagsetType.getFeatureByBaseName(FEATURE_BASE_NAME_LAYER);
            var nameFeature = tagsetType.getFeatureByBaseName(FEATURE_BASE_NAME_NAME);

            var tagSetModified = false;
            // modify existing tagset Name
            for (var fs : select(aCas, tagsetType)) {
                var layer = fs.getStringValue(layerFeature);
                var tagSetName = fs.getStringValue(nameFeature);
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
                var fs = aCas.createFS(tagsetType);
                fs.setStringValue(layerFeature, aLayer);
                fs.setStringValue(nameFeature, aTagSetName);
                aCas.addFsToIndexes(fs);
            }
        }
    }
}
