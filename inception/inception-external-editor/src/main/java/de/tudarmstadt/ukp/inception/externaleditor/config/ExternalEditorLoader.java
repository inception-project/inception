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

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromJsonStream;
import static de.tudarmstadt.ukp.inception.support.logging.BaseLoggers.BOOT_LOG;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.PathResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import de.tudarmstadt.ukp.inception.externaleditor.ExternalAnnotationEditorFactory;
import de.tudarmstadt.ukp.inception.support.SettingsUtil;

@ConditionalOnWebApplication
@Configuration(proxyBeanMethods = false)
public class ExternalEditorLoader
    implements BeanDefinitionRegistryPostProcessor, WebMvcConfigurer
{
    public static final String PLUGINS_EDITOR_BASE_URL = "/plugins/editor/";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private List<ExternalEditorPluginDescripion> descriptions = Collections.emptyList();

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
        descriptions = new ArrayList<>();

        for (Path pluginJsonFile : pluginJsonFiles) {
            try (InputStream is = Files.newInputStream(pluginJsonFile)) {
                var desc = fromJsonStream(ExternalEditorPluginDescripion.class, is);
                desc.setId(pluginJsonFile.getParent().getFileName().toString());
                desc.setBasePath(pluginJsonFile.getParent());

                registerEditorPlugin(aRegistry, desc);
                descriptions.add(desc);
            }
            catch (IOException e) {
                log.error("Error loading editor plugin description from [{}]", pluginJsonFile, e);
            }
        }
    }

    private void registerEditorPlugin(BeanDefinitionRegistry aRegistry,
            ExternalEditorPluginDescripion aDesc)
    {
        BOOT_LOG.info("Loading editor plugin: {}", aDesc.getName());
        var builder = BeanDefinitionBuilder.genericBeanDefinition(
                ExternalAnnotationEditorFactory.class,
                () -> new ExternalAnnotationEditorFactory(aDesc));
        aRegistry.registerBeanDefinition("external-editor-" + aDesc.getId(),
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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry aRegistry)
    {
        for (ExternalEditorPluginDescripion desc : descriptions) {
            aRegistry.addResourceHandler(PLUGINS_EDITOR_BASE_URL + desc.getId() + "/**")
                    .addResourceLocations(new PathResource(desc.getBasePath()));
        }
    }
}
