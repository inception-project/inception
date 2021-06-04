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

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonString;
import static java.lang.String.format;
import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

import de.tudarmstadt.ukp.clarin.webanno.api.identity.InstanceIdentityService;
import de.tudarmstadt.ukp.clarin.webanno.model.InstanceIdentity;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.DeploymentMode;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetryService;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.config.MatomoTelemetryServicePropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.model.TelemetrySettings;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class MatomoTelemetrySupportImplTest
{
    private MatomoTelemetrySupportImpl sut;

    private MockWebServer matomoServer;
    private MatomoTelemetryServicePropertiesImpl properties;
    private UUID instanceId;

    @BeforeEach
    public void setup() throws Exception
    {
        matomoServer = new MockWebServer();
        matomoServer.start();

        properties = new MatomoTelemetryServicePropertiesImpl();
        properties.setServerScheme("http");
        properties.setServerHost(matomoServer.getHostName() + ":" + matomoServer.getPort());
        properties.setServerPath("/");
        properties.setSiteId(1);

        instanceId = UUID.randomUUID();
        InstanceIdentityService identityService = mock(InstanceIdentityService.class);
        when(identityService.getInstanceIdentity())
                .thenReturn(new InstanceIdentity(instanceId.toString()));

        TelemetrySettings telemetrySettings = new TelemetrySettings();
        telemetrySettings.setVersion(2);
        telemetrySettings.setTraits("{\"enabled\": true}");
        TelemetryService telemetryService = mock(TelemetryService.class);
        when(telemetryService.readSettings(any())).thenReturn(Optional.of(telemetrySettings));
        when(telemetryService.getDeploymentMode()).thenReturn(DeploymentMode.DESKTOP);

        SessionRegistry sessionRegistry = mock(SessionRegistry.class);
        when(sessionRegistry.getAllPrincipals()).thenReturn(asList("John", "Jane"));
        when(sessionRegistry.getAllSessions(any(), anyBoolean())).thenAnswer(invocation -> {
            Object principal = invocation.getArgument(0);
            return asList(new SessionInformation(principal, "deadbeef", new Date()));
        });

        UserDao userService = mock(UserDao.class);
        when(userService.listEnabledUsers()).thenReturn(asList(new User("John"), new User("Jane")));

        sut = new MatomoTelemetrySupportImpl(telemetryService, identityService, userService,
                sessionRegistry, properties, "app");
    }

    @AfterEach
    public void teardown() throws Exception
    {
        matomoServer.shutdown();
    }

    @Test
    public void thatAliveIsRecieved() throws Exception
    {
        matomoServer.enqueue(new MockResponse().setResponseCode(200));

        sut.sendAlive();

        RecordedRequest aliveNotification = matomoServer.takeRequest();
        HttpUrl url = aliveNotification.getRequestUrl();
        assertThat(url.queryParameter("idsite")).isEqualTo(String.valueOf(properties.getSiteId()));
        assertThat(url.queryParameter("rec")).isEqualTo(String.valueOf(1));
        assertThat(url.queryParameter("apiv")).isEqualTo(String.valueOf(1));
        assertThat(url.queryParameter("send_image")).isEqualTo(String.valueOf(0));
        assertThat(url.queryParameter("uid")).isEqualTo(instanceId.toString());
        assertThat(url.queryParameter("action_name")).isEqualTo("alive");
        assertThat(url.queryParameter("_id"))
                .isEqualTo(format("%016x", instanceId.getMostSignificantBits()));
        assertThat(url.queryParameter("ua")).isNotNull();
        assertThat(url.queryParameter("url")).isEqualTo("https://webanno.github.io/telemetry");
        Map<?, ?> map = fromJsonString(HashMap.class, decode(url.queryParameter("_cvar"), UTF_8));
        assertThat(map.get("1")).isEqualTo(asList("app", "app"));
        assertThat(map.get("2")).isEqualTo(asList("version", "unknown"));
        assertThat(map.get("3")).isEqualTo(asList("activeUsers", "2"));
        assertThat(map.get("4")).isEqualTo(asList("enabledUsers", "2"));
        assertThat(map.get("5")).isEqualTo(asList("deploymentMode", "DESKTOP"));
    }

    @Test
    public void thatPingIsRecieved() throws Exception
    {
        matomoServer.enqueue(new MockResponse().setResponseCode(200));

        sut.sendPing();

        RecordedRequest aliveNotification = matomoServer.takeRequest();
        HttpUrl url = aliveNotification.getRequestUrl();
        assertThat(url.queryParameter("idsite")).isEqualTo(String.valueOf(properties.getSiteId()));
        assertThat(url.queryParameter("rec")).isEqualTo(String.valueOf(1));
        assertThat(url.queryParameter("apiv")).isEqualTo(String.valueOf(1));
        assertThat(url.queryParameter("send_image")).isEqualTo(String.valueOf(0));
        assertThat(url.queryParameter("uid")).isEqualTo(instanceId.toString());
        assertThat(url.queryParameter("action_name")).isEqualTo("ping");
        assertThat(url.queryParameter("_id"))
                .isEqualTo(format("%016x", instanceId.getMostSignificantBits()));
        assertThat(url.queryParameter("ua")).isNotNull();
        assertThat(url.queryParameter("url")).isEqualTo("https://webanno.github.io/telemetry");
        Map<?, ?> map = fromJsonString(HashMap.class, decode(url.queryParameter("_cvar"), UTF_8));
        assertThat(map.get("1")).isEqualTo(asList("app", "app"));
        assertThat(map.get("2")).isEqualTo(asList("version", "unknown"));
        assertThat(map.get("3")).isEqualTo(asList("activeUsers", "2"));
        assertThat(map.get("4")).isEqualTo(asList("enabledUsers", "2"));
        assertThat(map.get("5")).isEqualTo(asList("deploymentMode", "DESKTOP"));
    }
}
