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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

class LegacyEmptyPasswordCheckerTest
{
    @Test
    void thatNullIsDetectedAsEmpty()
    {
        assertThat(LegacyEmptyPasswordChecker.isEmptyOrNull(null)).isTrue();
    }

    @Test
    void thatLiteralEmptyStringIsDetectedAsEmpty()
    {
        assertThat(LegacyEmptyPasswordChecker.isEmptyOrNull("")).isTrue();
    }

    @Test
    void thatPrefixedBcryptEncodingOfEmptyStringIsDetectedAsEmpty()
    {
        // This is how the production DelegatingPasswordEncoder stores fresh passwords.
        var encoded = newDelegatingEncoder().encode("");
        assertThat(encoded).startsWith("{bcrypt}");
        assertThat(LegacyEmptyPasswordChecker.isEmptyOrNull(encoded)).isTrue();
    }

    @Test
    void thatLegacyStandardEncodingOfEmptyStringIsDetectedAsEmpty()
    {
        // Legacy passwords were encoded with StandardPasswordEncoder without an {id} prefix.
        var encoded = new StandardPasswordEncoder().encode("");
        assertThat(LegacyEmptyPasswordChecker.isEmptyOrNull(encoded)).isTrue();
    }

    @Test
    void thatBareBcryptEncodingOfNonEmptyPasswordIsNotDetectedAsEmpty()
    {
        var encoded = new BCryptPasswordEncoder().encode("secret");
        assertThat(LegacyEmptyPasswordChecker.isEmptyOrNull(encoded)).isFalse();
    }

    @Test
    void thatPrefixedBcryptEncodingOfNonEmptyPasswordIsNotDetectedAsEmpty()
    {
        var encoded = newDelegatingEncoder().encode("secret");
        assertThat(encoded).startsWith("{bcrypt}");
        assertThat(LegacyEmptyPasswordChecker.isEmptyOrNull(encoded)).isFalse();
    }

    @Test
    void thatLegacyStandardEncodingOfNonEmptyPasswordIsNotDetectedAsEmpty()
    {
        var encoded = new StandardPasswordEncoder().encode("secret");
        assertThat(LegacyEmptyPasswordChecker.isEmptyOrNull(encoded)).isFalse();
    }

    private static PasswordEncoder newDelegatingEncoder()
    {
        var encoderForEncoding = "bcrypt";
        var encoders = new HashMap<String, PasswordEncoder>();
        encoders.put(encoderForEncoding, new BCryptPasswordEncoder());
        var delegating = new DelegatingPasswordEncoder(encoderForEncoding, encoders);
        delegating.setDefaultPasswordEncoderForMatches(new StandardPasswordEncoder());
        return delegating;
    }
}
