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
package de.tudarmstadt.ukp.clarin.webanno.telemetry.matomo;

import static de.tudarmstadt.ukp.inception.support.SettingsUtil.PROP_VERSION;
import static de.tudarmstadt.ukp.inception.support.SettingsUtil.getVersionProperties;
import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.asList;
import static java.util.Collections.newSetFromMap;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.matomo.java.tracking.MatomoRequest;
import org.matomo.java.tracking.MatomoTracker;
import org.matomo.java.tracking.TrackerConfiguration;
import org.matomo.java.tracking.parameters.VisitorId;
import org.piwik.java.tracking.CustomVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.session.HttpSessionCreatedEvent;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetryDetail;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetryService;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.config.MatomoTelemetryServiceProperties;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.config.TelemetryServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.event.TelemetrySettingsSavedEvent;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.identity.InstanceIdentityService;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.model.TelemetrySettings;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link TelemetryServiceAutoConfiguration#matomoTelemetrySupport}.
 * </p>
 */
public class MatomoTelemetrySupportImpl
    implements MatomoTelemetrySupport, DisposableBean
{
    public static final String MATOMO_TELEMETRY_SUPPORT_ID = "MatomoTelemetry";
    public static final int CURRENT_SETTINGS_VERSION = 3;

    public static final String ACTION_BOOT = "boot";
    public static final String ACTION_HELLO = "hello";
    public static final String ACTION_PING = "ping";
    public static final String ACTION_ALIVE = "alive";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final InstanceIdentityService identityService;
    private final TelemetryService telemetryService;
    private final UserDao userService;
    private final String applicationName;
    private final SessionRegistry sessionRegistry;
    private final MatomoTelemetryServiceProperties properties;

    // Set of all principals that were active in the interval between one PING and the next. Gets
    // cleared when the PING is sent.
    private final Set<Object> activePrincipals = newSetFromMap(new ConcurrentHashMap<>());

    private ScheduledExecutorService scheduler;
    private boolean trackerInitialized = false;
    private MatomoTracker tracker;
    // private PiwikConfig config;

    @Autowired
    public MatomoTelemetrySupportImpl(TelemetryService aTelemetryService,
            InstanceIdentityService aIdentityService, UserDao aUserDao,
            SessionRegistry aSessionRegistry, MatomoTelemetryServiceProperties aMatomoProperties,
            @Value("${spring.application.name}") String aApplicationName)
    {
        telemetryService = aTelemetryService;
        identityService = aIdentityService;
        userService = aUserDao;
        applicationName = aApplicationName;
        sessionRegistry = aSessionRegistry;
        properties = aMatomoProperties;
    }

    private MatomoTracker getTracker()
    {
        if (trackerInitialized) {
            return tracker;
        }

        // This is even set to true of setting up the tracker fails to avoid trying to set up the
        // tracker over and over again.
        trackerInitialized = true;

        try {
            var url = properties.getServerScheme() + "://" + properties.getServerHost()
                    + prependIfMissing(properties.getServerPath(), "/");

            tracker = new MatomoTracker(TrackerConfiguration.builder() //
                    .enabled(true) //
                    .apiEndpoint(URI.create(url)) //
                    .connectTimeout(Duration.of(30, SECONDS)) //
                    .socketTimeout(Duration.of(30, SECONDS)) //
                    .disableSslCertValidation(true) //
                    .disableSslHostVerification(true) //
                    .build());
        }
        catch (Exception e) {
            LOG.info("Unable to set up telemetry client: {}", e.getMessage());
            tracker = null;
        }

        return tracker;
    }

    @Override
    public String getName()
    {
        return "Anonymous usage statistics";
    }

    @Override
    public String getId()
    {
        return MATOMO_TELEMETRY_SUPPORT_ID;
    }

    @Override
    public int getVersion()
    {
        return CURRENT_SETTINGS_VERSION;
    }

    @Override
    public boolean hasValidSettings()
    {
        var settings = telemetryService.readSettings(this);

        if (!settings.isPresent()) {
            return false;
        }

        var outdated = settings.get().getVersion() != getVersion();
        if (outdated) {
            return false;
        }

        return settings.map(this::readTraits) //
                .map(traits -> traits.isEnabled() != null) //
                .orElse(false);
    }

    public boolean isEnabled()
    {
        var settings = telemetryService.readSettings(this);

        if (!settings.isPresent()) {
            return false;
        }

        boolean outdated = settings.get().getVersion() < getVersion();
        if (outdated) {
            return false;
        }

        return settings.map(this::readTraits)
                .map(traits -> traits.isEnabled() != null && traits.isEnabled() == true)
                .orElse(false);
    }

    public boolean isEnabled(List<TelemetrySettings> aSettings)
    {
        // When we get this event, we don't really need to check for the version since this event
        // is generated from the settings page itself when the settings are updated - thus they
        // must have the latest version.

        return aSettings.stream() //
                .filter(settings -> settings.getSupport().equals(getId())).findFirst()
                .map(this::readTraits)
                .map(traits -> traits.isEnabled() != null && traits.isEnabled() == true)
                .orElse(false);
    }

    @Override
    public void destroy() throws Exception
    {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    @EventListener
    @Async
    public void onApplicationReady(ApplicationReadyEvent aEvent)
    {
        if (isEnabled()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();

            // Pinging at a 29 minute interval allows Matomo to detect continuous activity. Pings
            // are
            // only send if here are actually active users logged in.
            scheduler.scheduleAtFixedRate(() -> sendPing(), 29, 29, TimeUnit.MINUTES);

            // Server deployments are expected to run for a very long time. Also, they may be
            // without
            // any active users for a long time. So we send an alive signal every 24 hours no matter
            // if any users are actively logged in
            scheduler.scheduleAtFixedRate(() -> sendAlive(), 24, 24, TimeUnit.HOURS);

            sendTelemetry(ACTION_BOOT);
        }
        else {
            LOG.debug("Telemetry disabled");
        }
    }

    @Override
    @EventListener
    @Async
    public void onTelemetrySettingsSaved(TelemetrySettingsSavedEvent aEvent)
    {
        if (isEnabled(aEvent.getSettings())) {
            sendTelemetry(ACTION_HELLO);
        }
        else {
            resetActivePrincipals();
            LOG.debug("Telemetry disabled");
        }
    }

    private void updateActivePrincipals()
    {
        // Collect all active principals
        var allPrincipals = sessionRegistry.getAllPrincipals();
        allPrincipals.stream()
                .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
                .forEach(sessionInfo -> activePrincipals.add(sessionInfo.getPrincipal()));
    }

    private void resetActivePrincipals()
    {
        activePrincipals.clear();
    }

    @Override
    @EventListener
    public void onSessionCreated(HttpSessionCreatedEvent aEvent)
    {
        // Listen explicitly ot all sessions so we catch sessions that get created and destroyed
        // within a PING period.
        if (isEnabled()) {
            updateActivePrincipals();
        }
    }

    void sendPing()
    {
        if (!isEnabled()) {
            return;
        }

        // Check if there are any active (non-expired) sessions - if yes, we send a ping.
        updateActivePrincipals();
        var hasActiveSessions = !activePrincipals.isEmpty();

        if (!hasActiveSessions) {
            LOG.debug("Telemetry detected no active principals: {}", activePrincipals);
            return;
        }
        else {
            LOG.debug("Telemetry detected active principals: {}", activePrincipals);
        }

        // Clear the active principals from the last period and perform an initial collection for
        // the new period
        resetActivePrincipals();
        updateActivePrincipals();

        sendTelemetry(ACTION_PING);
    }

    void sendAlive()
    {
        if (!isEnabled()) {
            return;
        }

        sendTelemetry(ACTION_ALIVE);
    }

    private void sendTelemetry(String aAction)
    {
        if (getTracker() == null) {
            LOG.debug("Telemetry unavailable");
            return;
        }

        try {
            var id = identityService.getInstanceIdentity();

            var uuid = UUID.fromString(id.getId());

            var request = MatomoRequest.request() //
                    .siteId(properties.getSiteId()) //
                    .actionUrl(properties.getContext()) //
                    .build();
            request.setHeaderUserAgent(System.getProperty("os.name"));
            request.setActionName(aAction);
            request.setVisitorId(VisitorId.fromHex(format("%016x", uuid.getMostSignificantBits())));
            request.setUserId(id.getId());
            request.setVisitCustomVariable(new CustomVariable("app", applicationName), 1);
            request.setVisitCustomVariable(
                    new CustomVariable("version", getVersionProperties().getProperty(PROP_VERSION)),
                    2);
            request.setVisitCustomVariable(new CustomVariable("activeUsers",
                    String.valueOf(sessionRegistry.getAllPrincipals().size())), 3);
            request.setVisitCustomVariable(new CustomVariable("enabledUsers",
                    String.valueOf(userService.listEnabledUsers().size())), 4);
            request.setVisitCustomVariable(new CustomVariable("deploymentMode",
                    telemetryService.getDeploymentMode().toString()), 5);

            getTracker().sendRequestAsync(request);
            LOG.debug("Telemetry sent ({})", aAction);
        }
        catch (Exception e) {
            LOG.debug("Unable to send telemetry server", e);
        }
    }

    @Override
    public List<TelemetryDetail> getDetails()
    {
        var id = identityService.getInstanceIdentity();
        var uuid = UUID.fromString(id.getId());

        return asList(new TelemetryDetail("Instance ID", id.getId(),
                "Unique anonymous identifier for your installation. This value is randomly "
                        + "generated when the application is started for the first time and then "
                        + "stored in the database. By collecting this value, we can count the "
                        + "number of installations and report it e.g. when applying for funding."),
                new TelemetryDetail("Visitor ID", Long.toHexString(uuid.getMostSignificantBits()),
                        "This is a short version of the instance ID required for technical "
                                + "reasons by the Matomo telemetry server we are using."),
                new TelemetryDetail("Application", applicationName,
                        "The name of the application. There are different applications using this "
                                + "telemetry service. By this value, we can identify the popularity of "
                                + "these different applications."),
                new TelemetryDetail("Version", getVersionProperties().getProperty(PROP_VERSION),
                        "The version of the application. This helps us assess whether old "
                                + "versions are still used (e.g. to determine if a maintenance release "
                                + "is worth the effort) and also helps us discover how quickly users "
                                + "upgrade to new versions"),
                new TelemetryDetail("Operating system", System.getProperty("os.name"),
                        "The operating system of the host running the application. This helps us "
                                + "discover which operating systems are important to our users and should "
                                + "be supported."),
                new TelemetryDetail("Active users",
                        String.valueOf(sessionRegistry.getAllPrincipals().size()),
                        "The number of users logged in to the instance. Since a single instance "
                                + "can support many users, this is an important number for us to discover "
                                + "the size of our user base. Again, this number is important e.g. when "
                                + "reporting to funders."),
                new TelemetryDetail("Enabled users",
                        String.valueOf(userService.listEnabledUsers().size()),
                        "The number of enabled users in the application. Since a single instance "
                                + "can support many users, this is an important number for us to discover "
                                + "the size of our user base. Again, this number is important e.g. when "
                                + "reporting to funders."),
                new TelemetryDetail("Deployment mode",
                        String.valueOf(telemetryService.getDeploymentMode()),
                        "The mode of deployment. Desktop-based deployments are expected to be "
                                + "restarted regularly while server-based deployments are expected to be "
                                + "running for a long time."));
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<TelemetrySettings> aSettings)
    {
        return new MatomoTelemetryTraitsEditor(aId, aSettings);
    }

    @Override
    public MatomoTelemetryTraits readTraits(TelemetrySettings aSettings)
    {
        MatomoTelemetryTraits traits = null;
        try {
            traits = JSONUtil.fromJsonString(MatomoTelemetryTraits.class, aSettings.getTraits());
        }
        catch (IOException e) {
            LOG.error("Unable to read traits", e);
        }

        if (traits == null) {
            traits = new MatomoTelemetryTraits();
        }

        return traits;
    }

    @Override
    public void writeTraits(TelemetrySettings aSettings, MatomoTelemetryTraits aTraits)
    {
        try {
            aSettings.setTraits(JSONUtil.toJsonString(aTraits));
        }
        catch (IOException e) {
            LOG.error("Unable to write traits", e);
        }
    }

    @Override
    public void acceptAll(MatomoTelemetryTraits aTraits)
    {
        aTraits.setEnabled(true);
    }

    @Override
    public void rejectAll(MatomoTelemetryTraits aTraits)
    {
        aTraits.setEnabled(false);
    }
}
