/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.inception.kb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

public class IriConstants
{

    public static final String INCEPTION_SCHEMA_NAMESPACE = "http://www.ukp.informatik.tu-darmstadt.de/inception/schema-1.0#";

    public static final String INCEPTION_NAMESPACE = "http://www.ukp.informatik.tu-darmstadt.de/inception/1.0#";

    public static final String WIKIDATA_NAMESPACE = "https://www.wikidata.org/wiki/";

    public static final String[] IMPLICIT_NAMESPACES = { RDF.NAMESPACE, RDFS.NAMESPACE,
            XMLSchema.NAMESPACE, OWL.NAMESPACE, INCEPTION_SCHEMA_NAMESPACE };

    /**
     * Define "important" URIs to allow for importance-based sorting of statements.
     */
    public static final Set<String> IMPORTANT_CONCEPT_URIS = new HashSet<>(
            Arrays.asList(RDFS.SUBCLASSOF.stringValue()));

    /**
     * https://www.wikidata.org/wiki/Q35120
     */
    public static final IRI WIKIDATA_CLASS;
    /**
     * https://www.wikidata.org/wiki/Property:P279
     */
    public static final IRI WIKIDATA_SUBCLASS;
    /**
     * https://www.wikidata.org/wiki/Property:P31
     */
    public static final IRI WIKIDATA_TYPE;

    public static final List<IRI> CLASS_IRIS;
    public static final List<IRI> SUBCLASS_IRIS;
    public static final List<IRI> TYPE_IRIS;

    static {
        ValueFactory vf = SimpleValueFactory.getInstance();
        WIKIDATA_CLASS = vf.createIRI(WIKIDATA_NAMESPACE, "Q35120");
        WIKIDATA_SUBCLASS = vf.createIRI(WIKIDATA_NAMESPACE, "Property:P279");
        WIKIDATA_TYPE = vf.createIRI(WIKIDATA_NAMESPACE, "Property:P31");

        CLASS_IRIS = buildImmutableList(RDFS.CLASS, OWL.CLASS, WIKIDATA_CLASS);
        SUBCLASS_IRIS = buildImmutableList(RDFS.SUBCLASSOF, WIKIDATA_SUBCLASS);
        TYPE_IRIS = buildImmutableList(RDF.TYPE, WIKIDATA_TYPE);
    }

    private static <T> List<T> buildImmutableList(T... items)
    {
        List<T> list = new ArrayList(Arrays.asList(items));
        return Collections.unmodifiableList(list);
    }


}
