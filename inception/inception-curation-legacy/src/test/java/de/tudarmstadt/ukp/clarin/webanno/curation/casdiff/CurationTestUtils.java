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

package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff;

import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.support.uima.AnnotationBuilder.buildAnnotation;
import static de.tudarmstadt.ukp.inception.support.uima.FeatureStructureBuilder.buildFS;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;
import static org.apache.uima.fit.factory.CasFactory.createCas;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.dkpro.core.io.conll.Conll2006Reader;
import org.dkpro.core.io.xmi.XmiReader;

import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv2Reader;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsv3XReader;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerSupport;

public class CurationTestUtils
{
    public static final String HOST_TYPE = "webanno.custom.LinkHost";
    public static final String LINK_TYPE = "webanno.custom.LinkType";
    public static final String SLOT_FILLER_TYPE = "webanno.custom.SlotFiller";

    public static JCas loadWebAnnoTsv3(String aPath) throws UIMAException, IOException
    {
        var reader = createReader( //
                WebannoTsv3XReader.class, //
                WebannoTsv3XReader.PARAM_SOURCE_LOCATION, "src/test/resources/" + aPath);

        var jcas = JCasFactory.createJCas();
        reader.getNext(jcas.getCas());
        return jcas;
    }

    public static Map<String, CAS> load(String... aPaths) throws UIMAException, IOException
    {
        var casByUser = new LinkedHashMap<String, CAS>();
        var n = 1;
        for (var path : aPaths) {
            var cas = readConll2006(path);
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

        var cas = createCas();

        reader.getNext(cas);

        return cas;
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
            var allTypes = new ArrayList<TypeSystemDescription>();
            allTypes.add(builtInTypes);
            allTypes.add(aType);
            cas = createCas(mergeTypeSystems(allTypes));
        }
        else {
            cas = createCas();
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

        CAS cas;
        if (aType != null) {
            TypeSystemDescription builtInTypes = createTypeSystemDescription();
            List<TypeSystemDescription> allTypes = new ArrayList<>();
            allTypes.add(builtInTypes);
            allTypes.add(aType);
            cas = createCas(mergeTypeSystems(allTypes));
        }
        else {
            cas = createCas();
        }

        reader.getNext(cas);

        return cas;
    }

    public static TypeSystemDescription createMultiLinkWithRoleTestTypeSytem() throws Exception
    {
        var typeSystems = new ArrayList<TypeSystemDescription>();

        var tsd = new TypeSystemDescription_impl();

        // Link type
        var linkTD = tsd.addType(LINK_TYPE, "", CAS.TYPE_NAME_TOP);
        linkTD.addFeature("role", "", TYPE_NAME_STRING);
        linkTD.addFeature("target", "", TYPE_NAME_ANNOTATION);

        // Link host
        var hostTD = tsd.addType(HOST_TYPE, "", TYPE_NAME_ANNOTATION);
        hostTD.addFeature("links", "", CAS.TYPE_NAME_FS_ARRAY, linkTD.getName(), false);

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
        linkTD.addFeature("role", "", TYPE_NAME_STRING);
        linkTD.addFeature("target", "", TYPE_NAME_ANNOTATION);

        // Link host
        var hostTD = tsd.addType(HOST_TYPE, "", TYPE_NAME_ANNOTATION);
        hostTD.addFeature("links", "", CAS.TYPE_NAME_FS_ARRAY, linkTD.getName(), false);
        for (var feature : aFeatures) {
            hostTD.addFeature(feature, "", TYPE_NAME_STRING);
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
            var td = type.addType(aTypeName, "", TYPE_NAME_ANNOTATION);
            for (var feature : aFeatures) {
                td.addFeature(feature, "", TYPE_NAME_STRING);
            }

        }
        else if (RelationLayerSupport.TYPE.equals(aType)) {
            var td = type.addType(aTypeName, "", TYPE_NAME_ANNOTATION);

            td.addFeature(FEAT_REL_TARGET, "", aAttacheType);
            td.addFeature(FEAT_REL_SOURCE, "", aAttacheType);

            for (var feature : aFeatures) {
                td.addFeature(feature, "", TYPE_NAME_STRING);
            }
        }

        return type;
    }

    public static AnnotationFS makeLinkHostFS(JCas aCas, int aBegin, int aEnd,
            FeatureStructure... aLinks)
    {
        return buildAnnotation(aCas.getCas(), HOST_TYPE) //
                .at(aBegin, aEnd) //
                .withFeature("links", asList(aLinks)) //
                .buildAndAddToIndexes();
    }

    public static FeatureStructure makeLinkFS(JCas aCas, String aSlotLabel, int aTargetBegin,
            int aTargetEnd)
    {
        var filler = buildAnnotation(aCas.getCas(), SLOT_FILLER_TYPE) //
                .at(aTargetBegin, aTargetEnd) //
                .buildAndAddToIndexes();

        return buildFS(aCas.getCas(), LINK_TYPE) //
                .withFeature("role", aSlotLabel) //
                .withFeature("target", filler) //
                .buildAndAddToIndexes();
    }
}
