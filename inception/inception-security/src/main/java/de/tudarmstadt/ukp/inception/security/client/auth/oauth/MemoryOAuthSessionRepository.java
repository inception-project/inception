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
package de.tudarmstadt.ukp.inception.security.client.auth.oauth;

import org.apache.commons.lang3.function.FailableFunction;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;

public class MemoryOAuthSessionRepository<T>
{
    private final Cache<T, OAuthSession> sessions;

    public MemoryOAuthSessionRepository()
    {
        sessions = Caffeine.newBuilder() //
                .expireAfter(new OAuthSessionExpiry<T>()) //
                .build();
    }

    public OAuthSession get(T aKey, FailableFunction<T, OAuthSession, Throwable> aSessionCreator)
    {
        return sessions.get(aKey, _key -> {
            try {
                return aSessionCreator.apply(aKey);
            }
            catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public void clear(T aKey)
    {
        sessions.invalidate(aKey);
    }

    private final class OAuthSessionExpiry<K>
        implements Expiry<K, OAuthSession>
    {
        @Override
        public long expireAfterCreate(K aKey, OAuthSession aValue, long aCurrentTime)
        {
            if (aValue.getRefreshTokenExpiresIn().negated().isNegative()) {
                return aValue.getRefreshTokenExpiresIn().toNanos();
            }
            return aValue.getAccessTokenExpiresIn().toNanos();
        }

        @Override
        public long expireAfterUpdate(K aKey, OAuthSession aValue, long aCurrentTime,
                long aCurrentDuration)
        {
            if (aValue.getRefreshTokenExpiresIn().negated().isNegative()) {
                return aValue.getRefreshTokenExpiresIn().toNanos();
            }
            return aValue.getAccessTokenExpiresIn().toNanos();
        }

        @Override
        public long expireAfterRead(K aKey, OAuthSession aValue, long aCurrentTime,
                long aCurrentDuration)
        {
            return aCurrentDuration;
        }
    }
}
