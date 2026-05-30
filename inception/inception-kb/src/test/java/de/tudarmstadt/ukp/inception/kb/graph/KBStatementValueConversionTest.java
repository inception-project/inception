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
package de.tudarmstadt.ukp.inception.kb.graph;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

/**
 * Pins the RDF-Literal-to-Java-Object behavior of {@link KBStatement#setValue(Object)} when the
 * input is an RDF Literal. Originated as a characterization of the prior rdfbeans-based mapping;
 * preserved through the migration to {@link de.tudarmstadt.ukp.inception.kb.XsdDatatypes}.
 */
class KBStatementValueConversionTest
{
    private final ValueFactory vf = SimpleValueFactory.getInstance();

    private KBStatement statementWith(Object rdfValue)
    {
        var stmt = new KBStatement(new KBHandle("http://example.org/subj"));
        stmt.setValue(rdfValue);
        return stmt;
    }

    @Test
    void xsdIntBecomesInteger()
    {
        var stmt = statementWith(vf.createLiteral("42", XSD.INT));
        assertThat(stmt.getValue()).isInstanceOf(Integer.class).isEqualTo(42);
    }

    @Test
    void xsdLongBecomesLong()
    {
        var stmt = statementWith(vf.createLiteral("9999999999", XSD.LONG));
        assertThat(stmt.getValue()).isInstanceOf(Long.class).isEqualTo(9_999_999_999L);
    }

    @Test
    void xsdShortBecomesShort()
    {
        var stmt = statementWith(vf.createLiteral("5", XSD.SHORT));
        assertThat(stmt.getValue()).isInstanceOf(Short.class).isEqualTo((short) 5);
    }

    @Test
    void xsdByteBecomesByte()
    {
        var stmt = statementWith(vf.createLiteral("7", XSD.BYTE));
        assertThat(stmt.getValue()).isInstanceOf(Byte.class).isEqualTo((byte) 7);
    }

    @Test
    void xsdIntegerKeepsStringLabel()
    {
        // rdfbeans does not include BigInteger in its type map, so xsd:integer literals are
        // returned as their raw String label rather than a numeric type.
        var stmt = statementWith(vf.createLiteral("123456789012345678901234567890", XSD.INTEGER));
        assertThat(stmt.getValue()).isInstanceOf(String.class)
                .isEqualTo("123456789012345678901234567890");
    }

    @Test
    void xsdFloatBecomesFloat()
    {
        var stmt = statementWith(vf.createLiteral("1.5", XSD.FLOAT));
        assertThat(stmt.getValue()).isInstanceOf(Float.class).isEqualTo(1.5f);
    }

    @Test
    void xsdDoubleBecomesDouble()
    {
        var stmt = statementWith(vf.createLiteral("2.5", XSD.DOUBLE));
        assertThat(stmt.getValue()).isInstanceOf(Double.class).isEqualTo(2.5d);
    }

    @Test
    void xsdDecimalBecomesBigDecimal()
    {
        var stmt = statementWith(vf.createLiteral("3.14", XSD.DECIMAL));
        assertThat(stmt.getValue()).isInstanceOf(BigDecimal.class)
                .isEqualTo(new BigDecimal("3.14"));
    }

    @Test
    void xsdBooleanBecomesBoolean()
    {
        var stmt = statementWith(vf.createLiteral("true", XSD.BOOLEAN));
        assertThat(stmt.getValue()).isInstanceOf(Boolean.class).isEqualTo(Boolean.TRUE);
    }

    @Test
    void xsdStringBecomesString()
    {
        var stmt = statementWith(vf.createLiteral("hello", XSD.STRING));
        assertThat(stmt.getValue()).isInstanceOf(String.class).isEqualTo("hello");
    }

    @Test
    void langStringPopulatesLanguageAndKeepsLabel()
    {
        var stmt = statementWith(vf.createLiteral("hallo", "de"));
        assertThat(stmt.getValue()).isInstanceOf(String.class).isEqualTo("hallo");
        assertThat(stmt.getLanguage()).isEqualTo("de");
    }

    @Test
    void iriValueIsKeptAsIri()
    {
        var iri = vf.createIRI("http://example.org/x");
        var stmt = statementWith(iri);
        assertThat(stmt.getValue()).isEqualTo(iri);
    }

    @Test
    void bnodeBecomesNull()
    {
        var stmt = statementWith(vf.createBNode("b1"));
        assertThat(stmt.getValue()).isNull();
    }
}
