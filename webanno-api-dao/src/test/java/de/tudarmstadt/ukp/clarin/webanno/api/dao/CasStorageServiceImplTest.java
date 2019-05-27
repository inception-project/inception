/*
 * Copyright 2018
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

import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.type.CASMetadata;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class CasStorageServiceImplTest
{
    private CasStorageService sut;
    private BackupProperties backupProperties;
    private RepositoryProperties repositoryProperties;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
    
    @Before
    public void setup() throws Exception
    {
        backupProperties = new BackupProperties();
        
        repositoryProperties = new RepositoryProperties();
        repositoryProperties.setPath(testFolder.newFolder());
        
        sut = new CasStorageServiceImpl(null, null, repositoryProperties, backupProperties);
    }
    
    @Test
    public void testWriteReadExistsDeleteCas() throws Exception
    {
        SourceDocument doc = makeSourceDocument(1l, 1l);
        JCas cas = JCasFactory.createText("This is a test");
        String user = "test";
        
        sut.writeCas(doc, cas.getCas(), user);
        assertThat(sut.getCasFile(doc, user)).exists();
        assertThat(sut.existsCas(doc, user)).isTrue();
        
        CAS cas2 = sut.readCas(doc, user);
        assertThat(cas2.getDocumentText()).isEqualTo(cas.getDocumentText());
        
        sut.deleteCas(doc, user);
        assertThat(sut.getCasFile(doc, user)).doesNotExist();
        assertThat(sut.existsCas(doc, user)).isFalse();
    }
    
    @Test
    public void testCasMetadataGetsCreated() throws Exception
    {
        List<TypeSystemDescription> typeSystems = new ArrayList<>();
        typeSystems.add(createTypeSystemDescription());
        typeSystems.add(CasMetadataUtils.getInternalTypeSystem());
        JCas cas = JCasFactory.createJCas(mergeTypeSystems(typeSystems));
        
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
        SourceDocument doc = makeSourceDocument(2l, 2l);
        String user = "test";
        
        JCas cas = sut.readOrCreateCas(doc, user, () -> {
            try {
                return JCasFactory.createText("This is a test").getCas();
            }
            catch (UIMAException e) {
                throw new IOException(e);
            }
        }).getJCas();
        assertThat(sut.getCasFile(doc, user)).exists();
        assertThat(sut.existsCas(doc, user)).isTrue();
        
        JCas cas2 = sut.readCas(doc, user).getJCas();
        assertThat(cas2.getDocumentText()).isEqualTo(cas.getDocumentText());
        
        sut.deleteCas(doc, user);
        assertThat(sut.getCasFile(doc, user)).doesNotExist();
        assertThat(sut.existsCas(doc, user)).isFalse();
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
