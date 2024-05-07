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

package com.picimako.org.apache.lucene.luke.app.desktop.components;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An utility class for interaction between components
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.components.ComponentOperatorRegistry}.
 */
@Service(Service.Level.APP)
public final class ComponentOperatorRegistry {

  private final Map<Class<?>, Object> operators = new HashMap<>();

  public static ComponentOperatorRegistry getInstance() {
    return ApplicationManager.getApplication().getService(ComponentOperatorRegistry.class);
  }

  public <T extends ComponentOperator> void register(Class<T> type, T operator) {
    if (!operators.containsKey(type)) {
      operators.put(type, operator);
    }
  }

  @SuppressWarnings("unchecked")
  public <T extends ComponentOperator> Optional<T> get(Class<T> type) {
    return Optional.ofNullable((T) operators.get(type));
  }

  public void clear() {
    operators.clear();
  }

  /** marker interface for operators */
  public interface ComponentOperator {}
}
