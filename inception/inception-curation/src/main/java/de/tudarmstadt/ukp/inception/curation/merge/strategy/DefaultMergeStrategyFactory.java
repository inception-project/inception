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
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationServiceAutoConfiguration#defaultMergingStrategyFactory}.
 * </p>
 */
public class DefaultMergeStrategyFactory
    extends MergeStrategyFactory_ImplBase<Void>
{
    public static final String BEAN_NAME = "completeAgreementNonStacked";

    @Override
    public String getId()
    {
        return BEAN_NAME;
    }

    @Override
    public String getLabel()
    {
        return "Merge completely agreeing non-stacked annotations";
    }

    @Override
    protected Void createTraits()
    {
        // No traits
        return null;
    }

    @Override
    public DefaultMergeStrategy makeStrategy(Void aTraits)
    {
        return new DefaultMergeStrategy();
    }

    @Override
    public Component createTraitsEditor(String aString, IModel<CurationWorkflow> aModel)
    {
        // No traits
        return new EmptyPanel(aString);
    }
}
