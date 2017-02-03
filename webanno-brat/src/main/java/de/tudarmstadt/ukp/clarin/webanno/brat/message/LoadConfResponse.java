/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

/**
 * Response for the {@code loadConf} command.
 */
public class LoadConfResponse
    extends AjaxResponse
{
    public static final String COMMAND = "loadConf";

    private BratConfig config = new BratConfig();

    public LoadConfResponse()
    {
        super(COMMAND);
    }

    public BratConfig getConfig()
    {
        return config;
    }

    public void setConfig(BratConfig aConfig)
    {
        config = aConfig;
    }

    public static boolean is(String aCommand)
    {
        return COMMAND.equals(aCommand);
    }
    
    @JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY)
    public static class BratConfig
    {
        public boolean abbrevsOn = true;
        public String textBackgrounds = "striped";
        public String svgWidth = "100%";
        public boolean rapidModeOn = false;
        public boolean confirmModeOn = true;
        public boolean autorefreshOn = false;
        public BratVisualConfig visual = new BratVisualConfig();
    }
    
    @JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY)
    public static class BratVisualConfig {
        public int arcTextMargin = 1;
        public int boxSpacing = 1;
        public int curlyHeight = 4;
        public int arcSpacing = 9;
        public int arcStartHeight = 19;
        public BratVisualMargin margin = new BratVisualMargin();
    }
    
    @JsonAutoDetect(fieldVisibility = Visibility.PUBLIC_ONLY)
    public static class BratVisualMargin {
        public int x = 2;
        public int y = 1;
    }
}
