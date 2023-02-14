package io.liquichain.api.handler;

import java.util.Map;

import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.RawTransaction;

public class MethodHandlerInput extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandlerInput.class);

    private final RawTransaction rawTransaction;
    private final String smartContractAddress;
    private final CrossStorageApi crossStorageApi;
    private final Repository defaultRepo;
    private final ParamBean config;

    public MethodHandlerInput(CrossStorageApi crossStorageApi, Repository defaultRepo, ParamBean config,
        RawTransaction rawTransaction, String smartContractAddress) {
        this.crossStorageApi = crossStorageApi;
        this.defaultRepo = defaultRepo;
        this.config = config;
        this.rawTransaction = rawTransaction;
        this.smartContractAddress = smartContractAddress;
    }

    public RawTransaction getRawTransaction() {
        return rawTransaction;
    }

    public String getSmartContractAddress() {
        return smartContractAddress;
    }

    public CrossStorageApi getCrossStorageApi() {
        return crossStorageApi;
    }

    public Repository getDefaultRepo() {
        return defaultRepo;
    }

    public ParamBean getConfig() {
        return config;
    }

    @Override public String toString() {
        return "MethodHandlerInput{" +
            "rawTransaction=" + rawTransaction +
            ", smartContractAddress='" + smartContractAddress + '\'' +
            '}';
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
    }

}
