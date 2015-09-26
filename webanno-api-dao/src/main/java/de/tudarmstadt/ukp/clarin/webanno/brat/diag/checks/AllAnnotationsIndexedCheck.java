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
package de.tudarmstadt.ukp.clarin.webanno.brat.diag.checks;

import static de.tudarmstadt.ukp.clarin.webanno.brat.diag.CasDoctorUtils.getNonIndexedFSes;

import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;

import de.tudarmstadt.ukp.clarin.webanno.brat.diag.CasDoctor.LogLevel;
import de.tudarmstadt.ukp.clarin.webanno.brat.diag.CasDoctor.LogMessage;

public class AllAnnotationsIndexedCheck
    implements Check
{
    @Override
    public boolean check(CAS aCas, List<LogMessage> aMessages)
    {
        Set<FeatureStructure> nonIndexed = getNonIndexedFSes(aCas);

        if (!nonIndexed.isEmpty()) {
            aMessages.add(new LogMessage(this, LogLevel.ERROR, "Unindexed annotations: %d",
                    nonIndexed.size()));

            for (FeatureStructure fs : nonIndexed) {
                aMessages.add(new LogMessage(this, LogLevel.ERROR, "%s", fs));
            }
        }
        // else {
        // aMessages.add(String.format("[%s] OK", getClass().getSimpleName()));
        // }

        return nonIndexed.isEmpty();
    }
}
