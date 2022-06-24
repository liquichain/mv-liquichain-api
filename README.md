# Eth API Backend
The ETH API endpoint is at: **POST /meveo/rest/jsonrpc** and it is compatible with the following Ethereum API methods.

## Supported methods:
- [eth_accounts](#eth_accounts)
- [eth_blockNumber](#eth_blocknumber)
- [eth_call](#eth_call)
- [eth_chainId](#eth_chainid)
- [eth_coinbase](#eth_coinbase)
- [eth_estimateGas](#eth_estimategas)
- [eth_gasPrice](#eth_gasprice)
- [eth_getBalance](#eth_getbalance)
- [eth_getBlockByHash](#eth_getblockbyhash)
- [eth_getBlockByNumber](#eth_getblockbynumber)
- [eth_getBlockTransactionCountByHash](#eth_getblocktransactioncountbyhash)
- [eth_getBlockTransactionCountByNumber](#eth_getblocktransactioncountbynumber)
- [eth_getCode](#eth_getcode)
- [eth_getFilterChanges](#eth_getfilterchanges)
- [eth_getFilterLogs](#eth_getfilterlogs)
- [eth_getLogs](#eth_getlogs)
- [eth_getMinerDataByBlockHash](#eth_getminerdatabyblockhash)
- [eth_getMinerDataByBlockNumber](#eth_getminerdatabyblocknumber)
- [eth_getStorageAt](#eth_getstorageat)
- [eth_getTransactionByBlockHashAndIndex](#eth_gettransactionbyblockhashandindex)
- [eth_getTransactionByBlockNumberAndIndex](#eth_gettransactionbyblocknumberandindex)
- [eth_getTransactionByHash](#eth_gettransactionbyhash)
- [eth_getTransactionCount](#eth_gettransactioncount)
- [eth_getTransactionReceipt](#eth_gettransactionreceipt)
- [eth_getUncleByBlockHashAndIndex](#eth_getunclebyblockhashandindex)
- [eth_getUncleByBlockNumberAndIndex](#eth_getunclebyblocknumberandindex)
- [eth_getUncleCountByBlockHash](#eth_getunclecountbyblockhash)
- [eth_getUncleCountByBlockNumber](#eth_getunclecountbyblocknumber)
- [eth_hashrate](#eth_hashrate)
- [eth_mining](#eth_mining)
- [eth_newBlockFilter](#eth_newblockfilter)
- [eth_newFilter](#eth_newfilter)
- [eth_newPendingTransactionFilter](#eth_newpendingtransactionfilter)
- [eth_protocolVersion](#eth_protocolversion)
- [eth_sendRawTransaction](#eth_sendrawtransaction)
- [eth_submitHashrate](#eth_hashrate)
- [eth_syncing](#eth_syncing)
- [eth_uninstallFilter](#eth_uninstallfilter)

## Setup `meveo` and `keycloak` for keycloak user creation
- [Configure keycloak](#configure_keycloak)
- [Configure pasword rules in keycloak](#configure_password_rules_in_keycloak)
- [Update meveo settings](#update_meveo_settings)

## Wallet operations (not part of Ethereum API)
The wallet API endpoint is at: **POST /meveo/rest/wallet_jsonrpc** and it has the following methods.
- [wallet_creation](#wallet_creation)
- [wallet_update](#wallet_update)
- [wallet_info](#wallet_info)

## Not supported:
- eth_getProof
- eth_getWork
- eth_sendTransaction
- eth_submitWork

### eth_accounts
Returns a list of account addresses a client owns.
> **Note** - This is not implemented in the dev server (using database backend)

**Parameters**

None

**Returns**

`Array of data`: List of 20-byte account addresses owned by the client.

> **Note** - This method returns an empty object because the API doesn't support key management inside the client.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_accounts",
  "params": []
}
```
**Sample Response**
```json
{
  "id": 1,  
  "jsonrpc": "2.0",
  "result": []
}
```

### eth_blockNumber

Returns the index corresponding to the block number of the current chain head.

> **Note** - This returns a hardcoded value of 0x1 when using database backend

**Parameters**

None

**Returns**

`result` : QUANTITY - Hexadecimal integer representing the index corresponding to the block number of the current chain head.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_blockNumber",
  "params": []
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",  
  "result": "0x2377"
}
```

### eth_call

Invokes a contract function locally and does not change the state of the blockchain.

You can interact with contracts using [`eth_sendRawTransaction`](#eth_sendrawtransaction) or `eth_call`.
> **Note** - This returns a hard coded result `0x` when using database backend

**Parameters**

`OBJECT` - Transaction call object.

`QUANTITY`|`TAG` - Integer representing a block number or one of the string tags latest, earliest, or pending, as described in [Block Parameter](#block-parameter).

> **Note** - By default, `eth_call` does not fail if the sender account has an insufficient balance. This is done by setting the balance of the account to a large amount of ether. To enforce balance rules, set the strict parameter in the transaction call object to true.

**Returns**

`result` - `data` - Return value of the executed contract.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_call",
  "params": [
    {
      "to": "0x69498dd54bd25aa0c886cf1f8b8ae0856d55ff13",
      "value": "0x1"
    },
    "latest"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x"
}
```

### eth_chainId

Returns the chain ID.

**Parameters**

None

**Returns**

`result` : `quantity` - Chain ID, in hexadecimal.
> Note - this is hard coded to `0x4c`

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_chainId",
  "params": []
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",  
  "result": "0x4c"
}
```

### eth_coinbase

Returns the client coinbase address. The coinbase address is the account to pay mining rewards to.
> **Note** - This is not implemented when using database backend

**Parameters**

None

**Returns**

`result` : `data` - Coinbase address.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_coinbase",
  "params": []
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x5e2870e87bf1b6d2d231d4ecc808f5f642334986"
}
```

### eth_estimateGas

Returns an estimate of the gas required for a transaction to complete. The estimation process does not use gas and the transaction is not added to the blockchain. The resulting estimate can be greater than the amount of gas the transaction ends up using, for reasons including EVM mechanics and node performance.

The `eth_estimateGas` call does not send a transaction. You must call `eth_sendRawTransaction` to execute the transaction.

**Parameters**

The transaction call object parameters are the same as those for `eth_call` except for the `strict` parameter. If `strict` is set to `true`, the sender account balance is checked for value transfer and transaction fees. The default for `strict` is `false`.

For `eth_estimateGas`, all fields are optional because setting a gas limit is irrelevant to the estimation process (unlike transactions, in which gas limits apply).

`object` - Transaction call object.

**Returns**

result : quantity - Amount of gas used.
> **Note** - in dev server, this is hard coded to `0x0` while in staging server, this is by default set to `0x5208`

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_estimateGas",
  "params": [
    {
      "from": "0x687422eea2cb73b5d3e242ba5456b782919afc85",
      "to": "0xdd37f65db31c107f773e82a4f85c693058fef7a9",
      "value": "0x1"
    },
    "latest"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x5208"
}
```

### eth_gasPrice

Returns a percentile gas unit price for the most recent blocks, in Wei. By default, the last 100 blocks are examined and the 50th percentile gas unit price (that is, the median value) is returned.

If there are no blocks, by default it returns the minimum amount.  By default the return value is restricted to values between `1000 Wei` and `500 GWei`.

**Parameters**

None

**Returns**

`result` : `quantity` - Percentile gas unit price for the most recent blocks, in Wei, as a hexadecimal value.
> **Note** - the return value is hard coded to `0x0`

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_gasPrice",
  "params": []
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x0"
}
```

### eth_getBalance

Returns the account balance of the specified address.

**Parameters**

`DATA` - 20-byte account address from which to retrieve the balance.

`QUANTITY`|`TAG` - Integer representing a block number or one of the string tags `latest`, `earliest`, or `pending`, as described in [Block Parameter](#block-parameter).

**Returns**

`result` : QUANTITY - Current balance, in wei, as a hexadecimal value.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getBalance",
  "params": [
    "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73",
    "latest"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x1cfe56f3795885980000"
}
```

### eth_getBlockByHash

Returns information about the block by hash.
> **Note** - This is not implemented when using database backend

**Parameters**

`DATA` - 32-byte hash of a block.

`Boolean` - If `true`, returns the full transaction objects; if `false`, returns the transaction hashes.

**Returns**

`result` : OBJECT - Block object , or `null` when there is no block.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getBlockByHash",
  "params": [
    "0xaf5526fcb88b2f0d163c9a78ee678bf95b20115dc3d4e2b7b1f5fc4a308724a0",
    false
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": {
    "number": "0x68b3",
    "hash": "0xd5f1812548be429cbdc6376b29611fc49e06f1359758c4ceaaa3b393e2239f9c",
    "mixHash": "0x24900fb3da77674a861c428429dce0762707ecb6052325bbd9b3c64e74b5af9d",
    "parentHash": "0x1f68ac259155e2f38211ddad0f0a15394d55417b185a93923e2abe71bb7a4d6d",
    "nonce": "0x378da40ff335b070",
    "sha3Uncles": "0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347",
    "logsBloom": "0x00000000000000100000004080000000000500000000000000020000100000000800001000000004000001000000000000000800040010000020100000000400000010000000000000000040000000000000040000000000000000000000000000000400002400000000000000000000000000000004000004000000000000840000000800000080010004000000001000000800000000000000000000000000000000000800000000000040000000020000000000000000000800000400000000000000000000000600000400000000002000000000000000000000004000000000000000100000000000000000000000000000000000040000900010000000",
    "transactionsRoot": "0x4d0c8e91e16bdff538c03211c5c73632ed054d00a7e210c0eb25146c20048126",
    "stateRoot": "0x91309efa7e42c1f137f31fe9edbe88ae087e6620d0d59031324da3e2f4f93233",
    "receiptsRoot": "0x68461ab700003503a305083630a8fb8d14927238f0bc8b6b3d246c0c64f21f4a",
    "miner": "0xb42b6c4a95406c78ff892d270ad20b22642e102d",
    "difficulty": "0x66e619a",
    "totalDifficulty": "0x1e875d746ae",
    "extraData": "0xd583010502846765746885676f312e37856c696e7578",
    "size": "0x334",
    "gasLimit": "0x47e7c4",
    "gasUsed": "0x37993",
    "timestamp": "0x5835c54d",
    "uncles": [],
    "transactions": [
      "0xa0807e117a8dd124ab949f460f08c36c72b710188f01609595223b325e58e0fc",
      "0xeae6d797af50cb62a596ec3939114d63967c374fa57de9bc0f4e2b576ed6639d"
    ]
  }
}
```

### eth_getBlockByNumber

Returns information about a block by block number.

**Parameters**

`QUANTITY|TAG` - Integer representing a block number or one of the string tags `latest`, `earliest`, or `pending`, as described in [Block Parameter](#block-parameter).

`Boolean` - If `true`, returns the full transaction objects; if false, returns only the hashes of the transactions.

**Returns**

`result` : OBJECT - Block object , or `null` when there is no block.

**Sample Request**
```json
{
  "id": 1
  "jsonrpc": "2.0",
  "method": "eth_getBlockByNumber",
  "params": [
    "0xF",
    true
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": {
    "number": "0x68b3",
    "hash": "0xd5f1812548be429cbdc6376b29611fc49e06f1359758c4ceaaa3b393e2239f9c",
    "mixHash": "0x24900fb3da77674a861c428429dce0762707ecb6052325bbd9b3c64e74b5af9d",
    "parentHash": "0x1f68ac259155e2f38211ddad0f0a15394d55417b185a93923e2abe71bb7a4d6d",
    "nonce": "0x378da40ff335b070",
    "sha3Uncles": "0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347",
    "logsBloom": "0x00000000000000100000004080000000000500000000000000020000100000000800001000000004000001000000000000000800040010000020100000000400000010000000000000000040000000000000040000000000000000000000000000000400002400000000000000000000000000000004000004000000000000840000000800000080010004000000001000000800000000000000000000000000000000000800000000000040000000020000000000000000000800000400000000000000000000000600000400000000002000000000000000000000004000000000000000100000000000000000000000000000000000040000900010000000",
    "transactionsRoot": "0x4d0c8e91e16bdff538c03211c5c73632ed054d00a7e210c0eb25146c20048126",
    "stateRoot": "0x91309efa7e42c1f137f31fe9edbe88ae087e6620d0d59031324da3e2f4f93233",
    "receiptsRoot": "0x68461ab700003503a305083630a8fb8d14927238f0bc8b6b3d246c0c64f21f4a",
    "miner": "0xb42b6c4a95406c78ff892d270ad20b22642e102d",
    "difficulty": "0x66e619a",
    "totalDifficulty": "0x1e875d746ae",
    "extraData": "0xd583010502846765746885676f312e37856c696e7578",
    "size": "0x334",
    "gasLimit": "0x47e7c4",
    "gasUsed": "0x37993",
    "timestamp": "0x5835c54d",
    "uncles": [],
    "transactions": [
      "0xa0807e117a8dd124ab949f460f08c36c72b710188f01609595223b325e58e0fc",
      "0xeae6d797af50cb62a596ec3939114d63967c374fa57de9bc0f4e2b576ed6639d"
    ]
  }
}
```

### eth_getBlockTransactionCountByHash

Returns the number of transactions in the block matching the given block hash.
> **Note** - This is not implemented when using database backend

**Parameters**

`data` - 32-byte block hash.

**Returns**

`result` : `quantity` - Integer representing the number of transactions in the specified block.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getBlockTransactionCountByHash",
  "params": [
    "0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x1"
}
```

### eth_getBlockTransactionCountByNumber

Returns the number of transactions in a block matching the specified block number.
> **Note** - This is not implemented when using database backend

**Parameters**

`QUANTITY`|`TAG` - Integer representing a block number or one of the string tags `latest`, `earliest`, or `pending`, as described in [Block Parameter](#block-parameter).

**Returns**

`result` : QUANTITY - Integer representing the number of transactions in the specified block.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getBlockTransactionCountByNumber",
  "params": [
    "0xe8"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x8"
}
```

### eth_getCode

Returns the code of the smart contract at the specified address. Besu stores compiled smart contract code as a hexadecimal value.

**Parameters**

`DATA` - 20-byte contract address.

`QUANTITY`|`TAG` - Integer representing a block number or one of the string tags `latest`, `earliest`, or `pending`, as described in [Block Parameter](#block-parameter).

**Returns**
`result` : DATA - Code stored at the specified address.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getCode",
  "params": [
    "0xa50a51c09a5c451c52bb714527e1974b686d8e77",
    "latest"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x60806040526004361060485763ffffffff7c01000000000000000000000000000000000000000000000000000000006000350416633fa4f2458114604d57806355241077146071575b600080fd5b348015605857600080fd5b50605f6088565b60408051918252519081900360200190f35b348015607c57600080fd5b506086600435608e565b005b60005481565b60008190556040805182815290517f199cd93e851e4c78c437891155e2112093f8f15394aa89dab09e38d6ca0727879181900360200190a1505600a165627a7a723058209d8929142720a69bde2ab3bfa2da6217674b984899b62753979743c0470a2ea70029"
}
```

### eth_getFilterChanges

Polls the specified filter and returns an array of changes that have occurred since the last poll.
> **Note** - This is not implemented when using database backend

**Parameters**

`data` - Filter ID.

**Returns**

`result` : `Array of Object` - If nothing changed since the last poll, an empty list. Otherwise:

- For filters created with [`eth_newBlockFilter`](#eth_newblockfilter), returns block hashes.
- For filters created with [`eth_newPendingTransactionFilter`](#eth_newpendingtransactionfilter), returns transaction hashes.
- For filters created with [`eth_newFilter`](#eth_newfilter), returns log objects.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getFilterChanges",
  "params": [
    "0xf8bf5598d9e04fbe84523d42640b9b0e"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": [
    "0x1e977049b6db09362da09491bee3949d9362080ce3f4fc19721196d508580d46",
    "0xa3abc4b9a4e497fd58dc59cdff52e9bb5609136bcd499e760798aa92802769be"
  ]
}
```

### eth_getFilterLogs

Returns an array of logs for the specified filter.
> **Note** - This is not implemented when using database backend

> Note - `eth_getFilterLogs` is only used for filters created with [`eth_newFilter`](#eth_newfilter). To specify a filter object and get logs without creating a filter, use [`eth_getLogs`](#eth_getlogs) .

**Parameters**

`data` - Filter ID.

**Returns**

`array` - Log objects.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getFilterLogs",
  "params": [
    "0x5ace5de3985749b6a1b2b0d3f3e1fb69"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": [
    {
      "logIndex": "0x0",
      "removed": false,
      "blockNumber": "0xb3",
      "blockHash": "0xe7cd776bfee2fad031d9cc1c463ef947654a031750b56fed3d5732bee9c61998",
      "transactionHash": "0xff36c03c0fba8ac4204e4b975a6632c862a3f08aa01b004f570cc59679ed4689",
      "transactionIndex": "0x0",
      "address": "0x2e1f232a9439c3d459fceca0beef13acc8259dd8",
      "data": "0x0000000000000000000000000000000000000000000000000000000000000003",
      "topics": [
        "0x04474795f5b996ff80cb47c148d4c5ccdbe09ef27551820caa9c2f8ed149cce3"
      ]
    },
    {
      "logIndex": "0x0",
      "removed": false,
      "blockNumber": "0xb6",
      "blockHash": "0x3f4cf35e7ed2667b0ef458cf9e0acd00269a4bc394bb78ee07733d7d7dc87afc",
      "transactionHash": "0x117a31d0dbcd3e2b9180c40aca476586a648bc400aa2f6039afdd0feab474399",
      "transactionIndex": "0x0",
      "address": "0x2e1f232a9439c3d459fceca0beef13acc8259dd8",
      "data": "0x0000000000000000000000000000000000000000000000000000000000000005",
      "topics": [
        "0x04474795f5b996ff80cb47c148d4c5ccdbe09ef27551820caa9c2f8ed149cce3"
      ]
    }
  ]
}
```

### eth_getLogs

Returns an array of logs matching a specified filter object.
> **Note** - This is not implemented when using database backend

> **Attention** - Using `eth_getLogs` to get the logs from a large range of blocks, especially an entire chain from its genesis block, can cause the API to hang and never return a response. We recommend splitting one large query into multiple ones for better performance.

**Parameters**

`Object` - Filter options object.

**Returns**

`array` - Log objects.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getLogs",
  "params": [
    {
      "fromBlock": "earliest",
      "toBlock": "latest",
      "address": "0x2e1f232a9439c3d459fceca0beef13acc8259dd8",
      "topics": []
    }
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": [
    {
      "logIndex": "0x0",
      "removed": false,
      "blockNumber": "0xb3",
      "blockHash": "0xe7cd776bfee2fad031d9cc1c463ef947654a031750b56fed3d5732bee9c61998",
      "transactionHash": "0xff36c03c0fba8ac4204e4b975a6632c862a3f08aa01b004f570cc59679ed4689",
      "transactionIndex": "0x0",
      "address": "0x2e1f232a9439c3d459fceca0beef13acc8259dd8",
      "data": "0x0000000000000000000000000000000000000000000000000000000000000003",
      "topics": [
        "0x04474795f5b996ff80cb47c148d4c5ccdbe09ef27551820caa9c2f8ed149cce3"
      ]
    },
    {
      "logIndex": "0x0",
      "removed": false,
      "blockNumber": "0xb6",
      "blockHash": "0x3f4cf35e7ed2667b0ef458cf9e0acd00269a4bc394bb78ee07733d7d7dc87afc",
      "transactionHash": "0x117a31d0dbcd3e2b9180c40aca476586a648bc400aa2f6039afdd0feab474399",
      "transactionIndex": "0x0",
      "address": "0x2e1f232a9439c3d459fceca0beef13acc8259dd8",
      "data": "0x0000000000000000000000000000000000000000000000000000000000000005",
      "topics": [
        "0x04474795f5b996ff80cb47c148d4c5ccdbe09ef27551820caa9c2f8ed149cce3"
      ]
    }
  ]
}
```

### eth_getMinerDataByBlockHash

Returns miner data for the specified block.
> **Note** - This is not implemented when using database backend

**Parameters**

`data` - 32 byte block hash.

**Returns**

`result` : Object - Miner data.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getMinerDataByBlockHash",
  "params": [
    "0xbf137c3a7a1ebdfac21252765e5d7f40d115c2757e4a4abee929be88c624fdb7"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": {
    "netBlockReward": "0x47c6f3739f3da800",
    "staticBlockReward": "0x4563918244f40000",
    "transactionFee": "0x38456548220800",
    "uncleInclusionReward": "0x22b1c8c1227a000",
    "uncleRewards": [
      {
        "hash": "0x2422d43b4f72e19faf4368949a804494f67559405046b39c6d45b1bd53044974",
        "coinbase": "0x0c062b329265c965deef1eede55183b3acb8f611"
      }
    ],
    "coinbase": "0xb42b6c4a95406c78ff892d270ad20b22642e102d",
    "extraData": "0xd583010502846765746885676f312e37856c696e7578",
    "difficulty": "0x7348c20",
    "totalDifficulty": "0xa57bcfdd96"
  }
}
```

### eth_getMinerDataByBlockNumber

Returns miner data for the specified block.
> **Note** - This is not implemented when using database backend

**Parameters**

`QUANTITY`|`TAG` - Integer representing a block number or one of the string tags `latest`, `earliest`, or `pending`, as described in [Block Parameter](#block-parameter).

**Returns**

`result` : object - Miner data.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getMinerDataByBlockNumber",
  "params": [
    "0x7689D2"
  ]
}
```
**Sample Response**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "netBlockReward": "0x47c6f3739f3da800",
    "staticBlockReward": "0x4563918244f40000",
    "transactionFee": "0x38456548220800",
    "uncleInclusionReward": "0x22b1c8c1227a000",
    "uncleRewards": [
      {
        "hash": "0x2422d43b4f72e19faf4368949a804494f67559405046b39c6d45b1bd53044974",
        "coinbase": "0x0c062b329265c965deef1eede55183b3acb8f611"
      }
    ],
    "coinbase": "0xb42b6c4a95406c78ff892d270ad20b22642e102d",
    "extraData": "0xd583010502846765746885676f312e37856c696e7578",
    "difficulty": "0x7348c20",
    "totalDifficulty": "0xa57bcfdd96"
  }
}
```

### eth_getStorageAt

Returns the value of a storage position at a specified address.
> **Note** - This is not implemented when using database backend

**Parameters**

`DATA` - A 20-byte storage address.

`QUANTITY` - Integer index of the storage position.

`QUANTITY`|`TAG` - Integer representing a block number or one of the string tags `latest`, `earliest`, or `pending`, as described in [Block Parameter](#block-parameter).

**Returns**

`result` : DATA - The value at the specified storage position.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getStorageAt",
  "params": [
    "0x3B3F3E",
    "0x0",
    "latest"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x0000000000000000000000000000000000000000000000000000000000000000"
}
```

### eth_getTransactionByBlockHashAndIndex

Returns transaction information for the specified block hash and transaction index position.
> **Note** - This is not implemented when using database backend

**Parameters**

`DATA` - 32-byte hash of a block.

`QUANTITY` - Integer representing the transaction index position.

**Returns**

`Object` - Transaction object, or null when there is no transaction.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getTransactionByBlockHashAndIndex",
  "params": [
    "0xa52be92809541220ee0aaaede6047d9a6c5d0cd96a517c854d944ee70a0ebb44",
    "0x1"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": {
    "blockHash": "0xbf137c3a7a1ebdfac21252765e5d7f40d115c2757e4a4abee929be88c624fdb7",
    "blockNumber": "0x1442e",
    "from": "0x70c9217d814985faef62b124420f8dfbddd96433",
    "gas": "0x3d090",
    "gasPrice": "0x57148a6be",
    "hash": "0xfc766a71c406950d4a4955a340a092626c35083c64c7be907060368a5e6811d6",
    "input": "0x51a34eb8000000000000000000000000000000000000000000000029b9e659e41b780000",
    "nonce": "0x2cb2",
    "to": "0xcfdc98ec7f01dab1b67b36373524ce0208dc3953",
    "transactionIndex": "0x2",
    "value": "0x0",
    "v": "0x2a",
    "r": "0xa2d2b1021e1428740a7c67af3c05fe3160481889b25b921108ac0ac2c3d5d40a",
    "s": "0x63186d2aaefe188748bfb4b46fb9493cbc2b53cf36169e8501a5bc0ed941b484"
  }
}
```

### eth_getTransactionByBlockNumberAndIndex

Returns transaction information for the specified block number and transaction index position.
> **Note** - This is not implemented when using database backend

**Parameters**

`QUANTITY`|`TAG` - Integer representing a block number or one of the string tags `latest`, `earliest`, or `pending`, as described in [Block Parameter](#block-parameter).

`QUANTITY` - The transaction index position.

**Returns**

`Object` - Transaction object, or null when there is no transaction.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getTransactionByBlockNumberAndIndex",
  "params": [
    "latest",
    "0x0"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": {
    "blockHash": "0xbf137c3a7a1ebdfac21252765e5d7f40d115c2757e4a4abee929be88c624fdb7",
    "blockNumber": "0x1442e",
    "from": "0x70c9217d814985faef62b124420f8dfbddd96433",
    "gas": "0x3d090",
    "gasPrice": "0x57148a6be",
    "hash": "0xfc766a71c406950d4a4955a340a092626c35083c64c7be907060368a5e6811d6",
    "input": "0x51a34eb8000000000000000000000000000000000000000000000029b9e659e41b780000",
    "nonce": "0x2cb2",
    "to": "0xcfdc98ec7f01dab1b67b36373524ce0208dc3953",
    "transactionIndex": "0x2",
    "value": "0x0",
    "v": "0x2a",
    "r": "0xa2d2b1021e1428740a7c67af3c05fe3160481889b25b921108ac0ac2c3d5d40a",
    "s": "0x63186d2aaefe188748bfb4b46fb9493cbc2b53cf36169e8501a5bc0ed941b484"
  }
}
```

### eth_getTransactionByHash

Returns transaction information for the specified transaction hash.
> **Note** - This is not implemented when using database backend

**Parameters**

`DATA` - 32-byte transaction hash.

**Returns**

`Object` - Transaction object, or null when there is no transaction.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getTransactionByHash",
  "params": [
    "0xa52be92809541220ee0aaaede6047d9a6c5d0cd96a517c854d944ee70a0ebb44"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": {
    "blockHash": "0x510efccf44a192e6e34bcb439a1947e24b86244280762cbb006858c237093fda",
    "blockNumber": "0x422",
    "from": "0xfe3b557e8fb62b89f4916b721be55ceb828dbd73",
    "gas": "0x5208",
    "gasPrice": "0x3b9aca00",
    "hash": "0xa52be92809541220ee0aaaede6047d9a6c5d0cd96a517c854d944ee70a0ebb44",
    "input": "0x",
    "nonce": "0x1",
    "to": "0x627306090abab3a6e1400e9345bc60c78a8bef57",
    "transactionIndex": "0x0",
    "value": "0x4e1003b28d9280000",
    "v": "0xfe7",
    "r": "0x84caf09aefbd5e539295acc67217563438a4efb224879b6855f56857fa2037d3",
    "s": "0x5e863be3829812c81439f0ae9d8ecb832b531d651fb234c848d1bf45e62be8b9"
  }
}
```

### eth_getTransactionCount

Returns the number of transactions sent from a specified address. Use the `pending` tag to get the next account nonce not used by any pending transactions.

**Parameters**

`DATA` - A 20-byte storage address.

`QUANTITY`|`TAG` - Integer representing a block number or one of the string tags `latest`, `earliest`, or `pending`, as described in [Block Parameter](#block-parameter).

**Returns**

`result` : Quantity - Integer representing the number of transactions sent from the specified address.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getTransactionCount",
  "params": [
    "0x9d8f8572f345e1ae53db1dFA4a7fce49B467bD7f",
    "latest"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x1"
}
```

### eth_getTransactionReceipt

Returns the receipt of a transaction by transaction hash. Receipts for pending transactions are not available.
> **Note** - This is not implemented when using database backend

**Parameters**

`DATA` - 32-byte hash of a transaction.

**Returns**

`Object` - Transaction receipt object, or null when there is no receipt.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getTransactionReceipt",
  "params": [
    "0x96c6830efd87a70020d4d1647c93402d747c05ecf6beeb068dee621f8d13d8d1"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": {
    "blockHash": "0xe7212a92cfb9b06addc80dec2a0dfae9ea94fd344efeb157c41e12994fcad60a",
    "blockNumber": "0x50",
    "contractAddress": null,
    "cumulativeGasUsed": "0x5208",
    "from": "0x627306090abab3a6e1400e9345bc60c78a8bef57",
    "gasUsed": "0x5208",
    "logs": [],
    "logsBloom": "0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000",
    "status": "0x1",
    "to": "0xf17f52151ebef6c7334fad080c5704d77216b732",
    "transactionHash": "0xc00e97af59c6f88de163306935f7682af1a34c67245e414537d02e422815efc3",
    "transactionIndex": "0x0"
  }
}
```

### eth_getUncleByBlockHashAndIndex

Returns uncle specified by block hash and index.
> **Note** - This is not implemented when using database backend

**Parameters**

`data` - 32-byte block hash.

`quantity` - Index of the uncle.

**Returns**

`result` : Block object

> Note - Uncles do not contain individual transactions.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getUncleByBlockHashAndIndex",
  "params": [
    "0xc48fb64230a82f65a08e7280bd8745e7fea87bc7c206309dee32209fe9a985f7",
    "0x0"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": {
    "difficulty": "0x76b123df93230",
    "extraData": "0x50505945206e616e6f706f6f6c2e6f7267",
    "gasLimit": "0x7a121d",
    "gasUsed": "0x7a0175",
    "hash": "0xc20189c0b1a4a23116ab3b177e929137f6e826f17fc4c2e880e7258c620e9817",
    "logsBloom": "0x890086c024487ca422be846a201a10e41bc2882902312116c1119609482031e9c000e2a708004a10281024028020c505727a12570c4810121c59024490b040894406a1c23c37a0094810921da3923600c71c03044b40924280038d07ab91964a008084264a01641380798840805a284cce201a8026045451002500113a00de441001320805ca2840037000111640d090442c11116d2112948084240242340400236ce81502063401dcc214b9105194d050884721c1208800b20501a4201400276004142f118e60808284506979a86e050820101c170c185e2310005205a82a2100382422104182090184800c02489e033440218142140045801c024cc1818485",
    "miner": "0x52bc44d5378309ee2abf1539bf71de1b7d7be3b5",
    "mixHash": "0xf557cc827e058862aa3ea1bd6088fb8766f70c0eac4117c56cf85b7911f82a14",
    "nonce": "0xd320b48904347cdd",
    "number": "0x768964",
    "parentHash": "0x98d752708b3677df8f439c4529f999b94663d5494dbfc08909656db3c90f6255",
    "receiptsRoot": "0x0f838f0ceb73368e7fc8d713a7761e5be31e3b4beafe1a6875a7f275f82da45b",
    "sha3Uncles": "0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347",
    "size": "0x21a",
    "stateRoot": "0xa0c7d4fca79810c89c517eff8dadb9c6d6f4bcc27c2edfb301301e1cf7dec642",
    "timestamp": "0x5cdcbba6",
    "totalDifficulty": "0x229ad33cabd4c40d23d",
    "transactionsRoot": "0x866e38e91d01ef0387b8e07ccf35cd910224271ccf2b7477b8c8439e8b70f365",
    "uncles": []
  }
}
```

### eth_getUncleByBlockNumberAndIndex

Returns uncle specified by block number and index.
> **Note** - This is not implemented when using database backend

**Parameters**

`QUANTITY`|`TAG` - Index of the block, or one of the string tags `latest`, `earliest`, or `pending`, as described in [Block Parameter](#block-parameter).

`QUANTITY` - Index of the uncle.

**Returns**

`result` : Block object

> Note - Uncles do not contain individual transactions.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getUncleByBlockNumberAndIndex",
  "params": [
    "0x7689D2",
    "0x0"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": {
    "difficulty": "0x77daec467bf93",
    "extraData": "0x50505945206e616e6f706f6f6c2e6f7267",
    "gasLimit": "0x7a121d",
    "gasUsed": "0x7a0f7b",
    "hash": "0x42d83ae9c0743f4b1f9c61ff7ea8b164c1bab3627decd49233760680be006ecf",
    "logsBloom": "0x888200800000340120220008640200500408006100038400100581c000080240080a0014e8002010080004088040004022402a000c18010001400100002a041141a0610a0052900600041018c0002a0003090020404c00206010010513d00020005380124e08050480710000000108401012b0901c1424006000083a10a8c1040100a0440081050210124400040044304070004001100000012600806008061d0320800000b40042160600002480000000800000c0002100200940801c000820800048024904710000400640490026000a44300309000286088010c2300060003011380006400200812009144042204810209020410a84000410520c08802941",
    "miner": "0x52bc44d5378309ee2abf1539bf71de1b7d7be3b5",
    "mixHash": "0xf977fcdb52868be410b75ef2becc35cc312f13ab0a6ce400ecd9d445f66fa3f2",
    "nonce": "0x628b28403bf1e3d3",
    "number": "0x7689d0",
    "parentHash": "0xb32cfdfbf4adb05d30f02fcc6fe039cc6666402142954051c1a1cb9cc91aa11e",
    "receiptsRoot": "0x9c7c8361d1a24ea2841432234c81974a9920d3eba2b2b1c496b5f925a95cb4ac",
    "sha3Uncles": "0x7d972aa1b182b7e93f1db043f03fbdbfac6874fe7e67e162141bcc0aefa6336b",
    "size": "0x21a",
    "stateRoot": "0x74e97b77813146344d75acb5a52a006cc6dfaca678a10fb8a484a8443e919272",
    "timestamp": "0x5cdcc0a7",
    "totalDifficulty": "0x229b0583b4bd2698ca0",
    "transactionsRoot": "0x1d21626afddf05e5866de66ca3fcd98f1caf5357eba0cc6ec675606e116a891b",
    "uncles": []
  }
}
```

### eth_getUncleCountByBlockHash

Returns the number of uncles in a block from a block matching the given block hash.
> **Note** - This is not implemented when using database backend

**Parameters**

`DATA` - 32-byte block hash.

**Returns**

`result` : QUANTITY - Integer representing the number of uncles in the specified block.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getUncleCountByBlockHash",
  "params": [
    "0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x0"
}
```

### eth_getUncleCountByBlockNumber

Returns the number of uncles in a block matching the specified block number.
> **Note** - This is not implemented when using database backend

**Parameters**

`QUANTITY`|`TAG` - Integer representing either the index of the block within the blockchain, or one of the string tags `latest`, `earliest`, or `pending`, as described in [Block Parameter](#block-parameter).

**Returns**

`result` : QUANTITY - Integer representing the number of uncles in the specified block.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_getUncleCountByBlockNumber",
  "params": [
    "latest"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x1"
}
```

### eth_hashrate

Returns the number of hashes per second with which the node is mining.
> **Note** - This is not implemented when using database backend

**Parameters**

None

**Returns**

`result` : quantity - Number of hashes per second.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_hashrate",
  "params": []
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x12b"
}
```

### eth_mining

Whether the client is actively mining new blocks. The API pauses mining while the client synchronizes with the network regardless of command settings or methods called.
> **Note** - This is not implemented when using database backend

**Parameters**

None

**Returns**

`result` - BOOLEAN - `true` if the client is actively mining new blocks, otherwise `false`.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_mining",
  "params": []
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": true
}
```

### eth_newBlockFilter

Creates a filter to retrieve new block hashes. To poll for new blocks, use `eth_getFilterChanges`.
> **Note** - This is not implemented when using database backend

**Parameters**

None

**Returns**

`data` - Filter ID.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_newBlockFilter",
  "params": []
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x9d78b6780f844228b96ecc65a320a825"
}
```

### eth_newFilter

Creates a log filter. To poll for logs associated with the created filter, use [`eth_getFilterChanges`](#eth_getfilterchanges). To get all logs associated with the filter, use [`eth_getFilterLogs`](#eth_getfilterlogs).
> **Note** - This is not implemented when using database backend

**Parameters**

`Object` - Filter options object.

> Note - `fromBlock` and `toBlock` in the filter options object default to `latest`.

**Returns**

`data` - Filter ID.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_newFilter",
  "params": [
    {
      "fromBlock": "earliest",
      "toBlock": "latest",
      "topics": []
    }
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x1ddf0c00989044e9b41cc0ae40272df3"
}
```

### eth_newPendingTransactionFilter

Creates a filter to retrieve new pending transactions hashes. To poll for new pending transactions, use [`eth_getFilterChanges`](#eth_getfilterchanges).
> **Note** - This is not implemented when using database backend

**Parameters**

None

**Returns**

`data` - Filter ID.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_newPendingTransactionFilter",
  "params": []
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x443d6a77c4964707a8554c92f7e4debd"
}
```

### eth_protocolVersion

Returns current Ethereum protocol version.
> **Note** - This is not implemented when using database backend

**Parameters**

None

**Returns**

`result` : quantity - Ethereum protocol version.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_protocolVersion",
  "params": []
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x3f"
}
```

### eth_sendRawTransaction

Sends a signed transaction. A transaction can send ether, deploy a contract, or interact with a contract.

You can interact with contracts using [`eth_sendRawTransaction`](#eth_sendrawtransaction) or [`eth_call`](#eth_call).

To avoid exposing your private key, create signed transactions offline and send the signed transaction data using `eth_sendRawTransaction`.

> **Important** - The API does not implement eth_sendTransaction.

**Parameters**

`data` - Signed transaction serialized to hexadecimal format.

**Returns**

`result` : data - 32-byte transaction hash.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_sendRawTransaction",
  "params": [
    "0xf86a018203e882520894f17f52151ebef6c7334fad080c5704d77216b732896c6b935b8bbd400000801ba093129415f03b4794fd1512e79ee7f097e4271f66721020f8407aac92179893a5a0451b875d89721ec98be55201092980b0a87bb1c48507fccb86da713596b2a09e"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0xac182cc23bb94696217aa17ca15bd466106af9ba7ea7420aae24ff37338d6e3b"
}
```

### eth_submitHashrate

Submits the mining hashrate.
> **Note** - This is not implemented when using database backend

**Parameters**

`DATA` - 32 Bytes - Hexadecimal string representation of the hash rate.
`DATA` - 32 Bytes - Random hexadecimal ID identifying the client.

**Returns**

`result`: Boolean, `true` if submission is successful, otherwise `false`.

**Sample Request**
```json
{
  "id": 1
  "jsonrpc": "2.0",
  "method": "eth_submitHashrate",
  "params": [
    "0x0000000000000000000000000000000000000000000000000000000000500000",
    "0x59daa26581d0acd1fce254fb7e85952f4c09d0915afd33d3886cd914bc7d283c"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": true
}
```

### eth_syncing

Returns an object with data about the synchronization status, or `false` if not synchronizing.
> **Note** - This is not implemented when using database backend

**Parameters**

None

**Returns**

`result` : Object|Boolean - Object with synchronization status data or `false` if not synchronizing:

- `startingBlock` : quantity - Index of the highest block on the blockchain when the network synchronization starts.
- `currentBlock` : quantity - Index of the latest block (also known as the best block) for the current node. This is the same index that [`eth_blockNumber`](#eth_blocknumber) returns.
- `highestBlock` : quantity - Index of the highest known block in the peer network (that is, the highest block so far discovered among peer nodes). This is the same value as `currentBlock` if the current node has no peers.
- `pulledStates` : quantity - If fast synchronizing, the number of state entries fetched so far, or `null` if this is not known or not relevant. If full synchronizing or fully synchronized, this field is not returned.
- `knownStates` : quantity - If fast synchronizing, the number of states the node knows of so far, or `null` if this is not known or not relevant. If full synchronizing or fully synchronized, this field is not returned.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_syncing",
  "params": []
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": {
    "startingBlock": "0x0",
    "currentBlock": "0x1518",
    "highestBlock": "0x9567a3",
    "pulledStates": "0x203ca",
    "knownStates": "0x200636"
  }
}
```

### eth_uninstallFilter

Uninstalls a filter with the specified ID. When a filter is no longer required, call this method.

Filters time out when not requested by [`eth_getFilterChanges`](#eth_getfilterchanges) or [`eth_getFilterLogs`](#eth_getfilterlogs) for 10 minutes.
> **Note** - This is not implemented when using database backend

**Parameters**

`data` - Filter ID.

**Returns**

`Boolean` - `true` if the filter was successfully uninstalled, otherwise `false`.

**Sample Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_uninstallFilter",
  "params": [
    "0x70355a0b574b437eaa19fe95adfedc0a"
  ]
}
```
**Sample Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": true
}
```

## Setup `meveo` and `keycloak`
When creating or updating a wallet using the Wallet API, it is also possible to automatically create and update a corresponding keycloak and meveo user as well.
For this to work follow the steps below to setup `keycloak` and `meveo` to allow this functionality.

### Configure `keycloak`
1. Login to keycloak administration console.
2. Select `Master` realm at the top left of the admin page.
3. Click `Clients` at the sidebar menu.
4. Select `admin-cli` client.
5. Under the `Settings` tab select:
    - **Access Type**: `confidential`
    - **Service Accounts**: `enabled`
6. Under the `Credentials` tab copy the generated `secret` credentials, this will be needed later on when configuring `meveo` settings.
7. Select the `Service Account Roles` tab.
8. Type in and select `meveo-realm` in `Client Roles`.
9. Assign the following roles:
    - `manage-users`
    - `view-users`
10. Switch to the `Meveo` realm at the top left of the admin page.
11. Click `Roles` at the sidebar menu.
12. Select `Default Roles` tab and set the following roles:
    - **meveo-web**: `apiAccess`, `userSelfManagement`
    - **endpoints**: `Execute_All_Endpoints`
13. Click `Realm Settings` at the sidebar menu.
14. Select `Login` tab and select:
    - **Edit username**: `on`

### Configure password rules in `keycloak`
1. Login to keycloak administration console.
2. Select `Meveo` realm.
3. Click `Authentication` at the sidebar menu.
4. Click `Password Policy` tab.
5. Click `Add policy...` dropdown at the top right of the table and set the following policies:
    - **Minimum Length** - `8`
    - **Not Recently Used** - `1`
    - **Special Characters** - `1`
    - **Uppercase Characters** - `1`
    - **Digits** - `1`
6. Click `Save`.

> **Note**: `Maximum Length` password policy is only implemented in `Keycloak 15` or higher.  If available, set its value to 16

### Update `meveo` settings
1. Login to `meveo` admin.
2. Select `Configuration` > `Settings` > `System settings`
3. Create or replace the following setting:
    - **keycloak.client.secret**:  enter the `secret` copied from `step 6` in [Configure keyloak](#configure-keycloak) above.
4. Click or tab out of the new setting then click `Save`


## wallet_creation
This request is made via POST method to json rpc method **wallet_creation** with the following parameters in this order **[name, address, accountHash, signature, publicInfo, privateInfo]** where:
- **name** (required): is combination of firstname and lastname OR username only
- **address** (required): is the wallet's hash (not lowercase)
    - **e.g.** 0x307E27AA863E5dccdF8979FB9F5AF32539101421
- **accountHash** (required): the account hash
- **signature** (required): the signature generated from the **privateInfo** details
- **publicInfo** (required): string escaped json data containing public profile and other information
- **privateInfo** (required): string escaped json data containing emailAddress(required), and phoneNumber(optional)
    - **e.g.** `{\"emailAddress\":\"account1@gmail.com\", \"phoneNumber\":\"+639991234567\"}`

> **Note** -  It is possible to create a `keycloak` and `meveo` user automatically when a wallet is created.  To accomplish that, the **username** can be included in the **publicInfo** or in the **privateInfo** data and make sure that the **password** is included in the **privateInfo**.  See [Setup meveo and keycloak](#setup-meveo-and-keycloak) to setup `keycloak` and `meveo` properly for this functionality to work. 

**Sample Request**
```json
{
  "jsonrpc": "2.0",
  "method": "wallet_creation",
  "params": [
    "Test Wallet",
    "0xac08e612D1318BC9c0Aa671A1b90199bB12Bd876",
    "3ea21960cc11bb3d82bcf47d2992e39935e182a401385720f842cbdddc97a408",
    "0x42d1db7d16c111e62a8725371eb04e98ef353de88d11be7faef6768d18f0603851e4cfed4fd7d5130a70be022f8f96d6e280dedbcf190d9804d1540e95f0939c1c",
    "{\"shippingAddress\":{\"email\":\"testwallet1@telecelplay.io\",\"phone\":\"+639991234567\",\"address\":\"Milo\",\"street\":\"Kaban\",\"zipCode\":\"39242\",\"city\":\"Ciney\",\"country\":\"Combo\"},\"coords\":null}",
    "{\"username\": \"walletuser\", \"emailAddress\":\"testwallet1@telecelplay.io\", \"password\": \"walletuser\",\"phoneNumber\":\"+639991234567\"}"
  ]
}
```
**Sample Response**
```json
{
    "id": "null",
    "jsonrpc": "2.0",
    "result": "ac08e612d1318bc9c0aa671a1b90199bb12bd876"
}
```

## wallet_update
This request is made via POST method to json rpc method **wallet_update** with the following parameters in this order **[name, address, signature, publicInfo, privateInfo]** where:
- **name** (required): is combination of firstname and lastname OR username only
- **address** (required): is the wallet's hash (not lowercase)
    - **e.g.** 0x307E27AA863E5dccdF8979FB9F5AF32539101421
- **signature** (required): the signature generated from the **publicInfo**
- **publicInfo** (required): string escaped json data containing public profile and other information
- **privateInfo** (optional): string escaped json data containing emailAddress(required), and phoneNumber(optional)
    - **e.g.** `{\"emailAddress\":\"account1@gmail.com\", \"phoneNumber\":\"+639991234567\"}`

> **Note** -  It is possible to update `keycloak` and `meveo` user details when a wallet is updated.  To accomplish that, the **username** can be included in the **publicInfo** or in the **privateInfo**.  See [Setup meveo and keycloak](#setup-meveo-and-keycloak) to setup `keycloak` and `meveo` properly for this functionality to work.

**Sample Request**
```json
{
  "jsonrpc": "2.0",
  "method": "wallet_update",
  "params": [
    "Test Wallet",
    "0xac08e612D1318BC9c0Aa671A1b90199bB12Bd876",
    "0x80682b0d9f06cec1797c8d303eb0ee5b42129eed9d5d3aa7118c3b924cd23cf5539831ad585e90db545356c7db3ee18461aff0301ca2d64c922f685fc664b6be1c",
    "{\"shippingAddress\":{\"email\":\"account2@telecelplay.io\",\"phone\":\"+639997654321\",\"address\":\"Milo\",\"street\":\"Kaban\",\"zipCode\":\"39242\",\"city\":\"Ciney\",\"country\":\"Combo\"},\"coords\":null}",
    "{\"emailAddress\":\"account2@telecelplay.io\", \"phoneNumber\":\"+639997654321\"}"
  ]
}
```
**Sample Response**
```json
{
  "id": "null",
  "jsonrpc": "2.0",
  "result": "Test Wallet"
}
```

## wallet_info
This request is made via POST method to json rpc method **wallet_info** with the following parameters in this order **[address, signature, message]** where:
- **address** (required): is the wallet's hash (not lowercase)
    - **e.g** 0x307E27AA863E5dccdF8979FB9F5AF32539101421
- **signature** (optional, required only when retrieving privateInfo): is signed signature of the message
- **message** (optional, required only when retrieving privateInfo): is a comma separated value string containing the string **"walletInfo"**, the **address** (wallet hash in hex), and the **timestamp** (in millis) of the request
    - **e.g.**  walletInfo,0x307e27aa863e5dccdf8979fb9f5af32539101421,1648456123780

**Sample Request**
```json
{
  "jsonrpc": "2.0",
  "method": "wallet_info",
  "params": [
    "0xac08e612D1318BC9c0Aa671A1b90199bB12Bd876",
    "0x82a0faa065ce169afbd63c5bd36543fb56d9bb6b4f693ae5a41fbbabfa3bb1b709b5b5a7f9cef140cb6134d50b4005e2054fd8ca351f53498702aebd8330c8561c",
    "walletInfo,0xac08e612D1318BC9c0Aa671A1b90199bB12Bd876,1654238475264"
  ]
}
```
**Sample Response**
```json
{
  "id": "null",
  "jsonrpc": "2.0",
  "result": {
    "name": "Test Wallet",
    "publicInfo": "{\"shippingAddress\":{\"email\":\"account2@telecelplay.io\",\"phone\":\"+639997654321\",\"address\":\"Milo\",\"street\":\"Kaban\",\"zipCode\":\"39242\",\"city\":\"Ciney\",\"country\":\"Combo\"},\"coords\":null}"
  }
}
```

## Password reset process
To reset password, OTP verification is required.  Follow the steps below for proper password reset process.
1. Send an OTP to the wallet's verified phone number using either [`mv-twilio`](https://github.com/telecelplay/mv-twilio) or [`mv-smstelecel`](https://github.com/telecelplay/mv-smstelecel) module.
2. Verify the OTP using the `verifyOtpForPasswordReset` endpoint with the following details:
**POST - /rest/verifyOtpForPasswordReset/{phoneNumber}**
- **phoneNumber** - is the verified phone number associated with the user wallet
- **otp** - the otp code
- **password** - the new password

**Sample Request**
```json
{
    "otp": "123456",
    "password": "verifiedPassword"
}
```

**Sample Response**
```json
{
  "status": "success",
  "result": "password_updated"
}
```

## Block parameter 

The block parameter can have the following values:
   
- `blockNumber` : `quantity` - The block number, specified in hexadecimal or decimal. 0 represents the genesis block.
- `earliest` : `tag` - The earliest (genesis) block.
- `latest` : `tag` - The last block mined.
- `pending` : `tag` - The last block mined plus pending transactions. Use only with [eth_getTransactionCount](#eth_getTransactionCount).

## Postman Collections
[Postman](https://www.postman.com/) collections with sample requests are available in the [**/facets/postman**](https://github.com/telecelplay/mv-liquichain-api/tree/master/facets/postman) folder. 
