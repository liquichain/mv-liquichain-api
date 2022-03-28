package io.liquichain.api.rpc;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.exception.EntityDoesNotExistsException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.commons.utils.ParamBean;
import org.meveo.commons.utils.ParamBeanFactory;
import org.meveo.model.customEntities.LiquichainApp;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.customEntities.VerifiedEmail;
import org.meveo.model.customEntities.VerifiedPhoneNumber;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.web3j.crypto.*;

import io.liquichain.core.BlockForgerScript;


public class EthApiScript extends Script {
  private static final Logger LOG = LoggerFactory.getLogger(EthApiScript.class);
  private static final Map<String, Object[]> TRANSACTION_HOOKS = new HashMap<>();

  private static final String NOT_IMPLEMENTED_ERROR = "Feature not yet implemented";
  private static final String CREATE_WALLET_ERROR = "Failed to create wallet";
  private static final String UPDATE_WALLET_ERROR = "Failed to update wallet";
  private static final String UNKNOWN_WALLET_ERROR = "Unknown wallet";
  private static final String UNKNOWN_APPLICATION_ERROR = "Unknown application";
  private static final String WALLET_EXISTS_ERROR = "Wallet already exists";
  private static final String EMAIL_EXISTS_ERROR = "Email address: %s, already exists";
  private static final String PHONE_NUMBER_EXISTS_ERROR = "Phone number: %s, already exists";
  private static final String TRANSACTION_EXISTS_ERROR = "Transaction already exists: {}";
  private static final String INVALID_REQUEST = "-32600";
  private static final String INTERNAL_ERROR = "-32603";
  private static final String RESOURCE_NOT_FOUND = "-32001";
  private static final String TRANSACTION_REJECTED = "-32003";
  private static final String METHOD_NOT_FOUND = "-32601";

  private static final String SAMPLE_BLOCK = "{" + "\"difficulty\":\"0x5\","
      + "\"extraData\":\"0xd58301090083626f7286676f312e3133856c696e75780000000000000000000021c9effaf6549e725463c7877ddebe9a2916e03228624e4bfd1e3f811da792772b54d9e4eb793c54afb4a29f014846736755043e4778999046d0577c6e57e72100\","
      + "\"gasLimit\":\"0xe984c2\"," + "\"gasUsed\":\"0x0\","
      + "\"hash\":\"0xaa14340feb15e26bc354bb839b2aa41cc7984676249c155ac5e4d281a8d08809\","
      + "\"logsBloom\":\"0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000\","
      + "\"miner\":\"0x0000000000000000000000000000000000000000\","
      + "\"mixHash\":\"0x0000000000000000000000000000000000000000000000000000000000000000\","
      + "\"nonce\":\"0x0000000000000000\"," + "\"number\":\"0x1b4\","
      + "\"parentHash\":\"0xc8ccb81f484a428a3a1669d611f55f880b362b612f726711947d98f5bc5af573\","
      + "\"receiptsRoot\":\"0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421\","
      + "\"sha3Uncles\":\"0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347\","
      + "\"size\":\"0x260\","
      + "\"stateRoot\":\"0xffcb834d62706995e9e7bf10cc9a9e42a82fea998d59b3a5cfad8975dbfe3f87\","
      + "\"timestamp\":\"0x5ed9a43f\"," + "\"totalDifficulty\":\"0x881\"," + "\"transactions\":["
      + "],"
      + "\"transactionsRoot\":\"0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421\","
      + "\"uncles\":[  " + "]}";


  private final CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
  private final RepositoryService repositoryService = getCDIBean(RepositoryService.class);
  private final Repository defaultRepo = repositoryService.findDefaultRepository();
  private ParamBeanFactory paramBeanFactory = getCDIBean(ParamBeanFactory.class);
  private ParamBean config = paramBeanFactory.getInstance();

  private String APP_NAME = config.getProperty("eth.api.appname", "licoin");

  private String result;

  @Override
  public void execute(Map<String, Object> parameters) throws BusinessException {
    String method = "" + parameters.get("method");
    LOG.info("json rpc: {}, parameters:{}", method, parameters);
    String requestId = "" + parameters.get("id");
    switch (method) {
      case "eth_call":
        result = EthApiUtils.createResponse(requestId, "0x");
        break;
      case "eth_chainId":
        result = EthApiUtils.createResponse(requestId, "0x4c");
        break;
      case "web3_clientVersion":
        result = EthApiUtils.createResponse(requestId, "liquichainCentral");
        break;
      case "net_version":
        result = EthApiUtils.createResponse(requestId, "7");
        break;
      case "eth_blockNumber":
        result = EthApiUtils.createResponse(requestId, "0x" + Long.toHexString(BlockForgerScript.blockHeight));
        break;
      case "eth_getBalance":
        result = getBalance(requestId, parameters);
        break;
      case "eth_getTransactionCount":
        result = getTransactionCount(requestId, parameters);
        break;
      case "eth_getBlockByNumber":
        result = EthApiUtils.createResponse(requestId, SAMPLE_BLOCK);
        break;
      case "eth_estimateGas":
        result = EthApiUtils.createResponse(requestId, "0x0");
        break;
      case "eth_gasPrice":
        result = EthApiUtils.createResponse(requestId, "0x0");
        break;
      case "eth_getCode":
        result = getCode(requestId, parameters);
        break;
      case "eth_sendRawTransaction":
        result = sendRawTransaction(requestId, parameters);
        break;
      case "eth_getTransactionByHash":
        result = getTransactionByHash(requestId, parameters);
        break;
      case "wallet_creation":
        result = createWallet(requestId, parameters);
        break;
      case "wallet_update":
        result = updateWallet(requestId, parameters);
        break;
      case "wallet_info":
        result = getWalletInfo(requestId, parameters);
        break;
      case "wallet_report":
        result = EthApiUtils.createResponse(requestId, "wallet reported");
        break;
      default:
        result = EthApiUtils.createErrorResponse(requestId, METHOD_NOT_FOUND, NOT_IMPLEMENTED_ERROR);
        break;
    }
  }

  public String getResult() {
    return result;
  }

  public static boolean addTransactionHook(String regex, Script script) {
    String key = regex + ":" + script.getClass().getName();
    LOG.info("addTransactionHook key:{}", key);
    boolean result = true;
    result = !TRANSACTION_HOOKS.containsKey(key);
    if (result == true) {
      Pattern pattern = Pattern.compile(regex);
      TRANSACTION_HOOKS.put(key, new Object[] {pattern, script});
    }
    return result;
  }

  private void processTransactionHooks(SignedRawTransaction transaction, String transactionHash) {
    try {
      String data = new String(new BigInteger(transaction.getData(), 16).toByteArray());
      LOG.info("try matching {} hooks", TRANSACTION_HOOKS.size());
      TRANSACTION_HOOKS.forEach((String key, Object[] tuple) -> {
        LOG.info("try hook {} on {}", key, data);
        Pattern pattern = (Pattern) tuple[0];
        Script script = (Script) tuple[1];
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

  private String getTransactionByHash(String requestId, Map<String, Object> parameters) {
    List<String> params = (List<String>) parameters.get("params");
    String hash = EthApiUtils.retrieveHash(params, 0);
    LOG.info("lookup transaction hexHash={}", hash);

    try {
      Transaction transaction =
          crossStorageApi.find(defaultRepo, Transaction.class).by("hexHash", hash).getResult();
      String transactionDetails = "{\n";
      transactionDetails += "\"blockHash\": \"0x" + transaction.getBlockHash() + "\",\n";
      transactionDetails +=
          "\"blockNumber\": \"" + EthApiUtils.toBigHex(transaction.getBlockNumber()) + "\",\n";
      transactionDetails += "\"from\": \"0x" + transaction.getFromHexHash() + "\",\n";
      transactionDetails += "\"gas\": \"" + EthApiUtils.toBigHex(transaction.getGasLimit()) + "\",\n";
      transactionDetails += "\"gasPrice\": \"" + EthApiUtils.toBigHex(transaction.getGasPrice()) + "\",\n";
      transactionDetails += "\"hash\": \"" + hash + "\",\n";
      transactionDetails += "\"input\": \"\",\n";
      transactionDetails += "\"nonce\": \"" + EthApiUtils.toBigHex(transaction.getNonce()) + "\",\n";
      if (transaction.getData() != null) {
        if (EthApiUtils.isJSONValid(transaction.getData())) {
          transactionDetails += "\"data\": " + transaction.getData() + ",\n";
        } else {
          transactionDetails += "\"data\": \"" + transaction.getData() + "\",\n";
        }
      }
      transactionDetails += "\"r\": \"" + transaction.getR() + "\",\n";
      transactionDetails += "\"s\": \"" + transaction.getS() + "\",\n";
      transactionDetails += "\"to\": \"0x" + transaction.getToHexHash() + "\",\n";
      transactionDetails +=
          "\"transactionIndex\": \"0x" + EthApiUtils.toBigHex(transaction.getTransactionIndex() + "") + "\",";
      transactionDetails += "\"v\": \"" + transaction.getV() + "\",";
      transactionDetails += "\"value\": \"" + EthApiUtils.toBigHex(transaction.getValue()) + "\"\n";
      transactionDetails += "}";
      LOG.info("res={}" + transactionDetails);
      return EthApiUtils.createResponse(requestId, transactionDetails);
    } catch (Exception e) {
      e.printStackTrace();
      return EthApiUtils.createErrorResponse(requestId, RESOURCE_NOT_FOUND, "Resource not found");
    }
  }

  private String sendRawTransaction(String requestId, Map<String, Object> parameters) {
    List<String> params = (List<String>) parameters.get("params");
    String transactionData = params.get(0);
    String transactionHash = EthApiUtils.normalizeHash(Hash.sha3(transactionData));
    Transaction existingTransaction = null;
    result = "0x0";
    try {
      existingTransaction = crossStorageApi
          .find(defaultRepo, Transaction.class).by("hexHash", transactionHash).getResult();
    } catch (Exception e) {
      // do nothing, we want transaction to be unique
    }
    if (existingTransaction != null) {
      return EthApiUtils.createErrorResponse(requestId, INVALID_REQUEST, TRANSACTION_EXISTS_ERROR);
    }

    RawTransaction rawTransaction = TransactionDecoder.decode(transactionData);

    if (rawTransaction instanceof SignedRawTransaction) {
      SignedRawTransaction signedResult = (SignedRawTransaction) rawTransaction;
      Sign.SignatureData signatureData = signedResult.getSignatureData();
      try {
        LOG.info("from:{} chainedId:{}", signedResult.getFrom(), signedResult.getChainId());
        Transaction transaction = new Transaction();
        transaction.setHexHash(transactionHash);
        transaction.setFromHexHash(EthApiUtils.normalizeHash(signedResult.getFrom()));
        transaction.setToHexHash(EthApiUtils.normalizeHash(rawTransaction.getTo()));
        transaction.setNonce("" + rawTransaction.getNonce());
        transaction.setGasPrice("" + rawTransaction.getGasPrice());
        transaction.setGasLimit("" + rawTransaction.getGasLimit());
        transaction.setValue("" + rawTransaction.getValue());
        if (rawTransaction.getData() == null || rawTransaction.getData().isEmpty()) {
          transaction.setData("{\"type\":\"transfer\"}");
        } else {
          transaction.setData("" + rawTransaction.getData());
        }
        transaction.setSignedHash(transactionData);
        transaction.setCreationDate(java.time.Instant.now());
        transaction.setV(EthApiUtils.toHex(signatureData.getV()));
        transaction.setS(EthApiUtils.toHex(signatureData.getS()));
        transaction.setR(EthApiUtils.toHex(signatureData.getR()));
        LOG.info("transaction:{}", transaction);
        String uuid = crossStorageApi.createOrUpdate(defaultRepo, transaction);
        transferValue(transaction, rawTransaction.getValue());
        result = "0x" + transactionHash;
        LOG.info("created transaction with uuid:{}", uuid);
        if (rawTransaction.getData() != null && rawTransaction.getData().length() > 0) {
          processTransactionHooks(signedResult, transaction.getHexHash());
        }
      } catch (Exception e) {
        return EthApiUtils.createErrorResponse(requestId, TRANSACTION_REJECTED, e.getMessage());
      }
    }
    return EthApiUtils.createResponse(requestId, result);
  }

  private void transferValue(Transaction transaction, BigInteger value) throws BusinessException {
    String message = "transfer error";
    try {
      message = "cannot find origin wallet";
      Wallet originWallet =
          crossStorageApi.find(defaultRepo, transaction.getFromHexHash(), Wallet.class);
      message = "cannot find destination wallet";
      crossStorageApi.find(defaultRepo, transaction.getToHexHash(), Wallet.class);
      message = "insufficient balance";
      BigInteger originBalance = new BigInteger(originWallet.getBalance());
      LOG.info("originWallet 0x{} old balance:{}", transaction.getFromHexHash(),
          originWallet.getBalance());
      if (value.compareTo(originBalance) <= 0) {
        BlockForgerScript.addTransaction(transaction);
      } else {
        throw new BusinessException("insufficient balance");
      }
    } catch (Exception e) {
      throw new BusinessException(message);
    }
  }

  private String getTransactionCount(String requestId, Map<String, Object> parameters) {
    List<String> params = (List<String>) parameters.get("params");
    String transactionHash = EthApiUtils.retrieveHash(params, 0);
    try {
      int nbTransaction = (crossStorageApi.find(defaultRepo, Transaction.class)
          .by("fromHexHash", transactionHash)
          .getResults()).size();
      return EthApiUtils.createResponse(requestId, EthApiUtils.toBigHex(nbTransaction + ""));
    } catch (Exception e) {
      return EthApiUtils.createResponse(requestId, "0x0");
    }
  }

  private String getCode(String requestId, Map<String, Object> parameters) {
    List<String> params = (List<String>) parameters.get("params");
    String address = EthApiUtils.retrieveHash(params, 0);
    try {
      Wallet wallet =
          crossStorageApi.find(defaultRepo, address, Wallet.class);
      LOG.info("getCode wallet.application.uuid={}", wallet.getApplication().getUuid());
      return EthApiUtils.createResponse(requestId, "0x" + wallet.getApplication().getUuid());
    } catch (Exception e) {
      LOG.error("Wallet address {} not found", address, e);
      return EthApiUtils.createErrorResponse(requestId, RESOURCE_NOT_FOUND, "Address not found");
    }
  }

  private String getBalance(String requestId, Map<String, Object> parameters) {
    List<String> params = (List<String>) parameters.get("params");
    String address = EthApiUtils.retrieveHash(params, 0);
    try {
      Wallet wallet = crossStorageApi.find(defaultRepo, address, Wallet.class);
      return EthApiUtils.createResponse(requestId, EthApiUtils.toBigHex(wallet.getBalance()));
    } catch (Exception e) {

      return EthApiUtils.createErrorResponse(requestId, RESOURCE_NOT_FOUND, "Resource not found");
    }
  }

  public String createWallet(String requestId, Map<String, Object> parameters) {
    List<String> params = (List<String>) parameters.get("params");
    String name = params.get(0);
    String walletHash = EthApiUtils.retrieveHash(params, 1);
    String accountHash = EthApiUtils.retrieveHash(params, 2);
    String publicInfo = params.get(3);

    Wallet wallet = null;
    try {
      wallet = crossStorageApi.find(defaultRepo, walletHash, Wallet.class);
    } catch (EntityDoesNotExistsException e) {
      // do nothing, we want wallet to be unique
    }
    if (wallet != null) {
      return EthApiUtils.createErrorResponse(requestId, INVALID_REQUEST, WALLET_EXISTS_ERROR);
    } else {
      wallet = new Wallet();
    }
    LiquichainApp app = null;
    try {
      app =
          crossStorageApi.find(defaultRepo, LiquichainApp.class).by("name", APP_NAME).getResult();
    } catch (Exception e) {
      LOG.error(UNKNOWN_APPLICATION_ERROR, e);
      return EthApiUtils.createErrorResponse(requestId, INVALID_REQUEST, UNKNOWN_APPLICATION_ERROR);
    }
    wallet.setUuid(walletHash);
    wallet.setName(name);
    wallet.setAccountHash(accountHash);
    wallet.setPublicInfo(publicInfo);
    wallet.setBalance("0");
    wallet.setApplication(app);
    try {
      String savedHash = crossStorageApi.createOrUpdate(defaultRepo, wallet);
      return EthApiUtils.createResponse(requestId, "0x" + savedHash);
    } catch (Exception e) {
      LOG.error(CREATE_WALLET_ERROR, e);
      return EthApiUtils.createErrorResponse(requestId, INTERNAL_ERROR, CREATE_WALLET_ERROR);
    }
  }

  public String updateWallet(String requestId, Map<String, Object> parameters) {
    List<String> params = (List<String>) parameters.get("params");
    String name = params.get(0);
    String walletHash = EthApiUtils.retrieveHash(params, 1);
    String publicInfo = params.get(2);

    Wallet wallet = null;
    try {
      wallet = crossStorageApi.find(defaultRepo, walletHash.toLowerCase(), Wallet.class);
    } catch (EntityDoesNotExistsException e) {
      LOG.error(UNKNOWN_WALLET_ERROR, e);
      wallet = null;
    }
    if (wallet == null) {
      return EthApiUtils.createErrorResponse(requestId, INVALID_REQUEST, UNKNOWN_WALLET_ERROR);
    }
    wallet.setName(name);
    wallet.setPublicInfo(publicInfo);
    try {
      crossStorageApi.createOrUpdate(defaultRepo, wallet);
      return EthApiUtils.createResponse(requestId, name);
    } catch (Exception e) {
      LOG.error(UPDATE_WALLET_ERROR, e);
      return EthApiUtils.createErrorResponse(requestId, INTERNAL_ERROR, UPDATE_WALLET_ERROR);
    }
  }

  public String getWalletInfo(String requestId, Map<String, Object> parameters) {
    List<String> params = (List<String>) parameters.get("params");
    String walletHash = EthApiUtils.retrieveHash(params, 0);
    Wallet wallet = null;
    try {
      wallet = crossStorageApi.find(defaultRepo, walletHash.toLowerCase(), Wallet.class);
    } catch (Exception e) {
      LOG.error(UNKNOWN_WALLET_ERROR, e);
      wallet = null;
    }
    if (wallet == null) {
      return EthApiUtils.createErrorResponse(requestId, RESOURCE_NOT_FOUND, UNKNOWN_WALLET_ERROR);
    }
    String response = "{\n";
    response += "\"name\":\"" + wallet.getName() + "\"";
    if (wallet.getPublicInfo() != null) {
      response += ",\n\"publicInfo\":" + wallet.getPublicInfo() + "";
    }
    response += "\n}";
    return EthApiUtils.createResponse(requestId, response);
  }
}
