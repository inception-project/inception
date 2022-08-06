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
package de.tudarmstadt.ukp.inception.diam.editor;

import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class DiamJavaScriptReference
    extends JavaScriptResourceReference
{
    private static final long serialVersionUID = 8110724056092366243L;

    private static final DiamJavaScriptReference INSTANCE = new DiamJavaScriptReference();

    public static DiamJavaScriptReference get()
    {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private DiamJavaScriptReference()
    {
        super(DiamJavaScriptReference.class, "Diam.min.js");
    }
}
