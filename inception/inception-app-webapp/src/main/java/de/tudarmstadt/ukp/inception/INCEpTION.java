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
package de.tudarmstadt.ukp.inception;

import static com.giffing.wicket.spring.boot.starter.web.config.WicketWebInitializerAutoConfig.WebSocketWicketWebInitializerAutoConfiguration.REGISTER_SERVER_ENDPOINT_ENABLED;
import static de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil.getApplicationHome;
import static de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil.setGlobalLogFolder;
import static de.tudarmstadt.ukp.inception.INCEpTION.INCEPTION_BASE_PACKAGE;
import static de.tudarmstadt.ukp.inception.INCEpTION.WEBANNO_BASE_PACKAGE;
import static org.apache.uima.cas.impl.CASImpl.ALWAYS_HOLD_ONTO_FSS;
import static org.springframework.boot.WebApplicationType.SERVLET;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.validation.Validator;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.webresources.StandardRoot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import de.tudarmstadt.ukp.clarin.webanno.plugin.api.PluginManager;
import de.tudarmstadt.ukp.clarin.webanno.plugin.impl.PluginManagerImpl;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.standalone.LoadingSplashScreen;
import de.tudarmstadt.ukp.clarin.webanno.support.standalone.LoadingSplashScreen.SplashWindow;
import de.tudarmstadt.ukp.inception.app.config.InceptionApplicationContextInitializer;
import de.tudarmstadt.ukp.inception.app.config.InceptionBanner;
import de.tudarmstadt.ukp.inception.app.startup.StartupNoticeValve;

/**
 * Boots INCEpTION in standalone JAR or WAR modes.
 */
// @formatter:off
@SpringBootApplication
@AutoConfigurationPackage(basePackages = { INCEPTION_BASE_PACKAGE, WEBANNO_BASE_PACKAGE })
@EntityScan(basePackages = { INCEPTION_BASE_PACKAGE, WEBANNO_BASE_PACKAGE })
@ComponentScan(basePackages = { INCEPTION_BASE_PACKAGE, WEBANNO_BASE_PACKAGE },
    excludeFilters = {
        @Filter(type = FilterType.REGEX, pattern = ".*AutoConfiguration"),
        @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
@EnableAsync
@EnableCaching
@EnableGlobalMethodSecurity(prePostEnabled = true)
//@formatter:on
public class INCEpTION
    extends SpringBootServletInitializer
{
    static final String INCEPTION_BASE_PACKAGE = "de.tudarmstadt.ukp.inception";
    static final String WEBANNO_BASE_PACKAGE = "de.tudarmstadt.ukp.clarin.webanno";

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
            @Deprecated
            @Override
            public String getErrorPath()
            {
                return null;
            }

        };
    }

    @Bean
    public PluginManager pluginManager()
    {
        PluginManagerImpl pluginManager = new PluginManagerImpl(
                SettingsUtil.getApplicationHome().toPath().resolve("plugins"));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> pluginManager.stopPlugins()));
        return pluginManager;
    }

    // The WebAnno User model class picks this bean up by name!
    @Bean
    public PasswordEncoder passwordEncoder()
    {
        // Set up a DelegatingPasswordEncoder which decodes legacy passwords using the
        // StandardPasswordEncoder but encodes passwords using the modern BCryptPasswordEncoder
        String encoderForEncoding = "bcrypt";
        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put(encoderForEncoding, new BCryptPasswordEncoder());
        DelegatingPasswordEncoder delegatingEncoder = new DelegatingPasswordEncoder(
                encoderForEncoding, encoders);
        // Decode legacy passwords without encoder ID using the StandardPasswordEncoder
        delegatingEncoder.setDefaultPasswordEncoderForMatches(new StandardPasswordEncoder());
        return delegatingEncoder;
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
                StandardRoot standardRoot = new StandardRoot(context);
                standardRoot.setCacheMaxSize(maxCacheSize);
                context.setResources(standardRoot);
            }

            @Override
            public WebServer getWebServer(ServletContextInitializer... initializers)
            {
                final WebServer container = super.getWebServer(initializers);

                // Start server early so we can display the boot-up notice
                container.start();

                return container;
            }
        };

        startupNoticeValve = new StartupNoticeValve();
        factory.addContextValves(startupNoticeValve);

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

    @Override
    protected SpringApplicationBuilder createSpringApplicationBuilder()
    {
        SpringApplicationBuilder builder = super.createSpringApplicationBuilder();
        builder.properties("running.from.commandline=false");
        // add this property in the case of .war deployment
        builder.properties(REGISTER_SERVER_ENDPOINT_ENABLED + "=false");
        init(builder);

        return builder;
    }

    private static void init(SpringApplicationBuilder aBuilder)
    {
        // WebAnno relies on FS IDs being stable, so we need to enable this
        System.setProperty(ALWAYS_HOLD_ONTO_FSS, "true");

        aBuilder.banner(new InceptionBanner());
        aBuilder.initializers(new InceptionApplicationContextInitializer());
        aBuilder.headless(false);

        SettingsUtil.customizeApplication("inception.home", ".inception");

        // Traditionally, the INCEpTION configuration file is called settings.properties and is
        // either located in inception.home or under the user's home directory. Make sure we pick
        // it up from there in addition to reading the built-in application.yml file.
        aBuilder.properties("spring.config.additional-location="
                + "optional:${inception.home:${user.home}/.inception}/settings.properties");
    }

    protected static void run(String[] args, Class<?>... aSources)
    {
        Optional<SplashWindow> splash = LoadingSplashScreen.setupScreen("INCEpTION");

        SpringApplicationBuilder builder = new SpringApplicationBuilder();
        // Add the main application as the root Spring context
        builder.sources(aSources).web(SERVLET);

        // Signal that we may need the shutdown dialog
        builder.properties("running.from.commandline=true");
        init(builder);
        setGlobalLogFolder(getApplicationHome().toPath().resolve("log"));
        builder.listeners(event -> splash.ifPresent(_splash -> _splash.handleEvent(event)));
        builder.run(args);
    }

    public static void main(String[] args) throws Exception
    {
        run(args, INCEpTION.class);
    }
}
