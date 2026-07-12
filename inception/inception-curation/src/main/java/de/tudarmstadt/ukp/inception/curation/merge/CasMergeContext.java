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
package de.tudarmstadt.ukp.inception.curation.merge;

import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureTraits;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class CasMergeContext
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AnnotationSchemaService schemaService;

    private final Map<String, AnnotationLayer> layerCache;
    private final LoadingCache<AnnotationLayer, TypeAdapter> adapterCache;
    private final LoadingCache<AnnotationFeature, LinkFeatureTraits> linkTraitsCache;
    private final Map<AnnotationLayer, List<AnnotationFeature>> featureCache;

    private boolean silenceEvents = false;
    private boolean preserveExisting = false;

    // Records, per slot (a link-list feature on a particular host annotation), whether that slot
    // was
    // already filled in the target when the merge started. It is populated lazily the first time
    // each
    // slot is encountered during merging - before any links are added to it - so that links added
    // later during the same merge run are not mistaken for pre-existing curator decisions. The map
    // is
    // never cleared; this is safe because every merge run builds a fresh CasMerge (and thus a fresh
    // CasMergeContext), and SlotId host addresses are only unique within a single target CAS.
    private final Map<SlotId, Boolean> slotFilledBeforeMerge = new HashMap<>();

    public CasMergeContext(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;

        layerCache = new HashMap<>();
        featureCache = new HashMap<>();
        adapterCache = Caffeine.newBuilder() //
                .maximumSize(100) //
                .build(schemaService::getAdapter);
        linkTraitsCache = Caffeine.newBuilder() //
                .maximumSize(100) //
                .build(this::_readTraits);
    }

    public void setSilenceEvents(boolean aSilenceEvents)
    {
        silenceEvents = aSilenceEvents;
    }

    public boolean isSilenceEvents()
    {
        return silenceEvents;
    }

    /**
     * If enabled, the merge does not overwrite annotations that are already present in the target
     * CAS. Empty positions are still filled and - where the layer permits it - additional
     * annotations are still stacked, but a position already occupied by an existing annotation
     * (e.g. a decision made by a curator) is left untouched instead of being updated to the merged
     * value. This is used when merging into a target document that has not been cleared beforehand.
     *
     * @param aPreserveExisting
     *            whether to preserve annotations already present in the target.
     */
    public void setPreserveExisting(boolean aPreserveExisting)
    {
        preserveExisting = aPreserveExisting;
    }

    public boolean isPreserveExisting()
    {
        return preserveExisting;
    }

    /**
     * Determines whether the given slot (a link-list feature on a particular host annotation) was
     * already filled in the target document when the merge started - i.e. whether it represents a
     * pre-existing decision (e.g. by a curator) that {@link #isPreserveExisting()
     * preserve-existing} mode must not modify.
     * <p>
     * The state is captured lazily the first time each slot is seen and cached for the remainder of
     * the merge run. Because slots are only ever seen here before any link is added to them, links
     * that the merge itself adds to a previously empty slot are not mistaken for pre-existing ones.
     *
     * @param aHost
     *            the host annotation carrying the slot.
     * @param aSlotFeature
     *            the link-list feature.
     * @param aCurrentlyFilled
     *            whether the slot currently holds any links.
     * @return whether the slot was already filled before the merge started.
     */
    public boolean wasSlotFilledBeforeMerge(AnnotationFS aHost, AnnotationFeature aSlotFeature,
            boolean aCurrentlyFilled)
    {
        var slotId = new SlotId(getAddr(aHost), aSlotFeature.getName());
        return slotFilledBeforeMerge.computeIfAbsent(slotId, key -> aCurrentlyFilled);
    }

    /**
     * Identifies a slot - a link-list feature on a particular host annotation - within a single
     * merge run (i.e. within a single target CAS, where the host address is unique).
     */
    private record SlotId(int hostAddress, String featureName) {}

    public List<AnnotationFeature> listSupportedFeatures(AnnotationLayer aLayer)
    {
        return featureCache.computeIfAbsent(aLayer,
                key -> schemaService.listSupportedFeatures(key));
    }

    public AnnotationLayer findLayer(Project aProject, String aTypeName)
    {
        return layerCache.computeIfAbsent(aTypeName,
                typeName -> schemaService.findLayer(aProject, typeName));
    }

    public TypeAdapter getAdapter(AnnotationLayer aLayer)
    {
        return adapterCache.get(aLayer);
    }

    public LinkFeatureTraits readLinkTraits(AnnotationFeature aFeature)
    {
        return linkTraitsCache.get(aFeature);
    }

    // Would be better to use this from the LinkFeatureSupport - but I do not want to change the
    // constructor at the moment to inject another dependency.
    private LinkFeatureTraits _readTraits(AnnotationFeature aFeature)
    {
        LinkFeatureTraits traits = null;
        try {
            traits = JSONUtil.fromJsonString(LinkFeatureTraits.class, aFeature.getTraits());
        }
        catch (IOException e) {
            LOG.error("Unable to read traits", e);
        }

        if (traits == null) {
            traits = new LinkFeatureTraits();
        }

        return traits;
    }
}
