import java.util.*;
import java.util.zip.*;

/** Loads and reflects all own classes without plugin initialization or server startup. */
public final class LinkageInventory {
    public static void main(String[] args) throws Exception {
        int count=0;
        for(String path : args)try(ZipFile zip=new ZipFile(path)) {
            for(ZipEntry e : Collections.list(zip.entries()))if(e.getName().startsWith("dev/hubpilot/")&&e.getName().endsWith(".class")) {
                Class<?> c=Class.forName(e.getName().replace('/','.').replace(".class",""),false,LinkageInventory.class.getClassLoader());
                c.getDeclaredConstructors();c.getDeclaredMethods();c.getDeclaredFields();count++;
            }
        }
        System.out.println("PASS class loading and member reflection: "+count+" own classes; no plugin initialization");
    }
}
