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
package de.tudarmstadt.ukp.inception.editor;

import java.util.HashSet;
import java.util.Set;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.request.cycle.RequestCycle;

public class AnnotationEditorRenderedMetaDataKey
    extends MetaDataKey<Set<String>>
{
    private static final long serialVersionUID = 102615176759478581L;

    public final static AnnotationEditorRenderedMetaDataKey INSTANCE = new AnnotationEditorRenderedMetaDataKey();

    public static Set<String> get()
    {
        RequestCycle requestCycle = RequestCycle.get();
        Set<String> renderedEditors = requestCycle.getMetaData(INSTANCE);
        if (renderedEditors == null) {
            renderedEditors = new HashSet<>();
            requestCycle.setMetaData(INSTANCE, renderedEditors);
        }
        return renderedEditors;
    }
}
