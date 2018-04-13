package de.tudarmstadt.ukp.inception.kb.reification;

import java.util.List;

import de.tudarmstadt.ukp.inception.kb.graph.KBQualifier;
import org.eclipse.rdf4j.model.Statement;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public interface ReificationStrategy {
    List<Statement> reify(KnowledgeBase kb, KBStatement aStatement);
    List<KBStatement> listStatements(KnowledgeBase kb, KBHandle aInstance, boolean aAll);
    void deleteStatement(KnowledgeBase kb, KBStatement aStatement);
    void upsertStatement(KnowledgeBase kb, KBStatement aStatement);
    void addQualifier(KnowledgeBase kb, KBQualifier newQualifier);
    void deleteQualifier(KnowledgeBase kb, KBQualifier oldQualifier);
    List<KBQualifier> listQualifiers(KnowledgeBase kb, KBStatement aStatement);
}