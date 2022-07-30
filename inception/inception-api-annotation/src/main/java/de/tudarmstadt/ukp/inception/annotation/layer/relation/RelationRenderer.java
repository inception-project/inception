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
package de.tudarmstadt.ukp.inception.annotation.layer.relation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static org.apache.uima.fit.util.CasUtil.selectCovered;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.wicket.Page;
import org.apache.wicket.core.request.handler.IPageRequestHandler;
import org.apache.wicket.request.cycle.PageRequestHandlerTracker;
import org.apache.wicket.request.cycle.RequestCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.Renderer_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VComment;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VCommentType;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailQuery;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailResult;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VObject;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupportRegistry;

/**
 * A class that is used to create Brat Arc to CAS relations and vice-versa
 */
public class RelationRenderer
    extends Renderer_ImplBase<RelationAdapter>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<RelationLayerBehavior> behaviors;
    private final RelationLayerTraits traits;

    private Type type;
    private Type spanType;
    private Feature dependentFeature;
    private Feature governorFeature;
    private Feature arcSpanFeature;

    public RelationRenderer(RelationAdapter aTypeAdapter,
            LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry, List<RelationLayerBehavior> aBehaviors)
    {
        super(aTypeAdapter, aLayerSupportRegistry, aFeatureSupportRegistry);

        if (aBehaviors == null) {
            behaviors = emptyList();
        }
        else {
            List<RelationLayerBehavior> temp = new ArrayList<>(aBehaviors);
            AnnotationAwareOrderComparator.sort(temp);
            behaviors = temp;
        }

        RelationAdapter typeAdapter = getTypeAdapter();
        traits = typeAdapter.getTraits(RelationLayerTraits.class)
                .orElseGet(RelationLayerTraits::new);
    }

    @Override
    protected boolean typeSystemInit(TypeSystem aTypeSystem)
    {
        RelationAdapter typeAdapter = getTypeAdapter();
        type = aTypeSystem.getType(typeAdapter.getAnnotationTypeName());
        spanType = aTypeSystem.getType(typeAdapter.getAttachTypeName());

        if (type == null || spanType == null) {
            // If the types are not defined, then we do not need to try and render them because the
            // CAS does not contain any instances of them
            return false;
        }

        dependentFeature = type.getFeatureByBaseName(typeAdapter.getTargetFeatureName());
        governorFeature = type.getFeatureByBaseName(typeAdapter.getSourceFeatureName());
        arcSpanFeature = spanType.getFeatureByBaseName(typeAdapter.getAttachFeatureName());

        return true;
    }

    @Override
    public List<AnnotationFS> selectAnnotationsInWindow(CAS aCas, int aWindowBegin, int aWindowEnd)
    {
        return selectCovered(aCas, type, aWindowBegin, aWindowEnd);
    }

    @Override
    public void render(final CAS aCas, List<AnnotationFeature> aFeatures, VDocument aResponse,
            int aWindowBegin, int aWindowEnd)
    {
        if (!checkTypeSystem(aCas)) {
            return;
        }

        RelationAdapter typeAdapter = getTypeAdapter();

        // Map<Integer, Set<Integer>> relationLinks = getRelationLinks(aCas, aWindowBegin,
        // aWindowEnd);
        //
        // // if this is a governor for more than one dependent, avoid duplicate yield
        // List<Integer> yieldDeps = new ArrayList<>();

        // Index mapping annotations to the corresponding rendered arcs
        Map<AnnotationFS, VArc> annoToArcIdx = new HashMap<>();

        List<AnnotationFS> annotations = selectAnnotationsInWindow(aCas, aWindowBegin, aWindowEnd);

        for (AnnotationFS fs : annotations) {
            for (VObject arc : render(aResponse, fs, aFeatures, aWindowBegin, aWindowEnd)) {
                if (!(arc instanceof VArc)) {
                    continue;
                }

                aResponse.add(arc);
                annoToArcIdx.put(fs, (VArc) arc);

                // renderYield(aResponse, fs, relationLinks, yieldDeps);

                renderLazyDetails(fs, arc, aFeatures);
                renderRequiredFeatureErrors(aFeatures, fs, aResponse);
            }
        }

        for (RelationLayerBehavior behavior : behaviors) {
            behavior.onRender(typeAdapter, aResponse, annoToArcIdx);
        }
    }

    private Optional<String> renderYield(AnnotationFS fs, Map<Integer, Set<Integer>> relationLinks,
            List<Integer> yieldDeps)
    {
        FeatureStructure dependentFs = getDependentFs(fs);

        if (relationLinks.keySet().contains(ICasUtil.getAddr(dependentFs))
                && !yieldDeps.contains(ICasUtil.getAddr(dependentFs))) {
            yieldDeps.add(ICasUtil.getAddr(dependentFs));

            // sort the annotations (begin, end)
            List<Integer> sortedDepFs = new ArrayList<>(
                    relationLinks.get(ICasUtil.getAddr(dependentFs)));
            sortedDepFs.sort(comparingInt(
                    arg0 -> ICasUtil.selectAnnotationByAddr(fs.getCAS(), arg0).getBegin()));

            String cm = getYieldMessage(fs.getCAS(), sortedDepFs);

            return Optional.of(cm);
        }

        return Optional.empty();
    }

    @Override
    public List<VObject> render(VDocument aVDocument, AnnotationFS aFS,
            List<AnnotationFeature> aFeatures, int aWindowBegin, int aWindowEnd)
    {
        if (!checkTypeSystem(aFS.getCAS())) {
            return Collections.emptyList();
        }

        RelationAdapter typeAdapter = getTypeAdapter();
        FeatureStructure dependentFs = getDependentFs(aFS);
        FeatureStructure governorFs = getGovernorFs(aFS);

        if (dependentFs == null || governorFs == null) {
            StringBuilder message = new StringBuilder();

            message.append("Relation [" + typeAdapter.getLayer().getName() + "] with id ["
                    + ICasUtil.getAddr(aFS) + "] has loose ends - cannot render.");
            if (typeAdapter.getAttachFeatureName() != null) {
                message.append("\nRelation [" + typeAdapter.getLayer().getName()
                        + "] attached to feature [" + typeAdapter.getAttachFeatureName() + "].");
            }
            message.append("\nDependent: " + dependentFs);
            message.append("\nGovernor: " + governorFs);

            RequestCycle requestCycle = RequestCycle.get();
            IPageRequestHandler handler = PageRequestHandlerTracker.getLastHandler(requestCycle);
            Page page = (Page) handler.getPage();
            page.warn(message.toString());

            return Collections.emptyList();
        }

        String bratTypeName = typeAdapter.getEncodedTypeName();
        Map<String, String> labelFeatures = renderLabelFeatureValues(typeAdapter, aFS, aFeatures);

        if (traits.isRenderArcs()) {
            return asList(new VArc(typeAdapter.getLayer(), aFS, bratTypeName, governorFs,
                    dependentFs, labelFeatures));
        }
        else {
            AnnotationFS governor = (AnnotationFS) governorFs;
            AnnotationFS dependent = (AnnotationFS) dependentFs;

            StringBuilder noteBuilder = new StringBuilder();
            noteBuilder.append(typeAdapter.getLayer().getUiName());
            noteBuilder.append("\n");
            noteBuilder.append(governor.getCoveredText());
            noteBuilder.append(" -> ");
            noteBuilder.append(dependent.getCoveredText());
            noteBuilder.append("\n");

            for (Entry<String, String> entry : labelFeatures.entrySet()) {
                noteBuilder.append(entry.getKey());
                noteBuilder.append(" = ");
                noteBuilder.append(entry.getValue());
                noteBuilder.append("\n");
            }

            String note = noteBuilder.toString().stripTrailing();
            aVDocument.add(new VComment(governorFs, VCommentType.INFO, "\n⬆️ " + note));
            aVDocument.add(new VComment(dependentFs, VCommentType.INFO, "\n⬇️ " + note));

            return Collections.emptyList();
        }
    }

    @Override
    public List<VLazyDetailQuery> getLazyDetails(AnnotationFS aFs,
            List<AnnotationFeature> aFeatures)
    {
        List<VLazyDetailQuery> queries = super.getLazyDetails(aFs, aFeatures);

        if (!queries.contains(VLazyDetailQuery.LAYER_LEVEL_QUERY)) {
            queries.add(VLazyDetailQuery.LAYER_LEVEL_QUERY);
        }

        return queries;
    }

    @Override
    public List<VLazyDetailResult> renderLazyDetails(CAS aCas, VID aVid, int windowBeginOffset,
            int windowEndOffset)
    {
        checkTypeSystem(aCas);

        // FIXME Should also handle relations that are only partially visible using
        // selectAnnotationsInWindow()
        Map<Integer, Set<Integer>> relationLinks = getRelationLinks(aCas, windowBeginOffset,
                windowEndOffset);

        // if this is a governor for more than one dependent, avoid duplicate yield
        List<Integer> yieldDeps = new ArrayList<>();

        AnnotationFS fs = ICasUtil.selectByAddr(aCas, AnnotationFS.class, aVid.getId());

        Optional<String> yield = renderYield(fs, relationLinks, yieldDeps);

        List<VLazyDetailResult> details = super.renderLazyDetails(aCas, aVid, windowBeginOffset,
                windowEndOffset);

        if (yield.isPresent()) {
            details.add(new VLazyDetailResult("Yield", yield.get()));
        }

        return details;
    }

    /**
     * The relations yield message
     */
    private String getYieldMessage(CAS aCas, List<Integer> sortedDepFs)
    {
        StringBuilder cm = new StringBuilder();
        int end = -1;
        for (Integer depFs : sortedDepFs) {
            if (end == -1) {
                cm.append(ICasUtil.selectAnnotationByAddr(aCas, depFs).getCoveredText());
                end = ICasUtil.selectAnnotationByAddr(aCas, depFs).getEnd();
            }
            // if no space between token and punct
            else if (end == ICasUtil.selectAnnotationByAddr(aCas, depFs).getBegin()) {
                cm.append(ICasUtil.selectAnnotationByAddr(aCas, depFs).getCoveredText());
                end = ICasUtil.selectAnnotationByAddr(aCas, depFs).getEnd();
            }
            else if (end + 1 != ICasUtil.selectAnnotationByAddr(aCas, depFs).getBegin()) {
                cm.append(" ... ")
                        .append(ICasUtil.selectAnnotationByAddr(aCas, depFs).getCoveredText());
                end = ICasUtil.selectAnnotationByAddr(aCas, depFs).getEnd();
            }
            else {
                cm.append(" ")
                        .append(ICasUtil.selectAnnotationByAddr(aCas, depFs).getCoveredText());
                end = ICasUtil.selectAnnotationByAddr(aCas, depFs).getEnd();
            }

        }
        return cm.toString();
    }

    /**
     * Get relation links to display in relation yield
     */
    private Map<Integer, Set<Integer>> getRelationLinks(CAS aCas, int aWindowBegin, int aWindowEnd)
    {
        RelationAdapter typeAdapter = getTypeAdapter();
        Map<Integer, Set<Integer>> relations = new ConcurrentHashMap<>();

        for (AnnotationFS fs : selectCovered(aCas, type, aWindowBegin, aWindowEnd)) {
            FeatureStructure dependentFs = getGovernorFs(fs);
            FeatureStructure governorFs = getDependentFs(fs);

            if (dependentFs == null || governorFs == null) {
                log.warn("Relation [" + typeAdapter.getLayer().getName() + "] with id ["
                        + ICasUtil.getAddr(fs) + "] has loose ends - cannot render.");
                continue;
            }

            Set<Integer> links = relations.get(ICasUtil.getAddr(governorFs));
            if (links == null) {
                links = new ConcurrentSkipListSet<>();
            }

            links.add(ICasUtil.getAddr(dependentFs));
            relations.put(ICasUtil.getAddr(governorFs), links);
        }

        // Update other subsequent links
        for (int i = 0; i < relations.keySet().size(); i++) {
            for (Integer fs : relations.keySet()) {
                updateLinks(relations, fs);
            }
        }
        // to start displaying the text from the governor, include it
        for (Integer fs : relations.keySet()) {
            relations.get(fs).add(fs);
        }
        return relations;
    }

    private void updateLinks(Map<Integer, Set<Integer>> aRelLinks, Integer aGov)
    {
        for (Integer dep : aRelLinks.get(aGov)) {
            if (aRelLinks.containsKey(dep)
                    && !aRelLinks.get(aGov).containsAll(aRelLinks.get(dep))) {
                aRelLinks.get(aGov).addAll(aRelLinks.get(dep));
                updateLinks(aRelLinks, dep);
            }
        }
    }

    private FeatureStructure getGovernorFs(FeatureStructure fs)
    {
        RelationAdapter typeAdapter = getTypeAdapter();
        FeatureStructure governorFs;
        if (typeAdapter.getAttachFeatureName() != null) {
            governorFs = fs.getFeatureValue(governorFeature).getFeatureValue(arcSpanFeature);
        }
        else {
            governorFs = fs.getFeatureValue(governorFeature);
        }
        return governorFs;
    }

    private FeatureStructure getDependentFs(FeatureStructure fs)
    {
        RelationAdapter typeAdapter = getTypeAdapter();
        FeatureStructure dependentFs;
        if (typeAdapter.getAttachFeatureName() != null) {
            dependentFs = fs.getFeatureValue(dependentFeature).getFeatureValue(arcSpanFeature);
        }
        else {
            dependentFs = fs.getFeatureValue(dependentFeature);
        }
        return dependentFs;
    }
}
