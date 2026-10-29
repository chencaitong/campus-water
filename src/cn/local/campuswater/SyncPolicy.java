package cn.local.campuswater;

final class SyncPolicy {
  static final long INTERVAL_MS=60L*60L*1000L;
  static boolean due(long now,long lastRequest){return lastRequest<=0||now<lastRequest||now-lastRequest>=INTERVAL_MS;}
  static long delay(long now,long lastRequest){return due(now,lastRequest)?0:INTERVAL_MS-(now-lastRequest);}
  private SyncPolicy(){}
}
