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
    /** Whether to compress annotation files. */
    private boolean compressedCasSerialization = true;

    /**
     * Verify CAS serializability before writing to disk. The serialized form is round-tripped into
     * a dummy CAS first, and only written if the round-trip succeeds -- this guards against
     * UIMA-6162-style breakage where an unreadable CAS would render the document broken in the
     * project. Slower than the regular path; when enabled, the output is uncompressed regardless of
     * {@code compressedCasSerialization}.
     */
    private boolean paranoidCasSerialization = false;

    /**
     * Capture stack traces of the threads owning a CAS, and track per-file write attempts in an
     * in-memory metadata cache. Useful for diagnosing concurrent-access or stuck-CAS-ownership
     * issues; adds noticeable overhead per CAS borrow/release and per write, so leave disabled in
     * production.
     */
    private boolean traceAccess = false;

    /**
     * Leniency for file systems where timestamps are not exact. Use with extreme caution: if an
     * editor accesses an annotation file out-of-sync with the editor, this can lead to unexpected
     * behavior. However, when deploying on certain cloud storage facilities, file system timestamps
     * may not be exact down to the millisecond, so configuring a slight leniency here may be
     * helpful.
     */
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
