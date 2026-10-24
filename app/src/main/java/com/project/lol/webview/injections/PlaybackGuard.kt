package com.project.lol.webview.injections

/**
 * Keeps playback going and the page healthy (early payload, before Spotify's scripts).
 *
 * - No accidental drag and drop: on a phone a long-press on a song followed by a small finger
 *   move started Spotify's desktop drag, and letting go moved the song inside the playlist (or
 *   into another one). Drags are blocked unless localStorage `spoAllowDrag` is "1".
 * - window.spoResumeSoon(why, ms): plays again once the player is back, after a network drop,
 *   a reload or a crashed page, unless someone pauses or another device takes the music over.
 *   A reload marked with localStorage `spoResumeAt` (or a native request, AndBridge.takeResume)
 *   resumes by itself once the new page is up.
 * - window.spoHealth(): what the native side checks before deciding a page is stale.
 */
object PlaybackGuard {
    const val CONTENT = """
(function(){
  if(window.__spoPG)return;window.__spoPG=1;
  function log(l,m){try{AndBridge.dbg(l,'guard: '+m);}catch(e){}}

  /* ---------- no accidental drag and drop ---------- */
  function dragOk(){try{return localStorage.getItem('spoAllowDrag')==='1';}catch(e){return false;}}
  var blockDrag=function(e){
    if(dragOk())return;
    try{e.preventDefault();}catch(x){}
    e.stopImmediatePropagation();
  };
  ['dragstart','dragenter','dragover','drop'].forEach(function(t){window.addEventListener(t,blockDrag,true);});
  function dragCss(){
    var h=document.head||document.documentElement;
    if(!h||document.getElementById('spo-nodrag'))return;
    if(dragOk())return;
    var s=document.createElement('style');s.id='spo-nodrag';
    s.textContent='[draggable=true],[draggable=true] *,img,a{-webkit-user-drag:none!important}';
    h.appendChild(s);
  }
  dragCss();document.addEventListener('DOMContentLoaded',dragCss);

  /* ---------- play again after a drop ---------- */
  function playing(){try{if(typeof window.splIsPlaying==='function')return window.splIsPlaying();}catch(e){}return null;}
  function otherDevice(){
    try{
      var S=window.__spoConn,c=S&&S.cluster,id=S&&S.localId&&S.localId();
      if(c&&id&&c.active_device_id&&c.active_device_id!==id&&Date.now()-S.clusterAt<20000)return true;
    }catch(e){}
    return false;
  }
  var rs=null;
  window.spoResumeSoon=function(why,ms){
    var start=Date.now(),until=start+(ms||45000),tries=0;
    if(rs)clearInterval(rs);
    /* A pause from here on (a tap on pause, the notification) means stop trying. */
    try{window.reqPause=false;}catch(e){}
    log('i','resume after '+why);
    rs=setInterval(function(){
      var p=playing(),now=Date.now();
      var stop=p===true||now>until||window.reqPause===true||otherDevice()||window.__spoStopping;
      if(stop){clearInterval(rs);rs=null;log(p===true?'s':'w','resume '+(p===true?'done':'gave up')+' ('+why+')');return;}
      if(navigator.onLine===false||p!==false)return;
      if(typeof window.actPlayPause!=='function'||!(window.splPbNode&&window.splPbNode()))return;
      if(now-start<2000)return;
      if(tries>=5){clearInterval(rs);rs=null;log('w','resume gave up after '+tries+' tries ('+why+')');return;}
      tries++;
      try{window.actPlayPause(true);}catch(e){}
    },2500);
  };
  window.spoMarkResume=function(){try{localStorage.setItem('spoResumeAt',String(Date.now()));}catch(e){}};
  function resumeOnLoad(){
    var at=0,nat=false;
    try{at=+(localStorage.getItem('spoResumeAt')||0);localStorage.removeItem('spoResumeAt');}catch(e){}
    try{nat=!!(window.AndBridge&&typeof AndBridge.takeResume==='function'&&AndBridge.takeResume());}catch(e){}
    if(location.hostname!=='open.spotify.com')return;
    if(nat||(at&&Date.now()-at<120000))window.spoResumeSoon(nat?'crash':'reload',60000);
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',resumeOnLoad);else resumeOnLoad();

  /* ---------- page health, for the native stale-page check ---------- */
  function crashed(){
    var mv=document.querySelector('#main-view');if(!mv)return false;
    if(mv.querySelector('[data-testid=tracklist-row],[data-encore-id=card]'))return false;
    var b=mv.querySelector('button');if(!b)return false;
    return /reload|neu laden|recargar|recarregar|ricarica|recharger|odśwież/i.test(b.textContent||'')&&
      /went wrong|error|fehler|erro|errore|erreur|błąd|algo sali/i.test(mv.textContent||'');
  }
  window.spoHealth=function(){
    var S=window.__spoConn||{},ws=S.ws,now=Date.now();
    return JSON.stringify({
      host:location.hostname,playing:playing()===true,online:navigator.onLine!==false,
      late:typeof window.actPlayPause==='function',ui:!!(window.splPbNode&&window.splPbNode()),
      main:!!document.querySelector('#main-view,main'),crash:crashed(),
      ws:ws?ws.readyState:-1,closedAgo:S.lastClose?now-S.lastClose:-1,token:!!window.spotAuthToken
    });
  };
})();
"""
}
