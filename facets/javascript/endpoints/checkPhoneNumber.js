const checkPhoneNumber = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/checkPhoneNumber/${parameters.phoneNumber}`, baseUrl);
	return fetch(url.toString(), {
		method: 'GET'
	});
}

const checkPhoneNumberForm = (container) => {
	const html = `<form id='checkPhoneNumber-form'>
		<div id='checkPhoneNumber-phoneNumber-form-field'>
			<label for='phoneNumber'>phoneNumber</label>
			<input type='text' id='checkPhoneNumber-phoneNumber-param' name='phoneNumber'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const phoneNumber = container.querySelector('#checkPhoneNumber-phoneNumber-param');

	container.querySelector('#checkPhoneNumber-form button').onclick = () => {
		const params = {
			phoneNumber : phoneNumber.value !== "" ? phoneNumber.value : undefined
		};

		checkPhoneNumber(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { checkPhoneNumber, checkPhoneNumberForm };