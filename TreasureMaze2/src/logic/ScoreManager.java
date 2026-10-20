package logic; // package for game rules

// Holds two numbers: the score and the hearts. Other classes call small methods (loseLife, addCoinPoints...)
// instead of changing the numbers directly, so the rules (for example "hearts never go below 0") live in one place.
public class ScoreManager { // score and lives
    public static final int START_LIVES = 4; // hearts at the start of a game (was 3, plus one extra heart)

    private int totalScore; // points collected
    private int lives; // lives left

    public ScoreManager() { // new game state
        resetScore(); // set score and lives
    } // end of constructor

    public final void resetScore() { // reset for a new game (final: called from the constructor)
        this.totalScore = 0; // no points
        this.lives = START_LIVES; // full lives
    } // end of resetScore

    public void addTreasurePoints() { // treasure reward
        totalScore += 500; // +500 points
    } // end of addTreasurePoints

    public void addCoinPoints() { // reward for one coin
        totalScore += 100; // +100 points
    } // end of addCoinPoints

    public void addLevelClearBonus(int level) { // level clear reward
        totalScore += level * 300; // 300 points per level number
    } // end of addLevelClearBonus

    /** Removes one life. Returns true when no lives are left. */
    public boolean loseLife() { // subtract a life
        if (lives > 0) { // never go below zero
            lives--; // remove one life
        } // end of floor check
        return lives <= 0; // true means health is empty
    } // end of loseLife

    /** Instant elimination (level 1 bombs): all hearts are lost at once. Always returns true. */
    public boolean eliminate() { // remove every heart
        lives = 0; // health is empty
        return true; // empty
    } // end of eliminate

    /** Gives one heart back (the reward for escaping during the five-second countdown). */
    public void restoreLife() { // add one heart to an empty health bar
        if (lives <= 0) { // only when empty
            lives = 1; // one heart
        } // end of empty check
    } // end of restoreLife

    public int getLives() { // read lives
        return lives; // return them
    } // end of getLives

    public int getTotalScore() { // read score
        return totalScore; // return it
    } // end of getTotalScore
} // end of ScoreManager
