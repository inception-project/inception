# Multi-Prediction Set Support Implementation Plan

## Overview

Enable multiple independent sets of predictions to coexist, allowing assistant suggestions and recommender suggestions to be managed, rendered, and interacted with separately.

## Motivation

**Current limitation:** Single `Predictions` object causes ping-pong effect:
- Assistant creates suggestions → recommender suggestions disappear
- Recommenders re-run → assistant suggestions disappear

**UX requirements:**
- Visual distinction between assistant and recommender suggestions
- Bulk operations on assistant suggestions ("Accept all from assistant")
- Independent lifecycle (assistant suggestions don't auto-age/disappear)
- Different rendering (recommender suggestions merged, assistant suggestions individual)

## Architecture Change

### Current Structure
```java
private class RecommendationState {
    private Predictions activePredictions;
    private Predictions incomingPredictions;
}
```

### New Structure
```java
private class RecommendationState {
    private Map<String, Predictions> activePredictions = new ConcurrentHashMap<>();
    private Map<String, Predictions> incomingPredictions = new ConcurrentHashMap<>();
    
    public static final String OWNER_RECOMMENDERS = "recommenders";
    public static final String OWNER_ASSISTANT = "assistant";
}
```

## Core Changes Required

### 1. RecommendationState (~30 lines)

**File:** `inception-recommendation/src/main/java/de/tudarmstadt/ukp/inception/recommendation/service/RecommendationServiceImpl.java`

**Changes:**
```java
private final Map<String, Predictions> activePredictions = new ConcurrentHashMap<>();
private final Map<String, Predictions> incomingPredictions = new ConcurrentHashMap<>();

public Predictions getActivePredictions(String owner) {
    synchronized (this) {
        return activePredictions.get(owner);
    }
}

public void setIncomingPredictions(String owner, Predictions predictions) {
    synchronized (this) {
        incomingPredictions.put(owner, predictions);
    }
}

public boolean switchPredictions(String owner) {
    synchronized (this) {
        var incoming = incomingPredictions.remove(owner);
        if (incoming == null) return false;
        activePredictions.put(owner, incoming);
        return true;
    }
}

public Collection<Predictions> getAllActivePredictions() {
    synchronized (this) {
        return new ArrayList<>(activePredictions.values());
    }
}
```

### 2. RecommendationService Interface (~5 methods)

**File:** `inception-recommendation-api/src/main/java/de/tudarmstadt/ukp/inception/recommendation/api/RecommendationService.java`

**Add/modify:**
```java
// New: Get predictions for specific owner
Predictions getPredictions(User user, Project project, String owner);

// New: Get all predictions
Collection<Predictions> getAllPredictions(User user, Project project);

// Modified: Add owner parameter
void putIncomingPredictions(User user, Project project, String owner, Predictions predictions);

// Modified: Add owner parameter
boolean switchPredictions(User user, Project project, String owner);

// Keep for backward compatibility (defaults to OWNER_RECOMMENDERS)
@Deprecated
Predictions getPredictions(User user, Project project);
```

### 3. RecommendationServiceImpl (~50 lines)

**File:** `inception-recommendation/src/main/java/de/tudarmstadt/ukp/inception/recommendation/service/RecommendationServiceImpl.java`

**Implementation:**
```java
@Override
public Predictions getPredictions(User user, Project project, String owner) {
    var state = getState(user.getUsername(), project);
    synchronized (state) {
        return state.getActivePredictions(owner);
    }
}

@Override
public Collection<Predictions> getAllPredictions(User user, Project project) {
    var state = getState(user.getUsername(), project);
    synchronized (state) {
        return state.getAllActivePredictions();
    }
}

@Override
public void putIncomingPredictions(User user, Project project, String owner, Predictions p) {
    var state = getState(user.getUsername(), project);
    synchronized (state) {
        state.setIncomingPredictions(owner, p);
    }
}

@Override
public boolean switchPredictions(User user, Project project, String owner) {
    var state = getState(user.getUsername(), project);
    synchronized (state) {
        return state.switchPredictions(owner);
    }
}

// Backward compatibility
@Override
@Deprecated
public Predictions getPredictions(User user, Project project) {
    return getPredictions(user, project, RecommendationState.OWNER_RECOMMENDERS);
}
```

### 4. PredictionTask (~10 lines)

**File:** `inception-recommendation/src/main/java/de/tudarmstadt/ukp/inception/recommendation/task/PredictionTask.java`

**Changes:**
```java
// At start of execute()
var activePredictions = recommendationService.getPredictions(
    sessionOwner, project, RecommendationState.OWNER_RECOMMENDERS);

// At end of execute()
recommendationService.putIncomingPredictions(
    sessionOwner, project, RecommendationState.OWNER_RECOMMENDERS, incomingPredictions);
```

### 5. RecommendationRenderer (~20 lines)

**File:** `inception-recommendation/src/main/java/de/tudarmstadt/ukp/inception/recommendation/render/RecommendationRenderer.java`

**Changes:**
```java
@Override
public void render(VDocument aVDoc, RenderRequest aRequest) {
    var allPredictions = recommendationService.getAllPredictions(
        aRequest.getSessionOwner(), aRequest.getProject());
    
    for (var predictions : allPredictions) {
        if (predictions == null) continue;
        
        var suggestions = predictions.getSuggestionsByDocument(
            aRequest.getDocument().getId());
        
        // Render with owner-specific styling
        var owner = predictions.getOwner();
        renderSuggestions(aVDoc, aRequest, suggestions, owner);
    }
}
```

### 6. Assistant Tool (~15 lines)

**File:** `inception-assistant/src/main/java/de/tudarmstadt/ukp/inception/assistant/tool/...`

**Usage:**
```java
// Create predictions for assistant
var assistantPredictions = new Predictions(sessionOwner, project, 
    RecommendationState.OWNER_ASSISTANT);

// Add suggestions
for (var suggestion : generatedSuggestions) {
    assistantPredictions.putSuggestion(suggestion);
}

// Queue with assistant owner
recommendationService.putIncomingPredictions(
    sessionOwner, project, RecommendationState.OWNER_ASSISTANT, assistantPredictions);

// Trigger switch
recommendationService.switchPredictions(
    sessionOwner, project, RecommendationState.OWNER_ASSISTANT);
```

## Owner Tracking

**Solution:** Add owner field to Predictions class

```java
public class Predictions {
    private final String owner;
    
    public Predictions(User sessionOwner, Project project, String owner) {
        // ...
        this.owner = owner;
    }
    
    public String getOwner() {
        return owner;
    }
}
```

## Rendering Implications

### Current: Merged Rendering
All suggestions from all recommenders merged at same position → show as one

### New: Owner-Aware Rendering

**Recommender suggestions:** Merged as before
**Assistant suggestions:** Rendered individually with distinct styling

```java
if (owner.equals(OWNER_ASSISTANT)) {
    // Individual rendering, distinct color/icon
    renderIndividualSuggestion(suggestion, "assistant-suggestion");
} else {
    // Merged rendering (existing logic)
    renderMergedSuggestions(suggestions);
}
```

## Bulk Operations

### Accept All Assistant Suggestions

```java
public void acceptAllAssistantSuggestions(User user, Project project, Document doc) {
    var predictions = recommendationService.getPredictions(
        user, project, RecommendationState.OWNER_ASSISTANT);
    
    if (predictions == null) return;
    
    var suggestions = predictions.getSuggestionsByDocument(doc.getId());
    var cas = getEditorCas();
    
    for (var suggestion : suggestions) {
        recommendationService.acceptSuggestion(user, doc, cas, suggestion);
    }
    
    writeEditorCas(cas);
}
```

### Clear Assistant Suggestions

```java
recommendationService.clearPredictions(
    user, project, RecommendationState.OWNER_ASSISTANT);
```

## Migration Path

### Phase 1: Add Map-based storage (backward compatible)
- Keep deprecated single-Predictions API
- Map to OWNER_RECOMMENDERS internally
- Existing code continues working

### Phase 2: Migrate callers
- Update PredictionTask to use owner parameter
- Update rendering to iterate multiple predictions
- Add assistant support

### Phase 3: Remove deprecated API
- Clean up single-Predictions methods
- All code uses owner-based API

## Testing Considerations

1. **Concurrency:** Multiple owners updating simultaneously
2. **Switching:** Partial switches (only assistant, only recommenders)
3. **Rendering:** Correct styling per owner
4. **Bulk operations:** Accept/reject assistant suggestions
5. **Lifecycle:** Assistant suggestions persist across recommender runs

## Open Questions

1. **Owner constants:** Enum or String? (String for extensibility)
2. **Default owner:** What if owner not specified? (Default to OWNER_RECOMMENDERS)
3. **Owner visibility:** Should UI show owner label on each suggestion?
4. **Additional owners:** Support for user-manual, imports, etc.?

## Estimated Effort

- **Core implementation:** 2-3 days
- **Rendering updates:** 1-2 days
- **Assistant integration:** 1 day
- **Testing:** 2 days
- **Total:** ~1 week

## Risks

- **Breaking changes:** Deprecated API provides migration path
- **Concurrency bugs:** Existing synchronization should extend naturally
- **Rendering complexity:** Owner-aware rendering needs careful testing
- **Performance:** Map iteration adds minimal overhead

## Success Criteria

- ✅ Assistant suggestions persist across recommender runs
- ✅ Recommender suggestions persist across assistant operations
- ✅ Visual distinction between owners in UI
- ✅ Bulk accept/reject works for assistant suggestions
- ✅ No concurrency issues with multiple owners
- ✅ Backward compatibility maintained during migration
