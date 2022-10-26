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
package de.tudarmstadt.ukp.clarin.webanno.support;

import static java.nio.file.Files.getLastModifiedTime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import org.apache.commons.lang3.function.FailableFunction;

public class WatchedResourceFile<T>
{
    private final FailableFunction<InputStream, T, IOException> loader;
    private final Path resourcePath;
    private T resource;
    private Instant resourceMTime;

    public WatchedResourceFile(Path aResourcePath,
            FailableFunction<InputStream, T, IOException> aLoader)
    {
        loader = aLoader;
        resourcePath = aResourcePath;
    }

    public Optional<T> get() throws IOException
    {
        if (Files.exists(resourcePath)) {
            if (resourceMTime == null
                    || getLastModifiedTime(resourcePath).toInstant().isAfter(resourceMTime)) {
                try (var is = Files.newInputStream(resourcePath)) {
                    resource = loader.apply(is);
                }
            }

            return Optional.of(resource);
        }

        resourceMTime = null;
        return Optional.empty();
    }
}
