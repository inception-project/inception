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
package de.tudarmstadt.ukp.inception.support;

import static org.apache.commons.lang3.StringUtils.substring;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import de.tudarmstadt.ukp.inception.support.help.ImageLinkDecl;

public class SettingsUtil
{
    private static String propApplicationHome = "inception.home";
    private static String applicationUserHomeSubdir = ".inception";

    public static final String PROP_BUILD_NUMBER = "buildNumber";
    public static final String PROP_TIMESTAMP = "timestamp";
    public static final String PROP_VERSION = "version";

    private static final String PROP_USER_HOME = "user.home";

    private static final String SETTINGS_FILE = "settings.properties";
    private static final String SETTINGS_YAML_FILE = "settings.yml";

    /**
     * @deprecated Should introduce/use a Spring properties bean instead.
     */
    @Deprecated
    public static final String CFG_LOCALE = "locale";

    /**
     * @deprecated Should introduce/use a Spring properties bean instead.
     */
    @Deprecated
    public static final String CFG_AUTH_MODE = "auth.mode";
    @Deprecated
    public static final String CFG_AUTH_PREAUTH_NEWUSER_ROLES = "auth.preauth.newuser.roles";

    /**
     * @deprecated Should introduce/use a Spring properties bean instead.
     */
    @Deprecated
    public static final String CFG_WARNINGS_EMBEDDED_DATABASE = "warnings.embeddedDatabase";
    @Deprecated
    public static final String CFG_WARNINGS_UNSUPPORTED_BROWSER = "warnings.unsupportedBrowser";

    /**
     * @deprecated Should introduce/use a Spring properties bean instead.
     */
    @Deprecated
    public static final String CFG_LINK_PREFIX = "style.header.icon.";
    public static final @Deprecated String CFG_LINK_URL = ".linkUrl";
    public static final @Deprecated String CFG_LINK_IMAGE_URL = ".imageUrl";

    private static Properties versionInfo;
    private static Properties settings;

    public static void customizeApplication(String aPropertyName, String aSubdirName)
    {
        propApplicationHome = aPropertyName;
        applicationUserHomeSubdir = aSubdirName;
    }

    public static String getApplicationUserHomeSubdir()
    {
        return applicationUserHomeSubdir;
    }

    public static String getPropApplicationHome()
    {
        return propApplicationHome;
    }

    public static void setGlobalLogFolder(Path aPath)
    {
        System.setProperty("GLOBAL_LOG_FOLDER", aPath.toString());
    }

    public static Optional<Path> getGlobalLogFolder()
    {
        String prop = System.getProperty("GLOBAL_LOG_FOLDER");
        if (prop == null) {
            return Optional.empty();
        }
        return Optional.of(Paths.get(prop));
    }

    public static Optional<Path> getGlobalLogFile()
    {
        return getGlobalLogFolder().map(dir -> dir.resolve("application.log"));
    }

    public static synchronized Properties getVersionProperties()
    {
        if (versionInfo == null) {
            try {
                versionInfo = PropertiesLoaderUtils
                        .loadProperties(new ClassPathResource("META-INF/version.properties"));
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
            return props.getProperty(PROP_VERSION) + " (" + props.getProperty(PROP_TIMESTAMP)
                    + ", build " + substring(props.getProperty(PROP_BUILD_NUMBER), 0, 8) + ")";
        }
    }

    public static File getApplicationHome()
    {
        String appHome = System.getProperty(propApplicationHome);

        if (appHome != null) {
            return new File(appHome);
        }

        String userHome = System.getProperty(PROP_USER_HOME);
        return new File(userHome + "/" + applicationUserHomeSubdir);
    }

    /**
     * Locate the settings file and return its location.
     * 
     * @return the location of the settings file or {@code null} if none could be found.
     */
    public static File getSettingsFileLocation()
    {
        // Locate settings, first in application, then in user home
        String appHome = System.getProperty(propApplicationHome);
        if (appHome != null) {
            File yamlFile = new File(appHome, SETTINGS_YAML_FILE);
            if (yamlFile.exists()) {
                return yamlFile;
            }

            return new File(appHome, SETTINGS_FILE);
        }

        String userHome = System.getProperty(PROP_USER_HOME);
        if (userHome != null) {
            File yamlFile = new File(userHome + "/" + applicationUserHomeSubdir,
                    SETTINGS_YAML_FILE);
            if (yamlFile.exists()) {
                return yamlFile;
            }

            return new File(userHome + "/" + applicationUserHomeSubdir, SETTINGS_FILE);
        }

        return null;
    }

    /**
     * Locate the settings file and return its location if it exists.
     * 
     * @return the location of the settings file or {@code null} if none could be found.
     */
    public static File getSettingsFile()
    {
        File settingsFile = getSettingsFileLocation();

        if (settingsFile != null && settingsFile.exists()) {
            return settingsFile;
        }

        return null;
    }

    /**
     * @deprecated To access setting properties, use Spring Boot
     *             {@link org.springframework.boot.context.properties.ConfigurationProperties}
     *             classes implementing a corresponding interface instead (e.g. @see
     *             de.tudarmstadt.ukp.clarin.webanno.ui.core.users.RemoteApiProperties).
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    public static synchronized Properties getSettings()
    {
        if (settings == null) {
            var props = new Properties(System.getProperties());
            File settingsFile = getSettingsFile();
            if (settingsFile != null) {
                try (InputStream in = new FileInputStream(settingsFile)) {
                    props.load(in);
                }
                catch (IOException e) {
                    LoggerFactory.getLogger(SettingsUtil.class)
                            .error("Unable to load settings file [" + settings + "]", e);
                }
            }

            settings = props;
        }
        return settings;
    }

    public static List<ImageLinkDecl> getLinks()
    {
        Properties props = getSettings();
        Map<String, ImageLinkDecl> linkMap = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(CFG_LINK_PREFIX)) {
                String id = StringUtils.substringBetween(key, CFG_LINK_PREFIX, ".");

                // Create new declaration for current ID if there is none so far
                ImageLinkDecl e = linkMap.get(id);
                if (e == null) {
                    e = new ImageLinkDecl(id);
                    linkMap.put(id, e);
                }

                // Record link URL
                if (key.endsWith(CFG_LINK_URL)) {
                    e.setLinkUrl(props.getProperty(key));
                }
                // Record link URL
                if (key.endsWith(CFG_LINK_IMAGE_URL)) {
                    e.setImageUrl(props.getProperty(key));
                }
            }
        }

        // Sort by ID
        List<ImageLinkDecl> links = new ArrayList<>(linkMap.values());
        links.sort(Comparator.comparing(ImageLinkDecl::getId));

        return links;
    }
}
