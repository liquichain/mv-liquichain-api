package io.liquichain.api.rpc;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.meveo.admin.exception.BusinessException;
import org.meveo.api.persistence.CrossStorageApi;
import org.meveo.model.customEntities.LiquichainApp;
import org.meveo.model.customEntities.Transaction;
import org.meveo.model.customEntities.Wallet;
import org.meveo.model.storage.Repository;
import org.meveo.service.script.Script;
import org.meveo.service.storage.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.crypto.TransactionDecoder;
import io.liquichain.core.BlockForgerScript;

public class EthApiScript extends Script {

  private static final Logger log = LoggerFactory.getLogger(EthApiScript.class);

  private long chainId = 76;

  private String result;

  private int networkId = 7;

  private long blockHeight = 1662295;

  private BigInteger balance = new BigInteger("999965000000000000000");

  private String exampleBlock = "{" + "\"difficulty\":\"0x5\","
      + "\"extraData\":\"0xd58301090083626f7286676f312e3133856c696e75780000000000000000000021c9effaf6549e725463c7877ddebe9a2916e03228624e4bfd1e3f811da792772b54d9e4eb793c54afb4a29f014846736755043e4778999046d0577c6e57e72100\","
      + "\"gasLimit\":\"0xe984c2\"," + "\"gasUsed\":\"0x0\","
      + "\"hash\":\"0xaa14340feb15e26bc354bb839b2aa41cc7984676249c155ac5e4d281a8d08809\","
      + "\"logsBloom\":\"0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000\","
      + "\"miner\":\"0x0000000000000000000000000000000000000000\","
      + "\"mixHash\":\"0x0000000000000000000000000000000000000000000000000000000000000000\","
      + "\"nonce\":\"0x0000000000000000\"," + "\"number\":\"0x1b4\","
      + "\"parentHash\":\"0xc8ccb81f484a428a3a1669d611f55f880b362b612f726711947d98f5bc5af573\","
      + "\"receiptsRoot\":\"0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421\","
      + "\"sha3Uncles\":\"0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347\"," + "\"size\":\"0x260\","
      + "\"stateRoot\":\"0xffcb834d62706995e9e7bf10cc9a9e42a82fea998d59b3a5cfad8975dbfe3f87\","
      + "\"timestamp\":\"0x5ed9a43f\"," + "\"totalDifficulty\":\"0x881\"," + "\"transactions\":[" + "],"
      + "\"transactionsRoot\":\"0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421\","
      + "\"uncles\":[  " + "]}";

  private CrossStorageApi crossStorageApi = getCDIBean(CrossStorageApi.class);
  // {
  // crossStorageApi = CDI.current().select(CrossStorageApi.class).get();
  // }
  private RepositoryService repositoryService = getCDIBean(RepositoryService.class);
  // {
  // repositoryService = CDI.current().select(RepositoryService.class).get();
  // }
  private Repository defaultRepo = repositoryService.findDefaultRepository();

  //private String projectId;

  public String getResult() {
    return result;
  }

  static private Map<String, Object[]> transactionHooks = new HashMap<>();

  public static boolean addTransactionHook(String regex, Script script) {
    String key = regex+":"+script.getClass().getName();
    log.info("addTransactionHook key:{}",key);
    boolean result = true;
    result =!transactionHooks.containsKey(key);
    if(result == true){
      Pattern pattern = Pattern.compile(regex);
      transactionHooks.put(key, new Object[]{pattern, script});
    }
    return result;
  }

  private void processTransactionHooks(SignedRawTransaction transaction,String transactionHash) {
    try {
      String data = new String(new BigInteger(transaction.getData(), 16).toByteArray());
      log.info("try matching {} hooks",transactionHooks.size());
      transactionHooks.forEach((String key,Object[] tuple) -> {
        log.info("try hook {} on {}",key,data);
        Pattern pattern = (Pattern)tuple[0];
        Script script = (Script)tuple[1];
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
          log.info(" hook {} matched",key);
          Map<String, Object> context = new HashMap<>();
          context.put("transaction", transaction);
          context.put("transactionHash",transactionHash);
          context.put("matcher", matcher);
          try {
            script.execute(context);
            if(context.containsKey("result")){
          		log.info(" hook result:{} ",context.get("result"));
            }
          } catch (Exception e) {
            log.error("error while invoking transaction hook {}", script, e);
          }
        } else { 
          log.info(" hook {} matched",key);
        }
      });
      if (data.contains("orderId")) {
        log.info("detected orderId:{}", data);
      }
    } catch (Exception ex) {
      log.info("error while detecting order:{}", ex);
    }
  }

  @Override
  public void execute(Map<String, Object> parameters) throws BusinessException {
    // log.info("projectId : {}", projectId);
    String method = "" + parameters.get("method");
    log.info("json rpc: {}, parameters:{}", method, parameters);
    String requestId = "" + parameters.get("id");
    switch (method) {
      case "eth_call":
        log.info("params={}", parameters.get("params"));
        ArrayList<Object> paramscall = (ArrayList<Object>) parameters.get("params");
        Object hashcall = paramscall.get(0);
        result = createResponse(requestId, "0x");
        break;
      case "eth_chainId":
        result = createResponse(requestId, "0x4c");
        break;
      case "web3_clientVersion":
        result = createResponse(requestId, "liquichainCentral");
        break;
      case "net_version":
        result = createResponse(requestId, "" + networkId);
        break;
      case "eth_blockNumber":
        blockHeight = BlockForgerScript.blockHeight;
        result = createResponse(requestId, "0x" + Long.toHexString(blockHeight));
        break;
      case "eth_getBalance":
        log.info("params={}", parameters.get("params"));
        ArrayList<String> params = (ArrayList<String>) parameters.get("params");
        String hash = params.get(0).toLowerCase();
        if (hash.startsWith("0x")) {
          hash = hash.substring(2);
        }
        result = getBalance(requestId, hash);
        break;
      case "eth_getTransactionCount":
        log.info("params={}", parameters.get("params"));
        ArrayList<String> paramsc = (ArrayList<String>) parameters.get("params");
        String hashc = paramsc.get(0).toLowerCase();
        if (hashc.startsWith("0x")) {
          hashc = hashc.substring(2);
        }
        result = getTransactionCount(requestId, hashc);
        break;
      case "eth_getBlockByNumber":
        result = createResponse(requestId, exampleBlock);
        break;
      case "eth_estimateGas":
        result = createResponse(requestId, "0x0");
        break;
      case "eth_gasPrice":
        result = createResponse(requestId, "0x0");
        break;
      case "eth_getCode":
        ArrayList<String> paramsco = (ArrayList<String>) parameters.get("params");
        String hashco = paramsco.get(0).toLowerCase();
        result = getCode(requestId, hashco);
        break;
      case "eth_sendRawTransaction":
        log.info("received transaction : params={}", parameters.get("params"));
        ArrayList<String> params2 = (ArrayList<String>) parameters.get("params");
        String transacEncoded = params2.get(0);
        result = processTransaction(requestId, transacEncoded);
        break;
      case "eth_getTransactionByHash":
        ArrayList<String> params3 = (ArrayList<String>) parameters.get("params");
        String hash2 = params3.get(0).toLowerCase();
        result = getTransactionByHash(requestId, hash2);
        break;
      case "wallet_creation":
        ArrayList<String> params4 = (ArrayList<String>) parameters.get("params");
        String name = params4.get(0);
        String walletHash = params4.get(1).toLowerCase();
        if (walletHash.startsWith("0x")) {
          walletHash = walletHash.substring(2);
        }
        String accountHash = params4.get(2).toLowerCase();
        if (accountHash.startsWith("0x")) {
          accountHash = accountHash.substring(2);
        }
        String walletPublicInfo = params4.get(3);
        result = createWallet(requestId, "licoin", name, walletHash, accountHash, walletPublicInfo);
        break;
      case "wallet_update":
        ArrayList<String> params5 = (ArrayList<String>) parameters.get("params");
        String uname = params5.get(0);
        String uwalletHash = params5.get(1).toLowerCase();
        if (uwalletHash.startsWith("0x")) {
          uwalletHash = uwalletHash.substring(2);
        }
        String walletPublicInfo2 = params5.get(2);
        String signature = params5.get(3);
        result = updateWallet(requestId, "licoin", uname, uwalletHash, walletPublicInfo2, signature);
        break;
      case "wallet_info":
        ArrayList<String> params6 = (ArrayList<String>) parameters.get("params");
        String iwalletHash = params6.get(0).toLowerCase();
        if (iwalletHash.startsWith("0x")) {
          iwalletHash = iwalletHash.substring(2);
        }
        result = getWalletInfo(requestId, "licoin", iwalletHash);
        break;
      case "wallet_report":
        ArrayList<String> params7 = (ArrayList<String>) parameters.get("params");
        String reportedWalletHash = params7.get(0).toLowerCase();
        String signature2 = params7.get(1);
        result = createResponse(requestId, "wallet reported");
        break;
    }
  }

  public static String hex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte aByte : bytes) {
      result.append(String.format("%02x", aByte));
    }
    return result.toString().toLowerCase();
  }

  private String toBigHex(String i) {
    return "0x" + new BigInteger(i).toString(16);
  }

  private String getTransactionByHash(String requestId, String hash) {
    try {
      log.info("lookup transaction hexHash={}", hash);

      if (hash.startsWith("0x")) {
        hash = hash.substring(2);
      }
      Transaction transac = crossStorageApi.find(defaultRepo, Transaction.class).by("hexHash", hash).getResult();
      String result = "{\n";
      result += "\"blockHash\": \"0x" + transac.getBlockHash() + "\",\n";
      result += "\"blockNumber\": \"" + toBigHex(transac.getBlockNumber()) + "\",\n";
      result += "\"from\": \"0x" + transac.getFromHexHash() + "\",\n";
      result += "\"gas\": \"0x" + toBigHex(transac.getGasLimit()) + "\",\n";
      result += "\"gasPrice\": \"0x" + toBigHex(transac.getGasPrice()) + "\",\n";
      result += "\"hash\": \"" + hash + "\",\n";
      result += "\"input\": \"\",\n";
      result += "\"nonce\": \"" + toBigHex(transac.getNonce()) + "\",\n";
      result += "\"data\": \"" + ((transac.getData()==null)?"":transac.getData()) + "\",\n";
      result += "\"r\": \"" + transac.getR() + "\",\n";
      result += "\"s\": \"" + transac.getS() + "\",\n";
      result += "\"to\": \"0x" + transac.getToHexHash() + "\",\n";
      result += "\"transactionIndex\": \"0x" + toBigHex(transac.getTransactionIndex() + "") + "\",";
      result += "\"v\": \"" + transac.getV() + "\",";
      result += "\"value\": \"" + toBigHex(transac.getValue()) + "\"\n";
      result += "}";
      log.info("res={}" + result);
      return createResponse(requestId, result);
    } catch (Exception e) {
       e.printStackTrace();
      return createErrorResponse(requestId, "-32001", "Resource not found");
    }
  }

  private String processTransaction(String requestId, String hexTransactionData) {
    String result = "0x0";
    String hash = Hash.sha3(hexTransactionData).toLowerCase();
    Transaction existingTransaction = null;
    try {
      existingTransaction = crossStorageApi.find(defaultRepo, Transaction.class).by("hexHash", hash.substring(2))
          .getResult();
    } catch (Exception e) {
    }
    if (existingTransaction != null) {
      return createErrorResponse(requestId, "-32001", "transaction already exists hexHash:" + hash.substring(2));
    }
    RawTransaction t = TransactionDecoder.decode(hexTransactionData);
    log.info("nonce:{} to:{} , value:{}", t.getNonce(), t.getTo(), t.getValue());
    if (t instanceof SignedRawTransaction) {
      SignedRawTransaction signedResult = (SignedRawTransaction) t;
      signedResult.getData();
      Sign.SignatureData signatureData = signedResult.getSignatureData();
      // byte[] encodedTransaction = TransactionEncoder.encode(t);
      try {
        log.info("from:{} chainedId:{}", signedResult.getFrom(), signedResult.getChainId());
        Transaction transac = new Transaction();
        transac.setHexHash(hash.substring(2).toLowerCase());
        transac.setFromHexHash(signedResult.getFrom().substring(2).toLowerCase());
        transac.setToHexHash(t.getTo().substring(2).toLowerCase());
        transac.setNonce("" + t.getNonce());
        transac.setGasPrice("" + t.getGasPrice());
        transac.setGasLimit("" + t.getGasLimit());
        transac.setValue("" + t.getValue());
        if(t.getData()==null || t.getData().isEmpty()){
          transac.setData("{\"type\":\"transfer\"}");
        } else {
          transac.setData("" + t.getData());
        }
        transac.setSignedHash(hexTransactionData);
        transac.setCreationDate(java.time.Instant.now());
        transac.setV(hex(signatureData.getV()));
        transac.setS(hex(signatureData.getS()));
        transac.setR(hex(signatureData.getR()));
        log.info("transac:{}", transac);
        String uuid = crossStorageApi.createOrUpdate(defaultRepo, transac);
        transferValue(transac, t.getValue());
        result = hash;
        log.info("created transaction with uuid:{}", uuid);
        if (t.getData() != null && t.getData().length() > 0) {
          processTransactionHooks(signedResult,transac.getHexHash());
        }
      } catch (Exception e) {
        // e.printStackTrace();
        return createErrorResponse(requestId, "-32001", e.getMessage());
      }
    }
    return createResponse(requestId, result);
  }

  private String createResponse(String requestId, String result) {
    String res = "{\n";
    res += "  \"id\": " + requestId + ",\n";
    res += "  \"jsonrpc\": \"2.0\",\n";
    if (result.startsWith("{")) {
      res += "  \"result\": " + result + "\n";
    } else {
      res += "  \"result\": \"" + result + "\"\n";
    }
    res += "}";
    // log.info("res:{}", res);
    return res;
  }

  private void transferValue(Transaction transac, BigInteger value) throws Exception {
    String message = "transfer error";
    try {
      message = "cannot find origin wallet";
      Wallet originWallet = crossStorageApi.find(defaultRepo, transac.getFromHexHash(), Wallet.class);
      message = "cannot find destination wallet";
      Wallet destinationWallet = crossStorageApi.find(defaultRepo, transac.getToHexHash(), Wallet.class);
      message = "insufficient balance";
      BigInteger originBalance = new BigInteger(originWallet.getBalance());
      log.info("originWallet 0x{} old balance:{}", transac.getFromHexHash(), originWallet.getBalance());
      if (value.compareTo(originBalance) <= 0) {
        BlockForgerScript.addTransaction(transac);
      } else {
        throw new RuntimeException("insufficient balance");
      }
    } catch (Exception e) {
      throw new Exception(message);
    }
  }

  private String createErrorResponse(String requestId, String errorCode, String message) {
    String res = "{\n";
    res += "  \"id\": " + requestId + ",\n";
    res += "  \"jsonrpc\": \"2.0\",\n";
    res += "  \"error\": { \"code\" : " + errorCode + " , \"message\" : \"" + message + "\"}\n";
    res += "}";
    // log.info("err:{}", res);
    return res;
  }

  private String getTransactionCount(String requestId, String hash) {
    try {
      int nbTransaction = (crossStorageApi.find(defaultRepo, Transaction.class).by("fromHexHash", hash.toLowerCase())
          .getResults()).size();
      return createResponse(requestId, "0x" + new BigInteger(nbTransaction + "").toString(16));
    } catch (Exception e) {
      // e.printStackTrace();
      return createResponse(requestId, "0x0");
    }
  }

  private String getCode(String requestId, String hash) {
    try {
      Wallet wallet = crossStorageApi.find(defaultRepo, hash.substring(2).toLowerCase(), Wallet.class);
      log.info("getCode wallet.app.uuid={}", wallet.getApplication().getUuid());
      // LiquichainApp app = crossStorageApi.find(defaultRepo, LiquichainApp.class);
      return createResponse(requestId, "0x" + wallet.getApplication().getUuid());
    } catch (Exception e) {
      // e.printStackTrace();
      return createErrorResponse(requestId, "-32001", "Address not found");
    }
  }

  private String getBalance(String requestId, String hash) {
    try {
      Wallet wallet = crossStorageApi.find(defaultRepo, hash.toLowerCase(), Wallet.class);
      return createResponse(requestId, "0x" + new BigInteger(wallet.getBalance()).toString(16));
    } catch (Exception e) {
      // e.printStackTrace();
      return createErrorResponse(requestId, "-32001", "Resource not found");
    }
  }

  public String createWallet(String requestId, String appName, String name, String walletHash, String accountHash,
      String publicInfo) {
    Wallet wallet = null;
    try {
      wallet = crossStorageApi.find(defaultRepo, walletHash.toLowerCase(), Wallet.class);
    } catch (Exception e) {
    }
    if (wallet != null) {
      return createErrorResponse(requestId, "-32001", "Wallet already exists");
    } else {
      wallet = new Wallet();
    }
    try {
      LiquichainApp app = crossStorageApi.find(defaultRepo, LiquichainApp.class).by("name", appName).getResult();
      wallet.setUuid(walletHash.toLowerCase());
      wallet.setName(name);
      wallet.setAccountHash(accountHash.toLowerCase());
      wallet.setPublicInfo(publicInfo);
      wallet.setBalance("0");
      wallet.setApplication(app);
      crossStorageApi.createOrUpdate(defaultRepo, wallet);
      return createResponse(requestId, walletHash.toLowerCase());
    } catch (Exception ex) {
      return createErrorResponse(requestId, "-32001", "Cannot find application " + appName);
    }

  }

  public String updateWallet(String requestId, String appName, String name, String walletHash, String publicInfo,
      String signature) {
    Wallet wallet = null;
    try {
      wallet = crossStorageApi.find(defaultRepo, walletHash.toLowerCase(), Wallet.class);
    } catch (Exception e) {
    }
    if (wallet == null) {
      return createErrorResponse(requestId, "-32001", "Unkown Wallet");
    }
    try {
      wallet.setName(name);
      wallet.setPublicInfo(publicInfo);
      crossStorageApi.createOrUpdate(defaultRepo, wallet);
      return createResponse(requestId, name);
    } catch (Exception ex) {
      return createErrorResponse(requestId, "-32001", "Cannot update wallet " + ex.getMessage());
    }

  }

  public String getWalletInfo(String requestId, String appName, String walletHash) {
    Wallet wallet = null;
    try {
      wallet = crossStorageApi.find(defaultRepo, walletHash.toLowerCase(), Wallet.class);
    } catch (Exception e) {
    }
    if (wallet == null) {
      return createErrorResponse(requestId, "-32001", "Unkown Wallet");
    }
    String response = "{\n";
    response += "\"name\":\"" + wallet.getName() + "\"";
    if (wallet.getPublicInfo() != null) {
      response += ",\n\"publicInfo\":" + wallet.getPublicInfo() + "";
    }
    response += "\n}";
    return createResponse(requestId, response);
  }

 // public void setProjectId(String projectId) {
   // this.projectId = projectId;
  //}
}
