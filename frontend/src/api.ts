export interface HeroTrendPoint {
  date: string
  patchId: number
  buildNumber: number
  matches: number
  wins: number
  losses: number
  winRate: number
}

export interface ItemTrendPoint {
  date: string
  patchId: number
  buildNumber: number
  matches: number
  wins: number
  losses: number
  winRate: number
  avgBuyTimeS: number | null
}

export interface PatchDiffPoint {
  heroId: number
  displayName: string
  winRate: number
  prevWinRate: number | null
  delta: number | null
}

export interface MoverPoint {
  heroId: number
  displayName: string
  patchId: number
  winRate: number
  prevWinRate: number
  winRateDelta: number
}

async function get<T>(url: string): Promise<T> {
  const res = await fetch(url)
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  return res.json()
}

export const api = {
  heroTrend: (heroId: number) => get<HeroTrendPoint[]>(`/api/heroes/${heroId}/trend`),
  itemTrend: (itemId: number) => get<ItemTrendPoint[]>(`/api/items/${itemId}/trend`),
  patchDiff: (patchId: number) => get<PatchDiffPoint[]>(`/api/patches/${patchId}/diff`),
  movers: (limit = 20) => get<MoverPoint[]>(`/api/movers?limit=${limit}`),
}
