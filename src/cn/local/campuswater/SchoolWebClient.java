package cn.local.campuswater;
import android.webkit.*;
import java.io.ByteArrayInputStream;

class SchoolWebClient extends WebViewClient {
  @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return !SchoolUrlPolicy.allowed(request.getUrl().toString());}
  @Override public boolean shouldOverrideUrlLoading(WebView view,String url){return !SchoolUrlPolicy.allowed(url);}
  @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
    if(SchoolUrlPolicy.allowed(request.getUrl().toString()))return null;
    return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",java.util.Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));
  }
  @Override public void onReceivedSslError(WebView view,SslErrorHandler handler,android.net.http.SslError error){handler.cancel();}
}
