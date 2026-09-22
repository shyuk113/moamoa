(() => {
  const en = document.documentElement.lang === 'en';
  const say = (ko, english) => en ? english : ko;
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
      const detail = en && payload.message?.includes(' / ') ? payload.message.split(' / ').pop() : payload.message;
      const englishError = {400:'Check the values you entered.',401:'Please log in.',403:'Access denied. Refresh the page and try again.',409:'This item already exists or was changed. Reload and try again.',429:'Please wait one minute before trying again.'};
      throw new Error(en ? (payload.message?.includes(' / ') ? detail : englishError[response.status] || 'Unable to complete the request.') : payload.message || '요청을 처리하지 못했습니다.');
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
      if (form.dataset.kind === 'review') data.rating = Number(data.rating);
      if (form.dataset.kind === 'moderation') data.version = Number(data.version);
      if (form.dataset.kind === 'guide') ['latitude','longitude'].forEach(key => data[key] = data[key] ? Number(data[key]) : null);
      button.disabled = true; message.textContent = say('처리 중입니다…', 'Saving…');
      try {
        const result = await request(form.dataset.api, form.dataset.method || 'POST', data);
        message.textContent = en ? (result.message?.includes(' / ') ? result.message.split(' / ').pop() : form.dataset.api.includes('forgot-password') ? 'If this email is registered, a reset link has been sent.' : 'Saved successfully.') : result.message;
        if (form.dataset.reload) location.reload();
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
          el.textContent = saved ? say('♡ 모아두기','♡ Save') : say('♥ 모아둠','♥ Saved');
        });
        toast(saved ? say('모아둔 행사에서 해제했습니다.','Event removed.') : say('행사를 모아두었습니다.','Event saved.'));
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
  document.querySelectorAll('[data-language]').forEach(link => link.addEventListener('click', event => {
    event.preventDefault(); const url = new URL(location.href); url.searchParams.set('lang',link.dataset.language); location.assign(url);
  }));
  document.querySelectorAll('[data-embed]').forEach(button => button.addEventListener('click', () => {
    const frame=button.parentElement.querySelector('iframe'); frame.src=button.dataset.embed; frame.hidden=false; button.hidden=true;
  }));
  const shareUrl=() => {const u=new URL(location.href);u.search='';u.hash='';return u.href;};
  async function copyLink(){try{await navigator.clipboard.writeText(shareUrl());toast(say('링크를 복사했습니다.','Link copied.'));}catch{toast(say('주소창의 링크를 복사해주세요.','Please copy the link from the address bar.'));}}
  document.querySelector('[data-copy-link]')?.addEventListener('click',copyLink);
  document.querySelector('[data-share]')?.addEventListener('click',async()=>{
    if(!navigator.share){await copyLink();return;}
    try{await navigator.share({title:document.querySelector('h1')?.textContent,url:shareUrl()});}catch(e){if(e.name!=='AbortError')await copyLink();}
  });
  document.querySelector('[data-delete-review]')?.addEventListener('click',async event=>{
    const button=event.currentTarget;button.disabled=true;
    try{await request('/api/reviews/'+button.dataset.deleteReview,'DELETE');location.reload();}catch(e){toast(e.message);button.disabled=false;}
  });
})();
