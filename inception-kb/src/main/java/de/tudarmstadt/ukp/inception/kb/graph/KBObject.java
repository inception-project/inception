/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.kb.graph;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.eclipse.rdf4j.model.util.URIUtil.getLocalNameIndex;
import static org.eclipse.rdf4j.model.util.URIUtil.isValidURIReference;

import java.io.Serializable;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

/**
 * A {@code KnowledgeGraphItem} represents any element (entity, type, relation, etc.) in the
 * knowledge graph. {@code KnowledgeGraphItem}s can be identified by a {@code String} identifier.
 * {@code KnowledgeGraphItem}s feature a label, a human-readable {@code String}.
 */
public interface KBObject
    extends Serializable
{

    /**
     * Returns the knowledge base of this element.
     * 
     * @return the {@link KnowledgeBase} for the object 
     */
    KnowledgeBase getKB();

    void setKB(KnowledgeBase kb);

    
    /**
     * Returns the unique identifier of this element.
     * 
     * @return the unique identifier
     */
    String getIdentifier();

    void setIdentifier(String aIdentifier);

    /**
     * @return the label of this element.
     */
    String getName();

    /**
     * Sets the label of this element.
     * @param label the label of this element
     */
    void setName(String label);
    
    String getDescription();
    
    void setDescription(String label);

    /**
     * Returns the language (e.g. of label and description) of this element.
     */
    String getLanguage();

    /**
     * Sets the language (e.g. of label and description) of this element.
     */
    void setLanguage(String language);

    /**
     * Returns the name of the {@code KBObject} if available, otherwise return the local name of its
     * IRI or, as a last resort, return the full identifier, e.g. if the identifier is not a valid
     * IRI or if the local name is empty.
     * 
     * @return a UI-friendly representation of this {@code KBObject}.
     */
    default String getUiLabel() {
        String name = getName();
        if (name != null) {
            return name;
        }

        if (isValidURIReference(getIdentifier())) {
            int localNameIndex = getLocalNameIndex(getIdentifier());
            String label = getIdentifier().substring(localNameIndex).replace('_', ' ');
            if (isNotBlank(label)) {
                return label;
            }
            else {
                return getIdentifier();
            }
        } else {
            return getIdentifier();
        }
    }
    
    /**
     * 
     * @return a {@code KBHandle} from {@code KBObject}
     */
    default KBHandle toKBHandle() {
        KBHandle handle = new KBHandle();
        handle.setIdentifier(getIdentifier());
        handle.setName(getName());
        handle.setLanguage(getLanguage());
        handle.setDescription(getDescription());
        return handle;
    }
}
