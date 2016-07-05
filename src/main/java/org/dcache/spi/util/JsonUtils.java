package org.dcache.spi.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class JsonUtils
{
    public static List<String> jsonArrayToStringList(JSONArray array)
    {
        int len = array.length();
        List<String> list = new ArrayList<String>(len);
        for (int i = 0; i < len; i++) {
            list.add(array.getString(i));
        }
        return list;
    }

    public static String targetCapUriToJson (String targetCapability) {
        String[] parts = targetCapability.trim().split("/");
        JSONObject json = new JSONObject();
        json.put("update", parts[parts.length - 1]);
        return json.toString();
    }
}
