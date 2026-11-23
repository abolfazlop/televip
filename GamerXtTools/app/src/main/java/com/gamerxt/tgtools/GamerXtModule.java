package com.gamerxt.tgtools;

import android.content.SharedPreferences;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import io.github.libxposed.api.XposedModule;

public class GamerXtModule extends XposedModule {
 private static final String TAG="GamerXt";
 private SharedPreferences prefs;
 private volatile long lastOwnDelete,lastSend;
 public GamerXtModule(){}
 @Override public void onModuleLoaded(ModuleLoadedParam param){}
 @Override public void onPackageReady(PackageReadyParam param){
  if(!param.isFirstPackage()) return;
  ClassLoader cl=param.getClassLoader();
  try{prefs=getRemotePreferences(Keys.GROUP);}catch(Throwable t){log(Log.ERROR,TAG,"prefs failed",t);}
  run("network",()->hookNetwork(cl)); run("send",()->hookSend(cl));
  run("deleted",()->hookDeleted(cl)); run("ui",()->hookUi(cl));
 }
 private interface Task{void run() throws Throwable;}
 private void run(String n,Task t){try{t.run();}catch(Throwable e){log(Log.ERROR,TAG,n+" hooks failed",e);}}
 private boolean on(String k){try{return prefs!=null&&prefs.getBoolean(k,false);}catch(Throwable t){return false;}}
 private static Class<?> cls(ClassLoader cl,String n){try{return Class.forName(n,false,cl);}catch(Throwable t){return null;}}
 private int hookAll(Class<?> c,String name,XposedInterface.Hooker h){
  if(c==null)return 0; int n=0;
  for(Method m:c.getDeclaredMethods())if(m.getName().equals(name)){hook(m).intercept(h);n++;}
  if(n==0)log(Log.WARN,TAG,"no method "+c.getSimpleName()+"."+name); return n;
 }
 private static Object emptyResult(Method m){
  Class<?> r=m.getReturnType(); if(r==void.class)return null;if(r==boolean.class)return false;
  if(r==int.class)return 0;if(r==long.class)return 0L;if(List.class.isAssignableFrom(r))return new ArrayList<>();return null;
 }
 private void hookNetwork(ClassLoader cl){
  hookAll(cls(cl,"org.telegram.tgnet.ConnectionsManager"),"sendRequest",chain->{
   Object req=chain.getArg(0); if(req!=null&&shouldBlock(req))return 0;return chain.proceed();
  });
 }
 private boolean shouldBlock(Object req){
  switch(req.getClass().getSimpleName()){
   case "TL_messages_setTyping":case "TL_messages_setEncryptedTyping":return on("hide_typing");
   case "TL_messages_readHistory":case "TL_messages_readEncryptedHistory":case "TL_messages_readMessageContents":return on("hide_read_private")&&!recentlySent();
   case "TL_channels_readHistory":case "TL_channels_readMessageContents":case "TL_messages_readDiscussion":return on("hide_read_channel")&&!recentlySent();
   case "TL_account_updateStatus":try{java.lang.reflect.Field f=req.getClass().getField("offline");return on("hide_online")&&!f.getBoolean(req);}catch(Throwable t){return false;}
   case "TL_stories_readStories":case "TL_stories_incrementStoryViews":return on("hide_story_view");
   case "TL_messages_getSponsoredMessages":case "TL_help_getPromoData":return on("block_ads");
   default:return false;
  }
 }
 private boolean recentlySent(){return on("mark_read_after_send")&&System.currentTimeMillis()-lastSend<3000;}
 private void hookSend(ClassLoader cl){
  hookAll(cls(cl,"org.telegram.messenger.SendMessagesHelper"),"sendMessage",chain->{lastSend=System.currentTimeMillis();return chain.proceed();});
 }
 private void hookDeleted(ClassLoader cl){
  Class<?> mc=cls(cl,"org.telegram.messenger.MessagesController"),ms=cls(cl,"org.telegram.messenger.MessagesStorage"),nc=cls(cl,"org.telegram.messenger.NotificationsController");
  XposedInterface.Hooker mark=chain->{lastOwnDelete=System.currentTimeMillis();return chain.proceed();};
  XposedInterface.Hooker skip=chain->{if(on("keep_deleted")&&System.currentTimeMillis()-lastOwnDelete>8000)return emptyResult((Method)chain.getExecutable());return chain.proceed();};
  hookAll(mc,"deleteMessages",mark);hookAll(mc,"deleteDialog",mark);hookAll(mc,"updateDialogsWithDeletedMessages",skip);
  hookAll(ms,"markMessagesAsDeleted",skip);hookAll(ms,"updateDialogsWithDeletedMessages",skip);hookAll(nc,"removeDeletedMessagesFromNotifications",skip);
 }
 private void hookUi(ClassLoader cl){
  hookAll(cls(cl,"org.telegram.messenger.LocaleController"),"formatShortNumber",chain->{
   if(!on("exact_numbers"))return chain.proceed();Object n=chain.getArg(0);if(!(n instanceof Number))return chain.proceed();
   try{Object r=chain.getArg(1);if(r instanceof int[]&&((int[])r).length>0)((int[])r)[0]=((Number)n).intValue();}catch(Throwable ignored){}
   return String.valueOf(((Number)n).longValue());
  });
  hookAll(cls(cl,"org.telegram.messenger.MessagesController"),"storiesEnabled",chain->on("disable_stories")?false:chain.proceed());
  hookAll(cls(cl,"org.telegram.messenger.MessageObject"),"isSecretMedia",chain->on("secret_media")?false:chain.proceed());
  hookAll(cls(cl,"org.telegram.ui.ChatActivity"),"updatePinnedMessageView",chain->on("hide_pinned")?emptyResult((Method)chain.getExecutable()):chain.proceed());
  hookAll(cls(cl,"org.telegram.PhoneFormat.PhoneFormat"),"format",chain->{
   Object res=chain.proceed();if(!on("hide_phone"))return res;
   try{Object arg=chain.getArg(0);String mine=ownPhone(cl);if(arg instanceof String&&mine!=null){String a=((String)arg).replaceAll("\\D","");if(!a.isEmpty()&&a.equals(mine))return "••• ••••";}}catch(Throwable ignored){}
   return res;
  });
 }
 private static String ownPhone(ClassLoader cl)throws Throwable{
  Class<?> uc=Class.forName("org.telegram.messenger.UserConfig",false,cl);int acc=uc.getField("selectedAccount").getInt(null);
  Object inst=uc.getMethod("getInstance",int.class).invoke(null,acc);Object user=inst.getClass().getMethod("getCurrentUser").invoke(inst);
  if(user==null)return null;String p=(String)user.getClass().getField("phone").get(user);return p==null?null:p.replaceAll("\\D","");
 }
}
