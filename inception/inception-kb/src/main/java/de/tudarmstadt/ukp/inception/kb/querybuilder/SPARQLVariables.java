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
package de.tudarmstadt.ukp.inception.kb.querybuilder;

import static de.tudarmstadt.ukp.inception.kb.IriConstants.PREFIX_VIRTUOSO;
import static org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder.var;
import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.PropertyPath;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.PropertyPathBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

public interface SPARQLVariables
{
    String VAR_SUBJECT_NAME = "subj";
    String VAR_PREDICATE_NAME = "pred";
    String VAR_OBJECT_NAME = "obj";
    String VAR_MATCH_TERM_PROPERTY_NAME = "pMatch";
    String VAR_PREF_LABEL_PROPERTY_NAME = "pPrefLabel";
    String VAR_PREF_LABEL_NAME = "l";
    String VAR_MATCH_TERM_NAME = "m";
    String VAR_SCORE_NAME = "sc";
    String VAR_DESCRIPTION_NAME = "d";
    String VAR_DESCRIPTION_CANDIDATE_NAME = "dc";
    String VAR_RANGE_NAME = "range";
    String VAR_DOMAIN_NAME = "domain";
    String VAR_DEPRECATION_NAME = "dp";

    Variable VAR_SUBJECT = var(VAR_SUBJECT_NAME);
    Variable VAR_SCORE = var(VAR_SCORE_NAME);
    Variable VAR_PREDICATE = var(VAR_PREDICATE_NAME);
    Variable VAR_OBJECT = var(VAR_OBJECT_NAME);
    Variable VAR_RANGE = var(VAR_RANGE_NAME);
    Variable VAR_DOMAIN = var(VAR_DOMAIN_NAME);
    Variable VAR_PREF_LABEL = var(VAR_PREF_LABEL_NAME);
    Variable VAR_MATCH_TERM = var(VAR_MATCH_TERM_NAME);
    Variable VAR_PREF_LABEL_PROPERTY = var(VAR_PREF_LABEL_PROPERTY_NAME);
    Variable VAR_MATCH_TERM_PROPERTY = var(VAR_MATCH_TERM_PROPERTY_NAME);
    Variable VAR_DESCRIPTION = var(VAR_DESCRIPTION_NAME);
    Variable VAR_DESC_CANDIDATE = var(VAR_DESCRIPTION_CANDIDATE_NAME);
    Variable VAR_DEPRECATION = var(VAR_DEPRECATION_NAME);

    // Some versions of Virtuoso do not like it when we declare the bif prefix.
    // Prefix PREFIX_VIRTUOSO_SEARCH = prefix("bif", iri(PREFIX_VIRTUOSO));
    // Iri VIRTUOSO_QUERY = PREFIX_VIRTUOSO_SEARCH.iri("contains");
    Iri VIRTUOSO_QUERY = iri(PREFIX_VIRTUOSO, "contains");

    Iri OWL_INTERSECTIONOF = iri(OWL.INTERSECTIONOF.stringValue());
    Iri RDF_REST = iri(RDF.REST.stringValue());
    Iri RDF_FIRST = iri(RDF.FIRST.stringValue());
    PropertyPath OWL_INTERSECTIONOF_PATH = PropertyPathBuilder.of(OWL_INTERSECTIONOF).then(RDF_REST)
            .zeroOrMore().then(RDF_FIRST).build();
}
