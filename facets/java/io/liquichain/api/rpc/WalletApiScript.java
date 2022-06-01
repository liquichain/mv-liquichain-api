package io.liquichain.api.rpc;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.customEntities.LiquichainApp;
import org.meveo.model.customEntities.VerifiedEmail;
import org.meveo.model.customEntities.VerifiedPhoneNumber;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.*;
import org.web3j.utils.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class WalletApiScript extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(WalletApiScript.class);
    public static final List<String> WALLET_METHODS = Arrays
        .asList("wallet_creation", "wallet_update", "wallet_info", "wallet_report");

    private static final String PHONE_NUMBER_REQUIRED_ERROR = "Phone number is required";
    private static final String PHONE_NUMBER_EXISTS_ERROR = "Phone number: %s, already exists";
    private static final String NOT_IMPLEMENTED_ERROR = "Feature not yet implemented";
    private static final String CREATE_WALLET_ERROR = "Failed to create wallet";
    private static final String UPDATE_WALLET_ERROR = "Failed to update wallet";
    private static final String UNKNOWN_WALLET_ERROR = "Unknown wallet";
    private static final String UNKNOWN_APPLICATION_ERROR = "Unknown application";
    private static final String WALLET_EXISTS_ERROR = "Wallet already exists";
    private static final String INVALID_SIGNATURE_ERROR = "Invalid signature";
    private static final String INVALID_REQUEST = "-32600";
    private static final String INTERNAL_ERROR = "-32603";
    private static final String TRANSACTION_REJECTED = "-32003";
    private static final String METHOD_NOT_FOUND = "-32601";

    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);

    protected final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    protected final Repository defaultRepo = repositoryService.findDefaultRepository();
    protected ParamBean config = paramBeanFactory.getInstance();
    KeycloakUserService keycloakUserService = new KeycloakUserService(crossStorageApi, defaultRepo, config);

    protected String result;

    public String getResult() {
        return this.result;
    }

    private final String APP_NAME = config.getProperty("eth.api.appname", "licoin");

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
                    Numeric.hexStringToByteArray(s)))
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

    public static String createResponse(String requestId, String result) {
        String idFormat = requestId == null || NumberUtils.isParsable(requestId)
            ? "  \"id\": %s,"
            : "  \"id\": \"%s\",";
        String resultFormat = result.startsWith("{") ? "%s" : "\"%s\"";
        String response = "{\n" +
            String.format(idFormat, requestId) + "\n" +
            "  \"jsonrpc\": \"2.0\",\n" +
            "  \"result\": " + String.format(resultFormat, result) + "\n" +
            "}";
        LOG.debug("response: {}", response);
        return response;
    }

    public static String createErrorResponse(String requestId, String errorCode, String message) {
        String idFormat = requestId == null || NumberUtils.isParsable(requestId)
            ? "  \"id\": %s,"
            : "  \"id\": \"%s\",";
        String response = "{\n" +
            String.format(idFormat, requestId) + "\n" +
            "  \"jsonrpc\": \"2.0\",\n" +
            "  \"error\": {\n" +
            "    \"code\": " + errorCode + ",\n" +
            "    \"message\": \"" + message + "\"\n" +
            "  }\n" +
            "}";
        LOG.debug("error response: {}", response);
        return response;
    }

    public static String normalizeHash(String hash) {
        if (hash.startsWith("0x")) {
            return hash.substring(2).toLowerCase();
        }
        return hash.toLowerCase();
    }

    public static String retrieveHash(List<String> parameters, int parameterIndex) {
        return normalizeHash(parameters.get(parameterIndex));
    }

    private void validateSignature(String walletHash, String signature, String message)
        throws BusinessException {
        String validatedAddress;
        try {
            validatedAddress = parseAddress(signature, message);
        } catch (Exception e) {
            LOG.error(INVALID_REQUEST, e);
            throw new BusinessException(e.getMessage());
        }
        boolean sameAddress = walletHash.equals(validatedAddress);
        LOG.info("validated address: {}, walletHash: {}, same address: {}", validatedAddress,
            walletHash, sameAddress);

        if (!sameAddress) {
            throw new BusinessException(INVALID_SIGNATURE_ERROR);
        }
    }

    private String validatePhoneNumber(String phoneNumber, String walletId)
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

    private String createWallet(String requestId, Map<String, Object> parameters) {
        List<String> params = (ArrayList<String>) parameters.get("params");
        String name = params.get(0);
        String walletHash = retrieveHash(params, 1);
        String accountHash = retrieveHash(params, 2);
        String signature = params.get(3);
        String publicInfo = params.get(4);
        String privateInfo = null;
        if (params.size() > 5) {
            privateInfo = params.get(5);
        }

        try {
            validateSignature(walletHash, signature, privateInfo);
        } catch (BusinessException e) {
            return createErrorResponse(requestId, INVALID_REQUEST, e.getMessage());
        }

        Wallet wallet;
        LiquichainApp app;

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
                .fromJson(privateInfo, new TypeToken<Map<String, String>>() {
                }.getType());
            emailAddress = privateInfoMap.get("emailAddress");
            phoneNumber = privateInfoMap.get("phoneNumber");
        }

        if (phoneNumber != null) {
            try {
                phoneNumber = validatePhoneNumber(phoneNumber, walletHash);
            } catch (BusinessException e) {
                LOG.error(INVALID_REQUEST, e);
                return createErrorResponse(requestId, INVALID_REQUEST, e.getMessage());
            }
        }

        try {
            keycloakUserService.createUser(name, publicInfo, privateInfo);
        } catch (BusinessException e) {
            return createErrorResponse(requestId, INVALID_REQUEST, e.getMessage());
        }

        try {
            LOG.info(
                "wallet_creation Creating wallet with name={}, walletHash={}, accountHash={}, email={}, phoneNumber={}",
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
            if (!Objects.equals(newHash, walletHash)) {
                LOG.info("wallet_creation Wallet hash changed from {} to {}", walletHash, newHash);
                wallet.setUuid(walletHash);
                LOG.info("wallet_creation Attempt to update wallet with hash: {}", walletHash);
                newHash = crossStorageApi.createOrUpdate(defaultRepo, wallet);
                LOG.info("wallet_creation Updated wallet hash: {}", newHash);
            }
            return createResponse(requestId, walletHash);
        } catch (Exception e) {
            LOG.error(CREATE_WALLET_ERROR, e);
            return createErrorResponse(requestId, TRANSACTION_REJECTED, CREATE_WALLET_ERROR);
        }
    }

    private String updateWallet(String requestId, Map<String, Object> parameters) {
        List<String> params = (ArrayList<String>) parameters.get("params");
        String name = params.get(0);
        String walletHash = retrieveHash(params, 1);
        String signature = params.get(2);
        String publicInfo = params.get(3);
        String privateInfo = null;
        if (params.size() > 4) {
            privateInfo = params.get(4);
        }

        try {
            validateSignature(walletHash, signature, publicInfo);
        } catch (BusinessException e) {
            return createErrorResponse(requestId, INVALID_REQUEST, e.getMessage());
        }

        Wallet wallet;

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
                .fromJson(privateInfo, new TypeToken<Map<String, String>>() {
                }.getType());
            emailAddress = privateInfoMap.get("emailAddress");
            phoneNumber = privateInfoMap.get("phoneNumber");
        }

        LOG.info("wallet_update received email: {}", emailAddress);
        LOG.info("wallet_update received phoneNumber: {}", phoneNumber);

        VerifiedEmail verifiedEmail = wallet.getEmailAddress();
        VerifiedPhoneNumber verifiedPhoneNumber = wallet.getPhoneNumber();
        String existingEmail;
        String existingPhoneNumber;
        try {
            if (verifiedEmail != null) {
                LOG.info("wallet_update Verified email: {}", verifiedEmail.getUuid());
                VerifiedEmail oldEmail = findEntity(verifiedEmail.getUuid(), VerifiedEmail.class);
                if (oldEmail != null) {
                    existingEmail = oldEmail.getEmail();
                } else {
                    existingEmail = null;
                }
                LOG.info("wallet_update existing email: {}", existingEmail);
                if (emailAddress != null && existingEmail != null
                    && !existingEmail.equals(emailAddress)) {
                    verifiedEmail = new VerifiedEmail();
                    verifiedEmail.setUuid(DigestUtils.sha1Hex(emailAddress));
                    verifiedEmail.setEmail(emailAddress);
                    verifiedEmail.setWalletId(wallet.getUuid());
                    verifiedEmail.setVerified(false);
                    crossStorageApi.createOrUpdate(defaultRepo, verifiedEmail);
                    LOG.info("wallet_update old email: {}, saved email: {}", existingEmail, emailAddress);
                }
            } else if (emailAddress != null) {
                verifiedEmail = new VerifiedEmail();
                verifiedEmail.setUuid(DigestUtils.sha1Hex(emailAddress));
                verifiedEmail.setEmail(emailAddress);
                verifiedEmail.setWalletId(wallet.getUuid());
                verifiedEmail.setVerified(false);
                crossStorageApi.createOrUpdate(defaultRepo, verifiedEmail);
                LOG.info("wallet_update No old email, saved email: {}", emailAddress);
            }

            if (verifiedPhoneNumber != null) {
                LOG.info("wallet_update Verified phoneNumber: {}", verifiedPhoneNumber.getUuid());
                VerifiedPhoneNumber oldPhoneNumber =
                    findEntity(verifiedPhoneNumber.getUuid(), VerifiedPhoneNumber.class);
                if (oldPhoneNumber != null) {
                    existingPhoneNumber = oldPhoneNumber.getPhoneNumber();
                } else {
                    existingPhoneNumber = null;
                }
                LOG.info("wallet_update existing phoneNumber: {}", existingPhoneNumber);
                if (phoneNumber != null && existingPhoneNumber != null
                    && !existingPhoneNumber.equals(phoneNumber)) {
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

        Wallet wallet;

        LOG.info("wallet_info lookup wallet: {}", walletHash);
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
        long requestTime = 0L;
        if (shouldValidate) {
            String validatedAddress;
            try {
                validatedAddress = parseAddress(signature, message);
            } catch (Exception e) {
                LOG.error(INVALID_REQUEST, e);
                return createErrorResponse(requestId, INVALID_REQUEST, e.getMessage());
            }
            boolean sameAddress = walletHash.toLowerCase().equals(validatedAddress);
            LOG.info("wallet_info validated address: {}, walletHash: {}, same address: {}", validatedAddress,
                walletHash.toLowerCase(), sameAddress);
            Long lastRequest = wallet.getLastPrivateInfoRequest();
            if (lastRequest == null) {
                lastRequest = 0L;
            }
            requestTime = Long.parseLong(message.split(",")[2]);
            LOG.info("wallet_info lastRequest={}", lastRequest);
            LOG.info("wallet_info requestTime={}", requestTime);

            isValidSignature = sameAddress && requestTime > lastRequest;
            LOG.info("wallet_info isValidSignature={}", isValidSignature);
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
                LOG.info("wallet_info emailId={}", emailId);
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
                LOG.info("wallet_info emailAddress={}", emailAddress);
                if (emailAddress != null && !emailAddress.trim().isEmpty()) {
                    privateInfo.append(String.format(
                        "\"emailAddress\": {\"value\": \"%s\", \"verified\": \"%s\", \"hash\": \"%s\"}",
                        emailAddress, verifiedEmail.getVerified(),
                        verifiedEmail.getUuid()));
                }
            }

            VerifiedPhoneNumber verifiedPhoneNumber = wallet.getPhoneNumber();
            LOG.info("wallet_info verifiedPhoneNumber={}", verifiedPhoneNumber);
            if (verifiedPhoneNumber != null) {
                String phoneId = verifiedPhoneNumber.getUuid();
                LOG.info("wallet_info phoneId={}", phoneId);
                String phoneNumber = verifiedPhoneNumber.getPhoneNumber();
                boolean hasPhoneNumber = phoneNumber != null && !phoneNumber.isEmpty();
                if (phoneId != null && !hasPhoneNumber) {
                    try {
                        verifiedPhoneNumber = crossStorageApi.find(defaultRepo, phoneId, VerifiedPhoneNumber.class);
                        if (verifiedPhoneNumber != null) {
                            phoneNumber = verifiedPhoneNumber.getPhoneNumber();
                        }
                    } catch (Exception e) {
                        LOG.error("wallet_info Error retrieving phone number with uuid: " + phoneId, e);
                        phoneNumber = null;
                    }
                }
                LOG.info("wallet_info phoneNumber={}", phoneNumber);
                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                    if (privateInfo.indexOf("emailAddress") > 0) {
                        privateInfo.append(",");
                    }
                    privateInfo.append(String.format(
                        "\"phoneNumber\": {\"value\": \"%s\", \"verified\": \"%s\", \"hash\": \"%s\"}",
                        phoneNumber, verifiedPhoneNumber.getVerified(),
                        verifiedPhoneNumber.getUuid()));
                }
            }
            privateInfo.append("}");
            response.append(String.format(",\"privateInfo\":%s",
                new Gson().toJson(privateInfo.toString())));
            try {
                wallet.setLastPrivateInfoRequest(requestTime);
                crossStorageApi.createOrUpdate(defaultRepo, wallet);
            } catch (Exception e) {
                LOG.error("wallet_info Error updating wallet last private info request", e);
            }

        }
        response.append("}");

        return createResponse(requestId, response.toString());
    }
}


class KeycloakUserService {
    private static final Logger LOG = LoggerFactory.getLogger(KeycloakUserService.class);
    private static final Gson gson = new Gson();

    private static final int CONNECTION_POOL_SIZE = 50;
    private static final int MAX_POOLED_PER_ROUTE = 5;
    private static final long CONNECTION_TTL = 5;
    private static final Client client = new ResteasyClientBuilder()
        .connectionPoolSize(CONNECTION_POOL_SIZE)
        .maxPooledPerRoute(MAX_POOLED_PER_ROUTE)
        .connectionTTL(CONNECTION_TTL, TimeUnit.SECONDS)
        .build();

    private CrossStorageApi crossStorageApi;
    private Repository defaultRepo;
    private ParamBean config;

    private final String CLIENT_ID;
    private final String CLIENT_SECRET;
    private final String LOGIN_URL;
    private final String USERS_URL;

    public KeycloakUserService(CrossStorageApi crossStorageApi, Repository defaultRepo, ParamBean config) {
        this.crossStorageApi = crossStorageApi;
        this.defaultRepo = defaultRepo;
        this.config = config;

        String AUTH_URL = System.getProperty("meveo.keycloak.url");
        String REALM = System.getProperty("meveo.keycloak.realm");
        CLIENT_ID = config.getProperty("keycloak.client.id", "admin-cli");
        CLIENT_SECRET = config.getProperty("keycloak.client.secret", "1d1e1d9f-2d98-4f43-ac69-c8ecc1f188a5");
        LOGIN_URL = AUTH_URL + "/realms/master/protocol/openid-connect/token";
        USERS_URL = AUTH_URL + "/admin/realms/" + REALM + "/users";
    }

    private boolean isNotEmptyMap(Map<String, ?> map) {
        return map != null && !map.isEmpty();
    }

    private String login() {
        LOG.info("login - START");
        String token;
        Response response = null;
        try {
            Form form = new Form()
                .param("grant_type", "client_credentials")
                .param("client_id", CLIENT_ID)
                .param("client_secret", CLIENT_SECRET);

            response = client.target(LOGIN_URL)
                             .request(MediaType.APPLICATION_FORM_URLENCODED)
                             .post(Entity.form(form));
            String loginData = response.readEntity(String.class);
            Map<String, String> dataMap = gson.fromJson(loginData, new TypeToken<Map<String, String>>() {
            }.getType());
            token = dataMap.get("access_token");
        } finally {
            if (response != null) {
                response.close();
            }
        }
        LOG.info("login - token={}", token);
        return token;
    }

    public void createUser(String name, String publicInfo, String privateInfo) throws BusinessException {
        Map<String, Object> publicInfoMap = null;
        Map<String, String> privateInfoMap = null;
        if (StringUtils.isNotBlank(publicInfo)) {
            publicInfoMap = gson.fromJson(publicInfo, new TypeToken<Map<String, Object>>() {
            }.getType());
        }

        if (StringUtils.isNotBlank(privateInfo)) {
            privateInfoMap = gson.fromJson(privateInfo, new TypeToken<Map<String, String>>() {
            }.getType());
        }
        String username = null;
        String shippingAddress = null;
        String coords = null;
        String base64Avatar = null;
        if (isNotEmptyMap(publicInfoMap)) {
            username = (String) publicInfoMap.get("username");

            shippingAddress = gson.toJson(publicInfoMap.get("shippingAddress"));
            coords = (String) publicInfoMap.get("coords");
            base64Avatar = (String) publicInfoMap.get("base64Avatar");
        }
        String emailAddress = null;
        String phoneNumber = null;
        String password = null;
        if (isNotEmptyMap(privateInfoMap)) {
            password = privateInfoMap.get("password");
            emailAddress = privateInfoMap.get("emailAddress");
            phoneNumber = privateInfoMap.get("phoneNumber");
        }
        String token = login();

        username = StringUtils.isBlank(username) ? "" : username;
        shippingAddress = StringUtils.isBlank(shippingAddress) ? "" : gson.toJson(shippingAddress);
        coords = StringUtils.isBlank(coords) ? "" : coords;
        base64Avatar = StringUtils.isBlank(base64Avatar) ? "" : base64Avatar;
        emailAddress = StringUtils.isBlank(emailAddress) ? "" : emailAddress;
        phoneNumber = StringUtils.isBlank(phoneNumber) ? "" : phoneNumber;

        String userDetails = "{\n" +
            "    \"username\": \"" + username + "\",\n" +
            "    \"enabled\": true,\n" +
            "    \"email\": \"" + emailAddress + "\",\n" +
            "    \"emailVerified\": true,\n" +
            "    \"firstName\": \"" + name + "\",\n" +
            "    \"lastName\": \"\",\n" +
            "    \"attributes\": {\n" +
            "        \"locale\": [\n\"en\"\n],\n" +
            "        \"phoneNumber\": \"" + phoneNumber + "\",\n" +
            "        \"shippingAddress\": " + shippingAddress + ",\n" +
            "        \"coords\": \"" + coords + "\",\n" +
            "        \"base64Avatar\": \"" + base64Avatar + "\"\n" +
            "    },\n" +
            "    \"credentials\": [{" +
            "        \"type\": \"password\",\n" +
            "        \"value\": \"" + password + "\",\n" +
            "        \"temporary\": false\n" +
            "    }],\n" +
            "    \"access\": {\n" +
            "        \"manageGroupMembership\": true,\n" +
            "        \"view\": true,\n" +
            "        \"mapRoles\": true,\n" +
            "        \"impersonate\": false,\n" +
            "        \"manage\": true\n" +
            "    }\n" +
            "}";
        LOG.info("userDetails: {}", userDetails);
        Response response = null;
        String postResult;
        try {

            response = client.target(USERS_URL)
                             .request(MediaType.APPLICATION_JSON)
                             .header("Authorization", "Bearer " + token)
                             .post(Entity.json(userDetails));
            postResult = response.readEntity(String.class);
            if (postResult != null && postResult.contains("error")) {
                throw new BusinessException("Failed to save new keycloak user.");
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        LOG.info("postResult: {}", postResult);
    }

}
