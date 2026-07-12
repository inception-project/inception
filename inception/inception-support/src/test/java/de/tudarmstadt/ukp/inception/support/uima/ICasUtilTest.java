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
package de.tudarmstadt.ukp.inception.support.uima;

import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.findAllFeatureStructures;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.forceOverwriteSofa;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getAddr;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getDefaultFeatureValue;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getFeatureFS;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getFeatureValue;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.getUimaTypeName;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.hasSameType;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.isPrimitive;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.setFeature;
import static org.apache.uima.fit.factory.CasFactory.createCas;
import static org.apache.uima.fit.factory.JCasFactory.createText;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class ICasUtilTest
{
    private static final String CUSTOM_TYPE = "custom.Values";

    /**
     * A CAS with a custom type {@code custom.Values} carrying one feature per primitive range plus
     * an FS-valued feature named {@code ref}.
     */
    private CAS makeCasWithCustomType() throws Exception
    {
        TypeSystemDescription tsd = createTypeSystemDescription();
        TypeDescription td = tsd.addType(CUSTOM_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        td.addFeature("stringVal", "", CAS.TYPE_NAME_STRING);
        td.addFeature("boolVal", "", CAS.TYPE_NAME_BOOLEAN);
        td.addFeature("floatVal", "", CAS.TYPE_NAME_FLOAT);
        td.addFeature("intVal", "", CAS.TYPE_NAME_INTEGER);
        td.addFeature("byteVal", "", CAS.TYPE_NAME_BYTE);
        td.addFeature("doubleVal", "", CAS.TYPE_NAME_DOUBLE);
        td.addFeature("longVal", "", CAS.TYPE_NAME_LONG);
        td.addFeature("shortVal", "", CAS.TYPE_NAME_SHORT);
        td.addFeature("stringArray", "", CAS.TYPE_NAME_STRING_ARRAY, CAS.TYPE_NAME_STRING, false);
        td.addFeature("ref", "", CAS.TYPE_NAME_ANNOTATION);

        CAS cas = createCas(tsd);
        cas.setDocumentText("hello world");
        return cas;
    }

    // ---------------------------------------------------------------------------------------------
    // isPrimitive
    // ---------------------------------------------------------------------------------------------

    @Test
    public void thatIsPrimitiveRecognizesPrimitiveTypeNames()
    {
        assertThat(isPrimitive(CAS.TYPE_NAME_BOOLEAN)).isTrue();
        assertThat(isPrimitive(CAS.TYPE_NAME_BYTE)).isTrue();
        assertThat(isPrimitive(CAS.TYPE_NAME_DOUBLE)).isTrue();
        assertThat(isPrimitive(CAS.TYPE_NAME_FLOAT)).isTrue();
        assertThat(isPrimitive(CAS.TYPE_NAME_INTEGER)).isTrue();
        assertThat(isPrimitive(CAS.TYPE_NAME_LONG)).isTrue();
        assertThat(isPrimitive(CAS.TYPE_NAME_SHORT)).isTrue();
        assertThat(isPrimitive(CAS.TYPE_NAME_STRING)).isTrue();
    }

    @Test
    public void thatIsPrimitiveRejectsNonPrimitiveTypeNames()
    {
        assertThat(isPrimitive(null)).isFalse();
        assertThat(isPrimitive(CAS.TYPE_NAME_ANNOTATION)).isFalse();
        assertThat(isPrimitive(CAS.TYPE_NAME_TOP)).isFalse();
        assertThat(isPrimitive(CAS.TYPE_NAME_STRING_ARRAY)).isFalse();
        assertThat(isPrimitive("some.random.Type")).isFalse();
    }

    // ---------------------------------------------------------------------------------------------
    // getFeatureValue
    // ---------------------------------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    @Test
    public void thatGetFeatureValueReadsEachPrimitiveRange() throws Exception
    {
        CAS cas = makeCasWithCustomType();
        Type type = cas.getTypeSystem().getType(CUSTOM_TYPE);
        var fs = cas.createAnnotation(type, 0, 5);

        fs.setStringValue(type.getFeatureByBaseName("stringVal"), "abc");
        fs.setBooleanValue(type.getFeatureByBaseName("boolVal"), true);
        fs.setFloatValue(type.getFeatureByBaseName("floatVal"), 1.5f);
        fs.setIntValue(type.getFeatureByBaseName("intVal"), 42);
        fs.setByteValue(type.getFeatureByBaseName("byteVal"), (byte) 7);
        fs.setDoubleValue(type.getFeatureByBaseName("doubleVal"), 2.5d);
        fs.setLongValue(type.getFeatureByBaseName("longVal"), 99L);
        fs.setShortValue(type.getFeatureByBaseName("shortVal"), (short) 3);

        assertThat(getFeatureValue(fs, type.getFeatureByBaseName("stringVal"))).isEqualTo("abc");
        assertThat(getFeatureValue(fs, type.getFeatureByBaseName("boolVal"))).isEqualTo(true);
        assertThat(getFeatureValue(fs, type.getFeatureByBaseName("floatVal"))).isEqualTo(1.5f);
        assertThat(getFeatureValue(fs, type.getFeatureByBaseName("intVal"))).isEqualTo(42);
        assertThat(getFeatureValue(fs, type.getFeatureByBaseName("byteVal"))).isEqualTo((byte) 7);
        assertThat(getFeatureValue(fs, type.getFeatureByBaseName("doubleVal"))).isEqualTo(2.5d);
        assertThat(getFeatureValue(fs, type.getFeatureByBaseName("longVal"))).isEqualTo(99L);
        assertThat(getFeatureValue(fs, type.getFeatureByBaseName("shortVal"))).isEqualTo((short) 3);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void thatGetFeatureValueReturnsNullForNonPrimitiveRange() throws Exception
    {
        CAS cas = makeCasWithCustomType();
        Type type = cas.getTypeSystem().getType(CUSTOM_TYPE);
        var fs = cas.createAnnotation(type, 0, 5);

        // 'ref' is an FS-valued (non-primitive) feature -> not handled -> null
        assertThat(getFeatureValue(fs, type.getFeatureByBaseName("ref"))).isNull();
    }

    // ---------------------------------------------------------------------------------------------
    // getDefaultFeatureValue
    // ---------------------------------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    @Test
    public void thatGetDefaultFeatureValueMatchesRange() throws Exception
    {
        CAS cas = makeCasWithCustomType();
        Type type = cas.getTypeSystem().getType(CUSTOM_TYPE);

        assertThat(getDefaultFeatureValue(type.getFeatureByBaseName("stringVal"))).isNull();
        assertThat(getDefaultFeatureValue(type.getFeatureByBaseName("boolVal"))).isEqualTo(false);
        assertThat(getDefaultFeatureValue(type.getFeatureByBaseName("floatVal"))).isEqualTo(0.0f);
        assertThat(getDefaultFeatureValue(type.getFeatureByBaseName("intVal"))).isEqualTo(0);
        assertThat(getDefaultFeatureValue(type.getFeatureByBaseName("byteVal")))
                .isEqualTo((byte) 0);
        assertThat(getDefaultFeatureValue(type.getFeatureByBaseName("doubleVal"))).isEqualTo(0.0d);
        assertThat(getDefaultFeatureValue(type.getFeatureByBaseName("longVal"))).isEqualTo(0L);
        assertThat(getDefaultFeatureValue(type.getFeatureByBaseName("shortVal")))
                .isEqualTo((short) 0);
        assertThat(getDefaultFeatureValue(type.getFeatureByBaseName("stringArray")))
                .isEqualTo(Collections.emptySet());
    }

    // ---------------------------------------------------------------------------------------------
    // setFeature
    // ---------------------------------------------------------------------------------------------

    @Test
    public void thatSetFeatureSetsPrimitiveFromString() throws Exception
    {
        CAS cas = makeCasWithCustomType();
        Type type = cas.getTypeSystem().getType(CUSTOM_TYPE);
        var fs = cas.createAnnotation(type, 0, 5);

        setFeature(fs, type.getFeatureByBaseName("intVal"), "123");
        setFeature(fs, type.getFeatureByBaseName("boolVal"), "true");
        setFeature(fs, type.getFeatureByBaseName("stringVal"), "xyz");

        assertThat(fs.getIntValue(type.getFeatureByBaseName("intVal"))).isEqualTo(123);
        assertThat(fs.getBooleanValue(type.getFeatureByBaseName("boolVal"))).isTrue();
        assertThat(fs.getStringValue(type.getFeatureByBaseName("stringVal"))).isEqualTo("xyz");
    }

    @Test
    public void thatSetFeatureSetsStringArray() throws Exception
    {
        CAS cas = makeCasWithCustomType();
        Type type = cas.getTypeSystem().getType(CUSTOM_TYPE);
        var fs = cas.createAnnotation(type, 0, 5);

        setFeature(fs, type.getFeatureByBaseName("stringArray"), "a", "b", "c");

        var value = fs.getFeatureValue(type.getFeatureByBaseName("stringArray"));
        assertThat(value).isNotNull();
        assertThat(((org.apache.uima.jcas.cas.StringArray) value).toStringArray())
                .containsExactly("a", "b", "c");
    }

    // ---------------------------------------------------------------------------------------------
    // getFeatureFS
    // ---------------------------------------------------------------------------------------------

    @Test
    public void thatGetFeatureFSReturnsReferencedFs() throws Exception
    {
        CAS cas = makeCasWithCustomType();
        Type type = cas.getTypeSystem().getType(CUSTOM_TYPE);
        var target = cas.createAnnotation(type, 0, 5);
        var holder = cas.createAnnotation(type, 6, 11);

        holder.setFeatureValue(type.getFeatureByBaseName("ref"), target);

        assertThat(getFeatureFS(holder, "ref")).isSameAs(target);
    }

    @Test
    public void thatGetFeatureFSReturnsNullWhenUnset() throws Exception
    {
        CAS cas = makeCasWithCustomType();
        Type type = cas.getTypeSystem().getType(CUSTOM_TYPE);
        var holder = cas.createAnnotation(type, 0, 5);

        assertThat(getFeatureFS(holder, "ref")).isNull();
    }

    // ---------------------------------------------------------------------------------------------
    // getAddr / selectByAddr round-trip
    // ---------------------------------------------------------------------------------------------

    @Test
    public void thatAddrRoundTripsThroughSelect() throws Exception
    {
        var jcas = createText("one two", "en");
        var token = new Token(jcas, 0, 3);
        token.addToIndexes();

        int addr = getAddr(token);

        assertThat(selectAnnotationByAddr(jcas.getCas(), addr)).isSameAs(token);
        assertThat(selectFsByAddr(jcas.getCas(), addr)).isSameAs(token);
    }

    // ---------------------------------------------------------------------------------------------
    // getUimaTypeName
    // ---------------------------------------------------------------------------------------------

    @Test
    public void thatGetUimaTypeNameMapsBuiltinAndBase()
    {
        // Builtin uima.jcas.* classes map to uima.* type names
        assertThat(getUimaTypeName(Annotation.class)).isEqualTo(CAS.TYPE_NAME_ANNOTATION);
        assertThat(getUimaTypeName(org.apache.uima.jcas.cas.TOP.class))
                .isEqualTo(CAS.TYPE_NAME_TOP);

        // Regular JCas classes keep their fully-qualified class name
        assertThat(getUimaTypeName(Token.class)).isEqualTo(Token.class.getName());
    }

    // ---------------------------------------------------------------------------------------------
    // hasSameType
    // ---------------------------------------------------------------------------------------------

    @Test
    public void thatHasSameTypeReturnsFalseForNullArguments() throws Exception
    {
        var jcas = createText("test", "en");
        var token = new Token(jcas, 0, 4);

        assertThat(hasSameType(null, null)).isFalse();
        assertThat(hasSameType(token, null)).isFalse();
        assertThat(hasSameType(null, token)).isFalse();
    }

    @Test
    public void thatHasSameTypeReturnsTrueForSameInstance() throws Exception
    {
        var jcas = createText("test", "en");
        var token = new Token(jcas, 0, 4);

        assertThat(hasSameType(token, token)).isTrue();
    }

    @Test
    public void thatHasSameTypeReturnsTrueForDistinctAnnotationsOfSameType() throws Exception
    {
        var jcas = createText("one two", "en");
        var token1 = new Token(jcas, 0, 3);
        var token2 = new Token(jcas, 4, 7);

        assertThat(hasSameType(token1, token2)).isTrue();
    }

    @Test
    public void thatHasSameTypeReturnsFalseForColocatedAnnotationsOfDifferentType() throws Exception
    {
        var jcas = createText("test", "en");
        var token = new Token(jcas, 0, 4);
        var sentence = new Sentence(jcas, 0, 4);

        assertThat(hasSameType(token, sentence)).isFalse();
    }

    // ---------------------------------------------------------------------------------------------
    // forceOverwriteSofa
    // ---------------------------------------------------------------------------------------------

    @Test
    public void thatForceOverwriteSofaReplacesText() throws Exception
    {
        var jcas = createText("original text", "en");

        forceOverwriteSofa(jcas.getCas(), "replaced text");

        assertThat(jcas.getDocumentText()).isEqualTo("replaced text");
    }

    // ---------------------------------------------------------------------------------------------
    // findAllFeatureStructures
    // ---------------------------------------------------------------------------------------------

    @Test
    public void thatFindAllFeatureStructuresFindsIndexedAnnotations() throws Exception
    {
        var jcas = createText("one two", "en");
        var token1 = new Token(jcas, 0, 3);
        token1.addToIndexes();
        var token2 = new Token(jcas, 4, 7);
        token2.addToIndexes();

        assertThat(findAllFeatureStructures(jcas.getCas())).contains(token1, token2);
    }
}
