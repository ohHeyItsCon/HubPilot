import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import com.google.gson.GsonBuilder;

/** Ignores constant pool indices, debug info and stack-map encoding; preserves executable instructions and handler order. */
public final class BytecodeInventory {
    static String constant(Object value) { return value == null ? "null" : value.getClass().getName()+":"+value; }
    static Map<String,Object> inventory(byte[] bytes) {
        ClassNode c = new ClassNode(); new ClassReader(bytes).accept(c, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        Map<String,Object> result = new TreeMap<>(); result.put("header",List.of(c.version,c.access,c.name,String.valueOf(c.superName),c.interfaces));
        List<String> fields = new ArrayList<>();
        for (FieldNode f : c.fields) fields.add(f.access+" "+f.name+" "+f.desc+" "+constant(f.value));
        Collections.sort(fields);result.put("fields",fields);
        Map<String,Object> methods = new TreeMap<>();
        for (MethodNode m : c.methods) {
            Map<LabelNode,Integer> labels=new IdentityHashMap<>();int index=0;
            for (AbstractInsnNode i : m.instructions) {if (i instanceof LabelNode l) labels.put(l,index);else if(i.getOpcode()>=0) index++;}
            List<String> code=new ArrayList<>();
            for (AbstractInsnNode i : m.instructions) {
                if(i.getOpcode()<0)continue;
                String s=Integer.toString(i.getOpcode());
                if(i instanceof IntInsnNode n)s+=" "+n.operand;
                else if(i instanceof VarInsnNode n)s+=" "+n.var;
                else if(i instanceof TypeInsnNode n)s+=" "+n.desc;
                else if(i instanceof FieldInsnNode n)s+=" "+n.owner+" "+n.name+" "+n.desc;
                else if(i instanceof MethodInsnNode n)s+=" "+n.owner+" "+n.name+" "+n.desc+" "+n.itf;
                else if(i instanceof InvokeDynamicInsnNode n)s+=" "+n.name+" "+n.desc+" "+n.bsm+" "+Arrays.toString(n.bsmArgs);
                else if(i instanceof JumpInsnNode n)s+=" "+labels.get(n.label);
                else if(i instanceof LdcInsnNode n)s+=" "+constant(n.cst);
                else if(i instanceof IincInsnNode n)s+=" "+n.var+" "+n.incr;
                else if(i instanceof TableSwitchInsnNode n)s+=" "+n.min+" "+n.max+" "+labels.get(n.dflt)+" "+n.labels.stream().map(labels::get).toList();
                else if(i instanceof LookupSwitchInsnNode n)s+=" "+n.keys+" "+labels.get(n.dflt)+" "+n.labels.stream().map(labels::get).toList();
                else if(i instanceof MultiANewArrayInsnNode n)s+=" "+n.desc+" "+n.dims;
                else if(!(i instanceof InsnNode))throw new AssertionError(i.getClass());
                code.add(s);
            }
            List<String> handlers = new ArrayList<>();
            for(TryCatchBlockNode t : m.tryCatchBlocks)handlers.add(labels.get(t.start)+" "+labels.get(t.end)+" "+labels.get(t.handler)+" "+t.type);
            methods.put(m.name+m.desc,List.of(m.access,code,handlers,m.exceptions));
        }
        result.put("methods",methods);return result;
    }
    public static void main(String[] args) throws Exception {
        Map<String,Object> out=new TreeMap<>();
        try(ZipFile z=new ZipFile(args[0])) {
            for(ZipEntry e:Collections.list(z.entries()))if(e.getName().startsWith(args[1])&&e.getName().endsWith(".class"))out.put(e.getName(),inventory(z.getInputStream(e).readAllBytes()));
        }
        Files.writeString(Path.of(args[2]),new GsonBuilder().setPrettyPrinting().create().toJson(out)+"\n");
    }
}
