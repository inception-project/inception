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
package de.tudarmstadt.ukp.inception.diam.model.compactv2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VAnnotationMarker;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VArc;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VComment;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VCommentType;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VMarker;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VTextMarker;
import de.tudarmstadt.ukp.inception.schema.api.config.AnnotationSchemaProperties;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@ExtendWith(MockitoExtension.class)
public class CompactSerializerV2ImplTest
{
    private @Mock AnnotationSchemaProperties annotationSchemaProperties;

    @Test
    void thatSerializationWorks() throws Exception
    {
        var spanLayer = AnnotationLayer.builder() //
                .withId(1l) //
                .withUiName("Span") //
                .withName("custom.Span") //
                .withType(SpanLayerSupport.TYPE) //
                .build();
        var relationLayer = AnnotationLayer.builder() //
                .withId(2l) //
                .withUiName("Relation") //
                .withName("custom.Relation") //
                .withType(WebAnnoConst.RELATION_TYPE) //
                .build();
        var vdoc = new VDocument("This is a test.");
        var span1 = new VSpan(spanLayer, new VID(1), new VRange(0, 4), Map.of(), null);
        span1.setLabelHint("span1");
        vdoc.add(span1);
        var span2 = new VSpan(spanLayer, new VID(2), new VRange(5, 7), Map.of(), "000000");
        span2.setLabelHint("span2");
        span2.setScore(10.3123d);
        vdoc.add(span2);
        var span3 = new VSpan(spanLayer, new VID(3), VRange.clippedRange(vdoc, 0, 100).get(),
                Map.of(), "000000");
        span3.setLabelHint("span3");
        vdoc.add(span3);
        var rel = new VArc(relationLayer, new VID(4), span1.getVid(), span2.getVid(), "rel",
                Map.of(), "000000");
        vdoc.add(rel);
        vdoc.add(new VComment(span1.getVid(), VCommentType.INFO, "comment"));
        vdoc.add(new VComment(span2.getVid(), VCommentType.ERROR, "error"));
        vdoc.add(new VAnnotationMarker(VMarker.FOCUS, span1.getVid()));
        vdoc.add(new VTextMarker(VMarker.MATCH, new VRange(8, 9)));

        var req = RenderRequest.builder() //
                .withWindow(0, Integer.MAX_VALUE) //
                .build();
        var sut = new CompactSerializerV2Impl(annotationSchemaProperties);
        var cdoc = sut.render(vdoc, req);
        var actual = JSONUtil.toPrettyJsonString(cdoc);

        assertThat(actual)
                .isEqualTo(contentOf(getClass().getResource("/compactv2/reference.json")));
    }
}
