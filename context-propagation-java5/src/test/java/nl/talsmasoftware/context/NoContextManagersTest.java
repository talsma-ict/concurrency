/*
 * Copyright 2016-2019 Talsma ICT
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
package nl.talsmasoftware.context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class NoContextManagersTest {
    private static final String SERVICE_LOCATION = "target/test-classes/META-INF/services/";
    private static final File SERVICE_FILE = new File(SERVICE_LOCATION + ContextManager.class.getName());
    private static final File TMP_SERVICE_FILE = new File(SERVICE_LOCATION + "tmp-ContextManager");

    @Before
    public void avoidContextManagersCache() {
        ContextManagers.useClassLoader(new ClassLoader(Thread.currentThread().getContextClassLoader()) {
        });
        assertThat("Move service file", SERVICE_FILE.renameTo(TMP_SERVICE_FILE), is(true));
    }

    @After
    public void resetDefaultClassLoader() {
        ContextManagers.useClassLoader(null);
        assertThat("Restore service file!", TMP_SERVICE_FILE.renameTo(SERVICE_FILE), is(true));
    }

    @Test
    public void testReactivate_withoutContextManagers() {
        Context<String> ctx1 = new DummyContext("foo");
        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        ctx1.close();

        Context<Void> reactivated = snapshot.reactivate();
        reactivated.close();
    }

    @Test
    public void testCreateSnapshot_withoutContextManagers() {
        ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
        assertThat(snapshot, is(notNullValue()));

        Context<Void> reactivated = snapshot.reactivate();
        assertThat(reactivated, is(notNullValue()));
        reactivated.close();
    }

    @Test
    public void testClearManagedContexts_withoutContextManagers() {
        ContextManagers.clearActiveContexts(); // there should be no exception
    }

}
