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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface DataSplitter
{
    public static enum TargetSet
    {
        TRAIN, TEST, IGNORE
    }

    TargetSet getTargetSet(Object aObject);

    default <T> void apply(Iterable<T> data, Collection<T> aTrain, Collection<T> aTest)
    {
        for (var nameSample : data) {
            switch (getTargetSet(nameSample)) {
            case TRAIN:
                aTrain.add(nameSample);
                break;
            case TEST:
                aTest.add(nameSample);
                break;
            default:
                // Do nothing
                break;
            }
        }
    }

    default <T> SplitResult<List<T>> apply(Iterable<T> data)
    {
        var trainingSet = new ArrayList<T>();
        var testSet = new ArrayList<T>();
        apply(data, trainingSet, testSet);
        return new SplitResult<List<T>>(trainingSet, testSet);
    }

    record SplitResult<C>(C trainingSet, C testSet) {}
}
