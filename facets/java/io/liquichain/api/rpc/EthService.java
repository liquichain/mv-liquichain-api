package io.liquichain.api.rpc;

import static io.liquichain.api.rpc.EthApiScript.EthApiConstants.INTERNAL_ERROR;
import static io.liquichain.api.rpc.EthApiScript.EthApiConstants.PROXY_REQUEST_ERROR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;

import org.apache.commons.lang3.StringUtils;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthService extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(EthService.class);

    public static final String IS_SUCCESS = "isSuccess";
    public static final String IS_NULL = "isNull";

    private static final int SLEEP_DURATION = 1000;
    private static final int ATTEMPTS = 40;

    private static final int CONNECTION_POOL_SIZE = 50;
    private static final int MAX_POOLED_PER_ROUTE = 5;
    private static final long CONNECTION_TTL = 5;

    private final Client client = new ResteasyClientBuilder()
            .connectionPoolSize(CONNECTION_POOL_SIZE)
            .maxPooledPerRoute(MAX_POOLED_PER_ROUTE)
            .connectionTTL(CONNECTION_TTL, TimeUnit.SECONDS)
            .build();

    private final ScriptInstanceService scriptInstanceService = getCDIBean(ScriptInstanceService.class);
    private final EthApiUtils ethApiUtils = (EthApiUtils) scriptInstanceService.getExecutionEngine("EthApiUtils", null);
    private final ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
    private final ParamBean config = paramBeanFactory.getInstance();
    private final String BESU_API_URL = config.getProperty("besu.api.url", "https://testnet.liquichain.io/rpc");

    public CompletableFuture<String> callEthJsonRpc(String requestId, Map<String, Object> parameters) {
        Object id = parameters.get("id");
        Object jsonRpcVersion = parameters.get("jsonrpc");
        Object method = parameters.get("method");
        Object params = parameters.get("params");

        String body = "{" +
                "  \"id\": " + ethApiUtils.formatId(id) + "," +
                "  \"jsonrpc\": \"" + jsonRpcVersion + "\"," +
                "  \"method\": \"" + method + "\"," +
                "  \"params\": " + ethApiUtils.toJson(params) +
                "}";

        return CompletableFuture.supplyAsync(() -> {
            String result;
            Response response = null;
            LOG.debug("callEthJsonRpc body: {}", body);
            try {
                response = client.target(BESU_API_URL)
                                 .request(MediaType.APPLICATION_JSON)
                                 .post(Entity.json(body));
                result = response.readEntity(String.class);
            } catch (Exception e) {
                LOG.error(PROXY_REQUEST_ERROR, e);
                return ethApiUtils.createErrorResponse(requestId, INTERNAL_ERROR, PROXY_REQUEST_ERROR);
            } finally {
                if (response != null) {
                    response.close();
                }
            }

            LOG.debug("callEthJsonRpc result: {}", result);
            return result;
        });
    }

    private Map<String, Boolean> retrieveTransactionReceipt(String requestId, String hash,
            Map<String, Object> parameters) {
        LOG.debug("received hash: {}", hash);
        Map<String, Object> receiptParams = new HashMap<>() {{
            put("id", parameters.get("id"));
            put("jsonrpc", parameters.get("jsonrpc"));
            put("method", "eth_getTransactionReceipt");
            put("params", List.of(hash));
        }};
        LOG.debug("transaction receipt parameters: {}", ethApiUtils.toJson(receiptParams));

        String receiptResult = null;
        try {
            receiptResult = callEthJsonRpc(requestId, receiptParams).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        LOG.debug("transaction receipt result: {}", receiptResult);

        Map<String, Object> receiptResultMap = ethApiUtils.convert(receiptResult);
        Object errorMessage = receiptResultMap.get("error");
        boolean hasError = errorMessage != null && StringUtils.isNotEmpty(errorMessage.toString());
        if (hasError) {
            throw new RuntimeException(errorMessage.toString());
        }

        Object resultObject = receiptResultMap.get("result");
        if (resultObject == null) {
            return Map.of(IS_SUCCESS, false, IS_NULL, true);
        }

        Map<String, Object> resultMap = (Map<String, Object>) resultObject;
        Object resultError = resultMap.get("error");
        String transactionError = resultError != null ? resultError.toString() : null;
        if (StringUtils.isNotEmpty(transactionError)) {
            throw new RuntimeException(transactionError);
        }

        boolean success = "0x1".equals(resultMap.get("status"));
        return Map.of(IS_SUCCESS, success, IS_NULL, false);
    }

    public Map<String, Boolean> waitForTransactionReceipt(String requestId, String hash,
            Map<String, Object> parameters) {
        if (hash != null) {
            Map<String, Boolean> transactionReceipt = retrieveTransactionReceipt(requestId, hash, parameters);
            for (int attempt = ATTEMPTS; attempt > 0; attempt--) {
                if (transactionReceipt.get(IS_NULL)) {
                    try {
                        Thread.sleep(SLEEP_DURATION);
                        transactionReceipt = retrieveTransactionReceipt(requestId, hash, parameters);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Encountered error while delaying thread.", e);
                    }
                } else {
                    break;
                }
            }
            return transactionReceipt;
        } else {
            throw new RuntimeException("No transaction hash provided.");
        }
    }

}