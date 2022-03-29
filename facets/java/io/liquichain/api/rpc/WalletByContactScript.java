package io.liquichain.api.rpc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletByContactScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(WalletByContactScript.class);

    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    protected final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    protected final Repository defaultRepo = repositoryService.findDefaultRepository();

    private String result;
    private List<String> contactHashes;

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        LOG.info("contactHashes: {}", this.contactHashes);
        if(contactHashes != null && contactHashes.size() > 0){
            List<Wallet> wallets = crossStorageApi
                    .find(defaultRepo, Wallet.class)
                    .by("inList phoneNumber", this.contactHashes)
                    .getResults();
            result = new Gson().toJson(wallets);
        }
    }

    public String getResult() {
        return this.result;
    }

    public void setContactHashes(List<String> contactHashes){
        this.contactHashes = contactHashes;
    }
}
