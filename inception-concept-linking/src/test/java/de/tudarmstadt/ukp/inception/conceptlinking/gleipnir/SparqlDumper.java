package de.tudarmstadt.ukp.inception.conceptlinking.gleipnir;

import java.util.Set;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.export.exporters.PermissionsExporter;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.conceptlinking.service.ConceptLinkingServiceImpl;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureValueType;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseServiceImpl;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.exporter.KnowledgeBaseExporter;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;

@RunWith(SpringRunner.class)
@Transactional
@DataJpaTest
@AutoConfigurationPackage
@ComponentScan(
        excludeFilters = {
                // We do now text exporting here and the exporter depends on the annotation schema
                // service which is otherwise not needed. So we exclude this component here.
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        KnowledgeBaseExporter.class,
                        KnowledgeBaseService.class,
                        PermissionsExporter.class
                })
        },
        basePackages = {
                "de.tudarmstadt.ukp.inception"
        })
@EntityScan(
        basePackages = {
                "de.tudarmstadt.ukp.inception.kb.model",
                "de.tudarmstadt.ukp.clarin.webanno.model"
        })
@ContextConfiguration(classes = {KnowledgeBaseServiceImpl.class,
        RepositoryProperties.class, FeatureSupportRegistryImpl.class})
public class SparqlDumper {

    @Autowired
    private KnowledgeBaseServiceImpl knowledgeBaseService;

    @Autowired
    private ConceptLinkingServiceImpl sut;

    @Test
    public void dumpSparqlFts() throws Exception {
        KnowledgeBase kb = buildSkosFuseki();
        // KnowledgeBase kb = buildWikidata();

        String mention = "$Mention";
        // mention = "$Mention";

        Set<KBHandle> candidates = sut.generateCandidates(kb, null, ConceptFeatureValueType.INSTANCE, "", mention);


        System.out.println(knowledgeBaseService.readInstance(kb, "$IRI"));
    }

    private KnowledgeBase buildSkosFuseki() {
        KnowledgeBase kb = new KnowledgeBase();

        ValueFactory vf = SimpleValueFactory.getInstance();
        kb.setClassIri(vf.createIRI("http://www.w3.org/2004/02/skos/core#Concept"));
        kb.setSubclassIri(vf.createIRI("http://www.w3.org/2004/02/skos/core#broader"));
        kb.setTypeIri(vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
        kb.setDescriptionIri(vf.createIRI("http://www.w3.org/2000/01/rdf-schema#comment"));
        kb.setLabelIri(vf.createIRI("http://www.w3.org/2004/02/skos/core#prefLabel"));

        kb.setPropertyTypeIri(vf.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property"));
        kb.setSubPropertyIri(vf.createIRI("http://www.w3.org/2000/01/rdf-schema#subPropertyOf"));
        kb.setPropertyLabelIri(vf.createIRI("http://www.w3.org/2004/02/skos/core#prefLabel"));
        kb.setPropertyDescriptionIri(vf.createIRI("http://www.w3.org/2000/01/rdf-schema#comment"));
        kb.setFullTextSearchIri(IriConstants.FTS_FUSEKI);

        kb.setType(RepositoryType.REMOTE);
        kb.setName("wwo");
        kb.setReification(Reification.NONE);

        Project project  = new Project();
        project.setId(42L);
        kb.setProject(project);
        kb.setDefaultLanguage("en");
        kb.setMaxResults(1000);

        RepositoryImplConfig config = knowledgeBaseService.getRemoteConfig("http://localhost:3030/depositions/query");
        knowledgeBaseService.registerKnowledgeBase(kb, config);

        return kb;
    }

    private KnowledgeBase buildWikidata() {
        KnowledgeBase kb = new KnowledgeBase();

        ValueFactory vf = SimpleValueFactory.getInstance();
        kb.setClassIri(vf.createIRI("http://www.wikidata.org/entity/Q35120"));
        kb.setSubclassIri(vf.createIRI("http://www.wikidata.org/prop/direct/P279"));
        kb.setTypeIri(vf.createIRI("http://www.wikidata.org/prop/direct/P31"));
        kb.setDescriptionIri(vf.createIRI("http://schema.org/description"));
        kb.setLabelIri(vf.createIRI("http://www.w3.org/2000/01/rdf-schema#label"));

        kb.setPropertyTypeIri(vf.createIRI("http://www.wikidata.org/entity/Q18616576"));
        kb.setSubPropertyIri(vf.createIRI("http://www.wikidata.org/prop/direct/P1647"));
        kb.setPropertyLabelIri(vf.createIRI("http://www.w3.org/2000/01/rdf-schema#label"));
        kb.setPropertyDescriptionIri(vf.createIRI("http://www.w3.org/2000/01/rdf-schema#comment"));
        kb.setFullTextSearchIri(IriConstants.FTS_WIKIDATA);

        kb.setType(RepositoryType.REMOTE);
        kb.setName("wikidata");
        kb.setReification(Reification.NONE);

        Project project  = new Project();
        project.setId(42L);
        kb.setProject(project);
        kb.setDefaultLanguage("en");
        kb.setMaxResults(1000);

        RepositoryImplConfig config = knowledgeBaseService.getRemoteConfig("https://query.wikidata.org/sparql");
        knowledgeBaseService.registerKnowledgeBase(kb, config);

        return kb;
    }
}
