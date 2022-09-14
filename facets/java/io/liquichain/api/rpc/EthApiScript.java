package io.liquichain.api.rpc;

import static io.liquichain.api.rpc.EthApiConstants.*;

import java.util.Map;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.liquichain.api.rpc.BlockchainProcessor;

public class EthApiScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(EthApiScript.class);

    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    protected ParamBean config = paramBeanFactory.getInstance();

    private final String blockchainType = config.getProperty("txn.blockchain.type", "DATABASE");
    private final BLOCKCHAIN_TYPE BLOCKCHAIN_BACKEND = BLOCKCHAIN_TYPE.valueOf(blockchainType);

    protected String result;

    public String getResult() {
        return this.result;
    }

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
