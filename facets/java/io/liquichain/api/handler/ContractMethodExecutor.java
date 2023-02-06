package io.liquichain.api.handler;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.RawTransaction;

public class ContractMethodExecutor extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(ContractMethodExecutor.class);
    private final Map<String, String> contractMethodHandlers;
    private static final MethodHandlerResult DEFAULT_RESULT = new MethodHandlerResult(
        "transfer",
        MethodHandlerResult.DEFAULT_DATA,
        BigInteger.ZERO
    );

    public ContractMethodExecutor(Map<String, String> contractMethodHandlers) {
        super();
        this.contractMethodHandlers = contractMethodHandlers;
    }

    public static String normalize(String data) {
        if (data == null) {
            return "";
        }
        if (data.startsWith("0x")) {
            return data;
        }
        return "0x" + data;
    }

    public static String lowercaseHash(String data) {
        return normalize(data).toLowerCase();
    }

    public interface ContractMethodHandler {
        MethodHandlerResult processData(MethodHandlerInput input);
    }


    public static class MethodHandlerInput {
        private RawTransaction rawTransaction;
        private String smartContractAddress;
        private String recipient;
        private BigInteger value;

        public MethodHandlerInput(RawTransaction rawTransaction, String smartContractAddress) {
            this.rawTransaction = rawTransaction;
            this.smartContractAddress = smartContractAddress;
        }

        public boolean hasRawData() {
            return rawTransaction != null && rawTransaction.getData() != null;
        }

        public RawTransaction getRawTransaction() {
            return rawTransaction;
        }

        public void setRawTransaction(RawTransaction rawTransaction) {
            this.rawTransaction = rawTransaction;
        }

        public String getSmartContractAddress() {
            return smartContractAddress;
        }

        public void setSmartContractAddress(String smartContractAddress) {
            this.smartContractAddress = smartContractAddress;
        }

        public String getRecipient() {
            if (!hasRawData()) {
                return null;
            }
            if (isSmartContract()) {
                recipient = rawTransaction.getData().substring(34, 74);
            } else {
                recipient = rawTransaction.getTo();
            }
            return recipient;
        }

        public BigInteger getValue() {
            if (!hasRawData()) {
                return null;
            }
            if (isSmartContract()) {
                value = new BigInteger(rawTransaction.getData().substring(74), 16);
            } else {
                value = rawTransaction.getValue();
            }
            return value;
        }

        public boolean isSmartContract() {
            String rawRecipient = lowercaseHash(rawTransaction.getTo());
            return rawRecipient != null
                && smartContractAddress != null
                && lowercaseHash(rawRecipient).equals(lowercaseHash(smartContractAddress));
        }

        @Override public String toString() {
            return "MethodHandlerInput{" +
                "rawTransaction=" + rawTransaction +
                ", smartContractAddress='" + smartContractAddress + '\'' +
                ", recipient='" + recipient + '\'' +
                ", value=" + value +
                '}';
        }
    }


    public static class MethodHandlerResult {
        public static final String DEFAULT_DATA = "{\"type\":\"transfer\",\"description\":\"Transfer coins\"}";
        private String transactionType;
        private String extraData;
        private BigInteger value;

        public MethodHandlerResult(String transactionType, String extraData, BigInteger value) {
            this.transactionType = transactionType;
            this.extraData = extraData;
            this.value = value;
        }

        public String getTransactionType() {
            return transactionType;
        }

        public void setTransactionType(String transactionType) {
            this.transactionType = transactionType;
        }

        public String getExtraData() {
            return extraData;
        }

        public void setExtraData(String extraData) {
            this.extraData = extraData;
        }

        public BigInteger getValue() {
            return value;
        }

        public void setValue(BigInteger value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "MethodHandlerResult{" +
                "transactionType='" + transactionType + '\'' +
                ", extraData='" + extraData + '\'' +
                ", value=" + value +
                '}';
        }
    }

    public MethodHandlerResult execute(MethodHandlerInput input) {
        if (contractMethodHandlers == null || contractMethodHandlers.isEmpty()) {
            return DEFAULT_RESULT;
        }

        String rawData = lowercaseHash(input.getRawTransaction().getData());
        Map.Entry<String, String> handlerFound = contractMethodHandlers
            .entrySet()
            .stream()
            .filter(entry -> {
                String key = lowercaseHash(entry.getKey());
                return rawData.startsWith(key);
            })
            .findFirst()
            .orElse(null);

        if (handlerFound == null) {
            return DEFAULT_RESULT;
        }

        LOG.info("handler: {}", handlerFound);
        String className = handlerFound.getValue();
        Class<ContractMethodHandler> handlerClass;
        try {
            handlerClass = (Class<ContractMethodHandler>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load smart contract handler class: " + className, e);
        }

        CompletableFuture<MethodHandlerResult> handler = CompletableFuture.supplyAsync(() -> {
            ContractMethodHandler contractMethodHandler;
            try {
                contractMethodHandler = handlerClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new RuntimeException(
                    "Unable to instantiate smart contract handler: " + className, e);
            }
            return contractMethodHandler.processData(input);
        });

        try {
            return handler.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to execute method handler: " + className, e);
        }
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
    }

}
