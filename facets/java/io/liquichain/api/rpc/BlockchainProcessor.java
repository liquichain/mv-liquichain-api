package io.liquichain.api.rpc;

import org.meveo.service.script.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.web3j.crypto.*;

public abstract class BlockchainProcessor extends Script {
    private static final Logger LOG = LoggerFactory.getLogger(BlockchainProcessor.class);
    private static final Map<String, Object[]> TRANSACTION_HOOKS = new HashMap<>();

    protected String result;

    public String getResult(){
        return this.result;
    };

    public static boolean addTransactionHook(String regex, Script script) {
        boolean isHookAdded = true;
        String key = regex + ":" + script.getClass().getName();
        LOG.info("addTransactionHook key: {}", key);
        isHookAdded = !TRANSACTION_HOOKS.containsKey(key);
        if (isHookAdded == true) {
            Pattern pattern = Pattern.compile(regex);
            TRANSACTION_HOOKS.put(key, new Object[]{pattern, script});
        }
        return isHookAdded;
    }

    public static void processTransactionHooks(String transactionHash, SignedRawTransaction transaction) {
        try {
            String data = new String(new BigInteger(transaction.getData(), 16).toByteArray());
            LOG.info("try matching {} hooks", TRANSACTION_HOOKS.size());
            TRANSACTION_HOOKS.forEach((String key, Object[] hook) -> {
                LOG.info("try hook {} on {}", key, data);
                Pattern pattern = (Pattern) hook[0];
                Script script = (Script) hook[1];
                Matcher matcher = pattern.matcher(data);
                if (matcher.find()) {
                    LOG.info(" hook {} matched", key);
                    Map<String, Object> context = new HashMap<>();
                    context.put("transaction", transaction);
                    context.put("transactionHash", transactionHash);
                    context.put("matcher", matcher);
                    try {
                        script.execute(context);
                        if (context.containsKey("result")) {
                            LOG.info(" hook result:{} ", context.get("result"));
                        }
                    } catch (Exception e) {
                        LOG.error("error while invoking transaction hook {}", script, e);
                    }
                } else {
                    LOG.info(" hook {} matched", key);
                }
            });
            if (data.contains("orderId")) {
                LOG.info("detected orderId:{}", data);
            }
        } catch (Exception ex) {
            LOG.info("error while detecting order:{}", ex);
        }
    }
}
