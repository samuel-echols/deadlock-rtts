import { useEffect, useState } from 'react'
import { api } from './api'
import type { MoverPoint } from './api'

function fmt(v: number) { return `${(v * 100).toFixed(2)}%` }
function delta(v: number) {
  const s = `${v >= 0 ? '+' : ''}${(v * 100).toFixed(2)}%`
  return <span className={v > 0 ? 'positive' : v < 0 ? 'negative' : 'neutral'}>{s}</span>
}

export default function MoversPage() {
  const [data, setData] = useState<MoverPoint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api.movers(30)
      .then(setData)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  return (
    <main>
      <h1>Biggest Movers</h1>
      <p style={{ color: '#718096', marginBottom: '1rem', fontSize: '0.875rem' }}>
        Heroes with the largest win-rate change compared to the previous patch.
      </p>

      {loading && <p className="loading">Loading…</p>}
      {error && <p className="error">Error: {error}</p>}

      {!loading && !error && data.length === 0 && (
        <p className="neutral" style={{ padding: '2rem 0' }}>
          No mover data yet — requires at least two patches of snapshots.
        </p>
      )}

      {!loading && !error && data.length > 0 && (
        <div className="card">
          <table>
            <thead>
              <tr>
                <th>Hero</th>
                <th>Patch</th>
                <th>Prev Win Rate</th>
                <th>Current Win Rate</th>
                <th>Δ Win Rate</th>
              </tr>
            </thead>
            <tbody>
              {data.map(m => (
                <tr key={`${m.heroId}-${m.patchId}`}>
                  <td>{m.displayName}</td>
                  <td style={{ color: '#718096' }}>{m.patchId}</td>
                  <td>{fmt(m.prevWinRate)}</td>
                  <td>{fmt(m.winRate)}</td>
                  <td>{delta(m.winRateDelta)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </main>
  )
}
