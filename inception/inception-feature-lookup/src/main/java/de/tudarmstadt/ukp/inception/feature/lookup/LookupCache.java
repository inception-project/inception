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
package de.tudarmstadt.ukp.inception.feature.lookup;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.feature.lookup.config.LookupServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.feature.lookup.config.LookupServiceProperties;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link LookupServiceAutoConfiguration#lookupCache}.
 * </p>
 */
public class LookupCache
{
    private static final Logger LOG = LoggerFactory.getLogger(LookupCache.class);

    private final LoadingCache<Key, LookupEntry> labelCache;
    private final LookupService lookupService;

    public LookupCache(LookupService aLookupService,
            LookupServiceProperties aLookupServiceProperties)
    {
        lookupService = aLookupService;
        labelCache = Caffeine.newBuilder() //
                .maximumSize(aLookupServiceProperties.getRenderCacheSize()) //
                .expireAfterWrite(aLookupServiceProperties.getRenderCacheExpireDelay()) //
                .refreshAfterWrite(aLookupServiceProperties.getRenderCacheRefreshDelay()) //
                .build(key -> loadLabelValue(key));
    }

    public LookupEntry get(AnnotationFeature aFeature, LookupFeatureTraits aTraits, String aId)
    {
        return labelCache.get(new Key(aFeature, aTraits, aId));
    }

    private LookupEntry loadLabelValue(Key aKey)
    {
        try {
            // Use the concept from a particular knowledge base
            Optional<LookupEntry> lookupItem = lookupService.lookup(aKey.getTraits(), aKey.getId());
            return lookupItem.orElseThrow(NoSuchElementException::new);
        }
        catch (NoSuchElementException e) {
            LOG.error("No label for feature value [{}]", aKey.getId());
            return new LookupErrorEntry("NO LABEL (" + aKey.getId() + ")", e);
        }
        catch (Exception e) {
            LOG.error("Unable to obtain label value for feature value [{}]", aKey.getId(), e);
            return new LookupErrorEntry("ERROR (" + aKey.getId() + ")", e);
        }
    }

    private class Key
    {
        private final AnnotationFeature feature;
        private final String id;
        private final LookupFeatureTraits traits;

        public Key(AnnotationFeature aFeature, LookupFeatureTraits aTraits, String aId)
        {
            feature = aFeature;
            id = aId;
            traits = aTraits;
        }

        public String getId()
        {
            return id;
        }

        @SuppressWarnings("unused")
        public AnnotationFeature getAnnotationFeature()
        {
            return feature;
        }

        public LookupFeatureTraits getTraits()
        {
            return traits;
        }

        @Override
        public boolean equals(final Object other)
        {
            if (!(other instanceof Key)) {
                return false;
            }
            Key castOther = (Key) other;
            return Objects.equals(feature, castOther.feature) && Objects.equals(id, castOther.id);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(feature, id);
        }
    }
}
