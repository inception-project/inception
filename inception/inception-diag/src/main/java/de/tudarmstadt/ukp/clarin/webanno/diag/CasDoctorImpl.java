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
package de.tudarmstadt.ukp.clarin.webanno.diag;

import static de.tudarmstadt.ukp.inception.support.wicket.WicketUtil.serverTiming;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.clarin.webanno.diag.config.CasDoctorProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class CasDoctorImpl
    implements CasDoctor
{
    private static final Logger LOG = LoggerFactory.getLogger(CasDoctorImpl.class);

    private final ChecksRegistry checksRegistry;
    private final RepairsRegistry repairsRegistry;

    private Set<String> activeChecks;
    private Set<String> activeRepairs;

    private boolean fatalChecks = true;
    // private boolean disableAutoScan = false;

    public CasDoctorImpl(CasDoctorProperties aProperties, ChecksRegistry aChecksRegistry,
            RepairsRegistry aRepairsRegistry)
    {
        checksRegistry = aChecksRegistry;
        repairsRegistry = aRepairsRegistry;

        fatalChecks = aProperties.isFatal();
        // disableAutoScan = aProperties.isForceReleaseBehavior();

        activeChecks = new LinkedHashSet<>();
        if (isNotEmpty(aProperties.getChecks())) {
            activeChecks.addAll(aProperties.getChecks());
        }

        activeRepairs = new LinkedHashSet<>();
        if (isNotEmpty(aProperties.getRepairs())) {
            activeRepairs.addAll(aProperties.getRepairs());
        }
    }

    public CasDoctorImpl(ChecksRegistry aChecksRegistry, RepairsRegistry aRepairsRegistry)
    {
        checksRegistry = aChecksRegistry;
        repairsRegistry = aRepairsRegistry;

        fatalChecks = false;
        // disableAutoScan = true;

        activeChecks = new LinkedHashSet<>();
        activeRepairs = new LinkedHashSet<>();
    }

    @Override
    public Set<String> getActiveChecks()
    {
        return activeChecks;
    }

    @Override
    public void setFatalChecks(boolean aFatalChecks)
    {
        fatalChecks = aFatalChecks;
    }

    @Override
    public boolean isFatalChecks()
    {
        return fatalChecks;
    }

    @Override
    public void repair(SourceDocument aDocument, String aDataOwner, CAS aCas)
    {
        var messages = new ArrayList<LogMessage>();
        repair(aDocument, aDataOwner, aCas, messages);
        if (LOG.isWarnEnabled() && !messages.isEmpty()) {
            messages.forEach(s -> LOG.warn("{}", s));
        }
    }

    @Override
    public boolean isRepairsActive()
    {
        return !activeRepairs.isEmpty();
    }

    @Override
    public void repair(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        // APPLY REPAIRS
        var tStart = currentTimeMillis();
        for (String repairId : activeRepairs) {
            try {
                var repair = repairsRegistry.getExtension(repairId).orElseThrow(
                        () -> new NoSuchElementException("Unknown repair [" + repairId + "]"));
                var tStartTask = currentTimeMillis();
                LOG.info("CasDoctor repair [" + repair.getId() + "] running...");
                repair.repair(aDocument, aDataOwner, aCas, aMessages);
                LOG.info("CasDoctor repair [" + repair.getId() + "] completed in "
                        + (currentTimeMillis() - tStartTask) + "ms");
            }
            catch (Exception e) {
                LOG.error("Cannot perform repair [" + repairId + "]", e);
                throw new IllegalStateException(
                        "Repair attempt failed - ask system administrator " + "for details.");
            }
        }

        LOG.info("CasDoctor completed all repairs in " + (currentTimeMillis() - tStart) + "ms");

        // POST-CONDITION: CAS must be consistent
        // Ensure that the repairs actually fixed the CAS
        analyze(aDocument, aDataOwner, aCas, aMessages, true);
    }

    @Override
    public boolean analyze(SourceDocument aDocument, String aDataOwner, CAS aCas)
        throws CasDoctorException
    {
        var messages = new ArrayList<LogMessage>();
        var result = analyze(aDocument, aDataOwner, aCas, messages);
        if (LOG.isDebugEnabled()) {
            messages.forEach(s -> LOG.debug("{}", s));
        }
        return result;
    }

    @Override
    public boolean analyze(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
        throws CasDoctorException
    {
        return analyze(aDocument, aDataOwner, aCas, aMessages, isFatalChecks());
    }

    @Override
    public boolean analyze(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages, boolean aFatalChecks)
        throws CasDoctorException
    {
        if (activeChecks.isEmpty()) {
            return true;
        }

        var tStart = currentTimeMillis();

        var ok = true;
        for (var checkId : activeChecks) {
            try {
                var check = checksRegistry.getExtension(checkId).orElseThrow(
                        () -> new NoSuchElementException("Unknown check [" + checkId + "]"));

                var tStartTask = currentTimeMillis();
                LOG.debug("CasDoctor analysis [" + check.getId() + "] running...");
                ok &= check.check(aDocument, aDataOwner, aCas, aMessages);
                LOG.debug("CasDoctor analysis [" + check.getId() + "] completed in "
                        + (currentTimeMillis() - tStartTask) + "ms");
            }
            catch (Exception e) {
                LOG.error("Cannot apply check [" + checkId + "]", e);
            }
        }

        if (!ok) {
            aMessages.forEach(s -> LOG.error("{}", s));
        }

        if (!ok && aFatalChecks) {
            throw new CasDoctorException(aMessages);
        }

        var duration = currentTimeMillis() - tStart;
        LOG.debug("CasDoctor completed {} checks in {}ms", activeChecks.size(), duration);
        serverTiming("CasDoctor", "CasDoctor (analyze)", duration);

        return ok;
    }

    @Override
    public void setActiveChecks(String... aActiveChecks)
    {
        activeChecks = new LinkedHashSet<>(asList(aActiveChecks));
    }

    @Override
    public void setActiveRepairs(String... aActiveRepairs)
    {
        activeRepairs = new LinkedHashSet<>(asList(aActiveRepairs));
    }

    @EventListener
    public void onApplicationStartedEvent(ApplicationStartedEvent aEvent)
    {
        // When under development, automatically enable all checks.
        // var version = SettingsUtil.getVersionProperties().getProperty(SettingsUtil.PROP_VERSION);
        // if ("unknown".equals(version) || version.contains("-SNAPSHOT")
        // || version.contains("-beta-")) {
        // if (disableAutoScan) {
        // LOG.info("Detected SNAPSHOT/beta version - but FORCING release mode and NOT "
        // + "auto-enabling checks");
        // }
        // else {
        // checksRegistry.getExtensions().forEach(check -> activeChecks.add(check.getId()));
        // LOG.info("Detected SNAPSHOT/beta version - automatically enabling all checks");
        // }
        // }

        for (var checkId : activeChecks) {
            LOG.info("Check activated: " + checkId);
        }

        for (var repairId : activeRepairs) {
            LOG.info("Repair activated: " + repairId);
        }
    }
}
