const wallet-by-contact = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/wallet-by-contact/`, baseUrl);
	return fetch(url.toString(), {
		method: 'POST', 
		headers : new Headers({
 			'Content-Type': 'application/json'
		}),
		body: JSON.stringify({
			contactHashes : parameters.contactHashes
		})
	});
}

const wallet-by-contactForm = (container) => {
	const html = `<form id='wallet-by-contact-form'>
		<div id='wallet-by-contact-contactHashes-form-field'>
			<label for='contactHashes'>contactHashes</label>
			<input type='text' id='wallet-by-contact-contactHashes-param' name='contactHashes'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const contactHashes = container.querySelector('#wallet-by-contact-contactHashes-param');

	container.querySelector('#wallet-by-contact-form button').onclick = () => {
		const params = {
			contactHashes : contactHashes.value !== "" ? contactHashes.value : undefined
		};

		wallet-by-contact(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { wallet-by-contact, wallet-by-contactForm };