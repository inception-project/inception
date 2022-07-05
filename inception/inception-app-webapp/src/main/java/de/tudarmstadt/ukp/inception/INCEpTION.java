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

import static de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil.getApplicationHome;
import static de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil.setGlobalLogFolder;
import static de.tudarmstadt.ukp.inception.INCEpTION.INCEPTION_BASE_PACKAGE;
import static de.tudarmstadt.ukp.inception.INCEpTION.WEBANNO_BASE_PACKAGE;
import static org.apache.uima.cas.impl.CASImpl.ALWAYS_HOLD_ONTO_FSS;
import static org.springframework.boot.WebApplicationType.NONE;
import static org.springframework.boot.WebApplicationType.SERVLET;

import java.util.Optional;

import org.dkpro.core.api.resources.ResourceObjectProviderBase;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.standalone.LoadingSplashScreen;
import de.tudarmstadt.ukp.clarin.webanno.support.standalone.LoadingSplashScreen.SplashWindow;
import de.tudarmstadt.ukp.inception.app.config.InceptionApplicationContextInitializer;
import de.tudarmstadt.ukp.inception.app.config.InceptionBanner;

/**
 * Boots INCEpTION in standalone JAR or WAR modes.
 */
// @formatter:off
@SpringBootApplication(
        scanBasePackages = { INCEPTION_BASE_PACKAGE, WEBANNO_BASE_PACKAGE },
        exclude = { SolrAutoConfiguration.class, ElasticsearchRestClientAutoConfiguration.class} )
@AutoConfigurationPackage(basePackages = { INCEPTION_BASE_PACKAGE, WEBANNO_BASE_PACKAGE })
@EntityScan(basePackages = { INCEPTION_BASE_PACKAGE, WEBANNO_BASE_PACKAGE })
@EnableAsync
@EnableCaching
@EnableGlobalMethodSecurity(prePostEnabled = true)
//@formatter:on
public class INCEpTION
    extends SpringBootServletInitializer
{
    static final String INCEPTION_BASE_PACKAGE = "de.tudarmstadt.ukp.inception";
    static final String WEBANNO_BASE_PACKAGE = "de.tudarmstadt.ukp.clarin.webanno";

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
        // We rely on FS IDs being stable, so we need to enable this
        System.setProperty(ALWAYS_HOLD_ONTO_FSS, "true");

        // We do not want DKPro Core to try and auto-download anything
        System.setProperty(ResourceObjectProviderBase.PROP_REPO_OFFLINE, "true");

        aBuilder.banner(new InceptionBanner());
        aBuilder.initializers(new InceptionApplicationContextInitializer());
        aBuilder.headless(false);

        SettingsUtil.customizeApplication("inception.home", ".inception");

        // Traditionally, the INCEpTION configuration file is called settings.properties and is
        // either located in inception.home or under the user's home directory. Make sure we pick
        // it up from there in addition to reading the built-in application.yml file.
        aBuilder.properties("spring.config.additional-location="
                + "optional:${inception.home:${user.home}/.inception}/settings.properties;"
                + "optional:${inception.home:${user.home}/.inception}/settings.yml");
    }

    protected static void run(String[] args, Class<?>... aSources)
    {

        SpringApplicationBuilder builder = new SpringApplicationBuilder();

        // Add the main application as the root Spring context
        builder.sources(aSources);

        // Signal that we may need the shutdown dialog
        builder.properties("running.from.commandline=true");

        init(builder);

        Optional<SplashWindow> splash;

        // If invoking in command-mode via the command line, do not start a web server
        if (isCliCommandMode(args)) {
            builder.web(NONE);
            builder.headless(true);
            splash = Optional.empty();
        }
        else {
            builder.web(SERVLET);
            splash = LoadingSplashScreen.setupScreen("INCEpTION");
        }

        setGlobalLogFolder(getApplicationHome().toPath().resolve("log"));
        builder.listeners(event -> splash.ifPresent(_splash -> _splash.handleEvent(event)));
        var context = builder.run(args);

        if (isCliCommandMode(args)) {
            context.close();
        }
    }

    private static boolean isCliCommandMode(String[] args)
    {
        return args.length > 0;
    }

    public static void main(String[] args) throws Exception
    {
        run(args, INCEpTION.class);
    }
}
