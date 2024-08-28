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
package org.apache.zeppelin.rest;

import com.google.gson.Gson;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.zeppelin.MiniZeppelinServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class KnoxRestApiTest extends AbstractTestRestApi {
  private final String knoxCookie = "hadoop-jwt=eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsImlzcyI" +
          "6IktOT1hTU08iLCJleHAiOjE1MTM3NDU1MDd9.E2cWQo2sq75h0G_9fc9nWkL0SFMI5x_-Z0Zzr0NzQ86X4jfx" +
          "liWYjr0M17Bm9GfPHRRR66s7YuYXa6DLbB4fHE0cyOoQnkfJFpU_vr1xhy0_0URc5v-Gb829b9rxuQfjKe-37h" +
          "qbUdkwww2q6QQETVMvzp0rQKprUClZujyDvh0;";

  private static final Logger LOGGER = LoggerFactory.getLogger(KnoxRestApiTest.class);

  Gson gson = new Gson();
  private static MiniZeppelinServer zepServer;

  @BeforeAll
  public static void init() throws Exception {
    zepServer = new MiniZeppelinServer(KnoxRestApiTest.class.getSimpleName());
    zepServer.addConfigFile("shiro.ini", AbstractTestRestApi.ZEPPELIN_SHIRO_KNOX);
    zepServer.addConfigFile("knox-sso.pem", AbstractTestRestApi.KNOW_SSO_PEM_CERTIFICATE);
    zepServer.start();
  }

  @BeforeEach
  void setup() {
    zConf = zepServer.getZeppelinConfiguration();
  }

  @AfterAll
  public static void destroy() throws Exception {
    zepServer.destroy();
  }


  @Test
  @Disabled
  void testThatOtherUserCanAccessNoteIfPermissionNotSet() throws IOException {
    CloseableHttpResponse loginWithoutCookie = httpGet("/api/security/ticket");
    Map result = gson.fromJson(EntityUtils.toString(loginWithoutCookie.getEntity(), StandardCharsets.UTF_8), Map.class);

    assertThat("response contains redirect URL",
        ((Map) result.get("body")).get("redirectURL").toString(), equalTo(
            "https://domain.example.com/gateway/knoxsso/knoxauth/login.html?originalUrl="));

    CloseableHttpResponse loginWithCookie = httpGet("/api/security/ticket", "", "", knoxCookie);
    result = gson.fromJson(EntityUtils.toString(loginWithCookie.getEntity(), StandardCharsets.UTF_8), Map.class);

    assertThat("User logged in as admin",
        ((Map) result.get("body")).get("principal").toString(), equalTo("admin"));

    System.out.println(result);
  }
}
