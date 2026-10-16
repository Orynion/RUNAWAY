package com.orynion.runaway;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import java.util.Random;

/**
 * Procedural audio synthesizer using Android AudioTrack.
 * Generates all sound effects (gunfire, hits, reloads, explosions, train clatter)
 * directly from mathematical waveforms and noise — 100% offline, zero asset files.
 */
public class SoundManager {
    private static final int SAMPLE_RATE = 44100;

    private AudioTrack trackGunshot;
    private AudioTrack trackHitMarker;
    private AudioTrack trackReloadStart;
    private AudioTrack trackReloadComplete;
    private AudioTrack trackEmptyClick;
    private AudioTrack trackExplosion;
    private AudioTrack trackTrainChug;

    private boolean initialized = false;
    private final Random rand = new Random();

    public SoundManager() {
        new Thread(() -> {
            try {
                initAudio();
                initialized = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void initAudio() {
        trackGunshot = createTrack(generateGunshot());
        trackHitMarker = createTrack(generateHitMarker());
        trackReloadStart = createTrack(generateClick(800.0f, 0.08f));
        trackReloadComplete = createTrack(generateReloadLatch());
        trackEmptyClick = createTrack(generateClick(1400.0f, 0.04f));
        trackExplosion = createTrack(generateExplosion());
        trackTrainChug = createTrack(generateTrainChug());
    }

    private AudioTrack createTrack(short[] samples) {
        int bufferSize = samples.length * 2; // 16-bit = 2 bytes per sample

        AudioTrack track;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_GAME)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
        } else {
            track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize, AudioTrack.MODE_STATIC);
        }

        track.write(samples, 0, samples.length);
        return track;
    }

    private void playTrack(AudioTrack track) {
        if (!initialized || track == null) return;
        try {
            if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop();
            }
            track.reloadStaticData();
            track.play();
        } catch (Exception e) {
            // Ignore concurrent play issues
        }
    }

    public void playGunshot() {
        playTrack(trackGunshot);
    }

    public void playHitMarker() {
        playTrack(trackHitMarker);
    }

    public void playReloadStart() {
        playTrack(trackReloadStart);
    }

    public void playReloadComplete() {
        playTrack(trackReloadComplete);
    }

    public void playEmptyClick() {
        playTrack(trackEmptyClick);
    }

    public void playTargetExplosion() {
        playTrack(trackExplosion);
    }

    public void playTrainChug() {
        playTrack(trackTrainChug);
    }

    // --- Waveform Generators ---

    private short[] generateGunshot() {
        int length = (int) (SAMPLE_RATE * 0.28f);
        short[] samples = new short[length];

        for (int i = 0; i < length; i++) {
            float t = (float) i / length;
            // Exponential volume decay
            float env = (float) Math.exp(-t * 14.0);

            // Punchy bass transient drop from 180Hz to 45Hz
            float freq = 180.0f * (1.0f - t) + 45.0f;
            float bass = (float) Math.sin(2.0 * Math.PI * freq * (i / (float) SAMPLE_RATE));

            // White noise crack
            float noise = (rand.nextFloat() * 2.0f - 1.0f);

            float mix = (bass * 0.55f + noise * 0.45f) * env;
            samples[i] = (short) (Math.max(-1.0f, Math.min(1.0f, mix)) * 32767);
        }
        return samples;
    }

    private short[] generateHitMarker() {
        int length = (int) (SAMPLE_RATE * 0.12f);
        short[] samples = new short[length];

        for (int i = 0; i < length; i++) {
            float t = (float) i / length;
            float env = (float) Math.exp(-t * 22.0);

            // High metallic dual-tone chime (1600Hz + 2400Hz)
            double tSec = i / (double) SAMPLE_RATE;
            float tone1 = (float) Math.sin(2.0 * Math.PI * 1600.0 * tSec);
            float tone2 = (float) Math.sin(2.0 * Math.PI * 2400.0 * tSec);

            float mix = (tone1 * 0.6f + tone2 * 0.4f) * env;
            samples[i] = (short) (mix * 28000);
        }
        return samples;
    }

    private short[] generateClick(float freq, float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        short[] samples = new short[length];

        for (int i = 0; i < length; i++) {
            float t = (float) i / length;
            float env = (float) Math.exp(-t * 30.0);
            double tSec = i / (double) SAMPLE_RATE;
            float tone = (float) Math.sin(2.0 * Math.PI * freq * tSec);
            float noise = (rand.nextFloat() * 2.0f - 1.0f) * 0.3f;
            samples[i] = (short) ((tone * 0.7f + noise) * env * 24000);
        }
        return samples;
    }

    private short[] generateReloadLatch() {
        int length = (int) (SAMPLE_RATE * 0.16f);
        short[] samples = new short[length];

        for (int i = 0; i < length; i++) {
            float t = (float) i / length;
            float env = (float) Math.exp(-((t - 0.2f) * (t - 0.2f)) * 50.0);
            double tSec = i / (double) SAMPLE_RATE;
            float tone = (float) Math.sin(2.0 * Math.PI * 950.0 * tSec);
            float noise = (rand.nextFloat() * 2.0f - 1.0f) * 0.4f;
            samples[i] = (short) ((tone * 0.6f + noise) * env * 26000);
        }
        return samples;
    }

    private short[] generateExplosion() {
        int length = (int) (SAMPLE_RATE * 0.45f);
        short[] samples = new short[length];

        for (int i = 0; i < length; i++) {
            float t = (float) i / length;
            float env = (float) Math.exp(-t * 7.5);

            // Low frequency rumble (60Hz) + heavy noise
            double tSec = i / (double) SAMPLE_RATE;
            float rumble = (float) Math.sin(2.0 * Math.PI * 60.0 * tSec);
            float noise = (rand.nextFloat() * 2.0f - 1.0f);

            float mix = (rumble * 0.4f + noise * 0.6f) * env;
            samples[i] = (short) (mix * 30000);
        }
        return samples;
    }

    private short[] generateTrainChug() {
        int length = (int) (SAMPLE_RATE * 0.18f);
        short[] samples = new short[length];

        for (int i = 0; i < length; i++) {
            float t = (float) i / length;
            float env = (float) Math.sin(t * Math.PI) * (float) Math.exp(-t * 4.0);
            double tSec = i / (double) SAMPLE_RATE;
            float railClack = (float) Math.sin(2.0 * Math.PI * 110.0 * tSec);
            float steamHiss = (rand.nextFloat() * 2.0f - 1.0f) * 0.5f;

            float mix = (railClack * 0.5f + steamHiss) * env;
            samples[i] = (short) (mix * 18000);
        }
        return samples;
    }

    public void release() {
        AudioTrack[] tracks = {trackGunshot, trackHitMarker, trackReloadStart,
                trackReloadComplete, trackEmptyClick, trackExplosion, trackTrainChug};
        for (AudioTrack t : tracks) {
            if (t != null) {
                try {
                    t.stop();
                    t.release();
                } catch (Exception ignored) {
                }
            }
        }
        initialized = false;
    }
}

