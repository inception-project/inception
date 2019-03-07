/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.kb.querybuilder;

import static de.tudarmstadt.ukp.inception.kb.querybuilder.SPARQLQueryBuilderAsserts.asHandles;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

@RunWith(Parameterized.class)
public class SPARQLQueryBuilderGenericTest
{
    private static final List<String> SKIPPED_PROFILES = asList("babel_net", "yago");
    
    @Parameterized.Parameters(name = "KB = {0}")
    public static List<Object[]> data() throws Exception
    {
        Map<String, KnowledgeBaseProfile> profiles = KnowledgeBaseProfile.readKnowledgeBaseProfiles();
        
        List<Object[]> dataList = new ArrayList<>();
        for (Entry<String, KnowledgeBaseProfile> entry : profiles.entrySet()) {
            if (SKIPPED_PROFILES.contains(entry.getKey())) {
                continue;
            }
            
            dataList.add(new Object[] { entry.getKey(), entry.getValue() });
        }
        return dataList;
    }
    
    private final String profileName;
    private final KnowledgeBaseProfile profile;
    
    private KnowledgeBase kb;
    private Repository repo;
    
    public SPARQLQueryBuilderGenericTest(String aProfileName, KnowledgeBaseProfile aProfile) throws Exception
    {
        profileName = aProfileName;
        profile = aProfile;
    }
    
    @Before
    public void setup() throws Exception
    {
        // Force POST request instead of GET request
        // System.setProperty(SPARQLProtocolSession.MAXIMUM_URL_LENGTH_PARAM, "100");
        
        ValueFactory vf = SimpleValueFactory.getInstance();
        
        kb = new KnowledgeBase();
        kb.setDefaultLanguage(profile.getDefaultLanguage());
        kb.setType(profile.getType());
        
        kb.setClassIri(profile.getMapping().getClassIri());
        kb.setSubclassIri(profile.getMapping().getSubclassIri());
        kb.setTypeIri(profile.getMapping().getTypeIri());
        kb.setLabelIri(profile.getMapping().getLabelIri());
        kb.setDescriptionIri(profile.getMapping().getDescriptionIri());
        
        kb.setSubPropertyIri(profile.getMapping().getSubPropertyIri());
        kb.setPropertyTypeIri(profile.getMapping().getPropertyTypeIri());
        kb.setPropertyLabelIri(profile.getMapping().getPropertyLabelIri());
        kb.setPropertyDescriptionIri(profile.getMapping().getPropertyDescriptionIri());
        
        kb.setFullTextSearchIri(profile.getAccess().getFullTextSearchIri());
        
        kb.setDefaultDatasetIri(profile.getDefaultDataset());

        kb.setRootConcepts(profile.getRootConcepts().stream().map(vf::createIRI).collect(toList()));
        
        kb.setMaxResults(1000);

        switch (kb.getType()) {
        case LOCAL: {
            LuceneSail lucenesail = new LuceneSail();
            lucenesail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
            lucenesail.setBaseSail(new MemoryStore());
            repo = new SailRepository(lucenesail);
            repo.init();
            importData(repo, profile.getAccess().getAccessUrl());
            break;
        }
        case REMOTE: {
            repo = new SPARQLRepository(profile.getAccess().getAccessUrl());
            repo.init();
            break;
        }
        default:
            throw new IllegalStateException("Unknown repo type: " + kb.getType());
        }
    }

    @Test
    public void thatRootConceptsCanBeRetrieved()
    {
        List<KBHandle> roots = asHandles(repo, SPARQLQueryBuilder.forClasses(kb).roots());
        
        assertThat(roots).isNotEmpty();
    }

    @Test
    public void thatChildrenOfRootConceptHaveRootConceptAsParent()
    {
        List<KBHandle> roots = asHandles(repo, SPARQLQueryBuilder
                .forClasses(kb)
                .roots()
                .limit(3));
        Set<String> rootIdentifiers = roots.stream().map(KBHandle::getIdentifier).collect(toSet());
        
        assertThat(roots).extracting(KBHandle::getIdentifier).allMatch(_root -> {
            try (RepositoryConnection conn = repo.getConnection()) {
//                System.out.print("R"); 
                List<KBHandle> children = SPARQLQueryBuilder
                        .forClasses(kb)
                        .childrenOf(_root)
                        .asHandles(conn, true);
                
                return children.stream().map(KBHandle::getIdentifier).allMatch(_child -> 
                        SPARQLQueryBuilder
                                .forClasses(kb)
                                .parentsOf(_child)
                                .limit(3)
                                .asHandles(conn, true)
                                .stream()
                                .map(KBHandle::getIdentifier)
//                                .map(v -> { 
//                                    System.out.print("C"); 
//                                    return v;
//                                })
                                .anyMatch(iri -> rootIdentifiers.contains(iri)));
            }
        });
    }
    
    @SuppressWarnings("resource")
    private void importData(Repository aRepo, String aUrl) throws IOException
    {
        try (InputStream aIS = openAsStream(aUrl)) {
            InputStream is = new BufferedInputStream(aIS);
            try {
                // Stream is expected to be closed by caller of importData
                is = new CompressorStreamFactory().createCompressorInputStream(is);
            }
            catch (CompressorException e) {
                // Probably not compressed then or unknown format - just try as is.
            }
    
            // Detect the file format
            RDFFormat format = Rio.getParserFormatForFileName(aUrl).orElse(RDFFormat.RDFXML);
            
            try (RepositoryConnection conn = aRepo.getConnection()) {
                // If the RDF file contains relative URLs, then they probably start with a hash.
                // To avoid having two hashes here, we drop the hash from the base prefix configured
                // by the user.
                String prefix = StringUtils.removeEnd(kb.getBasePrefix(), "#");
                conn.add(is, prefix, format);
            }
        }
    }
    
    private InputStream openAsStream(String aUrl) throws MalformedURLException, IOException
    {
        if (aUrl.startsWith("classpath")) {
            return new PathMatchingResourcePatternResolver().getResource(aUrl).getInputStream();
        }
        else {
            return new URL(aUrl).openStream();
        }
    }
}
