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
package de.tudarmstadt.ukp.inception.bootloader;

import static java.lang.String.format;
import static java.lang.System.getProperty;

import java.awt.GraphicsEnvironment;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.loader.launch.Archive;
import org.springframework.boot.loader.launch.WarLauncher;

public class ExtensibleClasspathEnabledWarLauncher
    extends WarLauncher
{
    private static final String DEBUG = "loader.debug";

    private static final String EXTRA_LIB = "/lib";

    public ExtensibleClasspathEnabledWarLauncher() throws Exception
    {
        System.setProperty("apple.awt.application.name", "INCEpTION");
    }

    protected ExtensibleClasspathEnabledWarLauncher(Archive archive) throws Exception
    {
        super(archive);
    }

    @Override
    protected Set<URL> getClassPathUrls() throws Exception
    {
        Set<URL> urls = new LinkedHashSet<>(super.getClassPathUrls());

        Path extraLibPath = Paths.get(getApplicationHome() + EXTRA_LIB);

        if (Files.exists(extraLibPath)) {
            debug("Scanning for additional JARs in [" + extraLibPath + "]");

            for (Path jar : Files.newDirectoryStream(extraLibPath, "*.jar")) {
                debug("Registering JAR: [" + jar + "]");
                urls.add(jar.toUri().toURL());
            }
        }
        else {
            debug("Not scanning for additional JARs in [" + extraLibPath
                    + "] - path does not exist");
        }

        return urls;
    }

    private void debug(String message)
    {
        if (Boolean.getBoolean(DEBUG)) {
            System.out.println(message);
        }
    }

    /**
     * Locate the settings file and return its location.
     * 
     * @return the location of the settings file or {@code null} if none could be found.
     */
    public static String getApplicationHome()
    {
        String appHome = System.getProperty("inception.home");
        String userHome = System.getProperty("user.home");

        if (appHome != null) {
            return appHome;
        }
        else {
            return userHome + "/" + ".inception";
        }
    }

    public static int getJavaVersion()
    {
        String verString = getProperty("java.specification.version");
        int ver = 99;

        try {
            if (verString.contains(".")) {
                final String[] toParse = verString.split("\\.");
                if (toParse.length >= 1) {
                    ver = Integer.valueOf(toParse[0]);
                }
            }
            else {
                ver = Integer.valueOf(verString);
            }
        }
        catch (Exception e) {
            System.err.printf(
                    "Unable to determine your Java version from the version string [%s]. Let's "
                            + "assume it is compatible with Java 11 and start.%n",
                    verString);
        }

        return ver;
    }

    public static boolean checkSystemRequirements(int ver)
    {
        if (ver < 17) {
            StringBuilder message = new StringBuilder();
            message.append(
                    format("INCEpTION requires at least Java 17, but you are running Java %s.%n%n",
                            getProperty("java.specification.version")));
            message.append(format("Installation: %s%n", getProperty("java.home")));
            message.append(format("Vendor: %s%n", getProperty("java.vendor")));
            message.append(format("Version: %s%n", getProperty("java.version")));
            if (!GraphicsEnvironment.isHeadless()) {
                UIMessage.displayMessage(message.toString());
            }
            else {
                System.err.print(message);
            }

            return false;
        }

        return true;
    }

    public static void main(String[] args) throws Exception
    {
        boolean okToLaunch = checkSystemRequirements(getJavaVersion());

        if (okToLaunch) {
            new ExtensibleClasspathEnabledWarLauncher().launch(args);
        }
    }
}
