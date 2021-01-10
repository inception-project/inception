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
package de.tudarmstadt.ukp.inception.recommendation.api.evaluation;

import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter.TargetSet.TEST;
import static de.tudarmstadt.ukp.inception.recommendation.api.evaluation.DataSplitter.TargetSet.TRAIN;

import org.apache.commons.lang3.Validate;

public class PercentageBasedSplitter
    implements DataSplitter
{
    private final int trainBatchSize;
    private final int testBatchSize;
    private final int lowSampleThreshold;

    private int trainCount;
    private int testCount;

    public PercentageBasedSplitter(double aTrainPercentage, int aLowSampleThreshold)
    {
        Validate.inclusiveBetween(0, 1, aTrainPercentage, "Percentage has to be in (0,1)");

        trainBatchSize = (int) Math.round(10 * aTrainPercentage);
        testBatchSize = 10 - trainBatchSize;
        lowSampleThreshold = aLowSampleThreshold;
    }

    public PercentageBasedSplitter(int aTrainBatchSize, int aTestBatchSize, int aLowSampleThreshold)
    {
        trainBatchSize = aTrainBatchSize;
        testBatchSize = aTestBatchSize;
        lowSampleThreshold = aLowSampleThreshold;
    }

    @Override
    public TargetSet getTargetSet(Object aObject)
    {
        int module = trainBatchSize + testBatchSize;
        int count = trainCount + testCount;

        TargetSet target;
        // Low sample count behavior
        if (count < lowSampleThreshold) {
            target = (count % 2) == 0 ? TRAIN : TEST;
        }
        // Regular behavior
        else if (trainCount < trainBatchSize) {
            target = TRAIN;
        }
        else if (testCount < testBatchSize) {
            target = TEST;
        }
        else {
            target = count % module < trainBatchSize ? TRAIN : TEST;
        }

        switch (target) {
        case TRAIN:
            trainCount++;
            break;
        case TEST:
            testCount++;
            break;
        default:
            throw new IllegalStateException("Invalid target set [" + target + "]");
        }

        return target;
    }
}
