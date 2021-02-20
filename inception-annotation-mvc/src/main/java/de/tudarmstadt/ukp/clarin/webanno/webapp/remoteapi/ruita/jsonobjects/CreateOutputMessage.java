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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

public class CreateOutputMessage
    extends Message
{
    int createdAnnotationId;
    long layerId;
    String layerUiName;

    public CreateOutputMessage()
    {

    }

    public CreateOutputMessage(String text, int createdAnnotationId, long layerId,
            String layerUiName)
    {
        super(text);
        this.createdAnnotationId = createdAnnotationId;
        this.layerId = layerId;
        this.layerUiName = layerUiName;
    }

    public String getLayerUiName()
    {
        return layerUiName;
    }

    public void setLayerUiName(String layerUiName)
    {
        this.layerUiName = layerUiName;
    }

    public int getCreatedAnnotationId()
    {
        return createdAnnotationId;
    }

    public void setCreatedAnnotationId(int createdAnnotationId)
    {
        this.createdAnnotationId = createdAnnotationId;
    }

    public long getLayerId()
    {
        return layerId;
    }

    public void setLayerId(int layerId)
    {
        this.layerId = layerId;
    }

}
