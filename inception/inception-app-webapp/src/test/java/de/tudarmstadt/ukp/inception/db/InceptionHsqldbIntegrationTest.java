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
package de.tudarmstadt.ukp.inception.db;

import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.FileSystemUtils;

class InceptionHsqldbIntegrationTest
{
    private static final File STATIC_TEST_FOLDER = new File("target/hsqldb-test");

    static @TempDir Path tempDir;

    static ConfigurableApplicationContext appContext;

    @BeforeAll
    static void setupClass()
    {
        FileSystemUtils.deleteRecursively(STATIC_TEST_FOLDER);
    }

    @AfterAll
    static void tearDownClass()
    {
        if (appContext != null) {
            appContext.close();
        }
    }

    @Nested
    class SpringApplcationContext
        extends InceptionIntegrationTest_ImplBase
    {
        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry)
        {
            if (IS_OS_WINDOWS) {
                // HSQLDB files not closed on shutdown before TempDir is deleted, so we have to use
                // a non-temporary folder on Windows because open files cannot be deleted.
                STATIC_TEST_FOLDER.mkdirs();
                registry.add("inception.home", () -> STATIC_TEST_FOLDER);
            }
            else {
                registry.add("inception.home", () -> tempDir.toString());
            }
        }

        @BeforeEach
        void setup(ConfigurableApplicationContext aAppContext)
        {
            appContext = aAppContext;
        }
    }
}
