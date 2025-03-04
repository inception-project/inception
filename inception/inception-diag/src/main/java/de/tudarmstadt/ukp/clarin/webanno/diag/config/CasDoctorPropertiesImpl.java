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
package de.tudarmstadt.ukp.clarin.webanno.diag.config;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "debug.cas-doctor")
public class CasDoctorPropertiesImpl
    implements CasDoctorProperties
{
    private List<String> checks = Collections.emptyList();
    private List<String> repairs = Collections.emptyList();
    private boolean fatal = true;
    // private boolean forceReleaseBehavior = false;

    @Override
    public List<String> getChecks()
    {
        return checks;
    }

    public void setChecks(List<String> aChecks)
    {
        checks = aChecks;
    }

    @Override
    public List<String> getRepairs()
    {
        return repairs;
    }

    public void setRepairs(List<String> aRepairs)
    {
        repairs = aRepairs;
    }

    @Override
    public boolean isFatal()
    {
        return fatal;
    }

    public void setFatal(boolean aFatal)
    {
        fatal = aFatal;
    }

    // @Override
    // public boolean isForceReleaseBehavior()
    // {
    // return forceReleaseBehavior;
    // }
    //
    // public void setForceReleaseBehavior(boolean aForceReleaseBehavior)
    // {
    // forceReleaseBehavior = aForceReleaseBehavior;
    // }
}
