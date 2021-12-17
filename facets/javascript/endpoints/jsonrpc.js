const jsonrpc = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/jsonrpc/`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			
		})
	});
}

const jsonrpcForm = (container) => {
	const html = `<form id='jsonrpc-form'>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)


	container.querySelector('#jsonrpc-form button').onclick = () => {
		const params = {

		};

		jsonrpc(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { jsonrpc, jsonrpcForm };