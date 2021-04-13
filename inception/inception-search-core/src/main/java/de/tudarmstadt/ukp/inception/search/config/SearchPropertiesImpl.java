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
package de.tudarmstadt.ukp.inception.search.config;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.parseLong;

import java.util.Arrays;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("search")
public class SearchPropertiesImpl
    implements SearchProperties
{
    public static final String ALL = "all";

    long[] pagesSizes = { 10, 20, 50, 100, 500, 1000 };

    @Override
    public long[] getPageSizes()
    {
        return pagesSizes;
    }

    @Override
    public void setPageSizes(String[] aPageSizes)
    {
        pagesSizes = Arrays.stream(aPageSizes)
                .mapToLong(value -> ALL.equals(value) ? MAX_VALUE : parseLong(value)).toArray();
    }
}
