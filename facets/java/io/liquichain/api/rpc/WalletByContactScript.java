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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletByContactScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(WalletByContactScript.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();

    private String result;
    private List<String> contactHashes;

    public String getResult() {
        return this.result;
    }

    public void setContactHashes(List<String> contactHashes) {
        this.contactHashes = contactHashes;
    }

    public static String toJson(Object data) {
        String json = null;
        try {
            json = mapper.writeValueAsString(data);
        } catch (Exception e) {
            LOG.error("Failed to convert to json: {}", data, e);
        }
        return json;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        LOG.info("contactHashes: {}", this.contactHashes);
        if (contactHashes != null && contactHashes.size() > 0) {
            Map<String, String> walletHashes = crossStorageApi
                .find(defaultRepo, Wallet.class)
                .by("inList phoneNumber", this.contactHashes)
                .getResults()
                .stream()
                .collect(Collectors.toMap(wallet -> wallet.getPhoneNumber().getUuid(), Wallet::getUuid));

            result = toJson(walletHashes);
        }
    }
}
