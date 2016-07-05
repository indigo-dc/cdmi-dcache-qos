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

public class dCacheStorageBackend implements StorageBackend
{
    private static final Logger LOG = LoggerFactory.getLogger(dCacheStorageBackend.class);

    //public static final String dCacheServer = "https://prometheus.desy.de:3880/";
    public static final String dCacheServer = "http://dcache-qos-01.desy.de:3880/";
    public static final String apiPrefix = "api/v1/";
    public static final String qosPrefix = "qos-management/qos/";
    public static HashMap<String, String> capabilities = new HashMap<>();

    static {
        capabilities.put("cdmi_capabilities_templates", "true");
        capabilities.put("cdmi_capabilities_exact_inherit", "true");
        capabilities.put("cdmi_data_redundancy", "true");
        capabilities.put("cdmi_geographic_placement", "true");
        capabilities.put("cdmi_latency", "true");
    }

    @Override
    public List<BackendCapability> getCapabilities() throws BackEndException
    {
        System.out.println("Get Cdmi Capabilities");
        String url = dCacheServer + apiPrefix + qosPrefix;

        try {
            return HttpUtils.getBackendCapabilities(url);
        } catch (SpiException se) {
            LOG.warn(se.getMessage());
            throw new BackEndException(se.getMessage());
        }
    }

    @Override
    public void updateCdmiObject(String path, String targetCapabilityUri)
            throws BackEndException
    {
        System.out.println("Update Cdmi Object");
        String url = dCacheServer + apiPrefix + "qos-management/" + "namespace"  + path;
        try {
            HttpPost post = new HttpPost(url);
            post.setEntity(new StringEntity(JsonUtils.targetCapUriToJson(targetCapabilityUri)));
            LOG.debug("Request body {}, headers {}", JsonUtils.targetCapUriToJson(targetCapabilityUri),
                    post.getAllHeaders());
            List<Header> headers = new ArrayList<>();
            headers.add(new BasicHeader("Content-Type", "application/json"));
            headers.add(new BasicHeader("Accept", "application/json"));
            JSONObject response = HttpUtils.execute(post, headers);
            LOG.debug("QoS Update of {} to {}: {}", path, targetCapabilityUri, response.getString("message"));
        } catch (SpiException se) {
            throw new BackEndException(se.getMessage(), se.getCause());
        } catch (UnsupportedEncodingException ue) {
            throw new BackEndException(ue.getMessage(), ue.getCause());
        }
    }

    @Override
    public CdmiObjectStatus getCurrentStatus(String path) throws BackEndException {
        System.out.println("Get Current Status");
        String cdmiUrl = dCacheServer + apiPrefix + "qos-management/" + "namespace" + path;
        String restUrl = dCacheServer + apiPrefix + "namespace" + path;

        try {
            JSONObject cdmi = HttpUtils.currentStatus(cdmiUrl);
            JSONObject rest = HttpUtils.currentStatus(restUrl);

            String status = cdmi.getString("status"); // Use it to set Status
            String curQos = cdmi.getString("qos");

            String currentCapUrl = HttpUtils.getCapabilityUri(dCacheServer + apiPrefix + qosPrefix,
                                            rest.getString("fileType"),
                                            curQos);
            Map<String, String> monAttributes = HttpUtils.monitoredAttributes(currentCapUrl);

            String currentCapUri = "/cdmi_capabilities/" +
                                    ParseUtils.fileTypeToCapType(rest.getString("fileType")) +
                                    "/" + curQos;

            LOG.debug("fileType: {}, rest {}", rest.getString("fileType"), rest);

            String targetCapUri = null;
            if (cdmi.has("update")) {
                targetCapUri = "/cdmi_capabilities/" + ParseUtils.fileTypeToCapType(rest.getString("fileType")) + "/"
                                + cdmi.getString("update");
            }
            return new CdmiObjectStatus(monAttributes, currentCapUri, targetCapUri);
        } catch(SpiException | JSONException se) {
            LOG.warn(se.getMessage());
            throw new BackEndException(se.getMessage());
        }
    }
}
