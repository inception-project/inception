package de.tudarmstadt.ukp.inception.recommendation.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationEngineFactoryRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.v2.RecommendationEngineFactory;

public class RecommendationEngineFactoryRegistryImpl
    implements RecommendationEngineFactoryRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<RecommendationEngineFactory> extensionsProxy;

    private Map<String, RecommendationEngineFactory> extensions;

    public RecommendationEngineFactoryRegistryImpl(
        @Lazy @Autowired(required = false) List<RecommendationEngineFactory> aExtensions)
    {
        extensionsProxy = aExtensions;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    /* package private */ void init()
    {
        Map<String, RecommendationEngineFactory> exts = new HashMap<>();

        if (extensionsProxy != null) {
            for (RecommendationEngineFactory ext : extensionsProxy) {
                log.info("Found recommendation engine: {}",
                    ClassUtils.getAbbreviatedName(ext.getClass(), 20));
                exts.put(ext.getId(), ext);
            }
        }

        extensions = Collections.unmodifiableMap(exts);
    }

    @Override
    public List<RecommendationEngineFactory> getFactories(AnnotationLayer aLayer, AnnotationFeature aFeature) {
        List<RecommendationEngineFactory> result = new ArrayList<>();
        for (RecommendationEngineFactory factory : extensions.values()) {
            if (factory.accepts(aLayer, aFeature)) {
                result.add(factory);
            }
        }

        result.sort(Comparator.comparing(RecommendationEngineFactory::getName));

        return result;
    }

    @Override
    public RecommendationEngineFactory getFactory(String aId) {
        return null;
    }
}
