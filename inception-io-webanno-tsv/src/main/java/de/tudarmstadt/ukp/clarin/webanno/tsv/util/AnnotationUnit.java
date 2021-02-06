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

package de.tudarmstadt.ukp.clarin.webanno.tsv.util;

/**
 * An UNIT to be exported in one line of a TSV file format (annotations separated by TAB character).
 * <br>
 * This UNIT can be a Token element or a sub-token element<br>
 * Sub-token elements start with the "--"
 */
public class AnnotationUnit
{
    public int begin;
    public int end;
    public String token;
    public boolean isSubtoken;

    public AnnotationUnit(int aBegin, int aEnd, boolean aIsSubToken, String aToken)
    {
        this.begin = aBegin;
        this.end = aEnd;
        this.isSubtoken = aIsSubToken;
        this.token = aToken;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + begin;
        result = prime * result + end;
        result = prime * result + (isSubtoken ? 1231 : 1237);
        return result;
    }

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
        AnnotationUnit other = (AnnotationUnit) obj;
        return begin == other.begin && end == other.end && isSubtoken == other.isSubtoken;
    }
}
