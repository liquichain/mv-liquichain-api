package io.liquichain.api.rpc;

import static io.liquichain.api.rpc.EthApiConstants.*;

import java.util.Map;

import org.meveo.admin.exception.BusinessException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthApiScript extends BlockchainProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(EthApiScript.class);

    private String blockchainType = config.getProperty("txn.blockchain.type", "DATABASE");
    private BLOCKCHAIN_TYPE BLOCKCHAIN_BACKEND = BLOCKCHAIN_TYPE.valueOf(blockchainType);

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        String method = "" + parameters.get("method");
        BlockchainProcessor processor = null;
        if (WalletProcessor.WALLET_METHODS.contains(method)) {
            processor = new WalletProcessor();
        } else {
            switch (BLOCKCHAIN_BACKEND) {
                case BESU:
                    processor = new BesuProcessor();
                    break;
                case FABRIC:
                    break;
                case SMART_CONTRACT:
                    break;
                case BESU_DB:
                    break;
                case DATABASE:
                default:
                    processor = new DatabaseProcessor();
            }
        }
        if (processor != null) {
            processor.execute(parameters);
            result = processor.getResult();
        } else {
            LOG.info("json rpc: {}, parameters:{}", method, parameters);
            String requestId = "" + parameters.get("id");
            result = EthApiUtils.createErrorResponse(requestId, INVALID_REQUEST, NOT_IMPLEMENTED_ERROR);
        }
    }
}
