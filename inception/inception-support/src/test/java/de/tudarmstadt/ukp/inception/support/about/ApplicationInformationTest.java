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
package de.tudarmstadt.ukp.inception.support.about;

import static de.tudarmstadt.ukp.inception.support.about.ApplicationInformation.loadMavenDependencies;
import static de.tudarmstadt.ukp.inception.support.about.ApplicationInformation.loadNpmDependencies;

import java.io.File;

import org.junit.jupiter.api.Test;

class ApplicationInformationTest
{
    private static final String MAVEN_DEPS = "src/test/resources/ApplicationInformationTest/DEPENDENCIES.json";
    private static final String NPM_DEPS = "src/test/resources/ApplicationInformationTest/NPM-DEPENDENCIES.json";

    @Test
    void testLoadMavenDependencies() throws Exception
    {
        loadMavenDependencies(new File(MAVEN_DEPS).toURI().toURL());
    }

    @Test
    void testLoadNpmDependencies() throws Exception
    {
        loadNpmDependencies(new File(NPM_DEPS).toURI().toURL());
    }
}
