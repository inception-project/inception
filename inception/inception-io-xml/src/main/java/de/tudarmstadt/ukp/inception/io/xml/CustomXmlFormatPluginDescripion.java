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
package de.tudarmstadt.ukp.inception.io.xml;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class CustomXmlFormatPluginDescripion
    implements Serializable
{
    private static final long serialVersionUID = 7985647290137191912L;

    private String id;
    private String name;

    private List<String> stylesheets = Collections.emptyList();
    private List<String> sectionElements;
    private List<String> blockElements;
    private boolean splitSentencesInBlockElements;

    private @JsonIgnore Path basePath;

    public void setId(String aId)
    {
        id = aId;
    }

    public String getId()
    {
        return id;
    }

    public void setBasePath(Path aBasePath)
    {
        basePath = aBasePath;
    }

    public Path getBasePath()
    {
        return basePath;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String aName)
    {
        name = aName;
    }

    public List<String> getStylesheets()
    {
        return stylesheets;
    }

    public void setStylesheets(List<String> aStylesheets)
    {
        stylesheets = aStylesheets;
    }

    public List<String> getSectionElements()
    {
        return sectionElements;
    }

    public void setSectionElements(List<String> aSectionElements)
    {
        sectionElements = aSectionElements;
    }

    public void setBlockElements(List<String> aBlockElements)
    {
        blockElements = aBlockElements;
    }

    public List<String> getBlockElements()
    {
        return blockElements;
    }

    public void setSplitSentencesInBlockElements(boolean aSplitSentencesInBlockElements)
    {
        splitSentencesInBlockElements = aSplitSentencesInBlockElements;
    }

    public boolean isSplitSentencesInBlockElements()
    {
        return splitSentencesInBlockElements;
    }
}
