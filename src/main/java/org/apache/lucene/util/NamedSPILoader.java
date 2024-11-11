/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.util;

import org.eclipse.jetty.util.ServiceLoaderSpliterator;
import org.eclipse.jetty.util.TypeUtil;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * Helper class for loading named SPIs from classpath (e.g. Codec, PostingsFormat).
 * <p>
 * <strong>LICENSE NOTE:</strong> This is the modified version of the original {@code org.apache.lucene.util.NamedSPILoader}
 * in a way that services that can't be loaded (exception is thrown in {@link ServiceLoader}'s {@code hasNext()} method)
 * won't fail the loading of all subsequent services.
 * <p>
 * This can be the case for example when: Lucas (0.5.0) is built on v2024.2 of the IntelliJ Platform that bundles Lucene
 * 9.9 but Lucas uses Lucene 9.12.0. This causes at least the {@code Lucene99Codec} class to be available in two places:
 * in {@code org.apache.lucene.codecs.lucene99} in IJ sources, and in {@code org.apache.lucene.backward_codecs.lucene99}
 * coming via Lucas dependencies.
 * <p>
 * This causes the one in IJ sources fail the loading of in {@link #reload(ClassLoader)}, when iterating over the result
 * of {@code ServiceLoader.load()}, specifically in its {@code hasNext()} method, throwing a {@link ServiceConfigurationError}
 * and preventing the load of subsequent services.
 *
 * @lucene.internal
 */
public final class NamedSPILoader<S extends NamedSPILoader.NamedSPI> implements Iterable<S> {

  private volatile Map<String, S> services = Collections.emptyMap();
  private final Class<S> clazz;

  public NamedSPILoader(Class<S> clazz) {
    this(clazz, null);
  }

  public NamedSPILoader(Class<S> clazz, ClassLoader classloader) {
    this.clazz = clazz;
    // if clazz' classloader is not a parent of the given one, we scan clazz's classloader, too:
    final ClassLoader clazzClassloader = clazz.getClassLoader();
    if (classloader == null) {
      classloader = clazzClassloader;
    }
    if (clazzClassloader != null
        && !ClassLoaderUtils.isParentClassLoader(clazzClassloader, classloader)) {
      reload(clazzClassloader);
    }
    reload(classloader);
  }

  /**
   * Reloads the internal SPI list from the given {@link ClassLoader}. Changes to the service list
   * are visible after the method ends, all iterators ({@link #iterator()},...) stay consistent.
   *
   * <p><b>NOTE:</b> Only new service providers are added, existing ones are never removed or
   * replaced.
   *
   * <p><em>This method is expensive and should only be called for discovery of new service
   * providers on the given classpath/classloader!</em>
   */
  public void reload(ClassLoader classloader) {
    Objects.requireNonNull(classloader, "classloader");
    final LinkedHashMap<String, S> services = new LinkedHashMap<>(this.services);

    //Based on https://github.com/jetty/jetty.project/issues/4340 and https://github.com/jetty/jetty.project/pull/4602.
    // See class javadoc for details.
    var loadedServices = StreamSupport.stream(new ServiceLoaderSpliterator<>(ServiceLoader.load(clazz, classloader)), false)
        .flatMap(TypeUtil::mapToService);

    loadedServices.forEach(service -> {
      final String name = service.getName();
      // only add the first one for each name, later services will be ignored
      // this allows to place services before others in classpath to make
      // them used instead of others
      if (!services.containsKey(name)) {
        checkServiceName(name);
        services.put(name, service);
      }
    });

    this.services = Collections.unmodifiableMap(services);
  }

  /** Validates that a service name meets the requirements of {@link NamedSPI} */
  public static void checkServiceName(String name) {
    // based on harmony charset.java
    if (name.length() >= 128) {
      throw new IllegalArgumentException(
          "Illegal service name: '" + name + "' is too long (must be < 128 chars).");
    }
    for (int i = 0, len = name.length(); i < len; i++) {
      char c = name.charAt(i);
      if (!isLetterOrDigit(c)) {
        throw new IllegalArgumentException(
            "Illegal service name: '" + name + "' must be simple ascii alphanumeric.");
      }
    }
  }

  /** Checks whether a character is a letter or digit (ascii) which are defined in the spec. */
  private static boolean isLetterOrDigit(char c) {
    return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z') || ('0' <= c && c <= '9');
  }

  public S lookup(String name) {
    final S service = services.get(name);
    if (service != null) return service;
    throw new IllegalArgumentException(
        "An SPI class of type "
            + clazz.getName()
            + " with name '"
            + name
            + "' does not exist."
            + "  You need to add the corresponding JAR file supporting this SPI to your classpath."
            + "  The current classpath supports the following names: "
            + availableServices());
  }

  public Set<String> availableServices() {
    return services.keySet();
  }

  @Override
  public Iterator<S> iterator() {
    return services.values().iterator();
  }

  /**
   * Interface to support {@link NamedSPILoader#lookup(String)} by name.
   *
   * <p>Names must be all ascii alphanumeric, and less than 128 characters in length.
   */
  public interface NamedSPI {
    String getName();
  }
}
