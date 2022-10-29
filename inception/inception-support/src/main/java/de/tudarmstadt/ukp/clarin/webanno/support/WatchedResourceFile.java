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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;

import org.apache.commons.lang3.function.FailableFunction;

public class WatchedResourceFile<T>
{
    private final FailableFunction<InputStream, T, IOException> loader;
    private final URL resourceLocation;
    private T resource;
    private Instant resourceMTime;

    public WatchedResourceFile(URL aResourcePath,
            FailableFunction<InputStream, T, IOException> aLoader)
    {
        loader = aLoader;
        resourceLocation = aResourcePath;
    }

    public WatchedResourceFile(Path aResourcePath,
            FailableFunction<InputStream, T, IOException> aLoader)
        throws IOException
    {
        loader = aLoader;
        try {
            resourceLocation = aResourcePath.toUri().toURL();
        }
        catch (MalformedURLException e) {
            throw new IOException(e);
        }
    }

    public Optional<T> get() throws IOException
    {
        if (resourceLocation == null) {
            return Optional.empty();
        }

        if ("file".equals(resourceLocation.getProtocol())) {
            Path resourcePath;
            try {
                resourcePath = Paths.get(resourceLocation.toURI());
            }
            catch (URISyntaxException e) {
                throw new IOException(e);
            }

            if (Files.exists(resourcePath)) {
                Instant mtime = getLastModifiedTime(resourcePath).toInstant();
                if (resourceMTime == null || mtime.isAfter(resourceMTime)) {
                    try (var is = Files.newInputStream(resourcePath)) {
                        resource = loader.apply(is);
                    }
                }

                resourceMTime = mtime;

                return Optional.of(resource);
            }

            resourceMTime = null;
            return Optional.empty();
        }

        if (resourceMTime == null) {
            resourceMTime = Instant.now();
            try (var is = resourceLocation.openStream()) {
                resource = loader.apply(is);
            }
        }

        return Optional.ofNullable(resource);
    }
}
