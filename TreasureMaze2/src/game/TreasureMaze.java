package game; // package for core game objects

import audio.SoundManager; // sound effects and music
import java.awt.AlphaComposite; // transparency for drawing
import java.awt.Color; // RGB color
import java.awt.Font; // text font
import java.awt.FontMetrics; // text size measurements
import java.awt.Graphics; // drawing surface
import java.awt.Graphics2D; // richer drawing surface
import java.awt.Point; // integer x/y coordinate
import java.awt.RenderingHints; // drawing quality options
import java.awt.event.KeyAdapter; // key listener with empty methods
import java.awt.event.KeyEvent; // keyboard event
import java.awt.event.MouseAdapter; // mouse listener with empty methods
import java.awt.event.MouseEvent; // mouse event
import java.util.ArrayList; // growable list
import java.util.List; // list interface
import javax.swing.JPanel; // container used as the game canvas
import javax.swing.Timer; // Swing timer that fires on the UI thread
import logic.BombLogic; // bomb rules
import logic.CoinLogic; // coin rules (alternative way to unlock the exit)
import logic.LevelManager; // level tracking
import logic.MimicLogic; // mimic rules
import logic.Pathfinder; // BFS helpers
import logic.ScoreManager; // score and lives
import logic.TreasureLogic; // treasure rules
import logic.VanishLogic; // level 1 vanishing tiles
import ui.GameUI; // window controller

@SuppressWarnings("serial") // the panel is never serialized, so the warning is harmless
// HOW THIS CLASS WORKS (read this first):
// A Swing JPanel is a drawing board. This class is the whole game screen and does three jobs:
// 1. INPUT: handleKeyPressed() runs when a key is pressed (move, E, SPACE, M, Esc).
// 2. UPDATE: a javax.swing.Timer calls onTick() every 50 ms. Things that happen by themselves
//    (mimics walking, the 5 second countdown, crumbling tiles) live in onTick(), so they do not wait for a key.
// 3. DRAW: paintComponent() draws the map, the objects and the HUD. repaint() asks Swing to call it again.
// The rules themselves (bombs, coins, mimics, score...) are in small helper classes in the logic package;
// this class only connects them: it asks them questions and reacts to the answers.
public final class TreasureMaze extends JPanel { // the game screen: input, rules and drawing

    private static final long serialVersionUID = 1L; // version id for serialization

    private static final long MOVE_COOLDOWN = 90; // minimum milliseconds between moves
    private static final int LEVEL3_MIN_TILE = 32; // smallest tile size on level 3 (zoom)
    private static final int HUD_SPACE = 80; // pixels reserved at the top for the HUD
    // Move budget = (shortest safe route start -> treasure -> exit) * factor + slack  (the ORIGINAL formula)...
    private static final double[] MOVE_FACTOR = {3.0, 2.2, 1.8}; // route multiplier per level (unchanged)
    private static final int MOVE_SLACK = 10; // extra moves added to every budget (unchanged)
    // Level 1 keeps the formula above. Levels 2 and 3 use fixed starting limits instead.
    private static final int[] FIXED_MOVES = {0, 725, 888}; // 0 = use the formula (level 1)
    private static final int[] MIMIC_DAMAGE = {1, 2, 1}; // hearts a mimic takes per catch on levels 1, 2, 3
    private static final long CATCH_STUN_MILLIS = 1000; // the mimic that caught the player freezes this long, so one catch hurts once
    private static final long BLUE_STUN_MILLIS = 3500; // a blue bomb stuns mimics for exactly 3.5 seconds
    private static final int BLUE_RADIUS = 4; // blast radius of a blue bomb in cells
    private static final int COIN_MOVE_BONUS = 67; // moves added to the remaining allowance for each coin
    private static final long ESCAPE_MILLIS = 5000; // the five-second escape countdown
    private static final int HEARING_RANGE = 12; // mimic footsteps are audible within this many cells
    private static final long EXPLOSION_MILLIS = 600; // how long the explosion animation lasts
    private static final int NOISE_RANGE = 12; // path steps over which walking near the chest is heard
    private static final int CHEST_NOISE_RANGE = 20; // path steps over which the chest opening is heard
    private static final int TICK_MILLIS = 50; // game timer period (countdown, vanishing tiles, animation)

    /** A bomb explosion animation playing on a cell. */
    private static final class Explosion { // data holder
        final int x; // cell column
        final int y; // cell row
        final long start; // time it started

        Explosion(int x, int y, long start) { // create one
            this.x = x; // store column
            this.y = y; // store row
            this.start = start; // store start time
        } // end of constructor
    } // end of Explosion

    private final Player player = new Player(1, 1); // the player
    private final LevelManager levelManager = new LevelManager(); // active level tracker (generates this game's maps)
    private final BombLogic bombSystem = new BombLogic(); // bombs
    private final TreasureLogic treasureSystem = new TreasureLogic(); // treasure chest
    private final CoinLogic coinSystem = new CoinLogic(); // coins
    private final MimicLogic mimicSystem = new MimicLogic(); // mimics
    private final VanishLogic vanishSystem = new VanishLogic(); // vanishing tiles (level 1 only)
    private final ScoreManager scoreManager = new ScoreManager(); // score and lives
    private final Sprites sprites = new Sprites(); // pixel-art pictures
    private final SoundManager sound; // sound effects and music
    private final GameUI gameUI; // used to show the result screen
    private final List<Explosion> explosions = new ArrayList<>(); // explosions currently animating
    private final Timer gameTimer; // drives the countdown, tiles and animation

    private String[] currentMap; // map of the active level
    private Point startPos = new Point(1, 1); // player start cell
    private Point exitPos; // exit cell
    private int movesLeft; // remaining move budget
    private boolean finished; // true once the game ended
    private boolean dying; // true during the five-second countdown (health is zero)
    private long dyingEnd; // time when the countdown runs out
    private int lastTickSecond = -1; // last whole second that played the tick sound
    private long lastMoveTime = 0; // time of the last accepted move
    private long flashUntil = 0; // red damage flash lasts until this time
    private boolean facingLeft; // player sprite is mirrored when walking left
    private boolean stepParity; // alternates each step for the walking animation
    private String statusMessage = ""; // text shown in the HUD
    private int blueBombs; // blue Mimic-trap bombs the player carries (a plain counter, no inventory)
    private int blueAtLevelStart; // carried amount when the level began (a level restart gives back exactly this)
    private boolean paused; // true while the pause menu is open: everything is frozen
    private boolean confirmQuit; // true while the quit confirmation is shown
    private int pauseIndex; // pause menu cursor: 0 Resume, 1 Main Menu, 2 Quit Game
    private long pausedAt; // real time when the current pause began
    private long pausedTotal; // total real time spent paused (subtracted by clock())

    public TreasureMaze(GameUI gameUI, SoundManager sound) { // build the game panel
        this.gameUI = gameUI; // keep the window controller
        this.sound = sound != null ? sound : new SoundManager(false); // silent manager when none is given
        setBackground(new Color(10, 10, 14)); // near-black background
        setFocusable(true); // allow keyboard focus
        setFocusTraversalKeysEnabled(false); // let Tab and others reach the key handler
        addKeyListener(new KeyAdapter() { // listen for keys without passing this directly
            @Override // replaces the empty adapter method
            public void keyPressed(KeyEvent e) { // a key went down
                handleKeyPressed(e); // delegate to the game logic
            } // end of keyPressed
        }); // end of key listener
        addMouseListener(new MouseAdapter() { // listen for mouse clicks
            @Override // replaces the empty adapter method
            public void mouseClicked(MouseEvent e) { // panel was clicked
                requestFocusInWindow(); // regain keyboard focus
            } // end of mouseClicked
        }); // end of mouse listener
        gameTimer = new Timer(TICK_MILLIS, e -> onTick()); // create the periodic timer
        // the timer is started by resetGameToLevel1() when a game begins and stopped when the game ends or the player leaves
        loadCurrentLevel(); // load level 1
    } // end of constructor

    public void resetGameToLevel1() { // start a new game
        levelManager.resetToLevel1(); // back to level 1 with brand-new random maps
        scoreManager.resetScore(); // reset score and lives
        finished = false; // game is running again
        dying = false; // no countdown
        paused = false; // never start paused
        blueAtLevelStart = 0; // a new game starts with no blue bombs
        confirmQuit = false; // no quit prompt
        if (!gameTimer.isRunning()) gameTimer.start(); // one timer only: start it only if it is not running
        loadCurrentLevel(); // load the level
        repaint(); // redraw
    } // end of resetGameToLevel1

    private void loadCurrentLevel() { // set up the active level
        int level = levelManager.getCurrentLevel(); // level number
        currentMap = levelManager.getCurrentMap(); // level map

        Point p = GameMap.find(currentMap, 'P'); // start marker
        startPos = p != null ? p : new Point(1, 1); // fall back to (1,1)
        exitPos = GameMap.find(currentMap, 'E'); // exit marker
        player.setPosition(startPos.x, startPos.y); // place the player

        bombSystem.loadBombsFromMap(currentMap); // load bombs
        treasureSystem.loadTreasureFromMap(currentMap); // load treasure
        coinSystem.loadCoinsFromMap(currentMap); // load coins
        mimicSystem.loadMimicsFromMap(currentMap, level); // load mimics
        vanishSystem.load(level == 1, exitPos, treasureSystem.getPosition(), startPos); // vanishing tiles on level 1 only
        explosions.clear(); // no old explosions
        blueBombs = blueAtLevelStart; // carried blue bombs (picked-up pickups come back on a restart, so go back to the amount at level start)

        movesLeft = computeMoveBudget(level); // set the move limit
        int coins = coinSystem.getTotal(); // coins in this level
        if (level == 1) { // beginner-friendly hint for the calm first level
            statusMessage = "Level 1: chest or all " + coins + " coins opens the exit. Tiles crumble!"; // intro
        } else { // later levels
            statusMessage = "Level " + level + ": chest or all " + coins + " coins opens the exit. Beware mimics!"; // intro
        } // end of intro choice
    } // end of loadCurrentLevel

    private int computeMoveBudget(int level) { // move limit for a level
        Point t = treasureSystem.getPosition(); // treasure cell
        if (t == null || exitPos == null) { // incomplete map
            return 9999; // effectively unlimited
        } // end of check
        int toTreasure = Pathfinder.distance(currentMap, startPos, t, true); // safe steps start -> treasure
        int toExit = Pathfinder.distance(currentMap, t, exitPos, true); // safe steps treasure -> exit
        if (toTreasure < 0 || toExit < 0) { // no safe route
            return 9999; // effectively unlimited
        } // end of check
        int idx = Math.min(level, MOVE_FACTOR.length) - 1; // 0-based level index
        int original = (int) Math.ceil((toTreasure + toExit) * MOVE_FACTOR[idx]) + MOVE_SLACK; // the original budget
        return FIXED_MOVES[idx] > 0 ? FIXED_MOVES[idx] : original; // level 1: the formula; level 2: 725; level 3: 888
    } // end of computeMoveBudget

    private boolean isExitUnlocked() { // the exit opens with the chest OR with every coin
        return treasureSystem.isClaimed() || coinSystem.isComplete(); // either way unlocks it
    } // end of isExitUnlocked

    /**
     * Applies damage. Normal damage removes one heart; lethal damage (level 1 bombs) removes all hearts.
     * When health reaches zero the five-second escape countdown starts - exactly once.
     * While the countdown runs, further damage is ignored so nothing can restart it or end the game early.
     */
    private void damagePlayer(String message, boolean lethal) { // one-heart damage (bombs, out of moves)
        damagePlayer(message, lethal, 1); // the usual single heart
    } // end of damagePlayer

    private void damagePlayer(String message, boolean lethal, int hearts) { // central damage rule: hearts = how many hearts to remove
        if (dying || finished) return; // already at zero health: ignore
        boolean empty = false; // is health empty after the damage
        if (lethal) { // level 1 bombs
            empty = scoreManager.eliminate(); // all hearts at once
        } else { // normal damage
            for (int i = 0; i < hearts; i++) empty = scoreManager.loseLife(); // remove the hearts one by one (never below zero)
        } // end of damage kind
        statusMessage = message; // tell the player what happened
        flashUntil = clock() + 250; // red screen flash
        if (empty) { // no hearts left
            beginCountdown(); // start the escape timer
        } // end of empty check
    } // end of damagePlayer

    private void beginCountdown() { // start the five-second escape timer
        if (dying || finished) return; // never start twice
        dying = true; // countdown is running
        dyingEnd = clock() + ESCAPE_MILLIS; // when it expires
        lastTickSecond = -1; // so the first tick sound plays immediately
        statusMessage = "NO HEARTS! Reach the open exit in time!"; // instruction
    } // end of beginCountdown

    private void finish(boolean won) { // end the game
        finished = true; // ignore further input
        gameTimer.stop(); // the game is over: stop updating
        dying = false; // countdown (if any) is over
        repaint(); // redraw once
        if (gameUI != null) { // controller available
            gameUI.showResults(scoreManager.getTotalScore(), won); // show the result screen
        } // end of null check
    } // end of finish

    /** Runs every TICK_MILLIS on the UI thread: countdown, vanishing tiles, animation. */
    // onTick() is the "heartbeat" of the game. Each call does the same short list of steps:
    // update vanishing tiles -> let mimics think and step -> check blue bombs -> run the countdown -> redraw.
    // Because the Timer calls it, mimics keep moving even when the player is standing still.
    private void onTick() { // timer callback
        if (finished || paused) return; // nothing to update after the game ended or while paused
        long now = clock(); // current time
        boolean changed = vanishSystem.update(now); // tiles crumble, vanish and regrow
        int stepped = mimicSystem.update(player.getX(), player.getY(), currentMap, now); // mimics act on their own, no key needed
        if (stepped > 0) { // a mimic moved
            changed = true; // redraw
            if (mimicSystem.anyWithin(player.getX(), player.getY(), HEARING_RANGE)) sound.play("mimic_step"); // footstep if close enough to hear
            checkMimicCatch(); // a mimic may walk into the player
            if (finished) return; // the catch ended the game
        } // end of mimic step
        for (Point trap : new ArrayList<>(bombSystem.getTraps())) { // each placed blue bomb
            if (mimicSystem.triggerTrap(trap.x, trap.y, now, BLUE_RADIUS, BLUE_STUN_MILLIS)) { // a mimic came next to it
                bombSystem.removeTrap(trap); // it explodes only once
                explosions.add(new Explosion(trap.x, trap.y, now)); // explosion animation
                sound.play("bomb"); // explosion sound
                mimicSystem.hearNoise(trap.x, trap.y, currentMap, NOISE_RANGE); // mimics outside the blast that are not stunned come to look (the noise position, not the player)
                statusMessage = "Blue bomb! Nearby mimics are stunned for 3.5 s."; // tell the player
                changed = true; // redraw
            } // end of trigger
        } // end of traps
        if (dying) { // countdown running
            long remaining = dyingEnd - now; // milliseconds left
            if (remaining <= 0) { // time is up
                finish(false); // the game ends (exactly once: finish() clears dying)
                return; // stop here
            } // end of expiry check
            int secondsLeft = (int) Math.ceil(remaining / 1000.0); // whole seconds left
            if (secondsLeft != lastTickSecond) { // a new second started
                lastTickSecond = secondsLeft; // remember it
                sound.play("tick"); // tick once per second
            } // end of second check
            changed = true; // redraw the countdown text
        } // end of countdown
        explosions.removeIf(ex -> now - ex.start > EXPLOSION_MILLIS); // drop finished explosions
        if (!explosions.isEmpty() || now < flashUntil || now - lastMoveTime < 250) { // animations running
            changed = true; // redraw
        } // end of animation check
        if (changed && isShowing()) { // only draw when the game screen is visible
            repaint(); // redraw
        } // end of repaint check
    } // end of onTick

    // ------------------------------------------------------------------ input

    // Order of work for a movement key (the order matters):
    // 1. Esc and the pause menu are handled first, so a paused game never reaches the movement code.
    // 2. dx/dy become -1, 0 or +1 (the direction). Walls, fake chests and vanished tiles block the move.
    // 3. The player moves, then the cell is checked for bombs, mimics, blue bombs, coins and the exit.
    // 4. Finally the move counter is checked: 0 moves left costs a heart.
    void handleKeyPressed(KeyEvent e) { // process one key press
        if (finished) return; // game already ended
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { // Esc opens or closes the pause menu
            if (confirmQuit) confirmQuit = false; // Esc cancels the quit prompt first
            else setPaused(!paused); // otherwise pause / resume
            repaint(); // redraw
            return; // done
        } // end of Esc
        if (paused) { // the pause menu is open
            handlePauseKey(e); // menu navigation only: the game stays frozen
            return; // never reach the movement code
        } // end of paused

        int dx = 0; // horizontal step
        int dy = 0; // vertical step
        switch (e.getKeyCode()) { // which key
            case KeyEvent.VK_UP, KeyEvent.VK_W -> dy = -1; // up
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> dy = 1; // down
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> dx = -1; // left
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> dx = 1; // right
            case KeyEvent.VK_E -> { // open the chest next to the player
                interactWithChest(); // real chest: open it, fake chest: reveal the mimic
                return; // done
            } // end of E case
            case KeyEvent.VK_SPACE -> { // place a blue bomb
                placeBlueBomb(); // uses one carried blue bomb
                return; // done
            } // end of SPACE case
            case KeyEvent.VK_M -> { // mute key
                boolean muted = sound.toggleMute(); // flip sound on/off
                statusMessage = muted ? "Sound OFF (press M to turn it on)" : "Sound ON"; // feedback
                repaint(); // redraw
                return; // done
            } // end of M case
            default -> { // any other key
                return; // ignore
            } // end of default case
        } // end of switch

        long now = clock(); // current time
        if (now - lastMoveTime < MOVE_COOLDOWN) return; // too soon after the last move
        lastMoveTime = now; // remember this move time

        int tx = player.getX() + dx; // target column
        int ty = player.getY() + dy; // target row
        if (!Pathfinder.walkable(currentMap, tx, ty, false)) return; // wall or outside: no move

        // Both the real chest and disguised mimic chests block movement silently.
        Point realChest = treasureSystem.getPosition();
        if ((realChest != null && realChest.x == tx && realChest.y == ty)
                || mimicSystem.isDisguisedAt(tx, ty)) {
            return; // blocked silently; no move is spent
        }
        if (vanishSystem.isVanished(tx, ty)) { // the floor there has crumbled away
            statusMessage = "That tile crumbled away - wait for it to grow back!"; // explain
            repaint(); // redraw
            return; // blocked, and no move is spent
        } // end of vanished check

        if (dx < 0) facingLeft = true; // walking left: mirror the sprite
        if (dx > 0) facingLeft = false; // walking right: normal sprite
        int oldX = player.getX(); // cell being left
        int oldY = player.getY(); // cell being left
        player.move(dx, dy); // move the player
        stepParity = !stepParity; // alternate for the walking animation
        sound.play("step"); // footstep sound (once per accepted move)
        vanishSystem.onLeave(oldX, oldY, now); // the old tile starts to crumble (level 1)
        vanishSystem.onEnter(player.getX(), player.getY()); // a crumbling tile is saved while stood on
        if (!dying) { // moves are not counted during the countdown
            movesLeft--; // spend a move
        } // end of move count

        if (!dying && !treasureSystem.isClaimed() && nearChest()) { // walking near the closed chest is noisy
            mimicSystem.hearNoise(player.getX(), player.getY(), currentMap, NOISE_RANGE); // nearby mimics come to look
        } // end of chest noise

        if (bombSystem.triggerAt(player.getX(), player.getY())) { // stepped on a bomb
            sound.play("bomb"); // explosion sound
            explosions.add(new Explosion(player.getX(), player.getY(), now)); // explosion animation
            if (dying) { // hearts are already empty (countdown running): another bomb ends the game
                finish(false); // game over screen
                return; // nothing else to do
            } // end of second bomb
            boolean lethal = levelManager.getCurrentLevel() == 1; // level 1 bombs eliminate the player
            damagePlayer(lethal ? "BOOM! Eliminated! Reach the exit!" : "BOOM! Hit a Bomb! (-1 Heart)", lethal); // apply damage
        } // end of bomb check

        checkMimicCatch(); // stepped into a mimic?

        if (bombSystem.collectBlueAt(player.getX(), player.getY())) { // walked over a blue bomb pickup
            blueBombs++; // one more carried
            sound.play("coin"); // pickup sound
            statusMessage = "Blue bomb collected! Carrying " + blueBombs + ". Press SPACE to place it."; // hint
        } // end of blue pickup

        if (coinSystem.collectAt(player.getX(), player.getY())) { // picked up a coin
            scoreManager.addCoinPoints(); // +100
            movesLeft += COIN_MOVE_BONUS; // +67 moves on top of what is left (each coin disappears when taken, so only once)
            sound.play("coin"); // coin sound
            statusMessage = "Coin " + coinSystem.getCollected() + "/" + coinSystem.getTotal() + " (+100, +" + COIN_MOVE_BONUS + " moves)"; // progress
            if (coinSystem.isComplete() && !treasureSystem.isClaimed()) { // last coin
                statusMessage = "All coins found (+100)! Exit Unlocked!"; // the alternative route is done
            } // end of last coin check
        } // end of coin check

        if (isExitUnlocked() && exitPos != null // exit is open and exists
            && player.getX() == exitPos.x && player.getY() == exitPos.y) { // player on the exit
            boolean escaped = dying; // remember whether this is an escape from the countdown
            if (dying) { // escaped during the countdown
                dying = false; // countdown cancelled
                scoreManager.restoreLife(); // reward: one heart back
            } // end of escape
            scoreManager.addLevelClearBonus(levelManager.getCurrentLevel()); // level bonus
            if (levelManager.nextLevel()) { // another level exists
                blueAtLevelStart = blueBombs; // carried blue bombs go with the player
                loadCurrentLevel(); // load it
                if (escaped) { // keep the escape message visible
                    statusMessage = "ESCAPED! One heart restored. " + statusMessage; // celebrate
                } // end of message
            } else { // that was the last level
                finish(true); // win
                return; // done
            } // end of next-level check
        } else if (movesLeft <= 0 && !dying) { // out of moves
            damagePlayer("OUT OF MOVES! (-1 Heart) Level restarted.", false); // lose a heart
            if (!dying) { // still alive
                loadCurrentLevel(); // restart the level
                statusMessage = "OUT OF MOVES! (-1 Heart) Level restarted."; // message
            } // end of alive check
        } // end of exit/moves check
        repaint(); // redraw
    } // end of handleKeyPressed

    /** Game time in ms. It does not advance while the game is paused, so countdowns, tiles and mimics freeze. */
    // The game clock. Normal code would use System.currentTimeMillis() (milliseconds since 1970).
    // Here we subtract the time spent paused, so every timer built on clock() simply stops while the pause menu is open.
    private long clock() { // the game clock
        long real = paused ? pausedAt : System.currentTimeMillis(); // while paused, time stands still at the moment the pause began
        return real - pausedTotal; // minus all earlier paused time
    } // end of clock

    private void setPaused(boolean pause) { // pause or resume (never creates a timer: the one game timer just skips its work)
        if (pause == paused || finished) return; // already in that state
        long real = System.currentTimeMillis(); // real time
        if (pause) { // pausing
            pausedAt = real; // remember when
            pauseIndex = 0; // cursor on Resume
            confirmQuit = false; // no pending quit prompt
        } else { // resuming
            pausedTotal += real - pausedAt; // the paused time is cut out of the game clock
        } // end of toggle
        paused = pause; // new state
        if (pause) sound.pauseMusic(); else sound.resumeMusic(); // music pauses and resumes with the game (mute setting is kept)
        repaint(); // show or hide the menu
    } // end of setPaused

    /** Stops the game timer and pause state. Called when leaving the game for the main menu. */
    public void stopGame() { // end this game session
        gameTimer.stop(); // no more updates (a new game restarts the timer)
        paused = false; // not paused any more
        sound.resumeMusic(); // the main menu has its music (a single clip: no duplicate playback)
        confirmQuit = false; // no prompt
    } // end of stopGame

    private void handlePauseKey(KeyEvent e) { // keys while the pause menu is open
        int key = e.getKeyCode(); // which key
        if (confirmQuit) { // asking "really quit?"
            if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_Y) System.exit(0); // yes: close the program
            if (key == KeyEvent.VK_N) confirmQuit = false; // no: back to the menu
            repaint(); // redraw
            return; // done
        } // end of confirm
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) pauseIndex = (pauseIndex + 2) % 3; // cursor up (wraps)
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) pauseIndex = (pauseIndex + 1) % 3; // cursor down (wraps)
        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) { // choose
            if (pauseIndex == 0) { // Resume
                setPaused(false); // continue the game
            } else if (pauseIndex == 1) { // Main Menu
                stopGame(); // stop this session first
                if (gameUI != null) gameUI.showScreen("MENU"); // show the main menu
            } else { // Quit Game
                confirmQuit = true; // ask first
            } // end of choice
        } // end of choose
        repaint(); // redraw the cursor
    } // end of handlePauseKey

    private void drawPauseMenu(Graphics2D g2d, int panelWidth) { // dark overlay with the three options
        g2d.setColor(new Color(0, 0, 0, 180)); // translucent black
        g2d.fillRect(0, 0, panelWidth, getHeight()); // cover everything
        int cx = panelWidth / 2; // horizontal center
        int top = Math.max(150, getHeight() / 2 - 110); // first line position
        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 40)); // title font
        g2d.setColor(Color.CYAN); // title color
        String title = "PAUSED"; // title text
        g2d.drawString(title, cx - g2d.getFontMetrics().stringWidth(title) / 2, top); // centered title
        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26)); // option font
        String[] options = {"Resume", "Main Menu", "Quit Game"}; // the three choices
        for (int i = 0; i < options.length; i++) { // each option
            boolean selected = i == pauseIndex && !confirmQuit; // is the cursor here
            g2d.setColor(selected ? Color.YELLOW : Color.WHITE); // highlight the cursor line
            String text = (selected ? "> " : "  ") + options[i]; // cursor mark
            g2d.drawString(text, cx - 110, top + 60 + i * 44); // left aligned at a common x
        } // end of options
        g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16)); // hint font
        g2d.setColor(confirmQuit ? Color.ORANGE : Color.GRAY); // prompt color
        String hint = confirmQuit ? "Quit the game? Enter/Y = Yes, N or Esc = No" : "Up/Down = move, Enter = select, Esc = resume"; // help text
        g2d.drawString(hint, cx - g2d.getFontMetrics().stringWidth(hint) / 2, top + 60 + options.length * 44 + 20); // centered help
    } // end of drawPauseMenu

    /** Opens the real chest: +500, unlocks the exit, and the noise may be heard by mimics. Does nothing if it is already open. */
    private void openRealChest() { // the one place where the real chest is opened
        Point c = treasureSystem.getPosition(); // the real chest
        if (c == null || !treasureSystem.claimIfSteppedOn(c.x, c.y)) return; // nothing to open or already open
        scoreManager.addTreasurePoints(); // +500
        sound.play("chest"); // chest opening sound
        statusMessage = "Treasure Claimed (+500)! Exit Unlocked!"; // message
        if (mimicSystem.hearNoise(c.x, c.y, currentMap, CHEST_NOISE_RANGE) > 0) { // the noise carries
            statusMessage = "Chest opened (+500)! A mimic heard it - it is coming!"; // warn the player
        } // end of noise check
    } // end of openRealChest

    /** A fake chest was opened: the mimic is revealed and stands still for 0.5 s. No treasure, no exit unlock. */
    private void revealMimicChest(int x, int y) { // reveal rule
        if (!mimicSystem.revealAt(x, y, clock())) return; // not a fake chest
        sound.play("mimic_step"); // a growl-like sound (reuses an existing effect)
        statusMessage = "It was a MIMIC! It freezes for a moment - RUN!"; // warn the player
        flashUntil = clock() + 250; // brief red flash
    } // end of revealMimicChest

    /**
     * E key: look at the eight cells around the player in a fixed order: up, down, left, right, then the diagonals (the real chest also opens if you stand on it).
     * The first chest found is opened: a real chest gives its reward, a fake chest reveals the mimic.
     * With no chest next to the player nothing happens.
     */
    // How E works: the arrays below are "offsets" ({0,-1} means one cell up).
    // We add each offset to the player's position and look at that cell. The first chest found wins.
    // A real chest is opened normally; a fake chest wakes the Mimic. Nothing opens by walking, only by pressing E.
    private void interactWithChest() { // E key rule
        int[][] order = {{0, 0}, {0, -1}, {0, 1}, {-1, 0}, {1, 0}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}}; // up, down, left, right first, then the four diagonals (a chest you walk by)
        Point real = treasureSystem.getPosition(); // the real chest
        for (int[] d : order) { // each neighbour in the fixed order
            int cx = player.getX() + d[0]; // neighbour column
            int cy = player.getY() + d[1]; // neighbour row
            if (real != null && real.x == cx && real.y == cy) { // the real chest
                if (treasureSystem.isClaimed()) statusMessage = "This chest is already open."; // nothing more to take
                else openRealChest(); // open it normally
                repaint(); // redraw
                return; // only one chest per key press
            } // end of real chest
            if (mimicSystem.isDisguisedAt(cx, cy)) { // a fake chest
                revealMimicChest(cx, cy); // reveal the mimic instead
                repaint(); // redraw
                return; // only one chest per key press
            } // end of fake chest
        } // end of neighbours
        statusMessage = "No chest next to you."; // nothing to open
        repaint(); // redraw
    } // end of interactWithChest

    /** SPACE: put down one carried blue bomb on the player's cell (not on the chest or the exit). */
    private void placeBlueBomb() { // place a trap
        int x = player.getX(); // cell column
        int y = player.getY(); // cell row
        Point t = treasureSystem.getPosition(); // real chest
        boolean special = (t != null && t.x == x && t.y == y) || (exitPos != null && exitPos.x == x && exitPos.y == y); // chest or exit cell
        if (blueBombs <= 0) { // nothing to place
            statusMessage = "No blue bombs. Collect the blue pickups first."; // explain
        } else if (special || !bombSystem.placeTrap(x, y)) { // bad cell or a trap is already here
            statusMessage = "You cannot place a blue bomb here."; // explain
        } else { // placed
            blueBombs--; // one used
            sound.play("tick"); // placing sound
            statusMessage = "Blue bomb placed. It stuns mimics that come close."; // feedback
        } // end of result
        repaint(); // redraw
    } // end of placeBlueBomb

    private boolean nearChest() { // is the player within 4 cells of the chest
        Point t = treasureSystem.getPosition(); // chest cell
        return t != null && Math.abs(player.getX() - t.x) + Math.abs(player.getY() - t.y) <= 4; // Manhattan distance
    } // end of nearChest

    /** A mimic on the player's cell catches the player (called after player moves and after mimic steps). */
    private void checkMimicCatch() { // catch rule
        if (dying || finished || !mimicSystem.checkCollision(player.getX(), player.getY())) return; // nothing to do
        sound.play("mimic_catch"); // distinct catch sound
        int hearts = MIMIC_DAMAGE[Math.min(levelManager.getCurrentLevel(), MIMIC_DAMAGE.length) - 1]; // damage depends on the level
        damagePlayer("CAUGHT BY MIMIC! (-" + hearts + (hearts == 1 ? " Heart)" : " Hearts)"), false, hearts); // lose hearts, nothing else changes
        mimicSystem.stunNear(player.getX(), player.getY(), 0, clock() + CATCH_STUN_MILLIS); // the mimic freezes: one catch hurts once and the player can leave
    } // end of checkMimicCatch

    // ---------------------------------------------------------------- drawing

    @Override // replaces JPanel painting
    // Drawing order matters: whatever is drawn LATER appears ON TOP.
    // So: floor and walls first, then coins, bombs, chests, mimics, the player, explosions, and last the HUD and pause menu.
    // The tile size is a multiple of 16 so the 16x16 pixel art is scaled without blur. Level 3 is zoomed and scrolls with the player.
    protected void paintComponent(Graphics g) { // draw the whole screen
        super.paintComponent(g); // clear the background
        if (currentMap == null || currentMap.length == 0) return; // nothing to draw

        Graphics2D g2d = (Graphics2D) g; // richer drawing API
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); // smooth text
        long now = clock(); // current time for animations

        int panelWidth = getWidth(); // panel width in pixels
        int rows = currentMap.length; // map rows
        int cols = currentMap[0].length(); // map columns
        int availableHeight = Math.max(1, getHeight() - HUD_SPACE); // height left below the HUD
        int fit = Math.min(panelWidth / cols, availableHeight / rows); // largest square cell that fits
        int tile = fit >= 16 ? (fit / 16) * 16 : Math.max(1, fit); // whole multiples of 16 keep pixel art crisp
        if (levelManager.getCurrentLevel() == 3) { // level 3 is big: zoom in so sprites are easy to see
            tile = Math.max(tile, LEVEL3_MIN_TILE); // never smaller than 32 px (levels 1 and 2 are unchanged)
        } // end of zoom
        int offsetX = (panelWidth - cols * tile) / 2; // center horizontally
        int offsetY = HUD_SPACE + (availableHeight - rows * tile) / 2; // center vertically below the HUD
        if (cols * tile > panelWidth) { // zoomed map is wider than the window: scroll with the player
            offsetX = Math.max(panelWidth - cols * tile, Math.min(0, panelWidth / 2 - (player.getX() * tile + tile / 2))); // keep the player centered, never show past the map edge
        } // end of horizontal scroll
        if (rows * tile > availableHeight) { // zoomed map is taller than the window: scroll with the player
            offsetY = HUD_SPACE + Math.max(availableHeight - rows * tile, Math.min(0, availableHeight / 2 - (player.getY() * tile + tile / 2))); // same vertically
        } // end of vertical scroll
        g2d.setClip(0, HUD_SPACE, panelWidth, availableHeight); // draw the map only inside the map area (nothing over the HUD)

        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(8, tile * 3 / 4))); // font for letter fallbacks
        FontMetrics fm = g2d.getFontMetrics(); // measure glyphs for centering

        for (int r = 0; r < rows; r++) { // for each row
            for (int c = 0; c < cols; c++) { // for each column
                int x = offsetX + c * tile; // pixel x of the cell
                int y = offsetY + r * tile; // pixel y of the cell
                if (currentMap[r].charAt(c) == '#') { // wall cell
                    drawTileOrLetter(g2d, fm, wallSprite(c, r), '#', new Color(95, 95, 120), x, y, tile); // draw a wall
                } else { // open cell
                    drawTileOrLetter(g2d, fm, (c * 3 + r * 5) % 4 == 0 ? "floor1" : "floor0", '.', new Color(55, 55, 70), x, y, tile); // draw floor
                    if (vanishSystem.isVanished(c, r)) { // the tile has crumbled away
                        g2d.setColor(new Color(4, 4, 8)); // dark void
                        g2d.fillRect(x, y, tile, tile); // hole in the floor
                        g2d.setColor(new Color(60, 30, 30)); // faint rim
                        g2d.drawRect(x, y, tile - 1, tile - 1); // outline of the hole
                    } else if (vanishSystem.isCrumbling(c, r)) { // about to vanish
                        drawTileOrLetter(g2d, fm, "cracked", ':', new Color(200, 140, 80), x, y, tile); // cracked look
                    } // end of vanish drawing
                } // end of cell type check
            } // end of column loop
        } // end of row loop

        for (Point coin : coinSystem.getPositions()) { // each coin still on the map
            drawEntity(g2d, fm, "coin", 'o', Color.ORANGE, offsetX, offsetY, tile, coin.x, coin.y, false, 0); // draw a coin
        } // end of coin loop

        for (Point b : bombSystem.getBombPositions()) { // for each live bomb
            drawEntity(g2d, fm, "bomb", 'B', new Color(230, 50, 50), offsetX, offsetY, tile, b.x, b.y, false, 0); // draw a bomb
        } // end of bomb loop

        for (Point b : bombSystem.getBluePickups()) { // blue Mimic-trap pickups
            drawEntity(g2d, fm, "bombBlue", 'U', new Color(70, 120, 255), offsetX, offsetY, tile, b.x, b.y, false, 0); // blue bomb
        } // end of blue pickups
        for (Point b : bombSystem.getTraps()) { // blue bombs the player placed
            drawEntity(g2d, fm, "bombBlue", 'u', new Color(120, 170, 255), offsetX, offsetY, tile, b.x, b.y, false, 0); // same picture
        } // end of traps

        Point t = treasureSystem.getPosition(); // treasure cell
        if (t != null) { // chest exists
            String chest = treasureSystem.isClaimed() ? "chestOpen" : "chestClosed"; // open after claiming
            drawEntity(g2d, fm, chest, 'T', Color.YELLOW, offsetX, offsetY, tile, t.x, t.y, false, 0); // draw the chest
        } // end of chest check

        for (Point d : mimicSystem.getDisguisedPositions()) { // fake chests look exactly like the real closed chest
            drawEntity(g2d, fm, "chestClosed", 'T', Color.YELLOW, offsetX, offsetY, tile, d.x, d.y, false, 0); // same sprite, same fallback letter
        } // end of fake chests

        if (exitPos != null) { // exit exists
            boolean open = isExitUnlocked(); // unlocked state
            drawEntity(g2d, fm, open ? "doorOpen" : "doorClosed", 'E', open ? Color.CYAN : Color.GRAY, offsetX, offsetY, tile, exitPos.x, exitPos.y, false, 0); // draw the door
        } // end of exit check

        for (Point m : mimicSystem.getPositions()) { // for each mimic
            drawEntity(g2d, fm, "mimic", 'M', Color.MAGENTA, offsetX, offsetY, tile, m.x, m.y, false, 0); // draw a mimic
        } // end of mimic loop

        for (Point st : mimicSystem.getStunnedPositions()) { // stunned mimics get a blue tint
            g2d.setColor(new Color(80, 140, 255, 110)); // translucent blue
            g2d.fillRect(offsetX + st.x * tile, offsetY + st.y * tile, tile, tile); // cover the cell
        } // end of stunned

        int hop = (now - lastMoveTime < 120) ? -(tile / 8) : 0; // small hop right after a step = walking animation
        boolean flip = facingLeft; // mirror when facing left
        if (stepParity && now - lastMoveTime < 120) { // alternate frame of the walk
            hop = -(tile / 6); // slightly higher hop on every second step
        } // end of alternate frame
        drawEntity(g2d, fm, "player", '@', Color.GREEN, offsetX, offsetY, tile, player.getX(), player.getY(), flip, hop); // draw the player

        for (Explosion ex : explosions) { // each explosion animation
            long elapsed = now - ex.start; // time since it started
            int frame = (int) (elapsed * sprites.explosionFrames() / EXPLOSION_MILLIS); // current animation frame
            if (frame >= 0 && frame < sprites.explosionFrames()) { // still playing
                int w = tile * 3; // explosion is 3 cells wide
                int h = w * 91 / 96; // keep the picture's proportions
                int cx = offsetX + ex.x * tile + tile / 2; // cell center x
                int cy = offsetY + ex.y * tile + tile / 2; // cell center y
                sprites.drawScaled(g2d, "explosion" + frame, cx - w / 2, cy - h / 2, w, h); // draw the frame
            } // end of frame check
        } // end of explosion loop

        g2d.setClip(null); // back to full-window drawing for the flash and HUD
        if (now < flashUntil || dying) { // damage flash or countdown glow
            g2d.setColor(new Color(220, 0, 0, dying ? 35 : 70)); // translucent red
            g2d.fillRect(0, HUD_SPACE, panelWidth, getHeight() - HUD_SPACE); // cover the map area
        } // end of flash

        drawHud(g2d, panelWidth, now); // draw the status bar
        if (paused) drawPauseMenu(g2d, panelWidth); // pause overlay on top of everything
    } // end of paintComponent

    private String wallSprite(int c, int r) { // pick a wall picture by position, so it looks varied but stays stable
        int h = (c * 7 + r * 13) % 11; // pseudo-random value from the position
        if (h == 0) return "wall2"; // occasional barred window
        return h % 2 == 0 ? "wall0" : "wall1"; // two brick variants
    } // end of wallSprite

    private void drawTileOrLetter(Graphics2D g2d, FontMetrics fm, String sprite, char letter, Color color, // draw a picture or a letter
                                  int x, int y, int tile) { // pixel position and cell size
        if (sprites.has(sprite)) { // picture available
            sprites.draw(g2d, sprite, x, y, tile, false); // draw it
        } else { // missing picture
            drawLetter(g2d, fm, letter, color, x, y, tile); // fall back to a letter
        } // end of availability check
    } // end of drawTileOrLetter

    private void drawEntity(Graphics2D g2d, FontMetrics fm, String sprite, char letter, Color color, // draw an object on a cell
                            int offsetX, int offsetY, int tile, int col, int row, boolean flip, int yOffset) { // grid placement
        int x = offsetX + col * tile; // pixel x
        int y = offsetY + row * tile + yOffset; // pixel y (yOffset is used for the hop)
        if (sprites.has(sprite)) { // picture available
            sprites.draw(g2d, sprite, x, y, tile, flip); // draw it
        } else { // missing picture
            drawLetter(g2d, fm, letter, color, x, y, tile); // fall back to a letter
        } // end of availability check
    } // end of drawEntity

    private void drawLetter(Graphics2D g2d, FontMetrics fm, char ch, Color color, int x, int y, int tile) { // letter fallback
        g2d.setColor(color); // letter color
        int px = x + (tile - fm.charWidth(ch)) / 2; // centered x
        int py = y + (tile + fm.getAscent() - fm.getDescent()) / 2; // centered baseline
        g2d.drawString(String.valueOf(ch), px, py); // draw the character
    } // end of drawLetter

    private void drawHud(Graphics2D g2d, int panelWidth, long now) { // draw hearts, score, moves, level, coins, message, countdown
        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18)); // HUD font
        FontMetrics fm = g2d.getFontMetrics(); // measure text for alignment

        g2d.setColor(Color.RED); // label color
        String label = "HEALTH: "; // hearts label
        g2d.drawString(label, 20, 28); // label, top left
        int heartX = 20 + fm.stringWidth(label); // hearts start after the label
        int lives = scoreManager.getLives(); // hearts left
        for (int i = 0; i < ScoreManager.START_LIVES; i++) { // one slot per starting heart
            boolean full = i < lives; // full heart or lost heart
            if (sprites.has("heart")) { // picture available
                java.awt.Composite old = g2d.getComposite(); // remember the current transparency
                if (!full) { // lost hearts are drawn faint
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f)); // 25% visible
                } // end of faint
                sprites.draw(g2d, "heart", heartX + i * 26, 8, 24, false); // draw the heart
                g2d.setComposite(old); // restore transparency
            } else { // no picture: use text
                g2d.setColor(full ? Color.RED : Color.DARK_GRAY); // red or gray
                g2d.drawString("<3", heartX + i * 30, 28); // text heart
            } // end of picture check
        } // end of heart loop

        g2d.setColor(Color.CYAN); // score color
        String score = "SCORE: " + scoreManager.getTotalScore(); // score text
        g2d.drawString(score, panelWidth - fm.stringWidth(score) - 20, 28); // score, top right

        g2d.setColor(Color.WHITE); // level color
        g2d.drawString("LEVEL " + levelManager.getCurrentLevel() + "/" + GameMap.LEVEL_COUNT // level text, continued on the next line
            + "   COINS " + coinSystem.getCollected() + "/" + coinSystem.getTotal() + "   BLUE BOMBS " + blueBombs, 20, 58); // level and coins, second row left

        g2d.setColor(movesLeft <= 15 ? Color.ORANGE : Color.WHITE); // warn when moves run low
        String moves = "MOVES: " + Math.max(0, movesLeft); // moves text
        g2d.drawString(moves, panelWidth - fm.stringWidth(moves) - 20, 58); // moves, second row right

        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15)); // smaller font for the message
        FontMetrics small = g2d.getFontMetrics(); // measure it
        g2d.setColor(Color.YELLOW); // message color
        int msgX = Math.max(300, (panelWidth - small.stringWidth(statusMessage)) / 2); // center the message, keep clear of hearts
        g2d.drawString(statusMessage, msgX, 28); // status message, top center

        if (dying) { // five-second escape countdown (small text, as requested)
            double secondsLeft = Math.max(0, (dyingEnd - now) / 1000.0); // time left
            String timer = String.format("ESCAPE TIME: %.1f s", secondsLeft); // formatted text
            g2d.setColor(Color.RED); // urgent color
            g2d.drawString(timer, Math.max(300, (panelWidth - small.stringWidth(timer)) / 2), 58); // under the message
        } // end of countdown text
    } // end of drawHud
} // end of TreasureMaze
