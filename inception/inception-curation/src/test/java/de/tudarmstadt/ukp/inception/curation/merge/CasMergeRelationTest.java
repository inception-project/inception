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

import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS._FeatName_PosValue;
import static de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token._FeatName_pos;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.curation.RelationDiffAdapterImpl.DEPENDENCY_DIFF_ADAPTER;
import static de.tudarmstadt.ukp.inception.curation.merge.CasMergeOperationResult.ResultState.CREATED;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static org.apache.uima.fit.factory.CasFactory.createCas;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;

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

    private AnnotationFS createFeaturelessRelation(CAS aCas, AnnotationFS aSrcToken,
            AnnotationFS aTgtToken)
    {
        // Create custom FeaturelessRelation annotation
        var type = aCas.getTypeSystem().getType(featurelessRelLayer.getName());
        var fs = aCas.createAnnotation(type, aTgtToken.getBegin(), aTgtToken.getEnd());
        fs.setFeatureValue(type.getFeatureByBaseName("Dependent"), aSrcToken);
        fs.setFeatureValue(type.getFeatureByBaseName("Governor"), aTgtToken);
        aCas.addFsToIndexes(fs);
        return fs;
    }

    private AnnotationFS createFeaturelessRelationWithTokens(CAS aCas, int aSrcBegin, int aSrcEnd,
            int aTgtBegin, int aTgtEnd)
    {
        return createFeaturelessRelation(aCas, //
                createTokenAndOptionalPos(aCas, aSrcBegin, aSrcEnd, null), //
                createTokenAndOptionalPos(aCas, aTgtBegin, aTgtEnd, null));
    }

    @Test
    void thatFeaturelessRelationCanBeMerged() throws Exception
    {
        // Set up source with featureless relation
        var clickedFs = createFeaturelessRelationWithTokens(sourceCas, 0, 0, 1, 1);

        // Set up target with tokens only
        createTokenAndOptionalPos(targetCas, 0, 0, null);
        createTokenAndOptionalPos(targetCas, 1, 1, null);

        var result = sut.mergeRelationAnnotation(document, DUMMY_USER, featurelessRelLayer,
                targetCas, clickedFs);

        var type = targetCas.getTypeSystem().getType(featurelessRelLayer.getName());
        assertThat(result.state()).isEqualTo(CREATED);
        assertThat(targetCas.select(type).asList()) //
                .as("Featureless relation was merged") //
                .hasSize(1);
    }

    @Test
    void thatMultipleFeaturelessRelationsCanBeMerged() throws Exception
    {
        // Set up source with second featureless relation
        var clickedFs = createFeaturelessRelationWithTokens(sourceCas, 2, 2, 0, 0);

        // Set up target with existing featureless relation and tokens
        createFeaturelessRelationWithTokens(targetCas, 1, 1, 0, 0);
        createTokenAndOptionalPos(targetCas, 2, 2, null);

        var result = sut.mergeRelationAnnotation(document, DUMMY_USER, featurelessRelLayer,
                targetCas, clickedFs);

        var type = targetCas.getTypeSystem().getType(featurelessRelLayer.getName());
        assertThat(result.state()).isEqualTo(CREATED);
        assertThat(targetCas.select(type).asList()) //
                .extracting( //
                        dep -> getFeature(dep, "Dependent", AnnotationFS.class).getBegin(), //
                        dep -> getFeature(dep, "Dependent", AnnotationFS.class).getEnd(), //
                        dep -> getFeature(dep, "Governor", AnnotationFS.class).getBegin(), //
                        dep -> getFeature(dep, "Governor", AnnotationFS.class).getEnd())
                .containsExactly( //
                        tuple(1, 1, 0, 0), //
                        tuple(2, 2, 0, 0));
    }

    @Test
    void thatMergingFeaturelessRelationIsRejectedIfAlreadyExists() throws Exception
    {
        // Set up source with featureless relation
        var clickedFs = createFeaturelessRelationWithTokens(sourceCas, 0, 0, 1, 1);

        // Set up target with identical featureless relation
        createFeaturelessRelationWithTokens(targetCas, 0, 0, 1, 1);

        assertThatExceptionOfType(AnnotationException.class) //
                .as("Reject merging featureless relation which already exists")
                .isThrownBy(() -> sut.mergeRelationAnnotation(document, DUMMY_USER,
                        featurelessRelLayer, targetCas, clickedFs))
                .withMessageContaining("annotation already exists");
    }

    @Test
    void thatFeaturelessRelationCanAutoMerge() throws Exception
    {
        // Override schema service to return featureless layer for our custom type
        when(schemaService.findLayer(project, featurelessRelLayer.getName()))
                .thenReturn(featurelessRelLayer);

        // Remove default dependency adapter and add featureless adapter for this test
        diffAdapters.remove(DEPENDENCY_DIFF_ADAPTER);
        diffAdapters.add(FEATURELESS_REL_DIFF_ADAPTER);

        try {
            var casByUser = new LinkedHashMap<String, CAS>();

            var source1 = createCas();
            source1.setDocumentText("w0 w1 w2");
            var srcToken1 = createTokenAndOptionalPos(source1, 0, 1, null);
            var tgtToken1 = createTokenAndOptionalPos(source1, 3, 4, null);
            createFeaturelessRelation(source1, srcToken1, tgtToken1);
            casByUser.put("user1", source1);

            var source2 = createCas();
            source2.setDocumentText("w0 w1 w2");
            var srcToken2 = createTokenAndOptionalPos(source2, 0, 1, null);
            var tgtToken2 = createTokenAndOptionalPos(source2, 3, 4, null);
            createFeaturelessRelation(source2, srcToken2, tgtToken2);
            casByUser.put("user2", source2);

            targetCas.setDocumentText("w0 w1 w2");
            // Create tokens in target CAS so relation endpoints can be found
            createTokenAndOptionalPos(targetCas, 0, 1, null);
            createTokenAndOptionalPos(targetCas, 3, 4, null);

            var result = doDiff(diffAdapters, casByUser).toResult();
            sut.clearAndMergeCas(result, document, "curator", targetCas, casByUser);

            var type = source1.getTypeSystem().getType(featurelessRelLayer.getName());

            // Verify the featureless relation was auto-merged
            assertThat(targetCas.select(type).asList())
                    .as("Featureless relation should be auto-merged from agreeing annotators")
                    .hasSize(1);
        }
        finally {
            // Restore defaults
            diffAdapters.remove(FEATURELESS_REL_DIFF_ADAPTER);
            diffAdapters.add(DEPENDENCY_DIFF_ADAPTER);
        }
    }
}
