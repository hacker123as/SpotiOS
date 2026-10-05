package com.project.lol.webview.injections

/**
 * Keeps SpotiOS reachable in Spotify Connect (early payload, before Spotify's scripts).
 *
 * The web player stays in other devices' lists only while its "dealer" WebSocket to Spotify
 * is up. In the background Android slows the page's timers, so Spotify's own 30 s pings run
 * late and the socket can die quietly (a network switch leaves it half-open). This tracks the
 * dealer socket, and window.spoKeepAlive(), called every 20 s by the media service from
 * native code, pings it, drops a socket that stopped answering so Spotify reconnects at once,
 * and in Server Mode reloads the player as a last resort when nothing is playing.
 *
 * It also reads Spotify Connect state ("cluster": which device is active, what's playing,
 * the device list) from the dealer messages and the player's registration, for the Server
 * screen (window.__spoConn.onCluster listeners).
 */
object ConnectKeepAlive {
    const val CONTENT = """
(function(){
  if(window.__spoKA)return;window.__spoKA=1;
  var S={ws:null,lastMsg:0,lastOpen:0,lastClose:0,opened:0,reconnects:0,forced:0,since:Date.now(),
         myId:'',cluster:null,clusterAt:0,onCluster:[],pingAt:0};
  window.__spoConn=S;

  function emit(c){
    if(!c||typeof c!=='object')return;
    S.cluster=c;S.clusterAt=Date.now();
    for(var i=0;i<S.onCluster.length;i++){try{S.onCluster[i](c);}catch(e){}}
  }
  function clusterOf(o){
    if(!o||typeof o!=='object')return null;
    if(o.cluster&&typeof o.cluster==='object')return o.cluster;
    if(o.active_device_id!==undefined||(o.devices&&o.player_state))return o;
    return null;
  }
  function gunzip(b64){
    try{
      if(typeof DecompressionStream!=='function')return Promise.resolve(null);
      var bin=atob(b64),u=new Uint8Array(bin.length);
      for(var i=0;i<bin.length;i++)u[i]=bin.charCodeAt(i);
      var st=new Blob([u]).stream().pipeThrough(new DecompressionStream('gzip'));
      return new Response(st).text();
    }catch(e){return Promise.resolve(null);}
  }
  function readPayload(p,gz){
    if(p&&typeof p==='object'){emit(clusterOf(p));return;}
    if(typeof p!=='string')return;
    if(!gz){try{emit(clusterOf(JSON.parse(p)));return;}catch(e){}}
    gunzip(p).then(function(t){if(t){try{emit(clusterOf(JSON.parse(t)));}catch(e){}}}).catch(function(){});
  }
  function onDealerText(d){
    if(d.indexOf('connect-state')===-1&&d.indexOf('cluster')===-1)return;
    var m;try{m=JSON.parse(d);}catch(e){return;}
    if(!m||String(m.uri||'').indexOf('cluster')===-1)return;
    var h=m.headers||{},gz=/gzip/i.test(String(h['Transfer-Encoding']||h['transfer-encoding']||''));
    var ps=m.payloads||[];
    for(var i=0;i<ps.length;i++)readPayload(ps[i],gz);
  }

  var Base=window.WebSocket;
  if(typeof Base==='function'){
    var W=function(url,protocols){
      var s=protocols===undefined?new Base(url):new Base(url,protocols);
      try{
        if(/dealer/i.test(String(url))){
          if(S.ws&&S.ws!==s)S.reconnects++;
          S.ws=s;S.lastMsg=Date.now();
          s.addEventListener('open',function(){if(S.ws===s){S.lastOpen=Date.now();S.opened++;S.lastMsg=Date.now();}});
          s.addEventListener('message',function(ev){
            if(S.ws!==s)return;
            S.lastMsg=Date.now();
            var d=ev.data;
            if(typeof d==='string'){try{onDealerText(d);}catch(e){}}
          });
          s.addEventListener('close',function(){if(S.ws===s)S.lastClose=Date.now();});
        }
      }catch(e){}
      return s;
    };
    W.prototype=Base.prototype;
    W.CONNECTING=0;W.OPEN=1;W.CLOSING=2;W.CLOSED=3;
    W.__splWrapped=true;
    window.WebSocket=W;
  }

  /* This device's Spotify Connect id: from the player's registration requests. */
  var pf=window.fetch;
  if(typeof pf==='function'){
    window.fetch=function(input,init){
      var p=pf.apply(this,arguments);
      try{
        var url=typeof input==='string'?input:((input&&input.url)||'');
        var m=url.match(/connect-state\/v1\/devices\/hobs_([A-Za-z0-9]+)/);
        if(m){
          S.myId=m[1];window.__spoMyId=m[1];
          Promise.resolve(p).then(function(r){
            try{r.clone().json().then(function(j){emit(clusterOf(j));}).catch(function(){});}catch(e){}
          }).catch(function(){});
        }else if(!S.myId&&url.indexOf('/track-playback/v1/devices')!==-1&&init&&typeof init.body==='string'){
          var b=JSON.parse(init.body);
          if(b&&b.device&&b.device.device_id){S.myId=b.device.device_id;window.__spoMyId=S.myId;}
        }
      }catch(e){}
      return p;
    };
  }

  function playing(){
    try{if(typeof window.splIsPlaying==='function')return window.splIsPlaying()===true;}catch(e){}
    try{var a=document.querySelectorAll('audio,video');for(var i=0;i<a.length;i++)if(!a[i].paused)return true;}catch(e){}
    return false;
  }
  function mayReload(){
    try{
      var t=+(sessionStorage.getItem('spoKAReload')||0);
      if(Date.now()-t<5*60000)return false;
      sessionStorage.setItem('spoKAReload',String(Date.now()));
    }catch(e){}
    return true;
  }

  /* Called by the media service every 20 s (and right after a network change). */
  window.spoKeepAlive=function(server,reason){
    var now=Date.now(),ws=S.ws,st=ws?ws.readyState:-1,did='';
    var online=navigator.onLine!==false;
    var onPlayer=location.hostname==='open.spotify.com';
    if(st===1){try{ws.send('{"type":"ping"}');S.pingAt=now;}catch(e){}}
    var quiet=S.lastMsg?now-S.lastMsg:-1;
    if(st===1&&online&&quiet>65000){
      /* Open but silent even though we keep pinging: the connection is dead. Closing it
         makes Spotify reconnect and register SpotiOS again right away. */
      try{ws.close(4000,'stale');}catch(e){}
      S.forced++;S.lastClose=now;did='reconnect';
    }else if(reason==='net'&&st===1){
      var t0=now,w=ws;
      setTimeout(function(){
        if(S.ws===w&&w.readyState===1&&S.lastMsg<t0){try{w.close(4001,'network');}catch(e){}S.forced++;S.lastClose=Date.now();}
      },8000);
    }
    var down=st===-1||st===3;
    var deadFor=down?(S.lastClose?now-S.lastClose:now-S.since):0;
    if(server&&down&&deadFor>90000&&online&&onPlayer&&window.spotAuthToken&&!playing()&&mayReload()){
      did='reload';
      setTimeout(function(){location.reload();},100);
    }
    try{if(typeof window.spoSrvTick==='function')window.spoSrvTick(reason||'tick');}catch(e){}
    return JSON.stringify({
      state:st,msgAgo:quiet,closedAgo:S.lastClose?now-S.lastClose:-1,
      reconnects:S.reconnects,forced:S.forced,opened:S.opened,online:online,playing:playing(),
      did:did,signedIn:onPlayer||location.hostname.indexOf('accounts.')!==0,
      myId:S.myId||'',name:window.__spoDeviceName||'SpotiOS',clusterAgo:S.clusterAt?now-S.clusterAt:-1
    });
  };
})();
"""
}
