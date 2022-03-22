package io.liquichain.api.rpc;

import java.math.BigInteger;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import javax.enterprise.context.ApplicationScoped;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.customEntities.LiquichainApp;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;
import org.meveo.model.storage.Repository;
import org.meveo.api.persistence.CrossStorageApi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang3.StringEscapeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.web3j.crypto.*;

@ApplicationScoped
public class EthApiProxyScript extends Script {

    private static final Logger LOG = LoggerFactory.getLogger(EthApiProxyScript.class);

    private static final String NOT_IMPLEMENTED_ERROR = "Feature not yet implemented";
    private static final String METHOD_NOT_FOUND_ERROR = "Method not found";
    private static final String CREATE_WALLET_ERROR = "Failed to create wallet";
    private static final String UPDATE_WALLET_ERROR = "Failed to update wallet";
    private static final String WALLET_INFO_ERROR = "Failed to get wallet info";
    private static final String UNKNOWN_WALLET_ERROR = "Unknown wallet";
    private static final String WALLET_EXISTS_ERROR = "Wallet already exists";
    private static final String GET_CODE_ERROR = "Failed to get code";
    private static final String GET_BALANCE_ERROR = "Failed to get balance";
    private static final String TRANSACTION_EXISTS_ERROR = "Transaction already exists: {}";
    private static final String INVALID_REQUEST = "-32600";
    private static final String INTERNAL_ERROR = "-32603";
    private static final String RESOURCE_NOT_FOUND = "-32001";
    private static final String TRANSACTION_REJECTED = "-32003";
    private static final String METHOD_NOT_FOUND = "-32601";
    private static final String PROXY_REQUEST_ERROR =
            "Proxy request to remote json-rpc endpoint failed";

    private String result;

    private ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private ParamBean config = paramBeanFactory.getInstance();
    private String APP_NAME = config.getProperty("eth.api.appname", "licoin");
    private String fabricUrl = config.getProperty("fabric.sdk.url", "http://163.172.190.14:3011");
    private String explorerApiUrl =
            config.getProperty("explorer.api.url", "https://test-fabric.liquichain.io");
    private String fabricUsername = config.getProperty("fabric.sdk.username", "lchainadmin");
    private String fabricPassword = config.getProperty("fabric.sdk.password", "L194a1N_!_ .");
    private String fabricNetworkName = config.getProperty("fabric.network.name", "test-network");
    private String besuApiUrl =
            config.getProperty("besu.api.url", "https://testnet.liquichain.io/rpc");

    private RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private Repository defaultRepo = repositoryService.findDefaultRepository();

    private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);

    public String getResult() {
        return result;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        String method = "" + parameters.get("method");
        LOG.info("json rpc: {}, parameters:{}", method, parameters);
        String requestId = "" + parameters.get("id");
        switch (method) {
            case "get_chainId":
                result = createResponse(requestId, "0x4c");
                break;
            case "eth_sendRawTransaction":
                result = sendRawTransaction(requestId, parameters);
                break;
            case "wallet_creation":
                result = createWallet(requestId, parameters);
                break;
            case "wallet_update":
                result = updateWallet(requestId, parameters);
                break;
            case "wallet_info":
                result = getWalletInfo(requestId, parameters);
                break;
            case "wallet_report":
                result = createErrorResponse(requestId, METHOD_NOT_FOUND, NOT_IMPLEMENTED_ERROR);
                break;
            default:
                result = callEthJsonRpc(requestId, parameters);
                break;
        }
    }

    public static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte aByte : bytes) {
            result.append(String.format("%02x", aByte));
        }
        return result.toString().toLowerCase();
    }

    private String normalizeHash(String hash) {
        if (hash.startsWith("0x")) {
            return hash.substring(2);
        }
        return hash;
    }

    private String createResponse(String requestId, String result) {
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

    private String createErrorResponse(String requestId, String errorCode, String message) {
        String response = new StringBuilder()
                .append("{\n")
                .append("  \"id\": ").append(requestId).append(",\n")
                .append("  \"jsonrpc\": \"2.0\",\n")
                .append("  \"error\": {\n")
                .append("    \"code\": ").append(errorCode).append(",\n")
                .append("    \"message\": \"").append(message).append("\"\n")
                .append("  }\n")
                .append("}").toString();
        return response;
    }

    private String retrieveHash(Map<String, Object> parameters, int parameterIndex) {
        List<String> params = (List<String>) parameters.get("params");
        LOG.debug("params={}", params);
        String hash = normalizeHash(params.get(parameterIndex));
        LOG.debug("hash={}", hash);
        return hash;
    }

    private String callProxy(String body) throws IOException, InterruptedException {
        LOG.debug("callProxy body={}", body);
        LOG.debug("besuApiUrl={}", besuApiUrl);
        Client client = ClientBuilder.newClient();
        String response = client.target(besuApiUrl)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(body), String.class);
        LOG.debug("callProxy response={}", response);
        return response;
    }

    private String callEthJsonRpc(String requestId, Map<String, Object> parameters) {
        String requestBody = new StringBuilder()
                .append("{")
                .append(String.format("\"id\":%s,", parameters.get("id") + ""))
                .append(String.format("\"jsonrpc\":\"%s\",", (String) parameters.get("jsonrpc")))
                .append(String.format("\"method\":\"%s\",", (String) parameters.get("method")))
                .append(String.format("\"params\":%s", new Gson().toJson(parameters.get("params"))))
                .append("}")
                .toString();
        try {
            return callProxy(requestBody);
        } catch (Exception e) {
            LOG.error(PROXY_REQUEST_ERROR, e);
            return createErrorResponse(requestId, INTERNAL_ERROR, PROXY_REQUEST_ERROR);
        }
    }

    private String createWallet(String requestId, Map<String, Object> parameters) {
        List<String> params = (ArrayList<String>) parameters.get("params");
        String name = params.get(0);
        String walletHash = this.retrieveHash(parameters, 1);
        String accountHash = this.retrieveHash(parameters, 2);
        String publicInfo = params.get(3);
        Wallet wallet = null;

        try {
            wallet = crossStorageApi.find(defaultRepo, walletHash, Wallet.class);
            if (wallet != null) {
                return createErrorResponse(requestId, INVALID_REQUEST, WALLET_EXISTS_ERROR);
            }
        } catch (EntityDoesNotExistsException e) {
            // do nothing, we expect wallet to not exist
        }
        wallet = new Wallet();

        try {
            LiquichainApp app = crossStorageApi
                    .find(defaultRepo, LiquichainApp.class)
                    .by("name", APP_NAME)
                    .getResult();
            wallet.setName(name);
            wallet.setUuid(walletHash);
            wallet.setAccountHash(accountHash);
            wallet.setPublicInfo(publicInfo);
            wallet.setBalance("0");
            wallet.setApplication(app);
            crossStorageApi.createOrUpdate(defaultRepo, wallet);
            return createResponse(requestId, walletHash);
        } catch (Exception e) {
            LOG.error(CREATE_WALLET_ERROR, e);
            return createErrorResponse(requestId, TRANSACTION_REJECTED, CREATE_WALLET_ERROR);
        }
    }

    private String updateWallet(String requestId, Map<String, Object> parameters) {
        List<String> params = (ArrayList<String>) parameters.get("params");
        String name = params.get(0);
        String walletHash = this.retrieveHash(parameters, 1);
        String publicInfo = params.get(2);
        Wallet wallet = null;

        try {
            wallet = crossStorageApi.find(defaultRepo, walletHash, Wallet.class);
            if (wallet == null) {
                return createErrorResponse(requestId, INVALID_REQUEST, UNKNOWN_WALLET_ERROR);
            }
        } catch (EntityDoesNotExistsException e) {
            LOG.error(UNKNOWN_WALLET_ERROR, e);
            return createErrorResponse(requestId, INVALID_REQUEST, UNKNOWN_WALLET_ERROR);
        }

        try {
            wallet.setName(name);
            wallet.setPublicInfo(publicInfo);
            crossStorageApi.createOrUpdate(defaultRepo, wallet);
            return createResponse(requestId, name);
        } catch (Exception e) {
            LOG.error(UPDATE_WALLET_ERROR, e);
            return createErrorResponse(requestId, TRANSACTION_REJECTED, UPDATE_WALLET_ERROR);
        }
    }

    private String getWalletInfo(String requestId, Map<String, Object> parameters) {
        String walletHash = this.retrieveHash(parameters, 0);
        Wallet wallet = null;

        try {
            wallet = crossStorageApi.find(defaultRepo, walletHash, Wallet.class);
            if (wallet == null) {
                return createErrorResponse(requestId, INVALID_REQUEST, UNKNOWN_WALLET_ERROR);
            }
        } catch (EntityDoesNotExistsException e) {
            LOG.error(UNKNOWN_WALLET_ERROR, e);
            return createErrorResponse(requestId, INVALID_REQUEST, UNKNOWN_WALLET_ERROR);
        }

        String response = new StringBuilder()
                .append("{")
                .append(String.format("\"name\":\"%s\",", wallet.getName()))
                .append(String.format("\"publicInfo\":%s",
                        new Gson().toJson(wallet.getPublicInfo())))
                .append("}")
                .toString();

        return createResponse(requestId, response);
    }

    private String sendRawTransaction(String requestId, Map<String, Object> parameters) {
        result = callEthJsonRpc(requestId, parameters);
        boolean hasError = result.contains("\"error\"");
        if (hasError) {
            return result;
        }
        List<String> params = (List<String>) parameters.get("params");
        String data = (String) params.get(0);
        String transactionHash = normalizeHash(Hash.sha3(data));
        try {
            Transaction existingTransaction = crossStorageApi.find(defaultRepo, Transaction.class)
                    .by("hexHash", transactionHash).getResult();
            if (existingTransaction != null) {
                return createErrorResponse(
                        requestId,
                        TRANSACTION_REJECTED,
                        String.format(TRANSACTION_EXISTS_ERROR, transactionHash));
            }
        } catch (Exception e) {
            return createErrorResponse(requestId, RESOURCE_NOT_FOUND, e.getMessage());
        }

        RawTransaction rawTransaction = TransactionDecoder.decode(data);
        LOG.debug("to:{} , value:{}", rawTransaction.getTo(), rawTransaction.getValue());

        if (rawTransaction instanceof SignedRawTransaction) {
            SignedRawTransaction signedTransaction = (SignedRawTransaction) rawTransaction;
            Sign.SignatureData signatureData = signedTransaction.getSignatureData();
            try {
                String v = hex(signatureData.getV());
                String s = hex(signatureData.getS());
                String r = hex(signatureData.getR());
                LOG.debug("from:{} chainId:{} , v:{} , r:{} , s:{}",
                        signedTransaction.getFrom(), signedTransaction.getChainId(), v, r, s);
                String extraData = rawTransaction.getData();
                if (extraData == null || extraData.isEmpty()) {
                    extraData = "{\"type\":\"transfer\",\"description\":\"Transfer coins\"}";
                }
                Transaction transaction = new Transaction();
                transaction.setHexHash(transactionHash);
                transaction.setFromHexHash(normalizeHash(signedTransaction.getFrom()));
                transaction.setToHexHash(normalizeHash(rawTransaction.getTo()));
                transaction.setNonce("" + rawTransaction.getNonce());
                transaction.setGasPrice("" + rawTransaction.getGasPrice());
                transaction.setGasLimit("" + rawTransaction.getGasLimit());
                transaction.setValue("" + rawTransaction.getValue());
                transaction.setSignedHash(data);
                transaction.setData(extraData);
                transaction.setCreationDate(java.time.Instant.now());
                transaction.setV(v);
                transaction.setS(s);
                transaction.setR(r);
                String uuid = crossStorageApi.createOrUpdate(defaultRepo, transaction);
                LOG.debug("Created transaction on DB with uuid: {}", uuid);
            } catch (Exception e) {
                return createErrorResponse(requestId, TRANSACTION_REJECTED, e.getMessage());
            }
        }
        return result;
    }
}