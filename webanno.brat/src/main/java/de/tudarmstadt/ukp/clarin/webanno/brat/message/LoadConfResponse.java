/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

/**
 * Response for the {@code loadConf} command.
 */
public class LoadConfResponse
    extends AjaxResponse
{
    public static final String COMMAND = "loadConf";

    private String config = "{\"abbrevsOn\":true,\"textBackgrounds\":\"striped\",\"visual\":{\"margin\":{\"x\":2,\"y\":1},\"arcTextMargin\":1,\"boxSpacing\":1,\"curlyHeight\":4,\"arcSpacing\":9,\"arcStartHeight\":19},\"svgWidth\":\"100%\",\"rapidModeOn\":false,\"confirmModeOn\":true,\"autorefreshOn\":false}";

    public LoadConfResponse()
    {
        super(COMMAND);
    }

    public String getConfig()
    {
        return config;
    }

    public void setConfig(String aConfig)
    {
        config = aConfig;
    }
}
