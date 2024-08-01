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

package de.tudarmstadt.ukp.inception.security.oauth;

import de.tudarmstadt.ukp.clarin.webanno.security.config.InceptionSecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeService;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@ActiveProfiles(DeploymentModeService.PROFILE_AUTH_MODE_DATABASE)
@DataJpaTest( //
    showSql = false, //
    properties = { //
        "spring.liquibase.enabled=false", //
        "spring.main.banner-mode=off" })
@ImportAutoConfiguration({ //
    SecurityAutoConfiguration.class, //
    InceptionSecurityAutoConfiguration.class })
@EntityScan(basePackages = { //
    "de.tudarmstadt.ukp.clarin.webanno.security.model" })
public class OAuthUtilsTest {



}
