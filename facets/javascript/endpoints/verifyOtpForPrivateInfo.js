const verifyOtpForPrivateInfo = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/verifyOtpForPrivateInfo/${parameters.phoneNumber}`, baseUrl);
	if (parameters.otp !== undefined) {
		url.searchParams.append('otp', parameters.otp);
	}

	return fetch(url.toString(), {
		method: 'GET'
	});
}

const verifyOtpForPrivateInfoForm = (container) => {
	const html = `<form id='verifyOtpForPrivateInfo-form'>
		<div id='verifyOtpForPrivateInfo-phoneNumber-form-field'>
			<label for='phoneNumber'>phoneNumber</label>
			<input type='text' id='verifyOtpForPrivateInfo-phoneNumber-param' name='phoneNumber'/>
		</div>
		<div id='verifyOtpForPrivateInfo-otp-form-field'>
			<label for='otp'>otp</label>
			<input type='text' id='verifyOtpForPrivateInfo-otp-param' name='otp'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const phoneNumber = container.querySelector('#verifyOtpForPrivateInfo-phoneNumber-param');
	const otp = container.querySelector('#verifyOtpForPrivateInfo-otp-param');

	container.querySelector('#verifyOtpForPrivateInfo-form button').onclick = () => {
		const params = {
			phoneNumber : phoneNumber.value !== "" ? phoneNumber.value : undefined,
			otp : otp.value !== "" ? otp.value : undefined
		};

		verifyOtpForPrivateInfo(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { verifyOtpForPrivateInfo, verifyOtpForPrivateInfoForm };