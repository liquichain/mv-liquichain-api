package io.liquichain.api.service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.commons.utils.StringUtils;
import org.meveo.model.admin.User;
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
import org.meveo.service.storage.RepositoryService;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeycloakUserService extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(KeycloakUserService.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final int CONNECTION_POOL_SIZE = 50;
    private static final int MAX_POOLED_PER_ROUTE = 5;
    private static final long CONNECTION_TTL = 5;
    private static final Client client = new ResteasyClientBuilder()
            .connectionPoolSize(CONNECTION_POOL_SIZE)
            .maxPooledPerRoute(MAX_POOLED_PER_ROUTE)
            .connectionTTL(CONNECTION_TTL, TimeUnit.SECONDS)
            .build();

    private final UserService userService = getCDIBean(UserService.class);
    private final RoleService roleService = getCDIBean(RoleService.class);
    private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
    private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
    private final Repository defaultRepo = repositoryService.findDefaultRepository();
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ParamBean config = paramBeanFactory.getInstance();

    private final String APPCLIENT_CREDENTIALS = config.getProperty("appclient.credentials", "");
    private final String AUTH_URL = System.getProperty("meveo.keycloak.url");
    private final String REALM = System.getProperty("meveo.keycloak.realm");
    private final String CLIENT_ID = config.getProperty("keycloak.client.id", "admin-cli");
    private final String CLIENT_SECRET = config.getProperty("keycloak.client.secret",
            "1d1e1d9f-2d98-4f43-ac69-c8ecc1f188a5");
    private final String LOGIN_URL = AUTH_URL + "/realms/master/protocol/openid-connect/token";
    private final String USER_LOGIN_URL = AUTH_URL + "/realms/meveo/protocol/openid-connect/token";
    private final String CLIENT_REALM_URL = AUTH_URL + "/admin/realms/" + REALM;
    private final String USERS_URL = CLIENT_REALM_URL + "/users";

    private boolean isNotEmptyMap(Map<String, ?> map) {
        return map != null && !map.isEmpty();
    }

    public static String toJson(Object data) {
        String json = null;
        try {
            json = mapper.writeValueAsString(data);
        } catch (Exception e) {
            LOG.error("Failed to convert to json: {}", data, e);
        }
        return json;
    }

    public static <T> T convert(String data) {
        T value = null;
        try {
            value = mapper.readValue((data), new TypeReference<T>() {
            });
        } catch (Exception e) {
            LOG.error("Failed to parse data: {}", data, e);
        }
        return value;
    }

    private Set<Role> fetchDefaultRoles() {
        Set<Role> defaultRoles = new HashSet<>();
        defaultRoles.add(roleService.findByName(DefaultRole.EXECUTE_ALL_ENDPOINTS.getRoleName()));
        defaultRoles.add(roleService.findByName(DefaultRole.READ_ALL_CE.getRoleName()));
        defaultRoles.add(roleService.findByName(DefaultRole.MODIFY_ALL_CE.getRoleName()));
        defaultRoles.add(roleService.findByName("APP_USER"));
        return defaultRoles;
    }

    private String buildUserDetails(String username, String emailAddress, String name, String password) {
        emailAddress = StringUtils.isBlank(emailAddress) ? "" : emailAddress;

        String userDetails = "{\n" +
                "    \"username\": \"" + username + "\",\n" +
                "    \"enabled\": true,\n" +
                "    \"email\": \"" + emailAddress + "\",\n" +
                "    \"emailVerified\": true,\n" +
                "    \"firstName\": \"" + name + "\",\n" +
                "    \"lastName\": \"\",\n" +
                "    \"attributes\": {\n" +
                "        \"locale\": [\"en\"]\n" +
                "    },\n" +
                "    \"credentials\": [{\n" +
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
        return userDetails;
    }

    private String login() {
        LOG.debug("login - START");
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
            Map<String, String> dataMap = convert(loginData);
            token = dataMap.get("access_token");
        } finally {
            if (response != null) {
                response.close();
            }
        }
        LOG.debug("login - SUCCESS");
        return token;
    }

    public String loginAppClient() {
        LOG.debug("login - START");
        String token;
        Response response = null;
        try {
            Form form = new Form()
                    .param("grant_type", "password")
                    .param("client_id", "meveo-web")
                    .param("username", "appclient")
                    .param("password", APPCLIENT_CREDENTIALS);

            response = client.target(USER_LOGIN_URL)
                             .request(MediaType.APPLICATION_FORM_URLENCODED)
                             .post(Entity.form(form));
            String loginData = response.readEntity(String.class);
            Map<String, String> dataMap = convert(loginData);
            token = dataMap.get("access_token");
        } finally {
            if (response != null) {
                response.close();
            }
        }
        LOG.debug("login - SUCCESS");
        return token;
    }

    public Map<String, Object> findUser(String username) throws BusinessException {
        String token = login();
        return findUser(token, username);
    }

    public Map<String, Object> findUser(String token, String username) throws BusinessException {
        Response response = null;
        String getResult;
        List<Map<String, Object>> dataMap;
        try {
            String findUserUrl = USERS_URL + "?username=" + username;
            response = client.target(findUserUrl)
                             .request(MediaType.APPLICATION_JSON)
                             .header("Authorization", "Bearer " + token)
                             .get();
            getResult = response.readEntity(String.class);
            if (getResult != null && getResult.contains("error")) {
                LOG.error("Failed to find keycloak user: " + username + ". Error: " + getResult);
                throw new BusinessException("Failed to find keycloak user: " + username);
            }
            dataMap = convert(getResult);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return dataMap != null && dataMap.size() > 0 ? dataMap.get(0) : new HashMap<>();
    }

    public String createKeycloakUser(String token, String userDetails) throws BusinessException {
        Response response = null;
        String saveResult;
        try {
            response = client.target(USERS_URL)
                             .request(MediaType.APPLICATION_JSON)
                             .header("Authorization", "Bearer " + token)
                             .post(Entity.json(userDetails));
            saveResult = response.readEntity(String.class);
            if (saveResult != null && saveResult.contains("error")) {
                Map<String, Object> resultMap = convert(saveResult);
                LOG.error("Failed to save new keycloak user: " + saveResult);
                String errorMessage = "" + resultMap.get("errorMessage");
                throw new BusinessException("Failed to save new keycloak user. Cause: " + errorMessage);
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return saveResult;
    }

    public String deleteKeycloakUser(String token, String userId) throws BusinessException {
        Response response = null;
        String deleteResult;
        try {
            response = client.target(USERS_URL + "/" + userId)
                             .request(MediaType.APPLICATION_JSON)
                             .header("Authorization", "Bearer " + token)
                             .delete();
            deleteResult = response.readEntity(String.class);
            if (deleteResult != null && deleteResult.contains("error")) {
                Map<String, Object> resultMap = convert(deleteResult);
                LOG.error("Failed to delete keycloak user: " + deleteResult);
                String errorMessage = "" + resultMap.get("errorMessage");
                throw new BusinessException("Failed to delete keycloak user. Cause: " + errorMessage);
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return deleteResult;
    }

    public String updateKeycloakUser(String token, String userId, String userDetails) throws BusinessException {
        Response response = null;
        String updateResult;
        try {
            String requestUrl = USERS_URL + "/" + userId;
            LOG.debug("update url: {}", requestUrl);
            LOG.debug("userDetails: {}", userDetails);
            response = client.target(requestUrl)
                             .request(MediaType.APPLICATION_JSON)
                             .header("Authorization", "Bearer " + token)
                             .put(Entity.json(userDetails));
            updateResult = response.readEntity(String.class);
            if (updateResult != null && updateResult.contains("error")) {
                LOG.error("Failed to update keycloak user: " + updateResult);
                throw new BusinessException("Failed to update keycloak user.");
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return updateResult;
    }

    public void saveKeycloakAttribute(String username, String attribute, String value) throws BusinessException {
        String token = login();
        Map<String, Object> userMap = findUser(token, username);
        LOG.debug("userMap: {}", userMap);
        Map<String, Object> attributes = (Map<String, Object>) userMap.get("attributes");
        attributes.put(attribute, value);
        userMap.put("attributes", attributes);
        String userDetails = toJson(userMap);
        String updateResult = updateKeycloakUser(token, "" + userMap.get("id"), userDetails);
        LOG.debug("saveKeycloakAttribute result: {}", updateResult);
    }

    public void createMeveoUser(String name, String username, String emailAddress) throws BusinessException {
        User user = new User();
        Name fullName = new Name();

        fullName.setFirstName(name);
        user.setName(fullName);
        user.setUserName(username);
        user.setEmail(emailAddress);
        user.setRoles(fetchDefaultRoles());

        userService.create(user);
    }

    public String createUser(String name, String publicInfo, String privateInfo) throws BusinessException {
        Map<String, Object> publicMap = StringUtils.isNotBlank(publicInfo) ? convert(publicInfo) : null;
        Map<String, String> privateMap = StringUtils.isNotBlank(privateInfo) ? convert(privateInfo) : null;

        String username = null;
        if (isNotEmptyMap(publicMap)) {
            username = (String) publicMap.get("username");
        }

        String emailAddress = null;
        String password = null;
        if (isNotEmptyMap(privateMap)) {
            username = StringUtils.isNotBlank(privateMap.get("username")) ? privateMap.get("username") : username;
            password = privateMap.get("password");
            emailAddress = privateMap.get("emailAddress");
        }
        emailAddress = StringUtils.isBlank(emailAddress) ? "" : emailAddress;

        boolean hasUsername = StringUtils.isNotBlank(username);
        boolean hasPassword = StringUtils.isNotBlank(password);
        if (hasUsername && hasPassword) {
            return createUser(username, emailAddress, name, password);
        } else {
            LOG.warn("No username and password included, will not create keycloak and meveo user.");
            return username;
        }
    }

    public String createUser(String username, String emailAddress, String name, String password)
            throws BusinessException {
        String token = login();
        String userDetails = buildUserDetails(username, emailAddress, name, password);
        String keycloakUser = createKeycloakUser(token, userDetails);
        createMeveoUser(name, username, emailAddress);
        LOG.info("keycloak result: {}", keycloakUser);
        return username;
    }

    public void deleteUser(User user) throws BusinessException {
        String username = user.getUserName();
        String token = login();

        Map<String, Object> userMap = findUser(token, username);
        String userId = "" + userMap.get("id");

        deleteKeycloakUser(token, userId);
        userService.remove(user);
    }

    public void updateUser(String name, String publicInfo, String privateInfo, Wallet wallet) throws BusinessException {
        LOG.debug("publicInfo == {}", publicInfo);
        String currentPublicInfo = wallet.getPublicInfo();
        String currentPrivateInfo = wallet.getPrivateInfo();
        LOG.debug("wallet currentPublicInfo == {}", wallet.getPublicInfo());

        Map<String, Object> currentPublicInfoMap = null;
        if (StringUtils.isNotBlank(currentPublicInfo)) {
            currentPublicInfoMap = convert(currentPublicInfo);
        }

        Map<String, String> currentPrivateInfoMap = null;
        if (StringUtils.isNotBlank(currentPrivateInfo)) {
            currentPrivateInfoMap = convert(currentPrivateInfo);
        }

        String currentUsername = null;
        String currentLocale = null;
        if (isNotEmptyMap(currentPublicInfoMap)) {
            currentUsername = (String) currentPublicInfoMap.get("username");
            currentLocale = (String) currentPublicInfoMap.get("locale");
        }
        currentLocale = StringUtils.isBlank(currentLocale) ? "en" : currentLocale;

        if (isNotEmptyMap(currentPrivateInfoMap)) {
            currentUsername = StringUtils.isNotBlank(currentPrivateInfoMap.get("username"))
                    ? currentPrivateInfoMap.get("username")
                    : currentUsername;
        }

        LOG.debug("currentUsername: {}", currentUsername);
        if (StringUtils.isBlank(currentUsername)) {
            createUser(name, publicInfo, privateInfo);
        } else {
            Map<String, Object> publicMap = StringUtils.isNotBlank(publicInfo) ? convert(publicInfo) : null;
            Map<String, String> privateMap = StringUtils.isNotBlank(privateInfo) ? convert(privateInfo) : null;

            String username = "";
            String locale = "";
            if (isNotEmptyMap(publicMap)) {
                username = (String) publicMap.get("username");
                locale = (String) publicMap.get("locale");
            }

            String emailAddress = "";
            String password = "";
            if (isNotEmptyMap(privateMap)) {
                username = StringUtils.isNotBlank(privateMap.get("username")) ? privateMap.get("username") : username;
                password = privateMap.get("password");
                emailAddress = privateMap.get("emailAddress");
            }

            VerifiedEmail verifiedEmail = wallet.getEmailAddress();
            if (verifiedEmail != null) {
                verifiedEmail = crossStorageApi.find(defaultRepo, verifiedEmail.getUuid(), VerifiedEmail.class);
            }
            String currentEmailAddress = verifiedEmail != null ? verifiedEmail.getEmail() : null;

            boolean hasPassword = !StringUtils.isBlank(password);
            boolean hasUsername = !StringUtils.isBlank(username);

            boolean differentName = StringUtils.compare(name, wallet.getName()) != 0;
            boolean differentEmailAddress = StringUtils.compare(emailAddress, currentEmailAddress) != 0;
            boolean differentUsername = StringUtils.compare(username, currentUsername) != 0;
            boolean differentLocale = StringUtils.compare(locale, currentLocale) != 0;

            boolean shouldUpdateUser = hasPassword || hasUsername
                    && (differentName || differentEmailAddress || differentUsername || differentLocale);

            LOG.debug("hasPassword: {}", hasPassword);
            LOG.debug("hasUsername: {}", hasUsername);
            LOG.debug("name: {} => {}", wallet.getName(), name);
            LOG.debug("email address: {} => {}", currentEmailAddress, emailAddress);
            LOG.debug("username: {} => {}", currentUsername, username);
            LOG.debug("locale: {} => {}", currentLocale, locale);
            LOG.debug("shouldUpdateUser: {}", shouldUpdateUser);

            if (shouldUpdateUser) {
                String token = login();
                Map<String, Object> userMap = findUser(token, currentUsername);
                if (userMap != null) { // update keycloak user
                    if (differentName) {
                        userMap.put("firstName", name);
                    }
                    if (differentEmailAddress && StringUtils.isNotBlank(emailAddress)) {
                        userMap.put("email", emailAddress);
                        userMap.put("emailVerified", true);
                    }
                    if (differentUsername) {
                        userMap.put("username", username);
                    }
                    if (!StringUtils.isBlank(password)) {
                        List<Map<String, Object>> credentials = new ArrayList<>();
                        Map<String, Object> credentialMap = new HashMap<>();
                        credentialMap.put("type", "password");
                        credentialMap.put("value", password);
                        credentialMap.put("temporary", false);
                        credentials.add(credentialMap);
                        userMap.put("credentials", credentials);
                    }

                    if (differentLocale) {
                        LOG.debug("add/replace locale");
                        Map<String, Object> attributesMap = Objects
                                .requireNonNullElse((Map<String, Object>) userMap.get("attributes"), new HashMap<>());
                        String newLocale = Objects.requireNonNullElse(locale, currentLocale);
                        List<String> localeList = new ArrayList<>();
                        localeList.add(newLocale);
                        attributesMap.put("locale", localeList);
                        userMap.put("attributes", attributesMap);
                    }

                    String userDetails = toJson(userMap);
                    String updateResult = updateKeycloakUser(token, "" + userMap.get("id"), userDetails);
                    LOG.debug("updateResult: {}", updateResult);
                } else { // create keycloak user
                    if (StringUtils.isBlank(password)) {
                        String errorMessage =
                                "Keycloak user does not exist, include a password in privateInfo to create a new one.";
                        LOG.error(errorMessage);
                        throw new BusinessException(errorMessage);
                    }
                    String userDetails = buildUserDetails(username, emailAddress, name, password);
                    String saveResult = createKeycloakUser(token, userDetails);
                    createMeveoUser(name, username, emailAddress);
                    LOG.debug("saveResult: {}", saveResult);
                }
            } else {
                LOG.debug("No changes detected, will not update keycloak user");
            }

            if (shouldUpdateUser) {
                LOG.debug("shouldUpdateUser");
                User user = userService.findByUsername(currentUsername);
                if (user != null) { // update meveo user
                    // TODO - meveo username cannot be updated
                    // if (differentUsername) {
                    // user.setUserName(username);
                    // }
                    if (differentEmailAddress) {
                        user.setEmail(emailAddress);
                    }
                    if (differentName) {
                        Name fullName = new Name();
                        fullName.setFirstName(name);
                        user.setName(fullName);
                    }
                    userService.update(user);
                } else { // create meveo user
                    createMeveoUser(name, username, emailAddress);
                }
            } else {
                LOG.debug("No changes detected, will not update meveo user");
            }
        }
    }

    public void updateUserPasswordByPhoneNumber(String phoneNumber, String password) throws BusinessException {
        try {
            VerifiedPhoneNumber verifiedPhoneNumber = crossStorageApi.find(defaultRepo, VerifiedPhoneNumber.class)
                                                                     .by("phoneNumber", phoneNumber)
                                                                     .getResult();
            Wallet wallet = crossStorageApi.find(defaultRepo, Wallet.class)
                                           .by("phoneNumber", verifiedPhoneNumber)
                                           .getResult();

            Map<String, Object> privateInfo = convert(wallet.getPrivateInfo());
            String username = "" + privateInfo.get("username");
            String token = login();
            Map<String, Object> userMap = findUser(token, username);

            if (userMap != null) { // update keycloak user
                LOG.debug("new password: {}", password);
                List<Map<String, Object>> credentials = new ArrayList<>();
                Map<String, Object> credentialMap = new HashMap<>();
                credentialMap.put("type", "password");
                credentialMap.put("value", password);
                credentialMap.put("temporary", false);
                credentials.add(credentialMap);
                userMap.put("credentials", credentials);
                String userDetails = toJson(userMap);
                String updateResult = updateKeycloakUser(token, "" + userMap.get("id"), userDetails);
                LOG.debug("updateResult: {}", updateResult);
            } else { // create keycloak user
                String emailUuid = wallet.getEmailAddress().getUuid();
                VerifiedEmail verifiedEmail = crossStorageApi.find(defaultRepo, emailUuid, VerifiedEmail.class);
                String name = wallet.getName();
                String emailAddress = verifiedEmail.getEmail();
                String userDetails = this.buildUserDetails(username, emailAddress, name, password);
                String saveResult = this.createKeycloakUser(token, userDetails);
                this.createMeveoUser(name, username, emailAddress);
                LOG.debug("saveResult: {}", saveResult);
            }
        } catch (Exception e) {
            String errorMessage = "Failed to update user with phone number: " + phoneNumber + ". - " + e.getMessage();
            LOG.error(errorMessage, e);
            throw new BusinessException(errorMessage, e);
        }
    }
}
