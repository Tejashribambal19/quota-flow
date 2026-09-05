import { useEffect, useState } from 'react'
import { api } from './api'
import './App.css'

const TYPES = ['API_REQUEST', 'STORAGE_MB', 'COMPUTE_SECOND', 'BACKGROUND_JOB']
const label = value => (value || '').replaceAll('_', ' ')
const money = value => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(Number(value || 0))
const number = value => new Intl.NumberFormat('en-IN').format(Number(value || 0))

export default function App() {
  const [user, setUser] = useState(() => JSON.parse(sessionStorage.getItem('quota-user') || 'null'))
  const login = data => { sessionStorage.setItem('quota-user', JSON.stringify(data)); setUser(data) }
  const logout = () => { sessionStorage.removeItem('quota-user'); setUser(null) }
  if (!user) return <Login onLogin={login} />
  return user.role === 'PLATFORM_ADMIN'
    ? <Platform user={user} logout={logout} />
    : <Tenant user={user} logout={logout} />
}

function Login({ onLogin }) {
  const [email, setEmail] = useState('admin@abclogistics.com')
  const [password, setPassword] = useState('Tenant@12345')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('')
    try { onLogin(await api('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })) }
    catch (e) { setError(e.message) } finally { setBusy(false) }
  }
  return <main className="login-page">
    <section className="hero"><b className="logo">QUOTA FLOW</b><span>RESOURCE INTELLIGENCE PLATFORM</span><h1>Fair usage.<br />Clear costs.<br /><em>Zero surprises.</em></h1><p>Real-time metering, quota enforcement and transparent billing for every SaaS tenant.</p></section>
    <section className="login-side"><form className="login-card" onSubmit={submit}><span className="blue">WELCOME BACK</span><h2>Sign in to your workspace</h2><p>Use your platform or tenant administrator account.</p><label>Email<input type="email" value={email} onChange={e => setEmail(e.target.value)} /></label><label>Password<input type="password" value={password} onChange={e => setPassword(e.target.value)} /></label>{error && <div className="error">{error}</div>}<button disabled={busy}>{busy ? 'Connecting…' : 'Sign in securely →'}</button><small>Protected by JWT and role-based access control.</small></form></section>
  </main>
}

function Layout({ user, logout, title, subtitle, children }) {
  return <div className="shell"><aside><b className="logo">QUOTA FLOW</b><span>{user.role === 'PLATFORM_ADMIN' ? 'ADMIN CONSOLE' : 'TENANT PORTAL'}</span><nav><a href="#overview">Overview</a><a href="#management">Management</a><a href="#usage">Usage</a><a href="#billing">Billing</a></nav><div className="online">● Cloud services online</div></aside><section className="workspace"><header><div></div><b>{user.fullName}</b><small>{label(user.role)}</small><button className="logout" onClick={logout}>Log out</button></header><main><div className="heading"><div><h1>{title}</h1><p>{subtitle}</p></div></div>{children}</main></section></div>
}

function Platform({ user, logout }) {
  const [plans, setPlans] = useState([]), [tenants, setTenants] = useState([]), [audit, setAudit] = useState(null)
  const [message, setMessage] = useState('')
  const [plan, setPlan] = useState({ name: '', description: '', monthlyPrice: '', API_REQUEST: '', STORAGE_MB: '', COMPUTE_SECOND: '', BACKGROUND_JOB: '' })
  const [tenant, setTenant] = useState({ name: '', slug: '', planId: '', billingCycleDay: 1 })
  async function load() {
    try { const [p, t, a] = await Promise.all([api('/plans', {}, user.token), api('/tenants', {}, user.token), api('/audit/verify', {}, user.token)]); setPlans(p); setTenants(t); setAudit(a); if (!tenant.planId && p[0]) setTenant(x => ({ ...x, planId: p[0].id })) }
    catch (e) { setMessage(e.message) }
  }
  useEffect(() => { load() }, [])
  async function createPlan(e) { e.preventDefault(); try { await api('/plans', { method: 'POST', body: JSON.stringify({ name: plan.name, description: plan.description, monthlyPrice: Number(plan.monthlyPrice), quotas: TYPES.map(resourceType => ({ resourceType, hardLimit: Number(plan[resourceType]), warningPercentage: 80, criticalPercentage: 90 })) }) }, user.token); setMessage('Plan created successfully'); await load() } catch (x) { setMessage(x.message) } }
  async function createTenant(e) { e.preventDefault(); try { await api('/tenants', { method: 'POST', body: JSON.stringify({ ...tenant, billingCycleDay: Number(tenant.billingCycleDay) }) }, user.token); setMessage('Tenant created successfully'); await load() } catch (x) { setMessage(x.message) } }
  return <Layout user={user} logout={logout} title="Platform administration" subtitle="Manage tenants, plans and platform compliance"><div className="top-action"><button onClick={load}>↻ Refresh data</button></div>{message && <div className="notice">{message}</div>}<section className="stats" id="overview"><Stat name="Subscription plans" value={plans.length} /><Stat name="Total tenants" value={tenants.length} /><Stat name="Active tenants" value={tenants.filter(x => x.status === 'ACTIVE').length} /></section><h2 id="management">Platform management</h2><section className="forms"><form className="card" onSubmit={createPlan}><h3>Create subscription plan</h3><input required placeholder="Plan name" onChange={e => setPlan({ ...plan, name: e.target.value })} /><input placeholder="Description" onChange={e => setPlan({ ...plan, description: e.target.value })} /><input required type="number" min="0" placeholder="Monthly price" onChange={e => setPlan({ ...plan, monthlyPrice: e.target.value })} />{TYPES.map(x => <input required type="number" min="1" key={x} placeholder={label(x) + ' limit'} onChange={e => setPlan({ ...plan, [x]: e.target.value })} />)}<button>Create plan</button></form><form className="card" onSubmit={createTenant}><h3>Create tenant</h3><input required placeholder="Company name" onChange={e => setTenant({ ...tenant, name: e.target.value })} /><input required pattern="[a-z0-9-]+" placeholder="Slug, for example nova-tech" onChange={e => setTenant({ ...tenant, slug: e.target.value })} /><select required value={tenant.planId} onChange={e => setTenant({ ...tenant, planId: e.target.value })}><option value="">Select plan</option>{plans.map(x => <option key={x.id} value={x.id}>{x.name}</option>)}</select><input type="number" min="1" max="28" value={tenant.billingCycleDay} onChange={e => setTenant({ ...tenant, billingCycleDay: e.target.value })} /><button>Create tenant</button></form></section><h2>Audit-chain verification</h2><div className="card audit"><Badge level={audit?.valid ? 'NORMAL' : 'CRITICAL'} text={audit?.valid ? 'AUDIT CHAIN VALID' : 'CHECK FAILED'} /><p>{audit?.message || 'Checking integrity…'}</p></div><h2>Registered tenants</h2><div className="card table"><table><thead><tr><th>Tenant</th><th>Slug</th><th>Plan</th><th>Billing day</th><th>Status</th></tr></thead><tbody>{tenants.map(x => <tr key={x.id}><td>{x.name}</td><td>{x.slug}</td><td>{x.planName}</td><td>{x.billingCycleDay}</td><td><Badge level="NORMAL" text={x.status} /></td></tr>)}</tbody></table></div><h2 id="billing">Subscription plans</h2><section className="plan-list">{plans.map(x => <div className="card plan-card" key={x.id}><h3>{x.name}</h3><p>{x.description}</p><strong>{money(x.monthlyPrice)} / month</strong>{x.quotas.map(q => <small key={q.id}>{label(q.resourceType)}: {number(q.hardLimit)}</small>)}</div>)}</section></Layout>
}

function Tenant({ user, logout }) {
  const [summary, setSummary] = useState(null), [billing, setBilling] = useState(null), [type, setType] = useState('API_REQUEST'), [quantity, setQuantity] = useState(10), [message, setMessage] = useState('')
  async function load() { try { const [s, b] = await Promise.all([api('/reports/tenants/' + user.tenantId + '/usage', {}, user.token), api('/reports/tenants/' + user.tenantId + '/billing', {}, user.token)]); setSummary(s); setBilling(b) } catch (e) { setMessage(e.message) } }
  useEffect(() => { load() }, [])
  async function simulate(e) { e.preventDefault(); try { const result = await api('/usage/' + user.tenantId + '/consume', { method: 'POST', body: JSON.stringify({ resourceType: type, quantity: Number(quantity), requestId: 'web-' + Date.now() }) }, user.token); setMessage(result.message) } catch (x) { setMessage(x.message) } finally { await load() } }
  const alerts = summary?.resources?.filter(x => x.level !== 'NORMAL') || []
  return <Layout user={user} logout={logout} title={summary?.tenantName || 'Tenant dashboard'} subtitle={summary ? summary.planName + ' Plan • Billing month ' + summary.billingMonth : 'Loading cloud data…'}><div className="top-action"><button onClick={load}>↻ Refresh data</button></div>{message && <div className="notice">{message}</div>}<section className="stats" id="overview"><Stat name="Monthly plan" value={money(summary?.monthlyPrice)} /><Stat name="Utilized value" value={money(billing?.utilizedValue)} /><Stat name="Signed in as" value="TENANT ADMIN" /></section><h2 id="management">Live usage simulator</h2><form className="card simulator" onSubmit={simulate}><div><h3>Simulate customer activity</h3><p>Update quota consumption in real time.</p></div><select value={type} onChange={e => setType(e.target.value)}>{TYPES.map(x => <option key={x}>{x}</option>)}</select><input type="number" min="1" value={quantity} onChange={e => setQuantity(e.target.value)} /><button>Simulate usage</button></form><h2>Live alerts</h2>{alerts.length ? alerts.map(x => <div className={'alert ' + x.level.toLowerCase()} key={x.resourceType}>⚠ <b>{x.level}: {label(x.resourceType)}</b> is {x.percentage.toFixed(1)}% used. {number(Math.max(0, x.limit - x.used))} units remain.</div>) : <div className="alert normal">✓ All resources are within normal limits.</div>}<h2 id="usage">Resource usage</h2><section className="resources">{summary?.resources?.map(x => <Resource data={x} key={x.resourceType} />)}</section><h2>Usage analytics</h2><div className="card chart">{summary?.resources?.map(x => <div className="column" key={x.resourceType}><div className="track"><i className={x.level.toLowerCase()} style={{ height: Math.min(100, x.percentage) + '%' }}><b>{x.percentage.toFixed(1)}%</b></i></div><small>{label(x.resourceType)}</small></div>)}</div><h2 id="billing">Billing summary</h2><div className="card invoice"><span>Invoice <b>{billing?.invoiceNumber}</b></span><span>Subscription <b>{billing?.planName}</b></span><span>Resource value utilized <b>{money(billing?.utilizedValue)}</b></span><strong>Total payable: {money(billing?.totalPayable)}</strong><div><Badge level="NORMAL" text={billing?.status || 'CURRENT'} /><button onClick={() => window.print()}>Print / Save PDF</button></div></div></Layout>
}

function Stat({ name, value }) { return <div className="card stat"><span>{name}</span><b>{value}</b></div> }
function Badge({ level, text }) { return <span className={'badge ' + level.toLowerCase()}>{text || level}</span> }
function Resource({ data }) { return <div className="card resource"><div><h3>{label(data.resourceType)}</h3><Badge level={data.level} /></div><strong>{number(data.used)} <small>/ {number(data.limit)}</small></strong><div className="progress"><i className={data.level.toLowerCase()} style={{ width: Math.min(100, data.percentage) + '%' }} /></div><p>{data.percentage.toFixed(1)}% used</p></div> }
