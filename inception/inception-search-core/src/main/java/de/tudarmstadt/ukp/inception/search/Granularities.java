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
package de.tudarmstadt.ukp.inception.search;

import java.util.ArrayList;
import java.util.List;

public enum Granularities
{
    PER_DOCUMENT, PER_SENTENCE;

    public static String internalToUi(Granularities aGranularity)
    {
        switch (aGranularity) {
        case PER_DOCUMENT:
            return "per Document";
        default:
            return "per Sentence";
        }
    }

    public static Granularities uiToInternal(String aGranularity) throws ExecutionException
    {
        switch (aGranularity) {
        case "per Document":
            return PER_DOCUMENT;
        case "per Sentence":
            return PER_SENTENCE;
        default:
            throw new ExecutionException("The granularity " + aGranularity + " is not supported!");
        }
    }

    public static List<String> uiList()
    {
        List<String> granList = new ArrayList<String>();
        for (Granularities granularity : Granularities.values()) {
            granList.add(internalToUi(granularity));
        }
        return granList;
    }

}
