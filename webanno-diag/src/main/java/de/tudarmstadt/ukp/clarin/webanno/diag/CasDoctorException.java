/*
 * Copyright 2016
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
 */
package de.tudarmstadt.ukp.clarin.webanno.diag;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;

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
}
