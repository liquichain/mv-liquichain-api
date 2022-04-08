package io.liquichain.api.rpc;

import static io.liquichain.api.rpc.EthApiConstants.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;

import org.meveo.admin.exception.BusinessException;
import org.meveo.model.customEntities.Transaction;

import org.apache.commons.lang3.math.NumberUtils;

import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.web3j.crypto.*;
import org.web3j.utils.*;

import io.liquichain.api.rpc.BlockchainProcessor;

public class BesuProcessor extends BlockchainProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(BesuProcessor.class);

    private String BESU_API_URL = config.getProperty("besu.api.url", "https://testnet.liquichain.io/rpc");

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
        LOG.info("BESU_API_URL={}", BESU_API_URL);
        Client client = ClientBuilder.newClient();
        String response = client.target(BESU_API_URL)
                                .request(MediaType.APPLICATION_JSON)
                                .post(Entity.json(body), String.class);
        LOG.info("callProxy response={}", response);
        return response;
    }

    private synchronized String callEthJsonRpc(String requestId, Map<String, Object> parameters) {
        Object id = parameters.get("id");
        String idFormat = id == null || NumberUtils.isParsable("" + id) ? "\"id\": %s," : "\"id\": \"%s\",";
        String requestBody = new StringBuilder()
                .append("{")
                .append(String.format(idFormat, id))
                .append(String.format("\"jsonrpc\":\"%s\",", parameters.get("jsonrpc")))
                .append(String.format("\"method\":\"%s\",", parameters.get("method")))
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

        if (rawTransaction instanceof SignedRawTransaction) {
            SignedRawTransaction signedTransaction = (SignedRawTransaction) rawTransaction;
            Sign.SignatureData signatureData = signedTransaction.getSignatureData();
            try {
                String v = toHex(signatureData.getV());
                String s = toHex(signatureData.getS());
                String r = toHex(signatureData.getR());
                LOG.info("from:{} chainId:{} , v:{} , r:{} , s:{}",
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
                LOG.info("Created transaction on DB with uuid: {}", uuid);
            } catch (Exception e) {
                return createErrorResponse(requestId, TRANSACTION_REJECTED, e.getMessage());
            }
        }
        return result;
    }
}
