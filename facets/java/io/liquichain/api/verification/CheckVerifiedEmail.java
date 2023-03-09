package io.liquichain.api.verification;

import java.util.Map;

import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.VerifiedEmail;
import org.meveo.model.customEntities.VerifiedPhoneNumber;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckVerifiedEmail extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(CheckVerifiedPhoneNumber.class);
    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();

    private String email;
    private String result;

    public String getResult() {
        return result;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);

        if (email == null) {
            result = "email_required";
            return;
        }

        VerifiedEmail verifiedEmail = crossStorageApi.find(defaultRepo, VerifiedEmail.class)
                                                           .by("email", email)
                                                           .getResult();

        LOG.debug("verifiedEmail: {}", verifiedEmail);

        if (verifiedEmail == null) {
            result= "email_does_not_exist";
            return;
        }

        if(verifiedEmail.getVerified() == null || !verifiedEmail.getVerified()) {
            result = "email_exists_but_not_verified";
        }
    }
}
