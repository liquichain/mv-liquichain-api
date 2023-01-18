import "https://cdn.jsdelivr.net/npm/web3@1.8.1/dist/web3.min.js";
import abi from "./abi.js";

const web3 = new Web3("https://dev.telecelplay.io/meveo/rest/jsonrpc");
const mainContainer = document.getElementById("main");
const contractAddress = "0x64F782197283D592B9a6ceE67038f990fc29225B"

const contract = new web3.eth.Contract(abi, contractAddress);

(async () => {
    const senderPrivateKey = "807a0dee7e890b72976f4be47e7f25204e656d25954f38234cc04c4c01fab223";
    const recipient = "0xb4bF880BAfaF68eC8B5ea83FaA394f5133BB9623";
    const tokenId = 0;
    const transferAmount = web3.utils.toWei("1", "ether");

    // this will invoke the transfer method on the smart contract
    const data = contract.methods.transfer(recipient, tokenId, transferAmount).encodeABI();
    const transaction = {
        gas: 120000,
        to: contract.options.address,
        data
    };

    // this will sign the transaction using the sender's private key
    const {rawTransaction} = await web3.eth.accounts.signTransaction(transaction, senderPrivateKey);

    // this is just to show what the request will look like when sending to testnet
    const sampleRequest = JSON.stringify({
        jsonrpc: "2.0",
        id: 1,
        method: "eth_sendRawTransaction",
        params: [rawTransaction]
    }, null, 4);
    mainContainer.innerText = "\n\nSample request:\n" + sampleRequest;

    // this will execute the transaction and transfer 1 token
    const transactionResult = await web3.eth.sendSignedTransaction(rawTransaction);

    mainContainer.innerText += "\n\nTransaction result:\n" + JSON.stringify(transactionResult, null, 4) ;
})();
