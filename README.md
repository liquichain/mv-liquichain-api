## Wallet Creation
This request is made via POST method to json rpc endpoint **wallet_creation** with following params in order [name, address, signature, publicInfo] where:
- name: is combination of firstname and lastname OR username only
- address: wallet address hex
- signature: is signature of name and address as following sha3JS.keccak_256(firstname + lastname + account.address)
- publicInfo: json data containing public profile and other information
