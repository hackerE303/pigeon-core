package software.hacker_E303.pigeon_core.client.gui;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import software.hacker_E303.pigeon_core.PigeonCore;

import java.nio.FloatBuffer;

/**
 * Renders a procedural cosmic nebula as a fullscreen background using a custom GLSL shader.
 * The shader produces multi-layer domain-warped FBM nebulae, a dense star field with
 * twinkling, and two bright featured stars with lens-flare diffraction spikes.
 * Animation is continuous and never resets across screen open/close cycles.
 */
@OnlyIn(Dist.CLIENT)
final class CosmicBackgroundRenderer {

    private static final long BIRTH_NS = System.nanoTime();

    private static int     program = 0;
    private static int     vao     = 0;
    private static int     vbo     = 0;
    private static int     uTime   = -1;
    private static int     uRes    = -1;
    private static boolean failed  = false;

    // ── Public API ────────────────────────────────────────────────────────────

    static void render(int screenW, int screenH) {
        if (failed) return;
        if (program == 0) {
            init();
            if (failed) return;
        }

        float time = (System.nanoTime() - BIRTH_NS) / 1_000_000_000.0f;

        // Save GL state that we change
        int     savedProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int     savedVao     = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        boolean depthWas     = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blendWas     = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cullWas      = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        if (depthWas) GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (blendWas) GL11.glDisable(GL11.GL_BLEND);
        if (cullWas)  GL11.glDisable(GL11.GL_CULL_FACE);

        GL20.glUseProgram(program);
        GL20.glUniform1f(uTime, time);
        GL20.glUniform2f(uRes, screenW, screenH);

        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
        GL30.glBindVertexArray(savedVao);

        // Restore GL state
        GL20.glUseProgram(savedProgram);
        if (depthWas) GL11.glEnable(GL11.GL_DEPTH_TEST);
        if (blendWas) GL11.glEnable(GL11.GL_BLEND);
        if (cullWas)  GL11.glEnable(GL11.GL_CULL_FACE);
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    private static void init() {
        int vert = compileShader(GL20.GL_VERTEX_SHADER,   VERT_SRC);
        int frag = compileShader(GL20.GL_FRAGMENT_SHADER, FRAG_SRC);
        if (vert == 0 || frag == 0) {
            failed = true;
            if (vert != 0) GL20.glDeleteShader(vert);
            if (frag != 0) GL20.glDeleteShader(frag);
            return;
        }

        program = GL20.glCreateProgram();
        GL20.glBindAttribLocation(program, 0, "Position");
        GL20.glAttachShader(program, vert);
        GL20.glAttachShader(program, frag);
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            PigeonCore.LOGGER.error("[CosmicBG] shader link failed: {}", GL20.glGetProgramInfoLog(program));
            GL20.glDeleteProgram(program);
            program = 0;
            failed  = true;
            return;
        }

        uTime = GL20.glGetUniformLocation(program, "u_time");
        uRes  = GL20.glGetUniformLocation(program, "u_resolution");

        // Fullscreen triangle-strip quad (BL → BR → TL → TR in clip space)
        FloatBuffer buf = MemoryUtil.memAllocFloat(8);
        buf.put(new float[]{-1f, -1f,  1f, -1f,  -1f, 1f,  1f, 1f}).flip();

        int savedVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int savedVbo = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);

        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(buf);

        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 0, 0L);
        GL20.glEnableVertexAttribArray(0);

        GL30.glBindVertexArray(savedVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, savedVbo);
    }

    private static int compileShader(int type, String src) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, src);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            PigeonCore.LOGGER.error("[CosmicBG] shader compile error:\n{}", GL20.glGetShaderInfoLog(shader));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    // ── GLSL sources ──────────────────────────────────────────────────────────

    private static final String VERT_SRC =
        "#version 150 core\n" +
        "in vec2 Position;\n" +
        "void main() {\n" +
        "    gl_Position = vec4(Position, 0.0, 1.0);\n" +
        "}\n";

    /**
     * Fragment shader — mystical/legendary cosmic scene.
     *
     * Visual elements:
     *  - 5 nebula cloud types with distinct characters and colours
     *  - Ridged-FBM luminous filaments (veins of light inside gas)
     *  - 3 sweeping plasma tendrils (animated energy streams)
     *  - 5 star layers + dense star cluster
     *  - 3 featured bright stars with 4-spike diffraction flares
     *  - Global background nebula haze (no empty regions)
     *  - 7 concentrated nebula blobs + 3 elongated pillars/streaks
     *  - Cosmic fine dust texture (two frequencies)
     *  - Ring nebula around star C
     *  - Supernova-remnant shockwave ring
     *  - 5 distant galaxy oval glows
     *  - 5 plasma tendrils
     *  - 5 star layers + 2 dense star clusters
     *  - 3 featured bright stars with 4-spike diffraction
     *  - Spiral energy vortex at galactic centre
     *  - Aurora bands top + bottom
     *  - Filmic tone-map + gamma
     */
    private static final String FRAG_SRC =
        "#version 150 core\n" +
        "uniform float u_time;\n" +
        "uniform vec2  u_resolution;\n" +
        "out vec4 fragColor;\n" +
        "\n" +
        "// ── Hash / noise ────────────────────────────────────────────────────────\n" +
        "float hash1(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}\n" +
        "float gnoise(vec2 p){\n" +
        "    vec2 i=floor(p),f=fract(p),u=f*f*(3.0-2.0*f);\n" +
        "    return mix(mix(hash1(i),hash1(i+vec2(1,0)),u.x),\n" +
        "               mix(hash1(i+vec2(0,1)),hash1(i+vec2(1,1)),u.x),u.y);\n" +
        "}\n" +
        "float fbm(vec2 p){\n" +
        "    float v=0.,a=0.5; mat2 r=mat2(0.8,-0.6,0.6,0.8);\n" +
        "    for(int i=0;i<6;i++){v+=a*gnoise(p);p=r*p*2.1+vec2(17.3,4.7);a*=0.45;}\n" +
        "    return v;\n" +
        "}\n" +
        "// Ridged FBM — creates sharp luminous veins/filaments\n" +
        "float rfbm(vec2 p){\n" +
        "    float v=0.,a=0.5,w=1.; mat2 r=mat2(0.8,-0.6,0.6,0.8);\n" +
        "    for(int i=0;i<6;i++){\n" +
        "        float n=1.0-abs(gnoise(p)*2.0-1.0); n=n*n*w;\n" +
        "        v+=a*n; w=clamp(n*1.5,0.0,1.0);\n" +
        "        p=r*p*2.1+vec2(17.3,4.7); a*=0.5;\n" +
        "    } return v;\n" +
        "}\n" +
        "// Double domain-warp FBM (organic clouds)\n" +
        "float wfbm(vec2 p,float t){\n" +
        "    vec2 q=vec2(fbm(p+t*vec2(0.04,-0.03)),fbm(p+vec2(5.2,1.3)+t*vec2(-0.03,0.04)));\n" +
        "    vec2 rr=vec2(fbm(p+4.0*q+vec2(1.7,9.2)+t*vec2(-0.02,0.01)),\n" +
        "                 fbm(p+4.0*q+vec2(8.3,2.8)+t*vec2(0.02,-0.03)));\n" +
        "    return fbm(p+4.0*rr+t*0.018);\n" +
        "}\n" +
        "// Warped ridged FBM\n" +
        "float wrfbm(vec2 p,float t){\n" +
        "    vec2 q=vec2(fbm(p+t*vec2(0.025,-0.018)),fbm(p+vec2(3.1,2.7)+t*vec2(-0.02,0.022)));\n" +
        "    return rfbm(p+3.5*q+t*0.015);\n" +
        "}\n" +
        "\n" +
        "// ── Stars ───────────────────────────────────────────────────────────────\n" +
        "float star(vec2 uv,float sc,float th,float cr,float hr,float t){\n" +
        "    vec2 cell=floor(uv*sc); float h=hash1(cell);\n" +
        "    if(h<th)return 0.0;\n" +
        "    float h2=hash1(cell+vec2(7.3,31.1));\n" +
        "    float tw=0.55+0.45*sin(t*(0.8+h2*6.0)+h*6.2832);\n" +
        "    float d=length(fract(uv*sc)-0.5);\n" +
        "    return(smoothstep(cr,0.0,d)+smoothstep(hr,0.0,d)*0.32)*tw;\n" +
        "}\n" +
        "\n" +
        "// ── Featured star with 4 diffraction spikes ──────────────────────────────\n" +
        "vec3 fstar(vec2 uvA,vec2 pos,vec3 col,vec3 spikeCol,\n" +
        "           float cs,float gr,float sk,float phase,float t){\n" +
        "    vec2 d=uvA-pos; float r2=dot(d,d);\n" +
        "    float core=exp(-r2*cs);\n" +
        "    float glow=exp(-r2/(gr*gr))*0.65;\n" +
        "    float sH=exp(-d.y*d.y*sk*4.0)*exp(-abs(d.x)*sk*0.14);\n" +
        "    float sV=exp(-d.x*d.x*sk*4.0)*exp(-abs(d.y)*sk*0.14);\n" +
        "    float sD1=exp(-(d.x-d.y)*(d.x-d.y)*sk*6.0)*exp(-length(d)*sk*0.09);\n" +
        "    float sD2=exp(-(d.x+d.y)*(d.x+d.y)*sk*6.0)*exp(-length(d)*sk*0.09);\n" +
        "    float pulse=0.88+0.12*sin(t*0.9+phase);\n" +
        "    return(col*(core+glow)+spikeCol*(sH+sV+(sD1+sD2)*0.6)*0.45)*pulse;\n" +
        "}\n" +
        "\n" +
        "// ── Plasma tendril: curved sweeping energy line ───────────────────────────\n" +
        "float tendril(vec2 uvA,vec2 p0,vec2 p1,float w,float spd,float ph,float t){\n" +
        "    vec2 ab=p1-p0; float len=length(ab);\n" +
        "    vec2 dir=ab/len, perp=vec2(-dir.y,dir.x);\n" +
        "    vec2 rel=uvA-p0;\n" +
        "    float along=clamp(dot(rel,dir)/len,0.0,1.0);\n" +
        "    float curve=sin(along*6.2832+t*spd+ph)*0.045;\n" +
        "    float across=dot(rel,perp)-curve;\n" +
        "    float bright=exp(-across*across/(w*w));\n" +
        "    bright*=smoothstep(0.0,0.12,along)*smoothstep(1.0,0.88,along);\n" +
        "    float pulse=0.55+0.45*sin(along*10.0-t*spd*4.0+ph);\n" +
        "    return bright*pulse;\n" +
        "}\n" +
        "\n" +
        "void main(){\n" +
        "    vec2  uv =gl_FragCoord.xy/u_resolution;\n" +
        "    float ar =u_resolution.x/u_resolution.y;\n" +
        "    vec2  uvA=vec2(uv.x*ar,uv.y);\n" +
        "    float t  =u_time;\n" +
        "    float bx=sin(t*0.07)*0.009+cos(t*0.13)*0.004;\n" +
        "    float by=cos(t*0.09)*0.007+sin(t*0.11)*0.005;\n" +
        "    vec2  uvN=uvA*2.5+vec2(bx,by);\n" +
        "\n" +
        "    // ── Deep space base ──────────────────────────────────────────────────\n" +
        "    float bgG=1.0-length((uv-0.5)*1.3);\n" +
        "    vec3 color=mix(vec3(0.001,0.001,0.007),vec3(0.005,0.004,0.022),clamp(bgG,0.0,1.0));\n" +
        "\n" +
        "    // ── Global background haze ───────────────────────────────────────────\n" +
        "    {\n" +
        "        float raw=wfbm(uvN*0.55+vec2(2.3,-1.1),t*0.006)*0.5+0.5;\n" +
        "        float den=pow(clamp(raw,0.0,1.0),4.5)*0.40;\n" +
        "        color+=mix(vec3(0.02,0.01,0.09),vec3(0.12,0.05,0.32),den)*den;\n" +
        "    }\n" +
        "\n" +
        "    // ── Star positions ───────────────────────────────────────────────────\n" +
        "    vec2 sA=vec2(0.71*ar,0.73);\n" +
        "    vec2 sB=vec2(0.15*ar,0.21);\n" +
        "    vec2 sC=vec2(0.90*ar,0.50);\n" +
        "    float illumA=exp(-dot(uvA-sA,uvA-sA)*7.0);\n" +
        "    float illumB=exp(-dot(uvA-sB,uvA-sB)*10.0);\n" +
        "    float illumC=exp(-dot(uvA-sC,uvA-sC)*8.5);\n" +
        "\n" +
        "    // ── Organic blob deformation ─────────────────────────────────────────\n" +
        "    float ox=fbm(uvA*3.5+vec2(0.3,1.7)+t*0.004)*0.11;\n" +
        "    float oy=fbm(uvA*3.5+vec2(2.4,0.8)-t*0.003)*0.11;\n" +
        "    vec2  org=vec2(ox,oy);\n" +
        "\n" +
        "    // ── Blob masks with irregular edge dissolve ───────────────────────────\n" +
        "    // Edge erosion: noise erodes the boundary of each blob independently,\n" +
        "    // creating torn, ragged silhouettes instead of smooth Gaussians.\n" +
        "    vec2 c1=vec2(0.44*ar,0.55);\n" +
        "    vec2 c2=vec2(0.74*ar,0.28);\n" +
        "    vec2 c3=vec2(0.14*ar,0.22);\n" +
        "    vec2 c4=vec2(0.25*ar,0.80);\n" +
        "    vec2 c5=vec2(0.88*ar,0.62);\n" +
        "    vec2 c6=vec2(0.55*ar,0.12);\n" +
        "    vec2 c7=vec2(0.08*ar,0.62);\n" +
        "    float m1,m2,m3,m4,m5,m6,m7,mp1,mp2,mp3;\n" +
        "    {\n" +
        "        vec2 d; float g,n,ero;\n" +
        "        float tc=t*0.0018; // very slow edge evolution\n" +
        "        float erS=0.82;    // erosion strength\n" +
        "        // helper: ero factor = 1 minus noise that bites at edges (low g)\n" +
        "        // clamp(1-g*K,0,1) is 1 at edges and 0 at centre\n" +
        "        d=uvA-c1+org*1.00; g=exp(-dot(d,d)/0.090);\n" +
        "        n=gnoise(uvA*4.8+vec2(0.30,1.50)+tc)*0.5+0.5;\n" +
        "        ero=1.0-n*clamp(1.0-g*2.5,0.0,1.0)*erS;\n" +
        "        m1=clamp(g*ero,0.0,1.0);\n" +
        "        d=uvA-c2-org*0.85; g=exp(-dot(d,d)/0.055);\n" +
        "        n=gnoise(uvA*5.2+vec2(2.70,3.40)+tc)*0.5+0.5;\n" +
        "        ero=1.0-n*clamp(1.0-g*2.5,0.0,1.0)*erS;\n" +
        "        m2=clamp(g*ero,0.0,1.0);\n" +
        "        d=uvA-c3+org*1.20; g=exp(-dot(d,d)/0.035);\n" +
        "        n=gnoise(uvA*6.0+vec2(-1.50,0.80)+tc)*0.5+0.5;\n" +
        "        ero=1.0-n*clamp(1.0-g*2.5,0.0,1.0)*erS;\n" +
        "        m3=clamp(g*ero,0.0,1.0);\n" +
        "        d=uvA-c4-org*0.70; g=exp(-dot(d,d)/0.045);\n" +
        "        n=gnoise(uvA*4.2+vec2(3.80,-2.10)+tc)*0.5+0.5;\n" +
        "        ero=1.0-n*clamp(1.0-g*2.5,0.0,1.0)*erS;\n" +
        "        m4=clamp(g*ero,0.0,1.0);\n" +
        "        d=uvA-c5+org*0.60; g=exp(-dot(d,d)/0.040);\n" +
        "        n=gnoise(uvA*5.5+vec2(-3.20,4.50)+tc)*0.5+0.5;\n" +
        "        ero=1.0-n*clamp(1.0-g*2.5,0.0,1.0)*erS;\n" +
        "        m5=clamp(g*ero,0.0,1.0);\n" +
        "        d=uvA-c6-org*1.10; g=exp(-dot(d,d)/0.038);\n" +
        "        n=gnoise(uvA*5.0+vec2(1.20,-4.30)+tc)*0.5+0.5;\n" +
        "        ero=1.0-n*clamp(1.0-g*2.5,0.0,1.0)*erS;\n" +
        "        m6=clamp(g*ero,0.0,1.0);\n" +
        "        d=uvA-c7+org*0.90; g=exp(-dot(d,d)/0.032);\n" +
        "        n=gnoise(uvA*4.5+vec2(-0.80,2.90)+tc)*0.5+0.5;\n" +
        "        ero=1.0-n*clamp(1.0-g*2.5,0.0,1.0)*erS;\n" +
        "        m7=clamp(g*ero,0.0,1.0);\n" +
        "        // Elongated blobs\n" +
        "        d=(uvA-vec2(0.60*ar,0.48))+org*0.80;\n" +
        "        g=exp(-(d.x*d.x/0.004+d.y*d.y/0.095));\n" +
        "        n=gnoise(uvA*3.8+vec2(-1.10,2.80)+tc)*0.5+0.5;\n" +
        "        mp1=clamp(g*(1.0-n*clamp(1.0-g*2.5,0.0,1.0)*erS),0.0,1.0);\n" +
        "        d=(uvA-vec2(0.38*ar,0.42))-org*0.90;\n" +
        "        g=exp(-(d.x*d.x/0.100+d.y*d.y/0.003));\n" +
        "        n=gnoise(uvA*4.3+vec2(2.50,-1.60)+tc)*0.5+0.5;\n" +
        "        mp2=clamp(g*(1.0-n*clamp(1.0-g*2.5,0.0,1.0)*erS),0.0,1.0);\n" +
        "        d=(uvA-vec2(0.82*ar,0.46))+org*0.70;\n" +
        "        g=exp(-(d.x*d.x/0.003+d.y*d.y/0.065));\n" +
        "        n=gnoise(uvA*4.0+vec2(0.60,-3.50)+tc)*0.5+0.5;\n" +
        "        mp3=clamp(g*(1.0-n*clamp(1.0-g*2.5,0.0,1.0)*erS),0.0,1.0);\n" +
        "    }\n" +
        "\n" +
        "    float cosmVar=0.85+0.15*gnoise(uvN*0.45+t*0.003);\n" +
        "\n" +
        "    // ── 1. Grand violet/cobalt cloud ─────────────────────────────────────\n" +
        "    float den1=0.0;\n" +
        "    {\n" +
        "        float mask=clamp(m1*1.5+m2*0.6+m4*0.45+m7*0.5,0.0,1.0);\n" +
        "        float raw=wfbm(uvN,t)*0.5+0.5;\n" +
        "        float den=pow(max(0.0,raw-0.20)/0.80,1.4)*mask;\n" +
        "        den1=den;\n" +
        "        // Dense core is truly dark (Pillar of Creation effect)\n" +
        "        float emit=den*(1.0-pow(den,2.2)*0.85);\n" +
        "        vec3 c=mix(vec3(0.05,0.01,0.25),vec3(0.18,0.03,0.55),den);\n" +
        "        c=mix(c,vec3(0.50,0.04,0.75),smoothstep(0.18,0.55,den));\n" +
        "        c=mix(c,vec3(0.05,0.22,0.85),smoothstep(0.45,0.75,den));\n" +
        "        c=mix(c,vec3(0.75,0.88,1.00),smoothstep(0.72,0.98,den));\n" +
        "        c+=vec3(0.02,0.06,0.22)*illumA*clamp(den*2.0,0.0,1.0);\n" +
        "        c*=1.0+illumA*0.55;\n" +
        "        float eg=clamp(length(vec2(dFdx(den),dFdy(den)))*u_resolution.y*0.22,0.0,1.0)*(1.0-den*0.5);\n" +
        "        c+=vec3(0.22,0.38,0.95)*eg*0.55;\n" +
        "        float kn=pow(max(0.0,gnoise(uvN*9.0+vec2(3.3,1.1))-0.72),2.0)*den*8.0;\n" +
        "        c+=vec3(0.50,0.62,1.00)*kn;\n" +
        "        color+=c*emit*2.2*cosmVar;\n" +
        "    }\n" +
        "\n" +
        "    // ── 2. Crimson/magenta plume (hard-shadowed by cloud 1) ──────────────\n" +
        "    float den2=0.0;\n" +
        "    {\n" +
        "        float mask=clamp(mp2*3.0+m3*0.8+m5*0.65+m6*0.55,0.0,1.0);\n" +
        "        float raw=wfbm(uvN*0.78+vec2(-3.5,2.1),t*0.65)*0.5+0.5;\n" +
        "        // Cloud 1 is in front — aggressive shadow (0.78)\n" +
        "        float den=pow(max(0.0,raw-0.20)/0.80,1.5)*mask*(1.0-den1*0.78);\n" +
        "        den2=den;\n" +
        "        float emit=den*(1.0-pow(den,2.2)*0.82);\n" +
        "        vec3 c=mix(vec3(0.22,0.01,0.08),vec3(0.65,0.04,0.22),den);\n" +
        "        c=mix(c,vec3(0.95,0.18,0.42),smoothstep(0.35,0.75,den));\n" +
        "        c=mix(c,vec3(1.00,0.75,0.85),smoothstep(0.72,0.98,den));\n" +
        "        c+=vec3(0.18,0.08,0.01)*illumB*clamp(den*2.0,0.0,1.0);\n" +
        "        c*=1.0+illumB*0.40;\n" +
        "        float eg=clamp(length(vec2(dFdx(den),dFdy(den)))*u_resolution.y*0.24,0.0,1.0)*(1.0-den*0.5);\n" +
        "        c+=vec3(1.00,0.35,0.25)*eg*0.62;\n" +
        "        float kn=pow(max(0.0,gnoise(uvN*8.0+vec2(-1.2,4.4))-0.74),2.0)*den*9.0;\n" +
        "        c+=vec3(1.00,0.70,0.50)*kn;\n" +
        "        color+=c*emit*2.0*cosmVar;\n" +
        "    }\n" +
        "\n" +
        "    // ── 3. Cyan filaments (lit by star A) ────────────────────────────────\n" +
        "    {\n" +
        "        float mask=clamp(m1*0.7+m2*1.2+mp1*2.5+m6*0.65,0.0,1.0);\n" +
        "        float raw=wrfbm(uvN*1.3,t)*0.5+0.5;\n" +
        "        float den=pow(max(0.0,raw-0.32)/0.68,1.0)*mask;\n" +
        "        float lit=1.0+illumA*0.85;\n" +
        "        float eg=clamp(length(vec2(dFdx(den),dFdy(den)))*u_resolution.y*0.16,0.0,1.0);\n" +
        "        color+=vec3(0.05,0.65,0.90)*den*den*3.0*lit;\n" +
        "        color+=vec3(0.30,0.95,1.00)*pow(den,2.5)*5.0*lit;\n" +
        "        color+=vec3(0.18,1.00,0.90)*eg*0.42*lit;\n" +
        "        float kn=pow(max(0.0,gnoise(uvN*10.0+vec2(5.5,-2.1))-0.75),2.0)*den*7.0;\n" +
        "        color+=vec3(0.60,1.00,1.00)*kn;\n" +
        "    }\n" +
        "\n" +
        "    // ── 4. Gold pillars (dark spine + bright edges) ──────────────────────\n" +
        "    {\n" +
        "        float mask=clamp(mp1*2.8+mp3*2.0+m3*0.5,0.0,1.0);\n" +
        "        float raw=wrfbm(uvN*1.9+vec2(4.4,-3.1),t*0.9)*0.5+0.5;\n" +
        "        float den=pow(max(0.0,raw-0.33)/0.67,1.1)*mask;\n" +
        "        float emit=den*(1.0-pow(den,2.2)*0.88);\n" +
        "        float eg=clamp(length(vec2(dFdx(den),dFdy(den)))*u_resolution.y*0.30,0.0,1.0)*(1.0-den*0.4);\n" +
        "        color+=vec3(0.82,0.48,0.05)*emit*emit*3.5;\n" +
        "        color+=vec3(1.00,0.85,0.35)*pow(emit,3.0)*5.5;\n" +
        "        color+=vec3(1.00,0.90,0.45)*eg*0.85;\n" +
        "        float kn=pow(max(0.0,gnoise(uvN*11.0+vec2(-3.3,1.8))-0.73),2.0)*den*8.0;\n" +
        "        color+=vec3(1.00,0.95,0.55)*kn;\n" +
        "    }\n" +
        "\n" +
        "    // ── 5. Emerald wisps (shadowed by clouds 1+2) ────────────────────────\n" +
        "    {\n" +
        "        float mask=clamp(m4*1.5+m5*0.9+m7*0.6,0.0,1.0);\n" +
        "        float raw=wfbm(uvN*1.5+vec2(6.8,-5.0),t*1.1)*0.5+0.42;\n" +
        "        float den=pow(max(0.0,raw-0.26)/0.74,1.7)*mask*(1.0-(den1+den2)*0.55);\n" +
        "        float emit=den*(1.0-pow(den,2.5)*0.75);\n" +
        "        float lit=1.0+illumC*0.65;\n" +
        "        float eg=clamp(length(vec2(dFdx(den),dFdy(den)))*u_resolution.y*0.19,0.0,1.0)*(1.0-den*0.5);\n" +
        "        color+=vec3(0.04,0.62,0.28)*emit*emit*2.8*lit;\n" +
        "        color+=vec3(0.22,1.00,0.55)*pow(emit,2.8)*4.5*lit;\n" +
        "        color+=vec3(0.15,0.90,0.60)*eg*0.50;\n" +
        "        float kn=pow(max(0.0,gnoise(uvN*9.5+vec2(4.4,-5.5))-0.72),2.0)*den*7.0;\n" +
        "        color+=vec3(0.40,1.00,0.65)*kn;\n" +
        "    }\n" +
        "\n" +
        "    // ── 6. Orange/teal top cloud ─────────────────────────────────────────\n" +
        "    {\n" +
        "        float mask=clamp(m6*2.0+m2*0.45,0.0,1.0);\n" +
        "        float raw=wfbm(uvN*1.1+vec2(-5.2,3.3),t*0.8)*0.5+0.5;\n" +
        "        float den=pow(max(0.0,raw-0.22)/0.78,1.6)*mask;\n" +
        "        float emit=den*(1.0-pow(den,2.5)*0.78);\n" +
        "        float eg=clamp(length(vec2(dFdx(den),dFdy(den)))*u_resolution.y*0.22,0.0,1.0)*(1.0-den*0.5);\n" +
        "        vec3 c=mix(vec3(0.15,0.05,0.08),vec3(0.75,0.25,0.08),den);\n" +
        "        c=mix(c,vec3(0.05,0.50,0.60),smoothstep(0.40,0.75,den));\n" +
        "        c+=vec3(0.40,0.65,0.80)*eg*0.60;\n" +
        "        color+=c*emit*2.0*cosmVar;\n" +
        "    }\n" +
        "\n" +
        "    // ── 7. Deep indigo left edge (shadowed by cloud 1) ───────────────────\n" +
        "    {\n" +
        "        float mask=clamp(m7*2.2+m4*0.6,0.0,1.0);\n" +
        "        float raw=wfbm(uvN*1.25+vec2(9.1,-2.5),t*0.75)*0.5+0.5;\n" +
        "        float den=pow(max(0.0,raw-0.23)/0.77,1.7)*mask*(1.0-den1*0.65);\n" +
        "        float emit=den*(1.0-pow(den,2.5)*0.75);\n" +
        "        float lit=1.0+illumB*0.52;\n" +
        "        float eg=clamp(length(vec2(dFdx(den),dFdy(den)))*u_resolution.y*0.23,0.0,1.0)*(1.0-den*0.6);\n" +
        "        color+=mix(vec3(0.0),vec3(0.30,0.04,0.65),emit*emit)*2.8*lit;\n" +
        "        color+=vec3(0.50,0.08,1.00)*pow(emit,3.5)*2.5*lit;\n" +
        "        color+=vec3(0.35,0.10,0.75)*eg*0.60;\n" +
        "    }\n" +
        "\n" +
        "    // ── Dark absorption: dust lanes + Bok globules ────────────────────────\n" +
        "    // These are dense cold dust clouds that absorb all light behind them.\n" +
        "    // Only appears INSIDE nebula gas regions — creates dark lanes in the glow.\n" +
        "    // Stars added later will punch through, enhancing the contrast.\n" +
        "    {\n" +
        "        float gasMask=clamp(m1*1.3+m2*1.0+m3*0.8+m4*0.9+m5*0.7+mp1*1.8+mp2*2.2,0.0,1.0);\n" +
        "        // Dust lanes: large sweeping dark filaments, different phase from gas\n" +
        "        float dRaw=wfbm(uvN*1.65+vec2(5.5,-3.2),t*0.20)*0.5+0.5;\n" +
        "        float dLane=pow(max(0.0,dRaw-0.56)/0.44,1.15)*gasMask;\n" +
        "        // Bok globules: small isolated dense dark blobs at finer scale\n" +
        "        float gRaw=gnoise(uvN*3.6+vec2(-2.1,4.8))*0.5+0.5;\n" +
        "        float dGlob=pow(max(0.0,gRaw-0.68)/0.32,2.2)*gasMask;\n" +
        "        // Secondary smaller globules scattered throughout\n" +
        "        float gRaw2=gnoise(uvN*5.5+vec2(3.7,-1.4))*0.5+0.5;\n" +
        "        float dGlob2=pow(max(0.0,gRaw2-0.72)/0.28,2.5)*gasMask;\n" +
        "        float absorb=clamp(dLane*0.88+dGlob*0.72+dGlob2*0.55,0.0,0.95);\n" +
        "        color*=1.0-absorb;\n" +
        "    }\n" +
        "\n" +
        "    // ── Reflection nebulae ───────────────────────────────────────────────\n" +
        "    color+=vec3(0.20,0.36,0.95)*exp(-dot(uvA-sA,uvA-sA)/0.022)*clamp(m1+m2*0.5,0.0,1.0)*0.58;\n" +
        "    color+=vec3(0.76,0.42,0.12)*exp(-dot(uvA-sB,uvA-sB)/0.016)*clamp(m3+m7*0.6,0.0,1.0)*0.46;\n" +
        "    color+=vec3(0.55,0.20,0.90)*exp(-dot(uvA-sC,uvA-sC)/0.019)*clamp(m5+m4*0.4,0.0,1.0)*0.43;\n" +
        "\n" +
        "    // ── Cosmic fine dust ─────────────────────────────────────────────────\n" +
        "    color+=vec3(0.55,0.50,0.40)*pow(gnoise(uvN*8.0)*0.5+0.5,6.0)*0.10;\n" +
        "    color+=vec3(0.40,0.44,0.55)*pow(gnoise(uvN*14.0+vec2(3.3,1.7))*0.5+0.5,7.0)*0.07;\n" +
        "\n" +
        "    // ── Ring nebula around star C ────────────────────────────────────────\n" +
        "    {\n" +
        "        float rD=length(uvA-sC);\n" +
        "        float rR=0.055+sin(t*0.12)*0.004;\n" +
        "        color+=vec3(0.18,0.82,0.55)*exp(-pow(rD-rR,2.0)/0.00014)*1.2;\n" +
        "        color+=vec3(0.60,0.28,0.85)*exp(-pow(rD-rR*0.62,2.0)/0.00009)*0.60;\n" +
        "        color+=vec3(0.08,0.18,0.35)*exp(-rD*rD/0.004)*0.32;\n" +
        "    }\n" +
        "\n" +
        "    // ── Supernova shockwave ──────────────────────────────────────────────\n" +
        "    {\n" +
        "        vec2 shC=vec2(0.30*ar,0.68);\n" +
        "        float shD=length(uvA-shC);\n" +
        "        float shR=0.072+sin(t*0.08)*0.007;\n" +
        "        color+=vec3(0.58,0.22,0.95)*exp(-pow(shD-shR,2.0)/0.00020)*0.90;\n" +
        "        color+=vec3(0.18,0.06,0.42)*exp(-shD*shD/0.012)*0.38;\n" +
        "    }\n" +
        "\n" +
        "    // ── Distant galaxy hints ─────────────────────────────────────────────\n" +
        "    {vec2 g=uvA-vec2(0.08*ar,0.90);vec2 gs=g*vec2(1.8,4.5);color+=vec3(0.72,0.62,0.42)*exp(-dot(gs,gs)/0.0018)*0.32;}\n" +
        "    {vec2 g=uvA-vec2(0.96*ar,0.14);vec2 gs=g*vec2(3.5,1.2);color+=vec3(0.50,0.60,0.82)*exp(-dot(gs,gs)/0.0014)*0.28;}\n" +
        "    {vec2 g=uvA-vec2(0.50*ar,0.97);vec2 gs=g*vec2(2.2,5.0);color+=vec3(0.65,0.50,0.72)*exp(-dot(gs,gs)/0.0009)*0.22;}\n" +
        "    {vec2 g=uvA-vec2(0.82*ar,0.94);vec2 gs=g*vec2(4.0,1.5);color+=vec3(0.58,0.68,0.44)*exp(-dot(gs,gs)/0.0012)*0.24;}\n" +
        "    {vec2 g=uvA-vec2(0.02*ar,0.38);vec2 gs=g*vec2(1.2,3.5);color+=vec3(0.44,0.54,0.74)*exp(-dot(gs,gs)/0.0007)*0.20;}\n" +
        "\n" +
        "    // ── Plasma tendrils ──────────────────────────────────────────────────\n" +
        "    color+=vec3(0.35,0.75,1.00)*tendril(uvA,vec2(0.28*ar,0.88),vec2(0.78*ar,0.18),0.007,0.90,0.0,t)*2.5;\n" +
        "    color+=vec3(1.00,0.42,0.80)*tendril(uvA,vec2(0.06*ar,0.40),vec2(0.70*ar,0.74),0.005,0.70,1.6,t)*2.0;\n" +
        "    color+=vec3(0.52,1.00,0.62)*tendril(uvA,vec2(0.52*ar,0.06),vec2(0.94*ar,0.58),0.006,1.10,3.2,t)*1.8;\n" +
        "    color+=vec3(0.80,0.32,1.00)*tendril(uvA,vec2(0.88*ar,0.96),vec2(0.18*ar,0.55),0.005,0.80,5.0,t)*1.6;\n" +
        "    color+=vec3(1.00,0.78,0.18)*tendril(uvA,vec2(0.04*ar,0.76),vec2(0.62*ar,0.32),0.004,1.30,2.1,t)*1.4;\n" +
        "\n" +
        "    // ── Star field — stars punch through dark lanes ───────────────────────\n" +
        "    color+=vec3(0.78,0.82,0.96)*star(uv, 500.0,0.952,0.09,0.28,t)*0.65;\n" +
        "    color+=vec3(0.84,0.90,1.00)*star(uv, 265.0,0.960,0.11,0.30,t)*1.10;\n" +
        "    color+=vec3(0.90,0.95,1.00)*star(uv, 118.0,0.970,0.14,0.36,t)*2.50;\n" +
        "    color+=vec3(1.00,0.98,0.90)*star(uv,  50.0,0.982,0.17,0.42,t)*5.00;\n" +
        "    color+=vec3(1.00,0.95,0.78)*star(uvA, 21.0,0.992,0.21,0.48,t)*9.00;\n" +
        "    vec2 clC=vec2(0.62*ar,0.30);float clM=exp(-dot(uvA-clC,uvA-clC)/0.010);\n" +
        "    color+=vec3(0.88,0.93,1.00)*star(uv*1.6,170.0,0.938,0.15,0.40,t)*clM*7.0;\n" +
        "    vec2 clC2=vec2(0.20*ar,0.68);float clM2=exp(-dot(uvA-clC2,uvA-clC2)/0.008);\n" +
        "    color+=vec3(1.00,0.93,0.72)*star(uv*1.3,140.0,0.942,0.14,0.38,t)*clM2*5.5;\n" +
        "\n" +
        "    // ── Featured stars ─────────────────────────────────────────────────\n" +
        "    color+=fstar(uvA,sA,vec3(0.90,0.94,1.00),vec3(0.60,0.78,1.00),900.0,0.055,5500.0,0.0,t);\n" +
        "    color+=fstar(uvA,sB,vec3(1.00,0.88,0.58),vec3(0.92,0.72,0.35),1400.0,0.042,6000.0,2.3,t)*0.90;\n" +
        "    color+=fstar(uvA,sC,vec3(0.92,0.80,1.00),vec3(0.75,0.55,1.00),1100.0,0.046,4800.0,4.7,t)*0.82;\n" +
        "\n" +
        "    // ── Spiral vortex ───────────────────────────────────────────────────\n" +
        "    {\n" +
        "        vec2 vc=vec2(0.47*ar,0.52);vec2 vd=uvA-vc;\n" +
        "        float vr=length(vd);\n" +
        "        float vth=atan(vd.y,vd.x)+t*0.10;\n" +
        "        float vortex=exp(-vr*4.2)*(0.4+0.6*sin(vth*5.0+vr*18.0-t*0.4));\n" +
        "        vortex*=(1.0-exp(-vr*10.0));\n" +
        "        color+=vec3(0.25,0.06,0.55)*vortex*1.00;\n" +
        "        color+=vec3(0.55,0.14,0.90)*pow(max(vortex,0.0),2.0)*0.80;\n" +
        "        color+=vec3(0.10,0.03,0.28)*exp(-vr*vr*1.2)*0.40;\n" +
        "    }\n" +
        "\n" +
        "    // ── Aurora bands ────────────────────────────────────────────────────\n" +
        "    float a1=exp(-pow(uv.y-0.93,2.0)*180.0)*(0.42+0.33*sin(uvA.x*4.2+t*0.50)+0.25*cos(uvA.x*3.1-t*0.38));\n" +
        "    float a2=exp(-pow(uv.y-0.86,2.0)*280.0)*(0.35+0.42*sin(uvA.x*5.5-t*0.60)+0.23*cos(uvA.x*2.3+t*0.28));\n" +
        "    float a3=exp(-pow(uv.y-0.79,2.0)*400.0)*(0.28+0.35*sin(uvA.x*3.8+t*0.42)+0.37*cos(uvA.x*6.0-t*0.35));\n" +
        "    float a4=exp(-pow(uv.y-0.07,2.0)*220.0)*(0.35+0.38*sin(uvA.x*3.6-t*0.45)+0.27*cos(uvA.x*5.2+t*0.40));\n" +
        "    float a5=exp(-pow(uv.y-0.13,2.0)*350.0)*(0.25+0.38*sin(uvA.x*4.8+t*0.52)+0.37*cos(uvA.x*2.8-t*0.30));\n" +
        "    color+=vec3(0.08,0.38,0.85)*a1*0.65;\n" +
        "    color+=vec3(0.42,0.08,0.72)*a2*0.55;\n" +
        "    color+=vec3(0.04,0.58,0.50)*a3*0.45;\n" +
        "    color+=vec3(0.72,0.14,0.35)*a4*0.50;\n" +
        "    color+=vec3(0.05,0.42,0.82)*a5*0.38;\n" +
        "\n" +
        "    // ── Galactic ambient glow ────────────────────────────────────────────\n" +
        "    float cd=length((uv-vec2(0.5,0.5))*vec2(ar,1.0));\n" +
        "    color+=vec3(0.12,0.05,0.32)*exp(-cd*cd*1.3)*0.45;\n" +
        "\n" +
        "    // ── Vignette ────────────────────────────────────────────────────────\n" +
        "    float vig=1.0-smoothstep(0.38,1.10,length((uv-0.5)*vec2(ar,1.0)));\n" +
        "    color*=0.22+0.78*vig;\n" +
        "\n" +
        "    // ── Filmic Reinhard + gamma ──────────────────────────────────────────\n" +
        "    color=color/(color+vec3(0.40));\n" +
        "    color=pow(max(color,vec3(0.0)),vec3(0.85));\n" +
        "\n" +
        "    fragColor=vec4(color,1.0);\n" +
        "}\n";

    private CosmicBackgroundRenderer() {}
}
