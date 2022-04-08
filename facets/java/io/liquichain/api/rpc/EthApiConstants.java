package io.liquichain.api.rpc;

import org.meveo.service.script.Script;

public class EthApiConstants extends Script {
    public static final String NOT_IMPLEMENTED_ERROR = "Feature not yet implemented";
    public static final String CONTRACT_NOT_ALLOWED_ERROR = "Contract deployment not allowed";
    public static final String CREATE_WALLET_ERROR = "Failed to create wallet";
    public static final String UPDATE_WALLET_ERROR = "Failed to update wallet";
    public static final String UNKNOWN_WALLET_ERROR = "Unknown wallet";
    public static final String UNKNOWN_APPLICATION_ERROR = "Unknown application";
    public static final String WALLET_EXISTS_ERROR = "Wallet already exists";
    public static final String NAME_REQUIRED_ERROR = "Wallet name is required";
    public static final String NAME_EXISTS_ERROR = "Wallet with name: %s, already exists";
    public static final String EMAIL_REQUIRED_ERROR = "Email address is required";
    public static final String PHONE_NUMBER_REQUIRED_ERROR = "Phone number is required";
    public static final String EMAIL_EXISTS_ERROR = "Email address: %s, already exists";
    public static final String PHONE_NUMBER_EXISTS_ERROR = "Phone number: %s, already exists";
    public static final String INVALID_SIGNATURE_ERROR = "Invalid signature";
    public static final String TRANSACTION_EXISTS_ERROR = "Transaction already exists: {}";
    public static final String INVALID_REQUEST = "-32600";
    public static final String INTERNAL_ERROR = "-32603";
    public static final String RESOURCE_NOT_FOUND = "-32001";
    public static final String TRANSACTION_REJECTED = "-32003";
    public static final String METHOD_NOT_FOUND = "-32601";
    public static final String PROXY_REQUEST_ERROR = "Proxy request to remote json-rpc endpoint failed";

    public enum BLOCKCHAIN_TYPE {DATABASE, BESU, FABRIC, SMART_CONTRACT, BESU_DB}
}
