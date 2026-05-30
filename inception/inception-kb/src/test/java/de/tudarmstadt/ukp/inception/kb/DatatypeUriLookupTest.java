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
package de.tudarmstadt.ukp.inception.kb;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Date;

import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

/**
 * Pins the Java-class-to-XSD-IRI lookup behavior of {@link XsdDatatypes#datatypeOf(Class)}, used by
 * the {@code *LiteralValueSupport} hierarchy and {@code ValueTypeSupportRegistryImpl}. Originated
 * as a characterization of rdfbeans' {@code DefaultDatatypeMapper.getDatatypeURI}; the migration to
 * {@code XsdDatatypes} must preserve these mappings.
 */
class DatatypeUriLookupTest
{
    @Test
    void stringMapsToXsdString()
    {
        assertThat(XsdDatatypes.datatypeOf(String.class)).isEqualTo(XSD.STRING);
    }

    @Test
    void integerMapsToXsdInt()
    {
        assertThat(XsdDatatypes.datatypeOf(Integer.class)).isEqualTo(XSD.INT);
    }

    @Test
    void longMapsToXsdLong()
    {
        assertThat(XsdDatatypes.datatypeOf(Long.class)).isEqualTo(XSD.LONG);
    }

    @Test
    void shortMapsToXsdShort()
    {
        assertThat(XsdDatatypes.datatypeOf(Short.class)).isEqualTo(XSD.SHORT);
    }

    @Test
    void byteMapsToXsdByte()
    {
        assertThat(XsdDatatypes.datatypeOf(Byte.class)).isEqualTo(XSD.BYTE);
    }

    @Test
    void floatMapsToXsdFloat()
    {
        assertThat(XsdDatatypes.datatypeOf(Float.class)).isEqualTo(XSD.FLOAT);
    }

    @Test
    void doubleMapsToXsdDouble()
    {
        assertThat(XsdDatatypes.datatypeOf(Double.class)).isEqualTo(XSD.DOUBLE);
    }

    @Test
    void bigDecimalMapsToXsdDecimal()
    {
        assertThat(XsdDatatypes.datatypeOf(BigDecimal.class)).isEqualTo(XSD.DECIMAL);
    }

    @Test
    void booleanMapsToXsdBoolean()
    {
        assertThat(XsdDatatypes.datatypeOf(Boolean.class)).isEqualTo(XSD.BOOLEAN);
    }

    @Test
    void dateMapsToXsdDateTime()
    {
        assertThat(XsdDatatypes.datatypeOf(Date.class)).isEqualTo(XSD.DATETIME);
    }

    @Test
    void uriMapsToXsdAnyUri()
    {
        assertThat(XsdDatatypes.datatypeOf(URI.class)).isEqualTo(XSD.ANYURI);
    }

    @Test
    void unknownClassReturnsNull()
    {
        assertThat(XsdDatatypes.datatypeOf(Object.class)).isNull();
    }
}
