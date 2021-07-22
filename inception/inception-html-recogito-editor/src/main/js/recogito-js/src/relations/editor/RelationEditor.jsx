import React, { Component } from 'preact/compat';
import { TrashIcon, CheckIcon } from '@recogito/recogito-client-core';
import Autocomplete from '@recogito/recogito-client-core/src/editor/widgets/Autocomplete';

/**
 * Shorthand to get the label (= first tag body value) from the
 * annotation of a relation.
 */
const getContent = relation => {
  const firstTag = relation.annotation.bodies.find(b => b.purpose === 'tagging');
  return firstTag ? firstTag.value : '';
}

/** 
 * A React component for the relationship editor popup. Note that this
 * component is NOT wired into the RelationsLayer directly, but needs
 * to be used separately by the implementing application. We
 * still keep it in the /recogito-relations folder though, so that
 * all code that belongs together stays together.
 */
export default class RelationEditor extends Component {

  constructor(props) {
    super(props);

    this.element = React.createRef();
  }

  componentDidMount() {
    this.setPosition();
  }

  componentDidUpdate() {
    this.setPosition();
  }

  setPosition() {
    if (this.element.current) {
      const el = this.element.current;
      const { midX, midY } = this.props.relation;

      el.style.top = `${midY}px`;
      el.style.left = `${midX}px`;
    }
  }
  
  onSubmit = () => {
    const value = this.element.current.querySelector('input').value;

    const updatedAnnotation = this.props.relation.annotation.clone({
      motivation: 'linking',
      body: [{
        type: 'TextualBody',
        value,
        purpose: 'tagging'
      }]
    });

    const updatedRelation = { ...this.props.relation, annotation: updatedAnnotation };
    
    if (value) {
      // Fire create or update event
      if (this.props.relation.annotation.bodies.length === 0) 
        this.props.onRelationCreated(updatedRelation, this.props.relation);
      else 
        this.props.onRelationUpdated(updatedRelation, this.props.relation);
    } else {
      // Leaving the tag empty and hitting Enter equals cancel
      this.props.onCancel();
    }
  }

  onDelete = () =>
    this.props.onRelationDeleted(this.props.relation);

  render() {
    return(
      <div className="r6o-relation-editor" ref={this.element}>
        <div className="input-wrapper">
          <Autocomplete 
            initialValue={getContent(this.props.relation)}
            placeholder="Tag..."
            onSubmit={this.onSubmit} 
            onCancel={this.props.onCancel}
            vocabulary={this.props.vocabulary || []} />
        </div>

        <div className="buttons">
          <span 
            className="r6o-icon delete"
            onClick={this.onDelete}>
            <TrashIcon width={14} />
          </span>

          <span
            className="r6o-icon ok"
            onClick={this.onSubmit}>
            <CheckIcon width={14} />
          </span>
        </div>
      </div>
    )
  }

}