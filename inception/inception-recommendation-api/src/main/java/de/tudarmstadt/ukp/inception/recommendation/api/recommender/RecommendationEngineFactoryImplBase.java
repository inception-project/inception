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
package de.tudarmstadt.ukp.inception.recommendation.api.recommender;

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toJsonString;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

public abstract class RecommendationEngineFactoryImplBase<T>
    implements RecommendationEngineFactory<T>
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public AbstractTraitsEditor createTraitsEditor(String aId, IModel<Recommender> aModel)
    {
        return new DefaultTrainableRecommenderTraitsEditor(aId, aModel);
    }

    @Override
    public T createTraits()
    {
        return null;
    }

    @Override
    public boolean isDeprecated()
    {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T readTraits(Recommender aRecommender)
    {
        if (aRecommender.getTraits() == null) {
            return createTraits();
        }

        T traits = null;
        try {
            traits = fromJsonString((Class<T>) createTraits().getClass(), aRecommender.getTraits());
        }
        catch (IOException e) {
            LOG.error("Error while reading traits", e);
        }

        if (traits == null) {
            traits = createTraits();
        }

        return traits;
    }

    @Override
    public void writeTraits(Recommender aRecommender, T aTraits)
    {
        try {
            String json = toJsonString(aTraits);
            aRecommender.setTraits(json);
        }
        catch (IOException e) {
            LOG.error("Error while writing traits", e);
        }
    }
}
