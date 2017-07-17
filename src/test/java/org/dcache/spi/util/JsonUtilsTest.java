package org.dcache.spi.util;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;

public class JsonUtilsTest {

  @Test
  public void testTargetCapUriToJsonOld() throws Exception {
    String capUri = "/cdmi_capabilities/contaner/disk+tape";

    String targetCap = JsonUtils.targetCapUriToJsonOld(capUri);

    assertThat(targetCap, isJson());
    assertThat(targetCap, hasJsonPath("$.update"));
    assertThat(targetCap, hasJsonPath("$.update", equalTo("disk+tape")));
  }
}
