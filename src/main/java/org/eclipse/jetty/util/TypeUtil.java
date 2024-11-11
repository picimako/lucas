//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.util;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * TYPE Utilities.
 * Provides various static utility methods for manipulating types and their
 * string representations.
 * <p>
 * <strong>LICENSE NOTE</strong>: This is the modified version of the original {@code org.eclipse.jetty.util.TypeUtil} class
 * in the jetty-util project.
 *
 * @since Jetty 4.1
 */
public class TypeUtil
{
    /**
     * Used on a {@link ServiceLoader#stream()} with {@link Stream#flatMap(Function)},
     * so that in the case a {@link ServiceConfigurationError} is thrown it warns and
     * continues iterating through the service loader.
     * <br>Usage Example:
     * <p>{@code ServiceLoader.load(Service.class).stream().flatMap(TypeUtil::providerMap).collect(Collectors.toList());}</p>
     * @param <T> The class of the service type.
     * @param provider The service provider to instantiate.
     * @return a stream of the loaded service providers.
     */
    public static <T> Stream<T> mapToService(ServiceLoader.Provider<T> provider)
    {
        try
        {
            return Stream.of(provider.get());
        }
        catch (ServiceConfigurationError error)
        {
            return Stream.empty();
        }
    }
}