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
 * limitations under the L
 */

export interface AnnotationEditorProperties {
    /**
     * JavaScript expression evaluating to the editor factory instance used to
     * construct the editor in the iframe context.
     */
    editorFactory: string;

    /**
     * Stable identifier of the editor factory on the server side. Used e.g. as
     * a key when persisting user preferences for this editor.
     */
    editorFactoryId: string;

    /**
     * Key under which the editor's user preferences are stored.
     */
    userPreferencesKey: string;

    /**
     * URLs of JavaScript files to load into the editor iframe before the
     * editor is initialized.
     */
    scriptSources: ReadonlyArray<string>;

    /**
     * URLs of CSS stylesheets to load into the editor iframe.
     */
    stylesheetSources: ReadonlyArray<string>;

    /**
     * URL the editor uses to send DIAM Ajax requests (annotation actions) back
     * to the server.
     */
    diamAjaxCallbackUrl: string;

    /**
     * WebSocket URL used by the editor for receiving DIAM push updates.
     */
    diamWsUrl: string;

    /**
     * CSRF token sent along with requests to the server.
     */
    csrfToken: string;

    /**
     * If true, the editor suppresses the loading indicator overlay that is
     * normally shown while resources are being loaded.
     */
    loadingIndicatorDisabled: boolean;

    /**
     * CSS selectors identifying elements that delimit document sections (e.g.
     * for navigation or scrolling). Optional; format-specific.
     */
    sectionElements?: ReadonlyArray<string>;

    /**
     * CSS selectors identifying elements whose content must not be modified or
     * annotated (e.g. structural markup that should remain read-only).
     */
    protectedElements?: ReadonlyArray<string>;

    /**
     * Optional JavaScript expression evaluating to a {@link DocumentStructureFactory}
     * for the document's format -- an object whose `create()` method returns a
     * {@link DocumentStructureStrategy}. Evaluated in the editor's iframe context
     * after format scripts have loaded; the editor then calls `factory.create()`
     * once per editor initialization. Mirrors the `editorFactory` convention:
     * the expression always includes the invocation (e.g. `"HtmlDocumentStructure.factory()"`).
     */
    documentStructureFactory?: string;
}
