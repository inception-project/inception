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

import static de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil.getPropApplicationHome;
import static de.tudarmstadt.ukp.inception.externaleditor.policy.DefaultHtmlDocumentPolicy.DEFAULT_POLICY_YAML;
import static de.tudarmstadt.ukp.inception.externaleditor.policy.SafetyNetDocumentPolicyTest.touch;
import static java.lang.System.setProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.write;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultHtmlDocumentPolicyTest
{
    @Test
    void thatOverrideFileIsPickedUp(@TempDir Path aTemp) throws Exception
    {
        Path policyFile = aTemp.resolve(DEFAULT_POLICY_YAML);
        setProperty(getPropApplicationHome(), aTemp.toString());

        var sut = new DefaultHtmlDocumentPolicy();

        assertThat(sut.getPolicy().getElementPolicies()).hasSize(72);

        write(policyFile.toFile(), "policies: []", UTF_8);
        assertThat(policyFile).exists();
        assertThat(sut.getPolicy().getElementPolicies()).isEmpty();

        write(policyFile.toFile(), "policies: [ {elements: [a], action: PASS}]", UTF_8);
        assertThat(policyFile).exists();
        touch(policyFile);
        assertThat(sut.getPolicy().getElementPolicies()).hasSize(1);

        Files.delete(policyFile);
        assertThat(policyFile).doesNotExist();
        assertThat(sut.getPolicy().getElementPolicies()).hasSize(72);
    }
}
