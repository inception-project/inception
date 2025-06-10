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

import de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link CurationServiceAutoConfiguration#thresholdBasedMergeStrategyFactory}.
 * </p>
 */
public class ThresholdBasedMergeStrategyFactoryImpl
    extends MergeStrategyFactory_ImplBase<ThresholdBasedMergeStrategyTraits>
    implements ThresholdBasedMergeStrategyFactory
{
    public static final String BEAN_NAME = "thresholdBased";

    @Override
    public String getId()
    {
        return BEAN_NAME;
    }

    @Override
    public String getLabel()
    {
        return "Merge using thresholds";
    }

    @Override
    protected ThresholdBasedMergeStrategyTraits createTraits()
    {
        return new ThresholdBasedMergeStrategyTraits();
    }

    @Override
    public MergeStrategy makeStrategy(ThresholdBasedMergeStrategyTraits aTraits)
    {
        return ThresholdBasedMergeStrategy.builder() //
                .withUserThreshold(aTraits.getUserThreshold()) //
                .withConfidenceThreshold(aTraits.getConfidenceThreshold()) //
                .withTopRanks(aTraits.getTopRanks()) //
                .build();
    }

    @Override
    public Component createTraitsEditor(String aId, IModel<CurationWorkflow> aModel)
    {
        return new ThresholdBasedMergeStrategyTraitsEditor(aId, aModel);
    }
}
