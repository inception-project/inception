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

import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.RELATION_TYPE;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static de.tudarmstadt.ukp.inception.support.uima.FeatureStructureBuilder.buildFS;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.dkpro.core.io.conll.Conll2006Reader;
import org.dkpro.core.io.xmi.XmiReader;

import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv2Reader;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv3XReader;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;

public class CurationTestUtils
{
    public static final String TARGET_FEATURE = "target";
    public static final String ROLE_FEATURE = "role";
    public static final String LINKS_FEATURE = "links";
    public static final String ALT_LINKS_FEATURE = "altLinks";
    public static final String HOST_TYPE = "webanno.custom.LinkHost";
    public static final String LINK_TYPE = "webanno.custom.LinkType";

    public static JCas loadWebAnnoTsv3(String aPath) throws UIMAException, IOException
    {
        var reader = createReader( //
                WebannoTsv3XReader.class, //
                WebannoTsv3XReader.PARAM_SOURCE_LOCATION, "src/test/resources/" + aPath);
        var jcas = JCasFactory.createJCas();
        reader.getNext(jcas.getCas());
        return jcas;
    }

    public static JCas loadWebAnnoTsv3(File aPath) throws UIMAException, IOException
    {
        var reader = createReader( //
                WebannoTsv3XReader.class, //
                WebannoTsv3XReader.PARAM_SOURCE_LOCATION, aPath);
        var jcas = JCasFactory.createJCas();
        reader.getNext(jcas.getCas());
        return jcas;
    }

    public static Map<String, List<CAS>> load(String... aPaths) throws UIMAException, IOException
    {
        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        int n = 1;
        for (String path : aPaths) {
            CAS cas = readConll2006(path);
            casByUser.put("user" + n, asList(cas));
            n++;
        }
        return casByUser;
    }

    public static Map<String, List<CAS>> loadWebAnnoTSV(TypeSystemDescription aTypes,
            String... aPaths)
        throws UIMAException, IOException
    {
        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        int n = 1;
        for (String path : aPaths) {
            var cas = readWebAnnoTSV(path, aTypes);
            casByUser.put("user" + n, asList(cas));
            n++;
        }
        return casByUser;
    }

    public static Map<String, CAS> loadXMI(TypeSystemDescription aTypes, String... aPaths)
        throws UIMAException, IOException
    {
        Map<String, CAS> casByUser = new LinkedHashMap<>();
        int n = 1;
        for (var path : aPaths) {
            var cas = readXMI(path, aTypes);
            casByUser.put("user" + n, cas);
            n++;
        }
        return casByUser;
    }

    public static CAS readConll2006(String aPath) throws UIMAException, IOException
    {
        var reader = createReader( //
                Conll2006Reader.class, //
                Conll2006Reader.PARAM_SOURCE_LOCATION, "src/test/resources/" + aPath);

        var jcas = JCasFactory.createJCas().getCas();

        reader.getNext(jcas);

        return jcas;
    }

    public static CAS readWebAnnoTSV(String aPath, TypeSystemDescription aType)
        throws UIMAException, IOException
    {
        var reader = createReader( //
                WebannoTsv2Reader.class, //
                WebannoTsv2Reader.PARAM_SOURCE_LOCATION, "src/test/resources/" + aPath);
        CAS cas;
        if (aType != null) {
            var builtInTypes = createTypeSystemDescription();
            List<TypeSystemDescription> allTypes = new ArrayList<>();
            allTypes.add(builtInTypes);
            allTypes.add(aType);
            cas = JCasFactory.createJCas(mergeTypeSystems(allTypes)).getCas();
        }
        else {
            cas = JCasFactory.createJCas().getCas();
        }

        reader.getNext(cas);

        return cas;
    }

    public static CAS readXMI(String aPath, TypeSystemDescription aType)
        throws UIMAException, IOException
    {
        var reader = createReader( //
                XmiReader.class, //
                XmiReader.PARAM_SOURCE_LOCATION, "src/test/resources/" + aPath);
        CAS jcas;
        if (aType != null) {
            TypeSystemDescription builtInTypes = createTypeSystemDescription();
            List<TypeSystemDescription> allTypes = new ArrayList<>();
            allTypes.add(builtInTypes);
            allTypes.add(aType);
            jcas = JCasFactory.createJCas(mergeTypeSystems(allTypes)).getCas();
        }
        else {
            jcas = JCasFactory.createJCas().getCas();
        }

        reader.getNext(jcas);

        return jcas;
    }

    public static TypeSystemDescription createMultiLinkWithRoleTestTypeSytem() throws Exception
    {
        List<TypeSystemDescription> typeSystems = new ArrayList<>();

        var tsd = new TypeSystemDescription_impl();

        // Link type
        var linkTD = tsd.addType(LINK_TYPE, "", CAS.TYPE_NAME_TOP);
        linkTD.addFeature(ROLE_FEATURE, "", CAS.TYPE_NAME_STRING);
        linkTD.addFeature(TARGET_FEATURE, "", CAS.TYPE_NAME_ANNOTATION);

        // Link host
        var hostTD = tsd.addType(HOST_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        hostTD.addFeature(LINKS_FEATURE, "", CAS.TYPE_NAME_FS_ARRAY, linkTD.getName(), false);

        typeSystems.add(tsd);
        typeSystems.add(createTypeSystemDescription());

        return mergeTypeSystems(typeSystems);
    }

    public static TypeSystemDescription createMultiLinkWithRoleTestTypeSystem(String... aFeatures)
        throws Exception
    {
        var typeSystems = new ArrayList<TypeSystemDescription>();

        var tsd = new TypeSystemDescription_impl();

        // Link type
        var linkTD = tsd.addType(LINK_TYPE, "", CAS.TYPE_NAME_TOP);
        linkTD.addFeature(ROLE_FEATURE, "", CAS.TYPE_NAME_STRING);
        linkTD.addFeature(TARGET_FEATURE, "", CAS.TYPE_NAME_ANNOTATION);

        // Link host
        var hostTD = tsd.addType(HOST_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        hostTD.addFeature(LINKS_FEATURE, "", CAS.TYPE_NAME_FS_ARRAY, linkTD.getName(), false);
        for (var feature : aFeatures) {
            hostTD.addFeature(feature, "", CAS.TYPE_NAME_STRING);
        }

        typeSystems.add(tsd);
        typeSystems.add(createTypeSystemDescription());

        return mergeTypeSystems(typeSystems);
    }

    public static TypeSystemDescription createCustomTypeSystem(String aType, String aTypeName,
            List<String> aFeatures, String aAttacheType)
        throws Exception
    {
        var type = new TypeSystemDescription_impl();
        if (SpanLayerSupport.TYPE.equals(aType)) {
            var td = type.addType(aTypeName, "", CAS.TYPE_NAME_ANNOTATION);
            for (var feature : aFeatures) {
                td.addFeature(feature, "", CAS.TYPE_NAME_STRING);
            }

        }
        else if (aType.equals(RELATION_TYPE)) {
            var td = type.addType(aTypeName, "", CAS.TYPE_NAME_ANNOTATION);

            td.addFeature(WebAnnoConst.FEAT_REL_TARGET, "", aAttacheType);
            td.addFeature(WebAnnoConst.FEAT_REL_SOURCE, "", aAttacheType);

            for (var feature : aFeatures) {
                td.addFeature(feature, "", "uima.cas.String");
            }
        }

        return type;
    }

    public static AnnotationFS makeLinkHostFS(JCas aCas, int aBegin, int aEnd,
            FeatureStructure... aLinks)
    {
        return buildAnnotation(aCas.getCas(), HOST_TYPE) //
                .at(aBegin, aEnd) //
                .withFeature(LINKS_FEATURE, asList(aLinks)) //
                .buildAndAddToIndexes();
    }

    public static AnnotationFS makeLinkHostFS(JCas aCas, int aBegin, int aEnd, Feature aSpanFeature,
            String aValue, FeatureStructure... aLinks)
    {
        return buildAnnotation(aCas.getCas(), HOST_TYPE) //
                .at(aBegin, aEnd) //
                .withFeature(aSpanFeature, aValue) //
                .withFeature(LINKS_FEATURE, asList(aLinks)) //
                .buildAndAddToIndexes();
    }

    public static FeatureStructure makeLinkFS(JCas aCas, int aTargetBegin, int aTargetEnd)
    {
        var filler = new NamedEntity(aCas, aTargetBegin, aTargetEnd);
        filler.addToIndexes();

        return linkTo(null, filler);

    }

    public static FeatureStructure makeLinkFS(JCas aCas, String aRole, int aTargetBegin,
            int aTargetEnd)
    {
        var filler = new NamedEntity(aCas, aTargetBegin, aTargetEnd);
        filler.addToIndexes();

        return linkTo(aRole, filler);
    }

    public static FeatureStructure linkTo(String aRole, Annotation aFiller)
    {
        return buildFS(aFiller.getJCas().getCas(), LINK_TYPE) //
                .withFeature(ROLE_FEATURE, aRole) //
                .withFeature(TARGET_FEATURE, aFiller) //
                .buildAndAddToIndexes();
    }
}
