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

import static org.apache.commons.lang3.Strings.CS;

import java.io.Serializable;
import java.util.Objects;

import org.springframework.security.oauth2.client.registration.ClientRegistration;

public class Realm
    implements Serializable
{
    private static final long serialVersionUID = 8305079363870301352L;

    public static final String REALM_LOCAL_ID = null;

    public static final Realm LOCAL_REALM = new Realm(REALM_LOCAL_ID);

    public static final String REALM_GLOBAL = null;
    public static final String REALM_PROJECT_PREFIX = "project:";
    public static final String REALM_EXTERNAL_PREFIX = "external:";
    public static final String REALM_PREAUTH = "preauth";

    private final String id;
    private final String name;

    public Realm(String aId)
    {
        this(aId, null);
    }

    public Realm(String aId, String aName)
    {
        id = aId;
        name = aName;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        if (name != null) {
            return name;
        }

        if (id == null) {
            return "<LOCAL>";
        }

        return "<" + id + ">";
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
        if (aOne.id == null && aOther.id == null) {
            return 0;
        }

        if (aOne.id == null) {
            return -1;
        }

        if (aOther.id == null) {
            return 1;
        }

        return CS.compare(aOne.getName(), aOther.getName());
    }

    public static Realm forProject(long aProjectId, String aProjectName)
    {
        return new Realm(REALM_PROJECT_PREFIX + aProjectId, "<Project>" + aProjectName);
    }

    public static Realm forExternalOAuth(ClientRegistration aClientRegistration)
    {
        return new Realm(REALM_EXTERNAL_PREFIX + aClientRegistration.getRegistrationId(),
                "<External> " + aClientRegistration.getClientName());
    }

    public static Realm forExternalSaml(String aAuthenticationUri, String aRegistrationId)
    {
        return new Realm(REALM_EXTERNAL_PREFIX + aRegistrationId, "<External> " + aRegistrationId);
    }

    public static Realm local()
    {
        return LOCAL_REALM;
    }

    public static boolean isProjectRealm(String aRealm)
    {
        return CS.startsWith(aRealm, Realm.REALM_PROJECT_PREFIX);
    }
}
