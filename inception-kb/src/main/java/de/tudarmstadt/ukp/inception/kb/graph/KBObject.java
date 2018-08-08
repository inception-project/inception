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

import java.io.Serializable;

import org.eclipse.rdf4j.model.util.URIUtil;

/**
 * A {@code KnowledgeGraphItem} represents any element (entity, type, relation, etc.) in the
 * knowledge graph. {@code KnowledgeGraphItem}s can be identified by a {@code String} identifier.
 * {@code KnowledgeGraphItem}s feature a label, a human-readable {@code String}.
 */
public interface KBObject
    extends Serializable
{

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

    /**
     * Returns a UI-friendly representation of this {@code KBObject}.
     * 
     * @return the name of the {@code KBObject} if available, otherwise return the local name of its
     *         IRI or, as a last resort, return the full identifier if the identifier is not a valid
     *         IRI
     */
    default String getUiLabel() {
        String name = getName();
        if (name != null) {
            return name;
        }

        if (URIUtil.isValidURIReference(getIdentifier())) {
            int localNameIndex = URIUtil.getLocalNameIndex(getIdentifier());
            return getIdentifier().substring(localNameIndex).replace('_', ' ');
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
        return handle;
    }
}
