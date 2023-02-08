package io.liquichain.api.handler;

import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.RawTransaction;

public class ContractMethodExecutor extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(ContractMethodExecutor.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, String> contractMethodHandlers;
    private final String abi;

    public ContractMethodExecutor(Map<String, String> contractMethodHandlers, String abi) {
        super();
        this.contractMethodHandlers = contractMethodHandlers;
        this.abi = abi;
        loadAbi();
    }

    public static <T> T convert(String data) {
        T value = null;
        try {
            value = mapper.readValue(data, new com.fasterxml.jackson.core.type.TypeReference<T>() {
            });
        } catch (Exception e) {
            LOG.error("Failed to parse data: {}", data, e);
        }
        return value;
    }

    private void loadAbi() {
        LOG.info("ABI loaded: {}", abi);
        List<ContractFunction> contractFunction = convert(abi);
        LOG.info("Extracted contract details: {}", contractFunction);
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
//        MethodHandlerResult processData(MethodHandlerInput input);
    }

//    public MethodHandlerResult execute(MethodHandlerInput input) {
//        if (contractMethodHandlers == null || contractMethodHandlers.isEmpty()) {
//            return DEFAULT_RESULT;
//        }
//
//        String rawData = lowercaseHash(input.getRawTransaction().getData());
//        Map.Entry<String, String> handlerFound = contractMethodHandlers
//            .entrySet()
//            .stream()
//            .filter(entry -> {
//                String key = lowercaseHash(entry.getKey());
//                return rawData.startsWith(key);
//            })
//            .findFirst()
//            .orElse(null);
//
//        if (handlerFound == null) {
//            return DEFAULT_RESULT;
//        }
//
//        LOG.info("handler: {}", handlerFound);
//        String className = handlerFound.getValue();
//        Class<ContractMethodHandler> handlerClass;
//        try {
//            handlerClass = (Class<ContractMethodHandler>) Class.forName(className);
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException("Unable to load smart contract handler class: " + className, e);
//        }
//
//        CompletableFuture<MethodHandlerResult> handler = CompletableFuture.supplyAsync(() -> {
//            ContractMethodHandler contractMethodHandler;
//            try {
//                contractMethodHandler = handlerClass.getDeclaredConstructor().newInstance();
//            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
//                     InvocationTargetException e) {
//                throw new RuntimeException(
//                    "Unable to instantiate smart contract handler: " + className, e);
//            }
//            return contractMethodHandler.processData(input);
//        });
//
//        try {
//            return handler.get();
//        } catch (InterruptedException | ExecutionException e) {
//            throw new RuntimeException("Failed to execute method handler: " + className, e);
//        }
//    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
    }

}


class ContractFunction {
    String name;
    String stateMutability;
    String type;
    List<ContractFunctionParameter> inputs;
    List<ContractFunctionParameter> outputs;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStateMutability() {
        return stateMutability;
    }

    public void setStateMutability(String stateMutability) {
        this.stateMutability = stateMutability;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<ContractFunctionParameter> getInputs() {
        return inputs;
    }

    public void setInputs(List<ContractFunctionParameter> inputs) {
        this.inputs = inputs;
    }

    public List<ContractFunctionParameter> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<ContractFunctionParameter> outputs) {
        this.outputs = outputs;
    }

    @Override public String toString() {
        return "ContractFunction{" +
            "name='" + name + '\'' +
            ", stateMutability='" + stateMutability + '\'' +
            ", type='" + type + '\'' +
            ", inputs=" + inputs +
            ", outputs=" + outputs +
            '}';
    }
}


class ContractFunctionParameter {
    String name;
    String type;
    String internalType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getInternalType() {
        return internalType;
    }

    public void setInternalType(String internalType) {
        this.internalType = internalType;
    }

    @Override public String toString() {
        return "ContractFunctionParameter{" +
            "name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", internalType='" + internalType + '\'' +
            '}';
    }
}
