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

import static de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil.PROP_VERSION;
import static de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil.getVersionProperties;
import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.asList;
import static java.util.Collections.newSetFromMap;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.piwik.java.tracking.CustomVariable;
import org.piwik.java.tracking.PiwikRequest;
import org.piwik.java.tracking.PiwikTracker;
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

import de.tudarmstadt.ukp.clarin.webanno.api.identity.InstanceIdentityService;
import de.tudarmstadt.ukp.clarin.webanno.model.InstanceIdentity;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetryDetail;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetryService;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.config.MatomoTelemetryServiceProperties;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.config.TelemetryServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.event.TelemetrySettingsSavedEvent;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.model.TelemetrySettings;

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

    public static final String ACTION_BOOT = "boot";
    public static final String ACTION_HELLO = "hello";
    public static final String ACTION_PING = "ping";
    public static final String ACTION_ALIVE = "alive";

    private final Logger log = LoggerFactory.getLogger(getClass());

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
    private PiwikTracker tracker;
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

    private PiwikTracker getTracker()
    {
        if (trackerInitialized) {
            return tracker;
        }

        // This is even set to true of setting up the tracker fails to avoid trying to set up the
        // tracker over and over again.
        trackerInitialized = true;

        try {
            String url = properties.getServerScheme() + "://" + properties.getServerHost()
                    + prependIfMissing(properties.getServerPath(), "/");
            tracker = new SslIgnoringPiwikTracker(url, Duration.of(30, SECONDS));
        }
        catch (Exception e) {
            log.info("Unable to set up telemetry client: {}", e.getMessage());
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
        return 2;
    }

    @Override
    public boolean hasValidSettings()
    {
        Optional<TelemetrySettings> settings = telemetryService.readSettings(this);

        if (!settings.isPresent()) {
            return false;
        }

        boolean outdated = settings.get().getVersion() < getVersion();
        if (outdated) {
            return false;
        }

        return settings.map(this::readTraits).map(traits -> traits.isEnabled() != null)
                .orElse(false);
    }

    public boolean isEnabled()
    {
        Optional<TelemetrySettings> settings = telemetryService.readSettings(this);

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
            log.debug("Telemetry disabled");
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
            log.debug("Telemetry disabled");
        }
    }

    private void updateActivePrincipals()
    {
        // Collect all active principals
        List<Object> allPrincipals = sessionRegistry.getAllPrincipals();
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
        boolean hasActiveSessions = !activePrincipals.isEmpty();

        if (!hasActiveSessions) {
            log.debug("Telemetry detected no active principals: {}", activePrincipals);
            return;
        }
        else {
            log.debug("Telemetry detected active principals: {}", activePrincipals);
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
            log.debug("Telemetry unavailable");
            return;
        }

        try {
            InstanceIdentity id = identityService.getInstanceIdentity();

            UUID uuid = UUID.fromString(id.getId());

            PiwikRequest request = new PiwikRequest(properties.getSiteId(),
                    new URL(properties.getContext()));
            request.setHeaderUserAgent(System.getProperty("os.name"));
            request.setActionName(aAction);
            request.setVisitorId(format("%016x", uuid.getMostSignificantBits()));
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
            log.debug("Telemetry sent ({})", aAction);
        }
        catch (IOException e) {
            log.debug("Unable to send telemetry server", e);
        }
    }

    @Override
    public List<TelemetryDetail> getDetails()
    {
        InstanceIdentity id = identityService.getInstanceIdentity();
        UUID uuid = UUID.fromString(id.getId());

        return asList(new TelemetryDetail("Instance ID", id.getId(),
                "Unique anonymous identifier for your installation. This value is randomly "
                        + "generated when the application is started for the first time and then "
                        + "stored in the database. By collecting this value, we can count the "
                        + "number of installations and report it e.g. when applying for funding."),
                new TelemetryDetail("Visitor ID", Long.toHexString(uuid.getMostSignificantBits()),
                        "This is a short version of the instance ID required for technical "
                                + "resons by the Matomo telemetry server we are using."),
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

    private static final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager()
    {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
            throws CertificateException
        {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
            throws CertificateException
        {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers()
        {
            return new java.security.cert.X509Certificate[] {};
        }
    } };

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
            log.error("Unable to read traits", e);
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
            log.error("Unable to write traits", e);
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

    private class SslIgnoringPiwikTracker
        extends PiwikTracker
    {
        private final Duration timeout;
        private CloseableHttpAsyncClient asyncClient = null;

        public SslIgnoringPiwikTracker(String aHostUrl, Duration aTimeout)
        {
            super(aHostUrl, (int) aTimeout.toMillis());
            timeout = aTimeout;
        }

        @Override
        protected CloseableHttpAsyncClient getHttpAsyncClient()
        {
            if (asyncClient != null) {
                return asyncClient;
            }

            try {
                HttpAsyncClientBuilder builder = HttpAsyncClientBuilder.create();
                builder.setDefaultRequestConfig(RequestConfig.custom() //
                        .setConnectTimeout((int) timeout.toMillis()) //
                        .setConnectionRequestTimeout((int) timeout.toMillis()) //
                        .setSocketTimeout((int) timeout.toMillis()).build());
                builder.setSSLContext(new SSLContextBuilder()
                        .loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build());
                builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                asyncClient = builder.build();
            }
            catch (KeyStoreException | KeyManagementException | NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }

            return asyncClient;
        }
    }
}
