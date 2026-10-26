package local.shadowfight.reborn;

public final class CombatTest {
    static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);}
    static Combat game(int mode){Combat c=new Combat(mode,0,1,0,0,0,1234,null);c.intro=0;c.player.x=500;c.enemy.x=600;return c;}
    static void ticks(Combat c,float seconds){for(int i=0;i<(int)(seconds*120);i++)c.tick(1f/120);}
    public static void main(String[] args){
        Combat c=game(Combat.TRAINING);float hp=c.enemy.hp;
        check(c.attack(1,false),"punch accepted");check(!c.attack(1,false),"cannot restart a pending punch");ticks(c,.3f);
        check(c.enemy.hp<hp,"punch connects");float normal=hp-c.enemy.hp;
        c=game(Combat.TRAINING);c.enemy.guarding=true;c.attack(1,false);ticks(c,.3f);float block=100-c.enemy.hp;
        // Practice's passive partner clears guard; use direct hit resolution for guard rules.
        c=game(Combat.TRAINING);c.enemy.guarding=true;c.player.action=1;c.resolve(c.player,c.enemy);block=100-c.enemy.hp;
        check(block>0&&block<normal*.4f,"guard reduces damage");
        c=game(Combat.TRAINING);c.enemy.guarding=true;c.player.action=4;c.resolve(c.player,c.enemy);check(100-c.enemy.hp>block*2,"sweep defeats guard");
        c=game(Combat.TRAINING);c.enemy.y=60;c.player.action=4;c.resolve(c.player,c.enemy);check(c.enemy.hp==100,"jump evades sweep");
        c=game(Combat.TRAINING);c.enemy.x=950;c.player.action=1;c.resolve(c.player,c.enemy);check(c.enemy.hp==100,"out-of-range attacks miss");
        c=game(Combat.TRAINING);c.player.energy=0;check(c.attack(3,false),"unlimited supplies allow power strike at zero stored energy");check(c.player.energy==100,"player energy restored immediately");
        c=game(Combat.TRAINING);c.enemy.energy=0;check(!c.attack(c.enemy,3,false),"enemy still follows normal energy rules");
        c=game(Combat.TRAINING);c.player.guarding=true;c.player.energy=100;c.enemy.action=1;c.resolve(c.enemy,c.player);check(c.player.energy==100,"blocking does not consume player supplies");
        c=game(Combat.TRAINING);c.jump();ticks(c,1.2f);check(c.player.y==0&&c.player.vy==0,"jump returns to floor");
        c=game(Combat.TRAINING);ticks(c,110);check(!c.finished&&c.clock==99,"practice has no match timeout");
        c=game(Combat.STORY);c.enemy.hp=0;c.tick(.01f);check(c.roundOver&&c.playerRounds==1,"first KO awards round");ticks(c,2.6f);c.intro=0;c.enemy.hp=0;c.tick(.01f);ticks(c,2.6f);check(c.finished&&c.matchWinner==1,"best-of-three victory");
        c=game(Combat.STORY);float start=c.player.hp;boolean aiHit=false;for(int i=0;i<1440;i++){c.tick(1f/120);aiHit|=c.player.hp<start;}check(aiHit,"AI approaches and attacks");
        System.out.println("PASS: attack timing, guard, sweep, jumping, range, unlimited player supplies, enemy stamina, practice, rounds, AI");
    }
}
