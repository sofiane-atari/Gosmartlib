export interface GamificationProfile {
  totalPoints: number;
  currentLevel: number;
  pointsUntilNextLevel: number;
  latestAchievements: GamificationBadge[];
  earnedBadges: GamificationBadge[];
  badgeProgress: GamificationProgress[];
  recentEvents: GamificationRecentEvent[];
}

export interface GamificationBadge {
  code: string;
  name: string;
  description: string;
  assetKey: string;
  lockedAssetKey: string;
  rarity: GamificationBadgeRarity;
  themeColor: string;
  category: GamificationBadgeCategory;
  rewardPoints: number;
  earned: boolean;
  earnedAt: string | null;
}

export interface GamificationProgress {
  badgeCode: string;
  badgeName: string;
  badgeDescription: string;
  assetKey: string;
  lockedAssetKey: string;
  rarity: GamificationBadgeRarity;
  themeColor: string;
  currentValue: number;
  targetValue: number;
  progressPercentage: number;
  completed: boolean;
}

export interface GamificationRecentEvent {
  type: GamificationEventType;
  points: number;
  reason: string;
  createdAt: string;
}

export type GamificationBadgeRarity = 'COMMON' | 'RARE' | 'EPIC' | 'LEGENDARY';

export type GamificationBadgeCategory =
  | 'READING'
  | 'REVIEWING'
  | 'CONSISTENCY'
  | 'EXPLORATION';

export type GamificationEventType =
  | 'BOOK_RETURNED'
  | 'BOOK_RETURNED_ON_TIME'
  | 'REVIEW_CREATED'
  | 'BADGE_REWARD';
