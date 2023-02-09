package io.liquichain.api.handler;

import static io.liquichain.api.rpc.EthApiUtils.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.TypeReference;
import org.web3j.crypto.Hash;
import org.web3j.protocol.core.methods.response.AbiDefinition;

public class ContractMethodExecutor extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(ContractMethodExecutor.class);

    private final Map<String, String> contractMethodHandlers;
    private final String abi;
    private final List<ContractFunctionSignature> functionSignatures;

    public ContractMethodExecutor(Map<String, String> contractMethodHandlers, String abi) {
        super();
        this.contractMethodHandlers = contractMethodHandlers;
        this.abi = abi;
        List<AbiDefinition> abiDefinitions = new Gson()
            .fromJson(abi, new TypeToken<List<AbiDefinition>>() {}.getType());
        LOG.info("Abi Definitions: {}", abiDefinitions);

//        this.functionSignatures = contractFunctions
//            .stream()
//            .filter(contractFunction -> "function".equals(contractFunction.getType()))
//            .map(ContractFunctionSignature::new)
//            .collect(Collectors.toList());
    }

    public interface ContractMethodHandler {
        MethodHandlerResult processData(MethodHandlerInput input);
    }

    public MethodHandlerResult execute(MethodHandlerInput input) {
        if (contractMethodHandlers == null || contractMethodHandlers.isEmpty()) {
            return null;
        }
        String rawData = lowercaseHex(input.getRawTransaction().getData());
        Map.Entry<String, String> handlerFound = contractMethodHandlers
            .entrySet()
            .stream()
            .filter(entry -> {
                String key = lowercaseHex(entry.getKey());
                return rawData.startsWith(key);
            })
            .findFirst()
            .orElse(null);

        if (handlerFound == null) {
            return null;
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


class ContractFunctionSignature {
    private static final Logger LOG = LoggerFactory.getLogger(ContractFunctionSignature.class);

    private String fullSignature;
    private String signature;
    private String name;
    private List<TypeReference> inputParameters;
    private List<TypeReference> outputParameters;

    private TypeReference parseParameterType(ContractFunctionParameter contractFunctionParameter) {
        String type = contractFunctionParameter.getType();
        LOG.info("{}", contractFunctionParameter);
        boolean isTuple = "tuple".equals(type);
        boolean isTupleArray = "tuple[]".equals(type);
        LOG.info("isTuple: {}", isTuple);
        if (isTuple || isTupleArray) {
            // convert tuple to DynamicStruct
            return null;
        } else {
            try {
                return TypeReference.makeTypeReference(type);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ContractFunctionSignature(ContractFunction contractFunction) {
        this.name = contractFunction.getName();
        List<ContractFunctionParameter> inputs = contractFunction.getInputs();
        List<ContractFunctionParameter> outputs = contractFunction.getOutputs();
        String functionParameters = inputs.stream()
                                          .map(ContractFunctionParameter::getType)
                                          .collect(Collectors.joining(","));
        String functionDefinition = String.format("%s(%s)", name, functionParameters);
        this.fullSignature = Hash.sha3String(functionDefinition);
        this.signature = fullSignature.substring(0, 10);

        this.inputParameters = inputs.stream()
                                     .map(this::parseParameterType)
                                     .collect(Collectors.toList());
        this.outputParameters = outputs.stream()
                                       .map(this::parseParameterType)
                                       .collect(Collectors.toList());
    }

    public String getSignature() {
        return lowercaseHex(signature).substring(10);
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TypeReference> getInputParameters() {
        return inputParameters;
    }

    public void setInputParameters(List<TypeReference> inputParameters) {
        this.inputParameters = inputParameters;
    }

    public List<TypeReference> getOutputParameters() {
        return outputParameters;
    }

    public void setOutputParameters(List<TypeReference> outputParameters) {
        this.outputParameters = outputParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ContractFunctionSignature that = (ContractFunctionSignature) o;
        return Objects.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signature);
    }

    @Override public String toString() {
        return "ContractFunctionSignature{" +
            "signature='" + signature + '\'' +
            ", name='" + name + '\'' +
            ", inputParameters=" + inputParameters +
            ", outputParameters=" + outputParameters +
            '}';
    }
}


class ContractFunction {
    private String name;
    private String stateMutability;
    private String type;
    private List<ContractFunctionParameter> inputs;
    private List<ContractFunctionParameter> outputs;

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

    @Override
    public String toString() {
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
    private String name;
    private String type;
    private String internalType;
    private List<ContractFunctionParameter> components;

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

    public List<ContractFunctionParameter> getComponents() {
        return components;
    }

    public void setComponents(List<ContractFunctionParameter> components) {
        this.components = components;
    }

    @Override
    public String toString() {
        return "ContractFunctionParameter{" +
            "name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", internalType='" + internalType + '\'' +
            ", components=" + components +
            '}';
    }
}
