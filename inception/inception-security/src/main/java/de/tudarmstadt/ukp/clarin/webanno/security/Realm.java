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

import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.REALM_EXTERNAL_PREFIX;
import static de.tudarmstadt.ukp.clarin.webanno.security.UserDao.REALM_PROJECT_PREFIX;
import static org.apache.commons.lang3.StringUtils.startsWith;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

public class Realm
    implements Serializable
{
    private static final long serialVersionUID = 8305079363870301352L;

    public static final String REALM_LOCAL_ID = null;

    private static final Realm LOCAL_REALM = new Realm(REALM_LOCAL_ID);

    private final String id;
    private final String name;

    public Realm(String aId)
    {
        this(aId, null);
    }

    public Realm(String aId, String aName)
    {
        id = aId;

        if (aName != null) {
            name = aName;
        }
        else {
            if (aId == null) {
                name = "<LOCAL>";
            }
            else {
                name = "<" + aId + ">";
            }
        }
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof Realm)) {
            return false;
        }
        Realm castOther = (Realm) other;
        return Objects.equals(id, castOther.id);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }

    public static int compareRealms(Realm aOne, Realm aOther)
    {
        if (aOne.getId() == null && aOther.getId() == null) {
            return 0;
        }

        if (aOne.getId() == null) {
            return -1;
        }

        if (aOther.getId() == null) {
            return 1;
        }

        return StringUtils.compare(aOne.getName(), aOther.getName());
    }

    public static Realm forProject(long aProjectId, String aProjectName)
    {
        return new Realm(REALM_PROJECT_PREFIX + aProjectId, aProjectName);
    }

    public static Realm forExternalOAuth(ClientRegistration aClientRegistration)
    {
        return new Realm(REALM_EXTERNAL_PREFIX + aClientRegistration.getRegistrationId(),
                aClientRegistration.getClientName());
    }

    public static Realm local()
    {
        return LOCAL_REALM;
    }

    public static boolean isProjectRealm(String aRealm)
    {
        return startsWith(aRealm, REALM_PROJECT_PREFIX);
    }
}
