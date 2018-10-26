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
package de.tudarmstadt.ukp.inception.recommendation.api.evaluation;

import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter.TargetSet.IGNORE;
import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter.TargetSet.TEST;
import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter.TargetSet.TRAIN;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

public class IncrementalSplitter
    implements DataSplitter, Iterator<DataSplitter>
{
    private final int trainBatchSize;
    private final int testBatchSize;
    private final int lowSampleThreshold;
    private int trainCount;
    private int testCount;
    private int ignoreCount;
    
    private final int increment;
    private int limit;
    private boolean hitTheLimit = true;

    public IncrementalSplitter(double aTrainPercentage, int aIncrement, int aLowSampleThreshold)
    {
        Validate.inclusiveBetween(0, 1, aTrainPercentage, "Percentage has to be in (0,1)");

        trainBatchSize = (int) Math.round(10 * aTrainPercentage);
        testBatchSize = 10 - trainBatchSize;
        increment = aIncrement;
        lowSampleThreshold = aLowSampleThreshold;
    }
    
    public IncrementalSplitter(int aTrainBatchSize, int aTestBatchSize, int aIncrement,
            int aLowSampleThreshold)
    {
        trainBatchSize = aTrainBatchSize;
        testBatchSize = aTestBatchSize;
        increment = aIncrement;
        lowSampleThreshold = aLowSampleThreshold;
    }

    @Override
    public TargetSet getTargetSet(Object aObject)
    {
        int module = trainBatchSize + testBatchSize;
        int count = trainCount + testCount + ignoreCount;
        
        TargetSet target;
        // Low sample count behavior
        if (count < lowSampleThreshold) {
            target = (count % 2) == 0 ? TRAIN : TEST;
        }
        // Regular behavior
        else if (trainCount < trainBatchSize && trainCount < limit) {
            target = TRAIN;
        }
        else if (testCount < testBatchSize) {
            target = TEST;
        }
        else if (ignoreCount < (trainBatchSize - limit)) {
            target = IGNORE;
            hitTheLimit = true;
        }
        else {
            target = count % module < trainBatchSize ? TRAIN : TEST;
        }
        
        if (trainCount >= limit && target == TRAIN) {
            target = IGNORE;
            hitTheLimit = true;
        }
        
        switch (target) {
        case TRAIN:
            trainCount++;
            break;
        case TEST:
            testCount++;
            break;
        case IGNORE:
            ignoreCount++;
            break;
        default:
            throw new IllegalStateException("Invalid target set [" + target + "]");
        }

        return target;
    }

    @Override
    public boolean hasNext()
    {
        return hitTheLimit;
    }

    @Override
    public DataSplitter next()
    {
        trainCount = 0;
        testCount = 0;
        ignoreCount = 0;
        hitTheLimit = false;
        
        limit += increment;
        
        return this;
    }
}
