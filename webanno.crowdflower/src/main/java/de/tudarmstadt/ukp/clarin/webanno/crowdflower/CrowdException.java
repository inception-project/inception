/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.crowdflower;

/**
 * Exception which occurs then there is a problem in processing data uploaded to or returned by crowdflower.com
 * @author ich
 *
 */
public class CrowdException
    extends Exception
{
    private static final long serialVersionUID = 1L;

    public CrowdException()
    {
        super();
    }

    public CrowdException(String message)
    {
        super(message);
    }

    public CrowdException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public CrowdException(Throwable cause)
    {
        super(cause);
    }
}
