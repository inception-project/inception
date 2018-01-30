package de.tudarmstadt.ukp.inception.kb;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.inception.kb.model.Entity;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;




@Component
public class KnowledgeBaseExtensionRegistryImpl
    implements KnowledgeBaseExtensionRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final List<KnowledgeBaseExtension> extensions;
        
    public KnowledgeBaseExtensionRegistryImpl(
                @Autowired(required = false) List<KnowledgeBaseExtension> aExtensions)
    {
        if (aExtensions != null) {
            OrderComparator.sort(aExtensions);
            
            for (KnowledgeBaseExtension ext : aExtensions) {
                log.info("Found KnowledgeBase extension: {}",
                        ClassUtils.getAbbreviatedName(ext.getClass(), 20));
            }
            
            extensions = Collections.unmodifiableList(aExtensions);
        }
        else {
            extensions = Collections.emptyList();
        }
    }
    
    @Override
    public List<KnowledgeBaseExtension> getExtensions()
    {
        return extensions;
    }
    
    @Override
    public KnowledgeBaseExtension getExtension(String aId)
    {
        if (aId == null) {
            return null;
        }
        else {
            return extensions.stream().filter(ext -> aId.equals(ext.getBeanName())).findFirst()
                    .orElse(null);
        }
    }
    
    @Override
    public List<Entity> fireDisambiguate(KnowledgeBase aKB, IRI conceptIri, 
            AnnotatorState aState) {
        for (KnowledgeBaseExtension ext: getExtensions()) {
            return ext.disambiguate(aKB, conceptIri, aState);
        }
        throw new IllegalStateException();
    }
    
}
