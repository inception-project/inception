/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.recommendation.sidebar;

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.inception.recommendation.model.RecommenderEvaluationScoreMetricEnum;

public class DropDownEvent
{
    public RecommenderEvaluationScoreMetricEnum selectedValue;
    public AjaxRequestTarget target;

    public RecommenderEvaluationScoreMetricEnum getSelectedValue()
    {
        return selectedValue;
    }

    public void setSelectedValue(RecommenderEvaluationScoreMetricEnum selectedValue)
    {
        this.selectedValue = selectedValue;
    }

    public AjaxRequestTarget getTarget()
    {
        return target;
    }

    public void setTarget(AjaxRequestTarget target)
    {
        this.target = target;
    }
}
