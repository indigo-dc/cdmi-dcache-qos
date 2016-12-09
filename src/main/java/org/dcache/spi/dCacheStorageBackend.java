/*
 * Copyright 2016 Deutsches Elektronen-Synchrotron (DESY)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package org.dcache.spi;

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

public class dCacheStorageBackend implements StorageBackend
{
    private static final Logger LOG = LoggerFactory.getLogger(dCacheStorageBackend.class);
    private PluginConfig config;

    public static final String apiPrefix = "api/v1/";
    public static final String qosPrefix = "qos-management/qos/";
    private String DCACHE_SERVER ;
    public static final HashMap<String, Object> capabilities = new HashMap<>();

    static {
        //capabilities.put("cdmi_capabilities_templates", "true");
        //capabilities.put("cdmi_capabilities_exact_inherit", "true");
        capabilities.put("cdmi_data_redundancy", "true");
        capabilities.put("cdmi_geographic_placement", "true");
        capabilities.put("cdmi_capabilities_allowed", "true");
        capabilities.put("cdmi_latency", "true");
    }

    public dCacheStorageBackend() {
        config = new PluginConfig();
        DCACHE_SERVER = "http://" + config.get("dcache.server") + ":" + config.get("dcache.server.rest.endpoint") + "/";
        HttpUtils.setCredentials(config.get("dcache.rest.user"), config.get("dcache.rest.password"));
    }

    @Override
    public List<BackendCapability> getCapabilities() throws BackEndException
    {
        String url = DCACHE_SERVER + apiPrefix + qosPrefix;
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
            throws BackEndException
    {
        String url = DCACHE_SERVER + apiPrefix + "qos-management/" + "namespace"  + path;
        try {
            HttpPost post = new HttpPost(url);
            post.setEntity(new StringEntity(JsonUtils.targetCapUriToJson(targetCapabilityUri)));
            List<Header> headers = new ArrayList<>();
            headers.add(new BasicHeader("Content-Type", "application/json"));
            headers.add(new BasicHeader("Accept", "application/json"));
            JSONObject response = HttpUtils.execute(post, headers);
            LOG.info("QoS Update of {} to {}: {}", path, targetCapabilityUri, response.getString("message"));
        } catch (SpiException se) {
            LOG.error("Error Updating Capability of {} to {}: {}", path, targetCapabilityUri, se.getMessage());
            throw new BackEndException(se.getMessage(), se.getCause());
        } catch (UnsupportedEncodingException ue) {
            LOG.error("Error creating request to update capability of {} to {}: {}",
                    path, targetCapabilityUri, ue.getMessage());
            throw new BackEndException(ue.getMessage(), ue.getCause());
        }
    }

    @Override
    public CdmiObjectStatus getCurrentStatus(String path) throws BackEndException {
        LOG.debug("Get Current Cdmi Capability of {}", path);

        String cdmiUrl = DCACHE_SERVER + apiPrefix + "qos-management/" + "namespace" + path;
        String restUrl = DCACHE_SERVER + apiPrefix + "namespace" + path;
        try {
            JSONObject cdmi = HttpUtils.currentStatus(cdmiUrl);
            JSONObject rest = HttpUtils.currentStatus(restUrl);

            String status = cdmi.getString("status"); // Use it to set Status
            String curQos = cdmi.getString("qos");

            String currentCapUrl = HttpUtils.getCapabilityUri(DCACHE_SERVER + apiPrefix + qosPrefix,
                                            rest.getString("fileType"),
                                            curQos);
            Map<String, Object> monAttributes = HttpUtils.monitoredAttributes(currentCapUrl);

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
            return new CdmiObjectStatus(monAttributes, currentCapUri, targetCapUri);
        } catch(SpiException | JSONException se) {
            LOG.error(se.getMessage());
            throw new BackEndException(se.getMessage());
        }
    }
}
