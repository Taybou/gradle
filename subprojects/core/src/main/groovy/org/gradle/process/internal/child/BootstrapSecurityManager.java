/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.process.internal.child;

import java.io.DataInputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Permission;

/**
 * Used to bootstrap the system classpath.
 */
public class BootstrapSecurityManager extends SecurityManager {
    private boolean initialised;
    private final URLClassLoader target;

    public BootstrapSecurityManager() {
        this(null);
    }

    public BootstrapSecurityManager(URLClassLoader target) {
        this.target = target;
    }

    @Override
    public void checkPermission(Permission permission) {
        if (initialised) {
            return;
        }

        initialised = true;

        try {
            URLClassLoader systemClassLoader = target != null ? target : (URLClassLoader) getClass().getClassLoader();
            Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addUrlMethod.setAccessible(true);

            DataInputStream inputStream = new DataInputStream(System.in);
            int count = inputStream.readInt();
            StringBuilder classpathStr = new StringBuilder();
            for (int i = 0; i < count; i++) {
                String entry = inputStream.readUTF();
                File file = new File(entry);
                addUrlMethod.invoke(systemClassLoader, file.toURI().toURL());
                if (i > 0) {
                    classpathStr.append(File.pathSeparator);
                }
                classpathStr.append(file.toString());
            }
            System.setProperty("java.class.path", classpathStr.toString());
        } catch (Exception e) {
            throw new RuntimeException("Could not initialise system classpath.", e);
        }
    }
}
