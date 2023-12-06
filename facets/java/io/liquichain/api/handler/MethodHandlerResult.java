package io.liquichain.api.handler;

import org.meveo.model.customEntities.Transaction;
import org.meveo.service.script.Script;
import org.meveo.service.script.ScriptInstanceService;

import io.liquichain.api.rpc.EthApiUtils;

public class MethodHandlerResult extends Script {

    private final ScriptInstanceService scriptInstanceService = getCDIBean(ScriptInstanceService.class);
    private final EthApiUtils ethApiUtils = (EthApiUtils) scriptInstanceService.getExecutionEngine(
            EthApiUtils.class.getName(), null);

    private String transactionType;
    private String extraData;
    private String recipient;
    private String value;
    private Transaction transaction;

    public MethodHandlerResult(Transaction transaction) {
        this.transaction = transaction;
    }

    public MethodHandlerResult(String transactionType, String extraData) {
        this(transactionType, extraData, null, null);
    }

    public MethodHandlerResult(String transactionType, String extraData, String recipient, String value) {
        this.transactionType = transactionType;
        this.extraData = extraData;
        this.recipient = recipient;
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

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public String toString() {
        return "MethodHandlerResult{" +
                "transactionType='" + transactionType + '\'' +
                ", extraData='" + extraData + '\'' +
                ", value=" + value +
                ", transaction=" + ethApiUtils.toJson(transaction) +
                '}';
    }

}
