/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception;

import static org.springframework.boot.WebApplicationType.SERVLET;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.swing.JWindow;
import javax.validation.Validator;

import org.apache.catalina.connector.Connector;
import org.apache.uima.cas.impl.CASImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.giffing.wicket.spring.boot.starter.web.config.WicketWebInitializerAutoConfig.WebSocketWicketWebInitializerAutoConfiguration;

import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.export.AutomationMiraTemplateExporter;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.export.AutomationTrainingDocumentExporter;
import de.tudarmstadt.ukp.clarin.webanno.plugin.api.PluginManager;
import de.tudarmstadt.ukp.clarin.webanno.plugin.impl.PluginManagerImpl;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.standalone.LoadingSplashScreen;
import de.tudarmstadt.ukp.clarin.webanno.support.standalone.ShutdownDialogAvailableEvent;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.CurationPageMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.page.AgreementPageMenuItem;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.page.MonitoringPageMenuItem;
import de.tudarmstadt.ukp.inception.app.config.InceptionApplicationContextInitializer;
import de.tudarmstadt.ukp.inception.app.config.InceptionBanner;

/**
 * Boots INCEpTION in standalone JAR or WAR modes.
 */
@SpringBootApplication
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ComponentScan(
        basePackages = { 
                "de.tudarmstadt.ukp.inception",  
                "de.tudarmstadt.ukp.clarin.webanno" },
        excludeFilters = {
            @Filter(type = FilterType.REGEX, pattern = ".*AutoConfiguration"),
            @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
            @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
            @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { 
                // The INCEpTION dashboard uses a per-project view while WebAnno uses a global
                // activation strategies for menu items. Thus, we need to re-implement the menu
                // items for INCEpTION.
                AnnotationPageMenuItem.class,
                CurationPageMenuItem.class,
                MonitoringPageMenuItem.class,
                AgreementPageMenuItem.class,

                // INCEpTION uses its recommenders, not the WebAnno automation code
                AutomationService.class, 
                AutomationMiraTemplateExporter.class,
                AutomationTrainingDocumentExporter.class
        })})
@EntityScan(basePackages = {
        // Include WebAnno entity packages separately so we can skip the automation entities!
        "de.tudarmstadt.ukp.clarin.webanno.model",
        "de.tudarmstadt.ukp.clarin.webanno.security",
        "de.tudarmstadt.ukp.clarin.webanno.telemetry",
        "de.tudarmstadt.ukp.inception" })
@EnableAsync
public class INCEpTION
    extends SpringBootServletInitializer
{
    private static final String PROTOCOL = "AJP/1.3";
    
    @Value("${tomcat.ajp.port:-1}")
    private int ajpPort;
    
    @Bean
    @Primary
    public Validator validator()
    {
        return new LocalValidatorFactoryBean();
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
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        if (ajpPort > 0) {
            Connector ajpConnector = new Connector(PROTOCOL);
            ajpConnector.setPort(ajpPort);
            tomcat.addAdditionalTomcatConnectors(ajpConnector);
        }
        return tomcat;
    }

    @Override
    protected SpringApplicationBuilder createSpringApplicationBuilder()
    {
        SpringApplicationBuilder builder = super.createSpringApplicationBuilder();
        builder.properties("running.from.commandline=false");
        // add this property in the case of .war deployment
        builder.properties( 
                WebSocketWicketWebInitializerAutoConfiguration.REGISTER_SERVER_ENDPOINT_ENABLED 
                + "=false" );
        init(builder);
        return builder;
    }
    
    private static void init(SpringApplicationBuilder aBuilder)
    {
        // WebAnno relies on FS IDs being stable, so we need to enable this
        System.setProperty(CASImpl.ALWAYS_HOLD_ONTO_FSS, "true");
        
        aBuilder.banner(new InceptionBanner());
        aBuilder.initializers(new InceptionApplicationContextInitializer());
        aBuilder.headless(false);
        
        SettingsUtil.customizeApplication("inception.home", ".inception");
        
        // Traditionally, the INCEpTION configuration file is called settings.properties and is
        // either located in inception.home or under the user's home directory. Make sure we pick
        // it up from there in addition to reading the built-in application.properties file.
        aBuilder.properties("spring.config.additional-location="
                + "${inception.home:${user.home}/.inception}/settings.properties");
    }
    
    public static void main(String[] args) throws Exception
    {
        Optional<JWindow> splash = LoadingSplashScreen
                .setupScreen(INCEpTION.class.getResource("splash.png"));
        
        SpringApplicationBuilder builder = new SpringApplicationBuilder();
        // Add the main application as the root Spring context
        builder.sources(INCEpTION.class).web(SERVLET);
        
        // Signal that we may need the shutdown dialog
        builder.properties("running.from.commandline=true");
        init(builder);
        
        builder.listeners(event -> {
            if (event instanceof ApplicationReadyEvent
                    || event instanceof ShutdownDialogAvailableEvent) {
                splash.ifPresent(it -> it.dispose());
            }
        });
        
        builder.run(args);
    }
}
