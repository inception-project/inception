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

public class VAnnotationMarker
    extends VMarker
{
    private static final long serialVersionUID = -5386606974045570687L;

    private final VID[] vid;
    private String type;

    public VAnnotationMarker(String aType, VID aVid)
    {
        this(null, aType, aVid);
    }

    public VAnnotationMarker(Object aSource, String aType, VID aVid)
    {
        super(aSource);
        vid = new VID[] { aVid };
        type = aType;
    }

    public VID getVid()
    {
        return vid[0];
    }

    @Override
    public String getType()
    {
        return type;
    }
}
