package io.liquichain.api.rpc;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();

    private Map<String, String> result = null;
    private List<String> contactHashes;

    public Map<String, String> getResult() {
        return result;
    }

    public void setContactHashes(List<String> contactHashes) {
        this.contactHashes = contactHashes;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        LOG.debug("contactHashes: {}", this.contactHashes);

        if (contactHashes != null && contactHashes.size() > 0) {
            result = crossStorageApi
                .find(defaultRepo, Wallet.class)
                .by("inList phoneNumber", this.contactHashes)
                .getResults()
                .stream()
                .collect(Collectors.toMap(wallet -> wallet.getPhoneNumber().getUuid(), Wallet::getUuid));
        }
    }

}
