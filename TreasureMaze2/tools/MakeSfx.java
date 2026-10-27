import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public class MakeSfx {

    static final int RATE = 44100;
    static final File OUT_DIR = new File("assets/sfx");
    static Random rng = new Random(7);

    public static void main(String[] args) {
        try {
            if (!OUT_DIR.exists()) {
                OUT_DIR.mkdirs();
            }

            makeStep();
            makeMimicStep();
            makeBomb();
            makeMimicCatch();
            makeChest();
            makeCoin();
            makeTick();

            System.out.println("All sound effects written.");
        } catch (Exception e) {
            System.out.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static void makeStep() throws IOException {
        double[] t = tAxis(0.09);
        double[] step = new double[t.length];
        double[] noise = gaussianNoise(t.length);
        double[] lp = lowpass(noise, 24);
        for (int i = 0; i < t.length; i++) {
            step[i] = Math.sin(2 * Math.PI * 110 * t[i]) * Math.exp(-t[i] * 45)
                    + lp[i] * Math.exp(-t[i] * 60) * 0.8;
        }
        save("step", fade(step, 0.004, 0.02), 0.55);
    }

    static void makeMimicStep() throws IOException {
        double[] t = tAxis(0.13);
        double[] mstep = new double[t.length];
        double[] noise = gaussianNoise(t.length);
        double[] lp = lowpass(noise, 40);
        for (int i = 0; i < t.length; i++) {
            mstep[i] = Math.sin(2 * Math.PI * 70 * t[i]) * Math.exp(-t[i] * 28)
                     + lp[i] * Math.exp(-t[i] * 35) * 0.9;
            mstep[i] += 0.4 * Math.sin(2 * Math.PI * 150 * t[i]) * Math.exp(-t[i] * 60);
        }
        save("mimic_step", fade(mstep, 0.004, 0.02), 0.6);
    }

    static void makeBomb() throws IOException {
        double[] t = tAxis(0.8);
        int n = t.length;
        double[] noise = gaussianNoise(n);
        double[] dull = new double[n];
        int[] ks = {2, 6, 14, 30};
        for (int seg = 0; seg < 4; seg++) {
            double[] lp = lowpass(noise, ks[seg]);
            int start = n * seg / 4;
            int end = n * (seg + 1) / 4;
            for (int i = start; i < end; i++) {
                dull[i] = lp[i];
            }
        }
        double[] freqRamp = linspace(150, 38, n);
        double[] cum = cumsum(freqRamp);
        double[] bomb = new double[n];
        for (int i = 0; i < n; i++) {
            double boom = Math.sin(2 * Math.PI * cum[i] / RATE);
            bomb[i] = (dull[i] * 1.2 + boom * 1.0) * Math.exp(-t[i] * 4.5);
        }
        save("bomb", fade(bomb, 0.002, 0.08), 0.9);
    }

    static void makeMimicCatch() throws IOException {
        double[] t = tAxis(0.65);
        int n = t.length;
        double[] freq = linspace(260, 70, n);
        double[] phase = new double[n];
        double sum = 0;
        for (int i = 0; i < n; i++) {
            sum += freq[i];
            phase[i] = sum / RATE;
        }
        double[] saw = new double[n];
        for (int i = 0; i < n; i++) {
            saw[i] = 2 * (phase[i] - Math.floor(phase[i] + 0.5));
        }
        double[] noise = gaussianNoise(n);
        double[] lp = lowpass(noise, 4);
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            double growl = saw[i] * (0.6 + 0.4 * Math.sin(2 * Math.PI * 28 * t[i])) * Math.exp(-t[i] * 3.0);
            double bite = lp[i] * Math.exp(-t[i] * 18) * 0.7;
            result[i] = growl + bite;
        }
        save("mimic_catch", fade(result, 0.003, 0.1), 0.85);
    }

    static void makeChest() throws IOException {
        double[] t1 = tAxis(0.22);
        int n1 = t1.length;
        double[] creakF = new double[n1];
        for (int i = 0; i < n1; i++) {
            creakF[i] = 180 + 120 * Math.sin(2 * Math.PI * 9 * t1[i]);
        }
        double[] cum = cumsum(creakF);
        double[] creak = new double[n1];
        for (int i = 0; i < n1; i++) {
            creak[i] = Math.sin(cum[i] * 2 * Math.PI / RATE)
                     * (0.5 + 0.5 * Math.sin(2 * Math.PI * 31 * t1[i]))
                     * Math.exp(-t1[i] * 6);
        }

        int chimeLen = (int) (RATE * 0.55);
        double[] chime = new double[chimeLen];
        double[] fullT = tAxis(0.55);
        double[] freqs = {523.25, 659.25, 783.99, 1046.5};
        for (int i = 0; i < freqs.length; i++) {
            double f = freqs[i];
            int start = (int) (RATE * 0.07 * i);
            int len = chimeLen - start;
            for (int j = 0; j < len; j++) {
                double tt = fullT[j];
                double tone = (Math.sin(2 * Math.PI * f * tt) + 0.35 * Math.sin(2 * Math.PI * 2 * f * tt))
                            * Math.exp(-tt * 7);
                chime[start + j] += tone;
            }
        }

        double[] combined = new double[n1 + chimeLen];
        for (int i = 0; i < n1; i++) combined[i] = creak[i] * 0.7;
        for (int i = 0; i < chimeLen; i++) combined[n1 + i] = chime[i];

        save("chest", fade(combined, 0.004, 0.02), 0.8);
    }

    static void makeCoin() throws IOException {
        double[] t = tAxis(0.22);
        int n = t.length;
        double[] coin = new double[n];
        for (int i = 0; i < n; i++) {
            double val = (t[i] < 0.07)
                    ? Math.signum(Math.sin(2 * Math.PI * 988 * t[i]))
                    : Math.signum(Math.sin(2 * Math.PI * 1319 * t[i]));
            coin[i] = val * 0.5;
        }
        double[] lp = lowpass(coin, 2);
        for (int i = 0; i < n; i++) {
            coin[i] = lp[i] * Math.exp(-t[i] * 11);
        }
        save("coin", fade(coin, 0.002, 0.05), 0.6);
    }

    static void makeTick() throws IOException {
        double[] t = tAxis(0.07);
        double[] tick = new double[t.length];
        for (int i = 0; i < t.length; i++) {
            tick[i] = Math.sin(2 * Math.PI * 880 * t[i]) * Math.exp(-t[i] * 40);
        }
        save("tick", fade(tick, 0.002, 0.02), 0.5);
    }

    static double[] tAxis(double seconds) {
        int n = (int) (RATE * seconds);
        double[] t = new double[n];
        for (int i = 0; i < n; i++) t[i] = (double) i / RATE;
        return t;
    }

    static double[] gaussianNoise(int n) {
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = rng.nextGaussian();
        return x;
    }

    static double[] lowpass(double[] x, int k) {
        if (k <= 1) return x.clone();
        int n = x.length;
        double[] out = new double[n];
        int offset = (k - 1) / 2;
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = 0; j < k; j++) {
                int idx = i + offset - j;
                if (idx >= 0 && idx < n) sum += x[idx];
            }
            out[i] = sum / k;
        }
        return out;
    }

    static double[] fade(double[] x, double attack, double release) {
        int n = x.length;
        int a = Math.max(1, (int) (RATE * attack));
        int r = Math.max(1, (int) (RATE * release));
        double[] env = new double[n];
        for (int i = 0; i < n; i++) env[i] = 1.0;
        for (int i = 0; i < a && i < n; i++) {
            env[i] = (a == 1) ? 1.0 : (double) i / (a - 1);
        }
        for (int i = 0; i < r; i++) {
            int idx = n - r + i;
            if (idx >= 0 && idx < n) {
                env[idx] = (r == 1) ? 0.0 : 1.0 - (double) i / (r - 1);
            }
        }
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = x[i] * env[i];
        return out;
    }

    static double[] linspace(double start, double end, int n) {
        double[] out = new double[n];
        if (n == 1) {
            out[0] = start;
            return out;
        }
        double step = (end - start) / (n - 1);
        for (int i = 0; i < n; i++) out[i] = start + step * i;
        return out;
    }

    static double[] cumsum(double[] x) {
        double[] out = new double[x.length];
        double sum = 0;
        for (int i = 0; i < x.length; i++) {
            sum += x[i];
            out[i] = sum;
        }
        return out;
    }

    static void save(String name, double[] x, double peak) throws IOException {
        double maxAbs = 0;
        for (double v : x) {
            double av = Math.abs(v);
            if (av > maxAbs) maxAbs = av;
        }
        double scale = peak / (maxAbs + 1e-9);

        short[] data = new short[x.length];
        for (int i = 0; i < x.length; i++) {
            double v = x[i] * scale * 32767.0;
            if (v > 32767) v = 32767;
            if (v < -32768) v = -32768;
            data[i] = (short) Math.round(v);
        }

        writeWav(new File(OUT_DIR, name + ".wav"), data);
        double seconds = Math.round((x.length / (double) RATE) * 100.0) / 100.0;
        System.out.println("wrote " + name + ".wav " + seconds + " s");
    }

    static void writeWav(File file, short[] data) throws IOException {
        byte[] bytes = new byte[data.length * 2];
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (short s : data) bb.putShort(s);

        AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             AudioInputStream ais = new AudioInputStream(bais, format, data.length)) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);
        }
    }
}
