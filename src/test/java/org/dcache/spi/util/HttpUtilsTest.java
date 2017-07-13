package org.dcache.spi.util;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.indigo.cdmi.BackendCapability;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dcache.spi.exception.SpiException;

import static org.indigo.cdmi.BackendCapability.CapabilityType.CONTAINER;
import static org.indigo.cdmi.BackendCapability.CapabilityType.DATAOBJECT;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HttpUtils.class})
@PowerMockIgnore({"org.apache.http.ssl.*", "javax.net.ssl.*", "javax.crypto.*, java.security.*"})
public class HttpUtilsTest {
    private static String LIST_CAP_DIR = "{\"name\":[\"disk\",\"tape\"],\"message\":\"successful\",\"status\":\"200\"}";
    private static String CAP_DIR_TAPE = "{\"status\":\"200\",\"message\":\"successful\",\"backendCapability\":{\"name\":\"disk\",\"transition\":[\"tape\"],\"metadata\":{\"cdmi_data_redundancy_provided\":\"1\",\"cdmi_geographic_placement_provided\":[\"DE\"],\"cdmi_latency_provided\":\"100\"}}}";
    private static String CAP_DIR_DISK = "{\"status\":\"200\",\"message\":\"successful\",\"backendCapability\":{\"name\":\"tape\",\"transition\":[\"disk\"],\"metadata\":{\"cdmi_data_redundancy_provided\":\"1\",\"cdmi_geographic_placement_provided\":[\"DE\"],\"cdmi_latency_provided\":\"600000\"}}}";
    private static String LIST_CAP_FILE = "{\"name\":[\"disk\",\"tape\",\"disk+tape\"],\"message\":\"successful\",\"status\":\"200\"}";
    private static String CAP_FILE_DISK = "{\"status\":\"200\",\"message\":\"successful\",\"backendCapability\":{\"name\":\"disk\",\"transition\":[\"tape\",\"disk+tape\"],\"metadata\":{\"cdmi_data_redundancy_provided\":\"1\",\"cdmi_geographic_placement_provided\":[\"DE\"],\"cdmi_latency_provided\":\"100\"}}}";
    private static String CAP_FILE_TAPE = "{\"status\":\"200\",\"message\":\"successful\",\"backendCapability\":{\"name\":\"tape\",\"transition\":[\"disk+tape\"],\"metadata\":{\"cdmi_data_redundancy_provided\":\"1\",\"cdmi_geographic_placement_provided\":[\"DE\"],\"cdmi_latency_provided\":\"600000\"}}}";
    private static String CAP_FILE_BOTH = "{\"status\":\"200\",\"message\":\"successful\",\"backendCapability\":{\"name\":\"disk+tape\",\"transition\":[\"tape\"],\"metadata\":{\"cdmi_data_redundancy_provided\":\"2\",\"cdmi_geographic_placement_provided\":[\"DE\"],\"cdmi_latency_provided\":\"100\"}}}";
    private final String url = "https://someurl";
    private JSONObject listCapDirectory = new JSONObject(LIST_CAP_DIR);
    private JSONObject capDirDisk = new JSONObject(CAP_DIR_DISK);
    private JSONObject capDirTape = new JSONObject(CAP_DIR_TAPE);

    private JSONObject listCapFile = new JSONObject(LIST_CAP_FILE);
    private JSONObject capFileDisk = new JSONObject(CAP_FILE_DISK);
    private JSONObject capFileTape = new JSONObject(CAP_FILE_TAPE);
    private JSONObject capFileDiskAndTape = new JSONObject(CAP_FILE_BOTH);


    private BackendCapability backCapDirDisk = ParseUtils.backendCapabilityFromJson(capDirDisk, CONTAINER);
    private BackendCapability backCapDirTape = ParseUtils.backendCapabilityFromJson(capDirTape, CONTAINER);
    private BackendCapability backCapFileDisk = ParseUtils.backendCapabilityFromJson(capFileDisk, DATAOBJECT);
    private BackendCapability backCapFileTape = ParseUtils.backendCapabilityFromJson(capFileTape, DATAOBJECT);
    private BackendCapability backCapFileDiskTape = ParseUtils.backendCapabilityFromJson(capFileDiskAndTape,
            DATAOBJECT);

    @Before
    public void setUp() throws Exception {
        mockStatic(HttpUtils.class);
    }

    @Test
    public void testAddBackendCapability() throws Exception {
        ArrayList<BackendCapability> caps = new ArrayList<>(2);

        when(HttpUtils.class, "execute", Mockito.any(HttpUriRequest.class))
                .thenReturn(listCapDirectory)
                .thenReturn(capDirDisk)
                .thenReturn(capDirTape);

        when(HttpUtils.class, "addBackendCapability", Mockito.anyString(), Mockito.anyList(), Mockito.any())
                .thenCallRealMethod();

        HttpUtils.addBackendCapability(url, caps, CONTAINER);

        assertEquals(caps.size(), 2);

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
    }

    @Test(expected = SpiException.class)
    public void testAddBackendCapabilityExecuteException() throws Exception {
        ArrayList<BackendCapability> caps = new ArrayList<>(2);

        when(HttpUtils.class, "execute", Mockito.any(HttpUriRequest.class))
                .thenThrow(SpiException.class);

        when(HttpUtils.class, "addBackendCapability", Mockito.anyString(), Mockito.anyList(), Mockito.any())
                .thenCallRealMethod();
        HttpUtils.addBackendCapability(url, caps, CONTAINER);
    }

    @Test
    public void testGetBackendCapabilities() throws Exception {
        when(HttpUtils.class, "execute", Mockito.any(HttpUriRequest.class))
                .thenReturn(listCapDirectory)
                .thenReturn(capDirDisk)
                .thenReturn(capDirTape)
                .thenReturn(listCapFile)
                .thenReturn(capFileDisk)
                .thenReturn(capFileTape)
                .thenReturn(capFileDiskAndTape);

        when(HttpUtils.class, "addBackendCapability", Mockito.anyString(), Mockito.anyList(), Mockito.any())
                .thenCallRealMethod();

        when(HttpUtils.class, "getBackendCapabilities", Mockito.anyString()).thenCallRealMethod();

        List<BackendCapability> caps = HttpUtils.getBackendCapabilities(url);

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


    @Test(expected = SpiException.class)
    public void testGetBackendCapabilitiesWithException() throws Exception {
        when(HttpUtils.class, "execute", Mockito.any(HttpUriRequest.class))
                .thenThrow(SpiException.class);

        when(HttpUtils.class, "addBackendCapability", Mockito.anyString(), Mockito.anyList(), Mockito.any())
                .thenCallRealMethod();

        when(HttpUtils.class, "getBackendCapabilities", Mockito.anyString()).thenThrow(SpiException.class);

        HttpUtils.getBackendCapabilities(url);
    }

    @Test(expected = SpiException.class)
    public void testCurrentStatusWithSpiException() throws Exception {
        when(HttpUtils.class, "execute", Mockito.any(HttpUriRequest.class))
                .thenThrow(SpiException.class);

        when(HttpUtils.class, "currentStatus", Mockito.anyString())
                .thenCallRealMethod();

        HttpUtils.currentStatus(url);
    }

    @Test(expected = SpiException.class)
    public void testCurrentStatusWithIOException() throws Exception {
        stub(method(HttpClient.class, "execute", HttpUriRequest.class)).toThrow(new IOException());

        when(HttpUtils.class, "currentStatus", Mockito.anyString())
                .thenCallRealMethod();
        when(HttpUtils.class, "execute", Mockito.any(HttpUriRequest.class))
                .thenCallRealMethod();

        HttpUtils.currentStatus(url);
    }

    @Test(expected = SpiException.class)
    public void testCurrentStatusWithJsonException() throws Exception {
        stub(method(ParseUtils.class, "responseAsJson", HttpEntity.class))
                .toThrow(new JSONException("Error"));

        stub(method(HttpClient.class, "execute", HttpUriRequest.class)).toThrow(new IOException());
        when(HttpUtils.class, "currentStatus", Mockito.anyString())
                .thenCallRealMethod();
        when(HttpUtils.class, "execute", Mockito.any(HttpUriRequest.class))
                .thenCallRealMethod();

        HttpUtils.currentStatus(url);
    }
}