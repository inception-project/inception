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

import java.util.List;

import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class CasDoctorException
    extends RuntimeException
{
    private static final long serialVersionUID = 2061804333509464073L;
    private List<LogMessage> details;

    public CasDoctorException()
    {
        super();
    }

    public CasDoctorException(List<LogMessage> aDetails)
    {
        super();
        details = aDetails;
    }

    public CasDoctorException(String aMessage, Throwable aCause, boolean aEnableSuppression,
            boolean aWritableStackTrace)
    {
        super(aMessage, aCause, aEnableSuppression, aWritableStackTrace);
    }

    public CasDoctorException(String aMessage, Throwable aCause)
    {
        super(aMessage, aCause);
    }

    public CasDoctorException(String aMessage)
    {
        super(aMessage);
    }

    public CasDoctorException(Throwable aCause)
    {
        super(aCause);
    }

    public List<LogMessage> getDetails()
    {
        return details;
    }

    @Override
    public String getMessage()
    {
        var buffer = new StringBuffer();
        buffer.append("CasDoctor found " + details.size() + " issues:\n");
        for (var msg : details) {
            buffer.append(msg.getMessage());
            buffer.append("\n");
        }
        return buffer.toString();
    }
}
