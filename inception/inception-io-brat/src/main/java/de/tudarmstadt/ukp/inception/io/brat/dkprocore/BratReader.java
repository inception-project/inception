/*
 * Copyright 2017
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

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.parameter.ComponentParameters;
import org.dkpro.core.api.parameter.MimeTypes;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.mapping.Mapping;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.mapping.RelationMapping;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.mapping.TypeMapping;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratAnnotation;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratAnnotationDocument;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratAttribute;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratEventAnnotation;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratEventArgument;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratNoteAnnotation;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratRelationAnnotation;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratTextAnnotation;

/**
 * Reader for the brat format.
 * 
 * @see <a href="http://brat.nlplab.org/standoff.html">brat standoff format</a>
 * @see <a href="http://brat.nlplab.org/configuration.html">brat configuration format</a>
 */
@ResourceMetaData(name = "Brat Reader")
// @DocumentationResource("${docbase}/format-reference.html#format-${command}")
@MimeTypeCapability({ MimeTypes.APPLICATION_X_BRAT })
public class BratReader
    extends JCasResourceCollectionReader_ImplBase
{
    /**
     * Name of configuration parameter that contains the character encoding used by the input files.
     */
    public static final String PARAM_SOURCE_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
    @ConfigurationParameter(name = PARAM_SOURCE_ENCODING, defaultValue = ComponentParameters.DEFAULT_ENCODING)
    private String sourceEncoding;

    /**
     * Configuration
     */
    public static final String PARAM_MAPPING = "mapping";
    @ConfigurationParameter(name = PARAM_MAPPING, mandatory = false)
    private String mappingJson;

    /**
     * Lenient.
     */
    public static final String PARAM_LENIENT = "lenient";
    @ConfigurationParameter(name = PARAM_LENIENT, mandatory = true, defaultValue = "false")
    private boolean lenient;

    private Mapping mapping;

    private Map<String, AnnotationFS> idMap;

    private Set<String> warnings;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);

        if (mappingJson != null) {
            var mapper = new ObjectMapper();
            mapper.setDefaultSetterInfo(JsonSetter.Value.forContentNulls(Nulls.AS_EMPTY));
            mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            try {
                mapping = mapper.readValue(mappingJson, Mapping.class);
            }
            catch (IOException e) {
                throw new ResourceInitializationException(e);
            }
        }
        else {
            mapping = new Mapping();
        }

        warnings = new LinkedHashSet<String>();
    }

    @Override
    public void close() throws IOException
    {
        super.close();

        for (var warning : warnings) {
            getLogger().warn(warning);
        }
    }

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        idMap = new HashMap<>();

        var res = nextFile();
        initCas(aJCas, res);

        if (res.getLocation().endsWith(".zip")) {
            try (var zipFile = new ZipFile(res.getResource().getFile())) {
                ZipEntry annFile = null;
                for (var zipEnumerate = zipFile.entries(); zipEnumerate.hasMoreElements();) {
                    var entry = (ZipEntry) zipEnumerate.nextElement();
                    if (entry.getName().endsWith(".ann") && !entry.getName().contains("/")) {
                        annFile = entry;
                        break;
                    }
                }

                if (annFile == null) {
                    throw new FileNotFoundException(
                            "ZIP archive does not contain a [.ann] annotation file");
                }

                var textEntryName = FilenameUtils.removeExtension(annFile.getName()) + ".txt";
                var textEntry = zipFile.getEntry(textEntryName);
                if (textEntry == null) {
                    throw new FileNotFoundException(
                            "ZIP archive does not contain a [.txt] file that goes along with the [.ann] file");
                }

                readText(aJCas, zipFile.getInputStream(textEntry));
                readAnnotations(aJCas, zipFile.getInputStream(annFile));
            }
            catch (IOException e) {
                throw e;
            }
            catch (Exception e) {
                throw new CollectionException(e);
            }
        }
        else {
            var annUrl = res.getResource().getURL().toString();
            var textUrl = FilenameUtils.removeExtension(annUrl) + ".txt";

            try (var is = new URL(textUrl).openStream()) {
                readText(aJCas, is);
            }

            try (var is = res.getInputStream()) {
                readAnnotations(aJCas, is);
            }
        }
    }

    private void readAnnotations(JCas aJCas, InputStream aIs) throws IOException
    {
        BratAnnotationDocument doc;
        try (var r = new InputStreamReader(aIs, sourceEncoding)) {
            doc = BratAnnotationDocument.read(r);
        }

        var cas = aJCas.getCas();
        var ts = aJCas.getTypeSystem();

        var relations = new ArrayList<BratRelationAnnotation>();
        var events = new ArrayList<BratEventAnnotation>();
        var notes = new ArrayList<BratNoteAnnotation>();
        for (var anno : doc.getAnnotations()) {
            if (anno instanceof BratTextAnnotation) {
                var type = mapping.getTextTypeMapppings().getUimaType(ts, anno);
                create(cas, type, (BratTextAnnotation) anno);
            }
            else if (anno instanceof BratRelationAnnotation) {
                relations.add((BratRelationAnnotation) anno);
            }
            else if (anno instanceof BratNoteAnnotation) {
                notes.add((BratNoteAnnotation) anno);
            }
            else if (anno instanceof BratEventAnnotation) {
                var type = mapping.getTextTypeMapppings().getUimaType(ts, anno);
                create(cas, type, (BratEventAnnotation) anno);
                events.add((BratEventAnnotation) anno);
            }
            else {
                throw new IllegalStateException(
                        "Annotation type [" + anno.getClass() + "] is currently not supported.");
            }
        }

        // Go through the relations now
        for (var rel : relations) {
            var type = mapping.getRelationTypeMapppings().getUimaType(ts, rel);
            create(cas, type, rel);
        }

        // Go through the events again and handle the slots
        for (var event : events) {
            var type = mapping.getTextTypeMapppings().getUimaType(ts, event);
            fillSlots(cas, type, doc, event);
        }

        // Finally go through the notes and map them to features (if configured to do so)
        for (var n : notes) {
            var anno = idMap.get(n.getTarget());

            var type = anno.getType();
            var mappings = mapping.getCommentMapping(type.getName());

            if (mappings.isEmpty()) {
                warnings.add("No comment mappings defined for note type [" + n.getType()
                        + "] on annotation type [" + type.getName() + "]");
                continue;
            }

            var attrs = new ArrayList<BratAttribute>();
            for (var m : mappings) {
                if (m.matches(n.getNote())) {
                    attrs.add(new BratAttribute(-1, m.getFeature(), n.getTarget(), m.apply()));
                }
            }
            fillAttributes(anno, attrs);
        }
    }

    private void readText(JCas aJCas, InputStream aIS) throws IOException
    {
        try (var is = new BufferedInputStream(aIS)) {
            aJCas.setDocumentText(IOUtils.toString(is, sourceEncoding));
        }
    }

    private void create(CAS aCAS, Type aType, BratTextAnnotation aAnno)
    {
        var param = mapping.getSpanMapping(aType.getName());
        var tmap = mapping.getTextTypeMapppings().getMappingByBratType(aAnno.getType());

        for (var offset : aAnno.getOffsets()) {
            var anno = aCAS.createAnnotation(aType, offset.getBegin(), offset.getEnd());

            if (tmap != null) {
                fillDefaultAttributes(anno, tmap.getDefaultFeatureValues());
            }

            if (param != null) {
                fillDefaultAttributes(anno, param.getDefaultFeatureValues());
            }

            fillAttributes(anno, aAnno.getAttributes());

            if (param != null && param.getSubcat() != null) {
                anno.setStringValue(getFeature(anno, param.getSubcat()), aAnno.getType());
            }

            aCAS.addFsToIndexes(anno);
            idMap.put(aAnno.getId(), anno);
        }
    }

    private void create(CAS aCAS, Type aType, BratEventAnnotation aAnno)
    {
        var param = mapping.getSpanMapping(aType.getName());
        var tmap = mapping.getTextTypeMapppings().getMappingByBratType(aAnno.getType());

        for (var offset : aAnno.getTriggerAnnotation().getOffsets()) {
            var anno = aCAS.createAnnotation(aType, offset.getBegin(), offset.getEnd());

            if (tmap != null) {
                fillDefaultAttributes(anno, tmap.getDefaultFeatureValues());
            }

            if (param != null) {
                fillDefaultAttributes(anno, param.getDefaultFeatureValues());
            }

            fillAttributes(anno, aAnno.getAttributes());

            if (param != null && param.getSubcat() != null) {
                anno.setStringValue(getFeature(anno, param.getSubcat()), aAnno.getType());
            }

            // Slots cannot be handled yet because they might point to events that have not been
            // created yet.

            aCAS.addFsToIndexes(anno);
            idMap.put(aAnno.getId(), anno);
        }
    }

    private void create(CAS aCAS, Type aType, BratRelationAnnotation aAnno)
    {
        RelationMapping param = mapping.getRelationMapping(aType.getName());
        TypeMapping tmap = mapping.getRelationTypeMapppings().getMappingByBratType(aAnno.getType());

        AnnotationFS arg1 = idMap.get(aAnno.getArg1Target());
        AnnotationFS arg2 = idMap.get(aAnno.getArg2Target());

        AnnotationFS anno = aCAS.createFS(aType);

        anno.setFeatureValue(getFeature(anno, param.getArg1()), arg1);
        anno.setFeatureValue(getFeature(anno, param.getArg2()), arg2);

        AnnotationFS anchor = null;
        if (param.getFlags1().contains(RelationMapping.FLAG_ANCHOR)
                && param.getFlags2().contains(RelationMapping.FLAG_ANCHOR)) {
            throw new IllegalStateException("Only one argument can be the anchor.");
        }
        else if (param.getFlags1().contains(RelationMapping.FLAG_ANCHOR)) {
            anchor = arg1;
        }
        else if (param.getFlags2().contains(RelationMapping.FLAG_ANCHOR)) {
            anchor = arg2;
        }

        if (tmap != null) {
            fillDefaultAttributes(anno, tmap.getDefaultFeatureValues());
        }

        if (param != null) {
            fillDefaultAttributes(anno, param.getDefaultFeatureValues());
        }

        fillAttributes(anno, aAnno.getAttributes());

        if (param.getSubcat() != null) {
            anno.setStringValue(getFeature(anno, param.getSubcat()), aAnno.getType());
        }

        if (anchor != null) {
            anno.setIntValue(anno.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_BEGIN),
                    anchor.getBegin());
            anno.setIntValue(anno.getType().getFeatureByBaseName(CAS.FEATURE_BASE_NAME_END),
                    anchor.getEnd());
        }
        else {
            TypeSystem ts = aCAS.getTypeSystem();
            if (ts.subsumes(ts.getType(CAS.TYPE_NAME_ANNOTATION), anno.getType())) {
                warnings.add("Relation type [" + aType.getName()
                        + "] has offsets but no anchor is specified.");
            }
        }

        aCAS.addFsToIndexes(anno);
        idMap.put(aAnno.getId(), anno);
    }

    private void fillDefaultAttributes(FeatureStructure aAnno, Map<String, String> aValues)
    {
        for (Entry<String, String> e : aValues.entrySet()) {
            Feature feat = aAnno.getType().getFeatureByBaseName(e.getKey());

            if (feat == null) {
                throw new IllegalStateException("Type [" + aAnno.getType().getName()
                        + "] has no feature named [" + e.getKey() + "]");
            }

            aAnno.setFeatureValueFromString(feat, e.getValue());
        }
    }

    private void fillAttributes(FeatureStructure aAnno, Collection<BratAttribute> aAttributes)
    {
        for (var attr : aAttributes) {
            // Try treating the attribute name as an unqualified name, then as a qualified name.
            var feat = aAnno.getType().getFeatureByBaseName(attr.getName());
            if (feat == null) {
                var featName = attr.getName().replace('_', ':');
                featName = featName.substring(featName.indexOf(TypeSystem.FEATURE_SEPARATOR) + 1);
                feat = aAnno.getType().getFeatureByBaseName(featName);
            }

            // FIXME HACK! We may not find a "role" feature from slot links in the target type
            // because it should be in the link type. This here is a bad hack, but it should work
            // as long as the target type doesn't define a "role" feature itself.
            if ((("role".equals(attr.getName())) || attr.getName().endsWith("_role"))
                    && feat == null) {
                return;
            }

            if (feat == null) {
                if (lenient) {
                    continue;
                }

                throw new IllegalStateException("Type [" + aAnno.getType().getName()
                        + "] has no feature named [" + attr.getName() + "]");
            }

            if (attr.getValues().length == 0) {
                // Nothing to do
            }
            else if (attr.getValues().length == 1) {
                aAnno.setFeatureValueFromString(feat, attr.getValues()[0]);
            }
            else {
                throw new IllegalStateException("Multi-valued attributes currently not supported");
            }
        }
    }

    private void fillSlots(CAS aCas, Type aType, BratAnnotationDocument aDoc,
            BratEventAnnotation aE)
    {
        AnnotationFS event = idMap.get(aE.getId());
        Map<String, List<BratEventArgument>> groupedArgs = aE.getGroupedArguments();

        for (Entry<String, List<BratEventArgument>> slot : groupedArgs.entrySet()) {
            // Resolve the target IDs to feature structures
            List<FeatureStructure> targets = new ArrayList<>();

            // Lets see if there is a multi-valued feature by the name of the slot
            if (FSUtil.hasFeature(event, slot.getKey())
                    && FSUtil.isMultiValuedFeature(event, slot.getKey())) {
                for (BratEventArgument arg : slot.getValue()) {
                    FeatureStructure target = idMap.get(arg.getTarget());
                    if (target == null) {
                        throw new IllegalStateException(
                                "Unable to resolve id [" + arg.getTarget() + "]");
                    }

                    // Handle WebAnno-style slot links
                    // FIXME It would be better if the link type could be configured, e.g. what
                    // is the name of the link feature and what is the name of the role feature...
                    // but right now we just keep it hard-coded to the values that are used
                    // in the DKPro Core SemArgLink and that are also hard-coded in WebAnno
                    Type componentType = event.getType().getFeatureByBaseName(slot.getKey())
                            .getRange().getComponentType();
                    if (CAS.TYPE_NAME_TOP
                            .equals(aCas.getTypeSystem().getParent(componentType).getName())) {
                        BratAnnotation targetAnno = aDoc.getAnnotation(arg.getTarget());
                        BratAttribute roleAttr = targetAnno.getAttribute("role");
                        if (roleAttr == null) {
                            roleAttr = targetAnno.getAttribute(
                                    target.getType().getName().replace('.', '-') + "_role");
                        }
                        FeatureStructure link = aCas.createFS(componentType);
                        if (roleAttr != null) {
                            FSUtil.setFeature(link, "role", roleAttr.getValues());
                        }
                        FSUtil.setFeature(link, "target", target);
                        target = link;
                    }

                    targets.add(target);
                }
                FSUtil.setFeature(event, slot.getKey(), targets);
            }
            // Lets see if there is a single-valued feature by the name of the slot
            else if (FSUtil.hasFeature(event, slot.getKey())) {
                for (BratEventArgument arg : slot.getValue()) {
                    AnnotationFS target = idMap.get(arg.getTarget());
                    if (target == null) {
                        throw new IllegalStateException(
                                "Unable to resolve id [" + arg.getTarget() + "]");
                    }

                    String fname = arg.getSlot() + (arg.getIndex() > 0 ? arg.getIndex() : "");
                    if (FSUtil.hasFeature(event, fname)) {
                        FSUtil.setFeature(event, fname, target);
                    }
                    else {
                        throw new IllegalStateException("Type [" + event.getType().getName()
                                + "] has no feature named [" + fname + "]");
                    }
                }
            }
            else {
                throw new IllegalStateException("Type [" + event.getType().getName()
                        + "] has no feature named [" + slot.getKey() + "]");
            }
        }
    }

    private Feature getFeature(FeatureStructure aFS, String aName)
    {
        Feature f = aFS.getType().getFeatureByBaseName(aName);
        if (f == null) {
            throw new IllegalArgumentException(
                    "Type [" + aFS.getType().getName() + "] has no feature named [" + aName + "]");
        }
        return f;
    }
}
