package local.shadowfight.reborn;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import java.io.InputStream;
import java.util.*;
import org.json.*;

/** Procedural silhouette from the recovered original scene definitions. */
final class OriginalModel {
    final float[][] points,rest;
    final int[] rig;
    final boolean[] cloth;
    final ArrayList<Macro> macros=new ArrayList<>();
    final ArrayList<Edge> constraints=new ArrayList<>();
    final ArrayList<Figure> figures=new ArrayList<>();
    final Path path=new Path();
    static final class Macro {int index; int[] child; float[] weight;}
    static final class Edge {int a,b;float length;}
    static final class Figure {int a,b,c=-1,weaponOnly;float r1,r2,m1,m2;}
    static float value(JSONObject o,String k,float fallback){return (float)o.optDouble(k,fallback);}
    OriginalModel(Context ctx) throws Exception {
        byte[] data;
        try(InputStream in=ctx.getAssets().open("original_model.json")){
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();
            byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);data=out.toByteArray();
        }
        JSONObject scene=new JSONObject(new String(data,"UTF-8"));
        JSONObject nodes=scene.getJSONObject("nodes"),edges=scene.getJSONObject("edges");
        HashMap<String,Integer> index=new HashMap<>();ArrayList<String> names=new ArrayList<>();
        Iterator<String> it=nodes.keys();while(it.hasNext()){String s=it.next();index.put(s,names.size());names.add(s);}
        points=new float[names.size()][3];rest=new float[names.size()][3];cloth=new boolean[names.size()];
        JSONArray rigNames=scene.getJSONArray("rig");rig=new int[rigNames.length()];HashSet<Integer> fixed=new HashSet<>();
        for(int i=0;i<rig.length;i++){rig[i]=index.get(rigNames.getString(i));fixed.add(rig[i]);}
        for(int i=0;i<names.size();i++){
            JSONObject a=nodes.getJSONObject(names.get(i));rest[i][0]=value(a,"X",0);rest[i][1]=value(a,"Y",0);rest[i][2]=value(a,"Z",0);
            if(fixed.contains(i))continue;
            if(a.optString("Type").equals("MacroNode")){
                Macro m=new Macro();m.index=i;int count=a.getInt("NodesCount");m.child=new int[count];m.weight=new float[count];
                for(int j=0;j<count;j++){m.child[j]=index.get(a.getString("ChildNode"+(j+1)));m.weight[j]=value(a,"LCC"+(j+1),0);}macros.add(m);
            }else cloth[i]=true;
        }
        // Sort weighted nodes by their dependencies once, never during drawing.
        HashSet<Integer> ready=new HashSet<>(fixed);for(int i=0;i<cloth.length;i++)if(cloth[i])ready.add(i);
        ArrayList<Macro> ordered=new ArrayList<>();
        while(ordered.size()<macros.size()){
            int before=ordered.size();for(Macro m:macros)if(!ready.contains(m.index)){
                boolean ok=true;for(int c:m.child)if(!ready.contains(c))ok=false;
                if(ok){ordered.add(m);ready.add(m.index);}
            }
            if(before==ordered.size())throw new Exception("Unresolved model node dependency");
        }
        macros.clear();macros.addAll(ordered);
        it=edges.keys();while(it.hasNext()){
            JSONObject a=edges.getJSONObject(it.next());Integer ka=index.get(a.optString("End1")),kb=index.get(a.optString("End2"));
            if(ka==null||kb==null||!(cloth[ka]||cloth[kb]))continue;
            Edge e=new Edge();e.a=ka;e.b=kb;e.length=value(a,"Length",0);if(e.length>0)constraints.add(e);
        }
        JSONArray fs=scene.getJSONArray("figures");
        for(int i=0;i<fs.length();i++){
            JSONObject a=fs.getJSONObject(i);Figure f=new Figure();f.weaponOnly=a.optInt("WeaponOnly",0);
            if(a.optString("Type").equals("Triangle")){
                f.a=index.get(a.getString("Node1"));f.b=index.get(a.getString("Node2"));f.c=index.get(a.getString("Node3"));
            }else if(a.optString("Type").equals("Capsule")){
                JSONObject e=edges.getJSONObject(a.getString("Edge"));f.a=index.get(e.getString("End1"));f.b=index.get(e.getString("End2"));
                f.r1=value(a,"Radius1",1);f.r2=value(a,"Radius2",1);f.m1=value(a,"Margin1",0);f.m2=value(a,"Margin2",0);
            }else continue;figures.add(f);
        }
    }
    void draw(Canvas canvas,float[][] pose,Paint paint,int color,int weapon){
        for(int i=0;i<points.length;i++)System.arraycopy(rest[i],0,points[i],0,3);
        for(int i=0;i<rig.length;i++)System.arraycopy(pose[i],0,points[rig[i]],0,3);
        for(Macro m:macros){float[] p=points[m.index];Arrays.fill(p,0);for(int j=0;j<m.child.length;j++)for(int d=0;d<3;d++)p[d]+=points[m.child[j]][d]*m.weight[j];}
        // Distance constraints approximate cloth; the original engine also models inertia.
        for(int pass=0;pass<60;pass++)for(Edge e:constraints){
            float[] a=points[e.a],b=points[e.b];float dx=b[0]-a[0],dy=b[1]-a[1],dz=b[2]-a[2];float dist=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(dist<.00001f)continue;float factor=(dist-e.length)/dist*((cloth[e.a]&&cloth[e.b])?.5f:1f);
            if(cloth[e.a]){a[0]+=dx*factor;a[1]+=dy*factor;a[2]+=dz*factor;}
            if(cloth[e.b]){b[0]-=dx*factor;b[1]-=dy*factor;b[2]-=dz*factor;}
        }
        paint.setStyle(Paint.Style.FILL);paint.setColor(color);
        for(Figure f:figures){
            if(f.weaponOnly!=0&&f.weaponOnly!=weapon)continue;
            float[] a=points[f.a],b=points[f.b];path.reset();
            if(f.c>=0){float[] c=points[f.c];path.moveTo(a[0],a[1]);path.lineTo(b[0],b[1]);path.lineTo(c[0],c[1]);path.close();canvas.drawPath(path,paint);continue;}
            float dx=b[0]-a[0],dy=b[1]-a[1];float ax=a[0]+dx*f.m1,ay=a[1]+dy*f.m1,bx=b[0]-dx*f.m2,by=b[1]-dy*f.m2;
            dx=bx-ax;dy=by-ay;float len=(float)Math.hypot(dx,dy);if(len<.0001f)len=1;float nx=-dy/len,ny=dx/len;
            path.moveTo(ax+nx*f.r1,ay+ny*f.r1);path.lineTo(bx+nx*f.r2,by+ny*f.r2);path.lineTo(bx-nx*f.r2,by-ny*f.r2);path.lineTo(ax-nx*f.r1,ay-ny*f.r1);path.close();
            canvas.drawPath(path,paint);canvas.drawCircle(ax,ay,f.r1,paint);canvas.drawCircle(bx,by,f.r2,paint);
        }
    }
}
