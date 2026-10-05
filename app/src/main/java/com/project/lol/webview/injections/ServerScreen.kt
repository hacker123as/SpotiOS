package com.project.lol.webview.injections

import org.json.JSONObject

/**
 * The SpotiOS Server screen, shown instead of the web player in Server Mode.
 *
 * Shows what's playing and on which device, whether SpotiOS is ready on Spotify Connect,
 * how to connect to it, and lets you play something on SpotiOS (recent songs, playlists,
 * liked songs, search). It also runs Server Mode's device rules:
 *  - take-over: the default device (this phone's own Spotify app, found by its name, or one you
 *    pick) never keeps the music: as soon as it plays, SpotiOS moves the music to itself, every
 *    time. If the Spotify session drops while SpotiOS was playing, it takes it back too. Other
 *    devices are left alone;
 *  - when the Spotify app on this phone is closed, SpotiOS pauses;
 *  - who may play here: everyone on the account, or only allowed devices. Spotify has no
 *    way to refuse a connection, so music sent from a blocked device is sent straight back.
 *
 * Connect state comes from ConnectKeepAlive (window.__spoConn), and everything here talks to
 * Spotify the way the web player itself does (Connect commands through window.spoCS, lists and
 * search through Spotify's GraphQL API), because the public Web API rate-limits the web player
 * and answered "too many requests" all the time. The Web API is only a fallback. Lists and
 * play buttons retry by themselves when Spotify is busy, and lists are kept between starts.
 * Settings live in localStorage ("spoSrv").
 */
object ServerScreen {

    // The script is kept in two constants: one string constant in a class file can't pass 64 KB.
    fun content(on: Boolean): String =
        "window.__spoServer=$on;\n" + listOf(JS, JS2).joinToString("").replace("__SRV_CSS__", JSONObject.quote(CSS))

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
.ss-ib.stop{color:#ff6961}
#spoSrv.ss-stopping .ss-scroll,#spoSrv.ss-stopping .ss-top{opacity:.35;pointer-events:none;transition:opacity .4s}
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
.ss-ctrl{display:flex;align-items:center;justify-content:center;gap:clamp(8px,4.2vw,26px);margin:10px 0 6px}
.ss-ctrl button{width:52px;height:52px;display:flex;align-items:center;justify-content:center;border-radius:50%;transition:transform .3s var(--ss-spring),opacity .2s}
.ss-ctrl button svg{width:30px;height:30px}
.ss-ctrl button:active{transform:scale(.84)}
.ss-ctrl .ss-pp{width:70px;height:70px;background:#fff;color:#000;box-shadow:0 10px 30px rgba(0,0,0,.35)}
.ss-ctrl .ss-pp svg{width:32px;height:32px}
.ss-ctrl .ss-sm{width:44px;height:44px;color:var(--ss-ink2)}
.ss-ctrl .ss-sm svg{width:23px;height:23px}
.ss-ctrl .ss-sm.on{color:var(--ss-acc)}
.ss-outs{display:flex;flex-wrap:wrap;justify-content:center;gap:8px;margin-top:12px;max-width:100%}
.ss-outs .ss-on{margin-top:0}
.ss-on i{display:flex;font-style:normal}
.ss-net{margin:6px 0 4px;padding:15px 16px;border-radius:22px;background:rgba(255,159,10,.12);border:1px solid rgba(255,159,10,.38)}
.ss-net[hidden]{display:none}
.ss-net b{display:flex;align-items:center;gap:8px;font-size:15.5px;font-weight:750;color:#ffb340}
.ss-net b svg{width:20px;height:20px}
.ss-net p{margin:6px 0 0;font-size:13.5px;line-height:1.42;color:var(--ss-ink2)}
.ss-net .ss-btns{margin-top:10px}
#ss-lib>.ss-h{cursor:pointer;margin-bottom:0}
#ss-lib.open>.ss-h{margin-bottom:10px}
#ss-lib:not(.open)>:not(.ss-h){display:none}
.ss-sl{padding:12px 0 10px}
.ss-sl+.ss-sl,.ss-row+.ss-sl{border-top:1px solid var(--ss-sep)}
.ss-sl label{display:flex;justify-content:space-between;align-items:baseline;font-size:14.5px;font-weight:600;margin-bottom:10px}
.ss-sl label span{color:var(--ss-ink2);font-weight:500;font-size:13px;font-variant-numeric:tabular-nums}
.ss-sl input{-webkit-appearance:none;appearance:none;display:block;width:100%;height:6px;margin:0;border-radius:3px;outline:0;
  background:linear-gradient(90deg,var(--ss-acc) var(--p,0%),rgba(255,255,255,.16) var(--p,0%))}
.ss-sl input::-webkit-slider-thumb{-webkit-appearance:none;appearance:none;width:26px;height:26px;border-radius:50%;background:#fff;box-shadow:0 3px 10px rgba(0,0,0,.35)}
.ss-sl input:disabled{opacity:.35}
.ss-sl.na{opacity:.45}
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
.ss-it img.rnd{border-radius:50%}
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
.ss-msg small{display:block;margin-top:4px;font-size:12.5px;color:var(--ss-ink3)}
.ss-spin{display:flex;justify-content:center;margin-bottom:8px;color:var(--ss-ink3)}
.ss-spin svg{width:22px;height:22px;animation:ssSpin 1s linear infinite}
.ss-link{margin-top:8px;font-size:13px;font-weight:600;color:var(--ss-acc)!important;padding:6px 10px!important}
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
#ss-sheet .ss-row.dng .ss-tx b{color:#ff6961}
#ss-sheet .ss-row:active{opacity:.6}
#ss-toast{position:fixed;left:50%;top:calc(env(safe-area-inset-top,0px) + 70px);z-index:2147483647;transform:translate(-50%,-14px);opacity:0;pointer-events:none;
  padding:10px 16px;border-radius:999px;background:rgba(40,40,44,.94);border:1px solid var(--ss-rim);font:600 13.5px/1.3 -apple-system,Roboto,sans-serif;color:#fff;
  box-shadow:0 10px 30px rgba(0,0,0,.4);transition:opacity .25s,transform .4s cubic-bezier(.34,1.56,.64,1);max-width:86vw;text-align:center}
#ss-toast.show{opacity:1;transform:translate(-50%,0)}
#ss-back{position:fixed;top:calc(env(safe-area-inset-top,0px) + 10px);right:12px;z-index:2147483645;display:none;align-items:center;gap:8px;height:36px;padding:0 15px 0 12px;
  border-radius:999px;background:rgba(20,20,22,.8);border:1px solid rgba(30,215,96,.4);-webkit-backdrop-filter:blur(24px) saturate(1.6);backdrop-filter:blur(24px) saturate(1.6);
  font:700 13.5px/1 -apple-system,"SF Pro Text",Roboto,sans-serif;color:#fff;box-shadow:0 8px 24px rgba(0,0,0,.4);transition:transform .3s cubic-bezier(.34,1.56,.64,1)}
#ss-back:active{transform:scale(.92)}
#ss-back.show{display:flex}
html.ss-on #ss-back{display:none}
#ss-back .ss-dot{width:8px;height:8px}
#ss-tab{position:relative}
#ss-tab .ss-tab-dot{position:absolute;top:9px;left:calc(50% + 6px);width:7px;height:7px;border-radius:50%;background:var(--ss-acc);box-shadow:0 0 0 2px rgba(24,24,30,.9)}
#ss-tab svg{fill:none}
@media (min-width:700px) and (orientation:landscape){
  .ss-scroll{display:grid;grid-template-columns:minmax(300px,1fr) minmax(320px,1.1fr);column-gap:22px;align-items:start}
  .ss-np{grid-row:1/span 9;position:sticky;top:0}
  .ss-net{grid-column:2}
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
  next:'<svg viewBox="0 0 24 24"><path fill="currentColor" d="M18 5a1 1 0 0 1 1 1v12a1 1 0 1 1-2 0V6a1 1 0 0 1 1-1zM4.6 5.6a1 1 0 0 1 1 .1l8 5.5a1 1 0 0 1 0 1.6l-8 5.5A1 1 0 0 1 4 17.5v-11a1 1 0 0 1 .6-.9z"/></svg>',
  more:'<svg viewBox="0 0 24 24"><path fill="currentColor" d="M5 10.25a1.75 1.75 0 1 1 0 3.5a1.75 1.75 0 0 1 0-3.5zm7 0a1.75 1.75 0 1 1 0 3.5a1.75 1.75 0 0 1 0-3.5zm7 0a1.75 1.75 0 1 1 0 3.5a1.75 1.75 0 0 1 0-3.5z"/></svg>'
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
  refresh:'M20 11A8.1 8.1 0 0 0 4.5 9M4 5v4h4M4 13a8.1 8.1 0 0 0 15.5 2M20 19v-4h-4',
  star:'M12 17.75l-6.17 3.24 1.18-6.87-5-4.86 6.9-1L12 2l3.09 6.26 6.9 1-5 4.86 1.18 6.87z',
  shield:'M12 3a12 12 0 0 0 8.5 3A12 12 0 0 1 12 21 12 12 0 0 1 3.5 6 12 12 0 0 0 12 3M9 12l2 2 4-4',
  help:'M12 21a9 9 0 1 0 0-18a9 9 0 0 0 0 18zM12 17v.01M12 13.5a1.5 1.5 0 0 1 1-1.5a2.6 2.6 0 1 0-3-4',
  swipe:'M4 12h12M12 6l6 6-6 6M20 4v16',spotify:'M12 21a9 9 0 1 0 0-18a9 9 0 0 0 0 18zM8 11.5c2.5-1 5.5-.8 8 .5M8.5 14.5c2-.7 4.3-.6 6.3.4M7.5 8.6c3-1.1 6.8-.9 9.8.6',
  heart:'M19.5 12.57L12 20l-7.5-7.43A5 5 0 1 1 12 6.01a5 5 0 1 1 7.5 6.57',music:'M6 20a3 3 0 1 0 0-6a3 3 0 0 0 0 6zM16 18a3 3 0 1 0 0-6a3 3 0 0 0 0 6zM9 17V5l10-2v12',
  settings:'M10.3 4.3c.4-1.8 3-1.8 3.4 0a1.7 1.7 0 0 0 2.6 1.1c1.5-.9 3.3.8 2.4 2.4a1.7 1.7 0 0 0 1 2.5c1.8.5 1.8 3 0 3.5a1.7 1.7 0 0 0-1 2.6c.9 1.5-.9 3.3-2.4 2.3a1.7 1.7 0 0 0-2.6 1.1c-.4 1.8-3 1.8-3.4 0a1.7 1.7 0 0 0-2.6-1.1c-1.5.9-3.3-.8-2.4-2.3a1.7 1.7 0 0 0-1-2.6c-1.8-.4-1.8-3 0-3.4a1.7 1.7 0 0 0 1-2.6c-.9-1.6.9-3.3 2.4-2.4a1.7 1.7 0 0 0 2.6-1.1zM12 15a3 3 0 1 0 0-6a3 3 0 0 0 0 6z',
  history:'M12 8v4l2 2M3.05 11a9 9 0 1 1 .5 4M3 20v-5h5',list:'M9 6h11M9 12h11M9 18h11M5 6v.01M5 12v.01M5 18v.01',
  block:'M12 21a9 9 0 1 0 0-18a9 9 0 0 0 0 18zM5.7 5.7l12.6 12.6',
  takeover:'M4 12a8 8 0 0 1 13.66-5.66L20 8.7M20 4v4.7h-4.7M20 12a8 8 0 0 1-13.66 5.66L4 15.3M4 20v-4.7h4.7',
  download:'M12 4v11M7 10l5 5 5-5M5 20h14',sliders:'M4 7h9M17 7h3M4 17h3M11 17h9M15 5v4M9 15v4',
  output:'M11 5L6 9H3v6h3l5 4zM15.5 8.5a5 5 0 0 1 0 7M18.5 5.5a9 9 0 0 1 0 13',bt:'M7 7l10 10-5 5V2l5 5L7 17',
  headphones:'M4 15v-3a8 8 0 0 1 16 0v3M4 15a2 2 0 0 1 2-2h1v7H6a2 2 0 0 1-2-2zM20 15a2 2 0 0 0-2-2h-1v7h1a2 2 0 0 0 2-2z',
  warn:'M12 9v4M12 17v.01M10.3 3.9L2.4 18a2 2 0 0 0 1.7 3h15.8a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z',
  pausec:'M12 21a9 9 0 1 0 0-18a9 9 0 0 0 0 18zM10 9v6M14 9v6'
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
/* 3.0: take-over is for the default device only, and always on unless turned off */
if(cfg.v!==3){cfg.v=3;cfg.auto=true;delete cfg.from;if(cfg.def&&cfg.def.auto===undefined)cfg.def.auto=true;}
if(cfg.pauseClose===undefined)cfg.pauseClose=true;
if(cfg.policy!=='list')cfg.policy='all';
cfg.devs=cfg.devs&&typeof cfg.devs==='object'?cfg.devs:{};
cfg.log=Array.isArray(cfg.log)?cfg.log:[];
/* 2.10 saved SpotiOS itself as a device (it couldn't tell its own id apart): drop it. */
(function(){for(var k in cfg.devs){var e=cfg.devs[k];if(e&&e.name===(W.__spoDeviceName||'SpotiOS')&&normType(e.type)==='computer')delete cfg.devs[k];}
  if(cfg.def&&cfg.def.name===(W.__spoDeviceName||'SpotiOS')&&normType(cfg.def.type)==='computer')cfg.def=null;})();
function save(){try{localStorage.setItem(LS,JSON.stringify(cfg));}catch(e){}}
function log(m,k){cfg.log.unshift({t:now(),m:m,k:k||''});if(cfg.log.length>30)cfg.log.length=30;save();if(shown)renderLog();}

/* ---------- talking to Spotify ---------- */
function err(code,status){var e=new Error(code);e.status=status||0;return e;}
function why(e){var m=String(e&&e.message||'');
  if(m==='offline')return 'You’re offline.';
  if(m==='signin')return 'Spotify is still signing in.';
  if(m==='noid')return 'SpotiOS is still joining Spotify Connect. Try again in a few seconds.';
  if(m==='http429'||m==='busy')return 'Spotify is busy right now. Try again in a minute.';
  if(m==='http403')return 'Spotify didn’t allow that. Playing on other devices needs Premium.';
  if(m==='http404'||m==='http410')return 'Spotify couldn’t find that device. Tap Reconnect, then try again.';
  if(m==='nohash')return 'Spotify changed something on its side. Try again later.';
  return 'Couldn’t reach Spotify. Check the connection and try again.';}
function quiet(e){var m=String(e&&e.message||'');return m==='offline'||m==='signin';}
/* Worth trying again by itself: Spotify busy, still starting, or a network hiccup. */
function retryable(e){var m=String(e&&e.message||''),c=m.indexOf('http')===0?+m.slice(4):0;return !((c>=400&&c<=404)||c===410)&&m!=='offline'&&m!=='nohash';}
function retryWait(e,n){
  var m=String(e&&e.message||''),w;
  if(m==='signin'||m==='noid')w=1500;
  else if(m==='offline')w=5000;
  else w=[2000,4000,8000,15000,30000][n]||60000;
  if(e&&e.retryAfter)w=Math.max(w,Math.min(60,e.retryAfter)*1000);
  return w;
}
function later(ms){return new Promise(function(r){setTimeout(r,ms);});}
/* Runs fn again (up to n times) while Spotify is busy or still starting. */
function retry(fn,n){
  var i=0;
  var go=function(){return fn().catch(function(e){if(i>=n||!retryable(e))throw e;return later(Math.min(8000,retryWait(e,i++))).then(go);});};
  return go();
}
/* Spotify's own Connect API, the one the web player uses (ConnectKeepAlive's window.spoCS). */
function csCall(fn){return Promise.resolve().then(function(){var c=W.spoCS;if(!c)throw err('noid');return fn(c);});}
/* The public Web API is only a fallback: Spotify rate-limits the web player's key there. */
var apiUntil=0;
function api(path,opt){
  var tok=W.spotAuthToken;
  if(!tok)return Promise.reject(err('signin'));
  if(navigator.onLine===false)return Promise.reject(err('offline'));
  if(now()<apiUntil)return Promise.reject(err('busy'));
  opt=opt||{};
  var f=W.oriFetch||W.fetch;
  return f('https://api.spotify.com/v1'+path,{method:opt.method||'GET',headers:{'Authorization':tok,'Content-Type':'application/json'},body:opt.body?JSON.stringify(opt.body):undefined})
    .then(function(r){
      if(r.status===429){var ra=0;try{ra=+r.headers.get('retry-after')||0;}catch(e){}apiUntil=now()+Math.min(600,Math.max(30,ra||60))*1000;throw err('http429',429);}
      if(r.status===204||r.status===202)return null;
      if(!r.ok)throw err('http'+r.status,r.status);
      return r.text().then(function(t){try{return t?JSON.parse(t):null;}catch(e){return null;}});
    });
}
function either(main,fallback){
  return main().catch(function(e){if(quiet(e))throw e;return fallback().catch(function(){throw e;});});
}
var GQ={searchTracks:'b02683192a98dde7966b5e6655a79eeb62713eab703eda9902c932818dd52751',
  fetchLibraryTracks:'087278b20b743578a6262c2b0b4bcd20d879c503cc359a2285baf083ef944240',
  libraryV3:'390c78e5b951029bad359785e69b07b536a509c581cbcd0aded5e5067f187455',
  fetchEntitiesForRecentlyPlayed:'cf5d2e94ffd82788470788ae1f6090cc3e9e774fb8fd383580634c6e6f50f7be',
  profileAttributes:'08ffb4730af3746e04a8301396f20875dbbce10c75243803091a9274eacc8ac0'};
var gqFound={},gqScan=null;
function gqHash(op){
  var h=gqFound[op]||'';
  if(!h)try{h=(W.splOpHashes||{})[op]||'';}catch(e){}
  if(!h)try{h=localStorage.getItem('sp_hash_'+op)||'';}catch(e){}
  return h||GQ[op]||'';
}
function gqLearn(t){
  var re=/"([A-Za-z0-9]+)","query","([0-9a-f]{64})"/g,m;
  while((m=re.exec(t))){if(GQ[m[1]]!==undefined){gqFound[m[1]]=m[2];try{localStorage.setItem('sp_hash_'+m[1],m[2]);}catch(e){}}}
}
/* Spotify renames its queries now and then: read the current ones from its own scripts. */
function gqDiscover(){
  if(gqScan)return gqScan;
  var f=W.oriFetch||W.fetch;
  gqScan=Promise.resolve().then(function(){
    var ss=document.querySelectorAll('script[src]'),main='';
    for(var i=0;i<ss.length;i++){var s=ss[i].src||'';if(/\/web-player\.[0-9a-f]+\.js/.test(s))main=s;}
    if(!main)return;
    return f(main).then(function(r){return r.text();}).then(function(t){
      gqLearn(t);
      var m=t.match(/(\d+):"xpui-routes-search"/);if(!m)return;
      var base=main.slice(0,main.lastIndexOf('/')+1),re=new RegExp('[,{]'+m[1]+':"([0-9a-f]{8})"','g'),hs=[],x;
      while((x=re.exec(t))&&hs.length<4)if(hs.indexOf(x[1])<0)hs.push(x[1]);
      var one=function(i){
        if(i>=hs.length)return;
        return f(base+'xpui-routes-search.'+hs[i]+'.js').then(function(r){if(!r.ok)throw 0;return r.text();})
          .then(function(tx){if(tx.indexOf('searchTracks')<0)throw 0;gqLearn(tx);}).catch(function(){return one(i+1);});
      };
      return one(0);
    });
  }).catch(function(){});
  return gqScan;
}
function spHeaders(json){
  var h={'Authorization':W.spotAuthToken,'Accept':'application/json','App-Platform':'WebPlayer'};
  if(json)h['Content-Type']='application/json;charset=UTF-8';
  if(W.spotCliToken)h['Client-Token']=W.spotCliToken;
  var c=W.__spoConn;if(c&&c.appVer)h['Spotify-App-Version']=c.appVer;
  return h;
}
function gql(op,vars,retried){
  if(!W.spotAuthToken)return Promise.reject(err('signin'));
  if(navigator.onLine===false)return Promise.reject(err('offline'));
  var hash=gqHash(op),f=W.oriFetch||W.fetch;
  return f('https://api-partner.spotify.com/pathfinder/v2/query',{method:'POST',headers:spHeaders(true),
      body:JSON.stringify({variables:vars,operationName:op,extensions:{persistedQuery:{version:1,sha256Hash:hash}}})})
    .then(function(r){
      if(r.status===429)throw busyErr(r);
      return r.text().then(function(t){var j=null;try{j=t?JSON.parse(t):null;}catch(e){}return {r:r,j:j};});
    }).then(function(x){
      var j=x.j;
      if(x.r.ok&&j&&j.data)return j.data;
      var msg=JSON.stringify((j&&j.errors)||'');
      if(!retried&&(/persisted/i.test(msg)||x.r.status===400||x.r.status===404)){
        delete gqFound[op];try{localStorage.removeItem('sp_hash_'+op);}catch(e){}
        gqScan=null;
        return gqDiscover().then(function(){if(gqHash(op)===hash)throw err('nohash');return gql(op,vars,true);});
      }
      throw err(x.r.ok?'gql':'http'+x.r.status,x.r.status);
    });
}
function busyErr(r){var e=err('http429',429);try{e.retryAfter=+r.headers.get('retry-after')||0;}catch(x){}return e;}
/* Spotify's own servers for the web player ("spclient"), with the player's login. */
function spc(path){
  if(!W.spotAuthToken)return Promise.reject(err('signin'));
  if(navigator.onLine===false)return Promise.reject(err('offline'));
  var c=W.__spoConn||{},base=c.spc||'https://spclient.wg.spotify.com';
  return (W.oriFetch||W.fetch)(base+path,{headers:spHeaders(false)}).then(function(r){
    if(r.status===429)throw busyErr(r);
    if(!r.ok)throw err('http'+r.status,r.status);
    return r.json();
  });
}
var uidP=null;
function uid(){
  if(lib.uid)return Promise.resolve(lib.uid);
  if(W.spotUserId){lib.uid=W.spotUserId;return Promise.resolve(lib.uid);}
  if(uidP)return uidP;
  uidP=gql('profileAttributes',{}).then(function(d){return (d&&d.me&&d.me.profile&&d.me.profile.username)||'';})
    .catch(function(){return '';})
    .then(function(u){return u||api('/me').then(function(m){return (m&&m.id)||'';}).catch(function(){return '';});})
    .then(function(u){uidP=null;if(!u)throw err('signin');lib.uid=u;return u;});
  return uidP;
}

/* ---------- who is who ---------- */
function myId(){try{if(W.spoCS)return W.spoCS.id()||'';}catch(e){}return W.__spoMyId||'';}
function myName(){return W.__spoDeviceName||'SpotiOS';}
function isMine(d){
  if(!d)return false;
  var id=myId();
  if(id&&d.id)return d.id===id;
  var c=W.__spoConn,obs=c&&c.obs;
  if(obs&&d.id&&d.id.indexOf(obs)===0)return true;
  return !!d.name&&d.name===myName();
}
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
/* This phone's own Spotify app: a phone in the device list with this phone's name. */
var PH=null;
function phoneNames(){
  if(!PH){var st=status();PH=[st.phoneName,st.model].map(function(x){return String(x||'').trim().toLowerCase();}).filter(Boolean);}
  return PH;
}
function phoneNamed(d){var n=String(d&&d.name||'').trim().toLowerCase();return !!n&&phoneNames().indexOf(n)>=0;}
function isThisPhone(d){return !!d&&!!d.id&&!isMine(d)&&normType(d.type)==='smartphone'&&phoneNamed(d);}
function setDef(d,auto,why_){
  if(!d||!d.id||isMine(d))return;
  if(cfg.def&&cfg.def.id===d.id&&!!cfg.def.auto===!!auto)return;
  cfg.def={id:d.id,name:d.name||'Phone',type:d.type||'',auto:!!auto};
  log((d.name||'This phone')+' is your default device'+(why_?' ('+why_+')':''),'good');
  save();if(shown){renderDef();renderWho();}
}
/* Finds the default device by itself until you pick one. */
function autoDefault(){
  if(cfg.def&&!cfg.def.auto)return;
  for(var i=0;i<S.devices.length;i++)if(isThisPhone(S.devices[i])){setDef(S.devices[i],true,'this phone');return;}
  var a=S.active;
  if(a&&S.playing&&now()-S.spSeen<15000&&!isMine(a)&&normType(a.type)==='smartphone')setDef(a,true,'this phone');
}
function allowed(d){
  if(!d||isDef(d))return true;
  var e=rec(d);
  if(!e){for(var k in cfg.devs){var o=cfg.devs[k];if(o.name===d.name&&normType(o.type)===normType(d.type)){e=o;break;}}}
  if(e&&e.allow===false)return false;
  if(cfg.policy==='list')return !!(e&&e.allow===true);
  return true;
}

/* ---------- Connect state: pushed by Spotify over the player's own connection ---------- */
var S={active:null,playing:false,devices:[],mineAt:0,hold:null,pullAt:0,pullUntil:0,fails:0,localUntil:0,pollAt:0,bounceAt:0,
  gap:0,pulling:false,spSeen:0,dropT:null};
function fromCluster(c){
  var devs=c.devices||{},list=[];
  for(var k in devs){var d=devs[k]||{};list.push({id:d.device_id||k,name:d.name||'',type:d.device_type||'',vol:d.volume});}
  var aid=c.active_device_id||'',a=null;
  for(var i=0;i<list.length;i++)if(list[i].id===aid)a=list[i];
  if(aid&&!a)a={id:aid,name:'',type:''};
  var ps=c.player_state||{};
  onState(a,!!(ps.is_playing&&!ps.is_paused),list);
}
if(W.__spoConn){
  W.__spoConn.onCluster.push(fromCluster);
  if(W.__spoConn.cluster)setTimeout(function(){try{fromCluster(W.__spoConn.cluster);}catch(e){}},0);
}
/* Asks Spotify for the current Connect state (normally it arrives by itself). */
function poll(force){
  if(!W.__spoServer||!W.spotAuthToken||!W.spoCS)return;
  var c=W.__spoConn,age=c&&c.clusterAt?now()-c.clusterAt:1e12;
  if(!force&&age<180000)return;
  if(now()-S.pollAt<(force?3000:60000))return;
  S.pollAt=now();
  W.spoCS.cluster().catch(function(){});
}
function onState(a,playing,list){
  if(list){S.devices=list.filter(function(d){return d.id;});S.devices.forEach(known);}
  var prev=S.active;
  if(a&&!a.name&&prev&&prev.id===a.id)a=prev;
  if(a&&!a.name){for(var i=0;i<S.devices.length;i++)if(S.devices[i].id===a.id)a=S.devices[i];}
  if(a&&!a.name&&rec(a))a={id:a.id,name:rec(a).name,type:rec(a).type};
  S.playing=playing;
  var changed=(!prev!==!a)||(prev&&a&&prev.id!==a.id);
  S.active=a;
  if(W.__spoServer){
    autoDefault();
    if(changed){
      if(a&&isMine(a)){S.mineAt=now();connected(prev&&!isMine(prev)?prev:null);}
      else if(a&&prev&&isMine(prev))log('Music moved to '+(a.name||'another device'),'');
      else if(!a&&prev&&isMine(prev))dropped();
    }
    /* remembered across restarts of the page, for taking a dropped session back */
    if(a&&isMine(a)){var lp=playing?now():0;if(!lp!==!cfg.lastPlay||lp-(cfg.lastPlay||0)>30000)cfg.lastPlay=lp;}
  }
  if(a&&isMine(a))S.mineAt=now();
  save();
  autoConnect();
  if(shown){renderOn();renderDevs();}
}
/* The session dropped while SpotiOS was playing (nothing is active any more): take it back. */
function dropped(){
  clearTimeout(S.dropT);
  if(!cfg.auto||W.__spoStopping||!cfg.lastPlay||now()-cfg.lastPlay>10*60000)return;
  S.dropT=setTimeout(function(){
    if(S.active||W.__spoStopping||!cfg.lastPlay)return;
    var id=myId();if(!id)return;
    log('The Spotify session dropped, so SpotiOS took it back','good');
    S.localUntil=now()+10000;
    retry(function(){return transferTo(id,'resume');},4).then(function(){setTimeout(function(){poll(true);},2000);})
      .catch(function(e){log('Couldn’t get the session back: '+why(e),'bad');});
  },1500);
}
function holdCheck(){
  var h=S.hold;if(!h)return;  /* only Play on uses a hold now */
  var a=S.active,busy=!!a&&a.id===h.id&&S.playing;
  if(busy){h.idle=0;return;}
  if(!h.idle)h.idle=now();
  if(now()-h.idle>10000&&now()-h.at>10000)S.hold=null;
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
    transferTo(from.id,'resume').catch(function(){try{W.actPlayPause&&W.actPlayPause(false);}catch(e){}});
    return;
  }
  cfg.connectedOnce=true;
  if(!cfg.def&&normType(from.type)==='smartphone')setDef(from,true,'it sent music here');
  log(from.name+' connected','good');
  save();
}
function transferTo(id,mode){
  return either(
    function(){return csCall(function(c){return c.transfer(id,mode||'resume');});},
    function(){return api('/me/player',{method:'PUT',body:{device_ids:[id],play:mode!=='pause'}});});
}
/* Take-over: the default device started playing, so bring the music to SpotiOS, right away and every time. */
function takesFrom(a){return !!cfg.auto&&!!a&&!isMine(a)&&isDef(a);}
var acT=null;
function autoConnect(){
  clearTimeout(acT);acT=null;
  if(!W.__spoServer||W.__spoStopping||S.pulling)return;
  var a=S.active;
  if(!a||!S.playing||!takesFrom(a))return;
  var id=myId(),wait=Math.max(S.pullUntil-now(),S.pullAt+S.gap-now(),id?0:1000);
  if(wait>0){acT=setTimeout(autoConnect,wait+50);return;}
  S.pulling=true;S.pullAt=now();S.localUntil=now()+10000;
  var name=a.name||'your default device';
  if(!S.fails)log('Took the music over from '+name,'good');
  transferTo(id,'resume').then(function(){
      S.pulling=false;S.fails=0;S.gap=2500;
      toast('Now playing on SpotiOS');setTimeout(function(){poll(true);},1500);
      /* still shown as playing there a moment later (a missed update): check again */
      acT=setTimeout(autoConnect,S.gap+100);
    }).catch(function(e){
      S.pulling=false;S.fails++;
      S.gap=Math.min(30000,retryWait(e,S.fails-1));
      if(S.fails===1)log('Couldn’t take over from '+name+' yet. Trying again.','bad');
      if(!retryable(e)){S.fails=0;S.gap=0;S.pullUntil=now()+60000;log('Couldn’t take over from '+name+': '+why(e),'bad');}
      autoConnect();
    });
}
/* The Spotify app on this phone was closed: pause (SpotifyWatcher noticed its notification go). */
W.spoSrvSpotifyClosed=function(){
  if(!W.__spoServer||!cfg.pauseClose)return;
  var a=S.active,mine=a&&isMine(a)&&S.playing;
  if(!mine&&!W.playing)return;
  if(now()-S.pullAt<6000)return;   /* Spotify swaps its notification when SpotiOS takes over */
  cfg.lastPlay=0;save();
  try{if(W.actPlayPause)W.actPlayPause(false);}catch(e){}
  log('Spotify was closed, so SpotiOS paused','');
  toast('Spotify was closed, so SpotiOS paused');
};
W.spoSrvTick=function(reason){if(!W.__spoServer)return;holdCheck();poll(reason==='net');autoDefault();autoConnect();warmEme();if(shown)netTick(reason==='net');};
W.spoSrvCheck=function(reason){if(!W.__spoServer)return;if(reason==='spotify')S.spSeen=now();S.pollAt=0;poll(true);autoConnect();};

/* ---------- AdGuard, VPNs and Private DNS that block Spotify (util/NetCheck.kt) ---------- */
var net={ok:true},netAt=0,netT=null;
function netTick(force){
  if(!force&&now()-netAt<30000)return;
  netAt=now();
  var j=null;
  try{j=JSON.parse(AndBridge.netCheck(!!force)||'null');}catch(e){}
  if(!j||typeof j!=='object')return;
  net=j;
  if(shown){renderNet();renderConn();}
  clearTimeout(netT);
  if(j.checking)netT=setTimeout(function(){netAt=0;netTick(false);},2500);
}
function blockers(){return (net.blockers||[]).map(function(b){return typeof b==='string'?{name:b}:(b||{});}).filter(function(b){return b.name||b.pkg;});}
function orList(a){return a.length<2?a.join(''):a.slice(0,-1).join(', ')+' or '+a[a.length-1];}
function renderNet(){
  var el=byId('ss-net');if(!el)return;
  var bad=net.ok===false;
  el.hidden=!bad;
  if(!bad){if(el.innerHTML)el.innerHTML='';return;}
  /* "blockers" are the blocker apps installed on this phone, not necessarily running */
  var bl=blockers(),names=bl.map(function(b){return b.name||b.pkg;}),who,fix,allow=', or add SpotiOS and Spotify to its allowlist';
  if(net.privateDns&&!net.vpn){who='Private DNS';fix='Set Private DNS to Automatic or Off';bl=[];}
  else if(names.length===1){who=names[0];fix='Pause '+names[0]+allow;}
  else if(net.vpn){who=names.length?'A VPN or ad blocker':'A VPN';fix=names.length?'Pause '+orList(names)+allow:'Turn the VPN off';}
  else if(names.length){who=orList(names);fix='Pause it'+allow;}
  else{who='Something on this phone';fix='Turn off any ad blocker, VPN or firewall app';}
  var h='<b>'+ic('warn')+esc(who+' is blocking Spotify')+'</b><p>SpotiOS can’t reach Spotify'+(net.blocked&&net.blocked.length?' ('+esc([].concat(net.blocked).slice(0,3).join(', '))+')':'')+
    ', so the server can’t connect or take the music over. '+esc(fix)+', then tap Check again.</p><div class="ss-btns">';
  if(bl.length)h+='<button class="ss-btn sm pri" data-a="net-app" data-n="'+esc(bl[0].name||bl[0].pkg)+'">Open '+esc(bl[0].name||'the app')+'</button>';
  if(net.vpn)h+='<button class="ss-btn sm'+(bl.length?'':' pri')+'" data-a="net-vpn">VPN settings</button>';
  if(net.privateDns)h+='<button class="ss-btn sm" data-a="net-dns">Private DNS</button>';
  h+='<button class="ss-btn sm" data-a="net-check">'+ic('refresh')+'Check again</button></div>';
  el.innerHTML=h;
}

/* ---------- audio output: phone speaker, Bluetooth, headphones ---------- */
var out={},outAt=0;
function outTick(){
  if(now()-outAt<1500)return;outAt=now();
  try{out=JSON.parse(AndBridge.audioOutput()||'{}')||{};}catch(e){out={};}
  renderOut();
}
function outIc(k){return k==='bt'?'bt':(k==='wired'||k==='usb')?'headphones':'output';}
function outName(){return out.name||'Phone speaker';}
function renderOut(){
  var t=byId('ss-out-t');if(!t)return;
  var n=outName();if(t.textContent!==n)t.textContent=n;
  var i=byId('ss-out-i'),k=out.kind||'speaker';
  if(i&&i.getAttribute('data-k')!==k){i.setAttribute('data-k',k);i.innerHTML=ic(outIc(k));}
}
function pickOutput(){
  try{AndBridge.openAudioOutput();}catch(e){toast('Couldn’t open the output picker');}
  outAt=0;setTimeout(outTick,1500);setTimeout(outTick,5000);
}

/* The first song from another device starts faster when Widevine is already set up. */
var emeDone=false;
function warmEme(){
  if(emeDone||!W.__spoServer||!W.spotAuthToken)return;
  emeDone=true;
  try{['https://audio-ak-spotify-com.akamaized.net','https://audio4-ak-spotify-com.akamaized.net','https://audio-fa.scdn.co',
      'https://gew4-spclient.spotify.com','https://i.scdn.co','https://seektables.scdn.co'].forEach(function(u){
    var l=document.createElement('link');l.rel='preconnect';l.href=u;l.crossOrigin='anonymous';document.head.appendChild(l);});}catch(e){}
  try{
    if(!navigator.requestMediaKeySystemAccess)return;
    navigator.requestMediaKeySystemAccess('com.widevine.alpha',[{initDataTypes:['cenc'],audioCapabilities:[{contentType:'audio/mp4; codecs="mp4a.40.2"',robustness:'SW_SECURE_CRYPTO'}]}])
      .then(function(a){return a.createMediaKeys();}).catch(function(){});
  }catch(e){}
}

"""

    private const val JS2 = """
/* ---------- screen ---------- */
var shown=false,full=false,built=false,info={},tmr=null,lastNP='',np={pos:0,at:0,dur:0,playing:false},fullTipped=false;
function onPlayerPage(){return location.hostname==='open.spotify.com';}
function want(){return !!W.__spoServer&&!full&&onPlayerPage();}
function build(){
  if(built)return;built=true;
  var st=document.createElement('style');st.id='ss-style';st.textContent=CSS;document.head.appendChild(st);
  var el=document.createElement('div');el.id='spoSrv';
  el.innerHTML=
   '<div class="ss-bg" id="ss-bg"></div><div class="ss-shade"></div>'+
   '<div class="ss-top"><div class="ss-badge" id="ss-badge"><i class="ss-dot" id="ss-dot"></i><b>SpotiOS Server</b><span id="ss-state">Starting…</span></div>'+
     '<button class="ss-ib stop" id="ss-stop" aria-label="Stop server">'+ic('power')+'</button>'+
     '<button class="ss-ib" id="ss-menu" aria-label="More">'+ic('more')+'</button></div>'+
   '<div class="ss-scroll" id="ss-scroll">'+
    '<div class="ss-net" id="ss-net" hidden></div>'+
    '<section class="ss-np idle" id="ss-np">'+
     '<div class="ss-art" id="ss-art"><img id="ss-cover" alt=""><div class="ss-idle"><i></i><i></i><i></i><div class="ss-core">'+ic('cast')+'</div></div></div>'+
     '<div class="ss-title" id="ss-title">Waiting for Spotify</div>'+
     '<div class="ss-artist" id="ss-artist">Pick “SpotiOS” in Spotify’s device list</div>'+
     '<div class="ss-prog"><div class="ss-bar"><i id="ss-fill"></i></div><div class="ss-times"><span id="ss-pos">0:00</span><span id="ss-dur">0:00</span></div></div>'+
     '<div class="ss-ctrl"><button class="ss-sm" id="ss-fx" aria-label="Sound">'+ic('sliders')+'</button><button id="ss-prev" aria-label="Previous">'+ic('prev')+'</button>'+
      '<button class="ss-pp" id="ss-play" aria-label="Play">'+ic('play')+'</button><button id="ss-next" aria-label="Next">'+ic('next')+'</button>'+
      '<button class="ss-sm" id="ss-dl" aria-label="Download">'+ic('download')+'</button></div>'+
     '<div class="ss-outs"><button class="ss-on" id="ss-on">'+ic('cast')+'<span id="ss-on-t">Nothing playing</span>'+ic('chev')+'</button>'+
      '<button class="ss-on" id="ss-out" aria-label="Audio output"><i id="ss-out-i">'+ic('output')+'</i><span id="ss-out-t">Phone speaker</span></button></div>'+
    '</section>'+
    '<section class="ss-card'+(cfg.libFold?'':' open')+'" id="ss-lib">'+
      '<div class="ss-h" data-a="fold-lib">'+ic('music')+'Play on SpotiOS'+ic('chev').replace('<svg','<svg class="ss-chev"')+'</div>'+
      '<div class="ss-search" id="ss-search">'+ic('search')+'<input id="ss-q" type="search" enterkeyhint="search" placeholder="Search songs" autocomplete="off"><button id="ss-qx" aria-label="Clear">'+ic('x')+'</button></div>'+
      '<div class="ss-chips" id="ss-chips"><button data-t="recent" class="on">Recents</button><button data-t="playlists">Playlists</button><button data-t="liked">Liked songs</button></div>'+
      '<div class="ss-list" id="ss-list"></div>'+
    '</section>'+
    '<section class="ss-card" id="ss-def"></section>'+
    '<section class="ss-card" id="ss-conn"></section>'+
    '<section class="ss-card" id="ss-how"></section>'+
    '<section class="ss-card" id="ss-who"></section>'+
    '<section class="ss-card" id="ss-set"></section>'+
    '<section class="ss-card" id="ss-act"></section>'+
    '<div class="ss-foot">SpotiOS plays through this phone’s speaker or whatever it’s connected to. It can’t play while the phone is off or has no internet.</div>'+
   '</div>';
  document.body.appendChild(el);
  var sc=document.createElement('div');sc.id='ss-scrim';document.body.appendChild(sc);
  var sh=document.createElement('div');sh.id='ss-sheet';document.body.appendChild(sh);
  var to=document.createElement('div');to.id='ss-toast';document.body.appendChild(to);
  var back=document.createElement('button');back.id='ss-back';back.setAttribute('aria-label','Back to the Server screen');
  back.innerHTML='<i class="ss-dot ok"></i>Server';document.body.appendChild(back);
  bind(el);
  sc.addEventListener('click',closeSheet);
  sh.addEventListener('click',onSheetClick);
  sh.addEventListener('input',onSheetInput);
  back.addEventListener('click',function(){haptic();backToServer();});
}
/* In full Spotify: a Server tab in the tab bar (or a button at the top when tabs are off). */
function syncReturn(){
  var inFull=!!W.__spoServer&&full&&onPlayerPage();
  var tabs=byId('spoTabs'),tab=byId('ss-tab');
  var tabsOn=!!tabs&&document.documentElement.classList.contains('spo-tabs');
  if(W.__spoServer&&tabs&&!tab){
    tab=document.createElement('button');tab.type='button';tab.id='ss-tab';tab.setAttribute('aria-label','Server');
    tab.innerHTML=ic('cast')+'<i class="ss-tab-dot"></i><span>Server</span>';
    tab.addEventListener('click',function(e){e.stopPropagation();haptic();backToServer();});
    tabs.insertBefore(tab,tabs.firstChild);
  }else if(!W.__spoServer&&tab){tab.parentNode.removeChild(tab);}
  var b=byId('ss-back');if(b)b.classList.toggle('show',inFull&&!tabsOn);
}
function backToServer(){full=false;sync();var sc=byId('ss-scroll');if(sc)sc.scrollTop=0;}
function openFull(){
  full=true;closeSheet();sync();
  if(!fullTipped){fullTipped=true;
    var tabsOn=!!byId('spoTabs')&&document.documentElement.classList.contains('spo-tabs');
    setTimeout(function(){toast(tabsOn?'Tap Server in the tab bar to come back':'Tap Server at the top to come back');},450);}
}
function sync(){
  var on=want();
  if(on||W.__spoServer)build();
  document.documentElement.classList.toggle('ss-on',on);
  var el=byId('spoSrv');if(el)el.classList.toggle('ss-show',on);
  syncReturn();
  if(on&&!shown){
    shown=true;
    info=status();
    renderAll();
    loadList();
    netTick(false);outTick();
    poll(true);
    warmEme();
    if(!tmr)tmr=setInterval(loop,500);
  }else if(!on&&shown){
    shown=false;closeSheet();
  }
}
function realHidden(){try{return W.__spoRealHidden?W.__spoRealHidden():document.hidden;}catch(e){return false;}}
var n5=0;
function loop(){
  if(!shown){clearInterval(tmr);tmr=null;return;}
  if(realHidden()||W.__splBg)return;
  renderNP();
  if(++n5%10===0){info=status();renderConn();syncReturn();outTick();}
  if(n5%30===0){poll(false);renderLog();netTick(false);}
}
function status(){try{return JSON.parse(AndBridge.serverStatus()||'{}')||{};}catch(e){return {};}}
function renderAll(){fxLoad();renderFxBtn();renderNet();renderNP();renderOn();renderOut();renderConn();renderHow();renderDef();renderWho();renderSet();renderLog();}

function connState(){
  var c=W.__spoConn||{},n=now(),ws=c.ws,st=ws?ws.readyState:-1;
  if(navigator.onLine===false)return {k:'bad',t:'No internet',d:'SpotiOS reconnects by itself when the internet is back.'};
  if(!W.spotAuthToken)return n-(c.since||n)>20000?{k:'warn',t:'Signing in…',d:'Waiting for Spotify to sign SpotiOS in.'}:{k:'warn',t:'Starting…',d:'Starting the Spotify player.'};
  var listed=!!c.inCluster&&c.clusterAt&&n-c.clusterAt<10*60000;
  if(listed||(st===1&&c.lastMsg&&n-c.lastMsg<75000))return {k:'ok',t:'Ready',d:'Shows up as “'+myName()+'” on all your Spotify devices.'};
  if(st===3)return {k:'warn',t:'Reconnecting…',d:'The connection to Spotify dropped. SpotiOS is reconnecting.'};
  return {k:'warn',t:'Connecting…',d:'Joining Spotify Connect.'};
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
   (net.ok!==false&&net.vpn?'<div class="ss-row"><div class="ss-ico y">'+ic('shield')+'</div><div class="ss-tx"><b>A VPN is on</b><small>Spotify gets through right now. If SpotiOS stops connecting, turn off '+
     esc(blockers().length?orList(blockers().map(function(b){return b.name||b.pkg;})):'the VPN')+'.</small></div></div>':'')+
   '<div class="ss-btns"><button class="ss-btn sm" data-a="reconnect">'+ic('refresh')+'Reconnect now</button></div>';
}
function renderHow(){
  var el=byId('ss-how');if(!el)return;
  var open=el.classList.contains('open')||(!cfg.connectedOnce&&!el.getAttribute('data-r'));
  el.setAttribute('data-r','1');
  el.classList.toggle('open',open);
  el.innerHTML='<div class="ss-h" data-a="fold">'+ic('help')+'How to connect'+ic('chev').replace('<svg','<svg class="ss-chev"')+'</div>'+
   '<div class="ss-fold"><ol class="ss-steps">'+
    '<li>Leave SpotiOS running<small>Lock the phone or swipe SpotiOS away, the server keeps going. You don’t need to play anything here first.</small></li>'+
    '<li>Open Spotify<small>On this phone, a computer, a tablet, anything signed in to the same Spotify account.</small></li>'+
    '<li>Tap the devices button<small>The speaker icon at the bottom of the Now Playing screen, or next to the volume on a computer.</small></li>'+
    '<li>Pick “'+esc(myName())+'”<small>The music plays here, and Spotify becomes the remote. On this phone you don’t even need that: press play in Spotify and SpotiOS takes the music over by itself.</small></li>'+
   '</ol><div class="ss-note">Works on any network, Wi-Fi or mobile data, because Spotify Connect goes through Spotify’s servers. If “'+esc(myName())+'” is missing from the list, tap Reconnect now above.</div>'+
   '<div class="ss-btns"><button class="ss-btn pri" data-a="spotify">'+ic('spotify')+'Open Spotify</button></div></div>';
}
function renderDef(){
  var el=byId('ss-def');if(!el)return;
  var d=cfg.def,on=!!cfg.auto,h='<div class="ss-h">'+ic('takeover')+'Default device'+(on&&d?'<em>Take over on</em>':'')+'</div>';
  if(d){
    var online=S.devices.some(function(x){return same(x,d);});
    h+='<div class="ss-row"><div class="ss-ico g">'+ic(devIc(d.type))+'</div><div class="ss-tx"><b>'+esc(d.name)+(phoneNamed(d)?'<span class="ss-tag n">THIS PHONE</span>':'')+'</b><small>'+
      devLabel(d.type)+(online?' · online':' · not online right now')+(d.auto?' · found by itself':'')+'</small></div><button class="ss-btn sm" data-a="pickdef">Change</button></div>';
  }else{
    h+='<div class="ss-row"><div class="ss-ico y">'+ic('phone')+'</div><div class="ss-tx"><b>Not found yet</b><small>Open Spotify on this phone and press play: SpotiOS finds it by itself. Or choose it.</small></div>'+
      '<button class="ss-btn sm pri" data-a="pickdef">Choose</button></div>';
  }
  h+='<div class="ss-row"><div class="ss-ico'+(on?' g':'')+'">'+ic('cast')+'</div><div class="ss-tx"><b>Take over its music</b><small>'+
    (on?'Whenever '+(d?esc(d.name):'your default device')+' plays, SpotiOS takes the music over right away, every time, and takes it back if the Spotify session drops.':'Off. SpotiOS only plays what you send to it.')+
   '</small></div><button class="ss-sw'+(on?' on':'')+'" data-a="auto" aria-label="Take over its music"></button></div>'+
   '<div class="ss-note">Only the default device is taken over. Your computer, speakers and TVs play as usual.</div>';
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
    var def=isDef(d)&&!!cfg.auto,ok=allowed(d);
    var sub=devLabel(d.type)+(d.on?' · online':(d.seen?' · seen '+ago(d.seen):''));
    h+='<div class="ss-row"><div class="ss-ico'+(ok?'':' r')+'">'+ic(ok?devIc(d.type):'block')+'</div><div class="ss-tx"><b>'+esc(d.name||'Device')+
      (def?'<span class="ss-tag">TAKE OVER</span>':(ok?'':'<span class="ss-tag b">BLOCKED</span>'))+'</b><small>'+esc(sub)+'</small></div>'+
      '<button class="ss-sw'+(ok?' on':'')+(def?' dis':'')+'" data-a="allow" data-id="'+esc(d.id)+'" aria-label="Allow '+esc(d.name)+'"></button></div>';
  });
  h+='<div class="ss-note">Spotify can’t stop another device from picking SpotiOS, so when a blocked device sends music here, SpotiOS sends it straight back.'+(cfg.auto&&cfg.def?' '+esc(cfg.def.name)+' is always allowed, because SpotiOS takes its music over.':'')+'</div>';
  el.innerHTML=h;
}
function renderDevs(){renderWho();renderDef();}
function renderSet(){
  var el=byId('ss-set');if(!el)return;
  var ws=!!info.withSpotify,na=!!info.notifAccess,ob=info.onBoot!==false;
  var h='<div class="ss-h">'+ic('settings')+'Server</div>'+
   '<div class="ss-row"><div class="ss-ico'+(ob?' g':'')+'">'+ic('power')+'</div><div class="ss-tx"><b>Start when the phone starts</b><small>'+
     (ob?'The server starts by itself when the phone turns on and keeps running. Only Stop server stops it.':'Off. Open SpotiOS to start the server after a restart.')+
     '</small></div><button class="ss-sw'+(ob?' on':'')+'" data-a="boot" aria-label="Start when the phone starts"></button></div>'+
   '<div class="ss-row"><div class="ss-ico g">'+ic('spotify')+'</div><div class="ss-tx"><b>Start with Spotify</b><small>'+
     (ws&&!na?'Needs notification access so SpotiOS can notice Spotify. <u data-a="notif">Allow it</u>.':'When Spotify starts playing on this phone, SpotiOS starts too'+(cfg.auto?' and takes the music over':'')+'.')+
     (info.spotifyInstalled===false?' Spotify isn’t installed on this phone.':'')+'</small></div><button class="ss-sw'+(ws?' on':'')+'" data-a="withsp" aria-label="Start with Spotify"></button></div>'+
   '<div class="ss-row"><div class="ss-ico'+(cfg.pauseClose?' g':'')+'">'+ic('pausec')+'</div><div class="ss-tx"><b>Pause when Spotify closes</b><small>'+
     (cfg.pauseClose&&!na?'Needs notification access so SpotiOS can notice Spotify closing. <u data-a="notif">Allow it</u>.':
      cfg.pauseClose?'When you close the Spotify app on this phone, SpotiOS pauses the music.':'Off. The music keeps playing when Spotify is closed.')+
     '</small></div><button class="ss-sw'+(cfg.pauseClose?' on':'')+'" data-a="pauseclose" aria-label="Pause when Spotify closes"></button></div>'+
   '<div class="ss-row"><div class="ss-ico '+(info.battery?'g':'y')+'">'+ic('battery')+'</div><div class="ss-tx"><b>Battery: '+(info.battery?'unrestricted':'restricted')+'</b><small>'+
     (info.battery?'Android lets SpotiOS run in the background.':'Android may pause SpotiOS in the background and drop the connection.')+'</small></div>'+
     (info.battery?'':'<button class="ss-btn sm pri" data-a="battery">Fix</button>')+'</div>'+
   '<div class="ss-row"><div class="ss-ico g">'+ic('swipe')+'</div><div class="ss-tx"><b>Never stops by itself</b><small>Swiping SpotiOS out of recent apps doesn’t stop the server, and a watchdog starts it again if Android closes it. Tap Stop server to stop it.</small></div></div>'+
   '<div class="ss-btns"><button class="ss-btn sm dng" data-a="stop">'+ic('power')+'Stop server</button><button class="ss-btn sm" data-a="full">'+ic('grid')+'Spotify player mode</button><button class="ss-btn sm" data-a="settings">'+ic('settings')+'SpotiOS settings</button><button class="ss-btn sm" data-a="normal">'+ic('phone')+'Switch to Normal mode</button></div>';
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
var lib={tab:'recent',q:'',items:[],req:0,qt:null,uid:'',cache:{},tries:0,rt:null};
/* Recents, playlists and liked songs are kept between starts, so they show at once. */
var LISTS='spoSrvLists';
try{var kept=JSON.parse(localStorage.getItem(LISTS)||'{}')||{};for(var kk in kept){var kv=kept[kk];if(kv&&Array.isArray(kv.items))lib.cache[kk]={at:0,items:kv.items};}}catch(e){}
function keepLists(){
  var o={};['recent','playlists','liked'].forEach(function(k){var c=lib.cache[k];if(c&&c.items.length)o[k]={items:c.items.slice(0,50)};});
  try{localStorage.setItem(LISTS,JSON.stringify(o));}catch(e){}
}
function pick(src){
  if(!src||!src.length)return '';
  var b=null;
  for(var i=0;i<src.length;i++){var s=src[i]||{},w=+s.width||0;if(w>=120&&w<=400&&(!b||w<(+b.width||9999)))b=s;}
  return ((b||src[0])||{}).url||'';
}
function imgOf(d){
  if(!d)return '';
  var c=d.coverArt||(d.albumOfTrack&&d.albumOfTrack.coverArt);if(c&&c.sources)return pick(c.sources);
  var im=d.images;if(im&&im.items&&im.items.length)return pick(im.items[0].sources);
  if(im&&im.length)return pick(im);
  var v=d.visuals&&d.visuals.avatarImage;if(v&&v.sources)return pick(v.sources);
  if(d.image&&d.image.sources)return pick(d.image.sources);
  return '';
}
function isLiked(uri){return /:collection/.test(uri)&&!/:collection:/.test(uri);}
function gTrack(w){
  if(!w)return null;
  var d=w.data||w,uri=w._uri||d.uri||'';
  if(!uri||uri.indexOf(':track:')<0)return null;
  var ar=((d.artists&&d.artists.items)||[]).map(function(a){return a&&a.profile&&a.profile.name;}).filter(Boolean);
  var alb=d.albumOfTrack||{};
  return {kind:'track',name:d.name||'',sub:ar.join(', '),art:imgOf(d),uri:uri,ctx:alb.uri||''};
}
function wTrack(t){
  var im=(t.album&&t.album.images)||[];
  return {kind:'track',name:t.name,sub:(t.artists||[]).map(function(a){return a.name;}).join(', '),art:im.length?(im[1]||im[0]).url:'',uri:t.uri,ctx:(t.album&&t.album.uri)||''};
}
function gEntity(w){
  if(!w)return null;
  var d=w.data||w,uri=w._uri||d.uri||'';
  if(!uri||uri.indexOf(':folder:')!==-1)return null;
  if(isLiked(uri))return {kind:'liked',name:d.name||'Liked Songs',sub:'Your liked songs',art:'',uri:uri};
  var kind=uri.split(':')[1]||'';
  var label={playlist:'Playlist',album:'Album',artist:'Artist',show:'Podcast',episode:'Episode'}[kind]||'';
  var by=(d.ownerV2&&d.ownerV2.data&&d.ownerV2.data.name)||(d.artists&&d.artists.items&&d.artists.items[0]&&d.artists.items[0].profile&&d.artists.items[0].profile.name)||'';
  var name=d.name||(d.profile&&d.profile.name)||'';
  if(!name)return null;
  return {kind:'ctx',name:name,sub:label+(by?' · '+by:''),art:imgOf(d),uri:uri};
}
function listMsg(h){var l=byId('ss-list');if(l)l.innerHTML='<div class="ss-msg">'+h+'</div>';}
function fetchList(){
  if(lib.q){
    var q=lib.q;
    return either(function(){
      return gql('searchTracks',{searchTerm:q,offset:0,limit:30,includeAudiobooks:false,includePreReleases:true,includeAlbumPreReleases:true,includeAuthors:false,includeEpisodeContentRatingsV2:false})
        .then(function(d){var t=d&&d.searchV2&&d.searchV2.tracksV2;return ((t&&t.items)||[]).map(function(x){return gTrack(x&&(x.item||x));}).filter(Boolean);});
    },function(){return api('/search?type=track&limit=25&q='+encodeURIComponent(q)).then(function(j){return ((j&&j.tracks&&j.tracks.items)||[]).filter(Boolean).map(wTrack);});});
  }
  if(lib.tab==='playlists'){
    return either(function(){
      return gql('libraryV3',{filters:['Playlists'],order:null,textFilter:null,features:['LIKED_SONGS','YOUR_EPISODES_V2','PRERELEASES','PRERELEASES_V2','EVENTS'],
          limit:50,offset:0,flatten:true,expandedFolders:[],folderUri:null,includeFoldersWhenFlattening:true})
        .then(function(d){
          var l=d&&d.me&&d.me.libraryV3;
          var out=((l&&l.items)||[]).map(function(x){return gEntity(x&&(x.item||x));}).filter(Boolean);
          if(!out.some(function(x){return x.kind==='liked';}))out.unshift({kind:'liked',name:'Liked Songs',sub:'Your liked songs',art:'',uri:''});
          return out;
        });
    },function(){
      return api('/me/playlists?limit=50').then(function(j){
        return [{kind:'liked',name:'Liked Songs',sub:'Your liked songs',art:'',uri:''}].concat(((j&&j.items)||[]).filter(Boolean).map(function(pl){
          var im=pl.images||[];return {kind:'ctx',name:pl.name,sub:'Playlist'+(pl.owner&&pl.owner.display_name?' · '+pl.owner.display_name:''),art:im.length?(im[im.length>1?1:0].url):'',uri:pl.uri};
        }));
      });
    });
  }
  if(lib.tab==='liked'){
    return either(function(){
      return gql('fetchLibraryTracks',{offset:0,limit:50}).then(function(d){
        var t=d&&d.me&&d.me.library&&d.me.library.tracks;
        return ((t&&t.items)||[]).map(function(x){return gTrack(x&&(x.track||x.item||x));}).filter(Boolean);
      });
    },function(){return api('/me/tracks?limit=50').then(function(j){return ((j&&j.items)||[]).map(function(x){return x&&x.track;}).filter(Boolean).map(wTrack);});});
  }
  /* Recents: what you played lately (playlists, albums, artists), like the Spotify app's list. */
  return either(function(){
    return uid().then(function(u){
      return spc('/recently-played/v3/user/'+encodeURIComponent(u)+'/recently-played?format=json&offset=0&limit=50&filter=default,collection-new-episodes');
    }).then(function(j){
      var seen={},uris=[];
      ((j&&j.playContexts)||[]).forEach(function(p){var u=p&&p.uri;if(u&&!seen[u]&&uris.length<30){seen[u]=1;uris.push(u);}});
      if(!uris.length)return [];
      return gql('fetchEntitiesForRecentlyPlayed',{uris:uris}).then(function(d){
        var look=(d&&d.lookup)||[],out=[];
        for(var i=0;i<look.length;i++){var e=gEntity(look[i]);if(e){if(!e.uri)e.uri=uris[i];out.push(e);}}
        return out;
      });
    });
  },function(){
    return api('/me/player/recently-played?limit=40').then(function(j){
      var seen={},out=[];
      ((j&&j.items)||[]).forEach(function(x){var t=x&&x.track;if(t&&t.uri&&!seen[t.uri]){seen[t.uri]=1;out.push(wTrack(t));}});
      return out;
    });
  });
}
function loadList(force,again){
  clearTimeout(lib.rt);lib.rt=null;
  if(!again)lib.tries=0;
  var id=++lib.req,key=lib.q?'q:'+lib.q:lib.tab,hit=lib.cache[key];
  if(!force&&!again&&hit&&hit.at&&now()-hit.at<(lib.q?120000:300000)){lib.items=hit.items;renderList();return;}
  if(hit&&hit.items.length){lib.items=hit.items;renderList();}else if(!again)listWait('');
  fetchList().then(function(items){
    lib.tries=0;lib.cache[key]={at:now(),items:items};
    if(!lib.q)keepLists();
    if(id!==lib.req)return;
    lib.items=items;renderList();
  }).catch(function(e){
    if(id!==lib.req)return;
    /* Spotify busy or still starting: keep what's shown and try again by itself */
    var w=retryWait(e,lib.tries++);
    if(!retryable(e)&&lib.tries>2){if(!(hit&&hit.items.length))listMsg(esc(why(e))+'<br><button class="ss-btn sm" data-a="relist">Try again</button>');return;}
    if(!(hit&&hit.items.length))listWait(waitText(e));
    lib.rt=setTimeout(function(){if(shown&&id===lib.req)loadList(true,true);},w);
  });
}
function waitText(e){
  var m=String(e&&e.message||'');
  if(m==='offline')return 'Waiting for internet…';
  if(m==='signin'||m==='noid')return 'Spotify is starting…';
  if(m==='http429'||m==='busy')return 'Spotify is busy. Trying again by itself…';
  return 'Couldn’t reach Spotify. Trying again by itself…';
}
function listWait(sub){
  listMsg('<div class="ss-spin">'+ic('refresh')+'</div>Loading…'+(sub?'<small>'+esc(sub)+'</small><button class="ss-link" data-a="relist">Try now</button>':''));
}
function renderList(){
  var l=byId('ss-list');if(!l)return;
  if(!lib.items.length){listMsg(lib.q?'No songs found.':lib.tab==='recent'?'Nothing played yet.':'Nothing here yet.');return;}
  var cur=W.track||'';
  l.innerHTML=lib.items.map(function(it,i){
    var art=it.kind==='liked'?'<div class="ss-ph lk">'+ic('heart')+'</div>':(it.art?'<img loading="lazy" src="'+esc(it.art)+'" alt=""'+(it.uri.indexOf(':artist:')>0?' class="rnd"':'')+'>':'<div class="ss-ph">'+ic('music')+'</div>');
    return '<div class="ss-it'+(it.kind==='track'&&cur&&it.name===cur?' now':'')+'" data-i="'+i+'" role="button">'+art+'<div class="ss-tx"><b>'+esc(it.name)+'</b><small>'+esc(it.sub)+'</small></div><span class="ss-go">'+ic('play')+'</span></div>';
  }).join('');
}
function playItem(i,row){
  var it=lib.items[i];if(!it)return;
  np.pend=null;
  if(it.kind==='liked'){
    rowBusy(row);
    retry(uid,3).then(function(u){playCtx('spotify:user:'+u+':collection',null,row);}).catch(function(e){rowDone(row);toast(why(e));});
    return;
  }
  if(it.kind==='ctx'){playCtx(it.uri,null,row);return;}
  if(it.name&&it.name!==(W.track||'')){
    np.pend={t:it.name,a:it.sub||'',cv:it.art||'',old:W.track||'',until:now()+8000};
    renderNP();
  }
  /* a song keeps going like in the app: through Liked Songs, or through its album */
  if(lib.tab==='liked'&&!lib.q){
    rowBusy(row);
    retry(uid,3).then(function(u){playCtx('spotify:user:'+u+':collection',it.uri,row);}).catch(function(e){rowDone(row);np.pend=null;toast(why(e));});
    return;
  }
  playCtx(it.ctx||it.uri,it.ctx?it.uri:null,row);
}
function rowBusy(row){
  var b=document.querySelectorAll('#ss-list .ss-it.busy');for(var k=0;k<b.length;k++)rowDone(b[k]);
  if(!row)return;row.classList.add('busy');var g=row.querySelector('.ss-go');if(g)g.innerHTML=ic('refresh');
}
function rowDone(row){if(!row)return;row.classList.remove('busy');var g=row.querySelector('.ss-go');if(g)g.innerHTML=ic('play');}
function playCtx(ctx,skip,row){
  rowBusy(row);
  haptic();
  S.localUntil=now()+10000;S.hold=null;
  var early=!!np.pend;if(early)toTop();
  retry(function(){return either(function(){return csCall(function(c){return c.play(ctx,skip||null,'');});},
    function(){
      var id=myId(),q=id?'?device_id='+encodeURIComponent(id):'',body={context_uri:ctx};
      if(skip)body.offset={uri:skip};
      return api('/me/player/play'+q,{method:'PUT',body:body});
    });},3)
  .then(function(){
    rowDone(row);
    var all=document.querySelectorAll('#ss-list .ss-it.now');for(var k=0;k<all.length;k++)all[k].classList.remove('now');
    if(row)row.classList.add('now');
    if(!early)toTop();
  }).catch(function(e){np.pend=null;lastNP='';renderNP();rowDone(row);toast(why(e));});
}
function toTop(){var sc=byId('ss-scroll');if(sc)sc.scrollTo({top:0,behavior:'smooth'});}

/* ---------- sound: volume boost, bass, treble, surround (service/SoundFx.kt) ---------- */
var FX=[['boost','Volume boost','Louder than the phone’s maximum'],['bass','Bass boost',''],['treble','Treble',''],['surround','Surround','Wider sound on headphones']];
var fx={},fxT=null;
function fxLoad(){try{fx=JSON.parse(AndBridge.soundFx()||'{}')||{};}catch(e){fx={};}return fx;}
function fxBody(){
  var f=fxLoad(),sup=f.supported||{},any=false;
  FX.forEach(function(x){if(sup[x[0]]!==false)any=true;});
  if(!any)return '<div class="ss-msg">This phone doesn’t let apps change the sound.</div>';
  var h='<div class="ss-row" data-f="on"><div class="ss-ico'+(f.on?' g':'')+'">'+ic('sliders')+'</div><div class="ss-tx"><b>Sound effects</b><small>'+(f.on?'On':'Off')+'</small></div>'+
    '<button class="ss-sw'+(f.on?' on':'')+'" aria-label="Sound effects"></button></div>';
  FX.forEach(function(x){
    var k=x[0],v=Math.max(0,Math.min(100,Math.round(+f[k]||0))),na=sup[k]===false;
    h+='<div class="ss-sl'+(na?' na':'')+'"><label>'+x[1]+'<span id="ss-fxv-'+k+'">'+(na?'Not on this phone':v+'%')+'</span></label>'+
      '<input type="range" min="0" max="100" step="1" value="'+v+'" data-k="'+k+'" style="--p:'+v+'%"'+(f.on&&!na?'':' disabled')+' aria-label="'+x[1]+'"></div>';
  });
  h+='<div class="ss-row" data-f="out"><div class="ss-ico">'+ic(outIc(out.kind))+'</div><div class="ss-tx"><b>Audio output</b><small>'+esc(outName())+'</small></div><span class="ss-btn sm">Change</span></div>'+
    '<div class="ss-btns"><button class="ss-btn sm" data-f="reset">'+ic('refresh')+'Reset</button></div>'+
    '<div class="ss-note">If the sound crackles, turn Volume boost down.</div>';
  return h;
}
function fxSave(p){
  for(var k in p)fx[k]=p[k];
  clearTimeout(fxT);
  fxT=setTimeout(function(){try{AndBridge.setSoundFx(JSON.stringify({boost:+fx.boost||0,bass:+fx.bass||0,treble:+fx.treble||0,surround:+fx.surround||0}));}catch(e){}renderFxBtn();},120);
}
function onSheetInput(e){
  var t=e.target;if(sheetKind!=='fx'||!t||!t.getAttribute)return;
  var k=t.getAttribute('data-k');if(!k)return;
  var v=+t.value;t.style.setProperty('--p',v+'%');
  var l=byId('ss-fxv-'+k);if(l)l.textContent=v+'%';
  var p={};p[k]=v;fxSave(p);
}
function fxClick(e){
  var r=e.target.closest&&e.target.closest('[data-f]');if(!r)return;
  var f=r.getAttribute('data-f');haptic();
  if(f==='on'){var on=!fx.on;fx.on=on;try{AndBridge.setSoundFx(JSON.stringify({on:on}));}catch(x){}refillFx();return;}
  if(f==='reset'){try{AndBridge.setSoundFx(JSON.stringify({boost:0,bass:0,treble:0,surround:0}));}catch(x){}refillFx();toast('Sound reset');return;}
  if(f==='out'){pickOutput();return;}
}
function refillFx(){var sb=byId('ss-sb');if(sb&&sheetKind==='fx')sb.innerHTML=fxBody();else fxLoad();renderFxBtn();}
/* the Sound button is green while an effect is changing the sound */
function renderFxBtn(){var b=byId('ss-fx');if(b)b.classList.toggle('on',!!fx.on&&FX.some(function(x){return (+fx[x[0]]||0)>0;}));}

/* ---------- sheets: menu, stop, play on, default device, sound ---------- */
var sheetKind='';
function sheetRow(attr,icon,title,sub,cls){
  return '<div class="ss-row'+(cls?' '+cls:'')+'" '+attr+'><div class="ss-ico'+(cls&&cls.indexOf('dng')>-1?' r':'')+'">'+ic(icon)+'</div><div class="ss-tx"><b>'+title+'</b>'+(sub?'<small>'+sub+'</small>':'')+'</div>'+ic('check').replace('<svg','<svg class="ss-chk"')+'</div>';
}
function openSheet(kind){
  sheetKind=kind;
  var sh=byId('ss-sheet'),t,p,body='';
  if(kind==='menu'){
    t='SpotiOS Server';p='SpotiOS is a Spotify Connect speaker right now.';
    body=sheetRow('data-m="full"','spotify','Spotify player mode','Browse and play in the full Spotify player. Come back with the Server tab.')+
      sheetRow('data-m="play"','cast','Play on another device','Move the music to a speaker, computer or TV.')+
      sheetRow('data-m="out"','output','Audio output',esc(outName())+'. Switch to Bluetooth, headphones or the speaker.')+
      sheetRow('data-m="fx"','sliders','Sound','Volume boost, bass boost, treble and surround.')+
      sheetRow('data-m="settings"','settings','SpotiOS settings','Look, playback and updates.')+
      sheetRow('data-m="stop"','power','Stop server','Leave Spotify Connect and close SpotiOS.','dng');
  }else if(kind==='stop'){
    t='Stop the server?';p='SpotiOS leaves Spotify Connect and closes. It starts again when you open SpotiOS'+(info.onBoot!==false?' or restart the phone':'')+'.';
    body=sheetRow('data-m="stop-yes"','power','Stop server','','dng')+sheetRow('data-m="cancel"','x','Keep it running','');
  }else if(kind==='normal'){
    t='Switch to Normal mode?';p='SpotiOS stops being a Spotify Connect speaker in the background and opens as a regular player. You can turn Server Mode back on in Settings.';
    body=sheetRow('data-m="normal-yes"','phone','Switch to Normal mode','')+sheetRow('data-m="cancel"','x','Keep Server Mode','');
  }else if(kind==='def'){
    t='Default device';p='Pick this phone’s own Spotify. Whenever it plays, SpotiOS takes the music over.';
    body='<div class="ss-msg">Looking for devices…</div>';
  }else if(kind==='fx'){
    t='Sound';p='How SpotiOS sounds on this phone.';
    body=fxBody();
  }else{
    t='Play on';p='Move the music to another device, or bring it to SpotiOS.';
    body='<div class="ss-msg">Looking for devices…</div>';
  }
  sh.innerHTML='<div class="ss-grab"></div><h4>'+t+'</h4><p>'+p+'</p><div class="ss-sb" id="ss-sb">'+body+'</div>';
  sh.classList.add('open');byId('ss-scrim').classList.add('open');
  if(kind==='def'||kind==='play'){
    var fill=function(){if(sheetKind===kind)fillSheet();};
    var c=W.__spoConn;
    if(c&&c.clusterAt&&now()-c.clusterAt<20000){fill();return;}
    csCall(function(x){return x.cluster();}).then(fill).catch(function(){
      return api('/me/player/devices').then(function(j){
        S.devices=((j&&j.devices)||[]).map(function(d){return {id:d.id,name:d.name,type:d.type};});
        S.devices.forEach(known);save();
      }).catch(function(){});
    }).then(fill);
  }
}
function fillSheet(){
  var sb=byId('ss-sb');if(!sb||!sheetKind)return;
  var h='';
  if(sheetKind==='def'){
    var rows=devRows();
    h+=sheetRow('data-s="@auto"','refresh','Find it by itself','Uses this phone’s own Spotify as soon as it shows up.',cfg.def&&!cfg.def.auto?'':'cur');
    rows.forEach(function(d){h+=sheetRow('data-s="'+esc(d.id)+'"',devIc(d.type),esc(d.name)+(phoneNamed(d)?' (this phone)':''),devLabel(d.type)+(d.on?' · online':''),isDef(d)&&!cfg.def.auto?'cur':'');});
    if(!rows.length)h+='<div class="ss-note" style="padding:10px 2px">Your devices show up here once Spotify is open on them.</div>';
  }else{
    var a=S.active,mine={id:myId(),name:myName(),type:'Smartphone'};
    var list=[mine].concat(S.devices.filter(function(d){return !isMine(d);}));
    list.forEach(function(d){
      var me=d===mine,cur=a&&(me?isMine(a):a.id===d.id);
      h+=sheetRow('data-p="'+esc(me?'@me':d.id)+'"',me?'phone':devIc(d.type),esc(me?'SpotiOS (this phone)':d.name),me?'Plays here':devLabel(d.type),cur?'cur':'');
    });
    if(list.length<2)h+='<div class="ss-note" style="padding:10px 2px">Other devices show up here when Spotify is open on them.</div>';
  }
  sb.innerHTML=h;
}
function closeSheet(){sheetKind='';var sh=byId('ss-sheet');if(sh)sh.classList.remove('open');var s=byId('ss-scrim');if(s)s.classList.remove('open');}
function onSheetClick(e){
  if(sheetKind==='fx'){fxClick(e);return;}
  var r=e.target.closest&&e.target.closest('.ss-row');if(!r)return;
  haptic();
  var m=r.getAttribute('data-m');
  if(m){
    if(m==='full'){openFull();return;}
    if(m==='play'){openSheet('play');return;}
    if(m==='fx'){openSheet('fx');return;}
    if(m==='out'){closeSheet();pickOutput();return;}
    if(m==='settings'){closeSheet();try{AndBridge.openSettings();}catch(x){}return;}
    if(m==='normal'){openSheet('normal');return;}
    if(m==='normal-yes'){closeSheet();toNormal();return;}
    if(m==='stop'){openSheet('stop');return;}
    if(m==='stop-yes'){stopServer();return;}
    closeSheet();return;
  }
  if(sheetKind==='def'){
    var sid=r.getAttribute('data-s');
    if(sid==='@auto'){cfg.def=null;save();autoDefault();if(!cfg.def)toast('Open Spotify on this phone and press play');}
    else{var e2=cfg.devs[sid];if(e2){e2.allow=true;S.hold=null;setDef({id:sid,name:e2.name,type:e2.type},false,'you picked it');}}
    save();closeSheet();renderDef();renderWho();renderSet();autoConnect();return;
  }
  var pid=r.getAttribute('data-p'),id=pid==='@me'?myId():pid;
  if(!id){toast('SpotiOS is still joining Spotify Connect');return;}
  if(pid!=='@me'&&cfg.auto&&cfg.def&&cfg.def.id===pid){toast('SpotiOS takes the music over from '+cfg.def.name+'. Turn off Take over to play there.');return;}
  S.localUntil=now()+10000;
  if(pid!=='@me')S.hold={id:id,at:now(),idle:0};else S.hold=null;
  var rs=byId('ss-sb').querySelectorAll('.ss-row');for(var k=0;k<rs.length;k++)rs[k].classList.toggle('cur',rs[k]===r);
  retry(function(){return transferTo(id,'resume');},3).then(function(){closeSheet();setTimeout(function(){poll(true);},1500);})
    .catch(function(err2){toast(why(err2));fillSheet();});
}
/* Stop server: pause (so Spotify shows it stopped), then SpotiOS leaves Connect and closes. */
function stopServer(){
  closeSheet();
  if(W.__spoStopping)return;
  W.__spoStopping=true;clearTimeout(acT);
  log('Server stopped','');
  var el=byId('spoSrv');if(el)el.classList.add('ss-stopping');
  toast('Stopping the server…');
  var was=!!W.playing;
  try{if(was&&W.actPlayPause)W.actPlayPause(false);}catch(e){}
  setTimeout(function(){try{AndBridge.stopServer();}catch(e){}},was?700:250);
}
function toNormal(){
  try{AndBridge.setServerMode(false);}catch(e){}
  W.spoSetServer(false);
  toast('Normal mode: SpotiOS is a regular player again');
}
/* Download button: the app's own download (offline/DownloadManager), same as the player's. */
function download(){
  if(!W.track){toast('Play a song first');return;}
  if(typeof W.splDoDownload!=='function'){toast('Downloads aren’t ready yet. Try again in a moment.');return;}
  try{W.splDoDownload();toast('Downloading “'+W.track+'”');}catch(e){toast('Couldn’t start the download');}
}
var toastT=null;
function toast(m){var t=byId('ss-toast');if(!t)return;t.textContent=m;t.classList.add('show');clearTimeout(toastT);toastT=setTimeout(function(){t.classList.remove('show');},2800);}

/* ---------- input ---------- */
function bind(el){
  el.addEventListener('click',function(e){
    var t=e.target;
    var b=t.closest&&t.closest('[data-a],#ss-play,#ss-prev,#ss-next,#ss-fx,#ss-dl,#ss-on,#ss-out,#ss-menu,#ss-stop,.ss-it,#ss-chips button,#ss-qx');
    if(!b)return;
    if(b.id==='ss-play'){haptic();try{W.actPlayPause();}catch(x){}setTimeout(renderNP,250);return;}
    if(b.id==='ss-prev'){haptic();try{W.actSkipBack();}catch(x){}return;}
    if(b.id==='ss-next'){haptic();try{W.actSkipForward();}catch(x){}return;}
    if(b.id==='ss-on'){haptic();openSheet('play');return;}
    if(b.id==='ss-out'){haptic();pickOutput();return;}
    if(b.id==='ss-fx'){haptic();openSheet('fx');return;}
    if(b.id==='ss-dl'){haptic();download();return;}
    if(b.id==='ss-menu'){haptic();openSheet('menu');return;}
    if(b.id==='ss-stop'){haptic();openSheet('stop');return;}
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
    clearTimeout(lib.qt);lib.qt=setTimeout(function(){lib.q=v;loadList();},400);
  });
  q.addEventListener('keydown',function(e){if(e.key==='Enter'){q.blur();clearTimeout(lib.qt);lib.q=q.value.trim();loadList();}});
}
function action(a,b){
  haptic();
  if(a==='fold'){var c=byId('ss-how');c.classList.toggle('open');return;}
  if(a==='fold-lib'){var lc=byId('ss-lib');cfg.libFold=lc.classList.contains('open');lc.classList.toggle('open',!cfg.libFold);save();return;}
  if(a==='spotify'){try{AndBridge.openSpotifyApp();}catch(e){location.href='spotify:';}return;}
  if(a==='reconnect'){
    var c2=W.__spoConn,ws=c2&&c2.ws;
    if(ws&&ws.readyState===1){try{ws.close(4002,'user');}catch(e){}c2.forced++;toast('Reconnecting to Spotify…');setTimeout(function(){poll(true);},4000);}
    else{toast('Restarting the player…');setTimeout(function(){location.reload();},300);}
    log('Reconnected by hand','');setTimeout(renderConn,500);return;
  }
  if(a==='settings'){try{AndBridge.openSettings();}catch(e){}return;}
  if(a==='auto'){cfg.auto=!cfg.auto;save();if(cfg.auto)S.hold=null;renderDef();renderWho();renderSet();if(cfg.auto)autoConnect();return;}
  if(a==='boot'){var ob=info.onBoot===false;try{AndBridge.setStartOnBoot(ob);}catch(e){}info.onBoot=ob;renderSet();
    toast(ob?'The server starts when the phone starts':'The server no longer starts with the phone');return;}
  if(a==='pauseclose'){cfg.pauseClose=!cfg.pauseClose;save();renderSet();
    if(cfg.pauseClose&&!info.notifAccess)toast('Allow notification access so SpotiOS can notice Spotify closing');return;}
  if(a==='net-check'){toast('Checking the connection…');netTick(true);return;}
  if(a==='net-app'){try{AndBridge.openBlockerApp(b.getAttribute('data-n')||'');}catch(e){}return;}
  if(a==='net-vpn'){try{AndBridge.openVpnSettings();}catch(e){}return;}
  if(a==='net-dns'){try{AndBridge.openPrivateDnsSettings();}catch(e){}return;}
  if(a==='stop'){openSheet('stop');return;}
  if(a==='pickdef'){openSheet('def');return;}
  if(a==='pol-all'||a==='pol-list'){cfg.policy=a==='pol-list'?'list':'all';save();renderWho();
    toast(cfg.policy==='list'?'Only allowed devices can play here':'Every device on your account can play here');return;}
  if(a==='allow'){
    var id=b.getAttribute('data-id'),e=cfg.devs[id];if(!e)return;
    if(cfg.auto&&cfg.def&&cfg.def.id===id){toast(cfg.def.name+' is always allowed while SpotiOS takes its music over');return;}
    var nowOk=!allowed({id:id,name:e.name,type:e.type});
    e.allow=nowOk;save();renderWho();
    log((nowOk?'Allowed ':'Blocked ')+e.name,nowOk?'good':'bad');
    if(!nowOk&&S.active&&isMine(S.active)&&S.playing)toast(e.name+' is blocked from now on');
    return;
  }
  if(a==='withsp'){var on=!info.withSpotify;try{AndBridge.setStartWithSpotify(on);}catch(e){}info.withSpotify=on;renderSet();
    if(on)toast(info.notifAccess?'SpotiOS starts when Spotify plays':'Allow notification access so SpotiOS can notice Spotify');return;}
  if(a==='notif'){try{AndBridge.openNotificationAccess();}catch(e){}return;}
  if(a==='battery'){try{AndBridge.openBatterySettings();}catch(e){}return;}
  if(a==='full'){openFull();return;}
  if(a==='normal'){openSheet('normal');return;}
  if(a==='relist'){loadList(true,true);return;}
}

/* refresh battery and permission rows when coming back from Android settings */
document.addEventListener('visibilitychange',function(){if(!realHidden()&&shown){info=status();renderSet();renderConn();netTick(true);outAt=0;outTick();}});

/* ---------- back button: sheet, then full Spotify back to the Server screen ---------- */
var prevBack=W.spoBack;
W.spoBack=function(){
  if(sheetKind){closeSheet();return true;}
  if(want()){try{AndBridge.moveToBack();return true;}catch(e){return false;}}
  if(W.__spoServer&&full){
    if(prevBack&&prevBack())return true;
    backToServer();return true;
  }
  return prevBack?prevBack():false;
};

W.spoSetServer=function(on){
  W.__spoServer=!!on;W.__spoKeepVisible=!!on;full=false;
  if(on){log('Server Mode on','good');S.pollAt=0;}
  sync();
};
W.spoServerOpen=function(){backToServer();};
W.spoServerShown=function(){return shown;};
sync();
/* the tab bar can be built after us */
setTimeout(syncReturn,1500);setTimeout(syncReturn,4000);
})();
"""
}
