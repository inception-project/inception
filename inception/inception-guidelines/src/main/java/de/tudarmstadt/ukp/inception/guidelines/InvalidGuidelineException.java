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
package de.tudarmstadt.ukp.inception.guidelines;

import java.io.IOException;

/**
 * Thrown when a guideline is rejected, e.g. because it is not a PDF file or because its name is not
 * acceptable. The message is suitable for showing to the user.
 */
public class InvalidGuidelineException
    extends IOException
{
    private static final long serialVersionUID = 4208337421990114672L;

    public InvalidGuidelineException(String aMessage)
    {
        super(aMessage);
    }
}
