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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;

/**
 * Pins the Java-value-to-RDF-Value behavior of
 * {@link InceptionValueMapper#mapStatementValue(KBStatement, ValueFactory)}. Originated as a
 * characterization of the prior rdfbeans-based mapping; preserved through the migration to
 * {@link de.tudarmstadt.ukp.inception.kb.XsdDatatypes}.
 */
class InceptionValueMapperTest
{
    private final ValueFactory vf = SimpleValueFactory.getInstance();
    private final InceptionValueMapper sut = new InceptionValueMapper();

    private KBStatement statement(Object value)
    {
        var stmt = new KBStatement(new KBHandle("http://example.org/subj"));
        stmt.setValue(value);
        return stmt;
    }

    private KBStatement statementWithLanguage(Object value, String language)
    {
        var stmt = statement(value);
        stmt.setLanguage(language);
        return stmt;
    }

    @Test
    void iriPassthrough()
    {
        var iri = vf.createIRI("http://example.org/x");
        var result = sut.mapStatementValue(statement(iri), vf);
        assertThat(result).isEqualTo(iri);
    }

    @Test
    void uriLikeStringBecomesIri()
    {
        var result = sut.mapStatementValue(statement("http://example.org/x"), vf);
        assertThat(result).isInstanceOf(IRI.class);
        assertThat(result.stringValue()).isEqualTo("http://example.org/x");
    }

    @Test
    void plainStringBecomesXsdString()
    {
        var result = sut.mapStatementValue(statement("hello"), vf);
        assertThat(result).isInstanceOf(Literal.class);
        var lit = (Literal) result;
        assertThat(lit.getLabel()).isEqualTo("hello");
        assertThat(lit.getDatatype()).isEqualTo(XSD.STRING);
    }

    @Test
    void stringWithLanguageBecomesLangString()
    {
        var result = sut.mapStatementValue(statementWithLanguage("hello", "en"), vf);
        assertThat(result).isInstanceOf(Literal.class);
        var lit = (Literal) result;
        assertThat(lit.getLabel()).isEqualTo("hello");
        assertThat(lit.getLanguage()).hasValue("en");
    }

    @Test
    void integerBecomesXsdInt()
    {
        var result = sut.mapStatementValue(statement(42), vf);
        assertThat(result).isInstanceOf(Literal.class);
        var lit = (Literal) result;
        assertThat(lit.getLabel()).isEqualTo("42");
        assertThat(lit.getDatatype()).isEqualTo(XSD.INT);
    }

    @Test
    void longBecomesXsdLong()
    {
        var result = sut.mapStatementValue(statement(9_999_999_999L), vf);
        var lit = (Literal) result;
        assertThat(lit.getLabel()).isEqualTo("9999999999");
        assertThat(lit.getDatatype()).isEqualTo(XSD.LONG);
    }

    @Test
    void shortBecomesXsdShort()
    {
        var result = sut.mapStatementValue(statement((short) 5), vf);
        var lit = (Literal) result;
        assertThat(lit.getLabel()).isEqualTo("5");
        assertThat(lit.getDatatype()).isEqualTo(XSD.SHORT);
    }

    @Test
    void byteBecomesXsdByte()
    {
        var result = sut.mapStatementValue(statement((byte) 7), vf);
        var lit = (Literal) result;
        assertThat(lit.getLabel()).isEqualTo("7");
        assertThat(lit.getDatatype()).isEqualTo(XSD.BYTE);
    }

    @Test
    void floatBecomesXsdFloat()
    {
        var result = sut.mapStatementValue(statement(1.5f), vf);
        var lit = (Literal) result;
        assertThat(lit.getLabel()).isEqualTo("1.5");
        assertThat(lit.getDatatype()).isEqualTo(XSD.FLOAT);
    }

    @Test
    void doubleBecomesXsdDouble()
    {
        var result = sut.mapStatementValue(statement(2.5d), vf);
        var lit = (Literal) result;
        assertThat(lit.getLabel()).isEqualTo("2.5");
        assertThat(lit.getDatatype()).isEqualTo(XSD.DOUBLE);
    }

    @Test
    void bigDecimalBecomesXsdDecimal()
    {
        var result = sut.mapStatementValue(statement(new BigDecimal("3.14")), vf);
        var lit = (Literal) result;
        assertThat(lit.getLabel()).isEqualTo("3.14");
        assertThat(lit.getDatatype()).isEqualTo(XSD.DECIMAL);
    }

    @Test
    void booleanBecomesXsdBoolean()
    {
        var result = sut.mapStatementValue(statement(Boolean.TRUE), vf);
        var lit = (Literal) result;
        assertThat(lit.getLabel()).isEqualTo("true");
        assertThat(lit.getDatatype()).isEqualTo(XSD.BOOLEAN);
    }

    @Test
    void dateBecomesXsdDateTime()
    {
        var cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2025, Calendar.MAY, 30, 12, 0, 0);
        Date date = cal.getTime();

        var result = sut.mapStatementValue(statement(date), vf);
        var lit = (Literal) result;
        assertThat(lit.getDatatype()).isEqualTo(XSD.DATETIME);
        // Label is formatted in the local timezone, so we only check the date portion.
        assertThat(lit.getLabel()).contains("2025-05-30T");
    }

    @Test
    void uriBecomesIri() throws Exception
    {
        // rdfbeans handles java.net.URI by emitting an IRI (not a typed literal with xsd:anyURI).
        var result = sut.mapStatementValue(statement(new URI("urn:example:abc")), vf);
        assertThat(result).isInstanceOf(IRI.class);
        assertThat(result.stringValue()).isEqualTo("urn:example:abc");
    }
}
