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

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.CacheLoader;
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
                .build(new CacheLoader<Key, KBHandle>()
                {
                    @Override
                    public KBHandle load(Key aKey)
                    {
                        return loadLabelValue(aKey);
                    }

                    @Override
                    public Map<? extends Key, ? extends KBHandle> loadAll(Set<? extends Key> aKeys)
                    {
                        return bulkLoadLabelValues(aKeys);
                    }
                });
    }

    public KBHandle get(AnnotationFeature aFeature, String aRepositoryId, String aLabel)
    {
        return labelCache.get(new Key(aFeature, aRepositoryId, aLabel));
    }

    public Map<Key, KBHandle> getAll(Collection<Key> aKeys)
    {
        return labelCache.getAll(aKeys);
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
            return KBHandle.builder() //
                    .withIdentifier(aKey.getLabel()) //
                    .build();
        }
        catch (Exception e) {
            LOG.error("Unable to obtain label value for feature value [{}]", aKey.getLabel(), e);
            return new KBErrorHandle("ERROR (" + aKey.getLabel() + ")", e);
        }
    }

    /**
     * Bulk-load handles for the given keys via {@link KnowledgeBaseService#readHandles}, grouping
     * by (project, repositoryId) so each group produces one batched SPARQL request. Falls back to
     * single-key loading for any group that throws, preserving the per-key fault isolation of
     * {@link #loadLabelValue}.
     */
    private Map<Key, KBHandle> bulkLoadLabelValues(Set<? extends Key> aKeys)
    {
        var result = new LinkedHashMap<Key, KBHandle>();

        // Group by project ID (not Project reference) so distinct JPA instances of the same
        // project still batch together. A null repositoryId means "any KB in the project".
        var groups = new LinkedHashMap<SimpleEntry<Long, String>, java.util.List<Key>>();
        for (var key : aKeys) {
            var groupKey = new SimpleEntry<>(key.getAnnotationFeature().getProject().getId(),
                    key.getRepositoryId());
            groups.computeIfAbsent(groupKey, k -> new java.util.ArrayList<>()).add(key);
        }

        for (var group : groups.entrySet()) {
            var repositoryId = group.getKey().getValue();
            var keysInGroup = group.getValue();
            // Any key works — all share the same project ID.
            var project = keysInGroup.get(0).getAnnotationFeature().getProject();

            try {
                var distinctIds = keysInGroup.stream() //
                        .map(Key::getLabel) //
                        .collect(Collectors.toSet());

                Map<String, KBHandle> handlesById;
                if (repositoryId != null) {
                    var kbOpt = kbService.getKnowledgeBaseById(project, repositoryId) //
                            .filter(KnowledgeBase::isEnabled);
                    handlesById = kbOpt.map(kb -> kbService.readHandles(kb, distinctIds))
                            .orElse(Map.of());
                }
                else {
                    handlesById = kbService.readHandles(project, distinctIds);
                }

                for (var key : keysInGroup) {
                    // readHandles guarantees one entry per requested id (stub when not found),
                    // so we cache whatever it returned — mirrors loadLabelValue's behavior of
                    // keeping handles with name=null but other metadata (description/deprecated).
                    var handle = handlesById.get(key.getLabel());
                    if (handle != null) {
                        result.put(key, handle);
                    }
                    else {
                        result.put(key, KBHandle.builder() //
                                .withIdentifier(key.getLabel()) //
                                .build());
                    }
                }
            }
            catch (Exception e) {
                LOG.error(
                        "Bulk load failed for project [{}] repositoryId [{}]; "
                                + "falling back to per-key loading",
                        project.getName(), repositoryId, e);
                for (var key : keysInGroup) {
                    result.put(key, loadLabelValue(key));
                }
            }
        }

        return result;
    }

    public static class Key
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

        public static Key of(AnnotationFeature aFeature, String aRepositoryId, String aLabel)
        {
            return new Key(aFeature, aRepositoryId, aLabel);
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
