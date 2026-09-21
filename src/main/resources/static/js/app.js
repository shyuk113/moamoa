(() => {
  const token = document.querySelector('meta[name="_csrf"]')?.content;
  const header = document.querySelector('meta[name="_csrf_header"]')?.content;
  async function request(url, method, data) {
    const response = await fetch(url, {
      method, credentials: 'same-origin',
      headers: {'Content-Type': 'application/json', ...(header ? {[header]: token} : {})},
      ...(data === undefined ? {} : {body: JSON.stringify(data)})
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      if (response.status === 401 && !url.startsWith('/api/auth/')) location.assign('/auth/login');
      throw new Error(payload.message || (response.status === 403 ? '화면을 새로고침한 뒤 다시 시도해주세요.' : '요청을 처리하지 못했습니다.'));
    }
    return payload;
  }
  function toast(message) {
    const el = document.getElementById('toast');
    if (!el) return;
    el.textContent = message; el.classList.add('visible');
    setTimeout(() => el.classList.remove('visible'), 4000);
  }
  document.querySelectorAll('[data-api]').forEach(form => {
    form.addEventListener('submit', async event => {
      event.preventDefault();
      const button = form.querySelector('button[type="submit"],button:not([type])');
      const message = form.querySelector('.form-message');
      const fields = new FormData(form);
      const data = Object.fromEntries(fields);
      if (form.dataset.kind === 'interests') data.categories = fields.getAll('categories');
      button.disabled = true; message.textContent = '처리 중입니다…';
      try {
        const result = await request(form.dataset.api, form.dataset.method || 'POST', data);
        message.textContent = result.message;
        if (form.dataset.redirect) location.assign(form.dataset.redirect);
      } catch (error) { message.textContent = error.message || '연결을 확인하고 다시 시도해주세요.'; }
      finally { button.disabled = false; }
    });
  });
  document.querySelectorAll('[data-favorite]').forEach(button => {
    button.addEventListener('click', async () => {
      button.disabled = true;
      try {
        const saved = button.dataset.saved === 'true';
        await request('/api/favorites/' + button.dataset.favorite, saved ? 'DELETE' : 'POST');
        document.querySelectorAll('[data-favorite="' + button.dataset.favorite + '"]').forEach(el => {
          el.dataset.saved = String(!saved); el.setAttribute('aria-pressed', String(!saved));
          el.textContent = saved ? '♡ 모아두기' : '♥ 모아둠';
        });
        toast(saved ? '모아둔 행사에서 해제했습니다.' : '행사를 모아두었습니다.');
      } catch (error) { toast(error.message); }
      finally { button.disabled = false; }
    });
  });
  document.querySelector('[data-logout]')?.addEventListener('click', async () => {
    try { await request('/api/auth/logout', 'POST'); location.assign('/'); }
    catch (error) { toast(error.message); }
  });
  document.querySelectorAll('.poster img,.detail-poster img').forEach(img => {
    img.addEventListener('error', () => { img.hidden = true; });
  });
})();
