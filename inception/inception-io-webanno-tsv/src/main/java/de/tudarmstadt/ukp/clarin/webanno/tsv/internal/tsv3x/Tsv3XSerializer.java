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

import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Escaping.escapeText;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Escaping.escapeValue;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.RELATION_REF;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.SLOT_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.SLOT_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.FIELD_SEPARATOR;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_FIELD_SEPARATOR;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_PREFIX_BASE_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_PREFIX_CHAIN_LAYER;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_PREFIX_FORMAT;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_PREFIX_RELATION_LAYER;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_PREFIX_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.HEADER_PREFIX_SPAN_LAYER;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.LINE_BREAK;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.NULL_COLUMN;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.NULL_VALUE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.PREFIX_SENTENCE_ID;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.PREFIX_TEXT;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.SLOT_SEP;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FormatConstants.STACK_SEP;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.COREFERENCE_RELATION_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.COREFERENCE_TYPE_FEATURE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_SLOT_TARGET;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.splitPreserveAllTokens;
import static org.apache.uima.fit.util.FSUtil.getFeature;

import java.io.PrintWriter;
import java.util.List;

import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvChain;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvColumn;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvDocument;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvFormatHeader;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSentence;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSubToken;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvToken;
import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvUnit;

public class Tsv3XSerializer
{
    public void write(PrintWriter aOut, TsvDocument aDocument)
    {
        write(aOut, aDocument.getFormatHeader());

        var headerColumns = aDocument.getSchema().getHeaderColumns(aDocument.getActiveColumns());

        write(aOut, headerColumns);

        for (var sentence : aDocument.getSentences()) {
            aOut.print(LINE_BREAK);
            write(aOut, sentence, headerColumns);
        }
    }

    public void write(PrintWriter aOut, TsvFormatHeader aHeader)
    {
        aOut.print(HEADER_PREFIX_FORMAT);
        aOut.printf("%s %s\n", aHeader.getName(), aHeader.getVersion());
    }

    /**
     * Write the schema header.
     * 
     * @param aOut
     *            the writer
     * @param aHeaderColumns
     *            the header columns
     */
    public void write(PrintWriter aOut, List<TsvColumn> aHeaderColumns)
    {
        Type currentType = null;
        for (TsvColumn col : aHeaderColumns) {
            if (currentType == null || !currentType.equals(col.uimaType)) {
                if (currentType != null) {
                    aOut.print(LINE_BREAK);
                }
                currentType = col.uimaType;

                switch (col.layerType) {
                case SPAN:
                    aOut.print(HEADER_PREFIX_SPAN_LAYER);
                    break;
                case RELATION:
                    aOut.print(HEADER_PREFIX_RELATION_LAYER);
                    break;
                case CHAIN:
                    aOut.print(HEADER_PREFIX_CHAIN_LAYER);
                    break;
                }
                aOut.print(col.uimaType.getName());
            }

            if (RELATION_REF.equals(col.featureType)) {
                aOut.print(HEADER_FIELD_SEPARATOR);
                aOut.print(HEADER_PREFIX_BASE_TYPE);
                if (col.getTargetTypeHint() != null) {
                    // COMPATIBILITY NOTE:
                    // WebAnnoTsv3Writer obtains the type of a relation target column not from
                    // the type system definition but rather by looking at target used by the
                    // first actual annotation. This assumes that relations are always only on
                    // a single type.
                    aOut.printf(col.getTargetTypeHint().getName());
                }
                else {
                    aOut.printf(col.uimaFeature.getRange().getName());
                }
            }
            else if (SLOT_TARGET.equals(col.featureType)) {
                // NOTE: This is the same as for the RELATION_REF except that the type
                // name is not prefixed with "BT_" here.

                if (col.getTargetTypeHint() != null) {
                    // COMPATIBILITY NOTE:
                    // WebAnnoTsv3Writer obtains the type of a slot target column not from
                    // the type system definition but rather by looking at target used by the
                    // first actual annotation.
                    aOut.print(HEADER_FIELD_SEPARATOR);
                    aOut.print(col.getTargetTypeHint());
                }
                else {
                    aOut.print(HEADER_FIELD_SEPARATOR);
                    aOut.print(col.uimaFeature.getRange().getName());
                }
            }
            else if (SLOT_ROLE.equals(col.featureType)) {
                aOut.print(HEADER_FIELD_SEPARATOR);
                aOut.print(HEADER_PREFIX_ROLE);
                aOut.printf("%s_%s", col.uimaFeature.getName(),
                        col.uimaFeature.getRange().getComponentType().getName());
            }
            else if (SLOT_TARGET.equals(col.featureType)) {
                aOut.print(HEADER_FIELD_SEPARATOR);
                aOut.print(col.uimaFeature.getRange().getComponentType()
                        .getFeatureByBaseName(FEAT_SLOT_TARGET).getRange().getName());
            }
            else {
                // COMPATIBILITY NOTE:
                // Yes, this pipe symbol needs to be written
                aOut.print("|");
                if (col.uimaFeature != null) {
                    aOut.print(col.uimaFeature.getShortName());
                }
            }
        }

        // Add line-break to terminate the final column definition
        if (!aHeaderColumns.isEmpty()) {
            aOut.print(LINE_BREAK);
        }

        // COMPATIBILITY NOTE:
        // This is really just to make the output match exactly TSV3
        aOut.print(LINE_BREAK);
    }

    public void write(PrintWriter aOut, TsvSentence aSentence, List<TsvColumn> aHeaderColumns)
    {
        String[] lines = splitPreserveAllTokens(aSentence.getUimaSentence().getCoveredText(),
                LINE_BREAK);

        String sentenceId = aSentence.getUimaSentence().getId();
        if (isNotBlank(sentenceId)) {
            aOut.print(PREFIX_SENTENCE_ID);
            aOut.print(escapeText(sentenceId));
            aOut.print(LINE_BREAK);
        }

        for (String line : lines) {
            aOut.print(PREFIX_TEXT);
            aOut.print(escapeText(line));
            aOut.print(LINE_BREAK);
        }

        for (TsvToken token : aSentence.getTokens()) {
            write(aOut, token, aHeaderColumns);
            aOut.write(LINE_BREAK);
            for (TsvSubToken subToken : token.getSubTokens()) {
                write(aOut, subToken, aHeaderColumns);
                aOut.write(LINE_BREAK);
            }
        }
    }

    public void write(PrintWriter aOut, TsvUnit aUnit, List<TsvColumn> aHeaderColumns)
    {
        TsvDocument doc = aUnit.getDocument();

        // Write unit ID
        aOut.print(aUnit.getId());
        aOut.print(FIELD_SEPARATOR);

        // Write unit offset
        aOut.printf("%d-%d", aUnit.getBegin(), aUnit.getEnd());
        aOut.print(FIELD_SEPARATOR);

        // Write unit text
        aOut.print(doc.getJCas().getDocumentText().substring(aUnit.getBegin(), aUnit.getEnd()));
        aOut.printf(FIELD_SEPARATOR);

        // Write the remaining columns according to the schema definition
        for (TsvColumn col : aHeaderColumns) {
            // Write all the values in this column - there could be multiple due to stacking
            writeValues(aOut, aUnit, col);
            aOut.printf(FIELD_SEPARATOR);
        }
    }

    private void writeValues(PrintWriter aOut, TsvUnit aUnit, TsvColumn aCol)
    {
        List<AnnotationFS> columnAnnos = aUnit.getAnnotationsForColumn(aCol);

        // Encode the annotation values for the current column
        if (columnAnnos.isEmpty()) {
            aOut.print(NULL_COLUMN);
        }
        else {
            for (int i = 0; i < columnAnnos.size(); i++) {
                if (i > 0) {
                    aOut.print(STACK_SEP);
                }

                AnnotationFS fs = columnAnnos.get(i);
                writeValue(aOut, aUnit.getDocument(), aCol, fs);
            }
        }
    }

    private void writeValue(PrintWriter aOut, TsvDocument aDoc, TsvColumn aCol, AnnotationFS aFS)
    {
        // What kind of column is it? Depending on the type of column, the annotation value
        // has to be encoded differently.
        switch (aCol.featureType) {
        case PLACEHOLDER: {
            writePlaceholderValue(aOut, aDoc, aCol, aFS);
            writeDisambiguationId(aOut, aDoc, aFS);
            break;
        }
        case PRIMITIVE: {
            writePrimitiveValue(aOut, aDoc, aCol, aFS);
            writeDisambiguationId(aOut, aDoc, aFS);
            break;
        }
        case RELATION_REF: {
            writeRelationReference(aOut, aDoc, aCol, aFS);
            break;
        }
        case SLOT_ROLE: {
            writeSlotRole(aOut, aDoc, aCol, aFS);
            break;
        }
        case SLOT_TARGET: {
            writeSlotTarget(aOut, aDoc, aCol, aFS);
            break;
        }
        case CHAIN_ELEMENT_TYPE:
            writeChainElement(aOut, aDoc, aCol, aFS);
            break;
        case CHAIN_LINK_TYPE:
            writeChainLink(aOut, aDoc, aCol, aFS);
            break;
        default:
            throw new IllegalStateException("Unknown feature type: [" + aCol.featureType + "]");
        }
    }

    private static void writeDisambiguationId(PrintWriter aOut, TsvDocument aDoc, AnnotationFS aFS)
    {
        Integer disambiguationId = aDoc.getDisambiguationId(aFS);
        if (disambiguationId != null) {
            aOut.printf("[%d]", disambiguationId);
        }
    }

    private static void writePlaceholderValue(PrintWriter aOut, TsvDocument aDoc, TsvColumn aCol,
            AnnotationFS aFS)
    {
        aOut.print(NULL_VALUE);
    }

    private static void writePrimitiveValue(PrintWriter aOut, TsvDocument aDoc, TsvColumn aCol,
            AnnotationFS aFS)
    {
        Object value = getFeature(aFS, aCol.uimaFeature, Object.class);
        value = value == null ? NULL_VALUE : escapeValue(String.valueOf(value));
        aOut.print(value);
    }

    private static void writeRelationReference(PrintWriter aOut, TsvDocument aDoc, TsvColumn aCol,
            AnnotationFS aFS)
    {
        AnnotationFS targetFS = getFeature(aFS, FEAT_REL_TARGET, AnnotationFS.class);
        AnnotationFS sourceFS = getFeature(aFS, FEAT_REL_SOURCE, AnnotationFS.class);

        // The column contains the ID of the unit from which the relation is pointing to the
        // current unit, i.e. the sourceUnit of the relation.
        TsvUnit sourceUnit = aDoc.findIdDefiningUnit(sourceFS);
        aOut.print(sourceUnit.getId());

        // If the source/target is ambiguous, add the disambiguation IDs
        Integer sourceId = aDoc.getDisambiguationId(sourceFS);
        Integer targetId = aDoc.getDisambiguationId(targetFS);
        if (sourceId != null || targetId != null) {
            sourceId = sourceId != null ? sourceId : 0;
            targetId = targetId != null ? targetId : 0;
            aOut.printf("[%d_%d]", sourceId, targetId);
        }
    }

    private static void writeSlotRole(PrintWriter aOut, TsvDocument aDoc, TsvColumn aCol,
            AnnotationFS aFS)
    {
        FeatureStructure[] links = getFeature(aFS, aCol.uimaFeature, FeatureStructure[].class);
        if (links != null && links.length > 0) {
            for (int i = 0; i < links.length; i++) {
                if (i > 0) {
                    aOut.print(SLOT_SEP);
                }
                String value = getFeature(links[i], TsvSchema.FEAT_SLOT_ROLE, String.class);
                value = value == null ? NULL_VALUE : escapeValue(value);
                aOut.print(value);
            }
        }
        else {
            aOut.print(NULL_COLUMN);
        }
        writeDisambiguationId(aOut, aDoc, aFS);
    }

    private static void writeSlotTarget(PrintWriter aOut, TsvDocument aDoc, TsvColumn aCol,
            AnnotationFS aFS)
    {
        FeatureStructure[] links = getFeature(aFS, aCol.uimaFeature, FeatureStructure[].class);
        if (links != null && links.length > 0) {
            for (int i = 0; i < links.length; i++) {
                if (i > 0) {
                    aOut.print(SLOT_SEP);
                }
                AnnotationFS targetFS = getFeature(links[i], TsvSchema.FEAT_SLOT_TARGET,
                        AnnotationFS.class);
                if (targetFS == null) {
                    throw new IllegalStateException("Slot link has no target: " + links[i]);
                }

                TsvUnit target = aDoc.findIdDefiningUnit(targetFS);
                if (target == null) {
                    throw new IllegalStateException(
                            "Unable to find ID-defining unit for annotation: " + targetFS);
                }

                aOut.print(target.getId());
                writeDisambiguationId(aOut, aDoc, targetFS);
            }
        }
        else {
            // If the slot hosts has no slots, we use this column as a placeholder so we know
            // the span of the slot host
            aOut.print(NULL_VALUE);
        }
    }

    private static void writeChainElement(PrintWriter aOut, TsvDocument aDoc, TsvColumn aCol,
            AnnotationFS aFS)
    {
        String value = getFeature(aFS, COREFERENCE_TYPE_FEATURE, String.class);
        value = value == null ? NULL_VALUE : escapeValue(value);

        TsvChain chain = aDoc.getChain(aFS);

        aOut.printf("%s[%d]", value, chain.getId());
    }

    private static void writeChainLink(PrintWriter aOut, TsvDocument aDoc, TsvColumn aCol,
            AnnotationFS aFS)
    {
        String value = getFeature(aFS, COREFERENCE_RELATION_FEATURE, String.class);
        value = value == null ? NULL_VALUE : escapeValue(value);

        TsvChain chain = aDoc.getChain(aFS);

        aOut.printf("%s->%d-%d", value, chain.getId(), chain.indexOf(aFS) + 1);
    }
}
