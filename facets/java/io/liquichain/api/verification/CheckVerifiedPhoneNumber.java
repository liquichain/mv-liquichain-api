package io.liquichain.api.verification;

import java.util.Map;

import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.VerifiedPhoneNumber;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckVerifiedPhoneNumber extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(CheckVerifiedPhoneNumber.class);
    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();

    private String phoneNumber;
    private String result;

    public String getResult() {
        return result;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);

        if (phoneNumber == null) {
            result = "phone_number_required";
            return;
        }

        VerifiedPhoneNumber verifiedPhoneNumber = crossStorageApi.find(defaultRepo, VerifiedPhoneNumber.class)
                                                                 .by("phoneNumber", phoneNumber)
                                                                 .getResult();

        LOG.info("verifiedPhoneNumber: {}", verifiedPhoneNumber);

        if (verifiedPhoneNumber == null) {
            result= "phone_number_does_not_exist";
            return;
        }

        if(verifiedPhoneNumber.getVerified() == null || !verifiedPhoneNumber.getVerified()) {
            result = "phone_number_exists_but_not_verified";
        }
    }
}
