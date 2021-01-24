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
package de.tudarmstadt.ukp.inception.recommendation.model;

import java.io.Serializable;
import java.util.Map;

public class LearningCurve
    implements Serializable
{
    private static final long serialVersionUID = 6542281189066805238L;

    private Map<String, String> curveData;
    private String xAxis;

    public Map<String, String> getCurveData()
    {
        return curveData;
    }

    public void setCurveData(Map<String, String> aCurveData)
    {
        curveData = aCurveData;
    }

    public String getXaxis()
    {
        return xAxis;
    }

    public void setXaxis(String aXAxis)
    {
        this.xAxis = aXAxis;
    }
}
