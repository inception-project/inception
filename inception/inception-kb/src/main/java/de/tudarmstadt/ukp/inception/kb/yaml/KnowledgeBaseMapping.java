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
package de.tudarmstadt.ukp.inception.kb.yaml;

import java.io.Serializable;

import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class KnowledgeBaseMapping
    implements Serializable
{

    private static final long serialVersionUID = 8967034943386456692L;

    @JsonProperty("class")
    private String classIri = RDFS.CLASS.stringValue();

    @JsonProperty("subclass-of")
    private String subclassIri = RDFS.SUBCLASSOF.stringValue();

    @JsonProperty("instance-of")
    private String typeIri = RDF.TYPE.stringValue();

    @JsonProperty("subproperty-of")
    private String subPropertyIri = RDFS.SUBPROPERTYOF.stringValue();

    @JsonProperty("description")
    private String descriptionIri = RDFS.COMMENT.stringValue();

    @JsonProperty("label")
    private String labelIri = RDFS.LABEL.stringValue();

    @JsonProperty("property-type")
    private String propertyTypeIri = RDF.PROPERTY.stringValue();

    @JsonProperty("property-label")
    private String propertyLabelIri = RDFS.LABEL.stringValue();

    @JsonProperty("property-description")
    private String propertyDescriptionIri = RDFS.COMMENT.stringValue();

    @JsonProperty("deprecation-property")
    private String deprecationPropertyIri = OWL.DEPRECATED.stringValue();

    @JsonCreator
    public KnowledgeBaseMapping(@JsonProperty("class") String aClassIri,
            @JsonProperty("subclass-of") String aSubclassIri,
            @JsonProperty("instance-of") String aTypeIri,
            @JsonProperty("subproperty-of") String aSubPropertyIri,
            @JsonProperty("description") String aDescriptionIri,
            @JsonProperty("label") String aLabelIri,
            @JsonProperty("property-type") String aPropertyTypeIri,
            @JsonProperty("property-label") String aPropertyLabelIri,
            @JsonProperty("property-description") String aPropertyDescriptionIri,
            @JsonProperty("deprecation-property") String aDeprecationPropertyIri)

    {
        classIri = aClassIri;
        subclassIri = aSubclassIri;
        typeIri = aTypeIri;
        subPropertyIri = aSubPropertyIri;
        descriptionIri = aDescriptionIri;
        labelIri = aLabelIri;
        propertyTypeIri = aPropertyTypeIri;
        propertyLabelIri = aPropertyLabelIri;
        propertyDescriptionIri = aPropertyDescriptionIri;
        deprecationPropertyIri = aDeprecationPropertyIri;
    }

    public KnowledgeBaseMapping()
    {

    }

    public String getClassIri()
    {
        return classIri;
    }

    public void setClassIri(String aClassIri)
    {
        classIri = aClassIri;
    }

    public String getSubclassIri()
    {
        return subclassIri;
    }

    public void setSubclassIri(String aSubclassIri)
    {
        subclassIri = aSubclassIri;
    }

    public String getTypeIri()
    {
        return typeIri;
    }

    public void setTypeIri(String aTypeIri)
    {
        typeIri = aTypeIri;
    }

    public String getSubPropertyIri()
    {
        return subPropertyIri;
    }

    public void setSubPropertyIri(String subPropertyIri)
    {
        this.subPropertyIri = subPropertyIri;
    }

    public String getDescriptionIri()
    {
        return descriptionIri;
    }

    public void setDescriptionIri(String aDescriptionIri)
    {
        descriptionIri = aDescriptionIri;
    }

    public String getLabelIri()
    {
        return labelIri;
    }

    public void setLabelIri(String aLabelIri)
    {
        labelIri = aLabelIri;
    }

    public String getPropertyTypeIri()
    {
        return propertyTypeIri;
    }

    public void setPropertyTypeIri(String aPropertyTypeIri)
    {
        propertyTypeIri = aPropertyTypeIri;
    }

    public String getPropertyLabelIri()
    {
        return propertyLabelIri;
    }

    public void setPropertyLabelIri(String aPropertyLabelIri)
    {
        propertyLabelIri = aPropertyLabelIri;
    }

    public String getPropertyDescriptionIri()
    {
        return propertyDescriptionIri;
    }

    public void setPropertyDescriptionIri(String aPropertyDescriptionIri)
    {
        propertyDescriptionIri = aPropertyDescriptionIri;
    }

    public void setDeprecationPropertyIri(String aDeprecationPropertyIri)
    {
        deprecationPropertyIri = aDeprecationPropertyIri;
    }

    public String getDeprecationPropertyIri()
    {
        return deprecationPropertyIri;
    }
}
