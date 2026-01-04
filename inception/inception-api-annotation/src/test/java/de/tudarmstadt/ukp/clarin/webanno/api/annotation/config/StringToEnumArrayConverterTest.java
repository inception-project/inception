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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static wicket.contrib.input.events.key.KeyType.Ctrl;
import static wicket.contrib.input.events.key.KeyType.Delete;
import static wicket.contrib.input.events.key.KeyType.Page_down;
import static wicket.contrib.input.events.key.KeyType.Shift;
import static wicket.contrib.input.events.key.KeyType.z;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.TypeDescriptor;

import wicket.contrib.input.events.key.KeyType;

class StringToEnumArrayConverterTest
{
    private StringToEnumArrayConverter converter;
    private TypeDescriptor stringType;
    private TypeDescriptor keyTypeArrayType;

    @BeforeEach
    void setUp()
    {
        converter = new StringToEnumArrayConverter();
        stringType = TypeDescriptor.valueOf(String.class);
        keyTypeArrayType = TypeDescriptor.valueOf(KeyType[].class);
    }

    @Test
    void testMatches_withEnumArray_shouldReturnTrue()
    {
        assertThat(converter.matches(stringType, keyTypeArrayType)).isTrue();
    }

    @Test
    void testMatches_withNonEnumArray_shouldReturnFalse()
    {
        var stringArrayType = TypeDescriptor.valueOf(String[].class);
        assertThat(converter.matches(stringType, stringArrayType)).isFalse();
    }

    @Test
    void testConvert_withNull_shouldReturnNull()
    {
        var result = converter.convert(null, stringType, keyTypeArrayType);
        assertThat(result).isNull();
    }

    @Test
    void testConvert_withEmptyString_shouldReturnEmptyArray()
    {
        var result = (KeyType[]) converter.convert("", stringType, keyTypeArrayType);
        assertThat(result).isEmpty();
    }

    @Test
    void testConvert_withWhitespaceOnly_shouldReturnEmptyArray()
    {
        var result = (KeyType[]) converter.convert("   ", stringType, keyTypeArrayType);
        assertThat(result).isEmpty();
    }

    @Test
    void testConvert_withSingleValue_shouldReturnSingleElementArray()
    {
        var result = (KeyType[]) converter.convert("Page_down", stringType, keyTypeArrayType);
        assertThat(result).containsExactly(Page_down);
    }

    @Test
    void testConvert_withMultipleValues_shouldReturnMultiElementArray()
    {
        var result = (KeyType[]) converter.convert("Ctrl,z", stringType, keyTypeArrayType);
        assertThat(result).containsExactly(Ctrl, z);
    }

    @Test
    void testConvert_withThreeValues_shouldReturnThreeElementArray()
    {
        var result = (KeyType[]) converter.convert("Shift,Ctrl,z", stringType, keyTypeArrayType);
        assertThat(result).containsExactly(Shift, Ctrl, z);
    }

    @Test
    void testConvert_withWhitespaceAroundCommas_shouldTrimAndConvert()
    {
        var result = (KeyType[]) converter.convert("Shift , Ctrl , z", stringType,
                keyTypeArrayType);
        assertThat(result).containsExactly(Shift, Ctrl, z);
    }

    @Test
    void testConvert_withExtraWhitespace_shouldTrimAndConvert()
    {
        var result = (KeyType[]) converter.convert("  Shift  ,  Delete  ", stringType,
                keyTypeArrayType);
        assertThat(result).containsExactly(Shift, Delete);
    }

    @Test
    void testConvert_withInvalidEnumValue_shouldThrowException()
    {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> converter.convert("InvalidKey", stringType, keyTypeArrayType))
                .withMessageContaining("Cannot convert value 'InvalidKey'")
                .withMessageContaining("to enum type KeyType")
                .withMessageContaining("Valid values are:");
    }

    @Test
    void testConvert_withEmptyPartInList_shouldThrowException()
    {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> converter.convert("Ctrl,,z", stringType, keyTypeArrayType))
                .withMessageContaining("Empty value at position 1")
                .withMessageContaining("in comma-separated enum list")
                .withMessageContaining("All parts must be non-empty")
                .withMessageContaining("Input was: 'Ctrl,,z'");
    }

    @Test
    void testConvert_withWhitespaceOnlyPartInList_shouldThrowException()
    {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> converter.convert("Ctrl,   ,z", stringType, keyTypeArrayType))
                .withMessageContaining("Empty value at position 1")
                .withMessageContaining("in comma-separated enum list");
    }

    @Test
    void testConvert_withEmptyPartAtEnd_shouldThrowException()
    {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> converter.convert("Ctrl,z,", stringType, keyTypeArrayType))
                .withMessageContaining("Empty value at position 2")
                .withMessageContaining("in comma-separated enum list");
    }

    @Test
    void testConvert_withEmptyPartAtStart_shouldThrowException()
    {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> converter.convert(",Ctrl,z", stringType, keyTypeArrayType))
                .withMessageContaining("Empty value at position 0")
                .withMessageContaining("in comma-separated enum list");
    }

    @Test
    void testConvert_withMultipleInvalidValues_shouldThrowExceptionForFirst()
    {
        // Should fail on the first invalid value encountered
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(
                        () -> converter.convert("Invalid1,Invalid2", stringType, keyTypeArrayType))
                .withMessageContaining("Cannot convert value 'Invalid1'");
    }

    @Test
    void testConvert_withMixedValidAndInvalid_shouldThrowException()
    {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(
                        () -> converter.convert("Ctrl,InvalidKey,z", stringType, keyTypeArrayType))
                .withMessageContaining("Cannot convert value 'InvalidKey'");
    }

    @Test
    void testConvert_errorMessageIncludesValidValues()
    {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> converter.convert("BadValue", stringType, keyTypeArrayType))
                .withMessageContaining("Valid values are:").withMessageContaining("Ctrl")
                .withMessageContaining("Shift").withMessageContaining("Delete");
    }

    @Test
    void testGetConvertibleTypes_shouldReturnStringToObjectArray()
    {
        var types = converter.getConvertibleTypes();
        assertThat(types).hasSize(1);
        var pair = types.iterator().next();
        assertThat(pair.getSourceType()).isEqualTo(String.class);
        assertThat(pair.getTargetType()).isEqualTo(Object[].class);
    }
}
