package io.liquichain.api.handler;

import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.RawTransaction;

public class MethodHandlerInput extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandlerInput.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ParamBean config = paramBeanFactory.getInstance();

    private RawTransaction rawTransaction;
    private String smartContractAddress;
    private String transactionHash;
    private String data;

    public RawTransaction getRawTransaction() {
        return rawTransaction;
    }

    public String getSmartContractAddress() {
        return smartContractAddress;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public String getData() {
        return data;
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

    public void init(RawTransaction rawTransaction, String smartContractAddress, String transactionHash, String data) {
        this.rawTransaction = rawTransaction;
        this.smartContractAddress = smartContractAddress;
        this.transactionHash = transactionHash;
        this.data = data;
    }

    @Override
    public String toString() {
        return "MethodHandlerInput{" +
                "rawTransaction=" + rawTransaction +
                ", smartContractAddress='" + smartContractAddress + "'" +
                ", transactionHash=" + transactionHash +
                ", data='" + data +
                '}';
    }

}
