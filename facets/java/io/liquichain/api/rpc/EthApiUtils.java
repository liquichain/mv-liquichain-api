package io.liquichain.api.rpc;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthApiUtils extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(EthApiUtils.class);
    private static final Gson gson = new Gson();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String createResponse(String requestId, String result) {
        String idFormat = requestId == null || NumberUtils.isParsable(requestId)
            ? "  \"id\": %s,"
            : "  \"id\": \"%s\",";
        String resultFormat = result.startsWith("{") || result.startsWith("[") ? "%s" : "\"%s\"";
        String response = "{\n" +
            String.format(idFormat, requestId) + "\n" +
            "  \"jsonrpc\": \"2.0\",\n" +
            "  \"result\": " + String.format(resultFormat, result) + "\n" +
            "}";
        LOG.info("response: {}", response);
        return response;
    }

    public static String createErrorResponse(String requestId, String errorCode, String message) {
        String idFormat = requestId == null || NumberUtils.isParsable(requestId)
            ? "  \"id\": %s,"
            : "  \"id\": \"%s\",";
        String response = "{\n" +
            String.format(idFormat, requestId) + "\n" +
            "  \"jsonrpc\": \"2.0\",\n" +
            "  \"error\": {\n" +
            "    \"code\": " + errorCode + ",\n" +
            "    \"message\": \"" + message + "\"\n" +
            "  }\n" +
            "}";
        LOG.info("error response: {}", response);
        return response;
    }

    public static String normalizeHash(String hash) {
        if (hash.startsWith("0x")) {
            return hash.substring(2).toLowerCase();
        }
        return hash.toLowerCase();
    }

    public static String retrieveHash(List<String> parameters, int parameterIndex) {
        return normalizeHash(parameters.get(parameterIndex));
    }

    public static boolean isJSONValid(String jsonInString) {
        try {
            mapper.readTree(jsonInString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String toJson(Object data) {
        String json = null;
        try {
            json = gson.toJson(data);
        } catch (Exception e) {
            LOG.error("Failed to convert to json: {}", data, e);
        }
        return json;
    }

    public static <T> T convert(String data) {
        T value = null;
        try {
            value = gson.fromJson(data,  new TypeToken<T>() {}.getType());
        } catch (Exception e) {
            LOG.error("Failed to parse data: {}", data, e);
        }
        return value;
    }

    public static String toHex(byte[] bytes) {
        StringBuilder hexValue = new StringBuilder();
        for (byte aByte : bytes) {
            hexValue.append(String.format("%02x", aByte));
        }
        return hexValue.toString().toLowerCase();
    }

    public static String toBigHex(String value) {
        String hexValue = "";
        if (value != null) {
            try {
                hexValue = "0x" + new BigInteger(value).toString(16);
            } catch (NumberFormatException e) {
                LOG.error("Failed to convert {} to hex", value, e);
            }
        }
        return hexValue;
    }

    public static String addHexPrefix(String data) {
        if (data == null) {
            return "";
        }
        if (data.startsWith("0x")) {
            return data;
        }
        return "0x" + data;
    }

    public static String lowercaseHex(String data) {
        return addHexPrefix(data).toLowerCase();
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
    }

}
