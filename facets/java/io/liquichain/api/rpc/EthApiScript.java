package io.liquichain.api.rpc;

import static io.liquichain.api.rpc.EthApiConstants.*;

import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthApiScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(EthApiScript.class);

    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private ParamBean config = paramBeanFactory.getInstance();

    private String blockchainType = config.getProperty("txn.blockchain.type", "DATABASE");
    private BLOCKCHAIN_TYPE BLOCKCHAIN_BACKEND = BLOCKCHAIN_TYPE.valueOf(blockchainType);

    private String result;

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        BlockchainProcessor processor = null;
        switch (BLOCKCHAIN_BACKEND) {
            case BESU:
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
        if (processor != null) {
            processor.execute(parameters);
            result = processor.getResult();
        } else {
            String method = "" + parameters.get("method");
            LOG.info("json rpc: {}, parameters:{}", method, parameters);
            String requestId = "" + parameters.get("id");
            result = EthApiUtils.createErrorResponse(requestId, INVALID_REQUEST, NOT_IMPLEMENTED_ERROR);
        }
    }

    public String getResult() {
        return result;
    }
}
