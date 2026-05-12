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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.function.BiPredicate;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

/**
 * Since Spring 7, AbstractValidatingPasswordEncoder.matches is final and fails directly if the raw
 * password is empty. But we may still have legacy encoded empty passwords in the DB which we need
 * to detect. This checker routes through a DelegatingPasswordEncoder mirroring the production
 * setup, but bypasses the empty-raw-password rejection on each underlying encoder.
 */

class LegacyEmptyPasswordChecker
    extends BCryptPasswordEncoder
{
    private static final DelegatingPasswordEncoder INSTANCE = createEncoder();
    private static final Method DELEGATING_MATCHES_NON_NULL = lookupMatchesNonNull(
            DelegatingPasswordEncoder.class);

    public static boolean isEmptyOrNull(String aEncodedPassword)
    {
        if (aEncodedPassword == null || aEncodedPassword.isEmpty()) {
            return true;
        }

        try {
            return (boolean) DELEGATING_MATCHES_NON_NULL.invoke(INSTANCE, "", aEncodedPassword);
        }
        catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static Method lookupMatchesNonNull(Class<?> aClass)
    {
        try {
            // The protected matchesNonNull method is declared on AbstractValidatingPasswordEncoder.
            var method = aClass.getSuperclass().getDeclaredMethod("matchesNonNull", String.class,
                    String.class);
            method.setAccessible(true);
            return method;
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Spring Security API changed: matchesNonNull not found on " + aClass, e);
        }
    }

    boolean matchesAllowingEmpty(CharSequence aRawPassword, String aEncodedPassword)
    {
        if (aEncodedPassword == null || aEncodedPassword.isEmpty()) {
            return false;
        }
        return matchesNonNull(aRawPassword == null ? "" : aRawPassword.toString(),
                aEncodedPassword);
    }

    private static DelegatingPasswordEncoder createEncoder()
    {
        // Mirror the production DelegatingPasswordEncoder so we recognize {bcrypt}-prefixed
        // passwords as well as legacy prefix-less hashes from the old StandardPasswordEncoder.
        // The matches method on AbstractValidatingPasswordEncoder is final and rejects empty
        // raw passwords in Spring 7, so each underlying encoder is wrapped to bypass that.
        var bcrypt = new LegacyEmptyPasswordChecker();
        var encoders = new HashMap<String, PasswordEncoder>();
        encoders.put("bcrypt", new EmptyAwarePasswordEncoder(bcrypt::matchesAllowingEmpty));
        var delegating = new DelegatingPasswordEncoder("bcrypt", encoders);
        delegating.setDefaultPasswordEncoderForMatches(new EmptyAwareStandardPasswordEncoder());
        return delegating;
    }

    private static final class EmptyAwarePasswordEncoder
        implements PasswordEncoder
    {
        private final BiPredicate<CharSequence, String> matcher;

        EmptyAwarePasswordEncoder(BiPredicate<CharSequence, String> aMatcher)
        {
            matcher = aMatcher;
        }

        @Override
        public String encode(CharSequence aRawPassword)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean matches(CharSequence aRawPassword, String aEncodedPassword)
        {
            if (aEncodedPassword == null || aEncodedPassword.isEmpty()) {
                return false;
            }
            return matcher.test(aRawPassword == null ? "" : aRawPassword, aEncodedPassword);
        }
    }

    /**
     * Calls StandardPasswordEncoder.matchesNonNull reflectively, since the class is final and the
     * protected matchesNonNull method is the only way to bypass Spring 7's empty-raw-password
     * rejection.
     */
    private static final class EmptyAwareStandardPasswordEncoder
        implements PasswordEncoder
    {
        @SuppressWarnings("deprecation")
        private final StandardPasswordEncoder delegate = new StandardPasswordEncoder();
        private final Method matchesNonNull;

        EmptyAwareStandardPasswordEncoder()
        {
            try {
                matchesNonNull = StandardPasswordEncoder.class.getSuperclass()
                        .getDeclaredMethod("matchesNonNull", String.class, String.class);
                matchesNonNull.setAccessible(true);
            }
            catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                        "Spring Security API changed: matchesNonNull not found", e);
            }
        }

        @Override
        public String encode(CharSequence aRawPassword)
        {
            return delegate.encode(aRawPassword);
        }

        @Override
        public boolean matches(CharSequence aRawPassword, String aEncodedPassword)
        {
            if (aEncodedPassword == null || aEncodedPassword.isEmpty()) {
                return false;
            }
            try {
                return (boolean) matchesNonNull.invoke(delegate,
                        aRawPassword == null ? "" : aRawPassword.toString(), aEncodedPassword);
            }
            catch (ReflectiveOperationException e) {
                return false;
            }
        }
    }
}
