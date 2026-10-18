# Generates the dotted-circle Focus marks (docs/brand/focus-marks/). The kept ones are y1-mix, y2-sphere,
# y3-rim and y6-flow; their SVGs are frozen in docs/brand/focus-marks, so rerunning only matters for new designs.
# Usage: python3 scripts/focus-marks.py (writes SVGs to the current folder).
import math, random
S=200; C=100; R=92
BARS=[(-28,-46,-10,46),(-28,-46,34,-28),(-28,-9,22,9)]
def dist_f(x,y):
    px,py=x-C,y-C; best=1e9
    for x0,y0,x1,y1 in BARS:
        inside = x0<=px<=x1 and y0<=py<=y1
        if inside: return -1
        dx=max(x0-px,0,px-x1); dy=max(y0-py,0,py-y1); best=min(best,math.hypot(dx,dy))
    return best
def pack(rmax_at, rmin=1.4, fixed=(), gap=1.4, margin=2.5, tries=60000, seed=5, rim=R):
    random.seed(seed); circles=list(fixed)
    for _ in range(tries):
        a=random.random()*2*math.pi; rr=rim*math.sqrt(random.random())
        x=C+rr*math.cos(a); y=C+rr*math.sin(a)
        r=min(rmax_at(x,y), rim-math.hypot(x-C,y-C), dist_f(x,y)-margin)
        if r<rmin: continue
        for cx,cy,cr in circles:
            r=min(r,math.dist((x,y),(cx,cy))-cr-gap)
            if r<rmin: break
        if r>=rmin: circles.append((x,y,r))
    return circles
d=lambda x,y: math.hypot(x-C,y-C)/R
V={
 # 1 even mix of sizes, one big dot in the open space right of the F
 "y1-mix": pack(lambda x,y: 9, fixed=[(150,135,20)]),
 # 2 big dots in the middle, tiny at the rim (bubble sphere)
 "y2-sphere": pack(lambda x,y: 1.6+11*(1-d(x,y))**1.5, fixed=[(148,128,18)]),
 # 3 tiny dots inside, big dots around the rim
 "y3-rim": pack(lambda x,y: 1.6+10*d(x,y)**2),
 # 4 one huge dot fills the lower right, the rest small
 "y4-moon": pack(lambda x,y: 4.5, fixed=[(150,140,30)]),
 # 5 mostly tiny dots with a few big ones scattered
 "y5-scatter": pack(lambda x,y: 3.5, fixed=[(150,132,16),(52,160,10),(150,62,9),(110,170,8)], seed=9),
 # 6 sizes from big at the bottom-left to small at the top-right
 "y6-flow": pack(lambda x,y: 1.6+11*max(0,((x-C)*-0.7+(y-C)*0.7)/R*0.5+0.5)**1.8, fixed=[(70,160,15)]),
 # 7 bubble disc inside a ring of even dots
 "y7-ringed": pack(lambda x,y: 7, fixed=[(146,128,15)], rim=76) + [(C+87*math.sin(2*math.pi*k/56),C-87*math.cos(2*math.pi*k/56),2.1) for k in range(56)],
 # 8 tight packing, bigger dots near the F so it stands out sharply
 "y8-edge": pack(lambda x,y: 1.6+9*max(0,1-dist_f(x,y)/28), gap=1.0, margin=2.0, seed=13),
}
for name,dots in V.items():
    for theme,(bg,fg) in {"dark":("#000","#fff"),"light":("#fff","#000")}.items():
        c=''.join(f'<circle cx="{x:.2f}" cy="{y:.2f}" r="{r:.2f}"/>' for x,y,r in dots)
        open(f"{name}-{theme}.svg","w").write(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {S} {S}" width="{S}" height="{S}"><rect width="100%" height="100%" fill="{bg}"/><g fill="{fg}">{c}</g></svg>')
