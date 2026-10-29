package cn.local.campuswater;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;
import java.security.MessageDigest;

class RecordStore extends SQLiteOpenHelper {
  RecordStore(Context c){super(c,"water-records.db",null,1);}
  public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE records(account TEXT, id TEXT, stamp TEXT, water INTEGER, data TEXT, PRIMARY KEY(account,id))");}
  public void onUpgrade(SQLiteDatabase db,int a,int b){}
  void clear(){synchronized(RecordStore.class){SQLiteDatabase db=getWritableDatabase();db.execSQL("PRAGMA secure_delete=ON");db.delete("records",null,null);}}
  void merge(String account,JSONArray rows)throws Exception{
    SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
    try{for(int i=0;i<rows.length();i++){
      JSONObject j=rows.getJSONObject(i);String id=j.optString("id");
      if(id.isEmpty()){byte[] digest=MessageDigest.getInstance("SHA-256").digest((j.optString("date")+"|"+j.optString("time")+"|"+j.optString("signedAmount")+"|"+j.optString("fee")+"|"+j.optString("location")).getBytes("UTF-8"));StringBuilder h=new StringBuilder();for(byte b:digest)h.append(String.format("%02x",b&255));id=h.toString();}
      ContentValues v=new ContentValues();v.put("account",account);v.put("id",id);v.put("stamp",j.optString("date")+" "+j.optString("time"));v.put("water",j.optBoolean("water")?1:0);v.put("data",j.toString());db.insertWithOnConflict("records",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }db.setTransactionSuccessful();}finally{db.endTransaction();}
  }
  JSONArray list(String account,boolean all,int limit){JSONArray a=new JSONArray();try(Cursor c=getReadableDatabase().query("records",new String[]{"data"},"account=?"+(all?"":" AND water=1"),new String[]{account},null,null,"stamp DESC",String.valueOf(limit))){while(c.moveToNext())try{a.put(new JSONObject(c.getString(0)));}catch(Exception ignored){}}return a;}
}
