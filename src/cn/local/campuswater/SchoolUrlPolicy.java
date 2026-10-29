package cn.local.campuswater;
import java.net.URI;
import java.util.Locale;

final class SchoolUrlPolicy {
  static boolean allowed(String value){
    try{URI u=new URI(value);String scheme=u.getScheme(),host=u.getHost();
      if(host==null||u.getRawUserInfo()!=null||u.isOpaque())return false;
      scheme=scheme==null?"":scheme.toLowerCase(Locale.ROOT);host=host.toLowerCase(Locale.ROOT);
      if(!scheme.equals("http")&&!scheme.equals("https"))return false;
      int port=u.getPort()<0?(scheme.equals("https")?443:80):u.getPort();
      if(host.equals("111.14.213.18"))return port==7072;
      if(host.equals("ykt.sdmcjn.edu.cn"))return port==80||port==443||port==7248||port==8081;
      return (host.equals("sdmcjn.edu.cn")||host.equals("authserver.sdmcjn.edu.cn")||host.equals("ehall.sdmcjn.edu.cn"))&&(port==80||port==443);
    }catch(Exception e){return false;}
  }
  private SchoolUrlPolicy(){}
}
