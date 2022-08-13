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
package de.tudarmstadt.ukp.clarin.webanno.security;

import static java.lang.Character.isWhitespace;
import static org.apache.commons.lang3.StringUtils.containsAny;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class NameUtil
{
    public static final String WEBANNO_ILLEGAL_CHARACTERS = "^/\\&*?+$![] ";

    public static final String BAD_FOR_FILENAMES = "#%&{}\\<>*?/ $!'\":@+`|=";

    /**
     * Check if the name is valid, special characters are not allowed as a project/user name as it
     * will conflict with file naming system
     * 
     * @param aName
     *            a name.
     * @return if the name is valid.
     */
    public static boolean isNameValidUserName(String aName)
    {
        if (aName == null) {
            return false;
        }

        if (containsAny(aName, WEBANNO_ILLEGAL_CHARACTERS)) {
            return false;
        }

        var iter = new StringCharacterIterator(aName);
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (isWhitespace(c) || isControlCharacter(c)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isControlCharacter(char aChar)
    {
        return aChar < 32 || aChar == 127 || (aChar >= 128 && aChar <= 159);
    }
}
