import "https://cdn.jsdelivr.net/npm/web3@1.8.1/dist/web3.min.js";
import abi from "./abi.js";

const web3 = new Web3("https://dev.telecelplay.io/meveo/rest/jsonrpc");
const mainContainer = document.getElementById("main");
const contractAddress = "0xContractAddress"; // fetch the contractAddress from server

const contract = new web3.eth.Contract(abi, contractAddress);

(async () => {
    const senderPrivateKey = "0xSenderPrivateKey" // replace with valid private key ;
    const recipient = "0xRecipientWalletAddress" //replace with valid recipient wallet address;
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
