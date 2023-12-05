package io.liquichain.api.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.customEntities.OutboundSMS;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;
import org.meveo.service.storage.RepositoryService;

import io.liquichain.api.service.KeycloakUserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerifyOtpForPasswordReset extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(VerifyOtpForPasswordReset.class);

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final ParamBean config = paramBeanFactory.getInstance();
    private final ScriptInstanceService scriptInstanceService = getCDIBean(ScriptInstanceService.class);
    private final KeycloakUserService keycloakUserService =
            (KeycloakUserService) scriptInstanceService.getExecutionEngine("KeycloakUserService", null);

    private final String otpMaxAttempts = config.getProperty("otp.max.attempts", "5");
    private final String otpMaxDelay = config.getProperty("otp.max.delay", "3");

    private final int MAX_ATTEMPTS = Integer.parseInt(otpMaxAttempts, 10);
    private final Duration MAX_DELAY = Duration.ofMinutes(Long.parseLong(otpMaxDelay));

    private String to;
    private String otp;
    private String password;
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

    public void setPassword(String password) {
        this.password = password;
    }

    private String buildError(String errorMessage) {
        return "{\"status\": \"failed\", \"result\": \"" + errorMessage + "\"}";
    }

    private String checkParameters() {
        if (StringUtils.isBlank(to)) {
            return "recipient_required";
        }
        if (StringUtils.isBlank(otp)) {
            return "otp_required";
        }
        if (StringUtils.isBlank(password)) {
            return "password_required";
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

        LOG.debug("VerifyOtpForPasswordReset - verify otp:{} to:{}", otp, to);

        OutboundSMS latestSMS = crossStorageApi.find(defaultRepo, OutboundSMS.class)
                                               .by("to", to)
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

            long attempts = latestSMS.getVerificationAttempts();
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
                    keycloakUserService.updateUserPasswordByPhoneNumber(to, password);
                    result = "{\"status\": \"success\", \"result\": \"password_updated\"}";
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
