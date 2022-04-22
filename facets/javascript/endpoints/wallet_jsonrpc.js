const wallet_jsonrpc = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/wallet_jsonrpc/`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			
		})
	});
}

const wallet_jsonrpcForm = (container) => {
	const html = `<form id='wallet_jsonrpc-form'>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)


	container.querySelector('#wallet_jsonrpc-form button').onclick = () => {
		const params = {

		};

		wallet_jsonrpc(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { wallet_jsonrpc, wallet_jsonrpcForm };