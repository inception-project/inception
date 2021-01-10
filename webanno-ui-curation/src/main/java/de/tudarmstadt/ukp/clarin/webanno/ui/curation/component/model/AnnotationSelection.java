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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class comprises usernames, which agree on a specific annotation (same annotation type, same
 * annotation value, same position).
 */
public class AnnotationSelection
    implements Serializable
{
    private static final long serialVersionUID = -8839941493657969753L;

    private Map<String, Integer> addressByUsername = new LinkedHashMap<>();

    public AnnotationSelection()
    {
        // Nothing to do
    }

    public Map<String, Integer> getAddressByUsername()
    {
        return addressByUsername;
    }

    /**
     * Set Map of Username-Address-Tuples. The Map contains only annotations, which have the same
     * annotation type and annotation value, and the same position in the cas. <br>
     * <br>
     * Example:
     *
     * <pre>
     * {"Anno1": 1234, "Anno2": 1235}
     * </pre>
     *
     * @param aAddressByUsername
     *            HashMap of Username-Address-Tuples
     */
    public void setAddressByUsername(Map<String, Integer> aAddressByUsername)
    {
        addressByUsername = aAddressByUsername;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof AnnotationSelection)) {
            return false;
        }

        AnnotationSelection as = (AnnotationSelection) obj;
        return addressByUsername.equals(as.getAddressByUsername());
    }

    @Override
    public int hashCode()
    {
        return addressByUsername.hashCode();
    }
}
