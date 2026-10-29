package cn.local.campuswater;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/** Only ciphertext is stored in preferences. Cookie/WebView storage remains separate. */
final class WaterSessionStore {
  private static final String ALIAS="campus-water-session-v1";
  private static javax.crypto.SecretKey key()throws Exception{
    KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
    if(!ks.containsAlias(ALIAS)){KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}
    return (javax.crypto.SecretKey)ks.getKey(ALIAS,null);
  }
  static boolean save(Context c,String value){
    try{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
      JSONObject box=new JSONObject();box.put("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP));box.put("data",Base64.encodeToString(cipher.doFinal(value.getBytes("UTF-8")),Base64.NO_WRAP));
      return c.getSharedPreferences("water",0).edit().putString("encryptedSession",box.toString()).remove("session").commit();
    }catch(Exception e){return false;}
  }
  static String read(Context c){
    SharedPreferences p=c.getSharedPreferences("water",0);
    if(p.contains("session")){String old=p.getString("session","{}");if(save(c,old))return old;p.edit().remove("session").commit();return "{}";}
    try{JSONObject box=new JSONObject(p.getString("encryptedSession",""));Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(box.getString("iv"),Base64.NO_WRAP)));return new String(cipher.doFinal(Base64.decode(box.getString("data"),Base64.NO_WRAP)),"UTF-8");}catch(Exception e){return "{}";}
  }
  private WaterSessionStore(){}
}
