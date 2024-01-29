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
package de.tudarmstadt.ukp.inception.externaleditor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ui.external")
public class ExternalEditorPropertiesImpl
    implements ExternalEditorProperties
{
    private boolean blockStyle = true;

    private boolean blockImg = true;
    private boolean blockEmbed = true;
    private boolean blockAudio = true;
    private boolean blockObject = true;
    private boolean blockVideo = true;

    @Override
    public boolean isBlockStyle()
    {
        return blockStyle;
    }

    public void setBlockStyle(boolean aBlockStyle)
    {
        blockStyle = aBlockStyle;
    }

    @Override
    public boolean isBlockImg()
    {
        return blockImg;
    }

    public void setBlockImg(boolean aBlockImg)
    {
        blockImg = aBlockImg;
    }

    @Override
    public boolean isBlockEmbed()
    {
        return blockEmbed;
    }

    public void setBlockEmbed(boolean aBlockEmbed)
    {
        blockEmbed = aBlockEmbed;
    }

    @Override
    public boolean isBlockAudio()
    {
        return blockAudio;
    }

    public void setBlockAudio(boolean aBlockAudio)
    {
        blockAudio = aBlockAudio;
    }

    @Override
    public boolean isBlockObject()
    {
        return blockObject;
    }

    public void setBlockObject(boolean aBlockObject)
    {
        blockObject = aBlockObject;
    }

    @Override
    public boolean isBlockVideo()
    {
        return blockVideo;
    }

    public void setBlockVideo(boolean aBlockVideo)
    {
        blockVideo = aBlockVideo;
    }
}
