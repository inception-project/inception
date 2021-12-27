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

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonStream;
import static java.nio.file.Files.isDirectory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.inception.externaleditor.ExternalAnnotationEditorFactory;

@Configuration
public class ExternalEditorLoader
    implements BeanDefinitionRegistryPostProcessor
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory aFactory)
        throws BeansException
    {
        // Nothing to do
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry aRegistry)
        throws BeansException
    {
        List<Path> pluginJsonFiles = scanEditorPluginFolders();

        for (Path pluginJsonFile : pluginJsonFiles) {
            try (InputStream is = Files.newInputStream(pluginJsonFile)) {
                EditorPluginDescripion desc = fromJsonStream(EditorPluginDescripion.class, is);
                desc.setBasePath(pluginJsonFile.getParent());
                registerEditorPlugin(aRegistry, desc);
            }
            catch (IOException e) {
                log.error("Error loading editor plugin description from [{}]", pluginJsonFile, e);
            }
        }
    }

    private void registerEditorPlugin(BeanDefinitionRegistry aRegistry,
            EditorPluginDescripion aDesc)
    {
        log.info("Loading editor plugin: {}", aDesc.getName());
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
                ExternalAnnotationEditorFactory.class,
                () -> new ExternalAnnotationEditorFactory(aDesc));
        aRegistry.registerBeanDefinition("external-editor-" + aDesc.getImplementation(),
                builder.getBeanDefinition());
    }

    private List<Path> scanEditorPluginFolders()
    {
        File editorPlugins = new File(SettingsUtil.getApplicationHome(), "editors");
        if (!editorPlugins.exists()) {
            return Collections.emptyList();
        }

        List<Path> pluginJsonFiles = new ArrayList<>();
        try (Stream<Path> fileStream = Files.list(editorPlugins.toPath())) {
            Iterator<Path> pi = fileStream.filter(p -> isDirectory(p)).iterator();
            while (pi.hasNext()) {
                Path subdir = pi.next();
                Path pluginJsonFile = subdir.resolve("plugin.json");
                if (Files.isRegularFile(pluginJsonFile)) {
                    pluginJsonFiles.add(pluginJsonFile);
                }
            }
        }
        catch (IOException e) {
            log.error("Error scanning for editor plugins", e);
        }

        return pluginJsonFiles;
    }
}
