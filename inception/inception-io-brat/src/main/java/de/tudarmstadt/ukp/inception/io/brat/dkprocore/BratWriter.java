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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.io.JCasFileWriter_ImplBase;
import org.dkpro.core.api.parameter.ComponentParameters;
import org.dkpro.core.api.parameter.MimeTypes;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.mapping.RelationMapping;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.mapping.TypeMappings;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratAnnotationDocument;
import de.tudarmstadt.ukp.inception.io.brat.dkprocore.internal.model.BratConfiguration;

/**
 * Writer for the brat annotation format.
 * 
 * <p>
 * Known issues:
 * </p>
 * <ul>
 * <li><a href="https://github.com/nlplab/brat/issues/791">Brat is unable to read relation
 * attributes created by this writer.</a></li>
 * <li>PARAM_TYPE_MAPPINGS not implemented yet</li>
 * </ul>
 * 
 * @see <a href="http://brat.nlplab.org/standoff.html">brat standoff format</a>
 * @see <a href="http://brat.nlplab.org/configuration.html">brat configuration format</a>
 */
@ResourceMetaData(name = "Brat Writer")
// @DocumentationResource("${docbase}/format-reference.html#format-${command}")
@MimeTypeCapability({ MimeTypes.APPLICATION_X_BRAT })
public class BratWriter
    extends JCasFileWriter_ImplBase
{
    /**
     * Specify the suffix of text output files. Default value <code>.txt</code>. If the suffix is
     * not needed, provide an empty string as value.
     */
    public static final String PARAM_TEXT_FILENAME_EXTENSION = "textFilenameExtension";
    @ConfigurationParameter(name = PARAM_TEXT_FILENAME_EXTENSION, mandatory = true, defaultValue = ".txt")
    private String textFilenameExtension;

    /**
     * Specify the suffix of output files. Default value <code>.ann</code>. If the suffix is not
     * needed, provide an empty string as value.
     */
    public static final String PARAM_FILENAME_EXTENSION = ComponentParameters.PARAM_FILENAME_EXTENSION;
    @ConfigurationParameter(name = PARAM_FILENAME_EXTENSION, mandatory = true, defaultValue = ".ann")
    private String filenameSuffix;

    /**
     * Types that will not be written to the exported file.
     */
    public static final String PARAM_EXCLUDE_TYPES = "excludeTypes";
    @ConfigurationParameter(name = PARAM_EXCLUDE_TYPES, mandatory = true, defaultValue = {
            Sentence._TypeName })
    private Set<String> excludeTypes;

    /**
     * Types that are text annotations (aka entities or spans).
     */
    public static final String PARAM_TEXT_ANNOTATION_TYPES = "spanTypes";
    @ConfigurationParameter(name = PARAM_TEXT_ANNOTATION_TYPES, mandatory = true, defaultValue = {
            // "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
            // "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
            // "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS",
            // "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
            // "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem",
            // "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk",
            // "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity",
            // "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArg",
            // "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred"
    })
    private Set<String> spanTypes;

    /**
     * Types that are relations. It is mandatory to provide the type name followed by two feature
     * names that represent Arg1 and Arg2 separated by colons, e.g. <code>
     * de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency:Governor:Dependent
     * </code>.
     */
    public static final String PARAM_RELATION_TYPES = "relationTypes";
    @ConfigurationParameter(name = PARAM_RELATION_TYPES, defaultValue = {})
    /*
     * , defaultValue = { Dependency._TypeName + ":" + Dependency._FeatName_Governor + ":" +
     * Dependency._FeatName_Dependent }
     */
    private Set<String> relationTypes;

    // /**
    // * Types that are events. Optionally, multiple slot features can be specified.
    // * <code>my.type.Event:location:participant</code>.
    // */
    // public static final String PARAM_EVENT_TYPES = "eventTypes";
    // @ConfigurationParameter(name = PARAM_EVENT_TYPES, mandatory = true, defaultValue = { })
    // private Set<String> eventTypes;
    // private Map<String, EventParam> parsedEventTypes;

    /**
     * Enable type mappings.
     */
    public static final String PARAM_ENABLE_TYPE_MAPPINGS = "enableTypeMappings";
    @ConfigurationParameter(name = PARAM_ENABLE_TYPE_MAPPINGS, mandatory = true, defaultValue = "false")
    private boolean enableTypeMappings;

    /**
     * FIXME
     */
    public static final String PARAM_TYPE_MAPPINGS = "typeMappings";
    @ConfigurationParameter(name = PARAM_TYPE_MAPPINGS, mandatory = false, defaultValue = {
            "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.(\\w+) -> $1",
            "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.(\\w+) -> $1",
            "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.(\\w+) -> $1",
            "de.tudarmstadt.ukp.dkpro.core.api.ner.type.(\\w+) -> $1" })
    private String[] typeMappings;

    /**
     * The brat web application can currently not handle attributes on relations, thus they are
     * disabled by default. Here they can be enabled again.
     */
    public static final String PARAM_WRITE_RELATION_ATTRIBUTES = "writeRelationAttributes";
    @ConfigurationParameter(name = PARAM_WRITE_RELATION_ATTRIBUTES, mandatory = true, defaultValue = "false")
    private boolean writeRelationAttributes;

    /**
     * Enable writing of features with null values.
     */
    public static final String PARAM_WRITE_NULL_ATTRIBUTES = "writeNullAttributes";
    @ConfigurationParameter(name = PARAM_WRITE_NULL_ATTRIBUTES, mandatory = true, defaultValue = "false")
    private boolean writeNullAttributes;

    /**
     * Colors to be used for the visual configuration that is generated for brat.
     */
    public static final String PARAM_PALETTE = "palette";
    @ConfigurationParameter(name = PARAM_PALETTE, mandatory = false, defaultValue = { "#8dd3c7",
            "#ffffb3", "#bebada", "#fb8072", "#80b1d3", "#fdb462", "#b3de69", "#fccde5", "#d9d9d9",
            "#bc80bd", "#ccebc5", "#ffed6f" })
    private String[] palette;

    /**
     * Whether to render types by their short name or by their qualified name.
     */
    public static final String PARAM_SHORT_TYPE_NAMES = "shortTypeNames";
    @ConfigurationParameter(name = PARAM_SHORT_TYPE_NAMES, mandatory = true, defaultValue = "false")
    private boolean shortTypeNames;

    /**
     * Whether to render attributes by their short name or by their qualified name.
     */
    public static final String PARAM_SHORT_ATTRIBUTE_NAMES = "shortAttributeNames";
    @ConfigurationParameter(name = PARAM_SHORT_ATTRIBUTE_NAMES, mandatory = true, defaultValue = "false")
    private boolean shortAttributeNames;

    private BratConfiguration conf;
    private DKPro2Brat converter;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);

        // parsedEventTypes = new HashMap<>();
        // for (String rel : eventTypes) {
        // EventParam p = EventParam.parse(rel);
        // parsedEventTypes.put(p.getType(), p);
        // }

        conf = new BratConfiguration();
        converter = new DKPro2Brat(conf);
        converter.setWriteNullAttributes(writeNullAttributes);
        converter.setWriteRelationAttributes(writeRelationAttributes);
        converter.setShortAttributeNames(shortAttributeNames);
        converter.setShortTypeNames(shortTypeNames);
        converter.setPalette(palette);
        converter.setExcludeTypes(excludeTypes);
        converter.setSpanTypes(spanTypes);
        converter.setRelationTypes(
                relationTypes.stream().map(RelationMapping::parse).collect(Collectors.toList()));
        if (enableTypeMappings) {
            converter.setTypeMapping(new TypeMappings(typeMappings));
        }
    }

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException
    {
        try {
            if (".ann".equals(filenameSuffix)) {
                writeText(aJCas);
            }
            writeAnnotations(aJCas);
        }
        catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException
    {
        if (!".ann".equals(filenameSuffix)) {
            return;
        }

        try {
            writeAnnotationConfiguration();
            writeVisualConfiguration();
        }
        catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private void writeAnnotationConfiguration() throws IOException
    {
        try (Writer out = new OutputStreamWriter(getOutputStream("annotation", ".conf"), "UTF-8")) {
            conf.writeAnnotationConfiguration(out);
        }
    }

    private void writeVisualConfiguration() throws IOException
    {
        try (Writer out = new OutputStreamWriter(getOutputStream("visual", ".conf"), "UTF-8")) {
            conf.writeVisualConfiguration(out);
        }
    }

    private void writeAnnotations(JCas aJCas) throws IOException
    {
        BratAnnotationDocument doc = new BratAnnotationDocument();

        Collection<String> warnings = converter.convert(aJCas, doc);

        for (String warning : warnings) {
            getLogger().warn(warning);
        }

        switch (filenameSuffix) {
        case ".ann":
            try (Writer out = new OutputStreamWriter(getOutputStream(aJCas, filenameSuffix),
                    "UTF-8")) {
                doc.write(out);
                break;
            }
        case ".html":
        case ".json":
            String template;
            if (filenameSuffix.equals(".html")) {
                template = IOUtils.toString(getClass().getResource("html/template.html"));
            }
            else {
                template = "{ \"collData\" : ##COLL-DATA## , \"docData\" : ##DOC-DATA## }";
            }

            JsonFactory jfactory = new JsonFactory();
            try (Writer out = new OutputStreamWriter(getOutputStream(aJCas, filenameSuffix),
                    "UTF-8")) {
                String docData;
                try (StringWriter buf = new StringWriter()) {
                    try (JsonGenerator jg = jfactory.createGenerator(buf)) {
                        jg.useDefaultPrettyPrinter();
                        doc.write(jg, aJCas.getDocumentText());
                    }
                    docData = buf.toString();
                }

                String collData;
                try (StringWriter buf = new StringWriter()) {
                    try (JsonGenerator jg = jfactory.createGenerator(buf)) {
                        jg.useDefaultPrettyPrinter();
                        conf.write(jg);
                    }
                    collData = buf.toString();
                }

                template = StringUtils.replaceEach(template,
                        new String[] { "##COLL-DATA##", "##DOC-DATA##" },
                        new String[] { collData, docData });

                out.write(template);
            }
            conf = new BratConfiguration();
            break;
        default:
            throw new IllegalArgumentException("Unknown file format: [" + filenameSuffix + "]");
        }
    }

    private void writeText(JCas aJCas) throws IOException
    {
        try (OutputStream docOS = getOutputStream(aJCas, textFilenameExtension)) {
            IOUtils.write(aJCas.getDocumentText(), docOS);
        }
    }
}
