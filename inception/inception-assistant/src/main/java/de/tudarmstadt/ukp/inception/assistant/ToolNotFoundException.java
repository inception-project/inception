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
package de.tudarmstadt.ukp.inception.assistant;

public class ToolNotFoundException
    extends Exception
{
    private static final long serialVersionUID = -5362071917531348869L;

    private final String function;

    public ToolNotFoundException(String aFunction)
    {
        function = aFunction;
    }

    public String getFunction()
    {
        return function;
    }

    @Override
    public String getMessage()
    {
        return "There is no tool named `" + function + "`.";
    }
}
