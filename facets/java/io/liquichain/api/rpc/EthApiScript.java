package io.liquichain.api.rpc;

import static io.liquichain.api.rpc.EthApiConstants.*;
import static io.liquichain.api.rpc.EthApiUtils.*;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.ws.rs.client.*;
import javax.ws.rs.core.*;

import io.liquichain.core.BlockForgerScript;

import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.customEntities.VerifiedEmail;
import org.meveo.model.customEntities.VerifiedPhoneNumber;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.service.storage.RepositoryService;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.math.NumberUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.*;
import org.web3j.utils.*;

public class EthApiScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(EthApiScript.class);

    @Inject
    private RepositoryService repositoryService;
    @Inject
    private ParamBeanFactory paramBeanFactory;
    @Inject
    protected CrossStorageApi crossStorageApi;

    protected Repository defaultRepo;
    protected ParamBean config;
    private BLOCKCHAIN_TYPE BLOCKCHAIN_BACKEND;

    protected String result;

    public String getResult() {
        return this.result;
    }

    private void init() {
        defaultRepo = repositoryService.findDefaultRepository();
        config = paramBeanFactory.getInstance();
        String blockchainType = config.getProperty("txn.blockchain.type", "DATABASE");
        BLOCKCHAIN_BACKEND = BLOCKCHAIN_TYPE.valueOf(blockchainType);
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        this.init();
        String method = "" + parameters.get("method");
        BlockchainProcessor processor = null;
        switch (BLOCKCHAIN_BACKEND) {
            case BESU:
                processor = new BesuProcessor(crossStorageApi, defaultRepo, config);
                break;
            case FABRIC:
                break;
            case BESU_ONLY:
                break;
            case DATABASE:
            default:
                processor = new DatabaseProcessor(crossStorageApi, defaultRepo, config);
        }
        if (processor != null) {
            processor.execute(parameters);
            result = processor.getResult();
        } else {
            LOG.info("json rpc: {}, parameters:{}", method, parameters);
            String requestId = "" + parameters.get("id");
            result = createErrorResponse(requestId, INVALID_REQUEST, NOT_IMPLEMENTED_ERROR);
        }
    }
}


class EthApiConstants {
    public static final String NOT_IMPLEMENTED_ERROR = "Feature not yet implemented";
    public static final String CONTRACT_NOT_ALLOWED_ERROR = "Contract deployment not allowed";
    public static final String NAME_REQUIRED_ERROR = "Wallet name is required";
    public static final String NAME_EXISTS_ERROR = "Wallet with name: %s, already exists";
    public static final String EMAIL_REQUIRED_ERROR = "Email address is required";
    public static final String PHONE_NUMBER_REQUIRED_ERROR = "Phone number is required";
    public static final String EMAIL_EXISTS_ERROR = "Email address: %s, already exists";
    public static final String PHONE_NUMBER_EXISTS_ERROR = "Phone number: %s, already exists";
    public static final String TRANSACTION_EXISTS_ERROR = "Transaction already exists: {}";
    public static final String INVALID_REQUEST = "-32600";
    public static final String INTERNAL_ERROR = "-32603";
    public static final String RESOURCE_NOT_FOUND = "-32001";
    public static final String TRANSACTION_REJECTED = "-32003";
    public static final String METHOD_NOT_FOUND = "-32601";
    public static final String PROXY_REQUEST_ERROR = "Proxy request to remote json-rpc endpoint failed";


    public static enum BLOCKCHAIN_TYPE {DATABASE, BESU, FABRIC, BESU_ONLY}
}


class EthApiUtils {
    private static final Logger LOG = LoggerFactory.getLogger(EthApiUtils.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static String createResponse(String requestId, String result) {
        String idFormat = requestId == null || NumberUtils.isParsable(requestId)
            ? "  \"id\": %s,"
            : "  \"id\": \"%s\",";
        String resultFormat = result.startsWith("{") ? "%s" : "\"%s\"";
        String response = "{\n" +
            String.format(idFormat, requestId) + "\n" +
            "  \"jsonrpc\": \"2.0\",\n" +
            "  \"result\": " + String.format(resultFormat, result) + "\n" +
            "}";
        LOG.debug("response: {}", response);
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
        LOG.debug("error response: {}", response);
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
            json = mapper.writeValueAsString(data);
        } catch (Exception e) {
            LOG.error("Failed to convert to json: {}", data, e);
        }
        return json;
    }

    public static <T> T convert(String data) {
        T value = null;
        try {
            value = mapper.readValue(data, new TypeReference<T>() {
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
}


abstract class BlockchainProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(BlockchainProcessor.class);
    private static final Map<String, Object[]> TRANSACTION_HOOKS = new HashMap<>();
    protected CrossStorageApi crossStorageApi;
    protected Repository defaultRepo;
    protected ParamBean config;

    protected String result;

    public BlockchainProcessor(CrossStorageApi crossStorageApi, Repository defaultRepo, ParamBean config) {
        this.crossStorageApi = crossStorageApi;
        this.defaultRepo = defaultRepo;
        this.config = config;
    }

    public String getResult() {
        return this.result;
    }

    public abstract void execute(Map<String, Object> parameters) throws BusinessException;

    public static boolean addTransactionHook(String regex, Script script) {
        boolean isHookAdded = true;
        String key = regex + ":" + script.getClass().getName();
        LOG.info("addTransactionHook key: {}", key);
        isHookAdded = !TRANSACTION_HOOKS.containsKey(key);
        if (isHookAdded) {
            Pattern pattern = Pattern.compile(regex);
            TRANSACTION_HOOKS.put(key, new Object[] {pattern, script});
        }
        return isHookAdded;
    }

    protected void processTransactionHooks(String transactionHash, SignedRawTransaction transaction) {
        try {
            String data = new String(new BigInteger(transaction.getData(), 16).toByteArray());
            LOG.info("try matching {} hooks", TRANSACTION_HOOKS.size());
            TRANSACTION_HOOKS.forEach((String key, Object[] hook) -> {
                LOG.info("try hook {} on {}", key, data);
                Pattern pattern = (Pattern) hook[0];
                Script script = (Script) hook[1];
                Matcher matcher = pattern.matcher(data);
                if (matcher.find()) {
                    LOG.info(" hook {} matched", key);
                    Map<String, Object> context = new HashMap<>();
                    context.put("transaction", transaction);
                    context.put("transactionHash", transactionHash);
                    context.put("matcher", matcher);
                    try {
                        script.execute(context);
                        if (context.containsKey("result")) {
                            LOG.info(" hook result:{} ", context.get("result"));
                        }
                    } catch (Exception e) {
                        LOG.error("error while invoking transaction hook {}", script, e);
                    }
                } else {
                    LOG.info(" hook {} matched", key);
                }
            });
            if (data.contains("orderId")) {
                LOG.info("detected orderId:{}", data);
            }
        } catch (Exception ex) {
            LOG.info("error while detecting order:{}", ex);
        }
    }

    protected String validateName(String name) throws BusinessException {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(NAME_REQUIRED_ERROR);
        }
        Wallet walletWithSameName = null;
        try {
            walletWithSameName = crossStorageApi
                .find(defaultRepo, Wallet.class)
                .by("name", name)
                .getResult();
        } catch (Exception e) {
            // do nothing, we want wallet name to be unique
        }
        if (walletWithSameName != null) {
            String error = String.format(NAME_EXISTS_ERROR, name);
            LOG.error(error);
            throw new BusinessException(error);
        }
        return name;
    }

    protected String validateEmail(String email, String walletId) throws BusinessException {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(EMAIL_REQUIRED_ERROR);
        }
        VerifiedEmail existingEmail = null;
        try {
            existingEmail = crossStorageApi
                .find(defaultRepo, VerifiedEmail.class)
                .by("email", email)
                .by("not-inList walletId", Arrays.asList(walletId))
                .getResult();
        } catch (Exception e) {
            // do nothing, we want email address to be unique
        }
        if (existingEmail != null) {
            String error = String.format(EMAIL_EXISTS_ERROR, email);
            LOG.error(error);
            throw new BusinessException(error);
        }
        return email;
    }

    protected String validatePhoneNumber(String phoneNumber, String walletId)
        throws BusinessException {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new BusinessException(PHONE_NUMBER_REQUIRED_ERROR);
        }
        VerifiedPhoneNumber existingPhoneNumber = null;
        try {
            existingPhoneNumber = crossStorageApi
                .find(defaultRepo, VerifiedPhoneNumber.class)
                .by("phoneNumber", phoneNumber)
                .by("not-inList walletId", Arrays.asList(walletId))
                .getResult();
        } catch (Exception e) {
            // do nothing, we want wallet phoneNumber to be unique
        }
        if (existingPhoneNumber != null) {
            String error = String.format(PHONE_NUMBER_EXISTS_ERROR, phoneNumber);
            LOG.error(error);
            throw new BusinessException(error);
        }
        return phoneNumber;
    }

    protected String parseAddress(String signature, String message) throws Exception {
        byte[] messageHash = Hash.sha3(message.getBytes(StandardCharsets.UTF_8));
        LOG.info("messageHash={}", Numeric.toHexString(messageHash));
        String r = signature.substring(0, 66);
        String s = "0x" + signature.substring(66, 130);
        String v = "0x" + signature.substring(130, 132);
        String publicKey = Sign
            .signedMessageHashToKey(
                messageHash,
                new Sign.SignatureData(
                    Numeric.hexStringToByteArray(v)[0],
                    Numeric.hexStringToByteArray(r),
                    Numeric.hexStringToByteArray(s)
                )
            )
            .toString(16);
        String address = Keys.getAddress(publicKey);
        LOG.info("address: " + address);
        return address;
    }

    protected <T> T findEntity(String uuid, Class<T> clazz) {
        T entity = null;
        try {
            entity = crossStorageApi.find(defaultRepo, uuid, clazz);
        } catch (EntityDoesNotExistsException e) {
            LOG.warn("No {} with uuid: {}", clazz.getSimpleName(), uuid);
        }
        return entity;
    }
}


class BesuProcessor extends BlockchainProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(BesuProcessor.class);

    private String BESU_API_URL = null;
    public final String ORIGIN_WALLET = "b4bF880BAfaF68eC8B5ea83FaA394f5133BB9623".toLowerCase();
    private static final int CONNECTION_POOL_SIZE = 50;
    private static final int MAX_POOLED_PER_ROUTE = 5;
    private static final long CONNECTION_TTL = 5;
    private static Client client = new ResteasyClientBuilder()
        .connectionPoolSize(CONNECTION_POOL_SIZE)
        .maxPooledPerRoute(MAX_POOLED_PER_ROUTE)
        .connectionTTL(CONNECTION_TTL, TimeUnit.SECONDS)
        .build();

    public BesuProcessor(CrossStorageApi crossStorageApi, Repository defaultRepo, ParamBean config) {
        super(crossStorageApi, defaultRepo, config);
        BESU_API_URL = config.getProperty("besu.api.url", "https://testnet.liquichain.io/rpc");
        // ORIGIN_WALLET = config.getProperty("wallet.origin.account", "deE0d5bE78E1Db0B36d3C1F908f4165537217333");
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
            case "eth_sendSignedTransaction":
            case "eth_sendRawTransaction":
                result = sendRawTransaction(requestId, parameters);
                break;
            case "eth_getProof":
            case "eth_getWork":
            case "eth_submitWork":
            case "eea_sendRawTransaction":
                result = createErrorResponse(requestId, INVALID_REQUEST, NOT_IMPLEMENTED_ERROR);
                break;
            default:
                result = callEthJsonRpc(requestId, parameters);
                break;
        }
    }

    private String callProxy(String body) throws IOException, InterruptedException {
        LOG.info("callProxy body={}", body);
        String result = null;
        Response response = null;
        try {

            response = client.target(BESU_API_URL)
                             .request(MediaType.APPLICATION_JSON)
                             .post(Entity.json(body));
            result = response.readEntity(String.class);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        LOG.info("callProxy result={}", result);
        return result;
    }

    private String callEthJsonRpc(String requestId, Map<String, Object> parameters) {
        Object id = parameters.get("id");
        String idFormat =
            id == null || NumberUtils.isParsable("" + id) ? "\"id\": %s," : "\"id\": \"%s\",";
        String requestBody = new StringBuilder()
            .append("{")
            .append(String.format(idFormat, id))
            .append(String.format("\"jsonrpc\":\"%s\",", parameters.get("jsonrpc")))
            .append(String.format("\"method\":\"%s\",", parameters.get("method")))
            .append(String.format("\"params\":%s", toJson(parameters.get("params"))))
            .append("}")
            .toString();
        try {
            return callProxy(requestBody);
        } catch (Exception e) {
            LOG.error(PROXY_REQUEST_ERROR, e);
            return createErrorResponse(requestId, INTERNAL_ERROR, PROXY_REQUEST_ERROR);
        }
    }

    private String sendRawTransaction(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        String data = (String) params.get(0);
        String transactionHash = normalizeHash(Hash.sha3(data));
        try {
            Transaction existingTransaction = crossStorageApi
                .find(defaultRepo, Transaction.class)
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
        LOG.info("to:{} , value:{}", rawTransaction.getTo(), rawTransaction.getValue());

        // as per besu documentation
        // (https://besu.hyperledger.org/en/stable/Tutorials/Contracts/Deploying-Contracts/):
        // to - address of the receiver. To deploy a contract, set to null.
        // or it can also be set to 0x0 or 0x80 as per:
        // (https://stackoverflow.com/questions/48219716/what-is-address0-in-solidity)
        String recipient = rawTransaction.getTo();
        if (recipient == null || "0x0".equals(recipient) || "0x80".equals(recipient)) {
            return createErrorResponse(requestId, INVALID_REQUEST, CONTRACT_NOT_ALLOWED_ERROR);
        }

        result = callEthJsonRpc(requestId, parameters);
        boolean hasError = result.contains("\"error\"");
        if (hasError) {
            return result;
        }

        if (rawTransaction instanceof SignedRawTransaction) {
            SignedRawTransaction signedTransaction = (SignedRawTransaction) rawTransaction;
            Sign.SignatureData signatureData = signedTransaction.getSignatureData();
            try {
                String v = toHex(signatureData.getV());
                String s = toHex(signatureData.getS());
                String r = toHex(signatureData.getR());
                // LOG.info("from:{} chainId:{} , v:{} , r:{} , s:{}",
                // signedTransaction.getFrom(), signedTransaction.getChainId(), v, r, s);
                String extraData = rawTransaction.getData();
                String to = normalizeHash(rawTransaction.getTo());
                BigInteger value = rawTransaction.getValue();
                LOG.info("extraData:{} to:{}", extraData, to);
                String type = "transfer";
                if (extraData == null || extraData.isEmpty()) {
                    extraData = "{\"type\":\"transfer\",\"description\":\"Transfer coins\"}";
                } else if (extraData.startsWith("0xa9059cbb") || extraData.startsWith("a9059cbb")) {
                    if (extraData.startsWith("a9059cbb")) {
                        extraData = "0x" + extraData;
                    }
                    to = extraData.substring(34, 74);
                    value = new BigInteger(extraData.substring(74), 16);
                    extraData = "{\"type\":\"transfer\",\"description\":\"Transfer coins\"}";
                    if (ORIGIN_WALLET.equals(to)) {
                        type = "purchase";
                        extraData = "{\"type\":\"purchase\",\"description\":\"Shop purchase\"}";
                    }
                } else {
                    Map extraDataMap = convert(extraData);
                    if (extraDataMap != null) {
                        type = extraDataMap.get("type").toString();
                    }
                    extraData = "transfer".equals(type)
                        ? "{\"type\":\"transfer\",\"description\":\"Transfer coins\"}"
                        : extraData;
                }
                Transaction transaction = new Transaction();
                transaction.setHexHash(transactionHash);
                transaction.setFromHexHash(normalizeHash(signedTransaction.getFrom()));
                transaction.setToHexHash(to);
                transaction.setNonce("" + rawTransaction.getNonce());
                transaction.setGasPrice("" + rawTransaction.getGasPrice());
                transaction.setGasLimit("" + rawTransaction.getGasLimit());
                transaction.setValue("" + value);
                transaction.setType("" + type);
                transaction.setSignedHash(data);
                transaction.setData(extraData);
                transaction.setBlockNumber("1");
                transaction.setBlockHash(
                    "e8594f30d08b412027f4546506249d09134b9283530243e01e4cdbc34945bcf0");
                transaction.setCreationDate(java.time.Instant.now());
                transaction.setV(v);
                transaction.setS(s);
                transaction.setR(r);
                String uuid = crossStorageApi.createOrUpdate(defaultRepo, transaction);
                LOG.info("Created transaction on DB with uuid: {}", uuid);
            } catch (Exception e) {
                return createErrorResponse(requestId, TRANSACTION_REJECTED, e.getMessage());
            }
        }
        return result;
    }
}


class DatabaseProcessor extends BlockchainProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseProcessor.class);

    private static final String SAMPLE_BLOCK = "{" + "\"difficulty\":\"0x5\","
        + "\"extraData" +
        "\":\"0xd58301090083626f7286676f312e3133856c696e75780000000000000000000021c9effaf6549e725463c7877ddebe9a2916e03228624e4bfd1e3f811da792772b54d9e4eb793c54afb4a29f014846736755043e4778999046d0577c6e57e72100\","
        + "\"gasLimit\":\"0xe984c2\"," + "\"gasUsed\":\"0x0\","
        + "\"hash\":\"0xaa14340feb15e26bc354bb839b2aa41cc7984676249c155ac5e4d281a8d08809\","
        + "\"logsBloom" +
        "\":\"0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000\","
        + "\"miner\":\"0x0000000000000000000000000000000000000000\","
        + "\"mixHash\":\"0x0000000000000000000000000000000000000000000000000000000000000000\","
        + "\"nonce\":\"0x0000000000000000\"," + "\"number\":\"0x1b4\","
        + "\"parentHash\":\"0xc8ccb81f484a428a3a1669d611f55f880b362b612f726711947d98f5bc5af573\","
        + "\"receiptsRoot\":\"0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421\","
        + "\"sha3Uncles\":\"0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347\","
        + "\"size\":\"0x260\","
        + "\"stateRoot\":\"0xffcb834d62706995e9e7bf10cc9a9e42a82fea998d59b3a5cfad8975dbfe3f87\","
        + "\"timestamp\":\"0x5ed9a43f\"," + "\"totalDifficulty\":\"0x881\"," + "\"transactions\":["
        + "],"
        + "\"transactionsRoot\":\"0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421\","
        + "\"uncles\":[  " + "]}";

    public DatabaseProcessor(CrossStorageApi crossStorageApi, Repository defaultRepo, ParamBean config) {
        super(crossStorageApi, defaultRepo, config);
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        String method = "" + parameters.get("method");
        LOG.info("json rpc: {}, parameters:{}", method, parameters);
        String requestId = "" + parameters.get("id");
        switch (method) {
            case "eth_call":
                result = createResponse(requestId, "0x");
                break;
            case "eth_chainId":
                result = createResponse(requestId, "0x4c");
                break;
            case "web3_clientVersion":
                result = createResponse(requestId, "liquichainCentral");
                break;
            case "net_version":
                result = createResponse(requestId, "7");
                break;
            case "eth_blockNumber":
                result = createResponse(requestId, "0x" + Long.toHexString(BlockForgerScript.blockHeight));
                break;
            case "eth_getBalance":
                result = getBalance(requestId, parameters);
                break;
            case "eth_getTransactionCount":
                result = getTransactionCount(requestId, parameters);
                break;
            case "eth_getBlockByNumber":
                result = createResponse(requestId, SAMPLE_BLOCK);
                break;
            case "eth_estimateGas":
                result = createResponse(requestId, "0x0");
                break;
            case "eth_gasPrice":
                result = createResponse(requestId, "0x0");
                break;
            case "eth_getCode":
                result = getCode(requestId, parameters);
                break;
            case "eth_sendRawTransaction":
                result = sendRawTransaction(requestId, parameters);
                break;
            case "eth_getTransactionByHash":
                result = getTransactionByHash(requestId, parameters);
                break;
            default:
                result = createErrorResponse(requestId, METHOD_NOT_FOUND, NOT_IMPLEMENTED_ERROR);
                break;
        }
    }

    private String getTransactionByHash(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        String hash = retrieveHash(params, 0);
        LOG.info("lookup transaction hexHash={}", hash);

        try {
            Transaction transaction = crossStorageApi
                .find(defaultRepo, Transaction.class)
                .by("hexHash", hash)
                .getResult();
            String transactionDetails = "{\n";
            transactionDetails += "\"blockHash\": \"0x" + transaction.getBlockHash() + "\",\n";
            transactionDetails += "\"blockNumber\": \"" + toBigHex(transaction.getBlockNumber()) + "\",\n";
            transactionDetails += "\"from\": \"0x" + transaction.getFromHexHash() + "\",\n";
            transactionDetails += "\"gas\": \"" + toBigHex(transaction.getGasLimit()) + "\",\n";
            transactionDetails += "\"gasPrice\": \"" + toBigHex(transaction.getGasPrice()) + "\",\n";
            transactionDetails += "\"hash\": \"" + hash + "\",\n";
            transactionDetails += "\"input\": \"\",\n";
            transactionDetails += "\"nonce\": \"" + toBigHex(transaction.getNonce()) + "\",\n";
            if (transaction.getData() != null) {
                if (isJSONValid(transaction.getData())) {
                    transactionDetails += "\"data\": " + transaction.getData() + ",\n";
                } else {
                    transactionDetails += "\"data\": \"" + transaction.getData() + "\",\n";
                }
            }
            transactionDetails += "\"r\": \"" + transaction.getR() + "\",\n";
            transactionDetails += "\"s\": \"" + transaction.getS() + "\",\n";
            transactionDetails += "\"to\": \"0x" + transaction.getToHexHash() + "\",\n";
            transactionDetails +=
                "\"transactionIndex\": \"0x" + toBigHex(transaction.getTransactionIndex() + "") + "\",";
            transactionDetails += "\"v\": \"" + transaction.getV() + "\",";
            transactionDetails += "\"value\": \"" + toBigHex(transaction.getValue()) + "\"\n";
            transactionDetails += "}";
            LOG.info("res={}" + transactionDetails);
            return createResponse(requestId, transactionDetails);
        } catch (Exception e) {
            LOG.error("Resource not found.", e);
            return createErrorResponse(requestId, RESOURCE_NOT_FOUND, "Resource not found");
        }
    }

    private String sendRawTransaction(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        String transactionData = params.get(0);
        String transactionHash = normalizeHash(Hash.sha3(transactionData));
        Transaction existingTransaction = null;
        result = "0x0";
        try {
            existingTransaction = crossStorageApi
                .find(defaultRepo, Transaction.class)
                .by("hexHash", transactionHash)
                .getResult();
        } catch (Exception e) {
            // do nothing, we want transaction to be unique
        }
        if (existingTransaction != null) {
            return createErrorResponse(requestId, INVALID_REQUEST, TRANSACTION_EXISTS_ERROR);
        }

        RawTransaction rawTransaction = TransactionDecoder.decode(transactionData);

        if (rawTransaction instanceof SignedRawTransaction) {
            SignedRawTransaction signedResult = (SignedRawTransaction) rawTransaction;
            Sign.SignatureData signatureData = signedResult.getSignatureData();
            try {
                LOG.info("from:{} chainedId:{}", signedResult.getFrom(), signedResult.getChainId());
                Transaction transaction = new Transaction();
                transaction.setHexHash(transactionHash);
                transaction.setFromHexHash(normalizeHash(signedResult.getFrom()));
                transaction.setToHexHash(normalizeHash(rawTransaction.getTo()));
                transaction.setNonce("" + rawTransaction.getNonce());
                transaction.setGasPrice("" + rawTransaction.getGasPrice());
                transaction.setGasLimit("" + rawTransaction.getGasLimit());
                transaction.setValue("" + rawTransaction.getValue());
                if (rawTransaction.getData() == null || rawTransaction.getData().isEmpty()) {
                    transaction.setData("{\"type\":\"transfer\"}");
                } else {
                    transaction.setData("" + rawTransaction.getData());
                }
                transaction.setSignedHash(transactionData);
                transaction.setCreationDate(java.time.Instant.now());
                transaction.setV(toHex(signatureData.getV()));
                transaction.setS(toHex(signatureData.getS()));
                transaction.setR(toHex(signatureData.getR()));
                LOG.info("transaction:{}", transaction);
                String uuid = crossStorageApi.createOrUpdate(defaultRepo, transaction);
                transferValue(transaction, rawTransaction.getValue());
                result = "0x" + transactionHash;
                LOG.info("created transaction with uuid:{}", uuid);
                if (rawTransaction.getData() != null && rawTransaction.getData().length() > 0) {
                    processTransactionHooks(transaction.getHexHash(), signedResult);
                }
            } catch (Exception e) {
                return createErrorResponse(requestId, TRANSACTION_REJECTED, e.getMessage());
            }
        }
        return createResponse(requestId, result);
    }

    private void transferValue(Transaction transaction, BigInteger value) throws BusinessException {
        String message = "transfer error";
        try {
            message = "cannot find origin wallet";
            Wallet originWallet = crossStorageApi.find(defaultRepo, transaction.getFromHexHash(), Wallet.class);
            message = "cannot find destination wallet";
            crossStorageApi.find(defaultRepo, transaction.getToHexHash(), Wallet.class);
            message = "insufficient balance";
            BigInteger originBalance = new BigInteger(originWallet.getBalance());
            LOG.info("originWallet 0x{} old balance:{}", transaction.getFromHexHash(),
                originWallet.getBalance());
            if (value.compareTo(originBalance) <= 0) {
                BlockForgerScript.addTransaction(transaction);
            } else {
                throw new BusinessException("insufficient balance");
            }
        } catch (Exception e) {
            throw new BusinessException(message);
        }
    }

    private String getTransactionCount(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        String transactionHash = retrieveHash(params, 0);
        try {
            int nbTransaction = (crossStorageApi.find(defaultRepo, Transaction.class)
                                                .by("fromHexHash", transactionHash)
                                                .getResults()).size();
            return createResponse(requestId, toBigHex(nbTransaction + ""));
        } catch (Exception e) {
            return createResponse(requestId, "0x0");
        }
    }

    private String getCode(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        String address = retrieveHash(params, 0);
        try {
            Wallet wallet = crossStorageApi.find(defaultRepo, address, Wallet.class);
            LOG.info("getCode wallet.application.uuid={}", wallet.getApplication().getUuid());
            return createResponse(requestId, "0x" + wallet.getApplication().getUuid());
        } catch (Exception e) {
            LOG.error("Wallet address {} not found", address, e);
            return createErrorResponse(requestId, RESOURCE_NOT_FOUND, "Address not found");
        }
    }

    private String getBalance(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        String address = retrieveHash(params, 0);
        try {
            Wallet wallet = crossStorageApi.find(defaultRepo, address, Wallet.class);
            return createResponse(requestId, toBigHex(wallet.getBalance()));
        } catch (Exception e) {

            return createErrorResponse(requestId, RESOURCE_NOT_FOUND, "Resource not found");
        }
    }
}
