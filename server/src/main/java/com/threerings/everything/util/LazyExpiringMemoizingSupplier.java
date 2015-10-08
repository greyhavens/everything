//
// The Everything Game - slot machine plus encyclopedia equals educational fun!
// Copyright © 2009-2015 Grey Havens, LLC

package com.threerings.everything.util;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;

/**
 * A Supplier that provides an instance of T, caching it for some duration...
 * When the duration has expired, the next thread to call get() gets a new instance
 * from the delegate, but while it is computing it other threads use the old instance.
 */
public class LazyExpiringMemoizingSupplier<T>
    implements Supplier<T>
{
    public LazyExpiringMemoizingSupplier (Supplier<T> delegate, long duration, TimeUnit unit)
    {
        _delegate = Preconditions.checkNotNull(delegate);
        Preconditions.checkArgument(duration > 0);
        _durationNanos = unit.toNanos(duration);
    }

    // from interface Supplier
    public T get ()
    {
        synchronized (_sync) {
            switch (_state) {
            case UNINITIALIZED: // on the first call, we block other threads and generate the value
                return setValue(_delegate.get());

            case CACHED: // check the expiration and return the value if it's good
            default:
                if (System.nanoTime() - _expirationNanos < 0) {
                    return _value;
                }
                _state = State.RECOMPUTING;
                break; // we are going to recompute on this thread

            case RECOMPUTING: // another thread is regenerating, we return the old value
                return _value;
            }
        }

        // generate it outside of the sync block, allowing other callers to access the old value
        T value;
        try {
            value = _delegate.get();
        } catch (RuntimeException re) {
            synchronized (_sync) {
                _state = State.CACHED; // un-hork that we've set _state to RECOMPUTING
            }
            throw re;
        }
        // then we synchronize to set it
        synchronized (_sync) {
            return setValue(value);
        }
    }

    /** Internal helper. */
    protected T setValue (T value)
    {
        _value = value;
        _state = State.CACHED;
        _expirationNanos = System.nanoTime() + _durationNanos;
        return value;
    }

    protected static enum State
    {
        UNINITIALIZED, // no value exists yet
        CACHED,        // a value is generated
        RECOMPUTING;   // the value is old, use it while one thread is regenerating
    }

    final protected Supplier<T> _delegate;
    final protected long _durationNanos;
    transient protected T _value;
    transient protected long _expirationNanos;
    transient protected State _state = State.UNINITIALIZED;
    transient protected Object _sync = new Object();
}
