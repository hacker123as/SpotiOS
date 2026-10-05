package com.project.lol.webview.injections

import org.json.JSONObject

/**
 * Extra player features on top of the SpotiOS shell:
 * a More menu in Now Playing (playback speed, go to artist/album, share, stats),
 * on-device listening stats, hiding podcasts and audiobooks, swipe-to-skip on
 * the mini player and artwork, double-tap artwork to like, and shake to skip.
 *
 * Settings arrive as window.__spoX; spoXApply() re-reads them live.
 */
object SpoExtras {

    fun prefsJs(hidePods: Boolean, swipe: Boolean, dtLike: Boolean, stats: Boolean, shake: Boolean): String {
        val o = JSONObject()
            .put("hidePods", hidePods)
            .put("swipe", swipe)
            .put("dtLike", dtLike)
            .put("stats", stats)
            .put("shake", shake)
        return "window.__spoX=$o;if(window.spoXApply)window.spoXApply();"
    }

    fun content(hidePods: Boolean, swipe: Boolean, dtLike: Boolean, stats: Boolean, shake: Boolean): String =
        prefsJs(hidePods, swipe, dtLike, stats, shake) + "\n" + JS.replace("__SPOX_CSS__", JSONObject.quote(CSS))

    private const val CSS = """
#spoX{position:fixed;inset:0;z-index:2147483647;background:rgba(0,0,0,.45);display:flex;align-items:flex-end;justify-content:center;opacity:0;pointer-events:none;transition:opacity .25s}
#spoX.open{opacity:1;pointer-events:auto}
#spoX .x-card{width:min(560px,calc(100vw - 20px));max-height:84vh;overflow-y:auto;overscroll-behavior:contain;margin-bottom:calc(env(safe-area-inset-bottom,0px) + 10px);padding:10px;border-radius:28px;box-sizing:border-box;
  background:rgba(34,34,40,.84);backdrop-filter:blur(30px) saturate(190%);-webkit-backdrop-filter:blur(30px) saturate(190%);
  box-shadow:inset 0 1px 0 rgba(255,255,255,.16),0 0 0 1px rgba(255,255,255,.07),0 24px 60px rgba(0,0,0,.5);
  transform:translateY(30px);transition:transform .42s cubic-bezier(.2,.9,.25,1);font-family:-apple-system,system-ui,Roboto,sans-serif;color:#fff;scrollbar-width:none}
#spoX.open .x-card{transform:none}
#spoX .x-head{display:flex;align-items:center;gap:12px;padding:8px 10px 12px}
#spoX .x-head img{width:52px;height:52px;border-radius:10px;object-fit:cover;background:#333;flex:none}
#spoX .x-head b{display:block;font-size:16px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
#spoX .x-head small{display:block;font-size:13px;color:rgba(255,255,255,.6);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-top:2px}
#spoX .x-head>div{min-width:0}
#spoX .x-t{font-size:13px;font-weight:700;letter-spacing:.06em;text-transform:uppercase;color:rgba(255,255,255,.6);padding:10px 12px 8px}
#spoX .x-item{display:flex;align-items:center;gap:14px;width:100%;border:0;background:transparent;color:#fff;text-align:left;padding:12px;border-radius:16px;font:600 15.5px/1.2 inherit;font-family:inherit}
#spoX .x-item:active{background:rgba(255,255,255,.1)}
#spoX .x-item svg{width:22px;height:22px;flex:none;color:rgba(255,255,255,.85)}
#spoX .x-item small{margin-left:auto;font-size:13px;font-weight:600;color:rgba(255,255,255,.5)}
#spoX .x-chips{display:flex;gap:6px;padding:2px 10px 10px;overflow-x:auto;scrollbar-width:none}
#spoX .x-chip{flex:1 1 0;min-width:0;border:0;border-radius:999px;padding:9px 4px;background:rgba(255,255,255,.08);color:#fff;font:700 14px/1 inherit;font-family:inherit;box-shadow:inset 0 1px 0 rgba(255,255,255,.1)}
#spoX .x-chip.on{background:#1ed760;color:#000}
#spoX .x-sep{height:1px;margin:4px 12px;background:rgba(255,255,255,.08)}
#spoX .x-cancel{justify-content:center;margin-top:6px;background:rgba(255,255,255,.08);font-weight:700}
#spoX .x-tiles{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;padding:0 6px 6px}
#spoX .x-tile{border-radius:18px;padding:12px 10px;background:rgba(255,255,255,.07);box-shadow:inset 0 1px 0 rgba(255,255,255,.08)}
#spoX .x-tile b{display:block;font-size:22px;font-weight:800;letter-spacing:-.02em}
#spoX .x-tile small{display:block;font-size:12px;color:rgba(255,255,255,.55);margin-top:3px}
#spoX .x-bars{display:flex;align-items:flex-end;gap:6px;height:84px;padding:6px 12px 0}
#spoX .x-bar{flex:1;display:flex;flex-direction:column;align-items:center;gap:5px;height:100%;justify-content:flex-end}
#spoX .x-bar i{display:block;width:100%;border-radius:6px;background:linear-gradient(180deg,#5cf09a,#14a84d);min-height:3px}
#spoX .x-bar span{font-size:10.5px;color:rgba(255,255,255,.5);font-weight:600}
#spoX .x-rank{display:flex;align-items:center;gap:12px;padding:8px 12px}
#spoX .x-rank em{font-style:normal;width:18px;text-align:right;font-weight:800;color:rgba(255,255,255,.45);font-size:14px;flex:none}
#spoX .x-rank img{width:42px;height:42px;border-radius:8px;object-fit:cover;background:#333;flex:none}
#spoX .x-rank>div{min-width:0;flex:1}
#spoX .x-rank b{display:block;font-size:14.5px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
#spoX .x-rank small{display:block;font-size:12.5px;color:rgba(255,255,255,.55);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
#spoX .x-empty{padding:4px 12px 12px;color:rgba(255,255,255,.55);font-size:14px;line-height:1.4}
#spoX .x-danger{color:#ff6b6b}
#spoX .x-tabs{display:flex;gap:6px;padding:0 10px 8px}
.spo-heart-burst{position:absolute;left:50%;top:50%;width:96px;height:96px;margin:-48px 0 0 -48px;color:#fff;pointer-events:none;z-index:5;filter:drop-shadow(0 6px 18px rgba(0,0,0,.45));animation:spoBurst .75s cubic-bezier(.2,.9,.25,1) forwards}
@keyframes spoBurst{0%{transform:scale(.3);opacity:0}25%{transform:scale(1.15);opacity:1}60%{transform:scale(1);opacity:1}100%{transform:scale(1.25);opacity:0}}
#spoNP .spo-art-wrap{position:relative}
#spoNP #spo-art{transition:transform .28s cubic-bezier(.2,.9,.25,1),opacity .2s}
#spoNP .spo-head-t .x-speed{margin-left:6px;padding:2px 7px;border-radius:999px;background:#1ed760;color:#000;font-size:10.5px;letter-spacing:0}
#spoNP #spo-share{display:none}
html.spo-nopods [data-spo-pod]{display:none!important}
#spotilolPlayerControls .spl-top{transition:transform .25s cubic-bezier(.2,.9,.25,1)}
#spotilolPlayerControls.spo-swiping .spl-top{transition:none}
"""

    private const val JS = """
(function(){
if(window.__spoXBooted)return;window.__spoXBooted=1;
var X=window.__spoX||{};
function byId(i){return document.getElementById(i);}
function qs(s){return document.querySelector(s);}
function txt(e){return e?(e.textContent||'').trim():'';}
function hap(){try{AndBridge.haptic();}catch(e){}}
function esc(s){return String(s==null?'':s).replace(/[&<>"]/g,function(c){return{'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
function act(id){var b=byId(id);if(b)b.click();}
function playing(){try{return !!(window.splIsPlayingSticky&&window.splIsPlayingSticky());}catch(e){return false;}}
var st=document.createElement('style');st.id='spoXCss';st.textContent=__SPOX_CSS__;
(document.head||document.documentElement).appendChild(st);
var SV='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">';
var I={
  more:'<svg viewBox="0 0 24 24"><circle cx="5" cy="12" r="2" fill="currentColor"/><circle cx="12" cy="12" r="2" fill="currentColor"/><circle cx="19" cy="12" r="2" fill="currentColor"/></svg>',
  user:SV+'<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/></svg>',
  disc:SV+'<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="2.5"/></svg>',
  share:SV+'<path d="M12 3v12M7.5 7.5 12 3l4.5 4.5"/><path d="M6 11H5a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-8a1 1 0 0 0-1-1h-1"/></svg>',
  chart:SV+'<path d="M4 20V10M10 20V4M16 20v-7M22 20H2"/></svg>',
  queue:SV+'<path d="M3 6h13M3 12h13M3 18h8"/><path d="M16 15v6l5-3z" fill="currentColor"/></svg>',
  heart:'<svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 21s-8-4.9-9.9-9.8C.7 7.6 2.9 3.8 6.8 3.8c2.2 0 3.9 1.3 5.2 3.2 1.3-1.9 3-3.2 5.2-3.2 3.9 0 6.1 3.8 4.7 7.4C20 16.1 12 21 12 21z"/></svg>',
  speed:SV+'<path d="M12 13l4-4"/><path d="M3.5 18a9.5 9.5 0 1 1 17 0"/></svg>'
};

/* ---------- generic glass sheet ---------- */
function sheetEl(){
  var o=byId('spoX');if(o)return o;
  o=document.createElement('div');o.id='spoX';
  o.innerHTML='<div class="x-card" id="spoXCard"></div>';
  o.addEventListener('click',function(e){if(e.target===o||e.target.closest('[data-x=close]'))closeX();});
  document.body.appendChild(o);return o;
}
function openX(html){
  var o=sheetEl();byId('spoXCard').innerHTML=html;byId('spoXCard').scrollTop=0;
  requestAnimationFrame(function(){o.classList.add('open');});
  return o;
}
function closeX(){var o=byId('spoX');if(o)o.classList.remove('open');}
window.spoCloseExtras=closeX;

/* ---------- playback speed ---------- */
var SPEEDS=[0.5,0.75,1,1.25,1.5,2];
var speed=1;try{speed=parseFloat(localStorage.getItem('spoSpeed'))||1;}catch(e){}
function media(){
  try{if(window.__spoAllMedia)return window.__spoAllMedia();}catch(e){}
  return [].slice.call(document.querySelectorAll('audio,video'));
}
function applySpeed(){
  var l=media();
  for(var i=0;i<l.length;i++){var m=l[i];try{if(Math.abs(m.playbackRate-speed)>.001){m.playbackRate=speed;}if('preservesPitch' in m)m.preservesPitch=true;}catch(e){}}
}
function setSpeed(v){speed=v;try{localStorage.setItem('spoSpeed',String(v));}catch(e){}applySpeed();speedBadge();}
function speedLabel(v){return (v===1?'1':String(v))+'×';}
function speedBadge(){
  var h=qs('#spoNP .spo-head-t');if(!h)return;
  var b=h.querySelector('.x-speed');
  if(speed===1){if(b)b.remove();return;}
  if(!b){b=document.createElement('span');b.className='x-speed';h.appendChild(b);}
  b.textContent=speedLabel(speed);
}
document.addEventListener('ratechange',function(e){var m=e.target;if(m&&Math.abs(m.playbackRate-speed)>.001&&speed!==1)setTimeout(applySpeed,0);},true);
document.addEventListener('playing',function(){if(speed!==1)applySpeed();},true);

/* ---------- More menu in Now Playing ---------- */
function curInfo(){
  var cov=byId('spl-cover-img');
  return {t:txt(byId('spl-track')),a:txt(byId('spl-artist')),i:cov&&cov.src||''};
}
function openMore(){
  hap();
  var c=curInfo(),chips='';
  for(var i=0;i<SPEEDS.length;i++)chips+='<button class="x-chip'+(SPEEDS[i]===speed?' on':'')+'" data-speed="'+SPEEDS[i]+'">'+speedLabel(SPEEDS[i])+'</button>';
  var o=openX(
    '<div class="x-head"><img '+(c.i?'src="'+esc(c.i)+'" ':'')+'alt=""><div><b>'+esc(c.t||'Not playing')+'</b><small>'+esc(c.a)+'</small></div></div>'
    +'<div class="x-sep"></div>'
    +'<button class="x-item" data-m="artist">'+I.user+'Go to artist</button>'
    +'<button class="x-item" data-m="album">'+I.disc+'Go to album</button>'
    +'<button class="x-item" data-m="queue">'+I.queue+'Open queue</button>'
    +'<button class="x-item" data-m="share">'+I.share+'Share</button>'
    +'<button class="x-item" data-m="stats">'+I.chart+'Your listening stats</button>'
    +'<div class="x-sep"></div>'
    +'<div class="x-t">Playback speed</div><div class="x-chips">'+chips+'</div>'
    +'<button class="x-item x-cancel" data-x="close">Close</button>');
  byId('spoXCard').onclick=function(e){
    var s=e.target.closest('[data-speed]');
    if(s){hap();setSpeed(parseFloat(s.getAttribute('data-speed')));var cs=byId('spoXCard').querySelectorAll('.x-chip');for(var k=0;k<cs.length;k++)cs[k].classList.toggle('on',cs[k]===s);return;}
    var m=e.target.closest('[data-m]');if(!m)return;
    var k2=m.getAttribute('data-m');hap();
    if(k2==='stats'){openStats();return;}
    closeX();
    if(k2==='queue'&&window.spoOpenPanel){window.spoOpenPanel('queue');return;}
    var link=null;
    if(k2==='artist')link=qs('a[data-testid=context-item-info-artist]')||qs('a[data-testid=context-item-info-show]');
    if(k2==='album')link=qs('a[data-testid=context-item-link]');
    if(link||k2==='queue'||k2==='share'){
      if(window.spoClosePlayer)window.spoClosePlayer();
      setTimeout(function(){
        if(link)link.click();
        else if(k2==='queue')act('spl-queue');
        else if(window.spoShare)window.spoShare();
      },260);
    }
  };
}
function hookNP(){
  var np=byId('spoNP');if(!np||np.__x)return;
  var head=np.querySelector('.spo-head'),share=byId('spo-share');if(!head||!share)return;
  np.__x=1;
  var b=document.createElement('button');b.className='spo-ib';b.id='spo-more';b.setAttribute('aria-label','More');b.innerHTML=I.more;
  b.onclick=openMore;
  head.insertBefore(b,share);
  speedBadge();
  bindArt(np);
}

/* ---------- artwork: swipe to skip, double-tap to like ---------- */
function burst(wrap){
  var h=document.createElement('div');h.className='spo-heart-burst';h.innerHTML=I.heart;
  wrap.appendChild(h);setTimeout(function(){h.remove();},800);
}
function liked(){var lk=byId('spl-liked');return !!(lk&&lk.classList.contains('spl-active'));}
function bindArt(np){
  var wrap=np.querySelector('.spo-art-wrap'),art=byId('spo-art');if(!wrap||!art)return;
  var sx=0,sy=0,dx=0,on=false,lastTap=0;
  wrap.addEventListener('touchstart',function(e){if(X.swipe===false)return;on=true;sx=e.touches[0].clientX;sy=e.touches[0].clientY;dx=0;},{passive:true});
  wrap.addEventListener('touchmove',function(e){
    if(!on)return;var mx=e.touches[0].clientX-sx,my=e.touches[0].clientY-sy;
    if(Math.abs(mx)>Math.abs(my)&&Math.abs(mx)>8){dx=mx;art.style.transition='none';art.style.transform='translate3d('+(mx*.6)+'px,0,0) rotate('+(mx*.02)+'deg)';e.stopPropagation();}
  },{passive:true});
  wrap.addEventListener('touchend',function(){
    if(!on)return;on=false;art.style.transition='';
    if(Math.abs(dx)>70){
      hap();art.style.transform='translate3d('+(dx>0?120:-120)+'px,0,0)';art.style.opacity='0';
      act(dx<0?'spl-next':'spl-prev');
      setTimeout(function(){art.style.transition='none';art.style.transform='translate3d('+(dx>0?-60:60)+'px,0,0)';
        requestAnimationFrame(function(){art.style.transition='';art.style.transform='';art.style.opacity='';});},230);
    }else art.style.transform='';
  });
  wrap.addEventListener('click',function(){
    var now=Date.now();
    if(X.dtLike!==false&&now-lastTap<320){
      lastTap=0;burst(wrap);hap();
      if(!liked())act('spl-liked');
      return;
    }
    lastTap=now;
  });
}

/* ---------- mini player: swipe sideways to skip ---------- */
function hookMini(){
  var pl=byId('spotilolPlayerControls');if(!pl||pl.__x)return;pl.__x=1;
  var top=null,sx=0,sy=0,dx=0,on=false,horiz=false;
  pl.addEventListener('touchstart',function(e){
    if(X.swipe===false||e.target.closest('button,#spl-bar,#spl-edgebar,.spl-vol-bar'))return;
    top=pl.querySelector('.spl-top');on=true;horiz=false;sx=e.touches[0].clientX;sy=e.touches[0].clientY;dx=0;
  },{passive:true});
  pl.addEventListener('touchmove',function(e){
    if(!on||!top)return;var mx=e.touches[0].clientX-sx,my=e.touches[0].clientY-sy;
    if(!horiz&&Math.abs(mx)>12&&Math.abs(mx)>Math.abs(my)*1.4)horiz=true;
    if(horiz){dx=mx;pl.classList.add('spo-swiping');top.style.transform='translate3d('+(mx*.5)+'px,0,0)';}
  },{passive:true});
  pl.addEventListener('touchend',function(){
    if(!on)return;on=false;pl.classList.remove('spo-swiping');
    if(top){
      if(horiz&&Math.abs(dx)>60){hap();act(dx<0?'spl-next':'spl-prev');}
      top.style.transform='';
    }
  },{passive:true});
}

/* ---------- shake to skip ---------- */
var lastShake=0;
window.addEventListener('devicemotion',function(e){
  if(!X.shake||document.hidden)return;
  var a=e.acceleration;if(!a||a.x==null)return;
  var g=Math.sqrt(a.x*a.x+a.y*a.y+a.z*a.z);
  if(g>22&&Date.now()-lastShake>1800){lastShake=Date.now();hap();act('spl-next');}
});

/* ---------- listening stats (kept on this phone only) ---------- */
var SK='spoStats2';
function loadStats(){try{var s=JSON.parse(localStorage.getItem(SK));if(s&&s.t&&s.a&&s.d)return s;}catch(e){}return {t:{},a:{},d:{},since:Date.now()};}
var S=loadStats(),dirty=0,lastTick=Date.now(),curKey='',curMs=0,counted=false;
function dayKey(off){var d=new Date(Date.now()-(off||0)*864e5);return d.getFullYear()+'-'+(d.getMonth()+1)+'-'+d.getDate();}
function saveStats(){
  dirty=0;
  var keys=Object.keys(S.t);
  if(keys.length>2500){keys.sort(function(x,y){return S.t[x].ms-S.t[y].ms;});for(var i=0;i<keys.length-2000;i++)delete S.t[keys[i]];}
  var dk=Object.keys(S.d);if(dk.length>400){var keep={};for(var j=0;j<400;j++){var k=dayKey(j);if(S.d[k])keep[k]=S.d[k];}S.d=keep;}
  try{localStorage.setItem(SK,JSON.stringify(S));}catch(e){}
}
function statTick(){
  var now=Date.now(),dt=Math.min(now-lastTick,65000);lastTick=now;
  if(X.stats===false||!playing())return;
  try{if(window.__spoAdPlaying&&window.__spoAdPlaying())return;}catch(e){}
  var t=txt(byId('spl-track')),a=txt(byId('spl-artist'));
  if(!t||t==='—')return;
  var k=t+'\u0001'+a;
  if(k!==curKey){curKey=k;curMs=0;counted=false;}
  curMs+=dt;
  var tr=S.t[k]||(S.t[k]={n:0,ms:0,t:t,a:a});tr.ms+=dt;
  var cov=byId('spl-cover-img');if(cov&&cov.src&&cov.src.indexOf('http')===0)tr.i=cov.src;
  var ar=(a.split(', ')[0]||a).trim();
  if(ar){var A=S.a[ar]||(S.a[ar]={n:0,ms:0});A.ms+=dt;if(!counted&&curMs>=30000)A.n++;}
  var dk=dayKey();S.d[dk]=(S.d[dk]||0)+dt;
  if(!counted&&curMs>=30000){counted=true;tr.n++;}
  if(++dirty>=20)saveStats();
}
document.addEventListener('visibilitychange',function(){if(document.hidden&&dirty)saveStats();});
function mins(ms){var m=Math.round(ms/60000);return m>=1000?(m/1000).toFixed(1).replace('.0','')+'k':String(m);}
function hours(ms){var h=ms/3600000;return h>=10?Math.round(h)+'h':h.toFixed(1)+'h';}
var statsTab='songs';
function openStats(){
  if(dirty)saveStats();
  var today=S.d[dayKey()]||0,week=0,all=0,bars=[],max=1,i;
  for(i=6;i>=0;i--){var v=S.d[dayKey(i)]||0;week+=v;bars.push(v);if(v>max)max=v;}
  for(var dk in S.d)all+=S.d[dk];
  var names=['S','M','T','W','T','F','S'],bh='';
  for(i=0;i<7;i++){var d=new Date(Date.now()-(6-i)*864e5);bh+='<div class="x-bar"><i style="height:'+Math.max(3,Math.round(bars[i]/max*64))+'px"></i><span>'+names[d.getDay()]+'</span></div>';}
  var list='';
  if(statsTab==='songs'){
    var ts=Object.keys(S.t).map(function(k){return S.t[k];}).filter(function(x){return x.n>0||x.ms>60000;});
    ts.sort(function(x,y){return (y.n-x.n)||(y.ms-x.ms);});
    for(i=0;i<Math.min(10,ts.length);i++){var x=ts[i];
      list+='<div class="x-rank"><em>'+(i+1)+'</em><img '+(x.i?'src="'+esc(x.i)+'" ':'')+'alt=""><div><b>'+esc(x.t)+'</b><small>'+esc(x.a)+' · '+x.n+(x.n===1?' play':' plays')+'</small></div></div>';}
  }else{
    var as=Object.keys(S.a).map(function(k){return {k:k,n:S.a[k].n,ms:S.a[k].ms};});
    as.sort(function(x,y){return y.ms-x.ms;});
    for(i=0;i<Math.min(10,as.length);i++){var y=as[i];
      list+='<div class="x-rank"><em>'+(i+1)+'</em><div><b>'+esc(y.k)+'</b><small>'+mins(y.ms)+' min · '+y.n+(y.n===1?' play':' plays')+'</small></div></div>';}
  }
  if(!list)list='<div class="x-empty">Nothing yet. Songs count once you’ve listened for 30 seconds.</div>';
  var off=X.stats===false?'<div class="x-empty">Stats are turned off in Settings, so new listening isn’t being counted.</div>':'';
  openX('<div class="x-t">Your listening</div>'+off
    +'<div class="x-tiles"><div class="x-tile"><b>'+mins(today)+'</b><small>min today</small></div><div class="x-tile"><b>'+mins(week)+'</b><small>min this week</small></div><div class="x-tile"><b>'+hours(all)+'</b><small>all time</small></div></div>'
    +'<div class="x-bars">'+bh+'</div>'
    +'<div class="x-t" style="padding-top:16px">Top</div>'
    +'<div class="x-tabs"><button class="x-chip'+(statsTab==='songs'?' on':'')+'" data-tab="songs">Songs</button><button class="x-chip'+(statsTab==='artists'?' on':'')+'" data-tab="artists">Artists</button></div>'
    +list
    +'<div class="x-sep"></div>'
    +'<button class="x-item x-danger" data-reset="1">Reset stats</button>'
    +'<button class="x-item x-cancel" data-x="close">Close</button>');
  byId('spoXCard').onclick=function(e){
    var tb=e.target.closest('[data-tab]');
    if(tb){hap();statsTab=tb.getAttribute('data-tab');openStats();return;}
    var r=e.target.closest('[data-reset]');
    if(r){
      if(r.__armed){S={t:{},a:{},d:{},since:Date.now()};curKey='';saveStats();openStats();}
      else{r.__armed=1;r.textContent='Tap again to erase all stats';}
    }
  };
}
window.spoOpenStats=openStats;

/* account menu: "Your stats" next to Dev */
function addStatsItem(){
  var dev=byId('spo-dev-item');if(!dev||byId('spo-stats-item'))return;
  var li=dev.cloneNode(true);li.id='spo-stats-item';li.style.display='';
  var walk=document.createTreeWalker(li,NodeFilter.SHOW_TEXT);
  while(walk.nextNode()){if(walk.currentNode.nodeValue.trim()){walk.currentNode.nodeValue='Your stats';break;}}
  li.addEventListener('click',function(e){
    e.preventDefault();e.stopPropagation();hap();
    document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',code:'Escape',keyCode:27,bubbles:true}));
    setTimeout(openStats,120);
  },true);
  dev.parentNode.insertBefore(li,dev);
}

/* ---------- hide podcasts and audiobooks ---------- */
var POD=/\/(show|episode|audiobook)\//;
function tagPods(){
  var home=qs('main section[data-testid=home-page]');
  if(home){
    var shelves=home.querySelectorAll('section[data-testid=component-shelf]:not([data-spo-podchk])');
    for(var i=0;i<shelves.length;i++){
      /* grid shelves link their cards; newer carousel shelves only name them in aria-labelledby */
      var links=shelves[i].querySelectorAll('[data-encore-id=card]');if(links.length<2)links=shelves[i].querySelectorAll('[data-testid=grid-container] a[href]');if(links.length<2)continue;
      var p=0;
      for(var j=0;j<links.length;j++){
        var a=links[j].matches('a[href]')?links[j]:links[j].querySelector('a[href]');
        var ref=(links[j].getAttribute('aria-labelledby')||'')+' '+(a?a.getAttribute('href'):'');
        if(POD.test(ref)||/:(show|episode|audiobook):/.test(ref))p++;
      }
      shelves[i].setAttribute('data-spo-podchk','');
      if(p*2>=links.length)shelves[i].setAttribute('data-spo-pod','');
    }
  }
  var chips=document.querySelectorAll('[data-encore-id=chip]:not([data-spo-podchk])');
  for(var c=0;c<chips.length;c++){
    var t=txt(chips[c]);chips[c].setAttribute('data-spo-podchk','');
    if(/^(Podcasts|Audiobooks)/i.test(t)){var b=chips[c].closest('button')||chips[c];b.setAttribute('data-spo-pod','');}
  }
}

/* ---------- settings ---------- */
window.spoXApply=function(){
  X=window.__spoX||{};
  var r=document.documentElement;
  if(r.classList.contains('spo-nopods')!==!!X.hidePods)r.classList.toggle('spo-nopods',!!X.hidePods);
};
window.spoXApply();

setInterval(function(){
  if(window.__splBg)return;
  hookNP();hookMini();
  if(X.hidePods)tagPods();
  if(qs('[data-tippy-root] [role=menu]'))addStatsItem();
  if(speed!==1)applySpeed();
},500);
setInterval(statTick,1000);
})();
"""
}
