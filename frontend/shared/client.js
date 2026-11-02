export function start() {
  const el = id => document.getElementById(id);
  let token, me, peer, socket, retry, refresh, typingTimer;
  let contactList = [], messages = new Map(), lastTyping = 0;
  const status = text => el('status').textContent = text;

  async function api(path, body, auth = token) {
    const headers = auth ? { Authorization: 'Bearer ' + auth } : {};
    if (body) headers['Content-Type'] = 'application/json';
    const res = await fetch('/api/' + path, { method: body ? 'POST' : 'GET', headers,
      body: body ? JSON.stringify(body) : undefined, cache: 'no-store' });
    if (!res.ok) {
      if (res.status === 401 && auth && auth === token) logout();
      const hints = {400:'Vérifiez les champs saisis.',401:'Connexion refusée ou session expirée.',
        409:'Ce pseudo est déjà utilisé.'};
      throw Error(hints[res.status] || 'Requête impossible (' + res.status + ').');
    }
    return res.json();
  }

  function logout() {
    token = null; me = null; peer = null;
    clearTimeout(retry); clearInterval(refresh); clearTimeout(typingTimer);
    const previous = socket; socket = null; previous?.close();
    messages.clear(); contactList = [];
    el('chat').hidden = true; el('auth').hidden = false; el('logout').hidden = true;
    el('who').textContent = ''; el('typing').textContent = ''; el('draft').value = '';
    el('contacts').replaceChildren(); el('feed').replaceChildren();
    el('search').value = ''; el('presence').textContent = '';
    el('empty').hidden = false; el('empty').textContent = 'Sélectionnez un contact pour ouvrir une conversation.';
    el('title').textContent = 'Choisissez un contact'; status('Déconnecté');
  }

  function renderMessages() {
    const rows = [...messages.values()].filter(m => m.expediteurId === peer || m.destinataireId === peer)
      .sort((a,b) => (new Date(a.dateEnvoi) - new Date(b.dateEnvoi)) || a.id-b.id);
    el('empty').hidden = rows.length > 0;
    el('empty').textContent = peer ? 'Commencez la conversation avec un premier message.' : 'Sélectionnez un contact pour ouvrir une conversation.';
    el('feed').replaceChildren(...rows.map(m => {
      const row = document.createElement('li');
      row.className = m.expediteurId === me.id ? 'mine' : 'theirs';
      const text = document.createElement('span'); text.textContent = m.contenu;
      const time = document.createElement('small');
      time.textContent = new Date(m.dateEnvoi).toLocaleString('fr-FR');
      row.append(text,time); return row;
    }));
    el('feed').scrollTop = el('feed').scrollHeight;
  }

  async function history() {
    const selected = peer, session = token;
    if (!selected) return;
    const rows = await api('messages/' + selected);
    if (token !== session || peer !== selected) return;
    for (const m of rows) messages.set(m.id,m);
    renderMessages();
  }

  function select(id) {
    if (id !== peer) el('draft').value = '';
    peer = id; el('typing').textContent = '';
    el('title').textContent = contactList.find(c => c.id === id)?.username || 'Choisissez un contact';
    renderContacts(); renderMessages(); history().catch(e => status(e.message));
  }

  function renderContacts() {
    const query = el('search').value.trim().toLocaleLowerCase('fr');
    const matches = contactList.filter(c => c.username.toLocaleLowerCase('fr').includes(query));
    el('no-contacts').textContent = matches.length ? '' : query ? 'Aucun contact correspondant.' : 'Les autres comptes apparaîtront ici.';
    const selected = contactList.find(c => c.id === peer);
    el('presence').textContent = selected ? (selected.connecte ? 'En ligne' : 'Hors ligne') : '';
    el('contacts').replaceChildren(...matches.map(c => {
      const button = document.createElement('button');
      button.type = 'button'; button.className = c.id === peer ? 'selected' : '';
      button.textContent = c.username + (c.connecte ? ' · en ligne' : ' · hors ligne');
      button.setAttribute('aria-pressed', String(c.id === peer));
      button.onclick = () => select(c.id); return button;
    }));
  }

  async function contacts() {
    const session = token, rows = await api('utilisateurs');
    if (token !== session) return;
    const next = rows.filter(c => c.id !== me.id);
    if (contactList.length && JSON.stringify(next) === JSON.stringify(contactList)) return;
    contactList = next;
    renderContacts();
    if (!peer && contactList.length) select(contactList[0].id);
  }

  function connect() {
    const url = new URL('/ws/messages', location.href);
    url.protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    url.searchParams.set('token', token);
    const current = socket = new WebSocket(url);
    current.onopen = () => {
      if (current !== socket) return;
      status('Connecté'); contacts().catch(e => status(e.message)); history().catch(e => status(e.message));
    };
    current.onmessage = event => {
      if (current !== socket) return;
      const m = JSON.parse(event.data);
      if (m.type === 'message') {
        messages.set(m.id,m); renderMessages();
        if (m.expediteurId === peer) el('typing').textContent = '';
      } else if (m.type === 'typing' && m.expediteurId === peer) {
        el('typing').textContent = 'Votre contact écrit…'; clearTimeout(typingTimer);
        typingTimer = setTimeout(() => el('typing').textContent = '', 2200);
      } else if (m.type === 'error') status(m.detail);
    };
    current.onclose = () => {
      if (current !== socket || !token) return;
      status('Reconnexion…'); retry = setTimeout(connect, 2000);
    };
  }

  el('auth').onsubmit = async event => {
    event.preventDefault();
    const buttons = [...el('auth').querySelectorAll('button')];
    buttons.forEach(b => b.disabled = true);
    try {
      const account = { username:el('name').value.trim(), motDePasse:el('password').value };
      if (event.submitter?.value === 'register') await api('comptes', account);
      token = (await api('auth/login', account)).token;
      me = await api('comptes/me');
      el('password').value = ''; el('auth').hidden = true; el('chat').hidden = false;
      el('logout').hidden = false; el('who').textContent = me.username;
      status('Connexion au canal…'); connect();
      refresh = setInterval(() => contacts().catch(e => status(e.message)), 5000);
    } catch(e) { status(e.message); }
    finally { buttons.forEach(b => b.disabled = false); }
  };
  el('search').oninput = renderContacts;
  el('logout').onclick = logout;
  el('compose').onsubmit = event => {
    event.preventDefault();
    const contenu = el('draft').value.trim();
    if (!peer || socket?.readyState !== WebSocket.OPEN) return status('Choisissez un contact ou attendez votre reconnexion.');
    if (!contenu) return;
    socket.send(JSON.stringify({type:'message', destinataireId:peer, contenu}));
    el('draft').value = '';
  };
  el('draft').oninput = () => {
    if (peer && socket?.readyState === WebSocket.OPEN && Date.now()-lastTyping > 800) {
      socket.send(JSON.stringify({type:'typing', destinataireId:peer})); lastTyping = Date.now();
    }
  };
}

