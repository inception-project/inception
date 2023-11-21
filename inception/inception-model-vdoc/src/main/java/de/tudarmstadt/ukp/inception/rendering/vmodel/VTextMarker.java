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

public class VTextMarker
    extends VMarker
{
    private static final long serialVersionUID = -963583831065999692L;

    private final String type;
    private final VRange range;

    public VTextMarker(String aType, VRange aRange)
    {
        this(null, aType, aRange);
    }

    public VTextMarker(Object aSource, String aType, VRange aRange)
    {
        super(aSource);
        type = aType;
        range = aRange;
    }

    public VRange getRange()
    {
        return range;
    }

    @Override
    public String getType()
    {
        return type;
    }
}
