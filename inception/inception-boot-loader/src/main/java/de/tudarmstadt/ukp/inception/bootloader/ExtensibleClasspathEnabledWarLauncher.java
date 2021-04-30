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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.boot.loader.PropertiesLauncher;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;

public class ExtensibleClasspathEnabledWarLauncher
    extends PropertiesLauncher
{
    private static final String DEBUG = "loader.debug";

    private static final String WEB_INF = "WEB-INF/";

    private static final String WEB_INF_CLASSES = WEB_INF + "classes/";

    private static final String WEB_INF_LIB = WEB_INF + "lib/";

    private static final String WEB_INF_LIB_PROVIDED = WEB_INF + "lib-provided/";

    private static final String EXTRA_LIB = "/lib";

    private final Archive archive;

    public ExtensibleClasspathEnabledWarLauncher()
    {
        try {
            archive = createArchive();
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    protected Iterator<Archive> getClassPathArchivesIterator() throws Exception
    {
        return getClassPathArchives().iterator();
    }

    @Deprecated
    @Override
    protected List<Archive> getClassPathArchives() throws Exception
    {
        List<Archive> archives = new ArrayList<>();
        archive.getNestedArchives(this::isNestedArchive, candidateArchive -> true)
                .forEachRemaining(archives::add);

        Path extraLibPath = Paths.get(getApplicationHome() + EXTRA_LIB);

        if (Files.exists(extraLibPath)) {
            debug("Scanning for additional JARs in [" + extraLibPath + "]");

            for (Path jar : Files.newDirectoryStream(extraLibPath, "*.jar")) {
                debug("Registering JAR: [" + jar + "]");
                archives.add(new JarFileArchive(jar.toFile()));
            }
        }
        else {
            debug("Not scanning for additional JARs in [" + extraLibPath
                    + "] - path does not exist");
        }

        return archives;
    }

    protected boolean isNestedArchive(Archive.Entry entry)
    {
        if (entry.isDirectory()) {
            return entry.getName().equals(WEB_INF_CLASSES);
        }
        else {
            return entry.getName().startsWith(WEB_INF_LIB)
                    || entry.getName().startsWith(WEB_INF_LIB_PROVIDED);
        }
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
        if (ver < 11) {
            StringBuilder message = new StringBuilder();
            message.append(
                    format("INCEpTION requires at least Java 11, but you are running Java %s.%n%n",
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
