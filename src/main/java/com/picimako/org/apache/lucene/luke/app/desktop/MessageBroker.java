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

package com.picimako.org.apache.lucene.luke.app.desktop;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Message broker
 * <p>
 * LICENSE NOTE: This is the modified version of {@link org.apache.lucene.luke.app.desktop.MessageBroker}.
 */
@Service(Service.Level.APP)
public final class MessageBroker {

  private List<MessageReceiver> receivers = new ArrayList<>();

  public static MessageBroker getInstance() {
    return ApplicationManager.getApplication().getService(MessageBroker.class);
  }

  public void registerReceiver(MessageReceiver receiver) {
    receivers.add(receiver);
  }

  public void showStatusMessage(String message) {
    for (MessageReceiver receiver : receivers) {
      receiver.showStatusMessage(message);
    }
  }

  public void showUnknownErrorMessage() {
    for (MessageReceiver receiver : receivers) {
      receiver.showUnknownErrorMessage();
    }
  }

  public void clearStatusMessage() {
    for (MessageReceiver receiver : receivers) {
      receiver.clearStatusMessage();
    }
  }

  public void clear() {
    receivers.clear();
  }

  /** Message receiver in charge of rendering the message. */
  public interface MessageReceiver {
    void showStatusMessage(String message);

    void showUnknownErrorMessage();

    void clearStatusMessage();
  }
}
