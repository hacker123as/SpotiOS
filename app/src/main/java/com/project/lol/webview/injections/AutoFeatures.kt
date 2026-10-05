package com.project.lol.webview.injections

object AutoFeatures {
    const val CONTENT = """
            window.addAutoFeatures = function(){
                window.__spoBootAt = window.__spoBootAt || Date.now();
                if('pBtn' in window && firstPlay && window.autoPlayMode!=='disabled' && window.splIsPlaying()===false) {
                    pBtn.click();
                    firstPlay=false;
                }
                if(afint) clearInterval(afint);
                afint = setInterval(function(){
                    if(window.closeNpPref) closeNowPlay(true);
                    // Take control only once, right after launch. Doing it on every tick
                    // kept opening and closing the Connect picker and pulled playback back
                    // whenever the user (or another device) moved it somewhere else.
                    var ft = document.querySelector('aside div.encore-bright-accent-set button');
                    var early = (Date.now() - window.__spoBootAt) < 45000;
                    var held = window.__spoPanelHold && Date.now() < window.__spoPanelHold;
                    if(ft && window.__splTakeControl && early && !held && !window.__spoTookControl) {
                        window.__spoTookControl = true;
                        ft.click();
                        setTimeout(function(){
                            var cb = document.querySelector('aside ul[role=list] li[role=listitem] div[role=button]');
                            if(cb) cb.click();
                        },500);
                    }
                    if(window.autoPlayMode==='permanent' && 'pBtn' in window && !reqPause && !ulFlag && window.splIsPlaying()===false) {
                        pBtn.click();
                    }
                    if(window.autoPlayMode==='onetime' && !window.__splApDone && !window.__splApActive && 'pBtn' in window && !reqPause && window.splIsPlaying()===false) {
                        if(typeof splAutoPlay === 'function') splAutoPlay();
                    }
                },5000);
            };
        
    """
}
