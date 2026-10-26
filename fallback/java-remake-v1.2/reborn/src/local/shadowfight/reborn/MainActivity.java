package local.shadowfight.reborn;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.*;
import android.media.*;
import android.os.Bundle;
import android.view.*;
import java.io.*;
import java.nio.*;
import java.util.*;

public class MainActivity extends Activity {
    GameView game;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        game=new GameView(this);setContentView(game);immersive();
    }
    void immersive(){getWindow().getDecorView().setSystemUiVisibility(5894);}
    @Override public void onWindowFocusChanged(boolean has){super.onWindowFocusChanged(has);if(has){immersive();if(game!=null)game.resume();}else if(game!=null)game.suspend();}
    @Override protected void onPause(){super.onPause();game.suspend();}
    @Override protected void onResume(){super.onResume();if(game!=null)game.resume();}
    @Override protected void onDestroy(){game.release();super.onDestroy();}
    @Override public void onBackPressed(){game.back();}

    final class GameView extends View implements Combat.Events {
        static final int HOME=0,LADDER=1,ARMORY=2,HELP=3,FIGHT=4;
        final int GOLD=Color.rgb(223,188,111),CREAM=Color.rgb(245,230,197),MUTED=Color.rgb(178,174,156),DARK=Color.rgb(16,24,27),TEAL=Color.rgb(106,178,169);
        final Paint p=new Paint(3); final Path path=new Path();
        final HashMap<String,Bitmap> art=new HashMap<>();
        final HashMap<String,Motion> motions=new HashMap<>();
        final HashMap<String,Integer> sounds=new HashMap<>();
        final ArrayList<Button> buttons=new ArrayList<>(); final ArrayList<Spark> sparks=new ArrayList<>();
        final HashMap<Integer,PointF> touches=new HashMap<>(); final Random visualRandom=new Random(51);
        final SharedPreferences prefs; final SoundPool pool; MediaPlayer music;
        int screen=HOME,coins,progress,weapon,power,armor,difficulty,unlocks=1,survivalBest,survivalStage;
        int menuPressed=-1,joyId=-1,keys,earned; boolean paused,musicOn=true,soundOn=true,awarded,foreground=true;
        long last; float time,scale=1,offsetX,offsetY,shake,bannerTime,joyX,joyY; String banner="",musicName="";
        Combat combat; final float[][] pose=new float[67][3]; OriginalModel originalModel; Typeface originalFont;
        GameView(Context ctx) {
            super(ctx); setFocusableInTouchMode(true);setLayerType(View.LAYER_TYPE_HARDWARE,null);
            prefs=ctx.getSharedPreferences("shadow_reborn_v1",0);
            coins=999999999;progress=prefs.getInt("progress",0);weapon=prefs.getInt("weapon",0);
            power=prefs.getInt("power",0);armor=prefs.getInt("armor",0);unlocks=7;
            difficulty=prefs.getInt("difficulty",1);survivalBest=prefs.getInt("survivalBest",0);
            musicOn=prefs.getBoolean("music",true);soundOn=prefs.getBoolean("sound",true);
            AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            pool=new SoundPool.Builder().setMaxStreams(8).setAudioAttributes(attrs).build();
            try {
                for(String f:ctx.getAssets().list("")) {
                    if(f.endsWith(".jpg")||f.endsWith(".png")){try(InputStream in=ctx.getAssets().open(f)){art.put(f.substring(0,f.lastIndexOf('.')),BitmapFactory.decodeStream(in));}}
                    else if(f.endsWith(".motion"))motions.put(f.replace(".motion",""),new Motion(f));
                    else if(f.startsWith("snd_")&&f.endsWith(".wav")){try(AssetFileDescriptor fd=ctx.getAssets().openFd(f)){sounds.put(f.replace(".wav",""),pool.load(fd,1));}}
                }
            } catch(Exception e){banner="Some assets could not load. "+e.getMessage();bannerTime=8;}
            try { originalModel=new OriginalModel(ctx); }
            catch(Exception e){ android.util.Log.e("ShadowRecovery","Original model load failed",e); }
            originalFont=Typeface.createFromAsset(ctx.getAssets(),"original_font.ttf");
            setMusic("menu");
        }
        void save(){prefs.edit().putInt("coins",coins).putInt("progress",progress).putInt("weapon",weapon).putInt("power",power).putInt("armor",armor).putInt("unlocks",unlocks).putInt("difficulty",difficulty).putInt("survivalBest",survivalBest).putBoolean("sound",soundOn).putBoolean("music",musicOn).apply();}
        void sound(String n){Integer id=sounds.get(n);if(soundOn&&foreground&&id!=null)pool.play(id,.65f,.65f,1,0,1);}
        void setMusic(String name) {
            if(name.equals(musicName)&&music!=null){if(musicOn&&foreground&&!music.isPlaying())music.start();return;}
            if(music!=null){music.release();music=null;}
            musicName=name;if(!musicOn||!foreground)return;
            try {
                String file=name+".ogg";
                try{getAssets().openFd(file).close();}catch(Exception e){file=name+".wav";}
                AssetFileDescriptor fd=getAssets().openFd(file);music=new MediaPlayer();music.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());fd.close();
                music.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());music.setLooping(true);music.setVolume(.30f,.30f);music.prepare();music.start();
            }catch(Exception e){if(music!=null)music.release();music=null;}
        }
        void suspend(){foreground=false;if(screen==FIGHT&&combat!=null&&!combat.finished)paused=true;touches.clear();joyId=-1;joyX=joyY=0;keys=0;if(music!=null&&music.isPlaying())music.pause();save();last=0;}
        void resume(){foreground=true;last=0;if(musicOn)setMusic(screen==FIGHT?"fight1_samurai_spirit":"menu");invalidate();}
        void release(){pool.release();if(music!=null)music.release();}
        void back(){if(screen==FIGHT&&!combat.finished){paused=!paused;touches.clear();joyId=-1;}else{screen=HOME;setMusic("menu");}invalidate();}
        void notifyText(String s){banner=s;bannerTime=2.5f;}
        void start(int mode,int stage) {
            screen=FIGHT;paused=false;awarded=false;earned=0;sparks.clear();touches.clear();joyId=-1;joyX=joyY=0;
            combat=new Combat(mode,stage,difficulty,weapon,power,armor,System.nanoTime(),this);
            setMusic("fight1_samurai_spirit");last=0;
        }
        @Override public void event(String name,float x,float y,int value) {
            if(name.equals("swish")){sound(weapon==1?"snd_swish_sword1":"snd_swish1");return;}
            if(name.equals("gong")){sound("snd_gong");return;}
            if(name.equals("victory")){sound("snd_win");return;}
            if(name.equals("roundwin")||name.equals("roundloss")){sound("snd_bodyfall1");return;}
            if(name.equals("block")||name.equals("hit")||name.equals("heavyhit")) {
                sound(name.equals("block")?"snd_hit3":"snd_hit1");shake=name.equals("heavyhit")?8:3;
                for(int i=0;i<12;i++)sparks.add(new Spark(x,y,name.equals("block")?TEAL:GOLD,visualRandom));
                sparks.add(new Spark(x,y-28,value,name.equals("block")?"BLOCK":"−"+value));
            }
            if(name.equals("dodge"))sparks.add(new Spark(x,y,0,"DODGE"));
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            long now=System.nanoTime();float dt=last==0?0:Math.min(.05f,(now-last)/1e9f);last=now;
            if(foreground)time+=dt;
            scale=Math.min(getWidth()/1280f,getHeight()/720f);offsetX=(getWidth()-1280*scale)/2;offsetY=(getHeight()-720*scale)/2;
            c.drawColor(Color.BLACK);c.save();c.translate(offsetX,offsetY);c.scale(scale,scale);c.clipRect(0,0,1280,720);
            buttons.clear();
            if(screen==FIGHT){if(!paused&&foreground)update(dt);drawFight(c);}else drawMenu(c);
            if(bannerTime>0){bannerTime-=dt;box(c,260,16,1020,65,Color.rgb(24,36,38),GOLD,8);text(c,banner,640,48,20,CREAM,true);}
            c.restore();
            if(foreground)postInvalidateOnAnimation();
        }
        void update(float dt) {
            Combat.Fighter f=combat.player;f.axis=joyX;
            if((keys&1)!=0)f.axis=-1;if((keys&2)!=0)f.axis=1;
            f.ducking=joyY>.45f;f.guarding=(keys&4)!=0;
            boolean punch=(keys&8)!=0,kick=(keys&16)!=0,heavy=(keys&32)!=0;
            for(Map.Entry<Integer,PointF> t:touches.entrySet()) {
                if(t.getKey()==joyId)continue;PointF q=t.getValue();
                if(circle(q,813,608,53))f.guarding=true;
                if(circle(q,968,595,61))punch=true;
                if(circle(q,1110,595,61))kick=true;
                if(circle(q,1190,472,47))heavy=true;
            }
            if(joyY<-.55f||(keys&64)!=0)combat.jump();
            if(heavy)combat.attack(3,f.ducking);else if(kick)combat.attack(2,f.ducking);else if(punch)combat.attack(1,f.ducking);
            // Small fixed steps keep collision and attack timing stable during a slow frame.
            int n=Math.max(1,(int)Math.ceil(dt/.012f));for(int i=0;i<n;i++)combat.tick(dt/n);
            for(int i=sparks.size()-1;i>=0;i--){Spark s=sparks.get(i);s.life-=dt;s.x+=s.vx*dt;s.y+=s.vy*dt;s.vy+=dt*280;if(s.life<=0)sparks.remove(i);}
            shake=Math.max(0,shake-dt*35);
            if(combat.finished&&!awarded){awarded=true;if(combat.matchWinner==1){earned=75+combat.stage*20+(combat.mode==Combat.SURVIVAL?25:0);coins=999999999;if(combat.mode==Combat.STORY)progress=Math.max(progress,Math.min(6,combat.stage+1));if(combat.mode==Combat.SURVIVAL){survivalStage=combat.stage+1;survivalBest=Math.max(survivalBest,survivalStage);}}else{earned=15;coins+=15;}save();}
        }
        void drawMenu(Canvas c) {
            bitmap(c,"dojo",0,0,1280,720,255);rect(c,0,0,1280,720,screen==HOME?0x151A120B:0xAD18120C);
            if(screen==HOME) {
                bitmap(c,"ui_DialogScroll_Background_Center",0,0,510,720,255);
                bitmap(c,"ui_CommonScrolls_Paper_right",480,0,30,720,255);
                p.setColorFilter(new PorterDuffColorFilter(0xFF48301B,PorterDuff.Mode.SRC_IN));
                bitmap(c,"ui_Logo_left",62,90,183,108,255);bitmap(c,"ui_Logo_right",245,90,183,108,255);p.setColorFilter(null);
                text(c,"THE SHADOW RETURNS",250,251,22,0xFF61411F,true);
                text(c,"An offline journey through the dojo",250,290,18,0xFF61411F,true);
                bitmap(c,"character_sensei",195,307,110,110,255);
                button(c,78,402,432,466,progress>=6?"REPLAY THE STORY":"CONTINUE THE STORY",0,true);
                button(c,78,486,248,545,"PRACTICE",1,false);button(c,263,486,433,545,"SURVIVAL",2,false);
                button(c,78,563,248,618,"ARMORY",3,false);button(c,263,563,433,618,"CONTROLS",4,false);
                Combat.Fighter hero=new Combat.Fighter();hero.x=908;hero.weapon=weapon;hero.animationTime=time;hero.motion=weapon==1?"knives_stance_idle":"fists1_stance_idle";
                drawFighter(c,hero,595,1.8f,GOLD,false);
                bitmap(c,"ui_MenuButtons_Map_normal",593,606,76,76,255);
                bitmap(c,"ui_MenuButtons_Shop_normal",749,606,76,76,255);
                bitmap(c,"ui_MenuButtons_btn_punching_bag",905,606,76,76,255);
                bitmap(c,"ui_MenuButtons_Settings_normal",1061,606,76,76,255);
                buttons.add(new Button(593,606,669,682,0));buttons.add(new Button(749,606,825,682,3));buttons.add(new Button(905,606,981,682,1));buttons.add(new Button(1061,606,1137,682,4));
                text(c,"MAP",631,700,12,CREAM,true);text(c,"ARMORY",787,700,12,CREAM,true);text(c,"PRACTICE",943,700,12,CREAM,true);text(c,"CONTROLS",1099,700,12,CREAM,true);
                button(c,78,655,260,698,new String[]{"DIFFICULTY: EASY","DIFFICULTY: NORMAL","DIFFICULTY: HARD"}[difficulty],5,false);
                button(c,278,655,433,698,musicOn?"MUSIC: ON":"MUSIC: OFF",6,false);
                text(c,"SUPPLIES ∞",1185,58,20,GOLD,true);
                return;
            }
            button(c,38,28,170,76,"‹ DOJO",10,false);
            text(c,screen==LADDER?"THE ROAD TO LYNX":screen==ARMORY?"THE ARMORY":"MASTER THE SHADOW",640,72,34,GOLD,true);
            text(c,"SUPPLIES ∞",1155,65,22,CREAM,true);
            if(screen==LADDER) {
                text(c,"Defeat each challenger in a best-of-three duel.",640,126,21,MUTED,true);
                for(int i=0;i<6;i++){
                    float x=56+i*196;boolean locked=false;boolean won=i<progress;
                    box(c,x,180,x+182,502,locked?0xB0162024:0xD9222D30,won?TEAL:locked?0xFF3C4748:GOLD,9);
                    bitmap(c,i==5?"boss_lynx":"man_kungfu",x+17,210,148,148,locked?65:255);
                    text(c,Combat.NAMES[i],x+91,407,24,locked?MUTED:CREAM,true);
                    text(c,won?"DEFEATED":locked?"LOCKED":"READY",x+91,445,15,won?TEAL:MUTED,true);
                    if(!locked)buttons.add(new Button(x,180,x+182,502,30+i));
                }
                button(c,430,553,850,625,progress>=6?"CHALLENGE LYNX AGAIN":"FIGHT "+Combat.NAMES[Math.min(progress,5)],20,true);
                text(c,progress>=6?"The act is complete. Replay any duel or enter Survival.":"Story progress is saved automatically.",640,676,19,MUTED,true);
            } else if(screen==ARMORY) {
                text(c,"Choose your weapon. Upgrades carry into every mode.",640,125,21,MUTED,true);
                String[] names={"BARE HANDS","TWIN BLADES","BO STAFF"};
                String[] desc={"Quick strikes · fast recovery","Sharp reach · precise attacks","Long reach · heavier impact"};
                for(int i=0;i<3;i++){
                    float x=85+i*390;boolean owned=(unlocks&(1<<i))!=0;
                    box(c,x,170,x+350,436,0xE21B292C,weapon==i?GOLD:0xFF4A5A59,10);
                    drawWeaponIcon(c,x+175,235,i);
                    text(c,names[i],x+175,300,28,CREAM,true);text(c,desc[i],x+175,339,16,MUTED,true);
                    button(c,x+32,365,x+318,419,weapon==i?"EQUIPPED":"EQUIP — FREE",40+i,weapon==i);
                }
                button(c,96,492,613,572,"POWER "+power+"  →  "+(power>=8?"MAX":(power+1)+"    FREE"),45,false);
                button(c,665,492,1182,572,"ARMOR "+armor+"  →  "+(armor>=8?"MAX":(armor+1)+"    FREE"),46,false);
                text(c,"Every weapon is unlocked. Supplies and upgrades are free.",640,642,20,MUTED,true);
            } else {
                String[][] help={{"MOVE","Drag the left pad left or right.","Push up to jump. Push down to duck."},{"ATTACK","Hold PUNCH or KICK to chain attacks.","Down + KICK sweeps. Jump + attack strikes in the air."},{"DEFEND","Hold GUARD to block incoming strikes.","Sweeps beat guard. Jump to avoid a sweep."},{"POWER","POWER delivers a slower, stronger strike.","Your stamina is unlimited. Keep the pressure on."}};
                for(int i=0;i<4;i++){float y=175+i*105;text(c,"0"+(i+1),110,y+20,32,GOLD,true);text(c,help[i][0],175,y+8,23,CREAM,false);text(c,help[i][1],380,y+3,21,MUTED,false);text(c,help[i][2],380,y+33,21,MUTED,false);rect(c,174,y+66,1140,y+67,0xFF364347);}
                button(c,430,620,850,683,"ENTER PRACTICE",1,true);
            }
        }
        void drawWeaponIcon(Canvas c,float x,float y,int kind) {
            p.setColor(GOLD);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(8);p.setStrokeCap(Paint.Cap.ROUND);
            if(kind==0){for(int i=0;i<4;i++)c.drawOval(x-50+i*25,y-18,x-29+i*25,y+9,p);c.drawLine(x-47,y+23,x+46,y+23,p);}
            if(kind==1){c.drawLine(x-62,y+27,x-8,y-28,p);c.drawLine(x+8,y+27,x+62,y-28,p);p.setStrokeWidth(3);c.drawLine(x-50,y+26,x-38,y+39,p);c.drawLine(x+20,y+26,x+32,y+39,p);}
            if(kind==2){c.drawLine(x-94,y+14,x+94,y-14,p);p.setColor(CREAM);p.setStrokeWidth(10);c.drawLine(x-12,y+2,x+12,y-2,p);}
            p.setStyle(Paint.Style.FILL);
        }
        void drawFight(Canvas c) {
            String bg=new String[]{"dojo","bamboo_grove","moon"}[combat.stage%3];
            c.save();if(shake>0&&!paused)c.translate((visualRandom.nextFloat()-.5f)*shake,(visualRandom.nextFloat()-.5f)*shake);
            bitmap(c,bg,0,0,1280,720,255);
            p.setShader(new LinearGradient(0,0,0,210,new int[]{0xEF10191D,0x1810191D},null,Shader.TileMode.CLAMP));c.drawRect(0,0,1280,210,p);p.setShader(null);
            if(combat.player.x<combat.enemy.x){drawFighter(c,combat.player,555,1.30f,GOLD,true);drawFighter(c,combat.enemy,555,1.30f,TEAL,false);}else{drawFighter(c,combat.enemy,555,1.30f,TEAL,false);drawFighter(c,combat.player,555,1.30f,GOLD,true);}
            for(Spark s:sparks){if(s.label!=null)text(c,s.label,s.x,s.y,25,(s.label.equals("BLOCK")?TEAL:CREAM),true);else{p.setColor(s.color);p.setAlpha((int)(255*Math.min(1,s.life*3)));c.drawCircle(s.x,s.y,2.5f,p);p.setAlpha(255);}}
            c.restore();
            health(c,55,80,525,combat.player.hp/combat.player.maxHp,false,GOLD);health(c,755,80,1225,combat.enemy.hp/combat.enemy.maxHp,true,TEAL);
            text(c,"SHADOW",56,56,23,CREAM,false);text(c,combat.opponent(),1224,56,23,CREAM,false,true);
            text(c,"ROUND "+combat.round,640,48,15,MUTED,true);
            text(c,combat.mode==Combat.TRAINING?"∞":String.valueOf((int)Math.ceil(combat.clock)),640,104,44,GOLD,true);
            stamina(c,55,111,470,combat.player.energy,GOLD);stamina(c,810,111,470,combat.enemy.energy,TEAL);
            for(int i=0;i<2;i++){dot(c,67+i*24,145,6,i<combat.playerRounds?GOLD:0x66555555);dot(c,1213-i*24,145,6,i<combat.enemyRounds?TEAL:0x66555555);}
            text(c,combat.mode==Combat.TRAINING?"PRACTICE":combat.mode==Combat.SURVIVAL?"SURVIVAL • "+(combat.stage+1):"ACT I • THE HERO REBORN",640,148,15,MUTED,true);
            if(combat.combo>=2){text(c,combat.combo+" HIT COMBO",265,213,24,GOLD,true);}
            if(combat.mode==Combat.TRAINING){button(c,510,165,770,203,combat.practiceAI?"PARTNER: ACTIVE":"PARTNER: PASSIVE",62,false);text(c,"LAST HIT "+combat.lastDamage+"  ·  BEST COMBO "+combat.bestCombo,640,231,15,CREAM,true);}
            if(combat.intro>0){text(c,combat.intro>.65?"ROUND "+combat.round:"FIGHT",640,307,58,GOLD,true);}
            if(combat.roundOver&&!combat.finished){text(c,combat.enemy.hp<=0||combat.playerRounds>combat.enemyRounds?"ROUND WON":"ROUND OVER",640,295,48,GOLD,true);}
            if(!combat.finished&&!paused){drawControls(c);button(c,601,665,679,709,"Ⅱ",60,false);}
            if(paused&&!combat.finished){rect(c,0,0,1280,720,0xCF10191D);text(c,"PAUSED",640,235,58,GOLD,true);button(c,430,295,850,365,"RESUME",61,true);button(c,430,390,850,450,"RESTART DUEL",63,false);button(c,430,475,850,535,"RETURN TO DOJO",64,false);}
            if(combat.finished){
                rect(c,0,0,1280,720,0xDC10191D);boolean win=combat.matchWinner==1;
                text(c,win?"VICTORY":"DEFEAT",640,200,70,win?GOLD:CREAM,true);
                text(c,combat.playerRounds+"  :  "+combat.enemyRounds,640,270,36,CREAM,true);
                text(c,"UNLIMITED SUPPLIES",640,320,24,GOLD,true);
                text(c,combat.mode==Combat.STORY&&combat.stage==5&&win?"LYNX FALLS. THE SHADOW ENDURES.":win?"Another step on the path.":"Return stronger. Your reward has been saved.",640,371,21,MUTED,true);
                button(c,430,420,850,488,win?(combat.mode==Combat.STORY&&combat.stage==5?"ENTER SURVIVAL":"NEXT CHALLENGER"):"TRY AGAIN",65,true);
                button(c,430,514,850,576,"ARMORY",66,false);button(c,430,596,850,654,"RETURN TO DOJO",64,false);
            }
        }
        void drawControls(Canvas c) {
            p.setShader(new LinearGradient(0,465,0,720,new int[]{0x0010191D,0xA810191D},null,Shader.TileMode.CLAMP));c.drawRect(0,465,1280,720,p);p.setShader(null);
            bitmap(c,"ui_FightButtons_JoystickContainer_"+(joyId<0?"norm":"action"),65,480,200,200,205);
            bitmap(c,"ui_FightButtons_Joystick_"+(joyId<0?"norm":"action"),129+joyX*53,544+joyY*53,72,72,245);
            text(c,"MOVE / JUMP / DUCK",165,705,12,MUTED,true);
            control(c,813,608,53,"GUARD",TEAL);control(c,968,595,61,"PUNCH",GOLD);control(c,1110,595,61,"KICK",GOLD);control(c,1190,472,47,"POWER",GOLD);
        }
        void control(Canvas c,float x,float y,float r,String label,int color){
            String icon=label.equals("PUNCH")?"punch":label.equals("KICK")?"kick":label.equals("POWER")?"charge":null;
            if(icon!=null){boolean down=false;for(PointF q:touches.values())if(circle(q,x,y,r))down=true;bitmap(c,"ui_FightButtons_btn_"+icon+(down?"_action":"_normal"),x-r,y-r,r*2,r*2,220);}
            else {ring(c,x,y,r,0x903B4742,0xCC0B100D,4);text(c,label,x,y+7,17,CREAM,true);}
        }
        void health(Canvas c,float l,float t,float r,float ratio,boolean reverse,int color){
            bitmap(c,"ui_FightUI_HealthBar_Empty",l,t,r-l,23,255);float w=(r-l)*Math.max(0,Math.min(1,ratio));
            bitmap(c,reverse?"ui_FightUI_HealthBarBlue_Full":"ui_FightUI_HealthBar_Full",reverse?r-w:l,t,w,23,255);
        }
        void stamina(Canvas c,float x,float y,float w,float v,int color){rect(c,x,y,x+w,y+4,0xFF273234);rect(c,x,y,x+w*v/100,y+4,color);}
        void drawFighter(Canvas c,Combat.Fighter f,float floor,float size,int accent,boolean hero) {
            boolean dead=combat!=null&&screen==FIGHT&&combat.roundOver&&f.hp<=0;
            String motionName=f.motion.equals("fists1_stance_idle")?(f.weapon==2?"staff_idle":"stance_idle"):f.motion;
            Motion motion=motions.get(motionName);if(motion==null)motion=motions.get("stance_idle");
            float frame;
            if(dead)frame=Math.min(motion.frames-1,f.animationTime*24);
            else if(f.action!=0)frame=Math.min(motion.frames-1,f.actionTime/f.actionLength*(motion.frames-1));
            else if(f.stun>0)frame=Math.min(motion.frames-1,f.animationTime*25);
            else if(f.y>0)frame=Math.min(motion.frames-1,(.82f-f.vy/1600f)*24);
            else if(f.guarding||f.ducking)frame=Math.min(motion.frames-1,f.animationTime*25);
            else if(motionName.equals("knives_stance_idle"))frame=Math.max(0,motion.frames-7)+(f.animationTime*24)%Math.min(7,motion.frames);
            else frame=(f.animationTime*24)%motion.frames;
            motion.sample(frame,pose);
            // Original motion files contain the whole rig, including fingers and weapon pivots.
            // We render the major joints as a continuous silhouette on the Android canvas.
            float rootX=(pose[10][0]+pose[11][0])*.5f;
            float rootY=f.y>0?90-(pose[10][1]+pose[11][1])*.5f:0;
            p.setColor(0x55000000);c.drawOval(f.x-68,floor-7,f.x+68,floor+12,p);
            c.save();c.translate(f.x,floor-f.y);c.scale(size*f.facing,-size);c.translate(-rootX,rootY);
            int ink=Color.rgb(9,12,16);if(f.stun>0&&((int)(f.stun*40)%2)==0)ink=Color.rgb(39,35,30);
            if(originalModel!=null){
                originalModel.draw(c,pose,p,ink,f.weapon);
                if(f.weapon>2){
                    p.setStrokeWidth(f.weapon==2?5:3);p.setColor(f.weapon==2?0xFFB38954:0xFFDFDFD5);
                    if(f.weapon==1){blade(c,6,8);blade(c,7,9);}else{
                        float x=pose[7][0],y=pose[7][1],dx=pose[9][0]-x,dy=pose[9][1]-y;
                        float len=(float)Math.hypot(dx,dy);if(len<1)len=1;
                        c.drawLine(x-dx/len*64,y-dy/len*64,x+dx/len*95,y+dy/len*95,p);
                    }
                }
                c.restore();return;
            }
            // Back arm and leg, torso, then front limbs preserve a readable martial-arts pose.
            limb(c,3,5,11,9,ink);limb(c,5,7,9,7,ink);limb(c,7,9,7,5,ink);
            limb(c,11,13,16,12,ink);limb(c,13,15,12,8,ink);limb(c,15,17,8,6,ink);
            p.setColor(ink);path.reset();path.moveTo(pose[2][0]-7,pose[2][1]+4);path.lineTo(pose[3][0]+8,pose[3][1]+4);path.lineTo(pose[11][0]+13,pose[11][1]-7);path.lineTo(pose[10][0]-12,pose[10][1]-7);path.close();c.drawPath(path,p);
            limb(c,10,12,16,12,ink);limb(c,12,14,12,8,ink);limb(c,14,16,8,6,ink);
            limb(c,1,0,8,9,ink);
            p.setColor(ink);c.drawOval(pose[0][0]-15,pose[0][1]-15,pose[0][0]+14,pose[0][1]+22,p);
            // A short profile and tied headband distinguish the facing direction.
            path.reset();path.moveTo(pose[0][0]+10,pose[0][1]+10);path.lineTo(pose[0][0]+21,pose[0][1]+3);path.lineTo(pose[0][0]+10,pose[0][1]-2);path.close();c.drawPath(path,p);
            limb(c,2,4,12,9,ink);limb(c,4,6,9,7,ink);limb(c,6,8,7,5,ink);
            p.setColor(accent);p.setStrokeWidth(4);c.drawLine(pose[10][0]-9,pose[10][1]+1,pose[11][0]+9,pose[11][1]+1,p);
            c.drawLine(pose[0][0]-13,pose[0][1]+11,pose[0][0]+11,pose[0][1]+10,p);
            c.drawLine(pose[0][0]-12,pose[0][1]+10,pose[0][0]-33,pose[0][1]+3+(float)Math.sin(time*5)*5,p);
            if(f.weapon>0) {
                p.setStrokeWidth(f.weapon==2?5:3);p.setColor(f.weapon==2?0xFFB38954:0xFFDFDFD5);
                if(f.weapon==1){blade(c,6,8);blade(c,7,9);}else {float x=pose[7][0],y=pose[7][1];float dx=pose[9][0]-x,dy=pose[9][1]-y;float len=(float)Math.sqrt(dx*dx+dy*dy);if(len<1)len=1;c.drawLine(x-dx/len*64,y-dy/len*64,x+dx/len*95,y+dy/len*95,p);}
            }
            c.restore();
        }
        void blade(Canvas c,int a,int b){float dx=pose[b][0]-pose[a][0],dy=pose[b][1]-pose[a][1],len=(float)Math.sqrt(dx*dx+dy*dy);if(len<1)len=1;c.drawLine(pose[b][0],pose[b][1],pose[b][0]+dx/len*32,pose[b][1]+dy/len*32,p);}
        void limb(Canvas c,int a,int b,float ra,float rb,int color) {
            float ax=pose[a][0],ay=pose[a][1],bx=pose[b][0],by=pose[b][1];float dx=bx-ax,dy=by-ay,len=(float)Math.sqrt(dx*dx+dy*dy);if(len<.01)len=1;float nx=-dy/len,ny=dx/len;
            p.setColor(color);p.setStyle(Paint.Style.FILL);path.reset();path.moveTo(ax+nx*ra,ay+ny*ra);path.lineTo(bx+nx*rb,by+ny*rb);path.lineTo(bx-nx*rb,by-ny*rb);path.lineTo(ax-nx*ra,ay-ny*ra);path.close();c.drawPath(path,p);c.drawCircle(ax,ay,ra,p);c.drawCircle(bx,by,rb,p);
        }
        class Motion {
            int frames,n;float[] data;
            Motion(String name)throws IOException {
                ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=getAssets().open(name)){byte[] buf=new byte[8192];int count;while((count=in.read(buf))>0)out.write(buf,0,count);}
                ByteBuffer b=ByteBuffer.wrap(out.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);frames=b.getInt();n=b.getInt();data=new float[frames*n*3];for(int i=0;i<data.length;i++)data[i]=b.getFloat();
            }
            void sample(float frame,float[][] target){frame=Math.max(0,Math.min(frames-1,frame));int a=(int)frame,b=Math.min(frames-1,a+1);float f=frame-a;for(int i=0;i<n;i++)for(int k=0;k<3;k++){float x=data[(a*n+i)*3+k],y=data[(b*n+i)*3+k];target[i][k]=x+(y-x)*f;}}
        }
        class Button {RectF rect;int id;Button(float l,float t,float r,float b,int id){rect=new RectF(l,t,r,b);this.id=id;}}
        class Spark {
            float x,y,vx,vy,life;int color;String label;
            Spark(float x,float y,int color,Random r){this.x=x;this.y=y;this.color=color;vx=(r.nextFloat()-.5f)*430;vy=(r.nextFloat()-.8f)*300;life=.3f+r.nextFloat()*.3f;}
            Spark(float x,float y,int value,String label){this.x=x;this.y=y;this.label=label;life=.8f;vy=-110;}
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            int action=e.getActionMasked(),idx=e.getActionIndex(),id=e.getPointerId(idx);float x=(e.getX(idx)-offsetX)/scale,y=(e.getY(idx)-offsetY)/scale;
            if(action==android.view.MotionEvent.ACTION_CANCEL){touches.clear();joyId=-1;joyX=joyY=0;menuPressed=-1;return true;}
            if(screen==FIGHT&&!paused&&!combat.finished) {
                if(action==android.view.MotionEvent.ACTION_DOWN||action==android.view.MotionEvent.ACTION_POINTER_DOWN){
                    for(Button b:buttons)if(b.rect.contains(x,y)){handle(b.id);return true;}
                    touches.put(id,new PointF(x,y));if(joyId<0&&x<360&&y>405)joyId=id;
                }
                if(action==android.view.MotionEvent.ACTION_MOVE){for(int i=0;i<e.getPointerCount();i++)if(touches.containsKey(e.getPointerId(i)))touches.put(e.getPointerId(i),new PointF((e.getX(i)-offsetX)/scale,(e.getY(i)-offsetY)/scale));}
                if(action==android.view.MotionEvent.ACTION_UP||action==android.view.MotionEvent.ACTION_POINTER_UP){touches.remove(id);if(id==joyId){joyId=-1;joyX=joyY=0;}}
                PointF joy=touches.get(joyId);if(joy!=null){joyX=Math.max(-1,Math.min(1,(joy.x-165)/80));joyY=Math.max(-1,Math.min(1,(joy.y-580)/80));if(Math.abs(joyX)<.17)joyX=0;}
            } else {
                if(action==android.view.MotionEvent.ACTION_DOWN){menuPressed=-1;for(Button b:buttons)if(b.rect.contains(x,y))menuPressed=b.id;}
                if(action==android.view.MotionEvent.ACTION_UP){int selected=-1;for(Button b:buttons)if(b.rect.contains(x,y)&&b.id==menuPressed)selected=b.id;menuPressed=-1;if(selected>=0)handle(selected);performClick();}
            }
            return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
        void handle(int id) {
            sound("snd_click_1");
            if(id==0)screen=LADDER;
            else if(id==1)start(Combat.TRAINING,0);
            else if(id==2){survivalStage=0;start(Combat.SURVIVAL,0);}
            else if(id==3||id==66){screen=ARMORY;setMusic("menu");}
            else if(id==4)screen=HELP;
            else if(id==5){difficulty=(difficulty+1)%3;save();}
            else if(id==6){musicOn=!musicOn;if(musicOn)setMusic("menu");else if(music!=null)music.pause();save();}
            else if(id==10||id==64){screen=HOME;paused=false;touches.clear();joyId=-1;setMusic("menu");}
            else if(id==20)start(Combat.STORY,Math.min(progress,5));
            else if(id>=30&&id<36)start(Combat.STORY,id-30);
            else if(id>=40&&id<43){weapon=id-40;unlocks=7;save();}
            else if(id==45||id==46){int level=id==45?power:armor;if(level>=8){notifyText("Upgrade complete.");return;}if(id==45)power++;else armor++;sound("snd_coin_hit4");save();}
            else if(id==60||id==61){paused=!paused;touches.clear();joyId=-1;joyX=joyY=0;keys=0;last=0;}
            else if(id==62)combat.practiceAI=!combat.practiceAI;
            else if(id==63)start(combat.mode,combat.stage);
            else if(id==65){if(combat.matchWinner<0)start(combat.mode,combat.stage);else if(combat.mode==Combat.STORY&&combat.stage==5){survivalStage=0;start(Combat.SURVIVAL,0);}else start(combat.mode,combat.stage+1);}
        }
        @Override public boolean onKeyDown(int code,android.view.KeyEvent e){int bit=keyBit(code);if(bit!=0){keys|=bit;return true;}if(code==android.view.KeyEvent.KEYCODE_ESCAPE){back();return true;}return super.onKeyDown(code,e);}
        @Override public boolean onKeyUp(int code,android.view.KeyEvent e){int bit=keyBit(code);if(bit!=0){keys&=~bit;return true;}return super.onKeyUp(code,e);}
        int keyBit(int c){switch(c){case 21:case 29:return 1;case 22:case 32:return 2;case 62:return 4;case 38:return 8;case 39:return 16;case 40:return 32;case 19:case 51:return 64;default:return 0;}}
        boolean circle(PointF q,float x,float y,float r){return (q.x-x)*(q.x-x)+(q.y-y)*(q.y-y)<=r*r;}
        void bitmap(Canvas c,String n,float x,float y,float w,float h,int alpha){Bitmap b=art.get(n);if(b==null)return;p.setAlpha(alpha);c.drawBitmap(b,null,new RectF(x,y,x+w,y+h),p);p.setAlpha(255);}
        void rect(Canvas c,float l,float t,float r,float b,int color){p.setShader(null);p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawRect(l,t,r,b,p);}
        void dot(Canvas c,float x,float y,float r,int color){p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r,p);}
        void ring(Canvas c,float x,float y,float r,int fill,int stroke,float width){p.setStyle(Paint.Style.FILL);p.setColor(fill);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setColor(stroke);c.drawCircle(x,y,r,p);p.setStyle(Paint.Style.FILL);}
        void box(Canvas c,float l,float t,float r,float b,int fill,int stroke,float radius){p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(fill);c.drawRoundRect(l,t,r,b,radius,radius,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.3f);p.setColor(stroke);c.drawRoundRect(l,t,r,b,radius,radius,p);p.setStyle(Paint.Style.FILL);}
        void text(Canvas c,String s,float x,float y,float size,int color,boolean center){text(c,s,x,y,size,color,center,false);}
        void text(Canvas c,String s,float x,float y,float size,int color,boolean center,boolean right){p.setShader(null);p.setColor(color);p.setStyle(Paint.Style.FILL);p.setTypeface(originalFont!=null?originalFont:Typeface.create("serif",Typeface.BOLD));p.setTextSize(size);p.setTextAlign(right?Paint.Align.RIGHT:center?Paint.Align.CENTER:Paint.Align.LEFT);c.drawText(s,x,y,p);}
        void button(Canvas c,float l,float t,float r,float b,String label,int id,boolean filled){
            Bitmap skin=art.get(menuPressed==id?"ui_CommonButtons_BtnGreen":filled?"ui_CommonButtons_BtnGold":"ui_CommonButtons_btnBeige");
            if(skin!=null){float cap=Math.min((r-l)/3,(b-t)*.50f);int left=skin.getWidth()/4,right=skin.getWidth()-left;p.setAlpha(255);
                c.drawBitmap(skin,new Rect(0,0,left,skin.getHeight()),new RectF(l,t,l+cap,b),p);
                c.drawBitmap(skin,new Rect(left,0,right,skin.getHeight()),new RectF(l+cap,t,r-cap,b),p);
                c.drawBitmap(skin,new Rect(right,0,skin.getWidth(),skin.getHeight()),new RectF(r-cap,t,r,b),p);
            }else box(c,l,t,r,b,filled?GOLD:0xB01C292D,filled?GOLD:0x887D887C,5);
            float textSize=label.length()>28?16:label.length()>17?18:20;
            p.setTypeface(originalFont);p.setTextSize(textSize);float measured=p.measureText(label),available=r-l-36;
            if(measured>available)textSize*=available/measured;
            text(c,label,(l+r)/2,(t+b)/2+textSize*.32f,textSize,0xFF30271C,true);buttons.add(new Button(l,t,r,b,id));
        }
    }
}
