package io.liquichain.api.handler;

import static io.liquichain.api.rpc.EthApiScript.EthApiConstants.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.meveo.model.customEntities.EthereumMethod;
import org.meveo.service.script.Script;
import org.meveo.admin.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.liquichain.api.rpc.EthApiUtils.*;

public class EthereumMethodExecutor extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(EthereumMethodExecutor.class);

    private final Map<String, EthereumMethod> ethereumMethods;

    public EthereumMethodExecutor(Map<String, EthereumMethod> ethereumMethods) {
        super();
        this.ethereumMethods = ethereumMethods;
    }

    public interface EthereumMethodHandler {
        String processRequest(String requestId, Map<String, Object> parameters);
    }

    public String execute(String requestId, Map<String, Object> parameters) {
        try {
            Object methodName = parameters.get("method");
            EthereumMethod ethereumMethod = ethereumMethods.get(methodName);
            String className = ethereumMethod.getMethodHandler();
            Class<EthereumMethodHandler> handlerClass;
            try {
                handlerClass = (Class<EthereumMethodHandler>) Class.forName(className);
                LOG.info("class: {} was loaded.", handlerClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Unable to load ethereum method handler class: " + className, e);
            }

            EthereumMethodHandler ethereumMethodHandler;
            try {
                ethereumMethodHandler = handlerClass.getDeclaredConstructor().newInstance();
                LOG.info("handler class instantiated.");
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new RuntimeException("Unable to instantiate ethereum method handler: " + className, e);
            }
            return ethereumMethodHandler.processRequest(requestId, parameters);
        } catch (Exception e) {
            return createErrorResponse(requestId, INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
    }

}
