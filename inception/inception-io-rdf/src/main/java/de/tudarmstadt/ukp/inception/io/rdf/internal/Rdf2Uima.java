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
package de.tudarmstadt.ukp.inception.io.rdf.internal;

import static de.tudarmstadt.ukp.inception.io.rdf.internal.RdfCas.NS_RDFCAS;
import static de.tudarmstadt.ukp.inception.io.rdf.internal.RdfCas.PROP_INDEXED_IN;
import static de.tudarmstadt.ukp.inception.io.rdf.internal.RdfCas.PROP_SOFA_ID;
import static de.tudarmstadt.ukp.inception.io.rdf.internal.RdfCas.PROP_SOFA_MIME_TYPE;
import static de.tudarmstadt.ukp.inception.io.rdf.internal.RdfCas.PROP_SOFA_STRING;
import static de.tudarmstadt.ukp.inception.io.rdf.internal.RdfCas.SCHEME_UIMA;
import static de.tudarmstadt.ukp.inception.io.rdf.internal.RdfCas.TYPE_FEATURE_STRUCTURE;
import static de.tudarmstadt.ukp.inception.io.rdf.internal.RdfCas.TYPE_VIEW;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class Rdf2Uima
{
    public static void convert(Model aModel, Statement aContext, JCas aJCas) throws CASException
    {
        var m = aModel;

        var fsIndex = new HashMap<Resource, FeatureStructure>();

        // Convert the views/SofAs
        var viewIndex = new HashMap<Resource, JCas>();
        for (var view : aModel.filter(null, RDF.TYPE, TYPE_VIEW).subjects()) {
            var viewJCas = convertView(aModel, view, aJCas);
            viewIndex.put(view, viewJCas);
            fsIndex.put(view, viewJCas.getSofa());
        }

        // Convert the FSes but without setting their feature values yet - we cannot fill
        // the feature values just set because some of them may point to FSes not yet created
        var fses = m.filter(null, RDF.TYPE, TYPE_FEATURE_STRUCTURE).subjects()
                .toArray(Resource[]::new);
        for (var fs : fses) {
            var uimaFS = initFS(aModel, fs, aJCas);
            fsIndex.put(fs, uimaFS);
        }

        // Now fill the FSes with their feature values
        for (var fs : fses) {
            convertFS(aModel, fs, aJCas, fsIndex);
        }

        // Finally add the FSes to the indexes of the respective views
        for (var fs : fses) {
            for (var indexedIn : aModel.filter(fs, PROP_INDEXED_IN, null).objects()) {
                var viewJCas = viewIndex.get(indexedIn);
                viewJCas.addFsToIndexes(fsIndex.get(fs));
            }
        }
    }

    public static JCas convertView(Model aModel, Resource aView, JCas aJCas) throws CASException
    {
        // Get the values
        var viewName = aModel.filter(aView, PROP_SOFA_ID, null).objects().iterator().next()
                .stringValue();
        var sofaString = aModel.filter(aView, PROP_SOFA_STRING, null).objects().iterator().next()
                .stringValue();
        var sofaMimeType = aModel.filter(aView, PROP_SOFA_MIME_TYPE, null).objects().iterator()
                .next().stringValue();

        // Instantiate the view/SofA
        var view = JCasUtil.getView(aJCas, viewName, true);
        view.setSofaDataString(sofaString, sofaMimeType);

        return view;
    }

    public static FeatureStructure initFS(Model aModel, Resource aFS, JCas aJCas)
    {
        var cas = aJCas.getCas();

        // Figure out the UIMA type - there can be only one type per FS
        var types = aModel.filter(aFS, RDF.TYPE, null).objects();
        types.removeIf(res -> res.stringValue().startsWith(NS_RDFCAS));
        assert types.size() == 1;
        var type = CasUtil.getType(cas,
                types.iterator().next().stringValue().substring(SCHEME_UIMA.length()));

        FeatureStructure fs;
        if (type.getName().equals(DocumentMetaData.class.getName())) {
            // Special handling to avoid ending up with two document annotations in the CAS
            fs = DocumentMetaData.get(aJCas);
        }
        else {
            fs = cas.createFS(type);
        }

        return fs;
    }

    public static FeatureStructure convertFS(Model aModel, Resource aFS, JCas aJCas,
            Map<Resource, FeatureStructure> aFsIndex)
    {
        var fs = aFsIndex.get(aFS);

        for (var stmt : aModel.filter(aFS, null, null)) {
            // Skip all non-features
            if (!stmt.getPredicate().stringValue().startsWith("uima:")) {
                continue;
            }

            var featureName = substringAfterLast(stmt.getPredicate().stringValue(), "-");
            var uimaFeat = fs.getType().getFeatureByBaseName(featureName);

            // Cannot update start/end of document annotation because that FS is already indexed, so
            // we skip those
            if (fs == aJCas.getDocumentAnnotationFs()
                    && (CAS.FEATURE_BASE_NAME_BEGIN.equals(featureName)
                            || CAS.FEATURE_BASE_NAME_END.equals(featureName))) {
                continue;
            }

            if (uimaFeat.getRange().isPrimitive()) {
                Literal literal = null;
                if (stmt.getObject().isLiteral()) {
                    literal = (Literal) stmt.getObject();
                }

                switch (uimaFeat.getRange().getName()) {
                case CAS.TYPE_NAME_BOOLEAN:
                    fs.setBooleanValue(uimaFeat, literal.booleanValue());
                    break;
                case CAS.TYPE_NAME_BYTE:
                    fs.setByteValue(uimaFeat, literal.byteValue());
                    break;
                case CAS.TYPE_NAME_DOUBLE:
                    fs.setDoubleValue(uimaFeat, literal.doubleValue());
                    break;
                case CAS.TYPE_NAME_FLOAT:
                    fs.setFloatValue(uimaFeat, literal.floatValue());
                    break;
                case CAS.TYPE_NAME_INTEGER:
                    fs.setIntValue(uimaFeat, literal.intValue());
                    break;
                case CAS.TYPE_NAME_LONG:
                    fs.setLongValue(uimaFeat, literal.longValue());
                    break;
                case CAS.TYPE_NAME_SHORT:
                    fs.setShortValue(uimaFeat, literal.shortValue());
                    break;
                case CAS.TYPE_NAME_STRING: {
                    fs.setStringValue(uimaFeat, stmt.getObject().stringValue());
                    break;
                }
                default:
                    throw new IllegalArgumentException(
                            "Feature [" + uimaFeat.getName() + "] has unsupported primitive type ["
                                    + uimaFeat.getRange().getName() + "]");
                }
            }
            else {
                var targetUimaFS = aFsIndex.get(stmt.getObject());
                if (targetUimaFS == null) {
                    throw new IllegalStateException(
                            "No UIMA FS found for [" + stmt.getObject().stringValue() + "]");
                }
                fs.setFeatureValue(uimaFeat, targetUimaFS);
            }
        }

        return fs;
    }
}
