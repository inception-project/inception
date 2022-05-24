/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.graph.KBErrorHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.config.KnowledgeBaseServiceUIAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link KnowledgeBaseServiceUIAutoConfiguration#conceptLabelCache}.
 * </p>
 */
public class ConceptLabelCache
{
    private static final Logger LOG = LoggerFactory.getLogger(ConceptLabelCache.class);

    private final KnowledgeBaseService kbService;
    private final LoadingCache<Key, KBHandle> labelCache;

    public ConceptLabelCache(KnowledgeBaseService aKbService, KnowledgeBaseProperties aKBProperties)
    {
        kbService = aKbService;
        labelCache = Caffeine.newBuilder() //
                .maximumSize(aKBProperties.getRenderCacheSize()) //
                .expireAfterWrite(aKBProperties.getRenderCacheExpireDelay()) //
                .refreshAfterWrite(aKBProperties.getRenderCacheRefreshDelay()) //
                .build(key -> loadLabelValue(key));
    }

    public KBHandle get(AnnotationFeature aFeature, String aRepositoryId, String aLabel)
    {
        return labelCache.get(new Key(aFeature, aRepositoryId, aLabel));
    }

    private KBHandle loadLabelValue(Key aKey)
    {
        try {
            // Use the concept from a particular knowledge base
            Optional<KBHandle> kbHandle;
            if (aKey.getRepositoryId() != null) {
                kbHandle = kbService
                        .getKnowledgeBaseById(aKey.getAnnotationFeature().getProject(),
                                aKey.getRepositoryId()) //
                        .filter(KnowledgeBase::isEnabled) //
                        .flatMap(kb -> kbService.readHandle(kb, aKey.getLabel()));
            }

            // Use the concept from any knowledge base (leave KB unselected)
            else {
                kbHandle = kbService.readHandle(aKey.getAnnotationFeature().getProject(),
                        aKey.getLabel());

            }
            return kbHandle.orElseThrow(NoSuchElementException::new);
        }
        catch (NoSuchElementException e) {
            LOG.error("No label for feature value [{}]", aKey.getLabel());
            return new KBErrorHandle("NO LABEL (" + aKey.getLabel() + ")", e);
        }
        catch (Exception e) {
            LOG.error("Unable to obtain label value for feature value [{}]", aKey.getLabel(), e);
            return new KBErrorHandle("ERROR (" + aKey.getLabel() + ")", e);
        }
    }

    private class Key
    {
        private final AnnotationFeature feature;
        private final String repositoryId;
        private final String label;

        public Key(AnnotationFeature aFeature, String aRepositoryId, String aLabel)
        {
            feature = aFeature;
            repositoryId = aRepositoryId;
            label = aLabel;
        }

        public String getLabel()
        {
            return label;
        }

        public AnnotationFeature getAnnotationFeature()
        {
            return feature;
        }

        public String getRepositoryId()
        {
            return repositoryId;
        }

        @Override
        public boolean equals(final Object other)
        {
            if (!(other instanceof Key)) {
                return false;
            }
            Key castOther = (Key) other;
            return Objects.equals(feature, castOther.feature)
                    && Objects.equals(repositoryId, castOther.repositoryId)
                    && Objects.equals(label, castOther.label);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(feature, repositoryId, label);
        }
    }
}
