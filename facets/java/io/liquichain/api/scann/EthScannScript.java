package io.liquichain.api.scann;


import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.math.BigInteger;

import com.fasterxml.jackson.core.type.TypeReference;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EthScannScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(EthScannScript.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final ObjectMapper mapper = new ObjectMapper();

    private String result;

    private String action;
    private String address;
    private int offset;
    private int limit = 100;

    public String getResult() {
        return result;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void execute(Map<String, Object> parameters) throws BusinessException {
        if (address.startsWith("0x")) {
            address = address.substring(2);
        }
        switch (action) {
            case "balance":
                result = getBalance(address);
                break;
            case "balancehistory":
                result = getTransactionList(address);
                break;
        }
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

    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(String.format("%02x", aByte));
        }
        return result.toString();
    }

    private String toBigHex(String i) {
        return "0x" + new BigInteger(i).toString(16).toLowerCase();
    }

    private static String normalizeHash(String hash) {
        if (hash == null) {
            return null;
        }
        if (hash.startsWith("0x")) {
            return hash.substring(2).toLowerCase();
        }
        return hash.toLowerCase();
    }

    private String createResponse(String status, String message, String result) {
        String res = "{\n";
        res += "  \"status\": \"" + status + "\",\n";
        res += " \"message\" : \"" + message + "\",\n";
        res += " \"result\" : " + result + "\n";
        res += "}";
        return res;
    }

    private String getBalance(String hash) {
        try {
            Wallet wallet = crossStorageApi.find(defaultRepo, hash.toLowerCase(), Wallet.class);
            return createResponse("success", "OK-Missing/Invalid API Key, rate limit of 1/5sec applied",
                "\"0x" + new BigInteger(wallet.getBalance()).toString(16)) + "\"";
        } catch (Exception e) {
            return createResponse("fail", "Resource not found", e.getMessage());
        }
    }

    private <T> T convert(String data) {
        T value = null;
        try {
            value = mapper.readValue(data, new TypeReference<T>() {
            });
        } catch (Exception e) {
            LOG.error("Failed to parse data: {}", data, e);
        }
        return value;
    }

    private String toJson(Object data) {
        String json = null;
        try {
            json = mapper.writeValueAsString(data);
        } catch (Exception e) {
            LOG.error("Failed to convert to json: {}", data, e);
        }
        return json;
    }

    public String getTransactionList(String hash) {

        String walletId = normalizeHash(hash);
        List<Transaction> transactions = crossStorageApi.find(defaultRepo, Transaction.class)
                                                        .by("likeCriterias fromHexHash toHexHash initiator", walletId)
                                                        .orderBy("creationDate", false)
                                                        .limit(limit)
                                                        .offset(offset)
                                                        .getResults();

        List<Map<String, Object>> results = new ArrayList<>();
        for (Transaction transaction : transactions) {
            Map<String, Object> map = new HashMap<>();
            boolean hasCreationDate = transaction.getCreationDate() != null;
            Long creationDate = hasCreationDate ? transaction.getCreationDate().toEpochMilli() : null;
            Map<String, Object> data = convert(transaction.getData());
            String value = transaction.getValue() != null ? (new BigInteger(transaction.getValue())).toString(16) : "0";
            map.put("blockNumber", transaction.getBlockNumber());
            map.put("timeStamp", creationDate);
            map.put("hash", transaction.getHexHash());
            map.put("nonce", this.toBigHex(transaction.getNonce()));
            map.put("blockHash", transaction.getBlockHash());
            map.put("transactionIndex", transaction.getTransactionIndex());
            map.put("from", "0x" + transaction.getFromHexHash());
            map.put("to", "0x" + transaction.getToHexHash());
            map.put("initiatedBy", "0x" + transaction.getInitiator());
            map.put("value", "0x" + value);
            map.put("data", data);
            map.put("gas", "0");
            map.put("gasPrice", "0x" + transaction.getGasPrice());
            map.put("isError", "0");
            map.put("txreceipt_status", "1");
            map.put("input", "0x");
            map.put("contractAddress", "");
            map.put("cumulativeGasUsed", "");
            map.put("gasUsed", "");
            map.put("confirmations", "1");
            map.put("assetId", transaction.getAssetId());
            results.add(map);
        }
        return createResponse("success", "Balance history retrieved.", toJson(results));
    }
}
