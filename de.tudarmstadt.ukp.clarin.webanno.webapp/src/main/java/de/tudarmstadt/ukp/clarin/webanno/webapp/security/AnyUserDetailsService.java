/*******************************************************************************
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.webapp.security;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;

/**
 * Getting User Details, such as Authorities, Login Success,... from the database
 *
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
public class AnyUserDetailsService
    implements UserDetailsService
{
    @Resource(name = "documentRepository")
    private RepositoryService projectRepository;

    @Override
    public UserDetails loadUserByUsername(String aUsername)
        throws UsernameNotFoundException, DataAccessException
    {
        de.tudarmstadt.ukp.clarin.webanno.model.User user = projectRepository.getUser(aUsername);

        List<Authority> authorityList = projectRepository.listAuthorities(user);

        List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
        for (Authority authority : authorityList) {
            authorities.add(new SimpleGrantedAuthority(authority.getRole()));
        }
        return new User(user.getUsername(), user.getPassword(), true, true, true, true, authorities);
    }
}
