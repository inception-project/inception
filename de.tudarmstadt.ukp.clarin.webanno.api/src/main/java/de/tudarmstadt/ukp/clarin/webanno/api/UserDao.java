/*******************************************************************************
 * Copyright 2013
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
package de.tudarmstadt.ukp.clarin.webanno.api;

import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.User;


public interface UserDao
{
	void create(User aModel);

	User update(User aModel);

	boolean exists(final String aUsername);

	int delete(String aUsername);

	void delete(User aUser);

	User get(String aUsername);

	List<User> list();

	List<User> list(User aFilter);

	List<User> list(User aFilter, int aOffset, int aCount);
}
