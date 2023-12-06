package io.liquichain.api.rpc;

import static io.liquichain.api.rpc.EthApiScript.EthApiConstants.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;
import org.meveo.service.storage.RepositoryService;

import io.liquichain.core.BlockForgerScript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.*;

public class DatabaseProcessor extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseProcessor.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ParamBean config = paramBeanFactory.getInstance();

    private final ScriptInstanceService scriptInstanceService = getCDIBean(ScriptInstanceService.class);
    private final EthApiUtils ethApiUtils = (EthApiUtils) scriptInstanceService.getExecutionEngine(
            EthApiUtils.class.getName(), null);
    private final BlockchainProcessor blockchainProcessor =
            (BlockchainProcessor) scriptInstanceService.getExecutionEngine(BlockchainProcessor.class.getName(), null);
    private final String NETWORK_ID = config.getProperty("eth.network.id", "1662");
    private final String CHAIN_ID = "0x" + Integer.toHexString(Integer.parseInt(NETWORK_ID));

    private String result;

    public String getResult() {
        return this.result;
    }

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

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        String method = "" + parameters.get("method");
        LOG.debug("json rpc: {}, parameters:{}", method, parameters);
        String requestId = "" + parameters.get("id");
        switch (method) {
        case "eth_call":
            result = ethApiUtils.createResponse(requestId, "0x");
            break;
        case "eth_chainId":
            result = ethApiUtils.createResponse(requestId, CHAIN_ID);
            break;
        case "web3_clientVersion":
            result = ethApiUtils.createResponse(requestId, "liquichainCentral");
            break;
        case "net_version":
            result = ethApiUtils.createResponse(requestId, NETWORK_ID);
            break;
        case "eth_blockNumber":
            result = ethApiUtils.createResponse(requestId, "0x" + Long.toHexString(BlockForgerScript.blockHeight));
            break;
        case "eth_getBalance":
            result = getBalance(requestId, parameters);
            break;
        case "eth_getTransactionCount":
            result = getTransactionCount(requestId, parameters);
            break;
        case "eth_getBlockByNumber":
            result = ethApiUtils.createResponse(requestId, SAMPLE_BLOCK);
            break;
        case "eth_estimateGas":
            result = ethApiUtils.createResponse(requestId, "0x0");
            break;
        case "eth_gasPrice":
            result = ethApiUtils.createResponse(requestId, "0x0");
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
            result = ethApiUtils.createErrorResponse(requestId, METHOD_NOT_FOUND, NOT_IMPLEMENTED_ERROR);
            break;
        }
    }

    private String getTransactionByHash(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        String hash = ethApiUtils.retrieveHash(params, 0);
        LOG.debug("lookup transaction hexHash={}", hash);

        try {
            Transaction transaction = crossStorageApi
                    .find(defaultRepo, Transaction.class)
                    .by("hexHash", hash)
                    .getResult();
            String transactionDetails = "{\n";
            transactionDetails += "\"blockHash\": \"0x" + transaction.getBlockHash() + "\",\n";
            transactionDetails += "\"blockNumber\": \"" + ethApiUtils.toBigHex(transaction.getBlockNumber()) + "\",\n";
            transactionDetails += "\"from\": \"0x" + transaction.getFromHexHash() + "\",\n";
            transactionDetails += "\"gas\": \"" + ethApiUtils.toBigHex(transaction.getGasLimit()) + "\",\n";
            transactionDetails += "\"gasPrice\": \"" + ethApiUtils.toBigHex(transaction.getGasPrice()) + "\",\n";
            transactionDetails += "\"hash\": \"" + hash + "\",\n";
            transactionDetails += "\"input\": \"\",\n";
            transactionDetails += "\"nonce\": \"" + ethApiUtils.toBigHex(transaction.getNonce()) + "\",\n";
            if (transaction.getData() != null) {
                if (ethApiUtils.isJSONValid(transaction.getData())) {
                    transactionDetails += "\"data\": " + transaction.getData() + ",\n";
                } else {
                    transactionDetails += "\"data\": \"" + transaction.getData() + "\",\n";
                }
            }
            transactionDetails += "\"r\": \"" + transaction.getR() + "\",\n";
            transactionDetails += "\"s\": \"" + transaction.getS() + "\",\n";
            transactionDetails += "\"to\": \"0x" + transaction.getToHexHash() + "\",\n";
            transactionDetails +=
                    "\"transactionIndex\": \"0x" + ethApiUtils.toBigHex(transaction.getTransactionIndex() + "") + "\",";
            transactionDetails += "\"v\": \"" + transaction.getV() + "\",";
            transactionDetails += "\"value\": \"" + ethApiUtils.toBigHex(transaction.getValue()) + "\"\n";
            transactionDetails += "}";
            LOG.debug("res={}" + transactionDetails);
            return ethApiUtils.createResponse(requestId, transactionDetails);
        } catch (Exception e) {
            LOG.error("Resource not found.", e);
            return ethApiUtils.createErrorResponse(requestId, RESOURCE_NOT_FOUND, "Resource not found");
        }
    }

    private String sendRawTransaction(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        String transactionData = params.get(0);
        String transactionHash = ethApiUtils.normalizeHash(Hash.sha3(transactionData));
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
            return ethApiUtils.createErrorResponse(requestId, INVALID_REQUEST, TRANSACTION_EXISTS_ERROR);
        }

        RawTransaction rawTransaction = TransactionDecoder.decode(transactionData);

        if (rawTransaction instanceof SignedRawTransaction) {
            SignedRawTransaction signedResult = (SignedRawTransaction) rawTransaction;
            Sign.SignatureData signatureData = signedResult.getSignatureData();
            try {
                LOG.debug("from:{} chainedId:{}", signedResult.getFrom(), signedResult.getChainId());
                Transaction transaction = new Transaction();
                transaction.setHexHash(transactionHash);
                transaction.setFromHexHash(ethApiUtils.normalizeHash(signedResult.getFrom()));
                transaction.setToHexHash(ethApiUtils.normalizeHash(rawTransaction.getTo()));
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
                transaction.setV(ethApiUtils.toHex(signatureData.getV()));
                transaction.setS(ethApiUtils.toHex(signatureData.getS()));
                transaction.setR(ethApiUtils.toHex(signatureData.getR()));
                LOG.debug("transaction:{}", transaction);
                String uuid = crossStorageApi.createOrUpdate(defaultRepo, transaction);
                transferValue(transaction, rawTransaction.getValue());
                result = "0x" + transactionHash;
                LOG.debug("created transaction with uuid:{}", uuid);
                if (rawTransaction.getData() != null && rawTransaction.getData().length() > 0) {
                    blockchainProcessor.processTransactionHooks(transaction.getHexHash(), signedResult);
                }
            } catch (Exception e) {
                return ethApiUtils.createErrorResponse(requestId, TRANSACTION_REJECTED, e.getMessage());
            }
        }
        return ethApiUtils.createResponse(requestId, result);
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
        String transactionHash = ethApiUtils.retrieveHash(params, 0);
        try {
            int nbTransaction = (crossStorageApi.find(defaultRepo, Transaction.class)
                                                .by("fromHexHash", transactionHash)
                                                .getResults()).size();
            return ethApiUtils.createResponse(requestId, ethApiUtils.toBigHex(nbTransaction + ""));
        } catch (Exception e) {
            return ethApiUtils.createResponse(requestId, "0x0");
        }
    }

    private String getCode(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        String address = ethApiUtils.retrieveHash(params, 0);
        try {
            Wallet wallet = crossStorageApi.find(defaultRepo, address, Wallet.class);
            LOG.debug("getCode wallet.application.uuid={}", wallet.getApplication().getUuid());
            return ethApiUtils.createResponse(requestId, "0x" + wallet.getApplication().getUuid());
        } catch (Exception e) {
            LOG.error("Wallet address {} not found", address, e);
            return ethApiUtils.createErrorResponse(requestId, RESOURCE_NOT_FOUND, "Address not found");
        }
    }

    private String getBalance(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        String address = ethApiUtils.retrieveHash(params, 0);
        try {
            Wallet wallet = crossStorageApi.find(defaultRepo, address, Wallet.class);
            return ethApiUtils.createResponse(requestId, ethApiUtils.toBigHex(wallet.getBalance()));
        } catch (Exception e) {

            return ethApiUtils.createErrorResponse(requestId, RESOURCE_NOT_FOUND, "Resource not found");
        }
    }

}