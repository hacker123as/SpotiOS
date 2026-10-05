package com.project.lol.webview.injections

import org.json.JSONObject

/**
 * The SpotiOS Server screen, shown instead of the web player in Server Mode.
 *
 * Shows what's playing and on which device, whether SpotiOS is ready on Spotify Connect,
 * how to connect to it, and lets you play something on SpotiOS (recent songs, playlists,
 * liked songs, search). It also runs Server Mode's device rules:
 *  - the default device: the first device that sends music to SpotiOS is remembered, and
 *    when it starts playing on its own, SpotiOS takes the music over (auto-connect);
 *  - who may play here: everyone on the account, or only allowed devices. Spotify has no
 *    way to refuse a connection, so music sent from a blocked device is sent straight back.
 *
 * Connect state comes from ConnectKeepAlive (window.__spoConn) and the Spotify Web API with
 * the player's own login. Settings live in localStorage ("spoSrv").
 */
object ServerScreen {

    fun content(on: Boolean): String =
        "window.__spoServer=$on;\n" + JS.replace("__SRV_CSS__", JSONObject.quote(CSS))

    private const val CSS = """
:root{--ss-acc:var(--spl-accent,#1ed760);--ss-ink2:rgba(235,235,245,.62);--ss-ink3:rgba(235,235,245,.38);--ss-glass:rgba(255,255,255,.06);
  --ss-rim:rgba(255,255,255,.10);--ss-sep:rgba(255,255,255,.07);--ss-spring:cubic-bezier(.34,1.56,.64,1);--ss-ease:cubic-bezier(.22,1,.36,1)}
#spoSrv{position:fixed;inset:0;z-index:2147483646;background:#050608;color:#fff;display:flex;flex-direction:column;
  font-family:-apple-system,"SF Pro Text","Inter",Roboto,"Segoe UI",Helvetica,Arial,sans-serif;-webkit-font-smoothing:antialiased;
  opacity:0;visibility:hidden;pointer-events:none;transition:opacity .35s cubic-bezier(.22,1,.36,1),visibility 0s .35s;overflow:hidden}
#spoSrv.ss-show{opacity:1;visibility:visible;pointer-events:auto;transition:opacity .35s cubic-bezier(.22,1,.36,1)}
html.ss-on #main,html.ss-on #spotilolPlayerControls,html.ss-on #spoTabs,html.ss-on #spoNP,html.ss-on #spoScrim,html.ss-on #spoBackBtn,
html.ss-on [data-tippy-root]{visibility:hidden!important}
:where(#spoSrv,#ss-sheet) *{box-sizing:border-box;-webkit-tap-highlight-color:transparent}
:where(#spoSrv,#ss-sheet) button{font:inherit;color:inherit;background:none;border:0;padding:0;cursor:pointer;touch-action:manipulation}
:where(#spoSrv,#ss-sheet) svg{width:22px;height:22px;flex:none}
.ss-bg{position:absolute;inset:-25%;background:#050608 center/cover no-repeat;filter:blur(70px) saturate(1.6) brightness(.8);opacity:0;transform:scale(1.15);transition:opacity 1s,background-image .6s}
.ss-bg.has{opacity:.6}
.ss-shade{position:absolute;inset:0;background:linear-gradient(180deg,rgba(5,6,8,.15) 0%,rgba(5,6,8,.55) 38%,#050608 72%)}
.ss-top{position:relative;display:flex;align-items:center;gap:10px;padding:calc(env(safe-area-inset-top,0px) + 12px) 16px 10px;z-index:2}
.ss-badge{display:flex;align-items:center;gap:9px;min-width:0;flex:1;padding:8px 14px 8px 12px;border-radius:999px;background:rgba(255,255,255,.08);
  border:1px solid var(--ss-rim);-webkit-backdrop-filter:blur(24px) saturate(1.6);backdrop-filter:blur(24px) saturate(1.6)}
.ss-badge b{font-size:14px;font-weight:700;letter-spacing:-.01em;white-space:nowrap}
.ss-badge span{font-size:13px;color:var(--ss-ink2);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.ss-dot{position:relative;width:9px;height:9px;border-radius:50%;background:#8e8e93;flex:none}
.ss-dot.ok{background:var(--ss-acc)}
.ss-dot.ok::after{content:"";position:absolute;inset:-5px;border-radius:50%;border:2px solid var(--ss-acc);opacity:0;animation:ssPing 2.4s cubic-bezier(.22,1,.36,1) infinite}
.ss-dot.warn{background:#ffb020}.ss-dot.bad{background:#ff453a}
@keyframes ssPing{0%{transform:scale(.4);opacity:.9}100%{transform:scale(1.6);opacity:0}}
.ss-ib{width:42px;height:42px;border-radius:50%;display:flex;align-items:center;justify-content:center;background:rgba(255,255,255,.08);
  border:1px solid var(--ss-rim);-webkit-backdrop-filter:blur(24px);backdrop-filter:blur(24px);flex:none;transition:transform .3s var(--ss-spring)}
.ss-ib:active{transform:scale(.88)}
.ss-scroll{position:relative;flex:1;overflow-y:auto;overflow-x:hidden;-webkit-overflow-scrolling:touch;overscroll-behavior:contain;
  padding:4px 16px calc(env(safe-area-inset-bottom,0px) + 34px);z-index:1;scrollbar-width:none}
.ss-scroll::-webkit-scrollbar{display:none}
.ss-np{display:flex;flex-direction:column;align-items:center;text-align:center;padding:8px 4px 4px}
.ss-art{position:relative;width:min(70vw,330px);aspect-ratio:1/1;border-radius:22px;margin:6px auto 22px;transition:transform .55s var(--ss-spring)}
.ss-art img{position:absolute;inset:0;width:100%;height:100%;object-fit:cover;border-radius:22px;box-shadow:0 30px 70px rgba(0,0,0,.6),0 0 0 1px rgba(255,255,255,.06);opacity:0;transition:opacity .4s}
.ss-art.has img{opacity:1}
.ss-art.paused{transform:scale(.9)}
.ss-idle{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;transition:opacity .4s}
.ss-art.has .ss-idle{opacity:0;pointer-events:none}
.ss-idle i{position:absolute;inset:18%;border-radius:50%;border:1.5px solid rgba(30,215,96,.55);opacity:0;animation:ssRing 3.6s cubic-bezier(.22,1,.36,1) infinite}
.ss-idle i:nth-child(2){animation-delay:1.2s}.ss-idle i:nth-child(3){animation-delay:2.4s}
@keyframes ssRing{0%{transform:scale(.45);opacity:.9}100%{transform:scale(1.35);opacity:0}}
.ss-core{position:relative;width:38%;aspect-ratio:1/1;border-radius:50%;display:flex;align-items:center;justify-content:center;
  background:radial-gradient(circle at 35% 30%,rgba(30,215,96,.35),rgba(30,215,96,.08) 60%,rgba(255,255,255,.03));border:1px solid rgba(30,215,96,.35);
  box-shadow:0 0 60px rgba(30,215,96,.18),inset 0 1px 0 rgba(255,255,255,.18)}
.ss-core svg{width:44%;height:44%;color:var(--ss-acc)}
.ss-title{font-size:23px;line-height:1.2;font-weight:800;letter-spacing:-.02em;max-width:100%;overflow:hidden;text-overflow:ellipsis;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
.ss-artist{margin-top:5px;font-size:15.5px;color:var(--ss-ink2);max-width:100%;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.ss-prog{width:100%;margin-top:18px}
.ss-bar{position:relative;height:4px;border-radius:2px;background:rgba(255,255,255,.16);overflow:hidden}
.ss-bar i{position:absolute;left:0;top:0;bottom:0;width:100%;background:#fff;border-radius:2px;transform-origin:left;transform:scaleX(0);transition:transform .5s linear}
.ss-times{display:flex;justify-content:space-between;margin-top:7px;font-size:11.5px;color:var(--ss-ink3);font-variant-numeric:tabular-nums}
.ss-np.idle .ss-prog,.ss-np.idle .ss-ctrl{display:none}
.ss-ctrl{display:flex;align-items:center;justify-content:center;gap:34px;margin:10px 0 6px}
.ss-ctrl button{width:52px;height:52px;display:flex;align-items:center;justify-content:center;border-radius:50%;transition:transform .3s var(--ss-spring),opacity .2s}
.ss-ctrl button svg{width:30px;height:30px}
.ss-ctrl button:active{transform:scale(.84)}
.ss-ctrl .ss-pp{width:70px;height:70px;background:#fff;color:#000;box-shadow:0 10px 30px rgba(0,0,0,.35)}
.ss-ctrl .ss-pp svg{width:32px;height:32px}
.ss-on{display:inline-flex;align-items:center;gap:8px;margin-top:12px;padding:9px 14px;border-radius:999px;background:rgba(255,255,255,.07);
  border:1px solid var(--ss-rim);font-size:13.5px;font-weight:600;max-width:100%}
.ss-on span{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.ss-on svg{width:17px;height:17px}
.ss-on.mine{color:var(--ss-acc);background:rgba(30,215,96,.12);border-color:rgba(30,215,96,.3)}
.ss-card{margin-top:14px;padding:16px;border-radius:22px;background:var(--ss-glass);border:1px solid var(--ss-rim);
  -webkit-backdrop-filter:blur(28px) saturate(1.5);backdrop-filter:blur(28px) saturate(1.5)}
.ss-h{display:flex;align-items:center;gap:8px;margin:0 0 10px;font-size:12.5px;font-weight:700;letter-spacing:.06em;text-transform:uppercase;color:var(--ss-ink3)}
.ss-h em{margin-left:auto;font-style:normal;text-transform:none;letter-spacing:0;font-weight:600;color:var(--ss-acc);font-size:13.5px}
.ss-row{display:flex;align-items:center;gap:12px;min-height:50px;padding:8px 0}
.ss-row+.ss-row{border-top:1px solid var(--ss-sep)}
.ss-ico{width:36px;height:36px;border-radius:11px;display:flex;align-items:center;justify-content:center;background:rgba(255,255,255,.08);flex:none}
.ss-ico svg{width:19px;height:19px}
.ss-ico.g{background:rgba(30,215,96,.14);color:var(--ss-acc)}.ss-ico.r{background:rgba(255,69,58,.14);color:#ff6961}.ss-ico.y{background:rgba(255,176,32,.14);color:#ffb020}
.ss-tx{flex:1;min-width:0}
.ss-tx b{display:block;font-size:15px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.ss-tx small{display:block;margin-top:2px;font-size:12.5px;line-height:1.35;color:var(--ss-ink2)}
.ss-sw{position:relative;width:51px;height:31px;border-radius:16px;background:rgba(120,120,128,.36);flex:none;transition:background .25s}
.ss-sw::after{content:"";position:absolute;top:2px;left:2px;width:27px;height:27px;border-radius:50%;background:#fff;box-shadow:0 3px 8px rgba(0,0,0,.2);transition:transform .32s var(--ss-spring)}
.ss-sw.on{background:var(--ss-acc)}
.ss-sw.on::after{transform:translateX(20px)}
.ss-sw.dis{opacity:.45}
.ss-btn{display:inline-flex;align-items:center;justify-content:center;gap:7px;min-height:38px;padding:0 15px;border-radius:999px;
  background:rgba(255,255,255,.1);font-size:13.5px;font-weight:650;white-space:nowrap;transition:transform .3s var(--ss-spring),background .2s}
.ss-btn:active{transform:scale(.93)}
.ss-btn svg{width:17px;height:17px}
.ss-btn.pri{background:var(--ss-acc);color:#000}
.ss-btn.dng{color:#ff6961;background:rgba(255,69,58,.12)}
.ss-btn.sm{min-height:32px;padding:0 12px;font-size:12.5px}
.ss-btns{display:flex;flex-wrap:wrap;gap:8px;margin-top:12px}
.ss-steps{counter-reset:ss;margin:4px 0 0;padding:0;list-style:none}
.ss-steps li{position:relative;padding:0 0 14px 42px;font-size:14.5px;line-height:1.4}
.ss-steps li::before{counter-increment:ss;content:counter(ss);position:absolute;left:0;top:-2px;width:28px;height:28px;border-radius:50%;
  display:flex;align-items:center;justify-content:center;background:rgba(30,215,96,.15);color:var(--ss-acc);font-weight:800;font-size:13.5px}
.ss-steps li::after{content:"";position:absolute;left:13.5px;top:30px;bottom:2px;width:1px;background:rgba(30,215,96,.25)}
.ss-steps li:last-child::after{display:none}
.ss-steps small{display:block;margin-top:2px;color:var(--ss-ink2);font-size:12.5px}
.ss-note{margin-top:4px;font-size:12.5px;line-height:1.45;color:var(--ss-ink3)}
.ss-fold{display:none}
.ss-card.open .ss-fold{display:block}
.ss-card .ss-h .ss-chev{margin-left:auto;transition:transform .3s var(--ss-ease);width:18px;height:18px;color:var(--ss-ink3)}
.ss-card.open .ss-h .ss-chev{transform:rotate(90deg)}
.ss-seg{display:flex;padding:3px;border-radius:12px;background:rgba(118,118,128,.24);margin-bottom:6px}
.ss-seg button{flex:1;min-height:34px;border-radius:9px;font-size:13px;font-weight:600;color:var(--ss-ink2);transition:background .25s,color .25s}
.ss-seg button.on{background:rgba(255,255,255,.16);color:#fff;box-shadow:0 2px 8px rgba(0,0,0,.25)}
.ss-tag{display:inline-block;margin-left:6px;padding:1px 7px;border-radius:6px;font-size:10.5px;font-weight:700;letter-spacing:.02em;vertical-align:2px;background:rgba(30,215,96,.16);color:var(--ss-acc)}
.ss-tag.b{background:rgba(255,69,58,.16);color:#ff6961}.ss-tag.n{background:rgba(255,255,255,.1);color:var(--ss-ink2)}
.ss-search{position:relative;margin:2px 0 10px}
.ss-search input{width:100%;height:42px;border-radius:12px;border:0;outline:0;background:rgba(118,118,128,.24);color:#fff;font:inherit;font-size:15px;padding:0 38px 0 40px}
.ss-search input::placeholder{color:var(--ss-ink3)}
.ss-search>svg{position:absolute;left:12px;top:11px;width:20px;height:20px;color:var(--ss-ink3);pointer-events:none}
.ss-search button{position:absolute;right:6px;top:6px;width:30px;height:30px;border-radius:50%;display:none;align-items:center;justify-content:center;color:var(--ss-ink2)}
.ss-search.has button{display:flex}
.ss-search button svg{width:16px;height:16px}
.ss-chips{display:flex;gap:8px;margin-bottom:6px;overflow-x:auto;scrollbar-width:none}
.ss-chips::-webkit-scrollbar{display:none}
.ss-chips button{flex:none;min-height:32px;padding:0 14px;border-radius:999px;background:rgba(255,255,255,.08);font-size:13px;font-weight:600;color:var(--ss-ink2);transition:background .2s,color .2s}
.ss-chips button.on{background:var(--ss-acc);color:#000}
.ss-list{min-height:60px}
.ss-it{display:flex;align-items:center;gap:12px;padding:7px 6px;margin:0 -6px;border-radius:12px;transition:background .18s,transform .3s var(--ss-spring)}
.ss-it:active{background:rgba(255,255,255,.07);transform:scale(.98)}
.ss-it img,.ss-it .ss-ph{width:46px;height:46px;border-radius:7px;object-fit:cover;background:rgba(255,255,255,.08);flex:none}
.ss-ph{display:flex;align-items:center;justify-content:center;color:var(--ss-ink3)}
.ss-ph.lk{background:linear-gradient(135deg,#450af5,#8e8ee5);color:#fff}
.ss-it .ss-tx b{font-size:14.5px;font-weight:550}
.ss-it .ss-go{width:30px;height:30px;display:flex;align-items:center;justify-content:center;color:var(--ss-ink3);flex:none}
.ss-it .ss-go svg{width:18px;height:18px}
.ss-it.busy .ss-go{color:var(--ss-acc)}
.ss-it.busy .ss-go svg{animation:ssSpin 1s linear infinite}
.ss-it.now .ss-tx b{color:var(--ss-acc)}
@keyframes ssSpin{to{transform:rotate(360deg)}}
.ss-msg{padding:18px 4px;text-align:center;font-size:13.5px;line-height:1.45;color:var(--ss-ink2)}
.ss-msg .ss-btn{margin-top:10px}
.ss-log{font-size:13px;line-height:1.4}
.ss-log div{display:flex;gap:10px;padding:6px 0;color:var(--ss-ink2)}
.ss-log div+div{border-top:1px solid var(--ss-sep)}
.ss-log i{width:7px;height:7px;border-radius:50%;background:#8e8e93;margin-top:6px;flex:none}
.ss-log i.good{background:var(--ss-acc)}.ss-log i.bad{background:#ff453a}
.ss-log span{flex:1;min-width:0}
.ss-log time{color:var(--ss-ink3);font-size:12px;white-space:nowrap}
.ss-foot{margin:20px 4px 0;text-align:center;font-size:12px;line-height:1.5;color:var(--ss-ink3)}
#ss-scrim{position:fixed;inset:0;z-index:2147483646;background:rgba(0,0,0,.5);opacity:0;pointer-events:none;transition:opacity .3s}
#ss-scrim.open{opacity:1;pointer-events:auto}
#ss-sheet{position:fixed;left:0;right:0;bottom:0;z-index:2147483647;max-height:78vh;display:flex;flex-direction:column;border-radius:26px 26px 0 0;
  background:rgba(28,28,30,.92);border-top:1px solid var(--ss-rim);-webkit-backdrop-filter:blur(40px) saturate(1.6);backdrop-filter:blur(40px) saturate(1.6);
  padding:8px 16px calc(env(safe-area-inset-bottom,0px) + 18px);transform:translateY(105%);transition:transform .45s cubic-bezier(.32,.72,0,1);color:#fff;
  font-family:-apple-system,"SF Pro Text","Inter",Roboto,"Segoe UI",Helvetica,Arial,sans-serif}
#ss-sheet.open{transform:none}
#ss-sheet .ss-grab{width:38px;height:5px;border-radius:3px;background:rgba(255,255,255,.25);margin:2px auto 12px;flex:none}
#ss-sheet h4{margin:0 0 4px;font-size:19px;font-weight:800;letter-spacing:-.01em}
#ss-sheet p{margin:0 0 10px;font-size:13px;line-height:1.4;color:var(--ss-ink2)}
#ss-sheet .ss-sb{overflow-y:auto;margin:0 -4px;padding:0 4px}
#ss-sheet .ss-row{cursor:pointer}
#ss-sheet .ss-row.cur .ss-tx b{color:var(--ss-acc)}
#ss-sheet .ss-chk{width:22px;height:22px;color:var(--ss-acc);opacity:0}
#ss-sheet .ss-row.cur .ss-chk{opacity:1}
#ss-toast{position:fixed;left:50%;top:calc(env(safe-area-inset-top,0px) + 70px);z-index:2147483647;transform:translate(-50%,-14px);opacity:0;pointer-events:none;
  padding:10px 16px;border-radius:999px;background:rgba(40,40,44,.94);border:1px solid var(--ss-rim);font:600 13.5px/1.3 -apple-system,Roboto,sans-serif;color:#fff;
  box-shadow:0 10px 30px rgba(0,0,0,.4);transition:opacity .25s,transform .4s cubic-bezier(.34,1.56,.64,1);max-width:86vw;text-align:center}
#ss-toast.show{opacity:1;transform:translate(-50%,0)}
#ss-pill{position:fixed;right:14px;bottom:calc(var(--spo-dock,110px) + 10px);z-index:2147483645;display:none;align-items:center;gap:8px;padding:9px 14px 9px 12px;
  border-radius:999px;background:rgba(20,20,22,.82);border:1px solid rgba(30,215,96,.35);-webkit-backdrop-filter:blur(24px);backdrop-filter:blur(24px);
  font:700 13px/1 -apple-system,Roboto,sans-serif;color:#fff;box-shadow:0 8px 24px rgba(0,0,0,.4)}
#ss-pill.show{display:flex}
@media (min-width:700px) and (orientation:landscape){
  .ss-scroll{display:grid;grid-template-columns:minmax(300px,1fr) minmax(320px,1.1fr);column-gap:22px;align-items:start}
  .ss-np{grid-row:1/span 9;position:sticky;top:0}
  .ss-art{width:min(36vw,330px)}
}
"""

    private const val JS = """
(function(){
if(window.__spoSrvInit)return;window.__spoSrvInit=true;
var CSS=__SRV_CSS__;
var W=window;
function byId(i){return document.getElementById(i);}
function esc(v){return String(v==null?'':v).replace(/[&<>"]/g,function(c){return c==='&'?'&amp;':c==='<'?'&lt;':c==='>'?'&gt;':'&quot;';});}
function haptic(){try{AndBridge.haptic();}catch(e){}}
function now(){return Date.now();}
function fmt(ms){var s=Math.max(0,Math.floor((ms||0)/1000)),m=Math.floor(s/60);s=s%60;return m+':'+(s<10?'0':'')+s;}
function ago(t){var d=Math.round((now()-t)/1000);if(d<45)return 'just now';d=Math.round(d/60);if(d<60)return d+' min ago';d=Math.round(d/60);if(d<24)return d+' h ago';return Math.round(d/24)+' d ago';}
function span(ms){var m=Math.floor(ms/60000);if(m<1)return 'under a minute';if(m<60)return m+' min';var h=Math.floor(m/60);m=m%60;if(h<24)return h+' h'+(m?' '+m+' min':'');return Math.floor(h/24)+' d '+(h%24)+' h';}

var I={
  play:'<svg viewBox="0 0 24 24"><path fill="currentColor" d="M8 5.14v13.72a1 1 0 0 0 1.5.86l11-6.86a1 1 0 0 0 0-1.72l-11-6.86A1 1 0 0 0 8 5.14z"/></svg>',
  pause:'<svg viewBox="0 0 24 24"><path fill="currentColor" d="M7 4h3a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1zm7 0h3a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-3a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z"/></svg>',
  prev:'<svg viewBox="0 0 24 24"><path fill="currentColor" d="M6 5a1 1 0 0 1 1 1v12a1 1 0 1 1-2 0V6a1 1 0 0 1 1-1zm13.4.6a1 1 0 0 1 .6.9v11a1 1 0 0 1-1.6.8l-8-5.5a1 1 0 0 1 0-1.6l8-5.5a1 1 0 0 1 1-.1z"/></svg>',
  next:'<svg viewBox="0 0 24 24"><path fill="currentColor" d="M18 5a1 1 0 0 1 1 1v12a1 1 0 1 1-2 0V6a1 1 0 0 1 1-1zM4.6 5.6a1 1 0 0 1 1 .1l8 5.5a1 1 0 0 1 0 1.6l-8 5.5A1 1 0 0 1 4 17.5v-11a1 1 0 0 1 .6-.9z"/></svg>'
};
function st_(d){return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="'+d+'"/></svg>';}
var P={
  cast:'M18.36 19.36a9 9 0 1 0-12.72 0M15.54 16.54a5 5 0 1 0-7.08 0M12 13.5v.01',
  chev:'M9 6l6 6-6 6',grid:'M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z',x:'M18 6L6 18M6 6l12 12',
  search:'M10 17a7 7 0 1 0 0-14a7 7 0 0 0 0 14zM21 21l-6-6',
  phone:'M8 3h8a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zM11 4h2M12 17v.01',
  laptop:'M3 19h18M6 5h12a1 1 0 0 1 1 1v10H5V6a1 1 0 0 1 1-1z',tablet:'M6 3h12a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zM11 17h2',
  tv:'M3 7h18v12H3zM16 3l-4 4-4-4',car:'M5 17H3v-6l2-5h9l4 5h1a2 2 0 0 1 2 2v4h-2M9 17h6M7 19a2 2 0 1 0 0-4a2 2 0 0 0 0 4zM17 19a2 2 0 1 0 0-4a2 2 0 0 0 0 4z',
  game:'M2 9a3 3 0 0 1 3-3h14a3 3 0 0 1 3 3v6a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3zM6 12h4M8 10v4M15 11v.01M18 13v.01',
  speaker:'M7 3h10a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zM12 17a3 3 0 1 0 0-6a3 3 0 0 0 0 6zM12 7v.01',
  check:'M5 12l5 5L20 7',power:'M7 6a7.75 7.75 0 1 0 10 0M12 4v8',
  battery:'M6 7h11a2 2 0 0 1 2 2v.5h1v5h-1v.5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z',
  bell:'M10 5a2 2 0 1 1 4 0a7 7 0 0 1 4 6v3a4 4 0 0 0 2 3H4a4 4 0 0 0 2-3v-3a7 7 0 0 1 4-6M9 17v1a3 3 0 0 0 6 0v-1',
  refresh:'M20 11A8.1 8.1 0 0 0 4.5 9M4 5v4h4M4 13a8.1 8.1 0 0 0 15.5 2M20 19v-4h-4',
  star:'M12 17.75l-6.17 3.24 1.18-6.87-5-4.86 6.9-1L12 2l3.09 6.26 6.9 1-5 4.86 1.18 6.87z',
  shield:'M12 3a12 12 0 0 0 8.5 3A12 12 0 0 1 12 21 12 12 0 0 1 3.5 6 12 12 0 0 0 12 3M9 12l2 2 4-4',
  help:'M12 21a9 9 0 1 0 0-18a9 9 0 0 0 0 18zM12 17v.01M12 13.5a1.5 1.5 0 0 1 1-1.5a2.6 2.6 0 1 0-3-4',
  swipe:'M4 12h12M12 6l6 6-6 6M20 4v16',spotify:'M12 21a9 9 0 1 0 0-18a9 9 0 0 0 0 18zM8 11.5c2.5-1 5.5-.8 8 .5M8.5 14.5c2-.7 4.3-.6 6.3.4M7.5 8.6c3-1.1 6.8-.9 9.8.6',
  heart:'M19.5 12.57L12 20l-7.5-7.43A5 5 0 1 1 12 6.01a5 5 0 1 1 7.5 6.57',music:'M6 20a3 3 0 1 0 0-6a3 3 0 0 0 0 6zM16 18a3 3 0 1 0 0-6a3 3 0 0 0 0 6zM9 17V5l10-2v12',
  settings:'M10.3 4.3c.4-1.8 3-1.8 3.4 0a1.7 1.7 0 0 0 2.6 1.1c1.5-.9 3.3.8 2.4 2.4a1.7 1.7 0 0 0 1 2.5c1.8.5 1.8 3 0 3.5a1.7 1.7 0 0 0-1 2.6c.9 1.5-.9 3.3-2.4 2.3a1.7 1.7 0 0 0-2.6 1.1c-.4 1.8-3 1.8-3.4 0a1.7 1.7 0 0 0-2.6-1.1c-1.5.9-3.3-.8-2.4-2.3a1.7 1.7 0 0 0-1-2.6c-1.8-.4-1.8-3 0-3.4a1.7 1.7 0 0 0 1-2.6c-.9-1.6.9-3.3 2.4-2.4a1.7 1.7 0 0 0 2.6-1.1zM12 15a3 3 0 1 0 0-6a3 3 0 0 0 0 6z',
  history:'M12 8v4l2 2M3.05 11a9 9 0 1 1 .5 4M3 20v-5h5',list:'M9 6h11M9 12h11M9 18h11M5 6v.01M5 12v.01M5 18v.01',
  block:'M12 21a9 9 0 1 0 0-18a9 9 0 0 0 0 18zM5.7 5.7l12.6 12.6',link:'M9 15l6-6M11 6l.46-.54a5 5 0 0 1 7.07 7.07L18 13M13 18l-.4.53a5.07 5.07 0 0 1-7.12 0a4.97 4.97 0 0 1 0-7.07L6 11'
};
function ic(n){return P[n]?st_(P[n]):(I[n]||'');}
function devIc(t){t=String(t||'').toLowerCase();
  if(/phone/.test(t))return 'phone';if(/computer|laptop/.test(t))return 'laptop';if(/tablet/.test(t))return 'tablet';
  if(/tv|stb|cast_?video|castvideo/.test(t))return 'tv';if(/auto|car/.test(t))return 'car';if(/game|console/.test(t))return 'game';return 'speaker';}
function devLabel(t){t=String(t||'').toLowerCase().replace(/[_\s]/g,'');
  var m={computer:'Computer',smartphone:'Phone',tablet:'Tablet',speaker:'Speaker',tv:'TV',avr:'Receiver',stb:'TV box',audiodongle:'Speaker',gameconsole:'Console',castvideo:'Chromecast',castaudio:'Speaker group',automobile:'Car',smartwatch:'Watch'};
  return m[t]||'Spotify Connect';}
function normType(t){return String(t||'').toLowerCase().replace(/[_\s]/g,'');}

/* ---------- settings ---------- */
var LS='spoSrv',cfg={};
try{cfg=JSON.parse(localStorage.getItem(LS)||'{}')||{};}catch(e){cfg={};}
if(typeof cfg!=='object')cfg={};
if(cfg.auto===undefined)cfg.auto=true;
if(cfg.policy!=='list')cfg.policy='all';
cfg.devs=cfg.devs&&typeof cfg.devs==='object'?cfg.devs:{};
cfg.log=Array.isArray(cfg.log)?cfg.log:[];
function save(){try{localStorage.setItem(LS,JSON.stringify(cfg));}catch(e){}}
function log(m,k){cfg.log.unshift({t:now(),m:m,k:k||''});if(cfg.log.length>30)cfg.log.length=30;save();if(shown)renderLog();}

/* ---------- Spotify Web API with the player's own login ---------- */
function api(path,opt){
  var tok=W.spotAuthToken;
  if(!tok)return Promise.reject(new Error('signin'));
  if(navigator.onLine===false)return Promise.reject(new Error('offline'));
  opt=opt||{};
  var f=W.oriFetch||W.fetch;
  return f('https://api.spotify.com/v1'+path,{method:opt.method||'GET',headers:{'Authorization':tok,'Content-Type':'application/json'},body:opt.body?JSON.stringify(opt.body):undefined})
    .then(function(r){
      if(r.status===204||r.status===202)return null;
      if(!r.ok)throw new Error('http'+r.status);
      return r.text().then(function(t){try{return t?JSON.parse(t):null;}catch(e){return null;}});
    });
}
function why(e){var m=String(e&&e.message||'');
  return m==='offline'?'You’re offline.':m==='signin'?'Spotify is still signing in.':m==='http429'?'Spotify is busy, try again in a moment.':m==='http403'?'Spotify didn’t allow that (Premium may be needed).':m==='http404'?'Spotify couldn’t find SpotiOS yet. Give it a few seconds.':'Couldn’t reach Spotify.';}

/* ---------- who is who ---------- */
function myId(){var c=W.__spoConn;return (c&&c.myId)||W.__spoMyId||'';}
function myName(){return W.__spoDeviceName||'SpotiOS';}
function isMine(d){if(!d)return false;var id=myId();if(id)return d.id===id;return d.name===myName();}
function same(a,b){if(!a||!b)return false;if(a.id&&b.id&&a.id===b.id)return true;return !!a.name&&a.name===b.name&&normType(a.type)===normType(b.type);}
function rec(d){return cfg.devs[d.id]||null;}
function known(d){
  if(!d||!d.id||isMine(d))return;
  var e=cfg.devs[d.id]||{};e.name=d.name||e.name||'Device';e.type=d.type||e.type||'';e.seen=now();cfg.devs[d.id]=e;
  /* a phone's Spotify can come back with a new id after a reinstall: carry the old choice over */
  if(e.allow===undefined){for(var k in cfg.devs){var o=cfg.devs[k];if(k!==d.id&&o.allow!==undefined&&o.name===e.name&&normType(o.type)===normType(e.type)){e.allow=o.allow;break;}}}
  var ks=Object.keys(cfg.devs);
  if(ks.length>40){ks.sort(function(a,b){return (cfg.devs[a].seen||0)-(cfg.devs[b].seen||0);});for(var i=0;i<ks.length-40;i++)delete cfg.devs[ks[i]];}
}
function isDef(d){return !!cfg.def&&same(d,cfg.def);}
function allowed(d){
  if(!d||isDef(d))return true;
  var e=rec(d);
  if(!e){for(var k in cfg.devs){var o=cfg.devs[k];if(o.name===d.name&&normType(o.type)===normType(d.type)){e=o;break;}}}
  if(e&&e.allow===false)return false;
  if(cfg.policy==='list')return !!(e&&e.allow===true);
  return true;
}

/* ---------- Connect state: dealer pushes (instant) with Web API polling as backup ---------- */
var S={active:null,playing:false,devices:[],mineAt:0,snooze:0,pullAt:0,localUntil:0,pollAt:0,devAt:0,bounceAt:0};
function fromCluster(c){
  var devs=c.devices||{},list=[];
  for(var k in devs){var d=devs[k]||{};list.push({id:d.device_id||k,name:d.name||'',type:d.device_type||''});}
  var aid=c.active_device_id||'',a=null;
  for(var i=0;i<list.length;i++)if(list[i].id===aid)a=list[i];
  if(aid&&!a)a={id:aid,name:'',type:''};
  var ps=c.player_state||{};
  onState(a,!!(ps.is_playing&&!ps.is_paused),list,'dealer');
}
if(W.__spoConn){
  W.__spoConn.onCluster.push(fromCluster);
  if(W.__spoConn.cluster)setTimeout(function(){try{fromCluster(W.__spoConn.cluster);}catch(e){}},0);
}
function poll(force){
  if(!W.__spoServer||!W.spotAuthToken)return;
  var c=W.__spoConn,fresh=c&&c.clusterAt&&now()-c.clusterAt<60000;
  if(!force&&fresh&&now()-S.pollAt<60000)return;
  S.pollAt=now();
  api('/me/player').then(function(p){
    if(!p||!p.device){onState(null,false,null,'api');return;}
    onState({id:p.device.id,name:p.device.name,type:p.device.type},!!p.is_playing,null,'api');
  }).catch(function(){});
  if(force||now()-S.devAt>120000){
    S.devAt=now();
    api('/me/player/devices').then(function(j){
      var l=((j&&j.devices)||[]).map(function(d){return {id:d.id,name:d.name,type:d.type};});
      S.devices=l;l.forEach(known);save();if(shown)renderDevs();
    }).catch(function(){});
  }
}
function onState(a,playing,list,src){
  if(list){S.devices=list.filter(function(d){return d.id;});S.devices.forEach(known);}
  var prev=S.active;
  if(a&&!a.name&&prev&&prev.id===a.id)a=prev;
  if(a&&!a.name){for(var i=0;i<S.devices.length;i++)if(S.devices[i].id===a.id)a=S.devices[i];}
  if(a&&!a.name&&rec(a))a={id:a.id,name:rec(a).name,type:rec(a).type};
  S.playing=playing;
  var changed=(!prev!==!a)||(prev&&a&&prev.id!==a.id);
  S.active=a;
  if(changed&&W.__spoServer){
    if(a&&isMine(a)){
      S.mineAt=now();
      connected(prev&&!isMine(prev)?prev:null);
    }else if(a&&prev&&isMine(prev)){
      log('Music moved to '+(a.name||'another device'),'');
      /* someone moved it off SpotiOS on purpose: don't grab it back for a while */
      S.snooze=now()+30*60000;
    }
  }
  if(a&&isMine(a))S.mineAt=now();
  save();
  autoConnect();
  if(shown){renderOn();renderDevs();}
}
function connected(from){
  if(now()<S.localUntil){log('Playing on SpotiOS','good');return;}
  if(!from){log('A device started playing on SpotiOS','good');return;}
  known(from);
  if(!allowed(from)){
    if(now()-S.bounceAt<5000)return;
    S.bounceAt=now();
    log('Blocked '+from.name+' and sent the music back','bad');
    toast('Blocked '+from.name);
    S.localUntil=now()+8000;
    api('/me/player',{method:'PUT',body:{device_ids:[from.id],play:true}}).catch(function(){try{W.actPlayPause&&W.actPlayPause(false);}catch(e){}});
    return;
  }
  cfg.connectedOnce=true;
  if(!cfg.def){
    cfg.def={id:from.id,name:from.name,type:from.type};
    log(from.name+' is now your default device','good');
    toast('Saved '+from.name+' as your default device');
    if(shown)renderDef();
  }
  log(from.name+' connected','good');
  save();
}
/* Auto-connect: the default device started playing by itself, so bring the music to SpotiOS. */
function autoConnect(){
  if(!W.__spoServer||!cfg.auto||!cfg.def)return;
  var a=S.active;
  if(!a||isMine(a)||!S.playing||!isDef(a))return;
  if(now()<S.snooze||now()-S.pullAt<20000)return;
  var id=myId();if(!id)return;
  S.pullAt=now();S.localUntil=now()+10000;
  log('Took over from '+a.name+' (auto-connect)','good');
  api('/me/player',{method:'PUT',body:{device_ids:[id],play:true}}).then(function(){toast('Now playing on SpotiOS');})
    .catch(function(e){log('Couldn’t take over from '+a.name+': '+why(e),'bad');});
}
W.spoSrvTick=function(reason){if(W.__spoServer)poll(reason==='net');};
W.spoSrvCheck=function(){if(W.__spoServer){S.devAt=0;poll(true);}};

/* ---------- screen ---------- */
var shown=false,full=false,built=false,info={},tmr=null,lastNP='',np={pos:0,at:0,dur:0,playing:false};
function want(){return !!W.__spoServer&&!full&&location.hostname==='open.spotify.com';}
function build(){
  if(built)return;built=true;
  var st=document.createElement('style');st.id='ss-style';st.textContent=CSS;document.head.appendChild(st);
  var el=document.createElement('div');el.id='spoSrv';
  el.innerHTML=
   '<div class="ss-bg" id="ss-bg"></div><div class="ss-shade"></div>'+
   '<div class="ss-top"><div class="ss-badge" id="ss-badge"><i class="ss-dot" id="ss-dot"></i><b>SpotiOS Server</b><span id="ss-state">Starting…</span></div>'+
     '<button class="ss-ib" id="ss-full" aria-label="Open full Spotify">'+ic('grid')+'</button></div>'+
   '<div class="ss-scroll" id="ss-scroll">'+
    '<section class="ss-np idle" id="ss-np">'+
     '<div class="ss-art" id="ss-art"><img id="ss-cover" alt=""><div class="ss-idle"><i></i><i></i><i></i><div class="ss-core">'+ic('cast')+'</div></div></div>'+
     '<div class="ss-title" id="ss-title">Waiting for Spotify</div>'+
     '<div class="ss-artist" id="ss-artist">Pick “SpotiOS” in Spotify’s device list</div>'+
     '<div class="ss-prog"><div class="ss-bar"><i id="ss-fill"></i></div><div class="ss-times"><span id="ss-pos">0:00</span><span id="ss-dur">0:00</span></div></div>'+
     '<div class="ss-ctrl"><button id="ss-prev" aria-label="Previous">'+ic('prev')+'</button><button class="ss-pp" id="ss-play" aria-label="Play">'+ic('play')+'</button><button id="ss-next" aria-label="Next">'+ic('next')+'</button></div>'+
     '<button class="ss-on" id="ss-on">'+ic('speaker')+'<span id="ss-on-t">Nothing playing</span>'+ic('chev')+'</button>'+
    '</section>'+
    '<section class="ss-card" id="ss-conn"></section>'+
    '<section class="ss-card" id="ss-how"></section>'+
    '<section class="ss-card" id="ss-def"></section>'+
    '<section class="ss-card" id="ss-who"></section>'+
    '<section class="ss-card" id="ss-lib">'+
      '<div class="ss-h">'+ic('music')+'Play on SpotiOS</div>'+
      '<div class="ss-search" id="ss-search">'+ic('search')+'<input id="ss-q" type="search" enterkeyhint="search" placeholder="Search songs" autocomplete="off"><button id="ss-qx" aria-label="Clear">'+ic('x')+'</button></div>'+
      '<div class="ss-chips" id="ss-chips"><button data-t="recent" class="on">Recently played</button><button data-t="playlists">Playlists</button><button data-t="liked">Liked songs</button></div>'+
      '<div class="ss-list" id="ss-list"></div>'+
    '</section>'+
    '<section class="ss-card" id="ss-set"></section>'+
    '<section class="ss-card" id="ss-act"></section>'+
    '<div class="ss-foot">SpotiOS plays through this phone’s speaker or whatever it’s connected to. It can’t play while the phone is off or has no internet.</div>'+
   '</div>';
  document.body.appendChild(el);
  var sc=document.createElement('div');sc.id='ss-scrim';document.body.appendChild(sc);
  var sh=document.createElement('div');sh.id='ss-sheet';document.body.appendChild(sh);
  var to=document.createElement('div');to.id='ss-toast';document.body.appendChild(to);
  var pill=document.createElement('button');pill.id='ss-pill';pill.innerHTML='<i class="ss-dot ok" style="width:8px;height:8px"></i>Server';document.body.appendChild(pill);
  bind(el);
  sc.addEventListener('click',closeSheet);
  sh.addEventListener('click',onSheetClick);
  pill.addEventListener('click',function(){haptic();full=false;sync();});
}
function sync(){
  var on=want();
  if(on)build();
  document.documentElement.classList.toggle('ss-on',on);
  var el=byId('spoSrv');if(el)el.classList.toggle('ss-show',on);
  var pill=byId('ss-pill');if(pill)pill.classList.toggle('show',!!W.__spoServer&&full&&location.hostname==='open.spotify.com');
  if(on&&!shown){
    shown=true;
    info=status();
    renderAll();
    loadList();
    poll(true);
    if(!tmr)tmr=setInterval(loop,500);
  }else if(!on&&shown){
    shown=false;closeSheet();
  }
}
var n5=0;
function loop(){
  if(!shown){clearInterval(tmr);tmr=null;return;}
  if(document.hidden||W.__splBg)return;
  renderNP();
  if(++n5%10===0){info=status();renderConn();}
  if(n5%30===0){poll(false);renderLog();}
}
function status(){try{return JSON.parse(AndBridge.serverStatus()||'{}')||{};}catch(e){return {};}}
function renderAll(){renderNP();renderOn();renderConn();renderHow();renderDef();renderWho();renderSet();renderLog();}

function connState(){
  var c=W.__spoConn||{},n=now(),ws=c.ws,st=ws?ws.readyState:-1;
  if(navigator.onLine===false)return {k:'bad',t:'No internet',d:'SpotiOS reconnects by itself when the internet is back.'};
  if(!W.spotAuthToken&&n-(c.since||n)>20000)return {k:'warn',t:'Signing in…',d:'Waiting for Spotify to sign SpotiOS in.'};
  if(st===1&&c.lastMsg&&n-c.lastMsg<75000)return {k:'ok',t:'Ready',d:'Visible as “'+myName()+'” on all your Spotify devices.'};
  if(st===0||st===-1)return {k:'warn',t:'Connecting…',d:'Connecting to Spotify Connect.'};
  return {k:'warn',t:'Reconnecting…',d:'The connection dropped. SpotiOS is reconnecting.'};
}
function renderNP(){
  /* a song tapped here shows at once, until the player has moved on to it */
  var pd=np.pend;
  if(pd&&(now()>pd.until||(W.track||'')!==pd.old))pd=np.pend=null;
  var t=pd?pd.t:W.track||'',a=pd?pd.a:W.artist||'',cv=pd?pd.cv||W.cover||'':W.cover||'',pl=pd?true:!!W.playing;
  var pos=pd?0:+W.position||0,du=pd?0:+W.duration||0;
  var key=t+'|'+a+'|'+cv;
  var npEl=byId('ss-np'),art=byId('ss-art');if(!npEl)return;
  var has=!!t;
  npEl.classList.toggle('idle',!has);
  if(key!==lastNP){
    lastNP=key;
    var img=byId('ss-cover');
    if(cv&&img.getAttribute('src')!==cv)img.src=cv;
    art.classList.toggle('has',has&&!!cv);
    var bg=byId('ss-bg');bg.style.backgroundImage=cv?'url("'+cv.replace(/"/g,'')+'")':'';bg.classList.toggle('has',!!cv&&has);
    byId('ss-title').textContent=has?t:'Waiting for Spotify';
    byId('ss-artist').textContent=has?a:'Pick “'+myName()+'” in Spotify’s device list';
  }
  art.classList.toggle('paused',has&&!pl);
  var pb=byId('ss-play');var want_=pl?'pause':'play';
  if(pb.getAttribute('data-i')!==want_){pb.setAttribute('data-i',want_);pb.innerHTML=ic(want_);pb.setAttribute('aria-label',pl?'Pause':'Play');}
  if(pos!==np.pos||du!==np.dur){np.pos=pos;np.dur=du;np.at=now();}
  var p=np.pos+(pl&&np.at?now()-np.at:0);if(du&&p>du)p=du;
  byId('ss-fill').style.transform='scaleX('+(du?Math.min(1,p/du):0)+')';
  byId('ss-pos').textContent=fmt(p);byId('ss-dur').textContent=fmt(du);
  var st=connState(),dot=byId('ss-dot');dot.className='ss-dot '+st.k;byId('ss-state').textContent=st.t;
}
function renderOn(){
  var b=byId('ss-on');if(!b)return;var a=S.active,t;
  if(a&&isMine(a))t='Playing on SpotiOS (this phone)';
  else if(a)t=(S.playing?'Playing on ':'Paused on ')+(a.name||'another device');
  else t='Choose where to play';
  b.classList.toggle('mine',!!a&&isMine(a));
  byId('ss-on-t').textContent=t;
}
function renderConn(){
  var el=byId('ss-conn');if(!el)return;
  var st=connState(),c=W.__spoConn||{},since=info.since||c.since||now();
  var rc=(c.reconnects||0);
  el.innerHTML='<div class="ss-h">'+ic('cast')+'Connection</div>'+
   '<div class="ss-row"><div class="ss-ico '+(st.k==='ok'?'g':st.k==='bad'?'r':'y')+'">'+ic(st.k==='ok'?'check':'refresh')+'</div><div class="ss-tx"><b>'+esc(st.t==='Ready'?'Ready on Spotify Connect':st.t)+'</b><small>'+esc(st.d)+'</small></div></div>'+
   '<div class="ss-row"><div class="ss-ico">'+ic('history')+'</div><div class="ss-tx"><b>Running for '+esc(span(now()-since))+'</b><small>'+(rc?rc+' automatic reconnect'+(rc===1?'':'s')+' so far':'No dropped connections')+' · checked every 20 seconds</small></div></div>'+
   '<div class="ss-btns"><button class="ss-btn sm" data-a="reconnect">'+ic('refresh')+'Reconnect now</button><button class="ss-btn sm" data-a="rename">'+ic('settings')+'Rename “'+esc(myName())+'”</button></div>';
}
function renderHow(){
  var el=byId('ss-how');if(!el)return;
  var open=el.classList.contains('open')||(!cfg.connectedOnce&&!el.getAttribute('data-r'));
  el.setAttribute('data-r','1');
  el.classList.toggle('open',open);
  el.innerHTML='<div class="ss-h" data-a="fold">'+ic('help')+'How to connect'+ic('chev').replace('<svg','<svg class="ss-chev"')+'</div>'+
   '<div class="ss-fold"><ol class="ss-steps">'+
    '<li>Leave SpotiOS running<small>Lock the phone or swipe SpotiOS away, the server keeps going.</small></li>'+
    '<li>Open Spotify<small>On this phone, a computer, a tablet, anything signed in to the same Spotify account.</small></li>'+
    '<li>Tap the devices button<small>The speaker icon at the bottom of the Now Playing screen, or next to the volume on a computer.</small></li>'+
    '<li>Pick “'+esc(myName())+'”<small>The music plays here, and Spotify becomes the remote. The first device you do this from becomes your default device.</small></li>'+
   '</ol><div class="ss-note">Works on any network, Wi-Fi or mobile data, because Spotify Connect goes through Spotify’s servers.</div>'+
   '<div class="ss-btns"><button class="ss-btn pri" data-a="spotify">'+ic('spotify')+'Open Spotify</button></div></div>';
}
function renderDef(){
  var el=byId('ss-def');if(!el)return;
  var d=cfg.def,h='<div class="ss-h">'+ic('star')+'Default device</div>';
  if(d){
    var online=S.devices.some(function(x){return same(x,d);});
    h+='<div class="ss-row"><div class="ss-ico g">'+ic(devIc(d.type))+'</div><div class="ss-tx"><b>'+esc(d.name)+'</b><small>'+devLabel(d.type)+(online?' · online':' · not online right now')+'</small></div></div>'+
      '<div class="ss-row"><div class="ss-tx"><b>Auto-connect</b><small>When '+esc(d.name)+' starts playing by itself, SpotiOS takes the music over. If you move the music off SpotiOS, it waits 30 minutes before doing that again.</small></div><button class="ss-sw'+(cfg.auto?' on':'')+'" data-a="auto" aria-label="Auto-connect"></button></div>'+
      '<div class="ss-btns"><button class="ss-btn sm" data-a="pickdef">Change</button><button class="ss-btn sm dng" data-a="forgetdef">Forget</button></div>';
  }else{
    h+='<div class="ss-msg" style="text-align:left;padding:4px 0 0">No default device yet. The first device that plays on SpotiOS becomes the default, so SpotiOS can connect to it on its own next time.'+
      '<div><button class="ss-btn sm" data-a="pickdef">Choose a device</button></div></div>';
  }
  el.innerHTML=h;
}
function devRows(){
  var map={},out=[];
  S.devices.forEach(function(d){if(!isMine(d)&&d.id){map[d.id]=1;out.push({id:d.id,name:d.name,type:d.type,on:true});}});
  Object.keys(cfg.devs).forEach(function(k){if(!map[k]){var e=cfg.devs[k];out.push({id:k,name:e.name,type:e.type,on:false,seen:e.seen});}});
  out.sort(function(a,b){return (isDef(b)-isDef(a))||(b.on-a.on)||((b.seen||0)-(a.seen||0));});
  return out;
}
function renderWho(){
  var el=byId('ss-who');if(!el)return;
  var rows=devRows(),h='<div class="ss-h">'+ic('shield')+'Who can play on SpotiOS</div>'+
   '<div class="ss-seg"><button data-a="pol-all"'+(cfg.policy==='all'?' class="on"':'')+'>Every device</button><button data-a="pol-list"'+(cfg.policy==='list'?' class="on"':'')+'>Only allowed</button></div>';
  if(!rows.length)h+='<div class="ss-note" style="padding:8px 2px">Devices on your Spotify account show up here once they’re online.</div>';
  rows.slice(0,24).forEach(function(d){
    var def=isDef(d),ok=allowed(d);
    var sub=devLabel(d.type)+(d.on?' · online':(d.seen?' · seen '+ago(d.seen):''));
    h+='<div class="ss-row"><div class="ss-ico'+(ok?'':' r')+'">'+ic(ok?devIc(d.type):'block')+'</div><div class="ss-tx"><b>'+esc(d.name||'Device')+
      (def?'<span class="ss-tag">DEFAULT</span>':(ok?'':'<span class="ss-tag b">BLOCKED</span>'))+'</b><small>'+esc(sub)+'</small></div>'+
      '<button class="ss-sw'+(ok?' on':'')+(def?' dis':'')+'" data-a="allow" data-id="'+esc(d.id)+'" aria-label="Allow '+esc(d.name)+'"></button></div>';
  });
  h+='<div class="ss-note">Spotify can’t stop another device from picking SpotiOS, so when a blocked device sends music here, SpotiOS sends it straight back. Your default device is always allowed.</div>';
  el.innerHTML=h;
}
function renderDevs(){renderWho();renderDef();}
function renderSet(){
  var el=byId('ss-set');if(!el)return;
  var ws=!!info.withSpotify,na=!!info.notifAccess;
  var h='<div class="ss-h">'+ic('settings')+'Server</div>'+
   '<div class="ss-row"><div class="ss-ico g">'+ic('spotify')+'</div><div class="ss-tx"><b>Start with Spotify</b><small>'+
     (ws&&!na?'Needs notification access so SpotiOS can notice Spotify. <u data-a="notif">Allow it</u>.':'When Spotify starts playing on this phone, SpotiOS starts too'+(cfg.def&&cfg.auto?' and takes the music over':'')+'.')+
     (info.spotifyInstalled===false?' Spotify isn’t installed on this phone.':'')+'</small></div><button class="ss-sw'+(ws?' on':'')+'" data-a="withsp" aria-label="Start with Spotify"></button></div>'+
   '<div class="ss-row"><div class="ss-ico '+(info.battery?'g':'y')+'">'+ic('battery')+'</div><div class="ss-tx"><b>Battery: '+(info.battery?'unrestricted':'restricted')+'</b><small>'+
     (info.battery?'Android lets SpotiOS run in the background.':'Android may pause SpotiOS in the background and drop the connection.')+'</small></div>'+
     (info.battery?'':'<button class="ss-btn sm pri" data-a="battery">Fix</button>')+'</div>'+
   '<div class="ss-row"><div class="ss-ico g">'+ic('swipe')+'</div><div class="ss-tx"><b>Keeps running when swiped away</b><small>Swiping SpotiOS out of recent apps doesn’t stop the server. Use Turn off below to stop it.</small></div></div>'+
   '<div class="ss-btns"><button class="ss-btn sm" data-a="full">'+ic('grid')+'Open full Spotify</button><button class="ss-btn sm" data-a="settings">'+ic('settings')+'SpotiOS settings</button><button class="ss-btn sm dng" data-a="off">'+ic('power')+'Turn off Server Mode</button></div>';
  el.innerHTML=h;
}
function renderLog(){
  var el=byId('ss-act');if(!el)return;
  var h='<div class="ss-h">'+ic('history')+'Activity</div>';
  if(!cfg.log.length)h+='<div class="ss-note">Connections, take-overs and blocked devices show up here.</div>';
  else h+='<div class="ss-log">'+cfg.log.slice(0,8).map(function(x){return '<div><i class="'+esc(x.k)+'"></i><span>'+esc(x.m)+'</span><time>'+ago(x.t)+'</time></div>';}).join('')+'</div>';
  el.innerHTML=h;
}

/* ---------- play something ---------- */
var lib={tab:'recent',q:'',items:[],req:0,qt:null,uid:''};
function itemArt(it){var im=it&&((it.album&&it.album.images)||it.images);if(!im||!im.length)return '';var x=im[1]||im[0];return x&&x.url||'';}
function trackItem(t){return {kind:'track',name:t.name,sub:(t.artists||[]).map(function(a){return a.name;}).join(', '),art:itemArt(t),uri:t.uri};}
function listMsg(h){var l=byId('ss-list');if(l)l.innerHTML='<div class="ss-msg">'+h+'</div>';}
function loadList(){
  var id=++lib.req,p;
  listMsg('Loading…');
  if(lib.q){
    p=api('/search?type=track&limit=25&q='+encodeURIComponent(lib.q)).then(function(j){return ((j&&j.tracks&&j.tracks.items)||[]).filter(Boolean).map(trackItem);});
  }else if(lib.tab==='playlists'){
    p=api('/me/playlists?limit=50').then(function(j){
      return [{kind:'liked',name:'Liked Songs',sub:'Your liked songs',art:'',uri:''}].concat(((j&&j.items)||[]).filter(Boolean).map(function(pl){
        var im=pl.images||[];return {kind:'ctx',name:pl.name,sub:'Playlist'+(pl.owner&&pl.owner.display_name?' · '+pl.owner.display_name:''),art:im.length?(im[im.length>1?1:0].url):'',uri:pl.uri};
      }));
    });
  }else if(lib.tab==='liked'){
    p=api('/me/tracks?limit=50').then(function(j){return ((j&&j.items)||[]).map(function(x){return x&&x.track;}).filter(Boolean).map(trackItem);});
  }else{
    p=api('/me/player/recently-played?limit=40').then(function(j){
      var seen={},out=[];
      ((j&&j.items)||[]).forEach(function(x){var t=x&&x.track;if(t&&t.uri&&!seen[t.uri]){seen[t.uri]=1;out.push(trackItem(t));}});
      return out;
    });
  }
  p.then(function(items){if(id!==lib.req)return;lib.items=items;renderList();})
   .catch(function(e){if(id!==lib.req)return;listMsg(esc(why(e))+'<br><button class="ss-btn sm" data-a="relist">Try again</button>');});
}
function renderList(){
  var l=byId('ss-list');if(!l)return;
  if(!lib.items.length){listMsg(lib.q?'No songs found.':lib.tab==='recent'?'Nothing played yet.':'Nothing here yet.');return;}
  var cur=W.track||'';
  l.innerHTML=lib.items.map(function(it,i){
    var art=it.kind==='liked'?'<div class="ss-ph lk">'+ic('heart')+'</div>':(it.art?'<img loading="lazy" src="'+esc(it.art)+'" alt="">':'<div class="ss-ph">'+ic('music')+'</div>');
    return '<div class="ss-it'+(it.kind==='track'&&cur&&it.name===cur?' now':'')+'" data-i="'+i+'" role="button">'+art+'<div class="ss-tx"><b>'+esc(it.name)+'</b><small>'+esc(it.sub)+'</small></div><span class="ss-go">'+ic('play')+'</span></div>';
  }).join('');
}
function playItem(i,row){
  var it=lib.items[i];if(!it)return;
  np.pend=null;
  if(it.kind==='liked'){
    var go=function(uid){playOnMe({context_uri:'spotify:user:'+uid+':collection'},row,it.name);};
    if(lib.uid)go(lib.uid);else api('/me').then(function(me){lib.uid=me&&me.id||'';if(lib.uid)go(lib.uid);else rowFail(row,new Error('x'));}).catch(function(e){rowFail(row,e);});
    return;
  }
  if(it.kind==='ctx'){playOnMe({context_uri:it.uri},row,it.name);return;}
  if(it.name&&it.name!==(W.track||'')){
    np.pend={t:it.name,a:it.sub||'',cv:it.art||'',old:W.track||'',until:now()+8000};
    renderNP();
  }
  /* a song: play it and keep going down the list, like tapping a song in the app */
  var uris=[];
  if(lib.q)uris=[it.uri];
  else for(var j=i;j<lib.items.length&&uris.length<50;j++)if(lib.items[j].kind==='track')uris.push(lib.items[j].uri);
  playOnMe({uris:uris},row,it.name);
}
function rowFail(row,e){if(row)row.classList.remove('busy');toast(why(e));}
function playOnMe(body,row,name){
  if(row){var b=document.querySelectorAll('#ss-list .ss-it.busy');for(var k=0;k<b.length;k++)b[k].classList.remove('busy');row.classList.add('busy');var g=row.querySelector('.ss-go');if(g)g.innerHTML=ic('refresh');}
  haptic();
  S.localUntil=now()+10000;
  var id=myId(),q=id?'?device_id='+encodeURIComponent(id):'';
  function done(){
    if(row){row.classList.remove('busy');var g=row.querySelector('.ss-go');if(g)g.innerHTML=ic('play');var all=document.querySelectorAll('#ss-list .ss-it.now');for(var k=0;k<all.length;k++)all[k].classList.remove('now');row.classList.add('now');}
    if(name&&!np.pend){byId('ss-title').textContent=name;}
    if(!early)toTop();
  }
  function toTop(){var sc=byId('ss-scroll');if(sc)sc.scrollTo({top:0,behavior:'smooth'});}
  var early=!!np.pend;if(early)toTop();
  api('/me/player/play'+q,{method:'PUT',body:body}).then(done).catch(function(e){
    if(id&&/http404|http502/.test(String(e&&e.message))){
      return api('/me/player',{method:'PUT',body:{device_ids:[id],play:false}})
        .then(function(){return new Promise(function(r){setTimeout(r,700);});})
        .then(function(){return api('/me/player/play'+q,{method:'PUT',body:body});}).then(done);
    }
    throw e;
  }).catch(function(e){np.pend=null;lastNP='';renderNP();if(row){row.classList.remove('busy');var g=row.querySelector('.ss-go');if(g)g.innerHTML=ic('play');}toast(why(e));});
}

/* ---------- sheet (play on / default device) ---------- */
var sheetKind='';
function openSheet(kind){
  sheetKind=kind;
  var sh=byId('ss-sheet');
  var t=kind==='def'?'Default device':'Play on';
  var p=kind==='def'?'SpotiOS connects to this device on its own when it starts playing.':'Move the music to another device, or bring it to SpotiOS.';
  sh.innerHTML='<div class="ss-grab"></div><h4>'+t+'</h4><p>'+p+'</p><div class="ss-sb" id="ss-sb"><div class="ss-msg">Looking for devices…</div></div>';
  sh.classList.add('open');byId('ss-scrim').classList.add('open');
  api('/me/player/devices').then(function(j){
    S.devices=((j&&j.devices)||[]).map(function(d){return {id:d.id,name:d.name,type:d.type,active:d.is_active};});
    S.devices.forEach(known);save();fillSheet();
  }).catch(function(){fillSheet();});
}
function fillSheet(){
  var sb=byId('ss-sb');if(!sb||!sheetKind)return;
  var h='';
  if(sheetKind==='def'){
    var rows=devRows();
    h+='<div class="ss-row'+(cfg.def?'':' cur')+'" data-s="none"><div class="ss-ico">'+ic('x')+'</div><div class="ss-tx"><b>No default device</b><small>Don’t connect to anything on its own</small></div>'+ic('check').replace('<svg','<svg class="ss-chk"')+'</div>';
    rows.forEach(function(d){
      h+='<div class="ss-row'+(isDef(d)?' cur':'')+'" data-s="'+esc(d.id)+'"><div class="ss-ico">'+ic(devIc(d.type))+'</div><div class="ss-tx"><b>'+esc(d.name)+'</b><small>'+devLabel(d.type)+(d.on?' · online':'')+'</small></div>'+ic('check').replace('<svg','<svg class="ss-chk"')+'</div>';
    });
  }else{
    var a=S.active,mine={id:myId(),name:myName(),type:'Smartphone'};
    var list=[mine].concat(S.devices.filter(function(d){return !isMine(d);}));
    list.forEach(function(d){
      var cur=a&&(isMine(d)?isMine(a):a.id===d.id);
      h+='<div class="ss-row'+(cur?' cur':'')+'" data-p="'+esc(isMine(d)?'@me':d.id)+'"><div class="ss-ico'+(isMine(d)?' g':'')+'">'+ic(isMine(d)?'phone':devIc(d.type))+'</div><div class="ss-tx"><b>'+esc(isMine(d)?'SpotiOS (this phone)':d.name)+'</b><small>'+(isMine(d)?'Plays here':devLabel(d.type))+'</small></div>'+ic('check').replace('<svg','<svg class="ss-chk"')+'</div>';
    });
    if(list.length<2)h+='<div class="ss-note" style="padding:10px 2px">Other devices show up here when Spotify is open on them.</div>';
  }
  sb.innerHTML=h;
}
function closeSheet(){sheetKind='';var sh=byId('ss-sheet');if(sh)sh.classList.remove('open');var s=byId('ss-scrim');if(s)s.classList.remove('open');}
function onSheetClick(e){
  var r=e.target.closest&&e.target.closest('.ss-row');if(!r)return;
  haptic();
  if(sheetKind==='def'){
    var sid=r.getAttribute('data-s');
    if(sid==='none'){cfg.def=null;log('Default device cleared','');}
    else{var e2=cfg.devs[sid];if(e2){cfg.def={id:sid,name:e2.name,type:e2.type};e2.allow=true;log(e2.name+' is now your default device','good');}}
    save();closeSheet();renderDef();renderWho();renderSet();return;
  }
  var pid=r.getAttribute('data-p'),id=pid==='@me'?myId():pid;
  if(!id){toast('SpotiOS isn’t ready yet');return;}
  S.localUntil=now()+10000;
  if(pid!=='@me'&&S.active&&isMine(S.active))S.snooze=now()+30*60000;
  r.classList.add('cur');
  api('/me/player',{method:'PUT',body:{device_ids:[id],play:true}}).then(function(){closeSheet();setTimeout(function(){poll(true);},1200);})
    .catch(function(err){toast(why(err));});
}
var toastT=null;
function toast(m){var t=byId('ss-toast');if(!t)return;t.textContent=m;t.classList.add('show');clearTimeout(toastT);toastT=setTimeout(function(){t.classList.remove('show');},2600);}

/* ---------- input ---------- */
function bind(el){
  el.addEventListener('click',function(e){
    var t=e.target;
    var b=t.closest&&t.closest('[data-a],#ss-play,#ss-prev,#ss-next,#ss-on,#ss-full,.ss-it,#ss-chips button,#ss-qx');
    if(!b)return;
    if(b.id==='ss-play'){haptic();try{W.actPlayPause();}catch(x){}setTimeout(renderNP,250);return;}
    if(b.id==='ss-prev'){haptic();try{W.actSkipBack();}catch(x){}return;}
    if(b.id==='ss-next'){haptic();try{W.actSkipForward();}catch(x){}return;}
    if(b.id==='ss-on'){haptic();openSheet('play');return;}
    if(b.id==='ss-full'){haptic();full=true;sync();return;}
    if(b.id==='ss-qx'){var q=byId('ss-q');q.value='';lib.q='';byId('ss-search').classList.remove('has');loadList();return;}
    if(b.classList.contains('ss-it')){if(!b.classList.contains('busy'))playItem(+b.getAttribute('data-i'),b);return;}
    if(b.parentNode&&b.parentNode.id==='ss-chips'){
      haptic();var c=b.parentNode.children;for(var i=0;i<c.length;i++)c[i].classList.toggle('on',c[i]===b);
      lib.tab=b.getAttribute('data-t');lib.q='';var qi=byId('ss-q');if(qi)qi.value='';byId('ss-search').classList.remove('has');loadList();return;
    }
    var a=b.getAttribute('data-a');
    if(a)action(a,b);
  });
  var q=byId('ss-q');
  q.addEventListener('input',function(){
    var v=q.value.trim();byId('ss-search').classList.toggle('has',!!q.value);
    clearTimeout(lib.qt);lib.qt=setTimeout(function(){lib.q=v;loadList();},350);
  });
  q.addEventListener('keydown',function(e){if(e.key==='Enter'){q.blur();clearTimeout(lib.qt);lib.q=q.value.trim();loadList();}});
}
function action(a,b){
  haptic();
  if(a==='fold'){var c=byId('ss-how');c.classList.toggle('open');return;}
  if(a==='spotify'){try{AndBridge.openSpotifyApp();}catch(e){location.href='spotify:';}return;}
  if(a==='reconnect'){
    var c2=W.__spoConn,ws=c2&&c2.ws;
    if(ws&&ws.readyState===1){try{ws.close(4002,'user');}catch(e){}c2.forced++;toast('Reconnecting to Spotify…');}
    else{toast('Reloading the player…');setTimeout(function(){location.reload();},300);}
    log('Reconnected by hand','');setTimeout(renderConn,500);return;
  }
  if(a==='rename'||a==='settings'){try{AndBridge.openSettings();}catch(e){}return;}
  if(a==='auto'){cfg.auto=!cfg.auto;save();b.classList.toggle('on',cfg.auto);if(cfg.auto)S.snooze=0;renderSet();return;}
  if(a==='pickdef'){openSheet('def');return;}
  if(a==='forgetdef'){if(cfg.def)log('Forgot '+cfg.def.name+' as the default device','');cfg.def=null;save();renderDef();renderWho();return;}
  if(a==='pol-all'||a==='pol-list'){cfg.policy=a==='pol-list'?'list':'all';save();renderWho();
    toast(cfg.policy==='list'?'Only allowed devices can play here':'Every device on your account can play here');return;}
  if(a==='allow'){
    var id=b.getAttribute('data-id'),e=cfg.devs[id];if(!e)return;
    if(cfg.def&&cfg.def.id===id){toast('Your default device is always allowed');return;}
    var nowOk=!allowed({id:id,name:e.name,type:e.type});
    e.allow=nowOk;save();renderWho();
    log((nowOk?'Allowed ':'Blocked ')+e.name,nowOk?'good':'bad');
    /* blocking the device that is sending music right now sends it back */
    if(!nowOk&&S.active&&isMine(S.active)&&S.playing)toast(e.name+' is blocked from now on');
    return;
  }
  if(a==='withsp'){var on=!info.withSpotify;try{AndBridge.setStartWithSpotify(on);}catch(e){}info.withSpotify=on;renderSet();
    if(on)toast(info.notifAccess?'SpotiOS starts when Spotify plays':'Allow notification access so SpotiOS can notice Spotify');return;}
  if(a==='notif'){try{AndBridge.openNotificationAccess();}catch(e){}return;}
  if(a==='battery'){try{AndBridge.openBatterySettings();}catch(e){}return;}
  if(a==='full'){full=true;sync();return;}
  if(a==='off'){try{AndBridge.setServerMode(false);}catch(e){}W.spoSetServer(false);toast('Server Mode is off');return;}
  if(a==='relist'){loadList();return;}
}

/* refresh battery and permission rows when coming back from Android settings */
document.addEventListener('visibilitychange',function(){if(!document.hidden&&shown){info=status();renderSet();renderConn();}});

/* ---------- back button: sheet, then full Spotify back to the Server screen ---------- */
var prevBack=W.spoBack;
W.spoBack=function(){
  if(sheetKind){closeSheet();return true;}
  if(want()){try{AndBridge.moveToBack();return true;}catch(e){return false;}}
  if(W.__spoServer&&full){
    if(prevBack&&prevBack())return true;
    full=false;sync();return true;
  }
  return prevBack?prevBack():false;
};

W.spoSetServer=function(on){
  W.__spoServer=!!on;full=false;
  if(on){log('Server Mode on','good');S.devAt=0;}
  sync();
  if(!on){var p=byId('ss-pill');if(p)p.classList.remove('show');}
};
W.spoServerOpen=function(){full=false;sync();};
W.spoServerShown=function(){return shown;};
sync();
})();
"""
}
