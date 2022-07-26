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
package de.tudarmstadt.ukp.inception.rendering.vmodel;

public class VSentenceMarker
    extends VMarker
{
    private static final long serialVersionUID = 6070738111254385502L;

    private final String type;
    private final int index;

    public VSentenceMarker(String aType, int aIndex)
    {
        this(null, aType, aIndex);
    }

    public VSentenceMarker(Object aSource, String aType, int aIndex)
    {
        super(aSource);
        type = aType;
        index = aIndex;
    }

    public int getIndex()
    {
        return index;
    }

    @Override
    public String getType()
    {
        return type;
    }
}
