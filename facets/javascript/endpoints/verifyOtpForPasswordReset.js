const verifyOtpForPasswordReset = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/verifyOtpForPasswordReset/${parameters.to}`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			otp : parameters.otp,
			password : parameters.password
		})
	});
}

const verifyOtpForPasswordResetForm = (container) => {
	const html = `<form id='verifyOtpForPasswordReset-form'>
		<div id='verifyOtpForPasswordReset-to-form-field'>
			<label for='to'>to</label>
			<input type='text' id='verifyOtpForPasswordReset-to-param' name='to'/>
		</div>
		<div id='verifyOtpForPasswordReset-otp-form-field'>
			<label for='otp'>otp</label>
			<input type='text' id='verifyOtpForPasswordReset-otp-param' name='otp'/>
		</div>
		<div id='verifyOtpForPasswordReset-password-form-field'>
			<label for='password'>password</label>
			<input type='text' id='verifyOtpForPasswordReset-password-param' name='password'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const to = container.querySelector('#verifyOtpForPasswordReset-to-param');
	const otp = container.querySelector('#verifyOtpForPasswordReset-otp-param');
	const password = container.querySelector('#verifyOtpForPasswordReset-password-param');

	container.querySelector('#verifyOtpForPasswordReset-form button').onclick = () => {
		const params = {
			to : to.value !== "" ? to.value : undefined,
			otp : otp.value !== "" ? otp.value : undefined,
			password : password.value !== "" ? password.value : undefined
		};

		verifyOtpForPasswordReset(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { verifyOtpForPasswordReset, verifyOtpForPasswordResetForm };