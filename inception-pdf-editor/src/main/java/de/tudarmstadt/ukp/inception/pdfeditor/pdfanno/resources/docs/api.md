# API

## Annotation API
`PDFAnno` provides annotation API.

### Span
```
var span = new SpanAnnotation({
  page: 1,
  position:
 [["139.03536681054345","60.237086766202694","155.97302418023767","14.366197183098592"]],
  label: 'orange',
  text: 'Ready?',
  id: 1
});
window.add(span);
window.delete(span);
```

### Relation
```
var rel = new RelationAnnotation({
  dir: 'link',
  ids: ["1","2"],
  label: 'sample'
});
window.add(rel);
window.delete(rel);
```

### Rectangle
```
var rect = new RectAnnotation({
  page:1,
  position:["9.24324324324326","435.94054054054055","235.7027027027027","44.65945945945946"],
  label: 'rect-label',
  id: 2
});
window.add(rect);
window.delete(rect);
```

### Read from TOML or JSON
```
var toml = `

version = 0.2

[1]
type = "span"
page = 1
position = [["139.03536681054345","60.237086766202694","155.97302418023767","14.366197183098592"]]
label = "orange"
text = "Ready?"
`;

var anno = readTOML(toml);
var annoObj = window.addAll(anno);
window.delete(annoObj["1"]);

// delete all annotations
window.clear();
```
