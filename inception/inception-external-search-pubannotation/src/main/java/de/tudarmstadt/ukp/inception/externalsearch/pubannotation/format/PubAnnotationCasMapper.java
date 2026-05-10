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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationAttribute;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDenotation;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationDocument;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationRelation;
import de.tudarmstadt.ukp.inception.externalsearch.pubannotation.model.PubAnnotationSpan;

/**
 * Maps a deserialized {@link PubAnnotationDocument} into a UIMA {@link CAS}.
 * <p>
 * Type resolution for a denotation's <code>obj</code> (and a relation's <code>pred</code>):
 * <ol>
 * <li>If the value looks like a fully-qualified type name and the type exists in the CAS, use it.
 * <li>Else, if the value is a plain Java identifier, look for a type with that simple name. Skip
 * the annotation if more than one type matches (ambiguous).
 * <li>Else, fall back to {@link #BASIC_SPAN_LAYER} ({@link #BASIC_RELATION_LAYER} for relations).
 * The original value is stored in the {@link #LABEL_FEATURE label} feature.
 * </ol>
 * Attribute mapping for a {@code pred} on a target annotation:
 * <ol>
 * <li>If the target type has a feature with that exact name, set it.
 * <li>Else, if the target has only one attribute, the type has a {@link #LABEL_FEATURE label}
 * feature, and that slot has not already been claimed by a basic-fallback {@code obj} /
 * {@code pred}, set the {@code label} feature.
 * <li>Else, skip.
 * </ol>
 * Discontinuous spans (bagging model with multiple {@code begin}/{@code end} pairs) are skipped.
 */
public class PubAnnotationCasMapper
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String BASIC_SPAN_LAYER = "custom.Span";
    public static final String BASIC_RELATION_LAYER = "custom.Relation";
    public static final String LABEL_FEATURE = "label";

    public static final String FEAT_REL_SOURCE = "Governor";
    public static final String FEAT_REL_TARGET = "Dependent";

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern FQN = Pattern
            .compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+");

    private final CAS cas;
    private final TypeSystem typeSystem;

    public PubAnnotationCasMapper(CAS aCas)
    {
        cas = aCas;
        typeSystem = aCas.getTypeSystem();
    }

    public void apply(PubAnnotationDocument aDocument)
    {
        cas.setDocumentText(aDocument.getText() != null ? aDocument.getText() : "");

        if (aDocument.getTracks() != null && !aDocument.getTracks().isEmpty()) {
            for (var track : aDocument.getTracks()) {
                applyAnnotations(track.getDenotations(), track.getRelations(),
                        track.getAttributes());
            }
        }
        else {
            applyAnnotations(aDocument.getDenotations(), aDocument.getRelations(),
                    aDocument.getAttributes());
        }
    }

    private void applyAnnotations(List<PubAnnotationDenotation> aDenotations,
            List<PubAnnotationRelation> aRelations, List<PubAnnotationAttribute> aAttributes)
    {
        List<PubAnnotationDenotation> denotations = aDenotations != null ? aDenotations
                : emptyList();
        List<PubAnnotationRelation> relations = aRelations != null ? aRelations : emptyList();
        List<PubAnnotationAttribute> attributes = aAttributes != null ? aAttributes : emptyList();

        Map<String, Long> attributeCount = attributes.stream().filter(a -> a.getSubject() != null)
                .collect(groupingBy(PubAnnotationAttribute::getSubject, counting()));

        var byId = new HashMap<String, AnnotationFS>();
        var labelClaimedByObj = new HashSet<String>();

        for (var d : denotations) {
            createDenotation(d, labelClaimedByObj).ifPresent(fs -> byId.put(d.getId(), fs));
        }

        for (var r : relations) {
            createRelation(r, byId, labelClaimedByObj).ifPresent(fs -> byId.put(r.getId(), fs));
        }

        for (var a : attributes) {
            applyAttribute(a, byId, attributeCount.getOrDefault(a.getSubject(), 0L),
                    labelClaimedByObj);
        }
    }

    private Optional<AnnotationFS> createDenotation(PubAnnotationDenotation aDenotation,
            Set<String> aLabelClaimedByObj)
    {
        var spans = aDenotation.getSpans();
        if (spans == null || spans.size() != 1) {
            if (spans != null && spans.size() > 1) {
                LOG.warn("Skipping denotation {} with discontinuous span (not supported)",
                        aDenotation.getId());
            }
            return Optional.empty();
        }

        PubAnnotationSpan span = spans.get(0);
        var resolved = resolveType(aDenotation.getObject(), BASIC_SPAN_LAYER);
        if (resolved == null) {
            LOG.warn("Skipping denotation {}: cannot resolve type for '{}'", aDenotation.getId(),
                    aDenotation.getObject());
            return Optional.empty();
        }

        var fs = cas.createAnnotation(resolved.type, span.getBegin(), span.getEnd());
        if (resolved.isBasicFallback
                && setStringFeature(fs, LABEL_FEATURE, aDenotation.getObject())) {
            aLabelClaimedByObj.add(aDenotation.getId());
        }
        cas.addFsToIndexes(fs);
        return Optional.of(fs);
    }

    private Optional<AnnotationFS> createRelation(PubAnnotationRelation aRelation,
            Map<String, AnnotationFS> aById, Set<String> aLabelClaimedByPred)
    {
        var src = aById.get(aRelation.getSubject());
        var tgt = aById.get(aRelation.getObject());
        if (src == null || tgt == null) {
            LOG.warn("Skipping relation {}: missing endpoint(s) (subj={}, obj={})",
                    aRelation.getId(), aRelation.getSubject(), aRelation.getObject());
            return Optional.empty();
        }

        var resolved = resolveType(aRelation.getPredicate(), BASIC_RELATION_LAYER);
        if (resolved == null) {
            LOG.warn("Skipping relation {}: cannot resolve type for '{}'", aRelation.getId(),
                    aRelation.getPredicate());
            return Optional.empty();
        }

        var fs = cas.createAnnotation(resolved.type, tgt.getBegin(), tgt.getEnd());
        var sourceFeat = resolved.type.getFeatureByBaseName(FEAT_REL_SOURCE);
        var targetFeat = resolved.type.getFeatureByBaseName(FEAT_REL_TARGET);
        if (sourceFeat != null) {
            fs.setFeatureValue(sourceFeat, src);
        }
        if (targetFeat != null) {
            fs.setFeatureValue(targetFeat, tgt);
        }
        if (resolved.isBasicFallback
                && setStringFeature(fs, LABEL_FEATURE, aRelation.getPredicate())) {
            aLabelClaimedByPred.add(aRelation.getId());
        }
        cas.addFsToIndexes(fs);
        return Optional.of(fs);
    }

    private void applyAttribute(PubAnnotationAttribute aAttribute, Map<String, AnnotationFS> aById,
            long aTotalAttributesOnSubject, Set<String> aLabelClaimed)
    {
        var target = aById.get(aAttribute.getSubject());
        if (target == null || aAttribute.getPredicate() == null) {
            return;
        }
        var type = target.getType();
        var feature = type.getFeatureByBaseName(aAttribute.getPredicate());
        if (feature == null && aTotalAttributesOnSubject == 1
                && !aLabelClaimed.contains(aAttribute.getSubject())) {
            feature = type.getFeatureByBaseName(LABEL_FEATURE);
        }
        if (feature == null) {
            return;
        }
        setFeature(target, feature, aAttribute.getObject());
    }

    /**
     * Sets a string feature on the FS if it exists and is string-typed. Returns whether the feature
     * was actually written.
     */
    private static boolean setStringFeature(AnnotationFS aFs, String aFeatureName, String aValue)
    {
        var f = aFs.getType().getFeatureByBaseName(aFeatureName);
        if (f != null && CAS.TYPE_NAME_STRING.equals(f.getRange().getName())) {
            aFs.setStringValue(f, aValue);
            return true;
        }
        return false;
    }

    private static void setFeature(AnnotationFS aFs, Feature aFeature, Object aValue)
    {
        if (aValue == null) {
            return;
        }
        var range = aFeature.getRange().getName();
        switch (range) {
        case CAS.TYPE_NAME_STRING:
            aFs.setStringValue(aFeature, aValue.toString());
            break;
        case CAS.TYPE_NAME_BOOLEAN:
            if (aValue instanceof Boolean b) {
                aFs.setBooleanValue(aFeature, b);
            }
            break;
        case CAS.TYPE_NAME_INTEGER:
            if (aValue instanceof Number n) {
                aFs.setIntValue(aFeature, n.intValue());
            }
            break;
        case CAS.TYPE_NAME_FLOAT:
            if (aValue instanceof Number n) {
                aFs.setFloatValue(aFeature, n.floatValue());
            }
            break;
        default:
            // Unsupported feature range; skip silently.
            break;
        }
    }

    private ResolvedType resolveType(String aLabel, String aBasicLayerName)
    {
        if (aLabel != null) {
            // 1. Exact fully-qualified match
            if (FQN.matcher(aLabel).matches()) {
                var t = typeSystem.getType(aLabel);
                if (t != null) {
                    return new ResolvedType(t, false);
                }
            }
            // 2. Suffix match for plain identifiers
            if (IDENTIFIER.matcher(aLabel).matches()) {
                Type match = null;
                int matches = 0;
                for (var it = typeSystem.getTypeIterator(); it.hasNext();) {
                    var t = it.next();
                    var simple = t.getShortName();
                    if (aLabel.equals(simple)) {
                        matches++;
                        match = t;
                    }
                }
                if (matches == 1) {
                    return new ResolvedType(match, false);
                }
                if (matches > 1) {
                    LOG.warn("Skipping ambiguous type label '{}': {} matches in type system",
                            aLabel, matches);
                    return null;
                }
            }
        }
        // 3. Basic fallback
        var basic = typeSystem.getType(aBasicLayerName);
        if (basic == null) {
            return null;
        }
        return new ResolvedType(basic, true);
    }

    private record ResolvedType(Type type, boolean isBasicFallback) {}
}
