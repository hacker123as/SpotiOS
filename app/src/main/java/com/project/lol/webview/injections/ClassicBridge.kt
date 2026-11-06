package com.project.lol.webview.injections

object ClassicBridge {
    const val CONTENT = """
        (function(){
            if(window.__spotilolUseProxy) {
                window.mngFetch = function(url, opts) {
                    return (window.fetch || oriFetch).apply(window, arguments);
                };
            } else {
                // Native requests answer through window.__nfCb so the page never waits on them
                // (AndBridge.nFetch blocks the JS thread for the whole round trip). The id
                // prefix keeps a late answer meant for a previous page load from landing here.
                var nfPre = Math.random().toString(36).slice(2) + '_', nfSeq = 0, nfWait = {};
                window.__nfCb = function(id, raw){
                    var f = nfWait[id];
                    if(f){ delete nfWait[id]; f(raw); }
                };
                /* SpoNet: the origin-checked channel (PlayerHost). When it's there, AndBridge's
                   nFetch/nFetchAsync refuse, so it is used first. */
                var net = window.SpoNet;
                if(net && !net.__spoHooked){
                    net.__spoHooked = 1;
                    net.addEventListener('message', function(ev){
                        try { var m = JSON.parse(ev.data); if(window.__nfCb) window.__nfCb(m.id, m.raw); } catch(e){}
                    });
                }
                function nativeRaw(url, optsJson){
                    if(window.SpoNet && typeof window.SpoNet.postMessage === 'function'){
                        return new Promise(function(resolve){
                            var id = nfPre + (++nfSeq);
                            var guard = setTimeout(function(){
                                if(nfWait[id]){ delete nfWait[id]; resolve('{"status":0,"body":"timeout","headers":{}}'); }
                            }, 30000);
                            nfWait[id] = function(raw){ clearTimeout(guard); resolve(raw); };
                            try { window.SpoNet.postMessage(JSON.stringify({id: id, url: String(url), opts: optsJson})); }
                            catch(e){ delete nfWait[id]; clearTimeout(guard); resolve('{"status":0,"body":"bridge","headers":{}}'); }
                        });
                    }
                    if(typeof AndBridge.nFetchAsync !== 'function') return Promise.resolve(AndBridge.nFetch(url, optsJson));
                    return new Promise(function(resolve){
                        var id = nfPre + (++nfSeq);
                        var guard = setTimeout(function(){
                            if(nfWait[id]){ delete nfWait[id]; resolve('{"status":0,"body":"timeout","headers":{}}'); }
                        }, 30000);
                        nfWait[id] = function(raw){ clearTimeout(guard); resolve(raw); };
                        var started = false;
                        try { started = AndBridge.nFetchAsync(id, url, optsJson) === true; } catch(e){}
                        if(!started){ delete nfWait[id]; clearTimeout(guard); resolve(AndBridge.nFetch(url, optsJson)); }
                    });
                }
                window.mngFetch = async function(url, opts){
                    var o = opts || {};
                    var h = o.headers || {};
                    var headers = {};
                    if(typeof h.forEach === 'function') {
                        h.forEach(function(v,k){ headers[k]=v; });
                    } else if(typeof h.get === 'function') {
                        try {
                            headers['Authorization'] = h.get('Authorization');
                            headers['Client-Token'] = h.get('Client-Token');
                            headers['Content-Type'] = h.get('Content-Type');
                        } catch(e){}
                    } else {
                        headers = h;
                    }
                    var body = null;
                    if(o.body) {
                        body = (typeof o.body === 'string') ? o.body : JSON.stringify(o.body);
                    }
                    var raw = await nativeRaw(url, JSON.stringify({
                        method: o.method || 'GET',
                        headers: headers,
                        body: body
                    }));
                    var data;
                    try { data = JSON.parse(raw); }
                    catch(e) { throw new Error('Bad native response: ' + raw); }
                    if(!data || data.status === 0) {
                        throw new Error('network error: ' + (data ? data.body : 'unknown'));
                    }
                    return {
                        status: data.status,
                        ok: data.status >= 200 && data.status < 300,
                        headers: new Headers(data.headers || {}),
                        url: url,
                        json: function(){ return Promise.resolve(JSON.parse(data.body || '{}')); },
                        text: function(){ return Promise.resolve(data.body || ''); },
                        arrayBuffer: function(){ return Promise.resolve(new TextEncoder().encode(data.body || '').buffer); },
                        clone: function(){ return this; }
                    };
                };
            }
        })();
    
    """
}
