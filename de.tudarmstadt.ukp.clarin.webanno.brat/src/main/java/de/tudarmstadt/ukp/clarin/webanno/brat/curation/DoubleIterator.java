/*******************************************************************************
 * Copyright (c) 2004-2009 Richard Eckart de Castilho.
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
 * 
 * Contributors:
 *     Richard Eckart de Castilho - initial API and implementation
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.curation;

import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.cas.text.AnnotationFS;

public
class DoubleIterator<A extends AnnotationFS, B extends AnnotationFS>
{
    static final private Log _log = LogFactory.getLog(DoubleIterator.class);

	/** The A/B lists */
	private final List<A> 			_la;
	private final List<B> 			_lb;

	/** A values we do not want to see again (after a rewind). */
	private final boolean			_ignorea[];

	/** List iterators for the A/B lists */
	private final ListIterator<A>	_ia;
	private final ListIterator<B>	_ib;

	/** Indices of _cura/_curb within the lists */
	private int		_na;
	private int		_nb;

	/** Maximum A/B index within the lists */
	private final int		_maxa;
	private final int		_maxb;

	/** Current A/B item */
	private A		_cura;
	private B		_curb;

	private int		_last_b_step_na;
	private boolean _done;

	private int 	_stepCount;

	public
	DoubleIterator(
			final List<A> la,
			final List<B> lb)
	{
		_done = !((la.size() > 0) && (lb.size() > 0));

		// Intialize A
		_la   = la;
		_maxa = _la.size()-1; 			// Up until here and no further
		_ia   = _la.listIterator();		// Where we are now
		_na   = _ia.nextIndex();		// Index of _cura within _la
		_cura = _ia.next();				// The current object.
		_ignorea = new boolean[_la.size()];

		// Initialize B
		_lb       = lb;
		_maxb     = _lb.size()-1;
		_ib       = _lb.listIterator();
		_nb       = _ib.nextIndex();
		_curb     = _ib.next();

		_last_b_step_na = _na;
	}

	public
	int getStepCount()
	{
		return _stepCount;
	}

	public
	A getA()
	{
		return _cura;
	}

	public
	B getB()
	{
		return _curb;
	}

	public
	void ignoraA()
	{
		_ignorea[_na] = true;
	}

	public
	boolean hasNext()
	{
		return !_done;
	}

	public
	void step()
	{
		if (_done) {
			throw new NoSuchElementException();
		}

		// Peek ahead in the A list.
		A nexta = null;
		if (_na < _maxa) {
			nexta = _ia.next();
			_ia.previous();
		}

		final boolean nexta_starts_before_curb_ends = (nexta != null) && (nexta.getBegin() <= _curb.getEnd());
		final boolean cura_ends_before_or_with_curb = _cura.getEnd() <= _curb.getEnd();

		if (_log.isTraceEnabled()) {
			_log.trace("---");
			_log.trace("   A                            : "+_na+"/"+_maxa+" "+_cura+" peek: "+nexta);
			_log.trace("   B                            : "+_nb+"/"+_maxb+" "+_curb);
			_log.trace("   nexta starts before curb ends: "+nexta_starts_before_curb_ends);
			_log.trace("   cura ends before or with curb: "+cura_ends_before_or_with_curb);
		}

		// Which one to step up A or B?
		if (
				nexta_starts_before_curb_ends || cura_ends_before_or_with_curb
		) {
			// Can A be stepped up any more?
			if (_na < _maxa) {
				stepA();
			// if not, try stepping up B
			} else if (_nb < _maxb) {
				stepB();
			// if both are at the end, bail out
			} else {
				_done = true;
			}
		} else {
			// Can B be stepped up any more?
			if (_nb < _maxb) {
				stepB();
			// if not, try stepping up A
			} else if (_na < _maxa) {
				stepA();
			// if both are at the end, bail out
			} else {
				_done = true;
			}
		}

		if (_log.isTraceEnabled() && _done) {
			_log.trace("   -> Both lists at the end.");
		}
	}

	private
	void stepA()
	{
		_stepCount++;
		_na   = _ia.nextIndex();
		_cura = _ia.next();

		if (_log.isTraceEnabled()) {
			_log.trace("   -> A: "+_na+"/"+_maxa+" "+_cura);
		}
	}

	private
	void stepBackA()
	{
		_na   = _ia.previousIndex();
		_cura = _ia.previous();

		if (_log.isTraceEnabled()) {
			_log.trace("   <- A: "+_na+"/"+_maxa+" "+_cura);
		}
	}

	private
	void stepB()
	{
		_stepCount++;
		_nb   = _ib.nextIndex();
		_curb = _ib.next();

		if (_log.isTraceEnabled()) {
			_log.trace("   -> B: "+_nb+"/"+_maxb+" "+_curb);
		}

		if (_curb.getBegin() < _cura.getEnd()) {
			// Rewind A to the point where it was when we last stepped
			// up B.
			rewindA();
		} else {
			_last_b_step_na = _na;
		}
	}

	private
	void rewindA()
	{
		if (_log.isTraceEnabled()) {
			_log.trace("   <- rewinding A");
		}

		// Seek back to the first segment that does not overlap
		// with curb and at most until the last b step we made.
		boolean steppedBack = false;
		while (
				(_na > _last_b_step_na) &&
				(_cura.getEnd() > _curb.getBegin())
		) {
			stepBackA();
			steppedBack = true;
		}

		// Correct pointer
		if (steppedBack) {
			// Make sure the next peek really peeks ahead.
			_na   = _ia.nextIndex();
			_cura = _ia.next();
		}

		// Skip over the A's we do not want to see again.
		while (_ignorea[_na] && (_na < _maxa)) {
			stepA();
		}

		// If we skipped some As those we skip will always be skipped, so we
		// can as well update the _last_b_step_na so we don't have to skip them
		// every time.
		_last_b_step_na = _na;
	}
}
