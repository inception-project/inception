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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.webhooks;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class HttpWebhookDriver
    implements WebhookDriver
{
    public static final String ID = "http";

    private final RestTemplateBuilder restTemplateBuilder;
    private HttpComponentsClientHttpRequestFactory nonValidatingRequestFactory = null;

    public HttpWebhookDriver(RestTemplateBuilder aRestTemplateBuilder)
        throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException
    {
        restTemplateBuilder = aRestTemplateBuilder;

        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
        HostnameVerifier verifier = (String aHostname, SSLSession aSession) -> true;

        var sslContext = SSLContexts.custom() //
                .loadTrustMaterial(null, acceptingTrustStrategy) //
                .build();

        var cm = PoolingHttpClientConnectionManagerBuilder.create() //
                .setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext, verifier)) //
                .build();

        var httpClient = HttpClients.custom() //
                .setConnectionManager(cm) //
                .build();

        nonValidatingRequestFactory = new HttpComponentsClientHttpRequestFactory();
        nonValidatingRequestFactory.setHttpClient(httpClient);
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public void sendNotification(String aTopic, Object aMessage, Webhook aHook) throws IOException
    {
        WebhookService.LOG.trace("Sending webhook message on topic [{}] to [{}]", aTopic,
                aHook.getUrl());

        // Configure rest template without SSL certification check if that is disabled.
        RestTemplate restTemplate;
        if (aHook.isVerifyCertificates()) {
            restTemplate = restTemplateBuilder.build();
        }
        else {
            restTemplate = restTemplateBuilder.requestFactory(this::getNonValidatingRequestFactory)
                    .build();
        }

        var requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.set(X_AERO_NOTIFICATION, aTopic);

        // If a secret is set, then add a digest header that allows the client to verify
        // the message integrity
        var json = JSONUtil.toJsonString(aMessage);
        if (isNotBlank(aHook.getSecret())) {
            var digest = DigestUtils.sha1Hex(aHook.getSecret() + json);
            requestHeaders.set(X_AERO_SIGNATURE, digest);
        }

        if (isNotBlank(aHook.getAuthHeader()) && isNotBlank(aHook.getAuthHeaderValue())) {
            requestHeaders.set(aHook.getAuthHeader(), aHook.getAuthHeaderValue());
        }

        var httpEntity = new HttpEntity<Object>(json, requestHeaders);
        restTemplate.postForEntity(aHook.getUrl(), httpEntity, Void.class);
    }

    private HttpComponentsClientHttpRequestFactory getNonValidatingRequestFactory()
    {
        return nonValidatingRequestFactory;
    }
}
