const API_URL = import.meta.env.VITE_API_URL || 'https://quota-flow.onrender.com/api'

export async function api(path, options = {}, token) {
  const response = await fetch(API_URL + path, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
      ...options.headers,
    },
  })
  const text = await response.text()
  let data = null
  if (text) {
    try { data = JSON.parse(text) } catch { data = { message: text } }
  }
  if (!response.ok) throw new Error(data?.message || 'Request failed (' + response.status + ')')
  return data
}
