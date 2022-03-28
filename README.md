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
This request is made via POST method to json rpc endpoint **wallet_creation** with following params in order **[name, address, signature, publicInfo, privateInfo]** where:
- **name**: is combination of firstname and lastname OR username only
- **address**: is the wallet's hash (not lowercase)
    - **e.g.** 0x307E27AA863E5dccdF8979FB9F5AF32539101421
- **signature**: is the signed signature of the comma separated name and address(wallet hash in hex, lowercase)
    - **e.g.** 
      - data: wallet123,0x307e27aa863e5dccdf8979fb9f5af32539101421
      - signature: 0xfd34ab4ca7f40e325eaeba6d57f994b893ba515a2492f5329c93970a9edcf0570f578d02c6c0951b04ee0796e4877844488d64322005a159b66bca6dee712ce81b
- **publicInfo**: string escaped json data containing public profile and other information
- **privateInfo**: string escaped json data containing emailAddress(optional), and phoneNumber(required)
    - **e.g.** {\"emailAddress\":\"account1@gmail.com\", \"phoneNumber\":\"+639991234567\"}

### Wallet Update
This request is made via POST method to json rpc endpoint **wallet_creation** with following params in order **[name, address, signature, publicInfo, privateInfo]** where:
- **name**: is combination of firstname and lastname OR username only
- **address**: is the wallet's hash (not lowercase)
    - **e.g.** 0x307E27AA863E5dccdF8979FB9F5AF32539101421
- **publicInfo**: string escaped json data containing public profile and other information
- **privateInfo**: string escaped json data containing emailAddress(optional), and phoneNumber(required)
    - **e.g.** {\"emailAddress\":\"account1@gmail.com\", \"phoneNumber\":\"+639991234567\"}

### Get Wallet Info
This request is made via POST method to json rpc endpoint **wallet_info** with following params in order **[address, signature, message]** where:
- **address** (required): is the wallet's hash (not lowercase)
    - **e.g** 0x307E27AA863E5dccdF8979FB9F5AF32539101421
- **signature** (optional, required only when retrieving privateInfo): is signed signature of the message
- **message** (optional, required only when retrieving privateInfo): is a comma separated value string containing the string **"walletInfo"**, the **address** (wallet hash in hex, lowercase), and the **timestamp** (in millis) of the request
    - **e.g.**  walletInfo,0x307e27aa863e5dccdf8979fb9f5af32539101421,1648456123780

## Postman Collections
[Postman](https://www.postman.com/) collections are available /facets/postman folder. 