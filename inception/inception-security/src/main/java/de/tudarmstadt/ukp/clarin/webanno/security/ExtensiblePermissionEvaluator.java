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

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

public class ExtensiblePermissionEvaluator
    implements PermissionEvaluator
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final PermissionExtensionPoint permissionExtensionPoint;

    public ExtensiblePermissionEvaluator(PermissionExtensionPoint aPermissionExtensionPoint)
    {
        permissionExtensionPoint = aPermissionExtensionPoint;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public boolean hasPermission(Authentication aAuthentication, Object aTargetDomainObject,
            Object aPermission)
    {
        log.trace("Permission check for: {}, {}, {}", aAuthentication.getName(),
                aTargetDomainObject, aPermission);

        return permissionExtensionPoint.getExtensions(aTargetDomainObject).stream()
                .allMatch($ -> ((PermissionExtension) $).hasPermission(aAuthentication,
                        aTargetDomainObject, aPermission));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public boolean hasPermission(Authentication aAuthentication, Serializable aTargetId,
            String aTargetType, Object aPermission)
    {
        log.trace("Permission check for: {}, {}, {}, {}", aAuthentication.getName(), aTargetId,
                aTargetType, aPermission);

        return permissionExtensionPoint.getExtensions(aTargetType).stream()
                .allMatch($ -> ((PermissionExtension) $).hasPermission(aAuthentication, aTargetId,
                        aTargetType, aPermission));
    }
}
