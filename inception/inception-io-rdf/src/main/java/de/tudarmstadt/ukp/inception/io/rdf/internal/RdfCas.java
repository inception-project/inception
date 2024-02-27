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

/**
 * RDF CAS vocabulary.
 */
public class RdfCas
{
    public static final String PREFIX_RDFCAS = "rdfcas";

    public static final String NS_RDFCAS = "http://uima.apache.org/rdf/cas#";
    public static final String NS_UIMA = "uima:";

    public static final String PROP_VIEW = NS_RDFCAS + "view";
    public static final String PROP_INDEXED_IN = NS_RDFCAS + "indexedIn";

    // public static final String TYPE_CAS = NS_RDFCAS + "CAS";
    public static final String TYPE_VIEW = NS_RDFCAS + "View";
    public static final String TYPE_FEATURE_STRUCTURE = NS_RDFCAS + "FeatureStructure";

    public static final String PROP_SOFA_ID = NS_UIMA + CAS.TYPE_NAME_SOFA + '-'
            + CAS.FEATURE_BASE_NAME_SOFAID;
    public static final String PROP_SOFA_STRING = NS_UIMA + CAS.TYPE_NAME_SOFA + '-'
            + CAS.FEATURE_BASE_NAME_SOFASTRING;
    public static final String PROP_SOFA_MIME_TYPE = NS_UIMA + CAS.TYPE_NAME_SOFA + '-'
            + CAS.FEATURE_BASE_NAME_SOFAMIME;
}
