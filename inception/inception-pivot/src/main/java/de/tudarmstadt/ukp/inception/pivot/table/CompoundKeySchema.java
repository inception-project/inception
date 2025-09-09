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
package de.tudarmstadt.ukp.inception.pivot.table;

import static java.util.Arrays.asList;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import de.tudarmstadt.ukp.inception.pivot.api.extractor.Extractor;

public final class CompoundKeySchema
    implements Serializable
{
    private static final long serialVersionUID = 1533797376830520422L;

    private final String[] keyNames;
    private final boolean weak;

    public CompoundKeySchema(boolean aWeak, String... aKeys)
    {
        keyNames = aKeys;
        weak = aWeak;
    }

    public CompoundKeySchema(Extractor<?, ?>... aExtractors)
    {
        keyNames = asList(aExtractors).stream() //
                .map(Extractor::getName) //
                .distinct() //
                .toArray(String[]::new);

        weak = asList(aExtractors).stream() //
                .map(Extractor::isWeak) //
                .allMatch(w -> w == true);
    }

    public boolean isWeak()
    {
        return weak;
    }

    public int size()
    {
        return keyNames.length;
    }

    public int getIndex(String aName) {
        return ArrayUtils.indexOf(keyNames, aName);
    }
    
    public String getName(int index)
    {
        return keyNames[index];
    }

    @Override
    public String toString()
    {
        return Arrays.toString(keyNames);
    }
}
