package io.liquichain.api.handler;

import java.util.Map;

import org.meveo.admin.exception.BusinessException;
import org.meveo.service.script.Script;

public class MethodHandlerResult extends Script {
    private String transactionType;
    private String extraData;
    private String recipient;
    private String value;

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

    @Override
    public String toString() {
        return "MethodHandlerResult{" +
            "transactionType='" + transactionType + '\'' +
            ", extraData='" + extraData + '\'' +
            ", value=" + value +
            '}';
    }

    @Override
    public void execute(Map<String, Object> parameters) throws BusinessException {
        super.execute(parameters);
    }
}
