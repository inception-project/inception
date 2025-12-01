# PDF.js Customizations for INCEpTION

This document describes the customizations applied to the PDF.js viewer for use in INCEpTION.

## Overview

INCEpTION uses PDF.js as a read-only PDF viewer for annotation purposes. The viewer has been customized to disable features that are not needed or would interfere with INCEpTION's annotation workflow.

## Version

- **PDF.js Version**: 5.4.449
- **Build**: 84b34b083

## Files Modified

### 1. viewer.html

The main viewer HTML file has been modified with the following changes:

#### Resource Loading
All inline PDF.js resource loading (`<script>` and `<link>` tags) has been **commented out**. Resources are instead loaded through the Java Wicket resource system in `PdfJsViewerPage.java` to ensure proper resource path handling and ES module support.

#### PDF.js Configuration
PDF.js configuration is done in TypeScript (`pdfanno.ts`) rather than inline scripts to:
- Avoid Content Security Policy (CSP) violations from inline scripts
- Ensure proper initialization order (after library loads, before documents load)
- Keep configuration in version-controlled TypeScript code

See the "JavaScript Configuration" section below for details.

#### UI Element Hiding  
Various UI elements have been hidden by adding `hidden="true"` attributes:

#### Sidebar Panels
- **`viewAttachments`** - Attachments sidebar view button
- **`viewLayers`** - PDF layers sidebar view button
- **`outlineView`** - Document outline panel
- **`attachmentsView`** - Attachments panel
- **`layersView`** - Layers panel

#### Search/Find Functionality
- **`viewFindButton`** - Find button in toolbar
- **`findbar`** - Find toolbar/panel

#### Editor Mode Buttons
All annotation editor modes are hidden as they would conflict with INCEpTION's annotation system:
- **`editorComment`** - Comment editor mode
- **`editorSignature`** - Signature editor mode
- **`editorHighlight`** - Highlight editor mode
- **`editorFreeText`** - Free text editor mode
- **`editorInk`** - Ink/drawing editor mode
- **`editorStamp`** - Stamp editor mode

#### File Operations
- **`printButton`** - Primary toolbar print button
- **`downloadButton`** - Primary toolbar download button
- **`secondaryOpenFile`** - Secondary toolbar open file button
- **`secondaryPrint`** - Secondary toolbar print button
- **`secondaryDownload`** - Secondary toolbar download button

#### View/Navigation Controls
- **`presentationMode`** - Presentation mode button
- **`viewBookmark`** - Bookmark current view link
- **`viewBookmarkSeparator`** - Separator before bookmark button
- **`pageRotateCw`** - Rotate page clockwise button
- **`pageRotateCcw`** - Rotate page counter-clockwise button

#### Advanced View Controls
- **`cursorToolButtons`** - Text selection vs. hand tool toggle
- **`scrollModeButtons`** - Scroll mode selection (page/vertical/horizontal/wrapped)
- **`spreadModeButtons`** - Page spread selection (none/odd/even)

#### Secondary Toolbar Container
The `visibleMediumView` container in the secondary toolbar is hidden, which contains:
- Secondary print and download buttons (also individually hidden)

#### Toolbar Separators
Multiple `horizontalToolbarSeparator` divs are hidden to maintain clean spacing after removing adjacent controls.

### 2. viewer.mjs

This is the main PDF.js viewer JavaScript file (ES module format). No direct modifications are made to this file.

### 3. JavaScript Configuration

**Location**: `pdfanno.ts` in `initPdfAnno()` function

PDF.js is configured in TypeScript before documents are loaded. Configuration is applied in two phases:

#### Phase 1: Before Document Loading
Core `PDFViewerApplicationOptions` settings (applied in `initPdfAnno()`):
- `annotationMode: 0` - Disable PDF annotations rendering
- `defaultUrl: null` - No default document (loaded programmatically)
- `disablePreferences: true` - Disable user preferences storage
- `workerSrc: 'pdf.worker.min.mjs'` - Worker script location (ES module)
- 'wasmUrl', 'wasm/' - WASM  location
- `cMapUrl: 'cmaps/'` - Character maps for complex scripts
- `cMapPacked: true` - Use packed character maps
- `imageResourcesPath: 'image_decoders/'` - Image decoder resources path
- `standardFontDataUrl: 'standard_fonts/'` - Standard font data path
- `isEvalSupported: false` - Disable eval for security
- `enableScripting: false` - Disable PDF JavaScript
- `externalLinkEnabled: false` - Disable external links
- `externalLinkTarget: 0` - No link target (NONE)
- `viewOnLoad: 1` - Load view on startup
- `sidebarViewOnLoad: 0` - Don't auto-open sidebar (prevents unwanted rescaling)

Feature toggles:
- `supportsIntegratedFind: true` - Enable browser find (PDF.js find is disabled)
- `supportsPrinting: false` - Disable printing support

#### Phase 2: After Initialization
Additional link disabling via `PDFViewerApplication.initializedPromise`:
- Override `pdfLinkService.navigateTo()` - Block internal navigation
- Override `pdfLinkService.goToDestination()` - Block destination jumps  
- Set `pdfLinkService.externalLinkEnabled = false` - Ensure external links disabled

This two-phase approach ensures all link interactions are completely blocked.

### 4. Java Integration

**File**: `de.tudarmstadt.ukp.inception.pdfeditor2.view.pdfjs.PdfJsViewerPage`

The Java side is now simplified to only handle resource loading:
- Uses Wicket's built-in ES module support via `JavaScriptReferenceType.MODULE`
- Renders `PdfJsJavaScriptReference` (pdf.mjs) and `PdfJsViewerJavaScriptReference` (viewer.mjs) with `.setType(JavaScriptReferenceType.MODULE)`
- Uses `CssContentHeaderItem` for CSS dependency (`PdfJsViewerJavaCssReference`)
- Renders locale resource reference as `<link rel="resource" type="application/l10n">`

All PDF.js configuration is now in the HTML/JavaScript side (see section 3 above), making it easier to maintain without Java rebuilds.

## Maintenance Notes

When upgrading PDF.js to a new version:

1. Replace the contents of the `pdfjs-web` folder with the new PDF.js build
2. Re-apply all `hidden="true"` attributes listed above to `viewer.html`
3. Verify that `viewer.mjs` (not `viewer.js`) is being loaded
4. Verify configuration options in `pdfanno.ts` are compatible with the new PDF.js version
5. Test that:
   - PDF documents load correctly
   - Navigation (page up/down, zoom) works
   - Sidebar (thumbnails, outline) works when opened via toggle button
   - Document properties dialog works
   - All disabled features remain hidden/non-functional

## Rationale

These customizations ensure that:
- Users cannot modify the PDF document through the viewer
- File operations (open/save/print) are controlled by INCEpTION, not the viewer
- The UI remains clean and focused on viewing for annotation purposes
- Security is maintained by disabling scripting and eval
- The viewer integrates seamlessly with INCEpTION's annotation workflow
