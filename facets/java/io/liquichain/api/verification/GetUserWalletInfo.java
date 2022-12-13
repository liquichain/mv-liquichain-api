package io.liquichain.api.verification;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.customEntities.VerifiedEmail;
import org.meveo.model.customEntities.VerifiedPhoneNumber;
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

            Map<String, Object> response = new HashMap<>();
            response.put("address", wallet.getUuid());
            response.put("name", wallet.getName());
            response.put("publicInfo", convert(wallet.getPublicInfo()));

            Map<String, Object> privateInfo = new HashMap<>();
            VerifiedEmail verifiedEmail = wallet.getEmailAddress();
            LOG.info("verifiedEmail={}", verifiedEmail);
            if (verifiedEmail != null) {
                String emailId = verifiedEmail.getUuid();
                LOG.info("wallet_info emailId={}", emailId);
                String emailAddress = verifiedEmail.getEmail();
                boolean hasEmailAddress = StringUtils.isNotBlank(emailAddress);
                if (StringUtils.isNotBlank(emailId) && !hasEmailAddress) {
                    try {
                        verifiedEmail =
                            crossStorageApi.find(defaultRepo, emailId, VerifiedEmail.class);
                        if (verifiedEmail != null) {
                            emailAddress = verifiedEmail.getEmail();
                        }
                    } catch (Exception e) {
                        LOG.error("Error retrieving email with uuid: " + emailId, e);
                        emailAddress = null;
                    }
                }
                LOG.info("wallet_info emailAddress={}", emailAddress);
                if (verifiedEmail != null && StringUtils.isNotBlank(emailAddress)) {
                    Map<String, Object> email = new HashMap<>();
                    email.put("emailAddress", emailAddress);
                    email.put("verified", verifiedEmail.getVerified());
                    email.put("hash", verifiedEmail.getUuid());
                    privateInfo.put("email", email);
                }
            }

            VerifiedPhoneNumber verifiedPhoneNumber = wallet.getPhoneNumber();
            LOG.info("wallet_info verifiedPhoneNumber={}", verifiedPhoneNumber);
            if (verifiedPhoneNumber != null) {
                String phoneId = verifiedPhoneNumber.getUuid();
                LOG.info("wallet_info phoneId={}", phoneId);
                String phoneNumber = verifiedPhoneNumber.getPhoneNumber();
                boolean hasPhoneNumber = StringUtils.isNotBlank(phoneNumber);
                if (StringUtils.isNotBlank(phoneId) && !hasPhoneNumber) {
                    try {
                        verifiedPhoneNumber = crossStorageApi.find(defaultRepo, phoneId, VerifiedPhoneNumber.class);
                        if (verifiedPhoneNumber != null) {
                            phoneNumber = verifiedPhoneNumber.getPhoneNumber();
                        }
                    } catch (Exception e) {
                        LOG.error("wallet_info Error retrieving phone number with uuid: " + phoneId, e);
                        phoneNumber = null;
                    }
                }
                LOG.info("wallet_info phoneNumber={}", phoneNumber);
                if (verifiedPhoneNumber != null && StringUtils.isNotBlank(phoneNumber)) {
                    Map<String, Object> phone = new HashMap<>();
                    phone.put("phoneNumber", phoneNumber);
                    phone.put("verified", verifiedPhoneNumber.getVerified());
                    phone.put("hash", verifiedPhoneNumber.getUuid());
                    privateInfo.put("phoneNumber", phone);
                }
            }

            if (StringUtils.isNotBlank(wallet.getPrivateInfo())) {
                Map<String, Object> privateInfoMap = convert(wallet.getPrivateInfo());
                privateInfoMap.keySet().forEach(key -> privateInfo.put(key, privateInfoMap.get(key)));
            }

            if (StringUtils.isBlank(privateInfo)) {
                throw new RuntimeException("Private info is empty");
            }

            response.put("privateInfo", privateInfo);

            result.put("status", "success");
            result.put("result", response);
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
