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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;

public enum SchemaProfile
{
    RDFSCHEMA("RDF", RDFS.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY,
        RDFS.LABEL, RDFS.COMMENT),

    WIKIDATASCHEMA("WIKIDATA", IriConstants.WIKIDATA_CLASS, IriConstants.WIKIDATA_SUBCLASS,
        IriConstants.WIKIDATA_TYPE, RDFS.COMMENT, RDFS.LABEL, IriConstants.WIKIDATA_PROPERTY_TYPE,
        RDFS.LABEL, RDFS.COMMENT),

    OWLSCHEMA("OWL", OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL, RDF.PROPERTY,
        RDFS.LABEL, RDFS.COMMENT),

    SKOSSCHEMA("SKOS", SKOS.CONCEPT, SKOS.BROADER, RDF.TYPE, RDFS.COMMENT, SKOS.PREF_LABEL,
        RDF.PROPERTY, SKOS.PREF_LABEL, RDFS.COMMENT),

    CUSTOMSCHEMA("CUSTOM", RDFS.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.COMMENT, RDFS.LABEL,
        RDF.PROPERTY, RDFS.LABEL, RDFS.COMMENT);

    private final String label;
    private final IRI classIri;
    private final IRI subclassIri;
    private final IRI typeIri;
    private final IRI descriptionIri;
    private final IRI labelIri;
    private final IRI propertyTypeIri;
    private final IRI propertyLabelIri;
    private final IRI propertyDescriptionIri;

    private SchemaProfile(String aLabel, IRI aClassIri, IRI aSubclassIri, IRI aTypeIri,
        IRI aDescriptionIri, IRI aLabelIri, IRI aPropertyTypeIri, IRI aPropertyLabelIri,
        IRI aPropertyDescriptionIri)
    {
        label = aLabel;
        classIri = aClassIri;
        subclassIri = aSubclassIri;
        typeIri = aTypeIri;
        descriptionIri = aDescriptionIri;
        labelIri = aLabelIri;
        propertyTypeIri = aPropertyTypeIri;
        propertyLabelIri = aPropertyLabelIri;
        propertyDescriptionIri = aPropertyDescriptionIri;
    }

    public String getLabel()
    {
        return label;
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

    public IRI getLabelIri()
    {
        return labelIri;
    }

    public IRI getPropertyTypeIri()
    {
        return propertyTypeIri;
    }

    public IRI getPropertyLabelIri()
    {
        return propertyLabelIri;
    }

    public IRI getPropertyDescriptionIri()
    {
        return propertyDescriptionIri;
    }
}
