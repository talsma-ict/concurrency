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
package nl.talsmasoftware.context.futures;

import nl.talsmasoftware.context.Context;
import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.DummyContextManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Sjoerd Talsma
 */
public class ContextAwareCompletableFutureTest {
    private static final DummyContextManager manager = new DummyContextManager();
    private static final ExecutorService contextUnawareThreadpool = Executors.newCachedThreadPool();

    @Before
    @After
    public void clearDummyContext() {
        DummyContextManager.clear();
    }

    @Test
    public void testDefaultConstructor() throws ExecutionException, InterruptedException {
        Context<String> ctx = manager.initializeNewContext("foo");
        CompletableFuture<String> future1 = new ContextAwareCompletableFuture<>(); // should have a new snapshot with foo
        ctx.close();

        CompletableFuture<String> future2 = future1.thenApply(value -> manager.getActiveContext().getValue() + value);
        assertThat(manager.getActiveContext(), is(nullValue()));
        assertThat(future1.isDone(), is(false));
        assertThat(future2.isDone(), is(false));

        assertThat(future1.complete("bar"), is(true));
        assertThat(future1.isDone(), is(true));
        assertThat(future2.isDone(), is(true));
        assertThat(future2.get(), is("foobar"));
    }

    @Test
    public void testSupplyAsync() throws ExecutionException, InterruptedException {
        String expectedValue = "Vincent Vega";
        try (Context<String> ctx = manager.initializeNewContext(expectedValue)) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .supplyAsync(DummyContextManager::currentValue);
            assertThat(future.get().get(), is(expectedValue));
        }
    }

    @Test
    public void testSupplyAsync_executor() throws ExecutionException, InterruptedException {
        String expectedValue = "Marcellus Wallace";
        try (Context<String> ctx = manager.initializeNewContext(expectedValue)) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .supplyAsync(DummyContextManager::currentValue, contextUnawareThreadpool);
            assertThat(future.get().get(), is(expectedValue));
        }

    }

    @Test
    public void testSupplyAsync_executor_snapshot() throws ExecutionException, InterruptedException {
        String expectedValue = "Vincent Vega";
        try (Context<String> ctx = manager.initializeNewContext(expectedValue)) {
            ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
            manager.initializeNewContext("Jules Winnfield");
            assertThat(manager.getActiveContext().getValue(), is("Jules Winnfield"));

            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .supplyAsync(DummyContextManager::currentValue, contextUnawareThreadpool, snapshot);
            assertThat(future.get().get(), is(expectedValue));
        }
    }

    @Test
    public void testRunAsync() throws ExecutionException, InterruptedException {
        String expectedValue = "Mia Wallace";
        try (Context<String> ctx = manager.initializeNewContext(expectedValue)) {
            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> assertThat(DummyContextManager.currentValue(), is(Optional.of(expectedValue))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testRunAsync_executor() throws ExecutionException, InterruptedException {
        String expectedValue = "Jimmie";
        try (Context<String> ctx = manager.initializeNewContext(expectedValue)) {
            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(
                    () -> assertThat(DummyContextManager.currentValue(), is(Optional.of(expectedValue))),
                    contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testRunAsync_executor_snapshot() throws ExecutionException, InterruptedException {
        String expectedValue = "Pumpkin";
        try (Context<String> ctx = manager.initializeNewContext(expectedValue)) {
            ContextSnapshot snapshot = ContextManagers.createContextSnapshot();
            manager.initializeNewContext("Honey Bunny");
            assertThat(manager.getActiveContext().getValue(), is("Honey Bunny"));

            ContextAwareCompletableFuture<Void> future = ContextAwareCompletableFuture.runAsync(
                    () -> assertThat(DummyContextManager.currentValue(), is(Optional.of(expectedValue))),
                    contextUnawareThreadpool,
                    snapshot);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenApply() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Jimmie")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Bonnie"))
                    .thenApply(voidvalue -> DummyContextManager.currentValue());
            assertThat(future.get().get(), is("Bonnie"));
        }
    }

    @Test
    public void testThenApplyAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Esmerelda Villalobos"))
                    .thenApplyAsync(voidvalue -> DummyContextManager.currentValue());
            assertThat(future.get().get(), is("Esmerelda Villalobos"));
        }
    }

    @Test
    public void testThenApplyAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Maynard")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Zed"))
                    .thenApplyAsync(voidvalue -> DummyContextManager.currentValue(), contextUnawareThreadpool);
            assertThat(future.get().get(), is("Zed"));
        }
    }

    @Test
    public void testThenAccept() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("The Gimp")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Butch"))
                    .thenAccept(voidvalue -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Butch"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenAcceptAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Fabienne"))
                    .thenAcceptAsync(voidvalue -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Fabienne"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenAcceptAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Marvin")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Winston Wolfe"))
                    .thenAcceptAsync(
                            voidvalue -> assertThat(DummyContextManager.currentValue().get(), is("Winston Wolfe")),
                            contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenRun() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Lance")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Jody"))
                    .thenRun(() -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Jody"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenRunAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Ringo")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Yolanda"))
                    .thenRunAsync(() -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Yolanda"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testThenRunAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Capt. Koons")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Butch"))
                    .thenRunAsync(
                            () -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Butch"))),
                            contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testWhenComplete() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Floyd"))
                    .whenComplete((voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Floyd"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testWhenCompleteAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Zed")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Pipe hittin' niggers"))
                    .whenCompleteAsync((voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Pipe hittin' niggers"))));
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testWhenCompleteAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Butch")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Floyd"))
                    .whenCompleteAsync(
                            (voidValue, exception) -> assertThat(DummyContextManager.currentValue(), is(Optional.of("Floyd"))),
                            contextUnawareThreadpool);
            future.get(); // trigger asynchronous assertion
        }
    }

    @Test
    public void testHandle() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> {
                        manager.initializeNewContext("Trudy");
                        throw exception;
                    })
                    .handle((voidValue, throwable) -> DummyContextManager.currentValue());
            assertThat(future.get(), is(Optional.of("Trudy")));
        }
    }

    @Test
    public void testHandleAsync() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> {
                        manager.initializeNewContext("Trudy");
                        throw exception;
                    })
                    .handleAsync((voidValue, throwable) -> DummyContextManager.currentValue());
            assertThat(future.get(), is(Optional.of("Trudy")));
        }
    }

    @Test
    public void testHandleAsync_executor() throws ExecutionException, InterruptedException {
        final RuntimeException exception = new RuntimeException("Bad Motherfucker");
        try (Context<String> ctx = manager.initializeNewContext("Jody")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> {
                        manager.initializeNewContext("Trudy");
                        throw exception;
                    })
                    .handleAsync(
                            (voidValue, throwable) -> DummyContextManager.currentValue(),
                            contextUnawareThreadpool);
            assertThat(future.get(), is(Optional.of("Trudy")));
        }
    }

    @Test
    public void testExceptionally() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Gringo")) {
            Future<Void> future = ContextAwareCompletableFuture
                    .runAsync(() -> {
                        manager.initializeNewContext("Jules Winnfield");
                        throw new RuntimeException("Bad Motherfucker");
                    })
                    .exceptionally(ex -> {
                        assertThat(DummyContextManager.currentValue(), is(Optional.of("Jules Winnfield")));
                        return null;
                    });
            future.get();
        }
    }

    @Test
    public void testThenCombine() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Marcellus Wallace")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Vincent Vega"))
                    .thenCombine(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Jules Winnfield")),
                            (voidA, voidB) -> DummyContextManager.currentValue());
            assertThat(future.get(), is(Optional.of("Vincent Vega")));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Marcellus Wallace")));
        }
    }

    @Test
    public void testThenCombineAsync() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Brett")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Marvin"))
                    .thenCombineAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Flock of Seagulls")),
                            (voidA, voidB) -> DummyContextManager.currentValue());
            assertThat(future.get(), is(Optional.of("Marvin")));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Brett")));
        }
    }

    @Test
    public void testThenCombineAsync_executor() throws ExecutionException, InterruptedException {
        try (Context<String> ctx = manager.initializeNewContext("Brett")) {
            Future<Optional<String>> future = ContextAwareCompletableFuture
                    .runAsync(() -> manager.initializeNewContext("Marvin"))
                    .thenCombineAsync(
                            ContextAwareCompletableFuture.runAsync(() -> manager.initializeNewContext("Flock of Seagulls")),
                            (voidA, voidB) -> DummyContextManager.currentValue(),
                            contextUnawareThreadpool);
            assertThat(future.get(), is(Optional.of("Marvin")));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Brett")));
        }
    }

    @Test
    public void testTimingIssue55() throws ExecutionException, InterruptedException, TimeoutException {
        try (Context<String> ctx = manager.initializeNewContext("Vincent Vega")) {
            final CountDownLatch latch1 = new CountDownLatch(1), latch2 = new CountDownLatch(1);
            ContextAwareCompletableFuture<String> future1 = ContextAwareCompletableFuture
                    .supplyAsync(() -> {
                        String result = DummyContextManager.currentValue().orElse("NO VALUE");
                        DummyContextManager.setCurrentValue("Jules Winnfield");
                        waitFor(latch1);
                        return result;
                    });
            CompletableFuture<String> future2 = future1.thenApplyAsync(value -> {
                String result = value + ", " + DummyContextManager.currentValue().orElse("NO VALUE");
                DummyContextManager.setCurrentValue("Marcellus Wallace");
                waitFor(latch2);
                return result;
            });
            Future<String> future3 = future2.thenApplyAsync(value ->
                    value + ", " + DummyContextManager.currentValue().orElse("NO VALUE"));

            assertThat("Future creation may not block on previous stages", future1.isDone(), is(false));
            assertThat("Future creation may not block on previous stages", future2.isDone(), is(false));
            assertThat("Future creation may not block on previous stages", future3.isDone(), is(false));

            latch1.countDown();
            future1.get(500, TimeUnit.MILLISECONDS);
            assertThat("Future creation may not block on previous stages", future1.isDone(), is(true));
            assertThat("Future creation may not block on previous stages", future2.isDone(), is(false));
            assertThat("Future creation may not block on previous stages", future3.isDone(), is(false));

            latch2.countDown();
            future2.get(500, TimeUnit.MILLISECONDS);
            assertThat("Future creation may not block on previous stages", future2.isDone(), is(true));
            assertThat(future3.get(500, TimeUnit.MILLISECONDS), is("Vincent Vega, Jules Winnfield, Marcellus Wallace"));
            assertThat(DummyContextManager.currentValue(), is(Optional.of("Vincent Vega")));
        }
    }

    private static void waitFor(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted waiting for latch.", ie);
        }
    }
}
