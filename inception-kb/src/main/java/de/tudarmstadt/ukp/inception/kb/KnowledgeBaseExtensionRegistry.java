package de.tudarmstadt.ukp.inception.kb;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.inception.kb.model.Entity;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public interface KnowledgeBaseExtensionRegistry
{
    List<KnowledgeBaseExtension> getExtensions();

    KnowledgeBaseExtension getExtension(String aId);

    List<Entity> fireDisambiguate(KnowledgeBase aKB, IRI aConceptIri, AnnotatorState aState);

}
