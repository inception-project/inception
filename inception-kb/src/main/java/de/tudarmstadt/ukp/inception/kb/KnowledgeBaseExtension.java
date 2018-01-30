package de.tudarmstadt.ukp.inception.kb;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;

import de.tudarmstadt.ukp.inception.kb.model.Entity;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public interface KnowledgeBaseExtension
{
    /**
     * @return get the bean name.
     */
    String getBeanName();
    
    List<Entity> disambiguate(KnowledgeBase aKB, IRI aConceptIri, AnnotatorState aState);

}
