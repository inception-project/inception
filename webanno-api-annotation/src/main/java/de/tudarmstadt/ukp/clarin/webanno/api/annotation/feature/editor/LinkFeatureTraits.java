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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Traits for link features.
 */
public class LinkFeatureTraits
    implements Serializable
{
    private static final long serialVersionUID = -8450181605003189055L;

    private List<Long> defaultSlots = new ArrayList<>();

    public LinkFeatureTraits()
    {
        // Nothing to do
    }

    public List<Long> getDefaultSlots()
    {
        return defaultSlots;
    }

    public void setDefaultSlots(List<Long> aDefaultSlots)
    {
        defaultSlots = aDefaultSlots;
    }
}
