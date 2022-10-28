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
package de.tudarmstadt.ukp.inception.support.deployment;

import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentMode.DESKTOP;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentMode.SERVER_JAR;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentMode.SERVER_JAR_DOCKER;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentMode.SERVER_WAR;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentMode.SERVER_WAR_DOCKER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.readFileToString;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DeploymentModeServiceImpl
    implements DeploymentModeService
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("${running.from.commandline}")
    private boolean runningFromCommandline;

    private int port = -1;

    private final Environment environment;

    @Autowired
    public DeploymentModeServiceImpl(Environment aEnvironment)
    {
        environment = aEnvironment;
    }

    @Override
    @EventListener
    public void onApplicationEvent(WebServerInitializedEvent aEvt)
    {
        port = aEvt.getWebServer().getPort();
    }

    @Override
    public DeploymentMode getDeploymentMode()
    {
        boolean dockerized = isDockerized();

        if (isDesktopInstance()) {
            return DESKTOP;
        }

        boolean embeddedServerDeployment = isEmbeddedServerDeployment();
        if (dockerized && embeddedServerDeployment) {
            return SERVER_JAR_DOCKER;
        }

        if (embeddedServerDeployment) {
            return SERVER_JAR;
        }

        if (dockerized) {
            return SERVER_WAR_DOCKER;
        }

        return SERVER_WAR;
    }

    @Override
    public boolean isDesktopInstance()
    {
        return
        // The embedded server was used (i.e. not running as a WAR)
        isEmbeddedServerDeployment() &&
        // There is no console available (happens which double-clicking on the JAR)
                System.console() == null &&
                // There is a graphical environment available
                !GraphicsEnvironment.isHeadless();
    }

    /**
     * @return if the embedded server was used (i.e. not running as a WAR) and running in Docker.
     */
    @Override
    public boolean isDockerized()
    {
        final String cgroupPath = "/proc/1/cgroup";

        try {
            File cgroup = new File(cgroupPath);
            if (cgroup.exists() && cgroup.canRead()) {
                String content = readFileToString(cgroup, UTF_8);
                if (content.contains("docker")) {
                    return true;
                }
            }
        }
        catch (Exception e) {
            LOG.debug("Unable to check [{}]", cgroupPath, e);
        }

        return false;
    }

    /**
     * @return if the embedded server was used (i.e. not running as a WAR).
     */
    @Override
    public boolean isEmbeddedServerDeployment()
    {
        var byProfiles = isEmbeddedServerDeployment(environment.getActiveProfiles());
        var byClassic = port != -1 && runningFromCommandline;
        assert byProfiles == byClassic;
        return byProfiles;
    }

    /**
     * @return if the embedded server was used (i.e. not running as a WAR).
     */
    public static boolean isEmbeddedServerDeployment(String... aActiveProfiles)
    {
        return Set.of(aActiveProfiles)
                .containsAll(asList(PROFILE_APPLICATION_MODE, PROFILE_INTERNAL_SERVER));
    }
}
