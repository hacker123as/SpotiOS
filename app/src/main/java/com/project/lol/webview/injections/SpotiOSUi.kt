package com.project.lol.webview.injections

import org.json.JSONObject

/**
 * SpotiOS shell: Liquid Glass theme, docked mini player, full-screen Now Playing
 * sheet, floating tab bar, long-press quick actions and native share.
 * Builds on the SpotilolPlayer DOM (#spotilolPlayerControls) and only proxies
 * to its buttons, so playback logic stays in one place.
 */
object SpotiOSUi {

    fun content(tabBar: Boolean, artWallpaper: Boolean = true, lrcLib: Boolean = true): String =
        "window.__spoTabs=$tabBar;window.__spoArtWall=$artWallpaper;window.__spoLrc=$lrcLib;\n" +
            JS.replace("__SPO_CSS__", JSONObject.quote(CSS))

    private const val CSS = """
:root{
  --spo-accent:var(--spl-accent,#1ed760);
  --spo-safe-b:env(safe-area-inset-bottom,0px);
  --spo-safe-t:env(safe-area-inset-top,0px);
  --spo-gap:10px;
  --spo-tab-h:64px;
  --spo-mini-bottom:calc(var(--spo-safe-b) + var(--spo-gap));
  --spo-dock:calc(var(--spo-mini-bottom) + 96px);
  --spo-blur:28px;
  --spo-spring:cubic-bezier(.34,1.56,.64,1);
  --spo-ease:cubic-bezier(.22,1,.36,1);
  --spo-sheet:cubic-bezier(.32,.72,0,1);
  --spo-ink-2:rgba(235,235,245,.64);
  --spo-ink-3:rgba(235,235,245,.34);
  --spo-rim:inset 0 1px 0 rgba(255,255,255,.22),inset 0 -1px 0 rgba(255,255,255,.05),inset 1px 0 0 rgba(255,255,255,.06),inset -1px 0 0 rgba(255,255,255,.06);
  --spo-drop:0 18px 50px rgba(0,0,0,.55),0 4px 14px rgba(0,0,0,.3);
}
html.spo-tabs{--spo-mini-bottom:calc(var(--spo-safe-b) + var(--spo-gap) + var(--spo-tab-h) + 8px)}

/* ---------- Spotify colour system ---------- */
.encore-dark-theme,.encore-dark-theme .encore-base-set{
  --background-base:#08090c!important;
  --background-highlight:rgba(255,255,255,.075)!important;
  --background-press:rgba(255,255,255,.035)!important;
  --background-elevated-base:#1b1c21!important;
  --background-elevated-highlight:#24252b!important;
  --background-elevated-press:#15161a!important;
  --background-tinted-base:rgba(255,255,255,.075)!important;
  --background-tinted-highlight:rgba(255,255,255,.12)!important;
  --background-tinted-press:rgba(255,255,255,.05)!important;
  --text-subdued:rgba(235,235,245,.64)!important;
  --essential-subdued:rgba(235,235,245,.32)!important;
  --decorative-subdued:rgba(255,255,255,.09)!important;
}

/* ---------- wallpaper ---------- */
html.spo{background:#040507!important}
html.spo body{background:transparent!important;-webkit-tap-highlight-color:transparent;-webkit-font-smoothing:antialiased}
html.spo body::before{
  content:"";position:fixed;inset:-25vmax;z-index:-2;pointer-events:none;
  background:
    radial-gradient(38vmax 38vmax at 18% 12%,rgba(30,215,96,.20),transparent 70%),
    radial-gradient(44vmax 44vmax at 88% 18%,rgba(94,92,230,.22),transparent 70%),
    radial-gradient(40vmax 40vmax at 30% 78%,rgba(100,210,255,.09),transparent 70%),
    radial-gradient(48vmax 48vmax at 78% 96%,rgba(255,55,95,.12),transparent 70%);
}
@keyframes spoAurora{
  0%{transform:translate3d(0,0,0) rotate(0deg) scale(1)}
  50%{transform:translate3d(-5vmax,3vmax,0) rotate(8deg) scale(1.08)}
  100%{transform:translate3d(3vmax,-3vmax,0) rotate(-5deg) scale(1.05)}
}
/* album-art wallpaper: the playing cover, heavily blurred, behind the aurora's colours */
html.spo-artwall body::after{
  content:"";position:fixed;left:50%;top:50%;width:40vmax;height:40vmax;margin:-20vmax 0 0 -20vmax;z-index:-1;pointer-events:none;
  background:var(--spo-art,none) center/cover no-repeat;
  filter:blur(18px) saturate(165%) brightness(.42);opacity:.9;transform:scale(3.4);
}
html.spo-lite.spo-artwall body::after,html.spo-artwall:has(#spotilol-amoled-theme) body::after{display:none!important}
html.spo #main,html.spo .Root,html.spo .Root__top-container{background:transparent!important}
/* the grey bar along the bottom was the list's horizontal scrollbar; lists never scroll sideways */
html.spo .os-scrollbar-horizontal{display:none!important}
/* no 300ms double-tap-zoom wait: taps on songs and buttons fire right away */
html.spo,html.spo body,html.spo #main,html.spo [data-testid=tracklist-row],html.spo button,html.spo a,html.spo [role=button]{touch-action:manipulation}
/* download progress: a glass pill that sits above the mini player, not on it */
html.spo #spl-dl-progress[style*="position:fixed"],html.spo #spl-dl-progress[style*="position: fixed"]{
  bottom:calc(var(--spo-mini-bottom) + 80px)!important;width:min(560px,calc(100vw - 28px))!important;
  background:rgba(30,30,36,.78)!important;border:0!important;border-radius:16px!important;
  backdrop-filter:blur(24px) saturate(180%)!important;-webkit-backdrop-filter:blur(24px) saturate(180%)!important;
  box-shadow:var(--spo-rim),0 10px 30px rgba(0,0,0,.45)!important;padding:9px 14px!important;
}
html.spo-np-open #spl-dl-progress{opacity:0!important}
/* the "# Title (clock)" column header stuck over the first song on phones; it has no use there */
html.spo :is([data-testid=track-list],[data-testid=playlist-tracklist])>div:first-child:has([role=columnheader]){display:none!important}
html.spl-libopen #Desktop_LeftSidebar_Id header>div>div:first-child h1{font-size:0!important}
html.spl-libopen #Desktop_LeftSidebar_Id header>div>div:first-child h1>*{display:none!important}
html.spl-libopen #Desktop_LeftSidebar_Id header>div>div:first-child h1::after{content:"\2716\00a0\00a0Close Library";font-size:16px;font-weight:700}
html.spo .Root__main-view{background:linear-gradient(180deg,rgba(255,255,255,.035),rgba(255,255,255,0) 260px)!important;border-radius:26px 26px 0 0!important}
html.spo :is(.main-view-container,.main-view-container__scroll-node,[data-testid=main-view-container]){background:transparent!important}
html.spo :is(#main-view,.Root__main-view) main{padding-bottom:var(--spo-dock)!important}
html.spo .Root__main-view::after{
  content:"";position:fixed;left:0;right:0;bottom:0;height:calc(var(--spo-dock) + 24px);z-index:50;pointer-events:none;
  background:linear-gradient(180deg,rgba(4,5,7,0),rgba(4,5,7,.55) 45%,rgba(4,5,7,.92));
}

/* ---------- typography ---------- */
html.spo #main :is(h1,h2){letter-spacing:-.025em!important}
html.spo #main section h2{font-weight:800!important}
html.spo #main ::selection{background:rgba(30,215,96,.32)}

/* ---------- top nav: floating glass buttons ---------- */
html.spo #global-nav-bar{background:transparent!important;box-shadow:none!important}
html.spo #global-nav-bar :is(button,a)[aria-label]:not([data-encore-id=buttonPrimary]):not(form *):not(:has(button)){
  background:rgba(40,40,48,.46)!important;backdrop-filter:blur(22px) saturate(190%)!important;-webkit-backdrop-filter:blur(22px) saturate(190%)!important;
  box-shadow:var(--spo-rim),0 6px 18px rgba(0,0,0,.28)!important;border:0!important;border-radius:999px!important;color:#fff!important;
  transition:transform .34s var(--spo-spring),background .18s!important;
}
html.spo #global-nav-bar :is(button,a)[aria-label]:active{transform:scale(.88)!important}
html.spo :is(input[data-testid=search-input],form[role=search] input){
  background:rgba(40,40,48,.46)!important;backdrop-filter:blur(22px) saturate(190%)!important;-webkit-backdrop-filter:blur(22px) saturate(190%)!important;
  border:0!important;border-radius:999px!important;box-shadow:var(--spo-rim),0 6px 18px rgba(0,0,0,.25)!important;color:#fff!important;caret-color:var(--spo-accent)!important;
}
html.spo :is(input[data-testid=search-input],form[role=search] input):focus{box-shadow:var(--spo-rim),0 0 0 3px rgba(30,215,96,.18)!important;outline:none!important}

/* ---------- chips ---------- */
html.spo button:has(>[data-encore-id=chip]){background:transparent!important;box-shadow:none!important;border:0!important;transition:transform .34s var(--spo-spring)!important}
html.spo button:has(>[data-encore-id=chip]):active{transform:scale(.92)!important}
html.spo [data-encore-id=chip]{background:rgba(255,255,255,.08)!important;box-shadow:var(--spo-rim)!important;border:0!important;border-radius:999px!important;color:#fff!important;font-weight:600!important;transition:background .18s,color .18s!important}
html.spo :is([aria-checked=true],[aria-pressed=true])>[data-encore-id=chip]{background:#fff!important;color:#000!important;box-shadow:0 6px 20px rgba(255,255,255,.14)!important}

/* ---------- cards, rows, buttons ---------- */
html.spo [data-encore-id=card]{border-radius:20px!important;transition:transform .34s var(--spo-spring),background .18s!important}
html.spo [data-encore-id=card]:active{transform:scale(.955)!important;transition-duration:.1s!important}
html.spo [data-encore-id=card]:not(:has(a[href*="/artist/"])) :is(div,button):has(>img){border-radius:16px!important;overflow:hidden!important;box-shadow:0 12px 28px rgba(0,0,0,.42)!important}
html.spo [data-encore-id=card]:not(:has(a[href*="/artist/"])) img{border-radius:16px!important}
html.spo [data-encore-id=card] [data-encore-id=buttonPrimary]>span{box-shadow:0 8px 22px rgba(30,215,96,.38),inset 0 1px 0 rgba(255,255,255,.35)!important}
html.spo section a[href*="/section/"]:not(:has(img)):not(:is(h1,h2,h3,h4) *):not(:is(h1,h2,h3,h4)){
  display:inline-flex!important;align-items:center!important;padding:6px 12px!important;border-radius:999px!important;background:rgba(255,255,255,.08)!important;
  box-shadow:var(--spo-rim)!important;color:var(--spo-ink-2)!important;font-size:12.5px!important;font-weight:700!important;white-space:nowrap!important;text-decoration:none!important;
}
html.spo :is([data-testid=entity-image],[data-testid=playlist-image],[data-testid=album-image]){border-radius:20px!important;overflow:hidden!important;box-shadow:0 28px 70px rgba(0,0,0,.6),0 8px 20px rgba(0,0,0,.35)!important}
html.spo [data-testid=play-button]{transition:transform .34s var(--spo-spring)!important}
html.spo [data-testid=play-button]>span:first-child{box-shadow:0 12px 32px rgba(30,215,96,.38),inset 0 1px 0 rgba(255,255,255,.42),inset 0 -3px 8px rgba(0,0,0,.14)!important}
html.spo [data-testid=play-button]:active{transform:scale(.9)!important}
html.spo [data-testid=tracklist-row]{border-radius:12px!important;transition:background .18s,transform .34s var(--spo-spring)!important}
html.spo [data-testid=tracklist-row]:active{transform:scale(.982)!important;background:rgba(255,255,255,.07)!important}
html.spo [data-testid=tracklist-row] img{border-radius:8px!important}
html.spo [role=row][aria-selected=true] [data-testid=tracklist-row]{background:linear-gradient(90deg,rgba(30,215,96,.16),rgba(255,255,255,.04))!important}
html.spo :is([data-encore-id=buttonPrimary],[data-encore-id=buttonSecondary],[data-encore-id=buttonTertiary]){transition:transform .34s var(--spo-spring)!important}
html.spo :is([data-encore-id=buttonPrimary],[data-encore-id=buttonSecondary],[data-encore-id=buttonTertiary]):active{transform:scale(.93)!important}
html.spo [data-encore-id=listRow]{border-radius:12px!important;transition:background .18s,transform .34s var(--spo-spring)!important}
html.spo [data-encore-id=listRow]:active{transform:scale(.98)!important}
html.spo a[href*="/genre/"]{border-radius:20px!important;overflow:hidden!important;transition:transform .34s var(--spo-spring)!important}
html.spo a[href*="/genre/"]:active{transform:scale(.95)!important}
html.spo :is([data-testid=tracklist-row],[data-encore-id=card],[data-encore-id=listRow]){-webkit-touch-callout:none;-webkit-user-select:none;user-select:none}
html.spo .YourLibraryX{background:rgba(14,14,18,.86)!important;backdrop-filter:blur(30px) saturate(180%)!important;-webkit-backdrop-filter:blur(30px) saturate(180%)!important}

/* ---------- menus, dialogs, lyrics ---------- */
html.spo [data-tippy-root] :is(.tippy-box,.tippy-content){background:transparent!important;box-shadow:none!important;border:0!important}
html.spo [data-tippy-root] [role=menu]{
  background:rgba(32,32,38,.7)!important;backdrop-filter:blur(40px) saturate(200%)!important;-webkit-backdrop-filter:blur(40px) saturate(200%)!important;
  border:0!important;border-radius:24px!important;box-shadow:var(--spo-rim),var(--spo-drop)!important;padding:6px!important;
}
html.spo [data-tippy-root] [role=menuitem]{min-height:46px!important;border-radius:14px!important;padding-inline:14px!important}
html.spo [data-tippy-root] [role=menuitem]:is(:hover,:focus){background:rgba(255,255,255,.09)!important}
html.spo [data-tippy-root] [role=menuitem]:active{background:rgba(255,255,255,.16)!important}
html.spo [data-tippy-root] [role=menu] :is(hr,[role=separator]){border:0!important;height:1px!important;background:rgba(255,255,255,.08)!important;margin:4px 12px!important}
@media (max-width:820px){
  html.spo [data-tippy-root]:has([role=menu]){position:fixed!important;inset:auto 8px calc(var(--spo-safe-b) + 8px) 8px!important;transform:none!important;width:auto!important;max-width:none!important;margin:0!important}
  html.spo [data-tippy-root]:has([role=menu]) .tippy-box{width:100%!important;max-width:560px!important;margin:0 auto!important;transform:none!important}
  html.spo [data-tippy-root] [role=menu]{width:100%!important;min-width:0!important;max-width:none!important;max-height:min(72vh,600px)!important;overflow-y:auto!important;animation:spoUp .4s var(--spo-sheet) both}
}
@keyframes spoUp{from{transform:translate3d(0,40px,0);opacity:0}to{transform:none;opacity:1}}
#spoScrim{position:fixed;inset:0;z-index:2147483600;background:rgba(0,0,0,.45);opacity:0;pointer-events:none;transition:opacity .25s}
html.spo-menu #spoScrim{opacity:1}
html.spo .ReactModal__Overlay{background:rgba(0,0,0,.42)!important;backdrop-filter:blur(14px)!important;-webkit-backdrop-filter:blur(14px)!important}
html.spo .ReactModal__Content{background:rgba(30,30,36,.78)!important;backdrop-filter:blur(40px) saturate(200%)!important;-webkit-backdrop-filter:blur(40px) saturate(200%)!important;border:0!important;border-radius:28px!important;box-shadow:var(--spo-rim),var(--spo-drop)!important;overflow:hidden!important;max-width:calc(100vw - 20px)!important}
html.spo [data-testid=lyrics-container],html.spo :has(>[data-testid=fullscreen-lyric]){--lyrics-color-active:#fff!important;--lyrics-color-inactive:rgba(255,255,255,.34)!important;--lyrics-color-passed:rgba(255,255,255,.58)!important}
html.spo [data-testid=fullscreen-lyric]{font-size:clamp(24px,7vw,36px)!important;font-weight:800!important;line-height:1.22!important;letter-spacing:-.022em!important}
html.spo [data-testid=lyrics-container]{padding-bottom:var(--spo-dock)!important}
html.spo #main *::-webkit-scrollbar{width:0!important;height:0!important}
html.spo .Root__main-view main>*{animation:spoFade .4s var(--spo-ease) both}
@keyframes spoFade{from{opacity:0}to{opacity:1}}

/* ---------- mini player (docked glass capsule) ---------- */
html.spo #spotilolPlayerControls{
  left:10px!important;right:10px!important;bottom:var(--spo-mini-bottom)!important;max-width:min(640px,calc(100vw - 20px))!important;
  padding:8px 8px 13px!important;border:0!important;border-radius:24px!important;overflow:hidden!important;
  background:linear-gradient(180deg,rgba(54,54,62,.56),rgba(24,24,30,.7))!important;
  backdrop-filter:blur(var(--spo-blur)) saturate(190%) brightness(1.04)!important;-webkit-backdrop-filter:blur(var(--spo-blur)) saturate(190%) brightness(1.04)!important;
  box-shadow:var(--spo-rim),var(--spo-drop)!important;
  font-family:-apple-system,system-ui,"Segoe UI",Roboto,sans-serif!important;
  transition:transform .45s var(--spo-sheet),opacity .3s,bottom .45s var(--spo-sheet)!important;
}
html.spo #spotilolPlayerControls::after{
  content:"";position:absolute;inset:0;border-radius:inherit;padding:1px;pointer-events:none;
  background:linear-gradient(135deg,rgba(255,255,255,.5),rgba(255,255,255,.07) 32%,rgba(255,255,255,0) 58%,rgba(255,255,255,.22));
  -webkit-mask:linear-gradient(#000 0 0) content-box,linear-gradient(#000 0 0);-webkit-mask-composite:xor;
  mask:linear-gradient(#000 0 0) content-box exclude,linear-gradient(#000 0 0);
}
html.spo #spotilolPlayerControls .spl-top{gap:11px!important}
html.spo #spotilolPlayerControls .spl-cover img{width:46px!important;height:46px!important;border-radius:12px!important;-webkit-mask-image:none!important;mask-image:none!important;box-shadow:0 6px 16px rgba(0,0,0,.45)}
html.spo #spotilolPlayerControls .spl-track{font-size:14.5px!important;font-weight:650!important;letter-spacing:-.01em}
html.spo #spotilolPlayerControls .spl-artist{font-size:12.5px!important;color:var(--spo-ink-2)!important}
html.spo #spotilolPlayerControls .spl-btn{color:#fff!important;transition:transform .34s var(--spo-spring),background .18s!important}
html.spo #spotilolPlayerControls .spl-btn:active{transform:scale(.82)!important;background:rgba(255,255,255,.14)!important}
html.spo #spotilolPlayerControls .spl-mini-transport .spl-play{background:transparent!important}
html.spo #spotilolPlayerControls .spl-mini-transport svg{width:22px!important;height:22px!important}
html.spo #spotilolPlayerControls .spl-mini-transport .spl-play svg{width:26px!important;height:26px!important}
html.spo #spotilolPlayerControls .spl-liked-btn{color:var(--spo-ink-2)!important}
html.spo #spotilolPlayerControls .spl-liked-btn.spl-active{color:var(--spo-accent)!important}
html.spo #spotilolPlayerControls .spl-edgebar{left:18px!important;right:18px!important;bottom:6px!important;height:3px!important;border-radius:3px!important;background:rgba(255,255,255,.16)!important;overflow:hidden}
html.spo #spotilolPlayerControls .spl-edgebar .spl-fill{background:#fff!important;border-radius:3px}
html.spo-np-open #spotilolPlayerControls{opacity:0!important;pointer-events:none!important;transform:translate3d(0,30px,0)!important}

/* ---------- tab bar ---------- */
#spoTabs{
  position:fixed;z-index:2147483645;left:14px;right:14px;bottom:calc(var(--spo-safe-b) + var(--spo-gap));height:var(--spo-tab-h);max-width:520px;margin:0 auto;box-sizing:border-box;
  display:none;align-items:center;justify-content:space-around;padding:6px;border-radius:32px;
  background:linear-gradient(180deg,rgba(48,48,56,.52),rgba(20,20,26,.7));
  backdrop-filter:blur(30px) saturate(190%);-webkit-backdrop-filter:blur(30px) saturate(190%);
  box-shadow:var(--spo-rim),var(--spo-drop);
  transition:transform .45s var(--spo-sheet),opacity .3s;
}
html.spo-tabs #spoTabs{display:flex}
#spoTabs button{
  flex:1;height:100%;border:0;background:transparent;color:var(--spo-ink-2);display:flex;flex-direction:column;align-items:center;justify-content:center;gap:3px;
  border-radius:26px;font:600 10.5px/1 -apple-system,system-ui,"Segoe UI",Roboto,sans-serif;letter-spacing:.01em;padding:0;
  transition:color .2s,background .25s,transform .34s var(--spo-spring);-webkit-tap-highlight-color:transparent;
}
#spoTabs button svg{width:24px;height:24px}
#spoTabs button.on{color:var(--spo-accent);background:rgba(255,255,255,.1);box-shadow:inset 0 1px 0 rgba(255,255,255,.12)}
#spoTabs button:active{transform:scale(.88)}
html.spo-np-open #spoTabs,html.spo-menu #spoTabs{transform:translate3d(0,150%,0);opacity:0}

/* ---------- Now Playing sheet ---------- */
#spoNP{
  position:fixed;inset:0;z-index:2147483647;color:#fff;background:#0b0b0f;overflow:hidden;visibility:hidden;
  transform:translate3d(0,100%,0);border-radius:28px 28px 0 0;will-change:transform;
  font-family:-apple-system,system-ui,"Segoe UI",Roboto,sans-serif;
  transition:transform .52s var(--spo-sheet),border-radius .52s var(--spo-sheet),visibility 0s linear .52s;
}
#spoNP.open{transform:none;visibility:visible;border-radius:0;transition:transform .52s var(--spo-sheet),border-radius .52s var(--spo-sheet),visibility 0s}
#spoNP.dragging{transition:none}
#spoNP .spo-bg{position:absolute;inset:0;overflow:hidden;pointer-events:none}
#spoNP .spo-bg img{position:absolute;left:-25%;top:-25%;width:150%;height:150%;object-fit:cover;filter:blur(70px) saturate(170%) brightness(.62);opacity:.95}
#spoNP .spo-bg::after{content:"";position:absolute;inset:0;background:linear-gradient(180deg,rgba(0,0,0,.12),rgba(0,0,0,.32) 55%,rgba(0,0,0,.72))}
#spoNP .spo-in{
  position:relative;height:100%;box-sizing:border-box;display:flex;flex-direction:column;align-items:center;
  padding:calc(var(--spo-safe-t) + 8px) 24px calc(var(--spo-safe-b) + 16px);gap:clamp(6px,1.8vh,18px);overflow-y:auto;overscroll-behavior:contain;
}
#spoNP .spo-grab{width:38px;height:5px;border-radius:3px;background:rgba(255,255,255,.38);flex-shrink:0}
#spoNP .spo-head{width:100%;max-width:560px;display:flex;align-items:center;justify-content:space-between}
#spoNP .spo-head-t{font-size:12px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;color:rgba(255,255,255,.72)}
#spoNP .spo-ib{
  border:0;background:transparent;color:#fff;width:44px;height:44px;border-radius:50%;padding:0;flex-shrink:0;
  display:inline-flex;align-items:center;justify-content:center;-webkit-tap-highlight-color:transparent;
  transition:transform .34s var(--spo-spring),background .2s,color .2s,opacity .2s;
}
#spoNP .spo-ib svg{width:24px;height:24px}
#spoNP .spo-ib:active{transform:scale(.84);background:rgba(255,255,255,.14)}
#spoNP .spo-ib.on{color:var(--spo-accent)}
#spoNP .spo-ib.off{opacity:.3;pointer-events:none}
#spoNP .spo-art-wrap{width:min(86vw,46vh,520px);aspect-ratio:1/1;flex-shrink:0;margin:clamp(2px,1.6vh,22px) 0}
#spoNP #spo-art{width:100%;height:100%;object-fit:cover;border-radius:16px;background:rgba(255,255,255,.06);box-shadow:0 30px 70px rgba(0,0,0,.6),0 8px 22px rgba(0,0,0,.4);transition:transform .6s var(--spo-spring)}
#spoNP.paused #spo-art{transform:scale(.84)}
#spoNP .spo-ctl{width:100%;max-width:560px;display:flex;flex-direction:column;gap:clamp(8px,2.2vh,22px)}
#spoNP .spo-meta{display:flex;align-items:center;gap:12px}
#spoNP .spo-meta-t{flex:1;min-width:0}
#spoNP .spo-title{font-size:clamp(20px,5.6vw,26px);font-weight:700;letter-spacing:-.02em;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
#spoNP .spo-artist{font-size:clamp(15px,4.4vw,19px);color:rgba(255,255,255,.66);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-top:2px}
#spoNP #spo-like.on svg path{fill:currentColor}
#spoNP .spo-scrub{padding:10px 0 2px;touch-action:none;cursor:pointer}
#spoNP .spo-bar{height:6px;border-radius:3px;background:rgba(255,255,255,.22);overflow:hidden;transition:height .25s var(--spo-spring),border-radius .25s}
#spoNP .spo-scrub.drag .spo-bar{height:12px;border-radius:6px}
#spoNP .spo-fill{height:100%;width:100%;background:rgba(255,255,255,.92);transform-origin:left center;transform:scaleX(0)}
#spoNP .spo-times{display:flex;justify-content:space-between;margin-top:8px;font-size:12px;font-weight:600;color:rgba(255,255,255,.55);font-variant-numeric:tabular-nums}
#spoNP .spo-transport{display:flex;align-items:center;justify-content:space-between;padding:0 2px}
#spoNP .spo-md{width:64px;height:64px}
#spoNP .spo-md svg{width:38px;height:38px}
#spoNP .spo-lg{width:86px;height:86px}
#spoNP .spo-lg svg{width:54px;height:54px}
#spoNP .spo-sm{color:rgba(255,255,255,.7)}
#spoNP .spo-sm svg{width:22px;height:22px}
#spoNP .spo-vol{display:flex;align-items:center;gap:12px;color:rgba(255,255,255,.6)}
#spoNP .spo-vol svg{width:18px;height:18px;display:block}
#spoNP .spo-vtrack{flex:1;height:26px;display:flex;align-items:center;touch-action:none;cursor:pointer}
#spoNP .spo-vbar{width:100%;height:6px;border-radius:3px;background:rgba(255,255,255,.22);overflow:hidden;transition:height .25s var(--spo-spring)}
#spoNP .spo-vtrack.drag .spo-vbar{height:12px;border-radius:6px}
#spoNP .spo-vfill{height:100%;width:0;background:rgba(255,255,255,.92)}
#spoNP .spo-actions{display:flex;justify-content:space-between;padding-top:2px}
#spoNP .spo-actions .spo-ib{width:auto;height:auto;min-width:56px;flex-direction:column;gap:5px;padding:8px 6px;border-radius:16px;color:rgba(255,255,255,.75);font-size:11px;font-weight:600}
#spoNP .spo-actions .spo-ib.on{color:var(--spo-accent)}
#spoNP .spo-actions .spo-ib{flex:1 1 0;min-width:0}
#spoNP .spo-dev{display:none;align-self:center;max-width:100%;margin-top:-4px;padding:5px 12px;border-radius:999px;background:rgba(var(--spl-accent-rgb,30,215,96),.16);color:var(--spo-accent);font-size:12px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
#spoNP .spo-dev.show{display:block}
/* in-sheet lyrics (LRCLIB), Apple Music style */
#spoNP .spo-lyr{display:none;width:100%;max-width:560px;flex:1 1 0;min-height:0;overflow-y:auto;overscroll-behavior:contain;box-sizing:border-box;padding:14vh 2px 22vh;scrollbar-width:none;
  -webkit-mask-image:linear-gradient(180deg,transparent 0,#000 14%,#000 78%,transparent 100%);mask-image:linear-gradient(180deg,transparent 0,#000 14%,#000 78%,transparent 100%)}
#spoNP .spo-lyr::-webkit-scrollbar{display:none}
#spoNP.lyr .spo-lyr{display:block}
#spoNP.lyr .spo-art-wrap{display:none}
#spoNP .spo-lyr p{margin:0 0 .6em;font-size:clamp(22px,6.6vw,32px);font-weight:800;line-height:1.2;letter-spacing:-.02em;color:rgba(255,255,255,.32);transition:color .35s,transform .35s var(--spo-ease);transform-origin:left center;cursor:pointer}
#spoNP .spo-lyr p.past{color:rgba(255,255,255,.55)}
#spoNP .spo-lyr p.on{color:#fff;transform:scale(1.03)}
#spoNP .spo-lyr.plain p{color:rgba(255,255,255,.88);font-size:clamp(19px,5.4vw,24px);cursor:default}
#spoNP .spo-lyr .spo-lyr-msg{margin-top:12vh;text-align:center;color:rgba(255,255,255,.62);font-size:16px;font-weight:600;line-height:1.5}
#spoNP .spo-lyr .spo-lyr-msg button{margin-top:14px;border:0;border-radius:999px;padding:10px 18px;background:rgba(255,255,255,.14);color:#fff;font-weight:700;font-size:14px}
#spoNP .spo-lyr .spo-lyr-src{font-size:11px;font-weight:600;color:rgba(255,255,255,.38);margin-top:2.4em}
html.spo-lite #spoNP .spo-lyr p{transition:none}
/* shorter phones: shrink art and transport so the action row fits without scrolling */
@media (orientation:portrait) and (max-height:820px){
  #spoNP .spo-in{gap:clamp(4px,1.2vh,12px)}
  #spoNP .spo-art-wrap{width:min(80vw,38vh,520px);margin:clamp(2px,1vh,12px) 0}
  #spoNP .spo-ctl{gap:clamp(6px,1.6vh,16px)}
  #spoNP .spo-md{width:54px;height:54px}
  #spoNP .spo-md svg{width:32px;height:32px}
  #spoNP .spo-lg{width:72px;height:72px}
  #spoNP .spo-lg svg{width:46px;height:46px}
  #spoNP .spo-actions .spo-ib{padding:6px 4px}
}
@media (orientation:landscape) and (min-aspect-ratio:5/4){
  #spoNP .spo-in{display:grid;grid-template-columns:auto minmax(0,1fr);grid-template-rows:auto auto 1fr;column-gap:5vw;row-gap:8px;align-items:center;justify-items:center;padding-left:6vw;padding-right:6vw}
  #spoNP .spo-grab,#spoNP .spo-head{grid-column:1/-1}
  #spoNP .spo-head{max-width:none}
  #spoNP .spo-art-wrap{grid-column:1;grid-row:3;width:min(40vw,64vh);margin:0}
  #spoNP .spo-ctl{grid-column:2;grid-row:3;max-width:620px}
}
@media (min-width:900px) and (orientation:portrait){
  #spoNP .spo-art-wrap{width:min(70vw,46vh,620px)}
  #spoNP .spo-ctl{max-width:640px}
}

/* ---------- low power ---------- */
html.spo-lite *,html.spo-lite *::before,html.spo-lite *::after{backdrop-filter:none!important;-webkit-backdrop-filter:none!important;animation:none!important}
html.spo-lite body::before{display:none!important}
html.spo-lite #spotilolPlayerControls,html.spo-lite #spoTabs{background:rgba(26,26,31,.97)!important}
html.spo-lite #spoNP .spo-bg img{filter:blur(28px) brightness(.5)}
html.spo-lite .Root__main-view::after{display:none!important}
@media (prefers-reduced-motion:reduce){
  html.spo body::before{animation:none!important}
  #spoNP,#spoTabs,html.spo #spotilolPlayerControls{transition-duration:.01s!important}
}
"""

    private const val JS = """
(function(){
if(window.__spoBooted) return;
window.__spoBooted=true;
var CSS=__SPO_CSS__;
var SV='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">';
var I={
  home:SV+'<path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/></svg>',
  search:SV+'<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/></svg>',
  lib:SV+'<path d="M5 4v16M10 4v16M15 4.5l4.5 15"/></svg>',
  gear:SV+'<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1.08-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09a1.65 1.65 0 0 0 1.51-1.08 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9c.26.6.85 1 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>',
  down:SV+'<path d="m6 9 6 6 6-6"/></svg>',
  share:SV+'<path d="M12 3v12M7.5 7.5 12 3l4.5 4.5"/><path d="M6 11H5a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-8a1 1 0 0 0-1-1h-1"/></svg>',
  heart:SV+'<path d="M12 20.5s-7.5-4.6-9.3-9.2C1.4 8 3.4 4.5 7 4.5c2.1 0 3.6 1.2 5 3 1.4-1.8 2.9-3 5-3 3.6 0 5.6 3.5 4.3 6.8-1.8 4.6-9.3 9.2-9.3 9.2z"/></svg>',
  play:'<svg viewBox="0 0 24 24"><path fill="currentColor" d="M7 4.8v14.4a1 1 0 0 0 1.5.86l12-7.2a1 1 0 0 0 0-1.72l-12-7.2A1 1 0 0 0 7 4.8z"/></svg>',
  pause:'<svg viewBox="0 0 24 24"><rect x="5.5" y="4" width="4.6" height="16" rx="1.4" fill="currentColor"/><rect x="13.9" y="4" width="4.6" height="16" rx="1.4" fill="currentColor"/></svg>',
  prev:'<svg viewBox="0 0 24 24"><path fill="currentColor" d="M11.5 6.2v11.6a.8.8 0 0 1-1.25.66L2.6 12.66a.8.8 0 0 1 0-1.32l7.65-5.8a.8.8 0 0 1 1.25.66zm10 0v11.6a.8.8 0 0 1-1.25.66l-7.65-5.8a.8.8 0 0 1 0-1.32l7.65-5.8a.8.8 0 0 1 1.25.66z"/></svg>',
  next:'<svg viewBox="0 0 24 24"><path fill="currentColor" d="M2.5 6.2v11.6a.8.8 0 0 0 1.25.66l7.65-5.8a.8.8 0 0 0 0-1.32L3.75 5.54a.8.8 0 0 0-1.25.66zm10 0v11.6a.8.8 0 0 0 1.25.66l7.65-5.8a.8.8 0 0 0 0-1.32l-7.65-5.8a.8.8 0 0 0-1.25.66z"/></svg>',
  shuffle:SV+'<path d="M16 3h5v5M4 20 21 3M21 16v5h-5M15 15l6 6M4 4l5 5"/></svg>',
  repeat:SV+'<path d="m17 2 4 4-4 4"/><path d="M3 11v-1a4 4 0 0 1 4-4h14"/><path d="m7 22-4-4 4-4"/><path d="M21 13v1a4 4 0 0 1-4 4H3"/></svg>',
  repeat1:SV+'<path d="m17 2 4 4-4 4"/><path d="M3 11v-1a4 4 0 0 1 4-4h14"/><path d="m7 22-4-4 4-4"/><path d="M21 13v1a4 4 0 0 1-4 4H3"/><path d="M11 10h1v4"/></svg>',
  lyrics:SV+'<path d="M21 12a8.5 8.5 0 0 1-12.6 7.45L3 21l1.55-5.4A8.5 8.5 0 1 1 21 12z"/><path d="M8.5 10.5h7M8.5 14h4.5"/></svg>',
  moon:SV+'<path d="M20.5 14.5A8.5 8.5 0 1 1 9.5 3.5a7 7 0 0 0 11 11z"/></svg>',
  queue:SV+'<path d="M3 6h13M3 12h13M3 18h8"/><path d="M16 15v6l5-3z" fill="currentColor"/></svg>',
  dl:SV+'<path d="M12 3v12M7 10.5l5 5 5-5M5 20h14"/></svg>',
  pip:SV+'<rect x="3" y="4" width="18" height="14" rx="2.5"/><rect x="12" y="10.5" width="6.5" height="5" rx="1" fill="currentColor"/></svg>',
  dev:SV+'<rect x="4" y="3" width="10" height="18" rx="2"/><circle cx="9" cy="16" r="1.6"/><path d="M17.5 8.5a5 5 0 0 1 0 7M20 6a8.5 8.5 0 0 1 0 12"/></svg>',
  volLo:SV+'<path d="M11 5 6 9H3v6h3l5 4z"/></svg>',
  volHi:SV+'<path d="M11 5 6 9H3v6h3l5 4z"/><path d="M15.5 8.5a5 5 0 0 1 0 7M18.5 5.5a9 9 0 0 1 0 13"/></svg>'
};
function qs(s,r){try{return (r||document).querySelector(s);}catch(e){return null;}}
function byId(i){return document.getElementById(i);}
function txt(el){return el&&el.textContent?el.textContent.trim():'';}
function haptic(){try{AndBridge.haptic();}catch(e){try{navigator.vibrate(8);}catch(e2){}}}
window.spoHaptic=haptic;
function secs(t){if(!t)return 0;var p=t.trim().split(':'),s=0;for(var i=0;i<p.length;i++){s=s*60+(parseInt(p[i],10)||0);}return s;}
function fmt(s){s=Math.max(0,Math.round(s));var m=Math.floor(s/60),r=s%60;return m+':'+(r<10?'0':'')+r;}
function hiRes(u){return u?u.replace(/ab67616d0000(4851|1e02)/,'ab67616d0000b273'):u;}
function root(){return document.documentElement;}

function addStyle(){
  if(byId('spo-style'))return;
  var s=document.createElement('style');s.id='spo-style';s.textContent=CSS;
  (document.head||root()).appendChild(s);
}

/* ---------- low power ---------- */
function applyLite(on){root().classList.toggle('spo-lite',!!on);}
applyLite(window.__splPowerSavePref);
var prevPs=window.__splApplyPowerSave;
window.__splApplyPowerSave=function(on){applyLite(on);if(typeof prevPs==='function')return prevPs.apply(this,arguments);};

/* ---------- tab bar ---------- */
function libOpen(){var ls=byId('Desktop_LeftSidebar_Id');return !!(ls&&ls.style.width==='100%');}
function toggleLib(){
  if(window.lBtn){window.lBtn.click();return;}
  var b=qs('#Desktop_LeftSidebar_Id header button');if(b)b.click();
}
function scrollTop(){
  var v=qs('.main-view-container__scroll-node [data-overlayscrollbars-viewport]')||qs('.main-view-container__scroll-node');
  if(v&&v.scrollTo)v.scrollTo({top:0,behavior:'smooth'});
}
function openSearch(){
  var a=qs('a[href="/search"]');if(a){a.click();return;}
  var b=qs('#global-nav-bar button[aria-label*="earch"]');if(b){b.click();}
  var i=qs('input[data-testid=search-input]');if(i){i.focus();i.click();}
}
function go(k){
  closeSheet();
  if(k==='settings'){try{AndBridge.openSettings();}catch(e){}return;}
  if(k==='library'){toggleLib();setTimeout(syncTabs,350);return;}
  if(libOpen())toggleLib();
  try{if(typeof closeNowPlay==='function')closeNowPlay();}catch(e){}
  var p=location.pathname;
  if(k==='home'){
    if(p==='/'){scrollTop();}
    else{var h=qs('[data-testid=home-button]')||qs('a[href="/"]');if(h)h.click();}
  }
  if(k==='search'){if(p.indexOf('/search')===0)scrollTop();else openSearch();}
  setTimeout(syncTabs,350);
}
function tabBtn(k,label,icon){return '<button type="button" data-tab="'+k+'" aria-label="'+label+'">'+icon+'<span>'+label+'</span></button>';}
function buildTabs(){
  if(byId('spoTabs')||!document.body)return;
  var t=document.createElement('nav');t.id='spoTabs';
  t.innerHTML=tabBtn('home','Home',I.home)+tabBtn('search','Search',I.search)+tabBtn('library','Library',I.lib)+tabBtn('settings','Settings',I.gear);
  t.addEventListener('click',function(e){var b=e.target.closest('button[data-tab]');if(!b)return;haptic();go(b.getAttribute('data-tab'));});
  document.body.appendChild(t);
  syncTabs();
}
function syncTabs(){
  var t=byId('spoTabs');if(!t)return;
  var p=location.pathname;
  var k=libOpen()?'library':(p.indexOf('/search')===0?'search':(p==='/'?'home':''));
  var bs=t.querySelectorAll('button');
  for(var i=0;i<bs.length;i++)bs[i].classList.toggle('on',bs[i].getAttribute('data-tab')===k);
}
window.spoSetTabs=function(on){
  window.__spoTabs=!!on;root().classList.toggle('spo-tabs',!!on);
  if(on)buildTabs();
};

/* ---------- mini player: always docked, tap opens the sheet ---------- */
function hookPlayer(){
  var pl=byId('spotilolPlayerControls');
  if(!pl||pl.__spo)return;
  pl.__spo=true;
  window.splMiniPref=true;
  pl.classList.add('spl-mini');
  new MutationObserver(function(){
    if(!pl.classList.contains('spl-mini')){pl.classList.add('spl-mini');window.splMiniPref=true;}
  }).observe(pl,{attributes:true,attributeFilter:['class']});
  var skip='button,#spl-bar,#spl-edgebar,.spl-vol-bar',ty=0,swiped=false;
  pl.addEventListener('click',function(e){
    if(swiped){swiped=false;return;}
    if(e.target.closest(skip))return;
    openSheet();
  });
  pl.addEventListener('touchstart',function(e){ty=e.touches[0].clientY;swiped=false;},{passive:true});
  pl.addEventListener('touchend',function(e){
    var t=e.changedTouches&&e.changedTouches[0];if(!t)return;
    if(ty-t.clientY>40&&!e.target.closest(skip)){swiped=true;openSheet();setTimeout(function(){swiped=false;},400);}
  },{passive:true});
}

/* ---------- Spotify Connect ---------- */
function devButton(){
  var b=qs('button[data-testid="control-button-connect"]')||qs('button[data-testid*="device-picker"]')||qs('button[data-testid*="connect-device"]');
  if(b)return b;
  var bs=document.querySelectorAll('aside[data-testid=now-playing-bar] button[aria-label],[data-testid=extra-controls] button[aria-label]');
  for(var i=0;i<bs.length;i++){
    var l=(bs[i].getAttribute('aria-label')||'').toLowerCase();
    if(/device|connect|appareil|dispositi|ger\u00e4t|urz\u0105dz|apparaat|\u0443\u0441\u0442\u0440\u043e\u0439/.test(l))return bs[i];
  }
  return null;
}
/* "Playing on <device>" from Spotify's own (hidden) bar, or '' when this phone plays */
function otherDevice(){
  var b=qs('aside[data-testid=now-playing-bar] .encore-bright-accent-set')||qs('[data-testid=now-playing-bar] [data-testid*="remote"]');
  return b?txt(b).replace(/\s+/g,' ').slice(0,80):'';
}
window.spoOpenDevices=function(){
  /* keep the auto-close helpers away from the panel while the picker is in use */
  window.__spoPanelHold=Date.now()+5*60*1000;
  var b=devButton();
  if(b){b.click();return;}
  var o=qs('aside[data-testid=now-playing-bar] .encore-bright-accent-set button');
  if(o)o.click();
};

/* ---------- lyrics (LRCLIB, open source) ---------- */
var lyr={key:'',lines:null,plain:null,state:'',req:0,cur:-1};
function trackKey(){var t=txt(byId('spl-track'));return t?t+'\u0001'+txt(byId('spl-artist')):'';}
function parseLrc(s){
  var out=[];
  String(s||'').split(/\r?\n/).forEach(function(l){
    var re=/\[(\d+):(\d+(?:\.\d+)?)\]/g,m,ts=[],last=0;
    while((m=re.exec(l))){ts.push(parseInt(m[1],10)*60+parseFloat(m[2]));last=re.lastIndex;}
    var t=l.slice(last).trim();
    ts.forEach(function(x){out.push({t:x,s:t});});
  });
  out.sort(function(a,b){return a.t-b.t;});
  return out;
}
function esc(t){return String(t).replace(/[&<>"]/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c];});}
function spotifyHasLyrics(){var ly=byId('spl-lyrics');return !!(ly&&ly.style.display!=='none'&&!ly.classList.contains('spl-disabled'));}
function lyrRender(){
  var box=byId('spo-lyr');if(!box)return;
  lyr.cur=-1;box.classList.remove('plain');box.scrollTop=0;
  var src='<div class="spo-lyr-src">Lyrics from LRCLIB</div>';
  if(lyr.state==='synced'){
    box.innerHTML=lyr.lines.map(function(l,i){return '<p data-i="'+i+'">'+(l.s?esc(l.s):'\u266a')+'</p>';}).join('')+src;
    lyrTick();return;
  }
  if(lyr.state==='plain'){
    box.classList.add('plain');
    box.innerHTML=lyr.plain.split(/\r?\n/).map(function(l){return '<p>'+(l.trim()?esc(l):'&nbsp;')+'</p>';}).join('')+src;return;
  }
  var msg=lyr.state==='loading'?'Finding lyrics\u2026':(lyr.state==='instrumental'?'\u266a Instrumental':(lyr.state==='idle'?'Play a song to see its lyrics':'No lyrics found for this song'));
  var more=(lyr.state==='none'&&spotifyHasLyrics())?'<br><button type="button" id="spo-lyr-sp">Open Spotify lyrics</button>':'';
  box.innerHTML='<div class="spo-lyr-msg">'+msg+more+'</div>';
  var sp=byId('spo-lyr-sp');if(sp)sp.onclick=function(){closeSheet();setTimeout(function(){act('spl-lyrics');},280);};
}
function lyrLoad(){
  var k=trackKey();
  if(k===lyr.key)return;
  lyr.key=k;lyr.lines=null;lyr.plain=null;
  if(!k){lyr.state='idle';lyrRender();return;}
  lyr.state='loading';lyrRender();
  var id=String(++lyr.req);
  var dur=secs(txt(qs('[data-testid="playback-duration"]')));
  try{AndBridge.lyricsLookup(id,txt(byId('spl-artist')),txt(byId('spl-track')),dur);}
  catch(e){lyr.state='none';lyrRender();}
}
window.spoLyricsResult=function(id,r){
  if(String(id)!==String(lyr.req))return;
  if(r&&r.synced){var ls=parseLrc(r.synced);if(ls.length)lyr.lines=ls;}
  if(r&&r.plain)lyr.plain=String(r.plain);
  lyr.state=lyr.lines?'synced':(lyr.plain?'plain':(r&&r.instrumental?'instrumental':'none'));
  lyrRender();
};
function lyrPos(){
  var dur=secs(txt(qs('[data-testid="playback-duration"]')));
  var pct=progressPct();
  if(pct!==null&&dur)return pct*dur;
  return secs(txt(qs('[data-testid="playback-position"]')));
}
function lyrTick(){
  if(lyr.state!=='synced'||!lyr.lines)return;
  var box=byId('spo-lyr'),t=lyrPos()+0.25,i=-1;
  for(var j=0;j<lyr.lines.length;j++){if(lyr.lines[j].t<=t)i=j;else break;}
  if(i===lyr.cur)return;
  lyr.cur=i;
  var ps=box.querySelectorAll('p');
  for(var k=0;k<ps.length;k++){ps[k].classList.toggle('on',k===i);ps[k].classList.toggle('past',k<i);}
  var on=ps[i];
  if(on&&!box.__touch)box.scrollTo({top:on.offsetTop-box.clientHeight*0.36,behavior:root().classList.contains('spo-lite')?'auto':'smooth'});
}
function toggleLyr(on){
  if(!sheet)return;
  if(on===undefined)on=!sheet.classList.contains('lyr');
  sheet.classList.toggle('lyr',on);
  byId('spo-lyrics').classList.toggle('on',on);
  if(on){lyrLoad();lyrTick();}
}
function bindLyr(){
  var box=byId('spo-lyr'),tt=null;
  box.addEventListener('click',function(e){
    var p=e.target.closest('p[data-i]');if(!p||!lyr.lines)return;
    var l=lyr.lines[+p.getAttribute('data-i')],dur=secs(txt(qs('[data-testid="playback-duration"]'))),mx=rangeMax();
    if(!l||!dur||!mx)return;
    haptic();
    try{if(typeof actSeek==='function')actSeek(Math.round(Math.min(1,l.t/dur)*mx));}catch(err){}
  });
  /* let people scroll freely; follow the song again a moment after they stop */
  box.addEventListener('touchstart',function(){box.__touch=true;if(tt)clearTimeout(tt);},{passive:true});
  box.addEventListener('touchend',function(){if(tt)clearTimeout(tt);tt=setTimeout(function(){box.__touch=false;lyr.cur=-2;},2500);},{passive:true});
}

/* ---------- Now Playing sheet ---------- */
var sheet=null,sheetOpen=false,loop=null,lastPlay=null,lastArt='',scrubbing=false,volDrag=false;
function act(id){var b=byId(id);if(b)b.click();}
function buildSheet(){
  sheet=document.createElement('div');sheet.id='spoNP';
  sheet.innerHTML=''
    +'<div class="spo-bg"><img id="spo-bgimg" alt=""></div>'
    +'<div class="spo-in">'
    +'<div class="spo-grab"></div>'
    +'<div class="spo-head"><button class="spo-ib" id="spo-close" aria-label="Close">'+I.down+'</button><div class="spo-head-t">Now Playing</div><button class="spo-ib" id="spo-share" aria-label="Share">'+I.share+'</button></div>'
    +'<div class="spo-art-wrap"><img id="spo-art" alt=""></div>'
    +'<div class="spo-lyr" id="spo-lyr"></div>'
    +'<div class="spo-ctl">'
    +'<div class="spo-meta"><div class="spo-meta-t"><div class="spo-title" id="spo-title"></div><div class="spo-artist" id="spo-artist"></div></div><button class="spo-ib" id="spo-like" aria-label="Like">'+I.heart+'</button></div>'
    +'<div class="spo-scrub" id="spo-scrub"><div class="spo-bar"><div class="spo-fill" id="spo-fill"></div></div><div class="spo-times"><span id="spo-pos">0:00</span><span id="spo-rem">-0:00</span></div></div>'
    +'<div class="spo-transport"><button class="spo-ib spo-sm" id="spo-shuffle" aria-label="Shuffle">'+I.shuffle+'</button><button class="spo-ib spo-md" id="spo-prev" aria-label="Previous">'+I.prev+'</button><button class="spo-ib spo-lg" id="spo-play" aria-label="Play">'+I.play+'</button><button class="spo-ib spo-md" id="spo-next" aria-label="Next">'+I.next+'</button><button class="spo-ib spo-sm" id="spo-repeat" aria-label="Repeat">'+I.repeat+'</button></div>'
    +'<div class="spo-dev" id="spo-dev"></div>'
    +'<div class="spo-vol"><span>'+I.volLo+'</span><div class="spo-vtrack" id="spo-vol"><div class="spo-vbar"><div class="spo-vfill" id="spo-vfill"></div></div></div><span>'+I.volHi+'</span></div>'
    +'<div class="spo-actions">'
    +'<button class="spo-ib" id="spo-lyrics">'+I.lyrics+'<span>Lyrics</span></button>'
    +'<button class="spo-ib" id="spo-queue">'+I.queue+'<span>Queue</span></button>'
    +'<button class="spo-ib" id="spo-devices">'+I.dev+'<span>Devices</span></button>'
    +'<button class="spo-ib" id="spo-timer">'+I.moon+'<span>Sleep</span></button>'
    +'<button class="spo-ib" id="spo-dl">'+I.dl+'<span>Save</span></button>'
    +'<button class="spo-ib" id="spo-pip">'+I.pip+'<span>Float</span></button>'
    +'</div></div></div>';
  document.body.appendChild(sheet);
  byId('spo-close').onclick=closeSheet;
  byId('spo-share').onclick=function(){haptic();window.spoShare();};
  byId('spo-like').onclick=function(){haptic();act('spl-liked');setTimeout(update,250);};
  byId('spo-play').onclick=function(){haptic();act('spl-play');setTimeout(update,150);};
  byId('spo-prev').onclick=function(){act('spl-prev');setTimeout(update,300);};
  byId('spo-next').onclick=function(){act('spl-next');setTimeout(update,300);};
  byId('spo-shuffle').onclick=function(){haptic();act('spl-shuffle');setTimeout(update,250);};
  byId('spo-repeat').onclick=function(){haptic();act('spl-repeat');setTimeout(update,250);};
  byId('spo-lyrics').onclick=function(){
    haptic();
    if(window.__spoLrc===false){closeSheet();setTimeout(function(){act('spl-lyrics');},280);return;}
    toggleLyr();
  };
  byId('spo-devices').onclick=function(){haptic();closeSheet();setTimeout(window.spoOpenDevices,280);};
  byId('spo-queue').onclick=function(){closeSheet();setTimeout(function(){act('spl-queue');},280);};
  byId('spo-timer').onclick=function(){act('spl-timer');};
  byId('spo-dl').onclick=function(){haptic();act('spl-download');};
  byId('spo-pip').onclick=function(){closeSheet();setTimeout(function(){act('spl-pip');},300);};
  bindScrub();bindVol();bindDrag();bindLyr();
}
function rangeMax(){var rg=qs('[data-testid="playback-progressbar"] input[type=range]');return parseInt(rg?rg.getAttribute('max'):0,10)||0;}
function progressPct(){
  var pb=qs('[data-testid="playback-progressbar"] [data-testid="progress-bar"]');
  if(!pb)return null;
  var tr=getComputedStyle(pb).getPropertyValue('--progress-bar-transform');
  return tr?Math.max(0,Math.min(1,(parseFloat(tr)||0)/100)):null;
}
function pctFrom(el,x){var r=el.getBoundingClientRect();return Math.max(0,Math.min(1,(x-r.left)/(r.width||1)));}
function showProgress(pct,dur){
  var f=byId('spo-fill');if(f)f.style.transform='scaleX('+pct+')';
  if(dur>0){byId('spo-pos').textContent=fmt(pct*dur);byId('spo-rem').textContent='-'+fmt(dur-pct*dur);}
}
function bindScrub(){
  var sc=byId('spo-scrub'),pct=0;
  function dur(){return secs(txt(qs('[data-testid="playback-duration"]')));}
  function start(x){scrubbing=true;sc.classList.add('drag');pct=pctFrom(sc,x);showProgress(pct,dur());}
  function move(x){if(!scrubbing)return;pct=pctFrom(sc,x);showProgress(pct,dur());}
  function end(){
    if(!scrubbing)return;scrubbing=false;sc.classList.remove('drag');
    var mx=rangeMax();
    try{if(mx&&typeof actSeek==='function')actSeek(Math.round(pct*mx));}catch(e){}
    haptic();
  }
  sc.addEventListener('touchstart',function(e){e.stopPropagation();start(e.touches[0].clientX);},{passive:true});
  sc.addEventListener('touchmove',function(e){e.stopPropagation();move(e.touches[0].clientX);},{passive:true});
  sc.addEventListener('touchend',function(e){e.stopPropagation();end();});
  sc.addEventListener('mousedown',function(e){start(e.clientX);});
  document.addEventListener('mousemove',function(e){move(e.clientX);});
  document.addEventListener('mouseup',end);
}
function volRange(){return qs('div[data-testid="volume-bar"] input[type="range"]')||qs('input[type="range"][data-testid="volume-bar"]');}
function setVol(pct){
  var rng=volRange();if(!rng)return;
  var max=parseFloat(rng.getAttribute('max'))||1;
  var val=pct<=0?(parseFloat(rng.getAttribute('min'))||0):pct*max;
  var setter=Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype,'value').set;
  setter.call(rng,String(val));
  rng.dispatchEvent(new Event('input',{bubbles:true}));
  rng.dispatchEvent(new Event('change',{bubbles:true}));
}
function bindVol(){
  var vt=byId('spo-vol');
  function to(x){var p=pctFrom(vt,x);byId('spo-vfill').style.width=(p*100)+'%';setVol(p);}
  vt.addEventListener('touchstart',function(e){e.stopPropagation();volDrag=true;vt.classList.add('drag');to(e.touches[0].clientX);},{passive:true});
  vt.addEventListener('touchmove',function(e){e.stopPropagation();if(volDrag)to(e.touches[0].clientX);},{passive:true});
  vt.addEventListener('touchend',function(e){e.stopPropagation();volDrag=false;vt.classList.remove('drag');});
  vt.addEventListener('mousedown',function(e){volDrag=true;to(e.clientX);});
  document.addEventListener('mousemove',function(e){if(volDrag&&sheetOpen)to(e.clientX);});
  document.addEventListener('mouseup',function(){volDrag=false;vt.classList.remove('drag');});
}
function bindDrag(){
  var sy=0,dy=0,t0=0,on=false;
  var inner=sheet.querySelector('.spo-in');
  sheet.addEventListener('touchstart',function(e){
    if(e.target.closest('.spo-scrub,.spo-vtrack'))return;
    if(e.target.closest('.spo-lyr')&&byId('spo-lyr').scrollTop>0)return;
    if(inner.scrollTop>0)return;
    on=true;sy=e.touches[0].clientY;dy=0;t0=Date.now();
  },{passive:true});
  sheet.addEventListener('touchmove',function(e){
    if(!on)return;
    dy=Math.max(0,e.touches[0].clientY-sy);
    if(dy>6){sheet.classList.add('dragging');sheet.style.transform='translate3d(0,'+dy+'px,0)';if(e.cancelable)e.preventDefault();}
  },{passive:false});
  sheet.addEventListener('touchend',function(){
    if(!on)return;on=false;
    sheet.classList.remove('dragging');sheet.style.transform='';
    var v=dy/Math.max(1,Date.now()-t0);
    if(dy>130||(dy>40&&v>.6))closeSheet();
  });
}
function update(){
  if(!sheet)return;
  var cov=byId('spl-cover-img'),src=hiRes(cov&&cov.src||'');
  if(src&&src!==lastArt){lastArt=src;byId('spo-art').src=src;byId('spo-bgimg').src=src;}
  byId('spo-title').textContent=txt(byId('spl-track'))||'Not playing';
  byId('spo-artist').textContent=txt(byId('spl-artist'));
  var playing=false;
  try{playing=!!(window.splIsPlayingSticky&&window.splIsPlayingSticky());}catch(e){}
  if(playing!==lastPlay){
    lastPlay=playing;
    byId('spo-play').innerHTML=playing?I.pause:I.play;
    byId('spo-play').setAttribute('aria-label',playing?'Pause':'Play');
    sheet.classList.toggle('paused',!playing);
  }
  var lk=byId('spl-liked');byId('spo-like').classList.toggle('on',!!(lk&&lk.classList.contains('spl-active')));
  var sh=byId('spl-shuffle'),rp=byId('spl-repeat');
  byId('spo-shuffle').classList.toggle('on',!!(sh&&sh.classList.contains('spl-active')));
  byId('spo-shuffle').classList.toggle('off',!!(sh&&sh.classList.contains('spl-disabled')));
  var rOn=!!(rp&&rp.classList.contains('spl-active')),rOne=!!(rp&&rp.classList.contains('spl-repeat-track'));
  var rb=byId('spo-repeat');rb.classList.toggle('on',rOn);rb.classList.toggle('off',!!(rp&&rp.classList.contains('spl-disabled')));
  if(rb.__one!==rOne){rb.__one=rOne;rb.innerHTML=rOne?I.repeat1:I.repeat;}
  var tm=byId('spl-timer');byId('spo-timer').classList.toggle('on',!!(tm&&tm.classList.contains('spl-active'))||!!window.__spoEos);
  var ly=byId('spl-lyrics');byId('spo-lyrics').classList.toggle('off',window.__spoLrc===false&&(!ly||ly.style.display==='none'||ly.classList.contains('spl-disabled')));
  var dv=otherDevice(),de=byId('spo-dev');
  if(de.textContent!==dv)de.textContent=dv;
  de.classList.toggle('show',!!dv);
  if(sheet.classList.contains('lyr')){lyrLoad();lyrTick();}
  if(!scrubbing){
    var dur=secs(txt(qs('[data-testid="playback-duration"]')));
    var pos=secs(txt(qs('[data-testid="playback-position"]')));
    var pct=progressPct();if(pct===null)pct=dur?pos/dur:0;
    var f=byId('spo-fill');if(f)f.style.transform='scaleX('+pct+')';
    byId('spo-pos').textContent=fmt(pos);byId('spo-rem').textContent='-'+fmt(Math.max(0,dur-pos));
  }
  if(!volDrag){var vf=byId('spl-vol-fill');if(vf)byId('spo-vfill').style.width=vf.style.width||'0%';}
}
function openSheet(){
  if(!document.body)return;
  if(!sheet)buildSheet();
  lastPlay=null;update();
  sheetOpen=true;
  sheet.classList.add('open');root().classList.add('spo-np-open');
  haptic();
  if(!loop)loop=setInterval(function(){if(sheetOpen)update();},250);
}
function closeSheet(){
  if(!sheetOpen)return;
  sheetOpen=false;
  if(sheet)sheet.classList.remove('open');
  root().classList.remove('spo-np-open');
  if(loop){clearInterval(loop);loop=null;}
}
window.spoOpenPlayer=openSheet;
window.spoClosePlayer=closeSheet;

/* ---------- native share ---------- */
window.spoShare=function(){
  var a=qs('a[data-testid=context-item-link]');
  var title=txt(byId('spl-track')),artist=txt(byId('spl-artist'));
  var url='https://open.spotify.com/';
  if(a){
    var h=a.getAttribute('href')||'',d=h;
    try{d=decodeURIComponent(h);}catch(e){}
    var m=d.match(/spotify:(track|episode):([A-Za-z0-9]+)/);
    if(!m)m=d.match(/\/(track|episode|album|playlist)\/([A-Za-z0-9]+)/);
    if(m)url='https://open.spotify.com/'+m[1]+'/'+m[2];
  }
  var body=(title?title+(artist?' · '+artist:'')+'\n':'')+url;
  try{AndBridge.shareText(title,body);}catch(e){try{navigator.share({title:title,url:url});}catch(e2){}}
};
(function(){
  try{
    var cb=navigator.clipboard;
    if(!cb||!cb.writeText||cb.__spo)return;
    var orig=cb.writeText.bind(cb);
    cb.writeText=function(t){
      var r=orig(t);
      try{if(typeof t==='string'&&/^https:\/\/open\.spotify\.com\//.test(t))setTimeout(function(){AndBridge.shareText('',t);},150);}catch(e){}
      return r;
    };
    cb.__spo=true;
  }catch(e){}
})();

/* ---------- long-press quick actions ---------- */
(function(){
  var T='[data-testid=tracklist-row],[data-encore-id=card],[data-encore-id=listRow]';
  var timer=null,sx=0,sy=0,el=null,fired=false,nativeMenu=false,blockUntil=0;
  function clear(){if(timer){clearTimeout(timer);timer=null;}}
  document.addEventListener('touchstart',function(e){
    if(e.touches.length!==1)return;
    var t=e.target.closest&&e.target.closest(T);if(!t)return;
    el=t;fired=false;nativeMenu=false;sx=e.touches[0].clientX;sy=e.touches[0].clientY;clear();
    timer=setTimeout(function(){
      timer=null;if(nativeMenu||!el)return;
      fired=true;haptic();blockUntil=Date.now()+700;
      el.dispatchEvent(new MouseEvent('contextmenu',{bubbles:true,cancelable:true,clientX:sx,clientY:sy,button:2,buttons:2,view:window}));
    },450);
  },{passive:true,capture:true});
  document.addEventListener('touchmove',function(e){
    if(!timer)return;var p=e.touches[0];
    if(Math.abs(p.clientX-sx)>10||Math.abs(p.clientY-sy)>10)clear();
  },{passive:true,capture:true});
  document.addEventListener('touchend',function(e){clear();if(fired&&e.cancelable)e.preventDefault();fired=false;},{capture:true});
  document.addEventListener('touchcancel',clear,{capture:true});
  document.addEventListener('contextmenu',function(e){if(e.isTrusted){nativeMenu=true;clear();}},true);
  document.addEventListener('click',function(e){
    if(Date.now()<blockUntil&&!(e.target.closest&&e.target.closest('[role=menu]'))){e.preventDefault();e.stopPropagation();}
  },true);
})();

/* ---------- one tap plays a song, like the app ----------
   Spotify Web only selects a row on a single click and plays on double click,
   and its row play button only shows on hover, so on a phone tapping a song
   often did nothing. A tap on a song row now plays it; buttons in the row
   (like, more, download) keep working, and long-press still opens the menu. */
(function(){
  if(window.__spoTapPlay)return;window.__spoTapPlay=true;
  var lastRow=null,lastAt=0;
  function rowPlayButton(row){
    var c=row.querySelector('[aria-colindex="1"]')||row.firstElementChild;
    return c?c.querySelector('button'):null;
  }
  document.addEventListener('click',function(e){
    if(e.defaultPrevented||!e.isTrusted||window.__spoTapPlayOff)return;
    var row=e.target.closest&&e.target.closest('[data-testid=tracklist-row]');
    if(!row)return;
    if(e.target.closest('button,input,label,[role=checkbox],[data-testid=add-to-playlist-button]'))return;
    if(row.closest('[data-tippy-root],[role=dialog]'))return;
    e.preventDefault();e.stopPropagation();
    var now=Date.now();
    if(row===lastRow&&now-lastAt<600)return;
    lastRow=row;lastAt=now;
    haptic();
    var before=txt(byId('spl-track'));
    var title=txt(row.querySelector('a[href*="/track/"],a[href*="/episode/"]')||row.querySelector('[dir=auto]'));
    row.dispatchEvent(new MouseEvent('dblclick',{bubbles:true,cancelable:true,view:window,detail:2}));
    /* fallback only when nothing changed at all, so it can never pause a song */
    setTimeout(function(){
      if(!row.isConnected)return;
      var cur=txt(byId('spl-track'));
      if(cur!==before||(title&&cur===title))return;
      var b=rowPlayButton(row);if(b)b.click();
    },1200);
  },true);
})();

/* ---------- recover from Spotify's "Something went wrong" ----------
   Spotify swaps the main view for an error screen when one of its own views
   throws. A route change remounts that view, so go Home quietly (playback keeps
   going) instead of making people press "Reload page". Rate-limited so a page
   that keeps failing ends up on the normal reload button. */
var crashAt=[];
function crashScreen(){
  var mv=qs('.Root__main-view')||qs('#main-view');if(!mv)return null;
  var b=mv.querySelector('button');if(!b||mv.querySelector('[data-testid=tracklist-row],[data-encore-id=card]'))return null;
  if(!/reload|neu laden|recargar|recarregar|ricarica|recharger|odśwież/i.test(txt(b)))return null;
  if(!/went wrong|error|fehler|erro|errore|erreur|błąd|algo sali/i.test(mv.textContent||''))return null;
  return b;
}
function recoverCrash(){
  var b=crashScreen();if(!b)return;
  var now=Date.now();crashAt=crashAt.filter(function(t){return now-t<60000;});
  if(crashAt.length>=2){
    var playing=false;try{playing=!!(window.splIsPlaying&&window.splIsPlaying());}catch(e){}
    if(!playing&&crashAt.length<3){crashAt.push(now);b.click();}
    return;
  }
  crashAt.push(now);
  try{AndBridge.dbg('w','main view crashed at '+location.pathname+', recovering');}catch(e){}
  var target=location.pathname==='/'?'/search':'/';
  var back=location.pathname+location.search;
  var h=qs('[data-testid=home-button]');
  if(target==='/'&&h)h.click();
  else{try{history.pushState({},'',target);dispatchEvent(new PopStateEvent('popstate',{state:{}}));}catch(e){}}
  if(back!=='/'&&back!=='/search')setTimeout(function(){try{history.pushState({},'',back);dispatchEvent(new PopStateEvent('popstate',{state:{}}));}catch(e){}},900);
}

/* ---------- "Dev" in the account menu (user scripts) ----------
   The item is our own node appended at the end of the menu; Spotify's own
   items are never edited, so React's view of the menu stays intact. */
function accountMenu(){
  var ms=document.querySelectorAll('[data-tippy-root] [role=menu],#context-menu [role=menu]');
  for(var i=0;i<ms.length;i++){
    var m=ms[i];
    if(m.querySelector('[data-testid=user-widget-dropdown-logout],a[href*="/account"],a[href*="account/overview"]'))return m;
  }
  return null;
}
function addDevItem(){
  var m=accountMenu();if(!m||m.querySelector('#spo-dev-item'))return;
  var last=m.lastElementChild;if(!last)return;
  var li=last.cloneNode(true);li.id='spo-dev-item';
  var target=li.querySelector('a,button,[role=menuitem]')||li;
  if(target.tagName==='A'){target.removeAttribute('href');target.removeAttribute('target');}
  target.removeAttribute('data-testid');
  var label=null,walk=document.createTreeWalker(target,NodeFilter.SHOW_TEXT);
  while(walk.nextNode()){if(walk.currentNode.nodeValue.trim()){label=walk.currentNode;break;}}
  if(label)label.nodeValue='Dev';else target.textContent='Dev';
  var svgs=li.querySelectorAll('svg');for(var k=0;k<svgs.length;k++)svgs[k].style.display='none';
  li.addEventListener('click',function(e){
    e.preventDefault();e.stopPropagation();haptic();
    document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',code:'Escape',keyCode:27,bubbles:true}));
    try{AndBridge.openDevScripts();}catch(err){}
  },true);
  m.appendChild(li);
}
document.addEventListener('click',function(e){
  if(e.target.closest&&e.target.closest('[data-testid=user-widget-link]')){setTimeout(addDevItem,80);setTimeout(addDevItem,300);}
},true);

/* ---------- back button ---------- */
window.spoBack=function(){
  if(sheetOpen){closeSheet();return true;}
  if(qs('[data-tippy-root] [role=menu]')){
    document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',code:'Escape',keyCode:27,bubbles:true}));
    document.body.dispatchEvent(new MouseEvent('mousedown',{bubbles:true}));
    return true;
  }
  if(libOpen()){toggleLib();return true;}
  var rc=byId('Desktop_PanelContainer_Id');
  if(rc&&rc.parentNode&&rc.parentNode.parentNode&&rc.parentNode.parentNode.getAttribute('aria-hidden')==='false'){
    var cb=qs('#Desktop_PanelContainer_Id [data-testid="PanelHeader_CloseButton"] button')||qs('#Desktop_PanelContainer_Id [data-testid="PanelHeader_CloseButton"]');
    if(cb){window.__spoPanelHold=0;cb.click();return true;}
  }
  return false;
};

/* ---------- sleep timer: end of song ---------- */
window.spoSleepEndOfSong=function(on){window.__spoEos=on?{t:txt(byId('spl-track'))}:null;};
function eosCheck(){
  var s=window.__spoEos;if(!s)return;
  var p=secs(txt(qs('[data-testid=playback-position]'))),d=secs(txt(qs('[data-testid=playback-duration]')));
  var cur=txt(byId('spl-track'));
  if((d>0&&d-p<=1)||(s.t&&cur&&cur!==s.t)){
    window.__spoEos=null;
    try{actPlayPause(false);}catch(e){}
    try{AndBridge.sleepTimerFinished();}catch(e){}
  }
}

/* ---------- album-art wallpaper ---------- */
window.spoSetArtWall=function(on){
  window.__spoArtWall=!!on;
  root().classList.toggle('spo-artwall',!!on);
  if(!on){root().style.removeProperty('--spo-art');root().__spoArt='';}
  else artWall();
};
function artWall(){
  if(!window.__spoArtWall)return;
  var c=byId('spl-cover-img'),s=c&&c.src||'';
  if(!s||s.indexOf('data:')===0||s===root().__spoArt)return;
  root().__spoArt=s;
  root().style.setProperty('--spo-art','url("'+s.replace(/"/g,'%22')+'")');
}

/* ---------- boot ---------- */
function boot(){
  if(!document.body){setTimeout(boot,120);return;}
  root().classList.add('spo');
  addStyle();
  if(!byId('spoScrim')){var sc=document.createElement('div');sc.id='spoScrim';document.body.appendChild(sc);}
  if(window.__spoTabs!==false){root().classList.add('spo-tabs');buildTabs();}
  window.spoSetArtWall(window.__spoArtWall!==false);
  hookPlayer();
}
boot();
setInterval(function(){
  eosCheck();
  if(window.__splBg)return;
  var mo=!!qs('[data-tippy-root] [role=menu]');
  if(root().classList.contains('spo-menu')!==mo)root().classList.toggle('spo-menu',mo);
  if(mo)addDevItem();
  recoverCrash();
  artWall();
  hookPlayer();
  if(window.__spoTabs!==false)buildTabs();
  syncTabs();
},500);
})();
"""
}
