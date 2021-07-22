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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.util.PublicSuffixMatcherLoader;
import org.apache.http.impl.client.DefaultUserTokenHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
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

    private static String[] split(final String s)
    {
        if (StringUtils.isBlank(s)) {
            return null;
        }
        return s.split(" *, *");
    }

    private static LayeredConnectionSocketFactory newCertCheckAwareSSLConnectionSocketFactory()
    {
        return new LayeredConnectionSocketFactory()
        {
            private SSLConnectionSocketFactory factoryWithChecks;
            private SSLConnectionSocketFactory factoryWithoutSslChecks;

            {
                final String[] supportedProtocols = split(System.getProperty("https.protocols"));
                final String[] supportedCipherSuites = split(
                        System.getProperty("https.cipherSuites"));

                HostnameVerifier defaultHostNameVerifier = new DefaultHostnameVerifier(
                        PublicSuffixMatcherLoader.getDefault());

                factoryWithChecks = new SSLConnectionSocketFactory(
                        (SSLSocketFactory) SSLSocketFactory.getDefault(), supportedProtocols,
                        supportedCipherSuites, defaultHostNameVerifier);

                try {
                    SSLContextBuilder builder = new SSLContextBuilder();
                    builder.loadTrustMaterial(null,
                            (X509Certificate[] chain, String authType) -> true);

                    HostnameVerifier hostNameVerifier = (String hostname,
                            SSLSession session) -> true;
                    factoryWithoutSslChecks = new SSLConnectionSocketFactory(builder.build(),
                            hostNameVerifier);
                }
                catch (Exception e) {
                    // key management exception, etc.
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Socket createSocket(HttpContext aContext) throws IOException
            {
                LOG.trace("createSocket (SSL checks: {})", SSL_VERIFICATION_ENABLED.get().peek());
                if (SSL_VERIFICATION_ENABLED.get().peek()) {
                    return factoryWithChecks.createSocket(aContext);
                }
                else {
                    return factoryWithoutSslChecks.createSocket(aContext);
                }
            }

            @Override
            public Socket connectSocket(int aConnectTimeout, Socket aSock, HttpHost aHost,
                    InetSocketAddress aRemoteAddress, InetSocketAddress aLocalAddress,
                    HttpContext aContext)
                throws IOException
            {
                LOG.trace("connectSocket (SSL checks: {})", SSL_VERIFICATION_ENABLED.get().peek());
                if (SSL_VERIFICATION_ENABLED.get().peek()) {
                    return factoryWithChecks.connectSocket(aConnectTimeout, aSock, aHost,
                            aRemoteAddress, aLocalAddress, aContext);
                }
                else {
                    return factoryWithoutSslChecks.connectSocket(aConnectTimeout, aSock, aHost,
                            aRemoteAddress, aLocalAddress, aContext);
                }
            }

            @Override
            public Socket createLayeredSocket(Socket aSocket, String aTarget, int aPort,
                    HttpContext aContext)
                throws IOException, UnknownHostException
            {
                LOG.trace("createLayeredSocket (SSL checks: {})",
                        SSL_VERIFICATION_ENABLED.get().peek());
                if (SSL_VERIFICATION_ENABLED.get().peek()) {
                    return factoryWithChecks.createLayeredSocket(aSocket, aTarget, aPort, aContext);
                }
                else {
                    return factoryWithoutSslChecks.createLayeredSocket(aSocket, aTarget, aPort,
                            aContext);
                }
            }
        };
    }

    private static UserTokenHandler newCertCheckAwareUserTokenHandler()
    {
        return new DefaultUserTokenHandler()
        {
            @Override
            public Object getUserToken(HttpContext aContext)
            {
                LOG.trace("getUserToken (SSL checks: {})", SSL_VERIFICATION_ENABLED.get().peek());

                if (SSL_VERIFICATION_ENABLED.get().peek()) {
                    return super.getUserToken(aContext);
                }
                else {
                    return new WrappedUserToken(super.getUserToken(aContext),
                            SSL_VERIFICATION_ENABLED.get().peek());
                }
            }
        };
    }

    /**
     * Return an {@link HttpClientBuilder} that can be used to build an {@link HttpClient} which
     * trusts all certificates (particularly including self-signed certificates).
     *
     * @return a {@link HttpClientBuilder} for <i>SSL trust all</i>
     */
    public static HttpClientBuilder newPerThreadSslCheckingHttpClientBuilder()
    {
        return HttpClients.custom() //
                // Need to inject the certificate checking state into the "user token" otherwise
                // HTTP connections with checking and without checking will be considered as
                // equivalent by the connection pool used by the HTTPClient
                .setUserTokenHandler(newCertCheckAwareUserTokenHandler()) //
                .setSSLSocketFactory(newCertCheckAwareSSLConnectionSocketFactory()) //
                .useSystemProperties();
    }

    public static class WrappedUserToken
    {
        private final Object userToken;
        private final boolean sslCheckSkipped;

        public WrappedUserToken(Object aUserToken, boolean aSslCheckSkipped)
        {
            super();
            userToken = aUserToken;
            sslCheckSkipped = aSslCheckSkipped;
        }

        public Object getUserToken()
        {
            return userToken;
        }

        public boolean isSslCheckSkipped()
        {
            return sslCheckSkipped;
        }

        @Override
        public boolean equals(final Object other)
        {
            if (!(other instanceof WrappedUserToken)) {
                return false;
            }
            WrappedUserToken castOther = (WrappedUserToken) other;
            return Objects.equals(userToken, castOther.userToken)
                    && Objects.equals(sslCheckSkipped, castOther.sslCheckSkipped);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(userToken, sslCheckSkipped);
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
