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
package de.tudarmstadt.ukp.inception.support.xml.sanitizer;

public enum AttributeAction
{
    /**
     * Pass attribute as-is.
     */
    PASS, //

    /**
     * Pass attribute but remove the namespace.
     * <p>
     * The CSS {@code content: attr(XXX)} construct is unable to access attributes that are not in
     * the default namespace. Support for adding access to namespaced-attributes appears to have
     * been present in early proposals of the
     * <a href="https://www.w3.org/1999/06/25/WD-css3-namespace-19990625/#attr-function">CSS3
     * namespace enhancements</a> but appear to have been dropped for the final recommendation.
     * Also, browsers do not appear (yet) to have implemented support for this on their own.
     * <p>
     * Thus, if the attribute contains data that needs to be accessed using
     * {@code content: attr(XXX)}, then use this.
     */
    PASS_NO_NS, //

    /**
     * Attribute is not passed on - it is dropped.
     */
    DROP;
}
