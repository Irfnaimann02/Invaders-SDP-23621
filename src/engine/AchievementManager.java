package engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Manages achievement progress and persistence. */
public class AchievementManager {

	/** Number of player kills required for Three Kills. */
	private static final int THREE_KILLS_TARGET = 3;
	/** Single-run score required for High Roller. */
	private static final int HIGH_SCORE_TARGET = 1000;

	/** Persistent player profile. */
	private PlayerProfile playerProfile;
	/** Achievements currently supported by the game. */
	private List<Achievement> achievements;

	/** Creates the manager and loads the saved player profile. */
	public AchievementManager() {
		try {
			this.playerProfile = Core.getFileManager().loadPlayerProfile();
		} catch (IOException | NumberFormatException e) {
			Core.getLogger().warning("Couldn't load player profile.");
			this.playerProfile = new PlayerProfile();
		}

		this.achievements = new ArrayList<Achievement>();
		addFirstKillAchievement();
		addHighScoreAchievement();
	}

	/** Adds the Three Kills achievement. */
	private void addFirstKillAchievement() {
		this.achievements.add(new Achievement("first_kill", "Three Kills",
				"Defeat three enemies.", THREE_KILLS_TARGET, this.playerProfile
						.isAchievementUnlocked("first_kill")));
	}

	/** Adds the High Roller achievement. */
	private void addHighScoreAchievement() {
		this.achievements.add(new Achievement("high_score_1000",
				"High Roller", "Reach 1,000 points in a single run.", 0,
				HIGH_SCORE_TARGET, this.playerProfile
						.isAchievementUnlocked("high_score_1000")));
	}

	/**
	 * Records one confirmed enemy defeat and saves the resulting progress.
	 *
	 * @return Newly unlocked achievement, or null when nothing unlocks.
	 */
	public final Achievement recordEnemyDefeated() {
		this.playerProfile.recordEnemyDefeated();
		Achievement unlockedAchievement = null;

		for (Achievement achievement : this.achievements)
			if (!achievement.isUnlocked()
					&& achievement.getRequiredEnemyKills() > 0
					&& this.playerProfile.getTotalEnemiesKilled()
							>= achievement.getRequiredEnemyKills()) {
				achievement.unlock();
				this.playerProfile.unlockAchievement(achievement.getId());
				// TODO Connect the shared CurrencyManager reward here when its API is available.
				unlockedAchievement = achievement;
			}

		saveProfile();
		return unlockedAchievement;
	}

	/**
	 * Checks the current run's score against score-based achievements, and
	 * saves any newly unlocked progress.
	 *
	 * @param currentScore Score reached so far in the current run.
	 * @return Newly unlocked achievement, or null when nothing unlocks.
	 */
	public final Achievement recordScore(final int currentScore) {
		Achievement unlockedAchievement = null;

		for (Achievement achievement : this.achievements)
			if (!achievement.isUnlocked() && achievement.getRequiredScore() > 0
					&& currentScore >= achievement.getRequiredScore()) {
				achievement.unlock();
				this.playerProfile.unlockAchievement(achievement.getId());
				unlockedAchievement = achievement;
			}

		if (unlockedAchievement != null)
			saveProfile();
		return unlockedAchievement;
	}

	/** Saves the player profile, retaining progress after restarting. */
	private void saveProfile() {
		try {
			Core.getFileManager().savePlayerProfile(this.playerProfile);
		} catch (IOException e) {
			Core.getLogger().warning("Couldn't save player profile.");
		}
	}

	/** @return Read-only list of available achievements. */
	public final List<Achievement> getAchievements() {
		return Collections.unmodifiableList(this.achievements);
	}

	/** @return Total enemies defeated across all games. */
	public final int getTotalEnemiesKilled() {
		return this.playerProfile.getTotalEnemiesKilled();
	}

}