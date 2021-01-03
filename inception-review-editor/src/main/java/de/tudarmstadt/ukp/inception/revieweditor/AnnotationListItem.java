/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.revieweditor;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public class AnnotationListItem
{
    private final int addr;
    private final String label;
    private final AnnotationLayer layer;

    public AnnotationListItem(int aAddr, String aLabel, AnnotationLayer aLayer)
    {
        super();
        addr = aAddr;
        label = aLabel;
        layer = aLayer;
    }

    public int getAddr()
    {
        return addr;
    }

    public String getLabel()
    {
        return label;
    }

    public AnnotationLayer getLayer()
    {
        return layer;
    }
}
