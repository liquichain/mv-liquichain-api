package io.liquichain.api.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.customEntities.OutboundSMS;
import org.meveo.model.customEntities.VerifiedPhoneNumber;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerifyOtpForPrivateInfo extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(VerifyOtpForPasswordReset.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final ParamBean config = paramBeanFactory.getInstance();

    private final String otpMaxAttempts = config.getProperty("otp.max.attempts", "5");
    private final String otpMaxDelay = config.getProperty("otp.max.delay", "3");

    private final int MAX_ATTEMPTS = Integer.parseInt(otpMaxAttempts, 10);
    private final Duration MAX_DELAY = Duration.ofMinutes(Long.parseLong(otpMaxDelay));

    private String otp;
    private String phoneNumber;
    private String result;

    public String getResult() {
        return this.result;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    private String buildError(String errorMessage) {
        return "{\"status\": \"failed\", \"result\": \"" + errorMessage + "\"}";
    }

    private String checkParameters() {
        if (StringUtils.isBlank(phoneNumber)) {
            return "recipient_required";
        }
        if (StringUtils.isBlank(otp)) {
            return "otp_required";
        }
        return "valid_parameters";
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);

        String validationResult = checkParameters();
        if (!"valid_parameters".equals(validationResult)) {
            result = buildError(validationResult);
            return;
        }

        LOG.debug("VerifyOtpForPrivateInfo - verify otp:{} phoneNumber:{}", otp, phoneNumber);

        OutboundSMS latestSMS = crossStorageApi.find(defaultRepo, OutboundSMS.class)
                                               .by("to", phoneNumber)
                                               .by("purpose", "OTP")
                                               // order by descending creationDate
                                               .orderBy("creationDate", false)
                                               .getResult();
        if (latestSMS != null) {
            boolean isVerified = latestSMS.getVerificationDate() != null;
            boolean isFailed = latestSMS.getFailureDate() != null;

            Instant creationDate = latestSMS.getCreationDate();
            Duration delay = Duration.between(creationDate, Instant.now());
            boolean isExpired = delay.compareTo(MAX_DELAY) > 0;

            Long verificatonAttempts = latestSMS.getVerificationAttempts();
            Long attempts = verificatonAttempts == null ? 0L : verificatonAttempts;
            boolean isTooManyAttempts = attempts >= MAX_ATTEMPTS;

            LOG.debug("creationDate: {}", creationDate);
            LOG.debug("isExpired: {}", isExpired);
            LOG.debug("isVerified: {}", isVerified);
            LOG.debug("isFailed: {}", isFailed);

            if (isVerified) {
                result = buildError("already_verified");
            } else if (isFailed) {
                result = buildError("otp_no_longer_valid");
            } else if (isTooManyAttempts) {
                latestSMS.setFailureDate(Instant.now());
                result = buildError("too_many_attempts");
            } else if (isExpired) {
                latestSMS.setFailureDate(Instant.now());
                result = buildError("otp_expired");
            } else if (otp != null && otp.equals(latestSMS.getOtpCode())) {
                latestSMS.setVerificationDate(Instant.now());
                try {
                    VerifiedPhoneNumber verifiedPhoneNumber =
                        crossStorageApi.find(defaultRepo, VerifiedPhoneNumber.class)
                                       .by("phoneNumber", phoneNumber)
                                       .getResult();
                    Wallet wallet = crossStorageApi.find(defaultRepo, Wallet.class)
                                                   .by("phoneNumber", verifiedPhoneNumber)
                                                   .getResult();
                    result = "{\"status\": \"success\", \"result\": " + wallet.getPrivateInfo() + "}";
                } catch (Exception e) {
                    result = buildError(e.getMessage());
                }
            } else {
                latestSMS.setVerificationAttempts(++attempts);
                result = buildError("otp_incorrect");
            }
            LOG.debug("result:{}", result);
            if (!isVerified || !isFailed) {
                try {
                    crossStorageApi.createOrUpdate(defaultRepo, latestSMS);
                } catch (Exception e) {
                    LOG.error("Failed to save outbound sms: {}", e.getMessage());
                    result = buildError(e.getMessage());
                }
            }

        } else {
            result = buildError("otp_does_not_exist");
        }
    }
}
