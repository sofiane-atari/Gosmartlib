export interface GamificationNotification {
  id: number;
  type: GamificationNotificationType;
  title: string;
  message: string;
  assetKey: string;
  themeColor: string;
  rarity: GamificationNotificationRarity;
  rewardPoints: number;
  createdAt: string;
}

export type GamificationNotificationType = 'BADGE_UNLOCKED';

export type GamificationNotificationRarity =
  | 'COMMON'
  | 'RARE'
  | 'EPIC'
  | 'LEGENDARY';
