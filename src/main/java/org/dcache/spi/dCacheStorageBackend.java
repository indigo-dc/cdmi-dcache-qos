/*
 * Copyright 2016 Deutsches Elektronen-Synchrotron (DESY)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package org.dcache.spi;

import com.google.common.annotations.VisibleForTesting;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.indigo.cdmi.BackEndException;
import org.indigo.cdmi.BackendCapability;
import org.indigo.cdmi.CdmiObjectStatus;
import org.indigo.cdmi.spi.StorageBackend;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dcache.spi.exception.SpiException;
import org.dcache.spi.util.HttpUtils;
import org.dcache.spi.util.JsonUtils;
import org.dcache.spi.util.ParseUtils;
import org.dcache.spi.util.PluginConfig;

import static com.google.common.base.Preconditions.checkNotNull;

public class dCacheStorageBackend implements StorageBackend {
    public static final String apiPrefix = "api/v1/";
    public static final String qosPrefixOld = "qos-management/qos/";
    public static final String qosPrefixNew = "namespace";
    public static final HashMap<String, Object> capabilities = new HashMap<>();
    public static final HashMap<String, Object> exports = new HashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(dCacheStorageBackend.class);
    private static String qosPrefix;
    private static boolean isRestApiNew;
    private static String scheme;

    static {
        //capabilities.put("cdmi_capabilities_templates", "true");
        //capabilities.put("cdmi_capabilities_exact_inherit", "true");
        capabilities.put("cdmi_data_redundancy", "true");
        capabilities.put("cdmi_geographic_placement", "true");
        capabilities.put("cdmi_capabilities_allowed", "true");
        capabilities.put("cdmi_latency", "true");
        exports.put("Network/WebHTTP", new JSONObject().put("identifier", "https://dcache-qos-01.desy.de/")
                                                       .put("permissions", "oidc"));
    }

    private PluginConfig config;
    private String DCACHE_SERVER;

    public dCacheStorageBackend() {
        config = new PluginConfig();
        checkNotNull(config.get("cdmi.dcache.rest.version"));
        checkNotNull(config.get("dcache.server.rest.scheme"));
        checkNotNull(config.get("dcache.server"));
        checkNotNull(config.get("dcache.server.rest.endpoint"));

        isRestApiNew = config.get("cdmi.dcache.rest.version").equals("new") ? true : false;
        scheme = config.get("dcache.server.rest.scheme") + "://";

        if (isRestApiNew) {
            qosPrefix = qosPrefixNew;
        } else {
            qosPrefix = qosPrefixOld;
        }

        DCACHE_SERVER = scheme + config.get("dcache.server") + ":" + config.get("dcache.server.rest.endpoint") + "/";
        HttpUtils.setCredentials(config.get("dcache.rest.user"), config.get("dcache.rest.password"));
    }

    @Override
    public List<BackendCapability> getCapabilities() throws BackEndException {
        String url = DCACHE_SERVER + apiPrefix + qosPrefixOld;
        try {
            LOG.debug("Fetching Cdmi Capabilities from {}", url);
            return HttpUtils.getBackendCapabilities(url);
        } catch (SpiException se) {
            LOG.error("Error Fetching Cdmi Capabilities {}", se.getMessage());
            throw new BackEndException(se.getMessage());
        }
    }

    @Override
    public void updateCdmiObject(String path, String targetCapabilityUri)
            throws BackEndException {
        LOG.debug("QoS Update of {} to target capability {}", path, targetCapabilityUri);

        JSONObject response = updateCdmiObjectStub(path, targetCapabilityUri);

        LOG.info("QoS Update of {} to {}: {}", path,
                targetCapabilityUri,
                response.getString(isRestApiNew ? "status" : "message"));
    }

    @VisibleForTesting
    JSONObject updateCdmiObjectStub(String path, String targetCapabilityUri)
            throws BackEndException {
        String url = DCACHE_SERVER + apiPrefix + (isRestApiNew ? qosPrefixNew : "qos-management/" + "namespace") + path;
        try {
            HttpPost post = new HttpPost(url);
            post.setEntity(new StringEntity(isRestApiNew ? JsonUtils.targetCapUriToJsonNew(targetCapabilityUri) :
                    JsonUtils.targetCapUriToJsonOld(targetCapabilityUri)));
            List<Header> headers = new ArrayList<>();
            headers.add(new BasicHeader("Content-Type", "application/json"));
            headers.add(new BasicHeader("Accept", "application/json"));
            return HttpUtils.execute(post, headers);
        } catch (SpiException se) {
            LOG.error("Error Updating Capability of {} to {}: {}", path, targetCapabilityUri, se.getMessage());
            throw new BackEndException(se.getMessage(), se.getCause());
        } catch (UnsupportedEncodingException | IllegalArgumentException ue) {
            LOG.error("Error creating request to update capability of {} to {}: {}",
                    path, targetCapabilityUri, ue.getMessage());
            throw new BackEndException(ue.getMessage(), ue.getCause());
        }
    }

    @Override
    public CdmiObjectStatus getCurrentStatus(String path) throws BackEndException {
        LOG.debug("Get Current Cdmi Capability of {}", path);

        String cdmiUrl = DCACHE_SERVER + apiPrefix +
                (isRestApiNew ? qosPrefixNew : "qos-management/" + "namespace") + path +
                (isRestApiNew ? "/?qos=true" : "");
        String restUrl = DCACHE_SERVER + apiPrefix + "namespace" + path;
        String childUrl = DCACHE_SERVER + apiPrefix + "namespace" + path + "/?children=true";

        try {
            JSONObject cdmi = HttpUtils.currentStatus(cdmiUrl);
            JSONObject rest = HttpUtils.currentStatus(restUrl);
            JSONObject childrenJson = HttpUtils.currentStatus(childUrl);

            String curQos = cdmi.getString((isRestApiNew) ? "currentQos" : "qos");

            String currentCapUrl = HttpUtils.getCapabilityUri(DCACHE_SERVER + apiPrefix + qosPrefixOld,
                    rest.getString("fileType"),
                    curQos);

            Map<String, Object> monAttributes = HttpUtils.monitoredAttributes(currentCapUrl);
            LOG.debug("Children {}  of Cdmi object {}", childrenJson, path);

            List<String> children = ParseUtils.extractChildren(childrenJson);

            String currentCapUri = "/cdmi_capabilities/" +
                    ParseUtils.fileTypeToCapType(rest.getString("fileType")) +
                    "/" + curQos;

            LOG.debug("Cdmi object {} if of type: {}", path, rest.getString("fileType"));

            String targetCapUri = null;
            if (cdmi.has("targetQoS")) {
                targetCapUri = "/cdmi_capabilities/" + ParseUtils.fileTypeToCapType(rest.getString("fileType")) + "/"
                        + cdmi.getString("targetQoS");
            }
            LOG.info("Cdmi Capability of object {} is {}; in transition to {}", path, currentCapUri, targetCapUri);

            CdmiObjectStatus status = new CdmiObjectStatus(monAttributes, currentCapUri, targetCapUri);
            status.setExportAttributes(exports);
            status.setChildren(children);
            return status;
        } catch (SpiException | JSONException | IllegalArgumentException se) {
            LOG.error(se.getMessage());
            throw new BackEndException(se.getMessage());
        }
    }
}


