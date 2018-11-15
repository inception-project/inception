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

import static java.util.Arrays.asList;

import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;

public class IriConstants
{
    public static final String INCEPTION_SCHEMA_NAMESPACE = "http://www.ukp.informatik.tu-darmstadt.de/inception/schema-1.0#";
    public static final String INCEPTION_NAMESPACE = "http://www.ukp.informatik.tu-darmstadt.de/inception/1.0#";

    public static final String PREFIX_WIKIDATA_ENTITY = "http://www.wikidata.org/entity/";
    public static final String PREFIX_WIKIDATA_DIRECT = "http://www.wikidata.org/prop/direct/";
    public static final String PREFIX_SCHEMA = "http://schema.org/";
    public static final String PREFIX_LUCENE_SEARCH = "http://www.openrdf.org/contrib/lucenesail#";

    public static final String UKP_WIKIDATA_SPARQL_ENDPOINT = "http://knowledgebase.ukp.informatik.tu-darmstadt.de:8890/sparql";
    public static final List<String> IMPLICIT_NAMESPACES = asList(RDF.NAMESPACE, RDFS.NAMESPACE,
            XMLSchema.NAMESPACE, OWL.NAMESPACE, INCEPTION_SCHEMA_NAMESPACE);

    /**
     * http://www.wikidata.org/entity/Q35120
     */
    public static final IRI WIKIDATA_CLASS;

    /**
     * http://www.wikidata.org/prop/direct/P279
     */
    public static final IRI WIKIDATA_SUBCLASS;

    /**
     * http://www.wikidata.org/prop/direct/P31
     */
    public static final IRI WIKIDATA_TYPE;
    
    /**
     * http://www.wikidata.org/entity/Q18616576
     */
    public static final IRI WIKIDATA_PROPERTY_TYPE;

    /**
     * http://www.schema.org/description
     */
    public static final IRI SCHEMA_DESCRIPTION;

    public static final IRI FTS_VIRTUOSO;
    public static final IRI FTS_LUCENE;

    public static final List<IRI> CLASS_IRIS;
    public static final List<IRI> SUBCLASS_IRIS;
    public static final List<IRI> TYPE_IRIS;
    public static final List<IRI> DESCRIPTION_IRIS;
    public static final List<IRI> LABEL_IRIS;
    public static final List<IRI> PROPERTY_TYPE_IRIS;
    public static final List<IRI> PROPERTY_LABEL_IRIS;
    public static final List<IRI> PROPERTY_DESCRIPTION_IRIS;
    public static final List<IRI> FTS_IRIS;

    static {
        ValueFactory vf = SimpleValueFactory.getInstance();
        
        WIKIDATA_CLASS = vf.createIRI(PREFIX_WIKIDATA_ENTITY, "Q35120");
        WIKIDATA_SUBCLASS = vf.createIRI(PREFIX_WIKIDATA_DIRECT, "P279");
        WIKIDATA_TYPE = vf.createIRI(PREFIX_WIKIDATA_DIRECT, "P31");
        WIKIDATA_PROPERTY_TYPE =  vf.createIRI(PREFIX_WIKIDATA_ENTITY, "Q18616576");
        SCHEMA_DESCRIPTION = vf.createIRI(PREFIX_SCHEMA, "description");
        FTS_VIRTUOSO = vf.createIRI("bif:contains");
        FTS_LUCENE = vf.createIRI(PREFIX_LUCENE_SEARCH, "matches");

        CLASS_IRIS = asList(RDFS.CLASS, OWL.CLASS, WIKIDATA_CLASS, SKOS.CONCEPT);
        SUBCLASS_IRIS = asList(RDFS.SUBCLASSOF, WIKIDATA_SUBCLASS, SKOS.BROADER);
        TYPE_IRIS = asList(RDF.TYPE, WIKIDATA_TYPE);
        DESCRIPTION_IRIS = asList(RDFS.COMMENT, SCHEMA_DESCRIPTION);
        LABEL_IRIS = asList(RDFS.LABEL, SKOS.PREF_LABEL);
        PROPERTY_TYPE_IRIS = asList(RDF.PROPERTY, WIKIDATA_PROPERTY_TYPE);
        PROPERTY_LABEL_IRIS = asList(RDFS.LABEL, SKOS.PREF_LABEL);
        PROPERTY_DESCRIPTION_IRIS = asList(RDFS.COMMENT, SCHEMA_DESCRIPTION);
        FTS_IRIS = asList(FTS_VIRTUOSO, FTS_LUCENE);
    }
}
