/*
 * Copyright 2016-2017 Talsma ICT
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

import nl.talsmasoftware.context.ContextManagers;
import nl.talsmasoftware.context.ContextSnapshot;
import nl.talsmasoftware.context.functions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.*;

/**
 * This class extends the standard {@link CompletableFuture} that was introduced in java version 8.
 * <p>
 * The class is a 'normal' Completable Future, but every successive call made on the result will be made within the
 * {@link ContextSnapshot context during creation} of this {@link ContextAwareCompletableFuture}.
 *
 * @author Sjoerd Talsma
 */
public class ContextAwareCompletableFuture<T> extends CompletableFuture<T> {

    /**
     * A snapshot of the context as it was when this <code>CompletableFuture</code> was created.
     */
    private final ContextSnapshot snapshot;

    /**
     * Creates a new {@link ContextSnapshot} and remembers that in this completable future, running all
     * completion methods within this snapshot.
     *
     * @see ContextManagers#createContextSnapshot()
     */
    public ContextAwareCompletableFuture() {
        this(null);
    }

    /**
     * Creates a new {@link CompletableFuture} where all completion methods are run within the specified
     * snapshot context.
     *
     * @param snapshot The snapshot to run completion methods in (or specify <code>null</code> to take a
     *                 new snapshot upon creation of this completable future).
     * @see ContextManagers#createContextSnapshot()
     */
    public ContextAwareCompletableFuture(ContextSnapshot snapshot) {
        this.snapshot = snapshot != null ? snapshot : ContextManagers.createContextSnapshot();
    }

    public static <U> ContextAwareCompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return supplyAsync(supplier, null, null);
    }

    public static <U> ContextAwareCompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
        return supplyAsync(supplier, executor, null);
    }

    public static <U> ContextAwareCompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor, ContextSnapshot snapshot) {
        if (!(supplier instanceof SupplierWithContext)) {
            if (snapshot == null) snapshot = ContextManagers.createContextSnapshot();
            supplier = new SupplierWithContext<>(snapshot, supplier);
        }
        return executor == null
                ? wrap(CompletableFuture.supplyAsync(supplier), snapshot)
                : wrap(CompletableFuture.supplyAsync(supplier, executor), snapshot);
    }

    public static ContextAwareCompletableFuture<Void> runAsync(Runnable runnable) {
        return runAsync(runnable, null, null);
    }

    public static ContextAwareCompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        return runAsync(runnable, executor, null);
    }

    public static ContextAwareCompletableFuture<Void> runAsync(Runnable runnable, Executor executor, ContextSnapshot snapshot) {
        if (!(runnable instanceof RunnableWithContext)) {
            if (snapshot == null) snapshot = ContextManagers.createContextSnapshot();
            runnable = new RunnableWithContext(snapshot, runnable);
        }
        return executor == null
                ? wrap(CompletableFuture.runAsync(runnable), snapshot)
                : wrap(CompletableFuture.runAsync(runnable, executor), snapshot);
    }

    private static <U> ContextAwareCompletableFuture<U> wrap(CompletableFuture<U> completableFuture, ContextSnapshot snapshot) {
        if (completableFuture == null || completableFuture instanceof ContextAwareCompletableFuture) {
            return (ContextAwareCompletableFuture<U>) completableFuture;
        }
        ContextAwareCompletableFuture<U> contextAwareCompletableFuture = new ContextAwareCompletableFuture<>(snapshot);
        completableFuture.whenComplete((result, throwable) -> {
            if (throwable != null) contextAwareCompletableFuture.completeExceptionally(throwable);
            else contextAwareCompletableFuture.complete(result);
        });
        return contextAwareCompletableFuture;
    }

    /**
     * Wraps the resulting completable future from any folowup calls in this class.
     *
     * @param result   The original result after the contextualized followup call.
     * @param <RESULT> The result type of the completable future.
     * @return The wrapped completable future result.
     */
    protected <RESULT> CompletableFuture<RESULT> wrapResult(CompletableFuture<RESULT> result) {
        return result;
    }

    @Override
    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return wrapResult(super.thenApply(new FunctionWithContext<>(snapshot, fn)));
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return wrapResult(super.thenApplyAsync(new FunctionWithContext<>(snapshot, fn)));
    }

    @Override
    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
        return wrapResult(super.thenApplyAsync(new FunctionWithContext<>(snapshot, fn), executor));
    }

    @Override
    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return wrapResult(super.thenAccept(new ConsumerWithContext<>(snapshot, action)));
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return wrapResult(super.thenAcceptAsync(new ConsumerWithContext<>(snapshot, action)));
    }

    @Override
    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
        return wrapResult(super.thenAcceptAsync(new ConsumerWithContext<>(snapshot, action), executor));
    }

    @Override
    public CompletableFuture<Void> thenRun(Runnable action) {
        return wrapResult(super.thenRun(new RunnableWithContext(snapshot, action)));
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return wrapResult(super.thenRunAsync(new RunnableWithContext(snapshot, action)));
    }

    @Override
    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return wrapResult(super.thenRunAsync(new RunnableWithContext(snapshot, action), executor));
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return wrapResult(super.thenCombine(other, new BiFunctionWithContext<>(snapshot, fn)));
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn) {
        return wrapResult(super.thenCombineAsync(other, new BiFunctionWithContext<>(snapshot, fn)));
    }

    @Override
    public <U, V> CompletableFuture<V> thenCombineAsync(
            CompletionStage<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
        return wrapResult(super.thenCombineAsync(other, new BiFunctionWithContext<>(snapshot, fn), executor));
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return wrapResult(super.thenAcceptBoth(other, new BiConsumerWithContext<>(snapshot, action)));
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action) {
        return wrapResult(super.thenAcceptBothAsync(other, new BiConsumerWithContext<>(snapshot, action)));
    }

    @Override
    public <U> CompletableFuture<Void> thenAcceptBothAsync(
            CompletionStage<? extends U> other, BiConsumer<? super T, ? super U> action, Executor executor) {
        return wrapResult(super.thenAcceptBothAsync(other, new BiConsumerWithContext<>(snapshot, action), executor));
    }

    @Override
    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return wrapResult(super.runAfterBoth(other, new RunnableWithContext(snapshot, action)));
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return wrapResult(super.runAfterBothAsync(other, new RunnableWithContext(snapshot, action)));
    }

    @Override
    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return wrapResult(super.runAfterBothAsync(other, new RunnableWithContext(snapshot, action), executor));
    }

    @Override
    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return wrapResult(super.applyToEither(other, new FunctionWithContext<>(snapshot, fn)));
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
        return wrapResult(super.applyToEitherAsync(other, new FunctionWithContext<>(snapshot, fn)));
    }

    @Override
    public <U> CompletableFuture<U> applyToEitherAsync(
            CompletionStage<? extends T> other, Function<? super T, U> fn, Executor executor) {
        return wrapResult(super.applyToEitherAsync(other, new FunctionWithContext<>(snapshot, fn), executor));
    }

    @Override
    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return wrapResult(super.acceptEither(other, new ConsumerWithContext<>(snapshot, action)));
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
        return wrapResult(super.acceptEitherAsync(other, new ConsumerWithContext<>(snapshot, action)));
    }

    @Override
    public CompletableFuture<Void> acceptEitherAsync(
            CompletionStage<? extends T> other, Consumer<? super T> action, Executor executor) {
        return wrapResult(super.acceptEitherAsync(other, new ConsumerWithContext<>(snapshot, action), executor));
    }

    @Override
    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return wrapResult(super.runAfterEither(other, new RunnableWithContext(snapshot, action)));
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return wrapResult(super.runAfterEitherAsync(other, new RunnableWithContext(snapshot, action)));
    }

    @Override
    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
        return wrapResult(super.runAfterEitherAsync(other, new RunnableWithContext(snapshot, action), executor));
    }

    @Override
    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return wrapResult(super.thenCompose(new FunctionWithContext<>(snapshot, fn)));
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return wrapResult(super.thenComposeAsync(new FunctionWithContext<>(snapshot, fn)));
    }

    @Override
    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn, Executor executor) {
        return wrapResult(super.thenComposeAsync(new FunctionWithContext<>(snapshot, fn), executor));
    }

    @Override
    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return wrapResult(super.whenComplete(new BiConsumerWithContext<>(snapshot, action)));
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return wrapResult(super.whenCompleteAsync(new BiConsumerWithContext<>(snapshot, action)));
    }

    @Override
    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
        return wrapResult(super.whenCompleteAsync(new BiConsumerWithContext<>(snapshot, action), executor));
    }

    @Override
    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return wrapResult(super.handle(new BiFunctionWithContext<>(snapshot, fn)));
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return wrapResult(super.handleAsync(new BiFunctionWithContext<>(snapshot, fn)));
    }

    @Override
    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
        return wrapResult(super.handleAsync(new BiFunctionWithContext<>(snapshot, fn), executor));
    }

    @Override
    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return wrapResult(super.exceptionally(new FunctionWithContext<>(snapshot, fn)));
    }

}
