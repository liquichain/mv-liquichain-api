const etherscan = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/etherscan/`, baseUrl);
	if (parameters.module !== undefined) {
		url.searchParams.append('module', parameters.module);
	}

	if (parameters.action !== undefined) {
		url.searchParams.append('action', parameters.action);
	}

	if (parameters.address !== undefined) {
		url.searchParams.append('address', parameters.address);
	}

	if (parameters.tag !== undefined) {
		url.searchParams.append('tag', parameters.tag);
	}

	if (parameters.apikey !== undefined) {
		url.searchParams.append('apikey', parameters.apikey);
	}

	if (parameters.offset !== undefined) {
		url.searchParams.append('offset', parameters.offset);
	}

	if (parameters.limit !== undefined) {
		url.searchParams.append('limit', parameters.limit);
	}

	return fetch(url.toString(), {
		method: 'GET'
	});
}

const etherscanForm = (container) => {
	const html = `<form id='etherscan-form'>
		<div id='etherscan-module-form-field'>
			<label for='module'>module</label>
			<input type='text' id='etherscan-module-param' name='module'/>
		</div>
		<div id='etherscan-action-form-field'>
			<label for='action'>action</label>
			<input type='text' id='etherscan-action-param' name='action'/>
		</div>
		<div id='etherscan-address-form-field'>
			<label for='address'>address</label>
			<input type='text' id='etherscan-address-param' name='address'/>
		</div>
		<div id='etherscan-tag-form-field'>
			<label for='tag'>tag</label>
			<input type='text' id='etherscan-tag-param' name='tag'/>
		</div>
		<div id='etherscan-apikey-form-field'>
			<label for='apikey'>apikey</label>
			<input type='text' id='etherscan-apikey-param' name='apikey'/>
		</div>
		<div id='etherscan-offset-form-field'>
			<label for='offset'>offset</label>
			<input type='text' id='etherscan-offset-param' name='offset'/>
		</div>
		<div id='etherscan-limit-form-field'>
			<label for='limit'>limit</label>
			<input type='text' id='etherscan-limit-param' name='limit'/>
		</div>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)

	const module = container.querySelector('#etherscan-module-param');
	const action = container.querySelector('#etherscan-action-param');
	const address = container.querySelector('#etherscan-address-param');
	const tag = container.querySelector('#etherscan-tag-param');
	const apikey = container.querySelector('#etherscan-apikey-param');
	const offset = container.querySelector('#etherscan-offset-param');
	const limit = container.querySelector('#etherscan-limit-param');

	container.querySelector('#etherscan-form button').onclick = () => {
		const params = {
			module : module.value !== "" ? module.value : undefined,
			action : action.value !== "" ? action.value : undefined,
			address : address.value !== "" ? address.value : undefined,
			tag : tag.value !== "" ? tag.value : undefined,
			apikey : apikey.value !== "" ? apikey.value : undefined,
			offset : offset.value !== "" ? offset.value : undefined,
			limit : limit.value !== "" ? limit.value : undefined
		};

		etherscan(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { etherscan, etherscanForm };