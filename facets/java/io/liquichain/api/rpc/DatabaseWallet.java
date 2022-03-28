package io.liquichain.api.rpc;

import java.util.List;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.LiquichainApp;
import org.meveo.model.customEntities.VerifiedEmail;
import org.meveo.model.customEntities.VerifiedPhoneNumber;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.web3j.crypto.*;

import io.liquichain.core.BlockForgerScript;

public class DatabaseWallet extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseWallet.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private ParamBean config = paramBeanFactory.getInstance();

    private String APP_NAME = config.getProperty("eth.api.appname", "licoin");

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        // TODO Auto-generated method stub
    }

    public String createWallet(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        String name = params.get(0);
        String walletHash = EthApiUtils.retrieveHash(params, 1);
        String accountHash = EthApiUtils.retrieveHash(params, 2);
        String publicInfo = params.get(3);

        Wallet wallet = null;
        try {
            wallet = crossStorageApi.find(defaultRepo, walletHash, Wallet.class);
        } catch (EntityDoesNotExistsException e) {
            // do nothing, we want wallet to be unique
        }
        if (wallet != null) {
            return EthApiUtils.createErrorResponse(requestId, INVALID_REQUEST, WALLET_EXISTS_ERROR);
        } else {
            wallet = new Wallet();
        }
        LiquichainApp app = null;
        try {
            app = crossStorageApi.find(defaultRepo, LiquichainApp.class).by("name", APP_NAME).getResult();
        } catch (Exception e) {
            LOG.error(UNKNOWN_APPLICATION_ERROR, e);
            return EthApiUtils.createErrorResponse(requestId, INVALID_REQUEST, UNKNOWN_APPLICATION_ERROR);
        }
        wallet.setUuid(walletHash);
        wallet.setName(name);
        wallet.setAccountHash(accountHash);
        wallet.setPublicInfo(publicInfo);
        wallet.setBalance("0");
        wallet.setApplication(app);
        try {
            String savedHash = crossStorageApi.createOrUpdate(defaultRepo, wallet);
            return EthApiUtils.createResponse(requestId, "0x" + savedHash);
        } catch (Exception e) {
            LOG.error(CREATE_WALLET_ERROR, e);
            return EthApiUtils.createErrorResponse(requestId, INTERNAL_ERROR, CREATE_WALLET_ERROR);
        }
    }

    public String updateWallet(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        String name = params.get(0);
        String walletHash = EthApiUtils.retrieveHash(params, 1);
        String publicInfo = params.get(2);

        Wallet wallet = null;
        try {
            wallet = crossStorageApi.find(defaultRepo, walletHash.toLowerCase(), Wallet.class);
        } catch (EntityDoesNotExistsException e) {
            LOG.error(UNKNOWN_WALLET_ERROR, e);
            wallet = null;
        }
        if (wallet == null) {
            return EthApiUtils.createErrorResponse(requestId, INVALID_REQUEST, UNKNOWN_WALLET_ERROR);
        }
        wallet.setName(name);
        wallet.setPublicInfo(publicInfo);
        try {
            crossStorageApi.createOrUpdate(defaultRepo, wallet);
            return EthApiUtils.createResponse(requestId, name);
        } catch (Exception e) {
            LOG.error(UPDATE_WALLET_ERROR, e);
            return EthApiUtils.createErrorResponse(requestId, INTERNAL_ERROR, UPDATE_WALLET_ERROR);
        }
    }

    public String getWalletInfo(String requestId, Map<String, Object> parameters) {
        List<String> params = (List<String>) parameters.get("params");
        String walletHash = EthApiUtils.retrieveHash(params, 0);
        Wallet wallet = null;
        try {
            wallet = crossStorageApi.find(defaultRepo, walletHash.toLowerCase(), Wallet.class);
        } catch (Exception e) {
            LOG.error(UNKNOWN_WALLET_ERROR, e);
            wallet = null;
        }
        if (wallet == null) {
            return EthApiUtils.createErrorResponse(requestId, RESOURCE_NOT_FOUND, UNKNOWN_WALLET_ERROR);
        }
        String response = "{\n";
        response += "\"name\":\"" + wallet.getName() + "\"";
        if (wallet.getPublicInfo() != null) {
            response += ",\n\"publicInfo\":" + wallet.getPublicInfo() + "";
        }
        response += "\n}";
        return EthApiUtils.createResponse(requestId, response);
    }
}
