/* ==================== WEB AUDIO API UI SOUND EFFECTS (0 MP3, SIÊU NHẸ) ==================== */
(function () {
    'use strict';

    const STORAGE_KEY = 'pk_sound_enabled';
    let audioCtx = null;
    let enabled = localStorage.getItem(STORAGE_KEY) !== 'false';
    let unlocked = false;

    function getContext() {
        if (!enabled) return null;
        const AudioCtx = window.AudioContext || window.webkitAudioContext;
        if (!AudioCtx) return null;
        if (!audioCtx) {
            try {
                audioCtx = new AudioCtx();
            } catch (e) {
                return null;
            }
        }
        if (audioCtx.state === 'suspended') {
            audioCtx.resume().catch(() => {});
        }
        return audioCtx;
    }

    function unlockAudio() {
        if (unlocked) return;
        unlocked = true;
        if (enabled) {
            getContext();
        }
    }

    ['pointerdown', 'keydown'].forEach((evt) => {
        document.addEventListener(evt, unlockAudio, { once: true, passive: true });
    });

    function syncSoundIcons() {
        const iconOn = document.getElementById('sound-icon-on');
        const iconOff = document.getElementById('sound-icon-off');
        const btn = document.getElementById('sound-toggle-btn');
        if (iconOn && iconOff) {
            iconOn.classList.toggle('hidden', !enabled);
            iconOff.classList.toggle('hidden', enabled);
        }
        if (btn) {
            btn.setAttribute('aria-pressed', String(enabled));
            btn.title = enabled ? 'Tắt âm thanh giao diện' : 'Bật âm thanh giao diện';
        }
    }

    function playClick() {
        const ctx = getContext();
        if (!ctx) return;
        const now = ctx.currentTime;
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(760, now);
        osc.frequency.exponentialRampToValueAtTime(320, now + 0.038);
        gain.gain.setValueAtTime(0.055, now);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.042);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(now);
        osc.stop(now + 0.045);
    }

    function playCoin() {
        const ctx = getContext();
        if (!ctx) return;
        const now = ctx.currentTime;
        const notes = [
            { freq: 987.77, start: 0, dur: 0.09, vol: 0.075 },     // B5
            { freq: 1318.51, start: 0.065, dur: 0.22, vol: 0.08 }  // E6
        ];
        notes.forEach((n) => {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'sine';
            osc.frequency.setValueAtTime(n.freq, now + n.start);
            gain.gain.setValueAtTime(0.0001, now + n.start);
            gain.gain.linearRampToValueAtTime(n.vol, now + n.start + 0.012);
            gain.gain.exponentialRampToValueAtTime(0.0001, now + n.start + n.dur);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start(now + n.start);
            osc.stop(now + n.start + n.dur + 0.01);
        });
    }

    function playCardFlip() {
        const ctx = getContext();
        if (!ctx) return;
        const now = ctx.currentTime;
        const duration = 0.095;
        const bufferSize = Math.floor(ctx.sampleRate * duration);
        const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) {
            data[i] = (Math.random() * 2 - 1) * (1 - i / bufferSize);
        }
        const noise = ctx.createBufferSource();
        noise.buffer = buffer;

        const filter = ctx.createBiquadFilter();
        filter.type = 'bandpass';
        filter.frequency.setValueAtTime(1100, now);
        filter.frequency.exponentialRampToValueAtTime(2800, now + duration);
        filter.Q.value = 1.6;

        const gain = ctx.createGain();
        gain.gain.setValueAtTime(0.001, now);
        gain.gain.linearRampToValueAtTime(0.08, now + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + duration);

        noise.connect(filter);
        filter.connect(gain);
        gain.connect(ctx.destination);
        noise.start(now);
        noise.stop(now + duration);
    }

    function playRareHit() {
        const ctx = getContext();
        if (!ctx) return;
        const now = ctx.currentTime;
        const chord = [523.25, 659.25, 783.99, 1046.5, 1318.51]; // C5 E5 G5 C6 E6
        chord.forEach((freq, idx) => {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            const t = now + idx * 0.048;
            osc.type = idx >= 3 ? 'triangle' : 'sine';
            osc.frequency.setValueAtTime(freq, t);
            gain.gain.setValueAtTime(0.0001, t);
            gain.gain.linearRampToValueAtTime(0.065, t + 0.018);
            gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.42);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start(t);
            osc.stop(t + 0.45);
        });
    }

    function toggleSound() {
        enabled = !enabled;
        localStorage.setItem(STORAGE_KEY, String(enabled));
        syncSoundIcons();
        if (enabled) {
            playClick();
        }
        return enabled;
    }

    window.SoundFX = {
        playClick,
        playCoin,
        playCardFlip,
        playRareHit,
        toggle: toggleSound,
        isEnabled: () => enabled,
        syncIcons: syncSoundIcons
    };
    window.toggleBlindboxSound = toggleSound;

    // Event delegation nhẹ cho các nút CTA chính & thẻ trưng bày
    document.addEventListener('click', (e) => {
        if (!enabled) return;
        const target = e.target && e.target.closest
            ? e.target.closest('.pk-btn-primary, .pk-chase-card, .pk-hero-fan-card, [data-sfx="click"]')
            : null;
        if (target && !target.disabled && target.id !== 'sound-toggle-btn') {
            playClick();
        }
    }, { passive: true });

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', syncSoundIcons);
    } else {
        syncSoundIcons();
    }
})();
