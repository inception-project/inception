package de.tudarmstadt.ukp.inception.kb.reification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.InceptionValueMapper;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

/**
 * wd:Q12418 p:P186 ?statement1.    # Mona Lisa: material used: ?statement1
 * ?statement1 ps:P186 wd:Q296955.  # value: oil paint
 *
 * wd:Q12418 p:P186 ?statement2.    # Mona Lisa: material used: ?statement2
 * ?statement2 ps:P186 wd:Q291034.  # value: poplar wood
 * ?statement2 pq:P518 wd:Q861259.  # qualifier: applies to part: painting surface
 *
 * wd:Q12418 p:P186 ?statement3.    # Mona Lisa: material used: ?statement3
 * ?statement3 ps:P186 wd:Q287.     # value: wood
 * ?statement3 pq:P518 wd:Q1737943. # qualifier: applies to part: stretcher bar
 * ?statement3 pq:P580 1951.        # qualifier: start time: 1951 (pseudo-syntax)
 */
public class WikiDataReification
    implements ReificationStrategy
{
    private static final String NAMPESPACE_ROOT = "https://github.com/inception-project";
    private static final String PREDICATE_NAMESPACE = NAMPESPACE_ROOT + "/predicate#";
    private static final String QUALIFIER_NAMESPACE = NAMPESPACE_ROOT + "/qualifier#";

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final KnowledgeBaseService kbService;
    private final InceptionValueMapper valueMapper;
    private final ValueFactory vf;

    public WikiDataReification(KnowledgeBaseService aKbService)
    {
        valueMapper = new InceptionValueMapper();
        vf = SimpleValueFactory.getInstance();

        kbService = aKbService;
        kbService.registerImplicitNamespace(PREDICATE_NAMESPACE);
        kbService.registerImplicitNamespace(QUALIFIER_NAMESPACE);
    }

    @Override
    public List<Statement> reify(KnowledgeBase kb, KBStatement aStatement) {
        KBHandle instance = aStatement.getInstance();
        KBHandle property = aStatement.getProperty();
        Value value = valueMapper.mapStatementValue(aStatement, vf);

        IRI subject = vf.createIRI(instance.getIdentifier());
        IRI predicate = vf.createIRI(property.getIdentifier());
        Resource id = vf.createBNode();

        Statement root = vf.createStatement(subject, predicate, id);
        IRI valuePredicate = vf.createIRI(PREDICATE_NAMESPACE, predicate.getLocalName());
        Statement valueStatement = vf.createStatement(id, valuePredicate, value);

        List<Statement> statements = new ArrayList<>();
        statements.add(root);           // S    P   id
        statements.add(valueStatement); // id   p_s V

        System.out.println(statements);

        return statements;
    }

    @Override
    public List<KBStatement> listStatements(KnowledgeBase kb, KBHandle aInstance, boolean aAll) {
        String QUERY = String.join("\n"
                     , "SELECT DISTINCT ?p ?o ?id ?ps WHERE {"
                     , "  ?s  ?p  ?id ."
                     , "  ?id ?ps ?o ."
                     , "  FILTER(STRSTARTS(STR(?ps), STR(?ps_ns)))"
                     , "}"
                     , "LIMIT 10000");

        IRI instance = vf.createIRI(aInstance.getIdentifier());
        try(RepositoryConnection conn = kbService.getConnection(kb)) {
            TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, QUERY);
            tupleQuery.setBinding("s", instance);
            tupleQuery.setBinding("ps_ns", vf.createIRI(PREDICATE_NAMESPACE));

            tupleQuery.setIncludeInferred(false);
            TupleQueryResult result;

            try {
                result = tupleQuery.evaluate();
            } catch (QueryEvaluationException e) {
                log.warn("Listing statements failed.", e);
                return Collections.emptyList();
            }

            List<KBStatement> statements = new ArrayList<>();

            while (result.hasNext()) {
                BindingSet bindings = result.next();
                Binding p = bindings.getBinding("p");
                Binding o = bindings.getBinding("o");
                Binding id = bindings.getBinding("id");
                Binding ps = bindings.getBinding("ps");
                Value value = o.getValue();

                // Fill kbStatement
                KBHandle property = new KBHandle();
                property.setIdentifier(p.getValue().stringValue());
                KBStatement kbStatement = new KBStatement(aInstance, property, value);

                // Recreate original statements
                Resource idResource = vf.createBNode(id.getValue().stringValue());
                IRI predicate = vf.createIRI(property.getIdentifier());
                Statement root = vf.createStatement(instance, predicate, idResource);

                IRI valuePredicate = vf.createIRI(ps.getValue().stringValue());
                Statement valueStatement = vf.createStatement(idResource, valuePredicate, value);
                List<Statement> originalStatements = new ArrayList<>();

                originalStatements.add(root);
                originalStatements.add(valueStatement);
                kbStatement.setOriginalStatements(originalStatements);

                statements.add(kbStatement);
            }
            return statements;
        }
    }


}
