const getUsername = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/getUsername/${parameters.emailAddress}`, baseUrl);
	return fetch(url.toString(), {
		method: 'GET'
	});
}

const getUsernameForm = (container) => {
	const html = `<form id='getUsername-form'>
		<div id='getUsername-emailAddress-form-field'>
			<label for='emailAddress'>emailAddress</label>
			<input type='text' id='getUsername-emailAddress-param' name='emailAddress'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const emailAddress = container.querySelector('#getUsername-emailAddress-param');

	container.querySelector('#getUsername-form button').onclick = () => {
		const params = {
			emailAddress : emailAddress.value !== "" ? emailAddress.value : undefined
		};

		getUsername(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { getUsername, getUsernameForm };