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
package de.tudarmstadt.ukp.inception.io.xml;

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromJsonStream;
import static de.tudarmstadt.ukp.inception.support.logging.BaseLoggers.BOOT_LOG;
import static java.nio.file.Files.isDirectory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.protocol.http.WebApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.inception.support.SettingsUtil;

@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "format.custom-xml", name = "enabled", havingValue = "true", matchIfMissing = false)
@Configuration(proxyBeanMethods = false)
public class CustomXmlFormatLoader
    implements BeanDefinitionRegistryPostProcessor
{
    public static final String CUSTOM_XML_FORMAT_PREFIX = "custom-xml-format-";

    public static final String PLUGINS_XML_FORMAT_BASE_NAME = "plugins/xml-format/";

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private List<CustomXmlFormatPluginDescripion> descriptions = Collections.emptyList();

    private ConfigurableListableBeanFactory factory;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory aFactory)
        throws BeansException
    {
        factory = aFactory;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry aRegistry)
        throws BeansException
    {
        var pluginJsonFiles = scanEditorPluginFolders();
        descriptions = new ArrayList<>();

        for (var pluginJsonFile : pluginJsonFiles) {
            try (var is = Files.newInputStream(pluginJsonFile)) {
                var desc = fromJsonStream(CustomXmlFormatPluginDescripion.class, is);
                desc.setId(pluginJsonFile.getParent().getFileName().toString());
                desc.setBasePath(pluginJsonFile.getParent());

                registerEditorPlugin(aRegistry, desc);
                descriptions.add(desc);
            }
            catch (IOException e) {
                LOG.error("Error loading editor plugin description from [{}]", pluginJsonFile, e);
            }
        }
    }

    private void registerEditorPlugin(BeanDefinitionRegistry aRegistry,
            CustomXmlFormatPluginDescripion aDesc)
    {
        BOOT_LOG.info("Loading custom XML format plugin: {}", aDesc.getName());
        var builder = BeanDefinitionBuilder.genericBeanDefinition(CustomXmlFormatFactory.class,
                () -> {
                    var wicketApplication = factory.getBean(WebApplication.class);
                    return new CustomXmlFormatFactory(aDesc, wicketApplication);
                });
        aRegistry.registerBeanDefinition(CUSTOM_XML_FORMAT_PREFIX + aDesc.getId(),
                builder.getBeanDefinition());
    }

    private List<Path> scanEditorPluginFolders()
    {
        File editorPlugins = new File(SettingsUtil.getApplicationHome(), "xml-formats");
        if (!editorPlugins.exists()) {
            return Collections.emptyList();
        }

        var pluginJsonFiles = new ArrayList<Path>();
        try (var fileStream = Files.list(editorPlugins.toPath())) {
            var pi = fileStream.filter(p -> isDirectory(p)).iterator();
            while (pi.hasNext()) {
                var subdir = pi.next();
                var pluginJsonFile = subdir.resolve("plugin.json");
                if (Files.isRegularFile(pluginJsonFile)) {
                    pluginJsonFiles.add(pluginJsonFile);
                }
            }
        }
        catch (IOException e) {
            LOG.error("Error scanning for custom XML format plugins", e);
        }

        return pluginJsonFiles;
    }
}
