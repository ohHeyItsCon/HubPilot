"""Small read-only JVM metadata parser used for candidate ABI and reference inventory."""
import struct

class Reader:
    def __init__(self, data): self.data, self.pos = data, 0
    def read(self, size): out=self.data[self.pos:self.pos+size];self.pos+=size;return out
    def u1(self): return self.read(1)[0]
    def u2(self): return struct.unpack('>H',self.read(2))[0]
    def u4(self): return struct.unpack('>I',self.read(4))[0]

def parse(data):
    r=Reader(data)
    assert r.u4()==0xcafebabe
    minor,major=r.u2(),r.u2();cp=[None]*r.u2();i=1
    while i<len(cp):
        tag=r.u1()
        if tag==1: cp[i]=(tag,r.read(r.u2()).decode('utf-8',errors='replace'))
        elif tag in (3,4): cp[i]=(tag,r.read(4).hex())
        elif tag in (5,6): cp[i]=(tag,r.read(8).hex());i+=1
        elif tag in (7,8,16,19,20): cp[i]=(tag,r.u2())
        elif tag in (9,10,11,12,17,18): cp[i]=(tag,r.u2(),r.u2())
        elif tag==15: cp[i]=(tag,r.u1(),r.u2())
        else: raise ValueError(tag)
        i+=1
    def text(i):return cp[i][1]
    def classname(i):return text(cp[i][1]) if i else None
    flags=r.u2();name=classname(r.u2());parent=classname(r.u2());interfaces=[classname(r.u2()) for _ in range(r.u2())]
    def attrs():
        out={}
        for _ in range(r.u2()):key=text(r.u2());out[key]=r.read(r.u4())
        return out
    def members():
        out=[]
        for _ in range(r.u2()):
            access=r.u2();name=text(r.u2());desc=text(r.u2());a=attrs()
            out.append({'name':name,'descriptor':desc,'access':access,'synthetic':bool(access&0x1000),'signature':text(struct.unpack('>H',a['Signature'])[0]) if 'Signature' in a else None})
        return out
    fields,methods=members(),members();attrs()
    refs=[]
    for x in cp:
        if x and x[0] in (9,10,11):
            nt=cp[x[2]];refs.append((x[0],classname(x[1]),text(nt[1]),text(nt[2])))
    return {'name':name,'parent':parent,'interfaces':interfaces,'access':flags,'major':major,'fields':fields,'methods':methods,'refs':refs}
