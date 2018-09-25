/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.conll.sequencecodec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Sequence codec which treats encodes multi-unit items without a prefix. This means that during
 * decoding, there is no way to tell if two adjacent sequence items with the same label belong to
 * the same unit or not - they are always treated as belonging to the same unit.
 */
public class AdjacentLabelCodec
    implements SequenceCodec
{
    private String markOut = "O";
    
    private final int offset;

    public AdjacentLabelCodec()
    {
        this(1);
    }

    public AdjacentLabelCodec(int aOffset)
    {
        offset = aOffset;
    }

    @Override
    public List<SequenceItem> decode(List<SequenceItem> aEncoded)
    {
        List<SequenceItem> decoded = new ArrayList<>();
        
        Optional<SequenceItem> starter = Optional.empty();
        Optional<SequenceItem> previous = Optional.empty();
        
        Iterator<SequenceItem> i = aEncoded.iterator();
        while (i.hasNext()) {
            SequenceItem current = i.next();
            
            // Sequence items may not overlap
            if (previous.isPresent()) {
                SequenceItem prev = previous.get();
                if (current.getBegin() < prev.getEnd() || prev.getEnd() > current.getEnd()) {
                    throw new IllegalStateException(
                            "Illegal sequence item span " + current + " following " + prev);
                }
            }

            // Check item begin/end
            if (current.getBegin() > current.getEnd()) {
                throw new IllegalStateException("Illegal sequence item span: " + current);
            }

            if (current.getLabel().equals(markOut)) {
                if (starter.isPresent()) {
                    // If there is a starter, there must be a previous
                    assert previous.isPresent();
                    
                    decoded.add(new SequenceItem(starter.get().getBegin(), previous.get().getEnd(),
                            starter.get().getLabel()));
                }
                
                starter = Optional.empty();
            }
            else if (starter.isPresent()) {
                // If there is a starter, there must be a previous
                assert previous.isPresent();
                
                if (starter.get().getLabel().equals(current.getLabel())) {
                    // Nothing else to do here. We just continue the already started span.
                }
                else {
                    // Commit current span and start a new one
                    decoded.add(new SequenceItem(starter.get().getBegin(), previous.get().getEnd(),
                            starter.get().getLabel()));
                    starter = Optional.of(current);
                }
            }
            else {
                starter = Optional.of(current);
            }
            
            previous = Optional.of(current);
        }
        
        // Commit active span at the end of the sequence
        if (starter.isPresent()) {
            decoded.add(new SequenceItem(starter.get().getBegin(), previous.get().getEnd(),
                    starter.get().getLabel()));
        }
        
        return decoded;
    }

    @Override
    public List<SequenceItem> encode(List<SequenceItem> aDecoded, int aLength)
    {
        List<SequenceItem> encoded = new ArrayList<>();
        
        int idx = offset;
        
        Iterator<SequenceItem> i = aDecoded.iterator();
        while (i.hasNext()) {
            SequenceItem current = i.next();
            
            // Check overlap with already seen items
            if (idx > current.getBegin()) {
                throw new IllegalStateException("Illegal sequence item span: " + current);
            }

            // Check item begin/end
            if (current.getBegin() > current.getEnd()) {
                throw new IllegalStateException("Illegal sequence item span: " + current);
            }

            // Generate "outside" items
            while (idx < current.getBegin()) {
                encoded.add(new SequenceItem(idx, idx, markOut));
                idx++;
            }
            
            // Generate "inside" items
            while (idx <= current.getEnd()) {
                encoded.add(new SequenceItem(idx, idx, current.getLabel()));
                idx++;
            }
        }
        
        // Generate "outside" items until the final length is reached
        while (idx < aLength + offset) {
            encoded.add(new SequenceItem(idx, idx, markOut));
            idx++;
        }
        
        return encoded;
    }
}
