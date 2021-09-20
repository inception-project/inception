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
package de.tudarmstadt.ukp.inception.recommendation.chart.vue;

import org.apache.wicket.request.resource.CssResourceReference;


public class EvalChartCssReference
    extends CssResourceReference
{
    private static final long serialVersionUID = 1L;

    private static final EvalChartCssReference INSTANCE = new EvalChartCssReference();

    /**
     * Gets the instance of the resource reference
     *
     * @return the single instance of the resource reference
     */
    public static EvalChartCssReference get()
    {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private EvalChartCssReference()
    {
        super(EvalChartCssReference.class, "evalChart.css");
    }
}
