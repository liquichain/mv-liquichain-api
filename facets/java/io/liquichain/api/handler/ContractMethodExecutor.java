package io.liquichain.api.handler;

import java.security.SignatureException;
import java.util.*;
import java.util.stream.Collectors;

import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;

import io.liquichain.api.rpc.EthApiUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.protocol.core.methods.response.AbiDefinition;

public class ContractMethodExecutor extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(ContractMethodExecutor.class);
    private static final Gson gson = new Gson();

    private final ScriptInstanceService scriptInstanceService = getCDIBean(ScriptInstanceService.class);
    private final EthApiUtils ethApiUtils = (EthApiUtils) scriptInstanceService.getExecutionEngine(
            EthApiUtils.class.getName(), null);

    private Map<String, String> contractMethodHandlers;
    private Map<String, ContractFunctionSignature> functionSignatures;

    public void init(String abi, Map<String, String> handlers) {
        this.contractMethodHandlers = new HashMap<>();
        handlers.forEach((key, value) -> contractMethodHandlers.put(ethApiUtils.lowercaseHex(key), value));
        List<AbiDefinition> abiDefinitions = gson.fromJson(abi, new TypeToken<List<AbiDefinition>>() {}.getType());
        this.functionSignatures = abiDefinitions
                .stream()
                .filter(abiDefinition -> "function".equals(abiDefinition.getType()))
                .map(ContractFunctionSignature::new)
                .peek(signature -> LOG.info("signature: {}", ethApiUtils.toJson(signature)))
                .collect(Collectors.toMap(ContractFunctionSignature::getSignature, signature -> signature));
    }

    public interface ContractMethodHandler {
        MethodHandlerResult processData(MethodHandlerInput input, Map<String, Object> parameters);
    }

    public MethodHandlerResult execute(MethodHandlerInput input) {
        RawTransaction rawTransaction = input.getRawTransaction();
        String rawData = rawTransaction.getData();
        String normalizedData = ethApiUtils.lowercaseHex(rawData);

        if (contractMethodHandlers == null || contractMethodHandlers.isEmpty()) {
            return parseSmartContractResult(rawData);
        }

        Map.Entry<String, String> handler = contractMethodHandlers
                .entrySet()
                .stream()
                .filter(entry -> normalizedData.startsWith(entry.getKey()))
                .findFirst()
                .orElse(null);

        if (handler == null) {
            return parseSmartContractResult(rawData);
        }

        LOG.info("handler: {}", handler);

        String className = handler.getValue();
        Class<ContractMethodHandler> handlerClass;
        try {
            handlerClass = (Class<ContractMethodHandler>) Class.forName(className);
            LOG.info("class: {} was loaded.", handlerClass);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load smart contract handler class: " + className, e);
        }

        ContractFunctionSignature functionSignature = functionSignatures.get(handler.getKey());
        Map<String, Object> parameters = functionSignature.parseParameters(rawData);
        SignedRawTransaction signedTransaction = (SignedRawTransaction) rawTransaction;
        try {
            parameters.put("senderWallet", signedTransaction.getFrom());
        } catch (SignatureException e) {
            throw new RuntimeException("Failed to retrieve sender wallet address.", e);
        }

        ContractMethodHandler contractMethodHandler = (ContractMethodHandler) scriptInstanceService.getExecutionEngine(
                handlerClass.getName(), null);
        LOG.info("handler class instantiated.");
        return contractMethodHandler.processData(input, parameters);
    }

    private MethodHandlerResult parseSmartContractResult(String rawData) {
        String transactionData = ethApiUtils.lowercaseHex(rawData);
        ContractFunctionSignature functionSignature = this.functionSignatures
                .values()
                .stream()
                .filter(value -> transactionData.startsWith(value.getSignature()))
                .findFirst()
                .orElse(new ContractFunctionSignature());

        LOG.info("function signature: {}", ethApiUtils.toJson(functionSignature));
        String type = functionSignature.getName();
        Map<String, Object> parameters = functionSignature.parseParameters(rawData);
        String description = functionSignature.getFunctionDefinition();
        Map<String, Object> dataMap = new LinkedHashMap<>();
        dataMap.put("type", type);
        dataMap.put("description", description);
        dataMap.put("parameters", parameters);
        return new MethodHandlerResult(type, ethApiUtils.toJson(dataMap));
    }
}

class ContractFunctionSignature {
    private static final Logger LOG = LoggerFactory.getLogger(ContractFunctionSignature.class);

    private String signature;
    private String name;
    private List<TypeReference<Type>> inputParameters;
    private List<String> parameterNames;
    private String functionDefinition;
    private Map<String, Object> parameters;

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

    public ContractFunctionSignature() {
    }

    public ContractFunctionSignature(AbiDefinition abiDefinition) {
        this.name = abiDefinition.getName();
        List<AbiDefinition.NamedType> inputs = abiDefinition.getInputs();
        List<String> parameterTypes = new ArrayList<>();
        this.parameterNames = new ArrayList<>();

        if (!inputs.isEmpty()) {
            this.inputParameters = new ArrayList<>();
            inputs.forEach(input -> {
                this.inputParameters.add(this.parseParameterType(input));
                this.parameterNames.add(input.getName());
                parameterTypes.add(input.getType());
            });
        }

        this.functionDefinition = String.format("%s(%s)", name, String.join(", ", parameterNames));
        String baseDefinition = String.format("%s(%s)", name, String.join(",", parameterTypes));
        String fullSignature = Hash.sha3String(baseDefinition);
        this.signature = fullSignature.substring(0, 10).toLowerCase();
    }

    public String getSignature() {
        return signature;
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

    public List<String> getParameterNames() {
        return parameterNames;
    }

    public String getFunctionDefinition() {
        return functionDefinition;
    }

    public Map<String, Object> parseParameters(String rawData) {
        if (parameters == null || parameters.isEmpty()) {
            parameters = mapFunctionParameters(this, rawData);
        }
        return parameters;
    }

    private Map<String, Object> mapFunctionParameters(ContractFunctionSignature functionSignature, String rawData) {
        List<TypeReference<Type>> inputs = functionSignature.getInputParameters();
        Map<String, Object> parameters = new LinkedHashMap<>();

        if (!inputs.isEmpty()) {
            List<String> names = functionSignature.getParameterNames();
            List<Type> values = FunctionReturnDecoder.decode(rawData.substring(8), inputs);
            for (int index = 0; index < values.size(); index++) {
                Type type = values.get(index);
                String name = names.get(index);
                parameters.put(name, type.getValue());
            }
        }
        return parameters;
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

    @Override
    public String toString() {
        return "ContractFunctionSignature{" +
                "signature='" + signature + '\'' +
                ", name='" + name + '\'' +
                ", inputParameters=" + inputParameters +
                ", parameterNames=" + parameterNames +
                ", functionDefinition='" + functionDefinition + '\'' +
                '}';
    }

}
