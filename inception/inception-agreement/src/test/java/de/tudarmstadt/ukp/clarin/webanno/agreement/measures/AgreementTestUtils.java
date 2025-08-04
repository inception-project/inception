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

package de.tudarmstadt.ukp.clarin.webanno.agreement.measures;

import static de.tudarmstadt.ukp.clarin.webanno.agreement.CodingStudyUtils.dumpCodingStudy;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.CodingStudyUtils.makeCodingStudy;
import static de.tudarmstadt.ukp.clarin.webanno.agreement.measures.ConcreteAgreementMeasure.COHEN_KAPPA_AGREEMENT;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;

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
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.util.CasCreationUtils;
import org.dkpro.core.io.conll.Conll2006Reader;
import org.dkpro.core.io.xmi.XmiReader;
import org.dkpro.statistics.agreement.InsufficientDataException;

import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv2Reader;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv3XReader;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;

public class AgreementTestUtils
{

    public static final String MULTI_VALUE_SPAN_TYPE = "MultiValueSpan";
    public static final String HOST_TYPE = "LinkHost";
    public static final String LINK_TYPE = "LinkType";

    public static Map<String, List<CAS>> load(String... aPaths) throws UIMAException, IOException
    {
        Map<String, List<CAS>> casByUser = new LinkedHashMap<>();
        int n = 1;
        for (String path : aPaths) {
            CAS cas = read(path);
            casByUser.put("user" + n, asList(cas));
            n++;
        }
        return casByUser;
    }

    public static Map<String, List<JCas>> loadWebAnnoTSV(TypeSystemDescription aTypes,
            String... aPaths)
        throws UIMAException, IOException
    {
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        int n = 1;
        for (String path : aPaths) {
            JCas cas = readWebAnnoTSV(path, aTypes);
            casByUser.put("user" + n, asList(cas));
            n++;
        }
        return casByUser;
    }

    public static Map<String, List<JCas>> loadXMI(TypeSystemDescription aTypes, String... aPaths)
        throws UIMAException, IOException
    {
        Map<String, List<JCas>> casByUser = new LinkedHashMap<>();
        int n = 1;
        for (String path : aPaths) {
            JCas cas = readXMI(path, aTypes);
            casByUser.put("user" + n, asList(cas));
            n++;
        }
        return casByUser;
    }

    public static CAS read(String aPath) throws UIMAException, IOException
    {
        CollectionReader reader = createReader(Conll2006Reader.class,
                Conll2006Reader.PARAM_SOURCE_LOCATION, "src/test/resources/" + aPath);

        CAS cas = JCasFactory.createJCas().getCas();

        reader.getNext(cas);

        return cas;
    }

    public static JCas readWebAnnoTSV(String aPath, TypeSystemDescription aType)
        throws UIMAException, IOException
    {
        CollectionReader reader = createReader(WebannoTsv2Reader.class,
                WebannoTsv2Reader.PARAM_SOURCE_LOCATION, "src/test/resources/" + aPath);
        JCas jcas;
        if (aType != null) {
            TypeSystemDescription builtInTypes = TypeSystemDescriptionFactory
                    .createTypeSystemDescription();
            List<TypeSystemDescription> allTypes = new ArrayList<>();
            allTypes.add(builtInTypes);
            allTypes.add(aType);
            jcas = JCasFactory.createJCas(CasCreationUtils.mergeTypeSystems(allTypes));
        }
        else {
            jcas = JCasFactory.createJCas();
        }

        reader.getNext(jcas.getCas());

        return jcas;
    }

    public static JCas readXMI(String aPath, TypeSystemDescription aType)
        throws UIMAException, IOException
    {
        CollectionReader reader = createReader(XmiReader.class, XmiReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/" + aPath);
        JCas jcas;
        if (aType != null) {
            TypeSystemDescription builtInTypes = TypeSystemDescriptionFactory
                    .createTypeSystemDescription();
            List<TypeSystemDescription> allTypes = new ArrayList<>();
            allTypes.add(builtInTypes);
            allTypes.add(aType);
            jcas = JCasFactory.createJCas(CasCreationUtils.mergeTypeSystems(allTypes));
        }
        else {
            jcas = JCasFactory.createJCas();
        }

        reader.getNext(jcas.getCas());

        return jcas;
    }

    public static TypeSystemDescription createMultiLinkWithRoleTestTypeSytem() throws Exception
    {
        List<TypeSystemDescription> typeSystems = new ArrayList<>();

        TypeSystemDescription tsd = new TypeSystemDescription_impl();

        // Link type
        TypeDescription linkTD = tsd.addType(LINK_TYPE, "", CAS.TYPE_NAME_TOP);
        linkTD.addFeature("role", "", CAS.TYPE_NAME_STRING);
        linkTD.addFeature("target", "", Token.class.getName());

        // Link host
        TypeDescription hostTD = tsd.addType(HOST_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        hostTD.addFeature("links", "", CAS.TYPE_NAME_FS_ARRAY, linkTD.getName(), false);

        typeSystems.add(tsd);
        typeSystems.add(TypeSystemDescriptionFactory.createTypeSystemDescription());

        return CasCreationUtils.mergeTypeSystems(typeSystems);
    }

    public static TypeSystemDescription createMultiLinkWithRoleTestTypeSytem(String... aFeatures)
        throws Exception
    {
        List<TypeSystemDescription> typeSystems = new ArrayList<>();

        TypeSystemDescription tsd = new TypeSystemDescription_impl();

        // Link type
        TypeDescription linkTD = tsd.addType(LINK_TYPE, "", CAS.TYPE_NAME_TOP);
        linkTD.addFeature("role", "", CAS.TYPE_NAME_STRING);
        linkTD.addFeature("target", "", Token.class.getName());

        // Link host
        TypeDescription hostTD = tsd.addType(HOST_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        hostTD.addFeature("links", "", CAS.TYPE_NAME_FS_ARRAY, linkTD.getName(), false);
        for (String feature : aFeatures) {
            hostTD.addFeature(feature, "", CAS.TYPE_NAME_STRING);
        }

        typeSystems.add(tsd);
        typeSystems.add(TypeSystemDescriptionFactory.createTypeSystemDescription());

        return CasCreationUtils.mergeTypeSystems(typeSystems);
    }

    public static TypeSystemDescription createCustomTypeSystem(String aType, String aTypeName,
            List<String> aFeatures, String aAttacheType)
        throws Exception
    {
        TypeSystemDescription type = new TypeSystemDescription_impl();
        if (aType.equals(SpanLayerSupport.TYPE)) {
            TypeDescription td = type.addType(aTypeName, "", CAS.TYPE_NAME_ANNOTATION);
            for (String feature : aFeatures) {
                td.addFeature(feature, "", CAS.TYPE_NAME_STRING);
            }

        }
        else if (aType.equals(RelationLayerSupport.TYPE)) {
            TypeDescription td = type.addType(aTypeName, "", CAS.TYPE_NAME_ANNOTATION);

            td.addFeature(WebAnnoConst.FEAT_REL_TARGET, "", aAttacheType);
            td.addFeature(WebAnnoConst.FEAT_REL_SOURCE, "", aAttacheType);

            for (String feature : aFeatures) {
                td.addFeature(feature, "", "uima.cas.String");
            }
        }

        return type;
    }

    public static void makeLinkHostFS(JCas aCas, int aBegin, int aEnd, FeatureStructure... aLinks)
    {
        Type hostType = aCas.getTypeSystem().getType(HOST_TYPE);
        AnnotationFS hostA1 = aCas.getCas().createAnnotation(hostType, aBegin, aEnd);
        hostA1.setFeatureValue(hostType.getFeatureByBaseName("links"),
                FSCollectionFactory.createFSArray(aCas, asList(aLinks)));
        aCas.getCas().addFsToIndexes(hostA1);
    }

    public static AnnotationFS makeLinkHostMultiSPanFeatureFS(JCas aCas, int aBegin, int aEnd,
            Feature aSpanFeature, String aValue, FeatureStructure... aLinks)
    {
        Type hostType = aCas.getTypeSystem().getType(HOST_TYPE);
        AnnotationFS hostA1 = aCas.getCas().createAnnotation(hostType, aBegin, aEnd);
        hostA1.setFeatureValue(hostType.getFeatureByBaseName("links"),
                FSCollectionFactory.createFSArray(aCas, asList(aLinks)));
        hostA1.setStringValue(aSpanFeature, aValue);
        aCas.getCas().addFsToIndexes(hostA1);
        return hostA1;
    }

    public static FeatureStructure makeLinkFS(JCas aCas, String aSlotLabel, int aTargetBegin,
            int aTargetEnd)
    {
        Token token1 = new Token(aCas, aTargetBegin, aTargetEnd);
        token1.addToIndexes();

        Type linkType = aCas.getTypeSystem().getType(LINK_TYPE);
        FeatureStructure linkA1 = aCas.getCas().createFS(linkType);
        linkA1.setStringValue(linkType.getFeatureByBaseName("role"), aSlotLabel);
        linkA1.setFeatureValue(linkType.getFeatureByBaseName("target"), token1);
        aCas.getCas().addFsToIndexes(linkA1);

        return linkA1;
    }

    public static JCas loadWebAnnoTsv3(String aPath) throws UIMAException, IOException
    {
        CollectionReader reader = createReader(WebannoTsv3XReader.class,
                WebannoTsv3XReader.PARAM_SOURCE_LOCATION, "src/test/resources/" + aPath);
        JCas jcas = JCasFactory.createJCas();
        reader.getNext(jcas.getCas());
        return jcas;
    }

    public static JCas loadWebAnnoTsv3(File aPath) throws UIMAException, IOException
    {
        CollectionReader reader = createReader(WebannoTsv3XReader.class,
                WebannoTsv3XReader.PARAM_SOURCE_LOCATION, aPath);
        JCas jcas = JCasFactory.createJCas();
        reader.getNext(jcas.getCas());
        return jcas;
    }

    public static CAS readConll2006(String aPath) throws UIMAException, IOException
    {
        CollectionReader reader = createReader(Conll2006Reader.class,
                Conll2006Reader.PARAM_SOURCE_LOCATION, "src/test/resources/" + aPath);

        CAS jcas = JCasFactory.createJCas().getCas();

        reader.getNext(jcas);

        return jcas;
    }

    public static TypeSystemDescription createMultiValueStringTestTypeSystem()
        throws ResourceInitializationException
    {
        List<TypeSystemDescription> typeSystems = new ArrayList<>();

        TypeSystemDescription tsd = new TypeSystemDescription_impl();

        TypeDescription mvSpan = tsd.addType(MULTI_VALUE_SPAN_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        mvSpan.addFeature("values", "", CAS.TYPE_NAME_STRING_ARRAY, CAS.TYPE_NAME_STRING, false);
        typeSystems.add(tsd);
        typeSystems.add(TypeSystemDescriptionFactory.createTypeSystemDescription());

        return CasCreationUtils.mergeTypeSystems(typeSystems);
    }

    public static TypeSystemDescription createMultiLinkWithRoleTestTypeSystem(String... aFeatures)
        throws Exception
    {
        List<TypeSystemDescription> typeSystems = new ArrayList<>();

        TypeSystemDescription tsd = new TypeSystemDescription_impl();

        // Link type
        TypeDescription linkTD = tsd.addType(LINK_TYPE, "", CAS.TYPE_NAME_TOP);
        linkTD.addFeature("role", "", CAS.TYPE_NAME_STRING);
        linkTD.addFeature("target", "", CAS.TYPE_NAME_ANNOTATION);

        // Link host
        TypeDescription hostTD = tsd.addType(HOST_TYPE, "", CAS.TYPE_NAME_ANNOTATION);
        hostTD.addFeature("links", "", CAS.TYPE_NAME_FS_ARRAY, linkTD.getName(), false);
        for (String feature : aFeatures) {
            hostTD.addFeature(feature, "", CAS.TYPE_NAME_STRING);
        }

        typeSystems.add(tsd);
        typeSystems.add(TypeSystemDescriptionFactory.createTypeSystemDescription());

        return CasCreationUtils.mergeTypeSystems(typeSystems);
    }

    @Deprecated
    public static FullCodingAgreementResult getCohenKappaAgreement(CasDiff aDiff, String aType,
            String aFeature, Map<String, CAS> aCasMap)
    {
        return getAgreement(COHEN_KAPPA_AGREEMENT, true, aDiff, aType, aFeature, aCasMap);
    }

    @Deprecated
    public static FullCodingAgreementResult getAgreement(ConcreteAgreementMeasure aMeasure,
            boolean aExcludeIncomplete, CasDiff aDiff, String aType, String aFeature,
            Map<String, CAS> aCasMap)
    {
        if (aCasMap.size() != 2) {
            throw new IllegalArgumentException("CAS map must contain exactly two CASes");
        }

        var agreementResult = makeCodingStudy(aDiff, aType, aFeature, emptySet(),
                aExcludeIncomplete, aCasMap);
        try {
            var agreement = aMeasure.make(agreementResult.getStudy());

            if (!agreementResult.isEmpty()) {
                agreementResult.setAgreement(agreement.calculateAgreement());
            }
            else {
                agreementResult.setAgreement(Double.NaN);
            }

        }
        catch (InsufficientDataException e) {
            agreementResult.setAgreement(Double.NaN);
        }
        catch (RuntimeException e) {
            // FIXME
            dumpCodingStudy(System.out, agreementResult);
            throw e;
        }

        return agreementResult;
    }
}
