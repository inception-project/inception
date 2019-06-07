/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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

public class PluginEnabledWarLayoutFactory
    implements LayoutFactory
{
    @Override
    public Layout getLayout(File aSource)
    {
        if (!aSource.getName().endsWith(".war")) {
            throw new IllegalArgumentException("Only WAR archives are supported for this layout");
        }

        return new PluginEnabledWar();
    }

    public static class PluginEnabledWar
        extends War
        implements CustomLoaderLayout
    {
        @Override
        public String getLauncherClassName()
        {
            return "de.tudarmstadt.ukp.inception.bootloader.PluginEnabledWarLauncher";
        }
        
        @Override
        public String getLibraryDestination(String aLibraryName, LibraryScope aScope)
        {
            if (aLibraryName.startsWith("inception-boot-loader-")) {
                // Boot loader classes go to the root of the JAR
                return "";
            }
            
            return super.getLibraryDestination(aLibraryName, aScope);
        }

        @Override
        public void writeLoadedClasses(LoaderClassesWriter aWriter) throws IOException
        {
            // Package the default Spring Boot loader classes
            aWriter.writeLoaderClasses();
            
            // Package our own custom launcher class
            String classResourceName = "de/tudarmstadt/ukp/inception/bootloader/PluginEnabledWarLauncher.class";
            try (InputStream is = getClass().getResourceAsStream("/" + classResourceName)) {
                aWriter.writeEntry(classResourceName, is);
            }
        }
    }
}
