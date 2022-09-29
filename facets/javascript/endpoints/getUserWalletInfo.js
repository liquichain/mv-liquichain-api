const getUserWalletInfo = async (parameters) =>  {
	const baseUrl = window.location.origin;
	const url = new URL(`${window.location.pathname.split('/')[1]}/rest/getUserWalletInfo/`, baseUrl);
	return fetch(url.toString(), {
		method: 'GET'
	});
}

const getUserWalletInfoForm = (container) => {
	const html = `<form id='getUserWalletInfo-form'>
		<button type='button'>Test</button>
	</form>`;

	container.insertAdjacentHTML('beforeend', html)


	container.querySelector('#getUserWalletInfo-form button').onclick = () => {
		const params = {

		};

		getUserWalletInfo(params).then(r => r.text().then(
				t => alert(t)
			));
	};
}

export { getUserWalletInfo, getUserWalletInfoForm };