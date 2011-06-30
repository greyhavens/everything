//
// $Id$

package com.threerings.everything.server.credits;

import java.lang.reflect.Type;

import com.google.gson.InstanceCreator;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Turns a Guice {@link Provider} into a Gson {@link InstanceCreator}.
 */
public class ProviderCreator<T>
    implements InstanceCreator<T>
{
    @Override
    public T createInstance (Type arg0)
    {
        return _provider.get();
    }

    public Provider<T> getProvider ()
    {
        return _provider;
    }

    @Inject protected Provider<T> _provider;
}
