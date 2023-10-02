package io.liquichain.api.rpc;

import static io.liquichain.api.rpc.EthApiScript.EthApiConstants.*;
import static io.liquichain.api.rpc.EthApiUtils.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.customEntities.*;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import io.liquichain.api.handler.ContractMethodExecutor;
import io.liquichain.api.handler.EthereumMethodExecutor;
import io.liquichain.api.handler.MethodHandlerInput;
import io.liquichain.api.handler.MethodHandlerResult;
import io.liquichain.core.BlockForgerScript;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.*;
import org.web3j.utils.Numeric;

public class EthApiScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(EthApiScript.class);

    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final ParamBean config = paramBeanFactory.getInstance();
    private BLOCKCHAIN_TYPE BLOCKCHAIN_BACKEND;

    public enum BLOCKCHAIN_TYPE {DATABASE, BESU, FABRIC, BESU_ONLY}

    public static class EthApiConstants {
        public static final String NOT_IMPLEMENTED_ERROR = "Feature not yet implemented";
        public static final String CONTRACT_NOT_ALLOWED_ERROR = "Contract deployment not allowed";
        public static final String NAME_REQUIRED_ERROR = "Wallet name is required";
        public static final String NAME_EXISTS_ERROR = "Wallet with name: %s, already exists";
        public static final String EMAIL_REQUIRED_ERROR = "Email address is required";
        public static final String PHONE_NUMBER_REQUIRED_ERROR = "Phone number is required";
        public static final String EMAIL_EXISTS_ERROR = "Email address: %s, already exists";
        public static final String PHONE_NUMBER_EXISTS_ERROR = "Phone number: %s, already exists";
        public static final String TRANSACTION_EXISTS_ERROR = "Transaction already exists: %s";
        public static final String INVALID_REQUEST = "-32600";
        public static final String INTERNAL_ERROR = "-32603";
        public static final String RESOURCE_NOT_FOUND = "-32001";
        public static final String TRANSACTION_REJECTED = "-32003";
        public static final String METHOD_NOT_FOUND = "-32601";
        public static final String PROXY_REQUEST_ERROR = "Proxy request to remote json-rpc endpoint failed";
        public static final String RECIPIENT_NOT_FOUND = "Recipient wallet does not exist";
    }

    protected String result;

    public String getResult() {
        return this.result;
    }

    private void init() {
        String blockchainType = config.getProperty("txn.blockchain.type", "BESU");
        BLOCKCHAIN_BACKEND = BLOCKCHAIN_TYPE.valueOf(blockchainType);
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        this.init();
        BlockchainProcessor processor = null;
        switch (BLOCKCHAIN_BACKEND) {
        case BESU:
            processor = new BesuProcessor(crossStorageApi, defaultRepo, config);
            break;
        case DATABASE:
        default:
            processor = new DatabaseProcessor(crossStorageApi, defaultRepo, config);
        }
        processor.execute(parameters);
        result = processor.getResult();
    }
}

class TransactionReceipt {
    private boolean success;
    private boolean nullResult;

    public TransactionReceipt(boolean success, boolean nullResult) {
        this.success = success;
        this.nullResult = nullResult;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isNullResult() {
        return nullResult;
    }
}

class EthService {
    private static final Logger LOG = LoggerFactory.getLogger(EthService.class);

    private static final int SLEEP_DURATION = 1000;
    private static final int ATTEMPTS = 40;

    private final String BESU_API_URL;
    private static final int CONNECTION_POOL_SIZE = 50;
    private static final int MAX_POOLED_PER_ROUTE = 5;
    private static final long CONNECTION_TTL = 5;
    private final Client client = new ResteasyClientBuilder()
            .connectionPoolSize(CONNECTION_POOL_SIZE)
            .maxPooledPerRoute(MAX_POOLED_PER_ROUTE)
            .connectionTTL(CONNECTION_TTL, TimeUnit.SECONDS)
            .build();

    public EthService(ParamBean config) {
        BESU_API_URL = config.getProperty("besu.api.url", "https://testnet.liquichain.io/rpc");
    }

    public CompletableFuture<String> callEthJsonRpc(String requestId, Map<String, Object> parameters) {
        Object id = parameters.get("id");
        Object jsonRpcVersion = parameters.get("jsonrpc");
        Object method = parameters.get("method");
        Object params = parameters.get("params");

        String body = "{" +
                "  \"id\": " + formatId(id) + "," +
                "  \"jsonrpc\": \"" + jsonRpcVersion + "\"," +
                "  \"method\": \"" + method + "\"," +
                "  \"params\": " + toJson(params) +
                "}";

        return CompletableFuture.supplyAsync(() -> {
            String result;
            Response response = null;
            LOG.debug("callEthJsonRpc body: {}", body);
            try {
                response = client.target(BESU_API_URL)
                                 .request(MediaType.APPLICATION_JSON)
                                 .post(Entity.json(body));
                result = response.readEntity(String.class);
            } catch (Exception e) {
                LOG.error(PROXY_REQUEST_ERROR, e);
                return createErrorResponse(requestId, INTERNAL_ERROR, PROXY_REQUEST_ERROR);
            } finally {
                if (response != null) {
                    response.close();
                }
            }

            LOG.debug("callEthJsonRpc result: {}", result);
            return result;
        });
    }

    private TransactionReceipt retrieveTransactionReceipt(String requestId, String hash,
            Map<String, Object> parameters) {
        LOG.debug("received hash: {}", hash);
        Map<String, Object> receiptParams = new HashMap<>() {{
            put("id", parameters.get("id"));
            put("jsonrpc", parameters.get("jsonrpc"));
            put("method", "eth_getTransactionReceipt");
            put("params", List.of(hash));
        }};
        LOG.debug("transaction receipt parameters: {}", toJson(receiptParams));

        String receiptResult = null;
        try {
            receiptResult = callEthJsonRpc(requestId, receiptParams).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        LOG.debug("transaction receipt result: {}", receiptResult);

        Map<String, Object> receiptResultMap = convert(receiptResult);
        Object errorMessage = receiptResultMap.get("error");
        boolean hasError = errorMessage != null && StringUtils.isNotEmpty(errorMessage.toString());
        if (hasError) {
            throw new RuntimeException(errorMessage.toString());
        }

        Object resultObject = receiptResultMap.get("result");
        if (resultObject == null) {
            return new TransactionReceipt(false, true);
        }

        Map<String, Object> resultMap = (Map<String, Object>) resultObject;
        Object resultError = resultMap.get("error");
        String transactionError = resultError != null ? resultError.toString() : null;
        if (StringUtils.isNotEmpty(transactionError)) {
            throw new RuntimeException(transactionError);
        }

        boolean success = "0x1".equals(resultMap.get("status"));
        return new TransactionReceipt(success, false);
    }

    public TransactionReceipt waitForTransactionReceipt(String requestId, String hash,
            Map<String, Object> parameters) {
        if (hash != null) {
            TransactionReceipt transactionReceipt = retrieveTransactionReceipt(requestId, hash, parameters);
            for (int attempt = ATTEMPTS; attempt > 0; attempt--) {
                if (transactionReceipt.isNullResult()) {
                    try {
                        Thread.sleep(SLEEP_DURATION);
                        transactionReceipt = retrieveTransactionReceipt(requestId, hash, parameters);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Encountered error while delaying thread.", e);
                    }
                } else {
                    break;
                }
            }
            return transactionReceipt;
        } else {
            throw new RuntimeException("No transaction hash provided.");
        }
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
        LOG.debug("addTransactionHook key: {}", key);
        isHookAdded = !TRANSACTION_HOOKS.containsKey(key);
        if (isHookAdded) {
            Pattern pattern = Pattern.compile(regex);
            TRANSACTION_HOOKS.put(key, new Object[] { pattern, script });
        }
        return isHookAdded;
    }

    protected void processTransactionHooks(String transactionHash, SignedRawTransaction transaction) {
        try {
            String data = new String(new BigInteger(transaction.getData(), 16).toByteArray());
            LOG.debug("try matching {} hooks", TRANSACTION_HOOKS.size());
            TRANSACTION_HOOKS.forEach((String key, Object[] hook) -> {
                LOG.debug("try hook {} on {}", key, data);
                Pattern pattern = (Pattern) hook[0];
                Script script = (Script) hook[1];
                Matcher matcher = pattern.matcher(data);
                if (matcher.find()) {
                    LOG.debug(" hook {} matched", key);
                    Map<String, Object> context = new HashMap<>();
                    context.put("transaction", transaction);
                    context.put("transactionHash", transactionHash);
                    context.put("matcher", matcher);
                    try {
                        script.execute(context);
                        if (context.containsKey("result")) {
                            LOG.debug(" hook result:{} ", context.get("result"));
                        }
                    } catch (Exception e) {
                        LOG.error("error while invoking transaction hook {}", script, e);
                    }
                } else {
                    LOG.debug(" hook {} matched", key);
                }
            });
            if (data.contains("orderId")) {
                LOG.debug("Detected orderId: {}", data);
            }
        } catch (Exception e) {
            LOG.debug("Error while detecting order", e);
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
        LOG.debug("messageHash={}", Numeric.toHexString(messageHash));
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
        LOG.debug("address: " + address);
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

    private final EthService ethService;
    private final Map<String, EthereumMethod> ethereumMethods;
    private final String NETWORK_ID = config.getProperty("eth.network.id", "1662");
    private final String CHAIN_ID = Integer.toHexString(Integer.parseInt(NETWORK_ID));

    public BesuProcessor(CrossStorageApi crossStorageApi, Repository defaultRepo, ParamBean config) {
        super(crossStorageApi, defaultRepo, config);
        this.ethService = new EthService(config);
        List<EthereumMethod> ethereumMethods = crossStorageApi.find(defaultRepo, EthereumMethod.class).getResults();
        boolean hasEthereumMethods = ethereumMethods != null && !ethereumMethods.isEmpty();
        if (hasEthereumMethods) {
            this.ethereumMethods = ethereumMethods
                    .stream()
                    .collect(Collectors.toMap(EthereumMethod::getMethod, method -> method));
        } else {
            this.ethereumMethods = new HashMap<>();
        }
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        String method = "" + parameters.get("method");
        String requestId = "" + parameters.get("id");
        
      LOG.debug("json rpc: {}, parameters:{}", method, parameters);

        EthereumMethod ethereumMethod = ethereumMethods.get(method);
        if (ethereumMethod != null) {
            EthereumMethodExecutor executor = new EthereumMethodExecutor(ethereumMethods);
            result = executor.execute(requestId, parameters);
            return;
        }

        switch (method) {
        case "net_version":
            result = createResponse(requestId, NETWORK_ID);
            break;
        case "eth_chainId":
            result = createResponse(requestId, CHAIN_ID);
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
            try {
                result = ethService.callEthJsonRpc(requestId, parameters).get();
            } catch (InterruptedException | ExecutionException e) {
                result = createErrorResponse(requestId, TRANSACTION_REJECTED, e.getMessage());
            }
            break;
        }
    }

    private String sendRawTransaction(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        LOG.info("sendRawTransaction parameters: {}", params);
        String data = params.get(0);
        String transactionHash = normalizeHash(Hash.sha3(data));
        LOG.debug("computed transactionHash: {}", transactionHash);
        try {
            Transaction existingTransaction = crossStorageApi
                    .find(defaultRepo, Transaction.class)
                    .by("hexHash", transactionHash).getResult();
            if (existingTransaction != null) {
                String message = String.format(TRANSACTION_EXISTS_ERROR, transactionHash);
                return createErrorResponse(requestId, TRANSACTION_REJECTED, message);
            }
        } catch (Exception e) {
            return createErrorResponse(requestId, RESOURCE_NOT_FOUND, e.getMessage());
        }

        RawTransaction rawTransaction = TransactionDecoder.decode(data);
        String rawRecipient = rawTransaction.getTo();
        LOG.debug("RawTransaction recipient: {}", rawRecipient);

        // as per besu documentation
        // (https://besu.hyperledger.org/en/stable/Tutorials/Contracts/Deploying-Contracts/):
        // to - address of the receiver. To deploy a contract, set to null.
        // or it can also be set to 0x0 or 0x80 as per:
        // (https://stackoverflow.com/questions/48219716/what-is-address0-in-solidity)
        if (rawRecipient == null || "0x0".equals(rawRecipient) || "0x80".equals(rawRecipient)) {
            return createErrorResponse(requestId, INVALID_REQUEST, CONTRACT_NOT_ALLOWED_ERROR);
        }

        String smartContract;
        boolean isSmartContract = false;
        String defaultData = "{\"type\":\"transfer\",\"description\":\"Transfer coins\"}";
        String defaultValue = rawTransaction.getValue().toString();
        MethodHandlerResult handlerResult =
                new MethodHandlerResult("transfer", defaultData, rawRecipient, defaultValue);

        LiquichainApp liquichainApp = null;
        try {
            List<LiquichainApp> apps = crossStorageApi.find(defaultRepo, LiquichainApp.class).getResults();
            liquichainApp = apps.stream().filter(app -> {
                String contract = app.getHexCode();
                return contract != null && addHexPrefix(contract).equalsIgnoreCase(rawRecipient);
            }).findFirst().orElse(null);
            isSmartContract = liquichainApp != null;
        } catch (Exception e) {
            // if not found, then it is not a smart contract
        }

        LOG.debug("isSmartContract: {}", isSmartContract);
        if (isSmartContract) {
            smartContract = liquichainApp.getHexCode();
            Map<String, String> handlers = liquichainApp.getContractMethodHandlers();
            String abi = liquichainApp.getAbi();
            boolean hasAbi = abi != null && abi.length() > 0;
            LOG.debug("hasAbi: {}", hasAbi);
            if (hasAbi) {
                ContractMethodExecutor executor = new ContractMethodExecutor(abi, handlers);
                MethodHandlerInput input = new MethodHandlerInput(rawTransaction, smartContract, transactionHash, data);
                handlerResult = executor.execute(input);
            }
        } else {
            Wallet recipientWallet;
            try {
                recipientWallet = crossStorageApi.find(defaultRepo, normalizeHash(rawRecipient), Wallet.class);
            } catch (Exception e) {
                return createErrorResponse(requestId, TRANSACTION_REJECTED, RECIPIENT_NOT_FOUND);
            }
            if (recipientWallet == null) {
                return createErrorResponse(requestId, TRANSACTION_REJECTED, RECIPIENT_NOT_FOUND);
            }
        }
        LOG.debug("Handler result: {}", toJson(handlerResult));

        try {
            result = ethService.callEthJsonRpc(requestId, parameters).get();
        } catch (InterruptedException | ExecutionException e) {
            return createErrorResponse(requestId, TRANSACTION_REJECTED, e.getMessage());
        }
        Map<String, Object> resultMap = convert(result);
        LOG.debug("sendRawTransaction result: {}", toJson(result));

        Object errorMessage = resultMap.get("error");
        boolean hasError = errorMessage != null && StringUtils.isNotEmpty(errorMessage.toString());
        if (hasError) {
            return result;
        }

        Object receivedHash = resultMap.get("result");
        if (receivedHash == null) {
            return createErrorResponse(requestId, TRANSACTION_REJECTED, "No transaction hash received.");
        }

        TransactionReceipt transactionReceipt = null;
        try {
            transactionReceipt = ethService.waitForTransactionReceipt(requestId, receivedHash.toString(), parameters);
        } catch (Exception e) {
            return createErrorResponse(requestId, TRANSACTION_REJECTED, e.getMessage());
        }

        if (!transactionReceipt.isSuccess()) {
            return createErrorResponse(requestId, TRANSACTION_REJECTED,
                    "Transaction: " + receivedHash + " failed, check transaction logs.");
        }

        try {
            Transaction transaction;
            if (handlerResult.getTransaction() == null) {
                String recipient = Objects.requireNonNullElse(handlerResult.getRecipient(), rawRecipient);
                transaction = buildTransactionDetails(rawTransaction, transactionHash, recipient, data);
                transaction.setType(handlerResult.getTransactionType());
                transaction.setData(handlerResult.getExtraData());
                transaction.setValue(handlerResult.getValue());
            } else {
                transaction = handlerResult.getTransaction();
            }
            LOG.debug("Transaction CEI details: {}", toJson(transaction));

            String uuid = crossStorageApi.createOrUpdate(defaultRepo, transaction);
            LOG.debug("Created transaction on DB with uuid: {}", uuid);
        } catch (Exception e) {
            return createErrorResponse(requestId, TRANSACTION_REJECTED, e.getMessage());
        }

        return result;
    }
}

class DatabaseProcessor extends BlockchainProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseProcessor.class);
    private final String NETWORK_ID = config.getProperty("eth.network.id", "1662");
    private final String CHAIN_ID = Integer.toHexString(Integer.parseInt(NETWORK_ID));

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
        LOG.debug("json rpc: {}, parameters:{}", method, parameters);
        String requestId = "" + parameters.get("id");
        switch (method) {
        case "eth_call":
            result = createResponse(requestId, "0x");
            break;
        case "eth_chainId":
            result = createResponse(requestId, CHAIN_ID);
            break;
        case "web3_clientVersion":
            result = createResponse(requestId, "liquichainCentral");
            break;
        case "net_version":
            result = createResponse(requestId, NETWORK_ID);
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
        LOG.debug("lookup transaction hexHash={}", hash);

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
            LOG.debug("res={}" + transactionDetails);
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
                LOG.debug("from:{} chainedId:{}", signedResult.getFrom(), signedResult.getChainId());
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
                LOG.debug("transaction:{}", transaction);
                String uuid = crossStorageApi.createOrUpdate(defaultRepo, transaction);
                transferValue(transaction, rawTransaction.getValue());
                result = "0x" + transactionHash;
                LOG.debug("created transaction with uuid:{}", uuid);
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
            LOG.debug("originWallet 0x{} old balance:{}", transaction.getFromHexHash(),
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
            LOG.debug("getCode wallet.application.uuid={}", wallet.getApplication().getUuid());
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


