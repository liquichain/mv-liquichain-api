package io.liquichain.api.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class KeycloakUserService extends Script {
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

    private final UserService userService;
    private final RoleService roleService;
    private final CrossStorageApi crossStorageApi;
    private final Repository defaultRepo;

    private final String CLIENT_ID;
    private final String CLIENT_SECRET;
    private final String LOGIN_URL;
    private final String USERS_URL;

    public KeycloakUserService(ParamBean config, CrossStorageApi crossStorageApi, Repository defaultRepo,
        UserService userService, RoleService roleService) {
        this.crossStorageApi = crossStorageApi;
        this.defaultRepo = defaultRepo;
        this.userService = userService;
        this.roleService = roleService;

        String AUTH_URL = System.getProperty("meveo.keycloak.url");
        String REALM = System.getProperty("meveo.keycloak.realm");
        CLIENT_ID = config.getProperty("keycloak.client.id", "admin-cli");
        CLIENT_SECRET = config.getProperty("keycloak.client.secret", "1d1e1d9f-2d98-4f43-ac69-c8ecc1f188a5");
        LOGIN_URL = AUTH_URL + "/realms/master/protocol/openid-connect/token";
        String CLIENT_REALM_URL = AUTH_URL + "/admin/realms/" + REALM;
        USERS_URL = CLIENT_REALM_URL + "/users";
    }

    private boolean isNotEmptyMap(Map<String, ?> map) {
        return map != null && !map.isEmpty();
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

    private <T> T convertToMap(String data) {
        return gson.fromJson(data, new TypeToken<T>() {
        }.getType());
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
            Map<String, String> dataMap = convertToMap(loginData);
            token = dataMap.get("access_token");
        } finally {
            if (response != null) {
                response.close();
            }
        }
        LOG.info("login - SUCCESS");
        return token;
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
            dataMap = convertToMap(getResult);
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
                LOG.error("Failed to save new keycloak user: " + saveResult);
                throw new BusinessException("Failed to save new keycloak user.");
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return saveResult;
    }

    public String updateKeycloakUser(String token, String userId, String userDetails) throws BusinessException {
        Response response = null;
        String postResult;
        try {
            response = client.target(USERS_URL + "/" + userId)
                             .request(MediaType.APPLICATION_JSON)
                             .header("Authorization", "Bearer " + token)
                             .put(Entity.json(userDetails));
            postResult = response.readEntity(String.class);
            if (postResult != null && postResult.contains("error")) {
                LOG.error("Failed to update keycloak user: " + postResult);
                throw new BusinessException("Failed to update keycloak user.");
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return postResult;
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

    public void createUser(String name, String publicInfo, String privateInfo) throws BusinessException {
        Map<String, Object> publicMap = StringUtils.isNotBlank(publicInfo) ? convertToMap(publicInfo) : null;
        Map<String, String> privateMap = StringUtils.isNotBlank(privateInfo) ? convertToMap(privateInfo) : null;

        String username = null;
        if (isNotEmptyMap(publicMap)) {
            username = String.valueOf(publicMap.get("username"));
        }
        LOG.info("public info username: {}", username);

        String emailAddress = null;
        String password = null;
        if (isNotEmptyMap(privateMap)) {
            username = StringUtils.isNotBlank(privateMap.get("username")) ? privateMap.get("username") : username;
            password = privateMap.get("password");
            emailAddress = privateMap.get("emailAddress");
        }
        LOG.info("private info username: {}", username);

        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            String token = login();
            String userDetails = buildUserDetails(username, emailAddress, name, password);
            String saveResult = createKeycloakUser(token, userDetails);
            createMeveoUser(name, username, emailAddress);
            LOG.info("saveResult: {}", saveResult);
        } else {
            LOG.info("No username and password included, will not create keycloak and meveo user.");
        }
    }

    public void updateUser(String name, String publicInfo, String privateInfo, Wallet wallet) throws BusinessException {
        String currentPublicInfo = wallet.getPublicInfo();
        String currentPrivateInfo = wallet.getPrivateInfo();

        Map<String, Object> currentPublicInfoMap = null;
        if (StringUtils.isNotBlank(currentPublicInfo)) {
            currentPublicInfoMap = convertToMap(currentPublicInfo);
        }

        Map<String, String> currentPrivateInfoMap = null;
        if (StringUtils.isNotBlank(currentPrivateInfo)) {
            currentPrivateInfoMap = convertToMap(currentPrivateInfo);
        }

        String currentUsername = null;

        if (isNotEmptyMap(currentPublicInfoMap)) {
            currentUsername = String.valueOf(currentPublicInfoMap.get("username"));
        }
        LOG.info("public info currentUsername: {}", currentUsername);

        if (isNotEmptyMap(currentPrivateInfoMap)) {
            currentUsername = StringUtils.isNotBlank(currentPrivateInfoMap.get("username"))
                ? currentPrivateInfoMap.get("username")
                : currentUsername;
        }
        LOG.info("private info currentUsername: {}", currentUsername);

        if (StringUtils.isBlank(currentUsername)) {
            createUser(name, publicInfo, privateInfo);
        } else {
            Map<String, Object> publicMap = StringUtils.isNotBlank(publicInfo) ? convertToMap(publicInfo) : null;
            Map<String, String> privateMap = StringUtils.isNotBlank(privateInfo) ? convertToMap(privateInfo) : null;

            String username = "";
            if (isNotEmptyMap(publicMap)) {
                username = String.valueOf(publicMap.get("username"));
            }
            LOG.info("public info username: {}", username);

            String emailAddress = "";
            String password = "";
            if (isNotEmptyMap(privateMap)) {
                username = StringUtils.isNotBlank(privateMap.get("username")) ? privateMap.get("username") : username;
                password = String.valueOf(privateMap.get("password"));
                emailAddress = String.valueOf(privateMap.get("emailAddress"));
            }
            LOG.info("private info username: {}", username);

            LOG.info("wallet emailAddress: {}", wallet.getEmailAddress());
            VerifiedEmail verifiedEmail = wallet.getEmailAddress();
            if (verifiedEmail != null) {
                verifiedEmail = crossStorageApi.find(defaultRepo, verifiedEmail.getUuid(), VerifiedEmail.class);
            }
            String currentEmailAddress = verifiedEmail != null ? verifiedEmail.getEmail() : null;
            LOG.info("currentEmailAddress: {}", currentEmailAddress);

            boolean hasUsername = !"null".equalsIgnoreCase(username) || StringUtils.isNotBlank(username);
            boolean differentName = !String.valueOf(name).equals(wallet.getName());
            boolean differentEmailAddress = !emailAddress.equals(currentEmailAddress);
            boolean differentUsername = !username.equals(currentUsername);

            boolean shouldUpdateUser = hasUsername && (differentName || differentEmailAddress || differentUsername);

            LOG.info("hasUsername: {}", hasUsername);
            LOG.info("name: {} => {}", wallet.getName(), name);
            LOG.info("email address: {} => {}", currentEmailAddress, emailAddress);
            LOG.info("username: {} => {}", currentUsername, username);
            LOG.info("shouldUpdateUser: {}", shouldUpdateUser);

            if (shouldUpdateUser) {
                String token = login();
                Map<String, Object> userMap = findUser(token, currentUsername);
                if (userMap != null) { // update keycloak user
                    if (differentName) {
                        userMap.put("firstName", name);
                    }
                    if (differentEmailAddress) {
                        userMap.put("email", emailAddress);
                        userMap.put("emailVerified", true);
                    }
                    if (differentUsername) {
                        userMap.put("username", username);
                    }
                    String userDetails = gson.toJson(userMap);
                    String updateResult = updateKeycloakUser(token, "" + userMap.get("id"), userDetails);
                    LOG.info("updateResult: {}", updateResult);
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
                    LOG.info("saveResult: {}", saveResult);
                }
            } else {
                LOG.info("No changes detected, will not update keycloak user");
            }

            if (shouldUpdateUser) {
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
                LOG.info("No changes detected, will not update meveo user");
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


            Map<String, Object> privateInfo = convertToMap(wallet.getPrivateInfo());
            String username = "" + privateInfo.get("username");
            String token = login();
            Map<String, Object> userMap = findUser(token, username);

            if (userMap != null) { // update keycloak user
                LOG.info("new password: {}", password);
                List<Map<String, Object>> credentials = new ArrayList<>();
                Map<String, Object> credentialMap = new HashMap<>();
                credentialMap.put("type", "password");
                credentialMap.put("value", password);
                credentialMap.put("temporary", false);
                credentials.add(credentialMap);
                userMap.put("credentials", credentials);
                String userDetails = gson.toJson(userMap);
                String updateResult = updateKeycloakUser(token, "" + userMap.get("id"), userDetails);
                LOG.info("updateResult: {}", updateResult);
            } else { // create keycloak user
                String emailUuid = wallet.getEmailAddress().getUuid();
                VerifiedEmail verifiedEmail = crossStorageApi.find(defaultRepo, emailUuid, VerifiedEmail.class);
                String name = wallet.getName();
                String emailAddress = verifiedEmail.getEmail();
                String userDetails = this.buildUserDetails(username, emailAddress, name, password);
                String saveResult = this.createKeycloakUser(token, userDetails);
                this.createMeveoUser(name, username, emailAddress);
                LOG.info("saveResult: {}", saveResult);
            }
        } catch (Exception e) {
            String errorMessage = "Failed to update user with phone number: " + phoneNumber + ". - " + e.getMessage();
            LOG.error(errorMessage, e);
            throw new BusinessException(errorMessage, e);
        }
    }
}
