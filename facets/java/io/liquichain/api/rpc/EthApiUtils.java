package io.liquichain.api.rpc;

import java.math.BigInteger;
import java.security.SignatureException;
import java.util.List;
import java.util.Map;

import org.meveo.commons.utils.StringUtils;
import org.meveo.model.customEntities.Transaction;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;
import org.meveo.service.script.ScriptInterface;

import io.liquichain.api.handler.MethodHandlerInput;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.SignedRawTransaction;

public class EthApiUtils extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(EthApiUtils.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ScriptInstanceService scriptInstanceService = getCDIBean(ScriptInstanceService.class);

    public <T> T loadScript(Class<T> scriptClass, Map<String, Object> params) {
        ScriptInterface scriptInterface = scriptInstanceService.getExecutionEngine(scriptClass.getName(), params);
        return (T) scriptInterface;
    }

    public String formatId(Object id) {
        return id == null || NumberUtils.isParsable("" + id) ? "" + id : "\"" + id + "\"";
    }

    public String formatResult(String result) {
        return result.startsWith("{") || result.startsWith("[") ? result : "\"" + result + "\"";
    }

    public String createResponse(Object requestId, String result) {
        String response = "{\n" +
                "  \"id\": " + formatId(requestId) + ",\n" +
                "  \"jsonrpc\": \"2.0\",\n" +
                "  \"result\": " + formatResult(result) + "\n" +
                "}";
        LOG.debug("response: {}", response);
        return response;
    }

    public String createErrorResponse(Object requestId, String errorCode, String message) {
        String response = "{\n" +
                "  \"id\": " + formatId(requestId) + ",\n" +
                "  \"jsonrpc\": \"2.0\",\n" +
                "  \"error\": {\n" +
                "    \"code\": " + errorCode + ",\n" +
                "    \"message\": \"" + message + "\"\n" +
                "  }\n" +
                "}";
        LOG.error("error response: {}", response);
        return response;
    }

    public String normalizeHash(String hash) {
        return removeHexPrefix(hash).toLowerCase();
    }

    public String retrieveHash(List<String> parameters, int parameterIndex) {
        return normalizeHash(parameters.get(parameterIndex));
    }

    public boolean isJSONValid(String jsonInString) {
        try {
            mapper.readTree(jsonInString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String toJson(Object data) {
        String json = null;
        try {
            json = mapper.writeValueAsString(data);
        } catch (Exception e) {
            LOG.error("Failed to convert to json: {}", data, e);
        }
        return json;
    }

    public <T> T convert(String data) {
        T value = null;
        try {
            value = mapper.readValue(data, new com.fasterxml.jackson.core.type.TypeReference<T>() {
            });
        } catch (Exception e) {
            LOG.error("Failed to parse data: {}", data, e);
        }
        return value;
    }

    public String toHex(byte[] bytes) {
        StringBuilder hexValue = new StringBuilder();
        for (byte aByte : bytes) {
            hexValue.append(String.format("%02x", aByte));
        }
        return hexValue.toString().toLowerCase();
    }

    public String toBigHex(String value) {
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

    public String addHexPrefix(String data) {
        if (data == null) {
            return "";
        }
        if (data.startsWith("0x")) {
            return data;
        }
        return "0x" + data;
    }

    public String removeHexPrefix(String data) {
        if (data == null) {
            return "";
        }
        if (data.startsWith("0x")) {
            return data.substring(2);
        }
        return data;
    }

    public String lowercaseHex(String data) {
        return addHexPrefix(data).toLowerCase();
    }

    private Transaction loadCommonData(RawTransaction rawTransaction, String transactionHash, String data) {
        try {
            Transaction transaction = new Transaction();
            if (rawTransaction instanceof SignedRawTransaction) {
                SignedRawTransaction signedTransaction = (SignedRawTransaction) rawTransaction;
                Sign.SignatureData signatureData = signedTransaction.getSignatureData();
                transaction.setFromHexHash(normalizeHash(signedTransaction.getFrom()));
                transaction.setV(toHex(signatureData.getV()));
                transaction.setS(toHex(signatureData.getS()));
                transaction.setR(toHex(signatureData.getR()));
            }
            transaction.setHexHash(transactionHash);
            transaction.setNonce("" + rawTransaction.getNonce());
            transaction.setGasPrice("" + rawTransaction.getGasPrice());
            transaction.setGasLimit("" + rawTransaction.getGasLimit());
            transaction.setSignedHash(data);
            transaction.setRawData(rawTransaction.getData());
            transaction.setBlockNumber("1");
            transaction.setBlockHash("e8594f30d08b412027f4546506249d09134b9283530243e01e4cdbc34945bcf0");
            transaction.setCreationDate(java.time.Instant.now());
            return transaction;
        } catch (SignatureException e) {
            throw new RuntimeException("Failed to load signed transaction.", e);
        }
    }

    public Transaction buildTransactionDetails(MethodHandlerInput methodHandlerInput) {
        return buildTransactionDetails(methodHandlerInput, null);
    }

    public Transaction buildTransactionDetails(MethodHandlerInput methodHandlerInput, String recipient) {
        String transactionHash = methodHandlerInput.getTransactionHash();
        String data = methodHandlerInput.getData();
        RawTransaction rawTransaction = methodHandlerInput.getRawTransaction();
        return buildTransactionDetails(rawTransaction, transactionHash, recipient, data);
    }

    public Transaction buildTransactionDetails(RawTransaction rawTransaction, String transactionHash,
            String recipient, String data) {
        Transaction transaction = loadCommonData(rawTransaction, transactionHash, data);
        if (!StringUtils.isBlank(recipient)) {
            transaction.setToHexHash(normalizeHash(recipient));
        }
        return transaction;
    }
}
