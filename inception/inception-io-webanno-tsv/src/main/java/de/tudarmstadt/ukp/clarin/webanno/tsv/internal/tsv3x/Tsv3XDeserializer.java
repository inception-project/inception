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
package de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x;

import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Escaping.unescapeText;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XParserState.END;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XParserState.INTER_SENTENCE_SPACE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XParserState.SENTENCE_HEADER;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XParserState.SUBTOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XParserState.TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.CHAIN_ELEMENT_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.CHAIN_LINK_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.PRIMITIVE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.RELATION_REF;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.SLOT_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.SLOT_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.FIELD_SEPARATOR;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_FIELD_SEPARATOR;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_LAYER_PREFIX_SEPARATOR;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_PREFIX_BASE_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_PREFIX_CHAIN_LAYER;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_PREFIX_FORMAT;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_PREFIX_RELATION_LAYER;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_PREFIX_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_PREFIX_SPAN_LAYER;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.LINE_BREAK;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.NULL_COLUMN;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.NULL_VALUE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.PREFIX_SENTENCE_HEADER;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.PREFIX_SENTENCE_ID;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.PREFIX_TEXT;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.SLOT_SEP;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.STACK_SEP;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.CHAIN;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.RELATION;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.SPAN;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.CHAIN_FIRST_FEAT;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.CHAIN_NEXT_FEAT;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.COREFERENCE_RELATION_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.COREFERENCE_TYPE_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_SLOT_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_SLOT_TARGET;
import static java.util.Collections.emptyList;
import static java.util.regex.Pattern.quote;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.apache.commons.lang3.StringUtils.split;
import static org.apache.commons.lang3.StringUtils.splitPreserveAllTokens;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.apache.uima.fit.util.FSUtil.setFeature;

import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvChain;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvColumn;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvDocument;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvFormatHeader;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSentence;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSubToken;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvToken;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvUnit;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class Tsv3XDeserializer
{
    private static final Pattern FORMAT_PATTERN = Pattern.compile(
            "^" + quote(HEADER_PREFIX_FORMAT) + "(?<NAME>.*) " + "(?<VERSION>\\d+\\.\\d+)$");

    private static final Pattern STACK_SEP_PATTERN = Pattern
            .compile("(?<!\\\\)" + Pattern.quote(STACK_SEP));

    private static final Pattern SLOT_SEP_PATTERN = Pattern
            .compile("(?<!\\\\)" + Pattern.quote(SLOT_SEP));

    private static final Pattern CHAIN_SUFFIX_PATTERN = Pattern
            .compile("^.*(?<!\\\\)->" + "(?<CHAIN>\\d+-\\d+)$");

    private ThreadLocal<List<Runnable>> deferredActions = new ThreadLocal<>();

    public void read(LineNumberReader aIn, JCas aJCas) throws IOException
    {
        deferredActions.set(new ArrayList<>());

        TsvFormatHeader format = readFormat(aIn);
        TsvSchema schema = readSchema(aIn, aJCas);

        // Read the extra blank line after the schema declaration
        String emptyLine = aIn.readLine();
        assert isEmpty(emptyLine);

        TsvDocument doc = new TsvDocument(format, schema, aJCas);

        for (TsvColumn column : schema.getColumns()) {
            doc.activateColumn(column);
            doc.activateType(column.uimaType);
        }

        readContent(aIn, doc);

        // Complete the addition of the chains
        CAS cas = aJCas.getCas();
        for (TsvChain chain : doc.getChains()) {
            if (chain.getElements().isEmpty()) {
                continue;
            }

            Iterator<AnnotationFS> linkIterator = chain.getElements().iterator();
            AnnotationFS link = linkIterator.next();

            // Create the chain head
            FeatureStructure head = cas.createFS(chain.getHeadType());
            setFeature(head, CHAIN_FIRST_FEAT, link);
            cas.addFsToIndexes(head);

            // Connect the links to each other
            AnnotationFS prevLink = link;
            while (linkIterator.hasNext()) {
                link = linkIterator.next();
                setFeature(prevLink, CHAIN_NEXT_FEAT, link);
                prevLink = link;
            }
        }

        // Run deferred actions
        for (Runnable action : deferredActions.get()) {
            action.run();
        }
    }

    private TsvFormatHeader readFormat(LineNumberReader aIn) throws IOException
    {
        String line = aIn.readLine();

        expectStartsWith(line, HEADER_PREFIX_FORMAT);

        Matcher m = FORMAT_PATTERN.matcher(line);
        if (!m.matches()) {
            throw new IOException("Illlegal format header: [" + line + "]");
        }

        TsvFormatHeader format = new TsvFormatHeader(m.group("NAME"), m.group("VERSION"));
        return format;
    }

    private TsvSchema readSchema(LineNumberReader aIn, JCas aJCas) throws IOException
    {
        TsvSchema schema = new TsvSchema();
        int columnIndex = 0;

        // Read first line
        for (String line = aIn.readLine(); !isBlank(line); line = aIn.readLine()) {
            LayerType layerType;

            // Determine layer type
            if (startsWith(line, HEADER_PREFIX_SPAN_LAYER)) {
                layerType = SPAN;
            }
            else if (startsWith(line, HEADER_PREFIX_RELATION_LAYER)) {
                layerType = RELATION;
            }
            else if (startsWith(line, HEADER_PREFIX_CHAIN_LAYER)) {
                layerType = CHAIN;
            }
            else {
                // End of header
                break;
            }

            // Split up layer declaration
            String rest = substringAfter(line, HEADER_LAYER_PREFIX_SEPARATOR);
            String[] fields = split(rest, HEADER_FIELD_SEPARATOR);

            // Get the type name and the corresponding UIMA type from the type system of the
            // target CAS
            String typeName = fields[0];
            Type uimaType = aJCas.getTypeSystem().getType(typeName);
            if (uimaType == null) {
                throw new IOException(
                        "CAS type system does not contain a type named [" + typeName + "]");
            }

            // Parse the column declarations starting at the second field (the first is the
            // type name)
            TsvColumn prevColumn = null;
            for (int i = 1; i < fields.length; i++) {
                String colDecl = fields[i];
                TsvColumn col = parseColumnDeclaration(aJCas, layerType, uimaType, columnIndex,
                        colDecl, prevColumn);
                schema.addColumn(col);
                columnIndex++;
                prevColumn = col;
            }

            // If there is no second field, then add a placeholder column
            if (fields.length == 1) {
                schema.addColumn(new TsvColumn(columnIndex, uimaType, layerType));
                columnIndex++;
            }
        }

        return schema;
    }

    private TsvColumn parseColumnDeclaration(JCas aJCas, LayerType aLayerType, Type aUimaType,
            int aIndex, String aColDecl, TsvColumn aPrevCol)
        throws IOException
    {
        TypeSystem ts = aJCas.getTypeSystem();
        TsvColumn column;
        // Determine the feature type:
        // SLOT_ROLE - starts with "ROLE_"
        if (SPAN.equals(aLayerType) && startsWith(aColDecl, HEADER_PREFIX_ROLE)) {
            String[] subFields = splitPreserveAllTokens(aColDecl, '_');
            String featureName = substringAfter(subFields[1], ":");

            Feature feat = aUimaType.getFeatureByBaseName(featureName);
            if (feat == null) {
                throw new IOException("CAS type [" + aUimaType.getName()
                        + "] does not have a feature called [" + featureName + "]");
            }

            column = new TsvColumn(aIndex, aUimaType, aLayerType, featureName, SLOT_ROLE);

            String typeName = subFields[2];
            Type type = ts.getType(typeName);
            if (type == null) {
                throw new IOException("CAS does not contain a type called [" + typeName + "]");
            }

            column.setTargetTypeHint(type);
        }
        // RELATION_REF - starts with "BT_
        else if (RELATION.equals(aLayerType) && startsWith(aColDecl, HEADER_PREFIX_BASE_TYPE)) {
            column = new TsvColumn(aIndex, aUimaType, aLayerType, FEAT_REL_SOURCE, RELATION_REF);

            String typeName = substringAfter(aColDecl, HEADER_PREFIX_BASE_TYPE);
            Type type = ts.getType(typeName);
            if (type == null) {
                throw new IOException("CAS does not contain a type called [" + typeName + "]");
            }

            column.setTargetTypeHint(type);
        }
        // CHAIN_ELEMENT_TYPE - "referenceType"
        else if (CHAIN.equals(aLayerType) && COREFERENCE_TYPE_FEATURE.equals(aColDecl)) {
            column = new TsvColumn(aIndex, aUimaType, aLayerType, COREFERENCE_TYPE_FEATURE,
                    CHAIN_ELEMENT_TYPE);
        }
        // CHAIN_LINK_TYPE - "referenceRelation"
        else if (CHAIN.equals(aLayerType) && COREFERENCE_RELATION_FEATURE.equals(aColDecl)) {
            column = new TsvColumn(aIndex, aUimaType, aLayerType, COREFERENCE_RELATION_FEATURE,
                    CHAIN_LINK_TYPE);
        }
        // SLOT_TARGET - name of the link target type
        else if (SPAN.equals(aLayerType) && aColDecl.contains(".")
                || ts.getType(aColDecl) != null) {
            // In case we got here because the column declaration contains a dot, let's check if
            // the type name really exists in the target CAS.
            if (ts.getType(aColDecl) == null) {
                throw new IOException(
                        "CAS type system does not contain a type named [" + aColDecl + "]");
            }

            // The previous column must be a SLOT_ROLE because we need to obtain the feature
            // name from it.
            if (aPrevCol == null || !SLOT_ROLE.equals(aPrevCol.featureType)) {
                throw new IOException(
                        "Slot target column declaration must follow slot role column declaration");
            }

            column = new TsvColumn(aIndex, aUimaType, aLayerType,
                    aPrevCol.uimaFeature.getShortName(), SLOT_TARGET);

            Type type = ts.getType(aColDecl);
            if (type == null) {
                throw new IOException("CAS does not contain a type called [" + aColDecl + "]");
            }

            column.setTargetTypeHint(type);
        }
        // PRIMITIVE - feature name
        else if (aUimaType.getFeatureByBaseName(aColDecl) != null) {
            column = new TsvColumn(aIndex, aUimaType, aLayerType, aColDecl, PRIMITIVE);
        }
        else {
            throw new IOException("Type [" + aUimaType.getName()
                    + "] does not contain a feature called [" + aColDecl + "]");
        }
        // PLACEHOLDER - empty column declaration, i.e. only a separator after type name
        // This is not handled here, but rather in the calling method.

        return column;
    }

    private void readContent(LineNumberReader aIn, TsvDocument aDoc) throws IOException
    {
        StringBuilder text = new StringBuilder();

        Tsv3XParserState prevState = INTER_SENTENCE_SPACE;
        Tsv3XParserState state = INTER_SENTENCE_SPACE;

        StringBuilder sentenceText = new StringBuilder();
        String sentenceId = null;
        TsvSentence sentence = null;
        TsvToken token = null;

        List<TsvColumn> headerColumns = aDoc.getSchema()
                .getHeaderColumns(aDoc.getSchema().getColumns());

        int lineNo = 1;
        String line = aIn.readLine();
        try {
            while (!Tsv3XParserState.END.equals(state)) {
                // These variables are only used in TOKEN and SUBTOKEN states.
                String[] fields = null;
                String id = null;
                String[] offsets = null;
                int begin = -1;
                int end = -1;

                // Determine the status of the current line
                if ((state == INTER_SENTENCE_SPACE || state == SENTENCE_HEADER)
                        && startsWith(line, PREFIX_SENTENCE_HEADER)) {
                    state = SENTENCE_HEADER;
                }
                else if (line == null) {
                    state = Tsv3XParserState.END;
                }
                else if (isEmpty(line)) {
                    state = INTER_SENTENCE_SPACE;
                }
                else {
                    fields = splitPreserveAllTokens(line, FIELD_SEPARATOR);
                    int expectedFieldCount = headerColumns.size() + 3;
                    if (fields.length < expectedFieldCount) {
                        throw new IOException("Unable to parse line [" + lineNo + "] as [" + state
                                + "]: [" + line + "] - expected [" + expectedFieldCount
                                + "] fields but only found [" + fields.length + "]");
                    }

                    // Get token metadata
                    id = fields[0];
                    offsets = split(fields[1], "-");
                    begin = Integer.valueOf(offsets[0]);
                    end = Integer.valueOf(offsets[1]);

                    // TOKEN or SUBTOKEN?
                    if (id.contains(".")) {
                        state = SUBTOKEN;
                    }
                    else {
                        state = TOKEN;
                    }
                }

                // Assert that the order of information in the file is correct
                switch (prevState) {
                case INTER_SENTENCE_SPACE:
                    if (!SENTENCE_HEADER.equals(state)) {
                        throw new IOException("Line " + aIn.getLineNumber()
                                + ": Expected sentence header but got [" + state + "]");
                    }
                    break;
                case SENTENCE_HEADER:
                    if (!(SENTENCE_HEADER.equals(state) || TOKEN.equals(state))) {
                        throw new IOException("Line " + aIn.getLineNumber()
                                + ": Expected sentence header or token but got [" + state + "]");
                    }
                    break;
                case TOKEN:
                case SUBTOKEN:
                    if (!(INTER_SENTENCE_SPACE.equals(state) || END.equals(state)
                            || TOKEN.equals(state) || SUBTOKEN.equals(state))) {
                        throw new IOException("Line " + aIn.getLineNumber()
                                + ": Expected token, sub-token or sentence break but got [" + state
                                + "]");
                    }
                    break;
                }

                // Do the actual parsing
                switch (state) {
                case END:
                case INTER_SENTENCE_SPACE:
                    // End of sentence action
                    // The -1 here is to account for the tailing line break
                    sentence.getUimaSentence().setEnd(text.length() - 1);
                    sentence.getUimaSentence().addToIndexes();
                    sentence = null;
                    break;
                case TOKEN:
                    // Note that the token value is not used here. When we get here, we have already
                    // added the complete sentence text to the text buffer.

                    // End of sentence header action
                    if (SENTENCE_HEADER.equals(prevState)) {
                        // If there is no space between the previous sentence and the current
                        // sentence, then we have to strip off the trailing line break from the
                        // last sentence!
                        if (text.length() > begin) {
                            assert text.length() == begin + 1;
                            assert text.charAt(text.length() - 1) == LINE_BREAK;
                            text.setLength(text.length() - 1);
                        }

                        // If there is a gap between the current end of the text buffer and the
                        // offset of the first token in this sentence, then add whitespace to fill
                        // the gap.
                        if (text.length() < begin) {
                            text.append(repeat(' ', begin - text.length()));
                        }

                        assert text.length() == begin;
                        assert sentence == null;

                        Sentence uimaSentence = new Sentence(aDoc.getJCas());
                        if (isNotBlank(sentenceId)) {
                            uimaSentence.setId(sentenceId);
                        }
                        uimaSentence.setBegin(text.length());
                        sentence = aDoc.createSentence(uimaSentence);
                        text.append(sentenceText);
                        sentenceText.setLength(0);
                    }

                    // Token parsing action
                    Token uimaToken = new Token(aDoc.getJCas(), begin, end);
                    uimaToken.addToIndexes();
                    token = sentence.createToken(uimaToken);

                    // Read annotations from the columns
                    parseAnnotations(aDoc, sentence, token, fields, headerColumns);
                    break;
                case SUBTOKEN:
                    // Read annotations from the columns
                    TsvSubToken subToken = token.createSubToken(begin, end);
                    parseAnnotations(aDoc, sentence, subToken, fields, headerColumns);
                    break;
                case SENTENCE_HEADER:
                    // Header parsing action
                    if (line.startsWith(PREFIX_SENTENCE_ID)) {
                        sentenceId = substringAfter(line, "=");
                        sentenceId = unescapeText(aDoc.getFormatHeader(), sentenceId);
                    }
                    if (line.startsWith(PREFIX_TEXT)) {
                        String textFragment = substringAfter(line, "=");
                        textFragment = unescapeText(aDoc.getFormatHeader(), textFragment);
                        sentenceText.append(textFragment);
                        sentenceText.append(LINE_BREAK);
                    }
                    break;
                }

                prevState = state;
                line = aIn.readLine();
                lineNo++;
            }

            aDoc.getJCas().setDocumentText(text.toString());

            // After all data has been read, we also add the annotations with disambiguation ID to
            // the CAS indexes. This ensures we only add them after their final begin/end offsets
            // have been determined since most of these annotations are actually multi-token
            // annotations.
            CAS cas = aDoc.getJCas().getCas();
            Set<FeatureStructure> fses = new LinkedHashSet<>();
            for (TsvSentence s : aDoc.getSentences()) {
                for (TsvToken t : s.getTokens()) {
                    for (Type type : t.getUimaTypes()) {
                        fses.addAll(t.getUimaAnnotations(type));
                    }
                    for (TsvSubToken st : t.getSubTokens()) {
                        for (Type type : st.getUimaTypes()) {
                            fses.addAll(st.getUimaAnnotations(type));
                        }
                    }
                }
            }
            fses.forEach(cas::addFsToIndexes);
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(
                    "Unable to parse line [" + lineNo + "] as [" + state + "]: [" + line + "]", e);
        }
    }

    private void parseAnnotations(TsvDocument aDoc, TsvSentence aSentence, TsvUnit aUnit,
            String[] aFields, List<TsvColumn> aHeaderColumns)
    {
        for (TsvColumn col : aHeaderColumns) {
            String rawValue = aFields[col.index + 3];

            if (NULL_COLUMN.equals(rawValue)) {
                continue;
            }

            String[] stackedValues = STACK_SEP_PATTERN.split(rawValue);

            int index = 0;
            for (String val : stackedValues) {
                parseAnnotation(aDoc, aSentence, aUnit, col, index, val);
                index++;
            }
        }
    }

    /**
     * @param aDoc
     *            the TSV document.
     * @param aSentence
     *            the current sentence.
     * @param aUnit
     *            the current unit (token or subtoken).
     * @param aCol
     *            the column definition.
     * @param aStackingIndex
     *            the stack index within the column in case there are multiple stacked annotations
     *            (0-based).
     * @param aValue
     *            the value.
     */
    private void parseAnnotation(TsvDocument aDoc, TsvSentence aSentence, TsvUnit aUnit,
            TsvColumn aCol, int aStackingIndex, String aValue)
    {
        // Make a copy of the value argument since we may be modifying it below.
        String value = aValue;

        // Extract disambiguation/chain suffix if it exists.
        // If it is a slot column, skip this step because disambiguation info is provided per
        // slot value.
        String disambiguationInfo = null;
        if (!(SLOT_TARGET.equals(aCol.featureType))) {
            if (aValue.endsWith("]") && !aValue.endsWith("\\]")) {
                String buf = substringAfterLast(value, "[");
                disambiguationInfo = substringBefore(buf, "]");
                value = substringBeforeLast(value, "[");
            }
            else {
                Matcher m = CHAIN_SUFFIX_PATTERN.matcher(value);
                if (m.matches()) {
                    disambiguationInfo = m.group("CHAIN");
                    value = value.substring(0, m.start("CHAIN") - 2);
                }
            }
        }

        assert disambiguationInfo == null || disambiguationInfo.length() > 0;

        // Create the annotation of fetch an existing one
        AnnotationFS annotation;
        switch (aCol.layerType) {
        case SPAN:
            annotation = getOrCreateSpanAnnotation(aCol, aUnit, aStackingIndex, disambiguationInfo);
            break;
        case RELATION:
            annotation = getOrCreateRelationAnnotation(aCol, aUnit, aStackingIndex,
                    disambiguationInfo);
            break;
        case CHAIN:
            annotation = getOrCreateChainAnnotation(aCol, aUnit, aStackingIndex,
                    disambiguationInfo);
            break;
        default:
            throw new IllegalStateException("Unknown layer type [" + aCol.layerType + "]");
        }

        // Set feature values including references such as relation source/target or slot targets.
        setFeatures(aCol, aUnit, annotation, disambiguationInfo, aStackingIndex, value);
    }

    private AnnotationFS getOrCreateSpanAnnotation(TsvColumn aCol, TsvUnit aUnit,
            int aStackingIndex, String aDisambiguationInfo)
    {
        int disambiguationId = aDisambiguationInfo != null ? Integer.valueOf(aDisambiguationInfo)
                : -1;

        // Check if we have seen the same annotation already in the current unit but in another
        // column.
        AnnotationFS annotation = aUnit.getUimaAnnotation(aCol.uimaType, aStackingIndex);
        // If not, check if we have seen the same annotation already in a previous unit
        if (annotation == null && disambiguationId != -1) {
            annotation = aUnit.getDocument().getDisambiguatedAnnotation(disambiguationId);
            if (annotation != null) {
                aUnit.addUimaAnnotation(annotation);

                // Extend the span of the existing annotation
                // Unfortunately, the AnnotationFS interface does not define a setEnd() method.
                setFeature(annotation, CAS.FEATURE_BASE_NAME_END, aUnit.getEnd());
            }
        }

        // Still no annotation? Then we have to create one
        if (annotation == null) {
            annotation = aUnit.getDocument().getJCas().getCas().createAnnotation(aCol.uimaType,
                    aUnit.getBegin(), aUnit.getEnd());
            aUnit.addUimaAnnotation(annotation);

            // Check if there are slot features that need to be initialized
            List<TsvColumn> otherColumnsForType = aUnit.getDocument().getSchema()
                    .getColumns(aCol.uimaType);
            for (TsvColumn col : otherColumnsForType) {
                if (SLOT_TARGET.equals(col.featureType)) {
                    setFeature(annotation, col.uimaFeature.getShortName(), emptyList());
                }
            }

            // Special handling of DKPro Core Token-attached annotations
            if (Lemma.class.getName().equals(aCol.uimaType.getName())) {
                TsvToken token = (TsvToken) aUnit;
                token.getUimaToken().setLemma((Lemma) annotation);
            }
            if (Stem.class.getName().equals(aCol.uimaType.getName())) {
                TsvToken token = (TsvToken) aUnit;
                token.getUimaToken().setStem((Stem) annotation);
            }
            if (MorphologicalFeatures.class.getName().equals(aCol.uimaType.getName())) {
                TsvToken token = (TsvToken) aUnit;
                token.getUimaToken().setMorph((MorphologicalFeatures) annotation);
            }
            if (POS.class.getName().equals(aCol.uimaType.getName())) {
                TsvToken token = (TsvToken) aUnit;
                token.getUimaToken().setPos((POS) annotation);
            }
        }

        // If the current annotation carries an disambiguation ID, then register it in the
        // document so we can look up the annotation via its ID later. This is necessary
        // to extend the range of multi-token IDs.
        if (disambiguationId != -1) {
            aUnit.getDocument().addDisambiguationId(annotation, disambiguationId);
        }

        return annotation;
    }

    private AnnotationFS getOrCreateRelationAnnotation(TsvColumn aCol, TsvUnit aUnit,
            int aStackingIndex, String aDisambiguationInfo)
    {
        // Check if we have seen the same annotation already in the current unit but in another
        // column.
        AnnotationFS annotation = aUnit.getUimaAnnotation(aCol.uimaType, aStackingIndex);

        // If not, then we have to create one
        if (annotation == null) {
            annotation = aUnit.getDocument().getJCas().getCas().createAnnotation(aCol.uimaType, -1,
                    -1);
            aUnit.addUimaAnnotation(annotation);
        }

        return annotation;
    }

    private AnnotationFS getOrCreateChainAnnotation(TsvColumn aCol, TsvUnit aUnit,
            int aStackingIndex, String aDisambiguationInfo)
    {
        AnnotationFS annotation;

        // Check if we have seen the same annotation already in the current unit but in
        // another column.
        annotation = aUnit.getUimaAnnotation(aCol.uimaType, aStackingIndex);

        if (annotation == null && CHAIN_LINK_TYPE.equals(aCol.featureType)) {
            // Check if there is already an element with the same index/chain ID
            // No disambiguation info, only chain info: *-><chainId>-<elementIndex>
            String[] ids = split(aDisambiguationInfo, "-");
            int chainId = Integer.valueOf(ids[0]);
            int elementIndex = Integer.valueOf(ids[1]);
            annotation = aUnit.getDocument().getChainElement(chainId, elementIndex);

            if (annotation != null) {
                aUnit.addUimaAnnotation(annotation);

                // Extend the span of the existing annotation
                // Unfortunately, the AnnotationFS interface does not define a setEnd() method.
                setFeature(annotation, CAS.FEATURE_BASE_NAME_END, aUnit.getEnd());
            }

            // If not, then we have to create one - we do this only for link-type columns because
            // these columns include the chain id and the element index which we both need to
            // determine if there is already an existing annotation for this chain/element from
            // an earlier unit (i.e. for multi-unit chain elements).
            if (annotation == null) {
                annotation = aUnit.getDocument().getJCas().getCas().createAnnotation(aCol.uimaType,
                        aUnit.getBegin(), aUnit.getEnd());
                aUnit.addUimaAnnotation(annotation);
            }
        }

        return annotation;
    }

    private void setFeatures(TsvColumn aCol, TsvUnit aUnit, AnnotationFS aAnnotation,
            String aDisambiguationInfo, int aStackingIndex, String aValue)
    {
        // Set the feature value on the annotation
        switch (aCol.featureType) {
        case PLACEHOLDER:
            // Nothing to do!
            break;
        case CHAIN_LINK_TYPE: {
            // No disambiguation info, only chain info: *-><chainId>-<elementIndex>
            String[] ids = split(aDisambiguationInfo, "-");
            int chainId = Integer.valueOf(ids[0]);
            int elementIndex = Integer.valueOf(ids[1]);
            TsvChain chain = aUnit.getDocument().getChain(chainId);
            if (chain == null) {
                // Guess the head type using naming conventions.
                String headTypeName = removeEnd(aCol.uimaType.getName(), "Link");
                headTypeName += "Chain";

                Type headType = aUnit.getDocument().getJCas().getTypeSystem().getType(headTypeName);
                if (headType == null) {
                    throw new IllegalStateException(
                            "CAS type system does not contain a type named [" + headTypeName + "]");
                }

                chain = aUnit.getDocument().createChain(chainId, headType, aCol.uimaType);
            }

            chain.putElement(elementIndex, aAnnotation);
            // fall-through (to set the relation type)
        }
        case CHAIN_ELEMENT_TYPE: {
            deferredActions.get().add(() -> {
                // We need to do this later because first we need to wait until all the elements
                // have been created from the link-type columns. Then we have to look the
                // annotations up via their unit/stacking index.
                AnnotationFS annotation = aUnit.getUimaAnnotation(aCol.uimaType, aStackingIndex);
                setPrimitiveValue(aCol, annotation, aValue);
            });
            break;
        }
        case PRIMITIVE: {
            setPrimitiveValue(aCol, aAnnotation, aValue);
            break;
        }
        case RELATION_REF: {
            // Two disambiguation IDs in brackets after annotation value, e.g.: 1-1[0_2]
            final int sourceDisambiguationId;
            final int targetDisambiguationId;
            if (aDisambiguationInfo != null) {
                String[] ids = split(aDisambiguationInfo, "_");
                sourceDisambiguationId = Integer.valueOf(ids[0]);
                targetDisambiguationId = Integer.valueOf(ids[1]);
            }
            else {
                sourceDisambiguationId = -1;
                targetDisambiguationId = -1;
            }

            // We cannot set the source and target features set because we may not yet have
            // created the relevant annotations. So we defer setting these values until all
            // annotations have been created.
            deferredActions.get().add(() -> {
                Type attachType = aCol.getTargetTypeHint();

                // COMPATIBILITY NOTE:
                // WebAnnoTsv3Writer hard-changes the target type for DKPro Core
                // Dependency annotations from Token to POS - the reason is not really
                // clear. Probably because the Dependency relations in the WebAnno UI
                // attach to POS (Token's are not visible as annotations in the UI).
                if (aCol.uimaType.getName().equals(Dependency.class.getName())) {
                    attachType = aUnit.getDocument().getJCas().getTypeSystem()
                            .getType(Token.class.getName());
                }

                AnnotationFS sourceAnnotation = aUnit.getDocument().resolveReference(attachType,
                        aValue, sourceDisambiguationId);

                AnnotationFS targetAnnotation = aUnit.getDocument().resolveReference(attachType,
                        aUnit.getId(), targetDisambiguationId);

                assert sourceAnnotation != null;
                assert targetAnnotation != null;

                setFeature(aAnnotation, FEAT_REL_SOURCE, sourceAnnotation);
                setFeature(aAnnotation, FEAT_REL_TARGET, targetAnnotation);
                aAnnotation.setBegin(targetAnnotation.getBegin());
                aAnnotation.setEnd(targetAnnotation.getEnd());
            });
            break;
        }
        case SLOT_ROLE: {
            CAS cas = aUnit.getDocument().getJCas().getCas();
            List<FeatureStructure> links = new ArrayList<>();
            if (!NULL_COLUMN.equals(aValue)) {
                String[] values = SLOT_SEP_PATTERN.split(aValue);
                for (String value : values) {
                    FeatureStructure linkFS = cas.createFS(aCol.getTargetTypeHint());
                    if (!NULL_VALUE.equals(value)) {
                        String role = Escaping.unescapeValue(value);
                        setFeature(linkFS, FEAT_SLOT_ROLE, role);
                    }
                    // We index the link features here already so we do not have to track them
                    // down later. They do not have offsets and no other index-relevant features
                    // anyway.
                    cas.addFsToIndexes(linkFS);
                    links.add(linkFS);
                }
            }
            setFeature(aAnnotation, aCol.uimaFeature.getShortName(), links);
            break;
        }
        case SLOT_TARGET: {
            // Setting the target feature has to be deferred until we have created all the
            // annotations.
            deferredActions.get().add(() -> {
                String[] values;
                if (NULL_COLUMN.equals(aValue)) {
                    values = new String[0];
                }
                else {
                    values = SLOT_SEP_PATTERN.split(aValue);
                }

                FeatureStructure[] links = getFeature(aAnnotation, aCol.uimaFeature.getShortName(),
                        FeatureStructure[].class);

                assert (links.length == 0 && values.length == 1 && NULL_VALUE.equals(values[0]))
                        || (values.length == links.length);

                for (int i = 0; i < values.length; i++) {
                    String value = values[i];

                    if (NULL_VALUE.equals(value) || NULL_COLUMN.equals(value)) {
                        continue;
                    }

                    // Extract slot-local disambiguation info
                    int disambiguationId = -1;
                    if (value.endsWith("]") && !value.endsWith("\\]")) {
                        String disambiguationInfo = substringAfterLast(value, "[");
                        disambiguationId = Integer
                                .valueOf(substringBefore(disambiguationInfo, "]"));
                        value = substringBeforeLast(value, "[");
                    }

                    AnnotationFS targetAnnotation = aUnit.getDocument()
                            .resolveReference(aCol.getTargetTypeHint(), value, disambiguationId);

                    setFeature(links[i], FEAT_SLOT_TARGET, targetAnnotation);
                }
            });
            break;
        }
        }
    }

    private void setPrimitiveValue(TsvColumn aCol, AnnotationFS aAnnotation, String aValue)
    {
        // Unescape value - this needs to be done after extracting the disambiguation ID and
        // after determining whether the values is a null value.
        if (!NULL_VALUE.equals(aValue)) {
            String value = Escaping.unescapeValue(aValue);
            Feature feat = aAnnotation.getType()
                    .getFeatureByBaseName(aCol.uimaFeature.getShortName());

            if (feat == null) {
                throw new IllegalArgumentException(
                        "CAS type [" + aAnnotation.getType() + "] does not have a feature called ["
                                + aCol.uimaFeature.getShortName() + "]");
            }

            aAnnotation.setFeatureValueFromString(feat, value);
        }
    }

    private void expectStartsWith(String aLine, String aPrefix) throws IOException
    {
        if (!startsWith(aLine, aPrefix)) {
            throw new IOException(
                    "Line does not start with expected prefix [" + aPrefix + "]: [" + aLine + "]");
        }
    }
}
