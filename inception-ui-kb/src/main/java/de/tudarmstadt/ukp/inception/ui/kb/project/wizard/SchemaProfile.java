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
package de.tudarmstadt.ukp.inception.ui.kb.project.wizard;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import de.tudarmstadt.ukp.inception.kb.IriConstants;

public enum SchemaProfile
{
    RDFSCHEMA(RDFS.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT),

    WIKIDATASCHEMA(IriConstants.WIKIDATA_CLASS, IriConstants.WIKIDATA_SUBCLASS,
            IriConstants.WIKIDATA_TYPE, RDFS.COMMENT),

    OWLSCHEMA(OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT),

    CUSTOMSCHEMA(RDFS.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT);

    private final IRI classIri;
    private final IRI subclassIri;
    private final IRI typeIri;
    private final IRI descriptionIri;


    private SchemaProfile(IRI aClassIri, IRI aSubclassIri, IRI aTypeIri, IRI aDescriptionIri)
    {
        classIri = aClassIri;
        subclassIri = aSubclassIri;
        typeIri = aTypeIri;
        descriptionIri = aDescriptionIri;
    }

    public IRI getClassIri()
    {
        return classIri;
    }

    public IRI getSubclassIri()
    {
        return subclassIri;
    }

    public IRI getTypeIri()
    {
        return typeIri;
    }

    public IRI getDescriptionIri()
    {
        return descriptionIri;
    }
}
