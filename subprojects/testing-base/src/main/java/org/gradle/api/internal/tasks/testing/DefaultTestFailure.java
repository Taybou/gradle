/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks.testing;

import org.gradle.api.tasks.testing.TestFailure;
import org.gradle.api.tasks.testing.TestFailureDetails;

import java.io.PrintWriter;
import java.io.StringWriter;

public class DefaultTestFailure implements TestFailure {

    private final Throwable rawFailure;
    private final TestFailureDetails details;

    public DefaultTestFailure(Throwable rawFailure, TestFailureDetails details) {
        this.rawFailure = rawFailure;
        this.details = details;
    }

    @Override
    public Throwable getRawFailure() {
        return rawFailure;
    }

    @Override
    public TestFailureDetails getDetails() {
        return details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultTestFailure that = (DefaultTestFailure) o;

        if (rawFailure != null ? !rawFailure.equals(that.rawFailure) : that.rawFailure != null) {
            return false;
        }
        return details != null ? details.equals(that.details) : that.details == null;
    }

    @Override
    public int hashCode() {
        int result = rawFailure != null ? rawFailure.hashCode() : 0;
        result = 31 * result + (details != null ? details.hashCode() : 0);
        return result;
    }

    public static TestFailure fromTestAssertionFailure(Throwable failure, String expected, String actual) {
        DefaultTestFailureDetails details = new DefaultTestFailureDetails(failure.getMessage(), failure.getClass().getName(), stacktraceOf(failure), true, expected, actual);
        return new DefaultTestFailure(failure, details);
    }

    public static TestFailure fromTestFrameworkFailure(Throwable failure) {
        DefaultTestFailureDetails details = new DefaultTestFailureDetails(messageOf(failure), failure.getClass().getName(), stacktraceOf(failure), false, null, null);
        return new DefaultTestFailure(failure, details);
    }

    private static String messageOf(Throwable throwable) {
        try {
            return throwable.getMessage();
        } catch (Throwable t) {
            return String.format("Could not determine failure message for exception of type %s: %s", throwable.getClass().getName(), t);
        }
    }

    private static String stacktraceOf(Throwable throwable) {
        try {
            StringWriter out = new StringWriter();
            PrintWriter wrt = new PrintWriter(out);
            throwable.printStackTrace(wrt);
            return out.toString();
        } catch (Exception t) {
            return stacktraceOf(t);
        }

    }
}
