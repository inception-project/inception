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
package de.tudarmstadt.ukp.inception.io.rdf.internal;

import org.apache.uima.cas.CAS;
import org.eclipse.rdf4j.model.IRI;

/**
 * RDF CAS vocabulary.
 */
public class RdfCas
{
    public static final String PREFIX_RDFCAS = "rdfcas";

    public static final String NS_RDFCAS = "http://uima.apache.org/rdf/cas#";
    public static final String SCHEME_UIMA = "uima:";

    public static final IRI PROP_VIEW = new BasicIRI(NS_RDFCAS, "view");
    public static final IRI PROP_INDEXED_IN = new BasicIRI(NS_RDFCAS, "indexedIn");

    public static final IRI TYPE_VIEW = new BasicIRI(NS_RDFCAS, "View");
    public static final IRI TYPE_FEATURE_STRUCTURE = new BasicIRI(NS_RDFCAS, "FeatureStructure");

    public static final IRI PROP_SOFA_ID = new BasicIRI(SCHEME_UIMA,
            CAS.TYPE_NAME_SOFA + '-' + CAS.FEATURE_BASE_NAME_SOFAID);
    public static final IRI PROP_SOFA_STRING = new BasicIRI(SCHEME_UIMA,
            CAS.TYPE_NAME_SOFA + '-' + CAS.FEATURE_BASE_NAME_SOFASTRING);
    public static final IRI PROP_SOFA_MIME_TYPE = new BasicIRI(SCHEME_UIMA,
            CAS.TYPE_NAME_SOFA + '-' + CAS.FEATURE_BASE_NAME_SOFAMIME);
}
