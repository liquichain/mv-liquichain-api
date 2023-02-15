package org.meveo.model.customEntities;

import org.meveo.model.CustomEntity;
import java.util.List;
import org.meveo.model.persistence.DBStorageType;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class EthereumMethod implements CustomEntity {

    public EthereumMethod() {
    }

    public EthereumMethod(String uuid) {
        this.uuid = uuid;
    }

    private String uuid;

    @JsonIgnore()
    private DBStorageType storages;

    private String method;

    private String methodHandler;

    @Override()
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public DBStorageType getStorages() {
        return storages;
    }

    public void setStorages(DBStorageType storages) {
        this.storages = storages;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getMethodHandler() {
        return methodHandler;
    }

    public void setMethodHandler(String methodHandler) {
        this.methodHandler = methodHandler;
    }

    @Override()
    public String getCetCode() {
        return "EthereumMethod";
    }
}
