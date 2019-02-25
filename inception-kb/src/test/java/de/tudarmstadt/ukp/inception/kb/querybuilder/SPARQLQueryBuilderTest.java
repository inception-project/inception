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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.parser.sparql.ast.ParseException;
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

import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class SPARQLQueryBuilderTest
{
    private static final String TURTLE_PREFIX = String.join("\n",
            "@base <http://example.org/> .",
            "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .",
            "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .");
    
    private static final String DATA_LABELS_WITH_LANGUAGE = String.join("\n",
            "<#green-goblin>",
            "    rdfs:label 'Green Goblin' ;",
            "    rdfs:label 'Green Goblin'@en .",
            "",
            "<#lucky-green>",
            "    rdfs:label 'Lucky Green' ;",
            "    rdfs:label 'Lucky Green'@en .",
            "",
            "<#red-goblin>",
            "    rdfs:label 'Red Goblin' .");

    private static final String DATA_LABELS_WITHOUT_LANGUAGE = String.join("\n",
            "<#green-goblin>",
            "    rdfs:label 'Green Goblin' .",
            "",
            "<#lucky-green>",
            "    rdfs:label 'Lucky Green' .",
            "",
            "<#red-goblin>",
            "    rdfs:label 'Red Goblin' .");

    private static final String LABEL_SUBPROPERTY = String.join("\n",
            "<#sublabel>",
            "    rdfs:subPropertyOf rdfs:label .",
            "",
            "<#green-goblin>",
            "    <#sublabel> 'Green Goblin' .");

    /**
     * This dataset contains a hierarchy of classes and instances with a naming scheme.
     * There is an implicit and an explicit root class. All classes have "class" in their name.
     * Subclasses start with "subclass" and then a number. Instances start with the number of the
     * class to which they belong followed by a number.
     */
    private static final String DATA_CLASS_RDFS_HIERARCHY = String.join("\n",
            "<#explicitRoot>",
            "    rdf:type rdfs:Class .",
            "<#subclass1>",
            "    rdf:type rdfs:Class ;",
            "    rdfs:subClassOf <#explicitRoot> .",
            "<#subclass1-1>",
            "    rdfs:subClassOf <#subclass1> .",
            "<#subclass2>",
            "    rdfs:subClassOf <#explicitRoot> .",
            "<#subclass3>",
            "    rdfs:subClassOf <#implictRoot> .",
            "<#0-instance-1>",
            "    rdf:type <#root> .",
            "<#1-instance-1>",
            "    rdf:type <#subclass1> .",
            "<#2-instance-2>",
            "    rdf:type <#subclass2> .",
            "<#3-instance-3>",
            "    rdf:type <#subclass3> ."
    );

    private KnowledgeBase kb;
    private Repository rdf4jLocalRepo;
    private Repository ukpVirtuosoRepo;
    private Repository zbwStw;
    private Repository zbwGnd;
    private Repository wikidata;
    private Repository dbpedia;
    private Repository hucit;
    
    @Before
    public void setup()
    {
        kb = new KnowledgeBase();
        kb.setDefaultLanguage("en");
        kb.setType(RepositoryType.LOCAL);
        kb.setClassIri(RDFS.CLASS);
        kb.setSubclassIri(RDFS.SUBCLASSOF);
        kb.setTypeIri(RDF.TYPE);
        kb.setLabelIri(RDFS.LABEL);
        kb.setPropertyTypeIri(RDF.PROPERTY);
        kb.setDescriptionIri(RDFS.COMMENT);
        kb.setPropertyLabelIri(RDFS.LABEL);
        kb.setPropertyDescriptionIri(RDFS.COMMENT);
        kb.setSubPropertyIri(RDFS.SUBPROPERTYOF);
        kb.setFullTextSearchIri(null);
        kb.setMaxResults(1000);
        
        // Local in-memory store - this should be used for most tests because we can
        // a) rely on its availability
        // b) import custom test data
        LuceneSail lucenesail = new LuceneSail();
        lucenesail.setParameter(LuceneSail.LUCENE_RAMDIR_KEY, "true");
        lucenesail.setBaseSail(new MemoryStore());
        rdf4jLocalRepo = new SailRepository(lucenesail);
        rdf4jLocalRepo.initialize();
        
        ukpVirtuosoRepo = new SPARQLRepository(
                "http://knowledgebase.ukp.informatik.tu-darmstadt.de:8890/sparql");
        ukpVirtuosoRepo.initialize();

        // http://zbw.eu/beta/sparql-lab/?endpoint=http://zbw.eu/beta/sparql/stw/query
        zbwStw = new SPARQLRepository("http://zbw.eu/beta/sparql/stw/query");
        zbwStw.initialize();

        // http://zbw.eu/beta/sparql-lab/?endpoint=http://zbw.eu/beta/sparql/gnd/query
        zbwGnd = new SPARQLRepository("http://zbw.eu/beta/sparql/gnd/query");
        zbwGnd.initialize();

        // https://query.wikidata.org/sparql
        wikidata = new SPARQLRepository("https://query.wikidata.org/sparql");
        wikidata.initialize();
        
        // https://dbpedia.org/sparql
        dbpedia = new SPARQLRepository("https://dbpedia.org/sparql");
        dbpedia.initialize();
        
        // http://nlp.dainst.org:8888/sparql
        hucit = new SPARQLRepository("http://nlp.dainst.org:8888/sparql");
        hucit.initialize();
    }
    
    @Test
    public void testLimitToClasses() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forClasses(kb);
        List<KBHandle> results = asHandles(rdf4jLocalRepo, builder);
        
        assertThat(results).isNotEmpty();
        assertThat(results)
                .extracting(KBHandle::getUiLabel)
                .noneMatch(label -> label.contains("instance"));
    }

    @Test
    public void testLimitToClassesInScope() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forClasses(kb);
        builder.withScope("http://example.org/#subclass1");
        List<KBHandle> results = asHandles(rdf4jLocalRepo, builder);
        
        assertThat(results).isNotEmpty();
        assertThat(results)
                .extracting(KBHandle::getName)
                .allMatch(label -> label.startsWith("subclass1-"));
    }

    @Test
    public void testLimitToInstances() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forInstances(kb);
        List<KBHandle> results = asHandles(rdf4jLocalRepo, builder);
        
        assertThat(results).isNotEmpty();
        assertThat(results)
                .extracting(KBHandle::getUiLabel)
                .noneMatch(label -> label.contains("class"));
    }

    @Test
    public void testLimitToInstancesInScope() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forInstances(kb);
        builder.withScope("http://example.org/#subclass1");
        List<KBHandle> results = asHandles(rdf4jLocalRepo, builder);
        
        assertThat(results).isNotEmpty();
        assertThat(results)
                .extracting(KBHandle::getName)
                .allMatch(label -> label.startsWith("1-instance-"));
    }

    @Test
    public void testLimitToItemsInScope() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX, DATA_CLASS_RDFS_HIERARCHY);

        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withScope("http://example.org/#subclass1");
        List<KBHandle> results = asHandles(rdf4jLocalRepo, builder);
        
        assertThat(results).isNotEmpty();
        assertThat(results)
                .extracting(KBHandle::getName)
                .allMatch(label -> label.startsWith("1-instance-") || label.startsWith("subclass1-"));
    }

    @Test
    public void testWithLabelContainingAnyOf_RDF4J_withLanguage_noFTS() throws Exception
    {
        kb.setFullTextSearchIri(null);
        
        __testWithLabelContainingAnyOf_RDF4J_withLanguage();
    }
    
    @Test
    public void testWithLabelContainingAnyOf_RDF4J_withLanguage_FTS() throws Exception
    {
        kb.setFullTextSearchIri(IriConstants.FTS_LUCENE);
        
        __testWithLabelContainingAnyOf_RDF4J_withLanguage();
    }
    
    public void __testWithLabelContainingAnyOf_RDF4J_withLanguage() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX, DATA_LABELS_WITH_LANGUAGE);

        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelContainingAnyOf("Goblin");
        List<KBHandle> results = asHandles(rdf4jLocalRepo, builder);
        
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.contains("Goblin"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results)
                .usingElementComparatorOnFields(
                        "identifier", "name", "description", "language")
                .containsExactlyInAnyOrder(
                        new KBHandle("http://example.org/#red-goblin", "Red Goblin"),
                        new KBHandle("http://example.org/#green-goblin", "Green Goblin",
                                null, "en"));
    }
    
    @Test
    public void testWithLabelMatchingExactlyAnyOf_RDF4J_withLanguage_noFTS() throws Exception
    {
        kb.setFullTextSearchIri(null);
        
        __testWithLabelMatchingExactlyAnyOf_RDF4J_withLanguage();
    }

    @Test
    public void testWithLabelMatchingExactlyAnyOf_RDF4J_withLanguage_FTS() throws Exception
    {
        kb.setFullTextSearchIri(IriConstants.FTS_LUCENE);
        
        __testWithLabelMatchingExactlyAnyOf_RDF4J_withLanguage();
    }

    public void __testWithLabelMatchingExactlyAnyOf_RDF4J_withLanguage() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX, DATA_LABELS_WITH_LANGUAGE);

        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelMatchingExactlyAnyOf("Green Goblin");
        List<KBHandle> results = asHandles(rdf4jLocalRepo, builder);
        
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.equals("Green Goblin"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results)
                .usingElementComparatorOnFields(
                        "identifier", "name", "description", "language")
                .containsExactlyInAnyOrder(
                        new KBHandle("http://example.org/#green-goblin", "Green Goblin",
                                null, "en"));
    }
    
    @Test
    public void testWithLabelMatchingExactlyAnyOf_RDF4J_subproperty_noFTS() throws Exception
    {
        kb.setFullTextSearchIri(null);
        kb.setSubPropertyIri(RDFS.SUBPROPERTYOF);
        kb.setLabelIri(RDFS.LABEL);
        
        __testWithLabelMatchingExactlyAnyOf_RDF4J_subproperty();
    }
    
    @Test
    public void testWithLabelMatchingExactlyAnyOf_RDF4J_subproperty_FTS() throws Exception
    {
        kb.setFullTextSearchIri(IriConstants.FTS_LUCENE);
        kb.setSubPropertyIri(RDFS.SUBPROPERTYOF);
        kb.setLabelIri(RDFS.LABEL);
        
        __testWithLabelMatchingExactlyAnyOf_RDF4J_subproperty();
    }
    
    public void __testWithLabelMatchingExactlyAnyOf_RDF4J_subproperty() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX, LABEL_SUBPROPERTY);
        
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        // The label "Green Goblin" is not assigned directly via rdfs:label but rather via a
        // subproperty of it. Thus, this test also checks if the label sub-property support works.
        builder.withLabelMatchingExactlyAnyOf("Green Goblin");
        List<KBHandle> results = asHandles(rdf4jLocalRepo, builder);
        
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.equals("Green Goblin"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results)
                .usingElementComparatorOnFields(
                        "identifier", "name", "description", "language")
                .containsExactlyInAnyOrder(
                        new KBHandle("http://example.org/#green-goblin", "Green Goblin"));
    }    
        
    @Test
    public void testWithLabelStartingWith_RDF4J_withoutLanguage_noFTS() throws Exception
    {
        kb.setFullTextSearchIri(null);
        
        __testWithLabelStartingWith_RDF4J_withoutLanguage();
    }

    @Test
    public void testWithLabelStartingWith_RDF4J_withoutLanguage_FTS() throws Exception
    {
        kb.setFullTextSearchIri(IriConstants.FTS_LUCENE);
        
        __testWithLabelStartingWith_RDF4J_withoutLanguage();
    }

    public void __testWithLabelStartingWith_RDF4J_withoutLanguage() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX, DATA_LABELS_WITHOUT_LANGUAGE);
    
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelStartingWith("Green");
        List<KBHandle> results = asHandles(rdf4jLocalRepo, builder);
        
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Green"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results)
                .usingElementComparatorOnFields(
                        "identifier", "name", "description", "language")
                .containsExactlyInAnyOrder(
                        new KBHandle("http://example.org/#green-goblin", "Green Goblin"));
    }

    @Test
    public void testWithLabelStartingWith_RDF4J_withLanguage_noFTS() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX, DATA_LABELS_WITH_LANGUAGE);

        kb.setFullTextSearchIri(null);
        
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelStartingWith("Green Goblin");
        List<KBHandle> results = asHandles(rdf4jLocalRepo, builder);
        
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Green"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results)
                .usingElementComparatorOnFields(
                        "identifier", "name", "description", "language")
                .containsExactlyInAnyOrder(
                        new KBHandle("http://example.org/#green-goblin", "Green Goblin",
                                null, "en"));
    }

    @Test
    public void testWithLabelStartingWith_RDF4J_withLanguage_FTS_1() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX, DATA_LABELS_WITH_LANGUAGE);

        kb.setFullTextSearchIri(IriConstants.FTS_LUCENE);
        
        // Single word - actually, we add a wildcard here so anything that starts with "Green"
        // would also be matched
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelStartingWith("Green");
        List<KBHandle> results = asHandles(rdf4jLocalRepo, builder);
        
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Green"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results)
                .usingElementComparatorOnFields(
                        "identifier", "name", "description", "language")
                .containsExactlyInAnyOrder(
                        new KBHandle("http://example.org/#green-goblin", "Green Goblin", null, 
                                "en"));
    }
    
    @Test
    public void testWithLabelStartingWith_RDF4J_withLanguage_FTS_2() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX, DATA_LABELS_WITH_LANGUAGE);

        kb.setFullTextSearchIri(IriConstants.FTS_LUCENE);
        
        // Two words with the second being very short - this is no problem for the LUCENE FTS
        // and we simply add a wildcard to match "Green Go*"
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelStartingWith("Green Go");
        List<KBHandle> results = asHandles(rdf4jLocalRepo, builder);
        
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Green Go"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results)
                .usingElementComparatorOnFields(
                        "identifier", "name", "description", "language")
                .containsExactlyInAnyOrder(
                        new KBHandle("http://example.org/#green-goblin", "Green Goblin",
                                null, "en"));
    }
    
    @Test
    public void testWithLabelStartingWith_RDF4J_withLanguage_FTS_3() throws Exception
    {
        importDataFromString(RDFFormat.TURTLE, TURTLE_PREFIX, DATA_LABELS_WITH_LANGUAGE);

        kb.setFullTextSearchIri(IriConstants.FTS_LUCENE);
        
        // Two words with the second being very short and a space following - in this case we
        // assume that the user is in fact searching for "Barack Ob" and do either drop the
        // last element nor add a wildcard
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelStartingWith("Green Go ");
        List<KBHandle> results = asHandles(rdf4jLocalRepo, builder);
        
        assertThat(results).isEmpty();
    }
    
    @Test
    public void testWithLabelContainingAnyOf_Virtuoso_withLanguage_FTS() throws Exception
    {
        kb.setFullTextSearchIri(IriConstants.FTS_VIRTUOSO);
        
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelContainingAnyOf("Tower");
        List<KBHandle> results = asHandles(ukpVirtuosoRepo, builder);
        
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.toLowerCase().contains("tower"));
    }

    @Test
    public void testWithLabelMatchingExactlyAnyOf_Virtuoso_withLanguage_FTS() throws Exception
    {
        kb.setFullTextSearchIri(IriConstants.FTS_VIRTUOSO);
        
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelMatchingExactlyAnyOf("Green Goblin");
        List<KBHandle> results = asHandles(ukpVirtuosoRepo, builder);
        
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> "Green Goblin".equals(label));
    }
    
    @Test
    public void testWithLabelStartingWith_Virtuoso_withLanguage_FTS_1() throws Exception
    {
        kb.setFullTextSearchIri(IriConstants.FTS_VIRTUOSO);
    
        // Single word - actually, we add a wildcard here so anything that starts with "Barack"
        // would also be matched
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelStartingWith("Barack");
        List<KBHandle> results = asHandles(ukpVirtuosoRepo, builder);
        
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Barack"));
    }

    @Test
    public void testWithLabelStartingWith_Virtuoso_withLanguage_FTS_2() throws Exception
    {
        kb.setFullTextSearchIri(IriConstants.FTS_VIRTUOSO);
    
        // Two words with the second being very short - in this case, we drop the very short word
        // so that the user doesn't stop getting suggestions while writing because Virtuoso doesn't
        // do wildcards on words shorter than 4 characters
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelStartingWith("Barack Ob");
        List<KBHandle> results = asHandles(ukpVirtuosoRepo, builder);
        
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Barack"));
    }

    @Test
    public void testWithLabelStartingWith_Virtuoso_withLanguage_FTS_3() throws Exception
    {
        kb.setFullTextSearchIri(IriConstants.FTS_VIRTUOSO);
    
        // Two words with the second being very short and a space following - in this case we
        // assmume that the user is in fact searching for "Barack Ob" and do either drop the
        // last element nor add a wildcard
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelStartingWith("Barack Ob ");
        List<KBHandle> results = asHandles(ukpVirtuosoRepo, builder);
        
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Barack"));
    }

    @Test
    public void testWithLabelStartingWith_Virtuoso_withLanguage_FTS_4() throws Exception
    {
        kb.setFullTextSearchIri(IriConstants.FTS_VIRTUOSO);
    
        // Two words with the second being 4+ chars - we add a wildcard here so anything
        // starting with "Barack Obam" should match
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelStartingWith("Barack Obam");
        List<KBHandle> results = asHandles(ukpVirtuosoRepo, builder);
        
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Barack Obam"));
    }

    @Test
    public void testWithLabelMatchingExactlyAnyOf_ZBW_noFTS() throws Exception
    {
        kb.setFullTextSearchIri(null);
        
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelMatchingExactlyAnyOf("Labour");
        List<KBHandle> results = asHandles(zbwStw, builder);
        
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> "Labour".equals(label));
    }

    @Test
    public void testWithLabelMatchingExactlyAnyOf_GND_noFTS() throws Exception
    {
        kb.setFullTextSearchIri(null);
        kb.setLabelIri(RDFS.LABEL);
        kb.setSubPropertyIri(RDFS.SUBPROPERTYOF);
        
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        // The label "Thomas Henricus" is not assigned directly via rdfs:label but rather via a
        // subproperty of it. Thus, this test also checks if the label sub-property support works.
        builder.withLabelMatchingExactlyAnyOf("Thomas Henricus");
        List<KBHandle> results = asHandles(zbwGnd, builder);
        
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> "Thomas Henricus".equals(label));
    }

    @Test
    public void testWithLabelMatchingExactlyAnyOf_Wikidata_noFTS() throws Exception
    {
        kb.setFullTextSearchIri(null);
        
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelMatchingExactlyAnyOf("Labour");
        List<KBHandle> results = asHandles(wikidata, builder);
        
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> "Labour".equals(label));
    }
    
    @Test
    public void testWithLabelStartingWith_HUCIT_noFTS() throws Exception
    {
        kb.setFullTextSearchIri(null);
        
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelStartingWith("Achilles");
        List<KBHandle> results = asHandles(hucit, builder);
        
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Achilles"));
    }

    @Test
    public void testWithLabelStartingWith_scoped_HUCIT_noFTS() throws Exception
    {
        kb.setFullTextSearchIri(null);
        
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forInstances(kb);
        builder.withLabelStartingWith("Achilles");
        builder.withScope("http://erlangen-crm.org/efrbroo/F1_Work");
        List<KBHandle> results = asHandles(hucit, builder);
        
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.startsWith("Achilles"));
    }

    @Test
    public void testWithLabelContainingAnyOf_classes_HUCIT_FTS() throws Exception
    {
        kb.setFullTextSearchIri(IriConstants.FTS_VIRTUOSO);
        
        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forClasses(kb);
        builder.withLabelContainingAnyOf("work");
        List<KBHandle> results = asHandles(hucit, builder);
        
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results).isNotEmpty();
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.toLowerCase().contains("work"));
    }

    private List<KBHandle> asHandles(Repository aRepo, SPARQLQueryBuilder aBuilder)
    {
        try (RepositoryConnection conn = aRepo.getConnection()) {
            System.out.printf("Query   : %n");
            Arrays.stream(aBuilder.selectQuery().getQueryString().split("\n"))
                    .forEachOrdered(l -> System.out.printf("          %s%n", l));
            
            long startTime = System.currentTimeMillis();

            List<KBHandle> results = aBuilder.asHandles(conn, true);

            System.out.printf("Results : %d in %dms%n", results.size(),
                    System.currentTimeMillis() - startTime);
            results.forEach(r -> System.out.printf("          %s%n", r));
            
            return results;
        }
        catch (MalformedQueryException e) {
            String[] queryStringLines = aBuilder.selectQuery().getQueryString().split("\n");
            if (e.getCause() instanceof ParseException) {
                ParseException cause = (ParseException) e.getCause();
                String message = String.format(
                        "Error: %s%n" +
                        "Bad query part starting with: %s%n",
                        e.getMessage(),
                        queryStringLines[cause.currentToken.beginLine - 1]
                                .substring(cause.currentToken.beginColumn - 1));
                throw new MalformedQueryException(message);
            }
            else {
                throw e;
            }
        }
    }
    
    @Test
    public void testWithLabelContainingAnyOf_RDF4J_pets_ttl() throws Exception
    {
        importDataFromFile("src/test/resources/data/pets.ttl");

        SPARQLQueryBuilder builder = SPARQLQueryBuilder.forItems(kb);
        builder.withLabelContainingAnyOf("Socke");
        builder.retrieveDescription();
        List<KBHandle> results = asHandles(rdf4jLocalRepo, builder);
        
        assertThat(results).extracting(KBHandle::getUiLabel)
                .allMatch(label -> label.contains("Socke"));
        assertThat(results).extracting(KBHandle::getIdentifier).doesNotHaveDuplicates();
        assertThat(results)
                .usingElementComparatorOnFields(
                        "identifier", "name", "description", "language")
                .containsExactlyInAnyOrder(
                        new KBHandle("http://mbugert.de/pets#socke", "Socke"));
    }

    private void importDataFromFile(String aFilename) throws IOException
    {
        // Detect the file format
        RDFFormat format = Rio.getParserFormatForFileName(aFilename).orElse(RDFFormat.RDFXML);

        // Load files into the repository
        try (InputStream is = new FileInputStream(aFilename)) {
            importData(format, is);
        }
    }
    
    private void importDataFromString(RDFFormat aFormat, String... aRdfData) throws IOException
    {
        String data = String.join("\n", aRdfData);
        
        // Load files into the repository
        try (InputStream is = IOUtils.toInputStream(data, UTF_8)) {
            importData(aFormat, is);
        }
    }
    
    private void importData(RDFFormat aFormat, InputStream aIS) throws IOException
    {
        try (RepositoryConnection conn = rdf4jLocalRepo.getConnection()) {
            // If the RDF file contains relative URLs, then they probably start with a hash.
            // To avoid having two hashes here, we drop the hash from the base prefix configured
            // by the user.
            String prefix = StringUtils.removeEnd(kb.getBasePrefix(), "#");
            conn.add(aIS, prefix, aFormat);
        }
    }
}
