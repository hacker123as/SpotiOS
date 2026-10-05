package com.project.lol.webview.injections

/*
 * After signing in, accounts.spotify.com shows a "You're logged in" page with
 * Overview / Web Player buttons. That page renders after onPageFinished, so a
 * single check missed it and people had to tap Web Player themselves. Poll for
 * a few seconds and go straight to the web player as soon as it shows up.
 */
object LoginDetection {
    const val CONTENT = """
            (function() {
                if (window.__spoLoginPoll) return;
                window.__spoLoginPoll = 1;
                var tries = 0, done = false;
                function go(el) {
                    if (done) return;
                    done = true;
                    try { AndBridge.loginDetected(); } catch (e) {}
                    try { if (el) el.click(); } catch (e) {}
                    setTimeout(function(){
                        if (location.hostname !== 'open.spotify.com') location.replace('https://open.spotify.com/');
                    }, 400);
                }
                function check() {
                    if (done) return;
                    tries++;
                    var l = document.querySelector('button[data-testid=web-player-link],a[data-testid=web-player-link]');
                    if (l) { go(l); return; }
                    // the status page only exists once signed in
                    if (/^\/([a-z]{2}(-[a-zA-Z]{2,4})?\/)?status\/?$/.test(location.pathname)) {
                        var a = document.querySelector('a[href^="https://open.spotify.com"],a[href*="//open.spotify.com"]');
                        if (a || tries > 8) { go(null); return; }
                    }
                    if (tries < 60) setTimeout(check, 250);
                }
                check();
            })();
        
    """
}
