/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.export;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.RandomUtils;

public class ProjectExportTaskHandle
    implements Serializable
{
    private static final long serialVersionUID = -105340488296444406L;

    private static final AtomicLong NEXT_ID = new AtomicLong(1);
    private static final long INSTANCE_ID = RandomUtils.nextLong();

    // This is a random number initialized at boot time which is used whether a handle is from a
    // previous instance run and no longer valid.
    private final long instanceId;

    // This is the id of the task within the instance.
    private final long runId;
    

    public ProjectExportTaskHandle()
    {
        runId = NEXT_ID.getAndIncrement();
        instanceId = INSTANCE_ID;
    }

    
    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof ProjectExportTaskHandle)) {
            return false;
        }
        ProjectExportTaskHandle castOther = (ProjectExportTaskHandle) other;
        return Objects.equals(instanceId, castOther.instanceId)
                && Objects.equals(runId, castOther.runId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(instanceId, runId);
    }
}
