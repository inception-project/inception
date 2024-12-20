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
package de.tudarmstadt.ukp.inception.externaleditor.model;

import java.util.List;

public class AnnotationEditorProperties
{
    private String editorFactoryId;
    private String editorFactory;
    private String diamAjaxCallbackUrl;
    private String diamWsUrl;
    private String csrfToken;
    private String userPreferencesKey;
    private List<String> scriptSources;
    private List<String> stylesheetSources;
    private List<String> sectionElements;
    private boolean loadingIndicatorDisabled = false;

    public String getEditorFactory()
    {
        return editorFactory;
    }

    public void setEditorFactory(String aEditorFactory)
    {
        editorFactory = aEditorFactory;
    }

    public String getDiamAjaxCallbackUrl()
    {
        return diamAjaxCallbackUrl;
    }

    public void setDiamAjaxCallbackUrl(String aDiamAjaxCallbackUrl)
    {
        diamAjaxCallbackUrl = aDiamAjaxCallbackUrl;
    }

    public String getDiamWsUrl()
    {
        return diamWsUrl;
    }

    public void setDiamWsUrl(String aDiamWsUrl)
    {
        diamWsUrl = aDiamWsUrl;
    }

    public List<String> getScriptSources()
    {
        return scriptSources;
    }

    public void setScriptSources(List<String> aScriptSources)
    {
        scriptSources = aScriptSources;
    }

    public List<String> getStylesheetSources()
    {
        return stylesheetSources;
    }

    public void setStylesheetSources(List<String> aStylesheetSources)
    {
        stylesheetSources = aStylesheetSources;
    }

    public boolean isLoadingIndicatorDisabled()
    {
        return loadingIndicatorDisabled;
    }

    public void setLoadingIndicatorDisabled(boolean aLoadingIndicatorDisabled)
    {
        loadingIndicatorDisabled = aLoadingIndicatorDisabled;
    }

    public void setEditorFactoryId(String aEditorFactoryId)
    {
        editorFactoryId = aEditorFactoryId;
    }

    public String getEditorFactoryId()
    {
        return editorFactoryId;
    }

    public void setUserPreferencesKey(String aUserPreferencesKey)
    {
        userPreferencesKey = aUserPreferencesKey;
    }

    public String getUserPreferencesKey()
    {
        return userPreferencesKey;
    }

    public List<String> getSectionElements()
    {
        return sectionElements;
    }

    public void setSectionElements(List<String> aSectionElements)
    {
        sectionElements = aSectionElements;
    }

    public String getCsrfToken()
    {
        return csrfToken;
    }

    public void setCsrfToken(String aCsrfToken)
    {
        csrfToken = aCsrfToken;
    }
}
