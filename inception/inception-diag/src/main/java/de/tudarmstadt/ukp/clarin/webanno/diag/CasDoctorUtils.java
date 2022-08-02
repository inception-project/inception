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
package de.tudarmstadt.ukp.clarin.webanno.diag;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.impl.LowLevelCAS;

public class CasDoctorUtils
{
    public static Set<FeatureStructure> collectIndexed(CAS aCas)
    {
        LowLevelCAS llcas = aCas.getLowLevelCAS();
        Set<FeatureStructure> fses = new TreeSet<>(Comparator.comparingInt(llcas::ll_getFSRef));

        FSIterator<FeatureStructure> i = aCas.getIndexRepository()
                .getAllIndexedFS(aCas.getTypeSystem().getTopType());

        i.forEachRemaining(fses::add);

        return fses;
    }

    public static Set<FeatureStructure> collectReachable(CAS aCas)
    {
        LowLevelCAS llcas = aCas.getLowLevelCAS();
        Set<FeatureStructure> fses = new TreeSet<>(Comparator.comparingInt(llcas::ll_getFSRef));

        FSIterator<FeatureStructure> i = aCas.getIndexRepository()
                .getAllIndexedFS(aCas.getTypeSystem().getTopType());

        i.forEachRemaining(fs -> collect(fses, fs));

        return fses;
    }

    public static void collect(Set<FeatureStructure> aFSes, FeatureStructure aFS)
    {
        if (aFS != null && !aFSes.contains(aFS)) {
            aFSes.add(aFS);

            for (Feature f : aFS.getType().getFeatures()) {
                if (!f.getRange().isPrimitive()
                        && !CAS.FEATURE_BASE_NAME_SOFA.equals(f.getShortName())) {
                    collect(aFSes, aFS.getFeatureValue(f));
                }
            }
        }
    }

    /**
     * Recursively collect referenced FSes and also record for each the last indexed FS that refers
     * the them.
     * 
     * @param aFSes
     *            map collecting the found feature structures
     * @param aIndexed
     *            set if feature structures that are in the CAS'es index
     * @param aFS
     *            the current feature structure
     * @param aLastIndexed
     *            the last feature structure on the traversal path that was still in the CAS'es
     *            index
     */
    public static void collect(Map<FeatureStructure, FeatureStructure> aFSes,
            Set<FeatureStructure> aIndexed, FeatureStructure aFS, FeatureStructure aLastIndexed)
    {
        if (aFS != null && !aFSes.containsKey(aFS)) {
            // We might find an annotation indirectly. In that case make sure we consider it as
            // an indexed annotation instead of wrongly recording it as non-indexed
            if (aIndexed.contains(aFS)) {
                aFSes.put(aFS, aFS);
            }
            else {
                aFSes.put(aFS, aLastIndexed);
            }

            for (Feature f : aFS.getType().getFeatures()) {
                if (!f.getRange().isPrimitive()
                        && !CAS.FEATURE_BASE_NAME_SOFA.equals(f.getShortName())) {
                    collect(aFSes, aIndexed, aFS.getFeatureValue(f),
                            aIndexed.contains(aFS) ? aFS : aLastIndexed);
                }
            }
        }
    }

    public static Set<FeatureStructure> getNonIndexedFSes(CAS aCas)
    {
        TypeSystem ts = aCas.getTypeSystem();

        Set<FeatureStructure> allIndexedFS = collectIndexed(aCas);
        Set<FeatureStructure> allReachableFS = collectReachable(aCas);

        // Remove all that are indexed
        allReachableFS.removeAll(allIndexedFS);

        // Remove all that are not annotations
        allReachableFS.removeIf(fs -> !ts.subsumes(aCas.getAnnotationType(), fs.getType()));

        // All that is left are non-index annotations
        return allReachableFS;
    }

    public static Map<FeatureStructure, FeatureStructure> getNonIndexedFSesWithOwner(CAS aCas)
    {
        TypeSystem ts = aCas.getTypeSystem();

        LowLevelCAS llcas = aCas.getLowLevelCAS();

        Set<FeatureStructure> allIndexedFS = collectIndexed(aCas);
        Map<FeatureStructure, FeatureStructure> allReachableFS = new TreeMap<>(
                Comparator.comparingInt(llcas::ll_getFSRef));

        FSIterator<FeatureStructure> i = aCas.getIndexRepository()
                .getAllIndexedFS(aCas.getTypeSystem().getTopType());

        i.forEachRemaining(fs -> collect(allReachableFS, allIndexedFS, fs, fs));

        // Remove all that are not annotations
        allReachableFS.entrySet()
                .removeIf(e -> !ts.subsumes(aCas.getAnnotationType(), e.getKey().getType()));

        // Remove all that are indexed
        allReachableFS.entrySet().removeIf(e -> e.getKey() == e.getValue());

        // All that is left are non-index annotations
        return allReachableFS;
    }
}
