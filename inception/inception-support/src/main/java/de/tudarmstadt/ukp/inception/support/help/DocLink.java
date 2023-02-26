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
package de.tudarmstadt.ukp.inception.support.help;

import static de.tudarmstadt.ukp.inception.support.help.DocLink.Book.USER_GUIDE;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class DocLink
    extends ExternalLink
{
    private static final long serialVersionUID = 988400097799562423L;

    public static enum Book
    {
        USER_GUIDE("doc/user-guide.html"), //
        ADMIN_GUIDE("doc/admin-guide.html");

        private final String url;

        private Book(String aUrl)
        {
            url = aUrl;
        }
    }

    public DocLink(String aId, String aAnchor)
    {
        this(aId, USER_GUIDE, Model.of(aAnchor));
    }

    public DocLink(String aId, Book aBook, IModel<String> aAnchor)
    {
        super(aId, Model.of(), Model.of("<i class=\"fas fa-question-circle\"></i>"));
        setEscapeModelStrings(false); // SAFE - RENDERING STATIC ICON
        setContextRelative(true);
        setPopupSettings(new PopupSettings().setWindowName("_blank"));
        setDefaultModel(wrap(aAnchor).map(frag -> aBook.url + "#" + frag).orElse(aBook.url));
    }
}
