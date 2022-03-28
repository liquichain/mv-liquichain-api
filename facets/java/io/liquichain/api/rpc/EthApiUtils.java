package io.liquichain.api.rpc;

import java.math.BigInteger;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.web3j.crypto.*;

public class EthApiUtils {
  private static final Logger LOG = LoggerFactory.getLogger(EthApiUtils.class);

  public static String createResponse(String requestId, String result) {
    String resultFormat = result.startsWith("{") ? "%s" : "\"%s\"";
    String response = new StringBuilder()
        .append("{\n")
        .append("  \"id\": ").append(requestId).append(",\n")
        .append("  \"jsonrpc\": \"2.0\",\n")
        .append("  \"result\": ").append(String.format(resultFormat, result)).append("\n")
        .append("}").toString();
    LOG.debug("response: {}", response);
    return response;
  }

  public static String createErrorResponse(String requestId, String errorCode, String message) {
    String response = new StringBuilder()
        .append("{\n")
        .append("  \"id\": ").append(requestId).append(",\n")
        .append("  \"jsonrpc\": \"2.0\",\n")
        .append("  \"error\": {\n")
        .append("    \"code\": ").append(errorCode).append(",\n")
        .append("    \"message\": \"").append(message).append("\"\n")
        .append("  }\n")
        .append("}").toString();
    LOG.debug("error response: {}", response);
    return response;
  }

  public static String normalizeHash(String hash) {
    if (hash.startsWith("0x")) {
      return hash.substring(2);
    }
    return hash;
  }

  public static String retrieveHash(List<String> parameters, int parameterIndex) {
    return normalizeHash(parameters.get(parameterIndex));
  }

  public static boolean isJSONValid(String jsonInString) {
    try {
      final ObjectMapper mapper = new ObjectMapper();
      mapper.readTree(jsonInString);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static String toHexByteArray(byte[] bytes) {
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
}
