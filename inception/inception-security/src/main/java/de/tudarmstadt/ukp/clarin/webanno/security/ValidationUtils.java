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

import static de.tudarmstadt.ukp.inception.support.text.TextUtils.containsAnyCharacterMatching;
import static org.apache.commons.lang3.StringUtils.containsAny;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.startsWithAny;

import de.tudarmstadt.ukp.inception.support.text.TextUtils;

public final class ValidationUtils
{
    public static final String FILESYSTEM_ILLEGAL_PREFIX_CHARACTERS = "-.";
    public static final String FILESYSTEM_RESERVED_CHARACTERS = "<>:\"/\\|?*\0";

    // May be a bit too restrictive to exclude all of these...
    // private static final String SHELL_SPECIAL_CHARACTERS = "[]()^#%&$!@:+={}'~`";
    public static final String RELAXED_SHELL_SPECIAL_CHARACTERS = "#%&{}$!:@+'`=";

    private ValidationUtils()
    {
        // No instances
    }

    public static boolean isValidFilename(String aFilename)
    {
        if (isEmpty(aFilename)) {
            return false;
        }

        if (startsWithAny(aFilename, FILESYSTEM_ILLEGAL_PREFIX_CHARACTERS)) {
            return false;
        }

        if (containsAny(aFilename, FILESYSTEM_RESERVED_CHARACTERS)) {
            return false;
        }

        if (containsAnyCharacterMatching(aFilename, TextUtils::isControlCharacter)) {
            return false;
        }

        return true;
    }
}
