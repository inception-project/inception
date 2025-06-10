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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.chatgpt.client;

public class RateLimitInfo
{
    private int limitRequestsDay;
    private int limitTokensMinute;
    private int remainingRequestsDay;
    private int remainingTokensMinute;
    private double resetRequestsDay;
    private double resetTokensMinute;

    public int getLimitRequestsDay()
    {
        return limitRequestsDay;
    }

    public void setLimitRequestsDay(int aLimitRequestsDay)
    {
        limitRequestsDay = aLimitRequestsDay;
    }

    public int getLimitTokensMinute()
    {
        return limitTokensMinute;
    }

    public void setLimitTokensMinute(int aLimitTokensMinute)
    {
        limitTokensMinute = aLimitTokensMinute;
    }

    public int getRemainingRequestsDay()
    {
        return remainingRequestsDay;
    }

    public void setRemainingRequestsDay(int aRemainingRequestsDay)
    {
        remainingRequestsDay = aRemainingRequestsDay;
    }

    public int getRemainingTokensMinute()
    {
        return remainingTokensMinute;
    }

    public void setRemainingTokensMinute(int aRemainingTokensMinute)
    {
        remainingTokensMinute = aRemainingTokensMinute;
    }

    public double getResetRequestsDay()
    {
        return resetRequestsDay;
    }

    public void setResetRequestsDay(double aResetRequestsDay)
    {
        resetRequestsDay = aResetRequestsDay;
    }

    public double getResetTokensMinute()
    {
        return resetTokensMinute;
    }

    public void setResetTokensMinute(double aResetTokensMinute)
    {
        resetTokensMinute = aResetTokensMinute;
    }
}
