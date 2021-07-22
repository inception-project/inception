/**
 * 'Deflates' the HTML contained in the given parent node.
 * Deflation will completely drop empty text nodes, and replace
 * multiple spaces, tabs, newlines with a single space. This way,
 * character offsets in the markup will more closely represent
 * character offsets experienced in the browser.
 */
export const deflateHTML = parent => {
  deflateNodeList([ parent ]);
  return parent;
}

const deflateNodeList = parents => {

  // Deflates the immediate children of one parent (but not children of children)
  const deflateOne = parent => {
    return Array.from(parent.childNodes).reduce((compacted, node) => {
      if (node.nodeType === Node.TEXT_NODE) {
        if (node.textContent.trim().length > 0) {
          // Text node - trim
          const trimmed = node.textContent.replace(/\s\s+/g, ' ');
          return [...compacted, document.createTextNode(trimmed)];
        } else {
          // Empty text node - discard
          return compacted;
        }
      } else {
        return [...compacted, node];
      }
    }, []);
  }

  // Replace original children with deflated
  parents.forEach(parent => {
    const deflatedChildren = deflateOne(parent);

    // This would be easier, but breaks on IE11
    // parent.innerHTML = '';
    while (parent.firstChild)
      parent.removeChild(parent.lastChild);

    deflatedChildren.forEach(node => parent.appendChild(node));
  });

  // Then, get all children that have more children
  const childrenWithChildren = parents.reduce((childrenWithChildren, parent) => {
    return childrenWithChildren.concat(Array.from(parent.childNodes).filter(c => c.firstChild));
  }, []);

  // Recursion
  if (childrenWithChildren.length > 0)
    deflateNodeList(childrenWithChildren);

}

