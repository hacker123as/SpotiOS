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
 * screen (window.__spoConn.onCluster listeners), and offers window.spoCS: Spotify Connect
 * commands (play, transfer, volume, cluster) sent the way the web player sends its own, on
 * Spotify's internal API instead of the rate-limited public Web API.
 *
 * In Server Mode the page is told it is always visible (document.hidden stays false), so
 * Spotify doesn't stop or slow its player while the screen is off.
 */
object ConnectKeepAlive {
    const val CONTENT = """
(function(){
  if(window.__spoKA)return;window.__spoKA=1;
  var S={ws:null,lastMsg:0,lastOpen:0,lastClose:0,opened:0,reconnects:0,forced:0,since:Date.now(),
         myId:'',obs:'',tpId:'',fromId:'',spc:'',hdr:null,appVer:'',cluster:null,clusterAt:0,inCluster:false,
         onCluster:[],pingAt:0,pullAt:0,missing:0,kickAt:0};
  window.__spoConn=S;

  /* ---------- Server Mode: the page always counts as visible ---------- */
  try{window.__spoKeepVisible=!!(window.AndBridge&&typeof AndBridge.isServerMode==='function'&&AndBridge.isServerMode());}catch(e){}
  function keepVis(){return window.__spoKeepVisible===true||window.__spoServer===true;}
  try{
    var dp=Document.prototype;
    var spoof=function(name,val){
      var d=Object.getOwnPropertyDescriptor(dp,name);
      if(!d||typeof d.get!=='function')return null;
      Object.defineProperty(dp,name,{configurable:true,enumerable:d.enumerable,get:function(){return keepVis()?val:d.get.call(this);}});
      return d.get;
    };
    var realHidden=spoof('hidden',false);
    spoof('visibilityState','visible');spoof('webkitHidden',false);spoof('webkitVisibilityState','visible');
    if(realHidden){
      var stop=function(e){try{if(keepVis()&&realHidden.call(document))e.stopImmediatePropagation();}catch(x){}};
      window.addEventListener('visibilitychange',stop,true);
      window.addEventListener('webkitvisibilitychange',stop,true);
      /* For our own screens: whether anyone can actually see the page. */
      window.__spoRealHidden=function(){try{return realHidden.call(document);}catch(x){return false;}};
    }
  }catch(e){}

  /* ---------- this player's own Spotify Connect id ---------- */
  function okId(v){return typeof v==='string'&&v.length>=16&&v.length<=64&&!/[^A-Za-z0-9]/.test(v);}
  function localId(){
    var v='';
    try{v=localStorage.getItem('_spharmony_device_id')||'';}catch(e){}
    /* The registration only shows the first 35 characters ("hobs_" + id, cut to 40). */
    if(okId(v)&&(!S.obs||v.indexOf(S.obs)===0))return v;
    if(okId(S.tpId))return S.tpId;
    if(okId(S.fromId)&&(!S.obs||S.fromId.indexOf(S.obs)===0))return S.fromId;
    var c=S.cluster,ds=c&&c.devices;
    if(ds&&S.obs){for(var k in ds){if(k.indexOf(S.obs)===0&&k.length>S.obs.length)return k;}}
    return '';
  }
  function noteId(){var id=localId();if(id&&id!==S.myId){S.myId=id;window.__spoMyId=id;}return id;}
  S.localId=noteId;

  function emit(c){
    if(!c||typeof c!=='object')return;
    S.cluster=c;S.clusterAt=Date.now();
    var id=noteId(),ds=c.devices;
    if(ds&&typeof ds==='object')S.inCluster=!!(id&&ds[id]);
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

  /* Learns from the web player's own Connect requests: where they go, the headers they carry
     and this device's id. FetchOverride calls this too, for the requests it sends natively. */
  function plainHeaders(h){
    var o={};
    if(!h)return o;
    try{
      if(typeof h.forEach==='function'&&typeof h.get==='function')h.forEach(function(v,k){o[k]=v;});
      else if(Array.isArray(h))h.forEach(function(p){if(p&&p.length>1)o[p[0]]=p[1];});
      else for(var k in h)if(Object.prototype.hasOwnProperty.call(h,k))o[k]=h[k];
    }catch(e){}
    return o;
  }
  S.learn=function(url,init){
    try{
      url=String(url||'');
      var at=url.indexOf('/connect-state/');
      if(at>0){
        S.spc=url.slice(0,at);
        var hd=plainHeaders(init&&init.headers),keep={};
        for(var k in hd){var lk=k.toLowerCase();if(lk!=='content-length'&&lk!=='content-type')keep[k]=hd[k];if(lk==='spotify-app-version'&&hd[k])S.appVer=hd[k];}
        if(Object.keys(keep).length)S.hdr=keep;
        var o=url.match(/connect-state\/v1\/devices\/hobs_([A-Za-z0-9]+)/);
        if(o)S.obs=o[1];
        var f=url.match(/connect-state\/v1\/(?:player\/command|connect\/[a-z_]+)\/from\/([A-Za-z0-9]+)\/to\//);
        if(f)S.fromId=f[1];
      }
      if(url.indexOf('/track-playback/v1/devices')!==-1&&init&&typeof init.body==='string'&&init.body.indexOf('device_id')!==-1){
        var b=JSON.parse(init.body);
        if(b&&b.device&&okId(b.device.device_id))S.tpId=b.device.device_id;
      }
      if(!S.appVer&&init&&init.headers){var h2=plainHeaders(init.headers);for(var k2 in h2)if(k2.toLowerCase()==='spotify-app-version'&&h2[k2])S.appVer=h2[k2];}
      noteId();
    }catch(e){}
  };

  var pf=window.fetch;
  if(typeof pf==='function'){
    window.fetch=function(input,init){
      var p=pf.apply(this,arguments);
      try{
        var url=typeof input==='string'?input:((input&&input.url)||'');
        S.learn(url,init);
        if(/connect-state\/v1\/devices\/hobs_/.test(url)){
          Promise.resolve(p).then(function(r){
            try{r.clone().json().then(function(j){emit(clusterOf(j));}).catch(function(){});}catch(e){}
          }).catch(function(){});
        }
      }catch(e){}
      return p;
    };
  }

  /* ---------- Spotify Connect commands, the web player's own way ---------- */
  function rid(){var s='';for(var i=0;i<32;i++)s+=(Math.random()*16|0).toString(16);return s;}
  function csHeaders(){
    var h={};
    if(S.hdr)for(var k in S.hdr)h[k]=S.hdr[k];
    for(var k2 in h){var lk=k2.toLowerCase();if(lk==='authorization'||lk==='client-token')delete h[k2];}
    if(window.spotAuthToken)h['Authorization']=window.spotAuthToken;
    if(window.spotCliToken)h['Client-Token']=window.spotCliToken;
    var hasPlat=false,hasVer=false;
    for(var k3 in h){var l3=k3.toLowerCase();if(l3==='app-platform')hasPlat=true;if(l3==='spotify-app-version')hasVer=true;}
    if(!hasPlat)h['App-Platform']='WebPlayer';
    if(!hasVer&&S.appVer)h['Spotify-App-Version']=S.appVer;
    h['Content-Type']='application/json';
    return h;
  }
  function csErr(code,status,retry){var e=new Error(code);e.status=status||0;e.retryAfter=retry||0;return e;}
  function csReq(method,path,body){
    if(!window.spotAuthToken)return Promise.reject(csErr('signin'));
    if(navigator.onLine===false)return Promise.reject(csErr('offline'));
    var base=S.spc||'https://spclient.wg.spotify.com';
    var init={method:method,headers:csHeaders()};
    if(body!==undefined)init.body=JSON.stringify(body);
    return Promise.resolve(window.fetch(base+'/connect-state/v1/'+path,init)).then(function(r){
      if(!r)throw csErr('network');
      if(r.status===429){var ra=0;try{ra=+(r.headers&&r.headers.get&&r.headers.get('retry-after'))||0;}catch(e){}throw csErr('http429',429,ra);}
      if(r.status<200||r.status>=300)throw csErr('http'+r.status,r.status);
      return r.text().then(function(t){try{return t?JSON.parse(t):null;}catch(e){return null;}});
    });
  }
  function needId(){var id=noteId();if(!id)throw csErr('noid');return id;}
  var CS={
    id:function(){return noteId();},
    ready:function(){return !!(window.spotAuthToken&&noteId());},
    command:function(endpoint,cmd,target){
      return Promise.resolve().then(function(){
        var me=needId();cmd=cmd||{};cmd.endpoint=endpoint;
        cmd.logging_params=Object.assign({},cmd.logging_params||{},{command_id:rid()});
        return csReq('POST','player/command/from/'+me+'/to/'+(target||me),{command:cmd});
      });
    },
    /* Plays a context (playlist, album, artist, Liked Songs, a single track) on [target]
       (this device when empty), starting at [skipTo] (a track URI or index). */
    play:function(contextUri,skipTo,target,feature){
      var opts={license:'tft',skip_to:{},player_options_override:{}};
      if(typeof skipTo==='number')opts.skip_to={track_index:skipTo};
      else if(skipTo)opts.skip_to={track_uri:skipTo};
      return CS.command('play',{
        context:{uri:contextUri,url:'context://'+contextUri,metadata:{}},
        play_origin:{feature_identifier:feature||'harmony',feature_version:String(window.featVer||'web-player')},
        options:opts
      },target);
    },
    /* Moves playback to [target]; mode is restore (keep playing or paused), resume or pause. */
    transfer:function(target,mode){
      return Promise.resolve().then(function(){
        var me=needId();
        return csReq('POST','connect/transfer/from/'+me+'/to/'+target,{transfer_options:{restore_paused:mode||'restore'},command_id:rid()});
      });
    },
    volume:function(target,frac){
      return Promise.resolve().then(function(){
        var me=needId(),v=Math.max(0,Math.min(65535,Math.round(65535*(+frac||0))));
        return csReq('PUT','connect/volume/from/'+me+'/to/'+target,{volume:v});
      });
    },
    simple:function(endpoint,target){return CS.command(endpoint,{},target);},
    cluster:function(){
      S.pullAt=Date.now();
      return csReq('GET','cluster').then(function(j){var c=clusterOf(j);if(c)emit(c);return c;});
    }
  };
  window.spoCS=CS;

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
    /* Reload only when a socket we saw has been down a while; if the socket was never seen,
       a reload can't help and would only knock SpotiOS off Spotify Connect again. */
    var down=st===3||(st===-1&&S.opened>0);
    var deadFor=down?(S.lastClose?now-S.lastClose:now-S.since):0;
    if(server&&down&&deadFor>90000&&online&&onPlayer&&window.spotAuthToken&&!playing()&&mayReload()){
      did='reload';
      setTimeout(function(){location.reload();},100);
    }
    /* In Server Mode, check every few minutes that Spotify still lists this device, and
       re-register (a quick reconnect) if it dropped out of the list. */
    if(server&&onPlayer&&online&&window.spotAuthToken&&!did&&(reason==='net'||now-S.pullAt>150000)){
      CS.cluster().then(function(c){
        var id=noteId();
        if(!c||!id||!c.devices)return;
        if(c.devices[id]){S.missing=0;return;}
        S.missing++;
        var sock=S.ws;
        if(S.missing>=2&&sock&&sock.readyState===1&&!playing()&&Date.now()-S.kickAt>10*60000){
          S.kickAt=Date.now();S.missing=0;
          try{sock.close(4003,'unlisted');}catch(e){}
          S.forced++;S.lastClose=Date.now();
        }
      }).catch(function(){});
    }
    try{if(typeof window.spoSrvTick==='function')window.spoSrvTick(reason||'tick');}catch(e){}
    var id=noteId();
    return JSON.stringify({
      state:st,msgAgo:quiet,closedAgo:S.lastClose?now-S.lastClose:-1,seen:S.opened>0||!!S.ws,
      reconnects:S.reconnects,forced:S.forced,opened:S.opened,online:online,playing:playing(),
      did:did,signedIn:onPlayer||location.hostname.indexOf('accounts.')!==0,token:!!window.spotAuthToken,
      myId:id,listed:!!(id&&S.inCluster&&S.clusterAt&&now-S.clusterAt<10*60000),
      name:window.__spoDeviceName||'SpotiOS',clusterAgo:S.clusterAt?now-S.clusterAt:-1,
      upFor:now-S.since
    });
  };
})();
"""
}
