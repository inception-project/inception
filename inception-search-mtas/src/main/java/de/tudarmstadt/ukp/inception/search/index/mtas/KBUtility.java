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
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.RdfUtils;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class KBUtility {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private KnowledgeBaseService kbService;

    
    public KBUtility() {
        super();
        // TODO Auto-generated constructor stub
        
        kbService = ApplicationContextProvider.getApplicationContext()
                .getBean(KnowledgeBaseService.class);
        
    }
    
    
    
    public Optional<KBObject> read(Project aProject, String aIdentifier)
    {
        for (KnowledgeBase kb : kbService.getKnowledgeBases(aProject)) {
            try {
                RepositoryConnection conn = kbService.getConnection(kb);
                ValueFactory vf = conn.getValueFactory();
                // Try to figure out the type of the instance - we ignore the inferred types here
                // and only make use of the explicitly asserted types
                RepositoryResult<Statement> conceptStmts = RdfUtils.getStatementsSparql(conn,
                        vf.createIRI(aIdentifier), kb.getTypeIri(), null, false);
                
                String conceptIdentifier = null;
                
                
            }
            catch (QueryEvaluationException e) {
                log.warn("Reading concept for instance failed.", e);
                return Optional.empty();
            }
            
            
        }
        return null;
        
    }
    
    

}
