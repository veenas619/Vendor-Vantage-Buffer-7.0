// ═══════════════════════════════════════════════════════════════════
//  Vendor Vantage — script.js   (All 7 bugs fixed)
// ═══════════════════════════════════════════════════════════════════
const API = "http://localhost:8080/api";

// ── Shared helpers ────────────────────────────────────────────────
async function apiFetch(url, options = {}) {
    try {
        const res = await fetch(url, {
            ...options,
            headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }
        });
        return await res.json();
    } catch (err) {
        console.error("API error:", url, err);
        return null;
    }
}

// SESSION ISOLATION FIX: Use sessionStorage (per-tab) for currentUser identity.
// localStorage is shared across all tabs on the same origin — two users logging in
// on different tabs would overwrite each other's session. sessionStorage is
// completely isolated per tab, so each tab maintains its own logged-in user.
function getUser()    { return JSON.parse(sessionStorage.getItem('currentUser') || 'null'); }
function setUser(obj) { sessionStorage.setItem('currentUser', JSON.stringify(obj)); }

function showAlert(icon, title, msg) {
    const m = document.getElementById('alertModal');
    if (!m) { alert(title + '\n' + msg); return; }
    document.getElementById('alertIcon').innerText  = icon;
    document.getElementById('alertTitle').innerText = title;
    document.getElementById('alertMsg').innerText   = msg;
    m.style.display = 'flex';
}
function closeAlert() {
    const m = document.getElementById('alertModal');
    if (m) m.style.display = 'none';
}

// ── Gateway (index.html) ──────────────────────────────────────────
const customerCard = document.getElementById('customerCard');
const vendorCard   = document.getElementById('vendorCard');
if (customerCard) customerCard.onclick = () => window.location.href = 'customer-auth.html';
if (vendorCard)   vendorCard.onclick   = () => window.location.href = 'vendor-auth.html';

// ── Vendor Auth (vendor-auth.html) ────────────────────────────────
function showErr(id, msg)  { const el = document.getElementById(id); el.innerText = msg; el.style.display = ''; }
function hideErr(id)       { document.getElementById(id).style.display = 'none'; }

const loginBtn = document.getElementById('loginBtn');
if (loginBtn) {
    loginBtn.onclick = async () => {
        const key = document.getElementById('loginKey').value.trim().toLowerCase();
        const pw  = document.getElementById('loginPw').value;
        if (!key || !pw) { showErr('loginError', 'Please enter vendor key and password.'); return; }
        hideErr('loginError');

        const data = await apiFetch(`${API}/vendor/login`, {
            method: 'POST',
            body: JSON.stringify({ key, password: pw })
        });

        if (!data || data.error) {
            showErr('loginError', data ? data.error : 'Server not reachable. Is java Server running?');
            return;
        }
        setUser({ name: data.name, bizName: data.name, type: 'vendor', vendorKey: key });
        window.location.href = 'vendor-dashboard.html';
    };
}

const regBtn = document.getElementById('regBtn');
if (regBtn) {
    regBtn.onclick = async () => {
        const name = document.getElementById('regName').value.trim();
        const pw   = document.getElementById('regPw').value.trim();
        if (!name)       { showErr('regError', 'Business name is required.');      return; }
        if (pw.length < 4){ showErr('regError', 'Password must be at least 4 chars.'); return; }
        hideErr('regError');

        const stops = [];
        document.querySelectorAll('.stop-row').forEach(row => {
            const n = row.querySelector('.stop-name').value.trim();
            const s = parseInt(row.querySelector('.stop-stay').value)   || 15;
            const t = parseInt(row.querySelector('.stop-travel').value) || 10;
            if (n) stops.push({ name: n, stay: s, travel: t });
        });
        if (stops.length === 0) { showErr('regError', 'Add at least one stop.'); return; }

        const menu = {};
        document.querySelectorAll('.menu-row').forEach(row => {
            const item  = row.querySelector('.menu-name').value.trim();
            const price = parseInt(row.querySelector('.menu-price').value) || 0;
            if (item && price > 0) menu[item] = price;
        });
        if (Object.keys(menu).length === 0) { showErr('regError', 'Add at least one menu item.'); return; }

        const data = await apiFetch(`${API}/vendor/register`, {
            method: 'POST',
            body: JSON.stringify({ name, password: pw, stops, menu })
        });

        if (!data || data.error) {
            showErr('regError', data ? data.error : 'Server not reachable.');
            return;
        }

        const sEl = document.getElementById('regSuccess');
        sEl.innerText = `Registered! Your key: "${data.key}" — use it to login.`;
        sEl.style.display = '';
        setTimeout(() => {
            setUser({ name: data.name, bizName: data.name, type: 'vendor', vendorKey: data.key });
            window.location.href = 'vendor-dashboard.html';
        }, 1800);
    };
}

// ── Customer Auth (customer-auth.html) ────────────────────────────
const custLoginBtn = document.getElementById('custLoginBtn');
if (custLoginBtn) {
    custLoginBtn.onclick = () => {
        const name = document.getElementById('custName').value.trim();
        if (!name) {
            const el = document.getElementById('custError');
            if (el) { el.innerText = 'Please enter your name.'; el.style.display = ''; }
            return;
        }
        // SESSION ISOLATION: sessionStorage is already tab-scoped, but clear it
        // explicitly to ensure a clean slate when a new user logs in on this tab.
        sessionStorage.removeItem('currentUser');
        setUser({ name, type: 'customer' });
        window.location.href = 'dashboard.html';
    };
}

function logoutCustomer() {
    sessionStorage.removeItem('currentUser');
    window.location.href = 'index.html';
}

function logoutVendor() {
    sessionStorage.removeItem('currentUser');
    window.location.href = 'index.html';
}

// ── Customer Dashboard (dashboard.html) ───────────────────────────
const vendorGrid  = document.getElementById('vendorGrid');
const searchInput = document.getElementById('foodSearch');

async function renderVendors(keyword = '') {
    if (!vendorGrid) return;
    vendorGrid.innerHTML = "<p style='color:#888;padding:20px'>Loading vendors...</p>";
    const url = keyword.trim()
        ? `${API}/search?q=${encodeURIComponent(keyword)}`
        : `${API}/vendors`;
    const vendors = await apiFetch(url);
    vendorGrid.innerHTML = '';
    if (!vendors || vendors.length === 0) {
        vendorGrid.innerHTML = `<p style='color:#888;padding:20px'>No vendors found${keyword ? ' for "'+keyword+'"' : ''}.</p>`;
        return;
    }

    function getStallImageUrl(vendor) {
        const name  = (vendor.name || '').toLowerCase();
        const items = Object.keys(vendor.menu || {}).join(' ').toLowerCase();
        const all   = name + ' ' + items;
        if (all.includes('pizza'))                    return 'https://source.unsplash.com/400x220/?pizza,food';
        if (all.includes('burger'))                   return 'https://source.unsplash.com/400x220/?burger,street-food';
        if (all.includes('coffee') || all.includes('sandwich'))
                                                      return 'https://source.unsplash.com/400x220/?coffee,cafe,snacks';
        if (all.includes('vada') || all.includes('samosa') || all.includes('misal'))
                                                      return 'https://source.unsplash.com/400x220/?indian,street-food,snacks';
        if (all.includes('momos') || all.includes('chow'))
                                                      return 'https://source.unsplash.com/400x220/?momos,dumplings,streetfood';
        if (all.includes('biryani') || all.includes('rice'))
                                                      return 'https://source.unsplash.com/400x220/?biryani,indian-food';
        if (all.includes('dosa') || all.includes('idli'))
                                                      return 'https://source.unsplash.com/400x220/?dosa,south-indian-food';
        if (all.includes('noodle') || all.includes('pasta'))
                                                      return 'https://source.unsplash.com/400x220/?noodles,pasta,food';
        if (all.includes('juice') || all.includes('shake'))
                                                      return 'https://source.unsplash.com/400x220/?juice,fresh-drinks';
        return 'https://source.unsplash.com/400x220/?street-food,food-stall,india';
    }

    vendors.forEach(v => {
        const menuItems  = Object.keys(v.menu || {});
        const rating     = parseFloat(v.rating) > 0 ? v.rating : 'New';
        const menuJson   = JSON.stringify(v.menu || {}).replace(/'/g, "\\'");
        const imgUrl     = getStallImageUrl(v);

        // Show day-ended banner if vendor ended route OR pressed End Day Early
        if (v.dayEnded || v.status === 'DAY-ENDED') {
            vendorGrid.innerHTML += `
                <div class="vendor-card" style="opacity:0.7;position:relative">
                    <div style="position:absolute;top:0;left:0;right:0;z-index:10;background:#e74c3c;color:white;text-align:center;padding:10px 14px;font-weight:700;font-size:0.95rem;border-radius:12px 12px 0 0">
                        🔴 Vendor has ended the day early — No longer accepting orders
                    </div>
                    <div class="vendor-img" style="position:relative;overflow:hidden;height:160px;background:#1a1a2e;margin-top:38px">
                        <img src="${imgUrl}" alt="${v.name}"
                             style="width:100%;height:100%;object-fit:cover;display:block;opacity:0.4;transition:opacity 0.4s"
                             onload="this.style.opacity='0.4'">
                        <div class="rating-badge" style="position:absolute;top:10px;right:10px;z-index:2">⭐ ${rating}</div>
                    </div>
                    <div class="vendor-info">
                        <h3>${v.name}</h3>
                        <div class="menu-tags">${menuItems.map(item => `<span>${item} ₹${v.menu[item]}</span>`).join('')}</div>
                        <p style="color:#e74c3c;font-weight:600;margin-top:10px">🚫 Day Ended Early — check back tomorrow</p>
                    </div>
                </div>`;
            return;
        }

        const statusFlag = v.status === 'ON-SITE'
            ? `<span class="status-flag on-site">● Cooking & Selling</span>`
            : `<span class="status-flag in-transit">🚚 In-Transit</span>`;
        // IN-TRANSIT: currentStop is the destination (transit() advanced it)
        const locationPill = v.status === 'ON-SITE'
            ? `<p class="current-stop">📍 At: <b>${v.currentStop}</b></p>`
            : `<p class="current-stop">🚀 Heading to: <b>${v.currentStop}</b></p>`;

        vendorGrid.innerHTML += `
            <div class="vendor-card">
                <div class="vendor-img" style="position:relative;overflow:hidden;height:160px;background:#1a1a2e;">
                    <img src="${imgUrl}"
                         alt="${v.name}"
                         style="width:100%;height:100%;object-fit:cover;display:block;opacity:0;transition:opacity 0.4s"
                         onload="this.style.opacity='1'"
                         onerror="this.style.display='none'">
                    <div class="rating-badge" style="position:absolute;top:10px;right:10px;z-index:2">⭐ ${rating}</div>
                    <div style="position:absolute;bottom:0;left:0;width:100%;background:linear-gradient(transparent,rgba(0,0,0,0.7));height:50px;"></div>
                </div>
                <div class="vendor-info">
                    <h3>${v.name}</h3>
                    <div class="menu-tags">
                        ${menuItems.map(item => `<span>${item} ₹${v.menu[item]}</span>`).join('')}
                    </div>
                    <div class="status-section">
                        <div class="location-display">${locationPill}${statusFlag}</div>
                        <div class="next-stop-preview">Next: <b>${v.nextStop}</b></div>
                    </div>
                    <button onclick='openOrderModal("${v.name}","${v.key}",${menuJson})'
                        style="margin-top:14px;background:#ff9f43;color:black;border:none;padding:11px 20px;border-radius:8px;font-weight:bold;cursor:pointer;width:100%;font-family:Poppins,sans-serif">
                        🛒 Pre-Order
                    </button>
                </div>
            </div>`;
    });
}

async function refreshWalletDisplay() {
    const user = getUser();
    if (!user) return;
    const data = await apiFetch(`${API}/wallet/balance?customer=${encodeURIComponent(user.name)}`);
    if (!data) return;
    const el = document.getElementById('sidebarWallet');
    if (el) el.innerText = data.balance;
}

async function topupWallet() {
    const user = getUser();
    if (!user) return;
    const data = await apiFetch(`${API}/wallet/topup`, {
        method: 'POST',
        body: JSON.stringify({ customer: user.name, amount: 200 })
    });
    if (data) {
        const el = document.getElementById('sidebarWallet');
        if (el) el.innerText = data.balance;
        showAlert('💰', 'Wallet Topped Up', '+₹200 added to your wallet!\nNew balance: ₹' + data.balance);
    }
}

function showDashTab(tab, el) {
    ['vendors','myorders','reviews'].forEach(t => {
        document.getElementById('tab-'+t).style.display = t === tab ? '' : 'none';
    });
    document.querySelectorAll('.side-nav a').forEach(a => a.classList.remove('active'));
    if (el) el.classList.add('active');
    if (tab === 'myorders')  loadMyOrders();
    if (tab === 'reviews')   loadReviewVendors();
}

// ── ORDER MODAL ───────────────────────────────────────────────────
let currentCart = {}, currentVendorKey = '', currentMenu = {};

function openOrderModal(vendorName, vendorKey, menu) {
    currentCart = {}; currentVendorKey = vendorKey; currentMenu = menu;
    document.getElementById('modalVendorName').innerText = `Order from ${vendorName}`;
    document.getElementById('orderLocation').value = '';
    const list = document.getElementById('menuItemsList');
    list.innerHTML = '';
    Object.entries(menu).forEach(([item, price]) => {
        const safeId = item.replace(/[^a-zA-Z0-9]/g,'_');
        list.innerHTML += `
            <div class="menu-item-row">
                <span>${item}</span>
                <span style="color:#ff9f43;font-weight:600">₹${price}</span>
                <div class="qty-control">
                    <button class="qty-btn" onclick="changeQty('${item}',-1)">−</button>
                    <span class="qty-display" id="qty-${safeId}">0</span>
                    <button class="qty-btn" onclick="changeQty('${item}',1)">+</button>
                </div>
            </div>`;
    });
    updateCartSummary();
    document.getElementById('orderModal').style.display = 'flex';
}

function changeQty(item, delta) {
    currentCart[item] = Math.max(0, (currentCart[item] || 0) + delta);
    if (currentCart[item] === 0) delete currentCart[item];
    const safeId = item.replace(/[^a-zA-Z0-9]/g,'_');
    const el = document.getElementById('qty-'+safeId);
    if (el) el.innerText = currentCart[item] || 0;
    updateCartSummary();
}

function updateCartSummary() {
    const summary = document.getElementById('cartSummary');
    const entries = Object.entries(currentCart);
    if (!summary) return;
    if (entries.length === 0) { summary.style.display = 'none'; return; }
    summary.style.display = '';
    let total = 0;
    document.getElementById('cartItems').innerHTML = '';
    entries.forEach(([item, qty]) => {
        const price = currentMenu[item] * qty;
        total += price;
        document.getElementById('cartItems').innerHTML +=
            `<div class="cart-row"><span>${item} × ${qty}</span><span>₹${price}</span></div>`;
    });
    document.getElementById('cartTotal').innerText   = '₹' + total;
    document.getElementById('tokenDeposit').innerText = Math.floor(total / 4);
}

function closeOrderModal() { document.getElementById('orderModal').style.display = 'none'; }

async function submitOrder() {
    const user = getUser();
    if (!user) { showAlert('⚠️','Not Logged In','Please login first.'); return; }
    const location = document.getElementById('orderLocation').value.trim();
    if (!location) { showAlert('⚠️','Location Missing','Please enter your pickup location.'); return; }
    const items = [];
    Object.entries(currentCart).forEach(([item, qty]) => { for (let i = 0; i < qty; i++) items.push(item); });
    if (items.length === 0) { showAlert('⚠️','Empty Cart','Please select at least one item.'); return; }

    const result = await apiFetch(`${API}/order/place`, {
        method: 'POST',
        body: JSON.stringify({ vendor: currentVendorKey, customer: user.name, location, items })
    });
    closeOrderModal();

    if (!result) { showAlert('❌','Server Error','Make sure java Server is running on port 8080.'); return; }

    // BUG 4 FIX: Handle vendor day ended response
    if (result.error === 'VENDOR_DAY_ENDED') {
        showAlert('🔴','Vendor Closed Early', result.message || 'This vendor has ended their day early. Please choose another vendor.');
        return;
    }

    if (!result.valid) { showAlert('❌','Invalid Items','Some items are not on this vendor\'s menu.'); return; }

    // BUG 5 FIX: Use etaToYourLocation (ETA to CUSTOMER's location) not next-stop transit time
    const etaToCustomer = result.etaToYourLocation;
    const vendorStat    = result.vendorStatus || 'ON-SITE';
    const vendorCurStop = result.vendorCurrentStop || '';

    let etaMsg = '';
    if (etaToCustomer > 0) {
        etaMsg = `\n\n⏱️ Estimated time for vendor to reach your location: ~${etaToCustomer} min`;
        if (vendorStat === 'IN-TRANSIT') {
            etaMsg += `\n🚚 Vendor is currently in transit from ${vendorCurStop}`;
        }
    } else if (vendorStat === 'ON-SITE' && vendorCurStop.toLowerCase() === location.toLowerCase()) {
        etaMsg = `\n\n✅ Vendor is already at your location! Head over now.`;
    }

    // BUG 1 FIX: Store order history per user name — never mix between users
    const historyKey = 'orderHistory_' + (user.name || 'guest');
    const history = JSON.parse(localStorage.getItem(historyKey) || '[]');
    history.unshift({ orderId: result.orderId, vendorKey: currentVendorKey, status: result.status, bill: result.bill, items });
    localStorage.setItem(historyKey, JSON.stringify(history.slice(0,20)));

    refreshWalletDisplay();
    showAlert('✅','Order Placed!',
        `Bill: ₹${result.bill}\nToken deposit locked: ₹${Math.floor(result.bill/4)}\nOrder ID: ${result.orderId.substring(0,12)}...\nWallet: ₹${result.walletBalance}${etaMsg}`);
}

// ── MY ORDERS TAB ─────────────────────────────────────────────────
async function loadMyOrders() {
    const list = document.getElementById('myOrdersList');
    if (!list) return;
    const user = getUser();
    // BUG 1 FIX: Always use per-user key for history lookup
    const historyKey = 'orderHistory_' + (user ? user.name : 'guest');
    const history = JSON.parse(localStorage.getItem(historyKey) || '[]');
    if (history.length === 0) {
        list.innerHTML = "<p style='color:#888'>No orders yet. Go to Live Stalls to order!</p>"; return;
    }
    list.innerHTML = "<p style='color:#888;margin-bottom:12px'>Fetching latest status...</p>";
    let html = '';
    for (const h of history) {
        const data   = await apiFetch(`${API}/order/status?id=${h.orderId}`);
        const status = data ? data.status : h.status;
        const bill   = data ? data.bill   : h.bill;

        // BUG 6 FIX: Show "I have arrived" button when status is READY (customer goes first)
        // Show "Waiting for vendor" message when customer already confirmed
        let actionHtml = '';
        if (status === 'READY') {
            const custReady = data && data.customerReady;
            if (custReady) {
                actionHtml = `<div style="margin-top:10px;padding:10px;background:rgba(46,204,113,0.1);border:1px solid #2ecc71;border-radius:8px;color:#2ecc71;font-size:0.9rem;font-weight:600">
                    ✅ You confirmed arrival — waiting for vendor to complete handoff
                </div>`;
            } else {
                actionHtml = `<button class="collect-btn" onclick="customerConfirmArrival('${h.orderId}')">
                    🙋 I have arrived — Collect My Order
                </button>`;
            }
        } else if (status === 'COLLECTED') {
            actionHtml = `<div style="margin-top:10px;padding:8px 12px;background:rgba(150,150,150,0.1);border-radius:8px;color:#888;font-size:0.9rem">
                ✅ Order collected successfully
            </div>`;
        }

        html += `
            <div class="my-orders-card">
                <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px">
                    <span style="color:#ff9f43;font-size:0.85rem;font-weight:600">#${h.orderId.substring(0,12)}...</span>
                    <span class="order-status-pill pill-${status}">${status}</span>
                </div>
                <div style="color:white;margin:8px 0;font-size:1rem;line-height:1.6">${aggregateItems(h.items)}</div>
                <div style="color:#2ecc71;font-weight:700;font-size:1.1rem;margin:6px 0">₹${bill}</div>
                ${actionHtml}
            </div>`;
    }
    list.innerHTML = html;
}

// BUG 6 FIX: Customer confirms they've arrived — vendor can then complete collection
async function customerConfirmArrival(orderId) {
    const result = await apiFetch(`${API}/order/customer_ready`, {
        method: 'POST',
        body: JSON.stringify({ orderId })
    });
    if (result && result.customerReady) {
        showAlert('✅','Arrival Confirmed!','Great! The vendor has been notified and will complete the handoff shortly.');
        loadMyOrders();
    } else if (result && result.error) {
        showAlert('⚠️','Not Ready Yet', result.error);
    }
}

// ── REVIEWS TAB ───────────────────────────────────────────────────
async function loadReviewVendors() {
    const sel     = document.getElementById('reviewVendorKey');
    const readSel = document.getElementById('readReviewVendorKey');
    if (!sel && !readSel) return;
    const vendors = await apiFetch(`${API}/vendors`);
    if (vendors) {
        vendors.forEach(v => {
            if (sel) {
                const opt = document.createElement('option');
                opt.value = v.key; opt.innerText = v.name;
                sel.appendChild(opt);
            }
            if (readSel) {
                const opt = document.createElement('option');
                opt.value = v.key; opt.innerText = v.name;
                readSel.appendChild(opt);
            }
        });
    }
    loadVendorReviews();
}

async function loadVendorReviews() {
    const sel = document.getElementById('readReviewVendorKey');
    const container = document.getElementById('reviewsList');
    if (!sel || !container) return;

    const vkey = sel.value;
    if (!vkey) return;

    container.innerHTML = `<p style="color:#888">Loading reviews...</p>`;
    const reviews = await apiFetch(`${API}/review/get?vendor=${encodeURIComponent(vkey)}`);

    if (!reviews || reviews.length === 0) {
        container.innerHTML = `<p style="color:#888">No reviews yet for this vendor.</p>`;
        return;
    }

    container.innerHTML = reviews.map(r => {
        const stars = '★'.repeat(r.rating) + '☆'.repeat(5 - r.rating);
        return `
            <div style="border:1px solid rgba(255,255,255,0.1);border-radius:10px;padding:14px;margin-bottom:12px;background:rgba(255,255,255,0.04)">
                <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
                    <span style="font-weight:600;color:white">${r.customer}</span>
                    <span style="color:#f9ca24;font-size:1.1rem;letter-spacing:2px">${stars}</span>
                </div>
                <p style="color:#ccc;margin:0;font-size:0.92rem">${r.comment || '<em style="color:#666">No comment</em>'}</p>
            </div>`;
    }).join('');
}

document.querySelectorAll('.star').forEach(star => {
    star.onclick = () => {
        const val = parseInt(star.dataset.v);
        document.getElementById('reviewRating').value = val;
        document.querySelectorAll('.star').forEach(s => {
            s.classList.toggle('active', parseInt(s.dataset.v) <= val);
        });
    };
});

async function submitReview() {
    const user    = getUser();
    const vkey    = document.getElementById('reviewVendorKey')?.value;
    const rating  = document.getElementById('reviewRating')?.value;
    const comment = document.getElementById('reviewComment')?.value.trim();
    if (!vkey) { showAlert('⚠️','Select a Vendor','Please select a vendor first.'); return; }

    const result = await apiFetch(`${API}/review/add`, {
        method: 'POST',
        body: JSON.stringify({ vendor: vkey, customer: user ? user.name : 'Anonymous', rating, comment })
    });
    if (result) {
        const msg = document.getElementById('reviewMsg');
        if (msg) { msg.innerText = `Review submitted! Avg rating: ${result.avgRating} ⭐`; msg.style.display = ''; }
        setTimeout(() => { if (msg) msg.style.display = 'none'; }, 3000);
    }
}

function aggregateItems(itemsArray) {
    const counts = {};
    (itemsArray || []).forEach(item => { counts[item] = (counts[item] || 0) + 1; });
    return Object.entries(counts).map(([item, qty]) => qty > 1 ? `${item} × ${qty}` : item).join(' &nbsp;·&nbsp; ');
}

// Init customer dashboard
if (vendorGrid) {
    const user = getUser();
    if (user) {
        const el = document.getElementById('displayUserName');
        const gr = document.getElementById('welcomeGreeting');
        if (el) el.innerText = user.name;
        if (gr) gr.innerText = `Welcome, ${user.name}! 👋`;
    }
    renderVendors();
    refreshWalletDisplay();
    if (searchInput) searchInput.addEventListener('input', e => renderVendors(e.target.value));
}

// ══════════════════════════════════════════════════════════════════
//  Vendor Dashboard (vendor-dashboard.html)
// ══════════════════════════════════════════════════════════════════
const routeContainer = document.getElementById('vertical-route');
const orderContainer = document.getElementById('v-queue');
const departBtn      = document.getElementById('v-depart');
const reachBtn       = document.getElementById('v-reach');

let vendorKey  = null;

function showSection(name, el) {
    document.querySelectorAll('[id^="section-"]').forEach(s => s.classList.add('section-hidden'));
    const sec = document.getElementById('section-'+name);
    if (sec) sec.classList.remove('section-hidden');
    document.querySelectorAll('.v-nav li').forEach(li => li.classList.remove('active'));
    if (el) el.classList.add('active');
    if (name === 'orders') loadFullOrders();
}

async function loadVendorDashboard() {
    const user = getUser();
    if (!user || user.type !== 'vendor') return;
    vendorKey = user.vendorKey;

    const bizEl = document.getElementById('v-biz-name');
    const welEl = document.getElementById('v-welcome');
    const keyEl = document.getElementById('v-key-display');
    if (bizEl) bizEl.innerText = user.bizName || user.name;
    if (welEl) welEl.innerText = `Welcome, ${user.name}`;
    if (keyEl) keyEl.innerText = `Key: ${vendorKey}`;

    await refreshVendorStatus();
    await refreshOrders();
    await refreshVendorWallet();

    // BUG 4 FIX: Render end-day button
    renderEndDayButton();

    setInterval(async () => {
        await refreshOrders();
        await refreshVendorWallet();
        const sec = document.getElementById('section-orders');
        if (sec && !sec.classList.contains('section-hidden')) {
            await loadFullOrders();
        }
    }, 4000);
}

// BUG 4 FIX: End Day Early button — renders in vendor header
function renderEndDayButton() {
    const container = document.getElementById('endDayContainer');
    if (!container) return;
    container.innerHTML = `
        <button id="endDayBtn" onclick="endDayEarly()"
            style="background:#e74c3c;color:white;border:none;padding:10px 20px;border-radius:8px;
                   font-weight:700;cursor:pointer;font-family:Poppins,sans-serif;font-size:0.9rem;
                   display:flex;align-items:center;gap:8px">
            🔴 End Day Early
        </button>`;
}

async function endDayEarly() {
    if (!confirm('Are you sure you want to end your day early?\n\nCustomers will be notified that you are no longer accepting orders.')) return;
    const result = await apiFetch(`${API}/vendor/end_day`, {
        method: 'POST',
        body: JSON.stringify({ vendor: vendorKey })
    });
    if (result && result.dayEnded) {
        const btn = document.getElementById('endDayBtn');
        if (btn) {
            btn.disabled = true;
            btn.style.background = '#7f8c8d';
            btn.innerHTML = '🔴 Day Ended — Customers Notified';
        }
        showAlert('🔴','Day Ended Early','Your day has been ended. Customers on the dashboard will now see that you are closed. No new orders will be accepted.');
    }
}

async function refreshVendorStatus() {
    if (!vendorKey) return;
    const v = await apiFetch(`${API}/vendor/status?vendor=${vendorKey}`);
    if (v && routeContainer) {
        renderRoute(v);
        if (departBtn && reachBtn) {
            if (v.status === 'DAY-ENDED') {
                // Day is over — hide both buttons, show end-of-day message
                departBtn.style.display = 'none';
                reachBtn.style.display  = 'none';
                const routeActions = document.getElementById('routeActions');
                if (routeActions && !document.getElementById('dayOverMsg')) {
                    routeActions.innerHTML += `<div id="dayOverMsg" style="margin-top:14px;padding:12px 16px;background:rgba(231,76,60,0.15);border:1px solid #e74c3c;border-radius:10px;color:#e74c3c;font-weight:700;text-align:center">
                        🏁 All stops completed — Day Ended
                    </div>`;
                }
            } else if (v.status === 'IN-TRANSIT') {
                departBtn.style.display = 'none';
                reachBtn.style.display  = 'block';
            } else {
                // ON-SITE — check if this is the last stop; if so label depart as "End Day"
                reachBtn.style.display  = 'none';
                departBtn.style.display = 'block';
                if (v.nextStop === 'End of Day') {
                    departBtn.innerText = '🏁 Depart & End Day';
                } else {
                    departBtn.innerText = '🚗 Depart Current Location';
                }
            }
        }
    }
}

// STOP SKIP FIX:
// transit() now advances current_stop to the DESTINATION (the stop being headed to).
// move() just sets reached=true without advancing again.
// So the mapping is:
//   ON-SITE:    currentStop = where vendor IS right now
//   IN-TRANSIT: currentStop = the stop vendor is heading TO (already advanced)
//               nextStop    = the stop AFTER the destination (not the destination itself)
function renderRoute(v) {
    routeContainer.innerHTML = '';
    const inTransit = v.status === 'IN-TRANSIT';
    (v.stops || []).forEach(stop => {
        // ON-SITE:    highlight currentStop as "you are here"
        // IN-TRANSIT: highlight currentStop as "heading here" (it IS the destination now)
        const isHere        = !inTransit && stop === v.currentStop;
        const isDestination =  inTransit && stop === v.currentStop;

        let label = '';
        if (isHere)        label = ' <small style="color:#2ecc71">✅ You are here</small>';
        if (isDestination) label = ' <small style="color:#3498db">🎯 Heading here</small>';
        // Mark last stop visually
        if (isHere && v.nextStop === 'End of Day') label += ' <small style="color:#e74c3c">— Last Stop</small>';

        const activeClass = (isHere || isDestination) ? 'active' : '';
        routeContainer.innerHTML += `
            <div class="route-stop ${activeClass}">
                <div class="stop-dot ${isDestination ? 'dot-transit' : ''} ${isHere ? 'dot-here' : ''}"></div>
                <div class="stop-label">${stop}${label}</div>
            </div>`;
    });
}

async function refreshVendorWallet() {
    const walletEl = document.getElementById('v-wallet');
    if (!walletEl || !vendorKey) return;
    const w = await apiFetch(`${API}/wallet/balance?customer=${encodeURIComponent(vendorKey)}`);
    if (w) walletEl.innerText = w.balance;
}

// BUG 3 FIX: Vendor queue — completely redesigned for structure & spaciousness
async function refreshOrders() {
    if (!vendorKey || !orderContainer) return;
    const data = await apiFetch(`${API}/vendor/orders?vendor=${vendorKey}`);
    if (!data) return;
    const all = [...(data.pending || []), ...(data.ready || [])];
    const countEl = document.getElementById('order-count');
    if (countEl) countEl.innerText = all.length;

    orderContainer.innerHTML = '';
    if (all.length === 0) {
        orderContainer.innerHTML = `
            <div style="text-align:center;padding:40px 20px;color:#666">
                <div style="font-size:2.5rem;margin-bottom:12px">🍽️</div>
                <p style="font-size:1rem">No active orders right now</p>
                <p style="font-size:0.85rem;color:#555;margin-top:4px">New orders will appear here automatically</p>
            </div>`;
        return;
    }
    all.forEach(o => {
        let actions = '';
        if (o.status === 'PENDING') {
            actions = `
                <div class="order-actions">
                    <button class="btn-accept"  onclick="acceptOrder('${o.orderId}',true)">✓ Accept</button>
                    <button class="btn-decline" onclick="acceptOrder('${o.orderId}',false)">✗ Decline</button>
                </div>`;
        } else if (o.status === 'ACCEPTED') {
            actions = `<div class="order-actions"><button class="btn-done" onclick="markReady('${o.orderId}')">🍽️ Mark Ready</button></div>`;
        } else if (o.status === 'READY') {
            // BUG 6 FIX: Only show "Collected" button when customer has confirmed arrival
            if (o.customerReady) {
                actions = `
                    <div style="margin-bottom:8px;padding:8px 12px;background:rgba(46,204,113,0.15);border:1px solid #2ecc71;border-radius:8px;color:#2ecc71;font-size:0.85rem;font-weight:600">
                        ✅ Customer has confirmed arrival
                    </div>
                    <div class="order-actions"><button class="btn-collect" onclick="markCollected('${o.orderId}')">✅ Complete Handoff</button></div>`;
            } else {
                actions = `
                    <div style="padding:8px 12px;background:rgba(255,159,67,0.1);border:1px solid rgba(255,159,67,0.4);border-radius:8px;color:#ff9f43;font-size:0.85rem;font-weight:600">
                        ⏳ Waiting for customer to confirm arrival…
                    </div>`;
            }
        }

        // BUG 3 FIX: Much more spacious, structured order card layout
        orderContainer.innerHTML += `
            <div class="q-item">
                <div class="q-item-header">
                    <span class="q-item-id">#${o.orderId.substring(0,10)}</span>
                    <span class="status-pill pill-${o.status}">${o.status}</span>
                </div>
                <div class="q-divider"></div>
                <div class="q-row">
                    <span class="q-label">👤 Customer</span>
                    <span class="q-value">${o.customer}</span>
                </div>
                <div class="q-row">
                    <span class="q-label">📍 Pickup</span>
                    <span class="q-value">${o.location}</span>
                </div>
                <div class="q-row">
                    <span class="q-label">🛒 Items</span>
                    <span class="q-value">${aggregateItems(o.items)}</span>
                </div>
                <div class="q-row">
                    <span class="q-label">💰 Bill</span>
                    <span class="q-value q-bill">₹${o.bill}</span>
                </div>
                <div class="q-divider"></div>
                ${actions}
            </div>`;
    });
}

// BUG 7 FIX: Full orders list — more spacious layout
async function loadFullOrders() {
    if (!vendorKey) return;
    const data = await apiFetch(`${API}/vendor/orders?vendor=${vendorKey}`);
    if (!data) return;
    const renderList = (orders, containerId) => {
        const el = document.getElementById(containerId);
        if (!el) return;
        if (!orders.length) {
            el.innerHTML = `<div style="padding:24px;text-align:center;color:#666;border:1px dashed #333;border-radius:12px">None</div>`;
            return;
        }
        el.innerHTML = orders.map(o => `
            <div class="q-item" style="margin-bottom:20px">
                <div class="q-item-header">
                    <span class="q-item-id">#${o.orderId.substring(0,10)}</span>
                    <span class="status-pill pill-${o.status}">${o.status}</span>
                </div>
                <div class="q-divider"></div>
                <div class="q-row">
                    <span class="q-label">👤 Customer</span>
                    <span class="q-value">${o.customer}</span>
                </div>
                <div class="q-row">
                    <span class="q-label">📍 Pickup</span>
                    <span class="q-value">${o.location}</span>
                </div>
                <div class="q-row">
                    <span class="q-label">🛒 Items</span>
                    <span class="q-value">${aggregateItems(o.items)}</span>
                </div>
                <div class="q-row">
                    <span class="q-label">💰 Bill</span>
                    <span class="q-value q-bill">₹${o.bill}</span>
                </div>
            </div>`).join('');
    };
    renderList(data.pending || [], 'fullPendingList');
    renderList(data.ready   || [], 'fullReadyList');
}

async function acceptOrder(orderId, choice) {
    const result = await apiFetch(`${API}/vendor/accept`, {
        method: 'POST',
        body: JSON.stringify({ vendor: vendorKey, orderId, choice: String(choice) })
    });
    if (result && result.refundedBalance !== undefined) {
        showAlert('↩️', 'Order Declined', `Customer has been refunded.\nTheir new wallet balance: ₹${result.refundedBalance}`);
    }
    await refreshOrders();
    const sec = document.getElementById('section-orders');
    if (sec && !sec.classList.contains('section-hidden')) await loadFullOrders();
}

async function markReady(orderId) {
    await apiFetch(`${API}/vendor/food_ready`, {
        method: 'POST',
        body: JSON.stringify({ vendor: vendorKey, orderId })
    });
    await refreshOrders();
    const sec = document.getElementById('section-orders');
    if (sec && !sec.classList.contains('section-hidden')) await loadFullOrders();
}

// BUG 6 FIX: markCollected is called by vendor — backend enforces customer must confirm first
async function markCollected(orderId) {
    const result = await apiFetch(`${API}/order/collect`, {
        method: 'POST',
        body: JSON.stringify({ vendor: vendorKey, orderId })
    });
    if (result && result.error === 'CUSTOMER_NOT_READY') {
        showAlert('⏳','Customer Not Ready', 'The customer has not confirmed arrival yet. Please wait for them to confirm.');
        return;
    }
    await refreshOrders();
    await refreshVendorWallet();
    const sec = document.getElementById('section-orders');
    if (sec && !sec.classList.contains('section-hidden')) await loadFullOrders();
}

let moving = false;

if (departBtn) {
    departBtn.onclick = async () => {
        if (moving) return;
        if (!confirm('Mark as DEPARTED from current stop?')) return;
        moving = true;
        departBtn.disabled = true;
        await apiFetch(`${API}/vendor/transit?vendor=${vendorKey}`);
        departBtn.style.display = 'none';
        departBtn.disabled = false;
        reachBtn.style.display  = 'block';
        moving = false;
        await refreshVendorStatus();
    };
}
if (reachBtn) {
    reachBtn.onclick = async () => {
        if (moving) return;
        if (!confirm('Have you REACHED the next stop?')) return;
        moving = true;
        reachBtn.disabled = true;
        await apiFetch(`${API}/vendor/move?vendor=${vendorKey}`);
        reachBtn.style.display  = 'none';
        reachBtn.disabled = false;
        departBtn.style.display = 'block';
        moving = false;
        await refreshVendorStatus();
    };
}

if (routeContainer) loadVendorDashboard();
