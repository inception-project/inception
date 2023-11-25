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
package de.tudarmstadt.ukp.inception.externaleditor.policy;

import java.io.File;
import java.io.IOException;

import de.tudarmstadt.ukp.inception.support.SettingsUtil;
import de.tudarmstadt.ukp.inception.support.io.WatchedResourceFile;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollection;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollectionIOUtils;

public class DefaultHtmlDocumentPolicy
{
    static final String HTML_POLICY_OVERRIDE_YAML = "default-policy.yaml";

    static final String DEFAULT_HTML_POLICY_YAML = //
            "/de/tudarmstadt/ukp/inception/externaleditor/policy/DefaultHtmlDocumentPolicy.yaml";

    private final WatchedResourceFile<PolicyCollection> defaultPolicy;

    private final WatchedResourceFile<PolicyCollection> policyOverrideFile;

    public DefaultHtmlDocumentPolicy() throws IOException
    {
        defaultPolicy = new WatchedResourceFile<PolicyCollection>(
                getClass().getResource(DEFAULT_HTML_POLICY_YAML),
                PolicyCollectionIOUtils::loadPolicies);

        var appHome = SettingsUtil.getApplicationHome();
        policyOverrideFile = new WatchedResourceFile<>(
                new File(appHome, HTML_POLICY_OVERRIDE_YAML).toPath(),
                PolicyCollectionIOUtils::loadPolicies);
    }

    public PolicyCollection getPolicy() throws IOException
    {
        var policyOverride = policyOverrideFile.get();
        if (policyOverride.isPresent()) {
            return policyOverride.get();
        }

        return defaultPolicy.get().orElseThrow(
                () -> new IOException("Default HTML document policy file not found at ["
                        + DEFAULT_HTML_POLICY_YAML + "]"));
    }
}
