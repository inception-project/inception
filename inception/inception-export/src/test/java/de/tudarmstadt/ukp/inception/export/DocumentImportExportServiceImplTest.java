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
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.inception.annotation.storage.CasMetadataUtils.getInternalTypeSystem;
import static de.tudarmstadt.ukp.inception.export.DocumentImportExportServiceImpl.FEATURE_BASE_NAME_LAYER;
import static de.tudarmstadt.ukp.inception.export.DocumentImportExportServiceImpl.FEATURE_BASE_NAME_NAME;
import static de.tudarmstadt.ukp.inception.export.DocumentImportExportServiceImpl.FEATURE_BASE_NAME_UI_NAME;
import static de.tudarmstadt.ukp.inception.export.DocumentImportExportServiceImpl.TYPE_NAME_FEATURE_DEFINITION;
import static de.tudarmstadt.ukp.inception.export.DocumentImportExportServiceImpl.TYPE_NAME_LAYER_DEFINITION;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.fit.factory.JCasFactory.createJCas;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.diag.ChecksRegistry;
import de.tudarmstadt.ukp.clarin.webanno.diag.RepairsRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageServiceImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageBackupProperties;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageCachePropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStoragePropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.storage.driver.filesystem.FileSystemCasStorageDriver;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryPropertiesImpl;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServicePropertiesImpl;
import de.tudarmstadt.ukp.inception.io.xmi.XmiFormatSupport;
import de.tudarmstadt.ukp.inception.io.xmi.config.UimaFormatsPropertiesImpl.XmiFormatProperties;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.service.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.inception.support.logging.Logging;

@ExtendWith(MockitoExtension.class)
public class DocumentImportExportServiceImplTest
{
    private CasStorageSession casStorageSession;
    private @Spy AnnotationSchemaService schemaService;
    private @Mock ChecksRegistry checksRegistry;
    private @Mock RepairsRegistry repairsRegistry;

    public @TempDir File testFolder;

    private DocumentImportExportServiceImpl sut;

    private TypeSystemDescription typesystem;
    private Project project;
    private SourceDocument sourceDocument;

    @BeforeEach
    public void setup() throws Exception
    {
        // schemaService = mock(AnnotationSchemaServiceImpl.class);
        schemaService = Mockito.spy(new AnnotationSchemaServiceImpl());

        var properties = new DocumentImportExportServicePropertiesImpl();

        var repositoryProperties = new RepositoryPropertiesImpl();
        repositoryProperties.setPath(testFolder);

        MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

        var driver = new FileSystemCasStorageDriver(repositoryProperties,
                new CasStorageBackupProperties(), new CasStoragePropertiesImpl());

        var storageService = new CasStorageServiceImpl(driver, new CasStorageCachePropertiesImpl(),
                null, null);

        var xmiFormatSupport = new XmiFormatSupport(new XmiFormatProperties());
        sut = new DocumentImportExportServiceImpl(List.of(xmiFormatSupport), storageService,
                schemaService, properties, checksRegistry, repairsRegistry, xmiFormatSupport);
        sut.onContextRefreshedEvent();

        doReturn(emptyList()).when(schemaService).listSupportedLayers(any());
        doReturn(emptyList()).when(schemaService).listSupportedFeatures((Project) any());

        // The prepareCasForExport method internally calls getFullProjectTypeSystem, so we need to
        // ensure this is actually callable and doesn't run into a mocked version which simply
        // returns null.
        when(schemaService.getFullProjectTypeSystem(any(), anyBoolean())).thenCallRealMethod();
        doCallRealMethod().when(schemaService).upgradeCas(any(), any(),
                any(TypeSystemDescription.class));

        // Create type system with built-in types, internal types, but without any project-specific
        // types.
        typesystem = mergeTypeSystems(
                asList(createTypeSystemDescription(), getInternalTypeSystem()));

        project = new Project();
        project.setId(1l);

        sourceDocument = new SourceDocument();
        sourceDocument.setProject(project);
        sourceDocument.setId(1l);

        casStorageSession = CasStorageSession.open();
    }

    @AfterEach
    public void tearDown()
    {
        CasStorageSession.get().close();
    }

    @Test
    public void thatExportContainsNoCasMetadata() throws Exception
    {
        var jcas = makeJCas();

        // Pass the CAS through the export mechanism. Write as XMI because that is one of the
        // formats which best retains the information from the CAS and is nicely human-readable
        // if the test needs to be debugged.
        var exportedXmi = sut.exportCasToFile(jcas.getCas(), sourceDocument, "testfile",
                sut.getFormatById(XmiFormatSupport.ID).get());

        var jcas2 = loadJCasFromZippedXmi(exportedXmi);

        assertThat(select(jcas2, CASMetadata.class)).hasSize(0);
    }

    @Test
    public void thatExportedCasContainsLayerAndFeatureDefinitions() throws Exception
    {
        var l1 = new AnnotationLayer("my.A", "A", SpanLayerSupport.TYPE, project, false, TOKENS,
                NO_OVERLAP);
        var l2 = new AnnotationLayer("my.B", "B", SpanLayerSupport.TYPE, project, false, TOKENS,
                NO_OVERLAP);
        var f1 = new AnnotationFeature(project, l1, "f1", "feature1", TYPE_NAME_STRING);
        var f2 = new AnnotationFeature(project, l2, "f2", "feature2", TYPE_NAME_STRING);

        var features = asList(f1, f2);

        doReturn(features).when(schemaService).listSupportedFeatures((Project) any());

        var jcas = makeJCas();

        File exportedXmi = sut.exportCasToFile(jcas.getCas(), sourceDocument, "testfile",
                sut.getFormatById(XmiFormatSupport.ID).get());

        var jcas2 = loadJCasFromZippedXmi(exportedXmi);
        var layerDefs = jcas2.select(TYPE_NAME_LAYER_DEFINITION).asList().stream()
                .sorted(comparing(fs -> getFeature(fs, FEATURE_BASE_NAME_NAME, String.class)))
                .collect(Collectors.toList());
        var featureDefs = jcas2.select(TYPE_NAME_FEATURE_DEFINITION).asList().stream()
                .sorted(comparing(fs -> getFeature(fs, FEATURE_BASE_NAME_NAME, String.class)))
                .collect(Collectors.toList());

        assertThat(layerDefs) //
                .extracting( //
                        fs -> getFeature(fs, FEATURE_BASE_NAME_NAME, String.class),
                        fs -> getFeature(fs, FEATURE_BASE_NAME_UI_NAME, String.class))
                .containsExactly( //
                        tuple(l1.getName(), l1.getUiName()), //
                        tuple(l2.getName(), l2.getUiName()));
        assertThat(featureDefs) //
                .extracting( //
                        fs -> getFeature(fs, FEATURE_BASE_NAME_LAYER, TOP.class),
                        fs -> getFeature(fs, FEATURE_BASE_NAME_NAME, String.class),
                        fs -> getFeature(fs, FEATURE_BASE_NAME_UI_NAME, String.class))
                .containsExactly( //
                        tuple(layerDefs.get(0), f1.getName(), f1.getUiName()), //
                        tuple(layerDefs.get(1), f2.getName(), f2.getUiName()));
    }

    private JCas makeJCas() throws Exception
    {
        // Prepare a test CAS with a CASMetadata annotation (DocumentMetaData is added as well
        // because the DKPro Core writers used by the ImportExportService expect it.
        var jcas = createJCas(typesystem);
        casStorageSession.add(AnnotationSet.forTest("jcas"), EXCLUSIVE_WRITE_ACCESS, jcas.getCas());
        jcas.setDocumentText("This is a test .");
        DocumentMetaData.create(jcas);
        var cmd = new CASMetadata(jcas);
        cmd.addToIndexes(jcas);
        return jcas;
    }

    private JCas loadJCasFromZippedXmi(File exportedXmi) throws Exception
    {
        var tsd = mergeTypeSystems(asList( //
                sut.getTypeSystemForExport(project),
                schemaService.getFullProjectTypeSystem(project)));

        // Read the XMI back from the ZIP that was created by the exporter. This is because XMI
        // files are always serialized as XMI file + type system file.
        var jcas = JCasFactory.createJCas(tsd);
        casStorageSession.add(AnnotationSet.forTest("jcas2"), EXCLUSIVE_WRITE_ACCESS,
                jcas.getCas());
        try (var zipInput = new ZipArchiveInputStream(new FileInputStream(exportedXmi))) {
            ZipArchiveEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                if (entry.getName().endsWith(".xmi")) {
                    XmiCasDeserializer.deserialize(zipInput, jcas.getCas());
                    break;
                }
            }
        }
        finally {
            exportedXmi.delete();
        }
        return jcas;
    }
}
