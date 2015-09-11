/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.diag;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import de.tudarmstadt.ukp.clarin.webanno.brat.diag.checks.Check;

public class CasDoctor
    implements InitializingBean
{
    private Log log = LogFactory.getLog(getClass());
    
    @Value(value = "${debug.casDoctor.checks}")
    private String activeChecks;

    @Value(value = "${debug.casDoctor.fatal}")
    private boolean fatalChecks = true;

    private List<Class<? extends Check>> checkClasses = new ArrayList<>();
    
    public CasDoctor()
    {
        // Bean operation
    }

    public CasDoctor(Class<? extends Check>... aChecks)
    {
        // For testing
        StringBuilder sb = new StringBuilder();
        for (Class<? extends Check> clazz : aChecks) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(clazz.getSimpleName());
        }
        activeChecks = sb.toString();
        afterPropertiesSet();
    }

    public boolean analyze(CAS aCas)
    {
        List<String> messages = new ArrayList<String>();
        boolean result = analyze(aCas, messages);
        if (log.isDebugEnabled()) {
            messages.forEach(s -> log.debug(s));
        }
        return result;
    }

    public boolean analyze(CAS aCas, List<String> aMessages)
    {
        boolean ok = true;
        for (Class<? extends Check> checkClass : checkClasses) {
            try {
                Check check = checkClass.newInstance();
                ok &= check.check(aCas,  aMessages);
            }
            catch (InstantiationException | IllegalAccessException e) {
                aMessages.add(String.format("[%s] Cannot instantiate: %s",
                        checkClass.getSimpleName(), ExceptionUtils.getRootCauseMessage(e)));
                log.error(e);
            }
        }
        
        if (!ok && fatalChecks) {
            aMessages.forEach(s -> log.info(s));
            throw new IllegalStateException("CasDoctor has detected problems and checks are fatal.");
        }
        
        return ok;
    }
    
    public void setActiveChecks(String aActiveChecks)
    {
        activeChecks = aActiveChecks;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void afterPropertiesSet()
    {
        if (StringUtils.isNotBlank(activeChecks)) {
            for (String check : activeChecks.split(",")) {
                try {
                    checkClasses.add((Class<? extends Check>) Class.forName(Check.class.getPackage()
                            .getName() + "." + check.trim()));
                }
                catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }
}
