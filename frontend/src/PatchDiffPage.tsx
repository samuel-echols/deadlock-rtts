import { useState } from 'react'
import { api } from './api'
import type { PatchDiffPoint } from './api'

function fmt(v: number | null) { return v == null ? '—' : `${(v * 100).toFixed(2)}%` }
function delta(v: number | null) {
  if (v == null) return <span className="neutral">new</span>
  const s = `${v >= 0 ? '+' : ''}${(v * 100).toFixed(2)}%`
  return <span className={v > 0 ? 'positive' : v < 0 ? 'negative' : 'neutral'}>{s}</span>
}

export default function PatchDiffPage() {
  const [patchId, setPatchId] = useState('')
  const [inputVal, setInputVal] = useState('')
  const [data, setData] = useState<PatchDiffPoint[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function load() {
    const id = parseInt(inputVal)
    if (!id) return
    setPatchId(inputVal)
    setLoading(true)
    setError(null)
    api.patchDiff(id)
      .then(setData)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }

  return (
    <main>
      <h1>Patch Impact Report</h1>
      <p style={{ color: '#718096', marginBottom: '1rem', fontSize: '0.875rem' }}>
        Win-rate change for every hero compared to the previous patch.
      </p>

      <div className="card" style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
        <label style={{ color: '#a0aec0', fontSize: '0.9rem' }}>Patch ID</label>
        <input
          type="number"
          value={inputVal}
          onChange={e => setInputVal(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && load()}
          placeholder="e.g. 2"
          style={{
            background: '#2d3748', color: '#e2e8f0', border: '1px solid #4a5568',
            borderRadius: 4, padding: '0.4rem 0.75rem', fontSize: '0.9rem', width: 120,
          }}
        />
        <button
          onClick={load}
          style={{
            background: '#2b6cb0', color: '#fff', border: 'none', borderRadius: 4,
            padding: '0.4rem 1rem', cursor: 'pointer', fontSize: '0.9rem',
          }}
        >
          Load
        </button>
      </div>

      {loading && <p className="loading">Loading…</p>}
      {error && <p className="error">Error: {error}</p>}

      {!loading && !error && patchId && data.length === 0 && (
        <p className="neutral" style={{ padding: '2rem 0' }}>No data for patch {patchId}.</p>
      )}

      {!loading && !error && data.length > 0 && (
        <div className="card">
          <h2>Patch {patchId} — all heroes by win-rate delta</h2>
          <table>
            <thead>
              <tr>
                <th>Hero</th>
                <th>Prev Win Rate</th>
                <th>Current Win Rate</th>
                <th>Δ</th>
              </tr>
            </thead>
            <tbody>
              {data.map(d => (
                <tr key={d.heroId}>
                  <td>{d.displayName}</td>
                  <td>{fmt(d.prevWinRate)}</td>
                  <td>{fmt(d.winRate)}</td>
                  <td>{delta(d.delta)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
  )
}
