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
package de.tudarmstadt.ukp.inception.experimental.api.model;

import java.util.ArrayList;
import java.util.List;

public class Viewport
{
    private List<List<Integer>> viewport;
    private List<String> disabledLayers;

    public Viewport()
    {
    }

    public Viewport(List<List<Integer>> aViewport)
    {
        viewport = aViewport;
    }

    public Viewport(List<List<Integer>> aViewport, List<String> aDisabledLayers)
    {
        viewport = aViewport;
        disabledLayers = aDisabledLayers;
    }

    public List<List<Integer>> getViewport()
    {
        return viewport;
    }

    public void setViewport(List<List<Integer>> aViewport)
    {
        viewport = aViewport;
    }

    public List<String> getDisabledLayers()
    {
        if (disabledLayers == null) {
            return new ArrayList<>();
        }
        return disabledLayers;
    }

    public void setDisabledLayers(List<String> aDisabledLayers)
    {
        disabledLayers = aDisabledLayers;
    }
}
