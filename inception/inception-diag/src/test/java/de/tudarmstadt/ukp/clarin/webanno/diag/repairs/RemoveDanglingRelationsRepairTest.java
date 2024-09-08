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
package de.tudarmstadt.ukp.clarin.webanno.diag.repairs;

import static de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationLayerSupport.FEAT_REL_TARGET;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor;
import de.tudarmstadt.ukp.clarin.webanno.diag.ChecksRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.diag.RepairsRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.diag.checks.AllFeatureStructuresIndexedCheck;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationAdapter;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

@ExtendWith(MockitoExtension.class)
public class RemoveDanglingRelationsRepairTest
{
    private @Mock ConstraintsService constraintsService;
    private @Mock AnnotationSchemaService schemaService;

    @Test
    public void test() throws Exception
    {
        when(schemaService.findAdapter(any(), any()))
                .thenReturn(new RelationAdapter(null, null, null, null, FEAT_REL_SOURCE,
                        FEAT_REL_TARGET, () -> asList(), asList(), constraintsService));

        var checksRegistry = new ChecksRegistryImpl(asList(new AllFeatureStructuresIndexedCheck()));
        checksRegistry.init();
        var repairsRegistry = new RepairsRegistryImpl(
                asList(new RemoveDanglingRelationsRepair(schemaService)));
        repairsRegistry.init();

        var jcas = JCasFactory.createJCas();

        jcas.setDocumentText("This is a test.");

        var span1 = new Token(jcas, 0, 4);
        span1.addToIndexes();

        var span2 = new Token(jcas, 6, 8);

        var dep = new Dependency(jcas, 0, 8);
        dep.setGovernor(span1);
        dep.setDependent(span2);
        dep.addToIndexes();

        var messages = new ArrayList<LogMessage>();
        var cd = new CasDoctor(checksRegistry, repairsRegistry);
        cd.setActiveChecks(
                checksRegistry.getExtensions().stream().map(c -> c.getId()).toArray(String[]::new));
        cd.setActiveRepairs(repairsRegistry.getExtensions().stream().map(c -> c.getId())
                .toArray(String[]::new));

        // A project is not required for this check
        var result = cd.analyze(null, jcas.getCas(), messages);

        // A project is not required for this repair
        cd.repair(null, jcas.getCas(), messages);

        assertFalse(result);

        messages.forEach(System.out::println);
    }
}
