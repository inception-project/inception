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
package de.tudarmstadt.ukp.clarin.webanno.diag.checks;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctorImpl;
import de.tudarmstadt.ukp.clarin.webanno.diag.ChecksRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.diag.RepairsRegistryImpl;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class AllAnnotationsIndexedCheckTest
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test
    public void testFail() throws Exception
    {
        var checksRegistry = new ChecksRegistryImpl(asList(new AllFeatureStructuresIndexedCheck()));
        checksRegistry.init();
        var repairsRegistry = new RepairsRegistryImpl(emptyList());
        repairsRegistry.init();

        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();

        var refTypeName = "RefType";

        var refTypeDesc = tsd.addType(refTypeName, null, CAS.TYPE_NAME_ANNOTATION);
        refTypeDesc.addFeature("ref", null, CAS.TYPE_NAME_ANNOTATION);

        var cas = CasCreationUtils.createCas(tsd, null, null);

        var refType = cas.getTypeSystem().getType(refTypeName);

        // A regular index annotation
        var anno1 = cas.createAnnotation(cas.getAnnotationType(), 0, 1);
        cas.addFsToIndexes(anno1);

        // A non-index annotation but reachable through an indexed one (below)
        var anno2 = cas.createAnnotation(cas.getAnnotationType(), 0, 1);

        // An indexed annotation that references the non-indexed annotation above
        var anno3 = cas.createAnnotation(refType, 0, 1);
        anno3.setFeatureValue(refType.getFeatureByBaseName("ref"), anno2);
        cas.addFsToIndexes(anno3);

        var messages = new ArrayList<LogMessage>();
        var cd = new CasDoctorImpl(checksRegistry, repairsRegistry);
        cd.setActiveChecks(
                checksRegistry.getExtensions().stream().map(c -> c.getId()).toArray(String[]::new));
        cd.setActiveRepairs(repairsRegistry.getExtensions().stream().map(c -> c.getId())
                .toArray(String[]::new));

        // A project is not required for this check
        var result = cd.analyze(null, null, cas, messages);

        messages.forEach($ -> LOG.debug("{}", $));

        assertThat(result).isFalse();
    }

    @Test
    public void testOK() throws Exception
    {
        var checksRegistry = new ChecksRegistryImpl(asList(new AllFeatureStructuresIndexedCheck()));
        checksRegistry.init();
        var repairsRegistry = new RepairsRegistryImpl(emptyList());
        repairsRegistry.init();

        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();

        var refTypeName = "RefType";

        var refTypeDesc = tsd.addType(refTypeName, null, CAS.TYPE_NAME_ANNOTATION);
        refTypeDesc.addFeature("ref", null, CAS.TYPE_NAME_ANNOTATION);

        var cas = CasCreationUtils.createCas(tsd, null, null);

        var refType = cas.getTypeSystem().getType(refTypeName);

        // A regular index annotation
        var anno1 = cas.createAnnotation(cas.getAnnotationType(), 0, 1);
        cas.addFsToIndexes(anno1);

        // An indexed annotation but reachable through an indexed one (below)
        var anno2 = cas.createAnnotation(cas.getAnnotationType(), 0, 1);
        cas.addFsToIndexes(anno2);

        // An indexed annotation that references the non-indexed annotation above
        var anno3 = cas.createAnnotation(refType, 0, 1);
        anno3.setFeatureValue(refType.getFeatureByBaseName("ref"), anno2);
        cas.addFsToIndexes(anno3);

        var messages = new ArrayList<LogMessage>();
        var cd = new CasDoctorImpl(checksRegistry, repairsRegistry);
        cd.setActiveChecks(
                checksRegistry.getExtensions().stream().map(c -> c.getId()).toArray(String[]::new));
        cd.setActiveRepairs(repairsRegistry.getExtensions().stream().map(c -> c.getId())
                .toArray(String[]::new));

        // A project is not required for this check
        var result = cd.analyze(null, null, cas, messages);

        messages.forEach($ -> LOG.debug("{}", $));

        assertThat(result).isTrue();
    }
}
