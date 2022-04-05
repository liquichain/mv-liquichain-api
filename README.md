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
- [eth_getProof](#eth_getproof)
- [eth_getStorageAt](#eth_getstorageat)
- [eth_getTransactionByBlockHashAndIndex](#)
- [eth_getTransactionByBlockNumberAndIndex](#)
- [eth_getTransactionByHash](#)
- [eth_getTransactionCount](#)
- [eth_getTransactionReceipt](#)
- [eth_getUncleByBlockHashAndIndex](#)
- [eth_getUncleByBlockNumberAndIndex](#)
- [eth_getUncleCountByBlockHash](#)
- [eth_getUncleCountByBlockNumber](#)
- [eth_getWork](#)
- [eth_hashrate](#)
- [eth_mining](#)
- [eth_newBlockFilter](#)
- [eth_newFilter](#)
- [eth_newPendingTransactionFilter](#)
- [eth_protocolVersion](#)
- [eth_sendRawTransaction](#)
- [eth_submitHashrate](#)
- [eth_submitWork](#)
- [eth_syncing](#)
- [eth_uninstallFilter](#)

### eth_accounts
Returns a list of account addresses a client owns.
> Note - This is not implemented in the dev server (using database backend)

**Parameters**

None

**Returns**

`Array of data`: List of 20-byte account addresses owned by the client.

> **Note** - This method returns an empty object because the API doesn't support key management inside the client.

**Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_accounts",
  "params": []
}
```
**Response**
```json
{
  "id": 1,  
  "jsonrpc": "2.0",
  "result": []
}
```

### eth_blockNumber

Returns the index corresponding to the block number of the current chain head.

**Parameters**

None

**Returns**

`result` : QUANTITY - Hexadecimal integer representing the index corresponding to the block number of the current chain head.

**Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_blockNumber",
  "params": []
}
```
**Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",  
  "result": "0x2377"
}
```

### eth_call

Invokes a contract function locally and does not change the state of the blockchain.

You can interact with contracts using `eth_sendRawTransaction` or `eth_call`.
> Note - This returns a hard coded result `0x` in the dev server (using database backend)

**Parameters**

`OBJECT` - Transaction call object.

`QUANTITY`|`TAG` - Integer representing a block number or one of the string tags latest, earliest, or pending, as described in [Block Parameter](#block-parameter).

> Note - By default, `eth_call` does not fail if the sender account has an insufficient balance. This is done by setting the balance of the account to a large amount of ether. To enforce balance rules, set the strict parameter in the transaction call object to true.

**Returns**

`result` - `data` - Return value of the executed contract.

**Request**
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
**Response**
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

**Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_chainId",
  "params": []
}
```
**Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",  
  "result": "0x4c"
}
```

### eth_coinbase

Returns the client coinbase address. The coinbase address is the account to pay mining rewards to.
> Note - This is not implemented in the dev server (using database backend)

**Parameters**

None

**Returns**

`result` : `data` - Coinbase address.

**Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_coinbase",
  "params": []
}
```
**Response**
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
> Note - in dev server, this is hard coded to `0x0` while in staging server, this is by default set to `0x5208`

**Request**
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
**Response**
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
> Note - in both the dev server and staging server, the return value is hard coded to `0x0`

**Request**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "method": "eth_gasPrice",
  "params": []
}
```
**Response**
```json
{
  "id": 1,
  "jsonrpc": "2.0",
  "result": "0x0"
}
```

### eth_getBlockByHash

Returns the account balance of the specified address.

**Parameters**

`DATA` - 20-byte account address from which to retrieve the balance.

`QUANTITY`|`TAG` - Integer representing a block number or one of the string tags latest, earliest, or pending, as described in [Block Parameter](#block-parameter).

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_getBlockByNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_getBlockTransactionCountByHash
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_getBlockTransactionCountByNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_getCode
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_getFilterChanges
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_getFilterLogs
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_getLogs
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_getMinerDataByBlockHash
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_getMinerDataByBlockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_getProof
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_getStorageAt
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

### eth_blockNumber
**Parameters**

**Returns**

**Request**
```json

```
**Response**
```json

```

## Block parameter 

The block parameter can have the following values:
   
- `blockNumber` : `quantity` - The block number, specified in hexadecimal or decimal. 0 represents the genesis block.
- `earliest` : `tag` - The earliest (genesis) block.
- `latest` : `tag` - The last block mined.
- `pending` : `tag` - The last block mined plus pending transactions. Use only with [eth_getTransactionCount](#eth_getTransactionCount).

## Postman Collections
[Postman](https://www.postman.com/) collections with sample requests are available in the [**/facets/postman**](https://github.com/telecelplay/mv-liquichain-api/tree/master/facets/postman) folder. 
