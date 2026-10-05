package com.project.lol.webview.injections

/*
 * In-page ad cleanup, no certificate or proxy needed. Runs at document start
 * next to AdStateHook (which feeds ad audio IDs to the native request filter).
 *
 *  1. Hides ad and upsell slots with CSS. Nothing is removed from the page:
 *     deleting nodes React owns is what makes Spotify show "Something went
 *     wrong", so they are only hidden.
 *  2. Keeps a list of the media elements Spotify creates. While an ad is the
 *     current item it mutes them and jumps to the end, then restores the mute
 *     state it changed once music is back.
 */
object AdCleaner {
    const val CONTENT = """
        (function(){
            if (window.__spoAdCleaner) return;
            window.__spoAdCleaner = 1;

            var CSS = '[data-testid="ad-slot-container"],[data-testid="embedded-ad"],[data-testid="home-ad-card"],' +
                '[data-testid="leavebehind-advertiser"],[data-testid="sponsored-recommendation"],[data-testid="upsell-button"],' +
                'a[href*="spotify.com/premium"][data-encore-id],button[data-testid="upgrade-button"]' +
                '{display:none!important}';
            function addStyle(){
                if (document.getElementById('spo-adclean')) return true;
                var host = document.head || document.documentElement;
                if (!host) return false;
                var st = document.createElement('style');
                st.id = 'spo-adclean';
                st.textContent = CSS;
                host.appendChild(st);
                return true;
            }
            if (!addStyle()) document.addEventListener('DOMContentLoaded', addStyle);

            var media = [];
            var origCreate = document.createElement;
            document.createElement = function(tag){
                var el = origCreate.apply(this, arguments);
                try {
                    var t = String(tag).toLowerCase();
                    if (t === 'audio' || t === 'video') media.push(el);
                } catch(e){}
                return el;
            };
            function allMedia(){
                var list = media.slice();
                try {
                    var q = document.querySelectorAll('audio,video');
                    for (var i = 0; i < q.length; i++) if (list.indexOf(q[i]) === -1) list.push(q[i]);
                } catch(e){}
                return list;
            }
            function adPlaying(){
                var u = window.__curTrackUri;
                if (typeof u === 'string' && u.indexOf('spotify:ad:') === 0) return true;
                var bar = document.querySelector('aside[data-testid=now-playing-bar]') || document.querySelector('[data-testid=now-playing-widget]');
                if (!bar) return false;
                return !!bar.querySelector('[data-testid="context-item-info-ad-subtitle"],a[href*="/ad/"],a[href*="adclick"],[data-testid="ad-link"]');
            }
            var muted = [];
            function tick(){
                if (adPlaying()) {
                    var els = allMedia();
                    for (var i = 0; i < els.length; i++) {
                        var el = els[i];
                        try {
                            if (!el.muted) { el.muted = true; muted.push(el); }
                            if (isFinite(el.duration) && el.duration > 1 && el.currentTime < el.duration - 0.5) el.currentTime = el.duration - 0.2;
                        } catch(e){}
                    }
                } else if (muted.length) {
                    for (var j = 0; j < muted.length; j++) { try { muted[j].muted = false; } catch(e){} }
                    muted = [];
                }
            }
            setInterval(tick, 700);
        })();
    """
}
