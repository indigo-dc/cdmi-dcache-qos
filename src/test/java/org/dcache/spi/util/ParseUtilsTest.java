package org.dcache.spi.util;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static org.dcache.spi.util.ParseUtils.backendCapabilityFromJson;
import static org.indigo.cdmi.BackendCapability.CapabilityType.DATAOBJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.dcache.spi.dCacheStorageBackend;
import org.indigo.cdmi.BackendCapability;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class ParseUtilsTest {

  private static final HashMap<String, Object> capabilities = dCacheStorageBackend.capabilities;
  private static final HashMap<String, Object> exports = dCacheStorageBackend.exports;
  private static String CAP_DISK_TAPE = "{\"backendCapability\":{\"metadata\":{\"cdmi_data_redundancy_provided\":\"2\",\"cdmi_geographic_placement_provided\":[\"DE\"],\"cdmi_latency_provided\":\"100\"},\"name\":\"disk+tape\",\"transition\":[\"tape\"]}}";
  private static String CAP_DISK = "{\"backendCapability\":{\"metadata\":{\"cdmi_data_redundancy_provided\":\"1\",\"cdmi_geographic_placement_provided\":[\"DE\"],\"cdmi_latency_provided\":\"100\"},\"name\":\"disk\",\"transition\":[\"tape\",\"disk+tape\"]}}";

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testBackendCapabilityDiskAndTapeFromJson() throws Exception {
    assertThat(CAP_DISK_TAPE, isJson());

    JSONObject json = new JSONObject(CAP_DISK_TAPE);

    BackendCapability capability = backendCapabilityFromJson(json, DATAOBJECT);
    assertNotNull(capability);

    assertEquals(capability.getType(), DATAOBJECT);
    assertEquals(capability.getName(), "disk+tape");
    assertEquals(capability.getCapabilities(), capabilities);
    assertEquals(capability.getMetadata().get("cdmi_data_redundancy"),
        json.getJSONObject("backendCapability")
            .getJSONObject("metadata")
            .getInt("cdmi_data_redundancy_provided"));
    assertEquals(capability.getMetadata().get("cdmi_geographic_placement"),
        json.getJSONObject("backendCapability")
            .getJSONObject("metadata")
            .getJSONArray(
                "cdmi_geographic_placement_provided"));
  }

  @Test
  public void testBackendCapabilityDiskFromJson() throws Exception {
    assertThat(CAP_DISK, isJson());

    JSONObject json = new JSONObject(CAP_DISK);
    BackendCapability capability = backendCapabilityFromJson(json, DATAOBJECT);
    assertNotNull(capability);

    assertEquals(capability.getType(), DATAOBJECT);
    assertEquals(capability.getName(), "disk");
    assertEquals(capability.getCapabilities(), capabilities);
    assertEquals(capability.getMetadata().get("cdmi_data_redundancy"),
        json.getJSONObject("backendCapability")
            .getJSONObject("metadata")
            .getInt("cdmi_data_redundancy_provided"));
    assertEquals(capability.getMetadata().get("cdmi_geographic_placement"),
        json.getJSONObject("backendCapability")
            .getJSONObject("metadata")
            .getJSONArray(
                "cdmi_geographic_placement_provided"));
  }

  @Test
  public void testMetadataFromJson() throws Exception {
    assertThat(CAP_DISK, isJson());

    Map<String, Object> metadata = ParseUtils.metadataFromJson(new JSONObject(CAP_DISK));

    assertEquals(metadata.isEmpty(), false);
    assertEquals(metadata.get("cdmi_geographic_placement_provided") instanceof JSONArray, true);
    assertEquals(metadata.get("cdmi_data_redundancy_provided"), new Integer(1));
    assertEquals(metadata.get("cdmi_latency_provided"), new Long(100));
  }
}