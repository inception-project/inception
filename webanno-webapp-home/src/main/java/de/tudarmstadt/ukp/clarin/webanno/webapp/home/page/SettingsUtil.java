/*******************************************************************************
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.webapp.home.page;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.support.PropertiesLoaderUtils;

public class SettingsUtil
{
    public static final String PROP_BUILD_NUMBER = "buildNumber";
    public static final String PROP_TIMESTAMP = "timestamp";
    public static final String PROP_VERSION = "version";
    
    private static final String PROP_USER_HOME = "user.home";
    private static final String PROP_WEBANNO_HOME = "webanno.home";
    
    private static final String SETTINGS_FILE = "settings.properties";
    private static final String WEBANNO_USER_HOME_SUBDIR = ".webanno";
    
    private static Properties versionInfo;
    private static Properties settings;
    
    public static Properties getVersionProperties()
    {
        if (versionInfo == null) {
            try {
                versionInfo = PropertiesLoaderUtils.loadAllProperties("/META-INF/version.properties");
            }
            catch (IOException e) {
                versionInfo = new Properties();
                versionInfo.setProperty(PROP_VERSION, "unknown");
                versionInfo.setProperty(PROP_TIMESTAMP, "unknown");
                versionInfo.setProperty(PROP_BUILD_NUMBER, "unknown");
            }
        }
        
        return versionInfo;
    }
    
    public static String getVersionString()
    {
        Properties props = getVersionProperties();
        if ("unknown".equals(props.getProperty(PROP_VERSION))) {
            return "Version information not available";
        }
        else {
            return props.getProperty(SettingsUtil.PROP_VERSION) + " ("
                    + props.getProperty(SettingsUtil.PROP_TIMESTAMP) + ", build "
                    + props.getProperty(SettingsUtil.PROP_BUILD_NUMBER) + ")";
        }
    }
    
    /**
     * Locate the settings file and return its location.
     * 
     * @return the location of the settings file or {@code null} if none could be found.
     */
    public static File getSettingsFile()
    {
        String appHome = System.getProperty(PROP_WEBANNO_HOME);
        String userHome = System.getProperty(PROP_USER_HOME);

        // Locate settings, first in webanno.home, then in user home
        File settings = null;
        if (appHome != null) {
            settings = new File(appHome, SETTINGS_FILE);
        }
        else if (userHome != null) {
            settings = new File(userHome + "/" + WEBANNO_USER_HOME_SUBDIR, SETTINGS_FILE);
        }

        if (settings.exists()) {
            return settings;
        }
        else {
            return null;
        }
    }
    
    public static Properties getSettings()
    {
        if (settings == null) {
            settings = new Properties();
            File settingsFile = getSettingsFile();
            if (settingsFile != null) {
                try (InputStream in = new FileInputStream(settingsFile)) {
                    settings.load(in);
                }
                catch (IOException e) {
                    LogFactory.getLog(SettingsUtil.class).error(
                            "Unable to load settings file [" + settings + "]", e);
                }
            }
        }
        return settings;
    }
}
