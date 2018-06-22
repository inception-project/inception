/*
 * Copyright 2018
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

package de.tudarmstadt.ukp.inception.search.index.mtas;

import java.util.Optional;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.graph.RdfUtils;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class KBUtility {
    private final static Logger log = LoggerFactory.getLogger(KBUtility.class.getName());

    /**
     * Read identifier IRI and return {@link KBHandle}
     * @param featureSupportRegistry
     * @param feature
     * @param featureValue 
     * @return {@link KBHandle} which can be used to read the IRI.
     */
    public static KBHandle readKBIdentifier(FeatureSupportRegistry featureSupportRegistry,
            AnnotationFeature feature, String featureValue)
    {
        FeatureSupport<ConceptFeatureTraits> fs = featureSupportRegistry.getFeatureSupport(feature);
        KBHandle handle = (KBHandle) fs.wrapFeatureValue(feature, null, featureValue);
        return handle;
    }

    /**
     * Read identifier IRI and return {@link Optional} of {@link KBObject}
     * @param aProject 
     * @param aIdentifier
     * @return {@link Optional} of {@link KBObject} of type {@link KBConcept} or {@link KBInstance}
     */
    public static Optional<KBObject> readKBIdentifier(KnowledgeBaseService kbService,
            Project aProject, String aIdentifier)
    {
        for (KnowledgeBase kb : kbService.getKnowledgeBases(aProject)) {
            try (RepositoryConnection conn = kbService.getConnection(kb)) {
                ValueFactory vf = conn.getValueFactory();
                RepositoryResult<Statement> stmts = RdfUtils.getStatements(conn,
                        vf.createIRI(aIdentifier), kb.getTypeIri(), kb.getClassIri(), true);
                if (stmts.hasNext()) {
                    KBConcept kbConcept = KBConcept.read(conn, vf.createIRI(aIdentifier), kb);
                    if (kbConcept != null) {
                        return Optional.of(kbConcept);
                    }
                }
                else if (!stmts.hasNext()) {
                    Optional<KBInstance> kbInstance = kbService.readInstance(kb, aIdentifier);
                    if (kbInstance.isPresent()) {
                        return kbInstance.flatMap((p) -> Optional.of(p));
                    }
                }
            }
            catch (QueryEvaluationException e) {
                log.error("Reading KB Entries failed.", e);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
