package io.liquichain.api.rpc;

import static io.liquichain.api.rpc.EthApiConstants.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;

import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.web3j.crypto.*;
import org.web3j.utils.*;

import io.liquichain.core.BlockForgerScript;

public class DatabaseProcessor extends BlockchainProcessor {
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
            e.printStackTrace();
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
                    .by("hexHash", transactionHash).getResult();
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
