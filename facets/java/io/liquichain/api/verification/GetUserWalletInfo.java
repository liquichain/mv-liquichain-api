package io.liquichain.api.verification;

import java.util.HashMap;
import java.util.Map;

import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.security.MeveoUser;
import org.meveo.service.crm.impl.CurrentUserProducer;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetUserWalletInfo extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(GetUserWalletInfo.class);
    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final CurrentUserProducer userProducer = getCDIBean(CurrentUserProducer.class);

    private Map<String, Object> result = new HashMap<>();

    public Map<String, Object> getResult() {
        return result;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
        MeveoUser user = userProducer.getCurrentUser();
        try {
            if (user == null) {
                throw new RuntimeException("Failed to retrieve current user.");
            }
            String username = user.getUserName();
            if (StringUtils.isBlank(username)) {
                throw new RuntimeException("Username is empty");
            }
            Wallet wallet = crossStorageApi.find(defaultRepo, Wallet.class)
                                           .by("likeCriterias privateInfo", "*" + username + "*")
                                           .getResult();

            if (wallet == null) {
                throw new RuntimeException("Failed to retrieve wallet for username: " + username);
            }
            String privateInfo = wallet.getPrivateInfo();
            if (StringUtils.isBlank(privateInfo)) {
                throw new RuntimeException("Private info is empty");
            }
            result.put("status", "success");
            result.put("result", privateInfo);
        } catch (Exception e) {
            mapError(e);
        }

    }

    private void mapError(Throwable e) {
        LOG.error(e.getMessage(), e);
        result.put("status", "fail");
        result.put("result", e.getMessage());
    }

}
