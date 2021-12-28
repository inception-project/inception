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

public class EditorPluginDescripion
    implements Serializable
{
    private static final long serialVersionUID = 4400329006838299692L;
    
    private static final String DEFAULT_PLUGIN_CSS = "plugin.css";
    private static final String DEFAULT_PLUGIN_JS = "plugin.js";
    
    private Path basePath;
    private String factory;
    private String name;
    private String annotationFormat;
    private String view;
    private String js = DEFAULT_PLUGIN_JS;
    private String css = DEFAULT_PLUGIN_CSS;

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

    public String getAnnotationFormat()
    {
        return annotationFormat;
    }

    public void setAnnotationFormat(String aAnnotationFormat)
    {
        annotationFormat = aAnnotationFormat;
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

    public String getJs()
    {
        return js;
    }

    public void setJs(String aJs)
    {
        js = aJs;
    }

    public String getCss()
    {
        return css;
    }

    public void setCss(String aCss)
    {
        css = aCss;
    }
}
