package de.tudarmstadt.ukp.inception.search.index.mtas;

import java.util.Optional;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.RdfUtils;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class KBUtility {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private KnowledgeBaseService kbService;

    public KBUtility() {

        kbService = ApplicationContextProvider.getApplicationContext()
                .getBean(KnowledgeBaseService.class);

    }
    
    /**
     * Return Class type of KBObject
     * @param kbObject
     * @return
     */
    public String getType(Optional<KBObject> kbObject) {

        String classType = null;
        if (kbObject != null) {
            classType = kbObject.get().getClass().getSimpleName();
        }
        return classType;

    }
    

    public Optional<KBObject> readKBIdentifier(Project aProject, String aIdentifier) {
    
        for (KnowledgeBase kb : kbService.getKnowledgeBases(aProject)) {
            try (RepositoryConnection conn = kbService.getConnection(kb)) {
                ValueFactory vf = conn.getValueFactory();
                RepositoryResult<Statement> stmts = RdfUtils.getStatements(conn,
                        vf.createIRI(aIdentifier), kb.getTypeIri(), kb.getClassIri(), true);
    
                if (stmts.hasNext()) {
                    Statement conceptStmt = stmts.next();
                    KBConcept kbConcept = KBConcept.read(conn, conceptStmt);
                    return Optional.of(kbConcept);
                }
    
                else if (!stmts.hasNext()) {
    
                    Optional<KBInstance> kbInstance = kbService.readInstance(kb, aIdentifier);
    
                    if (kbInstance != null) {
                        return Optional.of(kbInstance.get());
                    }
    
                }
    
            } catch (QueryEvaluationException e) {
                log.error("Reading KB Entries failed.", e);
                return Optional.empty();
            } 
        }
        return Optional.empty();
    }

}
