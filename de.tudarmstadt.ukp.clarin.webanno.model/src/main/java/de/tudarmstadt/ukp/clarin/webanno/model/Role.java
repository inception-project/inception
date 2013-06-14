/*******************************************************************************
 * Copyright 2013
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

//public class Role
//{
//	public static final String ROLE_ADMIN = "ROLE_ADMIN";
//	public static final String ROLE_USER = "ROLE_USER";
//
//	public static List<String> getRoles()
//	{
//		List<String> roles = new ArrayList<String>();
//		roles.add(ROLE_ADMIN);
//		roles.add(ROLE_USER);
//		return roles;
//	}
//}

public enum Role
{
	ROLE_ADMIN, ROLE_USER, ROLE_REMOTE;

	public static Set<Role> getRoles()
	{
		return new HashSet<Role>(Arrays.asList(values()));
	}
}
