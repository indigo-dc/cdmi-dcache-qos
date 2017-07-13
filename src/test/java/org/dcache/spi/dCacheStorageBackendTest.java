package org.dcache.spi;

import static org.indigo.cdmi.BackendCapability.CapabilityType.CONTAINER;
import static org.indigo.cdmi.BackendCapability.CapabilityType.DATAOBJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.List;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.dcache.spi.exception.SpiException;
import org.dcache.spi.util.HttpUtils;
import org.dcache.spi.util.ParseUtils;
import org.indigo.cdmi.BackEndException;
import org.indigo.cdmi.BackendCapability;
import org.indigo.cdmi.CdmiObjectStatus;
import org.indigo.cdmi.spi.StorageBackend;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpUtils.class})
@PowerMockIgnore({"org.apache.http.ssl.*", "javax.net.ssl.*", "javax.crypto.*, java.security.*"})
public class dCacheStorageBackendTest {

  private static String LIST_CAP_DIR = "{\"name\":[\"disk\",\"tape\"],\"message\":\"successful\",\"status\":\"200\"}";
  private static String CAP_DIR_TAPE = "{\"status\":\"200\",\"message\":\"successful\",\"backendCapability\":{\"name\":\"disk\",\"transition\":[\"tape\"],\"metadata\":{\"cdmi_data_redundancy_provided\":\"1\",\"cdmi_geographic_placement_provided\":[\"DE\"],\"cdmi_latency_provided\":\"100\"}}}";
  private static String CAP_DIR_DISK = "{\"status\":\"200\",\"message\":\"successful\",\"backendCapability\":{\"name\":\"tape\",\"transition\":[\"disk\"],\"metadata\":{\"cdmi_data_redundancy_provided\":\"1\",\"cdmi_geographic_placement_provided\":[\"DE\"],\"cdmi_latency_provided\":\"600000\"}}}";
  private static String LIST_CAP_FILE = "{\"name\":[\"disk\",\"tape\",\"disk+tape\"],\"message\":\"successful\",\"status\":\"200\"}";
  private static String CAP_FILE_DISK = "{\"status\":\"200\",\"message\":\"successful\",\"backendCapability\":{\"name\":\"disk\",\"transition\":[\"tape\",\"disk+tape\"],\"metadata\":{\"cdmi_data_redundancy_provided\":\"1\",\"cdmi_geographic_placement_provided\":[\"DE\"],\"cdmi_latency_provided\":\"100\"}}}";
  private static String CAP_FILE_TAPE = "{\"status\":\"200\",\"message\":\"successful\",\"backendCapability\":{\"name\":\"tape\",\"transition\":[\"disk+tape\"],\"metadata\":{\"cdmi_data_redundancy_provided\":\"1\",\"cdmi_geographic_placement_provided\":[\"DE\"],\"cdmi_latency_provided\":\"600000\"}}}";
  private static String CAP_FILE_BOTH = "{\"status\":\"200\",\"message\":\"successful\",\"backendCapability\":{\"name\":\"disk+tape\",\"transition\":[\"tape\"],\"metadata\":{\"cdmi_data_redundancy_provided\":\"2\",\"cdmi_geographic_placement_provided\":[\"DE\"],\"cdmi_latency_provided\":\"100\"}}}";
  private static String UPDATE_CDMI = "{\"status\":\"200\",\"message\":\"Transition was successful\"}";
  private static String QOS_STATUS = "{\"fileMimeType\":\"application/vnd.dcache.folder\",\"currentQos\":\"disk\",\"fileType\":\"DIR\",\"mtime\":1495803045999,\"creationTime\":1461676283248,\"size\":512}";
  private static String CHILD_INFO = "{\"fileMimeType\":\"application/vnd.dcache.folder\",\"children\":[{\"fileName\":\"random.img\",\"fileMimeType\":\"application/octet-stream\",\"fileType\":\"REGULAR\",\"mtime\":1484755793329,\"creationTime\":1484755792650,\"size\":79},{\"fileName\":\"mountain.jpg\",\"fileMimeType\":\"image/jpeg\",\"fileType\":\"REGULAR\",\"mtime\":1495803046091,\"creationTime\":1495803045999,\"size\":375694}],\"fileType\":\"DIR\",\"mtime\":1495803045999,\"creationTime\":1461676283248,\"size\":512}";
  private static String FILE_INFO = "{\"fileMimeType\":\"application/vnd.dcache.folder\",\"fileType\":\"DIR\",\"mtime\":1495803045999,\"creationTime\":1461676283248,\"size\":512}";
  private final String url = "https://someurl";
  private JSONObject listCapDirectory = new JSONObject(LIST_CAP_DIR);
  private JSONObject capDirDisk = new JSONObject(CAP_DIR_DISK);
  private JSONObject capDirTape = new JSONObject(CAP_DIR_TAPE);

  private JSONObject listCapFile = new JSONObject(LIST_CAP_FILE);
  private JSONObject capFileDisk = new JSONObject(CAP_FILE_DISK);
  private JSONObject capFileTape = new JSONObject(CAP_FILE_TAPE);
  private JSONObject capFileDiskAndTape = new JSONObject(CAP_FILE_BOTH);

  private JSONObject updateCdmiResponse = new JSONObject(UPDATE_CDMI);

  private JSONObject qosStatus = new JSONObject(QOS_STATUS);
  private JSONObject children = new JSONObject(CHILD_INFO);
  private JSONObject fileInfo = new JSONObject(FILE_INFO);

  private BackendCapability backCapDirDisk = ParseUtils
      .backendCapabilityFromJson(capDirDisk, CONTAINER);
  private BackendCapability backCapDirTape = ParseUtils
      .backendCapabilityFromJson(capDirTape, CONTAINER);
  private BackendCapability backCapFileDisk = ParseUtils
      .backendCapabilityFromJson(capFileDisk, DATAOBJECT);
  private BackendCapability backCapFileTape = ParseUtils
      .backendCapabilityFromJson(capFileTape, DATAOBJECT);
  private BackendCapability backCapFileDiskTape = ParseUtils
      .backendCapabilityFromJson(capFileDiskAndTape,
          DATAOBJECT);

  private StorageBackend storageBackend;

  @Before
  public void setUp() throws Exception {
    //storageBackend = new dCacheStorageBackendFactory().createStorageBackend(new HashMap<>());
    storageBackend = new dCacheStorageBackend();
    mockStatic(HttpUtils.class);
  }

  @After
  public void tearDown() throws Exception {

  }

  @Test
  public void testGetCapabilities() throws Exception {
    when(HttpUtils.class, "execute", Mockito.any(HttpUriRequest.class))
        .thenReturn(listCapDirectory)
        .thenReturn(capDirDisk)
        .thenReturn(capDirTape)
        .thenReturn(listCapFile)
        .thenReturn(capFileDisk)
        .thenReturn(capFileTape)
        .thenReturn(capFileDiskAndTape);

    when(HttpUtils.class, "addBackendCapability", Mockito.anyString(), Mockito.anyList(),
        Mockito.any())
        .thenCallRealMethod();

    when(HttpUtils.class, "getBackendCapabilities", Mockito.anyString()).thenCallRealMethod();

    List<BackendCapability> caps = storageBackend.getCapabilities();

    assertEquals(caps.size(), 5);
    assertEquals(caps.get(0).getName(), backCapDirDisk.getName());
    assertEquals(caps.get(0).getType(), backCapDirDisk.getType());
    assertEquals(caps.get(0).getCapabilities(), backCapDirDisk.getCapabilities());
    assertEquals(((JSONArray) caps.get(0).getMetadata().get("cdmi_capabilities_allowed")).get(0),
        ((JSONArray) backCapDirDisk.getMetadata().get("cdmi_capabilities_allowed")).get(0));

    assertEquals(caps.get(1).getName(), backCapDirTape.getName());
    assertEquals(caps.get(1).getType(), backCapDirTape.getType());
    assertEquals(caps.get(1).getCapabilities(), backCapDirTape.getCapabilities());
    assertEquals(((JSONArray) caps.get(1).getMetadata().get("cdmi_capabilities_allowed")).get(0),
        ((JSONArray) backCapDirTape.getMetadata().get("cdmi_capabilities_allowed")).get(0));

    assertEquals(caps.get(2).getName(), backCapFileDisk.getName());
    assertEquals(caps.get(2).getType(), backCapFileDisk.getType());
    assertEquals(caps.get(2).getCapabilities(), backCapFileDisk.getCapabilities());
    assertEquals(((JSONArray) caps.get(2).getMetadata().get("cdmi_capabilities_allowed")).get(0),
        ((JSONArray) backCapFileDisk.getMetadata().get("cdmi_capabilities_allowed")).get(0));

    assertEquals(caps.get(3).getName(), backCapFileTape.getName());
    assertEquals(caps.get(3).getType(), backCapFileTape.getType());
    assertEquals(caps.get(3).getCapabilities(), backCapFileTape.getCapabilities());
    assertEquals(((JSONArray) caps.get(3).getMetadata().get("cdmi_capabilities_allowed")).get(0),
        ((JSONArray) backCapFileTape.getMetadata().get("cdmi_capabilities_allowed")).get(0));

    assertEquals(caps.get(4).getName(), backCapFileDiskTape.getName());
    assertEquals(caps.get(4).getType(), backCapFileDiskTape.getType());
    assertEquals(caps.get(4).getCapabilities(), backCapFileDiskTape.getCapabilities());
    assertEquals(((JSONArray) caps.get(4).getMetadata().get("cdmi_capabilities_allowed")).get(0),
        ((JSONArray) backCapFileDiskTape.getMetadata().get("cdmi_capabilities_allowed")).get(0));
  }

  @Test(expected = BackEndException.class)
  public void testGetCapabilitiesException() throws Exception {
    when(HttpUtils.class, "getBackendCapabilities", Mockito.anyString())
        .thenThrow(SpiException.class);
    storageBackend.getCapabilities();
  }

  @Test(expected = BackEndException.class)
  public void testUpdateCdmiObjectException() throws Exception {
    when(HttpUtils.class, "execute", Mockito.any(HttpPost.class), Mockito.anyList())
        .thenThrow(SpiException.class);
    storageBackend.updateCdmiObject("anypath", "anyString");
  }

  @Test
  public void testUpdateCdmiObject() throws Exception {
    when(HttpUtils.class, "execute", Mockito.any(HttpPost.class), Mockito.anyList())
        .thenReturn(updateCdmiResponse);
    JSONObject response = ((dCacheStorageBackend) storageBackend)
        .updateCdmiObjectStub("anypath", "anyString");

    assertEquals(response.has("status"), true);
    assertEquals(response.get("status"), "200");
    assertEquals(response.has("message"), true);
    assertEquals(response.get("message"), "Transition was successful");
  }

  @Test
  public void testGetCurrentStatus() throws Exception {
    when(HttpUtils.class, "currentStatus", Mockito.anyString())
        .thenReturn(qosStatus)
        .thenReturn(fileInfo)
        .thenReturn(children);

    when(HttpUtils.class, "monitoredAttributes", Mockito.anyString())
        .thenReturn(ParseUtils.metadataFromJson(capDirDisk));

    CdmiObjectStatus status = storageBackend.getCurrentStatus("anyPath");

    assertNotNull(status);
    assertEquals(status.getCurrentCapabilitiesUri(), "/cdmi_capabilities/container/disk");
    assertEquals(status.getMonitoredAttributes().size(), 3);
    assertEquals(status.getChildren().size(), 2);
    assertEquals(status.getExportAttributes().size(), 1);
  }

  @Test(expected = BackEndException.class)
  public void testGetCurrentStatusWithSpiException() throws Exception {
    when(HttpUtils.class, "currentStatus", Mockito.anyString()).thenThrow(SpiException.class);
    storageBackend.getCurrentStatus("anyPath");
  }

  @Test(expected = BackEndException.class)
  public void testGetCurrentStatusWithSpiException2() throws Exception {
    when(HttpUtils.class, "currentStatus", Mockito.anyString())
        .thenReturn(qosStatus)
        .thenReturn(fileInfo)
        .thenReturn(children);

    when(HttpUtils.class, "monitoredAttributes", Mockito.anyString())
        .thenThrow(SpiException.class);

    storageBackend.getCurrentStatus("anyPath");
  }
}