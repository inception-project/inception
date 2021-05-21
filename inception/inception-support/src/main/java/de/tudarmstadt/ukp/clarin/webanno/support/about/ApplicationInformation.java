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
package de.tudarmstadt.ukp.clarin.webanno.support.about;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.list;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationInformation
{
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationInformation.class);

    public static String loadDependencies()
    {
        try {
            List<URL> candidates = list(ApplicationInformation.class.getClassLoader()
                    .getResources("META-INF/DEPENDENCIES.txt"));

            Optional<URL> webappDependencies = candidates.stream() //
                    .filter(url -> //
                    url.toString().contains("/WEB-INF/classes!/META-INF/DEPENDENCIES.txt") || //
                            url.toString().contains("target/classes/META-INF/DEPENDENCIES.txt"))
                    .findFirst();

            if (webappDependencies.isEmpty()) {
                return "Unable to locate dependency information";
            }

            try (InputStream is = webappDependencies.get().openStream()) {
                return IOUtils.toString(is, UTF_8);
            }
        }
        catch (Exception e) {
            LOG.error("Unable to retrieve dependency information", e);
            return "Unable to retrieve dependency information";
        }
    }
}
