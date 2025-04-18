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
package de.tudarmstadt.ukp.inception.support.text;

public interface KeySanitizer
{
    public static final char SKIP_CHAR = 0;

    char map(char aChar);

    default CharSequence sanitize(CharSequence aKey)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < aKey.length(); i++) {
            char c = map(aKey.charAt(i));
            if (c != 0) {
                sb.append(c);
            }
        }
        return sb;
    }
}
