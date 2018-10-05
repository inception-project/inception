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
package de.tudarmstadt.ukp.inception.kb.yaml;

import java.io.Serializable;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class KnowledgeBaseMapping implements Serializable
{

    private static final long serialVersionUID = 8967034943386456692L;

    @JsonProperty("class")
    private IRI classIri;

    @JsonProperty("subclass-of")
    private IRI subclassIri;

    @JsonProperty("instance-of")
    private IRI typeIri;

    @JsonProperty("subproperty-of")
    private IRI subPropertyIri;

    @JsonProperty("description")
    private IRI descriptionIri;

    @JsonProperty("label")
    private IRI labelIri;

    @JsonProperty("property-type")
    private IRI propertyTypeIri;

    @JsonProperty("property-label")
    private IRI propertyLabelIri;

    @JsonProperty("property-description")
    private IRI propertyDescriptionIri;   
   
    @JsonCreator public KnowledgeBaseMapping(@JsonProperty("class") String classIri,
        @JsonProperty("subclass-of") String subclassIri,
        @JsonProperty("instance-of") String typeIri,
        @JsonProperty("subproperty-of") String subPropertyIri,
        @JsonProperty("description") String descriptionIri,
        @JsonProperty("label") String labelIri,
        @JsonProperty("property-type") String propertyTypeIri,
        @JsonProperty("property-label") String propertyLabelIri,
        @JsonProperty("property-description") String propertyDescriptionIri)
    {
        SimpleValueFactory vf = SimpleValueFactory.getInstance();
        this.classIri = vf.createIRI(classIri);
        this.subclassIri = vf.createIRI(subclassIri);
        this.typeIri = vf.createIRI(typeIri);
        this.subPropertyIri = vf.createIRI(subPropertyIri);
        this.descriptionIri = vf.createIRI(descriptionIri);
        this.labelIri = vf.createIRI(labelIri);
        this.propertyTypeIri = vf.createIRI(propertyTypeIri);
        this.propertyLabelIri = vf.createIRI(propertyLabelIri);
        this.propertyDescriptionIri = vf.createIRI(propertyDescriptionIri);
    }
    
    public KnowledgeBaseMapping() {
        
    }

    public IRI getClassIri()
    {
        return classIri;
    }
    
    public void setClassIri(IRI aClassIri)
    {   
        classIri = aClassIri;
    }

    public IRI getSubclassIri()
    {
        return subclassIri;
    }

    public void setSubclassIri(IRI aSubclassIri)
    {
        subclassIri = aSubclassIri;
    }

    public IRI getTypeIri()
    {
        return typeIri;
    }

    public void setTypeIri(IRI aTypeIri)
    {
        typeIri = aTypeIri;
    }

    public IRI getSubPropertyIri()
    {
        return subPropertyIri;
    }

    public void setSubPropertyIri(IRI subPropertyIri)
    {
        this.subPropertyIri = subPropertyIri;
    }

    public IRI getDescriptionIri()
    {
        return descriptionIri;
    }

    public void setDescriptionIri(IRI aDescriptionIri)
    {
        descriptionIri = aDescriptionIri;
    }

    public IRI getLabelIri()
    {
        return labelIri;
    }

    public void setLabelIri(IRI aLabelIri)
    {
        labelIri = aLabelIri;
    }

    public IRI getPropertyTypeIri()
    {
        return propertyTypeIri;
    }

    public void setPropertyTypeIri(IRI aPropertyTypeIri)
    {
        propertyTypeIri = aPropertyTypeIri;
    }

    public IRI getPropertyLabelIri()
    {
        return propertyLabelIri;
    }

    public void setPropertyLabelIri(IRI aPropertyLabelIri)
    {
        propertyLabelIri = aPropertyLabelIri;
    }

    public IRI getPropertyDescriptionIri()
    {
        return propertyDescriptionIri;
    }

    public void setPropertyDescriptionIri(IRI aPropertyDescriptionIri)
    {
        propertyDescriptionIri = aPropertyDescriptionIri;
    }
}
