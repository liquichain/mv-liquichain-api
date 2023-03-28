package io.liquichain.api.verification;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.OutboundSMS;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateOutboundSMS extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(CreateOutboundSMS.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();

    private String to;
    private String otp;
    private String result;

    public String getResult() {
        return result;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        OutboundSMS sms = new OutboundSMS();
        sms.setTo(to);
        sms.setOtpCode(otp);
        sms.setPurpose("OTP");
        sms.setCreationDate(Instant.now());
        sms.setMessage("Your telecelplay verification code is: " + otp);
        sms.setResponse("message sent");

        try {
            crossStorageApi.createOrUpdate(defaultRepo, sms);
            result = "success";
        } catch (IOException e) {
            throw new BusinessException("Failed to create outbound sms.", e);
        }
    }
}
