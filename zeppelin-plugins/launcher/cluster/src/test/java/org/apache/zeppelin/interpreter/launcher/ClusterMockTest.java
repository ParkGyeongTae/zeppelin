/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.zeppelin.interpreter.launcher;

import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.apache.zeppelin.cluster.ClusterManagerClient;
import org.apache.zeppelin.cluster.ClusterManagerServer;
import org.apache.zeppelin.cluster.meta.ClusterMeta;
import org.apache.zeppelin.cluster.meta.ClusterMetaType;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.interpreter.remote.RemoteInterpreterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.apache.zeppelin.cluster.meta.ClusterMeta.OFFLINE_STATUS;
import static org.apache.zeppelin.cluster.meta.ClusterMeta.ONLINE_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ClusterMockTest {
  private static Logger LOGGER = LoggerFactory.getLogger(ClusterMockTest.class);

  private static ClusterManagerServer clusterServer = null;
  private static ClusterManagerClient clusterClient = null;

  protected static ZeppelinConfiguration zConf = null;

  static String zServerHost;
  static int zServerPort;
  static final String metaKey = "ClusterMockKey";

  static TServerSocket tSocket = null;

  public static void startCluster() throws IOException, InterruptedException {
    LOGGER.info("startCluster >>>");

    zConf = ZeppelinConfiguration.load();

    // Set the cluster IP and port
    zServerHost = RemoteInterpreterUtils.findAvailableHostAddress();
    zServerPort = RemoteInterpreterUtils.findRandomAvailablePortOnAllLocalInterfaces();
    zConf.setClusterAddress(zServerHost + ":" + zServerPort);

    // mock cluster manager server
    clusterServer = ClusterManagerServer.getInstance(zConf);
    clusterServer.start();

    // mock cluster manager client
    clusterClient = ClusterManagerClient.getInstance(zConf);
    clusterClient.start(metaKey);

    // Waiting for cluster startup
    int wait = 0;
    while (wait++ < 100) {
      if (clusterServer.isClusterLeader()
          && clusterServer.raftInitialized()
          && clusterClient.raftInitialized()) {
        LOGGER.info("wait {}(ms) found cluster leader", wait * 3000);
        break;
      }
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    assertEquals(true, clusterServer.isClusterLeader());

    try {
      tSocket = new TServerSocket(0);
    } catch (TTransportException e) {
      throw new IOException("Fail to create TServerSocket", e);
    }

    LOGGER.info("startCluster <<<");
  }

  public static void stopCluster() {
    LOGGER.info("stopCluster >>>");
    if (null != clusterClient) {
      clusterClient.shutdown();
    }
    if (null != clusterClient) {
      clusterServer.shutdown();
    }

    tSocket.close();
    LOGGER.info("stopCluster <<<");
  }

  public void getServerMeta() {
    LOGGER.info("serverMeta >>>");

    // Get metadata for all services
    Map<String, Map<String, Object>> meta =
        clusterClient.getClusterMeta(ClusterMetaType.SERVER_META, "");

    LOGGER.info(meta.toString());

    assertNotNull(meta);
    assertEquals(true, (meta instanceof Map));

    // Get metadata for the current service
    Map<String, Object> values = meta.get(zServerHost + ":" + zServerPort);
    assertEquals(true, (values instanceof Map));

    assertEquals(true, values.size() > 0);

    LOGGER.info("serverMeta <<<");
  }

  public void mockIntpProcessMeta(String metaKey, boolean online) {
    // mock IntpProcess Meta
    Map<String, Object> meta = new HashMap<>();
    meta.put(ClusterMeta.SERVER_HOST, "127.0.0.1");
    meta.put(ClusterMeta.SERVER_PORT, 6000);
    meta.put(ClusterMeta.INTP_TSERVER_HOST, "127.0.0.1");
    meta.put(ClusterMeta.INTP_TSERVER_PORT, tSocket.getServerSocket().getLocalPort());
    meta.put(ClusterMeta.CPU_CAPACITY, "CPU_CAPACITY");
    meta.put(ClusterMeta.CPU_USED, "CPU_USED");
    meta.put(ClusterMeta.MEMORY_CAPACITY, "MEMORY_CAPACITY");
    meta.put(ClusterMeta.MEMORY_USED, "MEMORY_USED");
    meta.put(ClusterMeta.LATEST_HEARTBEAT, LocalDateTime.now());

    if (online) {
      meta.put(ClusterMeta.STATUS, ONLINE_STATUS);
    } else {
      meta.put(ClusterMeta.STATUS, OFFLINE_STATUS);
    }
    // put IntpProcess Meta
    clusterClient.putClusterMeta(ClusterMetaType.INTP_PROCESS_META, metaKey, meta);

    // get IntpProcess Meta
    Map<String, Map<String, Object>> check =
        clusterClient.getClusterMeta(ClusterMetaType.INTP_PROCESS_META, metaKey);

    LOGGER.info(check.toString());

    assertNotNull(check);
    assertNotNull(check.get(metaKey));
    assertEquals(true, check.get(metaKey).size() == 10);
  }
}
