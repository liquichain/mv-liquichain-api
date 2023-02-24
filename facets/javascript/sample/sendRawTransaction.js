import "https://cdn.jsdelivr.net/npm/web3@1.8.1/dist/web3.min.js";
import abi from "./abi.js";

const web3 = new Web3("https://dev.telecelplay.io/meveo/rest/jsonrpc");
const mainContainer = document.getElementById("main");
const contractAddress = "0xContractAddress"; // fetch the contractAddress from server

const contract = new web3.eth.Contract(abi, contractAddress);

(() => {
    const senderPrivateKey = "0xSenderPrivateKey" // replace with sender's private key ;
    const recipient = "0xRecipientAddress" //replace with valid recipient wallet address;
    const tokenId = 0;
    const transferAmount = web3.utils.toWei("1", "wei");

    const transfer = async () => {
        const transferData = contract.methods.transfer(recipient, tokenId, transferAmount).encodeABI();
        const transaction = {
            gas: 120000,
            to: contract.options.address,
            data: transferData
        };

        // this will sign the transaction using the sender's private key
        const {rawTransaction} = await web3.eth.accounts.signTransaction(transaction, senderPrivateKey);

        // this is just to show what the request will look like when sending to testnet
        const transferRequest = JSON.stringify({
            jsonrpc: "2.0",
            id: 1,
            method: "eth_sendRawTransaction",
            params: [rawTransaction]
        }, null, 4);
        mainContainer.innerText += "\n\ntransfer request:\n" + transferRequest;

        // this will execute the transaction and transfer 1 token
        const transactionResult = await web3.eth.sendSignedTransaction(rawTransaction);

        mainContainer.innerText += "\n\ntransfer result:\n" + JSON.stringify(transactionResult, null, 4);
    }

    const balanceOf = async () => {
        const balanceOfData = contract.methods.balanceOf(recipient, 0).encodeABI();

        // this is just to show what the request will look like when sending to testnet
        const balanceOfRequest = JSON.stringify({
            jsonrpc: "2.0",
            id: 1,
            method: "eth_call",
            params: [{"to": contractAddress, data: balanceOfData}, "latest"]
        }, null, 4);
        mainContainer.innerText += "\n\nSample balanceOf request:\n" + balanceOfRequest;

        const balanceOfResult = await web3.eth.call({to: contractAddress, data: balanceOfData})
        mainContainer.innerText += "\n\nbalanceOf result:\n" + balanceOfResult;
    }

    const getToken = async () => {
        const getTokenData = contract.methods.getToken(0).encodeABI();

        // this is just to show what the request will look like when sending to testnet
        const getTokenRequest = JSON.stringify({
            jsonrpc: "2.0",
            id: 1,
            method: "eth_call",
            params: [{"to": contractAddress, data: getTokenData}, "latest"]
        }, null, 4);
        mainContainer.innerText += "\n\nSample getToken request:\n" + getTokenRequest;

        const getTokenResult = await contract.methods.getToken(0).call();
        mainContainer.innerText += "\n\ngetToken result:\n" + getTokenResult;
    }

    const listTokens = async () => {
        const listTokensData = contract.methods.listTokenInfos().encodeABI();

        // this is just to show what the request will look like when sending to testnet
        const listTokensRequest = JSON.stringify({
            jsonrpc: "2.0",
            id: 1,
            method: "eth_call",
            params: [{"to": contractAddress, data: listTokensData}, "latest"]
        }, null, 4);
        mainContainer.innerText += "\n\nSample listTokenInfos request:\n" + listTokensRequest;

        const listTokensResult = await contract.methods.listTokenInfos().call();
        mainContainer.innerText += "\n\nlistTokenInfos result:\n" + listTokensResult;
    }

    // this call will transfer 1 wei to the wallet, uncomment to run it.
    // transfer();
    balanceOf();
    getToken();
    listTokens();

})();
