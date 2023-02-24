const getUsername = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/getUsername/${parameters.emailOrNumber}`, baseUrl);
	return fetch(url.toString(), {
		method: 'GET'
	});
}

const getUsernameForm = (container) => {
	const html = `<form id='getUsername-form'>
		<div id='getUsername-emailOrNumber-form-field'>
			<label for='emailOrNumber'>emailOrNumber</label>
			<input type='text' id='getUsername-emailOrNumber-param' name='emailOrNumber'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const emailOrNumber = container.querySelector('#getUsername-emailOrNumber-param');

	container.querySelector('#getUsername-form button').onclick = () => {
		const params = {
			emailOrNumber : emailOrNumber.value !== "" ? emailOrNumber.value : undefined
		};

		getUsername(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { getUsername, getUsernameForm };