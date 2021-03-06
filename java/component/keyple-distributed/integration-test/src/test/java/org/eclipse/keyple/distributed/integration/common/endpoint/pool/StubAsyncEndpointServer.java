/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.distributed.integration.common.endpoint.pool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.keyple.distributed.MessageDto;
import org.eclipse.keyple.distributed.PoolRemotePluginClient;
import org.eclipse.keyple.distributed.impl.PoolLocalServiceServerUtils;
import org.eclipse.keyple.distributed.integration.common.util.JacksonParser;
import org.eclipse.keyple.distributed.integration.common.util.NamedThreadFactory;
import org.eclipse.keyple.distributed.spi.AsyncEndpointServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulate a async server to test {@link PoolRemotePluginClient}. Send and receive asynchronously
 * serialized {@link MessageDto} with connected {@link StubAsyncEndpointClient}
 */
public class StubAsyncEndpointServer implements AsyncEndpointServer {

  private static final Logger logger = LoggerFactory.getLogger(StubAsyncEndpointServer.class);
  private final Map<String, StubAsyncEndpointClient> clients; // sessionId_client
  private final Map<String, Integer> messageCounts; // sessionId_counts
  private final ExecutorService taskPool;
  private final String localServiceName;

  public StubAsyncEndpointServer(String localServiceName) {
    clients = new ConcurrentHashMap<String, StubAsyncEndpointClient>();
    messageCounts = new ConcurrentHashMap<String, Integer>();
    taskPool = Executors.newCachedThreadPool(new NamedThreadFactory("server-async-pool"));
    this.localServiceName = localServiceName;
  }

  /** Simulate a close socket operation */
  void close(String sessionId) {
    messageCounts.remove(sessionId);
    clients.remove(sessionId);
    PoolLocalServiceServerUtils.getAsyncNode(localServiceName).onClose(sessionId);
  }

  /**
   * Simulate data received by the socket
   *
   * @param jsonData incoming json data
   */
  void onData(final String jsonData, final StubAsyncEndpointClient client) {
    final MessageDto message = JacksonParser.fromJson(jsonData);
    clients.put(message.getSessionId(), client);
    taskPool.submit(
        new Runnable() {
          @Override
          public void run() {
            PoolLocalServiceServerUtils.getAsyncNode(localServiceName).onMessage(message);
          }
        });
  }

  @Override
  public void sendMessage(final MessageDto msg) {
    final String data = JacksonParser.toJson(msg);
    logger.trace("Data sent to client {}", data);
    final StubAsyncEndpointClient client = clients.get(msg.getSessionId());
    taskPool.submit(
        new Runnable() {
          @Override
          public void run() {
            try {
              client.onMessage(data);
            } catch (Throwable t) {
              PoolLocalServiceServerUtils.getAsyncNode(localServiceName)
                  .onError(msg.getSessionId(), t);
            }
          }
        });
  }
}
