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
package de.tudarmstadt.ukp.inception.app.config;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.webresources.StandardRoot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import de.tudarmstadt.ukp.inception.app.startup.StartupNoticeValve;
import jakarta.validation.Validator;

@Component
public class InceptionApplicationContainerConfiguration
{
    private static final String PROTOCOL = "AJP/1.3";

    @Value("${server.ajp.port:-1}")
    private int ajpPort;

    @Value("${server.ajp.secret-required:true}")
    private String ajpSecretRequired;

    @Value("${server.ajp.secret:}")
    private String ajpSecret;

    @Value("${server.ajp.address:127.0.0.1}")
    private String ajpAddress;

    private StartupNoticeValve startupNoticeValve;

    @Value("${server.startup-notice.enabled:true}")
    private boolean statupNoticeEnabled;

    @Bean
    @Primary
    public Validator validator()
    {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public ErrorController errorController()
    {
        // Disable default error controller so that Wicket can take over
        return new ErrorController()
        {
        };
    }

    @Bean
    public TomcatServletWebServerFactory servletContainer()
    {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory()
        {
            @Override
            protected void postProcessContext(Context context)
            {
                final int maxCacheSize = 40 * 1024;
                var standardRoot = new StandardRoot(context);
                standardRoot.setCacheMaxSize(maxCacheSize);
                context.setResources(standardRoot);
            }

            @Override
            public WebServer getWebServer(ServletContextInitializer... initializers)
            {
                final var container = super.getWebServer(initializers);

                // Start server early so we can display the boot-up notice
                container.start();

                return container;
            }
        };

        if (statupNoticeEnabled) {
            startupNoticeValve = new StartupNoticeValve();
            factory.addContextValves(startupNoticeValve);
        }

        if (ajpPort > 0) {
            Connector ajpConnector = new Connector(PROTOCOL);
            ajpConnector.setPort(ajpPort);
            ajpConnector.setProperty("secretRequired", ajpSecretRequired);
            ajpConnector.setProperty("secret", ajpSecret);
            ajpConnector.setProperty("address", ajpAddress);
            factory.addAdditionalTomcatConnectors(ajpConnector);
        }

        return factory;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event)
    {
        if (startupNoticeValve != null && startupNoticeValve.getContainer() != null) {
            startupNoticeValve.getContainer().getPipeline().removeValve(startupNoticeValve);
            startupNoticeValve = null;
        }
    }
}
