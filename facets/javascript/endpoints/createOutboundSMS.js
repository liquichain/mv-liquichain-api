const createOutboundSMS = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/createOutboundSMS/`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			to : parameters.to,
			otp : parameters.otp
		})
	});
}

const createOutboundSMSForm = (container) => {
	const html = `<form id='createOutboundSMS-form'>
		<div id='createOutboundSMS-to-form-field'>
			<label for='to'>to</label>
			<input type='text' id='createOutboundSMS-to-param' name='to'/>
		</div>
		<div id='createOutboundSMS-otp-form-field'>
			<label for='otp'>otp</label>
			<input type='text' id='createOutboundSMS-otp-param' name='otp'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const to = container.querySelector('#createOutboundSMS-to-param');
	const otp = container.querySelector('#createOutboundSMS-otp-param');

	container.querySelector('#createOutboundSMS-form button').onclick = () => {
		const params = {
			to : to.value !== "" ? to.value : undefined,
			otp : otp.value !== "" ? otp.value : undefined
		};

		createOutboundSMS(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { createOutboundSMS, createOutboundSMSForm };