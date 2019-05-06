[![Build Status][ci-img]][ci]
[![Coverage Status][coveralls-img]][coveralls]
[![Maven Version][maven-img]][maven]

# Context propagation library

Standardized context propagation in concurrent systems.

Provides a standardized way to create snapshots from various supported
`ThreadLocal`-based `Context` types that can be reactivated in another
thread.

## How to use this library

Use a `ContextAwareExecutorService` instead of your usual threadpool to start
background threads.  
It will create a snapshot of supported `ThreadLocal`-based contexts and
reactivate them in the background thread when started.
The ThreadLocal values from the calling thread will therefore automatically 
be available in the background thread as well.

## Supported contexts

The following `ThreadLocal`-based contexts are currently supported 
out of the box by this context-propagation library:

- [ServletRequest contexts][servletrequest propagation]
- [Slf4J MDC (Mapped Diagnostic Context)][mdc propagation]
- [Locale context][locale context]
- [Spring Security Context]
- [OpenTracing Span contexts][opentracing span propagation]
- _Yours?_ Feel free to create an issue or pull-request
  if you believe there's a general context that was forgotten. 

Adding your own `Context` type is not difficult.

## Custom contexts

It is easy to add a custom `Context` type to be propagated:

1. Implement the `ContextManager` interface.  
   Create a class with a [default constructor]
   that implements _initializeNewContext_ and _getActiveContext_ methods.
2. Create a service file called
   `/META-INF/services/nl.talsmasoftware.context.ContextManager` 
   containing the qualified class name of your `ContextManager` implementation.
3. That's it. Now the result from your _getActiveContext_ method is propagated
   into each snapshot created by the `ContextManagers.createSnapshot()` method.
   This includes all usages of the `ContextAwareExecutorService`.

An example of a custom context implementation:
```java
public class DummyContextManager implements ContextManager<String> {
    public Context<String> initializeNewContext(String value) {
        return new DummyContext(value);
    }

    public Context<String> getActiveContext() {
        return DummyContext.current();
    }
    
    public static Optional<String> currentValue() {
        return Optional.ofNullable(DummyContext.current()).map(Context::getValue);
    }
    
    private static final class DummyContext extends AbstractThreadLocalContext<String> {
        private DummyContext(String newValue) {
            super(newValue);
        }
        
        private static Context<String> current() {
            return AbstractThreadLocalContext.current(DummyContext.class);
        }
    }
}
```

## Performance metrics

No library is 'free' with regards to performance.
Capturing a context snapshot and reactivating it in another thread is no different.
For insight, the library tracks the overall time used creating and reactivating
context snapshots along with time spent in each individual `ContextManager`.

### Logging performance
On a development machine, you can get timing for each snapshot by turning on logging
for `nl.talsmasoftware.context.Timing` at `FINEST` or `TRACE` level 
(depending on your logger of choice).
Please **do not** turn this on in production as the logging overhead will most likely
have a noticable impact to the context management itself.

### Dropwizard metrics
If your project happens to use [dropwizard metrics](https://metrics.dropwizard.io/),
adding the [context propagation metrics] module to your classpath will automatically 
configure various timers in the default metric registry of your application.

## License

[Apache 2.0 license](../LICENSE)


  [ci-img]: https://travis-ci.org/talsma-ict/context-propagation.svg?branch=develop
  [ci]: https://travis-ci.org/talsma-ict/context-propagation
  [maven-img]: https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/nl/talsmasoftware/context/context-propagation/maven-metadata.xml.svg
  [maven]: http://mvnrepository.com/artifact/nl.talsmasoftware.context
  [release-img]: https://img.shields.io/github/release/talsma-ict/context-propagation.svg
  [release]: https://github.com/talsma-ict/context-propagation/releases
  [coveralls-img]: https://coveralls.io/repos/github/talsma-ict/context-propagation/badge.svg
  [coveralls]: https://coveralls.io/github/talsma-ict/context-propagation

  [servletrequest propagation]: ../servletrequest-propagation
  [mdc propagation]: ../mdc-propagation
  [locale context]: ../locale-context
  [spring security context]: ../spring-security-context
  [opentracing span propagation]: ../opentracing-span-propagation
  [context propagation metrics]: ../context-propagation-metrics
  [default constructor]: https://en.wikipedia.org/wiki/Nullary_constructor
