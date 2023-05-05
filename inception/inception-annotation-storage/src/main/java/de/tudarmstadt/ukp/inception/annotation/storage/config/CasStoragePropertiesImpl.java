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
package de.tudarmstadt.ukp.inception.annotation.storage.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * <p>
 * This class is exposed as a Spring Component via {@link CasStorageServiceAutoConfiguration}.
 * </p>
 */
@ConfigurationProperties("cas-storage")
@ManagedResource
public class CasStoragePropertiesImpl
    implements CasStorageProperties
{
    private boolean compressedCasSerialization = true;
    private boolean paranoidCasSerialization = false;
    private boolean traceAccess = false;
    private Duration fileSystemTimestampAccuracy = Duration.ofMillis(0);

    @ManagedAttribute
    public void setTraceAccess(boolean aTraceAccess)
    {
        traceAccess = aTraceAccess;
    }

    @Override
    public boolean isTraceAccess()
    {
        return traceAccess;
    }

    public void setParanoidCasSerialization(boolean aParanoidCasSerialization)
    {
        paranoidCasSerialization = aParanoidCasSerialization;
    }

    @Override
    @ManagedAttribute
    public boolean isParanoidCasSerialization()
    {
        return paranoidCasSerialization;
    }

    @ManagedAttribute
    public void setCompressedCasSerialization(boolean aCompressedCasSerialization)
    {
        compressedCasSerialization = aCompressedCasSerialization;
    }

    @Override
    @ManagedAttribute
    public boolean isCompressedCasSerialization()
    {
        return compressedCasSerialization;
    }

    @ManagedAttribute
    public void setFileSystemTimestampAccuracy(Duration aFileSystemTimestampAccuracy)
    {
        fileSystemTimestampAccuracy = aFileSystemTimestampAccuracy;
    }

    @Override
    @ManagedAttribute
    public Duration getFileSystemTimestampAccuracy()
    {
        return fileSystemTimestampAccuracy;
    }
}
