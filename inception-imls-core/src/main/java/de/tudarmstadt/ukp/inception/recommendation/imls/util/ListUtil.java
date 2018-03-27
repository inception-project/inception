/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.imls.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Helper class for list manipulation.
 * 
 *
 *
 */
public class ListUtil
{
    private ListUtil()
    {
    }

    /** 
     * Flattens a list. 
     * 
     * @param source The source List of List of objects.
     * @return A single list of objects.
     */
    public static <T extends Object> List<T> flattenList(Collection<List<T>> source)
    {
        List<T> result = new LinkedList<T>();
        
        for (Collection<T> inner : source) {
            result.addAll(inner);
        }

        return result;
    }
}
