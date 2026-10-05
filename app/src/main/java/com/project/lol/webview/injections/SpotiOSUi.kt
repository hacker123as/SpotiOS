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
/* ---------- "Play on" output picker ---------- */
#spoOut{position:fixed;inset:0;z-index:2147483647;background:rgba(0,0,0,.42);display:flex;align-items:flex-end;justify-content:center;opacity:0;pointer-events:none;transition:opacity .25s}
#spoOut.open{opacity:1;pointer-events:auto}
#spoOut .spo-out-card{width:min(560px,calc(100vw - 20px));margin-bottom:calc(var(--spo-safe-b) + 10px);padding:10px;border-radius:28px;box-sizing:border-box;
  background:rgba(34,34,40,.82);backdrop-filter:blur(30px) saturate(190%);-webkit-backdrop-filter:blur(30px) saturate(190%);box-shadow:var(--spo-rim),var(--spo-drop);
  transform:translateY(30px);transition:transform .42s var(--spo-sheet);font-family:-apple-system,system-ui,Roboto,sans-serif;color:#fff}
#spoOut.open .spo-out-card{transform:none}
#spoOut .spo-out-t{font-size:13px;font-weight:700;letter-spacing:.06em;text-transform:uppercase;color:rgba(255,255,255,.6);padding:8px 12px 10px}
#spoOut .spo-out-now{font-size:13px;font-weight:700;color:var(--spo-accent);padding:0 12px 10px}
#spoOut button{display:flex;align-items:center;gap:14px;width:100%;border:0;background:transparent;color:#fff;text-align:left;padding:12px;border-radius:18px;font:inherit}
#spoOut button:active{background:rgba(255,255,255,.1)}
#spoOut button svg{width:26px;height:26px;flex:none}
#spoOut button b{display:block;font-size:16px}
#spoOut button small{display:block;font-size:12.5px;color:rgba(255,255,255,.6);margin-top:2px}
#spoOut .spo-out-cancel{justify-content:center;font-weight:700;margin-top:6px;background:rgba(255,255,255,.08)}
/* ---------- phone-style pages (portrait) ----------
   JS tags the playlist/album/show header with data-spo-hero (an attribute
   React never sets, so it survives re-renders) and its cover wrapper with
   data-spo-hero-img. */
@media (orientation:portrait){
  html.spo [data-spo-hero]{display:flex!important;flex-direction:column!important;align-items:center!important;text-align:center!important;gap:16px!important;height:auto!important;min-height:0!important;padding:6px 16px 4px!important}
  html.spo [data-spo-hero]>*{max-width:100%!important;align-items:center!important;text-align:center!important;margin-left:0!important;margin-right:0!important}
  html.spo [data-spo-hero-img]{width:min(66vw,320px)!important;height:auto!important;min-width:0!important;flex:none!important;margin:0 auto!important;align-self:center!important;justify-self:center!important;position:relative!important;inset:auto!important;transform:none!important;aspect-ratio:1/1}
  html.spo [data-spo-hero-img] img{width:100%!important;height:100%!important;object-fit:cover!important;border-radius:16px!important;box-shadow:0 24px 60px rgba(0,0,0,.55),0 6px 18px rgba(0,0,0,.35)!important}
  html.spo [data-spo-hero] h1{font-size:clamp(24px,7.4vw,34px)!important;line-height:1.1!important;letter-spacing:-.025em!important;text-align:center!important}
  html.spo [data-spo-hero] :is(span,div){justify-content:center!important}
  /* track rows like the app: cover, title and artist, then the more button */
  html.spo section:not([data-testid=artist-page]) [data-testid=tracklist-row]>[aria-colindex]:not(:last-child):not(:has(a[href*="/track/"],a[href*="/episode/"],[data-testid=internal-track-link])){display:none!important}
  html.spo section:not([data-testid=artist-page]) [data-testid=tracklist-row]{grid-template-columns:[first] minmax(0,1fr) [last] auto!important}
  html.spo [data-testid=tracklist-row]{padding:4px 6px!important;min-height:58px}
  /* artist Popular list keeps its numbers, like the app */
  html.spo section[data-testid=artist-page] [data-testid=tracklist-row]>[aria-colindex]:not([aria-colindex="1"]):not([aria-colindex="2"]):not(:last-child){display:none!important}
  html.spo section[data-testid=artist-page] [data-testid=tracklist-row]{grid-template-columns:[index] 30px [first] minmax(0,1fr) [last] auto!important}
  /* artist hero: full-bleed photo, big name bottom-left */
  html.spo section[data-testid=artist-page]>div>div:first-child:not([data-encore-id]){height:min(54vh,470px)!important;min-height:300px!important;position:relative}
  html.spo section[data-testid=artist-page]>div>div:first-child:not([data-encore-id])::after{content:"";position:absolute;inset:auto 0 0 0;height:55%;background:linear-gradient(180deg,transparent,rgba(0,0,0,.72));pointer-events:none;z-index:0}
  html.spo section[data-testid=artist-page]>div>div:first-child:not([data-encore-id])>div.contentSpacing{position:absolute!important;left:0;right:0;bottom:0;z-index:1;padding:0 18px 14px!important;display:flex!important;flex-direction:column!important;justify-content:flex-end!important}
  html.spo section[data-testid=artist-page] :is(h1,span.encore-text-headline-large){font-size:clamp(40px,13vw,64px)!important;font-weight:900!important;line-height:.98!important;letter-spacing:-.04em!important;text-shadow:0 2px 24px rgba(0,0,0,.45)}
  html.spo section[data-testid=artist-page] [data-testid=action-bar-row]{padding:12px 16px!important}
  /* Home: two-column shortcut tiles and sideways shelves */
  html.spo [data-spo-shortcuts]{display:grid!important;grid-template-columns:1fr 1fr!important;gap:8px!important;padding:4px 10px 8px!important;width:auto!important}
  html.spo [data-spo-shortcuts]>*{min-width:0!important;width:auto!important;height:56px!important;border-radius:10px!important;overflow:hidden!important;background:rgba(255,255,255,.08)!important;box-shadow:var(--spo-rim)!important;backdrop-filter:blur(18px) saturate(170%);-webkit-backdrop-filter:blur(18px) saturate(170%)}
  html.spo [data-spo-shortcuts] img{width:56px!important;height:56px!important;object-fit:cover!important;border-radius:0!important}
}
/* "Offline mode" under the tab bar */
#spoOffline{position:fixed;left:0;right:0;bottom:calc(var(--spo-safe-b) + 2px);z-index:2147483644;display:none;align-items:center;justify-content:center;gap:6px;height:16px;font:700 11.5px/1 -apple-system,system-ui,"Segoe UI",Roboto,sans-serif;color:rgba(255,255,255,.82);pointer-events:none}
#spoOffline svg{width:12px;height:12px}
html.spo-offline #spoOffline{display:flex}
html.spo-offline #spoTabs{bottom:calc(var(--spo-safe-b) + var(--spo-gap) + 12px)}
html.spo-offline.spo-tabs{--spo-mini-bottom:calc(var(--spo-safe-b) + var(--spo-gap) + var(--spo-tab-h) + 20px)}
html.spo-np-open #spoOffline{display:none}
@media (orientation:portrait){
  html.spo [data-testid=tracklist-row] [data-testid=add-button],html.spo [data-testid=tracklist-row] [data-testid=add-to-playlist-button]{display:none!important}
  html.spo [data-testid=tracklist-row] img{width:48px!important;height:48px!important}
}
/* ---------- glass on the pages themselves ---------- */
html.spo [data-testid=action-bar-row]{border-radius:22px!important}
html.spo [data-encore-id=card]{background:linear-gradient(180deg,rgba(255,255,255,.075),rgba(255,255,255,.03))!important;box-shadow:inset 0 1px 0 rgba(255,255,255,.12),inset 0 0 0 1px rgba(255,255,255,.04)!important}
html.spo [data-testid=tracklist-row]:hover,html.spo [role=row]:focus-within [data-testid=tracklist-row]{background:rgba(255,255,255,.05)!important}
html.spo [data-encore-id=buttonSecondary]{background:rgba(255,255,255,.1)!important;border:0!important;box-shadow:var(--spo-rim)!important;border-radius:999px!important}
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
/* our own Queue and Play-on panels: they take the artwork's place, the controls stay put */
#spoNP .spo-pan{display:none;width:100%;max-width:560px;flex:1 1 0;min-height:0;flex-direction:column}
#spoNP.pan .spo-pan{display:flex}
#spoNP.pan .spo-art-wrap,#spoNP.pan .spo-lyr{display:none!important}
#spoNP .spo-pan-h{display:flex;align-items:center;gap:8px;padding:2px 0 6px}
#spoNP .spo-pan-t{flex:1;font-size:21px;font-weight:800;letter-spacing:-.02em}
#spoNP .spo-pan-h .spo-ib{width:36px;height:36px;background:rgba(255,255,255,.1)}
#spoNP .spo-pan-h .spo-ib svg{width:18px;height:18px}
#spoNP .spo-pan-b{flex:1 1 0;min-height:0;overflow-y:auto;overscroll-behavior:contain;scrollbar-width:none;padding-bottom:18px;
  -webkit-mask-image:linear-gradient(180deg,#000 0,#000 88%,transparent 100%);mask-image:linear-gradient(180deg,#000 0,#000 88%,transparent 100%)}
#spoNP .spo-pan-b::-webkit-scrollbar{display:none}
#spoNP .spo-pan-sec{font-size:13px;font-weight:700;color:rgba(255,255,255,.58);margin:14px 4px 6px;letter-spacing:.01em}
#spoNP .spo-pan-sec:first-child{margin-top:4px}
#spoNP .spo-row{display:flex;align-items:center;gap:12px;width:100%;box-sizing:border-box;padding:7px 6px;border:0;background:transparent;color:#fff;text-align:left;border-radius:14px;font:inherit;-webkit-tap-highlight-color:transparent;transition:background .2s,opacity .2s,transform .3s var(--spo-spring)}
#spoNP .spo-row:active{background:rgba(255,255,255,.1);transform:scale(.985)}
#spoNP .spo-row img,#spoNP .spo-row .ic{width:46px;height:46px;border-radius:8px;object-fit:cover;flex:none;background:rgba(255,255,255,.1)}
#spoNP .spo-row .ic{display:flex;align-items:center;justify-content:center;border-radius:12px;color:#fff}
#spoNP .spo-row .ic svg{width:24px;height:24px}
#spoNP .spo-row .t{flex:1;min-width:0}
#spoNP .spo-row b{display:block;font-size:15px;font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
#spoNP .spo-row small{display:block;font-size:13px;color:rgba(255,255,255,.6);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin-top:2px}
#spoNP .spo-row.now b,#spoNP .spo-row.act b,#spoNP .spo-row.act small{color:var(--spo-accent)}
#spoNP .spo-row.act .ic{background:var(--spo-accent);color:#000}
#spoNP .spo-row .eq{display:none;width:18px;height:14px;flex:none;align-items:flex-end;gap:2px}
#spoNP .spo-row.now .eq,#spoNP .spo-row.act .eq{display:flex}
#spoNP .spo-row .eq i{flex:1;background:var(--spo-accent);border-radius:1px;animation:spoEq 1s ease-in-out infinite}
#spoNP .spo-row .eq i:nth-child(2){animation-delay:-.4s}
#spoNP .spo-row .eq i:nth-child(3){animation-delay:-.7s}
#spoNP.paused .spo-row .eq i,html.spo-lite #spoNP .spo-row .eq i{animation:none;height:40%}
@keyframes spoEq{0%,100%{height:30%}50%{height:100%}}
#spoNP .spo-row.busy{opacity:.45;pointer-events:none}
#spoNP .spo-row .go{font-size:12px;font-weight:700;color:rgba(255,255,255,.5);flex:none}
#spoNP .spo-dvol{display:flex;align-items:center;gap:10px;padding:2px 8px 8px 64px;color:rgba(255,255,255,.6)}
#spoNP .spo-dvol svg{width:16px;height:16px;flex:none}
#spoNP .spo-dvol input{flex:1;accent-color:var(--spo-accent);height:24px}
#spoNP .spo-pan-msg{text-align:center;color:rgba(255,255,255,.66);padding:28px 12px;font-size:15px;font-weight:600;line-height:1.5}
#spoNP .spo-pan-msg button,#spoNP .spo-pan-more{margin:14px 4px 0;border:0;border-radius:999px;padding:10px 18px;background:rgba(255,255,255,.14);color:#fff;font-family:inherit;font-weight:700;font-size:14px;line-height:1}
#spoNP .spo-pan-more{display:block;margin:18px auto 0;background:transparent;color:rgba(255,255,255,.55);font-size:13px}
#spoNP .spo-pan-hint{font-size:13px;color:rgba(255,255,255,.5);line-height:1.45;padding:6px 6px 0}
#spoNP .spo-dev{cursor:pointer}
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
  #spoNP .spo-lyr,#spoNP .spo-pan{grid-column:1;grid-row:3;width:min(44vw,560px);height:100%;align-self:stretch}
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

/* ================= 2.9: phone pages, rebuilt against Spotify's real markup ================= */
/* page-level scroll bars never show on a phone */
html.spo .os-scrollbar{display:none!important}
/* the bottom fade behind the mini player and tab bar (#main-view replaced .Root__main-view) */
html.spo #main-view main{padding-bottom:calc(var(--spo-dock) + 24px)!important}
html.spo #main-view::after{
  content:"";position:fixed;left:0;right:0;bottom:0;height:calc(var(--spo-dock) + 36px);z-index:50;pointer-events:none;
  background:linear-gradient(180deg,rgba(0,0,0,0),rgba(0,0,0,.72) 40%,rgba(0,0,0,.96));
}
html.spo-lite #main-view::after{display:none!important}
/* the sticky top bar: transparent at the top of a page, dark glass once Spotify fades it in on scroll */
html.spo [data-testid=topbar-content]{background:transparent!important;box-shadow:none!important;backdrop-filter:none!important;-webkit-backdrop-filter:none!important;border-radius:0!important}
html.spo header[data-testid=topbar]{pointer-events:none}
html.spo header[data-testid=topbar] :is(button,a){pointer-events:auto}
html.spo header[data-testid=topbar]>div:first-child{background:rgba(12,12,16,.62)!important;backdrop-filter:blur(26px) saturate(180%)!important;-webkit-backdrop-filter:blur(26px) saturate(180%)!important;box-shadow:inset 0 -1px 0 rgba(255,255,255,.06)!important}
html.spo header[data-testid=topbar]>div:first-child>*{display:none!important}
html.spo [data-testid=topbar-content] [data-testid=play-button]{transform:scale(.8)}
html.spo [data-testid=topbar-content]>span{font-size:16px!important;font-weight:800!important}

@media (orientation:portrait){
  /* ---- top bar of the app: the tab bar already has Home, Search and Library ---- */
  html.spo-tabs #global-nav-bar{background:transparent!important;padding-left:60px!important}
  html.spo-tabs #global-nav-bar [data-testid=home-button]{display:none!important}
  html.spo-tabs:not(.spo-route-search) #global-nav-bar form[role=search]{display:none!important}
  /* Search tab: a big "Search" title, then a full-width search box on its own row, like the app */
  html.spo-tabs.spo-route-search #global-nav-bar{flex-wrap:wrap!important;height:auto!important;padding:4px 16px 10px!important;row-gap:10px!important;align-items:center!important}
  html.spo-tabs.spo-route-search #global-nav-bar::before{content:"Search";order:0;flex:1 1 auto;font-size:27px;font-weight:800;letter-spacing:-.03em;color:#fff;line-height:48px}
  html.spo-tabs.spo-route-search #global-nav-bar>div:has(form[role=search]){order:5!important;flex:1 0 100%!important;width:100%!important;max-width:none!important;margin:0!important;padding:0!important}
  html.spo-tabs.spo-route-search #global-nav-bar>div:has(form[role=search]) div:has(>form[role=search]),html.spo-tabs.spo-route-search #global-nav-bar>div:has(form[role=search])>div{width:100%!important;max-width:none!important;flex:1 1 auto!important;margin:0!important;padding:0!important}
  html.spo-tabs.spo-route-search #global-nav-bar>div:last-child{order:1!important;flex:none!important;white-space:nowrap!important}
  html.spo-tabs.spo-route-search #global-nav-bar [data-testid=signup-button]{display:none!important}
  html.spo-tabs.spo-route-search #global-nav-bar form[role=search]{display:flex!important;width:100%!important;max-width:none!important;position:relative!important;margin:0!important}
  html.spo-tabs.spo-route-search #global-nav-bar form[role=search]>div:not([class*=icon__icon]){display:block!important;flex:1 1 auto!important;width:100%!important;min-width:0!important}
  html.spo-tabs.spo-route-search #global-nav-bar form[role=search] input{display:block!important;width:100%!important;height:46px!important;font-size:15.5px!important;font-weight:600!important;padding-left:46px!important;background:#fff!important;color:#121212!important;caret-color:#1db954!important;border-radius:10px!important;box-shadow:0 6px 20px rgba(0,0,0,.3)!important;backdrop-filter:none!important;-webkit-backdrop-filter:none!important;opacity:1!important;visibility:visible!important}
  html.spo-tabs.spo-route-search #global-nav-bar form[role=search] input::placeholder{color:#535353!important;opacity:1!important}
  html.spo-tabs.spo-route-search #global-nav-bar form[role=search] :is([class*=icon__icon]) button{color:#121212!important;background:transparent!important;box-shadow:none!important;backdrop-filter:none!important;-webkit-backdrop-filter:none!important}
  html.spo-tabs.spo-route-search #global-nav-bar form[role=search] :is(kbd,[data-testid=browse-button]){display:none!important}
  html.spo-tabs.spo-route-search #global-nav-bar form[role=search]>[class*=icon__icon--leading]{position:absolute!important;left:2px!important;top:0!important;bottom:0!important;z-index:2!important;display:flex!important;align-items:center!important;width:44px!important;margin:0!important}
  html.spo-tabs.spo-route-search #global-nav-bar form[role=search]>[class*=icon__icon--trailing]{position:absolute!important;right:4px!important;top:0!important;bottom:0!important;z-index:2!important;display:flex!important;align-items:center!important;margin:0!important}
  html.spo-tabs.spo-route-search #global-nav-bar form[role=search] input+div span{color:#535353!important}
  /* search results: same side margins as Home, chips inset, song rows aligned */
  html.spo-route-search [data-testid=grid-search-results]{padding:0 10px!important}
  html.spo-route-search main [data-testid=carousel-scroller]:has([data-encore-id=chip])>div{padding-inline:18px!important}
  html.spo-route-search main [data-testid=carousel-scroller]:has([data-encore-id=chip]){scroll-padding-inline:18px!important}
  html.spo section[data-testid=search-tracks-result]{margin-bottom:22px!important}
  html.spo section[data-testid=search-tracks-result] [data-testid=rich-title-row-shelf-header]{padding-inline:8px!important}
  html.spo section[data-testid=search-tracks-result] [data-testid=tracklist-row]{padding-left:8px!important}
  /* Browse all: two columns of coloured tiles */
  html.spo-route-search section[data-testid=component-shelf]:has(a[href*="/genre/"]) [data-testid=grid-container]{grid-auto-flow:row!important;grid-template-columns:1fr 1fr!important;grid-auto-columns:auto!important;overflow:visible!important;gap:10px!important;margin:0!important;padding:0!important}
  html.spo-tabs #Desktop_LeftSidebar_Id[style*="width: 48px"]{opacity:0!important;pointer-events:none!important}

  /* ---- playlist / album header: cover, then title and details, centred ---- */
  html.spo [data-testid=entity-header]{max-height:none!important;height:auto!important;min-height:0!important}
  html.spo [data-testid=entity-header]>.contentSpacing{height:auto!important;min-height:0!important}
  html.spo [data-spo-hero]>div:not([data-spo-hero-img]){width:100%!important;display:flex!important;flex-direction:column!important;align-items:center!important;gap:6px!important}
  html.spo [data-spo-hero] [data-testid=entityTitle],html.spo [data-spo-hero] [data-testid=entityTitle]+*,html.spo [data-spo-hero] span:has(>[data-testid=entityTitle]){display:block!important;width:100%!important;max-width:100%!important}
  html.spo [data-testid=entity-header] h1{white-space:normal!important;width:auto!important;overflow-wrap:anywhere;-webkit-line-clamp:3;display:-webkit-box!important;-webkit-box-orient:vertical;overflow:hidden}
  html.spo section:not([data-testid=artist-page]) [data-testid=entity-header] h1{font-size:clamp(24px,7.2vw,32px)!important;line-height:1.12!important;font-weight:800!important}
  html.spo [data-spo-hero] [data-testid=entityTitle]~*{justify-content:center!important;flex-wrap:wrap!important;row-gap:2px}
  html.spo [data-spo-hero] [class*=encore-text-body-small]{font-size:13.5px!important}

  /* ---- action row, Spotify-app order: save, download, more ... shuffle, big play ---- */
  html.spo [data-testid=action-bar]{background:transparent!important;padding:4px 14px 6px!important}
  html.spo [data-testid=action-bar-row]{display:flex!important;align-items:center!important;flex-wrap:nowrap!important;gap:2px!important;padding:0!important;min-height:64px;background:transparent!important}
  html.spo [data-testid=action-bar-row-actions]{display:contents!important}
  html.spo [data-testid=action-bar-row]>*,html.spo [data-testid=action-bar-row-actions]>*{order:3}
  html.spo [data-testid=action-bar-row] [data-testid=add-button],html.spo [data-testid=action-bar-row] button[aria-label*=ollow]{order:1}
  html.spo [data-testid=action-bar-row]>[data-testid=more-button]{order:4}
  html.spo [data-testid=action-bar-row] :is(#spl-dl-skip-btn,#spl-dl-cancel-btn){order:5;width:36px!important;height:36px!important;margin:0!important}
  html.spo [data-testid=action-bar-row] :is(#spl-dl-skip-btn,#spl-dl-cancel-btn) svg{width:20px!important;height:20px!important}
  html.spo [data-testid=action-bar-row]::after{content:"";order:7;flex:1 1 auto}
  html.spo [data-testid=action-bar-row] button[aria-label*=huffle]{order:8}
  html.spo [data-testid=action-bar-row]>div:has(>[data-testid=play-button]){order:9;margin-left:6px}
  html.spo [data-testid=action-bar-row-trailing]{display:none!important}
  html.spo [data-testid=action-bar-row] [data-encore-id=buttonTertiary]{min-width:40px;min-height:40px}
  html.spo [data-testid=action-bar-row] [data-encore-id=buttonTertiary] svg{width:24px!important;height:24px!important}
  html.spo [data-testid=action-bar-row] [data-testid=play-button]>span{width:56px!important;height:56px!important;min-block-size:56px!important;min-inline-size:56px!important}
  html.spo [data-testid=action-bar-row] button[aria-label=Follow],html.spo [data-testid=action-bar-row] button[aria-label=Following]{order:1}

  /* ---- song rows: cover, title and artist, then only the more button ---- */
  html.spo [data-testid=tracklist-row]>[aria-colindex]:last-child>:not([data-testid=more-button]){display:none!important}
  html.spo [data-testid=tracklist-row] [data-testid=more-button]{opacity:.7!important;transform:none!important;width:36px;height:36px;color:#fff!important}
  html.spo [data-testid=tracklist-row]>[aria-colindex="1"] button{display:none!important}
  html.spo [data-testid=tracklist-row]{padding:6px 8px 6px 16px!important;min-height:60px;grid-gap:0 12px!important;border-radius:10px!important}
  html.spo [data-testid=tracklist-row] [aria-colindex="2"]{gap:12px!important}
  html.spo [data-testid=tracklist-row] [aria-colindex="2"] img{width:48px!important;height:48px!important;border-radius:6px!important;margin:0!important}
  html.spo [data-testid=tracklist-row] [data-testid=internal-track-link]>div{font-size:15.5px!important;font-weight:500!important;line-height:1.25!important}
  html.spo [data-testid=tracklist-row] [aria-colindex="2"] [class*=encore-text-body-small]{font-size:13.5px!important}
  html.spo section[data-testid=artist-page] [data-testid=tracklist-row]>[aria-colindex="1"]{display:flex!important;justify-content:center}
  html.spo section[data-testid=artist-page] [data-testid=tracklist-row]>[aria-colindex="1"] span{display:block!important;visibility:visible!important;opacity:1!important;color:var(--spo-ink-2)!important;font-size:15px!important}

  /* ---- shelves: no boxes, just artwork and two lines of text, scrolling sideways ---- */
  html.spo [data-encore-id=card]{background:transparent!important;box-shadow:none!important;padding:0!important;border-radius:0!important}
  html.spo [data-encore-id=card] [data-testid=card-image],html.spo [data-encore-id=card] img{border-radius:8px!important;box-shadow:none!important}
  html.spo [data-encore-id=card][aria-labelledby*=":artist:"] img{border-radius:50%!important}
  html.spo [data-encore-id=card] :is([data-testid=card-image],[style*="--card-color"]){background:transparent!important}
  html.spo [data-encore-id=card] :is(div,button):has(>img){box-shadow:none!important}
  html.spo [data-encore-id=card] [data-encore-id=buttonPrimary]{display:none!important}
  html.spo [data-encore-id=cardTitle]{font-size:13.5px!important;font-weight:600!important;margin-top:8px!important}
  html.spo [data-encore-id=cardSubtitle]{font-size:12.5px!important;color:var(--spo-ink-2)!important}
  html.spo section:is([data-testid=component-shelf],[data-testid$="-search-entity-shelf"]) [data-testid=grid-container]{display:grid!important;grid-template-columns:none!important;grid-template-rows:auto!important;grid-auto-flow:column!important;grid-auto-columns:clamp(118px,35vw,170px)!important;grid-auto-rows:auto!important;gap:14px!important;overflow-x:auto!important;overflow-y:hidden!important;scroll-snap-type:x proximity;padding:0 18px 4px!important;margin:0 -18px!important;scrollbar-width:none;overscroll-behavior-x:contain}
  html.spo section:is([data-testid=component-shelf],[data-testid$="-search-entity-shelf"]) [data-testid=grid-container]>*{scroll-snap-align:start;min-width:0!important;scroll-margin-left:18px}
  /* newer carousel shelves: Spotify sizes the columns itself and relies on card padding for the gaps */
  html.spo section:is([data-testid=component-shelf],[data-testid$="-search-entity-shelf"]) [data-testid=carousel-scroller] [role=grid]{grid-template-columns:none!important;grid-auto-flow:column!important;grid-auto-columns:clamp(118px,35vw,170px)!important;column-gap:12px!important;padding-left:40px!important;padding-right:40px!important}
  html.spo section:is([data-testid=component-shelf],[data-testid$="-search-entity-shelf"]) [data-testid=carousel-scroller] [role=grid]>*{width:auto!important;min-width:0!important;animation:none!important;opacity:1!important;transform:none!important}
  html.spo section:is([data-testid=component-shelf],[data-testid$="-search-entity-shelf"]) [data-testid=carousel-scroller] [data-carousel-item]{width:auto!important;animation:none!important;opacity:1!important}
  html.spo :is([data-testid=carousel-previous-button],[data-testid=carousel-next-button]){display:none!important}
  html.spo section:is([data-testid=component-shelf],[data-testid$="-search-entity-shelf"]){margin-bottom:22px!important;padding:0 8px!important}
  html.spo [data-testid=rich-title-row-shelf-header]{padding:0 0 10px!important;--box-padding-inline-start:0px!important;--box-padding-inline-end:0px!important;min-height:0!important;background:transparent!important}
  html.spo [data-testid=rich-title-row-shelf-header]>*{padding-inline:0!important}
  html.spo [data-testid=rich-title-row-shelf-header] h2{font-size:21px!important;font-weight:800!important;letter-spacing:-.02em!important}
  html.spo [data-testid=rich-title-row-shelf-header] h2 a{color:#fff!important;text-decoration:none!important}
  html.spo [data-testid=rich-title-row-shelf-header] a:not(h2 a){background:transparent!important;box-shadow:none!important;padding:0!important;font-size:13px!important;font-weight:700!important;color:var(--spo-ink-2)!important;text-decoration:none!important}
  html.spo section a[href*="/section/"]:not(:has(img)):not(:is(h1,h2,h3,h4) *):not(:is(h1,h2,h3,h4)){background:transparent!important;box-shadow:none!important;padding:0!important}

  /* ---- artist page: the photo fills the header behind the name ---- */
  html.spo-route-artist .before-scroll-node>div:not([data-testid]){height:min(54vh,470px)!important}
  html.spo-route-artist .before-scroll-node [data-testid=background-image]{height:100%!important;top:0!important;background-position:center 22%!important}
  /* playlists with a photo header (e.g. Today's Top Hits): tall photo, title at the bottom, like the artist page */
  html.spo-imghdr .before-scroll-node>div:not([data-testid]){height:min(46vh,400px)!important}
  html.spo-imghdr .before-scroll-node [data-testid=background-image]{height:100%!important;top:0!important;background-position:center 30%!important}
  html.spo-imghdr main>section [data-testid=entity-header]{height:min(46vh,400px)!important;max-height:none!important;display:flex!important;flex-direction:column!important;justify-content:flex-end!important}
  html.spo-imghdr [data-testid=entity-header]>.contentSpacing{padding:0 8px 14px!important}
  html.spo-imghdr [data-testid=entity-header] h1{font-size:clamp(30px,9.5vw,44px)!important;line-height:1.02!important;font-weight:900!important;text-shadow:0 2px 20px rgba(0,0,0,.45)}
  /* song artwork never squeezes, so titles line up */
  html.spo [data-testid=tracklist-row]>[aria-colindex]>div:first-child:has(>img):not(:has(a)){flex:none!important;width:48px!important;height:48px!important;min-width:48px!important}
  html.spo section[data-testid=artist-page] [data-testid=entity-header]>.contentSpacing{position:absolute!important;left:0;right:0;bottom:0;padding:0 18px 16px!important}

  /* ---- home: 2-column shortcuts sit flush, chips on one row ---- */
  html.spo [data-spo-shortcuts]>*{background:rgba(255,255,255,.09)!important;box-shadow:none!important;border-radius:6px!important;backdrop-filter:none!important;-webkit-backdrop-filter:none!important}
  html.spo [data-spo-shortcuts] :is(p,span,a){font-size:13px!important;font-weight:700!important;line-height:1.2!important}
}

/* ---- chips: Spotify's pill is the chip's inner span; the selected one is green ---- */
html.spo [data-encore-id=chip][class*=legacy-chip]{background:transparent!important;box-shadow:none!important;border:0!important;padding:0!important}
html.spo [data-encore-id=chip][class*=legacy-chip]>span:first-child,html.spo [data-encore-id=chip]:not([class*=legacy-chip]){background:rgba(255,255,255,.1)!important;color:#fff!important;box-shadow:var(--spo-rim)!important;border:0!important;border-radius:999px!important;font-weight:600!important}
html.spo [data-encore-id=chip][class*=legacy-chip]:is([aria-checked=true],[aria-pressed=true],[aria-selected=true],[class*=selected])>span:first-child,
html.spo [data-encore-id=chip][class*=legacy-chip]>[class*=inner--selected],
html.spo [data-encore-id=chip]:not([class*=legacy-chip]):is([aria-checked=true],[aria-pressed=true],[aria-selected=true]){background:var(--spo-accent)!important;color:#000!important;box-shadow:0 6px 18px rgba(30,215,96,.25)!important}
html.spo [data-encore-id=chip] *{color:inherit!important}

/* empty mini player: no broken-image icon before the first song */
html.spo #spl-cover-img:is([src=""],:not([src])){opacity:0!important}
html.spo #spotilolPlayerControls .spl-cover{background:rgba(255,255,255,.08) url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ffffff66' stroke-width='1.8' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M9 18V5l12-2v13'/%3E%3Ccircle cx='6' cy='18' r='3'/%3E%3Ccircle cx='18' cy='16' r='3'/%3E%3C/svg%3E") center/52% no-repeat}

/* ---- our back button (top-left, every page except Home) ---- */
#spoBackBtn{position:fixed;z-index:60;top:calc(var(--spo-safe-t) + 4px);left:10px;width:40px;height:40px;border:0;border-radius:50%;display:none;align-items:center;justify-content:center;color:#fff;
  background:rgba(40,40,48,.5);backdrop-filter:blur(22px) saturate(190%);-webkit-backdrop-filter:blur(22px) saturate(190%);box-shadow:var(--spo-rim),0 6px 18px rgba(0,0,0,.28);transition:transform .3s var(--spo-spring)}
#spoBackBtn svg{width:22px;height:22px}
#spoBackBtn:active{transform:scale(.86)}
html.spo-tabs.spo-route-sub #spoBackBtn{display:flex}
html.spl-libopen #spoBackBtn,html.spo-np-open #spoBackBtn,html.spo-menu #spoBackBtn{display:none!important}
@media (orientation:landscape){#spoBackBtn{display:none!important}}
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
  x:SV+'<path d="M6 6l12 12M18 6 6 18"/></svg>',
  refresh:SV+'<path d="M20 11a8 8 0 1 0-2.3 5.7"/><path d="M20 4v7h-7"/></svg>',
  laptop:SV+'<rect x="4" y="5" width="16" height="11" rx="1.5"/><path d="M2 19h20"/></svg>',
  phone:SV+'<rect x="7" y="2.5" width="10" height="19" rx="2.2"/><path d="M11 18.5h2"/></svg>',
  tablet:SV+'<rect x="4.5" y="2.5" width="15" height="19" rx="2.2"/><path d="M11 18.5h2"/></svg>',
  speaker:SV+'<rect x="5" y="2.5" width="14" height="19" rx="2.5"/><circle cx="12" cy="14" r="3.5"/><circle cx="12" cy="7" r="1"/></svg>',
  tv:SV+'<rect x="2.5" y="4.5" width="19" height="13" rx="2"/><path d="M8 21h8"/></svg>',
  car:SV+'<path d="M5 16v-5l2-5h10l2 5v5"/><path d="M3 11h18v5H3z"/><circle cx="7.5" cy="16.5" r="1.4"/><circle cx="16.5" cy="16.5" r="1.4"/></svg>',
  game:SV+'<rect x="2.5" y="7" width="19" height="10" rx="5"/><path d="M7 10.5v3M5.5 12h3"/><path d="M16 11h.01M17.5 13h.01"/></svg>',
  bt:SV+'<path d="m7 7 10 10-5 4V3l5 4L7 17"/></svg>',
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
  if(k==='library'){libByUser=true;toggleLib();setTimeout(syncTabs,350);return;}
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

/* ---------- "Play on": this phone's outputs or Spotify Connect ---------- */
function outSheet(){
  var o=byId('spoOut');if(o)return o;
  o=document.createElement('div');o.id='spoOut';
  o.innerHTML='<div class="spo-out-card"><div class="spo-out-t">Play on</div><div class="spo-out-now" id="spo-out-now"></div>'
    +'<button type="button" data-o="phone">'+I.dev+'<span><b>This phone</b><small>Speaker, Bluetooth, headphones, car</small></span></button>'
    +'<button type="button" data-o="connect">'+I.queue+'<span><b>Spotify Connect</b><small>Laptop, TV, speakers, consoles</small></span></button>'
    +'<button type="button" data-o="x" class="spo-out-cancel">Cancel</button></div>';
  o.addEventListener('click',function(e){
    var b=e.target.closest('button[data-o]');
    if(!b&&e.target!==o)return;
    var k=b?b.getAttribute('data-o'):'x';
    o.classList.remove('open');
    haptic();
    if(k==='phone'){
      /* playing on another device: bring it back here first via Spotify's picker */
      if(otherDevice()){closeSheet();setTimeout(window.spoOpenDevices,250);}
      else{try{AndBridge.openAudioOutput();}catch(err){}}
    }else if(k==='connect'){closeSheet();setTimeout(window.spoOpenDevices,250);}
  });
  document.body.appendChild(o);
  return o;
}
window.spoPickOutput=function(){
  var o=outSheet(),dv=otherDevice();
  byId('spo-out-now').textContent=dv||'Playing on this phone';
  requestAnimationFrame(function(){o.classList.add('open');});
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
  if(on)closePan();
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
    +'<div class="spo-pan" id="spo-pan"><div class="spo-pan-h"><div class="spo-pan-t" id="spo-pan-t"></div><button class="spo-ib" id="spo-pan-r" aria-label="Refresh">'+I.refresh+'</button><button class="spo-ib" id="spo-pan-x" aria-label="Close">'+I.x+'</button></div><div class="spo-pan-b" id="spo-pan-b"></div></div>'
    +'<div class="spo-ctl">'
    +'<div class="spo-meta"><div class="spo-meta-t"><div class="spo-title" id="spo-title"></div><div class="spo-artist" id="spo-artist"></div></div><button class="spo-ib" id="spo-like" aria-label="Like">'+I.heart+'</button></div>'
    +'<div class="spo-scrub" id="spo-scrub"><div class="spo-bar"><div class="spo-fill" id="spo-fill"></div></div><div class="spo-times"><span id="spo-pos">0:00</span><span id="spo-rem">-0:00</span></div></div>'
    +'<div class="spo-transport"><button class="spo-ib spo-sm" id="spo-shuffle" aria-label="Shuffle">'+I.shuffle+'</button><button class="spo-ib spo-md" id="spo-prev" aria-label="Previous">'+I.prev+'</button><button class="spo-ib spo-lg" id="spo-play" aria-label="Play">'+I.play+'</button><button class="spo-ib spo-md" id="spo-next" aria-label="Next">'+I.next+'</button><button class="spo-ib spo-sm" id="spo-repeat" aria-label="Repeat">'+I.repeat+'</button></div>'
    +'<div class="spo-dev" id="spo-dev"></div>'
    +'<div class="spo-vol"><span>'+I.volLo+'</span><div class="spo-vtrack" id="spo-vol"><div class="spo-vbar"><div class="spo-vfill" id="spo-vfill"></div></div></div><span>'+I.volHi+'</span></div>'
    +'<div class="spo-actions">'
    +'<button class="spo-ib" id="spo-lyrics">'+I.lyrics+'<span>Lyrics</span></button>'
    +'<button class="spo-ib" id="spo-queue">'+I.queue+'<span>Queue</span></button>'
    +'<button class="spo-ib" id="spo-devices">'+I.dev+'<span>Play on</span></button>'
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
  byId('spo-devices').onclick=function(){haptic();togglePan('dev');};
  byId('spo-queue').onclick=function(){haptic();togglePan('queue');};
  byId('spo-dev').onclick=function(){haptic();openPan('dev');};
  byId('spo-pan-x').onclick=function(){haptic();closePan();};
  byId('spo-pan-r').onclick=function(){haptic();panRefresh();};
  bindPan();
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
  var playing=false,pend=window.__spoPend;
  try{playing=!!(window.splIsPlayingSticky&&window.splIsPlayingSticky())||!!pend;}catch(e){}
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
    var dur=secs(pend?pend.dur:txt(qs('[data-testid="playback-duration"]')));
    var pos=pend?0:secs(txt(qs('[data-testid="playback-position"]')));
    var pct=pend?0:progressPct();if(pct===null)pct=dur?pos/dur:0;
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
  if(pan.k){pan.req++;clearInterval(pan.t);setTimeout(function(){if(!sheetOpen)closePan();},560);}
}
window.spoOpenPlayer=openSheet;
window.spoClosePlayer=closeSheet;

/* ---------- our own Queue and Play-on panels (Spotify Web API, the player's own login) ---------- */
var pan={k:'',req:0,busy:false,t:null,key:'',n:0};
function api(path,opt){
  var tok=window.spotAuthToken;
  if(!tok)return Promise.reject(new Error('signin'));
  if(navigator.onLine===false)return Promise.reject(new Error('offline'));
  opt=opt||{};
  var f=window.oriFetch||window.fetch;
  return f('https://api.spotify.com/v1'+path,{method:opt.method||'GET',headers:{'Authorization':tok,'Content-Type':'application/json'},body:opt.body?JSON.stringify(opt.body):undefined})
    .then(function(r){
      if(r.status===204||r.status===202)return null;
      if(!r.ok)throw new Error('http'+r.status);
      return r.text().then(function(t){try{return t?JSON.parse(t):null;}catch(e){return null;}});
    });
}
function esc(v){return String(v==null?'':v).replace(/[&<>"]/g,function(c){return c==='&'?'&amp;':c==='<'?'&lt;':c==='>'?'&gt;':'&quot;';});}
function itemArt(it){var im=it&&((it.album&&it.album.images)||it.images||(it.show&&it.show.images));if(!im||!im.length)return '';var x=im[1]||im[0];return x&&x.url||'';}
function itemSub(it){return it&&it.artists?it.artists.map(function(a){return a.name;}).join(', '):((it&&it.show&&it.show.name)||'');}
var EQ='<span class="eq"><i></i><i></i><i></i></span>';
function trackRow(it,i,cls){
  var a=itemArt(it);
  return '<button type="button" class="spo-row '+(cls||'')+'" data-i="'+i+'">'+(a?'<img src="'+esc(a)+'" alt="" loading="lazy">':'<span class="ic">'+I.queue+'</span>')
    +'<span class="t"><b>'+esc(it.name)+'</b><small>'+esc(itemSub(it))+'</small></span>'+EQ+'</button>';
}
function devIcon(t){
  t=String(t||'').toLowerCase();
  if(t==='smartphone')return I.phone;if(t==='tablet')return I.tablet;
  if(t==='tv'||t==='castvideo'||t==='stb')return I.tv;
  if(t==='automobile')return I.car;if(t==='gameconsole')return I.game;
  if(t==='computer')return I.laptop;
  return I.speaker;
}
function devType(t){
  var m={computer:'Computer',smartphone:'Phone',tablet:'Tablet',speaker:'Speaker',tv:'TV',avr:'Receiver',stb:'TV box',audiodongle:'Speaker',gameconsole:'Console',castvideo:'Chromecast',castaudio:'Speaker group',automobile:'Car',smartwatch:'Watch'};
  return m[String(t||'').toLowerCase()]||'Spotify Connect';
}
function myName(){return window.__spoDeviceName||'SpotiOS';}
/* __spoMyId comes from this player's own registration (ConnectKeepAlive); spotDevId can end
   up holding the other device's id after a transfer, so it is only a fallback. */
function isMine(d){var me=window.__spoMyId||window.spotDevId;return !!d&&((me&&d.id===me)||d.name===myName());}
function panMsg(h){var b=byId('spo-pan-b');if(b)b.innerHTML='<div class="spo-pan-msg">'+h+'</div>';}
function panFail(k,e){
  var m=String(e&&e.message||'');
  var why=m==='offline'?'You’re offline.':(m==='signin'?'Log in to Spotify to see this.':(m==='http429'?'Spotify is busy right now.':'Couldn’t reach Spotify.'));
  panMsg(why+'<br><button type="button" data-p="retry">Try again</button><button type="button" data-p="sp'+k+'">Open Spotify’s '+(k==='queue'?'queue':'device list')+'</button>');
}
function panQueue(quiet){
  var id=++pan.req;
  if(!quiet)panMsg('Loading your queue…');
  api('/me/player/queue').then(function(j){
    if(id!==pan.req||pan.k!=='queue')return;
    var q=(j&&j.queue)||[],cur=j&&j.currently_playing,h='';
    if(cur)h+='<div class="spo-pan-sec">Now playing</div>'+trackRow(cur,-1,'now');
    if(q.length){
      h+='<div class="spo-pan-sec">Next up</div>';
      q.slice(0,40).forEach(function(it,i){h+=trackRow(it,i);});
      h+='<div class="spo-pan-hint">Tap a song to skip straight to it.</div>';
    }else h+='<div class="spo-pan-msg">Nothing up next.<br>Long-press a song and pick Add to queue.</div>';
    if(!cur&&!q.length)h='<div class="spo-pan-msg">Nothing is playing.<br>Play something and your queue shows up here.</div>';
    h+='<button type="button" class="spo-pan-more" data-p="spqueue">Open Spotify’s queue</button>';
    var b=byId('spo-pan-b'),st=b.scrollTop;b.innerHTML=h;b.scrollTop=st;b.__q=q;
  }).catch(function(e){if(id===pan.req&&pan.k==='queue'&&!quiet)panFail('queue',e);});
}
function panDevices(quiet){
  var id=++pan.req;
  if(!quiet)panMsg('Looking for devices…');
  api('/me/player/devices').then(function(j){
    if(id!==pan.req||pan.k!=='dev')return;
    var ds=(j&&j.devices)||[],mine=null,act=null,h='';
    ds.forEach(function(d){if(!mine&&isMine(d))mine=d;if(d.is_active)act=d;});
    var here=!act||(mine&&act.id===mine.id);
    h+='<div class="spo-pan-sec">'+(act?'Listening on':'Nothing playing')+'</div>';
    if(act&&!here){
      h+='<button type="button" class="spo-row act" data-d="'+esc(act.id)+'"><span class="ic">'+devIcon(act.type)+'</span><span class="t"><b>'+esc(act.name)+'</b><small>'+devType(act.type)+'</small></span>'+EQ+'</button>';
      if(act.volume_percent!=null&&act.supports_volume!==false)h+='<div class="spo-dvol">'+I.volLo+'<input type="range" min="0" max="100" value="'+(+act.volume_percent||0)+'" data-v="'+esc(act.id)+'" aria-label="Volume">'+I.volHi+'</div>';
    }
    h+='<button type="button" class="spo-row'+(here&&act?' act':'')+'" data-d="'+(mine?esc(mine.id):'@me')+'"><span class="ic">'+I.phone+'</span><span class="t"><b>This phone</b><small>'+(here&&act?'Playing on '+esc(myName()):esc(myName()))+'</small></span>'+EQ+'</button>';
    h+='<button type="button" class="spo-row" data-p="out"><span class="ic">'+I.bt+'</span><span class="t"><b>Phone speaker or Bluetooth</b><small>Headphones, car, speakers paired to this phone</small></span></button>';
    var others=ds.filter(function(d){return !d.is_active&&!isMine(d)&&!d.is_restricted;});
    if(others.length){
      h+='<div class="spo-pan-sec">Other devices</div>';
      others.forEach(function(d){h+='<button type="button" class="spo-row" data-d="'+esc(d.id)+'"><span class="ic">'+devIcon(d.type)+'</span><span class="t"><b>'+esc(d.name)+'</b><small>'+devType(d.type)+'</small></span><span class="go">Play here</span></button>';});
    }else h+='<div class="spo-pan-hint">Open Spotify on a laptop, TV or speaker signed in to the same account and it shows up here.</div>';
    h+='<button type="button" class="spo-pan-more" data-p="spdev">Open Spotify’s device list</button>';
    var b=byId('spo-pan-b'),st=b.scrollTop;
    if(b.querySelector('input[data-v]:active'))return;
    b.innerHTML=h;b.scrollTop=st;
  }).catch(function(e){if(id===pan.req&&pan.k==='dev'&&!quiet)panFail('dev',e);});
}
function panRefresh(quiet){if(pan.k==='queue')panQueue(quiet);else if(pan.k==='dev')panDevices(quiet);}
function openPan(k){
  if(!sheet)return;
  if(!sheetOpen)openSheet();
  if(sheet.classList.contains('lyr'))toggleLyr(false);
  var same=pan.k===k;
  pan.k=k;sheet.classList.add('pan');
  byId('spo-pan-t').textContent=k==='queue'?'Queue':'Play on';
  byId('spo-queue').classList.toggle('on',k==='queue');
  byId('spo-devices').classList.toggle('on',k==='dev');
  if(!same)byId('spo-pan-b').innerHTML='';
  panRefresh(same);
  pan.key=trackKey();
  clearInterval(pan.t);
  pan.t=setInterval(function(){
    if(!sheetOpen||!pan.k||document.hidden||pan.busy)return;
    var k2=trackKey();
    if(k2!==pan.key){pan.key=k2;panRefresh(true);}
    else if(pan.k==='dev'||(++pan.n)%3===0)panRefresh(true);
  },5000);
}
function closePan(){
  pan.k='';pan.req++;pan.busy=false;clearInterval(pan.t);
  if(sheet)sheet.classList.remove('pan');
  var q=byId('spo-queue'),d=byId('spo-devices');if(q)q.classList.remove('on');if(d)d.classList.remove('on');
}
function togglePan(k){if(pan.k===k&&sheet.classList.contains('pan'))closePan();else openPan(k);}
window.spoOpenPanel=function(k){if(!sheet&&document.body)buildSheet();openPan(k==='devices'?'dev':k);};
function skipTo(i,row){
  if(pan.busy)return;
  pan.busy=true;haptic();
  if(row)row.classList.add('busy');
  var n=Math.min(i+1,40),p=Promise.resolve();
  if(n===1){act('spl-next');}
  else for(var k=0;k<n;k++)p=p.then(function(){return api('/me/player/next',{method:'POST'});}).then(function(){return new Promise(function(r){setTimeout(r,160);});});
  p.then(function(){setTimeout(function(){pan.busy=false;panRefresh(true);update();},n===1?700:900);})
   .catch(function(e){pan.busy=false;panFail('queue',e);});
}
function playOn(id,row){
  haptic();
  if(id==='@me'){closeSheet();setTimeout(window.spoOpenDevices,280);return;}
  if(row)row.classList.add('busy');
  api('/me/player',{method:'PUT',body:{device_ids:[id],play:true}})
    .then(function(){setTimeout(function(){panRefresh(true);update();},1100);})
    .catch(function(e){if(row)row.classList.remove('busy');panFail('dev',e);});
}
function bindPan(){
  var b=byId('spo-pan-b'),vt=null;
  b.addEventListener('click',function(e){
    var r=e.target.closest('button');if(!r)return;
    var p=r.getAttribute('data-p');
    if(p==='retry'){haptic();panRefresh();return;}
    if(p==='spqueue'){closePan();closeSheet();setTimeout(function(){act('spl-queue');},300);return;}
    if(p==='spdev'){closePan();closeSheet();setTimeout(window.spoOpenDevices,300);return;}
    if(p==='out'){haptic();try{AndBridge.openAudioOutput();}catch(err){}return;}
    if(r.hasAttribute('data-d')){if(!r.classList.contains('act'))playOn(r.getAttribute('data-d'),r);return;}
    if(r.hasAttribute('data-i')){var i=+r.getAttribute('data-i');if(i>=0)skipTo(i,r);}
  });
  b.addEventListener('input',function(e){
    var v=e.target.closest('input[data-v]');if(!v)return;
    if(vt)clearTimeout(vt);
    vt=setTimeout(function(){api('/me/player/volume?volume_percent='+(+v.value)+'&device_id='+encodeURIComponent(v.getAttribute('data-v')),{method:'PUT'}).catch(function(){});},220);
  });
}

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
  function spotifyTitle(){return txt(qs('a[data-testid=context-item-link]'));}
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
    var before=spotifyTitle();
    var title=txt(row.querySelector('a[href*="/track/"],a[href*="/episode/"]')||row.querySelector('[dir=auto]'));
    row.dispatchEvent(new MouseEvent('dblclick',{bubbles:true,cancelable:true,view:window,detail:2}));
    showPending(row,title);
    /* fallback only when nothing changed at all, so it can never pause a song */
    setTimeout(function(){
      if(!row.isConnected)return;
      var cur=spotifyTitle();
      if(cur!==before||(title&&cur===title))return;
      var b=rowPlayButton(row);if(b)b.click();
    },1200);
  },true);

  /* Show the tapped song in the player right away. Spotify takes a moment to load it,
     and the player used to keep showing the old song until then. splUpdate keeps this
     until Spotify's own player moves off the old song (or 8 s pass and nothing came). */
  function showPending(row,title){
    var tk=byId('spl-track');if(!tk||!title)return;
    var cur=spotifyTitle();if(title===cur)return;
    var art=row.querySelector('a[href*="/artist/"],a[href*="/show/"]');
    var img=row.querySelector('img');
    if(!img&&/^\/album\//.test(location.pathname))img=qs('main img');
    var dur='',cells=row.querySelectorAll('[aria-colindex]');
    for(var i=cells.length-1;i>=0&&!dur;i--){var m=txt(cells[i]).match(/(?:^|\s)(\d{1,2}:\d\d(?::\d\d)?)\s*$/);if(m)dur=m[1];}
    var ar=byId('spl-artist'),ci=byId('spl-cover-img');
    window.__spoPend={t:title,old:cur,until:Date.now()+8000,dur:dur,
      prev:{t:tk.textContent,a:ar?ar.textContent:'',c:ci&&ci.getAttribute('src')||''}};
    tk.textContent=title;
    if(ar)ar.textContent=art?txt(art):'';
    if(ci)ci.src=img&&img.src||'';
    var f=byId('spl-fill');if(f)f.style.transform='scaleX(0)';
    var fe=byId('spl-fill-edge');if(fe)fe.style.transform='scaleX(0)';
    var hd=byId('spl-handle');if(hd)hd.style.left='0%';
    var ps=byId('spl-pos');if(ps)ps.textContent='0:00';
    var ds=byId('spl-dur');if(ds&&dur)ds.textContent=dur;
    try{update();}catch(e){}
  }
})();

/* ---------- warm up the audio connection ----------
   When a finger lands on a song (or a play button), open the connection to Spotify's
   audio servers right away, so the song's audio doesn't wait for a fresh handshake. The
   servers this account actually streams from are learned as songs play. */
(function(){
  if(window.__spoWarm)return;window.__spoWarm=true;
  var hosts=[];
  try{hosts=JSON.parse(localStorage.getItem('spoAudioHosts')||'[]')||[];}catch(e){}
  if(!hosts.length)hosts=['https://audio-ak-spotify-com.akamaized.net','https://audio4-ak-spotify-com.akamaized.net','https://audio-fa.scdn.co'];
  try{
    new PerformanceObserver(function(list){
      list.getEntries().forEach(function(en){
        var u=String(en.name||'');if(u.indexOf('/audio/')===-1)return;
        var o='';try{o=new URL(u).origin;}catch(e){return;}
        if(!o||hosts[0]===o)return;
        hosts=[o].concat(hosts.filter(function(h){return h!==o;})).slice(0,4);
        try{localStorage.setItem('spoAudioHosts',JSON.stringify(hosts));}catch(e){}
      });
    }).observe({type:'resource',buffered:true});
  }catch(e){}
  var at=0;
  function warm(){
    var n=Date.now();if(n-at<4000)return;at=n;
    hosts.concat(['https://seektables.scdn.co']).forEach(function(h){
      var id='spo-pc-'+h.replace(/\W/g,''),old=byId(id);
      if(old)old.remove();
      var l=document.createElement('link');l.id=id;l.rel='preconnect';l.href=h;l.crossOrigin='anonymous';
      (document.head||document.documentElement).appendChild(l);
    });
  }
  window.spoWarmAudio=warm;
  document.addEventListener('touchstart',function(e){
    var t=e.target;if(!t||!t.closest)return;
    if(t.closest('[data-testid=tracklist-row],[data-testid=play-button],[data-testid=control-button-playpause],[data-encore-id=card],#spotilolPlayerControls,#spoNP,#spoSrv,#ss-sheet'))warm();
  },{passive:true,capture:true});
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
  /* Dev shows only with Developer mode on in Settings; it stays in the menu (hidden) as the template for "Your stats" */
  if(!window.__spoDev)li.style.display='none';
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

/* ---------- phone-style headers ---------- */
function mobileHero(){
  var sec=qs('main section[data-testid$="-page"]:not([data-testid=artist-page]):not([data-testid=home-page]):not([data-testid=search-page])');
  if(!sec)return;
  if(sec.querySelector('[data-spo-hero]'))return;
  var h1=sec.querySelector('h1');if(!h1)return;
  var head=sec.firstElementChild;if(!head||!head.contains(h1))return;
  var imgs=head.querySelectorAll('img'),img=null,best=0;
  for(var i=0;i<imgs.length;i++){var w=imgs[i].getBoundingClientRect().width;if(w>best&&w>=80){best=w;img=imgs[i];}}
  if(!img)return;
  var a=img;while(a&&!a.contains(h1))a=a.parentElement;
  if(!a||a===sec)return;
  var wrap=img;while(wrap.parentElement&&wrap.parentElement!==a)wrap=wrap.parentElement;
  a.setAttribute('data-spo-hero','');
  wrap.setAttribute('data-spo-hero-img','');
}

/* ---------- Home shortcuts: the first block of wide link tiles ---------- */
function homeShortcuts(){
  var sec=qs('main section[data-testid=home-page]');
  if(!sec||sec.querySelector('[data-spo-shortcuts]'))return;
  var links=sec.querySelectorAll('a[href]');
  for(var i=0;i<links.length&&i<40;i++){
    var tile=links[i];
    while(tile.parentElement&&tile.parentElement!==sec){
      var p=tile.parentElement,n=p.children.length;
      if(n>=4&&n<=10){
        var ok=0;
        for(var j=0;j<n;j++){var c=p.children[j],r=c.getBoundingClientRect();if(c.querySelector('img')&&r.width>r.height*2&&r.height<90)ok++;}
        if(ok>=n-1){p.setAttribute('data-spo-shortcuts','');return;}
        break;
      }
      tile=p;
    }
  }
}

/* ---------- "Offline mode" label ---------- */
function offlineLabel(){
  if(!byId('spoOffline')){
    var d=document.createElement('div');d.id='spoOffline';
    d.innerHTML='<svg viewBox="0 0 16 16" fill="currentColor"><path d="M1.3.3 15.7 14.7l-1 1-3.3-3.3A6.5 6.5 0 0 1 8 13.5V12a5 5 0 0 0 2.3-.6L8.9 10A3.5 3.5 0 0 1 8 10.5V9l-1-1H2.6A5.6 5.6 0 0 0 2 8a6 6 0 0 0 .4 2l-1.4.5A7.5 7.5 0 0 1 .5 8c0-1.3.3-2.5.9-3.6L.3 1.3z"/></svg>Offline mode';
    document.body.appendChild(d);
  }
  var off=navigator.onLine===false;
  if(root().classList.contains('spo-offline')!==off)root().classList.toggle('spo-offline',off);
}
window.addEventListener('online',offlineLabel);window.addEventListener('offline',offlineLabel);

/* ---------- route classes and the back button ---------- */
var lastPath='';
function routes(){
  var p=location.pathname||'/';
  if(p!==lastPath){
    lastPath=p;
    var r=root(),home=(p==='/'||p===''),srch=p.indexOf('/search')===0;
    var m={'spo-route-home':home,'spo-route-search':srch,'spo-route-artist':p.indexOf('/artist/')===0,'spo-route-sub':!home&&!srch};
    for(var k in m){if(r.classList.contains(k)!==m[k])r.classList.toggle(k,m[k]);}
  }
  if(!byId('spoBackBtn')&&document.body){
    var b=document.createElement('button');b.id='spoBackBtn';b.type='button';b.setAttribute('aria-label','Back');
    b.innerHTML=SV+'<path d="M15 5l-7 7 7 7"/></svg>';
    b.onclick=function(){haptic();history.back();};
    document.body.appendChild(b);
  }
}

/* playlists whose header is a photo instead of a cover get the tall artist-style header */
function imgHeader(){
  var on=!!qs('.before-scroll-node [data-testid=background-image]')&&location.pathname.indexOf('/artist/')!==0;
  var r=root();if(r.classList.contains('spo-imghdr')!==on)r.classList.toggle('spo-imghdr',on);
}

/* ---------- start on Home, not with Library open ---------- */
var libByUser=false,bootAt=Date.now();
document.addEventListener('click',function(e){
  if(e.isTrusted&&e.target.closest&&e.target.closest('#Desktop_LeftSidebar_Id,#global-nav-bar'))libByUser=true;
},true);
function homeFirst(){
  if(libByUser||Date.now()-bootAt>20000)return;
  if(libOpen())toggleLib();
}

/* ---------- back button ---------- */
window.spoBack=function(){
  var po=byId('spoOut');if(po&&po.classList.contains('open')){po.classList.remove('open');return true;}
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
  routes();mobileHero();imgHeader();homeFirst();homeShortcuts();offlineLabel();
  recoverCrash();
  artWall();
  hookPlayer();
  if(window.__spoTabs!==false)buildTabs();
  syncTabs();
},500);
})();
"""
}
