export interface HotspotDto {
  id: number;
  lat: number;
  lng: number;
  /** Mean cleanliness (0-100, 100 = clean) of the merged reports. */
  cleanliness: number;
  reportCount: number;
  status: "OPEN" | "ESCALATED" | "DISPATCHED" | "RESOLVED";
  label?: string;
  segmentName?: string;
  createdAt: string;
  lastUpdatedAt: string;
}

export interface StreetSegmentDto {
  id: number;
  name: string;
  district?: string;
  lat: number;
  lng: number;
  /** 0–100, 100 = clean; null until the street has been scored. */
  aiCleanlinessScore: number | null;
  recentReportCount: number;
  facultyShortCode?: string;
  facultyName?: string;
  facultyColor?: string;
  lastEvaluatedAt: string;
}

export interface FacultyDto {
  id: number;
  name: string;
  shortCode: string;
  color: string;
  emoji: string;
  members: number;
  points: number;
  territoryGeoJson?: string;
}

export interface LeaderboardEntry {
  id: number;
  name: string;
  shortCode: string;
  color: string;
  emoji: string;
  points: number;
  rank: number;
}

export interface WorkOrderDto {
  id: number;
  planonRef?: string;
  hotspotId?: number;
  status: "PENDING" | "DISPATCHED" | "IN_PROGRESS" | "COMPLETED" | "FAILED";
  priority: "LOW" | "MEDIUM" | "HIGH" | "URGENT";
  crew?: string;
  summary?: string;
  lat?: number;
  lng?: number;
  createdAt: string;
  dispatchedAt?: string;
  completedAt?: string;
}

export interface RedemptionDto {
  id: number;
  rewardId: number;
  title: string;
  sponsor: string;
  voucherCode: string;
  costPoints: number;
  redeemedAt: string;
}

export interface RedeemResponse {
  redemption: RedemptionDto;
  pointsLeft: number;
}

export interface RewardItemDto {
  id: number;
  title: string;
  description: string;
  sponsor: string;
  imageRef?: string;
  category: string;
  costPoints: number;
  stock: number;
}

export interface TimeBucket {
  /** ISO date (Europe/Brussels calendar day). */
  date: string;
  label: string;
  count: number;
}

export interface StatsDto {
  totalReports: number;
  openHotspots: number;
  dispatchedOrders: number;
  completedOrders: number;
  avgCleanliness: number;
  reportsLast7Days: TimeBucket[];
  topHotspots: HotspotDto[];
}


export interface FeedItemDto {
  id: number;
  username: string;
  facultyShortCode?: string;
  facultyEmoji?: string;
  facultyColor?: string;
  photoUrl?: string;
  lat?: number;
  lng?: number;
  segmentName?: string;
  cleanlinessScore?: number;
  userRating?: number;
  tags?: string;
  /** The AI inspector's one-line assessment, when it gave one. */
  description?: string;
  reportedAt: string;
}

export interface FeedPage {
  content: FeedItemDto[];
  totalElements: number;
  totalPages: number;
  number: number;
  last: boolean;
}

export type Role = "USER" | "CREW";

export interface AuthUser {
  id: number;
  email: string;
  facultyShortCode?: string;
  role: Role;
  /** Personal points, earned by their own reports and spent on rewards. */
  points: number;
  /** Only returned by login/register; kept client-side afterwards. */
  token: string;
}

export interface PredictPoint {
  lat: number;
  lng: number;
  likelihood: number;
}

export interface PredictionGrid {
  /** Leuven local time the grid is for. */
  at: string;
  /** False when the ML service is down; `points` is then empty. */
  available: boolean;
  points: PredictPoint[];
}

/** The AI's verdict on a photo sent from the browser. */
export interface DemoReportResult {
  reportId: number;
  hotspotId: number;
  photoUrl: string;
  /** False when the AI was unreachable and `score` is a placeholder. */
  aiAvailable: boolean;
  score: number;
  label: string;
  description?: string | null;
  tags: string[];
}

export interface DemoReportForm {
  photo: Blob;
  lat: number;
  lng: number;
  clan?: string;
}
