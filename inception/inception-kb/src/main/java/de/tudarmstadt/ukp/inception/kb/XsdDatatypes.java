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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Date;
import java.util.Map;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.XSD;

/**
 * Java/RDF datatype conversion helpers — replaces the formerly-used
 * {@code org.cyberborean.rdfbeans.datatype.DefaultDatatypeMapper}. Behavior is pinned by
 * {@code InceptionValueMapperTest}, {@code KBStatementValueConversionTest} and
 * {@code DatatypeUriLookupTest}.
 */
public final class XsdDatatypes
{
    private static final Map<Class<?>, IRI> JAVA_TO_XSD = Map.ofEntries(
            Map.entry(String.class, XSD.STRING), Map.entry(Boolean.class, XSD.BOOLEAN),
            Map.entry(BigDecimal.class, XSD.DECIMAL), Map.entry(Byte.class, XSD.BYTE),
            Map.entry(Double.class, XSD.DOUBLE), Map.entry(Float.class, XSD.FLOAT),
            Map.entry(Integer.class, XSD.INT), Map.entry(Long.class, XSD.LONG),
            Map.entry(Short.class, XSD.SHORT), Map.entry(Date.class, XSD.DATETIME),
            Map.entry(URI.class, XSD.ANYURI));

    private XsdDatatypes()
    {
        // utility class
    }

    /**
     * Map a Java class to its corresponding XSD datatype IRI, or {@code null} if the class is not
     * one of the standard types.
     */
    public static IRI datatypeOf(Class<?> clazz)
    {
        return JAVA_TO_XSD.get(clazz);
    }

    /**
     * Convert a Java value to an RDF {@link Value}. {@link URI} values are emitted as IRIs (not as
     * {@code xsd:anyURI} literals); {@link BigDecimal} and {@link BigInteger} are mapped via the
     * value factory's typed-literal constructors; all other types are routed through
     * {@link Values#literal(ValueFactory, Object, boolean)} with {@code failOnUnknownType=true}.
     */
    public static Value toRdfValue(Object javaValue, ValueFactory vf)
    {
        if (javaValue instanceof URI) {
            return vf.createIRI(javaValue.toString());
        }
        if (javaValue instanceof BigDecimal) {
            return vf.createLiteral((BigDecimal) javaValue);
        }
        if (javaValue instanceof BigInteger) {
            return vf.createLiteral((BigInteger) javaValue);
        }
        return Values.literal(vf, javaValue, true);
    }

    /**
     * Convert an RDF {@link Literal} to a Java value of the natural type for its XSD datatype.
     * Literals with an unrecognized datatype (including {@code xsd:integer} and language-tagged
     * strings) are returned as their raw label.
     */
    public static Object toJavaObject(Literal lit)
    {
        IRI dt = lit.getDatatype();
        if (XSD.STRING.equals(dt)) {
            return lit.getLabel();
        }
        if (XSD.BOOLEAN.equals(dt)) {
            return lit.booleanValue();
        }
        if (XSD.INT.equals(dt)) {
            return lit.intValue();
        }
        if (XSD.LONG.equals(dt)) {
            return lit.longValue();
        }
        if (XSD.SHORT.equals(dt)) {
            return lit.shortValue();
        }
        if (XSD.BYTE.equals(dt)) {
            return lit.byteValue();
        }
        if (XSD.FLOAT.equals(dt)) {
            return lit.floatValue();
        }
        if (XSD.DOUBLE.equals(dt)) {
            return lit.doubleValue();
        }
        if (XSD.DECIMAL.equals(dt)) {
            return lit.decimalValue();
        }
        if (XSD.DATETIME.equals(dt)) {
            return lit.calendarValue().toGregorianCalendar().getTime();
        }
        return lit.getLabel();
    }
}
