package io.liquichain.api.rpc;

import static io.liquichain.api.rpc.EthApiConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.codec.digest.DigestUtils;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.model.customEntities.LiquichainApp;
import org.meveo.model.customEntities.VerifiedEmail;
import org.meveo.model.customEntities.VerifiedPhoneNumber;
import org.meveo.model.customEntities.Wallet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.web3j.crypto.*;

import io.liquichain.api.rpc.BlockchainProcessor;

public class WalletProcessor extends BlockchainProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(WalletProcessor.class);
    public static final List<String> WALLET_METHODS = Arrays.asList("wallet_creation", "wallet_update", "wallet_info",
                                                                    "wallet_report");
    private String APP_NAME = config.getProperty("eth.api.appname", "licoin");

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        String method = "" + parameters.get("method");
        LOG.info("json rpc: {}, parameters:{}", method, parameters);
        String requestId = "" + parameters.get("id");
        switch (method) {
            case "wallet_creation":
                result = createWallet(requestId, parameters);
                break;
            case "wallet_update":
                result = updateWallet(requestId, parameters);
                break;
            case "wallet_info":
                result = getWalletInfo(requestId, parameters);
                break;
            case "wallet_report":
                result = createResponse(requestId, "wallet reported");
                break;
            default:
                result = createErrorResponse(requestId, METHOD_NOT_FOUND, NOT_IMPLEMENTED_ERROR);
                break;
        }
    }

    private String createWallet(String requestId, Map<String, Object> parameters) {
        List<String> params = (ArrayList<String>) parameters.get("params");
        String name = params.get(0);
        String walletHash = retrieveHash(params, 1);
        String accountHash = retrieveHash(params, 2);
        String publicInfo = params.get(3);
        String privateInfo = null;
        if (params.size() > 4) {
            privateInfo = params.get(4);
        }
        Wallet wallet = null;
        LiquichainApp app = null;

        // check existing Wallet
        try {
            wallet = crossStorageApi.find(defaultRepo, walletHash, Wallet.class);
            if (wallet != null) {
                return createErrorResponse(requestId, INVALID_REQUEST, WALLET_EXISTS_ERROR);
            }
        } catch (EntityDoesNotExistsException e) {
            // do nothing, we expect wallet to not exist
        }

        wallet = new Wallet();

        try {
            app = crossStorageApi
                    .find(defaultRepo, LiquichainApp.class)
                    .by("name", APP_NAME)
                    .getResult();
            if (app == null) {
                return createErrorResponse(requestId, INTERNAL_ERROR, UNKNOWN_APPLICATION_ERROR);
            }
        } catch (Exception e) {
            LOG.error(UNKNOWN_APPLICATION_ERROR + ": " + APP_NAME, e);
            return createErrorResponse(requestId, INTERNAL_ERROR, UNKNOWN_APPLICATION_ERROR);
        }

        String emailAddress = null;
        String phoneNumber = null;
        if (privateInfo != null) {
            Map<String, String> privateInfoMap = new Gson()
                    .fromJson(privateInfo, new TypeToken<Map<String, String>>() {}.getType());
            emailAddress = privateInfoMap.get("emailAddress");
            phoneNumber = privateInfoMap.get("phoneNumber");
        }


        try {
            // name = validateName(name);
            // emailAddress = validateEmail(emailAddress, walletHash);
            phoneNumber = validatePhoneNumber(phoneNumber, walletHash);
        } catch (BusinessException e) {
            LOG.error(INVALID_REQUEST, e);
            return createErrorResponse(requestId, INVALID_REQUEST, e.getMessage());
        }

        try {
            LOG.info(
                    "Creating wallet with name={}, walletHash={}, accountHash={}, email={}, phoneNumber={}",
                    name, walletHash, accountHash, emailAddress, phoneNumber);
            wallet.setUuid(walletHash);
            wallet.setName(name);
            wallet.setAccountHash(accountHash);
            wallet.setPublicInfo(publicInfo);
            wallet.setBalance("0");
            wallet.setApplication(app);
            wallet.setVerified(false);

            if (emailAddress != null) {
                VerifiedEmail verifiedEmail = new VerifiedEmail();
                verifiedEmail.setUuid(DigestUtils.sha1Hex(emailAddress));
                verifiedEmail.setEmail(emailAddress);
                verifiedEmail.setWalletId(walletHash);
                verifiedEmail.setVerified(false);
                crossStorageApi.createOrUpdate(defaultRepo, verifiedEmail);
                wallet.setEmailAddress(verifiedEmail);
            }

            if (phoneNumber != null) {
                VerifiedPhoneNumber verifiedPhoneNumber = new VerifiedPhoneNumber();
                verifiedPhoneNumber.setUuid(DigestUtils.sha1Hex(phoneNumber));
                verifiedPhoneNumber.setPhoneNumber(phoneNumber);
                verifiedPhoneNumber.setWalletId(walletHash);
                verifiedPhoneNumber.setVerified(false);
                crossStorageApi.createOrUpdate(defaultRepo, verifiedPhoneNumber);
                wallet.setPhoneNumber(verifiedPhoneNumber);
            }

            String newHash = crossStorageApi.createOrUpdate(defaultRepo, wallet);
            if (newHash != walletHash) {
                LOG.info("Wallet hash changed from {} to {}", walletHash, newHash);
                wallet.setUuid(walletHash);
                LOG.info("Attempt to update wallet with hash: {}", walletHash);
                newHash = crossStorageApi.createOrUpdate(defaultRepo, wallet);
                LOG.info("Updated wallet hash: {}", newHash);
            }
            return createResponse(requestId, walletHash);
        } catch (Exception e) {
            LOG.error(CREATE_WALLET_ERROR, e);
            return createErrorResponse(requestId, TRANSACTION_REJECTED, CREATE_WALLET_ERROR);
        }
    }

    private String updateWallet(String requestId, Map<String, Object> parameters) {
        LOG.info("PARAMETERS: {}", parameters);
        List<String> params = (ArrayList<String>) parameters.get("params");
        String name = params.get(0);
        String walletHash = retrieveHash(params, 1);
        String signature = params.get(2);
        String publicInfo = params.get(3);
        String privateInfo = null;
        if (params.size() > 4) {
            privateInfo = params.get(4);
        }

        String validatedAddress = "";

        try {
            validatedAddress = parseAddress(signature, new Gson().toJson(publicInfo));
        } catch (Exception e) {
            LOG.error(INVALID_REQUEST, e);
            return createErrorResponse(requestId, INVALID_REQUEST, e.getMessage());
        }
        boolean sameAddress = walletHash.toLowerCase().equals(validatedAddress);
        LOG.info("validated address: {}, walletHash: {}, same address: {}", validatedAddress,
                 walletHash.toLowerCase(), sameAddress);

        if(!sameAddress) {
            return createErrorResponse(requestId, INVALID_REQUEST, INVALID_SIGNATURE_ERROR);
        }

        Wallet wallet = null;

        try {
            wallet = crossStorageApi.find(defaultRepo, walletHash, Wallet.class);
            if (wallet == null) {
                return createErrorResponse(requestId, INVALID_REQUEST, UNKNOWN_WALLET_ERROR);
            }
        } catch (EntityDoesNotExistsException e) {
            LOG.error(UNKNOWN_WALLET_ERROR, e);
            return createErrorResponse(requestId, INVALID_REQUEST, UNKNOWN_WALLET_ERROR);
        }

        String emailAddress = null;
        String phoneNumber = null;
        if (privateInfo != null) {
            Map<String, String> privateInfoMap = new Gson()
                    .fromJson(privateInfo, new TypeToken<Map<String, String>>() {}.getType());
            emailAddress = privateInfoMap.get("emailAddress");
            phoneNumber = privateInfoMap.get("phoneNumber");
        }

        LOG.info("received email: {}", emailAddress);
        LOG.info("received phoneNumber: {}", phoneNumber);

        VerifiedEmail verifiedEmail = wallet.getEmailAddress();
        VerifiedPhoneNumber verifiedPhoneNumber = wallet.getPhoneNumber();
        String existingEmail;
        String existingPhoneNumber;
        try {
            // if (!wallet.getName().equals(name)) {
            // name = validateName(name);
            // }

            if (verifiedEmail != null) {
                LOG.info("Verified email: {}", verifiedEmail.getUuid());
                VerifiedEmail oldEmail = findEntity(verifiedEmail.getUuid(), VerifiedEmail.class);
                if (oldEmail != null) {
                    existingEmail = oldEmail.getEmail();
                } else {
                    existingEmail = null;
                }
                LOG.info("existing email: {}", existingEmail);
                if (emailAddress != null && existingEmail != null && !existingEmail.equals(emailAddress)) {
                    // emailAddress = validateEmail(emailAddress, walletHash);
                    verifiedEmail = new VerifiedEmail();
                    verifiedEmail.setUuid(DigestUtils.sha1Hex(emailAddress));
                    verifiedEmail.setEmail(emailAddress);
                    verifiedEmail.setWalletId(wallet.getUuid());
                    verifiedEmail.setVerified(false);
                    crossStorageApi.createOrUpdate(defaultRepo, verifiedEmail);
                    LOG.info("old email: {}, saved email: {}", existingEmail, emailAddress);
                }
            } else if (emailAddress != null) {
                // emailAddress = validateEmail(emailAddress, walletHash);
                verifiedEmail = new VerifiedEmail();
                verifiedEmail.setUuid(DigestUtils.sha1Hex(emailAddress));
                verifiedEmail.setEmail(emailAddress);
                verifiedEmail.setWalletId(wallet.getUuid());
                verifiedEmail.setVerified(false);
                crossStorageApi.createOrUpdate(defaultRepo, verifiedEmail);
                LOG.info("No old email, saved email: {}", emailAddress);
            }

            if (verifiedPhoneNumber != null) {
                LOG.info("Verified phoneNumber: {}", verifiedPhoneNumber.getUuid());
                VerifiedPhoneNumber oldPhoneNumber =
                        findEntity(verifiedPhoneNumber.getUuid(), VerifiedPhoneNumber.class);
                if (oldPhoneNumber != null) {
                    existingPhoneNumber = oldPhoneNumber.getPhoneNumber();
                } else {
                    existingPhoneNumber = null;
                }
                LOG.info("existing phoneNumber: {}", existingPhoneNumber);
                if (existingPhoneNumber != null && !existingPhoneNumber.equals(phoneNumber)) {
                    phoneNumber = validatePhoneNumber(phoneNumber, walletHash);
                    verifiedPhoneNumber = new VerifiedPhoneNumber();
                    verifiedPhoneNumber.setUuid(DigestUtils.sha1Hex(phoneNumber));
                    verifiedPhoneNumber.setPhoneNumber(phoneNumber);
                    verifiedPhoneNumber.setWalletId(wallet.getUuid());
                    verifiedPhoneNumber.setVerified(false);
                    crossStorageApi.createOrUpdate(defaultRepo, verifiedPhoneNumber);
                }
            } else if (phoneNumber != null) {
                phoneNumber = validatePhoneNumber(phoneNumber, walletHash);
                verifiedPhoneNumber = new VerifiedPhoneNumber();
                verifiedPhoneNumber.setUuid(DigestUtils.sha1Hex(phoneNumber));
                verifiedPhoneNumber.setPhoneNumber(phoneNumber);
                verifiedPhoneNumber.setWalletId(wallet.getUuid());
                verifiedPhoneNumber.setVerified(false);
                crossStorageApi.createOrUpdate(defaultRepo, verifiedPhoneNumber);
            }
        } catch (Exception e) {
            LOG.error(INVALID_REQUEST, e);
            return createErrorResponse(requestId, INVALID_REQUEST, e.getMessage());
        }

        try {
            wallet.setName(name);
            wallet.setPublicInfo(publicInfo);
            if (verifiedEmail != null) {
                wallet.setEmailAddress(verifiedEmail);
            }
            if (verifiedPhoneNumber != null) {
                wallet.setPhoneNumber(verifiedPhoneNumber);
            }

            crossStorageApi.createOrUpdate(defaultRepo, wallet);
            return createResponse(requestId, name);
        } catch (Exception e) {
            LOG.error(UPDATE_WALLET_ERROR, e);
            return createErrorResponse(requestId, TRANSACTION_REJECTED, UPDATE_WALLET_ERROR);
        }
    }

    private String getWalletInfo(String requestId, Map<String, Object> parameters) {
        List<String> params = (ArrayList<String>) parameters.get("params");
        String walletHash = retrieveHash(params, 0);
        String signature = "";
        String message = "";

        if (params.size() > 1) {
            signature = params.get(1);
        }
        if (params.size() > 2) {
            message = params.get(2);
        }

        Wallet wallet = null;

        try {
            wallet = crossStorageApi.find(defaultRepo, walletHash, Wallet.class);
            if (wallet == null) {
                return createErrorResponse(requestId, INVALID_REQUEST, UNKNOWN_WALLET_ERROR);
            }
        } catch (EntityDoesNotExistsException e) {
            LOG.error(UNKNOWN_WALLET_ERROR, e);
            return createErrorResponse(requestId, INVALID_REQUEST, UNKNOWN_WALLET_ERROR);
        }

        boolean shouldValidate = signature != null && !signature.isEmpty()
                && message != null && !message.isEmpty();
        boolean isValidSignature = false;
        Long requestTime = 0L;
        if (shouldValidate) {
            String validatedAddress = "";
            try {
                validatedAddress = parseAddress(signature, message);
            } catch (Exception e) {
                LOG.error(INVALID_REQUEST, e);
                return createErrorResponse(requestId, INVALID_REQUEST, e.getMessage());
            }
            boolean sameAddress = walletHash.toLowerCase().equals(validatedAddress);
            LOG.info("validated address: {}, walletHash: {}, same address: {}", validatedAddress,
                     walletHash.toLowerCase(), sameAddress);
            Long lastRequest = wallet.getLastPrivateInfoRequest();
            if (lastRequest == null) {
                lastRequest = 0L;
            }
            requestTime = Long.parseLong(message.split(",")[2]);
            LOG.info("lastRequest={}", lastRequest);
            LOG.info("requestTime={}", requestTime);

            isValidSignature = sameAddress && requestTime > lastRequest;
            LOG.info("isValidSignature={}", isValidSignature);
        }

        StringBuilder response = new StringBuilder()
                .append("{")
                .append(String.format("\"name\":\"%s\",", wallet.getName()))
                .append(String.format("\"publicInfo\":%s",
                                      new Gson().toJson(wallet.getPublicInfo())));
        if (isValidSignature) {
            StringBuilder privateInfo = new StringBuilder("{");
            VerifiedEmail verifiedEmail = wallet.getEmailAddress();
            LOG.info("verifiedEmail={}", verifiedEmail);
            if (verifiedEmail != null) {
                String emailId = verifiedEmail.getUuid();
                LOG.info("emailId={}", emailId);
                String emailAddress = verifiedEmail.getEmail();
                boolean hasEmailAddress = emailAddress != null && !emailAddress.isEmpty();
                if (emailId != null && !hasEmailAddress) {
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
                LOG.info("emailAddress={}", emailAddress);
                if (emailAddress != null && !emailAddress.trim().isEmpty()) {
                    privateInfo
                            .append(String.format(
                                    "\"emailAddress\": {\"value\": \"%s\", \"verified\": \"%s\", \"hash\": \"%s\"}",
                                    emailAddress, verifiedEmail.getVerified(), verifiedEmail.getUuid()));
                }
            }

            VerifiedPhoneNumber verifiedPhoneNumber = wallet.getPhoneNumber();
            LOG.info("verifiedPhoneNumber={}", verifiedPhoneNumber);
            if (verifiedPhoneNumber != null) {
                String phoneId = verifiedPhoneNumber.getUuid();
                LOG.info("phoneId={}", phoneId);
                String phoneNumber = verifiedPhoneNumber.getPhoneNumber();
                boolean hasPhoneNumber = phoneNumber != null && !phoneNumber.isEmpty();
                if (phoneId != null && !hasPhoneNumber) {
                    try {
                        verifiedPhoneNumber =
                                crossStorageApi.find(defaultRepo, phoneId,
                                                     VerifiedPhoneNumber.class);
                        if (verifiedPhoneNumber != null) {
                            phoneNumber = verifiedPhoneNumber.getPhoneNumber();
                        }
                    } catch (Exception e) {
                        LOG.error("Error retrieving phone number with uuid: " + phoneId, e);
                        phoneNumber = null;
                    }
                }
                LOG.info("phoneNumber={}", phoneNumber);
                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    if (response.indexOf("emailAddress") > 0) {
                        response.append(",");
                    }
                    privateInfo
                            .append(String.format(
                                    "\"phoneNumber\": {\"value\": \"%s\", \"verified\": \"%s\", \"hash\": \"%s\"}",
                                    phoneNumber, verifiedPhoneNumber.getVerified(), verifiedPhoneNumber.getUuid()));
                }
            }
            privateInfo.append("}");
            response.append(String.format(",\"privateInfo\":%s",
                                          new Gson().toJson(privateInfo.toString())));
            try {
                wallet.setLastPrivateInfoRequest(requestTime);
                crossStorageApi.createOrUpdate(defaultRepo, wallet);
            } catch (Exception e) {
                LOG.error("Error updating wallet last private info request", e);
            }

        }
        response.append("}");

        return createResponse(requestId, response.toString());
    }
}
