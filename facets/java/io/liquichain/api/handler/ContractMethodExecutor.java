package io.liquichain.api.handler;

import static io.liquichain.api.rpc.EthApiUtils.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.core.methods.response.AbiDefinition;

public class ContractMethodExecutor extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(ContractMethodExecutor.class);
    private static final Gson gson = new Gson();

    private final Map<String, String> contractMethodHandlers;
    private final Map<String, ContractFunctionSignature> functionSignatures;

    public ContractMethodExecutor(Map<String, String> handlers, String abi) {
        super();
        contractMethodHandlers = new HashMap<>();
        handlers.forEach((key, value) -> contractMethodHandlers.put(lowercaseHex(key), value));
        List<AbiDefinition> abiDefinitions = gson.fromJson(abi, new TypeToken<List<AbiDefinition>>() {}.getType());

        this.functionSignatures = abiDefinitions
            .stream()
            .filter(abiDefinition -> "function".equals(abiDefinition.getType()))
            .map(ContractFunctionSignature::new)
            .filter(functionSignature -> contractMethodHandlers.containsKey(functionSignature.getSignature()))
            .collect(Collectors.toMap(signature -> signature.getSignature(), signature -> signature));
    }

    public interface ContractMethodHandler {
        MethodHandlerResult processData(MethodHandlerInput input, Map<String, Object> parameters);
    }

    public MethodHandlerResult execute(MethodHandlerInput input) {
        if (contractMethodHandlers == null || contractMethodHandlers.isEmpty()) {
            return null;
        }

        RawTransaction rawTransaction = input.getRawTransaction();
        String rawData = rawTransaction.getData();
        String normalizedData = lowercaseHex(rawData);
        Map.Entry<String, String> handler = contractMethodHandlers
            .entrySet()
            .stream()
            .filter(entry -> {
                String key = lowercaseHex(entry.getKey());
                return normalizedData.startsWith(key);
            })
            .findFirst()
            .orElse(null);

        if (handler == null) {
            return null;
        }
        LOG.info("handler: {}", handler);

        String className = handler.getValue();
        Class<ContractMethodHandler> handlerClass;
        try {
            handlerClass = (Class<ContractMethodHandler>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load smart contract handler class: " + className, e);
        }

        ContractFunctionSignature functionSignature = functionSignatures.get(handler.getKey());
        List<TypeReference<Type>> inputs = functionSignature.getInputParameters();
        Map<String, Object> parameters = new HashMap<>();
        if (!inputs.isEmpty()) {
            List<String> names = functionSignature.getParameterNames();
            List<Type> values = FunctionReturnDecoder.decode(rawData.substring(8), inputs);
            for (int index = 0; index < values.size(); index++) {
                Type type = values.get(index);
                String name = names.get(index);
                parameters.put(name, type.getValue());
            }
        }

        CompletableFuture<MethodHandlerResult> asyncHandler = CompletableFuture.supplyAsync(() -> {
            ContractMethodHandler contractMethodHandler;
            try {
                contractMethodHandler = handlerClass.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new RuntimeException(
                    "Unable to instantiate smart contract handler: " + className, e);
            }
            return contractMethodHandler.processData(input, parameters);
        });

        try {
            return asyncHandler.get();
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

    private final String fullSignature;
    private String signature;
    private String name;
    private List<TypeReference<Type>> inputParameters;
    private List<String> parameterNames;

    private TypeReference<Type> parseParameterType(AbiDefinition.NamedType contractFunctionParameter) {
        String type = contractFunctionParameter.getType();
        boolean isTuple = "tuple".equals(type);
        boolean isTupleArray = "tuple[]".equals(type);
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

    public ContractFunctionSignature(AbiDefinition abiDefinition) {
        this.name = abiDefinition.getName();
        List<AbiDefinition.NamedType> inputs = abiDefinition.getInputs();
        List<String> parameterTypes = new ArrayList<>();
        if (!inputs.isEmpty()) {
            this.inputParameters = new ArrayList<>();
            this.parameterNames = new ArrayList<>();
            inputs.forEach(input -> {
                this.inputParameters.add(this.parseParameterType(input));
                this.parameterNames.add(input.getName());
                parameterTypes.add(input.getType());
            });
        }
        String functionDefinition = String.format("%s(%s)", name, String.join(",", parameterTypes));
        this.fullSignature = Hash.sha3String(functionDefinition);
        this.signature = fullSignature.substring(0, 10).toLowerCase();
    }

    public String getSignature() {
        return signature;
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

    public List<TypeReference<Type>> getInputParameters() {
        return inputParameters;
    }

    public void setInputParameters(List<TypeReference<Type>> inputParameters) {
        this.inputParameters = inputParameters;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    public void setParameterNames(List<String> parameterNames) {
        this.parameterNames = parameterNames;
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
            "fullSignature='" + fullSignature + '\'' +
            ", signature='" + signature + '\'' +
            ", name='" + name + '\'' +
            ", inputParameters=" + inputParameters +
            '}';
    }
}
