package io.liquichain.api.rpc;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthApiUtils extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(EthApiUtils.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String formatId(Object id) {
        return id == null || NumberUtils.isParsable("" + id) ? "" + id : "\"" + id + "\"";
    }

    public static String formatResult(String result) {
        return result.startsWith("{") || result.startsWith("[") ? result : "\"" + result + "\"";
    }

    public static String createResponse(Object requestId, String result) {
        String response = "{\n" +
            "  \"id\": " + formatId(requestId) + ",\n" +
            "  \"jsonrpc\": \"2.0\",\n" +
            "  \"result\": " + formatResult(result) + "\n" +
            "}";
        LOG.info("response: {}", response);
        return response;
    }

    public static String createErrorResponse(Object requestId, String errorCode, String message) {
        String response = "{\n" +
            "  \"id\": " + formatId(requestId) + ",\n" +
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
        return removeHexPrefix(hash).toLowerCase();
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
            json = mapper.writeValueAsString(data);
        } catch (Exception e) {
            LOG.error("Failed to convert to json: {}", data, e);
        }
        return json;
    }

    public static <T> T convert(String data) {
        T value = null;
        try {
            value = mapper.readValue(data, new com.fasterxml.jackson.core.type.TypeReference<T>() {
            });
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

    public static String removeHexPrefix(String data) {
        if (data == null) {
            return "";
        }
        if (data.startsWith("0x")) {
            return data.substring(2);
        }
        return data;
    }

    public static String lowercaseHex(String data) {
        return addHexPrefix(data).toLowerCase();
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
    }

}
