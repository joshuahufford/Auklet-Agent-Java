package io.auklet.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Messages {

    private Messages(){ }

    protected static Map<String, Object> map = new HashMap<>();

    protected static byte[] createMessagePack(){
        byte[] bytes = new byte[]{};
        try {
            map.put("id", UUID.randomUUID());
            map.put("application", Auklet.AppId);
            map.put("macAddressHash", Util.getMacAddressHash());
            map.put("publicIP", Util.getIpAddress());
            map.put("systemMetrics", Util.getSystemMetrics());
            map.put("agentVersion", "0.0");
            map.put("device", Device.getClient_Username());

            ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());
            bytes = objectMapper.writeValueAsBytes(map);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return bytes;
    }
}
