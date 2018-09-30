/*
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.diag;

import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.uima.cas.CAS;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.diag.checks.Check;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil;

@Component("casDoctor")
public class CasDoctor
    implements InitializingBean, ApplicationContextAware
{
    private Logger log = LoggerFactory.getLogger(getClass());

    @Value(value = "${debug.casDoctor.checks}")
    private String activeChecks;

    @Value(value = "${debug.casDoctor.fatal}")
    private boolean fatalChecks = true;

    @Value(value = "${debug.casDoctor.repairs}")
    private String activeRepairs;

    private ApplicationContext context;
    
    private List<Class<? extends Check>> checkClasses = new ArrayList<>();
    private List<Class<? extends Repair>> repairClasses = new ArrayList<>();

    @Value(value = "${debug.casDoctor.forceReleaseBehavior}")
    private boolean disableAutoScan = false;

    public CasDoctor()
    {
        // Bean operation
    }

    /**
     * This constructor must only be used for unit tests.
     */
    public CasDoctor(Class<?>... aChecksRepairs)
    {
        StringBuilder checks = new StringBuilder();
        StringBuilder repairs = new StringBuilder();
        for (Class<?> clazz : aChecksRepairs) {
            boolean isCheck = Check.class.isAssignableFrom(clazz);
            boolean isRepair = Repair.class.isAssignableFrom(clazz);
            
            if (isCheck) {
                if (checks.length() > 0) {
                    checks.append(',');
                }
                checks.append(clazz.getSimpleName());
            }
            
            if (isRepair) {
                if (repairs.length() > 0) {
                    repairs.append(',');
                }
                repairs.append(clazz.getSimpleName());
            }
            
            if (!isCheck && !isRepair) {
                throw new IllegalArgumentException("[" + clazz.getName()
                        + "] is neither a check nor a repair");
            }
        }
        activeChecks = checks.toString();
        fatalChecks = false;
        
        activeRepairs = repairs.toString();
        
        // This constructor is only used for tests. In tests we want to control which checks are
        // used and do not want to auto-scan.
        disableAutoScan = true;
        
        afterPropertiesSet();
    }

    public void setFatalChecks(boolean aFatalChecks)
    {
        fatalChecks = aFatalChecks;
    }

    public boolean isFatalChecks()
    {
        return fatalChecks;
    }

    public void repair(Project aProject, CAS aCas)
    {
        List<LogMessage> messages = new ArrayList<>();
        repair(aProject, aCas, messages);
        if (log.isWarnEnabled() && !messages.isEmpty()) {
            messages.forEach(s -> log.warn("{}", s));
        }
    }
    
    public boolean isRepairsActive()
    {
        return !repairClasses.isEmpty();
    }
    
    public void repair(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        // If there are no active repairs, don't do anything
        if (repairClasses.isEmpty()) {
            return;
        }
        
        // APPLY REPAIRS
        long tStart = System.currentTimeMillis();
        for (Class<? extends Repair> repairClass : repairClasses) {
            try {
                long tStartTask = System.currentTimeMillis();
                Repair repair = repairClass.newInstance();
                if (context != null) {
                    context.getAutowireCapableBeanFactory().autowireBean(repair);
                }
                log.info("CasDoctor repair [" + repairClass.getSimpleName() + "] running...");
                repair.repair(aProject, aCas, aMessages);
                log.info("CasDoctor repair [" + repairClass.getSimpleName() + "] completed in "
                        + (System.currentTimeMillis() - tStartTask) + "ms");
            }
            catch (Exception e) {
//                aMessages.add(new LogMessage(this, LogLevel.ERROR, "Cannot perform repair [%s]: %s",
//                        repairClass.getSimpleName(), ExceptionUtils.getRootCauseMessage(e)));
                log.error("Cannot perform repair [" + repairClass.getSimpleName() + "]", e);
                throw new IllegalStateException("Repair attempt failed - ask system administrator "
                        + "for details.");
            }
        }
        
        log.info("CasDoctor completed all repairs in " + (System.currentTimeMillis() - tStart) + "ms");
        
        // POST-CONDITION: CAS must be consistent
        // Ensure that the repairs actually fixed the CAS
        analyze(aProject, aCas, aMessages, true);
    }
    
    public boolean analyze(Project aProject, CAS aCas)
        throws CasDoctorException
    {
        List<LogMessage> messages = new ArrayList<>();
        boolean result = analyze(aProject, aCas, messages);
        if (log.isDebugEnabled()) {
            messages.forEach(s -> log.debug("{}", s));
        }
        return result;
    }

    public boolean analyze(Project aProject, CAS aCas, List<LogMessage> aMessages)
        throws CasDoctorException
    {
        return analyze(aProject, aCas, aMessages, isFatalChecks());
    }

    public boolean analyze(Project aProject, CAS aCas, List<LogMessage> aMessages,
            boolean aFatalChecks)
        throws CasDoctorException
    {
        long tStart = System.currentTimeMillis();
        
        boolean ok = true;
        for (Class<? extends Check> checkClass : checkClasses) {
            try {
                long tStartTask = System.currentTimeMillis();
                Check check = checkClass.newInstance();
                if (context != null) {
                    context.getAutowireCapableBeanFactory().autowireBean(check);
                }
                log.debug("CasDoctor analysis [" + checkClass.getSimpleName() + "] running...");
                ok &= check.check(aProject, aCas, aMessages);
                log.debug("CasDoctor analysis [" + checkClass.getSimpleName() + "] completed in "
                        + (System.currentTimeMillis() - tStartTask) + "ms");
            }
            catch (InstantiationException | IllegalAccessException e) {
                aMessages.add(new LogMessage(this, LogLevel.ERROR, "Cannot instantiate [%s]: %s",
                        checkClass.getSimpleName(), ExceptionUtils.getRootCauseMessage(e)));
                log.error("Error running check", e);
            }
        }

        if (!ok) {
            aMessages.forEach(s -> log.error("{}", s));
        }
        
        if (!ok && aFatalChecks) {
            throw new CasDoctorException(aMessages);
        }

        log.debug("CasDoctor completed all analyses in " + (System.currentTimeMillis() - tStart) + "ms");

        return ok;
    }

    public void setCheckClasses(List<Class<? extends Check>> aCheckClasses)
    {
        checkClasses = aCheckClasses;
    }
    
    public void setRepairClasses(List<Class<? extends Repair>> aRepairClasses)
    {
        repairClasses = aRepairClasses;
    }
    
    public void setActiveChecks(String aActiveChecks)
    {
        activeChecks = aActiveChecks;
    }

    public void setActiveRepairs(String aActiveRepairs)
    {
        activeRepairs = aActiveRepairs;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void afterPropertiesSet()
    {
        // If WebAnno is in under development, automatically enable all checks.
        String version = SettingsUtil.getVersionProperties().getProperty(SettingsUtil.PROP_VERSION);
        if (
                "unknown".equals(version) || 
                version.contains("-SNAPSHOT") || 
                version.contains("-beta-")
        ) {
            if (disableAutoScan) {
                log.info("Detected SNAPSHOT/beta version - but FORCING release mode and NOT "
                        + "auto-enabling checks");
            }
            else {
                checkClasses.addAll(scanChecks());
                log.info("Detected SNAPSHOT/beta version - automatically enabling all checks");
            }
        }
        
        if (StringUtils.isNotBlank(activeChecks)) {
            for (String check : activeChecks.split(",")) {
                try {
                    Class<? extends Check> checkClass = (Class<? extends Check>) Class
                            .forName(Check.class.getPackage().getName() + "." + check.trim());
                    if (!checkClasses.contains(checkClass)) {
                        checkClasses.add(checkClass);
                    }
                }
                catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        
        for (Class<? extends Check> c : checkClasses) {
            log.info("Check activated: " + c.getSimpleName());
        }
        
        if (StringUtils.isNotBlank(activeRepairs)) {
            for (String check : activeRepairs.split(",")) {
                try {
                    repairClasses.add((Class<? extends Repair>) Class.forName(Repair.class
                            .getPackage().getName() + "." + check.trim()));
                }
                catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        
        for (Class<? extends Repair> c : repairClasses) {
            log.info("Repair activated: " + c.getSimpleName());
        }
    }
    
    public static List<Class<? extends Check>> scanChecks()
    {
        Reflections reflections = new Reflections(Check.class.getPackage().getName());
        return reflections.getSubTypesOf(Check.class).stream()
                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                .sorted(Comparator.comparing(Class::getName))
                .collect(Collectors.toList());
    }

    public static List<Class<? extends Repair>> scanRepairs()
    {
        Reflections reflections = new Reflections(Repair.class.getPackage().getName());
        return reflections.getSubTypesOf(Repair.class).stream()
                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                .sorted(Comparator.comparing(Class::getName))
                .collect(Collectors.toList());
    }

    public enum LogLevel
    {
        INFO, WARN, ERROR
    }

    public static class LogMessage
        implements Serializable
    {
        private static final long serialVersionUID = 2002139781814027105L;
        
        public final LogLevel level;
        public final Class<?> source;
        public final String message;

        public LogMessage(Object aSource, LogLevel aLevel, String aMessage)
        {
            this(aSource, aLevel, "%s", aMessage);
        }

        public LogMessage(Object aSource, LogLevel aLevel, String aFormat, Object... aValues)
        {
            super();
            if (aSource instanceof Class) {
                source = (Class) aSource;
            }
            else {
                source = aSource != null ? aSource.getClass() : null;
            }
            level = aLevel;
            message = String.format(aFormat, aValues);
        }
        
        @Override
        public String toString()
        {
            return String.format("[%s] %s", source != null ? source.getSimpleName() : "<unknown>",
                    message);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext aContext)
        throws BeansException
    {
        context = aContext;
    }
}
