<!--
  Licensed to the Technische Universität Darmstadt under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The Technische Universität Darmstadt
  licenses this file to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
# INCEpTION Bootstrap

This folder produces the bundled Bootstrap stylesheet shipped to the application.

## Files

- `bootstrap.scss` — the only build entry point. Compiles to the final CSS bundle.
- `_inception-bootstrap-config.scss` — facade that `@forward`s Bootstrap with
  INCEpTION's variable overrides applied. This is the single place to configure
  Bootstrap; do not override Bootstrap variables anywhere else.
- `_inception-*.scss` — INCEpTION's own styling layered on top of Bootstrap
  (dashboard, navbar, action bar, feature editors, tables, cards, etc.).
- `_shim-*.scss` — compatibility patches that adapt third-party widgets
  (Kendo, Wicket, Bootstrap 3 markup, Bootstrap Select, jQuery UI, …) to
  Bootstrap 5 conventions.

## Conventions

- All partials are prefixed with `_` so Sass never emits them as standalone CSS.
- Partials consume Bootstrap variables via `@use 'inception-bootstrap-config' as *;`
  rather than importing Bootstrap directly, so they see the configured values.
- New Bootstrap variable overrides go into `_inception-bootstrap-config.scss`'s
  `@forward ... with (...)` block, never into individual partials.
