package io.liquichain.api.rpc;

import java.util.Map;

import org.meveo.api.persistence.CrossStorageApi;
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
    private String[] contactHashes;

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        LOG.info("contactHashes: {}", this.contactHashes);
    }

    public String getResult() {
        return this.result;
    }

    public void setContactHashes(String[] contactHashes){
        this.contactHashes = contactHashes;
    }
}
