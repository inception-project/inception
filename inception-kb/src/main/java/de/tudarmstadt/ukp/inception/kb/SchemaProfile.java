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

import java.util.Map;
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseMapping;
import de.tudarmstadt.ukp.inception.kb.yaml.KnowledgeBaseProfile;

public enum SchemaProfile
{
    RDFSCHEMA("RDF", RDFS.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.SUBPROPERTYOF, RDFS.COMMENT,
            RDFS.LABEL, RDF.PROPERTY, RDFS.LABEL, RDFS.COMMENT),

    WIKIDATASCHEMA("WIKIDATA", IriConstants.WIKIDATA_CLASS, IriConstants.WIKIDATA_SUBCLASS,
            IriConstants.WIKIDATA_TYPE, RDFS.SUBPROPERTYOF, RDFS.COMMENT, RDFS.LABEL,
            IriConstants.WIKIDATA_PROPERTY_TYPE, RDFS.LABEL, RDFS.COMMENT),

    OWLSCHEMA("OWL", OWL.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.SUBPROPERTYOF, RDFS.COMMENT,
            RDFS.LABEL, RDF.PROPERTY, RDFS.LABEL, RDFS.COMMENT),

    SKOSSCHEMA("SKOS", SKOS.CONCEPT, SKOS.BROADER, RDF.TYPE, RDFS.SUBPROPERTYOF, RDFS.COMMENT,
            SKOS.PREF_LABEL, RDF.PROPERTY, SKOS.PREF_LABEL, RDFS.COMMENT),

    CUSTOMSCHEMA("CUSTOM", RDFS.CLASS, RDFS.SUBCLASSOF, RDF.TYPE, RDFS.SUBPROPERTYOF, RDFS.COMMENT,
            RDFS.LABEL, RDF.PROPERTY, RDFS.LABEL, RDFS.COMMENT);

    private final String uiLabel;
    private final IRI classIri;
    private final IRI subclassIri;
    private final IRI typeIri;
    private final IRI subPropertyIri;
    private final IRI descriptionIri;
    private final IRI labelIri;
    private final IRI propertyTypeIri;
    private final IRI propertyLabelIri;
    private final IRI propertyDescriptionIri;

    private SchemaProfile(String aUiLabel, IRI aClassIri, IRI aSubclassIri, IRI aTypeIri,
            IRI aSubPropertyIri, IRI aDescriptionIri, IRI aLabelIri, IRI aPropertyTypeIri,
            IRI aPropertyLabelIri, IRI aPropertyDescriptionIri)
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
    }

    public String getUiLabel()
    {
        return uiLabel;
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

    public IRI getSubPropertyIri()
    {
        return subPropertyIri;
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

    /**
     * Check if the given profile equals one of the schema profiles defined in {@link SchemaProfile}
     * @param aProfile
     * @return the corresponding schema profile (CUSTOM if the given profile does not equal any of
     * the pre-defined ones
     */
    public static SchemaProfile checkSchemaProfile(KnowledgeBaseProfile aProfile)
    {
        SchemaProfile[] profiles = SchemaProfile.values();
        KnowledgeBaseMapping mapping = aProfile.getMapping();
        for (int i = 0; i < profiles.length; i++) {
            // Check if kb profile corresponds to a known schema profile
            if (equalsSchemaProfileFirst(profiles[i], mapping.getClassIri(), mapping.getSubclassIri(),
                mapping.getTypeIri(), mapping.getSubPropertyIri(), mapping.getDescriptionIri())&&equalsSchemaProfile(profiles[i],
                        mapping.getLabelIri(), mapping.getPropertyTypeIri(), mapping.getPropertyLabelIri(),
                        mapping.getPropertyDescriptionIri())) {
                return profiles[i];
            }
        }
        // If the iris don't represent a known schema profile , return CUSTOM
        return SchemaProfile.CUSTOMSCHEMA;
    }

    /**
     * Check if the IRIs of the given {@link KnowledgeBase} object are equal to the IRIs of one of
     * the schema profiles defined in {@link SchemaProfile}
     * @param aKb
     * @return the corresponding schema profile (CUSTOM if the given profile does not equal any of
     * the pre-defined ones
     */
    public static SchemaProfile checkSchemaProfile(KnowledgeBase aKb)
    {
        SchemaProfile[] profiles = SchemaProfile.values();
        for (int i = 0; i < profiles.length; i++) {
            // Check if kb has a known schema profile
            if (equalsSchemaProfileFirst(profiles[i], aKb.getClassIri(), aKb.getSubclassIri(),
                aKb.getTypeIri(), aKb.getSubPropertyIri(),aKb.getDescriptionIri())&&equalsSchemaProfile(profiles[i],
                    aKb.getLabelIri(), aKb.getPropertyTypeIri(), aKb.getPropertyLabelIri(),
                    aKb.getPropertyDescriptionIri())) {
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
    private static boolean equalsSchemaProfileFirst(SchemaProfile profile, IRI classIri, IRI subclassIri,
            IRI typeIri, IRI subPropertyIri,IRI descriptionIri)
        {
            return Objects.equals(profile.getClassIri(), classIri) &&
                Objects.equals(profile.getSubclassIri(), subclassIri) &&
                Objects.equals(profile.getTypeIri(), typeIri) &&
                Objects.equals(profile.getSubPropertyIri(), subPropertyIri)&&
                Objects.equals(profile.getDescriptionIri(), descriptionIri);
        }
    
    private static boolean equalsSchemaProfile(SchemaProfile profile,  IRI labelIri, IRI propertyTypeIri,
        IRI propertyLabelIri, IRI propertyDescriptionIri)
    {
        return
            Objects.equals(profile.getLabelIri(), labelIri) &&
            Objects.equals(profile.getPropertyTypeIri(), propertyTypeIri) &&
            Objects.equals(profile.getPropertyLabelIri(), propertyLabelIri) &&
            Objects.equals(profile.getPropertyDescriptionIri(), propertyDescriptionIri);
    }
}
