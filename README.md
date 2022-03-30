## Eth API Backend
The ETH API endpoint is at: **/meveo/rest/jsonrpc** and it is compatible with [Ethereum API methods](https://docs.infura.io/infura/networks/ethereum/json-rpc-methods)

There are currently two possible backends we can use with this API.  To select the desired backend:
1. Login to Meveo Admin.
2. Select **Configuration > Settings > System settings** menu.
3. Set the **txn.blockchain.type** value to either **BESU** or **DATABASE**
  - **DATABASE** - this will save data to the database
  - **BESU** - this will proxy the requests to a [BESU](https://besu.hyperledger.org/en/stable/) API endpoint
    - when using BESU, also check the **besu.api.url** value to point to the correct BESU API endpoint. 

## Wallet Backend
The wallet endpoints are also served through the ETH API endpoint.  It has thee methods currently implemented: **wallet_creation, wallet_update, and wallet_info**.  Wallet data are saved to the database regardless of the backend chosen.

### Wallet Creation
This request is made via POST method to json rpc method **wallet_creation** with the following parameters in this order **[name, address, accountHash, signature, publicInfo, privateInfo]** where:
- **name** (required): is combination of firstname and lastname OR username only
- **address** (required): is the wallet's hash (not lowercase)
    - **e.g.** 0x307E27AA863E5dccdF8979FB9F5AF32539101421
- **accountHash** (required): the account hash
- **signature** (required): the signature generated from the **publicInfo** details  
- **publicInfo** (required): string escaped json data containing public profile and other information
- **privateInfo** (optional): string escaped json data containing emailAddress(optional), and phoneNumber(required)
    - **e.g.** `{\"emailAddress\":\"account1@gmail.com\", \"phoneNumber\":\"+639991234567\"}`

### Wallet Update
This request is made via POST method to json rpc method **wallet_creation** with the following parameters in this order **[name, address, signature, publicInfo, privateInfo]** where:
- **name** (required): is combination of firstname and lastname OR username only
- **address** (required): is the wallet's hash (not lowercase)
    - **e.g.** 0x307E27AA863E5dccdF8979FB9F5AF32539101421
- **signature** (required): the signature generated from the **publicInfo**
- **publicInfo** (required): string escaped json data containing public profile and other information
- **privateInfo** (optional): string escaped json data containing emailAddress(optional), and phoneNumber(required)
    - **e.g.** `{\"emailAddress\":\"account1@gmail.com\", \"phoneNumber\":\"+639991234567\"}`

### Get Wallet Info
This request is made via POST method to json rpc method **wallet_info** with the following parameters in this order **[address, signature, message]** where:
- **address** (required): is the wallet's hash (not lowercase)
    - **e.g** 0x307E27AA863E5dccdF8979FB9F5AF32539101421
- **signature** (optional, required only when retrieving privateInfo): is signed signature of the message
- **message** (optional, required only when retrieving privateInfo): is a comma separated value string containing the string **"walletInfo"**, the **address** (wallet hash in hex, lowercase), and the **timestamp** (in millis) of the request
    - **e.g.**  walletInfo,0x307e27aa863e5dccdf8979fb9f5af32539101421,1648456123780


## Wallet by Contact
Wallet address/hash can be retrieved by sending a POST request to the endpoint **/meveo/rest/wallet-by-contact**.

The **request** should be a JSON object with a single property named **contactHashes**

e.g.
```json
{
    "contactHashes": [
      "eda1d32b8cd98fca5d205a64cd3248b55a76d987",
      "1b087d5293780b059da57ada26d51e79aa33b2c1"
    ]
}
```
The **response** will be in the format: `{"phoneHash": "wallet address/hash"}`

e.g.
```json
{
    "1b087d5293780b059da57ada26d51e79aa33b2c1": "cfoEb1bE78E1Db0B36d3C1F908f4165537217321",
    "eda1d32b8cd98fca5d205a64cd3248b55a76d987": "deE0d5bE78E1Db0B36d3C1F908f4165537217333"
}
``` 

## Postman Collections
[Postman](https://www.postman.com/) collections are available in the **/facets/postman** folder. 
