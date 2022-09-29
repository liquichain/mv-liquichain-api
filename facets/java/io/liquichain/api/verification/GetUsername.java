package io.liquichain.api.verification;

import java.util.HashMap;
import java.util.Map;

import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.customEntities.VerifiedEmail;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.storage.RepositoryService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetUsername extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(GetUsername.class);
    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();

    private String emailAddress;
    private Map<String, Object> result = new HashMap<>();

    public Map<String, Object> getResult() {
        return result;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
        try {
            if (StringUtils.isBlank(emailAddress)) {
                throw new RuntimeException("emailAddress is required");
            }
            VerifiedEmail verifiedEmail = crossStorageApi.find(defaultRepo, VerifiedEmail.class)
                                                         .by("email", emailAddress)
                                                         .getResult();
            if (verifiedEmail == null) {
                throw new RuntimeException("Failed to find email: " + emailAddress);
            }

            Wallet wallet = crossStorageApi.find(defaultRepo, Wallet.class)
                                           .by("emailAddress", verifiedEmail)
                                           .getResult();

            if (wallet == null) {
                throw new RuntimeException("Failed to retrieve wallet for email address: " + emailAddress);
            }
            String privateInfo = wallet.getPrivateInfo();
            if (StringUtils.isBlank(privateInfo)) {
                throw new RuntimeException("Private info is empty.");
            }

            Map<String, Object> privateInfoMap = convert(privateInfo);
            String username = String.valueOf((Object) privateInfoMap.get("username"));
            if (StringUtils.isBlank(username)) {
                throw new RuntimeException("Username not found.");
            }
        } catch (Exception e) {
            mapError(e);
        }
    }

    public static <T> T convert(String data) {
        T value = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            value = mapper.readValue(data, new TypeReference<T>() {
            });
        } catch (Exception e) {
            LOG.error("Failed to parse data: {}", data, e);
        }
        return value;
    }

    private void mapError(Throwable e) {
        LOG.error(e.getMessage(), e);
        result.put("status", "fail");
        result.put("result", e.getMessage());
    }

}
