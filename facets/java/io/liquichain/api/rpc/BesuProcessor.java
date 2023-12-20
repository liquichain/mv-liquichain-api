package io.liquichain.api.rpc;

import static io.liquichain.api.rpc.EthApiScript.EthApiConstants.*;
import static io.liquichain.api.rpc.EthService.IS_SUCCESS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.EthereumMethod;
import org.meveo.model.customEntities.LiquichainApp;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import io.liquichain.api.handler.ContractMethodExecutor;
import io.liquichain.api.handler.EthereumMethodExecutor;
import io.liquichain.api.handler.MethodHandlerInput;
import io.liquichain.api.handler.MethodHandlerResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionDecoder;

public class BesuProcessor extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(BesuProcessor.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ParamBean config = paramBeanFactory.getInstance();
    private final String NETWORK_ID = config.getProperty("eth.network.id", "1662");
    private final String CHAIN_ID = "0x" + Integer.toHexString(Integer.parseInt(NETWORK_ID));

    //    private final ScriptInstanceService scriptInstanceService = getCDIBean(ScriptInstanceService.class);
    //    private final ScriptInterface ethApiUtilsScript = scriptInstanceService.getExecutionEngine(
    //            EthApiUtils.class.getName(), null);
    private final EthApiUtils ethApiUtils = new EthApiUtils();
    private final EthService ethService = new EthService();

    private final Map<String, EthereumMethod> ethereumMethods;
    private String result;

    public String getResult() {
        return this.result;
    }

    public BesuProcessor() {
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
            result = ethApiUtils.createResponse(requestId, NETWORK_ID);
            break;
        case "eth_chainId":
            result = ethApiUtils.createResponse(requestId, CHAIN_ID);
            break;
        case "eth_sendSignedTransaction":
        case "eth_sendRawTransaction":
            result = sendRawTransaction(requestId, parameters);
            break;
        case "eth_getProof":
        case "eth_getWork":
        case "eth_submitWork":
        case "eea_sendRawTransaction":
            result = ethApiUtils.createErrorResponse(requestId, INVALID_REQUEST, NOT_IMPLEMENTED_ERROR);
            break;
        default:
            try {
                result = ethService.callEthJsonRpc(requestId, parameters).get();
            } catch (InterruptedException | ExecutionException e) {
                result = ethApiUtils.createErrorResponse(requestId, TRANSACTION_REJECTED, e.getMessage());
            }
            break;
        }
    }

    private String sendRawTransaction(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        LOG.info("sendRawTransaction parameters: {}", params);
        String data = params.get(0);
        String transactionHash = ethApiUtils.normalizeHash(Hash.sha3(data));
        LOG.debug("computed transactionHash: {}", transactionHash);
        try {
            Transaction existingTransaction = crossStorageApi
                    .find(defaultRepo, Transaction.class)
                    .by("hexHash", transactionHash).getResult();
            if (existingTransaction != null) {
                String message = String.format(TRANSACTION_EXISTS_ERROR, transactionHash);
                return ethApiUtils.createErrorResponse(requestId, TRANSACTION_REJECTED, message);
            }
        } catch (Exception e) {
            return ethApiUtils.createErrorResponse(requestId, RESOURCE_NOT_FOUND, e.getMessage());
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
            return ethApiUtils.createErrorResponse(requestId, INVALID_REQUEST, CONTRACT_NOT_ALLOWED_ERROR);
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
                return contract != null && ethApiUtils.addHexPrefix(contract).equalsIgnoreCase(rawRecipient);
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
                LOG.info("Start calling smart contract method handler.");
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                ContractMethodExecutor executor = new ContractMethodExecutor();
                executor.init(abi, handlers);
                MethodHandlerInput input = new MethodHandlerInput();
                input.init(rawTransaction, smartContract, transactionHash, data);
                handlerResult = executor.execute(input);
                stopWatch.stop();
                LOG.info("Executed smart contract handler in: {}ms", stopWatch.getTime());
            }
        } else {
            Wallet recipientWallet;
            try {
                recipientWallet = crossStorageApi.find(defaultRepo, ethApiUtils.normalizeHash(rawRecipient),
                        Wallet.class);
            } catch (Exception e) {
                return ethApiUtils.createErrorResponse(requestId, TRANSACTION_REJECTED, RECIPIENT_NOT_FOUND);
            }
            if (recipientWallet == null) {
                return ethApiUtils.createErrorResponse(requestId, TRANSACTION_REJECTED, RECIPIENT_NOT_FOUND);
            }
        }
        LOG.debug("Handler result: {}", ethApiUtils.toJson(handlerResult));

        try {
            LOG.info("Start calling smart contract method on blockchain.");
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            result = ethService.callEthJsonRpc(requestId, parameters).get();
            stopWatch.stop();
            LOG.info("Executed smart contract method call on blockchain in: {}ms", stopWatch.getTime());
        } catch (InterruptedException | ExecutionException e) {
            return ethApiUtils.createErrorResponse(requestId, TRANSACTION_REJECTED, e.getMessage());
        }
        Map<String, Object> resultMap = ethApiUtils.convert(result);
        LOG.debug("sendRawTransaction result: {}", ethApiUtils.toJson(result));

        Object errorMessage = resultMap.get("error");
        boolean hasError = errorMessage != null && StringUtils.isNotEmpty(errorMessage.toString());
        if (hasError) {
            return result;
        }

        Object receivedHash = resultMap.get("result");
        if (receivedHash == null) {
            return ethApiUtils.createErrorResponse(requestId, TRANSACTION_REJECTED, "No transaction hash received.");
        }

        Map<String, Boolean> transactionReceipt = null;
        try {
            LOG.info("Retrieving transaction receipt for hash: {}", receivedHash);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            transactionReceipt = ethService.waitForTransactionReceipt(requestId, receivedHash.toString(), parameters);
            stopWatch.stop();
            LOG.info("Retrieving transaction reciept took: {}ms", stopWatch.getTime());
        } catch (Exception e) {
            return ethApiUtils.createErrorResponse(requestId, TRANSACTION_REJECTED, e.getMessage());
        }

        if (!transactionReceipt.get(IS_SUCCESS)) {
            return ethApiUtils.createErrorResponse(requestId, TRANSACTION_REJECTED,
                    "Transaction: " + receivedHash + " failed, check transaction logs.");
        }

        try {
            Transaction transaction;
            if (handlerResult.getTransaction() == null) {
                String recipient = Objects.requireNonNullElse(handlerResult.getRecipient(), rawRecipient);
                transaction = ethApiUtils.buildTransactionDetails(rawTransaction, transactionHash, recipient, data);
                transaction.setType(handlerResult.getTransactionType());
                transaction.setData(handlerResult.getExtraData());
                transaction.setValue(handlerResult.getValue());
            } else {
                transaction = handlerResult.getTransaction();
            }
            LOG.debug("Transaction CEI details: {}", ethApiUtils.toJson(transaction));

            String uuid = crossStorageApi.createOrUpdate(defaultRepo, transaction);
            LOG.debug("Created transaction on DB with uuid: {}", uuid);
        } catch (Exception e) {
            return ethApiUtils.createErrorResponse(requestId, TRANSACTION_REJECTED, e.getMessage());
        }

        return result;
    }

}