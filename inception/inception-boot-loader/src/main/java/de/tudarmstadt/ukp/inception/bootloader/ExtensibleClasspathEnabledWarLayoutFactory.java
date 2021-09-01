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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.boot.loader.tools.CustomLoaderLayout;
import org.springframework.boot.loader.tools.Layout;
import org.springframework.boot.loader.tools.LayoutFactory;
import org.springframework.boot.loader.tools.Layouts.War;
import org.springframework.boot.loader.tools.LibraryScope;
import org.springframework.boot.loader.tools.LoaderClassesWriter;

public class ExtensibleClasspathEnabledWarLayoutFactory
    implements LayoutFactory
{
    @Override
    public Layout getLayout(File aSource)
    {
        if (!aSource.getName().endsWith(".war")) {
            throw new IllegalArgumentException("Only WAR archives are supported for this layout");
        }

        return new ExtensibleClasspathEnabledWar();
    }

    public static class ExtensibleClasspathEnabledWar
        extends War
        implements CustomLoaderLayout
    {
        @Override
        public String getLauncherClassName()
        {
            return "de.tudarmstadt.ukp.inception.bootloader.ExtensibleClasspathEnabledWarLauncher";
        }

        @Override
        public String getLibraryLocation(String aLibraryName, LibraryScope aScope)
        {
            if (aLibraryName.startsWith("inception-boot-loader-")) {
                // Boot loader classes go to the root of the JAR
                return "";
            }

            return super.getLibraryLocation(aLibraryName, aScope);
        }

        @Override
        public void writeLoadedClasses(LoaderClassesWriter aWriter) throws IOException
        {
            // Package the default Spring Boot loader classes
            aWriter.writeLoaderClasses();

            // Package our own custom launcher classes
            registerClassResource(aWriter,
                    "de/tudarmstadt/ukp/inception/bootloader/ExtensibleClasspathEnabledWarLauncher.class");
            registerClassResource(aWriter,
                    "de/tudarmstadt/ukp/inception/bootloader/UIMessage.class");
        }

        private void registerClassResource(LoaderClassesWriter aWriter, String aClassResourceName)
            throws IOException
        {
            try (InputStream is = getClass().getResourceAsStream("/" + aClassResourceName)) {
                aWriter.writeEntry(aClassResourceName, is);
            }
        }
    }
}
