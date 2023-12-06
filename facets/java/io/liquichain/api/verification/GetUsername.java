package io.liquichain.api.verification;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.User;
import org.meveo.model.customEntities.VerifiedEmail;
import org.meveo.model.customEntities.VerifiedPhoneNumber;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.service.admin.impl.UserService;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;
import org.meveo.service.storage.RepositoryService;

import io.liquichain.api.rpc.EthApiUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetUsername extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(GetUsername.class);
    private static final String EMAIL_REGEX =
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final UserService userService = getCDIBean(UserService.class);
    private final ScriptInstanceService scriptInstanceService = getCDIBean(ScriptInstanceService.class);
    private final EthApiUtils ethApiUtils = (EthApiUtils) scriptInstanceService.getExecutionEngine(
            EthApiUtils.class.getName(), null);

    private String emailOrNumber;
    private Map<String, Object> result = new HashMap<>();

    public Map<String, Object> getResult() {
        return result;
    }

    public void setEmailOrNumber(String emailOrNumber) {
        this.emailOrNumber = emailOrNumber;
    }

    public static boolean isValidEmail(String email) {
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
        try {
            if (StringUtils.isBlank(emailOrNumber)) {
                throw new RuntimeException("Email address or phone number is required");
            }

            String username;
            if (isValidEmail(emailOrNumber)) {
                VerifiedEmail verifiedEmail = crossStorageApi.find(defaultRepo, VerifiedEmail.class)
                                                             .by("email", emailOrNumber)
                                                             .getResult();
                if (verifiedEmail == null) {
                    User user = userService.findByEmail(emailOrNumber);
                    if (user == null) {
                        throw new RuntimeException("Failed to find email: " + emailOrNumber);
                    }
                    username = user.getUserName();
                } else {
                    Wallet wallet = crossStorageApi.find(defaultRepo, Wallet.class)
                                                   .by("emailAddress", verifiedEmail)
                                                   .getResult();
                    username = extractUsernameFromWallet(wallet);
                }

            } else {
                VerifiedPhoneNumber verifiedPhoneNumber = crossStorageApi.find(defaultRepo, VerifiedPhoneNumber.class)
                                                                         .by("phoneNumber", emailOrNumber)
                                                                         .getResult();
                if (verifiedPhoneNumber == null) {
                    throw new RuntimeException("Failed to find phone number: " + emailOrNumber);
                }
                Wallet wallet = crossStorageApi.find(defaultRepo, Wallet.class)
                                               .by("phoneNumber", verifiedPhoneNumber)
                                               .getResult();
                username = extractUsernameFromWallet(wallet);
            }

            result.put("status", "success");
            result.put("result", username);
        } catch (Exception e) {
            mapError(e);
        }
    }

    private String extractUsernameFromWallet(Wallet wallet) {
        if (wallet == null) {
            throw new RuntimeException("Failed to retrieve wallet for: " + emailOrNumber);
        }

        String privateInfo = wallet.getPrivateInfo();
        if (StringUtils.isBlank(privateInfo)) {
            throw new RuntimeException("Private info is empty.");
        }

        Map<String, Object> privateInfoMap = ethApiUtils.convert(privateInfo);
        String username = String.valueOf((Object) privateInfoMap.get("username"));
        if (StringUtils.isBlank(username)) {
            throw new RuntimeException("Username not found.");
        }
        return username;
    }

    private void mapError(Throwable e) {
        LOG.error(e.getMessage(), e);
        result.put("status", "fail");
        result.put("result", e.getMessage());
    }

}
