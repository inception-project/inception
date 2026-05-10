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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format;

import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicRelationLayerInitializer.BASIC_RELATION_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.project.initializers.basic.BasicSpanLayerInitializer.BASIC_SPAN_LAYER_NAME;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format.PubAnnotationToCasConverter.LABEL_FEATURE;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationAttribute;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDenotation;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocument;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationRelation;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationSpan;

/**
 * Converts a UIMA {@link CAS} into a {@link PubAnnotationDocument}. Inverse of
 * {@link PubAnnotationToCasConverter}.
 * <p>
 * Only annotations whose type FQN is in {@code spanTypes} or {@code relationTypes} are exported.
 * Relations are detected by membership in {@code relationTypes} and require {@code Governor} /
 * {@code Dependent} endpoint features.
 * <p>
 * Type → {@code obj}/{@code pred}: emit FQN for any non-basic type; for the basic span/relation
 * layers, emit the {@link PubAnnotationToCasConverter#LABEL_FEATURE label} feature value.
 * <p>
 * Each primitive feature on an exported annotation becomes one or more PubAnnotation attributes.
 * Array-typed features are expanded into multiple attribute records sharing the same {@code pred}.
 * Reserved features (the type's claimed {@code label}, {@code Governor}, {@code Dependent}) are
 * skipped.
 */
public class CasToPubAnnotationConverter
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Set<String> spanTypes;
    private final Set<String> relationTypes;
    private final boolean shortTypeNames;

    /** Per-convert state — simple name → count in the source CAS type system. */
    private Map<String, Integer> simpleNameCounts = Map.of();

    public CasToPubAnnotationConverter(Set<String> aSpanTypes, Set<String> aRelationTypes)
    {
        this(aSpanTypes, aRelationTypes, true);
    }

    public CasToPubAnnotationConverter(Set<String> aSpanTypes, Set<String> aRelationTypes,
            boolean aShortTypeNames)
    {
        spanTypes = aSpanTypes;
        relationTypes = aRelationTypes;
        shortTypeNames = aShortTypeNames;
    }

    public PubAnnotationDocument convert(CAS aCas, String aSourceDb, String aSourceId)
    {
        var doc = new PubAnnotationDocument();
        doc.setSourceDb(aSourceDb);
        doc.setSourceId(aSourceId);
        doc.setText(aCas.getDocumentText() != null ? aCas.getDocumentText() : "");

        var ts = aCas.getTypeSystem();
        var fsToId = new HashMap<AnnotationFS, String>();
        var denotations = new ArrayList<PubAnnotationDenotation>();
        var attributes = new ArrayList<PubAnnotationAttribute>();
        var counters = new IdCounters();

        // Map simpleName -> count, computed across the entire CAS type system. A short name is
        // safe to emit when its simple name is unique; otherwise the importer's suffix-match
        // would be ambiguous, so emit the FQN instead.
        var counts = new HashMap<String, Integer>();
        if (shortTypeNames) {
            for (var it = ts.getTypeIterator(); it.hasNext();) {
                var t = it.next();
                counts.merge(t.getShortName(), 1, Integer::sum);
            }
        }
        simpleNameCounts = counts;

        // Pass 1: spans (exported in document order so IDs are stable for the same input).
        for (var typeName : spanTypes) {
            var type = ts.getType(typeName);
            if (type == null) {
                continue;
            }
            for (var fs : aCas.<AnnotationFS> getAnnotationIndex(type)) {
                if (!fs.getType().getName().equals(typeName)) {
                    // Skip subtype FSes — those will be handled when their own typeName visits.
                    continue;
                }
                var id = counters.nextDenotationId();
                fsToId.put(fs, id);
                denotations.add(readDenotation(id, fs, typeName));
                readAttributesFor(fs, id, typeName, false, attributes, counters);
            }
        }

        // Pass 2: relations.
        var relations = new ArrayList<PubAnnotationRelation>();
        for (var typeName : relationTypes) {
            var type = ts.getType(typeName);
            if (type == null) {
                continue;
            }
            for (var fs : aCas.<AnnotationFS> getAnnotationIndex(type)) {
                if (!fs.getType().getName().equals(typeName)) {
                    continue;
                }
                var rel = readRelation(fs, typeName, fsToId, counters);
                if (rel == null) {
                    continue;
                }
                fsToId.put(fs, rel.getId());
                relations.add(rel);
                readAttributesFor(fs, rel.getId(), typeName, true, attributes, counters);
            }
        }

        doc.setDenotations(denotations);
        doc.setRelations(relations);
        doc.setAttributes(attributes);
        return doc;
    }

    private PubAnnotationDenotation readDenotation(String aId, AnnotationFS aFs, String aTypeName)
    {
        var d = new PubAnnotationDenotation();
        d.setId(aId);
        var span = new PubAnnotationSpan();
        span.setBegin(aFs.getBegin());
        span.setEnd(aFs.getEnd());
        d.setSpans(List.of(span));
        d.setObject(readObj(aFs, aTypeName, BASIC_SPAN_LAYER_NAME));
        return d;
    }

    private PubAnnotationRelation readRelation(AnnotationFS aFs, String aTypeName,
            Map<AnnotationFS, String> aFsToId, IdCounters aCounters)
    {
        var src = readFsFeature(aFs, FEAT_REL_SOURCE);
        var tgt = readFsFeature(aFs, FEAT_REL_TARGET);
        if (src == null || tgt == null) {
            LOG.warn("Skipping relation of type {}: missing Governor/Dependent endpoint",
                    aTypeName);
            return null;
        }
        var srcId = aFsToId.get(src);
        var tgtId = aFsToId.get(tgt);
        if (srcId == null || tgtId == null) {
            LOG.warn("Skipping relation of type {}: endpoint not in exported span set", aTypeName);
            return null;
        }
        var r = new PubAnnotationRelation();
        r.setId(aCounters.nextRelationId());
        r.setSubject(srcId);
        r.setObject(tgtId);
        r.setPredicate(readObj(aFs, aTypeName, BASIC_RELATION_LAYER_NAME));
        return r;
    }

    private String readObj(AnnotationFS aFs, String aTypeName, String aBasicLayerName)
    {
        if (aTypeName.equals(aBasicLayerName)) {
            var labelFeature = aFs.getType().getFeatureByBaseName(LABEL_FEATURE);
            if (labelFeature != null) {
                var val = aFs.getStringValue(labelFeature);
                if (val != null) {
                    return val;
                }
            }
        }
        if (shortTypeNames) {
            var simple = aFs.getType().getShortName();
            if (simpleNameCounts.getOrDefault(simple, 0) == 1) {
                return simple;
            }
        }
        return aTypeName;
    }

    private void readAttributesFor(AnnotationFS aFs, String aSubjectId, String aTypeName,
            boolean aIsRelation, List<PubAnnotationAttribute> aSink, IdCounters aCounters)
    {
        var type = aFs.getType();
        boolean basic = aIsRelation ? aTypeName.equals(BASIC_RELATION_LAYER_NAME)
                : aTypeName.equals(BASIC_SPAN_LAYER_NAME);

        for (var feat : type.getFeatures()) {
            var name = feat.getShortName();
            // Skip system features ("sofa", "begin", "end").
            if ("sofa".equals(name) || "begin".equals(name) || "end".equals(name)) {
                continue;
            }
            // Skip relation endpoints (consumed as subj/obj).
            if (aIsRelation && (FEAT_REL_SOURCE.equals(name) || FEAT_REL_TARGET.equals(name))) {
                continue;
            }
            // Skip the label slot when it was claimed by the basic-fallback obj/pred.
            if (basic && LABEL_FEATURE.equals(name)) {
                continue;
            }
            readFeature(aFs, feat, aSubjectId, aSink, aCounters);
        }
    }

    private void readFeature(AnnotationFS aFs, Feature aFeature, String aSubjectId,
            List<PubAnnotationAttribute> aSink, IdCounters aCounters)
    {
        var range = aFeature.getRange().getName();
        var name = aFeature.getShortName();
        switch (range) {
        case CAS.TYPE_NAME_STRING: {
            var v = FSUtil.getFeature(aFs, name, String.class);
            if (v != null) {
                aSink.add(attr(aCounters, aSubjectId, name, v));
            }
            break;
        }
        case CAS.TYPE_NAME_BOOLEAN:
            aSink.add(
                    attr(aCounters, aSubjectId, name, FSUtil.getFeature(aFs, name, Boolean.class)));
            break;
        case CAS.TYPE_NAME_INTEGER:
            aSink.add(
                    attr(aCounters, aSubjectId, name, FSUtil.getFeature(aFs, name, Integer.class)));
            break;
        case CAS.TYPE_NAME_FLOAT:
            aSink.add(attr(aCounters, aSubjectId, name, FSUtil.getFeature(aFs, name, Float.class)));
            break;
        case CAS.TYPE_NAME_STRING_ARRAY:
            readArray(aSink, aCounters, aSubjectId, name,
                    FSUtil.getFeature(aFs, name, String[].class));
            break;
        case CAS.TYPE_NAME_BOOLEAN_ARRAY:
            readArray(aSink, aCounters, aSubjectId, name,
                    FSUtil.getFeature(aFs, name, Boolean[].class));
            break;
        case CAS.TYPE_NAME_INTEGER_ARRAY:
            readArray(aSink, aCounters, aSubjectId, name,
                    FSUtil.getFeature(aFs, name, Integer[].class));
            break;
        case CAS.TYPE_NAME_FLOAT_ARRAY:
            readArray(aSink, aCounters, aSubjectId, name,
                    FSUtil.getFeature(aFs, name, Float[].class));
            break;
        default:
            // Skip FS-link features and other unsupported ranges silently.
            break;
        }
    }

    private static void readArray(List<PubAnnotationAttribute> aSink, IdCounters aCounters,
            String aSubjectId, String aName, Object[] aValues)
    {
        if (aValues == null) {
            return;
        }
        for (var v : aValues) {
            aSink.add(attr(aCounters, aSubjectId, aName, v));
        }
    }

    private static AnnotationFS readFsFeature(AnnotationFS aFs, String aFeatureName)
    {
        var f = aFs.getType().getFeatureByBaseName(aFeatureName);
        if (f == null) {
            return null;
        }
        var v = aFs.getFeatureValue(f);
        return v instanceof AnnotationFS afs ? afs : null;
    }

    private static PubAnnotationAttribute attr(IdCounters aCounters, String aSubjectId,
            String aPred, Object aValue)
    {
        var a = new PubAnnotationAttribute();
        a.setId(aCounters.nextAttributeId());
        a.setSubject(aSubjectId);
        a.setPredicate(aPred);
        a.setObject(aValue);
        return a;
    }

    private static class IdCounters
    {
        int t = 0;
        int r = 0;
        int a = 0;

        String nextDenotationId()
        {
            return "T" + (++t);
        }

        String nextRelationId()
        {
            return "R" + (++r);
        }

        String nextAttributeId()
        {
            return "A" + (++a);
        }
    }

}
