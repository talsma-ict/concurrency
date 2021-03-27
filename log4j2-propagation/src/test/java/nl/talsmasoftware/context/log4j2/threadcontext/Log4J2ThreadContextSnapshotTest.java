/*
 * Copyright 2016-2021 Talsma ICT
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
package nl.talsmasoftware.context.log4j2.threadcontext;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Log4J2ThreadContextSnapshotTest {
    @BeforeEach
    @AfterEach
    void clearThreadContext() {
        ThreadContext.clearAll();
    }

    @Test
    void testCaptureFromCurrentThread() {
        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        Log4j2ThreadContextSnapshot snapshot = Log4j2ThreadContextSnapshot.captureFromCurrentThread();

        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("map1", "value1");
        expectedMap.put("map2", "value2");
        assertThat(snapshot.getContextMap(), equalTo(expectedMap));

        List<String> expectedStack = Arrays.asList("stack1", "stack2");
        assertThat(snapshot.getContextStack(), equalTo(expectedStack));
    }

    @Test
    void testCaptureFromCurrentThread_empty() {
        assertThat(ThreadContext.getDepth(), is(0));
        assertThat(ThreadContext.isEmpty(), is(true));

        Log4j2ThreadContextSnapshot snapshot = Log4j2ThreadContextSnapshot.captureFromCurrentThread();
        assertThat(snapshot.getContextMap(), is(anEmptyMap()));
        assertThat(snapshot.getContextStack(), is(empty()));
//
//        // Applying empty snapshot should have no effect
//        data.applyToCurrentThread();
//        assertEquals(0, ThreadContext.getDepth());
//        assertTrue(ThreadContext.isEmpty());
    }

/*
    @Test
    void testFromCurrentThreadContext_empty_apply() {
        assertEquals(0, ThreadContext.getDepth());
        assertTrue(ThreadContext.isEmpty());

        Log4j2ThreadContextData data = Log4j2ThreadContextData.fromCurrentThreadContext();

        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        // Modification of ThreadContext should not have affected snapshot
        assertTrue(data.getContextMap().isEmpty());
        assertTrue(data.getContextStack().isEmpty());

        // Applying empty context on top of existing one should have no effect
        data.applyToCurrentThread();

        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("map1", "value1");
        expectedMap.put("map2", "value2");
        assertEquals(expectedMap, ThreadContext.getContext());

        List<String> expectedStack = Arrays.asList("stack1", "stack2");
        assertEquals(expectedStack, ThreadContext.getImmutableStack().asList());
    }
*/

    /**
     * Verify that {@link Log4j2ThreadContextSnapshot} is a snapshot and not affected
     * by subsequent modification of {@link ThreadContext}.
     */
    @Test
    void testFromCurrentThreadContext_modification() {
        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        Log4j2ThreadContextSnapshot snapshot = Log4j2ThreadContextSnapshot.captureFromCurrentThread();

        // Should not affect snapshot
        ThreadContext.put("map3", "value3");
        ThreadContext.push("stack3");

        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("map1", "value1");
        expectedMap.put("map2", "value2");
        assertThat(snapshot.getContextMap(), equalTo(expectedMap));

        List<String> expectedStack = Arrays.asList("stack1", "stack2");
        assertThat(snapshot.getContextStack(), equalTo(expectedStack));
    }

    @Test
    void test_umodifiableViews() {
        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        Log4j2ThreadContextSnapshot snapshot = Log4j2ThreadContextSnapshot.captureFromCurrentThread();
        final Map<String, String> contextMap = snapshot.getContextMap();
        final List<String> contextStack = snapshot.getContextStack();

        assertThrows(UnsupportedOperationException.class, new Executable() {
            public void execute() {
                contextMap.clear();
            }
        });
        assertThrows(UnsupportedOperationException.class, new Executable() {
            public void execute() {
                contextMap.put("map3", "value3");
            }
        });
        assertThrows(UnsupportedOperationException.class, new Executable() {
            public void execute() {
                contextStack.clear();
            }
        });
        assertThrows(UnsupportedOperationException.class, new Executable() {
            public void execute() {
                contextStack.add("stack3");
            }
        });

        // Modification attempts should not have had any effect
        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("map1", "value1");
        expectedMap.put("map2", "value2");
        assertThat(contextMap, equalTo(expectedMap));

        List<String> expectedStack = Arrays.asList("stack1", "stack2");
        assertThat(contextStack, equalTo(expectedStack));
    }

/*
    @Test
    void testToString() {
        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        Log4j2ThreadContextData data = Log4j2ThreadContextData.fromCurrentThreadContext();
        String expectedString = "Log4j2ThreadContextData{map=" + data.getContextMap()
            + ",stack=" + data.getContextStack() + "}";
        assertThat(data, hasToString(expectedString));
    }
*/

    /**
     * Verify that {@link Log4j2ThreadContextSnapshot#applyToCurrentThread(Log4j2ThreadContextSnapshot, boolean)}
     * with {@code overwrite=false} appends data to existing one, only overwriting existing
     * one in case of conflict.
     */
/*
    @Test
    void testApplyToCurrentThread() {
        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        Log4j2ThreadContextData data = Log4j2ThreadContextData.fromCurrentThreadContext();

        ThreadContext.clearAll();
        ThreadContext.put("map1", "old-value1");
        ThreadContext.put("map3", "old-value3");
        ThreadContext.push("stack3");

        Log4j2ThreadContextData.applyToCurrentThread(data, false);

        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("map1", "value1"); // old-value1 should have been overwritten
        expectedMap.put("map2", "value2");
        expectedMap.put("map3", "old-value3");
        assertEquals(expectedMap, ThreadContext.getContext());

        List<String> expectedStack = Arrays.asList("stack3", "stack1", "stack2");
        assertEquals(expectedStack, ThreadContext.getImmutableStack().asList());
    }
*/

    /**
     * Verify that {@link Log4j2ThreadContextSnapshot#applyToCurrentThread(Log4j2ThreadContextSnapshot, boolean)}
     * with {@code overwrite=true} overwrites all existing data.
     */
/*
    @Test
    void testApplyToCurrentThread_overwrite() {
        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        Log4j2ThreadContextData data = Log4j2ThreadContextData.fromCurrentThreadContext();

        ThreadContext.clearAll();
        ThreadContext.put("map1", "old-value1");
        ThreadContext.put("map3", "old-value3");
        ThreadContext.push("stack3");

        Log4j2ThreadContextData.applyToCurrentThread(data, true);

        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("map1", "value1");
        expectedMap.put("map2", "value2");
        assertEquals(expectedMap, ThreadContext.getContext());

        List<String> expectedStack = Arrays.asList("stack1", "stack2");
        assertEquals(expectedStack, ThreadContext.getImmutableStack().asList());
    }
*/

    /**
     * Verify that {@link Log4j2ThreadContextSnapshot#applyToCurrentThread(Log4j2ThreadContextSnapshot, boolean)}
     * with {@code data=null,overwrite=false} has no effect.
     */
/*
    @Test
    void testApplyToCurrentThread_null() {
        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        // Should have no effect
        Log4j2ThreadContextData.applyToCurrentThread(null, false);

        Map<String, String> expectedMap = new HashMap<String, String>();
        expectedMap.put("map1", "value1");
        expectedMap.put("map2", "value2");
        assertEquals(expectedMap, ThreadContext.getContext());

        List<String> expectedStack = Arrays.asList("stack1", "stack2");
        assertEquals(expectedStack, ThreadContext.getImmutableStack().asList());
    }
*/

    /**
     * Verify that {@link Log4j2ThreadContextSnapshot#applyToCurrentThread(Log4j2ThreadContextSnapshot, boolean)}
     * with {@code data=null,overwrite=true} clears thread context.
     */
/*
    @Test
    void testApplyToCurrentThread_null_overwrite() {
        ThreadContext.put("map1", "value1");
        ThreadContext.put("map2", "value2");
        ThreadContext.push("stack1");
        ThreadContext.push("stack2");

        // Should clear existing context
        Log4j2ThreadContextData.applyToCurrentThread(null, true);

        assertTrue(ThreadContext.isEmpty());
        assertEquals(0, ThreadContext.getDepth());
    }
*/
}