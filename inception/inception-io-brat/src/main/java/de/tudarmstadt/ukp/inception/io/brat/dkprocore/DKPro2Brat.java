/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.io.brat.dkprocore;

import static de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratConstants.CARD_ZERO_OR_MORE;
import static org.apache.uima.cas.CAS.TYPE_NAME_BYTE;
import static org.apache.uima.cas.CAS.TYPE_NAME_DOUBLE;
import static org.apache.uima.cas.CAS.TYPE_NAME_FLOAT;
import static org.apache.uima.cas.CAS.TYPE_NAME_INTEGER;
import static org.apache.uima.cas.CAS.TYPE_NAME_LONG;
import static org.apache.uima.cas.CAS.TYPE_NAME_SHORT;
import static org.apache.uima.fit.util.JCasUtil.selectAll;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.mapping.RelationMapping;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.mapping.TypeMappings;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratAnnotation;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratAnnotationDocument;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratConfiguration;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratConstants;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratEventAnnotation;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratEventAnnotationDecl;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratEventArgument;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratEventArgumentDecl;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratRelationAnnotation;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratTextAnnotation;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratTextAnnotationDrawingDecl;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.Offsets;

public class DKPro2Brat
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final static Pattern NEWLINE_EXTRACT_PATTERN = Pattern.compile("(.+?)(?:\\R|$)+");

    private final BratConfiguration conf;

    private int nextEventAnnotationId;
    private int nextTextAnnotationId;
    private int nextRelationAnnotationId;
    private int nextAttributeId;
    private int nextPaletteIndex;
    private Map<FeatureStructure, String> spanIdMap;

    private Set<String> warnings;

    private String[] palette = new String[] { "#8dd3c7", "#ffffb3", "#bebada", "#fb8072", "#80b1d3",
            "#fdb462", "#b3de69", "#fccde5", "#d9d9d9", "#bc80bd", "#ccebc5", "#ffed6f" };
    private Set<String> excludeTypes = Collections
            .singleton("de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence");
    private Set<String> spanTypes = new HashSet<>();
    private Map<String, RelationMapping> parsedRelationTypes = new HashMap<>();
    private TypeMappings typeMapping;

    private boolean writeRelationAttributes;
    private boolean writeNullAttributes;
    private boolean shortAttributeNames;
    private boolean shortTypeNames;

    public DKPro2Brat(BratConfiguration aConf)
    {
        super();
        conf = aConf;
    }

    public boolean isWriteRelationAttributes()
    {
        return writeRelationAttributes;
    }

    public void setWriteRelationAttributes(boolean aWriteRelationAttributes)
    {
        writeRelationAttributes = aWriteRelationAttributes;
    }

    public boolean isWriteNullAttributes()
    {
        return writeNullAttributes;
    }

    public void setWriteNullAttributes(boolean aWriteNullAttributes)
    {
        writeNullAttributes = aWriteNullAttributes;
    }

    public boolean isShortAttributeNames()
    {
        return shortAttributeNames;
    }

    public boolean isShortTypeNames()
    {
        return shortTypeNames;
    }

    public void setShortTypeNames(boolean aShortTypeNames)
    {
        shortTypeNames = aShortTypeNames;
    }

    public void setShortAttributeNames(boolean aShortAttributeNames)
    {
        shortAttributeNames = aShortAttributeNames;
    }

    public String[] getPalette()
    {
        return palette;
    }

    public void setPalette(String[] aPalette)
    {
        palette = aPalette;
    }

    public Set<String> getExcludeTypes()
    {
        return excludeTypes;
    }

    public void setExcludeTypes(Set<String> aExcludeTypes)
    {
        excludeTypes = aExcludeTypes;
    }

    public Map<String, RelationMapping> getRelationTypes()
    {
        return parsedRelationTypes;
    }

    public void setRelationTypes(Collection<RelationMapping> aRelationTypes)
    {
        aRelationTypes.stream().forEachOrdered(p -> parsedRelationTypes.put(p.getType(), p));
    }

    public Set<String> getSpanTypes()
    {
        return spanTypes;
    }

    public void setSpanTypes(Set<String> aSpanTypes)
    {
        spanTypes = aSpanTypes;
    }

    public TypeMappings getTypeMapping()
    {
        return typeMapping;
    }

    public void setTypeMapping(TypeMappings aTypeMapping)
    {
        typeMapping = aTypeMapping;
    }

    private void init()
    {
        nextEventAnnotationId = 1;
        nextTextAnnotationId = 1;
        nextRelationAnnotationId = 1;
        nextAttributeId = 1;
        nextPaletteIndex = 0;
        spanIdMap = new HashMap<>();
        warnings = new LinkedHashSet<>();
    }

    public Set<String> convert(JCas aJCas, BratAnnotationDocument doc)
    {
        init();

        List<FeatureStructure> relationFS = new ArrayList<>();

        Map<BratEventAnnotation, FeatureStructure> eventFS = new LinkedHashMap<>();

        // Go through all the annotations but only handle the ones that have no references to
        // other annotations.
        for (FeatureStructure fs : selectAll(aJCas)) {
            // Skip document annotation
            if (fs == aJCas.getDocumentAnnotationFs()) {
                continue;
            }

            // Skip excluded types
            if (excludeTypes.contains(fs.getType().getName())) {
                LOG.debug("Excluding [" + fs.getType().getName() + "]");
                continue;
            }

            if (spanTypes.contains(fs.getType().getName())) {
                writeTextAnnotation(doc, (AnnotationFS) fs);
            }
            else if (parsedRelationTypes.containsKey(fs.getType().getName())) {
                relationFS.add(fs);
            }
            else if (hasNonPrimitiveFeatures(fs) && (fs instanceof AnnotationFS)) {
                // else if (parsedEventTypes.containsKey(fs.getType().getName())) {
                BratEventAnnotation event = writeEventAnnotation(doc, (AnnotationFS) fs);
                eventFS.put(event, fs);
            }
            else if (fs instanceof AnnotationFS) {
                warnings.add("Assuming annotation type [" + fs.getType().getName() + "] is span");
                writeTextAnnotation(doc, (AnnotationFS) fs);
            }
            else {
                warnings.add("Skipping annotation with type [" + fs.getType().getName() + "]");
            }
        }

        // Handle relations now since now we can resolve their targets to IDs.
        for (FeatureStructure fs : relationFS) {
            writeRelationAnnotation(doc, fs);
        }

        // Handle event slots now since now we can resolve their targets to IDs.
        for (Entry<BratEventAnnotation, FeatureStructure> e : eventFS.entrySet()) {
            writeSlots(doc, e.getKey(), e.getValue());
        }

        return warnings;
    }

    /**
     * Checks if the feature structure has non-default non-primitive properties.
     */
    private boolean hasNonPrimitiveFeatures(FeatureStructure aFS)
    {
        for (Feature f : aFS.getType().getFeatures()) {
            if (CAS.FEATURE_BASE_NAME_SOFA.equals(f.getShortName())) {
                continue;
            }

            if (!f.getRange().isPrimitive()) {
                return true;
            }
        }

        return false;
    }

    private BratEventAnnotation writeEventAnnotation(BratAnnotationDocument aDoc, AnnotationFS aFS)
    {

        // Write trigger annotation
        BratTextAnnotation trigger = splitNewline(aFS);

        nextTextAnnotationId++;

        // Write event annotation
        BratEventAnnotation event = new BratEventAnnotation(nextEventAnnotationId,
                getBratType(aFS.getType()), trigger.getId());
        spanIdMap.put(aFS, event.getId());
        nextEventAnnotationId++;

        // We do not add the trigger annotations to the document - they are owned by the event
        // aDoc.addAnnotation(trigger);
        event.setTriggerAnnotation(trigger);

        // Write attributes
        writeAttributes(event, aFS);

        // Slots are written later after we know all the span/event IDs

        conf.addLabelDecl(event.getType(), aFS.getType().getShortName(),
                aFS.getType().getShortName().substring(0, 1));

        if (!conf.hasDrawingDecl(event.getType())) {
            conf.addDrawingDecl(new BratTextAnnotationDrawingDecl(event.getType(), "black",
                    palette[nextPaletteIndex % palette.length]));
            nextPaletteIndex++;
        }

        aDoc.addAnnotation(event);
        return event;
    }

    private void writeTextAnnotation(BratAnnotationDocument aDoc, AnnotationFS aFS)
    {
        String superType = getBratType(aFS.getCAS().getTypeSystem().getParent(aFS.getType()));
        String type = getBratType(aFS.getType());
        BratTextAnnotation anno = splitNewline(aFS);

        nextTextAnnotationId++;

        conf.addEntityDecl(superType, type);

        conf.addLabelDecl(anno.getType(), aFS.getType().getShortName(),
                aFS.getType().getShortName().substring(0, 1));

        if (!conf.hasDrawingDecl(anno.getType())) {
            conf.addDrawingDecl(new BratTextAnnotationDrawingDecl(anno.getType(), "black",
                    palette[nextPaletteIndex % palette.length]));
            nextPaletteIndex++;
        }

        aDoc.addAnnotation(anno);

        writeAttributes(anno, aFS);

        spanIdMap.put(aFS, anno.getId());
    }

    private void writeRelationAnnotation(BratAnnotationDocument aDoc, FeatureStructure aFS)
    {
        RelationMapping rel = parsedRelationTypes.get(aFS.getType().getName());

        FeatureStructure arg1 = aFS
                .getFeatureValue(aFS.getType().getFeatureByBaseName(rel.getArg1()));
        FeatureStructure arg2 = aFS
                .getFeatureValue(aFS.getType().getFeatureByBaseName(rel.getArg2()));

        if (arg1 == null || arg2 == null) {
            throw new IllegalArgumentException("Dangling relation");
        }

        String arg1Id = spanIdMap.get(arg1);
        String arg2Id = spanIdMap.get(arg2);

        if (arg1Id == null || arg2Id == null) {
            throw new IllegalArgumentException("Unknown targets!");
        }

        String superType = getBratType(aFS.getCAS().getTypeSystem().getParent(aFS.getType()));
        String type = getBratType(aFS.getType());

        BratRelationAnnotation anno = new BratRelationAnnotation(nextRelationAnnotationId, type,
                rel.getArg1(), arg1Id, rel.getArg2(), arg2Id);
        nextRelationAnnotationId++;

        conf.addRelationDecl(superType, type, rel.getArg1(), rel.getArg2());

        conf.addLabelDecl(anno.getType(), aFS.getType().getShortName(),
                aFS.getType().getShortName().substring(0, 1));

        aDoc.addAnnotation(anno);

        // brat doesn't support attributes on relations
        // https://github.com/nlplab/brat/issues/791
        if (writeRelationAttributes) {
            writeAttributes(anno, aFS);
        }
    }

    private void writeAttributes(BratAnnotation aAnno, FeatureStructure aFS)
    {
        for (Feature feat : aFS.getType().getFeatures()) {
            // Skip Sofa feature
            if (isInternalFeature(feat)) {
                continue;
            }

            // No need to write begin / end, they are already on the text annotation
            if (CAS.FEATURE_FULL_NAME_BEGIN.equals(feat.getName())
                    || CAS.FEATURE_FULL_NAME_END.equals(feat.getName())) {
                continue;
            }

            // No need to write link endpoints again, they are already on the relation annotation
            RelationMapping relParam = parsedRelationTypes.get(aFS.getType().getName());
            if (relParam != null) {
                if (relParam.getArg1().equals(feat.getShortName())
                        || relParam.getArg2().equals(feat.getShortName())) {
                    continue;
                }
            }

            if (feat.getRange().isPrimitive()) {
                writePrimitiveAttribute(aAnno, aFS, feat);
            }
            // The following warning is not relevant for event annotations because these render such
            // features as slots.
            else if (!(aAnno instanceof BratEventAnnotation)) {
                warnings.add("Unable to render feature [" + feat.getName() + "] with range ["
                        + feat.getRange().getName() + "] as attribute");
            }
        }
    }

    private void writeSlots(BratAnnotationDocument aDoc, BratEventAnnotation aEvent,
            FeatureStructure aFS)
    {
        String superType = getBratType(aFS.getCAS().getTypeSystem().getParent(aFS.getType()));
        String type = getBratType(aFS.getType());

        assert type.equals(aEvent.getType());

        BratEventAnnotationDecl decl = conf.getEventDecl(type);
        if (decl == null) {
            decl = new BratEventAnnotationDecl(superType, type);
            conf.addEventDecl(decl);
        }

        Map<String, List<BratEventArgument>> slots = new LinkedHashMap<>();
        for (Feature feat : aFS.getType().getFeatures()) {
            if (!isSlotFeature(aFS, feat)) {
                continue;
            }
            String slot = feat.getShortName();

            List<BratEventArgument> args = slots.get(slot);
            if (args == null) {
                args = new ArrayList<>();
                slots.put(slot, args);
            }

            if (FSUtil.isMultiValuedFeature(aFS, feat)
                    // this can only be true for array types
                    && feat.getRange().getComponentType() != null
                    // Avoid calling getParent on TOP
                    && !CAS.TYPE_NAME_TOP.equals(feat.getRange().getComponentType().getName())
                    && CAS.TYPE_NAME_TOP.equals(aFS.getCAS().getTypeSystem()
                            .getParent(feat.getRange().getComponentType()).getName())
                    && (feat.getRange().getComponentType().getFeatureByBaseName("target") != null)
                    && (feat.getRange().getComponentType().getFeatureByBaseName("role") != null)) {
                // Handle WebAnno-style slot links
                // FIXME It would be better if the link type could be configured, e.g. what
                // is the name of the link feature and what is the name of the role feature...
                // but right now we just keep it hard-coded to the values that are used
                // in the DKPro Core SemArgLink and that are also hard-coded in WebAnno
                var slotDecl = new BratEventArgumentDecl(slot, CARD_ZERO_OR_MORE);
                decl.addSlot(slotDecl);

                FeatureStructure[] links = FSUtil.getFeature(aFS, feat, FeatureStructure[].class);
                if (links != null) {
                    for (FeatureStructure link : links) {
                        FeatureStructure target = FSUtil.getFeature(link, "target",
                                FeatureStructure.class);
                        Feature roleFeat = link.getType().getFeatureByBaseName("role");
                        BratEventArgument arg = new BratEventArgument(slot, args.size(),
                                spanIdMap.get(target));
                        args.add(arg);

                        // Attach the role attribute to the target span
                        BratAnnotation targetAnno = aDoc.getAnnotation(spanIdMap.get(target));
                        writePrimitiveAttribute(targetAnno, link, roleFeat);
                    }
                }
            }
            else if (FSUtil.isMultiValuedFeature(aFS, feat)) {
                // Handle normal multi-valued features
                var slotDecl = new BratEventArgumentDecl(slot, CARD_ZERO_OR_MORE);
                decl.addSlot(slotDecl);

                FeatureStructure[] targets = FSUtil.getFeature(aFS, feat, FeatureStructure[].class);
                if (targets != null) {
                    for (FeatureStructure target : targets) {
                        BratEventArgument arg = new BratEventArgument(slot, args.size(),
                                spanIdMap.get(target));
                        args.add(arg);
                    }
                }
            }
            else {
                // Handle normal single-valued features
                var slotDecl = new BratEventArgumentDecl(slot, BratConstants.CARD_OPTIONAL);
                decl.addSlot(slotDecl);

                FeatureStructure target = FSUtil.getFeature(aFS, feat, FeatureStructure.class);
                if (target != null) {
                    var arg = new BratEventArgument(slot, args.size(), spanIdMap.get(target));
                    args.add(arg);
                }
            }
        }

        aEvent.setArguments(slots.values().stream().flatMap(args -> args.stream())
                .collect(Collectors.toList()));
    }

    private boolean isSlotFeature(FeatureStructure aFS, Feature aFeature)
    {
        return !isInternalFeature(aFeature) && (FSUtil.isMultiValuedFeature(aFS, aFeature)
                || !aFeature.getRange().isPrimitive());
    }

    private boolean isInternalFeature(Feature aFeature)
    {
        return CAS.FEATURE_FULL_NAME_SOFA.equals(aFeature.getName());
    }

    private void writePrimitiveAttribute(BratAnnotation aAnno, FeatureStructure aFS, Feature feat)
    {
        var featureValue = aFS.getFeatureValueAsString(feat);
        var rangeType = feat.getRange().getName();

        // Do not write attributes with null values unless this is explicitly enabled
        if (!writeNullAttributes && (
        // null value
        featureValue == null || (
        // zero value for integer values
        "0".equals(featureValue)
                && (TYPE_NAME_BYTE.equals(rangeType) || TYPE_NAME_SHORT.equals(rangeType)
                        || TYPE_NAME_INTEGER.equals(rangeType) || TYPE_NAME_LONG.equals(rangeType)))
        // zero value for float values
                || (TYPE_NAME_DOUBLE.equals(rangeType) && aFS.getDoubleValue(feat) == 0.0d)
                || (TYPE_NAME_FLOAT.equals(rangeType) && aFS.getFloatValue(feat) == 0.0f))) {
            return;
        }

        var attributeName = shortAttributeNames //
                ? feat.getShortName() //
                : aAnno.getType() + '_' + feat.getShortName();

        aAnno.addAttribute(nextAttributeId, attributeName, featureValue);
        nextAttributeId++;

        // Do not write certain values to the visual/annotation configuration because
        // they are not compatible with the brat annotation file format. The values are
        // still maintained in the ann file.
        if (isValidFeatureValue(featureValue)) {
            // Features are inherited to subtypes in UIMA. By storing the attribute under
            // the name of the type that declares the feature (domain) instead of the name
            // of the actual instance we are processing, we make sure not to maintain
            // multiple value sets for the same feature.
            var attrDecl = conf.addAttributeDecl(aAnno.getType(),
                    getAllSubtypes(aFS.getCAS().getTypeSystem(), feat.getDomain()), attributeName,
                    featureValue);
            conf.addDrawingDecl(attrDecl);
        }
    }

    // This generates lots of types as well that we may not otherwise have in declared in the
    // brat configuration files, but brat doesn't seem to mind.
    private Set<String> getAllSubtypes(TypeSystem aTS, Type aType)
    {
        Set<String> types = new LinkedHashSet<>();
        aTS.getProperlySubsumedTypes(aType).stream().forEach(t -> types.add(getBratType(t)));
        return types;
    }

    /**
     * Some feature values do not need to be registered or cannot be registered because brat does
     * not support them.
     */
    private boolean isValidFeatureValue(String aFeatureValue)
    {
        // https://github.com/nlplab/brat/issues/1149
        return !(aFeatureValue == null || aFeatureValue.length() == 0 || aFeatureValue.equals(","));
    }

    private BratTextAnnotation splitNewline(AnnotationFS aFS)
    {

        // extract all but newlines as groups
        Matcher m = NEWLINE_EXTRACT_PATTERN.matcher(aFS.getCoveredText());
        List<Offsets> offsets = new ArrayList<>();
        while (m.find()) {
            Offsets offset = new Offsets(m.start(1) + aFS.getBegin(), m.end(1) + aFS.getBegin());
            offsets.add(offset);
        }
        // replaces any group of newline by one space
        String[] texts = new String[] { aFS.getCoveredText().replaceAll("\\R+", " ") };
        return new BratTextAnnotation(nextTextAnnotationId, getBratType(aFS.getType()), offsets,
                texts);
    }

    private String getBratType(Type aType)
    {
        if (typeMapping != null) {
            return typeMapping.getBratType(aType);
        }

        if (shortTypeNames) {
            return aType.getShortName().replace('.', '-');
        }

        return aType.getName().replace('.', '-');
    }
}
