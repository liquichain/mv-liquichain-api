const jsonrpc = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/jsonrpc/`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			null : parameters.null
		})
	});
}

const jsonrpcForm = (container) => {
	const html = `<form id='jsonrpc-form'>
		<div id='jsonrpc-null-form-field'>
			<label for='null'>null</label>
			<input type='text' id='jsonrpc-null-param' name='null'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const null = container.querySelector('#jsonrpc-null-param');

	container.querySelector('#jsonrpc-form button').onclick = () => {
		const params = {
			null : null.value !== "" ? null.value : undefined
		};

		jsonrpc(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { jsonrpc, jsonrpcForm };