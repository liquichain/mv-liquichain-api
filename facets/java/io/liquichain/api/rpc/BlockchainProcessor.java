package io.liquichain.api.rpc;

import static io.liquichain.api.rpc.EthApiConstants.*;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.VerifiedEmail;
import org.meveo.model.customEntities.VerifiedPhoneNumber;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.web3j.crypto.*;
import org.web3j.utils.*;

public class BlockchainProcessor extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(BlockchainProcessor.class);
    private static final Map<String, Object[]> TRANSACTION_HOOKS = new HashMap<>();

    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);

    protected final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    protected final Repository defaultRepo = repositoryService.findDefaultRepository();
    protected ParamBean config = paramBeanFactory.getInstance();
    
    protected String result;

    public String getResult() {
        return this.result;
    }

    public static boolean addTransactionHook(String regex, Script script) {
        boolean isHookAdded = true;
        String key = regex + ":" + script.getClass().getName();
        LOG.info("addTransactionHook key: {}", key);
        isHookAdded = !TRANSACTION_HOOKS.containsKey(key);
        if (isHookAdded) {
            Pattern pattern = Pattern.compile(regex);
            TRANSACTION_HOOKS.put(key, new Object[]{pattern, script});
        }
        return isHookAdded;
    }

    protected void processTransactionHooks(String transactionHash, SignedRawTransaction transaction) {
        try {
            String data = new String(new BigInteger(transaction.getData(), 16).toByteArray());
            LOG.info("try matching {} hooks", TRANSACTION_HOOKS.size());
            TRANSACTION_HOOKS.forEach((String key, Object[] hook) -> {
                LOG.info("try hook {} on {}", key, data);
                Pattern pattern = (Pattern) hook[0];
                Script script = (Script) hook[1];
                Matcher matcher = pattern.matcher(data);
                if (matcher.find()) {
                    LOG.info(" hook {} matched", key);
                    Map<String, Object> context = new HashMap<>();
                    context.put("transaction", transaction);
                    context.put("transactionHash", transactionHash);
                    context.put("matcher", matcher);
                    try {
                        script.execute(context);
                        if (context.containsKey("result")) {
                            LOG.info(" hook result:{} ", context.get("result"));
                        }
                    } catch (Exception e) {
                        LOG.error("error while invoking transaction hook {}", script, e);
                    }
                } else {
                    LOG.info(" hook {} matched", key);
                }
            });
            if (data.contains("orderId")) {
                LOG.info("detected orderId:{}", data);
            }
        } catch (Exception ex) {
            LOG.info("error while detecting order:{}", ex);
        }
    }

    protected String createResponse(String requestId, String response) {
        return EthApiUtils.createResponse(requestId, response);
    }

    protected String createErrorResponse(String requestId, String errorCode, String message) {
        return EthApiUtils.createErrorResponse(requestId, errorCode, message);
    }

    protected String normalizeHash(String hash) {
        return EthApiUtils.normalizeHash(hash);
    }

    protected String retrieveHash(List<String> parameters, int parameterIndex) {
        return normalizeHash(parameters.get(parameterIndex));
    }

    protected boolean isJSONValid(String json) {
        return EthApiUtils.isJSONValid(json);
    }

    protected String toHex(byte[] bytes) {
        return EthApiUtils.toHex(bytes);
    }

    protected String toBigHex(String value) {
        return EthApiUtils.toBigHex(value);
    }

    protected String validateName(String name) throws BusinessException {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(NAME_REQUIRED_ERROR);
        }
        Wallet walletWithSameName = null;
        try {
            walletWithSameName = crossStorageApi
                    .find(defaultRepo, Wallet.class)
                    .by("name", name)
                    .getResult();
        } catch (Exception e) {
            // do nothing, we want wallet name to be unique
        }
        if (walletWithSameName != null) {
            String error = String.format(NAME_EXISTS_ERROR, name);
            LOG.error(error);
            throw new BusinessException(error);
        }
        return name;
    }

    protected String validateEmail(String email, String walletId) throws BusinessException {
        // if (email == null || email.trim().isEmpty()) {
        // throw new BusinessException(EMAIL_REQUIRED_ERROR);
        // }
        if (email != null && !email.trim().isEmpty()) {
            VerifiedEmail existingEmail = null;
            try {
                existingEmail = crossStorageApi
                        .find(defaultRepo, VerifiedEmail.class)
                        .by("email", email)
                        .by("not-inList walletId", Arrays.asList(walletId))
                        .getResult();
            } catch (Exception e) {
                // do nothing, we want email address to be unique
            }
            if (existingEmail != null) {
                String error = String.format(EMAIL_EXISTS_ERROR, email);
                LOG.error(error);
                throw new BusinessException(error);
            }
        }
        return email;
    }

    protected String validatePhoneNumber(String phoneNumber, String walletId)
            throws BusinessException {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new BusinessException(PHONE_NUMBER_REQUIRED_ERROR);
        }
        VerifiedPhoneNumber existingPhoneNumber = null;
        try {
            existingPhoneNumber = crossStorageApi
                    .find(defaultRepo, VerifiedPhoneNumber.class)
                    .by("phoneNumber", phoneNumber)
                    .by("not-inList walletId", Arrays.asList(walletId))
                    .getResult();
        } catch (Exception e) {
            // do nothing, we want wallet phoneNumber to be unique
        }
        if (existingPhoneNumber != null) {
            String error = String.format(PHONE_NUMBER_EXISTS_ERROR, phoneNumber);
            LOG.error(error);
            throw new BusinessException(error);
        }
        return phoneNumber;
    }

    protected String parseAddress(String signature, String message) throws Exception {
        byte[] messageHash = Hash.sha3(message.getBytes(StandardCharsets.UTF_8));
        LOG.info("messageHash={}", Numeric.toHexString(messageHash));
        String r = signature.substring(0, 66);
        String s = "0x" + signature.substring(66, 130);
        String v = "0x" + signature.substring(130, 132);
        String publicKey = Sign
                .signedMessageHashToKey(
                        messageHash,
                        new Sign.SignatureData(
                                Numeric.hexStringToByteArray(v)[0],
                                Numeric.hexStringToByteArray(r),
                                Numeric.hexStringToByteArray(s)
                        )
                )
                .toString(16);
        String address = Keys.getAddress(publicKey);
        LOG.info("address: " + address);
        return address;
    }

    protected <T> T findEntity(String uuid, Class<T> clazz) {
        T entity = null;
        try {
            entity = crossStorageApi.find(defaultRepo, uuid, clazz);
        } catch (EntityDoesNotExistsException e) {
            LOG.warn("No {} with uuid: {}", clazz.getSimpleName(), uuid);
        }
        return entity;
    }
}
