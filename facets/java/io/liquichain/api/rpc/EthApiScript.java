package io.liquichain.api.rpc;

import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthApiScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(EthApiScript.class);

    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ParamBean config = paramBeanFactory.getInstance();
    private final ScriptInstanceService scriptInstanceService = getCDIBean(ScriptInstanceService.class);

    public enum BLOCKCHAIN_TYPE {DATABASE, BESU, FABRIC, BESU_ONLY;}

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

    private String result;

    public String getResult() {
        return this.result;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        String blockchainType = config.getProperty("txn.blockchain.type", "BESU");
        BLOCKCHAIN_TYPE type = BLOCKCHAIN_TYPE.valueOf(blockchainType);
        switch (type) {
        case BESU:
            result = executeOnBesu(parameters);
            break;
        case DATABASE:
        default:
            result = executeOnDatabase(parameters);
        }
    }

    private String executeOnBesu(Map<String, Object> parameters) throws BusinessException {
        BesuProcessor besuProcessor = (BesuProcessor) scriptInstanceService.getExecutionEngine(
                BesuProcessor.class.getName(), null);
        besuProcessor.execute(parameters);
        return besuProcessor.getResult();
    }

    private String executeOnDatabase(Map<String, Object> parameters) throws BusinessException {
        DatabaseProcessor databaseProcessor = (DatabaseProcessor) scriptInstanceService.getExecutionEngine(
                DatabaseProcessor.class.getName(), null);
        databaseProcessor.execute(parameters);
        return databaseProcessor.getResult();
    }
}


