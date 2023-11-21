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
package org.dkpro.core.io.nif.internal;

/**
 * ITS vocabulary.
 * 
 * @see <a href="https://www.w3.org/TR/its20/">Internationalization Tag Set (ITS) Version 2.0</a>
 */
public class ITS
{
    public static final String PREFIX_ITS = "itsrdf";

    public static final String NS_ITS = "http://www.w3.org/2005/11/its/rdf#";

    public static final String PROP_TA_IDENT_REF = NS_ITS + "taIdentRef";

    public static final String PROP_TA_CLASS_REF = NS_ITS + "taClassRef";
}
