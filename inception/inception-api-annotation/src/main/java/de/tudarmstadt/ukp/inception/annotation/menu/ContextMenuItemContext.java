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
package de.tudarmstadt.ukp.inception.annotation.menu;

import de.tudarmstadt.ukp.inception.diam.model.DiamContext;
import de.tudarmstadt.ukp.inception.editor.ContextMenuLookup;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

/**
 * Identifies the annotation ({@link #vid}) that a context menu was opened on, together with the
 * {@link #context editor} it was opened in. Both the {@code context} and the
 * {@code contextMenuLookup} belong to the editor whose {@code DiamAjaxBehavior} received the
 * request, so context-menu items act on <i>that</i> editor rather than unconditionally on the main
 * editor (cf. #6146). For the main editor the {@code context} is simply its
 * {@code AnnotationPageBase}, reproducing the historic behavior.
 */
public record ContextMenuItemContext(VID vid, DiamContext context,
        ContextMenuLookup contextMenuLookup)
{

}
