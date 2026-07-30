import { useEffect, useState } from 'react'
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  ReferenceLine, ResponsiveContainer, Legend,
} from 'recharts'
import { api } from './api'
import type { HeroTrendPoint } from './api'

const HERO_IDS = [1, 2, 3, 4, 6, 7, 8, 10, 11, 12, 15, 17, 18, 19, 20, 25, 27, 31, 35, 50]

function pct(v: number) { return `${(v * 100).toFixed(1)}%` }

export default function HeroTrendPage() {
  const [heroId, setHeroId] = useState(1)
  const [data, setData] = useState<HeroTrendPoint[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    setError(null)
    api.heroTrend(heroId)
      .then(setData)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [heroId])

  // find dates where patch changes so we can draw reference lines
  const patchLines = data.reduce<{ date: string; build: number }[]>((acc, pt, i) => {
    if (i === 0 || pt.buildNumber !== data[i - 1].buildNumber) {
      acc.push({ date: pt.date, build: pt.buildNumber })
    }
    return acc
  }, [])

  return (
    <main>
      <h1>Hero Win-Rate Trend</h1>
      <div className="card">
        <label style={{ marginRight: '0.75rem', color: '#a0aec0', fontSize: '0.9rem' }}>
          Hero ID
        </label>
        <select value={heroId} onChange={e => setHeroId(Number(e.target.value))}>
          {HERO_IDS.map(id => <option key={id} value={id}>Hero {id}</option>)}
        </select>
      </div>

      {loading && <p className="loading">Loading…</p>}
      {error && <p className="error">Error: {error}</p>}

      {!loading && !error && data.length === 0 && (
        <p className="neutral" style={{ padding: '2rem 0' }}>
          No data yet — the ingestion job runs daily at 04:00 UTC.
        </p>
      )}

      {!loading && !error && data.length > 0 && (
        <div className="card">
          <h2>Win Rate Over Time — Hero {heroId}</h2>
          <ResponsiveContainer width="100%" height={340}>
            <LineChart data={data} margin={{ top: 8, right: 24, bottom: 8, left: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#2d3748" />
              <XAxis dataKey="date" tick={{ fill: '#718096', fontSize: 11 }} />
              <YAxis
                tickFormatter={pct}
                domain={['auto', 'auto']}
                tick={{ fill: '#718096', fontSize: 11 }}
                width={52}
              />
              <Tooltip
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                formatter={(v: any) => pct(v as number)}
                contentStyle={{ background: '#1a1f2e', border: '1px solid #2d3748' }}
                labelStyle={{ color: '#e2e8f0' }}
              />
              <Legend />
              {patchLines.map(({ date, build }) => (
                <ReferenceLine
                  key={date}
                  x={date}
                  stroke="#f6e05e"
                  strokeDasharray="4 2"
                  label={{ value: `b${build}`, position: 'top', fill: '#f6e05e', fontSize: 10 }}
                />
              ))}
              <Line
                type="monotone"
                dataKey="winRate"
                name="Win Rate"
                stroke="#63b3ed"
                dot={false}
                strokeWidth={2}
              />
            </LineChart>
          </ResponsiveContainer>
          <p style={{ fontSize: '0.75rem', color: '#4a5568', marginTop: '0.5rem' }}>
            Yellow lines mark patch boundaries (build number).
          </p>
        </div>
      )}
    </main>
  )
}
