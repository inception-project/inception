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
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.NO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession.openNested;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasSessionException;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.event.LayerConfigurationChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CasStorageServiceImplTest
{
    private CasStorageServiceImpl sut;
    private BackupProperties backupProperties;
    private RepositoryProperties repositoryProperties;
    private CasStorageSession casStorageSession;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setup() throws Exception
    {
        casStorageSession = CasStorageSession.open();

        backupProperties = new BackupProperties();

        repositoryProperties = new RepositoryProperties();
        repositoryProperties.setPath(testFolder.newFolder());

        sut = new CasStorageServiceImpl(null, null, repositoryProperties, backupProperties);
    }

    @After
    public void tearDown()
    {
        CasStorageSession.get().close();
    }

    @Test
    public void testWriteReadExistsDeleteCas() throws Exception
    {
        // Setup fixture
        SourceDocument doc = makeSourceDocument(1l, 1l);
        JCas templateCas = JCasFactory.createText("This is a test");
        casStorageSession.add("cas", EXCLUSIVE_WRITE_ACCESS, templateCas.getCas());
        String user = "test";

        sut.writeCas(doc, templateCas.getCas(), user);
        assertThat(sut.getCasFile(doc, user)).exists();
        assertThat(sut.existsCas(doc, user)).isTrue();

        // Actual test
        CAS cas = sut.readCas(doc, user);
        assertThat(cas.getDocumentText()).isEqualTo(templateCas.getDocumentText());

        sut.deleteCas(doc, user);
        assertThat(sut.getCasFile(doc, user)).doesNotExist();
        assertThat(sut.existsCas(doc, user)).isFalse();
        assertThat(casStorageSession.contains(cas)).isFalse();
    }

    @Test
    public void testCasMetadataGetsCreated() throws Exception
    {
        List<TypeSystemDescription> typeSystems = new ArrayList<>();
        typeSystems.add(createTypeSystemDescription());
        typeSystems.add(CasMetadataUtils.getInternalTypeSystem());

        JCas cas = JCasFactory.createJCas(mergeTypeSystems(typeSystems));
        casStorageSession.add("cas", EXCLUSIVE_WRITE_ACCESS, cas.getCas());

        SourceDocument doc = makeSourceDocument(1l, 1l);
        String user = "test";

        sut.writeCas(doc, cas.getCas(), user);

        JCas cas2 = sut.readCas(doc, user).getJCas();

        List<CASMetadata> cmds = new ArrayList<>(select(cas2, CASMetadata.class));
        assertThat(cmds).hasSize(1);
        assertThat(cmds.get(0).getProjectId()).isEqualTo(doc.getProject().getId());
        assertThat(cmds.get(0).getSourceDocumentId()).isEqualTo(doc.getId());
        assertThat(cmds.get(0).getLastChangedOnDisk())
                .isEqualTo(sut.getCasTimestamp(doc, user).get());
    }

    @Test
    public void testReadOrCreateCas() throws Exception
    {
        // Setup fixture
        SourceDocument doc = makeSourceDocument(2l, 2l);
        String user = "test";
        String text = "This is a test";
        createCasFile(doc, user, text);

        // Actual test
        JCas cas = sut.readCas(doc, user).getJCas();
        assertThat(cas.getDocumentText()).isEqualTo(text);

        sut.deleteCas(doc, user);
        assertThat(sut.getCasFile(doc, user)).doesNotExist();
        assertThat(sut.existsCas(doc, user)).isFalse();
    }

    @Test
    public void testThatLayerChangeEventInvalidatesCachedCas() throws Exception
    {
        // Setup fixture
        SourceDocument doc = makeSourceDocument(2l, 2l);
        String user = "test";
        try (CasStorageSession session = openNested(true)) {
            String text = "This is a test";
            createCasFile(doc, user, text);
        }

        // Actual test
        int casIdentity1;
        try (CasStorageSession session = openNested(true)) {
            JCas cas = sut.readCas(doc, user).getJCas();
            casIdentity1 = System.identityHashCode(cas);
        }

        int casIdentity2;
        try (CasStorageSession session = openNested(true)) {
            JCas cas = sut.readCas(doc, user).getJCas();
            casIdentity2 = System.identityHashCode(cas);
        }

        sut.beforeLayerConfigurationChanged(
                new LayerConfigurationChangedEvent(this, doc.getProject()));

        int casIdentity3;
        try (CasStorageSession session = openNested(true)) {
            JCas cas = sut.readCas(doc, user).getJCas();
            casIdentity3 = System.identityHashCode(cas);
        }

        assertThat(casIdentity1)
                .as("Getting the CAS a second time returns the same instance from memory")
                .isEqualTo(casIdentity2);
        assertThat(casIdentity1)
                .as("After a type system change event must return a different CAS instance")
                .isNotEqualTo(casIdentity3);
    }

    private JCas createCasFile(SourceDocument doc, String user, String text)
        throws CASException, CasSessionException, IOException
    {
        JCas casTemplate = sut.readOrCreateCas(doc, user, NO_CAS_UPGRADE, () -> {
            try {
                return JCasFactory.createText(text).getCas();
            }
            catch (UIMAException e) {
                throw new IOException(e);
            }
        }, EXCLUSIVE_WRITE_ACCESS).getJCas();
        assertThat(sut.getCasFile(doc, user)).exists();
        assertThat(sut.existsCas(doc, user)).isTrue();

        return casTemplate;
    }

    private SourceDocument makeSourceDocument(long aProjectId, long aDocumentId)
    {
        Project project = new Project();
        project.setId(aProjectId);

        SourceDocument doc = new SourceDocument();
        doc.setProject(project);
        doc.setId(aDocumentId);

        return doc;
    }
}
