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

import java.io.IOException;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

/**
 * Ensures that the CAS is up-to-date with the project type system. It performs the same operation
 * which is regularly performed when a user opens a document for annotation/curation.
 */
@Safe(true)
public class UpgradeCasRepair
    implements Repair
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AnnotationSchemaService annotationService;

    public UpgradeCasRepair(AnnotationSchemaService aAnnotationService)
    {
        annotationService = aAnnotationService;
    }

    @Override
    public void repair(Project aProject, CAS aCas, List<LogMessage> aMessages)
    {
        try {
            annotationService.upgradeCas(aCas, aProject);
            aMessages.add(LogMessage.info(this, "CAS upgraded."));
        }
        catch (UIMAException | IOException e) {
            log.error("Unabled to access CAS", e);
            aMessages.add(LogMessage.error(this, "Unabled to access CAS", e.getMessage()));
        }
    }
}
