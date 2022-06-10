const createOutboundSMS = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/createOutboundSMS/`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			
		})
	});
}

const createOutboundSMSForm = (container) => {
	const html = `<form id='createOutboundSMS-form'>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)


	container.querySelector('#createOutboundSMS-form button').onclick = () => {
		const params = {

		};

		createOutboundSMS(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { createOutboundSMS, createOutboundSMSForm };