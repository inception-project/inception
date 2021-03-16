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
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

/**
 * Response to the {@code getDocumentTimestamp} command.
 */
public class GetDocumentTimestampResponse
    extends AjaxResponse
{
    public static final String COMMAND = "getDocumentTimestamp";

    private long mtime;

    public GetDocumentTimestampResponse()
    {
        super(COMMAND);
    }

    /**
     * Get last modification time.
     *
     * @return last modification time.
     */
    public long getMtime()
    {
        return mtime;
    }

    /**
     * Set last modification time.
     *
     * @param aMtime
     *            last modfication time.
     */
    public void setMtime(long aMtime)
    {
        mtime = aMtime;
    }

    public static boolean is(String aCommand)
    {
        return COMMAND.equals(aCommand);
    }
}
