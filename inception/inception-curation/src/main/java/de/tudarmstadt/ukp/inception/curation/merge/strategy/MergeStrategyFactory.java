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
package de.tudarmstadt.ukp.inception.curation.merge.strategy;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.support.extensionpoint.Extension;

public interface MergeStrategyFactory<T>
    extends Extension<Project>

{
    String getLabel();

    @Override
    default boolean accepts(Project aContext)
    {
        return true;
    }

    T readTraits(CurationWorkflow aCurationWorkflow);

    void writeTraits(CurationWorkflow aCurationWorkflow, T aTrait);

    MergeStrategy makeStrategy(T aTraits);

    Component createTraitsEditor(String aString, IModel<CurationWorkflow> aModel);
}
