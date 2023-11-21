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
package de.tudarmstadt.ukp.clarin.webanno.brat.config;

public interface BratAnnotationEditorProperties
{
    /**
     * NBSP is recognized by Firefox as a proper addressable character in
     * {@code SVGText.getNumberOfChars()}.
     */
    public static String NBSP = "\u00A0";
    public static String REPLACEMENT_CHARACTER = "\uFFFD";

    boolean isSingleClickSelection();

    /**
     * @return whether the profiling built into the the brat visualization JS should be enabled. If
     *         this is enabled, profiling data is collected and a report is printed to the browser's
     *         JS console after every rendering action
     */
    boolean isClientSideProfiling();

    /**
     * Some browsers (e.g. Firefox) do not count invisible chars in some functions
     * {@code (e.g. SVGText.getNumberOfChars())} and this causes trouble. To avoid this, we replace
     * the chars with a visible whitespace character before sending the data to the browser.
     * 
     * This property indicates which character should be used to replace problematic whitespace
     * characters during rendering.
     * 
     * The default value is {@link #NBSP}
     * 
     * @see <a href="https://github.com/inception-project/inception/issues/1849">INCEpTION issue
     *      #1849</a>
     * @see <a href="https://github.com/webanno/webanno/issues/307">WebAnno issue #307</a>
     * @return a single replacement character.
     */
    String getWhiteSpaceReplacementCharacter();
}
