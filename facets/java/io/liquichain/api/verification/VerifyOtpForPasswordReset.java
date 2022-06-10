package io.liquichain.api.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.liquichain.api.service.KeycloakUserService;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.User;
import org.meveo.model.customEntities.OutboundSMS;
import org.meveo.model.customEntities.VerifiedEmail;
import org.meveo.model.customEntities.VerifiedPhoneNumber;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.security.DefaultRole;
import org.meveo.model.security.Role;
import org.meveo.model.shared.Name;
import org.meveo.model.storage.Repository;
import org.meveo.service.admin.impl.RoleService;
import org.meveo.service.admin.impl.UserService;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class VerifyOtpForPasswordReset extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(VerifyOtpForPasswordReset.class);
    private static final Gson gson = new Gson();
    private static final int CONNECTION_POOL_SIZE = 50;
    private static final int MAX_POOLED_PER_ROUTE = 5;
    private static final long CONNECTION_TTL = 5;
    private static final Client client = new ResteasyClientBuilder().connectionPoolSize(CONNECTION_POOL_SIZE)
                                                                    .maxPooledPerRoute(MAX_POOLED_PER_ROUTE)
                                                                    .connectionTTL(CONNECTION_TTL, TimeUnit.SECONDS)
                                                                    .build();

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final UserService userService = getCDIBean(UserService.class);
    private final RoleService roleService = getCDIBean(RoleService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final ParamBean config = paramBeanFactory.getInstance();
    private final KeycloakUserService keycloakUserService =
        new KeycloakUserService(config, crossStorageApi, defaultRepo, userService, roleService);

    private final String otpMaxAttempts = config.getProperty("otp.max.attempts", "5");
    private final String otpMaxDelay = config.getProperty("otp.max.delay", "3");

    private final int MAX_ATTEMPTS = Integer.parseInt(otpMaxAttempts, 10);
    private final Duration MAX_DELAY = Duration.ofMinutes(Long.parseLong(otpMaxDelay));
    private final String AUTH_URL = System.getProperty("meveo.keycloak.url");
    private final String REALM = System.getProperty("meveo.keycloak.realm");
    private final String CLIENT_ID = config.getProperty("keycloak.client.id", "admin-cli");
    private final String CLIENT_SECRET = config
        .getProperty("keycloak.client.secret", "1d1e1d9f-2d98-4f43-ac69-c8ecc1f188a5");
    private final String LOGIN_URL = AUTH_URL + "/realms/master/protocol/openid-connect/token";
    private final String CLIENT_REALM_URL = AUTH_URL + "/admin/realms/" + REALM;
    private final String USERS_URL = CLIENT_REALM_URL + "/users";

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

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);

        String validationResult = checkParameters();
        if (!"valid_parameters".equals(validationResult)) {
            result = buildError(validationResult);
            return;
        }

        LOG.info("VerifyOtpForPasswordReset - verify otp:{} to:{}", otp, to);

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

            LOG.info("creationDate: {}", creationDate);
            LOG.info("isExpired: {}", isExpired);
            LOG.info("isVerified: {}", isVerified);
            LOG.info("isFailed: {}", isFailed);

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
            LOG.info("result:{}", result);
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

    private <T> T convertToMap(String data) {
        return gson.fromJson(data, new TypeToken<T>() {
        }.getType());
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
}
