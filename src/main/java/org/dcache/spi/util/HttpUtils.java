package org.dcache.spi.util;

import com.google.common.base.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.indigo.cdmi.BackendCapability;
import org.indigo.cdmi.BackendCapability.CapabilityType;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;

import java.io.IOException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dcache.spi.exception.SpiException;

import static org.indigo.cdmi.BackendCapability.CapabilityType.CONTAINER;
import static org.indigo.cdmi.BackendCapability.CapabilityType.DATAOBJECT;

public class HttpUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(HttpUtils.class);

    private static final HttpClient client = HttpClientBuilder.create().build();
    private static BasicScheme scheme = new BasicScheme(Charsets.UTF_8);
    private static UsernamePasswordCredentials clientCreds;

    public static List<BackendCapability> getBackendCapabilities(String url) throws SpiException
    {
        List<BackendCapability> backendCapabilities = new ArrayList<>();
        addBackendCapability(url, backendCapabilities, CONTAINER);
        addBackendCapability(url, backendCapabilities, DATAOBJECT);
        return backendCapabilities;
    }

    public static JSONObject currentStatus(String url) throws SpiException {
        HttpGet request = new HttpGet(url);
        return HttpUtils.execute(request);
    }

    public static Map<String, String> monitoredAttributes(String capabilityUri)
            throws SpiException {
        HttpGet dirRequest = new HttpGet(capabilityUri);
        return ParseUtils.metadataFromJson(HttpUtils.execute(dirRequest));
    }

    public static String getCapabilityUri(String capabilityUriPrefix, String fileType, String curQos) {
        return capabilityUriPrefix +
                fileTypeToCapString(fileType) + "/" +
                curQos;
    }

    public static boolean statusOk(HttpResponse response)
    {
        return (response.getStatusLine().getStatusCode() == 200);
    }

    public static void checkStatusError(HttpResponse response) throws SpiException, IOException
    {
        if (response.getStatusLine().getStatusCode() == 401 ||
                response.getStatusLine().getStatusCode() == 400 ||
                response.getStatusLine().getStatusCode() == 501 ||
                response.getStatusLine().getStatusCode() == 404 ||
                response.getStatusLine().getStatusCode() == 500 ) {
            throw new SpiException(ParseUtils.responseAsJson(response.getEntity()).getString("error"));
        }
    }

    public static JSONObject execute(HttpUriRequest request, List<Header> headers) throws SpiException {
        for (Header header: headers) {
            request.addHeader(header);
        }
        return execute(request);
    }

    public static JSONObject execute(HttpUriRequest request) throws SpiException {
        try {
            Subject subject = Subject.getSubject(AccessController.getContext());
            LOG.debug("Subject credentials = {}", subject);

            if (subject == null && clientCreds != null) {
                request.addHeader(scheme.authenticate(clientCreds, request, new BasicHttpContext()));
            } else if (subject != null){
                String bearer = (String) subject.getPrivateCredentials().stream().findFirst().get();
                if (bearer != null) {
                    request.addHeader("Authorization", "Bearer " + bearer);
                }
                LOG.debug("Http Request looks like this: {}", request);
            }

            HttpResponse httpResponse = client.execute(request);

            if (statusOk(httpResponse)) {
                return ParseUtils.responseAsJson(httpResponse.getEntity());
            } else {
                LOG.warn("{}  {}: {} ", request.getMethod(), request.getURI(), httpResponseToString(httpResponse));
                checkStatusError(httpResponse);
            }
        } catch (IOException | JSONException | AuthenticationException ie ) {
            throw new SpiException(request.getURI(), request.getMethod(), ie.getMessage());
        }
        return null;
    }

    private static String httpResponseToString(HttpResponse response) throws IOException {
        return EntityUtils.toString(response.getEntity(), "UTF-8");
    }

    private static void addBackendCapability(String url,
                                             List<BackendCapability> backendCapabilities,
                                             CapabilityType type) throws SpiException
    {
        HttpGet request = new HttpGet(url + backendCapTypeTofileType(type));
        JSONObject response = execute(request);

        try {
            List<String> capabilities = JsonUtils.jsonArrayToStringList(response.getJSONArray("name").getJSONArray(0));
            for (String capability: capabilities) {
                request = new HttpGet(url + backendCapTypeTofileType(type) + "/" + capability);
                response = execute(request);

                BackendCapability backendCapability = ParseUtils.backendCapabilityFromJson(response, type);
                backendCapabilities.add(backendCapability);
            }
        } catch (JSONException je) {
            throw new SpiException(je.getMessage());
        }

    }

    public static String backendCapTypeTofileType(CapabilityType type) {
        switch (type) {
            case CONTAINER:
                return "directory";
            case DATAOBJECT:
                return "file";
            default:
                return null;
        }
    }

    private static String fileTypeToCapString (String type) {
        switch (type) {
        case "DIR":
            return "directory";
        case "REGULAR":
            return "file";
        default:
            return null;
        }
    }

    public static void setCredentials(String username, String password) {
        if (username != null && password != null) {
            clientCreds = new UsernamePasswordCredentials(username, password);
        } else {
            clientCreds = null;
        }
    }
}
