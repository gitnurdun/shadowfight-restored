package local.shadowfight.reborn;

import java.util.Random;

/** Offline combat rules. Coordinates and time are independent of the display. */
public final class Combat {
    public static final int STORY=0, SURVIVAL=1, TRAINING=2;
    public static final String[] NAMES={"SHIN","BRICK","NEEDLE","GHOST","DANDY","LYNX"};
    public interface Events { void event(String name,float x,float y,int value); }
    public final Fighter player=new Fighter(), enemy=new Fighter();
    final Random random;
    final Events events;
    public int mode, stage, difficulty, weapon, attackLevel, armorLevel;
    public int playerRounds, enemyRounds, round=1, matchWinner, lastDamage, combo, bestCombo;
    public float clock=99, intro=2.1f, roundBreak, aiClock=.8f, elapsed, hitStop;
    public boolean roundOver, finished, practiceAI;
    float comboClock;
    public static final class Fighter {
        public float x,hp,maxHp=100,energy=100,y,vy,axis,actionTime,actionLength=.4f,stun,animationTime;
        public int facing=1,action,weapon;
        public boolean guarding,ducking,hitDone;
        public String motion="fists1_stance_idle";
    }
    public Combat(int mode,int stage,int difficulty,int weapon,int power,int armor,long seed,Events events) {
        this.mode=mode; this.stage=stage; this.difficulty=difficulty; this.weapon=weapon;
        this.attackLevel=power; this.armorLevel=armor; this.events=events; random=new Random(seed);
        player.weapon=weapon; enemy.weapon=stage>=2?1:0;
        player.maxHp=100+armor*9; enemy.maxHp=mode==TRAINING?100:100+stage*5;
        resetRound();
    }
    void emit(String name,float x,float y,int value) { if(events!=null)events.event(name,x,y,value); }
    void resetRound() {
        player.x=440; enemy.x=840; player.facing=1; enemy.facing=-1;
        for(Fighter f:new Fighter[]{player,enemy}) { f.hp=f.maxHp; f.energy=100; f.y=f.vy=f.axis=f.stun=f.actionTime=0; f.action=0; f.guarding=f.ducking=f.hitDone=false; f.motion=f.weapon==1?"knives_stance_idle":"fists1_stance_idle"; }
        clock=99; intro=round==1?2.1f:1.7f; roundOver=false; roundBreak=0; combo=0; aiClock=.7f;
    }
    public boolean attack(int button,boolean low) { return attack(player,button,low); }
    boolean attack(Fighter f,int button,boolean low) {
        if(finished||roundOver||intro>0||f.stun>0||f.action!=0)return false;
        int a=button==3?3:(button==2?(low?4:2):1);
        if(f.y>12)a=5;
        float cost=a==3?27:a==4?20:a==5?18:12;
        if(f!=player&&f.energy<cost)return false;
        if(f==player)f.energy=100;else f.energy-=cost;
        f.action=a; f.actionTime=0; f.hitDone=false; f.guarding=false;
        f.actionLength=a==3?.92f:a==4?.78f:a==2?.64f:a==5?.68f:.46f;
        f.motion=a==3?(f.weapon==1?"knives_heavy_slash":"heavy_punch"):a==4?"sweep":a==2?"front_kick":a==5?"front_jump_kick":f.weapon==1?"knives_slash":"high_punch";
        emit("swish",f.x,360,0); return true;
    }
    public void jump() {
        if(intro<=0&&!finished&&!roundOver&&player.y==0&&player.stun<=0&&player.action==0) {
            player.energy=100; player.vy=650; player.motion="jump"; player.animationTime=0;
        }
    }
    public void tick(float dt) {
        if(finished)return;
        elapsed+=dt;
        if(hitStop>0){hitStop-=dt;return;}
        if(intro>0){ intro-=dt; if(intro<=0)emit("gong",640,250,0); return; }
        if(roundOver) {
            player.animationTime+=dt; enemy.animationTime+=dt;
            roundBreak+=dt;
            if(roundBreak>2.4f) {
                if(playerRounds==2||enemyRounds==2) { finished=true; matchWinner=playerRounds==2?1:-1; emit(matchWinner==1?"victory":"defeat",640,300,0); }
                else {round++;resetRound();}
            }
            return;
        }
        if(mode!=TRAINING)clock=Math.max(0,clock-dt);
        if(comboClock>0)comboClock-=dt;else combo=0;
        if(mode!=TRAINING||practiceAI)ai(dt);else {enemy.axis=0;enemy.guarding=false;if(enemy.hp<enemy.maxHp)enemy.hp=Math.min(enemy.maxHp,enemy.hp+dt*5);}
        updateFighter(player,enemy,dt); updateFighter(enemy,player,dt);
        if(Math.abs(player.x-enemy.x)<65&&Math.abs(player.y-enemy.y)<70){float mid=(player.x+enemy.x)/2;int side=player.x<enemy.x?-1:1;player.x=mid+side*32.5f;enemy.x=mid-side*32.5f;}
        if(player.action==0)player.facing=enemy.x>=player.x?1:-1;
        if(enemy.action==0)enemy.facing=player.x>=enemy.x?1:-1;
        if(mode==TRAINING) {if(enemy.hp<=0){enemy.hp=enemy.maxHp;enemy.stun=0;}if(player.hp<=0)player.hp=player.maxHp;}
        else if(player.hp<=0||enemy.hp<=0||clock<=0)endRound();
    }
    void updateFighter(Fighter f,Fighter other,float dt) {
        f.energy=f==player?100:Math.min(100,f.energy+dt*(f.guarding?10:20)); f.stun=Math.max(0,f.stun-dt); f.animationTime+=dt;
        if(f.vy!=0||f.y>0){f.y+=f.vy*dt;f.vy-=1600*dt;if(f.y<=0){f.y=0;f.vy=0;}}
        if(f.action!=0) {
            f.actionTime+=dt;
            if(!f.hitDone&&f.actionTime>=f.actionLength*.42f){f.hitDone=true;resolve(f,other);}
            if(f.actionTime>=f.actionLength){f.action=0;f.actionTime=0;}
        }
        if(f.stun<=0&&f.action==0) {
            float speed=f.guarding?60:f.ducking?65:235;
            f.x=Math.max(115,Math.min(1165,f.x+f.axis*speed*dt));
            f.motion=f.y>2?"jump":f.guarding?"high_block":f.ducking?"duck":Math.abs(f.axis)>.12?(f.axis*f.facing>0?"step_forward":"step_back"):f.weapon==1?"knives_stance_idle":"fists1_stance_idle";
        }
    }
    void resolve(Fighter a,Fighter b) {
        float dist=Math.abs(a.x-b.x);
        float range=a.action==3?153:a.action==2?148:a.action==4?150:108;
        if(a.weapon==2&&a.action!=2&&a.action!=4)range+=65;
        if(a.weapon==1&&a.action==1)range+=23;
        if((b.x-a.x)*a.facing<0||dist>range)return;
        if(a.action==4&&b.y>35)return;
        if(a.action!=4&&Math.abs(a.y-b.y)>92)return;
        if(b.ducking&&b.y==0&&(a.action==1||a.action==2)&&a.y==0){emit("dodge",b.x,330,0);return;}
        float damage=a.action==3?21:a.action==4?14:a.action==2?14:a.action==5?17:9;
        damage+=a.weapon==2?3:a.weapon==1?2:0;
        damage+=a==player?attackLevel*2:stage*.8f+difficulty*1.6f;
        boolean blocked=b.guarding&&b.action==0&&b.energy>=10&&b.y==0&&a.action!=4;
        if(blocked){damage*=.16f;b.energy=b==player?100:Math.max(0,b.energy-16);emit("block",(a.x+b.x)/2,390,(int)damage);}
        else {
            b.action=0;b.stun=a.action==3?.39f:.22f;b.motion="middle_hit";b.animationTime=0;
            b.x=Math.max(115,Math.min(1165,b.x+a.facing*(a.action==3?27:12)));hitStop=.045f;
            if(a==player){combo++;comboClock=1.9f;bestCombo=Math.max(bestCombo,combo);lastDamage=Math.round(damage);}
            emit(a.action==3?"heavyhit":"hit",(a.x+b.x)/2,410-b.y,Math.round(damage));
        }
        b.hp=Math.max(0,b.hp-damage);
    }
    void ai(float dt) {
        float dist=Math.abs(player.x-enemy.x); enemy.axis=dist>128?(player.x>enemy.x?1:-1):0;
        enemy.ducking=false;
        aiClock-=dt;
        if(aiClock<=0) {
            aiClock=.27f+random.nextFloat()*(difficulty==0?.7f:.42f);
            enemy.guarding=player.action!=0&&random.nextFloat()<.25f+difficulty*.13f;
            if(dist<185&&!enemy.guarding&&random.nextFloat()>.22f)attack(enemy,random.nextFloat()<.18?3:random.nextBoolean()?1:2,random.nextFloat()<.16);
            if(dist<100&&random.nextFloat()<.20)enemy.axis=enemy.facing*-1;
        }
    }
    void endRound() {
        roundOver=true;roundBreak=0;
        boolean win=enemy.hp<=0||(player.hp>0&&player.hp/player.maxHp>enemy.hp/enemy.maxHp);
        if(win)playerRounds++;else enemyRounds++;
        player.axis=enemy.axis=0;
        Fighter loser=win?enemy:player;loser.motion="lose_fall";loser.actionTime=0;loser.animationTime=0;
        emit(win?"roundwin":"roundloss",640,300,0);
    }
    public String opponent(){return mode==TRAINING?"SPARRING PARTNER":mode==SURVIVAL?"CHALLENGER "+(stage+1):NAMES[Math.min(stage,5)];}
}
