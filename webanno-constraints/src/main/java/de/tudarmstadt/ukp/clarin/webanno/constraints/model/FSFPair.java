/*
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.constraints.model;

import java.io.Serializable;

public class FSFPair
    implements Serializable
{
    /**
     * Pair of input Feature Structure and target feature which can be affected by a Constraint
     * rule.
     */
    private static final long serialVersionUID = -7207254925786134633L;
    private String featureStructure;
    private String affectedFeature;

    public FSFPair(String featureStructure, String affectedFeature)
    {
        this.featureStructure = featureStructure;
        this.affectedFeature = affectedFeature;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((affectedFeature == null) ? 0 : affectedFeature.hashCode());
        result = prime * result + ((featureStructure == null) ? 0 : featureStructure.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FSFPair other = (FSFPair) obj;
        if (affectedFeature == null) {
            if (other.affectedFeature != null) {
                return false;
            }
        }
        else if (!affectedFeature.equals(other.affectedFeature)) {
            return false;
        }
        if (featureStructure == null) {
            if (other.featureStructure != null) {
                return false;
            }
        }
        else if (!featureStructure.equals(other.featureStructure)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        return "FSFPair [featureStructure=" + featureStructure + ", affectedFeature="
                + affectedFeature + "]";
    }
}
