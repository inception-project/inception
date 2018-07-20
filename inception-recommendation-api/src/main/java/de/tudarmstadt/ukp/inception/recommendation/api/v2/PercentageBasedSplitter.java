/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.api.v2;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.Validate;

public class PercentageBasedSplitter
    implements DataSplitter
{
    private final double trainPercentage;
    private Map<Class<?>, Integer> counts;
    private Optional<Integer> total;

    public PercentageBasedSplitter(double aTrainPercentage) {
        Validate.inclusiveBetween(0, 1, aTrainPercentage, "Percentage has to be in (0,1)");

        trainPercentage = aTrainPercentage;
        counts = new HashMap<>();
        total = Optional.empty();
    }

    @Override
    public void setTotal(int aTotal) {
        total = Optional.of(aTotal);
    }

    @Override
    public boolean belongsToTrainingSet(Object aObject) {
        int count = counts.getOrDefault(aObject.getClass(), 0);
        counts.put(aObject.getClass(), count + 1);
        return total.orElseThrow(this::totalNotSet) * trainPercentage > count;
    }

    private RuntimeException totalNotSet() {
        return new IllegalStateException("Total has to be set before querying!");
    }
}
