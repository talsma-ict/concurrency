/*
 * Copyright 2016-2018 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.talsmasoftware.context.locale;

import nl.talsmasoftware.context.threadlocal.AbstractThreadLocalContext;

import java.util.Locale;

/**
 * Package protected context implementation based on {@link AbstractThreadLocalContext}.
 *
 * @author Sjoerd Talsma
 */
final class LocaleContext extends AbstractThreadLocalContext<Locale> {
    /**
     * Instantiates a new context with the specified value.
     * The new context will be made the active context for the current thread.
     *
     * @param newValue The new value to become active in this new context
     *                 (or <code>null</code> to register a new context with 'no value').
     */
    LocaleContext(Locale newValue) {
        super(newValue);
    }

    /**
     * @return The current {@code LocaleContext} or {@code null} if there is no current Locale context.
     */
    static LocaleContext current() {
        return AbstractThreadLocalContext.current(LocaleContext.class);
    }

    /**
     * Unconditionally clears the entire {@link LocaleContext}.
     * This can be useful when returning threads to a threadpool.
     */
    static void clear() {
        AbstractThreadLocalContext.threadLocalInstanceOf(LocaleContext.class).remove();
    }
}
