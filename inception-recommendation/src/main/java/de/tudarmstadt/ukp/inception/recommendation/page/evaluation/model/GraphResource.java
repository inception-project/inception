/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.inception.recommendation.page.evaluation.model;

import java.awt.image.BufferedImage;
import java.util.List;

import org.apache.wicket.request.resource.DynamicImageResource;

import de.tudarmstadt.ukp.inception.recommendation.imls.core.dataobjects.ExtendedResult;
import de.tudarmstadt.ukp.inception.recommendation.imls.util.Reporter;

public class GraphResource
    extends DynamicImageResource 
{   
    private static final long serialVersionUID = 4871460002366212989L;
    
    private List<ExtendedResult> results2Plot;
    
    public GraphResource(List<ExtendedResult> results2Plot)
    {
        this.results2Plot = results2Plot;
        setFormat("png");
    }

    @Override
    protected byte[] getImageData(Attributes attributes)
    {
        BufferedImage bufferedImage = Reporter.plotToImage(results2Plot, 800, 600);
        return toImageData(bufferedImage);
    }
    
}
