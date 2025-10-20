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
package de.tudarmstadt.ukp.inception.recommendation.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportQuery;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportRegistry;
import de.tudarmstadt.ukp.inception.support.extensionpoint.ExtensionPoint_ImplBase;

public class SuggestionSupportRegistryImpl
    extends ExtensionPoint_ImplBase<SuggestionSupportQuery, SuggestionSupport>
    implements SuggestionSupportRegistry
{
    @Autowired
    public SuggestionSupportRegistryImpl(
            @Lazy @Autowired(required = false) List<SuggestionSupport> aExtensions)
    {
        super(aExtensions);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <X extends SuggestionSupport> Optional<X> findGenericExtension(
            SuggestionSupportQuery aKey)
    {
        return getExtensions().stream() //
                .filter(e -> e.accepts(aKey)) //
                .map(e -> (X) e) //
                .findFirst();
    }
}
