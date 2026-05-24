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
package de.tudarmstadt.ukp.inception.kb.http;

import static java.lang.ThreadLocal.withInitial;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Deque;
import java.util.LinkedList;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.rdf4j.http.client.spi.HttpRequest;
import org.eclipse.rdf4j.http.client.spi.HttpResponse;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClient;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClientConfig;
import org.eclipse.rdf4j.http.client.spi.RDF4JHttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerThreadSslCheckingHttpClientUtils
{
    private static final Logger LOG = LoggerFactory
            .getLogger(PerThreadSslCheckingHttpClientUtils.class);

    private static final ThreadLocal<Deque<Boolean>> SSL_VERIFICATION_ENABLED = withInitial(
            () -> new LinkedList<Boolean>(asList(true)));

    public static void pushSslVerification(boolean aCheckEnabled)
    {
        SSL_VERIFICATION_ENABLED.get().push(aCheckEnabled);
    }

    public static void suspendSslVerification()
    {
        SSL_VERIFICATION_ENABLED.get().push(false);
    }

    public static void restoreSslVerification()
    {
        Deque<Boolean> stack = SSL_VERIFICATION_ENABLED.get();

        if (stack.size() == 1) {
            throw new IllegalStateException(
                    "SSL verification stack underflow - request to restore SSL verification state ignored.");
        }

        stack.pop();
    }

    public static SslCertificateCheckingContext withSslCertificateChecks(boolean aCheckEnabled)
    {
        return new SslCertificateCheckingContext(aCheckEnabled);
    }

    public static SslCertificateCheckingContext skipCertificateChecks(boolean aSkipChecks)
    {
        return new SslCertificateCheckingContext(!aSkipChecks);
    }

    /**
     * Return an {@link RDF4JHttpClient} that delegates each request to one of two underlying
     * clients based on the per-thread SSL-verification flag controlled by
     * {@link #pushSslVerification(boolean)}, {@link #suspendSslVerification()}, and
     * {@link #restoreSslVerification()}. Allows untrusted certificates to be tolerated for specific
     * user-initiated operations (e.g. KB connection tests) without weakening SSL for the rest of
     * the application.
     */
    public static RDF4JHttpClient newPerThreadSslCheckingHttpClient()
    {
        return new PerThreadSslCheckingRdf4jHttpClient();
    }

    private static SSLContext newTrustAllSslContext()
    {
        try {
            var ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[] { new X509TrustManager()
            {
                @Override
                public void checkClientTrusted(X509Certificate[] aChain, String aAuthType)
                {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] aChain, String aAuthType)
                {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers()
                {
                    return new X509Certificate[0];
                }
            } }, null);
            return ctx;
        }
        catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class PerThreadSslCheckingRdf4jHttpClient
        implements RDF4JHttpClient
    {
        private final RDF4JHttpClient strictClient;
        private final RDF4JHttpClient trustAllClient;

        PerThreadSslCheckingRdf4jHttpClient()
        {
            strictClient = RDF4JHttpClients.newDefaultClient();
            trustAllClient = RDF4JHttpClients.newDefaultClient(RDF4JHttpClientConfig.newBuilder() //
                    .sslContext(newTrustAllSslContext()) //
                    .disableHostnameVerification(true) //
                    .build());
        }

        @Override
        public HttpResponse execute(HttpRequest aRequest) throws IOException
        {
            boolean checksEnabled = SSL_VERIFICATION_ENABLED.get().peek();
            LOG.trace("execute (SSL checks: {})", checksEnabled);
            return (checksEnabled ? strictClient : trustAllClient).execute(aRequest);
        }

        @Override
        public void close()
        {
            try {
                strictClient.close();
            }
            finally {
                trustAllClient.close();
            }
        }
    }

    public static class SslCertificateCheckingContext
        implements AutoCloseable
    {
        public SslCertificateCheckingContext(boolean aChecksEnabled)
        {
            pushSslVerification(aChecksEnabled);
        }

        @Override
        public void close()
        {
            restoreSslVerification();
        }
    };
}
