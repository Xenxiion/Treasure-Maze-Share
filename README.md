# TreasureMaze2 — Complete Game and Java Code Walkthrough

> **Purpose:** This is a learning guide to the actual Java source in this project. It explains how the game runs, what each source file is responsible for, how the main methods cooperate, and how to trace individual lines in the code. It is written for a beginner who needs to understand and defend the project—not just run it.

> **Source accuracy:** This guide was prepared from the Java files in the supplied `TreasureMaze2` archive. It describes what that source currently does, not what a future version is supposed to do. The archive has a few behaviors worth noticing: the exit can be unlocked by opening the real chest **or collecting every coin**; `MimicLogic.loadMimicsFromMap` adds one disguised Mimic on every level; and `GameMap` specifies 1 regular Mimic for Levels 1–2 and 2 regular Mimics for Level 3. Therefore, the counts in the current source are Level 1: 2 total (1 regular + 1 disguised), Level 2: 2 total (1 regular + 1 disguised), and Level 3: 3 total (2 regular + 1 disguised), assuming placement succeeds. Always check the code itself if it has been edited after this archive.

## Contents

1. [What the game does](#1-what-the-game-does)
2. [How the game runs from launch to exit](#2-how-the-game-runs-from-launch-to-exit)
3. [Map symbols and core Java ideas](#3-map-symbols-and-core-java-ideas)
4. [How the main game systems work together](#4-how-the-main-game-systems-work-together)
5. [Detailed file-by-file code walkthrough](#5-detailed-file-by-file-code-walkthrough)
6. [Important behaviors and source-level facts](#6-important-behaviors-and-source-level-facts)
7. [How to trace a feature through the code](#7-how-to-trace-a-feature-through-the-code)
8. [Compile and run](#8-compile-and-run)
9. [Line-number index for every Java file](#9-line-number-index-for-every-java-file)
10. [Java syntax quick reference](#10-java-syntax-quick-reference)

---

## 1. What the game does

**TreasureMaze2 / The Last Symbol** is a Java desktop maze game built with Swing. The player moves through a randomly generated grid, collects optional coins, opens a treasure chest or completes the coin objective to unlock the exit, avoids black bombs, and avoids Mimics that can patrol, chase, investigate sounds, search a last-known location, or be stunned by a blue trap bomb. Level 1 also uses crumbling/vanishing floor tiles. The game draws pixel-art sprites when assets are available and can fall back to text symbols.

The game is divided into responsibilities instead of placing every rule in one class:

- `Main` starts the program.
- `GameUI` owns the application window and switches screens.
- `TreasureMaze` coordinates the live game: input, timer, health, movement, level loading, collisions, pause state, and drawing.
- `GameMap` creates and validates randomized maps.
- `Player` stores the player's grid coordinates.
- Classes in `logic/` implement separate rules for Mimics, pathfinding, coins, bombs, treasure, score/lives, levels, and vanishing tiles.
- `Sprites` loads and draws images.
- `SoundManager` plays music and sound effects.
- `SaveManager` loads and saves the high score.
- Classes in `ui/` build the menu, start screen, preview, and results screen.

## 2. How the game runs from launch to exit

### Step 1 — Java starts at `src/Main.java`

The Java Virtual Machine calls `main(String[] args)`. `SwingUtilities.invokeLater(...)` schedules the creation of the Swing interface on Swing's event-dispatch thread. It creates `GameUI`, then asks it to show the window.

### Step 2 — `GameUI` creates the window and screens

`GameUI` creates a `JFrame` (the operating-system window), a `CardLayout` (a layout that displays one panel at a time), and panels for the menu, instructions, preview, game, and results. When the user selects a screen, `showScreen(...)` switches the visible card.

### Step 3 — starting a game constructs or resets `TreasureMaze`

`TreasureMaze` creates its rule managers (`LevelManager`, `BombLogic`, `CoinLogic`, `TreasureLogic`, `MimicLogic`, `VanishLogic`, `ScoreManager`) and its sprite/audio helpers. It installs a keyboard listener and a Swing `Timer`. The timer calls `onTick()` every 50 milliseconds; the timer drives timed gameplay, AI updates, countdowns, vanishing tiles, and animation updates.

### Step 4 — `LevelManager` asks `GameMap` for a map

`GameMap.generate(level)` chooses that level's width, height, shortcut count, bomb count, regular Mimic count, and coin count. It carves a random maze, places the start/chest/exit/coins/Mimics/bombs, checks routes and spacing, and retries invalid layouts. A map is stored as an array of strings, one string per row.

### Step 5 — the map's symbols are loaded into rule managers

`TreasureMaze.loadCurrentLevel()` finds `P` (player start) and `E` (exit), positions the player, and passes the map to `BombLogic`, `TreasureLogic`, `CoinLogic`, `MimicLogic`, and `VanishLogic`. These managers scan the map and turn marker characters into positions/state objects.

### Step 6 — input changes the player or game state

The key listener forwards key presses to the game input handler. Movement updates the player's grid coordinates only when the destination is valid, applies move-limit rules, and then checks for coins, bombs, Mimic contact, and other cell interactions. Chest interaction goes through `interactWithChest()` and `openRealChest()` / `revealMimicChest(...)`. Blue bombs are placed through `placeBlueBomb()`.

### Step 7 — the timer updates the world

`onTick()` is the recurring update loop. It exits early when the game is paused or finished; otherwise it advances timed effects, Mimic AI, explosion animations, the zero-health countdown, and vanishing tiles. Mimic movement can create footstep sounds. Sound investigation is handled through `MimicLogic.hearNoise(...)` and related calls.

### Step 8 — Swing draws the current state

Swing calls `paintComponent(Graphics g)`. The method paints the background, map cells, sprites, items, Mimics, player, effects, HUD, and any pause overlay. Drawing reads the game state; it should not be the place where gameplay rules are changed.

### Step 9 — winning or losing ends the session

The game marks itself finished, stops the active game timer as appropriate, updates/saves the high score, and tells `GameUI` to show the results screen. Main Menu / Quit actions should stop game activity and audio so the old game does not keep running behind another screen.

## 3. Map symbols and core Java ideas

### Map characters

| Character | Meaning in the current source |
|---|---|
| `#` | Wall; the player and Mimics cannot walk through it. |
| `.` | Ordinary walkable floor and a candidate cell for generated objects. |
| `P` | Player's spawn position in a generated map. |
| `T` | Real treasure chest position. The fake chest is stored separately by `MimicLogic`, not as a second `T` marker. |
| `E` | Exit position. |
| `B` | Black bomb pickup / hazardous bomb cell. `BombLogic` consumes it when triggered. |
| `U` | Blue Mimic-trap bomb pickup. |
| `M` | Spawn/home marker for a regular Mimic. |
| `C` | Coin. |

### Coordinates: `x` and `y`

`x` is the column (left to right); `y` is the row (top to bottom). A `Point(x, y)` stores a grid coordinate. Java arrays are indexed as `[row][column]`, so map access usually looks like `map[y].charAt(x)` or `grid[y][x]`. Mixing up `x` and `y` is a common cause of misplaced objects.

### Arrays, lists, and objects

- `String[] map`: one string per row. A row's character at a column describes that cell.
- `char[][] grid`: editable character grid used while generating a maze. It is converted to `String[]` after generation.
- `ArrayList<Point>`: a list of positions that can grow and shrink as items are collected.
- A class such as `BombLogic` groups state and operations for one system.
- `private`: only the class itself can directly access a member.
- `public`: other classes can call it.
- `static`: the member belongs to the class rather than one instance.
- `final`: the reference/value cannot be reassigned (for a collection, the collection can still be modified unless wrapped).
- `return`: exits a method and optionally gives a value back to its caller.
- `if / else`: selects behavior based on a condition.
- `for` loops: visit rows, columns, positions, or objects.
- `null`: no object/reference is available; code checks it before use.
- `Point` is mutable, so the code often copies points when it needs to avoid sharing coordinates.

## 4. How the main game systems work together

### Random maze generation

`GameMap.generate()` chooses a level specification and calls `buildValid()`. `tryBuild()` begins with walls, uses randomized depth-first maze carving to create corridors, opens shortcuts, and uses breadth-first-search distance maps to select reachable positions. It places required markers and then tests whether the chest, exit, and coins remain reachable without stepping on black bombs. If a candidate map fails validation, generation tries again.

### Pathfinding

`Pathfinder.distances()` performs breadth-first search (BFS). It starts at one cell, explores walkable neighboring cells, and records the number of steps to each reachable cell. A distance of `-1` means unreachable. `nextStep()` uses the distance table to choose a neighboring cell that gets closer to the target. The same logic supports shortest routes, safe-route checks, and Mimic navigation.

### Mimic AI

`MimicLogic` owns Mimic objects and their states. The code uses states such as patrol, chase, investigate, search, and waking. A Mimic sees the player only within its sight range and if `canSee(...)` finds no wall blocking the line. If it loses sight while chasing, it investigates the last known player position rather than receiving the player's exact live location. Sound events can redirect nearby Mimics to investigate. Stun timers temporarily skip AI movement; when the timer expires, the normal state machine resumes.

A disguised Mimic is initially kept still and drawn as a chest. `revealAt(...)` is called when the game interaction code decides the player opened that fake chest; the Mimic then wakes and can chase. The fake chest does not come from the `T` map character—it is held in Mimic state, which is why it can be drawn exactly like a chest until revealed.

### Black and blue bombs

`BombLogic` tracks three separate lists: black bombs still on the map, blue pickups still on the map, and blue traps the player has placed. A black bomb is removed when `triggerAt(...)` finds the player on its cell. A blue pickup is removed when `collectBlueAt(...)` finds the player on it. A placed trap is added by `placeTrap(...)` and removed when it triggers. `TreasureMaze` owns the carried blue-bomb counter and uses `MimicLogic` to apply the stun and sound effects.

### Coins, treasure, exit, and score

`CoinLogic` tracks remaining coins and the original total. When the player enters a coin cell, `collectAt(...)` removes that coin and returns `true`, allowing `TreasureMaze` to award points and move bonus. `TreasureLogic` tracks the real chest and whether it has been claimed. `ScoreManager` stores score and lives. In the current source, `TreasureMaze.isExitUnlocked()` checks the real chest **or** whether all coins are collected; this is an alternate objective in the current code, not merely optional scoring. The preview/rules should be aligned with this behavior unless the gameplay is deliberately changed.

### Vanishing floor

`VanishLogic` remembers the time a player leaves a crumbling cell and the time a vanished cell can return. It protects important cells passed to `load(...)`, such as the exit, chest, and start. `update(now)` advances these timers. `TreasureMaze` asks whether a tile is crumbling or vanished before drawing or allowing movement.

### Pause and audio

`TreasureMaze.setPaused(...)` records pause time and changes the game state. `clock()` compensates for time spent paused so game countdowns can use game time instead of raw wall-clock time. `SoundManager` separately tracks whether music was requested, whether it is muted, and whether it is paused. That separation lets the game pause audio without losing the user's mute choice.

---

## 5. Detailed file-by-file code walkthrough

This section explains each Java file and its methods. Line numbers refer to the source files in this archive; if the files are edited, line numbers may shift. For a literal statement, open the file and use the method's name or line range in your editor. Existing inline comments explain many individual statements; the explanations below tell you why the statements are grouped together and how each part affects the game.

### `src/Main.java`

**Role:** application entry point. `main(String[] args)` is the method Java calls first. `SwingUtilities.invokeLater(...)` schedules UI creation on Swing's event thread; the lambda creates `GameUI` and displays the window. The imports bring in the Swing thread helper and the app's window controller. There is no game logic here: this file only starts the application.
**Method-by-method index:**

- **Lines 6–11, `main(...)`:** program entry point. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/audio/SoundManager.java`

**Role:** loads and plays sound effects/music. `clips` maps sound names to loaded audio clips; `playCounts` counts play requests (useful for checking behavior); `music` holds the background track; `musicWanted`, `musicPaused`, and mute state represent separate audio decisions. The constructor optionally loads files. `loadAll()` loads named audio assets. `openClip(file)` opens a WAV file into a Java Sound `Clip`; errors are handled if the file/audio line is unavailable. `setGain(clip,decibels)` adjusts volume when the clip supports gain control. `play(name)` restarts/plays the requested effect if audio is available and the sound is allowed. `getPlayCount(name)` returns the request count. `startMusic()` marks music as wanted and starts it if allowed. `pauseMusic()` pauses the music without changing mute state. `resumeMusic()` clears the paused state and reapplies the audio state. `toggleMute()` changes mute and returns the new setting. `applyMusicState()` makes the clip match the combination of wanted/paused/muted flags, preventing pause from accidentally toggling mute. `isAudioAvailable()` reports whether audio loaded successfully.
**Method-by-method index:**

- **Lines 45–68, `loadAll(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 70–79, `openClip(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 81–87, `setGain(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 90–97, `play(...)`:** play a named sound effect. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 100–102, `getPlayCount(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 105–108, `startMusic(...)`:** request background music playback. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 111–114, `pauseMusic(...)`:** pause music without changing mute. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 117–120, `resumeMusic(...)`:** resume music if it is wanted and not muted. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 123–127, `toggleMute(...)`:** toggle mute state. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 129–139, `applyMusicState(...)`:** apply wanted/paused/muted state to the music clip. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 141–143, `isAudioAvailable(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/data/AssetPaths.java`

**Role:** finds the `assets` folder and builds asset paths. The private constructor prevents instantiation. `dir()` checks candidate locations so assets can be found when launching from the project root or a build folder. `file(relativePath)` combines the asset directory with a relative file path. Centralizing this prevents every sprite/audio class from hard-coding a different root path.
**Method-by-method index:**

- **Lines 15–23, `dir(...)`:** locate the assets root folder. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 26–28, `file(...)`:** resolve a relative asset path. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/data/SaveManager.java`

**Role:** reads and writes the high score. A logger reports read/write problems. `LEADING_DIGITS` is a regular expression used to extract a number from the stored text. The private constructor makes the class utility-only. `loadHighScore()` reads the saved value if the file exists, parses it safely, and returns a fallback if loading fails. `saveHighScore(score)` writes a new record when appropriate and catches I/O errors so a save failure does not crash the game. This class stores persistent high-score data; current level score and health live in `ScoreManager`.
**Method-by-method index:**

- **Lines 23–38, `loadHighScore(...)`:** read the saved high score. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 41–50, `saveHighScore(...)`:** persist a high-score record. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/game/GameMap.java`

**Role:** randomized level factory. `LEVEL_COUNT` says there are three levels. `SPEC` stores each level's width, height, shortcut count, total bomb pickups, regular Mimic count, and coin count. `BLUE_BOMBS` tells the generator how many bomb markers are converted to blue. `generate(level)` clamps the requested level to 1–3, calls `buildValid`, and avoids returning the same exact map twice in a row for that level. `find(map, symbol)` scans rows and returns the first coordinate containing a marker. `buildValid(idx)` retries up to `MAX_ATTEMPTS` until `tryBuild` returns a valid map. `tryBuild(spec, level)` builds a grid and places the objects; a failed placement returns `null` so the caller can retry. `makeBlueBombs(grid, start)` changes a subset of black bomb markers into blue markers while checking reachability and spread. `safeRoutesExist(...)` checks that the required objective cells remain reachable without crossing black bombs. `patrolIsValid(...)` rejects Mimic patrol routes that come too close to important cells. `farFromOtherHomes(...)` spaces Mimic homes apart. `pickCoins(...)` distributes coin cells. `carveMaze(...)` digs randomized corridors with a stack/backtracking algorithm. `openShortcuts(...)` removes selected walls to add loops and reduce dead ends. `pickInBand(...)` selects a cell whose BFS distance lies in a requested range. `maxOf(...)` finds the largest reachable distance. `toMap(...)` converts the mutable character grid into row strings. The nested loops are generally scanning every row/column; the `continue` statements reject unsafe candidates early; the `return null` statements mean “this random attempt did not meet the rules—try again.”
**Method-by-method index:**

- **Lines 47–59, `generate(...)`:** generate and validate a randomized level map. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 62–70, `find(...)`:** find the first occurrence of a map symbol. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 72–80, `buildValid(...)`:** retry generation until a valid map is found. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 83–184, `tryBuild(...)`:** construct and validate one candidate map. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 190–232, `makeBlueBombs(...)`:** convert reachable black bomb markers into blue pickups. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 235–242, `safeRoutesExist(...)`:** check required paths remain reachable without black bombs. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 244–253, `patrolIsValid(...)`:** reject unsafe Mimic patrol routes. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 255–260, `farFromOtherHomes(...)`:** keep Mimic homes apart. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 262–299, `pickCoins(...)`:** choose separated coin positions. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 301–325, `carveMaze(...)`:** carve randomized corridors through the wall grid. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 327–340, `openShortcuts(...)`:** open extra connections between corridors. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 342–354, `pickInBand(...)`:** choose a cell by its BFS distance range. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 356–364, `maxOf(...)`:** find the maximum reachable distance. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 366–372, `toMap(...)`:** convert character rows into strings. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/game/Player.java`

**Role:** minimal player-position model. The constructor stores the starting `x` and `y`. `getX()` and `getY()` return the current column and row. `setPosition(x, y)` assigns an exact location (used for spawn/reset). `move(dx, dy)` adds a relative change to the current location. The class intentionally does not decide whether a move is safe; `TreasureMaze` checks walls, bombs, and game rules before allowing movement.
**Method-by-method index:**

- **Lines 12–14, `getX(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 16–18, `getY(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 20–23, `setPosition(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 25–28, `move(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/game/Sprites.java`

**Role:** sprite/image loading and drawing. The constructor registers image names and paths, then calls `load(...)`. `load(name, relativePath)` resolves the asset path, reads the image, and stores it in a map keyed by sprite name; loading errors are handled so missing art does not crash the whole game. `has(name)` checks whether an image exists. `draw(...)` draws a sprite into a square cell and can flip it horizontally (for facing direction). `drawScaled(...)` draws to a custom width/height. `explosionFrames()` reports how many explosion images are available. The map lets the renderer request images by a short name instead of reopening files every frame.
**Method-by-method index:**

- **Lines 48–60, `load(...)`:** reset vanishing-floor state for a level. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 62–64, `has(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 67–76, `draw(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 79–84, `drawScaled(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 86–88, `explosionFrames(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/game/TreasureMaze.java`

**Role:** central controller for the active game. It connects keyboard input, player movement, map objects, AI, timers, scoring, sound, and drawing.

- **Constants (roughly lines 30–65):** define move cooldown, Level 3 minimum tile size, HUD space, move-budget multipliers/fixed budgets, per-level Mimic damage, catch stun, blue-bomb stun/radius, coin move bonus, escape countdown, sound ranges, explosion duration, and timer interval. Constants keep tuning values in one place.
- **`Explosion` inner class:** stores the cell and start time for a temporary explosion visual. It does not itself implement bomb rules.
- **Fields (roughly lines 68–101):** hold the player and each system manager, current map/start/exit, remaining moves, score/life state, animation flags, blue bomb count, pause/quit state, and the single Swing timer. These fields are the current live game state.
- **Constructor (line 103):** stores the UI/audio references, configures the panel, makes it keyboard-focusable, installs keyboard and mouse listeners, creates the timer, and loads the current level. The keyboard listener delegates to the game handler rather than containing all behavior itself.
- **`resetGameToLevel1()` (line 126):** resets level progression, score/lives, pause/death flags, carried blue bombs, and timer state, then loads Level 1 and repaints.
- **`loadCurrentLevel()` (line 139):** gets the active map; finds `P` and `E`; places the player; reloads bombs, chest, coins, Mimics, and vanishing tiles; clears old explosions; resets level-start bomb count; calculates moves; and displays a level hint/status message.
- **`computeMoveBudget(level)` (line 165):** chooses a fixed budget for levels that use one; otherwise computes a route-based budget from path distance, multiplier, and slack. It is intended to make the move allowance reflect map size/route length.
- **`isExitUnlocked()` (line 180):** checks whether the chest has been claimed or all coins are collected. This means coins are an alternative unlock objective in the uploaded source.
- **`damagePlayer(message, lethal)` and overload (lines 189–207):** centralize damage. The shorter overload applies one heart; the second applies the requested number, prevents damage after the game is already dying/finished, updates health, sets feedback/flash state, and starts the escape countdown if health reaches zero.
- **`beginCountdown()` (line 208):** begins the five-second zero-health escape window.
- **`finish(won)` (line 216):** marks the game finished, stops the game timer, and tells `GameUI` to display the result.
- **`onTick()` (line 227):** timer-driven update loop. It avoids updating when paused/finished; updates Mimic AI, sound and catch checks, bomb/trap effects, vanishing floor state, explosion lifetimes, and countdown timing; and repaints when state changes. The timer is why AI and timed effects progress even when no key is pressed.
- **`clock()` (line 409):** returns game time adjusted for time spent paused, so countdown/stun timing does not incorrectly include paused time.
- **`setPaused(pause)` (line 414):** changes pause state, records pause start/total pause duration, and tells the sound manager to pause/resume music. It uses the existing timer rather than creating a second timer.
- **`stopGame()` (line 430):** stops the session's timer and related activity when leaving the game.
- **`handlePauseKey(e)` (line 437):** handles keyboard choices while the pause menu is open, including resume, main menu, and quit confirmation.
- **`drawPauseMenu(...)` (line 460):** draws the dimmed overlay and pause choices; it should only render state, not advance gameplay.
- **`openRealChest()` (line 484):** runs the real-chest reward/opening behavior and status/sound updates.
- **`revealMimicChest(x, y)` (line 496):** asks `MimicLogic` to reveal the disguised Mimic at the selected cell and updates feedback/sound when reveal succeeds.
- **`interactWithChest()` (line 508):** handles the chest interaction key. It decides whether the nearby chest is real or disguised, then routes to the corresponding action.
- **`placeBlueBomb()` (line 531):** checks the carried blue-bomb count, asks `BombLogic` to place a trap at the player cell (or the chosen target cell according to this implementation), decrements the count only on successful placement, and updates the message/UI.
- **`nearChest()` (line 548):** tests whether the player is close enough to the chest for interaction. Check this method's distance rule before changing interaction range.
- **`checkMimicCatch()` (line 554):** detects player/Mimic contact and routes damage through the damage method. Catch/stun state prevents continuous contact from subtracting health on every timer tick.
- **`restartLevelWithPenalty()` (line 562):** reloads the current level for the R-key restart and applies the existing health penalty.
- **`paintComponent(g)` (line 578):** the main rendering routine. It clears/paints the panel, calculates tile size/scrolling, draws walls/floor and entities, overlays effects, and calls HUD/pause drawing. `super.paintComponent(g)` clears the previous frame. `Graphics2D` is used for sprites, alpha/transparency, and text.
- **`wallSprite(c, r)` (line 696):** selects a stable wall sprite based on position so the wall pattern varies but does not flicker randomly each frame.
- **`drawTileOrLetter(...)`, `drawEntity(...)`, `drawLetter(...)` (lines 702–728):** shared drawing helpers. They prefer sprite images when present and otherwise draw readable character fallbacks.
- **`drawHud(...)` (line 729):** draws status information such as health, score, level, moves, coins, messages, and countdown state.

**How to read this class:** it is the coordinator, not the owner of every rule. For example, it decides *when* a coin pickup check should happen, while `CoinLogic` owns the list of coins; it decides *when* Mimic AI updates, while `MimicLogic` chooses Mimic movement.
**Method-by-method index:**

- **Lines 111–113, `keyPressed(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 117–119, `mouseClicked(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 126–137, `resetGameToLevel1(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 139–163, `loadCurrentLevel(...)`:** load map objects and reset level-specific state. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 165–178, `computeMoveBudget(...)`:** calculate the player movement allowance. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 180–182, `isExitUnlocked(...)`:** check the current exit-unlock conditions. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 189–191, `damagePlayer(...)`:** apply health damage and handle zero health. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 208–214, `beginCountdown(...)`:** start the five-second escape countdown. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 216–224, `finish(...)`:** end the session and show results. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 227–268, `onTick(...)`:** run one periodic gameplay update. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 409–412, `clock(...)`:** return game time excluding paused duration. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 414–427, `setPaused(...)`:** pause/resume gameplay and music. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 430–435, `stopGame(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 437–458, `handlePauseKey(...)`:** handle input while the pause overlay is open. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 460–481, `drawPauseMenu(...)`:** draw the pause overlay. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 484–493, `openRealChest(...)`:** open the real chest and award its normal result. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 496–501, `revealMimicChest(...)`:** reveal the disguised Mimic at a cell. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 508–528, `interactWithChest(...)`:** route chest interaction to real chest or disguised Mimic. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 531–546, `placeBlueBomb(...)`:** place a carried blue trap bomb. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 548–551, `nearChest(...)`:** check player proximity to the chest. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 554–560, `checkMimicCatch(...)`:** detect a Mimic catch and apply damage. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 562–573, `restartLevelWithPenalty(...)`:** restart the level with the existing penalty. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 578–694, `paintComponent(...)`:** render the current game state. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 696–700, `wallSprite(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 702–709, `drawTileOrLetter(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 711–720, `drawEntity(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 722–727, `drawLetter(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 729–777, `drawHud(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/logic/BombLogic.java`

**Role:** stores bomb positions, not the whole gameplay response. `loadBombsFromMap(map)` clears old state and scans the map: `B` positions go into the black-bomb list; `U` positions go into the blue-pickup list; placed traps are cleared on level load. `triggerAt(x,y)` finds a black bomb at the player cell, removes it, and returns `true` so the caller knows to apply the effect exactly once. `collectBlueAt(x,y)` removes one blue pickup and returns `true` only on collection. `placeTrap(x,y)` refuses a second trap at the same cell, otherwise stores a new point. `removeTrap(point)` deletes a triggered trap. The `get...()` methods return unmodifiable views so other code can draw/check positions without directly adding or deleting list entries. The player's carried blue-bomb count is stored in `TreasureMaze`, not in an inventory class.
**Method-by-method index:**

- **Lines 13–28, `loadBombsFromMap(...)`:** read black and blue bomb positions from map symbols. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 31–40, `triggerAt(...)`:** consume a black bomb under the player. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 43–52, `collectBlueAt(...)`:** collect a blue pickup once. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 55–61, `placeTrap(...)`:** place a blue trap if the cell has none. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 63–65, `removeTrap(...)`:** remove a trap after it triggers. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 67–69, `getBluePickups(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 71–73, `getTraps(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 75–77, `getBombPositions(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/logic/CoinLogic.java`

**Role:** tracks the coins that remain on a level. `loadCoinsFromMap(map)` clears old coins, resets `total`, scans every cell for `C`, stores each coordinate, and counts the original total. `collectAt(x,y)` removes a coin on the player cell and returns `true` once; subsequent calls return false because that point is gone. `isComplete()` is true only if the level had at least one coin and no coins remain. `getCollected()` calculates original total minus remaining list size. `getTotal()` returns the starting count. `getPositions()` exposes a read-only list for drawing. The class does not award score itself; the caller awards points/moves after a successful `collectAt` result.
**Method-by-method index:**

- **Lines 16–29, `loadCoinsFromMap(...)`:** load all coin positions from the map. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 32–41, `collectAt(...)`:** remove a coin under the player. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 43–45, `isComplete(...)`:** check whether all coins were collected. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 47–49, `getCollected(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 51–53, `getTotal(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 55–57, `getPositions(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/logic/LevelManager.java`

**Role:** keeps track of the active level and the generated map for the game session. The constructor calls `resetToLevel1()`. That reset sets the active level to 1 and prepares maps. `getCurrentLevel()` returns the level number. `nextLevel()` advances if another level exists and reports success/failure. `getCurrentMap()` returns the active map. This separates level progression from movement and drawing.
**Method-by-method index:**

- **Lines 14–19, `resetToLevel1(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 21–23, `getCurrentLevel(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 25–31, `nextLevel(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 33–35, `getCurrentMap(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/logic/MimicLogic.java`

**Role:** the Mimic AI state machine. Its fields hold the Mimic list, patrol guard locations, previous player position/direction, current game clock, sight range, and step intervals.

- `patrolRadiusFor(level)` chooses how far a Mimic may patrol on a level.
- `buildPatrolRoute(map, home, radius)` uses pathfinding to find a walkable route through the patrol area. It chooses distant endpoints and constructs the path one step at a time.
- `loadMimicsFromMap(map, level)` clears old Mimics/state, adjusts sight and movement timing for the level, records chest/exit guard spots, scans `M` markers into regular Mimic objects, then calls `addDisguised(...)` to add a fake chest Mimic.
- `addDisguised(map, chest, exit)` searches for an unused floor cell far from the player start, real chest, and exit, preferring a dead end. It marks that Mimic as disguised and sets its slow movement multiplier. If no suitable cell exists after progressively relaxing constraints, it returns without adding one.
- `canSee(map,a,b)` samples cells between two points; any blocked cell means the line of sight is blocked.
- `update(playerX,playerY,map,now)` advances each active Mimic. Disguised Mimics remain inert; stunned Mimics do not move; a waking Mimic waits until its wake time; a Mimic that sees the player chases; a chaser that loses sight investigates the last known position. Each Mimic only moves when its next-step time is due. Patrol, investigate, search, and chase decisions happen in this update loop.
- `ahead(map,player)` chooses a cell ahead of the player's recent direction to help an AI intercept without magically reading the current location through walls.
- `randomCell(...)` chooses a valid random target around a center.
- `hearNoise(x,y,map,range)` alerts eligible Mimics near a sound to investigate it.
- `checkCollision(x,y)` checks whether any active Mimic occupies the player cell.
- `anyWithin(x,y,range)` answers whether a Mimic is close enough for an event to be audible.
- `resetToSpawn()` returns Mimics to their home positions.
- `getPositions()` provides visible Mimic positions; `getDisguisedPositions()` provides fake-chest cells; `isDisguisedAt(x,y)` checks one cell; `revealAt(x,y,now)` changes a disguised Mimic into a revealed/waking Mimic; `stunNear(x,y,radius,until)` freezes nearby Mimics; `triggerTrap(...)` applies the trap effect while preventing one trap from triggering repeatedly; `getStunnedPositions()` helps drawing show the stun state; `isActive()` reports whether Mimics exist.

**AI state flow:** patrol → chase when seen; chase → investigate when sight is lost; investigate → search when the last known cell is reached; search eventually returns to patrol. A sound can redirect a Mimic to investigate. A stun temporarily pauses movement and then normal AI resumes. The fake chest is not supposed to move or react before reveal.
**Method-by-method index:**

- **Lines 64–66, `patrolRadiusFor(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 73–110, `buildPatrolRoute(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 112–134, `loadMimicsFromMap(...)`:** create regular Mimics from map markers and add a disguised Mimic. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 140–170, `addDisguised(...)`:** choose a safe cell for a fake chest Mimic. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 173–181, `canSee(...)`:** test whether walls block line of sight. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 187–264, `update(...)`:** advance the system state for this timer tick. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 267–276, `ahead(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 279–292, `randomCell(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 298–311, `hearNoise(...)`:** alert Mimics to investigate a sound. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 313–320, `checkCollision(...)`:** check if a Mimic shares the player cell. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 323–330, `anyWithin(...)`:** check if any Mimic is within a distance. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 332–340, `resetToSpawn(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 342–348, `getPositions(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 350–356, `getDisguisedPositions(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 358–363, `isDisguisedAt(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 369–379, `revealAt(...)`:** reveal the disguised Mimic at the specified cell. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 382–392, `stunNear(...)`:** stun Mimics near a position. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 398–407, `triggerTrap(...)`:** trigger a trap against nearby Mimics. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 409–415, `getStunnedPositions(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 417–419, `isActive(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/logic/Pathfinder.java`

**Role:** shared grid-search utilities. The private constructor prevents creating instances because all operations are static. `walkable(map,x,y,avoidBombs)` checks bounds and cell type, optionally treating black bombs as blocked for safe-route calculations. `distances(map,from,avoidBombs)` performs breadth-first search (BFS): initialize distances as unreachable, set the start to zero, put it in a queue, repeatedly visit its four neighbors, and assign each newly reachable cell its parent's distance plus one. `distance(from,to,...)` reads the target's value from the distance table. `nextStep(from,to,...)` chooses a neighboring cell that reduces the distance to the target, returning the next move or `null` if no route exists. BFS explores by distance layers, so the first distance assigned is a shortest path in an unweighted grid.
**Method-by-method index:**

- **Lines 15–24, `walkable(...)`:** check bounds and whether a map cell can be entered. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 27–51, `distances(...)`:** calculate shortest grid distances with BFS. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 53–55, `distance(...)`:** return the path distance between two cells. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 58–72, `nextStep(...)`:** choose one path step toward a target. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/logic/ScoreManager.java`

**Role:** stores score and remaining hearts. `START_LIVES` defines starting health. The constructor initializes the manager; `resetScore()` restores the new-game values. `addTreasurePoints()` adds the chest reward; `addCoinPoints()` adds points for one coin; `addLevelClearBonus(level)` adds the bonus for clearing a level. `loseLife()` subtracts one heart and reports whether health is empty. `eliminate()` removes all remaining hearts for a lethal event and reports that health is empty. `restoreLife()` adds a heart in the zero-health escape rule. `getLives()` and `getTotalScore()` return current values. It does not decide what caused damage; `TreasureMaze` calls the appropriate method based on the event.
**Method-by-method index:**

- **Lines 13–16, `resetScore(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 18–20, `addTreasurePoints(...)`:** award treasure points. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 22–24, `addCoinPoints(...)`:** award coin points. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 26–28, `addLevelClearBonus(...)`:** award a level completion bonus. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 31–36, `loseLife(...)`:** subtract one heart. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 39–42, `eliminate(...)`:** remove all hearts. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 45–49, `restoreLife(...)`:** restore one heart. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 51–53, `getLives(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 55–57, `getTotalScore(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/logic/TreasureLogic.java`

**Role:** tracks the real treasure chest. `loadTreasureFromMap(map)` finds the `T` cell and resets the claimed state for a new level. `claimIfSteppedOn(x,y)` is a position check: if the player is at the treasure and it has not already been claimed, it marks it claimed and returns true. `isClaimed()` exposes whether the chest was collected/opened. `getPosition()` returns the chest cell for rendering/interaction. The actual interaction flow and rewards are coordinated by `TreasureMaze`; this class stores the chest state.
**Method-by-method index:**

- **Lines 9–21, `loadTreasureFromMap(...)`:** find the real chest and reset its claimed state. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 23–29, `claimIfSteppedOn(...)`:** claim the treasure if the player reaches its cell. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 31–33, `isClaimed(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 35–37, `getPosition(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/logic/VanishLogic.java`

**Role:** manages the Level 1 crumbling floor. The `crumblingUntil` map stores when a tile is expected to vanish; `vanishedUntil` stores when it can return; `protectedCells` marks cells that must never vanish. `load(enabled, protect...)` clears old timers, turns the mechanic on/off for the level, and records protected cells. `isEnabled()` reports whether the mechanic is active. `onLeave(x,y,now)` starts the crumble timer when the player leaves an eligible tile. `onEnter(x,y)` updates the state when entering a cell. `isVanished(...)` tells movement/rendering whether a tile is currently gone. `isCrumbling(...)` tells the renderer whether it should show a warning state. `update(now)` advances expiry/return timers and reports whether the map state changed. The class uses timestamps rather than a separate thread; the main game timer calls `update` regularly.
**Method-by-method index:**

- **Lines 26–36, `load(...)`:** reset vanishing-floor state for a level. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 38–40, `isEnabled(...)`:** perform the operation described by its method name. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 43–48, `onLeave(...)`:** start a tile crumble timer. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 51–53, `onEnter(...)`:** update state when the player enters a tile. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 55–57, `isVanished(...)`:** check whether a tile is currently absent. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 59–61, `isCrumbling(...)`:** check whether a tile is in its warning phase. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 64–85, `update(...)`:** advance the system state for this timer tick. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/ui/GamePreview.java`

**Role:** displays the level preview/map information. The constructor creates labels and text areas describing levels and their mechanics, then adds navigation controls. This class does not generate the maps; `GameMap` does. If a preview count differs from the generation code, correct the preview only after verifying which behavior is intended.

### `src/ui/GameUI.java`

**Role:** owns the application window and screen transitions. The constructor creates the `JFrame`, `CardLayout`, screen panels, sound manager, and game panel. `showWindow()` sets the initial screen, packs/sizes the window, and makes it visible. `showScreen(screenName)` changes which named card is visible and performs screen-specific setup such as starting/stopping audio or requesting focus. `showResults(finalScore,won)` saves a new high score when appropriate, updates the result panel, and switches to it. The class keeps screen navigation out of the actual gameplay logic.
**Method-by-method index:**

- **Lines 25–51, `showWindow(...)`:** display the main application window. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 53–71, `showScreen(...)`:** switch visible UI cards. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.
- **Lines 73–79, `showResults(...)`:** show the final score and outcome. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/ui/MainMenu.java`

**Role:** builds the main menu. The constructor creates the title, buttons, layout constraints, and click actions for starting the game, opening instructions/preview, or quitting. `refreshHighScore()` asks `SaveManager` for the stored record and updates the displayed label. `GridBagLayout` and `GridBagConstraints` position elements; fonts, colors, and insets affect presentation, not gameplay.
**Method-by-method index:**

- **Lines 58–60, `refreshHighScore(...)`:** refresh the menu high-score label. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/ui/ResultScreen.java`

**Role:** displays win/loss results and score. The constructor creates the labels/buttons and their actions. `setResult(score,won)` changes the headline and score text to match the completed game. Buttons route back to the menu or start another game through `GameUI`.
**Method-by-method index:**

- **Lines 59–64, `setResult(...)`:** update result text for a win or loss. Read the body in order: the opening lines validate or prepare inputs, the middle lines do the work, and the final `return`/closing brace completes the method.


### `src/ui/StartScreen.java`

**Role:** displays instructions and controls. The constructor creates the heading, multi-line rules area, and navigation button(s). `JTextArea` stores the readable rule text; layout/font/color code styles it. The displayed rules are plain UI text, so they must be kept consistent with the real rules in `TreasureMaze`, `GameMap`, and the logic classes.

### `src/ui/UiStyle.java`

**Role:** shared visual styling. The private constructor prevents instantiation. Static helper methods configure standard button appearance and shared dimensions/colors/fonts so the UI remains consistent. Styling helpers should not contain game rules.

## 6. Important behaviors and source-level facts

These points matter when explaining the game to a teacher or debugging it:

1. **The map is not a picture.** It is a character grid. Rendering turns each character/position into a sprite or letter.
2. **The game timer is the heartbeat.** The `Timer` calls `onTick()` repeatedly; the timer does not mean a new thread is created for every Mimic.
3. **Movement and AI are separate.** Player key input attempts a player move. Mimic movement occurs during AI updates.
4. **A false return usually means “not done.”** For example, `collectAt(...) == false` means no coin was picked up at that coordinate; the caller should not award points.
5. **BFS distance `-1` means unreachable.** Maze generation uses this to reject layouts that trap the player or important objects.
6. **Lists change when an object is collected/consumed.** Coins and blue pickups are removed from their lists; a black bomb is removed after it triggers; a placed trap is removed after triggering.
7. **The fake chest is represented as a Mimic state, not as a real chest reward.** It is drawn as a chest before reveal and as a Mimic afterward.
8. **Audio state has multiple flags.** “Music is wanted,” “music is paused,” and “audio is muted” are different conditions. Pause should not toggle mute.
9. **The current exit rule is OR, not chest-only.** `TreasureMaze.isExitUnlocked()` allows the exit when the real chest is claimed **or** all coins are collected. If the intended design is “real chest required, coins optional,” that gameplay rule must be deliberately changed and tested; editing only the rules text would make the guide inaccurate.
10. **Counts in this uploaded source:** `GameMap.SPEC` asks for 1 regular Mimic in Level 1, 1 in Level 2, and 2 in Level 3. `MimicLogic.loadMimicsFromMap()` calls `addDisguised()` for each level. Therefore this version attempts totals of 2, 2, and 3, respectively. The code's `addDisguised()` can return without adding a fake chest if no eligible floor cell exists, although its fallback tiers are intended to make that rare.
11. **Bomb pickup count in the map specification:** each level requests 25 bomb pickups; `makeBlueBombs` converts five `B` markers into `U`, producing the intended 20 black + 5 blue split if generation succeeds.
12. **The README explains code; it does not prove the project compiles.** Compilation and gameplay tests must be run separately before claiming they passed.

## 7. How to trace a feature through the code

### Example: collecting a coin

1. `GameMap.tryBuild(...)` places a `C` marker in the generated grid.
2. `TreasureMaze.loadCurrentLevel()` calls `coinSystem.loadCoinsFromMap(currentMap)`.
3. `CoinLogic.loadCoinsFromMap(...)` records the coin's coordinate and increases the total count.
4. After player movement, `TreasureMaze` checks the player cell with `coinSystem.collectAt(x,y)`.
5. If it returns `true`, the game awards points and the coin's move bonus, updates the status, and repaints.
6. `paintComponent(...)` draws the remaining positions returned by `coinSystem.getPositions()`. Because the collected point was removed, it no longer appears.

### Example: a black bomb

1. The generator places `B` on an eligible floor cell and validates the safe routes.
2. `BombLogic.loadBombsFromMap(...)` records that position.
3. When the player enters the cell, the game calls `triggerAt(...)`.
4. If it returns `true`, the bomb has already been removed from the live list, so it cannot trigger a second time at that cell.
5. `TreasureMaze` applies the level's bomb/damage rule, creates an explosion visual/sound, and notifies nearby Mimics about the noise where applicable.

### Example: blue bomb trap

1. `GameMap.makeBlueBombs(...)` converts five bomb markers from `B` to `U`.
2. `BombLogic.loadBombsFromMap(...)` loads `U` into the blue-pickup list.
3. Walking over one calls `collectBlueAt(...)`, which removes the pickup. The game increments its simple carried-blue-bomb integer.
4. SPACE calls the blue-bomb placement method. A successful placement consumes one carried bomb and records a trap position.
5. The timer checks placed traps. A triggered trap is removed and `MimicLogic.stunNear(...)` sets a stun-until time for Mimics in range.
6. While the game clock is before that time, the Mimic update skips movement. After the time expires, normal AI continues.

### Example: Mimic chase and sound investigation

1. `MimicLogic.update(...)` receives the player's current cell and the game clock.
2. A Mimic chases only if the player is within its sight range and `canSee(...)` finds no blocking wall.
3. If sight is lost, it uses the last known location and enters investigation rather than reading the player's exact live location through walls.
4. A sound event can call `hearNoise(...)`; eligible nearby Mimics switch to investigating the sound source.
5. The main game checks collision and calls the central damage function when a Mimic catches the player.

### Example: pausing

1. ESC is handled by the game key handler.
2. `setPaused(true)` marks the game paused and records the pause timestamp.
3. `onTick()` skips gameplay updates while paused, so AI, timed effects, and countdowns do not advance.
4. `SoundManager.pauseMusic()` pauses music without changing the mute flag.
5. On resume, `setPaused(false)` adjusts paused time and resumes music according to the current mute state.

## 8. Compile and run

From the `TreasureMaze2` project root, ensure a JDK is installed and available in the terminal. Because this is a plain Java/Swing project with packages under `src`, one straightforward Windows PowerShell workflow is:

```powershell
# From the TreasureMaze2 folder
New-Item -ItemType Directory -Force out | Out-Null
$files = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d out $files
java -cp out Main
```

If your setup requires assets to be in a particular working directory, launch from the project root so `assets/` can be found. If the game window opens but images/audio are missing, verify the `assets/` directory and the paths resolved by `AssetPaths`.

### Common issues

- **`javac` is not recognized:** install a JDK and configure `PATH`.
- **`file not found`:** run the command from the project root and check the exact file/folder spelling; avoid accidental spaces in filenames.
- **`class X is public, should be declared in X.java`:** the public class name and filename must match exactly.
- **`package ... does not exist`:** compile all source files together, including their package folders, instead of compiling only one file.
- **Missing sprites/sounds:** verify asset paths and the current working directory.
- **Keyboard does not respond:** click the game panel to restore focus; the panel requests keyboard focus on mouse click.
- **Unexpected game rules:** compare the rule text with `TreasureMaze` and the relevant logic class before editing the UI text.

## 9. Line-number index for every Java file

The line ranges below are from the source archive used to write this guide. Use VS Code's **Ctrl+G** to jump to a line, or **Ctrl+F** to search for a method name. The file-by-file section above explains the purpose of each method; comments already present next to individual statements explain many local operations.

### `src/Main.java` — 12 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 6–11 | `main(...)` | Method-by-method explanation in Section 5. |

### `src/audio/SoundManager.java` — 144 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 45–68 | `loadAll(...)` | Method-by-method explanation in Section 5. |
| 70–79 | `openClip(...)` | Method-by-method explanation in Section 5. |
| 81–87 | `setGain(...)` | Method-by-method explanation in Section 5. |
| 90–97 | `play(...)` | Method-by-method explanation in Section 5. |
| 100–102 | `getPlayCount(...)` | Method-by-method explanation in Section 5. |
| 105–108 | `startMusic(...)` | Method-by-method explanation in Section 5. |
| 111–114 | `pauseMusic(...)` | Method-by-method explanation in Section 5. |
| 117–120 | `resumeMusic(...)` | Method-by-method explanation in Section 5. |
| 123–127 | `toggleMute(...)` | Method-by-method explanation in Section 5. |
| 129–139 | `applyMusicState(...)` | Method-by-method explanation in Section 5. |
| 141–143 | `isAudioAvailable(...)` | Method-by-method explanation in Section 5. |

### `src/data/AssetPaths.java` — 29 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 15–23 | `dir(...)` | Method-by-method explanation in Section 5. |
| 26–28 | `file(...)` | Method-by-method explanation in Section 5. |

### `src/data/SaveManager.java` — 51 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 23–38 | `loadHighScore(...)` | Method-by-method explanation in Section 5. |
| 41–50 | `saveHighScore(...)` | Method-by-method explanation in Section 5. |

### `src/game/GameMap.java` — 373 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 47–59 | `generate(...)` | Method-by-method explanation in Section 5. |
| 62–70 | `find(...)` | Method-by-method explanation in Section 5. |
| 72–80 | `buildValid(...)` | Method-by-method explanation in Section 5. |
| 83–184 | `tryBuild(...)` | Method-by-method explanation in Section 5. |
| 190–232 | `makeBlueBombs(...)` | Method-by-method explanation in Section 5. |
| 235–242 | `safeRoutesExist(...)` | Method-by-method explanation in Section 5. |
| 244–253 | `patrolIsValid(...)` | Method-by-method explanation in Section 5. |
| 255–260 | `farFromOtherHomes(...)` | Method-by-method explanation in Section 5. |
| 262–299 | `pickCoins(...)` | Method-by-method explanation in Section 5. |
| 301–325 | `carveMaze(...)` | Method-by-method explanation in Section 5. |
| 327–340 | `openShortcuts(...)` | Method-by-method explanation in Section 5. |
| 342–354 | `pickInBand(...)` | Method-by-method explanation in Section 5. |
| 356–364 | `maxOf(...)` | Method-by-method explanation in Section 5. |
| 366–372 | `toMap(...)` | Method-by-method explanation in Section 5. |

### `src/game/Player.java` — 29 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 12–14 | `getX(...)` | Method-by-method explanation in Section 5. |
| 16–18 | `getY(...)` | Method-by-method explanation in Section 5. |
| 20–23 | `setPosition(...)` | Method-by-method explanation in Section 5. |
| 25–28 | `move(...)` | Method-by-method explanation in Section 5. |

### `src/game/Sprites.java` — 89 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 48–60 | `load(...)` | Method-by-method explanation in Section 5. |
| 62–64 | `has(...)` | Method-by-method explanation in Section 5. |
| 67–76 | `draw(...)` | Method-by-method explanation in Section 5. |
| 79–84 | `drawScaled(...)` | Method-by-method explanation in Section 5. |
| 86–88 | `explosionFrames(...)` | Method-by-method explanation in Section 5. |

### `src/game/TreasureMaze.java` — 778 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 111–113 | `keyPressed(...)` | Method-by-method explanation in Section 5. |
| 117–119 | `mouseClicked(...)` | Method-by-method explanation in Section 5. |
| 126–137 | `resetGameToLevel1(...)` | Method-by-method explanation in Section 5. |
| 139–163 | `loadCurrentLevel(...)` | Method-by-method explanation in Section 5. |
| 165–178 | `computeMoveBudget(...)` | Method-by-method explanation in Section 5. |
| 180–182 | `isExitUnlocked(...)` | Method-by-method explanation in Section 5. |
| 189–191 | `damagePlayer(...)` | Method-by-method explanation in Section 5. |
| 208–214 | `beginCountdown(...)` | Method-by-method explanation in Section 5. |
| 216–224 | `finish(...)` | Method-by-method explanation in Section 5. |
| 227–268 | `onTick(...)` | Method-by-method explanation in Section 5. |
| 409–412 | `clock(...)` | Method-by-method explanation in Section 5. |
| 414–427 | `setPaused(...)` | Method-by-method explanation in Section 5. |
| 430–435 | `stopGame(...)` | Method-by-method explanation in Section 5. |
| 437–458 | `handlePauseKey(...)` | Method-by-method explanation in Section 5. |
| 460–481 | `drawPauseMenu(...)` | Method-by-method explanation in Section 5. |
| 484–493 | `openRealChest(...)` | Method-by-method explanation in Section 5. |
| 496–501 | `revealMimicChest(...)` | Method-by-method explanation in Section 5. |
| 508–528 | `interactWithChest(...)` | Method-by-method explanation in Section 5. |
| 531–546 | `placeBlueBomb(...)` | Method-by-method explanation in Section 5. |
| 548–551 | `nearChest(...)` | Method-by-method explanation in Section 5. |
| 554–560 | `checkMimicCatch(...)` | Method-by-method explanation in Section 5. |
| 562–573 | `restartLevelWithPenalty(...)` | Method-by-method explanation in Section 5. |
| 578–694 | `paintComponent(...)` | Method-by-method explanation in Section 5. |
| 696–700 | `wallSprite(...)` | Method-by-method explanation in Section 5. |
| 702–709 | `drawTileOrLetter(...)` | Method-by-method explanation in Section 5. |
| 711–720 | `drawEntity(...)` | Method-by-method explanation in Section 5. |
| 722–727 | `drawLetter(...)` | Method-by-method explanation in Section 5. |
| 729–777 | `drawHud(...)` | Method-by-method explanation in Section 5. |

### `src/logic/BombLogic.java` — 78 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 13–28 | `loadBombsFromMap(...)` | Method-by-method explanation in Section 5. |
| 31–40 | `triggerAt(...)` | Method-by-method explanation in Section 5. |
| 43–52 | `collectBlueAt(...)` | Method-by-method explanation in Section 5. |
| 55–61 | `placeTrap(...)` | Method-by-method explanation in Section 5. |
| 63–65 | `removeTrap(...)` | Method-by-method explanation in Section 5. |
| 67–69 | `getBluePickups(...)` | Method-by-method explanation in Section 5. |
| 71–73 | `getTraps(...)` | Method-by-method explanation in Section 5. |
| 75–77 | `getBombPositions(...)` | Method-by-method explanation in Section 5. |

### `src/logic/CoinLogic.java` — 58 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 16–29 | `loadCoinsFromMap(...)` | Method-by-method explanation in Section 5. |
| 32–41 | `collectAt(...)` | Method-by-method explanation in Section 5. |
| 43–45 | `isComplete(...)` | Method-by-method explanation in Section 5. |
| 47–49 | `getCollected(...)` | Method-by-method explanation in Section 5. |
| 51–53 | `getTotal(...)` | Method-by-method explanation in Section 5. |
| 55–57 | `getPositions(...)` | Method-by-method explanation in Section 5. |

### `src/logic/LevelManager.java` — 36 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 14–19 | `resetToLevel1(...)` | Method-by-method explanation in Section 5. |
| 21–23 | `getCurrentLevel(...)` | Method-by-method explanation in Section 5. |
| 25–31 | `nextLevel(...)` | Method-by-method explanation in Section 5. |
| 33–35 | `getCurrentMap(...)` | Method-by-method explanation in Section 5. |

### `src/logic/MimicLogic.java` — 420 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 64–66 | `patrolRadiusFor(...)` | Method-by-method explanation in Section 5. |
| 73–110 | `buildPatrolRoute(...)` | Method-by-method explanation in Section 5. |
| 112–134 | `loadMimicsFromMap(...)` | Method-by-method explanation in Section 5. |
| 140–170 | `addDisguised(...)` | Method-by-method explanation in Section 5. |
| 173–181 | `canSee(...)` | Method-by-method explanation in Section 5. |
| 187–264 | `update(...)` | Method-by-method explanation in Section 5. |
| 267–276 | `ahead(...)` | Method-by-method explanation in Section 5. |
| 279–292 | `randomCell(...)` | Method-by-method explanation in Section 5. |
| 298–311 | `hearNoise(...)` | Method-by-method explanation in Section 5. |
| 313–320 | `checkCollision(...)` | Method-by-method explanation in Section 5. |
| 323–330 | `anyWithin(...)` | Method-by-method explanation in Section 5. |
| 332–340 | `resetToSpawn(...)` | Method-by-method explanation in Section 5. |
| 342–348 | `getPositions(...)` | Method-by-method explanation in Section 5. |
| 350–356 | `getDisguisedPositions(...)` | Method-by-method explanation in Section 5. |
| 358–363 | `isDisguisedAt(...)` | Method-by-method explanation in Section 5. |
| 369–379 | `revealAt(...)` | Method-by-method explanation in Section 5. |
| 382–392 | `stunNear(...)` | Method-by-method explanation in Section 5. |
| 398–407 | `triggerTrap(...)` | Method-by-method explanation in Section 5. |
| 409–415 | `getStunnedPositions(...)` | Method-by-method explanation in Section 5. |
| 417–419 | `isActive(...)` | Method-by-method explanation in Section 5. |

### `src/logic/Pathfinder.java` — 73 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 15–24 | `walkable(...)` | Method-by-method explanation in Section 5. |
| 27–51 | `distances(...)` | Method-by-method explanation in Section 5. |
| 53–55 | `distance(...)` | Method-by-method explanation in Section 5. |
| 58–72 | `nextStep(...)` | Method-by-method explanation in Section 5. |

### `src/logic/ScoreManager.java` — 58 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 13–16 | `resetScore(...)` | Method-by-method explanation in Section 5. |
| 18–20 | `addTreasurePoints(...)` | Method-by-method explanation in Section 5. |
| 22–24 | `addCoinPoints(...)` | Method-by-method explanation in Section 5. |
| 26–28 | `addLevelClearBonus(...)` | Method-by-method explanation in Section 5. |
| 31–36 | `loseLife(...)` | Method-by-method explanation in Section 5. |
| 39–42 | `eliminate(...)` | Method-by-method explanation in Section 5. |
| 45–49 | `restoreLife(...)` | Method-by-method explanation in Section 5. |
| 51–53 | `getLives(...)` | Method-by-method explanation in Section 5. |
| 55–57 | `getTotalScore(...)` | Method-by-method explanation in Section 5. |

### `src/logic/TreasureLogic.java` — 38 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 9–21 | `loadTreasureFromMap(...)` | Method-by-method explanation in Section 5. |
| 23–29 | `claimIfSteppedOn(...)` | Method-by-method explanation in Section 5. |
| 31–33 | `isClaimed(...)` | Method-by-method explanation in Section 5. |
| 35–37 | `getPosition(...)` | Method-by-method explanation in Section 5. |

### `src/logic/VanishLogic.java` — 86 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 26–36 | `load(...)` | Method-by-method explanation in Section 5. |
| 38–40 | `isEnabled(...)` | Method-by-method explanation in Section 5. |
| 43–48 | `onLeave(...)` | Method-by-method explanation in Section 5. |
| 51–53 | `onEnter(...)` | Method-by-method explanation in Section 5. |
| 55–57 | `isVanished(...)` | Method-by-method explanation in Section 5. |
| 59–61 | `isCrumbling(...)` | Method-by-method explanation in Section 5. |
| 64–85 | `update(...)` | Method-by-method explanation in Section 5. |

### `src/ui/GamePreview.java` — 65 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 1–65 | class body | Read imports, fields, constructor, and helper methods in order. |

### `src/ui/GameUI.java` — 80 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 25–51 | `showWindow(...)` | Method-by-method explanation in Section 5. |
| 53–71 | `showScreen(...)` | Method-by-method explanation in Section 5. |
| 73–79 | `showResults(...)` | Method-by-method explanation in Section 5. |

### `src/ui/MainMenu.java` — 61 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 58–60 | `refreshHighScore(...)` | Method-by-method explanation in Section 5. |

### `src/ui/ResultScreen.java` — 65 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 59–64 | `setResult(...)` | Method-by-method explanation in Section 5. |

### `src/ui/StartScreen.java` — 72 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 1–72 | class body | Read imports, fields, constructor, and helper methods in order. |

### `src/ui/UiStyle.java` — 26 lines

| Line(s) | Symbol / method | What to inspect |
|---:|---|---|
| 1–26 | class body | Read imports, fields, constructor, and helper methods in order. |

## 10. Java syntax quick reference

| Syntax | Meaning in this project |
|---|---|
| `package game;` | Declares the namespace/folder grouping for a class. |
| `import ...;` | Lets the source use a class by its short name. |
| `class Name { ... }` | Defines a type and the members inside it. |
| `private final List<Point> items = new ArrayList<>();` | Creates a list field that this class owns; the list can change, but the field cannot be reassigned. |
| `new Point(x, y)` | Creates a coordinate object. |
| `if (condition) { ... }` | Runs a block only if the condition is true. |
| `else` | Alternative block when the previous condition is false. |
| `for (int i = 0; i < n; i++)` | Repeats while `i` is below `n`, increasing `i` each time. |
| `for (Point p : points)` | Visits each point in a collection. |
| `return true;` | Stops the method and reports success. |
| `return false;` | Stops the method and reports that the operation did not happen. |
| `return null;` | Reports that no object/result could be supplied. Callers must check for null. |
| `&&` | AND: both conditions must be true. |
| `||` | OR: at least one condition must be true. |
| `!condition` | NOT: reverses a boolean condition. |
| `a ? b : c` | Ternary expression: use `b` if `a` is true, otherwise `c`. |
| `list.add(value)` | Adds an item to a list. |
| `list.remove(i)` | Removes the item at an index. |
| `Collections.unmodifiableList(list)` | Returns a view that callers cannot modify through that view. |
| `Math.abs(value)` | Absolute value; often used for grid distance. |
| `Math.max(a,b)` / `Math.min(a,b)` | Returns the larger/smaller value. |
| `try { ... } catch (...) { ... }` | Handles an error without letting it crash through the current code path. |
| `@Override` | Declares that a method replaces a method defined by a parent class/interface. |
| `e -> onTick()` | Lambda expression: a compact function passed to the Swing timer. |
| `repaint()` | Asks Swing to redraw the component; it does not directly draw pixels at that line. |
| `Timer(delay, listener)` | Calls the listener repeatedly on Swing's event-dispatch thread after the delay. |

## Final study advice

To learn the code, trace one feature from beginning to end instead of trying to memorize every class at once. Start with **coin collection**, then **black bomb damage**, then **opening a chest**, then **Mimic AI**, and finally **pause/music**. For each feature, explain four things: (1) where its state is stored, (2) which method changes that state, (3) which method checks the result, and (4) which method draws or displays it. That is the clearest way to explain how the code and the game work together.


# Appendix A — Literal Line-by-Line Java Source Reference

This appendix covers **every line** in every Java source file in the supplied archive. Each row shows the exact source line number and text, plus a plain-language note. Where a source line already has a useful inline comment, that comment is used as the explanation; for un-commented statements, the note explains the Java syntax or the operation. A line may be part of a multi-line expression, so read consecutive rows together. Blank lines and braces are included because they separate and define code blocks.

## `src/Main.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>import javax.swing.SwingUtilities; // Swing helper that runs code on the UI thread</code> | Inline comment: Swing helper that runs code on the UI thread |
| 2 | <code>import ui.GameUI; // window controller from the ui package</code> | Inline comment: window controller from the ui package |
| 3 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 4 | <code>public class Main { // program entry class</code> | Inline comment: program entry class |
| 5 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 6 | <code>    public static void main(String[] args) { // the JVM starts here</code> | Inline comment: the JVM starts here |
| 7 | <code>        SwingUtilities.invokeLater(() -&gt; { // build the UI on the Swing event thread</code> | Inline comment: build the UI on the Swing event thread |
| 8 | <code>            GameUI ui = new GameUI(); // create the window controller</code> | Inline comment: create the window controller |
| 9 | <code>            ui.showWindow(); // build and show the window</code> | Inline comment: build and show the window |
| 10 | <code>        }); // end of the UI-thread task</code> | Inline comment: end of the UI-thread task |
| 11 | <code>    } // end of main</code> | Inline comment: end of main |
| 12 | <code>} // end of Main</code> | Inline comment: end of Main |


## `src/audio/SoundManager.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package audio; // package for sound</code> | Inline comment: package for sound |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import data.AssetPaths; // finds the assets folder</code> | Inline comment: finds the assets folder |
| 4 | <code>import java.io.File; // file path</code> | Inline comment: file path |
| 5 | <code>import java.io.IOException; // thrown on read problems</code> | Inline comment: thrown on read problems |
| 6 | <code>import java.util.Map; // map interface</code> | Inline comment: map interface |
| 7 | <code>import java.util.concurrent.ConcurrentHashMap; // map safe to use from two threads</code> | Inline comment: map safe to use from two threads |
| 8 | <code>import javax.sound.sampled.AudioFormat; // describes a sound format</code> | Inline comment: describes a sound format |
| 9 | <code>import javax.sound.sampled.AudioInputStream; // stream of sound data</code> | Inline comment: stream of sound data |
| 10 | <code>import javax.sound.sampled.AudioSystem; // entry point of Java Sound</code> | Inline comment: entry point of Java Sound |
| 11 | <code>import javax.sound.sampled.Clip; // sound held in memory that can be replayed</code> | Inline comment: sound held in memory that can be replayed |
| 12 | <code>import javax.sound.sampled.DataLine; // describes a line type</code> | Inline comment: describes a line type |
| 13 | <code>import javax.sound.sampled.FloatControl; // volume control</code> | Inline comment: volume control |
| 14 | <code>import javax.sound.sampled.LineUnavailableException; // thrown when no audio output exists</code> | Inline comment: thrown when no audio output exists |
| 15 | <code>import javax.sound.sampled.UnsupportedAudioFileException; // thrown for unknown file types</code> | Inline comment: thrown for unknown file types |
| 16 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 17 | <code>/**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 18 | <code> * Plays sound effects and the looping background music.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 19 | <code> * Files load on a background thread, so the window never freezes.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 20 | <code> * If the computer has no audio output the game keeps running silently.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 21 | <code> */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 22 | <code>public final class SoundManager { // sound controller</code> | Inline comment: sound controller |
| 23 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 24 | <code>    // Sound effect names; each one is the file assets/sfx/&lt;name&gt;.wav</code> | Inline comment: Sound effect names; each one is the file assets/sfx/&lt;name&gt;.wav |
| 25 | <code>    private static final String[] SFX_NAMES = {"step", "mimic_step", "bomb", "mimic_catch", "chest", "coin", "tick"}; // all effects</code> | Inline comment: all effects |
| 26 | <code>    private static final String MUSIC_FILE = "music/alex-morgan-video-game-pixel-chiptune-music-583271.wav"; // background music</code> | Inline comment: background music |
| 27 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 28 | <code>    private final Map&lt;String, Clip&gt; clips = new ConcurrentHashMap&lt;&gt;(); // effect name -&gt; loaded clip</code> | Inline comment: effect name -&gt; loaded clip |
| 29 | <code>    private final Map&lt;String, Integer&gt; playCounts = new ConcurrentHashMap&lt;&gt;(); // effect name -&gt; times requested (for testing)</code> | Inline comment: effect name -&gt; times requested (for testing) |
| 30 | <code>    private volatile Clip music; // background music clip (null until loaded)</code> | Inline comment: background music clip (null until loaded) |
| 31 | <code>    private volatile boolean musicWanted; // true after startMusic() was called</code> | Inline comment: true after startMusic() was called |
| 32 | <code>    private volatile boolean muted; // true when the player pressed M</code> | Inline comment: true when the player pressed M |
| 33 | <code>    private volatile boolean musicPaused; // true while the game is paused (music waits, the mute setting is untouched)</code> | Inline comment: true while the game is paused (music waits, the mute setting is untouched) |
| 34 | <code>    private volatile boolean audioAvailable; // true if this computer has an audio output</code> | Inline comment: true if this computer has an audio output |
| 35 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 36 | <code>    /** @param loadFiles false skips loading (used by tests that only need the counters) */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 37 | <code>    public SoundManager(boolean loadFiles) { // create the manager</code> | Inline comment: create the manager |
| 38 | <code>        if (loadFiles) { // loading wanted</code> | Inline comment: loading wanted |
| 39 | <code>            Thread loader = new Thread(this::loadAll, "audio-loader"); // background thread</code> | Inline comment: background thread |
| 40 | <code>            loader.setDaemon(true); // do not keep the program alive for it</code> | Inline comment: do not keep the program alive for it |
| 41 | <code>            loader.start(); // load files without blocking the window</code> | Inline comment: load files without blocking the window |
| 42 | <code>        } // end of loadFiles check</code> | Inline comment: end of loadFiles check |
| 43 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 44 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 45 | <code>    private void loadAll() { // runs on the loader thread</code> | Inline comment: runs on the loader thread |
| 46 | <code>        try { // any audio problem must not crash the game</code> | Inline comment: any audio problem must not crash the game |
| 47 | <code>            AudioFormat probe = new AudioFormat(44100f, 16, 1, true, false); // typical effect format</code> | Inline comment: typical effect format |
| 48 | <code>            if (!AudioSystem.isLineSupported(new DataLine.Info(Clip.class, probe))) { // no audio output device</code> | Inline comment: no audio output device |
| 49 | <code>                audioAvailable = false; // stay silent</code> | Inline comment: stay silent |
| 50 | <code>                return; // nothing more to do</code> | Inline comment: nothing more to do |
| 51 | <code>            } // end of device check</code> | Inline comment: end of device check |
| 52 | <code>            audioAvailable = true; // sound can work</code> | Inline comment: sound can work |
| 53 | <code>            for (String name : SFX_NAMES) { // each effect</code> | Inline comment: each effect |
| 54 | <code>                Clip clip = openClip(AssetPaths.file("sfx/" + name + ".wav")); // load it</code> | Inline comment: load it |
| 55 | <code>                if (clip != null) { // loaded</code> | Inline comment: loaded |
| 56 | <code>                    clips.put(name, clip); // keep it for later</code> | Inline comment: keep it for later |
| 57 | <code>                } // end of loaded check</code> | Inline comment: end of loaded check |
| 58 | <code>            } // end of effect loop</code> | Inline comment: end of effect loop |
| 59 | <code>            Clip bgm = openClip(AssetPaths.file(MUSIC_FILE)); // load the music (large file)</code> | Inline comment: load the music (large file) |
| 60 | <code>            if (bgm != null) { // loaded</code> | Inline comment: loaded |
| 61 | <code>                setGain(bgm, -6f); // a bit quieter than the effects</code> | Inline comment: a bit quieter than the effects |
| 62 | <code>                music = bgm; // publish it</code> | Inline comment: publish it |
| 63 | <code>                applyMusicState(); // start it if the game already asked for music</code> | Inline comment: start it if the game already asked for music |
| 64 | <code>            } // end of music loaded check</code> | Inline comment: end of music loaded check |
| 65 | <code>        } catch (RuntimeException e) { // unexpected audio failure</code> | Inline comment: unexpected audio failure |
| 66 | <code>            audioAvailable = false; // give up on sound</code> | Inline comment: give up on sound |
| 67 | <code>        } // end of try/catch</code> | Inline comment: end of try/catch |
| 68 | <code>    } // end of loadAll</code> | Inline comment: end of loadAll |
| 69 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 70 | <code>    private Clip openClip(File file) { // load one WAV file into a Clip</code> | Inline comment: load one WAV file into a Clip |
| 71 | <code>        if (!file.isFile()) return null; // missing file: skip</code> | Inline comment: missing file: skip |
| 72 | <code>        try (AudioInputStream in = AudioSystem.getAudioInputStream(file)) { // open the WAV</code> | Inline comment: open the WAV |
| 73 | <code>            Clip clip = AudioSystem.getClip(); // create an empty clip</code> | Inline comment: create an empty clip |
| 74 | <code>            clip.open(in); // copy the sound data into it</code> | Inline comment: copy the sound data into it |
| 75 | <code>            return clip; // ready to play</code> | Inline comment: ready to play |
| 76 | <code>        } catch (UnsupportedAudioFileException &#124; IOException &#124; LineUnavailableException &#124; IllegalArgumentException e) { // cannot load</code> | Inline comment: cannot load |
| 77 | <code>            return null; // skip this sound</code> | Inline comment: skip this sound |
| 78 | <code>        } // end of try/catch</code> | Inline comment: end of try/catch |
| 79 | <code>    } // end of openClip</code> | Inline comment: end of openClip |
| 80 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 81 | <code>    private void setGain(Clip clip, float decibels) { // change a clip's volume</code> | Inline comment: change a clip's volume |
| 82 | <code>        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) { // volume control exists</code> | Inline comment: volume control exists |
| 83 | <code>            FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN); // get it</code> | Inline comment: get it |
| 84 | <code>            float safe = Math.max(control.getMinimum(), Math.min(control.getMaximum(), decibels)); // stay inside the allowed range</code> | Inline comment: stay inside the allowed range |
| 85 | <code>            control.setValue(safe); // apply</code> | Inline comment: apply |
| 86 | <code>        } // end of control check</code> | Inline comment: end of control check |
| 87 | <code>    } // end of setGain</code> | Inline comment: end of setGain |
| 88 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 89 | <code>    /** Plays one effect once. Returns immediately (Clip.start does not block). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 90 | <code>    public void play(String name) { // request an effect</code> | Inline comment: request an effect |
| 91 | <code>        playCounts.merge(name, 1, Integer::sum); // count the request</code> | Inline comment: count the request |
| 92 | <code>        Clip clip = clips.get(name); // find the clip</code> | Inline comment: find the clip |
| 93 | <code>        if (clip == null &#124;&#124; muted) return; // not loaded or muted: stay silent</code> | Inline comment: not loaded or muted: stay silent |
| 94 | <code>        clip.stop(); // stop it if it is still playing from last time</code> | Inline comment: stop it if it is still playing from last time |
| 95 | <code>        clip.setFramePosition(0); // rewind to the start</code> | Inline comment: rewind to the start |
| 96 | <code>        clip.start(); // play asynchronously</code> | Inline comment: play asynchronously |
| 97 | <code>    } // end of play</code> | Inline comment: end of play |
| 98 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 99 | <code>    /** How many times an effect was requested (used by the tests). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 100 | <code>    public int getPlayCount(String name) { // read a counter</code> | Inline comment: read a counter |
| 101 | <code>        return playCounts.getOrDefault(name, 0); // 0 if never played</code> | Inline comment: 0 if never played |
| 102 | <code>    } // end of getPlayCount</code> | Inline comment: end of getPlayCount |
| 103 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 104 | <code>    /** Starts the looping background music (safe to call before loading has finished). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 105 | <code>    public void startMusic() { // begin music</code> | Inline comment: begin music |
| 106 | <code>        musicWanted = true; // remember the request</code> | Inline comment: remember the request |
| 107 | <code>        applyMusicState(); // start it if it is already loaded</code> | Inline comment: start it if it is already loaded |
| 108 | <code>    } // end of startMusic</code> | Inline comment: end of startMusic |
| 109 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 110 | <code>    /** Pauses the music together with the game (does not change the mute setting). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 111 | <code>    public void pauseMusic() { // called when the pause menu opens</code> | Inline comment: called when the pause menu opens |
| 112 | <code>        musicPaused = true; // remember</code> | Inline comment: remember |
| 113 | <code>        applyMusicState(); // stop the clip (its position is kept)</code> | Inline comment: stop the clip (its position is kept) |
| 114 | <code>    } // end of pauseMusic</code> | Inline comment: end of pauseMusic |
| 115 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 116 | <code>    /** Continues the music after a pause (stays silent if the player muted it). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 117 | <code>    public void resumeMusic() { // called when the game resumes or the main menu opens</code> | Inline comment: called when the game resumes or the main menu opens |
| 118 | <code>        musicPaused = false; // remember</code> | Inline comment: remember |
| 119 | <code>        applyMusicState(); // start again if wanted and not muted (the isRunning check prevents a second playback)</code> | Inline comment: start again if wanted and not muted (the isRunning check prevents a second playback) |
| 120 | <code>    } // end of resumeMusic</code> | Inline comment: end of resumeMusic |
| 121 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 122 | <code>    /** Toggles all sound on/off. Returns true if sound is now muted. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 123 | <code>    public boolean toggleMute() { // M key</code> | Inline comment: M key |
| 124 | <code>        muted = !muted; // flip the flag</code> | Inline comment: flip the flag |
| 125 | <code>        applyMusicState(); // start or stop the music to match</code> | Inline comment: start or stop the music to match |
| 126 | <code>        return muted; // report the new state</code> | Inline comment: report the new state |
| 127 | <code>    } // end of toggleMute</code> | Inline comment: end of toggleMute |
| 128 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 129 | <code>    private void applyMusicState() { // make the music match musicWanted and muted</code> | Inline comment: make the music match musicWanted and muted |
| 130 | <code>        Clip bgm = music; // local copy</code> | Inline comment: local copy |
| 131 | <code>        if (bgm == null) return; // not loaded yet</code> | Inline comment: not loaded yet |
| 132 | <code>        if (musicWanted &amp;&amp; !muted &amp;&amp; !musicPaused) { // should be playing</code> | Inline comment: should be playing |
| 133 | <code>            if (!bgm.isRunning()) { // not playing yet</code> | Inline comment: not playing yet |
| 134 | <code>                bgm.loop(Clip.LOOP_CONTINUOUSLY); // loop forever</code> | Inline comment: loop forever |
| 135 | <code>            } // end of running check</code> | Inline comment: end of running check |
| 136 | <code>        } else { // should be silent</code> | Inline comment: should be silent |
| 137 | <code>            bgm.stop(); // stop playing</code> | Inline comment: stop playing |
| 138 | <code>        } // end of state check</code> | Inline comment: end of state check |
| 139 | <code>    } // end of applyMusicState</code> | Inline comment: end of applyMusicState |
| 140 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 141 | <code>    public boolean isAudioAvailable() { // does this computer have audio output</code> | Inline comment: does this computer have audio output |
| 142 | <code>        return audioAvailable; // flag set by the loader thread</code> | Inline comment: flag set by the loader thread |
| 143 | <code>    } // end of isAudioAvailable</code> | Inline comment: end of isAudioAvailable |
| 144 | <code>} // end of SoundManager</code> | Inline comment: end of SoundManager |


## `src/data/AssetPaths.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package data; // package for saving, loading and locating files</code> | Inline comment: package for saving, loading and locating files |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import java.io.File; // represents a file or folder path</code> | Inline comment: represents a file or folder path |
| 4 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 5 | <code>/** Finds the "assets" folder (sprites, sound effects, music) no matter where the game was started from. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 6 | <code>public final class AssetPaths { // static helper, no instances</code> | Inline comment: static helper, no instances |
| 7 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 8 | <code>    // Folders tried in order: normal run from the project folder, run from src/, run from the parent folder.</code> | Inline comment: Folders tried in order: normal run from the project folder, run from src/, run from the parent folder. |
| 9 | <code>    private static final String[] CANDIDATES = {"assets", "../assets", "TreasureMaze2/assets"}; // possible locations</code> | Inline comment: possible locations |
| 10 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 11 | <code>    private AssetPaths() { // private: never instantiated</code> | Inline comment: private: never instantiated |
| 12 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 13 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 14 | <code>    /** Returns the assets folder (the first candidate that exists, otherwise plain "assets"). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 15 | <code>    public static File dir() { // locate the folder</code> | Inline comment: locate the folder |
| 16 | <code>        for (String candidate : CANDIDATES) { // try each location</code> | Inline comment: try each location |
| 17 | <code>            File folder = new File(candidate); // build the path</code> | Inline comment: build the path |
| 18 | <code>            if (folder.isDirectory()) { // it exists</code> | Inline comment: it exists |
| 19 | <code>                return folder; // use it</code> | Inline comment: use it |
| 20 | <code>            } // end of exists check</code> | Inline comment: end of exists check |
| 21 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 22 | <code>        return new File("assets"); // nothing found: callers will simply find no files</code> | Inline comment: nothing found: callers will simply find no files |
| 23 | <code>    } // end of dir</code> | Inline comment: end of dir |
| 24 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 25 | <code>    /** Returns a file inside the assets folder, for example file("sfx/step.wav"). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 26 | <code>    public static File file(String relativePath) { // build a path inside assets</code> | Inline comment: build a path inside assets |
| 27 | <code>        return new File(dir(), relativePath); // assets folder + relative path</code> | Inline comment: assets folder + relative path |
| 28 | <code>    } // end of file</code> | Inline comment: end of file |
| 29 | <code>} // end of AssetPaths</code> | Inline comment: end of AssetPaths |


## `src/data/SaveManager.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package data; // package for saving and loading data</code> | Inline comment: package for saving and loading data |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import java.io.IOException; // thrown on file read/write failure</code> | Inline comment: thrown on file read/write failure |
| 4 | <code>import java.nio.charset.StandardCharsets; // fixed character set for the file</code> | Inline comment: fixed character set for the file |
| 5 | <code>import java.nio.file.Files; // file read/write helpers</code> | Inline comment: file read/write helpers |
| 6 | <code>import java.nio.file.Path; // represents a file path</code> | Inline comment: represents a file path |
| 7 | <code>import java.nio.file.Paths; // builds a Path from a string</code> | Inline comment: builds a Path from a string |
| 8 | <code>import java.util.logging.Level; // log severity levels</code> | Inline comment: log severity levels |
| 9 | <code>import java.util.logging.Logger; // logging facility</code> | Inline comment: logging facility |
| 10 | <code>import java.util.regex.Matcher; // runs a regex on text</code> | Inline comment: runs a regex on text |
| 11 | <code>import java.util.regex.Pattern; // compiled regex</code> | Inline comment: compiled regex |
| 12 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 13 | <code>public final class SaveManager { // static helper for the high score file</code> | Inline comment: static helper for the high score file |
| 14 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 15 | <code>    private static final Logger LOGGER = Logger.getLogger(SaveManager.class.getName()); // logger for warnings</code> | Inline comment: logger for warnings |
| 16 | <code>    private static final String FILE_NAME = "highscore.txt"; // file kept in the launch folder</code> | Inline comment: file kept in the launch folder |
| 17 | <code>    private static final Pattern LEADING_DIGITS = Pattern.compile("^\\d+"); // matches digits at the start of the text</code> | Inline comment: matches digits at the start of the text |
| 18 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 19 | <code>    private SaveManager() { // private: this class is never instantiated</code> | Inline comment: private: this class is never instantiated |
| 20 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 21 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 22 | <code>    /** Reads the saved high score. Returns 0 if the file is missing or unreadable. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 23 | <code>    public static int loadHighScore() { // returns the stored high score</code> | Inline comment: returns the stored high score |
| 24 | <code>        Path path = Paths.get(FILE_NAME); // path of the score file</code> | Inline comment: path of the score file |
| 25 | <code>        if (!Files.exists(path)) { // no file yet</code> | Inline comment: no file yet |
| 26 | <code>            return 0; // so the high score is 0</code> | Inline comment: so the high score is 0 |
| 27 | <code>        } // end of missing-file check</code> | Inline comment: end of missing-file check |
| 28 | <code>        try { // file access can fail</code> | Inline comment: file access can fail |
| 29 | <code>            String text = Files.readString(path, StandardCharsets.ISO_8859_1).trim(); // read whole file, strip whitespace</code> | Inline comment: read whole file, strip whitespace |
| 30 | <code>            Matcher m = LEADING_DIGITS.matcher(text); // look for leading digits</code> | Inline comment: look for leading digits |
| 31 | <code>            if (m.find()) { // digits found</code> | Inline comment: digits found |
| 32 | <code>                return Integer.parseInt(m.group()); // convert them to an int</code> | Inline comment: convert them to an int |
| 33 | <code>            } // end of digits check</code> | Inline comment: end of digits check |
| 34 | <code>        } catch (IOException &#124; NumberFormatException e) { // unreadable file or number too large</code> | Inline comment: unreadable file or number too large |
| 35 | <code>            LOGGER.log(Level.WARNING, "Unable to load high score from file: " + FILE_NAME, e); // log the problem</code> | Inline comment: log the problem |
| 36 | <code>        } // end of try/catch</code> | Inline comment: end of try/catch |
| 37 | <code>        return 0; // fall back to 0</code> | Inline comment: fall back to 0 |
| 38 | <code>    } // end of loadHighScore</code> | Inline comment: end of loadHighScore |
| 39 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 40 | <code>    /** Saves the score only if it beats the stored high score. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 41 | <code>    public static void saveHighScore(int score) { // stores score if it is a new record</code> | Inline comment: stores score if it is a new record |
| 42 | <code>        if (score &lt;= loadHighScore()) { // not a new record</code> | Inline comment: not a new record |
| 43 | <code>            return; // nothing to save</code> | Inline comment: nothing to save |
| 44 | <code>        } // end of record check</code> | Inline comment: end of record check |
| 45 | <code>        try { // file access can fail</code> | Inline comment: file access can fail |
| 46 | <code>            Files.writeString(Paths.get(FILE_NAME), score + System.lineSeparator(), StandardCharsets.ISO_8859_1); // overwrite file with the score</code> | Inline comment: overwrite file with the score |
| 47 | <code>        } catch (IOException e) { // write failed</code> | Inline comment: write failed |
| 48 | <code>            LOGGER.log(Level.WARNING, "Unable to save high score to file: " + FILE_NAME, e); // log the problem</code> | Inline comment: log the problem |
| 49 | <code>        } // end of try/catch</code> | Inline comment: end of try/catch |
| 50 | <code>    } // end of saveHighScore</code> | Inline comment: end of saveHighScore |
| 51 | <code>} // end of SaveManager</code> | Inline comment: end of SaveManager |


## `src/game/GameMap.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package game; // package for core game objects</code> | Inline comment: package for core game objects |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import java.awt.Point; // integer x/y coordinate</code> | Inline comment: integer x/y coordinate |
| 4 | <code>import java.util.ArrayDeque; // stack used by the maze carver</code> | Inline comment: stack used by the maze carver |
| 5 | <code>import java.util.ArrayList; // growable list</code> | Inline comment: growable list |
| 6 | <code>import java.util.Arrays; // array fill helper</code> | Inline comment: array fill helper |
| 7 | <code>import java.util.Collections; // shuffle helper</code> | Inline comment: shuffle helper |
| 8 | <code>import java.util.List; // list interface</code> | Inline comment: list interface |
| 9 | <code>import java.util.Random; // random numbers</code> | Inline comment: random numbers |
| 10 | <code>import logic.MimicLogic; // patrol route builder</code> | Inline comment: patrol route builder |
| 11 | <code>import logic.Pathfinder; // BFS helpers</code> | Inline comment: BFS helpers |
| 12 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 13 | <code>/**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 14 | <code> * Generates a NEW random map for each level every time a game starts, then VALIDATES it:</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 15 | <code> * the start, treasure, exit and every coin must be reachable without stepping on a bomb,</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 16 | <code> * and mimic patrol areas must keep away from the treasure, start and exit.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 17 | <code> * One small generator replaces hand-written maps, so there is no long list of fixed levels.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 18 | <code> *</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 19 | <code> * Symbols: # wall, . floor, P start, T treasure chest, E exit, B bomb, M mimic home, C coin.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 20 | <code> */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 21 | <code>public final class GameMap { // static level factory</code> | Inline comment: static level factory |
| 22 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 23 | <code>    public static final int LEVEL_COUNT = 3; // number of levels</code> | Inline comment: number of levels |
| 24 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 25 | <code>    // width, height, shortcut openings (more = easier), bombs, mimics, coins</code> | Inline comment: width, height, shortcut openings (more = easier), bombs, mimics, coins |
| 26 | <code>    private static final int[][] SPEC = { // one row of settings per level</code> | Inline comment: one row of settings per level |
| 27 | <code>        {15, 15, 12, 25, 1, 3}, // level 1: small: 25 bomb pickups (20 black + 5 blue), 1 regular mimic, 3 coins</code> | Inline comment: level 1: small: 25 bomb pickups (20 black + 5 blue), 1 regular mimic, 3 coins |
| 28 | <code>        {31, 25, 14, 25, 1, 4}, // level 2: 25 bomb pickups, 1 regular mimic, 4 coins</code> | Inline comment: level 2: 25 bomb pickups, 1 regular mimic, 4 coins |
| 29 | <code>        {35, 31, 18, 25, 2, 5} // level 3: 25 bomb pickups, 2 regular mimics, 5 coins</code> | Inline comment: level 3: 25 bomb pickups, 2 regular mimics, 5 coins |
| 30 | <code>    }; // end of SPEC (every level also gets 1 disguised mimic chest, added by MimicLogic when the level loads)</code> | Inline comment: end of SPEC (every level also gets 1 disguised mimic chest, added by MimicLogic when the level loads) |
| 31 | <code>    private static final int BLUE_BOMBS = 5; // how many of the 25 pickups are blue Mimic-trap bombs ('U' on the map)</code> | Inline comment: how many of the 25 pickups are blue Mimic-trap bombs ('U' on the map) |
| 32 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 33 | <code>    private static final int MAX_ATTEMPTS = 300; // tries before giving up on one map</code> | Inline comment: tries before giving up on one map |
| 34 | <code>    private static final int MIN_MIMIC_HOME_TO_TREASURE = 10; // mimic home must be this many steps from the chest</code> | Inline comment: mimic home must be this many steps from the chest |
| 35 | <code>    private static final int MIN_PATROL_TO_TREASURE = 6; // every patrol cell must be this many steps from the chest</code> | Inline comment: every patrol cell must be this many steps from the chest |
| 36 | <code>    private static final int MIN_PATROL_TO_START = 8; // ...and from the start, so the player is never ambushed at once</code> | Inline comment: ...and from the start, so the player is never ambushed at once |
| 37 | <code>    private static final int MIN_PATROL_TO_EXIT = 3; // ...and from the exit</code> | Inline comment: ...and from the exit |
| 38 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 39 | <code>    private static final Random RND = new Random(); // not seeded: a different map each run</code> | Inline comment: not seeded: a different map each run |
| 40 | <code>    private static final String[] LAST = new String[LEVEL_COUNT]; // previous layout per level (to avoid repeats)</code> | Inline comment: previous layout per level (to avoid repeats) |
| 41 | <code>    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}; // right, left, down, up</code> | Inline comment: right, left, down, up |
| 42 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 43 | <code>    private GameMap() { // private: never instantiated</code> | Inline comment: private: never instantiated |
| 44 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 45 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 46 | <code>    /** Generates a valid map for a level (1-3) that is different from the previous one of that level. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 47 | <code>    public static synchronized String[] generate(int level) { // public entry point</code> | Inline comment: public entry point |
| 48 | <code>        int idx = Math.max(1, Math.min(LEVEL_COUNT, level)) - 1; // clamp to 1..3, make 0-based</code> | Inline comment: clamp to 1..3, make 0-based |
| 49 | <code>        String[] result = null; // the map to return</code> | Inline comment: the map to return |
| 50 | <code>        for (int round = 0; round &lt; 20; round++) { // repeat if we happen to produce the same layout again</code> | Inline comment: repeat if we happen to produce the same layout again |
| 51 | <code>            result = buildValid(idx); // a validated map</code> | Inline comment: a validated map |
| 52 | <code>            String key = String.join("\n", result); // layout as one string for comparison</code> | Inline comment: layout as one string for comparison |
| 53 | <code>            if (!key.equals(LAST[idx])) { // different from last time</code> | Inline comment: different from last time |
| 54 | <code>                LAST[idx] = key; // remember it</code> | Inline comment: remember it |
| 55 | <code>                return result; // done</code> | Inline comment: done |
| 56 | <code>            } // end of repeat check</code> | Inline comment: end of repeat check |
| 57 | <code>        } // end of rounds</code> | Inline comment: end of rounds |
| 58 | <code>        return result; // practically unreachable: 20 identical random maps in a row</code> | Inline comment: practically unreachable: 20 identical random maps in a row |
| 59 | <code>    } // end of generate</code> | Inline comment: end of generate |
| 60 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 61 | <code>    /** Position of the first cell holding {@code symbol}, or null. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 62 | <code>    public static Point find(String[] map, char symbol) { // locate a symbol on the map</code> | Inline comment: locate a symbol on the map |
| 63 | <code>        for (int r = 0; r &lt; map.length; r++) { // for each row</code> | Inline comment: for each row |
| 64 | <code>            int c = map[r].indexOf(symbol); // column of the symbol in this row</code> | Inline comment: column of the symbol in this row |
| 65 | <code>            if (c &gt;= 0) { // found</code> | Inline comment: found |
| 66 | <code>                return new Point(c, r); // return its position</code> | Inline comment: return its position |
| 67 | <code>            } // end of found check</code> | Inline comment: end of found check |
| 68 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 69 | <code>        return null; // symbol not on the map</code> | Inline comment: symbol not on the map |
| 70 | <code>    } // end of find</code> | Inline comment: end of find |
| 71 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 72 | <code>    private static String[] buildValid(int idx) { // keep trying until a map passes validation</code> | Inline comment: keep trying until a map passes validation |
| 73 | <code>        for (int attempt = 0; attempt &lt; MAX_ATTEMPTS; attempt++) { // limited number of tries</code> | Inline comment: limited number of tries |
| 74 | <code>            String[] map = tryBuild(SPEC[idx], idx + 1); // build and validate</code> | Inline comment: build and validate |
| 75 | <code>            if (map != null) { // passed</code> | Inline comment: passed |
| 76 | <code>                return map; // use it</code> | Inline comment: use it |
| 77 | <code>            } // end of pass check</code> | Inline comment: end of pass check |
| 78 | <code>        } // end of attempts</code> | Inline comment: end of attempts |
| 79 | <code>        throw new IllegalStateException("Could not generate a valid map for level " + (idx + 1)); // should never happen</code> | Inline comment: should never happen |
| 80 | <code>    } // end of buildValid</code> | Inline comment: end of buildValid |
| 81 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 82 | <code>    /** Builds one candidate map. Returns null if it fails validation (the caller then tries again). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 83 | <code>    private static String[] tryBuild(int[] spec, int level) { // generate and validate</code> | Inline comment: generate and validate |
| 84 | <code>        int w = spec[0]; // map width</code> | Inline comment: map width |
| 85 | <code>        int h = spec[1]; // map height</code> | Inline comment: map height |
| 86 | <code>        char[][] g = new char[h][w]; // editable grid</code> | Inline comment: editable grid |
| 87 | <code>        for (char[] row : g) { // for each row</code> | Inline comment: for each row |
| 88 | <code>            Arrays.fill(row, '#'); // start with solid wall</code> | Inline comment: start with solid wall |
| 89 | <code>        } // end of fill loop</code> | Inline comment: end of fill loop |
| 90 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 91 | <code>        Point[] corners = {new Point(1, 1), new Point(w - 2, 1), new Point(1, h - 2), new Point(w - 2, h - 2)}; // possible starts</code> | Inline comment: possible starts |
| 92 | <code>        Point start = corners[RND.nextInt(corners.length)]; // random corner start</code> | Inline comment: random corner start |
| 93 | <code>        carveMaze(g, w, h, start); // dig a perfect maze (random walls)</code> | Inline comment: dig a perfect maze (random walls) |
| 94 | <code>        openShortcuts(g, w, h, spec[2]); // knock out some walls to create loops</code> | Inline comment: knock out some walls to create loops |
| 95 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 96 | <code>        String[] base = toMap(g); // snapshot for BFS</code> | Inline comment: snapshot for BFS |
| 97 | <code>        int[][] fromStart = Pathfinder.distances(base, start, false); // distance from start to each cell</code> | Inline comment: distance from start to each cell |
| 98 | <code>        int maxD = maxOf(fromStart); // farthest cell from the start</code> | Inline comment: farthest cell from the start |
| 99 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 100 | <code>        // Treasure: random cell in a distance band. Level 1 is closer to the start (calmer).</code> | Inline comment: Treasure: random cell in a distance band. Level 1 is closer to the start (calmer). |
| 101 | <code>        double lo = level == 1 ? 0.35 : 0.45; // band start (fraction of the longest distance)</code> | Inline comment: band start (fraction of the longest distance) |
| 102 | <code>        double hi = level == 1 ? 0.60 : 0.75; // band end</code> | Inline comment: band end |
| 103 | <code>        Point treasure = pickInBand(fromStart, (int) (maxD * lo), (int) (maxD * hi), Collections.singletonList(start)); // chest cell</code> | Inline comment: chest cell |
| 104 | <code>        if (treasure == null) return null; // no cell in the band</code> | Inline comment: no cell in the band |
| 105 | <code>        int[][] fromTreasure = Pathfinder.distances(base, treasure, false); // distance from the chest to each cell</code> | Inline comment: distance from the chest to each cell |
| 106 | <code>        int maxT = maxOf(fromTreasure); // farthest cell from the chest</code> | Inline comment: farthest cell from the chest |
| 107 | <code>        List&lt;Point&gt; taken = new ArrayList&lt;&gt;(); // cells already used by objects</code> | Inline comment: cells already used by objects |
| 108 | <code>        taken.add(start); // start is used</code> | Inline comment: start is used |
| 109 | <code>        taken.add(treasure); // chest is used</code> | Inline comment: chest is used |
| 110 | <code>        Point exit = pickInBand(fromTreasure, (int) (maxT * 0.7), maxT, taken); // exit: far from the chest</code> | Inline comment: exit: far from the chest |
| 111 | <code>        if (exit == null) return null; // none found</code> | Inline comment: none found |
| 112 | <code>        taken.add(exit); // exit is used</code> | Inline comment: exit is used |
| 113 | <code>        int[][] fromExit = Pathfinder.distances(base, exit, false); // distance from the exit to each cell</code> | Inline comment: distance from the exit to each cell |
| 114 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 115 | <code>        // Coins (the alternative way to unlock the exit): dead ends and far corners, spread apart.</code> | Inline comment: Coins (the alternative way to unlock the exit): dead ends and far corners, spread apart. |
| 116 | <code>        List&lt;Point&gt; coins = pickCoins(g, fromStart, spec[5], taken); // coin cells</code> | Inline comment: coin cells |
| 117 | <code>        if (coins.size() &lt; spec[5]) return null; // not enough good spots</code> | Inline comment: not enough good spots |
| 118 | <code>        taken.addAll(coins); // coins are used</code> | Inline comment: coins are used |
| 119 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 120 | <code>        // Mimics: patrol areas must be valid and away from chest, start and exit.</code> | Inline comment: Mimics: patrol areas must be valid and away from chest, start and exit. |
| 121 | <code>        List&lt;Point&gt; homes = new ArrayList&lt;&gt;(); // chosen mimic homes</code> | Inline comment: chosen mimic homes |
| 122 | <code>        if (spec[4] &gt; 0) { // this level has mimics</code> | Inline comment: this level has mimics |
| 123 | <code>            int radius = MimicLogic.patrolRadiusFor(level); // patrol area size</code> | Inline comment: patrol area size |
| 124 | <code>            List&lt;Point&gt; candidates = new ArrayList&lt;&gt;(); // possible homes</code> | Inline comment: possible homes |
| 125 | <code>            for (int y = 1; y &lt; h - 1; y++) { // inner rows</code> | Inline comment: inner rows |
| 126 | <code>                for (int x = 1; x &lt; w - 1; x++) { // inner columns</code> | Inline comment: inner columns |
| 127 | <code>                    if (g[y][x] == '.' &amp;&amp; fromStart[y][x] &gt;= maxD * 0.4 // far from the start</code> | Inline comment: far from the start |
| 128 | <code>                        &amp;&amp; fromTreasure[y][x] &gt;= MIN_MIMIC_HOME_TO_TREASURE // far from the chest</code> | Inline comment: far from the chest |
| 129 | <code>                        &amp;&amp; fromExit[y][x] &gt;= 6) { // far from the exit</code> | Inline comment: far from the exit |
| 130 | <code>                        candidates.add(new Point(x, y)); // keep it</code> | Inline comment: keep it |
| 131 | <code>                    } // end of filter</code> | Inline comment: end of filter |
| 132 | <code>                } // end of column loop</code> | Inline comment: end of column loop |
| 133 | <code>            } // end of row loop</code> | Inline comment: end of row loop |
| 134 | <code>            Collections.shuffle(candidates, RND); // random order</code> | Inline comment: random order |
| 135 | <code>            for (Point cand : candidates) { // test candidates</code> | Inline comment: test candidates |
| 136 | <code>                if (homes.size() &gt;= spec[4]) break; // enough mimics</code> | Inline comment: enough mimics |
| 137 | <code>                if (!farFromOtherHomes(cand, homes)) continue; // keep mimics apart</code> | Inline comment: keep mimics apart |
| 138 | <code>                List&lt;Point&gt; route = MimicLogic.buildPatrolRoute(base, cand, radius); // its patrol path</code> | Inline comment: its patrol path |
| 139 | <code>                if (route == null &#124;&#124; route.size() &lt; 5) continue; // too short to be a real patrol</code> | Inline comment: too short to be a real patrol |
| 140 | <code>                if (!patrolIsValid(route, taken, fromStart, fromTreasure, fromExit)) continue; // too close to something important</code> | Inline comment: too close to something important |
| 141 | <code>                homes.add(cand); // accept this mimic</code> | Inline comment: accept this mimic |
| 142 | <code>            } // end of candidate loop</code> | Inline comment: end of candidate loop |
| 143 | <code>            if (homes.size() &lt; spec[4]) return null; // could not place all mimics</code> | Inline comment: could not place all mimics |
| 144 | <code>        } // end of mimic placement</code> | Inline comment: end of mimic placement |
| 145 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 146 | <code>        // Write all markers into the grid first, so bomb validation sees the final layout.</code> | Inline comment: Write all markers into the grid first, so bomb validation sees the final layout. |
| 147 | <code>        g[start.y][start.x] = 'P'; // mark start</code> | Inline comment: mark start |
| 148 | <code>        g[treasure.y][treasure.x] = 'T'; // mark chest</code> | Inline comment: mark chest |
| 149 | <code>        g[exit.y][exit.x] = 'E'; // mark exit</code> | Inline comment: mark exit |
| 150 | <code>        for (Point c : coins) { // each coin</code> | Inline comment: each coin |
| 151 | <code>            g[c.y][c.x] = 'C'; // mark coin</code> | Inline comment: mark coin |
| 152 | <code>        } // end of coin loop</code> | Inline comment: end of coin loop |
| 153 | <code>        for (Point m : homes) { // each mimic home</code> | Inline comment: each mimic home |
| 154 | <code>            g[m.y][m.x] = 'M'; // mark mimic</code> | Inline comment: mark mimic |
| 155 | <code>        } // end of mimic loop</code> | Inline comment: end of mimic loop |
| 156 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 157 | <code>        // Bombs: only on empty floor, never closing the safe routes.</code> | Inline comment: Bombs: only on empty floor, never closing the safe routes. |
| 158 | <code>        int minBombDist = level == 1 ? 5 : 3; // level 1 keeps bombs away from the start</code> | Inline comment: level 1 keeps bombs away from the start |
| 159 | <code>        List&lt;Point&gt; bombCandidates = new ArrayList&lt;&gt;(); // possible bomb cells</code> | Inline comment: possible bomb cells |
| 160 | <code>        for (int y = 1; y &lt; h - 1; y++) { // inner rows</code> | Inline comment: inner rows |
| 161 | <code>            for (int x = 1; x &lt; w - 1; x++) { // inner columns</code> | Inline comment: inner columns |
| 162 | <code>                if (g[y][x] == '.' &amp;&amp; fromStart[y][x] &gt;= minBombDist // empty floor, not next to the start</code> | Inline comment: empty floor, not next to the start |
| 163 | <code>                    &amp;&amp; Math.abs(x - treasure.x) + Math.abs(y - treasure.y) &gt; 1 // never touching the real chest</code> | Inline comment: never touching the real chest |
| 164 | <code>                    &amp;&amp; Math.abs(x - exit.x) + Math.abs(y - exit.y) &gt; 1) { // never touching the exit</code> | Inline comment: never touching the exit |
| 165 | <code>                    bombCandidates.add(new Point(x, y)); // keep it</code> | Inline comment: keep it |
| 166 | <code>                } // end of filter</code> | Inline comment: end of filter |
| 167 | <code>            } // end of column loop</code> | Inline comment: end of column loop |
| 168 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 169 | <code>        Collections.shuffle(bombCandidates, RND); // random order</code> | Inline comment: random order |
| 170 | <code>        int placed = 0; // bombs placed so far</code> | Inline comment: bombs placed so far |
| 171 | <code>        for (Point p : bombCandidates) { // try each candidate</code> | Inline comment: try each candidate |
| 172 | <code>            if (placed &gt;= spec[3]) break; // enough bombs</code> | Inline comment: enough bombs |
| 173 | <code>            g[p.y][p.x] = 'B'; // tentatively place a bomb</code> | Inline comment: tentatively place a bomb |
| 174 | <code>            if (safeRoutesExist(g, start, treasure, exit, coins)) { // everything still reachable without bombs</code> | Inline comment: everything still reachable without bombs |
| 175 | <code>                placed++; // keep the bomb</code> | Inline comment: keep the bomb |
| 176 | <code>            } else { // a route would be blocked</code> | Inline comment: a route would be blocked |
| 177 | <code>                g[p.y][p.x] = '.'; // undo it</code> | Inline comment: undo it |
| 178 | <code>            } // end of check</code> | Inline comment: end of check |
| 179 | <code>        } // end of candidate loop</code> | Inline comment: end of candidate loop |
| 180 | <code>        if (placed &lt; spec[3]) return null; // could not place all bombs</code> | Inline comment: could not place all bombs |
| 181 | <code>        if (!makeBlueBombs(g, start)) return null; // turn 5 of the pickups into blue ones</code> | Inline comment: turn 5 of the pickups into blue ones |
| 182 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 183 | <code>        return toMap(g); // validated map</code> | Inline comment: validated map |
| 184 | <code>    } // end of tryBuild</code> | Inline comment: end of tryBuild |
| 185 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 186 | <code>    /**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 187 | <code>     * Converts BLUE_BOMBS of the placed black bombs ('B') into blue pickups ('U'). Only bombs next to a cell the player can</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 188 | <code>     * reach without touching a black bomb are used (so every blue bomb is reachable), and they are picked far apart (spread out).</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 189 | <code>     */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 190 | <code>    private static boolean makeBlueBombs(char[][] g, Point start) { // returns false if not enough reachable bombs</code> | Inline comment: returns false if not enough reachable bombs |
| 191 | <code>        int h = g.length; // rows</code> | Inline comment: rows |
| 192 | <code>        int w = g[0].length; // columns</code> | Inline comment: columns |
| 193 | <code>        boolean[][] seen = new boolean[h][w]; // cells reachable from the start without touching a black bomb</code> | Inline comment: cells reachable from the start without touching a black bomb |
| 194 | <code>        java.util.ArrayDeque&lt;Point&gt; queue = new java.util.ArrayDeque&lt;&gt;(); // BFS queue</code> | Inline comment: BFS queue |
| 195 | <code>        queue.add(start); // begin at the start</code> | Inline comment: begin at the start |
| 196 | <code>        seen[start.y][start.x] = true; // visited</code> | Inline comment: visited |
| 197 | <code>        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}; // four directions</code> | Inline comment: four directions |
| 198 | <code>        while (!queue.isEmpty()) { // flood fill</code> | Inline comment: flood fill |
| 199 | <code>            Point c = queue.poll(); // next cell</code> | Inline comment: next cell |
| 200 | <code>            for (int[] d : dirs) { // each neighbour</code> | Inline comment: each neighbour |
| 201 | <code>                int nx = c.x + d[0]; // neighbour column</code> | Inline comment: neighbour column |
| 202 | <code>                int ny = c.y + d[1]; // neighbour row</code> | Inline comment: neighbour row |
| 203 | <code>                if (nx &lt; 0 &#124;&#124; ny &lt; 0 &#124;&#124; nx &gt;= w &#124;&#124; ny &gt;= h &#124;&#124; seen[ny][nx] &#124;&#124; g[ny][nx] == '#' &#124;&#124; g[ny][nx] == 'B') continue; // wall, bomb or visited</code> | Inline comment: wall, bomb or visited |
| 204 | <code>                seen[ny][nx] = true; // reachable</code> | Inline comment: reachable |
| 205 | <code>                queue.add(new Point(nx, ny)); // continue from there</code> | Inline comment: continue from there |
| 206 | <code>            } // end of neighbour loop</code> | Inline comment: end of neighbour loop |
| 207 | <code>        } // end of flood fill</code> | Inline comment: end of flood fill |
| 208 | <code>        List&lt;Point&gt; candidates = new ArrayList&lt;&gt;(); // black bombs that can be reached</code> | Inline comment: black bombs that can be reached |
| 209 | <code>        for (int y = 1; y &lt; h - 1; y++) { // each row</code> | Inline comment: each row |
| 210 | <code>            for (int x = 1; x &lt; w - 1; x++) { // each column</code> | Inline comment: each column |
| 211 | <code>                if (g[y][x] != 'B') continue; // only black bombs</code> | Inline comment: only black bombs |
| 212 | <code>                boolean next = seen[y][x - 1] &#124;&#124; seen[y][x + 1] &#124;&#124; seen[y - 1][x] &#124;&#124; seen[y + 1][x]; // a reachable neighbour exists</code> | Inline comment: a reachable neighbour exists |
| 213 | <code>                if (next) candidates.add(new Point(x, y)); // usable</code> | Inline comment: usable |
| 214 | <code>            } // end of column loop</code> | Inline comment: end of column loop |
| 215 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 216 | <code>        if (candidates.size() &lt; BLUE_BOMBS) return false; // not enough</code> | Inline comment: not enough |
| 217 | <code>        List&lt;Point&gt; chosen = new ArrayList&lt;&gt;(); // picked blue bombs</code> | Inline comment: picked blue bombs |
| 218 | <code>        chosen.add(candidates.remove(RND.nextInt(candidates.size()))); // first one at random</code> | Inline comment: first one at random |
| 219 | <code>        while (chosen.size() &lt; BLUE_BOMBS) { // pick the rest as far from the others as possible</code> | Inline comment: pick the rest as far from the others as possible |
| 220 | <code>            Point best = null; // best candidate so far</code> | Inline comment: best candidate so far |
| 221 | <code>            int bestDist = -1; // its distance to the nearest chosen bomb</code> | Inline comment: its distance to the nearest chosen bomb |
| 222 | <code>            for (Point c : candidates) { // each candidate</code> | Inline comment: each candidate |
| 223 | <code>                int nearest = Integer.MAX_VALUE; // distance to the closest chosen one</code> | Inline comment: distance to the closest chosen one |
| 224 | <code>                for (Point o : chosen) nearest = Math.min(nearest, Math.abs(c.x - o.x) + Math.abs(c.y - o.y)); // Manhattan distance</code> | Inline comment: Manhattan distance |
| 225 | <code>                if (nearest &gt; bestDist) { bestDist = nearest; best = c; } // keep the most isolated</code> | Inline comment: keep the most isolated |
| 226 | <code>            } // end of candidate loop</code> | Inline comment: end of candidate loop |
| 227 | <code>            candidates.remove(best); // used</code> | Inline comment: used |
| 228 | <code>            chosen.add(best); // chosen</code> | Inline comment: chosen |
| 229 | <code>        } // end of spread</code> | Inline comment: end of spread |
| 230 | <code>        for (Point p : chosen) g[p.y][p.x] = 'U'; // blue pickup marker</code> | Inline comment: blue pickup marker |
| 231 | <code>        return true; // done</code> | Inline comment: done |
| 232 | <code>    } // end of makeBlueBombs</code> | Inline comment: end of makeBlueBombs |
| 233 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 234 | <code>    /** True when the start can reach the chest, the exit and every coin without stepping on a bomb. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 235 | <code>    private static boolean safeRoutesExist(char[][] g, Point start, Point treasure, Point exit, List&lt;Point&gt; coins) { // validation</code> | Inline comment: validation |
| 236 | <code>        int[][] d = Pathfinder.distances(toMap(g), start, true); // BFS that treats bombs as walls</code> | Inline comment: BFS that treats bombs as walls |
| 237 | <code>        if (d[treasure.y][treasure.x] &lt; 0 &#124;&#124; d[exit.y][exit.x] &lt; 0) return false; // chest or exit cut off</code> | Inline comment: chest or exit cut off |
| 238 | <code>        for (Point c : coins) { // each coin</code> | Inline comment: each coin |
| 239 | <code>            if (d[c.y][c.x] &lt; 0) return false; // a coin cut off</code> | Inline comment: a coin cut off |
| 240 | <code>        } // end of coin loop</code> | Inline comment: end of coin loop |
| 241 | <code>        return true; // all reachable (paths are two-way, so chest -&gt; exit works too)</code> | Inline comment: all reachable (paths are two-way, so chest -&gt; exit works too) |
| 242 | <code>    } // end of safeRoutesExist</code> | Inline comment: end of safeRoutesExist |
| 243 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 244 | <code>    private static boolean patrolIsValid(List&lt;Point&gt; route, List&lt;Point&gt; taken, // every patrol cell must be safe</code> | Inline comment: every patrol cell must be safe |
| 245 | <code>                                         int[][] fromStart, int[][] fromTreasure, int[][] fromExit) { // distance tables</code> | Inline comment: distance tables |
| 246 | <code>        for (Point p : route) { // each patrol cell</code> | Inline comment: each patrol cell |
| 247 | <code>            if (taken.contains(p)) return false; // on top of start, chest, exit or a coin</code> | Inline comment: on top of start, chest, exit or a coin |
| 248 | <code>            if (fromTreasure[p.y][p.x] &lt; MIN_PATROL_TO_TREASURE) return false; // too close to the chest</code> | Inline comment: too close to the chest |
| 249 | <code>            if (fromStart[p.y][p.x] &lt; MIN_PATROL_TO_START) return false; // too close to the start</code> | Inline comment: too close to the start |
| 250 | <code>            if (fromExit[p.y][p.x] &lt; MIN_PATROL_TO_EXIT) return false; // too close to the exit</code> | Inline comment: too close to the exit |
| 251 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 252 | <code>        return true; // route is fine</code> | Inline comment: route is fine |
| 253 | <code>    } // end of patrolIsValid</code> | Inline comment: end of patrolIsValid |
| 254 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 255 | <code>    private static boolean farFromOtherHomes(Point cand, List&lt;Point&gt; homes) { // keep mimics apart</code> | Inline comment: keep mimics apart |
| 256 | <code>        for (Point other : homes) { // each existing mimic</code> | Inline comment: each existing mimic |
| 257 | <code>            if (Math.abs(cand.x - other.x) + Math.abs(cand.y - other.y) &lt; 10) return false; // too close</code> | Inline comment: too close |
| 258 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 259 | <code>        return true; // far enough</code> | Inline comment: far enough |
| 260 | <code>    } // end of farFromOtherHomes</code> | Inline comment: end of farFromOtherHomes |
| 261 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 262 | <code>    private static List&lt;Point&gt; pickCoins(char[][] g, int[][] fromStart, int count, List&lt;Point&gt; taken) { // choose coin cells</code> | Inline comment: choose coin cells |
| 263 | <code>        List&lt;Point&gt; deadEnds = new ArrayList&lt;&gt;(); // corridor ends (a corridor with one exit)</code> | Inline comment: corridor ends (a corridor with one exit) |
| 264 | <code>        List&lt;Point&gt; others = new ArrayList&lt;&gt;(); // ordinary floor cells</code> | Inline comment: ordinary floor cells |
| 265 | <code>        for (int y = 1; y &lt; g.length - 1; y++) { // inner rows</code> | Inline comment: inner rows |
| 266 | <code>            for (int x = 1; x &lt; g[y].length - 1; x++) { // inner columns</code> | Inline comment: inner columns |
| 267 | <code>                Point p = new Point(x, y); // this cell</code> | Inline comment: this cell |
| 268 | <code>                if (g[y][x] != '.' &#124;&#124; fromStart[y][x] &lt; 6 &#124;&#124; taken.contains(p)) continue; // must be free floor, not near the start</code> | Inline comment: must be free floor, not near the start |
| 269 | <code>                int open = 0; // number of walkable neighbours</code> | Inline comment: number of walkable neighbours |
| 270 | <code>                for (int[] d : DIRS) { // each direction</code> | Inline comment: each direction |
| 271 | <code>                    if (g[y + d[1]][x + d[0]] != '#') open++; // neighbour is walkable</code> | Inline comment: neighbour is walkable |
| 272 | <code>                } // end of direction loop</code> | Inline comment: end of direction loop |
| 273 | <code>                if (open == 1) { // dead end</code> | Inline comment: dead end |
| 274 | <code>                    deadEnds.add(p); // best coin spot</code> | Inline comment: best coin spot |
| 275 | <code>                } else { // ordinary cell</code> | Inline comment: ordinary cell |
| 276 | <code>                    others.add(p); // fallback spot</code> | Inline comment: fallback spot |
| 277 | <code>                } // end of type check</code> | Inline comment: end of type check |
| 278 | <code>            } // end of column loop</code> | Inline comment: end of column loop |
| 279 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 280 | <code>        Collections.shuffle(deadEnds, RND); // random order</code> | Inline comment: random order |
| 281 | <code>        Collections.shuffle(others, RND); // random order</code> | Inline comment: random order |
| 282 | <code>        List&lt;Point&gt; pool = new ArrayList&lt;&gt;(deadEnds); // dead ends first</code> | Inline comment: dead ends first |
| 283 | <code>        pool.addAll(others); // then other cells</code> | Inline comment: then other cells |
| 284 | <code>        List&lt;Point&gt; chosen = new ArrayList&lt;&gt;(); // result</code> | Inline comment: result |
| 285 | <code>        for (Point p : pool) { // each candidate</code> | Inline comment: each candidate |
| 286 | <code>            if (chosen.size() &gt;= count) break; // enough coins</code> | Inline comment: enough coins |
| 287 | <code>            boolean farEnough = true; // spacing check</code> | Inline comment: spacing check |
| 288 | <code>            for (Point c : chosen) { // each coin already chosen</code> | Inline comment: each coin already chosen |
| 289 | <code>                if (Math.abs(p.x - c.x) + Math.abs(p.y - c.y) &lt; 6) { // too close to another coin</code> | Inline comment: too close to another coin |
| 290 | <code>                    farEnough = false; // reject</code> | Inline comment: reject |
| 291 | <code>                    break; // stop checking</code> | Inline comment: stop checking |
| 292 | <code>                } // end of distance check</code> | Inline comment: end of distance check |
| 293 | <code>            } // end of chosen loop</code> | Inline comment: end of chosen loop |
| 294 | <code>            if (farEnough) { // accepted</code> | Inline comment: accepted |
| 295 | <code>                chosen.add(p); // use it</code> | Inline comment: use it |
| 296 | <code>            } // end of accept</code> | Inline comment: end of accept |
| 297 | <code>        } // end of pool loop</code> | Inline comment: end of pool loop |
| 298 | <code>        return chosen; // coin cells</code> | Inline comment: coin cells |
| 299 | <code>    } // end of pickCoins</code> | Inline comment: end of pickCoins |
| 300 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 301 | <code>    private static void carveMaze(char[][] g, int w, int h, Point start) { // randomized depth-first maze carving</code> | Inline comment: randomized depth-first maze carving |
| 302 | <code>        ArrayDeque&lt;int[]&gt; stack = new ArrayDeque&lt;&gt;(); // cells on the current path</code> | Inline comment: cells on the current path |
| 303 | <code>        g[start.y][start.x] = '.'; // open the start cell</code> | Inline comment: open the start cell |
| 304 | <code>        stack.push(new int[] {start.x, start.y}); // begin at the start</code> | Inline comment: begin at the start |
| 305 | <code>        while (!stack.isEmpty()) { // until every reachable cell is visited</code> | Inline comment: until every reachable cell is visited |
| 306 | <code>            int[] cur = stack.peek(); // current cell</code> | Inline comment: current cell |
| 307 | <code>            List&lt;int[]&gt; dirs = new ArrayList&lt;&gt;(List.of(DIRS)); // directions to try</code> | Inline comment: directions to try |
| 308 | <code>            Collections.shuffle(dirs, RND); // random direction order</code> | Inline comment: random direction order |
| 309 | <code>            boolean moved = false; // did we dig somewhere</code> | Inline comment: did we dig somewhere |
| 310 | <code>            for (int[] d : dirs) { // try each direction</code> | Inline comment: try each direction |
| 311 | <code>                int nx = cur[0] + d[0] * 2; // target column, two cells away</code> | Inline comment: target column, two cells away |
| 312 | <code>                int ny = cur[1] + d[1] * 2; // target row, two cells away</code> | Inline comment: target row, two cells away |
| 313 | <code>                if (nx &gt;= 1 &amp;&amp; nx &lt;= w - 2 &amp;&amp; ny &gt;= 1 &amp;&amp; ny &lt;= h - 2 &amp;&amp; g[ny][nx] == '#') { // inside the border and still wall</code> | Inline comment: inside the border and still wall |
| 314 | <code>                    g[cur[1] + d[1]][cur[0] + d[0]] = '.'; // open the wall between</code> | Inline comment: open the wall between |
| 315 | <code>                    g[ny][nx] = '.'; // open the target cell</code> | Inline comment: open the target cell |
| 316 | <code>                    stack.push(new int[] {nx, ny}); // continue from the new cell</code> | Inline comment: continue from the new cell |
| 317 | <code>                    moved = true; // we dug</code> | Inline comment: we dug |
| 318 | <code>                    break; // leave the direction loop</code> | Inline comment: leave the direction loop |
| 319 | <code>                } // end of dig check</code> | Inline comment: end of dig check |
| 320 | <code>            } // end of direction loop</code> | Inline comment: end of direction loop |
| 321 | <code>            if (!moved) { // dead end</code> | Inline comment: dead end |
| 322 | <code>                stack.pop(); // backtrack</code> | Inline comment: backtrack |
| 323 | <code>            } // end of dead-end check</code> | Inline comment: end of dead-end check |
| 324 | <code>        } // end of carving loop</code> | Inline comment: end of carving loop |
| 325 | <code>    } // end of carveMaze</code> | Inline comment: end of carveMaze |
| 326 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 327 | <code>    private static void openShortcuts(char[][] g, int w, int h, int count) { // remove some walls to create loops</code> | Inline comment: remove some walls to create loops |
| 328 | <code>        int opened = 0; // walls removed so far</code> | Inline comment: walls removed so far |
| 329 | <code>        int attempts = 0; // tries so far</code> | Inline comment: tries so far |
| 330 | <code>        while (opened &lt; count &amp;&amp; attempts &lt; count * 200) { // until enough or too many tries</code> | Inline comment: until enough or too many tries |
| 331 | <code>            attempts++; // count the try</code> | Inline comment: count the try |
| 332 | <code>            int x = 1 + RND.nextInt(w - 2); // random inner column</code> | Inline comment: random inner column |
| 333 | <code>            int y = 1 + RND.nextInt(h - 2); // random inner row</code> | Inline comment: random inner row |
| 334 | <code>            boolean wallBetweenCells = (x % 2 == 1 &amp;&amp; y % 2 == 0) &#124;&#124; (x % 2 == 0 &amp;&amp; y % 2 == 1); // wall that separates two floor cells</code> | Inline comment: wall that separates two floor cells |
| 335 | <code>            if (g[y][x] == '#' &amp;&amp; wallBetweenCells) { // removable wall</code> | Inline comment: removable wall |
| 336 | <code>                g[y][x] = '.'; // open it</code> | Inline comment: open it |
| 337 | <code>                opened++; // count it</code> | Inline comment: count it |
| 338 | <code>            } // end of wall check</code> | Inline comment: end of wall check |
| 339 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 340 | <code>    } // end of openShortcuts</code> | Inline comment: end of openShortcuts |
| 341 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 342 | <code>    private static Point pickInBand(int[][] dist, int lo, int hi, List&lt;Point&gt; excluded) { // random cell with lo &lt;= distance &lt;= hi</code> | Inline comment: random cell with lo &lt;= distance &lt;= hi |
| 343 | <code>        List&lt;Point&gt; options = new ArrayList&lt;&gt;(); // matching cells</code> | Inline comment: matching cells |
| 344 | <code>        for (int y = 0; y &lt; dist.length; y++) { // for each row</code> | Inline comment: for each row |
| 345 | <code>            for (int x = 0; x &lt; dist[y].length; x++) { // for each column</code> | Inline comment: for each column |
| 346 | <code>                int d = dist[y][x]; // distance of this cell</code> | Inline comment: distance of this cell |
| 347 | <code>                if (d &gt;= lo &amp;&amp; d &lt;= hi &amp;&amp; d &gt; 0 &amp;&amp; !excluded.contains(new Point(x, y))) { // inside the band, reachable, not excluded</code> | Inline comment: inside the band, reachable, not excluded |
| 348 | <code>                    options.add(new Point(x, y)); // candidate</code> | Inline comment: candidate |
| 349 | <code>                } // end of band check</code> | Inline comment: end of band check |
| 350 | <code>            } // end of column loop</code> | Inline comment: end of column loop |
| 351 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 352 | <code>        if (options.isEmpty()) return null; // nothing matched</code> | Inline comment: nothing matched |
| 353 | <code>        return options.get(RND.nextInt(options.size())); // pick one at random</code> | Inline comment: pick one at random |
| 354 | <code>    } // end of pickInBand</code> | Inline comment: end of pickInBand |
| 355 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 356 | <code>    private static int maxOf(int[][] dist) { // largest value in a distance table</code> | Inline comment: largest value in a distance table |
| 357 | <code>        int max = 0; // best so far</code> | Inline comment: best so far |
| 358 | <code>        for (int[] row : dist) { // for each row</code> | Inline comment: for each row |
| 359 | <code>            for (int d : row) { // for each cell</code> | Inline comment: for each cell |
| 360 | <code>                max = Math.max(max, d); // track the maximum</code> | Inline comment: track the maximum |
| 361 | <code>            } // end of cell loop</code> | Inline comment: end of cell loop |
| 362 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 363 | <code>        return max; // result</code> | Inline comment: result |
| 364 | <code>    } // end of maxOf</code> | Inline comment: end of maxOf |
| 365 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 366 | <code>    private static String[] toMap(char[][] g) { // convert the char grid to strings</code> | Inline comment: convert the char grid to strings |
| 367 | <code>        String[] out = new String[g.length]; // one string per row</code> | Inline comment: one string per row |
| 368 | <code>        for (int i = 0; i &lt; g.length; i++) { // for each row</code> | Inline comment: for each row |
| 369 | <code>            out[i] = new String(g[i]); // build the row string</code> | Inline comment: build the row string |
| 370 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 371 | <code>        return out; // string map</code> | Inline comment: string map |
| 372 | <code>    } // end of toMap</code> | Inline comment: end of toMap |
| 373 | <code>} // end of GameMap</code> | Inline comment: end of GameMap |


## `src/game/Player.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package game; // package for core game objects</code> | Inline comment: package for core game objects |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>public class Player { // the player position holder</code> | Inline comment: the player position holder |
| 4 | <code>    private int x; // column on the grid</code> | Inline comment: column on the grid |
| 5 | <code>    private int y; // row on the grid</code> | Inline comment: row on the grid |
| 6 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 7 | <code>    public Player(int startX, int startY) { // create a player at a start cell</code> | Inline comment: create a player at a start cell |
| 8 | <code>        this.x = startX; // store start column</code> | Inline comment: store start column |
| 9 | <code>        this.y = startY; // store start row</code> | Inline comment: store start row |
| 10 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 11 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 12 | <code>    public int getX() { // read the column</code> | Inline comment: read the column |
| 13 | <code>        return x; // return column</code> | Inline comment: return column |
| 14 | <code>    } // end of getX</code> | Inline comment: end of getX |
| 15 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 16 | <code>    public int getY() { // read the row</code> | Inline comment: read the row |
| 17 | <code>        return y; // return row</code> | Inline comment: return row |
| 18 | <code>    } // end of getY</code> | Inline comment: end of getY |
| 19 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 20 | <code>    public void setPosition(int x, int y) { // teleport to a cell</code> | Inline comment: teleport to a cell |
| 21 | <code>        this.x = x; // set column</code> | Inline comment: set column |
| 22 | <code>        this.y = y; // set row</code> | Inline comment: set row |
| 23 | <code>    } // end of setPosition</code> | Inline comment: end of setPosition |
| 24 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 25 | <code>    public void move(int dx, int dy) { // move relative to the current cell</code> | Inline comment: move relative to the current cell |
| 26 | <code>        this.x += dx; // shift column</code> | Inline comment: shift column |
| 27 | <code>        this.y += dy; // shift row</code> | Inline comment: shift row |
| 28 | <code>    } // end of move</code> | Inline comment: end of move |
| 29 | <code>} // end of Player</code> | Inline comment: end of Player |


## `src/game/Sprites.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package game; // package for core game objects</code> | Inline comment: package for core game objects |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import data.AssetPaths; // finds the assets folder</code> | Inline comment: finds the assets folder |
| 4 | <code>import java.awt.Graphics2D; // drawing surface</code> | Inline comment: drawing surface |
| 5 | <code>import java.awt.RenderingHints; // drawing quality options</code> | Inline comment: drawing quality options |
| 6 | <code>import java.awt.image.BufferedImage; // in-memory picture</code> | Inline comment: in-memory picture |
| 7 | <code>import java.io.File; // file path</code> | Inline comment: file path |
| 8 | <code>import java.io.IOException; // thrown when an image cannot be read</code> | Inline comment: thrown when an image cannot be read |
| 9 | <code>import java.util.HashMap; // name -&gt; image lookup table</code> | Inline comment: name -&gt; image lookup table |
| 10 | <code>import java.util.Map; // map interface</code> | Inline comment: map interface |
| 11 | <code>import javax.imageio.ImageIO; // reads PNG files</code> | Inline comment: reads PNG files |
| 12 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 13 | <code>/**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 14 | <code> * Loads the pixel-art pictures once and draws them crisp (no smoothing).</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 15 | <code> * Tiles come from Kenney "Tiny Dungeon" (CC0); the explosion from Kenney "Explosion Pack" (CC0);</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 16 | <code> * bomb, coin and heart are drawn by tools/make_custom_sprites.py.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 17 | <code> * If a file is missing the game still runs: callers fall back to letters.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 18 | <code> */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 19 | <code>public final class Sprites { // picture library</code> | Inline comment: picture library |
| 20 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 21 | <code>    private static final int EXPLOSION_FRAMES = 9; // pixelExplosion00 .. pixelExplosion08</code> | Inline comment: pixelExplosion00 .. pixelExplosion08 |
| 22 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 23 | <code>    private final Map&lt;String, BufferedImage&gt; images = new HashMap&lt;&gt;(); // sprite name -&gt; picture</code> | Inline comment: sprite name -&gt; picture |
| 24 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 25 | <code>    public Sprites() { // load every sprite</code> | Inline comment: load every sprite |
| 26 | <code>        String td = "kenney-tiny-dungeon/tile_"; // folder + file prefix of the dungeon tiles</code> | Inline comment: folder + file prefix of the dungeon tiles |
| 27 | <code>        load("floor0", td + "0000.png"); // plain dungeon floor</code> | Inline comment: plain dungeon floor |
| 28 | <code>        load("floor1", td + "0001.png"); // second floor variant</code> | Inline comment: second floor variant |
| 29 | <code>        load("cracked", td + "0012.png"); // cracked floor (level 1 tile about to vanish)</code> | Inline comment: cracked floor (level 1 tile about to vanish) |
| 30 | <code>        load("wall0", td + "0040.png"); // stone brick wall</code> | Inline comment: stone brick wall |
| 31 | <code>        load("wall1", td + "0014.png"); // second brick wall variant</code> | Inline comment: second brick wall variant |
| 32 | <code>        load("wall2", td + "0028.png"); // wall with a barred window</code> | Inline comment: wall with a barred window |
| 33 | <code>        load("doorClosed", td + "0045.png"); // locked exit door</code> | Inline comment: locked exit door |
| 34 | <code>        load("doorOpen", td + "0010.png"); // open exit doorway</code> | Inline comment: open exit doorway |
| 35 | <code>        load("chestClosed", td + "0090.png"); // treasure chest, closed</code> | Inline comment: treasure chest, closed |
| 36 | <code>        load("chestOpen", td + "0091.png"); // treasure chest, open</code> | Inline comment: treasure chest, open |
| 37 | <code>        load("mimic", td + "0092.png"); // mimic (chest with teeth and tongue)</code> | Inline comment: mimic (chest with teeth and tongue) |
| 38 | <code>        load("player", td + "0097.png"); // knight with visor</code> | Inline comment: knight with visor |
| 39 | <code>        load("bomb", "custom/bomb.png"); // bomb (custom sprite)</code> | Inline comment: bomb (custom sprite) |
| 40 | <code>        load("bombBlue", "custom/bomb_blue.png"); // blue Mimic-trap bomb (recolored bomb)</code> | Inline comment: blue Mimic-trap bomb (recolored bomb) |
| 41 | <code>        load("coin", "custom/coin.png"); // coin (custom sprite)</code> | Inline comment: coin (custom sprite) |
| 42 | <code>        load("heart", "custom/heart.png"); // heart (custom sprite)</code> | Inline comment: heart (custom sprite) |
| 43 | <code>        for (int i = 0; i &lt; EXPLOSION_FRAMES; i++) { // each explosion frame</code> | Inline comment: each explosion frame |
| 44 | <code>            load("explosion" + i, String.format("kenney-explosion/pixelExplosion%02d.png", i)); // frame i</code> | Inline comment: frame i |
| 45 | <code>        } // end of frame loop</code> | Inline comment: end of frame loop |
| 46 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 47 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 48 | <code>    private void load(String name, String relativePath) { // read one image into the table</code> | Inline comment: read one image into the table |
| 49 | <code>        try { // reading can fail</code> | Inline comment: reading can fail |
| 50 | <code>            File file = AssetPaths.file(relativePath); // locate the file</code> | Inline comment: locate the file |
| 51 | <code>            if (file.isFile()) { // it exists</code> | Inline comment: it exists |
| 52 | <code>                BufferedImage image = ImageIO.read(file); // decode the PNG</code> | Inline comment: decode the PNG |
| 53 | <code>                if (image != null) { // decoded successfully</code> | Inline comment: decoded successfully |
| 54 | <code>                    images.put(name, image); // remember it</code> | Inline comment: remember it |
| 55 | <code>                } // end of decode check</code> | Inline comment: end of decode check |
| 56 | <code>            } // end of exists check</code> | Inline comment: end of exists check |
| 57 | <code>        } catch (IOException e) { // unreadable file</code> | Inline comment: unreadable file |
| 58 | <code>            // leave the sprite out: the game falls back to a letter for this object</code> | Inline comment: leave the sprite out: the game falls back to a letter for this object |
| 59 | <code>        } // end of try/catch</code> | Inline comment: end of try/catch |
| 60 | <code>    } // end of load</code> | Inline comment: end of load |
| 61 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 62 | <code>    public boolean has(String name) { // is this sprite available</code> | Inline comment: is this sprite available |
| 63 | <code>        return images.containsKey(name); // true if it loaded</code> | Inline comment: true if it loaded |
| 64 | <code>    } // end of has</code> | Inline comment: end of has |
| 65 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 66 | <code>    /** Draws a sprite scaled to a square of {@code size} pixels, optionally mirrored left-right. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 67 | <code>    public void draw(Graphics2D g, String name, int x, int y, int size, boolean flipX) { // draw one sprite</code> | Inline comment: draw one sprite |
| 68 | <code>        BufferedImage image = images.get(name); // look it up</code> | Inline comment: look it up |
| 69 | <code>        if (image == null) return; // missing sprite: draw nothing</code> | Inline comment: missing sprite: draw nothing |
| 70 | <code>        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR); // crisp pixels</code> | Inline comment: crisp pixels |
| 71 | <code>        if (flipX) { // mirrored</code> | Inline comment: mirrored |
| 72 | <code>            g.drawImage(image, x + size, y, -size, size, null); // negative width flips the picture</code> | Inline comment: negative width flips the picture |
| 73 | <code>        } else { // normal</code> | Inline comment: normal |
| 74 | <code>            g.drawImage(image, x, y, size, size, null); // draw scaled</code> | Inline comment: draw scaled |
| 75 | <code>        } // end of flip check</code> | Inline comment: end of flip check |
| 76 | <code>    } // end of draw</code> | Inline comment: end of draw |
| 77 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 78 | <code>    /** Draws a sprite scaled to an exact width and height (used for the explosion, which is not square). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 79 | <code>    public void drawScaled(Graphics2D g, String name, int x, int y, int width, int height) { // draw with custom size</code> | Inline comment: draw with custom size |
| 80 | <code>        BufferedImage image = images.get(name); // look it up</code> | Inline comment: look it up |
| 81 | <code>        if (image == null) return; // missing sprite: draw nothing</code> | Inline comment: missing sprite: draw nothing |
| 82 | <code>        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR); // crisp pixels</code> | Inline comment: crisp pixels |
| 83 | <code>        g.drawImage(image, x, y, width, height, null); // draw scaled</code> | Inline comment: draw scaled |
| 84 | <code>    } // end of drawScaled</code> | Inline comment: end of drawScaled |
| 85 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 86 | <code>    public int explosionFrames() { // number of explosion frames</code> | Inline comment: number of explosion frames |
| 87 | <code>        return EXPLOSION_FRAMES; // constant</code> | Inline comment: constant |
| 88 | <code>    } // end of explosionFrames</code> | Inline comment: end of explosionFrames |
| 89 | <code>} // end of Sprites</code> | Inline comment: end of Sprites |


## `src/game/TreasureMaze.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package game; // package for core game objects</code> | Inline comment: package for core game objects |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import audio.SoundManager; // sound effects and music</code> | Inline comment: sound effects and music |
| 4 | <code>import java.awt.AlphaComposite; // transparency for drawing</code> | Inline comment: transparency for drawing |
| 5 | <code>import java.awt.Color; // RGB color</code> | Inline comment: RGB color |
| 6 | <code>import java.awt.Font; // text font</code> | Inline comment: text font |
| 7 | <code>import java.awt.FontMetrics; // text size measurements</code> | Inline comment: text size measurements |
| 8 | <code>import java.awt.Graphics; // drawing surface</code> | Inline comment: drawing surface |
| 9 | <code>import java.awt.Graphics2D; // richer drawing surface</code> | Inline comment: richer drawing surface |
| 10 | <code>import java.awt.Point; // integer x/y coordinate</code> | Inline comment: integer x/y coordinate |
| 11 | <code>import java.awt.RenderingHints; // drawing quality options</code> | Inline comment: drawing quality options |
| 12 | <code>import java.awt.event.KeyAdapter; // key listener with empty methods</code> | Inline comment: key listener with empty methods |
| 13 | <code>import java.awt.event.KeyEvent; // keyboard event</code> | Inline comment: keyboard event |
| 14 | <code>import java.awt.event.MouseAdapter; // mouse listener with empty methods</code> | Inline comment: mouse listener with empty methods |
| 15 | <code>import java.awt.event.MouseEvent; // mouse event</code> | Inline comment: mouse event |
| 16 | <code>import java.util.ArrayList; // growable list</code> | Inline comment: growable list |
| 17 | <code>import java.util.List; // list interface</code> | Inline comment: list interface |
| 18 | <code>import javax.swing.JPanel; // container used as the game canvas</code> | Inline comment: container used as the game canvas |
| 19 | <code>import javax.swing.Timer; // Swing timer that fires on the UI thread</code> | Inline comment: Swing timer that fires on the UI thread |
| 20 | <code>import logic.BombLogic; // bomb rules</code> | Inline comment: bomb rules |
| 21 | <code>import logic.CoinLogic; // coin rules (alternative way to unlock the exit)</code> | Inline comment: coin rules (alternative way to unlock the exit) |
| 22 | <code>import logic.LevelManager; // level tracking</code> | Inline comment: level tracking |
| 23 | <code>import logic.MimicLogic; // mimic rules</code> | Inline comment: mimic rules |
| 24 | <code>import logic.Pathfinder; // BFS helpers</code> | Inline comment: BFS helpers |
| 25 | <code>import logic.ScoreManager; // score and lives</code> | Inline comment: score and lives |
| 26 | <code>import logic.TreasureLogic; // treasure rules</code> | Inline comment: treasure rules |
| 27 | <code>import logic.VanishLogic; // level 1 vanishing tiles</code> | Inline comment: level 1 vanishing tiles |
| 28 | <code>import ui.GameUI; // window controller</code> | Inline comment: window controller |
| 29 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 30 | <code>@SuppressWarnings("serial") // the panel is never serialized, so the warning is harmless</code> | Inline comment: the panel is never serialized, so the warning is harmless |
| 31 | <code>public final class TreasureMaze extends JPanel { // the game screen: input, rules and drawing</code> | Inline comment: the game screen: input, rules and drawing |
| 32 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 33 | <code>    private static final long serialVersionUID = 1L; // version id for serialization</code> | Inline comment: version id for serialization |
| 34 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 35 | <code>    private static final long MOVE_COOLDOWN = 90; // minimum milliseconds between moves</code> | Inline comment: minimum milliseconds between moves |
| 36 | <code>    private static final int LEVEL3_MIN_TILE = 32; // smallest tile size on level 3 (zoom)</code> | Inline comment: smallest tile size on level 3 (zoom) |
| 37 | <code>    private static final int HUD_SPACE = 80; // pixels reserved at the top for the HUD</code> | Inline comment: pixels reserved at the top for the HUD |
| 38 | <code>    // Move budget = (shortest safe route start -&gt; treasure -&gt; exit) * factor + slack  (the ORIGINAL formula)...</code> | Inline comment: Move budget = (shortest safe route start -&gt; treasure -&gt; exit) * factor + slack  (the ORIGINAL formula)... |
| 39 | <code>    private static final double[] MOVE_FACTOR = {3.0, 2.2, 1.8}; // route multiplier per level (unchanged)</code> | Inline comment: route multiplier per level (unchanged) |
| 40 | <code>    private static final int MOVE_SLACK = 10; // extra moves added to every budget (unchanged)</code> | Inline comment: extra moves added to every budget (unchanged) |
| 41 | <code>    // Level 1 keeps the formula above. Levels 2 and 3 use fixed starting limits instead.</code> | Inline comment: Level 1 keeps the formula above. Levels 2 and 3 use fixed starting limits instead. |
| 42 | <code>    private static final int[] FIXED_MOVES = {0, 725, 888}; // 0 = use the formula (level 1)</code> | Inline comment: 0 = use the formula (level 1) |
| 43 | <code>    private static final int[] MIMIC_DAMAGE = {1, 2, 1}; // hearts a mimic takes per catch on levels 1, 2, 3</code> | Inline comment: hearts a mimic takes per catch on levels 1, 2, 3 |
| 44 | <code>    private static final long CATCH_STUN_MILLIS = 2500; // the mimic that caught the player freezes this long, so one catch hurts once</code> | Inline comment: the mimic that caught the player freezes this long, so one catch hurts once |
| 45 | <code>    private static final long BLUE_STUN_MILLIS = 3500; // a blue bomb stuns mimics for exactly 3.5 seconds</code> | Inline comment: a blue bomb stuns mimics for exactly 3.5 seconds |
| 46 | <code>    private static final int BLUE_RADIUS = 4; // blast radius of a blue bomb in cells</code> | Inline comment: blast radius of a blue bomb in cells |
| 47 | <code>    private static final int COIN_MOVE_BONUS = 67; // moves added to the remaining allowance for each coin</code> | Inline comment: moves added to the remaining allowance for each coin |
| 48 | <code>    private static final long ESCAPE_MILLIS = 5000; // the five-second escape countdown</code> | Inline comment: the five-second escape countdown |
| 49 | <code>    private static final int HEARING_RANGE = 12; // mimic footsteps are audible within this many cells</code> | Inline comment: mimic footsteps are audible within this many cells |
| 50 | <code>    private static final long EXPLOSION_MILLIS = 600; // how long the explosion animation lasts</code> | Inline comment: how long the explosion animation lasts |
| 51 | <code>    private static final int NOISE_RANGE = 12; // path steps over which walking near the chest is heard</code> | Inline comment: path steps over which walking near the chest is heard |
| 52 | <code>    private static final int CHEST_NOISE_RANGE = 20; // path steps over which the chest opening is heard</code> | Inline comment: path steps over which the chest opening is heard |
| 53 | <code>    private static final int TICK_MILLIS = 50; // game timer period (countdown, vanishing tiles, animation)</code> | Inline comment: game timer period (countdown, vanishing tiles, animation) |
| 54 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 55 | <code>    /** A bomb explosion animation playing on a cell. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 56 | <code>    private static final class Explosion { // data holder</code> | Inline comment: data holder |
| 57 | <code>        final int x; // cell column</code> | Inline comment: cell column |
| 58 | <code>        final int y; // cell row</code> | Inline comment: cell row |
| 59 | <code>        final long start; // time it started</code> | Inline comment: time it started |
| 60 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 61 | <code>        Explosion(int x, int y, long start) { // create one</code> | Inline comment: create one |
| 62 | <code>            this.x = x; // store column</code> | Inline comment: store column |
| 63 | <code>            this.y = y; // store row</code> | Inline comment: store row |
| 64 | <code>            this.start = start; // store start time</code> | Inline comment: store start time |
| 65 | <code>        } // end of constructor</code> | Inline comment: end of constructor |
| 66 | <code>    } // end of Explosion</code> | Inline comment: end of Explosion |
| 67 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 68 | <code>    private final Player player = new Player(1, 1); // the player</code> | Inline comment: the player |
| 69 | <code>    private final LevelManager levelManager = new LevelManager(); // active level tracker (generates this game's maps)</code> | Inline comment: active level tracker (generates this game's maps) |
| 70 | <code>    private final BombLogic bombSystem = new BombLogic(); // bombs</code> | Inline comment: bombs |
| 71 | <code>    private final TreasureLogic treasureSystem = new TreasureLogic(); // treasure chest</code> | Inline comment: treasure chest |
| 72 | <code>    private final CoinLogic coinSystem = new CoinLogic(); // coins</code> | Inline comment: coins |
| 73 | <code>    private final MimicLogic mimicSystem = new MimicLogic(); // mimics</code> | Inline comment: mimics |
| 74 | <code>    private final VanishLogic vanishSystem = new VanishLogic(); // vanishing tiles (level 1 only)</code> | Inline comment: vanishing tiles (level 1 only) |
| 75 | <code>    private final ScoreManager scoreManager = new ScoreManager(); // score and lives</code> | Inline comment: score and lives |
| 76 | <code>    private final Sprites sprites = new Sprites(); // pixel-art pictures</code> | Inline comment: pixel-art pictures |
| 77 | <code>    private final SoundManager sound; // sound effects and music</code> | Inline comment: sound effects and music |
| 78 | <code>    private final GameUI gameUI; // used to show the result screen</code> | Inline comment: used to show the result screen |
| 79 | <code>    private final List&lt;Explosion&gt; explosions = new ArrayList&lt;&gt;(); // explosions currently animating</code> | Inline comment: explosions currently animating |
| 80 | <code>    private final Timer gameTimer; // drives the countdown, tiles and animation</code> | Inline comment: drives the countdown, tiles and animation |
| 81 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 82 | <code>    private String[] currentMap; // map of the active level</code> | Inline comment: map of the active level |
| 83 | <code>    private Point startPos = new Point(1, 1); // player start cell</code> | Inline comment: player start cell |
| 84 | <code>    private Point exitPos; // exit cell</code> | Inline comment: exit cell |
| 85 | <code>    private int movesLeft; // remaining move budget</code> | Inline comment: remaining move budget |
| 86 | <code>    private boolean finished; // true once the game ended</code> | Inline comment: true once the game ended |
| 87 | <code>    private boolean dying; // true during the five-second countdown (health is zero)</code> | Inline comment: true during the five-second countdown (health is zero) |
| 88 | <code>    private long dyingEnd; // time when the countdown runs out</code> | Inline comment: time when the countdown runs out |
| 89 | <code>    private int lastTickSecond = -1; // last whole second that played the tick sound</code> | Inline comment: last whole second that played the tick sound |
| 90 | <code>    private long lastMoveTime = 0; // time of the last accepted move</code> | Inline comment: time of the last accepted move |
| 91 | <code>    private long flashUntil = 0; // red damage flash lasts until this time</code> | Inline comment: red damage flash lasts until this time |
| 92 | <code>    private boolean facingLeft; // player sprite is mirrored when walking left</code> | Inline comment: player sprite is mirrored when walking left |
| 93 | <code>    private boolean stepParity; // alternates each step for the walking animation</code> | Inline comment: alternates each step for the walking animation |
| 94 | <code>    private String statusMessage = ""; // text shown in the HUD</code> | Inline comment: text shown in the HUD |
| 95 | <code>    private int blueBombs; // blue Mimic-trap bombs the player carries (a plain counter, no inventory)</code> | Inline comment: blue Mimic-trap bombs the player carries (a plain counter, no inventory) |
| 96 | <code>    private int blueAtLevelStart; // carried amount when the level began (a level restart gives back exactly this)</code> | Inline comment: carried amount when the level began (a level restart gives back exactly this) |
| 97 | <code>    private boolean paused; // true while the pause menu is open: everything is frozen</code> | Inline comment: true while the pause menu is open: everything is frozen |
| 98 | <code>    private boolean confirmQuit; // true while the quit confirmation is shown</code> | Inline comment: true while the quit confirmation is shown |
| 99 | <code>    private int pauseIndex; // pause menu cursor: 0 Resume, 1 Main Menu, 2 Quit Game</code> | Inline comment: pause menu cursor: 0 Resume, 1 Main Menu, 2 Quit Game |
| 100 | <code>    private long pausedAt; // real time when the current pause began</code> | Inline comment: real time when the current pause began |
| 101 | <code>    private long pausedTotal; // total real time spent paused (subtracted by clock())</code> | Inline comment: total real time spent paused (subtracted by clock()) |
| 102 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 103 | <code>    public TreasureMaze(GameUI gameUI, SoundManager sound) { // build the game panel</code> | Inline comment: build the game panel |
| 104 | <code>        this.gameUI = gameUI; // keep the window controller</code> | Inline comment: keep the window controller |
| 105 | <code>        this.sound = sound != null ? sound : new SoundManager(false); // silent manager when none is given</code> | Inline comment: silent manager when none is given |
| 106 | <code>        setBackground(new Color(10, 10, 14)); // near-black background</code> | Inline comment: near-black background |
| 107 | <code>        setFocusable(true); // allow keyboard focus</code> | Inline comment: allow keyboard focus |
| 108 | <code>        setFocusTraversalKeysEnabled(false); // let Tab and others reach the key handler</code> | Inline comment: let Tab and others reach the key handler |
| 109 | <code>        addKeyListener(new KeyAdapter() { // listen for keys without passing this directly</code> | Inline comment: listen for keys without passing this directly |
| 110 | <code>            @Override // replaces the empty adapter method</code> | Inline comment: replaces the empty adapter method |
| 111 | <code>            public void keyPressed(KeyEvent e) { // a key went down</code> | Inline comment: a key went down |
| 112 | <code>                handleKeyPressed(e); // delegate to the game logic</code> | Inline comment: delegate to the game logic |
| 113 | <code>            } // end of keyPressed</code> | Inline comment: end of keyPressed |
| 114 | <code>        }); // end of key listener</code> | Inline comment: end of key listener |
| 115 | <code>        addMouseListener(new MouseAdapter() { // listen for mouse clicks</code> | Inline comment: listen for mouse clicks |
| 116 | <code>            @Override // replaces the empty adapter method</code> | Inline comment: replaces the empty adapter method |
| 117 | <code>            public void mouseClicked(MouseEvent e) { // panel was clicked</code> | Inline comment: panel was clicked |
| 118 | <code>                requestFocusInWindow(); // regain keyboard focus</code> | Inline comment: regain keyboard focus |
| 119 | <code>            } // end of mouseClicked</code> | Inline comment: end of mouseClicked |
| 120 | <code>        }); // end of mouse listener</code> | Inline comment: end of mouse listener |
| 121 | <code>        gameTimer = new Timer(TICK_MILLIS, e -&gt; onTick()); // create the periodic timer</code> | Inline comment: create the periodic timer |
| 122 | <code>        // the timer is started by resetGameToLevel1() when a game begins and stopped when the game ends or the player leaves</code> | Inline comment: the timer is started by resetGameToLevel1() when a game begins and stopped when the game ends or the player leaves |
| 123 | <code>        loadCurrentLevel(); // load level 1</code> | Inline comment: load level 1 |
| 124 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 125 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 126 | <code>    public void resetGameToLevel1() { // start a new game</code> | Inline comment: start a new game |
| 127 | <code>        levelManager.resetToLevel1(); // back to level 1 with brand-new random maps</code> | Inline comment: back to level 1 with brand-new random maps |
| 128 | <code>        scoreManager.resetScore(); // reset score and lives</code> | Inline comment: reset score and lives |
| 129 | <code>        finished = false; // game is running again</code> | Inline comment: game is running again |
| 130 | <code>        dying = false; // no countdown</code> | Inline comment: no countdown |
| 131 | <code>        paused = false; // never start paused</code> | Inline comment: never start paused |
| 132 | <code>        blueAtLevelStart = 0; // a new game starts with no blue bombs</code> | Inline comment: a new game starts with no blue bombs |
| 133 | <code>        confirmQuit = false; // no quit prompt</code> | Inline comment: no quit prompt |
| 134 | <code>        if (!gameTimer.isRunning()) gameTimer.start(); // one timer only: start it only if it is not running</code> | Inline comment: one timer only: start it only if it is not running |
| 135 | <code>        loadCurrentLevel(); // load the level</code> | Inline comment: load the level |
| 136 | <code>        repaint(); // redraw</code> | Inline comment: redraw |
| 137 | <code>    } // end of resetGameToLevel1</code> | Inline comment: end of resetGameToLevel1 |
| 138 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 139 | <code>    private void loadCurrentLevel() { // set up the active level</code> | Inline comment: set up the active level |
| 140 | <code>        int level = levelManager.getCurrentLevel(); // level number</code> | Inline comment: level number |
| 141 | <code>        currentMap = levelManager.getCurrentMap(); // level map</code> | Inline comment: level map |
| 142 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 143 | <code>        Point p = GameMap.find(currentMap, 'P'); // start marker</code> | Inline comment: start marker |
| 144 | <code>        startPos = p != null ? p : new Point(1, 1); // fall back to (1,1)</code> | Inline comment: fall back to (1,1) |
| 145 | <code>        exitPos = GameMap.find(currentMap, 'E'); // exit marker</code> | Inline comment: exit marker |
| 146 | <code>        player.setPosition(startPos.x, startPos.y); // place the player</code> | Inline comment: place the player |
| 147 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 148 | <code>        bombSystem.loadBombsFromMap(currentMap); // load bombs</code> | Inline comment: load bombs |
| 149 | <code>        treasureSystem.loadTreasureFromMap(currentMap); // load treasure</code> | Inline comment: load treasure |
| 150 | <code>        coinSystem.loadCoinsFromMap(currentMap); // load coins</code> | Inline comment: load coins |
| 151 | <code>        mimicSystem.loadMimicsFromMap(currentMap, level); // load mimics</code> | Inline comment: load mimics |
| 152 | <code>        vanishSystem.load(level == 1, exitPos, treasureSystem.getPosition(), startPos); // vanishing tiles on level 1 only</code> | Inline comment: vanishing tiles on level 1 only |
| 153 | <code>        explosions.clear(); // no old explosions</code> | Inline comment: no old explosions |
| 154 | <code>        blueBombs = blueAtLevelStart; // carried blue bombs (picked-up pickups come back on a restart, so go back to the amount at level start)</code> | Inline comment: carried blue bombs (picked-up pickups come back on a restart, so go back to the amount at level start) |
| 155 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 156 | <code>        movesLeft = computeMoveBudget(level); // set the move limit</code> | Inline comment: set the move limit |
| 157 | <code>        int coins = coinSystem.getTotal(); // coins in this level</code> | Inline comment: coins in this level |
| 158 | <code>        if (level == 1) { // beginner-friendly hint for the calm first level</code> | Inline comment: beginner-friendly hint for the calm first level |
| 159 | <code>            statusMessage = "Level 1: chest or all " + coins + " coins opens the exit. Tiles crumble!"; // intro</code> | Inline comment: intro |
| 160 | <code>        } else { // later levels</code> | Inline comment: later levels |
| 161 | <code>            statusMessage = "Level " + level + ": chest or all " + coins + " coins opens the exit. Beware mimics!"; // intro</code> | Inline comment: intro |
| 162 | <code>        } // end of intro choice</code> | Inline comment: end of intro choice |
| 163 | <code>    } // end of loadCurrentLevel</code> | Inline comment: end of loadCurrentLevel |
| 164 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 165 | <code>    private int computeMoveBudget(int level) { // move limit for a level</code> | Inline comment: move limit for a level |
| 166 | <code>        Point t = treasureSystem.getPosition(); // treasure cell</code> | Inline comment: treasure cell |
| 167 | <code>        if (t == null &#124;&#124; exitPos == null) { // incomplete map</code> | Inline comment: incomplete map |
| 168 | <code>            return 9999; // effectively unlimited</code> | Inline comment: effectively unlimited |
| 169 | <code>        } // end of check</code> | Inline comment: end of check |
| 170 | <code>        int toTreasure = Pathfinder.distance(currentMap, startPos, t, true); // safe steps start -&gt; treasure</code> | Inline comment: safe steps start -&gt; treasure |
| 171 | <code>        int toExit = Pathfinder.distance(currentMap, t, exitPos, true); // safe steps treasure -&gt; exit</code> | Inline comment: safe steps treasure -&gt; exit |
| 172 | <code>        if (toTreasure &lt; 0 &#124;&#124; toExit &lt; 0) { // no safe route</code> | Inline comment: no safe route |
| 173 | <code>            return 9999; // effectively unlimited</code> | Inline comment: effectively unlimited |
| 174 | <code>        } // end of check</code> | Inline comment: end of check |
| 175 | <code>        int idx = Math.min(level, MOVE_FACTOR.length) - 1; // 0-based level index</code> | Inline comment: 0-based level index |
| 176 | <code>        int original = (int) Math.ceil((toTreasure + toExit) * MOVE_FACTOR[idx]) + MOVE_SLACK; // the original budget</code> | Inline comment: the original budget |
| 177 | <code>        return FIXED_MOVES[idx] &gt; 0 ? FIXED_MOVES[idx] : original; // level 1: the formula; level 2: 725; level 3: 888</code> | Inline comment: level 1: the formula; level 2: 725; level 3: 888 |
| 178 | <code>    } // end of computeMoveBudget</code> | Inline comment: end of computeMoveBudget |
| 179 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 180 | <code>    private boolean isExitUnlocked() { // the exit opens with the chest OR with every coin</code> | Inline comment: the exit opens with the chest OR with every coin |
| 181 | <code>        return treasureSystem.isClaimed() &#124;&#124; coinSystem.isComplete(); // either way unlocks it</code> | Inline comment: either way unlocks it |
| 182 | <code>    } // end of isExitUnlocked</code> | Inline comment: end of isExitUnlocked |
| 183 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 184 | <code>    /**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 185 | <code>     * Applies damage. Normal damage removes one heart; lethal damage (level 1 bombs) removes all hearts.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 186 | <code>     * When health reaches zero the five-second escape countdown starts - exactly once.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 187 | <code>     * While the countdown runs, further damage is ignored so nothing can restart it or end the game early.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 188 | <code>     */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 189 | <code>    private void damagePlayer(String message, boolean lethal) { // one-heart damage (bombs, out of moves)</code> | Inline comment: one-heart damage (bombs, out of moves) |
| 190 | <code>        damagePlayer(message, lethal, 1); // the usual single heart</code> | Inline comment: the usual single heart |
| 191 | <code>    } // end of damagePlayer</code> | Inline comment: end of damagePlayer |
| 192 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 193 | <code>    private void damagePlayer(String message, boolean lethal, int hearts) { // central damage rule: hearts = how many hearts to remove</code> | Inline comment: central damage rule: hearts = how many hearts to remove |
| 194 | <code>        if (dying &#124;&#124; finished) return; // already at zero health: ignore</code> | Inline comment: already at zero health: ignore |
| 195 | <code>        boolean empty = false; // is health empty after the damage</code> | Inline comment: is health empty after the damage |
| 196 | <code>        if (lethal) { // level 1 bombs</code> | Inline comment: level 1 bombs |
| 197 | <code>            empty = scoreManager.eliminate(); // all hearts at once</code> | Inline comment: all hearts at once |
| 198 | <code>        } else { // normal damage</code> | Inline comment: normal damage |
| 199 | <code>            for (int i = 0; i &lt; hearts; i++) empty = scoreManager.loseLife(); // remove the hearts one by one (never below zero)</code> | Inline comment: remove the hearts one by one (never below zero) |
| 200 | <code>        } // end of damage kind</code> | Inline comment: end of damage kind |
| 201 | <code>        statusMessage = message; // tell the player what happened</code> | Inline comment: tell the player what happened |
| 202 | <code>        flashUntil = clock() + 250; // red screen flash</code> | Inline comment: red screen flash |
| 203 | <code>        if (empty) { // no hearts left</code> | Inline comment: no hearts left |
| 204 | <code>            beginCountdown(); // start the escape timer</code> | Inline comment: start the escape timer |
| 205 | <code>        } // end of empty check</code> | Inline comment: end of empty check |
| 206 | <code>    } // end of damagePlayer</code> | Inline comment: end of damagePlayer |
| 207 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 208 | <code>    private void beginCountdown() { // start the five-second escape timer</code> | Inline comment: start the five-second escape timer |
| 209 | <code>        if (dying &#124;&#124; finished) return; // never start twice</code> | Inline comment: never start twice |
| 210 | <code>        dying = true; // countdown is running</code> | Inline comment: countdown is running |
| 211 | <code>        dyingEnd = clock() + ESCAPE_MILLIS; // when it expires</code> | Inline comment: when it expires |
| 212 | <code>        lastTickSecond = -1; // so the first tick sound plays immediately</code> | Inline comment: so the first tick sound plays immediately |
| 213 | <code>        statusMessage = "NO HEARTS! Reach the open exit in time!"; // instruction</code> | Inline comment: instruction |
| 214 | <code>    } // end of beginCountdown</code> | Inline comment: end of beginCountdown |
| 215 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 216 | <code>    private void finish(boolean won) { // end the game</code> | Inline comment: end the game |
| 217 | <code>        finished = true; // ignore further input</code> | Inline comment: ignore further input |
| 218 | <code>        gameTimer.stop(); // the game is over: stop updating</code> | Inline comment: the game is over: stop updating |
| 219 | <code>        dying = false; // countdown (if any) is over</code> | Inline comment: countdown (if any) is over |
| 220 | <code>        repaint(); // redraw once</code> | Inline comment: redraw once |
| 221 | <code>        if (gameUI != null) { // controller available</code> | Inline comment: controller available |
| 222 | <code>            gameUI.showResults(scoreManager.getTotalScore(), won); // show the result screen</code> | Inline comment: show the result screen |
| 223 | <code>        } // end of null check</code> | Inline comment: end of null check |
| 224 | <code>    } // end of finish</code> | Inline comment: end of finish |
| 225 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 226 | <code>    /** Runs every TICK_MILLIS on the UI thread: countdown, vanishing tiles, animation. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 227 | <code>    private void onTick() { // timer callback</code> | Inline comment: timer callback |
| 228 | <code>        if (finished &#124;&#124; paused) return; // nothing to update after the game ended or while paused</code> | Inline comment: nothing to update after the game ended or while paused |
| 229 | <code>        long now = clock(); // current time</code> | Inline comment: current time |
| 230 | <code>        boolean changed = vanishSystem.update(now); // tiles crumble, vanish and regrow</code> | Inline comment: tiles crumble, vanish and regrow |
| 231 | <code>        int stepped = mimicSystem.update(player.getX(), player.getY(), currentMap, now); // mimics act on their own, no key needed</code> | Inline comment: mimics act on their own, no key needed |
| 232 | <code>        if (stepped &gt; 0) { // a mimic moved</code> | Inline comment: a mimic moved |
| 233 | <code>            changed = true; // redraw</code> | Inline comment: redraw |
| 234 | <code>            if (mimicSystem.anyWithin(player.getX(), player.getY(), HEARING_RANGE)) sound.play("mimic_step"); // footstep if close enough to hear</code> | Inline comment: footstep if close enough to hear |
| 235 | <code>            checkMimicCatch(); // a mimic may walk into the player</code> | Inline comment: a mimic may walk into the player |
| 236 | <code>            if (finished) return; // the catch ended the game</code> | Inline comment: the catch ended the game |
| 237 | <code>        } // end of mimic step</code> | Inline comment: end of mimic step |
| 238 | <code>        for (Point trap : new ArrayList&lt;&gt;(bombSystem.getTraps())) { // each placed blue bomb</code> | Inline comment: each placed blue bomb |
| 239 | <code>            if (mimicSystem.triggerTrap(trap.x, trap.y, now, BLUE_RADIUS, BLUE_STUN_MILLIS)) { // a mimic came next to it</code> | Inline comment: a mimic came next to it |
| 240 | <code>                bombSystem.removeTrap(trap); // it explodes only once</code> | Inline comment: it explodes only once |
| 241 | <code>                explosions.add(new Explosion(trap.x, trap.y, now)); // explosion animation</code> | Inline comment: explosion animation |
| 242 | <code>                sound.play("bomb"); // explosion sound</code> | Inline comment: explosion sound |
| 243 | <code>                mimicSystem.hearNoise(trap.x, trap.y, currentMap, NOISE_RANGE); // mimics outside the blast that are not stunned come to look (the noise position, not the player)</code> | Inline comment: mimics outside the blast that are not stunned come to look (the noise position, not the player) |
| 244 | <code>                statusMessage = "Blue bomb! Nearby mimics are stunned for 3.5 s."; // tell the player</code> | Inline comment: tell the player |
| 245 | <code>                changed = true; // redraw</code> | Inline comment: redraw |
| 246 | <code>            } // end of trigger</code> | Inline comment: end of trigger |
| 247 | <code>        } // end of traps</code> | Inline comment: end of traps |
| 248 | <code>        if (dying) { // countdown running</code> | Inline comment: countdown running |
| 249 | <code>            long remaining = dyingEnd - now; // milliseconds left</code> | Inline comment: milliseconds left |
| 250 | <code>            if (remaining &lt;= 0) { // time is up</code> | Inline comment: time is up |
| 251 | <code>                finish(false); // the game ends (exactly once: finish() clears dying)</code> | Inline comment: the game ends (exactly once: finish() clears dying) |
| 252 | <code>                return; // stop here</code> | Inline comment: stop here |
| 253 | <code>            } // end of expiry check</code> | Inline comment: end of expiry check |
| 254 | <code>            int secondsLeft = (int) Math.ceil(remaining / 1000.0); // whole seconds left</code> | Inline comment: whole seconds left |
| 255 | <code>            if (secondsLeft != lastTickSecond) { // a new second started</code> | Inline comment: a new second started |
| 256 | <code>                lastTickSecond = secondsLeft; // remember it</code> | Inline comment: remember it |
| 257 | <code>                sound.play("tick"); // tick once per second</code> | Inline comment: tick once per second |
| 258 | <code>            } // end of second check</code> | Inline comment: end of second check |
| 259 | <code>            changed = true; // redraw the countdown text</code> | Inline comment: redraw the countdown text |
| 260 | <code>        } // end of countdown</code> | Inline comment: end of countdown |
| 261 | <code>        explosions.removeIf(ex -&gt; now - ex.start &gt; EXPLOSION_MILLIS); // drop finished explosions</code> | Inline comment: drop finished explosions |
| 262 | <code>        if (!explosions.isEmpty() &#124;&#124; now &lt; flashUntil &#124;&#124; now - lastMoveTime &lt; 250) { // animations running</code> | Inline comment: animations running |
| 263 | <code>            changed = true; // redraw</code> | Inline comment: redraw |
| 264 | <code>        } // end of animation check</code> | Inline comment: end of animation check |
| 265 | <code>        if (changed &amp;&amp; isShowing()) { // only draw when the game screen is visible</code> | Inline comment: only draw when the game screen is visible |
| 266 | <code>            repaint(); // redraw</code> | Inline comment: redraw |
| 267 | <code>        } // end of repaint check</code> | Inline comment: end of repaint check |
| 268 | <code>    } // end of onTick</code> | Inline comment: end of onTick |
| 269 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 270 | <code>    // ------------------------------------------------------------------ input</code> | Inline comment: ------------------------------------------------------------------ input |
| 271 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 272 | <code>    void handleKeyPressed(KeyEvent e) { // process one key press</code> | Inline comment: process one key press |
| 273 | <code>        if (finished) return; // game already ended</code> | Inline comment: game already ended |
| 274 | <code>        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { // Esc opens or closes the pause menu</code> | Inline comment: Esc opens or closes the pause menu |
| 275 | <code>            if (confirmQuit) confirmQuit = false; // Esc cancels the quit prompt first</code> | Inline comment: Esc cancels the quit prompt first |
| 276 | <code>            else setPaused(!paused); // otherwise pause / resume</code> | Inline comment: otherwise pause / resume |
| 277 | <code>            repaint(); // redraw</code> | Inline comment: redraw |
| 278 | <code>            return; // done</code> | Inline comment: done |
| 279 | <code>        } // end of Esc</code> | Inline comment: end of Esc |
| 280 | <code>        if (paused) { // the pause menu is open</code> | Inline comment: the pause menu is open |
| 281 | <code>            handlePauseKey(e); // menu navigation only: the game stays frozen</code> | Inline comment: menu navigation only: the game stays frozen |
| 282 | <code>            return; // never reach the movement code</code> | Inline comment: never reach the movement code |
| 283 | <code>        } // end of paused</code> | Inline comment: end of paused |
| 284 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 285 | <code>        int dx = 0; // horizontal step</code> | Inline comment: horizontal step |
| 286 | <code>        int dy = 0; // vertical step</code> | Inline comment: vertical step |
| 287 | <code>        switch (e.getKeyCode()) { // which key</code> | Inline comment: which key |
| 288 | <code>            case KeyEvent.VK_UP, KeyEvent.VK_W -&gt; dy = -1; // up</code> | Inline comment: up |
| 289 | <code>            case KeyEvent.VK_DOWN, KeyEvent.VK_S -&gt; dy = 1; // down</code> | Inline comment: down |
| 290 | <code>            case KeyEvent.VK_LEFT, KeyEvent.VK_A -&gt; dx = -1; // left</code> | Inline comment: left |
| 291 | <code>            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -&gt; dx = 1; // right</code> | Inline comment: right |
| 292 | <code>            case KeyEvent.VK_R -&gt; { // restart key</code> | Inline comment: restart key |
| 293 | <code>                restartLevelWithPenalty(); // restart, costs a heart</code> | Inline comment: restart, costs a heart |
| 294 | <code>                return; // done</code> | Inline comment: done |
| 295 | <code>            } // end of R case</code> | Inline comment: end of R case |
| 296 | <code>            case KeyEvent.VK_T -&gt; { // open the chest next to the player</code> | Inline comment: open the chest next to the player |
| 297 | <code>                interactWithChest(); // real chest: open it, fake chest: reveal the mimic</code> | Inline comment: real chest: open it, fake chest: reveal the mimic |
| 298 | <code>                return; // done</code> | Inline comment: done |
| 299 | <code>            } // end of T case</code> | Inline comment: end of T case |
| 300 | <code>            case KeyEvent.VK_SPACE -&gt; { // place a blue bomb</code> | Inline comment: place a blue bomb |
| 301 | <code>                placeBlueBomb(); // uses one carried blue bomb</code> | Inline comment: uses one carried blue bomb |
| 302 | <code>                return; // done</code> | Inline comment: done |
| 303 | <code>            } // end of SPACE case</code> | Inline comment: end of SPACE case |
| 304 | <code>            case KeyEvent.VK_M -&gt; { // mute key</code> | Inline comment: mute key |
| 305 | <code>                boolean muted = sound.toggleMute(); // flip sound on/off</code> | Inline comment: flip sound on/off |
| 306 | <code>                statusMessage = muted ? "Sound OFF (press M to turn it on)" : "Sound ON"; // feedback</code> | Inline comment: feedback |
| 307 | <code>                repaint(); // redraw</code> | Inline comment: redraw |
| 308 | <code>                return; // done</code> | Inline comment: done |
| 309 | <code>            } // end of M case</code> | Inline comment: end of M case |
| 310 | <code>            default -&gt; { // any other key</code> | Inline comment: any other key |
| 311 | <code>                return; // ignore</code> | Inline comment: ignore |
| 312 | <code>            } // end of default case</code> | Inline comment: end of default case |
| 313 | <code>        } // end of switch</code> | Inline comment: end of switch |
| 314 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 315 | <code>        long now = clock(); // current time</code> | Inline comment: current time |
| 316 | <code>        if (now - lastMoveTime &lt; MOVE_COOLDOWN) return; // too soon after the last move</code> | Inline comment: too soon after the last move |
| 317 | <code>        lastMoveTime = now; // remember this move time</code> | Inline comment: remember this move time |
| 318 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 319 | <code>        int tx = player.getX() + dx; // target column</code> | Inline comment: target column |
| 320 | <code>        int ty = player.getY() + dy; // target row</code> | Inline comment: target row |
| 321 | <code>        if (!Pathfinder.walkable(currentMap, tx, ty, false)) return; // wall or outside: no move</code> | Inline comment: wall or outside: no move |
| 322 | <code>        if (mimicSystem.isDisguisedAt(tx, ty)) { // walking into a chest cell is trying to open it, and it is a fake chest</code> | Inline comment: walking into a chest cell is trying to open it, and it is a fake chest |
| 323 | <code>            revealMimicChest(tx, ty); // the mimic shows itself (the player does not step onto the cell)</code> | Inline comment: the mimic shows itself (the player does not step onto the cell) |
| 324 | <code>            repaint(); // redraw</code> | Inline comment: redraw |
| 325 | <code>            return; // no move is spent</code> | Inline comment: no move is spent |
| 326 | <code>        } // end of fake chest</code> | Inline comment: end of fake chest |
| 327 | <code>        if (vanishSystem.isVanished(tx, ty)) { // the floor there has crumbled away</code> | Inline comment: the floor there has crumbled away |
| 328 | <code>            statusMessage = "That tile crumbled away - wait for it to grow back!"; // explain</code> | Inline comment: explain |
| 329 | <code>            repaint(); // redraw</code> | Inline comment: redraw |
| 330 | <code>            return; // blocked, and no move is spent</code> | Inline comment: blocked, and no move is spent |
| 331 | <code>        } // end of vanished check</code> | Inline comment: end of vanished check |
| 332 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 333 | <code>        if (dx &lt; 0) facingLeft = true; // walking left: mirror the sprite</code> | Inline comment: walking left: mirror the sprite |
| 334 | <code>        if (dx &gt; 0) facingLeft = false; // walking right: normal sprite</code> | Inline comment: walking right: normal sprite |
| 335 | <code>        int oldX = player.getX(); // cell being left</code> | Inline comment: cell being left |
| 336 | <code>        int oldY = player.getY(); // cell being left</code> | Inline comment: cell being left |
| 337 | <code>        player.move(dx, dy); // move the player</code> | Inline comment: move the player |
| 338 | <code>        stepParity = !stepParity; // alternate for the walking animation</code> | Inline comment: alternate for the walking animation |
| 339 | <code>        sound.play("step"); // footstep sound (once per accepted move)</code> | Inline comment: footstep sound (once per accepted move) |
| 340 | <code>        vanishSystem.onLeave(oldX, oldY, now); // the old tile starts to crumble (level 1)</code> | Inline comment: the old tile starts to crumble (level 1) |
| 341 | <code>        vanishSystem.onEnter(player.getX(), player.getY()); // a crumbling tile is saved while stood on</code> | Inline comment: a crumbling tile is saved while stood on |
| 342 | <code>        if (!dying) { // moves are not counted during the countdown</code> | Inline comment: moves are not counted during the countdown |
| 343 | <code>            movesLeft--; // spend a move</code> | Inline comment: spend a move |
| 344 | <code>        } // end of move count</code> | Inline comment: end of move count |
| 345 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 346 | <code>        if (!dying &amp;&amp; !treasureSystem.isClaimed() &amp;&amp; nearChest()) { // walking near the closed chest is noisy</code> | Inline comment: walking near the closed chest is noisy |
| 347 | <code>            mimicSystem.hearNoise(player.getX(), player.getY(), currentMap, NOISE_RANGE); // nearby mimics come to look</code> | Inline comment: nearby mimics come to look |
| 348 | <code>        } // end of chest noise</code> | Inline comment: end of chest noise |
| 349 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 350 | <code>        if (bombSystem.triggerAt(player.getX(), player.getY())) { // stepped on a bomb</code> | Inline comment: stepped on a bomb |
| 351 | <code>            sound.play("bomb"); // explosion sound</code> | Inline comment: explosion sound |
| 352 | <code>            explosions.add(new Explosion(player.getX(), player.getY(), now)); // explosion animation</code> | Inline comment: explosion animation |
| 353 | <code>            boolean lethal = levelManager.getCurrentLevel() == 1; // level 1 bombs eliminate the player</code> | Inline comment: level 1 bombs eliminate the player |
| 354 | <code>            damagePlayer(lethal ? "BOOM! Eliminated! Reach the exit!" : "BOOM! Hit a Bomb! (-1 Heart)", lethal); // apply damage</code> | Inline comment: apply damage |
| 355 | <code>        } // end of bomb check</code> | Inline comment: end of bomb check |
| 356 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 357 | <code>        checkMimicCatch(); // stepped into a mimic?</code> | Inline comment: stepped into a mimic? |
| 358 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 359 | <code>        if (bombSystem.collectBlueAt(player.getX(), player.getY())) { // walked over a blue bomb pickup</code> | Inline comment: walked over a blue bomb pickup |
| 360 | <code>            blueBombs++; // one more carried</code> | Inline comment: one more carried |
| 361 | <code>            sound.play("coin"); // pickup sound</code> | Inline comment: pickup sound |
| 362 | <code>            statusMessage = "Blue bomb collected! Carrying " + blueBombs + ". Press SPACE to place it."; // hint</code> | Inline comment: hint |
| 363 | <code>        } // end of blue pickup</code> | Inline comment: end of blue pickup |
| 364 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 365 | <code>        if (coinSystem.collectAt(player.getX(), player.getY())) { // picked up a coin</code> | Inline comment: picked up a coin |
| 366 | <code>            scoreManager.addCoinPoints(); // +100</code> | Inline comment: +100 |
| 367 | <code>            movesLeft += COIN_MOVE_BONUS; // +67 moves on top of what is left (each coin disappears when taken, so only once)</code> | Inline comment: +67 moves on top of what is left (each coin disappears when taken, so only once) |
| 368 | <code>            sound.play("coin"); // coin sound</code> | Inline comment: coin sound |
| 369 | <code>            statusMessage = "Coin " + coinSystem.getCollected() + "/" + coinSystem.getTotal() + " (+100, +" + COIN_MOVE_BONUS + " moves)"; // progress</code> | Inline comment: progress |
| 370 | <code>            if (coinSystem.isComplete() &amp;&amp; !treasureSystem.isClaimed()) { // last coin</code> | Inline comment: last coin |
| 371 | <code>                statusMessage = "All coins found (+100)! Exit Unlocked!"; // the alternative route is done</code> | Inline comment: the alternative route is done |
| 372 | <code>            } // end of last coin check</code> | Inline comment: end of last coin check |
| 373 | <code>        } // end of coin check</code> | Inline comment: end of coin check |
| 374 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 375 | <code>        Point chestCell = treasureSystem.getPosition(); // the real chest</code> | Inline comment: the real chest |
| 376 | <code>        if (chestCell != null &amp;&amp; player.getX() == chestCell.x &amp;&amp; player.getY() == chestCell.y) { // stepping onto the real chest opens it (the original control)</code> | Inline comment: stepping onto the real chest opens it (the original control) |
| 377 | <code>            openRealChest(); // reward and unlock</code> | Inline comment: reward and unlock |
| 378 | <code>        } // end of step-onto-chest</code> | Inline comment: end of step-onto-chest |
| 379 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 380 | <code>        if (isExitUnlocked() &amp;&amp; exitPos != null // exit is open and exists</code> | Inline comment: exit is open and exists |
| 381 | <code>            &amp;&amp; player.getX() == exitPos.x &amp;&amp; player.getY() == exitPos.y) { // player on the exit</code> | Inline comment: player on the exit |
| 382 | <code>            boolean escaped = dying; // remember whether this is an escape from the countdown</code> | Inline comment: remember whether this is an escape from the countdown |
| 383 | <code>            if (dying) { // escaped during the countdown</code> | Inline comment: escaped during the countdown |
| 384 | <code>                dying = false; // countdown cancelled</code> | Inline comment: countdown cancelled |
| 385 | <code>                scoreManager.restoreLife(); // reward: one heart back</code> | Inline comment: reward: one heart back |
| 386 | <code>            } // end of escape</code> | Inline comment: end of escape |
| 387 | <code>            scoreManager.addLevelClearBonus(levelManager.getCurrentLevel()); // level bonus</code> | Inline comment: level bonus |
| 388 | <code>            if (levelManager.nextLevel()) { // another level exists</code> | Inline comment: another level exists |
| 389 | <code>                blueAtLevelStart = blueBombs; // carried blue bombs go with the player</code> | Inline comment: carried blue bombs go with the player |
| 390 | <code>                loadCurrentLevel(); // load it</code> | Inline comment: load it |
| 391 | <code>                if (escaped) { // keep the escape message visible</code> | Inline comment: keep the escape message visible |
| 392 | <code>                    statusMessage = "ESCAPED! One heart restored. " + statusMessage; // celebrate</code> | Inline comment: celebrate |
| 393 | <code>                } // end of message</code> | Inline comment: end of message |
| 394 | <code>            } else { // that was the last level</code> | Inline comment: that was the last level |
| 395 | <code>                finish(true); // win</code> | Inline comment: win |
| 396 | <code>                return; // done</code> | Inline comment: done |
| 397 | <code>            } // end of next-level check</code> | Inline comment: end of next-level check |
| 398 | <code>        } else if (movesLeft &lt;= 0 &amp;&amp; !dying) { // out of moves</code> | Inline comment: out of moves |
| 399 | <code>            damagePlayer("OUT OF MOVES! (-1 Heart) Level restarted.", false); // lose a heart</code> | Inline comment: lose a heart |
| 400 | <code>            if (!dying) { // still alive</code> | Inline comment: still alive |
| 401 | <code>                loadCurrentLevel(); // restart the level</code> | Inline comment: restart the level |
| 402 | <code>                statusMessage = "OUT OF MOVES! (-1 Heart) Level restarted."; // message</code> | Inline comment: message |
| 403 | <code>            } // end of alive check</code> | Inline comment: end of alive check |
| 404 | <code>        } // end of exit/moves check</code> | Inline comment: end of exit/moves check |
| 405 | <code>        repaint(); // redraw</code> | Inline comment: redraw |
| 406 | <code>    } // end of handleKeyPressed</code> | Inline comment: end of handleKeyPressed |
| 407 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 408 | <code>    /** Game time in ms. It does not advance while the game is paused, so countdowns, tiles and mimics freeze. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 409 | <code>    private long clock() { // the game clock</code> | Inline comment: the game clock |
| 410 | <code>        long real = paused ? pausedAt : System.currentTimeMillis(); // while paused, time stands still at the moment the pause began</code> | Inline comment: while paused, time stands still at the moment the pause began |
| 411 | <code>        return real - pausedTotal; // minus all earlier paused time</code> | Inline comment: minus all earlier paused time |
| 412 | <code>    } // end of clock</code> | Inline comment: end of clock |
| 413 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 414 | <code>    private void setPaused(boolean pause) { // pause or resume (never creates a timer: the one game timer just skips its work)</code> | Inline comment: pause or resume (never creates a timer: the one game timer just skips its work) |
| 415 | <code>        if (pause == paused &#124;&#124; finished) return; // already in that state</code> | Inline comment: already in that state |
| 416 | <code>        long real = System.currentTimeMillis(); // real time</code> | Inline comment: real time |
| 417 | <code>        if (pause) { // pausing</code> | Inline comment: pausing |
| 418 | <code>            pausedAt = real; // remember when</code> | Inline comment: remember when |
| 419 | <code>            pauseIndex = 0; // cursor on Resume</code> | Inline comment: cursor on Resume |
| 420 | <code>            confirmQuit = false; // no pending quit prompt</code> | Inline comment: no pending quit prompt |
| 421 | <code>        } else { // resuming</code> | Inline comment: resuming |
| 422 | <code>            pausedTotal += real - pausedAt; // the paused time is cut out of the game clock</code> | Inline comment: the paused time is cut out of the game clock |
| 423 | <code>        } // end of toggle</code> | Inline comment: end of toggle |
| 424 | <code>        paused = pause; // new state</code> | Inline comment: new state |
| 425 | <code>        if (pause) sound.pauseMusic(); else sound.resumeMusic(); // music pauses and resumes with the game (mute setting is kept)</code> | Inline comment: music pauses and resumes with the game (mute setting is kept) |
| 426 | <code>        repaint(); // show or hide the menu</code> | Inline comment: show or hide the menu |
| 427 | <code>    } // end of setPaused</code> | Inline comment: end of setPaused |
| 428 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 429 | <code>    /** Stops the game timer and pause state. Called when leaving the game for the main menu. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 430 | <code>    public void stopGame() { // end this game session</code> | Inline comment: end this game session |
| 431 | <code>        gameTimer.stop(); // no more updates (a new game restarts the timer)</code> | Inline comment: no more updates (a new game restarts the timer) |
| 432 | <code>        paused = false; // not paused any more</code> | Inline comment: not paused any more |
| 433 | <code>        sound.resumeMusic(); // the main menu has its music (a single clip: no duplicate playback)</code> | Inline comment: the main menu has its music (a single clip: no duplicate playback) |
| 434 | <code>        confirmQuit = false; // no prompt</code> | Inline comment: no prompt |
| 435 | <code>    } // end of stopGame</code> | Inline comment: end of stopGame |
| 436 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 437 | <code>    private void handlePauseKey(KeyEvent e) { // keys while the pause menu is open</code> | Inline comment: keys while the pause menu is open |
| 438 | <code>        int key = e.getKeyCode(); // which key</code> | Inline comment: which key |
| 439 | <code>        if (confirmQuit) { // asking "really quit?"</code> | Inline comment: asking "really quit?" |
| 440 | <code>            if (key == KeyEvent.VK_ENTER &#124;&#124; key == KeyEvent.VK_Y) System.exit(0); // yes: close the program</code> | Inline comment: yes: close the program |
| 441 | <code>            if (key == KeyEvent.VK_N) confirmQuit = false; // no: back to the menu</code> | Inline comment: no: back to the menu |
| 442 | <code>            repaint(); // redraw</code> | Inline comment: redraw |
| 443 | <code>            return; // done</code> | Inline comment: done |
| 444 | <code>        } // end of confirm</code> | Inline comment: end of confirm |
| 445 | <code>        if (key == KeyEvent.VK_UP &#124;&#124; key == KeyEvent.VK_W) pauseIndex = (pauseIndex + 2) % 3; // cursor up (wraps)</code> | Inline comment: cursor up (wraps) |
| 446 | <code>        if (key == KeyEvent.VK_DOWN &#124;&#124; key == KeyEvent.VK_S) pauseIndex = (pauseIndex + 1) % 3; // cursor down (wraps)</code> | Inline comment: cursor down (wraps) |
| 447 | <code>        if (key == KeyEvent.VK_ENTER &#124;&#124; key == KeyEvent.VK_SPACE) { // choose</code> | Inline comment: choose |
| 448 | <code>            if (pauseIndex == 0) { // Resume</code> | Inline comment: Resume |
| 449 | <code>                setPaused(false); // continue the game</code> | Inline comment: continue the game |
| 450 | <code>            } else if (pauseIndex == 1) { // Main Menu</code> | Inline comment: Main Menu |
| 451 | <code>                stopGame(); // stop this session first</code> | Inline comment: stop this session first |
| 452 | <code>                if (gameUI != null) gameUI.showScreen("MENU"); // show the main menu</code> | Inline comment: show the main menu |
| 453 | <code>            } else { // Quit Game</code> | Inline comment: Quit Game |
| 454 | <code>                confirmQuit = true; // ask first</code> | Inline comment: ask first |
| 455 | <code>            } // end of choice</code> | Inline comment: end of choice |
| 456 | <code>        } // end of choose</code> | Inline comment: end of choose |
| 457 | <code>        repaint(); // redraw the cursor</code> | Inline comment: redraw the cursor |
| 458 | <code>    } // end of handlePauseKey</code> | Inline comment: end of handlePauseKey |
| 459 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 460 | <code>    private void drawPauseMenu(Graphics2D g2d, int panelWidth) { // dark overlay with the three options</code> | Inline comment: dark overlay with the three options |
| 461 | <code>        g2d.setColor(new Color(0, 0, 0, 180)); // translucent black</code> | Inline comment: translucent black |
| 462 | <code>        g2d.fillRect(0, 0, panelWidth, getHeight()); // cover everything</code> | Inline comment: cover everything |
| 463 | <code>        int cx = panelWidth / 2; // horizontal center</code> | Inline comment: horizontal center |
| 464 | <code>        int top = Math.max(150, getHeight() / 2 - 110); // first line position</code> | Inline comment: first line position |
| 465 | <code>        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 40)); // title font</code> | Inline comment: title font |
| 466 | <code>        g2d.setColor(Color.CYAN); // title color</code> | Inline comment: title color |
| 467 | <code>        String title = "PAUSED"; // title text</code> | Inline comment: title text |
| 468 | <code>        g2d.drawString(title, cx - g2d.getFontMetrics().stringWidth(title) / 2, top); // centered title</code> | Inline comment: centered title |
| 469 | <code>        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26)); // option font</code> | Inline comment: option font |
| 470 | <code>        String[] options = {"Resume", "Main Menu", "Quit Game"}; // the three choices</code> | Inline comment: the three choices |
| 471 | <code>        for (int i = 0; i &lt; options.length; i++) { // each option</code> | Inline comment: each option |
| 472 | <code>            boolean selected = i == pauseIndex &amp;&amp; !confirmQuit; // is the cursor here</code> | Inline comment: is the cursor here |
| 473 | <code>            g2d.setColor(selected ? Color.YELLOW : Color.WHITE); // highlight the cursor line</code> | Inline comment: highlight the cursor line |
| 474 | <code>            String text = (selected ? "&gt; " : "  ") + options[i]; // cursor mark</code> | Inline comment: cursor mark |
| 475 | <code>            g2d.drawString(text, cx - 110, top + 60 + i * 44); // left aligned at a common x</code> | Inline comment: left aligned at a common x |
| 476 | <code>        } // end of options</code> | Inline comment: end of options |
| 477 | <code>        g2d.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16)); // hint font</code> | Inline comment: hint font |
| 478 | <code>        g2d.setColor(confirmQuit ? Color.ORANGE : Color.GRAY); // prompt color</code> | Inline comment: prompt color |
| 479 | <code>        String hint = confirmQuit ? "Quit the game? Enter/Y = Yes, N or Esc = No" : "Up/Down = move, Enter = select, Esc = resume"; // help text</code> | Inline comment: help text |
| 480 | <code>        g2d.drawString(hint, cx - g2d.getFontMetrics().stringWidth(hint) / 2, top + 60 + options.length * 44 + 20); // centered help</code> | Inline comment: centered help |
| 481 | <code>    } // end of drawPauseMenu</code> | Inline comment: end of drawPauseMenu |
| 482 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 483 | <code>    /** Opens the real chest: +500, unlocks the exit, and the noise may be heard by mimics. Does nothing if it is already open. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 484 | <code>    private void openRealChest() { // the one place where the real chest is opened</code> | Inline comment: the one place where the real chest is opened |
| 485 | <code>        Point c = treasureSystem.getPosition(); // the real chest</code> | Inline comment: the real chest |
| 486 | <code>        if (c == null &#124;&#124; !treasureSystem.claimIfSteppedOn(c.x, c.y)) return; // nothing to open or already open</code> | Inline comment: nothing to open or already open |
| 487 | <code>        scoreManager.addTreasurePoints(); // +500</code> | Inline comment: +500 |
| 488 | <code>        sound.play("chest"); // chest opening sound</code> | Inline comment: chest opening sound |
| 489 | <code>        statusMessage = "Treasure Claimed (+500)! Exit Unlocked!"; // message</code> | Inline comment: message |
| 490 | <code>        if (mimicSystem.hearNoise(c.x, c.y, currentMap, CHEST_NOISE_RANGE) &gt; 0) { // the noise carries</code> | Inline comment: the noise carries |
| 491 | <code>            statusMessage = "Chest opened (+500)! A mimic heard it - it is coming!"; // warn the player</code> | Inline comment: warn the player |
| 492 | <code>        } // end of noise check</code> | Inline comment: end of noise check |
| 493 | <code>    } // end of openRealChest</code> | Inline comment: end of openRealChest |
| 494 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 495 | <code>    /** A fake chest was opened: the mimic is revealed and stands still for 0.5 s. No treasure, no exit unlock. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 496 | <code>    private void revealMimicChest(int x, int y) { // reveal rule</code> | Inline comment: reveal rule |
| 497 | <code>        if (!mimicSystem.revealAt(x, y, clock())) return; // not a fake chest</code> | Inline comment: not a fake chest |
| 498 | <code>        sound.play("mimic_step"); // a growl-like sound (reuses an existing effect)</code> | Inline comment: a growl-like sound (reuses an existing effect) |
| 499 | <code>        statusMessage = "It was a MIMIC! It freezes for a moment - RUN!"; // warn the player</code> | Inline comment: warn the player |
| 500 | <code>        flashUntil = clock() + 250; // brief red flash</code> | Inline comment: brief red flash |
| 501 | <code>    } // end of revealMimicChest</code> | Inline comment: end of revealMimicChest |
| 502 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 503 | <code>    /**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 504 | <code>     * T key: look at the four cells next to the player in the fixed order up, down, left, right.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 505 | <code>     * The first chest found is opened: a real chest gives its reward, a fake chest reveals the mimic.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 506 | <code>     * With no chest next to the player nothing happens.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 507 | <code>     */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 508 | <code>    private void interactWithChest() { // T key rule</code> | Inline comment: T key rule |
| 509 | <code>        int[][] order = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}}; // up, down, left, right</code> | Inline comment: up, down, left, right |
| 510 | <code>        Point real = treasureSystem.getPosition(); // the real chest</code> | Inline comment: the real chest |
| 511 | <code>        for (int[] d : order) { // each neighbour in the fixed order</code> | Inline comment: each neighbour in the fixed order |
| 512 | <code>            int cx = player.getX() + d[0]; // neighbour column</code> | Inline comment: neighbour column |
| 513 | <code>            int cy = player.getY() + d[1]; // neighbour row</code> | Inline comment: neighbour row |
| 514 | <code>            if (real != null &amp;&amp; real.x == cx &amp;&amp; real.y == cy) { // the real chest</code> | Inline comment: the real chest |
| 515 | <code>                if (treasureSystem.isClaimed()) statusMessage = "This chest is already open."; // nothing more to take</code> | Inline comment: nothing more to take |
| 516 | <code>                else openRealChest(); // open it normally</code> | Inline comment: open it normally |
| 517 | <code>                repaint(); // redraw</code> | Inline comment: redraw |
| 518 | <code>                return; // only one chest per key press</code> | Inline comment: only one chest per key press |
| 519 | <code>            } // end of real chest</code> | Inline comment: end of real chest |
| 520 | <code>            if (mimicSystem.isDisguisedAt(cx, cy)) { // a fake chest</code> | Inline comment: a fake chest |
| 521 | <code>                revealMimicChest(cx, cy); // reveal the mimic instead</code> | Inline comment: reveal the mimic instead |
| 522 | <code>                repaint(); // redraw</code> | Inline comment: redraw |
| 523 | <code>                return; // only one chest per key press</code> | Inline comment: only one chest per key press |
| 524 | <code>            } // end of fake chest</code> | Inline comment: end of fake chest |
| 525 | <code>        } // end of neighbours</code> | Inline comment: end of neighbours |
| 526 | <code>        statusMessage = "No chest next to you."; // nothing to open</code> | Inline comment: nothing to open |
| 527 | <code>        repaint(); // redraw</code> | Inline comment: redraw |
| 528 | <code>    } // end of interactWithChest</code> | Inline comment: end of interactWithChest |
| 529 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 530 | <code>    /** SPACE: put down one carried blue bomb on the player's cell (not on the chest or the exit). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 531 | <code>    private void placeBlueBomb() { // place a trap</code> | Inline comment: place a trap |
| 532 | <code>        int x = player.getX(); // cell column</code> | Inline comment: cell column |
| 533 | <code>        int y = player.getY(); // cell row</code> | Inline comment: cell row |
| 534 | <code>        Point t = treasureSystem.getPosition(); // real chest</code> | Inline comment: real chest |
| 535 | <code>        boolean special = (t != null &amp;&amp; t.x == x &amp;&amp; t.y == y) &#124;&#124; (exitPos != null &amp;&amp; exitPos.x == x &amp;&amp; exitPos.y == y); // chest or exit cell</code> | Inline comment: chest or exit cell |
| 536 | <code>        if (blueBombs &lt;= 0) { // nothing to place</code> | Inline comment: nothing to place |
| 537 | <code>            statusMessage = "No blue bombs. Collect the blue pickups first."; // explain</code> | Inline comment: explain |
| 538 | <code>        } else if (special &#124;&#124; !bombSystem.placeTrap(x, y)) { // bad cell or a trap is already here</code> | Inline comment: bad cell or a trap is already here |
| 539 | <code>            statusMessage = "You cannot place a blue bomb here."; // explain</code> | Inline comment: explain |
| 540 | <code>        } else { // placed</code> | Inline comment: placed |
| 541 | <code>            blueBombs--; // one used</code> | Inline comment: one used |
| 542 | <code>            sound.play("tick"); // placing sound</code> | Inline comment: placing sound |
| 543 | <code>            statusMessage = "Blue bomb placed. It stuns mimics that come close."; // feedback</code> | Inline comment: feedback |
| 544 | <code>        } // end of result</code> | Inline comment: end of result |
| 545 | <code>        repaint(); // redraw</code> | Inline comment: redraw |
| 546 | <code>    } // end of placeBlueBomb</code> | Inline comment: end of placeBlueBomb |
| 547 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 548 | <code>    private boolean nearChest() { // is the player within 4 cells of the chest</code> | Inline comment: is the player within 4 cells of the chest |
| 549 | <code>        Point t = treasureSystem.getPosition(); // chest cell</code> | Inline comment: chest cell |
| 550 | <code>        return t != null &amp;&amp; Math.abs(player.getX() - t.x) + Math.abs(player.getY() - t.y) &lt;= 4; // Manhattan distance</code> | Inline comment: Manhattan distance |
| 551 | <code>    } // end of nearChest</code> | Inline comment: end of nearChest |
| 552 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 553 | <code>    /** A mimic on the player's cell catches the player (called after player moves and after mimic steps). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 554 | <code>    private void checkMimicCatch() { // catch rule</code> | Inline comment: catch rule |
| 555 | <code>        if (dying &#124;&#124; finished &#124;&#124; !mimicSystem.checkCollision(player.getX(), player.getY())) return; // nothing to do</code> | Inline comment: nothing to do |
| 556 | <code>        sound.play("mimic_catch"); // distinct catch sound</code> | Inline comment: distinct catch sound |
| 557 | <code>        int hearts = MIMIC_DAMAGE[Math.min(levelManager.getCurrentLevel(), MIMIC_DAMAGE.length) - 1]; // damage depends on the level</code> | Inline comment: damage depends on the level |
| 558 | <code>        damagePlayer("CAUGHT BY MIMIC! (-" + hearts + (hearts == 1 ? " Heart)" : " Hearts)"), false, hearts); // lose hearts, nothing else changes</code> | Inline comment: lose hearts, nothing else changes |
| 559 | <code>        mimicSystem.stunNear(player.getX(), player.getY(), 0, clock() + CATCH_STUN_MILLIS); // the mimic freezes: one catch hurts once and the player can leave</code> | Inline comment: the mimic freezes: one catch hurts once and the player can leave |
| 560 | <code>    } // end of checkMimicCatch</code> | Inline comment: end of checkMimicCatch |
| 561 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 562 | <code>    private void restartLevelWithPenalty() { // R key: restart the level for 1 heart</code> | Inline comment: R key: restart the level for 1 heart |
| 563 | <code>        if (dying) return; // no restarting during the countdown</code> | Inline comment: no restarting during the countdown |
| 564 | <code>        if (scoreManager.getLives() &lt;= 1) { // restarting would remove the last heart</code> | Inline comment: restarting would remove the last heart |
| 565 | <code>            statusMessage = "You cannot restart on your last heart!"; // explain</code> | Inline comment: explain |
| 566 | <code>            repaint(); // redraw</code> | Inline comment: redraw |
| 567 | <code>            return; // refuse</code> | Inline comment: refuse |
| 568 | <code>        } // end of last heart check</code> | Inline comment: end of last heart check |
| 569 | <code>        scoreManager.loseLife(); // pay one heart</code> | Inline comment: pay one heart |
| 570 | <code>        loadCurrentLevel(); // reload the level</code> | Inline comment: reload the level |
| 571 | <code>        statusMessage = "Level restarted (-1 Heart)."; // message</code> | Inline comment: message |
| 572 | <code>        repaint(); // redraw</code> | Inline comment: redraw |
| 573 | <code>    } // end of restartLevelWithPenalty</code> | Inline comment: end of restartLevelWithPenalty |
| 574 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 575 | <code>    // ---------------------------------------------------------------- drawing</code> | Inline comment: ---------------------------------------------------------------- drawing |
| 576 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 577 | <code>    @Override // replaces JPanel painting</code> | Inline comment: replaces JPanel painting |
| 578 | <code>    protected void paintComponent(Graphics g) { // draw the whole screen</code> | Inline comment: draw the whole screen |
| 579 | <code>        super.paintComponent(g); // clear the background</code> | Inline comment: clear the background |
| 580 | <code>        if (currentMap == null &#124;&#124; currentMap.length == 0) return; // nothing to draw</code> | Inline comment: nothing to draw |
| 581 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 582 | <code>        Graphics2D g2d = (Graphics2D) g; // richer drawing API</code> | Inline comment: richer drawing API |
| 583 | <code>        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); // smooth text</code> | Inline comment: smooth text |
| 584 | <code>        long now = clock(); // current time for animations</code> | Inline comment: current time for animations |
| 585 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 586 | <code>        int panelWidth = getWidth(); // panel width in pixels</code> | Inline comment: panel width in pixels |
| 587 | <code>        int rows = currentMap.length; // map rows</code> | Inline comment: map rows |
| 588 | <code>        int cols = currentMap[0].length(); // map columns</code> | Inline comment: map columns |
| 589 | <code>        int availableHeight = Math.max(1, getHeight() - HUD_SPACE); // height left below the HUD</code> | Inline comment: height left below the HUD |
| 590 | <code>        int fit = Math.min(panelWidth / cols, availableHeight / rows); // largest square cell that fits</code> | Inline comment: largest square cell that fits |
| 591 | <code>        int tile = fit &gt;= 16 ? (fit / 16) * 16 : Math.max(1, fit); // whole multiples of 16 keep pixel art crisp</code> | Inline comment: whole multiples of 16 keep pixel art crisp |
| 592 | <code>        if (levelManager.getCurrentLevel() == 3) { // level 3 is big: zoom in so sprites are easy to see</code> | Inline comment: level 3 is big: zoom in so sprites are easy to see |
| 593 | <code>            tile = Math.max(tile, LEVEL3_MIN_TILE); // never smaller than 32 px (levels 1 and 2 are unchanged)</code> | Inline comment: never smaller than 32 px (levels 1 and 2 are unchanged) |
| 594 | <code>        } // end of zoom</code> | Inline comment: end of zoom |
| 595 | <code>        int offsetX = (panelWidth - cols * tile) / 2; // center horizontally</code> | Inline comment: center horizontally |
| 596 | <code>        int offsetY = HUD_SPACE + (availableHeight - rows * tile) / 2; // center vertically below the HUD</code> | Inline comment: center vertically below the HUD |
| 597 | <code>        if (cols * tile &gt; panelWidth) { // zoomed map is wider than the window: scroll with the player</code> | Inline comment: zoomed map is wider than the window: scroll with the player |
| 598 | <code>            offsetX = Math.max(panelWidth - cols * tile, Math.min(0, panelWidth / 2 - (player.getX() * tile + tile / 2))); // keep the player centered, never show past the map edge</code> | Inline comment: keep the player centered, never show past the map edge |
| 599 | <code>        } // end of horizontal scroll</code> | Inline comment: end of horizontal scroll |
| 600 | <code>        if (rows * tile &gt; availableHeight) { // zoomed map is taller than the window: scroll with the player</code> | Inline comment: zoomed map is taller than the window: scroll with the player |
| 601 | <code>            offsetY = HUD_SPACE + Math.max(availableHeight - rows * tile, Math.min(0, availableHeight / 2 - (player.getY() * tile + tile / 2))); // same vertically</code> | Inline comment: same vertically |
| 602 | <code>        } // end of vertical scroll</code> | Inline comment: end of vertical scroll |
| 603 | <code>        g2d.setClip(0, HUD_SPACE, panelWidth, availableHeight); // draw the map only inside the map area (nothing over the HUD)</code> | Inline comment: draw the map only inside the map area (nothing over the HUD) |
| 604 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 605 | <code>        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(8, tile * 3 / 4))); // font for letter fallbacks</code> | Inline comment: font for letter fallbacks |
| 606 | <code>        FontMetrics fm = g2d.getFontMetrics(); // measure glyphs for centering</code> | Inline comment: measure glyphs for centering |
| 607 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 608 | <code>        for (int r = 0; r &lt; rows; r++) { // for each row</code> | Inline comment: for each row |
| 609 | <code>            for (int c = 0; c &lt; cols; c++) { // for each column</code> | Inline comment: for each column |
| 610 | <code>                int x = offsetX + c * tile; // pixel x of the cell</code> | Inline comment: pixel x of the cell |
| 611 | <code>                int y = offsetY + r * tile; // pixel y of the cell</code> | Inline comment: pixel y of the cell |
| 612 | <code>                if (currentMap[r].charAt(c) == '#') { // wall cell</code> | Inline comment: wall cell |
| 613 | <code>                    drawTileOrLetter(g2d, fm, wallSprite(c, r), '#', new Color(95, 95, 120), x, y, tile); // draw a wall</code> | Inline comment: draw a wall |
| 614 | <code>                } else { // open cell</code> | Inline comment: open cell |
| 615 | <code>                    drawTileOrLetter(g2d, fm, (c * 3 + r * 5) % 4 == 0 ? "floor1" : "floor0", '.', new Color(55, 55, 70), x, y, tile); // draw floor</code> | Inline comment: draw floor |
| 616 | <code>                    if (vanishSystem.isVanished(c, r)) { // the tile has crumbled away</code> | Inline comment: the tile has crumbled away |
| 617 | <code>                        g2d.setColor(new Color(4, 4, 8)); // dark void</code> | Inline comment: dark void |
| 618 | <code>                        g2d.fillRect(x, y, tile, tile); // hole in the floor</code> | Inline comment: hole in the floor |
| 619 | <code>                        g2d.setColor(new Color(60, 30, 30)); // faint rim</code> | Inline comment: faint rim |
| 620 | <code>                        g2d.drawRect(x, y, tile - 1, tile - 1); // outline of the hole</code> | Inline comment: outline of the hole |
| 621 | <code>                    } else if (vanishSystem.isCrumbling(c, r)) { // about to vanish</code> | Inline comment: about to vanish |
| 622 | <code>                        drawTileOrLetter(g2d, fm, "cracked", ':', new Color(200, 140, 80), x, y, tile); // cracked look</code> | Inline comment: cracked look |
| 623 | <code>                    } // end of vanish drawing</code> | Inline comment: end of vanish drawing |
| 624 | <code>                } // end of cell type check</code> | Inline comment: end of cell type check |
| 625 | <code>            } // end of column loop</code> | Inline comment: end of column loop |
| 626 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 627 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 628 | <code>        for (Point coin : coinSystem.getPositions()) { // each coin still on the map</code> | Inline comment: each coin still on the map |
| 629 | <code>            drawEntity(g2d, fm, "coin", 'o', Color.ORANGE, offsetX, offsetY, tile, coin.x, coin.y, false, 0); // draw a coin</code> | Inline comment: draw a coin |
| 630 | <code>        } // end of coin loop</code> | Inline comment: end of coin loop |
| 631 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 632 | <code>        for (Point b : bombSystem.getBombPositions()) { // for each live bomb</code> | Inline comment: for each live bomb |
| 633 | <code>            drawEntity(g2d, fm, "bomb", 'B', new Color(230, 50, 50), offsetX, offsetY, tile, b.x, b.y, false, 0); // draw a bomb</code> | Inline comment: draw a bomb |
| 634 | <code>        } // end of bomb loop</code> | Inline comment: end of bomb loop |
| 635 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 636 | <code>        for (Point b : bombSystem.getBluePickups()) { // blue Mimic-trap pickups</code> | Inline comment: blue Mimic-trap pickups |
| 637 | <code>            drawEntity(g2d, fm, "bombBlue", 'U', new Color(70, 120, 255), offsetX, offsetY, tile, b.x, b.y, false, 0); // blue bomb</code> | Inline comment: blue bomb |
| 638 | <code>        } // end of blue pickups</code> | Inline comment: end of blue pickups |
| 639 | <code>        for (Point b : bombSystem.getTraps()) { // blue bombs the player placed</code> | Inline comment: blue bombs the player placed |
| 640 | <code>            drawEntity(g2d, fm, "bombBlue", 'u', new Color(120, 170, 255), offsetX, offsetY, tile, b.x, b.y, false, 0); // same picture</code> | Inline comment: same picture |
| 641 | <code>        } // end of traps</code> | Inline comment: end of traps |
| 642 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 643 | <code>        Point t = treasureSystem.getPosition(); // treasure cell</code> | Inline comment: treasure cell |
| 644 | <code>        if (t != null) { // chest exists</code> | Inline comment: chest exists |
| 645 | <code>            String chest = treasureSystem.isClaimed() ? "chestOpen" : "chestClosed"; // open after claiming</code> | Inline comment: open after claiming |
| 646 | <code>            drawEntity(g2d, fm, chest, 'T', Color.YELLOW, offsetX, offsetY, tile, t.x, t.y, false, 0); // draw the chest</code> | Inline comment: draw the chest |
| 647 | <code>        } // end of chest check</code> | Inline comment: end of chest check |
| 648 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 649 | <code>        for (Point d : mimicSystem.getDisguisedPositions()) { // fake chests look exactly like the real closed chest</code> | Inline comment: fake chests look exactly like the real closed chest |
| 650 | <code>            drawEntity(g2d, fm, "chestClosed", 'T', Color.YELLOW, offsetX, offsetY, tile, d.x, d.y, false, 0); // same sprite, same fallback letter</code> | Inline comment: same sprite, same fallback letter |
| 651 | <code>        } // end of fake chests</code> | Inline comment: end of fake chests |
| 652 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 653 | <code>        if (exitPos != null) { // exit exists</code> | Inline comment: exit exists |
| 654 | <code>            boolean open = isExitUnlocked(); // unlocked state</code> | Inline comment: unlocked state |
| 655 | <code>            drawEntity(g2d, fm, open ? "doorOpen" : "doorClosed", 'E', open ? Color.CYAN : Color.GRAY, offsetX, offsetY, tile, exitPos.x, exitPos.y, false, 0); // draw the door</code> | Inline comment: draw the door |
| 656 | <code>        } // end of exit check</code> | Inline comment: end of exit check |
| 657 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 658 | <code>        for (Point m : mimicSystem.getPositions()) { // for each mimic</code> | Inline comment: for each mimic |
| 659 | <code>            drawEntity(g2d, fm, "mimic", 'M', Color.MAGENTA, offsetX, offsetY, tile, m.x, m.y, false, 0); // draw a mimic</code> | Inline comment: draw a mimic |
| 660 | <code>        } // end of mimic loop</code> | Inline comment: end of mimic loop |
| 661 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 662 | <code>        for (Point st : mimicSystem.getStunnedPositions()) { // stunned mimics get a blue tint</code> | Inline comment: stunned mimics get a blue tint |
| 663 | <code>            g2d.setColor(new Color(80, 140, 255, 110)); // translucent blue</code> | Inline comment: translucent blue |
| 664 | <code>            g2d.fillRect(offsetX + st.x * tile, offsetY + st.y * tile, tile, tile); // cover the cell</code> | Inline comment: cover the cell |
| 665 | <code>        } // end of stunned</code> | Inline comment: end of stunned |
| 666 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 667 | <code>        int hop = (now - lastMoveTime &lt; 120) ? -(tile / 8) : 0; // small hop right after a step = walking animation</code> | Inline comment: small hop right after a step = walking animation |
| 668 | <code>        boolean flip = facingLeft; // mirror when facing left</code> | Inline comment: mirror when facing left |
| 669 | <code>        if (stepParity &amp;&amp; now - lastMoveTime &lt; 120) { // alternate frame of the walk</code> | Inline comment: alternate frame of the walk |
| 670 | <code>            hop = -(tile / 6); // slightly higher hop on every second step</code> | Inline comment: slightly higher hop on every second step |
| 671 | <code>        } // end of alternate frame</code> | Inline comment: end of alternate frame |
| 672 | <code>        drawEntity(g2d, fm, "player", '@', Color.GREEN, offsetX, offsetY, tile, player.getX(), player.getY(), flip, hop); // draw the player</code> | Inline comment: draw the player |
| 673 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 674 | <code>        for (Explosion ex : explosions) { // each explosion animation</code> | Inline comment: each explosion animation |
| 675 | <code>            long elapsed = now - ex.start; // time since it started</code> | Inline comment: time since it started |
| 676 | <code>            int frame = (int) (elapsed * sprites.explosionFrames() / EXPLOSION_MILLIS); // current animation frame</code> | Inline comment: current animation frame |
| 677 | <code>            if (frame &gt;= 0 &amp;&amp; frame &lt; sprites.explosionFrames()) { // still playing</code> | Inline comment: still playing |
| 678 | <code>                int w = tile * 3; // explosion is 3 cells wide</code> | Inline comment: explosion is 3 cells wide |
| 679 | <code>                int h = w * 91 / 96; // keep the picture's proportions</code> | Inline comment: keep the picture's proportions |
| 680 | <code>                int cx = offsetX + ex.x * tile + tile / 2; // cell center x</code> | Inline comment: cell center x |
| 681 | <code>                int cy = offsetY + ex.y * tile + tile / 2; // cell center y</code> | Inline comment: cell center y |
| 682 | <code>                sprites.drawScaled(g2d, "explosion" + frame, cx - w / 2, cy - h / 2, w, h); // draw the frame</code> | Inline comment: draw the frame |
| 683 | <code>            } // end of frame check</code> | Inline comment: end of frame check |
| 684 | <code>        } // end of explosion loop</code> | Inline comment: end of explosion loop |
| 685 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 686 | <code>        g2d.setClip(null); // back to full-window drawing for the flash and HUD</code> | Inline comment: back to full-window drawing for the flash and HUD |
| 687 | <code>        if (now &lt; flashUntil &#124;&#124; dying) { // damage flash or countdown glow</code> | Inline comment: damage flash or countdown glow |
| 688 | <code>            g2d.setColor(new Color(220, 0, 0, dying ? 35 : 70)); // translucent red</code> | Inline comment: translucent red |
| 689 | <code>            g2d.fillRect(0, HUD_SPACE, panelWidth, getHeight() - HUD_SPACE); // cover the map area</code> | Inline comment: cover the map area |
| 690 | <code>        } // end of flash</code> | Inline comment: end of flash |
| 691 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 692 | <code>        drawHud(g2d, panelWidth, now); // draw the status bar</code> | Inline comment: draw the status bar |
| 693 | <code>        if (paused) drawPauseMenu(g2d, panelWidth); // pause overlay on top of everything</code> | Inline comment: pause overlay on top of everything |
| 694 | <code>    } // end of paintComponent</code> | Inline comment: end of paintComponent |
| 695 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 696 | <code>    private String wallSprite(int c, int r) { // pick a wall picture by position, so it looks varied but stays stable</code> | Inline comment: pick a wall picture by position, so it looks varied but stays stable |
| 697 | <code>        int h = (c * 7 + r * 13) % 11; // pseudo-random value from the position</code> | Inline comment: pseudo-random value from the position |
| 698 | <code>        if (h == 0) return "wall2"; // occasional barred window</code> | Inline comment: occasional barred window |
| 699 | <code>        return h % 2 == 0 ? "wall0" : "wall1"; // two brick variants</code> | Inline comment: two brick variants |
| 700 | <code>    } // end of wallSprite</code> | Inline comment: end of wallSprite |
| 701 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 702 | <code>    private void drawTileOrLetter(Graphics2D g2d, FontMetrics fm, String sprite, char letter, Color color, // draw a picture or a letter</code> | Inline comment: draw a picture or a letter |
| 703 | <code>                                  int x, int y, int tile) { // pixel position and cell size</code> | Inline comment: pixel position and cell size |
| 704 | <code>        if (sprites.has(sprite)) { // picture available</code> | Inline comment: picture available |
| 705 | <code>            sprites.draw(g2d, sprite, x, y, tile, false); // draw it</code> | Inline comment: draw it |
| 706 | <code>        } else { // missing picture</code> | Inline comment: missing picture |
| 707 | <code>            drawLetter(g2d, fm, letter, color, x, y, tile); // fall back to a letter</code> | Inline comment: fall back to a letter |
| 708 | <code>        } // end of availability check</code> | Inline comment: end of availability check |
| 709 | <code>    } // end of drawTileOrLetter</code> | Inline comment: end of drawTileOrLetter |
| 710 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 711 | <code>    private void drawEntity(Graphics2D g2d, FontMetrics fm, String sprite, char letter, Color color, // draw an object on a cell</code> | Inline comment: draw an object on a cell |
| 712 | <code>                            int offsetX, int offsetY, int tile, int col, int row, boolean flip, int yOffset) { // grid placement</code> | Inline comment: grid placement |
| 713 | <code>        int x = offsetX + col * tile; // pixel x</code> | Inline comment: pixel x |
| 714 | <code>        int y = offsetY + row * tile + yOffset; // pixel y (yOffset is used for the hop)</code> | Inline comment: pixel y (yOffset is used for the hop) |
| 715 | <code>        if (sprites.has(sprite)) { // picture available</code> | Inline comment: picture available |
| 716 | <code>            sprites.draw(g2d, sprite, x, y, tile, flip); // draw it</code> | Inline comment: draw it |
| 717 | <code>        } else { // missing picture</code> | Inline comment: missing picture |
| 718 | <code>            drawLetter(g2d, fm, letter, color, x, y, tile); // fall back to a letter</code> | Inline comment: fall back to a letter |
| 719 | <code>        } // end of availability check</code> | Inline comment: end of availability check |
| 720 | <code>    } // end of drawEntity</code> | Inline comment: end of drawEntity |
| 721 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 722 | <code>    private void drawLetter(Graphics2D g2d, FontMetrics fm, char ch, Color color, int x, int y, int tile) { // letter fallback</code> | Inline comment: letter fallback |
| 723 | <code>        g2d.setColor(color); // letter color</code> | Inline comment: letter color |
| 724 | <code>        int px = x + (tile - fm.charWidth(ch)) / 2; // centered x</code> | Inline comment: centered x |
| 725 | <code>        int py = y + (tile + fm.getAscent() - fm.getDescent()) / 2; // centered baseline</code> | Inline comment: centered baseline |
| 726 | <code>        g2d.drawString(String.valueOf(ch), px, py); // draw the character</code> | Inline comment: draw the character |
| 727 | <code>    } // end of drawLetter</code> | Inline comment: end of drawLetter |
| 728 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 729 | <code>    private void drawHud(Graphics2D g2d, int panelWidth, long now) { // draw hearts, score, moves, level, coins, message, countdown</code> | Inline comment: draw hearts, score, moves, level, coins, message, countdown |
| 730 | <code>        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 18)); // HUD font</code> | Inline comment: HUD font |
| 731 | <code>        FontMetrics fm = g2d.getFontMetrics(); // measure text for alignment</code> | Inline comment: measure text for alignment |
| 732 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 733 | <code>        g2d.setColor(Color.RED); // label color</code> | Inline comment: label color |
| 734 | <code>        String label = "HEALTH: "; // hearts label</code> | Inline comment: hearts label |
| 735 | <code>        g2d.drawString(label, 20, 28); // label, top left</code> | Inline comment: label, top left |
| 736 | <code>        int heartX = 20 + fm.stringWidth(label); // hearts start after the label</code> | Inline comment: hearts start after the label |
| 737 | <code>        int lives = scoreManager.getLives(); // hearts left</code> | Inline comment: hearts left |
| 738 | <code>        for (int i = 0; i &lt; ScoreManager.START_LIVES; i++) { // one slot per starting heart</code> | Inline comment: one slot per starting heart |
| 739 | <code>            boolean full = i &lt; lives; // full heart or lost heart</code> | Inline comment: full heart or lost heart |
| 740 | <code>            if (sprites.has("heart")) { // picture available</code> | Inline comment: picture available |
| 741 | <code>                java.awt.Composite old = g2d.getComposite(); // remember the current transparency</code> | Inline comment: remember the current transparency |
| 742 | <code>                if (!full) { // lost hearts are drawn faint</code> | Inline comment: lost hearts are drawn faint |
| 743 | <code>                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f)); // 25% visible</code> | Inline comment: 25% visible |
| 744 | <code>                } // end of faint</code> | Inline comment: end of faint |
| 745 | <code>                sprites.draw(g2d, "heart", heartX + i * 26, 8, 24, false); // draw the heart</code> | Inline comment: draw the heart |
| 746 | <code>                g2d.setComposite(old); // restore transparency</code> | Inline comment: restore transparency |
| 747 | <code>            } else { // no picture: use text</code> | Inline comment: no picture: use text |
| 748 | <code>                g2d.setColor(full ? Color.RED : Color.DARK_GRAY); // red or gray</code> | Inline comment: red or gray |
| 749 | <code>                g2d.drawString("&lt;3", heartX + i * 30, 28); // text heart</code> | Inline comment: text heart |
| 750 | <code>            } // end of picture check</code> | Inline comment: end of picture check |
| 751 | <code>        } // end of heart loop</code> | Inline comment: end of heart loop |
| 752 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 753 | <code>        g2d.setColor(Color.CYAN); // score color</code> | Inline comment: score color |
| 754 | <code>        String score = "SCORE: " + scoreManager.getTotalScore(); // score text</code> | Inline comment: score text |
| 755 | <code>        g2d.drawString(score, panelWidth - fm.stringWidth(score) - 20, 28); // score, top right</code> | Inline comment: score, top right |
| 756 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 757 | <code>        g2d.setColor(Color.WHITE); // level color</code> | Inline comment: level color |
| 758 | <code>        g2d.drawString("LEVEL " + levelManager.getCurrentLevel() + "/" + GameMap.LEVEL_COUNT // level text, continued on the next line</code> | Inline comment: level text, continued on the next line |
| 759 | <code>            + "   COINS " + coinSystem.getCollected() + "/" + coinSystem.getTotal() + "   BLUE BOMBS " + blueBombs, 20, 58); // level and coins, second row left</code> | Inline comment: level and coins, second row left |
| 760 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 761 | <code>        g2d.setColor(movesLeft &lt;= 15 ? Color.ORANGE : Color.WHITE); // warn when moves run low</code> | Inline comment: warn when moves run low |
| 762 | <code>        String moves = "MOVES: " + Math.max(0, movesLeft); // moves text</code> | Inline comment: moves text |
| 763 | <code>        g2d.drawString(moves, panelWidth - fm.stringWidth(moves) - 20, 58); // moves, second row right</code> | Inline comment: moves, second row right |
| 764 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 765 | <code>        g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15)); // smaller font for the message</code> | Inline comment: smaller font for the message |
| 766 | <code>        FontMetrics small = g2d.getFontMetrics(); // measure it</code> | Inline comment: measure it |
| 767 | <code>        g2d.setColor(Color.YELLOW); // message color</code> | Inline comment: message color |
| 768 | <code>        int msgX = Math.max(300, (panelWidth - small.stringWidth(statusMessage)) / 2); // center the message, keep clear of hearts</code> | Inline comment: center the message, keep clear of hearts |
| 769 | <code>        g2d.drawString(statusMessage, msgX, 28); // status message, top center</code> | Inline comment: status message, top center |
| 770 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 771 | <code>        if (dying) { // five-second escape countdown (small text, as requested)</code> | Inline comment: five-second escape countdown (small text, as requested) |
| 772 | <code>            double secondsLeft = Math.max(0, (dyingEnd - now) / 1000.0); // time left</code> | Inline comment: time left |
| 773 | <code>            String timer = String.format("ESCAPE TIME: %.1f s", secondsLeft); // formatted text</code> | Inline comment: formatted text |
| 774 | <code>            g2d.setColor(Color.RED); // urgent color</code> | Inline comment: urgent color |
| 775 | <code>            g2d.drawString(timer, Math.max(300, (panelWidth - small.stringWidth(timer)) / 2), 58); // under the message</code> | Inline comment: under the message |
| 776 | <code>        } // end of countdown text</code> | Inline comment: end of countdown text |
| 777 | <code>    } // end of drawHud</code> | Inline comment: end of drawHud |
| 778 | <code>} // end of TreasureMaze</code> | Inline comment: end of TreasureMaze |


## `src/logic/BombLogic.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package logic; // package for game rules</code> | Inline comment: package for game rules |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import java.awt.Point; // integer x/y coordinate</code> | Inline comment: integer x/y coordinate |
| 4 | <code>import java.util.ArrayList; // growable list</code> | Inline comment: growable list |
| 5 | <code>import java.util.Collections; // read-only wrapper helper</code> | Inline comment: read-only wrapper helper |
| 6 | <code>import java.util.List; // list interface</code> | Inline comment: list interface |
| 7 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 8 | <code>public class BombLogic { // keeps track of active bombs</code> | Inline comment: keeps track of active bombs |
| 9 | <code>    private final List&lt;Point&gt; bombPositions = new ArrayList&lt;&gt;(); // cells holding a live (black) bomb</code> | Inline comment: cells holding a live (black) bomb |
| 10 | <code>    private final List&lt;Point&gt; bluePickups = new ArrayList&lt;&gt;(); // blue Mimic-trap bombs lying on the map ('U'), to be collected</code> | Inline comment: blue Mimic-trap bombs lying on the map ('U'), to be collected |
| 11 | <code>    private final List&lt;Point&gt; traps = new ArrayList&lt;&gt;(); // blue bombs the player has placed (waiting for a mimic)</code> | Inline comment: blue bombs the player has placed (waiting for a mimic) |
| 12 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 13 | <code>    public void loadBombsFromMap(String[] map) { // read bombs from a level map</code> | Inline comment: read bombs from a level map |
| 14 | <code>        bombPositions.clear(); // forget old bombs</code> | Inline comment: forget old bombs |
| 15 | <code>        bluePickups.clear(); // forget old blue pickups</code> | Inline comment: forget old blue pickups |
| 16 | <code>        traps.clear(); // forget placed traps</code> | Inline comment: forget placed traps |
| 17 | <code>        if (map == null) return; // nothing to read</code> | Inline comment: nothing to read |
| 18 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 19 | <code>        for (int r = 0; r &lt; map.length; r++) { // for each row</code> | Inline comment: for each row |
| 20 | <code>            for (int c = 0; c &lt; map[r].length(); c++) { // for each column</code> | Inline comment: for each column |
| 21 | <code>                if (map[r].charAt(c) == 'B') { // bomb symbol</code> | Inline comment: bomb symbol |
| 22 | <code>                    bombPositions.add(new Point(c, r)); // remember its cell</code> | Inline comment: remember its cell |
| 23 | <code>                } else if (map[r].charAt(c) == 'U') { // blue bomb pickup symbol</code> | Inline comment: blue bomb pickup symbol |
| 24 | <code>                    bluePickups.add(new Point(c, r)); // remember its cell</code> | Inline comment: remember its cell |
| 25 | <code>                } // end of symbol check</code> | Inline comment: end of symbol check |
| 26 | <code>            } // end of column loop</code> | Inline comment: end of column loop |
| 27 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 28 | <code>    } // end of loadBombsFromMap</code> | Inline comment: end of loadBombsFromMap |
| 29 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 30 | <code>    /** Returns true if a bomb is at the position. The bomb is consumed (it explodes once). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 31 | <code>    public boolean triggerAt(int playerX, int playerY) { // explode a bomb at the player's cell</code> | Inline comment: explode a bomb at the player's cell |
| 32 | <code>        for (int i = 0; i &lt; bombPositions.size(); i++) { // for each bomb</code> | Inline comment: for each bomb |
| 33 | <code>            Point b = bombPositions.get(i); // current bomb</code> | Inline comment: current bomb |
| 34 | <code>            if (b.x == playerX &amp;&amp; b.y == playerY) { // player stands on it</code> | Inline comment: player stands on it |
| 35 | <code>                bombPositions.remove(i); // remove it after exploding</code> | Inline comment: remove it after exploding |
| 36 | <code>                return true; // hit</code> | Inline comment: hit |
| 37 | <code>            } // end of match check</code> | Inline comment: end of match check |
| 38 | <code>        } // end of bomb loop</code> | Inline comment: end of bomb loop |
| 39 | <code>        return false; // no bomb here</code> | Inline comment: no bomb here |
| 40 | <code>    } // end of triggerAt</code> | Inline comment: end of triggerAt |
| 41 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 42 | <code>    /** Picks up the blue bomb at the position (walking over it). Returns true if there was one; it is removed so it counts once. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 43 | <code>    public boolean collectBlueAt(int x, int y) { // collect a blue pickup</code> | Inline comment: collect a blue pickup |
| 44 | <code>        for (int i = 0; i &lt; bluePickups.size(); i++) { // each pickup</code> | Inline comment: each pickup |
| 45 | <code>            Point b = bluePickups.get(i); // current pickup</code> | Inline comment: current pickup |
| 46 | <code>            if (b.x == x &amp;&amp; b.y == y) { // player stands on it</code> | Inline comment: player stands on it |
| 47 | <code>                bluePickups.remove(i); // gone from the map</code> | Inline comment: gone from the map |
| 48 | <code>                return true; // collected</code> | Inline comment: collected |
| 49 | <code>            } // end of match check</code> | Inline comment: end of match check |
| 50 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 51 | <code>        return false; // nothing here</code> | Inline comment: nothing here |
| 52 | <code>    } // end of collectBlueAt</code> | Inline comment: end of collectBlueAt |
| 53 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 54 | <code>    /** Places a trap at the position unless one is already there. Returns true if placed. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 55 | <code>    public boolean placeTrap(int x, int y) { // put a blue bomb down</code> | Inline comment: put a blue bomb down |
| 56 | <code>        for (Point t : traps) { // each placed trap</code> | Inline comment: each placed trap |
| 57 | <code>            if (t.x == x &amp;&amp; t.y == y) return false; // already a trap here</code> | Inline comment: already a trap here |
| 58 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 59 | <code>        traps.add(new Point(x, y)); // place it</code> | Inline comment: place it |
| 60 | <code>        return true; // placed</code> | Inline comment: placed |
| 61 | <code>    } // end of placeTrap</code> | Inline comment: end of placeTrap |
| 62 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 63 | <code>    public void removeTrap(Point t) { // a trap went off: it is gone, so it cannot trigger again</code> | Inline comment: a trap went off: it is gone, so it cannot trigger again |
| 64 | <code>        traps.remove(t); // remove it from the list</code> | Inline comment: remove it from the list |
| 65 | <code>    } // end of removeTrap</code> | Inline comment: end of removeTrap |
| 66 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 67 | <code>    public List&lt;Point&gt; getBluePickups() { // blue pickups for drawing</code> | Inline comment: blue pickups for drawing |
| 68 | <code>        return Collections.unmodifiableList(bluePickups); // read-only view</code> | Inline comment: read-only view |
| 69 | <code>    } // end of getBluePickups</code> | Inline comment: end of getBluePickups |
| 70 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 71 | <code>    public List&lt;Point&gt; getTraps() { // placed traps for drawing and checking</code> | Inline comment: placed traps for drawing and checking |
| 72 | <code>        return Collections.unmodifiableList(traps); // read-only view</code> | Inline comment: read-only view |
| 73 | <code>    } // end of getTraps</code> | Inline comment: end of getTraps |
| 74 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 75 | <code>    public List&lt;Point&gt; getBombPositions() { // bombs for drawing</code> | Inline comment: bombs for drawing |
| 76 | <code>        return Collections.unmodifiableList(bombPositions); // read-only view</code> | Inline comment: read-only view |
| 77 | <code>    } // end of getBombPositions</code> | Inline comment: end of getBombPositions |
| 78 | <code>} // end of BombLogic</code> | Inline comment: end of BombLogic |


## `src/logic/CoinLogic.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package logic; // package for game rules</code> | Inline comment: package for game rules |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import java.awt.Point; // integer x/y coordinate</code> | Inline comment: integer x/y coordinate |
| 4 | <code>import java.util.ArrayList; // growable list</code> | Inline comment: growable list |
| 5 | <code>import java.util.Collections; // read-only wrapper helper</code> | Inline comment: read-only wrapper helper |
| 6 | <code>import java.util.List; // list interface</code> | Inline comment: list interface |
| 7 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 8 | <code>/**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 9 | <code> * Alternative way to unlock the exit: collect ALL coins ('C' on the map) instead of opening the chest.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 10 | <code> * Coins sit in far corners and dead ends, so this costs more moves and exploring than the chest.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 11 | <code> */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 12 | <code>public class CoinLogic { // coin state of a level</code> | Inline comment: coin state of a level |
| 13 | <code>    private final List&lt;Point&gt; coins = new ArrayList&lt;&gt;(); // coins still on the map</code> | Inline comment: coins still on the map |
| 14 | <code>    private int total; // coins the level started with</code> | Inline comment: coins the level started with |
| 15 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 16 | <code>    public void loadCoinsFromMap(String[] map) { // read coins from a level map</code> | Inline comment: read coins from a level map |
| 17 | <code>        coins.clear(); // forget old coins</code> | Inline comment: forget old coins |
| 18 | <code>        total = 0; // reset the count</code> | Inline comment: reset the count |
| 19 | <code>        if (map == null) return; // nothing to read</code> | Inline comment: nothing to read |
| 20 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 21 | <code>        for (int r = 0; r &lt; map.length; r++) { // for each row</code> | Inline comment: for each row |
| 22 | <code>            for (int c = 0; c &lt; map[r].length(); c++) { // for each column</code> | Inline comment: for each column |
| 23 | <code>                if (map[r].charAt(c) == 'C') { // coin symbol</code> | Inline comment: coin symbol |
| 24 | <code>                    coins.add(new Point(c, r)); // remember its cell</code> | Inline comment: remember its cell |
| 25 | <code>                    total++; // count it</code> | Inline comment: count it |
| 26 | <code>                } // end of symbol check</code> | Inline comment: end of symbol check |
| 27 | <code>            } // end of column loop</code> | Inline comment: end of column loop |
| 28 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 29 | <code>    } // end of loadCoinsFromMap</code> | Inline comment: end of loadCoinsFromMap |
| 30 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 31 | <code>    /** Picks up the coin at the position, if any. Returns true when a coin was collected. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 32 | <code>    public boolean collectAt(int playerX, int playerY) { // pick up a coin under the player</code> | Inline comment: pick up a coin under the player |
| 33 | <code>        for (int i = 0; i &lt; coins.size(); i++) { // for each coin</code> | Inline comment: for each coin |
| 34 | <code>            Point p = coins.get(i); // current coin</code> | Inline comment: current coin |
| 35 | <code>            if (p.x == playerX &amp;&amp; p.y == playerY) { // same cell</code> | Inline comment: same cell |
| 36 | <code>                coins.remove(i); // take it</code> | Inline comment: take it |
| 37 | <code>                return true; // collected</code> | Inline comment: collected |
| 38 | <code>            } // end of match check</code> | Inline comment: end of match check |
| 39 | <code>        } // end of coin loop</code> | Inline comment: end of coin loop |
| 40 | <code>        return false; // no coin here</code> | Inline comment: no coin here |
| 41 | <code>    } // end of collectAt</code> | Inline comment: end of collectAt |
| 42 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 43 | <code>    public boolean isComplete() { // were all coins collected</code> | Inline comment: were all coins collected |
| 44 | <code>        return total &gt; 0 &amp;&amp; coins.isEmpty(); // at least one existed and none are left</code> | Inline comment: at least one existed and none are left |
| 45 | <code>    } // end of isComplete</code> | Inline comment: end of isComplete |
| 46 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 47 | <code>    public int getCollected() { // coins picked up so far</code> | Inline comment: coins picked up so far |
| 48 | <code>        return total - coins.size(); // started minus remaining</code> | Inline comment: started minus remaining |
| 49 | <code>    } // end of getCollected</code> | Inline comment: end of getCollected |
| 50 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 51 | <code>    public int getTotal() { // coins the level started with</code> | Inline comment: coins the level started with |
| 52 | <code>        return total; // stored count</code> | Inline comment: stored count |
| 53 | <code>    } // end of getTotal</code> | Inline comment: end of getTotal |
| 54 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 55 | <code>    public List&lt;Point&gt; getPositions() { // coins for drawing</code> | Inline comment: coins for drawing |
| 56 | <code>        return Collections.unmodifiableList(coins); // read-only view</code> | Inline comment: read-only view |
| 57 | <code>    } // end of getPositions</code> | Inline comment: end of getPositions |
| 58 | <code>} // end of CoinLogic</code> | Inline comment: end of CoinLogic |


## `src/logic/LevelManager.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package logic; // package for game rules</code> | Inline comment: package for game rules |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import game.GameMap; // level map source</code> | Inline comment: level map source |
| 4 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 5 | <code>public class LevelManager { // tracks which level is active and holds this game's maps</code> | Inline comment: tracks which level is active and holds this game's maps |
| 6 | <code>    private int currentLevel; // active level number, 1-based</code> | Inline comment: active level number, 1-based |
| 7 | <code>    private final String[][] maps = new String[GameMap.LEVEL_COUNT][]; // the three maps of the current game</code> | Inline comment: the three maps of the current game |
| 8 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 9 | <code>    public LevelManager() { // start a game</code> | Inline comment: start a game |
| 10 | <code>        resetToLevel1(); // level 1 with freshly generated maps</code> | Inline comment: level 1 with freshly generated maps |
| 11 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 12 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 13 | <code>    /** Starts over: back to level 1 AND a brand-new random map for every level. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 14 | <code>    public final void resetToLevel1() { // new game (final: called from the constructor)</code> | Inline comment: new game (final: called from the constructor) |
| 15 | <code>        this.currentLevel = 1; // back to level 1</code> | Inline comment: back to level 1 |
| 16 | <code>        for (int i = 0; i &lt; GameMap.LEVEL_COUNT; i++) { // each level</code> | Inline comment: each level |
| 17 | <code>            maps[i] = GameMap.generate(i + 1); // generate a new random map (differs from the previous game)</code> | Inline comment: generate a new random map (differs from the previous game) |
| 18 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 19 | <code>    } // end of resetToLevel1</code> | Inline comment: end of resetToLevel1 |
| 20 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 21 | <code>    public int getCurrentLevel() { // read the active level</code> | Inline comment: read the active level |
| 22 | <code>        return currentLevel; // return it</code> | Inline comment: return it |
| 23 | <code>    } // end of getCurrentLevel</code> | Inline comment: end of getCurrentLevel |
| 24 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 25 | <code>    public boolean nextLevel() { // advance one level</code> | Inline comment: advance one level |
| 26 | <code>        if (currentLevel &lt; GameMap.LEVEL_COUNT) { // not on the last level</code> | Inline comment: not on the last level |
| 27 | <code>            currentLevel++; // go to the next level</code> | Inline comment: go to the next level |
| 28 | <code>            return true; // advanced</code> | Inline comment: advanced |
| 29 | <code>        } // end of limit check</code> | Inline comment: end of limit check |
| 30 | <code>        return false; // already on the last level</code> | Inline comment: already on the last level |
| 31 | <code>    } // end of nextLevel</code> | Inline comment: end of nextLevel |
| 32 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 33 | <code>    public String[] getCurrentMap() { // map of the active level</code> | Inline comment: map of the active level |
| 34 | <code>        return maps[currentLevel - 1]; // same map until the game is reset (so R restarts the same maze)</code> | Inline comment: same map until the game is reset (so R restarts the same maze) |
| 35 | <code>    } // end of getCurrentMap</code> | Inline comment: end of getCurrentMap |
| 36 | <code>} // end of LevelManager</code> | Inline comment: end of LevelManager |


## `src/logic/MimicLogic.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package logic; // package for game rules</code> | Inline comment: package for game rules |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import game.GameMap; // finds the start, chest and exit on the map</code> | Inline comment: finds the start, chest and exit on the map |
| 4 | <code>import java.awt.Point; // integer x/y coordinate</code> | Inline comment: integer x/y coordinate |
| 5 | <code>import java.util.ArrayList; // growable list</code> | Inline comment: growable list |
| 6 | <code>import java.util.Collections; // read-only wrapper helper</code> | Inline comment: read-only wrapper helper |
| 7 | <code>import java.util.List; // list interface</code> | Inline comment: list interface |
| 8 | <code>import java.util.Random; // random patrol goals and search spots</code> | Inline comment: random patrol goals and search spots |
| 9 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 10 | <code>/**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 11 | <code> * Real-time Mimic AI. It is updated by the game timer, so mimics move even when the player stands still.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 12 | <code> * States: PATROL (wander to random places all over the map) -&gt; CHASE (sees the player, walls block sight)</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 13 | <code> *   -&gt; INVESTIGATE (walk to the last known position or to a noise) -&gt; SEARCH (look around 3 spots) -&gt; PATROL.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 14 | <code> * Hearing: the player walking near the chest, or opening it, makes noise that only nearby mimics (by path) hear.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 15 | <code> * Fairness: mimics are always slower than a player who keeps moving, and they give up after a short search.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 16 | <code> */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 17 | <code>public class MimicLogic { // controls all mimics on a level</code> | Inline comment: controls all mimics on a level |
| 18 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 19 | <code>    /** One mimic. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 20 | <code>    private static final class Mimic { // data holder</code> | Inline comment: data holder |
| 21 | <code>        final Point pos; // current cell (changes as it moves)</code> | Inline comment: current cell (changes as it moves) |
| 22 | <code>        final Point home; // spawn cell</code> | Inline comment: spawn cell |
| 23 | <code>        int state = PATROL; // current behaviour state</code> | Inline comment: current behaviour state |
| 24 | <code>        final Point last = new Point(); // last known player position (or noise position)</code> | Inline comment: last known player position (or noise position) |
| 25 | <code>        Point goal; // current patrol goal or search spot (null = pick a new one)</code> | Inline comment: current patrol goal or search spot (null = pick a new one) |
| 26 | <code>        int searchLeft; // search spots still to visit</code> | Inline comment: search spots still to visit |
| 27 | <code>        long nextMove; // time (ms) of the next step, 0 = not scheduled yet</code> | Inline comment: time (ms) of the next step, 0 = not scheduled yet |
| 28 | <code>        boolean disguised; // true: looks like a closed treasure chest and does not move until the player tries to open it</code> | Inline comment: true: looks like a closed treasure chest and does not move until the player tries to open it |
| 29 | <code>        int slow = 1; // step time multiplier: 1 for regular mimics, 3 for the former fake chest (3 times slower)</code> | Inline comment: step time multiplier: 1 for regular mimics, 3 for the former fake chest (3 times slower) |
| 30 | <code>        long stunUntil; // game time (ms) until which the mimic is stunned (0 = not stunned)</code> | Inline comment: game time (ms) until which the mimic is stunned (0 = not stunned) |
| 31 | <code>        long wakeEnd; // time (ms) when a just-revealed mimic starts chasing (the player's reaction time)</code> | Inline comment: time (ms) when a just-revealed mimic starts chasing (the player's reaction time) |
| 32 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 33 | <code>        Mimic(Point home) { // create a mimic</code> | Inline comment: create a mimic |
| 34 | <code>            this.home = new Point(home); // keep a copy of the spawn cell</code> | Inline comment: keep a copy of the spawn cell |
| 35 | <code>            this.pos = new Point(home); // starts at home</code> | Inline comment: starts at home |
| 36 | <code>        } // end of constructor</code> | Inline comment: end of constructor |
| 37 | <code>    } // end of Mimic</code> | Inline comment: end of Mimic |
| 38 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 39 | <code>    private static final int PATROL = 0; // wandering</code> | Inline comment: wandering |
| 40 | <code>    private static final int CHASE = 1; // sees the player</code> | Inline comment: sees the player |
| 41 | <code>    private static final int INVESTIGATE = 2; // walking to the last known position or noise</code> | Inline comment: walking to the last known position or noise |
| 42 | <code>    private static final int SEARCH = 3; // looking around that position</code> | Inline comment: looking around that position |
| 43 | <code>    private static final int WAKING = 4; // just revealed: shows itself for a moment before it chases</code> | Inline comment: just revealed: shows itself for a moment before it chases |
| 44 | <code>    private static final long WAKE_MILLIS = 500; // reaction time: a revealed mimic stands still this long so the player can run</code> | Inline comment: reaction time: a revealed mimic stands still this long so the player can run |
| 45 | <code>    private static final int DECOY_SLOW = 3; // the former fake chest moves 3 times slower than regular mimics</code> | Inline comment: the former fake chest moves 3 times slower than regular mimics |
| 46 | <code>    private static final int INTERCEPT_PERCENT = 35; // chance that a chasing mimic aims ahead of the player instead of at them</code> | Inline comment: chance that a chasing mimic aims ahead of the player instead of at them |
| 47 | <code>    private static final int GUARD_PERCENT = 30; // chance that a new patrol goal is near the chest or the exit</code> | Inline comment: chance that a new patrol goal is near the chest or the exit |
| 48 | <code>    private static final int SEARCH_SPOTS = 3; // spots checked before giving up</code> | Inline comment: spots checked before giving up |
| 49 | <code>    private static final int SEARCH_RADIUS = 4; // search area around the last known position</code> | Inline comment: search area around the last known position |
| 50 | <code>    private static final int MIN_GOAL_DISTANCE = 8; // patrol goals are at least this far away, so mimics cover the map</code> | Inline comment: patrol goals are at least this far away, so mimics cover the map |
| 51 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 52 | <code>    private final Random random = new Random(); // random choices</code> | Inline comment: random choices |
| 53 | <code>    private final List&lt;Mimic&gt; mimics = new ArrayList&lt;&gt;(); // all mimics of the level</code> | Inline comment: all mimics of the level |
| 54 | <code>    private final List&lt;Point&gt; guardSpots = new ArrayList&lt;&gt;(); // places a patrolling mimic likes to check: the chest and the exit</code> | Inline comment: places a patrolling mimic likes to check: the chest and the exit |
| 55 | <code>    private int prevPx = -1; // player column at the previous update (-1 = unknown)</code> | Inline comment: player column at the previous update (-1 = unknown) |
| 56 | <code>    private int prevPy = -1; // player row at the previous update</code> | Inline comment: player row at the previous update |
| 57 | <code>    private int heading = 0; // player's last move: 0 none, 1 right, 2 left, 3 down, 4 up</code> | Inline comment: player's last move: 0 none, 1 right, 2 left, 3 down, 4 up |
| 58 | <code>    private long clockNow; // game time of the latest update (used to check stuns)</code> | Inline comment: game time of the latest update (used to check stuns) |
| 59 | <code>    private int sightRange = 7; // how far (in cells) a mimic can see along a clear line</code> | Inline comment: how far (in cells) a mimic can see along a clear line |
| 60 | <code>    private int patrolMillis = 650; // milliseconds per step while patrolling</code> | Inline comment: milliseconds per step while patrolling |
| 61 | <code>    private int alertMillis = 420; // milliseconds per step while chasing, investigating or searching (still slower than the player)</code> | Inline comment: milliseconds per step while chasing, investigating or searching (still slower than the player) |
| 62 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 63 | <code>    /** Patrol radius (in steps from the home cell) for a level. Used by the map generator too. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 64 | <code>    public static int patrolRadiusFor(int level) { // area size</code> | Inline comment: area size |
| 65 | <code>        return level &gt;= 3 ? 7 : 6; // level 3 mimics roam a little more</code> | Inline comment: level 3 mimics roam a little more |
| 66 | <code>    } // end of patrolRadiusFor</code> | Inline comment: end of patrolRadiusFor |
| 67 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 68 | <code>    /**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 69 | <code>     * Builds a patrol route inside the area of {@code radius} steps around {@code home}:</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 70 | <code>     * edge A = farthest cell from home, edge B = the cell of the area farthest from A.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 71 | <code>     * Returns the cells from A to B (all walkable, because it is a real shortest path), or null.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 72 | <code>     */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 73 | <code>    public static List&lt;Point&gt; buildPatrolRoute(String[] map, Point home, int radius) { // compute a route</code> | Inline comment: compute a route |
| 74 | <code>        int[][] fromHome = Pathfinder.distances(map, home, false); // distances from home</code> | Inline comment: distances from home |
| 75 | <code>        Point a = null; // edge A</code> | Inline comment: edge A |
| 76 | <code>        int bestA = -1; // its distance from home</code> | Inline comment: its distance from home |
| 77 | <code>        for (int y = 0; y &lt; fromHome.length; y++) { // each row</code> | Inline comment: each row |
| 78 | <code>            for (int x = 0; x &lt; fromHome[y].length; x++) { // each column</code> | Inline comment: each column |
| 79 | <code>                int d = fromHome[y][x]; // distance of this cell</code> | Inline comment: distance of this cell |
| 80 | <code>                if (d &gt;= 0 &amp;&amp; d &lt;= radius &amp;&amp; d &gt; bestA) { // inside the area and farther than before</code> | Inline comment: inside the area and farther than before |
| 81 | <code>                    bestA = d; // remember the distance</code> | Inline comment: remember the distance |
| 82 | <code>                    a = new Point(x, y); // remember the cell</code> | Inline comment: remember the cell |
| 83 | <code>                } // end of improvement check</code> | Inline comment: end of improvement check |
| 84 | <code>            } // end of column loop</code> | Inline comment: end of column loop |
| 85 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 86 | <code>        if (a == null) return null; // no cells at all</code> | Inline comment: no cells at all |
| 87 | <code>        int[][] fromA = Pathfinder.distances(map, a, false); // distances from edge A</code> | Inline comment: distances from edge A |
| 88 | <code>        Point b = null; // edge B</code> | Inline comment: edge B |
| 89 | <code>        int bestB = -1; // its distance from A</code> | Inline comment: its distance from A |
| 90 | <code>        for (int y = 0; y &lt; fromA.length; y++) { // each row</code> | Inline comment: each row |
| 91 | <code>            for (int x = 0; x &lt; fromA[y].length; x++) { // each column</code> | Inline comment: each column |
| 92 | <code>                if (fromHome[y][x] &gt;= 0 &amp;&amp; fromHome[y][x] &lt;= radius &amp;&amp; fromA[y][x] &gt; bestB) { // inside the area, farther from A</code> | Inline comment: inside the area, farther from A |
| 93 | <code>                    bestB = fromA[y][x]; // remember the distance</code> | Inline comment: remember the distance |
| 94 | <code>                    b = new Point(x, y); // remember the cell</code> | Inline comment: remember the cell |
| 95 | <code>                } // end of improvement check</code> | Inline comment: end of improvement check |
| 96 | <code>            } // end of column loop</code> | Inline comment: end of column loop |
| 97 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 98 | <code>        if (b == null) return null; // should not happen</code> | Inline comment: should not happen |
| 99 | <code>        List&lt;Point&gt; route = new ArrayList&lt;&gt;(); // path A -&gt; B</code> | Inline comment: path A -&gt; B |
| 100 | <code>        Point cur = a; // start at A</code> | Inline comment: start at A |
| 101 | <code>        route.add(new Point(cur)); // first cell</code> | Inline comment: first cell |
| 102 | <code>        int guard = 0; // protects against endless loops</code> | Inline comment: protects against endless loops |
| 103 | <code>        while (!cur.equals(b) &amp;&amp; guard++ &lt; 200) { // walk toward B</code> | Inline comment: walk toward B |
| 104 | <code>            Point next = Pathfinder.nextStep(map, cur, b); // one step along the shortest path</code> | Inline comment: one step along the shortest path |
| 105 | <code>            if (next == null) return null; // path broke</code> | Inline comment: path broke |
| 106 | <code>            cur = next; // move on</code> | Inline comment: move on |
| 107 | <code>            route.add(new Point(cur)); // add the cell</code> | Inline comment: add the cell |
| 108 | <code>        } // end of walk loop</code> | Inline comment: end of walk loop |
| 109 | <code>        return route; // finished route</code> | Inline comment: finished route |
| 110 | <code>    } // end of buildPatrolRoute</code> | Inline comment: end of buildPatrolRoute |
| 111 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 112 | <code>    public void loadMimicsFromMap(String[] map, int level) { // read mimics from a level map</code> | Inline comment: read mimics from a level map |
| 113 | <code>        mimics.clear(); // forget old mimics</code> | Inline comment: forget old mimics |
| 114 | <code>        sightRange = level &gt;= 3 ? 8 : 7; // level 3 mimics see a little farther</code> | Inline comment: level 3 mimics see a little farther |
| 115 | <code>        patrolMillis = level &gt;= 3 ? 500 : 650; // patrol pace</code> | Inline comment: patrol pace |
| 116 | <code>        alertMillis = level &gt;= 3 ? 340 : 420; // alert pace (slightly faster than patrol)</code> | Inline comment: alert pace (slightly faster than patrol) |
| 117 | <code>        guardSpots.clear(); // forget old guard spots</code> | Inline comment: forget old guard spots |
| 118 | <code>        prevPx = -1; // forget the old player position</code> | Inline comment: forget the old player position |
| 119 | <code>        heading = 0; // no heading yet</code> | Inline comment: no heading yet |
| 120 | <code>        clockNow = 0; // fresh level</code> | Inline comment: fresh level |
| 121 | <code>        if (map == null) return; // nothing to read</code> | Inline comment: nothing to read |
| 122 | <code>        Point chestCell = GameMap.find(map, 'T'); // the chest</code> | Inline comment: the chest |
| 123 | <code>        Point exitCell = GameMap.find(map, 'E'); // the exit</code> | Inline comment: the exit |
| 124 | <code>        if (chestCell != null) guardSpots.add(chestCell); // patrols sometimes check the chest</code> | Inline comment: patrols sometimes check the chest |
| 125 | <code>        if (exitCell != null) guardSpots.add(exitCell); // and the exit, where the player must go</code> | Inline comment: and the exit, where the player must go |
| 126 | <code>        for (int r = 0; r &lt; map.length; r++) { // for each row</code> | Inline comment: for each row |
| 127 | <code>            for (int c = 0; c &lt; map[r].length(); c++) { // for each column</code> | Inline comment: for each column |
| 128 | <code>                if (map[r].charAt(c) == 'M') { // mimic symbol = spawn cell</code> | Inline comment: mimic symbol = spawn cell |
| 129 | <code>                    mimics.add(new Mimic(new Point(c, r))); // register the mimic</code> | Inline comment: register the mimic |
| 130 | <code>                } // end of symbol check</code> | Inline comment: end of symbol check |
| 131 | <code>            } // end of column loop</code> | Inline comment: end of column loop |
| 132 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 133 | <code>        addDisguised(map, chestCell, exitCell); // every level hides exactly one fake chest (it counts as one of the level's mimics)</code> | Inline comment: every level hides exactly one fake chest (it counts as one of the level's mimics) |
| 134 | <code>    } // end of loadMimicsFromMap</code> | Inline comment: end of loadMimicsFromMap |
| 135 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 136 | <code>    /**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 137 | <code>     * Hides one mimic that looks like a closed chest. It is placed in a dead end (so it can never block a route),</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 138 | <code>     * far from the start, and away from the real chest and the exit. It does not move until the player comes close.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 139 | <code>     */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 140 | <code>    private void addDisguised(String[] map, Point chestCell, Point exitCell) { // place the fake chest</code> | Inline comment: place the fake chest |
| 141 | <code>        Point start = GameMap.find(map, 'P'); // where the player starts</code> | Inline comment: where the player starts |
| 142 | <code>        if (start == null) return; // incomplete map</code> | Inline comment: incomplete map |
| 143 | <code>        int[][] fromStart = Pathfinder.distances(map, start, false); // distances from the start</code> | Inline comment: distances from the start |
| 144 | <code>        int[][] fromChest = chestCell == null ? null : Pathfinder.distances(map, chestCell, false); // distances from the real chest</code> | Inline comment: distances from the real chest |
| 145 | <code>        int[][] fromExit = exitCell == null ? null : Pathfinder.distances(map, exitCell, false); // distances from the exit</code> | Inline comment: distances from the exit |
| 146 | <code>        // Try strict rules first; if a (small) map has no such cell, relax the distances step by step so the level always gets its fake chest.</code> | Inline comment: Try strict rules first; if a (small) map has no such cell, relax the distances step by step so the level always gets its fake chest. |
| 147 | <code>        int[][] tiers = {{12, 6, 4, 1}, {8, 4, 3, 1}, {4, 2, 2, 1}, {2, 1, 2, 1}, {2, 1, 2, 2}}; // start, chest, exit distance, max open neighbours</code> | Inline comment: start, chest, exit distance, max open neighbours |
| 148 | <code>        List&lt;Point&gt; spots = new ArrayList&lt;&gt;(); // possible cells</code> | Inline comment: possible cells |
| 149 | <code>        for (int[] tier : tiers) { // strict to relaxed</code> | Inline comment: strict to relaxed |
| 150 | <code>            for (int y = 1; y &lt; map.length - 1; y++) { // each row</code> | Inline comment: each row |
| 151 | <code>                for (int x = 1; x &lt; map[y].length() - 1; x++) { // each column</code> | Inline comment: each column |
| 152 | <code>                    if (map[y].charAt(x) != '.' &#124;&#124; fromStart[y][x] &lt; tier[0]) continue; // plain floor (no bomb, coin, chest, exit, start), far from the start</code> | Inline comment: plain floor (no bomb, coin, chest, exit, start), far from the start |
| 153 | <code>                    if (fromChest != null &amp;&amp; fromChest[y][x] &lt; tier[1]) continue; // away from the real chest</code> | Inline comment: away from the real chest |
| 154 | <code>                    if (fromExit != null &amp;&amp; fromExit[y][x] &lt; tier[2]) continue; // away from the exit</code> | Inline comment: away from the exit |
| 155 | <code>                    int open = 0; // walkable neighbours</code> | Inline comment: walkable neighbours |
| 156 | <code>                    if (Pathfinder.walkable(map, x + 1, y, false)) open++; // right</code> | Inline comment: right |
| 157 | <code>                    if (Pathfinder.walkable(map, x - 1, y, false)) open++; // left</code> | Inline comment: left |
| 158 | <code>                    if (Pathfinder.walkable(map, x, y + 1, false)) open++; // down</code> | Inline comment: down |
| 159 | <code>                    if (Pathfinder.walkable(map, x, y - 1, false)) open++; // up</code> | Inline comment: up |
| 160 | <code>                    if (open &lt;= tier[3]) spots.add(new Point(x, y)); // a dead end (last tier also allows corridors)</code> | Inline comment: a dead end (last tier also allows corridors) |
| 161 | <code>                } // end of column loop</code> | Inline comment: end of column loop |
| 162 | <code>            } // end of row loop</code> | Inline comment: end of row loop |
| 163 | <code>            if (!spots.isEmpty()) break; // found cells at this strictness</code> | Inline comment: found cells at this strictness |
| 164 | <code>        } // end of tiers</code> | Inline comment: end of tiers |
| 165 | <code>        if (spots.isEmpty()) return; // practically impossible: no free floor at all</code> | Inline comment: practically impossible: no free floor at all |
| 166 | <code>        Mimic m = new Mimic(spots.get(random.nextInt(spots.size()))); // create it</code> | Inline comment: create it |
| 167 | <code>        m.disguised = true; // looks like a chest</code> | Inline comment: looks like a chest |
| 168 | <code>        m.slow = DECOY_SLOW; // and is slow once revealed</code> | Inline comment: and is slow once revealed |
| 169 | <code>        mimics.add(m); // register it</code> | Inline comment: register it |
| 170 | <code>    } // end of addDisguised</code> | Inline comment: end of addDisguised |
| 171 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 172 | <code>    /** True when no wall blocks the straight line between two cells (simple line walk). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 173 | <code>    private static boolean canSee(String[] map, Point a, Point b) { // line of sight test</code> | Inline comment: line of sight test |
| 174 | <code>        int steps = Math.max(Math.abs(b.x - a.x), Math.abs(b.y - a.y)); // number of samples along the line</code> | Inline comment: number of samples along the line |
| 175 | <code>        for (int i = 1; i &lt; steps; i++) { // each cell strictly between the two</code> | Inline comment: each cell strictly between the two |
| 176 | <code>            int x = a.x + Math.round((float) (b.x - a.x) * i / steps); // sampled column</code> | Inline comment: sampled column |
| 177 | <code>            int y = a.y + Math.round((float) (b.y - a.y) * i / steps); // sampled row</code> | Inline comment: sampled row |
| 178 | <code>            if (!Pathfinder.walkable(map, x, y, false)) return false; // a wall blocks the view</code> | Inline comment: a wall blocks the view |
| 179 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 180 | <code>        return true; // clear view</code> | Inline comment: clear view |
| 181 | <code>    } // end of canSee</code> | Inline comment: end of canSee |
| 182 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 183 | <code>    /**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 184 | <code>     * Called by the game timer (not by key presses). Moves each mimic when its step time has come.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 185 | <code>     * Returns how many mimics moved (the game plays a footstep sound when it is greater than zero).</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 186 | <code>     */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 187 | <code>    public int update(int playerX, int playerY, String[] map, long now) { // one AI update</code> | Inline comment: one AI update |
| 188 | <code>        if (mimics.isEmpty() &#124;&#124; map == null) return 0; // no mimics on this level</code> | Inline comment: no mimics on this level |
| 189 | <code>        Point player = new Point(playerX, playerY); // where the player is</code> | Inline comment: where the player is |
| 190 | <code>        if (prevPx &gt;= 0 &amp;&amp; Math.abs(playerX - prevPx) + Math.abs(playerY - prevPy) == 1) { // the player made one step</code> | Inline comment: the player made one step |
| 191 | <code>            heading = playerX &gt; prevPx ? 1 : playerX &lt; prevPx ? 2 : playerY &gt; prevPy ? 3 : 4; // remember the direction</code> | Inline comment: remember the direction |
| 192 | <code>        } // end of heading</code> | Inline comment: end of heading |
| 193 | <code>        prevPx = playerX; // remember for the next update</code> | Inline comment: remember for the next update |
| 194 | <code>        prevPy = playerY; // remember for the next update</code> | Inline comment: remember for the next update |
| 195 | <code>        clockNow = now; // remember the game time</code> | Inline comment: remember the game time |
| 196 | <code>        int moved = 0; // mimics that moved</code> | Inline comment: mimics that moved |
| 197 | <code>        for (Mimic m : mimics) { // each mimic</code> | Inline comment: each mimic |
| 198 | <code>            if (m.disguised) continue; // a fake chest never moves or reacts: only revealAt() (the player opening it) wakes it</code> | Inline comment: a fake chest never moves or reacts: only revealAt() (the player opening it) wakes it |
| 199 | <code>            if (now &lt; m.stunUntil) continue; // stunned: frozen until the stun ends, then the normal AI continues</code> | Inline comment: stunned: frozen until the stun ends, then the normal AI continues |
| 200 | <code>            if (m.state == WAKING) { // just revealed</code> | Inline comment: just revealed |
| 201 | <code>                if (now &lt; m.wakeEnd) continue; // still frozen: the reaction time</code> | Inline comment: still frozen: the reaction time |
| 202 | <code>                m.state = CHASE; // now it hunts</code> | Inline comment: now it hunts |
| 203 | <code>                m.last.setLocation(player); // from where it saw the player</code> | Inline comment: from where it saw the player |
| 204 | <code>                m.nextMove = 0; // schedule the first step normally</code> | Inline comment: schedule the first step normally |
| 205 | <code>            } // end of waking</code> | Inline comment: end of waking |
| 206 | <code>            boolean sees = Math.abs(m.pos.x - playerX) + Math.abs(m.pos.y - playerY) &lt;= sightRange // close enough</code> | Inline comment: close enough |
| 207 | <code>                &amp;&amp; canSee(map, m.pos, player); // and no wall in between</code> | Inline comment: and no wall in between |
| 208 | <code>            if (sees) { // spotted</code> | Inline comment: spotted |
| 209 | <code>                m.state = CHASE; // chase</code> | Inline comment: chase |
| 210 | <code>                m.last.setLocation(player); // remember where</code> | Inline comment: remember where |
| 211 | <code>            } else if (m.state == CHASE) { // just lost the player</code> | Inline comment: just lost the player |
| 212 | <code>                m.state = INVESTIGATE; // go to the last known position, not straight to the player</code> | Inline comment: go to the last known position, not straight to the player |
| 213 | <code>            } // end of sight check</code> | Inline comment: end of sight check |
| 214 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 215 | <code>            long interval = (m.state == PATROL ? patrolMillis : alertMillis) * m.slow; // time between steps (x3 for the former fake chest)</code> | Inline comment: time between steps (x3 for the former fake chest) |
| 216 | <code>            if (m.nextMove == 0) m.nextMove = now + interval; // first update: schedule, so the player gets a head start</code> | Inline comment: first update: schedule, so the player gets a head start |
| 217 | <code>            if (now &lt; m.nextMove) continue; // not time to step yet</code> | Inline comment: not time to step yet |
| 218 | <code>            m.nextMove = now + interval; // schedule the next step</code> | Inline comment: schedule the next step |
| 219 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 220 | <code>            if (m.state == INVESTIGATE &amp;&amp; m.pos.equals(m.last)) { // arrived</code> | Inline comment: arrived |
| 221 | <code>                m.state = SEARCH; // start searching</code> | Inline comment: start searching |
| 222 | <code>                m.searchLeft = SEARCH_SPOTS; // spots to check</code> | Inline comment: spots to check |
| 223 | <code>                m.goal = null; // pick the first spot below</code> | Inline comment: pick the first spot below |
| 224 | <code>            } // end of arrival check</code> | Inline comment: end of arrival check |
| 225 | <code>            Point next = null; // the cell to step to</code> | Inline comment: the cell to step to |
| 226 | <code>            if (m.state == CHASE) { // chasing</code> | Inline comment: chasing |
| 227 | <code>                Point target = player; // by default go straight for the player</code> | Inline comment: by default go straight for the player |
| 228 | <code>                if (Math.abs(m.pos.x - playerX) + Math.abs(m.pos.y - playerY) &gt; 3 &amp;&amp; random.nextInt(100) &lt; INTERCEPT_PERCENT) { // sometimes cut them off</code> | Inline comment: sometimes cut them off |
| 229 | <code>                    target = ahead(map, player); // aim at the cell the player is heading to</code> | Inline comment: aim at the cell the player is heading to |
| 230 | <code>                } // end of intercept</code> | Inline comment: end of intercept |
| 231 | <code>                next = Pathfinder.nextStep(map, m.pos, target); // one step toward the target</code> | Inline comment: one step toward the target |
| 232 | <code>                if (next == null) next = Pathfinder.nextStep(map, m.pos, player); // fall back to the player</code> | Inline comment: fall back to the player |
| 233 | <code>            } else if (m.state == INVESTIGATE) { // going to the last known position</code> | Inline comment: going to the last known position |
| 234 | <code>                next = Pathfinder.nextStep(map, m.pos, m.last); // one step toward it</code> | Inline comment: one step toward it |
| 235 | <code>                if (next == null) m.state = PATROL; // unreachable: give up</code> | Inline comment: unreachable: give up |
| 236 | <code>            } else if (m.state == SEARCH) { // looking around</code> | Inline comment: looking around |
| 237 | <code>                if (m.goal == null &#124;&#124; m.pos.equals(m.goal)) { // need a new spot</code> | Inline comment: need a new spot |
| 238 | <code>                    if (m.searchLeft &lt;= 0) { // search period is over</code> | Inline comment: search period is over |
| 239 | <code>                        m.state = PATROL; // resume patrolling</code> | Inline comment: resume patrolling |
| 240 | <code>                        m.goal = null; // pick a patrol goal below</code> | Inline comment: pick a patrol goal below |
| 241 | <code>                    } else { // next spot</code> | Inline comment: next spot |
| 242 | <code>                        m.searchLeft--; // one fewer to go</code> | Inline comment: one fewer to go |
| 243 | <code>                        m.goal = randomCell(map, m.last, 1, SEARCH_RADIUS); // random cell near the last known position</code> | Inline comment: random cell near the last known position |
| 244 | <code>                    } // end of spot check</code> | Inline comment: end of spot check |
| 245 | <code>                } // end of new spot</code> | Inline comment: end of new spot |
| 246 | <code>                if (m.state == SEARCH &amp;&amp; m.goal != null) next = Pathfinder.nextStep(map, m.pos, m.goal); // step to the spot</code> | Inline comment: step to the spot |
| 247 | <code>            } // end of state handling</code> | Inline comment: end of state handling |
| 248 | <code>            if (m.state == PATROL) { // patrolling (also right after a finished search)</code> | Inline comment: patrolling (also right after a finished search) |
| 249 | <code>                if (m.goal == null &#124;&#124; m.pos.equals(m.goal)) { // need a new goal</code> | Inline comment: need a new goal |
| 250 | <code>                    if (!guardSpots.isEmpty() &amp;&amp; random.nextInt(100) &lt; GUARD_PERCENT) { // sometimes guard the chest or the exit</code> | Inline comment: sometimes guard the chest or the exit |
| 251 | <code>                        m.goal = randomCell(map, guardSpots.get(random.nextInt(guardSpots.size())), 1, 5); // a cell near it</code> | Inline comment: a cell near it |
| 252 | <code>                    } // end of guard goal</code> | Inline comment: end of guard goal |
| 253 | <code>                    if (m.goal == null &#124;&#124; m.pos.equals(m.goal)) m.goal = randomCell(map, m.pos, MIN_GOAL_DISTANCE, 999); // otherwise anywhere reachable on the map</code> | Inline comment: otherwise anywhere reachable on the map |
| 254 | <code>                } // end of goal check</code> | Inline comment: end of goal check |
| 255 | <code>                if (m.goal != null) next = Pathfinder.nextStep(map, m.pos, m.goal); // one step toward the goal</code> | Inline comment: one step toward the goal |
| 256 | <code>                if (next == null) m.goal = null; // blocked: choose another goal next time</code> | Inline comment: blocked: choose another goal next time |
| 257 | <code>            } // end of patrol</code> | Inline comment: end of patrol |
| 258 | <code>            if (next != null) { // a step exists</code> | Inline comment: a step exists |
| 259 | <code>                m.pos.setLocation(next); // take it (always a walkable cell)</code> | Inline comment: take it (always a walkable cell) |
| 260 | <code>                moved++; // count it</code> | Inline comment: count it |
| 261 | <code>            } // end of step check</code> | Inline comment: end of step check |
| 262 | <code>        } // end of mimic loop</code> | Inline comment: end of mimic loop |
| 263 | <code>        return moved; // report</code> | Inline comment: report |
| 264 | <code>    } // end of update</code> | Inline comment: end of update |
| 265 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 266 | <code>    /** The cell up to 3 steps ahead of the player in the direction they last moved (the player's own cell if blocked). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 267 | <code>    private Point ahead(String[] map, Point player) { // interception target</code> | Inline comment: interception target |
| 268 | <code>        int dx = heading == 1 ? 1 : heading == 2 ? -1 : 0; // column step</code> | Inline comment: column step |
| 269 | <code>        int dy = heading == 3 ? 1 : heading == 4 ? -1 : 0; // row step</code> | Inline comment: row step |
| 270 | <code>        for (int k = 3; k &gt;= 1 &amp;&amp; (dx != 0 &#124;&#124; dy != 0); k--) { // farthest first</code> | Inline comment: farthest first |
| 271 | <code>            if (Pathfinder.walkable(map, player.x + dx * k, player.y + dy * k, false)) { // open cell</code> | Inline comment: open cell |
| 272 | <code>                return new Point(player.x + dx * k, player.y + dy * k); // intercept here</code> | Inline comment: intercept here |
| 273 | <code>            } // end of open check</code> | Inline comment: end of open check |
| 274 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 275 | <code>        return player; // no heading or blocked</code> | Inline comment: no heading or blocked |
| 276 | <code>    } // end of ahead</code> | Inline comment: end of ahead |
| 277 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 278 | <code>    /** A random cell whose path distance from the centre is between min and max (falls back to any reachable cell), or null. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 279 | <code>    private Point randomCell(String[] map, Point center, int min, int max) { // choose a goal</code> | Inline comment: choose a goal |
| 280 | <code>        int[][] d = Pathfinder.distances(map, center, false); // distances from the centre</code> | Inline comment: distances from the centre |
| 281 | <code>        List&lt;Point&gt; good = new ArrayList&lt;&gt;(); // cells in the wanted range</code> | Inline comment: cells in the wanted range |
| 282 | <code>        List&lt;Point&gt; any = new ArrayList&lt;&gt;(); // any other reachable cell</code> | Inline comment: any other reachable cell |
| 283 | <code>        for (int y = 0; y &lt; d.length; y++) { // each row</code> | Inline comment: each row |
| 284 | <code>            for (int x = 0; x &lt; d[y].length; x++) { // each column</code> | Inline comment: each column |
| 285 | <code>                if (d[y][x] &lt; 1) continue; // unreachable or the centre itself</code> | Inline comment: unreachable or the centre itself |
| 286 | <code>                if (d[y][x] &gt;= min &amp;&amp; d[y][x] &lt;= max) good.add(new Point(x, y)); // wanted range</code> | Inline comment: wanted range |
| 287 | <code>                else any.add(new Point(x, y)); // fallback</code> | Inline comment: fallback |
| 288 | <code>            } // end of column loop</code> | Inline comment: end of column loop |
| 289 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 290 | <code>        List&lt;Point&gt; pick = good.isEmpty() ? any : good; // prefer the wanted range</code> | Inline comment: prefer the wanted range |
| 291 | <code>        return pick.isEmpty() ? null : pick.get(random.nextInt(pick.size())); // random choice</code> | Inline comment: random choice |
| 292 | <code>    } // end of randomCell</code> | Inline comment: end of randomCell |
| 293 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 294 | <code>    /**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 295 | <code>     * A noise at (x, y) is heard by mimics within {@code range} PATH steps (walls matter), so far-away mimics hear nothing.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 296 | <code>     * They investigate the noise position. Returns how many mimics heard it.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 297 | <code>     */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 298 | <code>    public int hearNoise(int x, int y, String[] map, int range) { // alert nearby mimics</code> | Inline comment: alert nearby mimics |
| 299 | <code>        int heard = 0; // mimics alerted</code> | Inline comment: mimics alerted |
| 300 | <code>        if (map == null) return 0; // nothing to do</code> | Inline comment: nothing to do |
| 301 | <code>        int[][] d = Pathfinder.distances(map, new Point(x, y), false); // distances from the noise</code> | Inline comment: distances from the noise |
| 302 | <code>        for (Mimic m : mimics) { // each mimic</code> | Inline comment: each mimic |
| 303 | <code>            int dist = d[m.pos.y][m.pos.x]; // how far this mimic is</code> | Inline comment: how far this mimic is |
| 304 | <code>            if (!m.disguised &amp;&amp; m.state != WAKING &amp;&amp; clockNow &gt;= m.stunUntil &amp;&amp; dist &gt;= 0 &amp;&amp; dist &lt;= range &amp;&amp; m.state != CHASE) { // heard it and not already chasing</code> | Inline comment: heard it and not already chasing |
| 305 | <code>                m.state = INVESTIGATE; // go and look</code> | Inline comment: go and look |
| 306 | <code>                m.last.setLocation(x, y); // at the noise position</code> | Inline comment: at the noise position |
| 307 | <code>                heard++; // count it</code> | Inline comment: count it |
| 308 | <code>            } // end of hearing check</code> | Inline comment: end of hearing check |
| 309 | <code>        } // end of mimic loop</code> | Inline comment: end of mimic loop |
| 310 | <code>        return heard; // report</code> | Inline comment: report |
| 311 | <code>    } // end of hearNoise</code> | Inline comment: end of hearNoise |
| 312 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 313 | <code>    public boolean checkCollision(int playerX, int playerY) { // is a mimic on the player's cell</code> | Inline comment: is a mimic on the player's cell |
| 314 | <code>        for (Mimic m : mimics) { // for each mimic</code> | Inline comment: for each mimic |
| 315 | <code>            if (!m.disguised &amp;&amp; clockNow &gt;= m.stunUntil &amp;&amp; m.pos.x == playerX &amp;&amp; m.pos.y == playerY) { // same cell, and not a stunned or sleeping one</code> | Inline comment: same cell, and not a stunned or sleeping one |
| 316 | <code>                return true; // caught</code> | Inline comment: caught |
| 317 | <code>            } // end of match check</code> | Inline comment: end of match check |
| 318 | <code>        } // end of mimic loop</code> | Inline comment: end of mimic loop |
| 319 | <code>        return false; // safe</code> | Inline comment: safe |
| 320 | <code>    } // end of checkCollision</code> | Inline comment: end of checkCollision |
| 321 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 322 | <code>    /** True if any mimic is within {@code range} cells (Manhattan distance) of the position. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 323 | <code>    public boolean anyWithin(int x, int y, int range) { // used to decide if a footstep is audible</code> | Inline comment: used to decide if a footstep is audible |
| 324 | <code>        for (Mimic m : mimics) { // for each mimic</code> | Inline comment: for each mimic |
| 325 | <code>            if (Math.abs(m.pos.x - x) + Math.abs(m.pos.y - y) &lt;= range) { // close enough</code> | Inline comment: close enough |
| 326 | <code>                return true; // audible</code> | Inline comment: audible |
| 327 | <code>            } // end of distance check</code> | Inline comment: end of distance check |
| 328 | <code>        } // end of mimic loop</code> | Inline comment: end of mimic loop |
| 329 | <code>        return false; // too far away</code> | Inline comment: too far away |
| 330 | <code>    } // end of anyWithin</code> | Inline comment: end of anyWithin |
| 331 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 332 | <code>    public void resetToSpawn() { // send every mimic back to its spawn cell</code> | Inline comment: send every mimic back to its spawn cell |
| 333 | <code>        for (Mimic m : mimics) { // for each mimic</code> | Inline comment: for each mimic |
| 334 | <code>            if (m.disguised) continue; // a hidden fake chest stays where it is</code> | Inline comment: a hidden fake chest stays where it is |
| 335 | <code>            m.pos.setLocation(m.home); // back to the spawn cell</code> | Inline comment: back to the spawn cell |
| 336 | <code>            m.state = PATROL; // calm again</code> | Inline comment: calm again |
| 337 | <code>            m.goal = null; // new goal later</code> | Inline comment: new goal later |
| 338 | <code>            m.nextMove = 0; // reschedule (gives the player a head start)</code> | Inline comment: reschedule (gives the player a head start) |
| 339 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 340 | <code>    } // end of resetToSpawn</code> | Inline comment: end of resetToSpawn |
| 341 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 342 | <code>    public List&lt;Point&gt; getPositions() { // mimic cells for drawing</code> | Inline comment: mimic cells for drawing |
| 343 | <code>        List&lt;Point&gt; list = new ArrayList&lt;&gt;(); // collect copies</code> | Inline comment: collect copies |
| 344 | <code>        for (Mimic m : mimics) { // for each mimic</code> | Inline comment: for each mimic |
| 345 | <code>            if (!m.disguised) list.add(new Point(m.pos)); // disguised ones are drawn as chests instead</code> | Inline comment: disguised ones are drawn as chests instead |
| 346 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 347 | <code>        return Collections.unmodifiableList(list); // read-only view</code> | Inline comment: read-only view |
| 348 | <code>    } // end of getPositions</code> | Inline comment: end of getPositions |
| 349 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 350 | <code>    public List&lt;Point&gt; getDisguisedPositions() { // cells that must be drawn as ordinary closed chests</code> | Inline comment: cells that must be drawn as ordinary closed chests |
| 351 | <code>        List&lt;Point&gt; list = new ArrayList&lt;&gt;(); // collect copies</code> | Inline comment: collect copies |
| 352 | <code>        for (Mimic m : mimics) { // for each mimic</code> | Inline comment: for each mimic |
| 353 | <code>            if (m.disguised) list.add(new Point(m.pos)); // copy of its cell</code> | Inline comment: copy of its cell |
| 354 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 355 | <code>        return list; // the fake chests</code> | Inline comment: the fake chests |
| 356 | <code>    } // end of getDisguisedPositions</code> | Inline comment: end of getDisguisedPositions |
| 357 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 358 | <code>    public boolean isDisguisedAt(int x, int y) { // is a fake chest on this cell</code> | Inline comment: is a fake chest on this cell |
| 359 | <code>        for (Mimic m : mimics) { // each mimic</code> | Inline comment: each mimic |
| 360 | <code>            if (m.disguised &amp;&amp; m.pos.x == x &amp;&amp; m.pos.y == y) return true; // found</code> | Inline comment: found |
| 361 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 362 | <code>        return false; // none here</code> | Inline comment: none here |
| 363 | <code>    } // end of isDisguisedAt</code> | Inline comment: end of isDisguisedAt |
| 364 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 365 | <code>    /**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 366 | <code>     * The player tried to open the chest at (x, y). If it is a fake chest the mimic is revealed: it stands still for</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 367 | <code>     * WAKE_MILLIS (the reaction time) and then hunts. Returns true if a mimic was revealed.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 368 | <code>     */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 369 | <code>    public boolean revealAt(int x, int y, long now) { // reveal a fake chest</code> | Inline comment: reveal a fake chest |
| 370 | <code>        for (Mimic m : mimics) { // each mimic</code> | Inline comment: each mimic |
| 371 | <code>            if (m.disguised &amp;&amp; m.pos.x == x &amp;&amp; m.pos.y == y) { // this is the one</code> | Inline comment: this is the one |
| 372 | <code>                m.disguised = false; // the disguise drops</code> | Inline comment: the disguise drops |
| 373 | <code>                m.state = WAKING; // standing still for a moment</code> | Inline comment: standing still for a moment |
| 374 | <code>                m.wakeEnd = now + WAKE_MILLIS; // then the chase starts</code> | Inline comment: then the chase starts |
| 375 | <code>                return true; // revealed</code> | Inline comment: revealed |
| 376 | <code>            } // end of match</code> | Inline comment: end of match |
| 377 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 378 | <code>        return false; // that cell is not a fake chest</code> | Inline comment: that cell is not a fake chest |
| 379 | <code>    } // end of revealAt</code> | Inline comment: end of revealAt |
| 380 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 381 | <code>    /** Stuns every visible mimic within {@code radius} cells (Manhattan) of (x, y) until the given game time. Returns how many. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 382 | <code>    public int stunNear(int x, int y, int radius, long until) { // freeze nearby mimics</code> | Inline comment: freeze nearby mimics |
| 383 | <code>        int count = 0; // stunned mimics</code> | Inline comment: stunned mimics |
| 384 | <code>        for (Mimic m : mimics) { // each mimic</code> | Inline comment: each mimic |
| 385 | <code>            if (m.disguised) continue; // sleeping fake chests are not affected</code> | Inline comment: sleeping fake chests are not affected |
| 386 | <code>            if (Math.abs(m.pos.x - x) + Math.abs(m.pos.y - y) &lt;= radius) { // inside the blast</code> | Inline comment: inside the blast |
| 387 | <code>                m.stunUntil = until; // frozen until then</code> | Inline comment: frozen until then |
| 388 | <code>                count++; // count it</code> | Inline comment: count it |
| 389 | <code>            } // end of range check</code> | Inline comment: end of range check |
| 390 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 391 | <code>        return count; // report</code> | Inline comment: report |
| 392 | <code>    } // end of stunNear</code> | Inline comment: end of stunNear |
| 393 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 394 | <code>    /**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 395 | <code>     * A placed blue bomb at (x, y) goes off when a visible mimic comes within 1 cell: every visible mimic within</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 396 | <code>     * {@code radius} cells is stunned for {@code stunMillis}. Returns true if it went off (the game then removes the bomb).</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 397 | <code>     */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 398 | <code>    public boolean triggerTrap(int x, int y, long now, int radius, long stunMillis) { // check one trap</code> | Inline comment: check one trap |
| 399 | <code>        clockNow = now; // use the caller's game time</code> | Inline comment: use the caller's game time |
| 400 | <code>        boolean near = false; // a mimic is next to the bomb</code> | Inline comment: a mimic is next to the bomb |
| 401 | <code>        for (Mimic m : mimics) { // each mimic</code> | Inline comment: each mimic |
| 402 | <code>            if (!m.disguised &amp;&amp; Math.abs(m.pos.x - x) + Math.abs(m.pos.y - y) &lt;= 1) near = true; // close enough to set it off</code> | Inline comment: close enough to set it off |
| 403 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 404 | <code>        if (!near) return false; // nothing happens</code> | Inline comment: nothing happens |
| 405 | <code>        stunNear(x, y, radius, now + stunMillis); // stun everyone in the blast</code> | Inline comment: stun everyone in the blast |
| 406 | <code>        return true; // it exploded</code> | Inline comment: it exploded |
| 407 | <code>    } // end of triggerTrap</code> | Inline comment: end of triggerTrap |
| 408 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 409 | <code>    public List&lt;Point&gt; getStunnedPositions() { // stunned mimics (the game tints them)</code> | Inline comment: stunned mimics (the game tints them) |
| 410 | <code>        List&lt;Point&gt; list = new ArrayList&lt;&gt;(); // collect copies</code> | Inline comment: collect copies |
| 411 | <code>        for (Mimic m : mimics) { // each mimic</code> | Inline comment: each mimic |
| 412 | <code>            if (!m.disguised &amp;&amp; clockNow &lt; m.stunUntil) list.add(new Point(m.pos)); // still stunned</code> | Inline comment: still stunned |
| 413 | <code>        } // end of loop</code> | Inline comment: end of loop |
| 414 | <code>        return list; // result</code> | Inline comment: result |
| 415 | <code>    } // end of getStunnedPositions</code> | Inline comment: end of getStunnedPositions |
| 416 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 417 | <code>    public boolean isActive() { // are there any mimics</code> | Inline comment: are there any mimics |
| 418 | <code>        return !mimics.isEmpty(); // true if at least one</code> | Inline comment: true if at least one |
| 419 | <code>    } // end of isActive</code> | Inline comment: end of isActive |
| 420 | <code>} // end of MimicLogic</code> | Inline comment: end of MimicLogic |


## `src/logic/Pathfinder.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package logic; // package for game rules</code> | Inline comment: package for game rules |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import java.awt.Point; // integer x/y coordinate</code> | Inline comment: integer x/y coordinate |
| 4 | <code>import java.util.ArrayDeque; // queue used by the search</code> | Inline comment: queue used by the search |
| 5 | <code>import java.util.Arrays; // array fill helper</code> | Inline comment: array fill helper |
| 6 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 7 | <code>/** Breadth-first search helpers shared by map generation, move budgets and mimic AI. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 8 | <code>public final class Pathfinder { // static helper, no instances</code> | Inline comment: static helper, no instances |
| 9 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 10 | <code>    private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}; // right, left, down, up</code> | Inline comment: right, left, down, up |
| 11 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 12 | <code>    private Pathfinder() { // private: never instantiated</code> | Inline comment: private: never instantiated |
| 13 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 14 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 15 | <code>    public static boolean walkable(String[] map, int x, int y, boolean avoidBombs) { // can a cell be entered</code> | Inline comment: can a cell be entered |
| 16 | <code>        if (y &lt; 0 &#124;&#124; y &gt;= map.length &#124;&#124; x &lt; 0 &#124;&#124; x &gt;= map[y].length()) { // outside the grid</code> | Inline comment: outside the grid |
| 17 | <code>            return false; // not walkable</code> | Inline comment: not walkable |
| 18 | <code>        } // end of bounds check</code> | Inline comment: end of bounds check |
| 19 | <code>        char c = map[y].charAt(x); // symbol in the cell</code> | Inline comment: symbol in the cell |
| 20 | <code>        if (c == '#') { // wall</code> | Inline comment: wall |
| 21 | <code>            return false; // not walkable</code> | Inline comment: not walkable |
| 22 | <code>        } // end of wall check</code> | Inline comment: end of wall check |
| 23 | <code>        return !(avoidBombs &amp;&amp; c == 'B'); // bombs block only when avoidBombs is set</code> | Inline comment: bombs block only when avoidBombs is set |
| 24 | <code>    } // end of walkable</code> | Inline comment: end of walkable |
| 25 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 26 | <code>    /** Step distance from {@code from} to every cell, -1 where unreachable. Indexed [y][x]. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 27 | <code>    public static int[][] distances(String[] map, Point from, boolean avoidBombs) { // BFS distance table</code> | Inline comment: BFS distance table |
| 28 | <code>        int[][] dist = new int[map.length][]; // one row per map row</code> | Inline comment: one row per map row |
| 29 | <code>        for (int r = 0; r &lt; map.length; r++) { // for each row</code> | Inline comment: for each row |
| 30 | <code>            dist[r] = new int[map[r].length()]; // allocate the row</code> | Inline comment: allocate the row |
| 31 | <code>            Arrays.fill(dist[r], -1); // -1 means not reached</code> | Inline comment: -1 means not reached |
| 32 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 33 | <code>        if (!walkable(map, from.x, from.y, false)) { // start inside a wall</code> | Inline comment: start inside a wall |
| 34 | <code>            return dist; // nothing reachable</code> | Inline comment: nothing reachable |
| 35 | <code>        } // end of start check</code> | Inline comment: end of start check |
| 36 | <code>        ArrayDeque&lt;int[]&gt; queue = new ArrayDeque&lt;&gt;(); // cells waiting to be expanded</code> | Inline comment: cells waiting to be expanded |
| 37 | <code>        dist[from.y][from.x] = 0; // start is 0 steps from itself</code> | Inline comment: start is 0 steps from itself |
| 38 | <code>        queue.add(new int[] {from.x, from.y}); // enqueue the start</code> | Inline comment: enqueue the start |
| 39 | <code>        while (!queue.isEmpty()) { // until the search is exhausted</code> | Inline comment: until the search is exhausted |
| 40 | <code>            int[] cur = queue.poll(); // take the next cell</code> | Inline comment: take the next cell |
| 41 | <code>            for (int[] d : DIRS) { // try 4 directions</code> | Inline comment: try 4 directions |
| 42 | <code>                int nx = cur[0] + d[0]; // neighbor column</code> | Inline comment: neighbor column |
| 43 | <code>                int ny = cur[1] + d[1]; // neighbor row</code> | Inline comment: neighbor row |
| 44 | <code>                if (walkable(map, nx, ny, avoidBombs) &amp;&amp; dist[ny][nx] &lt; 0) { // enterable and not yet reached</code> | Inline comment: enterable and not yet reached |
| 45 | <code>                    dist[ny][nx] = dist[cur[1]][cur[0]] + 1; // one step farther than current</code> | Inline comment: one step farther than current |
| 46 | <code>                    queue.add(new int[] {nx, ny}); // expand it later</code> | Inline comment: expand it later |
| 47 | <code>                } // end of neighbor check</code> | Inline comment: end of neighbor check |
| 48 | <code>            } // end of direction loop</code> | Inline comment: end of direction loop |
| 49 | <code>        } // end of search loop</code> | Inline comment: end of search loop |
| 50 | <code>        return dist; // finished table</code> | Inline comment: finished table |
| 51 | <code>    } // end of distances</code> | Inline comment: end of distances |
| 52 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 53 | <code>    public static int distance(String[] map, Point from, Point to, boolean avoidBombs) { // distance between two cells</code> | Inline comment: distance between two cells |
| 54 | <code>        return distances(map, from, avoidBombs)[to.y][to.x]; // look up target in the table</code> | Inline comment: look up target in the table |
| 55 | <code>    } // end of distance</code> | Inline comment: end of distance |
| 56 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 57 | <code>    /** Next cell on a shortest path from {@code from} toward {@code to}; null if none. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 58 | <code>    public static Point nextStep(String[] map, Point from, Point to) { // one step of a chase</code> | Inline comment: one step of a chase |
| 59 | <code>        int[][] dist = distances(map, to, false); // distances measured from the target</code> | Inline comment: distances measured from the target |
| 60 | <code>        int here = dist[from.y][from.x]; // current distance to the target</code> | Inline comment: current distance to the target |
| 61 | <code>        if (here &lt;= 0) { // unreachable or already there</code> | Inline comment: unreachable or already there |
| 62 | <code>            return null; // no step</code> | Inline comment: no step |
| 63 | <code>        } // end of check</code> | Inline comment: end of check |
| 64 | <code>        for (int[] d : DIRS) { // try 4 directions</code> | Inline comment: try 4 directions |
| 65 | <code>            int nx = from.x + d[0]; // neighbor column</code> | Inline comment: neighbor column |
| 66 | <code>            int ny = from.y + d[1]; // neighbor row</code> | Inline comment: neighbor row |
| 67 | <code>            if (walkable(map, nx, ny, false) &amp;&amp; dist[ny][nx] &gt;= 0 &amp;&amp; dist[ny][nx] &lt; here) { // neighbor is closer to the target</code> | Inline comment: neighbor is closer to the target |
| 68 | <code>                return new Point(nx, ny); // step there</code> | Inline comment: step there |
| 69 | <code>            } // end of neighbor check</code> | Inline comment: end of neighbor check |
| 70 | <code>        } // end of direction loop</code> | Inline comment: end of direction loop |
| 71 | <code>        return null; // no closer cell found</code> | Inline comment: no closer cell found |
| 72 | <code>    } // end of nextStep</code> | Inline comment: end of nextStep |
| 73 | <code>} // end of Pathfinder</code> | Inline comment: end of Pathfinder |


## `src/logic/ScoreManager.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package logic; // package for game rules</code> | Inline comment: package for game rules |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>public class ScoreManager { // score and lives</code> | Inline comment: score and lives |
| 4 | <code>    public static final int START_LIVES = 4; // hearts at the start of a game (was 3, plus one extra heart)</code> | Inline comment: hearts at the start of a game (was 3, plus one extra heart) |
| 5 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 6 | <code>    private int totalScore; // points collected</code> | Inline comment: points collected |
| 7 | <code>    private int lives; // lives left</code> | Inline comment: lives left |
| 8 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 9 | <code>    public ScoreManager() { // new game state</code> | Inline comment: new game state |
| 10 | <code>        resetScore(); // set score and lives</code> | Inline comment: set score and lives |
| 11 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 12 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 13 | <code>    public final void resetScore() { // reset for a new game (final: called from the constructor)</code> | Inline comment: reset for a new game (final: called from the constructor) |
| 14 | <code>        this.totalScore = 0; // no points</code> | Inline comment: no points |
| 15 | <code>        this.lives = START_LIVES; // full lives</code> | Inline comment: full lives |
| 16 | <code>    } // end of resetScore</code> | Inline comment: end of resetScore |
| 17 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 18 | <code>    public void addTreasurePoints() { // treasure reward</code> | Inline comment: treasure reward |
| 19 | <code>        totalScore += 500; // +500 points</code> | Inline comment: +500 points |
| 20 | <code>    } // end of addTreasurePoints</code> | Inline comment: end of addTreasurePoints |
| 21 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 22 | <code>    public void addCoinPoints() { // reward for one coin</code> | Inline comment: reward for one coin |
| 23 | <code>        totalScore += 100; // +100 points</code> | Inline comment: +100 points |
| 24 | <code>    } // end of addCoinPoints</code> | Inline comment: end of addCoinPoints |
| 25 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 26 | <code>    public void addLevelClearBonus(int level) { // level clear reward</code> | Inline comment: level clear reward |
| 27 | <code>        totalScore += level * 300; // 300 points per level number</code> | Inline comment: 300 points per level number |
| 28 | <code>    } // end of addLevelClearBonus</code> | Inline comment: end of addLevelClearBonus |
| 29 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 30 | <code>    /** Removes one life. Returns true when no lives are left. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 31 | <code>    public boolean loseLife() { // subtract a life</code> | Inline comment: subtract a life |
| 32 | <code>        if (lives &gt; 0) { // never go below zero</code> | Inline comment: never go below zero |
| 33 | <code>            lives--; // remove one life</code> | Inline comment: remove one life |
| 34 | <code>        } // end of floor check</code> | Inline comment: end of floor check |
| 35 | <code>        return lives &lt;= 0; // true means health is empty</code> | Inline comment: true means health is empty |
| 36 | <code>    } // end of loseLife</code> | Inline comment: end of loseLife |
| 37 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 38 | <code>    /** Instant elimination (level 1 bombs): all hearts are lost at once. Always returns true. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 39 | <code>    public boolean eliminate() { // remove every heart</code> | Inline comment: remove every heart |
| 40 | <code>        lives = 0; // health is empty</code> | Inline comment: health is empty |
| 41 | <code>        return true; // empty</code> | Inline comment: empty |
| 42 | <code>    } // end of eliminate</code> | Inline comment: end of eliminate |
| 43 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 44 | <code>    /** Gives one heart back (the reward for escaping during the five-second countdown). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 45 | <code>    public void restoreLife() { // add one heart to an empty health bar</code> | Inline comment: add one heart to an empty health bar |
| 46 | <code>        if (lives &lt;= 0) { // only when empty</code> | Inline comment: only when empty |
| 47 | <code>            lives = 1; // one heart</code> | Inline comment: one heart |
| 48 | <code>        } // end of empty check</code> | Inline comment: end of empty check |
| 49 | <code>    } // end of restoreLife</code> | Inline comment: end of restoreLife |
| 50 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 51 | <code>    public int getLives() { // read lives</code> | Inline comment: read lives |
| 52 | <code>        return lives; // return them</code> | Inline comment: return them |
| 53 | <code>    } // end of getLives</code> | Inline comment: end of getLives |
| 54 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 55 | <code>    public int getTotalScore() { // read score</code> | Inline comment: read score |
| 56 | <code>        return totalScore; // return it</code> | Inline comment: return it |
| 57 | <code>    } // end of getTotalScore</code> | Inline comment: end of getTotalScore |
| 58 | <code>} // end of ScoreManager</code> | Inline comment: end of ScoreManager |


## `src/logic/TreasureLogic.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package logic; // package for game rules</code> | Inline comment: package for game rules |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import java.awt.Point; // integer x/y coordinate</code> | Inline comment: integer x/y coordinate |
| 4 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 5 | <code>public class TreasureLogic { // treasure state of a level</code> | Inline comment: treasure state of a level |
| 6 | <code>    private Point position; // treasure cell, null if none</code> | Inline comment: treasure cell, null if none |
| 7 | <code>    private boolean claimed; // true once picked up</code> | Inline comment: true once picked up |
| 8 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 9 | <code>    public void loadTreasureFromMap(String[] map) { // read the treasure from a level map</code> | Inline comment: read the treasure from a level map |
| 10 | <code>        position = null; // forget old treasure</code> | Inline comment: forget old treasure |
| 11 | <code>        claimed = false; // not claimed yet</code> | Inline comment: not claimed yet |
| 12 | <code>        if (map == null) return; // nothing to read</code> | Inline comment: nothing to read |
| 13 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 14 | <code>        for (int r = 0; r &lt; map.length; r++) { // for each row</code> | Inline comment: for each row |
| 15 | <code>            int c = map[r].indexOf('T'); // column of the treasure in this row</code> | Inline comment: column of the treasure in this row |
| 16 | <code>            if (c &gt;= 0) { // found</code> | Inline comment: found |
| 17 | <code>                position = new Point(c, r); // remember its cell</code> | Inline comment: remember its cell |
| 18 | <code>                return; // only one treasure per level</code> | Inline comment: only one treasure per level |
| 19 | <code>            } // end of found check</code> | Inline comment: end of found check |
| 20 | <code>        } // end of row loop</code> | Inline comment: end of row loop |
| 21 | <code>    } // end of loadTreasureFromMap</code> | Inline comment: end of loadTreasureFromMap |
| 22 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 23 | <code>    public boolean claimIfSteppedOn(int playerX, int playerY) { // pick up when the player stands on it</code> | Inline comment: pick up when the player stands on it |
| 24 | <code>        if (!claimed &amp;&amp; position != null &amp;&amp; position.x == playerX &amp;&amp; position.y == playerY) { // unclaimed and same cell</code> | Inline comment: unclaimed and same cell |
| 25 | <code>            claimed = true; // mark as picked up</code> | Inline comment: mark as picked up |
| 26 | <code>            return true; // newly claimed</code> | Inline comment: newly claimed |
| 27 | <code>        } // end of check</code> | Inline comment: end of check |
| 28 | <code>        return false; // nothing claimed</code> | Inline comment: nothing claimed |
| 29 | <code>    } // end of claimIfSteppedOn</code> | Inline comment: end of claimIfSteppedOn |
| 30 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 31 | <code>    public boolean isClaimed() { // is the treasure taken</code> | Inline comment: is the treasure taken |
| 32 | <code>        return claimed; // return the flag</code> | Inline comment: return the flag |
| 33 | <code>    } // end of isClaimed</code> | Inline comment: end of isClaimed |
| 34 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 35 | <code>    public Point getPosition() { // treasure cell for drawing</code> | Inline comment: treasure cell for drawing |
| 36 | <code>        return position; // may be null</code> | Inline comment: may be null |
| 37 | <code>    } // end of getPosition</code> | Inline comment: end of getPosition |
| 38 | <code>} // end of TreasureLogic</code> | Inline comment: end of TreasureLogic |


## `src/logic/VanishLogic.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package logic; // package for game rules</code> | Inline comment: package for game rules |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import java.awt.Point; // integer x/y coordinate</code> | Inline comment: integer x/y coordinate |
| 4 | <code>import java.util.HashMap; // cell -&gt; time lookup table</code> | Inline comment: cell -&gt; time lookup table |
| 5 | <code>import java.util.Iterator; // lets us remove entries while looping</code> | Inline comment: lets us remove entries while looping |
| 6 | <code>import java.util.Map; // map interface</code> | Inline comment: map interface |
| 7 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 8 | <code>/**</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 9 | <code> * Level 1 special rule: floor tiles vanish after the player steps OFF them.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 10 | <code> * 1. The player leaves a tile -&gt; it starts to crumble (shown cracked).</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 11 | <code> * 2. After CRUMBLE_MILLIS it vanishes and cannot be walked on.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 12 | <code> * 3. After REGROW_MILLIS it comes back, so the player can never be stuck for good.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 13 | <code> * Stepping back onto a tile that is still only crumbling saves it. The exit and the chest never vanish.</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 14 | <code> */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 15 | <code>public class VanishLogic { // vanishing tile state</code> | Inline comment: vanishing tile state |
| 16 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 17 | <code>    public static final long CRUMBLE_MILLIS = 2200; // time between leaving a tile and it vanishing</code> | Inline comment: time between leaving a tile and it vanishing |
| 18 | <code>    public static final long REGROW_MILLIS = 5000; // time a vanished tile stays gone</code> | Inline comment: time a vanished tile stays gone |
| 19 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 20 | <code>    private final Map&lt;Point, Long&gt; crumblingUntil = new HashMap&lt;&gt;(); // cell -&gt; time when it vanishes</code> | Inline comment: cell -&gt; time when it vanishes |
| 21 | <code>    private final Map&lt;Point, Long&gt; vanishedUntil = new HashMap&lt;&gt;(); // cell -&gt; time when it grows back</code> | Inline comment: cell -&gt; time when it grows back |
| 22 | <code>    private final Map&lt;Point, Boolean&gt; protectedCells = new HashMap&lt;&gt;(); // cells that never vanish</code> | Inline comment: cells that never vanish |
| 23 | <code>    private boolean enabled; // false on levels 2 and 3</code> | Inline comment: false on levels 2 and 3 |
| 24 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 25 | <code>    /** Prepares the rule for a level. Pass enabled=false for levels without vanishing tiles. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 26 | <code>    public void load(boolean enabled, Point... protect) { // reset for a new level</code> | Inline comment: reset for a new level |
| 27 | <code>        this.enabled = enabled; // remember the setting</code> | Inline comment: remember the setting |
| 28 | <code>        crumblingUntil.clear(); // no crumbling tiles</code> | Inline comment: no crumbling tiles |
| 29 | <code>        vanishedUntil.clear(); // no vanished tiles</code> | Inline comment: no vanished tiles |
| 30 | <code>        protectedCells.clear(); // forget old protected cells</code> | Inline comment: forget old protected cells |
| 31 | <code>        for (Point p : protect) { // each protected cell</code> | Inline comment: each protected cell |
| 32 | <code>            if (p != null) { // skip missing ones</code> | Inline comment: skip missing ones |
| 33 | <code>                protectedCells.put(new Point(p), true); // never vanishes</code> | Inline comment: never vanishes |
| 34 | <code>            } // end of null check</code> | Inline comment: end of null check |
| 35 | <code>        } // end of protect loop</code> | Inline comment: end of protect loop |
| 36 | <code>    } // end of load</code> | Inline comment: end of load |
| 37 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 38 | <code>    public boolean isEnabled() { // is the rule active on this level</code> | Inline comment: is the rule active on this level |
| 39 | <code>        return enabled; // flag</code> | Inline comment: flag |
| 40 | <code>    } // end of isEnabled</code> | Inline comment: end of isEnabled |
| 41 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 42 | <code>    /** Called when the player steps off a tile: the tile starts to crumble. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 43 | <code>    public void onLeave(int x, int y, long now) { // player left a cell</code> | Inline comment: player left a cell |
| 44 | <code>        if (!enabled) return; // rule not active</code> | Inline comment: rule not active |
| 45 | <code>        Point cell = new Point(x, y); // the cell that was left</code> | Inline comment: the cell that was left |
| 46 | <code>        if (protectedCells.containsKey(cell)) return; // exit and chest never vanish</code> | Inline comment: exit and chest never vanish |
| 47 | <code>        crumblingUntil.put(cell, now + CRUMBLE_MILLIS); // start the timer</code> | Inline comment: start the timer |
| 48 | <code>    } // end of onLeave</code> | Inline comment: end of onLeave |
| 49 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 50 | <code>    /** Called when the player steps onto a tile: a crumbling tile is saved while someone stands on it. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 51 | <code>    public void onEnter(int x, int y) { // player entered a cell</code> | Inline comment: player entered a cell |
| 52 | <code>        crumblingUntil.remove(new Point(x, y)); // cancel its crumble timer</code> | Inline comment: cancel its crumble timer |
| 53 | <code>    } // end of onEnter</code> | Inline comment: end of onEnter |
| 54 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 55 | <code>    public boolean isVanished(int x, int y) { // can this cell not be walked on</code> | Inline comment: can this cell not be walked on |
| 56 | <code>        return enabled &amp;&amp; vanishedUntil.containsKey(new Point(x, y)); // gone while its regrow timer runs</code> | Inline comment: gone while its regrow timer runs |
| 57 | <code>    } // end of isVanished</code> | Inline comment: end of isVanished |
| 58 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 59 | <code>    public boolean isCrumbling(int x, int y) { // is this cell about to vanish</code> | Inline comment: is this cell about to vanish |
| 60 | <code>        return enabled &amp;&amp; crumblingUntil.containsKey(new Point(x, y)); // crumble timer running</code> | Inline comment: crumble timer running |
| 61 | <code>    } // end of isCrumbling</code> | Inline comment: end of isCrumbling |
| 62 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 63 | <code>    /** Advances the timers. Returns true if any tile changed (so the screen should be redrawn). */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 64 | <code>    public boolean update(long now) { // called often by the game timer</code> | Inline comment: called often by the game timer |
| 65 | <code>        if (!enabled) return false; // nothing to do</code> | Inline comment: nothing to do |
| 66 | <code>        boolean changed = false; // did anything change</code> | Inline comment: did anything change |
| 67 | <code>        Iterator&lt;Map.Entry&lt;Point, Long&gt;&gt; crumbleIt = crumblingUntil.entrySet().iterator(); // walk crumbling tiles</code> | Inline comment: walk crumbling tiles |
| 68 | <code>        while (crumbleIt.hasNext()) { // each crumbling tile</code> | Inline comment: each crumbling tile |
| 69 | <code>            Map.Entry&lt;Point, Long&gt; entry = crumbleIt.next(); // current entry</code> | Inline comment: current entry |
| 70 | <code>            if (now &gt;= entry.getValue()) { // its time is up</code> | Inline comment: its time is up |
| 71 | <code>                vanishedUntil.put(entry.getKey(), now + REGROW_MILLIS); // it vanishes and will regrow later</code> | Inline comment: it vanishes and will regrow later |
| 72 | <code>                crumbleIt.remove(); // no longer crumbling</code> | Inline comment: no longer crumbling |
| 73 | <code>                changed = true; // redraw needed</code> | Inline comment: redraw needed |
| 74 | <code>            } // end of due check</code> | Inline comment: end of due check |
| 75 | <code>        } // end of crumbling loop</code> | Inline comment: end of crumbling loop |
| 76 | <code>        Iterator&lt;Map.Entry&lt;Point, Long&gt;&gt; vanishIt = vanishedUntil.entrySet().iterator(); // walk vanished tiles</code> | Inline comment: walk vanished tiles |
| 77 | <code>        while (vanishIt.hasNext()) { // each vanished tile</code> | Inline comment: each vanished tile |
| 78 | <code>            Map.Entry&lt;Point, Long&gt; entry = vanishIt.next(); // current entry</code> | Inline comment: current entry |
| 79 | <code>            if (now &gt;= entry.getValue()) { // regrow time reached</code> | Inline comment: regrow time reached |
| 80 | <code>                vanishIt.remove(); // the tile is back</code> | Inline comment: the tile is back |
| 81 | <code>                changed = true; // redraw needed</code> | Inline comment: redraw needed |
| 82 | <code>            } // end of due check</code> | Inline comment: end of due check |
| 83 | <code>        } // end of vanished loop</code> | Inline comment: end of vanished loop |
| 84 | <code>        return changed; // report</code> | Inline comment: report |
| 85 | <code>    } // end of update</code> | Inline comment: end of update |
| 86 | <code>} // end of VanishLogic</code> | Inline comment: end of VanishLogic |


## `src/ui/GamePreview.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package ui; // package for screens</code> | Inline comment: package for screens |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import java.awt.Color; // RGB color</code> | Inline comment: RGB color |
| 4 | <code>import java.awt.Font; // text font</code> | Inline comment: text font |
| 5 | <code>import java.awt.GridBagConstraints; // layout settings for one cell</code> | Inline comment: layout settings for one cell |
| 6 | <code>import java.awt.GridBagLayout; // grid layout manager</code> | Inline comment: grid layout manager |
| 7 | <code>import java.awt.Insets; // padding values</code> | Inline comment: padding values |
| 8 | <code>import javax.swing.JButton; // clickable button</code> | Inline comment: clickable button |
| 9 | <code>import javax.swing.JLabel; // text label</code> | Inline comment: text label |
| 10 | <code>import javax.swing.JPanel; // container</code> | Inline comment: container |
| 11 | <code>import javax.swing.JTextArea; // multi-line text box</code> | Inline comment: multi-line text box |
| 12 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 13 | <code>public final class GamePreview extends JPanel { // level overview screen</code> | Inline comment: level overview screen |
| 14 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 15 | <code>    private static final long serialVersionUID = 1L; // version id for serialization</code> | Inline comment: version id for serialization |
| 16 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 17 | <code>    public GamePreview(GameUI ui) { // build the screen</code> | Inline comment: build the screen |
| 18 | <code>        setBackground(UiStyle.BACKGROUND); // dark background</code> | Inline comment: dark background |
| 19 | <code>        setLayout(new GridBagLayout()); // center content in a grid</code> | Inline comment: center content in a grid |
| 20 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 21 | <code>        GridBagConstraints gbc = new GridBagConstraints(); // layout settings</code> | Inline comment: layout settings |
| 22 | <code>        gbc.insets = new Insets(12, 12, 12, 12); // spacing around components</code> | Inline comment: spacing around components |
| 23 | <code>        gbc.gridx = 0; // single column</code> | Inline comment: single column |
| 24 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 25 | <code>        JLabel title = new JLabel("GAME PREVIEW &amp; MAP INFO"); // heading</code> | Inline comment: heading |
| 26 | <code>        title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 32)); // big monospaced font</code> | Inline comment: big monospaced font |
| 27 | <code>        title.setForeground(Color.YELLOW); // yellow text</code> | Inline comment: yellow text |
| 28 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 29 | <code>        // The info text block follows; its lines cannot carry comments, so they are described here:</code> | Inline comment: The info text block follows; its lines cannot carry comments, so they are described here: |
| 30 | <code>        // one line per level (size, coins, mimics, damage), then bombs, moves, chest and blue bomb rules.</code> | Inline comment: one line per level (size, coins, mimics, damage), then bombs, moves, chest and blue bomb rules. |
| 31 | <code>        JTextArea info = new JTextArea("""</code> | Continuation of the current declaration/expression in `class body`. |
| 32 | <code>            LEVEL 1 (EASY): 15x15 &#124; 3 coins &#124; 1 Mimic + 1 fake chest &#124; 1 heart</code> | Continuation of the current declaration/expression in `class body`. |
| 33 | <code>            LEVEL 2 (MID):  31x25 &#124; 4 coins &#124; 1 Mimic + 1 fake chest &#124; 2 hearts</code> | Continuation of the current declaration/expression in `class body`. |
| 34 | <code>            LEVEL 3 (HARD): 35x31 &#124; 5 coins &#124; 2 Mimics + 1 fake chest &#124; 1 heart</code> | Continuation of the current declaration/expression in `class body`. |
| 35 | <code>            Each level: 20 black bombs + 5 blue bombs (25 pickups).</code> | Continuation of the current declaration/expression in `class body`. |
| 36 | <code>            Moves: L1 by route length, L2 725, L3 888.  Coin = +67 moves.</code> | Continuation of the current declaration/expression in `class body`. |
| 37 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 38 | <code>            Goal: open the real chest (T, or walk onto it) -&gt; Exit unlocks</code> | Continuation of the current declaration/expression in `class body`. |
| 39 | <code>            -&gt; reach E.  T next to a fake chest reveals a Mimic instead.</code> | Continuation of the current declaration/expression in `class body`. |
| 40 | <code>            Blue bomb: pick up, SPACE to place, stuns Mimics 3.5 s.</code> | Continuation of the current declaration/expression in `class body`. |
| 41 | <code>            Maps are random every game.</code> | Continuation of the current declaration/expression in `class body`. |
| 42 | <code>            """); // end of the info text block</code> | Continuation of the current declaration/expression in `class body`. |
| 43 | <code>        info.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16)); // monospaced bold</code> | Inline comment: monospaced bold |
| 44 | <code>        info.setForeground(Color.GREEN); // green text</code> | Inline comment: green text |
| 45 | <code>        info.setBackground(UiStyle.PANEL); // panel color</code> | Inline comment: panel color |
| 46 | <code>        info.setEditable(false); // read only</code> | Inline comment: read only |
| 47 | <code>        info.setFocusable(false); // never takes keyboard focus</code> | Inline comment: never takes keyboard focus |
| 48 | <code>        info.setMargin(new Insets(15, 15, 15, 15)); // inner padding</code> | Inline comment: inner padding |
| 49 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 50 | <code>        JButton startBtn = UiStyle.button("ENTER MAZE", 22, 240, 50, new Color(30, 120, 60)); // start button</code> | Inline comment: start button |
| 51 | <code>        startBtn.addActionListener(e -&gt; ui.showScreen("GAME")); // open the game</code> | Inline comment: open the game |
| 52 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 53 | <code>        JButton backBtn = UiStyle.button("BACK", 16, 140, 40, new Color(60, 60, 70)); // back button</code> | Inline comment: back button |
| 54 | <code>        backBtn.addActionListener(e -&gt; ui.showScreen("MENU")); // return to the menu</code> | Inline comment: return to the menu |
| 55 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 56 | <code>        gbc.gridy = 0; // row 0</code> | Inline comment: row 0 |
| 57 | <code>        add(title, gbc); // add heading</code> | Inline comment: add heading |
| 58 | <code>        gbc.gridy = 1; // row 1</code> | Inline comment: row 1 |
| 59 | <code>        add(info, gbc); // add level info</code> | Inline comment: add level info |
| 60 | <code>        gbc.gridy = 2; // row 2</code> | Inline comment: row 2 |
| 61 | <code>        add(startBtn, gbc); // add start button</code> | Inline comment: add start button |
| 62 | <code>        gbc.gridy = 3; // row 3</code> | Inline comment: row 3 |
| 63 | <code>        add(backBtn, gbc); // add back button</code> | Inline comment: add back button |
| 64 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 65 | <code>} // end of GamePreview</code> | Inline comment: end of GamePreview |


## `src/ui/GameUI.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package ui; // package for screens</code> | Inline comment: package for screens |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import audio.SoundManager; // sound effects and music</code> | Inline comment: sound effects and music |
| 4 | <code>import data.SaveManager; // high score storage</code> | Inline comment: high score storage |
| 5 | <code>import game.TreasureMaze; // the game panel</code> | Inline comment: the game panel |
| 6 | <code>import java.awt.CardLayout; // layout that shows one screen at a time</code> | Inline comment: layout that shows one screen at a time |
| 7 | <code>import java.awt.Dimension; // width/height pair</code> | Inline comment: width/height pair |
| 8 | <code>import javax.swing.JFrame; // application window</code> | Inline comment: application window |
| 9 | <code>import javax.swing.JPanel; // container</code> | Inline comment: container |
| 10 | <code>import javax.swing.SwingUtilities; // run code on the UI thread</code> | Inline comment: run code on the UI thread |
| 11 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 12 | <code>public class GameUI { // owns the window and switches screens</code> | Inline comment: owns the window and switches screens |
| 13 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 14 | <code>    private JFrame window; // the application window</code> | Inline comment: the application window |
| 15 | <code>    private JPanel mainPanel; // holds all screens</code> | Inline comment: holds all screens |
| 16 | <code>    private CardLayout cardLayout; // switches between screens</code> | Inline comment: switches between screens |
| 17 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 18 | <code>    private MainMenu mainMenu; // title screen</code> | Inline comment: title screen |
| 19 | <code>    private StartScreen startScreen; // how-to-play screen</code> | Inline comment: how-to-play screen |
| 20 | <code>    private GamePreview gamePreview; // level overview screen</code> | Inline comment: level overview screen |
| 21 | <code>    private TreasureMaze treasureMaze; // the game itself</code> | Inline comment: the game itself |
| 22 | <code>    private ResultScreen resultScreen; // end-of-game screen</code> | Inline comment: end-of-game screen |
| 23 | <code>    private SoundManager soundManager; // sound effects and background music</code> | Inline comment: sound effects and background music |
| 24 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 25 | <code>    public void showWindow() { // build and show the window</code> | Inline comment: build and show the window |
| 26 | <code>        window = new JFrame("The Last Symbol"); // window with a title</code> | Inline comment: window with a title |
| 27 | <code>        cardLayout = new CardLayout(); // screen switcher</code> | Inline comment: screen switcher |
| 28 | <code>        mainPanel = new JPanel(cardLayout); // container using the switcher</code> | Inline comment: container using the switcher |
| 29 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 30 | <code>        mainMenu = new MainMenu(this); // create the menu</code> | Inline comment: create the menu |
| 31 | <code>        startScreen = new StartScreen(this); // create the rules screen</code> | Inline comment: create the rules screen |
| 32 | <code>        gamePreview = new GamePreview(this); // create the preview screen</code> | Inline comment: create the preview screen |
| 33 | <code>        soundManager = new SoundManager(true); // load sounds and music in the background</code> | Inline comment: load sounds and music in the background |
| 34 | <code>        soundManager.startMusic(); // start the background music loop</code> | Inline comment: start the background music loop |
| 35 | <code>        treasureMaze = new TreasureMaze(this, soundManager); // create the game panel</code> | Inline comment: create the game panel |
| 36 | <code>        resultScreen = new ResultScreen(this); // create the result screen</code> | Inline comment: create the result screen |
| 37 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 38 | <code>        mainPanel.add(mainMenu, "MENU"); // register the menu</code> | Inline comment: register the menu |
| 39 | <code>        mainPanel.add(startScreen, "HOW"); // register the rules screen</code> | Inline comment: register the rules screen |
| 40 | <code>        mainPanel.add(gamePreview, "PREVIEW"); // register the preview</code> | Inline comment: register the preview |
| 41 | <code>        mainPanel.add(treasureMaze, "GAME"); // register the game</code> | Inline comment: register the game |
| 42 | <code>        mainPanel.add(resultScreen, "RESULT"); // register the results</code> | Inline comment: register the results |
| 43 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 44 | <code>        window.add(mainPanel); // put the screens in the window</code> | Inline comment: put the screens in the window |
| 45 | <code>        window.setMinimumSize(new Dimension(900, 700)); // smallest allowed size</code> | Inline comment: smallest allowed size |
| 46 | <code>        window.setExtendedState(JFrame.MAXIMIZED_BOTH); // start maximized</code> | Inline comment: start maximized |
| 47 | <code>        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // closing the window ends the program</code> | Inline comment: closing the window ends the program |
| 48 | <code>        window.setVisible(true); // show it</code> | Inline comment: show it |
| 49 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 50 | <code>        showScreen("MENU"); // start on the menu</code> | Inline comment: start on the menu |
| 51 | <code>    } // end of showWindow</code> | Inline comment: end of showWindow |
| 52 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 53 | <code>    public void showScreen(String screenName) { // switch to a screen by name</code> | Inline comment: switch to a screen by name |
| 54 | <code>        if ("MENU".equals(screenName) &amp;&amp; mainMenu != null) { // going to the menu</code> | Inline comment: going to the menu |
| 55 | <code>            mainMenu.refreshHighScore(); // update the displayed best score</code> | Inline comment: update the displayed best score |
| 56 | <code>        } // end of menu check</code> | Inline comment: end of menu check |
| 57 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 58 | <code>        if ("GAME".equals(screenName) &amp;&amp; treasureMaze != null) { // going to the game</code> | Inline comment: going to the game |
| 59 | <code>            treasureMaze.resetGameToLevel1(); // start a fresh game</code> | Inline comment: start a fresh game |
| 60 | <code>        } // end of game check</code> | Inline comment: end of game check |
| 61 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 62 | <code>        if (cardLayout != null &amp;&amp; mainPanel != null) { // window is built</code> | Inline comment: window is built |
| 63 | <code>            cardLayout.show(mainPanel, screenName); // show the requested screen</code> | Inline comment: show the requested screen |
| 64 | <code>            mainPanel.revalidate(); // recompute layout</code> | Inline comment: recompute layout |
| 65 | <code>            mainPanel.repaint(); // redraw</code> | Inline comment: redraw |
| 66 | <code>        } // end of layout check</code> | Inline comment: end of layout check |
| 67 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 68 | <code>        if ("GAME".equals(screenName) &amp;&amp; treasureMaze != null) { // going to the game</code> | Inline comment: going to the game |
| 69 | <code>            SwingUtilities.invokeLater(() -&gt; treasureMaze.requestFocusInWindow()); // give the game keyboard focus</code> | Inline comment: give the game keyboard focus |
| 70 | <code>        } // end of focus check</code> | Inline comment: end of focus check |
| 71 | <code>    } // end of showScreen</code> | Inline comment: end of showScreen |
| 72 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 73 | <code>    public void showResults(int finalScore, boolean won) { // end the game and show the result</code> | Inline comment: end the game and show the result |
| 74 | <code>        SaveManager.saveHighScore(finalScore); // store the score if it is a record</code> | Inline comment: store the score if it is a record |
| 75 | <code>        if (resultScreen != null) { // screen exists</code> | Inline comment: screen exists |
| 76 | <code>            resultScreen.setResult(finalScore, won); // fill it with the outcome</code> | Inline comment: fill it with the outcome |
| 77 | <code>        } // end of null check</code> | Inline comment: end of null check |
| 78 | <code>        showScreen("RESULT"); // display it</code> | Inline comment: display it |
| 79 | <code>    } // end of showResults</code> | Inline comment: end of showResults |
| 80 | <code>} // end of GameUI</code> | Inline comment: end of GameUI |


## `src/ui/MainMenu.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package ui; // package for screens</code> | Inline comment: package for screens |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import data.SaveManager; // high score storage</code> | Inline comment: high score storage |
| 4 | <code>import java.awt.Color; // RGB color</code> | Inline comment: RGB color |
| 5 | <code>import java.awt.Font; // text font</code> | Inline comment: text font |
| 6 | <code>import java.awt.GridBagConstraints; // layout settings for one cell</code> | Inline comment: layout settings for one cell |
| 7 | <code>import java.awt.GridBagLayout; // grid layout manager</code> | Inline comment: grid layout manager |
| 8 | <code>import java.awt.Insets; // padding values</code> | Inline comment: padding values |
| 9 | <code>import javax.swing.JButton; // clickable button</code> | Inline comment: clickable button |
| 10 | <code>import javax.swing.JLabel; // text label</code> | Inline comment: text label |
| 11 | <code>import javax.swing.JPanel; // container</code> | Inline comment: container |
| 12 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 13 | <code>public final class MainMenu extends JPanel { // title screen</code> | Inline comment: title screen |
| 14 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 15 | <code>    private static final long serialVersionUID = 1L; // version id for serialization</code> | Inline comment: version id for serialization |
| 16 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 17 | <code>    private final JLabel highScoreLabel; // shows the saved high score</code> | Inline comment: shows the saved high score |
| 18 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 19 | <code>    public MainMenu(GameUI ui) { // build the menu</code> | Inline comment: build the menu |
| 20 | <code>        setBackground(UiStyle.BACKGROUND); // dark background</code> | Inline comment: dark background |
| 21 | <code>        setLayout(new GridBagLayout()); // center content in a grid</code> | Inline comment: center content in a grid |
| 22 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 23 | <code>        GridBagConstraints gbc = new GridBagConstraints(); // layout settings</code> | Inline comment: layout settings |
| 24 | <code>        gbc.insets = new Insets(15, 15, 15, 15); // spacing around components</code> | Inline comment: spacing around components |
| 25 | <code>        gbc.gridx = 0; // single column</code> | Inline comment: single column |
| 26 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 27 | <code>        JLabel titleLabel = new JLabel("THE LAST SYMBOL"); // game title</code> | Inline comment: game title |
| 28 | <code>        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 48)); // big monospaced font</code> | Inline comment: big monospaced font |
| 29 | <code>        titleLabel.setForeground(Color.CYAN); // cyan text</code> | Inline comment: cyan text |
| 30 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 31 | <code>        highScoreLabel = new JLabel("HIGH SCORE: 0"); // placeholder text</code> | Inline comment: placeholder text |
| 32 | <code>        highScoreLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20)); // medium font</code> | Inline comment: medium font |
| 33 | <code>        highScoreLabel.setForeground(Color.YELLOW); // yellow text</code> | Inline comment: yellow text |
| 34 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 35 | <code>        Color base = new Color(40, 40, 60); // button color</code> | Inline comment: button color |
| 36 | <code>        JButton startBtn = UiStyle.button("PLAY GAME", 20, 250, 50, base); // play button</code> | Inline comment: play button |
| 37 | <code>        JButton howBtn = UiStyle.button("HOW TO PLAY", 20, 250, 50, base); // rules button</code> | Inline comment: rules button |
| 38 | <code>        JButton exitBtn = UiStyle.button("EXIT", 20, 250, 50, base); // quit button</code> | Inline comment: quit button |
| 39 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 40 | <code>        startBtn.addActionListener(e -&gt; ui.showScreen("PREVIEW")); // play opens the preview</code> | Inline comment: play opens the preview |
| 41 | <code>        howBtn.addActionListener(e -&gt; ui.showScreen("HOW")); // rules opens the how-to screen</code> | Inline comment: rules opens the how-to screen |
| 42 | <code>        exitBtn.addActionListener(e -&gt; System.exit(0)); // quit ends the program</code> | Inline comment: quit ends the program |
| 43 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 44 | <code>        gbc.gridy = 0; // row 0</code> | Inline comment: row 0 |
| 45 | <code>        add(titleLabel, gbc); // add title</code> | Inline comment: add title |
| 46 | <code>        gbc.gridy = 1; // row 1</code> | Inline comment: row 1 |
| 47 | <code>        add(highScoreLabel, gbc); // add high score</code> | Inline comment: add high score |
| 48 | <code>        gbc.gridy = 2; // row 2</code> | Inline comment: row 2 |
| 49 | <code>        add(startBtn, gbc); // add play button</code> | Inline comment: add play button |
| 50 | <code>        gbc.gridy = 3; // row 3</code> | Inline comment: row 3 |
| 51 | <code>        add(howBtn, gbc); // add rules button</code> | Inline comment: add rules button |
| 52 | <code>        gbc.gridy = 4; // row 4</code> | Inline comment: row 4 |
| 53 | <code>        add(exitBtn, gbc); // add quit button</code> | Inline comment: add quit button |
| 54 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 55 | <code>        refreshHighScore(); // show the saved score</code> | Inline comment: show the saved score |
| 56 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 57 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 58 | <code>    public void refreshHighScore() { // update the high score text</code> | Inline comment: update the high score text |
| 59 | <code>        highScoreLabel.setText("HIGH SCORE: " + SaveManager.loadHighScore()); // read file and display</code> | Inline comment: read file and display |
| 60 | <code>    } // end of refreshHighScore</code> | Inline comment: end of refreshHighScore |
| 61 | <code>} // end of MainMenu</code> | Inline comment: end of MainMenu |


## `src/ui/ResultScreen.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package ui; // package for screens</code> | Inline comment: package for screens |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import data.SaveManager; // high score storage</code> | Inline comment: high score storage |
| 4 | <code>import java.awt.Color; // RGB color</code> | Inline comment: RGB color |
| 5 | <code>import java.awt.Font; // text font</code> | Inline comment: text font |
| 6 | <code>import java.awt.GridBagConstraints; // layout settings for one cell</code> | Inline comment: layout settings for one cell |
| 7 | <code>import java.awt.GridBagLayout; // grid layout manager</code> | Inline comment: grid layout manager |
| 8 | <code>import java.awt.Insets; // padding values</code> | Inline comment: padding values |
| 9 | <code>import javax.swing.JButton; // clickable button</code> | Inline comment: clickable button |
| 10 | <code>import javax.swing.JLabel; // text label</code> | Inline comment: text label |
| 11 | <code>import javax.swing.JPanel; // container</code> | Inline comment: container |
| 12 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 13 | <code>public final class ResultScreen extends JPanel { // end-of-game screen</code> | Inline comment: end-of-game screen |
| 14 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 15 | <code>    private static final long serialVersionUID = 1L; // version id for serialization</code> | Inline comment: version id for serialization |
| 16 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 17 | <code>    private final JLabel titleLabel; // win or game over heading</code> | Inline comment: win or game over heading |
| 18 | <code>    private final JLabel scoreLabel; // final score text</code> | Inline comment: final score text |
| 19 | <code>    private final JLabel highScoreLabel; // best score text</code> | Inline comment: best score text |
| 20 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 21 | <code>    public ResultScreen(GameUI ui) { // build the screen</code> | Inline comment: build the screen |
| 22 | <code>        setBackground(UiStyle.BACKGROUND); // dark background</code> | Inline comment: dark background |
| 23 | <code>        setLayout(new GridBagLayout()); // center content in a grid</code> | Inline comment: center content in a grid |
| 24 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 25 | <code>        GridBagConstraints gbc = new GridBagConstraints(); // layout settings</code> | Inline comment: layout settings |
| 26 | <code>        gbc.insets = new Insets(15, 15, 15, 15); // spacing around components</code> | Inline comment: spacing around components |
| 27 | <code>        gbc.gridx = 0; // single column</code> | Inline comment: single column |
| 28 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 29 | <code>        titleLabel = new JLabel("THE END - RESULTS"); // placeholder heading</code> | Inline comment: placeholder heading |
| 30 | <code>        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 40)); // big monospaced font</code> | Inline comment: big monospaced font |
| 31 | <code>        titleLabel.setForeground(Color.CYAN); // cyan text</code> | Inline comment: cyan text |
| 32 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 33 | <code>        scoreLabel = new JLabel("FINAL SCORE: 0"); // placeholder score</code> | Inline comment: placeholder score |
| 34 | <code>        scoreLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24)); // medium font</code> | Inline comment: medium font |
| 35 | <code>        scoreLabel.setForeground(Color.WHITE); // white text</code> | Inline comment: white text |
| 36 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 37 | <code>        highScoreLabel = new JLabel("BEST HIGH SCORE: 0"); // placeholder best score</code> | Inline comment: placeholder best score |
| 38 | <code>        highScoreLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20)); // smaller font</code> | Inline comment: smaller font |
| 39 | <code>        highScoreLabel.setForeground(Color.YELLOW); // yellow text</code> | Inline comment: yellow text |
| 40 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 41 | <code>        JButton playAgainBtn = UiStyle.button("PLAY AGAIN", 20, 240, 50, new Color(30, 120, 60)); // replay button</code> | Inline comment: replay button |
| 42 | <code>        playAgainBtn.addActionListener(e -&gt; ui.showScreen("PREVIEW")); // back to the preview</code> | Inline comment: back to the preview |
| 43 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 44 | <code>        JButton menuBtn = UiStyle.button("MAIN MENU", 18, 240, 45, new Color(50, 50, 70)); // menu button</code> | Inline comment: menu button |
| 45 | <code>        menuBtn.addActionListener(e -&gt; ui.showScreen("MENU")); // back to the menu</code> | Inline comment: back to the menu |
| 46 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 47 | <code>        gbc.gridy = 0; // row 0</code> | Inline comment: row 0 |
| 48 | <code>        add(titleLabel, gbc); // add heading</code> | Inline comment: add heading |
| 49 | <code>        gbc.gridy = 1; // row 1</code> | Inline comment: row 1 |
| 50 | <code>        add(scoreLabel, gbc); // add score</code> | Inline comment: add score |
| 51 | <code>        gbc.gridy = 2; // row 2</code> | Inline comment: row 2 |
| 52 | <code>        add(highScoreLabel, gbc); // add best score</code> | Inline comment: add best score |
| 53 | <code>        gbc.gridy = 3; // row 3</code> | Inline comment: row 3 |
| 54 | <code>        add(playAgainBtn, gbc); // add replay button</code> | Inline comment: add replay button |
| 55 | <code>        gbc.gridy = 4; // row 4</code> | Inline comment: row 4 |
| 56 | <code>        add(menuBtn, gbc); // add menu button</code> | Inline comment: add menu button |
| 57 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 58 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 59 | <code>    public void setResult(int score, boolean won) { // fill the screen with the outcome</code> | Inline comment: fill the screen with the outcome |
| 60 | <code>        titleLabel.setText(won ? "YOU ESCAPED - ALL LEVELS CLEARED" : "GAME OVER"); // heading by outcome</code> | Inline comment: heading by outcome |
| 61 | <code>        titleLabel.setForeground(won ? Color.GREEN : Color.RED); // green win, red loss</code> | Inline comment: green win, red loss |
| 62 | <code>        scoreLabel.setText("FINAL SCORE: " + score); // show the score</code> | Inline comment: show the score |
| 63 | <code>        highScoreLabel.setText("BEST HIGH SCORE: " + SaveManager.loadHighScore()); // show the saved best</code> | Inline comment: show the saved best |
| 64 | <code>    } // end of setResult</code> | Inline comment: end of setResult |
| 65 | <code>} // end of ResultScreen</code> | Inline comment: end of ResultScreen |


## `src/ui/StartScreen.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package ui; // package for screens</code> | Inline comment: package for screens |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import java.awt.Color; // RGB color</code> | Inline comment: RGB color |
| 4 | <code>import java.awt.Font; // text font</code> | Inline comment: text font |
| 5 | <code>import java.awt.GridBagConstraints; // layout settings for one cell</code> | Inline comment: layout settings for one cell |
| 6 | <code>import java.awt.GridBagLayout; // grid layout manager</code> | Inline comment: grid layout manager |
| 7 | <code>import java.awt.Insets; // padding values</code> | Inline comment: padding values |
| 8 | <code>import javax.swing.JButton; // clickable button</code> | Inline comment: clickable button |
| 9 | <code>import javax.swing.JLabel; // text label</code> | Inline comment: text label |
| 10 | <code>import javax.swing.JPanel; // container</code> | Inline comment: container |
| 11 | <code>import javax.swing.JTextArea; // multi-line text box</code> | Inline comment: multi-line text box |
| 12 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 13 | <code>/** "How to play" screen. */</code> | Documentation/comment text; it explains nearby code and is not executed. |
| 14 | <code>public final class StartScreen extends JPanel { // rules screen</code> | Inline comment: rules screen |
| 15 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 16 | <code>    private static final long serialVersionUID = 1L; // version id for serialization</code> | Inline comment: version id for serialization |
| 17 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 18 | <code>    public StartScreen(GameUI ui) { // build the screen</code> | Inline comment: build the screen |
| 19 | <code>        setBackground(UiStyle.BACKGROUND); // dark background</code> | Inline comment: dark background |
| 20 | <code>        setLayout(new GridBagLayout()); // center content in a grid</code> | Inline comment: center content in a grid |
| 21 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 22 | <code>        GridBagConstraints gbc = new GridBagConstraints(); // layout settings</code> | Inline comment: layout settings |
| 23 | <code>        gbc.insets = new Insets(10, 10, 10, 10); // spacing around components</code> | Inline comment: spacing around components |
| 24 | <code>        gbc.gridx = 0; // single column</code> | Inline comment: single column |
| 25 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 26 | <code>        JLabel title = new JLabel("HOW TO PLAY"); // heading</code> | Inline comment: heading |
| 27 | <code>        title.setFont(new Font(Font.MONOSPACED, Font.BOLD, 36)); // big monospaced font</code> | Inline comment: big monospaced font |
| 28 | <code>        title.setForeground(Color.CYAN); // cyan text</code> | Inline comment: cyan text |
| 29 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 30 | <code>        // The rules text block follows; its lines cannot carry comments, so they are described here:</code> | Inline comment: The rules text block follows; its lines cannot carry comments, so they are described here: |
| 31 | <code>        // 8 numbered rules: keys, goal, coins, chest controls, hearts and countdown, mimic counts and damage, bombs, move limits.</code> | Inline comment: 8 numbered rules: keys, goal, coins, chest controls, hearts and countdown, mimic counts and damage, bombs, move limits. |
| 32 | <code>        JTextArea rules = new JTextArea("""</code> | Continuation of the current declaration/expression in `class body`. |
| 33 | <code>            1. Move: WASD or Arrows.  R restart level (-1 heart).</code> | Continuation of the current declaration/expression in `class body`. |
| 34 | <code>               M sound on/off.  Esc pause menu.</code> | Continuation of the current declaration/expression in `class body`. |
| 35 | <code>            2. GOAL: open the REAL treasure chest (+500).  That unlocks</code> | Continuation of the current declaration/expression in `class body`. |
| 36 | <code>               the Exit (E).  Reach E to finish the level.  (Collecting</code> | Continuation of the current declaration/expression in `class body`. |
| 37 | <code>               every coin also unlocks E.)</code> | Continuation of the current declaration/expression in `class body`. |
| 38 | <code>            3. Coins are optional: +100 points and +67 moves each.</code> | Continuation of the current declaration/expression in `class body`. |
| 39 | <code>            4. CHEST CONTROLS: walk onto the real chest OR press T next</code> | Continuation of the current declaration/expression in `class body`. |
| 40 | <code>               to it to open it.  T next to a chest that is really a</code> | Continuation of the current declaration/expression in `class body`. |
| 41 | <code>               MIMIC reveals the Mimic: no treasure, no unlock, and it</code> | Continuation of the current declaration/expression in `class body`. |
| 42 | <code>               freezes 0.5 s so you can run.  Fake chests look exactly</code> | Continuation of the current declaration/expression in `class body`. |
| 43 | <code>               like real ones.  T with no chest next to you does nothing.</code> | Continuation of the current declaration/expression in `class body`. |
| 44 | <code>            5. Hearts: 4.  At 0 a 5 second countdown starts: reach the</code> | Continuation of the current declaration/expression in `class body`. |
| 45 | <code>               open exit in time (1 heart back) or the game ends.</code> | Continuation of the current declaration/expression in `class body`. |
| 46 | <code>            6. MIMICS (hearts lost per catch):</code> | Continuation of the current declaration/expression in `class body`. |
| 47 | <code>               L1: 1 Mimic + 1 fake chest (1)   L2: 1 + 1 fake (2)</code> | Continuation of the current declaration/expression in `class body`. |
| 48 | <code>               L3: 2 Mimics + 1 fake chest (1)  Fake ones move 3x slower.</code> | Continuation of the current declaration/expression in `class body`. |
| 49 | <code>            7. BOMBS: 20 black (cost a heart, level 1: eliminate you)</code> | Continuation of the current declaration/expression in `class body`. |
| 50 | <code>               and 5 blue per level.  Walk over a blue bomb to pick it up,</code> | Continuation of the current declaration/expression in `class body`. |
| 51 | <code>               SPACE places it: it stuns Mimics within 4 cells for 3.5 s.</code> | Continuation of the current declaration/expression in `class body`. |
| 52 | <code>            8. MOVES: level 1 by route length, level 2 = 725, level 3 = 888.</code> | Continuation of the current declaration/expression in `class body`. |
| 53 | <code>               Out of moves: -1 heart.  Level 1 floor tiles crumble.</code> | Continuation of the current declaration/expression in `class body`. |
| 54 | <code>            """); // end of the rules text block</code> | Continuation of the current declaration/expression in `class body`. |
| 55 | <code>        rules.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14)); // readable font</code> | Inline comment: readable font |
| 56 | <code>        rules.setForeground(Color.WHITE); // white text</code> | Inline comment: white text |
| 57 | <code>        rules.setBackground(UiStyle.PANEL); // panel color</code> | Inline comment: panel color |
| 58 | <code>        rules.setEditable(false); // read only</code> | Inline comment: read only |
| 59 | <code>        rules.setFocusable(false); // never takes keyboard focus</code> | Inline comment: never takes keyboard focus |
| 60 | <code>        rules.setMargin(new Insets(15, 15, 15, 15)); // inner padding</code> | Inline comment: inner padding |
| 61 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 62 | <code>        JButton backBtn = UiStyle.button("BACK TO MENU", 18, 220, 45, new Color(40, 40, 60)); // back button</code> | Inline comment: back button |
| 63 | <code>        backBtn.addActionListener(e -&gt; ui.showScreen("MENU")); // return to the menu</code> | Inline comment: return to the menu |
| 64 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 65 | <code>        gbc.gridy = 0; // row 0</code> | Inline comment: row 0 |
| 66 | <code>        add(title, gbc); // add heading</code> | Inline comment: add heading |
| 67 | <code>        gbc.gridy = 1; // row 1</code> | Inline comment: row 1 |
| 68 | <code>        add(rules, gbc); // add rules</code> | Inline comment: add rules |
| 69 | <code>        gbc.gridy = 2; // row 2</code> | Inline comment: row 2 |
| 70 | <code>        add(backBtn, gbc); // add back button</code> | Inline comment: add back button |
| 71 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 72 | <code>} // end of StartScreen</code> | Inline comment: end of StartScreen |


## `src/ui/UiStyle.java` — every source line

| Line | Exact source | Explanation |
|---:|---|---|
| 1 | <code>package ui; // package for screens</code> | Inline comment: package for screens |
| 2 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 3 | <code>import java.awt.Color; // RGB color</code> | Inline comment: RGB color |
| 4 | <code>import java.awt.Dimension; // width/height pair</code> | Inline comment: width/height pair |
| 5 | <code>import java.awt.Font; // text font</code> | Inline comment: text font |
| 6 | <code>import javax.swing.JButton; // clickable button</code> | Inline comment: clickable button |
| 7 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 8 | <code>final class UiStyle { // shared colors and button factory</code> | Inline comment: shared colors and button factory |
| 9 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 10 | <code>    static final Color BACKGROUND = new Color(15, 15, 25); // screen background</code> | Inline comment: screen background |
| 11 | <code>    static final Color PANEL = new Color(25, 25, 35); // text box background</code> | Inline comment: text box background |
| 12 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 13 | <code>    private UiStyle() { // private: never instantiated</code> | Inline comment: private: never instantiated |
| 14 | <code>    } // end of constructor</code> | Inline comment: end of constructor |
| 15 | <code>&nbsp;</code> | Blank separator; it has no runtime effect and improves readability. |
| 16 | <code>    static JButton button(String text, int fontSize, int width, int height, Color background) { // build a styled button</code> | Inline comment: build a styled button |
| 17 | <code>        JButton btn = new JButton(text); // create the button</code> | Inline comment: create the button |
| 18 | <code>        btn.setFont(new Font(Font.MONOSPACED, Font.BOLD, fontSize)); // monospaced bold text</code> | Inline comment: monospaced bold text |
| 19 | <code>        btn.setPreferredSize(new Dimension(width, height)); // fixed size</code> | Inline comment: fixed size |
| 20 | <code>        btn.setFocusPainted(false); // no focus outline</code> | Inline comment: no focus outline |
| 21 | <code>        btn.setOpaque(true); // draw the background color</code> | Inline comment: draw the background color |
| 22 | <code>        btn.setBackground(background); // set the color</code> | Inline comment: set the color |
| 23 | <code>        btn.setForeground(Color.WHITE); // white text</code> | Inline comment: white text |
| 24 | <code>        return btn; // finished button</code> | Inline comment: finished button |
| 25 | <code>    } // end of button</code> | Inline comment: end of button |
| 26 | <code>} // end of UiStyle</code> | Inline comment: end of UiStyle |

[README.md](https://github.com/user-attachments/files/33710539/README.md)
