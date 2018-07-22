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

import java.util.Iterator;
import java.util.Optional;

import org.apache.commons.lang3.Validate;

public class IncrementalSplitter
    implements DataSplitter, Iterator<DataSplitter>
{
    private final double trainPercentage;
    private final int increment;
    private int count;
    private int limit;
    private Optional<Integer> total;

    public IncrementalSplitter(double aTrainPercentage, int aIncrement) {
        Validate.inclusiveBetween(0, 1, aTrainPercentage, "Percentage has to be in (0,1)");

        trainPercentage = aTrainPercentage;
        total = Optional.empty();
        increment = aIncrement;
    }

    @Override
    public void setTotal(int aTotal) {
        total = Optional.of(aTotal);
    }

    @Override
    public TargetSet getTargetSet(Object aObject)
    {
        long upperBound = Math.round(total.orElseThrow(this::totalNotSet) * trainPercentage);
        
        // Is the data in the training set?
        TargetSet target;
        if (count < Math.min(limit, upperBound)) {
            target = TargetSet.TRAIN;
        }
        else if (count >= upperBound) {
            target = TargetSet.TEST;
        }
        else {
            target = TargetSet.IGNORE;
        }
        
        count++;

        return target;
    }

    private RuntimeException totalNotSet() {
        return new IllegalStateException("Total has to be set before querying!");
    }

    @Override
    public boolean hasNext()
    {
        if (total.isPresent()) {
            return limit + increment <= total.get() * trainPercentage;
        }
        else {
            // The total is only set by the evaluate method, but the incremental loop is outside
            // so we need to return true here to give the evaluate method to set the total
            return true;
        }
    }

    @Override
    public DataSplitter next()
    {
        count = 0;
        limit += increment;
        
        return this;
    }
}
