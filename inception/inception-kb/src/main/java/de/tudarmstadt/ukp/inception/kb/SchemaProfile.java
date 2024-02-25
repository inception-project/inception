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

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

public enum SchemaProfile
{
    RDFSCHEMA("RDF", RDFS.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.SUBPROPERTYOF, RDFS.COMMENT,
            RDFS.LABEL, RDF.PROPERTY, RDFS.LABEL, RDFS.COMMENT, OWL.DEPRECATED),

    WIKIDATASCHEMA("WIKIDATA", IriConstants.WIKIDATA_CLASS, IriConstants.WIKIDATA_SUBCLASS,
            IriConstants.WIKIDATA_TYPE, RDFS.SUBPROPERTYOF, RDFS.COMMENT, RDFS.LABEL,
            IriConstants.WIKIDATA_PROPERTY_TYPE, RDFS.LABEL, RDFS.COMMENT, OWL.DEPRECATED),

    OWLSCHEMA("OWL", OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.SUBPROPERTYOF, RDFS.COMMENT,
            RDFS.LABEL, RDF.PROPERTY, RDFS.LABEL, RDFS.COMMENT, OWL.DEPRECATED),

    SKOSSCHEMA("SKOS", SKOS.CONCEPT, SKOS.BROADER, RDF.TYPE, RDFS.SUBPROPERTYOF, RDFS.COMMENT,
            SKOS.PREF_LABEL, RDF.PROPERTY, SKOS.PREF_LABEL, RDFS.COMMENT, OWL.DEPRECATED),

    CUSTOMSCHEMA("CUSTOM", RDFS.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.SUBPROPERTYOF, RDFS.COMMENT,
            RDFS.LABEL, RDF.PROPERTY, RDFS.LABEL, RDFS.COMMENT, OWL.DEPRECATED);

    private final String uiLabel;
    private final String classIri;
    private final String subclassIri;
    private final String typeIri;
    private final String subPropertyIri;
    private final String descriptionIri;
    private final String labelIri;
    private final String propertyTypeIri;
    private final String propertyLabelIri;
    private final String propertyDescriptionIri;
    private final String deprecationPropertyIri;

    private SchemaProfile(String aUiLabel, IRI aClassIri, IRI aSubclassIri, IRI aTypeIri,
            IRI aSubPropertyIri, IRI aDescriptionIri, IRI aLabelIri, IRI aPropertyTypeIri,
            IRI aPropertyLabelIri, IRI aPropertyDescriptionIri, IRI aDeprecationPropertyIri)
    {
        uiLabel = aUiLabel;
        classIri = aClassIri.stringValue();
        subclassIri = aSubclassIri.stringValue();
        subPropertyIri = aSubPropertyIri.stringValue();
        typeIri = aTypeIri.stringValue();
        descriptionIri = aDescriptionIri.stringValue();
        labelIri = aLabelIri.stringValue();
        propertyTypeIri = aPropertyTypeIri.stringValue();
        propertyLabelIri = aPropertyLabelIri.stringValue();
        propertyDescriptionIri = aPropertyDescriptionIri.stringValue();
        deprecationPropertyIri = aDeprecationPropertyIri.stringValue();
    }

    private SchemaProfile(String aUiLabel, String aClassIri, String aSubclassIri, String aTypeIri,
            String aSubPropertyIri, String aDescriptionIri, String aLabelIri,
            String aPropertyTypeIri, String aPropertyLabelIri, String aPropertyDescriptionIri,
            String aDeprecationPropertyIri)
    {
        uiLabel = aUiLabel;
        classIri = aClassIri;
        subclassIri = aSubclassIri;
        subPropertyIri = aSubPropertyIri;
        typeIri = aTypeIri;
        descriptionIri = aDescriptionIri;
        labelIri = aLabelIri;
        propertyTypeIri = aPropertyTypeIri;
        propertyLabelIri = aPropertyLabelIri;
        propertyDescriptionIri = aPropertyDescriptionIri;
        deprecationPropertyIri = aDeprecationPropertyIri;
    }

    public String getUiLabel()
    {
        return uiLabel;
    }

    public String getClassIri()
    {
        return classIri;
    }

    public String getSubclassIri()
    {
        return subclassIri;
    }

    public String getTypeIri()
    {
        return typeIri;
    }

    public String getSubPropertyIri()
    {
        return subPropertyIri;
    }

    public String getDescriptionIri()
    {
        return descriptionIri;
    }

    public String getLabelIri()
    {
        return labelIri;
    }

    public String getPropertyTypeIri()
    {
        return propertyTypeIri;
    }

    public String getPropertyLabelIri()
    {
        return propertyLabelIri;
    }

    public String getPropertyDescriptionIri()
    {
        return propertyDescriptionIri;
    }

    public String getDeprecationPropertyIri()
    {
        return deprecationPropertyIri;
    }

    /**
     * Check if the given profile equals one of the schema profiles defined in {@link SchemaProfile}
     * 
     * @return the corresponding schema profile (CUSTOM if the given profile does not equal any of
     *         the pre-defined ones
     */
    @SuppressWarnings("javadoc")
    public static SchemaProfile checkSchemaProfile(KnowledgeBaseProfile aProfile)
    {
        var profiles = SchemaProfile.values();
        var mapping = aProfile.getMapping();
        for (var i = 0; i < profiles.length; i++) {
            // Check if kb profile corresponds to a known schema profile
            if (equalsSchemaProfile(profiles[i], mapping.getClassIri(), mapping.getSubclassIri(),
                    mapping.getTypeIri(), mapping.getSubPropertyIri(), mapping.getDescriptionIri(),
                    mapping.getLabelIri(), mapping.getPropertyTypeIri(),
                    mapping.getPropertyLabelIri(), mapping.getPropertyDescriptionIri(),
                    mapping.getDeprecationPropertyIri())) {
                return profiles[i];
            }
        }
        // If the iris don't represent a known schema profile , return CUSTOM
        return SchemaProfile.CUSTOMSCHEMA;
    }

    /**
     * Check if the IRIs of the given {@link KnowledgeBase} object are equal to the IRIs of one of
     * the schema profiles defined in {@link SchemaProfile}
     * 
     * @return the corresponding schema profile (CUSTOM if the given profile does not equal any of
     *         the pre-defined ones
     */
    @SuppressWarnings("javadoc")
    public static SchemaProfile checkSchemaProfile(KnowledgeBase aKb)
    {
        var profiles = SchemaProfile.values();
        for (var i = 0; i < profiles.length; i++) {
            // Check if kb has a known schema profile
            if (equalsSchemaProfile(profiles[i], aKb.getClassIri(), aKb.getSubclassIri(),
                    aKb.getTypeIri(), aKb.getSubPropertyIri(), aKb.getDescriptionIri(),
                    aKb.getLabelIri(), aKb.getPropertyTypeIri(), aKb.getPropertyLabelIri(),
                    aKb.getPropertyDescriptionIri(), aKb.getDeprecationPropertyIri())) {
                return profiles[i];
            }
        }
        // If the iris don't represent a known schema profile , return CUSTOM
        return SchemaProfile.CUSTOMSCHEMA;
    }

    /**
     * Compares a schema profile to given IRIs. Returns true if the IRIs are the same as in the
     * profile
     */
    private static boolean equalsSchemaProfile(SchemaProfile profile, String classIri,
            String subclassIri, String typeIri, String subPropertyIri, String descriptionIri,
            String labelIri, String propertyTypeIri, String propertyLabelIri,
            String propertyDescriptionIri, String deprecationPropertyIri)
    {
        return Objects.equals(profile.getClassIri(), classIri)
                && Objects.equals(profile.getSubclassIri(), subclassIri)
                && Objects.equals(profile.getTypeIri(), typeIri)
                && Objects.equals(profile.getSubPropertyIri(), subPropertyIri)
                && Objects.equals(profile.getDescriptionIri(), descriptionIri)
                && Objects.equals(profile.getLabelIri(), labelIri)
                && Objects.equals(profile.getPropertyTypeIri(), propertyTypeIri)
                && Objects.equals(profile.getPropertyLabelIri(), propertyLabelIri)
                && Objects.equals(profile.getPropertyDescriptionIri(), propertyDescriptionIri)
                && Objects.equals(profile.getDeprecationPropertyIri(), deprecationPropertyIri);
    }
}
