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
package de.tudarmstadt.ukp.clarin.webanno.telemetry;

import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentMode.DESKTOP;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentMode.SERVER_JAR;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentMode.SERVER_JAR_DOCKER;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentMode.SERVER_WAR;
import static de.tudarmstadt.ukp.inception.support.deployment.DeploymentMode.SERVER_WAR_DOCKER;
import static de.tudarmstadt.ukp.inception.support.logging.BaseLoggers.BOOT_LOG;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;
import static org.apache.commons.io.FileUtils.readFileToString;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import de.tudarmstadt.ukp.clarin.webanno.telemetry.config.TelemetryServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.config.TelemetryServiceProperties;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.event.TelemetrySettingsSavedEvent;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.model.TelemetrySettings;
import de.tudarmstadt.ukp.inception.support.deployment.DeploymentMode;
import de.tudarmstadt.ukp.inception.support.deployment.DeploymentModeServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link TelemetryServiceAutoConfiguration#telemetryService}.
 * </p>
 */
public class TelemetryServiceImpl
    implements TelemetryService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private @PersistenceContext EntityManager entityManager;

    private final TelemetryServiceProperties properties;
    private final List<TelemetrySupport<?>> telemetrySupportsProxy;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    private List<TelemetrySupport<?>> telemetrySupports;

    @Value("${running.from.commandline}")
    private boolean runningFromCommandline;

    private int port = -1;

    public TelemetryServiceImpl(
            @Lazy @Autowired(required = false) List<TelemetrySupport<?>> aTelemetrySupports,
            ApplicationEventPublisher aEventPublisher, TelemetryServiceProperties aProperties,
            PlatformTransactionManager aTransactionManager)
    {
        telemetrySupportsProxy = aTelemetrySupports;
        eventPublisher = aEventPublisher;
        properties = aProperties;
        transactionTemplate = new TransactionTemplate(aTransactionManager);
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    /**
     * @deprecated Moved to {@link DeploymentModeServiceImpl}
     */
    @SuppressWarnings("javadoc")
    @Deprecated
    @EventListener
    public void onApplicationEvent(WebServerInitializedEvent aEvt)
    {
        port = aEvt.getWebServer().getPort();
    }

    public void init()
    {
        List<TelemetrySupport<?>> tsp = new ArrayList<>();

        if (telemetrySupportsProxy != null) {
            tsp.addAll(telemetrySupportsProxy);
            AnnotationAwareOrderComparator.sort(tsp);

            for (TelemetrySupport<?> ts : tsp) {
                log.debug("Found telemetry support: {}", ts.getId());
            }
        }

        BOOT_LOG.info("Found [{}] telemetry supports", tsp.size());

        telemetrySupports = unmodifiableList(tsp);

        autoAcceptOrReject();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void autoAcceptOrReject()
    {
        if (properties.getAutoRespond() == null) {
            return;
        }

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            List<TelemetrySettings> settingsToSave = new ArrayList<>();

            for (TelemetrySupport support : getTelemetrySupports()) {
                if (!support.hasValidSettings()) {
                    TelemetrySettings settings = readOrCreateSettings(support);
                    Object traits = support.readTraits(settings);
                    switch (properties.getAutoRespond()) {
                    case ACCEPT:
                        support.acceptAll(traits);
                        break;
                    case REJECT:
                        support.rejectAll(traits);
                        break;
                    }
                    support.writeTraits(settings, traits);
                    settingsToSave.add(settings);
                }
            }

            writeAllSettings(settingsToSave);
        });
    }

    /**
     * @return if the embedded server was used (i.e. not running as a WAR).
     * @deprecated Use {@link DeploymentModeServiceImpl#isEmbeddedServerDeployment()}
     */
    @Deprecated
    public boolean isEmbeddedServerDeployment()
    {
        return port != -1 && runningFromCommandline;
    }

    /**
     * @deprecated Use {@link DeploymentModeServiceImpl#isDesktopInstance()}
     */
    @Deprecated
    @SuppressWarnings("javadoc")
    public boolean isDesktopInstance()
    {
        return // The embedded server was used (i.e. not running as a WAR)
        isEmbeddedServerDeployment() &&
        // There is no console available (happens which double-clicking on the JAR)
                System.console() == null &&
                // There is a graphical environment available
                !GraphicsEnvironment.isHeadless();
    }

    /**
     * @return if the embedded server was used (i.e. not running as a WAR) and running in Docker.
     * @deprecated Use {@link DeploymentModeServiceImpl#isDockerized()}
     */
    @Deprecated
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
            log.debug("Unable to check [{}]", cgroupPath, e);
        }

        return false;
    }

    /**
     * @deprecated Use {@link DeploymentModeServiceImpl#getDeploymentMode()}
     */
    @Deprecated
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
    public List<TelemetrySupport<?>> getTelemetrySupports()
    {
        return telemetrySupports;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<TelemetrySupport<T>> getTelemetrySuppport(String aSupport)
    {
        return telemetrySupports.stream() //
                .filter(ts -> ts.getId().equals(aSupport)) //
                .map(ts -> (TelemetrySupport<T>) ts) //
                .findFirst();
    }

    @Override
    @Transactional
    public List<TelemetrySettings> listSettings()
    {
        String query = "FROM TelemetrySettings";

        return entityManager.createQuery(query, TelemetrySettings.class).getResultList();
    }

    @Override
    @Transactional
    public <T> Optional<TelemetrySettings> readSettings(TelemetrySupport<T> aSupport)
    {
        String query = "FROM TelemetrySettings WHERE support = :support";

        List<TelemetrySettings> results = entityManager.createQuery(query, TelemetrySettings.class)
                .setParameter("support", aSupport.getId()).getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(results.get(0));
    }

    @Override
    @Transactional
    public <T> TelemetrySettings readOrCreateSettings(TelemetrySupport<T> aSupport)
    {
        String query = "FROM TelemetrySettings WHERE support = :support";

        List<TelemetrySettings> results = entityManager.createQuery(query, TelemetrySettings.class)
                .setParameter("support", aSupport.getId()).getResultList();

        if (!results.isEmpty()) {
            return results.get(0);
        }
        else {
            return new TelemetrySettings(aSupport);
        }
    }

    private void writeSettings(TelemetrySettings aSettings)
    {
        if (isNull(aSettings.getId())) {
            entityManager.persist(aSettings);
        }
        else {
            entityManager.merge(aSettings);
        }
    }

    @Override
    @Transactional
    public void writeAllSettings(List<TelemetrySettings> aSettings)
    {
        for (TelemetrySettings settings : aSettings) {
            writeSettings(settings);
        }

        eventPublisher.publishEvent(new TelemetrySettingsSavedEvent(this, aSettings));
    }
}
