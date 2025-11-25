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
package de.tudarmstadt.ukp.inception.curation.merge;

import static de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS._FeatName_PosValue;
import static de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token._FeatName_pos;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.curation.merge.CasMergeOperationResult.ResultState.CREATED;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static org.apache.uima.fit.factory.CasFactory.createCas;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;

@Execution(CONCURRENT)
public class CasMergeRelationTest
    extends CasMergeTestBase
{
    private static final String DUMMY_USER = "dummyTargetUser";

    private CAS sourceCas;
    private CAS targetCas;

    @Override
    @BeforeEach
    public void setup() throws Exception
    {
        super.setup();
        sourceCas = createCas();
        targetCas = createCas();
    }

    @Test
    void simpleCopyRelationToEmptyAnnoTest() throws Exception
    {
        // Set up source
        var clickedFs = createDependencyWithTokenAndPos(sourceCas, 0, 0, "NN", 1, 1, "NN");

        // Set up target
        createTokenAndOptionalPos(targetCas, 0, 0, "NN");
        createTokenAndOptionalPos(targetCas, 1, 1, "NN");

        sut.mergeRelationAnnotation(document, DUMMY_USER, depLayer, targetCas, clickedFs);

        assertThat(targetCas.select(Dependency.class).coveredBy(0, 1).asList())
                .as("Relation was merged") //
                .hasSize(1);
    }

    @Test
    public void simpleCopyRelationToStackedTargetsTest() throws Exception
    {
        // Set up source
        // Create a dependency relation with endpoints in the annotator CAS
        var clickedFs = createDependencyWithTokenAndPos(sourceCas, 0, 0, "NN", 1, 1, "NN");

        // Set up target
        // Create stacked endpoint candidates in the merge CAS
        createTokenAndOptionalPos(targetCas, 0, 0, "NN");
        createTokenAndOptionalPos(targetCas, 0, 0, "NN");
        createTokenAndOptionalPos(targetCas, 1, 1, "NN");
        createTokenAndOptionalPos(targetCas, 1, 1, "NN");

        assertThatExceptionOfType(AnnotationException.class) //
                .as("Cannot merge when there are multiple/stacked candidates") //
                .isThrownBy(() -> sut.mergeRelationAnnotation(document, DUMMY_USER, depLayer,
                        targetCas, clickedFs))
                .withMessageContaining("multiple possible");
    }

    @Test
    public void thatMergingRelationIsRejectedIfAlreadyExists() throws Exception
    {
        // Set up source
        var clickedFs = createDependencyWithTokenAndPos(sourceCas, 0, 0, "NN", 1, 1, "NN");

        // Set up target
        createDependencyWithTokenAndPos(targetCas, 0, 0, "NN", 1, 1, "NN");

        assertThatExceptionOfType(AnnotationException.class) //
                .as("Reject merging relation which already exists")
                .isThrownBy(() -> sut.mergeRelationAnnotation(document, DUMMY_USER, depLayer,
                        targetCas, clickedFs))
                .withMessageContaining("annotation already exists");
    }

    @Test
    public void thatSecondRelationCanBeMergedWithSameTarget() throws Exception
    {
        // Set up source
        var clickedFs = createDependencyWithTokenAndPos(sourceCas, 2, 2, "NN", 0, 0, "NN");

        // Set up target
        createDependencyWithTokenAndPos(targetCas, 1, 1, "NN", 0, 0, "NN");
        createTokenAndOptionalPos(targetCas, 2, 2, "NN");

        var result = sut.mergeRelationAnnotation(document, DUMMY_USER, depLayer, targetCas,
                clickedFs);

        assertThat(result.state()).isEqualTo(CREATED);
        assertThat(targetCas.select(Dependency.class).asList()) //
                .extracting( //
                        dep -> getFeature(dep, FEAT_REL_SOURCE, AnnotationFS.class).getBegin(), //
                        dep -> getFeature(dep, FEAT_REL_SOURCE, AnnotationFS.class).getEnd(), //
                        dep -> getFeature(dep, FEAT_REL_TARGET, AnnotationFS.class).getBegin(), //
                        dep -> getFeature(dep, FEAT_REL_TARGET, AnnotationFS.class).getEnd())
                .containsExactly( //
                        tuple(1, 1, 0, 0), //
                        tuple(2, 2, 0, 0));
    }

    @Test
    public void thatSecondRelationCanBeMergedWithSameSource() throws Exception
    {
        // Set up source
        var clickedFs = createDependencyWithTokenAndPos(sourceCas, 0, 0, "NN", 2, 2, "NN");

        // Set up target
        createDependencyWithTokenAndPos(targetCas, 0, 0, "NN", 1, 1, "NN");
        createTokenAndOptionalPos(targetCas, 2, 2, "NN");

        var result = sut.mergeRelationAnnotation(document, DUMMY_USER, depLayer, targetCas,
                clickedFs);

        assertThat(result.state()).isEqualTo(CREATED);
        assertThat(targetCas.select(Dependency.class).asList()) //
                .extracting( //
                        dep -> getFeature(dep, FEAT_REL_SOURCE, AnnotationFS.class).getBegin(), //
                        dep -> getFeature(dep, FEAT_REL_SOURCE, AnnotationFS.class).getEnd(), //
                        dep -> getFeature(dep, FEAT_REL_TARGET, AnnotationFS.class).getBegin(), //
                        dep -> getFeature(dep, FEAT_REL_TARGET, AnnotationFS.class).getEnd())
                .containsExactly( //
                        tuple(0, 0, 1, 1), //
                        tuple(0, 0, 2, 2));
    }

    private AnnotationFS createTokenAndOptionalPos(CAS aCas, int aBegin, int aEnd, String aPos)
    {
        AnnotationFS pos = null;

        if (aPos != null) {
            pos = buildAnnotation(aCas, POS.class) //
                    .at(aBegin, aEnd) //
                    .withFeature(_FeatName_PosValue, aPos) //
                    .buildAndAddToIndexes();
        }

        return buildAnnotation(aCas, Token.class) //
                .at(aBegin, aEnd) //
                .withFeature(_FeatName_pos, pos) //
                .buildAndAddToIndexes();
    }

    private AnnotationFS createDependency(CAS aCas, AnnotationFS aSrcToken, AnnotationFS aTgtToken)
    {
        return buildAnnotation(aCas, Dependency.class) //
                .at(aTgtToken.getBegin(), aTgtToken.getEnd()) //
                .withFeature(FEAT_REL_SOURCE, aSrcToken) //
                .withFeature(FEAT_REL_TARGET, aTgtToken) //
                .buildAndAddToIndexes();
    }

    private AnnotationFS createDependencyWithTokenAndPos(CAS aCas, int aSrcBegin, int aSrcEnd,
            String aSrcPos, int aTgtBegin, int aTgtEnd, String aTgtPos)
    {
        return createDependency(aCas, //
                createTokenAndOptionalPos(aCas, aSrcBegin, aSrcEnd, aSrcPos), //
                createTokenAndOptionalPos(aCas, aTgtBegin, aTgtEnd, aTgtPos));
    }
}
