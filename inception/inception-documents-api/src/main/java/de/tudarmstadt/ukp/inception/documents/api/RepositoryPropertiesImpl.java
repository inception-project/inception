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
package de.tudarmstadt.ukp.inception.documents.api;

import java.io.File;

import org.slf4j.MDC;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jmx.export.annotation.ManagedResource;

import de.tudarmstadt.ukp.inception.support.SettingsUtil;
import de.tudarmstadt.ukp.inception.support.logging.Logging;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link RepositoryAutoConfiguration}.
 * </p>
 */
@ConfigurationProperties("repository")
@ManagedResource
public class RepositoryPropertiesImpl
    implements RepositoryProperties
{
    private File path;

    @Override
    public File getPath()
    {
        if (path != null) {
            return path;
        }

        return new File(System.getProperty(SettingsUtil.getPropApplicationHome(),
                System.getProperty("user.home") + "/"
                        + SettingsUtil.getApplicationUserHomeSubdir()),
                "repository");
    }

    public void setPath(File aPath)
    {
        path = aPath;

        // This is mainly a convenience for unit tests. For production environments, it must be made
        // sure that the MDC is configured e.g. on the worker threads for incoming requests and
        // such.
        MDC.put(Logging.KEY_REPOSITORY_PATH, aPath.getPath().toString());
    }
}
