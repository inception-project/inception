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
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.linesOf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class GetCollectionInformationResponseTest
{
    /**
     * generate BRAT JSON for the collection information
     *
     * @throws IOException
     *             if an I/O error occurs.
     */
    @Test
    public void testGenerateBratJsonGetCollection() throws IOException
    {
        String jsonFilePath = "target/test-output/output_cas_to_json_collection.json";

        GetCollectionInformationResponse collectionInformation = new GetCollectionInformationResponse();

        List<AnnotationLayer> layerList = new ArrayList<>();

        AnnotationLayer layer = new AnnotationLayer();
        layer.setId(1l);
        layer.setDescription("span annoattion");
        layer.setName("pos");
        layer.setType(SpanLayerSupport.TYPE);

        TagSet tagset = new TagSet();
        tagset.setId(1l);
        tagset.setDescription("pos");
        tagset.setLanguage("de");
        tagset.setName("STTS");

        Tag tag = new Tag();
        tag.setId(1l);
        tag.setDescription("noun");
        tag.setName("NN");
        tag.setTagSet(tagset);

        layerList.add(layer);

        // collectionInformation.addCollection("/Collection1/");
        // collectionInformation.addCollection("/Collection2/");
        // collectionInformation.addCollection("/Collection3/");
        //
        // collectionInformation.addDocument("/Collection1/doc1");
        // collectionInformation.addDocument("/Collection2/doc1");
        // collectionInformation.addDocument("/Collection3/doc1");
        // collectionInformation.addDocument("/Collection1/doc2");
        // collectionInformation.addDocument("/Collection2/doc2");
        // collectionInformation.addDocument("/Collection3/doc2");
        //
        // collectionInformation.setSearchConfig(new ArrayList<>());

        List<String> tagSetNames = new ArrayList<>();
        tagSetNames.add(de.tudarmstadt.ukp.inception.support.WebAnnoConst.POS);
        tagSetNames.add(de.tudarmstadt.ukp.inception.support.WebAnnoConst.DEPENDENCY);
        tagSetNames.add(de.tudarmstadt.ukp.inception.support.WebAnnoConst.NAMEDENTITY);
        tagSetNames.add(de.tudarmstadt.ukp.inception.support.WebAnnoConst.COREFERENCE);
        tagSetNames.add(de.tudarmstadt.ukp.inception.support.WebAnnoConst.COREFRELTYPE);

        JSONUtil.generatePrettyJson(collectionInformation, new File(jsonFilePath));

        assertThat(
                linesOf(new File("src/test/resources/output_cas_to_json_collection_expected.json"),
                        "UTF-8")).isEqualTo(linesOf(new File(jsonFilePath), "UTF-8"));
    }
}
