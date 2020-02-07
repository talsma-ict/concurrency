/*
 * Copyright 2016-2020 Talsma ICT
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
package nl.talsmasoftware.context.functions;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.DummyContextManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Sjoerd Talsma
 */
public class BooleanSupplierWithContextTest {

    private ExecutorService unawareThreadpool;

    @BeforeEach
    @AfterEach
    public void clearDummyContext() {
        DummyContextManager.clear();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        unawareThreadpool = Executors.newCachedThreadPool();
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        unawareThreadpool.shutdown();
        unawareThreadpool.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void testConstructWithoutSnapshot() {
        try {
            new BooleanSupplierWithContext(null, () -> true);
            fail("Exception expected");
        } catch (RuntimeException expected) {
            assertThat(expected, hasToString(containsString("No context snapshot provided")));
        }
    }

    @Test
    public void testConstructWithoutSnapshotSupplier() {
        try {
            new BooleanSupplierWithContext((Supplier<ContextSnapshot>) null, () -> true, snapshot -> {
            });
            fail("Exception expected");
        } catch (RuntimeException expected) {
            assertThat(expected, hasToString(containsString("No context snapshot supplier provided")));
        }
    }

    @Test
    public void testGetAsBooleanWithSnapshotConsumer() throws ExecutionException, InterruptedException {
        DummyContextManager.setCurrentValue("true");
        final ContextSnapshot[] snapshotHolder = new ContextSnapshot[1];

        BooleanSupplier supplier = new BooleanSupplierWithContext(ContextManagers.createContextSnapshot(), () -> {
            try {
                return Boolean.parseBoolean(DummyContextManager.currentValue().get());
            } finally {
                DummyContextManager.setCurrentValue("false");
            }
        }, s -> snapshotHolder[0] = s);

        Future<Boolean> future = unawareThreadpool.submit(supplier::getAsBoolean);
        assertThat(future.get(), is(true));

        assertThat(DummyContextManager.currentValue(), is(Optional.of("true")));
        try (Context<Void> reactivation = snapshotHolder[0].reactivate()) {
            assertThat(DummyContextManager.currentValue(), is(Optional.of("false")));
        }
        assertThat(DummyContextManager.currentValue(), is(Optional.of("true")));
    }

}
