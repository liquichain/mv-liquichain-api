package io.liquichain.api.scann;


import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ArrayList;
import java.math.BigInteger;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.storage.Repository;
import org.meveo.service.storage.RepositoryService;
import org.meveo.api.persistence.CrossStorageApi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class EthScannScript extends Script {

    private static final Logger LOG = LoggerFactory.getLogger(EthScannScript.class);

    private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private Repository defaultRepo = repositoryService.findDefaultRepository();

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

    private String createResponse(String status, String message, String result) {
        String res = "{\n";
        res += "  \"status\": " + 1 + ",\n";
        res += " \"message\" : \"" + message + "\",\n";
        res += " \"result\" : " + result + "\n";
        res += "}";
        LOG.info("response:{}", res);
        return res;
    }

    private String getBalance(String hash) {
        try {
            Wallet wallet = crossStorageApi.find(defaultRepo,hash.toLowerCase(), Wallet.class);
            return createResponse("1", "OK-Missing/Invalid API Key, rate limit of 1/5sec applied",
                                  "\"0x" + new BigInteger(wallet.getBalance()).toString(16)) + "\"";
        } catch (Exception e) {
            return createResponse("0", "Resource not found", e.getMessage());
        }
    }

    public String getTransactionList(String hash) {
        ObjectMapper mapper = new ObjectMapper();
        List<Transaction> transactions = crossStorageApi.find(defaultRepo, Transaction.class)
                                                        .by("fromHexHash", hash.toLowerCase())
                                                        .limit(offset + limit)
                                                        .getResults();
        List<Transaction> transactionsTo = crossStorageApi.find(defaultRepo, Transaction.class)
                                                          .by("toHexHash", hash.toLowerCase())
                                                          .limit(offset + limit)
                                                          .getResults();
        for (Transaction transac : transactionsTo) {
            //we reverse the amount for transfer received
            BigInteger amount = new BigInteger(transac.getValue()).negate();
            transac.setValue(amount.toString());
        }
        transactions.addAll(transactionsTo);
        //we order by date descending
        transactions = transactions.stream()
                                   .sorted(Comparator.comparing(Transaction::getCreationDate).reversed())
                                   .collect(Collectors.toList());
        //check offset and limit
        if (transactions.size() <= offset) {
            transactions = new ArrayList<>();
        } else {
            transactions = transactions.subList(offset, Math.min(offset + limit, transactions.size()));
        }
        String result = "[";
        String sep = "";
        for (Transaction transac : transactions) {
            Map<String, Object> map = new HashMap<>();
            map.put("blockNumber", transac.getBlockNumber());
            map.put("timeStamp", transac.getCreationDate());
            map.put("hash", transac.getHexHash());
            map.put("nonce", this.toBigHex(transac.getNonce()));
            map.put("blockHash", transac.getBlockHash());
            map.put("transactionIndex", transac.getTransactionIndex());
            map.put("from", "0x" + transac.getFromHexHash());
            map.put("to", "0x" + transac.getToHexHash());
            map.put("value", "0x" + (new BigInteger(transac.getValue())).toString(16));
            map.put("data", transac.getData());
            map.put("gas", "0");
            map.put("gasPrice", "0x" + transac.getGasPrice());
            map.put("isError", "0");
            map.put("txreceipt_status", "1");
            map.put("input", "0x");
            map.put("contractAddress", "");
            map.put("cumulativeGasUsed", "");
            map.put("gasUsed", "");
            map.put("confirmations", "1");
            try {
                result += sep + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            sep = ",";
        }
        result += "]";
        return createResponse("1", "OK", result);
    }
}
