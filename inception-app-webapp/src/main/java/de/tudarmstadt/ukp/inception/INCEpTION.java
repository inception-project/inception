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

import java.util.Optional;

import javax.swing.JWindow;
import javax.validation.Validator;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import de.tudarmstadt.ukp.clarin.webanno.automation.service.AutomationService;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.export.AutomationMiraTemplateExporter;
import de.tudarmstadt.ukp.clarin.webanno.automation.service.export.AutomationTrainingDocumentExporter;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.standalone.LoadingSplashScreen;
import de.tudarmstadt.ukp.clarin.webanno.support.standalone.ShutdownDialogAvailableEvent;
import de.tudarmstadt.ukp.inception.app.config.InceptionApplicationContextInitializer;
import de.tudarmstadt.ukp.inception.app.config.InceptionBanner;

/**
 * Boots INCEpTION in standalone JAR or WAR modes.
 */
@SpringBootApplication
@ComponentScan(excludeFilters = {
        @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { 
                AutomationService.class, AutomationMiraTemplateExporter.class,
                AutomationTrainingDocumentExporter.class
        })})
@EntityScan(basePackages = {
        // Include WebAnno entity packages separately so we can skip the automation entities!
        "de.tudarmstadt.ukp.clarin.webanno.model",
        "de.tudarmstadt.ukp.clarin.webanno.security",
        "de.tudarmstadt.ukp.inception" })
@ImportResource({ 
        "classpath:/META-INF/application-context.xml",
        "classpath:/META-INF/rest-context.xml", 
        "classpath:/META-INF/static-resources-context.xml" })
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
    public EmbeddedServletContainerFactory servletContainer()
    {
        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
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
        init(builder);
        return builder;
    }
    
    private static void init(SpringApplicationBuilder aBuilder)
    {
        aBuilder.banner(new InceptionBanner());
        aBuilder.initializers(new InceptionApplicationContextInitializer());
        aBuilder.headless(false);
        
        SettingsUtil.customizeApplication("inception.home", ".inception");
        
        // Traditionally, the INCEpTION configuration file is called settings.properties and is
        // either located in inception.home or under the user's home directory. Make sure we pick
        // it up from there in addition to reading the built-in application.properties file.
        aBuilder.properties("spring.config.location="
                + "${inception.home:${user.home}/.inception}/settings.properties");
    }
    
    public static void main(String[] args) throws Exception
    {
        Optional<JWindow> splash = LoadingSplashScreen
                .setupScreen(INCEpTION.class.getResource("splash.png"));
        
        SpringApplicationBuilder builder = new SpringApplicationBuilder();
        // Signal that we may need the shutdown dialog
        builder.properties("running.from.commandline=true");
        init(builder);
        builder.sources(INCEpTION.class);
        builder.listeners(event -> {
            if (event instanceof ApplicationReadyEvent
                    || event instanceof ShutdownDialogAvailableEvent) {
                splash.ifPresent(it -> it.dispose());
            }
        });
        builder.run(args);
    }
}
