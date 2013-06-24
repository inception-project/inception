/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.ajax.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.junit.Test;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.JCasFactory;

import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.CasToBratJson;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.tcf.TcfReader;
import eu.clarin.weblicht.wlfxb.io.WLFormatException;
import eu.clarin.weblicht.wlfxb.tc.xb.TextCorpusLayerTag;

/**
 * Test case for generating Brat Json data for getcollection and getcollection actions
 *
 * @author Seid M. Yimam
 *
 */

public class CasToBratJsonTest
    extends TestCase
{
    /*
     * @Resource(name = "annotationService") private AnnotationService annotationService;
     *
     * @Resource(name = "jsonConverter") private MappingJacksonHttpMessageConverter jsonConverter;
     */

    private Log LOG = LogFactory.getLog(getClass());

    /**
     * generate BRAT JSON for the collection informations
     *
     * @throws IOException
     */
    @Test
    public void testGenerateBratJsonGetCollection()
        throws IOException

    {
        MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
        String jsonFilePath = "target/test-output/output_cas_to_json_collection.json";

        GetCollectionInformationResponse collectionInformation = new GetCollectionInformationResponse();

        BratAjaxConfiguration configuration = new BratAjaxConfiguration();

        List<Tag> tagList = new ArrayList<Tag>();

        AnnotationType type = new AnnotationType();
        type.setDescription("span annoattion");
        type.setName("pos");
        type.setType("span");

        TagSet tagset = new TagSet();
        tagset.setDescription("pos");
        tagset.setLanguage("de");
        tagset.setName("STTS");
        tagset.setType(type);

        Tag tag = new Tag();
        tag.setDescription("noun");
        tag.setName("NN");
        tag.setTagSet(tagset);
        tagList.add(tag);

        collectionInformation.setEntityTypes(configuration
                .configureVisualizationAndAnnotation(tagList));

        collectionInformation.addCollection("/Collection1/");
        collectionInformation.addCollection("/Collection2/");
        collectionInformation.addCollection("/Collection3/");

        collectionInformation.addDocument("/Collection1/doc1");
        collectionInformation.addDocument("/Collection2/doc1");
        collectionInformation.addDocument("/Collection3/doc1");
        collectionInformation.addDocument("/Collection1/doc2");
        collectionInformation.addDocument("/Collection2/doc2");
        collectionInformation.addDocument("/Collection3/doc2");

        collectionInformation.setSearchConfig(new ArrayList<String[]>());

        List<String> tagSetNames = new ArrayList<String>();
        tagSetNames.add(de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.POS);
        tagSetNames
                .add(de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.DEPENDENCY);
        tagSetNames
                .add(de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.NAMEDENTITY);
        tagSetNames
                .add(de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.COREFERENCE);
        tagSetNames
                .add(de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.COREFRELTYPE);


        ApplicationUtils.setJsonConverter(jsonConverter);
        ApplicationUtils.generateJson(collectionInformation, new File(jsonFilePath));

        String reference = FileUtils.readFileToString(new File(
                "src/test/resources/output_cas_to_json_collection_expected.json"), "UTF-8");
        String actual = FileUtils.readFileToString(new File(
                "target/test-output/output_cas_to_json_collection.json"), "UTF-8");
        assertEquals(reference, actual);
    }

    /**
     * generate brat JSON data for the document
     *
     * @throws IOException
     * @throws WLFormatException
     * @throws UIMAException
     */
    @Test
    public void testGenerateBratJsonGetDocument()
        throws IOException, WLFormatException, UIMAException

    {
        MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
        String jsonFilePath = "target/test-output/output_cas_to_json_document.json";

        EnumSet<TextCorpusLayerTag> layersToRead = EnumSet.of(TextCorpusLayerTag.TEXT,
                TextCorpusLayerTag.TOKENS, TextCorpusLayerTag.PARSING_DEPENDENCY,
                TextCorpusLayerTag.SENTENCES, TextCorpusLayerTag.POSTAGS,
                TextCorpusLayerTag.LEMMAS, TextCorpusLayerTag.NAMED_ENTITIES,
                TextCorpusLayerTag.REFERENCES);
        InputStream is = null;
        JCas jCas = null;
        try {
            // is = new FileInputStream("src/test/resources/tcf04-karin-wl.xml");
            String path = "src/test/resources/";
            String file = "tcf04-karin-wl.xml";
            CAS cas = JCasFactory.createJCas().getCas();
            CollectionReader reader = CollectionReaderFactory.createCollectionReader(
                    TcfReader.class, TcfReader.PARAM_PATH, path, TcfReader.PARAM_PATTERNS,
                    new String[] { "[+]" + file });
            if (!reader.hasNext()) {
                throw new FileNotFoundException("Annotation file [" + file + "] not found in ["
                        + path + "]");
            }
            reader.getNext(cas);
            jCas = cas.getJCas();

        }
        catch (FileNotFoundException ex) {
            LOG.info("The file specified not found " + ex.getCause());
        }
        catch (Exception ex) {
            LOG.info(ex);
        }

        List<String> tagSetNames = new ArrayList<String>();
        tagSetNames.add(de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.POS);
        tagSetNames
                .add(de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.DEPENDENCY);
        tagSetNames
                .add(de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.NAMEDENTITY);
        tagSetNames
                .add(de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.COREFERENCE);
        tagSetNames
                .add(de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant.COREFRELTYPE);

        CasToBratJson casToBratJson = new CasToBratJson();

        BratAnnotatorModel bratannotatorModel = new BratAnnotatorModel();
        bratannotatorModel.setWindowSize(10);
        bratannotatorModel.setSentenceAddress(BratAjaxCasUtil.getFirstSenetnceAddress(jCas));
        bratannotatorModel.setLastSentenceAddress(BratAjaxCasUtil.getLastSenetnceAddress(jCas));

        Project project = new Project();
        project.setReverseDependencyDirection(true);
        bratannotatorModel.setProject(project);
        bratannotatorModel.setMode(Mode.ANNOTATION);

        ApplicationUtils.setJsonConverter(jsonConverter);

        GetDocumentResponse response = new GetDocumentResponse();
        response.setText(jCas.getDocumentText());

        casToBratJson.addTokenToResponse(jCas, response, bratannotatorModel);
        casToBratJson.addSentenceToResponse(jCas, response, bratannotatorModel);

        SpanAdapter.getPosAdapter().render(jCas, response, bratannotatorModel);
        ChainAdapter.getCoreferenceLinkAdapter().render(jCas, response, bratannotatorModel);

        SpanAdapter.getLemmaAdapter().render(jCas, response, bratannotatorModel);
        SpanAdapter.getNamedEntityAdapter().render(jCas, response, bratannotatorModel);
        ArcAdapter.getDependencyAdapter().render(jCas, response, bratannotatorModel);
        ChainAdapter.getCoreferenceChainAdapter().render(jCas, response, bratannotatorModel);

        ApplicationUtils.generateJson(response, new File(jsonFilePath));

        String reference = FileUtils.readFileToString(new File(
                "src/test/resources/output_cas_to_json_document_expected.json"), "UTF-8");
        String actual = FileUtils.readFileToString(new File(
                "target/test-output/output_cas_to_json_document.json"), "UTF-8");
        assertEquals(reference, actual);
    }
}
