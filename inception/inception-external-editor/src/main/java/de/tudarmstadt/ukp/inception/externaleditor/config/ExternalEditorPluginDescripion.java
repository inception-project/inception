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

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ExternalEditorPluginDescripion
    implements Serializable
{
    private static final long serialVersionUID = 4400329006838299692L;

    private String id;
    private String factory;
    private String name;
    private String view;

    private List<String> scripts = Collections.emptyList();
    private List<String> stylesheets = Collections.emptyList();
    private List<String> sectionElements = Collections.emptyList();

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

    public String getView()
    {
        return view;
    }

    public void setView(String aView)
    {
        view = aView;
    }

    public String getFactory()
    {
        return factory;
    }

    public void setFactory(String aImplementation)
    {
        factory = aImplementation;
    }

    public List<String> getScripts()
    {
        return scripts;
    }

    public void setScripts(List<String> aScripts)
    {
        scripts = aScripts;
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
}
