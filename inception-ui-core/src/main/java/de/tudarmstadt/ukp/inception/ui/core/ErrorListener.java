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
package de.tudarmstadt.ukp.inception.ui.core;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;

public class ErrorListener
    implements IRequestCycleListener
{
    public static final MetaDataKey<Exception> EXCEPTION = new MetaDataKey<Exception>()
    {
        private static final long serialVersionUID = 1L;
    };

    @Override
    public IRequestHandler onException(RequestCycle aCycle, Exception aEx)
    {
        aCycle.setMetaData(EXCEPTION, aEx);
        return IRequestCycleListener.super.onException(aCycle, aEx);
    }
}
