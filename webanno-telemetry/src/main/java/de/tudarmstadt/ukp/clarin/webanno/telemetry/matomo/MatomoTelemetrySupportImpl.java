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
package de.tudarmstadt.ukp.clarin.webanno.telemetry.matomo;

import static ch.rasc.piwik.tracking.QueryParameter.ACTION_NAME;
import static ch.rasc.piwik.tracking.QueryParameter.HEADER_USER_AGENT;
import static ch.rasc.piwik.tracking.QueryParameter.USER_ID;
import static ch.rasc.piwik.tracking.QueryParameter.VISITOR_ID;
import static ch.rasc.piwik.tracking.QueryParameter.VISIT_CUSTOM_VARIABLE;
import static de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil.PROP_VERSION;
import static de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil.getVersionProperties;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

import ch.rasc.piwik.tracking.PiwikConfig;
import ch.rasc.piwik.tracking.PiwikRequest;
import ch.rasc.piwik.tracking.PiwikTracker;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;

import de.tudarmstadt.ukp.clarin.webanno.api.identity.InstanceIdentityService;
import de.tudarmstadt.ukp.clarin.webanno.model.InstanceIdentity;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetryDetail;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetryService;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.event.TelemetrySettingsSavedEvent;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.model.TelemetrySettings;
import okhttp3.OkHttpClient;

@Component
public class MatomoTelemetrySupportImpl
    implements MatomoTelemetrySupport, DisposableBean
{
    public static final String MATOMO_TELEMETRY_SUPPORT_ID = "MatomoTelemetry";
    
    public static final String ACTION_BOOT = "boot";
    public static final String ACTION_HELLO = "hello";
    public static final String ACTION_PING = "ping";
    public static final String ACTION_ALIVE = "alive";
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final static String TELEMETRY_SERVER_SCHEME = "https";
    private final static String TELEMETRY_SERVER_HOST = "blinky.ukp.informatik.tu-darmstadt.de";
    private final static String TELEMETRY_SERVER_PATH = "matomo/piwik.php";
    private final static String TELEMETRY_SITE_ID = "2";
    private final static String TELEMETRY_CONTEXT = "https://webanno.github.io/telemetry";
    
    private final InstanceIdentityService identityService;
    private final TelemetryService telemetryService;
    private final UserDao userService;
    private final String applicationName;
    private final SessionRegistry sessionRegistry;
    private final ScheduledExecutorService scheduler;

    private PiwikTracker tracker;
    
    @Autowired
    public MatomoTelemetrySupportImpl(TelemetryService aTelemetryService,
            InstanceIdentityService aIdentityService, UserDao aUserDao,
            SessionRegistry aSessionRegistry,
            @Value("${spring.application.name}") String aApplicationName)
    {
        telemetryService = aTelemetryService;
        identityService = aIdentityService;
        userService = aUserDao;
        applicationName = aApplicationName;
        sessionRegistry = aSessionRegistry;
        
        PiwikConfig config = new PiwikConfig.Builder()
                .scheme(TELEMETRY_SERVER_SCHEME)
                .host(TELEMETRY_SERVER_HOST)
                .path(TELEMETRY_SERVER_PATH)
                .addIdSite(TELEMETRY_SITE_ID)
                .build();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // Pinging at a 29 minute interval allows Matomo to detect continuous activity. Pings are
        // only send if here are actually active users logged in.
        scheduler.scheduleAtFixedRate(() -> sendPing(), 29, 29, TimeUnit.MINUTES);
        
        // Server deployments are expected to run for a very long time. Also, they may be without
        // any active users for a long time. So we send an alive signal every 24 hours no matter
        // if any users are actively logged in
        scheduler.scheduleAtFixedRate(() -> sendAlive(), 24, 24, TimeUnit.HOURS);
        
        try {
            SSLContext trustAllSslContext = SSLContext.getInstance("SSL");
            trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            OkHttpClient client = new OkHttpClient.Builder()
                    .sslSocketFactory(trustAllSslContext.getSocketFactory(),
                            (X509TrustManager) trustAllCerts[0])
                    .build();

            tracker = new PiwikTracker(config, client);
        }
        catch (Exception e) {
            log.info("Unable to set up telemetry client: {}", e.getMessage());
            tracker = null;
        }
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
        
        return settings.map(this::readTraits)
                .map(traits -> traits.isEnabled() != null)
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
        
        return aSettings.stream()
                .filter(settings -> settings.getSupport().equals(getId()))
                .findFirst()
                .map(this::readTraits)
                .map(traits -> traits.isEnabled() != null && traits.isEnabled() == true)
                .orElse(false);
    }

    @Override
    public void destroy() throws Exception
    {
        scheduler.shutdownNow();
    }
    
    @Override
    @EventListener
    @Async
    public void onApplicationReady(ApplicationReadyEvent aEvent)
    {
        if (isEnabled()) {
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
            log.debug("Telemetry disabled");
        }
    }
    
    private void sendPing()
    {
        if (!isEnabled()) {
            return;
        }

        // Check if there are any active (non-expired) sessions - if yes, we send a ping.
        List<Object> allPrincipals = sessionRegistry.getAllPrincipals();
        boolean hasActiveSessions = allPrincipals.stream()
                .map(principal -> sessionRegistry.getAllSessions(principal, false))
                .anyMatch(list -> !list.isEmpty());
        
        if (!hasActiveSessions) {
            return;
        }
        else {
            log.debug("Telemetry detected active principals: {}", allPrincipals);
        }

        sendTelemetry(ACTION_PING);
    }

    private void sendAlive()
    {
        if (!isEnabled()) {
            return;
        }

        sendTelemetry(ACTION_ALIVE);
    }

    private void sendTelemetry(String aAction)
    {
        if (tracker == null) {
            log.debug("Telemetry unavailable");
            return;
        }
        
        InstanceIdentity id = identityService.getInstanceIdentity();
        
        UUID uuid = UUID.fromString(id.getId());
        
        JSONObject payload = new JSONObject();
        payload.put("1", new JSONArray(asList("app", applicationName)));
        payload.put("2",
                new JSONArray(asList("version", getVersionProperties().getProperty(PROP_VERSION))));
        payload.put("3", new JSONArray(
                asList("activeUsers", String.valueOf(sessionRegistry.getAllPrincipals().size()))));
        payload.put("4", new JSONArray(
                asList("enabledUsers", String.valueOf(userService.listEnabledUsers().size()))));
        payload.put("5", new JSONArray(
                asList("deploymentMode", telemetryService.getDeploymentMode().toString())));
        
        PiwikRequest request = PiwikRequest.builder()
                .url(TELEMETRY_CONTEXT)
                .putParameter(HEADER_USER_AGENT, System.getProperty("os.name"))
                .putParameter(ACTION_NAME, aAction)
                .putParameter(VISITOR_ID, Long.toHexString(uuid.getMostSignificantBits()))
                .putParameter(USER_ID, id.getId())
                .putParameter(VISIT_CUSTOM_VARIABLE, payload.toString())
                .build();

        boolean success = tracker.send(request);

        if (success) {
            log.debug("Telemetry sent ({})", aAction);
        }
        else {
            log.debug("Unable to reach telemetry server");
        }
    }
    
    @Override
    public List<TelemetryDetail> getDetails()
    {
        InstanceIdentity id = identityService.getInstanceIdentity();
        UUID uuid = UUID.fromString(id.getId());
        
        return asList(
                new TelemetryDetail("Instance ID", id.getId(), 
                        "Unique anonyous identifier for your installation. This value is randomly "
                        + "generated when the application is started for the first time and then "
                        + "stored in the database. By collecting this value, we can count the "
                        + "number of installations and report it e.g. when applying for funding."),
                new TelemetryDetail("Visitor ID", Long.toHexString(uuid.getMostSignificantBits()), 
                        "This is a short version of the instance ID required for technical "
                        + "resons by the Matomo telemetry server we are using."),
                new TelemetryDetail("Appliction", applicationName, 
                        "The name of the application. There are different applications using this"
                        + "telemetry service By this value, we can identify the popularity of "
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
                        + "running for a long time.")
                );
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
}
