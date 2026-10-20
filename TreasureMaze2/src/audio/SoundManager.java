package audio; // package for sound

import data.AssetPaths; // finds the assets folder
import java.io.File; // file path
import java.io.IOException; // thrown on read problems
import java.util.Map; // map interface
import java.util.concurrent.ConcurrentHashMap; // map safe to use from two threads
import javax.sound.sampled.AudioFormat; // describes a sound format
import javax.sound.sampled.AudioInputStream; // stream of sound data
import javax.sound.sampled.AudioSystem; // entry point of Java Sound
import javax.sound.sampled.Clip; // sound held in memory that can be replayed
import javax.sound.sampled.DataLine; // describes a line type
import javax.sound.sampled.FloatControl; // volume control
import javax.sound.sampled.LineUnavailableException; // thrown when no audio output exists
import javax.sound.sampled.UnsupportedAudioFileException; // thrown for unknown file types

/**
 * Plays sound effects and the looping background music.
 * Files load on a background thread, so the window never freezes.
 * If the computer has no audio output the game keeps running silently.
 */
public final class SoundManager { // sound controller

    // Sound effect names; each one is the file assets/sfx/<name>.wav
    private static final String[] SFX_NAMES = {"step", "mimic_step", "bomb", "mimic_catch", "chest", "coin", "tick"}; // all effects
    private static final String MUSIC_FILE = "music/alex-morgan-video-game-pixel-chiptune-music-583271.wav"; // background music

    private final Map<String, Clip> clips = new ConcurrentHashMap<>(); // effect name -> loaded clip
    private final Map<String, Integer> playCounts = new ConcurrentHashMap<>(); // effect name -> times requested (for testing)
    private volatile Clip music; // background music clip (null until loaded)
    private volatile boolean musicWanted; // true after startMusic() was called
    private volatile boolean muted; // true when the player pressed M
    private volatile boolean musicPaused; // true while the game is paused (music waits, the mute setting is untouched)
    private volatile boolean audioAvailable; // true if this computer has an audio output

    /** @param loadFiles false skips loading (used by tests that only need the counters) */
    public SoundManager(boolean loadFiles) { // create the manager
        if (loadFiles) { // loading wanted
            Thread loader = new Thread(this::loadAll, "audio-loader"); // background thread
            loader.setDaemon(true); // do not keep the program alive for it
            loader.start(); // load files without blocking the window
        } // end of loadFiles check
    } // end of constructor

    private void loadAll() { // runs on the loader thread
        try { // any audio problem must not crash the game
            AudioFormat probe = new AudioFormat(44100f, 16, 1, true, false); // typical effect format
            if (!AudioSystem.isLineSupported(new DataLine.Info(Clip.class, probe))) { // no audio output device
                audioAvailable = false; // stay silent
                return; // nothing more to do
            } // end of device check
            audioAvailable = true; // sound can work
            for (String name : SFX_NAMES) { // each effect
                Clip clip = openClip(AssetPaths.file("sfx/" + name + ".wav")); // load it
                if (clip != null) { // loaded
                    clips.put(name, clip); // keep it for later
                } // end of loaded check
            } // end of effect loop
            Clip bgm = openClip(AssetPaths.file(MUSIC_FILE)); // load the music (large file)
            if (bgm != null) { // loaded
                setGain(bgm, -6f); // a bit quieter than the effects
                music = bgm; // publish it
                applyMusicState(); // start it if the game already asked for music
            } // end of music loaded check
        } catch (RuntimeException e) { // unexpected audio failure
            audioAvailable = false; // give up on sound
        } // end of try/catch
    } // end of loadAll

    private Clip openClip(File file) { // load one WAV file into a Clip
        if (!file.isFile()) return null; // missing file: skip
        try (AudioInputStream in = AudioSystem.getAudioInputStream(file)) { // open the WAV
            Clip clip = AudioSystem.getClip(); // create an empty clip
            clip.open(in); // copy the sound data into it
            return clip; // ready to play
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException | IllegalArgumentException e) { // cannot load
            return null; // skip this sound
        } // end of try/catch
    } // end of openClip

    private void setGain(Clip clip, float decibels) { // change a clip's volume
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) { // volume control exists
            FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN); // get it
            float safe = Math.max(control.getMinimum(), Math.min(control.getMaximum(), decibels)); // stay inside the allowed range
            control.setValue(safe); // apply
        } // end of control check
    } // end of setGain

    /** Plays one effect once. Returns immediately (Clip.start does not block). */
    // Sound effects are loaded once into a table (name -> Clip). play() rewinds the clip and starts it.
    // The count of requests is stored too, which is how the tests check that each event plays exactly once.
    public void play(String name) { // request an effect
        playCounts.merge(name, 1, Integer::sum); // count the request
        Clip clip = clips.get(name); // find the clip
        if (clip == null || muted) return; // not loaded or muted: stay silent
        clip.stop(); // stop it if it is still playing from last time
        clip.setFramePosition(0); // rewind to the start
        clip.start(); // play asynchronously
    } // end of play

    /** How many times an effect was requested (used by the tests). */
    public int getPlayCount(String name) { // read a counter
        return playCounts.getOrDefault(name, 0); // 0 if never played
    } // end of getPlayCount

    /** Starts the looping background music (safe to call before loading has finished). */
    public void startMusic() { // begin music
        musicWanted = true; // remember the request
        applyMusicState(); // start it if it is already loaded
    } // end of startMusic

    /** Pauses the music together with the game (does not change the mute setting). */
    public void pauseMusic() { // called when the pause menu opens
        musicPaused = true; // remember
        applyMusicState(); // stop the clip (its position is kept)
    } // end of pauseMusic

    /** Continues the music after a pause (stays silent if the player muted it). */
    public void resumeMusic() { // called when the game resumes or the main menu opens
        musicPaused = false; // remember
        applyMusicState(); // start again if wanted and not muted (the isRunning check prevents a second playback)
    } // end of resumeMusic

    /** Toggles all sound on/off. Returns true if sound is now muted. */
    public boolean toggleMute() { // M key
        muted = !muted; // flip the flag
        applyMusicState(); // start or stop the music to match
        return muted; // report the new state
    } // end of toggleMute

    private void applyMusicState() { // make the music match musicWanted and muted
        Clip bgm = music; // local copy
        if (bgm == null) return; // not loaded yet
        if (musicWanted && !muted && !musicPaused) { // should be playing
            if (!bgm.isRunning()) { // not playing yet
                bgm.loop(Clip.LOOP_CONTINUOUSLY); // loop forever
            } // end of running check
        } else { // should be silent
            bgm.stop(); // stop playing
        } // end of state check
    } // end of applyMusicState

    public boolean isAudioAvailable() { // does this computer have audio output
        return audioAvailable; // flag set by the loader thread
    } // end of isAudioAvailable
} // end of SoundManager
