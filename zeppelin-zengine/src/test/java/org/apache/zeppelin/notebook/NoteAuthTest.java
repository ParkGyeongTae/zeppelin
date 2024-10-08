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

package org.apache.zeppelin.notebook;

import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class NoteAuthTest {
  private ZeppelinConfiguration zConf = mock(ZeppelinConfiguration.class);

  @Test
  void testAnonymous() {
    NoteAuth auth = new NoteAuth("note1", zConf);
    assertEquals(0, auth.getOwners().size());
    assertEquals(0, auth.getReaders().size());
    assertEquals(0, auth.getRunners().size());
    assertEquals(0, auth.getWriters().size());
  }

  @Test
  void testPublicNotes() {

    when(zConf.isNotebookPublic()).thenReturn(true);

    NoteAuth auth = new NoteAuth("note1", new AuthenticationInfo("TestUser"), zConf);
    assertEquals("note1", auth.getNoteId());
    assertEquals(1, auth.getOwners().size());
    assertTrue(auth.getOwners().contains("TestUser"));

    assertEquals(0, auth.getReaders().size());
    assertEquals(0, auth.getRunners().size());
    assertEquals(0, auth.getWriters().size());

    /*
     * simple Map check
     */
    assertEquals(4, auth.toMap().size());
    assertTrue(auth.toMap().get("owners").contains("TestUser"));
    assertTrue(auth.toMap().get("readers").isEmpty());
    assertTrue(auth.toMap().get("runners").isEmpty());
    assertTrue(auth.toMap().get("writers").isEmpty());
  }

  @Test
  void testNoPublicNotes() {

    when(zConf.isNotebookPublic()).thenReturn(false);

    NoteAuth auth = new NoteAuth("note1", new AuthenticationInfo("TestUser"), zConf);
    assertEquals(1, auth.getOwners().size());
    assertTrue(auth.getOwners().contains("TestUser"));

    assertEquals(1, auth.getReaders().size());
    assertTrue(auth.getReaders().contains("TestUser"));

    assertEquals(1, auth.getRunners().size());
    assertTrue(auth.getRunners().contains("TestUser"));

    assertEquals(1, auth.getWriters().size());
    assertTrue(auth.getWriters().contains("TestUser"));

    /*
     * simple Map check
     */
    assertEquals(4, auth.toMap().size());
    assertTrue(auth.toMap().get("owners").contains("TestUser"));
    assertTrue(auth.toMap().get("readers").contains("TestUser"));
    assertTrue(auth.toMap().get("runners").contains("TestUser"));
    assertTrue(auth.toMap().get("writers").contains("TestUser"));
  }

  @Test
  void testFoceLowerCaseUsers() {

    when(zConf.isNotebookPublic()).thenReturn(false);
    when(zConf.isUsernameForceLowerCase()).thenReturn(true);

    NoteAuth auth = new NoteAuth("note1", new AuthenticationInfo("TestUser"), zConf);
    assertEquals(1, auth.getOwners().size());
    assertTrue(auth.getOwners().contains("testuser"));

    assertEquals(1, auth.getReaders().size());
    assertTrue(auth.getReaders().contains("testuser"));

    assertEquals(1, auth.getRunners().size());
    assertTrue(auth.getRunners().contains("testuser"));

    assertEquals(1, auth.getWriters().size());
    assertTrue(auth.getWriters().contains("testuser"));
  }

  @Test
  void testMapConstructor() {
    when(zConf.isNotebookPublic()).thenReturn(false);

    NoteAuth auth = new NoteAuth("note1", getTestMap("TestUser", "TestGroup"), zConf);
    assertEquals(2, auth.getOwners().size());
    assertTrue(auth.getOwners().contains("TestUser"));
    assertTrue(auth.getOwners().contains("TestGroup"));

    assertEquals(2, auth.getReaders().size());
    assertTrue(auth.getReaders().contains("TestUser"));
    assertTrue(auth.getRunners().contains("TestGroup"));

    assertEquals(2, auth.getRunners().size());
    assertTrue(auth.getRunners().contains("TestUser"));
    assertTrue(auth.getRunners().contains("TestGroup"));

    assertEquals(2, auth.getWriters().size());
    assertTrue(auth.getWriters().contains("TestUser"));
    assertTrue(auth.getWriters().contains("TestGroup"));
  }

  private static Map<String, Set<String>> getTestMap(String user, String group) {
    Map<String, Set<String>> map = new HashMap<>();
    Set<String> readers = new HashSet<String>();
    readers.add(user);
    readers.add(group);
    Set<String> writers = new HashSet<String>(readers);
    Set<String> runners = new HashSet<String>(readers);
    Set<String> owners = new HashSet<String>(readers);
    map.put("readers", readers);
    map.put("writers", writers);
    map.put("runners", runners);
    map.put("owners", owners);
    return map;
  }
}
