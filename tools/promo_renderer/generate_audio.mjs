import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createRequire } from 'node:module';

const require = createRequire(path.join(process.cwd(), 'package.json'));
const { chromium } = require('playwright-core');

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const OUT_WAV = path.join(__dirname, 'lexora_120bpm.wav');

function findBrowserExecutable() {
  const candidates = [
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
    'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
  ];
  for (const p of candidates) {
    if (fs.existsSync(p)) return p;
  }
  return undefined;
}

async function main() {
  const executablePath = findBrowserExecutable();
  const browser = await chromium.launch({
    headless: true,
    ...(executablePath ? { executablePath } : {}),
  });

  try {
    const page = await browser.newPage();
    const wavBytes = await page.evaluate(async () => {
      const sampleRate = 48000;
      const duration = 15;
      const totalFrames = sampleRate * duration;
      const ctx = new OfflineAudioContext(2, totalFrames, sampleRate);

      // Master bus with gentle fade-in (30ms) and fade-out (150ms) for seamless loop
      const master = ctx.createGain();
      master.gain.setValueAtTime(0.0, 0);
      master.gain.linearRampToValueAtTime(0.85, 0.03);
      master.gain.setValueAtTime(0.85, duration - 0.15);
      master.gain.linearRampToValueAtTime(0.0, duration);
      master.connect(ctx.destination);

      // Helper: warm minimalist low pulse on 120 BPM beat (every 0.5s)
      function triggerBeatPulse(t, accent = false) {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(accent ? 116 : 104, t);
        osc.frequency.exponentialRampToValueAtTime(52, t + 0.18);

        const peak = accent ? 0.28 : 0.20;
        gain.gain.setValueAtTime(0.0001, t);
        gain.gain.linearRampToValueAtTime(peak, t + 0.008);
        gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.24);

        osc.connect(gain);
        gain.connect(master);
        osc.start(t);
        osc.stop(t + 0.26);
      }

      // Helper: subtle off-beat muted percussive tick (0.25s, 0.75s, ...)
      function triggerSoftTick(t) {
        const osc = ctx.createOscillator();
        const filter = ctx.createBiquadFilter();
        const gain = ctx.createGain();

        osc.type = 'triangle';
        osc.frequency.setValueAtTime(920, t);
        osc.frequency.exponentialRampToValueAtTime(340, t + 0.035);

        filter.type = 'lowpass';
        filter.frequency.setValueAtTime(1600, t);

        gain.gain.setValueAtTime(0.0001, t);
        gain.gain.linearRampToValueAtTime(0.055, t + 0.003);
        gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.045);

        osc.connect(filter);
        filter.connect(gain);
        gain.connect(master);
        osc.start(t);
        osc.stop(t + 0.05);
      }

      // Helper: soft harmonic glass chime for act transitions & interactions
      function triggerGlassChime(t, baseFreq, dur = 0.65, level = 0.13) {
        const partials = [
          { mult: 1.0, amp: 1.0 },
          { mult: 1.5, amp: 0.35 },
          { mult: 2.0, amp: 0.22 },
        ];
        for (const p of partials) {
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(baseFreq * p.mult, t);

          const peak = level * p.amp;
          gain.gain.setValueAtTime(0.0001, t);
          gain.gain.linearRampToValueAtTime(peak, t + 0.012);
          gain.gain.exponentialRampToValueAtTime(0.0001, t + dur);

          osc.connect(gain);
          gain.connect(master);
          osc.start(t);
          osc.stop(t + dur + 0.02);
        }
      }

      // 120 BPM grid: beats at 0.0, 0.5, ..., 14.5; off-beats at 0.25, 0.75, ...
      for (let i = 0; i < 30; i++) {
        const beatTime = i * 0.5;
        const isDownbeat = i % 4 === 0;
        triggerBeatPulse(beatTime, isDownbeat);
        if (beatTime + 0.25 < duration - 0.15) {
          triggerSoftTick(beatTime + 0.25);
        }
      }

      // Key visual moment chimes (D major / B minor pentatonic glass voicings)
      triggerGlassChime(0.30, 587.33, 0.85, 0.12); // D5: L-border draw
      triggerGlassChime(3.05, 523.25, 0.55, 0.11); // C5: Pillar 1
      triggerGlassChime(3.50, 587.33, 0.55, 0.11); // D5: Pillar 2
      triggerGlassChime(3.95, 659.25, 0.65, 0.12); // E5: Pillar 3
      triggerGlassChime(6.00, 440.00, 0.75, 0.12); // A4: Real Interface entry
      triggerGlassChime(7.20, 587.33, 0.65, 0.11); // D5: Phone -> Tablet glide
      triggerGlassChime(9.65, 659.25, 0.60, 0.13); // E5: Flashcard 3D flip
      triggerGlassChime(10.75, 783.99, 0.70, 0.14); // G5: SM-2 "Nhớ 6 ngày" tap
      triggerGlassChime(12.10, 587.33, 1.00, 0.13); // D5: Close card & logo resolve

      const buffer = await ctx.startRendering();
      const ch0 = buffer.getChannelData(0);
      const ch1 = buffer.getChannelData(1);

      // Encode 16-bit PCM stereo WAV
      const dataSize = totalFrames * 4;
      const wav = new ArrayBuffer(44 + dataSize);
      const view = new DataView(wav);

      function writeStr(offset, str) {
        for (let i = 0; i < str.length; i++) {
          view.setUint8(offset + i, str.charCodeAt(i));
        }
      }

      writeStr(0, 'RIFF');
      view.setUint32(4, 36 + dataSize, true);
      writeStr(8, 'WAVE');
      writeStr(12, 'fmt ');
      view.setUint32(16, 16, true);
      view.setUint16(20, 1, true); // PCM
      view.setUint16(22, 2, true); // stereo
      view.setUint32(24, sampleRate, true);
      view.setUint32(28, sampleRate * 4, true);
      view.setUint16(32, 4, true);
      view.setUint16(34, 16, true);
      writeStr(36, 'data');
      view.setUint32(40, dataSize, true);

      let offset = 44;
      for (let i = 0; i < totalFrames; i++) {
        const s0 = Math.max(-1, Math.min(1, ch0[i]));
        const s1 = Math.max(-1, Math.min(1, ch1[i]));
        view.setInt16(offset, s0 < 0 ? s0 * 0x8000 : s0 * 0x7fff, true);
        view.setInt16(offset + 2, s1 < 0 ? s1 * 0x8000 : s1 * 0x7fff, true);
        offset += 4;
      }

      return Array.from(new Uint8Array(wav));
    });

    fs.mkdirSync(path.dirname(OUT_WAV), { recursive: true });
    fs.writeFileSync(OUT_WAV, Buffer.from(wavBytes));
    console.log(`Generated WAV: ${OUT_WAV} (${wavBytes.length} bytes)`);
  } finally {
    await browser.close();
  }
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
