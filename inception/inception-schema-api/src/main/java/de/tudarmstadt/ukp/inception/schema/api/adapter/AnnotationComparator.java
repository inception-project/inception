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
package de.tudarmstadt.ukp.inception.schema.api.adapter;

import java.util.Comparator;

import org.apache.uima.cas.text.AnnotationFS;

/**
 * Sort ascending by begin and descending by end.
 */
public class AnnotationComparator
    implements Comparator<AnnotationFS>
{
    @Override
    public int compare(AnnotationFS arg0, AnnotationFS arg1)
    {
        int beginDiff = arg0.getBegin() - arg1.getBegin();
        if (beginDiff == 0) {
            return arg1.getEnd() - arg0.getEnd();
        }
        else {
            return beginDiff;
        }
    }
}
