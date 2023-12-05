package io.liquichain.api.rpc;

import static io.liquichain.api.rpc.EthApiScript.EthApiConstants.*;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.VerifiedEmail;
import org.meveo.model.customEntities.VerifiedPhoneNumber;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.utils.Numeric;

public class BlockchainProcessor extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(BlockchainProcessor.class);

    private static final Map<String, Object[]> TRANSACTION_HOOKS = new HashMap<>();

    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();

    public boolean addTransactionHook(String regex, Script script) {
        boolean isHookAdded = true;
        String key = regex + ":" + script.getClass().getName();
        LOG.debug("addTransactionHook key: {}", key);
        isHookAdded = !TRANSACTION_HOOKS.containsKey(key);
        if (isHookAdded) {
            Pattern pattern = Pattern.compile(regex);
            TRANSACTION_HOOKS.put(key, new Object[] { pattern, script });
        }
        return isHookAdded;
    }

    public void processTransactionHooks(String transactionHash, SignedRawTransaction transaction) {
        try {
            String data = new String(new BigInteger(transaction.getData(), 16).toByteArray());
            LOG.debug("try matching {} hooks", TRANSACTION_HOOKS.size());
            TRANSACTION_HOOKS.forEach((String key, Object[] hook) -> {
                LOG.debug("try hook {} on {}", key, data);
                Pattern pattern = (Pattern) hook[0];
                Script script = (Script) hook[1];
                Matcher matcher = pattern.matcher(data);
                if (matcher.find()) {
                    LOG.debug(" hook {} matched", key);
                    Map<String, Object> context = new HashMap<>();
                    context.put("transaction", transaction);
                    context.put("transactionHash", transactionHash);
                    context.put("matcher", matcher);
                    try {
                        script.execute(context);
                        if (context.containsKey("result")) {
                            LOG.debug(" hook result:{} ", context.get("result"));
                        }
                    } catch (Exception e) {
                        LOG.error("error while invoking transaction hook {}", script, e);
                    }
                } else {
                    LOG.debug(" hook {} matched", key);
                }
            });
            if (data.contains("orderId")) {
                LOG.debug("Detected orderId: {}", data);
            }
        } catch (Exception e) {
            LOG.debug("Error while detecting order", e);
        }
    }

    public String validateName(String name) throws BusinessException {
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

    public String validateEmail(String email, String walletId) throws BusinessException {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(EMAIL_REQUIRED_ERROR);
        }
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
        return email;
    }

    public String validatePhoneNumber(String phoneNumber, String walletId)
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

    public String parseAddress(String signature, String message) throws Exception {
        byte[] messageHash = Hash.sha3(message.getBytes(StandardCharsets.UTF_8));
        LOG.debug("messageHash={}", Numeric.toHexString(messageHash));
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
        LOG.debug("address: " + address);
        return address;
    }

    public <T> T findEntity(String uuid, Class<T> clazz) {
        T entity = null;
        try {
            entity = crossStorageApi.find(defaultRepo, uuid, clazz);
        } catch (EntityDoesNotExistsException e) {
            LOG.warn("No {} with uuid: {}", clazz.getSimpleName(), uuid);
        }
        return entity;
    }
}